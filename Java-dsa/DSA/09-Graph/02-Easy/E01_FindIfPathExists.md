# E01 · Find if Path Exists in Graph

| | |
|---|---|
| **LeetCode** | [#1971 Find if Path Exists in Graph](https://leetcode.com/problems/find-if-path-exists-in-graph/) |
| **Difficulty** | Easy |
| **Pattern** | Build the adjacency list, then BFS/DFS with a visited array |
| **Code** | [E01_FindIfPathExists.java](E01_FindIfPathExists.java) |
| **Run** | `java E01_FindIfPathExists.java` |

This is the first note of the Graph topic, so section 3 covers **graph vocabulary, representations, the adjacency list in Java, and BFS/DFS with a visited array** in full. Later notes give only short recaps.

---

## 1. The problem

An **undirected** graph has n vertices (0..n−1) and an edge list. Is there a path from `source` to `destination`?

```
n = 3, edges [[0,1],[1,2],[2,0]], 0 → 2          → true

    0 ── 1
     \  /
      2

n = 6, edges [[0,1],[0,2],[3,5],[5,4],[4,3]], 0 → 5  → false (different components)

    0 ── 1      3 ── 5
    |            \  /
    2             4
```

**Constraints:** `1 ≤ n ≤ 2 · 10⁵`, up to 2 · 10⁵ edges.

---

## 2. How to think about it

### The graph template (you'll write this in almost every problem)
1. edge list → **adjacency list** (both directions for an undirected graph)
2. BFS (or DFS) from the source with a `visited[]` array; **mark a vertex when you push it**
3. stop as soon as the destination is reached

Without `visited[]`, the cycle 0–1–2–0 would make the search loop forever.

(Union-Find also answers "are these two connected?": see [11-Union-Find](../../11-Union-Find/README.md).)

---

## 3. Java concepts used in this problem

### 3.1 Graph vocabulary

| Term | Meaning |
|---|---|
| **vertex (node)** | a point: here the integers 0..n−1 |
| **edge** | a connection between two vertices |
| **undirected / directed** | the edge works both ways / only from u to v |
| **weighted** | each edge has a cost (distance, time, …) |
| **neighbours (adjacent vertices)** | the vertices joined to u by an edge |
| **degree** | the number of edges at a vertex (in-degree / out-degree for directed graphs) |
| **path** | a sequence of vertices joined by edges |
| **cycle** | a path that returns to its start |
| **connected component** | a maximal group of vertices that can all reach each other |
| **DAG** | a directed acyclic graph (no directed cycles) |
| **V, E** | the number of vertices and of edges |

Trees, linked lists and grids are all special kinds of graphs. The two differences from trees that matter in code: graphs can have **cycles** (you need a `visited` set) and **several components** (you may need to try every vertex as a start).

### 3.2 Three ways to store a graph

```
edges: 0–1, 0–2, 1–2

edge list:        [[0,1], [0,2], [1,2]]              how most inputs arrive
adjacency matrix: m[u][v] = 1 if there is an edge     O(V²) space, O(1) edge check
                    0 1 2
                0 [ 0 1 1 ]
                1 [ 1 0 1 ]
                2 [ 1 1 0 ]
adjacency list:   0: [1, 2]   1: [0, 2]   2: [0, 1]   O(V + E) space: the interview default
```

| | space | "is u–v an edge?" | "neighbours of u" |
|---|---|---|---|
| edge list | O(E) | O(E) | O(E) |
| adjacency matrix | O(V²) | **O(1)** | O(V) |
| adjacency list | **O(V + E)** | O(deg u) | **O(deg u)** |

Searches need "the neighbours of u" over and over, which is why the adjacency list wins.

### 3.3 Building an adjacency list in Java

```java
List<List<Integer>> adj = new ArrayList<>();
for (int i = 0; i < n; i++) adj.add(new ArrayList<>());     // one empty list per vertex
for (int[] e : edges) {
    adj.get(e[0]).add(e[1]);
    adj.get(e[1]).add(e[0]);                                // undirected: add BOTH directions
}
for (int v : adj.get(u)) { ... }                            // loop over u's neighbours
```

Other forms you'll see: `List<Integer>[] adj = new List[n];` (an array of lists, which needs the unchecked cast from [Hash Table E03](../../06-Hash-Table/02-Easy/E03_DesignHashSet.md)), `Map<Integer, List<Integer>>` when vertices aren't 0..n−1 (e.g. strings), and `List<int[]>` per vertex for weighted edges (`{neighbour, weight}`).

### 3.4 BFS: breadth-first search with a queue

```
queue ← [source];  visited[source] = true
while the queue isn't empty:
    u = poll
    for each neighbour v of u:
        if not visited[v]: visited[v] = true; offer v
```

BFS explores in **rings**: everything 1 edge away, then 2 edges away, and so on. That's why it finds **shortest paths in unweighted graphs** (used heavily later, e.g. [M07 Rotting Oranges](../03-Medium/M07_RottingOranges.md)).

**Mark when you push, not when you pop.** Marking at pop time lets the same vertex be queued many times (once from each neighbour that reaches it first), which can blow up to O(E) queue entries and duplicate work.

### 3.5 DFS: depth-first search (recursion or a stack)

```java
static boolean[] visited;               // fields shared by the recursive calls
static List<List<Integer>> adj;

static void dfs(int u) {
    visited[u] = true;
    for (int v : adj.get(u)) if (!visited[v]) dfs(v);
}
```

DFS goes as deep as possible before backtracking. For "can I reach it?" questions, BFS and DFS are interchangeable. DFS is natural for cycles, topological order and components; BFS is the one for shortest paths. Deep recursion can overflow the stack on long chains, so use an explicit stack (`ArrayDeque.push/pop`) for big graphs.

### 3.6 Complexity: O(V + E)

Each vertex is pushed once (thanks to `visited`), and each edge is looked at twice (once from each end) in an undirected adjacency list. That's O(V + E) time, and O(V + E) space for the list.

---

## 4. Approach 1: Transitive closure (the test referee)

The file's test builds a reachability matrix with a triple loop (Floyd–Warshall style):

```java
static boolean pathByClosure(int n, int[][] edges, int s, int d) {
    boolean[][] r = new boolean[n][n];
    for (int i = 0; i < n; i++) r[i][i] = true;
    for (int[] e : edges) { r[e[0]][e[1]] = true; r[e[1]][e[0]] = true; }
    for (int k = 0; k < n; k++)
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                r[i][j] |= r[i][k] && r[k][j];          // i reaches j through k
    return r[s][d];
}
```

After processing intermediate vertex k, `r[i][j]` is true if j is reachable from i using only intermediates 0..k. **O(V³)**: fine for n ≤ 10 in a test, far too slow here. (Floyd–Warshall itself appears in [M15](../03-Medium/M15_FindTheCityWithSmallestNeighbors.md).)

---

## 5. Approach 2: BFS on the adjacency list (the file's solution)

### Code ([E01_FindIfPathExists.java:33-50](E01_FindIfPathExists.java#L33-L50))

```java
static boolean validPath(int n, int[][] edges, int source, int destination) {
    List<List<Integer>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }

    boolean[] visited = new boolean[n];
    Queue<Integer> q = new ArrayDeque<>();
    q.add(source);
    visited[source] = true;
    while (!q.isEmpty()) {
        int u = q.poll();
        if (u == destination) return true;
        for (int v : adj.get(u)) {
            if (!visited[v]) { visited[v] = true; q.add(v); }
        }
    }
    return false;
}
```

### Line by line

| Line | What it does |
|---|---|
| the first two loops | build the undirected adjacency list |
| `visited[source] = true` | mark the start as soon as it's queued |
| `u = q.poll()` | the next vertex in BFS order |
| `if (u == destination) return true;` | reached (this also covers source == destination) |
| the neighbour loop | queue every unvisited neighbour, marking it immediately |
| `return false` | the whole component of the source was explored without meeting the destination |

### Dry run 1: the triangle, 0 → 2

adj: 0: [1, 2], 1: [0, 2], 2: [1, 0]

| poll | == 2? | neighbours queued | queue after | visited |
|---|---|---|---|---|
| 0 | no | 1, 2 | [1, 2] | {0, 1, 2} |
| 1 | no | (0 and 2 already visited) | [2] | |
| 2 | **yes** → true | | | |

### Dry run 2: two components, 0 → 5

adj: 0: [1, 2], 1: [0], 2: [0], 3: [5, 4], 4: [5, 3], 5: [3, 4]

| poll | queued | queue after |
|---|---|---|
| 0 | 1, 2 | [1, 2] |
| 1 | — | [2] |
| 2 | — | [] |

The queue is empty → **false**. Vertices 3, 4 and 5 are never touched.

### Complexity
- **Time O(V + E).**
- **Space O(V + E).**

---

## 6. Approach 3: Iterative DFS with a stack

```java
static boolean validPathDfs(int n, int[][] edges, int source, int destination) {
    List<List<Integer>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }
    boolean[] visited = new boolean[n];
    Deque<Integer> stack = new ArrayDeque<>();
    stack.push(source);
    visited[source] = true;
    while (!stack.isEmpty()) {
        int u = stack.pop();
        if (u == destination) return true;
        for (int v : adj.get(u)) if (!visited[v]) { visited[v] = true; stack.push(v); }
    }
    return false;
}
```

The only change from BFS is a stack instead of a queue. The visiting order differs, but the set of reachable vertices is the same, so the answer is too. It's the same O(V + E), and it avoids recursion depth limits.

---

## 7. Comparing the approaches

| | Transitive closure | BFS | Iterative DFS |
|---|---|---|---|
| Time | O(V³) | O(V + E) | O(V + E) |
| Space | O(V²) | O(V + E) | O(V + E) |
| Also gives shortest path length? | no | **yes** | no |

The file checks BFS against the transitive closure on 500 random graphs (with self-loops and repeated edges).

---

## 8. Edge cases and common mistakes

**Edge cases:** source == destination, no edges, self-loops, duplicate edges, the vertices in different components.

**Mistakes:**
1. Adding only one direction of an undirected edge.
2. Forgetting `visited` (an infinite loop on any cycle).
3. Marking visited at poll time (duplicate queue entries).
4. Recursive DFS on a 2 · 10⁵-long chain → StackOverflowError.

---

## 9. Interview questions and answers

**Q1. BFS or DFS here?**
> Either: both explore exactly the source's connected component. BFS additionally gives the shortest number of edges, and an iterative version of either avoids stack overflows.

**Q2. Many queries "are a and b connected?" on the same graph?**
> Precompute the component id of every vertex once (BFS from each unvisited vertex), then each query is O(1). Or use Union-Find, which also handles edges being added over time.

**Q3. Why is the complexity O(V + E) and not O(V · E)?**
> Each vertex is processed once, and when it's processed, its neighbour list is scanned once. Summed over all vertices, the list lengths add up to 2E.

### Follow-up questions

**F1. Return the path itself.**
> Store `parent[v] = u` when v is first queued; walk back from the destination.

**F2. A directed graph.**
> Add only `adj[u].add(v)`; the same BFS answers "can u reach v?".

**F3. Count the connected components.** → [M11](../03-Medium/M11_NumberOfConnectedComponents.md)
> Loop over all vertices; start a new BFS from each unvisited one, counting the starts.

---

## 10. Running the file

```bash
java E01_FindIfPathExists.java      # 4/4 tests passed (including 500 random graphs vs transitive closure)
```

**One-line summary:** *"Build an adjacency list (both directions), BFS from the source marking vertices when you push them, and return true when the destination is polled."*
