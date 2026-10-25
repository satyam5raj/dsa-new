# M07 · Rotting Oranges

| | |
|---|---|
| **LeetCode** | [#994 Rotting Oranges](https://leetcode.com/problems/rotting-oranges/) |
| **Difficulty** | Medium |
| **Pattern** | Multi-source BFS: all rotten oranges start in the queue at minute 0 |
| **Code** | [M07_RottingOranges.java](M07_RottingOranges.java) |
| **Run** | `java M07_RottingOranges.java` |

BFS basics are explained in [E01](../02-Easy/E01_FindIfPathExists.md#34-bfs-breadth-first-search-with-a-queue); the level-size snapshot in [Trees M01](../../07-Trees/03-Medium/M01_BinaryTreeLevelOrderTraversal.md).

---

## 1. The problem

0 = empty, 1 = fresh, 2 = rotten. Every minute, the fresh oranges next to a rotten one (in 4 directions) rot. How many minutes until no fresh orange remains, or −1 if that's impossible?

| grid | answer |
|---|---|
| `[[2,1,1],[1,1,0],[0,1,1]]` | 4 |
| `[[2,1,1],[0,1,1],[1,0,1]]` | −1 (the bottom-left 1 is cut off) |
| `[[0,2]]` | 0 (no fresh oranges at all) |
| `[[1]]` | −1 (fresh, but nothing rotten) |

---

## 2. How to think about it

### The wrong idea
A BFS from each rotten orange separately, then combining the results: messy and slow.

### Multi-source BFS
The rot spreads from **all** the rotten oranges **simultaneously**, one ring per minute. That's exactly a BFS whose queue **starts** with every source:

```
queue = all rotten cells;  fresh = the number of 1s
minutes = 0
while the queue isn't empty and fresh > 0:
    process one LEVEL (a size snapshot) = one minute:
        rot each fresh neighbour, fresh−−, enqueue it
    minutes++
return fresh == 0 ? minutes : −1
```

Each BFS level is one minute (the same level-size snapshot as tree level-order traversal).

### Dry run: `[[2,1,1],[1,1,0],[0,1,1]]` (from the real run)

| minute | newly rotten | fresh left |
|---|---|---|
| start | | 6 |
| 1 | (1,0), (0,1) | 4 |
| 2 | (1,1), (0,2) | 2 |
| 3 | (2,1) | 1 |
| 4 | (2,2) | 0 → **4** |

---

## 3. Java concepts used in this problem

### 3.1 Seeding the queue with several starts

```java
for (...) if (grid[r][c] == 2) q.add(new int[]{r, c});      // ALL sources start together
```

Any number of sources can be in the queue at distance 0. BFS then naturally computes the distance from the **nearest** source for every cell.

### 3.2 The level loop: `for (int i = q.size(); i > 0; i--)`

`q.size()` is read once at the start of the minute, so only the oranges that were rotten at the start of this minute spread now. The ones they infect are added for the next minute.

### 3.3 `fresh > 0` in the loop condition

It stops as soon as everything has rotted. Without it, the last level would add an extra (empty) minute.

### 3.4 Changing the grid as "visited"

`grid[r][c] = 2` marks the orange as rotten **when it's queued**, so it can't be queued twice.

---

## 4. Approach 1: Simulate minute by minute (the referee)

### Code ([M07_RottingOranges.java:73-87](M07_RottingOranges.java#L73-L87))

```java
static int simulate(int[][] g) {
    int R = g.length, C = g[0].length;
    for (int minute = 0; ; minute++) {
        boolean anyFresh = false;
        List<int[]> toRot = new ArrayList<>();
        for (int r = 0; r < R; r++) for (int c = 0; c < C; c++) {
            if (g[r][c] != 1) continue;
            anyFresh = true;
            if ((r > 0 && g[r - 1][c] == 2) || (r < R - 1 && g[r + 1][c] == 2) || (c > 0 && g[r][c - 1] == 2) || (c < C - 1 && g[r][c + 1] == 2)) toRot.add(new int[]{r, c});
        }
        if (!anyFresh) return minute;
        if (toRot.isEmpty()) return -1;
        for (int[] p : toRot) g[p[0]][p[1]] = 2;
    }
}
```

| Line | What it does |
|---|---|
| `for (int minute = 0; ; minute++)` | an infinite loop that ends with a `return` |
| the scan | find every fresh orange next to a rotten one |
| `toRot` | rot them **after** the scan, so this minute's new rot doesn't spread within the same minute |
| `!anyFresh` | done |
| `toRot.isEmpty()` | fresh oranges remain, but nothing can spread → −1 |

Each minute rescans the whole grid: **O((R · C)²)** worst case.

---

## 5. Approach 2: Multi-source BFS (the file's main solution)

### Code ([M07_RottingOranges.java:44-70](M07_RottingOranges.java#L44-L70))

```java
static int orangesRotting(int[][] grid) {
    int R = grid.length, C = grid[0].length, fresh = 0;
    Queue<int[]> q = new ArrayDeque<>();
    for (int r = 0; r < R; r++) {
        for (int c = 0; c < C; c++) {
            if (grid[r][c] == 2) q.add(new int[]{r, c});      // ALL sources start together
            else if (grid[r][c] == 1) fresh++;
        }
    }
    int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    int minutes = 0;
    while (!q.isEmpty() && fresh > 0) {
        for (int i = q.size(); i > 0; i--) {                 // one BFS level = one minute
            int[] p = q.poll();
            for (int[] d : dirs) {
                int r = p[0] + d[0], c = p[1] + d[1];
                if (r >= 0 && r < R && c >= 0 && c < C && grid[r][c] == 1) {
                    grid[r][c] = 2;
                    fresh--;
                    q.add(new int[]{r, c});
                }
            }
        }
        minutes++;
    }
    return fresh == 0 ? minutes : -1;
}
```

### Line by line

| Line | What it does |
|---|---|
| the first scan | queue every rotten orange; count the fresh ones |
| `while (!q.isEmpty() && fresh > 0)` | keep spreading while something can spread and something is left to rot |
| the level loop | one minute's worth of spreading |
| `grid[r][c] == 1` | a fresh neighbour → it rots now |
| `fresh--` | track what's left |
| `minutes++` | one minute has passed |
| `fresh == 0 ? minutes : −1` | everything rotted, or some oranges are unreachable |

### Dry run: `[[1]]`
No rotten oranges → the queue is empty → the loop never runs → fresh = 1 → **−1** ✓

### Complexity
- **Time O(R · C):** each cell is queued at most once.
- **Space O(R · C).**

---

## 6. Comparing the approaches

| | Minute-by-minute simulation | Multi-source BFS |
|---|---|---|
| Time | O((R · C)²) | **O(R · C)** |
| Space | O(R · C) | O(R · C) |

The file checks the BFS against the simulation on 1,000 random grids.

---

## 7. Edge cases and common mistakes

**Edge cases:** no fresh oranges (0, even with no rotten ones), fresh oranges but no rotten ones (−1), isolated fresh oranges (−1).

**Mistakes:**
1. Counting one minute too many (missing `fresh > 0` in the loop condition).
2. A separate BFS per rotten orange.
3. Marking an orange rotten when it's polled instead of when it's queued.
4. Returning 0 for a grid with fresh oranges and nothing rotten.

---

## 8. Interview questions and answers

**Q1. Why does the multi-source BFS give the right time?**
> BFS from a set of sources computes each cell's distance to the **nearest** source. An orange rots at exactly that distance (in minutes), and the answer is the largest such distance.

**Q2. What does each BFS level represent?**
> One minute: the oranges infected during that minute.

### Follow-up questions

**F1. 01 Matrix (LC 542): the distance to the nearest 0 for every cell.**
> A multi-source BFS from all the zeros.

**F2. Walls and Gates (LC 286).**
> A multi-source BFS from all the gates, filling in the distances.

**F3. As Far from Land as Possible (LC 1162).**
> A multi-source BFS from all the land cells; the answer is the last level reached.

---

## 9. Running the file

```bash
java M07_RottingOranges.java      # 5/5 tests passed (including 1,000 random grids vs minute-by-minute)
```

**One-line summary:** *"Put every rotten orange in the queue at minute 0 and BFS level by level (one level = one minute), counting the fresh oranges down; −1 if some remain."*
