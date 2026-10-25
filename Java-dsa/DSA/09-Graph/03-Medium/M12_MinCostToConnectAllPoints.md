# M12 · Min Cost to Connect All Points

| | |
|---|---|
| **LeetCode** | [#1584 Min Cost to Connect All Points](https://leetcode.com/problems/min-cost-to-connect-all-points/) |
| **Difficulty** | Medium |
| **Pattern** | A minimum spanning tree on a complete graph → O(n²) Prim (no heap) |
| **Code** | [M12_MinCostToConnectAllPoints.java](M12_MinCostToConnectAllPoints.java) |
| **Run** | `java M12_MinCostToConnectAllPoints.java` |

Graph basics are explained in [E01](../02-Easy/E01_FindIfPathExists.md#3-java-concepts-used-in-this-problem). MSTs are built from scratch in [01-Basics/MinimumSpanningTree](../01-Basics/MinimumSpanningTree.java); Union-Find in [M11](M11_NumberOfConnectedComponents.md).

---

## 1. The problem

Points on a plane; connecting two costs their **Manhattan distance** `|x₁ − x₂| + |y₁ − y₂|`. What's the minimum total cost so that every pair is connected (directly or through others)?

| points | answer |
|---|---|
| `[[0,0],[2,2],[3,10],[5,2],[7,0]]` | 20 |
| `[[3,12],[-2,5],[-4,1]]` | 18 |
| `[[0,0]]` | 0 |

**Constraints:** up to 1000 points.

---

## 2. How to think about it

### This is a minimum spanning tree
"Connect everything as cheaply as possible" never needs a cycle (removing an edge from a cycle keeps everything connected and lowers the cost). So the answer is a **spanning tree** of minimum total weight: an **MST**.

### Which MST algorithm?
Every pair of points can be connected: E = n(n − 1)/2 ≈ 500,000 edges for n = 1000.

| algorithm | time | memory |
|---|---|---|
| Kruskal (sort all edges + Union-Find) | O(n² log n) | O(n²) |
| Prim with a heap | O(n² log n) | O(n²) heap entries |
| **Prim with an array** | **O(n²)** | **O(n)** ← best for dense graphs |

### Prim with an array
- `minDist[v]` = the cheapest edge from the current tree to v
- repeat n times: pick the unvisited v with the smallest `minDist` (an O(n) scan), add it to the tree, then update `minDist` of all the others **through v** (O(n))

### Dry run (from the real run)

| step | added | cost | total | minDist after (to points 0..4) |
|---|---|---|---|---|
| 1 | (0, 0) | 0 | 0 | [0, 4, 13, 7, 7] |
| 2 | (2, 2) | 4 | 4 | [0, 4, 9, 3, 7] |
| 3 | (5, 2) | 3 | 7 | [0, 4, 9, 3, 4] |
| 4 | (7, 0) | 4 | 11 | |
| 5 | (3, 10) | 9 | **20** | |

---

## 3. Java concepts used in this problem

### 3.1 Implicit edges

The graph is never stored. The weight of u–v is computed on demand:

```java
int d = Math.abs(points[u][0] - points[v][0]) + Math.abs(points[u][1] - points[v][1]);
```

That's what keeps the memory at O(n).

### 3.2 The "pick the minimum" scan

```java
int u = -1;
for (int v = 0; v < n; v++) if (!inTree[v] && (u == -1 || minDist[v] < minDist[u])) u = v;
```

A linear scan replaces the heap. For dense graphs that's actually cheaper: n scans of O(n) = O(n²), with no log factor.

### 3.3 Kruskal's edge list (the referee)

```java
edges.add(new int[]{weight, i, j});
edges.sort(Comparator.comparingInt(e -> e[0]));
```

Every pair becomes an edge `{weight, i, j}`; sorting by weight, then union-finding, gives Kruskal's MST.

---

## 4. Approach 1: Kruskal (the referee)

### Code ([M12_MinCostToConnectAllPoints.java:60-76](M12_MinCostToConnectAllPoints.java#L60-L76))

```java
static int kruskal(int[][] p) {
    int n = p.length;
    List<int[]> edges = new ArrayList<>();
    for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++)
        edges.add(new int[]{Math.abs(p[i][0] - p[j][0]) + Math.abs(p[i][1] - p[j][1]), i, j});
    edges.sort(Comparator.comparingInt(e -> e[0]));
    int[] parent = new int[n];
    for (int i = 0; i < n; i++) parent[i] = i;
    int total = 0;
    for (int[] e : edges) {
        int a = find(parent, e[1]), b = find(parent, e[2]);
        if (a != b) { parent[a] = b; total += e[0]; }
    }
    return total;
}
```

| Line | What it does |
|---|---|
| every pair → an edge | the complete graph, n(n − 1)/2 edges |
| sort by weight | consider the cheapest edges first |
| `a != b` | the edge joins two different components → take it |
| (`a == b`) | it would create a cycle → skip it |

**Why it works (the cut property):** the cheapest edge crossing any split of the vertices into two groups belongs to some MST. **O(n² log n)** time, **O(n²)** memory.

---

## 5. Approach 2: Prim with an array (the file's main solution)

### Code ([M12_MinCostToConnectAllPoints.java:38-57](M12_MinCostToConnectAllPoints.java#L38-L57))

```java
static int minCostConnectPoints(int[][] points) {
    int n = points.length;
    int[] minDist = new int[n];
    Arrays.fill(minDist, Integer.MAX_VALUE);
    minDist[0] = 0;
    boolean[] inTree = new boolean[n];
    int total = 0;
    for (int step = 0; step < n; step++) {
        int u = -1;
        for (int v = 0; v < n; v++) if (!inTree[v] && (u == -1 || minDist[v] < minDist[u])) u = v;
        inTree[u] = true;
        total += minDist[u];
        for (int v = 0; v < n; v++) {
            if (inTree[v]) continue;
            int d = Math.abs(points[u][0] - points[v][0]) + Math.abs(points[u][1] - points[v][1]);
            if (d < minDist[v]) minDist[v] = d;                // a cheaper way into the tree, via u
        }
    }
    return total;
}
```

### Line by line

| Line | What it does |
|---|---|
| `minDist[0] = 0` | start the tree at point 0 (any point works) |
| the selection scan | the point outside the tree that's cheapest to connect |
| `total += minDist[u]` | pay for the edge that brings u in |
| the update loop | u is now in the tree, so a point v may now connect more cheaply via u |

### Complexity
- **Time O(n²).**
- **Space O(n).**

---

## 6. Comparing the approaches

| | Kruskal | Prim (array) |
|---|---|---|
| Time | O(n² log n) | **O(n²)** |
| Memory | O(n²) edges | **O(n)** |
| Best for | sparse graphs | dense / complete graphs |

The file checks Prim against Kruskal on 300 random point sets.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single point (0), duplicate points (distance 0), collinear points.

**Mistakes:**
1. Euclidean instead of Manhattan distance.
2. Materialising all n² edges with a heap-based Prim (memory-heavy).
3. Adding the edge cost for the starting point (it's 0 by construction).

---

## 8. Interview questions and answers

**Q1. Why is the result a tree?**
> Any cycle contains an edge whose removal keeps everything connected, and removing it lowers the cost, so an optimal network has no cycle.

**Q2. Why is the array-based Prim better here?**
> In a complete graph E ≈ V², so heap-based algorithms pay an extra log factor and store O(V²) entries. The array scan picks the minimum in O(V), giving O(V²) overall.

### Follow-up questions

**F1. Some edges are mandatory, or already built (LC 1135, LC 1168).**
> Kruskal: union the forced edges first, then continue with the sorted remaining ones.

**F2. Find the critical and pseudo-critical MST edges (LC 1489).**
> For each edge, compute the MST without it (critical if the cost rises) and the MST forcing it (pseudo-critical if the cost stays the same).

**F3. Sparse graphs.**
> Kruskal with Union-Find, or Prim with a heap: O(E log V).

---

## 9. Running the file

```bash
java M12_MinCostToConnectAllPoints.java      # 4/4 tests passed (including 300 random point sets: Prim == Kruskal)
```

**One-line summary:** *"It's an MST on a complete graph: array-based Prim adds the cheapest outside point n times and relaxes everyone's distance through it, in O(n²) time and O(n) memory."*
