# A07 · Making A Large Island ⭐

| | |
|---|---|
| **LeetCode** | [#827 Making A Large Island](https://leetcode.com/problems/making-a-large-island/) |
| **Difficulty** | Hard |
| **Mix of structures** | **DFS labelling** (grid → island ids) + **HashMap** (id → size) + **HashSet** (distinct neighbour ids) |
| **Code** | [A07_MakingALargeIsland.java](A07_MakingALargeIsland.java) |
| **Run** | `java A07_MakingALargeIsland.java` |

⭐ = not in `leetcode.md`, added because it combines graph traversal with hashing.

---

## 1. The problem

An `n × n` grid of `0` (water) and `1` (land). You may change **at most one** 0 into a 1. Return the size of the largest island afterwards (an island is a group of 1s connected up/down/left/right).

```
[[1,0],
 [0,1]]   → 3      flip either 0: it joins both 1s
[[1,1],
 [1,0]]   → 4      flip the 0
[[1,1],
 [1,1]]   → 4      nothing to flip
```

---

## 2. How to think about it

### Brute force
Try flipping every 0, flood-fill the whole grid to find the biggest island, and undo. There are n² zeros and each flood fill is n²: **O(n⁴)**. For n = 500 that's 6 × 10¹⁰: too slow.

### The insight: precompute island sizes
When you flip a 0, the new island is that cell **plus every island touching it**. If we already know each island's size, we don't need to flood-fill again:

```
new size = 1 + sum of the sizes of the DISTINCT islands among the 4 neighbours
```

So:
1. **Pass 1:** flood-fill each island once, writing an **id** (2, 3, 4, …) into a `label` grid and recording `size[id]`.
2. **Pass 2:** for each 0, collect the ids of its neighbours **in a Set** and add up their sizes.

### Why a Set
One island can touch a cell from two sides. Without a Set it would be counted twice:

```
1 1 1
1 0 0       the 0 at (1,1) has island #2 above it AND to its left
0 0 0       correct: 1 + 4 = 5      without a Set: 1 + 4 + 4 = 9
```

### Why ids start at 2
In the code the ids live in a separate `label` grid, so 0 just means "not labelled yet". Starting at 2 also lets you write ids straight into the input grid in place, where 0 and 1 are already taken. Many solutions do that to save memory.

### Dry run: a 3 × 3 grid
```
grid          labels after pass 1      sizes
1 1 0         2 2 0                    #2 = 2
0 0 1         0 0 3                    #3 = 2
1 0 1         4 0 3                    #4 = 1
```

| zero at | neighbour ids (Set) | new size |
|---|---|---|
| (0,2) | {2, 3} | 1 + 2 + 2 = **5** |
| (1,0) | {2, 4} | 1 + 2 + 1 = 4 |
| (1,1) | {2, 3} | 1 + 2 + 2 = **5** |
| (2,1) | {4, 3} | 1 + 1 + 2 = 4 |

Answer **5** (the largest island without a flip is 2).

---

## 3. Java concepts used in this problem

### 3.1 An explicit stack instead of recursion
```java
Deque<int[]> stack = new ArrayDeque<>();
stack.push(new int[]{r0, c0});
while (!stack.isEmpty()) { int[] cur = stack.pop(); ... stack.push(new int[]{nr, nc}); }
```
A recursive DFS on a 500 × 500 island of land would go 250,000 calls deep, far beyond Java's default stack (usually around 10,000–20,000 frames), and crash with `StackOverflowError`. An `ArrayDeque` used as a stack lives on the **heap**, so it can grow as big as needed. The file tests a 400 × 400 grid to prove it.

### 3.2 Mark when pushing, not when popping
```java
label[nr][nc] = id;                 // mark when pushed, so nothing is pushed twice
stack.push(new int[]{nr, nc});
```
If you mark only when popping, a cell can be pushed several times by different neighbours before it's popped, which wastes time and inflates the count unless you re-check.

### 3.3 A direction array
```java
static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
for (int[] d : DIRS) { int nr = r + d[0], nc = c + d[1]; ... }
```
This replaces four copy-pasted neighbour blocks.

### 3.4 `HashSet` for "distinct"
`around.add(id)` ignores duplicates, so iterating `around` sums each island once. See [Arrays E02](../../01-Arrays/02-Easy/E02_ContainsDuplicate.md) for how `HashSet` works.

---

## 4. Approach 1: Flip each 0 and flood-fill again (the referee)

### Code ([A07_MakingALargeIsland.java:42-52](A07_MakingALargeIsland.java#L42-L52))
```java
static int largestIslandBrute(int[][] grid) {
    int n = grid.length, best = largestIslandNoFlip(grid);
    for (int r = 0; r < n; r++)
        for (int c = 0; c < n; c++)
            if (grid[r][c] == 0) {
                grid[r][c] = 1;
                best = Math.max(best, largestIslandNoFlip(grid));
                grid[r][c] = 0;                                      // undo
            }
    return best;
}
```

`largestIslandNoFlip` ([lines 54-78](A07_MakingALargeIsland.java#L54-L78)) is a standard flood fill with a `seen` array that returns the biggest island. The brute force tries every flip, plus "no flip" for an all-land grid: **O(n⁴)**.

---

## 5. Approach 2: Label islands once, then try each 0 (main solution)

### Code ([A07_MakingALargeIsland.java:81-111](A07_MakingALargeIsland.java#L81-L111))
```java
static int largestIsland(int[][] grid) {
    int n = grid.length;
    int[][] label = new int[n][n];
    Map<Integer, Integer> size = new HashMap<>();
    int next = 2, best = 0;

    for (int r = 0; r < n; r++)                                      // pass 1: label + measure
        for (int c = 0; c < n; c++)
            if (grid[r][c] == 1 && label[r][c] == 0) {
                int s = flood(grid, label, r, c, next);
                size.put(next, s);
                best = Math.max(best, s);
                next++;
            }

    boolean sawZero = false;
    for (int r = 0; r < n; r++)                                      // pass 2: try every 0
        for (int c = 0; c < n; c++)
            if (grid[r][c] == 0) {
                sawZero = true;
                Set<Integer> around = new HashSet<>();
                for (int[] d : DIRS) {
                    int nr = r + d[0], nc = c + d[1];
                    if (nr >= 0 && nr < n && nc >= 0 && nc < n && label[nr][nc] != 0) around.add(label[nr][nc]);
                }
                int joined = 1;
                for (int id : around) joined += size.get(id);
                best = Math.max(best, joined);
            }
    return sawZero ? best : n * n;
}
```

And the flood fill ([lines 113-130](A07_MakingALargeIsland.java#L113-L130)):
```java
private static int flood(int[][] grid, int[][] label, int r0, int c0, int id) {
    int n = grid.length, count = 0;
    Deque<int[]> stack = new ArrayDeque<>();
    stack.push(new int[]{r0, c0});
    label[r0][c0] = id;
    while (!stack.isEmpty()) {
        int[] cur = stack.pop();
        count++;
        for (int[] d : DIRS) {
            int nr = cur[0] + d[0], nc = cur[1] + d[1];
            if (nr >= 0 && nr < n && nc >= 0 && nc < n && grid[nr][nc] == 1 && label[nr][nc] == 0) {
                label[nr][nc] = id;                                  // mark when pushed, so nothing is pushed twice
                stack.push(new int[]{nr, nc});
            }
        }
    }
    return count;
}
```

### Line by line
| Line | What it does |
|---|---|
| pass 1: `grid == 1 && label == 0` | land that isn't labelled yet = a new island |
| `flood(...)` | labels the whole island with `next` and returns its size |
| `best = max(best, s)` | covers the "don't flip" case (useful when flipping can't beat an existing island) |
| pass 2: `around` | the distinct islands touching this 0 |
| `joined = 1 + sizes` | the flipped cell plus everything it connects |
| `sawZero ? best : n * n` | no water at all → the whole grid is one island |

### Complexity
- Pass 1 visits each cell once; pass 2 looks at 4 neighbours per cell.
- **O(n²)** time, **O(n²)** space (the label grid).

---

## 6. Comparing the approaches

| | Time | Notes |
|---|---|---|
| Flip and refill | O(n⁴) | simple, too slow for n = 500 |
| **Label + sizes + set** | **O(n²)** | every cell touched a constant number of times |

The file checks both on 1,500 random grids, plus the edge cases and a 400 × 400 grid.

---

## 7. Edge cases and common mistakes

**Edge cases:** all water (answer 1), all land (answer n²), one island touching a 0 from several sides, a 0 surrounded by four different islands.

**Mistakes:**
1. Adding neighbour sizes without a Set (double counting).
2. Recursive DFS on huge grids (`StackOverflowError`).
3. Forgetting the all-land case: pass 2 never runs. If you only update `best` in pass 2, you return 0. The explicit `n * n` makes it impossible to miss.
4. Re-flood-filling for each 0 (back to O(n⁴)).
5. Marking cells only when popping from the stack.

---

## 8. Interview questions and answers

**Q1. What's the key idea?**
> Precompute each island's size once and label its cells with an id. Then flipping a 0 is just 1 + the sizes of its distinct neighbouring islands, an O(1) check per cell.

**Q2. Why a Set in pass 2?**
> The same island can be adjacent through two or more sides; the Set makes sure it's counted once.

**Q3. DFS or BFS or union-find?**
> Any of them can label islands. Union-find works too: union adjacent land cells, and a component's size is its root's size. Then pass 2 looks up the distinct roots of the neighbours.

### Follow-up questions

**F1. "You may flip up to k zeros."**
> That's much harder (close to a search problem). For small k, try combinations with pruning; in general it's NP-hard-flavoured.

**F2. "The grid is updated online (Number of Islands II)."**
> Use union-find, adding land cells one at a time. See [Union-Find H01](../../11-Union-Find/04-Hard/H01_NumberOfIslandsII.md).

**F3. "Save memory."**
> Write ids into the input grid (starting at 2) instead of a separate `label` array.

---

## 9. Running the file

```bash
java A07_MakingALargeIsland.java     # 7/7 tests passed (examples, all water, shared island, 400x400, 1,500 random grids)
```

**One-line summary:** *"Flood-fill each island once, labelling its cells with an id and storing id → size in a HashMap; then for each 0, add 1 to the sizes of the distinct island ids around it, using a HashSet so no island counts twice."*
