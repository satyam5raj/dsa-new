# E06 · Balanced Binary Tree

| | |
|---|---|
| **LeetCode** | [#110 Balanced Binary Tree](https://leetcode.com/problems/balanced-binary-tree/) |
| **Difficulty** | Easy |
| **Pattern** | Bottom-up with a sentinel (−1 = "already unbalanced, stop") |
| **Code** | [E06_BalancedBinaryTree.java](E06_BalancedBinaryTree.java) |
| **Run** | `java E06_BalancedBinaryTree.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

A tree is **height-balanced** if, for **every** node, the heights of its left and right subtrees differ by at most 1.

```
      3                     1
     / \                   / \
    9   20     true       2   2        false (at the root: 3 vs 1)
       /  \              / \
      15   7            3   3
                       / \
                      4   4
```

| tree | answer |
|---|---|
| `[3,9,20,null,null,15,7]` | true |
| `[1,2,2,3,3,null,null,4,4]` | false |
| `[]` | true |
| `[1,2,2,3,null,null,3,4,null,null,4]` | false (the root looks fine, a child isn't) |

---

## 2. How to think about it

### Brute force: top-down
```
balanced(n) = |height(n.left) − height(n.right)| ≤ 1  &&  balanced(n.left)  &&  balanced(n.right)
```
Each `height` call walks the whole subtree, and it's called again at every level: O(n²) on a chain, O(n log n) on a balanced tree.

### One pass: bottom-up, with a sentinel
Compute the height and the balance check **together**. Return:
- the **height**, if the subtree is balanced
- **−1**, if it is **not** (heights are never negative, so −1 can't be confused with a real height)

At a node: if either child returned −1, or the heights differ by more than 1 → return −1. Each node is visited once, and the −1 lets us **stop early**.

### Why "every node" matters
The 4th example: both of the root's subtrees have height 3, so the root itself is fine. But each 2 has a chain of length 2 on one side and nothing on the other, so the tree is unbalanced. Checking only the root misses that.

```
          1
        /   \
       2     2
      /       \
     3         3
    /           \
   4             4
```

---

## 3. Java concepts used in this problem

### 3.1 A sentinel return value

```java
static int check(TreeNode n)     // height in nodes, or -1 if unbalanced somewhere
```

One `int` carries two pieces of information: "is it balanced?" and "how tall is it?". It works because the valid range (heights ≥ 0) leaves −1 free. The alternative is a small result object or record (`record Info(boolean balanced, int height)`), which is clearer but more verbose.

### 3.2 Early return to prune the recursion

```java
int l = check(n.left);
if (l == -1) return -1;                     // stop early: no need to look at the right side
```

Once any subtree is unbalanced, the answer is decided, so the right subtree isn't even visited, and every ancestor just passes the −1 up.

### 3.3 `Math.abs`

`Math.abs(l - r)` is the absolute difference. Here l and r are small, so there's no overflow risk (`Math.abs(Integer.MIN_VALUE)` is famously still negative).

---

## 4. Approach 1: Top-down (brute force)

### Code ([E06_BalancedBinaryTree.java:59-64](E06_BalancedBinaryTree.java#L59-L64))

```java
static boolean brute(TreeNode n) {
    if (n == null) return true;
    return Math.abs(height(n.left) - height(n.right)) <= 1 && brute(n.left) && brute(n.right);
}

static int height(TreeNode n) { return n == null ? 0 : 1 + Math.max(height(n.left), height(n.right)); }
```

| Line | What it does |
|---|---|
| `n == null → true` | an empty tree is balanced |
| `Math.abs(height(l) − height(r)) <= 1` | this node's condition |
| `&& brute(l) && brute(r)` | and every node below |

It's correct but recomputes the heights: **O(n²)** worst case. The file's test oracle.

---

## 5. Approach 2: Bottom-up with −1 (the file's solution)

### Code ([E06_BalancedBinaryTree.java:45-57](E06_BalancedBinaryTree.java#L45-L57))

```java
static boolean isBalanced(TreeNode root) {
    return check(root) != -1;
}

// height in nodes, or -1 if the subtree is unbalanced somewhere
static int check(TreeNode n) {
    if (n == null) return 0;
    int l = check(n.left);
    if (l == -1) return -1;                     // stop early: no need to look at the right side
    int r = check(n.right);
    if (r == -1 || Math.abs(l - r) > 1) return -1;
    return 1 + Math.max(l, r);
}
```

### Line by line

| Line | What it does |
|---|---|
| `check(root) != -1` | balanced ⇔ no −1 came back |
| `if (n == null) return 0;` | an empty subtree: height 0, balanced |
| `if (l == -1) return -1;` | the left side is already unbalanced → propagate |
| `if (r == -1 \|\| Math.abs(l - r) > 1)` | the right side is unbalanced, or this node is |
| `return 1 + Math.max(l, r);` | balanced so far: the real height |

### Dry run 1: `[1, 2, 2, 3, 3, null, null, 4, 4]`

| call | l | r | \|l − r\| | returns |
|---|---|---|---|---|
| check(4), check(4) | 0 | 0 | 0 | 1 |
| check(3) (with the 4s) | 1 | 1 | 0 | 2 |
| check(3) (leaf) | 0 | 0 | 0 | 1 |
| check(2) (left) | 2 | 1 | 1 | 3 |
| check(2) (right, leaf) | 0 | 0 | 0 | 1 |
| check(1) | 3 | 1 | **2 > 1** | **−1** |

`isBalanced` → **false** ✓

### Dry run 2: `[1, 2, 2, 3, null, null, 3, 4, null, null, 4]` (early stop)

| call | l | r | returns |
|---|---|---|---|
| check(4) | 0 | 0 | 1 |
| check(3) (left side) | 1 | 0 | 2 |
| check(2) (left) | 2 | 0 | **−1** (2 − 0 > 1) |
| check(1) | **−1** → return −1 immediately | (the right 2 is never visited) | −1 |

**false** ✓

### Dry run 3: `[3, 9, 20, null, null, 15, 7]`
check(9) = 1; check(15) = check(7) = 1 → check(20) = 2; check(3): |1 − 2| = 1 → 3 → **true** ✓

### Complexity
- **Time O(n):** each node at most once.
- **Space O(h).**

---

## 6. Comparing the approaches

| | Top-down | Bottom-up with −1 |
|---|---|---|
| Time | O(n²) chain / O(n log n) balanced | **O(n)** |
| Space | O(h) | O(h) |
| Early stop | partially (via `&&`) | yes |

The file checks both on 1,000 random trees.

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty tree (true), a single node, a chain of 2 (true) vs a chain of 3 (false), imbalance only deep down.

**Mistakes:**
1. Checking only the root's two heights.
2. Using 0 as the "unbalanced" sentinel (0 is a valid height).
3. Confusing "balanced" with "complete" or "perfect".
4. Forgetting to propagate −1 from the left child before computing with it.

---

## 8. Interview questions and answers

**Q1. Why does the top-down version cost O(n²)?**
> `height` is O(size of the subtree) and gets called at every node, so every node's height is recomputed once per ancestor. On a chain, that's 1 + 2 + … + n.

**Q2. Why is −1 safe as a signal?**
> Heights are always ≥ 0, so −1 can never be a real height.

### Follow-up questions

**F1. Return the first (deepest) unbalanced node.**
> When `|l − r| > 1` first triggers, record that node before returning −1.

**F2. Make a tree balanced.**
> Inorder → a sorted list → rebuild with [E08](E08_ConvertSortedArrayToBST.md) (LC 1382). Or maintain balance on every insert with AVL rotations ([AVLTree](../01-Basics/AVLTree.java)).

**F3. Why do balanced trees matter?**
> BST operations cost O(h). Balance keeps h = O(log n), instead of O(n) for a degenerate tree.

---

## 9. Running the file

```bash
java E06_BalancedBinaryTree.java      # 5/5 tests passed (including 1,000 random trees vs O(n²))
```

**One-line summary:** *"Return the height bottom-up, or −1 as soon as any node's children differ by more than 1, and propagate the −1 to the root."*
