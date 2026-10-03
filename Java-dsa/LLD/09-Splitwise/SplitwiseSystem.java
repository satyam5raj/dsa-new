/*
 * ============================================================================
 *  LLD 09 : SPLITWISE (expense sharing + debt simplification)
 * ============================================================================
 *  Run:  java SplitwiseSystem.java          (design notes: README.md)
 *
 *     [model]     User, Group, Expense (immutable), Split, SplitType, Transfer
 *     [split]     SplitStrategy: Equal, Exact, Percent, Share - each returns exact paise
 *                 (rounding remainder distributed, so parts ALWAYS add up to the total)
 *     [ledger]    Append-only ledger per group; balances are DERIVED (net per user)
 *     [simplify]  DebtSimplifier: Greedy (max creditor vs max debtor, <= n-1 transfers)
 *                 vs Optimal (backtracking, minimum transfers - LeetCode 465, NP-hard)
 *     [service]   ExpenseService: add expense, settle up, delete (as a reversal), balances,
 *                 simplified plan; ExpenseListener observers
 *     [demo]      rounding, validation, balances, simplification, random checks, concurrency
 *
 *  THE INVARIANT THAT MAKES IT CORRECT: in every group, the sum of all net balances is 0.
 *    Every expense adds +amount to the payer and -share to each participant; the shares add
 *    up EXACTLY to the amount (integer paise, remainder distributed) -> the sum stays 0.
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;

public class SplitwiseSystem {

    // =====================================================================
    // [model]
    // =====================================================================

    record User(String id, String name) {}

    enum SplitType { EQUAL, EXACT, PERCENT, SHARE }

    /** One participant's share of an expense, in paise. */
    record Split(String userId, long amountPaise) {}

    /** Immutable once recorded. Editing = reversal + a new expense (keeps an audit trail). */
    record Expense(String id, String groupId, String description, String paidBy, long amountPaise, List<Split> splits, boolean reversal) {}

    /** "from pays to `amount`" - an instruction produced by settlement or simplification. */
    record Transfer(String from, String to, long amountPaise) {
        @Override public String toString() { return from + "->" + to + ":" + amountPaise; }
    }

    // =====================================================================
    // [split] strategies
    // =====================================================================

    /**
     * STRATEGY: turns (total, participants, parameters) into exact paise per person.
     * `params` meaning depends on the type: EXACT = amounts, PERCENT = percents, SHARE = weights.
     */
    interface SplitStrategy {
        List<Split> split(long totalPaise, List<String> users, List<Long> params);
    }

    /** Distribute `total` proportionally to `weights` with NO paise lost (largest remainder method). */
    static List<Split> proportional(long total, List<String> users, List<Long> weights) {
        long wSum = weights.stream().mapToLong(Long::longValue).sum();
        if (wSum <= 0) throw new IllegalArgumentException("weights must be positive");
        long[] parts = new long[users.size()];
        long[] remainders = new long[users.size()];
        long given = 0;
        for (int i = 0; i < users.size(); i++) {
            parts[i] = total * weights.get(i) / wSum;                     // floor
            remainders[i] = total * weights.get(i) % wSum;
            given += parts[i];
        }
        Integer[] order = new Integer[users.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> remainders[b] != remainders[a] ? Long.compare(remainders[b], remainders[a]) : Integer.compare(a, b));
        for (int k = 0; given < total; k++, given++) parts[order[k]]++;      // hand out leftover paise
        List<Split> out = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) out.add(new Split(users.get(i), parts[i]));
        return out;
    }

    static class EqualSplit implements SplitStrategy {
        public List<Split> split(long total, List<String> users, List<Long> p) {
            return proportional(total, users, Collections.nCopies(users.size(), 1L));   // Rs 100 / 3 = 33.34, 33.33, 33.33
        }
    }

    static class ExactSplit implements SplitStrategy {
        public List<Split> split(long total, List<String> users, List<Long> amounts) {
            if (amounts.size() != users.size()) throw new IllegalArgumentException("one amount per user");
            if (amounts.stream().mapToLong(Long::longValue).sum() != total) throw new IllegalArgumentException("exact amounts must add up to the total");
            List<Split> out = new ArrayList<>();
            for (int i = 0; i < users.size(); i++) out.add(new Split(users.get(i), amounts.get(i)));
            return out;
        }
    }

    static class PercentSplit implements SplitStrategy {
        public List<Split> split(long total, List<String> users, List<Long> percents) {
            if (percents.stream().mapToLong(Long::longValue).sum() != 100) throw new IllegalArgumentException("percents must add up to 100");
            return proportional(total, users, percents);
        }
    }

    static class ShareSplit implements SplitStrategy {                    // "2 shares for the couple, 1 for each single"
        public List<Split> split(long total, List<String> users, List<Long> shares) { return proportional(total, users, shares); }
    }

    /** FACTORY: SplitType -> strategy. New type = new class + one map entry. */
    static final Map<SplitType, SplitStrategy> STRATEGIES = Map.of(
            SplitType.EQUAL, new EqualSplit(), SplitType.EXACT, new ExactSplit(),
            SplitType.PERCENT, new PercentSplit(), SplitType.SHARE, new ShareSplit());

    // =====================================================================
    // [simplify]
    // =====================================================================

    interface DebtSimplifier { List<Transfer> simplify(Map<String, Long> net); }

    /**
     * GREEDY: repeatedly match the biggest creditor with the biggest debtor; settle the
     * smaller of the two amounts. Each step zeroes at least one person -> at most n-1 transfers.
     * O(n log n) with two heaps. Not always minimal, but what Splitwise effectively does.
     */
    static class GreedySimplifier implements DebtSimplifier {
        public List<Transfer> simplify(Map<String, Long> net) {
            PriorityQueue<Map.Entry<String, Long>> creditors = new PriorityQueue<>((a, b) -> Long.compare(b.getValue(), a.getValue()));
            PriorityQueue<Map.Entry<String, Long>> debtors = new PriorityQueue<>((a, b) -> Long.compare(a.getValue(), b.getValue()));
            new TreeMap<>(net).forEach((u, v) -> {                          // TreeMap: deterministic order
                if (v > 0) creditors.add(new AbstractMap.SimpleEntry<>(u, v));
                else if (v < 0) debtors.add(new AbstractMap.SimpleEntry<>(u, v));
            });
            List<Transfer> out = new ArrayList<>();
            while (!creditors.isEmpty() && !debtors.isEmpty()) {
                var c = creditors.poll();
                var d = debtors.poll();
                long amt = Math.min(c.getValue(), -d.getValue());
                out.add(new Transfer(d.getKey(), c.getKey(), amt));
                if (c.getValue() - amt > 0) creditors.add(new AbstractMap.SimpleEntry<>(c.getKey(), c.getValue() - amt));
                if (d.getValue() + amt < 0) debtors.add(new AbstractMap.SimpleEntry<>(d.getKey(), d.getValue() + amt));
            }
            return out;
        }
    }

    /**
     * OPTIMAL (minimum number of transfers): NP-hard in general. Backtracking over the
     * non-zero balances: settle balance[i] against each later balance of the opposite sign.
     * Fine for small groups (n <= ~15); shows WHY greedy is the production choice.
     * Here we only compute the COUNT (enough to compare); LeetCode 465.
     */
    static int optimalTransferCount(Map<String, Long> net) {
        long[] b = net.values().stream().mapToLong(Long::longValue).filter(v -> v != 0).toArray();
        return dfs(b, 0);
    }

    private static int dfs(long[] b, int i) {
        while (i < b.length && b[i] == 0) i++;
        if (i == b.length) return 0;
        int best = Integer.MAX_VALUE;
        for (int j = i + 1; j < b.length; j++) {
            if (b[j] == 0 || (b[i] > 0) == (b[j] > 0)) continue;            // need the opposite sign
            b[j] += b[i];                                                  // i settles entirely with j
            best = Math.min(best, 1 + dfs(b, i + 1));
            b[j] -= b[i];
            if (b[j] + b[i] == 0) break;                                    // perfect match: can't do better
        }
        return best;
    }

    // =====================================================================
    // [service]
    // =====================================================================

    interface ExpenseListener { void onExpense(Expense e); }

    static class Group {
        final String id, name;
        final Set<String> members = ConcurrentHashMap.newKeySet();
        final List<Expense> ledger = new ArrayList<>();                     // append-only, guarded by lock
        final Map<String, Long> net = new HashMap<>();                      // derived cache, guarded by lock
        final ReentrantLock lock = new ReentrantLock();
        Group(String id, String name) { this.id = id; this.name = name; }
    }

    static class ExpenseService {
        private final Map<String, User> users = new ConcurrentHashMap<>();
        private final Map<String, Group> groups = new ConcurrentHashMap<>();
        private final List<ExpenseListener> listeners = new CopyOnWriteArrayList<>();
        private final DebtSimplifier simplifier;
        private final AtomicLong seq = new AtomicLong();

        ExpenseService(DebtSimplifier simplifier) { this.simplifier = simplifier; }

        void addUser(User u) { users.put(u.id(), u); }
        void addListener(ExpenseListener l) { listeners.add(l); }
        Group createGroup(String id, String name, String... memberIds) {
            Group g = new Group(id, name);
            for (String m : memberIds) { requireUser(m); g.members.add(m); }
            groups.put(id, g);
            return g;
        }

        Expense addExpense(String groupId, String description, String paidBy, long amountPaise, SplitType type, List<String> participants, List<Long> params) {
            Group g = group(groupId);
            if (amountPaise <= 0) throw new IllegalArgumentException("amount must be positive");
            if (!g.members.contains(paidBy)) throw new IllegalArgumentException(paidBy + " is not in the group");
            for (String p : participants) if (!g.members.contains(p)) throw new IllegalArgumentException(p + " is not in the group");
            List<Split> splits = STRATEGIES.get(type).split(amountPaise, participants, params);
            Expense e = new Expense("E" + seq.incrementAndGet(), groupId, description, paidBy, amountPaise, List.copyOf(splits), false);
            record(g, e);
            return e;
        }

        /** Settle up = an expense paid by `from` whose only participant is `to`. Same ledger, same invariant. */
        Expense settle(String groupId, String from, String to, long amountPaise) {
            Group g = group(groupId);
            Expense e = new Expense("E" + seq.incrementAndGet(), groupId, "settle-up", from, amountPaise, List.of(new Split(to, amountPaise)), false);
            record(g, e);
            return e;
        }

        /** Never mutate history: append the NEGATION of the expense. */
        Expense delete(String groupId, String expenseId) {
            Group g = group(groupId);
            g.lock.lock();
            try {
                Expense orig = g.ledger.stream().filter(x -> x.id().equals(expenseId)).findFirst().orElseThrow(() -> new IllegalArgumentException("No expense " + expenseId));
                if (g.ledger.stream().anyMatch(x -> x.reversal() && x.description().equals("reversal of " + expenseId)))
                    throw new IllegalStateException("Already deleted");
                List<Split> neg = orig.splits().stream().map(s -> new Split(s.userId(), -s.amountPaise())).toList();
                Expense rev = new Expense("E" + seq.incrementAndGet(), groupId, "reversal of " + expenseId, orig.paidBy(), -orig.amountPaise(), neg, true);
                apply(g, rev);
                return rev;
            } finally { g.lock.unlock(); }
        }

        private void record(Group g, Expense e) {
            g.lock.lock();                                                  // ledger + balances change together
            try { apply(g, e); } finally { g.lock.unlock(); }
            listeners.forEach(l -> l.onExpense(e));                         // notify outside the lock
        }

        private void apply(Group g, Expense e) {
            g.ledger.add(e);
            g.net.merge(e.paidBy(), e.amountPaise(), Long::sum);           // payer is owed the full amount...
            for (Split s : e.splits()) g.net.merge(s.userId(), -s.amountPaise(), Long::sum);   // ...minus everyone's share
        }

        /** Net per user: + means others owe them, - means they owe. Sum is always 0. */
        Map<String, Long> balances(String groupId) {
            Group g = group(groupId);
            g.lock.lock();
            try {
                Map<String, Long> m = new TreeMap<>();
                g.net.forEach((u, v) -> { if (v != 0) m.put(u, v); });
                return m;
            } finally { g.lock.unlock(); }
        }

        List<Transfer> settlementPlan(String groupId) { return simplifier.simplify(balances(groupId)); }

        /** Rebuild balances from scratch by replaying the ledger - proves the cache is right. */
        Map<String, Long> replay(String groupId) {
            Group g = group(groupId);
            Map<String, Long> m = new TreeMap<>();
            g.lock.lock();
            try {
                for (Expense e : g.ledger) { m.merge(e.paidBy(), e.amountPaise(), Long::sum); for (Split s : e.splits()) m.merge(s.userId(), -s.amountPaise(), Long::sum); }
            } finally { g.lock.unlock(); }
            m.values().removeIf(v -> v == 0);
            return m;
        }

        private Group group(String id) { Group g = groups.get(id); if (g == null) throw new IllegalArgumentException("No group " + id); return g; }
        private void requireUser(String id) { if (!users.containsKey(id)) throw new IllegalArgumentException("No user " + id); }
    }

    // =====================================================================
    // [demo]
    // =====================================================================

    static Map<String, Long> applyTransfers(Map<String, Long> net, List<Transfer> ts) {
        Map<String, Long> m = new HashMap<>(net);
        for (Transfer t : ts) { m.merge(t.from(), t.amountPaise(), Long::sum); m.merge(t.to(), -t.amountPaise(), Long::sum); }
        m.values().removeIf(v -> v == 0);
        return m;
    }

    public static void main(String[] args) throws Exception {
        ExpenseService svc = new ExpenseService(new GreedySimplifier());
        for (String u : List.of("A", "B", "C", "D")) svc.addUser(new User(u, u));
        svc.createGroup("goa", "Goa trip", "A", "B", "C", "D");
        List<String> feed = new CopyOnWriteArrayList<>();
        svc.addListener(e -> feed.add(e.description()));

        System.out.println("1. Split strategies with exact paise");
        check("Rs 100 / 3 equal", "[Split[userId=A, amountPaise=3334], Split[userId=B, amountPaise=3333], Split[userId=C, amountPaise=3333]]",
                new EqualSplit().split(10_000, List.of("A", "B", "C"), null));
        check("percent 50/30/20 of Rs 99.99 (largest remainders get the extra paise)", "[4999, 3000, 2000]",
                new PercentSplit().split(9_999, List.of("A", "B", "C"), List.of(50L, 30L, 20L)).stream().map(Split::amountPaise).toList());
        check("shares 2:1:1 of Rs 1000", "[50000, 25000, 25000]",
                new ShareSplit().split(100_000, List.of("A", "B", "C"), List.of(2L, 1L, 1L)).stream().map(Split::amountPaise).toList());
        expect("exact must add up", () -> new ExactSplit().split(1000, List.of("A", "B"), List.of(400L, 500L)));
        expect("percent must be 100", () -> new PercentSplit().split(1000, List.of("A", "B"), List.of(40L, 50L)));
        Random rnd = new Random(1);
        boolean exact = true;
        for (int t = 0; t < 2000; t++) {
            long total = 1 + rnd.nextInt(1_000_000);
            int n = 1 + rnd.nextInt(9);
            List<String> us = new ArrayList<>(); List<Long> w = new ArrayList<>();
            for (int i = 0; i < n; i++) { us.add("u" + i); w.add(1L + rnd.nextInt(7)); }
            exact &= new ShareSplit().split(total, us, w).stream().mapToLong(Split::amountPaise).sum() == total;
        }
        check("2000 random splits sum exactly to the total", true, exact);

        System.out.println("2. Expenses and balances");
        svc.addExpense("goa", "hotel", "A", 1_200_000, SplitType.EQUAL, List.of("A", "B", "C", "D"), null);   // A paid 12,000
        svc.addExpense("goa", "dinner", "B", 400_000, SplitType.EXACT, List.of("A", "B", "C"), List.of(100_000L, 200_000L, 100_000L));
        svc.addExpense("goa", "cab", "C", 100_000, SplitType.PERCENT, List.of("C", "D"), List.of(40L, 60L));
        Map<String, Long> bal = svc.balances("goa");
        check("net balances", "{A=800000, B=-100000, C=-340000, D=-360000}", bal);
        check("sum of balances is 0", 0L, bal.values().stream().mapToLong(Long::longValue).sum());
        expect("outsider can't be in a split", () -> svc.addExpense("goa", "x", "A", 100, SplitType.EQUAL, List.of("A", "Z"), null));

        System.out.println("3. Simplified settlement plan");
        List<Transfer> plan = svc.settlementPlan("goa");
        check("greedy plan", "[D->A:360000, C->A:340000, B->A:100000]", plan);
        for (Transfer t : plan) svc.settle("goa", t.from(), t.to(), t.amountPaise());
        check("after settling: everyone is even", "{}", svc.balances("goa"));

        System.out.println("4. Delete = append a reversal (history is never rewritten)");
        Expense oops = svc.addExpense("goa", "wrong entry", "D", 50_000, SplitType.EQUAL, List.of("A", "D"), null);
        check("balance after the mistake", "{A=-25000, D=25000}", svc.balances("goa"));
        svc.delete("goa", oops.id());
        check("balance restored", "{}", svc.balances("goa"));
        expect("can't delete twice", () -> svc.delete("goa", oops.id()));
        check("ledger replay == cached balances", svc.replay("goa"), svc.balances("goa"));

        System.out.println("5. Greedy vs optimal number of transfers");
        // Greedy pairs the biggest creditor (A=5) with the biggest debtor (C=-4), breaking the
        // perfect pair B=4 / C=-4 that the optimal plan uses.
        Map<String, Long> tricky = Map.of("A", 5L, "B", 4L, "C", -4L, "D", -3L, "E", -2L);
        check("greedy needs 4", 4, new GreedySimplifier().simplify(tricky).size());
        check("optimal is 3 (C->B, D->A, E->A)", 3, optimalTransferCount(tricky));
        boolean settlesAll = true, boundOk = true, optimalNotWorse = true;
        for (int t = 0; t < 300; t++) {
            int n = 2 + rnd.nextInt(7);
            Map<String, Long> net = new HashMap<>();
            long sum = 0;
            for (int i = 0; i < n - 1; i++) { long v = rnd.nextInt(2001) - 1000; net.put("p" + i, v); sum += v; }
            net.put("p" + (n - 1), -sum);                                  // force sum 0
            List<Transfer> g = new GreedySimplifier().simplify(net);
            settlesAll &= applyTransfers(net, g).isEmpty();
            long nonZero = net.values().stream().filter(v -> v != 0).count();
            boundOk &= g.size() <= Math.max(0, nonZero - 1);
            optimalNotWorse &= optimalTransferCount(net) <= g.size();
        }
        check("300 random: greedy plan settles everyone", true, settlesAll);
        check("greedy uses <= n-1 transfers", true, boundOk);
        check("optimal <= greedy", true, optimalNotWorse);

        System.out.println("6. Concurrency: 16 threads add 4,000 expenses to one group");
        ExpenseService c = new ExpenseService(new GreedySimplifier());
        for (int i = 0; i < 6; i++) c.addUser(new User("u" + i, "U" + i));
        c.createGroup("g", "flat", "u0", "u1", "u2", "u3", "u4", "u5");
        ExecutorService pool = Executors.newFixedThreadPool(16);
        for (int t = 0; t < 16; t++) pool.submit(() -> {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < 250; i++) c.addExpense("g", "x", "u" + r.nextInt(6), 1 + r.nextInt(100_000), SplitType.EQUAL, List.of("u0", "u1", "u2", "u3", "u4", "u5"), null);
        });
        pool.shutdown();
        pool.awaitTermination(20, TimeUnit.SECONDS);
        Map<String, Long> cb = c.balances("g");
        check("sum still exactly 0", 0L, cb.values().stream().mapToLong(Long::longValue).sum());
        check("cached balances == ledger replay", c.replay("g"), cb);
        check("observer saw all 7 recorded expenses", 7, feed.size());
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
