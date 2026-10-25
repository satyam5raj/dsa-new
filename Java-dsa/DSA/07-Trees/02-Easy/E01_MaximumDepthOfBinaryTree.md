# E01 · Maximum Depth of Binary Tree

| | |
|---|---|
| **LeetCode** | [#104 Maximum Depth of Binary Tree](https://leetcode.com/problems/maximum-depth-of-binary-tree/) |
| **Difficulty** | Easy |
| **Pattern** | Bottom-up recursion (the "hello world" of trees) |
| **Code** | [E01_MaximumDepthOfBinaryTree.java](E01_MaximumDepthOfBinaryTree.java) |
| **Run** | `java E01_MaximumDepthOfBinaryTree.java` |

This is the first note of the Trees topic, so section 3 covers **`TreeNode`, tree vocabulary, how recursion runs on a tree, and the file's helpers** in full. Later notes give only short recaps.

---

## 1. The problem

Return the **number of nodes** on the longest path from the root down to a leaf.

```
        3
      /   \             → 3   (3 → 20 → 15, or 3 → 20 → 7)
     9     20
          /  \
         15   7
```

| tree (LeetCode format) | answer |
|---|---|
| `[3,9,20,null,null,15,7]` | 3 |
| `[1,null,2]` | 2 |
| `[]` | 0 |

**Constraints:** up to 10⁴ nodes.

---

## 2. How to think about it

### Think recursively (the checklist from [TreeRecursionPatterns](../01-Basics/TreeRecursionPatterns.java))
1. **Define the function:** `depth(node)` = the max depth of the subtree rooted at node.
2. **Base case:** `depth(null) = 0` (an empty tree has no nodes).
3. **Assume** `depth(left)` and `depth(right)` are already correct (the "leap of faith"). Then:

```
depth(node) = 1 + max(depth(left), depth(right))
```

That's the whole solution. Don't trace the entire recursion in your head: trust that it works for the children, and only check that one node combines them correctly.

### Bottom-up
Each node's answer is built from its children's answers, so information flows **up** from the leaves. Most tree problems are this shape: "compute something for each subtree from the children's results."

### The alternative: BFS by levels
Visit the tree level by level with a queue and count the levels. No recursion, so no stack overflow on very deep trees.

---

## 3. Java concepts used in this problem

### 3.1 `TreeNode`: the building block

```java
static class TreeNode {
    int val;
    TreeNode left, right;                // null when there's no child
    TreeNode(int val) { this.val = val; }
    TreeNode(int val, TreeNode left, TreeNode right) { this.val = val; this.left = left; this.right = right; }
}
```

Like the Linked Lists topic's `ListNode`, but with **two** "next" pointers. A tree is just a root reference: `TreeNode root`. An empty tree is `root == null`.

```java
TreeNode root = new TreeNode(3,
        new TreeNode(9),
        new TreeNode(20, new TreeNode(15), new TreeNode(7)));
```

Every tree file contains the same `TreeNode` and helpers (at the bottom), so each file runs on its own.

### 3.2 Tree vocabulary

```
          3          ← root (depth 0, or "level 1")
        /   \
       9     20      ← 9 is a LEAF (no children); 20 is an INTERNAL node
            /  \
           15   7    ← leaves
```

| Term | Meaning |
|---|---|
| **root** | the top node (no parent) |
| **leaf** | a node with no children |
| **depth of a node** | the number of edges from the root to it |
| **height / max depth of a tree** | the number of nodes (LeetCode's convention here) or edges on the longest root-to-leaf path. **Read each problem's definition**: this one counts nodes |
| **subtree** | a node plus all its descendants |
| **balanced** | the left and right heights differ by at most 1 at every node ([E06](E06_BalancedBinaryTree.md)) |
| **BST** | left subtree < node < right subtree, everywhere ([E08](E08_ConvertSortedArrayToBST.md)) |

A tree of n nodes has height between **log₂ n** (balanced) and **n** (a chain, like a linked list).

### 3.3 How recursion runs on a tree: the call stack

Each call to `maxDepth` waits for its two child calls to return. The pending calls live on the **call stack**:

```
maxDepth(3)
├── maxDepth(9)
│   ├── maxDepth(null) → 0
│   └── maxDepth(null) → 0
│   returns 1 + max(0, 0) = 1
└── maxDepth(20)
    ├── maxDepth(15) → 1
    └── maxDepth(7)  → 1
    returns 1 + max(1, 1) = 2
returns 1 + max(1, 2) = 3
```

At most **h + 1** calls are pending at once (one per level of the current path), so recursion uses **O(h) space**. For a 10⁴-node chain, that's 10⁴ stack frames. Java handles that, but a chain of about 10⁵–10⁶ nodes can throw `StackOverflowError`, which is why the iterative BFS version exists.

### 3.4 The LeetCode level-order format and the `build(...)` helper

LeetCode writes trees level by level, with `null` for missing children:

```
[3, 9, 20, null, null, 15, 7]
 3 has children 9 and 20;  9 has children null, null;  20 has children 15 and 7
```

The file's helper rebuilds that with a queue:

```java
static TreeNode build(Integer... vals)        // build(3, 9, 20, null, null, 15, 7)
```

- **`Integer...` is a varargs parameter:** you can pass any number of arguments, and inside the method `vals` is an `Integer[]`. It's `Integer` (not `int`) so that `null` can be passed.
- It takes nodes from the queue in order and attaches the next two values as their left and right children.

`TreeNode.toString()` does the reverse: a level-order walk that prints `null` for missing children and trims the trailing nulls. It uses a `LinkedList` as the queue because, unlike `ArrayDeque`, **`LinkedList` accepts null elements**.

### 3.5 `randomTree(rnd, size, lo, hi)`: the test helper

It builds a random tree shape with exactly `size` nodes, by randomly splitting `size − 1` nodes between the left and right subtrees. The tests use it to cross-check two implementations on hundreds of shapes.

### 3.6 Level-by-level BFS: `for (int i = q.size(); i > 0; i--)`

```java
while (!q.isEmpty()) {
    for (int i = q.size(); i > 0; i--) {     // q.size() is read ONCE: the size of the current level
        TreeNode n = q.poll();
        // add n's children (they belong to the NEXT level)
    }
    levels++;
}
```

Reading `q.size()` once, at the start of the inner loop, freezes the number of nodes in the current level, even though the children are added to the same queue during the loop. This is **the** template for level-order problems ([M01](../03-Medium/M01_BinaryTreeLevelOrderTraversal.md)).

---

## 4. Approach 1: Recursive DFS (the file's main solution)

### Code ([E01_MaximumDepthOfBinaryTree.java:38-41](E01_MaximumDepthOfBinaryTree.java#L38-L41))

```java
static int maxDepth(TreeNode root) {
    if (root == null) return 0;
    return 1 + Math.max(maxDepth(root.left), maxDepth(root.right));
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (root == null) return 0;` | the base case: an empty subtree has depth 0. It also handles leaves: both children are null |
| `maxDepth(root.left)`, `maxDepth(root.right)` | the depths of the two subtrees (trust them) |
| `1 + Math.max(...)` | this node plus the deeper side |

### Dry run: `[3, 9, 20, null, null, 15, 7]`

Calls finish **bottom-up** (postorder: children before parent):

| call | left result | right result | returns |
|---|---|---|---|
| maxDepth(9) | 0 (null) | 0 (null) | 1 |
| maxDepth(15) | 0 | 0 | 1 |
| maxDepth(7) | 0 | 0 | 1 |
| maxDepth(20) | 1 | 1 | 2 |
| maxDepth(3) | 1 | 2 | **3** |

### Dry run: `[1, null, 2]` (a right-leaning chain)

```
1
 \
  2
```

maxDepth(2) = 1 + max(0, 0) = 1; maxDepth(1) = 1 + max(0, 1) = **2** ✓

### Complexity
- **Time O(n):** every node is visited once.
- **Space O(h):** the recursion depth. O(log n) when balanced, O(n) for a chain.

---

## 5. Approach 2: BFS, counting the levels

### Code ([E01_MaximumDepthOfBinaryTree.java:43-57](E01_MaximumDepthOfBinaryTree.java#L43-L57))

```java
static int maxDepthBfs(TreeNode root) {
    if (root == null) return 0;
    Queue<TreeNode> q = new ArrayDeque<>();
    q.add(root);
    int levels = 0;
    while (!q.isEmpty()) {
        for (int i = q.size(); i > 0; i--) {          // one full level per outer loop
            TreeNode n = q.poll();
            if (n.left != null) q.add(n.left);
            if (n.right != null) q.add(n.right);
        }
        levels++;
    }
    return levels;
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (root == null) return 0;` | an empty tree (and `ArrayDeque` can't hold null) |
| `q.add(root);` | level 1 |
| `for (int i = q.size(); i > 0; i--)` | process exactly the nodes of the current level |
| `if (n.left != null) q.add(n.left);` | queue the children for the next level (never queue null) |
| `levels++;` | one more level finished |

### Dry run: `[3, 9, 20, null, null, 15, 7]`

| level | queue at the start | polled | children added | levels |
|---|---|---|---|---|
| 1 | [3] | 3 | 9, 20 | 1 |
| 2 | [9, 20] | 9, 20 | 15, 7 | 2 |
| 3 | [15, 7] | 15, 7 | — | 3 |
| — | [] → stop | | | **3** |

### Complexity
- **Time O(n).**
- **Space O(w):** the maximum width of a level (up to n/2 for the last level of a full tree).

---

## 6. Comparing the approaches

| | Recursive DFS | BFS by levels |
|---|---|---|
| Time | O(n) | O(n) |
| Space | O(h) stack | O(w) queue |
| Deep chain (10⁶ nodes) | may overflow the stack | fine |
| Code length | 2 lines | ~12 lines |

The file checks that both agree on 300 random trees.

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty tree (0), a single node (1), a chain (depth n), a perfect tree (log₂(n + 1)).

**Mistakes:**
1. Returning 1 for null (that counts phantom nodes).
2. Counting edges instead of nodes (read the definition).
3. In BFS, calling `q.size()` inside the loop condition on every iteration (`for (int i = 0; i < q.size(); i++)`): the size changes as children are added.
4. Adding null children to an `ArrayDeque` → NullPointerException.

---

## 8. Interview questions and answers

**Q1. What's the space complexity of the recursive version?**
> O(h), the recursion depth: O(log n) for a balanced tree and O(n) for a degenerate (chain) tree.

**Q2. Why is this called bottom-up?**
> A node's answer is computed from its children's answers after they return, so the results flow from the leaves up to the root.

**Q3. How would you avoid stack overflow on a very deep tree?**
> Use BFS, or an iterative DFS with an explicit stack of (node, depth) pairs.

### Follow-up questions

**F1. Minimum depth.** → [E10](E10_MinimumDepthOfBinaryTree.md)
> Careful: a node with one child isn't a leaf, so `1 + min(left, right)` is wrong when one side is null.

**F2. Maximum depth of an N-ary tree.**
> `1 + max over all the children`, or the same BFS.

**F3. Iterative DFS with a stack.**
> Push (node, depth) pairs; track the max depth seen when popping.

**F4. Diameter (the longest path between any two nodes).** → [E05](E05_DiameterOfBinaryTree.md)
> The same height recursion, plus a global best of `left + right` at every node.

---

## 9. Running the file

```bash
java E01_MaximumDepthOfBinaryTree.java      # 5/5 tests passed (including 300 random trees: DFS == BFS)
```

**One-line summary:** *"depth(null) = 0, depth(node) = 1 + max(depth(left), depth(right)); or BFS and count the levels."*
