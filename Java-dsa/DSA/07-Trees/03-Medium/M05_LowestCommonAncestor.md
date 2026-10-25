# M05 · Lowest Common Ancestor of a Binary Tree

| | |
|---|---|
| **LeetCode** | [#236 Lowest Common Ancestor of a Binary Tree](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-tree/) (+ [#235 for a BST](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-search-tree/)) |
| **Difficulty** | Medium |
| **Pattern** | Bottom-up: "what did you find down there?" |
| **Code** | [M05_LowestCommonAncestor.java](M05_LowestCommonAncestor.java) |
| **Run** | `java M05_LowestCommonAncestor.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](../02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

The **lowest common ancestor (LCA)** of nodes p and q is the **deepest** node that has both p and q as descendants. A node counts as a descendant of itself.

```
            3
          /   \
         5     1           LCA(5, 1) = 3
        / \   / \          LCA(5, 4) = 5   (5 is an ancestor of 4, and of itself)
       6   2 0   8         LCA(6, 4) = 5
          / \              LCA(7, 8) = 3
         7   4
```

p and q are guaranteed to exist in the tree and to be different nodes. (The file's random test also tries p == q, where the answer is p.)

---

## 2. How to think about it

### Brute force: compare the root-to-node paths
Find the path from the root to p and the path to q (as lists), then walk both from the root. The **last node they share** is the LCA. O(n), but with two searches and two lists.

### One pass: ask each subtree "what did you find?"
`lca(node)` returns:
- **p or q**, if it finds one of them in this subtree (or the finished answer, if it found both)
- **null**, if neither is in this subtree

At each node:
| situation | return |
|---|---|
| node is null, p or q | node (found something, or nothing) |
| left and right results **both** non-null | p is on one side and q on the other → **this node is the LCA** |
| only one is non-null | pass it up (it's p, q, or an LCA found deeper) |

### "Return as soon as node == p": what if q is below p?
Then p **is** the LCA. The search never looks inside p's subtree, but it doesn't need to: nothing else in the tree returns non-null, so p propagates all the way up. (This relies on the guarantee that both p and q exist.)

### Dry run: LCA(6, 4)

```
at 6: it's p → return 6
at 2: left (7) → null; right (4) → it's q → 4; only one → return 4
at 5: left = 6, right = 4 → both non-null → return 5
at 1: 0 → null, 8 → null → return null
at 3: left = 5, right = null → pass up 5
```

### For a BST (LC 235): use the ordering
Walk from the root: if both values are smaller, go left; if both are larger, go right; otherwise the current node is where they **split**, and that's the LCA.

---

## 3. Java concepts used in this problem

### 3.1 Comparing nodes by reference: `root == p`

```java
if (root == null || root == p || root == q) return root;
```

p and q are **node objects** inside the tree, so `==` checks "is this the same node?". That's correct even when values repeat (the random test uses values 0..9, with many duplicates).

### 3.2 Returning a node as a "found" signal

The return type does double duty: `null` = "found nothing", a node = "found p, q, or the answer". No extra flags are needed.

### 3.3 Backtracking a path list

```java
out.add(n);
if (n == target || path(n.left, target, out) || path(n.right, target, out)) return true;
out.remove(out.size() - 1);                           // backtrack
```

The brute force builds the root-to-target path in one shared list: add the node, search below it, and remove it again if the target isn't down there. (The same add/recurse/remove shape as [M06](M06_PathSumII.md).)

---

## 4. Approach 1: Compare the root-to-node paths

### Code ([M05_LowestCommonAncestor.java:71-86](M05_LowestCommonAncestor.java#L71-L86))

```java
static TreeNode brute(TreeNode root, TreeNode p, TreeNode q) {
    List<TreeNode> a = new ArrayList<>(), b = new ArrayList<>();
    path(root, p, a);
    path(root, q, b);
    TreeNode lca = null;
    for (int i = 0; i < Math.min(a.size(), b.size()) && a.get(i) == b.get(i); i++) lca = a.get(i);
    return lca;
}

static boolean path(TreeNode n, TreeNode target, List<TreeNode> out) {
    if (n == null) return false;
    out.add(n);
    if (n == target || path(n.left, target, out) || path(n.right, target, out)) return true;
    out.remove(out.size() - 1);                           // backtrack
    return false;
}
```

| Line | What it does |
|---|---|
| `path(root, p, a)` | a = the nodes from the root to p |
| the `for` loop | walk both paths while they agree; the last shared node is the LCA |
| `path`'s backtrack | removes nodes that aren't on the way to the target |

**Dry run** LCA(6, 4): path to 6 = [3, 5, 6], path to 4 = [3, 5, 2, 4]. They agree on 3, 5 and differ at index 2 → **5**.

O(n) time, O(n) extra space.

---

## 5. Approach 2: One-pass recursion (the file's main solution)

### Code ([M05_LowestCommonAncestor.java:51-57](M05_LowestCommonAncestor.java#L51-L57))

```java
static TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
    if (root == null || root == p || root == q) return root;
    TreeNode left = lowestCommonAncestor(root.left, p, q);
    TreeNode right = lowestCommonAncestor(root.right, p, q);
    if (left != null && right != null) return root;      // split point
    return left != null ? left : right;                  // pass up whatever was found
}
```

### Line by line

| Line | What it does |
|---|---|
| `root == null` | nothing here → null |
| `root == p \|\| root == q` | found one target → report it (don't search below) |
| `left`, `right` | what each subtree found |
| `left != null && right != null` | one target on each side → root is the deepest node above both |
| `left != null ? left : right` | pass up the single finding (or null if neither side found anything) |

### Dry run: LCA(5, 4)

| call | result | why |
|---|---|---|
| at 5 | **5** (immediately) | root == p; 5's subtree (containing 4) is never searched |
| at 1 | null | 0 and 8 return null |
| at 3 | 5 | left = 5, right = null → pass up 5 |

**5** ✓ (correct, because 4 is inside 5's subtree).

### Dry run: LCA(7, 8)

| call | result |
|---|---|
| at 7 | 7 (p) |
| at 4 | null |
| at 2 | 7 |
| at 6 | null |
| at 5 | 7 |
| at 0 | null |
| at 8 | 8 (q) |
| at 1 | 8 |
| at 3 | left 7, right 8 → **3** |

### Complexity
- **Time O(n).**
- **Space O(h).**

---

## 6. Approach 3: LCA in a BST (LC 235)

### Code ([M05_LowestCommonAncestor.java:60-68](M05_LowestCommonAncestor.java#L60-L68))

```java
static TreeNode lcaBST(TreeNode root, int p, int q) {
    TreeNode n = root;
    while (n != null) {
        if (p < n.val && q < n.val) n = n.left;
        else if (p > n.val && q > n.val) n = n.right;
        else return n;                                    // they split here (or one IS n)
    }
    return null;
}
```

### Line by line

| Line | What it does |
|---|---|
| both smaller | both targets are in the left subtree → the LCA is there too |
| both larger | both are in the right subtree |
| otherwise | they're on different sides, or one of them is n → n is the LCA |

### Dry run: BST `[6,2,8,0,4,7,9,null,null,3,5]`, LCA(3, 5)

| at | comparison | move |
|---|---|---|
| 6 | 3 < 6 and 5 < 6 | left |
| 2 | 3 > 2 and 5 > 2 | right |
| 4 | 3 < 4 < 5 | **split → 4** |

And LCA(2, 8): at 6, 2 < 6 < 8 → **6** immediately.

### Complexity
- **Time O(h).**
- **Space O(1)** (iterative).

---

## 7. Comparing the approaches

| | Path comparison | One-pass recursion | BST walk |
|---|---|---|---|
| Works on | any binary tree | any binary tree | BSTs only |
| Time | O(n) | O(n) | O(h) |
| Space | O(n) | O(h) | O(1) |

The file checks the one-pass recursion against path comparison on 500 random trees (with duplicate values, using node identity).

---

## 8. Edge cases and common mistakes

**Edge cases:** one node is an ancestor of the other, p == q, the root is the LCA, a deep chain.

**Mistakes:**
1. Comparing by value with duplicate values present (use node references).
2. Continuing to search below p after finding it (unnecessary, given the guarantee).
3. Using the one-pass version when p or q might **not** be in the tree: it would return the single found node as if it were the LCA. Fix: count the found targets, or verify both exist first.
4. In the BST version, using `<=` (fails when p or q equals n).

---

## 9. Interview questions and answers

**Q1. Why does returning p immediately work when q is below p?**
> Then the LCA is p. No other node in the tree returns non-null, so p's result is passed up unchanged to the root.

**Q2. What if p or q might not be in the tree?**
> Track whether each was actually found (e.g. a counter incremented when root == p or root == q, searching the whole tree), and return null unless both were found.

### Follow-up questions

**F1. Nodes have parent pointers (LC 1650).**
> It's the intersection of two linked lists ([Linked Lists](../../03-Linked-Lists/README.md)): walk both upward, switching to the other start at the top, until they meet.

**F2. Many LCA queries on a static tree.**
> Binary lifting (O(n log n) preprocessing, O(log n) per query) or an Euler tour + range-minimum query.

**F3. The distance between two nodes.**
> depth(p) + depth(q) − 2 · depth(LCA).

---

## 10. Running the file

```bash
java M05_LowestCommonAncestor.java      # 7/7 tests passed (including 500 random trees vs path comparison)
```

**One-line summary:** *"Return p or q when you hit one, null for nothing; a node whose left and right both return non-null is the LCA. In a BST, walk down until p and q split."*
