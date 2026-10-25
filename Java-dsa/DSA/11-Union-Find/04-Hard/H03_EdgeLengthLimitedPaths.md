# H03 · Checking Existence of Edge Length Limited Paths ⭐

| | |
|---|---|
| **LeetCode** | [#1697 Checking Existence of Edge Length Limited Paths](https://leetcode.com/problems/checking-existence-of-edge-length-limited-paths/) |
| **Difficulty** | Hard |
| **Pattern** | **Offline queries:** sort the edges and the queries by weight so the Union-Find only ever grows |
| **Code** | [H03_EdgeLengthLimitedPaths.java](H03_EdgeLengthLimitedPaths.java) |
| **Run** | `java H03_EdgeLengthLimitedPaths.java` |

⭐ = not in `leetcode.md`, added for a key technique. Union-Find basics are explained in [E01](../02-Easy/E01_FindIfPathExistsUnionFind.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

An undirected weighted graph on n nodes (parallel edges are allowed). Each query `[p, q, limit]` asks: is there a path from p to q that uses **only** edges with weight **strictly less than** `limit`?

```
n = 3
edges   = [[0,1,2], [1,2,4], [2,0,8], [1,0,16]]
queries = [[0,1,2], [0,2,5]]          →  [false, true]

query [0,1,2]: the only usable edges have weight < 2 → none → false
query [0,2,5]: edges 2 (0–1) and 4 (1–2) are usable → 0 → 1 → 2 → true
```

| n | edges | queries | answer |
|---|---|---|---|
| 3 | [[0,1,2],[1,2,4],[2,0,8],[1,0,16]] | [[0,1,2],[0,2,5]] | [false, true] |
| 5 | [[0,1,10],[1,2,5],[2,3,9],[3,4,13]] | [[0,4,14],[1,4,13]] | [true, false] |

---

## 2. How to think about it

### Per-query BFS is too slow
For each query, ignore the edges with weight ≥ limit and BFS from p. That's O(Q · (V + E)), and with 10⁵ of each it's 10¹⁰.

### The offline trick
Notice that a query with a **bigger** limit can use every edge a smaller-limit query can, plus more. So if we answer the queries in **increasing order of limit**, the set of usable edges only grows, and growing is exactly what Union-Find is good at:

```
sort the edges by weight
sort the query indices by limit
for each query, smallest limit first:
    union every not-yet-added edge with weight < limit      (a pointer moves forward)
    answer = find(p) == find(q)      → store it at the query's ORIGINAL index
```

Every edge is unioned once and every query is one find. We never need to **remove** an edge, which Union-Find can't do anyway.

"Offline" means we're allowed to see all the queries first and answer them in any order we like, as long as each answer goes back to its original position. The same idea appears in [Trie H05](../../10-Trie/04-Hard/H05_MaximumXorWithAnElement.md): sort the queries so that the data structure only ever grows.

### Dry run: example 2

Sorted edges: (1–2, 5), (2–3, 9), (0–1, 10), (3–4, 13). Queries sorted by limit: q1 (limit 13), then q0 (limit 14).

| query | limit | edges added now | groups | p ~ q? | ans |
|---|---|---|---|---|---|
| q1 = [1,4,13] | 13 | 5, 9, 10 | {0, 1, 2, 3}, {4} | 1 vs 4: no | ans[1] = false |
| q0 = [0,4,14] | 14 | 13 | {0, 1, 2, 3, 4} | 0 vs 4: yes | ans[0] = true |

Result in the original order: **[true, false]** ✓. The edge of weight 13 is **not** usable for limit 13 because the comparison is strict.

---

## 3. Java concepts used in this problem

### 3.1 Sorting an array of arrays by one column

```java
int[][] edges = edgeList.clone();
Arrays.sort(edges, Comparator.comparingInt(e -> e[2]));
```

- `clone()` makes a new outer array so that sorting doesn't reorder the caller's input. It's a **shallow** copy: the inner `int[]` rows are shared, which is fine because we never modify them.
- `Comparator.comparingInt(e -> e[2])` builds a comparator from a lambda that extracts the weight.

### 3.2 Sorting indices instead of the queries themselves

```java
Integer[] order = new Integer[queries.length];
for (int i = 0; i < order.length; i++) order[i] = i;
Arrays.sort(order, Comparator.comparingInt(i -> queries[i][2]));
```

We need to remember where each query came from, so we sort the **indices** by their query's limit. `order` must be `Integer[]`, not `int[]`: `Arrays.sort` with a comparator only works on arrays of objects.

### 3.3 The two-pointer sweep

```java
int next = 0;                         // the first edge not yet added
while (next < edges.length && edges[next][2] < q[2]) { ...; next++; }
```

`next` never moves backwards, so across all the queries the `while` loop runs at most E times in total.

### 3.4 Writing answers back in the original order

`ans[qi] = ...` uses the original index `qi`, not the position in the sorted order.

---

## 4. Approach 1: DFS per query (the referee)

### Code ([H03_EdgeLengthLimitedPaths.java:65-83](H03_EdgeLengthLimitedPaths.java#L65-L83))

```java
static boolean[] brute(int n, int[][] edges, int[][] queries) {
    boolean[] out = new boolean[queries.length];
    for (int j = 0; j < queries.length; j++) {
        int[] q = queries[j];
        boolean[] seen = new boolean[n];
        Deque<Integer> st = new ArrayDeque<>(List.of(q[0]));
        seen[q[0]] = true;
        while (!st.isEmpty()) {
            int u = st.pop();
            for (int[] e : edges) {
                if (e[2] >= q[2]) continue;
                int v = e[0] == u ? e[1] : e[1] == u ? e[0] : -1;
                if (v >= 0 && !seen[v]) { seen[v] = true; st.push(v); }
            }
        }
        out[j] = seen[q[1]];
    }
    return out;
}
```

| Line | What it does |
|---|---|
| a fresh `seen` per query | each query is a separate search |
| `e[2] >= q[2]` → skip | only edges strictly lighter than the limit |
| the nested ternary | the other endpoint of `e` if it touches `u`, else −1 |
| `seen[q[1]]` | whether q was reached |

**O(Q · V · E)** as written (it scans every edge for every popped node), which is fine for a referee.

---

## 5. Approach 2: Offline sort + Union-Find (the file's main solution)

### Code ([H03_EdgeLengthLimitedPaths.java:37-63](H03_EdgeLengthLimitedPaths.java#L37-L63))

```java
static int[] parent;

static int find(int x) {
    while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
    return x;
}

static boolean[] distanceLimitedPathsExist(int n, int[][] edgeList, int[][] queries) {
    parent = new int[n];
    for (int i = 0; i < n; i++) parent[i] = i;
    int[][] edges = edgeList.clone();
    Arrays.sort(edges, Comparator.comparingInt(e -> e[2]));
    Integer[] order = new Integer[queries.length];
    for (int i = 0; i < order.length; i++) order[i] = i;
    Arrays.sort(order, Comparator.comparingInt(i -> queries[i][2]));
    boolean[] ans = new boolean[queries.length];
    int next = 0;
    for (int qi : order) {
        int[] q = queries[qi];
        while (next < edges.length && edges[next][2] < q[2]) {     // add every edge allowed so far
            parent[find(edges[next][0])] = find(edges[next][1]);
            next++;
        }
        ans[qi] = find(q[0]) == find(q[1]);                        // write at the ORIGINAL position
    }
    return ans;
}
```

### Line by line

| Line | What it does |
|---|---|
| the parent reset | every node alone |
| sort `edges` | lightest first |
| sort `order` | the queries' indices, smallest limit first |
| `for (int qi : order)` | process the queries in limit order (auto-unboxing `Integer` → `int`) |
| the `while` | add every edge lighter than this limit that hasn't been added yet |
| `parent[find(a)] = find(b)` | union (a self-loop or a redundant edge just links a root to itself) |
| `ans[qi] = ...` | connected means a valid path exists |

### Dry run: example 1

Sorted edges: 2 (0–1), 4 (1–2), 8 (2–0), 16 (1–0). Queries by limit: q0 (2), q1 (5).

| query | limit | edges added | p ~ q? |
|---|---|---|---|
| q0 = [0,1,2] | 2 | none (2 is not < 2) | 0 vs 1: no → false |
| q1 = [0,2,5] | 5 | 2, 4 | 0 vs 2: yes → true |

### Complexity
- **Time O(E log E + Q log Q + (E + Q) · α).** The sorting dominates.
- **Space O(V + Q).** Plus the cloned edge array.

---

## 6. Comparing the approaches

| | Search per query | Offline Union-Find |
|---|---|---|
| Time | O(Q · (V + E)) or worse | **O((E + Q) log)** |
| Works online (one query at a time, in order)? | yes | no: needs all the queries up front |

The file checks the offline version against the per-query search on 500 random graphs (with self-loops and parallel edges).

---

## 7. Edge cases and common mistakes

**Edge cases:** p == q (always true: the empty path), self-loops, parallel edges with different weights, a limit smaller than every edge, no edges at all.

**Mistakes:**
1. Using `<=` instead of `<` (the limit is strict).
2. Writing the answer at the sorted position instead of `qi`.
3. Resetting `next` for every query (that makes it O(Q · E)).
4. Sorting `int[]` indices with a comparator (it doesn't compile; use `Integer[]`).

---

## 8. Interview questions and answers

**Q1. Why does sorting the queries make Union-Find usable here?**
> Union-Find can add edges but not remove them. In increasing limit order, each query's allowed edges are a superset of the previous query's, so we only ever add.

**Q2. What does "offline" mean and when is it allowed?**
> All the queries are known before we answer any of them, so we can reorder them. It's not allowed when each query depends on the previous answer or arrives in real time.

**Q3. Why is a strict `<` correct with this pointer?**
> Edges are added only while `weight < limit`. Equal-weight edges stay unadded until a query with a larger limit comes along.

### Follow-up questions

**F1. Online queries (LC 1724, Checking Existence of Edge Length Limited Paths II).**
> Build the minimum spanning forest. The path between p and q in it minimises the largest edge, so the answer is "is the maximum edge on the MST path < limit?", answered with binary lifting over the tree.

**F2. Minimise the maximum edge on a p–q path.**
> The same MST property: add the edges by weight until p and q connect; the last edge added is the answer.

**F3. Kruskal's algorithm.** → [Graph M12](../../09-Graph/03-Medium/M12_MinCostToConnectAllPoints.md)
> The same "sort the edges, union in order" loop.

---

## 9. Running the file

```bash
java H03_EdgeLengthLimitedPaths.java      # 3/3 tests passed (including 500 random graphs vs BFS per query)
```

**One-line summary:** *"Sort the edges by weight and the queries by limit; for each query, union every edge lighter than its limit, then check whether p and q share a root, storing the answer at the original index."*
