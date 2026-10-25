# H01 · Number of Islands II ⭐

| | |
|---|---|
| **LeetCode** | [#305 Number of Islands II](https://leetcode.com/problems/number-of-islands-ii/) (premium) |
| **Difficulty** | Hard |
| **Pattern** | **Online** connectivity: add land one cell at a time and keep a running island count |
| **Code** | [H01_NumberOfIslandsII.java](H01_NumberOfIslandsII.java) |
| **Run** | `java H01_NumberOfIslandsII.java` |

⭐ = not in `leetcode.md`, added for a key technique. Union-Find basics are explained in [E01](../02-Easy/E01_FindIfPathExistsUnionFind.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

An m × n grid starts as all water. `positions[i]` is a cell that turns into land. After **each** addition, report the number of islands.

```
m = 3, n = 3, positions = [[0,0],[0,1],[1,2],[2,1]]  →  [1, 1, 2, 3]

after (0,0)     after (0,1)     after (1,2)     after (2,1)
1 . .           1 1 .           1 1 .           1 1 .
. . .           . . .           . . 1           . . 1
. . .           . . .           . . .           . 1 .
```

| m × n | positions | answer |
|---|---|---|
| 3 × 3 | [[0,0],[0,1],[1,2],[2,1]] | [1, 1, 2, 3] |
| 1 × 1 | [[0,0],[0,0]] | [1, 1] (a duplicate changes nothing) |
| 1 × 3 | [[0,0],[0,2],[0,1]] | [1, 2, 1] (the middle cell bridges two islands) |

---

## 2. How to think about it

### Why not DFS?
Recounting the islands ([Graph M01](../../09-Graph/03-Medium/M01_NumberOfIslands.md)) after every addition costs O(m·n) per step, so O(k·m·n) in total. With 10⁴ additions on a 10⁴-cell grid that's 10⁸ or more.

### Union-Find is built for "edges arrive over time"
Keep a running count:

```
add (r, c):
    already land?  → report count again (duplicates are allowed!)
    make it land, count++                      (a brand-new one-cell island)
    for each of the 4 neighbours that is land:
        if union(cell, neighbour) succeeds → count--     (two islands merged)
```

Cell id = `r · n + c`. Each addition costs O(4 · α) ≈ O(1).

A new cell next to **two different** islands causes two successful unions: +1 − 2, so the count drops by one overall. A new cell next to two land cells of the **same** island causes one successful union and one failed one: +1 − 1.

### Dry run: the example

| add | id | count after +1 | land neighbours | successful unions | count |
|---|---|---|---|---|---|
| (0,0) | 0 | 1 | none | 0 | **1** |
| (0,1) | 1 | 2 | (0,0) | 1 | **1** |
| (1,2) | 5 | 2 | none ((0,2) and (2,2) are water) | 0 | **2** |
| (2,1) | 7 | 3 | none | 0 | **3** |

### The bridge: 1 × 3, [[0,0],[0,2],[0,1]]

| add | count after +1 | unions | count |
|---|---|---|---|
| (0,0) | 1 | | **1** |
| (0,2) | 2 | | **2** |
| (0,1) | 3 | with (0,0) ✓, with (0,2) ✓ | **1** |

### What Union-Find can't do
Land turning back into water would need an "un-union", which Union-Find doesn't support. The usual trick is to process the operations **backwards** (offline), so removals become additions.

---

## 3. Java concepts used in this problem

### 3.1 A 2D grid flattened to 1D ids

```java
int id = p[0] * n + p[1];          // (row, col) → 0..m·n − 1
```

The parent array and the `land` array are both 1D, indexed by this id.

### 3.2 A `boolean[]` for "is it land yet?"

The grid itself is never stored. `land[id]` is the only state besides `parent`, and it's what makes duplicate positions harmless.

### 3.3 The direction array

```java
int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
for (int[] d : dirs) { int r = p[0] + d[0], c = p[1] + d[1]; ... }
```

The same four-neighbour idiom as the graph grid problems. The bounds check must come before `land[r * n + c]`, which `||` guarantees by short-circuiting.

### 3.4 Building the answer with `List<Integer>`

`result.add(count)` autoboxes each `int` into an `Integer`. The test compares it with `"[1, 1, 2, 3]"` through `List.toString()`.

---

## 4. Approach 1: Recount with DFS after every addition (the referee)

### Code ([H01_NumberOfIslandsII.java:74-97](H01_NumberOfIslandsII.java#L74-L97))

```java
static List<Integer> brute(int m, int n, int[][] positions) {
    boolean[][] g = new boolean[m][n];
    List<Integer> out = new ArrayList<>();
    for (int[] p : positions) {
        g[p[0]][p[1]] = true;
        boolean[][] seen = new boolean[m][n];
        int c = 0;
        for (int r = 0; r < m; r++) for (int q = 0; q < n; q++) {
            if (!g[r][q] || seen[r][q]) continue;
            c++;
            Deque<int[]> st = new ArrayDeque<>(List.<int[]>of(new int[]{r, q}));
            seen[r][q] = true;
            while (!st.isEmpty()) {
                int[] x = st.pop();
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int a = x[0] + d[0], b = x[1] + d[1];
                    if (a >= 0 && a < m && b >= 0 && b < n && g[a][b] && !seen[a][b]) { seen[a][b] = true; st.push(new int[]{a, b}); }
                }
            }
        }
        out.add(c);
    }
    return out;
}
```

| Line | What it does |
|---|---|
| `g[p[0]][p[1]] = true` | add the land |
| a fresh `seen` | the recount starts from scratch every time |
| the double loop + stack | Number of Islands with an iterative DFS |
| `out.add(c)` | the count after this addition |

**O(k · m · n).** Obviously correct, and far too slow for big inputs.

---

## 5. Approach 2: Incremental Union-Find (the file's main solution)

### Code ([H01_NumberOfIslandsII.java:42-71](H01_NumberOfIslandsII.java#L42-L71))

```java
static int[] parent;

static int find(int x) {
    while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
    return x;
}

static List<Integer> numIslands2(int m, int n, int[][] positions) {
    parent = new int[m * n];
    boolean[] land = new boolean[m * n];
    for (int i = 0; i < m * n; i++) parent[i] = i;
    int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    List<Integer> result = new ArrayList<>();
    int count = 0;
    for (int[] p : positions) {
        int id = p[0] * n + p[1];
        if (!land[id]) {
            land[id] = true;
            count++;                                          // a new one-cell island
            for (int[] d : dirs) {
                int r = p[0] + d[0], c = p[1] + d[1];
                if (r < 0 || r >= m || c < 0 || c >= n || !land[r * n + c]) continue;
                int a = find(id), b = find(r * n + c);
                if (a != b) { parent[a] = b; count--; }       // merged two islands
            }
        }
        result.add(count);
    }
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| `parent`, `land` | all m·n cells exist up front, but only land cells take part |
| `if (!land[id])` | a duplicate skips straight to reporting the same count |
| `count++` | the new cell is its own island for now |
| the `continue` | skip neighbours off the grid or still water |
| `find(id)` inside the loop | after the first merge, `id`'s root has changed, so it's recomputed each time |
| `count--` | each successful union removes one island |
| `result.add(count)` | report after every position, duplicate or not |

### Complexity
- **Time O(m·n + k · α).** The m·n is the initialisation.
- **Space O(m·n).**

---

## 6. Comparing the approaches

| | DFS recount | Incremental Union-Find |
|---|---|---|
| Time | O(k · m · n) | **O(m·n + k · α)** |
| Space | O(m·n) | O(m·n) |
| Handles removals? | yes (recount) | no (needs the offline reverse trick) |

The file checks the Union-Find version against the DFS recount on 300 random sequences (with duplicates).

---

## 7. Edge cases and common mistakes

**Edge cases:** duplicate positions, a 1 × 1 grid, a cell that joins three or four islands at once, a single row.

**Mistakes:**
1. Not handling duplicates (incrementing `count` again for land that already exists).
2. Using `find(id)` once before the loop and reusing it after a merge (fine here only by luck of the order; recomputing is the safe habit).
3. Unioning with neighbours that are water.
4. `id = r · m + c` instead of `r · n + c` (n is the number of columns).

---

## 8. Interview questions and answers

**Q1. Why does each successful union lower the count by exactly one?**
> A successful union merges two different islands into one. A failed union means the neighbour was already in the same island, so nothing changes.

**Q2. Why not just run Number of Islands after every step?**
> It's O(m·n) per step. Union-Find makes each addition nearly O(1) by updating the answer instead of recomputing it.

**Q3. How do you avoid allocating m·n up front for a huge sparse grid?**
> Use a `HashMap<Integer, Integer>` for the parents and create nodes lazily, as in [M03](../03-Medium/M03_MostStonesRemoved.md).

### Follow-up questions

**F1. Cells can also turn back into water.**
> Offline: if all operations are known, process them in reverse so removals become additions. Online: you need a fully dynamic connectivity structure, which is much more complex.

**F2. Bricks Falling When Hit (LC 803).**
> The classic reverse trick: remove all hit bricks, union what's left, then add the bricks back in reverse and count how many become attached to the top.

**F3. Report the size of the largest island after each step.**
> Keep a size per root and a running maximum; a merge only ever grows sizes, so the maximum never decreases.

---

## 9. Running the file

```bash
java H01_NumberOfIslandsII.java      # 4/4 tests passed (including 300 random sequences vs recounting with DFS)
```

**One-line summary:** *"Each new land cell adds one island, and each successful union with a land neighbour removes one; duplicates just repeat the count."*
