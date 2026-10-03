/*
 * ============================================================================
 *  LLD 12 : ATM SYSTEM  (State pattern + transaction consistency)
 * ============================================================================
 *  Run:  java AtmSystem.java          (design notes: README.md)
 *
 *     [bank]       BankService interface + InMemoryBank: accounts, PIN check with lockout,
 *                  daily limit, IDEMPOTENT debit/credit by transaction id, reversal
 *     [dispenser]  CashDispenser as a CHAIN OF RESPONSIBILITY of note handlers
 *                  (2000 -> 500 -> 200 -> 100), PLAN first, COMMIT only if fully payable
 *     [atm]        Atm (Context) + AtmState: Idle, CardInserted, Authenticated, OutOfService
 *     [txn]        Withdrawal as a TEMPLATE: validate -> plan cash -> debit -> dispense ->
 *                  (fault? -> REVERSE the debit)
 *     [demo]       PIN lockout, withdrawals, limits, out-of-notes, hardware fault with
 *                  reversal, idempotent retries, and a 2-ATM concurrency test on one account
 *
 *  THE CONSISTENCY PROBLEM: money moves in TWO systems - the bank ledger and the ATM's cash.
 *     debit OK + dispense FAILS  ->  customer charged, no cash   (worst outcome)
 *     dispense OK + debit FAILS  ->  free money
 *  Order of operations that minimises damage:
 *     1. plan the notes (no side effects) - can this ATM pay exactly this amount?
 *     2. debit the bank with a unique txn id (idempotent)
 *     3. dispense
 *     4. if dispensing fails -> REVERSE the debit (compensating transaction)
 *  This is the saga pattern in miniature.
 * ============================================================================
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

public class AtmSystem {

    // =====================================================================
    // [bank]
    // =====================================================================

    static class BankException extends RuntimeException { BankException(String m) { super(m); } }

    /** What the ATM needs from the bank (the ATM never touches balances directly). */
    interface BankService {
        boolean verifyPin(String cardNo, String pin);                  // counts failures, blocks after 3
        boolean isBlocked(String cardNo);
        long balance(String cardNo);
        void debit(String txnId, String cardNo, long amount);           // idempotent on txnId
        void credit(String txnId, String cardNo, long amount);
        void reverse(String txnId);                                     // undo a debit (compensation)
    }

    static class Account {
        final String cardNo, pinHash;
        long balance;
        long withdrawnToday;
        int failedPins;
        boolean blocked;
        final ReentrantLock lock = new ReentrantLock();
        Account(String cardNo, String pin, long balance) { this.cardNo = cardNo; this.pinHash = hash(pin); this.balance = balance; }
        static String hash(String pin) { return Integer.toHexString(("salt:" + pin).hashCode()); }   // never store raw PINs (real: HSM)
    }

    record LedgerEntry(String txnId, String cardNo, long amount, String type) {}

    static class InMemoryBank implements BankService {
        static final long DAILY_LIMIT = 40_000;
        private final Map<String, Account> accounts = new ConcurrentHashMap<>();
        private final Map<String, LedgerEntry> ledger = new ConcurrentHashMap<>();   // txnId -> entry (idempotency)
        final AtomicInteger debitCalls = new AtomicInteger();

        void open(String card, String pin, long balance) { accounts.put(card, new Account(card, pin, balance)); }

        private Account acc(String card) { Account a = accounts.get(card); if (a == null) throw new BankException("Unknown card"); return a; }

        public boolean verifyPin(String card, String pin) {
            Account a = acc(card);
            a.lock.lock();
            try {
                if (a.blocked) return false;
                if (a.pinHash.equals(Account.hash(pin))) { a.failedPins = 0; return true; }
                if (++a.failedPins >= 3) a.blocked = true;
                return false;
            } finally { a.lock.unlock(); }
        }

        public boolean isBlocked(String card) { return acc(card).blocked; }
        public long balance(String card) { Account a = acc(card); a.lock.lock(); try { return a.balance; } finally { a.lock.unlock(); } }

        public void debit(String txnId, String card, long amount) {
            debitCalls.incrementAndGet();
            Account a = acc(card);
            a.lock.lock();                                              // per-ACCOUNT lock: two ATMs, one account
            try {
                if (ledger.containsKey(txnId)) return;                 // a retry of the same txn: already applied
                if (a.balance < amount) throw new BankException("Insufficient funds");
                if (a.withdrawnToday + amount > DAILY_LIMIT) throw new BankException("Daily limit exceeded");
                a.balance -= amount;
                a.withdrawnToday += amount;
                ledger.put(txnId, new LedgerEntry(txnId, card, -amount, "DEBIT"));
            } finally { a.lock.unlock(); }
        }

        public void credit(String txnId, String card, long amount) {
            Account a = acc(card);
            a.lock.lock();
            try {
                if (ledger.containsKey(txnId)) return;
                a.balance += amount;
                ledger.put(txnId, new LedgerEntry(txnId, card, amount, "CREDIT"));
            } finally { a.lock.unlock(); }
        }

        public void reverse(String txnId) {
            LedgerEntry e = ledger.get(txnId);
            if (e == null || !e.type().equals("DEBIT")) return;
            String revId = "REV-" + txnId;
            Account a = acc(e.cardNo());
            a.lock.lock();
            try {
                if (ledger.containsKey(revId)) return;                 // reversal is idempotent too
                a.balance += -e.amount();
                a.withdrawnToday -= -e.amount();
                ledger.put(revId, new LedgerEntry(revId, e.cardNo(), -e.amount(), "REVERSAL"));
            } finally { a.lock.unlock(); }
        }
    }

    // =====================================================================
    // [dispenser] chain of responsibility
    // =====================================================================

    /**
     * Each handler takes as many of ITS notes as possible, then passes the rest down the chain.
     * plan() has no side effects; commit() actually removes notes. Splitting them lets the
     * ATM say "can't dispense that amount" BEFORE debiting the customer.
     */
    static abstract class NoteHandler {
        final int denomination;
        int count;
        NoteHandler next;
        NoteHandler(int denomination, int count) { this.denomination = denomination; this.count = count; }
        NoteHandler then(NoteHandler n) { next = n; return n; }

        /** Returns denomination -> notes, or null if the chain can't pay `amount` exactly. */
        Map<Integer, Integer> plan(int amount, Map<Integer, Integer> acc) {
            int use = Math.min(count, amount / denomination);
            for (; use >= 0; use--) {                                   // try most notes first, back off if the rest fails
                int rest = amount - use * denomination;
                Map<Integer, Integer> attempt = new TreeMap<>(Comparator.reverseOrder());
                attempt.putAll(acc);
                if (use > 0) attempt.put(denomination, use);
                if (rest == 0) return attempt;
                if (next != null) { Map<Integer, Integer> r = next.plan(rest, attempt); if (r != null) return r; }
            }
            return null;
        }
    }

    static class Note2000 extends NoteHandler { Note2000(int c) { super(2000, c); } }
    static class Note500 extends NoteHandler { Note500(int c) { super(500, c); } }
    static class Note200 extends NoteHandler { Note200(int c) { super(200, c); } }
    static class Note100 extends NoteHandler { Note100(int c) { super(100, c); } }

    static class CashDispenser {
        private final NoteHandler head;
        private final Map<Integer, NoteHandler> byDenomination = new HashMap<>();
        volatile boolean faulty;                                        // simulate a jam

        CashDispenser(NoteHandler... chain) {
            head = chain[0];
            for (int i = 0; i < chain.length; i++) { byDenomination.put(chain[i].denomination, chain[i]); if (i + 1 < chain.length) chain[i].then(chain[i + 1]); }
        }
        synchronized Optional<Map<Integer, Integer>> plan(int amount) { return Optional.ofNullable(head.plan(amount, Map.of())); }
        synchronized void dispense(Map<Integer, Integer> notes) {
            if (faulty) throw new IllegalStateException("Dispenser jammed");
            for (var e : notes.entrySet()) if (byDenomination.get(e.getKey()).count < e.getValue()) throw new IllegalStateException("Notes changed since planning");
            notes.forEach((d, n) -> byDenomination.get(d).count -= n);
        }
        synchronized int cash() { return byDenomination.values().stream().mapToInt(h -> h.denomination * h.count).sum(); }
    }

    // =====================================================================
    // [atm] State pattern
    // =====================================================================

    interface AtmState {
        default void insertCard(Atm atm, String card) { atm.say("Card already inserted"); }
        default void enterPin(Atm atm, String pin) { atm.say("Insert card first"); }
        default void withdraw(Atm atm, int amount) { atm.say("Not authenticated"); }
        default void deposit(Atm atm, int amount) { atm.say("Not authenticated"); }
        default void balance(Atm atm) { atm.say("Not authenticated"); }
        default void eject(Atm atm) { atm.say("No card"); }
        String name();
    }

    static class IdleState implements AtmState {
        public void insertCard(Atm atm, String card) {
            if (atm.bank.isBlocked(card)) { atm.say("Card blocked - contact your bank"); return; }
            atm.card = card;
            atm.setState(atm.cardInserted);
        }
        public String name() { return "IDLE"; }
    }

    static class CardInsertedState implements AtmState {
        public void enterPin(Atm atm, String pin) {
            if (atm.bank.verifyPin(atm.card, pin)) { atm.setState(atm.authenticated); return; }
            if (atm.bank.isBlocked(atm.card)) { atm.say("Too many wrong PINs - card retained"); atm.retained.add(atm.card); atm.card = null; atm.setState(atm.idle); return; }
            atm.say("Wrong PIN");
        }
        public void eject(Atm atm) { atm.card = null; atm.setState(atm.idle); }
        public String name() { return "CARD_INSERTED"; }
    }

    static class AuthenticatedState implements AtmState {
        public void withdraw(Atm atm, int amount) { atm.performWithdrawal(amount); }
        public void deposit(Atm atm, int amount) {
            if (amount <= 0 || amount % 100 != 0) { atm.say("Deposit multiples of 100"); return; }
            atm.bank.credit(atm.newTxnId(), atm.card, amount);
            atm.say("Deposited " + amount);
        }
        public void balance(Atm atm) { atm.say("Balance " + atm.bank.balance(atm.card)); }
        public void eject(Atm atm) { atm.card = null; atm.setState(atm.idle); }
        public String name() { return "AUTHENTICATED"; }
    }

    static class OutOfServiceState implements AtmState {
        public void insertCard(Atm atm, String card) { atm.say("Out of service"); }
        public String name() { return "OUT_OF_SERVICE"; }
    }

    static class Atm {
        final String id;
        final BankService bank;
        final CashDispenser dispenser;
        final AtmState idle = new IdleState(), cardInserted = new CardInsertedState(), authenticated = new AuthenticatedState(), outOfService = new OutOfServiceState();
        private AtmState state = idle;
        String card;
        final List<String> screen = new ArrayList<>();
        final List<String> retained = new ArrayList<>();
        private final AtomicLong seq = new AtomicLong();

        Atm(String id, BankService bank, CashDispenser dispenser) { this.id = id; this.bank = bank; this.dispenser = dispenser; }

        synchronized void insertCard(String c) { state.insertCard(this, c); }
        synchronized void enterPin(String p) { state.enterPin(this, p); }
        synchronized void withdraw(int amount) { state.withdraw(this, amount); }
        synchronized void deposit(int amount) { state.deposit(this, amount); }
        synchronized void checkBalance() { state.balance(this); }
        synchronized void eject() { state.eject(this); }

        void setState(AtmState s) { state = s; }
        void say(String msg) { screen.add(msg); }
        String newTxnId() { return id + "-" + seq.incrementAndGet(); }
        synchronized String state() { return state.name(); }
        synchronized String lastMessage() { return screen.isEmpty() ? "" : screen.get(screen.size() - 1); }

        /**
         * TEMPLATE of a withdrawal - the ORDER of these steps is the whole design:
         *   validate -> plan notes (no side effects) -> debit (idempotent) -> dispense -> or REVERSE
         */
        void performWithdrawal(int amount) {
            if (amount <= 0 || amount % 100 != 0) { say("Amount must be a multiple of 100"); return; }
            Optional<Map<Integer, Integer>> plan = dispenser.plan(amount);
            if (plan.isEmpty()) { say("ATM cannot dispense " + amount + " - try another amount"); return; }   // nothing debited
            String txn = newTxnId();
            try {
                bank.debit(txn, card, amount);
            } catch (BankException e) { say(e.getMessage()); return; }
            try {
                dispenser.dispense(plan.get());
                say("Dispensed " + plan.get());
            } catch (IllegalStateException fault) {
                bank.reverse(txn);                                          // COMPENSATE: customer must not lose money
                say("Could not dispense - your account was not charged");
                setState(outOfService);                                     // a jammed ATM stops serving
                card = null;
            }
        }
    }

    // =====================================================================
    // [demo]
    // =====================================================================

    static CashDispenser loaded(int n2000, int n500, int n200, int n100) {
        return new CashDispenser(new Note2000(n2000), new Note500(n500), new Note200(n200), new Note100(n100));
    }

    public static void main(String[] args) throws Exception {
        InMemoryBank bank = new InMemoryBank();
        bank.open("4111", "1234", 50_000);
        bank.open("4222", "9999", 10_000);
        Atm atm = new Atm("ATM1", bank, loaded(20, 20, 20, 20));   // Rs 56,000 of notes

        System.out.println("1. Session state machine and PIN lockout");
        atm.withdraw(500);
        check("withdraw without a card", "Not authenticated", atm.lastMessage());
        atm.insertCard("4222");
        atm.enterPin("0000"); atm.enterPin("1111");
        check("2 wrong PINs: still CARD_INSERTED", "CARD_INSERTED", atm.state());
        atm.enterPin("2222");
        check("3rd wrong PIN retains the card", "Too many wrong PINs - card retained", atm.lastMessage());
        check("back to IDLE", "IDLE", atm.state());
        atm.insertCard("4222");
        check("blocked card refused", "Card blocked - contact your bank", atm.lastMessage());

        System.out.println("2. Withdrawal: chain of note handlers");
        atm.insertCard("4111");
        atm.enterPin("1234");
        check("authenticated", "AUTHENTICATED", atm.state());
        atm.withdraw(3700);
        check("2000 + 500x3 + 200", "Dispensed {2000=1, 500=3, 200=1}", atm.lastMessage());
        check("balance debited", 46_300L, bank.balance("4111"));
        atm.withdraw(150);
        check("not a multiple of 100", "Amount must be a multiple of 100", atm.lastMessage());
        atm.withdraw(40_000);
        check("daily limit (40k incl. earlier 3.7k)", "Daily limit exceeded", atm.lastMessage());
        atm.checkBalance();
        check("balance inquiry", "Balance 46300", atm.lastMessage());

        System.out.println("3. Plan BEFORE debit: an ATM short of small notes");
        Atm noSmall = new Atm("ATM2", bank, loaded(5, 0, 0, 0));
        noSmall.insertCard("4111"); noSmall.enterPin("1234");
        long before = bank.balance("4111");
        noSmall.withdraw(2500);
        check("refused up front", "ATM cannot dispense 2500 - try another amount", noSmall.lastMessage());
        check("account untouched", before, bank.balance("4111"));
        Atm backoff = new Atm("ATM3", bank, loaded(0, 1, 3, 0));
        backoff.insertCard("4111"); backoff.enterPin("1234");
        backoff.withdraw(600);
        check("greedy 500 fails, handler backs off -> 200x3", "Dispensed {200=3}", backoff.lastMessage());

        System.out.println("4. Hardware fault after debit -> compensating reversal");
        Atm jam = new Atm("ATM4", bank, loaded(10, 10, 10, 10));
        jam.insertCard("4111"); jam.enterPin("1234");
        long pre = bank.balance("4111");
        jam.dispenser.faulty = true;
        jam.withdraw(1000);
        check("customer told they weren't charged", "Could not dispense - your account was not charged", jam.lastMessage());
        check("balance restored", pre, bank.balance("4111"));
        check("ATM goes OUT_OF_SERVICE", "OUT_OF_SERVICE", jam.state());
        jam.insertCard("4111");
        check("refuses new customers", "Out of service", jam.lastMessage());

        System.out.println("5. Idempotent debit and reversal (network retries)");
        bank.open("4333", "1111", 5_000);
        bank.debit("T-1", "4333", 1000);
        bank.debit("T-1", "4333", 1000);                                   // retry after a timeout
        check("debited once", 4_000L, bank.balance("4333"));
        bank.reverse("T-1"); bank.reverse("T-1");
        check("reversed once", 5_000L, bank.balance("4333"));

        System.out.println("6. Concurrency: 2 ATMs x 8 threads withdraw from one joint account");
        bank.open("4444", "4444", 30_000);
        CashDispenser big1 = loaded(0, 0, 0, 10_000), big2 = loaded(0, 0, 0, 10_000);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        AtomicLong dispensed = new AtomicLong();
        CountDownLatch go = new CountDownLatch(1);
        for (int t = 0; t < 16; t++) {
            CashDispenser d = t % 2 == 0 ? big1 : big2;
            int th = t;
            pool.submit(() -> {
                go.await();
                for (int i = 0; i < 100; i++) {
                    String txn = "C" + th + "-" + i;
                    try { bank.debit(txn, "4444", 100); d.dispense(Map.of(100, 1)); dispensed.addAndGet(100); }
                    catch (BankException e) { /* insufficient funds or limit */ }
                }
                return null;
            });
        }
        go.countDown();
        pool.shutdown(); pool.awaitTermination(10, TimeUnit.SECONDS);
        check("never overdrawn", true, bank.balance("4444") >= 0);
        check("cash out == money debited", 30_000L - bank.balance("4444"), dispensed.get());
        check("daily limit respected", true, dispensed.get() <= InMemoryBank.DAILY_LIMIT);
        check("dispensers lost exactly that cash", dispensed.get(), 2_000_000L - big1.cash() - big2.cash());
        summary();
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
