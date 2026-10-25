# 05 · URL Shortener (explained simply)

| | |
|---|---|
| **Code** | [UrlShortenerSystem.java](UrlShortenerSystem.java) |
| **Run** | `java UrlShortenerSystem.java` (27 self-checks, including 16 threads creating 20,000 links) |
| **Design notes** | [README.md](README.md) (301 vs 302, capacity numbers, more Q&A) |
| **Prerequisite** | [How to think in LLD + the concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md) |

This question is half LLD (how to generate unique short codes, the classes, the rules) and half HLD (scale, caching, sharding). This note covers the LLD half: base62, four ways to generate codes, collisions, and the atomic insert that guarantees uniqueness.

---

## 1. The problem in plain words

Users give us a long URL like `https://example.com/a/very/long/path` and get back a short one like `sho.rt/0000q0U`. When someone opens the short link, we **redirect** them to the long URL. We also want:
- **custom aliases** (`sho.rt/launch`),
- links that **expire**,
- **click counts**,
- only the owner can **delete** a link.

Redirects happen far more often than shortening (often 100×), so `resolve` must be fast.

---

## 2. How to think about it (the 7 steps)

### Step 1 · Clarify

| Question to ask | Answer we'll assume |
|---|---|
| How long are codes? | 7 characters from [0-9a-zA-Z] |
| Custom aliases? | Yes, 3–30 characters, some words reserved (`api`, `admin`, …) |
| Same URL shortened twice by the same user? | Return the **same** code (don't waste codes) |
| Do links expire? | Optionally (a time-to-live) |
| Analytics? | Click counts |
| Must codes be unguessable? | It depends: discussed as a trade-off between generators |
| Scale? | Huge in reality (billions of links); here, in-memory with the right interfaces |

### Step 2 · Requirements
1. `shorten(url, owner, alias?, ttl?)` → a code.
2. `resolve(code)` → the long URL (or not found / expired).
3. Validate URLs and aliases.
4. Codes are **unique**, even with many servers and threads.
5. The way codes are generated is swappable.
6. Storage is swappable (in memory now, a database later).

### Step 3 · Entities

| Noun | Becomes |
|---|---|
| a short link | `record ShortUrl(code, longUrl, owner, createdAt, expiresAt)` |
| the code alphabet | `class Base62` (number ↔ string) |
| how codes are made | `interface KeyGenerationStrategy` + 4 implementations |
| storage | `interface UrlRepository` + `InMemoryUrlRepository` |
| click analytics | `interface ClickListener` + `ClickCounter` |
| the service | `class UrlShortenerService` |

### Step 4 · Relationships

```
UrlShortenerService
   ├── uses KeyGenerationStrategy  (Counter / Range / Hash / Random)
   ├── uses UrlRepository          (insertIfAbsent = the uniqueness guarantee)
   └── tells ClickListener(s)      (analytics)
```

### Step 5 · API

```java
String shorten(String longUrl, String owner)
String shorten(String longUrl, String owner, String alias, long ttlMs)
Optional<String> resolve(String code)
void delete(String code, String requester)
```

### Step 6 · Patterns

| What changes / matters | Pattern |
|---|---|
| how codes are generated | **Strategy** (`KeyGenerationStrategy`) |
| where links are stored | **Repository** interface |
| analytics on each click | **Observer** (`ClickListener`) |

### Step 7 · Concurrency
The contended resource is **a code**: two requests must never get the same code, and two users must never both get the alias "promo". The final safety net is an **atomic "insert if absent"** in storage.

---

## 3. The core idea: a short code is just a number

**Base 62** means writing a number with 62 "digits": `0-9`, `a-z`, `A-Z`. Just like decimal uses 10 digits and binary uses 2.

```
125 in base 62:  125 = 2 × 62 + 1   →  digits [2, 1]  →  "21"
```

With 7 characters there are **62⁷ ≈ 3.5 trillion** codes. So the real design question is: **how do we pick the number** so it's unique, without a bottleneck?

| Generator | How | Good | Bad |
|---|---|---|---|
| **Counter** | a global counter: 100000, 100001, … | unique by construction, no retries | one point of contention; codes are **predictable** (anyone can enumerate links) |
| **Range** | each server leases a **block** of 1,000 numbers at a time | a counter without contention: coordination once per 1,000 links | gaps when a server dies (that's fine) |
| **Hash** | hash the URL, take 42 bits | the same URL always gets the same code (natural de-duplication) | **collisions** must be handled |
| **Random** | 7 random characters | unpredictable | collisions (rare until the space fills up) |

Whatever the generator, the **final safety net** is the storage's atomic insert-if-absent.

---

## 4. Java concepts used in this design

| Concept | In this design | Simple meaning |
|---|---|---|
| `StringBuilder.reverse()` | base62 encode | digits come out least-significant first, so reverse at the end |
| `String.indexOf(char)` | base62 decode | the digit value of a character |
| `MessageDigest.getInstance("SHA-256")` | the hash generator | a standard cryptographic hash |
| bit operations `(v << 8) \| (b & 0xff)` | turning hash bytes into a `long` | shift left by 8 bits and add the next byte |
| `& ((1L << 42) - 1)` | keep 42 bits | a mask of 42 one-bits |
| `ToLongFunction<String>` | an injectable hash function | lets the test plug in a deliberately weak hash |
| `ConcurrentHashMap.putIfAbsent` | `insertIfAbsent` | add only if the key isn't there; returns the existing value otherwise, atomically |
| `synchronized` method | `RangeKeyGenerator.generate` | only one thread at a time hands out ids from this server's block |
| `LongAdder` | click counts | a counter optimised for many threads incrementing at once |
| `String.matches(regex)` | alias validation | `[0-9a-zA-Z_-]{3,30}` = 3 to 30 of those characters |
| interface `default` method | `mayCollide()` | a default implementation that generators can override |

---

## 5. The code, piece by piece

### 5.1 Base62

```java
static String encode(long n) {
    if (n < 0) throw new IllegalArgumentException("negative");
    if (n == 0) return "0";
    StringBuilder sb = new StringBuilder();
    while (n > 0) { sb.append(ALPHABET.charAt((int) (n % 62))); n /= 62; }   // least significant digit first
    return sb.reverse().toString();
}

static long decode(String s) {
    long n = 0;
    for (char c : s.toCharArray()) {
        int d = ALPHABET.indexOf(c);
        if (d < 0) throw new IllegalArgumentException("bad char " + c);
        n = n * 62 + d;
    }
    return n;
}
```

The same idea as converting a number to binary: repeatedly take `n % 62` (the last digit) and divide by 62. Decoding goes the other way: multiply by 62 and add each digit. `padded(n, 7)` adds leading zeros so every code is 7 characters (`0000q0U`).

### 5.2 Counter and range generators

```java
static class CounterKeyGenerator implements KeyGenerationStrategy {
    private final AtomicLong counter;
    public String generate(String url, int attempt) { return Base62.padded(counter.getAndIncrement(), 7); }
    public boolean mayCollide() { return false; }
}

static class RangeKeyGenerator implements KeyGenerationStrategy {
    private long next, end;                                     // guarded by `this`
    public synchronized String generate(String url, int attempt) {
        if (next >= end) { next = coordinator.lease(blockSize); end = next + blockSize; }
        return Base62.padded(next++, 7);
    }
}
```

**Range allocation, simply:** instead of asking a central counter for **every** id, each server asks once for a block ("give me 1,000 numbers") and hands them out locally. Like a cashier taking a roll of 100 coins instead of asking the bank for each coin. The central coordinator (in real life ZooKeeper, or a database sequence) is touched once per 1,000 links.

### 5.3 Hash generator: same URL, same code

```java
public String generate(String url, int attempt) {
    String input = attempt == 0 ? url : url + "#" + attempt;    // salt on retry
    long h = hash.applyAsLong(input) & ((1L << 42) - 1);        // 42 bits < 62^7
    return Base62.padded(h % 3_521_614_606_208L, 7);           // 62^7
}
```

| Line | Simple meaning |
|---|---|
| the salt | on a retry, hash `url#1`, `url#2`, … to get a different code |
| 42 bits | 2⁴² ≈ 4.4 trillion, roughly 62⁷ |
| `% 62⁷` | make sure the number fits in 7 base62 characters |

A **collision** means two different URLs got the same code. With a retry and a salt, the second URL simply gets a different code.

### 5.4 Random generator

```java
public String generate(String url, int attempt) {
    return Base62.padded(Math.floorMod(rnd.nextLong(), 3_521_614_606_208L), 7);
}
```

Unpredictable codes. The chance of a collision stays tiny until billions of links exist, and a collision just means "try again".

### 5.5 Storage: the uniqueness guarantee

```java
interface UrlRepository {
    /** Atomic insert: returns false if the code already exists. THE uniqueness guarantee. */
    boolean insertIfAbsent(ShortUrl u);
    Optional<ShortUrl> find(String code);
    void delete(String code);
}

static class InMemoryUrlRepository implements UrlRepository {
    private final ConcurrentHashMap<String, ShortUrl> byCode = new ConcurrentHashMap<>();
    public boolean insertIfAbsent(ShortUrl u) { return byCode.putIfAbsent(u.code(), u) == null; }
}
```

`putIfAbsent` is atomic: if two threads try to insert the same code, exactly one succeeds. In a database, this is a **primary key / unique constraint**: the second `INSERT` fails.

### 5.6 Shorten

```java
String shorten(String longUrl, String owner, String alias, long ttlMs) {
    validateUrl(longUrl);
    long now = clock.getAsLong();
    long expiresAt = ttlMs > 0 ? now + ttlMs : 0;
    if (alias != null) {
        validateAlias(alias);
        if (!repo.insertIfAbsent(new ShortUrl(alias, longUrl, owner, now, expiresAt)))
            throw new IllegalStateException("Alias '" + alias + "' is taken");
        return alias;
    }
    String dedupeKey = owner + "|" + longUrl;
    String existing = dedupe.get(dedupeKey);
    if (existing != null && repo.find(existing).filter(u -> !u.expired(now)).isPresent()) return existing;

    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
        String code = keygen.generate(longUrl, attempt);
        if (repo.insertIfAbsent(new ShortUrl(code, longUrl, owner, now, expiresAt))) {   // atomic
            dedupe.put(dedupeKey, code);
            return code;
        }
        collisions.incrementAndGet();
        if (!keygen.mayCollide()) throw new IllegalStateException("Generator produced a duplicate - bug");
    }
    throw new IllegalStateException("Could not allocate a code after " + MAX_ATTEMPTS + " attempts");
}
```

In plain words:
1. Validate the URL (must be http/https, at most 2,048 characters, so `javascript:` links are rejected).
2. **Custom alias:** validate it, then insert-if-absent. If it's taken, fail.
3. **Same owner + same URL as before?** Return the existing code (if it hasn't expired).
4. Otherwise, try up to 8 codes from the generator; the first one that inserts successfully wins.
5. If a generator that **can't** collide (counter, range) produces a duplicate, that's a bug: fail loudly.

### 5.7 Resolve: the fast path

```java
Optional<String> resolve(String code) {
    long now = clock.getAsLong();
    Optional<ShortUrl> u = repo.find(code);
    if (u.isEmpty()) return Optional.empty();
    if (u.get().expired(now)) { repo.delete(code); return Optional.empty(); }    // lazy expiry
    listeners.forEach(l -> l.onClick(code, now));                 // async in production (Kafka)
    return Optional.of(u.get().longUrl());
}
```

One lookup, a **lazy expiry** check (an expired link is deleted the first time someone tries it), and an analytics event. In production the analytics would be sent asynchronously (for example through Kafka) so they never slow the redirect.

### 5.8 Analytics with `LongAdder`

```java
static class ClickCounter implements ClickListener {
    final ConcurrentHashMap<String, LongAdder> clicks = new ConcurrentHashMap<>();
    public void onClick(String code, long at) { clicks.computeIfAbsent(code, k -> new LongAdder()).increment(); }
}
```

`LongAdder` keeps several internal counters that threads update separately and adds them up when you read it. Much faster than one `AtomicLong` when thousands of clicks hit the same viral link.

---

## 6. Dry run (from the real output)

| Step | What happens | Result |
|---|---|---|
| encode 125 | 2 × 62 + 1 | `"21"` |
| encode 62⁷ − 1 | the largest 7-character code | `ZZZZZZZ` |
| Alice shortens a long URL (counter from 100,000) | a 7-character code | `0000q0U` |
| resolve it | | the long URL |
| Alice shortens the same URL again | de-duplicated | the same `0000q0U` |
| Bob shortens the same URL | a different owner | a new code |
| resolve 3 times in total | | 3 clicks |
| resolve `nope123` | | not found (404) |
| alias `launch` | | `launch` |
| Bob tries alias `launch` | taken | `IllegalStateException` |
| alias `admin` | reserved | `IllegalArgumentException` |
| `javascript:alert(1)` | not http(s) | rejected |
| a link with a 60 s TTL | at 59.999 s: works; at 60 s: gone | lazy expiry |
| Bob deletes `launch` | not the owner | `SecurityException` |
| hash strategy, same URL twice | | the same code (`isLXLk1`) |
| a **weak hash with only 4 values**, 4 URLs | all collide at first | 4 codes, after 7 salted retries |
| a 5th URL in that 4-code space | | "Could not allocate a code after 8 attempts" |
| range: 2 servers × 2,500 links, blocks of 1,000 | | 5,000 unique links, only **6 leases** |
| 16 threads × 1,250 links, for counter / random / hash | | 20,000 unique each |
| 10 threads race for alias `promo` | | exactly 1 winner |

---

## 7. Concurrency, explained simply

**Danger 1: two requests get the same code.** Whatever the generator does, the code only "exists" once `insertIfAbsent` succeeds, and that's atomic. A loser simply tries another code.

**Danger 2: two users get the same alias.** Same protection: `insertIfAbsent(alias)`, exactly one winner.

**The range generator** is `synchronized` so one server's threads don't hand out the same number from its block. Different servers have different blocks, so they never need to talk to each other.

**One honest limitation:** the de-duplication check ("has this owner already shortened this URL?") is a separate read before the insert. If the **same** owner shortens the **same** URL from two threads at the exact same instant, they may get two different codes. Both codes work, so it's harmless; if it mattered, you'd make the dedupe map's update atomic (`computeIfAbsent`) or add a unique constraint on (owner, url) in the database.

---

## 8. Edge cases and common mistakes

**Edge cases handled:** invalid URLs, invalid or reserved aliases, an alias already taken, expired links, deleting someone else's link, an exhausted code space (a clear error rather than an infinite loop).

**Common mistakes:**
1. Generating a code and checking "does it exist?" with a separate `get` before inserting (a race).
2. A global counter that every request hits (a bottleneck), with no mention of ranges.
3. Ignoring hash collisions.
4. Retrying collisions forever.
5. Predictable codes when links should be private.
6. Doing analytics synchronously on the redirect path.

---

## 9. Interview questions and answers

**Q1. How do you turn a number into a short code?**
> Write it in base 62 (digits, lowercase, uppercase). Seven characters give about 3.5 trillion codes.

**Q2. Compare the ways of generating codes.**
> Counter: unique and simple, but a contention point and predictable. Ranges: each server leases blocks of ids, so coordination is rare. Hash: the same URL maps to the same code (natural de-duplication) but collisions need handling. Random: unpredictable, with retry on the rare collision.

**Q3. What actually guarantees uniqueness?**
> An atomic insert-if-absent in storage: `putIfAbsent` in memory, a primary key or unique constraint in a database. Generators only propose candidates.

**Q4. How do you handle hash collisions?**
> If the insert fails, re-hash with a salt (the attempt number) and try again, up to a limit; then fail with a clear error.

**Q5. 301 or 302 redirect?**
> 301 (permanent) lets browsers cache the redirect, which reduces load but hides repeat clicks from analytics. 302 (temporary) sends every click through you, so analytics stay accurate. Most shorteners use 302 for that reason.

**Q6. How do expiring links work?**
> Store an absolute `expiresAt`; on resolve, if it has passed, delete the link and return not found (lazy expiry). A periodic cleanup job can remove links nobody visits.

**Q7. How do you make redirects fast?**
> Cache popular codes (in memory or Redis) in front of the database, and send analytics asynchronously.

### Follow-up questions

**F1. "Codes must not be guessable."**
> Use the random generator (or hash with a secret salt) instead of a counter, and consider longer codes for private links.

**F2. "Scale to billions of links across many servers."**
> Range allocation from ZooKeeper or a database sequence for ids; a key-value store (DynamoDB, Cassandra) sharded by code; a cache for hot codes; analytics through Kafka.

**F3. "Block malicious URLs."**
> Check URLs against a blocklist / safe-browsing service at shorten time, and re-check periodically.

**F4. "Let users edit where a link points."**
> Allow updating `longUrl` for the owner. With 302 redirects, the change takes effect immediately; with 301, browsers may keep the old target cached.

**F5. "Show per-country or per-day click stats."**
> The click event carries the timestamp, country and referrer; a stream processor aggregates them into a stats table.

---

## 10. Running the file

```bash
java UrlShortenerSystem.java      # 27/27 tests passed (including 20,000 links from 16 threads and a 10-thread alias race)
```

**One-line summary:** *"A short code is a number in base 62; a swappable Strategy proposes candidates (counter, leased ranges, hash with salted retries, or random), and an atomic insert-if-absent in the repository is the final uniqueness guarantee; resolve is a fast lookup with lazy expiry and click events."*
