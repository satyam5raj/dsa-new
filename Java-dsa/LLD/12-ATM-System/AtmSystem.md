# 12 · ATM System (explained simply)

| | |
|---|---|
| **Code** | [AtmSystem.java](AtmSystem.java) |
| **Run** | `java AtmSystem.java` (24 self-checks, including 2 ATMs × 8 threads on one joint account) |
| **Design notes** | [README.md](README.md) (state diagram, more Q&A) |
| **Prerequisite** | [How to think in LLD + the concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md) |
| **Related** | [11 · Vending Machine](../11-Vending-Machine/VendingMachineSystem.md) (the State pattern basics) |

This question combines the **State pattern** (the ATM session), a **Chain of Responsibility** (the note dispensers), and, most importantly, **consistency between two systems**: the bank's ledger and the ATM's physical cash. The order of steps is the whole design.

---

## 1. The problem in plain words

A customer inserts a **card**, enters a **PIN**, and can **withdraw**, **deposit** or check the **balance**, then the card is ejected. Rules:
- 3 wrong PINs **block** the card and the ATM keeps it;
- withdrawals must be multiples of ₹100, within the balance and a **daily limit** (₹40,000);
- the ATM must be able to pay the **exact** amount with the notes it has (₹2,000 / ₹500 / ₹200 / ₹100).

The scary part: money moves in **two** places, the bank account and the ATM's cash box.

| What goes wrong | Result |
|---|---|
| the account is debited, but the dispenser jams | the customer is charged and gets **no cash** (the worst outcome) |
| cash comes out, but the debit fails | **free money** |

---

## 2. How to think about it (the 7 steps)

### Step 1 · Clarify

| Question to ask | Answer we'll assume |
|---|---|
| Operations? | Withdraw, deposit, balance |
| PIN rules? | 3 wrong attempts → card blocked and retained |
| Limits? | Multiples of ₹100; daily limit ₹40,000 |
| Note types? | ₹2,000, ₹500, ₹200, ₹100, limited counts per ATM |
| Network retries to the bank? | Yes, so debits must be **idempotent** |
| Dispenser failure? | Possible: the customer must not lose money |
| Joint accounts used at two ATMs at once? | Yes |

### Step 2 · Requirements
1. Session flow: insert card → PIN → operations → eject.
2. PIN lockout after 3 failures.
3. Withdraw: validate → can the ATM pay it? → debit → dispense; **reverse the debit if dispensing fails**.
4. Debits and reversals are idempotent by transaction id.
5. Never overdraw an account, even from two ATMs at once.
6. A jammed ATM goes out of service.

### Step 3 · Entities

| Noun | Becomes |
|---|---|
| bank | `interface BankService` + `InMemoryBank` |
| account | `class Account` (balance, withdrawn today, failed PINs, blocked, lock) |
| ledger entry | `record LedgerEntry(txnId, card, amount, type)` |
| a note slot | `abstract class NoteHandler` + `Note2000`, `Note500`, `Note200`, `Note100` |
| the dispenser | `class CashDispenser` (a chain of note handlers) |
| ATM state | `interface AtmState` + Idle, CardInserted, Authenticated, OutOfService |
| the ATM | `class Atm` (the context) |

### Step 4 · Relationships

```
Atm (context)
   ├── current AtmState
   ├── BankService          (the ATM never touches balances directly)
   └── CashDispenser ── chain: Note2000 → Note500 → Note200 → Note100
InMemoryBank ── accounts (each with its own lock) + ledger (txnId → entry, for idempotency)
```

### Step 5 · The withdrawal flow (the order is the design)

```
1. validate the amount               (multiple of 100)
2. PLAN the notes                    (no side effects: can this ATM pay exactly this?)   → if not, stop: nothing debited
3. DEBIT the bank with a unique txn id (idempotent)                                    → if refused (funds, limit), stop
4. DISPENSE
5. if dispensing fails → REVERSE the debit (a compensating transaction) and go out of service
```

This is the **saga pattern** in miniature: a sequence of steps across systems, where a failure triggers **compensating** actions for the steps already done.

### Step 6 · Patterns

| What changes / matters | Pattern |
|---|---|
| the ATM's behaviour depends on the session state | **State** |
| paying an amount with several note types | **Chain of Responsibility** |
| the fixed order of a withdrawal | **Template Method** (`performWithdrawal`) |
| undoing a debit when a later step fails | **Saga / compensation** |
| retries must not double-debit | **idempotency keys** |

### Step 7 · Concurrency
The contended resource is **one account** (a joint account used at two ATMs). The bank locks per account.

---

## 3. Java concepts used in this design

| Concept | In this design | Simple meaning |
|---|---|---|
| `interface` with many `default` methods | `AtmState` | every state starts by refusing everything; each state only overrides what it allows |
| `abstract class` + a `next` field | `NoteHandler` | a chain: each handler can pass the rest to the next one |
| recursion with back-off | `NoteHandler.plan` | try the most notes first; if the rest can't be paid, try one fewer |
| `TreeMap(Comparator.reverseOrder())` | the plan | denominations printed largest first |
| `ReentrantLock` per account | `Account.lock` | two ATMs on different accounts never wait for each other |
| `ConcurrentHashMap` as a ledger keyed by txn id | idempotency | "already applied?" is one lookup |
| `volatile boolean faulty` | simulating a jam | the test flips it from another place |
| a hashed PIN | `Account.hash` | never store raw PINs (real systems use a hardware security module) |

---

## 4. The code, piece by piece

### 4.1 The bank: PIN lockout, limits, idempotency

```java
public boolean verifyPin(String card, String pin) {
    Account a = acc(card);
    a.lock.lock();
    try {
        if (a.blocked) return false;
        if (a.pinHash.equals(Account.hash(pin))) { a.failedPins = 0; return true; }
        if (++a.failedPins >= 3) a.blocked = true;
        return false;
    } finally { a.lock.unlock(); }
}

public void debit(String txnId, String card, long amount) {
    Account a = acc(card);
    a.lock.lock();                                              // per-ACCOUNT lock: two ATMs, one account
    try {
        if (ledger.containsKey(txnId)) return;                 // a retry of the same txn: already applied
        if (a.balance < amount) throw new BankException("Insufficient funds");
        if (a.withdrawnToday + amount > DAILY_LIMIT) throw new BankException("Daily limit exceeded");
        a.balance -= amount;
        a.withdrawnToday += amount;
        ledger.put(txnId, new LedgerEntry(txnId, card, -amount, "DEBIT"));
    } finally { a.lock.unlock(); }
}
```

| Rule | Where |
|---|---|
| 3 wrong PINs → blocked | `++a.failedPins >= 3` |
| a correct PIN resets the counter | `a.failedPins = 0` |
| a retried debit does nothing | `if (ledger.containsKey(txnId)) return;` |
| no overdraft, daily limit | checked under the account's lock |

### 4.2 Reversal: idempotent too

```java
public void reverse(String txnId) {
    LedgerEntry e = ledger.get(txnId);
    if (e == null || !e.type().equals("DEBIT")) return;
    String revId = "REV-" + txnId;
    Account a = acc(e.cardNo());
    a.lock.lock();
    try {
        if (ledger.containsKey(revId)) return;                 // reversal is idempotent too
        a.balance += -e.amount();
        a.withdrawnToday -= -e.amount();
        ledger.put(revId, new LedgerEntry(revId, e.cardNo(), -e.amount(), "REVERSAL"));
    } finally { a.lock.unlock(); }
}
```

The reversal is a **new** ledger entry (`REV-<txn>`), not an edit. Reversing twice does nothing the second time.

### 4.3 The dispenser: a chain of note handlers

```java
Map<Integer, Integer> plan(int amount, Map<Integer, Integer> acc) {
    int use = Math.min(count, amount / denomination);
    for (; use >= 0; use--) {                                   // try most notes first, back off if the rest fails
        int rest = amount - use * denomination;
        Map<Integer, Integer> attempt = new TreeMap<>(Comparator.reverseOrder());
        attempt.putAll(acc);
        if (use > 0) attempt.put(denomination, use);
        if (rest == 0) return attempt;
        if (next != null) { Map<Integer, Integer> r = next.plan(rest, attempt); if (r != null) return r; }
    }
    return null;
}
```

**Chain of Responsibility, simply:** a line of cashiers, each with one kind of note. The ₹2,000 cashier pays as many ₹2,000 notes as fit and hands the rest to the ₹500 cashier, and so on.

**With back-off:** if the rest can't be paid further down the line, the cashier tries one fewer of their notes. Example: ₹600 with one ₹500 and three ₹200 notes. Greedy would take the ₹500, leaving ₹100, which nobody can pay. Backing off to zero ₹500 notes gives 3 × ₹200. (It's the same "greedy fails with limited stock" lesson as the [vending machine](../11-Vending-Machine/VendingMachineSystem.md#4-change-making-why-greedy-fails), solved here by backtracking.)

`plan` has **no side effects**. `dispense` actually removes notes:

```java
synchronized void dispense(Map<Integer, Integer> notes) {
    if (faulty) throw new IllegalStateException("Dispenser jammed");
    for (var e : notes.entrySet()) if (byDenomination.get(e.getKey()).count < e.getValue()) throw new IllegalStateException("Notes changed since planning");
    notes.forEach((d, n) -> byDenomination.get(d).count -= n);
}
```

Splitting **plan** and **commit** lets the ATM say "can't dispense that amount" **before** debiting anyone.

### 4.4 The ATM states

```java
interface AtmState {
    default void insertCard(Atm atm, String card) { atm.say("Card already inserted"); }
    default void enterPin(Atm atm, String pin) { atm.say("Insert card first"); }
    default void withdraw(Atm atm, int amount) { atm.say("Not authenticated"); }
    default void deposit(Atm atm, int amount) { atm.say("Not authenticated"); }
    default void balance(Atm atm) { atm.say("Not authenticated"); }
    default void eject(Atm atm) { atm.say("No card"); }
    String name();
}
```

| State | Allows | On the event |
|---|---|---|
| **IDLE** | insert card | blocked card → refused; otherwise → CARD_INSERTED |
| **CARD_INSERTED** | enter PIN, eject | right PIN → AUTHENTICATED; 3rd wrong → card retained → IDLE |
| **AUTHENTICATED** | withdraw, deposit, balance, eject | eject → IDLE |
| **OUT_OF_SERVICE** | nothing | "Out of service" |

```
IDLE ──insertCard──> CARD_INSERTED ──right PIN──> AUTHENTICATED ──eject──> IDLE
                          │ 3rd wrong PIN (card retained)          │ dispenser jams
                          └──────────────> IDLE                    └──> OUT_OF_SERVICE
```

### 4.5 The withdrawal template

```java
void performWithdrawal(int amount) {
    if (amount <= 0 || amount % 100 != 0) { say("Amount must be a multiple of 100"); return; }
    Optional<Map<Integer, Integer>> plan = dispenser.plan(amount);
    if (plan.isEmpty()) { say("ATM cannot dispense " + amount + " - try another amount"); return; }   // nothing debited
    String txn = newTxnId();
    try {
        bank.debit(txn, card, amount);
    } catch (BankException e) { say(e.getMessage()); return; }
    try {
        dispenser.dispense(plan.get());
        say("Dispensed " + plan.get());
    } catch (IllegalStateException fault) {
        bank.reverse(txn);                                          // COMPENSATE: customer must not lose money
        say("Could not dispense - your account was not charged");
        setState(outOfService);                                     // a jammed ATM stops serving
        card = null;
    }
}
```

The steps exactly match section 2: validate → plan → debit → dispense → (on failure) reverse. Each transaction gets a unique id (`ATM1-1`, `ATM1-2`, …), which is what makes the debit and the reversal safe to retry.

---

## 5. Dry run (from the real output)

The bank: card 4111 (PIN 1234, ₹50,000) and card 4222 (PIN 9999, ₹10,000). ATM1 has 20 notes of each type (₹56,000).

| Step | State | Result |
|---|---|---|
| withdraw with no card | IDLE | "Not authenticated" |
| insert 4222, PINs 0000, 1111 | CARD_INSERTED | still CARD_INSERTED |
| PIN 2222 (3rd wrong) | → IDLE | "Too many wrong PINs - card retained" |
| insert 4222 again | IDLE | "Card blocked - contact your bank" |
| insert 4111, PIN 1234 | → AUTHENTICATED | |
| withdraw ₹3,700 | | ₹2,000 × 1 + ₹500 × 3 + ₹200 × 1; balance ₹46,300 |
| withdraw ₹150 | | "Amount must be a multiple of 100" |
| withdraw ₹40,000 | 3,700 + 40,000 > 40,000 | "Daily limit exceeded" |
| check balance | | "Balance 46300" |
| ATM2 (only ₹2,000 notes) withdraw ₹2,500 | the plan fails | "ATM cannot dispense 2500"; **the account is untouched** |
| ATM3 (one ₹500, three ₹200) withdraw ₹600 | the ₹500 path fails → back off | ₹200 × 3 |
| ATM4 jams, withdraw ₹1,000 | debit, dispense fails, **reverse** | "your account was not charged"; balance restored to ₹45,700; ATM4 OUT_OF_SERVICE |
| insert a card into ATM4 | OUT_OF_SERVICE | "Out of service" |
| debit T-1 twice (a retry) | | debited once: ₹5,000 → ₹4,000 |
| reverse T-1 twice | | reversed once: back to ₹5,000 |

**The race:** a joint account with ₹30,000; 16 threads across 2 ATMs each try 100 × ₹100. Result: never overdrawn, the cash handed out exactly equals the money debited (₹30,000), within the daily limit, and the dispensers lost exactly that much cash.

---

## 6. Concurrency, explained simply

**The danger:** two ATMs withdraw from the same joint account at the same instant. Both read "balance ₹100", both dispense ₹100: overdrawn.

**The protection:** the bank does "check balance and limit → subtract → record" under **that account's lock**. Two ATMs on the same account take turns; ATMs on different accounts don't wait for each other.

**Each ATM** is one physical device, so its own methods are `synchronized` (one customer session at a time), and its dispenser is `synchronized` too.

**Idempotency** protects against the network: if the ATM's debit request times out and it retries with the same txn id, the bank applies it only once.

---

## 7. Edge cases and common mistakes

**Edge cases handled:** operations before inserting a card or entering the PIN, wrong PINs and lockout, a blocked card, invalid amounts, insufficient funds, the daily limit, the ATM unable to make the exact amount, a dispenser jam after the debit, network retries of debits and reversals, two ATMs on one account.

**Common mistakes:**
1. Debiting **before** checking the ATM can pay the amount (then you need to reverse even when nothing went wrong).
2. Dispensing before debiting (free money if the debit fails).
3. No compensation when the dispenser fails after the debit.
4. Non-idempotent debits (a network retry charges twice).
5. A greedy note plan with no back-off (₹600 fails with one ₹500 and three ₹200 notes).
6. The ATM changing balances itself instead of asking the bank.
7. Storing raw PINs.

---

## 8. Interview questions and answers

**Q1. In what order do you do a withdrawal, and why?**
> Validate, plan the notes (no side effects), debit the bank with a unique transaction id, dispense, and reverse the debit if dispensing fails. Planning first avoids debiting when the ATM can't pay; debiting before dispensing avoids free money; the reversal protects the customer if the hardware fails.

**Q2. What's a compensating transaction?**
> An action that undoes the effect of an earlier step when a later step fails, because the steps span systems that can't share one database transaction. Here: a reversal entry that credits back the debit. That's the saga pattern.

**Q3. Why does the debit need a transaction id?**
> Networks time out. If the ATM retries, the bank must recognise the same transaction and not debit again. The same applies to the reversal.

**Q4. Explain the Chain of Responsibility in the dispenser.**
> Each handler owns one denomination: it takes as many of its notes as fit and passes the remainder to the next handler. If the remainder can't be paid, it backs off by one note and tries again.

**Q5. How do you handle two ATMs using one account?**
> The bank locks per account around check-and-debit, so the balance and the daily limit are always checked against the latest values.

**Q6. Why does the ATM go out of service after a jam?**
> A jammed dispenser can't be trusted to dispense correctly; continuing would risk more failed or partial withdrawals. An operator must clear it.

### Follow-up questions

**F1. "The ATM loses power after debiting but before dispensing."**
> When it restarts, it checks its journal of in-flight transactions: a debit without a recorded dispense is reversed (or reconciled against the dispenser's counters). Banks also reconcile ATM cash against the ledger daily.

**F2. "Partial dispense: 3 of 5 notes came out."**
> The dispenser reports what was actually dispensed; the bank reverses the difference (a partial reversal), and the ATM goes out of service.

**F3. "Add mini statements or PIN change."**
> More operations in the AUTHENTICATED state, each calling the bank. No other state changes.

**F4. "Card-less withdrawal with a one-time code."**
> A new path into AUTHENTICATED: verify a code from the bank instead of card + PIN.

**F5. "A daily limit that resets at midnight."**
> Store the date with `withdrawnToday`; if the stored date isn't today, reset the counter before checking.

---

## 9. Running the file

```bash
java AtmSystem.java      # 24/24 tests passed (including 2 ATMs x 8 threads on one joint account)
```

**One-line summary:** *"A State pattern drives the session (Idle → CardInserted → Authenticated, with PIN lockout and out-of-service); withdrawals follow plan → idempotent debit → dispense → reverse on failure (a saga); a Chain of Responsibility of note handlers plans the notes with back-off before anything is debited; and the bank locks per account so two ATMs can't overdraw a joint account."*
