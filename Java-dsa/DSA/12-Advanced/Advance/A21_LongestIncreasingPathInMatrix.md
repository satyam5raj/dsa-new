# A21 · Longest Increasing Path in a Matrix ⭐

| | |
|---|---|
| **LeetCode** | [#329 Longest Increasing Path in a Matrix](https://leetcode.com/problems/longest-increasing-path-in-a-matrix/) |
| **Difficulty** | Hard |
| **Mix of structures** | grid **graph** (a DAG) + **DFS with memo** (DP), or **topological sort** with a queue |
| **Code** | [A21_LongestIncreasingPathInMatrix.java](A21_LongestIncreasingPathInMatrix.java) |
| **Run** | `java A21_LongestIncreasingPathInMatrix.java` |

⭐ = not in `leetcode.md`, added because it's the classic "DP on a graph" problem, and interviewers love it.

---

## 1. The problem

Return the length of the longest **strictly increasing** path in the matrix. You can move up, down, left or right (no diagonals, no wrapping).

```
[[9,9,4],
 [6,6,8],      →  4     1 → 2 → 6 → 9
 [2,1,1]]

[[3,4,5],
 [3,2,6],      →  4     3 → 4 → 5 → 6
 [2,2,1]]
```

---

## 2. How to think about it

### See the grid as a graph
Draw an arrow from each cell to every neighbour with a **bigger** value. Values strictly increase along arrows, so you can never come back to where you started: **no cycles**. The graph is a **DAG** (directed acyclic graph).

"Longest increasing path" = "longest path in this DAG". In a general graph, the longest simple path is NP-hard; in a DAG it's easy, because there's a processing order where every node comes after everything that leads into it.

### Brute force
DFS from every cell, exploring every increasing path. It's correct, but the same sub-paths are explored again and again: exponential.

### Approach 1: DFS + memo (top-down DP)
```
best(cell) = 1 + max(best(n) for each bigger neighbour n)       (1 if there is none)
```
The answer for a cell never changes, so compute it once and cache it. Each cell is solved once and looks at 4 neighbours: **O(m · n)**.

**The catch:** on a 200 × 200 "snake" (values increasing along a zig-zag through every cell), the recursion is 40,000 calls deep, which can overflow Java's call stack.

### Approach 2: topological sort (bottom-up, main)
- `indegree(cell)` = how many **smaller** neighbours it has (arrows coming in).
- Start a BFS from every cell with indegree 0, the local minima.
- Process level by level, like Kahn's algorithm: when a cell is processed, each bigger neighbour loses one incoming arrow, and joins the next level when it reaches 0.
- **The number of levels is the length of the longest path.** A cell enters level L exactly when the longest path ending at it has length L.

It's iterative, so there's no stack-overflow risk.

### Dry run: example 1 (from the real run)
```
[[9,9,4],          indegree        [[1,2,0],
 [6,6,8],                           [1,1,3],
 [2,1,1]]                           [1,0,0]]
```
| level | cells (value@position) |
|---|---|
| 1 | 4@(0,2), 1@(2,1), 1@(2,2) |
| 2 | 6@(1,1), 2@(2,0) |
| 3 | 9@(0,1), 8@(1,2), 6@(1,0) |
| 4 | 9@(0,0) |

**4 levels** ✓. Notice 8 waits until level 3: it's bigger than 4, 1 **and** the 6 at (1,1), which only finishes at level 2.

---

## 3. Java concepts used in this problem

### 3.1 Memo with `0` meaning "not computed"
```java
int[][] memo = new int[matrix.length][matrix[0].length];   // 0 = not computed yet
if (memo[r][c] != 0) return memo[r][c];
```
Every real answer is at least 1, so 0 can safely mean "unknown". `return memo[r][c] = best;` stores and returns in one statement (an assignment is an expression whose value is the assigned value).

### 3.2 Recursion depth in Java
Each call uses a stack frame; the default thread stack fits roughly 10,000–50,000 small frames, depending on the JVM and the frame size. Deep recursion throws `StackOverflowError`. Fixes: an iterative algorithm (Approach 2), an explicit stack, or running in a thread created with a bigger stack size.

### 3.3 Level-by-level BFS
The same `for (int size = queue.size(); size > 0; size--)` pattern as [A08](A08_ShortestPathWithObstaclesElimination.md#3-java-concepts-used-in-this-problem): read the size once per level, and count the levels.

### 3.4 `--indegree[nr][nc] == 0` inside a condition
The decrement happens first, then the comparison. It's written last in the `&&` chain, so it only runs when the neighbour really is bigger (short-circuit evaluation).

---

## 4. The referee: DFS without memo

### Code ([A21_LongestIncreasingPathInMatrix.java:50-65](A21_LongestIncreasingPathInMatrix.java#L50-L65))
```java
static int longestIncreasingPathBrute(int[][] matrix) {
    int best = 0;
    for (int r = 0; r < matrix.length; r++)
        for (int c = 0; c < matrix[0].length; c++) best = Math.max(best, explore(matrix, r, c));
    return best;
}

private static int explore(int[][] mat, int r, int c) {
    int best = 1;
    for (int[] d : DIRS) {
        int nr = r + d[0], nc = c + d[1];
        if (nr >= 0 && nr < mat.length && nc >= 0 && nc < mat[0].length && mat[nr][nc] > mat[r][c])
            best = Math.max(best, 1 + explore(mat, nr, nc));
    }
    return best;
}
```

No visited set is needed: strictly increasing values can't loop. But nothing is cached, so it's **exponential**; the tests use grids up to 4 × 4.

---

## 5. Approach 1: DFS + memo

### Code ([A21_LongestIncreasingPathInMatrix.java:68-85](A21_LongestIncreasingPathInMatrix.java#L68-L85))
```java
static int longestIncreasingPathMemo(int[][] matrix) {
    int[][] memo = new int[matrix.length][matrix[0].length];         // 0 = not computed yet
    int best = 0;
    for (int r = 0; r < matrix.length; r++)
        for (int c = 0; c < matrix[0].length; c++) best = Math.max(best, dfs(matrix, r, c, memo));
    return best;
}

private static int dfs(int[][] mat, int r, int c, int[][] memo) {
    if (memo[r][c] != 0) return memo[r][c];
    int best = 1;
    for (int[] d : DIRS) {
        int nr = r + d[0], nc = c + d[1];
        if (nr >= 0 && nr < mat.length && nc >= 0 && nc < mat[0].length && mat[nr][nc] > mat[r][c])
            best = Math.max(best, 1 + dfs(mat, nr, nc, memo));
    }
    return memo[r][c] = best;
}
```

It's the brute force plus two lines: return the cached value, and store the result. **O(m · n)** time and space; recursion depth up to m · n.

---

## 6. Approach 2: Topological sort, count the levels (main solution)

### Code ([A21_LongestIncreasingPathInMatrix.java:88-114](A21_LongestIncreasingPathInMatrix.java#L88-L114))
```java
static int longestIncreasingPath(int[][] matrix) {
    int m = matrix.length, n = matrix[0].length;
    int[][] indegree = new int[m][n];                                 // arrows point from smaller to bigger
    for (int r = 0; r < m; r++)
        for (int c = 0; c < n; c++)
            for (int[] d : DIRS) {
                int nr = r + d[0], nc = c + d[1];
                if (nr >= 0 && nr < m && nc >= 0 && nc < n && matrix[nr][nc] < matrix[r][c]) indegree[r][c]++;
            }
    Deque<int[]> queue = new ArrayDeque<>();
    for (int r = 0; r < m; r++)
        for (int c = 0; c < n; c++) if (indegree[r][c] == 0) queue.add(new int[]{r, c});   // local minima

    int levels = 0;
    while (!queue.isEmpty()) {
        levels++;
        for (int size = queue.size(); size > 0; size--) {
            int[] cur = queue.poll();
            for (int[] d : DIRS) {
                int nr = cur[0] + d[0], nc = cur[1] + d[1];
                if (nr >= 0 && nr < m && nc >= 0 && nc < n && matrix[nr][nc] > matrix[cur[0]][cur[1]]
                        && --indegree[nr][nc] == 0) queue.add(new int[]{nr, nc});
            }
        }
    }
    return levels;
}
```

### Line by line
| Line | What it does |
|---|---|
| the indegree loops | count each cell's smaller neighbours |
| seed the queue | cells nothing leads into: every path can start at one of them |
| `levels++` | one more step along the longest path |
| the inner loop | process exactly one level |
| bigger neighbour, `--indegree == 0` | all of its smaller neighbours are done, so it belongs to the next level |
| `return levels` | the length of the longest chain of levels |

### Complexity
**O(m · n)** time (each cell enters the queue once and checks 4 neighbours), **O(m · n)** space, and no recursion.

---

## 7. Comparing the approaches

| | Time | Stack risk |
|---|---|---|
| DFS without memo | exponential | deep |
| DFS + memo | O(m · n) | depth up to m · n |
| **Topological levels** | **O(m · n)** | none |

The file checks all three on 1,500 random grids, and runs the topological version on a 200 × 200 snake (answer 40,000).

---

## 8. Edge cases and common mistakes

**Edge cases:** all values equal (answer 1), a single cell, one long snake path, many equal neighbours (equal is **not** increasing).

**Mistakes:**
1. Using a `visited` set like normal DFS: it blocks valid paths through a cell, and isn't needed because there are no cycles.
2. `>=` instead of `>` (equal values would create cycles).
3. Forgetting the memo, which is exponential.
4. Counting edges instead of cells (off by one).
5. Deep recursion on big inputs without considering the stack.

---

## 9. Interview questions and answers

**Q1. Why is memoisation valid here?**
> The graph of strictly increasing moves is a DAG, so the longest path from a cell doesn't depend on how you got there: it's a pure function of the cell. Compute it once and reuse it.

**Q2. Why don't you need a visited set?**
> Values strictly increase along a path, so a path can never revisit a cell.

**Q3. How does the topological sort give the answer?**
> Peeling local minima level by level puts each cell in the level equal to the longest increasing path ending at it. The number of levels is the overall maximum.

### Follow-up questions

**F1. "Return the path itself."**
> With memo, store the best next cell for each cell and follow it from the cell with the largest value. With the topological version, store each cell's predecessor from the previous level.

**F2. "Count the number of increasing paths (Count Paths in a Grid, 2328)."**
> The same DAG DP, summing instead of maximising: `ways(cell) = 1 + sum of ways(bigger neighbours)`, mod 1e9+7.

**F3. "Allow diagonal moves."**
> Use 8 directions; still a DAG, same algorithms.

---

## 10. Running the file

```bash
java A21_LongestIncreasingPathInMatrix.java     # 6/6 tests passed (examples, all equal, 200x200 snake, 1,500 random grids)
```

**One-line summary:** *"Increasing moves form a DAG, so the longest path from a cell can be memoised (DFS + memo), or found iteratively with a topological sort: start from local minima, peel the grid level by level, and count the levels."*
