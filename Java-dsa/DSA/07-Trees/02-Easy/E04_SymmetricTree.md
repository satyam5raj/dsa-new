# E04 · Symmetric Tree

| | |
|---|---|
| **LeetCode** | [#101 Symmetric Tree](https://leetcode.com/problems/symmetric-tree/) |
| **Difficulty** | Easy |
| **Pattern** | Two trees in lockstep: the left and right subtrees, crossed over |
| **Code** | [E04_SymmetricTree.java](E04_SymmetricTree.java) |
| **Run** | `java E04_SymmetricTree.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Is the tree a **mirror image of itself** around its centre?

```
       1                        1
     /   \                    /   \
    2     2      true        2     2      false
   / \   / \                  \     \
  3   4 4   3                  3     3
```

| tree | answer |
|---|---|
| `[1,2,2,3,4,4,3]` | true |
| `[1,2,2,null,3,null,3]` | false |
| `[1]` | true |
| `[1,2,3]` | false (the values differ) |

---

## 2. How to think about it

**Insight:** the tree is symmetric ⇔ `root.left` is a **mirror** of `root.right`.

Two subtrees a and b are mirrors when:
- `a.val == b.val`
- `a.left` mirrors `b.right` (the **outer** pair)
- `a.right` mirrors `b.left` (the **inner** pair)

```
        a        b
       / \      / \
      x   y    y'  x'       x ↔ x' (outer),  y ↔ y' (inner)
```

It's exactly [Same Tree](E02_SameTree.md) with the children compared **crosswise**. The same three base cases apply: both null → true, one null → false, values differ → false.

### Iterative
Push **pairs** into a queue: (a.left, b.right) and (a.right, b.left), and check each pair as it comes out.

---

## 3. Java concepts used in this problem

### 3.1 A helper with a different signature

```
static boolean isSymmetric(TreeNode root)        // what the problem asks
static boolean mirror(TreeNode a, TreeNode b)    // what the recursion needs
```

The question is about one tree, but the recursion naturally works on **pairs**. A helper with extra parameters is a very common pattern in tree problems.

### 3.2 `root == null || mirror(...)`

`||` short-circuits: an empty tree is symmetric, and `mirror` is only called when root isn't null (so `root.left` is safe).

### 3.3 A queue that holds nulls on purpose

```java
Queue<TreeNode> q = new LinkedList<>();          // LinkedList: we enqueue nulls on purpose
```

Missing children are part of the comparison, so the iterative version adds them to the queue. `ArrayDeque` throws on `add(null)`; `LinkedList` allows it.

### 3.4 Polling two at a time

```java
TreeNode a = q.poll(), b = q.poll();
```

The queue always holds an even number of elements, added in pairs, so two polls always give one matching pair.

---

## 4. Approach 1: Compare with a mirrored copy (the test referee)

### Code ([E04_SymmetricTree.java:84-90](E04_SymmetricTree.java#L84-L90))

```java
static boolean equalsItsMirror(TreeNode r) {
    return String.valueOf(r).equals(String.valueOf(mirrorCopy(r)));
}

static TreeNode mirrorCopy(TreeNode n) {
    return n == null ? null : new TreeNode(n.val, mirrorCopy(n.right), mirrorCopy(n.left));
}
```

Build the inverted copy ([E03](E03_InvertBinaryTree.md)) and compare the serializations. It's correct by definition, but uses O(n) extra memory, so the file uses it only as a referee.

---

## 5. Approach 2: Recursive mirror check (the file's solution)

### Code ([E04_SymmetricTree.java:37-45](E04_SymmetricTree.java#L37-L45))

```java
static boolean isSymmetric(TreeNode root) {
    return root == null || mirror(root.left, root.right);
}

static boolean mirror(TreeNode a, TreeNode b) {
    if (a == null && b == null) return true;
    if (a == null || b == null) return false;
    return a.val == b.val && mirror(a.left, b.right) && mirror(a.right, b.left);
}
```

### Line by line

| Line | What it does |
|---|---|
| `root == null \|\| mirror(root.left, root.right)` | an empty tree is symmetric; otherwise compare the two halves |
| `a == null && b == null` | two empty subtrees mirror each other |
| `a == null \|\| b == null` | one side is missing → not a mirror |
| `a.val == b.val` | the mirrored nodes must hold equal values |
| `mirror(a.left, b.right)` | the outer pair |
| `mirror(a.right, b.left)` | the inner pair |

### Dry run 1: `[1, 2, 2, 3, 4, 4, 3]`

| call | values | result |
|---|---|---|
| mirror(2ᴸ, 2ᴿ) | 2 = 2 | check the outer and inner pairs |
| mirror(3, 3) (outer) | 3 = 3 | both have null children → true |
| mirror(4, 4) (inner) | 4 = 4 | true |
| mirror(2ᴸ, 2ᴿ) | | **true** |

### Dry run 2: `[1, 2, 2, null, 3, null, 3]`

```
    2ᴸ         2ᴿ
      \          \
       3          3
```

| call | result |
|---|---|
| mirror(2ᴸ, 2ᴿ) | 2 = 2 → the outer pair: (2ᴸ.left, 2ᴿ.right) = (null, 3) |
| mirror(null, 3) | one null → **false** |

The structure is a copy, not a mirror, so → **false** ✓

### Complexity
- **Time O(n).**
- **Space O(h).**

---

## 6. Approach 3: Iterative with a queue of pairs

### Code ([E04_SymmetricTree.java:47-60](E04_SymmetricTree.java#L47-L60))

```java
static boolean isSymmetricIterative(TreeNode root) {
    if (root == null) return true;
    Queue<TreeNode> q = new LinkedList<>();          // LinkedList: we enqueue nulls on purpose
    q.add(root.left);
    q.add(root.right);
    while (!q.isEmpty()) {
        TreeNode a = q.poll(), b = q.poll();         // always taken as a pair
        if (a == null && b == null) continue;
        if (a == null || b == null || a.val != b.val) return false;
        q.add(a.left);  q.add(b.right);              // outer pair
        q.add(a.right); q.add(b.left);               // inner pair
    }
    return true;
}
```

### Line by line

| Line | What it does |
|---|---|
| `q.add(root.left); q.add(root.right);` | the first pair to compare |
| `a = q.poll(), b = q.poll()` | take one pair |
| `if (a == null && b == null) continue;` | a matching empty pair: nothing to expand |
| `if (a == null \|\| b == null \|\| a.val != b.val)` | a mismatch |
| the two pairs of `add`s | queue the outer pair, then the inner pair |

### Dry run: `[1, 2, 2, null, 3, null, 3]`

| step | pair polled | check | pairs added | queue after |
|---|---|---|---|---|
| 1 | (2ᴸ, 2ᴿ) | 2 = 2 ✓ | (null, 3), (3, null) | [null, 3, 3, null] |
| 2 | (null, 3) | one null → **false** | | |

### Complexity
- **Time O(n).**
- **Space O(w).**

---

## 7. Comparing the approaches

| | Mirror copy | Recursive | Iterative |
|---|---|---|---|
| Time | O(n) | O(n) | O(n) |
| Extra space | O(n) | O(h) | O(w) |
| Early exit | no | yes | yes |

The file checks all three on 2,000 random trees with values 0/1 (so many of them are symmetric).

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty tree, a single node, a perfect tree with mirrored values, the same shape but different values.

**Mistakes:**
1. Comparing left with left (that tests "same tree", not "mirror").
2. Checking that root.left and root.right are **each** symmetric (that's not the condition).
3. Comparing the inorder traversal with its reverse. It fails on some trees, e.g. `[1,2,2,2,null,2]`.
4. Using `ArrayDeque` with null elements.

---

## 9. Interview questions and answers

**Q1. How is this different from Same Tree?**
> The base cases are identical. The recursion pairs a.left with b.right and a.right with b.left, instead of matching sides.

**Q2. Why is comparing traversals unreliable?**
> Without null markers, different shapes can produce the same sequence, so a palindromic inorder doesn't imply symmetry.

### Follow-up questions

**F1. Make the tree symmetric with the fewest value changes.**
> Walk the mirrored pairs; each pair with different values costs one change. Shape differences can't be fixed by value changes.

**F2. Check symmetry for an N-ary tree.**
> At each mirrored pair, children[i] must mirror children[k − 1 − i].

**F3. Check each level of the tree for palindromic values (with null markers).**
> BFS by levels, recording the nulls, and check that each level's list is a palindrome. That's equivalent to symmetry.

---

## 10. Running the file

```bash
java E04_SymmetricTree.java      # 6/6 tests passed (including 2,000 random trees: rec == iter == (tree == its mirror))
```

**One-line summary:** *"Symmetric ⇔ mirror(root.left, root.right): equal values, outer pair (a.left, b.right) and inner pair (a.right, b.left) both mirror."*
