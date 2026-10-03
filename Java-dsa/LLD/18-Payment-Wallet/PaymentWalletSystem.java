/*
 * ============================================================================
 *  LLD 18 : PAYMENT GATEWAY + WALLET  (double-entry ledger, idempotency, payment methods)
 * ============================================================================
 *  Run:  java PaymentWalletSystem.java          (design notes: README.md)
 *
 *     [money]     amounts in long PAISE, never double
 *     [ledger]    DOUBLE-ENTRY: every transaction is a JournalEntry whose Postings sum to ZERO.
 *                 Accounts: user wallets, merchants, and SYSTEM accounts (external funding,
 *                 platform fees, settlement). Balances are cached but always re-derivable.
 *     [methods]   PaymentMethod (Strategy) via a registry (Factory): UPI, CARD (auth + capture),
 *                 NETBANKING - each with its own fee rule and behaviour
 *     [intent]    PaymentIntent state machine: CREATED -> AUTHORIZED -> CAPTURED ->
 *                 PARTIALLY_REFUNDED / REFUNDED, or FAILED / VOIDED
 *     [service]   WalletService: top-up, pay merchant (with fee), P2P transfer, refund, all
 *                 IDEMPOTENT by client key; account locks in id order (no deadlock)
 *     [demo]      every flow + invariants + a concurrency storm with duplicate retries
 *
 *  WHY DOUBLE-ENTRY: with "balance -= x; other += x" a bug or crash between the two lines
 *  creates or destroys money silently. In double-entry, money never appears or vanishes:
 *  each entry is a set of debits/credits that MUST sum to 0, written atomically, so
 *       SUM(all account balances) == 0     ALWAYS (the external world is an account too)
 *  and any balance can be recomputed from the immutable journal (audit, reconciliation).
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;

public class PaymentWalletSystem {

    // =====================================================================
    // [ledger]
    // =====================================================================

    enum AccountType { USER_WALLET, MERCHANT, SYSTEM }

    static final class Account {
        final String id;
        final AccountType type;
        final boolean mayGoNegative;                     // only SYSTEM accounts (the outside world) may
        long balance;                                    // cache; guarded by lock
        final ReentrantLock lock = new ReentrantLock();
        Account(String id, AccountType type) { this.id = id; this.type = type; this.mayGoNegative = type == AccountType.SYSTEM; }
    }

    /** One line of a journal entry. Positive = credit to the account (money in), negative = debit. */
    record Posting(String accountId, long amount) {}

    /** Immutable, append-only. Sum of postings is ALWAYS zero. */
    record JournalEntry(String id, String kind, String reference, List<Posting> postings) {
        JournalEntry {
            if (postings.stream().mapToLong(Posting::amount).sum() != 0) throw new IllegalArgumentException("unbalanced entry " + kind);
            postings = List.copyOf(postings);
        }
    }

    static class LedgerException extends RuntimeException { LedgerException(String m) { super(m); } }

    static class Ledger {
        private final Map<String, Account> accounts = new ConcurrentHashMap<>();
        final List<JournalEntry> journal = new CopyOnWriteArrayList<>();
        private final AtomicLong seq = new AtomicLong();

        Account open(String id, AccountType t) {
            if (accounts.putIfAbsent(id, new Account(id, t)) != null) throw new LedgerException("Account " + id + " exists");
            return accounts.get(id);
        }

        /**
         * Post an entry atomically: lock every account involved IN SORTED ID ORDER (deadlock-free),
         * check no non-system account goes negative, apply, append to the journal.
         */
        JournalEntry post(String kind, String reference, List<Posting> postings) {
            JournalEntry draft = new JournalEntry("J" + seq.incrementAndGet(), kind, reference, postings);   // validates sum == 0
            List<Account> involved = postings.stream().map(p -> account(p.accountId())).distinct()
                    .sorted(Comparator.comparing(a -> a.id)).toList();
            involved.forEach(a -> a.lock.lock());
            try {
                Map<String, Long> delta = new HashMap<>();
                for (Posting p : postings) delta.merge(p.accountId(), p.amount(), Long::sum);
                for (Account a : involved)
                    if (!a.mayGoNegative && a.balance + delta.get(a.id) < 0) throw new LedgerException("Insufficient funds in " + a.id);
                for (Account a : involved) a.balance += delta.get(a.id);
                journal.add(draft);
                return draft;
            } finally {
                for (int i = involved.size() - 1; i >= 0; i--) involved.get(i).lock.unlock();
            }
        }

        long balance(String id) { Account a = account(id); a.lock.lock(); try { return a.balance; } finally { a.lock.unlock(); } }
        Account account(String id) { Account a = accounts.get(id); if (a == null) throw new LedgerException("No account " + id); return a; }

        /** Audit: recompute every balance from the journal and compare with the cache. */
        boolean verify() {
            Map<String, Long> fromJournal = new HashMap<>();
            for (JournalEntry e : journal) for (Posting p : e.postings()) fromJournal.merge(p.accountId(), p.amount(), Long::sum);
            for (Account a : accounts.values()) if (fromJournal.getOrDefault(a.id, 0L) != balance(a.id)) return false;
            return true;
        }

        long sumOfAllBalances() { return accounts.values().stream().mapToLong(a -> balance(a.id)).sum(); }
    }

    // =====================================================================
    // [methods]
    // =====================================================================

    enum MethodType { UPI, CARD, NETBANKING }

    record GatewayResult(boolean ok, String providerRef, String error) {}

    /** STRATEGY: how a method talks to its external rail, and what it costs. */
    interface PaymentMethod {
        MethodType type();
        long feePaise(long amount);
        boolean twoPhase();                                      // cards: authorize now, capture later
        GatewayResult authorize(String intentId, long amount);
        default GatewayResult capture(String intentId, long amount) { return new GatewayResult(true, "cap-" + intentId, null); }
        default void voidAuth(String intentId) {}
    }

    static class FakeRail {
        final Set<String> declineIntents = ConcurrentHashMap.newKeySet();
        final AtomicInteger calls = new AtomicInteger();
        GatewayResult call(String prefix, String intent) {
            calls.incrementAndGet();
            return declineIntents.contains(intent) ? new GatewayResult(false, null, "declined") : new GatewayResult(true, prefix + "-" + intent, null);
        }
    }

    record UpiMethod(FakeRail rail) implements PaymentMethod {
        public MethodType type() { return MethodType.UPI; }
        public long feePaise(long a) { return 0; }                                     // UPI: zero MDR
        public boolean twoPhase() { return false; }
        public GatewayResult authorize(String id, long a) { return rail.call("upi", id); }
    }

    record CardMethod(FakeRail rail) implements PaymentMethod {
        public MethodType type() { return MethodType.CARD; }
        public long feePaise(long a) { return Math.round(a * 0.02); }                  // 2%
        public boolean twoPhase() { return true; }
        public GatewayResult authorize(String id, long a) { return rail.call("auth", id); }
    }

    record NetBankingMethod(FakeRail rail) implements PaymentMethod {
        public MethodType type() { return MethodType.NETBANKING; }
        public long feePaise(long a) { return 1_000; }                                 // flat Rs 10
        public boolean twoPhase() { return false; }
        public GatewayResult authorize(String id, long a) { return rail.call("nb", id); }
    }

    // =====================================================================
    // [intent] state machine
    // =====================================================================

    enum IntentStatus {
        CREATED, AUTHORIZED, CAPTURED, PARTIALLY_REFUNDED, REFUNDED, FAILED, VOIDED;
        boolean canGoTo(IntentStatus n) {
            return switch (this) {
                case CREATED -> n == AUTHORIZED || n == CAPTURED || n == FAILED;
                case AUTHORIZED -> n == CAPTURED || n == VOIDED;
                case CAPTURED, PARTIALLY_REFUNDED -> n == PARTIALLY_REFUNDED || n == REFUNDED;
                default -> false;
            };
        }
    }

    static final class PaymentIntent {
        final String id, userId;
        final long amount;
        final PaymentMethod method;
        IntentStatus status = IntentStatus.CREATED;
        long refunded;
        String providerRef;
        PaymentIntent(String id, String userId, long amount, PaymentMethod m) { this.id = id; this.userId = userId; this.amount = amount; this.method = m; }
        synchronized void to(IntentStatus n) { if (!status.canGoTo(n)) throw new IllegalStateException(id + ": " + status + " -> " + n); status = n; }
    }

    // =====================================================================
    // [service]
    // =====================================================================

    record Result(boolean ok, String reference, String message) {}

    static class WalletService {
        static final String EXTERNAL = "SYS:external", FEES = "SYS:fees";
        final Ledger ledger = new Ledger();
        private final Map<MethodType, PaymentMethod> methods = new EnumMap<>(MethodType.class);
        private final Map<String, PaymentIntent> intents = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, CompletableFuture<Result>> idempotency = new ConcurrentHashMap<>();
        private final AtomicLong seq = new AtomicLong();
        private final int merchantFeeBps;                                               // platform fee on merchant payments

        WalletService(int merchantFeeBps, PaymentMethod... ms) {
            this.merchantFeeBps = merchantFeeBps;
            for (PaymentMethod m : ms) methods.put(m.type(), m);                       // registry = factory by type
            ledger.open(EXTERNAL, AccountType.SYSTEM);
            ledger.open(FEES, AccountType.SYSTEM);
        }

        void openWallet(String user) { ledger.open("W:" + user, AccountType.USER_WALLET); }
        void openMerchant(String m) { ledger.open("M:" + m, AccountType.MERCHANT); }
        long balance(String accountId) { return ledger.balance(accountId); }

        /**
         * IDEMPOTENCY: the first request with a key runs; concurrent or later duplicates get the
         * SAME result (even a failure) instead of running again. Stripe/Razorpay semantics.
         */
        private Result idempotent(String key, Supplier<Result> op) {
            CompletableFuture<Result> mine = new CompletableFuture<>();
            CompletableFuture<Result> prev = idempotency.putIfAbsent(key, mine);
            if (prev != null) return prev.join();
            Result r;
            try { r = op.get(); } catch (RuntimeException e) { r = new Result(false, null, e.getMessage()); }
            mine.complete(r);
            return r;
        }

        // ---- top up the wallet from an external method ----
        Result topUp(String key, String user, long amount, MethodType type) {
            return idempotent(key, () -> {
                if (amount <= 0) throw new LedgerException("amount must be positive");
                PaymentMethod m = method(type);
                PaymentIntent pi = new PaymentIntent("PI" + seq.incrementAndGet(), user, amount, m);
                intents.put(pi.id, pi);
                GatewayResult auth = m.authorize(pi.id, amount);
                if (!auth.ok()) { pi.to(IntentStatus.FAILED); return new Result(false, pi.id, auth.error()); }
                pi.providerRef = auth.providerRef();
                if (m.twoPhase()) { pi.to(IntentStatus.AUTHORIZED); return new Result(true, pi.id, "authorized; capture to credit"); }
                creditTopUp(pi);
                pi.to(IntentStatus.CAPTURED);
                return new Result(true, pi.id, "credited");
            });
        }

        /** Cards: money moves on CAPTURE (hotels/ride apps authorize first, capture the final amount). */
        Result capture(String intentId) {
            PaymentIntent pi = intent(intentId);
            synchronized (pi) {
                if (pi.status != IntentStatus.AUTHORIZED) return new Result(false, intentId, "not authorized: " + pi.status);
                GatewayResult cap = pi.method.capture(pi.id, pi.amount);
                if (!cap.ok()) return new Result(false, intentId, cap.error());
                creditTopUp(pi);
                pi.to(IntentStatus.CAPTURED);
                return new Result(true, intentId, "captured");
            }
        }

        Result voidAuthorization(String intentId) {
            PaymentIntent pi = intent(intentId);
            synchronized (pi) { pi.method.voidAuth(pi.id); pi.to(IntentStatus.VOIDED); return new Result(true, intentId, "voided"); }
        }

        /** external -> wallet (full amount), wallet -> fees (method fee charged to the user). */
        private void creditTopUp(PaymentIntent pi) {
            long fee = pi.method.feePaise(pi.amount);
            ledger.post("TOPUP", pi.id, List.of(
                    new Posting(EXTERNAL, -pi.amount), new Posting("W:" + pi.userId, pi.amount - fee), new Posting(FEES, fee)));
        }

        // ---- pay a merchant from the wallet (platform keeps a fee) ----
        Result payMerchant(String key, String user, String merchant, long amount) {
            return idempotent(key, () -> {
                long fee = amount * merchantFeeBps / 10_000;
                JournalEntry e = ledger.post("PAYMENT", key, List.of(
                        new Posting("W:" + user, -amount), new Posting("M:" + merchant, amount - fee), new Posting(FEES, fee)));
                return new Result(true, e.id(), "paid");
            });
        }

        // ---- peer to peer ----
        Result transfer(String key, String from, String to, long amount) {
            return idempotent(key, () -> {
                if (amount <= 0) throw new LedgerException("amount must be positive");
                if (from.equals(to)) throw new LedgerException("same wallet");
                JournalEntry e = ledger.post("P2P", key, List.of(new Posting("W:" + from, -amount), new Posting("W:" + to, amount)));
                return new Result(true, e.id(), "sent");
            });
        }

        /**
         * REFUND = a NEW reversing entry referencing the original (never edit history).
         * Partial refunds allowed; total refunded can't exceed the payment.
         */
        Result refundPayment(String key, String paymentEntryId, long amount) {
            return idempotent(key, () -> {
                JournalEntry orig = ledger.journal.stream().filter(j -> j.id().equals(paymentEntryId) && j.kind().equals("PAYMENT")).findFirst()
                        .orElseThrow(() -> new LedgerException("No payment " + paymentEntryId));
                long paid = -orig.postings().get(0).amount();
                long already = ledger.journal.stream().filter(j -> j.kind().equals("REFUND") && j.reference().equals(paymentEntryId))
                        .mapToLong(j -> j.postings().get(0).amount()).sum();
                if (already + amount > paid) throw new LedgerException("Refund exceeds payment (" + already + " already refunded)");
                // proportional split: merchant returns its share, platform returns its fee share
                long merchantShare = Math.round((double) amount * orig.postings().get(1).amount() / paid);
                long feeShare = amount - merchantShare;
                JournalEntry r = ledger.post("REFUND", paymentEntryId, List.of(
                        new Posting(orig.postings().get(0).accountId(), amount),
                        new Posting(orig.postings().get(1).accountId(), -merchantShare),
                        new Posting(FEES, -feeShare)));
                return new Result(true, r.id(), already + amount == paid ? "fully refunded" : "partially refunded");
            });
        }

        private PaymentMethod method(MethodType t) { PaymentMethod m = methods.get(t); if (m == null) throw new LedgerException("Method " + t + " not supported"); return m; }
        PaymentIntent intent(String id) { PaymentIntent p = intents.get(id); if (p == null) throw new LedgerException("No intent " + id); return p; }
    }

    // =====================================================================
    // [demo]
    // =====================================================================

    public static void main(String[] args) throws Exception {
        FakeRail rail = new FakeRail();
        WalletService w = new WalletService(200, new UpiMethod(rail), new CardMethod(rail), new NetBankingMethod(rail));   // 2% merchant fee
        for (String u : List.of("asha", "ravi")) w.openWallet(u);
        w.openMerchant("cafe");

        System.out.println("1. Top-ups through different payment methods (Strategy)");
        check("UPI, no fee", true, w.topUp("k1", "asha", 100_000, MethodType.UPI).ok());
        check("wallet credited Rs 1000", 100_000L, w.balance("W:asha"));
        w.topUp("k2", "ravi", 50_000, MethodType.NETBANKING);
        check("netbanking: Rs 10 flat fee", 49_000L, w.balance("W:ravi"));
        Result auth = w.topUp("k3", "asha", 20_000, MethodType.CARD);
        check("card is two-phase: authorized, not yet credited", "authorized; capture to credit/100000", auth.message() + "/" + w.balance("W:asha"));
        w.capture(auth.reference());
        check("captured: +Rs 200 minus 2% fee", 119_600L, w.balance("W:asha"));
        check("capture twice is refused", false, w.capture(auth.reference()).ok());
        Result auth2 = w.topUp("k4", "asha", 5_000, MethodType.CARD);
        w.voidAuthorization(auth2.reference());
        check("void: authorization released, nothing credited", IntentStatus.VOIDED + "/119600", w.intent(auth2.reference()).status + "/" + w.balance("W:asha"));
        rail.declineIntents.add("PI5");
        check("declined top-up", "declined", w.topUp("k5", "asha", 1_000, MethodType.UPI).message());

        System.out.println("2. Idempotency: a retried request is not executed twice");
        int railCalls = rail.calls.get();
        Result again = w.topUp("k1", "asha", 100_000, MethodType.UPI);
        check("same result returned", true, again.ok() && again.message().equals("credited"));
        check("no second gateway call, no second credit", railCalls + "/119600", rail.calls.get() + "/" + w.balance("W:asha"));

        System.out.println("3. Pay a merchant (platform fee), P2P, insufficient funds");
        Result pay = w.payMerchant("p1", "asha", "cafe", 50_000);
        check("merchant gets 98%", 49_000L, w.balance("M:cafe"));
        check("platform fees so far (nb 10 + card 4 + 10)", 2_400L, w.balance(WalletService.FEES));
        check("P2P", true, w.transfer("t1", "ravi", "asha", 9_000).ok());
        Result tooMuch = w.transfer("t2", "ravi", "asha", 1_000_000);
        check("insufficient funds rejected", "Insufficient funds in W:ravi", tooMuch.message());
        check("failed result is idempotent too", tooMuch.message(), w.transfer("t2", "ravi", "asha", 1).message());

        System.out.println("4. Refunds: new reversing entries, partial then full, never over-refund");
        check("partial", "partially refunded", w.refundPayment("r1", pay.reference(), 20_000).message());
        check("merchant returned 98% of 200", 29_400L, w.balance("M:cafe"));
        check("rest", "fully refunded", w.refundPayment("r2", pay.reference(), 30_000).message());
        check("over-refund rejected", false, w.refundPayment("r3", pay.reference(), 1).ok());
        check("merchant back to 0", 0L, w.balance("M:cafe"));

        System.out.println("5. Ledger invariants");
        check("every entry balanced (enforced at construction)", true, w.ledger.journal.stream().allMatch(j -> j.postings().stream().mapToLong(Posting::amount).sum() == 0));
        check("sum of ALL accounts is 0", 0L, w.ledger.sumOfAllBalances());
        check("cached balances == recomputed from journal", true, w.ledger.verify());
        expect("an unbalanced entry can't even be built", () -> new JournalEntry("x", "BAD", "", List.of(new Posting("a", 5), new Posting("b", -4))));

        System.out.println("6. Concurrency: 16 threads, random P2P among 10 wallets, every request sent twice");
        WalletService c = new WalletService(0, new UpiMethod(new FakeRail()));
        for (int i = 0; i < 10; i++) { c.openWallet("u" + i); c.topUp("seed" + i, "u" + i, 10_000, MethodType.UPI); }
        ExecutorService pool = Executors.newFixedThreadPool(16);
        AtomicInteger ok = new AtomicInteger();
        CountDownLatch go = new CountDownLatch(1);
        for (int t = 0; t < 16; t++) {
            int th = t;
            pool.submit(() -> {
                go.await();
                ThreadLocalRandom r = ThreadLocalRandom.current();
                for (int i = 0; i < 500; i++) {
                    int a = r.nextInt(10), b = (a + 1 + r.nextInt(9)) % 10;
                    String key = "tx-" + th + "-" + i;
                    long amt = 1 + r.nextInt(3_000);
                    Result r1 = c.transfer(key, "u" + a, "u" + b, amt);
                    Result r2 = c.transfer(key, "u" + a, "u" + b, amt);              // client retry
                    if (r1.ok()) ok.incrementAndGet();
                    if (!Objects.equals(r1.reference(), r2.reference())) throw new IllegalStateException("idempotency broken");
                }
                return null;
            });
        }
        go.countDown();
        pool.shutdown();
        check("finished without deadlock", true, pool.awaitTermination(20, TimeUnit.SECONDS));
        long walletSum = 0; boolean noneNegative = true;
        for (int i = 0; i < 10; i++) { long b = c.balance("W:u" + i); walletSum += b; noneNegative &= b >= 0; }
        check("no wallet ever negative", true, noneNegative);
        check("wallet money conserved (100,000)", 100_000L, walletSum);
        check("journal has exactly one entry per successful transfer (+10 seeds)", ok.get() + 10, c.ledger.journal.size());
        check("ledger verifies", true, c.ledger.verify());
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
