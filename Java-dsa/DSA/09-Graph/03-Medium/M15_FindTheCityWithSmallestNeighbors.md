# M15 · Find the City With the Smallest Number of Neighbors at a Threshold Distance

| | |
|---|---|
| **LeetCode** | [#1334 Find the City With the Smallest Number of Neighbors at a Threshold Distance](https://leetcode.com/problems/find-the-city-with-the-smallest-number-of-neighbors-at-a-threshold-distance/) |
| **Difficulty** | Medium |
| **Pattern** | All-pairs shortest paths → Floyd–Warshall (n ≤ 100) |
| **Code** | [M15_FindTheCityWithSmallestNeighbors.java](M15_FindTheCityWithSmallestNeighbors.java) |
| **Run** | `java M15_FindTheCityWithSmallestNeighbors.java` |

Dijkstra is introduced in [M08](M08_NetworkDelayTime.md).

---

## 1. The problem

n cities, with weighted undirected roads. For each city, count the **other** cities reachable with total distance **≤ distanceThreshold**. Return the city with the **smallest** count; on a tie, the one with the **largest** index.

```
n = 4, roads [[0,1,3],[1,2,1],[1,3,4],[2,3,1]], threshold 4  →  3

   0 ─3─ 1 ─1─ 2
         │     │
         4     1
         └─ 3 ─┘
```

| example | answer |
|---|---|
| the one above | 3 |
| n = 5, [[0,1,2],[0,4,8],[1,2,3],[1,4,2],[2,3,1],[3,4,1]], threshold 2 | 0 |

**Constraints:** `2 ≤ n ≤ 100`.

---

## 2. How to think about it

### We need the distance between every pair
That's **all-pairs shortest paths**:
- **Floyd–Warshall**, O(n³): 10⁶ steps for n = 100, with the simplest code
  ```
  for k: for i: for j:  d[i][j] = min(d[i][j], d[i][k] + d[k][j])
  ```
  **k must be the outermost loop:** after step k, `d[i][j]` is the best path using only cities 0..k as intermediate stops. Putting k inside is a classic bug.
- or Dijkstra from every city: O(n · E log n), which is better for big sparse graphs (the file's referee).

### Then count and pick
Count per city; take the minimum count; ties go to the larger index (iterate upwards and use `<=`).

### Dry run (from the real run)

The final distance matrix:

```
      0  1  2  3
0  [  0, 3, 4, 5 ]     within 4: {1, 2}       → 2
1  [  3, 0, 1, 2 ]               {0, 2, 3}    → 3
2  [  4, 1, 0, 1 ]               {0, 1, 3}    → 3
3  [  5, 2, 1, 0 ]               {1, 2}       → 2
```

The minimum count is 2 (cities 0 and 3) → the larger index → **3**.

---

## 3. Java concepts used in this problem

### 3.1 An adjacency matrix with a safe "infinity"

```java
int INF = 1_000_000_000;
```

It's large enough to mean "unreachable", and small enough that `INF + INF = 2,000,000,000` still fits in an `int` (whose maximum is 2,147,483,647). So `d[i][k] + d[k][j]` never overflows. That's exactly why 10⁹, and not `Integer.MAX_VALUE`, is chosen.

### 3.2 Parallel roads

```java
d[e[0]][e[1]] = Math.min(d[e[0]][e[1]], e[2]);
```

If two roads join the same pair, keep the shorter one.

### 3.3 Ties to the larger index with `<=`

```java
if (count <= bestCount) { bestCount = count; best = i; }
```

Iterating i upwards with `<=` means a later city with an equal count replaces the earlier one.

---

## 4. Approach 1: Dijkstra from every city (the referee)

### Code ([M15_FindTheCityWithSmallestNeighbors.java:61-82](M15_FindTheCityWithSmallestNeighbors.java#L61-L82))

```java
static int withDijkstra(int n, int[][] edges, int th) {
    List<List<int[]>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (int[] e : edges) { adj.get(e[0]).add(new int[]{e[1], e[2]}); adj.get(e[1]).add(new int[]{e[0], e[2]}); }
    int best = -1, bestCount = Integer.MAX_VALUE;
    for (int s = 0; s < n; s++) {
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[s] = 0;
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(x -> x[0]));
        pq.add(new int[]{0, s});
        while (!pq.isEmpty()) {
            int[] t = pq.poll();
            if (t[0] > dist[t[1]]) continue;
            for (int[] e : adj.get(t[1])) if (t[0] + e[1] < dist[e[0]]) { dist[e[0]] = t[0] + e[1]; pq.add(new int[]{dist[e[0]], e[0]}); }
        }
        int c = 0;
        for (int j = 0; j < n; j++) if (j != s && dist[j] <= th) c++;
        if (c <= bestCount) { bestCount = c; best = s; }
    }
    return best;
}
```

[M08](M08_NetworkDelayTime.md)'s Dijkstra, run once per city. **O(n · E log n).** Better when n is large and the graph is sparse.

---

## 5. Approach 2: Floyd–Warshall (the file's main solution)

### Code ([M15_FindTheCityWithSmallestNeighbors.java:38-58](M15_FindTheCityWithSmallestNeighbors.java#L38-L58))

```java
static int findTheCity(int n, int[][] edges, int distanceThreshold) {
    int INF = 1_000_000_000;
    int[][] d = new int[n][n];
    for (int[] row : d) Arrays.fill(row, INF);
    for (int i = 0; i < n; i++) d[i][i] = 0;
    for (int[] e : edges) {
        d[e[0]][e[1]] = Math.min(d[e[0]][e[1]], e[2]);
        d[e[1]][e[0]] = Math.min(d[e[1]][e[0]], e[2]);
    }
    for (int k = 0; k < n; k++)                          // k = allowed intermediate city
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                if (d[i][k] + d[k][j] < d[i][j]) d[i][j] = d[i][k] + d[k][j];
    int best = -1, bestCount = Integer.MAX_VALUE;
    for (int i = 0; i < n; i++) {
        int count = 0;
        for (int j = 0; j < n; j++) if (i != j && d[i][j] <= distanceThreshold) count++;
        if (count <= bestCount) { bestCount = count; best = i; }   // <= : ties go to the larger index
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| `INF`, `d[i][i] = 0` | start with only the direct roads known |
| the road loop | both directions, keeping the shortest of parallel roads |
| the triple loop | allow city k as a stopover, for every pair (i, j) |
| the counting loop | the neighbours within the threshold |
| `count <= bestCount` | the minimum count, ties to the larger index |

### Why k must be outermost
The invariant is "`d[i][j]` = the shortest path using intermediates from {0..k}". To extend it to k + 1, every pair must already have its {0..k} value. With k inside, `d[i][k]` might still be using fewer intermediates than allowed, and some paths are missed.

### Complexity
- **Time O(n³).**
- **Space O(n²).**

---

## 6. Comparing the approaches

| | Floyd–Warshall | n × Dijkstra |
|---|---|---|
| Time | O(n³) | O(n · E log n) |
| Code | 4 lines of core logic | longer |
| Best for | dense graphs, small n | large sparse graphs |
| Negative edges | yes (no negative cycles) | no |

The file checks Floyd–Warshall against n × Dijkstra on 500 random maps.

---

## 7. Edge cases and common mistakes

**Edge cases:** isolated cities (count 0), a threshold of 0, parallel roads, every city reachable from every other.

**Mistakes:**
1. The k loop not outermost.
2. `Integer.MAX_VALUE` as infinity (overflow in `d[i][k] + d[k][j]`).
3. Counting the city itself.
4. Ties resolved to the smaller index.

---

## 8. Interview questions and answers

**Q1. Why is Floyd–Warshall a dynamic program?**
> `d_k[i][j] = min(d_{k−1}[i][j], d_{k−1}[i][k] + d_{k−1}[k][j])`: the best path either avoids k, or goes through k exactly once. The table is updated in place because row and column k don't change during step k.

**Q2. When would you choose Dijkstra per source instead?**
> For large n with few edges: n · E log n beats n³ when E is much smaller than n².

### Follow-up questions

**F1. Detect a negative cycle.**
> After Floyd–Warshall, any `d[i][i] < 0` means i is on a negative cycle.

**F2. Reconstruct the path.**
> Keep `next[i][j]` (the first hop), updated whenever `d[i][j]` improves via k.

**F3. Course Schedule IV (LC 1462): many reachability queries.**
> Floyd–Warshall on booleans (transitive closure), then O(1) per query.

---

## 9. Running the file

```bash
java M15_FindTheCityWithSmallestNeighbors.java      # 3/3 tests passed (including 500 random maps: Floyd == n x Dijkstra)
```

**One-line summary:** *"Floyd–Warshall (k outermost, INF = 10⁹) for all-pairs distances, then pick the city with the fewest others within the threshold, ties to the larger index."*
