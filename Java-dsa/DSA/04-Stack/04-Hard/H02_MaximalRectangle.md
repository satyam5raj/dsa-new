# H02 · Maximal Rectangle ⭐

| | |
|---|---|
| **LeetCode** | [#85 Maximal Rectangle](https://leetcode.com/problems/maximal-rectangle/) |
| **Difficulty** | Hard |
| **Pattern** | Turn each row into a histogram + a monotonic stack |
| **Code** | [H02_MaximalRectangle.java](H02_MaximalRectangle.java) |
| **Run** | `java H02_MaximalRectangle.java` |

⭐ = a bonus; it builds directly on [Largest Rectangle in Histogram](../../01-Arrays/04-Hard/H03_LargestRectangleInHistogram.md). Read that note first.

---

## 1. The problem

Given a matrix of `'0'`/`'1'`, find the area of the **largest rectangle containing only 1s**.

```
1 0 1 0 0
1 0 1 1 1
1 1 1 1 1      → 6   (rows 1–2, columns 2–4)
1 0 0 1 0
```

| matrix | answer |
|---|---|
| the example above | 6 |
| `[[0]]` | 0 |
| `[[1]]` | 1 |
| a 3×4 grid of all 1s | 12 |

**Constraints:** `1 ≤ rows, cols ≤ 200`.

---

## 2. How to think about it

### Brute force
Try every top-left and bottom-right corner and check whether everything inside is 1: O((mn)²) rectangles × the check. Hopeless at 200 × 200.

### Reduce it to histograms
Look at the matrix row by row, treating the **current row as the ground**:

```
heights[j] = how many consecutive 1s stand on top of each other in column j,
             ending at the current row (reset to 0 when the cell is '0')
```

| after row | row | heights | largest rectangle in this histogram |
|---|---|---|---|
| 0 | `10100` | [1, 0, 1, 0, 0] | 1 |
| 1 | `10111` | [2, 0, 2, 1, 1] | 3 |
| 2 | `11111` | [3, 1, 3, 2, 2] | **6** |
| 3 | `10010` | [4, 0, 0, 3, 0] | 4 |

**Why does this work?** Every all-1 rectangle has a **bottom row**. In that row's histogram, the rectangle appears as a rectangle under the bars (the columns above it are all 1s, so the bars are at least as tall as the rectangle). So:

```
answer = max over all rows of largestRectangle(heights)
```

**The lesson:** a hard 2D problem = a known 1D problem applied once per row.

---

## 3. Java concepts used in this problem

### 3.1 `char[][]` from strings

```java
char[][] m = { "10100".toCharArray(), "10111".toCharArray(), ... };
row[j] == '1'          // compare with the character '1', not the number 1
```

### 3.2 Updating the histogram in place

```java
heights[j] = row[j] == '1' ? heights[j] + 1 : 0;
```

One `int[cols]` array is reused for every row: a 1 grows the bar and a 0 resets it. This is a rolling 1D state (like a space-optimised DP).

### 3.3 Reusing a helper

`largestRectangleArea(int[])` is exactly the monotonic-stack algorithm from [Arrays H03](../../01-Arrays/04-Hard/H03_LargestRectangleInHistogram.md) (with a sentinel 0 at the end). Building on solved problems is a key interview skill.

---

## 4. Approach 1: Brute force (the file's test reference)

### Code ([H02_MaximalRectangle.java:71-83](H02_MaximalRectangle.java#L71-L83))

```java
static int brute(char[][] m) {
    int R = m.length, C = m[0].length, best = 0;
    for (int r1 = 0; r1 < R; r1++)
        for (int c1 = 0; c1 < C; c1++)
            for (int r2 = r1; r2 < R; r2++)
                for (int c2 = c1; c2 < C; c2++) {
                    boolean all = true;
                    for (int r = r1; r <= r2 && all; r++)
                        for (int c = c1; c <= c2 && all; c++) all = m[r][c] == '1';
                    if (all) best = Math.max(best, (r2 - r1 + 1) * (c2 - c1 + 1));
                }
    return best;
}
```

Every rectangle, checked cell by cell: **O(R³C³)** in the worst case. It's only usable on tiny grids (the random tests use up to 5×5).

---

## 5. Approach 2: DP with heights + left/right boundaries (an alternative O(mn))

A classic alternative keeps, for each column j in the current row: `height[j]`, `left[j]` (the leftmost column the bar can extend to) and `right[j]`, updated row by row:

```java
static int maximalRectangleDP(char[][] m) {
    int C = m[0].length, best = 0;
    int[] h = new int[C], left = new int[C], right = new int[C];
    Arrays.fill(right, C);
    for (char[] row : m) {
        int curLeft = 0, curRight = C;
        for (int j = 0; j < C; j++) h[j] = row[j] == '1' ? h[j] + 1 : 0;
        for (int j = 0; j < C; j++) {
            if (row[j] == '1') left[j] = Math.max(left[j], curLeft);
            else { left[j] = 0; curLeft = j + 1; }
        }
        for (int j = C - 1; j >= 0; j--) {
            if (row[j] == '1') right[j] = Math.min(right[j], curRight);
            else { right[j] = C; curRight = j; }
        }
        for (int j = 0; j < C; j++) best = Math.max(best, h[j] * (right[j] - left[j]));
    }
    return best;
}
```

It's the same O(mn), without a stack, but it's harder to derive. Mention it, but present the histogram reduction.

---

## 6. Approach 3: A histogram per row + a monotonic stack (the file's solution)

### Code ([H02_MaximalRectangle.java:42-69](H02_MaximalRectangle.java#L42-L69))

```java
static int maximalRectangle(char[][] matrix) {
    if (matrix.length == 0) return 0;
    int[] heights = new int[matrix[0].length];
    int best = 0;
    for (char[] row : matrix) {
        for (int j = 0; j < row.length; j++) {
            heights[j] = row[j] == '1' ? heights[j] + 1 : 0;    // grow the bar, or reset it
        }
        best = Math.max(best, largestRectangleArea(heights));
    }
    return best;
}

// Largest Rectangle in Histogram (explained in Arrays/H03): increasing stack of indices
static int largestRectangleArea(int[] h) {
    Deque<Integer> stack = new ArrayDeque<>();
    int best = 0;
    for (int i = 0; i <= h.length; i++) {
        int cur = i == h.length ? 0 : h[i];
        while (!stack.isEmpty() && h[stack.peek()] > cur) {
            int height = h[stack.pop()];
            int left = stack.isEmpty() ? -1 : stack.peek();
            best = Math.max(best, height * (i - left - 1));
        }
        stack.push(i);
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int[] heights = new int[cols];` | the histogram, reused for every row |
| `heights[j] = row[j] == '1' ? heights[j] + 1 : 0;` | update the column bars for this row as the ground |
| `best = Math.max(best, largestRectangleArea(heights));` | the best rectangle whose bottom edge is this row |
| `largestRectangleArea` | the increasing-stack algorithm with a 0 sentinel (see Arrays H03 for the full walkthrough) |

### Dry run: the example matrix

| row | heights | best rectangle in the histogram | best so far |
|---|---|---|---|
| `10100` | [1,0,1,0,0] | 1 | 1 |
| `10111` | [2,0,2,1,1] | 3 (height 1 × width 3, columns 2–4) | 3 |
| `11111` | [3,1,3,2,2] | **6** (height 2 × width 3, columns 2–4) | 6 |
| `10010` | [4,0,0,3,0] | 4 (height 4 × width 1) | 6 |

**Return 6** ✓

For row 2, the histogram `[3, 1, 3, 2, 2]`: the bars 2..4 have heights 3, 2, 2, so a rectangle of height 2 and width 3 gives 6; the full width at height 1 gives 5; and one bar alone gives at most 3. So 6 is the best.

### Complexity
- **Time O(m · n):** an O(n) histogram pass per row.
- **Space O(n).**

---

## 7. Comparing the approaches

| | Brute force | Heights + left/right DP | Histogram + stack |
|---|---|---|---|
| Time | O(m³n³) | **O(mn)** | **O(mn)** |
| Space | O(1) | O(n) | O(n) |
| Ease of explaining | — | harder | **reuses a known problem** |

---

## 8. Edge cases and common mistakes

**Edge cases:** all 0s, all 1s, a single row or column, a 1×1 grid.

**Mistakes:**
1. Comparing `row[j] == 1` (the number) instead of `'1'` (the character).
2. Not resetting a bar to 0 when the cell is `'0'`.
3. Recomputing the heights from scratch for each row (O(m²n)).
4. Forgetting the histogram helper's sentinel, so some bars are never evaluated.

---

## 9. Interview questions and answers

**Q1. Why is the answer the max over rows of the histogram answer?**
> Any all-1 rectangle has a lowest row. Using that row as the ground, every column of the rectangle has a bar at least as tall as the rectangle, so the rectangle fits under the histogram, and the histogram algorithm finds a rectangle at least that large.

**Q2. What's the complexity, and why?**
> O(m · n): the heights update is O(n) per row, and the histogram algorithm is O(n) per row, because each index is pushed and popped once.

### Follow-up questions

**F1. Maximal Square (LC 221).**
> A simpler DP: `dp[i][j] = 1 + min(top, left, top-left)`; the answer is (the max dp)².

**F2. Count Submatrices With All Ones (LC 1504).**
> The same per-row histograms; for each row, count the rectangles ending at each column with a monotonic stack (summing the counts instead of taking a max).

**F3. The largest rectangle of equal values (not just 1s).**
> Build the "heights" per row by comparing with the cell above for equality, and reset when the value changes horizontally too (split the row into segments of the same value).

---

## 10. Running the file

```bash
java H02_MaximalRectangle.java      # 5/5 tests passed (including 200 random grids vs brute force)
```

**One-line summary:** *"Each row is the ground of a histogram (heights grow on 1s, reset on 0s); answer = the max over the rows of the largest rectangle in that histogram."*
