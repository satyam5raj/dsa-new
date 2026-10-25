# M03 · Validate Binary Search Tree

| | |
|---|---|
| **LeetCode** | [#98 Validate Binary Search Tree](https://leetcode.com/problems/validate-binary-search-tree/) |
| **Difficulty** | Medium |
| **Pattern** | Top-down with an allowed range (low, high), or: the inorder is strictly increasing |
| **Code** | [M03_ValidateBinarySearchTree.java](M03_ValidateBinarySearchTree.java) |
| **Run** | `java M03_ValidateBinarySearchTree.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](../02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem). BST basics: [E08](../02-Easy/E08_ConvertSortedArrayToBST.md).

---

## 1. The problem

Is the tree a valid **BST**? Every node's **entire** left subtree must be strictly smaller and its **entire** right subtree strictly larger.

| tree | answer | why |
|---|---|---|
| `[2,1,3]` | true | |
| `[5,1,4,null,null,3,6]` | false | 4 is in 5's right subtree, but 4 < 5 |
| `[5,4,6,null,null,3,7]` | false | 3 is in 5's right subtree (see below) |
| `[2,2,2]` | false | duplicates aren't allowed (strict) |
| `[2147483647]` | true | a node can hold `Integer.MAX_VALUE` |

---

## 2. How to think about it

### The classic wrong answer: compare each node only with its children

```
        5
       / \
      4   6          every parent/child pair looks fine (4 < 5, 6 > 5, 3 < 6, 7 > 6),
         / \         but 3 is in the RIGHT subtree of 5, and 3 < 5 → NOT a BST
        3   7
```

The rule is about whole **subtrees**, not just children.

### Approach 1: pass the allowed range down (top-down)
Each node must lie in an open interval `(low, high)` inherited from its ancestors:
- going **left**, the node becomes the new **upper** bound: `valid(left, low, node.val)`
- going **right**, the node becomes the new **lower** bound: `valid(right, node.val, high)`

Start with (−∞, +∞).

```
5 in (−∞, ∞) ✓ → 4 in (−∞, 5) ✓ → 6 in (5, ∞) ✓ → 3 in (5, 6) ✗ → false
```

### Approach 2: the inorder must be strictly increasing
An inorder traversal of a BST visits the values in sorted order ([E08](../02-Easy/E08_ConvertSortedArrayToBST.md)). So traverse in order, remember the previous value, and fail if `prev >= cur`.

---

## 3. Java concepts used in this problem

### 3.1 `long` bounds to represent "infinity"

```java
valid(root, Long.MIN_VALUE, Long.MAX_VALUE)
```

A node may hold `Integer.MIN_VALUE` or `Integer.MAX_VALUE`. With `int` bounds, "infinity" would have to be one of those values, and a node equal to it would wrongly fail the strict `<` check. A `long` range is strictly larger than any int, so every int fits strictly inside. (Using `Integer` bounds with `null` for "no bound" is the other common fix.)

`n.val <= low` compares an `int` with a `long`: the int is widened to long automatically.

### 3.2 A static `prev` field for the inorder walk

```java
static TreeNode prev;
```

The inorder recursion needs to remember the last node it visited **across** calls (not just from the parent), so it uses a field that's reset before each run. Storing the node (not its value) lets `null` mean "nothing visited yet", with no sentinel value needed.

### 3.3 Short-circuiting with early returns in inorder

```java
if (!inorder(n.left)) return false;
```

Once a violation is found anywhere, every call above it returns false immediately.

---

## 4. Approach 1: Children-only check (wrong, kept for comparison)

### Code ([M03_ValidateBinarySearchTree.java:67-72](M03_ValidateBinarySearchTree.java#L67-L72))

```java
static boolean wrongChildrenOnly(TreeNode n) {               // the classic mistake
    if (n == null) return true;
    if (n.left != null && n.left.val >= n.val) return false;
    if (n.right != null && n.right.val <= n.val) return false;
    return wrongChildrenOnly(n.left) && wrongChildrenOnly(n.right);
}
```

On `[5,4,6,null,null,3,7]` it returns **true**: wrong. The file's tests show it.

---

## 5. Approach 2: Allowed range, top-down (the file's main solution)

### Code ([M03_ValidateBinarySearchTree.java:42-50](M03_ValidateBinarySearchTree.java#L42-L50))

```java
static boolean isValidBST(TreeNode root) {
    return valid(root, Long.MIN_VALUE, Long.MAX_VALUE);
}

static boolean valid(TreeNode n, long low, long high) {
    if (n == null) return true;
    if (n.val <= low || n.val >= high) return false;
    return valid(n.left, low, n.val) && valid(n.right, n.val, high);
}
```

### Line by line

| Line | What it does |
|---|---|
| `valid(root, MIN, MAX)` | the root may be anything |
| `if (n == null) return true;` | an empty subtree is a valid BST |
| `n.val <= low \|\| n.val >= high` | outside the open interval (low, high) → invalid; `<=` / `>=` makes the comparison strict, so duplicates fail |
| `valid(n.left, low, n.val)` | everything on the left must also be < n.val |
| `valid(n.right, n.val, high)` | everything on the right must also be > n.val |

### Dry run 1: `[5, 4, 6, null, null, 3, 7]`

| node | allowed range | ok? |
|---|---|---|
| 5 | (−∞, ∞) | ✓ |
| 4 | (−∞, 5) | ✓ (its children are null) |
| 6 | (5, ∞) | ✓ |
| 3 | (5, 6) | **✗** 3 ≤ 5 → false |

### Dry run 2: `[5, 1, 4, null, null, 3, 6]`

| node | range | ok? |
|---|---|---|
| 5 | (−∞, ∞) | ✓ |
| 1 | (−∞, 5) | ✓ |
| 4 | (5, ∞) | **✗** → false |

### Dry run 3: `[2147483647]`
MAX_VALUE in (Long.MIN, Long.MAX) → ✓ → **true** (with int bounds this would have failed).

### Complexity
- **Time O(n).**
- **Space O(h).**

---

## 6. Approach 3: Inorder must strictly increase

### Code ([M03_ValidateBinarySearchTree.java:52-65](M03_ValidateBinarySearchTree.java#L52-L65))

```java
static TreeNode prev;

static boolean isValidBSTInorder(TreeNode root) {
    prev = null;
    return inorder(root);
}

static boolean inorder(TreeNode n) {
    if (n == null) return true;
    if (!inorder(n.left)) return false;
    if (prev != null && prev.val >= n.val) return false;     // must strictly increase
    prev = n;
    return inorder(n.right);
}
```

### Line by line

| Line | What it does |
|---|---|
| `prev = null;` | reset: nothing visited yet |
| `inorder(n.left)` first | all smaller values come before n |
| `prev.val >= n.val` | the sequence didn't strictly increase → invalid |
| `prev = n;` | n is now the last visited node |
| `inorder(n.right)` | then the larger values |

### Dry run: `[5, 4, 6, null, null, 3, 7]`

The inorder visits 4, 5, 3, 6, 7:

| visit | prev | check | result |
|---|---|---|---|
| 4 | null | | prev = 4 |
| 5 | 4 | 4 < 5 ✓ | prev = 5 |
| 3 | 5 | **5 ≥ 3** | **false** |

### Complexity
- **Time O(n)** (it stops at the first violation).
- **Space O(h).**

---

## 7. Comparing the approaches

| | Children only | Range top-down | Inorder |
|---|---|---|---|
| Correct? | ✗ | ✓ | ✓ |
| Time | | O(n) | O(n) |
| Space | | O(h) | O(h) |
| Pitfall | | int bounds vs MAX_VALUE | resetting `prev` |

The file checks both correct approaches against a referee (collect the inorder, then check that it strictly increases) on 3,000 random trees.

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty tree, a single node, duplicates (invalid), `Integer.MIN_VALUE`/`MAX_VALUE` nodes, a violation deep in the tree.

**Mistakes:**
1. Comparing only with the children.
2. `int` bounds initialised to `Integer.MIN_VALUE`/`MAX_VALUE` (fails on those values).
3. Allowing equality (`<` instead of `<=`).
4. Forgetting to reset `prev`.

---

## 9. Interview questions and answers

**Q1. Why isn't checking each node against its children enough?**
> The BST property constrains a node relative to **all** its ancestors. A grandchild can satisfy its parent's constraint but violate the grandparent's.

**Q2. Range or inorder: which is better?**
> Both are O(n) time and O(h) space. The range version makes the invariant explicit; the inorder version follows from "inorder of a BST is sorted" and is easy to make iterative.

### Follow-up questions

**F1. Duplicates allowed on the left (≤).**
> Change the left check to allow equality: the node must be in (low, high], or adjust the inorder check to `prev.val > n.val`.

**F2. Recover a BST where two nodes were swapped.** → [M15](M15_RecoverBinarySearchTree.md)
> The inorder has one or two "descents"; the swapped nodes are found there.

**F3. The largest BST subtree (LC 333).**
> Bottom-up: each node returns (isBST, size, min, max) of its subtree.

---

## 10. Running the file

```bash
java M03_ValidateBinarySearchTree.java      # 8/8 tests passed (including 3,000 random trees, both approaches)
```

**One-line summary:** *"Pass an open range (low, high) down the tree with long bounds, tightening it at each step, or check that the inorder strictly increases."*
