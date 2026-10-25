# M14 · Unique Paths

| | |
|---|---|
| **LeetCode** | [#62 Unique Paths](https://leetcode.com/problems/unique-paths/) |
| **Difficulty** | Medium |
| **Pattern** | **Grid DP** (counting) with a rolling row (plus the combinatorics shortcut) |
| **Code** | [M14_UniquePaths.java](M14_UniquePaths.java) |
| **Run** | `java M14_UniquePaths.java` |

The DP recipe is explained in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe). This is the first grid DP of the topic.

---

## 1. The problem

A robot starts at the top-left of an m × n grid and can only move **right** or **down**. How many different paths reach the bottom-right?

| m × n | answer |
|---|---|
| 3 × 7 | 28 |
| 3 × 2 | 3 |
| 1 × 1 | 1 |
| 23 × 12 | 193,536,720 |

---

## 2. How to think about it

### The last move
The last move into (r, c) came either from **above** (r − 1, c) or from the **left** (r, c − 1). Those groups don't overlap, so:

| step | for this problem |
|---|---|
| **State** | dp[r][c] = the number of paths to (r, c) |
| **Transition** | dp[r][c] = dp[r − 1][c] + dp[r][c − 1] |
| **Base** | the first row and the first column are all 1 (only one straight path) |
| **Order** | row by row, left to right |
| **Answer** | dp[m − 1][n − 1] |

### Rolling one row
Row r only needs row r − 1, so a single array is enough, updated left to right:

```
dp[c] += dp[c − 1]
```

Before the update, dp[c] still holds the value from the row **above**; dp[c − 1] has already been updated, so it's the cell to the **left** in the current row.

### The math shortcut
Every path is (m − 1) downs and (n − 1) rights in some order. Choosing where the downs go gives

```
C(m + n − 2, m − 1)
```

computed incrementally so it never overflows.

### Dry run: m = 3, n = 3 (the full table)

| | c0 | c1 | c2 |
|---|---|---|---|
| r0 | 1 | 1 | 1 |
| r1 | 1 | 2 | 3 |
| r2 | 1 | 3 | **6** |

C(4, 2) = 6 ✓

### Dry run: the rolling array for m = 3, n = 3

| after row | dp |
|---|---|
| 0 | [1, 1, 1] |
| 1 | [1, 1+1 = 2, 1+2 = 3] |
| 2 | [1, 1+2 = 3, 3+3 = 6] |

---

## 3. Java concepts used in this problem

### 3.1 In-place row updates

```java
for (int c = 1; c < n; c++) dp[c] += dp[c - 1];
```

The order matters: going left to right, `dp[c − 1]` is already the new row's value and `dp[c]` is still the old row's value, exactly the two inputs we need. Column 0 never changes (always 1).

### 3.2 Computing a binomial coefficient without overflow

```java
for (int i = 1; i <= k; i++) res = res * (total - k + i) / i;
```

After step i, `res` equals C(total − k + i, i), which is always an integer, so each division is exact. Using `long` and the smaller of m − 1 and n − 1 for k keeps the numbers small.

### 3.3 Recursion with a grid base case (in the referee)

`r == 0 || c == 0 ? 1 : brute(r − 1, c) + brute(r, c − 1)` is the recurrence written top-down, with no memo.

---

## 4. Approach 1: Plain recursion (the referee)

### Code ([M14_UniquePaths.java:51](M14_UniquePaths.java#L51))

```java
static int brute(int r, int c) { return r == 0 || c == 0 ? 1 : brute(r - 1, c) + brute(r, c - 1); }
```

- **Time exponential** (it equals the answer itself, which grows like C(m + n, m)). The test uses grids up to 12 × 12.

---

## 5. Approach 2: Grid DP with one row (the file's main solution)

### Code ([M14_UniquePaths.java:36-42](M14_UniquePaths.java#L36-L42))

```java
static int uniquePaths(int m, int n) {
    int[] dp = new int[n];
    Arrays.fill(dp, 1);                               // row 0: all ones
    for (int r = 1; r < m; r++)
        for (int c = 1; c < n; c++) dp[c] += dp[c - 1];   // above (old dp[c]) + left (new dp[c-1])
    return dp[n - 1];
}
```

| Line | What it does |
|---|---|
| `Arrays.fill(dp, 1)` | the first row |
| the outer loop | rows 1..m − 1 |
| the inner loop | from column 1 (column 0 stays 1) |

- **Time O(m · n), space O(n).**

---

## 6. Approach 3: Combinatorics

### Code ([M14_UniquePaths.java:44-49](M14_UniquePaths.java#L44-L49))

```java
static long uniquePathsMath(int m, int n) {
    long res = 1;
    int k = Math.min(m, n) - 1, total = m + n - 2;
    for (int i = 1; i <= k; i++) res = res * (total - k + i) / i;   // stays an integer at every step
    return res;
}
```

- **Time O(min(m, n)), space O(1).**

---

## 7. Comparing the approaches

| | Recursion | Grid DP | Combinatorics |
|---|---|---|---|
| Time | exponential | O(m · n) | **O(min(m, n))** |
| Space | O(m + n) stack | O(n) | O(1) |
| Works with obstacles? | yes | **yes** | no |

The file checks all three agree for every size up to 12 × 12.

---

## 8. Edge cases and common mistakes

**Edge cases:** a 1 × n or m × 1 grid (1 path), 1 × 1 (1), large grids (the answer fits in an `int` for LeetCode's limits).

**Mistakes:**
1. Updating the row right to left (it reads the wrong "left" value).
2. Computing C(n, k) with factorials (overflows quickly).
3. Off-by-one: there are m − 1 downs, not m.

---

## 9. Interview questions and answers

**Q1. Why can one row replace the whole table?**
> Each cell depends only on the cell above (the previous row, same column) and the cell to its left (the current row). Updating left to right keeps exactly those two values available.

**Q2. When does the formula stop working?**
> As soon as cells can be blocked or have different costs, because then not every ordering of moves is allowed. The DP still works.

### Follow-up questions

**F1. Unique Paths II (with obstacles).** → [M15](M15_UniquePathsII.md)

**F2. Minimum Path Sum.** → [M16](M16_MinimumPathSum.md)
> The same grid with min instead of +.

**F3. Moving diagonally too.**
> Add dp[r − 1][c − 1] to the transition (Delannoy numbers).

---

## 10. Running the file

```bash
java M14_UniquePaths.java      # 5/5 tests passed (including all 12x12 sizes: DP == recursion == C(.,.))
```

**One-line summary:** *"Paths to a cell = paths to the cell above + paths to the cell on the left; one array updated left to right does it, or use C(m + n − 2, m − 1)."*
