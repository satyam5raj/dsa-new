# H01 · N-Queens

| | |
|---|---|
| **LeetCode** | [#51 N-Queens](https://leetcode.com/problems/n-queens/) |
| **Difficulty** | Hard |
| **Pattern** | One queen per **row** + O(1) attack checks with column and diagonal arrays |
| **Code** | [H01_NQueens.java](H01_NQueens.java) |
| **Run** | `java H01_NQueens.java` |

Backtracking basics are explained in [E01](../02-Easy/E01_BinaryWatch.md#3-java-concepts-used-in-this-problem). The effect of pruning on this exact problem is measured in [01-Basics/PruningAndConstraints.java](../01-Basics/PruningAndConstraints.java).

---

## 1. The problem

Place n queens on an n × n board so that no two attack each other (no shared row, column or diagonal). Return every board, drawn with `Q` and `.`.

```
n = 4 → two solutions:

. Q . .        . . Q .
. . . Q        Q . . .
Q . . .        . . . Q
. . Q .        . Q . .
```

| n | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 |
|---|---|---|---|---|---|---|---|---|---|
| solutions | 1 | 0 | 0 | 2 | 10 | 4 | 40 | 92 | 352 |

---

## 2. How to think about it

### Shrink the search
Two queens can't share a row, so each row has **exactly one** queen. The decision at depth r is "which column for row r's queen?". That's at most nⁿ leaves instead of C(n², n) ways to place n queens anywhere.

### O(1) attack checks
Square (r, c) is attacked if:

| array | meaning | why the index works |
|---|---|---|
| `cols[c]` | a queen is in column c | |
| `diag[r + c]` | a queen is on the same "/" diagonal | r + c is constant along it (0..2n − 2) |
| `anti[r − c + n − 1]` | a queen is on the same "\" diagonal | r − c is constant along it; adding n − 1 shifts it to 0..2n − 2 |

Mark all three when placing a queen and unmark them when backtracking.

```
r + c for n = 4          r − c + 3 for n = 4
0 1 2 3                  3 2 1 0
1 2 3 4                  4 3 2 1
2 3 4 5                  5 4 3 2
3 4 5 6                  6 5 4 3
```

[PruningAndConstraints](../01-Basics/PruningAndConstraints.java) measured that for n = 8, the search visits 19 million nodes without these checks and 2,057 with them.

### Dry run: n = 4 (from a real run)

```
row 0 col 0
  row 1 col 2
    row 2: all attacked            → back
  row 1 col 3
    row 2 col 1
      row 3: all attacked          → back
row 0 col 1
  row 1 col 3
    row 2 col 0
      row 3 col 2  → SOLUTION [1, 3, 0, 2]
row 0 col 2
  row 1 col 0
    row 2 col 3
      row 3 col 1  → SOLUTION [2, 0, 3, 1]
row 0 col 3
  row 1 col 0
    row 2 col 2
      row 3: all attacked
  row 1 col 1
    row 2: all attacked
```

(Row 1 only ever tries the columns that aren't attacked: after row 0 col 0, columns 0 and 1 are attacked, so it starts with col 2.) `[1, 3, 0, 2]` means row 0's queen is in column 1, row 1's in column 3, and so on, which is the first board above.

---

## 3. Java concepts used in this problem

### 3.1 Chained assignment

```java
cols[c] = diag[r + c] = anti[r - c + n - 1] = true;
```

Assignment evaluates right to left and returns the assigned value, so this sets all three to `true` in one statement.

### 3.2 Storing the solution compactly

`int[] queenCol` holds the column of each row's queen. The board is drawn only when a solution is complete, which avoids maintaining a 2D `char` grid during the search. There's no need to "undo" `queenCol[r]`: it's overwritten by the next choice for that row.

### 3.3 Drawing a row

```java
char[] row = new char[n];
Arrays.fill(row, '.');
row[c] = 'Q';
board.add(new String(row));
```

`Arrays.fill` sets every element, and `new String(char[])` turns the array into a string.

### 3.4 Validating a board independently (in the test)

```java
if (a[0] == b[0] || a[1] == b[1] || Math.abs(a[0] - b[0]) == Math.abs(a[1] - b[1])) return false;
```

Two queens share a diagonal exactly when their row distance equals their column distance. The test uses this pairwise check, which doesn't depend on the index tricks, to confirm every board.

---

## 4. Approach 1: Checking every pair of queens (the validity referee)

The test's `valid` function ([H01_NQueens.java:67-77](H01_NQueens.java#L67-L77)) finds all the queens on a board and checks every pair with the row/column/diagonal-distance rule. It's O(n²) per board, independent of the solver's bookkeeping. Combined with the known solution counts for n = 1..9, it confirms the solver.

---

## 5. Approach 2: Row-by-row backtracking with O(1) checks (the file's main solution)

### Code ([H01_NQueens.java:38-65](H01_NQueens.java#L38-L65))

```java
static List<List<String>> solveNQueens(int n) {
    List<List<String>> out = new ArrayList<>();
    int[] queenCol = new int[n];
    bt(n, 0, queenCol, new boolean[n], new boolean[2 * n - 1], new boolean[2 * n - 1], out);
    return out;
}

static void bt(int n, int r, int[] queenCol, boolean[] cols, boolean[] diag, boolean[] anti, List<List<String>> out) {
    if (r == n) { out.add(draw(queenCol)); return; }
    for (int c = 0; c < n; c++) {
        if (cols[c] || diag[r + c] || anti[r - c + n - 1]) continue;   // attacked
        queenCol[r] = c;
        cols[c] = diag[r + c] = anti[r - c + n - 1] = true;
        bt(n, r + 1, queenCol, cols, diag, anti, out);
        cols[c] = diag[r + c] = anti[r - c + n - 1] = false;           // undo
    }
}

static List<String> draw(int[] queenCol) {
    List<String> board = new ArrayList<>();
    for (int c : queenCol) {
        char[] row = new char[queenCol.length];
        Arrays.fill(row, '.');
        row[c] = 'Q';
        board.add(new String(row));
    }
    return board;
}
```

### Line by line

| Line | What it does |
|---|---|
| the array sizes | n columns, and 2n − 1 diagonals in each direction |
| `r == n` | every row has a queen: draw and record the board |
| the `continue` | column c is attacked in row r |
| `queenCol[r] = c` | remember the choice for drawing |
| mark / recurse / unmark | place the queen, solve the next row, remove it |
| `draw` | builds the n strings for one board |

### Complexity
- **Time:** roughly O(n!) (each row has fewer free columns than the one before), plus O(n²) to draw each solution.
- **Space O(n)** for the arrays and recursion.

---

## 6. Comparing the approaches

| | No pruning (any square) | One per row, check at the end | One per row + O(1) checks |
|---|---|---|---|
| Nodes for n = 8 | astronomically many | 19 million (measured) | **2,057** (measured) |

The file checks the solution counts for n = 1..9 against the known values, that no board repeats, and that every board passes the pairwise check.

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 1 (one board, "Q"), n = 2 and n = 3 (no solutions).

**Mistakes:**
1. Indexing the "\" diagonal with `r − c` (it goes negative).
2. Arrays of size n for the diagonals (there are 2n − 1).
3. Forgetting to unmark all three arrays.
4. Checking attacks by scanning the board (O(n) per check instead of O(1)).

---

## 8. Interview questions and answers

**Q1. Why is r + c constant along one diagonal and r − c along the other?**
> Moving along a "/" diagonal changes r by +1 and c by −1 (or the reverse), so their sum doesn't change. Along a "\" diagonal both change by +1, so their difference doesn't change.

**Q2. Why place by rows?**
> It builds the "one queen per row" constraint into the search itself, so it never has to be checked, and it reduces each decision to choosing a column.

### Follow-up questions

**F1. Count the solutions only.** → [H02](H02_NQueensII.md)
> Bitmask backtracking, about 8× faster.

**F2. Use symmetry to halve the work.**
> Only try the first row's queen in the left half of the columns and double the count (handling the middle column separately for odd n).

**F3. Sudoku Solver.** → [H03](H03_SudokuSolver.md)
> The same "constraint arrays + backtracking", with rows, columns and boxes.

---

## 9. Running the file

```bash
java H01_NQueens.java      # 3/3 tests passed (including n = 1..9: known counts, every board valid)
```

**One-line summary:** *"Place one queen per row, choosing columns not marked in cols[c], diag[r + c] or anti[r − c + n − 1]; mark before recursing, unmark after, and draw the board when every row has a queen."*
