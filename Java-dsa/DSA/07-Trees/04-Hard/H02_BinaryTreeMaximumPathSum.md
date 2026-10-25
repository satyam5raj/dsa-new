# H02 · Binary Tree Maximum Path Sum

| | |
|---|---|
| **LeetCode** | [#124 Binary Tree Maximum Path Sum](https://leetcode.com/problems/binary-tree-maximum-path-sum/) |
| **Difficulty** | Hard |
| **Pattern** | "Return one thing, track another" + drop the negative branches |
| **Code** | [H02_BinaryTreeMaximumPathSum.java](H02_BinaryTreeMaximumPathSum.java) |
| **Run** | `java H02_BinaryTreeMaximumPathSum.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](../02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem). The pattern was introduced in [E05 Diameter](../02-Easy/E05_DiameterOfBinaryTree.md).

---

## 1. The problem

A **path** is any sequence of connected nodes, each used at most once. It can start and end anywhere and doesn't have to pass through the root. Values can be **negative**. Return the maximum path sum (a path has at least one node).

```
       -10
       /  \
      9    20          → 42   (15 → 20 → 7)
          /  \
         15   7
```

| tree | answer | path |
|---|---|---|
| `[1,2,3]` | 6 | 2 → 1 → 3 |
| `[-10,9,20,null,null,15,7]` | 42 | 15 → 20 → 7 |
| `[-3]` | −3 | the single node |
| `[-2,-1]` | −1 | just −1 |
| `[2,-1]` | 2 | just 2 (drop the negative arm) |

---

## 2. How to think about it

It's [Diameter](../02-Easy/E05_DiameterOfBinaryTree.md) with **values** instead of edge counts, plus negative numbers.

### Every path bends at its highest node
The best path bending at node n is:
```
n.val + (the best downward chain from n.left, or 0 if that's negative)
      + (the best downward chain from n.right, or 0 if that's negative)
```

### Return one arm, record the V
`gain(n)` **returns** the best **downward** chain starting at n, which is one arm only: a parent can extend a straight chain, not a "V" shape.

```
left  = max(0, gain(n.left))            ← a negative arm: just don't take it
right = max(0, gain(n.right))
best  = max(best, n.val + left + right) ← RECORD the V through n
return n.val + max(left, right)         ← RETURN one arm
```

```
gain(9) = 9               (best 9)
gain(15) = 15, gain(7) = 7
gain(20): l = 15, r = 7 → best = 20 + 15 + 7 = 42, return 35
gain(−10): l = 9, r = 35 → candidate −10 + 9 + 35 = 34 (best stays 42), return 25
```

### Traps
- **All values negative:** `[-3]` → −3. Start `best` at `Integer.MIN_VALUE`, not 0.
- **Never clamp the node itself to 0;** only the arms are optional. The path must contain at least the node where it bends.

---

## 3. Java concepts used in this problem

### 3.1 `Math.max(0, x)`: "take it only if it helps"

```java
int left = Math.max(0, gain(n.left));
```

It clamps a negative contribution to 0, which means "don't extend the path into that subtree". It's the tree version of Kadane's "restart instead of carrying a negative sum" ([Arrays M01](../../01-Arrays/03-Medium/M01_MaximumSubarray.md)).

### 3.2 A static `best`, reset per call

As in E05: the return value carries the arm for the parent, and the static field carries the global answer. Reset it to `Integer.MIN_VALUE` on every top-level call.

### 3.3 Running sums up a parent chain (the referee)

The brute force walks from a up to the root, storing the running sums in a map; then it walks from b up until it hits a node on a's path (their LCA). The path sum is `sum(a..LCA) + sum(b..just below LCA)`.

---

## 4. Approach 1: Every pair of nodes (the referee)

### Code ([H02_BinaryTreeMaximumPathSum.java:66-93](H02_BinaryTreeMaximumPathSum.java#L66-L93))

```java
static int brute(TreeNode root) {
    List<TreeNode> nodes = new ArrayList<>();
    Map<TreeNode, TreeNode> parent = new HashMap<>();
    collect(root, null, nodes, parent);
    int bestSum = Integer.MIN_VALUE;
    for (TreeNode a : nodes) {
        for (TreeNode b : nodes) {
            // walk a up to the root, remembering the running sums; then walk b up until we hit a's path
            Map<TreeNode, Integer> sumUpFromA = new HashMap<>();
            int s = 0;
            for (TreeNode x = a; x != null; x = parent.get(x)) { s += x.val; sumUpFromA.put(x, s); }
            int t = 0;
            for (TreeNode y = b; y != null; y = parent.get(y)) {
                if (sumUpFromA.containsKey(y)) { bestSum = Math.max(bestSum, sumUpFromA.get(y) + t); break; }
                t += y.val;
            }
        }
    }
    return bestSum;
}
```

| Line | What it does |
|---|---|
| `collect` | every node and its parent |
| `sumUpFromA` | for each ancestor x of a: the sum of the values from a up to x |
| walk y up from b | `t` = the sum from b up to (but excluding) y |
| the first y on a's path | the LCA → the path sum is `sum(a → LCA) + t` |

Every pair (including a == b, a single node) is tried: **O(n² · h)**. Obviously correct.

---

## 5. Approach 2: One bottom-up pass (the file's solution)

### Code ([H02_BinaryTreeMaximumPathSum.java:49-63](H02_BinaryTreeMaximumPathSum.java#L49-L63))

```java
static int best;

static int maxPathSum(TreeNode root) {
    best = Integer.MIN_VALUE;
    gain(root);
    return best;
}

static int gain(TreeNode n) {
    if (n == null) return 0;
    int left = Math.max(0, gain(n.left));            // skip a branch that only loses
    int right = Math.max(0, gain(n.right));
    best = Math.max(best, n.val + left + right);    // the path bending at n
    return n.val + Math.max(left, right);           // a parent can use only one arm
}
```

### Line by line

| Line | What it does |
|---|---|
| `best = Integer.MIN_VALUE` | the answer can be negative |
| `if (n == null) return 0;` | an empty arm adds nothing |
| `Math.max(0, gain(...))` | use an arm only if it's positive |
| `n.val + left + right` | the best path whose highest node is n |
| `n.val + Math.max(left, right)` | the best chain going down from n, for the parent to extend |

### Dry run 1: `[-10, 9, 20, null, null, 15, 7]`

| call | left arm | right arm | candidate (V) | best | returns |
|---|---|---|---|---|---|
| gain(9) | 0 | 0 | 9 | 9 | 9 |
| gain(15) | 0 | 0 | 15 | 15 | 15 |
| gain(7) | 0 | 0 | 7 | 15 | 7 |
| gain(20) | 15 | 7 | **42** | 42 | 35 |
| gain(−10) | 9 | 35 | 34 | 42 | 25 |

**42** ✓

### Dry run 2: `[-2, -1]`

| call | arms | candidate | best | returns |
|---|---|---|---|---|
| gain(−1) | 0, 0 | −1 | −1 | −1 |
| gain(−2) | max(0, −1) = 0, 0 | −2 | −1 | −2 |

**−1** ✓ (the best path is the single node −1).

### Dry run 3: `[2, -1]`
gain(−1) = −1 (best −1) → at 2: left arm max(0, −1) = 0 → candidate 2 → best **2** ✓

### Complexity
- **Time O(n).**
- **Space O(h).**

---

## 6. Comparing the approaches

| | Every pair | One pass |
|---|---|---|
| Time | O(n² · h) | **O(n)** |
| Space | O(n) | O(h) |

The file checks the one-pass version against every pair on 1,000 random trees with values −10..10.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single node, all negative values, a chain, a path that doesn't include the root.

**Mistakes:**
1. Starting `best` at 0 (wrong when everything is negative).
2. Returning `n.val + left + right` to the parent (a V can't be extended).
3. Clamping the node's own value, e.g. `Math.max(0, n.val + …)` as the candidate.
4. Forgetting to reset the static `best`.

---

## 8. Interview questions and answers

**Q1. Why return one arm but record both?**
> A parent can only continue a path down one side of its child, so it needs the child's best single chain. A path whose highest point is the node itself can use both arms, and that's the candidate for the answer.

**Q2. Why clamp the arms at 0?**
> A negative arm only lowers the sum, so the path is better off stopping at the node. The node itself is never optional, since the path must contain it.

### Follow-up questions

**F1. Return the path itself.**
> Record the bend node and whether each arm was used, then walk down along the arm that gave each node's return value.

**F2. The path must go from a leaf to a leaf.**
> Only count a V when both children exist, and don't clamp the arms to 0 (a leaf-to-leaf path must reach a leaf on each side).

**F3. Longest Univalue Path (LC 687).**
> The same skeleton: an arm counts only if the child has the same value.

---

## 9. Running the file

```bash
java H02_BinaryTreeMaximumPathSum.java      # 6/6 tests passed (including 1,000 random trees vs all pairs)
```

**One-line summary:** *"gain(n) returns n.val + its best non-negative arm; at each node, record n.val + both non-negative arms as the path that bends there."*
