# M10 · Binary Tree Zigzag Level Order Traversal

| | |
|---|---|
| **LeetCode** | [#103 Binary Tree Zigzag Level Order Traversal](https://leetcode.com/problems/binary-tree-zigzag-level-order-traversal/) |
| **Difficulty** | Medium |
| **Pattern** | Level-order BFS + fill each level from alternating ends (a deque) |
| **Code** | [M10_ZigzagLevelOrder.java](M10_ZigzagLevelOrder.java) |
| **Run** | `java M10_ZigzagLevelOrder.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](../02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem). The level-order template is in [M01](M01_BinaryTreeLevelOrderTraversal.md).

---

## 1. The problem

Level order, but alternate the direction: left → right, then right → left, then left → right, …

```
      3
     / \              → [[3], [20, 9], [15, 7]]
    9   20
       /  \
      15   7
```

| tree | answer |
|---|---|
| `[3,9,20,null,null,15,7]` | `[[3],[20,9],[15,7]]` |
| `[1,2,3,4,5,6,7]` | `[[1],[3,2],[4,5,6,7]]` |
| `[1]` | `[[1]]` |
| `[]` | `[]` |

---

## 2. How to think about it

### Simple: reverse every odd level
Do the normal level order ([M01](M01_BinaryTreeLevelOrderTraversal.md)) and reverse the levels at odd indices. It works, with one extra O(level size) pass per level.

### Cleaner: fill each level's list from alternating ends
Keep the BFS **exactly the same** (always queue the children left, then right), but build each level's values in a **Deque**:
- left → right: `addLast(val)`
- right → left: `addFirst(val)` (later nodes end up in front, so the level is reversed)

```
level 0 (L→R): 3                          → [3]
level 1 (R→L): addFirst 9 → [9], addFirst 20 → [20, 9]
level 2 (L→R): 15, 7                      → [15, 7]
```

**Don't change the order in which children are queued.** Flipping the queue order per level gets confusing fast, and it's the usual source of bugs in this problem.

---

## 3. Java concepts used in this problem

### 3.1 `Deque` as a double-ended list

```java
Deque<Integer> level = new ArrayDeque<>();
level.addLast(x);      // append at the back
level.addFirst(x);     // insert at the front, O(1)
```

Both are O(1). An `ArrayList` would need `add(0, x)`, which shifts everything (O(n) per insert).

### 3.2 Converting a deque to a list

```java
result.add(new ArrayList<>(level));
```

The result type is `List<List<Integer>>`, so each level's deque is copied into an `ArrayList`, in the deque's front-to-back order.

### 3.3 Toggling a boolean

```java
leftToRight = !leftToRight;
```

It flips the direction after each level.

### 3.4 `Collections.reverse(list)`

It reverses a list in place, in O(n). The simple version uses it on the odd levels.

---

## 4. Approach 1: Level order + reverse the odd levels

### Code ([M10_ZigzagLevelOrder.java:62-78](M10_ZigzagLevelOrder.java#L62-L78))

```java
static List<List<Integer>> simple(TreeNode root) {
    List<List<Integer>> result = new ArrayList<>();
    Queue<TreeNode> q = new ArrayDeque<>();
    if (root != null) q.add(root);
    while (!q.isEmpty()) {
        List<Integer> level = new ArrayList<>();
        for (int i = q.size(); i > 0; i--) {
            TreeNode n = q.poll();
            level.add(n.val);
            if (n.left != null) q.add(n.left);
            if (n.right != null) q.add(n.right);
        }
        if (result.size() % 2 == 1) Collections.reverse(level);
        result.add(level);
    }
    return result;
}
```

| Line | What it does |
|---|---|
| the BFS | exactly M01 |
| `result.size() % 2 == 1` | the level about to be added has an odd index → reverse it |

Easy to get right; one extra pass per odd level.

---

## 5. Approach 2: A deque per level (the file's main solution)

### Code ([M10_ZigzagLevelOrder.java:40-59](M10_ZigzagLevelOrder.java#L40-L59))

```java
static List<List<Integer>> zigzagLevelOrder(TreeNode root) {
    List<List<Integer>> result = new ArrayList<>();
    if (root == null) return result;
    Queue<TreeNode> q = new ArrayDeque<>();
    q.add(root);
    boolean leftToRight = true;
    while (!q.isEmpty()) {
        Deque<Integer> level = new ArrayDeque<>();
        for (int i = q.size(); i > 0; i--) {
            TreeNode n = q.poll();
            if (leftToRight) level.addLast(n.val);
            else level.addFirst(n.val);
            if (n.left != null) q.add(n.left);          // children ALWAYS left then right
            if (n.right != null) q.add(n.right);
        }
        result.add(new ArrayList<>(level));
        leftToRight = !leftToRight;
    }
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| `boolean leftToRight = true;` | level 0 goes left to right |
| `Deque<Integer> level` | this level's values, filled from either end |
| `addLast` / `addFirst` | the normal order, or reversed |
| the children | always queued left then right: the BFS itself never changes |
| `result.add(new ArrayList<>(level));` | copy the level into the result |
| `leftToRight = !leftToRight;` | alternate |

### Dry run: `[1, 2, 3, 4, 5, 6, 7]`

| level | direction | polled (in BFS order) | deque after each | result entry |
|---|---|---|---|---|
| 0 | L→R | 1 | [1] | [1] |
| 1 | R→L | 2, 3 | [2] → [3, 2] | [3, 2] |
| 2 | L→R | 4, 5, 6, 7 | [4] → [4, 5] → [4, 5, 6] → [4, 5, 6, 7] | [4, 5, 6, 7] |

→ `[[1], [3, 2], [4, 5, 6, 7]]` ✓

### Complexity
- **Time O(n).**
- **Space O(w).**

---

## 6. Approach 3: DFS with the depth

```java
static List<List<Integer>> zigzagDfs(TreeNode root) {
    List<List<Integer>> result = new ArrayList<>();
    dfs(root, 0, result);
    return result;
}

static void dfs(TreeNode n, int depth, List<List<Integer>> result) {
    if (n == null) return;
    if (depth == result.size()) result.add(new LinkedList<>());
    if (depth % 2 == 0) result.get(depth).add(n.val);          // even: append
    else result.get(depth).add(0, n.val);                      // odd: prepend (O(1) on a LinkedList)
    dfs(n.left, depth + 1, result);
    dfs(n.right, depth + 1, result);
}
```

M01's DFS, with odd levels prepended. A `LinkedList` makes `add(0, x)` O(1).

---

## 7. Comparing the approaches

| | Reverse odd levels | Deque per level | DFS + prepend |
|---|---|---|---|
| Time | O(n) | O(n) | O(n) |
| Space | O(w) | O(w) | O(h) |
| Risk | low | low | `add(0, …)` on an ArrayList is O(n) |

The file checks the deque version against the reverse-odd-levels version on 500 random trees.

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty tree, a single node, a chain (each level has one node, so the direction doesn't matter).

**Mistakes:**
1. Changing the order in which children are queued, depending on the direction.
2. Using `ArrayList.add(0, x)`, which is quadratic on wide levels.
3. Starting with right-to-left.

---

## 9. Interview questions and answers

**Q1. Why not swap the order of the children in the queue on alternate levels?**
> Then the **next** level's order depends on the reversed order of this one, so the reversals compound and the levels come out wrong. Keeping the BFS fixed and only changing how each level is recorded is much simpler.

**Q2. Can it be done with two stacks?**
> Yes: pop from the current stack, and push the children into the other stack, left-then-right on one level and right-then-left on the next. The LIFO order produces the zigzag.

### Follow-up questions

**F1. Spiral matrix order.**
> The same "alternate the direction" idea on a 2D grid ([Arrays](../../01-Arrays/README.md)).

**F2. Vertical order traversal.** → [H03](../04-Hard/H03_VerticalOrderTraversal.md)
> Group by column, not by level.

**F3. Zigzag over an N-ary tree.**
> The same deque trick per level.

---

## 10. Running the file

```bash
java M10_ZigzagLevelOrder.java      # 5/5 tests passed (including 500 random trees: deque == reverse odd levels)
```

**One-line summary:** *"Do a normal BFS (children always left then right) but record each level in a deque: addLast on even levels, addFirst on odd ones."*
