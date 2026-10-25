# A29 · Bricks Falling When Hit ⭐

| | |
|---|---|
| **LeetCode** | [#803 Bricks Falling When Hit](https://leetcode.com/problems/bricks-falling-when-hit/) |
| **Difficulty** | Hard |
| **Mix of structures** | **Offline, reverse-time processing** + **Union-Find** with sizes + a virtual **roof** node |
| **Code** | [A29_BricksFallingWhenHit.java](A29_BricksFallingWhenHit.java) |
| **Run** | `java A29_BricksFallingWhenHit.java` |

⭐ = not in `leetcode.md`, added because "reverse time so deletions become additions" is a beautiful trick that turns an impossible union-find problem into an easy one.

---

## 1. The problem

`grid` holds `1` (brick) and `0` (empty). A brick is **stable** if it's in the top row, or it's connected (up/down/left/right) to a stable brick. `hits[i]` is a cell to erase, applied in order. After each hit, every brick that's no longer stable **falls** (disappears). Return how many bricks fall after each hit; the erased brick itself doesn't count.

```
grid = [[1,0,0,0],
        [1,1,1,0]],  hits = [[1,0]]   →  [2]
   erasing (1,0) leaves (1,1) and (1,2) hanging: both fall

grid = [[1,0,0,0],
        [1,1,0,0]],  hits = [[1,1],[1,0]]   →  [0, 0]
   (1,1) hangs off (1,0), which is still attached; erasing it drops nothing.
   Then (1,0) has nothing hanging off it.
```

As in LeetCode's tests, every brick is stable before the first hit.

---

## 2. How to think about it

### Why the forward direction is hard
"Which bricks are still connected to the roof?" is a connectivity question, which is normally **union-find** territory. But a hit **cuts** connections, and union-find can only **join** groups, never split them. Doing it forwards means a BFS from the top after every hit: O(hits · m · n).

### Reverse time
Watch the film backwards: erasing bricks forwards = **adding** bricks backwards. Adding is exactly what union-find is good at.

1. **Jump to the end:** remove every hit brick from the grid.
2. **Build union-find** over the remaining bricks. Add a virtual **ROOF** node, joined to every top-row brick, so "stable" means "in the roof's group", and the roof group's size tells us how many stable bricks there are.
3. **Replay the hits backwards**, putting each brick back:
   ```
   before = size of the roof's group
   join the brick with its neighbouring bricks (and the roof if it's in the top row)
   after  = size of the roof's group
   fallen at this hit = after − before − 1      (−1: the restored brick itself)
                        or 0 if negative (the brick didn't connect to the roof)
   ```
Why that's the right count: putting the brick back re-attaches exactly the bricks that fell when it was erased.

### Repeated hits and empty cells
A cell can be hit twice, or hit when it's already empty; only the **first** hit on a real brick erases anything. The code decrements the cell once per hit (`1 → 0 → −1 …` for a brick, `0 → −1 …` for an empty cell). Replaying backwards, it increments the cell and restores the brick only when the value climbs back to **exactly 1**, which happens at that brick's first hit.

### Dry run: example 1
| Step | State | Roof group |
|---|---|---|
| remove hit (1,0) | `[[1,0,0,0],[0,1,1,0]]` | {roof, (0,0)}: size 2. (1,1)–(1,2) form their own group of 2 |
| restore (1,0): before = 2 | joins (0,0) (roof group) and (1,1) (the pair) | size 2 + 1 + 2 = **5** |
| fallen = 5 − 2 − 1 | | **2** → `[2]` |

---

## 3. Java concepts used in this problem

### 3.1 Union-find with sizes
```java
void union(int a, int b) { ... parent[rb] = ra; size[ra] += size[rb]; }
int sizeOf(int x) { return size[find(x)]; }
```
The root of each group stores the group's size. `sizeOf(roof)` is "how many bricks (plus the roof itself) are stable". Path halving in `find` keeps it fast ([Union-Find basics](../../11-Union-Find/01-Basics/UnionFindBasics.java)).

### 3.2 A virtual node
Cells are numbered `r * n + c` (0 … m·n − 1), and the roof gets the extra index `m * n`. Connecting every top-row brick to it turns "touches the top" into ordinary connectivity.

### 3.3 Joining only up and left when building
```java
if (r > 0 && g[r - 1][c] == 1) dsu.union(...);   // up
if (c > 0 && g[r][c - 1] == 1) dsu.union(...);   // left
```
Scanning top-left to bottom-right and joining each brick with its up and left neighbours covers every adjacent pair exactly once.

### 3.4 Copying a 2-D array
`grid[r].clone()` for each row: `grid.clone()` alone would copy only the outer array and share the rows, so changes would leak into the caller's grid.

---

## 4. Approach 1: Forward simulation with a BFS after every hit (the referee)

### Code ([A29_BricksFallingWhenHit.java:67-96](A29_BricksFallingWhenHit.java#L67-L96))
```java
static int[] hitBricksBrute(int[][] grid, int[][] hits) {
    int m = grid.length;
    int[][] g = new int[m][];
    for (int r = 0; r < m; r++) g[r] = grid[r].clone();
    int[] ans = new int[hits.length];
    for (int i = 0; i < hits.length; i++) {
        if (g[hits[i][0]][hits[i][1]] == 0) continue;                 // nothing there
        g[hits[i][0]][hits[i][1]] = 0;
        ans[i] = dropUnstable(g);
    }
    return ans;
}

/** BFS from the top row; erase every brick it can't reach and return how many fell. */
static int dropUnstable(int[][] g) {
    int m = g.length, n = g[0].length, fallen = 0;
    boolean[][] stable = new boolean[m][n];
    Deque<int[]> queue = new ArrayDeque<>();
    for (int c = 0; c < n; c++) if (g[0][c] == 1) { stable[0][c] = true; queue.add(new int[]{0, c}); }
    while (!queue.isEmpty()) {
        int[] cur = queue.poll();
        for (int[] d : DIRS) {
            int nr = cur[0] + d[0], nc = cur[1] + d[1];
            if (nr >= 0 && nr < m && nc >= 0 && nc < n && g[nr][nc] == 1 && !stable[nr][nc]) { stable[nr][nc] = true; queue.add(new int[]{nr, nc}); }
        }
    }
    for (int r = 0; r < m; r++)
        for (int c = 0; c < n; c++) if (g[r][c] == 1 && !stable[r][c]) { g[r][c] = 0; fallen++; }   // falls
    return fallen;
}
```

This follows the rules literally: erase, BFS from the top, remove everything unreached. **O(hits · m · n).** The test also uses `dropUnstable` to make the random starting grids fully stable, matching LeetCode's guarantee.

---

## 5. Approach 2: Reverse time + union-find (main solution)

### Code ([A29_BricksFallingWhenHit.java:99-128](A29_BricksFallingWhenHit.java#L99-L128))
```java
static int[] hitBricks(int[][] grid, int[][] hits) {
    int m = grid.length, n = grid[0].length, roof = m * n;
    int[][] g = new int[m][];
    for (int r = 0; r < m; r++) g[r] = grid[r].clone();
    for (int[] h : hits) g[h[0]][h[1]]--;                             // a brick hit at least once is gone at the end

    DSU dsu = new DSU(m * n + 1);
    for (int r = 0; r < m; r++)
        for (int c = 0; c < n; c++) {
            if (g[r][c] != 1) continue;
            if (r == 0) dsu.union(c, roof);
            if (r > 0 && g[r - 1][c] == 1) dsu.union(r * n + c, (r - 1) * n + c);   // up and left cover every pair once
            if (c > 0 && g[r][c - 1] == 1) dsu.union(r * n + c, r * n + c - 1);
        }

    int[] ans = new int[hits.length];
    for (int i = hits.length - 1; i >= 0; i--) {
        int r = hits[i][0], c = hits[i][1];
        if (++g[r][c] != 1) continue;                                 // this hit erased nothing
        int before = dsu.sizeOf(roof);
        if (r == 0) dsu.union(c, roof);
        for (int[] d : DIRS) {
            int nr = r + d[0], nc = c + d[1];
            if (nr >= 0 && nr < m && nc >= 0 && nc < n && g[nr][nc] == 1) dsu.union(r * n + c, nr * n + nc);
        }
        int after = dsu.sizeOf(roof);
        ans[i] = Math.max(0, after - before - 1);                     // the restored brick itself didn't "fall"
    }
    return ans;
}
```

### Line by line
| Line | What it does |
|---|---|
| `g[h]--` per hit | the final state; a value ≤ 0 means "no brick at the end" |
| build loop | union the final bricks (and the top row with the roof) |
| `for i` from last to first | replay time backwards |
| `++g[r][c] != 1 → continue` | this hit didn't erase a brick (an empty cell, or a later repeat) |
| `before` | stable bricks just before restoring |
| the unions | reconnect the brick to its neighbours (and the roof) |
| `after − before − 1` | bricks that become stable again because of this brick = the bricks that fell when it was erased |
| `max(0, …)` | restoring a brick that stays disconnected changes nothing |

### Complexity
- Building: O(m · n · α). Each hit: O(α) for up to 5 unions.
- **O((m · n + hits) · α(m · n))** time, **O(m · n)** space.

---

## 6. Comparing the approaches

| | Time |
|---|---|
| Forward BFS after each hit | O(hits · m · n) |
| **Reverse + union-find** | **O((m · n + hits) · α)** |

The file checks both on 2,000 random (initially stable) grids with random hits, including repeated hits and hits on empty cells.

---

## 7. Edge cases and common mistakes

**Edge cases:** hitting an empty cell (0), hitting the same brick twice (the second hit drops nothing), hitting a top-row brick, a hit that drops a long chain, a hit that drops nothing because the bricks are held from another side.

**Mistakes:**
1. Trying to "un-union" in the forward direction (impossible).
2. Restoring a cell that was originally empty, or restoring a repeated hit twice.
3. Forgetting the roof, or connecting only `(0,0)` to it instead of every top-row brick.
4. Not subtracting 1 for the restored brick itself.
5. Counting bricks that were never stable as "fallen" (LeetCode's grids start stable).

---

## 8. Interview questions and answers

**Q1. Why process hits in reverse?**
> Hits split components, which union-find can't do. In reverse, each hit becomes adding a brick, which only merges components, exactly what union-find does well.

**Q2. What's the roof node for?**
> It represents the ceiling. Connecting all top-row bricks to it means "stable" is "in the roof's component", and the component's size counts the stable bricks.

**Q3. How do you get the number of fallen bricks?**
> Compare the roof component's size before and after restoring the brick; the increase minus one (the brick itself) is how many bricks it was holding up.

### Follow-up questions

**F1. "Online: answer each hit before seeing the next."**
> Reverse time needs all hits up front. Online, you'd need a BFS per hit, or a fully dynamic connectivity structure (much more complex).

**F2. "Other 'reverse the deletions' problems?"**
> Graph connectivity under edge deletions, processed offline: add edges in reverse order with union-find. The same idea is used in "Remove Max Number of Edges" style questions and in island-flooding simulations.

**F3. "Bricks also stick to the side walls."**
> Join side-column bricks to the roof (or to a second virtual node that counts as stable).

---

## 9. Running the file

```bash
java A29_BricksFallingWhenHit.java     # 5/5 tests passed (2 examples, empty and repeated hits, cutting a column, 2,000 random grids)
```

**One-line summary:** *"Remove all hit bricks, union the rest with a virtual roof joined to the top row, then restore the hits in reverse order; each restoration's increase in the roof component's size, minus one, is how many bricks fell at that hit."*
