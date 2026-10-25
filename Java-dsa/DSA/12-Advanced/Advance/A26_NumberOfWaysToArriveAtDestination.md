# A26 · Number of Ways to Arrive at Destination ⭐

| | |
|---|---|
| **LeetCode** | [#1976 Number of Ways to Arrive at Destination](https://leetcode.com/problems/number-of-ways-to-arrive-at-destination/) |
| **Difficulty** | Medium |
| **Mix of structures** | **Dijkstra** (min-heap) + a **counting DP** carried along with the distances |
| **Code** | [A26_NumberOfWaysToArriveAtDestination.java](A26_NumberOfWaysToArriveAtDestination.java) |
| **Run** | `java A26_NumberOfWaysToArriveAtDestination.java` |

⭐ = not in `leetcode.md`, added because "Dijkstra + count/track something extra" is a very common interview twist.

---

## 1. The problem

There are `n` intersections and `roads[i] = [u, v, time]` (undirected, `time ≥ 1`). How many **different shortest routes** are there from intersection 0 to n − 1? Return the count modulo 10⁹ + 7.

```
n = 7, roads = [[0,6,7],[0,1,2],[1,2,3],[1,3,3],[6,3,3],[3,5,1],[6,5,1],[2,5,1],[0,4,5],[4,6,2]]
→ 4      the shortest time is 7, and four routes achieve it:
         0→6,  0→4→6,  0→1→2→5→6,  0→1→3→5→6

n = 2, roads = [[1,0,10]]  →  1
```

---

## 2. How to think about it

### Two numbers per node
```
dist[v] = the shortest time from 0 to v     ← Dijkstra
ways[v] = how many routes achieve dist[v]   ← counting
```

When Dijkstra relaxes an edge `u → v` with `nd = dist[u] + w`:

| Case | Meaning | Update |
|---|---|---|
| `nd < dist[v]` | a strictly shorter route to v | `dist[v] = nd`, **`ways[v] = ways[u]`** (forget the old, longer routes) |
| `nd == dist[v]` | another equally short route | **`ways[v] += ways[u]`** |
| `nd > dist[v]` | longer | ignore |

### Why ways[u] is complete when u is popped
Every shortest route into u arrives from a node with a **strictly smaller** distance (each road takes ≥ 1 minute). Dijkstra pops nodes in increasing distance, so all those predecessors were popped before u and have already added their ways into `ways[u]`.

### The bug to avoid: stale heap entries
The heap can hold an old entry for u that was pushed before a shorter route was found. If you process it, you relax u's edges a second time and add `ways[u]` to its neighbours **twice**. Always skip entries whose distance is greater than `dist[u]`.

### Dry run: a square, roads 0–1, 0–2, 1–3, 2–3 (all 1)
| pop | relaxations | dist | ways |
|---|---|---|---|
| 0 (d 0) | 1: 1 < ∞ → reset; 2: 1 < ∞ → reset | [0,1,1,∞] | [1,1,1,0] |
| 1 (d 1) | 3: 2 < ∞ → reset to ways[1] | [0,1,1,2] | [1,1,1,1] |
| 2 (d 1) | 3: 2 == 2 → add ways[2] | | [1,1,1,**2**] |
| 3 (d 2) | – | | |

Answer **2** (0-1-3 and 0-2-3).

---

## 3. Java concepts used in this problem

### 3.1 A heap of `long[]` pairs
```java
PriorityQueue<long[]> heap = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));   // {distance, node}
```
Distances can reach 200 × 10⁹, so they're `long`; the node is stored in the same array and cast back with `(int)`.

### 3.2 Adjacency lists of `int[]`
`adj.get(u)` holds `{v, time}` pairs. Each undirected road is added in both directions.

### 3.3 Modular counting
`ways[v] = (ways[v] + ways[u]) % MOD`: counts can be astronomically large (the file's ladder graph has 2⁶⁰ shortest routes). Reducing after every addition keeps them below 2 × 10⁹, safely within `long`.

### 3.4 Sorting indices by a `long` key (in the referee)
`Arrays.sort(order, Comparator.comparingLong(i -> dist[i]))` orders the nodes by distance.

---

## 4. The referee: Bellman-Ford, then count in distance order

### Code ([A26_NumberOfWaysToArriveAtDestination.java:47-67](A26_NumberOfWaysToArriveAtDestination.java#L47-L67))
```java
static int countPathsBrute(int n, int[][] roads) {
    long[] dist = new long[n];
    Arrays.fill(dist, Long.MAX_VALUE);
    dist[0] = 0;
    for (int round = 0; round < n - 1; round++)                       // relax every edge n-1 times
        for (int[] r : roads) {
            if (dist[r[0]] != Long.MAX_VALUE) dist[r[1]] = Math.min(dist[r[1]], dist[r[0]] + r[2]);
            if (dist[r[1]] != Long.MAX_VALUE) dist[r[0]] = Math.min(dist[r[0]], dist[r[1]] + r[2]);
        }
    Integer[] order = new Integer[n];
    for (int i = 0; i < n; i++) order[i] = i;
    Arrays.sort(order, Comparator.comparingLong(i -> dist[i]));
    long[] ways = new long[n];
    ways[0] = 1;
    for (int v : order)                                               // predecessors always come earlier
        for (int[] r : roads) {
            if (r[1] == v && dist[r[0]] + r[2] == dist[v]) ways[v] = (ways[v] + ways[r[0]]) % MOD;
            if (r[0] == v && dist[r[1]] + r[2] == dist[v]) ways[v] = (ways[v] + ways[r[1]]) % MOD;
        }
    return (int) ways[n - 1];
}
```

| Part | What it does |
|---|---|
| Bellman-Ford | the true shortest distances, by relaxing every edge n − 1 times |
| sort by distance | an order where every shortest-route predecessor comes first |
| the counting loop | `ways[v]` = sum of `ways[u]` over edges that lie on a shortest route |

It's a completely different method, so it makes a good referee. **O(n · E)**.

---

## 5. Main solution: Dijkstra + ways

### Code ([A26_NumberOfWaysToArriveAtDestination.java:70-99](A26_NumberOfWaysToArriveAtDestination.java#L70-L99))
```java
static int countPaths(int n, int[][] roads) {
    List<List<int[]>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (int[] r : roads) { adj.get(r[0]).add(new int[]{r[1], r[2]}); adj.get(r[1]).add(new int[]{r[0], r[2]}); }

    long[] dist = new long[n];
    long[] ways = new long[n];
    Arrays.fill(dist, Long.MAX_VALUE);
    dist[0] = 0;
    ways[0] = 1;
    PriorityQueue<long[]> heap = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));   // {distance, node}
    heap.add(new long[]{0, 0});
    while (!heap.isEmpty()) {
        long[] top = heap.poll();
        int u = (int) top[1];
        if (top[0] > dist[u]) continue;                               // stale entry: u was improved later
        for (int[] e : adj.get(u)) {
            int v = e[0];
            long nd = dist[u] + e[1];
            if (nd < dist[v]) {                                       // strictly shorter: reset the count
                dist[v] = nd;
                ways[v] = ways[u];
                heap.add(new long[]{nd, v});
            } else if (nd == dist[v]) {                               // equally short: add the routes through u
                ways[v] = (ways[v] + ways[u]) % MOD;
            }
        }
    }
    return (int) ways[n - 1];
}
```

### Line by line
| Line | What it does |
|---|---|
| `ways[0] = 1` | one way to be at the start: do nothing |
| `top[0] > dist[u] → continue` | skip outdated entries, so no double counting |
| `nd < dist[v]` | better route: the old count is about longer routes, so replace it |
| `nd == dist[v]` | tie: every shortest route to u extends to v |
| no push on a tie | v's distance didn't change, so its existing heap entry is still right |

### Complexity
**O(E log V)** time (standard Dijkstra), **O(V + E)** space.

---

## 6. Comparing the approaches

| | Time |
|---|---|
| Bellman-Ford + counting | O(V · E) |
| **Dijkstra + ways** | **O(E log V)** |

The file checks both on 1,500 random connected graphs with small weights (lots of ties), and on a ladder with 2⁶⁰ shortest routes to exercise the modulo.

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 2, many equal-length routes, routes that tie at intermediate nodes, very large counts (mod), large times (long distances).

**Mistakes:**
1. Processing stale heap entries (double counting).
2. Adding to `ways[v]` when a strictly shorter route is found instead of **resetting** it.
3. `int` distances (overflow).
4. Forgetting the modulo, or applying it to distances.
5. Counting with BFS on a weighted graph.

---

## 8. Interview questions and answers

**Q1. How do you count shortest paths with Dijkstra?**
> Carry `ways[v]` alongside `dist[v]`. On a strictly shorter relaxation, copy `ways[u]`; on an equal one, add it. Because weights are positive, a node's count is final when it's popped.

**Q2. Why must you skip stale heap entries?**
> A stale entry would relax the node's edges a second time and add its ways to its neighbours again, overcounting.

**Q3. What if some roads took 0 minutes?**
> Then equal-distance nodes can be predecessors of each other, and pop order no longer guarantees finished counts. You'd count over the shortest-path DAG in a topological order instead (and zero-weight cycles would make the count infinite).

### Follow-up questions

**F1. "Count paths in an unweighted graph."**
> The same idea with BFS: `ways[v] += ways[u]` when `dist[v] == dist[u] + 1`.

**F2. "Return one shortest path."**
> Store a parent per node when `dist` improves, and walk back from n − 1.

**F3. "Second shortest path."**
> Keep the two best distances per node (and allow each node to be popped twice).

---

## 9. Running the file

```bash
java A26_NumberOfWaysToArriveAtDestination.java     # 5/5 tests passed (2 examples, a square, 2^60 routes, 1,500 random graphs)
```

**One-line summary:** *"Run Dijkstra with a ways[] array: a strictly shorter relaxation copies ways[u], an equal one adds ways[u] (mod 1e9+7), and stale heap entries are skipped so nothing is counted twice."*
