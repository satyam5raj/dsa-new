# E09 · Subtree of Another Tree

| | |
|---|---|
| **LeetCode** | [#572 Subtree of Another Tree](https://leetcode.com/problems/subtree-of-another-tree/) |
| **Difficulty** | Easy |
| **Pattern** | "Same Tree" at every node (or: serialize + string search) |
| **Code** | [E09_SubtreeOfAnotherTree.java](E09_SubtreeOfAnotherTree.java) |
| **Run** | `java E09_SubtreeOfAnotherTree.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Is `subRoot` a **subtree** of `root`? A subtree is some node of root **plus all of its descendants**. You can't cut off part of the bottom.

```
     root             subRoot
       3                4
      / \              / \          → true
     4   5            1   2
    / \
   1   2
```

If root had an extra 0 under the 2, the answer would be **false**: the subtree at 4 would then include the 0, and it wouldn't match subRoot exactly.

| root | subRoot | answer |
|---|---|---|
| `[3,4,5,1,2]` | `[4,1,2]` | true |
| `[3,4,5,1,2,null,null,null,null,0]` | `[4,1,2]` | false |
| `[12]` | `[2]` | false |

---

## 2. How to think about it

### Approach 1: Same Tree at every node
"Is subRoot a subtree?" = "does **some** node of root start a tree identical to subRoot?" We already have isSameTree ([E02](E02_SameTree.md)):

```
isSubtree(r, s) = same(r, s) || isSubtree(r.left, s) || isSubtree(r.right, s)
```

O(n · m) in the worst case (n and m are the two sizes), but mismatches usually stop within a node or two.

### Approach 2: Serialize, then search for a substring
Write both trees as strings (preorder **with null markers**), then check whether the subRoot string appears inside the root string. With a linear-time string search (KMP, from [Strings 01-Basics](../../02-Strings/01-Basics/StringMatchingAlgorithms.java)), that's **O(n + m)**.

**Two traps:**
- **The nulls must be written.** Without them, different shapes give the same string.
- **Each value needs a separator in front.** Without one, root `[12]` serializes to `"12,#,#"` and subRoot `[2]` to `"2,#,#"`, which **is** found inside `"12,#,#"`: a false match. With a leading comma, `",12,#,#"` does **not** contain `",2,#,#"`.

---

## 3. Java concepts used in this problem

### 3.1 Recursion that calls another recursion

`isSubtree` walks root; at each node it calls `isSame`, which walks both trees in lockstep. Thinking of `isSame` as a black box (it's already correct from E02) keeps this easy to reason about.

### 3.2 `StringBuilder` passed down the recursion

```java
static void serialize(TreeNode n, StringBuilder sb) {
    if (n == null) { sb.append(",#"); return; }
    sb.append(',').append(n.val);
    ...
```

One `StringBuilder` is shared by every call (it's an object, so all calls refer to the same one), and each call appends to it. That's O(total length) instead of O(n²) for repeated `String` concatenation. (StringBuilder basics: [Strings E01](../../02-Strings/02-Easy/E01_ValidAnagram.md).)

`append` returns the builder itself, which is why calls can be chained: `sb.append(',').append(n.val)`.

### 3.3 `StringBuilder.indexOf(String)`

Returns the first position of the substring, or −1. Java's implementation is a simple search, O(n · m) in the worst case. KMP would guarantee O(n + m).

---

## 4. Approach 1: Same Tree at every node (the file's main solution)

### Code ([E09_SubtreeOfAnotherTree.java:41-50](E09_SubtreeOfAnotherTree.java#L41-L50))

```java
static boolean isSubtree(TreeNode root, TreeNode subRoot) {
    if (root == null) return subRoot == null;
    return isSame(root, subRoot) || isSubtree(root.left, subRoot) || isSubtree(root.right, subRoot);
}

static boolean isSame(TreeNode a, TreeNode b) {
    if (a == null && b == null) return true;
    if (a == null || b == null) return false;
    return a.val == b.val && isSame(a.left, b.left) && isSame(a.right, b.right);
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (root == null) return subRoot == null;` | we've run out of root: only an empty subRoot matches here |
| `isSame(root, subRoot)` | does the tree starting **here** match exactly? |
| `\|\| isSubtree(root.left, subRoot)` | otherwise, look for it in the left subtree |
| `\|\| isSubtree(root.right, subRoot)` | …or the right |
| `isSame` | E02's lockstep comparison |

### Dry run 1: root `[3,4,5,1,2]`, subRoot `[4,1,2]`

| isSubtree at | isSame? | why |
|---|---|---|
| 3 | false | 3 ≠ 4 |
| 4 (left of 3) | **true** | 4 = 4, (1, 1) ✓, (2, 2) ✓, and all the nulls line up |

→ **true** (5 is never visited).

### Dry run 2: the same, but with a 0 under the 2

| isSubtree at | isSame? | why |
|---|---|---|
| 3 | false | 3 ≠ 4 |
| 4 | false | at (2, 2): root's 2 has a left child 0, subRoot's 2 doesn't → (0, null) → false |
| 1, 2, 0 | false | the values differ from 4 |
| 5 | false | |

→ **false** ✓

### Complexity
- **Time O(n · m)** worst case (e.g. many nodes that almost match).
- **Space O(h)** (the recursion of both functions).

---

## 5. Approach 2: Serialize + substring search

### Code ([E09_SubtreeOfAnotherTree.java:53-65](E09_SubtreeOfAnotherTree.java#L53-L65))

```java
static boolean isSubtreeBySerialization(TreeNode root, TreeNode subRoot) {
    StringBuilder a = new StringBuilder(), b = new StringBuilder();
    serialize(root, a);
    serialize(subRoot, b);
    return a.indexOf(b.toString()) >= 0;      // swap in KMP for a guaranteed O(n + m)
}

static void serialize(TreeNode n, StringBuilder sb) {
    if (n == null) { sb.append(",#"); return; }
    sb.append(',').append(n.val);             // the leading comma separates "12" from "2"
    serialize(n.left, sb);
    serialize(n.right, sb);
}
```

### Line by line

| Line | What it does |
|---|---|
| `serialize(root, a)`, `serialize(subRoot, b)` | preorder strings with `#` for null |
| `a.indexOf(b.toString()) >= 0` | does subRoot's string occur inside root's? |
| `sb.append(",#")` | the null marker, so shapes are preserved |
| `sb.append(',').append(n.val)` | a comma **before** each value, so a value can't match the tail of a longer number |

### Why a contiguous match means a real subtree
In preorder, a node's whole subtree (with its nulls) is written as one **contiguous** block that starts with that node. So a match of b inside a, starting at a value boundary (guaranteed by the leading comma), is exactly the serialization of some node's full subtree.

### Dry run: `[3,4,5,1,2]` and `[4,1,2]`

```
a = ,3,4,1,#,#,2,#,#,5,#,#
b =   ,4,1,#,#,2,#,#
        ↑ found at position 2 → true
```

### Dry run: `[12]` and `[2]` (the trap)

```
a = ,12,#,#
b = ,2,#,#       not found (",2" would need a comma right before the 2) → false ✓
```

Without the leading commas: `"12,#,#"` contains `"2,#,#"` → a wrong **true**.

### Complexity
- **Time:** O(n + m) to serialize, plus the search: O(n · m) worst case with `indexOf`, O(n + m) with KMP.
- **Space O(n + m)** for the strings.

---

## 6. Comparing the approaches

| | Same Tree at every node | Serialize + search |
|---|---|---|
| Time | O(n · m) worst | O(n + m) with KMP |
| Space | O(h) | O(n + m) |
| Pitfalls | few | null markers, separators |

The file checks that both agree on 2,000 random pairs (values 0..2, so near-matches are common).

---

## 7. Edge cases and common mistakes

**Edge cases:** subRoot equals root, subRoot is a single leaf, extra nodes below a matching part, repeated values.

**Mistakes:**
1. Matching only the top part of a subtree (the extra-node case).
2. Serializing without null markers or without separators.
3. `if (root == null) return false;` is fine for LeetCode (subRoot is non-empty), but `subRoot == null` is the precise answer.
4. Calling isSubtree instead of isSame for the children inside the matching check (that allows gaps).

---

## 8. Interview questions and answers

**Q1. Why isn't the first approach O(n + m)?**
> At each of the n nodes, isSame can walk up to m nodes before failing, e.g. a tree full of the same value that almost matches every time.

**Q2. Why must each value be preceded by a separator?**
> Otherwise a value can match the tail of a longer number ("2" inside "12"), producing a false match across a value boundary.

### Follow-up questions

**F1. Guarantee O(n + m) without strings.**
> Hash each subtree (Merkle-style: combine the value and the children's hashes) bottom-up, then compare subRoot's hash with every node's hash, verifying on equality.

**F2. Find all the duplicate subtrees (LC 652).**
> Serialize every subtree bottom-up into a HashMap of string → count, and report those seen twice.

**F3. Count the occurrences of subRoot.**
> Count the matches in either approach instead of returning at the first one.

---

## 9. Running the file

```bash
java E09_SubtreeOfAnotherTree.java      # 5/5 tests passed (including 2,000 random pairs: both approaches agree)
```

**One-line summary:** *"Check isSameTree(node, subRoot) at every node of root, or serialize both with null markers and leading separators and search for one string inside the other."*
