# 11 · Vending Machine (explained simply)

| | |
|---|---|
| **Code** | [VendingMachineSystem.java](VendingMachineSystem.java) |
| **Run** | `java VendingMachineSystem.java` (26 self-checks, including 8 threads hammering one machine) |
| **Design notes** | [README.md](README.md) (state diagram, more Q&A) |
| **Prerequisite** | [How to think in LLD + the concurrency toolkit](../00-LLD-Playbook/ConcurrencyToolkit.md) |
| **The simpler enum version** | [OOP/06-Design-Patterns/P9_State.java](../../OOP/06-Design-Patterns/P9_State.java) |

This is **the** textbook question for the **State pattern**. Interviewers then push on the parts most candidates miss: making change from a **limited** coin box (where the obvious greedy method fails), refusing a sale when change can't be given, and keeping the sale all-or-nothing.

---

## 1. The problem in plain words

A machine has **slots** (A1 = chips ₹20, B1 = soda ₹35, …). A customer:
1. inserts coins or notes;
2. selects a slot;
3. gets the product **and change**, or an error ("insert ₹10 more", "sold out");
4. can **cancel** and get their own coins back.

The machine holds a **limited** box of coins for change. If it can't give the exact change, it must **refuse the sale and refund**, not keep the money. An operator can put it in **maintenance** to restock, load coins and collect cash.

---

## 2. How to think about it (the 7 steps)

### Step 1 · Clarify

| Question to ask | Answer we'll assume |
|---|---|
| Which denominations? | ₹1, 2, 5, 10, 20, 50, 100 |
| Is the change box limited? | **Yes**, which is what makes it interesting |
| Can't make change? | Refuse the sale and return the customer's money |
| One customer at a time? | Yes (it's one physical machine) |
| Maintenance mode? | Yes: restock, load coins, collect cash; customers refused |
| Card payments, touch screens? | Out of scope |

### Step 2 · Requirements
1. Events: insert, select, cancel, start/end maintenance.
2. The machine's response depends on its **current state**.
3. Correct change from a limited box, using the fewest coins.
4. The sale is all-or-nothing: either product + change + coins kept, or nothing changes and the money is returned.
5. Admin actions only in maintenance.
6. Money is conserved: everything inserted is either sold or returned.

### Step 3 · Entities

| Noun | Becomes |
|---|---|
| coin / note | `enum Coin(value)` |
| the coin box | `class CoinInventory` |
| making change | `interface ChangeMaker` + `GreedyChange`, `DpChange` |
| product | `record Product(name, price)` |
| slot | `class Slot(code, product, quantity)` |
| machine state | `interface VendingState` + 4 state classes |
| the machine | `class VendingMachine` (the "context") |

### Step 4 · Relationships

```
VendingMachine (context)
   ├── current VendingState  →  IdleState / HasMoneyState / DispensingState / MaintenanceState
   ├── slots (Map<code, Slot>)
   ├── CoinInventory box     (the machine's money)
   ├── credit                (the CURRENT customer's coins, kept separately)
   └── ChangeMaker           (DP)
```

**Keeping the customer's coins separate (`credit`) from the box** is what makes "cancel → give back their own coins" and "refuse → refund" simple.

### Step 5 · API

```java
void insert(Coin c)
void select(String slotCode)
void cancel()
void startMaintenance() / endMaintenance()
void restock(code, product, qty) / loadCoins(coin, n) / collectCash()   // maintenance only
```

### Step 6 · Patterns

| What changes / matters | Pattern |
|---|---|
| the behaviour depends on the current state | **State** |
| how change is computed | **Strategy** (`ChangeMaker`) |
| the denominations | an `enum` (a closed, validated set) |

### Step 7 · Concurrency
One physical machine serves one customer at a time, so every event method is `synchronized` on the machine. The test checks that money is conserved under 8 threads.

---

## 3. The State pattern, simply

### The problem with a big `switch`
Without the pattern, **every** method looks like this:

```
insert():  switch (state) { case IDLE: …; case HAS_MONEY: …; case DISPENSING: …; case MAINTENANCE: … }
select():  switch (state) { case IDLE: …; case HAS_MONEY: …; … }
cancel():  switch (state) { … }
```

Adding a new state (say, OUT_OF_ORDER) means editing **every** method, and the rules for one state are scattered across all of them.

### The State pattern
Make **one class per state**, each handling **every** event for that state. The machine just forwards each event to its current state object:

```java
synchronized void insert(Coin c) { state.insert(this, c); }
```

A new state = one new class. All the rules for a state live together.

| Event → | insert | select | cancel |
|---|---|---|---|
| **IDLE** | add credit → HAS_MONEY | "Insert money first" | "Nothing to refund" |
| **HAS_MONEY** | add more credit | try the sale (see below) | refund → IDLE |
| **DISPENSING** | "Busy", coin returned | "Busy" | "Busy" |
| **MAINTENANCE** | "Out of service", coin returned | "Out of service" | "Out of service" |

```
IDLE ──insert──> HAS_MONEY ──select OK──> DISPENSING ──> IDLE
  │                  │ └──cancel / can't make change──> IDLE (refund)
  └─startMaintenance─> MAINTENANCE ──endMaintenance──> IDLE
```

---

## 4. Change-making: why greedy fails

**Greedy** ("take the biggest coin that fits, repeat") works for real currencies **when you have unlimited coins**. With a **limited** box it can fail:

> The box has one ₹5 and three ₹2 coins. Change due: ₹6.
> Greedy takes the ₹5 → ₹1 left → no ₹1 coin → **fails**.
> But 2 + 2 + 2 = 6 works.

So the machine uses **dynamic programming**: a bounded coin-change DP that finds the fewest coins whenever **any** combination exists. It's the same idea as [DSA Coin Change](../../DSA/15-Dynamic-Programming/03-Medium/M09_CoinChange.md), but each physical coin can be used **at most once** (0/1 knapsack, iterating amounts downward, as in [Partition Equal Subset Sum](../../DSA/15-Dynamic-Programming/03-Medium/M11_PartitionEqualSubsetSum.md)).

---

## 5. Java concepts used in this design

| Concept | In this design | Simple meaning |
|---|---|---|
| `enum` with a value | `Coin(int value)` | only valid denominations exist |
| `EnumMap<Coin, Integer>` | coin counts | a compact map keyed by an enum, iterated in declaration order |
| `interface` with `default` methods | `VendingState.startMaintenance` | most states share the same answer; only Idle and Maintenance override it |
| one object per state, shared | `idle`, `hasMoney`, … | the state classes hold no data, so one instance of each is enough |
| `synchronized` methods | every event | one customer action at a time |
| `Optional<Map<Coin, Integer>>` | the change result | "maybe a way to make change" |
| `int[]` DP with `clone()` snapshots | `DpChange` | rows kept so the chosen coins can be reconstructed |
| `Integer.MAX_VALUE / 2` | DP "infinity" | big, but adding 1 can't overflow |

---

## 6. The code, piece by piece

### 6.1 The state interface

```java
interface VendingState {
    void insert(VendingMachine m, Coin c);
    void select(VendingMachine m, String slotCode);
    void cancel(VendingMachine m);
    default void startMaintenance(VendingMachine m) { m.reject("Finish the current transaction first"); }
    default void endMaintenance(VendingMachine m) { m.reject("Not in maintenance"); }
    String name();
}
```

Each method receives the machine so the state can change it (add credit, switch state, refund).

### 6.2 Idle

```java
static class IdleState implements VendingState {
    public void insert(VendingMachine m, Coin c) { m.credit(c); m.setState(m.hasMoney); }
    public void select(VendingMachine m, String code) { m.reject("Insert money first"); }
    public void cancel(VendingMachine m) { m.reject("Nothing to refund"); }
    public void startMaintenance(VendingMachine m) { m.setState(m.maintenance); }
    public String name() { return "IDLE"; }
}
```

### 6.3 Has money: where the sale happens

```java
public void select(VendingMachine m, String code) {
    Slot s = m.slots.get(code);
    if (s == null) { m.reject("No slot " + code); return; }
    if (s.quantity == 0) { m.reject(s.product.name() + " is sold out"); return; }
    if (m.balance() < s.product.price()) { m.reject("Insert Rs " + (s.product.price() - m.balance()) + " more"); return; }
    // Can we give change? Check BEFORE taking anything: the sale is all-or-nothing.
    int changeDue = m.balance() - s.product.price();
    Map<Coin, Integer> boxAfterDeposit = m.coinBoxWithCredit();
    Optional<Map<Coin, Integer>> change = m.changeMaker.makeChange(changeDue, boxAfterDeposit);
    if (change.isEmpty()) { m.reject("Exact change only - can't return Rs " + changeDue + "; refunding"); m.refundAll(); m.setState(m.idle); return; }
    m.setState(m.dispensing);
    m.completeSale(s, change.get());                                         // commits coins, product, change
    m.setState(m.idle);
}
```

In plain words:
1. Check the slot exists, isn't empty, and the customer has paid enough. If any check fails, show a message and **stay** in HAS_MONEY (they can add more coins or pick another slot).
2. **Before changing anything**, work out the change. The customer's own coins count as available change (`coinBoxWithCredit`): if you put in ₹50 for a ₹35 soda, your ₹50 note goes into the box.
3. Can't make change? Refuse, refund everything, go back to IDLE.
4. Otherwise: DISPENSING → commit the sale → IDLE.

### 6.4 The commit: all effects in one place

```java
void completeSale(Slot s, Map<Coin, Integer> change) {
    box.addAll(credit);
    credit.clear();
    box.remove(change);
    change.forEach((c, n) -> { for (int i = 0; i < n; i++) returnTray.add(c); });
    s.quantity--;
    salesTotal += s.product.price();
    dispensed.add(s.product.name());
}
```

Every effect of a sale happens here, **after** all checks passed: the customer's coins move into the box, the change comes out, the stock goes down, and the product drops. There's no half-sale.

### 6.5 Dispensing and maintenance

```java
static class DispensingState implements VendingState {
    public void insert(VendingMachine m, Coin c) { m.reject("Busy"); m.returnCoin(c); }
    ...
}

static class MaintenanceState implements VendingState {
    public void insert(VendingMachine m, Coin c) { m.reject("Out of service"); m.returnCoin(c); }
    public void endMaintenance(VendingMachine m) { m.setState(m.idle); }
    ...
}
```

A coin inserted during maintenance (or while the motor is running) is **returned** at once.

### 6.6 Greedy change (kept to show its failure)

```java
public Optional<Map<Coin, Integer>> makeChange(int amount, Map<Coin, Integer> available) {
    Map<Coin, Integer> out = new EnumMap<>(Coin.class);
    Coin[] cs = Coin.values();
    for (int i = cs.length - 1; i >= 0 && amount > 0; i--) {
        int use = Math.min(available.getOrDefault(cs[i], 0), amount / cs[i].value);
        if (use > 0) { out.put(cs[i], use); amount -= use * cs[i].value; }
    }
    return amount == 0 ? Optional.of(out) : Optional.empty();
}
```

### 6.7 DP change (the one the machine uses)

```java
public Optional<Map<Coin, Integer>> makeChange(int amount, Map<Coin, Integer> available) {
    int INF = Integer.MAX_VALUE / 2;
    int[] dp = new int[amount + 1];
    Arrays.fill(dp, INF);
    dp[0] = 0;
    List<Coin> units = new ArrayList<>();                       // each physical coin = one 0/1 item
    for (Coin c : Coin.values()) for (int k = 0; k < Math.min(available.getOrDefault(c, 0), amount / c.value); k++) units.add(c);
    int[][] choice = new int[units.size() + 1][];               // for reconstruction: dp row snapshots
    choice[0] = dp.clone();
    for (int i = 0; i < units.size(); i++) {
        int v = units.get(i).value;
        for (int a = amount; a >= v; a--) if (dp[a - v] + 1 < dp[a]) dp[a] = dp[a - v] + 1;   // 0/1 knapsack, backwards
        choice[i + 1] = dp.clone();
    }
    if (dp[amount] >= INF) return Optional.empty();
    Map<Coin, Integer> out = new EnumMap<>(Coin.class);
    for (int i = units.size(), a = amount; i > 0 && a > 0; i--)
        if (choice[i][a] != choice[i - 1][a]) { Coin c = units.get(i - 1); out.merge(c, 1, Integer::sum); a -= c.value; }   // coin i was used
    return Optional.of(out);
}
```

| Step | Simple meaning |
|---|---|
| `units` | list every **physical** coin (no more of each type than could be useful) |
| `dp[a]` | the fewest coins to make amount `a` using the coins seen so far |
| the backwards loop | each coin is used at most once (0/1 knapsack) |
| `choice` snapshots | the DP row after each coin, kept for reconstruction |
| reconstruction | walk back: if the row changed at `a` when coin i was added, coin i was used |

---

## 7. Dry run (from the real output)

The stocked machine: A1 chips ₹20 × 2, B1 soda ₹35 × 5, C1 candy ₹7 × 5; the box holds 5 × ₹10, 5 × ₹5 and 5 × ₹1.

| Step | State | What happens |
|---|---|---|
| select A1 | IDLE | "Insert money first" |
| insert ₹50 | IDLE → HAS_MONEY | credit ₹50 |
| select B1 (₹35) | HAS_MONEY → DISPENSING → IDLE | soda + ₹15 change (₹10 + ₹5) |
| insert ₹10, select A1 | HAS_MONEY | "Insert Rs 10 more" (stays HAS_MONEY) |
| insert ₹10, select A1 | → IDLE | chips (exact) |
| insert ₹20, select A1 | → IDLE | the 2nd (last) chips |
| insert ₹20, select A1 | HAS_MONEY | "Chips is sold out" |
| cancel | → IDLE | the customer's own ₹20 back |

**Greedy vs DP** (box: one ₹5, three ₹2; change ₹6): greedy fails; DP gives `{TWO=3}`. On 500 random boxes, DP succeeded whenever greedy did, and its change was always exact and within the box.

**Exact change only:** a machine with an **empty** coin box and candy at ₹7. Insert ₹10 → "Exact change only - can't return Rs 3; refunding": ₹10 back, no candy, stock unchanged. Insert ₹5 + ₹2 → candy (no change needed).

**Maintenance:** restocking outside maintenance throws. In maintenance, an inserted ₹10 is returned at once. The operator restocks and collects ₹155. Starting maintenance while a customer has money in → "Finish the current transaction first".

**Concurrency:** 8 threads × 2,000 random insert/select/cancel calls. **Inserted = sales + returned** (₹162,140), sales × ₹35 = the sales total (₹2,660), and the box grew by exactly the sales.

---

## 8. Concurrency, explained simply

A vending machine is one physical device, so it handles one action at a time: every public method is `synchronized`. The state objects themselves hold no data, so they're safe to share. The real test is **conservation of money**: whatever order the 8 threads' actions happen in, every rupee inserted is either kept for a sale or returned, never lost or created.

---

## 9. Edge cases and common mistakes

**Edge cases handled:** selecting before paying, not enough money (stay and ask for more), a sold-out slot, an unknown slot, no way to make change (refund), coins during maintenance (returned), admin actions outside maintenance, entering maintenance mid-transaction.

**Common mistakes:**
1. A `switch (state)` in every method.
2. Greedy change with a limited box.
3. Taking the money or the product **before** checking change can be given (a half-sale).
4. Mixing the customer's coins with the box (a refund might return different coins, or the wrong amount).
5. Leaving HAS_MONEY after a "not enough money" error (the customer's credit gets lost).
6. Admin operations allowed while customers are using the machine.

---

## 10. Interview questions and answers

**Q1. Why the State pattern instead of an enum + switch?**
> Each state's rules live in one class, and adding a state means adding one class instead of editing every method. The context just delegates events to the current state.

**Q2. Why can greedy change fail, and what do you use instead?**
> With a limited coin box, taking the biggest coin first can leave an amount that can't be made (₹6 from {5, 2, 2, 2}). A bounded coin-change DP finds the fewest coins whenever any combination exists.

**Q3. What if the machine can't make change?**
> Check before committing anything; if it's impossible, refuse the sale and return the customer's own coins. Nothing about the box or the stock has changed.

**Q4. How do you make the sale atomic?**
> All checks first (slot, stock, payment, change); then one `completeSale` method applies every effect. The machine-level lock means nothing else happens in between.

**Q5. Why keep the customer's credit separate from the coin box?**
> So cancelling returns exactly their coins, and their coins can still be counted as available change during the check without committing them.

**Q6. Why is DISPENSING a separate state?**
> While the motor runs, every other event must be refused. Modelling it as a state makes that explicit (and handles a jam or timeout as a future transition).

### Follow-up questions

**F1. "Add card and UPI payments."**
> A `PaymentMethod` strategy: coins add credit; a card authorises the exact price (no change needed). The states stay the same.

**F2. "The product jams."**
> From DISPENSING, a sensor failure moves to an OUT_OF_ORDER state, refunds the customer, and alerts the operator.

**F3. "Report low stock and low change."**
> An observer notified after each sale; it alerts when a slot or a denomination falls below a threshold.

**F4. "A fleet of machines."**
> Each machine reports sales and stock to a backend (events); restocking routes are planned from those reports.

**F5. "Give change using the most common coins first, to save rare ones."**
> A different `ChangeMaker` (a DP that weights coins by scarcity instead of counting them).

---

## 11. Running the file

```bash
java VendingMachineSystem.java      # 26/26 tests passed (including money conservation under 8 threads)
```

**One-line summary:** *"Use the State pattern (Idle, HasMoney, Dispensing, Maintenance each handle every event); keep the customer's coins separate from the box; compute change with a bounded coin-change DP because greedy fails on a limited box; check everything first and commit the sale in one method, refunding when change is impossible."*
