# H04 · Remove Max Number of Edges to Keep Graph Fully Traversable ⭐

| | |
|---|---|
| **LeetCode** | [#1579 Remove Max Number of Edges to Keep Graph Fully Traversable](https://leetcode.com/problems/remove-max-number-of-edges-to-keep-graph-fully-traversable/) |
| **Difficulty** | Hard |
| **Pattern** | **Two** Union-Finds (Alice's view and Bob's view) + greedy: shared edges first |
| **Code** | [H04_RemoveMaxEdgesKeepGraphTraversable.java](H04_RemoveMaxEdgesKeepGraphTraversable.java) |
| **Run** | `java H04_RemoveMaxEdgesKeepGraphTraversable.java` |

⭐ = not in `leetcode.md`, added for a key technique. Union-Find basics are explained in [E01](../02-Easy/E01_FindIfPathExistsUnionFind.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

A graph on nodes 1..n with three edge types:
- type **1**: only Alice can use it;
- type **2**: only Bob can use it;
- type **3**: both can use it.

Remove as many edges as possible so that **both** Alice and Bob can still reach every node. Return the number removed, or −1 if even the full graph doesn't let one of them reach everything.

| n | edges | answer |
|---|---|---|
| 4 | [[3,1,2],[3,2,3],[1,1,3],[1,2,4],[1,1,2],[2,3,4]] | 2 |
| 4 | [[3,1,2],[3,2,3],[1,1,4],[2,1,4]] | 0 |
| 4 | [[3,2,3],[1,1,2],[2,3,4]] | −1 |

---

## 2. How to think about it

### Maximise the removals = minimise the edges kept
Each person needs a spanning tree of the edges they can use (n − 1 edges each). We want the two trees to overlap as much as possible, because an overlapping edge is kept once but counts for both.

### Greedy: type 3 first
A type-3 edge does the job of a type-1 **and** a type-2 edge at once, so it's worth twice as much. Take them first:

```
alice, bob = two separate Union-Finds
pass 1, type 3: if the edge joins two components, union it in BOTH and keep it
pass 2, type 1: union in alice's only; keep it if it joined two components
        type 2: union in bob's only;   keep it if it joined two components
if either Union-Find still has more than one component → −1
answer = total edges − kept edges
```

### Why shared-first is optimal
After pass 1, both Union-Finds have the same components, built with as few type-3 edges as possible. Any spanning solution needs at least that many cross-component connections; making them with type-3 edges costs 1 kept edge each, while making the same connection for both people with type-1 + type-2 edges costs 2. So taking every useful type-3 edge first never hurts.

This is Kruskal's MST idea (see [Graph M12](../../09-Graph/03-Medium/M12_MinCostToConnectAllPoints.md)) with all the weights equal and a priority by type.

### Dry run: example 1

| pass | edge | Alice | Bob | kept? | kept total |
|---|---|---|---|---|---|
| 1 | 3: 1–2 | joins → 3 comps | joins → 3 comps | ✓ | 1 |
| 1 | 3: 2–3 | joins → 2 comps | joins → 2 comps | ✓ | 2 |
| 2 | 1: 1–3 | already connected | | ✗ | 2 |
| 2 | 1: 2–4 | joins → 1 comp | | ✓ | 3 |
| 2 | 1: 1–2 | already connected | | ✗ | 3 |
| 2 | 2: 3–4 | | joins → 1 comp | ✓ | 4 |

Both have 1 component. Answer: 6 − 4 = **2** ✓

### Dry run: example 3
Pass 1 keeps 3: 2–3. Pass 2: Alice gets 1–2, so Alice has {1, 2, 3} and {4}: 2 components → **−1**.

---

## 3. Java concepts used in this problem

### 3.1 A static nested class for a reusable Union-Find

```java
static class DSU {
    final int[] p;
    int components;
    DSU(int n) { p = new int[n + 1]; for (int i = 0; i <= n; i++) p[i] = i; components = n; }
    ...
}
```

- We need **two independent** Union-Finds, so a class beats a single static array.
- `static class` inside the outer class means it doesn't need an instance of the outer class to be created.
- `DSU(int n) { ... }` is a **constructor**: it runs on `new DSU(n)` and sets up the fields.
- `final int[] p` means the field always refers to the same array; the array's contents can still change.
- Nodes are 1..n, so the array has size n + 1; `components = n` counts only the real nodes.

### 3.2 A `union` that reports success

```java
boolean union(int a, int b) {
    int ra = find(a), rb = find(b);
    if (ra == rb) return false;
    p[ra] = rb;
    components--;
    return true;
}
```

Returning `true` only for a real merge lets the caller write `if (alice.union(x, y)) kept++;`.

### 3.3 Short-circuit `&&` controlling a side effect

```java
if (e[0] == 3 && alice.union(e[1], e[2])) { ... }
```

`alice.union` only runs when the edge is type 3. With `&&`, the right side isn't evaluated when the left side is false, so type-1 and type-2 edges never touch Alice's Union-Find in pass 1.

### 3.4 Bitmasks to enumerate subsets (the referee)

```java
for (int mask = 0; mask < (1 << m); mask++)
    if ((mask >> i & 1) == 0) continue;      // edge i isn't in this subset
```

Each of the 2ᵐ masks is one subset of edges; bit i says whether edge i is kept. `Integer.bitCount(mask)` counts the kept edges.

---

## 4. Approach 1: Try every subset of edges to keep (the referee)

### Code ([H04_RemoveMaxEdgesKeepGraphTraversable.java:73-87](H04_RemoveMaxEdgesKeepGraphTraversable.java#L73-L87))

```java
static int brute(int n, int[][] edges) {
    int m = edges.length, bestKeep = Integer.MAX_VALUE;
    for (int mask = 0; mask < (1 << m); mask++) {
        int keep = Integer.bitCount(mask);
        if (keep >= bestKeep) continue;
        DSU a = new DSU(n), b = new DSU(n);
        for (int i = 0; i < m; i++) {
            if ((mask >> i & 1) == 0) continue;
            if (edges[i][0] != 2) a.union(edges[i][1], edges[i][2]);
            if (edges[i][0] != 1) b.union(edges[i][1], edges[i][2]);
        }
        if (a.components == 1 && b.components == 1) bestKeep = keep;
    }
    return bestKeep == Integer.MAX_VALUE ? -1 : m - bestKeep;
}
```

| Line | What it does |
|---|---|
| `keep >= bestKeep` → skip | a subset can't beat the best one found so far |
| `!= 2` / `!= 1` | Alice can use types 1 and 3; Bob can use types 2 and 3 |
| both have one component | this subset works |
| the return | removed = m − the smallest working keep-set, or −1 |

**O(2ᵐ · m · α).** Only for m ≤ about 20.

---

## 5. Approach 2: Two Union-Finds, type 3 first (the file's main solution)

### Code ([H04_RemoveMaxEdgesKeepGraphTraversable.java:55-70](H04_RemoveMaxEdgesKeepGraphTraversable.java#L55-L70))

```java
static int maxNumEdgesToRemove(int n, int[][] edges) {
    DSU alice = new DSU(n), bob = new DSU(n);
    int kept = 0;
    for (int[] e : edges) {                                         // type 3 first: they help both
        if (e[0] == 3 && alice.union(e[1], e[2])) {
            bob.union(e[1], e[2]);                                  // identical so far: also joins in bob's view
            kept++;
        }
    }
    for (int[] e : edges) {
        if (e[0] == 1 && alice.union(e[1], e[2])) kept++;
        if (e[0] == 2 && bob.union(e[1], e[2])) kept++;
    }
    if (alice.components > 1 || bob.components > 1) return -1;
    return edges.length - kept;
}
```

### Line by line

| Line | What it does |
|---|---|
| two `DSU`s | Alice's and Bob's views of connectivity |
| pass 1 | a type-3 edge that merges in Alice's view also merges in Bob's (they're identical during pass 1) |
| `bob.union(...)` without a check | its result is guaranteed to match Alice's here |
| pass 2 | the single-person edges, each kept only if it merges something for its owner |
| the −1 check | someone still can't reach every node |
| `edges.length − kept` | everything not kept can be removed |

### Complexity
- **Time O(E · α).**
- **Space O(n).**

---

## 6. Comparing the approaches

| | Every subset | Greedy two Union-Finds |
|---|---|---|
| Time | O(2ᵐ · m) | **O(E · α)** |
| Space | O(n) | O(n) |

The file checks the greedy version against every subset on 500 random small graphs (including self-loops and n = 1).

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 1 (nothing needed; every edge is removable), self-loops (never kept), duplicate edges, a graph with only type-3 edges.

**Mistakes:**
1. Processing the edges in input order instead of type 3 first (it keeps extra edges).
2. Using one Union-Find for both people.
3. Checking `components` only for Alice.
4. Forgetting the 1-indexed nodes (`new int[n + 1]` but `components = n`).

---

## 8. Interview questions and answers

**Q1. Why process the type-3 edges first?**
> One type-3 edge can replace a type-1 and a type-2 edge, so it saves an edge whenever it's useful. Using single-person edges first could waste merges that a shared edge would have done for both.

**Q2. Why is `bob.union` in pass 1 guaranteed to succeed when `alice.union` does?**
> During pass 1 both Union-Finds receive exactly the same unions, so they have the same components.

**Q3. Why can't the answer be improved by keeping fewer than this?**
> Each person's kept edges must contain a spanning tree (n − 1 edges). The greedy keeps exactly a spanning forest of type-3 edges plus the minimum extra per person, which is the least possible.

### Follow-up questions

**F1. The edges have weights; minimise the total kept weight.**
> Sort each type by weight and run Kruskal in the same order: type 3 first, then each person's own edges. (Then the exchange argument needs care, since one cheap type-1 and one cheap type-2 edge might cost less than an expensive type-3 edge.)

**F2. Redundant Connection.** → [M01](../03-Medium/M01_RedundantConnection.md)
> The single-person version of "which edges don't merge anything".

**F3. Number of Operations to Make Network Connected.** → [M06](../03-Medium/M06_NumberOfOperationsToMakeNetworkConnected.md)
> Also counts the redundant edges with a counted Union-Find.

---

## 9. Running the file

```bash
java H04_RemoveMaxEdgesKeepGraphTraversable.java      # 4/4 tests passed (including 500 random graphs vs all subsets)
```

**One-line summary:** *"Keep every type-3 edge that merges something (in both Union-Finds), then every type-1/type-2 edge that merges something for its owner; −1 if either person isn't fully connected, otherwise remove everything else."*
