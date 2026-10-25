# M11 · Delete Node in a Linked List

| | |
|---|---|
| **LeetCode** | [#237 Delete Node in a Linked List](https://leetcode.com/problems/delete-node-in-a-linked-list/) |
| **Difficulty** | Medium (really a trick question) |
| **Pattern** | Copy the next node into this one, then skip the next |
| **Code** | [M11_DeleteNodeInLinkedList.java](M11_DeleteNodeInLinkedList.java) |
| **Run** | `java M11_DeleteNodeInLinkedList.java` |

---

## 1. The problem

You're given **only the node to delete**, **not** the head. The node is guaranteed **not to be the tail**. "Delete" it, so its value disappears from the list.

| list | node given | result |
|---|---|---|
| `4 → 5 → 1 → 9` | the node 5 | `4 → 1 → 9` |
| `4 → 5 → 1 → 9` | the node 1 | `4 → 5 → 9` |
| `1 → 2` | the node 1 (the head) | `2` |

---

## 2. How to think about it

### The puzzle
Normal deletion needs the **previous** node (`prev.next = node.next`), and we can't reach it: there's no head and no `prev` pointer.

### The trick
Don't delete **this** node object. Turn it into a **copy of the next node**, and then delete the **next** node (whose predecessor we **do** have: this one):

```
node.val  = node.next.val
node.next = node.next.next

4 → [5] → 1 → 9     copy 1 into it:   4 → [1] → 1 → 9
                    skip the next:    4 → [1] → 9
```

### The limitations (discussing them is the real interview value)
- It's **impossible for the tail**: there's no next node to copy.
- The node **object** that disappears is actually the **next** one. If other code holds references to node objects (e.g. a `HashMap<key, node>` in an LRU cache), those references would now be wrong: the "deleted" key's node still exists, holding a different value.
- It only works when copying the value is allowed and cheap.

---

## 3. Java concepts used in this problem

### 3.1 Changing an object through a reference

```java
static void deleteNode(ListNode node) {
    node.val = node.next.val;     // modifies the actual node object the caller's list contains
```

The method receives a **copy of the reference**, but it points to the **same node** that's in the list, so the change is visible through the list (see Arrays E03, "pass-by-value of references").

### 3.2 Identity versus value

After the trick, the list *looks* right (the values are correct), but **which node object** is gone changed. In Java terms: the list's values are right, but object identity is not preserved. That distinction matters whenever node references are shared elsewhere.

---

## 4. The solution (the file's code)

### Code ([M11_DeleteNodeInLinkedList.java:38-41](M11_DeleteNodeInLinkedList.java#L38-L41))

```java
static void deleteNode(ListNode node) {
    node.val = node.next.val;          // become the next node...
    node.next = node.next.next;        // ...and drop the original next
}
```

### Line by line

| Line | What it does |
|---|---|
| `node.val = node.next.val;` | copy the successor's value into this node |
| `node.next = node.next.next;` | unlink the successor (now a duplicate) |

### Dry run 1: delete 5 from `4 → 5 → 1 → 9`

| step | list |
|---|---|
| start | `4 → [5] → 1 → 9` |
| copy | `4 → [1] → 1 → 9` |
| skip | `4 → [1] → 9` |

**Result `4 → 1 → 9`** ✓

### Dry run 2: delete the head of `1 → 2`
Copy 2 into the head → `2 → 2`; skip → `2`. The caller's `head` variable still points to the same object, which now holds 2 → prints `[2]` ✓

### Complexity
- **Time O(1)**, **Space O(1).**

---

## 5. Why there's no "approach 2"

Without access to the head or a `prev` pointer, there's simply no other way to change what points **to** this node. The value-copy is the only O(1) technique. (If you **were** given the head, the normal `prev.next = node.next` deletion would be the right choice.)

---

## 6. Edge cases and common mistakes

**Edge cases:** deleting the head (works), deleting the second-to-last node (works), the tail (impossible by the rules).

**Mistakes:**
1. Trying to set `node = null`, which only changes the local variable, not the list.
2. Copying the value but forgetting to unlink the next node, which leaves a duplicate value.

---

## 7. Interview questions and answers

**Q1. Why can't you delete the tail this way?**
> There's no next node to copy. You'd have to set the previous node's `next` to null, but you can't reach it.

**Q2. Is this a "real" deletion?**
> The list's **values** are correct, but the node object that disappears is the successor, not the given one. Any external references to nodes (caches, maps, iterators) can become inconsistent, so it's a hack that's only acceptable under this problem's constraints.

**Q3. When is this trick legitimately useful?**
> Removing an element from a singly linked structure in O(1) when you hold a pointer to it and its successor exists: some lock-free and lazy data structures mark nodes and swap contents in a similar way.

### Follow-up questions

**F1. Delete with access to the head.** → [E06](../02-Easy/E06_RemoveLinkedListElements.md)
> The standard predecessor-based unlinking.

**F2. O(1) deletion of any node, including the tail.**
> Use a **doubly** linked list: `node.prev.next = node.next; node.next.prev = node.prev`. This is what LRU caches use.

---

## 8. Running the file

```bash
java M11_DeleteNodeInLinkedList.java      # 3/3 tests passed
```

**One-line summary:** *"Can't reach the predecessor, so become your successor (copy its value) and unlink it."*
