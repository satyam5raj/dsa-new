# M03 · Most Stones Removed with Same Row or Column

| | |
|---|---|
| **LeetCode** | [#947 Most Stones Removed with Same Row or Column](https://leetcode.com/problems/most-stones-removed-with-same-row-or-column/) |
| **Difficulty** | Medium |
| **Pattern** | answer = stones − components; union **rows with columns** (not pairs of stones) |
| **Code** | [M03_MostStonesRemoved.java](M03_MostStonesRemoved.java) |
| **Run** | `java M03_MostStonesRemoved.java` |

Union-Find basics are explained in [E01](../02-Easy/E01_FindIfPathExistsUnionFind.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Stones on a grid. A stone can be removed if another **remaining** stone shares its row or column. What's the maximum number of stones you can remove?

| stones | answer |
|---|---|
| [[0,0],[0,1],[1,0],[1,2],[2,1],[2,2]] | 5 |
| [[0,0],[0,2],[1,1],[2,0],[2,2]] | 3 |
| [[0,0]] | 0 |

---

## 2. How to think about it

### The key insight
Connect two stones if they share a row or a column. In each connected group, you can remove stones one at a time until exactly **one** is left: remove the leaves of a spanning tree first, so the rest stays connected. Different groups never help each other. So:

```
answer = number of stones − number of groups
```

### Building the groups
- **O(n²):** union every pair of stones that shares a row or column.
- **O(n), the trick:** treat each **row** and each **column** as a node. Stone (r, c) unions node "row r" with node "column c". Stones in the same row share the row node; stones in the same column share the column node. Offset the columns so they don't collide with the rows: column c → c + 10001 (or use a HashMap). Then groups = the number of distinct roots among the row/column nodes used.

### Dry run: example 1
```
stones link: r0–c0, r0–c1, r1–c0, r1–c2, r2–c1, r2–c2
→ all of rows 0, 1, 2 and columns 0, 1, 2 end up in one group → 6 − 1 = 5
```

Example 2: r0–c0, r0–c2, r1–c1, r2–c0, r2–c2 → groups {r0, c0, c2, r2} and {r1, c1} → 5 − 2 = **3**.

---

## 3. Java concepts used in this problem

### 3.1 A Union-Find over a `HashMap` (with sparse keys)

```java
static Map<Integer, Integer> parent;

static int find(int x) {
    parent.putIfAbsent(x, x);                    // create nodes lazily
    ...
}
```

Rows and columns can be up to 10⁴, and only the ones that actually hold stones matter. A map creates nodes on first use and makes "count the roots among the used nodes" easy.

### 3.2 Full path compression in two passes

```java
int root = x;
while (parent.get(root) != root) root = parent.get(root);                    // 1. find the root
while (parent.get(x) != root) { int nx = parent.get(x); parent.put(x, root); x = nx; }   // 2. point the path at it
```

The first loop finds the root; the second re-points every node on the path directly at it.

### 3.3 `Integer` vs `int` comparisons

`parent.get(root) != root`: the left side is an `Integer` and the right an `int`, so Java unboxes and compares the **values**. (Two `Integer`s compared with `!=` would compare references, and would break for values above 127.)

### 3.4 Offsetting the columns

`s[1] + 10_001` keeps column ids disjoint from row ids (the rows go up to 10⁴), so "row 3" and "column 3" are different nodes.

---

## 4. Approach 1: Union every pair of stones (the referee)

### Code ([M03_MostStonesRemoved.java:58-72](M03_MostStonesRemoved.java#L58-L72))

```java
static int brute(int[][] s) {
    int n = s.length;
    int[] p = new int[n];
    for (int i = 0; i < n; i++) p[i] = i;
    int groups = n;
    for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) {
        if (s[i][0] == s[j][0] || s[i][1] == s[j][1]) {
            int a = root(p, i), b = root(p, j);
            if (a != b) { p[a] = b; groups--; }
        }
    }
    return n - groups;
}
```

Stones are the nodes; any two sharing a row or column are united. **O(n² · α).**

---

## 5. Approach 2: Union rows with columns (the file's main solution)

### Code ([M03_MostStonesRemoved.java:39-55](M03_MostStonesRemoved.java#L39-L55))

```java
static Map<Integer, Integer> parent;

static int find(int x) {
    parent.putIfAbsent(x, x);
    int root = x;
    while (parent.get(root) != root) root = parent.get(root);
    while (parent.get(x) != root) { int nx = parent.get(x); parent.put(x, root); x = nx; }   // compress
    return root;
}

static int removeStones(int[][] stones) {
    parent = new HashMap<>();
    for (int[] s : stones) parent.put(find(s[0]), find(s[1] + 10_001));   // row node <-> column node
    Set<Integer> roots = new HashSet<>();
    for (int key : parent.keySet()) roots.add(find(key));
    return stones.length - roots.size();
}
```

### Line by line

| Line | What it does |
|---|---|
| `parent.put(find(row), find(col))` | each stone merges its row's group with its column's group |
| the roots loop | the number of distinct groups among the row/column nodes |
| `stones.length − roots.size()` | each group keeps one stone; the rest can be removed |

**Why the groups of row/column nodes match the groups of stones:** two stones are connected iff their row/column nodes are connected (sharing a row means sharing the row node, and so on). Each stone belongs to exactly one row/column group.

### Complexity
- **Time O(n · α).**
- **Space O(n).**

---

## 6. Comparing the approaches

| | Every pair of stones | Rows ↔ columns |
|---|---|---|
| Time | O(n²) | **O(n)** |
| Nodes | the stones | the rows and columns used |

The file checks the row/column trick against the pairwise version on 1,000 random boards.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single stone (0), all stones in one row (n − 1), every stone isolated (0).

**Mistakes:**
1. Rows and columns sharing ids (no offset).
2. Counting the roots over all possible ids instead of only the used ones.
3. Greedy removal without the "stones − groups" insight.

---

## 8. Interview questions and answers

**Q1. Why can every group be reduced to one stone?**
> Take a spanning tree of the group and remove the stones in leaf-first order: a leaf always has a neighbour remaining (its tree parent), sharing a row or column.

**Q2. Why union rows with columns?**
> It connects all the stones in a row (or column) through a single node, without examining every pair: O(n) unions instead of O(n²) pair checks.

### Follow-up questions

**F1. Return a valid removal order.**
> DFS each group and remove the stones in postorder, keeping the root last.

**F2. Number of Islands II.** → [H01](../04-Hard/H01_NumberOfIslandsII.md)
> Components that change as cells are added.

**F3. Similar String Groups (LC 839).**
> Union the strings that are similar; count the components.

---

## 9. Running the file

```bash
java M03_MostStonesRemoved.java      # 4/4 tests passed (including 1,000 random boards: row/col trick == pairwise)
```

**One-line summary:** *"Each stone unions its row node with its column node (columns offset by 10001); the answer is the number of stones minus the number of resulting groups."*
