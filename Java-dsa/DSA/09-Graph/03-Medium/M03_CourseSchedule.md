# M03 · Course Schedule

| | |
|---|---|
| **LeetCode** | [#207 Course Schedule](https://leetcode.com/problems/course-schedule/) |
| **Difficulty** | Medium |
| **Pattern** | Cycle detection in a directed graph (Kahn's topological sort) |
| **Code** | [M03_CourseSchedule.java](M03_CourseSchedule.java) |
| **Run** | `java M03_CourseSchedule.java` |

Graph basics are explained in [E01](../02-Easy/E01_FindIfPathExists.md#3-java-concepts-used-in-this-problem). Topological sort is built from scratch in [01-Basics/TopologicalSort](../01-Basics/TopologicalSort.java).

---

## 1. The problem

There are `numCourses` courses. `prerequisites[i] = [a, b]` means "take b before a". Can you finish all of them?

| numCourses | prerequisites | answer |
|---|---|---|
| 2 | [[1,0]] | true (0, then 1) |
| 2 | [[1,0],[0,1]] | false (each waits for the other) |
| 1 | [[0,0]] | false (a self-loop) |
| 4 | [[1,0],[2,0],[3,1],[3,2]] | true (a diamond, no cycle) |

---

## 2. How to think about it

### The model
Draw an edge **b → a** ("b unlocks a"). All the courses can be finished ⇔ the graph has **no cycle** ⇔ a **topological order** exists (an ordering where every edge goes forward).

```
diamond:      0 → 1 → 3              cycle:   0 ⇄ 1
              0 → 2 → 3
```

### Kahn's algorithm
Start with the courses that have no prerequisites (**in-degree 0**). "Take" one, which lowers the in-degree of every course it unlocks; any that reach 0 become takeable. Count how many were taken. All of them → true.

Courses on a cycle never reach in-degree 0 (each waits for another one on the cycle), so the count falls short.

### The DFS alternative: three colours
- **white** (0) = not visited, **gray** (1) = on the current DFS path, **black** (2) = finished.
- Reaching a **gray** vertex means we've walked back onto our own path: a **cycle**.

### The direction trap
`[a, b]` means b → a. Getting it backwards still detects cycles correctly here (a reversed graph has the same cycles), but it matters in [Course Schedule II](M04_CourseScheduleII.md), where the **order** must be returned.

---

## 3. Java concepts used in this problem

### 3.1 In-degrees for a directed graph

```java
for (int[] p : prerequisites) {
    unlocks.get(p[1]).add(p[0]);         // the edge p[1] → p[0]
    indeg[p[0]]++;                       // p[0] has one more prerequisite
}
```

For directed graphs, add **one** direction only.

### 3.2 Pre-decrement in a condition: `--indeg[next] == 0`

It decrements first, then compares: "this was the last remaining prerequisite of `next`".

### 3.3 An `int[] color` instead of a `boolean[] visited`

Directed-cycle detection needs **three** states. A plain visited flag can't distinguish "on the current path" (a cycle) from "finished earlier via another path" (not a cycle):

```
0 → 1,  0 → 2,  1 → 2       2 is reached twice, but there is no cycle
```

---

## 4. Approach 1: DFS with three colours

### Code ([M03_CourseSchedule.java:56-73](M03_CourseSchedule.java#L56-L73))

```java
static boolean canFinishDfs(int n, int[][] prereq) {
    List<List<Integer>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (int[] p : prereq) adj.get(p[1]).add(p[0]);
    int[] color = new int[n];
    for (int u = 0; u < n; u++) if (color[u] == 0 && hasCycle(adj, u, color)) return false;
    return true;
}

static boolean hasCycle(List<List<Integer>> adj, int u, int[] color) {
    color[u] = 1;                            // gray: on the current path
    for (int v : adj.get(u)) {
        if (color[v] == 1) return true;
        if (color[v] == 0 && hasCycle(adj, v, color)) return true;
    }
    color[u] = 2;                            // black: done
    return false;
}
```

### Line by line

| Line | What it does |
|---|---|
| the outer loop | start a DFS from every unvisited vertex (the graph may be disconnected) |
| `color[u] = 1` | u is on the current path |
| `color[v] == 1` | an edge back to the current path → a cycle |
| `color[v] == 0 && hasCycle(...)` | explore an unvisited neighbour |
| `color[u] = 2` | everything reachable from u is cycle-free; never explore u again |

### Dry run: `2, [[1,0],[0,1]]` (edges 0 → 1, 1 → 0)

| call | action | colours (0, 1) |
|---|---|---|
| hasCycle(0) | gray 0 → neighbour 1 is white → recurse | (1, 0) |
| hasCycle(1) | gray 1 → neighbour 0 is **gray** → cycle! | (1, 1) |

→ **false** ✓

### Complexity
- **Time O(V + E).**
- **Space O(V + E)**, with O(V) recursion depth.

---

## 5. Approach 2: Kahn's algorithm (the file's main solution)

### Code ([M03_CourseSchedule.java:37-54](M03_CourseSchedule.java#L37-L54))

```java
static boolean canFinish(int numCourses, int[][] prerequisites) {
    List<List<Integer>> unlocks = new ArrayList<>();
    for (int i = 0; i < numCourses; i++) unlocks.add(new ArrayList<>());
    int[] indeg = new int[numCourses];
    for (int[] p : prerequisites) {
        unlocks.get(p[1]).add(p[0]);         // p[1] must come before p[0]
        indeg[p[0]]++;
    }
    Queue<Integer> ready = new ArrayDeque<>();
    for (int c = 0; c < numCourses; c++) if (indeg[c] == 0) ready.add(c);
    int taken = 0;
    while (!ready.isEmpty()) {
        int c = ready.poll();
        taken++;
        for (int next : unlocks.get(c)) if (--indeg[next] == 0) ready.add(next);
    }
    return taken == numCourses;              // anything left is stuck on a cycle
}
```

### Line by line

| Line | What it does |
|---|---|
| `unlocks`, `indeg` | the directed graph b → a and each course's number of unmet prerequisites |
| the initial `ready` | all the courses with no prerequisites |
| `c = ready.poll(); taken++` | take a course |
| `--indeg[next] == 0` | `next` has just lost its last unmet prerequisite → ready |
| `taken == numCourses` | everything was taken ⇔ no cycle |

### Dry run: the diamond, `4, [[1,0],[2,0],[3,1],[3,2]]`

indeg = [0, 1, 1, 2]

| take | in-degree updates | ready after | taken |
|---|---|---|---|
| (start) | | [0] | 0 |
| 0 | 1 → 0, 2 → 0 | [1, 2] | 1 |
| 1 | 3: 2 → 1 | [2] | 2 |
| 2 | 3: 1 → 0 | [3] | 3 |
| 3 | | [] | 4 |

taken = 4 → **true** ✓

### Dry run: the cycle `2, [[1,0],[0,1]]`
indeg = [1, 1] → nothing is ready → taken = 0 < 2 → **false** ✓

### Complexity
- **Time O(V + E).**
- **Space O(V + E).**

---

## 6. Comparing the approaches

| | DFS colours | Kahn (BFS) |
|---|---|---|
| Time | O(V + E) | O(V + E) |
| Recursion | yes (depth V) | no |
| Also gives an order? | yes (reversed postorder) | yes (the poll order) |

The file checks that Kahn and the DFS colours agree on 1,000 random prerequisite lists (with self-loops and duplicates).

---

## 7. Edge cases and common mistakes

**Edge cases:** no prerequisites (true), a self-loop (false), duplicate prerequisite pairs (handled, since in-degree counts every copy), disconnected parts.

**Mistakes:**
1. Treating it as undirected (any undirected edge looks like a "cycle").
2. A boolean visited instead of three colours in DFS.
3. Starting the DFS only from course 0 (misses the other components).
4. Forgetting that duplicate edges add to the in-degree twice (Kahn handles this naturally, since each copy is also decremented once).

---

## 8. Interview questions and answers

**Q1. Why do courses on a cycle never become ready in Kahn's algorithm?**
> Each course on the cycle has a prerequisite that is also on the cycle. None of them can be taken first, so their in-degrees never reach 0.

**Q2. Why are three colours needed in DFS?**
> A vertex finished earlier (black) via a different path is fine to meet again. Only meeting a vertex on the **current** path (gray) means the path loops back on itself.

### Follow-up questions

**F1. Return a valid order.** → [M04](M04_CourseScheduleII.md)
> Record Kahn's poll order.

**F2. The minimum number of semesters (LC 1136).** → [M20 Parallel Courses](M20_ParallelCourses.md)
> Kahn level by level: the number of BFS levels.

**F3. Find all the courses on cycles / eventual safe states.** → [M18](M18_FindEventualSafeStates.md)
> Kahn on the reversed graph, or the DFS colours.

---

## 9. Running the file

```bash
java M03_CourseSchedule.java      # 5/5 tests passed (including 1,000 random: Kahn == DFS colours)
```

**One-line summary:** *"Edges b → a; Kahn's algorithm repeatedly takes the courses with in-degree 0; if every course gets taken there's no cycle, so the answer is true."*
