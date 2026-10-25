# E01 · Logger Rate Limiter ⭐

| | |
|---|---|
| **LeetCode** | [#359 Logger Rate Limiter](https://leetcode.com/problems/logger-rate-limiter/) (premium) |
| **Difficulty** | Easy |
| **Pattern** | A HashMap of "next allowed time" per key (a tiny TTL cache) |
| **Code** | [E01_LoggerRateLimiter.java](E01_LoggerRateLimiter.java) |
| **Run** | `java E01_LoggerRateLimiter.java` |

⭐ = not in `leetcode.md`, added for a key technique. This is the first note of the Advanced topic, so section 3 covers how **design problems** are written in Java.

---

## 1. The problem

Design a logger with one method, `shouldPrintMessage(timestamp, message)`. It returns `true` (print it) only if the same message wasn't printed in the last 10 seconds. Timestamps arrive in non-decreasing order.

| call | result | why |
|---|---|---|
| (1, "foo") | true | first time |
| (2, "bar") | true | first time |
| (3, "foo") | false | "foo" was printed at 1; allowed again from 11 |
| (8, "bar") | false | allowed again from 12 |
| (10, "foo") | false | 10 < 11 |
| (11, "foo") | true | exactly 10 seconds later |

Note that a **rejected** message doesn't reset the timer: only printing does.

---

## 2. How to think about it

### Store the next allowed time
For each message, remember the earliest timestamp it may be printed **again**:

```
if timestamp >= allowedAt.getOrDefault(msg, 0):
    allowedAt[msg] = timestamp + 10
    return true
return false
```

O(1) per call. A message that has never been seen gets the default 0, so it's always allowed.

### The memory problem
The map grows forever: every distinct message stays in it. Real systems **expire** old entries (a "time to live", TTL):
- keep a queue of (time, message) in arrival order, and evict entries older than 10 seconds from the front; or
- keep two maps, "this window" and "the previous window", and swap them every 10 seconds.

This is the same idea as Redis `EXPIRE` and rate limiters. The file's `BoundedLogger` uses the queue.

### Dry run: the simple logger

| call | `allowedAt` before | check | result | `allowedAt` after |
|---|---|---|---|---|
| (1, foo) | {} | 1 ≥ 0 | **T** | {foo: 11} |
| (2, bar) | {foo: 11} | 2 ≥ 0 | **T** | {foo: 11, bar: 12} |
| (3, foo) | | 3 < 11 | **F** | unchanged |
| (8, bar) | | 8 < 12 | **F** | unchanged |
| (10, foo) | | 10 < 11 | **F** | unchanged |
| (11, foo) | | 11 ≥ 11 | **T** | {foo: 21, bar: 12} |

---

## 3. Java concepts used in this problem

### 3.1 Design problems are classes with state

LeetCode "design" problems give you a class to fill in. The object keeps **state** between calls, and each method call reads and updates it:

```java
static class Logger {
    private final Map<String, Integer> allowedAt = new HashMap<>();     // the state

    public boolean shouldPrintMessage(int timestamp, String message) { ... }   // the API
}

Logger lg = new Logger();                 // one object = one logger
lg.shouldPrintMessage(1, "foo");          // calls change lg's map
```

- **`static class Logger` inside the file's class** is a *static nested class*. On LeetCode you'd write a top-level `class Logger`; here it's nested so that each problem fits in one runnable file. `static` means it doesn't need an instance of the outer class.
- **Fields** (the variables declared in the class, outside any method) live as long as the object does. That's where the state goes.
- **`new Logger()`** creates an object, and `lg.method(...)` calls a method on it.

### 3.2 `private` and `final` on fields

```java
private final Map<String, Integer> allowedAt = new HashMap<>();
```

- `private`: only code inside `Logger` can touch the map. Callers must go through the public method, so they can't corrupt the state. This is **encapsulation**, and interviewers notice when you use it.
- `final`: the field always refers to the **same** map. The map's contents can still change (`put`, `remove`). `final` just stops `allowedAt = something else`.
- The field is initialised right where it's declared, so no constructor is needed.

### 3.3 `getOrDefault`

```java
allowedAt.getOrDefault(message, 0)
```

It returns the stored value, or `0` if the key is missing. This avoids a separate `containsKey` check and a `null` (unboxing a `null` `Integer` into an `int` would throw a `NullPointerException`).

### 3.4 `Object[]` as a quick pair, and casting

```java
Deque<Object[]> recent = new ArrayDeque<>();
recent.addLast(new Object[]{timestamp, message});    // {Integer, String}
(int) recent.peekFirst()[0]                          // cast back to int
(String) old[1]                                      // cast back to String
```

An `Object[]` can hold values of different types. `timestamp` is **autoboxed** into an `Integer` when it's stored. Reading it back needs a **cast**: `(int)` unboxes the `Integer`, and `(String)` tells the compiler the element really is a `String`. (A small `record Entry(int time, String msg)` would be cleaner and type-safe; `Object[]` is the quick version.)

### 3.5 `Deque` as a queue

`addLast` adds at the back; `peekFirst` looks at the front without removing it; `pollFirst` removes the front. Covered in [Queue E01](../../05-Queue/02-Easy/E01_ImplementStackUsingQueues.md#3-java-concepts-used-in-this-problem).

---

## 4. Approach 1: A map of next allowed times (the file's main solution)

### Code ([E01_LoggerRateLimiter.java:35-43](E01_LoggerRateLimiter.java#L35-L43))

```java
static class Logger {
    private final Map<String, Integer> allowedAt = new HashMap<>();

    public boolean shouldPrintMessage(int timestamp, String message) {
        if (timestamp < allowedAt.getOrDefault(message, 0)) return false;
        allowedAt.put(message, timestamp + 10);
        return true;
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `allowedAt` | message → the earliest time it may be printed again |
| `if (timestamp < ...) return false` | still inside the 10-second window: reject, and don't touch the map |
| `put(message, timestamp + 10)` | printing starts a new window |
| `return true` | print it |

The dry run is in section 2.

### Complexity
- **Time O(1)** per call (on average, for the hash lookup).
- **Space O(M)** for M distinct messages ever seen: it never shrinks.

---

## 5. Approach 2: Memory-bounded with an expiry queue

### Code ([E01_LoggerRateLimiter.java:46-62](E01_LoggerRateLimiter.java#L46-L62))

```java
static class BoundedLogger {
    private final Map<String, Integer> lastPrinted = new HashMap<>();
    private final Deque<Object[]> recent = new ArrayDeque<>();   // {time, message}, oldest first

    public boolean shouldPrintMessage(int timestamp, String message) {
        while (!recent.isEmpty() && (int) recent.peekFirst()[0] <= timestamp - 10) {
            Object[] old = recent.pollFirst();
            if (lastPrinted.get((String) old[1]) == (int) old[0]) lastPrinted.remove((String) old[1]);   // expired
        }
        if (lastPrinted.containsKey(message)) return false;
        lastPrinted.put(message, timestamp);
        recent.addLast(new Object[]{timestamp, message});
        return true;
    }

    int remembered() { return lastPrinted.size(); }
}
```

### Line by line

| Line | What it does |
|---|---|
| `lastPrinted` | only the messages printed in the last 10 seconds, with their print time |
| `recent` | the same prints in time order, oldest at the front |
| the `while` | every print at time ≤ now − 10 has expired: pop it |
| the `if` inside | remove the map entry, but only if it still belongs to this print (a safety check) |
| `containsKey` → false | printed less than 10 seconds ago |
| `put` + `addLast` | record the new print in both structures |
| `remembered()` | the map size, used by the test to show memory stays small |

`old time ≤ now − 10` is the same as `now ≥ old time + 10`, so this agrees exactly with the simple logger.

### Dry run: the same calls

| call | expired from the front | `lastPrinted` after | result |
|---|---|---|---|
| (1, foo) | | {foo: 1} | **T** |
| (2, bar) | 1 ≤ −8? no | {foo: 1, bar: 2} | **T** |
| (3, foo) | 1 ≤ −7? no | unchanged | **F** |
| (8, bar) | 1 ≤ −2? no | unchanged | **F** |
| (10, foo) | 1 ≤ 0? no | unchanged | **F** |
| (11, foo) | 1 ≤ 1 → pop (1, foo), remove foo; 2 ≤ 1? no | {bar: 2, foo: 11} | **T** |

### Complexity
- **Time O(1) amortised** per call: each print is added to the queue once and removed once.
- **Space O(messages printed in the last 10 seconds).**

---

## 6. Comparing the approaches

| | Next-allowed map | Expiry queue |
|---|---|---|
| Time per call | O(1) | O(1) amortised |
| Memory | grows forever | only the last 10 seconds |
| Code | 3 lines | about 10 lines |

The file runs 100,000 random calls through both and checks they always agree. The bounded logger never remembered more than a few dozen messages at once, while the simple one kept all of them.

---

## 7. Edge cases and common mistakes

**Edge cases:** exactly 10 seconds later (allowed), many calls with the same timestamp, a rejected message (doesn't reset the timer).

**Mistakes:**
1. Updating the timestamp on a rejected call (the window should start only when something is printed).
2. Using `> 10` instead of `>= 10` for the gap.
3. `int x = map.get(key)` for a missing key: unboxing `null` throws.
4. Forgetting the memory growth when asked about production use.

---

## 8. Interview questions and answers

**Q1. Why store the next allowed time instead of the last printed time?**
> Either works. Storing `timestamp + 10` makes the check a single comparison and lets `getOrDefault(..., 0)` handle unseen messages.

**Q2. How do you stop the map from growing forever?**
> Expire entries older than 10 seconds: an expiry queue (as in `BoundedLogger`) or two rotating maps. Each print is removed once, so it's still O(1) amortised.

**Q3. What if many threads call it at once?**
> Use a `ConcurrentHashMap` and an atomic check-and-update (`compute`), or a lock around the method. Otherwise two threads could both see "allowed" for the same message.

### Follow-up questions

**F1. Allow up to k prints per window instead of one.**
> Keep a queue of print times per message (a sliding-window counter); allow while the queue holds fewer than k times within the window.

**F2. Timestamps can arrive out of order.**
> The simple check can wrongly reject or accept. You'd need per-message sorted timestamps or a defined tolerance, which is a design discussion.

**F3. LRU Cache.** → [M01](../03-Medium/M01_LRUCache.md)
> Another bounded-memory cache, evicting by recency instead of by time.

---

## 9. Running the file

```bash
java E01_LoggerRateLimiter.java      # 3/3 tests passed (example, 100,000 calls simple == bounded, bounded memory stays small)
```

**One-line summary:** *"Map each message to the time it may be printed again; print and set it to timestamp + 10 when allowed, and expire old entries with a queue if memory matters."*
