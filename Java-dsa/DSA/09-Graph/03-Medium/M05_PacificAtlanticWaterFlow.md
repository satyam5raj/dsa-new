# M05 · Pacific Atlantic Water Flow

| | |
|---|---|
| **LeetCode** | [#417 Pacific Atlantic Water Flow](https://leetcode.com/problems/pacific-atlantic-water-flow/) |
| **Difficulty** | Medium |
| **Pattern** | Reverse the direction: search **inward from the oceans** (multi-source) |
| **Code** | [M05_PacificAtlanticWaterFlow.java](M05_PacificAtlanticWaterFlow.java) |
| **Run** | `java M05_PacificAtlanticWaterFlow.java` |

The grid DFS template is in [E02 Flood Fill](../02-Easy/E02_FloodFill.md); graph basics in [E01](../02-Easy/E01_FindIfPathExists.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

A grid of heights. The **Pacific** touches the top and left edges; the **Atlantic** touches the bottom and right edges. Water flows from a cell to a 4-directional neighbour whose height is **≤** its own. Return the cells from which water can reach **both** oceans.

```
        Pacific
     1  2  2  3  5
  P  3  2  3  4  4  A
  a  2  4  5  3  1  t
  c  6  7  1  4  5  l
     5  1  1  2  4
        Atlantic

→ [[0,4],[1,3],[1,4],[2,2],[3,0],[3,1],[4,0]]
```

---

## 2. How to think about it

### Brute force
From every cell, DFS **downhill** and see which oceans you hit: O((R · C)²), since each cell may explore the whole grid.

### Think in reverse
Instead of asking "where can water from this cell go?", ask "**which cells can reach the Pacific?**" Start at **all** the Pacific border cells at once and walk **uphill** (to neighbours with height ≥ the current one): every cell you reach can drain into the Pacific. Do the same for the Atlantic. The answer is the cells in **both** sets.

Two traversals: **O(R · C)** in total.

### A small example
```
heights        reach (P = Pacific, A = Atlantic)
1 2 3          P.  P.  PA
8 9 4          PA  PA  PA
7 6 5          PA  PA  PA
```

(0, 0) holds 1, and its neighbours (2 and 8) are both higher, so its water can't flow anywhere: it reaches only the Pacific (it's on that edge). (Traced by running the code.)

### The general trick
"Start from the targets and go backwards" is everywhere: Surrounded Regions ([M06](M06_SurroundedRegions.md), from the border), Rotting Oranges ([M07](M07_RottingOranges.md), from all the rotten oranges), 01 Matrix (from all the zeros), Walls and Gates.

---

## 3. Java concepts used in this problem

### 3.1 Two boolean grids, one per ocean

```java
boolean[][] pac = new boolean[R][C], atl = new boolean[R][C];
```

Each one doubles as that search's visited array.

### 3.2 Seeding a search from every border cell

```java
for (int r = 0; r < R; r++) { climb(h, r, 0, pac); climb(h, r, C - 1, atl); }   // left / right edges
for (int c = 0; c < C; c++) { climb(h, 0, c, pac); climb(h, R - 1, c, atl); }   // top / bottom edges
```

Calling the DFS from each border cell (it returns immediately for already-marked cells) is equivalent to one multi-source search.

### 3.3 The reversed comparison

Water flows downhill (`next ≤ current`); the reverse search walks **uphill**: `h[nr][nc] >= h[r][c]`. Getting this backwards is the classic bug.

### 3.4 `List.of(r, c)` for the output pairs

It's a compact immutable `List<Integer>` for each coordinate pair.

---

## 4. Approach 1: Flow downhill from every cell (the referee)

### Code ([M05_PacificAtlanticWaterFlow.java:59-82](M05_PacificAtlanticWaterFlow.java#L59-L82))

```java
static List<List<Integer>> brute(int[][] h) {
    int R = h.length, C = h[0].length;
    List<List<Integer>> out = new ArrayList<>();
    for (int r = 0; r < R; r++) {
        for (int c = 0; c < C; c++) {
            boolean[][] seen = new boolean[R][C];
            boolean[] oceans = new boolean[2];
            flow(h, r, c, seen, oceans);
            if (oceans[0] && oceans[1]) out.add(List.of(r, c));
        }
    }
    return out;
}

static void flow(int[][] h, int r, int c, boolean[][] seen, boolean[] oceans) {
    seen[r][c] = true;
    if (r == 0 || c == 0) oceans[0] = true;
    if (r == h.length - 1 || c == h[0].length - 1) oceans[1] = true;
    int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    for (int[] d : dirs) {
        int nr = r + d[0], nc = c + d[1];
        if (nr >= 0 && nr < h.length && nc >= 0 && nc < h[0].length && !seen[nr][nc] && h[nr][nc] <= h[r][c]) flow(h, nr, nc, seen, oceans);
    }
}
```

| Line | What it does |
|---|---|
| a fresh `seen` per cell | each start cell gets its own search |
| `oceans[0]` / `oceans[1]` | touched the top/left edge (Pacific) or the bottom/right edge (Atlantic) |
| `h[nr][nc] <= h[r][c]` | the water can flow there |

**O((R · C)²).** Correct by definition: the file's referee.

---

## 5. Approach 2: Climb uphill from both oceans (the file's main solution)

### Code ([M05_PacificAtlanticWaterFlow.java:35-56](M05_PacificAtlanticWaterFlow.java#L35-L56))

```java
static List<List<Integer>> pacificAtlantic(int[][] h) {
    int R = h.length, C = h[0].length;
    boolean[][] pac = new boolean[R][C], atl = new boolean[R][C];
    for (int r = 0; r < R; r++) { climb(h, r, 0, pac); climb(h, r, C - 1, atl); }   // left / right edges
    for (int c = 0; c < C; c++) { climb(h, 0, c, pac); climb(h, R - 1, c, atl); }   // top / bottom edges
    List<List<Integer>> result = new ArrayList<>();
    for (int r = 0; r < R; r++) for (int c = 0; c < C; c++) if (pac[r][c] && atl[r][c]) result.add(List.of(r, c));
    return result;
}

// mark every cell reachable by walking UPHILL (or level) from (r, c)
static void climb(int[][] h, int r, int c, boolean[][] reach) {
    if (reach[r][c]) return;
    reach[r][c] = true;
    int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    for (int[] d : dirs) {
        int nr = r + d[0], nc = c + d[1];
        if (nr >= 0 && nr < h.length && nc >= 0 && nc < h[0].length && !reach[nr][nc] && h[nr][nc] >= h[r][c]) {
            climb(h, nr, nc, reach);
        }
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| the border loops | every cell next to an ocean drains into it directly |
| `climb` | spread from there to every cell whose water could flow down into the visited region |
| `reach[r][c] = true` | this cell drains into that ocean |
| `h[nr][nc] >= h[r][c]` | the neighbour is at least as high, so its water can flow here |
| the final loop | the cells that drain into both oceans |

### Complexity
- **Time O(R · C):** each search visits each cell at most once.
- **Space O(R · C)** for the two grids and the recursion.

---

## 6. Comparing the approaches

| | Downhill from every cell | Uphill from the oceans |
|---|---|---|
| Time | O((R · C)²) | **O(R · C)** |
| Space | O(R · C) per search | O(R · C) |

The file checks the reverse search against the per-cell search on 300 random grids (with many equal heights).

---

## 7. Edge cases and common mistakes

**Edge cases:** a single cell (both oceans), a 1-row grid (every cell touches both), equal heights (flow is allowed both ways).

**Mistakes:**
1. `>` instead of `>=` (blocks flow between equal heights).
2. Climbing downhill instead of uphill in the reverse search.
3. Mixing up which edges belong to which ocean.
4. Re-running a full search from every cell.

---

## 8. Interview questions and answers

**Q1. Why search from the oceans instead of from each cell?**
> Each cell's search can cost O(R · C), giving O((R · C)²) in total. Searching backwards from the oceans visits each cell once per ocean.

**Q2. Why is the reversed edge condition `>=`?**
> Water moves from a to b when h[b] ≤ h[a]. Reversing the edge, we move from b to a when h[a] ≥ h[b].

### Follow-up questions

**F1. Diagonal flow too.**
> Add the 4 diagonal directions.

**F2. Return the cells reaching exactly one ocean.**
> `pac[r][c] ^ atl[r][c]`.

**F3. Trapping Rain Water II (LC 407).**
> Also "from the border inward", but with a min-heap (always process the lowest boundary cell next).

---

## 9. Running the file

```bash
java M05_PacificAtlanticWaterFlow.java      # 3/3 tests passed (including 300 random grids vs per-cell search)
```

**One-line summary:** *"Climb uphill (≥) from all the Pacific border cells and, separately, from all the Atlantic ones; the answer is the cells marked by both searches."*
