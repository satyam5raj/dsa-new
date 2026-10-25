# M08 · Rotate Image

| | |
|---|---|
| **LeetCode** | [#48 Rotate Image](https://leetcode.com/problems/rotate-image/) |
| **Difficulty** | Medium |
| **Pattern** | Matrix rotation = transpose + reverse each row (in place) |
| **Code** | [M08_RotateImage.java](M08_RotateImage.java) |
| **Run** | `java M08_RotateImage.java` |

---

## 1. The problem

Rotate an **n × n** matrix **90° clockwise**, **in place** (no second matrix).

```
1 2 3        7 4 1
4 5 6   →    8 5 2
7 8 9        9 6 3
```

```
 5  1  9 11        15 13  2  5
 2  4  8 10   →    14  3  4  1
13  3  6  7        12  6  8  9
15 14 12 16        16  7 10 11
```

**Constraints:** `1 ≤ n ≤ 20`.

---

## 2. How to think about it

1. **With a second matrix it's easy.** Where does cell (i, j) go? After a clockwise rotation, **row i becomes column n−1−i**, and column j becomes row j:

   ```
   rotated[j][n − 1 − i] = m[i][j]
   ```

   Check with the 3×3 example: the 1 at (0,0) → (0, 2) ✓, the 7 at (2,0) → (0, 0) ✓.
2. **In place:** a rotation can be built from two simple **reflections**, each easy to do in place:
   - **transpose** (flip across the main diagonal): `m[i][j] ↔ m[j][i]`
   - **reverse each row** (flip left ↔ right)

```
1 2 3   transpose   1 4 7   reverse rows   7 4 1
4 5 6   ───────→    2 5 8   ──────────→    8 5 2
7 8 9               3 6 9                  9 6 3
```

Why does it work? The transpose sends (i, j) → (j, i). Reversing the row sends (j, i) → (j, n−1−i). Combined: (i, j) → (j, n−1−i), which is exactly the clockwise formula ✓.

**Transpose trap:** only swap cells **above** the diagonal (`j > i`). If you looped over all (i, j), every pair would be swapped **twice**, and the matrix would end up unchanged.

---

## 3. Java concepts used in this problem

### 3.1 2D arrays: `int[][]`

```java
int[][] m = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
m.length       // 3: the number of ROWS
m[0].length    // 3: the number of COLUMNS (the length of row 0)
m[1][2]        // 6: row 1, column 2
```

In Java, a 2D array is really an **array of arrays**: `m` holds 3 references, each pointing to a separate `int[]` row:

```
m ──► [ ref0, ref1, ref2 ]
         │     │     └──► [7, 8, 9]
         │     └────────► [4, 5, 6]
         └──────────────► [1, 2, 3]
```

So rows can even have different lengths ("jagged" arrays, as in Pascal's triangle E15).

### 3.2 For-each over rows: `for (int[] row : m)`

```java
for (int[] row : m) {     // row is a REFERENCE to the actual row array
    row[0] = 99;          // this modifies m itself
}
```

`row` isn't a copy. It points at the same inner array as `m[i]`, so changing `row[l]` changes the matrix. (Contrast this with `for (int x : arr)`, where `x` **is** a copy of a primitive, so assigning to it changes nothing.)

### 3.3 A `for` loop with two variables

```java
for (int l = 0, r = n - 1; l < r; l++, r--) { ... }
```

You can declare several variables of the same type in the init part and update several in the update part (separated by commas). It's ideal for two pointers moving toward each other.

### 3.4 Printing 2D arrays: `Arrays.deepToString`

`Arrays.toString(m)` would print the **references** (`[[I@1b6d3586, ...]`). `Arrays.deepToString(m)` prints the contents: `[[7, 4, 1], [8, 5, 2], [9, 6, 3]]`. The test helper uses it.

---

## 4. Approach 1: Extra matrix (not in place)

```java
static int[][] rotateCopy(int[][] m) {
    int n = m.length;
    int[][] r = new int[n][n];
    for (int i = 0; i < n; i++)
        for (int j = 0; j < n; j++)
            r[j][n - 1 - i] = m[i][j];
    return r;
}
```

**Dry run** for the 3×3 example (a few cells):

| (i, j) | value | goes to (j, n−1−i) |
|---|---|---|
| (0, 0) | 1 | (0, 2) |
| (0, 1) | 2 | (1, 2) |
| (1, 0) | 4 | (0, 1) |
| (2, 0) | 7 | (0, 0) |
| (2, 2) | 9 | (2, 0) |

**O(n²) time, O(n²) space**, which breaks the in-place rule.

---

## 5. Approach 2: Transpose + reverse each row (the file's solution)

### Code ([M08_RotateImage.java:41-59](M08_RotateImage.java#L41-L59))

```java
static void rotate(int[][] m) {
    int n = m.length;
    // 1. transpose
    for (int i = 0; i < n; i++) {
        for (int j = i + 1; j < n; j++) {         // j > i: upper triangle only
            int t = m[i][j];
            m[i][j] = m[j][i];
            m[j][i] = t;
        }
    }
    // 2. reverse each row
    for (int[] row : m) {
        for (int l = 0, r = n - 1; l < r; l++, r--) {
            int t = row[l];
            row[l] = row[r];
            row[r] = t;
        }
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `for (int i = 0; i < n; i++)` | each row |
| `for (int j = i + 1; j < n; j++)` | only the columns **right of the diagonal**, so each off-diagonal pair is swapped exactly once. The diagonal (i == j) stays put |
| `t = m[i][j]; m[i][j] = m[j][i]; m[j][i] = t;` | swap a cell with its mirror image across the diagonal |
| `for (int[] row : m)` | each row (by reference) |
| `for (int l = 0, r = n - 1; l < r; l++, r--)` | two pointers from the ends of the row |
| swap `row[l]`, `row[r]` | reverse the row in place |

### Dry run 1: the 3×3 matrix

**Transpose** (the swaps, in order):

| (i, j) | swap | matrix after |
|---|---|---|
| (0,1) | 2 ↔ 4 | `[[1,4,3],[2,5,6],[7,8,9]]` |
| (0,2) | 3 ↔ 7 | `[[1,4,7],[2,5,6],[3,8,9]]` |
| (1,2) | 6 ↔ 8 | `[[1,4,7],[2,5,8],[3,6,9]]` |

**Reverse each row:**

| row before | after |
|---|---|
| `[1,4,7]` | `[7,4,1]` |
| `[2,5,8]` | `[8,5,2]` |
| `[3,6,9]` | `[9,6,3]` |

**Result** `[[7,4,1],[8,5,2],[9,6,3]]` ✓

### Dry run 2: the 4×4 matrix

```
start:            after transpose:     after reversing rows:
 5  1  9 11        5  2 13 15          15 13  2  5
 2  4  8 10        1  4  3 14          14  3  4  1
13  3  6  7        9  8  6 12          12  6  8  9
15 14 12 16       11 10  7 16          16  7 10 11   ✓
```

### Dry run 3: rotating 4 times returns the original
The test `for (i < 4) rotate(m)` on `[[1,2],[3,4]]`: `[[3,1],[4,2]]` → `[[4,3],[2,1]]` → `[[2,4],[1,3]]` → `[[1,2],[3,4]]` ✓. Four quarter-turns make a full turn.

### Complexity
- **Time O(n²):** each cell is touched a constant number of times. You can't do better, since every cell moves (except the centre of an odd-sized matrix).
- **Space O(1).**

---

## 6. (Bonus) Approach 3: Rotate layer by layer (four-way swap)

Process the matrix as concentric **rings**. In each ring, every group of 4 cells rotates in a cycle: top → right → bottom → left → top.

```java
static void rotateLayers(int[][] m) {
    int n = m.length;
    for (int layer = 0; layer < n / 2; layer++) {
        int first = layer, last = n - 1 - layer;
        for (int i = first; i < last; i++) {
            int offset = i - first;
            int top = m[first][i];                                  // save top
            m[first][i] = m[last - offset][first];                  // left   → top
            m[last - offset][first] = m[last][last - offset];       // bottom → left
            m[last][last - offset] = m[i][last];                    // right  → bottom
            m[i][last] = top;                                       // top    → right
        }
    }
}
```

- Each cell is written **once**, so it does fewer writes than transpose + reverse (which writes most cells twice).
- It's much easier to get an index wrong. Present transpose + reverse first, and mention this as the "minimum writes" variant.

---

## 7. Comparing the approaches

| | Extra matrix | Transpose + reverse | Layer by layer |
|---|---|---|---|
| Time | O(n²) | O(n²) | O(n²) |
| Extra space | O(n²) | **O(1)** | **O(1)** |
| Writes per cell | 1 | about 2 | **1** |
| Bug risk | low | **low** | high |

### Other rotations (memorise these)

| Rotation | Recipe |
|---|---|
| 90° clockwise | transpose, then reverse each **row** |
| 90° counter-clockwise | transpose, then reverse each **column** (or reverse each row first, then transpose) |
| 180° | reverse the order of the rows, then reverse each row |

---

## 8. Edge cases and common mistakes

**Edge cases:** 1×1, 2×2, odd n (the centre cell stays), even n.

**Mistakes:**
1. Transposing over the full matrix (`j` from 0), which swaps everything twice: no change.
2. Reversing the columns instead of the rows (that gives a counter-clockwise rotation).
3. Trying to apply this to a **non-square** m × n matrix in place. That's impossible in place: the shape changes to n × m, so you need a new matrix.
4. Using `Arrays.toString` to debug a 2D array (it prints references).

---

## 9. Interview questions and answers

**Q1. Why does transpose + reverse rows equal a clockwise rotation?**
> The transpose maps (i, j) → (j, i); reversing a row maps (r, c) → (r, n−1−c). Composed: (i, j) → (j, n−1−i), the clockwise rotation formula.

**Q2. Why loop `j` from `i + 1` in the transpose?**
> To swap each pair across the diagonal exactly once. Including j < i would undo the swaps, and j == i is a no-op.

**Q3. Can you rotate a non-square matrix in place?**
> No: an m × n matrix becomes n × m, so the memory layout changes. You'd allocate `new int[n][m]` and use `r[j][m−1−i] = a[i][j]`.

### Follow-up questions

**F1. Counter-clockwise rotation.**
> Transpose, then reverse each column; or reverse each row, then transpose.

**F2. Spiral order traversal.** → [M10 Spiral Matrix](M10_SpiralMatrix.md)
> The same "layer by layer" thinking with four boundaries.

**F3. Determine whether a matrix can be obtained by rotating another (LC 1886).**
> Compare after 0, 1, 2 and 3 rotations.

**F4. Rotate a matrix by k × 90°.**
> Rotate `k % 4` times, or apply the direct recipe for 90/180/270.

**F5. An image stored as a 1D array (row-major).**
> Index `(i, j)` = `i × n + j`. The same math applies with index arithmetic.

---

## 10. Running the file

```bash
java M08_RotateImage.java      # 4/4 tests passed
```

**One-line summary:** *"Clockwise 90° = transpose (swap above the diagonal only) + reverse every row."*
