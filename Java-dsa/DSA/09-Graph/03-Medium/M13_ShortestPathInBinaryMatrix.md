# M13 · Shortest Path in Binary Matrix

| | |
|---|---|
| **LeetCode** | [#1091 Shortest Path in Binary Matrix](https://leetcode.com/problems/shortest-path-in-binary-matrix/) |
| **Difficulty** | Medium |
| **Pattern** | BFS on a grid with 8 directions (an unweighted shortest path) |
| **Code** | [M13_ShortestPathInBinaryMatrix.java](M13_ShortestPathInBinaryMatrix.java) |
| **Run** | `java M13_ShortestPathInBinaryMatrix.java` |

BFS basics are explained in [E01](../02-Easy/E01_FindIfPathExists.md#34-bfs-breadth-first-search-with-a-queue); the level-by-level BFS in [M07 Rotting Oranges](M07_RottingOranges.md).

---

## 1. The problem

An n × n grid of 0 (open) and 1 (blocked). Return the length (the number of **cells**) of the shortest path from the top-left to the bottom-right, moving in **8 directions**; −1 if there's none.

| grid | answer |
|---|---|
| `[[0,1],[1,0]]` | 2 (one diagonal step) |
| `[[0,0,0],[1,1,0],[1,1,0]]` | 4 |
| `[[1,0,0],[1,1,0],[1,1,0]]` | −1 (the start is blocked) |
| `[[0]]` | 1 |

---

## 2. How to think about it

### Every move costs the same → BFS
The first time BFS reaches a cell, it's via the fewest steps. DFS would **not** give that: it finds *a* path, not the shortest, and exploring every path is exponential.

### The details
- 8 neighbours: every (dr, dc) in {−1, 0, 1}², except (0, 0)
- if the start or the end is blocked → −1 immediately
- mark cells visited **when enqueueing**
- the answer counts **cells**, so the start has distance 1

### Dry run: `[[0,0,0],[1,1,0],[1,1,0]]` (from the real run)

| distance (cells) | BFS level |
|---|---|
| 1 | (0, 0) |
| 2 | (0, 1) |
| 3 | (0, 2), (1, 2) (reached diagonally from (0, 1)) |
| 4 | (2, 2) → the target → **4** |

(A* search with a heuristic such as the Chebyshev distance can be faster in practice; BFS is what interviewers expect.)

---

## 3. Java concepts used in this problem

### 3.1 Eight directions with two nested loops

```java
for (int dr = -1; dr <= 1; dr++)
    for (int dc = -1; dc <= 1; dc++) { int r = p[0] + dr, c = p[1] + dc; ... }
```

It covers all 9 offsets. (dr, dc) = (0, 0) is harmless: it points back at the current cell, which is already `seen`, so it's skipped.

### 3.2 Distance by levels

```java
int dist = 1;
while (!q.isEmpty()) {
    for (int i = q.size(); i > 0; i--) { ...if (target) return dist; ... }
    dist++;
}
```

Each level of the BFS is one more cell on the path. (An alternative is storing the distance inside each queue entry: `{r, c, dist}`.)

### 3.3 The referee: relaxation until nothing changes

The test computes distances by repeatedly scanning the grid and lowering `d[neighbour]` to `d[cell] + 1` until no value changes, which is Bellman-Ford on the grid. It's slow, but obviously correct.

---

## 4. Approach 1: Repeated relaxation (the referee)

### Code ([M13_ShortestPathInBinaryMatrix.java:64-82](M13_ShortestPathInBinaryMatrix.java#L64-L82))

```java
static int relax(int[][] g) {
    int n = g.length, INF = 1_000_000;
    if (g[0][0] == 1) return -1;
    int[][] d = new int[n][n];
    for (int[] row : d) Arrays.fill(row, INF);
    d[0][0] = 1;
    boolean changed = true;
    while (changed) {
        changed = false;
        for (int r = 0; r < n; r++) for (int c = 0; c < n; c++) {
            if (g[r][c] == 1 || d[r][c] == INF) continue;
            for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) {
                int nr = r + dr, nc = c + dc;
                if (nr >= 0 && nr < n && nc >= 0 && nc < n && g[nr][nc] == 0 && d[r][c] + 1 < d[nr][nc]) { d[nr][nc] = d[r][c] + 1; changed = true; }
            }
        }
    }
    return d[n - 1][n - 1] == INF ? -1 : d[n - 1][n - 1];
}
```

Each full sweep can only improve distances; once a sweep changes nothing, every cell holds its shortest distance. Up to O(n²) sweeps of O(n²) cells: **O(n⁴)**.

---

## 5. Approach 2: BFS by levels (the file's main solution)

### Code ([M13_ShortestPathInBinaryMatrix.java:36-61](M13_ShortestPathInBinaryMatrix.java#L36-L61))

```java
static int shortestPathBinaryMatrix(int[][] grid) {
    int n = grid.length;
    if (grid[0][0] == 1 || grid[n - 1][n - 1] == 1) return -1;
    boolean[][] seen = new boolean[n][n];
    Queue<int[]> q = new ArrayDeque<>();
    q.add(new int[]{0, 0});
    seen[0][0] = true;
    int dist = 1;                                          // counting cells
    while (!q.isEmpty()) {
        for (int i = q.size(); i > 0; i--) {
            int[] p = q.poll();
            if (p[0] == n - 1 && p[1] == n - 1) return dist;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int r = p[0] + dr, c = p[1] + dc;
                    if (r >= 0 && r < n && c >= 0 && c < n && grid[r][c] == 0 && !seen[r][c]) {
                        seen[r][c] = true;
                        q.add(new int[]{r, c});
                    }
                }
            }
        }
        dist++;
    }
    return -1;
}
```

### Line by line

| Line | What it does |
|---|---|
| the blocked start/end check | no path is possible |
| `dist = 1` | the path length counts the start cell |
| the level loop | every cell in this level is exactly `dist` cells from the start |
| the target check at poll time | the first time the target comes out of the queue, `dist` is minimal |
| the 8-neighbour loops | queue every open, unseen neighbour, marking it immediately |
| `return -1` | the target's region was exhausted without reaching it |

### Complexity
- **Time O(n²)** (each cell is queued at most once, with 8 neighbour checks).
- **Space O(n²).**

---

## 6. Comparing the approaches

| | Repeated relaxation | BFS |
|---|---|---|
| Time | O(n⁴) | **O(n²)** |
| Space | O(n²) | O(n²) |

The file checks BFS against relaxation on 500 random grids.

---

## 7. Edge cases and common mistakes

**Edge cases:** a 1 × 1 open grid (1), the start or the end blocked, no path, a fully open grid (n, along the diagonal).

**Mistakes:**
1. Only 4 directions.
2. Counting edges instead of cells (an off-by-one).
3. Marking visited at poll time (duplicate queue entries).
4. Forgetting to check that the target itself is open.

---

## 8. Interview questions and answers

**Q1. Why does BFS give the shortest path?**
> It explores cells in non-decreasing distance order: all the cells at distance d are processed before any at distance d + 1. So the first visit to any cell is along a shortest path.

**Q2. What changes if moves have different costs?**
> Then BFS is no longer enough: use Dijkstra (or 0-1 BFS with a deque when the costs are only 0 or 1).

### Follow-up questions

**F1. Return the path itself.**
> Store each cell's parent when it's first queued, and walk back from the target.

**F2. Shortest Path in a Grid with Obstacles Elimination (LC 1293).**
> BFS over the states (r, c, eliminations left).

**F3. Path With Minimum Effort.** → [M14](M14_PathWithMinimumEffort.md)
> Weighted moves (max-of-edges cost): Dijkstra.

---

## 9. Running the file

```bash
java M13_ShortestPathInBinaryMatrix.java      # 5/5 tests passed (including 500 random grids vs relaxation)
```

**One-line summary:** *"Unweighted moves → BFS level by level over the 8 neighbours, marking cells when queued; the level where the bottom-right cell is polled is the answer."*
