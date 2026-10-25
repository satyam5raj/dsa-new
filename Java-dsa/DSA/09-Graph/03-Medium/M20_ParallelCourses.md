# M20 · Parallel Courses

| | |
|---|---|
| **LeetCode** | [#1136 Parallel Courses](https://leetcode.com/problems/parallel-courses/) (premium) |
| **Difficulty** | Medium |
| **Pattern** | Kahn's algorithm, level by level (each level = one semester) |
| **Code** | [M20_ParallelCourses.java](M20_ParallelCourses.java) |
| **Run** | `java M20_ParallelCourses.java` |

Kahn's algorithm is introduced in [M03](M03_CourseSchedule.md) and [M04](M04_CourseScheduleII.md).

---

## 1. The problem

n courses (1..n), and `relations [prev, next]`: prev must be taken before next. In one semester you may take **any number** of courses whose prerequisites were all completed in **earlier** semesters. What's the minimum number of semesters to take them all? −1 if there's a cycle.

| n | relations | answer |
|---|---|---|
| 3 | [[1,3],[2,3]] | 2 (1 and 2, then 3) |
| 3 | [[1,2],[2,3],[3,1]] | −1 (a cycle) |
| 4 | [[1,2],[2,3],[3,4]] | 4 (a chain) |

---

## 2. How to think about it

### Course Schedule II + the BFS "level" idea
```
semester 1 = every course with in-degree 0
taking a whole semester's courses lowers the in-degrees;
the courses that drop to 0 form the NEXT semester
count the levels
```

### What the answer means
It equals the number of vertices on the **longest path** in the DAG: the **critical path**, a chain of courses that must be done one after another. Courses off that chain can always be fitted in alongside it.

### Dry run: `[[1,3],[2,3]]`
```
in-degrees: 1 → 0, 2 → 0, 3 → 2
level 1: {1, 2} → 3's in-degree drops to 0
level 2: {3}
→ 2 semesters
```

---

## 3. Java concepts used in this problem

### 3.1 The level-by-level BFS on a DAG

```java
while (!q.isEmpty()) {
    semesters++;
    for (int i = q.size(); i > 0; i--) { ... }       // everything ready this semester
}
```

The same size snapshot as level-order traversal: the courses unlocked during a semester are added for the **next** one.

### 3.2 1-based course labels

The lists and the in-degree array have n + 1 slots, so the labels 1..n are used directly as indices.

### 3.3 Memoised DFS for the longest path (the referee)

```java
return memo[u] = best;          // assign and return in one statement
```

`state[]` uses the 3-colour idea: 1 = on the current stack (seeing it again means a cycle), 2 = finished (its memo is valid).

---

## 4. Approach 1: The longest path via memoised DFS (the referee)

### Code ([M20_ParallelCourses.java:53-79](M20_ParallelCourses.java#L53-L79))

```java
static int longestPath(int n, int[][] rel) {
    List<List<Integer>> next = new ArrayList<>();
    for (int i = 0; i <= n; i++) next.add(new ArrayList<>());
    for (int[] r : rel) next.get(r[0]).add(r[1]);
    int[] memo = new int[n + 1], state = new int[n + 1];
    int best = 0;
    for (int c = 1; c <= n; c++) {
        int l = dfs(c, next, memo, state);
        if (l < 0) return -1;
        best = Math.max(best, l);
    }
    return best;
}

static int dfs(int u, List<List<Integer>> next, int[] memo, int[] state) {
    if (state[u] == 1) return -1;                        // on the stack: cycle
    if (state[u] == 2) return memo[u];
    state[u] = 1;
    int best = 1;
    for (int v : next.get(u)) {
        int l = dfs(v, next, memo, state);
        if (l < 0) return -1;
        best = Math.max(best, 1 + l);
    }
    state[u] = 2;
    return memo[u] = best;
}
```

| Line | What it does |
|---|---|
| `dfs(u)` | the number of courses on the longest chain starting at u |
| `state[u] == 1` | reached u again while still exploring it → a cycle → −1 |
| `best = 1` | the chain containing just u |
| `1 + l` | u followed by the best chain starting at a successor |
| the memo | each node is computed once |

**O(V + E).** It computes the critical path directly (this is a DP on a DAG).

---

## 5. Approach 2: Kahn by levels (the file's main solution)

### Code ([M20_ParallelCourses.java:33-50](M20_ParallelCourses.java#L33-L50))

```java
static int minimumSemesters(int n, int[][] relations) {
    List<List<Integer>> next = new ArrayList<>();
    for (int i = 0; i <= n; i++) next.add(new ArrayList<>());
    int[] indeg = new int[n + 1];
    for (int[] r : relations) { next.get(r[0]).add(r[1]); indeg[r[1]]++; }
    Queue<Integer> q = new ArrayDeque<>();
    for (int c = 1; c <= n; c++) if (indeg[c] == 0) q.add(c);
    int semesters = 0, taken = 0;
    while (!q.isEmpty()) {
        semesters++;
        for (int i = q.size(); i > 0; i--) {            // everything ready this semester
            int c = q.poll();
            taken++;
            for (int d : next.get(c)) if (--indeg[d] == 0) q.add(d);
        }
    }
    return taken == n ? semesters : -1;
}
```

### Line by line

| Line | What it does |
|---|---|
| the graph setup | prev → next edges; count each course's prerequisites |
| the initial queue | semester 1: the courses with no prerequisites |
| `semesters++` | a new semester starts |
| the size-snapshot loop | take every course that's ready now |
| `--indeg[d] == 0` | d becomes available **next** semester |
| `taken == n` | all the courses were taken; otherwise there's a cycle |

### Dry run: the chain `[[1,2],[2,3],[3,4]]`
Each level contains one course: {1}, {2}, {3}, {4} → **4** semesters ✓

### Complexity
- **Time O(V + E).**
- **Space O(V + E).**

---

## 6. Comparing the approaches

| | Longest path (memoised DFS) | Kahn by levels |
|---|---|---|
| Time | O(V + E) | O(V + E) |
| Recursion | yes | no |
| Gives the schedule? | no | yes (the levels) |

The file checks that the number of levels equals the longest path on 1,000 random inputs (with cycles and duplicate relations).

---

## 7. Edge cases and common mistakes

**Edge cases:** no relations (1 semester), a cycle (−1), a self-loop (−1), duplicate relations.

**Mistakes:**
1. Counting polled courses instead of levels.
2. Not checking `taken == n` (missing cycles).
3. Allowing a course in the same semester as its prerequisite (forgetting the level snapshot).

---

## 8. Interview questions and answers

**Q1. Why does the number of levels equal the longest path?**
> A course at level L has a prerequisite at level L − 1 (that's why it wasn't ready earlier), so there's a chain of L courses ending at it. Conversely, a chain of L courses needs at least L semesters.

**Q2. What if at most k courses can be taken per semester (LC 1494)?**
> It becomes NP-hard in general; for small n, use a DP over bitmasks of the completed courses.

### Follow-up questions

**F1. Parallel Courses III (LC 2050): each course takes time[i] months.**
> The longest **weighted** path in the DAG: Kahn, tracking each course's earliest finish time.

**F2. Return the actual schedule.**
> Record the courses of each level.

**F3. Course Schedule II.** → [M04](M04_CourseScheduleII.md)

---

## 9. Running the file

```bash
java M20_ParallelCourses.java      # 4/4 tests passed (including 1,000 random: levels == longest path)
```

**One-line summary:** *"Kahn's algorithm level by level: each level of in-degree-0 courses is one semester; the number of levels is the answer, or −1 if not every course gets taken."*
