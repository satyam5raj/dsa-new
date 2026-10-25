# M13 · Reverse Linked List II ⭐

| | |
|---|---|
| **LeetCode** | [#92 Reverse Linked List II](https://leetcode.com/problems/reverse-linked-list-ii/) |
| **Difficulty** | Medium |
| **Pattern** | Reverse a sublist in one pass ("head insertion") |
| **Code** | [M13_ReverseLinkedListII.java](M13_ReverseLinkedListII.java) |
| **Run** | `java M13_ReverseLinkedListII.java` |

⭐ = a bonus: it's the core technique behind Reverse Nodes in k-Group (H01).

---

## 1. The problem

Reverse only the nodes at positions **left..right** (1-indexed), in one pass.

| list | left | right | result |
|---|---|---|---|
| `1 → 2 → 3 → 4 → 5` | 2 | 4 | `1 → 4 → 3 → 2 → 5` |
| `5` | 1 | 1 | `5` |
| `1 → 2 → 3 → 4` | 1 | 4 | `4 → 3 → 2 → 1` |
| `3 → 5` | 1 | 2 | `5 → 3` |

**Constraints:** `1 ≤ left ≤ right ≤ n ≤ 500`.

---

## 2. How to think about it

### Option A: cut, reverse, reconnect
Find the node before the section and the node after it, reverse the section with [E01](../02-Easy/E01_ReverseLinkedList.md)'s loop, then reconnect both ends. It works, but there's a lot of bookkeeping.

### Option B: head insertion (the file's solution)
1. `dummy → head`; walk `prev` to the node **before** position `left`. (The dummy covers left = 1.)
2. `start = prev.next` is the first node of the section. It will end up as the section's **last** node, and it **stays in place** while we work.
3. Repeat `right − left` times: take the node right **after** `start` and move it to the **front** of the section (right after `prev`).

```
then = start.next
start.next = then.next     (unhook 'then')
then.next  = prev.next     (point it at the current front of the section)
prev.next  = then          (make it the new front)
```

Each move puts one more node in reversed position.

---

## 3. Java concepts used in this problem

### 3.1 1-indexed positions with a dummy

```java
for (int i = 1; i < left; i++) prev = prev.next;   // left − 1 steps from the dummy
```

Starting at the dummy and taking `left − 1` steps lands on the node at position `left − 1`, i.e. the predecessor of the section (the dummy itself when left = 1).

### 3.2 "Head insertion" as a general trick

Moving a node to the front of a list is O(1): unhook it and link it after the head pointer. Doing that repeatedly with each next node **reverses** the sequence. The same idea is used to reverse k-groups.

### 3.3 An exhaustive test

The file tries **every** `(n ≤ 10, left, right)` combination against an array reversal: 220 cases. With small sizes, exhaustive tests catch every off-by-one.

---

## 4. Approach 1: Cut, reverse, reconnect

```java
static ListNode reverseBetweenCut(ListNode head, int left, int right) {
    ListNode dummy = new ListNode(0, head), before = dummy;
    for (int i = 1; i < left; i++) before = before.next;
    ListNode first = before.next;                 // the first node of the section (it becomes the last)
    ListNode prev = null, cur = first;
    for (int i = left; i <= right; i++) {         // the standard reversal of right − left + 1 nodes
        ListNode next = cur.next;
        cur.next = prev;
        prev = cur;
        cur = next;
    }
    before.next = prev;                           // prev = the new front of the section
    first.next = cur;                             // cur = the node after the section
    return dummy.next;
}
```

**O(n) time, O(1) space.** It's the "obvious" version; you just have to remember the two reconnections.

---

## 5. Approach 2: Head insertion (the file's solution)

### Code ([M13_ReverseLinkedListII.java:39-50](M13_ReverseLinkedListII.java#L39-L50))

```java
static ListNode reverseBetween(ListNode head, int left, int right) {
    ListNode dummy = new ListNode(0, head), prev = dummy;
    for (int i = 1; i < left; i++) prev = prev.next;      // node before the section
    ListNode start = prev.next;                           // becomes the section's tail
    for (int i = 0; i < right - left; i++) {
        ListNode then = start.next;
        start.next = then.next;
        then.next = prev.next;
        prev.next = then;
    }
    return dummy.next;
}
```

### Line by line

| Line | What it does |
|---|---|
| `dummy → head`, walk prev | prev = the node before position left |
| `ListNode start = prev.next;` | the first node of the section; it slides towards the end as nodes jump in front of it |
| `for (i < right - left)` | one move per node after the first in the section |
| `then = start.next;` | the next node to move |
| `start.next = then.next;` | unhook `then` (start now skips it) |
| `then.next = prev.next;` | `then` points at the current section front |
| `prev.next = then;` | `then` becomes the new section front |

### Dry run: `1 → 2 → 3 → 4 → 5`, left = 2, right = 4

prev = 1, start = 2. Two moves (right − left = 2):

| move | then | after the move |
|---|---|---|
| 1 | 3 | `1 → 3 → 2 → 4 → 5` |
| 2 | 4 | `1 → 4 → 3 → 2 → 5` |

**Result `1 → 4 → 3 → 2 → 5`** ✓. `start` (the node 2) stayed in place and ended up as the section's tail, still linked to 5.

### Dry run 2: `3 → 5`, left = 1, right = 2
prev = dummy, start = 3, one move: then = 5 → `3.next = null`, `5.next = 3`, `dummy.next = 5` → **`5 → 3`** ✓ (the dummy handled the head change).

### Complexity
- **Time O(n)** (it walks up to `right`), **Space O(1).**

---

## 6. Comparing the approaches

| | Cut + reverse + reconnect | Head insertion |
|---|---|---|
| Time | O(n) | O(n) |
| Space | O(1) | O(1) |
| Bookkeeping | 2 reconnections at the end | none at the end |

---

## 7. Edge cases and common mistakes

**Edge cases:** left = right (no change), left = 1 (the head changes), right = n (the section ends at the tail), the whole list.

**Mistakes:**
1. No dummy, which breaks left = 1.
2. Doing `right − left + 1` moves (one too many; the first node doesn't move).
3. Moving `start` inside the loop (it must stay fixed; the other nodes jump in front of it).
4. Wrong assignment order, which loses `then.next`.

---

## 8. Interview questions and answers

**Q1. Explain head insertion.**
> I keep the predecessor of the section fixed and repeatedly take the node after the original first node and insert it right after the predecessor. After `right − left` moves, the section is reversed and still connected on both sides.

**Q2. Why does `start` end at the tail of the section?**
> Every other node in the section is moved in front of it, one by one, so it drifts to the last position. Its `next` always points to the first node not yet processed (finally, the node after the section).

### Follow-up questions

**F1. Reverse Nodes in k-Group (LC 25).** → [H01](../04-Hard/H01_ReverseNodesInKGroup.md)
> Apply this to positions 1..k, k+1..2k, … while a full group remains.

**F2. Reverse Nodes in Even Length Groups (LC 2074).**
> Walk groups of size 1, 2, 3…; reverse a group only when its actual length is even.

**F3. Reverse alternate k-node segments.**
> Reverse k, skip k, repeat, using the same head insertion.

---

## 9. Running the file

```bash
java M13_ReverseLinkedListII.java      # 5/5 tests passed (including every n ≤ 10, left, right)
```

**One-line summary:** *"Fix prev and start; right − left times, move start.next to just after prev."*
