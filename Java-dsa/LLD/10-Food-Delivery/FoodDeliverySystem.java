/*
 * ============================================================================
 *  LLD 10 : FOOD DELIVERY (Swiggy / Zomato)
 * ============================================================================
 *  Run:  java FoodDeliverySystem.java          (design notes: README.md)
 *
 *     [catalog]   Restaurant, MenuItem (stock can be limited), Customer, DeliveryAgent
 *     [cart]      Cart: single-restaurant rule, quantities
 *     [pricing]   PriceCalculator: items + delivery fee + taxes, DiscountRule chain (coupons)
 *     [order]     Order + OrderStatus: a MULTI-ACTOR state machine - every transition says
 *                 WHO may perform it (customer / restaurant / agent / system)
 *     [dispatch]  AgentAssignmentStrategy (nearest available), agent claimed by CAS
 *     [events]    OrderEventBus (Observer): customer app, restaurant tablet, agent app, analytics
 *     [service]   OrderService: place, accept/reject, prepare, ready, pick up, deliver, cancel + refunds
 *     [demo]      full lifecycle, wrong-actor attempts, cancel rules, coupons, stock race,
 *                 and "customer cancels while the restaurant accepts" race
 *
 *  THE KEY MODELLING IDEA: the state machine is not just FROM -> TO. It's FROM -> TO BY WHOM.
 *     PLACED -> ACCEPTED        only the RESTAURANT
 *     READY  -> PICKED_UP       only the assigned AGENT
 *     PLACED -> CANCELLED       the CUSTOMER (free) ... but after ACCEPTED only with a fee,
 *                               and after PICKED_UP not at all.
 *  Encoding (from, to, actor) in ONE table means no actor can push an order into a state
 *  it's not responsible for, and the rules are reviewable in one place.
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public class FoodDeliverySystem {

    // =====================================================================
    // [catalog]
    // =====================================================================

    record Location(double x, double y) { double dist(Location o) { return Math.hypot(x - o.x, y - o.y); } }

    static class MenuItem {
        final String id, name;
        final long pricePaise;
        final AtomicInteger stock;                                  // -1 = unlimited; else portions left today
        MenuItem(String id, String name, long pricePaise, int stock) { this.id = id; this.name = name; this.pricePaise = pricePaise; this.stock = new AtomicInteger(stock); }

        /** Atomically reserve `qty` portions; never goes below zero. */
        boolean reserve(int qty) {
            while (true) {
                int cur = stock.get();
                if (cur == -1) return true;
                if (cur < qty) return false;
                if (stock.compareAndSet(cur, cur - qty)) return true;
            }
        }
        void release(int qty) { stock.updateAndGet(s -> s == -1 ? -1 : s + qty); }
    }

    static class Restaurant {
        final String id, name;
        final Location location;
        final Map<String, MenuItem> menu = new LinkedHashMap<>();
        volatile boolean open = true;
        Restaurant(String id, String name, Location loc) { this.id = id; this.name = name; this.location = loc; }
        Restaurant item(MenuItem m) { menu.put(m.id, m); return this; }
    }

    record Customer(String id, String name, Location address) {}

    enum AgentStatus { OFFLINE, AVAILABLE, BUSY }

    static class DeliveryAgent {
        final String id;
        volatile Location location;
        final AtomicReference<AgentStatus> status = new AtomicReference<>(AgentStatus.AVAILABLE);
        DeliveryAgent(String id, Location l) { this.id = id; this.location = l; }
        boolean tryClaim() { return status.compareAndSet(AgentStatus.AVAILABLE, AgentStatus.BUSY); }
        void release() { status.set(AgentStatus.AVAILABLE); }
    }

    // =====================================================================
    // [cart]
    // =====================================================================

    static class Cart {
        final Customer customer;
        Restaurant restaurant;
        final Map<MenuItem, Integer> lines = new LinkedHashMap<>();
        Cart(Customer c) { customer = c; }

        /** One restaurant per order: adding from another restaurant is rejected (the app asks "start a new cart?"). */
        void add(Restaurant r, String itemId, int qty) {
            if (qty <= 0) throw new IllegalArgumentException("qty must be positive");
            if (restaurant != null && restaurant != r) throw new IllegalStateException("Cart has items from " + restaurant.name);
            MenuItem m = r.menu.get(itemId);
            if (m == null) throw new IllegalArgumentException("No item " + itemId);
            restaurant = r;
            lines.merge(m, qty, Integer::sum);
        }
        long itemsTotal() { return lines.entrySet().stream().mapToLong(e -> e.getKey().pricePaise * e.getValue()).sum(); }
    }

    // =====================================================================
    // [pricing] coupons as a chain of rules
    // =====================================================================

    record Bill(long items, long deliveryFee, long tax, long discount, long total) {}

    /** One discount rule. Each rule may or may not apply; the calculator applies the BEST one. */
    interface DiscountRule { long discountPaise(Cart cart, long itemsTotal); String code(); }

    record PercentOff(String code, int percent, long maxOff, long minOrder) implements DiscountRule {
        public long discountPaise(Cart c, long items) { return items < minOrder ? 0 : Math.min(maxOff, items * percent / 100); }
    }

    record FlatOff(String code, long off, long minOrder) implements DiscountRule {
        public long discountPaise(Cart c, long items) { return items < minOrder ? 0 : Math.min(off, items); }
    }

    static class PriceCalculator {
        private final Map<String, DiscountRule> coupons = new HashMap<>();
        void addCoupon(DiscountRule r) { coupons.put(r.code(), r); }

        Bill bill(Cart cart, String couponCode) {
            long items = cart.itemsTotal();
            double km = cart.restaurant.location.dist(cart.customer.address);
            long delivery = 2_000 + Math.round(km * 800);                       // Rs 20 + Rs 8/km
            long discount = 0;
            if (couponCode != null) {
                DiscountRule r = coupons.get(couponCode);
                if (r == null) throw new IllegalArgumentException("Invalid coupon " + couponCode);
                discount = r.discountPaise(cart, items);
                if (discount == 0) throw new IllegalArgumentException("Coupon " + couponCode + " not applicable");
            }
            long tax = (items - discount) * 5 / 100;                            // 5% GST on food after discount
            return new Bill(items, delivery, tax, discount, items - discount + delivery + tax);
        }
    }

    // =====================================================================
    // [order] multi-actor state machine
    // =====================================================================

    enum Actor { CUSTOMER, RESTAURANT, AGENT, SYSTEM }

    enum OrderStatus { PLACED, ACCEPTED, PREPARING, READY, PICKED_UP, DELIVERED, REJECTED, CANCELLED }

    /** (from, to) -> who may do it. THE single source of truth for the lifecycle. */
    static final Map<OrderStatus, Map<OrderStatus, Set<Actor>>> RULES = Map.of(
            OrderStatus.PLACED, Map.of(OrderStatus.ACCEPTED, Set.of(Actor.RESTAURANT), OrderStatus.REJECTED, Set.of(Actor.RESTAURANT, Actor.SYSTEM),
                    OrderStatus.CANCELLED, Set.of(Actor.CUSTOMER, Actor.SYSTEM)),
            OrderStatus.ACCEPTED, Map.of(OrderStatus.PREPARING, Set.of(Actor.RESTAURANT), OrderStatus.CANCELLED, Set.of(Actor.CUSTOMER, Actor.RESTAURANT)),
            OrderStatus.PREPARING, Map.of(OrderStatus.READY, Set.of(Actor.RESTAURANT), OrderStatus.CANCELLED, Set.of(Actor.CUSTOMER, Actor.RESTAURANT)),
            OrderStatus.READY, Map.of(OrderStatus.PICKED_UP, Set.of(Actor.AGENT)),
            OrderStatus.PICKED_UP, Map.of(OrderStatus.DELIVERED, Set.of(Actor.AGENT)));

    static class Order {
        final String id;
        final Customer customer;
        final Restaurant restaurant;
        final Map<MenuItem, Integer> lines;
        final Bill bill;
        volatile DeliveryAgent agent;
        private OrderStatus status = OrderStatus.PLACED;
        final List<String> history = new CopyOnWriteArrayList<>();
        long refundPaise;

        Order(String id, Cart cart, Bill bill) {
            this.id = id; this.customer = cart.customer; this.restaurant = cart.restaurant; this.lines = Map.copyOf(cart.lines); this.bill = bill;
            history.add("PLACED");
        }

        /** Validated, atomic transition. Returns the previous status. */
        synchronized OrderStatus transition(OrderStatus to, Actor by) {
            Set<Actor> allowed = RULES.getOrDefault(status, Map.of()).getOrDefault(to, Set.of());
            if (!allowed.contains(by)) throw new IllegalStateException(by + " cannot move order " + id + " from " + status + " to " + to);
            OrderStatus from = status;
            status = to;
            history.add(to + " by " + by);
            return from;
        }
        synchronized OrderStatus status() { return status; }
    }

    // =====================================================================
    // [dispatch]
    // =====================================================================

    interface AgentAssignmentStrategy { List<DeliveryAgent> rank(Order o, Collection<DeliveryAgent> agents); }

    static class NearestAgentToRestaurant implements AgentAssignmentStrategy {
        public List<DeliveryAgent> rank(Order o, Collection<DeliveryAgent> agents) {
            return agents.stream().filter(a -> a.status.get() == AgentStatus.AVAILABLE)
                    .sorted(Comparator.comparingDouble(a -> a.location.dist(o.restaurant.location))).toList();
        }
    }

    // =====================================================================
    // [events]
    // =====================================================================

    record OrderEvent(String orderId, OrderStatus status, Actor by) {}

    static class OrderEventBus {
        private final Map<String, List<Consumer<OrderEvent>>> subscribers = new ConcurrentHashMap<>();
        void subscribe(String topic, Consumer<OrderEvent> c) { subscribers.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(c); }
        void publish(String topic, OrderEvent e) { subscribers.getOrDefault(topic, List.of()).forEach(c -> c.accept(e)); }
    }

    // =====================================================================
    // [service]
    // =====================================================================

    interface PaymentGateway { boolean charge(String orderId, long paise); void refund(String orderId, long paise); }

    static class OrderService {
        private final Map<String, Order> orders = new ConcurrentHashMap<>();
        private final Map<String, DeliveryAgent> agents = new ConcurrentHashMap<>();
        private final PriceCalculator pricing;
        private final AgentAssignmentStrategy assignment;
        private final PaymentGateway payments;
        final OrderEventBus bus = new OrderEventBus();
        private final AtomicLong seq = new AtomicLong();

        OrderService(PriceCalculator p, AgentAssignmentStrategy a, PaymentGateway pay) { pricing = p; assignment = a; payments = pay; }
        void addAgent(DeliveryAgent a) { agents.put(a.id, a); }

        Order place(Cart cart, String coupon) {
            if (cart.lines.isEmpty()) throw new IllegalStateException("Empty cart");
            if (!cart.restaurant.open) throw new IllegalStateException(cart.restaurant.name + " is closed");
            // reserve stock for every line, all-or-nothing
            List<Map.Entry<MenuItem, Integer>> reserved = new ArrayList<>();
            for (var line : cart.lines.entrySet()) {
                if (!line.getKey().reserve(line.getValue())) {
                    reserved.forEach(r -> r.getKey().release(r.getValue()));
                    throw new IllegalStateException(line.getKey().name + " is sold out");
                }
                reserved.add(line);
            }
            Bill bill = pricing.bill(cart, coupon);
            String id = "O" + seq.incrementAndGet();
            if (!payments.charge(id, bill.total())) {
                reserved.forEach(r -> r.getKey().release(r.getValue()));
                throw new IllegalStateException("Payment failed");
            }
            Order o = new Order(id, cart, bill);
            orders.put(id, o);
            publish(o, Actor.CUSTOMER);
            return o;
        }

        void accept(String id) { Order o = order(id); o.transition(OrderStatus.ACCEPTED, Actor.RESTAURANT); publish(o, Actor.RESTAURANT); assignAgent(o); }
        void reject(String id) { Order o = order(id); o.transition(OrderStatus.REJECTED, Actor.RESTAURANT); refundAll(o); publish(o, Actor.RESTAURANT); }
        void startPreparing(String id) { Order o = order(id); o.transition(OrderStatus.PREPARING, Actor.RESTAURANT); publish(o, Actor.RESTAURANT); }
        void markReady(String id) { Order o = order(id); o.transition(OrderStatus.READY, Actor.RESTAURANT); publish(o, Actor.RESTAURANT); }

        void pickUp(String id, String agentId) {
            Order o = order(id);
            if (o.agent == null || !o.agent.id.equals(agentId)) throw new IllegalStateException(agentId + " is not assigned to " + id);
            o.transition(OrderStatus.PICKED_UP, Actor.AGENT);
            publish(o, Actor.AGENT);
        }

        void deliver(String id, String agentId) {
            Order o = order(id);
            if (o.agent == null || !o.agent.id.equals(agentId)) throw new IllegalStateException(agentId + " is not assigned to " + id);
            o.transition(OrderStatus.DELIVERED, Actor.AGENT);
            o.agent.location = o.customer.address();
            o.agent.release();
            publish(o, Actor.AGENT);
        }

        /** Customer cancel: full refund before the restaurant accepts, 50% after (food is being made). */
        long cancelByCustomer(String id) {
            Order o = order(id);
            OrderStatus before;
            DeliveryAgent toRelease;
            synchronized (o) {                                                      // transition + agent read together
                before = o.transition(OrderStatus.CANCELLED, Actor.CUSTOMER);       // throws once READY/PICKED_UP
                toRelease = o.agent;
            }
            long refund = before == OrderStatus.PLACED ? o.bill.total() : o.bill.total() / 2;
            if (before == OrderStatus.PLACED) o.lines.forEach(MenuItem::release);      // nothing cooked yet: stock back
            if (toRelease != null) toRelease.release();
            payments.refund(o.id, refund);
            o.refundPaise = refund;
            publish(o, Actor.CUSTOMER);
            return refund;
        }

        /** System timeout: the restaurant didn't respond in time. */
        void autoRejectIfUnanswered(String id) {
            Order o = order(id);
            if (o.status() == OrderStatus.PLACED) { o.transition(OrderStatus.REJECTED, Actor.SYSTEM); refundAll(o); publish(o, Actor.SYSTEM); }
        }

        /**
         * Claim an agent (CAS), then attach it UNDER THE ORDER'S LOCK after re-checking the status.
         * Without the re-check this race leaks agents (found by the race test in main):
         *   restaurant: transition PLACED -> ACCEPTED
         *   customer:   transition ACCEPTED -> CANCELLED, sees agent == null, releases nobody
         *   restaurant: claims agent X for the now-CANCELLED order -> X is BUSY forever.
         */
        private void assignAgent(Order o) {
            for (DeliveryAgent a : assignment.rank(o, agents.values())) {
                if (!a.tryClaim()) continue;                                     // CAS: one order per agent
                synchronized (o) {
                    OrderStatus st = o.status();
                    if (st == OrderStatus.ACCEPTED || st == OrderStatus.PREPARING) { o.agent = a; return; }
                }
                a.release();                                                     // order was cancelled meanwhile
                return;
            }
            // none free: production would retry on a timer / when an agent frees up
        }

        private void refundAll(Order o) { o.lines.forEach(MenuItem::release); payments.refund(o.id, o.bill.total()); o.refundPaise = o.bill.total(); }

        private void publish(Order o, Actor by) {
            OrderEvent e = new OrderEvent(o.id, o.status(), by);
            bus.publish("customer:" + o.customer.id(), e);
            bus.publish("restaurant:" + o.restaurant.id, e);
            if (o.agent != null) bus.publish("agent:" + o.agent.id, e);
        }

        Order order(String id) { Order o = orders.get(id); if (o == null) throw new IllegalArgumentException("No order " + id); return o; }
    }

    // =====================================================================
    // [demo]
    // =====================================================================

    public static void main(String[] args) throws Exception {
        Map<String, Long> charged = new ConcurrentHashMap<>(), refunded = new ConcurrentHashMap<>();
        PaymentGateway pay = new PaymentGateway() {
            public boolean charge(String id, long p) { charged.put(id, p); return true; }
            public void refund(String id, long p) { refunded.merge(id, p, Long::sum); }
        };
        PriceCalculator pricing = new PriceCalculator();
        pricing.addCoupon(new PercentOff("WELCOME50", 50, 10_000, 15_000));
        pricing.addCoupon(new FlatOff("FLAT75", 7_500, 50_000));
        OrderService svc = new OrderService(pricing, new NearestAgentToRestaurant(), pay);
        Restaurant meghana = new Restaurant("r1", "Meghana Foods", new Location(0, 0))
                .item(new MenuItem("biryani", "Chicken Biryani", 30_000, -1))
                .item(new MenuItem("special", "Chef's Special", 50_000, 2));
        Restaurant other = new Restaurant("r2", "Truffles", new Location(5, 5)).item(new MenuItem("burger", "Burger", 25_000, -1));
        Customer asha = new Customer("asha", "Asha", new Location(3, 4));                // 5 km away
        DeliveryAgent nearAgent = new DeliveryAgent("a-near", new Location(0.5, 0)), farAgent = new DeliveryAgent("a-far", new Location(4, 0));
        svc.addAgent(nearAgent); svc.addAgent(farAgent);
        List<String> customerFeed = new CopyOnWriteArrayList<>();
        svc.bus.subscribe("customer:asha", e -> customerFeed.add(e.status().name()));

        System.out.println("1. Cart rules and billing");
        Cart cart = new Cart(asha);
        cart.add(meghana, "biryani", 2);
        expect("one restaurant per cart", () -> cart.add(other, "burger", 1));
        Bill b = pricing.bill(cart, "WELCOME50");
        check("bill: 600 items, 100 off (capped), 5km fee 60, 5% tax", "Bill[items=60000, deliveryFee=6000, tax=2500, discount=10000, total=58500]", b);
        expect("coupon below minimum order", () -> { Cart c = new Cart(asha); c.add(meghana, "biryani", 1); pricing.bill(c, "FLAT75"); });

        System.out.println("2. Full lifecycle, each step by the right actor");
        Order o = svc.place(cart, "WELCOME50");
        expect("customer can't accept their own order", () -> o.transition(OrderStatus.ACCEPTED, Actor.CUSTOMER));
        svc.accept(o.id);
        check("nearest agent assigned on accept", "a-near", o.agent.id);
        svc.startPreparing(o.id);
        svc.markReady(o.id);
        expect("wrong agent can't pick up", () -> svc.pickUp(o.id, "a-far"));
        svc.pickUp(o.id, "a-near");
        expect("customer can't cancel after pickup", () -> svc.cancelByCustomer(o.id));
        svc.deliver(o.id, "a-near");
        check("status", OrderStatus.DELIVERED, o.status());
        check("customer saw every step", "[PLACED, ACCEPTED, PREPARING, READY, PICKED_UP, DELIVERED]", customerFeed);
        check("agent free again", AgentStatus.AVAILABLE, nearAgent.status.get());

        System.out.println("3. Cancellation and rejection refunds");
        Cart c2 = new Cart(asha); c2.add(meghana, "biryani", 1);
        Order o2 = svc.place(c2, null);
        check("cancel before accept: full refund", o2.bill.total(), svc.cancelByCustomer(o2.id));
        Cart c3 = new Cart(asha); c3.add(meghana, "biryani", 1);
        Order o3 = svc.place(c3, null);
        svc.accept(o3.id);
        check("cancel after accept: 50%", o3.bill.total() / 2, svc.cancelByCustomer(o3.id));
        check("its agent was released", AgentStatus.AVAILABLE, nearAgent.status.get());
        Cart c4 = new Cart(asha); c4.add(meghana, "biryani", 1);
        Order o4 = svc.place(c4, null);
        svc.autoRejectIfUnanswered(o4.id);
        check("restaurant silent -> SYSTEM rejects + refund", OrderStatus.REJECTED + "/" + o4.bill.total(), o4.status() + "/" + refunded.get(o4.id));

        System.out.println("4. Limited stock: 2 portions of the special, 10 customers at once");
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger placed = new AtomicInteger();
        for (int i = 0; i < 10; i++) {
            Customer cu = new Customer("c" + i, "C", new Location(1, 1));
            pool.submit(() -> { go.await(); try { Cart c = new Cart(cu); c.add(meghana, "special", 1); svc.place(c, null); placed.incrementAndGet(); } catch (IllegalStateException ignored) {} return null; });
        }
        go.countDown();
        pool.shutdown(); pool.awaitTermination(5, TimeUnit.SECONDS);
        check("exactly 2 orders got the special", 2, placed.get());
        check("stock never negative", 0, meghana.menu.get("special").stock.get());

        System.out.println("5. Race: customer cancels while the restaurant accepts (500 rounds)");
        int wrongRefund = 0, cancelFirst = 0, acceptFirst = 0;
        for (int round = 0; round < 500; round++) {
            Cart c = new Cart(asha); c.add(meghana, "biryani", 1);
            Order ro = svc.place(c, null);
            CountDownLatch start = new CountDownLatch(1);
            AtomicBoolean accepted = new AtomicBoolean();
            Thread t1 = new Thread(() -> { try { start.await(); svc.accept(ro.id); accepted.set(true); } catch (Exception ignored) {} });
            Thread t2 = new Thread(() -> { try { start.await(); svc.cancelByCustomer(ro.id); } catch (Exception ignored) {} });
            t1.start(); t2.start(); start.countDown(); t1.join(); t2.join();
            // Two legal serialisations: cancel wins (accept is rejected, full refund) or
            // accept wins and the cancel then pays the 50% penalty. Nothing else may happen.
            if (accepted.get()) { acceptFirst++; if (ro.refundPaise != ro.bill.total() / 2) wrongRefund++; }
            else { cancelFirst++; if (ro.refundPaise != ro.bill.total()) wrongRefund++; }
            if (ro.status() != OrderStatus.CANCELLED) wrongRefund++;
        }
        check("refund always matches who won the race", 0, wrongRefund);
        System.out.println("   cancel won " + cancelFirst + " times, accept won " + acceptFirst + " times");
        check("all agents released afterwards", true, nearAgent.status.get() == AgentStatus.AVAILABLE && farAgent.status.get() == AgentStatus.AVAILABLE);
        summary();
    }

    static void expect(String label, Runnable r) {
        try { r.run(); check(label, "exception", "none"); }
        catch (RuntimeException e) { check(label, "exception", "exception"); System.out.println("         (" + e.getMessage() + ")"); }
    }

    // ---------------- tiny test helper (same in every problem file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = fmt(expected), a = fmt(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-34s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static String fmt(Object o) {
        if (o instanceof int[] x) return Arrays.toString(x);
        if (o instanceof int[][] x) return Arrays.deepToString(x);
        if (o instanceof char[] x) return Arrays.toString(x);
        if (o instanceof char[][] x) return Arrays.deepToString(x);
        return String.valueOf(o);
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " tests passed");
        if (passed != total) System.exit(1);
    }
}
