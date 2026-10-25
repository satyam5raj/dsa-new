# M10 · Graph Valid Tree

| | |
|---|---|
| **LeetCode** | [#261 Graph Valid Tree](https://leetcode.com/problems/graph-valid-tree/) (premium) |
| **Difficulty** | Medium |
| **Pattern** | Tree ⇔ exactly n − 1 edges **and** connected |
| **Code** | [M10_GraphValidTree.java](M10_GraphValidTree.java) |
| **Run** | `java M10_GraphValidTree.java` |

Graph basics are explained in [E01](../02-Easy/E01_FindIfPathExists.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

n nodes (0..n−1) and a list of undirected edges. Do they form a valid **tree**?

| n | edges | answer |
|---|---|---|
| 5 | [[0,1],[0,2],[0,3],[1,4]] | true |
| 5 | [[0,1],[1,2],[2,3],[1,3],[1,4]] | false (the cycle 1-2-3) |
| 4 | [[0,1],[1,2],[2,0]] | false (3 edges, but a cycle and node 3 is isolated) |
| 1 | [] | true |

---

## 2. How to think about it

### A tree = connected + no cycle
Graph theory gives a shortcut: **with exactly n − 1 edges, connected ⇔ acyclic**. So:
1. if `edges.length != n − 1` → false (too few: disconnected; too many: a cycle)
2. BFS/DFS from node 0; return whether all n nodes were reached

No explicit cycle detection is needed!

### Why the shortcut holds
- A connected graph on n nodes needs at least n − 1 edges.
- A connected graph with exactly n − 1 edges has no cycle (removing an edge from a cycle would leave it connected with n − 2 edges, which is impossible).

### The Union-Find version
For each edge, if both ends are already in the same set → it closes a cycle → false. ([11-Union-Find](../../11-Union-Find/README.md))

### Dry run
n = 5 with 5 edges → 5 ≠ 4 → **false** immediately.

---

## 3. Java concepts used in this problem

### 3.1 `new ArrayDeque<>(List.of(0))`

It creates a deque that already contains 0, a one-line way to seed the DFS stack.

### 3.2 Counting the reached nodes instead of scanning `seen`

```java
int reached = 1;
... if (!seen[v]) { seen[v] = true; reached++; stack.push(v); }
return reached == n;
```

That's the same result as checking that every `seen[i]` is true, without a second loop.

### 3.3 Undirected cycle detection needs the "came from" edge (in the referee)

In an undirected graph, every edge appears in both adjacency lists, so a DFS would "see" its parent as an already-visited neighbour. The referee skips the **edge it arrived by** (`ei == viaEdge`). Tracking the edge index rather than the parent node also handles parallel edges correctly (two edges between the same pair do form a cycle).

---

## 4. Approach 1: An explicit cycle check + connectivity (the referee)

### Code ([M10_GraphValidTree.java:52-70](M10_GraphValidTree.java#L52-L70))

```java
static boolean referee(int n, int[][] edges) {
    List<List<Integer>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (int i = 0; i < edges.length; i++) { adj.get(edges[i][0]).add(i); adj.get(edges[i][1]).add(i); }
    boolean[] seen = new boolean[n];
    if (cycle(0, -1, adj, edges, seen)) return false;
    for (boolean s : seen) if (!s) return false;
    return true;
}

static boolean cycle(int u, int viaEdge, List<List<Integer>> adj, int[][] edges, boolean[] seen) {
    seen[u] = true;
    for (int ei : adj.get(u)) {
        if (ei == viaEdge) continue;                    // don't go back along the same edge
        int v = edges[ei][0] == u ? edges[ei][1] : edges[ei][0];
        if (seen[v] || cycle(v, ei, adj, edges, seen)) return true;
    }
    return false;
}
```

| Line | What it does |
|---|---|
| `adj` stores **edge indices** | so the DFS knows which edge it came through |
| `ei == viaEdge → continue` | don't treat the edge back to the parent as a cycle |
| `seen[v]` | reaching an already-visited node by a **different** edge → a cycle |
| the `seen` scan | every node must be reached (connected) |

The definition checked directly: **O(V + E)**.

---

## 5. Approach 2: Union-Find

```java
static boolean validTreeUnionFind(int n, int[][] edges) {
    if (edges.length != n - 1) return false;
    int[] parent = new int[n];
    for (int i = 0; i < n; i++) parent[i] = i;
    for (int[] e : edges) {
        int a = find(parent, e[0]), b = find(parent, e[1]);
        if (a == b) return false;                       // both ends already connected → a cycle
        parent[a] = b;
    }
    return true;                                        // n − 1 successful unions → one component
}

static int find(int[] p, int x) {
    while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; }
    return x;
}
```

With n − 1 edges and no failed union, exactly n − 1 merges happened, leaving a single component. (Stress-tested against the DFS version.)

---

## 6. Approach 3: Edge count + DFS reachability (the file's main solution)

### Code ([M10_GraphValidTree.java:34-49](M10_GraphValidTree.java#L34-L49))

```java
static boolean validTree(int n, int[][] edges) {
    if (edges.length != n - 1) return false;            // a tree has exactly n - 1 edges
    List<List<Integer>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }
    boolean[] seen = new boolean[n];
    Deque<Integer> stack = new ArrayDeque<>(List.of(0));
    seen[0] = true;
    int reached = 1;
    while (!stack.isEmpty()) {
        for (int v : adj.get(stack.pop())) {
            if (!seen[v]) { seen[v] = true; reached++; stack.push(v); }
        }
    }
    return reached == n;                                // connected + n-1 edges = tree
}
```

### Line by line

| Line | What it does |
|---|---|
| `edges.length != n − 1` | the wrong number of edges for a tree |
| the adjacency list | undirected |
| the iterative DFS from 0 | mark everything reachable |
| `reached == n` | connected; with n − 1 edges that also means acyclic |

### Dry run: n = 4, [[0,1],[1,2],[2,0]]
3 edges = n − 1 ✓ → DFS from 0 reaches 0, 1, 2 → reached = 3 ≠ 4 → **false** ✓ (the edge count was right, but the cycle "used up" an edge, leaving node 3 disconnected).

### Complexity
- **Time O(V + E).**
- **Space O(V + E).**

---

## 7. Comparing the approaches

| | Explicit cycle + connectivity | Union-Find | Edge count + DFS |
|---|---|---|---|
| Time | O(V + E) | O(E · α(V)) | O(V + E) |
| Code | longest | short | short |
| Needs the n − 1 insight? | no | yes | yes |

The file checks the edge-count version against the explicit check on 2,000 random graphs.

---

## 8. Edge cases and common mistakes

**Edge cases:** n = 1 with no edges (a single node is a tree), parallel edges, self-loops, n − 1 edges containing a cycle.

**Mistakes:**
1. Checking only for "no cycle" (a forest isn't a tree).
2. Checking only connectivity (extra edges mean cycles).
3. In an undirected DFS, treating the parent as a cycle.

---

## 9. Interview questions and answers

**Q1. Why does n − 1 edges + connected imply no cycle?**
> A connected graph has a spanning tree with n − 1 edges. If the graph has exactly n − 1 edges, it **is** that spanning tree, so it has no cycle.

**Q2. When is Union-Find preferable?**
> When the edges arrive as a stream, or you need to report the first edge that creates a cycle (Redundant Connection, LC 684).

### Follow-up questions

**F1. Redundant Connection (LC 684): which edge, if removed, leaves a tree?**
> Union-Find: the first edge whose endpoints are already connected.

**F2. Count the components.** → [M11](M11_NumberOfConnectedComponents.md)

**F3. Minimum Height Trees.** → [M19](M19_MinimumHeightTrees.md)
> Peel the leaves layer by layer to find the centre(s) of the tree.

---

## 10. Running the file

```bash
java M10_GraphValidTree.java      # 5/5 tests passed (including 2,000 random graphs vs explicit check)
```

**One-line summary:** *"A graph is a tree iff it has exactly n − 1 edges and a DFS from node 0 reaches all n nodes."*
