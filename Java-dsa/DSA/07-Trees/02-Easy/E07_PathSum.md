# E07 · Path Sum

| | |
|---|---|
| **LeetCode** | [#112 Path Sum](https://leetcode.com/problems/path-sum/) |
| **Difficulty** | Easy |
| **Pattern** | Top-down: pass the **remaining** target down the tree |
| **Code** | [E07_PathSum.java](E07_PathSum.java) |
| **Run** | `java E07_PathSum.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Is there a **root-to-leaf** path whose values add up to `targetSum`?

```
            5
           / \
          4   8
         /   / \          target 22 → true   (5 + 4 + 11 + 2)
        11  13  4         target 26 → true   (5 + 8 + 13)
       /  \      \        target 9  → false  (5 + 4 stops at 4, which is not a leaf)
      7    2      1
```

| tree | target | answer |
|---|---|---|
| the tree above | 22 | true |
| `[1,2]` | 1 | false (1 alone is not a leaf) |
| `[]` | 0 | false |
| `[-2,null,-3]` | −5 | true |

---

## 2. How to think about it

### Top-down: carry "how much is still needed"
E01–E06 were **bottom-up** (children report to the parent). Here, information flows **down**: each node subtracts its value from the target and passes the rest to its children.

```
hasPath(node, remaining):
    null       → false            (no path here)
    remaining −= node.val
    leaf       → remaining == 0
    otherwise  → hasPath(left, remaining) || hasPath(right, remaining)
```

### The traps
- **The path must end at a leaf.** For `[1,2]` with target 1, node 1 isn't a leaf (it has child 2). That's why the check happens **at the leaf**, not at null: checking "null and remaining == 0" would wrongly accept node 1 through its empty right side.
- **Values can be negative**, so you can't stop early when remaining < 0.
- **Empty tree → false**, even for target 0.

---

## 3. Java concepts used in this problem

### 3.1 Parameters are copies (pass by value)

```java
int remaining = targetSum - root.val;
hasPathSum(root.left, remaining) || hasPathSum(root.right, remaining)
```

Each call gets its **own** copy of the int. Subtracting in a child never changes the parent's value, so after the left subtree is explored, the right subtree starts from the correct remaining amount. No "undo" step is needed (unlike when a shared list is used, as in [M06 Path Sum II](../03-Medium/M06_PathSumII.md)).

### 3.2 The leaf test

```java
if (root.left == null && root.right == null)
```

A leaf has **no** children; a node with exactly one child is not a leaf.

### 3.3 `||` short-circuits

If a path is found on the left, the right subtree isn't explored.

### 3.4 Passing an accumulator **and** a collection (the test referee)

```java
static void sums(TreeNode n, int acc, Set<Integer> out)
```

The referee collects every root-to-leaf sum into a set. `acc` is copied per call (as above), while `out` is a reference to one shared `HashSet` that every call adds to.

---

## 4. Approach 1: Collect all the leaf sums (the referee)

### Code ([E07_PathSum.java:49-55](E07_PathSum.java#L49-L55))

```java
static void sums(TreeNode n, int acc, Set<Integer> out) {
    if (n == null) return;
    acc += n.val;
    if (n.left == null && n.right == null) out.add(acc);
    sums(n.left, acc, out);
    sums(n.right, acc, out);
}
```

Then `answer = set.contains(target)`. O(n) time but O(number of leaves) extra memory, and no early exit.

---

## 5. Approach 2: Pass the remaining target down (the file's solution)

### Code ([E07_PathSum.java:41-46](E07_PathSum.java#L41-L46))

```java
static boolean hasPathSum(TreeNode root, int targetSum) {
    if (root == null) return false;
    int remaining = targetSum - root.val;
    if (root.left == null && root.right == null) return remaining == 0;   // at a leaf: done?
    return hasPathSum(root.left, remaining) || hasPathSum(root.right, remaining);
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (root == null) return false;` | no node, no path (this also handles the empty tree) |
| `int remaining = targetSum - root.val;` | what the rest of the path must add up to |
| the leaf check | a complete root-to-leaf path: does it hit the target exactly? |
| the recursive `\|\|` | try the left subtree, then the right |

### Dry run: the example tree, target 22

| call (node, target) | remaining | leaf? | result |
|---|---|---|---|
| (5, 22) | 17 | no | → left |
| (4, 17) | 13 | no | → left |
| (11, 13) | 2 | no | → left |
| (7, 2) | −5 | yes | false |
| (2, 2) | **0** | yes | **true** |
| (11) | | | true |
| (4) | | | true (4's right is never visited) |
| (5) | | | **true** (8's subtree is never visited) |

### Dry run: `[1, 2]`, target 1

| call | remaining | leaf? | result |
|---|---|---|---|
| (1, 1) | 0 | **no** (has a left child) | → left, right |
| (2, 0) | −2 | yes | false |
| (null, 0) | | | false |

**false** ✓. Note that remaining was 0 at node 1, but that doesn't count because 1 isn't a leaf.

### Dry run: `[-2, null, -3]`, target −5
(−2, −5) → remaining −3, not a leaf → (−3, −3) → remaining 0, leaf → **true** ✓

### Complexity
- **Time O(n)** worst case.
- **Space O(h).**

---

## 6. Approach 3: Iterative with a stack of (node, remaining)

```java
static boolean hasPathSumIterative(TreeNode root, int targetSum) {
    if (root == null) return false;
    Deque<TreeNode> nodes = new ArrayDeque<>();
    Deque<Integer> rem = new ArrayDeque<>();
    nodes.push(root);
    rem.push(targetSum - root.val);
    while (!nodes.isEmpty()) {
        TreeNode n = nodes.pop();
        int r = rem.pop();
        if (n.left == null && n.right == null && r == 0) return true;
        if (n.right != null) { nodes.push(n.right); rem.push(r - n.right.val); }
        if (n.left != null)  { nodes.push(n.left);  rem.push(r - n.left.val); }
    }
    return false;
}
```

Two parallel stacks hold each node with its own remaining amount, which is the state the recursion kept on the call stack.

---

## 7. Comparing the approaches

| | All leaf sums | Recursive | Iterative |
|---|---|---|---|
| Time | O(n) | O(n) | O(n) |
| Space | O(leaves + h) | O(h) | O(h) |
| Early exit | no | yes | yes |

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty tree, a single node equal to the target, negative values, a node with one child where remaining hits 0.

**Mistakes:**
1. Checking `root == null && remaining == 0` (accepts non-leaf endings).
2. Pruning when remaining < 0 (wrong with negative values).
3. Returning true for an empty tree with target 0.

---

## 9. Interview questions and answers

**Q1. Why check at the leaf rather than at null?**
> A null child of a one-child node isn't the end of a root-to-leaf path. Checking at the leaf guarantees the path really ends at a leaf.

**Q2. What's the difference between top-down and bottom-up here?**
> Top-down passes state (the remaining sum) from parent to child. Bottom-up returns results from child to parent. Path problems that start at the root are naturally top-down.

### Follow-up questions

**F1. Return all such paths.** → [M06 Path Sum II](../03-Medium/M06_PathSumII.md)
> Carry a shared list of the current path; add on the way down and remove on the way back (backtracking).

**F2. Paths that start and end anywhere going downward (LC 437 Path Sum III).**
> Prefix sums along the root-to-node path plus a HashMap of counts ([Hash Table M05/M07](../../06-Hash-Table/03-Medium/M07_ContiguousArray.md) family), O(n).

**F3. Sum of root-to-leaf numbers.** → [M09](../03-Medium/M09_SumRootToLeafNumbers.md)
> The same top-down carry, but with `value = value · 10 + digit`.

---

## 10. Running the file

```bash
java E07_PathSum.java      # 7/7 tests passed (including 1,000 random trees vs all leaf sums)
```

**One-line summary:** *"Subtract each node's value on the way down and check remaining == 0 only at a leaf."*
