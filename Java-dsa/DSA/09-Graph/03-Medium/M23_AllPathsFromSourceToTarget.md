# M23 · All Paths From Source to Target

| | |
|---|---|
| **LeetCode** | [#797 All Paths From Source to Target](https://leetcode.com/problems/all-paths-from-source-to-target/) |
| **Difficulty** | Medium |
| **Pattern** | DFS + backtracking on a DAG (no visited set needed: there are no cycles) |
| **Code** | [M23_AllPathsFromSourceToTarget.java](M23_AllPathsFromSourceToTarget.java) |
| **Run** | `java M23_AllPathsFromSourceToTarget.java` |

The add / recurse / remove backtracking shape is explained in [Trees M06 Path Sum II](../../07-Trees/03-Medium/M06_PathSumII.md).

---

## 1. The problem

A DAG with nodes 0..n−1 (`graph[u]` = u's out-neighbours). Return **every** path from 0 to n − 1.

```
[[1,2],[3],[3],[]]  →  [[0,1,3],[0,2,3]]

0 → 1 → 3
 ↘ 2 ↗
```

| graph | answer |
|---|---|
| `[[1,2],[3],[3],[]]` | `[[0,1,3],[0,2,3]]` |
| `[[4,3,1],[3,2,4],[3],[4],[]]` | `[[0,4],[0,3,4],[0,1,3,4],[0,1,2,3,4],[0,1,4]]` |

---

## 2. How to think about it

### "All paths" means enumerate → backtracking
```
dfs(u, path):
    if u == target: record a COPY of path
    for each v in graph[u]:  path.add(v);  dfs(v);  path.remove(last)
```

### Why no visited set?
It's a **DAG**: no path can return to a node, so the search always terminates. And a node **must** be visitable again by a **different** path: a global visited set would wrongly cut off the second path through node 3. (On a graph with cycles, you'd use an "on the current path" set instead.)

### The output size
The number of paths can be exponential (2^(n−2) in the worst case, when every node points to every later node), so no algorithm can be polynomial here.

---

## 3. Java concepts used in this problem

### 3.1 Copying the path on success

```java
result.add(new ArrayList<>(path));               // copy!
```

The shared `path` keeps changing; the copy freezes it (the #1 backtracking bug is forgetting this).

### 3.2 `new ArrayList<>(List.of(0))`

A **mutable** list starting with the source. `List.of(0)` alone is immutable, and calling `add` on it would throw.

### 3.3 `Long[]` memo with null = "not computed"

```java
static long countPaths(int[][] g, int u, Long[] memo)
```

The test counts the paths with a memoised DP. A boxed `Long[]` uses `null` to mean "not computed yet", so 0 can be a real count.

---

## 4. Approach 1: Count the paths with a DP (the test's cross-check)

### Code ([M23_AllPathsFromSourceToTarget.java:55-61](M23_AllPathsFromSourceToTarget.java#L55-L61))

```java
static long countPaths(int[][] g, int u, Long[] memo) {
    if (u == g.length - 1) return 1;
    if (memo[u] != null) return memo[u];
    long c = 0;
    for (int v : g[u]) c += countPaths(g, v, memo);
    return memo[u] = c;
}
```

`paths(u) = Σ paths(v)` over u's out-neighbours, with `paths(target) = 1`. **O(V + E)**. It only **counts** them; listing them needs the DFS. The test checks that the DFS produces exactly this many distinct paths.

---

## 5. Approach 2: DFS + backtracking (the file's main solution)

### Code ([M23_AllPathsFromSourceToTarget.java:35-52](M23_AllPathsFromSourceToTarget.java#L35-L52))

```java
static List<List<Integer>> allPathsSourceTarget(int[][] graph) {
    List<List<Integer>> result = new ArrayList<>();
    List<Integer> path = new ArrayList<>(List.of(0));
    dfs(graph, 0, path, result);
    return result;
}

static void dfs(int[][] g, int u, List<Integer> path, List<List<Integer>> result) {
    if (u == g.length - 1) {
        result.add(new ArrayList<>(path));               // copy!
        return;
    }
    for (int v : g[u]) {
        path.add(v);                                     // choose
        dfs(g, v, path, result);
        path.remove(path.size() - 1);                    // un-choose
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `path = [0]` | every path starts at the source |
| `u == g.length − 1` | reached the target → record a copy |
| `path.add(v)` | extend the path |
| `dfs(g, v, ...)` | explore everything after v |
| `path.remove(...)` | undo, so the next neighbour starts from the same prefix |

### Dry run: `[[1,2],[3],[3],[]]`

| call | path | action |
|---|---|---|
| dfs(0) | [0] | try 1 |
| dfs(1) | [0, 1] | try 3 |
| dfs(3) | [0, 1, 3] | **record** |
| back in dfs(1) | [0, 1] | done → remove 1 |
| dfs(0) | [0] | try 2 |
| dfs(2) | [0, 2] | try 3 |
| dfs(3) | [0, 2, 3] | **record** |

→ `[[0,1,3],[0,2,3]]` ✓ (node 3 was visited twice, by different paths).

### Complexity
- **Time O(2ⁿ · n)** in the worst case (exponentially many paths, each of length up to n).
- **Space O(n)** recursion + the output.

---

## 6. Comparing the approaches

| | Count with DP | Enumerate with DFS |
|---|---|---|
| Output | the number of paths | the paths themselves |
| Time | O(V + E) | O(#paths · n) |

The file checks, on 300 random DAGs, that the DFS produces exactly the DP count of paths, that every path runs from 0 to n − 1, and that there are no duplicates.

---

## 7. Edge cases and common mistakes

**Edge cases:** a direct edge 0 → n − 1, a single path, the maximum branching (exponential output).

**Mistakes:**
1. Adding `path` without copying.
2. A global visited set (it loses paths through shared nodes).
3. Forgetting to remove the last node after recursing.

---

## 8. Interview questions and answers

**Q1. Why is a visited set unnecessary here?**
> In a DAG, no path revisits a node, so recursion depth is bounded and every path is finite. Different paths legitimately share nodes.

**Q2. What changes on a graph with cycles?**
> Track the nodes on the **current** path (add before recursing, remove after) to avoid looping, while still allowing other paths to pass through them.

### Follow-up questions

**F1. Only count the paths.**
> The memoised DP above, O(V + E).

**F2. All simple paths in a graph with cycles.**
> Backtracking with an on-path set (still exponential in general).

**F3. The k shortest paths, or the shortest of all paths.**
> BFS (unweighted) or Dijkstra (weighted), instead of enumerating.

---

## 9. Running the file

```bash
java M23_AllPathsFromSourceToTarget.java      # 3/3 tests passed (including 300 random DAGs: count == DP count)
```

**One-line summary:** *"Backtrack from 0 with a shared path list, recording a copy at n − 1; no visited set is needed because a DAG has no cycles."*
