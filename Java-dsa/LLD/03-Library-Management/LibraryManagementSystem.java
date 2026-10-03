/*
 * ============================================================================
 *  LLD 03 : LIBRARY MANAGEMENT SYSTEM
 * ============================================================================
 *  Run:  java LibraryManagementSystem.java          (design notes: README.md)
 *
 *     [model]     Book (title-level metadata) vs BookCopy (a physical item), Member,
 *                 MembershipTier, Loan, Hold
 *     [policy]    FinePolicy (Strategy), MembershipTier rules (data on the enum)
 *     [search]    Catalog with inverted indexes (title words, author, subject)
 *     [observer]  LibraryListener -> notifications ("your hold is ready", "overdue")
 *     [service]   LibraryService: checkout / return / renew / hold / pay fine
 *     [demo]      main(): rules, fines, the hold queue, renewals, a concurrency race
 *
 *  THE #1 MODELLING INSIGHT: "Book" is two things.
 *     Book      = the TITLE (ISBN, title, author)       - what you search for, place holds on
 *     BookCopy  = one PHYSICAL item (barcode, status)   - what you lend, lose, put on a shelf
 *  Mixing them makes "3 copies of the same book" and "hold on ANY copy" impossible to model.
 *
 *  CONCURRENCY: the contended resource is "the available copies of one title". Two members
 *  grabbing the last copy must not both succeed -> a lock PER ISBN (titles don't block
 *  each other).
 * ============================================================================
 */

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;

public class LibraryManagementSystem {

    // =====================================================================
    // [model]
    // =====================================================================

    /** Title-level, immutable metadata. Equal by ISBN. */
    record Book(String isbn, String title, String author, String subject) {}

    enum CopyStatus { AVAILABLE, ON_LOAN, RESERVED, LOST }

    /** One physical copy. Its status is ONLY changed by LibraryService while holding the title's lock. */
    static class BookCopy {
        final String barcode;
        final Book book;
        CopyStatus status = CopyStatus.AVAILABLE;
        String reservedFor;                                 // member id, when RESERVED for a hold
        LocalDate reservedUntil;
        BookCopy(String barcode, Book book) { this.barcode = barcode; this.book = book; }
    }

    /**
     * Tier rules are DATA on the enum, not subclasses: a premium member doesn't BEHAVE
     * differently, they just have bigger limits. A new tier is one line.
     */
    enum MembershipTier {
        STUDENT(3, 14, 1), STANDARD(5, 21, 2), PREMIUM(10, 30, 3);
        final int maxLoans, loanDays, maxRenewals;
        MembershipTier(int maxLoans, int loanDays, int maxRenewals) { this.maxLoans = maxLoans; this.loanDays = loanDays; this.maxRenewals = maxRenewals; }
    }

    static class Member {
        final String id, name;
        final MembershipTier tier;
        final Set<String> activeLoanIds = ConcurrentHashMap.newKeySet();
        final AtomicLong unpaidFinePaise = new AtomicLong();
        Member(String id, String name, MembershipTier tier) { this.id = id; this.name = name; this.tier = tier; }
    }

    static class Loan {
        final String id;
        final BookCopy copy;
        final Member member;
        final LocalDate issuedOn;
        LocalDate dueOn;
        LocalDate returnedOn;
        int renewals;
        Loan(String id, BookCopy copy, Member member, LocalDate issuedOn, LocalDate dueOn) {
            this.id = id; this.copy = copy; this.member = member; this.issuedOn = issuedOn; this.dueOn = dueOn;
        }
    }

    // =====================================================================
    // [policy] fines (Strategy)
    // =====================================================================

    interface FinePolicy { long finePaise(Loan loan, LocalDate returnedOn); }

    /** Rs 5 per day late, capped at Rs 200 - the usual public-library rule. */
    static class PerDayFine implements FinePolicy {
        private final long perDay, cap;
        PerDayFine(long perDayPaise, long capPaise) { this.perDay = perDayPaise; this.cap = capPaise; }
        public long finePaise(Loan loan, LocalDate returnedOn) {
            long late = ChronoUnit.DAYS.between(loan.dueOn, returnedOn);
            return late <= 0 ? 0 : Math.min(cap, late * perDay);
        }
    }

    /** Grace period, then escalating: days 1-7 at x, after that 2x (e.g. a university library). */
    static class GraceThenEscalatingFine implements FinePolicy {
        private final int graceDays;
        private final long base;
        GraceThenEscalatingFine(int graceDays, long basePaise) { this.graceDays = graceDays; this.base = basePaise; }
        public long finePaise(Loan loan, LocalDate returnedOn) {
            long late = ChronoUnit.DAYS.between(loan.dueOn, returnedOn) - graceDays;
            if (late <= 0) return 0;
            return Math.min(late, 7) * base + Math.max(0, late - 7) * base * 2;
        }
    }

    // =====================================================================
    // [search] the catalog with inverted indexes
    // =====================================================================

    interface Searchable { List<Book> byTitleWord(String w); List<Book> byAuthor(String a); List<Book> bySubject(String s); }

    /**
     * Inverted index: word -> set of ISBNs. A search is a map lookup instead of scanning every
     * book. (At scale this is exactly what Elasticsearch/Lucene does.)
     */
    static class Catalog implements Searchable {
        private final Map<String, Book> byIsbn = new ConcurrentHashMap<>();
        private final Map<String, Set<String>> titleIndex = new ConcurrentHashMap<>(), authorIndex = new ConcurrentHashMap<>(), subjectIndex = new ConcurrentHashMap<>();

        void add(Book b) {
            byIsbn.put(b.isbn(), b);
            for (String w : b.title().toLowerCase().split("\\W+")) index(titleIndex, w, b);
            index(authorIndex, b.author().toLowerCase(), b);
            index(subjectIndex, b.subject().toLowerCase(), b);
        }

        private static void index(Map<String, Set<String>> idx, String key, Book b) {
            idx.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(b.isbn());
        }

        private List<Book> lookup(Map<String, Set<String>> idx, String key) {
            return idx.getOrDefault(key.toLowerCase(), Set.of()).stream().map(byIsbn::get)
                    .sorted(Comparator.comparing(Book::title)).toList();
        }

        public List<Book> byTitleWord(String w) { return lookup(titleIndex, w); }
        public List<Book> byAuthor(String a) { return lookup(authorIndex, a); }
        public List<Book> bySubject(String s) { return lookup(subjectIndex, s); }
        Book get(String isbn) { return byIsbn.get(isbn); }
    }

    // =====================================================================
    // [observer]
    // =====================================================================

    interface LibraryListener {
        default void holdReady(Member m, BookCopy c, LocalDate until) {}
        default void overdue(Loan loan, long days) {}
    }

    static class NotificationLog implements LibraryListener {
        final List<String> sent = new CopyOnWriteArrayList<>();
        public void holdReady(Member m, BookCopy c, LocalDate until) { sent.add("to " + m.id + ": '" + c.book.title() + "' is ready until " + until); }
        public void overdue(Loan l, long days) { sent.add("to " + l.member.id + ": '" + l.copy.book.title() + "' overdue by " + days + " days"); }
    }

    // =====================================================================
    // [service]
    // =====================================================================

    static class LibraryException extends RuntimeException { LibraryException(String m) { super(m); } }

    static class LibraryService {
        static final long MAX_UNPAID_FINE_PAISE = 10_000;              // Rs 100: above this you can't borrow
        static final int HOLD_PICKUP_DAYS = 3;

        final Catalog catalog = new Catalog();
        private final Map<String, List<BookCopy>> copiesByIsbn = new ConcurrentHashMap<>();
        private final Map<String, BookCopy> copiesByBarcode = new ConcurrentHashMap<>();
        private final Map<String, Member> members = new ConcurrentHashMap<>();
        private final Map<String, Loan> loans = new ConcurrentHashMap<>();
        private final Map<String, Deque<String>> holdQueues = new ConcurrentHashMap<>();   // isbn -> FIFO of member ids
        private final Map<String, ReentrantLock> titleLocks = new ConcurrentHashMap<>();
        private final List<LibraryListener> listeners = new CopyOnWriteArrayList<>();
        private final FinePolicy finePolicy;
        private final Supplier<LocalDate> today;
        private final AtomicLong seq = new AtomicLong();

        LibraryService(FinePolicy finePolicy, Supplier<LocalDate> today) { this.finePolicy = finePolicy; this.today = today; }

        // ---- admin ----
        void addBook(Book b, int copies) {
            catalog.add(b);
            for (int i = 0; i < copies; i++) {
                BookCopy c = new BookCopy(b.isbn() + "-" + (copiesByIsbn.getOrDefault(b.isbn(), List.of()).size() + 1), b);
                copiesByIsbn.computeIfAbsent(b.isbn(), k -> new CopyOnWriteArrayList<>()).add(c);
                copiesByBarcode.put(c.barcode, c);
            }
        }
        Member register(String id, String name, MembershipTier tier) { Member m = new Member(id, name, tier); members.put(id, m); return m; }
        void addListener(LibraryListener l) { listeners.add(l); }

        /** All state changes for one title happen under that title's lock. */
        private <T> T withTitle(String isbn, Supplier<T> action) {
            ReentrantLock lock = titleLocks.computeIfAbsent(isbn, k -> new ReentrantLock());
            lock.lock();
            try { return action.get(); } finally { lock.unlock(); }
        }

        // ---- borrowing ----
        Loan checkout(String memberId, String isbn) {
            Member m = member(memberId);
            if (catalog.get(isbn) == null) throw new LibraryException("Unknown ISBN " + isbn);
            return withTitle(isbn, () -> {
                // rules first (fail fast, nothing changed yet)
                if (m.activeLoanIds.size() >= m.tier.maxLoans) throw new LibraryException(m.id + " reached the limit of " + m.tier.maxLoans);
                if (m.unpaidFinePaise.get() > MAX_UNPAID_FINE_PAISE) throw new LibraryException(m.id + " has unpaid fines");
                expireStaleReservations(isbn);
                // 1) a copy reserved for THIS member (their hold came in), else 2) a free copy
                //    that isn't needed for someone ahead in the hold queue
                BookCopy copy = copiesByIsbn.get(isbn).stream()
                        .filter(c -> c.status == CopyStatus.RESERVED && memberId.equals(c.reservedFor)).findFirst()
                        .orElseGet(() -> copiesByIsbn.get(isbn).stream().filter(c -> c.status == CopyStatus.AVAILABLE).findFirst().orElse(null));
                if (copy == null) throw new LibraryException("No copy of " + isbn + " available; place a hold");
                copy.status = CopyStatus.ON_LOAN;
                copy.reservedFor = null;
                holdQueues.getOrDefault(isbn, new ArrayDeque<>()).remove(memberId);   // their hold is fulfilled
                LocalDate now = today.get();
                Loan loan = new Loan("L" + seq.incrementAndGet(), copy, m, now, now.plusDays(m.tier.loanDays));
                loans.put(loan.id, loan);
                m.activeLoanIds.add(loan.id);
                return loan;
            });
        }

        /** Returns the fine charged (paise). If someone holds the title, the copy is reserved for them. */
        long returnCopy(String barcode) {
            BookCopy copy = copiesByBarcode.get(barcode);
            if (copy == null) throw new LibraryException("Unknown copy " + barcode);
            return withTitle(copy.book.isbn(), () -> {
                Loan loan = loans.values().stream().filter(l -> l.copy == copy && l.returnedOn == null).findFirst()
                        .orElseThrow(() -> new LibraryException(barcode + " is not on loan"));
                LocalDate now = today.get();
                loan.returnedOn = now;
                loan.member.activeLoanIds.remove(loan.id);
                long fine = finePolicy.finePaise(loan, now);
                loan.member.unpaidFinePaise.addAndGet(fine);
                copy.status = CopyStatus.AVAILABLE;
                assignToNextHold(copy);
                return fine;
            });
        }

        Loan renew(String loanId) {
            Loan loan = loans.get(loanId);
            if (loan == null || loan.returnedOn != null) throw new LibraryException("No active loan " + loanId);
            return withTitle(loan.copy.book.isbn(), () -> {
                if (!holdQueues.getOrDefault(loan.copy.book.isbn(), new ArrayDeque<>()).isEmpty())
                    throw new LibraryException("Can't renew: others are waiting");       // fairness rule
                if (loan.renewals >= loan.member.tier.maxRenewals) throw new LibraryException("Renewal limit reached");
                if (today.get().isAfter(loan.dueOn)) throw new LibraryException("Overdue loans can't be renewed");
                loan.renewals++;
                loan.dueOn = loan.dueOn.plusDays(loan.member.tier.loanDays);
                return loan;
            });
        }

        // ---- holds (reservation queue per TITLE, not per copy) ----
        int placeHold(String memberId, String isbn) {
            Member m = member(memberId);
            return withTitle(isbn, () -> {
                boolean anyFree = copiesByIsbn.get(isbn).stream().anyMatch(c -> c.status == CopyStatus.AVAILABLE);
                if (anyFree) throw new LibraryException("A copy is available - just borrow it");
                Deque<String> q = holdQueues.computeIfAbsent(isbn, k -> new ArrayDeque<>());
                if (q.contains(m.id)) throw new LibraryException("Already on the hold list");
                q.addLast(m.id);
                return q.size();                                            // position in the queue
            });
        }

        private void assignToNextHold(BookCopy copy) {                    // caller holds the title lock
            Deque<String> q = holdQueues.get(copy.book.isbn());
            if (q == null || q.isEmpty()) return;
            String next = q.pollFirst();
            copy.status = CopyStatus.RESERVED;
            copy.reservedFor = next;
            copy.reservedUntil = today.get().plusDays(HOLD_PICKUP_DAYS);
            Member m = members.get(next);
            listeners.forEach(l -> l.holdReady(m, copy, copy.reservedUntil));
        }

        /** Lazy expiry: checked whenever the title is touched, no background thread needed. */
        private void expireStaleReservations(String isbn) {
            for (BookCopy c : copiesByIsbn.get(isbn))
                if (c.status == CopyStatus.RESERVED && today.get().isAfter(c.reservedUntil)) {
                    c.status = CopyStatus.AVAILABLE;
                    c.reservedFor = null;
                    assignToNextHold(c);                                     // offer it to the next person
                }
        }

        // ---- fines, reports ----
        void payFine(String memberId, long paise) {
            member(memberId).unpaidFinePaise.updateAndGet(f -> { if (paise > f) throw new LibraryException("Overpayment"); return f - paise; });
        }

        /** Daily job: notify overdue members. */
        int sendOverdueNotices() {
            LocalDate now = today.get();
            int n = 0;
            for (Loan l : loans.values())
                if (l.returnedOn == null && now.isAfter(l.dueOn)) {
                    long days = ChronoUnit.DAYS.between(l.dueOn, now);
                    listeners.forEach(x -> x.overdue(l, days));
                    n++;
                }
            return n;
        }

        long availableCopies(String isbn) { return copiesByIsbn.get(isbn).stream().filter(c -> c.status == CopyStatus.AVAILABLE).count(); }
        private Member member(String id) { Member m = members.get(id); if (m == null) throw new LibraryException("Unknown member " + id); return m; }
    }

    // =====================================================================
    // [demo]
    // =====================================================================

    public static void main(String[] args) throws Exception {
        AtomicReference<LocalDate> date = new AtomicReference<>(LocalDate.of(2026, 1, 1));
        LibraryService lib = new LibraryService(new PerDayFine(500, 20_000), date::get);
        NotificationLog notes = new NotificationLog();
        lib.addListener(notes);
        lib.addBook(new Book("111", "Clean Code", "Robert Martin", "Software"), 1);
        lib.addBook(new Book("222", "Clean Architecture", "Robert Martin", "Software"), 2);
        lib.addBook(new Book("333", "The Pragmatic Programmer", "Andy Hunt", "Software"), 1);
        lib.register("asha", "Asha", MembershipTier.STUDENT);
        lib.register("ravi", "Ravi", MembershipTier.STANDARD);
        lib.register("meera", "Meera", MembershipTier.PREMIUM);

        System.out.println("1. Search through inverted indexes");
        check("title word 'clean'", 2, lib.catalog.byTitleWord("clean").size());
        check("author", "[Clean Architecture, Clean Code]", lib.catalog.byAuthor("Robert Martin").stream().map(Book::title).toList());

        System.out.println("2. Checkout rules");
        Loan l1 = lib.checkout("asha", "111");
        check("due date = 14 days for a student", LocalDate.of(2026, 1, 15), l1.dueOn);
        expectThrows("no copies left", () -> lib.checkout("ravi", "111"));
        lib.checkout("asha", "222");
        lib.checkout("asha", "333");
        lib.addBook(new Book("444", "Refactoring", "Martin Fowler", "Software"), 1);
        expectThrows("student limit of 3", () -> lib.checkout("asha", "444"));

        System.out.println("3. Holds: FIFO queue per title, copy reserved for the first in line");
        check("ravi is #1 in line", 1, lib.placeHold("ravi", "111"));
        check("meera is #2", 2, lib.placeHold("meera", "111"));
        expectThrows("asha can't renew (others waiting)", () -> lib.renew(l1.id));
        date.set(LocalDate.of(2026, 1, 20));                               // 5 days late
        check("late return fine = 5 x Rs 5", 2500L, lib.returnCopy("111-1"));
        check("copy reserved, not free", 0L, lib.availableCopies("111"));
        expectThrows("meera can't jump the queue", () -> lib.checkout("meera", "111"));
        check("ravi got it", "ravi", lib.checkout("ravi", "111").member.id);
        check("notification sent", true, notes.sent.get(0).contains("to ravi"));

        System.out.println("4. Reservation expiry (lazy, no background thread)");
        Loan r = lib.loans.values().stream().filter(x -> x.member.id.equals("ravi")).findFirst().get();
        lib.returnCopy(r.copy.barcode);                                    // now reserved for meera until Jan 23
        date.set(LocalDate.of(2026, 1, 30));                               // meera never came
        check("after expiry anyone can borrow it", "ravi", lib.checkout("ravi", "111").member.id);

        System.out.println("5. Renewals and fines");
        Loan m1 = lib.checkout("meera", "444");
        check("renewed: due +30 days", m1.dueOn.plusDays(30), lib.renew(m1.id).dueOn);
        lib.renew(m1.id); lib.renew(m1.id);
        expectThrows("premium max 3 renewals", () -> lib.renew(m1.id));
        date.set(date.get().plusDays(200));
        check("overdue notices", 4, lib.sendOverdueNotices());
        check("fine capped at Rs 200", 20_000L, lib.returnCopy(m1.copy.barcode));
        expectThrows("blocked by unpaid fines", () -> lib.checkout("meera", "111"));
        lib.payFine("meera", 20_000);
        check("fine cleared", 0L, lib.members.get("meera").unpaidFinePaise.get());

        System.out.println("6. Fine policy is a Strategy");
        FinePolicy uni = new GraceThenEscalatingFine(2, 1000);
        Loan fake = new Loan("x", null, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10));
        check("within grace", 0L, uni.finePaise(fake, LocalDate.of(2026, 1, 12)));
        check("12 days late - 2 grace: 7x10 + 3x20", 13_000L, uni.finePaise(fake, LocalDate.of(2026, 1, 22)));

        System.out.println("7. Concurrency: 30 members race for 3 copies of a new title");
        LibraryService race = new LibraryService(new PerDayFine(500, 20_000), date::get);
        race.addBook(new Book("999", "Designing Data-Intensive Applications", "Martin Kleppmann", "Systems"), 3);
        for (int i = 0; i < 30; i++) race.register("m" + i, "M" + i, MembershipTier.STANDARD);
        AtomicInteger ok = new AtomicInteger();
        CountDownLatch go = new CountDownLatch(1);
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            String id = "m" + i;
            Thread t = new Thread(() -> { try { go.await(); race.checkout(id, "999"); ok.incrementAndGet(); } catch (Exception ignored) {} });
            t.start(); ts.add(t);
        }
        go.countDown();
        for (Thread t : ts) t.join();
        check("exactly 3 loans", 3, ok.get());
        check("no copy lent twice", 3L, race.loans.values().stream().map(l -> l.copy.barcode).distinct().count());
        summary();
    }

    static void expectThrows(String label, Runnable r) {
        try { r.run(); check(label, "LibraryException", "no exception"); }
        catch (LibraryException e) { check(label, "LibraryException", "LibraryException"); System.out.println("         (" + e.getMessage() + ")"); }
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
