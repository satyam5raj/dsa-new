# E01 · Climbing Stairs

| | |
|---|---|
| **LeetCode** | [#70 Climbing Stairs](https://leetcode.com/problems/climbing-stairs/) |
| **Difficulty** | Easy |
| **Pattern** | 1-D DP, the "hello world" of dynamic programming (it's Fibonacci) |
| **Code** | [E01_ClimbingStairs.java](E01_ClimbingStairs.java) |
| **Run** | `java E01_ClimbingStairs.java` |

This is the first note of the Dynamic Programming topic, so section 3 covers **the DP recipe**: state, transition, base case, order, answer, and the memo → table → rolling-variables progression. Memoization itself was introduced in [Recursion E01 Fibonacci](../../13-Recursion/02-Easy/E01_FibonacciNumber.md#34-integer-as-a-memo-null-means-not-computed-yet).

---

## 1. The problem

There are n steps. Each move climbs 1 or 2 steps. How many distinct ways are there to reach the top?

| n | answer | ways |
|---|---|---|
| 1 | 1 | 1 |
| 2 | 2 | 1+1, 2 |
| 3 | 3 | 1+1+1, 1+2, 2+1 |
| 5 | 8 | |
| 45 | 1,836,311,903 | (LeetCode's maximum) |

---

## 2. How to think about it

### Brute force
`ways(n) = ways(n − 1) + ways(n − 2)` as plain recursion: O(2ⁿ), recomputing the same values over and over (exactly like plain Fibonacci).

### Look at the last move
The **last** move to step n was either a 1-step (from n − 1) or a 2-step (from n − 2). Those two groups of paths don't overlap and together cover every path, so their counts simply add:

| step of the recipe | for this problem |
|---|---|
| **State** | dp[i] = the number of ways to reach step i |
| **Transition** | dp[i] = dp[i − 1] + dp[i − 2] |
| **Base** | dp[0] = 1 (one way to "be" at the bottom: do nothing), dp[1] = 1 |
| **Order** | i from 2 up to n |
| **Answer** | dp[n] |

dp[i] only reads the previous two values, so two variables are enough.

### Dry run: n = 5

| i | 0 | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|---|
| dp[i] | 1 | 1 | 2 | 3 | 5 | **8** |

With two variables:

| i | cur = prev1 + prev2 | prev2 | prev1 |
|---|---|---|---|
| start | | 1 | 1 |
| 2 | 2 | 1 | 2 |
| 3 | 3 | 2 | 3 |
| 4 | 5 | 3 | 5 |
| 5 | 8 | 5 | **8** |

---

## 3. Java concepts used in this problem (and the DP recipe)

### 3.1 What dynamic programming is

DP is **recursion plus a cache**. It applies when:
1. **Overlapping subproblems:** the brute-force recursion solves the same subproblem many times (ways(3) is needed by both ways(4) and ways(5));
2. **Optimal substructure:** the answer is built from the answers to smaller subproblems.

Storing each subproblem's answer once turns exponential time into polynomial time.

### 3.2 The five-step recipe

```
1. STATE       what does dp[i] (or dp[i][j]) MEAN? Write it as one English sentence.
2. TRANSITION  how is it built from smaller states? Look at the LAST choice.
3. BASE        the smallest states you know directly.
4. ORDER       fill the table so every input is ready before it's needed.
5. ANSWER      which cell? (dp[n], the max over all dp[i], dp[0][n−1], ...)
```

Writing the state as a precise sentence is the most important step. Most DP bugs come from a vague state.

### 3.3 Three implementations of the same recurrence

| style | how | pros | cons |
|---|---|---|---|
| **memoization** (top-down) | the recursion, plus a cache checked at the start of each call | follows the recurrence directly; computes only the states needed | recursion depth, call overhead |
| **tabulation** (bottom-up) | a loop that fills dp[] in order | no recursion, no stack overflow | must work out the fill order |
| **rolling variables** | keep only the states the transition reads | O(1) or O(row) space | can't reconstruct the path afterwards |

For this problem:

```java
class ThreeWays {
    static int memo(int n, int[] cache) {                 // cache[i] == 0 means "not computed" (every answer is ≥ 1)
        if (n <= 1) return 1;
        if (cache[n] != 0) return cache[n];
        return cache[n] = memo(n - 1, cache) + memo(n - 2, cache);
    }

    static int table(int n) {
        int[] dp = new int[n + 1];
        dp[0] = dp[1] = 1;
        for (int i = 2; i <= n; i++) dp[i] = dp[i - 1] + dp[i - 2];
        return dp[n];
    }
}
```

(Call `memo(n, new int[n + 1])`. Here a plain `int[]` works as the cache because no answer is 0; [Fibonacci](../../13-Recursion/02-Easy/E01_FibonacciNumber.md#34-integer-as-a-memo-null-means-not-computed-yet) needed `Integer[]` because F(0) = 0.)

### 3.4 Why dp[0] = 1

"The number of ways to climb 0 steps" is 1: the empty sequence of moves. It's also what makes dp[2] = dp[1] + dp[0] = 2 come out right. Choosing base cases so the recurrence works at the edges is a common DP technique.

### 3.5 Variable names for rolling state

`prev2` and `prev1` stand for dp[i − 2] and dp[i − 1]. After computing `cur`, they **shift**: `prev2 = prev1; prev1 = cur;`. The order matters; doing it the other way round would overwrite `prev1` before it's copied.

---

## 4. Approach 1: Plain recursion (the referee)

### Code ([E01_ClimbingStairs.java:44](E01_ClimbingStairs.java#L44))

```java
static int brute(int n) { return n <= 1 ? 1 : brute(n - 1) + brute(n - 2); }
```

The recurrence with no cache. **O(φⁿ)** calls (φ ≈ 1.618); the test only runs it for n ≤ 30.

---

## 5. Approach 2: Two rolling variables (the file's main solution)

### Code ([E01_ClimbingStairs.java:34-42](E01_ClimbingStairs.java#L34-L42))

```java
static int climbStairs(int n) {
    int prev2 = 1, prev1 = 1;                         // dp[i-2], dp[i-1], starting at dp[0], dp[1]
    for (int i = 2; i <= n; i++) {
        int cur = prev1 + prev2;
        prev2 = prev1;
        prev1 = cur;
    }
    return prev1;
}
```

### Line by line

| Line | What it does |
|---|---|
| `prev2 = 1, prev1 = 1` | dp[0] and dp[1] |
| the loop | compute dp[2], dp[3], …, dp[n] |
| `cur` | dp[i] = dp[i − 1] + dp[i − 2] |
| the shift | slide the window forward |
| `return prev1` | dp[n] (for n = 1 the loop doesn't run, and dp[1] = 1) |

### Complexity
- **Time O(n), space O(1).**

---

## 6. Comparing the approaches

| | Plain recursion | Memo | Table | Two variables |
|---|---|---|---|---|
| Time | O(φⁿ) | O(n) | O(n) | O(n) |
| Space | O(n) stack | O(n) | O(n) | **O(1)** |

The file checks the two-variable version against plain recursion for n = 1..30, plus n = 45.

An O(log n) version exists using matrix exponentiation, as described in [Recursion E01](../../13-Recursion/02-Easy/E01_FibonacciNumber.md#8-comparing-the-approaches).

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 1, n = 2, n = 45 (the answer just fits in an `int`).

**Mistakes:**
1. Base cases dp[1] = 1, dp[2] = 2 with a loop starting at 2 (it recomputes dp[2] using an undefined dp[0]).
2. Shifting the variables in the wrong order.
3. Using `int` for n > 45 (overflow).

---

## 8. Interview questions and answers

**Q1. Why is the answer Fibonacci?**
> The last move splits all paths to step n into those ending with a 1-step and those ending with a 2-step, giving dp[n] = dp[n − 1] + dp[n − 2], with dp[0] = dp[1] = 1: the Fibonacci numbers shifted by one.

**Q2. What changes if you can climb 1, 2 or 3 steps?**
> dp[i] = dp[i − 1] + dp[i − 2] + dp[i − 3], with three rolling variables.

**Q3. What if some steps are broken?**
> Set dp[i] = 0 for a broken step i; the rest of the recurrence is unchanged.

### Follow-up questions

**F1. Min Cost Climbing Stairs.** → [E02](E02_MinCostClimbingStairs.md)
> The same shape, minimising cost instead of counting.

**F2. Steps from any set S.**
> dp[i] = sum of dp[i − s] for s in S: that's [Coin Change II](../03-Medium/M10_CoinChangeII.md) with order mattering (Combination Sum IV).

**F3. Decode Ways.** → [M03](../03-Medium/M03_DecodeWays.md)
> Fibonacci-like as well, but each "step" (one or two digits) is only allowed when it forms a valid letter.

---

## 9. Running the file

```bash
java E01_ClimbingStairs.java      # 6/6 tests passed (including n = 45 and n = 1..30 vs plain recursion)
```

**One-line summary:** *"The last move is 1 or 2 steps, so dp[i] = dp[i − 1] + dp[i − 2] with dp[0] = dp[1] = 1; keep only the previous two values."*
