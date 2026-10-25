# M10 · Design Linked List

| | |
|---|---|
| **LeetCode** | [#707 Design Linked List](https://leetcode.com/problems/design-linked-list/) |
| **Difficulty** | Medium |
| **Pattern** | Implementation with a sentinel (dummy) head + a size counter |
| **Code** | [M10_DesignLinkedList.java](M10_DesignLinkedList.java) |
| **Run** | `java M10_DesignLinkedList.java` |

---

## 1. The problem

Implement `MyLinkedList` with 0-indexed operations:

| method | behaviour |
|---|---|
| `get(index)` | the value at index, or `-1` if the index is invalid |
| `addAtHead(val)` | insert at the front |
| `addAtTail(val)` | append at the end |
| `addAtIndex(index, val)` | insert **before** the index-th node; `index == size` appends; `index > size` does nothing |
| `deleteAtIndex(index)` | delete if the index is valid |

LeetCode example: `addAtHead(1)`, `addAtTail(3)`, `addAtIndex(1, 2)` → `1 → 2 → 3`; `get(1)` = 2; `deleteAtIndex(1)` → `1 → 3`; `get(1)` = 3.

---

## 2. How to think about it

The design decisions (say them out loud in an interview):

1. **A sentinel head.** Every operation becomes "find the node **before** position i, then splice". Inserting at index 0 or deleting index 0 needs no special case. Then `addAtHead = addAtIndex(0)` and `addAtTail = addAtIndex(size)`.
2. **Keep `size`.** It gives O(1) bounds checks, with no walking to validate an index.
3. **Singly vs doubly.** A doubly linked list plus a tail sentinel lets you walk from the **closer** end (halving the average work), at the cost of more pointer updates. Singly is shown here; see [DoublyLinkedList](../01-Basics/DoublyLinkedList.java) for the other.

Every operation reduces to **one helper**: `predecessor(i)` = walk i steps from the sentinel.

---

## 3. Java concepts used in this problem

### 3.1 A private nested `Node` class

```java
static class MyLinkedList {
    private static class Node { int val; Node next; ... }
    private final Node sentinel = new Node(0, null);
    private int size;
```

- `private static class Node` hides the node type inside the list class. Users of `MyLinkedList` never see nodes, only values. That's **encapsulation**.
- `private final Node sentinel`: the sentinel reference never changes (`final`), but the nodes after it do.

### 3.2 Public API methods versus private helpers

`get`, `addAtHead`, … are `public` (the API). `predecessor` is `private` (an implementation detail). Splitting the code like this keeps every public method short and reduces duplicated loops.

### 3.3 Inserting with the two-argument constructor

```java
p.next = new Node(val, p.next);    // the new node points to p's old next, and p points to the new node
```

This one line does "new.next = p.next; p.next = new" in the correct order: the right-hand side is evaluated first, while `p.next` still has its old value.

### 3.4 Switch expressions with `->` (in the test)

```java
switch (rnd.nextInt(5)) {
    case 0 -> { mine.addAtHead(v); ref.add(0, v); }
    ...
    default -> ok &= ...;
}
```

The arrow form (Java 14+) has **no fall-through**, so you don't need `break`. The test runs 5000 random operations against `java.util.ArrayList` as the reference.

---

## 4. Approach 1: Without a sentinel (for contrast)

```java
// addAtIndex without a sentinel needs a special case for index 0:
void addAtIndexNoSentinel(int index, int val) {
    if (index < 0 || index > size) return;
    if (index == 0) { head = new Node(val, head); size++; return; }   // special case
    Node p = head;
    for (int i = 0; i < index - 1; i++) p = p.next;
    p.next = new Node(val, p.next);
    size++;
}
```

Every operation would need its own `index == 0` branch. The sentinel removes all of them.

---

## 5. Approach 2: Sentinel + size (the file's solution)

### Code ([M10_DesignLinkedList.java:33-72](M10_DesignLinkedList.java#L33-L72))

```java
static class MyLinkedList {
    private static class Node {
        int val;
        Node next;
        Node(int val, Node next) { this.val = val; this.next = next; }
    }

    private final Node sentinel = new Node(0, null);
    private int size;

    // node right BEFORE position index (index 0 -> the sentinel)
    private Node predecessor(int index) {
        Node p = sentinel;
        for (int i = 0; i < index; i++) p = p.next;
        return p;
    }

    public int get(int index) {
        if (index < 0 || index >= size) return -1;
        return predecessor(index).next.val;
    }

    public void addAtHead(int val) { addAtIndex(0, val); }

    public void addAtTail(int val) { addAtIndex(size, val); }

    public void addAtIndex(int index, int val) {
        if (index < 0 || index > size) return;      // index == size is allowed (append)
        Node p = predecessor(index);
        p.next = new Node(val, p.next);
        size++;
    }

    public void deleteAtIndex(int index) {
        if (index < 0 || index >= size) return;
        Node p = predecessor(index);
        p.next = p.next.next;
        size--;
    }
}
```

### Line by line

| Method | What it does |
|---|---|
| `predecessor(index)` | walk `index` steps from the sentinel; index 0 → the sentinel itself |
| `get` | invalid → −1; otherwise the node after the predecessor |
| `addAtHead` / `addAtTail` | just `addAtIndex(0)` / `addAtIndex(size)` |
| `addAtIndex` | valid range `0..size` (inclusive, so appending works); splice the new node after the predecessor; size++ |
| `deleteAtIndex` | valid range `0..size−1`; unlink the predecessor's next; size−− |

**Note the different bounds:** inserting allows `index == size` (append), but get and delete need `index < size`.

### Dry run: the LeetCode sequence

| operation | predecessor | list after | size |
|---|---|---|---|
| addAtHead(1) → addAtIndex(0, 1) | sentinel | 1 | 1 |
| addAtTail(3) → addAtIndex(1, 3) | node 1 | 1 → 3 | 2 |
| addAtIndex(1, 2) | node 1 | 1 → 2 → 3 | 3 |
| get(1) | node 1 → next = 2 | | **2** |
| deleteAtIndex(1) | node 1 | 1 → 3 | 2 |
| get(1) | | | **3** |
| get(5) | invalid | | **−1** |
| addAtIndex(5, 9) | 5 > size → ignored | 1 → 3 | 2 |

### Complexity

| operation | time |
|---|---|
| get / addAtIndex / deleteAtIndex at index i | O(i) |
| addAtHead | O(1) |
| addAtTail | O(n) (O(1) with a tail pointer, which then needs updating on deletes) |
| space | O(n) |

---

## 6. Comparing the designs

| | Singly + sentinel | Doubly + head & tail sentinels |
|---|---|---|
| addAtTail | O(n) (or O(1) with a tail pointer) | **O(1)** |
| get(i) | O(i) | O(min(i, n − i)) |
| Pointer updates per insert | 2 | 4 |
| Memory per node | 1 reference | 2 references |

---

## 7. Edge cases and common mistakes

**Edge cases:** an index equal to size (append), a negative index, deleting from a single-element list, inserting into an empty list.

**Mistakes:**
1. Using `index >= size` as invalid for `addAtIndex` (appending would be rejected).
2. Forgetting `size++` / `size--`.
3. Walking `index − 1` steps from the head instead of `index` steps from the sentinel (off by one).
4. Exposing `Node` publicly (fine for LeetCode, but poor design).

---

## 8. Interview questions and answers

**Q1. Why a sentinel node?**
> Every position, including 0, then has a predecessor, so insert and delete are a single code path: find the predecessor and splice.

**Q2. Why store `size`?**
> O(1) validation of indices, and `addAtTail` can reuse `addAtIndex(size)`.

**Q3. How would you make addAtTail O(1)?**
> Keep a `tail` pointer, and update it on appends and when the last node is deleted. For deletes near the end you still need the predecessor, which in a singly list is O(n). A doubly linked list fixes that.

### Follow-up questions

**F1. Design a doubly linked list with O(1) remove(node).** → [DoublyLinkedList](../01-Basics/DoublyLinkedList.java)
> Head and tail sentinels; `node.prev.next = node.next; node.next.prev = node.prev`.

**F2. LRU Cache (LC 146).** → [12-Advanced M01](../../12-Advanced/03-Medium/M01_LRUCache.md)
> HashMap from key to node + a doubly linked list for the recency order: O(1) get and put.

**F3. Make it generic (`MyLinkedList<T>`).**
> `class Node<T> { T val; Node<T> next; }`, and return null instead of −1 for invalid gets (or throw).

---

## 9. Running the file

```bash
java M10_DesignLinkedList.java      # 5/5 tests passed (including 5,000 random ops vs ArrayList)
```

**One-line summary:** *"Sentinel + size; every operation = find the predecessor of index i, then splice."*
