# M16 · Minimum Path Sum

| | |
|---|---|
| **LeetCode** | [#64 Minimum Path Sum](https://leetcode.com/problems/minimum-path-sum/) |
| **Difficulty** | Medium |
| **Pattern** | Grid DP (optimisation) + **reconstructing the path** |
| **Code** | [M16_MinimumPathSum.java](M16_MinimumPathSum.java) |
| **Run** | `java M16_MinimumPathSum.java` |

The counting version of this grid is [M14 Unique Paths](M14_UniquePaths.md); the DP recipe is in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe).

---

## 1. The problem

A grid of non-negative numbers. Move only right or down from the top-left to the bottom-right, minimising the sum of the cells you visit.

| grid | answer | path |
|---|---|---|
| [[1,3,1],[1,5,1],[4,2,1]] | 7 | 1 → 3 → 1 → 1 → 1 |
| [[1,2,3],[4,5,6]] | 12 | 1 → 2 → 3 → 6 |
| [[5]] | 5 | |

---

## 2. How to think about it

### Greedy fails
Always stepping to the cheaper neighbour can walk into an expensive region later. The DP considers both ways of arriving at every cell.

| step | for this problem |
|---|---|
| **State** | dp[r][c] = the minimum sum of a path from (0, 0) to (r, c), counting both ends |
| **Transition** | dp[r][c] = grid[r][c] + min(dp[r − 1][c], dp[r][c − 1]) |
| **Base** | dp[0][0] = grid[0][0]; the first row and column have only one predecessor |
| **Answer** | dp[m − 1][n − 1] |

With one rolling row: dp[c] = grid[r][c] + min(dp[c] (above), dp[c − 1] (left)).

### Reconstruction
Keep the full table. Start at the end and step back to whichever predecessor holds dp[cur] − grid[cur]: that's the cell the best path came from.

### Dry run: [[1,3,1],[1,5,1],[4,2,1]]

| | c0 | c1 | c2 |
|---|---|---|---|
| r0 | 1 | 1+3 = 4 | 4+1 = 5 |
| r1 | 1+1 = 2 | min(4, 2)+5 = 7 | min(5, 7)+1 = 6 |
| r2 | 2+4 = 6 | min(7, 6)+2 = 8 | min(6, 8)+1 = **7** |

### Walking the path back

| at | need = dp − grid | predecessor with that value | move |
|---|---|---|---|
| (2,2) | 7 − 1 = 6 | above (1,2) = 6 | up |
| (1,2) | 6 − 1 = 5 | above (0,2) = 5 | up |
| (0,2) | 5 − 1 = 4 | (row 0: only left) (0,1) | left |
| (0,1) | 4 − 3 = 1 | left (0,0) | left |
| (0,0) | | | stop |

Path: (0,0) → (0,1) → (0,2) → (1,2) → (2,2) ✓

---

## 3. Java concepts used in this problem

### 3.1 Four cases in one loop

```java
if (r == 0 && c == 0) dp[c] = grid[0][0];
else if (r == 0) dp[c] = dp[c - 1] + grid[r][c];             // only from the left
else if (c == 0) dp[c] = dp[c] + grid[r][c];                 // only from above
else dp[c] = Math.min(dp[c], dp[c - 1]) + grid[r][c];
```

The first row and column are handled inside the same loop instead of being pre-filled.

### 3.2 `LinkedList.addFirst` to build the path in order

```java
LinkedList<String> out = new LinkedList<>();
out.addFirst("(" + r + "," + c + ")");
```

The walk goes backwards from the end, so adding each cell at the **front** gives the path in forward order without reversing. `addFirst` is O(1) on a linked list.

### 3.3 Starting a minimum at 0 or infinity

```java
int best = r == 0 && c == 0 ? 0 : Integer.MAX_VALUE;
```

In the full-table version, the start has "0 cost to arrive"; every other cell starts at infinity and takes the min of its existing predecessors.

---

## 4. Approach 1: Recursion over both moves (the referee)

### Code ([M16_MinimumPathSum.java:70-76](M16_MinimumPathSum.java#L70-L76))

```java
static int brute(int[][] g, int r, int c) {
    if (r == g.length - 1 && c == g[0].length - 1) return g[r][c];
    int best = Integer.MAX_VALUE;
    if (r + 1 < g.length) best = Math.min(best, brute(g, r + 1, c));
    if (c + 1 < g[0].length) best = Math.min(best, brute(g, r, c + 1));
    return g[r][c] + best;
}
```

The cost from (r, c) to the end, trying both moves. **Exponential.**

---

## 5. Approach 2: One rolling row (the file's main solution)

### Code ([M16_MinimumPathSum.java:36-47](M16_MinimumPathSum.java#L36-L47))

```java
static int minPathSum(int[][] grid) {
    int n = grid[0].length;
    int[] dp = new int[n];
    for (int r = 0; r < grid.length; r++)
        for (int c = 0; c < n; c++) {
            if (r == 0 && c == 0) dp[c] = grid[0][0];
            else if (r == 0) dp[c] = dp[c - 1] + grid[r][c];             // only from the left
            else if (c == 0) dp[c] = dp[c] + grid[r][c];                 // only from above
            else dp[c] = Math.min(dp[c], dp[c - 1]) + grid[r][c];
        }
    return dp[n - 1];
}
```

- **Time O(m · n), space O(n).**

---

## 6. Approach 3: Full table + path reconstruction

### Code ([M16_MinimumPathSum.java:49-68](M16_MinimumPathSum.java#L49-L68))

```java
static List<String> path(int[][] g) {
    int m = g.length, n = g[0].length;
    int[][] dp = new int[m][n];
    for (int r = 0; r < m; r++)
        for (int c = 0; c < n; c++) {
            int best = r == 0 && c == 0 ? 0 : Integer.MAX_VALUE;
            if (r > 0) best = Math.min(best, dp[r - 1][c]);
            if (c > 0) best = Math.min(best, dp[r][c - 1]);
            dp[r][c] = best + g[r][c];
        }
    LinkedList<String> out = new LinkedList<>();
    int r = m - 1, c = n - 1;
    while (true) {
        out.addFirst("(" + r + "," + c + ")");
        if (r == 0 && c == 0) break;
        int need = dp[r][c] - g[r][c];
        if (r > 0 && dp[r - 1][c] == need) r--; else c--;                   // the predecessor that produced it
    }
    return out;
}
```

| Part | What it does |
|---|---|
| the table | the same DP, keeping every cell |
| `need` | the cost of the best path to the predecessor |
| the move | go up if the cell above has exactly that cost, otherwise left |

- **Time O(m · n), space O(m · n).** Reconstruction needs the full table; the rolling row has thrown the old rows away.

---

## 7. Comparing the approaches

| | Recursion | Rolling row | Full table |
|---|---|---|---|
| Time | exponential | O(m · n) | O(m · n) |
| Space | O(m + n) stack | **O(n)** | O(m · n) |
| Gives the path? | could | no | **yes** |

The file checks the rolling version against the recursion on 500 random grids and checks the reconstructed path of the example.

---

## 8. Edge cases and common mistakes

**Edge cases:** a single cell, a single row or column, all zeros, ties between up and left.

**Mistakes:**
1. Greedy.
2. Initialising the first row with 0s and taking min with it (it treats the first row as reachable from "above").
3. Reconstructing from the rolling array (the old rows are gone).

---

## 9. Interview questions and answers

**Q1. How do you recover the path?**
> Keep the full dp table. From the end, the previous cell is whichever predecessor has dp equal to dp[cur] − grid[cur]; repeat until the start.

**Q2. What if you could also move up or left?**
> Then the grid has cycles, so DP in one order no longer works; it becomes a shortest-path problem (Dijkstra, since the costs are non-negative).

### Follow-up questions

**F1. Triangle.** → [M17](M17_Triangle.md)
> The same min-sum idea on a triangle, filled bottom-up.

**F2. Path With Minimum Effort.** → [Graph M14](../../09-Graph/03-Medium/M14_PathWithMinimumEffort.md)
> Four directions: a Dijkstra or binary-search problem, not a DP.

**F3. Cherry Pickup (LC 741).**
> Two walkers at once: a 3-D DP over (step, r1, r2).

---

## 10. Running the file

```bash
java M16_MinimumPathSum.java      # 5/5 tests passed (including the reconstructed path and 500 random grids vs recursion)
```

**One-line summary:** *"dp[r][c] = grid[r][c] + min(above, left), with one rolling row for the value or the full table to walk the path back from the end."*
