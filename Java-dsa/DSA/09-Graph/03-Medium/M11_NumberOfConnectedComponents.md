# M11 · Number of Connected Components in an Undirected Graph

| | |
|---|---|
| **LeetCode** | [#323 Number of Connected Components in an Undirected Graph](https://leetcode.com/problems/number-of-connected-components-in-an-undirected-graph/) (premium) |
| **Difficulty** | Medium |
| **Pattern** | Count how many times a DFS has to **start** (or Union-Find: n − successful unions) |
| **Code** | [M11_NumberOfConnectedComponents.java](M11_NumberOfConnectedComponents.java) |
| **Run** | `java M11_NumberOfConnectedComponents.java` |

Graph basics are explained in [E01](../02-Easy/E01_FindIfPathExists.md#3-java-concepts-used-in-this-problem). The grid version is [M01 Number of Islands](M01_NumberOfIslands.md).

---

## 1. The problem

n nodes (0..n−1) and undirected edges. How many connected components are there?

```
n = 5, [[0,1],[1,2],[3,4]]  →  2          0 ─ 1 ─ 2     3 ─ 4
n = 5, [[0,1],[1,2],[2,3],[3,4]]  →  1    0 ─ 1 ─ 2 ─ 3 ─ 4
n = 4, []  →  4                           (every node alone)
```

---

## 2. How to think about it

### DFS: count the starts
```
for v in 0..n−1:
    if not visited:  components++,  DFS(v) marks its whole component
```

Each DFS start discovers exactly one new component. Number of Islands is the grid version of this.

### Union-Find: count the merges
Start with n components (every node alone). Every edge that joins two **different** sets merges them → components−−. Edges inside one set change nothing.

It's handy when the edges arrive **one at a time** ("how many components after each edge?"), where re-running a DFS would be too slow. ([11-Union-Find](../../11-Union-Find/README.md))

---

## 3. Java concepts used in this problem

### 3.1 A compact iterative DFS

```java
while (!stack.isEmpty()) for (int w : adj.get(stack.pop())) if (!seen[w]) { seen[w] = true; stack.push(w); }
```

One line: pop a node, push each unseen neighbour (marking it). It's equivalent to the multi-line versions in earlier notes.

### 3.2 Union-Find in a few lines

```java
int[] parent = new int[n];                   // parent[i] == i → i is a root (the set's representative)
static int find(int[] p, int x) {
    while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; }   // walk to the root, halving the path as we go
    return x;
}
// union: a = find(u), b = find(v); if a != b: parent[a] = b   (merge the two sets)
```

- `find` returns the **representative** (root) of x's set; two nodes are connected iff they have the same root.
- `p[x] = p[p[x]]` is **path halving**: each step makes x point to its grandparent, which flattens the tree and keeps later finds fast.
- With path compression (and union by rank/size), each operation is nearly O(1): O(α(n)), where α is the inverse Ackermann function (≤ 4 for any practical n).

---

## 4. Approach 1: DFS from each unvisited node (the file's main solution)

### Code ([M11_NumberOfConnectedComponents.java:28-42](M11_NumberOfConnectedComponents.java#L28-L42))

```java
static int countComponents(int n, int[][] edges) {
    List<List<Integer>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }
    boolean[] seen = new boolean[n];
    int count = 0;
    for (int v = 0; v < n; v++) {
        if (seen[v]) continue;
        count++;                                          // v starts a new component
        Deque<Integer> stack = new ArrayDeque<>(List.of(v));
        seen[v] = true;
        while (!stack.isEmpty()) for (int w : adj.get(stack.pop())) if (!seen[w]) { seen[w] = true; stack.push(w); }
    }
    return count;
}
```

### Line by line

| Line | What it does |
|---|---|
| the adjacency list | undirected |
| `if (seen[v]) continue;` | already part of a counted component |
| `count++` | v is the first node seen in a new component |
| the DFS | mark that whole component |

### Dry run: n = 5, [[0,1],[1,2],[3,4]]

| v | seen? | action | count |
|---|---|---|---|
| 0 | no | DFS marks 0, 1, 2 | 1 |
| 1, 2 | yes | skip | 1 |
| 3 | no | DFS marks 3, 4 | 2 |
| 4 | yes | skip | 2 |

**2** ✓

### Complexity
- **Time O(V + E).**
- **Space O(V + E).**

---

## 5. Approach 2: Union-Find

### Code ([M11_NumberOfConnectedComponents.java:44-58](M11_NumberOfConnectedComponents.java#L44-L58))

```java
static int unionFind(int n, int[][] edges) {
    int[] parent = new int[n];
    for (int i = 0; i < n; i++) parent[i] = i;
    int count = n;
    for (int[] e : edges) {
        int a = find(parent, e[0]), b = find(parent, e[1]);
        if (a != b) { parent[a] = b; count--; }          // merged two components
    }
    return count;
}

static int find(int[] p, int x) {
    while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; }
    return x;
}
```

### Line by line

| Line | What it does |
|---|---|
| `parent[i] = i` | every node starts in its own set |
| `count = n` | n components to begin with |
| `a = find(u), b = find(v)` | the representatives of the two endpoints' sets |
| `a != b` | different sets → merge them, one fewer component |
| (`a == b`) | the edge is inside a component already: nothing changes |

### Dry run: n = 5, [[0,1],[1,2],[3,4]]

| edge | roots | merge? | parent after | count |
|---|---|---|---|---|
| | | | [0, 1, 2, 3, 4] | 5 |
| 0–1 | 0, 1 | yes | parent[0] = 1 | 4 |
| 1–2 | 1, 2 | yes | parent[1] = 2 | 3 |
| 3–4 | 3, 4 | yes | parent[3] = 4 | **2** |

### Complexity
- **Time O(E · α(n)) ≈ O(E)** (plus O(n) to initialise).
- **Space O(n).**

---

## 6. Comparing the approaches

| | DFS | Union-Find |
|---|---|---|
| Time | O(V + E) | O(V + E · α(V)) |
| Space | O(V + E) (adjacency list) | **O(V)** |
| Edges arriving over time | rerun everything | update incrementally |

The file checks DFS against Union-Find on 1,000 random graphs (with self-loops and duplicate edges).

---

## 7. Edge cases and common mistakes

**Edge cases:** no edges (n components), a complete graph (1), self-loops and duplicate edges (no effect), n = 1.

**Mistakes:**
1. Decrementing the count for every edge (edges inside a component must not count).
2. In Union-Find, setting `parent[u] = v` instead of joining the **roots**.
3. Forgetting the isolated nodes (they're components too).

---

## 8. Interview questions and answers

**Q1. DFS or Union-Find?**
> For a static graph, both are linear. Union-Find shines when the edges arrive dynamically, or when only connectivity (not the paths) is needed and memory for an adjacency list is a concern.

**Q2. What does path compression do?**
> It makes the nodes on a find path point closer to the root, so future finds on them are almost O(1).

### Follow-up questions

**F1. Number of Provinces.** → [M21](M21_NumberOfProvinces.md)
> The same problem with an adjacency-matrix input.

**F2. The number of components after each added edge.**
> Union-Find, reporting `count` after each union.

**F3. The minimum number of edges to connect everything.**
> components − 1 (Number of Operations to Make Network Connected, LC 1319, also needs at least n − 1 cables).

---

## 9. Running the file

```bash
java M11_NumberOfConnectedComponents.java      # 5/5 tests passed (including 1,000 random graphs: DFS == Union-Find)
```

**One-line summary:** *"Count the DFS starts from unvisited nodes, or start at n and subtract one for every edge that unions two different sets."*
