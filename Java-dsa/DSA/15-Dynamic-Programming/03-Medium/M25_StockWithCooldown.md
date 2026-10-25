# M25 · Best Time to Buy and Sell Stock with Cooldown

| | |
|---|---|
| **LeetCode** | [#309 Best Time to Buy and Sell Stock with Cooldown](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-with-cooldown/) |
| **Difficulty** | Medium |
| **Pattern** | **State-machine DP** (3 states per day) |
| **Code** | [M25_StockWithCooldown.java](M25_StockWithCooldown.java) |
| **Run** | `java M25_StockWithCooldown.java` |

The DP recipe is explained in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe). The simplest stock problem is [Arrays E05](../../01-Arrays/02-Easy/E05_BestTimeToBuyAndSellStock.md).

---

## 1. The problem

Unlimited transactions, at most one share at a time, and after **selling** you must wait one day before buying again. What's the maximum profit?

| prices | answer | trades |
|---|---|---|
| [1, 2, 3, 0, 2] | 3 | buy 1, sell 2, cooldown, buy 0, sell 2 |
| [1] | 0 | |
| [5, 4, 3, 2, 1] | 0 | never trade |
| [1, 4, 2] | 3 | buy 1, sell 4 |

---

## 2. How to think about it

### Draw the states (at the end of each day)

```
        buy            sell
  REST ─────→ HOLD ─────→ SOLD
   ↑ ↺          ↺           │
   └────────────────────────┘
  ↺ = stay (do nothing)     after SOLD you can only go to REST: that's the cooldown
```

Every arrow is exactly one term in a `max`:

| state | transition | meaning |
|---|---|---|
| hold[i] | max(hold[i − 1], rest[i − 1] − p[i]) | keep holding, or buy today (only from REST) |
| sold[i] | hold[i − 1] + p[i] | sell today |
| rest[i] | max(rest[i − 1], sold[i − 1]) | stay idle, or finish the cooldown |

- **Base:** hold = −∞ (you can't hold before buying), sold = −∞, rest = 0.
- **Answer:** max(sold, rest) on the last day (you never want to end holding a share).

This recipe works for every stock variant: a fee ([M26](M26_StockWithTransactionFee.md)), at most k transactions ([H11](../04-Hard/H11_StockIV.md)), and cooldown (here).

### Dry run: [1, 2, 3, 0, 2] (from a real run)

| price | hold | sold | rest |
|---|---|---|---|
| 1 | max(−∞, 0 − 1) = −1 | −∞ | 0 |
| 2 | max(−1, 0 − 2) = −1 | −1 + 2 = 1 | max(0, −∞) = 0 |
| 3 | max(−1, 0 − 3) = −1 | −1 + 3 = 2 | max(0, 1) = 1 |
| 0 | max(−1, 1 − 0) = 1 | −1 + 0 = −1 | max(1, 2) = 2 |
| 2 | max(1, 2 − 2) = 1 | 1 + 2 = **3** | max(2, −1) = 2 |

Answer: max(3, 2) = **3**. On the day with price 0, "hold = 1" means "rested after selling at 2, then bought at 0" (1 − 0 = 1), which respects the cooldown.

---

## 3. Java concepts used in this problem

### 3.1 A safe "minus infinity"

```java
int NEG = Integer.MIN_VALUE / 2;
```

`sold = hold + p` with `hold = Integer.MIN_VALUE` would overflow to a huge positive number. Halving it leaves room to add prices without overflowing, while staying far below any real profit.

### 3.2 Updating several states at once

```java
int newHold = Math.max(hold, rest - p);
int newSold = hold + p;
int newRest = Math.max(rest, sold);
hold = newHold; sold = newSold; rest = newRest;
```

All three new values must be computed from **yesterday's** values. Writing them into temporaries first prevents, say, `newSold` from reading today's `hold`.

### 3.3 Recursion that skips a day (in the referee)

```java
if (holding) return Math.max(skip, p[day] + brute(p, day + 2, false));   // sell -> skip tomorrow
```

Jumping to `day + 2` after selling is the cooldown.

---

## 4. Approach 1: Recursion over (day, holding) (the referee)

### Code ([M25_StockWithCooldown.java:56-61](M25_StockWithCooldown.java#L56-L61))

```java
static int brute(int[] p, int day, boolean holding) {
    if (day >= p.length) return 0;
    int skip = brute(p, day + 1, holding);
    if (holding) return Math.max(skip, p[day] + brute(p, day + 2, false));   // sell -> skip tomorrow
    return Math.max(skip, -p[day] + brute(p, day + 1, true));
}
```

| Line | What it does |
|---|---|
| `skip` | do nothing today |
| holding | sell today and jump past tomorrow |
| not holding | buy today |

- **Time exponential** without memo; memoizing on (day, holding) gives O(n).

---

## 5. Approach 2: Three-state machine (the file's main solution)

### Code ([M25_StockWithCooldown.java:43-53](M25_StockWithCooldown.java#L43-L53))

```java
static int maxProfit(int[] prices) {
    int NEG = Integer.MIN_VALUE / 2;
    int hold = NEG, sold = NEG, rest = 0;
    for (int p : prices) {
        int newHold = Math.max(hold, rest - p);        // buying is only allowed from REST
        int newSold = hold + p;
        int newRest = Math.max(rest, sold);
        hold = newHold; sold = newSold; rest = newRest;
    }
    return Math.max(sold, rest);
}
```

| Line | What it does |
|---|---|
| the initial values | before day 0, you can only be resting with 0 profit |
| `newHold` | keep the share, or buy from REST |
| `newSold` | sell the share held yesterday |
| `newRest` | stay resting, or come out of yesterday's sale |
| the return | the best state without a share |

### Complexity
- **Time O(n), space O(1).**

---

## 6. Comparing the approaches

| | Recursion | State machine |
|---|---|---|
| Time | exponential | **O(n)** |
| Space | O(n) stack | O(1) |

The file checks the state machine against the recursion on 1,000 random price lists.

---

## 7. Edge cases and common mistakes

**Edge cases:** one day (0), falling prices (0), the cooldown not mattering (one trade).

**Mistakes:**
1. Allowing a buy from SOLD (that ignores the cooldown).
2. Updating the states in place without temporaries.
3. Starting hold at 0 (you'd "hold" a share for free).
4. Returning `hold` (ending with an unsold share is never better).

---

## 8. Interview questions and answers

**Q1. How do you design the states?**
> List what you need to know at the end of a day to decide tomorrow: whether you hold a share, and whether you just sold (so you can't buy). Each allowed action is an arrow between states, and each arrow becomes one term in a max.

**Q2. Why −∞ for hold and sold at the start?**
> Those states are unreachable before any trade; −∞ makes sure they're never chosen until a real buy happens.

### Follow-up questions

**F1. With a transaction fee.** → [M26](M26_StockWithTransactionFee.md)
> Two states; subtract the fee on each sale.

**F2. At most k transactions.** → [H11 Stock IV](../04-Hard/H11_StockIV.md)
> 2k states: hold and free for each transaction count.

**F3. A cooldown of c days.**
> Keep the last c values of `sold` (or buy from rest[i − c]).

---

## 9. Running the file

```bash
java M25_StockWithCooldown.java      # 5/5 tests passed (including 1000 random vs recursion)
```

**One-line summary:** *"Track hold, sold and rest each day: hold = max(hold, rest − p), sold = hold + p, rest = max(rest, sold); buying only from rest enforces the cooldown, and the answer is max(sold, rest)."*
