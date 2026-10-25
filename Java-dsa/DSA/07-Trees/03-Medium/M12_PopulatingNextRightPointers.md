# M12 · Populating Next Right Pointers in Each Node

| | |
|---|---|
| **LeetCode** | [#116 Populating Next Right Pointers in Each Node](https://leetcode.com/problems/populating-next-right-pointers-in-each-node/) |
| **Difficulty** | Medium |
| **Pattern** | Use the level you've already linked to link the next one (O(1) space) |
| **Code** | [M12_PopulatingNextRightPointers.java](M12_PopulatingNextRightPointers.java) |
| **Run** | `java M12_PopulatingNextRightPointers.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](../02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem). This file uses its own `Node` class with an extra `next` field.

---

## 1. The problem

The tree is **perfect**: all leaves are on the same level and every parent has two children. Set each node's `next` pointer to the node on its **right** at the same level (null at the end of a level).

```
          1 → null
         / \
        2 → 3 → null
       / \  / \
      4→5→6→7 → null
```

LeetCode prints the result level by level along the next pointers, with `#` marking the end of each level: `[1,#,2,3,#,4,5,6,7,#]`.

---

## 2. How to think about it

### The easy way: BFS
Level by level ([M01](M01_BinaryTreeLevelOrderTraversal.md)); link each polled node to the next one in the same level. O(n) time, but O(w) space for the queue.

### O(1) space: a linked level is a linked list
Once a level is linked, you can **walk it** with `next` pointers, with no queue. While walking level L, wire up level L + 1. For each node `cur` on the current level:

```
cur.left.next  = cur.right              (siblings: the same parent)
cur.right.next = cur.next.left          (across parents, if cur.next exists)
```

Then go down one level: `leftmost = leftmost.left`.

```
level 1 (just 1):   2.next = 3                       (1.next is null: nothing across)
level 2 (2 → 3):    at 2: 4.next = 5, 5.next = 3.left = 6
                    at 3: 6.next = 7                 (3.next is null)
level 3: leaves → stop
```

Perfection matters: every node on a non-leaf level has both children, so `cur.left`, `cur.right` and `cur.next.left` always exist.

(LC 117 is the same for **any** tree. There, keep a dummy head and a tail for the next level while walking the current one.)

---

## 3. Java concepts used in this problem

### 3.1 A node class with an extra pointer

```java
static class Node {
    int val;
    Node left, right, next;
    Node(int val) { this.val = val; }
}
```

`next` starts as null (the default for object fields), which is already correct for the last node of each level.

### 3.2 A `for` loop over a linked structure

```java
for (Node cur = leftmost; cur != null; cur = cur.next)
```

The same shape as walking a linked list: start, a condition, and "advance" = follow the pointer.

### 3.3 The serialization helper (in the test)

It walks each level through the `next` pointers (going down via `leftmost.left`), appending `#` after each level. `sb.substring(0, sb.length() - 1)` drops the trailing comma.

### 3.4 `1 << k` = 2ᵏ

In the test, level k of a perfect tree has `1 << k` nodes (the bit shift computes powers of two).

---

## 4. Approach 1: BFS by levels

```java
static Node connectBfs(Node root) {
    if (root == null) return null;
    Queue<Node> q = new ArrayDeque<>();
    q.add(root);
    while (!q.isEmpty()) {
        Node prevInLevel = null;
        for (int i = q.size(); i > 0; i--) {
            Node n = q.poll();
            if (prevInLevel != null) prevInLevel.next = n;
            prevInLevel = n;
            if (n.left != null) { q.add(n.left); q.add(n.right); }
        }
    }
    return root;
}
```

| Line | What it does |
|---|---|
| `prevInLevel` | the previous node in this level (null at the start of each level) |
| `prevInLevel.next = n` | link the left neighbour to n |
| the children | the next level (a perfect tree has both or neither) |

The last node of each level keeps `next = null`. **O(n)** time, **O(w)** space.

---

## 5. Approach 2: Walk the linked level (the file's solution)

### Code ([M12_PopulatingNextRightPointers.java:48-58](M12_PopulatingNextRightPointers.java#L48-L58))

```java
static Node connect(Node root) {
    Node leftmost = root;
    while (leftmost != null && leftmost.left != null) {     // while there is a level below
        for (Node cur = leftmost; cur != null; cur = cur.next) {   // walk the linked level
            cur.left.next = cur.right;                       // siblings
            if (cur.next != null) cur.right.next = cur.next.left;   // across parents
        }
        leftmost = leftmost.left;                            // go down one level
    }
    return root;
}
```

### Line by line

| Line | What it does |
|---|---|
| `leftmost` | the first node of the current level |
| `leftmost.left != null` | there's a level below (in a perfect tree, this fails exactly at the leaves) |
| the `for` | walk the current level via the next pointers, which are already set |
| `cur.left.next = cur.right;` | link the two children of cur |
| `cur.right.next = cur.next.left;` | link cur's right child to the left child of cur's neighbour |
| `leftmost = leftmost.left;` | the first node of the next level |

### Dry run: the depth-3 perfect tree (values 1..7)

| level walked | cur | links set |
|---|---|---|
| [1] | 1 | 2.next = 3 (1.next is null, so no cross link) |
| [2 → 3] | 2 | 4.next = 5; 5.next = 3.left = 6 |
| | 3 | 6.next = 7 (3.next is null) |
| [4 → 5 → 6 → 7] | — | leftmost.left is null → stop |

Serialized: `[1,#,2,3,#,4,5,6,7,#]` ✓

### Complexity
- **Time O(n):** every node is visited once as cur (except the leaves).
- **Space O(1).**

---

## 6. Approach 3: Recursive (also O(1) extra besides the stack)

```java
static void connectRec(Node n) {
    if (n == null || n.left == null) return;
    n.left.next = n.right;
    if (n.next != null) n.right.next = n.next.left;
    connectRec(n.left);
    connectRec(n.right);
}
```

The same two links, done in preorder. Preorder guarantees that `n.next` is set before n's children are processed. The recursion uses O(log n) stack, since the tree is perfect.

---

## 7. Comparing the approaches

| | BFS | Walk the linked level | Recursive |
|---|---|---|---|
| Time | O(n) | O(n) | O(n) |
| Extra space | O(w) = O(n) | **O(1)** | O(log n) |
| Works for any tree (LC 117)? | yes | no (needs the dummy-head variant) | no |

The file checks perfect trees of depths 1 to 12 (up to 4,095 nodes).

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty tree, a single node (nothing to link), depth 2.

**Mistakes:**
1. Forgetting the cross-parent link (`cur.right.next = cur.next.left`).
2. Accessing `cur.next.left` without checking `cur.next != null`.
3. Using this exact code on a non-perfect tree (null children).

---

## 9. Interview questions and answers

**Q1. How does the O(1) version avoid a queue?**
> The level above is already a linked list through the next pointers, so walking it replaces the queue. Each step links the children of the current node.

**Q2. What changes for an arbitrary tree (LC 117)?**
> Children may be missing, so keep a dummy head and a `tail` pointer for the next level. For each cur, append `cur.left` and `cur.right` (when present) after tail. Then move to `dummy.next` for the next level.

### Follow-up questions

**F1. Connect each level into a circular list.**
> After linking each level, set the last node's next to `leftmost`.

**F2. Link nodes to their left neighbour instead.**
> Symmetric: `cur.right.prev = cur.left`, and `cur.left.prev = cur.prev.right`.

**F3. Right side view using next pointers.** → [M02](M02_BinaryTreeRightSideView.md)
> The last node of each linked level (the one whose next is null) is visible from the right.

---

## 10. Running the file

```bash
java M12_PopulatingNextRightPointers.java      # 4/4 tests passed (including depths 1..12, up to 4,095 nodes)
```

**One-line summary:** *"Walk each linked level through next pointers, setting left.next = right and right.next = next.left for the level below: O(1) space."*
