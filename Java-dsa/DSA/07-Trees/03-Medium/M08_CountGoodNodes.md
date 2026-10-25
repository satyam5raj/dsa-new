# M08 · Count Good Nodes in Binary Tree

| | |
|---|---|
| **LeetCode** | [#1448 Count Good Nodes in Binary Tree](https://leetcode.com/problems/count-good-nodes-in-binary-tree/) |
| **Difficulty** | Medium |
| **Pattern** | Top-down: pass the "max so far" on the path |
| **Code** | [M08_CountGoodNodes.java](M08_CountGoodNodes.java) |
| **Run** | `java M08_CountGoodNodes.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](../02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

A node X is **good** if no node on the path from the root to X has a value **greater** than X. Count the good nodes.

```
          3*
         / \
        1   4*          good: 3 (root), 3 (the leftmost leaf), 4, 5  → 4
       /   / \
      3*  1   5*
```

| tree | answer |
|---|---|
| `[3,1,4,3,null,1,5]` | 4 |
| `[3,3,null,4,2]` | 3 (3, 3, 4; the 2 is not good) |
| `[1]` | 1 |

---

## 2. How to think about it

### What does "good" depend on?
Only on the **maximum value above the node** on its root-to-node path. So pass that maximum **down** (top-down, like [E07 Path Sum](../02-Easy/E07_PathSum.md)):

```
good(node, maxAbove):
    isGood = node.val >= maxAbove
    newMax = max(maxAbove, node.val)
    return (isGood ? 1 : 0) + good(left, newMax) + good(right, newMax)
```

Start with `maxAbove = −∞`, so the root is always good. Note `>=`: a node **equal** to the max above it is still good ("no node is greater").

### Dry run (picture above)

```
3 (max −∞): good → max 3
  1 (max 3): not good → max 3
    3 (max 3): good (equal counts) 
  4 (max 3): good → max 4
    1 (max 4): not good
    5 (max 4): good
total 4
```

---

## 3. Java concepts used in this problem

### 3.1 `Integer.MIN_VALUE` as "−∞"

```java
count(root, Integer.MIN_VALUE)
```

Every int is ≥ `Integer.MIN_VALUE`, so the root always counts as good, even if its value is MIN_VALUE itself (thanks to `>=`).

### 3.2 Combining a count from both subtrees

```java
return good + count(n.left, newMax) + count(n.right, newMax);
```

It's top-down (newMax flows down) **and** bottom-up (the counts flow up), all in one return expression.

### 3.3 `allMatch` on a stream (in the referee)

```java
boolean good = path.stream().allMatch(v -> v <= n.val);
```

True if **every** value on the path is ≤ n.val (and true for an empty path). The referee carries the whole path in a shared list with backtracking, as in [M06](M06_PathSumII.md).

---

## 4. Approach 1: Carry the whole path (the referee)

### Code ([M08_CountGoodNodes.java:52-59](M08_CountGoodNodes.java#L52-L59))

```java
static int brute(TreeNode n, List<Integer> path) {
    if (n == null) return 0;
    boolean good = path.stream().allMatch(v -> v <= n.val);
    path.add(n.val);
    int c = (good ? 1 : 0) + brute(n.left, path) + brute(n.right, path);
    path.remove(path.size() - 1);
    return c;
}
```

| Line | What it does |
|---|---|
| `allMatch(v -> v <= n.val)` | check every ancestor explicitly: O(h) per node |
| `path.add` / `path.remove` | backtracking on the shared path |

**O(n · h)**. It works, but it recomputes something that a single number can summarise.

---

## 5. Approach 2: Pass the max down (the file's solution)

### Code ([M08_CountGoodNodes.java:40-49](M08_CountGoodNodes.java#L40-L49))

```java
static int goodNodes(TreeNode root) {
    return count(root, Integer.MIN_VALUE);
}

static int count(TreeNode n, int maxAbove) {
    if (n == null) return 0;
    int good = n.val >= maxAbove ? 1 : 0;
    int newMax = Math.max(maxAbove, n.val);
    return good + count(n.left, newMax) + count(n.right, newMax);
}
```

### Line by line

| Line | What it does |
|---|---|
| `count(root, Integer.MIN_VALUE)` | nothing is above the root |
| `if (n == null) return 0;` | no node, no good nodes |
| `n.val >= maxAbove ? 1 : 0` | good if nothing above is larger |
| `Math.max(maxAbove, n.val)` | the max that the children will see |
| `good + count(left) + count(right)` | this node plus the good nodes below |

### Dry run: `[3, 3, null, 4, 2]`

```
      3
     /
    3
   / \
  4   2
```

| node | maxAbove | good? | newMax |
|---|---|---|---|
| 3 (root) | −∞ | ✓ | 3 |
| 3 | 3 | ✓ (equal) | 3 |
| 4 | 3 | ✓ | 4 |
| 2 | 3 | ✗ | 3 |

Total **3** ✓

### Complexity
- **Time O(n).**
- **Space O(h).**

---

## 6. Comparing the approaches

| | Carry the whole path | Pass the max |
|---|---|---|
| Time | O(n · h) | **O(n)** |
| Space | O(h) | O(h) |

The file checks both on 1,000 random trees with values −5..5.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single node, equal values along a path (all good), a decreasing chain (only the root is good), negative values.

**Mistakes:**
1. Using `>` instead of `>=` (equal values are good).
2. Starting with 0 as "−∞" (fails with negative values).
3. Updating the max before checking the node.
4. Comparing only with the parent instead of the whole path's max.

---

## 8. Interview questions and answers

**Q1. Why is one number enough instead of the whole path?**
> "No ancestor is greater than X" is the same as "the max of the ancestors ≤ X". The max is all that matters.

**Q2. Top-down or bottom-up?**
> Both: the max flows down as a parameter, and the counts flow up as return values.

### Follow-up questions

**F1. Count the nodes that are strictly greater than all their ancestors.**
> Use `>` instead of `>=`.

**F2. The maximum difference between a node and an ancestor (LC 1026).**
> Pass both the min and the max of the path down; at each node, the candidate is max(|val − min|, |val − max|).

**F3. Iterative version.**
> BFS or a stack of (node, maxAbove) pairs.

---

## 9. Running the file

```bash
java M08_CountGoodNodes.java      # 4/4 tests passed (including 1,000 random trees vs the full-path check)
```

**One-line summary:** *"Pass the max value on the path down; a node is good if val ≥ that max, then recurse with max(maxAbove, val)."*
