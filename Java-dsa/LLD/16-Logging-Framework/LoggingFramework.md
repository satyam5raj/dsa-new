# 16 · Logging Framework, a mini Log4j / Logback (explained simply)

| | |
|---|---|
| **Code** | [LoggingFramework.java](LoggingFramework.java) |
| **Run** | `java LoggingFramework.java` (20 self-checks, including 8 threads × 1,000 lines) |
| **Design notes** | [README.md](README.md) (more Q&A, production logging) |
| **Prerequisite** | [How to think in LLD + the concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md) |

You've used `log.info(...)` a thousand times. Designing the thing behind it is a classic interview question because it packs **five patterns** into one small system: Strategy (formatters), Chain of Responsibility (filters), Observer (appenders), Decorator (async), and Builder (config), plus a performance rule that matters a lot: **disabled log calls must cost almost nothing.**

---

## 1. The problem in plain words

Application code calls `logger.debug/info/warn/error`. The framework decides:
- **is this level enabled** for this logger? (DEBUG for the database package, INFO everywhere else);
- **where does it go?** console, a file that rolls over when it gets big, a JSON stream for log tools;
- **what does it look like?** `2026-10-04T10:00:00Z [main] INFO com.shop.OrderService - order placed`;
- **which lines to drop?** health-check noise, but always keep lines for a user being debugged;
- **extra context**, like the request id, automatically on every line;
- and it must never slow the app down or mix up lines from different threads.

---

## 2. How to think about it (the 7 steps)

### Step 1 · Clarify

| Question to ask | Answer we'll assume |
|---|---|
| Levels? | TRACE < DEBUG < INFO < WARN < ERROR < FATAL (OFF disables) |
| Per-package levels? | Yes, by dotted logger names that inherit |
| Destinations? | Console, rolling file, in-memory (for tests), any combination |
| Formats? | A text pattern and JSON |
| Filtering? | Per destination |
| Context (request id)? | Yes, per thread (MDC) |
| Async? | Optional: the app thread shouldn't wait for disk I/O |
| Thread-safe? | Yes: whole lines, never mixed |

### Step 2 · Requirements
1. `getLogger(name)` returns the same logger every time; loggers form a tree by dots.
2. A logger's effective level = its own, or the nearest ancestor's.
3. A record goes to the logger's appenders, then its parent's, and so on (**additivity**), unless a logger is non-additive.
4. Each appender has filters + a formatter.
5. Async appender with a bounded queue: BLOCK or DROP on overflow; drain on close.
6. Disabled calls build no strings.

### Step 3 · Entities

| Noun | Becomes |
|---|---|
| level | `enum Level` |
| log event | `record LogRecord(time, level, logger, thread, message, error, mdc)` |
| context | `class MDC` (a `ThreadLocal` map) |
| logger | `class Logger` (name, parent, level, additive, appenders) |
| logger registry | `class LoggerRepository` |
| output destination | `interface Appender` (+ Console, InMemory, RollingFile, Async) |
| layout | `interface Formatter` (Pattern, Json) |
| filter | `interface Filter` → ACCEPT / DENY / NEUTRAL |
| configuration | `LoggingConfig.Builder` |

### Step 4 · Relationships

```
ROOT (INFO, appender: main)
 └── com
      └── shop        (appender: errors [JSON, ERROR+])
           ├── OrderService          → effective level INFO (from ROOT)
           └── db     (DEBUG)
                └── Pool             → effective level DEBUG (from com.shop.db)

Appender = filters (chain) + formatter (strategy) + write()
```

### Step 5 · API

```java
Logger log = repo.getLogger("com.shop.OrderService");
log.info("order placed");
log.debug(() -> "cart " + cart);          // lazy: only built if DEBUG is on
log.error("payment failed", exception);
MDC.put("requestId", "r-42"); try { ... } finally { MDC.clear(); }
LoggingConfig.builder().root(INFO, main).logger("com.shop.db", DEBUG).appender("com.shop", errors).build().applyTo(repo);
```

### Step 6 · Patterns

| What changes / matters | Pattern |
|---|---|
| how a line looks | **Strategy** (`Formatter`) |
| which lines an appender keeps | **Chain of Responsibility** (`Filter`) |
| several destinations get the same record | **Observer**-like fan-out to appenders |
| "make any appender async" | **Decorator** (`AsyncAppender`) |
| filter → format → write steps fixed, write() varies | **Template Method** (`AbstractAppender`) |
| one logger per name | **Factory + cache** (`LoggerRepository`) |
| lots of optional settings | **Builder** (`LoggingConfig`) |

### Step 7 · Concurrency
Many threads log at once: lines mustn't interleave (lock in the appender), the per-thread context mustn't leak between threads (`ThreadLocal`, cleared in `finally`), and loggers can be created concurrently (`computeIfAbsent`).

---

## 3. The key ideas, simply

### Idea 1: the logger tree, like company departments
A rule set for "com.shop.db" applies to everything under it ("com.shop.db.Pool"), unless something lower sets its own rule. If nobody sets one, ROOT's rule (INFO) applies. **Effective level = walk up until you find a set level.**

### Idea 2: additivity, like CC-ing your manager
When `com.shop.OrderService` logs, the record goes to its own appenders, then `com.shop`'s, then ROOT's: each level "gets a copy". Setting `additive = false` on a logger stops the copies going further up.

### Idea 3: the fast path
In production, DEBUG is usually off, yet the code is full of `log.debug(...)`. If each call built a string first (`"user " + user.toString()`), you'd pay for millions of strings nobody reads. So:

```java
log.debug(() -> "user " + expensiveToString());   // a Supplier: called only if DEBUG is enabled
```

The disabled path is just "read the level, compare, return".

---

## 4. Java concepts used in this design

| Concept | In this design | Simple meaning |
|---|---|---|
| `enum` + `ordinal()` | `Level.atLeast` | levels are ordered by declaration, so compare their positions |
| `Supplier<String>` | `debug(() -> ...)` | a message that's built **only when asked** |
| `ThreadLocal` | `MDC` | each thread gets its own private map |
| `volatile` | `level`, `additive` | a change made on one thread is seen by others immediately |
| `CopyOnWriteArrayList` | logger appenders | read on every log call, changed rarely: ideal |
| `ArrayBlockingQueue` (`put` / `offer` / `take`) | `AsyncAppender` | a bounded queue: `put` waits, `offer` returns false when full, `take` waits for an item |
| a daemon thread | the async worker | doesn't keep the JVM alive by itself |
| the **poison pill** | `POISON` record | a special item meaning "stop after this" |
| `abstract class` + `final` method | `AbstractAppender.append` | the steps are fixed; subclasses only fill in `write` |
| `ArrayDeque` | rolling files | add the new file at the end, drop the oldest from the front |

---

## 5. The code, piece by piece

### 5.1 Effective level and the fast path

```java
Level effectiveLevel() { for (Logger l = this; l != null; l = l.parent) if (l.level != null) return l.level; return Level.INFO; }
boolean isEnabled(Level lv) { return lv != Level.OFF && lv.atLeast(effectiveLevel()); }

void log(Level lv, Supplier<String> msg, Throwable t) {
    if (!isEnabled(lv)) return;                          // FAST PATH: nothing allocated, supplier never called
    LogRecord r = new LogRecord(repo.clock.getAsLong(), lv, name, Thread.currentThread().getName(), msg.get(), t, MDC.snapshot());
    for (Logger l = this; l != null; l = l.parent) {     // additivity: my appenders, then my parent's, ...
        for (Appender a : l.appenders) a.append(r);
        if (!l.additive) break;
    }
}
```

| Part | Simple meaning |
|---|---|
| `level == null` | "not set here, ask my parent" |
| `msg.get()` | the message is built only after the level check passed |
| `MDC.snapshot()` | copy this thread's context **now**: an async appender reads it later on another thread |
| the loop + `break` | additivity |

### 5.2 Getting loggers: a cached tree

```java
Logger getLogger(String name) {
    if (name.isEmpty()) return root;
    Logger existing = loggers.get(name);
    if (existing != null) return existing;
    int dot = name.lastIndexOf('.');
    Logger parent = dot < 0 ? root : getLogger(name.substring(0, dot));    // create parents first
    return loggers.computeIfAbsent(name, n -> new Logger(n, parent, this));
}
```

`getLogger("com.shop.db.Pool")` recursively makes sure `com.shop.db`, `com.shop` and `com` exist and links each to its parent. `computeIfAbsent` means two threads asking at the same time get the **same** instance.

### 5.3 MDC: per-thread context

```java
private static final ThreadLocal<Map<String, String>> CTX = ThreadLocal.withInitial(LinkedHashMap::new);
static void put(String k, String v) { CTX.get().put(k, v); }
static void clear() { CTX.get().clear(); }     // ALWAYS clear in finally: thread pools reuse threads!
```

**The analogy:** a name badge each worker wears. Everything that worker writes is stamped with their badge. In a thread pool, the same worker serves the next request, so if you don't take the badge off (`clear()`), the next request's logs carry the **wrong request id**.

### 5.4 Formatters (Strategy)

`PatternFormatter("%d [%t] %p %c - %m [req=%X{requestId}]")` walks the pattern; `%` followed by a letter is replaced: d = time, t = thread, p = level padded to 5, c = logger, m = message, `X{key}` = an MDC value (or `-`), n = newline.

`JsonFormatter` writes one JSON object per line and **escapes** quotes, backslashes, newlines and control characters:

```java
case '"' -> o.append("\\\""); case '\\' -> o.append("\\\\"); case '\n' -> o.append("\\n"); ...
```

An unescaped quote or newline in a message would break every log parser downstream.

### 5.5 Filters (Chain of Responsibility)

```java
static boolean passes(List<Filter> chain, LogRecord r) {
    for (Filter f : chain) {
        FilterResult res = f.decide(r);
        if (res != FilterResult.NEUTRAL) return res == FilterResult.ACCEPT;   // the first firm answer wins
    }
    return true;                                                              // nobody objected
}
```

| Filter | Says |
|---|---|
| `ThresholdFilter(ERROR)` | DENY below ERROR, else NEUTRAL |
| `ContainsDenyFilter("/health")` | DENY if the text contains it |
| `MdcAcceptFilter("user", "vip")` | ACCEPT if MDC user = vip (put it **first** so it overrides later DENYs) |

Three answers instead of yes/no let one filter say "definitely keep this", which ends the chain early.

### 5.6 AbstractAppender (Template Method)

```java
public final void append(LogRecord r) {
    if (!passes(filters, r)) return;
    String line = formatter.format(r);           // format OUTSIDE the lock (it's the slow part)
    synchronized (this) { write(line); }         // whole lines, never interleaved
}
protected abstract void write(String line);
```

### 5.7 RollingFileAppender

```java
if (files.getLast().length() + line.length() + 1 > maxBytes && files.getLast().length() > 0) {
    files.addLast(new StringBuilder());                     // roll: start a new file
    while (files.size() > maxFiles) files.removeFirst();    // retention: delete the oldest
}
files.getLast().append(line).append('\n');
```

The `length() > 0` check means a single huge line still gets written (into an empty file) instead of rolling forever.

### 5.8 AsyncAppender (Decorator)

```java
public void append(LogRecord r) {
    if (!running) return;
    if (overflow == Overflow.BLOCK) queue.put(r);            // wait for space: lossless
    else if (!queue.offer(r)) dropped.incrementAndGet();     // full: drop and count, caller never waits
}
// worker thread:
while (true) { LogRecord r = queue.take(); if (r == POISON) return; inner.append(r); }

public void close() { running = false; queue.put(POISON); worker.join(5000); inner.close(); }
```

**The analogy:** a mailbox. The app drops letters in (fast) and a postman (the worker thread) delivers them (slow). The box has a fixed size, so you either wait for space (BLOCK) or throw the letter away and count it (DROP). On shutdown, you put in a "STOP" card (poison pill): the postman delivers everything before it and then goes home, so **the last logs, often the crash explanation, aren't lost**.

---

## 6. Dry run (from the real output)

Config: ROOT = INFO → `main` (pattern); `com.shop.db` = DEBUG; `com.shop` → `errors` (JSON, ERROR and up).

| # | Call | Result | main count |
|---|---|---|---|
| 1 | `svc.debug("hidden")` | OrderService is INFO → dropped | 0 |
| 1 | `db.debug("pool size 10")` | db.Pool is DEBUG → main | 1 |
| 2 | 100,000 × `svc.debug(supplier)` | supplier called **0** times | 1 |
| 3 | MDC requestId=r-42, `svc.info("order placed")` | `2026-10-04T10:00:00Z [main] INFO  com.shop.OrderService - order placed [req=r-42]` | 2 |
| 3 | `svc.info("no context")` | ends with `[req=-]` | 3 |
| 4 | `svc.error("payment failed: card \"declined\"\nretrying", ex)` | `errors` gets one JSON line with `\"declined\"\n` escaped; also main (additive) | 4 |
| 4 | `lib.error(...)` (org.lib) | not under com.shop → only ROOT's main | 5 |

**Filters (step 5):** `com.web` is non-additive with the filtered appender `[MdcAccept(vip), ContainsDeny(/health)]`.

| Line | Chain | Kept? |
|---|---|---|
| GET /health 200 | NEUTRAL, then DENY | no |
| GET /orders 200 | NEUTRAL, NEUTRAL | yes |
| GET /health 200 with user=vip | **ACCEPT** (stops) | yes |

So 2 kept, and main still has 5: nothing leaked to ROOT.

**Rolling (step 6):** limit 50 bytes, keep 3 files. Each line is about 20 bytes ("INFO  line number 0" + newline), so 2 lines per file; 20 lines → 10 files made, only the newest 3 kept, and the last one contains "line number 19".

**Async (step 7):**
- BLOCK with capacity 1024: 5,000 lines, `close()` drains → all 5,000 **in order**.
- DROP with capacity 10 and a stuck inner appender: the worker holds 1, the queue holds 10, so at least 89 of 100 are dropped and counted, and the caller never waits.

**Concurrency (step 8):** 8 threads × 1,000 lines, each with MDC tid = its number → 8,000 lines, every line `tid|t<tid>-i`: no interleaving, and no thread's MDC appeared on another's lines.

**Console JSON (step 9):**
```
{"ts":"2026-10-04T10:00:00Z","level":"WARN","logger":"demo.console","thread":"main","msg":"disk 91% full","requestId":"r-7"}
```

---

## 7. Concurrency, explained simply

| Risk | Protection |
|---|---|
| two threads' lines mixed | `synchronized (this) { write(line); }` in each appender |
| slow formatting blocking others | format **before** taking the lock |
| one request's id on another's lines | `ThreadLocal` MDC, cleared in `finally`, **snapshotted** into the record |
| two different loggers for the same name | `computeIfAbsent` |
| config changes while logging | `volatile` level/additive, `CopyOnWriteArrayList` appenders |
| the app waiting on disk | `AsyncAppender` with a bounded queue |

---

## 8. Edge cases and common mistakes

**Edge cases handled:** unset levels (inherit), OFF, missing MDC keys (`-`), quotes and newlines in JSON, a single line bigger than the file limit, a full async queue, shutdown with queued logs.

**Common mistakes:**
1. Building the message string before checking the level.
2. Not clearing MDC in a thread pool, so request ids leak.
3. Reading MDC in the async worker thread instead of snapshotting it on the caller's thread.
4. An unbounded async queue: a slow disk eventually means OutOfMemoryError.
5. Not draining the async queue on shutdown, losing the last logs.
6. Hand-built JSON without escaping.
7. Formatting inside the lock.

---

## 9. Interview questions and answers

**Q1. How does level inheritance work?**
> Loggers form a tree by dotted names. A logger's effective level is its own if set, otherwise the nearest ancestor's; ROOT always has one.

**Q2. What's additivity?**
> A record goes to the logger's appenders and then each ancestor's, unless a logger is marked non-additive, which stops the propagation there. It's how one ERROR reaches both the error file and the main log.

**Q3. How do you keep disabled log calls cheap?**
> Check the level first (a volatile read and a compare) and accept messages as `Supplier`s, so the string is built only when the level is enabled.

**Q4. Why an async appender, and what are the risks?**
> So the request thread doesn't wait for disk or network I/O. Risks: memory (so bound the queue), losing logs when it's full (BLOCK vs DROP, and count drops), and losing logs at shutdown (drain with a poison pill).

**Q5. What's MDC and what's the trap?**
> Per-thread key/values added to every line, like a request id. In thread pools threads are reused, so you must clear it in `finally`, and with async logging you must copy it into the record on the calling thread.

**Q6. Explain the filter chain.**
> Each filter returns ACCEPT, DENY or NEUTRAL; the first non-neutral answer decides; all-neutral means accept. ACCEPT lets an earlier filter override later DENYs.

### Follow-up questions

**F1. "Change the log level at runtime without a restart."**
> Levels are `volatile`; an admin endpoint or a config-file watcher sets the new level and every thread sees it on its next call.

**F2. "Logs from 500 servers in one place."**
> Write JSON lines to stdout or a file; a shipper (Fluent Bit, Filebeat) sends them to Elasticsearch/Loki. Include a trace id in the MDC to follow a request across services.

**F3. "Avoid logging passwords or card numbers."**
> A masking step (a formatter decorator or a filter that rewrites messages) with patterns for secrets, plus code review and structured fields instead of free text.

**F4. "Rolling by time instead of size."**
> Roll when the date (or hour) of a record differs from the current file's; name files with the date; delete files older than N days.

**F5. "What happens if the disk is full?"**
> The appender must catch I/O errors and never crash the app: count errors, fall back to stderr, and alert.

**F6. "Why is `isDebugEnabled()` still useful if you have suppliers?"**
> For blocks that do several steps of work just for logging; guard the whole block with the check.

---

## 10. Running the file

```bash
java LoggingFramework.java      # 20/20 tests passed (8,000 lines from 8 threads, no interleaving, no MDC leaks)
```

**One-line summary:** *"Loggers form a dotted-name tree with inherited levels and additive propagation to ancestors' appenders. Disabled calls return after one level check and never build their message (Supplier). Each appender runs a filter chain (ACCEPT/DENY/NEUTRAL), formats with a Strategy outside the lock and writes inside it, and an async decorator with a bounded queue, a BLOCK/DROP policy and a poison-pill drain keeps I/O off request threads. MDC gives per-thread context."*
