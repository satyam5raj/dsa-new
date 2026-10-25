# H07 · Dungeon Game

| | |
|---|---|
| **LeetCode** | [#174 Dungeon Game](https://leetcode.com/problems/dungeon-game/) |
| **Difficulty** | Hard |
| **Pattern** | Grid DP filled **backwards** (from the goal), because the constraint is a running minimum, not a sum |
| **Code** | [H07_DungeonGame.java](H07_DungeonGame.java) |
| **Run** | `java H07_DungeonGame.java` |

Forward grid DP is explained in [M16 Minimum Path Sum](../03-Medium/M16_MinimumPathSum.md).

---

## 1. The problem

A knight starts at the top-left and moves right or down to the princess at the bottom-right. Each cell adds health (or removes it, if negative). The knight's health must stay **≥ 1 at all times**. What's the minimum starting health?

| dungeon | answer |
|---|---|
| [[−2,−3,3],[−5,−10,1],[10,30,−5]] | 7 (right, right, down, down) |
| [[0]] | 1 |
| [[100]] | 1 (a potion doesn't lower the requirement below 1) |
| [[−5]] | 6 |

---

## 2. How to think about it

### Why forward DP fails
Going forward, each cell would need **two** numbers: the current health and the lowest point reached so far. Minimising one can hurt the other, so there's no single "best" value per cell.

### Go backwards
What matters at a cell is how much health you need when you **enter** it, and that depends only on the path **ahead**.

| step | for this problem |
|---|---|
| **State** | need[r][c] = the minimum health when entering (r, c) to survive to the end |
| **Transition** | next = min(need[r + 1][c], need[r][c + 1]) (take the easier way on) |
| | need[r][c] = max(1, next − dungeon[r][c]) |
| **Base** | "after" the princess you need 1, so need[m − 1][n − 1] = max(1, 1 − dungeon[m − 1][n − 1]) |
| **Answer** | need[0][0] |

The `max(1, …)` matters: a big potion can't "bank" negative need. You still must enter the cell with at least 1.

### Dry run (filled from the bottom-right)

| dungeon | | | | need | | |
|---|---|---|---|---|---|---|
| −2 | −3 | 3 | | **7** | 5 | 2 |
| −5 | −10 | 1 | | 6 | 11 | 5 |
| 10 | 30 | −5 | | 1 | 1 | 6 |

- (2,2): max(1, 1 − (−5)) = 6.
- (2,1): next = 6 (right); max(1, 6 − 30) = 1. (2,0): next = 1; max(1, 1 − 10) = 1.
- (1,2): next = 6 (below); 6 − 1 = 5. (1,1): next = min(1, 5) = 1; 1 + 10 = 11. (1,0): next = min(1, 11) = 1; 1 + 5 = 6.
- (0,2): next = 5; 5 − 3 = 2. (0,1): next = min(11, 2) = 2; 2 + 3 = 5. (0,0): next = min(6, 5) = 5; 5 + 2 = **7**.

---

## 3. Java concepts used in this problem

### 3.1 One row, filled right to left and bottom to top

```java
int[] need = new int[n + 1];
Arrays.fill(need, Integer.MAX_VALUE);            // off-grid: impossible
need[n - 1] = 1;                                  // "after" the princess you need 1 (seeds the corner)
```

- `need[n]` stays `MAX_VALUE` forever: there's nothing to the right of the last column.
- Before the bottom row is processed, the other cells are `MAX_VALUE` (nothing below the grid), except `need[n − 1] = 1`, the virtual cell after the princess.

### 3.2 "Below" and "right" in the same array

```java
int next = Math.min(need[c], need[c + 1]);       // below (old need[c]) or right (new need[c+1])
```

Going right to left, `need[c]` still holds the row below, and `need[c + 1]` has already been updated for the current row.

### 3.3 Why `MAX_VALUE − dungeon` is safe

`next` is never `MAX_VALUE` in practice: every cell has at least one real way on (right or down), so `min` always picks a real value. That avoids overflow in `next − d[r][c]`.

---

## 4. Approach 1: Every path (the referee)

### Code ([H07_DungeonGame.java:57-65](H07_DungeonGame.java#L57-L65))

```java
static int brute(int[][] d, int r, int c, int sum, int lowest) {
    sum += d[r][c];
    lowest = Math.min(lowest, sum);
    if (r == d.length - 1 && c == d[0].length - 1) return Math.max(1, 1 - lowest);
    int best = Integer.MAX_VALUE;
    if (r + 1 < d.length) best = Math.min(best, brute(d, r + 1, c, sum, lowest));
    if (c + 1 < d[0].length) best = Math.min(best, brute(d, r, c + 1, sum, lowest));
    return best;
}
```

For one path, the starting health must make every prefix sum stay ≥ 1: start = max(1, 1 − the lowest prefix sum). The referee tries every path. **Exponential.**

---

## 5. Approach 2: Backward DP (the file's main solution)

### Code ([H07_DungeonGame.java:43-54](H07_DungeonGame.java#L43-L54))

```java
static int calculateMinimumHP(int[][] d) {
    int m = d.length, n = d[0].length;
    int[] need = new int[n + 1];
    Arrays.fill(need, Integer.MAX_VALUE);            // off-grid: impossible
    need[n - 1] = 1;                                  // "after" the princess you need 1 (seeds the corner)
    for (int r = m - 1; r >= 0; r--)
        for (int c = n - 1; c >= 0; c--) {
            int next = Math.min(need[c], need[c + 1]);          // below (old need[c]) or right (new need[c+1])
            need[c] = Math.max(1, next - d[r][c]);
        }
    return need[0];
}
```

| Line | What it does |
|---|---|
| the setup | off-grid is impossible; after the princess you need 1 |
| the loops | from the bottom-right corner back to the top-left |
| `next` | the cheaper of the two ways forward |
| `need[c]` | enter with enough to pay this cell and still have `next`, but never below 1 |

### Complexity
- **Time O(m · n), space O(n).**

---

## 6. Comparing the approaches

| | Every path | Backward DP |
|---|---|---|
| Time | exponential | **O(m · n)** |
| Single value per cell? | no (needs the path's history) | yes |

The file checks the DP against every path on 500 random dungeons.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single cell (positive → 1, negative x → 1 − x), all-positive dungeons (1), very negative cells.

**Mistakes:**
1. Forward DP with a single value.
2. Forgetting `max(1, …)` (a potion makes the need look ≤ 0).
3. Computing the minimum path sum and returning 1 − sum (that ignores the "≥ 1 at all times" rule along the way).

---

## 8. Interview questions and answers

**Q1. Why go backwards?**
> The requirement at a cell depends only on what's ahead of it. Going backwards, each cell has a single meaningful value (the minimum health needed on entry), so the DP works.

**Q2. Why not binary-search the starting health?**
> You could: for a guessed start, a forward DP of "max health possible at each cell while staying ≥ 1" checks feasibility. That's O(m · n · log range), slower than the backward DP but a valid alternative.

### Follow-up questions

**F1. Cherry Pickup (LC 741).**
> Another grid problem where the forward/backward choice matters; it uses two simultaneous walkers.

**F2. Allow moving in four directions.**
> No longer a DAG: binary search on the start plus BFS/Dijkstra-style checking.

**F3. Minimum Path Sum.** → [M16](../03-Medium/M16_MinimumPathSum.md)

---

## 9. Running the file

```bash
java H07_DungeonGame.java      # 5/5 tests passed (including 500 random dungeons vs every path)
```

**One-line summary:** *"Fill from the princess backwards: need = max(1, min(need below, need right) − cell), seeded with 1 after the goal; the answer is need at the start."*
