/*
 * ============================================================================
 *  LLD 19 : STOCK EXCHANGE ORDER MATCHING ENGINE
 * ============================================================================
 *  Run:  java OrderMatchingEngine.java          (design notes: README.md)
 *
 *     [model]     Side, OrderType (LIMIT / MARKET), TimeInForce (GTC / IOC / FOK), Order, Trade
 *     [book]      OrderBook per symbol: bids TreeMap(price DESC) and asks TreeMap(price ASC),
 *                 each price level a FIFO queue -> PRICE-TIME PRIORITY
 *     [matching]  incoming order crosses the opposite side best-first; trades execute at the
 *                 RESTING (maker) order's price; partial fills; remainder rests or is cancelled
 *                 by its time-in-force; FOK checks liquidity first; cancel; amend
 *     [engine]    MatchingEngine: one SINGLE-THREADED sequencer per symbol (LMAX style) fed by
 *                 a queue - deterministic, no locks in the matching code
 *     [events]    TradeListener / market data (best bid/ask, depth)
 *     [demo]      price-time priority, partial fills, market/IOC/FOK, cancel/amend, a random
 *                 cross-check against a naive reference matcher, and concurrent submitters
 *
 *  PRICE-TIME PRIORITY: best price first; at the same price, earliest order first.
 *  "Best" = highest bid / lowest ask. A buy at 101 meets asks 100 (A, then B) and 101:
 *  it fills A@100, then B@100, then 101 - each at the RESTING order's price.
 *
 *  Prices and quantities are longs (paise / shares): exact, fast, comparable.
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public class OrderMatchingEngine {

    // =====================================================================
    // [model]
    // =====================================================================

    enum Side { BUY, SELL; Side opposite() { return this == BUY ? SELL : BUY; } }
    enum OrderType { LIMIT, MARKET }
    enum TimeInForce { GTC, IOC, FOK }        // good-till-cancel, immediate-or-cancel, fill-or-kill
    enum OrderStatus { NEW, PARTIALLY_FILLED, FILLED, CANCELLED, REJECTED }

    static final class Order {
        final String id, trader, symbol;
        final Side side;
        final OrderType type;
        final TimeInForce tif;
        final long price;                 // ignored for MARKET
        long qty;                         // original quantity (amend down reduces it)
        long filled;
        long seq;                         // arrival sequence = time priority
        OrderStatus status = OrderStatus.NEW;

        Order(String id, String trader, String symbol, Side side, OrderType type, TimeInForce tif, long price, long qty) {
            if (qty <= 0) throw new IllegalArgumentException("qty must be positive");
            if (type == OrderType.LIMIT && price <= 0) throw new IllegalArgumentException("limit price must be positive");
            this.id = id; this.trader = trader; this.symbol = symbol; this.side = side; this.type = type; this.tif = tif; this.price = price; this.qty = qty;
        }
        long remaining() { return qty - filled; }

        /** Can this incoming order trade at `restingPrice`? */
        boolean crosses(long restingPrice) {
            if (type == OrderType.MARKET) return true;
            return side == Side.BUY ? price >= restingPrice : price <= restingPrice;
        }
    }

    record Trade(long seq, String symbol, String buyOrder, String sellOrder, long price, long qty, String aggressor) {
        @Override public String toString() { return qty + "@" + price; }
    }

    // =====================================================================
    // [book]
    // =====================================================================

    static final class OrderBook {
        final String symbol;
        // price -> FIFO queue. TreeMap gives O(log P) best-price lookup and ordered depth.
        final TreeMap<Long, ArrayDeque<Order>> bids = new TreeMap<>(Comparator.reverseOrder());
        final TreeMap<Long, ArrayDeque<Order>> asks = new TreeMap<>();
        final Map<String, Order> resting = new HashMap<>();                  // id -> order, for cancel/amend
        private long seq, tradeSeq;
        final List<Trade> trades = new ArrayList<>();
        private final List<Consumer<Trade>> listeners;

        OrderBook(String symbol, List<Consumer<Trade>> listeners) { this.symbol = symbol; this.listeners = listeners; }

        private TreeMap<Long, ArrayDeque<Order>> sideOf(Side s) { return s == Side.BUY ? bids : asks; }

        /** The whole matching algorithm. Returns the trades this order produced. */
        List<Trade> submit(Order o) {
            o.seq = ++seq;
            TreeMap<Long, ArrayDeque<Order>> opposite = sideOf(o.side.opposite());

            if (o.tif == TimeInForce.FOK && available(o, opposite) < o.qty) {   // all or nothing: check BEFORE touching the book
                o.status = OrderStatus.CANCELLED;
                return List.of();
            }

            List<Trade> produced = new ArrayList<>();
            while (o.remaining() > 0 && !opposite.isEmpty()) {
                Map.Entry<Long, ArrayDeque<Order>> best = opposite.firstEntry();  // best price on the other side
                if (!o.crosses(best.getKey())) break;
                ArrayDeque<Order> level = best.getValue();
                Order maker = level.peekFirst();                                   // earliest at that price
                long q = Math.min(o.remaining(), maker.remaining());
                Trade t = new Trade(++tradeSeq, symbol, o.side == Side.BUY ? o.id : maker.id, o.side == Side.SELL ? o.id : maker.id, best.getKey(), q, o.id);
                o.filled += q; maker.filled += q;
                maker.status = maker.remaining() == 0 ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
                if (maker.remaining() == 0) { level.pollFirst(); resting.remove(maker.id); }
                if (level.isEmpty()) opposite.pollFirstEntry();                    // price level exhausted
                produced.add(t);
                trades.add(t);
                listeners.forEach(l -> l.accept(t));
            }

            if (o.remaining() == 0) o.status = OrderStatus.FILLED;
            else if (o.type == OrderType.MARKET || o.tif != TimeInForce.GTC) o.status = OrderStatus.CANCELLED;   // never rests
            else {                                                                   // GTC limit: rest the remainder
                o.status = o.filled > 0 ? OrderStatus.PARTIALLY_FILLED : OrderStatus.NEW;
                sideOf(o.side).computeIfAbsent(o.price, p -> new ArrayDeque<>()).addLast(o);
                resting.put(o.id, o);
            }
            return produced;
        }

        /** Quantity the order could fill right now (for FOK). */
        private long available(Order o, TreeMap<Long, ArrayDeque<Order>> opposite) {
            long sum = 0;
            for (var level : opposite.entrySet()) {
                if (!o.crosses(level.getKey())) break;
                for (Order r : level.getValue()) { sum += r.remaining(); if (sum >= o.qty) return sum; }
            }
            return sum;
        }

        boolean cancel(String orderId) {
            Order o = resting.remove(orderId);
            if (o == null) return false;
            TreeMap<Long, ArrayDeque<Order>> book = sideOf(o.side);
            ArrayDeque<Order> level = book.get(o.price);
            level.remove(o);                                                         // O(level size); see README for O(1)
            if (level.isEmpty()) book.remove(o.price);
            o.status = OrderStatus.CANCELLED;
            return true;
        }

        /**
         * Amend rules used by real exchanges: REDUCING quantity keeps time priority (it can only
         * help others); changing the PRICE or INCREASING quantity = cancel + new order (loses priority).
         */
        Order amend(String orderId, long newPrice, long newQty) {
            Order o = resting.get(orderId);
            if (o == null) throw new IllegalStateException("not resting: " + orderId);
            if (newPrice == o.price && newQty < o.qty && newQty > o.filled) { o.qty = newQty; return o; }
            cancel(orderId);
            Order replacement = new Order(o.id + "'", o.trader, o.symbol, o.side, OrderType.LIMIT, TimeInForce.GTC, newPrice, newQty - o.filled);
            submit(replacement);
            return replacement;
        }

        OptionalLong bestBid() { return bids.isEmpty() ? OptionalLong.empty() : OptionalLong.of(bids.firstKey()); }
        OptionalLong bestAsk() { return asks.isEmpty() ? OptionalLong.empty() : OptionalLong.of(asks.firstKey()); }

        /** Market data: aggregated quantity per price, top N levels. */
        String depth(int n) {
            StringBuilder sb = new StringBuilder("bids ");
            bids.entrySet().stream().limit(n).forEach(e -> sb.append(e.getValue().stream().mapToLong(Order::remaining).sum()).append('@').append(e.getKey()).append(' '));
            sb.append("| asks ");
            asks.entrySet().stream().limit(n).forEach(e -> sb.append(e.getValue().stream().mapToLong(Order::remaining).sum()).append('@').append(e.getKey()).append(' '));
            return sb.toString().trim();
        }
    }

    // =====================================================================
    // [engine] single writer per symbol
    // =====================================================================

    /**
     * Each symbol gets its own single-threaded executor. All commands for a symbol are
     * applied in arrival order by one thread: the book needs NO locks, and the result is
     * deterministic (replaying the same input log gives the same trades). Symbols run in
     * parallel. This is the LMAX / exchange "sequencer" design.
     */
    static final class MatchingEngine {
        private final Map<String, OrderBook> books = new ConcurrentHashMap<>();
        private final Map<String, ExecutorService> sequencers = new ConcurrentHashMap<>();
        private final List<Consumer<Trade>> listeners = new CopyOnWriteArrayList<>();
        private final AtomicLong ids = new AtomicLong();

        void addTradeListener(Consumer<Trade> l) { listeners.add(l); }

        private ExecutorService seq(String symbol) {
            return sequencers.computeIfAbsent(symbol, s -> Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "seq-" + s); t.setDaemon(true); return t; }));
        }
        private OrderBook book(String symbol) { return books.computeIfAbsent(symbol, s -> new OrderBook(s, listeners)); }

        Future<List<Trade>> submit(String trader, String symbol, Side side, OrderType type, TimeInForce tif, long price, long qty) {
            Order o = new Order("O" + ids.incrementAndGet(), trader, symbol, side, type, tif, price, qty);   // validation on the caller thread
            return seq(symbol).submit(() -> book(symbol).submit(o));
        }

        Future<Boolean> cancel(String symbol, String orderId) { return seq(symbol).submit(() -> book(symbol).cancel(orderId)); }

        /** Run a read on the sequencer thread too, so it sees a consistent book. */
        <T> T query(String symbol, Function<OrderBook, T> f) throws Exception { return seq(symbol).submit(() -> f.apply(book(symbol))).get(); }

        void shutdown() { sequencers.values().forEach(ExecutorService::shutdown); }
    }

    // =====================================================================
    // [reference] a deliberately naive matcher used to cross-check the real one
    // =====================================================================

    /** Keeps resting orders in a plain list and sorts it every time. Slow, obviously correct. */
    static List<Trade> naive(List<Order> input) {
        List<Order> book = new ArrayList<>();
        List<Trade> out = new ArrayList<>();
        long seq = 0, tseq = 0;
        for (Order in : input) {
            in.seq = ++seq;
            List<Order> opp = new ArrayList<>(book.stream().filter(r -> r.side != in.side).toList());
            opp.sort(Comparator.comparingLong((Order r) -> in.side == Side.BUY ? r.price : -r.price).thenComparingLong(r -> r.seq));
            if (in.tif == TimeInForce.FOK) {
                long avail = opp.stream().filter(r -> in.crosses(r.price)).mapToLong(Order::remaining).sum();
                if (avail < in.qty) continue;
            }
            for (Order r : opp) {
                if (in.remaining() == 0 || !in.crosses(r.price)) break;
                long q = Math.min(in.remaining(), r.remaining());
                in.filled += q; r.filled += q;
                out.add(new Trade(++tseq, "X", in.side == Side.BUY ? in.id : r.id, in.side == Side.SELL ? in.id : r.id, r.price, q, in.id));
                if (r.remaining() == 0) book.remove(r);
            }
            if (in.remaining() > 0 && in.type == OrderType.LIMIT && in.tif == TimeInForce.GTC) book.add(in);
        }
        return out;
    }

    // =====================================================================
    // [demo]
    // =====================================================================

    static Order lim(String id, Side s, long p, long q) { return new Order(id, "t-" + id, "X", s, OrderType.LIMIT, TimeInForce.GTC, p, q); }

    public static void main(String[] args) throws Exception {
        List<Trade> tape = new ArrayList<>();
        OrderBook b = new OrderBook("INFY", List.of(tape::add));

        System.out.println("1. Price-time priority and maker price");
        b.submit(lim("A", Side.SELL, 100, 50));
        b.submit(lim("B", Side.SELL, 100, 50));                                  // same price, later
        b.submit(lim("C", Side.SELL, 101, 100));
        b.submit(lim("D", Side.SELL, 99, 10));                                   // better price, arrived last
        check("book", "bids | asks 10@99 100@100 100@101", b.depth(5));
        List<Trade> t = b.submit(lim("E", Side.BUY, 100, 80));                   // buyer willing to pay 100
        check("fills best price first, then FIFO at 100", "[10@99, 50@100, 20@100]", t);
        check("who traded: D, A, B", "[D, A, B]", t.stream().map(Trade::sellOrder).toList());
        check("B partially filled, keeps its place", "PARTIALLY_FILLED/30", b.resting.get("B").status + "/" + b.resting.get("B").remaining());
        check("best bid / ask (book never crossed)", "none/100", (b.bestBid().isPresent() ? b.bestBid().getAsLong() : "none") + "/" + b.bestAsk().getAsLong());

        System.out.println("2. A limit order that doesn't fully cross rests the remainder");
        t = b.submit(lim("F", Side.BUY, 100, 50));
        check("takes B's 30, rests 20 @100", "[30@100] / bids 20@100 | asks 100@101", t + " / " + b.depth(5));

        System.out.println("3. Market, IOC and FOK");
        Order mkt = new Order("G", "g", "INFY", Side.BUY, OrderType.MARKET, TimeInForce.IOC, 0, 150);
        t = b.submit(mkt);
        check("market buy sweeps what exists, never rests", "[100@101] CANCELLED filled=100", t + " " + mkt.status + " filled=" + mkt.filled);
        b.submit(lim("H", Side.SELL, 105, 40));
        Order ioc = new Order("I", "i", "INFY", Side.BUY, OrderType.LIMIT, TimeInForce.IOC, 105, 60);
        check("IOC fills 40, cancels the rest", "[40@105] CANCELLED", b.submit(ioc) + " " + ioc.status);
        b.submit(lim("J", Side.SELL, 106, 30));
        Order fokFail = new Order("K", "k", "INFY", Side.BUY, OrderType.LIMIT, TimeInForce.FOK, 106, 50);
        check("FOK: not enough -> nothing traded", "[] CANCELLED 30@106", b.submit(fokFail) + " " + fokFail.status + " " + b.depth(5).split("asks ")[1]);
        Order fokOk = new Order("L", "l", "INFY", Side.BUY, OrderType.LIMIT, TimeInForce.FOK, 106, 30);
        check("FOK: exactly enough -> filled", "[30@106] FILLED", b.submit(fokOk) + " " + fokOk.status);

        System.out.println("4. Cancel and amend");
        b.submit(lim("M", Side.SELL, 110, 10));
        b.submit(lim("N", Side.SELL, 110, 10));
        check("cancel", true, b.cancel("M"));
        check("cancel unknown/filled order", false, b.cancel("A"));
        b.submit(lim("P", Side.SELL, 110, 10));
        b.amend("N", 110, 5);                                                    // reduce qty: keeps priority
        check("qty down keeps the queue position", "[N, P]", b.asks.get(110L).stream().map(o -> o.id).toList());
        Order moved = b.amend("N", 110, 20);                                     // increase qty: loses priority
        check("qty up goes to the back", "[P, N']", b.asks.get(110L).stream().map(o -> o.id).toList());
        expect("invalid orders rejected at the door", () -> lim("Z", Side.BUY, 0, 10));

        System.out.println("5. Cross-check against a naive matcher on 300 random sessions");
        Random rnd = new Random(7);
        boolean same = true, neverCrossed = true, conserved = true;
        for (int s = 0; s < 300; s++) {
            List<Order> a = new ArrayList<>(), c = new ArrayList<>();
            for (int i = 0; i < 60; i++) {
                Side side = rnd.nextBoolean() ? Side.BUY : Side.SELL;
                int kind = rnd.nextInt(10);
                OrderType type = kind == 0 ? OrderType.MARKET : OrderType.LIMIT;
                TimeInForce tif = kind == 0 || kind == 1 ? TimeInForce.IOC : kind == 2 ? TimeInForce.FOK : TimeInForce.GTC;
                long price = 95 + rnd.nextInt(11), qty = 1 + rnd.nextInt(20);
                a.add(new Order("o" + i, "t", "X", side, type, tif, price, qty));
                c.add(new Order("o" + i, "t", "X", side, type, tif, price, qty));
            }
            OrderBook book = new OrderBook("X", List.of());
            List<Trade> real = new ArrayList<>();
            for (Order o : a) {
                real.addAll(book.submit(o));
                if (book.bestBid().isPresent() && book.bestAsk().isPresent() && book.bestBid().getAsLong() >= book.bestAsk().getAsLong()) neverCrossed = false;
            }
            same &= real.toString().equals(naive(c).toString())
                    && real.stream().map(x -> x.buyOrder() + x.sellOrder()).toList().equals(naive(copy(c)).stream().map(x -> x.buyOrder() + x.sellOrder()).toList());
            long bought = real.stream().mapToLong(Trade::qty).sum();
            long filledBuys = a.stream().filter(o -> o.side == Side.BUY).mapToLong(o -> o.filled).sum();
            long filledSells = a.stream().filter(o -> o.side == Side.SELL).mapToLong(o -> o.filled).sum();
            conserved &= bought == filledBuys && bought == filledSells && a.stream().allMatch(o -> o.filled <= o.qty);
        }
        check("identical trades to the reference", true, same);
        check("book never crossed (best bid < best ask)", true, neverCrossed);
        check("bought == sold == traded, no overfill", true, conserved);

        System.out.println("6. Engine: 8 threads submit to 2 symbols; each symbol is sequenced on one thread");
        MatchingEngine engine = new MatchingEngine();
        Map<String, AtomicLong> tradedQty = new ConcurrentHashMap<>();
        engine.addTradeListener(tr -> tradedQty.computeIfAbsent(tr.symbol(), k -> new AtomicLong()).addAndGet(tr.qty()));
        ExecutorService clients = Executors.newFixedThreadPool(8);
        List<Future<?>> fs = new ArrayList<>();
        for (int c = 0; c < 8; c++) {
            int id = c;
            fs.add(clients.submit(() -> {
                ThreadLocalRandom r = ThreadLocalRandom.current();
                for (int i = 0; i < 2000; i++) {
                    String sym = r.nextBoolean() ? "TCS" : "INFY";
                    engine.submit("trader" + id, sym, r.nextBoolean() ? Side.BUY : Side.SELL, OrderType.LIMIT, TimeInForce.GTC, 95 + r.nextInt(11), 1 + r.nextInt(50));
                }
                return null;
            }));
        }
        for (Future<?> f : fs) f.get();
        clients.shutdown();
        for (String sym : List.of("TCS", "INFY")) {
            boolean ok = engine.query(sym, bk -> {
                boolean notCrossed = bk.bestBid().isEmpty() || bk.bestAsk().isEmpty() || bk.bestBid().getAsLong() < bk.bestAsk().getAsLong();
                return notCrossed && bk.trades.stream().mapToLong(Trade::qty).sum() == tradedQty.get(sym).get();
            });
            check(sym + ": consistent book, trades == listener feed", true, ok);
        }
        System.out.println("   depth INFY: " + engine.query("INFY", bk -> bk.depth(3)));
        engine.shutdown();
        summary();
    }

    static List<Order> copy(List<Order> l) {
        List<Order> out = new ArrayList<>();
        for (Order o : l) out.add(new Order(o.id, o.trader, o.symbol, o.side, o.type, o.tif, o.price, o.qty));
        return out;
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
