# M15 · Recover Binary Search Tree

| | |
|---|---|
| **LeetCode** | [#99 Recover Binary Search Tree](https://leetcode.com/problems/recover-binary-search-tree/) |
| **Difficulty** | Medium |
| **Pattern** | The inorder should be sorted → find the inversions → swap the values back |
| **Code** | [M15_RecoverBinarySearchTree.java](M15_RecoverBinarySearchTree.java) |
| **Run** | `java M15_RecoverBinarySearchTree.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](../02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem). BST basics: [E08](../02-Easy/E08_ConvertSortedArrayToBST.md), [M03](M03_ValidateBinarySearchTree.md).

---

## 1. The problem

Exactly **two** nodes of a BST had their values swapped by mistake. Fix the tree **without changing its structure**.

```
    1            3                 3              2
   /            /                 / \            / \
  3      →     1                 1   4    →     1   4
   \            \                   /              /
    2            2                 2              3
```

| input | output |
|---|---|
| `[1,3,null,null,2]` | `[3,1,null,null,2]` |
| `[3,1,4,null,null,2]` | `[2,1,4,null,null,3]` |

---

## 2. How to think about it

### The inorder of a BST is sorted
Swapping two values creates **drops** (places where prev > cur) in that sequence:

```
NOT ADJACENT:   1  2 [6] 4  5 [3] 7          two drops: 6 > 4 and 5 > 3
                      ↑first     ↑second
    first  = the BIGGER value at the FIRST drop  (prev of drop 1) = 6
    second = the SMALLER value at the LAST drop  (cur of drop 2)  = 3

ADJACENT:       1  2 [4][3] 5                only one drop: 4 > 3
    first = prev = 4,  second = cur = 3
```

### One rule covers both cases
```
on every drop:  if first is null → first = prev;   ALWAYS second = cur
```
Then swap `first.val` and `second.val`.

### Space
A recursive or stack inorder uses O(h). The follow-up asks for **O(1)** space: use **Morris inorder** ([BinaryTreeBasics](../01-Basics/BinaryTreeBasics.java)) with the same drop detection. The file has both.

---

## 3. Java concepts used in this problem

### 3.1 Static fields shared by the traversal and the visitor

```java
static TreeNode first, second, prev;
```

`visit(n)` is called from inside the traversal and needs to remember the previous node and the two culprits across calls. Reset them at the start of each run.

### 3.2 Separating "how to traverse" from "what to do at each node"

`inorder` and the Morris loop both call the same `visit(n)`. Swapping the traversal doesn't touch the drop logic.

### 3.3 Morris inorder in brief

For a node `cur` with a left subtree, find its **inorder predecessor** `pred` (the rightmost node of the left subtree):
- if `pred.right == null`: create a temporary **thread** `pred.right = cur`, then go left
- if `pred.right == cur`: we've come back up through the thread, so remove it, **visit cur**, then go right

With no left subtree: visit cur and go right (possibly following a thread back up). Every thread is created once and removed once, so the tree is restored and the time is O(n), with O(1) extra space.

### 3.4 Swapping two fields

```java
int tmp = first.val; first.val = second.val; second.val = tmp;
```

Only the **values** move; the structure stays as it is (the problem requires that).

---

## 4. Approach 1: Collect, sort, rewrite

```java
static void recoverBySorting(TreeNode root) {
    List<TreeNode> nodes = new ArrayList<>();
    nodes(root, nodes);                                  // the inorder list of nodes (in the file)
    List<Integer> vals = new ArrayList<>();
    for (TreeNode n : nodes) vals.add(n.val);
    Collections.sort(vals);
    for (int i = 0; i < nodes.size(); i++) nodes.get(i).val = vals.get(i);
}
```

Write the sorted values back in inorder order. It works for any number of misplaced values, but costs **O(n log n)** time and O(n) space.

---

## 5. Approach 2: Inorder with drop detection (the file's main solution)

### Code ([M15_RecoverBinarySearchTree.java:43-64](M15_RecoverBinarySearchTree.java#L43-L64))

```java
static TreeNode first, second, prev;

static void recoverTree(TreeNode root) {
    first = second = prev = null;
    inorder(root);
    int tmp = first.val; first.val = second.val; second.val = tmp;
}

static void inorder(TreeNode n) {
    if (n == null) return;
    inorder(n.left);
    visit(n);
    inorder(n.right);
}

static void visit(TreeNode n) {
    if (prev != null && prev.val > n.val) {     // a drop
        if (first == null) first = prev;        // first drop: the bigger one
        second = n;                             // last drop: the smaller one
    }
    prev = n;
}
```

### Line by line

| Line | What it does |
|---|---|
| the reset | clean state for each tree |
| `inorder` | the standard recursive inorder, calling `visit` |
| `prev.val > n.val` | a drop in what should be an increasing sequence |
| `if (first == null) first = prev;` | only the first drop sets `first` |
| `second = n;` | every drop overwrites `second`, so it ends at the last drop |
| `prev = n;` | move on |
| the swap | put the two values back |

### Dry run 1: `[3, 1, 4, null, null, 2]` (inorder 1, 3, 2, 4)

| visit | prev | drop? | first | second |
|---|---|---|---|---|
| 1 | — | | | |
| 3 | 1 | no | | |
| 2 | 3 | **3 > 2** | 3 | 2 |
| 4 | 2 | no | 3 | 2 |

Swap 3 ↔ 2 → `[2, 1, 4, null, null, 3]` ✓ (one drop: the swapped values were adjacent in inorder).

### Dry run 2: `[1, 3, null, null, 2]` (inorder 3, 2, 1)

| visit | prev | drop? | first | second |
|---|---|---|---|---|
| 3 | — | | | |
| 2 | 3 | **3 > 2** | 3 | 2 |
| 1 | 2 | **2 > 1** | 3 (unchanged) | **1** |

Swap 3 ↔ 1 → `[3, 1, null, null, 2]` ✓. (The file's test labels this case "adjacent", but the swapped values 1 and 3 sit at inorder positions 0 and 2, so there are two drops.)

### Complexity
- **Time O(n).**
- **Space O(h).**

---

## 6. Approach 3: Morris inorder (O(1) space)

### Code ([M15_RecoverBinarySearchTree.java:67-82](M15_RecoverBinarySearchTree.java#L67-L82))

```java
static void recoverTreeMorris(TreeNode root) {
    first = second = prev = null;
    TreeNode cur = root;
    while (cur != null) {
        if (cur.left == null) {
            visit(cur);
            cur = cur.right;
        } else {
            TreeNode pred = cur.left;
            while (pred.right != null && pred.right != cur) pred = pred.right;
            if (pred.right == null) { pred.right = cur; cur = cur.left; }
            else { pred.right = null; visit(cur); cur = cur.right; }
        }
    }
    int tmp = first.val; first.val = second.val; second.val = tmp;
}
```

### Line by line

| Line | What it does |
|---|---|
| `cur.left == null` | nothing smaller is left to visit under cur → visit it, go right |
| the `pred` loop | the inorder predecessor of cur (stops at a thread back to cur) |
| `pred.right == null` | first arrival: thread pred → cur, then explore the left subtree |
| else | we came back via the thread: remove it, visit cur, go right |

### Dry run: `[3, 1, 4, null, null, 2]` (traced by running the code)

| step | action |
|---|---|
| cur = 3 | pred = 1; thread 1.right → 3; go left |
| cur = 1 | no left → **visit 1**; go right (follows the thread to 3) |
| cur = 3 | pred = 1 with 1.right == 3 → unthread; **visit 3**; go right |
| cur = 4 | pred = 2; thread 2.right → 4; go left |
| cur = 2 | no left → **visit 2** (drop 3 > 2: first = 3, second = 2); go right (the thread to 4) |
| cur = 4 | unthread 2.right; **visit 4**; go right → null |

Swap 3 ↔ 2 ✓, and every thread has been removed.

### Complexity
- **Time O(n)** (each edge is walked a constant number of times).
- **Space O(1).**

---

## 7. Comparing the approaches

| | Sort + rewrite | Inorder + drops | Morris + drops |
|---|---|---|---|
| Time | O(n log n) | O(n) | O(n) |
| Extra space | O(n) | O(h) | **O(1)** |

The file breaks 1,000 random BSTs by swapping two random values and checks that both O(n) versions repair them exactly.

---

## 8. Edge cases and common mistakes

**Edge cases:** the swapped nodes are adjacent in inorder (one drop), the root is involved, a two-node tree.

**Mistakes:**
1. Setting `second = prev` (it must be the smaller value, `cur`).
2. Assuming there are always two drops (the adjacent case has one).
3. Swapping nodes (relinking) instead of values.
4. Leaving Morris threads in place.

---

## 9. Interview questions and answers

**Q1. Why is first the prev of the first drop, and second the cur of the last drop?**
> The bigger value moved to an earlier position, so it shows up as the left side of the first descent. The smaller value moved later, so it's the right side of the last descent.

**Q2. How do you get O(1) space?**
> Morris traversal: temporary threads from each inorder predecessor back to its successor replace the stack, and they're removed on the way back.

### Follow-up questions

**F1. More than two values are misplaced.**
> The drop trick no longer identifies them; sort the inorder values and write them back (approach 1).

**F2. Detect whether a BST is "almost" valid (fixable with one swap).**
> Count the drops: 0 means already valid; 1 or 2 means swapping the identified pair fixes it (verify after the swap).

**F3. Validate a BST.** → [M03](M03_ValidateBinarySearchTree.md)
> The same inorder check, with no swap.

---

## 10. Running the file

```bash
java M15_RecoverBinarySearchTree.java      # 4/4 tests passed (including 1,000 random swaps repaired)
```

**One-line summary:** *"In the inorder, first = prev at the first drop and second = cur at the last drop; swap their values (recursively in O(h), or with Morris in O(1))."*
