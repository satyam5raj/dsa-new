# A27 · Largest Color Value in a Directed Graph ⭐

| | |
|---|---|
| **LeetCode** | [#1857 Largest Color Value in a Directed Graph](https://leetcode.com/problems/largest-color-value-in-a-directed-graph/) |
| **Difficulty** | Hard |
| **Mix of structures** | **Topological sort** (Kahn, queue) + **DP table** `count[node][color]` + **cycle detection** |
| **Code** | [A27_LargestColorValueInDirectedGraph.java](A27_LargestColorValueInDirectedGraph.java) |
| **Run** | `java A27_LargestColorValueInDirectedGraph.java` |

⭐ = not in `leetcode.md`, added because it's the best example of "topological order + DP" in one problem.

---

## 1. The problem

A directed graph with n nodes; node i has a colour `colors.charAt(i)` from `'a'` to `'z'`. The **colour value** of a path is the number of times its **most frequent** colour appears on it. Return the largest colour value over all paths, or **−1 if the graph has a cycle**.

```
colors = "abaca", edges = [[0,1],[0,2],[2,3],[3,4]]
→ 3       the path 0 → 2 → 3 → 4 has colours a, a, c, a: 'a' appears 3 times

colors = "a", edges = [[0,0]]
→ −1      a self-loop is a cycle
```

---

## 2. How to think about it

### Why −1 for a cycle
On a cycle you could go round forever, making a colour's count as large as you like. So the answer only makes sense on a **DAG**.

### The DP
```
count[v][c] = the most times colour c can appear on a path ENDING at v
count[v][c] = max over edges u → v of count[u][c]      (+1 if v's own colour is c)
```
To compute `count[v]` we need every `count[u]` for its predecessors finished first, which means processing nodes in **topological order**.

### Kahn's algorithm does two jobs at once
1. Start with every node of indegree 0. Pop a node, "remove" its outgoing edges, and push any node whose indegree drops to 0. The pop order is a **topological order**.
2. If fewer than n nodes are ever popped, the rest are stuck on (or behind) a **cycle**, so return −1.

When u is popped: add 1 for its own colour, then push its counts forward to each child (`count[v][c] = max(count[v][c], count[u][c])`).

### A subtle detail: only check u's own colour
For the answer, the code only looks at `count[u][own colour of u]`. That's enough: the best path for colour c, cut at its **last** c-coloured node, is a path ending at a node whose own colour **is** c, and that node is checked when it's popped.

### Dry run: "abaca"
`0(a) → 1(b)`, `0 → 2(a) → 3(c) → 4(a)`

| pop | own colour | counts after (only non-zero) | best |
|---|---|---|---|
| 0 | a | a:1 → pushed to 1 and 2 | 1 |
| 1 | b | a:1, b:1 | 1 |
| 2 | a | a:2 → pushed to 3 | 2 |
| 3 | c | a:2, c:1 → pushed to 4 | 2 |
| 4 | a | a:3, c:1 | **3** |

All 5 nodes were popped, so there's no cycle; the answer is **3**.

---

## 3. Java concepts used in this problem

### 3.1 `charAt(i) - 'a'`
Characters are numbers: `'a'` is 97, `'c'` is 99, so `'c' − 'a' = 2`. That turns a letter into an index 0..25 for the `count[node][26]` table.

### 3.2 Indegree array + queue (Kahn)
```java
for (int[] e : edges) indegree[e[1]]++;
...
if (--indegree[v] == 0) queue.add(v);
```
A node is ready when everything pointing into it has been processed. See [Graph basics: TopologicalSort](../../09-Graph/01-Basics/TopologicalSort.java) and [Course Schedule II](../../09-Graph/03-Medium/M04_CourseScheduleII.md).

### 3.3 Three-state DFS for cycle detection (in the referee)
`state[u]`: 0 = not visited, 1 = on the current DFS path, 2 = finished. Reaching a node in state 1 means we went around a loop. A plain visited flag isn't enough, because it can't tell "on my current path" from "finished earlier".

### 3.4 Backtracking a counter array
```java
count[c]++;  ... recurse ...  count[c]--;                    // undo before returning
```
The referee walks every path with one shared `count` array, undoing each change on the way back.

---

## 4. The referee: DFS cycle check, then walk every path

### Code ([A27_LargestColorValueInDirectedGraph.java:46-79](A27_LargestColorValueInDirectedGraph.java#L46-L79))
```java
static int largestPathValueBrute(String colors, int[][] edges) {
    int n = colors.length();
    List<List<Integer>> adj = adjacency(n, edges);
    int[] state = new int[n];                                         // 0 new, 1 on the stack, 2 done
    for (int i = 0; i < n; i++) if (hasCycle(adj, i, state)) return -1;
    int best = 0;
    for (int s = 0; s < n; s++) best = Math.max(best, walk(colors, adj, s, new int[26]));
    return best;
}

private static boolean hasCycle(List<List<Integer>> adj, int u, int[] state) {
    if (state[u] == 1) return true;                                   // back to a node on the current path
    if (state[u] == 2) return false;
    state[u] = 1;
    for (int v : adj.get(u)) if (hasCycle(adj, v, state)) return true;
    state[u] = 2;
    return false;
}

private static int walk(String colors, List<List<Integer>> adj, int u, int[] count) {
    int c = colors.charAt(u) - 'a';
    count[c]++;
    int best = Arrays.stream(count).max().getAsInt();
    for (int v : adj.get(u)) best = Math.max(best, walk(colors, adj, v, count));
    count[c]--;                                                       // undo before returning
    return best;
}

static List<List<Integer>> adjacency(int n, int[][] edges) {
    List<List<Integer>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (int[] e : edges) adj.get(e[0]).add(e[1]);
    return adj;
}
```

It applies the definition directly: no cycle, then try every path from every start. It's exponential, so the tests use n ≤ 7.

---

## 5. Main solution: Kahn's topological sort + `count[node][colour]`

### Code ([A27_LargestColorValueInDirectedGraph.java:82-105](A27_LargestColorValueInDirectedGraph.java#L82-L105))
```java
static int largestPathValue(String colors, int[][] edges) {
    int n = colors.length();
    List<List<Integer>> adj = adjacency(n, edges);
    int[] indegree = new int[n];
    for (int[] e : edges) indegree[e[1]]++;

    int[][] count = new int[n][26];                                   // best count of each colour on a path ending here
    Deque<Integer> queue = new ArrayDeque<>();
    for (int i = 0; i < n; i++) if (indegree[i] == 0) queue.add(i);

    int processed = 0, best = 0;
    while (!queue.isEmpty()) {
        int u = queue.poll();
        processed++;
        int own = colors.charAt(u) - 'a';
        count[u][own]++;                                              // u itself is on every path ending at u
        best = Math.max(best, count[u][own]);
        for (int v : adj.get(u)) {
            for (int c = 0; c < 26; c++) count[v][c] = Math.max(count[v][c], count[u][c]);
            if (--indegree[v] == 0) queue.add(v);
        }
    }
    return processed == n ? best : -1;                                // unprocessed nodes are stuck on a cycle
}
```

### Line by line
| Line | What it does |
|---|---|
| `indegree` | how many edges point into each node |
| seed the queue | nodes with no incoming edges can start paths |
| `count[u][own]++` | everything pushed into u came from predecessors; now add u itself |
| `best = max(..., count[u][own])` | the only colour that can improve at u |
| push counts to v | v's best path may come through u |
| `--indegree[v] == 0` | all of v's predecessors are finished |
| `processed == n` | otherwise a cycle exists |

### Complexity
**O((n + m) · 26)** time, **O(n · 26)** space.

---

## 6. Comparing the approaches

| | Time |
|---|---|
| DFS over every path | exponential |
| **Kahn + DP** | **O((n + m) · 26)** |

The file checks the two on 2,000 random graphs: half forced acyclic (edges always go from a lower to a higher node), half random (often with cycles and self-loops).

---

## 7. Edge cases and common mistakes

**Edge cases:** no edges (answer 1), a self-loop (−1), a cycle in a part of the graph unrelated to the best path (still −1), several paths merging into one node, a single node.

**Mistakes:**
1. Plain DFS with memo but no cycle detection (infinite recursion or wrong answers).
2. Adding u's own colour **before** receiving its predecessors' counts (here it's added when u is popped, after all of them).
3. Only tracking the single best colour per node instead of all 26 (a colour that's behind now can win later).
4. Forgetting that **any** cycle makes the answer −1, even far from the best path.
5. A two-state visited array for cycle detection.

---

## 8. Interview questions and answers

**Q1. Why topological order?**
> `count[v]` depends on all of v's predecessors. Processing in topological order guarantees they're finished first, the standard way to do DP on a DAG.

**Q2. How do you detect the cycle?**
> Kahn's algorithm: if some nodes never reach indegree 0, they're on or behind a cycle, so fewer than n nodes get processed.

**Q3. Why keep 26 counts per node instead of one?**
> The colour that's most frequent so far might not be the one that wins later. Keeping all 26 lets each colour be maximised independently.

### Follow-up questions

**F1. "Return the path."**
> Store, for each (node, colour), the predecessor that gave the max; walk back from the best (node, colour).

**F2. "Weighted nodes: maximise the total weight of one colour."**
> The same DP, with `+weight` instead of `+1`.

**F3. "Other DP-on-DAG problems?"**
> [A21 Longest Increasing Path](A21_LongestIncreasingPathInMatrix.md), Parallel Courses III (the longest weighted path), and Longest Path in a DAG.

---

## 9. Running the file

```bash
java A27_LargestColorValueInDirectedGraph.java     # 6/6 tests passed (example, self loop, no edges, merge, cycle elsewhere, 2,000 random graphs)
```

**One-line summary:** *"Run Kahn's topological sort while carrying count[node][26], the best count of each colour on a path ending at that node; add the node's own colour when it's popped, push the counts to its children, and return −1 if not every node gets processed."*
