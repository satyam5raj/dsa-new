# M14 · All Nodes Distance K in Binary Tree

| | |
|---|---|
| **LeetCode** | [#863 All Nodes Distance K in Binary Tree](https://leetcode.com/problems/all-nodes-distance-k-in-binary-tree/) |
| **Difficulty** | Medium |
| **Pattern** | Tree → graph (add parent links), then BFS from the target |
| **Code** | [M14_AllNodesDistanceK.java](M14_AllNodesDistanceK.java) |
| **Run** | `java M14_AllNodesDistanceK.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](../02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Return the values of all nodes **exactly k edges** away from `target` (in any order).

```
            3
          /   \
         5     1          target 5, k = 2 → [7, 4, 1]
        / \   / \         (7 and 4 are 2 steps DOWN; 1 is up to 3, then down)
       6   2 0   8
          / \
         7   4
```

| target | k | answer |
|---|---|---|
| 5 | 2 | 7, 4, 1 |
| 5 | 0 | 5 |
| 7 | 3 | 6, 3 (up 3 levels to 3, or up 2 to 5 and down to 6) |
| 5 | 10 | [] |

---

## 2. How to think about it

### The difficulty
Tree pointers only go **down**, but distance k may require going **up** first.

### A tree is just an undirected graph
Give every node a link to its **parent** (a HashMap node → parent, built with one DFS). Now each node has up to 3 neighbours: left, right and parent. Then it's plain **BFS**:
- start at the target
- expand level by level, k times, with a **visited** set (otherwise you'd go down to a child and straight back up to where you came from)
- whatever is in the queue after k levels is the answer

```
level 0: {5}
level 1: 6, 2, 3 (the parent)
level 2: from 6: none;  from 2: 7, 4;  from 3: 1 (5 is already visited)   → [7, 4, 1]
```

The same "convert to a graph, then BFS" move solves: Amount of Time for a Binary Tree to Be Infected (LC 2385) and Closest Leaf (LC 742). Graph BFS is covered fully in [09-Graph](../../09-Graph/README.md).

---

## 3. Java concepts used in this problem

### 3.1 A parent map

```java
Map<TreeNode, TreeNode> parent = new HashMap<>();
parent.put(n, p);                 // during a DFS that passes p down
parent.get(n)                     // the parent, or null for the root
```

Node identity is the key (as in [M05](M05_LowestCommonAncestor.md)). The root maps to `null`, which the BFS simply skips.

### 3.2 Iterating over a small array literal

```java
for (TreeNode next : new TreeNode[]{n.left, n.right, parent.get(n)})
```

This creates a 3-element array on the spot and loops over it, which is a compact way to treat the three neighbours uniformly. Nulls are filtered inside the loop.

### 3.3 `visited.add(next)` as "check and mark"

```java
if (next != null && visited.add(next)) q.add(next);
```

`HashSet.add` returns false if the element was already there, so one call both checks and marks (the same idiom as [Hash Table E01](../../06-Hash-Table/02-Easy/E01_HappyNumber.md)).

### 3.4 Iterating a queue without removing

```java
for (TreeNode n : q) result.add(n.val);
```

A for-each over a `Queue` walks its elements front to back without polling them.

---

## 4. Approach 1: Distance through the LCA (the referee)

### Code ([M14_AllNodesDistanceK.java:73-91](M14_AllNodesDistanceK.java#L73-L91))

```java
static List<Integer> brute(TreeNode root, TreeNode target, int k) {
    Map<TreeNode, TreeNode> parent = new HashMap<>();
    linkParents(root, null, parent);
    List<TreeNode> targetPath = pathUp(target, parent);
    List<Integer> out = new ArrayList<>();
    for (TreeNode n : parent.keySet()) {
        List<TreeNode> p = pathUp(n, parent);
        Set<TreeNode> onP = new HashSet<>(p);
        int up = 0;
        for (TreeNode a : targetPath) { if (onP.contains(a)) { if (up + p.indexOf(a) == k) out.add(n.val); break; } up++; }
    }
    return out;
}

static List<TreeNode> pathUp(TreeNode n, Map<TreeNode, TreeNode> parent) {
    List<TreeNode> p = new ArrayList<>();
    for (; n != null; n = parent.get(n)) p.add(n);
    return p;
}
```

| Line | What it does |
|---|---|
| `pathUp(target)` | target, its parent, …, the root |
| for every node n | its own path up to the root |
| the inner loop | walk up from the target until reaching a node on n's path: that's their LCA; `up` steps from the target plus `p.indexOf(a)` steps from n |
| `== k` | n is at distance k |

The distance between two tree nodes is the number of steps up to their LCA from each side. **O(n · h)** to O(n²), so it's only a referee.

---

## 5. Approach 2: Parent links + BFS (the file's solution)

### Code ([M14_AllNodesDistanceK.java:44-70](M14_AllNodesDistanceK.java#L44-L70))

```java
static List<Integer> distanceK(TreeNode root, TreeNode target, int k) {
    Map<TreeNode, TreeNode> parent = new HashMap<>();
    linkParents(root, null, parent);

    Queue<TreeNode> q = new ArrayDeque<>();
    Set<TreeNode> visited = new HashSet<>();
    q.add(target);
    visited.add(target);
    for (int dist = 0; dist < k && !q.isEmpty(); dist++) {       // expand k levels
        for (int i = q.size(); i > 0; i--) {
            TreeNode n = q.poll();
            for (TreeNode next : new TreeNode[]{n.left, n.right, parent.get(n)}) {
                if (next != null && visited.add(next)) q.add(next);
            }
        }
    }
    List<Integer> result = new ArrayList<>();
    for (TreeNode n : q) result.add(n.val);
    return result;
}

static void linkParents(TreeNode n, TreeNode p, Map<TreeNode, TreeNode> parent) {
    if (n == null) return;
    parent.put(n, p);
    linkParents(n.left, n, parent);
    linkParents(n.right, n, parent);
}
```

### Line by line

| Line | What it does |
|---|---|
| `linkParents(root, null, parent)` | one DFS records every node's parent |
| `q.add(target); visited.add(target);` | distance 0 |
| `dist < k && !q.isEmpty()` | expand exactly k levels (or stop early if the tree runs out) |
| the inner `for` over `q.size()` | one BFS level = one more unit of distance |
| the three neighbours | down-left, down-right, up |
| `visited.add(next)` | never revisit (prevents going back the way you came) |
| the final loop | the queue now holds exactly the nodes at distance k |

### Dry run: target 7, k = 3

| dist | queue at the start | expansion | visited after |
|---|---|---|---|
| 0 | [7] | 7 → parent 2 | {7, 2} |
| 1 | [2] | 2 → 4, parent 5 | {7, 2, 4, 5} |
| 2 | [4, 5] | 4 → none new; 5 → 6, parent 3 | + 6, 3 |
| 3 | [6, 3] | (the loop stops: dist = 3 = k) | |

→ **[6, 3]** ✓

### Complexity
- **Time O(n):** the DFS plus a BFS that visits each node at most once.
- **Space O(n)** for the parent map, visited set and queue.

---

## 6. Approach 3: Without a parent map (DFS that returns the distance to the target)

```java
static List<Integer> distanceKNoMap(TreeNode root, TreeNode target, int k) {
    List<Integer> out = new ArrayList<>();
    find(root, target, k, out);
    return out;
}

// returns the distance from n down to target, or -1 if target isn't in n's subtree
static int find(TreeNode n, TreeNode target, int k, List<Integer> out) {
    if (n == null) return -1;
    if (n == target) { collectDown(n, k, out); return 0; }
    int l = find(n.left, target, k, out);
    if (l >= 0) {
        if (l + 1 == k) out.add(n.val);
        else collectDown(n.right, k - l - 2, out);      // go over to the other side
        return l + 1;
    }
    int r = find(n.right, target, k, out);
    if (r >= 0) {
        if (r + 1 == k) out.add(n.val);
        else collectDown(n.left, k - r - 2, out);
        return r + 1;
    }
    return -1;
}

static void collectDown(TreeNode n, int d, List<Integer> out) {
    if (n == null || d < 0) return;
    if (d == 0) { out.add(n.val); return; }
    collectDown(n.left, d - 1, out);
    collectDown(n.right, d - 1, out);
}
```

Each ancestor of the target at distance `d` either is the answer (`d == k`) or sends the search down its **other** child with `k − d − 1` steps left. It uses O(h) space instead of O(n).

---

## 7. Comparing the approaches

| | LCA distance (referee) | Parent map + BFS | DFS distance (no map) |
|---|---|---|---|
| Time | O(n · h) | O(n) | O(n) |
| Space | O(n) | O(n) | **O(h)** |
| Ease | | easiest to explain | trickiest |

The file checks the BFS against the referee on 300 random trees (with unique labels).

---

## 8. Edge cases and common mistakes

**Edge cases:** k = 0 (just the target), k larger than any distance (empty), the target is the root, the target is a leaf.

**Mistakes:**
1. Only searching downward from the target.
2. No visited set (bouncing between a node and its parent).
3. Expanding k + 1 levels (an off-by-one), or collecting the nodes while expanding instead of after.

---

## 9. Interview questions and answers

**Q1. Why convert the tree into a graph?**
> Distance in a tree can go up then down. Parent links make every edge traversable in both directions, and then BFS gives distances directly.

**Q2. Can you do it in O(h) extra space?**
> Yes: a DFS that returns each ancestor's distance to the target, and at each ancestor searches down the other child for the remaining distance (approach 3).

### Follow-up questions

**F1. The time for an infection to spread through the tree (LC 2385).**
> BFS from the start node over the parent-linked graph; the answer is the number of levels − 1.

**F2. The closest leaf to a target (LC 742).**
> BFS from the target over the parent-linked graph; the first leaf polled is the closest.

**F3. Distance between two given nodes.**
> depth(a) + depth(b) − 2 · depth(LCA(a, b)) ([M05](M05_LowestCommonAncestor.md)).

---

## 10. Running the file

```bash
java M14_AllNodesDistanceK.java      # 5/5 tests passed (including 300 random trees vs the LCA distance)
```

**One-line summary:** *"Record each node's parent so the tree becomes an undirected graph, then BFS k levels from the target with a visited set."*
