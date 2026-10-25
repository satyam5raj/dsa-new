# E05 · Best Time to Buy and Sell Stock

| | |
|---|---|
| **LeetCode** | [#121 Best Time to Buy and Sell Stock](https://leetcode.com/problems/best-time-to-buy-and-sell-stock/) |
| **Difficulty** | Easy |
| **Pattern** | Track the minimum so far (one-pass greedy) |
| **Code** | [E05_BestTimeToBuyAndSellStock.java](E05_BestTimeToBuyAndSellStock.java) |
| **Run** | `java E05_BestTimeToBuyAndSellStock.java` |

---

## 1. The problem

`prices[i]` is the stock price on day `i`. You may **buy once** and **sell once, on a later day**. Return the maximum profit, or `0` if no profit is possible.

| prices | answer | why |
|---|---|---|
| `[7, 1, 5, 3, 6, 4]` | 5 | buy at 1 (day 1), sell at 6 (day 4) |
| `[7, 6, 4, 3, 1]` | 0 | prices only fall, so don't trade |
| `[2, 4, 1]` | 2 | buy 2, sell 4. The minimum (1) comes **after** the maximum, so it's useless |
| `[1, 2, 3, 4, 5]` | 4 | buy day 0, sell day 4 |
| `[5]` | 0 | one day, nothing to sell later |

**Constraints:** `1 ≤ n ≤ 10⁵`, `0 ≤ prices[i] ≤ 10⁴`.

---

## 2. How to think about it

- **Brute force:** try every pair `(buy day i, sell day j)` with `i < j`. O(n²), which is too slow for n = 10⁵.
- **Change the question:** instead of "which pair?", ask *"if I **sell today**, what's my best profit?"* The answer is **today's price − the cheapest price before today**.
- So walk left to right, remembering:
  - `minPrice`: the cheapest price seen so far (the best buy day up to now)
  - `maxProfit`: the best `price − minPrice` seen so far
- **Why not just `max − min`?** Order matters. In `[5, 1]` max − min = 4, but you can't buy at 1 and sell at 5 because 5 came first. The answer is 0.

This "**keep the best thing seen so far**" idea comes up again and again (Kadane's algorithm in M01 is its big brother).

---

## 3. Java concepts used in this problem

### 3.1 `Integer.MAX_VALUE` and `Integer.MIN_VALUE`

```java
Integer.MAX_VALUE   //  2147483647  (2³¹ − 1), the biggest int
Integer.MIN_VALUE   // -2147483648  (−2³¹),    the smallest int
```

They're perfect **starting values**:
- To find a **minimum**, start at `MAX_VALUE`, because any real value is smaller and replaces it.
- To find a **maximum**, start at `MIN_VALUE`.

> **Overflow trap:** `Integer.MAX_VALUE + 1` wraps around to `Integer.MIN_VALUE`. And `price − Integer.MIN_VALUE` would overflow too. In this code `minPrice` gets replaced by a real price on the very first day, before any subtraction, so we're safe.

Also: `Long.MAX_VALUE`, `Double.MAX_VALUE`, etc. (`Double.MIN_VALUE` is the smallest **positive** double, not the most negative. That's a classic gotcha.)

### 3.2 `Math.min` and `Math.max`

```java
Math.min(3, 7)   // 3
Math.max(3, 7)   // 7
```

These are static methods of the `Math` class (always available; no import needed, since it's in `java.lang`). `minPrice = Math.min(minPrice, price);` is a short way to write `if (price < minPrice) minPrice = price;`.

Other useful `Math` methods: `Math.abs(x)`, `Math.pow(a, b)` (returns `double`), `Math.sqrt(x)`, `Math.floorMod(a, b)` (a modulo that's never negative), `Math.addExact(a, b)` (throws on overflow).

### 3.3 Greedy

A **greedy** algorithm makes the locally best choice at each step (here: "keep the cheapest buy price") and never looks back. It works here because the best sell-today profit depends **only** on the minimum before today, and keeping that one number throws away no useful information.

---

## 4. Approach 1: Brute force (every buy/sell pair)

```java
static int maxProfitBrute(int[] prices) {
    int best = 0;
    for (int buy = 0; buy < prices.length; buy++) {
        for (int sell = buy + 1; sell < prices.length; sell++) {
            best = Math.max(best, prices[sell] - prices[buy]);
        }
    }
    return best;
}
```

| Line | What it does |
|---|---|
| `int best = 0;` | starting at 0 means "don't trade" is the default, so the answer is never negative |
| `buy` loop | every possible buy day |
| `sell = buy + 1` | sell strictly **after** buying |
| `best = Math.max(best, prices[sell] - prices[buy]);` | profit for this pair; keep the best |

**Dry run** `[2, 4, 1]`:

| buy | sell | profit | best |
|---|---|---|---|
| 0 (2) | 1 (4) | 2 | 2 |
| 0 (2) | 2 (1) | −1 | 2 |
| 1 (4) | 2 (1) | −3 | 2 |
| | | | **return 2** |

**Time O(n²)**: about 5 × 10⁹ pairs for n = 10⁵, so it **times out**. **Space O(1).**

---

## 5. Approach 2: One pass with the minimum so far (optimal)

### Code ([E05_BestTimeToBuyAndSellStock.java:43-51](E05_BestTimeToBuyAndSellStock.java#L43-L51))

```java
static int maxProfit(int[] prices) {
    int minPrice = Integer.MAX_VALUE;
    int maxProfit = 0;
    for (int price : prices) {
        minPrice = Math.min(minPrice, price);               // best day to have bought, so far
        maxProfit = Math.max(maxProfit, price - minPrice);  // sell today?
    }
    return maxProfit;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int minPrice = Integer.MAX_VALUE;` | "no buy price yet". The first real price will replace it |
| `int maxProfit = 0;` | no trade = profit 0 |
| `for (int price : prices)` | walk the days in order. We don't need the day numbers, only the prices |
| `minPrice = Math.min(minPrice, price);` | is today the cheapest day so far? If yes, it becomes the buy candidate |
| `maxProfit = Math.max(maxProfit, price - minPrice);` | profit if I sold **today**, having bought at the cheapest earlier day; keep the best |
| `return maxProfit;` | |

**"Isn't updating `minPrice` first the same as buying and selling on the same day?"** Yes, and that's fine: if today is the new minimum, then `price − minPrice = 0`, which can never beat `maxProfit` (it's ≥ 0). Same-day trades are harmless. The order of the two lines doesn't change the answer.

### Dry run 1: `[7, 1, 5, 3, 6, 4]`

| day | price | minPrice after update | price − minPrice | maxProfit |
|---|---|---|---|---|
| 0 | 7 | 7 | 0 | 0 |
| 1 | 1 | **1** (new cheapest) | 0 | 0 |
| 2 | 5 | 1 | 4 | 4 |
| 3 | 3 | 1 | 2 | 4 |
| 4 | 6 | 1 | **5** | **5** |
| 5 | 4 | 1 | 3 | 5 |
| | | | | **return 5** |

### Dry run 2: `[2, 4, 1]` (the max comes before the min)

| day | price | minPrice | price − minPrice | maxProfit |
|---|---|---|---|---|
| 0 | 2 | 2 | 0 | 0 |
| 1 | 4 | 2 | 2 | 2 |
| 2 | 1 | 1 | 0 | 2 |
| | | | | **return 2** |

The new minimum 1 on the last day can't help, because there's no later day to sell on. The algorithm handles this automatically.

### Dry run 3: `[7, 6, 4, 3, 1]` (falling)

| price | minPrice | price − minPrice | maxProfit |
|---|---|---|---|
| 7 | 7 | 0 | 0 |
| 6 | 6 | 0 | 0 |
| 4 | 4 | 0 | 0 |
| 3 | 3 | 0 | 0 |
| 1 | 1 | 0 | 0 |

Every day is a new minimum, so profit stays 0 → **return 0**.

### Complexity
- **Time O(n):** one pass.
- **Space O(1):** two variables.

---

## 6. (Bonus) The same problem as "maximum subarray" (Kadane view)

Turn prices into daily **changes**: `diff[i] = prices[i] − prices[i−1]`. Buying on day `b` and selling on day `s` earns `diff[b+1] + ... + diff[s]`, which is a **subarray sum**. So max profit = maximum subarray sum of `diff` (or 0). That's exactly Kadane's algorithm ([M01 Maximum Subarray](../03-Medium/M01_MaximumSubarray.md)):

```java
static int maxProfitKadane(int[] prices) {
    int cur = 0, best = 0;
    for (int i = 1; i < prices.length; i++) {
        cur = Math.max(0, cur + prices[i] - prices[i - 1]);  // extend, or restart at 0
        best = Math.max(best, cur);
    }
    return best;
}
```

Seeing that one problem is "the same as" another is a strong interview signal.

---

## 7. Comparing the approaches

| | Brute force | Min so far | Kadane on differences |
|---|---|---|---|
| Time | O(n²) | **O(n)** | O(n) |
| Space | O(1) | O(1) | O(1) |

---

## 8. Edge cases and common mistakes

**Edge cases:** one day, strictly falling prices, all prices equal, the maximum before the minimum, prices equal to 0.

**Mistakes:**
1. Returning `max(prices) − min(prices)`, which ignores order.
2. Starting `maxProfit` at `Integer.MIN_VALUE`, which can return a negative profit. The problem says return 0.
3. Starting `minPrice` at `prices[0]` without checking for an empty array. LeetCode guarantees n ≥ 1, but `MAX_VALUE` is safer.

---

## 9. Interview questions and answers

**Q1. Why is tracking just the minimum enough?**
> For a sell on day i, the best buy is the cheapest earlier day. That depends only on the minimum of `prices[0..i]`, so one running variable keeps everything I need.

**Q2. Prove the greedy is correct.**
> Take the optimal pair (b, s). When the loop reaches day s, `minPrice ≤ prices[b]` because b < s was already seen. So the loop's profit at s is at least `prices[s] − prices[b]`, the optimal value. And the loop never computes a profit that isn't achievable. So it returns exactly the optimum.

**Q3. Return the actual buy and sell days too.**
> Keep `minIndex` (updated when `minPrice` changes) and save `bestBuy = minIndex, bestSell = i` whenever `maxProfit` improves.

### Follow-up questions (the "stock series", very common)

**F1. Unlimited transactions (LC 122).**
> Add every positive daily rise: `profit += max(0, prices[i] − prices[i−1])`. Any up-run can be split into one-day trades with the same total.

**F2. At most two transactions (LC 123).**
> Use four running states: `buy1, sell1, buy2, sell2`. Update them in order each day: `buy1 = max(buy1, −p)`, `sell1 = max(sell1, buy1 + p)`, `buy2 = max(buy2, sell1 − p)`, `sell2 = max(sell2, buy2 + p)`. O(n), O(1).

**F3. At most k transactions (LC 188).**
> Generalise to arrays `buy[1..k]` and `sell[1..k]`. O(nk). If `k ≥ n/2`, it's the same as unlimited. See [15-DP H11_StockIV](../../15-Dynamic-Programming/04-Hard/H11_StockIV.md).

**F4. Cooldown of one day after selling (LC 309).**
> Three states: holding, just sold, resting. See [15-DP M25](../../15-Dynamic-Programming/03-Medium/M25_StockWithCooldown.md).

**F5. Transaction fee per trade (LC 714).**
> Two states, and subtract the fee when selling. See [15-DP M26](../../15-Dynamic-Programming/03-Medium/M26_StockWithTransactionFee.md).

**F6. Prices arrive as a live stream.**
> The same algorithm works as is: it's already single-pass with O(1) memory, so you can update `minPrice` / `maxProfit` as each price arrives.

---

## 10. Running the file

```bash
java E05_BestTimeToBuyAndSellStock.java      # 5/5 tests passed
```

**One-line summary:** *"Sell-today profit = price − cheapest price before today; keep the best."*
