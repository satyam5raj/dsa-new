# A14 · Find Critical and Pseudo-Critical Edges in Minimum Spanning Tree ⭐

| | |
|---|---|
| **LeetCode** | [#1489 Find Critical and Pseudo-Critical Edges in MST](https://leetcode.com/problems/find-critical-and-pseudo-critical-edges-in-minimum-spanning-tree/) |
| **Difficulty** | Hard |
| **Mix of structures** | **Sorting** edges + **Union-Find** (Kruskal), rerun with one edge **skipped** or **forced** |
| **Code** | [A14_CriticalAndPseudoCriticalEdges.java](A14_CriticalAndPseudoCriticalEdges.java) |
| **Run** | `java A14_CriticalAndPseudoCriticalEdges.java` |

⭐ = not in `leetcode.md`, added because it combines sorting, union-find and "what if" reasoning about MSTs.

---

## 1. The problem

A connected, weighted, undirected graph with n nodes; `edges[i] = [a, b, weight]`. A **minimum spanning tree (MST)** connects all nodes with n − 1 edges of the smallest total weight. There can be several MSTs with the same weight.

- **Critical** edge: in **every** MST. Removing it makes the best tree heavier, or disconnects the graph.
- **Pseudo-critical** edge: in **some** MST, but not all.

Return `[critical indices, pseudo-critical indices]`.

```
n = 5, edges = [[0,1,1],[1,2,1],[2,3,2],[0,3,2],[0,4,3],[3,4,3],[1,4,6]]
→ [[0,1], [2,3,4,5]]

n = 4, edges = [[0,1,1],[1,2,1],[2,3,1],[0,3,1]]      (a square, all weight 1)
→ [[], [0,1,2,3]]          any 3 of the 4 edges form an MST
```

---

## 2. How to think about it

### Kruskal, in one paragraph
Sort the edges by weight. Go through them, and add an edge if it connects two **different** components. **Union-find** answers "same component?" and merges components in nearly O(1). Stop when everything is connected. The total is the MST weight. (Full explanation: [Graph M12](../../09-Graph/03-Medium/M12_MinCostToConnectAllPoints.md) and [Union-Find E01](../../11-Union-Find/02-Easy/E01_FindIfPathExistsUnionFind.md).)

### Two "what if" experiments per edge
Let `base` = the MST weight. For each edge e:

| Experiment | Result | Meaning |
|---|---|---|
| **Skip e** and rebuild | heavier than base, or not connected | every MST needs e → **critical** |
| otherwise, **force e** (add it first) and rebuild | exactly base | some MST contains e → **pseudo-critical** |
| | heavier than base | e is in no MST |

**Why forcing works:** Kruskal that starts with e already chosen builds the cheapest spanning tree that **contains** e. If that equals `base`, it's an MST with e in it.

**Why the order of the two checks matters:** a critical edge would also pass the "force" test, so check "critical" first and only test "pseudo" for the rest.

### Dry run: example 1 (`base` = 1 + 1 + 2 + 3 = 7)
Sorted by weight: e0 (0–1, 1), e1 (1–2, 1), e2 (2–3, 2), e3 (0–3, 2), e4 (0–4, 3), e5 (3–4, 3), e6 (1–4, 6).

| edge | skip it → weight | force it → weight | verdict |
|---|---|---|---|
| e0 | e1 + e2 + e3 + e4 = 1+2+2+3 = **8** > 7 | | **critical** |
| e1 | e0 + e2 + e3 + e4 = 8 > 7 | | **critical** |
| e2 | e0 + e1 + e3 + e4 = 7 | e2 first, then e0, e1, e4 = 7 | **pseudo** |
| e3 | 7 (use e2) | 7 | **pseudo** |
| e4 | 7 (use e5) | 7 | **pseudo** |
| e5 | 7 (use e4) | 7 | **pseudo** |
| e6 | 7 | e6 (6) + e0 + e1 + e2 = 10 > 7 | neither |

Answer **[[0, 1], [2, 3, 4, 5]]** ✓

---

## 3. Java concepts used in this problem

### 3.1 Union-find with path halving and union by size
```java
int find(int x) { while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; } return x; }
```
`parent[x] = parent[parent[x]]` makes every other node on the path point to its grandparent ("path halving"), which keeps trees flat without recursion. `union` hangs the smaller tree under the bigger one and decrements `components`, so "is the graph connected?" is just `components == 1`.

### 3.2 Sorting indices once, reusing the order
```java
Integer[] order = new Integer[edges.length];
for (int i = 0; i < edges.length; i++) order[i] = i;
Arrays.sort(order, Comparator.comparingInt(i -> edges[i][2]));   // sort ONCE, reuse for every rebuild
```
We need original indices (they're the answer) and up to 2E rebuilds. Sorting indices once and passing `order` to every rebuild avoids E extra sorts.

### 3.3 `List.of(a, b)` and comparing lists with `equals`
The result is `List.of(critical, pseudo)`. Lists compare element by element with `equals`, which is how the test checks the main solution against the brute force.

### 3.4 Subsets as bitmasks (in the referee)
`for (int mask = 0; mask < (1 << m); mask++)` enumerates every subset of the m edges; `Integer.bitCount(mask) != n - 1` skips subsets with the wrong number of edges. Bit tricks are explained in [A09](A09_ShortestPathVisitingAllNodes.md#3-java-concepts-used-in-this-problem).

---

## 4. The referee: list every spanning tree (tiny graphs)

### Code ([A14_CriticalAndPseudoCriticalEdges.java:58-80](A14_CriticalAndPseudoCriticalEdges.java#L58-L80))
```java
static List<List<Integer>> findBrute(int n, int[][] edges) {
    int m = edges.length, best = Integer.MAX_VALUE;
    List<Integer> mstMasks = new ArrayList<>();
    for (int mask = 0; mask < (1 << m); mask++) {
        if (Integer.bitCount(mask) != n - 1) continue;
        DSU d = new DSU(n);
        int w = 0;
        boolean tree = true;
        for (int i = 0; i < m && tree; i++)
            if ((mask >> i & 1) == 1) { tree = d.union(edges[i][0], edges[i][1]); w += edges[i][2]; }
        if (!tree) continue;                                         // n-1 edges without a cycle = spanning tree
        if (w < best) { best = w; mstMasks.clear(); }
        if (w == best) mstMasks.add(mask);
    }
    List<Integer> critical = new ArrayList<>(), pseudo = new ArrayList<>();
    for (int i = 0; i < m; i++) {
        int in = 0;
        for (int mask : mstMasks) if ((mask >> i & 1) == 1) in++;
        if (in == mstMasks.size()) critical.add(i);
        else if (in > 0) pseudo.add(i);
    }
    return List.of(critical, pseudo);
}
```

| Line | What it does |
|---|---|
| `bitCount != n − 1 → continue` | a spanning tree has exactly n − 1 edges |
| `tree = d.union(...)` | `union` returns false if the edge closes a cycle; n − 1 edges with no cycle form a spanning tree |
| `best` / `mstMasks` | keep every tree of the minimum weight |
| the final loop | in all MSTs → critical; in some → pseudo |

This applies the **definition** directly, which makes it a trustworthy referee. It's exponential in E, so the tests use graphs with at most about 10 edges.

---

## 5. Kruskal with skip / force (main solution)

### Code ([A14_CriticalAndPseudoCriticalEdges.java:83-107](A14_CriticalAndPseudoCriticalEdges.java#L83-L107))
```java
static List<List<Integer>> findCriticalAndPseudoCriticalEdges(int n, int[][] edges) {
    Integer[] order = new Integer[edges.length];
    for (int i = 0; i < edges.length; i++) order[i] = i;
    Arrays.sort(order, Comparator.comparingInt(i -> edges[i][2]));   // sort ONCE, reuse for every rebuild

    int base = mst(n, edges, order, -1, -1);
    List<Integer> critical = new ArrayList<>(), pseudo = new ArrayList<>();
    for (int e = 0; e < edges.length; e++) {
        if (mst(n, edges, order, e, -1) > base) critical.add(e);
        else if (mst(n, edges, order, -1, e) == base) pseudo.add(e);
    }
    return List.of(critical, pseudo);
}

/** MST weight, skipping edge 'skip' and/or taking edge 'force' first; MAX_VALUE if not connected. */
static int mst(int n, int[][] edges, Integer[] order, int skip, int force) {
    DSU d = new DSU(n);
    int weight = 0;
    if (force != -1) { d.union(edges[force][0], edges[force][1]); weight += edges[force][2]; }
    for (int i : order) {
        if (i == skip) continue;
        if (d.union(edges[i][0], edges[i][1])) weight += edges[i][2];
    }
    return d.components == 1 ? weight : Integer.MAX_VALUE;
}
```

### Line by line
| Line | What it does |
|---|---|
| `base = mst(..., -1, -1)` | the normal MST weight (−1 = no skip, no force) |
| `mst(skip = e) > base` | without e the best tree is heavier, **or** the graph is disconnected (`MAX_VALUE`) → critical |
| `mst(force = e) == base` | an MST containing e exists → pseudo-critical |
| `mst`: `force` first | union its endpoints and count its weight before anything else |
| `if (i == skip) continue` | pretend the edge doesn't exist |
| `union(...)` returns true | the edge joined two components, so it's in the tree |
| `components == 1` | the result spans the whole graph |

### Complexity
- One sort: O(E log E).
- 1 + 2E runs of Kruskal, each O(E · α(n)).
- **O(E² · α(n))**, which is fine for LeetCode's E ≤ 200.

---

## 6. Comparing the approaches

| | Time |
|---|---|
| Every spanning tree | exponential (every subset of n − 1 edges) |
| **Kruskal × (1 + 2E)** | **O(E log E + E² α(n))** |

The file checks the main solution against the full enumeration on 800 random small graphs.

---

## 7. Edge cases and common mistakes

**Edge cases:** a bridge (always critical), all edges of equal weight (many MSTs, everything pseudo), parallel edges, an edge heavier than every alternative (in no MST).

**Mistakes:**
1. Testing "pseudo" before "critical" (critical edges also pass the force test).
2. Treating a disconnected result as weight 0 or as just "not heavier"; it must count as heavier.
3. Re-sorting for every rebuild (works, but slower).
4. Sorting the `edges` array itself and losing the original indices.
5. Thinking "is in the MST Kruskal found" means critical: ties make that wrong.

---

## 8. Interview questions and answers

**Q1. How do you decide if an edge is critical?**
> Build the MST without it. If the weight goes up, or the graph becomes disconnected, every MST must contain it.

**Q2. How do you decide pseudo-critical?**
> Among non-critical edges, force the edge into the tree first and finish with Kruskal. If the total equals the MST weight, some MST contains it.

**Q3. Why not just check whether the edge appears in Kruskal's MST?**
> With equal weights there are several MSTs, and Kruskal picks one arbitrarily by tie order. Membership in one MST says nothing about all, or about others.

### Follow-up questions

**F1. "Faster than O(E²)."**
> Process edges in groups of equal weight. Within a group, contract the components built by lighter edges; an edge in the group is critical if it's a **bridge** in that contracted multigraph (Tarjan), and pseudo if it connects different components but isn't a bridge. That's about O(E log E).

**F2. "Is the MST unique?"**
> It's unique exactly when every MST edge is critical, i.e. the pseudo list is empty.

**F3. "Second-best MST."**
> For each non-MST edge (u, v, w), adding it creates a cycle; remove the heaviest MST edge on the u–v path. The best such swap gives the second-best MST.

---

## 9. Running the file

```bash
java A14_CriticalAndPseudoCriticalEdges.java     # 4/4 tests passed (two examples, a bridge, 800 random graphs)
```

**One-line summary:** *"Sort edge indices by weight once and compute the MST weight with Kruskal plus union-find. An edge is critical if skipping it makes the MST heavier or disconnected, and otherwise pseudo-critical if forcing it into the tree first still gives the MST weight."*
