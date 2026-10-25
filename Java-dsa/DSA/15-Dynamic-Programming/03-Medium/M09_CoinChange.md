# M09 · Coin Change

| | |
|---|---|
| **LeetCode** | [#322 Coin Change](https://leetcode.com/problems/coin-change/) |
| **Difficulty** | Medium |
| **Pattern** | **Unbounded knapsack**, minimising the number of items |
| **Code** | [M09_CoinChange.java](M09_CoinChange.java) |
| **Run** | `java M09_CoinChange.java` |

The DP recipe is explained in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe); knapsack variants are surveyed in [01-Basics/KnapsackPatterns.java](../01-Basics/KnapsackPatterns.java).

---

## 1. The problem

Return the fewest coins (with unlimited coins of each denomination) that make up `amount`, or −1 if it's impossible.

| coins | amount | answer |
|---|---|---|
| [1, 2, 5] | 11 | 3 (5 + 5 + 1) |
| [2] | 3 | −1 |
| [1] | 0 | 0 |
| [1, 3, 4] | 6 | 2 (3 + 3) |
| [186, 419, 83, 408] | 6249 | 20 |

---

## 2. How to think about it

### Greedy fails for arbitrary coins
[1, 3, 4], amount 6: greedy takes 4 + 1 + 1 = 3 coins, but 3 + 3 = 2 coins. (Greedy happens to work for real currency systems like [1, 2, 5, 10], but not in general.)

### Look at the last coin
The **last** coin used is some c, and what's left, a − c, must itself be made optimally.

| step | for this problem |
|---|---|
| **State** | dp[a] = the fewest coins summing to a (INF if impossible) |
| **Transition** | dp[a] = 1 + min over coins c ≤ a of dp[a − c] |
| **Base** | dp[0] = 0 |
| **Order** | a from 1 up to amount |
| **Answer** | dp[amount], or −1 if it's still INF |

Use **INF = amount + 1** (no answer can need more than `amount` coins, since the smallest coin is 1) instead of `Integer.MAX_VALUE`, so `INF + 1` can't overflow.

### Dry run: [1, 2, 5], amount 11 (from a real run)

| a | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| dp[a] | 0 | 1 | 1 | 2 | 2 | 1 | 2 | 2 | 3 | 3 | 2 | **3** |

dp[11] = 1 + min(dp[10], dp[9], dp[6]) = 1 + min(2, 3, 2) = **3**.

### Alternative: BFS
BFS from 0 where each edge adds one coin: the levels are coin counts, and the first level that reaches `amount` is the answer.

---

## 3. Java concepts used in this problem

### 3.1 A safe "infinity"

```java
int INF = amount + 1;
Arrays.fill(dp, INF);
```

`dp[a − c] + 1` is at most `amount + 2`, so nothing overflows, and the final check `dp[amount] == INF` detects "impossible".

### 3.2 `Integer[]` memo with `null` for "unknown" (in the referee)

The memoized recursion uses `Integer[] cache`, where `null` means "not computed yet" and `Integer.MAX_VALUE` means "impossible". Memoization was explained in [Recursion E01](../../13-Recursion/02-Easy/E01_FibonacciNumber.md#34-integer-as-a-memo-null-means-not-computed-yet).

### 3.3 `return cache[a] = best;`

An assignment is an expression, so this stores and returns the value in one step.

### 3.4 Greedy with `/` and `%`

```java
count += amount / s[i];      // how many of this coin fit
amount %= s[i];              // what's left
```

---

## 4. Approach 1: Greedy (wrong in general)

### Code ([M09_CoinChange.java:56-61](M09_CoinChange.java#L56-L61))

```java
static int greedy(int[] coins, int amount) {
    int[] s = coins.clone(); Arrays.sort(s);
    int count = 0;
    for (int i = s.length - 1; i >= 0; i--) { count += amount / s[i]; amount %= s[i]; }
    return amount == 0 ? count : -1;
}
```

Biggest coin first. The test shows it returns 3 for [1, 3, 4], 6, where the answer is 2.

---

## 5. Approach 2: Memoized recursion (the referee)

### Code ([M09_CoinChange.java:47-54](M09_CoinChange.java#L47-L54))

```java
static int memo(int[] coins, int a, Integer[] cache) {
    if (a == 0) return 0;
    if (cache[a] != null) return cache[a];
    int best = Integer.MAX_VALUE;
    for (int c : coins)
        if (c <= a) { int r = memo(coins, a - c, cache); if (r != Integer.MAX_VALUE) best = Math.min(best, r + 1); }
    return cache[a] = best;
}
```

The same recurrence, top-down: try every coin as the last one. `r != MAX_VALUE` skips impossible remainders.

- **Time O(amount · coins)**, plus recursion depth up to `amount`.

---

## 6. Approach 3: Bottom-up table (the file's main solution)

### Code ([M09_CoinChange.java:35-44](M09_CoinChange.java#L35-L44))

```java
static int coinChange(int[] coins, int amount) {
    int INF = amount + 1;
    int[] dp = new int[amount + 1];
    Arrays.fill(dp, INF);
    dp[0] = 0;
    for (int a = 1; a <= amount; a++)
        for (int c : coins)
            if (c <= a) dp[a] = Math.min(dp[a], dp[a - c] + 1);
    return dp[amount] == INF ? -1 : dp[amount];
}
```

### Line by line

| Line | What it does |
|---|---|
| `INF` | larger than any real answer |
| `dp[0] = 0` | zero coins make 0 |
| the outer loop | every amount, smallest first |
| the inner loop | every coin as the last one |
| the return | −1 if the amount is unreachable |

### Complexity
- **Time O(amount · coins), space O(amount).**

---

## 7. Comparing the approaches

| | Greedy | Memo | Table |
|---|---|---|---|
| Correct | **no** | yes | yes |
| Time | O(coins log coins) | O(amount · coins) | O(amount · coins) |
| Recursion | no | depth up to amount | no |

The file checks the table against the memoized recursion on 500 random coin sets and amounts.

---

## 8. Edge cases and common mistakes

**Edge cases:** amount 0 (0 coins), no combination possible (−1), a single coin, coins larger than the amount.

**Mistakes:**
1. `Integer.MAX_VALUE + 1` overflowing to a negative number.
2. Greedy.
3. Returning INF instead of −1.
4. Looping coins outside and amounts inside, thinking it matters here (for the **minimum**, either loop order works; it matters for **counting**, see [M10](M10_CoinChangeII.md)).

---

## 9. Interview questions and answers

**Q1. Why does greedy fail?**
> Taking the largest coin can leave a remainder that needs many small coins, while a smaller first coin leaves a remainder that's cheap to make. DP considers every last coin.

**Q2. Why is INF = amount + 1 enough?**
> With a coin of value at least 1, no amount needs more than `amount` coins, so any value above that means "impossible".

### Follow-up questions

**F1. Count the combinations instead.** → [M10 Coin Change II](M10_CoinChangeII.md)
> Sum instead of min, with the coin loop on the outside.

**F2. Each coin can be used at most once.**
> 0/1 knapsack: iterate the amounts **downwards** inside the coin loop so each coin is counted once.

**F3. Perfect Squares.** → [M08](M08_PerfectSquares.md)
> This problem with the squares as coins.

---

## 10. Running the file

```bash
java M09_CoinChange.java      # 7/7 tests passed (including greedy failing on [1,3,4] and 500 random vs memoized recursion)
```

**One-line summary:** *"dp[a] = 1 + min over coins c ≤ a of dp[a − c], with dp[0] = 0 and amount + 1 as infinity; return −1 if dp[amount] stayed infinite."*
