# H11 · Best Time to Buy and Sell Stock IV ⭐

| | |
|---|---|
| **LeetCode** | [#188 Best Time to Buy and Sell Stock IV](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-iv/) |
| **Difficulty** | Hard |
| **Pattern** | State machine with a **transaction counter**: buy[j] and sell[j] for j = 1..k |
| **Code** | [H11_StockIV.java](H11_StockIV.java) |
| **Run** | `java H11_StockIV.java` |

⭐ = not in `leetcode.md`, added because it generalises the whole stock family. The state-machine approach is explained in [M25](../03-Medium/M25_StockWithCooldown.md).

---

## 1. The problem

At most k transactions (a buy followed by a sell), holding at most one share at a time. What's the maximum profit?

| k | prices | answer |
|---|---|---|
| 2 | [2, 4, 1] | 2 |
| 2 | [3, 2, 6, 5, 0, 3] | 7 (2 → 6, 0 → 3) |
| 1 | [7, 1, 5, 3, 6, 4] | 5 (Stock I) |
| 100 | [7, 1, 5, 3, 6, 4] | 7 (effectively unlimited) |
| 0 | [1, 5] | 0 |

### The whole family

| problem | k | extra rule |
|---|---|---|
| Stock I (#121) | 1 | |
| Stock II (#122) | ∞ | sum every rise |
| Stock III (#123) | 2 | |
| Stock IV (#188) | any k | |
| [Cooldown (M25)](../03-Medium/M25_StockWithCooldown.md) | ∞ | wait a day after selling |
| [Fee (M26)](../03-Medium/M26_StockWithTransactionFee.md) | ∞ | pay per transaction |

---

## 2. How to think about it

### States per day, for j = 1..k

| state | meaning | transition (today's price p) |
|---|---|---|
| buy[j] | the best profit while **holding**, having started the j-th transaction | max(buy[j], sell[j − 1] − p): keep holding, or open transaction j |
| sell[j] | the best profit **not** holding, having completed j transactions | max(sell[j], buy[j] + p): keep, or close transaction j |

- **Base:** sell[0] = 0 forever, buy[j] = −∞, sell[j] = 0.
- **Answer:** sell[k].

### Shortcut
If k ≥ n / 2, the limit can never bind (each transaction needs at least 2 days), so use the Stock II greedy: sum every rise. That also avoids allocating a huge k-sized array.

### Dry run: k = 2, [3, 2, 6, 5, 0, 3] (from a real run)

| price | buy1 | sell1 | buy2 | sell2 |
|---|---|---|---|---|
| 3 | −3 | 0 | −3 | 0 |
| 2 | −2 | 0 | −2 | 0 |
| 6 | −2 | 4 | −2 | 4 |
| 5 | −2 | 4 | −1 | 4 |
| 0 | 0 | 4 | 4 | 4 |
| 3 | 0 | 4 | 4 | **7** |

On the day with price 0, buy2 = sell1 − 0 = 4: "finished the first trade with +4, now holding a second share bought at 0". Selling it at 3 gives 7.

---

## 3. Java concepts used in this problem

### 3.1 Arrays of states indexed by the transaction count

```java
int[] buy = new int[k + 1], sell = new int[k + 1];
Arrays.fill(buy, Integer.MIN_VALUE / 2);
```

Index 0 of `sell` stays 0 (no transactions yet). `buy[0]` is never used. `MIN_VALUE / 2` is the safe −∞ from [M25](../03-Medium/M25_StockWithCooldown.md#31-a-safe-minus-infinity).

### 3.2 Same-day updates are harmless here

```java
buy[j] = Math.max(buy[j], sell[j - 1] - p);
sell[j] = Math.max(sell[j], buy[j] + p);
```

`sell[j]` reads the **new** `buy[j]`, which might mean "buy and sell on the same day". That gains 0, so it never improves the answer and never breaks it, so no temporaries are needed.

### 3.3 `Math.max(0, prices[i] − prices[i − 1])`

The Stock II greedy: add every positive day-to-day change.

---

## 4. Approach 1: Recursion over (day, holding, transactions left) (the referee)

### Code ([H11_StockIV.java:59-65](H11_StockIV.java#L59-L65))

```java
static int brute(int[] p, int day, boolean holding, int left) {
    if (day == p.length) return 0;
    int skip = brute(p, day + 1, holding, left);
    if (holding) return Math.max(skip, p[day] + brute(p, day + 1, false, left));
    if (left == 0) return skip;
    return Math.max(skip, -p[day] + brute(p, day + 1, true, left - 1));      // buying opens a transaction
}
```

A transaction is counted when it's **opened** (at the buy). **Exponential** without memo.

---

## 5. Approach 2: State machine with a counter (the file's main solution)

### Code ([H11_StockIV.java:42-57](H11_StockIV.java#L42-L57))

```java
static int maxProfit(int k, int[] prices) {
    int n = prices.length;
    if (k >= n / 2) {                                 // unlimited in practice: take every rise
        int s = 0;
        for (int i = 1; i < n; i++) s += Math.max(0, prices[i] - prices[i - 1]);
        return s;
    }
    int[] buy = new int[k + 1], sell = new int[k + 1];
    Arrays.fill(buy, Integer.MIN_VALUE / 2);
    for (int p : prices)
        for (int j = 1; j <= k; j++) {
            buy[j] = Math.max(buy[j], sell[j - 1] - p);
            sell[j] = Math.max(sell[j], buy[j] + p);
        }
    return sell[k];
}
```

### Line by line

| Line | What it does |
|---|---|
| the shortcut | k is effectively unlimited |
| the arrays | one buy/sell pair per transaction number |
| `buy[j]` | open transaction j using the profit after j − 1 completed ones |
| `sell[j]` | close transaction j |
| `sell[k]` | at most k completed transactions (sell[k] ≥ sell[j] for j < k, because unused transactions can be "empty") |

### Complexity
- **Time O(n · k), space O(k).** (O(n) with the shortcut.)

---

## 6. Comparing the approaches

| | Recursion | State machine |
|---|---|---|
| Time | exponential | **O(n · k)** |
| Space | O(n) stack | O(k) |

The file checks the state machine against the recursion on 1,000 random cases with k from 0 to 4.

---

## 7. Edge cases and common mistakes

**Edge cases:** k = 0 (0), k ≥ n / 2 (unlimited), falling prices (0), a single day.

**Mistakes:**
1. Allocating `new int[k + 1]` for k = 10⁹ (use the shortcut first).
2. Counting a transaction at both the buy and the sell.
3. Starting buy[j] at 0.

---

## 8. Interview questions and answers

**Q1. Why is sell[k] the answer and not the max over all j?**
> Allowing more transactions never hurts: any plan with fewer than k can be seen as a plan with k where some transactions are skipped. So sell[k] already includes those.

**Q2. Why the k ≥ n/2 shortcut?**
> Each transaction needs a buy day and a later sell day, so at most n/2 transactions can ever happen. With that many allowed, the limit doesn't matter and the answer is the sum of every rise.

### Follow-up questions

**F1. Stock III (k = 2) with four variables.**
> buy1, sell1, buy2, sell2: exactly this DP unrolled.

**F2. Add a fee or a cooldown to Stock IV.**
> Subtract the fee in the sell transition, or buy from the sell state of two days earlier.

**F3. Return the trades.**
> Keep the full (day, j, holding) table and walk back through the choices.

---

## 9. Running the file

```bash
java H11_StockIV.java      # 6/6 tests passed (including 1000 random vs recursion)
```

**One-line summary:** *"For each transaction number j keep buy[j] = max(buy[j], sell[j−1] − p) and sell[j] = max(sell[j], buy[j] + p); the answer is sell[k], with the 'sum every rise' shortcut when k ≥ n/2."*
