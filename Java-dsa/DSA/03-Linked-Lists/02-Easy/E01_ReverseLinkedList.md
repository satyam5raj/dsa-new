# E01 · Reverse Linked List

| | |
|---|---|
| **LeetCode** | [#206 Reverse Linked List](https://leetcode.com/problems/reverse-linked-list/) |
| **Difficulty** | Easy |
| **Pattern** | In-place reversal (`prev` / `cur` / `next`): **the #1 linked-list skill** |
| **Code** | [E01_ReverseLinkedList.java](E01_ReverseLinkedList.java) |
| **Run** | `java E01_ReverseLinkedList.java` |

---

## 1. The problem

Reverse a singly linked list and return the **new head**.

| input | output |
|---|---|
| `1 → 2 → 3 → 4 → 5` | `5 → 4 → 3 → 2 → 1` |
| `1 → 2` | `2 → 1` |
| `7` | `7` |
| `[]` (null) | `[]` (null) |

**Constraints:** `0 ≤ n ≤ 5000`. Follow-up: *both iteratively and recursively*.

This reversal shows up **inside** many harder problems: Palindrome List (E05), Reorder List (M04), Reverse Between (M13), Reverse in k-Group (H01). Master it until you can write it without thinking.

---

## 2. How to think about it

- **Easy but not wanted:** copy the values into an array and rebuild (O(n) extra space).
- **In place:** flip each arrow, one node at a time. The danger is that the moment you flip `cur.next`, you **lose** the rest of the list. So always **save it first**:

```
prev = null, cur = head
while cur != null:
    next = cur.next     // 1. SAVE the rest (we're about to lose it)
    cur.next = prev     // 2. FLIP the arrow
    prev = cur          // 3. move prev forward
    cur = next          // 4. move cur forward
return prev             // cur is null; prev is the old tail = the new head
```

Remember it as **"save, flip, move, move"**.

- **Recursive:** trust that `reverse(head.next)` reverses everything after `head` and returns the new head. Then just attach `head` at the end of that reversed part.

---

## 3. Java concepts used in this problem

This is the first linked-list problem, so here are the basics.

### 3.1 The `ListNode` class

```java
static class ListNode {
    int val;            // the data
    ListNode next;      // a REFERENCE to the next node (or null at the end)
    ListNode(int val) { this.val = val; }
    ListNode(int val, ListNode next) { this.val = val; this.next = next; }
}
```

- A **node** holds a value and a **reference** (a pointer) to the next node. The list is just the chain you get by following `next` from the **head**.
- The last node's `next` is **`null`**, which means "no more nodes".
- **Two constructors** (overloading): same name, different parameters. `new ListNode(5)` uses the first; `new ListNode(5, other)` the second. `this.val = val` assigns the **field** `val` from the **parameter** `val`.
- It's a `static class` nested inside the problem class, so each file stays self-contained (see the arrays notes on nested classes).

### 3.2 References, not copies

```java
ListNode a = new ListNode(1);
ListNode b = a;        // b points to the SAME node object, nothing is copied
b.val = 9;             // a.val is now 9 too
```

Variables of a class type hold **references** (addresses). `cur = cur.next` doesn't move any node; it moves your **finger** to the next node. `cur.next = prev` **changes an arrow** inside a node. Understanding the difference between "moving a pointer variable" and "changing a node's next field" is the whole game in linked-list problems.

### 3.3 `null` and `NullPointerException`

`null` means "refers to nothing". Calling a method or reading a field through `null` (`cur.next` when `cur` is `null`) throws a **NullPointerException (NPE)**. That's why loop conditions are `while (cur != null)`, and why we check `head == null` before touching `head.next`.

### 3.4 `@Override public String toString()`

```java
@Override
public String toString() { ... }   // prints the whole list from this node: [1, 2, 3]
```

Every Java class inherits `toString()` from `Object` (by default it prints something like `ListNode@1b6d3586`). **Overriding** it lets us print `[5, 4, 3, 2, 1]`. `@Override` asks the compiler to check that we really are overriding a method; it catches typos like `tostring`. The helper also has a `guard` counter that stops after 1000 nodes, in case a bug creates a cycle.

### 3.5 The `build(int... vals)` helper with a dummy node

```java
static ListNode build(int... vals) {           // build(1, 2, 3) → 1 → 2 → 3
    ListNode dummy = new ListNode(0), t = dummy;
    for (int v : vals) { t.next = new ListNode(v); t = t.next; }
    return dummy.next;
}
```

It uses varargs (see Arrays E06) and a **dummy head**: a fake first node, so that appending is the same code for the first node and every later one. You'll use the dummy-node trick in many problems (E02, E06…).

### 3.6 Recursion on lists

A list is naturally recursive: **a node followed by a (smaller) list**. Recursive solutions say "solve the rest, then fix up the current node". Each call uses stack space, so the recursion depth is n (5000 is fine; around 10⁵ could overflow the default stack).

---

## 4. Approach 1: Copy the values into an array

```java
static ListNode reverseByArray(ListNode head) {
    List<Integer> vals = new ArrayList<>();
    for (ListNode c = head; c != null; c = c.next) vals.add(c.val);
    ListNode c = head;
    for (int i = vals.size() - 1; i >= 0; i--) { c.val = vals.get(i); c = c.next; }   // overwrite the values
    return head;
}
```

- `for (ListNode c = head; c != null; c = c.next)` is the standard **list traversal loop**.
- It reverses the **values**, not the links. It works, but uses **O(n) extra space** and isn't what interviewers want (and in real code, nodes often carry more than one value).

---

## 5. Approach 2: Iterative pointer reversal (the file's main solution)

### Code ([E01_ReverseLinkedList.java:45-54](E01_ReverseLinkedList.java#L45-L54))

```java
static ListNode reverseList(ListNode head) {
    ListNode prev = null, cur = head;
    while (cur != null) {
        ListNode next = cur.next;
        cur.next = prev;
        prev = cur;
        cur = next;
    }
    return prev;
}
```

### Line by line

| Line | What it does |
|---|---|
| `ListNode prev = null, cur = head;` | `prev` = the head of the **already-reversed** part (empty at first); `cur` = the next node to process |
| `while (cur != null)` | there are still nodes to flip |
| `ListNode next = cur.next;` | **save** the rest of the list before we lose it |
| `cur.next = prev;` | **flip** cur's arrow to point backwards |
| `prev = cur;` | the reversed part now starts at cur |
| `cur = next;` | move on to the saved rest |
| `return prev;` | when cur is null, prev is the old tail, which is the new head |

### Dry run: `1 → 2 → 3`

| step | prev | cur | next (saved) | the list after this step |
|---|---|---|---|---|
| start | null | 1 | — | `1 → 2 → 3 → null` |
| 1 | 1 | 2 | 2 | `null ← 1`   `2 → 3` |
| 2 | 2 | 3 | 3 | `null ← 1 ← 2`   `3` |
| 3 | 3 | null | null | `null ← 1 ← 2 ← 3` |

The loop ends (cur is null) → **return 3**: `3 → 2 → 1` ✓

### Dry run: empty list
`cur = null` → the loop never runs → **return null** ✓

### Complexity
- **Time O(n):** each node is visited once.
- **Space O(1):** three pointer variables.

---

## 6. Approach 3: Recursive reversal (the file's second method)

### Code ([E01_ReverseLinkedList.java:56-62](E01_ReverseLinkedList.java#L56-L62))

```java
static ListNode reverseListRecursive(ListNode head) {
    if (head == null || head.next == null) return head;
    ListNode newHead = reverseListRecursive(head.next);
    head.next.next = head;
    head.next = null;
    return newHead;
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (head == null \|\| head.next == null) return head;` | **base case**: an empty or single-node list is its own reverse |
| `ListNode newHead = reverseListRecursive(head.next);` | reverse everything **after** head. Trust it: afterwards, `head.next` (the old second node) is the **tail** of that reversed part |
| `head.next.next = head;` | make that tail point back to head |
| `head.next = null;` | head is the new tail (otherwise there'd be a 2-node cycle) |
| `return newHead;` | the new head (the old last node) is passed all the way back up |

### Dry run: `1 → 2 → 3`

The calls go down: `rev(1)` → `rev(2)` → `rev(3)`. `rev(3)` hits the base case and returns 3.

| returning from | head | action | list state | returns |
|---|---|---|---|---|
| rev(3) | 3 | base case | `1 → 2 → 3` | 3 |
| rev(2) | 2 | `2.next.next = 2` (3 → 2), `2.next = null` | `1 → 2 ← 3` | 3 |
| rev(1) | 1 | `1.next.next = 1` (2 → 1), `1.next = null` | `1 ← 2 ← 3` | 3 |

**Result `3 → 2 → 1`** ✓

### Complexity
- **Time O(n)**, **Space O(n)** for the call stack (one frame per node).

---

## 7. Comparing the approaches

| | Array copy | Iterative | Recursive |
|---|---|---|---|
| Time | O(n) | O(n) | O(n) |
| Extra space | O(n) | **O(1)** | O(n) stack |
| Changes links (not just values) | no | **yes** | yes |
| Interview default | no | **yes** | as a follow-up |

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty list, a single node, two nodes.

**Mistakes:**
1. Flipping `cur.next` **before** saving it, which loses the rest of the list.
2. Returning `head` (that's now the tail) instead of `prev`.
3. In the recursive version, forgetting `head.next = null`, which leaves a cycle between the first two nodes.
4. Writing `while (cur.next != null)`, which skips the last node and can throw an NPE on an empty list.

---

## 9. Interview questions and answers

**Q1. Explain the iterative reversal.**
> Walk the list with `prev` and `cur`. For each node: save `cur.next`, point `cur.next` back to `prev`, then advance both. When `cur` falls off the end, `prev` is the new head. O(n) time, O(1) space.

**Q2. What's the risk with the recursive version?**
> It uses O(n) stack frames; for very long lists it can throw `StackOverflowError`, because Java doesn't optimise tail calls.

**Q3. How would you test it?**
> An empty list, one node, two nodes, many nodes; and reversing twice should give back the original.

### Follow-up questions

**F1. Reverse only the positions left..right (LC 92).** → [M13 Reverse Linked List II](../03-Medium/M13_ReverseLinkedListII.md)
> Walk to the node before `left`, reverse `right − left + 1` nodes with the same loop, then reconnect both ends.

**F2. Reverse in groups of k (LC 25).** → [H01](../04-Hard/H01_ReverseNodesInKGroup.md)
> Repeatedly reverse k-node blocks, connecting each reversed block to the next.

**F3. Is the list a palindrome?** → [E05](E05_PalindromeLinkedList.md)
> Find the middle, reverse the second half, compare.

**F4. Reverse a doubly linked list.**
> For each node, swap its `next` and `prev`; the new head is the old tail.

**F5. Print a list in reverse without modifying it.**
> Recursion (print after the recursive call) or a stack.

---

## 10. Running the file

```bash
cd Java-dsa/DSA/03-Linked-Lists/02-Easy
java E01_ReverseLinkedList.java      # 5/5 tests passed
```

The test helper prints lists using `ListNode.toString()`, so `check("...", "[5, 4, 3, 2, 1]", result)` compares the printed form.

**One-line summary:** *"Save next, flip the arrow, move prev, move cur; return prev."*
