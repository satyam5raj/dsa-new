# H04 · Binary Tree Cameras

| | |
|---|---|
| **LeetCode** | [#968 Binary Tree Cameras](https://leetcode.com/problems/binary-tree-cameras/) |
| **Difficulty** | Hard |
| **Pattern** | Greedy bottom-up with 3 states per node |
| **Code** | [H04_BinaryTreeCameras.java](H04_BinaryTreeCameras.java) |
| **Run** | `java H04_BinaryTreeCameras.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](../02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem). The 2-state version of tree DP is [M13 House Robber III](../03-Medium/M13_HouseRobberIII.md).

---

## 1. The problem

A camera on a node watches **its parent, itself and its children**. What's the minimum number of cameras needed to watch **every** node?

```
        0                      0
       /                      /
      0          →           0*        1 camera: the middle node covers all four
     / \
    0   0
```

| tree | answer |
|---|---|
| `[0,0,null,0,0]` | 1 |
| `[0,0,null,0,null,0,null,null,0]` | 2 |
| `[0]` | 1 |
| `[0,0,0]` | 1 (the root covers both children) |

---

## 2. How to think about it

### The greedy insight: never put a camera on a leaf
A camera on the leaf's **parent** covers everything the leaf camera would (the leaf and the parent), plus more (the grandparent and the sibling). So work from the **bottom up**, and place a camera only when a child would otherwise stay unwatched.

### Three states, reported to the parent (postorder)

| state | meaning |
|---|---|
| **NOT_COVERED** (0) | nobody watches me yet: my parent **must** take a camera |
| **HAS_CAMERA** (1) | I have a camera (so my parent is covered too) |
| **COVERED** (2) | I'm watched by a child, but have no camera |

`null` children count as **COVERED**: they need nothing and offer nothing.

### The rules at a node, given its children's states
```
any child NOT_COVERED     → put a camera here: cameras++, return HAS_CAMERA
else any child HAS_CAMERA → return COVERED
else (both COVERED)       → return NOT_COVERED   (let my parent handle me)
```
At the end, if the **root** is NOT_COVERED, it has no parent to help, so add one more camera.

**The order of the first two rules matters.** If one child is NOT_COVERED and the other has a camera, we still need a camera here: the unwatched child has no other way to be covered.

### Dry run (the picture)
```
the two leaves: both children null (COVERED) → NOT_COVERED
the middle:     a child is NOT_COVERED → camera (1), HAS_CAMERA
the root:       its child HAS_CAMERA → COVERED         total 1
```

---

## 3. Java concepts used in this problem

### 3.1 Named integer constants for the states

```java
static final int NOT_COVERED = 0, HAS_CAMERA = 1, COVERED = 2;
```

`static final` makes them compile-time constants; the names make the rules readable. A Java `enum State { NOT_COVERED, HAS_CAMERA, COVERED }` would be the type-safe alternative.

### 3.2 A static counter updated during the recursion

`cameras` is incremented whenever a node decides to take a camera, while the return value carries the state to the parent. It's the "return one thing, track another" pattern from [E05](../02-Easy/E05_DiameterOfBinaryTree.md).

### 3.3 `Integer.bitCount(mask)` (in the referee)

It counts the 1 bits of an int, i.e. the number of nodes chosen in that subset. The referee skips any subset that isn't smaller than the best one found so far.

---

## 4. Approach 1: Try every subset (the referee)

### Code ([H04_BinaryTreeCameras.java:73-94](H04_BinaryTreeCameras.java#L73-L94))

```java
static int brute(TreeNode root) {
    if (root == null) return 0;
    List<TreeNode> nodes = new ArrayList<>();
    Map<TreeNode, TreeNode> parent = new HashMap<>();
    collect(root, null, nodes, parent);
    int n = nodes.size(), best = n;
    for (int mask = 0; mask < (1 << n); mask++) {
        int count = Integer.bitCount(mask);
        if (count >= best) continue;
        Set<TreeNode> watched = new HashSet<>();
        for (int i = 0; i < n; i++) {
            if ((mask >> i & 1) == 0) continue;
            TreeNode c = nodes.get(i);
            watched.add(c);
            if (parent.get(c) != null) watched.add(parent.get(c));
            if (c.left != null) watched.add(c.left);
            if (c.right != null) watched.add(c.right);
        }
        if (watched.size() == n) best = count;
    }
    return best;
}
```

| Line | What it does |
|---|---|
| `best = n` | a camera on every node always works |
| `count >= best → continue` | prune subsets that can't improve |
| the inner loop | mark everything each chosen camera watches |
| `watched.size() == n` | every node is watched → a valid placement |

**O(2ⁿ · n).** It's the ground truth for small trees (≤ 14 nodes in the test).

---

## 5. Approach 2: Tree DP with explicit minimum costs

```java
static final int INF = 1_000_000;

// {cost if this node HAS a camera, cost if it's COVERED by a child (no camera), cost if NOT covered yet}
static int[] camDp(TreeNode n) {
    if (n == null) return new int[]{INF, 0, 0};          // null: can't hold a camera, but needs nothing
    int[] l = camDp(n.left), r = camDp(n.right);
    int minL = Math.min(l[0], Math.min(l[1], l[2])), minR = Math.min(r[0], Math.min(r[1], r[2]));
    int has = 1 + minL + minR;                            // my camera covers both children whatever they are
    int coveredL = l[0] + Math.min(r[0], r[1]);           // the left child has a camera, the right is fine
    int coveredR = r[0] + Math.min(l[0], l[1]);
    int covered = Math.min(coveredL, coveredR);
    int notCovered = Math.min(l[0], l[1]) + Math.min(r[0], r[1]);   // the children are covered without me
    return new int[]{has, covered, notCovered};
}
// answer = min(camDp(root)[0], camDp(root)[1])   (the root can't stay uncovered)
```

This is the "proper" DP: each state stores the **minimum cameras** in the subtree, given the node's situation. It's correct and O(n), and it's what you'd fall back to if you didn't trust the greedy. The greedy is a simplification of exactly this table.

---

## 6. Approach 3: Greedy with 3 states (the file's solution)

### Code ([H04_BinaryTreeCameras.java:52-70](H04_BinaryTreeCameras.java#L52-L70))

```java
static final int NOT_COVERED = 0, HAS_CAMERA = 1, COVERED = 2;
static int cameras;

static int minCameraCover(TreeNode root) {
    cameras = 0;
    if (state(root) == NOT_COVERED) cameras++;          // the root has no parent to rely on
    return cameras;
}

static int state(TreeNode n) {
    if (n == null) return COVERED;
    int l = state(n.left), r = state(n.right);
    if (l == NOT_COVERED || r == NOT_COVERED) {          // a child needs me
        cameras++;
        return HAS_CAMERA;
    }
    if (l == HAS_CAMERA || r == HAS_CAMERA) return COVERED;
    return NOT_COVERED;
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (n == null) return COVERED;` | so that a leaf sees "both children fine, neither has a camera" → NOT_COVERED |
| `l = state(n.left), r = state(n.right)` | postorder: decide the children first |
| `l == NOT_COVERED \|\| r == NOT_COVERED` | a child relies on me → I must take a camera |
| `l == HAS_CAMERA \|\| r == HAS_CAMERA` | a child's camera covers me |
| `return NOT_COVERED;` | I'm uncovered; my parent will deal with it |
| `if (state(root) == NOT_COVERED) cameras++;` | the root has no parent |

### Dry run: `[0,0,null,0,null,0,null,null,0]`

The tree is a chain: a → (left) b → (left) c → (left) d → (right) e.

| node | children's states | rule | returns | cameras |
|---|---|---|---|---|
| e (leaf) | COVERED, COVERED | none is NOT_COVERED or HAS_CAMERA | NOT_COVERED | 0 |
| d | null (COVERED), e = NOT_COVERED | a child is NOT_COVERED → camera | HAS_CAMERA | 1 |
| c | d = HAS_CAMERA, null | a child has a camera | COVERED | 1 |
| b | c = COVERED, null | both covered | NOT_COVERED | 1 |
| a (root) | b = NOT_COVERED, null | camera | HAS_CAMERA | 2 |

The root is HAS_CAMERA (no extra camera) → **2** ✓

### Dry run: `[0]`
The single node: both children null → NOT_COVERED → the root is uncovered → cameras = **1** ✓

### Complexity
- **Time O(n).**
- **Space O(h).**

---

## 7. Comparing the approaches

| | All subsets | 3-state min-cost DP | Greedy 3 states |
|---|---|---|---|
| Time | O(2ⁿ · n) | O(n) | O(n) |
| Space | O(n) | O(h) | O(h) |
| Needs a proof? | no | no (it's exhaustive DP) | yes (the leaf argument) |

The file checks the greedy against all subsets on 500 random trees.

---

## 8. Edge cases and common mistakes

**Edge cases:** a single node (1), a chain, a perfect tree, the root left uncovered at the end.

**Mistakes:**
1. Treating null children as NOT_COVERED (it would put a camera on every leaf).
2. Checking HAS_CAMERA before NOT_COVERED (it leaves a child unwatched).
3. Forgetting the final root check.
4. Greedily placing cameras top-down (it's suboptimal).

---

## 9. Interview questions and answers

**Q1. Why is a camera never needed on a leaf?**
> Moving it to the leaf's parent covers the same nodes (the leaf and its parent) and possibly more, so it's never worse.

**Q2. Why bottom-up?**
> The leaves' needs are certain (they must be covered by their parent), so decisions made from the bottom are forced. Going top-down, you can't know whether a node's children will need it.

### Follow-up questions

**F1. Each camera has a cost per node (weighted).**
> The greedy breaks; use the 3-state min-cost DP with weights.

**F2. A camera covers distance 2.**
> More states (the distance to the nearest camera below, or how far up the coverage is needed).

**F3. Minimum vertex cover / dominating set on trees.**
> The same family: tree DP with a few states per node. This problem is the minimum dominating set.

---

## 10. Running the file

```bash
java H04_BinaryTreeCameras.java      # 5/5 tests passed (including 500 random trees vs all subsets)
```

**One-line summary:** *"Postorder with states NOT_COVERED / HAS_CAMERA / COVERED (null = COVERED): take a camera if any child is uncovered, report COVERED if a child has one, else NOT_COVERED; fix the root at the end."*
