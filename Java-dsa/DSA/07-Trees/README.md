# 07 · Trees

A tree is a hierarchy: one root, each other node has one parent, and there are no cycles. A **binary** tree has at most 2 children per node. A **BST** keeps left < node < right everywhere, so search is O(h). **Balanced** trees (AVL, red-black) keep h = O(log n). **Segment and Fenwick trees** answer range queries on a changing array in O(log n).
In Java, `TreeMap`/`TreeSet` are red-black trees. Interview problems use a hand-written `TreeNode` (included in every file).

Run any file with `java FileName.java`, or every problem with `./run-all.sh`. ⭐ = not in `leetcode.md`, added for a key technique.

## 01-Basics: start here (in this order)

| File | What you learn |
|---|---|
| `BinaryTreeBasics` | vocabulary (depth, height, full/complete/perfect/balanced), **all traversals** recursive and iterative (pre/in/post with a stack, level order with a queue), **Morris** O(1)-space inorder, why deep recursion overflows |
| `TreeRecursionPatterns` | the **5 shapes** of tree problems: bottom-up, top-down, "return one thing, track another", two trees at once, level by level, plus a checklist for writing any recursive tree function |
| `BinarySearchTree` | search/insert/**delete (3 cases)**/min/max/floor/ceiling from scratch, checked against `TreeSet`. Shows sorted inserts → height 4,999 for 5,000 keys |
| `AVLTree` | heights + **rotations** (LL, RR, LR, RL). 1,000,000 sorted inserts → height 19. Checked against `TreeSet` with invariants verified |
| `SegmentTreeAndFenwick` | segment tree (sum, min), **lazy propagation** (range add), **Fenwick / BIT** with the `i & -i` trick, all checked against brute force |

## 02-Easy (11)

| # | Problem | LC | Pattern |
|---|---|---|---|
| E01 | Maximum Depth of Binary Tree | 104 | Bottom-up (or count BFS levels) |
| E02 | Same Tree | 100 | Two trees in lockstep |
| E03 | Invert Binary Tree | 226 | Swap children at every node |
| E04 | Symmetric Tree | 101 | Lockstep, crossed (outer + inner pairs) |
| E05 | Diameter of Binary Tree | 543 | **Return height, track left + right** |
| E06 | Balanced Binary Tree | 110 | Bottom-up with −1 sentinel, O(n) |
| E07 | Path Sum | 112 | Top-down remaining target, check at the **leaf** |
| E08 | Convert Sorted Array to BST | 108 | Middle element = root, recurse |
| E09 | Subtree of Another Tree | 572 | Same Tree at every node / serialize + search |
| E10 | Minimum Depth of Binary Tree | 111 | BFS stops at the first leaf (DFS trap) |
| E11 | Cousins in Binary Tree | 993 | Record depth + parent |

## 03-Medium (16)

| # | Problem | LC | Pattern |
|---|---|---|---|
| M01 | Binary Tree Level Order Traversal | 102 | **BFS with the level-size snapshot** |
| M02 | Binary Tree Right Side View | 199 | Last node per level / DFS right-first |
| M03 | Validate Binary Search Tree | 98 | Pass the allowed **(low, high) range** down |
| M04 | Kth Smallest Element in a BST | 230 | Iterative inorder, stop at k (+ subtree sizes) |
| M05 | Lowest Common Ancestor | 236 | Bottom-up "what did you find?" (+ BST version) |
| M06 | Path Sum II | 113 | DFS + **backtracking** (add, recurse, remove) |
| M07 | Construct from Preorder + Inorder | 105 | Preorder gives the root, inorder splits; index map |
| M08 | Count Good Nodes | 1448 | Top-down max so far |
| M09 | Sum Root to Leaf Numbers | 129 | Top-down `num * 10 + digit` |
| M10 | Zigzag Level Order | 103 | BFS + deque addFirst/addLast |
| M11 | Flatten Binary Tree to Linked List | 114 | Rewire in place, O(1) space |
| M12 | Populating Next Right Pointers | 116 | Use the linked level to wire the next, O(1) space |
| M13 | House Robber III | 337 | **Tree DP**: return {rob, skip} |
| M14 | All Nodes Distance K | 863 | Add parent links → **BFS on a graph** |
| M15 | Recover Binary Search Tree | 99 | Inorder drops → swap back (+ Morris) |
| M16 | ⭐ Range Sum Query – Mutable | 307 | **Fenwick** / iterative segment tree |

## 04-Hard (5)

| # | Problem | LC | Pattern |
|---|---|---|---|
| H01 | Serialize and Deserialize Binary Tree | 297 | Preorder **with null markers** (+ BFS format) |
| H02 | Binary Tree Maximum Path Sum | 124 | Diameter with values: `max(0, arm)`, track the bend |
| H03 | Vertical Order Traversal | 987 | Coordinates + 3-key sort |
| H04 | Binary Tree Cameras | 968 | **Greedy bottom-up, 3 states** |
| H05 | ⭐ Count of Smaller Numbers After Self | 315 | Fenwick over ranks (coordinate compression) |

All 30 tree problems from `leetcode.md` (151–180) are here. Trie (prefix tree) → **10-Trie**. Heaps (complete trees in an array) → **08-Heap**.

## Which pattern? A cheat sheet

| The question sounds like… | Reach for |
|---|---|
| height, size, balanced, diameter, max path | **bottom-up** recursion (postorder) |
| something about the path from the root (sum so far, max so far, range) | **top-down**: pass it as a parameter |
| the answer is a path that bends at some node | return one arm, **track the best "V" in a field** (E05, H02) |
| per level, leftmost/rightmost, shortest to a leaf | **BFS** with `size = q.size()` |
| anything about BST order / k-th / validate / recover | **inorder** = sorted |
| choose nodes with constraints between neighbours | **tree DP**: return a small array of states (M13, H04) |
| distance in any direction from a node | add parent pointers → **graph BFS** |
| changing array + range sum/min | **Fenwick** (sums) / **segment tree** (anything, + lazy for range updates) |

## Interview questions

1. Preorder / inorder / postorder: what is each good for? Which one gives sorted output on a BST?
2. Delete a node with two children from a BST: why use the inorder successor?
3. Why can a BST degrade to O(n), and how do AVL and red-black trees prevent it? AVL vs red-black trade-off?
4. Which traversal pairs determine a unique tree? (inorder + pre/post: yes. pre + post: no.)
5. Recursive DFS on a 10⁵-deep tree: what happens, and how do you fix it? (iterative stack, Morris)
6. Segment tree vs Fenwick tree: when can't you use Fenwick? (non-invertible ops like min/max, range updates)
7. How does `TreeMap` give you floor/ceiling in O(log n), and when would you pick it over `HashMap`?
