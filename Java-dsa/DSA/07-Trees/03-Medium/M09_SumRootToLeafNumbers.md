# M09 · Sum Root to Leaf Numbers

| | |
|---|---|
| **LeetCode** | [#129 Sum Root to Leaf Numbers](https://leetcode.com/problems/sum-root-to-leaf-numbers/) |
| **Difficulty** | Medium |
| **Pattern** | Top-down: build the number digit by digit (`number · 10 + digit`) |
| **Code** | [M09_SumRootToLeafNumbers.java](M09_SumRootToLeafNumbers.java) |
| **Run** | `java M09_SumRootToLeafNumbers.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](../02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Every node holds a digit 0–9. Each root-to-leaf path spells a number. Return the **sum** of all those numbers.

```
        4
       / \
      9   0          paths: 495, 491, 40   →  495 + 491 + 40 = 1026
     / \
    5   1
```

| tree | answer |
|---|---|
| `[1,2,3]` | 25 (12 + 13) |
| `[4,9,0,5,1]` | 1026 |
| `[1,2]` | 12 (not 1 + 12: node 1 isn't a leaf) |
| `[0]` | 0 |

The answer fits in a 32-bit int (the tree depth is at most 10).

---

## 2. How to think about it

### Appending a digit
Appending digit d to the number x gives **x · 10 + d**, the same trick as parsing "495" → ((4)·10 + 9)·10 + 5 from the Basics digit problems.

### Pass the number built so far down the tree (top-down)
```
sum(node, soFar):
    null → 0
    cur = soFar · 10 + node.val
    leaf → cur                         (a complete number)
    else → sum(left, cur) + sum(right, cur)
```

```
4 → cur 4;   9 → 49;   5 (leaf) → 495;   1 (leaf) → 491;   0 (leaf) → 40
total 1026
```

### The leaf check again
As in [Path Sum](../02-Easy/E07_PathSum.md): a null child must contribute **0**, not the partial number. Otherwise `[1,2]` would count "1" (via the empty right side) as well as "12".

---

## 3. Java concepts used in this problem

### 3.1 Building a number from digits: `x * 10 + d`

```
x = 0
x = 0·10 + 4 = 4
x = 4·10 + 9 = 49
x = 49·10 + 5 = 495
```

Each step shifts the existing digits one place left and puts the new digit in the units place.

### 3.2 `int` parameters are per-call copies

`soFar` and `cur` belong to each call, so the left and right subtrees both start from the same prefix without any undo step (compare with the shared list in [M06](M06_PathSumII.md)).

### 3.3 String concatenation in the referee

```java
s += n.val;                    // "4" → "49" → "495"
Integer::parseInt              // "495" → 495
```

`s += n.val` creates a **new** String each time (Strings are immutable), so each call has its own string with no undo needed. It's slower (O(h) per step), which is fine for a referee. `mapToInt(Integer::parseInt).sum()` parses and adds all the path strings.

---

## 4. Approach 1: Collect the path strings (the referee)

### Code ([M09_SumRootToLeafNumbers.java:54-60](M09_SumRootToLeafNumbers.java#L54-L60))

```java
static void paths(TreeNode n, String s, List<String> out) {
    if (n == null) return;
    s += n.val;
    if (n.left == null && n.right == null) out.add(s);
    paths(n.left, s, out);
    paths(n.right, s, out);
}
```

Then `sum = Σ Integer.parseInt(path)`. It's correct but builds O(h)-length strings at every node: O(n · h).

---

## 5. Approach 2: Pass the number down (the file's solution)

### Code ([M09_SumRootToLeafNumbers.java:42-51](M09_SumRootToLeafNumbers.java#L42-L51))

```java
static int sumNumbers(TreeNode root) {
    return sum(root, 0);
}

static int sum(TreeNode n, int soFar) {
    if (n == null) return 0;
    int cur = soFar * 10 + n.val;                         // append this digit
    if (n.left == null && n.right == null) return cur;    // a complete number
    return sum(n.left, cur) + sum(n.right, cur);
}
```

### Line by line

| Line | What it does |
|---|---|
| `sum(root, 0)` | nothing built yet |
| `if (n == null) return 0;` | a missing child contributes nothing |
| `cur = soFar * 10 + n.val` | append this node's digit |
| the leaf check | a full root-to-leaf number |
| `sum(left, cur) + sum(right, cur)` | the numbers below share the prefix `cur` |

### Dry run: `[4, 9, 0, 5, 1]`

| call | soFar | cur | leaf? | returns |
|---|---|---|---|---|
| 4 | 0 | 4 | no | sum(9) + sum(0) |
| 9 | 4 | 49 | no | sum(5) + sum(1) |
| 5 | 49 | 495 | yes | 495 |
| 1 | 49 | 491 | yes | 491 |
| 9 | | | | 986 |
| 0 | 4 | 40 | yes | 40 |
| 4 | | | | **1026** |

### Dry run: `[1, 2]`
1 → cur 1, not a leaf → sum(2, 1) + sum(null, 1) = 12 + **0** = **12** ✓

### Complexity
- **Time O(n).**
- **Space O(h).**

---

## 6. Approach 3: Iterative with a stack of (node, number)

```java
static int sumNumbersIterative(TreeNode root) {
    if (root == null) return 0;
    int total = 0;
    Deque<TreeNode> nodes = new ArrayDeque<>();
    Deque<Integer> nums = new ArrayDeque<>();
    nodes.push(root);
    nums.push(root.val);
    while (!nodes.isEmpty()) {
        TreeNode n = nodes.pop();
        int v = nums.pop();
        if (n.left == null && n.right == null) total += v;
        if (n.right != null) { nodes.push(n.right); nums.push(v * 10 + n.right.val); }
        if (n.left != null)  { nodes.push(n.left);  nums.push(v * 10 + n.left.val); }
    }
    return total;
}
```

Each stack entry carries the number built down to that node. It's the same state that the recursion keeps in its parameters.

---

## 7. Comparing the approaches

| | Path strings | Recursive number | Iterative |
|---|---|---|---|
| Time | O(n · h) | O(n) | O(n) |
| Space | O(n · h) strings | O(h) | O(h) |

The file checks the recursive version against the string referee on 1,000 random trees (at most 9 digits, so every number fits in an int).

---

## 8. Edge cases and common mistakes

**Edge cases:** a single node, zeros (`[0]`, or a 0 at the root giving numbers with a "leading zero"), a chain (one long number).

**Mistakes:**
1. Returning `soFar` for a null child (it counts partial numbers).
2. Adding the digit instead of shifting (`soFar + n.val`).
3. Overflow on deep trees. LeetCode bounds the depth; in general, use `long`.

---

## 9. Interview questions and answers

**Q1. Why pass the number down rather than return it up?**
> Every number starts at the root, so the prefix is known before visiting the children. Passing it down builds each number in O(1) per node.

**Q2. What happens with a leading 0, like the path 0 → 5?**
> It's simply 5: 0 · 10 + 5. The arithmetic handles it naturally (a string would show "05").

### Follow-up questions

**F1. Binary digits (LC 1022, Sum of Root To Leaf Binary Numbers).**
> `cur = soFar * 2 + node.val`, or `(soFar << 1) | node.val`.

**F2. Return the numbers themselves.**
> Add `cur` to a list at each leaf instead of summing.

**F3. The largest root-to-leaf number.**
> Take `max` instead of `+` at each node (with a sentinel for the missing children).

---

## 10. Running the file

```bash
java M09_SumRootToLeafNumbers.java      # 5/5 tests passed (including 1,000 random trees vs string paths)
```

**One-line summary:** *"Pass soFar · 10 + digit down the tree; return it at a leaf, 0 for null, and add the two subtrees' sums."*
