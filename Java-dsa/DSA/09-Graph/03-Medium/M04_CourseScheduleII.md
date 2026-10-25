# M04 · Course Schedule II

| | |
|---|---|
| **LeetCode** | [#210 Course Schedule II](https://leetcode.com/problems/course-schedule-ii/) |
| **Difficulty** | Medium |
| **Pattern** | Topological order (Kahn): return the order itself, or [] on a cycle |
| **Code** | [M04_CourseScheduleII.java](M04_CourseScheduleII.java) |
| **Run** | `java M04_CourseScheduleII.java` |

Graph basics are explained in [E01](../02-Easy/E01_FindIfPathExists.md#3-java-concepts-used-in-this-problem). Kahn's algorithm and the DFS colours are introduced in [M03](M03_CourseSchedule.md).

---

## 1. The problem

The same as [M03](M03_CourseSchedule.md), but return **one valid order** for taking all the courses (or `[]` if that's impossible).

| numCourses | prerequisites | answer |
|---|---|---|
| 2 | [[1,0]] | [0, 1] |
| 4 | [[1,0],[2,0],[3,1],[3,2]] | [0,1,2,3] or [0,2,1,3] |
| 1 | [] | [0] |
| 2 | [[1,0],[0,1]] | [] (a cycle) |

---

## 2. How to think about it

### Kahn's algorithm already produces the order
It's the sequence in which the courses are polled from the "ready" queue. If fewer than numCourses get polled, there's a cycle → return `[]`.

### Here the edge direction matters
`[a, b]` = b **before** a → the edge b → a, and `indeg[a]++`. Reverse it and you'd output the order backwards.

### The DFS version: reversed postorder
A vertex finishes (turns black) only after everything it unlocks has finished, so listing the vertices in **reverse finishing order** puts every prerequisite before the courses that depend on it ([01-Basics/TopologicalSort](../01-Basics/TopologicalSort.java)). Kahn is usually preferred in interviews: it's iterative, and the cycle detection comes for free.

### Dry run: `4, [[1,0],[2,0],[3,1],[3,2]]`
```
indeg [0, 1, 1, 2];  ready [0]
take 0 → indeg[1], indeg[2] drop to 0 → ready [1, 2]
take 1 → indeg[3] = 1
take 2 → indeg[3] = 0 → ready [3]
take 3
→ [0, 1, 2, 3]
```

---

## 3. Java concepts used in this problem

### 3.1 Filling an array with a moving index

```java
int[] order = new int[numCourses];
int idx = 0;
order[idx++] = c;                // store, then advance
return idx == numCourses ? order : new int[0];
```

`idx` doubles as the count of courses taken, which is the cycle check.

### 3.2 `new int[0]`: an empty array

LeetCode expects an empty array, not null, when there's no valid order.

### 3.3 Verifying an order (in the test)

```java
pos[order[i]] = i;                                   // where each course appears
for (int[] p : prereq) if (pos[p[1]] > pos[p[0]]) return false;
```

An order is valid iff it's a permutation of all the courses and every prerequisite b appears **before** a. Since many orders can be valid, the test checks validity instead of comparing with one fixed answer.

### 3.4 `ArrayDeque.push` builds a reversed list

In the DFS version, pushing each vertex as it finishes onto a deque used as a stack, then iterating from the front, gives the **reverse** finishing order directly.

---

## 4. Approach 1: DFS reversed postorder

```java
static int[] findOrderDfs(int n, int[][] pre) {
    List<List<Integer>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (int[] p : pre) adj.get(p[1]).add(p[0]);
    int[] color = new int[n];
    Deque<Integer> post = new ArrayDeque<>();
    for (int u = 0; u < n; u++) if (color[u] == 0 && !visit(adj, u, color, post)) return new int[0];
    int[] order = new int[n];
    int i = 0;
    for (int x : post) order[i++] = x;                   // front of the deque = finished last
    return order;
}

static boolean visit(List<List<Integer>> adj, int u, int[] color, Deque<Integer> post) {
    color[u] = 1;                                         // gray
    for (int v : adj.get(u)) {
        if (color[v] == 1) return false;                  // a cycle
        if (color[v] == 0 && !visit(adj, v, color, post)) return false;
    }
    color[u] = 2;                                         // black
    post.push(u);                                         // finished: goes in front of everything it unlocks
    return true;
}
```

| Line | What it does |
|---|---|
| `color` | the 3-state cycle detection from M03 |
| `post.push(u)` after the loop | u finishes after everything it unlocks |
| iterating `post` | the most recently finished first = the reverse postorder |

**Dry run (the diamond):** visit(0) → visit(1) → visit(3): 3 finishes (post [3]); 1 finishes ([1, 3]); visit(2): 3 is black; 2 finishes ([2, 1, 3]); 0 finishes ([0, 2, 1, 3]) → **[0, 2, 1, 3]**, a valid order (checked by running it).

**O(V + E).**

---

## 5. Approach 2: Kahn's algorithm (the file's main solution)

### Code ([M04_CourseScheduleII.java:37-52](M04_CourseScheduleII.java#L37-L52))

```java
static int[] findOrder(int numCourses, int[][] prerequisites) {
    List<List<Integer>> unlocks = new ArrayList<>();
    for (int i = 0; i < numCourses; i++) unlocks.add(new ArrayList<>());
    int[] indeg = new int[numCourses];
    for (int[] p : prerequisites) { unlocks.get(p[1]).add(p[0]); indeg[p[0]]++; }
    Queue<Integer> ready = new ArrayDeque<>();
    for (int c = 0; c < numCourses; c++) if (indeg[c] == 0) ready.add(c);
    int[] order = new int[numCourses];
    int idx = 0;
    while (!ready.isEmpty()) {
        int c = ready.poll();
        order[idx++] = c;
        for (int next : unlocks.get(c)) if (--indeg[next] == 0) ready.add(next);
    }
    return idx == numCourses ? order : new int[0];
}
```

### Line by line

| Line | What it does |
|---|---|
| the graph setup | the edge b → a for "b before a"; count each course's prerequisites |
| `ready` | the courses that can be taken right now |
| `order[idx++] = c` | the poll order **is** the topological order |
| `--indeg[next] == 0` | a course becomes available once its last prerequisite is taken |
| `idx == numCourses` | everything was placed; otherwise some courses are stuck on a cycle |

### Dry run: `2, [[1,0]]`
indeg = [0, 1] → ready [0] → take 0 (order [0]) → indeg[1] = 0 → ready [1] → take 1 → **[0, 1]** ✓

### Complexity
- **Time O(V + E).**
- **Space O(V + E).**

---

## 6. Comparing the approaches

| | DFS reversed postorder | Kahn |
|---|---|---|
| Time | O(V + E) | O(V + E) |
| Recursion | yes | no |
| Cycle detection | gray vertex met | fewer than V polled |
| Lexicographically smallest order | hard | swap the queue for a min-heap |

The file checks Kahn's output on 1,000 random inputs: either a valid order, or `[]` exactly when an independent DFS finds a real cycle.

---

## 7. Edge cases and common mistakes

**Edge cases:** no prerequisites (any permutation, e.g. 0..n−1), a self-loop, isolated courses, duplicate pairs.

**Mistakes:**
1. The edge direction reversed (prints the order backwards).
2. Returning a partial order instead of `[]` when there's a cycle.
3. In DFS, appending in postorder **without** reversing.

---

## 8. Interview questions and answers

**Q1. Is the topological order unique?**
> Only if, at every step, exactly one course is ready (the graph contains a Hamiltonian path). Otherwise there are several valid orders.

**Q2. How would you get the lexicographically smallest order?**
> Use a min-heap (`PriorityQueue`) instead of the FIFO queue in Kahn's algorithm: O((V + E) log V).

### Follow-up questions

**F1. Alien Dictionary.** → [H03](../04-Hard/H03_AlienDictionary.md)
> Build the letter-order edges from adjacent words, then a topological sort.

**F2. Parallel Courses: the minimum number of semesters.** → [M20](M20_ParallelCourses.md)
> Kahn level by level.

**F3. Sort Items by Groups Respecting Dependencies (LC 1203).**
> Two nested topological sorts: the groups, then the items inside each group.

---

## 9. Running the file

```bash
java M04_CourseScheduleII.java      # 5/5 tests passed (including 1,000 random: valid order or a real cycle)
```

**One-line summary:** *"Kahn's algorithm: the order in which in-degree-0 courses are polled is a valid schedule; if fewer than numCourses are polled, return [] (a cycle)."*
