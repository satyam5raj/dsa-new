# A09 · Shortest Path Visiting All Nodes ⭐

| | |
|---|---|
| **LeetCode** | [#847 Shortest Path Visiting All Nodes](https://leetcode.com/problems/shortest-path-visiting-all-nodes/) |
| **Difficulty** | Hard |
| **Mix of structures** | **Graph BFS (queue)** + **bitmask** state (node, set of visited nodes); alternative: **all-pairs BFS + bitmask DP** |
| **Code** | [A09_ShortestPathVisitingAllNodes.java](A09_ShortestPathVisitingAllNodes.java) |
| **Run** | `java A09_ShortestPathVisitingAllNodes.java` |

⭐ = not in `leetcode.md`, added because it combines BFS with bitmasks, the standard tool for "visit every node" on small graphs.

---

## 1. The problem

An undirected, connected graph with **n ≤ 12** nodes, given as an adjacency list. Return the length of the shortest walk that **visits every node**. You can start and stop anywhere and reuse nodes and edges.

```
graph = [[1,2,3],[0],[0],[0]]          (a star: 0 in the middle)
answer 4: e.g. 1 → 0 → 2 → 0 → 3

graph = [[1],[0,2,4],[1,3,4],[2],[1,2]]
answer 4: e.g. 0 → 1 → 4 → 2 → 3
```

---

## 2. How to think about it

### Why "where am I" isn't enough
In a normal shortest path, the state is the current node. Here, being at node 0 after visiting {0, 1} is very different from being at 0 after visiting {0, 1, 2}. The state must also remember **which nodes have been visited**.

### Bitmasks: a set as an int
With n ≤ 12, a set of nodes fits in the low 12 bits of an `int`: bit i is 1 if node i was visited.

| set | mask (binary) | value |
|---|---|---|
| {0} | 0001 | 1 |
| {0, 1} | 0011 | 3 |
| {0, 2, 3} | 1101 | 13 |
| all four | 1111 | 15 = (1 << 4) − 1 |

```java
mask | (1 << j)            // add node j
(mask & (1 << j)) != 0     // is node j in the set?
```

### BFS over (node, mask)
```
state = (node, mask)                 n · 2ⁿ = 12 · 4096 = 49,152 states at most
start: (i, 1 << i) for EVERY i       (multi-source: we can start anywhere)
move:  (u, mask) → (v, mask | 1 << v) for each neighbour v
goal:  mask == full
```
Every move costs 1, so BFS reaches the goal in the fewest steps.

### Another way: Held-Karp (the travelling salesman DP)
1. Compute all-pairs shortest distances `dist[i][j]` with a BFS from each node.
2. `dp[mask][i]` = the shortest walk that has visited exactly `mask` and ends at i.
   - start: `dp[1 << i][i] = 0`
   - extend: `dp[mask | 1 << j][j] = min(dp[mask][i] + dist[i][j])` for each j not in `mask`
3. Answer: `min over i of dp[full][i]`.

It's the same idea, but each step jumps straight to the next **new** node along a shortest path.

### Dry run: the star `[[1,2,3],[0],[0],[0]]` (BFS)
| level | some states (node, mask) |
|---|---|
| 0 | (0,0001) (1,0010) (2,0100) (3,1000) |
| 1 | (1,0011) (2,0101) (3,1001) (0,0011) (0,0101) (0,1001) |
| 2 | from (0,0011): (2,0111) (3,1011) … |
| 3 | from (2,0111): (0,0111) … |
| 4 | from (0,0111): (3,**1111**) → done |

Answer **4**. Held-Karp agrees: `dist` from a leaf to the centre is 1 and between leaves is 2, so the order 1, 0, 2, 3 costs 1 + 1 + 2 = 4.

---

## 3. Java concepts used in this problem

### 3.1 Bit operations
| Expression | Meaning |
|---|---|
| `1 << i` | an int with only bit i set (= 2ⁱ) |
| `mask \| (1 << i)` | the set plus node i |
| `mask & (1 << i)` | non-zero if node i is in the set |
| `(1 << n) - 1` | n ones: the full set |
| `Integer.bitCount(mask)` | how many nodes are in the set |

### 3.2 A 2-D visited array indexed by a mask
```java
boolean[][] seen = new boolean[n][1 << n];
```
`seen[node][mask]`: 12 × 4096 booleans is only 48 KB.

### 3.3 `nextPermutation` (in the referee)
It rearranges an array into the next permutation in lexicographic order, and returns `false` after the last one. Looping `do { … } while (nextPermutation(perm))` from the sorted array visits all n! orders.

### 3.4 Converting a `Set<Integer>` to `int[]`
```java
adj.get(i).stream().mapToInt(Integer::intValue).toArray();
```
That's how the random-graph generator builds each adjacency row.

---

## 4. The referee: try every visiting order (n ≤ 7)

### Code ([A09_ShortestPathVisitingAllNodes.java:44-56](A09_ShortestPathVisitingAllNodes.java#L44-L56))
```java
static int shortestPathBrute(int[][] graph) {
    int n = graph.length;
    int[][] dist = allPairs(graph);
    int[] perm = new int[n];
    for (int i = 0; i < n; i++) perm[i] = i;
    int best = Integer.MAX_VALUE;
    do {
        int len = 0;
        for (int i = 0; i + 1 < n; i++) len += dist[perm[i]][perm[i + 1]];
        best = Math.min(best, len);
    } while (nextPermutation(perm));
    return best;
}
```

Any walk visiting all nodes has an order of **first visits**; between consecutive first visits it's best to take a shortest path. So the answer is the minimum, over all orders, of the sum of distances. There are n! orders, so this only works for small n.

`allPairs` ([lines 69-83](A09_ShortestPathVisitingAllNodes.java#L69-L83)) runs a BFS from every node (unweighted graph, so BFS gives shortest distances).

---

## 5. Approach 1: All-pairs distances + Held-Karp DP

### Code ([A09_ShortestPathVisitingAllNodes.java:86-104](A09_ShortestPathVisitingAllNodes.java#L86-L104))
```java
static int shortestPathDP(int[][] graph) {
    int n = graph.length, full = (1 << n) - 1;
    int[][] dist = allPairs(graph);
    int[][] dp = new int[1 << n][n];
    for (int[] row : dp) Arrays.fill(row, Integer.MAX_VALUE);
    for (int i = 0; i < n; i++) dp[1 << i][i] = 0;
    for (int mask = 1; mask <= full; mask++)
        for (int i = 0; i < n; i++) {
            if (dp[mask][i] == Integer.MAX_VALUE) continue;
            for (int j = 0; j < n; j++) {
                if ((mask & (1 << j)) != 0) continue;
                int next = mask | (1 << j);
                dp[next][j] = Math.min(dp[next][j], dp[mask][i] + dist[i][j]);
            }
        }
    int best = Integer.MAX_VALUE;
    for (int i = 0; i < n; i++) best = Math.min(best, dp[full][i]);
    return best;
}
```

| Line | What it does |
|---|---|
| `dp[1 << i][i] = 0` | start at any node, having visited only it |
| `for mask = 1 .. full` | a mask always grows when extended, so increasing order means every `dp[mask]` is final before it's used |
| `MAX_VALUE → continue` | unreachable state (also avoids overflow when adding) |
| `(mask & (1 << j)) != 0 → continue` | j is already visited |
| the update | walk from i to j along a shortest path |

**Cost:** **O(2ⁿ · n²)** time, O(2ⁿ · n) space.

---

## 6. Approach 2: BFS over (node, mask) (main solution)

### Code ([A09_ShortestPathVisitingAllNodes.java:107-130](A09_ShortestPathVisitingAllNodes.java#L107-L130))
```java
static int shortestPathLength(int[][] graph) {
    int n = graph.length, full = (1 << n) - 1;
    if (n == 1) return 0;
    boolean[][] seen = new boolean[n][1 << n];
    Deque<int[]> queue = new ArrayDeque<>();                         // {node, mask}
    for (int i = 0; i < n; i++) {                                    // multi-source: start anywhere
        queue.add(new int[]{i, 1 << i});
        seen[i][1 << i] = true;
    }
    for (int steps = 0; !queue.isEmpty(); steps++) {
        for (int size = queue.size(); size > 0; size--) {
            int[] cur = queue.poll();
            if (cur[1] == full) return steps;
            for (int next : graph[cur[0]]) {
                int mask = cur[1] | (1 << next);
                if (!seen[next][mask]) {
                    seen[next][mask] = true;
                    queue.add(new int[]{next, mask});
                }
            }
        }
    }
    return -1;                                                       // unreachable for a connected graph
}
```

### Line by line
| Line | What it does |
|---|---|
| `n == 1 → 0` | a single node is visited at the start |
| all `(i, 1 << i)` at level 0 | multi-source BFS: like starting from all nodes at once |
| `cur[1] == full` | every node visited: the current level is the answer |
| `mask = cur[1] \| (1 << next)` | moving to `next` adds it to the set (no change if it was already visited) |
| `seen[next][mask]` | each state is processed once |

### Complexity
- States: n · 2ⁿ. Each state scans its neighbours (≤ n).
- **O(2ⁿ · n²)** time in the worst case, **O(2ⁿ · n)** space. For n = 12: about 600,000 operations.

---

## 7. Comparing the approaches

| | Time | Notes |
|---|---|---|
| All orders | O(n! · n) | referee only (n ≤ 7) |
| Held-Karp DP | O(2ⁿ · n²) | needs all-pairs distances first |
| **BFS on (node, mask)** | O(2ⁿ · n²) | simplest; no distance matrix |

The file checks all three on 400 random connected graphs.

---

## 8. Edge cases and common mistakes

**Edge cases:** one node (0), a path (n − 1 steps), a star (revisiting the centre), a complete graph (n − 1 steps).

**Mistakes:**
1. A state of just `node`: BFS then stops too early or forbids needed revisits.
2. Starting from node 0 only (the best walk may start elsewhere).
3. Forbidding revisits: the star needs the centre several times.
4. Overflow in the DP (`MAX_VALUE + dist`): skip unreachable states.
5. `1 << n` with large n: this technique only works for small n (≤ about 20).

---

## 9. Interview questions and answers

**Q1. Why a bitmask in the state?**
> Progress depends on which nodes have been visited, not just where you are. With n ≤ 12, the visited set fits in an int, giving n · 2ⁿ states, small enough for BFS.

**Q2. Why multi-source BFS?**
> The walk can start anywhere. Putting every (i, {i}) in the queue at distance 0 is the same as adding a virtual start connected to all of them.

**Q3. How does it relate to TSP?**
> It's TSP on the shortest-path metric, except we don't return to the start. Held-Karp solves it in O(2ⁿ n²); the BFS gets there directly because edges are unweighted.

### Follow-up questions

**F1. "Weighted edges."**
> BFS no longer works: use Dijkstra on (node, mask) states, or Held-Karp with Dijkstra/Floyd–Warshall distances.

**F2. "Must return to the start."**
> Held-Karp: the answer is `min(dp[full][i] + dist[i][start])` for a fixed start.

**F3. "Visit only a subset of nodes (must-visit list)."**
> Bitmask over the required nodes only, with all-pairs distances between them: the same DP.

---

## 10. Running the file

```bash
java A09_ShortestPathVisitingAllNodes.java     # 5/5 tests passed (star, example 2, single node, 12-node path, 400 random graphs)
```

**One-line summary:** *"Multi-source BFS over states (node, bitmask of visited nodes), starting from every (i, 1<<i); the first time a state with the full mask comes off the queue, its level is the answer. Held-Karp DP over masks with all-pairs distances gives the same result."*
