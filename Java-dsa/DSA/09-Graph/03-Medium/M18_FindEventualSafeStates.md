# M18 · Find Eventual Safe States

| | |
|---|---|
| **LeetCode** | [#802 Find Eventual Safe States](https://leetcode.com/problems/find-eventual-safe-states/) |
| **Difficulty** | Medium |
| **Pattern** | 3-colour DFS ("on a cycle or leads to one" = unsafe), or Kahn on the reversed graph |
| **Code** | [M18_FindEventualSafeStates.java](M18_FindEventualSafeStates.java) |
| **Run** | `java M18_FindEventualSafeStates.java` |

The DFS colours and Kahn's algorithm are introduced in [M03 Course Schedule](M03_CourseSchedule.md).

---

## 1. The problem

A directed graph. A **terminal** node has no outgoing edges. A node is **safe** if **every** path from it ends at a terminal node (it can never get stuck in a cycle). Return all the safe nodes, sorted.

```
graph [[1,2],[2,3],[5],[0],[5],[],[]]  →  [2, 4, 5, 6]

0 → 1 → 3 → 0   (a cycle: 0, 1, 3 are unsafe)
0 → 2,  1 → 2,  2 → 5,  4 → 5      5 and 6 are terminal
```

| graph | answer |
|---|---|
| the example | [2, 4, 5, 6] |
| `[[1,2,3,4],[1,2],[3,4],[0,4],[]]` | [4] |

---

## 2. How to think about it

### Approach 1: DFS with 3 colours (the directed cycle detector)
- **GRAY** = on the current path, **or** found to be unsafe (we leave it gray)
- **BLACK** = proven safe

```
safe(u):  if GRAY → false (a cycle);  if BLACK → true
          colour u GRAY;  if any neighbour is unsafe → return false (u stays GRAY)
          otherwise colour u BLACK → true
```

**The trick is leaving unsafe nodes GRAY:** anyone who later reaches them also sees "gray" and becomes unsafe, which is correct (they lead to a cycle).

### Approach 2: reverse the graph + Kahn
Terminal nodes are safe. A node is safe once **all** its out-neighbours are safe. Reverse every edge; then the out-degree plays the role of the in-degree: start with the out-degree-0 nodes and "peel" backwards. Whatever gets peeled is safe.

### Dry run: the DFS on the example (from the real run)

```
isSafe(0) gray
  isSafe(1) gray
    isSafe(2) gray
      isSafe(5) gray → no edges → black (safe)
      2 black (safe)
    isSafe(3) gray
      isSafe(0) → gray → false        (the cycle 0 → 1 → 3 → 0)
      3 unsafe (stays gray)
    1 unsafe (stays gray)
  0 unsafe (stays gray)
isSafe(1), isSafe(3) → gray → false;  isSafe(2), isSafe(5) → black → true
isSafe(4) → 5 is black → 4 black;  isSafe(6) → no edges → black
→ [2, 4, 5, 6]
```

---

## 3. Java concepts used in this problem

### 3.1 Using the colour as the memo

```java
if (color[u] != 0) return color[u] == 2;
```

Once a node is decided (gray = unsafe, black = safe), it's never recomputed, so the total work is O(V + E) even though `isSafe` is called for every node.

### 3.2 Early `return false` leaves the node gray

```java
for (int v : g[u]) if (!isSafe(g, v, color)) return false;   // stays gray = unsafe
color[u] = 2;
```

The black assignment is skipped on the early return, so "gray after the DFS" = unsafe.

### 3.3 Reversed adjacency lists

```java
for (int v : graph[u]) rev.get(v).add(u);        // the edge u → v becomes v → u
```

Reversing lets Kahn's "peel the nodes whose remaining count hit 0" run backwards from the terminals.

---

## 4. Approach 1: 3-colour DFS (the file's main solution)

### Code ([M18_FindEventualSafeStates.java:38-52](M18_FindEventualSafeStates.java#L38-L52))

```java
static List<Integer> eventualSafeNodes(int[][] graph) {
    int n = graph.length;
    int[] color = new int[n];                    // 0 white, 1 gray (on path / unsafe), 2 black (safe)
    List<Integer> safe = new ArrayList<>();
    for (int u = 0; u < n; u++) if (isSafe(graph, u, color)) safe.add(u);
    return safe;
}

static boolean isSafe(int[][] g, int u, int[] color) {
    if (color[u] != 0) return color[u] == 2;
    color[u] = 1;
    for (int v : g[u]) if (!isSafe(g, v, color)) return false;   // stays gray = unsafe
    color[u] = 2;
    return true;
}
```

### Line by line

| Line | What it does |
|---|---|
| the loop over u, in order | the safe nodes are collected already sorted |
| `color[u] != 0` | decided (or on the current path): gray → false, black → true |
| `color[u] = 1` | u is on the current path |
| an unsafe neighbour | then u is unsafe too; return while it's still gray |
| `color[u] = 2` | every path from u reaches a terminal |

### Complexity
- **Time O(V + E).**
- **Space O(V)** (plus the recursion).

---

## 5. Approach 2: Kahn on the reversed graph

### Code ([M18_FindEventualSafeStates.java:54-71](M18_FindEventualSafeStates.java#L54-L71))

```java
static List<Integer> kahnReversed(int[][] graph) {
    int n = graph.length;
    List<List<Integer>> rev = new ArrayList<>();
    for (int i = 0; i < n; i++) rev.add(new ArrayList<>());
    int[] outdeg = new int[n];
    for (int u = 0; u < n; u++) { outdeg[u] = graph[u].length; for (int v : graph[u]) rev.get(v).add(u); }
    Queue<Integer> q = new ArrayDeque<>();
    for (int u = 0; u < n; u++) if (outdeg[u] == 0) q.add(u);     // terminals
    boolean[] safe = new boolean[n];
    while (!q.isEmpty()) {
        int v = q.poll();
        safe[v] = true;
        for (int u : rev.get(v)) if (--outdeg[u] == 0) q.add(u);   // all of u's exits are safe now
    }
    List<Integer> out = new ArrayList<>();
    for (int u = 0; u < n; u++) if (safe[u]) out.add(u);
    return out;
}
```

### Line by line

| Line | What it does |
|---|---|
| `outdeg[u]` | how many of u's exits are not yet known to be safe |
| `rev` | who points at v |
| the initial queue | the terminals are safe |
| `--outdeg[u] == 0` | the last unknown exit of u just became safe → u is safe |

### Dry run: the example
Terminals: 5, 6. Peel 5 → its predecessors 2 (outdeg 1 → 0, safe) and 4 (1 → 0, safe). Peel 6 → nobody points at it. Peel 2 → predecessors 0 (2 → 1) and 1 (2 → 1): not yet. Peel 4 → nobody. The queue is empty: 0, 1 and 3 never reach 0 → safe = **{2, 4, 5, 6}** ✓

### Complexity
- **Time O(V + E).**
- **Space O(V + E).**

---

## 6. Comparing the approaches

| | 3-colour DFS | Reversed Kahn |
|---|---|---|
| Time | O(V + E) | O(V + E) |
| Recursion | yes | no |
| Extra structures | the colour array | the reversed graph + out-degrees |

The file checks both on 1,000 random directed graphs (with self-loops).

---

## 7. Edge cases and common mistakes

**Edge cases:** self-loops (unsafe), all terminals (all safe), a single big cycle (none safe), nodes leading into a cycle (unsafe).

**Mistakes:**
1. Treating "can reach a terminal" as safe (it needs **every** path to).
2. Resetting gray nodes to white after finding them unsafe (repeated work, and wrong answers).
3. Forgetting to sort the output (here, the loop order already sorts it).

---

## 8. Interview questions and answers

**Q1. Why can unsafe nodes stay gray?**
> Gray means "on a cycle or leads to one". A later DFS that reaches a gray node is also on a path to a cycle, so treating gray as unsafe is exactly right.

**Q2. Why does reversed Kahn identify the safe nodes?**
> A node is safe iff all its successors are safe. Starting from the terminals and peeling backwards marks exactly the nodes whose successors have all been marked. Nodes on or leading into cycles always keep some unmarked successor.

### Follow-up questions

**F1. Course Schedule.** → [M03](M03_CourseSchedule.md)
> The same colours, asking "is there any cycle?".

**F2. Count the nodes that can reach a cycle.**
> n − (the number of safe nodes).

**F3. Detect cycles in an undirected graph.**
> Track the parent (or the arriving edge): [M10](M10_GraphValidTree.md).

---

## 9. Running the file

```bash
java M18_FindEventualSafeStates.java      # 4/4 tests passed (including 1,000 random graphs: DFS == reversed Kahn)
```

**One-line summary:** *"3-colour DFS where unsafe nodes stay gray (anything reaching gray is unsafe), or reverse the edges and peel from the terminals with Kahn's algorithm."*
