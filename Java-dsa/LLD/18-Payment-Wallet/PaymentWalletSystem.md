# 18 · Payment Gateway + Wallet (explained simply)

| | |
|---|---|
| **Code** | [PaymentWalletSystem.java](PaymentWalletSystem.java) |
| **Run** | `java PaymentWalletSystem.java` (29 self-checks, including 16 threads where every request is sent twice) |
| **Design notes** | [README.md](README.md) (reconciliation, webhooks, more Q&A) |
| **Prerequisite** | [How to think in LLD + the concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md), [Splitwise ledger](../09-Splitwise/SplitwiseSystem.md) |

When money is involved, "mostly works" isn't good enough. A wallet must never **create** money (a bug that credits twice) or **lose** it (a crash between "subtract here" and "add there"). Accountants solved this 500 years ago with **double-entry bookkeeping**; this design is built on it, plus **idempotency keys** so a retried request never charges twice.

---

## 1. The problem in plain words

Users have wallets. They can:
- **top up** from UPI (free), net banking (₹10 fee), or a card (2% fee, **authorize first and capture later**, like a hotel holding money on your card);
- **pay a merchant** (the platform keeps 2%);
- **send money** to friends (P2P);
- get **refunds**, partial or full, never more than they paid.

And:
- an app that retries because the network dropped must **not** pay twice;
- a wallet can never go negative;
- at any moment, the books must **balance**, and every balance must be re-computable from history.

---

## 2. How to think about it (the 7 steps)

### Step 1 · Clarify

| Question to ask | Answer we'll assume |
|---|---|
| Currency? | One (INR), amounts in **paise** as `long` |
| Payment methods? | UPI, card (two-phase), net banking; each with its own fee |
| Who pays fees? | Top-up fees: the user; merchant payments: the merchant (the platform keeps a %) |
| Overdraft? | Never for users or merchants |
| Retries? | Clients send an idempotency key; duplicates return the first result |
| Refunds? | Partial allowed; the total can't exceed the payment |
| Audit? | Every balance must be derivable from an immutable journal |

### Step 2 · Requirements
1. A ledger where every transaction is a set of postings that **sum to zero**.
2. Postings are applied atomically, with no non-system account going negative.
3. Payment methods are pluggable (fee rule, one- or two-phase).
4. A payment intent state machine: CREATED → AUTHORIZED → CAPTURED → (PARTIALLY_)REFUNDED, or FAILED / VOIDED.
5. Every operation is idempotent by key.
6. Thread-safe and deadlock-free.

### Step 3 · Entities

| Noun | Becomes |
|---|---|
| account (wallet, merchant, system) | `class Account` (balance cache + lock) |
| one line of a transaction | `record Posting(accountId, amount)` (+ credit, − debit) |
| a transaction | `record JournalEntry(id, kind, reference, postings)`: must sum to 0 |
| the books | `class Ledger` |
| payment method | `interface PaymentMethod` (UPI, Card, NetBanking records) |
| a top-up attempt | `class PaymentIntent` + `enum IntentStatus` |
| the API | `class WalletService` |

### Step 4 · Relationships

```
WalletService
  ├── Ledger: accounts {W:asha, W:ravi, M:cafe, SYS:external, SYS:fees} + journal (append-only)
  ├── methods: UPI / CARD / NETBANKING (Strategy, in a registry)
  ├── intents: PI1, PI2, ...   (state machine per top-up)
  └── idempotency: key → CompletableFuture<Result>
```

### Step 5 · API

```java
Result topUp(key, user, amount, MethodType)
Result capture(intentId) / voidAuthorization(intentId)
Result payMerchant(key, user, merchant, amount)
Result transfer(key, from, to, amount)
Result refundPayment(key, paymentEntryId, amount)
```

### Step 6 · Patterns

| What changes / matters | Pattern |
|---|---|
| how each payment method works and what it costs | **Strategy** (`PaymentMethod`) |
| type → method | **Factory / registry** (`EnumMap`) |
| top-up lifecycle | **State machine** (`IntentStatus.canGoTo`) |
| retries | **idempotency key** with a shared future |
| money correctness | **double-entry ledger** (immutable, append-only) |
| refunds | a **reversing entry**, never editing history |

### Step 7 · Concurrency
A transaction touches 2–3 accounts. Lock **all** of them, always in **sorted id order** (so no deadlock), check balances, apply, append, unlock.

---

## 3. The key idea, simply: double-entry bookkeeping

**The naive way:** `asha.balance -= 500; cafe.balance += 500;`. If the program crashes between the two lines, ₹500 vanished. If a bug runs the second line twice, ₹500 appeared from nowhere, and nothing tells you.

**Double entry:** every transaction is a list of postings (+ in, − out) that must **add up to zero**:

| Transaction | Postings | Sum |
|---|---|---|
| Asha tops up ₹1000 by UPI | SYS:external −1,00,000 · W:asha +1,00,000 | 0 |
| Asha pays the cafe ₹500 (2% fee) | W:asha −50,000 · M:cafe +49,000 · SYS:fees +1,000 | 0 |
| Ravi sends Asha ₹90 | W:ravi −9,000 · W:asha +9,000 | 0 |

**The "outside world" is an account too** (`SYS:external`). Money entering the system makes it negative. So:

> **The sum of ALL account balances is always exactly 0.**

If it ever isn't, there's a bug, and the journal shows exactly where. Balances are just a **cache**: you can always re-add every posting in the journal to recompute them (`verify()`).

**The analogy:** a seesaw. Every movement on one side is matched by the other; it can never tip without someone noticing.

---

## 4. Java concepts used in this design

| Concept | In this design | Simple meaning |
|---|---|---|
| `long` paise | every amount | integers never have rounding errors (₹0.1 + ₹0.2 ≠ ₹0.3 in `double`) |
| record **compact constructor** | `JournalEntry { if (sum != 0) throw ...; }` | an unbalanced entry **can't even be created** |
| `List.copyOf` | postings | an immutable copy: nobody can change history afterwards |
| `ReentrantLock` per account | `Account.lock` | different accounts don't block each other |
| `stream().distinct().sorted(...)` | `Ledger.post` | the accounts involved, sorted by id, for lock ordering |
| `Map.merge(k, v, Long::sum)` | net delta per account | adds up several postings to the same account |
| `CompletableFuture` + `putIfAbsent` | `idempotent` | the first caller runs; duplicates wait for and share its result |
| `EnumMap` | method registry | a map keyed by enum: compact and fast |
| `synchronized (pi)` | capture / void | one state change at a time per intent |
| `Math.round(a * 0.02)` | card fee | round to the nearest paisa |

---

## 5. The code, piece by piece

### 5.1 An entry that can't be unbalanced

```java
record JournalEntry(String id, String kind, String reference, List<Posting> postings) {
    JournalEntry {
        if (postings.stream().mapToLong(Posting::amount).sum() != 0) throw new IllegalArgumentException("unbalanced entry " + kind);
        postings = List.copyOf(postings);
    }
}
```

The rule "every entry sums to zero" is enforced by the **type itself**. No code path anywhere can build a bad one.

### 5.2 Posting atomically (the heart of the system)

```java
JournalEntry post(String kind, String reference, List<Posting> postings) {
    JournalEntry draft = new JournalEntry(...);                                  // validates sum == 0
    List<Account> involved = ...accounts in the postings, distinct, sorted by id...;
    involved.forEach(a -> a.lock.lock());                                        // lock ALL, in id order
    try {
        Map<String, Long> delta = new HashMap<>();
        for (Posting p : postings) delta.merge(p.accountId(), p.amount(), Long::sum);
        for (Account a : involved)
            if (!a.mayGoNegative && a.balance + delta.get(a.id) < 0) throw new LedgerException("Insufficient funds in " + a.id);
        for (Account a : involved) a.balance += delta.get(a.id);                 // apply all ...
        journal.add(draft);                                                      // ... and record
        return draft;
    } finally { ...unlock in reverse order... }
}
```

| Step | Simple meaning |
|---|---|
| sorted locking | Asha→Ravi and Ravi→Asha at the same time both lock `W:asha` first, so they can't deadlock |
| **check everything first** | if any account would go negative, throw before changing anything |
| then apply | all balances change together, under the locks |
| `mayGoNegative` | only SYSTEM accounts (the outside world) can go below 0 |

### 5.3 Payment methods (Strategy)

```java
interface PaymentMethod {
    MethodType type(); long feePaise(long amount); boolean twoPhase();
    GatewayResult authorize(String intentId, long amount);
    default GatewayResult capture(...) { ... } default void voidAuth(...) {}
}
```

| Method | Fee | Two-phase? |
|---|---|---|
| UPI | 0 | no: authorize = money moves |
| Card | 2% | **yes**: authorize holds the money, capture moves it |
| NetBanking | ₹10 flat | no |

**Why two-phase for cards?** A hotel or a cab app reserves money first (authorize) and takes the final amount later (capture), or releases the hold (void).

### 5.4 The intent state machine

```java
boolean canGoTo(IntentStatus n) {
    return switch (this) {
        case CREATED -> n == AUTHORIZED || n == CAPTURED || n == FAILED;
        case AUTHORIZED -> n == CAPTURED || n == VOIDED;
        case CAPTURED, PARTIALLY_REFUNDED -> n == PARTIALLY_REFUNDED || n == REFUNDED;
        default -> false;                        // FAILED, VOIDED, REFUNDED are final
    };
}
```

```
CREATED ──(UPI/NB ok)──▶ CAPTURED
   │  └──(card ok)──▶ AUTHORIZED ──capture──▶ CAPTURED
   │                       └──void──▶ VOIDED
   └──(declined)──▶ FAILED
```

That's why "capture twice" fails: the second time, the status is CAPTURED, not AUTHORIZED.

### 5.5 Top-up

```java
GatewayResult auth = m.authorize(pi.id, amount);
if (!auth.ok()) { pi.to(FAILED); return new Result(false, pi.id, auth.error()); }
if (m.twoPhase()) { pi.to(AUTHORIZED); return ...("authorized; capture to credit"); }
creditTopUp(pi); pi.to(CAPTURED);

// creditTopUp: external −amount, wallet +(amount − fee), fees +fee
```

The user is charged the full amount; the wallet gets the amount minus the method fee; the platform's fee account gets the fee.

### 5.6 Idempotency with a shared future

```java
private Result idempotent(String key, Supplier<Result> op) {
    CompletableFuture<Result> mine = new CompletableFuture<>();
    CompletableFuture<Result> prev = idempotency.putIfAbsent(key, mine);
    if (prev != null) return prev.join();                  // duplicate: wait for and return the FIRST result
    Result r;
    try { r = op.get(); } catch (RuntimeException e) { r = new Result(false, null, e.getMessage()); }
    mine.complete(r);
    return r;
}
```

**The analogy:** a restaurant order number. If you show the same number twice, you get the same order, not a second meal. And if two copies arrive **at the same moment**, the second waits for the first to finish (`join`) instead of running again.

**Failures are remembered too:** the retry of "insufficient funds" returns the same failure, even though the retry asked for ₹0.01. That's how Stripe and Razorpay behave: a key means "this exact request".

### 5.7 Refunds: a reversing entry

```java
long paid = -orig.postings().get(0).amount();                       // what the user paid
long already = ...sum of earlier REFUND entries for this payment...;
if (already + amount > paid) throw new LedgerException("Refund exceeds payment ...");
long merchantShare = Math.round((double) amount * orig.postings().get(1).amount() / paid);
long feeShare = amount - merchantShare;
post("REFUND", paymentEntryId, List.of(
        new Posting(user wallet, +amount), new Posting(merchant, -merchantShare), new Posting(FEES, -feeShare)));
```

History is **never edited**. A refund is a **new** entry that moves money back, split proportionally: the merchant returns its 98% share, the platform returns its 2% fee. `feeShare = amount − merchantShare` (not a separate rounding) keeps the entry summing to exactly zero.

---

## 6. Dry run (from the real output)

Merchant fee 2% (200 basis points). Amounts in paise (1,00,000 = ₹1000).

| # | Action | Asha | Ravi | Cafe | Fees |
|---|---|---|---|---|---|
| 1 | Asha UPI top-up 1,00,000 (k1) | 1,00,000 | | | 0 |
| 2 | Ravi net banking 50,000 (k2): ₹10 fee | | 49,000 | | 1,000 |
| 3 | Asha card 20,000 (k3): **authorized only** | 1,00,000 | | | 1,000 |
| 4 | capture PI3: 2% fee = 400 | 1,19,600 | | | 1,400 |
| 5 | capture PI3 again | refused | | | |
| 6 | card 5,000 (k4) then void | 1,19,600 (VOIDED) | | | |
| 7 | UPI top-up PI5 declined | "declined" | | | |
| 8 | **retry k1** | same "credited" result; rail calls still 5; balance unchanged | | | |
| 9 | Asha pays cafe 50,000 (p1) | 69,600 | | 49,000 | 2,400 |
| 10 | Ravi → Asha 9,000 (t1) | 78,600 | 40,000 | | |
| 11 | Ravi → Asha 10,00,000 (t2) | "Insufficient funds in W:ravi"; the retry of t2 gets the **same** failure | | | |
| 12 | refund 20,000 of p1 (r1): cafe −19,600, fees −400 | 98,600 | | 29,400 | 2,000 |
| 13 | refund the remaining 30,000 (r2) → "fully refunded" | 1,28,600 | | 0 | 1,400 |
| 14 | refund 1 more (r3) | rejected: exceeds the payment | | | |

Fees at step 9: net banking 1,000 + card 400 + merchant fee 1,000 = **2,400** ✓.

**Invariants (step 5 of the demo):** every entry sums to 0, the **sum of all accounts is 0**, cached balances equal the balances recomputed from the journal, and building an unbalanced entry throws.

**Concurrency:** 10 wallets × ₹100 = 1,00,000 paise. 16 threads each do 500 random transfers, sending **each one twice** (a retry):
- no deadlock, and no wallet ever went negative;
- the wallet total is still **1,00,000**;
- the journal has **exactly one entry per successful transfer** + 10 seeds = 6,931 (this run), so no retry executed twice;
- the ledger verifies.

---

## 7. Concurrency, explained simply

| Danger | Protection |
|---|---|
| two transfers from the same wallet both pass the balance check | the balance check and the update happen under that account's lock |
| A→B and B→A deadlock | lock accounts in **sorted id order** |
| a retry racing the original | `putIfAbsent` on a future: one runs, the other waits for its result |
| capture and void racing on one intent | `synchronized (pi)` + the state machine |
| half-applied transactions | check everything first, then apply everything, inside the locks |

---

## 8. Edge cases and common mistakes

**Edge cases handled:** zero or negative amounts, transferring to yourself, insufficient funds, declined authorizations, double capture, void, retries (both successful and failed), partial and full refunds, over-refunds, rounding in fees and refund splits.

**Honest limitations in this code (good to mention):**
- The "already refunded" check reads the journal **outside** the account locks, so two refunds of the same payment with different keys at the same moment could both pass it. A fix: lock per payment (or keep a `refunded` counter on the payment, updated under a lock).
- Refund lookup scans the journal (O(n)); production indexes entries by id and reference.
- The idempotency key isn't tied to the request body; real gateways reject a reused key with different parameters.
- The idempotency map lives forever; production keeps keys for a limited time (for example, 24 hours).

**Common mistakes:**
1. `double` for money.
2. `balance -= x; other += x` with no journal, so money silently appears or vanishes.
3. Editing or deleting a payment for a refund instead of adding a reversing entry.
4. Locking accounts in request order (A→B vs B→A deadlock).
5. Checking idempotency with `containsKey` then `put` (two duplicates both run).
6. Crediting a card top-up at authorization instead of capture.

---

## 9. Interview questions and answers

**Q1. Why double-entry?**
> Every transaction is postings summing to zero, written atomically, and the outside world is an account. Money can't be created or destroyed by a bug unnoticed: the sum of all balances is always 0, and balances can be recomputed from the immutable journal for audit and reconciliation.

**Q2. How do you prevent double charging on retries?**
> Clients send an idempotency key. The first request with a key runs; duplicates, even concurrent ones, wait on the same future and get the same result, success or failure.

**Q3. How do you avoid deadlocks in transfers?**
> Lock every involved account in a global order (sorted by id) and release in reverse.

**Q4. How do refunds work?**
> A new REFUND entry referencing the payment, splitting the amount proportionally between the merchant and the platform's fee, and rejecting it if total refunds would exceed the payment. History is never edited.

**Q5. Why authorize and capture for cards?**
> Authorization reserves funds without moving them; capture moves the final amount later; void releases the hold. Hotels, cabs and marketplaces need it. The intent's state machine ensures you can't capture twice or capture after a void.

**Q6. How are payment methods added?**
> Implement `PaymentMethod` (fee rule, two-phase or not, how to call the rail) and register it. The wallet logic doesn't change.

### Follow-up questions

**F1. "The bank says the payment succeeded, but our server crashed before crediting."**
> Use the provider's webhook (with its own idempotency) to credit, plus a periodic **reconciliation** job that compares our intents with the provider's settlement report and fixes stuck ones. Intents in AUTHORIZED/CREATED for too long get queried.

**F2. "Scale beyond one database."**
> Shard accounts by id; a cross-shard transfer becomes a saga: debit with a hold on shard A, credit on shard B, then confirm or compensate, all recorded in the ledger with idempotent steps.

**F3. "A hot merchant account receives thousands of payments per second."**
> Split the merchant into sub-accounts (sharded balances) that are summed for reads, or credit asynchronously from a queue. Credits can't overdraw, so they're easier to batch.

**F4. "Support multiple currencies."**
> Every amount carries a currency; an entry must balance **per currency**; FX conversions use a SYS:fx account with postings in both currencies at a recorded rate.

**F5. "How do you get a user's statement?"**
> Query journal postings for their account ordered by time; the running sum gives the balance after each line.

**F6. "What would you store in the database?"**
> `accounts(id, type, balance, version)`, `journal_entries(id, kind, reference, created_at)`, `postings(entry_id, account_id, amount)`, `idempotency_keys(key, request_hash, response, expires_at)`, `payment_intents(id, status, ...)`. Post each entry in one database transaction with the account rows locked (`SELECT ... FOR UPDATE` in id order).

---

## 10. Running the file

```bash
java PaymentWalletSystem.java     # 29/29 tests passed (16 threads, duplicated retries, money conserved, ledger verifies)
```

**One-line summary:** *"Every money movement is an immutable journal entry whose postings sum to zero (the outside world and fees are accounts), applied atomically by locking the involved accounts in id order and checking for overdrafts before changing anything. Payment methods are strategies with their own fees and authorize/capture behaviour, governed by an intent state machine. Every operation is idempotent through a shared future per key, and refunds are proportional reversing entries."*
