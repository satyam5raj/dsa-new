# M04 · Kth Smallest Element in a BST

| | |
|---|---|
| **LeetCode** | [#230 Kth Smallest Element in a BST](https://leetcode.com/problems/kth-smallest-element-in-a-bst/) |
| **Difficulty** | Medium |
| **Pattern** | Inorder = sorted; stop after k nodes (iterative inorder) |
| **Code** | [M04_KthSmallestElementInBST.java](M04_KthSmallestElementInBST.java) |
| **Run** | `java M04_KthSmallestElementInBST.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](../02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem). BST basics: [E08](../02-Easy/E08_ConvertSortedArrayToBST.md).

---

## 1. The problem

Return the **k-th smallest** value (1-indexed) in a BST.

```
          5
         / \
        3   6          k = 3 → 3   (sorted: 1 2 3 4 5 6)
       / \
      2   4
     /
    1
```

| tree | k | answer |
|---|---|---|
| the tree above | 3 | 3 |
| the tree above | 1 | 1 (the min) |
| the tree above | 6 | 6 (the max) |
| `[3,1,4,null,2]` | 1 | 1 |

---

## 2. How to think about it

### Brute force
Collect the whole inorder traversal (which is sorted) into a list and return `list[k − 1]`. O(n) time and space.

### Better: count and stop
Inorder visits the nodes in sorted order, so just **count** as you visit and **stop** at the k-th one. With the iterative inorder: push the left spine, pop, count, go right. **Time O(h + k):** we walk down once, then visit k nodes.

```
push 5, 3, 2, 1
pop 1  (count 1)
pop 2  (count 2)
pop 3  (count 3 = k) → return 3
```

### The follow-up interviewers love
"The BST is modified often and you query the k-th smallest often." → store the **subtree size** in every node (an **order-statistic tree**):

```
leftSize = size(node.left)
k == leftSize + 1   → this node
k <= leftSize       → go left with the same k
else                → go right with k − leftSize − 1
```

That's O(h) per query, so O(log n) in a balanced tree.

---

## 3. Java concepts used in this problem

### 3.1 The iterative inorder traversal with a stack

```java
Deque<TreeNode> stack = new ArrayDeque<>();
TreeNode cur = root;
while (cur != null || !stack.isEmpty()) {
    while (cur != null) { stack.push(cur); cur = cur.left; }   // 1. go as far left as possible
    cur = stack.pop();                                           // 2. visit the smallest unvisited node
    // ... visit cur ...
    cur = cur.right;                                             // 3. then its right subtree
}
```

The stack holds the "ancestors still waiting to be visited". It replaces the call stack of the recursive inorder, and, unlike recursion, it's easy to **stop in the middle**. (`push`/`pop` on an ArrayDeque work on the front, so it behaves as a stack: [Stack E01](../../04-Stack/02-Easy/E01_ValidParentheses.md).)

### 3.2 `while (true)` with a guaranteed return

The file uses `while (true)` because k is guaranteed to be ≤ n, so the k-th pop always happens. Without that guarantee, use the full loop condition above.

### 3.3 Pre-decrement inside a condition: `--k == 0`

```java
if (--k == 0) return cur.val;
```

`--k` decrements **first**, then compares. It counts down from k, and the k-th popped node makes it 0. (`k-- == 0` would compare the old value: an off-by-one.)

### 3.4 A `HashMap<TreeNode, Integer>` as a memo

```java
static Map<TreeNode, Integer> sizes = new HashMap<>();
```

TreeNode doesn't override `equals`/`hashCode`, so the map uses **identity**: each node object is its own key. That's exactly right for caching per-node data without changing the class. (In a real order-statistic tree, the size would be a field of the node, updated on insert and delete.)

---

## 4. Approach 1: Full inorder list

```java
static int kthSmallestList(TreeNode root, int k) {
    List<Integer> sorted = new ArrayList<>();
    collect(root, sorted);
    return sorted.get(k - 1);
}

static void collect(TreeNode n, List<Integer> out) {
    if (n == null) return;
    collect(n.left, out);
    out.add(n.val);
    collect(n.right, out);
}
```

Simple and correct: **O(n)** time and space, even when k = 1.

---

## 5. Approach 2: Iterative inorder, stop at k (the file's main solution)

### Code ([M04_KthSmallestElementInBST.java:44-56](M04_KthSmallestElementInBST.java#L44-L56))

```java
static int kthSmallest(TreeNode root, int k) {
    Deque<TreeNode> stack = new ArrayDeque<>();
    TreeNode cur = root;
    while (true) {
        while (cur != null) {             // go as far left as possible
            stack.push(cur);
            cur = cur.left;
        }
        cur = stack.pop();                // the next smallest
        if (--k == 0) return cur.val;
        cur = cur.right;
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| the inner `while` | push the whole left spine of the current subtree; the top of the stack is now its minimum |
| `cur = stack.pop();` | the next smallest value overall |
| `if (--k == 0) return cur.val;` | was that the k-th? |
| `cur = cur.right;` | the next values come from cur's right subtree (if any), before cur's ancestors |

### Dry run: the tree above, k = 3

| step | action | stack (top last) | k |
|---|---|---|---|
| 1 | push 5, 3, 2, 1 | [5, 3, 2, 1] | 3 |
| 2 | pop 1 → k = 2; cur = 1.right = null | [5, 3, 2] | 2 |
| 3 | (nothing to push) pop 2 → k = 1; cur = null | [5, 3] | 1 |
| 4 | pop 3 → **k = 0 → return 3** | [5] | 0 |

Node 4, 5 and 6 are never visited.

### Dry run: `[3, 1, 4, null, 2]`, k = 1
push 3, 1 → pop 1 → k = 0 → **return 1** ✓

### Complexity
- **Time O(h + k).**
- **Space O(h)** for the stack.

---

## 6. Approach 3: Subtree sizes (the follow-up)

### Code ([M04_KthSmallestElementInBST.java:59-75](M04_KthSmallestElementInBST.java#L59-L75))

```java
static Map<TreeNode, Integer> sizes = new HashMap<>();

static int size(TreeNode n) {
    if (n == null) return 0;
    Integer s = sizes.get(n);
    if (s == null) { s = 1 + size(n.left) + size(n.right); sizes.put(n, s); }
    return s;
}

static int kthBySize(TreeNode n, int k) {
    while (true) {
        int leftSize = size(n.left);
        if (k == leftSize + 1) return n.val;
        if (k <= leftSize) n = n.left;
        else { k -= leftSize + 1; n = n.right; }
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `size(n)` | the number of nodes in n's subtree, memoised in the map |
| `leftSize = size(n.left)` | how many values are smaller than n.val **within this subtree** |
| `k == leftSize + 1` | n itself is the k-th |
| `k <= leftSize` | the answer is among the smaller values: go left with the same k |
| `k -= leftSize + 1; n = n.right;` | skip the left subtree and n itself, then look in the right subtree |

### Dry run: the tree above, k = 4

Sizes: 1 → 1, 2 → 2, 4 → 1, 3 → 4, 6 → 1, 5 → 6.

| at | leftSize | k | decision |
|---|---|---|---|
| 5 | 4 | 4 | 4 ≤ 4 → go left |
| 3 | 2 | 4 | 4 > 3 → k = 4 − 3 = 1, go right |
| 4 | 0 | 1 | 1 == 0 + 1 → **return 4** |

### Complexity
- **Query O(h)** once the sizes are known; computing all of them is a one-time O(n).
- In a real order-statistic tree, insert and delete update the sizes along their path, O(h) each.

---

## 7. Comparing the approaches

| | Full inorder list | Iterative inorder, stop at k | Subtree sizes |
|---|---|---|---|
| Time per query | O(n) | O(h + k) | **O(h)** |
| Extra space | O(n) | O(h) | O(n) for the sizes |
| Best when | it's simple enough | a single query | many queries + updates |

The file checks the iterative inorder and the size version on 300 random BSTs, for every k.

---

## 8. Edge cases and common mistakes

**Edge cases:** k = 1 (the leftmost node), k = n (the rightmost), a chain, a single node.

**Mistakes:**
1. `k--` instead of `--k` (off by one).
2. Forgetting to go right after the pop (`cur = cur.right`).
3. In the size version, going right without subtracting `leftSize + 1`.
4. A stale size cache after modifying the tree.

---

## 9. Interview questions and answers

**Q1. Why is the iterative traversal better than the recursive one here?**
> It can stop as soon as the k-th node is popped. A recursive version needs a counter and a way to propagate the stop through every return.

**Q2. How would you support frequent inserts, deletes and k-th queries?**
> Augment every node with its subtree size and keep it updated along the insert/delete path. Then each k-th query is O(h), so O(log n) with a balanced tree (AVL / red-black).

### Follow-up questions

**F1. The k-th largest.**
> A reverse inorder (right, node, left), or the k-th smallest with k' = n − k + 1.

**F2. The rank of a value (how many are smaller?).**
> With sizes: walk down; every time you go right, add `size(left) + 1`.

**F3. The k-th smallest in a stream (not a BST).**
> A max-heap of size k ([08-Heap](../../08-Heap/README.md)).

---

## 10. Running the file

```bash
java M04_KthSmallestElementInBST.java      # 6/6 tests passed (including 300 random BSTs, every k)
```

**One-line summary:** *"An iterative inorder visits values in sorted order: push the left spine, pop, count down k, go right; for many queries, keep subtree sizes and walk down in O(h)."*
