# M08 · Perfect Squares

| | |
|---|---|
| **LeetCode** | [#279 Perfect Squares](https://leetcode.com/problems/perfect-squares/) |
| **Difficulty** | Medium |
| **Pattern** | **Unbounded knapsack**, minimum count (the "coins" are 1, 4, 9, 16, …) |
| **Code** | [M08_PerfectSquares.java](M08_PerfectSquares.java) |
| **Run** | `java M08_PerfectSquares.java` |

The DP recipe is explained in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe). The general version is [M09 Coin Change](M09_CoinChange.md).

---

## 1. The problem

Return the least number of perfect squares (1, 4, 9, 16, …) that sum to n.

| n | answer | squares |
|---|---|---|
| 12 | 3 | 4 + 4 + 4 |
| 13 | 2 | 4 + 9 |
| 1 | 1 | 1 |

---

## 2. How to think about it

### Greedy fails
Taking the largest square each time: 12 → 9 + 1 + 1 + 1 = **4** squares, but 4 + 4 + 4 is only 3.

### It's Coin Change with the squares as coins

| step | for this problem |
|---|---|
| **State** | dp[i] = the fewest squares summing to i |
| **Transition** | dp[i] = 1 + min over squares s ≤ i of dp[i − s] (the last square used is s) |
| **Base** | dp[0] = 0 |
| **Answer** | dp[n] |

Every square can be used any number of times, which is what "unbounded" means.

### Dry run (from a real run)

| i | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| dp[i] | 0 | 1 | 2 | 3 | 1 | 2 | 3 | 4 | 2 | 1 | 2 | 3 | **3** | **2** |

dp[12] = 1 + min(dp[11], dp[8], dp[3]) = 1 + min(3, 2, 3) = **3** (from the squares 1, 4, 9).

### Alternatives worth mentioning
- **BFS:** level k is the set of numbers reachable with k squares; the first level containing n is the answer. Same O(n√n), often faster in practice.
- **Math (Lagrange + Legendre):** every n is a sum of 4 squares (Lagrange), and it needs exactly 4 iff n = 4ᵃ(8b + 7) (Legendre). Check for 1 and 2 directly; otherwise the answer is 3. O(√n).

---

## 3. Java concepts used in this problem

### 3.1 Looping over squares with `s * s <= i`

```java
for (int s = 1; s * s <= i; s++) dp[i] = Math.min(dp[i], dp[i - s * s] + 1);
```

Squaring the counter avoids computing a square root, and stops exactly at the largest square ≤ i.

### 3.2 Initialising to `Integer.MAX_VALUE` safely

`dp[i] = Integer.MAX_VALUE` and then `dp[i − s·s] + 1` is safe here: `dp[i − 1]` (with s = 1) is always finite, since every number is a sum of 1s. So at least one candidate is a real number. (In [Coin Change](M09_CoinChange.md), where a state can be impossible, a smaller "infinity" is used to avoid overflow.)

### 3.3 Testing for a perfect square

```java
static boolean isSquare(int x) { int r = (int) Math.sqrt(x); return r * r == x; }
```

`Math.sqrt` returns a `double`; truncating it to an `int` and squaring back checks whether x is exactly a square. (For values up to about 2⁵², the `double` square root is accurate enough for this.)

### 3.4 Dividing out factors of 4

```java
while (m % 4 == 0) m /= 4;
if (m % 8 == 7) return 4;
```

This removes the 4ᵃ factor and tests the 8b + 7 form from Legendre's theorem.

---

## 4. Approach 1: Greedy (wrong, kept as a warning)

### Code ([M08_PerfectSquares.java:56](M08_PerfectSquares.java#L56))

```java
static int greedy(int n) { int c = 0; while (n > 0) { int r = (int) Math.sqrt(n); n -= r * r; c++; } return c; }
```

It returns 4 for n = 12 (9 + 1 + 1 + 1). The test checks that it's wrong.

---

## 5. Approach 2: Unbounded-knapsack DP (the file's main solution)

### Code ([M08_PerfectSquares.java:36-43](M08_PerfectSquares.java#L36-L43))

```java
static int numSquares(int n) {
    int[] dp = new int[n + 1];
    for (int i = 1; i <= n; i++) {
        dp[i] = Integer.MAX_VALUE;
        for (int s = 1; s * s <= i; s++) dp[i] = Math.min(dp[i], dp[i - s * s] + 1);
    }
    return dp[n];
}
```

| Line | What it does |
|---|---|
| `dp[0] = 0` | (the array's default) |
| `dp[i] = MAX_VALUE` | start with "infinity" |
| the inner loop | try every square as the last one |

- **Time O(n√n), space O(n).**

---

## 6. Approach 3: Number theory

### Code ([M08_PerfectSquares.java:45-54](M08_PerfectSquares.java#L45-L54))

```java
static int numSquaresMath(int n) {
    if (isSquare(n)) return 1;
    int m = n;
    while (m % 4 == 0) m /= 4;
    if (m % 8 == 7) return 4;                         // Legendre's three-square theorem
    for (int a = 1; a * a <= n; a++) if (isSquare(n - a * a)) return 2;
    return 3;
}
```

| Line | What it does |
|---|---|
| `isSquare(n)` | the answer is 1 |
| the Legendre test | n = 4ᵃ(8b + 7) needs exactly 4 |
| the loop | is n − a² a square for some a? Then 2 |
| otherwise | 3 (guaranteed by the theorems) |

- **Time O(√n), space O(1).**

---

## 7. Comparing the approaches

| | Greedy | DP | Number theory |
|---|---|---|---|
| Correct | **no** | yes | yes |
| Time | O(√n) | O(n√n) | O(√n) |
| Needs theorems | | no | yes |

The file checks DP == number theory for every n from 1 to 3,000.

---

## 8. Edge cases and common mistakes

**Edge cases:** n itself a square (1), n = 7 (4: 4 + 1 + 1 + 1), large n near 10⁴.

**Mistakes:**
1. Greedy.
2. Looping `s <= Math.sqrt(i)` with `double` comparisons (works, but slower and riskier).
3. Forgetting that 1 is a perfect square (so the answer always exists).

---

## 9. Interview questions and answers

**Q1. How is this Coin Change?**
> The squares are the coin denominations, each usable any number of times, and we want the fewest coins summing to n.

**Q2. Why does BFS work?**
> Each "edge" subtracts a square. The number of edges on the shortest path from n to 0 is the minimum number of squares, and BFS finds shortest paths in unweighted graphs.

### Follow-up questions

**F1. Coin Change.** → [M09](M09_CoinChange.md)

**F2. List the squares used.**
> Store which square gave the minimum for each i, then walk back from n.

**F3. Count the ways (rather than the minimum).**
> [Coin Change II](M10_CoinChangeII.md) with squares as coins.

---

## 10. Running the file

```bash
java M08_PerfectSquares.java      # 5/5 tests passed (including n = 1..3000: DP == number theory)
```

**One-line summary:** *"dp[i] = 1 + min over squares s ≤ i of dp[i − s], with dp[0] = 0 (Coin Change with the squares as coins), or the Lagrange/Legendre shortcut that the answer is always 1–4."*
