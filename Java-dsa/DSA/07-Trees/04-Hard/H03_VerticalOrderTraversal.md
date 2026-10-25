# H03 · Vertical Order Traversal of a Binary Tree

| | |
|---|---|
| **LeetCode** | [#987 Vertical Order Traversal of a Binary Tree](https://leetcode.com/problems/vertical-order-traversal-of-a-binary-tree/) |
| **Difficulty** | Hard |
| **Pattern** | Give every node coordinates (row, col), then sort with a 3-key comparator |
| **Code** | [H03_VerticalOrderTraversal.java](H03_VerticalOrderTraversal.java) |
| **Run** | `java H03_VerticalOrderTraversal.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](../02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

The root is at (row 0, col 0). The left child of (r, c) is at (r + 1, c − 1), and the right child is at (r + 1, c + 1). Return the **columns from left to right**. Inside a column, list the nodes **top to bottom**; nodes in the **same row and column** are ordered by **value**.

```
                1 (0,0)
             /          \
        2 (1,−1)       3 (1,1)
         /    \         /    \
   4 (2,−2) 5 (2,0)  6 (2,0)  7 (2,2)       → [[4], [2], [1, 5, 6], [3], [7]]
                                              (5 and 6 tie on (2, 0): sorted by value)
```

| tree | answer |
|---|---|
| `[3,9,20,null,null,15,7]` | `[[9],[3,15],[20],[7]]` |
| `[1,2,3,4,5,6,7]` | `[[4],[2],[1,5,6],[3],[7]]` |
| `[1,2,3,4,6,5,7]` | `[[4],[2],[1,5,6],[3],[7]]` (the tie is still sorted by value) |

---

## 2. How to think about it

### Why it's "Hard": the tie rule
The traversal itself is easy. A plain BFS by column (LC 314, the easier variant) would list 5 and 6 in left-to-right order, but here ties must be **sorted by value**. With `[1,2,3,4,6,5,7]`, BFS order would give 6, 5, which is wrong.

### Approach
1. A DFS (any order) collecting triples **(col, row, val)**
2. Sort by col, then row, then val
3. Walk the sorted list, starting a new group whenever the column changes

```
[3,9,20,null,null,15,7]:
triples: (0,0,3) (−1,1,9) (1,1,20) (0,2,15) (2,2,7)
sorted:  (−1,1,9) | (0,0,3) (0,2,15) | (1,1,20) | (2,2,7)
groups:  [9]        [3, 15]            [20]       [7]
```

### Alternative
`TreeMap<col, TreeMap<row, PriorityQueue<val>>>`: the maps keep the columns and rows sorted, and the heap handles ties. Same O(n log n), with more ceremony. The file uses it as the referee.

---

## 3. Java concepts used in this problem

### 3.1 A multi-key comparator

```java
nodes.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0])
                   : a[1] != b[1] ? Integer.compare(a[1], b[1])
                   : Integer.compare(a[2], b[2]));
```

Chained ternaries: compare by column; if equal, by row; if equal, by value. `Integer.compare` avoids the overflow risk of `a − b`. An equivalent, more readable form:

```java
Comparator<int[]> byColRowVal = Comparator.<int[]>comparingInt(a -> a[0])
        .thenComparingInt(a -> a[1])
        .thenComparingInt(a -> a[2]);
```

### 3.2 `int[]` as a lightweight tuple

`new int[]{col, row, val}` packs three numbers without defining a class (a `record` would be clearer in production code).

### 3.3 Nested `computeIfAbsent`

```java
cols.computeIfAbsent(col, k -> new TreeMap<>()).computeIfAbsent(row, k -> new PriorityQueue<>()).add(n.val);
```

Get or create the column's map, then get or create the row's heap, then add the value. `TreeMap` keeps its keys sorted; its `values()` come back in key order.

### 3.4 `Integer prevCol = null`

A boxed `Integer` can be null, meaning "no column yet". `n[0] != prevCol` compares an `int` with an `Integer`, which unboxes prevCol (safe, because the null case is checked first with `||`).

---

## 4. Approach 1: Nested TreeMaps + heaps (the referee)

### Code ([H03_VerticalOrderTraversal.java:67-84](H03_VerticalOrderTraversal.java#L67-L84))

```java
static List<List<Integer>> withTreeMaps(TreeNode root) {
    TreeMap<Integer, TreeMap<Integer, PriorityQueue<Integer>>> cols = new TreeMap<>();
    fill(root, 0, 0, cols);
    List<List<Integer>> result = new ArrayList<>();
    for (TreeMap<Integer, PriorityQueue<Integer>> rows : cols.values()) {
        List<Integer> col = new ArrayList<>();
        for (PriorityQueue<Integer> pq : rows.values()) while (!pq.isEmpty()) col.add(pq.poll());
        result.add(col);
    }
    return result;
}

static void fill(TreeNode n, int row, int col, TreeMap<Integer, TreeMap<Integer, PriorityQueue<Integer>>> cols) {
    if (n == null) return;
    cols.computeIfAbsent(col, k -> new TreeMap<>()).computeIfAbsent(row, k -> new PriorityQueue<>()).add(n.val);
    fill(n.left, row + 1, col - 1, cols);
    fill(n.right, row + 1, col + 1, cols);
}
```

| Line | What it does |
|---|---|
| `fill` | put each value into its column's row's heap |
| `cols.values()` | the columns, from left to right |
| `rows.values()` | the rows in that column, top to bottom |
| `pq.poll()` until empty | the tied values, smallest first |

O(n log n).

---

## 5. Approach 2: Collect triples, then sort (the file's solution)

### Code ([H03_VerticalOrderTraversal.java:41-64](H03_VerticalOrderTraversal.java#L41-L64))

```java
static List<List<Integer>> verticalTraversal(TreeNode root) {
    List<int[]> nodes = new ArrayList<>();                 // {col, row, val}
    collect(root, 0, 0, nodes);
    nodes.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0])
                       : a[1] != b[1] ? Integer.compare(a[1], b[1])
                       : Integer.compare(a[2], b[2]));
    List<List<Integer>> result = new ArrayList<>();
    Integer prevCol = null;
    for (int[] n : nodes) {
        if (prevCol == null || n[0] != prevCol) {          // a new column starts
            result.add(new ArrayList<>());
            prevCol = n[0];
        }
        result.get(result.size() - 1).add(n[2]);
    }
    return result;
}

static void collect(TreeNode n, int row, int col, List<int[]> out) {
    if (n == null) return;
    out.add(new int[]{col, row, n.val});
    collect(n.left, row + 1, col - 1, out);
    collect(n.right, row + 1, col + 1, out);
}
```

### Line by line

| Line | What it does |
|---|---|
| `collect(root, 0, 0, nodes)` | record (col, row, val) for every node |
| `nodes.sort(...)` | order by column, then row, then value |
| `prevCol == null \|\| n[0] != prevCol` | the first node of a new column |
| `result.get(result.size() - 1).add(n[2])` | append the value to the current column |

### Dry run: `[1, 2, 3, 4, 6, 5, 7]` (the tie appears in reverse)

```
         1
       /   \
      2     3
     / \   / \
    4   6 5   7         6 is at (2, 0) via 2's right; 5 is at (2, 0) via 3's left
```

| triples collected (DFS order) | after sorting |
|---|---|
| (0,0,1) (−1,1,2) (−2,2,4) (0,2,6) (1,1,3) (0,2,5) (2,2,7) | (−2,2,4) · (−1,1,2) · (0,0,1) (0,2,**5**) (0,2,**6**) · (1,1,3) · (2,2,7) |

Groups: `[[4], [2], [1, 5, 6], [3], [7]]` ✓ (the value tiebreak puts 5 before 6).

### Complexity
- **Time O(n log n)** (the sort).
- **Space O(n).**

---

## 6. Comparing the approaches

| | TreeMap + heap | Triples + sort |
|---|---|---|
| Time | O(n log n) | O(n log n) |
| Space | O(n) | O(n) |
| Code | nested generics | one comparator |

The file checks the sort version against the TreeMap version on 500 random trees (values 0..20, so ties are common).

---

## 7. Edge cases and common mistakes

**Edge cases:** a single node, a chain going left (every node in its own column), many nodes on the same (row, col).

**Mistakes:**
1. Ordering ties by BFS order instead of by value.
2. Sorting only by column (the rows get mixed up).
3. A comparator using subtraction (fine here, but `Integer.compare` is the safe habit).
4. Comparing two `Integer` objects with `!=` (here one side is an `int`, so it's safe).

---

## 8. Interview questions and answers

**Q1. Why can't BFS alone solve this version?**
> BFS gives top-to-bottom order and left-to-right within a row, but nodes sharing the same (row, col) must be sorted by value, which BFS order doesn't guarantee.

**Q2. Can you avoid the full sort?**
> Track the min and max column during the DFS and bucket by column (an array of lists), then sort only within each column by (row, val). It's still O(n log n) in the worst case, but with smaller sorts.

### Follow-up questions

**F1. LC 314 (ties in left-to-right order).**
> BFS with (node, col) pairs and a `TreeMap<col, list>`, or track min/max col. No value sort is needed.

**F2. The top view and the bottom view of a tree.**
> BFS by column: keep the first node seen (top) or the last one (bottom) per column.

**F3. The sum per column.**
> The same coordinates, accumulating in a map of col → sum.

---

## 9. Running the file

```bash
java H03_VerticalOrderTraversal.java      # 4/4 tests passed (including 500 random trees vs TreeMap+heap)
```

**One-line summary:** *"Give each node (col, row), sort all the (col, row, val) triples, and cut the sorted list into groups wherever the column changes."*
