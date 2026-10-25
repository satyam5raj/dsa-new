# M08 · Word Search

| | |
|---|---|
| **LeetCode** | [#79 Word Search](https://leetcode.com/problems/word-search/) |
| **Difficulty** | Medium |
| **Pattern** | Grid backtracking (mark, recurse 4 ways, unmark): the base of Word Search II |
| **Code** | [M08_WordSearch.java](M08_WordSearch.java) |
| **Run** | `java M08_WordSearch.java` |

The grid DFS template is in [Graph E02 Flood Fill](../../09-Graph/02-Easy/E02_FloodFill.md); backtracking in [Trees M06](../../07-Trees/03-Medium/M06_PathSumII.md).

---

## 1. The problem

Does `word` exist in the board as a path of horizontally or vertically adjacent cells, using each cell **at most once**?

```
A B C E
S F C S        "ABCCED" → true      "SEE" → true      "ABCB" → false (it would reuse the B)
A D E E
```

---

## 2. How to think about it

### Backtracking from every cell
```
dfs(r, c, i):       // can word[i..] be matched starting at (r, c)?
    if i == len:  true
    if out of bounds or board[r][c] != word[i]:  false
    MARK the cell (e.g. board[r][c] = '#') so this path can't reuse it
    found = dfs on the 4 neighbours with i + 1
    UNMARK (restore the letter): other paths may use this cell
    return found
```

**Unlike flood fill, we unmark:** "visited" only applies to the **current** path.

### Pruning (big speed-ups in practice)
- if the word has more of some letter than the board → false immediately
- if the last letter is rarer on the board than the first, search the **reversed** word (fewer starting points)

### Dry run: "ABCB"
```
start (0,0) 'A' → (0,1) 'B' → (0,2) 'C' → needs 'B': neighbours (1,2) C, (0,3) E, (0,1) is '#' (used) → fail
backtrack … no other start works → false
```

[Word Search II (H01)](../04-Hard/H01_WordSearchII.md) searches **many** words: running this once per word is too slow, and a trie lets one DFS find them all at once.

---

## 3. Java concepts used in this problem

### 3.1 Marking in place and restoring

```java
char saved = b[r][c];
b[r][c] = '#';                // mark: used on this path
... recurse ...
b[r][c] = saved;              // unmark: other paths may use it
```

Overwriting the cell avoids a separate `visited` array. Restoring it is essential: the test checks that the board is unchanged afterwards.

### 3.2 `||` stops at the first success

```java
boolean found = dfs(down) || dfs(up) || dfs(right) || dfs(left);
```

Short-circuiting: once one direction finds the word, the others aren't explored. (The unmark line still runs afterwards.)

### 3.3 Letter counts with `int[128]`

```java
int[] boardCount = new int[128], wordCount = new int[128];
```

Indexing by the character itself (its ASCII code) covers both upper- and lowercase letters.

### 3.4 Reversing a string

`new StringBuilder(word).reverse().toString()`: a path for the reversed word is the same path walked backwards, so the answer doesn't change.

---

## 4. Approach 1: Plain backtracking with a visited array (the referee)

### Code ([M08_WordSearch.java:67-80](M08_WordSearch.java#L67-L80))

```java
static boolean plain(char[][] b, String w) {
    boolean[][] used = new boolean[b.length][b[0].length];
    for (int r = 0; r < b.length; r++) for (int c = 0; c < b[0].length; c++) if (go(b, w, r, c, 0, used)) return true;
    return false;
}

static boolean go(char[][] b, String w, int r, int c, int i, boolean[][] used) {
    if (r < 0 || r >= b.length || c < 0 || c >= b[0].length || used[r][c] || b[r][c] != w.charAt(i)) return false;
    if (i == w.length() - 1) return true;
    used[r][c] = true;
    boolean f = go(b, w, r + 1, c, i + 1, used) || go(b, w, r - 1, c, i + 1, used) || go(b, w, r, c + 1, i + 1, used) || go(b, w, r, c - 1, i + 1, used);
    used[r][c] = false;
    return f;
}
```

The same backtracking with a separate `used[][]` array and no pruning. It's correct, and the file's referee.

---

## 5. Approach 2: Backtracking with in-place marking + pruning (the file's main solution)

### Code ([M08_WordSearch.java:42-64](M08_WordSearch.java#L42-L64))

```java
static boolean exist(char[][] board, String word) {
    int[] boardCount = new int[128], wordCount = new int[128];
    for (char[] row : board) for (char c : row) boardCount[c]++;
    for (char c : word.toCharArray()) if (++wordCount[c] > boardCount[c]) return false;   // impossible: prune
    if (boardCount[word.charAt(0)] > boardCount[word.charAt(word.length() - 1)]) {
        word = new StringBuilder(word).reverse().toString();                               // start from the rarer end
    }
    for (int r = 0; r < board.length; r++)
        for (int c = 0; c < board[0].length; c++)
            if (dfs(board, word, r, c, 0)) return true;
    return false;
}

static boolean dfs(char[][] b, String w, int r, int c, int i) {
    if (i == w.length()) return true;
    if (r < 0 || r >= b.length || c < 0 || c >= b[0].length || b[r][c] != w.charAt(i)) return false;
    char saved = b[r][c];
    b[r][c] = '#';                                    // mark: used on this path
    boolean found = dfs(b, w, r + 1, c, i + 1) || dfs(b, w, r - 1, c, i + 1)
                 || dfs(b, w, r, c + 1, i + 1) || dfs(b, w, r, c - 1, i + 1);
    b[r][c] = saved;                                  // unmark: other paths may use it
    return found;
}
```

### Line by line

| Line | What it does |
|---|---|
| the letter counts | the board must contain enough of every letter |
| the reversal | start from whichever end letter is rarer on the board |
| the double loop | every cell is a possible start |
| `i == w.length()` | every letter was matched |
| the guard | out of bounds, already used (`'#'` never equals a letter), or the wrong letter |
| mark / recurse / unmark | standard backtracking |

### Complexity
- **Time O(R · C · 3^L)** (after the first step, each cell has at most 3 unvisited neighbours, since the previous cell is marked).
- **Space O(L)** recursion.

---

## 6. Comparing the approaches

| | Plain backtracking | In-place + pruning |
|---|---|---|
| Worst case | O(R · C · 4 · 3^(L−1)) | the same |
| In practice | slower | much faster on impossible or skewed words |
| Extra memory | visited R × C | none |

The file checks the pruned version against plain backtracking on 1,000 random boards, and confirms the board is restored.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single-cell board, a word longer than the number of cells, repeated letters (`AAAA`), a word that needs a U-turn.

**Mistakes:**
1. Not unmarking (later paths think the cell is used).
2. Unmarking only on success.
3. Allowing diagonal moves.
4. Returning without restoring the cell, which corrupts the board.

---

## 8. Interview questions and answers

**Q1. Why must the cell be unmarked?**
> The mark means "on the current path". Once this path is abandoned, other paths through the cell are still valid.

**Q2. Why does reversing the word help?**
> The search starts from every cell matching the first letter. If that letter is common and the last one is rare, starting from the rare end creates far fewer starting branches.

### Follow-up questions

**F1. Many words.** → [H01 Word Search II](../04-Hard/H01_WordSearchII.md)
> Build a trie of the words and run one DFS from each cell, following the trie.

**F2. Count every occurrence of the word.**
> Don't stop at the first success: sum the results over all the paths.

**F3. Allow 8-directional moves.**
> Add the diagonal directions.

---

## 9. Running the file

```bash
java M08_WordSearch.java      # 5/5 tests passed (including 1,000 random boards vs plain backtracking)
```

**One-line summary:** *"From every cell, backtrack: match the next letter, mark the cell '#', try the 4 neighbours, then restore it; prune with letter counts and by starting from the rarer end."*
