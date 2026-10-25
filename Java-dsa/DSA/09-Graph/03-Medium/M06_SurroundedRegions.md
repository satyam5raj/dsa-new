# M06 · Surrounded Regions

| | |
|---|---|
| **LeetCode** | [#130 Surrounded Regions](https://leetcode.com/problems/surrounded-regions/) |
| **Difficulty** | Medium |
| **Pattern** | Protect what touches the border first, then flip everything else |
| **Code** | [M06_SurroundedRegions.java](M06_SurroundedRegions.java) |
| **Run** | `java M06_SurroundedRegions.java` |

The grid DFS template is in [E02 Flood Fill](../02-Easy/E02_FloodFill.md); the "search from the border" idea in [M05](M05_PacificAtlanticWaterFlow.md).

---

## 1. The problem

A board of 'X' and 'O'. **Capture** every region of 'O' that is completely surrounded by 'X' (flip it to 'X'). Regions that touch the border survive.

```
X X X X        X X X X
X O O X   →    X X X X
X X O X        X X X X
X O X X        X O X X     ← the bottom O touches the border: safe
```

| board | result |
|---|---|
| the example | as above |
| `OOO / OXO / OOO` | unchanged (all the O's touch the border) |

---

## 2. How to think about it

### The direct approach (awkward)
For each region, check whether it reaches the border, then flip it if not. That needs a second pass over each region.

### Reverse it (as in Pacific Atlantic)
An 'O' survives ⇔ it's connected to a **border** 'O'. So:
1. DFS from every border 'O', marking its whole region as **safe** ('S')
2. sweep the board: any remaining 'O' → 'X' (captured), and 'S' → 'O' (restored)

The 'S' marks live on the board itself: no extra memory.

```
after step 1 (from the border):     after step 2:
X X X X                             X X X X
X O O X                             X X X X
X X O X                             X X X X
X S X X                             X O X X
```

---

## 3. Java concepts used in this problem

### 3.1 A temporary third state on the board

Using `'S'` as a marker avoids a separate `boolean[][]`. The final sweep converts every cell back to one of the two legal characters.

### 3.2 `if / else if` in the sweep

```java
if (row[c] == 'O') row[c] = 'X';
else if (row[c] == 'S') row[c] = 'O';
```

The `else` guarantees that each cell changes at most once. It matters if the checks are reordered: with `if ('S') → 'O'; if ('O') → 'X'` and no `else`, a cell just restored to 'O' would be captured immediately.

### 3.3 A for-each over the rows

```java
for (char[] row : board) for (int c = 0; c < C; c++) ...
```

`row` refers to the actual row array, so `row[c] = …` modifies the board.

### 3.4 `List.<int[]>of(...)` (in the referee)

The explicit type witness `<int[]>` tells Java the element type when creating a list containing one array.

---

## 4. Approach 1: Region by region (the referee)

### Code ([M06_SurroundedRegions.java:56-76](M06_SurroundedRegions.java#L56-L76))

```java
static void brute(char[][] b) {
    int R = b.length, C = b[0].length;
    boolean[][] seen = new boolean[R][C];
    for (int r = 0; r < R; r++) for (int c = 0; c < C; c++) {
        if (b[r][c] != 'O' || seen[r][c]) continue;
        List<int[]> region = new ArrayList<>();
        Deque<int[]> st = new ArrayDeque<>(List.<int[]>of(new int[]{r, c}));
        seen[r][c] = true;
        boolean border = false;
        while (!st.isEmpty()) {
            int[] p = st.pop();
            region.add(p);
            if (p[0] == 0 || p[1] == 0 || p[0] == R - 1 || p[1] == C - 1) border = true;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nr = p[0] + d[0], nc = p[1] + d[1];
                if (nr >= 0 && nr < R && nc >= 0 && nc < C && b[nr][nc] == 'O' && !seen[nr][nc]) { seen[nr][nc] = true; st.push(new int[]{nr, nc}); }
            }
        }
        if (!border) for (int[] p : region) b[p[0]][p[1]] = 'X';
    }
}
```

| Line | What it does |
|---|---|
| the outer loops | find each unvisited 'O' region |
| the stack loop | collect the whole region, noting whether any cell is on the border |
| `if (!border)` | flip the region only if it never touched the border |

Also O(R · C), but it stores every region's cells; it's the file's referee.

---

## 5. Approach 2: Mark the border regions safe, then sweep (the file's main solution)

### Code ([M06_SurroundedRegions.java:34-53](M06_SurroundedRegions.java#L34-L53))

```java
static void solve(char[][] board) {
    int R = board.length, C = board[0].length;
    for (int r = 0; r < R; r++) { markSafe(board, r, 0); markSafe(board, r, C - 1); }
    for (int c = 0; c < C; c++) { markSafe(board, 0, c); markSafe(board, R - 1, c); }
    for (char[] row : board) {
        for (int c = 0; c < C; c++) {
            if (row[c] == 'O') row[c] = 'X';         // never reached from the border: captured
            else if (row[c] == 'S') row[c] = 'O';    // safe: restore
        }
    }
}

static void markSafe(char[][] b, int r, int c) {
    if (r < 0 || r >= b.length || c < 0 || c >= b[0].length || b[r][c] != 'O') return;
    b[r][c] = 'S';
    markSafe(b, r + 1, c);
    markSafe(b, r - 1, c);
    markSafe(b, r, c + 1);
    markSafe(b, r, c - 1);
}
```

### Line by line

| Line | What it does |
|---|---|
| the border loops | start a DFS from every cell on the 4 edges |
| `markSafe` | flood an 'O' region into 'S' (it stops at 'X' and at cells already marked) |
| the sweep | an 'O' that was never reached is enclosed → 'X'; 'S' → back to 'O' |

### Dry run: the example
- border 'O's: only (3, 1) → marked 'S' (its neighbours are all 'X')
- sweep: (1, 1), (1, 2), (2, 2) are still 'O' → captured; (3, 1) is 'S' → restored to 'O' ✓

### Complexity
- **Time O(R · C).**
- **Space O(R · C)** worst case for the recursion.

---

## 6. Comparing the approaches

| | Region by region | Border first |
|---|---|---|
| Time | O(R · C) | O(R · C) |
| Extra memory | seen + the region lists | none besides the recursion |
| Code | longer | short |

The file checks the border-first solution against the region-by-region one on 500 random boards.

---

## 7. Edge cases and common mistakes

**Edge cases:** a 1 × 1 board, a board that's all 'O', boards with 1 or 2 rows (everything touches the border).

**Mistakes:**
1. Flipping 'O's adjacent to the border without following the whole region.
2. Forgetting to restore the 'S' marks.
3. Recursive DFS on a 200 × 200 all-'O' board (deep recursion) → use a stack or BFS.

---

## 8. Interview questions and answers

**Q1. Why start from the border?**
> Survival is defined by connection to the border. Marking everything reachable from it identifies exactly the survivors, and every other 'O' must be enclosed.

**Q2. How could you solve it with Union-Find?**
> Create a virtual "border" node; union every border 'O' with it, and every pair of adjacent 'O's. At the end, flip each 'O' not connected to the virtual node.

### Follow-up questions

**F1. Number of Enclaves (LC 1020): count the land cells that can't reach the border.**
> The same border-first flood, then count what remains.

**F2. Number of Closed Islands (LC 1254).**
> Sink the islands touching the border, then count the remaining islands.

**F3. Pacific Atlantic Water Flow.** → [M05](M05_PacificAtlanticWaterFlow.md)
> The same "from the border inward" idea with a height condition.

---

## 9. Running the file

```bash
java M06_SurroundedRegions.java      # 3/3 tests passed (including 500 random boards vs region-by-region)
```

**One-line summary:** *"Flood every border-connected 'O' region into 'S', then sweep: the remaining 'O' → 'X' (captured), 'S' → 'O' (safe)."*
