/*
 * ============================================================================
 *  LLD 11 : VENDING MACHINE  (the textbook STATE pattern - done properly)
 * ============================================================================
 *  Run:  java VendingMachineSystem.java          (design notes: README.md)
 *
 *     [money]     Coin denominations, CoinInventory (what the machine holds),
 *                 ChangeMaker: greedy vs DP with LIMITED coins (greedy can fail!)
 *     [products]  Product, Slot (code, product, quantity)
 *     [state]     VendingState interface + IdleState, HasMoneyState, DispensingState,
 *                 MaintenanceState - each class handles EVERY event for its state
 *     [machine]   VendingMachine (the State pattern's Context): delegates events to the
 *                 current state; one customer at a time (synchronized)
 *     [demo]      happy path, insufficient money, sold out, refund, exact-change-only
 *                 refusal, greedy-vs-DP change, maintenance, and a concurrency check
 *
 *  A simpler enum version lives in OOP/06-Design-Patterns/P9_State.java. This one adds
 *  what interviewers push on: making change with a LIMITED coin box, refusing a sale
 *  (and refunding) when change can't be made, and the "all-or-nothing" sale.
 *
 *  STATE vs a big switch: in a switch design, every method has `switch (state)`, and
 *  adding MAINTENANCE means editing every method. With State classes, adding a state is
 *  ONE new class; each state's rules live together.
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class VendingMachineSystem {

    // =====================================================================
    // [money]
    // =====================================================================

    /** Denominations in rupees (coins + notes). An enum = a closed, validated set of values. */
    enum Coin {
        ONE(1), TWO(2), FIVE(5), TEN(10), TWENTY(20), FIFTY(50), HUNDRED(100);
        final int value;
        Coin(int v) { value = v; }
    }

    /** The machine's coin box. EnumMap: compact and ordered by denomination. */
    static class CoinInventory {
        private final EnumMap<Coin, Integer> counts = new EnumMap<>(Coin.class);
        CoinInventory() { for (Coin c : Coin.values()) counts.put(c, 0); }
        void add(Coin c, int n) { counts.merge(c, n, Integer::sum); }
        void addAll(Map<Coin, Integer> m) { m.forEach(this::add); }
        void remove(Map<Coin, Integer> m) { m.forEach((c, n) -> { if (counts.get(c) < n) throw new IllegalStateException("not enough " + c); counts.merge(c, -n, Integer::sum); }); }
        int count(Coin c) { return counts.get(c); }
        Map<Coin, Integer> snapshot() { return new EnumMap<>(counts); }
        int total() { return counts.entrySet().stream().mapToInt(e -> e.getKey().value * e.getValue()).sum(); }
    }

    interface ChangeMaker { Optional<Map<Coin, Integer>> makeChange(int amount, Map<Coin, Integer> available); }

    /** Greedy: biggest coin first. Optimal for canonical systems with UNLIMITED coins - but can FAIL with a limited box. */
    static class GreedyChange implements ChangeMaker {
        public Optional<Map<Coin, Integer>> makeChange(int amount, Map<Coin, Integer> available) {
            Map<Coin, Integer> out = new EnumMap<>(Coin.class);
            Coin[] cs = Coin.values();
            for (int i = cs.length - 1; i >= 0 && amount > 0; i--) {
                int use = Math.min(available.getOrDefault(cs[i], 0), amount / cs[i].value);
                if (use > 0) { out.put(cs[i], use); amount -= use * cs[i].value; }
            }
            return amount == 0 ? Optional.of(out) : Optional.empty();
        }
    }

    /**
     * DP (bounded coin change, fewest coins): dp[a] = min coins to make a using the coins
     * seen so far with their limits. Each coin TYPE is processed with its count limit
     * (binary splitting would speed this up; amounts here are small).
     * Always finds change if any combination exists.
     */
    static class DpChange implements ChangeMaker {
        public Optional<Map<Coin, Integer>> makeChange(int amount, Map<Coin, Integer> available) {
            int INF = Integer.MAX_VALUE / 2;
            int[] dp = new int[amount + 1];
            Arrays.fill(dp, INF);
            dp[0] = 0;
            List<Coin> units = new ArrayList<>();                       // each physical coin = one 0/1 item
            for (Coin c : Coin.values()) for (int k = 0; k < Math.min(available.getOrDefault(c, 0), amount / c.value); k++) units.add(c);
            int[][] choice = new int[units.size() + 1][];               // for reconstruction: dp row snapshots
            choice[0] = dp.clone();
            for (int i = 0; i < units.size(); i++) {
                int v = units.get(i).value;
                for (int a = amount; a >= v; a--) if (dp[a - v] + 1 < dp[a]) dp[a] = dp[a - v] + 1;   // 0/1 knapsack, backwards
                choice[i + 1] = dp.clone();
            }
            if (dp[amount] >= INF) return Optional.empty();
            Map<Coin, Integer> out = new EnumMap<>(Coin.class);
            for (int i = units.size(), a = amount; i > 0 && a > 0; i--)
                if (choice[i][a] != choice[i - 1][a]) { Coin c = units.get(i - 1); out.merge(c, 1, Integer::sum); a -= c.value; }   // coin i was used
            return Optional.of(out);
        }
    }

    // =====================================================================
    // [products]
    // =====================================================================

    record Product(String name, int price) {}

    static class Slot {
        final String code;
        Product product;
        int quantity;
        Slot(String code, Product p, int q) { this.code = code; this.product = p; this.quantity = q; }
    }

    // =====================================================================
    // [state]
    // =====================================================================

    /** Every event the machine understands. Each state decides what each event MEANS. */
    interface VendingState {
        void insert(VendingMachine m, Coin c);
        void select(VendingMachine m, String slotCode);
        void cancel(VendingMachine m);
        default void startMaintenance(VendingMachine m) { m.reject("Finish the current transaction first"); }
        default void endMaintenance(VendingMachine m) { m.reject("Not in maintenance"); }
        String name();
    }

    static class IdleState implements VendingState {
        public void insert(VendingMachine m, Coin c) { m.credit(c); m.setState(m.hasMoney); }
        public void select(VendingMachine m, String code) { m.reject("Insert money first"); }
        public void cancel(VendingMachine m) { m.reject("Nothing to refund"); }
        public void startMaintenance(VendingMachine m) { m.setState(m.maintenance); }
        public String name() { return "IDLE"; }
    }

    static class HasMoneyState implements VendingState {
        public void insert(VendingMachine m, Coin c) { m.credit(c); }                 // keep adding
        public void select(VendingMachine m, String code) {
            Slot s = m.slots.get(code);
            if (s == null) { m.reject("No slot " + code); return; }
            if (s.quantity == 0) { m.reject(s.product.name() + " is sold out"); return; }
            if (m.balance() < s.product.price()) { m.reject("Insert Rs " + (s.product.price() - m.balance()) + " more"); return; }
            // Can we give change? Check BEFORE taking anything: the sale is all-or-nothing.
            int changeDue = m.balance() - s.product.price();
            Map<Coin, Integer> boxAfterDeposit = m.coinBoxWithCredit();
            Optional<Map<Coin, Integer>> change = m.changeMaker.makeChange(changeDue, boxAfterDeposit);
            if (change.isEmpty()) { m.reject("Exact change only - can't return Rs " + changeDue + "; refunding"); m.refundAll(); m.setState(m.idle); return; }
            m.setState(m.dispensing);
            m.completeSale(s, change.get());                                         // commits coins, product, change
            m.setState(m.idle);
        }
        public void cancel(VendingMachine m) { m.refundAll(); m.setState(m.idle); }
        public String name() { return "HAS_MONEY"; }
    }

    /** While the motor runs, everything else is refused. (Short-lived, but it is a real state.) */
    static class DispensingState implements VendingState {
        public void insert(VendingMachine m, Coin c) { m.reject("Busy"); m.returnCoin(c); }
        public void select(VendingMachine m, String code) { m.reject("Busy"); }
        public void cancel(VendingMachine m) { m.reject("Busy"); }
        public String name() { return "DISPENSING"; }
    }

    static class MaintenanceState implements VendingState {
        public void insert(VendingMachine m, Coin c) { m.reject("Out of service"); m.returnCoin(c); }
        public void select(VendingMachine m, String code) { m.reject("Out of service"); }
        public void cancel(VendingMachine m) { m.reject("Out of service"); }
        public void startMaintenance(VendingMachine m) { m.reject("Already in maintenance"); }
        public void endMaintenance(VendingMachine m) { m.setState(m.idle); }
        public String name() { return "MAINTENANCE"; }
    }

    // =====================================================================
    // [machine] the Context
    // =====================================================================

    static class VendingMachine {
        // states are stateless objects -> one instance each, shared
        final VendingState idle = new IdleState(), hasMoney = new HasMoneyState(), dispensing = new DispensingState(), maintenance = new MaintenanceState();
        private VendingState state = idle;
        final Map<String, Slot> slots = new LinkedHashMap<>();
        final CoinInventory box = new CoinInventory();
        final ChangeMaker changeMaker;
        private final EnumMap<Coin, Integer> credit = new EnumMap<>(Coin.class);   // coins of the CURRENT customer
        // outputs (what a real machine would push out of its trays)
        final List<String> dispensed = new ArrayList<>(), messages = new ArrayList<>();
        final List<Coin> returnTray = new ArrayList<>();
        long salesTotal;

        VendingMachine(ChangeMaker cm) { this.changeMaker = cm; }

        // ---- events: one customer at a time -> synchronized on the machine ----
        synchronized void insert(Coin c) { state.insert(this, c); }
        synchronized void select(String code) { state.select(this, code); }
        synchronized void cancel() { state.cancel(this); }
        synchronized void startMaintenance() { state.startMaintenance(this); }
        synchronized void endMaintenance() { state.endMaintenance(this); }

        // ---- admin (only in maintenance) ----
        synchronized void restock(String code, Product p, int qty) {
            requireMaintenance();
            slots.computeIfAbsent(code, k -> new Slot(k, p, 0)).quantity += qty;
            slots.get(code).product = p;
        }
        synchronized void loadCoins(Coin c, int n) { requireMaintenance(); box.add(c, n); }
        synchronized int collectCash() { requireMaintenance(); int t = box.total(); box.remove(box.snapshot()); return t; }
        private void requireMaintenance() { if (state != maintenance) throw new IllegalStateException("Admin operations need MAINTENANCE"); }

        // ---- helpers used by states ----
        void setState(VendingState s) { state = s; }
        void credit(Coin c) { credit.merge(c, 1, Integer::sum); }
        int balance() { return credit.entrySet().stream().mapToInt(e -> e.getKey().value * e.getValue()).sum(); }
        void reject(String msg) { messages.add(msg); }
        void returnCoin(Coin c) { returnTray.add(c); }

        Map<Coin, Integer> coinBoxWithCredit() {
            Map<Coin, Integer> m = box.snapshot();
            credit.forEach((c, n) -> m.merge(c, n, Integer::sum));            // the customer's coins can be used as change
            return m;
        }

        void refundAll() {
            credit.forEach((c, n) -> { for (int i = 0; i < n; i++) returnTray.add(c); });   // give back THEIR coins
            credit.clear();
        }

        /** The commit: every effect of a sale happens here, after all checks passed. */
        void completeSale(Slot s, Map<Coin, Integer> change) {
            box.addAll(credit);
            credit.clear();
            box.remove(change);
            change.forEach((c, n) -> { for (int i = 0; i < n; i++) returnTray.add(c); });
            s.quantity--;
            salesTotal += s.product.price();
            dispensed.add(s.product.name());
        }

        synchronized String state() { return state.name(); }
        synchronized int returned() { int t = returnTray.stream().mapToInt(c -> c.value).sum(); returnTray.clear(); return t; }
        synchronized String lastMessage() { return messages.isEmpty() ? "" : messages.get(messages.size() - 1); }
    }

    // =====================================================================
    // [demo]
    // =====================================================================

    static VendingMachine stocked(ChangeMaker cm) {
        VendingMachine m = new VendingMachine(cm);
        m.startMaintenance();
        m.restock("A1", new Product("Chips", 20), 2);
        m.restock("B1", new Product("Soda", 35), 5);
        m.restock("C1", new Product("Candy", 7), 5);
        m.loadCoins(Coin.TEN, 5);
        m.loadCoins(Coin.FIVE, 5);
        m.loadCoins(Coin.ONE, 5);
        m.endMaintenance();
        return m;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("1. Happy path with change");
        VendingMachine m = stocked(new DpChange());
        check("starts IDLE", "IDLE", m.state());
        m.select("A1");
        check("select before paying is refused", "Insert money first", m.lastMessage());
        m.insert(Coin.FIFTY);
        check("HAS_MONEY after a coin", "HAS_MONEY", m.state());
        m.select("B1");                                                           // soda Rs 35, change Rs 15
        check("dispensed", "[Soda]", m.dispensed);
        check("change Rs 15", 15, m.returned());
        check("back to IDLE", "IDLE", m.state());

        System.out.println("2. Not enough money, top up, sold out, cancel");
        m.insert(Coin.TEN);
        m.select("A1");
        check("asks for more", "Insert Rs 10 more", m.lastMessage());
        m.insert(Coin.TEN);
        m.select("A1");
        m.insert(Coin.TWENTY); m.select("A1");                                      // second (last) chips
        m.insert(Coin.TWENTY); m.select("A1");
        check("sold out message", "Chips is sold out", m.lastMessage());
        m.returned();
        m.cancel();
        check("cancel returns the customer's own coins", 20, m.returned());
        check("chips sold twice", 2L, m.dispensed.stream().filter("Chips"::equals).count());

        System.out.println("3. Change-making: greedy FAILS with a limited coin box, DP succeeds");
        Map<Coin, Integer> box = new EnumMap<>(Map.of(Coin.FIVE, 1, Coin.TWO, 3));
        check("greedy for Rs 6 (takes 5, then stuck)", false, new GreedyChange().makeChange(6, box).isPresent());
        check("DP finds 2+2+2", "{TWO=3}", new DpChange().makeChange(6, box).orElse(null));
        Random rnd = new Random(1);
        boolean dpNeverWorse = true, dpValid = true;
        for (int t = 0; t < 500; t++) {
            Map<Coin, Integer> b = new EnumMap<>(Coin.class);
            for (Coin c : Coin.values()) b.put(c, rnd.nextInt(4));
            int amt = rnd.nextInt(120);
            Optional<Map<Coin, Integer>> g = new GreedyChange().makeChange(amt, b), d = new DpChange().makeChange(amt, b);
            if (g.isPresent() && d.isEmpty()) dpNeverWorse = false;
            if (d.isPresent()) {
                int sum = 0;
                for (var e : d.get().entrySet()) { sum += e.getKey().value * e.getValue(); if (e.getValue() > b.get(e.getKey())) dpValid = false; }
                if (sum != amt) dpValid = false;
            }
        }
        check("500 random boxes: DP succeeds whenever greedy does", true, dpNeverWorse);
        check("DP change is exact and within the box", true, dpValid);

        System.out.println("4. Exact-change-only: the sale is refused and fully refunded");
        VendingMachine poor = new VendingMachine(new DpChange());
        poor.startMaintenance(); poor.restock("C1", new Product("Candy", 7), 3); poor.endMaintenance();   // empty coin box
        poor.insert(Coin.TEN);
        poor.select("C1");
        check("refused", true, poor.lastMessage().startsWith("Exact change only"));
        check("customer gets their Rs 10 back", 10, poor.returned());
        check("no candy given", 0, poor.dispensed.size());
        check("stock unchanged", 3, poor.slots.get("C1").quantity);
        poor.insert(Coin.FIVE); poor.insert(Coin.TWO);
        poor.select("C1");
        check("exact amount works", "[Candy]", poor.dispensed);

        System.out.println("5. Maintenance state");
        expect("restock outside maintenance", () -> m.restock("A1", new Product("Chips", 20), 5));
        m.startMaintenance();
        m.insert(Coin.TEN);
        check("coins rejected and returned", 10, m.returned());
        m.restock("A1", new Product("Chips", 20), 5);
        int cash = m.collectCash();
        System.out.println("   collected Rs " + cash + " from the box");
        m.endMaintenance();
        check("back in service", "IDLE", m.state());
        m.insert(Coin.FIFTY);
        m.startMaintenance();
        check("can't enter maintenance mid-transaction", "Finish the current transaction first", m.lastMessage());
        m.cancel();
        m.returned();

        System.out.println("6. Concurrency: 8 threads hammer one machine with insert/select/cancel");
        VendingMachine busy = stocked(new DpChange());
        busy.startMaintenance(); busy.restock("B1", new Product("Soda", 35), 1000); busy.loadCoins(Coin.FIVE, 200); busy.endMaintenance();
        int startCash = busy.box.total();
        AtomicInteger inserted = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int t = 0; t < 8; t++) pool.submit(() -> {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < 2000; i++) {
                switch (r.nextInt(3)) {
                    case 0 -> { Coin c = r.nextBoolean() ? Coin.TEN : Coin.FIFTY; busy.insert(c); inserted.addAndGet(c.value); }
                    case 1 -> busy.select("B1");
                    default -> busy.cancel();
                }
            }
        });
        pool.shutdown(); pool.awaitTermination(20, TimeUnit.SECONDS);
        busy.cancel();
        int returned = busy.returned();
        long sold = busy.dispensed.size();
        check("money conserved: inserted = sales + returned", (long) inserted.get(), busy.salesTotal + returned);
        check("sales x price == salesTotal", sold * 35, busy.salesTotal);
        check("box grew by exactly the sales", (long) startCash + busy.salesTotal, (long) busy.box.total());
        summary();
    }

    static void expect(String label, Runnable r) {
        try { r.run(); check(label, "exception", "none"); }
        catch (RuntimeException e) { check(label, "exception", "exception"); }
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
