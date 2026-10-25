# M07 · Integer Break

| | |
|---|---|
| **LeetCode** | [#343 Integer Break](https://leetcode.com/problems/integer-break/) |
| **Difficulty** | Medium |
| **Pattern** | 1-D DP over "the first piece" (plus the math shortcut: use 3s) |
| **Code** | [M07_IntegerBreak.java](M07_IntegerBreak.java) |
| **Run** | `java M07_IntegerBreak.java` |

The DP recipe is explained in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe).

---

## 1. The problem

Split n (2 ≤ n ≤ 58) into **at least two** positive integers so that their product is as large as possible.

| n | answer | split |
|---|---|---|
| 2 | 1 | 1 + 1 |
| 3 | 2 | 1 + 2 |
| 10 | 36 | 3 + 3 + 4 |
| 58 | 1,549,681,956 | 3¹⁸ · 4 |

---

## 2. How to think about it

### DP over the first piece

| step | for this problem |
|---|---|
| **State** | dp[i] = the best product from splitting i into at least 2 parts |
| **Transition** | cut off a first piece j (1..i − 1); the rest, i − j, is either **kept whole** (j · (i − j)) or **split further** (j · dp[i − j]) |
| | dp[i] = max over j of j · max(i − j, dp[i − j]) |
| **Base** | dp[1] = 1 (only used as a placeholder) |
| **Answer** | dp[n] |

**The subtle part is `max(i − j, dp[i − j])`.** dp requires at least 2 parts, but the remainder is allowed to stay in one piece. For example dp[2] = 1 (1 · 1), but keeping a remainder of 2 whole is worth 2.

### Dry run (from a real run)

| i | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
|---|---|---|---|---|---|---|---|---|---|
| dp[i] | 1 | 2 | 4 | 6 | 9 | 12 | 18 | 27 | **36** |
| best j | 1 | 1 | 2 | 2 | 3 | 2 | 2 | 3 | 2 |

For example dp[10] with j = 2: 2 · max(8, dp[8] = 18) = 36. (j = 3 gives 3 · dp[7] = 36 as well.)

### The math shortcut
Use as many **3s** as possible; never use a 1; if the remainder is 1, turn a 3 + 1 into 2 + 2.

**Why 3?**
- Any piece k ≥ 5 is better split: 2 · (k − 2) > k when k > 4. (A 4 is the same as 2 + 2.)
- So the pieces are only 2s and 3s, and 3 · 3 > 2 · 2 · 2 (six units: 9 beats 8), so prefer 3s.
- (The continuous version, maximising xⁿ/ˣ, peaks at x = e ≈ 2.718, which is closest to 3.)

n = 10: 10 = 3 + 3 + 3 + 1 → 3 + 3 + 2 + 2 → 3 · 3 · 4 = **36**.

---

## 3. Java concepts used in this problem

### 3.1 Nested `Math.max`

```java
dp[i] = Math.max(dp[i], j * Math.max(i - j, dp[i - j]));
```

The inner max chooses "keep the rest whole" or "split it"; the outer max keeps the best over all j.

### 3.2 `Math.pow` returns a `double`

```java
return (int) Math.pow(3, threes) * 2;
```

`Math.pow` works in `double`, so the result must be cast. Note the precedence: the cast applies to `Math.pow(3, threes)`, and then the `int` is multiplied by 2. For n ≤ 58 the values are exact in a `double`.

### 3.3 Integer division and remainder

`n / 3` is the number of 3s and `n % 3` is what's left (0, 1 or 2).

### 3.4 Partitions with a maximum part (in the referee)

```java
static long brute(int n, int maxPart, boolean mustSplit)
```

Generating parts in non-increasing order (each part ≤ the previous one, `maxPart`) lists every partition exactly once. `mustSplit` forbids taking n itself as the only part at the top level.

---

## 4. Approach 1: Every partition (the referee)

### Code ([M07_IntegerBreak.java:51-56](M07_IntegerBreak.java#L51-L56))

```java
static long brute(int n, int maxPart, boolean mustSplit) {     // every partition of n
    if (n == 0) return 1;
    long best = 0;
    for (int p = Math.min(n - (mustSplit ? 1 : 0), maxPart); p >= 1; p--) best = Math.max(best, p * brute(n - p, p, false));
    return best;
}
```

| Line | What it does |
|---|---|
| `n == 0` | the empty product is 1 |
| the loop | the next part p, no larger than the last one (and < n at the top level) |
| `p * brute(n − p, p, false)` | p times the best product of the rest |

- **Time:** the number of partitions of n (exponential); the test uses n ≤ 30.

---

## 5. Approach 2: DP over the first piece (the file's main solution)

### Code ([M07_IntegerBreak.java:35-41](M07_IntegerBreak.java#L35-L41))

```java
static int integerBreak(int n) {
    int[] dp = new int[n + 1];
    dp[1] = 1;
    for (int i = 2; i <= n; i++)
        for (int j = 1; j < i; j++) dp[i] = Math.max(dp[i], j * Math.max(i - j, dp[i - j]));
    return dp[n];
}
```

| Line | What it does |
|---|---|
| `dp[1] = 1` | only reached through max(…), where i − j = 1 is the bigger of the two anyway |
| the loops | every i, every first piece j |

- **Time O(n²), space O(n).**

---

## 6. Approach 3: Use 3s

### Code ([M07_IntegerBreak.java:43-49](M07_IntegerBreak.java#L43-L49))

```java
static int integerBreakMath(int n) {
    if (n <= 3) return n - 1;                        // must split: 2 -> 1*1, 3 -> 1*2
    int threes = n / 3, rem = n % 3;
    if (rem == 0) return (int) Math.pow(3, threes);
    if (rem == 1) return (int) Math.pow(3, threes - 1) * 4;     // 3 + 1 -> 2 + 2
    return (int) Math.pow(3, threes) * 2;
}
```

| Case | Product |
|---|---|
| n ≤ 3 | n − 1 (the forced split) |
| rem 0 | 3ⁿᐟ³ |
| rem 1 | 3^(threes − 1) · 4 |
| rem 2 | 3^threes · 2 |

- **Time O(log n)** for the power, **space O(1).**

---

## 7. Comparing the approaches

| | Every partition | DP | Math |
|---|---|---|---|
| Time | exponential | O(n²) | **O(log n)** |
| Needs the insight | no | the "keep or split the rest" max | why 3s are optimal |

The file checks DP == math for n = 2..58 and DP == every partition for n = 2..30.

---

## 8. Edge cases and common mistakes

**Edge cases:** n = 2 (1), n = 3 (2), n = 4 (4 = 2 · 2), n = 58 (the maximum).

**Mistakes:**
1. dp[i] = max of j · dp[i − j] only (forgets keeping the rest whole: dp[4] would be 2 · dp[2] = 2 instead of 4).
2. Returning n itself for n = 2 or 3 (at least two parts are required).
3. Leaving a remainder of 1 in the math version (3 + 1 → 3 is worse than 2 + 2 → 4).

---

## 9. Interview questions and answers

**Q1. Why is `max(i − j, dp[i − j])` needed?**
> dp[k] is defined with at least two parts, but the remainder in the final split doesn't have to be split. For small k (2, 3), keeping it whole is better than splitting it.

**Q2. Prove that no piece should be 5 or more.**
> For k ≥ 5, splitting k into 2 and k − 2 gives 2(k − 2) = 2k − 4 > k. So any optimal split only uses pieces of size ≤ 4, and 4 = 2 + 2 gives the same product.

### Follow-up questions

**F1. Large n with a modulus (Cutting Rope II).**
> The math version with modular fast power ([Recursion M07](../../13-Recursion/03-Medium/M07_CountGoodNumbers.md)).

**F2. Perfect Squares.** → [M08](M08_PerfectSquares.md)
> Another "split n into pieces" DP, minimising the number of pieces.

**F3. Rod Cutting.**
> The same DP with a price table: dp[i] = max over j of price[j] + dp[i − j].

---

## 10. Running the file

```bash
java M07_IntegerBreak.java      # 5/5 tests passed (including n = 2..58: DP == math, and n = 2..30 vs every partition)
```

**One-line summary:** *"dp[i] = max over the first piece j of j · max(i − j, dp[i − j]); or simply use as many 3s as possible, replacing a 3 + 1 with 2 + 2."*
