# M10 · Coin Change II

| | |
|---|---|
| **LeetCode** | [#518 Coin Change II](https://leetcode.com/problems/coin-change-ii/) |
| **Difficulty** | Medium |
| **Pattern** | Unbounded knapsack, **counting combinations** (coins in the **outer** loop) |
| **Code** | [M10_CoinChangeII.java](M10_CoinChangeII.java) |
| **Run** | `java M10_CoinChangeII.java` |

The DP recipe is explained in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe); the minimum-coins version is [M09](M09_CoinChange.md). Both loop orders are compared side by side in [01-Basics/KnapsackPatterns.java](../01-Basics/KnapsackPatterns.java).

---

## 1. The problem

Count the **combinations** (order doesn't matter) of coins that sum to `amount`, with unlimited coins of each kind.

| amount | coins | answer | combinations |
|---|---|---|---|
| 5 | [1, 2, 5] | 4 | 5; 2+2+1; 2+1+1+1; 1+1+1+1+1 |
| 3 | [2] | 0 | |
| 10 | [10] | 1 | |
| 0 | [7] | 1 | the empty combination |

---

## 2. How to think about it

### The trap
`ways[a] = sum over coins of ways[a − c]` counts 1 + 2 and 2 + 1 **separately**. That's the number of ordered sequences (Combination Sum IV, the **permutation** count), which gives **9** for the first example, not 4.

### Impose an order on the coins
Decide coin type by coin type:

| step | for this problem |
|---|---|
| **State** | ways[i][a] = the number of combinations of the first i coin types that sum to a |
| **Transition** | ways[i][a] = ways[i − 1][a] (use no more of coin i) + ways[i][a − coinᵢ] (use one more of coin i) |
| **Base** | ways[·][0] = 1 |
| **Answer** | ways[n][amount] |

As a single row, with the coins in the **outer** loop and the amounts going **up** (because coin i can be reused):

```
for coin in coins:
    for a = coin .. amount:
        ways[a] += ways[a − coin]
```

Each multiset of coins is then built in exactly one order: non-decreasing coin index.

### Dry run: [1, 2, 5], amount 5

| after coin | ways[0..5] |
|---|---|
| (start) | 1, 0, 0, 0, 0, 0 |
| 1 | 1, 1, 1, 1, 1, 1 |
| 2 | 1, 1, 2, 2, 3, 3 |
| 5 | 1, 1, 2, 2, 3, **4** |

After coin 2, ways[4] = 3: {1,1,1,1}, {1,1,2}, {2,2}. After coin 5, ways[5] gains ways[0] = 1 for {5}.

### Why the other loop order counts orders
With the amount outside and the coins inside, ways[a] sums over **which coin is last**, for every possible history. So 1 + 2 (last coin 2) and 2 + 1 (last coin 1) both get counted.

---

## 3. Java concepts used in this problem

### 3.1 Loop order changes the meaning

| outer loop | inner loop | counts |
|---|---|---|
| coins | amounts | **combinations** (this problem) |
| amounts | coins | **permutations** (ordered sequences) |

The code differs only in which `for` is outside, so it's worth saying out loud in an interview.

### 3.2 Generating distinct random coins (in the test)

```java
do c[i] = 1 + rnd.nextInt(15); while (!used.add(c[i]));
```

A `do … while` runs the body at least once. `used.add` returns false for a repeat, so it keeps drawing until it gets a new value. (Duplicate coin values would make the "combination" count ambiguous.)

---

## 4. Approach 1: Recursion "use coin i again, or move on" (the referee)

### Code ([M10_CoinChangeII.java:54-58](M10_CoinChangeII.java#L54-L58))

```java
static int brute(int[] coins, int i, int rem) {     // recursion: use coin i again, or move on
    if (rem == 0) return 1;
    if (i == coins.length) return 0;
    return (coins[i] <= rem ? brute(coins, i, rem - coins[i]) : 0) + brute(coins, i + 1, rem);
}
```

The 2-D transition from section 2, written top-down: each combination is built by deciding, for each coin type in order, how many to use.

- **Time:** exponential without memoization.

---

## 5. Approach 2: Amount in the outer loop (wrong: it counts orders)

### Code ([M10_CoinChangeII.java:47-52](M10_CoinChangeII.java#L47-L52))

```java
static int permutationsWrong(int amount, int[] coins) {   // amount outside -> counts orders
    int[] ways = new int[amount + 1];
    ways[0] = 1;
    for (int a = 1; a <= amount; a++) for (int c : coins) if (c <= a) ways[a] += ways[a - c];
    return ways[amount];
}
```

It returns 9 for amount 5 with [1, 2, 5]. (It's the right answer to **Combination Sum IV**, LC 377.)

---

## 6. Approach 3: Coins outside, amounts inside (the file's main solution)

### Code ([M10_CoinChangeII.java:39-45](M10_CoinChangeII.java#L39-L45))

```java
static int change(int amount, int[] coins) {
    int[] ways = new int[amount + 1];
    ways[0] = 1;                                      // one way to make 0: no coins
    for (int coin : coins)                            // coin types OUTSIDE -> combinations
        for (int a = coin; a <= amount; a++) ways[a] += ways[a - coin];
    return ways[amount];
}
```

### Line by line

| Line | What it does |
|---|---|
| `ways[0] = 1` | the empty combination |
| the outer loop | add one coin type at a time |
| the inner loop | going **up**, so ways[a − coin] already includes this coin (reuse allowed) |

### Complexity
- **Time O(amount · coins), space O(amount).**

---

## 7. Comparing the approaches

| | Recursion | Amount outside | Coins outside |
|---|---|---|---|
| Counts | combinations | **permutations** | combinations |
| Time | exponential | O(amount · coins) | O(amount · coins) |

The file checks the coins-outside version against the recursion on 400 random cases with distinct coins, and checks that the other order gives 9.

---

## 8. Edge cases and common mistakes

**Edge cases:** amount 0 (1 way), no way to make the amount (0), a single coin.

**Mistakes:**
1. Swapping the loops (counting permutations).
2. Iterating the amounts **downwards** (that's 0/1 knapsack: each coin used at most once).
3. `ways[0] = 0` (then everything stays 0).

---

## 9. Interview questions and answers

**Q1. Why does the loop order matter?**
> With coins outside, each combination is built with its coins in a fixed type order, so it's counted once. With amounts outside, every ordering of the same coins is counted.

**Q2. What does the inner loop's direction mean?**
> Upward lets ways[a − coin] already include the current coin, so it can be used again (unbounded). Downward would only see the previous coin types (0/1).

### Follow-up questions

**F1. Combination Sum IV (count ordered sequences).**
> The amount-outside version above.

**F2. Coin Change (minimum coins).** → [M09](M09_CoinChange.md)

**F3. List the combinations.** → [Backtracking M06 Combination Sum](../../14-Backtracking/03-Medium/M06_CombinationSum.md)
> Its referee is exactly this counting DP.

---

## 10. Running the file

```bash
java M10_CoinChangeII.java      # 6/6 tests passed (including 400 random vs recursion and the 9-permutations check)
```

**One-line summary:** *"ways[0] = 1; for each coin (outer loop), for a from coin up to amount, ways[a] += ways[a − coin]; the coin-outer order counts each combination once."*
