# E15 · Pascal's Triangle (+ Pascal's Triangle II)

| | |
|---|---|
| **LeetCode** | [#118 Pascal's Triangle](https://leetcode.com/problems/pascals-triangle/), [#119 Pascal's Triangle II](https://leetcode.com/problems/pascals-triangle-ii/) |
| **Difficulty** | Easy |
| **Pattern** | Build each row from the previous one (simple dynamic programming) |
| **Code** | [E15_PascalsTriangle.java](E15_PascalsTriangle.java) |
| **Run** | `java E15_PascalsTriangle.java` |

---

## 1. The problem

**#118:** return the first `numRows` rows of Pascal's triangle.

```
row 0:          1
row 1:         1 1
row 2:        1 2 1
row 3:       1 3 3 1
row 4:      1 4 6 4 1
```

Each number is the **sum of the two numbers directly above it**. The edges are always 1.

`generate(5)` → `[[1], [1,1], [1,2,1], [1,3,3,1], [1,4,6,4,1]]`

**#119:** return **only row `k`** (0-indexed), using **O(k)** extra space.
`getRow(3)` → `[1, 3, 3, 1]`, `getRow(5)` → `[1, 5, 10, 10, 5, 1]`

**Constraints:** `1 ≤ numRows ≤ 30`, `0 ≤ rowIndex ≤ 33`.

---

## 2. How to think about it

Line the rows up on the left, so the "two numbers above" become easy to index:

```
          j=0 j=1 j=2 j=3 j=4
row 0:     1
row 1:     1   1
row 2:     1   2   1
row 3:     1   3   3   1
row 4:     1   4   6   4   1
```

The number at `(i, j)` = the number at `(i−1, j−1)` + the number at `(i−1, j)`: the one **above-left** and the one **directly above**.

```
row[j] = prev[j-1] + prev[j]      for 0 < j < i
row[0] = row[i] = 1
```

That's **dynamic programming** in its simplest form: each answer is built from answers you already computed.

For #119 we only need one row, so we keep **one array** and update it in place for each new row. The catch: we must update it **from right to left** (explained below).

---

## 3. Java concepts used in this problem

### 3.1 Nested generic lists: `List<List<Integer>>`

```java
List<List<Integer>> triangle = new ArrayList<>();
```

Read it from the inside out: `List<Integer>` is a single row; `List<List<Integer>>` is a list of rows.

```java
triangle.get(2)          // the row [1, 2, 1]
triangle.get(2).get(1)   // 2
triangle.add(row);       // append a whole row
```

**Why lists instead of `int[][]`?** Each row has a different length (row i has i+1 numbers), and LeetCode's method signature requires a `List`. Java 2D arrays can also be "jagged" (`int[][] t = new int[n][]; t[i] = new int[i + 1];`), so both work.

### 3.2 Printing a list

`List.toString()` prints `[1, 2, 1]`, and nested lists print as `[[1], [1, 1], ...]`. That's why the tests in this file compare against strings like `"[1, 3, 3, 1]"`: the test helper turns the result into text with `String.valueOf(list)`.

### 3.3 Converting `int[]` to `List<Integer>`

```java
List<Integer> out = new ArrayList<>();
for (int x : row) out.add(x);   // autoboxing int → Integer
```

`Arrays.asList(row)` does **not** work for `int[]`: it would create a `List<int[]>` with one element. One-liner alternative: `Arrays.stream(row).boxed().toList()`.

### 3.4 Counting down in a nested loop

```java
for (int j = i; j > 0; j--)   // j = i, i-1, ..., 1   (stops before 0)
```

---

## 4. Approach 1 (#118): Build each row from the previous one

### Code ([E15_PascalsTriangle.java:39-54](E15_PascalsTriangle.java#L39-L54))

```java
static List<List<Integer>> generate(int numRows) {
    List<List<Integer>> triangle = new ArrayList<>();
    for (int i = 0; i < numRows; i++) {
        List<Integer> row = new ArrayList<>();
        for (int j = 0; j <= i; j++) {
            if (j == 0 || j == i) {
                row.add(1);                                    // the edges
            } else {
                List<Integer> prev = triangle.get(i - 1);
                row.add(prev.get(j - 1) + prev.get(j));        // the two numbers above
            }
        }
        triangle.add(row);
    }
    return triangle;
}
```

### Line by line

| Line | What it does |
|---|---|
| `List<List<Integer>> triangle = new ArrayList<>();` | the result: a list of rows |
| `for (int i = 0; i < numRows; i++)` | build row 0, 1, …, numRows−1 |
| `List<Integer> row = new ArrayList<>();` | a fresh row |
| `for (int j = 0; j <= i; j++)` | row i has **i+1** numbers (indices 0..i), hence `<=` |
| `if (j == 0 \|\| j == i) row.add(1);` | the first and last numbers of each row are 1 |
| `List<Integer> prev = triangle.get(i - 1);` | the row above. Safe: we only get here when 0 < j < i, so i ≥ 2 |
| `row.add(prev.get(j - 1) + prev.get(j));` | above-left + above. The two `Integer`s are unboxed, added, and the result boxed again |
| `triangle.add(row);` | the row is finished; append it |

### Dry run: `generate(5)`

| i | j | edge? | computation | row so far |
|---|---|---|---|---|
| 0 | 0 | yes | 1 | `[1]` |
| 1 | 0, 1 | yes, yes | 1, 1 | `[1, 1]` |
| 2 | 0 | yes | 1 | `[1]` |
| 2 | 1 | no | prev[0]+prev[1] = 1+1 = 2 | `[1, 2]` |
| 2 | 2 | yes | 1 | `[1, 2, 1]` |
| 3 | 1 | no | 1+2 = 3 | `[1, 3]` |
| 3 | 2 | no | 2+1 = 3 | `[1, 3, 3]` |
| 3 | 3 | yes | 1 | `[1, 3, 3, 1]` |
| 4 | 1 | no | 1+3 = 4 | |
| 4 | 2 | no | 3+3 = 6 | |
| 4 | 3 | no | 3+1 = 4 | `[1, 4, 6, 4, 1]` |

Result: `[[1], [1,1], [1,2,1], [1,3,3,1], [1,4,6,4,1]]` ✓

### Complexity
- **Time O(numRows²):** the total count of numbers is 1 + 2 + … + n = n(n+1)/2. You can't do better, because that's the size of the output.
- **Space O(numRows²)** for the output.

---

## 5. Approach 2 (#119): One row, updated in place from right to left

### Code ([E15_PascalsTriangle.java:57-68](E15_PascalsTriangle.java#L57-L68))

```java
static List<Integer> getRow(int rowIndex) {
    int[] row = new int[rowIndex + 1];
    row[0] = 1;
    for (int i = 1; i <= rowIndex; i++) {
        for (int j = i; j > 0; j--) {          // RIGHT to LEFT
            row[j] += row[j - 1];
        }
    }
    List<Integer> out = new ArrayList<>();
    for (int x : row) out.add(x);
    return out;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int[] row = new int[rowIndex + 1];` | room for the final row (k+1 numbers), all zeros at first |
| `row[0] = 1;` | row 0 is `[1]`. The zeros to the right act as "nothing there yet" |
| `for (int i = 1; i <= rowIndex; i++)` | turn row i−1 into row i, in place |
| `for (int j = i; j > 0; j--)` | update positions i down to 1. Position 0 stays 1 forever |
| `row[j] += row[j - 1];` | new `row[j]` = old `row[j]` (the number above) + old `row[j-1]` (above-left) |
| convert to `List<Integer>` | the required return type |

**Why does `row[i]` become 1 automatically?** Before step i, `row[i]` is 0 (never touched), so `row[i] = 0 + row[i-1] = 0 + 1 = 1`, because `row[i-1]` was the previous row's last 1.

### Why RIGHT to LEFT? (the heart of this problem)

`row[j] += row[j-1]` needs the **old** (previous row's) value of `row[j-1]`.
- Going **right to left**, when we update j, position j−1 has **not been updated yet**, so it's still the old value ✓
- Going **left to right**, position j−1 was **already overwritten** with the new row's value ✗

Example, turning row 2 `[1,2,1,0]` into row 3:

| order | steps | result |
|---|---|---|
| **right → left** ✓ | j=3: 0+1 = 1 → `[1,2,1,1]`; j=2: 1+2 = 3 → `[1,2,3,1]`; j=1: 2+1 = 3 → `[1,3,3,1]` | `[1,3,3,1]` ✓ |
| left → right ✗ | j=1: 2+1 = 3 → `[1,3,1,0]`; j=2: 1+**3** = 4 (used the NEW 3) → `[1,3,4,0]`; j=3: 0+4 = 4 | `[1,3,4,4]` ✗ |

This "iterate backwards so you read old values" trick is exactly how the **0/1 knapsack** 1D DP works later (15-Dynamic-Programming).

### Dry run: `getRow(4)`

| after i | row |
|---|---|
| start | `[1, 0, 0, 0, 0]` |
| 1 | j=1: 0+1=1 → `[1, 1, 0, 0, 0]` |
| 2 | j=2: 0+1=1, j=1: 1+1=2 → `[1, 2, 1, 0, 0]` |
| 3 | j=3: 1, j=2: 1+2=3, j=1: 2+1=3 → `[1, 3, 3, 1, 0]` |
| 4 | j=4: 1, j=3: 1+3=4, j=2: 3+3=6, j=1: 3+1=4 → `[1, 4, 6, 4, 1]` ✓ |

### Complexity
- **Time O(k²):** about k²/2 additions.
- **Space O(k):** one array.

---

## 6. (Bonus) Approach 3 (#119): Direct formula with binomial coefficients

Row k consists of the **binomial coefficients** `C(k, 0), C(k, 1), …, C(k, k)`, where C(k, j) is "k choose j". The next one can be computed from the previous one:

```
C(k, j) = C(k, j-1) × (k − j + 1) / j
```

```java
static List<Integer> getRowFormula(int k) {
    List<Integer> row = new ArrayList<>();
    long c = 1;                                  // C(k, 0)
    for (int j = 0; j <= k; j++) {
        row.add((int) c);
        c = c * (k - j) / (j + 1);               // C(k, j+1)
    }
    return row;
}
```

- Multiply **before** dividing so the division is always exact.
- Use `long` because the product `c × (k − j)` can exceed `int` before the division (for k = 33, the middle coefficient is about 1.1 × 10⁹, and multiplying it pushes past 2³¹).
- **O(k) time, O(k) space.**

**Dry run** k = 4: c = 1 → 1×4/1 = 4 → 4×3/2 = 6 → 6×2/3 = 4 → 4×1/4 = 1 → `[1, 4, 6, 4, 1]` ✓

---

## 7. Comparing the approaches

| | Build all rows (#118) | In place, right→left (#119) | Formula (#119) |
|---|---|---|---|
| Time | O(n²) | O(k²) | **O(k)** |
| Space | O(n²) (it's the output) | **O(k)** | O(k) |

---

## 8. Edge cases and common mistakes

**Edge cases:** `numRows = 1` → `[[1]]`, `rowIndex = 0` → `[1]`, the largest inputs (overflow check: row 33 still fits in `int`).

**Mistakes:**
1. `j < i` instead of `j <= i` in #118, which loses the last 1 of each row.
2. Updating left to right in #119.
3. Using `int` in the formula approach, which overflows during the multiplication.
4. Reusing the same `row` object for every row in #118 (creating it outside the loop), which makes all rows the same list.

---

## 9. Interview questions and answers

**Q1. Why is this called dynamic programming?**
> Each value is built from smaller subproblems (the row above) that were already solved, and each one is computed only once. With no overlapping recomputation, it's DP in its simplest "table filling" form.

**Q2. Why must the in-place update go from right to left?**
> The update needs the previous row's value at j−1. Going right to left, index j−1 hasn't been overwritten yet when j is updated. Going left to right, it has.

**Q3. What's the connection to combinatorics?**
> Row k, position j is C(k, j), the number of ways to choose j items from k. The recurrence `C(k,j) = C(k−1,j−1) + C(k−1,j)` (Pascal's rule) is exactly "the sum of the two above". It also gives the coefficients of (a + b)ᵏ.

### Follow-up questions

**F1. Return the value at (row, col) only.**
> `C(row, col)` via the multiplicative formula, O(col) time, O(1) space. Use C(n, k) = C(n, n−k) to iterate over the smaller side.

**F2. Count the paths in a grid moving only right/down (LC 62 Unique Paths).** → [15-DP M14](../../15-Dynamic-Programming/03-Medium/M14_UniquePaths.md)
> The same addition recurrence (`paths[i][j] = paths[i−1][j] + paths[i][j−1]`), or directly `C(m+n−2, m−1)`.

**F3. Pascal's triangle mod p, or huge rows.**
> Compute with `% p` at every step (the additive version is safe). For C(n, k) mod a prime with huge n, use **Lucas' theorem** or precomputed factorials with modular inverses.

**F4. Minimum path sum from top to bottom of a number triangle (LC 120).** → [15-DP M17 Triangle](../../15-Dynamic-Programming/03-Medium/M17_Triangle.md)
> A similar row-by-row DP, usually done bottom-up: `dp[j] = tri[i][j] + min(dp[j], dp[j+1])`.

---

## 10. Running the file

```bash
java E15_PascalsTriangle.java      # 5/5 tests passed
```

**One-line summary:** *"Each number = above-left + above; for one row in O(k) space, update in place right to left."*
