# M15 · Unique Paths II

| | |
|---|---|
| **LeetCode** | [#63 Unique Paths II](https://leetcode.com/problems/unique-paths-ii/) |
| **Difficulty** | Medium |
| **Pattern** | Grid DP with **obstacles** (a blocked cell has 0 paths) |
| **Code** | [M15_UniquePathsII.java](M15_UniquePathsII.java) |
| **Run** | `java M15_UniquePathsII.java` |

The obstacle-free version is [M14 Unique Paths](M14_UniquePaths.md); the DP recipe is in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe).

---

## 1. The problem

The same as Unique Paths, but `grid[r][c] == 1` is an obstacle.

| grid | answer |
|---|---|
| [[0,0,0],[0,1,0],[0,0,0]] | 2 |
| [[0,1],[0,0]] | 1 |
| [[1]] | 0 (the start is blocked) |
| [[0,1,0],[0,0,0]] | 1 |

---

## 2. How to think about it

The same recurrence with one extra rule: an obstacle has 0 paths.

```
dp[r][c] = grid[r][c] == 1 ? 0 : dp[r − 1][c] + dp[r][c − 1]
```

### Why the formula dies
Obstacles break the "any order of moves is a valid path" symmetry, so C(m + n − 2, m − 1) no longer applies.

### The common bug
Initialising the whole first row (and first column) to 1. An obstacle in row 0 blocks **everything to its right**: those cells can't be reached at all. Using a single dp[] with dp[0] = 1 for the start, and applying the general rule everywhere (including row 0), avoids special cases.

### Dry run: 3 × 3 with the centre blocked (one row array)

| row | grid row | dp after |
|---|---|---|
| 0 | 0 0 0 | [1, 1, 1] |
| 1 | 0 1 0 | [1, **0**, 0 + 1 = 1] |
| 2 | 0 0 0 | [1, 0 + 1 = 1, 1 + 1 = **2**] |

### Dry run: [[0,1,0],[0,0,0]] (an obstacle in row 0)

| row | grid row | dp after |
|---|---|---|
| 0 | 0 1 0 | [1, 0, 0 + 0 = 0] (everything right of the obstacle is unreachable) |
| 1 | 0 0 0 | [1, 0 + 1 = 1, 0 + 1 = **1**] |

---

## 3. Java concepts used in this problem

### 3.1 One loop for every row, including the first

```java
dp[0] = 1;
for (int[] row : grid)
    for (int c = 0; c < n; c++) {
        if (row[c] == 1) dp[c] = 0;
        else if (c > 0) dp[c] += dp[c - 1];
    }
```

Before row 0 is processed, dp = [1, 0, 0, …], which acts like a virtual row "above" the grid with a single path into the start. Then the same rule fills row 0 correctly, obstacles included.

### 3.2 `else if (c > 0)`

Column 0 has no left neighbour. Its value just carries down from the row above (unless it's an obstacle, which sets it to 0, and it stays 0 for all the rows below).

### 3.3 Bounds and obstacles together (in the referee)

```java
if (r >= g.length || c >= g[0].length || g[r][c] == 1) return 0;
```

The `||` checks the bounds first, so `g[r][c]` is never read out of range.

---

## 4. Approach 1: Recursion from the start (the referee)

### Code ([M15_UniquePathsII.java:43-47](M15_UniquePathsII.java#L43-L47))

```java
static int brute(int[][] g, int r, int c) {
    if (r >= g.length || c >= g[0].length || g[r][c] == 1) return 0;
    if (r == g.length - 1 && c == g[0].length - 1) return 1;
    return brute(g, r + 1, c) + brute(g, r, c + 1);
}
```

| Line | What it does |
|---|---|
| the first check | off the grid or blocked: no paths |
| the second check | at the target: one path |
| the return | paths going down plus paths going right |

- **Time exponential.** The test uses grids up to 7 × 7.

---

## 5. Approach 2: One-row DP with obstacles (the file's main solution)

### Code ([M15_UniquePathsII.java:31-41](M15_UniquePathsII.java#L31-L41))

```java
static int uniquePathsWithObstacles(int[][] grid) {
    int n = grid[0].length;
    int[] dp = new int[n];
    dp[0] = 1;                                        // "one way to be at the start" (killed if it's blocked)
    for (int[] row : grid)
        for (int c = 0; c < n; c++) {
            if (row[c] == 1) dp[c] = 0;               // obstacle: no path ends here
            else if (c > 0) dp[c] += dp[c - 1];       // above + left
        }
    return dp[n - 1];
}
```

### Line by line

| Line | What it does |
|---|---|
| `dp[0] = 1` | the start, if it's open |
| the obstacle case | no path ends on a blocked cell |
| the open case | above (the old dp[c]) + left (the new dp[c − 1]) |
| the return | the bottom-right cell |

If the start itself is blocked, row 0 sets dp[0] = 0 and everything stays 0.

### Complexity
- **Time O(m · n), space O(n).**

---

## 6. Comparing the approaches

| | Recursion | One-row DP |
|---|---|---|
| Time | exponential | **O(m · n)** |
| Space | O(m + n) stack | O(n) |

The file checks the DP against the recursion on 500 random grids (about 20% obstacles).

---

## 7. Edge cases and common mistakes

**Edge cases:** a blocked start or end (0), a single cell, a fully blocked row or column (0), an obstacle in the first row or column.

**Mistakes:**
1. Initialising the first row/column to all 1s.
2. Forgetting that a blocked start means 0.
3. Using the combinatorics formula.

---

## 8. Interview questions and answers

**Q1. Why does dp[0] = 1 before the first row work?**
> It's like a virtual row above the grid with exactly one path coming down into the start. Then row 0 follows the same rule as every other row, and obstacles in it automatically block everything to their right.

**Q2. What if obstacles could also be on the path counts' "edges" (blocked moves)?**
> Only add the above or left term when that specific move is allowed.

### Follow-up questions

**F1. Minimum Path Sum.** → [M16](M16_MinimumPathSum.md)

**F2. Unique Paths III (LC 980): visit every empty cell exactly once.**
> No longer a DP over cells: backtracking (or a bitmask DP), since the order of visits matters.

**F3. Dungeon Game.** → [H07](../04-Hard/H07_DungeonGame.md)
> A grid DP that has to run backwards from the goal.

---

## 9. Running the file

```bash
java M15_UniquePathsII.java      # 5/5 tests passed (including 500 random grids vs recursion)
```

**One-line summary:** *"The Unique Paths recurrence with obstacles set to 0: one array, dp[0] = 1, and for every row dp[c] = 0 on an obstacle or dp[c] += dp[c − 1] otherwise."*
