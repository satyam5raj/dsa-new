# A08 · Shortest Path in a Grid with Obstacles Elimination ⭐

| | |
|---|---|
| **LeetCode** | [#1293 Shortest Path in a Grid with Obstacles Elimination](https://leetcode.com/problems/shortest-path-in-a-grid-with-obstacles-elimination/) |
| **Difficulty** | Hard |
| **Mix of structures** | **BFS (queue)** over **states** (row, col, eliminations left) + a visited table for states |
| **Code** | [A08_ShortestPathWithObstaclesElimination.java](A08_ShortestPathWithObstaclesElimination.java) |
| **Run** | `java A08_ShortestPathWithObstaclesElimination.java` |

⭐ = not in `leetcode.md`, added because it teaches "BFS on states", one of the most useful ideas in grid problems.

---

## 1. The problem

An `m × n` grid of `0` (empty) and `1` (obstacle). You move up, down, left or right. You may walk through **at most k obstacles** (eliminating them). Return the minimum number of steps from the top-left to the bottom-right, or −1.

```
grid = [[0,0,0],
        [1,1,0],
        [0,0,0],
        [0,1,1],
        [0,0,0]],   k = 1   →  6     (break the obstacle at (3,2))
                    k = 0   →  10    (walk all the way around)
```

---

## 2. How to think about it

### Plain BFS isn't enough
For a normal grid, BFS with `visited[r][c]` gives the shortest path ([Graph M13](../../09-Graph/03-Medium/M13_ShortestPathInBinaryMatrix.md)). Here it fails: arriving at a cell with **0 eliminations left** is worse than arriving at it with **1 left**, even if it takes longer, because the second one can still break a wall later. If `visited[r][c]` blocks the second arrival, we lose the only path that works.

### Put the "resources" into the state
```
state = (r, c, remaining eliminations)          at most m · n · (k + 1) states
```
BFS over **states** still finds the fewest steps, because every move costs exactly 1. Two states at the same cell with different `remaining` are different nodes in the graph.

This is the general trick: **when the same position can be reached in situations that behave differently later, add that situation to the state** (keys collected, bits visited, fuel left, …).

### Approach 1: a 3-D visited array
`seen[r][c][remaining]`: each state is visited at most once.

### Approach 2: keep only the best "remaining" per cell
BFS processes states **level by level** (by number of steps). If we already reached a cell at an earlier or equal step with **at least as many** eliminations left, a new arrival with fewer (or the same) can't do anything better: it's **dominated**. So one number per cell is enough:

```
best[r][c] = the most eliminations left we've ever arrived here with
skip a new state if remaining <= best[r][c]
```

### A shortcut
The shortest conceivable path has `m + n − 2` steps and passes through `m + n − 3` cells between the two corners. If `k >= m + n − 3`, we can bulldoze straight along that path, so the answer is `m + n − 2` immediately.

### Dry run: example 1, k = 1 (Approach 2, from the real BFS)
| level (steps) | states (r, c, left) entering this level |
|---|---|
| 0 | (0,0,1) |
| 1 | (1,0,**0**) through a wall · (0,1,1) |
| 2 | (2,0,0) · (1,1,0) · (0,2,1) |
| 3 | (3,0,0) · (2,1,0) · (1,2,0) · (1,2,**1**) ← the same cell again, but with more left: kept |
| 4 | (4,0,0) · (2,2,0) · (2,2,1) |
| 5 | (4,1,0) · (3,2,0) through a wall · (2,1,1) |
| 6 | (4,2,0) ← **the target** · … |

Answer **6**. Notice level 3: plain `visited[r][c]` would have thrown away `(1,2,1)`.

---

## 3. Java concepts used in this problem

### 3.1 BFS level by level
```java
for (int steps = 0; !queue.isEmpty(); steps++) {
    for (int size = queue.size(); size > 0; size--) {
        int[] cur = queue.poll();
        ...
    }
}
```
`queue.size()` is read **once**, at the start of each level, so the inner loop processes exactly the states that are `steps` away; states added during the loop belong to the next level. That's how `steps` stays correct without storing a distance per state.

### 3.2 States as small arrays
`new int[]{r, c, remaining}` packs a state into one object for the queue. An alternative is a record, `record State(int r, int c, int k)`, which is more readable but slightly heavier.

### 3.3 A 3-D boolean array
```java
boolean[][][] seen = new boolean[m][n][k + 1];
```
`seen[r][c][left]`, with indices `0..k` for `left`. Memory: m · n · (k + 1) booleans (40 × 40 × 1,601 ≈ 2.5 MB in the worst LeetCode case).

### 3.4 `Arrays.fill` on each row
```java
for (int[] row : best) Arrays.fill(row, -1);
```
`Arrays.fill` works on one-dimensional arrays only, so fill each row. −1 means "never reached", which is smaller than any real `remaining` (≥ 0).

---

## 4. The referee: try every simple path (tiny grids)

### Code ([A08_ShortestPathWithObstaclesElimination.java:43-65](A08_ShortestPathWithObstaclesElimination.java#L43-L65))
```java
static int shortestPathBrute(int[][] grid, int k) {
    int m = grid.length, n = grid[0].length;
    boolean[][] onPath = new boolean[m][n];
    onPath[0][0] = true;
    int best = dfs(grid, 0, 0, k, 0, onPath);
    return best == Integer.MAX_VALUE ? -1 : best;
}

private static int dfs(int[][] grid, int r, int c, int left, int steps, boolean[][] onPath) {
    int m = grid.length, n = grid[0].length;
    if (r == m - 1 && c == n - 1) return steps;
    int best = Integer.MAX_VALUE;
    for (int[] d : DIRS) {
        int nr = r + d[0], nc = c + d[1];
        if (nr < 0 || nr >= m || nc < 0 || nc >= n || onPath[nr][nc]) continue;
        int nl = left - grid[nr][nc];
        if (nl < 0) continue;
        onPath[nr][nc] = true;
        best = Math.min(best, dfs(grid, nr, nc, nl, steps + 1, onPath));
        onPath[nr][nc] = false;
    }
    return best;
}
```

A shortest path never visits a cell twice, so trying **every simple path** (backtracking with `onPath`) finds the true answer. It's exponential, so the tests only use grids up to 4 × 4.

---

## 5. Approach 1: BFS with a 3-D visited array

### Code ([A08_ShortestPathWithObstaclesElimination.java:68-89](A08_ShortestPathWithObstaclesElimination.java#L68-L89))
```java
static int shortestPath3D(int[][] grid, int k) {
    int m = grid.length, n = grid[0].length;
    boolean[][][] seen = new boolean[m][n][k + 1];
    Deque<int[]> queue = new ArrayDeque<>();                         // {r, c, remaining}
    queue.add(new int[]{0, 0, k});
    seen[0][0][k] = true;
    for (int steps = 0; !queue.isEmpty(); steps++) {
        for (int size = queue.size(); size > 0; size--) {
            int[] cur = queue.poll();
            if (cur[0] == m - 1 && cur[1] == n - 1) return steps;
            for (int[] d : DIRS) {
                int nr = cur[0] + d[0], nc = cur[1] + d[1];
                if (nr < 0 || nr >= m || nc < 0 || nc >= n) continue;
                int left = cur[2] - grid[nr][nc];
                if (left < 0 || seen[nr][nc][left]) continue;
                seen[nr][nc][left] = true;
                queue.add(new int[]{nr, nc, left});
            }
        }
    }
    return -1;
}
```

| Line | What it does |
|---|---|
| start state `(0, 0, k)` | the top-left is guaranteed to be empty |
| reaching the target | return the current level, which is the first time we get there |
| `left = cur[2] - grid[nr][nc]` | stepping onto an obstacle uses one elimination |
| `left < 0` | not allowed |
| `seen[nr][nc][left]` | this exact state was already queued |

**Cost:** O(m · n · k) time and space.

---

## 6. Approach 2: BFS with the best "remaining" per cell (main solution)

### Code ([A08_ShortestPathWithObstaclesElimination.java:92-115](A08_ShortestPathWithObstaclesElimination.java#L92-L115))
```java
static int shortestPath(int[][] grid, int k) {
    int m = grid.length, n = grid[0].length;
    if (k >= m + n - 3) return m + n - 2;                            // enough to bulldoze any straight route
    int[][] best = new int[m][n];
    for (int[] row : best) Arrays.fill(row, -1);
    Deque<int[]> queue = new ArrayDeque<>();
    queue.add(new int[]{0, 0, k});
    best[0][0] = k;
    for (int steps = 0; !queue.isEmpty(); steps++) {
        for (int size = queue.size(); size > 0; size--) {
            int[] cur = queue.poll();
            if (cur[0] == m - 1 && cur[1] == n - 1) return steps;
            for (int[] d : DIRS) {
                int nr = cur[0] + d[0], nc = cur[1] + d[1];
                if (nr < 0 || nr >= m || nc < 0 || nc >= n) continue;
                int left = cur[2] - grid[nr][nc];
                if (left <= best[nr][nc]) continue;                  // been here sooner with at least as many left
                best[nr][nc] = left;
                queue.add(new int[]{nr, nc, left});
            }
        }
    }
    return -1;
}
```

### What changes from Approach 1
| Line | What it does |
|---|---|
| the shortcut | `k ≥ m + n − 3` → a straight route always works. For a 1 × 1 grid this gives 0 ✓ |
| `best[r][c] = -1` | never reached |
| `left <= best[nr][nc] → skip` | dominated: an earlier-or-same-level visit had at least this much left. This also rejects `left < 0`, since `best` is at least −1 |
| `best[nr][nc] = left` | the new record for this cell |

### Complexity
- A cell can be enqueued at most k + 1 times (each time with strictly more remaining), so the time is still **O(m · n · k)** in the worst case, usually far less.
- Memory: **O(m · n)** for `best`, plus the queue.

---

## 7. Comparing the approaches

| | Time | Extra memory |
|---|---|---|
| Every simple path | exponential | O(m · n) |
| BFS, 3-D seen | O(m · n · k) | O(m · n · k) |
| **BFS, best per cell + shortcut** | O(m · n · k) worst, usually less | **O(m · n)** |

The file checks both BFS versions against the exhaustive search on 1,500 random small grids.

---

## 8. Edge cases and common mistakes

**Edge cases:** a 1 × 1 grid (0 steps), k = 0 (plain BFS), k big enough for the shortcut, no path at all (−1), obstacles that must be broken in sequence.

**Mistakes:**
1. `visited[r][c]` without the remaining count (wrong answers).
2. Using DFS: it doesn't find shortest paths without exploring everything.
3. Counting the level wrong (reading `queue.size()` inside the loop condition every iteration).
4. Forgetting `k + 1` in the 3-D array size (indices 0..k).
5. With `best`, skipping only when `left < best` instead of `<=`. Equal states then get re-queued again and again, which is an infinite loop when the target can't be reached.

---

## 9. Interview questions and answers

**Q1. Why does plain BFS fail?**
> Reaching a cell with more eliminations left is a genuinely different situation, since it can still pass walls later. The state must be (r, c, remaining), otherwise BFS discards paths that are longer now but the only ones that can finish.

**Q2. Why is BFS still optimal on states?**
> Every move costs 1 regardless of the state, so BFS explores states in order of steps, and the first time any state at the target is dequeued, its level is the minimum.

**Q3. Explain the dominance pruning.**
> BFS visits levels in increasing order. If a cell was reached at an earlier or equal step with at least as many eliminations, any continuation from the new arrival can be copied from the old one, at least as fast and with at least as many eliminations. So the new arrival is useless.

### Follow-up questions

**F1. "Different costs per move (e.g. breaking a wall costs 3 steps)."**
> Then BFS doesn't work; use Dijkstra on the same states, or 0-1 BFS if costs are 0 or 1.

**F2. "Collect keys to open doors (Shortest Path to Get All Keys)."**
> The same idea: state = (r, c, bitmask of keys). See [A09](A09_ShortestPathVisitingAllNodes.md) for bitmask states.

**F3. "Return the actual path."**
> Store a parent pointer per state and walk back from the target.

---

## 10. Running the file

```bash
java A08_ShortestPathWithObstaclesElimination.java     # 6/6 tests passed (examples, k=0, blocked, 1x1, shortcut, 1,500 random grids)
```

**One-line summary:** *"Do BFS over states (row, col, eliminations left) instead of cells, because arriving with more eliminations is a different and possibly better situation. Prune a state when the cell was already reached with at least as many left, and return m+n−2 straight away when k ≥ m+n−3."*
