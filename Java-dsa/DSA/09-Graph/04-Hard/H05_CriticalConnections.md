# H05 · Critical Connections in a Network

| | |
|---|---|
| **LeetCode** | [#1192 Critical Connections in a Network](https://leetcode.com/problems/critical-connections-in-a-network/) |
| **Difficulty** | Hard |
| **Pattern** | Bridges with Tarjan's low-link values (one DFS) |
| **Code** | [H05_CriticalConnections.java](H05_CriticalConnections.java) |
| **Run** | `java H05_CriticalConnections.java` |

The theory of bridges is in [01-Basics/BridgesAndEulerPaths](../01-Basics/BridgesAndEulerPaths.java); Union-Find (used by the referee) in [M11](../03-Medium/M11_NumberOfConnectedComponents.md).

---

## 1. The problem

n servers with undirected connections. A connection is **critical** if removing it disconnects some servers. Return all the critical connections.

```
n = 4, [[0,1],[1,2],[2,0],[1,3]]  →  [[1,3]]

  0 ─── 1 ─── 3          the triangle 0-1-2 survives losing any one edge,
   ╲   ╱                 but removing 1–3 cuts 3 off
     2
```

| network | critical |
|---|---|
| the example | [1, 3] |
| [[0,1]] | [0, 1] |
| a 4-cycle | none |

**Constraints:** up to 10⁵ servers and connections.

---

## 2. How to think about it

### A critical connection is a **bridge**
An edge is a bridge exactly when it is **not on any cycle**.

### Brute force
Remove each edge and test the connectivity: O(E · (V + E)), which is 10¹⁰ with 10⁵ edges. Too slow.

### Tarjan's algorithm (one DFS, O(V + E))
- `disc[u]` = when u was first visited (its DFS discovery time)
- `low[u]` = the smallest `disc` reachable from u's DFS subtree using at most **one back edge** (an edge to an already-visited vertex, other than the parent edge)

After exploring a child v of u:

```
low[u] = min(low[u], low[v])
if low[v] > disc[u]  →  v's subtree has NO way back to u or above
                     →  u–v is the only link  →  a BRIDGE
```

**Intuition:** a back edge from below creates a cycle that "protects" every tree edge it spans.

### Dry run: the example (from the real run)

| event | values |
|---|---|
| visit 0, 1, 2 | disc 0, 1, 2 |
| 2 → 0 is a back edge | low[2] = 0 |
| back in 1 from 2 | low[1] = min(1, 0) = 0; low[2] = 0 is not > disc[1] = 1 → 1–2 is protected |
| visit 3 | disc 3; no other edges |
| back in 1 from 3 | low[3] = 3 > disc[1] = 1 → **bridge 1–3** |
| back in 0 from 1 | low[0] = 0; 0–1 is not a bridge |
| 0 → 2 (already visited) | low[0] = min(0, 2) = 0 |

→ `[[1, 3]]` ✓

---

## 3. Java concepts used in this problem

### 3.1 Two arrays filled during one DFS

```java
disc[u] = low[u] = timer++;          // chained assignment, then increment
```

`timer++` gives the current value (the discovery time) and advances the counter. `disc[]` filled with −1 doubles as "not visited yet".

### 3.2 Skipping the parent edge

```java
if (v == parent) continue;
```

In an undirected graph, the edge back to the parent would look like a back edge and wrongly make every edge "protected". (With **parallel** edges between the same pair, you'd track the edge index instead of the parent vertex, as in [M10](../03-Medium/M10_GraphValidTree.md)'s referee.)

### 3.3 `low[u] = min(low[u], disc[v])` for an already-visited v

The edge u → v reaches an earlier vertex (or a descendant that's already finished: then disc[v] > disc[u], and the min changes nothing). Either way, the rule is safe.

### 3.4 Deep recursion

The DFS can be 10⁵ calls deep on a path-shaped network. On the JVM, run with a bigger stack (`-Xss`), run the DFS in a thread with a large stack, or rewrite it iteratively for production code.

---

## 4. Approach 1: Remove each edge, count the components (the referee)

### Code ([H05_CriticalConnections.java:73-92](H05_CriticalConnections.java#L73-L92))

```java
static List<List<Integer>> brute(int n, List<List<Integer>> cs) {
    List<List<Integer>> out = new ArrayList<>();
    int base = comps(n, cs, -1);
    for (int i = 0; i < cs.size(); i++) if (comps(n, cs, i) > base) out.add(cs.get(i));
    return out;
}

static int comps(int n, List<List<Integer>> cs, int skip) {
    int[] p = new int[n];
    for (int i = 0; i < n; i++) p[i] = i;
    int c = n;
    for (int i = 0; i < cs.size(); i++) {
        if (i == skip) continue;
        int a = root(p, cs.get(i).get(0)), b = root(p, cs.get(i).get(1));
        if (a != b) { p[a] = b; c--; }
    }
    return c;
}

static int root(int[] p, int x) { while (p[x] != x) x = p[x]; return x; }
```

| Line | What it does |
|---|---|
| `base` | the number of components with every edge |
| `comps(n, cs, i)` | the components without edge i (Union-Find) |
| `> base` | removing edge i split a component → it's a bridge |

**O(E · E · α).** Correct by definition.

---

## 5. Approach 2: Tarjan's low-link DFS (the file's main solution)

### Code ([H05_CriticalConnections.java:44-70](H05_CriticalConnections.java#L44-L70))

```java
static int timer;

static List<List<Integer>> criticalConnections(int n, List<List<Integer>> connections) {
    List<List<Integer>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (List<Integer> c : connections) { adj.get(c.get(0)).add(c.get(1)); adj.get(c.get(1)).add(c.get(0)); }
    int[] disc = new int[n], low = new int[n];
    Arrays.fill(disc, -1);
    timer = 0;
    List<List<Integer>> bridges = new ArrayList<>();
    for (int u = 0; u < n; u++) if (disc[u] == -1) dfs(u, -1, adj, disc, low, bridges);
    return bridges;
}

static void dfs(int u, int parent, List<List<Integer>> adj, int[] disc, int[] low, List<List<Integer>> bridges) {
    disc[u] = low[u] = timer++;
    for (int v : adj.get(u)) {
        if (v == parent) continue;
        if (disc[v] == -1) {
            dfs(v, u, adj, disc, low, bridges);
            low[u] = Math.min(low[u], low[v]);
            if (low[v] > disc[u]) bridges.add(List.of(u, v));      // v's subtree can't climb past u
        } else {
            low[u] = Math.min(low[u], disc[v]);                    // back edge
        }
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| the outer loop | handle a disconnected network (every component) |
| `disc[u] = low[u] = timer++` | discovery time; initially u can only "reach" itself |
| `v == parent` | ignore the edge we came along |
| an unvisited v | a tree edge: recurse, then inherit v's low |
| `low[v] > disc[u]` | nothing below v reaches u or an ancestor → u–v is a bridge |
| an already-visited v | a back edge: u's subtree can reach disc[v] |

### Complexity
- **Time O(V + E).**
- **Space O(V + E).**

---

## 6. Comparing the approaches

| | Remove each edge | Tarjan |
|---|---|---|
| Time | O(E² · α) | **O(V + E)** |
| Space | O(V) | O(V + E) |

The file checks Tarjan against remove-each-edge on 500 random connected networks.

---

## 7. Edge cases and common mistakes

**Edge cases:** a tree (every edge is a bridge), a single cycle (none), two cycles joined by one edge (that edge), n = 2.

**Mistakes:**
1. Using `>=` instead of `>` (that's the test for articulation **points**, not bridges).
2. Not skipping the parent edge (no bridges would ever be found).
3. Updating `low[u]` from a back edge with `low[v]` instead of `disc[v]` (it works for bridges, but it's the textbook-incorrect form and breaks articulation points).
4. Stack overflow on long paths.

---

## 8. Interview questions and answers

**Q1. What does low[v] > disc[u] mean intuitively?**
> No vertex in v's subtree has an edge to u or anything discovered before u. So the only way out of the subtree is the edge u–v itself: removing it disconnects the subtree.

**Q2. How do you find articulation points (critical servers)?**
> A non-root u is an articulation point if some child v has `low[v] >= disc[u]`; the root is one if it has ≥ 2 DFS children.

### Follow-up questions

**F1. 2-edge-connected components.**
> Remove the bridges; the remaining connected components are the 2-edge-connected components.

**F2. Strongly connected components in a directed graph.**
> Tarjan's SCC algorithm uses the same disc/low idea with a stack (or Kosaraju's two-pass DFS).

**F3. Make the network bridge-free with the fewest new edges.**
> Contract the 2-edge-connected components into a tree; the answer is ⌈leaves / 2⌉.

---

## 9. Running the file

```bash
java H05_CriticalConnections.java      # 4/4 tests passed (including 500 random networks vs remove-each-edge)
```

**One-line summary:** *"One DFS computing discovery times and low-links (the earliest vertex reachable via one back edge); a tree edge u–v is a bridge iff low[v] > disc[u]."*
