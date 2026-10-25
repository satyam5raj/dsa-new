# M01 · Redundant Connection

| | |
|---|---|
| **LeetCode** | [#684 Redundant Connection](https://leetcode.com/problems/redundant-connection/) |
| **Difficulty** | Medium |
| **Pattern** | Add the edges one by one: the first edge whose ends are **already** connected closes a cycle |
| **Code** | [M01_RedundantConnection.java](M01_RedundantConnection.java) |
| **Run** | `java M01_RedundantConnection.java` |

Union-Find basics are explained in [E01](../02-Easy/E01_FindIfPathExistsUnionFind.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

A tree of n nodes (1..n) plus **one extra edge**. Return an edge whose removal leaves a tree; if several qualify, the one appearing **last** in the input.

```
[[1,2],[1,3],[2,3]]              →  [2,3]

[[1,2],[2,3],[3,4],[1,4],[1,5]]  →  [1,4]

    5 ─ 1 ─ 2
        |   |
        4 ─ 3
```

---

## 2. How to think about it

The extra edge creates **exactly one cycle**; any edge **on** that cycle can be removed. Which one appears last? Process the edges in input order with Union-Find:

```
union(a, b) fails  ⇔  a and b were already connected  ⇔  this edge closes the cycle
```

**Every earlier cycle edge was needed to make a and b connected**, so the edge that closes the cycle **is** the last cycle edge in the input. Return it.

### Dry run: [[1,2],[2,3],[3,4],[1,4],[1,5]]
```
1–2 ok,  2–3 ok,  3–4 ok,  1–4: find(1) == find(4) → [1,4]
```

The directed version (where a node can get two parents) is [Redundant Connection II (H02)](../04-Hard/H02_RedundantConnectionII.md).

---

## 3. Java concepts used in this problem

### 3.1 Union-Find without union by size

```java
int a = find(e[0]), b = find(e[1]);
if (a == b) return e;
parent[a] = b;
```

The file links the roots directly (no size array). With path halving in `find`, that's still fast enough for n ≤ 1000. Union by size would guarantee O(α) in theory.

### 3.2 1-based nodes

`parent = new int[edges.length + 1]`: n nodes with labels 1..n, and n edges (a tree has n − 1, plus the extra one), so the array needs n + 1 slots.

### 3.3 "n nodes, n − 1 edges, no cycle" = a tree (in the referee)

Counting the successful merges: n − 1 merges with no cycle means everything is connected, which is a tree ([Graph M10](../../09-Graph/03-Medium/M10_GraphValidTree.md)).

---

## 4. Approach 1: Try removing each edge, from last to first (the referee)

### Code ([M01_RedundantConnection.java:55-74](M01_RedundantConnection.java#L55-L74))

```java
static int[] brute(int[][] edges) {
    int n = edges.length;
    for (int skip = n - 1; skip >= 0; skip--) {
        int[] p = new int[n + 1];
        for (int i = 0; i <= n; i++) p[i] = i;
        int merges = 0;
        boolean cycle = false;
        for (int i = 0; i < n; i++) {
            if (i == skip) continue;
            int a = root(p, edges[i][0]), b = root(p, edges[i][1]);
            if (a == b) { cycle = true; break; }
            p[a] = b;
            merges++;
        }
        if (!cycle && merges == n - 1) return edges[skip];      // n nodes, n-1 edges, no cycle = a tree
    }
    return new int[0];
}
```

| Line | What it does |
|---|---|
| `skip` from the last edge | the first one that works is the answer by definition |
| the inner loop | rebuild without that edge, checking for a cycle |
| `merges == n − 1` | everything connected |

**O(E · (V + E))**: obviously correct, the file's referee.

---

## 5. Approach 2: The first edge that closes a cycle (the file's main solution)

### Code ([M01_RedundantConnection.java:37-53](M01_RedundantConnection.java#L37-L53))

```java
static int[] parent;

static int find(int x) {
    while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
    return x;
}

static int[] findRedundantConnection(int[][] edges) {
    parent = new int[edges.length + 1];
    for (int i = 0; i < parent.length; i++) parent[i] = i;
    for (int[] e : edges) {
        int a = find(e[0]), b = find(e[1]);
        if (a == b) return e;                      // already connected: this edge closes the cycle
        parent[a] = b;
    }
    return new int[0];
}
```

### Line by line

| Line | What it does |
|---|---|
| the initialisation | every node alone |
| `find(e[0]), find(e[1])` | the endpoints' groups |
| `a == b` | already connected → this edge is redundant |
| `parent[a] = b` | otherwise merge |

### Dry run: [[1,2],[1,3],[2,3]]

| edge | roots | action |
|---|---|---|
| 1–2 | 1, 2 | merge |
| 1–3 | find(1) = 2, 3 | merge |
| 2–3 | find(2) = 3, find(3) = 3 | **the same → return [2, 3]** |

### Complexity
- **Time O(n · α(n)).**
- **Space O(n).**

---

## 6. Comparing the approaches

| | Remove each edge | First cycle-closing edge |
|---|---|---|
| Time | O(E · (V + E)) | **O(n · α)** |
| Space | O(V) | O(V) |

The file checks the Union-Find version against the brute force on 1,000 random trees with one extra edge (shuffled).

---

## 7. Edge cases and common mistakes

**Edge cases:** the extra edge first in the input (the cycle closes later, at a different edge), a triangle, a long cycle.

**Mistakes:**
1. Returning the first edge **on** the cycle rather than the one that **closes** it.
2. Comparing the nodes instead of their roots.
3. 0-based arrays for 1-based nodes.

---

## 8. Interview questions and answers

**Q1. Why is the cycle-closing edge the last cycle edge in the input?**
> Before it, the other cycle edges had connected its two endpoints; all of them appear earlier. So it's the last one of the cycle in input order.

**Q2. What changes for a directed graph?**
> A node may have two parents, or there may be a directed cycle: three cases ([H02](../04-Hard/H02_RedundantConnectionII.md)).

### Follow-up questions

**F1. Report all the edges on the cycle.**
> After finding the closing edge (a, b), BFS/DFS from a to b in the tree formed by the other edges.

**F2. Many extra edges: how many are redundant?**
> Count the failed unions (E − (V − components)).

**F3. Graph Valid Tree.** → [Graph M10](../../09-Graph/03-Medium/M10_GraphValidTree.md)

---

## 9. Running the file

```bash
java M01_RedundantConnection.java      # 3/3 tests passed (including 1,000 random trees + 1 edge vs brute force)
```

**One-line summary:** *"Union the edges in input order; the first edge whose endpoints already share a root closes the cycle, and it's the last cycle edge, so return it."*
