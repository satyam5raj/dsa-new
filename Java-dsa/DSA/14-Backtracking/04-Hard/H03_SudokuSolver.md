# H03 · Sudoku Solver

| | |
|---|---|
| **LeetCode** | [#37 Sudoku Solver](https://leetcode.com/problems/sudoku-solver/) |
| **Difficulty** | Hard |
| **Pattern** | Constraint satisfaction: **bitmask** constraints + "most constrained cell first" (MRV) |
| **Code** | [H03_SudokuSolver.java](H03_SudokuSolver.java) |
| **Run** | `java H03_SudokuSolver.java` |

Backtracking basics are explained in [E01](../02-Easy/E01_BinaryWatch.md#3-java-concepts-used-in-this-problem). The bitmask tricks (`x & −x`, `~` with a mask) are explained in [H02 N-Queens II](H02_NQueensII.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Fill the empty cells (`'.'`) of a 9 × 9 board so that every row, every column and every 3 × 3 box contains the digits 1–9 exactly once. The puzzle has exactly one solution. Modify the board in place.

```
5 3 . | . 7 . | . . .          5 3 4 | 6 7 8 | 9 1 2
6 . . | 1 9 5 | . . .          6 7 2 | 1 9 5 | 3 4 8
. 9 8 | . . . | . 6 .    →     1 9 8 | 3 4 2 | 5 6 7
------+-------+------          ...
```

---

## 2. How to think about it

### Basic backtracking
Find an empty cell, try each digit 1–9 that doesn't clash, and recurse. If no digit works, undo and return false, so the caller tries its next digit. **Return true as soon as the board is solved**, which stops the whole search at the first solution.

### O(1) clash checks with bitmasks
Keep three arrays of 9-bit masks: `rows[r]`, `cols[c]` and `boxes[b]`. Bit d is set when digit d + 1 is already used there.

```
box index      b = (r / 3) * 3 + c / 3
allowed digits = ~(rows[r] | cols[c] | boxes[b]) & 0x1FF       (0x1FF = nine 1-bits)
```

```
box indices:
0 0 0 | 1 1 1 | 2 2 2
0 0 0 | 1 1 1 | 2 2 2
0 0 0 | 1 1 1 | 2 2 2
------+-------+------
3 3 3 | 4 4 4 | 5 5 5        e.g. (r 4, c 7): 4/3*3 + 7/3 = 3 + 2 = 5
...
```

### Most constrained cell first (MRV)
Instead of filling the next empty cell in reading order, fill the empty cell with the **fewest** allowed digits ("minimum remaining values"):
- a cell with **one** option is forced, so there's no branching at all;
- a cell with **zero** options means a contradiction, so backtrack **now**, before wasting effort elsewhere.

Measured on a famously hard puzzle (by Arto Inkala):

| cell choice | nodes visited |
|---|---|
| next empty cell | 91,351 |
| most constrained cell | **15,573** |

### Dry run: one step on the LeetCode example
Cell (0, 2) is empty. Row 0 contains {5, 3, 7}, column 2 contains {8}, and box 0 contains {5, 3, 6, 9, 8}. Used = {3, 5, 6, 7, 8, 9}, so the allowed digits are {1, 2, 4}, three options. The MRV scan looks for a cell with fewer options (stopping early at one with ≤ 1) and fills that one first.

For a cell with allowed mask `0b000001010` (digits 2 and 4): `mask & −mask` = `0b10` → digit 2 is tried first; `mask &= mask − 1` drops it, and then `0b1000` → digit 4.

---

## 3. Java concepts used in this problem

### 3.1 Static fields as solver state

`static int[] rows, cols, boxes;` are shared by all recursive calls and reset at the start of `solve`. (A solver object with instance fields is the cleaner design when several solves can run at once.)

### 3.2 Digits as bits

```java
int bit = 1 << (board[r][c] - '1');      // '1' → bit 0, '9' → bit 8
rows[r] |= bit;                          // mark it used
rows[r] ^= bit;                          // unmark (XOR clears a bit we know is set)
```

### 3.3 Iterating over set bits

```java
for (int mask = allowed(r, c); mask != 0; mask &= mask - 1) {
    int bit = mask & -mask;                          // the lowest allowed digit
    int d = Integer.numberOfTrailingZeros(bit);      // its index 0..8
    ...
}
```

`mask &= mask − 1` clears the lowest set bit (the same trick as in [Recursion E03](../../13-Recursion/02-Easy/E03_PowerOfTwo.md)). `Integer.numberOfTrailingZeros` turns a single bit back into its position.

### 3.4 `Collections.swap` to keep the empty cells in a list

```java
Collections.swap(empty, pick, remaining - 1);       // move the chosen cell to the end
...
Collections.swap(empty, pick, remaining - 1);       // put it back when backtracking
```

The first `remaining` entries of `empty` are the cells still to fill. Swapping the chosen cell to position `remaining − 1` and then recursing with `remaining − 1` removes it from consideration in O(1), with no list copying. Swapping back on the way out restores the order.

### 3.5 Copying a 2D array

```java
Arrays.stream(b).map(char[]::clone).toArray(char[][]::new)
```

`b.clone()` alone would copy only the outer array (the rows would be shared). Cloning each row gives a real deep copy, so the test can solve copies of the same puzzle independently.

### 3.6 `(char) ('1' + d)`

`'1' + d` is an `int` (characters are numbers in arithmetic), so the cast turns it back into the digit character.

---

## 4. Approach 1: Next empty cell in order

The same code with `mrv = false` ([H03_SudokuSolver.java:56](H03_SudokuSolver.java#L56)): `pick` stays `remaining − 1`, so cells are filled in a fixed order. Correct, and fine for easy puzzles, but on the hard puzzle it visits 91,351 nodes.

---

## 5. Approach 2: Bitmasks + MRV (the file's main solution)

### Code ([H03_SudokuSolver.java:35-80](H03_SudokuSolver.java#L35-L80))

```java
static int[] rows, cols, boxes;
static long nodes;

static void solveSudoku(char[][] board) { solve(board, true); }

static boolean solve(char[][] board, boolean mrv) {
    rows = new int[9]; cols = new int[9]; boxes = new int[9];
    List<int[]> empty = new ArrayList<>();
    for (int r = 0; r < 9; r++) for (int c = 0; c < 9; c++) {
        if (board[r][c] == '.') { empty.add(new int[]{r, c}); continue; }
        int bit = 1 << (board[r][c] - '1');
        rows[r] |= bit; cols[c] |= bit; boxes[r / 3 * 3 + c / 3] |= bit;
    }
    nodes = 0;
    return bt(board, empty, empty.size(), mrv);
}

static boolean bt(char[][] board, List<int[]> empty, int remaining, boolean mrv) {
    nodes++;
    if (remaining == 0) return true;
    // choose the cell to fill: the most constrained (mrv) or simply the next one
    int pick = remaining - 1, best = 10;
    if (mrv) {
        for (int i = 0; i < remaining; i++) {
            int[] cell = empty.get(i);
            int options = Integer.bitCount(allowed(cell[0], cell[1]));
            if (options < best) { best = options; pick = i; if (options <= 1) break; }
        }
    }
    Collections.swap(empty, pick, remaining - 1);              // move the chosen cell to the end
    int[] cell = empty.get(remaining - 1);
    int r = cell[0], c = cell[1], b = r / 3 * 3 + c / 3;
    for (int mask = allowed(r, c); mask != 0; mask &= mask - 1) {
        int bit = mask & -mask;
        int d = Integer.numberOfTrailingZeros(bit);
        board[r][c] = (char) ('1' + d);
        rows[r] |= bit; cols[c] |= bit; boxes[b] |= bit;
        if (bt(board, empty, remaining - 1, mrv)) return true;   // solved: stop everything
        rows[r] ^= bit; cols[c] ^= bit; boxes[b] ^= bit;          // undo
    }
    board[r][c] = '.';
    Collections.swap(empty, pick, remaining - 1);
    return false;
}

static int allowed(int r, int c) { return ~(rows[r] | cols[c] | boxes[r / 3 * 3 + c / 3]) & 0x1FF; }
```

### Line by line

| Line | What it does |
|---|---|
| the setup loop | record the given digits in the masks; collect the empty cells |
| `remaining == 0` | every cell is filled: solved |
| the MRV scan | find the empty cell with the fewest allowed digits; stop early at 0 or 1 |
| the first swap | move that cell to the end of the active part of the list |
| the digit loop | try each allowed digit, lowest first |
| place + mark | write the digit and update the three masks |
| `return true` | propagate success all the way up without undoing anything |
| unmark | the digit led to a dead end |
| `board[r][c] = '.'` | no digit worked: clear the cell before returning false |
| the second swap | restore the list order for the caller |

A cell with 0 options simply has an empty digit loop, so `bt` returns false immediately: that's the "backtrack now" effect.

### Complexity
- **Time:** exponential in the worst case, fast in practice (thousands of nodes for the hardest known puzzles).
- **Space O(81)** for the masks, the empty list and the recursion depth.

---

## 6. Comparing the approaches

| | Next empty cell | MRV |
|---|---|---|
| Hard puzzle | 91,351 nodes | **15,573** nodes |
| Extra work per node | none | a scan over the remaining cells |

The file checks that both versions produce the same valid solution on the hard puzzle, that the LeetCode example is solved correctly, and that 50 random puzzles (made by blanking 45–54 cells of a solved board) are solved validly.

---

## 7. Edge cases and common mistakes

**Edge cases:** an almost-full board, a puzzle needing deep backtracking, a board already complete (`remaining == 0` at once).

**Mistakes:**
1. Not stopping at the first solution (without `return true`, the search undoes the finished board).
2. The wrong box formula (it's `r / 3 * 3 + c / 3`).
3. Forgetting `& 0x1FF` after `~` (bits 9–31 look like allowed digits).
4. Leaving a digit in the cell after all options fail (it must be reset to `'.'`).

---

## 8. Interview questions and answers

**Q1. Why return a boolean from the recursion?**
> To stop as soon as a solution is found. The `true` propagates up through every caller without undoing anything, leaving the solved board in place.

**Q2. Why does MRV help so much?**
> It branches where the fewest choices exist, so the tree is narrow near the root. Cells with one option are filled without branching, and contradictions (zero options) are found immediately.

**Q3. How would you speed it up further?**
> Constraint propagation: after each placement, repeatedly fill every cell that has only one option (naked singles) and every digit that has only one possible cell in a unit (hidden singles). Or model it as exact cover and use Knuth's Dancing Links.

### Follow-up questions

**F1. Valid Sudoku (LC 36).**
> Only check the given digits: the same three masks, returning false if a bit is already set.

**F2. Count all solutions (for puzzle generation).**
> Don't return early; count the leaves (and stop at 2 if you only need to know whether the solution is unique).

**F3. N-Queens.** → [H01](H01_NQueens.md)
> The same constraint-array backtracking with rows, columns and diagonals.

---

## 9. Running the file

```bash
java H03_SudokuSolver.java      # 5/5 tests passed; prints 91,351 vs 15,573 nodes on the hard puzzle
```

**One-line summary:** *"Track used digits per row, column and box as 9-bit masks, always fill the empty cell with the fewest allowed digits, try them by lowest set bit, and return true up the stack as soon as the board is complete."*
