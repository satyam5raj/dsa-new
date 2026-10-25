# M02 · Binary Tree Right Side View

| | |
|---|---|
| **LeetCode** | [#199 Binary Tree Right Side View](https://leetcode.com/problems/binary-tree-right-side-view/) |
| **Difficulty** | Medium |
| **Pattern** | The last node of each BFS level (or DFS right-first, recording the first visit per depth) |
| **Code** | [M02_BinaryTreeRightSideView.java](M02_BinaryTreeRightSideView.java) |
| **Run** | `java M02_BinaryTreeRightSideView.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](../02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Standing on the **right** of the tree, which values do you see, from top to bottom?

```
      1            ← see 1
     / \
    2   3          ← see 3
     \
      5            ← see 5   (nothing on the right at this depth hides it!)

→ [1, 3, 5]
```

| tree | answer |
|---|---|
| `[1,2,3,null,5,null,4]` | `[1,3,4]` |
| `[1,2,3,null,5]` | `[1,3,5]` |
| `[1,null,3]` | `[1,3]` |
| `[]` | `[]` |

---

## 2. How to think about it

### The trap
"Just follow the right children" gives `[1, 3]` for the picture above. A node deep on the **left** side is visible when the right side is shorter.

What you see = **the rightmost node of each level**.

### BFS
The level-order template ([M01](M01_BinaryTreeLevelOrderTraversal.md)); record the **last** node polled in each level.

### DFS, right first
Visit the node, then the **right** child, then the left, passing the depth. The **first** node you reach at each depth is the rightmost one. Record it when `depth == view.size()` (the same "first visit to this depth" trick as M01's DFS).

```
1 (depth 0, new) → [1]
3 (depth 1, new) → [1, 3]
2 (depth 1, already seen)
5 (depth 2, new) → [1, 3, 5]
```

---

## 3. Java concepts used in this problem

### 3.1 Spotting the last element of a level: `i == size - 1`

Inside the BFS level loop, `i` counts from 0 to size − 1, so `i == size - 1` is the last node polled in the level: the rightmost one, because children are queued left before right.

### 3.2 Changing the visit order changes "first"

The DFS is just preorder with the two recursive calls swapped. Whichever side you visit first wins the "first at this depth" race. Left-first gives the **left** side view.

---

## 4. Approach 1: BFS, the last node per level (the file's main solution)

### Code ([M02_BinaryTreeRightSideView.java:39-54](M02_BinaryTreeRightSideView.java#L39-L54))

```java
static List<Integer> rightSideView(TreeNode root) {
    List<Integer> view = new ArrayList<>();
    if (root == null) return view;
    Queue<TreeNode> q = new ArrayDeque<>();
    q.add(root);
    while (!q.isEmpty()) {
        int size = q.size();
        for (int i = 0; i < size; i++) {
            TreeNode n = q.poll();
            if (i == size - 1) view.add(n.val);         // the last one on this level
            if (n.left != null) q.add(n.left);
            if (n.right != null) q.add(n.right);
        }
    }
    return view;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int size = q.size();` | the snapshot of this level |
| `if (i == size - 1) view.add(n.val);` | the rightmost node of the level |
| left, then right | keeps each level in left-to-right order, so the last one is the rightmost |

### Dry run: `[1, 2, 3, null, 5]`

| level | queue | the last one polled | view |
|---|---|---|---|
| 0 | [1] | 1 | [1] |
| 1 | [2, 3] | 3 | [1, 3] |
| 2 | [5] | 5 | [1, 3, **5**] |

The 5 is a **left**-side node (2's right child), but it's the only node at depth 2.

### Complexity
- **Time O(n).**
- **Space O(w).**

---

## 5. Approach 2: DFS, right first

### Code ([M02_BinaryTreeRightSideView.java:56-67](M02_BinaryTreeRightSideView.java#L56-L67))

```java
static List<Integer> rightSideViewDfs(TreeNode root) {
    List<Integer> view = new ArrayList<>();
    dfs(root, 0, view);
    return view;
}

static void dfs(TreeNode n, int depth, List<Integer> view) {
    if (n == null) return;
    if (depth == view.size()) view.add(n.val);         // first node seen at this depth
    dfs(n.right, depth + 1, view);                      // RIGHT first
    dfs(n.left, depth + 1, view);
}
```

### Line by line

| Line | What it does |
|---|---|
| `depth == view.size()` | no node at this depth has been recorded yet → this one is the rightmost |
| `dfs(n.right, …)` first | the right subtree gets the first chance at every depth |
| `dfs(n.left, …)` | fills in the depths the right side didn't reach |

### Dry run: `[1, 2, 3, null, 5, null, 4]`

```
      1
     / \
    2   3
     \   \
      5   4
```

| visit | depth | view.size() | recorded? | view |
|---|---|---|---|---|
| 1 | 0 | 0 | yes | [1] |
| 3 | 1 | 1 | yes | [1, 3] |
| 4 | 2 | 2 | yes | [1, 3, 4] |
| 2 | 1 | 3 | no | |
| 5 | 2 | 3 | no (4 already covers depth 2) | |

**[1, 3, 4]** ✓

### Complexity
- **Time O(n).**
- **Space O(h).**

---

## 6. Comparing the approaches

| | BFS | DFS right-first |
|---|---|---|
| Time | O(n) | O(n) |
| Space | O(w) | O(h) |
| Idea | the last of each level | the first visit per depth |

The file checks both on 500 random trees.

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty tree, a left-only chain (every node is visible), a deep left subtree under a short right one.

**Mistakes:**
1. Following only the right pointers.
2. In BFS, recording the first node of the level (that's the left view).
3. In DFS, visiting left first.

---

## 8. Interview questions and answers

**Q1. Why can a left-subtree node be visible?**
> Visibility is per level: if the right side has no node at some depth, the rightmost node at that depth may be in the left subtree.

**Q2. Why does DFS right-first work?**
> Within any depth, it reaches nodes from right to left, so the first one it reaches is the rightmost. `depth == view.size()` detects that first arrival.

### Follow-up questions

**F1. The left side view.**
> Record `i == 0` in BFS, or visit left first in DFS.

**F2. The top view / bottom view.**
> Track horizontal distances (left −1, right +1) with BFS; keep the first (top) or last (bottom) node per column ([H03 Vertical Order](../04-Hard/H03_VerticalOrderTraversal.md)).

**F3. The boundary of a binary tree (LC 545).**
> The left boundary, then the leaves left to right, then the right boundary in reverse.

---

## 9. Running the file

```bash
java M02_BinaryTreeRightSideView.java      # 6/6 tests passed (including 500 random trees: BFS == DFS)
```

**One-line summary:** *"You see the rightmost node of each level: the last one polled per BFS level, or the first one reached per depth in a right-first DFS."*
