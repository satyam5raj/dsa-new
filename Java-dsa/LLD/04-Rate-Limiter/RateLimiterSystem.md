# 04 · Rate Limiter (explained simply)

| | |
|---|---|
| **Code** | [RateLimiterSystem.java](RateLimiterSystem.java) |
| **Run** | `java RateLimiterSystem.java` (22 self-checks, including a 16-thread race for every algorithm) |
| **Design notes** | [README.md](README.md) (Redis + Lua version, more Q&A) |
| **Prerequisite** | [How to think in LLD + the concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md) |

This question tests whether you know the **five classic algorithms** and their trade-offs, can put them behind one interface (Strategy), keep state **per user** without one global lock, and avoid background threads.

---

## 1. The problem in plain words

An API must not be flooded. A **rate limiter** sits in front of it and, for each request, answers **"allow"** or **"deny (try again in X ms)"**. Typical rules:
- "Each user may make **100 requests per minute**."
- "Free users get 3 per second, Pro users get 20."
- "And the whole service handles at most 1,000 per second, whoever sends them."

When a request is denied, the server returns HTTP **429 Too Many Requests**, with headers telling the client how many requests remain and when to retry.

---

## 2. How to think about it (the 7 steps)

### Step 1 · Clarify

| Question to ask | Answer we'll assume |
|---|---|
| Limit **per what**? User, API key, IP? | Per **key** (any string), so all three work |
| Which algorithm? | Several, swappable by configuration |
| Are bursts OK? | Depends on the algorithm (a token bucket allows short bursts) |
| Different limits per plan? | Yes (Free, Pro, Enterprise) |
| Global limits as well as per-user? | Yes, both must pass |
| One server or many? | One, in memory; many servers is a follow-up (Redis) |
| What does the caller need back? | allowed or not, requests remaining, retry-after |

### Step 2 · Requirements
1. `tryAcquire(key)` → a decision: allowed, remaining, retry-after.
2. Five algorithms, chosen by configuration.
3. Per-plan rules, and layered rules (per user **and** global).
4. Thread-safe: with a limit of 100, **exactly** 100 requests are allowed even under heavy concurrency.
5. Cheap: millions of keys must cost nothing while idle (no timer per key).

### Step 3 · Entities

| Noun | Becomes |
|---|---|
| a decision | `record Decision(allowed, remaining, retryAfterMs)` |
| a rate limiter | `interface RateLimiter { Decision tryAcquire(String key); }` |
| each algorithm | a class implementing it |
| per-key state | a small state object per key (a counter, a list of timestamps, a token count) |
| a rule from config | `record Rule(algorithm, limit, windowMs)` |
| a plan | `enum Plan { FREE, PRO, ENTERPRISE }` |

### Step 4 · Relationships

```
RateLimiter (interface)
  ├── PerKeyLimiter<S> (abstract: "find this key's state, lock it, decide")
  │      ├── FixedWindowLimiter     ├── SlidingLogLimiter
  │      ├── SlidingCounterLimiter  ├── TokenBucketLimiter
  │      └── LeakyBucketLimiter
  ├── TieredRateLimiter   (picks a limiter by the key's plan)
  └── CompositeRateLimiter (a request must pass every layer)

RateLimiterFactory: Rule → RateLimiter
```

### Step 5 · API

```java
Decision tryAcquire(String key)
```

One method. An HTTP filter calls it for each request and returns 200 or 429.

### Step 6 · Patterns

| What changes / matters | Pattern |
|---|---|
| which algorithm | **Strategy** (`RateLimiter`) |
| "per-key state + lock + decide" is the same skeleton for all five | **Template Method** (`PerKeyLimiter`) |
| creating a limiter from a config rule | **Factory** (`RateLimiterFactory`) |
| per-user **and** global limits | **Composite** (`CompositeRateLimiter`) |
| per-plan limits | delegation (`TieredRateLimiter`) |

### Step 7 · Concurrency
The contended resource is **one key's state**. Each state object is locked on its own (`synchronized (state)`), so two different users never wait for each other.

---

## 3. The five algorithms, explained simply

Imagine a nightclub door that must not let in more than 5 people per minute.

| Algorithm | Analogy | How it decides | Good | Bad |
|---|---|---|---|---|
| **Fixed window** | a counter that resets on the clock: 10:00, 10:01, … | count requests in the current minute | simplest, O(1) memory | up to **2×** the limit around a boundary |
| **Sliding window log** | a notebook with the time of every entry | drop entries older than 1 minute; allow if fewer than 5 remain | **exact** | stores one timestamp per request |
| **Sliding window counter** | this minute's count + part of last minute's count | estimate = current + previous × (how much of the previous minute is still inside the last 60 s) | O(1) memory, nearly exact (Cloudflare uses it) | approximate |
| **Token bucket** | a jar of tokens that refills slowly | each request takes a token; tokens refill at r per second, up to a maximum b | allows **bursts** up to b, average r (AWS, Stripe) | bursts may not suit every backend |
| **Leaky bucket** | a bucket with a small hole that leaks at a steady rate | accept if the bucket isn't full | **smooth** output, no bursts | a burst is rejected even if the average is fine |

### Why the fixed window allows 2×
With a limit of 5 per second: 5 requests at 0.999 s (the end of window 1) and 5 more at 1.000 s (the start of window 2) are all allowed: **10 requests in 2 milliseconds**. The demo proves it.

### Lazy refill: no background threads
A token bucket "refills" over time, but there's **no timer**. When a request arrives, the code computes how much time has passed since the last request and adds that many tokens. An idle key costs nothing. With millions of keys, that matters enormously.

---

## 4. Java concepts used in this design

| Concept | In this design | Simple meaning |
|---|---|---|
| `abstract class` with generics `<S>` | `PerKeyLimiter<S>` | a shared skeleton; each subclass chooses its own state type `S` |
| `final` method | `tryAcquire` in `PerKeyLimiter` | subclasses can't change the skeleton, only fill in the steps |
| `abstract` methods | `newState()`, `decide(...)` | the steps each subclass must provide |
| `synchronized (state)` | locking one key's state | only one thread at a time may run the block for **that** object |
| `ConcurrentHashMap.computeIfAbsent` | creating a key's state | create it once, atomically, the first time a key is seen |
| static nested `State` classes | each algorithm's state | small mutable holders, only touched under their lock |
| `ArrayDeque<Long>` | the sliding log | a queue of timestamps |
| switch expression on an enum | the factory | map each algorithm to a constructor; the compiler checks every case is covered |
| `Function<String, Plan>` | `planOf` | "given a key, look up its plan", passed in |
| `UnaryOperator<String>` | the composite's key mapper | a function from a string to a string (e.g. any user → "GLOBAL") |
| static factory methods on a record | `Decision.allow(...)`, `Decision.deny(...)` | readable named constructors |

---

## 5. The code, piece by piece

### 5.1 The decision and the interface

```java
record Decision(boolean allowed, long remaining, long retryAfterMs) {
    static Decision allow(long remaining) { return new Decision(true, remaining, 0); }
    static Decision deny(long retryAfterMs) { return new Decision(false, 0, Math.max(1, retryAfterMs)); }
}

interface RateLimiter {
    Decision tryAcquire(String key);
}
```

Exactly what an HTTP filter needs: 200 or 429, an `X-RateLimit-Remaining` header, and a `Retry-After` header.

### 5.2 The Template Method skeleton

```java
static abstract class PerKeyLimiter<S> implements RateLimiter {
    protected final LongSupplier clock;
    private final ConcurrentHashMap<String, S> states = new ConcurrentHashMap<>();

    public final Decision tryAcquire(String key) {
        S state = states.computeIfAbsent(key, k -> newState());
        synchronized (state) {                                   // lock ONE user's state, not the limiter
            return decide(state, clock.getAsLong());
        }
    }
    protected abstract S newState();
    protected abstract Decision decide(S state, long nowMs);
}
```

Every algorithm does the same three things: find (or create) this key's state, lock it, and decide. The skeleton lives here once; each algorithm only writes `newState()` and `decide()`. That's the **Template Method** pattern.

### 5.3 Fixed window

```java
protected Decision decide(State s, long now) {
    long window = now - now % windowMs;                       // align to wall-clock windows
    if (window != s.windowStart) { s.windowStart = window; s.count = 0; }   // new window: reset
    if (s.count < limit) { s.count++; return Decision.allow(limit - s.count); }
    return Decision.deny(window + windowMs - now);
}
```

`now - now % windowMs` rounds the time down to the start of its window (e.g. 1,000,450 ms → 1,000,000). If we're in a new window, reset the count. Retry-after is the time left until the next window.

### 5.4 Sliding window log

```java
protected Decision decide(ArrayDeque<Long> log, long now) {
    while (!log.isEmpty() && log.peekFirst() <= now - windowMs) log.pollFirst();   // drop expired
    if (log.size() < limit) { log.addLast(now); return Decision.allow(limit - log.size()); }
    return Decision.deny(log.peekFirst() + windowMs - now);   // when the oldest one expires
}
```

Keep the timestamps of accepted requests. Drop those older than one window. Allow if fewer than `limit` remain. Retry-after is when the oldest timestamp falls out of the window.

### 5.5 Sliding window counter

```java
protected Decision decide(State s, long now) {
    long window = now - now % windowMs;
    if (window != s.windowStart) {
        s.previous = window - s.windowStart == windowMs ? s.current : 0;   // gap of 2+ windows -> 0
        s.current = 0;
        s.windowStart = window;
    }
    double overlap = 1.0 - (double) (now - window) / windowMs;
    double estimate = s.current + s.previous * overlap;
    if (estimate + 1 <= limit) { s.current++; return Decision.allow((long) (limit - estimate - 1)); }
    return Decision.deny(Math.max(1, (long) (windowMs * 0.1)));
}
```

Halfway through the current window, the last 1 second still includes half of the previous window. So `estimate = current + previous × 0.5`. It assumes the previous window's requests were spread evenly.

### 5.6 Token bucket

```java
protected State newState() { return new State(capacity, clock.getAsLong()); }   // start full
protected Decision decide(State s, long now) {
    s.tokens = Math.min(capacity, s.tokens + (now - s.last) * perMs);   // refill for the elapsed time
    s.last = now;
    if (s.tokens >= 1) { s.tokens -= 1; return Decision.allow((long) s.tokens); }
    return Decision.deny((long) Math.ceil((1 - s.tokens) / perMs));      // time until one full token
}
```

| Line | Simple meaning |
|---|---|
| start full | a new user can burst up to `capacity` straight away |
| the refill | tokens += time passed × rate, never above the capacity (**lazy refill**) |
| allow | take one token |
| retry-after | how long until the jar has one whole token again |

### 5.7 Leaky bucket

```java
protected Decision decide(State s, long now) {
    s.water = Math.max(0, s.water - (now - s.last) * leakPerMs);       // leaked since last time
    s.last = now;
    if (s.water + 1 <= capacity) { s.water += 1; return Decision.allow((long) (capacity - s.water)); }
    return Decision.deny((long) Math.ceil((s.water + 1 - capacity) / leakPerMs));
}
```

The mirror image of the token bucket: each request adds water, and water leaks out at a constant rate. Accept if there's room. (This is the "meter" version that accepts or rejects; the "queue" version would actually delay requests.)

### 5.8 Factory, tiers and composite

```java
static RateLimiter create(Rule r, LongSupplier clock) {
    return switch (r.algorithm()) {
        case FIXED_WINDOW -> new FixedWindowLimiter(r.limit(), r.windowMs(), clock);
        case SLIDING_LOG -> new SlidingLogLimiter(r.limit(), r.windowMs(), clock);
        case SLIDING_COUNTER -> new SlidingCounterLimiter(r.limit(), r.windowMs(), clock);
        case TOKEN_BUCKET -> new TokenBucketLimiter(r.limit(), r.limit() * 1000.0 / r.windowMs(), clock);
        case LEAKY_BUCKET -> new LeakyBucketLimiter(r.limit(), r.limit() * 1000.0 / r.windowMs(), clock);
    };
}
```

The **factory** turns a config rule ("100 per 60 s, token bucket") into a limiter, so callers never write `new TokenBucketLimiter(...)` themselves.

```java
static class TieredRateLimiter implements RateLimiter {
    public Decision tryAcquire(String key) { return byPlan.get(planOf.apply(key)).tryAcquire(key); }
}

static class CompositeRateLimiter implements RateLimiter {
    public Decision tryAcquire(String key) {
        long remaining = Long.MAX_VALUE;
        for (Layer l : layers) {
            Decision d = l.limiter().tryAcquire(l.keyMapper().apply(key));
            if (!d.allowed()) return d;                              // the first rejection wins
            remaining = Math.min(remaining, d.remaining());
        }
        return Decision.allow(remaining);
    }
}
```

- **Tiered:** look up the key's plan, then use that plan's limiter.
- **Composite:** the request must pass **every** layer, in order. The key mapper lets the global layer use one shared key ("GLOBAL") for everyone.

**A caveat worth saying in the interview:** if layer 1 accepts and layer 2 rejects, layer 1 has already counted the request. That's acceptable for protecting a backend; a strict version would need "check all, then commit all".

---

## 6. Dry run (from the real output)

| Test | Setup | Result | Why |
|---|---|---|---|
| fixed window | limit 5/s, 8 requests | 5 allowed | |
| | the 9th request's retry-after | 1000 ms | the rest of the window |
| | another user | 5 allowed | state is per key |
| | 5 at the last ms of a window + 5 at the first ms of the next | **10 allowed in 2 ms** | the boundary flaw |
| sliding log | the same edge test | only 5 | exact |
| sliding counter | limit 10: 12 requests | 10 | |
| | 1.5 s later (halfway into the next window) | 5 | estimate = 0 + 10 × 0.5 = 5 |
| token bucket | capacity 10, 5/s: 15 requests | 10 | the initial burst |
| | the next request's retry-after | 200 ms | 1 token at 5 per second |
| | 1 s later | 5 | refilled 5 tokens |
| | idle for a minute | 10 | capped at capacity |
| leaky bucket | holds 3, leaks 1/s | 3, then 1 more after a second | smooth |
| tiered | Free = 3/s, Pro = 20/s | 3 and 20 | |
| composite | 10 per user AND 25 global, 4 users × 10 | 25 in total | a: 10, b: 10, c: 5, d: 0 |
| concurrency | 16 threads × 1,000 calls, limit 100, each algorithm | **exactly 100** for all five | per-key lock |

---

## 7. Concurrency, explained simply

**The danger:** two threads read `count = 99` at the same moment, both see "under 100", both increment. Now 101 requests got through.

**The protection:** the whole "read the state, decide, update the state" runs inside `synchronized (state)`. Since the state object is **per key**, users don't block each other: only requests for the **same** key wait in line.

`computeIfAbsent` makes sure two threads seeing a brand-new key don't each create their own state object (which would let both "first requests" through separately).

**Test:** 16 threads × 1,000 calls on one hot key with a limit of 100 → exactly 100 allowed, for every algorithm.

---

## 8. Edge cases and common mistakes

**Edge cases handled:** a brand-new key (state created lazily), long idle periods (the token bucket caps at capacity; the sliding counter treats a gap of 2+ windows as 0), denial with a meaningful retry-after.

**Common mistakes:**
1. One global lock for all keys.
2. A background thread per bucket to refill tokens.
3. Not knowing the fixed-window boundary problem.
4. Using `System.currentTimeMillis()` directly (tests become slow and flaky).
5. Forgetting the response headers (remaining, retry-after).
6. Only per-user limits, so many users together can still overload the backend.

---

## 9. Interview questions and answers

**Q1. Compare the algorithms.**
> Fixed window: simplest, but 2× bursts at boundaries. Sliding log: exact but stores every timestamp. Sliding counter: O(1) and nearly exact. Token bucket: allows bursts up to the capacity with a steady average; the most common choice for APIs. Leaky bucket: smooth output, no bursts.

**Q2. Which would you choose for a public API?**
> A token bucket: it allows short bursts (good user experience) while enforcing the average rate, and it's O(1) per key with lazy refill.

**Q3. How do you refill tokens without a thread per user?**
> Lazily: store the token count and the last update time; on each request, add elapsed time × rate (capped at the capacity) before deciding.

**Q4. How is it thread-safe without a global lock?**
> State is per key in a `ConcurrentHashMap`; each request locks only its key's state object. Different keys never contend.

**Q5. Why a Template Method here?**
> All five algorithms share the skeleton "get or create this key's state, lock it, decide". The abstract class owns that skeleton; each algorithm only implements the decision.

**Q6. How do you combine per-user and global limits?**
> A composite limiter that checks each layer in order and stops at the first rejection; the global layer maps every key to one shared key.

### Follow-up questions

**F1. "Make it work across many servers."**
> Keep the state in Redis. The read-decide-update must be atomic, so run it as a Lua script on the Redis server (or use `INCR` with `EXPIRE` for a fixed window). Every app server calls the same script, and Redis runs scripts one at a time.

**F2. "What if Redis is down?"**
> Decide between **fail open** (allow requests, protecting availability) and **fail closed** (reject, protecting the backend). Many systems fail open and fall back to a local in-memory limiter.

**F3. "Different costs per request (an export costs 10, a read costs 1)."**
> Make `tryAcquire(key, cost)` take `cost` tokens instead of 1.

**F4. "Stop the state map growing forever."**
> Evict keys that have been idle for a while (their state would be "full bucket" anyway), for example with a cache that expires entries after access.

**F5. "Where should the limiter live?"**
> As close to the edge as possible: an API gateway or a filter/middleware before the business logic, so rejected requests cost almost nothing.

---

## 10. Running the file

```bash
java RateLimiterSystem.java      # 22/22 tests passed (including exactly 100 allowed under 16 threads, for all 5 algorithms)
```

**One-line summary:** *"Five algorithms behind one RateLimiter interface; a Template Method keeps per-key state in a ConcurrentHashMap and locks only that key's state; refill is lazy from elapsed time; a factory builds limiters from config, a tiered limiter picks by plan, and a composite enforces per-user and global limits together."*
