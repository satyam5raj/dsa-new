# E02 · Flood Fill ⭐

| | |
|---|---|
| **LeetCode** | [#733 Flood Fill](https://leetcode.com/problems/flood-fill/) |
| **Difficulty** | Easy |
| **Pattern** | A grid is an implicit graph: DFS over the 4-directional neighbours |
| **Code** | [E02_FloodFill.java](E02_FloodFill.java) |
| **Run** | `java E02_FloodFill.java` |

⭐ = not in `leetcode.md`, added for a key technique. Graph basics (BFS, DFS, visited) are explained in [E01](E01_FindIfPathExists.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

The paint-bucket tool: starting at (sr, sc), recolour that pixel and every pixel **connected** to it (in 4 directions) that has the **same original colour**.

```
image             start (1, 1), colour 2
1 1 1                   2 2 2
1 1 0         →         2 2 0
1 0 1                   2 0 1     ← the bottom-right 1 is NOT connected (only diagonally)
```

| image | start | colour | result |
|---|---|---|---|
| `[[1,1,1],[1,1,0],[1,0,1]]` | (1,1) | 2 | `[[2,2,2],[2,2,0],[2,0,1]]` |
| `[[0,0,0],[0,0,0]]` | (0,0) | 0 | unchanged |

---

## 2. How to think about it

### A grid is a graph
Each cell is a vertex; edges join it to its up, down, left and right neighbours. You never build an adjacency list: the neighbours are computed on the fly from (r ± 1, c) and (r, c ± 1).

### The grid template (Number of Islands, Rotting Oranges, Surrounded Regions, …)
```
dfs(r, c):
    if out of bounds OR not the colour we're replacing → return
    paint it                 ← painting doubles as "visited" (no extra array)
    dfs on the 4 neighbours
```

### The trap
If the new colour equals the original colour, painting doesn't change anything, so "visited" never gets marked and the DFS recurses forever (well, until it overflows the stack). **Return early.**

### Deep recursion
A 1000 × 1000 single-colour image means a recursion up to 10⁶ deep → `StackOverflowError`. An explicit stack (or a BFS queue) avoids it. The file's test fills a 1000 × 1000 image iteratively.

---

## 3. Java concepts used in this problem

### 3.1 Bounds checks with `||` short-circuiting

```java
if (r < 0 || r >= img.length || c < 0 || c >= img[0].length || img[r][c] != from) return;
```

The bounds tests come first, so `img[r][c]` is only read when (r, c) is inside the grid.

### 3.2 A direction array

```java
int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
for (int[] d : dirs) { int r = p[0] + d[0], c = p[1] + d[1]; ... }
```

It replaces four copy-pasted neighbour blocks with one loop. (Add the 4 diagonals for 8-directional problems.)

### 3.3 Mutating the input as "visited"

The recolouring itself marks a cell as done. That saves the O(R · C) visited array, but it **modifies the input**, which is fine here because that's the required output.

### 3.4 Deep-copying a 2D array (in the test)

```java
int[][] b = Arrays.stream(a).map(int[]::clone).toArray(int[][]::new);
```

`a.clone()` alone would copy only the outer array (the rows would be shared). Cloning each row gives a truly independent copy, so the two versions can be compared.

---

## 4. Approach 1: Recursive DFS (the file's main solution)

### Code ([E02_FloodFill.java:35-49](E02_FloodFill.java#L35-L49))

```java
static int[][] floodFill(int[][] image, int sr, int sc, int color) {
    int original = image[sr][sc];
    if (original == color) return image;                 // nothing to do (and avoids infinite recursion)
    dfs(image, sr, sc, original, color);
    return image;
}

static void dfs(int[][] img, int r, int c, int from, int to) {
    if (r < 0 || r >= img.length || c < 0 || c >= img[0].length || img[r][c] != from) return;
    img[r][c] = to;                                      // paint = mark visited
    dfs(img, r + 1, c, from, to);
    dfs(img, r - 1, c, from, to);
    dfs(img, r, c + 1, from, to);
    dfs(img, r, c - 1, from, to);
}
```

### Line by line

| Line | What it does |
|---|---|
| `original == color` | painting would change nothing → return (and avoid infinite recursion) |
| the guard in `dfs` | outside the grid, or a different colour (including already painted) → stop |
| `img[r][c] = to` | paint the cell, which also marks it visited |
| the four calls | down, up, right, left |

### Dry run: the example, start (1, 1)

The cells are painted in this order (from running the code):

| order | cell | how it was reached |
|---|---|---|
| 1 | (1, 1) | the start |
| 2 | (0, 1) | up from (1, 1) (down was (2, 1) = 0, rejected) |
| 3 | (0, 2) | right from (0, 1) |
| 4 | (0, 0) | left from (0, 1) |
| 5 | (1, 0) | down from (0, 0) |
| 6 | (2, 0) | down from (1, 0) |

(2, 2) is never reached: its only neighbours are (1, 2) = 0 and (2, 1) = 0. Result `[[2,2,2],[2,2,0],[2,0,1]]` ✓

### Complexity
- **Time O(R · C).**
- **Space O(R · C)** for the recursion in the worst case (one long snake-shaped region).

---

## 5. Approach 2: Iterative with an explicit stack

### Code ([E02_FloodFill.java:51-69](E02_FloodFill.java#L51-L69))

```java
static int[][] floodFillIterative(int[][] image, int sr, int sc, int color) {
    int original = image[sr][sc];
    if (original == color) return image;
    int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    Deque<int[]> stack = new ArrayDeque<>();
    stack.push(new int[]{sr, sc});
    image[sr][sc] = color;
    while (!stack.isEmpty()) {
        int[] p = stack.pop();
        for (int[] d : dirs) {
            int r = p[0] + d[0], c = p[1] + d[1];
            if (r >= 0 && r < image.length && c >= 0 && c < image[0].length && image[r][c] == original) {
                image[r][c] = color;
                stack.push(new int[]{r, c});
            }
        }
    }
    return image;
}
```

### Line by line

| Line | What it does |
|---|---|
| `stack.push(new int[]{sr, sc})` | cells are stored as `{row, col}` pairs |
| `image[sr][sc] = color` | paint (mark) **when pushing**, as in E01 |
| `p = stack.pop()` | the next cell to expand |
| the `dirs` loop | each in-bounds neighbour that still has the original colour gets painted and pushed |

### Dry run (from the real run)

| step | action |
|---|---|
| 1 | pop (1, 1) → paint and push (0, 1), (1, 0) |
| 2 | pop (1, 0) → paint and push (2, 0), (0, 0) |
| 3 | pop (0, 0) → nothing new |
| 4 | pop (2, 0) → nothing new |
| 5 | pop (0, 1) → paint and push (0, 2) |
| 6 | pop (0, 2) → nothing new |

The same 6 cells, in a different order. Swapping the stack for a queue gives BFS, with the same result.

### Complexity
- **Time O(R · C).**
- **Space O(R · C)** for the stack, but on the heap: no stack overflow.

---

## 6. Comparing the approaches

| | Recursive DFS | Iterative stack |
|---|---|---|
| Time | O(R · C) | O(R · C) |
| Space | O(R · C) call stack | O(R · C) heap |
| 1000 × 1000 single colour | StackOverflowError | fine |

The file checks that both give identical images on 500 random inputs.

---

## 7. Edge cases and common mistakes

**Edge cases:** the new colour equals the old one, a 1 × 1 image, the whole image is one colour, the start is isolated.

**Mistakes:**
1. Missing the same-colour early return (infinite recursion).
2. Including diagonals (not connected here).
3. Checking `img[r][c]` before the bounds.
4. Comparing against the **new** colour instead of the original.

---

## 8. Interview questions and answers

**Q1. How does painting replace a visited array?**
> A painted cell no longer has the original colour, so the guard rejects it. Every cell is painted at most once.

**Q2. When would you avoid the recursive version?**
> When regions can be large (deep recursion). Use an explicit stack or a BFS queue.

### Follow-up questions

**F1. Number of Islands.** → [M01](../03-Medium/M01_NumberOfIslands.md)
> Flood fill each unvisited land cell, and count how many fills you start.

**F2. 8-directional connectivity.**
> Add the 4 diagonal directions to `dirs`.

**F3. Fill everything NOT connected to the border.** → [M06 Surrounded Regions](../03-Medium/M06_SurroundedRegions.md)
> Flood fill from the border first, then flip everything else.

---

## 9. Running the file

```bash
java E02_FloodFill.java      # 5/5 tests passed (including 500 random images and a 1000×1000 iterative fill)
```

**One-line summary:** *"Treat the grid as a graph: DFS from the start, painting cells of the original colour (painting = visited); return early if the colour doesn't change."*
