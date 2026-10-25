# E06 · Remove Linked List Elements

| | |
|---|---|
| **LeetCode** | [#203 Remove Linked List Elements](https://leetcode.com/problems/remove-linked-list-elements/) |
| **Difficulty** | Easy |
| **Pattern** | Dummy head (removing the head becomes a normal case) |
| **Code** | [E06_RemoveLinkedListElements.java](E06_RemoveLinkedListElements.java) |
| **Run** | `java E06_RemoveLinkedListElements.java` |

---

## 1. The problem

Remove **every** node whose value equals `val`. Return the new head.

| list | val | result |
|---|---|---|
| `1 → 2 → 6 → 3 → 4 → 5 → 6` | 6 | `1 → 2 → 3 → 4 → 5` |
| `7 → 7 → 7 → 7` | 7 | `[]` |
| `[]` | 1 | `[]` |
| `7 → 7 → 1` | 7 | `1` (the head changes!) |

**Constraints:** `0 ≤ n ≤ 10⁴`.

---

## 2. How to think about it

### How do you delete a node from a singly linked list?
You need its **predecessor**: `prev.next = prev.next.next`. That "skips over" the node, and Java's garbage collector reclaims it.

```
prev → [6] → 3        becomes        prev ───────→ 3
```

### The problem with the head
The head has **no predecessor**. Without a trick you'd need two loops: one to skip matching nodes at the head, then another for the rest. Two code paths means twice the bugs.

### The dummy head
Put a **dummy** node before the head: `dummy → head`. Now **every** real node has a predecessor, so one loop handles everything:

```
prev = dummy
while prev.next != null:
    if prev.next.val == val:  prev.next = prev.next.next     // unlink; DON'T move prev
    else:                     prev = prev.next
return dummy.next
```

**Why not move `prev` after unlinking?** The node that is now `prev.next` hasn't been checked yet. It might also need deleting (`7 → 7`).

---

## 3. Java concepts used in this problem

### 3.1 Deleting by relinking + garbage collection

`prev.next = prev.next.next` makes the removed node **unreachable**. Java's garbage collector (GC) frees unreachable objects automatically, so there's no `delete` / `free` as in C/C++. (In C++ you'd have to free it yourself to avoid a memory leak.)

### 3.2 The two-argument constructor for the dummy

```java
ListNode dummy = new ListNode(0, head);   // the dummy points at head right away
```

### 3.3 Recursion returning the "fixed" list

```java
head.next = removeElementsRecursive(head.next, val);   // fix the rest first
return head.val == val ? head.next : head;             // then decide about head itself
```

"Solve the rest, then attach or skip myself" is a common recursive pattern for lists.

---

## 4. Approach 1: Without a dummy (two phases)

```java
static ListNode removeElementsNoDummy(ListNode head, int val) {
    while (head != null && head.val == val) head = head.next;   // phase 1: drop the matching head nodes
    ListNode cur = head;
    while (cur != null && cur.next != null) {                   // phase 2: the rest
        if (cur.next.val == val) cur.next = cur.next.next;
        else cur = cur.next;
    }
    return head;
}
```

It works, but needs a separate phase for the head, which is exactly what the dummy removes. **O(n), O(1).**

---

## 5. Approach 2: Dummy head (the file's main solution)

### Code ([E06_RemoveLinkedListElements.java:37-48](E06_RemoveLinkedListElements.java#L37-L48))

```java
static ListNode removeElements(ListNode head, int val) {
    ListNode dummy = new ListNode(0, head);
    ListNode prev = dummy;
    while (prev.next != null) {
        if (prev.next.val == val) {
            prev.next = prev.next.next;      // unlink
        } else {
            prev = prev.next;                // only advance when we keep the node
        }
    }
    return dummy.next;
}
```

### Line by line

| Line | What it does |
|---|---|
| `ListNode dummy = new ListNode(0, head);` | a fake node before the real head |
| `ListNode prev = dummy;` | `prev` = the last node we've decided to **keep** |
| `while (prev.next != null)` | there's a node after prev to examine |
| `if (prev.next.val == val) prev.next = prev.next.next;` | unlink it; prev stays, because the new `prev.next` must be checked too |
| `else prev = prev.next;` | keep the node and move on |
| `return dummy.next;` | the real head (possibly changed, possibly null) |

### Dry run 1: `7 → 7 → 1`, val = 7

| step | list (from dummy) | prev | prev.next | action |
|---|---|---|---|---|
| 1 | D → 7 → 7 → 1 | D | 7 | unlink |
| 2 | D → 7 → 1 | D | 7 | unlink |
| 3 | D → 1 | D | 1 | keep, prev = 1 |
| 4 | D → 1 | 1 | null | loop ends |

**Return `1`** ✓. The head changed twice, with no special code.

### Dry run 2: `1 → 2 → 6 → 3 → 4 → 5 → 6`, val = 6

The prev pointer moves over 1, 2; the first 6 is unlinked (prev stays at 2); prev moves over 3, 4, 5; the last 6 is unlinked; `prev.next` is null → done. **Result `1 → 2 → 3 → 4 → 5`** ✓

### Complexity
- **Time O(n)**, **Space O(1).**

---

## 6. Approach 3: Recursive (the file's second method)

### Code ([E06_RemoveLinkedListElements.java:50-54](E06_RemoveLinkedListElements.java#L50-L54))

```java
static ListNode removeElementsRecursive(ListNode head, int val) {
    if (head == null) return null;
    head.next = removeElementsRecursive(head.next, val);
    return head.val == val ? head.next : head;
}
```

| Line | What it does |
|---|---|
| `if (head == null) return null;` | base case: an empty list |
| `head.next = remove(head.next, val);` | clean the rest of the list first |
| `return head.val == val ? head.next : head;` | if head itself must go, return the cleaned rest; otherwise keep head |

**Dry run** `7 → 7 → 1`: `rem(1)` returns 1; `rem(second 7)` sets next = 1 and returns 1 (it's removed); `rem(first 7)` sets next = 1 and returns 1. **Result `1`** ✓

**O(n) time, O(n) stack.**

---

## 7. Comparing the approaches

| | No dummy | Dummy head | Recursive |
|---|---|---|---|
| Time | O(n) | O(n) | O(n) |
| Space | O(1) | O(1) | O(n) stack |
| Special cases | the head phase | **none** | none |

---

## 8. Edge cases and common mistakes

**Edge cases:** all nodes removed, the head removed, consecutive matches, an empty list, no matches.

**Mistakes:**
1. Advancing `prev` after unlinking, which skips the check of consecutive matches (`7 → 7` leaves one 7).
2. Returning `head` instead of `dummy.next` (the old head may have been removed).
3. Looping `while (cur != null)` and then reading `cur.next.val` without a null check.

---

## 9. Interview questions and answers

**Q1. Why use a dummy node?**
> It gives the head a predecessor, so deleting the head is the same operation as deleting any other node: `prev.next = prev.next.next`.

**Q2. Why doesn't `prev` advance after a deletion?**
> The node that took the deleted node's place hasn't been examined yet. Advancing would skip it.

### Follow-up questions

**F1. Delete a node when you're only given that node (LC 237).** → [M11](../03-Medium/M11_DeleteNodeInLinkedList.md)
> Copy the next node's value into it and unlink the next node.

**F2. Remove the nth node from the end (LC 19).** → [M02](../03-Medium/M02_RemoveNthNodeFromEnd.md)
> A dummy plus two pointers n apart.

**F3. Remove the duplicates from a sorted list.** → [E08](E08_RemoveDuplicatesFromSortedList.md)

**F4. The array version (Remove Element).** → [Arrays E24](../../01-Arrays/02-Easy/E24_RemoveElement.md)

---

## 10. Running the file

```bash
java E06_RemoveLinkedListElements.java      # 5/5 tests passed
```

**One-line summary:** *"dummy → head; if prev.next matches, unlink it (don't move prev), else advance prev."*
