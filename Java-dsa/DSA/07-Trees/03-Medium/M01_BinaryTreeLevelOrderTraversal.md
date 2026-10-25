# M01 · Binary Tree Level Order Traversal

| | |
|---|---|
| **LeetCode** | [#102 Binary Tree Level Order Traversal](https://leetcode.com/problems/binary-tree-level-order-traversal/) |
| **Difficulty** | Medium |
| **Pattern** | BFS with the level-size snapshot (the template for every level problem) |
| **Code** | [M01_BinaryTreeLevelOrderTraversal.java](M01_BinaryTreeLevelOrderTraversal.java) |
| **Run** | `java M01_BinaryTreeLevelOrderTraversal.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](../02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Return the node values **level by level**, left to right, as a list of lists.

```
      3
     / \            → [[3], [9, 20], [15, 7]]
    9   20
       /  \
      15   7
```

| tree | answer |
|---|---|
| `[3,9,20,null,null,15,7]` | `[[3],[9,20],[15,7]]` |
| `[1]` | `[[1]]` |
| `[]` | `[]` |

---

## 2. How to think about it

### Why a queue?
FIFO order means everything at depth d is dequeued before anything at depth d + 1: children are always added **behind** the current level.

### How do you know where a level ends?
At the start of each round, the queue holds **exactly one full level**. Snapshot `size = q.size()` and poll exactly that many nodes (adding their children behind them). Those `size` nodes are one level.

```
q = [3]         size 1 → level [3]          q = [9, 20]
q = [9, 20]     size 2 → level [9, 20]      q = [15, 7]
q = [15, 7]     size 2 → level [15, 7]      q = []
```

### The DFS alternative
Pass the depth down. Append the value to `result[depth]`, creating that list the first time the depth is reached. Preorder visits left before right, so each level still comes out left to right.

### One template, many problems
Right side view ([M02](M02_BinaryTreeRightSideView.md)), zigzag ([M10](M10_ZigzagLevelOrder.md)), level averages, the largest value per level, min depth ([E10](../02-Easy/E10_MinimumDepthOfBinaryTree.md)), bottom-up level order.

---

## 3. Java concepts used in this problem

### 3.1 `List<List<Integer>>`

A list whose elements are lists: `result.get(d)` is level d's list. Created with `new ArrayList<>()`, each inner list added separately.

### 3.2 `new ArrayList<>(size)`: an initial capacity

```java
List<Integer> level = new ArrayList<>(size);
```

The argument is the **capacity**, not the length: the list is still empty, but it won't need to grow while we add `size` elements. (Not to be confused with `Arrays.asList` or `List.of`.)

### 3.3 `offer`/`poll` vs `add`/`remove`

On a `Queue`, `offer` and `add` both enqueue (`offer` returns false on a capacity-limited queue instead of throwing), and `poll` returns null on an empty queue while `remove` throws. With an unbounded `ArrayDeque` they behave the same. (Queue basics: [Queue E01](../../05-Queue/02-Easy/E01_ImplementStackUsingQueues.md).)

### 3.4 `depth == result.size()` means "first visit to this depth"

In the DFS, depths are reached in order 0, 1, 2, … (a child is always one deeper than its parent), so the first time we reach depth d, the result has exactly d lists.

---

## 4. Approach 1: BFS with the level-size snapshot (the file's solution)

### Code ([M01_BinaryTreeLevelOrderTraversal.java:40-57](M01_BinaryTreeLevelOrderTraversal.java#L40-L57))

```java
static List<List<Integer>> levelOrder(TreeNode root) {
    List<List<Integer>> result = new ArrayList<>();
    if (root == null) return result;
    Queue<TreeNode> q = new ArrayDeque<>();
    q.offer(root);
    while (!q.isEmpty()) {
        int size = q.size();                           // the whole current level
        List<Integer> level = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            TreeNode n = q.poll();
            level.add(n.val);
            if (n.left != null) q.offer(n.left);
            if (n.right != null) q.offer(n.right);
        }
        result.add(level);
    }
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (root == null) return result;` | an empty tree → `[]` (and ArrayDeque can't hold null) |
| `int size = q.size();` | the snapshot: exactly this level's nodes are in the queue |
| `for (int i = 0; i < size; i++)` | uses the **saved** `size`, not `q.size()`, which grows during the loop |
| `level.add(n.val);` | record the value |
| the two `offer`s | the children go behind, into the next level |
| `result.add(level);` | one finished level |

### Dry run: `[3, 9, 20, null, null, 15, 7]`

| round | queue at the start | size | polled | children added | level | result |
|---|---|---|---|---|---|---|
| 1 | [3] | 1 | 3 | 9, 20 | [3] | [[3]] |
| 2 | [9, 20] | 2 | 9, 20 | 15, 7 | [9, 20] | [[3], [9, 20]] |
| 3 | [15, 7] | 2 | 15, 7 | — | [15, 7] | [[3], [9, 20], [15, 7]] |

### Complexity
- **Time O(n).**
- **Space O(w):** the widest level, up to n/2 for the last level of a perfect tree.

---

## 5. Approach 2: DFS with the depth (preorder)

### Code ([M01_BinaryTreeLevelOrderTraversal.java:59-71](M01_BinaryTreeLevelOrderTraversal.java#L59-L71))

```java
static List<List<Integer>> levelOrderDfs(TreeNode root) {
    List<List<Integer>> result = new ArrayList<>();
    dfs(root, 0, result);
    return result;
}

static void dfs(TreeNode n, int depth, List<List<Integer>> result) {
    if (n == null) return;
    if (depth == result.size()) result.add(new ArrayList<>());   // first visit to this depth
    result.get(depth).add(n.val);
    dfs(n.left, depth + 1, result);
    dfs(n.right, depth + 1, result);
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (depth == result.size())` | the first node at this depth: create its list |
| `result.get(depth).add(n.val)` | append to this depth's list |
| left before right | keeps each level in left-to-right order |

### Dry run

| visit | depth | result after |
|---|---|---|
| 3 | 0 | [[3]] |
| 9 | 1 | [[3], [9]] |
| 20 | 1 | [[3], [9, 20]] |
| 15 | 2 | [[3], [9, 20], [15]] |
| 7 | 2 | [[3], [9, 20], [15, 7]] |

The levels aren't produced in order of time, but each one is filled left to right.

### Complexity
- **Time O(n).**
- **Space O(h)** recursion (plus the output).

---

## 6. Comparing the approaches

| | BFS | DFS with depth |
|---|---|---|
| Time | O(n) | O(n) |
| Extra space | O(w) | O(h) |
| Natural for | level-by-level processing, early stops | when recursion is simpler, deep but narrow trees |

The file checks both on 500 random trees.

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty tree, a single node, a chain (one node per level), a perfect tree.

**Mistakes:**
1. `for (int i = 0; i < q.size(); i++)`: the size grows as children are added, so levels run together.
2. Adding null children to an `ArrayDeque`.
3. In DFS, visiting right before left (each level comes out reversed).

---

## 8. Interview questions and answers

**Q1. How do you separate the levels in BFS?**
> Snapshot the queue size at the start of each round. Exactly that many nodes belong to the current level, because everything behind them was added by this level.

**Q2. BFS or DFS: which uses less memory?**
> BFS uses O(width), DFS uses O(height). A perfect tree is wide at the bottom (n/2) but only log n deep, so DFS wins there. A chain is the opposite.

### Follow-up questions

**F1. Bottom-up level order (LC 107).**
> The same BFS, then reverse the result (or insert each level at index 0 of a `LinkedList`).

**F2. The average of each level (LC 637).**
> Sum the values in the inner loop (as a `long`) and divide by `size`.

**F3. Level order for an N-ary tree.**
> Loop over `n.children` instead of left/right.

**F4. A null-marker variant (no size snapshot).**
> Enqueue a `null` after each level; when you poll it, the level has ended (needs a `LinkedList`, since ArrayDeque rejects nulls).

---

## 9. Running the file

```bash
java M01_BinaryTreeLevelOrderTraversal.java      # 5/5 tests passed (including 500 random trees: BFS == DFS)
```

**One-line summary:** *"BFS: snapshot q.size(), poll exactly that many nodes (queuing their children), and that's one level."*
