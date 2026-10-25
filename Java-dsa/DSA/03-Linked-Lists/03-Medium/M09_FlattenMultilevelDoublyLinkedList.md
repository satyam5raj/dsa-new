# M09 · Flatten a Multilevel Doubly Linked List

| | |
|---|---|
| **LeetCode** | [#430 Flatten a Multilevel Doubly Linked List](https://leetcode.com/problems/flatten-a-multilevel-doubly-linked-list/) |
| **Difficulty** | Medium |
| **Pattern** | Depth-first splicing (stack or recursion): like a tree's pre-order |
| **Code** | [M09_FlattenMultilevelDoublyLinkedList.java](M09_FlattenMultilevelDoublyLinkedList.java) |
| **Run** | `java M09_FlattenMultilevelDoublyLinkedList.java` |

---

## 1. The problem

Nodes have `prev`, `next` **and** `child` (pointing to another doubly linked list, which may have children of its own). Flatten everything into **one level**: a child list goes **right after its parent**, before the parent's old next. All `child` pointers must end up `null`, and the `prev` pointers must be correct.

```
1 - 2 - 3 - 4 - 5 - 6
        |
        7 - 8 - 9 - 10
            |
            11 - 12

→  1 2 3 7 8 11 12 9 10 4 5 6
```

**Constraints:** up to 1000 nodes.

---

## 2. How to think about it

Turn the picture 45°: `next` is like a "right sibling" and `child` is like a "first child". The flattened order is a **pre-order traversal**: visit a node, then its child branch **completely**, then its next.

"Finish the deeper thing first, then come back to what you left" calls for a **stack** (or recursion, which uses the call stack).

```
cur = head
while cur:
    if cur.child:
        if cur.next: push(cur.next)          // come back to it later
        splice: cur.next = child, child.prev = cur, cur.child = null
    if cur.next == null and the stack isn't empty:
        resume = pop(); cur.next = resume; resume.prev = cur
    cur = cur.next
```

---

## 3. Java concepts used in this problem

### 3.1 A doubly linked node

```java
static class Node {
    int val;
    Node prev, next, child;
}
```

With two links (`prev` and `next`), **every** splice must update **both directions**: `a.next = b` **and** `b.prev = a`. Forgetting one side is the classic doubly-linked-list bug. The file's `consistent()` test checks that `c.next.prev == c` for every node.

### 3.2 `Deque` as a stack

```java
Deque<Node> stack = new ArrayDeque<>();
stack.push(x);      // add on top
stack.pop();        // remove the top (last in, first out)
```

### 3.3 Iterative versus recursive DFS

Recursion is a hidden stack. An explicit stack does the same job without risking a `StackOverflowError` on very deep nesting, and it's usually what interviewers expect for "iterative" solutions.

---

## 4. Approach 1: Recursive (return the tail of the flattened part)

```java
static Node flattenRec(Node head) {
    flattenTail(head);
    return head;
}

// flattens the list starting at 'node' and returns its LAST node
static Node flattenTail(Node node) {
    Node cur = node, last = null;
    while (cur != null) {
        Node next = cur.next;
        if (cur.child != null) {
            Node childTail = flattenTail(cur.child);   // flatten the child list first
            cur.next = cur.child;  cur.child.prev = cur;
            childTail.next = next;
            if (next != null) next.prev = childTail;
            cur.child = null;
            last = childTail;
        } else {
            last = cur;
        }
        cur = next;
    }
    return last;
}
```

The recursion returns the **tail** of each flattened child, so we can connect it to the parent's old next in O(1). **O(n) time, O(depth) stack.**

---

## 5. Approach 2: An explicit stack (the file's solution)

### Code ([M09_FlattenMultilevelDoublyLinkedList.java:52-70](M09_FlattenMultilevelDoublyLinkedList.java#L52-L70))

```java
static Node flatten(Node head) {
    Deque<Node> stack = new ArrayDeque<>();
    Node cur = head;
    while (cur != null) {
        if (cur.child != null) {
            if (cur.next != null) stack.push(cur.next);   // remember where to come back
            cur.next = cur.child;
            cur.child.prev = cur;
            cur.child = null;
        }
        if (cur.next == null && !stack.isEmpty()) {
            Node resume = stack.pop();
            cur.next = resume;
            resume.prev = cur;
        }
        cur = cur.next;
    }
    return head;
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (cur.child != null)` | a branch starts here |
| `if (cur.next != null) stack.push(cur.next);` | save the rest of this level for later |
| `cur.next = cur.child; cur.child.prev = cur; cur.child = null;` | splice the child list in right after cur (both directions), and clear the child pointer |
| `if (cur.next == null && !stack.isEmpty())` | we reached the end of a branch while saved work is waiting |
| `resume = stack.pop(); cur.next = resume; resume.prev = cur;` | reconnect the most recently saved part (the deepest level first) |
| `cur = cur.next;` | continue in the now-flattened order |

### Dry run: the 3-level example

| cur | action | stack (top first) |
|---|---|---|
| 1 | — | [] |
| 2 | — | [] |
| 3 | has child: **push 4**, go into 7 | [4] |
| 7 | — | [4] |
| 8 | has child: **push 9**, go into 11 | [9, 4] |
| 11 | — | [9, 4] |
| 12 | dead end: **pop 9**, 12 → 9 | [4] |
| 9 | — | [4] |
| 10 | dead end: **pop 4**, 10 → 4 | [] |
| 4, 5, 6 | — | [] |

**Result `1 2 3 7 8 11 12 9 10 4 5 6`** ✓, with consistent prev pointers and no children left.

### Dry run 2: a vertical line, `1` with child `2` with child `3`
1: has child, no next to push → 1 → 2. 2: has child → 2 → 3. 3: no next, the stack is empty → done. **`[1, 2, 3]`** ✓

### Complexity
- **Time O(n):** each node is visited once; each push and pop happens once.
- **Space O(depth):** the stack holds at most one saved node per level.

---

## 6. Comparing the approaches

| | Recursive (returns the tail) | Explicit stack |
|---|---|---|
| Time | O(n) | O(n) |
| Space | O(depth) call stack | O(depth) heap stack |
| Stack-overflow risk | for very deep nesting | **none** |

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty list, a child on the head, a child on the last node (nothing to push), deep nesting with no `next` anywhere, several children on one level.

**Mistakes:**
1. Forgetting to set `child = null`.
2. Updating only `next` and not `prev` (the doubly-linked invariant breaks).
3. Pushing `null` when the node with a child has no next.
4. Using a queue instead of a stack, which gives the wrong order (that would be breadth-first).

---

## 8. Interview questions and answers

**Q1. Why is this a pre-order traversal?**
> Each node comes before its child branch, which comes before the node's next sibling: node, child subtree, then the rest. That's pre-order, with `child` as the left child and `next` as the right child.

**Q2. Why a stack?**
> When we descend into a child, the parent's old `next` must be resumed **after** the whole child branch, and the deepest saved part must resume first. That's LIFO.

### Follow-up questions

**F1. Flatten Binary Tree to Linked List (LC 114).** → [07-Trees M11](../../07-Trees/03-Medium/M11_FlattenBinaryTreeToLinkedList.md)
> The same pre-order splicing with left/right pointers.

**F2. Flatten Nested List Iterator (LC 341).**
> A stack of iterators, lazily descending into nested lists.

**F3. Un-flatten (rebuild the levels).**
> You'd need to keep the original child information (e.g. store which nodes had children), because flattening loses it.

---

## 9. Running the file

```bash
java M09_FlattenMultilevelDoublyLinkedList.java      # 5/5 tests passed
```

**One-line summary:** *"Pre-order splice: at a child, push the old next and dive in; at a dead end, pop and reconnect; fix both prev and next."*
