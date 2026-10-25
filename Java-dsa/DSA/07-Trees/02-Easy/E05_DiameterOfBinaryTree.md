# E05 · Diameter of Binary Tree

| | |
|---|---|
| **LeetCode** | [#543 Diameter of Binary Tree](https://leetcode.com/problems/diameter-of-binary-tree/) |
| **Difficulty** | Easy |
| **Pattern** | "Return one thing, track another": return the height, record the best path |
| **Code** | [E05_DiameterOfBinaryTree.java](E05_DiameterOfBinaryTree.java) |
| **Run** | `java E05_DiameterOfBinaryTree.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Return the **length in edges** of the longest path between **any two nodes**. The path does **not** have to pass through the root.

```
        1
       / \
      2   3        → 3   (4 → 2 → 1 → 3, or 5 → 2 → 1 → 3)
     / \
    4   5
```

| tree | answer |
|---|---|
| `[1,2,3,4,5]` | 3 |
| `[1,2]` | 1 |
| `[1]` | 0 |

**Constraints:** up to 10⁴ nodes.

---

## 2. How to think about it

### Every path has a highest node where it "bends"
Any path goes up from one end to some highest node, then down to the other end. At that bend node b, the longest such path uses the deepest branch on each side:

```
length bending at b = height(b.left) + height(b.right)       (heights counted in NODES)
```

(A left height of 2 nodes means 2 edges from b down to the deepest left node, so node-heights add up to the edge count.)

### Brute force
Try every node as the bend and compute both heights from scratch: O(n) per node → **O(n²)**.

### One pass: return one thing, track another
We compute heights bottom-up anyway ([E01](E01_MaximumDepthOfBinaryTree.md)), so check every bend while we're there:
- `depth(n)` **returns** the height in nodes: what the **parent** needs (one arm)
- and **records** `best = max(best, leftDepth + rightDepth)`: the path that bends **here** (both arms)

```
depth(4) = depth(5) = 1           best stays 0
depth(2): l = 1, r = 1 → best = 2, return 2
depth(3) = 1
depth(1): l = 2, r = 1 → best = 3, return 3        → answer 3
```

This is **the** pattern for Binary Tree Maximum Path Sum ([H02](../04-Hard/H02_BinaryTreeMaximumPathSum.md)), Longest Univalue Path, and Binary Tree Cameras ([H04](../04-Hard/H04_BinaryTreeCameras.md)).

---

## 3. Java concepts used in this problem

### 3.1 A `static` field as the "tracked" result

```java
static int best;

static int diameterOfBinaryTree(TreeNode root) {
    best = 0;                // reset before every call!
    depth(root);
    return best;
}
```

The recursive helper returns the height, so the diameter needs another channel. A static field is the simplest. **Reset it** at the start of each top-level call, or the next test would see the previous answer.

Alternatives: an `int[] best = {0}` passed down (an array is an object, so the helper can modify its contents), or an instance field in a non-static solution class.

### 3.2 Two results from one recursion

| | returned to the parent | recorded globally |
|---|---|---|
| what | `1 + max(l, r)`: one arm | `l + r`: both arms |
| why | the parent can extend only **one** side of this subtree | a path can bend here and use **both** |

Keeping these two apart is the key idea.

---

## 4. Approach 1: Try every bend (brute force)

### Code ([E05_DiameterOfBinaryTree.java:61-67](E05_DiameterOfBinaryTree.java#L61-L67))

```java
static int brute(TreeNode n) {
    if (n == null) return 0;
    int through = height(n.left) + height(n.right);
    return Math.max(through, Math.max(brute(n.left), brute(n.right)));
}

static int height(TreeNode n) { return n == null ? 0 : 1 + Math.max(height(n.left), height(n.right)); }
```

| Line | What it does |
|---|---|
| `through = height(left) + height(right)` | the longest path bending at n |
| `Math.max(through, brute(left), brute(right))` | or the best path entirely inside a subtree |
| `height` | E01's depth, as a one-line ternary |

**O(n²)** on a chain (each height call walks the whole subtree). It's the file's test oracle.

---

## 5. Approach 2: One bottom-up pass (the file's solution)

### Code ([E05_DiameterOfBinaryTree.java:44-58](E05_DiameterOfBinaryTree.java#L44-L58))

```java
static int best;

static int diameterOfBinaryTree(TreeNode root) {
    best = 0;
    depth(root);
    return best;
}

// height in NODES of the subtree; updates 'best' with the path bending here
static int depth(TreeNode n) {
    if (n == null) return 0;
    int l = depth(n.left), r = depth(n.right);
    best = Math.max(best, l + r);
    return 1 + Math.max(l, r);
}
```

### Line by line

| Line | What it does |
|---|---|
| `best = 0;` | reset (a single node or an empty tree → 0) |
| `depth(root);` | its return value (the tree's height) isn't needed |
| `int l = depth(n.left), r = depth(n.right);` | the heights of both arms |
| `best = Math.max(best, l + r);` | the path that bends at n |
| `return 1 + Math.max(l, r);` | the height of n's subtree, for the parent |

### Dry run 1: `[1, 2, 3, 4, 5]`

| call | l | r | l + r | best after | returns |
|---|---|---|---|---|---|
| depth(4) | 0 | 0 | 0 | 0 | 1 |
| depth(5) | 0 | 0 | 0 | 0 | 1 |
| depth(2) | 1 | 1 | 2 | 2 | 2 |
| depth(3) | 0 | 0 | 0 | 2 | 1 |
| depth(1) | 2 | 1 | **3** | **3** | 3 |

### Dry run 2: the best bend is below the root

```
          1
         /
        2              the best path 5-4-3-2-6-7-8 bends at 2
       / \
      3   6
     /     \
    4       7
   /         \
  5           8
```

| call | l | r | l + r | best | returns |
|---|---|---|---|---|---|
| depth(5), depth(8) | 0 | 0 | 0 | 0 | 1 |
| depth(4), depth(7) | 1 | 0 | 1 | 1 | 2 |
| depth(3), depth(6) | 2 | 0 | 2 | 2 | 3 |
| depth(2) | 3 | 3 | **6** | **6** | 4 |
| depth(1) | 4 | 0 | 4 | 6 | 5 |

**Return 6.** Through the root, the best would only be 4.

### Complexity
- **Time O(n):** one visit per node.
- **Space O(h).**

---

## 6. Comparing the approaches

| | Every bend from scratch | One pass |
|---|---|---|
| Time | O(n²) worst case | **O(n)** |
| Space | O(h) | O(h) |

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty tree or a single node (0), a chain (n − 1), the best path not through the root.

**Mistakes:**
1. Returning `height(left) + height(right)` at the root only (misses deeper bends).
2. Returning `l + r` to the parent (a parent can't use both arms of a child).
3. Counting nodes instead of edges in the answer (off by one).
4. Forgetting to reset the static `best` between calls.

---

## 8. Interview questions and answers

**Q1. Why is the diameter not always through the root?**
> The root may have one shallow side. A deeper node with two long branches gives a longer path, as in the second dry run.

**Q2. Why return the height but record l + r?**
> A path through a parent can continue down into only one side of a child, so the parent needs the longest single arm. A path that bends at the node itself can use both arms, and that's the candidate for the answer.

### Follow-up questions

**F1. Return the nodes on the diameter path.**
> Record the bend node and walk down the deeper child from each side.

**F2. Diameter of an N-ary tree.**
> Keep the two largest child heights at each node; the candidate is their sum.

**F3. Diameter of a general tree (a graph).**
> Two BFS passes: from any node, find the farthest node u; from u, the farthest distance is the diameter.

**F4. Binary Tree Maximum Path Sum.** → [H02](../04-Hard/H02_BinaryTreeMaximumPathSum.md)
> The same skeleton with node values, and negative arms clipped to 0.

---

## 9. Running the file

```bash
java E05_DiameterOfBinaryTree.java      # 5/5 tests passed (including 500 random trees vs O(n²))
```

**One-line summary:** *"Return each subtree's height to the parent, and record left + right at every node as the path that bends there."*
