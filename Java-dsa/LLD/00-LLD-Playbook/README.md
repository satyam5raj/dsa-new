# 00 · The LLD Playbook: how to think in a Low-Level Design interview

Read this first. Each of the 19 designs that follow uses the same method, so you practise one way of thinking 19 times instead of memorising 19 answers.

---

## 1. What the interviewer is actually grading

| Signal | Junior answer | Senior answer |
|---|---|---|
| Requirements | starts coding immediately | asks 5–8 sharp questions, writes down scope **and non-scope** |
| Modelling | one class per noun, getters/setters | entities with **behaviour** and **invariants**, a clear owner for each rule |
| Extensibility | `if (type == CAR) … else if …` | "what will change?" → interfaces at exactly those seams (Strategy / Factory / State) |
| Concurrency | ignored, or `synchronized` everywhere | names the **contended resource**, picks the narrowest lock / CAS / DB constraint, explains why |
| Correctness | happy path only | invalid transitions, idempotency, partial failure, money in `long` minor units |
| Trade-offs | "this is the best design" | "I chose X over Y because…; if Z changes I'd switch" |
| Communication | silent coding | narrates, checks in, time-boxes |

The code matters less than the **reasoning you say out loud**.

---

## 2. The 7-step method (≈ 45–60 minutes)

```
1. CLARIFY        (5 min)   scope, actors, scale, what's OUT of scope
2. REQUIREMENTS   (3 min)   functional list + non-functional (concurrency, extensibility, latency)
3. ENTITIES       (7 min)   nouns → classes; verbs → methods; find the AGGREGATES
4. RELATIONSHIPS  (5 min)   has-a / is-a / uses; who OWNS each piece of state
5. APIs & FLOWS   (5 min)   public methods of the service layer; walk through 1–2 key flows
6. PATTERNS       (5 min)   find the axes of change → apply patterns at those seams only
7. CODE + CONCURRENCY + EXTENSIONS (20 min) core classes, the critical path, thread-safety, follow-ups
```

### Step 1: questions that are almost always worth asking
- **Who are the actors?** (customer, admin, driver, system/cron)
- **What's the scale?** single machine/in-memory, or does it have to survive restarts?
- **Concurrency?** Can two users act on the same resource at the same moment? (seat, slot, wallet)
- **What varies?** pricing rules, algorithms, channels, payment methods → these become Strategies
- **Failure semantics?** retries, idempotency, what if a payment succeeds and the next step fails
- **What's explicitly out of scope?** (auth, UI, persistence, distributed deployment)

### Step 3: from nouns to classes
1. Write the requirements as sentences and underline the **nouns** (candidate classes) and **verbs** (candidate methods).
2. Drop nouns that are just attributes (a "colour" is a field, not a class).
3. Group into **aggregates**: a cluster with ONE root that guards its invariants.
   e.g. `Show` owns its `ShowSeat`s; nobody changes a seat's status except through the Show/booking service.
4. Decide **value objects** (immutable, compared by value: `Money`, `Location`, `TimeSlot`) vs **entities** (identity + lifecycle: `Booking`, `Ride`).

### Step 4: who owns what
- Each rule lives in **exactly one** place. "A spot can hold only one vehicle" belongs to `ParkingSpot`, not to the controller.
- Prefer **composition** (has-a) to inheritance. Use inheritance only for a true *is-a* with shared behaviour (`Car is-a Vehicle`), and even then ask whether an enum + a strategy is simpler.

### Step 6: patterns, applied where things change

| When the requirement says… | Reach for | Example in this course |
|---|---|---|
| "different algorithms / rules, maybe more later" | **Strategy** | pricing (01, 08), elevator scheduling (02), eviction (06, 17), split types (09) |
| "create one of several types by input" | **Factory** | vehicles/spots (01), notification channels (15), payment methods (18) |
| "behaviour depends on the current status" | **State** / enum transition table | vending (11), ATM (12), elevator (02), order (10) |
| "notify many parties when X happens" | **Observer** / event bus | display boards (01), order updates (10), chat delivery (14) |
| "add features by wrapping, in combinations" | **Decorator** | notification retry/audit (15), logging formatters (16) |
| "many optional parameters, immutable result" | **Builder** | logger config (16), job definitions (13), orders (19) |
| "a fixed sequence with replaceable steps" | **Template Method** | payment flow (18), withdrawal flow (12) |
| "chain of handlers, any may stop the request" | **Chain of Responsibility** | cash dispenser (12), log levels (16), rate-limit rules (04) |
| "exactly one instance" | **Singleton**, but prefer DI | explain why it hurts testability |
| "undo / queue / log operations" | **Command** | elevator requests (02), scheduler jobs (13) |

**Anti-pattern alert:** using patterns where nothing varies. Every interface needs a reason ("there are 3 pricing models and marketing adds more every quarter").

---

## 3. The concurrency toolbox (see `ConcurrencyToolkit.java`)

Always start by naming **what** is contended. Then pick the narrowest tool that works:

| Situation | Tool | Why |
|---|---|---|
| Single counter / reference | `AtomicLong`, `AtomicReference` + CAS | lock-free, no blocking |
| Many independent resources (seats, accounts, keys) | **per-key lock** (lock striping / `ConcurrentHashMap.compute`) | users on different resources never wait for each other |
| Read-heavy shared structure | `ReadWriteLock` / `ConcurrentHashMap` | many parallel readers |
| Check-then-act on a status ("if AVAILABLE then BOOK") | do the check and the act **atomically** (same lock / CAS on version) | otherwise two threads both pass the check → **double booking** |
| Two resources at once (transfer A→B) | lock both **in a global order** (by id) | prevents deadlock |
| Retries from clients / networks | **idempotency key** → stored result | a retry returns the first result instead of charging twice |
| Temporary reservation (seat hold) | lock with **expiry (TTL)** | abandoned carts free the seat automatically |
| Producer / consumer work | `BlockingQueue`, `ExecutorService` | back-pressure, bounded threads |
| Time-based work | `DelayQueue` / `ScheduledExecutorService` / `PriorityQueue` by time | job schedulers, TTL expiry |

**In a real distributed system** the same ideas move to the database: unique constraints, `SELECT … FOR UPDATE`, optimistic `version` columns, Redis `SET NX PX` locks, outbox tables. Say this in interviews: "In-memory I'd use a per-seat lock; across instances I'd rely on a unique constraint on (show_id, seat_id) or a conditional update on a version column."

---

## 4. Modelling rules that prevent most bugs

1. **Money is never `double`.** Use `long` minor units (paise/cents) or `BigDecimal`. Show this in every payment-related design.
2. **Status changes go through one method** that validates the transition (`order.transitionTo(SHIPPED)` throws if illegal).
3. **IDs are generated by the system**, not taken from callers. Use `UUID`, or an `AtomicLong` in interviews.
4. **Time is injected** (`Clock` / `LongSupplier`), never read inside logic with `System.currentTimeMillis()`. That makes expiry and rate limits testable.
5. **Return immutable views** (`List.copyOf`, records) so callers can't break invariants.
6. **Interfaces at the boundaries**, concrete classes inside: `PaymentGateway`, `NotificationSender`, `Repository`.
7. **Fail loudly on illegal input** (`IllegalArgumentException` / `IllegalStateException`), and keep "business failures" (seat taken, insufficient funds) as explicit results or exceptions the caller expects.

---

## 5. Answer template for the final 5 minutes: extensions

Interviewers almost always ask "how would you extend this?". Have a ready structure:
- **New variant** → "add a class implementing `XStrategy` and register it in the factory; no existing class changes (Open/Closed)."
- **Scale out** → "the in-memory map becomes a table/Redis; the lock becomes a DB constraint / distributed lock; events go through Kafka."
- **Persistence** → "repositories behind interfaces, so swapping the in-memory one for JPA doesn't touch services."
- **Observability** → "domain events → metrics, audit log."

---

## 6. SOLID, applied concretely (what to say)

| Principle | Concrete LLD sentence |
|---|---|
| S | "`ParkingLot` finds spots; `PricingStrategy` computes fees; `Ticket` records the stay. Each has one reason to change." |
| O | "New vehicle type = new enum value + spot rule; new pricing = new strategy. The service code is closed for modification." |
| L | "Any `EvictionPolicy` can replace another without the cache noticing; none of them throws 'unsupported'." |
| I | "`Searchable` and `Borrowable` are separate interfaces: an admin console doesn't depend on lending." |
| D | "`BookingService` depends on the `PaymentGateway` interface; the Stripe adapter is injected." |

---

## 7. Common mistakes that cost offers

- Jumping to code before agreeing on scope.
- One giant `Manager` class (a "God object").
- Inheritance trees for everything (`ElectricSUV extends SUV extends Car extends Vehicle`).
- Singletons everywhere ("how will you test this?").
- No answer to "two users click Book at the same time".
- `double` for money; `Date` mutable fields; exposing internal mutable lists.
- Over-engineering: 12 patterns for a 30-minute problem.

---

## Interview Q&A: general LLD

**Q1. How do you start an LLD round?**
Clarify actors, core use cases, scale, concurrency and out-of-scope items, then write the functional/non-functional requirements down and confirm them before modelling.

**Q2. How do you decide between an interface and an abstract class?**
Interface for a capability/contract with several unrelated implementations (strategies, gateways). Abstract class when implementations share real state and a skeleton algorithm (Template Method). Default to interfaces.

**Q3. Why is Singleton often called an anti-pattern?**
It's hidden global state: it hides dependencies, makes tests share state, and makes it hard to have two configurations. Prefer one instance created by the composition root (DI) and passed in. If you must, use an enum or a holder class for lazy, thread-safe init.

**Q4. Composition vs inheritance in LLD?**
Inheritance couples you to the parent's implementation (fragile base class) and explodes combinatorially (vehicle × fuel × size). Composition lets you swap behaviour at runtime (`vehicle.pricing = new SurgePricing()`).

**Q5. How do you prevent double booking?**
Make check-and-reserve atomic on the specific resource: a per-resource lock or a CAS on a status/version field in memory; in a DB, a unique constraint or a conditional `UPDATE … WHERE status='AVAILABLE'` and check the affected row count.

**Q6. Optimistic vs pessimistic locking?**
Pessimistic: lock first, so it suits high contention and short critical sections. Optimistic: read a version, write only if it's unchanged, retry on conflict, so it suits low contention, with no lock held during user think-time.

**Q7. What is idempotency and where does it matter?**
Doing an operation twice has the same effect as once. It matters for payments, order placement and job execution: the client sends an idempotency key, and the server stores key → result and replays the stored result on a retry.

**Q8. How would you make your in-memory design production-ready?**
Persistence behind repository interfaces, DB constraints for invariants, distributed locks or optimistic versions, an outbox for events, metrics/logging, configuration for strategies, and horizontal scaling of stateless services.

**Q9. How do you handle time in designs (expiry, rate limits)?**
Inject a clock so tests can move time forward deterministically; store absolute expiry timestamps; use delay queues or lazy expiry checks on access.

**Q10. How do you show extensibility without over-engineering?**
Ask "what is likely to change?" and put exactly one abstraction there. Mention other seams verbally as "if this becomes a requirement, I'd introduce…".

---

Run the toolkit: `java ConcurrencyToolkit.java`. It demonstrates every primitive in section 3, each with a race that **fails without** the technique and **passes with** it.
