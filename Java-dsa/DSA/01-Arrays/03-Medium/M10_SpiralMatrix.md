# M10 · Spiral Matrix

| | |
|---|---|
| **LeetCode** | [#54 Spiral Matrix](https://leetcode.com/problems/spiral-matrix/) |
| **Difficulty** | Medium |
| **Pattern** | Shrinking boundaries (simulation) |
| **Code** | [M10_SpiralMatrix.java](M10_SpiralMatrix.java) |
| **Run** | `java M10_SpiralMatrix.java` |

---

## 1. The problem

Return all the elements of an m × n matrix in **spiral order**: clockwise, starting at the top-left corner.

```
 1  2  3  4
 5  6  7  8      →  [1, 2, 3, 4, 8, 12, 11, 10, 9, 5, 6, 7]
 9 10 11 12
```

| matrix | answer |
|---|---|
| 3×3 `1..9` | `[1,2,3,6,9,8,7,4,5]` |
| 3×4 above | `[1,2,3,4,8,12,11,10,9,5,6,7]` |
| 1×4 `[[1,2,3,4]]` | `[1,2,3,4]` |
| 4×1 `[[1],[2],[3],[4]]` | `[1,2,3,4]` |
| 1×1 `[[7]]` | `[7]` |

**Constraints:** `1 ≤ m, n ≤ 10`.

---

## 2. How to think about it

Peel the matrix like an onion, one ring at a time. Keep **four boundaries** for the part not yet visited:

```
         left        right
          ↓            ↓
 top  →   1   2   3   4
          5   6   7   8
bottom→   9  10  11  12
```

Repeat while `top <= bottom && left <= right`:
1. **Right** along the top row (`left → right`), then `top++` (that row is done)
2. **Down** along the right column (`top → bottom`), then `right--`
3. **Left** along the bottom row (`right → left`), then `bottom--`. **Only if `top <= bottom`**
4. **Up** along the left column (`bottom → top`), then `left++`. **Only if `left <= right`**

### Why the extra checks in steps 3 and 4?
When only **one row** remains, step 1 already read it. After `top++`, `top > bottom`, and without the check step 3 would read that row **again**, backwards. The same goes for a single remaining **column** in step 4. **Always test 1×n and n×1 matrices.** They expose this bug instantly.

---

## 3. Java concepts used in this problem

### 3.1 Loops that count down: `for (int j = right; j >= left; j--)`

Steps 3 and 4 walk **backwards**. Note the `>=`: we include the `left` boundary itself.

### 3.2 A `while` with a compound condition

```java
while (top <= bottom && left <= right) { ... }
```

Both "there are rows left" **and** "there are columns left" must hold.

### 3.3 `List<Integer>` as output

The output length is m·n, so an `int[]` would work too, but LeetCode's signature returns `List<Integer>`. `out.add(matrix[i][j])` autoboxes each int.

---

## 4. Approach 1: Direction vectors + visited matrix

A general "walk and turn when blocked" simulation:

```java
static List<Integer> spiralVisited(int[][] matrix) {
    int m = matrix.length, n = matrix[0].length;
    int[] dr = {0, 1, 0, -1};          // right, down, left, up
    int[] dc = {1, 0, -1, 0};
    boolean[][] seen = new boolean[m][n];
    List<Integer> out = new ArrayList<>();
    int r = 0, c = 0, d = 0;
    for (int step = 0; step < m * n; step++) {
        out.add(matrix[r][c]);
        seen[r][c] = true;
        int nr = r + dr[d], nc = c + dc[d];
        if (nr < 0 || nr >= m || nc < 0 || nc >= n || seen[nr][nc]) {
            d = (d + 1) % 4;                 // turn clockwise
            nr = r + dr[d]; nc = c + dc[d];
        }
        r = nr; c = nc;
    }
    return out;
}
```

- **Direction arrays** `dr`/`dc`: index d picks a direction; `(d + 1) % 4` turns right. This trick is used constantly in grid and graph problems (BFS/DFS on grids in 09-Graph).
- `boolean[][] seen = new boolean[m][n]` is a 2D array of `false`s.
- **O(m·n) time, O(m·n) space** (the visited matrix). It's easy to reason about, but uses extra memory.

---

## 5. Approach 2: Shrinking boundaries (the file's solution)

### Code ([M10_SpiralMatrix.java:40-60](M10_SpiralMatrix.java#L40-L60))

```java
static List<Integer> spiralOrder(int[][] matrix) {
    List<Integer> out = new ArrayList<>();
    int top = 0, bottom = matrix.length - 1;
    int left = 0, right = matrix[0].length - 1;

    while (top <= bottom && left <= right) {
        for (int j = left; j <= right; j++) out.add(matrix[top][j]);      // 1. right
        top++;
        for (int i = top; i <= bottom; i++) out.add(matrix[i][right]);    // 2. down
        right--;
        if (top <= bottom) {
            for (int j = right; j >= left; j--) out.add(matrix[bottom][j]); // 3. left
            bottom--;
        }
        if (left <= right) {
            for (int i = bottom; i >= top; i--) out.add(matrix[i][left]);   // 4. up
            left++;
        }
    }
    return out;
}
```

### Line by line

| Line | What it does |
|---|---|
| `top = 0, bottom = m − 1, left = 0, right = n − 1` | the boundaries of the unvisited rectangle (inclusive) |
| `while (top <= bottom && left <= right)` | something is still unvisited |
| step 1 loop, `top++` | read the top row from left to right; that row is used up |
| step 2 loop, `right--` | read the right column from the new top down; that column is used up |
| `if (top <= bottom)` + step 3, `bottom--` | if a row still remains, read the bottom row from right to left |
| `if (left <= right)` + step 4, `left++` | if a column still remains, read the left column from bottom to top |

### Dry run 1: the 3×4 matrix

| ring step | boundaries before (t, b, l, r) | reads | boundary change |
|---|---|---|---|
| 1 right | (0, 2, 0, 3) | 1 2 3 4 | top = 1 |
| 2 down | (1, 2, 0, 3) | 8 12 | right = 2 |
| 3 left (1 ≤ 2 ✓) | (1, 2, 0, 2) | 11 10 9 | bottom = 1 |
| 4 up (0 ≤ 2 ✓) | (1, 1, 0, 2) | 5 | left = 1 |
| 1 right | (1, 1, 1, 2) | 6 7 | top = 2 |
| 2 down | (2, 1, 1, 2) | (i from 2 to 1: none) | right = 1 |
| 3 left? | top 2 ≤ bottom 1? **no** → skip | | |
| 4 up? | left 1 ≤ right 1 ✓ → i from bottom 1 down to top 2: none | | left = 2 |
| loop check | top 2 > bottom 1 → stop | | |

**Result:** `[1,2,3,4, 8,12, 11,10,9, 5, 6,7]` ✓

### Dry run 2: 1×4 `[[1, 2, 3, 4]]` (why the check in step 3 matters)

| step | boundaries | reads | change |
|---|---|---|---|
| 1 right | (0, 0, 0, 3) | 1 2 3 4 | top = 1 |
| 2 down | i from 1 to 0: none | | right = 2 |
| 3 left? | 1 ≤ 0? **no → skip** | (without the check: 3 2 1 would be read again!) | |
| 4 up? | 0 ≤ 2 ✓, i from 0 down to 1: none | | left = 1 |

**Result:** `[1, 2, 3, 4]` ✓

### Dry run 3: 4×1 `[[1],[2],[3],[4]]`

| step | reads | change |
|---|---|---|
| 1 right (j = 0..0) | 1 | top = 1 |
| 2 down (i = 1..3) | 2 3 4 | right = −1 |
| 3 left? 1 ≤ 3 ✓, j from −1 down to 0: none | | bottom = 2 |
| 4 up? 0 ≤ −1? **no → skip** | (without the check: it would read column 0 again) | |

**Result:** `[1, 2, 3, 4]` ✓

### Complexity
- **Time O(m·n):** each cell is read exactly once.
- **Space O(1) extra** (the output list doesn't count).

---

## 6. Comparing the approaches

| | Direction + visited | Shrinking boundaries |
|---|---|---|
| Time | O(mn) | O(mn) |
| Extra space | O(mn) | **O(1)** |
| Generalises to "walk until blocked" | **yes** | spiral only |

---

## 7. Edge cases and common mistakes

**Edge cases:** 1×n, n×1, 1×1, a square with odd n (a single centre cell), a non-square rectangle.

**Mistakes:**
1. Missing the `if (top <= bottom)` / `if (left <= right)` checks, which duplicates elements in single-row/column cases.
2. Updating a boundary **before** finishing its loop.
3. Off-by-one in the loop bounds (`<` instead of `<=`). The boundaries are inclusive.

---

## 8. Interview questions and answers

**Q1. What invariant do the four boundaries maintain?**
> The rectangle `[top..bottom] × [left..right]` is exactly the set of cells not yet output. Each step outputs one edge of it and shrinks the matching boundary.

**Q2. Why can steps 3 and 4 duplicate output?**
> After steps 1 and 2, the remaining rectangle might be empty in one dimension (a single row or column was consumed). The checks make sure there's still a row (for step 3) or a column (for step 4) left.

**Q3. How would you test this?**
> 1×1, 1×n, n×1, 2×2, 3×3, 3×4, 4×3. The degenerate shapes catch most bugs.

### Follow-up questions

**F1. Spiral Matrix II (LC 59): fill an n × n matrix with 1..n² in spiral order.**
> The same boundary loops, writing `matrix[...] = counter++` instead of reading.

**F2. Spiral Matrix III (LC 885): spiral outward from a start cell in a grid.**
> Use direction vectors, with step lengths following the pattern 1, 1, 2, 2, 3, 3, …; record the cells that are inside the grid.

**F3. Counter-clockwise spiral, or starting from another corner.**
> Change the order of the four steps (and their directions) to match.

**F4. Diagonal traversal (LC 498).**
> Walk the anti-diagonals (`i + j = s` for s = 0..m+n−2), alternating direction; or use direction vectors with bounce rules.

---

## 9. Running the file

```bash
java M10_SpiralMatrix.java      # 5/5 tests passed
```

**One-line summary:** *"Four boundaries: right, down, then left/up only if a row/column remains; shrink after each edge."*
