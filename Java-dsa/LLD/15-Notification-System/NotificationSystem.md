# 15 · Notification System: email, SMS and push (explained simply)

| | |
|---|---|
| **Code** | [NotificationSystem.java](NotificationSystem.java) |
| **Run** | `java NotificationSystem.java` (19 self-checks, including 8 threads sending 4,000 requests) |
| **Design notes** | [README.md](README.md) (queues, providers at scale, more Q&A) |
| **Prerequisite** | [How to think in LLD + the concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md) |

Sending one SMS is easy. A **notification system** is everything around it: which channel, does the user even want this, is it 2 a.m., what if the provider is down, what if the same event arrives twice. This design is the best example in the course of the **Decorator** pattern: each of those concerns is a small wrapper you stack around a plain sender.

---

## 1. The problem in plain words

Other services (orders, payments, auth) need to tell users things. The notification system must:
- listen for **events** ("OrderShipped") without the order service knowing anything about email or SMS;
- respect **preferences**: muted categories, disabled channels;
- respect **quiet hours**: no marketing at 11 p.m. (but an OTP always goes through);
- fill in **templates** ("Hi {{name}}") and never send a half-filled one;
- try channels **in order** (push → SMS → email) and stop at the first success;
- **retry** temporary failures, **fail fast** when a provider is down and switch to a **backup provider**, and **rate-limit** per recipient;
- never send the **same notification twice**.

---

## 2. How to think about it (the 7 steps)

### Step 1 · Clarify

| Question to ask | Answer we'll assume |
|---|---|
| Channels? | Push, SMS, email |
| Who triggers notifications? | Domain events from other services, plus direct calls |
| Kinds of notifications? | SECURITY (OTP, fraud), TRANSACTIONAL (order updates), MARKETING |
| Can users opt out? | Of channels and categories, except SECURITY |
| Quiet hours? | LOW-priority messages wait until morning |
| Can an event be delivered twice? | Yes (message buses are at-least-once), so dedupe |
| Providers fail? | Yes: retries, circuit breaker, backup provider |

### Step 2 · Requirements
1. Subscribe to domain events and turn them into notification requests.
2. Pipeline: dedupe → preferences → quiet hours → render template → send on the first working channel → log.
3. Senders are composable: retry, circuit breaker with fallback, rate limit, audit.
4. Thread-safe, so the same key is delivered exactly once under concurrency.

### Step 3 · Entities

| Noun | Becomes |
|---|---|
| event | `record DomainEvent(type, userId, data, eventId)` + `DomainEventBus` |
| channel / priority / category | three enums |
| request | `record NotificationRequest(idempotencyKey, userId, templateId, params, category, priority, channels)` |
| user + preferences | `record UserProfile`, `record Preferences` |
| template | `TemplateEngine` (keyed by template id + channel) |
| sender | `interface NotificationSender` + decorators |
| outcome | `record DeliveryRecord(key, channel, status, detail)` |

### Step 4 · Relationships

```
OrderService ── publish(OrderShipped) ──▶ DomainEventBus ──▶ NotificationService.notify(request)
                                                                 │ dedupe, prefs, quiet hours, template
                                                                 ▼
                                                   SenderRegistry: channel → decorated sender
        SMS: Auditing( RateLimited( CircuitBreaker( Retrying(twilio), fallback = msg91 ) ) )
```

### Step 5 · API

```java
bus.publish(DomainEvent e)
svc.subscribeTo(bus, "OrderShipped", "shipped", TRANSACTIONAL, HIGH, List.of(PUSH, SMS, EMAIL))
svc.notify(NotificationRequest r)
svc.flushDeferred()              // called when quiet hours end
```

### Step 6 · Patterns

| What changes / matters | Pattern |
|---|---|
| producers mustn't know about channels | **Observer** (event bus) |
| how each channel/provider sends | **Strategy** (`NotificationSender`) |
| retry, breaker, rate limit, audit around any sender | **Decorator** |
| channel → configured sender | **Factory / registry** |
| a provider that's down | **Circuit breaker** (a small State machine) |
| duplicate events | **idempotency key** |

### Step 7 · Concurrency
Many threads call `notify`. The one race that matters: two threads with the same key both decide "not seen yet". A concurrent set's `add` is atomic and returns false for the loser.

---

## 3. The key idea, simply: decorators are like phone cases

You have a plain phone (the SMS provider). You can add a screen protector, a case, a ring holder, in any combination, and each one does one job. You don't buy a "phone with protector and case and ring" model.

Subclasses would explode: `RetryingSmsSender`, `RetryingRateLimitedSmsSender`, `RetryingAuditedEmailSender`… Decorators all implement the same `NotificationSender` interface and **wrap** another one:

```java
new AuditingSender(                       // outermost: log what happened
    new RateLimitedSender(                //   at most 3 per number per hour
        new CircuitBreakerSender(         //     fail fast if twilio is dead, use msg91 instead
            new RetryingSender(sms, 2, 100, sleeper),   // retry once on a blip
            smsBackup, 3, 60_000, clock),
        3, 3_600_000, clock),
    audit);
```

The call travels inwards; each layer can stop it, retry it or redirect it. Java's own I/O works the same way: `new BufferedReader(new InputStreamReader(new FileInputStream(f)))`.

**Order matters.** The rate limiter is **outside** the breaker, so a rate-limited message never counts as a provider failure.

---

## 4. Java concepts used in this design

| Concept | In this design | Simple meaning |
|---|---|---|
| `Consumer<DomainEvent>` | event subscribers | "a function that takes an event" |
| `CopyOnWriteArrayList` | subscriber lists, the log | safe to iterate while others add; fine when reads dominate |
| `Pattern` / `Matcher.appendReplacement` | templates | find each `{{var}}` and replace it |
| `Matcher.quoteReplacement` | templates | so a `$` in a value isn't treated as a regex group |
| `LongConsumer sleeper`, `LongSupplier clock`, `IntSupplier hourOfDay` | decorators, service | **injected** time, so tests run instantly and can "jump" to 23:00 |
| `ConcurrentHashMap.newKeySet()` | `seenKeys` | a thread-safe set; `add` returns false if already present |
| `EnumMap` / `EnumSet` | registry, preferences | compact, fast maps and sets keyed by an enum |
| `synchronized (c)` on a `long[]` | rate limiter | lock just this recipient's counter |
| `ConcurrentLinkedQueue` | deferred requests | a lock-free queue |
| switch expression | `UserProfile.address(c)` | channel → email / phone / push token |

---

## 5. The code, piece by piece

### 5.1 The event bus (Observer)

```java
void subscribe(String type, Consumer<DomainEvent> c) { subs.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(c); }
void publish(DomainEvent e) { subs.getOrDefault(e.type(), List.of()).forEach(c -> c.accept(e)); }
```

The order service only says "OrderShipped happened". The notification service subscribed earlier and turns the event into a request, **using the event id as the idempotency key**:

```java
bus.subscribe(eventType, e -> notify(new NotificationRequest(e.eventId(), e.userId(), templateId, e.data(), cat, p, channels)));
```

### 5.2 Quiet hours that wrap past midnight

```java
boolean inQuietHours(int hour) {
    return quietFromHour <= quietToHour ? hour >= quietFromHour && hour < quietToHour
                                        : hour >= quietFromHour || hour < quietToHour;   // e.g. 22 -> 7 wraps midnight
}
```

For 22 → 7, the quiet hours are "22 or later **or** before 7". A plain `from <= h < to` check would never be true for that range. With `0, 0` (from == to) nothing is quiet.

### 5.3 Templates that fail loudly

```java
Matcher m = VAR.matcher(t);                         // VAR = \{\{(\w+)}}
while (m.find()) {
    String v = params.get(m.group(1));
    if (v == null) throw new IllegalArgumentException("Missing param '" + m.group(1) + "' ...");   // never send "Hi {{name}}"
    m.appendReplacement(sb, Matcher.quoteReplacement(v));
}
m.appendTail(sb);
```

Each channel has its own template (an SMS is short, an email can be long). Sending "Hi {{name}}" to a customer is worse than sending nothing, so a missing value throws.

### 5.4 RetryingSender: exponential backoff

```java
for (int i = 1; i <= attempts; i++) {
    try { inner.send(m); return; }
    catch (SendException e) { last = e; if (i < attempts) sleeper.accept(baseMs << (i - 1)); }
}
throw last;
```

`baseMs << (i - 1)` is base × 2^(i−1): 200, 400, … The sleep function is injected, so the test just records `[200, 400]` instead of waiting.

### 5.5 CircuitBreakerSender: stop calling a dead provider

**The analogy:** your home's electrical breaker. When something keeps failing, it **trips** and stops the current instead of letting it keep burning. After a while you try switching it back on.

| State | Meaning | What `send` does |
|---|---|---|
| CLOSED | healthy | call the primary; count consecutive failures |
| OPEN | tripped (≥ threshold failures) | don't call the primary at all: go straight to the fallback |
| HALF_OPEN | cool-down over | let one trial call through: success → CLOSED, failure → OPEN again |

```java
synchronized (this) {
    if (state == State.OPEN && clock.getAsLong() - openedAt >= coolDownMs) state = State.HALF_OPEN;
    tryPrimary = state != State.OPEN;
}
if (tryPrimary) {
    try { primary.send(m); synchronized (this) { state = State.CLOSED; failures = 0; } return; }
    catch (SendException e) {
        synchronized (this) {
            failures++;
            if (state == State.HALF_OPEN || failures >= threshold) { state = State.OPEN; openedAt = clock.getAsLong(); }
        }
        if (fallback == null) throw e;
    }
}
if (fallback == null) throw new SendException("circuit open");
fallback.send(m);
```

The state changes are `synchronized`, but the **network call is outside the lock**, so a slow provider doesn't block other threads from reading the state.

**Why bother?** Without a breaker, every OTP waits for a timeout (and its retries) on a dead provider. With it, after 3 failures OTPs go **straight** to the backup.

### 5.6 RateLimitedSender: per-recipient fixed window

```java
long now = clock.getAsLong(), window = now - now % windowMs;
long[] c = counters.computeIfAbsent(m.to(), k -> new long[]{window, 0});
synchronized (c) {
    if (c[0] != window) { c[0] = window; c[1] = 0; }       // a new hour: reset
    if (c[1] >= limit) throw new SendException("rate limited for " + m.to());
    c[1]++;
}
inner.send(m);
```

A fixed-window counter (see [LLD 04](../04-Rate-Limiter/RateLimiterSystem.md)), locked per recipient so different numbers never contend.

### 5.7 The service pipeline

```java
void notify(NotificationRequest r) {
    if (!seenKeys.add(r.idempotencyKey())) { log DUPLICATE; return; }                    // 1. dedupe
    ...
    if (r.category() != Category.SECURITY && p.mutedCategories().contains(r.category())) // 2. muted category
        { log SKIPPED_PREFERENCE; return; }
    if (r.priority() == Priority.LOW && p.inQuietHours(hourOfDay.getAsInt())) {          // 3. quiet hours
        seenKeys.remove(r.idempotencyKey());          // so the replay later isn't called a duplicate
        deferred.add(r); log DEFERRED_QUIET_HOURS; return;
    }
    params.putIfAbsent("name", u.name());
    for (Channel c : r.channels()) {                                                     // 4. channels in order
        boolean allowed = r.category() == Category.SECURITY || p.enabledChannels().contains(c);
        if (!allowed || u.address(c) == null || !templates.has(r.templateId(), c)) { log SKIPPED; continue; }
        try {
            senders.get(c).send(new Message(c, u.address(c), templates.render(...)));
            log SENT; return;                                                            // first success wins
        } catch (SendException e) { log FAILED; }                                        // try the next channel
    }
}
```

| Step | Simple meaning |
|---|---|
| 1 | `add` returns false if the key was already there, so a second copy of the event stops here |
| 2 | users can mute marketing, but **never security** |
| 3 | LOW priority at night goes into a queue that `flushDeferred()` replays in the morning |
| 4 | push first; no push token → SMS; SMS failed → email |

---

## 6. Dry run (from the real output)

Asha: all channels enabled, quiet 22 → 7. Ravi: email + SMS only, **no push token**, marketing muted.

| # | What happens | Log for that key |
|---|---|---|
| 1 | OrderShipped for Asha (evt-1) | `[PUSH:SENT]`; body "Hi Asha, order A-17 is on the way" (name filled from the profile) |
| 2 | OrderShipped for Ravi (evt-2) | `[PUSH:SKIPPED_PREFERENCE, SMS:SENT]` (no push token, falls back) |
| 3 | the bus delivers evt-1 **again** | `[PUSH:SENT, -:DUPLICATE]`; push delivered only once |
| 4a | marketing to Ravi | `[-:SKIPPED_PREFERENCE]` (muted) |
| 4b | 23:00, LOW marketing to Asha | `[-:DEFERRED_QUIET_HOURS]` |
| 4c | 23:00, CRITICAL OTP to Ravi | `[SMS:SENT]`: quiet hours don't apply |
| 4d | 08:00, `flushDeferred()` → 1 | Asha's: `[-:DEFERRED_QUIET_HOURS, EMAIL:SENT]` |
| 5 | render "shipped" without orderId | exception |
| 6 | email fails twice, then works | `[EMAIL:SENT]`, sleeps `[200, 400]` |

### The circuit breaker step by step (step 7)
Twilio is down; 6 OTPs to 6 different numbers (so the rate limiter stays out of it). The retry decorator makes 2 calls per attempt.

| OTP | Breaker before | What happens | Failures → state |
|---|---|---|---|
| cb-0 | CLOSED | twilio fails twice (retry) → backup msg91 | 1, CLOSED |
| cb-1 | CLOSED | the same | 2, CLOSED |
| cb-2 | CLOSED | the same | 3 → **OPEN** |
| cb-3, 4, 5 | OPEN | twilio **not called**; straight to msg91 | OPEN |

All 6 delivered via the backup. Twilio's call count is 8 (2 earlier SMS + 3 × 2 attempts) and stays 8 for the next OTP. Then twilio recovers and the clock moves 60 s → HALF_OPEN → the trial succeeds → **CLOSED**.

### Rate limit (step 8)
5 SMS to the same number in one hour: 3 SENT, 2 FAILED ("rate limited").

### Concurrency (step 9)
8 threads × 500 requests, keys built so each key is sent several times from different threads: 1,000 distinct keys → **exactly 1,000** pushes.

---

## 7. Concurrency, explained simply

| Shared thing | Protection |
|---|---|
| "have I sent this key?" | `seenKeys.add` (atomic check-and-insert) |
| subscriber lists, logs | `CopyOnWriteArrayList` |
| breaker state | `synchronized` blocks, with the network call outside them |
| per-recipient counters | `synchronized` on that recipient's `long[]` |
| deferred queue | `ConcurrentLinkedQueue` |

**The race that's prevented:** with `if (!seen.contains(k)) { seen.add(k); send(); }`, two threads can both pass the `contains` check and send twice. A single `add` both checks and inserts.

---

## 8. Edge cases and common mistakes

**Edge cases handled:** duplicate events, a missing push token or template for a channel, muted categories, SECURITY bypassing mutes and disabled channels, quiet hours across midnight, missing template params, transient failures, a dead provider, rate limits, concurrent duplicates.

**Honest limitations in this code (good to mention):**
- The key is marked "seen" **before** sending, so if every channel fails, a later retry of the same key is treated as a duplicate. In production you'd record the outcome and allow retries of FAILED keys (or retry from a queue).
- `seenKeys` grows forever; production keeps keys with a TTL (for example, in Redis for 24 hours).
- Everything runs on the caller's thread; production puts requests on a queue per priority and has workers send them (see F1).

**Common mistakes:**
1. The order service calling the email API directly (tight coupling).
2. Subclasses for every combination of retry / rate limit / audit.
3. Retrying a dead provider forever instead of using a circuit breaker.
4. Letting users mute OTPs.
5. Quiet hours as `from <= h < to`, which breaks for 22 → 7.
6. Sending templates with unfilled `{{placeholders}}`.

---

## 9. Interview questions and answers

**Q1. How do producers trigger notifications without knowing about channels?**
> They publish domain events to a bus; the notification service subscribes and maps each event type to a template, category, priority and channel list (Observer).

**Q2. Why decorators?**
> Retry, rate limiting, circuit breaking and auditing are independent concerns that you combine differently per channel. Decorators wrap any sender in any order, one job each; subclasses would explode combinatorially.

**Q3. Explain the circuit breaker.**
> After N consecutive failures it opens and calls fail fast or go to a fallback. After a cool-down it lets one trial through (half-open); success closes it, failure re-opens it. That protects latency and the struggling provider.

**Q4. How do you prevent duplicate notifications?**
> An idempotency key per request (the event id) and an atomic set `add`. Buses deliver at least once, so dedupe is required.

**Q5. How do preferences and quiet hours work?**
> Muted categories and disabled channels are skipped unless the category is SECURITY. LOW-priority requests during quiet hours are deferred and replayed later; CRITICAL ones go immediately.

**Q6. How does channel fallback work?**
> The request lists channels in order; the service tries each allowed channel with an address and a template, and stops at the first success.

### Follow-up questions

**F1. "Make it handle millions of notifications."**
> Put requests on a durable queue (Kafka/SQS), with separate queues per priority so a marketing blast can't delay OTPs. Stateless workers consume, send and record outcomes. Dedupe keys live in Redis with a TTL.

**F2. "A marketing campaign to 10 million users."**
> A campaign job reads the audience in batches and enqueues LOW-priority requests, throttled to the provider's rate limits, respecting each user's local quiet hours.

**F3. "How do you know a message was actually delivered?"**
> Providers send delivery callbacks (webhooks). Store the provider's message id and update the delivery record when the callback arrives; track opens and clicks for email.

**F4. "Retrying could send a duplicate if the provider succeeded but the response was lost."**
> Pass an idempotency key to the provider if it supports one; otherwise accept rare duplicates for non-critical messages. That's at-least-once delivery.

**F5. "Users in different time zones."**
> Store the user's time zone and compute their local hour; the injected `hourOfDay` stands in for that.

**F6. "Add a new channel, like WhatsApp."**
> Add an enum value, an address field, templates, and register a sender (wrapped in whatever decorators it needs). The pipeline doesn't change.

---

## 10. Running the file

```bash
java NotificationSystem.java     # 19/19 tests passed (8 threads, 1,000 distinct keys → exactly 1,000 sends)
```

**One-line summary:** *"Services publish events and the notification service subscribes; each request goes through dedupe by idempotency key, preference and quiet-hour checks, and per-channel templates, then is tried on channels in order. Each channel's sender is a stack of decorators (audit, rate limit, circuit breaker with a backup provider, retry with backoff) around a provider strategy."*
