# M06 · Path Sum II

| | |
|---|---|
| **LeetCode** | [#113 Path Sum II](https://leetcode.com/problems/path-sum-ii/) |
| **Difficulty** | Medium |
| **Pattern** | DFS + backtracking (one shared path list: add, recurse, remove) |
| **Code** | [M06_PathSumII.java](M06_PathSumII.java) |
| **Run** | `java M06_PathSumII.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](../02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Return **all** root-to-leaf paths whose values sum to `targetSum`.

```
            5
           / \
          4   8
         /   / \          target 22 → [[5, 4, 11, 2], [5, 8, 4, 5]]
        11  13  4
       /  \    / \
      7    2  5   1
```

| tree | target | answer |
|---|---|---|
| the tree above | 22 | `[[5,4,11,2],[5,8,4,5]]` |
| `[1,2,3]` | 5 | `[]` |
| `[1,2]` | 0 | `[]` |

---

## 2. How to think about it

[Path Sum](../02-Easy/E07_PathSum.md) answered yes/no by passing the remaining target down. Now we must **record** the paths.

### Naive: a new list for every child
Pass a **copy** of the path to each child. It's simple, but it copies O(h) elements at every node: O(n · h) copying even when nothing matches.

### Backtracking: one shared list
Keep **one** list for the current path:

```
path.add(node)                          ← choose
if leaf and remaining == 0:
    result.add(new ArrayList<>(path))   ← copy only on success!
recurse left, recurse right
path.remove(last)                       ← un-choose: the parent's list is as it was
```

**The #1 bug: forgetting the copy.** Without `new ArrayList<>(path)`, you'd add the **same** list object every time, and by the end it's empty (everything has been removed again), so the result is a list of empty lists.

This add / recurse / remove shape is the core of all backtracking: subsets, permutations, N-Queens ([14-Backtracking](../../14-Backtracking/README.md)).

---

## 3. Java concepts used in this problem

### 3.1 References vs copies of a list

```java
List<Integer> path = new ArrayList<>();
result.add(path);                       // stores a REFERENCE: later changes to path show up in result
result.add(new ArrayList<>(path));      // stores a SNAPSHOT: an independent copy
```

The copy constructor `new ArrayList<>(collection)` creates a new list with the same elements.

### 3.2 `path.remove(path.size() - 1)`: remove by index

An `int` argument picks the index overload, so this removes the **last** element in O(1). (Removing by value would need `Integer.valueOf`, and would remove the first matching value, which could be the wrong one when values repeat.)

### 3.3 `removeIf` with a lambda (in the test)

```java
all.removeIf(p -> p.stream().mapToInt(Integer::intValue).sum() != target);
```

This deletes every path whose sum isn't the target. `p.stream().mapToInt(Integer::intValue).sum()` sums a `List<Integer>`.

### 3.4 Why the remaining sum needs no undo

`remaining` is an `int` parameter, so each call has its own copy (as in [E07](../02-Easy/E07_PathSum.md)). The **list** is shared, which is why it, and only it, needs the explicit `remove`.

---

## 4. Approach 1: Copy the path at every node (the referee)

### Code ([M06_PathSumII.java:62-69](M06_PathSumII.java#L62-L69))

```java
static void allPaths(TreeNode n, List<Integer> soFar, List<List<Integer>> out) {
    if (n == null) return;
    List<Integer> p = new ArrayList<>(soFar);
    p.add(n.val);
    if (n.left == null && n.right == null) out.add(p);
    allPaths(n.left, p, out);
    allPaths(n.right, p, out);
}
```

Each node gets its own copy, so there's nothing to undo. Collect every root-to-leaf path, then filter by sum. It's simple, but it copies at every node: O(n · h).

---

## 5. Approach 2: Backtracking with one shared list (the file's solution)

### Code ([M06_PathSumII.java:42-59](M06_PathSumII.java#L42-L59))

```java
static List<List<Integer>> pathSum(TreeNode root, int targetSum) {
    List<List<Integer>> result = new ArrayList<>();
    dfs(root, targetSum, new ArrayList<>(), result);
    return result;
}

static void dfs(TreeNode n, int remaining, List<Integer> path, List<List<Integer>> result) {
    if (n == null) return;
    path.add(n.val);                                          // choose
    remaining -= n.val;
    if (n.left == null && n.right == null && remaining == 0) {
        result.add(new ArrayList<>(path));                    // COPY the current path
    } else {
        dfs(n.left, remaining, path, result);
        dfs(n.right, remaining, path, result);
    }
    path.remove(path.size() - 1);                             // un-choose (backtrack)
}
```

### Line by line

| Line | What it does |
|---|---|
| `path.add(n.val);` | extend the current path with this node |
| `remaining -= n.val;` | the local copy of what's still needed |
| the leaf + `remaining == 0` check | a complete matching path → store a **snapshot** |
| `else` the two recursive calls | explore below (a leaf that doesn't match just falls through to the backtrack) |
| `path.remove(path.size() - 1);` | undo the `add`, so the caller sees its own path again |

Every `add` is matched by exactly one `remove` on every return path, which is what keeps the shared list consistent.

### Dry run: target 22 (path contents when each node is reached)

| visit | path after add | remaining | event |
|---|---|---|---|
| 5 | [5] | 17 | |
| 4 | [5, 4] | 13 | |
| 11 | [5, 4, 11] | 2 | |
| 7 | [5, 4, 11, 7] | −5 | a leaf, no match → remove 7 |
| 2 | [5, 4, 11, 2] | 0 | **a leaf, match → copy [5, 4, 11, 2]** → remove 2 |
| (back up) | [5, 4] → [5] | | remove 11, then 4 |
| 8 | [5, 8] | 9 | |
| 13 | [5, 8, 13] | −4 | a leaf, no match |
| 4 | [5, 8, 4] | 5 | |
| 5 | [5, 8, 4, 5] | 0 | **match → copy [5, 8, 4, 5]** |
| 1 | [5, 8, 4, 1] | 4 | no match |
| (back up) | … → [] | | |

result = `[[5, 4, 11, 2], [5, 8, 4, 5]]` ✓, and the shared path is empty again at the end.

### Complexity
- **Time O(n · h)** worst case (copying each found path costs O(h)). Without many matches, it's O(n).
- **Space O(h)** for the path and the recursion (plus the output).

---

## 6. Comparing the approaches

| | Copy at every node | Backtracking |
|---|---|---|
| Copies | at every node | only for matching paths |
| Time | O(n · h) always | O(n + matches · h) |
| Extra space | O(n · h) total allocations | O(h) |

The file checks the backtracking version against the referee on 1,000 random trees (with negative values).

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty tree, no matching path, negative values, several matching paths, a single-node tree equal to the target.

**Mistakes:**
1. `result.add(path)` without copying → the result fills with references to one list that ends up empty.
2. Forgetting `path.remove` (paths accumulate nodes from other branches).
3. Returning right after a match **before** removing the node (the list is left dirty).
4. Matching at a non-leaf node.

---

## 8. Interview questions and answers

**Q1. Why copy only on success?**
> The shared list changes as the search continues; a snapshot freezes the path at the moment it matched. Copying elsewhere is wasted work.

**Q2. What's the worst-case output size?**
> In a perfect tree, there are about n/2 leaves with paths of length log n, so O(n log n) values. A chain has one path of length n.

### Follow-up questions

**F1. Path Sum III: paths that start anywhere and go downward (LC 437).**
> Count them with prefix sums along the current root-to-node path plus a HashMap of counts (the [Hash Table M05/M07](../../06-Hash-Table/03-Medium/M07_ContiguousArray.md) family), with backtracking on the map: O(n).

**F2. Return all root-to-leaf paths as strings (LC 257).**
> The same backtracking with a `StringBuilder` and `setLength` to undo.

**F3. Iterative version.**
> A stack of (node, remaining, path) states, or a parent map to rebuild each matching path from its leaf.

---

## 9. Running the file

```bash
java M06_PathSumII.java      # 4/4 tests passed (including 1,000 random trees vs all paths)
```

**One-line summary:** *"DFS with one shared path list: add the node, record a copy at a matching leaf, recurse, then remove the node (backtrack)."*
