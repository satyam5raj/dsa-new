# H02 · Redundant Connection II ⭐

| | |
|---|---|
| **LeetCode** | [#685 Redundant Connection II](https://leetcode.com/problems/redundant-connection-ii/) |
| **Difficulty** | Hard |
| **Pattern** | The **directed** version of [M01](../03-Medium/M01_RedundantConnection.md): handle "a node with two parents" as well as cycles |
| **Code** | [H02_RedundantConnectionII.java](H02_RedundantConnectionII.java) |
| **Run** | `java H02_RedundantConnectionII.java` |

⭐ = not in `leetcode.md`, added for a key technique. Union-Find basics are explained in [E01](../02-Easy/E01_FindIfPathExistsUnionFind.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

A **rooted** tree (every node has exactly one parent, except the root) on nodes 1..n, plus **one** extra **directed** edge [u, v] meaning u → v. Return the edge to remove so the rest is a rooted tree again. If several edges work, return the one that appears **last**.

| edges | answer | what went wrong |
|---|---|---|
| [[1,2],[1,3],[2,3]] | [2,3] | node 3 has two parents |
| [[1,2],[2,3],[3,4],[4,1],[1,5]] | [4,1] | a cycle; nobody has two parents |
| [[2,1],[3,1],[4,2],[1,4]] | [2,1] | node 1 has two parents **and** there's a cycle |

---

## 2. How to think about it

### What can a rooted tree survive?
A valid rooted tree needs: exactly one node with no parent, every other node with **exactly one** parent, and no cycle. The extra edge u → v breaks it in one of three ways:

| case | what happens | which edge to remove |
|---|---|---|
| **A** | v already had a parent, so v has two parents; no cycle | the **later** of v's two parent edges |
| **B** | v is the root, so nobody has two parents, but there's a cycle | the edge that closes the cycle (the last one on it) |
| **C** | v has two parents **and** one of them is on a cycle | the parent edge that is **on** the cycle |

### The algorithm
```
1. Scan the edges; if a node gets a second parent, record
       cand1 = its earlier parent edge, cand2 = the later one.
2. Run Union-Find over all the edges EXCEPT cand2 (pretend it's removed):
       if an edge closes a cycle:
           no candidates  → case B: return that edge
           candidates     → removing cand2 didn't help, so cand1 is on the cycle: return cand1 (case C)
3. No cycle → removing cand2 fixed everything: return cand2 (case A)
```

### Why can undirected Union-Find detect a directed problem here?
Once the double-parent edge is excluded, every node has at most one parent. With n nodes and n − 1 remaining edges, the only way to fail is a cycle, and with in-degree ≤ 1 any undirected cycle must be a directed one. Union-Find spots it.

### Why return cand2 in case A even though cand1 might also work?
In case A, removing **either** parent edge of v leaves a valid tree whenever no cycle exists, and the problem asks for the **last** such edge, which is cand2.

### Dry runs (from the code)

**Case A:** [[1,2],[1,3],[2,3]]
```
parents: 2←1, 3←1, then [2,3] gives 3 a second parent → cand1 = [1,3], cand2 = [2,3]
UF without [2,3]: [1,2] joins, [1,3] joins → no cycle → return cand2 = [2,3]
```

**Case B:** [[1,2],[2,3],[3,4],[4,1],[1,5]]
```
no node gets two parents → cand1 = cand2 = null
UF: [1,2], [2,3], [3,4] join; [4,1]: find(4) = find(1) → cycle, no candidates → return [4,1]
```

**Case C:** [[2,1],[3,1],[4,2],[1,4]]
```
parents: 1←2, then [3,1] gives 1 a second parent → cand1 = [2,1], cand2 = [3,1]
UF without [3,1]: [2,1] joins, [4,2] joins; [1,4]: find(1) = find(4) → cycle
candidates exist → the cycle 1→4→2→1 survives without cand2, so return cand1 = [2,1]
```

---

## 3. Java concepts used in this problem

### 3.1 Reference equality to skip one specific edge

```java
cand2 = e;                       // the same array object as in edges
...
if (e == cand2) continue;        // == compares references: skips exactly this edge
```

`==` on arrays asks "is this the same object?", not "do they hold the same numbers?". That's exactly right here, because `cand2` is the very array stored in `edges`. (`cand1` is built with `new int[]{...}`, so it's a different object; it's only ever returned, never compared.)

### 3.2 `null` as "not found"

`int[] cand1 = null, cand2 = null;` stays `null` when no node has two parents, and `cand1 == null ? e : cand1` picks the case-B answer.

### 3.3 The ternary operator

```java
return cand1 == null ? e : cand1;
```

`condition ? a : b` is an expression: `a` if the condition is true, else `b`.

### 3.4 Nodes are 1-indexed

`new int[n + 1]` wastes index 0 so node v can be used directly as `treeParent[v]` and `parent[v]`. With n edges there are exactly n nodes (a tree on n nodes has n − 1 edges, plus the extra one).

---

## 4. Approach 1: Try removing each edge, last to first (the referee)

### Code ([H02_RedundantConnectionII.java:72-91](H02_RedundantConnectionII.java#L72-L91))

```java
static int[] brute(int[][] edges) {
    int n = edges.length;
    for (int skip = n - 1; skip >= 0; skip--) {
        int[] indeg = new int[n + 1];
        List<List<Integer>> ch = new ArrayList<>();
        for (int i = 0; i <= n; i++) ch.add(new ArrayList<>());
        for (int i = 0; i < n; i++) if (i != skip) { indeg[edges[i][1]]++; ch.get(edges[i][0]).add(edges[i][1]); }
        int root = -1, roots = 0;
        boolean bad = false;
        for (int v = 1; v <= n; v++) { if (indeg[v] == 0) { root = v; roots++; } else if (indeg[v] > 1) bad = true; }
        if (bad || roots != 1) continue;
        boolean[] seen = new boolean[n + 1];
        Deque<Integer> st = new ArrayDeque<>(List.of(root));
        seen[root] = true;
        int reached = 1;
        while (!st.isEmpty()) for (int c : ch.get(st.pop())) if (!seen[c]) { seen[c] = true; reached++; st.push(c); }
        if (reached == n) return edges[skip];
    }
    return null;
}
```

| Line | What it does |
|---|---|
| `skip` from the end | the first edge that works is the **last** valid one |
| `indeg`, `ch` | in-degrees and children of the graph without that edge |
| the root check | exactly one node with in-degree 0, nobody with in-degree > 1 |
| the DFS from the root | every node must be reachable (this rules out a separate cycle) |
| `return edges[skip]` | it's a rooted tree |

It checks the definition of a rooted tree directly. **O(n²).**

---

## 5. Approach 2: Candidates + Union-Find (the file's main solution)

### Code ([H02_RedundantConnectionII.java:41-69](H02_RedundantConnectionII.java#L41-L69))

```java
static int[] parent;

static int find(int x) {
    while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
    return x;
}

static int[] findRedundantDirectedConnection(int[][] edges) {
    int n = edges.length;
    int[] treeParent = new int[n + 1];
    int[] cand1 = null, cand2 = null;
    for (int[] e : edges) {
        if (treeParent[e[1]] != 0) {                       // e[1] already has a parent
            cand1 = new int[]{treeParent[e[1]], e[1]};
            cand2 = e;
        } else {
            treeParent[e[1]] = e[0];
        }
    }
    parent = new int[n + 1];
    for (int i = 0; i <= n; i++) parent[i] = i;
    for (int[] e : edges) {
        if (e == cand2) continue;                          // try the tree WITHOUT the later parent edge
        int a = find(e[0]), b = find(e[1]);
        if (a == b) return cand1 == null ? e : cand1;      // a cycle remains
        parent[a] = b;
    }
    return cand2;                                          // removing cand2 fixed everything
}
```

### Line by line

| Line | What it does |
|---|---|
| `treeParent` | the first parent seen for each node (0 = none yet) |
| the second-parent branch | record the earlier edge as `cand1` and the current one as `cand2` |
| the UF reset | fresh sets for nodes 1..n |
| `if (e == cand2) continue` | pretend the later parent edge is gone |
| `a == b` | this edge closes a cycle |
| `cand1 == null ? e : cand1` | case B → this edge; case C → the earlier parent edge |
| `return cand2` | case A: no cycle once cand2 is gone |

### Complexity
- **Time O(n · α).**
- **Space O(n).**

---

## 6. Comparing the approaches

| | Remove each edge + check | Candidates + Union-Find |
|---|---|---|
| Time | O(n²) | **O(n · α)** |
| Idea | the definition | the three-case analysis |

The file checks the fast version against the brute force on 2,000 random rooted trees with one extra edge.

---

## 7. Edge cases and common mistakes

**Edge cases:** the extra edge points into the root (case B), the extra edge points to the root's own child, a two-node cycle like [[1,2],[2,1]].

**Mistakes:**
1. Reusing [M01](../03-Medium/M01_RedundantConnection.md) unchanged. It returns the edge that closes an undirected cycle, which can leave a node with two parents. On the case C example it returns [1,4], and removing that still leaves node 1 with parents 2 and 3.
2. In case C, returning cand2 (the later edge) even though removing it leaves the cycle.
3. Skipping cand2 with `Arrays.equals` when the input could contain the same pair twice: reference equality is precise.

---

## 8. Interview questions and answers

**Q1. Why exclude cand2 and not cand1?**
> If removing cand2 works, it's the later of the valid answers, which the problem prefers. If it doesn't work, a cycle survives, and the only other edge that can fix both problems at once is cand1, which must lie on that cycle.

**Q2. In case C, why must cand1 be on the cycle?**
> After removing cand2 there's still a cycle. Removing any edge other than cand1 or cand2 would leave v with two parents, so the answer must be one of the two; cand2 didn't break the cycle, so cand1 must.

**Q3. How does this differ from the undirected Redundant Connection?**
> Undirected trees only fail by having a cycle. Rooted directed trees can also fail by giving a node two parents, which needs the candidate analysis.

### Follow-up questions

**F1. Redundant Connection (undirected).** → [M01](../03-Medium/M01_RedundantConnection.md)
> Return the first edge whose endpoints are already connected.

**F2. Validate a binary tree from parent/child lists (LC 1361).**
> Check every node has at most one parent, exactly one root exists, and everything is reachable from it (or that Union-Find never sees a cycle and ends with one component).

**F3. More than one extra edge?**
> No longer a single-removal problem: you'd look for a minimum set of edges to delete (related to finding a spanning arborescence).

---

## 9. Running the file

```bash
java H02_RedundantConnectionII.java      # 4/4 tests passed (including 2,000 random rooted trees + 1 edge vs brute)
```

**One-line summary:** *"Find the node with two parents (cand1 earlier, cand2 later); union every edge except cand2: a cycle means return cand1 (or the cycle edge if there were no candidates), and no cycle means return cand2."*
