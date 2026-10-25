# H01 · Serialize and Deserialize Binary Tree

| | |
|---|---|
| **LeetCode** | [#297 Serialize and Deserialize Binary Tree](https://leetcode.com/problems/serialize-and-deserialize-binary-tree/) |
| **Difficulty** | Hard |
| **Pattern** | Preorder with null markers: one traversal fully describes a tree |
| **Code** | [H01_SerializeDeserializeBinaryTree.java](H01_SerializeDeserializeBinaryTree.java) |
| **Run** | `java H01_SerializeDeserializeBinaryTree.java` |

New to trees? `TreeNode`, the vocabulary, the call stack and the file helpers are explained in [E01](../02-Easy/E01_MaximumDepthOfBinaryTree.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Design `serialize(root) → String` and `deserialize(String) → tree` so that the round trip gives back an **identical** tree. Any format is allowed.

```
        1
       / \          preorder with nulls:  1,2,#,#,3,4,#,#,5,#,#
      2   3         level order (LeetCode): 1,2,3,#,#,4,5,#,#,#,#
         / \
        4   5
```

---

## 2. How to think about it

### Why one plain traversal isn't enough
The preorder `1 2` could mean "2 is the left child" or "2 is the right child". That's why [M07](../03-Medium/M07_ConstructFromPreorderInorder.md) needed preorder **and** inorder (and unique values).

### The fix: write the nulls too
With an explicit marker for every missing child, **preorder alone is unambiguous**, even with duplicate values.

### Serialize (preorder)
```
null → "#"
else → val, serialize(left), serialize(right)
```

### Deserialize: read the tokens in the same order
```
build():
    token = next()
    "#" → return null
    node = new TreeNode(token)
    node.left  = build()      ← consumes exactly the left subtree's tokens
    node.right = build()      ← then the right subtree's
    return node
```
The null markers tell the recursion exactly where each subtree ends.

### BFS version (LeetCode's own format)
Serialize level by level, writing the nulls. Deserialize with a **queue of parents**: the next two tokens are always the current parent's left and right children.

### Interview talking points
- **Separators** matter (multi-digit and negative values).
- A tree with n nodes has **n + 1** null children, so the output is O(n).
- The recursive version can overflow the stack on a 10⁵-deep chain; the BFS version can't.

---

## 3. Java concepts used in this problem

### 3.1 `String.split(",")` and its trailing-empty rule

```java
"1,2,#,".split(",")   →  ["1", "2", "#"]      // trailing empty strings are DROPPED
```

That's why the BFS serializer can leave a trailing comma. The DFS serializer trims it explicitly with `sb.substring(0, sb.length() - 1)`.

### 3.2 A `Deque` of tokens as a shared cursor

```java
Deque<String> tokens = new ArrayDeque<>(Arrays.asList(data.split(",")));
String t = tokens.poll();         // consume the next token
```

Every recursive call polls from the **same** deque, so the tokens are consumed in order without passing an index around. (An `int[] pos = {0}` or a static index works too, as in M07.)

`Arrays.asList(array)` wraps the array as a List, and the `ArrayDeque` constructor copies it.

### 3.3 `Integer.parseInt` handles negatives

`Integer.parseInt("-10")` → −10, and `"2147483647"` → `Integer.MAX_VALUE`. Because each value is a separate token, multi-digit and negative values are no problem.

### 3.4 `String.equals` for the marker

`t.equals("#")`, never `t == "#"`: the tokens are new String objects created by `split`, so `==` would compare references and fail.

---

## 4. Approach 1: Preorder with null markers (the file's main solution)

### Code ([H01_SerializeDeserializeBinaryTree.java:52-77](H01_SerializeDeserializeBinaryTree.java#L52-L77))

```java
static String serialize(TreeNode root) {
    StringBuilder sb = new StringBuilder();
    write(root, sb);
    return sb.substring(0, sb.length() - 1);                 // drop the trailing comma
}

static void write(TreeNode n, StringBuilder sb) {
    if (n == null) { sb.append("#,"); return; }
    sb.append(n.val).append(',');
    write(n.left, sb);
    write(n.right, sb);
}

static TreeNode deserialize(String data) {
    Deque<String> tokens = new ArrayDeque<>(Arrays.asList(data.split(",")));
    return read(tokens);
}

static TreeNode read(Deque<String> tokens) {
    String t = tokens.poll();
    if (t.equals("#")) return null;
    TreeNode n = new TreeNode(Integer.parseInt(t));
    n.left = read(tokens);                                     // eats the whole left subtree
    n.right = read(tokens);
    return n;
}
```

### Line by line

| Line | What it does |
|---|---|
| `write`: null → `"#,"` | record the missing child |
| `sb.append(n.val).append(',')` | the value, then a separator |
| `write(left); write(right);` | preorder |
| `substring(0, length − 1)` | remove the final comma |
| `data.split(",")` | the tokens, in preorder |
| `read`: `"#"` → null | an empty subtree ends here |
| `new TreeNode(parseInt(t))` | this subtree's root |
| `n.left = read(tokens)` | consumes the left subtree's tokens, exactly |
| `n.right = read(tokens)` | then the right subtree's |

### Dry run: deserialize `"1,2,#,#,3,4,#,#,5,#,#"`

| read call | token | result |
|---|---|---|
| root | 1 | node 1, read its left |
| 1.left | 2 | node 2, read its left |
| 2.left | # | null |
| 2.right | # | null → node 2 is done |
| 1.right | 3 | node 3, read its left |
| 3.left | 4 | node 4 |
| 4.left, 4.right | #, # | null, null |
| 3.right | 5 | node 5 |
| 5.left, 5.right | #, # | null, null |

All 11 tokens are consumed and the tree is `[1, 2, 3, null, null, 4, 5]` ✓

### Dry run: the empty tree
serialize(null) → `"#,"` → trimmed to `"#"`. deserialize("#") → the first token is `#` → **null** ✓

### Complexity
- **Time O(n)** each way.
- **Space O(n)** for the string; O(h) recursion.

---

## 5. Approach 2: Level order (LeetCode's format)

### Code ([H01_SerializeDeserializeBinaryTree.java:80-107](H01_SerializeDeserializeBinaryTree.java#L80-L107))

```java
static String serializeBfs(TreeNode root) {
    if (root == null) return "";
    StringBuilder sb = new StringBuilder();
    Queue<TreeNode> q = new LinkedList<>();                    // allows nulls
    q.add(root);
    while (!q.isEmpty()) {
        TreeNode n = q.poll();
        if (n == null) { sb.append("#,"); continue; }
        sb.append(n.val).append(',');
        q.add(n.left);
        q.add(n.right);
    }
    return sb.toString();
}

static TreeNode deserializeBfs(String data) {
    if (data.isEmpty()) return null;
    String[] t = data.split(",");
    TreeNode root = new TreeNode(Integer.parseInt(t[0]));
    Queue<TreeNode> parents = new ArrayDeque<>();
    parents.add(root);
    for (int i = 1; i < t.length; i += 2) {
        TreeNode p = parents.poll();                           // the next two tokens are p's children
        if (!t[i].equals("#")) { p.left = new TreeNode(Integer.parseInt(t[i])); parents.add(p.left); }
        if (i + 1 < t.length && !t[i + 1].equals("#")) { p.right = new TreeNode(Integer.parseInt(t[i + 1])); parents.add(p.right); }
    }
    return root;
}
```

### Line by line

| Line | What it does |
|---|---|
| `LinkedList` queue | it must hold nulls, so they can be written as `#` |
| `if (n == null) { … continue; }` | write the marker; a null has no children to queue |
| `t[0]` | the root |
| the `for` with `i += 2` | each real node, in BFS order, owns the next two tokens |
| `parents.add(...)` | only real children become future parents |
| `i + 1 < t.length` | the trailing nulls may have been dropped by `split` |

### Dry run: deserialize `"1,2,3,#,#,4,5,#,#,#,#,"`

After `split`: `[1, 2, 3, #, #, 4, 5, #, #, #, #]`

| i | parent | tokens | children |
|---|---|---|---|
| 1 | 1 | 2, 3 | left 2, right 3 |
| 3 | 2 | #, # | none |
| 5 | 3 | 4, 5 | left 4, right 5 |
| 7 | 4 | #, # | none |
| 9 | 5 | #, # | none |

→ `[1, 2, 3, null, null, 4, 5]` ✓

### Complexity
- **Time O(n)** each way.
- **Space O(n)**; no recursion, so there's no risk of a stack overflow.

---

## 6. Comparing the approaches

| | Preorder + nulls | Level order + nulls |
|---|---|---|
| Time | O(n) | O(n) |
| Recursion | O(h) (risky on deep chains) | none |
| Code size | shorter | a bit longer |
| Format | `1,2,#,#,3,4,#,#,5,#,#` | `1,2,3,#,#,4,5,#,#,#,#` |

The file round-trips 1,000 random trees (values −1000..1000) through both codecs.

---

## 7. Edge cases and common mistakes

**Edge cases:** the empty tree, a single node, negative and multi-digit values, `Integer.MAX_VALUE`, deep chains.

**Mistakes:**
1. Omitting the null markers (ambiguous).
2. No separators ("12" vs "1","2").
3. Comparing tokens with `==`.
4. In BFS deserialize, reading `t[i + 1]` without checking the length.
5. Using `ArrayDeque` to queue null children while serializing (it throws).

---

## 8. Interview questions and answers

**Q1. Why is preorder with nulls enough on its own?**
> The null markers say exactly where each subtree ends, so the recursive reader knows when to stop reading the left subtree and start the right one. Nothing is ambiguous, even with duplicate values.

**Q2. How big is the output?**
> n values plus n + 1 null markers: O(n) tokens.

**Q3. How would you make it more compact?**
> Binary encoding (fixed 4-byte ints and a bitmap of which children exist), or omitting the trailing nulls of the level order. For a BST, preorder **without** nulls suffices (F1).

### Follow-up questions

**F1. Serialize a BST (LC 449).**
> Preorder values only: rebuild with value bounds (each value goes left if it's below the current bound), O(n).

**F2. An N-ary tree (LC 428).**
> Write each node's value followed by its child count, or wrap the children in brackets.

**F3. Find duplicate subtrees (LC 652).**
> Serialize every subtree bottom-up and count the strings in a HashMap.

---

## 9. Running the file

```bash
java H01_SerializeDeserializeBinaryTree.java      # 8/8 tests passed (including 1,000 random trees, both codecs)
```

**One-line summary:** *"Write the preorder with '#' for every null; read it back in the same order, with each recursive call consuming exactly one subtree's tokens."*
