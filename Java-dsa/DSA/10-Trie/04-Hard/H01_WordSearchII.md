# H01 · Word Search II

| | |
|---|---|
| **LeetCode** | [#212 Word Search II](https://leetcode.com/problems/word-search-ii/) |
| **Difficulty** | Hard |
| **Pattern** | Trie + grid backtracking: search **all** the words in one pass, pruning by prefix |
| **Code** | [H01_WordSearchII.java](H01_WordSearchII.java) |
| **Run** | `java H01_WordSearchII.java` |

The single-word search is [M08 Word Search](../03-Medium/M08_WordSearch.md); trie basics are in [E01](../02-Easy/E01_CountingWordsWithPrefix.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Given a board of letters and a list of words, return every word that can be traced in the board (adjacent cells, each cell used once per word).

```
o a a n
e t a e        words [oath, pea, eat, rain]  →  [eat, oath]
i h k r
i f l v
```

**Constraints:** up to 12 × 12 boards and 3 · 10⁴ words of length ≤ 10.

---

## 2. How to think about it

### Naive
Run Word Search ([M08](../03-Medium/M08_WordSearch.md)) once per word: O(W · R · C · 3^L). With 3 · 10⁴ words that's hopeless, because the same board paths are explored again and again.

### A trie of all the words
DFS the board **once** from every cell, walking the trie in parallel. The trie node says which words are still possible:
- the next letter isn't a child of the current node → **no** word continues this way → stop immediately (this pruning is the whole speed-up)
- a node that ends a word → record it

### The optimisations (these turn a TLE into an AC)
1. Store the full word in its end node (no StringBuilder needed).
2. After finding a word, set `node.word = null`: each word is reported once.
3. **Prune the trie:** after exploring a child, if it has no children and no word left, remove it. Later searches don't walk dead branches.
4. Mark the visited cells in place (`'#'`) and restore them afterwards.

### Dry run: the example (from the real run)
```
found "oath" via (0,0) o → (0,1) a → (1,1) t → (2,1) h
found "eat"  via (1,3) e → (1,2) a → (1,1) t
"pea": no 'p' on the board → the trie branch is never entered
"rain": r(2,3) → a? its neighbours are e, k, v → no 'a' → pruned at once
→ [oath, eat]
```

---

## 3. Java concepts used in this problem

### 3.1 Passing the trie node through the recursion

```java
static void dfs(char[][] b, int r, int c, Node parent, List<String> found)
```

Each call receives the **parent** trie node and looks up the child for the current cell's letter. That also makes the pruning possible (the parent can drop the child).

### 3.2 Removing a dead branch

```java
if (n.word == null && isLeaf(n)) parent.next[ch - 'a'] = null;
```

Once every word below a node has been found, the node is useless: unlinking it means future DFS calls stop one step earlier. The trie shrinks as the words are found.

### 3.3 `new LinkedHashSet<>(Arrays.asList(words))` (in the referee)

It removes duplicate words while keeping their order, so the per-word referee reports each word once, like the trie version.

---

## 4. Approach 1: One Word Search per word (the referee)

### Code ([H01_WordSearchII.java:89-107](H01_WordSearchII.java#L89-L107))

```java
static List<String> perWord(char[][] b, String[] words) {
    List<String> out = new ArrayList<>();
    for (String w : new LinkedHashSet<>(Arrays.asList(words))) {
        boolean f = false;
        for (int r = 0; r < b.length && !f; r++) for (int c = 0; c < b[0].length && !f; c++) f = one(b, w, r, c, 0);
        if (f) out.add(w);
    }
    return out;
}

static boolean one(char[][] b, String w, int r, int c, int i) {
    if (i == w.length()) return true;
    if (r < 0 || r >= b.length || c < 0 || c >= b[0].length || b[r][c] != w.charAt(i)) return false;
    char s = b[r][c];
    b[r][c] = '#';
    boolean f = one(b, w, r + 1, c, i + 1) || one(b, w, r - 1, c, i + 1) || one(b, w, r, c + 1, i + 1) || one(b, w, r, c - 1, i + 1);
    b[r][c] = s;
    return f;
}
```

M08's backtracking repeated for every word: **O(W · R · C · 3^L)**. Correct, and the file's referee.

---

## 5. Approach 2: Trie-guided DFS with pruning (the file's main solution)

### Code ([H01_WordSearchII.java:42-86](H01_WordSearchII.java#L42-L86))

```java
static class Node {
    Node[] next = new Node[26];
    String word;                                         // non-null: a word ends here
}

static List<String> findWords(char[][] board, String[] words) {
    Node root = new Node();
    for (String w : words) {
        Node n = root;
        for (char c : w.toCharArray()) {
            if (n.next[c - 'a'] == null) n.next[c - 'a'] = new Node();
            n = n.next[c - 'a'];
        }
        n.word = w;
    }
    List<String> found = new ArrayList<>();
    for (int r = 0; r < board.length; r++)
        for (int c = 0; c < board[0].length; c++)
            dfs(board, r, c, root, found);
    return found;
}

static void dfs(char[][] b, int r, int c, Node parent, List<String> found) {
    if (r < 0 || r >= b.length || c < 0 || c >= b[0].length) return;
    char ch = b[r][c];
    if (ch == '#') return;                               // already on the current path
    Node n = parent.next[ch - 'a'];
    if (n == null) return;                               // no word continues this way: prune
    if (n.word != null) {
        found.add(n.word);
        n.word = null;                                   // report each word once
    }
    b[r][c] = '#';
    dfs(b, r + 1, c, n, found);
    dfs(b, r - 1, c, n, found);
    dfs(b, r, c + 1, n, found);
    dfs(b, r, c - 1, n, found);
    b[r][c] = ch;
    if (n.word == null && isLeaf(n)) parent.next[ch - 'a'] = null;   // prune exhausted branches
}

static boolean isLeaf(Node n) {
    for (Node x : n.next) if (x != null) return false;
    return true;
}
```

### Line by line

| Line | What it does |
|---|---|
| the build | a trie of all the words, with each word stored at its end node |
| the outer loops | start a DFS from every cell, at the trie root |
| `ch == '#'` | the cell is on the current path |
| `parent.next[ch − 'a'] == null` | no remaining word has this prefix → stop |
| `n.word != null` | a complete word → record it once, then forget it |
| mark / 4 calls / unmark | M08's backtracking, with the trie node carried along |
| the pruning line | a node with no children and no word is removed from its parent |

### Complexity
- **Worst case O(R · C · 4 · 3^(L−1))**, far less in practice thanks to the pruning.
- **Space O(total characters of the words).**

---

## 6. Comparing the approaches

| | One search per word | Trie-guided DFS |
|---|---|---|
| Board exploration | repeated W times | **once**, shared by all the words |
| Dead prefixes | explored per word | cut off immediately |
| 3 · 10⁴ words | hopeless | fast |

The file checks the trie version against one search per word on 500 random boards (with duplicate words).

---

## 7. Edge cases and common mistakes

**Edge cases:** duplicate words (report once), a word that's a prefix of another (both can be found on the same path), a single-cell board, words longer than the board.

**Mistakes:**
1. Returning after finding a word (longer words through the same node are missed).
2. Not nulling `node.word` (duplicates in the output).
3. Forgetting to restore the cell.
4. Pruning a node that still holds a word, or still has children.

---

## 8. Interview questions and answers

**Q1. Why is a trie better than a HashSet of words here?**
> A HashSet can only say whether a complete string is a word. The trie also says "no word starts with this prefix", which lets the DFS abandon a path as soon as it goes wrong.

**Q2. What does the pruning step buy?**
> As words are found, their branches disappear, so later DFS calls (from other cells) stop earlier. On boards with many repeated letters, this is the difference between timing out and passing.

### Follow-up questions

**F1. Count the occurrences of each word.**
> Don't null the word; increment a counter at each find (and don't prune).

**F2. Boggle with 8-directional moves.**
> Add the diagonals.

**F3. Word Search (one word).** → [M08](../03-Medium/M08_WordSearch.md)

---

## 9. Running the file

```bash
java H01_WordSearchII.java      # 4/4 tests passed (including 500 random boards vs one search per word)
```

**One-line summary:** *"Build a trie of all the words, then backtrack from every cell while walking the trie: stop when no child matches, record and clear words at their end nodes, and prune exhausted branches."*
