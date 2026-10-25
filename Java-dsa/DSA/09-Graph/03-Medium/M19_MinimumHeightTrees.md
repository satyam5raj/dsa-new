# M19 · Minimum Height Trees

| | |
|---|---|
| **LeetCode** | [#310 Minimum Height Trees](https://leetcode.com/problems/minimum-height-trees/) |
| **Difficulty** | Medium |
| **Pattern** | Peel the leaves layer by layer (a topological sort on an undirected tree) |
| **Code** | [M19_MinimumHeightTrees.java](M19_MinimumHeightTrees.java) |
| **Run** | `java M19_MinimumHeightTrees.java` |

Kahn's algorithm is introduced in [M03 Course Schedule](M03_CourseSchedule.md).

---

## 1. The problem

A tree with n nodes. Rooting it at node r gives some height. Return **all** the roots that give the **minimum** height.

```
n = 4, [[1,0],[1,2],[1,3]]  →  [1]          n = 6, [[3,0],[3,1],[3,2],[3,4],[5,4]]  →  [3, 4]

    0                                        0   1   2
    |                                         \  |  /
2 ─ 1 ─ 3                                        3 ─ 4 ─ 5
```

**Constraints:** up to 2 · 10⁴ nodes.

---

## 2. How to think about it

### Brute force
BFS from every node and measure the height: O(n²), too slow for n = 2 · 10⁴.

### The insight: the best roots are the tree's centre
The optimal roots are the **middle** of the longest path (the diameter). There are always **1 or 2** centres. To find them, **burn the tree from the outside in**: remove all the current leaves (degree 1) at once, which creates new leaves; repeat. The last 1 or 2 nodes standing are the centres.

It's like Kahn's algorithm, with "degree == 1" instead of "in-degree == 0".

```
while remaining > 2:
    remove every current leaf (remaining −= #leaves)
    for each removed leaf's neighbour: degree−−; if it becomes 1 → a leaf of the next layer
return the leaves left
```

### Dry run: n = 6
```
edges 0-3, 1-3, 2-3, 3-4, 4-5
leaves {0, 1, 2, 5} → remove them → 3's degree 4 → 1, 4's degree 2 → 1
remaining 6 − 4 = 2 → stop → answer [3, 4]
```

### Why the centres?
Rooted at r, the height = the distance from r to the farthest node. The farthest node from any vertex is an end of a diameter, so the height is minimised at the middle of the diameter: ⌈D/2⌉ for a diameter of D edges. Peeling the leaves shortens every longest path by 2 per round, from both ends at once, until only its middle remains.

---

## 3. Java concepts used in this problem

### 3.1 `List.of(0)` for the single-node case

When n = 1 there are no edges and no leaves of degree 1; the answer is just [0]. `List.of` creates an immutable one-element list.

### 3.2 A `Set` per node in the adjacency list

`List<Set<Integer>>` works like a list of lists here. (A `HashSet` would also make removing edges O(1), which some versions of this algorithm do.)

### 3.3 Swapping in the next layer

```java
List<Integer> next = new ArrayList<>();
...
leaves = next;
```

Each round builds a fresh list of the new leaves, then replaces the old list. It's the level-by-level BFS pattern without a queue.

---

## 4. Approach 1: BFS from every root (the referee)

### Code ([M19_MinimumHeightTrees.java:67-83](M19_MinimumHeightTrees.java#L67-L83))

```java
static List<Integer> brute(int n, int[][] edges) {
    List<List<Integer>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }
    int[] h = new int[n];
    for (int r = 0; r < n; r++) {
        int[] d = new int[n];
        Arrays.fill(d, -1);
        d[r] = 0;
        Queue<Integer> q = new ArrayDeque<>(List.of(r));
        while (!q.isEmpty()) { int u = q.poll(); for (int v : adj.get(u)) if (d[v] < 0) { d[v] = d[u] + 1; q.add(v); h[r] = Math.max(h[r], d[v]); } }
    }
    int min = Arrays.stream(h).min().getAsInt();
    List<Integer> out = new ArrayList<>();
    for (int r = 0; r < n; r++) if (h[r] == min) out.add(r);
    return out;
}
```

| Line | What it does |
|---|---|
| a BFS from r | the distance from r to every node |
| `h[r]` | r's height = the largest distance |
| the last loop | every root achieving the minimum height |

**O(n²).**

---

## 5. Approach 2: Peel the leaves (the file's main solution)

### Code ([M19_MinimumHeightTrees.java:40-64](M19_MinimumHeightTrees.java#L40-L64))

```java
static List<Integer> findMinHeightTrees(int n, int[][] edges) {
    if (n == 1) return List.of(0);
    List<Set<Integer>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new HashSet<>());
    for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }
    int[] degree = new int[n];
    List<Integer> leaves = new ArrayList<>();
    for (int i = 0; i < n; i++) {
        degree[i] = adj.get(i).size();
        if (degree[i] == 1) leaves.add(i);
    }
    int remaining = n;
    while (remaining > 2) {
        remaining -= leaves.size();                      // peel the whole outer layer
        List<Integer> next = new ArrayList<>();
        for (int leaf : leaves) {
            for (int nb : adj.get(leaf)) {
                if (--degree[nb] == 1) next.add(nb);     // nb became a leaf of the inner tree
            }
        }
        leaves = next;
    }
    Collections.sort(leaves);
    return leaves;
}
```

### Line by line

| Line | What it does |
|---|---|
| `n == 1` | a single node is its own centre |
| `degree`, the initial `leaves` | every node with exactly one neighbour |
| `while (remaining > 2)` | more than 2 nodes left → there's still an outer layer to peel |
| `remaining -= leaves.size()` | remove the whole layer at once |
| `--degree[nb] == 1` | the neighbour just lost its last edge but one → it's a leaf of the inner tree |
| the sort | a deterministic output order |

(The removed leaf's own neighbour list still contains already-removed nodes, but their degrees are never read again, so it's harmless.)

### Dry run: the star, n = 4
Leaves {0, 2, 3} → remaining = 4 − 3 = 1. While peeling them, `--degree[1]` goes 3 → 2 → 1 → 0; it equals 1 after the second leaf, so node 1 is added to `next` (exactly once). leaves = [1], remaining = 1 → the loop ends → **[1]** ✓

### Complexity
- **Time O(n):** each node is peeled once, and each edge is looked at a constant number of times.
- **Space O(n).**

---

## 6. Comparing the approaches

| | BFS from every root | Peel the leaves |
|---|---|---|
| Time | O(n²) | **O(n)** |
| Space | O(n) | O(n) |

The file checks peeling against BFS-from-every-root on 500 random trees.

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 1 ([0]), n = 2 (both nodes), a path (1 or 2 middle nodes), a star (the centre).

**Mistakes:**
1. Removing the leaves one at a time instead of a whole layer.
2. Looping while `remaining > 1` (with 2 centres, it would remove one of them).
3. Forgetting the n = 1 case (no degree-1 nodes).

---

## 8. Interview questions and answers

**Q1. Why are there at most 2 answers?**
> The minimum height is ⌈D/2⌉ for a diameter of D edges, achieved only at the middle of the diameter: 1 node if D is even, the 2 middle nodes if D is odd.

**Q2. How does this relate to topological sort?**
> It's Kahn's algorithm on an undirected tree: repeatedly remove the "outermost" nodes (degree 1 instead of in-degree 0), layer by layer.

### Follow-up questions

**F1. Find the diameter of a tree.**
> BFS from any node to the farthest node u, then BFS from u: the farthest distance is the diameter (the binary-tree version: [Trees E05](../../07-Trees/02-Easy/E05_DiameterOfBinaryTree.md)).

**F2. The sum of the distances from each node (LC 834).**
> Re-rooting DP: compute the subtree sizes, then move the root across each edge in O(1).

**F3. Is the graph a tree?** → [M10](M10_GraphValidTree.md)

---

## 9. Running the file

```bash
java M19_MinimumHeightTrees.java      # 5/5 tests passed (including 500 random trees vs BFS from every root)
```

**One-line summary:** *"Repeatedly remove all the current leaves (degree 1) until at most 2 nodes remain; those centres are the minimum-height roots."*
