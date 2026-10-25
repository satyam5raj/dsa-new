# M13 · House Robber III

| | |
|---|---|
| **LeetCode** | [#337 House Robber III](https://leetcode.com/problems/house-robber-iii/) |
| **Difficulty** | Medium |
| **Pattern** | Tree DP: every node returns a pair {rob me, skip me} |
| **Code** | [M13_HouseRobberIII.java](M13_HouseRobberIII.java) |
| **Run** | `java M13_HouseRobberIII.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](../02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

The houses form a binary tree. You can't rob two **directly connected** houses (a parent and its child). Maximise the money.

```
        3*
       / \
      2   3          → 7   (rob 3 + 3 + 1: the root and both grandchildren)
       \   \
        3*  1*
```

| tree | answer | robbed |
|---|---|---|
| `[3,2,3,null,3,null,1]` | 7 | 3 + 3 + 1 |
| `[3,4,5,1,3,null,1]` | 9 | 4 + 5 |
| `[4,1,null,2,null,3]` | 7 | 4 + 3 (levels 0 and 3) |
| `[]` | 0 | |

---

## 2. How to think about it

### Greedy "rob every other level" fails
`[4,1,null,2,null,3]` is a chain 4 → 1 → 2 → 3. Even levels give 4 + 2 = 6; odd levels give 1 + 3 = 4. The best is **4 + 3 = 7** (levels 0 and 3), which no alternating-level rule picks.

### Naive recursion
```
rob(node) = max( node.val + rob(the 4 grandchildren),     // take node: the children are off-limits
                 rob(left) + rob(right) )                 // skip node
```
Each node's answer is recomputed by its parent **and** its grandparent, so the work grows exponentially. A memo map (node → best) fixes it.

### Clean tree DP: return two numbers per node (postorder)
```
withNode    = node.val + left.without + right.without
withoutNode = max(left.with, left.without) + max(right.with, right.without)
answer      = max(root.with, root.without)
```
If we skip a node, each child is free to be robbed or not, so take the better of the two. Each node is computed once.

```
leaf 3 → {3, 0};   leaf 1 → {1, 0}
node 2 (right child 3): with = 2 + 0 = 2,  without = max(3, 0) = 3        → {2, 3}
node 3 (right child 1): with = 3 + 0 = 3,  without = max(1, 0) = 1        → {3, 1}
root 3: with = 3 + 3 + 1 = 7,  without = max(2, 3) + max(3, 1) = 6         → 7
```

"Return {take, skip}" is how you do **DP on trees** in general: maximum independent set, and Binary Tree Cameras ([H04](../04-Hard/H04_BinaryTreeCameras.md)) uses three states. (DP gets its own topic: [15-Dynamic-Programming](../../15-Dynamic-Programming/README.md).)

---

## 3. Java concepts used in this problem

### 3.1 Returning two values with an `int[]`

```
static int[] dp(TreeNode n)          // returns {rob this node, skip this node}
    ...
    return new int[]{with, without};
```

Java methods return one value, so a small array (or a `record Pair(int with, int without)`) carries both. Index 0 = "with", index 1 = "without": document it, because it's easy to mix up.

### 3.2 A `HashMap<TreeNode, Integer>` memo (for the naive version)

Nodes have identity-based hashing (TreeNode doesn't override `hashCode`), so each node is its own cache key, as in [M04](M04_KthSmallestElementInBST.md).

### 3.3 Enumerating subsets with a bitmask (the referee)

```java
for (int mask = 0; mask < (1 << nodes.size()); mask++)
    if ((mask >> i & 1) == 1) ...        // is node i chosen?
```

Each integer from 0 to 2ⁿ − 1 is one subset: bit i says whether node i is chosen. `mask >> i & 1` reads bit i. Exponential, so only for small trees (the test uses ≤ 11 nodes). (Bit tricks: [12-Advanced](../../12-Advanced/README.md).)

---

## 4. Approach 1: Brute force over all subsets (the referee)

### Code ([M13_HouseRobberIII.java:63-85](M13_HouseRobberIII.java#L63-L85))

```java
static int brute(TreeNode root) {
    List<TreeNode> nodes = new ArrayList<>();
    Map<TreeNode, TreeNode> parent = new HashMap<>();
    collect(root, null, nodes, parent);
    int best = 0;
    for (int mask = 0; mask < (1 << nodes.size()); mask++) {
        Set<TreeNode> chosen = new HashSet<>();
        int sum = 0;
        for (int i = 0; i < nodes.size(); i++) if ((mask >> i & 1) == 1) { chosen.add(nodes.get(i)); sum += nodes.get(i).val; }
        boolean ok = true;
        for (TreeNode c : chosen) if (parent.get(c) != null && chosen.contains(parent.get(c))) { ok = false; break; }
        if (ok) best = Math.max(best, sum);
    }
    return best;
}

static void collect(TreeNode n, TreeNode p, List<TreeNode> nodes, Map<TreeNode, TreeNode> parent) {
    if (n == null) return;
    nodes.add(n);
    parent.put(n, p);
    collect(n.left, n, nodes, parent);
    collect(n.right, n, nodes, parent);
}
```

| Line | What it does |
|---|---|
| `collect` | list every node and record its parent |
| the `mask` loop | every subset of nodes |
| the `chosen` loop | the subset's sum |
| the validity check | no chosen node may have a chosen parent |

**O(2ⁿ · n).** Obviously correct, so it's a good referee.

---

## 5. Approach 2: Recursion with a memo

```java
static Map<TreeNode, Integer> memo = new HashMap<>();

static int robMemo(TreeNode n) {
    if (n == null) return 0;
    Integer cached = memo.get(n);
    if (cached != null) return cached;
    int take = n.val;
    if (n.left != null) take += robMemo(n.left.left) + robMemo(n.left.right);
    if (n.right != null) take += robMemo(n.right.left) + robMemo(n.right.right);
    int skip = robMemo(n.left) + robMemo(n.right);
    int best = Math.max(take, skip);
    memo.put(n, best);
    return best;
}
```

| Line | What it does |
|---|---|
| `take` | rob n, so jump to the grandchildren |
| `skip` | don't rob n: the children are free |
| `memo` | each node's best is computed once |

**O(n)** with the memo (exponential without it). Clear the memo between different trees.

---

## 6. Approach 3: Return {with, without} (the file's solution)

### Code ([M13_HouseRobberIII.java:48-60](M13_HouseRobberIII.java#L48-L60))

```java
static int rob(TreeNode root) {
    int[] r = dp(root);
    return Math.max(r[0], r[1]);
}

// returns {best if this node IS robbed, best if it is NOT}
static int[] dp(TreeNode n) {
    if (n == null) return new int[]{0, 0};
    int[] l = dp(n.left), r = dp(n.right);
    int with = n.val + l[1] + r[1];                                  // children must be skipped
    int without = Math.max(l[0], l[1]) + Math.max(r[0], r[1]);       // children free
    return new int[]{with, without};
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (n == null) return {0, 0};` | an empty subtree is worth nothing either way |
| `l = dp(n.left), r = dp(n.right)` | the children's pairs (postorder) |
| `with = n.val + l[1] + r[1]` | rob n → the children must be **without** |
| `without = max(l) + max(r)` | skip n → each child takes its better option |
| `Math.max(r[0], r[1])` at the root | rob the root or not, whichever is better |

### Dry run: `[3, 4, 5, 1, 3, null, 1]`

```
        3
       / \
      4   5
     / \   \
    1   3   1
```

| node | left pair | right pair | with | without | returns |
|---|---|---|---|---|---|
| 1 (under 4) | {0, 0} | {0, 0} | 1 | 0 | {1, 0} |
| 3 (under 4) | | | 3 | 0 | {3, 0} |
| 4 | {1, 0} | {3, 0} | 4 + 0 + 0 = 4 | 1 + 3 = 4 | {4, 4} |
| 1 (under 5) | | | 1 | 0 | {1, 0} |
| 5 | {0, 0} | {1, 0} | 5 + 0 + 0 = 5 | 0 + 1 = 1 | {5, 1} |
| 3 (root) | {4, 4} | {5, 1} | 3 + 4 + 1 = 8 | 4 + 5 = **9** | {8, 9} |

**max(8, 9) = 9** (rob 4 and 5) ✓

### Dry run: the chain `4 → 1 → 2 → 3`

| node | child pair | with | without |
|---|---|---|---|
| 3 | {0, 0} | 3 | 0 |
| 2 | {3, 0} | 2 + 0 = 2 | 3 |
| 1 | {2, 3} | 1 + 3 = 4 | max(2, 3) = 3 |
| 4 | {4, 3} | 4 + 3 = **7** | max(4, 3) = 4 |

→ **7** ✓ (the "without" of node 1 is 3, which comes from robbing node 3 two levels below).

### Complexity
- **Time O(n).**
- **Space O(h).**

---

## 7. Comparing the approaches

| | All subsets | Memo recursion | {with, without} |
|---|---|---|---|
| Time | O(2ⁿ · n) | O(n) | O(n) |
| Extra space | O(n) | O(n) memo | **O(h)** |
| Elegance | | needs the grandchildren | each node looks only at its children |

The file checks the DP against all subsets on 500 random trees.

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty tree, a single node, a chain, zeros.

**Mistakes:**
1. The alternating-levels greedy.
2. In "without", using `l[1] + r[1]` (forcing the children to be skipped too).
3. Mixing up indices 0 and 1.
4. The naive recursion without a memo (exponential).

---

## 9. Interview questions and answers

**Q1. Why do you need two values per node?**
> The parent's decision depends on whether the child was robbed. One "best" number loses that information, so return the best for both cases.

**Q2. How does this relate to House Robber I (an array)?**
> In an array, each house has one neighbour on each side, so `dp[i] = max(dp[i−1], dp[i−2] + nums[i])`. On a tree, "the previous house" becomes "the children", and the pair (with, without) plays the role of (dp[i−2] + val, dp[i−1]).

### Follow-up questions

**F1. Return which houses to rob.**
> Re-walk from the root: if `with > without`, rob the node and skip its children; otherwise, decide for each child independently.

**F2. You can't rob nodes within distance 2.**
> Track more states per node (e.g. robbed, child robbed, neither), like the 3-state camera DP.

**F3. Maximum independent set on a general tree (N-ary).**
> The same pair, summing over all the children.

---

## 10. Running the file

```bash
java M13_HouseRobberIII.java      # 5/5 tests passed (including 500 random trees vs all subsets)
```

**One-line summary:** *"Each node returns {with = val + children's without, without = sum of the children's best}; the answer is the root's max."*
