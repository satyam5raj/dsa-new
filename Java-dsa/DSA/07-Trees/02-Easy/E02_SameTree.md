# E02 · Same Tree

| | |
|---|---|
| **LeetCode** | [#100 Same Tree](https://leetcode.com/problems/same-tree/) |
| **Difficulty** | Easy |
| **Pattern** | Two trees in lockstep |
| **Code** | [E02_SameTree.java](E02_SameTree.java) |
| **Run** | `java E02_SameTree.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Are two binary trees **identical**: the same shape **and** the same values?

```
  1         1                1          1
 / \       / \     true     /            \      false   (2 is a left child in one,
2   3     2   3            2              2              a right child in the other)
```

| p | q | answer |
|---|---|---|
| `[1,2,3]` | `[1,2,3]` | true |
| `[1,2]` | `[1,null,2]` | false |
| `[1,2,1]` | `[1,1,2]` | false |
| `[]` | `[]` | true |

---

## 2. How to think about it

### Why not just compare traversals?
`[1,2]` and `[1,null,2]` have the **same preorder** (1, 2), even though their shapes differ. A single traversal **without null markers** loses the shape. You'd have to record the nulls too, which is what serialization does.

### Recursion on *pairs* of nodes (p, q)

| case | answer |
|---|---|
| both null | **true**: two empty trees are the same |
| exactly one null | **false**: the shapes differ |
| the values differ | **false** |
| otherwise | `same(p.left, q.left) && same(p.right, q.right)` |

These three base cases are the **template for every "two trees" problem**: Symmetric Tree ([E04](E04_SymmetricTree.md)), Subtree of Another Tree ([E09](E09_SubtreeOfAnotherTree.md)), Merge Two Trees, and more.

---

## 3. Java concepts used in this problem

### 3.1 The order of the null checks

```java
if (p == null && q == null) return true;
if (p == null || q == null) return false;   // here: exactly one is null
```

After the first line, "at least one is null" can only mean "exactly one". After both lines, both are non-null, so `p.val` and `q.val` are safe.

### 3.2 `&&` short-circuits the recursion

```java
return p.val == q.val && isSameTree(p.left, q.left) && isSameTree(p.right, q.right);
```

If the values differ, neither subtree is visited; if the left subtrees differ, the right ones aren't visited. The first mismatch stops the whole search.

### 3.3 Serialization as a test referee

The tests compare `isSameTree(a, b)` with `String.valueOf(a).equals(String.valueOf(b))`. The level-order string **with nulls** describes a tree exactly, so equal strings ⇔ identical trees. (`String.valueOf(null)` gives `"null"`, which handles empty trees.)

---

## 4. Approach 1: Serialize and compare

```java
static boolean isSameTreeBySerialization(TreeNode p, TreeNode q) {
    return String.valueOf(p).equals(String.valueOf(q));
}
```

It's correct (the format includes the nulls), but it builds two strings: O(n) extra memory and no early exit. It's useful as a referee, not as the answer.

---

## 5. Approach 2: Lockstep recursion (the file's solution)

### Code ([E02_SameTree.java:36-40](E02_SameTree.java#L36-L40))

```java
static boolean isSameTree(TreeNode p, TreeNode q) {
    if (p == null && q == null) return true;
    if (p == null || q == null) return false;
    return p.val == q.val && isSameTree(p.left, q.left) && isSameTree(p.right, q.right);
}
```

### Line by line

| Line | What it does |
|---|---|
| `p == null && q == null` | both subtrees are empty → they match |
| `p == null \|\| q == null` | one is empty, the other isn't → the shapes differ |
| `p.val == q.val` | compare this pair of nodes |
| `isSameTree(p.left, q.left)` | left with left |
| `isSameTree(p.right, q.right)` | right with right |

### Dry run 1: `[1,2]` vs `[1,null,2]`

| call (p, q) | result |
|---|---|
| (1, 1) | values equal → check the left pair |
| (2, null) | exactly one null → **false** |
| (1, 1) | false (the right pair is never checked) |

### Dry run 2: `[1,2,3]` vs `[1,2,3]`

| call | result |
|---|---|
| (1, 1) | equal → recurse |
| (2, 2) | equal → (null, null) true, (null, null) true → true |
| (3, 3) | equal → true |
| (1, 1) | **true** |

### Dry run 3: `[1,2,1]` vs `[1,1,2]`
(1, 1) equal → left pair (2, 1): values differ → **false** ✓

### Complexity
- **Time O(min(n, m)):** it stops at the first difference, and never goes deeper than the smaller tree.
- **Space O(h):** the recursion depth.

---

## 6. Approach 3: Iterative, with a queue of pairs

```java
static boolean isSameTreeIterative(TreeNode p, TreeNode q) {
    Queue<TreeNode> queue = new LinkedList<>();         // LinkedList: nulls are allowed
    queue.add(p);
    queue.add(q);
    while (!queue.isEmpty()) {
        TreeNode a = queue.poll(), b = queue.poll();
        if (a == null && b == null) continue;
        if (a == null || b == null || a.val != b.val) return false;
        queue.add(a.left);  queue.add(b.left);
        queue.add(a.right); queue.add(b.right);
    }
    return true;
}
```

The same three checks, but pairs are queued instead of recursed on. Note the `continue` for (null, null): it's a match, but there's nothing to expand.

---

## 7. Comparing the approaches

| | Serialization | Recursion | Queue of pairs |
|---|---|---|---|
| Time | O(n + m) | O(min(n, m)) | O(min(n, m)) |
| Space | O(n + m) | O(h) | O(w) |
| Early exit | no | yes | yes |

---

## 8. Edge cases and common mistakes

**Edge cases:** both empty, one empty, same values but different shapes, same shape but one different value deep down.

**Mistakes:**
1. Checking `p.val` before the null checks → NullPointerException.
2. Comparing only one traversal without nulls.
3. Using `ArrayDeque` for the iterative version (it rejects null).

---

## 9. Interview questions and answers

**Q1. Why do you need both base cases?**
> (null, null) is a successful match at the bottom. (null, non-null) is a shape mismatch. Without the second, you'd dereference a null.

**Q2. Can you decide sameness from traversals?**
> Preorder **with null markers** (or level order with nulls) determines a tree uniquely. Alternatively, preorder plus inorder determines it **if the values are distinct**.

### Follow-up questions

**F1. Symmetric Tree.** → [E04](E04_SymmetricTree.md)
> The same lockstep, but compare left with right crosswise.

**F2. Are the trees the same up to swapping children (Flip Equivalent, LC 951)?**
> At each pair: values equal **and** ((left, left) and (right, right) match, **or** (left, right) and (right, left) match).

**F3. Compare many trees for equality quickly.**
> Hash each tree (a Merkle-style hash of the value and the children's hashes) or canonical serialization, then compare the hashes.

---

## 10. Running the file

```bash
java E02_SameTree.java      # 5/5 tests passed (including 500 random pairs vs serialization)
```

**One-line summary:** *"Both null → true, one null → false, values differ → false; otherwise recurse left-with-left and right-with-right."*
