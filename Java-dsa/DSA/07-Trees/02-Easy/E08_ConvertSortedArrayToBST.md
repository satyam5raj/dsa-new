# E08 · Convert Sorted Array to Binary Search Tree

| | |
|---|---|
| **LeetCode** | [#108 Convert Sorted Array to Binary Search Tree](https://leetcode.com/problems/convert-sorted-array-to-binary-search-tree/) |
| **Difficulty** | Easy |
| **Pattern** | Divide and conquer: the middle element is the root |
| **Code** | [E08_ConvertSortedArrayToBST.java](E08_ConvertSortedArrayToBST.java) |
| **Run** | `java E08_ConvertSortedArrayToBST.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Turn a **sorted** array (strictly increasing) into a **height-balanced BST**. Any valid answer is accepted.

```
[-10, -3, 0, 5, 9]   →        0
                            /   \
                         -10     5
                            \     \
                            -3     9
```

| nums | one valid output (this file's) |
|---|---|
| `[-10,-3,0,5,9]` | `[0,-10,5,null,-3,null,9]` |
| `[1,3]` | `[1,null,3]` |
| `[]` | `[]` |

---

## 2. How to think about it

### What a BST is
A **binary search tree** keeps, at every node: everything in the left subtree is **smaller**, everything in the right subtree is **larger**. Consequently, an **inorder traversal (left, node, right) visits the values in sorted order**, so a sorted array **is** the inorder of the BST we want.

### Pick the middle as the root
In a sorted array, "smaller than nums[mid]" = "left of mid" and "larger" = "right of mid". Choosing the **middle** element as the root gives both halves (almost) the same size, so the tree is balanced. Then do the same for each half:

```
build(lo, hi):
    lo > hi → null
    mid = (lo + hi) / 2
    node(nums[mid]),  left = build(lo, mid − 1),  right = build(mid + 1, hi)
```

### Dry run sketch

```
build(0, 4): mid 2 → 0
    build(0, 1): mid 0 → −10,   left = build(0, −1) = null,  right = build(1, 1) → −3
    build(3, 4): mid 3 → 5,     left = null,                  right = build(4, 4) → 9
```

LeetCode accepts any balanced answer. Choosing `mid = (lo + hi + 1) / 2` (the upper middle) gives another valid tree: `[0, −3, 9, −10, null, 5]`.

---

## 3. Java concepts used in this problem

### 3.1 A recursive helper over index ranges

```java
static TreeNode sortedArrayToBST(int[] nums) { return build(nums, 0, nums.length - 1); }
static TreeNode build(int[] nums, int lo, int hi)
```

Passing `lo` and `hi` instead of copying subarrays (`Arrays.copyOfRange`) keeps each call O(1) besides its node. Copying would add O(n log n) work and memory.

### 3.2 `lo + (hi - lo) / 2` instead of `(lo + hi) / 2`

The same value, but `lo + hi` could overflow for huge indices. It's the standard safe midpoint (used in every binary search).

### 3.3 The inclusive range and the empty base case

`[lo, hi]` includes both ends, so an empty range is `lo > hi`, e.g. `build(0, -1)` for an empty array.

### 3.4 Checking the result: inorder + balance

```java
in.equals(Arrays.stream(a).boxed().toList())     // the BST's inorder equals the input
heightIfBalanced(r) >= 0                          // E06's −1 sentinel
```

- `Arrays.stream(a).boxed().toList()` turns an `int[]` into a `List<Integer>` (`boxed` converts each int to Integer; `toList()` exists since Java 16).
- Equal inorder **and** strictly sorted input ⇒ a valid BST with the same keys.

---

## 4. Approach 1: Insert one by one (what *not* to do)

```java
static TreeNode insert(TreeNode root, int v) {
    if (root == null) return new TreeNode(v);
    if (v < root.val) root.left = insert(root.left, v);
    else root.right = insert(root.right, v);
    return root;
}
// for (int v : nums) root = insert(root, v);
```

Inserting **sorted** values into a plain BST builds a **chain** (every value is larger than all previous ones, so it always goes right): height n, O(n²) time. It's a valid BST but **not balanced**. This is exactly the degenerate case that balanced trees exist to prevent.

---

## 5. Approach 2: Divide and conquer on the middle (the file's solution)

### Code ([E08_ConvertSortedArrayToBST.java:44-55](E08_ConvertSortedArrayToBST.java#L44-L55))

```java
static TreeNode sortedArrayToBST(int[] nums) {
    return build(nums, 0, nums.length - 1);
}

static TreeNode build(int[] nums, int lo, int hi) {
    if (lo > hi) return null;
    int mid = lo + (hi - lo) / 2;
    TreeNode node = new TreeNode(nums[mid]);
    node.left = build(nums, lo, mid - 1);
    node.right = build(nums, mid + 1, hi);
    return node;
}
```

### Line by line

| Line | What it does |
|---|---|
| `build(nums, 0, nums.length - 1)` | the whole array |
| `if (lo > hi) return null;` | an empty range → no node |
| `int mid = lo + (hi - lo) / 2;` | the (lower) middle index |
| `new TreeNode(nums[mid])` | the root of this range |
| `node.left = build(lo, mid − 1)` | the smaller half becomes the left subtree |
| `node.right = build(mid + 1, hi)` | the larger half becomes the right subtree |

### Dry run: `[-10, -3, 0, 5, 9]`

| call (lo, hi) | mid | node | left call | right call |
|---|---|---|---|---|
| (0, 4) | 2 | 0 | (0, 1) | (3, 4) |
| (0, 1) | 0 | −10 | (0, −1) → null | (1, 1) |
| (1, 1) | 1 | −3 | (1, 0) → null | (2, 1) → null |
| (3, 4) | 3 | 5 | (3, 2) → null | (4, 4) |
| (4, 4) | 4 | 9 | null | null |

Level order: `[0, −10, 5, null, −3, null, 9]` ✓ (the file's expected output).

### Dry run: `[1, 3]`
(0, 1): mid 0 → 1; left (0, −1) null; right (1, 1) → 3 → `[1, null, 3]` ✓

### Why the result is balanced
At each node, the two halves have sizes ⌊(k−1)/2⌋ and ⌈(k−1)/2⌉, which differ by at most 1. Inductively, their heights differ by at most 1 too, and the height is ⌈log₂(n + 1)⌉.

### Complexity
- **Time O(n):** one node per element, O(1) work each.
- **Space O(log n)** recursion (the tree itself is O(n) output).

---

## 6. Comparing the approaches

| | Insert one by one | Middle as the root |
|---|---|---|
| Time | O(n²) on sorted input | **O(n)** |
| Height | n (a chain) | ⌈log₂(n + 1)⌉ |
| Balanced? | no | yes |

The file checks n = 0..300: the inorder equals the input, and the tree is height-balanced.

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty array, one element, two elements (either one can be the root), even lengths.

**Mistakes:**
1. An off-by-one in the ranges (`build(lo, mid)` repeats mid forever).
2. Copying subarrays at every level (slower, more memory).
3. Using `(lo + hi) / 2` in languages or contexts where it can overflow.

---

## 8. Interview questions and answers

**Q1. Why does the middle element give a balanced tree?**
> The two halves differ in size by at most one at every node, so their heights differ by at most one.

**Q2. Is the answer unique?**
> No. With an even-length range, either middle can be the root, and both choices produce valid balanced BSTs.

### Follow-up questions

**F1. Convert a sorted linked list to a balanced BST (LC 109).**
> Find the middle with slow/fast pointers (O(n log n)), or build in inorder while advancing a list pointer (O(n)).

**F2. Balance an existing BST (LC 1382).**
> Inorder traversal → a sorted list → this function.

**F3. Validate that the output is a BST.** → [M03](../03-Medium/M03_ValidateBinarySearchTree.md)
> Inorder must be strictly increasing, or pass (min, max) bounds down the tree.

---

## 9. Running the file

```bash
java E08_ConvertSortedArrayToBST.java      # 4/4 tests passed (including n = 0..300: BST order + balanced)
```

**One-line summary:** *"Make the middle element the root, then build the left half and the right half recursively: a balanced BST in O(n)."*
