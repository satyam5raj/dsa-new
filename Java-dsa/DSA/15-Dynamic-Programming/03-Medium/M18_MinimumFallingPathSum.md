# M18 · Minimum Falling Path Sum

| | |
|---|---|
| **LeetCode** | [#931 Minimum Falling Path Sum](https://leetcode.com/problems/minimum-falling-path-sum/) |
| **Difficulty** | Medium |
| **Pattern** | Row-by-row grid DP with **three** predecessors |
| **Code** | [M18_MinimumFallingPathSum.java](M18_MinimumFallingPathSum.java) |
| **Run** | `java M18_MinimumFallingPathSum.java` |

The DP recipe is explained in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe); the two-predecessor grids are [M16](M16_MinimumPathSum.md) and [M17](M17_Triangle.md).

---

## 1. The problem

An n × n matrix. Start anywhere in row 0. From (r, c) you may move to (r + 1, c − 1), (r + 1, c) or (r + 1, c + 1). What's the minimum sum of a falling path?

| matrix | answer | path |
|---|---|---|
| [[2,1,3],[6,5,4],[7,8,9]] | 13 | 1 → 5 → 7 or 1 → 4 → 8 |
| [[−19,57],[−40,−5]] | −59 | −19 → −40 |
| [[7]] | 7 | |

---

## 2. How to think about it

| step | for this problem |
|---|---|
| **State** | dp[r][c] = the minimum falling path sum ending at (r, c) |
| **Transition** | dp[r][c] = a[r][c] + min(dp[r−1][c−1], dp[r−1][c], dp[r−1][c+1]), using only the ones in range |
| **Base** | dp[0] = row 0 |
| **Answer** | the minimum of the last row |

### Why two rows instead of one
In [M14](M14_UniquePaths.md) and [M16](M16_MinimumPathSum.md), updating one array in place worked because the "left" input was supposed to be the **new** value. Here all three inputs come from the **previous** row, including c − 1. Updating in place would overwrite dp[c − 1] before dp[c] reads it. So build a separate `next` row from `prev`.

### Dry run: [[2,1,3],[6,5,4],[7,8,9]]

| row | c0 | c1 | c2 |
|---|---|---|---|
| 0 | 2 | 1 | 3 |
| 1 | 6 + min(2, 1) = 7 | 5 + min(2, 1, 3) = 6 | 4 + min(1, 3) = 5 |
| 2 | 7 + min(7, 6) = **13** | 8 + min(7, 6, 5) = **13** | 9 + min(6, 5) = 14 |

The minimum of the last row is **13**.

---

## 3. Java concepts used in this problem

### 3.1 Copying the first row

`int[] prev = a[0].clone();` copies row 0 so the matrix itself isn't modified.

### 3.2 Swapping rows by reference

```java
int[] next = new int[n];
...
prev = next;
```

After filling `next`, `prev` simply points at it. (Allocating a new row each time is simple; reusing two arrays and swapping them would avoid the allocations.)

### 3.3 `Arrays.stream(prev).min().getAsInt()`

`min()` on an `IntStream` returns an `OptionalInt` (empty for an empty stream); `getAsInt()` extracts the value.

### 3.4 Out-of-range as infinity (in the referee)

```java
if (c < 0 || c >= a.length) return Integer.MAX_VALUE;
```

Moving off the matrix returns "infinity", so `Math.min` never picks it. The in-range move (straight down) always exists, so `a[r][c] + best` never adds to `MAX_VALUE`.

---

## 4. Approach 1: Recursion down three ways (the referee)

### Code ([M18_MinimumFallingPathSum.java:48-53](M18_MinimumFallingPathSum.java#L48-L53))

```java
static int brute(int[][] a, int r, int c) {
    if (c < 0 || c >= a.length) return Integer.MAX_VALUE;
    if (r == a.length - 1) return a[r][c];
    int best = Math.min(brute(a, r + 1, c), Math.min(brute(a, r + 1, c - 1), brute(a, r + 1, c + 1)));
    return a[r][c] + best;
}
```

The test takes the minimum over every starting column. **O(3ⁿ)** per start.

---

## 5. Approach 2: Two rolling rows (the file's main solution)

### Code ([M18_MinimumFallingPathSum.java:32-46](M18_MinimumFallingPathSum.java#L32-L46))

```java
static int minFallingPathSum(int[][] a) {
    int n = a.length;
    int[] prev = a[0].clone();
    for (int r = 1; r < n; r++) {
        int[] next = new int[n];
        for (int c = 0; c < n; c++) {
            int best = prev[c];
            if (c > 0) best = Math.min(best, prev[c - 1]);
            if (c < n - 1) best = Math.min(best, prev[c + 1]);
            next[c] = a[r][c] + best;
        }
        prev = next;
    }
    return Arrays.stream(prev).min().getAsInt();
}
```

| Line | What it does |
|---|---|
| `prev = row 0` | the base |
| `best` | straight up, then the diagonals if they exist |
| `next[c]` | this cell plus the best way to arrive |
| the return | the cheapest place to finish |

- **Time O(n²), space O(n).**

---

## 6. Comparing the approaches

| | Recursion | Two rows |
|---|---|---|
| Time | O(n · 3ⁿ) | **O(n²)** |
| Space | O(n) stack | O(n) |

The file checks the DP against the recursion on 300 random matrices with negative values.

---

## 7. Edge cases and common mistakes

**Edge cases:** a 1 × 1 matrix, negative values, ties.

**Mistakes:**
1. Updating a single array in place (dp[c − 1] is overwritten too early).
2. Forgetting the bounds on c − 1 and c + 1.
3. Returning the last cell instead of the minimum of the last row.

---

## 8. Interview questions and answers

**Q1. Why can't you update in place here?**
> All three predecessors must come from the previous row. Going left to right, dp[c − 1] would already hold the current row's value when dp[c] needs the previous one.

**Q2. Can it be done in O(1) extra space?**
> Yes, by overwriting the matrix row by row (each row reads only the row above it, which is already final).

### Follow-up questions

**F1. Minimum Falling Path Sum II (LC 1289): you can't use the same column twice in a row.**
> Track the smallest and second-smallest values of the previous row: each cell uses the smallest unless it's in the same column, then the second smallest. O(n²).

**F2. Triangle.** → [M17](M17_Triangle.md)

**F3. Return the path.**
> Keep the full table, or the chosen predecessor per cell, and walk back from the best last cell.

---

## 9. Running the file

```bash
java M18_MinimumFallingPathSum.java      # 4/4 tests passed (including 300 random matrices vs recursion)
```

**One-line summary:** *"Each cell adds the minimum of the three cells above it (within range), built row by row from a separate previous row; the answer is the minimum of the last row."*
