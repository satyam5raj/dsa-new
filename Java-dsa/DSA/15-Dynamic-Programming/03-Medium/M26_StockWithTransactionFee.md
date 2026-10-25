# M26 · Best Time to Buy and Sell Stock with Transaction Fee

| | |
|---|---|
| **LeetCode** | [#714 Best Time to Buy and Sell Stock with Transaction Fee](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-with-transaction-fee/) |
| **Difficulty** | Medium |
| **Pattern** | **State-machine DP** (2 states: holding / free) |
| **Code** | [M26_StockWithTransactionFee.java](M26_StockWithTransactionFee.java) |
| **Run** | `java M26_StockWithTransactionFee.java` |

The state-machine approach is explained in [M25](M25_StockWithCooldown.md).

---

## 1. The problem

Unlimited transactions, one share at a time, and each completed transaction costs `fee`. What's the maximum profit?

| prices | fee | answer | trades |
|---|---|---|---|
| [1, 3, 2, 8, 4, 9] | 2 | 8 | buy 1 sell 8 (+5), buy 4 sell 9 (+3) |
| [1, 3, 7, 5, 10, 3] | 3 | 6 | |
| [9, 8, 7] | 1 | 0 | |

---

## 2. How to think about it

### "Sum every rise" fails
That's the greedy for unlimited transactions without fees. With a fee, many small trades lose to one long trade: on the first example, trading every rise nets (3 − 1 − 2) + (8 − 2 − 2) + (9 − 4 − 2) = 0 + 4 + 3 = 7, but the best is 8. Merging 1 → 3 and 2 → 8 into one trade 1 → 8 pays one fee instead of two.

### Two states (at the end of each day)

| state | transition |
|---|---|
| hold = the best profit while holding a share | max(hold, free − p): keep, or buy today |
| free = the best profit with no share | max(free, hold + p − fee): keep waiting, or sell today and pay the fee |

- **Base:** hold = −∞, free = 0. **Answer:** free.
- The fee can be charged on the sell or on the buy; either works, as long as it's once per round trip.

### Order of updates
The code updates `free` first and then uses the **new** `free` to compute `hold`. That would mean "sell and re-buy on the same day", which only loses a fee, so it never helps and never changes the result. No temporaries are needed here (unlike [M25](M25_StockWithCooldown.md)).

### Dry run: [1, 3, 2, 8, 4, 9], fee 2 (from a real run)

| price | free = max(free, hold + p − 2) | hold = max(hold, free − p) |
|---|---|---|
| 1 | 0 | −1 |
| 3 | max(0, −1 + 3 − 2) = 0 | −1 |
| 2 | 0 | −1 |
| 8 | max(0, −1 + 8 − 2) = 5 | max(−1, 5 − 8) = −1 |
| 4 | 5 | max(−1, 5 − 4) = 1 |
| 9 | max(5, 1 + 9 − 2) = **8** | 1 |

---

## 3. Java concepts used in this problem

### 3.1 `Integer.MIN_VALUE / 2` as −∞

As in [M25](M25_StockWithCooldown.md#31-a-safe-minus-infinity): `hold + p − fee` mustn't overflow.

### 3.2 A deliberately simple update order

```java
free = Math.max(free, hold + p - fee);         // sell
hold = Math.max(hold, free - p);               // buy (same-day re-buy can't help)
```

Reusing the new `free` is safe because selling and buying back at the same price only costs a fee.

---

## 4. Approach 1: Trade every rise (wrong with a fee)

### Code ([M26_StockWithTransactionFee.java:48-52](M26_StockWithTransactionFee.java#L48-L52))

```java
static int greedyNoFeeAware(int[] p, int fee) {     // Stock II's "take every rise", paying the fee each time
    int s = 0;
    for (int i = 1; i < p.length; i++) if (p[i] > p[i - 1]) s += p[i] - p[i - 1] - fee;
    return s;
}
```

It returns 7 for the first example. The test keeps it as a counterexample.

---

## 5. Approach 2: Recursion (the referee)

### Code ([M26_StockWithTransactionFee.java:54-59](M26_StockWithTransactionFee.java#L54-L59))

```java
static int brute(int[] p, int fee, int day, boolean holding) {
    if (day == p.length) return 0;
    int skip = brute(p, fee, day + 1, holding);
    int act = holding ? p[day] - fee + brute(p, fee, day + 1, false) : -p[day] + brute(p, fee, day + 1, true);
    return Math.max(skip, act);
}
```

Each day: do nothing, or act (sell if holding, buy if not). **Exponential** without memo.

---

## 6. Approach 3: Two-state machine (the file's main solution)

### Code ([M26_StockWithTransactionFee.java:39-46](M26_StockWithTransactionFee.java#L39-L46))

```java
static int maxProfit(int[] prices, int fee) {
    int hold = Integer.MIN_VALUE / 2, free = 0;
    for (int p : prices) {
        free = Math.max(free, hold + p - fee);         // sell
        hold = Math.max(hold, free - p);               // buy (same-day re-buy can't help)
    }
    return free;
}
```

- **Time O(n), space O(1).**

---

## 7. Comparing the approaches

| | Every rise | Recursion | State machine |
|---|---|---|---|
| Correct with a fee | **no** | yes | yes |
| Time | O(n) | exponential | **O(n)** |

The file checks the state machine against the recursion on 1,000 random cases with fees 0–4.

---

## 8. Edge cases and common mistakes

**Edge cases:** fee 0 (it becomes "sum every rise"), a fee larger than any rise (0), falling prices.

**Mistakes:**
1. Charging the fee on both the buy and the sell.
2. Greedy per rise.
3. hold starting at 0.

---

## 9. Interview questions and answers

**Q1. Why can a single long trade beat several short ones?**
> Each trade pays the fee once. If the dips between rises are smaller than the fee, staying in the position avoids paying the fee again.

**Q2. Why is the same-day update order safe here but not in the cooldown problem?**
> Here, selling and re-buying the same day just loses a fee, so it never wins. With a cooldown, using today's sold value to buy would break the rule, so temporaries are required.

### Follow-up questions

**F1. With a cooldown.** → [M25](M25_StockWithCooldown.md)

**F2. At most k transactions.** → [H11](../04-Hard/H11_StockIV.md)

**F3. Fee 0.**
> Exactly LC 122: the answer is the sum of every positive daily change.

---

## 10. Running the file

```bash
java M26_StockWithTransactionFee.java      # 5/5 tests passed (including 1000 random vs recursion)
```

**One-line summary:** *"Two states per day: free = max(free, hold + p − fee) and hold = max(hold, free − p), starting from hold = −∞ and free = 0; the answer is free."*
