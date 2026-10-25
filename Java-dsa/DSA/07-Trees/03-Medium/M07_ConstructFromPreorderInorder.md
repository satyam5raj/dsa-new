# M07 · Construct Binary Tree from Preorder and Inorder Traversal

| | |
|---|---|
| **LeetCode** | [#105 Construct Binary Tree from Preorder and Inorder Traversal](https://leetcode.com/problems/construct-binary-tree-from-preorder-and-inorder-traversal/) |
| **Difficulty** | Medium |
| **Pattern** | Divide and conquer + a HashMap of inorder positions |
| **Code** | [M07_ConstructFromPreorderInorder.java](M07_ConstructFromPreorderInorder.java) |
| **Run** | `java M07_ConstructFromPreorderInorder.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](../02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Rebuild the tree from its **preorder** and **inorder** traversals. All values are unique.

```
preorder [3, 9, 20, 15, 7]
inorder  [9, 3, 15, 20, 7]       →        3
                                         / \
                                        9   20
                                           /  \
                                          15   7
```

| preorder | inorder | tree |
|---|---|---|
| `[3,9,20,15,7]` | `[9,3,15,20,7]` | `[3,9,20,null,null,15,7]` |
| `[-1]` | `[-1]` | `[-1]` |
| `[3,2,1]` | `[1,2,3]` | `[3,2,null,1]` (a left chain) |

---

## 2. How to think about it

### The traversal orders
| traversal | order |
|---|---|
| **preorder** | root, [left subtree…], [right subtree…] |
| **inorder** | [left subtree…], root, [right subtree…] |
| postorder | [left subtree…], [right subtree…], root |

### Two facts
- In **preorder**, the **first** value is the root.
- In **inorder**, the root **splits** the array into the left subtree and the right subtree.

```
preorder: [3 | 9 | 20 15 7]          inorder: [9 | 3 | 15 20 7]
           ↑root                                  ↑ the root at index 1
→ the left subtree has 1 node (everything left of 3 in inorder): {9}
→ the right subtree has 3 nodes: {15, 20, 7}
```

Recurse on each part. The **next root in preorder is simply the next unused value**: preorder lists the whole left subtree (its root first) before the right subtree.

### The algorithm
```
map: value → its index in inorder        (O(1) lookup instead of an O(n) search)
pre = 0                                  (a shared pointer into preorder)
build(inLo, inHi):
    if inLo > inHi → null
    root = preorder[pre++]
    mid  = map[root]
    root.left  = build(inLo, mid − 1)    ← MUST build the left first: preorder lists the
    root.right = build(mid + 1, inHi)       left subtree before the right one
```

### Why unique values?
With duplicates, the root's position in inorder is ambiguous. And preorder + **postorder** isn't enough in general: a single child could be left or right. Inorder is what tells the two sides apart.

---

## 3. Java concepts used in this problem

### 3.1 A static index shared across the recursion

```java
static int pre;
// inside build():  TreeNode root = new TreeNode(preorder[pre++]);
```

`pre++` uses the current value, then increments it. Because `pre` is a **field** (not a parameter), every recursive call advances the same pointer: after the left subtree is built, `pre` already points at the right subtree's root. Reset it at the start of each top-level call.

### 3.2 A HashMap for O(1) "where is this value?"

```java
for (int i = 0; i < inorder.length; i++) inIndex.put(inorder[i], i);
```

Without it, finding the root in the inorder range is a linear scan, making the whole algorithm O(n²) on a chain.

### 3.3 Several statements on one line

```java
out.add(n.val); preorder(n.left, out); preorder(n.right, out);
```

The test's traversal helpers are compressed onto one line each; they're the standard recursive preorder and inorder.

### 3.4 `List<Integer>` → `int[]`

```java
p.stream().mapToInt(Integer::intValue).toArray()
```

The test produces traversals as lists, and the solution takes arrays. This converts between them.

---

## 4. Approach 1: Search the inorder each time, with subarray copies

```java
static TreeNode buildSlow(int[] preorder, int[] inorder) {
    if (preorder.length == 0) return null;
    TreeNode root = new TreeNode(preorder[0]);
    int mid = 0;
    while (inorder[mid] != preorder[0]) mid++;                       // linear search
    root.left = buildSlow(Arrays.copyOfRange(preorder, 1, mid + 1),
                          Arrays.copyOfRange(inorder, 0, mid));
    root.right = buildSlow(Arrays.copyOfRange(preorder, mid + 1, preorder.length),
                           Arrays.copyOfRange(inorder, mid + 1, inorder.length));
    return root;
}
```

| Line | What it does |
|---|---|
| `preorder[0]` | the root |
| the `while` | find the root in inorder: `mid` = the size of the left subtree |
| `copyOfRange(preorder, 1, mid + 1)` | the left subtree's preorder: the next `mid` values |
| `copyOfRange(inorder, 0, mid)` | the left subtree's inorder |
| the right side | everything after |

It's the clearest statement of the idea, but **O(n²)** worst case (the searches and copies on a chain).

---

## 5. Approach 2: HashMap + a shared preorder pointer (the file's solution)

### Code ([M07_ConstructFromPreorderInorder.java:45-62](M07_ConstructFromPreorderInorder.java#L45-L62))

```java
static int pre;
static Map<Integer, Integer> inIndex;

static TreeNode buildTree(int[] preorder, int[] inorder) {
    pre = 0;
    inIndex = new HashMap<>();
    for (int i = 0; i < inorder.length; i++) inIndex.put(inorder[i], i);
    return build(preorder, 0, inorder.length - 1);
}

static TreeNode build(int[] preorder, int inLo, int inHi) {
    if (inLo > inHi) return null;
    TreeNode root = new TreeNode(preorder[pre++]);         // next root from preorder
    int mid = inIndex.get(root.val);                       // where it splits inorder
    root.left = build(preorder, inLo, mid - 1);            // left first (preorder order)
    root.right = build(preorder, mid + 1, inHi);
    return root;
}
```

### Line by line

| Line | What it does |
|---|---|
| `pre = 0; inIndex = …` | reset the shared state and index the inorder positions |
| `if (inLo > inHi) return null;` | an empty inorder range → no subtree |
| `preorder[pre++]` | the next root, in preorder order |
| `mid = inIndex.get(root.val)` | inorder[inLo..mid−1] is the left subtree, inorder[mid+1..inHi] the right |
| left, then right | matches the order in which preorder lists them |

### Dry run: preorder `[3, 9, 20, 15, 7]`, inorder `[9, 3, 15, 20, 7]`

inIndex = {9: 0, 3: 1, 15: 2, 20: 3, 7: 4}

| call (inLo, inHi) | pre before | root | mid | then |
|---|---|---|---|---|
| (0, 4) | 0 | 3 | 1 | left (0, 0), right (2, 4) |
| (0, 0) | 1 | 9 | 0 | left (0, −1) null, right (1, 0) null |
| (2, 4) | 2 | 20 | 3 | left (2, 2), right (4, 4) |
| (2, 2) | 3 | 15 | 2 | null, null |
| (4, 4) | 4 | 7 | 4 | null, null |

Result `[3, 9, 20, null, null, 15, 7]` ✓, and pre = 5 at the end (every value used once).

### Dry run: the left chain, preorder `[3, 2, 1]`, inorder `[1, 2, 3]`

| call | root | mid | left range | right range |
|---|---|---|---|---|
| (0, 2) | 3 | 2 | (0, 1) | (3, 2) empty |
| (0, 1) | 2 | 1 | (0, 0) | (2, 1) empty |
| (0, 0) | 1 | 0 | empty | empty |

→ `[3, 2, null, 1]` ✓

### Complexity
- **Time O(n):** each value becomes a node once, with O(1) lookups.
- **Space O(n)** for the map, plus O(h) recursion.

---

## 6. Comparing the approaches

| | Search + copy | HashMap + pointer |
|---|---|---|
| Time | O(n²) worst case | **O(n)** |
| Extra space | O(n²) copies on a chain | O(n) |

The file rebuilds 500 random trees (with unique values in a non-sorted order) and checks that each one is identical to the original.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single node, a left chain, a right chain, an empty array.

**Mistakes:**
1. Building the right subtree before the left (the preorder pointer then picks the wrong roots).
2. Not resetting the static `pre` between calls.
3. A linear search for the root (O(n²) on chains).
4. An off-by-one in the ranges (`mid` instead of `mid − 1`).

---

## 8. Interview questions and answers

**Q1. Why must the left subtree be built first?**
> Preorder lists the root, then the entire left subtree, then the right subtree. The shared pointer only works if the recursion consumes values in that same order.

**Q2. Can you rebuild from preorder and postorder?**
> Not uniquely in general: a node with a single child could have it on either side. It's unique for **full** binary trees (every node has 0 or 2 children), LC 889.

### Follow-up questions

**F1. Inorder + postorder (LC 106).**
> The root is the **last** value of postorder. Walk the pointer backwards and build the **right** subtree first.

**F2. Preorder of a BST only (LC 1008).**
> The inorder is just the sorted preorder. Better: build with value bounds (like [M03](M03_ValidateBinarySearchTree.md)) in O(n).

**F3. Serialize and deserialize any tree.** → [H01](../04-Hard/H01_SerializeDeserializeBinaryTree.md)
> One preorder **with null markers** is enough, even with duplicate values.

---

## 9. Running the file

```bash
java M07_ConstructFromPreorderInorder.java      # 4/4 tests passed (including 500 random trees rebuilt exactly)
```

**One-line summary:** *"The next preorder value is the root; its position in inorder (from a HashMap) splits the left and right ranges; build the left first."*
