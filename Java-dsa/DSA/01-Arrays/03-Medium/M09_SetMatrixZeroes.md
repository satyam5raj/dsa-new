# M09 · Set Matrix Zeroes

| | |
|---|---|
| **LeetCode** | [#73 Set Matrix Zeroes](https://leetcode.com/problems/set-matrix-zeroes/) |
| **Difficulty** | Medium |
| **Pattern** | Use the first row/column as marker storage (O(1) extra space) |
| **Code** | [M09_SetMatrixZeroes.java](M09_SetMatrixZeroes.java) |
| **Run** | `java M09_SetMatrixZeroes.java` |

---

## 1. The problem

If a cell is `0`, set its **entire row and column** to 0. Do it **in place**.

```
1 1 1        1 0 1
1 0 1   →    0 0 0
1 1 1        1 0 1
```

```
0 1 2 0        0 0 0 0
3 4 5 2   →    0 4 5 0
1 3 1 5        0 3 1 0
```

**Constraints:** `1 ≤ m, n ≤ 200`. Follow-up: *O(m·n) space is a bad idea, O(m + n) is better, but can you do **O(1)**?*

---

## 2. How to think about it

### The trap: zeroing while scanning
If you zero a row and column **as soon as** you find a 0, the new zeros look like original zeros later in the scan, and they wipe out even more rows and columns. Eventually the whole matrix becomes 0. So you must **first record** which rows and columns to clear, **then** clear them.

### Three levels
1. **Copy the matrix** and scan the copy: O(m·n) space.
2. **Two boolean arrays** `zeroRow[m]` and `zeroCol[n]`: O(m + n) space.
3. **O(1) space:** store those flags **inside the matrix itself**:
   - `matrix[i][0] = 0` means "clear row i" (column 0 acts as `zeroRow[]`)
   - `matrix[0][j] = 0` means "clear column j" (row 0 acts as `zeroCol[]`)

   **Problem:** row 0 and column 0 also hold real data. Were they **themselves** supposed to be cleared? Save that in two booleans **before** using them as markers: `firstRowZero` and `firstColZero`.

### The order of steps (very important)
1. Remember whether row 0 / column 0 contain a zero.
2. Scan the **inner** cells (i ≥ 1, j ≥ 1); for each zero, write the markers into row 0 and column 0.
3. Clear the **inner** cells using the markers.
4. **Last**, clear row 0 / column 0 if their flags say so. If you did this earlier, you'd destroy the markers that step 3 needs.

---

## 3. Java concepts used in this problem

### 3.1 Rows and columns of a 2D array

```java
int m = matrix.length;       // rows
int n = matrix[0].length;    // columns (assumes at least one row)
```

See [M08](M08_RotateImage.md) for how 2D arrays are stored (an array of row arrays).

### 3.2 `boolean[]` default values

`new boolean[m]` is all `false` at first, which is exactly "nothing marked yet".

### 3.3 One-line `if` + `for`

```java
if (firstRowZero) for (int j = 0; j < n; j++) matrix[0][j] = 0;
```

A `for` is a single statement, so it can be the body of an `if` without braces. It's compact, but use braces when it gets longer.

### 3.4 "Reusing the input as storage"

This is the same trick as E16 (sign marking): when the problem demands O(1) extra space, look for **parts of the input you can safely overwrite** to store your bookkeeping, and make sure you don't lose information you'll need later (here, via the two booleans).

---

## 4. Approach 1: Two boolean arrays, O(m + n) space

```java
static void setZeroesExtra(int[][] matrix) {
    int m = matrix.length, n = matrix[0].length;
    boolean[] zeroRow = new boolean[m], zeroCol = new boolean[n];
    for (int i = 0; i < m; i++)
        for (int j = 0; j < n; j++)
            if (matrix[i][j] == 0) { zeroRow[i] = true; zeroCol[j] = true; }   // 1. record
    for (int i = 0; i < m; i++)
        for (int j = 0; j < n; j++)
            if (zeroRow[i] || zeroCol[j]) matrix[i][j] = 0;                   // 2. clear
}
```

| Step | What it does |
|---|---|
| record | mark every row and column that contains an original 0 |
| clear | a cell becomes 0 if its row **or** its column is marked |

**Dry run** on the first example: the zero at (1,1) → zeroRow = [F, T, F], zeroCol = [F, T, F]. Clearing: row 1 entirely, plus column 1 → `[[1,0,1],[0,0,0],[1,0,1]]` ✓

**O(m·n) time, O(m + n) space.** It's the simplest correct version. Start here in an interview.

---

## 5. Approach 2: Markers in the first row/column, O(1) space (the file's solution)

### Code ([M09_SetMatrixZeroes.java:43-67](M09_SetMatrixZeroes.java#L43-L67))

```java
static void setZeroes(int[][] matrix) {
    int m = matrix.length, n = matrix[0].length;
    boolean firstRowZero = false, firstColZero = false;
    for (int j = 0; j < n; j++) if (matrix[0][j] == 0) firstRowZero = true;
    for (int i = 0; i < m; i++) if (matrix[i][0] == 0) firstColZero = true;

    // record markers in row 0 / column 0
    for (int i = 1; i < m; i++) {
        for (int j = 1; j < n; j++) {
            if (matrix[i][j] == 0) {
                matrix[i][0] = 0;
                matrix[0][j] = 0;
            }
        }
    }
    // clear the inner cells using the markers
    for (int i = 1; i < m; i++) {
        for (int j = 1; j < n; j++) {
            if (matrix[i][0] == 0 || matrix[0][j] == 0) matrix[i][j] = 0;
        }
    }
    // finally the first row / column themselves
    if (firstRowZero) for (int j = 0; j < n; j++) matrix[0][j] = 0;
    if (firstColZero) for (int i = 0; i < m; i++) matrix[i][0] = 0;
}
```

### Line by line

| Line | What it does |
|---|---|
| `firstRowZero` loop | does row 0 **originally** contain a zero? (We must know before overwriting it with markers) |
| `firstColZero` loop | the same for column 0 |
| marking loop (i, j ≥ 1) | an inner zero at (i, j) → mark its row in column 0 (`matrix[i][0] = 0`) and its column in row 0 (`matrix[0][j] = 0`) |
| clearing loop (i, j ≥ 1) | an inner cell becomes 0 if its row marker or its column marker is 0 |
| `if (firstRowZero) ...` | now it's safe to clear row 0 (the markers are no longer needed) |
| `if (firstColZero) ...` | and column 0 |

**A subtle bonus:** an **original** zero in row 0 at column j already acts as a correct "clear column j" marker, because that column must be cleared anyway. You'll see this in dry run 2.

### Dry run 1: the 3×3 with a centre zero

```
1 1 1
1 0 1
1 1 1
```

1. firstRowZero = false, firstColZero = false
2. Marking: (1,1) is 0 → `matrix[1][0] = 0`, `matrix[0][1] = 0`:
   ```
   1 0 1
   0 0 1
   1 1 1
   ```
3. Clearing the inner cells:

| cell | row marker m[i][0] | col marker m[0][j] | becomes 0? |
|---|---|---|---|
| (1,1) | 0 | 0 | yes (already 0) |
| (1,2) | 0 | 1 | **yes** |
| (2,1) | 1 | 0 | **yes** |
| (2,2) | 1 | 1 | no |

   ```
   1 0 1
   0 0 0
   1 0 1
   ```
4. Both flags are false, so row 0 and column 0 aren't touched beyond their markers.

**Result** `[[1,0,1],[0,0,0],[1,0,1]]` ✓

### Dry run 2: zeros in the first row

```
0 1 2 0
3 4 5 2
1 3 1 5
```

1. Row 0 has zeros → **firstRowZero = true**. Column 0 has `matrix[0][0] = 0` → **firstColZero = true**.
2. Marking (the inner cells i ≥ 1, j ≥ 1): **no inner zeros**, so nothing changes.
3. Clearing the inner cells: the column markers in row 0 are `[_, 1, 2, 0]`, so **column 3** is marked (by the original zero at (0,3)):
   ```
   0 1 2 0
   3 4 5 0
   1 3 1 0
   ```
4. firstRowZero → row 0 becomes all 0. firstColZero → column 0 becomes all 0:
   ```
   0 0 0 0
   0 4 5 0
   0 3 1 0
   ```

**Result** ✓

### Dry run 3: a single row `[[1, 0, 3]]`
firstRowZero = true; there are no inner rows (m = 1), so the marking and clearing loops do nothing; then row 0 is zeroed → `[[0, 0, 0]]` ✓

### Complexity
- **Time O(m·n):** a few passes over the matrix.
- **Space O(1):** two booleans.

### Why two booleans instead of one?
`matrix[0][0]` sits in **both** row 0 and column 0, so it can't mark both on its own. The file keeps one boolean for each. (A common variant uses `matrix[0][0]` for row 0 and a single extra boolean for column 0. That works, but it's easier to get wrong.)

---

## 6. Comparing the approaches

| | Copy | Two boolean arrays | Markers in row/col 0 |
|---|---|---|---|
| Time | O(mn) | O(mn) | O(mn) |
| Extra space | O(mn) | O(m + n) | **O(1)** |
| Bug risk | low | **low** | medium (ordering!) |

---

## 7. Edge cases and common mistakes

**Edge cases:** a single row or column, a zero at (0,0), zeros only in the first row/column, no zeros, all zeros.

**Mistakes:**
1. Zeroing rows/columns during the first scan (the cascade).
2. Clearing row 0 / column 0 **before** the inner cells, which destroys the markers.
3. Starting the marking and clearing loops at 0 instead of 1, which mixes markers with data.
4. Forgetting to record the first row/column's original state.

---

## 8. Interview questions and answers

**Q1. Why can't you zero rows and columns immediately?**
> The new zeros are indistinguishable from the original ones, so they'd trigger more clearing. You have to separate "detect" from "apply".

**Q2. How do you achieve O(1) space?**
> Use the first row and first column as the marker arrays. Before that, remember in two booleans whether they originally contained zeros. Clear the inner cells using the markers, and clear the first row/column last.

**Q3. Why must the first row and column be processed last?**
> They hold the markers for every other row and column. Clearing them earlier would erase that information.

### Follow-up questions

**F1. Game of Life (LC 289): update every cell simultaneously, in place.**
> The same "don't overwrite information you still need" issue. Encode both states in one cell: e.g. 2 = "was alive, becomes dead", 3 = "was dead, becomes alive", then normalise in a second pass.

**F2. The matrix is huge and sparse (few zeros).**
> Store only the coordinates of the zeros (a list of rows and columns), then clear. O(z) memory for z zeros.

**F3. Return a new matrix instead of modifying it.**
> Use the two boolean arrays and write into a fresh matrix. That's simpler, and there's no ordering subtlety.

**F4. Count how many cells would remain non-zero.**
> No clearing needed: `(m − zeroRows) × (n − zeroCols)`, where zeroRows/zeroCols count the distinct rows and columns containing a zero.

---

## 9. Running the file

```bash
java M09_SetMatrixZeroes.java      # 4/4 tests passed
```

**One-line summary:** *"Record first, clear later; to use O(1) space, store the flags in row 0 and column 0 and clear those last."*
