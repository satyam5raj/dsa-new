# A25 · Max Sum of Rectangle No Larger Than K ⭐

| | |
|---|---|
| **LeetCode** | [#363 Max Sum of Rectangle No Larger Than K](https://leetcode.com/problems/max-sum-of-rectangle-no-larger-than-k/) |
| **Difficulty** | Hard |
| **Mix of structures** | **2-D → 1-D compression** (fix two rows) + **prefix sums** + **TreeSet.ceiling** |
| **Code** | [A25_MaxSumOfRectangleNoLargerThanK.java](A25_MaxSumOfRectangleNoLargerThanK.java) |
| **Run** | `java A25_MaxSumOfRectangleNoLargerThanK.java` |

⭐ = not in `leetcode.md`, added because it combines three classic ideas: row compression, prefix sums, and an ordered set.

---

## 1. The problem

Return the largest sum of any rectangle in the matrix whose sum is **no larger than k**. A rectangle with sum ≤ k always exists.

```
matrix = [[1, 0, 1],
          [0,-2, 3]],  k = 2   →  2      the rectangle [[0,1],[-2,3]] sums to 2
matrix = [[2, 2,-1]],  k = 3   →  3      [2, 2, -1]
```

---

## 2. How to think about it

### Step 1: from 2-D to 1-D
Pick a **top** row and a **bottom** row. Add up each column between them into one array `colSum`. Every rectangle that spans exactly those rows is a **subarray** of `colSum`.

```
rows 0..1 of the example:   [1, 0, 1]
                          + [0,−2, 3]
                          = [1,−2, 4]   ← rectangles over rows 0..1 = subarrays of this
```
There are O(m²) row pairs. For a fixed top, extending `bottom` by one row just adds that row into `colSum`, so each pair costs O(n) to build.

### Step 2: max subarray sum ≤ k
Kadane's algorithm finds the maximum subarray sum, but it can't respect "≤ k". Use **prefix sums**: with `cur` = the running sum, a subarray ending here has sum `cur − p` for some earlier prefix p. We want

```
cur − p ≤ k     ⟺     p ≥ cur − k
```
and to make `cur − p` as large as possible we want the **smallest** such p. A `TreeSet` of earlier prefixes answers that with `ceiling(cur − k)` in O(log n). Insert `cur` after using it.

### Dry run: `colSum = [1, −2, 4]`, k = 2
| x | cur | `ceiling(cur − 2)` in the set | candidate | set after |
|---|---|---|---|---|
| | | | | {0} |
| 1 | 1 | ceiling(−1) = 0 | 1 − 0 = 1 | {0, 1} |
| −2 | −1 | ceiling(−3) = 0 | −1 | {−1, 0, 1} |
| 4 | 3 | ceiling(1) = 1 | 3 − 1 = **2** | {−1, 0, 1, 3} |

The best is **2**: the subarray after prefix 1, i.e. columns 1..2 = `[−2, 4]`, which is the rectangle `[[0,1],[−2,3]]` ✓.

### Which dimension goes in the O(m²) loop?
The cost is O(m² · n log n), so put the **smaller** dimension in the m² loop (transpose if there are far more rows than columns). LeetCode's follow-up asks exactly this.

---

## 3. Java concepts used in this problem

### 3.1 `TreeSet.ceiling(x)`
It returns the smallest element **≥ x**, or `null`. Its siblings: `floor` (≤), `higher` (>), `lower` (<). All are O(log n). See [A05 §3.1](A05_DataStreamAsDisjointIntervals.md#3-java-concepts-used-in-this-problem) for the TreeMap versions.

### 3.2 Why `prefixes.add(0)` first
The empty prefix lets subarrays start at index 0: `cur − 0 = cur` is the sum of everything so far.

### 3.3 A 2-D prefix sum (in the referee)
```
P[r+1][c+1] = matrix[r][c] + P[r][c+1] + P[r+1][c] − P[r][c]
sum(r1..r2, c1..c2) = P[r2+1][c2+1] − P[r1][c2+1] − P[r2+1][c1] + P[r1][c1]
```
This is inclusion–exclusion: add the big rectangle, subtract the two strips, and add back the corner that was subtracted twice.

### 3.4 Early exit
`if (best == k) return k;`: nothing can beat k itself, so stop searching.

---

## 4. The referee: every rectangle with a 2-D prefix sum

### Code ([A25_MaxSumOfRectangleNoLargerThanK.java:43-57](A25_MaxSumOfRectangleNoLargerThanK.java#L43-L57))
```java
static int maxSumSubmatrixBrute(int[][] matrix, int k) {
    int m = matrix.length, n = matrix[0].length;
    int[][] P = new int[m + 1][n + 1];                                // P[r][c] = sum of matrix[0..r-1][0..c-1]
    for (int r = 0; r < m; r++)
        for (int c = 0; c < n; c++) P[r + 1][c + 1] = matrix[r][c] + P[r][c + 1] + P[r + 1][c] - P[r][c];
    int best = Integer.MIN_VALUE;
    for (int r1 = 0; r1 < m; r1++)
        for (int r2 = r1; r2 < m; r2++)
            for (int c1 = 0; c1 < n; c1++)
                for (int c2 = c1; c2 < n; c2++) {
                    int s = P[r2 + 1][c2 + 1] - P[r1][c2 + 1] - P[r2 + 1][c1] + P[r1][c1];
                    if (s <= k) best = Math.max(best, s);
                }
    return best;
}
```

Every rectangle's sum in O(1), with O(m² n²) rectangles.

---

## 5. Main solution: row pairs + prefix sums + TreeSet

### Code ([A25_MaxSumOfRectangleNoLargerThanK.java:60-85](A25_MaxSumOfRectangleNoLargerThanK.java#L60-L85))
```java
static int maxSumSubmatrix(int[][] matrix, int k) {
    int m = matrix.length, n = matrix[0].length, best = Integer.MIN_VALUE;
    for (int top = 0; top < m; top++) {
        int[] colSum = new int[n];
        for (int bottom = top; bottom < m; bottom++) {
            for (int c = 0; c < n; c++) colSum[c] += matrix[bottom][c];   // grow the strip by one row
            best = Math.max(best, maxSubarrayAtMost(colSum, k));
            if (best == k) return k;                                  // can't do better than k itself
        }
    }
    return best;
}

/** Largest subarray sum that is <= k (Integer.MIN_VALUE if none). */
static int maxSubarrayAtMost(int[] a, int k) {
    TreeSet<Integer> prefixes = new TreeSet<>();
    prefixes.add(0);                                                  // the empty prefix: subarrays starting at 0
    int cur = 0, best = Integer.MIN_VALUE;
    for (int x : a) {
        cur += x;
        Integer p = prefixes.ceiling(cur - k);                       // smallest earlier prefix with cur - p <= k
        if (p != null) best = Math.max(best, cur - p);
        prefixes.add(cur);
    }
    return best;
}
```

### Line by line
| Line | What it does |
|---|---|
| `colSum` reset per `top` | a new strip starts at this row |
| `colSum[c] += matrix[bottom][c]` | extend the strip down by one row |
| `maxSubarrayAtMost` | the best rectangle with exactly these rows |
| `ceiling(cur − k)` | the smallest prefix that keeps the subarray sum ≤ k |
| `p != null` | some valid subarray ends here |
| `prefixes.add(cur)` | available for later subarrays |

### Complexity
- O(m²) strips × (O(n) to extend + O(n log n) for the TreeSet pass).
- **O(m² · n log n)** time, **O(n)** extra space.

---

## 6. Comparing the approaches

| | Time |
|---|---|
| Every rectangle | O(m² · n²) |
| **Row pairs + TreeSet** | O(m² · n log n) |

The file checks the main solution on 2,000 random matrices with negative values and random k.

---

## 7. Edge cases and common mistakes

**Edge cases:** all-negative matrices (the answer is negative), k smaller than every positive rectangle, a single row or column, the best rectangle being a single cell.

**Mistakes:**
1. Using Kadane's maximum and then checking ≤ k (the best sum ≤ k is often not the maximum).
2. Forgetting to insert prefix 0.
3. `floor(cur − k)` instead of `ceiling(cur − k)`.
4. Inserting `cur` before querying (it would pair the empty subarray with itself, giving sum 0 even when 0 isn't achievable as a non-empty rectangle).
5. Putting the larger dimension in the O(m²) loop.

---

## 8. Interview questions and answers

**Q1. How do you reduce the 2-D problem to 1-D?**
> Fix a top and bottom row and sum each column between them; rectangles over those rows are exactly subarrays of that column-sum array.

**Q2. How do you find the best subarray sum ≤ k?**
> Keep earlier prefix sums in a TreeSet. For the current prefix `cur`, the best earlier prefix is the smallest one ≥ `cur − k`, which is `ceiling`. The candidate is `cur − ceiling`.

**Q3. Why can't Kadane do it?**
> Kadane greedily extends or restarts to maximise the sum; it has no way to reject sums above k while keeping the best one below it.

### Follow-up questions

**F1. "Far more rows than columns."**
> Iterate over **column** pairs instead (the smaller dimension), and compress rows.

**F2. "Count rectangles with sum exactly k."**
> The same compression, with a HashMap of prefix counts instead of a TreeSet ([Subarray Sum Equals K](../../01-Arrays/03-Medium/M07_SubarraySumEqualsK.md)): O(m² · n).

**F3. "Maximum sum rectangle with no limit."**
> The same compression plus Kadane: O(m² · n).

---

## 9. Running the file

```bash
java A25_MaxSumOfRectangleNoLargerThanK.java     # 6/6 tests passed (2 examples, all negative, small k, 1-D helper, 2,000 random matrices)
```

**One-line summary:** *"For every pair of rows, compress the strip into column sums, then find the best subarray sum ≤ k by keeping earlier prefix sums in a TreeSet and taking ceiling(cur − k)."*
