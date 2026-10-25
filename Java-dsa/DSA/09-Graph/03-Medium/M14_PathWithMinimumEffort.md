# M14 · Path With Minimum Effort

| | |
|---|---|
| **LeetCode** | [#1631 Path With Minimum Effort](https://leetcode.com/problems/path-with-minimum-effort/) |
| **Difficulty** | Medium |
| **Pattern** | Dijkstra where a path's cost is its **max** edge (a "minimax" path) |
| **Code** | [M14_PathWithMinimumEffort.java](M14_PathWithMinimumEffort.java) |
| **Run** | `java M14_PathWithMinimumEffort.java` |

Dijkstra is introduced in [M08 Network Delay Time](M08_NetworkDelayTime.md).

---

## 1. The problem

A grid of heights. A route's **effort** = the largest absolute height difference between two consecutive cells on it. What's the minimum effort from the top-left to the bottom-right (4 directions)?

```
1 2 2
3 8 2          → 2   (1 → 3 → 5 → 3 → 5: steps of 2, 2, 2, 2)
5 3 5
```

| heights | answer |
|---|---|
| `[[1,2,2],[3,8,2],[5,3,5]]` | 2 |
| `[[1,2,3],[3,8,4],[5,3,5]]` | 1 |
| `[[7]]` | 0 |

---

## 2. How to think about it

### A shortest path where the cost is a MAX
```
cost(path + edge) = max(cost(path), edge weight)
```

**Dijkstra still works**, because extending a path never makes its cost smaller (`max` is monotonic, just like adding non-negative weights). Replace `dist[u] + w` with `max(dist[u], w)` and keep everything else.

### Alternatives
- **Binary search on the answer E:** "can I get there using only steps ≤ E?" is a plain BFS, and feasibility is monotonic in E: O(R · C · log(maxH)).
- **Union-Find (Kruskal-style):** add the edges in increasing order of weight until the start and the end are connected; the last edge added is the answer.

[Swim in Rising Water (H06)](../04-Hard/H06_SwimInRisingWater.md) is the same problem with cell weights.

### Dry run: example 1 (from the real run)

| pop (cell, height, effort) | improvements |
|---|---|
| (0,0) h1 e0 | (1,0) = 2, (0,1) = 1 |
| (0,1) h2 e1 | (1,1) = 6, (0,2) = 1 |
| (0,2) h2 e1 | (1,2) = 1 |
| (1,2) h2 e1 | (2,2) = 3 |
| (1,0) h3 e2 | (2,0) = 2, (1,1) = 5 |
| (2,0) h5 e2 | (2,1) = 2 |
| (2,1) h3 e2 | (2,2) = **2** (improves on 3) |
| pop (2,2) with e = 2 | → **2** |

Note how (2, 2) first got effort 3 (through the right column), then 2 (through the left column), and the heap delivered the better one first.

---

## 3. Java concepts used in this problem

### 3.1 Dijkstra on a grid: `{effort, r, c}` heap entries

```java
PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(x -> x[0]));
heap.add(new int[]{0, 0, 0});
```

The cells are the vertices; the cost travels with each entry.

### 3.2 Returning when the target is **popped**

```java
if (r == R - 1 && c == C - 1) return e;
```

In Dijkstra, a node's value is final when it's popped (not when it's first pushed), so this early exit is safe.

### 3.3 The "smallest feasible value" binary search

```java
while (lo < hi) { int mid = (lo + hi) / 2; if (canReach(h, mid)) hi = mid; else lo = mid + 1; }
```

The same template as [Heap M06](../../08-Heap/03-Medium/M06_KthSmallestInSortedMatrix.md): find the smallest effort limit for which a BFS succeeds.

---

## 4. Approach 1: Binary search + BFS

### Code ([M14_PathWithMinimumEffort.java:62-85](M14_PathWithMinimumEffort.java#L62-L85))

```java
static int binarySearch(int[][] h) {
    int lo = 0, hi = 1_000_000;
    while (lo < hi) {
        int mid = (lo + hi) / 2;
        if (canReach(h, mid)) hi = mid; else lo = mid + 1;
    }
    return lo;
}

static boolean canReach(int[][] h, int limit) {
    int R = h.length, C = h[0].length;
    boolean[][] seen = new boolean[R][C];
    Deque<int[]> q = new ArrayDeque<>(List.<int[]>of(new int[]{0, 0}));
    seen[0][0] = true;
    while (!q.isEmpty()) {
        int[] p = q.poll();
        if (p[0] == R - 1 && p[1] == C - 1) return true;
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            int r = p[0] + d[0], c = p[1] + d[1];
            if (r >= 0 && r < R && c >= 0 && c < C && !seen[r][c] && Math.abs(h[r][c] - h[p[0]][p[1]]) <= limit) { seen[r][c] = true; q.add(new int[]{r, c}); }
        }
    }
    return false;
}
```

| Line | What it does |
|---|---|
| `lo = 0, hi = 10⁶` | the effort lies in this range (heights ≤ 10⁶) |
| `canReach(mid)` | a BFS that only uses steps ≤ mid |
| `hi = mid` / `lo = mid + 1` | the smallest feasible limit |

**O(R · C · log(maxH)).** It's the file's referee, and a fine interview answer too.

---

## 5. Approach 2: Union-Find over the edges, sorted by weight

```java
static int minimumEffortUnionFind(int[][] h) {
    int R = h.length, C = h[0].length;
    if (R * C == 1) return 0;
    List<int[]> edges = new ArrayList<>();                         // {weight, cellA, cellB}
    for (int r = 0; r < R; r++)
        for (int c = 0; c < C; c++) {
            if (r + 1 < R) edges.add(new int[]{Math.abs(h[r][c] - h[r + 1][c]), r * C + c, (r + 1) * C + c});
            if (c + 1 < C) edges.add(new int[]{Math.abs(h[r][c] - h[r][c + 1]), r * C + c, r * C + c + 1});
        }
    edges.sort(Comparator.comparingInt(e -> e[0]));
    int[] parent = new int[R * C];
    for (int i = 0; i < R * C; i++) parent[i] = i;
    for (int[] e : edges) {
        int a = find(parent, e[1]), b = find(parent, e[2]);
        if (a != b) parent[a] = b;
        if (find(parent, 0) == find(parent, R * C - 1)) return e[0];   // start and end just got connected
    }
    return 0;
}

static int find(int[] p, int x) {
    while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; }
    return x;
}
```

Adding the edges cheapest first, the moment the start and the end become connected, the current edge weight is the smallest possible maximum. **O(R · C · log(R · C))** for the sort. (Stress-tested against Dijkstra on 2,000 random grids.)

---

## 6. Approach 3: Minimax Dijkstra (the file's main solution)

### Code ([M14_PathWithMinimumEffort.java:35-59](M14_PathWithMinimumEffort.java#L35-L59))

```java
static int minimumEffortPath(int[][] h) {
    int R = h.length, C = h[0].length;
    int[][] effort = new int[R][C];
    for (int[] row : effort) Arrays.fill(row, Integer.MAX_VALUE);
    effort[0][0] = 0;
    PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(x -> x[0]));   // {effort, r, c}
    heap.add(new int[]{0, 0, 0});
    int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    while (!heap.isEmpty()) {
        int[] top = heap.poll();
        int e = top[0], r = top[1], c = top[2];
        if (r == R - 1 && c == C - 1) return e;           // the first time we POP the target, it's optimal
        if (e > effort[r][c]) continue;                   // stale
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (nr < 0 || nr >= R || nc < 0 || nc >= C) continue;
            int ne = Math.max(e, Math.abs(h[nr][nc] - h[r][c]));   // MAX instead of SUM
            if (ne < effort[nr][nc]) {
                effort[nr][nc] = ne;
                heap.add(new int[]{ne, nr, nc});
            }
        }
    }
    return 0;
}
```

### Line by line

| Line | What it does |
|---|---|
| `effort[][]` | the best known effort to reach each cell |
| the target check | popped = final |
| the stale check | skip outdated heap entries |
| `ne = max(e, |Δh|)` | the effort of the extended path |
| `ne < effort[nr][nc]` | a better route to the neighbour → record it and push it |

### Complexity
- **Time O(R · C · log(R · C)).**
- **Space O(R · C).**

---

## 7. Comparing the approaches

| | Binary search + BFS | Union-Find (sorted edges) | Minimax Dijkstra |
|---|---|---|---|
| Time | O(RC log maxH) | O(RC log RC) | O(RC log RC) |
| Space | O(RC) | O(RC) edges | O(RC) |
| Idea | "is effort E enough?" | Kruskal until connected | shortest path with max |

The file checks Dijkstra against the binary search on 300 random grids.

---

## 8. Edge cases and common mistakes

**Edge cases:** a single cell (0), a single row or column, all equal heights (0).

**Mistakes:**
1. Summing the differences instead of taking the max.
2. Using BFS without weights.
3. Returning when the target is first **pushed** rather than popped.
4. Only moving right/down (paths may go up or left).

---

## 9. Interview questions and answers

**Q1. Why is Dijkstra valid with a max-cost?**
> Dijkstra needs costs that never decrease when a path is extended. `max(cost, w) ≥ cost` holds, just like `cost + w ≥ cost` for non-negative w.

**Q2. Why does the Union-Find method give the answer?**
> Adding the edges in increasing order, the first moment the start and the end are connected, a path exists using only edges ≤ the current weight, and no path can avoid using an edge at least that large (otherwise they'd have been connected earlier).

### Follow-up questions

**F1. Swim in Rising Water.** → [H06](../04-Hard/H06_SwimInRisingWater.md)
> The cost is the max cell value along the path: the same minimax Dijkstra.

**F2. Minimise the total effort (the sum).**
> Standard Dijkstra with `+`.

**F3. The minimax path between all pairs.**
> Build the minimum spanning tree; the minimax path between any two nodes runs along the MST.

---

## 10. Running the file

```bash
java M14_PathWithMinimumEffort.java      # 5/5 tests passed (including 300 random grids: Dijkstra == binary search)
```

**One-line summary:** *"Dijkstra with the path cost max(effort so far, |Δheight|) instead of a sum; the effort when the bottom-right cell is popped is the answer."*
