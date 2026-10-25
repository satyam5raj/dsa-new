# E11 · Cousins in Binary Tree

| | |
|---|---|
| **LeetCode** | [#993 Cousins in Binary Tree](https://leetcode.com/problems/cousins-in-binary-tree/) |
| **Difficulty** | Easy |
| **Pattern** | Record (depth, parent) for the two targets |
| **Code** | [E11_CousinsInBinaryTree.java](E11_CousinsInBinaryTree.java) |
| **Run** | `java E11_CousinsInBinaryTree.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

All values are unique. x and y are **cousins** if they are at the **same depth** but have **different parents**.

```
          1
         / \
        2   3          x = 5, y = 4 → true   (both at depth 2; parents 3 and 2)
         \   \         x = 2, y = 3 → false  (the same parent: they're siblings)
          4   5
```

| tree | x, y | answer |
|---|---|---|
| `[1,2,3,null,4,null,5]` | 5, 4 | true |
| `[1,2,3,null,4,null,5]` | 2, 3 | false (siblings) |
| `[1,2,3,4]` | 4, 3 | false (different depths) |

**Constraints:** 2 to 100 nodes, unique values.

---

## 2. How to think about it

Two facts per target decide the answer: its **depth** and its **parent**.

### DFS (top-down)
Walk the tree passing `(depth, parent)` **down**. When you meet x or y, write down its depth and parent. At the end:

```
depthX == depthY  &&  parentX != parentY
```

### BFS alternative
Process one level at a time:
- If both x and y appear on this level → cousins, **unless** they're children of the same node (check that while looking at each node's two children).
- If only one of them appears → they're at different depths → false.

---

## 3. Java concepts used in this problem

### 3.1 Passing the parent down as a parameter

```
static void dfs(TreeNode n, TreeNode parent, int depth, int x, int y)
dfs(n.left, n, depth + 1, x, y);       // for the child, "n" is the parent
```

A node doesn't know its parent (TreeNode has no parent pointer), so the caller passes it down. The root's parent is `null`.

### 3.2 Static fields for the results, and resetting them

```
static int depthX, depthY;
static TreeNode parentX, parentY;
depthX = depthY = -1;        // chained assignment: both become -1
```

As in [E05](E05_DiameterOfBinaryTree.md), static fields collect the results from deep inside the recursion; they must be reset on every call.

### 3.3 Comparing nodes with `!=`

`parentX != parentY` compares **references**: are they the same node object? That's exactly what "same parent" means. (Comparing `.val` would also work here, because the values are unique.)

### 3.4 `int[] next = {1}`: a mutable counter for a recursive helper

The test's `relabel` gives every node a unique value. An `int` parameter would be copied per call, but an `int[]` of length 1 is a shared object, so `next[0]++` increments one counter across all calls.

---

## 4. Approach 1: DFS recording (depth, parent) (the file's main solution)

### Code ([E11_CousinsInBinaryTree.java:35-51](E11_CousinsInBinaryTree.java#L35-L51))

```java
static int depthX, depthY;
static TreeNode parentX, parentY;

static boolean isCousins(TreeNode root, int x, int y) {
    depthX = depthY = -1;
    parentX = parentY = null;
    dfs(root, null, 0, x, y);
    return depthX == depthY && depthX != -1 && parentX != parentY;
}

static void dfs(TreeNode n, TreeNode parent, int depth, int x, int y) {
    if (n == null) return;
    if (n.val == x) { depthX = depth; parentX = parent; }
    if (n.val == y) { depthY = depth; parentY = parent; }
    dfs(n.left, n, depth + 1, x, y);
    dfs(n.right, n, depth + 1, x, y);
}
```

### Line by line

| Line | What it does |
|---|---|
| the resets | −1 = "not found yet" |
| `dfs(root, null, 0, x, y)` | the root: depth 0, no parent |
| `if (n.val == x) { … }` | found x: record where it is |
| the two recursive calls | the children are one level deeper, with n as their parent |
| `depthX == depthY && depthX != -1` | the same depth, and both were actually found |
| `parentX != parentY` | different parents |

### Dry run: `[1,2,3,null,4,null,5]`, x = 5, y = 4

| visit | depth | parent | records |
|---|---|---|---|
| 1 | 0 | null | |
| 2 | 1 | 1 | |
| 4 | 2 | 2 | depthY = 2, parentY = 2 |
| 3 | 1 | 1 | |
| 5 | 2 | 3 | depthX = 2, parentX = 3 |

2 == 2 and node 3 ≠ node 2 → **true** ✓

### Dry run: x = 2, y = 3
depthX = depthY = 1, but parentX = parentY = node 1 → **false** (siblings) ✓

### Complexity
- **Time O(n).** (It could stop early once both are found.)
- **Space O(h).**

---

## 5. Approach 2: BFS by levels

### Code ([E11_CousinsInBinaryTree.java:53-73](E11_CousinsInBinaryTree.java#L53-L73))

```java
static boolean isCousinsBfs(TreeNode root, int x, int y) {
    Queue<TreeNode> q = new ArrayDeque<>();
    q.add(root);
    while (!q.isEmpty()) {
        boolean seenX = false, seenY = false;
        for (int i = q.size(); i > 0; i--) {
            TreeNode n = q.poll();
            if (n.val == x) seenX = true;
            if (n.val == y) seenY = true;
            if (n.left != null && n.right != null) {          // siblings check
                int a = n.left.val, b = n.right.val;
                if ((a == x && b == y) || (a == y && b == x)) return false;
            }
            if (n.left != null) q.add(n.left);
            if (n.right != null) q.add(n.right);
        }
        if (seenX && seenY) return true;
        if (seenX || seenY) return false;                    // only one on this level
    }
    return false;
}
```

### Line by line

| Line | What it does |
|---|---|
| `seenX`, `seenY` | reset for each level |
| the siblings check | if x and y are this node's two children, they're siblings → false |
| `if (seenX && seenY) return true;` | both on this level, and the sibling check never fired |
| `if (seenX \|\| seenY) return false;` | only one of them on this level → different depths |

The sibling check runs while the **parent's** level is being processed, one level **before** x and y are seen. So by the time both are seen together, siblings have already been ruled out.

### Dry run: x = 5, y = 4

| level | nodes | seen | sibling pairs checked |
|---|---|---|---|
| 0 | [1] | — | (2, 3): not {4, 5} |
| 1 | [2, 3] | — | 2 has only a right child, 3 has only a right child: no pairs |
| 2 | [4, 5] | x and y | → **true** |

### Complexity
- **Time O(n).**
- **Space O(w).**

(It assumes a non-empty root: `ArrayDeque.add(null)` would throw. LeetCode guarantees at least 2 nodes.)

---

## 6. Comparing the approaches

| | DFS with (depth, parent) | BFS by levels |
|---|---|---|
| Time | O(n) | O(n), and it stops at the targets' level |
| Space | O(h) | O(w) |
| Logic | compare at the end | decided level by level |

The file checks both on 1,000 random trees with unique labels.

---

## 7. Edge cases and common mistakes

**Edge cases:** siblings (false), different depths (false), one target is the root (depth 0; no cousins possible), both at the deepest level.

**Mistakes:**
1. Checking only the depth (siblings would wrongly count).
2. Checking only the parent.
3. Not resetting the static fields between calls.
4. Comparing parents by value when values could repeat (here they're unique).

---

## 8. Interview questions and answers

**Q1. Why pass the parent down instead of storing parent pointers?**
> TreeNode has no parent field. Passing it as a parameter costs nothing and is available exactly when needed.

**Q2. How does the BFS rule out siblings?**
> While processing a node, it checks whether that node's two children are exactly x and y. That happens one level before x and y are examined.

### Follow-up questions

**F1. Return all the cousins of a given node (LC 2641-style).**
> BFS: on the target's level, collect every node except the target and its sibling.

**F2. Replace each node's value with the sum of its cousins' values (LC 2641).**
> For each level: the level sum minus the sum of the node's own sibling group.

**F3. Distance between two nodes.** → [M14 All Nodes Distance K](../03-Medium/M14_AllNodesDistanceK.md)
> Build parent pointers (a map), then BFS outward from a node.

---

## 9. Running the file

```bash
java E11_CousinsInBinaryTree.java      # 5/5 tests passed (including 1,000 random trees: DFS == BFS)
```

**One-line summary:** *"Record each target's depth and parent (pass the parent down in a DFS); cousins ⇔ same depth and different parents."*
