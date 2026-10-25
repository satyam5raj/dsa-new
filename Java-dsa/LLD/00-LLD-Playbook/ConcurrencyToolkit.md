# 00 · How to Think in LLD + the Concurrency Toolkit (explained simply)

| | |
|---|---|
| **Code** | [ConcurrencyToolkit.java](ConcurrencyToolkit.java) |
| **Run** | `java ConcurrencyToolkit.java` |
| **The full playbook** | [README.md](README.md) (the interviewer's grading sheet, pattern table, SOLID) |

**Read this before any of the 19 designs.** Part A explains what Low-Level Design is and how to think about it, step by step, in plain words. Part B explains the six concurrency tools that come up in almost every design, each with an everyday analogy, the code, and the real output of running it.

---

# Part A · What LLD is, and how to think about it

## A1. What is Low-Level Design?

In an LLD interview you're asked something like "design a parking lot". You're **not** asked to draw servers and databases (that's High-Level Design). You're asked to design the **code**:
- which **classes** exist,
- what **data** each one holds,
- what **methods** each one has,
- how they **talk to each other**,
- and how the design stays **correct** (no double booking, no lost money) and **easy to change** (a new vehicle type, a new pricing rule).

Think of it like designing a restaurant kitchen. You decide who the chef is, who the waiter is, which counter holds which dishes, and the rules ("only the chef touches the stove"). You don't need to decide which city the restaurant is in.

## A2. What the interviewer is really checking

| They want to see that you… | What it looks like |
|---|---|
| **ask before you build** | "Can two cars arrive at the same gate at the same moment? Do we need payments?" |
| **turn real things into clean classes** | `ParkingSpot`, `Ticket`, `Vehicle`, each with its own rules |
| **know what will change and prepare for it** | "Pricing rules change often, so I'll put pricing behind an interface." |
| **think about two people at once** | "Two users click Book on the same seat. Only one must win." |
| **handle the unhappy paths** | the payment fails, the user retries, the ticket is lost |
| **explain your choices** | "I chose X over Y because…" |

**The code matters less than the reasoning you say out loud.**

## A3. The 7-step method, in simple words

Use the same 7 steps for every design. In all 19 notes, section 2 ("How to think about it") walks through these steps for that problem.

| Step | Time | What you do | Simple way to remember it |
|---|---|---|---|
| **1. Clarify** | 5 min | Ask questions: who uses it, how big, what's out of scope | "Don't cook before you know what they ordered." |
| **2. Requirements** | 3 min | Write two lists: what it must do (functional) and how well (concurrency, extensibility) | "Write the order down." |
| **3. Entities** | 7 min | Underline the **nouns** in the requirements → classes; the **verbs** → methods | "Who are the people and things in the story?" |
| **4. Relationships** | 5 min | Who **has** what, who **is** what, and who **owns** each rule | "Who is responsible for what?" |
| **5. APIs and flows** | 5 min | The public methods, and walk through 1–2 main stories step by step | "Act out the main scene." |
| **6. Patterns** | 5 min | Find what will **change** and put an interface exactly there | "Leave a door where you'll need one." |
| **7. Code + concurrency + extensions** | 20 min | Write the core classes, protect shared data, discuss "what if…" | "Build it, lock it, grow it." |

### Step 3 in practice: nouns become classes

Take this requirement: "A **driver** parks a **vehicle** in a free **spot** on a **floor** and gets a **ticket**. At exit, they **pay** a **fee**."

| Noun | Class? | Why |
|---|---|---|
| vehicle | yes: `Vehicle` | has a plate and a type |
| spot | yes: `ParkingSpot` | has an id, a size, and is free or taken |
| floor | yes: `Floor` | holds spots |
| ticket | yes: `Ticket` | records who parked where and when |
| fee | **no**, just a number | a value computed by pricing |
| driver | often no | the system doesn't need to store drivers |

| Verb | Method |
|---|---|
| parks | `ParkingLot.park(vehicle)` |
| pays | `ParkingLot.exit(ticketId)` |

### Step 4 in practice: one owner for every rule

"A spot can hold only one vehicle" should live in **one** place: the `ParkingSpot` class. If three classes all check it, sooner or later one of them gets it wrong.

Prefer **has-a** (composition: a `Floor` *has* spots) over **is-a** (inheritance). Use inheritance only for a true "is a kind of" with shared behaviour. Even then, ask whether an enum plus a strategy is simpler.

### Step 6 in practice: patterns only where things change

A **design pattern** is just a well-known shape of code for a common problem. Don't add patterns to look clever: every interface needs a reason. The ones you'll see most:

| Pattern | Simple meaning | Example |
|---|---|---|
| **Strategy** | "swap the algorithm": an interface with several implementations | hourly vs flat pricing |
| **State** | "the object behaves differently depending on its current status" | a vending machine: idle → has money → dispensing |
| **Observer** | "tell everyone who's interested when something happens" | update the display board when a spot is taken |
| **Factory** | "a method that creates the right kind of object for you" | create a car, bike or truck from a string |
| **Builder** | "build an object with many optional settings step by step" | `ParkingLot.builder().floors(3).pricing(...).build()` |
| **Decorator** | "wrap an object to add a feature, without changing it" | add retries around a notification sender |
| **Command** | "turn a request into an object you can queue, log or undo" | an elevator request |

(The patterns themselves are taught in [OOP/06-Design-Patterns](../../OOP/06-Design-Patterns/).)

## A4. Five modelling rules that prevent most bugs

1. **Money is never `double`.** `0.1 + 0.2` is `0.30000000000000004` in a `double`. Use `long` in paise or cents (₹12.50 = 1250 paise).
2. **Status changes go through one method** that checks the change is allowed: `order.transitionTo(SHIPPED)` throws if the order was never paid.
3. **The system creates IDs**, not the caller (a `UUID` or an `AtomicLong` counter).
4. **Time is passed in** (a clock object), never read directly inside logic. Then a test can say "pretend 11 minutes passed" instantly (see tool 5 below).
5. **Return read-only views** of internal lists (`List.copyOf(...)`), so callers can't break your rules from outside.

---

# Part B · The concurrency toolkit

## B0. Why concurrency matters at all

A real system serves many users **at the same time**, on many threads. Two threads can read and write the same data in an interleaved order you didn't plan for. The classic bug:

```
Thread 1: is seat A1 free?  → yes
Thread 2: is seat A1 free?  → yes        (thread 1 hasn't marked it yet!)
Thread 1: mark A1 booked
Thread 2: mark A1 booked                 → two people own the same seat
```

**Always start by naming what is contended**: "the contended resource is a single seat" (or a parking spot, or a wallet balance). Then pick the **narrowest** tool that protects exactly that.

## B1. How the toolkit tests races: the "starting gun"

```java
static int race(int n, Supplier<Boolean> task) throws InterruptedException {
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger wins = new AtomicInteger();
    List<Thread> ts = new ArrayList<>();
    for (int i = 0; i < n; i++) {
        Thread t = new Thread(() -> {
            try { start.await(); } catch (InterruptedException e) { return; }
            if (task.get()) wins.incrementAndGet();
        });
        t.start();
        ts.add(t);
    }
    start.countDown();                                                // starting gun
    for (Thread t : ts) t.join();
    return wins.get();
}
```

Like runners at a starting line: every thread is created and waits at `start.await()`. Then `start.countDown()` fires the gun and they all run at the same instant, which makes a race as likely as possible. `wins` counts how many threads succeeded. `t.join()` waits for every thread to finish.

| Java tool | Simple meaning |
|---|---|
| `Thread` | a separate worker running code at the same time as others |
| `CountDownLatch(1)` | a gate that opens once, for everyone |
| `AtomicInteger` | a counter that's safe for many threads to increment |
| `join()` | "wait until that worker is done" |

---

## Tool 1 · Check-then-act → make the check and the act one atomic step

### The analogy
Two people look at the last cinema seat on two different screens. Both see "free", both press Book. If "look" and "book" are separate steps, both succeed.

### The broken code

```java
static class UnsafeSeatMap {
    final Map<String, SeatStatus> seats = new ConcurrentHashMap<>();   // a thread-safe MAP is NOT enough!

    boolean book(String seat) {
        if (seats.get(seat) == SeatStatus.AVAILABLE) {                // CHECK
            sleepQuietly(1);                                          // widen the window (a real app does DB/IO here)
            seats.put(seat, SeatStatus.BOOKED);                       // ACT  - another thread may have passed the check too
            return true;
        }
        return false;
    }
}
```

`ConcurrentHashMap` makes **each single** `get` and `put` safe, but not the **pair** of them. Between the check and the act, other threads slip in.

**Real output:** `unsafe (ConcurrentHashMap, no atomicity): 20 users 'booked' the same seat`.

### Fix A: one lock per seat

```java
static class KeyedLocks {
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    <T> T withLock(String key, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());   // one lock object per key
        lock.lock();
        try { return action.get(); }
        finally { lock.unlock(); }                                    // ALWAYS unlock in finally
    }
}
```

| Line | Simple meaning |
|---|---|
| `computeIfAbsent(key, …)` | get this seat's lock, creating it the first time (atomically, so two threads never create two locks for one seat) |
| `lock.lock()` | wait until nobody else holds this seat's lock |
| `try { … } finally { unlock }` | always release the lock, even if the action throws |

**Why per seat, not one big lock?** A single global lock is also correct, but then booking seat A1 in Mumbai waits for seat B7 in Delhi. With one lock per seat, people booking **different** seats never wait for each other.

**Real output:** `per-key lock: exactly one winner -> 1`.

### Fix B: an atomic map operation (no explicit lock)

```java
static boolean bookWithReplace(Map<String, SeatStatus> seats, String seat) {
    return seats.replace(seat, SeatStatus.AVAILABLE, SeatStatus.BOOKED);
}
```

`replace(key, expected, newValue)` means "change it to BOOKED **only if** it's still AVAILABLE", done as one indivisible step. It returns `true` for exactly one thread. This is called **compare-and-set (CAS)**.

**Real output:** `CHM.replace (CAS): exactly one winner -> 1`.

### In a real database
`UPDATE seat SET status='BOOKED' WHERE id='A1' AND status='AVAILABLE'`, then check that exactly 1 row was updated. Or a **unique constraint** on (show_id, seat_id) in the bookings table.

---

## Tool 2 · Optimistic locking → version + compare-and-set, retry on conflict

### The analogy
Editing a shared document: you note "this is version 7", make your changes, and save **only if** it's still version 7. If someone saved version 8 meanwhile, you reload and redo your change.

### The code

```java
record Versioned(long balance, long version) {}

static class OptimisticAccount {
    final AtomicReference<Versioned> state = new AtomicReference<>(new Versioned(0, 0));
    final AtomicLong conflicts = new AtomicLong();

    void deposit(long amount) {
        while (true) {
            Versioned cur = state.get();                              // read (no lock)
            Versioned next = new Versioned(cur.balance() + amount, cur.version() + 1);
            if (state.compareAndSet(cur, next)) return;               // write only if nobody changed it meanwhile
            conflicts.incrementAndGet();                              // someone won the race -> retry with fresh data
        }
    }
}
```

| Line | Simple meaning |
|---|---|
| `record Versioned(...)` | an immutable little object: a balance and its version |
| `AtomicReference` | a box holding one object, which can be swapped safely |
| `compareAndSet(cur, next)` | "if the box still holds exactly `cur`, put `next` in; otherwise fail" |
| the `while (true)` loop | on failure, read the fresh value and try again |

**Real output:** 8 threads × 1,000 deposits → `no lost updates -> 8000`, `version counts every write -> 8000`, with about 20,000 conflicts retried in one run (the exact number changes every run).

### Optimistic vs pessimistic
- **Pessimistic** (tool 1, a lock): "lock first, then work". Good when conflicts are frequent.
- **Optimistic** (this tool): "work first, check at the end, retry if someone beat you". Good when conflicts are rare, and nobody waits while a user is thinking.

### In a database
`UPDATE account SET balance=?, version=version+1 WHERE id=? AND version=?`; 0 rows updated means a conflict, so reload and retry.

---

## Tool 3 · Lock ordering → always lock in the same global order

### The analogy
Two people each need both the salt and the pepper. One grabs salt and waits for pepper; the other grabs pepper and waits for salt. They wait forever: that's a **deadlock**. If everyone always picks up the salt first, it can't happen.

### The code

```java
static boolean transfer(Account from, Account to, long amount) {
    Account first = from.id < to.id ? from : to;
    Account second = from.id < to.id ? to : from;
    first.lock.lock();
    try {
        second.lock.lock();
        try {
            if (from.balance < amount) return false;                  // invariant: no negative balance
            from.balance -= amount;
            to.balance += amount;
            return true;
        } finally { second.lock.unlock(); }
    } finally { first.lock.unlock(); }
}
```

Whatever the direction of the transfer (A → B or B → A), the account with the **smaller id** is always locked first. So two transfers in opposite directions can never each hold one lock and wait for the other.

**Real output:** 100,000 transfers each way at once → `finished (no deadlock) -> false` (no thread is still stuck) and `money conserved -> 2000000`.

---

## Tool 4 · Idempotency keys → a retry must not charge twice

### The analogy
You tap "Pay" and the screen freezes. You tap again. You should be charged **once**. The app sends the same "order-42" label both times, and the server remembers: "I already did order-42, here's the same receipt."

**Idempotent** means doing it twice has the same effect as doing it once.

### The code

```java
static class IdempotentPaymentService {
    private final ConcurrentHashMap<String, CompletableFuture<String>> results = new ConcurrentHashMap<>();
    final AtomicInteger actualCharges = new AtomicInteger();

    String charge(String idempotencyKey, long amount) {
        CompletableFuture<String> mine = new CompletableFuture<>();
        CompletableFuture<String> existing = results.putIfAbsent(idempotencyKey, mine);
        if (existing != null) return existing.join();                 // duplicate: replay the first result
        try {
            actualCharges.incrementAndGet();                          // the real side effect, exactly once
            sleepQuietly(5);
            String receipt = "receipt-" + idempotencyKey + "-" + amount;
            mine.complete(receipt);
            return receipt;
        } catch (RuntimeException e) {
            results.remove(idempotencyKey);                           // failed attempts may be retried
            mine.completeExceptionally(e);
            throw e;
        }
    }
}
```

| Line | Simple meaning |
|---|---|
| `CompletableFuture` | an "IOU" for a result that will arrive later |
| `putIfAbsent(key, mine)` | "store my IOU unless someone already stored one"; returns the existing IOU if there was one |
| `existing.join()` | a duplicate request waits for the first request's result |
| the `catch` | if the charge fails, forget the key so a later retry can try again |

**Why store a future, not just the receipt?** If two duplicates arrive at the same instant, the second one can't find a receipt yet (the first is still charging). With a future, the second simply waits for the first to finish.

**Real output:** 10 concurrent retries → `charged exactly once -> 1`, `every retry got the same receipt -> 1`.

### In production
Keys go into a database table with a **unique constraint**, together with the stored response and an expiry (for example 24 hours). Stripe's API works exactly this way.

---

## Tool 5 · Holds with expiry (TTL) → reservations that free themselves

### The analogy
A shop keeps an item aside for you for 10 minutes. If you don't come back, it goes back on the shelf automatically. Nobody has to remember to release it.

### The code

```java
static class FakeClock implements LongSupplier {
    private long now;
    public long getAsLong() { return now; }
    void advance(long ms) { now += ms; }
}

static class SeatHolds {
    record Hold(String user, long expiresAt) {}
    private final ConcurrentHashMap<String, Hold> holds = new ConcurrentHashMap<>();
    private final LongSupplier clock;
    private final long ttlMs;
    SeatHolds(LongSupplier clock, long ttlMs) { this.clock = clock; this.ttlMs = ttlMs; }

    /** Atomically: grant the hold if the seat is free OR the previous hold expired. */
    boolean hold(String seat, String user) {
        long now = clock.getAsLong();
        Hold result = holds.compute(seat, (k, cur) ->
                cur == null || cur.expiresAt() <= now ? new Hold(user, now + ttlMs) : cur);
        return result.user().equals(user) && result.expiresAt() == now + ttlMs;
    }

    boolean confirm(String seat, String user) {                      // only the current, unexpired holder
        Hold h = holds.get(seat);
        return h != null && h.user().equals(user) && h.expiresAt() > clock.getAsLong();
    }
}
```

| Idea | Simple meaning |
|---|---|
| **TTL** (time to live) | how long the hold lasts |
| `expiresAt` | store the **absolute** time it ends, not "10 minutes left" |
| `compute(seat, …)` | read and update this one seat's entry as a single atomic step |
| the lambda | "if there's no hold, or the old one expired, give it to this user; otherwise keep the old one" |
| `LongSupplier clock` | the time is **passed in**: production passes `System::currentTimeMillis`, tests pass a `FakeClock` |

There's no background "cleaner" thread: an expired hold is simply treated as free the next time someone looks (**lazy expiry**).

**Real output:**
```
PASS  alice holds A1                     -> true
PASS  bob can't while it's held          -> false
      (the clock moves forward 11 minutes, instantly)
PASS  alice's confirm fails after expiry -> false
PASS  bob gets it after expiry           -> true
```

---

## Tool 6 · Producer / consumer → a bounded queue between workers

### The analogy
A restaurant: waiters (producers) put orders on a rail, cooks (consumers) take them off. If the rail is full, waiters wait; if it's empty, cooks wait. The rail's limited size stops the kitchen from being flooded (**back-pressure**).

### The code

```java
static int producerConsumer(int items, int workers) throws InterruptedException {
    BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);       // BOUNDED -> producers block when full (back-pressure)
    AtomicInteger processed = new AtomicInteger();
    int POISON = -1;                                                  // "poison pill" tells a worker to stop
    List<Thread> ts = new ArrayList<>();
    for (int w = 0; w < workers; w++) {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    int x = queue.take();                             // blocks while empty
                    if (x == POISON) return;
                    processed.incrementAndGet();
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        t.start();
        ts.add(t);
    }
    for (int i = 0; i < items; i++) queue.put(i);                     // blocks while full
    for (int w = 0; w < workers; w++) queue.put(POISON);
    for (Thread t : ts) t.join();
    return processed.get();
}
```

| Line | Simple meaning |
|---|---|
| `ArrayBlockingQueue<>(10)` | a queue holding at most 10 items |
| `put` | add an item, waiting if the queue is full |
| `take` | remove an item, waiting if the queue is empty |
| **poison pill** | a special value that means "stop": one per worker, so every worker exits |

**Real output:** `all 10,000 items processed by 4 workers -> 10000`.

---

## B2. Which tool when? (the cheat sheet)

| Situation | Tool |
|---|---|
| "if it's free, take it" on one resource | **atomic check-and-act**: a per-key lock, or CAS (`replace`, `compareAndSet`) |
| many independent resources (seats, spots, accounts) | **one lock per resource**, not one global lock |
| updates that rarely conflict | **optimistic**: a version + CAS, retry on conflict |
| touching two resources at once | **lock in a global order** (by id) |
| a client may retry | **idempotency key** → store and replay the result |
| temporary reservations | **hold with an expiry**, checked lazily, with an injected clock |
| work handed between threads | **bounded `BlockingQueue`** |

**Say this in interviews:** "In memory I'd use a per-seat lock; across many servers I'd rely on a database unique constraint or a conditional update on a version column."

---

# Part C · Interview questions and follow-ups

**Q1. Why isn't `ConcurrentHashMap` enough to prevent double booking?**
> It makes each single operation safe, but "get, then put" is two operations. Another thread can act in between. You need the check and the act to be **one** atomic step: a lock around both, or a single atomic operation like `replace(key, expected, new)` or `compute`.

**Q2. A global lock vs a lock per key?**
> Both are correct. A global lock makes every operation in the system wait in one line, even for unrelated resources. A lock per key only makes operations on the **same** resource wait, so throughput scales with the number of resources.

**Q3. When do you prefer optimistic locking?**
> When conflicts are rare and you don't want to hold a lock during slow work (like a user filling in a form). You read a version, do the work, and write only if the version is unchanged; on a conflict you retry.

**Q4. What causes a deadlock, and how do you prevent it?**
> Two threads each hold one lock and wait for the other's. Prevent it by always acquiring multiple locks in the same global order (for example by id), by using `tryLock` with a timeout, or by avoiding holding two locks at all.

**Q5. What is idempotency, and how do you implement it?**
> Repeating a request has the same effect as doing it once. The client sends a unique key; the server atomically records "key → result" the first time and returns the stored result for every repeat. In a database: a unique constraint on the key column.

**Q6. Why store a future instead of the result in the idempotency map?**
> Two duplicates can arrive while the first is still running. With a future, the duplicate waits for the first one's result instead of either charging again or failing.

**Q7. How do you implement a seat hold that expires?**
> Store the absolute expiry time with the hold. When someone tries to hold the seat, treat an expired hold as free, atomically (`compute`). Confirm only if the caller is the current holder and the hold hasn't expired. Pass the clock in so tests can move time.

**Q8. Why inject the clock?**
> So tests can control time ("advance 11 minutes") instantly and deterministically, instead of sleeping or getting flaky results.

**Q9. What is back-pressure?**
> When consumers are slower than producers, a bounded queue makes producers wait instead of letting memory fill up. It's a natural brake on the system.

**Q10. How do these ideas look in a distributed system?**
> Per-key lock → a database row lock (`SELECT … FOR UPDATE`) or a Redis lock (`SET key value NX PX ttl`). CAS → a conditional `UPDATE … WHERE version = ?`. Idempotency → a table with a unique key. Holds → rows with an `expires_at` column. Queues → Kafka or SQS.

### Follow-up questions

**F1. "Your per-key lock map grows forever. Is that a problem?"**
> For a bounded set of keys (seats in a theatre) it's fine. For unbounded keys, use lock striping (a fixed array of locks, choose one by `hash(key) % N`) or remove locks when they're no longer needed.

**F2. "What if the idempotency map gets huge?"**
> Expire old keys after a retention window (like 24 hours), or store them in a database table with a TTL.

**F3. "Could you use `synchronized` instead of `ReentrantLock`?"**
> Yes, for simple cases. `ReentrantLock` adds `tryLock` (with a timeout), fairness options and multiple conditions, which help avoid deadlocks and support "give up after 2 seconds".

---

## Running the file

```bash
java ConcurrencyToolkit.java      # 13/13 tests passed (the unsafe version also prints how many users "booked" the same seat)
```

**One-line summary:** *"Name the contended resource, then make every check-and-act on it atomic (per-key lock or CAS), lock multiple resources in a fixed order, de-duplicate retries with idempotency keys, expire holds with an injected clock, and connect workers with a bounded queue."*
