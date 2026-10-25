# 09 · Splitwise: Expense Sharing (explained simply)

| | |
|---|---|
| **Code** | [SplitwiseSystem.java](SplitwiseSystem.java) |
| **Run** | `java SplitwiseSystem.java` (23 self-checks, including 16 threads adding 4,000 expenses) |
| **Design notes** | [README.md](README.md) (more Q&A) |
| **Prerequisite** | [How to think in LLD + the concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md) |

This question tests money handling (exact splits with **no lost paise**), a swappable split strategy, an **append-only ledger** with derived balances, and the debt-simplification algorithm (and why the perfect version is too hard).

---

## 1. The problem in plain words

Friends share costs on a trip. Someone pays for the hotel, someone else pays for dinner. Splitwise records each **expense** (who paid, how much, and how it's split), keeps a running **balance** per person ("you're owed ₹8,000", "you owe ₹3,600"), and suggests the **fewest payments** to settle up. Expenses can be split:
- **equally** (₹100 ÷ 3),
- by **exact amounts** (A: ₹1,000, B: ₹2,000, C: ₹1,000),
- by **percent** (40% / 60%),
- by **shares** (a couple counts as 2 shares, a single person as 1).

A mistaken expense can be **deleted**, but history should never be rewritten.

---

## 2. How to think about it (the 7 steps)

### Step 1 · Clarify

| Question to ask | Answer we'll assume |
|---|---|
| Groups? | Yes; expenses and balances live in a group |
| Split types? | Equal, exact, percent, shares, **and more later** |
| Money precision? | Paise (`long`). ₹100 ÷ 3 must still add up to exactly ₹100 |
| Settle-up suggestions? | Yes, with few transfers |
| Editing or deleting expenses? | Delete = add a reversing entry (audit trail) |
| Multiple currencies, receipts, notifications? | Out of scope (notifications via a listener) |

### Step 2 · Requirements
1. Add an expense with a split type; validate the inputs.
2. Show net balances per person.
3. Suggest a settlement plan; record settle-ups.
4. Delete an expense without rewriting history.
5. **The invariant: in every group, the balances always add up to exactly 0.**
6. Thread-safe when many people add expenses at once.

### Step 3 · Entities

| Noun | Becomes |
|---|---|
| user | `record User(id, name)` |
| group | `class Group` (members, ledger, balances, lock) |
| expense | `record Expense(id, groupId, description, paidBy, amount, splits, reversal)` (**immutable**) |
| one person's share | `record Split(userId, amountPaise)` |
| split type | `enum SplitType { EQUAL, EXACT, PERCENT, SHARE }` |
| how to split | `interface SplitStrategy` + 4 implementations |
| a suggested payment | `record Transfer(from, to, amount)` |
| how to simplify debts | `interface DebtSimplifier` (greedy) + an optimal counter |
| the service | `class ExpenseService` |

### Step 4 · Relationships

```
ExpenseService
   ├── Group ── has ──> members, ledger (List<Expense>, append-only), net balances (derived)
   ├── SplitStrategy (by SplitType)     ← a small factory map
   ├── DebtSimplifier                   ← greedy
   └── ExpenseListener(s)               ← activity feed
Expense ── has ──> List<Split>
```

**Ledger vs balances:** the **ledger** (the list of expenses) is the truth. The **balances** are just a running total computed from the ledger, kept up to date for speed, and can always be rebuilt by **replaying** the ledger.

### Step 5 · API

```java
Expense addExpense(groupId, description, paidBy, amountPaise, SplitType type, participants, params)
Expense settle(groupId, from, to, amountPaise)
Expense delete(groupId, expenseId)
Map<String, Long> balances(groupId)
List<Transfer> settlementPlan(groupId)
```

### Step 6 · Patterns

| What changes / matters | Pattern |
|---|---|
| how an amount is split | **Strategy** (`SplitStrategy`) |
| picking the strategy by type | **Factory** (`STRATEGIES` map) |
| how debts are simplified | **Strategy** (`DebtSimplifier`) |
| the activity feed | **Observer** (`ExpenseListener`) |
| never rewriting history | **append-only ledger** + reversals (like accounting) |

### Step 7 · Concurrency
The contended resource is **one group's ledger and balances**. They must change together, so each group has its own lock.

---

## 3. The two key ideas, simply

### Idea 1: never lose a paisa
₹100 ÷ 3 = ₹33.333… You can't pay a third of a paisa. If everyone pays ₹33.33, that's ₹99.99, and **1 paisa disappears**. The fix: compute each person's share rounded down, then hand the leftover paise one at a time to the people who were rounded down the most (the **largest remainder method**). Result: ₹33.34, ₹33.33, ₹33.33, which adds up to exactly ₹100.

### Idea 2: balances always sum to zero
Every expense adds **+amount** to the payer and **−share** to each participant. Since the shares add up exactly to the amount, each expense adds 0 to the total. So, across the whole group, **what people are owed equals what people owe**. If that ever stops being true, there's a bug.

---

## 4. Java concepts used in this design

| Concept | In this design | Simple meaning |
|---|---|---|
| `record` | `Expense`, `Split`, `Transfer`, `User` | immutable data |
| `List.copyOf` | the expense's splits | an unmodifiable copy, so nobody can change a recorded expense |
| integer division and `%` | splitting | `total × weight / sum` rounds down; `%` gives what was rounded away |
| `Integer[]` + comparator sort | the largest remainders | sort positions by remainder, largest first |
| `Collections.nCopies(n, 1L)` | equal split | a list of n ones: equal weights |
| `Map.merge(key, value, Long::sum)` | updating balances | "add to the existing value, or insert it" |
| `PriorityQueue` with comparators | greedy simplification | always get the biggest creditor and the biggest debtor |
| `AbstractMap.SimpleEntry` | a (user, amount) pair in a heap | a ready-made key-value pair |
| `TreeMap` | deterministic order | sorted keys, so results are the same every run |
| `ReentrantLock` per group | ledger + balances together | one lock per group |

---

## 5. The code, piece by piece

### 5.1 Splitting with exact paise

```java
static List<Split> proportional(long total, List<String> users, List<Long> weights) {
    long wSum = weights.stream().mapToLong(Long::longValue).sum();
    if (wSum <= 0) throw new IllegalArgumentException("weights must be positive");
    long[] parts = new long[users.size()];
    long[] remainders = new long[users.size()];
    long given = 0;
    for (int i = 0; i < users.size(); i++) {
        parts[i] = total * weights.get(i) / wSum;                     // floor
        remainders[i] = total * weights.get(i) % wSum;
        given += parts[i];
    }
    Integer[] order = new Integer[users.size()];
    for (int i = 0; i < order.length; i++) order[i] = i;
    Arrays.sort(order, (a, b) -> remainders[b] != remainders[a] ? Long.compare(remainders[b], remainders[a]) : Integer.compare(a, b));
    for (int k = 0; given < total; k++, given++) parts[order[k]]++;      // hand out leftover paise
    ...
}
```

| Step | Simple meaning |
|---|---|
| `total * w / wSum` | each person's share, rounded **down** |
| `total * w % wSum` | how much was rounded away (the "remainder") |
| sort by remainder | who lost the most to rounding |
| hand out leftovers | one paisa each to the top of that list until the total matches |

Ties go to the earlier person, so results are deterministic.

**Equal, percent and shares** all reuse `proportional` with different weights: equal = all 1s, percent = the percentages (which must add up to 100), shares = the share counts. **Exact** just checks that the amounts add up to the total.

### 5.2 A factory map for the strategies

```java
static final Map<SplitType, SplitStrategy> STRATEGIES = Map.of(
        SplitType.EQUAL, new EqualSplit(), SplitType.EXACT, new ExactSplit(),
        SplitType.PERCENT, new PercentSplit(), SplitType.SHARE, new ShareSplit());
```

A new split type = a new class + one map entry. The service never changes.

### 5.3 Recording an expense: ledger and balances together

```java
private void record(Group g, Expense e) {
    g.lock.lock();                                                  // ledger + balances change together
    try { apply(g, e); } finally { g.lock.unlock(); }
    listeners.forEach(l -> l.onExpense(e));                         // notify outside the lock
}

private void apply(Group g, Expense e) {
    g.ledger.add(e);
    g.net.merge(e.paidBy(), e.amountPaise(), Long::sum);           // payer is owed the full amount...
    for (Split s : e.splits()) g.net.merge(s.userId(), -s.amountPaise(), Long::sum);   // ...minus everyone's share
}
```

**Balance meaning:** **+** = others owe you, **−** = you owe others.

### 5.4 Settle-up is just another expense

```java
Expense settle(String groupId, String from, String to, long amountPaise) {
    Expense e = new Expense("E" + ..., groupId, "settle-up", from, amountPaise, List.of(new Split(to, amountPaise)), false);
    record(g, e);
    return e;
}
```

"B pays A ₹1,000" is recorded as an expense paid by B whose only participant is A. Same ledger, same invariant. No special code path.

### 5.5 Delete = append a reversal

```java
Expense delete(String groupId, String expenseId) {
    ...
    List<Split> neg = orig.splits().stream().map(s -> new Split(s.userId(), -s.amountPaise())).toList();
    Expense rev = new Expense("E" + ..., groupId, "reversal of " + expenseId, orig.paidBy(), -orig.amountPaise(), neg, true);
    apply(g, rev);
    return rev;
}
```

Like an accountant: you never erase a line in the books; you add a new line that cancels it. History stays complete (an **audit trail**). Deleting twice is refused. (Note: reversals are applied directly, so in this code the activity listener isn't notified about deletions.)

### 5.6 Greedy debt simplification

```java
public List<Transfer> simplify(Map<String, Long> net) {
    PriorityQueue<...> creditors = new PriorityQueue<>((a, b) -> Long.compare(b.getValue(), a.getValue()));   // biggest first
    PriorityQueue<...> debtors = new PriorityQueue<>((a, b) -> Long.compare(a.getValue(), b.getValue()));     // most negative first
    ...
    while (!creditors.isEmpty() && !debtors.isEmpty()) {
        var c = creditors.poll();
        var d = debtors.poll();
        long amt = Math.min(c.getValue(), -d.getValue());
        out.add(new Transfer(d.getKey(), c.getKey(), amt));
        if (c.getValue() - amt > 0) creditors.add(... c.getValue() - amt);
        if (d.getValue() + amt < 0) debtors.add(... d.getValue() + amt);
    }
    return out;
}
```

**In plain words:** the person who owes the most pays the person who is owed the most, as much as possible. Whoever reaches zero drops out. Repeat. Each step settles at least one person, so there are at most **n − 1 transfers**. Fast: O(n log n) with two heaps.

### 5.7 Optimal simplification: correct but exponential

```java
private static int dfs(long[] b, int i) {
    while (i < b.length && b[i] == 0) i++;
    if (i == b.length) return 0;
    int best = Integer.MAX_VALUE;
    for (int j = i + 1; j < b.length; j++) {
        if (b[j] == 0 || (b[i] > 0) == (b[j] > 0)) continue;            // need the opposite sign
        b[j] += b[i];                                                  // i settles entirely with j
        best = Math.min(best, 1 + dfs(b, i + 1));
        b[j] -= b[i];
        if (b[j] + b[i] == 0) break;                                    // perfect match: can't do better
    }
    return best;
}
```

Finding the **minimum** number of transfers is **NP-hard** (no known fast algorithm). This backtracking ([LeetCode 465](https://leetcode.com/problems/optimal-account-balancing/)) tries settling each balance against every opposite-sign balance. It's fine for about 15 people but explodes beyond that. That's **why greedy is the production choice**: it's fast and never needs more than n − 1 transfers.

---

## 6. Dry run (from the real output)

### Splits

| Split | Calculation | Result |
|---|---|---|
| ₹100 equal among 3 | 3333 each, 1 paisa left → A | ₹33.34, ₹33.33, ₹33.33 |
| ₹99.99 at 50 / 30 / 20 % | 4999.5, 2999.7, 1999.8 → floors 4999, 2999, 1999 (2 paise left) → to the largest remainders (.8, then .7) | 4999, 3000, 2000 |
| ₹1,000 by shares 2 : 1 : 1 | | ₹500, ₹250, ₹250 |
| exact 400 + 500 ≠ 1000 | | rejected |
| percents 40 + 50 ≠ 100 | | rejected |
| 2,000 random share splits | | every one sums exactly to its total |

### The Goa trip (in rupees)

| Expense | A | B | C | D |
|---|---|---|---|---|
| hotel ₹12,000 paid by A, equal ÷ 4 | +12,000 − 3,000 | −3,000 | −3,000 | −3,000 |
| dinner ₹4,000 paid by B, exact A 1,000 / B 2,000 / C 1,000 | −1,000 | +4,000 − 2,000 | −1,000 | |
| cab ₹1,000 paid by C, C 40% / D 60% | | | +1,000 − 400 | −600 |
| **net** | **+8,000** | **−1,000** | **−3,400** | **−3,600** |

The sum is 0 ✓. An outsider in a split is rejected.

### The settlement plan (greedy)

| Step | Biggest creditor | Biggest debtor | Transfer |
|---|---|---|---|
| 1 | A +8,000 | D −3,600 | D → A ₹3,600 |
| 2 | A +4,400 | C −3,400 | C → A ₹3,400 |
| 3 | A +1,000 | B −1,000 | B → A ₹1,000 |

After recording these settle-ups, everyone is even.

### Delete
D adds a wrong ₹500 expense split between A and D → A −250, D +250. Delete it → balances back to zero. Deleting again is refused. Replaying the ledger gives the same balances as the running totals.

### Greedy vs optimal
Balances A +5, B +4, C −4, D −3, E −2:
- **Greedy** pairs the biggest creditor A (5) with the biggest debtor C (−4), breaking the perfect pair B (4) / C (−4). It needs **4** transfers.
- **Optimal** finds **3**: C → B 4, D → A 3, E → A 2.

On 300 random groups: greedy always settles everyone, never uses more than n − 1 transfers, and the optimal count is never larger.

### Concurrency
16 threads add 4,000 expenses to one group → the sum is still exactly 0, and the running balances equal a fresh replay of the ledger.

---

## 7. Concurrency, explained simply

**The danger:** two threads add expenses to the same group at once. One adds to the ledger while the other updates the balances halfway; the balance map (a plain `HashMap`) could be corrupted, or an expense could be in the ledger but missing from the balances.

**The protection:** each group has its own lock. Appending to the ledger and updating the balances happen together inside it. Different groups never block each other. Listeners are called **after** the lock is released.

**The proof it works:** after 4,000 concurrent expenses, the sum is exactly 0 and the cached balances equal a full replay of the ledger.

---

## 8. Edge cases and common mistakes

**Edge cases handled:** amounts that don't divide evenly, exact amounts that don't add up, percents that don't total 100, zero or negative amounts, participants outside the group, deleting twice, deleting a non-existent expense.

**Common mistakes:**
1. `double` for money (₹33.333…).
2. Rounding each share and losing (or creating) a paisa.
3. Editing or deleting expenses in place (no audit trail; balances can drift).
4. Storing only balances, with no ledger to rebuild them from.
5. Claiming greedy is optimal (it isn't; optimal is NP-hard).
6. A separate code path for settle-ups that breaks the sum-to-zero invariant.

---

## 9. Interview questions and answers

**Q1. How do you split ₹100 between 3 people exactly?**
> Use integer paise. Give everyone the rounded-down share, then distribute the leftover paise to the people with the largest remainders: 3334, 3333, 3333.

**Q2. What invariant proves your balances are right?**
> In every group, the net balances sum to zero, because each expense's shares add up exactly to its amount. Balances can also be recomputed by replaying the append-only ledger.

**Q3. How do you simplify debts?**
> Greedy: repeatedly match the biggest creditor with the biggest debtor and transfer the smaller of the two amounts. At most n − 1 transfers, O(n log n).

**Q4. Is greedy optimal?**
> No. Finding the minimum number of transfers is NP-hard; the demo shows a case where greedy needs 4 and optimal needs 3. Backtracking finds the optimum for small groups, but greedy is the practical choice.

**Q5. How do you handle deleting or editing an expense?**
> Append a reversal (the negated expense) instead of changing history; an edit is a reversal plus a new expense. The audit trail stays complete.

**Q6. Why is settle-up just an expense?**
> "B pays A" has exactly the same effect on balances as an expense paid by B with A as the only participant. Reusing the same path keeps one invariant and one code path.

### Follow-up questions

**F1. "Multiple currencies."**
> Store each expense in its own currency; keep balances per currency, or convert at the expense's date with a stored rate. Never mix currencies in one total.

**F2. "Balances across all groups for one user."**
> Sum each group's net balance for that user (or maintain a per-user summary updated on every expense).

**F3. "Simplify debts across groups."**
> Combine the per-user net balances across the chosen groups and run the same greedy simplifier.

**F4. "Store it in a database."**
> An `expenses` table and an `expense_splits` table (append-only), plus a `balances` table updated in the same transaction. A periodic job can recompute balances from the ledger to detect drift.

**F5. "Recurring expenses (monthly rent)."**
> A scheduled job ([13 · Job Scheduler](../13-Job-Scheduler/JobSchedulerSystem.md)) adds the expense each month with an idempotency key (rent-2026-10), so a retried job doesn't add it twice.

---

## 10. Running the file

```bash
java SplitwiseSystem.java      # 23/23 tests passed (including 4,000 concurrent expenses with the sum still exactly 0)
```

**One-line summary:** *"Split strategies produce exact paise using the largest-remainder method so shares always add up to the amount; expenses go into an append-only ledger per group (deletes are reversals) with running balances that always sum to zero; and a greedy two-heap simplifier settles debts in at most n − 1 transfers because the optimal version is NP-hard."*
