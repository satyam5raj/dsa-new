# E03 · Invert Binary Tree

| | |
|---|---|
| **LeetCode** | [#226 Invert Binary Tree](https://leetcode.com/problems/invert-binary-tree/) |
| **Difficulty** | Easy |
| **Pattern** | Visit every node and swap its children (any traversal works) |
| **Code** | [E03_InvertBinaryTree.java](E03_InvertBinaryTree.java) |
| **Run** | `java E03_InvertBinaryTree.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

**Mirror** the tree: every left child becomes the right child and vice versa. Return the root.

```
       4                    4
     /   \                /   \
    2     7     →        7     2
   / \   / \            / \   / \
  1   3 6   9          9   6 3   1
```

| input | output |
|---|---|
| `[4,2,7,1,3,6,9]` | `[4,7,2,9,6,3,1]` |
| `[2,1,3]` | `[2,3,1]` |
| `[1,2]` | `[1,null,2]` (the shape changes) |
| `[]` | `[]` |

(Famous because the author of Homebrew tweeted that he failed it in a Google interview.)

---

## 2. How to think about it

**Insight:** "mirror the whole tree" = **at every node, swap left and right**.

Does the order of the swaps matter? No. Each node's swap only exchanges its own two child pointers, and every node needs its swap exactly once. So any traversal works:
- **recursive preorder:** swap, then invert both children
- **recursive postorder:** invert both children, then swap
- **iterative:** BFS (or a stack); swap each node's children as it's polled

### The inorder trap
```java
invert(root.left);
swap(root);
invert(root.right);     // BUG: this is the ORIGINAL left subtree (already inverted!)
```
After the swap, the old left subtree (already inverted) is now on the right, so this inverts it a second time and leaves the old right subtree untouched. The fix is to call `invert(root.left)` twice. Preorder and postorder avoid the confusion.

---

## 3. Java concepts used in this problem

### 3.1 Swapping two fields with a temporary

```java
TreeNode tmp = root.left;
root.left = root.right;
root.right = tmp;
```

The same three-step swap as for array elements. Without `tmp`, the first assignment would lose the original left child.

### 3.2 Mutating in place and returning the root

`invertTree` changes the existing nodes (no new nodes are created) and returns `root` for convenience, so the call can be chained: `print(invertTree(root))`. The recursive calls' return values are ignored, because they modify the subtrees in place.

### 3.3 Several statements on one line

```java
TreeNode tmp = n.left; n.left = n.right; n.right = tmp;
```

Semicolons separate statements; putting them on one line is just compact formatting.

### 3.4 A test with two identical trees: `new Random(seed)`

```java
long seed = rnd.nextLong();
TreeNode a = randomTree(new Random(seed), 20, 0, 99), b = randomTree(new Random(seed), 20, 0, 99);
```

The same seed gives the same random sequence, so `a` and `b` are identical but **separate** trees. That lets the test invert one recursively and the other iteratively and compare. It also checks that inverting twice gives back the original.

---

## 4. Approach 1: Recursive (preorder swap)

### Code ([E03_InvertBinaryTree.java:36-44](E03_InvertBinaryTree.java#L36-L44))

```java
static TreeNode invertTree(TreeNode root) {
    if (root == null) return null;
    TreeNode tmp = root.left;          // swap the children of this node
    root.left = root.right;
    root.right = tmp;
    invertTree(root.left);             // then fix both subtrees
    invertTree(root.right);
    return root;
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (root == null) return null;` | nothing to invert |
| the three swap lines | exchange this node's children |
| `invertTree(root.left)`, `invertTree(root.right)` | mirror both subtrees (which ones are "left" and "right" no longer matters: both get inverted) |
| `return root;` | the root doesn't change |

### Dry run: `[4, 2, 7, 1, 3, 6, 9]`

| call | children before the swap | after the swap | then recurse into |
|---|---|---|---|
| invert(4) | 2, 7 | 7, 2 | 7, then 2 |
| invert(7) | 6, 9 | 9, 6 | 9, 6 (leaves: they swap null with null) |
| invert(2) | 1, 3 | 3, 1 | 3, 1 (leaves) |

Result: 4 → (7 → 9, 6), (2 → 3, 1) = `[4, 7, 2, 9, 6, 3, 1]` ✓

### Dry run: `[1, 2]`

```
  1            1
 /      →       \
2                2
```

invert(1): swap (2, null) → (null, 2); invert(null); invert(2) → its children are both null. Result `[1, null, 2]` ✓

### Complexity
- **Time O(n).**
- **Space O(h)** for the recursion.

---

## 5. Approach 2: Iterative BFS

### Code ([E03_InvertBinaryTree.java:46-56](E03_InvertBinaryTree.java#L46-L56))

```java
static TreeNode invertIterative(TreeNode root) {
    Queue<TreeNode> q = new ArrayDeque<>();
    if (root != null) q.add(root);
    while (!q.isEmpty()) {
        TreeNode n = q.poll();
        TreeNode tmp = n.left; n.left = n.right; n.right = tmp;
        if (n.left != null) q.add(n.left);
        if (n.right != null) q.add(n.right);
    }
    return root;
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (root != null) q.add(root);` | an empty tree → the loop never runs |
| `TreeNode n = q.poll();` | the next node to fix |
| the swap | mirror its two children |
| the two `add`s | queue the (already swapped) children so they get swapped too |

### Dry run: `[4, 2, 7, 1, 3, 6, 9]`

| poll | swap | queue after |
|---|---|---|
| 4 | (2, 7) → (7, 2) | [7, 2] |
| 7 | (6, 9) → (9, 6) | [2, 9, 6] |
| 2 | (1, 3) → (3, 1) | [9, 6, 3, 1] |
| 9, 6, 3, 1 | leaves: null ↔ null | [] |

The same result ✓

### Complexity
- **Time O(n).**
- **Space O(w)** (the widest level).

---

## 6. Comparing the approaches

| | Recursive | Iterative BFS |
|---|---|---|
| Time | O(n) | O(n) |
| Space | O(h) | O(w) |
| Risk | stack overflow on very deep trees | none |

The file's tests check, on 300 random trees, that recursive == iterative and that inverting twice gives the original.

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty tree, a single node, a one-sided chain (it flips to the other side).

**Mistakes:**
1. The inorder version calling `invert(root.right)` after the swap.
2. Swapping without a temporary (losing a subtree).
3. Building a new tree but returning the old root (or vice versa).

---

## 8. Interview questions and answers

**Q1. Does the traversal order matter?**
> Preorder, postorder and BFS all work, because each node's swap is independent. Inorder works only if you recurse into the same side twice.

**Q2. Can you do it without modifying the input?**
> Build a mirrored copy: `mirror(n) = n == null ? null : new TreeNode(n.val, mirror(n.right), mirror(n.left))`. That's O(n) extra memory. (The [E04](E04_SymmetricTree.md) file's test uses exactly this.)

### Follow-up questions

**F1. Check whether a tree is symmetric.** → [E04](E04_SymmetricTree.md)
> A tree is symmetric exactly when it equals its own mirror.

**F2. Are two trees mirrors of each other?**
> E02's lockstep, but compare a.left with b.right and a.right with b.left.

**F3. Invert only the nodes at odd levels (LC 2415, for a perfect tree).**
> BFS by levels; at odd levels, reverse the values of that level.

---

## 9. Running the file

```bash
java E03_InvertBinaryTree.java      # 6/6 tests passed (including 300 random: recursive == iterative, twice = identity)
```

**One-line summary:** *"Swap every node's left and right children, in any traversal order (preorder recursion or BFS)."*
