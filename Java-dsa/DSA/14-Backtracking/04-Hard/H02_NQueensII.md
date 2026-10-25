# H02 · N-Queens II

| | |
|---|---|
| **LeetCode** | [#52 N-Queens II](https://leetcode.com/problems/n-queens-ii/) |
| **Difficulty** | Hard |
| **Pattern** | Count only → **bitmask** backtracking (the diagonal attacks shift with each row) |
| **Code** | [H02_NQueensII.java](H02_NQueensII.java) |
| **Run** | `java H02_NQueensII.java` |

The board-listing version is [H01](H01_NQueens.md); backtracking basics are in [E01](../02-Easy/E01_BinaryWatch.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Return the **number** of N-Queens solutions.

| n | 4 | 8 | 13 |
|---|---|---|---|
| answer | 2 | 92 | 73,712 |

---

## 2. How to think about it

[H01](H01_NQueens.md) with a counter instead of boards already works. The classic speed-up keeps the attacked columns **of the current row** as three bitmasks (bit k = column k):

| mask | meaning |
|---|---|
| `cols` | columns already used |
| `diag` | squares in this row attacked along one diagonal direction |
| `anti` | squares in this row attacked along the other diagonal direction |

Moving down to the next row, each diagonal attack moves one column sideways:

```
diag → ((diag | bit) << 1) & all        anti → (anti | bit) >> 1
```

The free squares in the current row are

```
free = ~(cols | diag | anti) & all          where all = (1 << n) − 1
```

and we pick them one at a time with the **lowest set bit** trick, `bit = free & −free`. Everything is a few machine instructions: no arrays and no loop over attacked columns.

### Dry run: n = 4, the branch that finds the first solution (from a real run)
Masks are shown as 4 bits, with column 0 on the **right**.

| row | cols | diag | anti | free | pick |
|---|---|---|---|---|---|
| 0 | 0000 | 0000 | 0000 | 1111 | 0010 (col 1) |
| 1 | 0010 | 0100 | 0001 | 1000 | 1000 (col 3) |
| 2 | 1010 | 1000 | 0100 | 0001 | 0001 (col 0) |
| 3 | 1011 | 0010 | 0010 | 0100 | 0100 (col 2) |
| 4 | | | | | count 1 |

Row 1's diag `0100` is the queen in column 1 shifted left (it attacks column 2 in row 1), and anti `0001` is it shifted right (column 0). Columns 0–2 are blocked, so only column 3 is free. This is the solution [1, 3, 0, 2] from H01.

A branch dies when `free` becomes 0: for example, picking column 0 and then column 2 leaves row 2 with free = 0000.

---

## 3. Java concepts used in this problem

### 3.1 `~` (bitwise NOT) and masking with `all`

`~x` flips **all 32** bits, including those above column n − 1. `& all` (where all = `(1 << n) − 1`, n ones) keeps only the bits for real columns.

### 3.2 The lowest set bit: `x & −x`

In two's complement, −x = ~x + 1. ANDing x with −x leaves only the lowest 1 bit:

```
x      = 0110 1000
−x     = 1001 1000
x & −x = 0000 1000
```

`free -= bit` then removes it, so the loop visits each free square once.

### 3.3 Shifts move the attacks

`<< 1` moves every bit one column up and `>> 1` one column down. A queen at column c attacks column c + 1 on one diagonal and column c − 1 on the other in the next row; those keep moving one more column per row. The `& all` after `<< 1` drops attacks that slide off the board on one side; on the other side, `>> 1` drops them automatically.

### 3.4 Passing state by value

The masks are `int` parameters, so each call gets its own copies. There's nothing to undo: the parent's masks are untouched when the child returns.

### 3.5 Timing with `printf("%,d")`

The test prints the solution count with thousands separators and the two running times.

---

## 4. Approach 1: Boolean arrays (the referee)

### Code ([H02_NQueensII.java:50-60](H02_NQueensII.java#L50-L60))

```java
static int arrays(int n, int r, boolean[] c, boolean[] d, boolean[] a) {
    if (r == n) return 1;
    int t = 0;
    for (int col = 0; col < n; col++) {
        if (c[col] || d[r + col] || a[r - col + n - 1]) continue;
        c[col] = d[r + col] = a[r - col + n - 1] = true;
        t += arrays(n, r + 1, c, d, a);
        c[col] = d[r + col] = a[r - col + n - 1] = false;
    }
    return t;
}
```

The [H01](H01_NQueens.md#5-approach-2-row-by-row-backtracking-with-o1-checks-the-files-main-solution) search, returning a count instead of drawing boards.

---

## 5. Approach 2: Bitmask backtracking (the file's main solution)

### Code ([H02_NQueensII.java:34-47](H02_NQueensII.java#L34-L47))

```java
static int totalNQueens(int n) { return count(n, 0, 0, 0, 0); }

static int count(int n, int row, int cols, int diag, int anti) {
    if (row == n) return 1;
    int all = (1 << n) - 1;
    int free = ~(cols | diag | anti) & all;          // squares not attacked in this row
    int total = 0;
    while (free != 0) {
        int bit = free & -free;                       // lowest free square
        free -= bit;
        total += count(n, row + 1, cols | bit, ((diag | bit) << 1) & all, (anti | bit) >> 1);
    }
    return total;
}
```

### Line by line

| Line | What it does |
|---|---|
| `row == n` | every row has a queen: one solution |
| `all` | n one-bits, one per column |
| `free` | the columns not attacked in this row |
| `bit = free & −free` | the next free column to try |
| `free -= bit` | don't try it again in this loop |
| the recursive call | place the queen and shift the diagonal attacks for the next row |

### Complexity
- **Time:** roughly O(n!) like H01, with a much smaller constant.
- **Space O(n)** recursion; no arrays.

---

## 6. Comparing the approaches

| | Boolean arrays | Bitmasks |
|---|---|---|
| n = 13 (measured) | 169 ms | **20 ms** |
| Work per node | a loop over all n columns | only the free ones |
| State | three arrays, marked and unmarked | three ints, passed by value |

The file checks that both give the same counts for n = 1..11, and times n = 13.

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 1 (1), n = 2 and 3 (0), n up to about 16 before the counts get slow.

**Mistakes:**
1. Forgetting `& all` after `~` (bits above n look free and the counts explode).
2. Forgetting `& all` after `<< 1` (attacks slide past column n − 1 and later wrongly reappear as "occupied" bits; they also make `free` wrong).
3. Shifting `diag` and `anti` in the same direction.

---

## 8. Interview questions and answers

**Q1. Why do the diagonal masks shift by one per row?**
> A queen at column c attacks column c ± k in the row k below it. Shifting the masks by one at every row moves each attack one more column away, exactly matching that.

**Q2. Explain `free & −free`.**
> In two's complement, −free flips every bit above the lowest set bit and keeps that bit, so the AND isolates the lowest set bit.

### Follow-up questions

**F1. Return the boards with bitmasks.**
> Record the chosen `bit` for each row (`Integer.numberOfTrailingZeros(bit)` gives the column) and draw at the leaves.

**F2. Halve the work with symmetry.**
> Only branch on the first row's left half, double the count, and add the middle column's count once for odd n.

**F3. Bitmasks elsewhere.**
> Sudoku solvers ([H03](H03_SudokuSolver.md)) keep a 9-bit mask of used digits per row, column and box in the same style.

---

## 9. Running the file

```bash
java H02_NQueensII.java      # 4/4 tests passed; prints n=13 (73,712 solutions): bitmask ~20 ms vs arrays ~170 ms
```

**One-line summary:** *"Count solutions row by row with three bitmasks (columns and two diagonals), finding the free squares as ~(cols | diag | anti) & all, trying them by lowest set bit, and shifting the diagonal masks by one for the next row."*
