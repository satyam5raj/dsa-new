# H06 · Swim in Rising Water

| | |
|---|---|
| **LeetCode** | [#778 Swim in Rising Water](https://leetcode.com/problems/swim-in-rising-water/) |
| **Difficulty** | Hard |
| **Pattern** | A minimax path: Dijkstra on `max()`, like [Path With Minimum Effort (M14)](../03-Medium/M14_PathWithMinimumEffort.md) |
| **Code** | [H06_SwimInRisingWater.java](H06_SwimInRisingWater.java) |
| **Run** | `java H06_SwimInRisingWater.java` |

Dijkstra is introduced in [M08](../03-Medium/M08_NetworkDelayTime.md); the minimax variant in [M14](../03-Medium/M14_PathWithMinimumEffort.md).

---

## 1. The problem

An n × n grid of **distinct** elevations 0..n² − 1. At time t the water level is t; you can swim between adjacent cells if **both** elevations are ≤ t (instantly). What's the least time to get from (0, 0) to (n − 1, n − 1)?

| grid | answer |
|---|---|
| `[[0,2],[1,3]]` | 3 (you must stand on the 3 at the end) |
| the 5 × 5 spiral `[[0,1,2,3,4],[24,23,22,21,5],[12,13,14,15,16],[11,17,18,19,20],[10,9,8,7,6]]` | 16 |
| `[[0]]` | 0 |

---

## 2. How to think about it

### Reframe
A path is usable at time t iff **every cell** on it is ≤ t. So the time a path needs = the **max elevation along it**, and we want the path that minimises that max. It's a **minimax path**, the same as M14, but with the weights on the **cells** instead of the edges.

### Dijkstra with cost(path) = the max over its cells
Always expand the cell with the lowest "max so far"; the first time the target is popped, that's the answer.

### Other views (all O(n² log n))
- binary search on t + BFS: "can I reach the end using cells ≤ t?" (the file's referee)
- Union-Find: add the cells in increasing elevation, union each with its already-added neighbours; stop when (0, 0) and the end are connected → that elevation

### Dry run: `[[0,2],[1,3]]` (from the real run)

| pop | time | pushes |
|---|---|---|
| (0,0) | 0 | (1,0) with max(0, 1) = 1, (0,1) with max(0, 2) = 2 |
| (1,0) | 1 | (1,1) with max(1, 3) = 3 |
| (0,1) | 2 | (its neighbours are already seen) |
| (1,1) | **3** | the target → 3 |

---

## 3. Java concepts used in this problem

### 3.1 Marking cells seen at **push** time (with node weights)

```java
seen[r][c] = true;                     // node weights: the first push is already the best
heap.add(new int[]{Math.max(top[0], grid[r][c]), r, c});
```

In general Dijkstra, a node can be pushed several times with improving costs. Here the cost of entering a cell is `max(cost of the cell we came from, the cell's own elevation)`. Cells are popped in increasing cost order, so the **first** time a cell is reached is from the cheapest possible predecessor, and no later push could beat it. That's why marking at push time (with no stale-entry check) is safe here.

### 3.2 `while (true)` with a guaranteed return

The target is always reachable (eventually t = n² − 1 floods everything), so the loop always returns.

### 3.3 Binary search bounds

```java
int lo = Math.max(g[0][0], g[n - 1][n - 1]), hi = n * n - 1;
```

You must be able to stand on both the start and the end, so the answer is at least the larger of the two; it's at most the highest elevation.

---

## 4. Approach 1: Binary search on t + DFS reachability (the referee)

### Code ([H06_SwimInRisingWater.java:59-82](H06_SwimInRisingWater.java#L59-L82))

```java
static int binarySearch(int[][] g) {
    int n = g.length, lo = Math.max(g[0][0], g[n - 1][n - 1]), hi = n * n - 1;
    while (lo < hi) {
        int mid = (lo + hi) / 2;
        if (reachable(g, mid)) hi = mid; else lo = mid + 1;
    }
    return lo;
}

static boolean reachable(int[][] g, int t) {
    int n = g.length;
    boolean[][] seen = new boolean[n][n];
    Deque<int[]> st = new ArrayDeque<>(List.<int[]>of(new int[]{0, 0}));
    seen[0][0] = true;
    while (!st.isEmpty()) {
        int[] p = st.pop();
        if (p[0] == n - 1 && p[1] == n - 1) return true;
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            int r = p[0] + d[0], c = p[1] + d[1];
            if (r >= 0 && r < n && c >= 0 && c < n && !seen[r][c] && g[r][c] <= t) { seen[r][c] = true; st.push(new int[]{r, c}); }
        }
    }
    return false;
}
```

| Line | What it does |
|---|---|
| `reachable(g, t)` | flood from (0, 0) through cells ≤ t |
| the binary search | the smallest t that reaches the end (feasibility is monotonic in t) |

**O(n² log n²).**

---

## 5. Approach 2: Union-Find in elevation order

```java
static int swimUnionFind(int[][] g) {
    int n = g.length;
    int[] where = new int[n * n];                     // elevation → cell id
    for (int r = 0; r < n; r++) for (int c = 0; c < n; c++) where[g[r][c]] = r * n + c;
    int[] parent = new int[n * n];
    for (int i = 0; i < n * n; i++) parent[i] = i;
    boolean[] added = new boolean[n * n];
    int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    for (int t = 0; t < n * n; t++) {
        int id = where[t], r = id / n, c = id % n;
        added[id] = true;
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (nr >= 0 && nr < n && nc >= 0 && nc < n && added[nr * n + nc]) {
                parent[find(parent, id)] = find(parent, nr * n + nc);
            }
        }
        if (find(parent, 0) == find(parent, n * n - 1)) return t;    // start and end just got connected
    }
    return n * n - 1;
}

static int find(int[] p, int x) {
    while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; }
    return x;
}
```

Because the elevations are a permutation of 0..n² − 1, cell ids can be listed in elevation order directly. When (0, 0) and the end first share a set, the current elevation is the answer.

---

## 6. Approach 3: Minimax Dijkstra (the file's main solution)

### Code ([H06_SwimInRisingWater.java:39-57](H06_SwimInRisingWater.java#L39-L57))

```java
static int swimInWater(int[][] grid) {
    int n = grid.length;
    boolean[][] seen = new boolean[n][n];
    PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(x -> x[0]));   // {time needed, r, c}
    heap.add(new int[]{grid[0][0], 0, 0});
    seen[0][0] = true;
    int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    while (true) {
        int[] top = heap.poll();
        if (top[1] == n - 1 && top[2] == n - 1) return top[0];
        for (int[] d : dirs) {
            int r = top[1] + d[0], c = top[2] + d[1];
            if (r >= 0 && r < n && c >= 0 && c < n && !seen[r][c]) {
                seen[r][c] = true;                     // node weights: the first push is already the best
                heap.add(new int[]{Math.max(top[0], grid[r][c]), r, c});
            }
        }
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `{grid[0][0], 0, 0}` | starting needs at least the start's own elevation |
| `heap.poll()` | the reachable cell with the smallest required time |
| the target check | popped = optimal |
| `Math.max(top[0], grid[r][c])` | the time needed to also stand on (r, c) |

### Complexity
- **Time O(n² log n).**
- **Space O(n²).**

---

## 7. Comparing the approaches

| | Binary search + DFS | Union-Find by elevation | Minimax Dijkstra |
|---|---|---|---|
| Time | O(n² log n) | O(n² · α) | O(n² log n) |
| Idea | "is time t enough?" | flood the cells in order | the cheapest frontier first |

The file checks Dijkstra against the binary search on 300 random permutation grids.

---

## 8. Edge cases and common mistakes

**Edge cases:** a 1 × 1 grid (its elevation), the start or the end being the highest cell, a snake-shaped optimal path (the spiral).

**Mistakes:**
1. Summing the elevations instead of taking the max.
2. Forgetting the start cell's own elevation.
3. Moving only right/down.

---

## 9. Interview questions and answers

**Q1. How is this the same as Path With Minimum Effort?**
> Both minimise the maximum weight along a path. M14 has the weights on edges (height differences); here they're on cells. The same Dijkstra with `max` works.

**Q2. Why can a cell be marked seen when it's first pushed here?**
> The cost of entering a cell is max(predecessor's cost, its elevation). The predecessors are processed in increasing cost order, so the first one to reach it gives the minimum possible cost.

### Follow-up questions

**F1. Path With Minimum Effort.** → [M14](../03-Medium/M14_PathWithMinimumEffort.md)

**F2. The minimum spanning tree view.**
> The minimax path between two cells is the path in the MST of the grid (with edge weight = max of the two cells).

**F3. Trapping Rain Water II (LC 407).**
> Also a min-heap flood from the boundary, accumulating water instead of the time.

---

## 10. Running the file

```bash
java H06_SwimInRisingWater.java      # 4/4 tests passed (including 300 random grids: Dijkstra == binary search)
```

**One-line summary:** *"The time a path needs is its highest cell, so run Dijkstra with cost = max(cost so far, the cell's elevation); the cost when the bottom-right cell is popped is the answer."*
