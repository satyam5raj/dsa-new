# A10 · Trapping Rain Water II ⭐

| | |
|---|---|
| **LeetCode** | [#407 Trapping Rain Water II](https://leetcode.com/problems/trapping-rain-water-ii/) |
| **Difficulty** | Hard |
| **Mix of structures** | **Min-heap** of boundary cells + **BFS** inward on a grid (a Dijkstra-like flood) |
| **Code** | [A10_TrappingRainWaterII.java](A10_TrappingRainWaterII.java) |
| **Run** | `java A10_TrappingRainWaterII.java` |

⭐ = not in `leetcode.md`, added because it combines a heap with grid BFS. It's the 2-D version of [Trapping Rain Water](../../01-Arrays/04-Hard/H01_TrappingRainWater.md).

---

## 1. The problem

`heightMap` is an `m × n` grid of bar heights. After it rains, how much water is trapped?

```
[[1,4,3,1,3,2],
 [3,2,1,3,2,4],
 [2,3,3,2,3,1]]          →  4

[[3,3,3,3,3],
 [3,2,2,2,3],
 [3,2,1,2,3],            →  10   (a bowl: eight 2s hold 1 each, the 1 holds 2)
 [3,2,2,2,3],
 [3,3,3,3,3]]
```

---

## 2. How to think about it

### Why the 1-D formula doesn't carry over
In 1-D, water at i = `min(tallest on the left, tallest on the right) − h[i]`. In 2-D, water can leak out **in any direction, along any winding path**. Looking at the row and column isn't enough: a low gap anywhere around the edge lets the water out.

### The right question
A cell's water level is the **lowest possible "highest wall"** on a path from that cell to the outside. Water escapes over the weakest point of whatever surrounds it.

### Flood from the outside in
Think of the water rising from the outside:
1. Every **border cell** is part of the wall: water spills off the edge. Push them all into a **min-heap** by height.
2. Pop the **lowest** wall cell. It's the weakest point: the water level anywhere it leads can't be higher.
3. For each unvisited neighbour:
   - if it's **lower** than the current level, it fills with water up to that level: `water += level − height`;
   - either way it joins the wall at height `max(level, its height)`, so push it.

The heap always expands from the lowest wall first, exactly like **Dijkstra** always expands the closest node, so each cell's level is final when it's reached.

### Dry run: example 1 (from the real run)
```
[[1,4,3,1,3,2],
 [3,2,1,3,2,4],        inner cells: (1,1)=2  (1,2)=1  (1,3)=3  (1,4)=2
 [2,3,3,2,3,1]]
```
| pop (level) | unvisited neighbour | water added | pushed level |
|---|---|---|---|
| (0,3) level 1 | (1,3) h3 | 0 | 3 |
| border cells at levels 1 and 2 | (none unvisited) | | |
| (2,1) level 3 | (1,1) h2 | **+1** | 3 |
| (1,3) level 3 | (1,4) h2 | **+1** | 3 |
| | (1,2) h1 | **+2** | 3 |
| everything else | only visited neighbours | | |

Total **4** ✓

---

## 3. Java concepts used in this problem

### 3.1 `PriorityQueue<int[]>` ordered by one field
```java
PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> Integer.compare(a[2], b[2]));   // {r, c, level}
```
Each entry is `{row, col, level}`; the comparator looks only at `level`, so the lowest wall comes out first.

### 3.2 Marking visited when pushing
`visited[nr][nc] = true` happens **before** the push, so each cell enters the heap exactly once. The border cells are marked at the start.

### 3.3 `Math.max(0, x)`
`water += Math.max(0, level − height)` adds nothing when the neighbour is taller than the current level.

---

## 4. Approach 1: Relax water levels until nothing changes (the referee)

### Code ([A10_TrappingRainWaterII.java:49-69](A10_TrappingRainWaterII.java#L49-L69))
```java
static int trapRainWaterRelax(int[][] h) {
    int m = h.length, n = h[0].length;
    int[][] level = new int[m][n];
    for (int r = 0; r < m; r++)
        for (int c = 0; c < n; c++)
            level[r][c] = (r == 0 || c == 0 || r == m - 1 || c == n - 1) ? h[r][c] : Integer.MAX_VALUE;
    boolean changed = true;
    while (changed) {                                                // like Bellman-Ford
        changed = false;
        for (int r = 1; r < m - 1; r++)
            for (int c = 1; c < n - 1; c++) {
                int lowestNeighbour = Integer.MAX_VALUE;
                for (int[] d : DIRS) lowestNeighbour = Math.min(lowestNeighbour, level[r + d[0]][c + d[1]]);
                int want = Math.max(h[r][c], lowestNeighbour);
                if (want < level[r][c]) { level[r][c] = want; changed = true; }
            }
    }
    int water = 0;
    for (int r = 0; r < m; r++) for (int c = 0; c < n; c++) water += level[r][c] - h[r][c];
    return water;
}
```

| Line | What it does |
|---|---|
| initial `level` | border cells hold no water (level = height); inner cells start "infinitely full" |
| the rule | a cell's level is at least its own height, and at most the lowest neighbouring level, because water would flow out there |
| repeat until stable | levels only go down, so this terminates |
| the sum | water = level − height everywhere |

It's a different method from the heap (repeated relaxation, like Bellman-Ford), which makes it a good independent check. It can take many passes: about **O((m n)²)** in the worst case.

---

## 5. Approach 2: Min-heap from the border inward (main solution)

### Code ([A10_TrappingRainWaterII.java:72-95](A10_TrappingRainWaterII.java#L72-L95))
```java
static int trapRainWater(int[][] heightMap) {
    int m = heightMap.length, n = heightMap[0].length;
    if (m < 3 || n < 3) return 0;
    boolean[][] visited = new boolean[m][n];
    PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> Integer.compare(a[2], b[2]));   // {r, c, level}
    for (int r = 0; r < m; r++)
        for (int c = 0; c < n; c++)
            if (r == 0 || c == 0 || r == m - 1 || c == n - 1) {
                heap.add(new int[]{r, c, heightMap[r][c]});
                visited[r][c] = true;
            }
    int water = 0;
    while (!heap.isEmpty()) {
        int[] cell = heap.poll();                                     // the weakest point of the wall
        for (int[] d : DIRS) {
            int nr = cell[0] + d[0], nc = cell[1] + d[1];
            if (nr < 0 || nr >= m || nc < 0 || nc >= n || visited[nr][nc]) continue;
            visited[nr][nc] = true;
            water += Math.max(0, cell[2] - heightMap[nr][nc]);        // fills up to the wall's level
            heap.add(new int[]{nr, nc, Math.max(cell[2], heightMap[nr][nc])});
        }
    }
    return water;
}
```

### Line by line
| Line | What it does |
|---|---|
| `m < 3 \|\| n < 3` | every cell is on the border, so nothing can be held |
| push all border cells | the initial wall |
| `poll()` | the lowest point of the current wall: water can't stand higher than this anywhere it leads |
| `water += max(0, level − h)` | a lower neighbour fills up to the wall's level |
| push `max(level, h)` | the neighbour becomes part of the wall: either as water at `level` or as a taller bar |

### Why it's correct (the Dijkstra argument)
When a cell is popped with level L, every unvisited cell is reachable from the outside only through wall cells of level ≥ L (anything lower would have been popped first). So the neighbour's water level is exactly `max(L, its height)`.

### Complexity
- Each cell is pushed and popped once: **O(m n log(m n))**.
- Space **O(m n)**.

---

## 6. Comparing the approaches

| | Time | Idea |
|---|---|---|
| Relaxation | up to O((m n)²) | Bellman-Ford style |
| **Heap flood** | **O(m n log(m n))** | Dijkstra style |

The file checks both on 1,500 random height maps.

---

## 7. Edge cases and common mistakes

**Edge cases:** fewer than 3 rows or columns (0), a bowl (all water held), a single low gap in the border (everything leaks to that level), flat ground.

**Mistakes:**
1. Applying the 1-D formula per row and per column (water leaks diagonally around walls).
2. Pushing the neighbour with its own height instead of `max(level, height)`, which lets later water "leak" through a filled cell.
3. Forgetting to mark border cells visited.
4. A max-heap instead of a min-heap (you must always expand the **lowest** wall).

---

## 8. Interview questions and answers

**Q1. Why a min-heap?**
> Water escapes over the lowest point of its surrounding wall. Always expanding from the lowest wall cell means each inner cell is reached through the weakest possible boundary, so its water level is decided correctly the first time.

**Q2. How is this like Dijkstra?**
> Dijkstra pops the closest node and finalises its distance; here we pop the lowest wall and finalise the level of its neighbours. The "distance" of a path is its highest wall (a minimax path), not the sum of edges.

**Q3. Why push `max(level, height)`?**
> A filled cell acts like a wall at the water's level; a taller cell acts like a wall at its own height.

### Follow-up questions

**F1. "Which cells hold water?"**
> Record `level − height` per cell during the flood.

**F2. "Water drains through holes (cells that leak to the ground)."**
> Treat holes like border cells: push them into the heap at the start.

**F3. "Similar minimax paths."**
> [Swim in Rising Water](../../09-Graph/04-Hard/H06_SwimInRisingWater.md) and [Path With Minimum Effort](../../09-Graph/03-Medium/M14_PathWithMinimumEffort.md) use the same "minimise the maximum along a path" Dijkstra.

---

## 9. Running the file

```bash
java A10_TrappingRainWaterII.java     # 5/5 tests passed (two examples, a leak, a thin map, 1,500 random maps)
```

**One-line summary:** *"Push all border cells into a min-heap; repeatedly pop the lowest wall cell, fill each unvisited neighbour up to that level (adding the difference as water), and push the neighbour with height max(level, its height)."*
