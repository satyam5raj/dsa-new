# M05 · Regions Cut By Slashes

| | |
|---|---|
| **LeetCode** | [#959 Regions Cut By Slashes](https://leetcode.com/problems/regions-cut-by-slashes/) |
| **Difficulty** | Medium |
| **Pattern** | Split each cell into 4 triangles; union the pieces that touch; count the sets |
| **Code** | [M05_RegionsCutBySlashes.java](M05_RegionsCutBySlashes.java) |
| **Run** | `java M05_RegionsCutBySlashes.java` |

Union-Find basics are explained in [E01](../02-Easy/E01_FindIfPathExistsUnionFind.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

An n × n grid; each cell is ' ', '/' or '\'. The slashes split the square into regions. How many regions are there?

```
[" /", "/ "]  →  2                    ["/\\", "\\/"]  →  5 (a diamond in the middle)

+---+---+                             +---+---+
|   | / |                             | / | \ |
+---+---+                             +---+---+
| / |   |                             | \ | / |
+---+---+                             +---+---+
```

---

## 2. How to think about it

### The modelling trick: 4 triangles per cell
Cut every cell along both diagonals:

```
   +-----+
   |\ 0 /|        0 = top, 1 = right, 2 = bottom, 3 = left
   | 3 X 1|
   |/ 2 \|
   +-----+
```

**Inside a cell:**
- `' '` → all 4 triangles are one region: union 0–1, 1–2, 2–3
- `'/'` → the line runs from bottom-left to top-right: top + left together (0–3), right + bottom together (1–2)
- `'\'` → top + right together (0–1), bottom + left together (2–3)

**Between cells** (always open, since slashes never lie on cell borders):
- this cell's **right** (1) touches the right neighbour's **left** (3)
- this cell's **bottom** (2) touches the lower neighbour's **top** (0)

Triangle id = `4 · (r · n + c) + k`. The answer is the number of disjoint sets.

### The alternative: upscale
Blow each cell up to a 3 × 3 pixel block, draw the slashes as pixels, and count the islands with a flood fill. The same answer, with more memory (the file's referee).

### Dry run: the counts as cells are processed (from the real run)

| grid | after each cell (inside → after the borders) | regions |
|---|---|---|
| `[" /","/ "]` | (0,0) ' ': 16 → 13 → 11; (0,1) '/': 9 → 8; (1,0) '/': 6 → 5; (1,1) ' ': 2 → 2 | **2** |
| `["/\\","\\/"]` | (0,0) '/': 14 → 12; (0,1) '\': 10 → 9; (1,0) '\': 7 → 6; (1,1) '/': 5 → 5 | **5** |

---

## 3. Java concepts used in this problem

### 3.1 Encoding (row, col, piece) as one int

```java
int base = 4 * (r * n + c);       // the cell's first triangle id
base + k                          // triangle k of this cell
```

Union-Find needs integer ids 0..4n² − 1; this formula packs the three coordinates into one.

### 3.2 A counted union

```java
static void union(int a, int b) {
    int ra = find(a), rb = find(b);
    if (ra != rb) { parent[ra] = rb; count--; }
}
```

`count` starts at 4n² and drops by 1 on every successful merge, so at the end it **is** the number of regions.

### 3.3 Escaping the backslash

In Java source, `'\\'` is a single backslash character and `"/\\"` is the two-character string `/\`. That's why the test strings look doubled.

---

## 4. Approach 1: Upscale ×3 and flood-fill (the referee)

### Code ([M05_RegionsCutBySlashes.java:74-99](M05_RegionsCutBySlashes.java#L74-L99))

```java
static int upscale(String[] grid) {
    int n = grid.length, N = 3 * n;
    boolean[][] wall = new boolean[N][N];
    for (int r = 0; r < n; r++) for (int c = 0; c < n; c++) {
        char ch = grid[r].charAt(c);
        for (int k = 0; k < 3; k++) {
            if (ch == '/') wall[3 * r + k][3 * c + 2 - k] = true;
            if (ch == '\\') wall[3 * r + k][3 * c + k] = true;
        }
    }
    int regions = 0;
    for (int r = 0; r < N; r++) for (int c = 0; c < N; c++) {
        if (wall[r][c]) continue;
        regions++;
        Deque<int[]> st = new ArrayDeque<>(List.<int[]>of(new int[]{r, c}));
        wall[r][c] = true;
        while (!st.isEmpty()) {
            int[] p = st.pop();
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nr = p[0] + d[0], nc = p[1] + d[1];
                if (nr >= 0 && nr < N && nc >= 0 && nc < N && !wall[nr][nc]) { wall[nr][nc] = true; st.push(new int[]{nr, nc}); }
            }
        }
    }
    return regions;
}
```

| Line | What it does |
|---|---|
| the 3 × 3 block | a '/' becomes the anti-diagonal pixels, a '\' the main diagonal |
| the flood fill | count the connected open pixels (Number of Islands) |

A 3× scale is the smallest that keeps diagonal walls watertight in 4-connectivity. **O(9n²).**

---

## 5. Approach 2: 4 triangles per cell + Union-Find (the file's main solution)

### Code ([M05_RegionsCutBySlashes.java:41-71](M05_RegionsCutBySlashes.java#L41-L71))

```java
static int[] parent;
static int count;

static int find(int x) {
    while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
    return x;
}

static void union(int a, int b) {
    int ra = find(a), rb = find(b);
    if (ra != rb) { parent[ra] = rb; count--; }
}

static int regionsBySlashes(String[] grid) {
    int n = grid.length;
    parent = new int[4 * n * n];
    for (int i = 0; i < parent.length; i++) parent[i] = i;
    count = parent.length;
    for (int r = 0; r < n; r++) {
        for (int c = 0; c < n; c++) {
            int base = 4 * (r * n + c);
            char ch = grid[r].charAt(c);
            if (ch == ' ') { union(base, base + 1); union(base + 1, base + 2); union(base + 2, base + 3); }
            else if (ch == '/') { union(base, base + 3); union(base + 1, base + 2); }
            else { union(base, base + 1); union(base + 2, base + 3); }
            if (c + 1 < n) union(base + 1, 4 * (r * n + c + 1) + 3);     // right <-> neighbour's left
            if (r + 1 < n) union(base + 2, 4 * ((r + 1) * n + c));       // bottom <-> lower neighbour's top
        }
    }
    return count;
}
```

### Line by line

| Line | What it does |
|---|---|
| `count = 4n²` | every triangle starts as its own region |
| the `' '` case | the whole cell is one piece |
| the `'/'` case | top + left, and right + bottom |
| the `'\'` case | top + right, and bottom + left |
| the border unions | neighbouring cells always connect across their shared edge |
| `return count` | the number of regions |

### Complexity
- **Time O(n² · α).**
- **Space O(n²).**

---

## 6. Comparing the approaches

| | Upscale + flood fill | Triangles + Union-Find |
|---|---|---|
| Time | O(9n²) | O(4n² · α) |
| Memory | 9n² booleans | 4n² ints |
| Insight needed | the 3× scale | the 4-triangle split |

The file checks the triangle version against the upscaled flood fill on 500 random grids.

---

## 7. Edge cases and common mistakes

**Edge cases:** an all-blank grid (1), n = 1 with a slash (2), the diamond (5).

**Mistakes:**
1. Mixing up which triangles '/' and '\' connect.
2. Connecting a cell's right to the neighbour's **right** (it should be the neighbour's left).
3. Upscaling by 2 (diagonals then leak in 4-connectivity).

---

## 8. Interview questions and answers

**Q1. Why 4 triangles?**
> Both kinds of slash run along a cell's diagonals, so cutting along both diagonals makes every possible boundary an edge between triangles. A slash just decides which triangles stay together.

**Q2. Why are the cell borders always connected?**
> Slashes run between corners inside a cell, never along a border, so adjacent cells' touching triangles are always in the same region.

### Follow-up questions

**F1. Return the size of each region.**
> Track the set sizes in Union-Find (each triangle counts as a quarter of a cell).

**F2. Number of Islands.** → [Graph M01](../../09-Graph/03-Medium/M01_NumberOfIslands.md)
> The upscaled version is exactly this.

**F3. Count the enclosed regions only.**
> Union every border triangle with a virtual "outside" node, then count the sets not attached to it.

---

## 9. Running the file

```bash
java M05_RegionsCutBySlashes.java      # 4/4 tests passed (including 500 random grids vs 3x upscale + flood fill)
```

**One-line summary:** *"Split each cell into 4 triangles, union them according to the cell's slash and with the neighbouring cells' touching triangles, and count the disjoint sets."*
