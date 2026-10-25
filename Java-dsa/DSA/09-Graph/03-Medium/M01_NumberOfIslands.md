# M01 · Number of Islands

| | |
|---|---|
| **LeetCode** | [#200 Number of Islands](https://leetcode.com/problems/number-of-islands/) |
| **Difficulty** | Medium |
| **Pattern** | Count the connected components in a grid (each unvisited '1' starts one) |
| **Code** | [M01_NumberOfIslands.java](M01_NumberOfIslands.java) |
| **Run** | `java M01_NumberOfIslands.java` |

Graph basics are explained in [E01](../02-Easy/E01_FindIfPathExists.md#3-java-concepts-used-in-this-problem); the grid DFS template in [E02 Flood Fill](../02-Easy/E02_FloodFill.md).

---

## 1. The problem

'1' = land, '0' = water. An island is a group of land cells connected **horizontally or vertically**. Count the islands.

```
1 1 0 0 0
1 1 0 0 0          → 3
0 0 1 0 0
0 0 0 1 1
```

| grid | answer |
|---|---|
| the example | 3 |
| `11110 / 11010 / 11000 / 00000` | 1 |
| `10 / 01` | 2 (diagonals don't connect) |

---

## 2. How to think about it

### It's "count the connected components" on an implicit grid graph
```
for every cell:
    if it's unvisited land:
        count++
        flood the WHOLE island (DFS/BFS), so none of its cells starts another count
```

"Sinking" the island (overwriting '1' with '0') doubles as the visited marker. Ask whether you may modify the input; if not, use a `boolean[][] visited`.

### Three ways, all O(R · C)
- **DFS** (the shortest code)
- **BFS** (no deep recursion)
- **Union-Find**: union the adjacent land cells; count = the number of roots ([11-Union-Find](../../11-Union-Find/README.md))

### Dry run
```
(0, 0) is land → count 1, sink the 2×2 block
(2, 2) is land → count 2
(3, 3) is land → count 3, and sinking it also sinks (3, 4)
```

---

## 3. Java concepts used in this problem

### 3.1 `char[][]` grids

The input uses characters `'1'` and `'0'`, not ints, so compare with `'1'` (single quotes = a `char`). `"1"` would be a String and wouldn't compile here.

### 3.2 Building a test grid from strings

```java
static char[][] grid(String... rows) { return Arrays.stream(rows).map(String::toCharArray).toArray(char[][]::new); }
```

Each row string becomes a `char[]` with `toCharArray`. A varargs parameter (`String...`) lets the test write `grid("11000", "11000", ...)`.

### 3.3 Separate `visited` vs mutating the input

The DFS version sinks the land (mutating the input); the BFS version keeps a `seen[][]` array and leaves the grid untouched. The file runs both on copies of the same random grids.

---

## 4. Approach 1: DFS, sinking each island (the file's main solution)

### Code ([M01_NumberOfIslands.java:38-58](M01_NumberOfIslands.java#L38-L58))

```java
static int numIslands(char[][] grid) {
    int count = 0;
    for (int r = 0; r < grid.length; r++) {
        for (int c = 0; c < grid[0].length; c++) {
            if (grid[r][c] == '1') {
                count++;                     // a new island
                sink(grid, r, c);            // erase it so we don't count it again
            }
        }
    }
    return count;
}

static void sink(char[][] g, int r, int c) {
    if (r < 0 || r >= g.length || c < 0 || c >= g[0].length || g[r][c] != '1') return;
    g[r][c] = '0';
    sink(g, r + 1, c);
    sink(g, r - 1, c);
    sink(g, r, c + 1);
    sink(g, r, c - 1);
}
```

### Line by line

| Line | What it does |
|---|---|
| the double loop | try every cell as a possible new island |
| `grid[r][c] == '1'` | land that hasn't been sunk yet → an island nobody has counted |
| `count++` | count it |
| `sink(grid, r, c)` | flood the whole island to '0' |
| `sink`'s guard | outside the grid, or not land → stop |
| `g[r][c] = '0'` | sunk = visited |

### Dry run: the example

| scan reaches | land? | action | count |
|---|---|---|---|
| (0, 0) | yes | sink (0,0), (1,0), (1,1), (0,1) | 1 |
| (0, 1), (1, 0), (1, 1) | already sunk | | 1 |
| (2, 2) | yes | sink (2, 2) | 2 |
| (3, 3) | yes | sink (3, 3), (3, 4) | 3 |
| (3, 4) | already sunk | | 3 |

**3** ✓

### Complexity
- **Time O(R · C):** each cell is sunk at most once, and each is examined a constant number of times.
- **Space O(R · C)** worst case for the recursion (one huge snake-shaped island).

---

## 5. Approach 2: BFS with a seen array

### Code ([M01_NumberOfIslands.java:60-84](M01_NumberOfIslands.java#L60-L84))

```java
static int numIslandsBfs(char[][] grid) {
    int R = grid.length, C = grid[0].length, count = 0;
    boolean[][] seen = new boolean[R][C];
    int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    for (int r = 0; r < R; r++) {
        for (int c = 0; c < C; c++) {
            if (grid[r][c] != '1' || seen[r][c]) continue;
            count++;
            Queue<int[]> q = new ArrayDeque<>();
            q.add(new int[]{r, c});
            seen[r][c] = true;
            while (!q.isEmpty()) {
                int[] p = q.poll();
                for (int[] d : dirs) {
                    int nr = p[0] + d[0], nc = p[1] + d[1];
                    if (nr >= 0 && nr < R && nc >= 0 && nc < C && grid[nr][nc] == '1' && !seen[nr][nc]) {
                        seen[nr][nc] = true;
                        q.add(new int[]{nr, nc});
                    }
                }
            }
        }
    }
    return count;
}
```

### Line by line

| Line | What it does |
|---|---|
| `seen[][]` | the visited marker; the grid isn't modified |
| `continue` for water or seen cells | only unvisited land starts a new island |
| the BFS | the same flood, level by level, with a queue |
| the long condition | in bounds, land, not yet seen → enqueue and mark |

### Complexity
- **Time O(R · C).**
- **Space O(R · C)** for `seen`; the queue holds at most O(min(R, C))-ish frontier cells for typical shapes, O(R · C) worst case.

---

## 6. Approach 3: Union-Find

```java
static int numIslandsUnionFind(char[][] grid) {
    int R = grid.length, C = grid[0].length;
    int[] parent = new int[R * C];
    int count = 0;
    for (int i = 0; i < R * C; i++) parent[i] = i;
    for (int r = 0; r < R; r++)
        for (int c = 0; c < C; c++) {
            if (grid[r][c] != '1') continue;
            count++;                                              // every land cell starts as its own island
            if (r > 0 && grid[r - 1][c] == '1' && union(parent, r * C + c, (r - 1) * C + c)) count--;
            if (c > 0 && grid[r][c - 1] == '1' && union(parent, r * C + c, r * C + c - 1)) count--;
        }
    return count;
}

static int find(int[] p, int x) { while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; } return x; }

static boolean union(int[] p, int a, int b) {
    int ra = find(p, a), rb = find(p, b);
    if (ra == rb) return false;
    p[ra] = rb;
    return true;                                                  // two islands merged into one
}
```

Each cell (r, c) gets the id `r · C + c`. Every land cell starts as its own island; each successful union of two adjacent land cells merges two islands, so the count goes down by 1. (Union-Find gets its own topic: [11-Union-Find](../../11-Union-Find/README.md).)

---

## 7. Comparing the approaches

| | DFS (sink) | BFS (seen) | Union-Find |
|---|---|---|---|
| Time | O(R · C) | O(R · C) | O(R · C · α) |
| Extra space | recursion | seen + queue | parent array |
| Modifies the input? | yes | no | no |
| Handles land added over time? | no | no | **yes** (Number of Islands II) |

The file checks DFS against BFS on 500 random grids.

---

## 8. Edge cases and common mistakes

**Edge cases:** all water (0), all land (1), a single cell, diagonal-only neighbours (separate islands).

**Mistakes:**
1. Counting land cells instead of islands.
2. Including the diagonal neighbours.
3. Comparing `char` with `1` (an int) instead of `'1'`.
4. Recursive DFS on a 300 × 300 all-land grid in a small stack.

---

## 9. Interview questions and answers

**Q1. Why is the total time O(R · C) even though we start many DFS calls?**
> Each land cell is sunk exactly once, and each sunk cell triggers at most 4 more calls that return immediately. The total work over all the islands is proportional to the grid size.

**Q2. What if land is added cell by cell, and you report the count after each addition (LC 305)?**
> Union-Find: add the cell as a new island (count++), then union it with its land neighbours, decrementing the count on each successful union. That's near-O(1) per addition.

### Follow-up questions

**F1. Max area of an island (LC 695).**
> Make `sink` return 1 + the sum of its 4 calls, and take the max.

**F2. Count the islands surrounded by water on all sides (LC 1254, closed islands).**
> First sink everything connected to the border, then count the rest.

**F3. Count the distinct island shapes (LC 694).**
> Record each island's DFS path (or its cells relative to its first cell) as a string in a HashSet.

---

## 10. Running the file

```bash
java M01_NumberOfIslands.java      # 5/5 tests passed (including 500 random grids: DFS == BFS)
```

**One-line summary:** *"Scan the grid; each unvisited land cell starts a new island: count it, then flood-fill (sink) the whole island so it isn't counted again."*
