# 03 · Library Management System (explained simply)

| | |
|---|---|
| **Code** | [LibraryManagementSystem.java](LibraryManagementSystem.java) |
| **Run** | `java LibraryManagementSystem.java` (24 self-checks, including a 30-member race) |
| **Design notes** | [README.md](README.md) (class diagram, more Q&A) |
| **Prerequisite** | [How to think in LLD + the concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md) |

This question looks easy, but it hides the most important modelling insight in all of LLD: **a "book" is two different things**. It also tests business rules (limits, fines, renewals), a fair waiting queue, and the "last copy" race.

---

## 1. The problem in plain words

A library has **books** with several **copies** each. **Members** can:
- **search** the catalogue by title word, author or subject;
- **borrow** a copy (within their limit) and **return** it (paying a fine if it's late);
- **renew** a loan (a limited number of times, and not if others are waiting);
- place a **hold** when no copy is free, and be told when one is set aside for them.

Different membership tiers (student, standard, premium) get different limits.

---

## 2. How to think about it (the 7 steps)

### Step 1 · Clarify

| Question to ask | Answer we'll assume |
|---|---|
| Several copies of the same book? | **Yes**, each with its own barcode |
| Membership types? | Student, Standard, Premium, with different loan limits, loan lengths and renewals |
| Fines? | Per day late, capped; **the rule may change** |
| Holds (reservations)? | Yes: a first-come-first-served queue **per title**; a returned copy is set aside for the first person |
| How long is a held copy kept? | 3 days, then it goes to the next person (or back on the shelf) |
| Search? | By title word, author, subject |
| Payments, e-books, multiple branches? | Out of scope |

### Step 2 · Requirements
1. Search the catalogue.
2. Checkout with rules: a copy must be available; the member is under their loan limit; their unpaid fines are not too high.
3. Return: compute the fine; if someone is waiting, reserve the copy for them and notify them.
4. Renew: under the renewal limit, not overdue, nobody waiting.
5. Holds: a FIFO queue per title; reservations expire.
6. Thread-safe: two members must never get the last copy.

### Step 3 · Entities: the #1 insight

> "**Book**" is two things:
> - **`Book`** = the **title** (ISBN, title, author). That's what you **search for** and **place a hold on**.
> - **`BookCopy`** = **one physical item** (barcode, status). That's what you **lend**, **lose** and **put on a shelf**.
>
> If you mix them up, you can't model "the library has 3 copies of Clean Code" or "I'll take **any** copy".

| Noun | Becomes |
|---|---|
| book (title) | `record Book(isbn, title, author, subject)` |
| copy | `class BookCopy(barcode, book, status)` |
| copy status | `enum CopyStatus { AVAILABLE, ON_LOAN, RESERVED, LOST }` |
| member | `class Member(id, name, tier)` |
| membership tier | `enum MembershipTier` (with its limits as data) |
| loan | `class Loan(copy, member, issued, due, returned, renewals)` |
| hold | a queue of member ids per ISBN |
| fine | a `long` computed by a `FinePolicy` |
| catalogue | `class Catalog` (search) |
| the library | `class LibraryService` (all the rules) |

### Step 4 · Relationships

```
Book (title) 1 ──── many BookCopy (physical items)
Member 1 ──── many Loan ──── 1 BookCopy
LibraryService
   ├── Catalog            (search, with indexes)
   ├── hold queue per ISBN (who's waiting, in order)
   ├── FinePolicy          (how much a late return costs)
   └── LibraryListener(s)  (notifications)
```

**Tiers are data, not subclasses.** A premium member doesn't **behave** differently; they just have bigger numbers. So `MembershipTier` is an enum with fields, and a new tier is one line.

### Step 5 · API

```java
Loan checkout(String memberId, String isbn)
long returnCopy(String barcode)              // returns the fine charged
Loan renew(String loanId)
int  placeHold(String memberId, String isbn) // returns your position in the queue
void payFine(String memberId, long paise)
int  sendOverdueNotices()                    // a daily job
```

### Step 6 · Patterns

| What changes / matters | Pattern |
|---|---|
| how fines are computed | **Strategy** (`FinePolicy`) |
| who's told when a hold is ready or a loan is overdue | **Observer** (`LibraryListener`) |
| searching without scanning every book | an **inverted index** (a data structure, not a pattern) |
| searching vs lending are separate capabilities | **Interface segregation** (`Searchable`) |

### Step 7 · Concurrency
The contended resource is **"the available copies of one title"**. Two members grabbing the last copy must not both succeed, so there's **one lock per ISBN**. Different titles never block each other.

---

## 3. Java concepts used in this design

| Concept | In this design | Simple meaning |
|---|---|---|
| `record` | `Book` | immutable title data |
| `enum` with fields | `MembershipTier(maxLoans, loanDays, maxRenewals)` | each tier carries its own numbers |
| `LocalDate`, `ChronoUnit.DAYS.between` | due dates and fines | dates without time; count the days between two dates |
| `Supplier<LocalDate>` | `today` | "today" is passed in, so tests can jump forward in time |
| `ReentrantLock` per ISBN | `titleLocks` | one lock per title |
| `ArrayDeque` | the hold queue | a fast queue: `addLast` to join, `pollFirst` to take the next person |
| `computeIfAbsent` | building indexes and queues | create the entry the first time it's needed |
| streams (`filter`, `findFirst`, `orElseGet`) | finding a copy | "the first copy matching this condition, otherwise try this" |
| `AtomicLong.updateAndGet` | paying a fine | update a number atomically with a function |
| interface `default` methods | `LibraryListener` | listeners only override the events they care about |
| `String.split("\\W+")` | splitting a title into words | split on anything that isn't a letter or digit |

---

## 4. The code, piece by piece

### 4.1 Title vs copy

```java
record Book(String isbn, String title, String author, String subject) {}

enum CopyStatus { AVAILABLE, ON_LOAN, RESERVED, LOST }

static class BookCopy {
    final String barcode;
    final Book book;
    CopyStatus status = CopyStatus.AVAILABLE;
    String reservedFor;                                 // member id, when RESERVED for a hold
    LocalDate reservedUntil;
}
```

```
AVAILABLE ──checkout──> ON_LOAN ──return (nobody waiting)──> AVAILABLE
                          │
                          └──return (someone waiting)──> RESERVED ──their checkout──> ON_LOAN
                                                            │
                                                            └──3 days pass──> next person, or AVAILABLE
```

A copy's barcode looks like `111-1`: the ISBN, then the copy number.

### 4.2 Tiers are data

```java
enum MembershipTier {
    STUDENT(3, 14, 1), STANDARD(5, 21, 2), PREMIUM(10, 30, 3);
    final int maxLoans, loanDays, maxRenewals;
}
```

| Tier | Max loans | Loan days | Max renewals |
|---|---|---|---|
| Student | 3 | 14 | 1 |
| Standard | 5 | 21 | 2 |
| Premium | 10 | 30 | 3 |

### 4.3 Fines are a Strategy

```java
interface FinePolicy { long finePaise(Loan loan, LocalDate returnedOn); }

/** Rs 5 per day late, capped at Rs 200 - the usual public-library rule. */
static class PerDayFine implements FinePolicy {
    public long finePaise(Loan loan, LocalDate returnedOn) {
        long late = ChronoUnit.DAYS.between(loan.dueOn, returnedOn);
        return late <= 0 ? 0 : Math.min(cap, late * perDay);
    }
}

/** Grace period, then escalating: days 1-7 at x, after that 2x. */
static class GraceThenEscalatingFine implements FinePolicy {
    public long finePaise(Loan loan, LocalDate returnedOn) {
        long late = ChronoUnit.DAYS.between(loan.dueOn, returnedOn) - graceDays;
        if (late <= 0) return 0;
        return Math.min(late, 7) * base + Math.max(0, late - 7) * base * 2;
    }
}
```

A public library and a university library can use different rules by passing a different `FinePolicy` to the service. Nothing else changes.

### 4.4 Search with an inverted index

```java
static class Catalog implements Searchable {
    private final Map<String, Book> byIsbn = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> titleIndex = ..., authorIndex = ..., subjectIndex = ...;

    void add(Book b) {
        byIsbn.put(b.isbn(), b);
        for (String w : b.title().toLowerCase().split("\\W+")) index(titleIndex, w, b);
        index(authorIndex, b.author().toLowerCase(), b);
        index(subjectIndex, b.subject().toLowerCase(), b);
    }
}
```

An **inverted index** works like the index at the back of a textbook: instead of reading every page to find "clean", you look up "clean" and get the page numbers. Here: word → set of ISBNs.

```
titleIndex:  "clean"        → {111, 222}
             "code"         → {111}
             "architecture" → {222}
authorIndex: "robert martin" → {111, 222}
```

A search is one map lookup instead of a scan over every book. At scale, this is exactly what Elasticsearch and Lucene do.

### 4.5 One lock per title

```java
private <T> T withTitle(String isbn, Supplier<T> action) {
    ReentrantLock lock = titleLocks.computeIfAbsent(isbn, k -> new ReentrantLock());
    lock.lock();
    try { return action.get(); } finally { lock.unlock(); }
}
```

The per-key lock from [tool 1](../00-LLD-Playbook/ConcurrencyToolkit.md#tool-1--check-then-act--make-the-check-and-the-act-one-atomic-step). Every change to a title's copies, holds or loans happens inside `withTitle(isbn, …)`.

### 4.6 Checkout

```java
Loan checkout(String memberId, String isbn) {
    Member m = member(memberId);
    if (catalog.get(isbn) == null) throw new LibraryException("Unknown ISBN " + isbn);
    return withTitle(isbn, () -> {
        // rules first (fail fast, nothing changed yet)
        if (m.activeLoanIds.size() >= m.tier.maxLoans) throw new LibraryException(m.id + " reached the limit of " + m.tier.maxLoans);
        if (m.unpaidFinePaise.get() > MAX_UNPAID_FINE_PAISE) throw new LibraryException(m.id + " has unpaid fines");
        expireStaleReservations(isbn);
        // 1) a copy reserved for THIS member, else 2) a free copy
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
```

In plain words:
1. **Check the rules first** (loan limit, unpaid fines), before changing anything. That's called **fail fast**.
2. Release any reservations that have expired (section 4.8).
3. Pick a copy: first one **reserved for this member**, otherwise any **available** one. Copies reserved for **someone else** are never picked, so nobody can jump the queue.
4. Mark it ON_LOAN, create the loan with a due date from the member's tier.

### 4.7 Return: fine, then hand it to the next person

```java
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
```

The returned copy goes to the **first person in the queue** (FIFO), is reserved for 3 days, and they get a "your hold is ready" notification.

### 4.8 Lazy expiry of reservations

```java
private void expireStaleReservations(String isbn) {
    for (BookCopy c : copiesByIsbn.get(isbn))
        if (c.status == CopyStatus.RESERVED && today.get().isAfter(c.reservedUntil)) {
            c.status = CopyStatus.AVAILABLE;
            c.reservedFor = null;
            assignToNextHold(c);                                     // offer it to the next person
        }
}
```

No background timer thread. Whenever someone touches the title, stale reservations are cleaned up first (**lazy expiry**, like the seat holds in [tool 5](../00-LLD-Playbook/ConcurrencyToolkit.md#tool-5--holds-with-expiry-ttl--reservations-that-free-themselves)).

### 4.9 Renewal rules

```java
Loan renew(String loanId) {
    ...
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
```

### 4.10 Paying fines atomically

```java
void payFine(String memberId, long paise) {
    member(memberId).unpaidFinePaise.updateAndGet(f -> { if (paise > f) throw new LibraryException("Overpayment"); return f - paise; });
}
```

`updateAndGet` applies the function atomically (retrying internally if another thread changed the value meanwhile).

---

## 5. Dry run (from the real output)

Members: Asha (student), Ravi (standard), Meera (premium). Clean Code (ISBN 111) has 1 copy; Clean Architecture (222) has 2; The Pragmatic Programmer (333) has 1. The date starts at 1 Jan 2026.

| Step | What happens | Result |
|---|---|---|
| search "clean" | `titleIndex["clean"]` | 2 books |
| search author "Robert Martin" | `authorIndex` | Clean Architecture, Clean Code |
| Asha borrows 111 | student → 14 days | due 15 Jan |
| Ravi borrows 111 | no copy left | "No copy of 111 available; place a hold" |
| Asha borrows 222 and 333, then tries 444 | the student limit is 3 | "asha reached the limit of 3" |
| Ravi holds 111 | queue: [ravi] | position 1 |
| Meera holds 111 | queue: [ravi, meera] | position 2 |
| Asha tries to renew 111 | others are waiting | refused |
| 20 Jan: Asha returns 111 | 5 days late × ₹5 | fine ₹25 (2500 paise); the copy is RESERVED for Ravi |
| Meera tries to borrow 111 | the copy is reserved for Ravi | refused |
| Ravi borrows 111 | it's reserved for him | ✓ (and he was notified) |
| Ravi returns 111 | queue: [meera] | reserved for Meera until 23 Jan |
| 30 Jan: Ravi borrows 111 | Meera's reservation expired, the queue is empty → available | ✓ Ravi |
| Meera borrows 444, renews 3 times | premium: +30 days each, max 3 | the 4th renewal is refused |
| +200 days: overdue notices | Asha's 2 loans, Ravi's, Meera's | 4 notices |
| Meera returns 444 very late | capped | ₹200 (20,000 paise) |
| Meera borrows 111 | unpaid ₹200 > ₹100 limit | refused |
| Meera pays ₹200 | | fine 0 |

**The university fine policy** (2 grace days, ₹10/day for 7 days, then ₹20/day): due 10 Jan, returned 22 Jan = 12 days late − 2 grace = 10 days → 7 × ₹10 + 3 × ₹20 = **₹130**.

**The race:** 30 members try to borrow 3 copies of a new title at once → **exactly 3 loans**, all different copies.

---

## 6. Concurrency, explained simply

**The danger:** 30 members click Borrow on the last few copies at the same instant. Without a lock, several could read "copy 1 is AVAILABLE" and all borrow it.

**The protection:** every change for one title runs inside that title's lock. The check ("is there a free copy? is the member under the limit?") and the act ("mark it ON_LOAN") happen together, with nobody else in between.

**Why per title and not one big lock?** People borrowing *Harry Potter* shouldn't wait for people borrowing *Clean Code*.

**Note:** the hold queue (`ArrayDeque`) and the copy fields are **not** thread-safe on their own; they're safe because they're only touched while holding the title's lock.

---

## 7. Edge cases and common mistakes

**Edge cases handled:** unknown member/ISBN/barcode, returning a copy that isn't on loan, renewing an overdue loan, holding a title that has a free copy ("just borrow it"), holding twice, overpaying a fine, a reservation nobody picks up.

**Common mistakes:**
1. **One class for title and copy.** Then "3 copies" and "hold any copy" don't work.
2. Holds on a specific copy instead of a title (you'd wait for copy #2 while copy #1 sits on the shelf).
3. Subclasses for each membership tier.
4. Scanning every book for each search.
5. Letting a returned copy go straight back to AVAILABLE even when someone is waiting (the queue gets skipped).
6. A background thread just to expire reservations (lazy expiry is simpler).

---

## 8. Interview questions and answers

**Q1. Why separate `Book` and `BookCopy`?**
> A `Book` is the title (searched for, held); a `BookCopy` is a physical item (lent, lost, shelved). The library owns many copies of one title, and a hold should be satisfied by any copy. One class can't model both.

**Q2. How do you prevent two members borrowing the last copy?**
> All changes for a title happen under that title's lock: the availability check and the status change are atomic together. Different titles use different locks, so they don't block each other.

**Q3. How does the hold queue stay fair?**
> It's a FIFO queue per title. A returned copy is reserved for the first person and they're notified; checkout only takes copies that are AVAILABLE or reserved for the borrower, so nobody can jump the queue. Renewals are refused while people are waiting.

**Q4. How do reservations expire without a timer?**
> Lazily: whenever a title is touched, any reservation past its pickup date is released and offered to the next person in line.

**Q5. Why is the fine policy a strategy?**
> Different libraries (public vs university) have different rules, and they change. The service just calls `finePolicy.finePaise(...)`.

**Q6. How does search avoid scanning all books?**
> Inverted indexes: word → ISBNs for title words, author and subject. A search is a map lookup.

**Q7. Why is the tier an enum, not a subclass?**
> Tiers differ only in numbers (limits, days, renewals), not behaviour. Data on an enum keeps it simple; a new tier is one line.

### Follow-up questions

**F1. "Add lost books."**
> Mark the copy LOST, close the loan with a replacement charge (another rule for the fine policy, or a separate charge), and remove it from availability.

**F2. "Add multiple branches."**
> Give `BookCopy` a branch; checkout prefers copies at the member's branch; a hold can request a transfer between branches.

**F3. "Notify by email and SMS."**
> More `LibraryListener` implementations. The service doesn't change.

**F4. "Members can hold at most 3 titles."**
> Track active holds per member and check the limit in `placeHold`, inside the title lock.

**F5. "Scale to a big system with a database."**
> Copies become rows; checkout is a conditional update (`UPDATE copy SET status='ON_LOAN' WHERE barcode=? AND status='AVAILABLE'`) or `SELECT … FOR UPDATE` on the title's copies. The hold queue becomes a table ordered by creation time. Search moves to Elasticsearch.

**F6. "Run the overdue notices every night."**
> A scheduled job calls `sendOverdueNotices()` (see [13 · Job Scheduler](../13-Job-Scheduler/JobSchedulerSystem.md)).

---

## 9. Running the file

```bash
java LibraryManagementSystem.java      # 24/24 tests passed (including 30 members racing for 3 copies)
```

**One-line summary:** *"Separate the title (Book) from the physical copy (BookCopy); keep tier limits as enum data and fines as a Strategy; search with inverted indexes; serve holds from a FIFO queue per title with lazily expiring reservations; and do every change for a title under that title's lock."*
