# A28 · Minimum Cost to Make at Least One Valid Path in a Grid ⭐

| | |
|---|---|
| **LeetCode** | [#1368 Minimum Cost to Make at Least One Valid Path in a Grid](https://leetcode.com/problems/minimum-cost-to-make-at-least-one-valid-path-in-a-grid/) |
| **Difficulty** | Hard |
| **Mix of structures** | grid **graph** + **0-1 BFS** with a **deque** (or Dijkstra with a heap) |
| **Code** | [A28_MinimumCostToMakeValidPath.java](A28_MinimumCostToMakeValidPath.java) |
| **Run** | `java A28_MinimumCostToMakeValidPath.java` |

⭐ = not in `leetcode.md`, added because it's the standard 0-1 BFS problem: shortest paths where every edge costs 0 or 1.

---

## 1. The problem

Every cell has a sign telling you where to go next: `1` = right, `2` = left, `3` = down, `4` = up. Starting at the top-left, you follow the signs. Changing one cell's sign costs 1 (once per cell). Return the minimum total cost so that following the signs reaches the bottom-right.

```
[[1,1,1,1],
 [2,2,2,2],          →  3     change 3 signs to "down" on the way
 [1,1,1,1],
 [2,2,2,2]]

[[1,1,3],
 [3,2,2],            →  0     the signs already lead there
 [1,1,4]]

[[1,2],
 [4,3]]              →  1
```

---

## 2. How to think about it

### As a graph with 0/1 edges
From every cell you can go in 4 directions:
- the direction the sign points: **free** (cost 0);
- any other direction: change the sign, **cost 1**.

So it's a shortest-path problem where every edge costs 0 or 1.

### Dijkstra works…
A min-heap by cost: O(mn log mn).

### …but 0-1 BFS is simpler and faster
With only 0 and 1 costs, a **deque** can replace the heap:
- a free move: `addFirst` (same cost as the current cell, so handle it before anything more expensive);
- a cost-1 move: `addLast`.

At any moment the deque holds costs `d` at the front and `d + 1` at the back, exactly the order a heap would give, but every operation is O(1). Total **O(mn)**.

**The analogy:** a priority queue with only two priorities. "Same cost" jumps to the front of the line; "one more" goes to the back.

### Dry run: `[[1,2],[4,3]]`
Signs: (0,0) = right, (0,1) = left, (1,0) = up, (1,1) = down.

| pop (cost) | moves | deque after |
|---|---|---|
| (0,0) 0 | right → (0,1) cost **0** (free) → **front**; down → (1,0) cost 1 → back | [(0,1)·0, (1,0)·1] |
| (0,1) 0 | left → (0,0) cost 0, not better; down → (1,1) cost 1 → back | [(1,0)·1, (1,1)·1] |
| (1,0) 1 | up → (0,0) no; right → (1,1) cost 2, not better | [(1,1)·1] |
| (1,1) 1 | the target | **1** |

---

## 3. Java concepts used in this problem

### 3.1 `ArrayDeque` as a two-ended queue
`addFirst`, `addLast`, `pollFirst`: all O(1). It's the only structure 0-1 BFS needs.

### 3.2 Mapping signs to directions with an array
```java
static final int[][] DIRS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};       // index + 1 = the sign: 1 right, 2 left, 3 down, 4 up
```
Direction index `d` matches sign `d + 1`, so "is this move free?" is just `grid[r][c] == d + 1`.

### 3.3 Storing the cost in each entry
Entries are `{cost, r, c}`. A cell can be added more than once (first at cost 1, later at cost 0); `if (cur[0] > cost[r][c]) continue;` skips the outdated copy, exactly like stale entries in Dijkstra.

### 3.4 Early return at the target
In 0-1 BFS (as in Dijkstra), the first time the target comes off the **front**, its cost is final.

---

## 4. The referee: relax until nothing changes

### Code ([A28_MinimumCostToMakeValidPath.java:46-66](A28_MinimumCostToMakeValidPath.java#L46-L66))
```java
static int minCostRelax(int[][] grid) {
    int m = grid.length, n = grid[0].length;
    int[][] cost = new int[m][n];
    for (int[] row : cost) Arrays.fill(row, Integer.MAX_VALUE);
    cost[0][0] = 0;
    boolean changed = true;
    while (changed) {
        changed = false;
        for (int r = 0; r < m; r++)
            for (int c = 0; c < n; c++) {
                if (cost[r][c] == Integer.MAX_VALUE) continue;
                for (int d = 0; d < 4; d++) {
                    int nr = r + DIRS[d][0], nc = c + DIRS[d][1];
                    if (nr < 0 || nr >= m || nc < 0 || nc >= n) continue;
                    int nd = cost[r][c] + (grid[r][c] == d + 1 ? 0 : 1);
                    if (nd < cost[nr][nc]) { cost[nr][nc] = nd; changed = true; }
                }
            }
    }
    return cost[m - 1][n - 1];
}
```

Bellman-Ford style: keep relaxing every edge until no cost improves. It's slow (many passes) but obviously correct.

---

## 5. Approach 1: Dijkstra with a heap

### Code ([A28_MinimumCostToMakeValidPath.java:69-88](A28_MinimumCostToMakeValidPath.java#L69-L88))
```java
static int minCostDijkstra(int[][] grid) {
    int m = grid.length, n = grid[0].length;
    int[][] cost = new int[m][n];
    for (int[] row : cost) Arrays.fill(row, Integer.MAX_VALUE);
    cost[0][0] = 0;
    PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));   // {cost, r, c}
    heap.add(new int[]{0, 0, 0});
    while (!heap.isEmpty()) {
        int[] cur = heap.poll();
        int r = cur[1], c = cur[2];
        if (cur[0] > cost[r][c]) continue;                            // stale
        for (int d = 0; d < 4; d++) {
            int nr = r + DIRS[d][0], nc = c + DIRS[d][1];
            if (nr < 0 || nr >= m || nc < 0 || nc >= n) continue;
            int nd = cur[0] + (grid[r][c] == d + 1 ? 0 : 1);
            if (nd < cost[nr][nc]) { cost[nr][nc] = nd; heap.add(new int[]{nd, nr, nc}); }
        }
    }
    return cost[m - 1][n - 1];
}
```

This is standard Dijkstra with 0/1 edge weights: **O(mn log mn)**.

---

## 6. Approach 2: 0-1 BFS with a deque (main solution)

### Code ([A28_MinimumCostToMakeValidPath.java:91-116](A28_MinimumCostToMakeValidPath.java#L91-L116))
```java
static int minCost(int[][] grid) {
    int m = grid.length, n = grid[0].length;
    int[][] cost = new int[m][n];
    for (int[] row : cost) Arrays.fill(row, Integer.MAX_VALUE);
    cost[0][0] = 0;
    Deque<int[]> deque = new ArrayDeque<>();                          // {cost, r, c}
    deque.add(new int[]{0, 0, 0});
    while (!deque.isEmpty()) {
        int[] cur = deque.pollFirst();
        int r = cur[1], c = cur[2];
        if (cur[0] > cost[r][c]) continue;                            // already reached more cheaply
        if (r == m - 1 && c == n - 1) return cur[0];                  // the front always has the smallest cost
        for (int d = 0; d < 4; d++) {
            int nr = r + DIRS[d][0], nc = c + DIRS[d][1];
            if (nr < 0 || nr >= m || nc < 0 || nc >= n) continue;
            boolean free = grid[r][c] == d + 1;
            int nd = cur[0] + (free ? 0 : 1);
            if (nd < cost[nr][nc]) {
                cost[nr][nc] = nd;
                if (free) deque.addFirst(new int[]{nd, nr, nc});      // same cost: go first
                else deque.addLast(new int[]{nd, nr, nc});            // one more: go last
            }
        }
    }
    return cost[m - 1][n - 1];
}
```

### Line by line
| Line | What it does |
|---|---|
| `pollFirst()` | the cheapest cell available |
| stale check | a cheaper copy of this cell was already handled |
| target check | its cost is final when it's at the front |
| `free = grid[r][c] == d + 1` | moving the way the sign points |
| `addFirst` / `addLast` | cost 0 to the front, cost 1 to the back |

### Complexity
**O(m · n)** time: each cell is added at most a few times, each operation O(1). **O(m · n)** space.

---

## 7. Comparing the approaches

| | Time |
|---|---|
| Relaxation | O((mn)²) worst case |
| Dijkstra | O(mn log mn) |
| **0-1 BFS** | **O(mn)** |

The file checks all three on 2,000 random grids.

---

## 8. Edge cases and common mistakes

**Edge cases:** a 1 × 1 grid (cost 0), signs already forming a valid path (0), signs pointing off the grid, every sign pointing away from the target.

**Mistakes:**
1. Plain BFS (treats every move as cost 1, so it ignores free moves).
2. `addLast` for free moves (the deque is no longer ordered by cost).
3. Marking cells visited when first **added** (as in plain BFS): a cell first reached at cost 1 might later be reached at cost 0.
4. Charging cost 1 per move instead of per changed sign (following a sign is free).
5. Skipping the bounds check for the free direction: a sign can point off the grid, and that move must be ignored like any other.

---

## 9. Interview questions and answers

**Q1. What's 0-1 BFS?**
> A shortest-path algorithm for graphs whose edges cost 0 or 1: a deque where cost-0 neighbours go to the front and cost-1 neighbours go to the back. It processes nodes in order of cost like Dijkstra, in O(V + E).

**Q2. Why is the deque always sorted?**
> When a node of cost d is popped, it adds nodes of cost d (front) or d + 1 (back). The deque only ever contains costs d and d + 1, in order, so the front is always a minimum.

**Q3. How do you model sign changes as a graph?**
> Each cell has 4 outgoing edges; the one matching its sign costs 0 and the other three cost 1 (changing the sign once).

### Follow-up questions

**F1. "Costs up to C instead of 0/1."**
> Dial's algorithm: C + 1 buckets used as a circular array, O(V · C + E). Or just Dijkstra.

**F2. "Other 0-1 BFS problems?"**
> Minimum Obstacle Removal to Reach Corner (2290), and [A08](A08_ShortestPathWithObstaclesElimination.md)'s cousin where breaking a wall costs 1 and walking costs 0.

**F3. "Return which signs to change."**
> Store a parent and the move type per cell, and walk back from the target, collecting the cost-1 moves.

---

## 10. Running the file

```bash
java A28_MinimumCostToMakeValidPath.java     # 6/6 tests passed (3 examples, Dijkstra, a single cell, 2,000 random grids)
```

**One-line summary:** *"Treat following a cell's sign as a cost-0 edge and every other direction as cost 1, then run 0-1 BFS: a deque where free moves go to the front and paid moves to the back, so cells come out in increasing cost."*
