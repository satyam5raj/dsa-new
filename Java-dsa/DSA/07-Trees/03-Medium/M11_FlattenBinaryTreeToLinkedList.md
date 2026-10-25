# M11 · Flatten Binary Tree to Linked List

| | |
|---|---|
| **LeetCode** | [#114 Flatten Binary Tree to Linked List](https://leetcode.com/problems/flatten-binary-tree-to-linked-list/) |
| **Difficulty** | Medium |
| **Pattern** | Rewire in place in preorder (Morris-style, O(1) space) |
| **Code** | [M11_FlattenBinaryTreeToLinkedList.java](M11_FlattenBinaryTreeToLinkedList.java) |
| **Run** | `java M11_FlattenBinaryTreeToLinkedList.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](../02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Flatten the tree **in place** into a "linked list" that uses the **right** pointers, in **preorder** order, with every left pointer set to null.

```
        1
       / \               1 → 2 → 3 → 4 → 5 → 6
      2   5        →     (all through .right; every .left is null)
     / \   \
    3   4   6
```

The preorder of the tree is 1, 2, 3, 4, 5, 6, and that's the order of the list.

---

## 2. How to think about it

### Approach 1: collect, then relink
Collect the preorder into a list of nodes, then set `list[i].right = list[i+1]` and `left = null`. O(n) extra space.

### Approach 2: reverse preorder with a `prev` pointer
Visit the nodes in **reverse** preorder: right subtree, left subtree, node. Then, when a node is processed, the node that should come **after** it in the list has just been processed:

```
node.right = prev;   node.left = null;   prev = node;
```

It builds the list from the back (6, 5, 4, …). O(h) recursion space.

### Approach 3: O(1) space (Morris-like)
Walk down the right spine with `cur`. If `cur` has a left subtree:
1. find the **rightmost** node of that left subtree (the last node of its preorder)
2. hang cur's right subtree there: `rightmost.right = cur.right`
3. move the left subtree to the right: `cur.right = cur.left; cur.left = null`

Then step to `cur.right` and repeat.

```
cur = 1: left subtree 2(3, 4); its rightmost is 4 → 4.right = 5(…)
         1.right = 2, 1.left = null       →   1 – 2(3, 4 – 5 – 6)
cur = 2: left 3; rightmost is 3 → 3.right = 4
         2.right = 3, 2.left = null       →   1 – 2 – 3 – 4 – 5 – 6
cur = 3, 4, 5, 6: no left children → done
```

**Why the rightmost node?** In preorder, cur's right subtree comes right after the **last** node of cur's left subtree, and that last node is the rightmost one (keep going right from the left child).

---

## 3. Java concepts used in this problem

### 3.1 Mutating a tree in place

The method returns `void`: it changes the existing nodes' pointers, and the caller's `root` reference now points at the head of the list. No new nodes are created.

### 3.2 A static `prev` across the recursion

```java
static TreeNode prev;
```

As in [M03](M03_ValidateBinarySearchTree.md)'s inorder check: the recursion needs to remember the last processed node across calls, so it's a field, reset at the start.

### 3.3 Why reverse preorder is safe

`reversePre(n)` calls `reversePre(n.right)` and `reversePre(n.left)` **before** changing n's own pointers. The children's processing never touches n, so `n.left` is still the original left child when it's needed.

### 3.4 Testing a "list" made of tree nodes

```java
for (TreeNode n = root; n != null; n = n.right)
```

`asList` walks the right pointers like a linked list and also checks that every left pointer is null.

---

## 4. Approach 1: Collect the preorder, then relink

```java
static void flattenWithList(TreeNode root) {
    List<TreeNode> nodes = new ArrayList<>();
    collect(root, nodes);
    for (int i = 0; i < nodes.size(); i++) {
        nodes.get(i).left = null;
        nodes.get(i).right = i + 1 < nodes.size() ? nodes.get(i + 1) : null;
    }
}

static void collect(TreeNode n, List<TreeNode> out) {
    if (n == null) return;
    out.add(n);
    collect(n.left, out);
    collect(n.right, out);
}
```

Straightforward and hard to get wrong. **O(n)** time, **O(n)** extra space.

---

## 5. Approach 2: Reverse preorder with `prev`

### Code ([M11_FlattenBinaryTreeToLinkedList.java:62-76](M11_FlattenBinaryTreeToLinkedList.java#L62-L76))

```java
static TreeNode prev;

static void flattenRecursive(TreeNode root) {
    prev = null;
    reversePre(root);
}

static void reversePre(TreeNode n) {
    if (n == null) return;
    reversePre(n.right);
    reversePre(n.left);
    n.right = prev;            // link to the node that comes next in preorder
    n.left = null;
    prev = n;
}
```

### Line by line

| Line | What it does |
|---|---|
| `reversePre(n.right); reversePre(n.left);` | process everything that comes **after** n in preorder, in reverse order |
| `n.right = prev;` | prev is the node right after n in preorder |
| `n.left = null;` | lists have no left pointers |
| `prev = n;` | n is now the head of the already-built tail |

### Dry run

| processed | prev before | links | prev after |
|---|---|---|---|
| 6 | null | 6 → null | 6 |
| 5 | 6 | 5 → 6 | 5 |
| 4 | 5 | 4 → 5 | 4 |
| 3 | 4 | 3 → 4 | 3 |
| 2 | 3 | 2 → 3 | 2 |
| 1 | 2 | 1 → 2 | 1 |

→ 1 → 2 → 3 → 4 → 5 → 6 ✓ (built from the back).

### Complexity
- **Time O(n).**
- **Space O(h)** recursion.

---

## 6. Approach 3: O(1) space with the rightmost node (the file's main solution)

### Code ([M11_FlattenBinaryTreeToLinkedList.java:47-59](M11_FlattenBinaryTreeToLinkedList.java#L47-L59))

```java
static void flatten(TreeNode root) {
    TreeNode cur = root;
    while (cur != null) {
        if (cur.left != null) {
            TreeNode rightmost = cur.left;
            while (rightmost.right != null) rightmost = rightmost.right;
            rightmost.right = cur.right;     // hang the right subtree after the left part
            cur.right = cur.left;            // the left part comes first in preorder
            cur.left = null;
        }
        cur = cur.right;
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `while (cur != null)` | walk down the (growing) right spine |
| `rightmost` loop | the last node of cur's left subtree in preorder |
| `rightmost.right = cur.right;` | cur's right subtree now follows the left subtree |
| `cur.right = cur.left; cur.left = null;` | the left subtree moves into the right slot |
| `cur = cur.right;` | continue with the next node of the list |

### Dry run: `[1, 2, 5, 3, 4, null, 6]`

| cur | left? | rightmost of the left subtree | rewiring | the right spine afterwards |
|---|---|---|---|---|
| 1 | 2(3, 4) | 4 | 4.right = 5; 1.right = 2; 1.left = null | 1 → 2 (2 still has left child 3; 2.right = 4 → 5 → 6) |
| 2 | 3 | 3 | 3.right = 4; 2.right = 3; 2.left = null | 1 → 2 → 3 → 4 → 5 → 6 |
| 3, 4 | none | | | |
| 5 | none (5's right child is 6) | | | |
| 6 | none | | | done |

### Complexity
- **Time O(n):** each node is walked over as part of a "find the rightmost" search at most once (after the move, it's on the main spine and the searches start below it).
- **Space O(1).**

---

## 7. Comparing the approaches

| | Collect + relink | Reverse preorder | Rightmost rewiring |
|---|---|---|---|
| Time | O(n) | O(n) | O(n) |
| Extra space | O(n) | O(h) | **O(1)** |
| Ease | easiest | elegant | needs the insight |

The file checks the rewiring and reverse-preorder versions against the real preorder on 500 random trees.

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty tree, a single node, a left-only chain (it becomes a right chain), a right-only chain (unchanged).

**Mistakes:**
1. Forgetting `cur.left = null` (the result keeps stray left pointers).
2. Hanging the right subtree on cur.left instead of on the **rightmost** node of the left subtree.
3. In the recursive version, processing the node before its subtrees (that overwrites `n.right` before it's visited).

---

## 9. Interview questions and answers

**Q1. Why does the rightmost node of the left subtree get cur's right subtree?**
> In preorder, everything in cur's left subtree comes before everything in its right subtree, and the last node of the left subtree's preorder is its rightmost node. So the right subtree continues from there.

**Q2. How is this related to Morris traversal?**
> Both find a predecessor-like node (the rightmost node of the left subtree) and rewire pointers to avoid a stack, giving O(1) extra space ([BinaryTreeBasics](../01-Basics/BinaryTreeBasics.java) covers Morris inorder).

### Follow-up questions

**F1. Flatten into inorder order.**
> Inorder with a `prev` pointer, linking `prev.right = n` and `n.left = null` (BST → sorted list).

**F2. Flatten into a doubly linked list (LC 426, BST to a circular doubly linked list).**
> Inorder with prev, setting both `prev.right = n` and `n.left = prev`; link the head and tail at the end.

**F3. Flatten a multilevel doubly linked list (LC 430).**
> The same "splice the child list in, then continue" idea, on a list with child pointers.

---

## 10. Running the file

```bash
java M11_FlattenBinaryTreeToLinkedList.java      # 4/4 tests passed (including 500 random trees == preorder)
```

**One-line summary:** *"For each node on the right spine, hang its right subtree on the rightmost node of its left subtree, then move the left subtree to the right: O(1) space, preorder order."*
