# E05 · Island Perimeter ⭐

| | |
|---|---|
| **LeetCode** | [#463 Island Perimeter](https://leetcode.com/problems/island-perimeter/) |
| **Difficulty** | Easy |
| **Pattern** | Grid counting: each land cell adds 4, each shared edge removes 2 |
| **Code** | [E05_IslandPerimeter.java](E05_IslandPerimeter.java) |
| **Run** | `java E05_IslandPerimeter.java` |

⭐ = not in `leetcode.md`, added for a key technique. The grid-as-graph idea is explained in [E02](E02_FloodFill.md).

---

## 1. The problem

A grid of 1 (land) and 0 (water) contains exactly one island (and no lakes inside it). Return the island's perimeter.

```
0 1 0 0
1 1 1 0          → 16
0 1 0 0
1 1 0 0
```

| grid | answer |
|---|---|
| the example | 16 |
| `[[1]]` | 4 |
| `[[1,0]]` | 4 |

---

## 2. How to think about it

### Counting
Every land cell has 4 sides. Each pair of **adjacent** land cells hides 2 sides (one from each). So:

```
perimeter = 4 × landCells − 2 × adjacentPairs
```

Count each pair once by looking only **up** and **left** from each land cell (the pair will never be counted again from the other cell's point of view).

```
the example: 7 land cells → 28;  adjacent pairs: 6  →  28 − 12 = 16
pairs: (0,1)-(1,1)  (1,0)-(1,1)  (1,1)-(1,2)  (1,1)-(2,1)  (2,1)-(3,1)  (3,0)-(3,1)
```

### The DFS version (the grid-graph way)
Walk the island; every step that leaves the grid or lands in water crosses one edge of the perimeter, so count it.

Both are O(rows × cols); the counting version needs no recursion.

---

## 3. Java concepts used in this problem

### 3.1 `continue` to skip water

```java
if (grid[r][c] != 1) continue;
```

It jumps to the next cell, keeping the land-cell logic unindented.

### 3.2 Bounds-checked neighbour tests

```java
if (r > 0 && grid[r - 1][c] == 1) shared++;      // neighbour above
```

`r > 0` is checked first, so `grid[r − 1]` is never read for the top row.

### 3.3 A recursive function that returns a count

```java
return dfs(down) + dfs(up) + dfs(right) + dfs(left);
```

Each call returns how many perimeter edges it found: 1 for stepping into water or off the grid, 0 for an already-seen cell, otherwise the sum from its 4 neighbours.

---

## 4. Approach 1: DFS counting crossed edges

### Code ([E05_IslandPerimeter.java:49-62](E05_IslandPerimeter.java#L49-L62))

```java
static int dfsPerimeter(int[][] grid) {
    boolean[][] seen = new boolean[grid.length][grid[0].length];
    for (int r = 0; r < grid.length; r++)
        for (int c = 0; c < grid[0].length; c++)
            if (grid[r][c] == 1) return dfs(grid, r, c, seen);
    return 0;
}

static int dfs(int[][] g, int r, int c, boolean[][] seen) {
    if (r < 0 || r >= g.length || c < 0 || c >= g[0].length || g[r][c] == 0) return 1;   // crossed an edge
    if (seen[r][c]) return 0;
    seen[r][c] = true;
    return dfs(g, r + 1, c, seen) + dfs(g, r - 1, c, seen) + dfs(g, r, c + 1, seen) + dfs(g, r, c - 1, seen);
}
```

### Line by line

| Line | What it does |
|---|---|
| the outer loops | find any land cell to start from (there's only one island) |
| out of bounds or water → `return 1` | we just crossed one side of the island's boundary |
| `seen → return 0` | an already-counted land cell |
| `seen[r][c] = true` | mark it (the grid itself isn't modified) |
| the sum of 4 calls | all the boundary edges reachable from here |

**Dry run** `[[1, 0]]`: dfs(0, 0): down → off the grid (1), up → off (1), right → water (1), left → off (1) → **4** ✓

**O(R · C)** time; O(R · C) recursion depth in the worst case.

---

## 5. Approach 2: Count cells and shared edges (the file's main solution)

### Code ([E05_IslandPerimeter.java:36-47](E05_IslandPerimeter.java#L36-L47))

```java
static int islandPerimeter(int[][] grid) {
    int land = 0, shared = 0;
    for (int r = 0; r < grid.length; r++) {
        for (int c = 0; c < grid[0].length; c++) {
            if (grid[r][c] != 1) continue;
            land++;
            if (r > 0 && grid[r - 1][c] == 1) shared++;      // neighbour above
            if (c > 0 && grid[r][c - 1] == 1) shared++;      // neighbour to the left
        }
    }
    return 4 * land - 2 * shared;
}
```

### Line by line

| Line | What it does |
|---|---|
| `land++` | every land cell contributes 4 sides |
| the "above" check | a vertical pair, counted once (from the lower cell) |
| the "left" check | a horizontal pair, counted once (from the right cell) |
| `4 * land − 2 * shared` | each shared edge removes one side from each of its two cells |

### Dry run: the example

| cell | land | above is land? | left is land? | shared so far |
|---|---|---|---|---|
| (0, 1) | 1 | (top row) | 0 | 0 |
| (1, 0) | 2 | 0 | (left edge) | 0 |
| (1, 1) | 3 | ✓ (0, 1) | ✓ (1, 0) | 2 |
| (1, 2) | 4 | 0 | ✓ (1, 1) | 3 |
| (2, 1) | 5 | ✓ (1, 1) | 0 | 4 |
| (3, 0) | 6 | 0 | (left edge) | 4 |
| (3, 1) | 7 | ✓ (2, 1) | ✓ (3, 0) | 6 |

4 × 7 − 2 × 6 = **16** ✓

### Complexity
- **Time O(R · C).**
- **Space O(1).**

---

## 6. Comparing the approaches

| | DFS crossing edges | Counting |
|---|---|---|
| Time | O(R · C) | O(R · C) |
| Space | O(R · C) (seen + recursion) | **O(1)** |
| Handles several islands? | one island per DFS | the total perimeter of all of them |

The file checks both on 500 random islands grown by a random walk.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single cell (4), a full grid (2(R + C)), a 1-row or 1-column strip.

**Mistakes:**
1. Counting each shared edge twice (checking all 4 directions) and subtracting only 1 per check.
2. Forgetting the bounds checks on the top row and the left column.
3. In the DFS, returning 1 for an already-seen cell.

---

## 8. Interview questions and answers

**Q1. Why subtract 2 per shared edge?**
> A shared edge would be counted once in each of its two cells' 4 sides, but it isn't part of the boundary, so both counts are removed.

**Q2. What if lakes (water fully inside the island) were allowed?**
> Both methods count the lake's shoreline as perimeter too, since the cells next to the lake have water sides. If only the outer boundary were wanted, you'd first flood-fill the water from the border to identify the lakes.

### Follow-up questions

**F1. Max area of an island (LC 695).**
> DFS from each land cell, returning 1 + the sum over its neighbours, and take the max.

**F2. Number of Islands.** → [M01](../03-Medium/M01_NumberOfIslands.md)
> Count the DFS starts.

**F3. The perimeter in 3D: the surface area of a stack of cubes (LC 892).**
> The same idea: 6 faces per cube (+2 per non-empty stack for top and bottom), minus the hidden faces between neighbouring stacks.

---

## 9. Running the file

```bash
java E05_IslandPerimeter.java      # 5/5 tests passed (including 500 random islands: counting == DFS)
```

**One-line summary:** *"perimeter = 4 × land cells − 2 × adjacent land pairs, counting each pair once by looking only up and left."*
