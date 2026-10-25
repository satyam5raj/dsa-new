# E01 · Find if Path Exists in Graph: the Union-Find Way ⭐

| | |
|---|---|
| **LeetCode** | [#1971 Find if Path Exists in Graph](https://leetcode.com/problems/find-if-path-exists-in-graph/) |
| **Difficulty** | Easy |
| **Pattern** | Union every edge, then ask find(source) == find(destination) |
| **Code** | [E01_FindIfPathExistsUnionFind.java](E01_FindIfPathExistsUnionFind.java) |
| **Run** | `java E01_FindIfPathExistsUnionFind.java` |

⭐ = not in `leetcode.md`, added for a key technique. The same problem was solved with BFS in [Graph E01](../../09-Graph/02-Easy/E01_FindIfPathExists.md). This is the first note of the Union-Find topic, so section 3 covers **the data structure itself** in full: the parent array, find, union by size, path compression and the costs. Later notes give only short recaps.

---

## 1. The problem

An undirected graph with n vertices and an edge list. Is there a path between `source` and `destination`?

| n | edges | query | answer |
|---|---|---|---|
| 3 | [[0,1],[1,2],[2,0]] | 0 → 2 | true |
| 6 | [[0,1],[0,2],[3,5],[5,4],[4,3]] | 0 → 5 | false (different components) |

---

## 2. How to think about it

"Is there a path?" in an **undirected** graph = "are they in the same **component**?". That's exactly what Union-Find answers:

```
for each edge (a, b):  union(a, b)
return find(source) == find(destination)
```

No adjacency list, no queue, no visited array: just two int arrays.

### When is Union-Find better than BFS here?
- the edges arrive **one at a time**, and you're asked after each one
- **many** (source, destination) queries on the same graph: each query is about O(1)

### When is BFS better?
When you need the **path itself** or its **length**. Union-Find only says yes or no.

This is the template every other note in this topic builds on.

---

## 3. Java concepts used in this problem

### 3.1 What Union-Find (a disjoint set union, DSU) stores

Every element points to a **parent**; following the parents leads to the group's **root** (its representative). Each group is a tree, and the whole structure is a forest.

```
parent[] = [0, 0, 1, 3, 3]          groups: {0, 1, 2} with root 0,  {3, 4} with root 3

  0        3
  |        |
  1        4
  |
  2
```

Initially every element is its own root: `parent[i] = i` (n groups of one).

### 3.2 `find(x)`: the root of x's group

```java
static int find(int x) {
    while (parent[x] != x) {
        parent[x] = parent[parent[x]];       // path halving: skip a level
        x = parent[x];
    }
    return x;
}
```

Two elements are in the same group ⇔ they have the same root.

**Path compression / halving:** while walking up, make nodes point higher (here, to their grandparent). The trees get flatter, so future finds are almost immediate. (Full path compression points every node on the path straight at the root; halving is a simpler loop with the same guarantees.)

### 3.3 `union(a, b)`: merge two groups

```java
static void union(int a, int b) {
    int ra = find(a), rb = find(b);
    if (ra == rb) return;                    // already together
    if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }
    parent[rb] = ra;                         // hang the SMALLER tree under the larger
    size[ra] += size[rb];
}
```

- Always link **roots** (never `parent[a] = b` directly).
- **Union by size:** hanging the smaller tree under the larger keeps the trees shallow (height O(log n) even without compression).
- The returned or observed "were they already together?" is the basis of cycle detection ([M01](../03-Medium/M01_RedundantConnection.md)).

### 3.4 The cost: α(n)

With union by size **and** path compression, each operation costs O(α(n)), where α is the inverse Ackermann function. α(n) ≤ 4 for any n you'll ever see, so in practice it's constant time.

### 3.5 What Union-Find can't do

- **split** groups (deleting edges)
- find **paths** or distances
- work with **directed** reachability

For those, use BFS/DFS ([09-Graph](../../09-Graph/README.md)).

---

## 4. Approach 1: BFS (the referee)

### Code ([E01_FindIfPathExistsUnionFind.java:55-64](E01_FindIfPathExistsUnionFind.java#L55-L64))

```java
static boolean bfs(int n, int[][] edges, int s, int d) {
    List<List<Integer>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }
    boolean[] seen = new boolean[n];
    Deque<Integer> q = new ArrayDeque<>(List.of(s));
    seen[s] = true;
    while (!q.isEmpty()) for (int v : adj.get(q.poll())) if (!seen[v]) { seen[v] = true; q.add(v); }
    return seen[d];
}
```

The approach from [Graph E01](../../09-Graph/02-Easy/E01_FindIfPathExists.md): build an adjacency list, BFS from s, check whether d was reached. **O(V + E).**

---

## 5. Approach 2: Union-Find (the file's main solution)

### Code ([E01_FindIfPathExistsUnionFind.java:32-53](E01_FindIfPathExistsUnionFind.java#L32-L53))

```java
static int[] parent, size;

static int find(int x) {
    while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
    return x;
}

static void union(int a, int b) {
    int ra = find(a), rb = find(b);
    if (ra == rb) return;
    if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }
    parent[rb] = ra;
    size[ra] += size[rb];
}

static boolean validPath(int n, int[][] edges, int source, int destination) {
    parent = new int[n];
    size = new int[n];
    for (int i = 0; i < n; i++) { parent[i] = i; size[i] = 1; }
    for (int[] e : edges) union(e[0], e[1]);
    return find(source) == find(destination);
}
```

### Line by line

| Line | What it does |
|---|---|
| `parent[i] = i; size[i] = 1` | n groups of one |
| `union(e[0], e[1])` | each edge merges its endpoints' groups |
| `find(source) == find(destination)` | the same root ⇔ connected |

### Dry run: n = 6, edges [[0,1],[0,2],[3,5],[5,4],[4,3]]

| edge | roots | action | parent after |
|---|---|---|---|
| | | | [0, 1, 2, 3, 4, 5] |
| 0–1 | 0, 1 | sizes equal → 1 under 0 | [0, 0, 2, 3, 4, 5] |
| 0–2 | 0, 2 | 2 under 0 (size 2 > 1) | [0, 0, 0, 3, 4, 5] |
| 3–5 | 3, 5 | 5 under 3 | [0, 0, 0, 3, 4, 3] |
| 5–4 | 3, 4 | 4 under 3 | [0, 0, 0, 3, 3, 3] |
| 4–3 | 3, 3 | already together | |

find(0) = 0, find(5) = 3 → **false** ✓

### Complexity
- **Time O((V + E) · α(V)) ≈ O(V + E).**
- **Space O(V)** (no adjacency list needed).

---

## 6. Comparing the approaches

| | BFS | Union-Find |
|---|---|---|
| Time | O(V + E) | O((V + E) · α) |
| Space | O(V + E) | **O(V)** |
| Many queries / edges arriving over time | rerun the BFS | ~O(1) each |
| Path / distance | yes | no |

The file checks Union-Find against BFS on 1,000 random graphs.

---

## 7. Edge cases and common mistakes

**Edge cases:** source == destination (true), no edges, self-loops, duplicate edges.

**Mistakes:**
1. Linking the elements instead of their roots (`parent[a] = b`).
2. Forgetting to initialise `parent[i] = i`.
3. Comparing `parent[source] == parent[destination]` instead of their **roots**.

---

## 8. Interview questions and answers

**Q1. Why are union by size and path compression both used?**
> Each one alone gives O(log n) per operation; together they give O(α(n)), which is effectively constant.

**Q2. What does Union-Find cost compared with BFS for this problem?**
> Both are linear here. Union-Find wins when the queries or edges keep coming, since each later operation is about O(1) without rebuilding anything.

### Follow-up questions

**F1. Count the components.** → [Graph M11](../../09-Graph/03-Medium/M11_NumberOfConnectedComponents.md)
> Start at n and subtract one on every successful union.

**F2. Detect the edge that creates a cycle.** → [M01 Redundant Connection](../03-Medium/M01_RedundantConnection.md)

**F3. The size of each group.**
> It's `size[find(x)]`.

---

## 9. Running the file

```bash
java E01_FindIfPathExistsUnionFind.java      # 3/3 tests passed (including 1,000 random graphs: Union-Find == BFS)
```

**One-line summary:** *"Union the endpoints of every edge (by size, with path compression); a path exists iff find(source) == find(destination)."*
