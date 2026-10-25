# M08 · Network Delay Time

| | |
|---|---|
| **LeetCode** | [#743 Network Delay Time](https://leetcode.com/problems/network-delay-time/) |
| **Difficulty** | Medium |
| **Pattern** | Dijkstra from one source; answer = the **largest** shortest distance |
| **Code** | [M08_NetworkDelayTime.java](M08_NetworkDelayTime.java) |
| **Run** | `java M08_NetworkDelayTime.java` |

Graph basics are explained in [E01](../02-Easy/E01_FindIfPathExists.md#3-java-concepts-used-in-this-problem). Shortest-path algorithms are built from scratch in [01-Basics/ShortestPaths](../01-Basics/ShortestPaths.java). This is the first Dijkstra note, so section 3 explains it in full.

---

## 1. The problem

n nodes (1..n), and `times[i] = [u, v, w]`: a signal takes w time to travel u → v (directed). A signal starts at k. How long until **all** the nodes have it? −1 if some never do.

```
times [[2,1,1],[2,3,1],[3,4,1]], n = 4, k = 2  →  2

    1 ←(1)─ 2 ─(1)→ 3 ─(1)→ 4
```

| times | n | k | answer |
|---|---|---|---|
| the example | 4 | 2 | 2 |
| [[1,2,1]] | 2 | 1 | 1 |
| [[1,2,1]] | 2 | 2 | −1 (1 is unreachable from 2) |

---

## 2. How to think about it

### Every node gets the signal at its shortest-path time
The signal travels along all the edges at once, so each node receives it at its **shortest** distance from k. Everyone has it once the **last** one does:

```
answer = max over v of dist[v]      (or −1 if some dist is infinite)
```

The weights are non-negative → **Dijkstra**.

### Dijkstra in one paragraph
Keep a tentative `dist[]` (∞ except `dist[k] = 0`) and a min-heap of `{dist, node}`. Repeatedly pop the closest unprocessed node u; its distance is now **final** (no path through farther nodes can beat it, because the weights are non-negative). **Relax** each edge u → v: if `dist[u] + w < dist[v]`, update dist[v] and push it.

### Dry run, k = 2 (from the real run)

| pop | relaxations | dist (1, 2, 3, 4) |
|---|---|---|
| 2 (0) | 1 → 1, 3 → 1 | (1, 0, 1, ∞) |
| 1 (1) | (no outgoing edges) | |
| 3 (1) | 4 → 2 | (1, 0, 1, 2) |
| 4 (2) | | |

max = **2** ✓

### Alternatives
Bellman-Ford, O(V · E), works too (and handles negative weights). For n ≤ 100, even Floyd–Warshall, O(n³), is fine. The file uses Bellman-Ford as the referee.

---

## 3. Java concepts used in this problem

### 3.1 A weighted adjacency list

```java
List<List<int[]>> adj = new ArrayList<>();
for (int i = 0; i <= n; i++) adj.add(new ArrayList<>());        // n + 1 lists: the nodes are 1-based
for (int[] t : times) adj.get(t[0]).add(new int[]{t[1], t[2]}); // {neighbour, weight}
```

### 3.2 "Infinity" with `Integer.MAX_VALUE`

`Arrays.fill(dist, Integer.MAX_VALUE)` marks every node as unreached. The relaxation `d + e[1] < dist[...]` never adds to a MAX_VALUE distance (d is always a real popped distance), so there's no overflow.

### 3.3 Lazy deletion: skipping stale heap entries

```java
if (d > dist[u]) continue;                       // stale
```

Java's PriorityQueue has no "decrease key". Instead, a better distance is pushed as a **new** entry, and the old one is left behind. When the old entry is eventually popped, its distance is larger than the recorded one, so it's skipped. (The same lazy-deletion idea as [Heap H04](../../08-Heap/04-Hard/H04_SlidingWindowMedian.md).)

### 3.4 Why Dijkstra needs non-negative weights

When u is popped with the smallest tentative distance, any other path to u must go through some node that's at least as far, and adding non-negative weights can't make it shorter. With a negative edge, that argument breaks.

---

## 4. Approach 1: Bellman-Ford (the referee)

### Code ([M08_NetworkDelayTime.java:67-75](M08_NetworkDelayTime.java#L67-L75))

```java
static int bellmanFord(int[][] times, int n, int k) {
    long[] d = new long[n + 1];
    Arrays.fill(d, Long.MAX_VALUE / 2);
    d[k] = 0;
    for (int i = 0; i < n - 1; i++) for (int[] t : times) d[t[1]] = Math.min(d[t[1]], d[t[0]] + t[2]);
    long max = 0;
    for (int v = 1; v <= n; v++) max = Math.max(max, d[v]);
    return max >= Long.MAX_VALUE / 2 ? -1 : (int) max;
}
```

| Line | What it does |
|---|---|
| `Long.MAX_VALUE / 2` | "infinity" with room to add a weight without overflowing |
| n − 1 rounds of relaxing every edge | a shortest path has at most n − 1 edges, and each round extends the correct prefix by one edge |
| `max >= Long.MAX_VALUE / 2` | some node was never reached |

**O(V · E).** Simple, and it also works with negative weights (without negative cycles).

---

## 5. Approach 2: Dijkstra with a min-heap (the file's main solution)

### Code ([M08_NetworkDelayTime.java:39-65](M08_NetworkDelayTime.java#L39-L65))

```java
static int networkDelayTime(int[][] times, int n, int k) {
    List<List<int[]>> adj = new ArrayList<>();
    for (int i = 0; i <= n; i++) adj.add(new ArrayList<>());
    for (int[] t : times) adj.get(t[0]).add(new int[]{t[1], t[2]});
    int[] dist = new int[n + 1];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[k] = 0;
    PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(x -> x[0]));
    heap.add(new int[]{0, k});
    while (!heap.isEmpty()) {
        int[] top = heap.poll();
        int d = top[0], u = top[1];
        if (d > dist[u]) continue;                       // stale
        for (int[] e : adj.get(u)) {
            if (d + e[1] < dist[e[0]]) {
                dist[e[0]] = d + e[1];
                heap.add(new int[]{dist[e[0]], e[0]});
            }
        }
    }
    int max = 0;
    for (int v = 1; v <= n; v++) {
        if (dist[v] == Integer.MAX_VALUE) return -1;     // someone never hears it
        max = Math.max(max, dist[v]);
    }
    return max;
}
```

### Line by line

| Line | What it does |
|---|---|
| `adj` | the directed weighted graph |
| `dist`, `dist[k] = 0` | the best known times; only the source is known at the start |
| the heap of `{dist, node}` | always pops the closest known node |
| `if (d > dist[u]) continue;` | an outdated entry (a better one was already processed) |
| the relaxation | a faster way to reach `e[0]` through u → record it and push it |
| the final loop | −1 if any node was never reached; otherwise the slowest arrival |

### Complexity
- **Time O(E log V)** (each improvement pushes one entry; there are at most E of them).
- **Space O(V + E).**

---

## 6. Comparing the approaches

| | Bellman-Ford | Dijkstra (heap) | Floyd–Warshall |
|---|---|---|---|
| Time | O(V · E) | **O(E log V)** | O(V³) |
| Negative weights | yes | **no** | yes |
| Single source? | yes | yes | all pairs |

The file checks Dijkstra against Bellman-Ford on 1,000 random networks (including zero-weight edges and self-loops).

---

## 7. Edge cases and common mistakes

**Edge cases:** an unreachable node (−1), n = 1 (answer 0), zero-weight edges, parallel edges with different weights.

**Mistakes:**
1. Returning the sum of the distances instead of the max.
2. Not skipping stale heap entries (correct, but it does extra work).
3. 0-based vs 1-based nodes (the adjacency list needs n + 1 lists).
4. Using BFS (it ignores the weights).

---

## 8. Interview questions and answers

**Q1. Why is the answer the maximum distance?**
> The signal reaches each node at its shortest-path time; all the nodes have it when the latest of those times has passed.

**Q2. Why does Dijkstra fail with negative edges?**
> It finalises a node when it's popped. A later negative edge could still provide a shorter path to an already-finalised node.

### Follow-up questions

**F1. Cheapest Flights Within K Stops.** → [M09](M09_CheapestFlightsWithinKStops.md)
> A limit on the number of edges: Bellman-Ford with k + 1 rounds, or BFS by stops.

**F2. Path With Minimum Effort.** → [M14](M14_PathWithMinimumEffort.md)
> Dijkstra where a path's cost is its **max** edge instead of its sum.

**F3. The number of shortest paths (LC 1976).**
> Dijkstra that also counts the ways: when an equal distance is found, add the counts.

---

## 9. Running the file

```bash
java M08_NetworkDelayTime.java      # 4/4 tests passed (including 1,000 random networks vs Bellman-Ford)
```

**One-line summary:** *"Dijkstra from k with a min-heap (skipping stale entries); the answer is the largest distance, or −1 if any node stays at infinity."*
