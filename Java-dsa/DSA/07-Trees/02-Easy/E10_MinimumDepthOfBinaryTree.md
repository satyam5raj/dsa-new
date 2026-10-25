# E10 · Minimum Depth of Binary Tree

| | |
|---|---|
| **LeetCode** | [#111 Minimum Depth of Binary Tree](https://leetcode.com/problems/minimum-depth-of-binary-tree/) |
| **Difficulty** | Easy |
| **Pattern** | BFS stops at the first leaf (and a classic DFS trap) |
| **Code** | [E10_MinimumDepthOfBinaryTree.java](E10_MinimumDepthOfBinaryTree.java) |
| **Run** | `java E10_MinimumDepthOfBinaryTree.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Return the number of nodes on the **shortest root-to-leaf** path.

```
      3
     / \          → 2   (3 → 9)
    9   20
       /  \
      15   7

  2
   \
    3
     \            → 5   (a chain: the only leaf is 6)
      4
       \
        5
         \
          6
```

| tree | answer |
|---|---|
| `[3,9,20,null,null,15,7]` | 2 |
| `[2,null,3,null,4,null,5,null,6]` | 5 |
| `[]` | 0 |

---

## 2. How to think about it

### The trap: copying max depth with `min`
```java
1 + Math.min(minDepth(left), minDepth(right))      // WRONG
```
For node 2 in the chain, the left child is null → 0 → the answer would be 1. But node 2 is **not a leaf**, so "2" alone isn't a root-to-leaf path. **If one child is missing, you must go down the other one:**

```
left == null   → 1 + minDepth(right)
right == null  → 1 + minDepth(left)
both present   → 1 + min(both)
```

(Max depth doesn't have this problem: `max` naturally ignores a 0 from a missing side.)

### Better: BFS
BFS explores **level by level**, so the **first leaf it meets is the shallowest one**: return immediately. On a big lopsided tree, BFS may touch only a few nodes, while DFS always explores everything.

```
level 1: 3   (not a leaf)
level 2: 9 → a leaf! → return 2        (15 and 7 are never looked at)
```

---

## 3. Java concepts used in this problem

### 3.1 Early `return` from inside nested loops

```java
while (!q.isEmpty()) {
    for (int i = q.size(); i > 0; i--) {
        ...
        if (n.left == null && n.right == null) return depth;
```

`return` exits the whole method at once, from inside both loops. (`break` would only leave the inner `for`.)

### 3.2 Unreachable code that still has to compile

```java
return depth;   // unreachable: a non-empty tree always has a leaf
```

The compiler can't prove that the loop always returns, so the method still needs a final `return`. The comment documents why it never runs.

### 3.3 The level-by-level template

`for (int i = q.size(); i > 0; i--)` freezes the size of the current level (explained in [E01](E01_MaximumDepthOfBinaryTree.md#36-level-by-level-bfs-for-int-i--qsize-i--0-i--)). Here, `depth` is incremented after each level.

---

## 4. Approach 1: The wrong DFS (kept in the file for comparison)

### Code ([E10_MinimumDepthOfBinaryTree.java:63-66](E10_MinimumDepthOfBinaryTree.java#L63-L66))

```java
static int wrongDfs(TreeNode n) {                             // the trap, for comparison
    if (n == null) return 0;
    return 1 + Math.min(wrongDfs(n.left), wrongDfs(n.right));
}
```

On the chain `[2,null,3,null,4,null,5,null,6]`: wrongDfs(2) = 1 + min(wrongDfs(null) = 0, …) = **1**. That's wrong (the file's test shows it returning 1 instead of 5).

---

## 5. Approach 2: The correct DFS

### Code ([E10_MinimumDepthOfBinaryTree.java:56-61](E10_MinimumDepthOfBinaryTree.java#L56-L61))

```java
static int minDepthDfs(TreeNode n) {
    if (n == null) return 0;
    if (n.left == null) return 1 + minDepthDfs(n.right);     // must go right
    if (n.right == null) return 1 + minDepthDfs(n.left);     // must go left
    return 1 + Math.min(minDepthDfs(n.left), minDepthDfs(n.right));
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (n == null) return 0;` | only reached for the empty tree, or through the one-child branches (where the other side is null) |
| `if (n.left == null)` | there's no left path; the only paths go right. (For a leaf, the right is null too → 1 + 0 = 1 ✓) |
| `if (n.right == null)` | the only paths go left |
| `1 + Math.min(...)` | both sides exist: take the shorter |

### Dry run: the chain `2 → 3 → 4 → 5 → 6` (all right children)

| call | left null? | returns |
|---|---|---|
| minDepthDfs(6) | yes | 1 + minDepthDfs(null) = 1 |
| minDepthDfs(5) | yes | 1 + 1 = 2 |
| minDepthDfs(4) | yes | 3 |
| minDepthDfs(3) | yes | 4 |
| minDepthDfs(2) | yes | **5** |

### Complexity
- **Time O(n):** it always visits every node.
- **Space O(h).**

---

## 6. Approach 3: BFS with an early stop (the file's main solution)

### Code ([E10_MinimumDepthOfBinaryTree.java:39-54](E10_MinimumDepthOfBinaryTree.java#L39-L54))

```java
static int minDepth(TreeNode root) {
    if (root == null) return 0;
    Queue<TreeNode> q = new ArrayDeque<>();
    q.add(root);
    int depth = 1;
    while (!q.isEmpty()) {
        for (int i = q.size(); i > 0; i--) {
            TreeNode n = q.poll();
            if (n.left == null && n.right == null) return depth;   // first leaf = shallowest
            if (n.left != null) q.add(n.left);
            if (n.right != null) q.add(n.right);
        }
        depth++;
    }
    return depth;   // unreachable: a non-empty tree always has a leaf
}
```

### Line by line

| Line | What it does |
|---|---|
| `int depth = 1;` | the root is at depth 1 (counting nodes) |
| the `for` over `q.size()` | one level at a time |
| `if (leaf) return depth;` | the first leaf found is at the smallest depth |
| the two `add`s | the next level |
| `depth++;` | this level had no leaf; go one deeper |

### Dry run 1: `[3, 9, 20, null, null, 15, 7]`

| depth | queue at the start | polled | leaf? | result |
|---|---|---|---|---|
| 1 | [3] | 3 | no → add 9, 20 | |
| 2 | [9, 20] | 9 | **yes** | **return 2** |

20, 15 and 7 are never examined.

### Dry run 2: the chain
Each level holds one node, and only 6 is a leaf → depth 5 ✓

### Complexity
- **Time O(n)** worst case, but often far less (it stops at the shallowest leaf).
- **Space O(w)** (the widest level visited).

---

## 7. Comparing the approaches

| | Wrong DFS | Correct DFS | BFS |
|---|---|---|---|
| Correct? | ✗ | ✓ | ✓ |
| Time | | always O(n) | O(nodes down to the first leaf) |
| Space | | O(h) | O(w) |

The file checks that BFS and the correct DFS agree on 500 random trees.

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty tree (0), a single node (1), a chain (n), a leaf at the second level of a huge tree.

**Mistakes:**
1. The `1 + min(...)` trap with one missing child.
2. Treating a node with one child as a leaf.
3. Starting the BFS depth at 0 (answers off by one).

---

## 9. Interview questions and answers

**Q1. Why is max depth simpler than min depth?**
> For max, a missing child contributes 0, which never wins a `max` against a real path. For min, that 0 would win wrongly, so the missing side must be excluded explicitly.

**Q2. Why prefer BFS here?**
> The answer is the first leaf by level, and BFS reaches it without exploring the deeper parts of the tree.

### Follow-up questions

**F1. Return the shallowest leaf itself.**
> In the BFS, return `n` instead of the depth.

**F2. Minimum depth of an N-ary tree.**
> BFS works unchanged: the first node with no children ends the search.

**F3. Shortest path between two nodes in a tree.**
> Find their lowest common ancestor ([M05](../03-Medium/M05_LowestCommonAncestor.md)); the distance is depth(a) + depth(b) − 2 · depth(LCA).

---

## 10. Running the file

```bash
java E10_MinimumDepthOfBinaryTree.java      # 6/6 tests passed (including 500 random trees: BFS == DFS)
```

**One-line summary:** *"BFS level by level and return at the first leaf; in DFS, a node with one missing child must go down the other side, not take min with 0."*
