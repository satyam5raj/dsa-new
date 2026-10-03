# LLD: Low-Level Design for senior backend interviews

Every design has two files:
- **`README.md`**: how to think about it. Clarifying questions, requirements, the reasoning from nouns to classes, class and state diagrams (Mermaid), key flows, the patterns used and *why*, concurrency, edge cases, extensions, and 10+ interview Q&As with answers.
- **One runnable `.java` file**, with every concept explained in comments. Run it with `java File.java`. Each one tests itself, including concurrency races. Run everything with `./run-all.sh`.

**Start with [00 · LLD Playbook](00-LLD-Playbook/)**: the 7-step method, which pattern fits which requirement, the concurrency toolbox, and what interviewers grade. The design patterns themselves are taught in [OOP/06-Design-Patterns](../OOP/06-Design-Patterns/).

## Roadmap (from `JAVA.md`)

| # | Design | Tier | Core ideas | Status |
|---|---|---|---|---|
| 00 | [LLD Playbook](00-LLD-Playbook/) + `ConcurrencyToolkit` | – | method, check-then-act, CAS, lock ordering, idempotency, TTL holds | ✅ |
| 01 | [Parking Lot](01-Parking-Lot/) | 1 | Strategy (pricing, allocation), Builder, Observer, lock-free spot allocation, claim-before-charge | ✅ |
| 02 | [Elevator System](02-Elevator-System/) | 1 | dispatch vs ordering, LOOK vs FCFS (49 vs 675 floors), Command, single-writer event loop | ✅ |
| 03 | [Library Management](03-Library-Management/) | 1 | Book vs BookCopy, hold queue, fines Strategy, inverted index, per-title lock | ✅ |
| 04 | [Rate Limiter](04-Rate-Limiter/) | 1 | 5 algorithms, lazy refill, Template Method, Factory, Composite, Redis + Lua | ✅ |
| 05 | [URL Shortener](05-URL-Shortener/) | 1 | base62, counter / range / hash / random keys, collisions, atomic insert, 301 vs 302 | ✅ |
| 06 | [Cache (LRU/LFU)](06-Cache-LRU-LFU/) | 1 | eviction Strategy, TTL, single-flight (stampede), segments, evict-before-insert bug | ✅ |
| 07 | [Movie Ticket Booking](07-Movie-Ticket-Booking/) (+ hotel, airline) | 2 | hold → pay → confirm, all-or-nothing CAS holds with TTL, booking state machine | ✅ |
| 08 | [Ride-Sharing](08-Ride-Sharing/) | 2 | grid spatial index, matching Strategy, surge Decorator, driver CAS | ✅ |
| 09 | [Splitwise](09-Splitwise/) | 2 | exact paise splits, append-only ledger, greedy vs NP-hard optimal simplification | ✅ |
| 10 | [Food Delivery](10-Food-Delivery/) | 2 | multi-actor state machine (from → to → **who**), stock CAS, the agent-leak race | ✅ |
| 11 | [Vending Machine](11-Vending-Machine/) | 2 | State pattern, change-making where greedy fails (DP), atomic sale | ✅ |
| 12 | [ATM System](12-ATM-System/) | 2 | State, Chain of Responsibility, plan → debit → dispense → reverse (saga) | ✅ |
| 13 | [Job Scheduler](13-Job-Scheduler/) | 3 | at-least-once + idempotent handlers, leases, backoff + jitter, misfires, DLQ, Builder | ✅ |
| 14 | [Chat Application](14-Chat-Application/) | 3 | server seq per chat, clientMsgId dedupe, cursor-based sync and receipts, hold-back buffer | ✅ |
| 15 | [Notification System](15-Notification-System/) | 3 | event bus, channel Strategy, Decorators (retry, circuit breaker, rate limit), quiet hours | ✅ |
| 16 | [Logging Framework](16-Logging-Framework/) | 3 | logger hierarchy + additivity, lazy messages, filter chain, async Decorator, MDC, Builder | ✅ |
| 17 | [In-Memory Key-Value Store](17-KeyValue-Store/) | 3 | lock striping vs Redis single thread, lazy + active expiry, sampled LRU, WATCH/EXEC, AOF | ✅ |
| 18 | [Payment Gateway / Wallet](18-Payment-Wallet/) | 3 | double-entry ledger (Σ = 0), idempotency, auth/capture/void, refunds as reversals | ✅ |
| 19 | [Order Matching Engine](19-Order-Matching-Engine/) | 3 | price-time priority book, GTC/IOC/FOK, amend rules, single-threaded sequencer | ✅ |

## Real bugs the tests caught while building these (good interview stories)
- **Parking lot:** charging at the exit *before* claiming the ticket would let two gates both charge. Fixed by CAS `ACTIVE → PAYING` first.
- **Cache:** insert-then-evict made LFU evict the brand-new key every time. Fixed by evicting first.
- **Food delivery:** cancel racing with accept leaked a delivery agent (BUSY forever). Fixed by attaching the agent under the order lock after re-checking the status.

## Patterns → where to see them

| Pattern | Designs |
|---|---|
| Strategy | 01–11, 13, 15, 16, 17, 18 |
| State / state machine | 02, 07, 08, 10, 11, 12, 13, 18 |
| Observer / event bus | 01, 02, 03, 05, 07, 08, 09, 10, 14, 15, 16, 19 |
| Factory / registry | 04, 06, 09, 15, 16, 18 |
| Builder | 01, 06, 13, 16 |
| Decorator | 07, 08, 15 (retry, breaker, rate limit), 16 (async appender) |
| Template Method | 04, 12, 16 |
| Chain of Responsibility | 12, 16 (filters) |
| Command | 02, 17 (AOF), 19 |
| Composite | 04 |
| Idempotency keys | 00, 05, 07, 12, 13, 14, 15, 18 |
| Single-writer / sequencer | 02, 19 |
