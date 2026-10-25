# M02 · Remove Nth Node From End of List

| | |
|---|---|
| **LeetCode** | [#19 Remove Nth Node From End of List](https://leetcode.com/problems/remove-nth-node-from-end-of-list/) |
| **Difficulty** | Medium |
| **Pattern** | Two pointers with a fixed gap + a dummy head (one pass) |
| **Code** | [M02_RemoveNthNodeFromEnd.java](M02_RemoveNthNodeFromEnd.java) |
| **Run** | `java M02_RemoveNthNodeFromEnd.java` |

---

## 1. The problem

Remove the **n-th node from the end** and return the head. Follow-up: **one pass**.

| list | n | result |
|---|---|---|
| `1 → 2 → 3 → 4 → 5` | 2 | `1 → 2 → 3 → 5` (the 4 is removed) |
| `1` | 1 | `[]` |
| `1 → 2` | 1 | `1` (the tail is removed) |
| `1 → 2` | 2 | `2` (the head is removed) |

**Constraints:** `1 ≤ n ≤ length ≤ 30`.

---

## 2. How to think about it

1. **Two passes:** count the length L; the target is at position L − n (0-based) from the start. Walk to the node before it and unlink.
2. **One pass:** keep two pointers **exactly n nodes apart**. When the front one runs off the end, the back one is **right before** the node to delete.
   1. `dummy → head` (the head itself might be the one removed)
   2. `fast = slow = dummy`; move `fast` **n + 1** steps ahead
   3. move both until `fast == null`
   4. `slow.next` is the target: `slow.next = slow.next.next`

**Why n + 1 steps?** We want `slow` to stop **one node before** the target (to unlink it). With a gap of n + 1 positions, when fast is at null (one past the end), slow is n + 1 positions back: the node just before the n-th from the end.

**Why the dummy?** When n equals the length, the target is the head, and slow must stand **before** the head. The dummy is that spot.

---

## 3. Java concepts used in this problem

### 3.1 A fixed-gap ("ruler") pair of pointers

Instead of different speeds (as in [E07](../02-Easy/E07_MiddleOfLinkedList.md)), the two pointers move at the **same speed** with a **constant distance** between them. When the front hits the end, the back is at a known distance from the end. Think of a ruler of length n + 1 sliding along the list.

### 3.2 `for (int i = 0; i <= n; i++)` runs n + 1 times

`<=` gives the n + 1 steps. The constraint `n ≤ length` guarantees `fast` never dereferences null: after n + 1 steps from the dummy, it's at worst `null` (one past the last node).

---

## 4. Approach 1: Two passes (count, then remove)

```java
static ListNode removeNthTwoPass(ListNode head, int n) {
    int len = 0;
    for (ListNode c = head; c != null; c = c.next) len++;
    ListNode dummy = new ListNode(0, head), prev = dummy;
    for (int i = 0; i < len - n; i++) prev = prev.next;   // stop just before the target
    prev.next = prev.next.next;
    return dummy.next;
}
```

**Dry run** `1..5`, n = 2: len = 5 → walk 3 steps from the dummy → prev = 3 → unlink 4 ✓.
**O(L) time (two passes), O(1) space.** Perfectly good; the one-pass version is the follow-up.

---

## 5. Approach 2: One pass with a gap (the file's solution)

### Code ([M02_RemoveNthNodeFromEnd.java:38-48](M02_RemoveNthNodeFromEnd.java#L38-L48))

```java
static ListNode removeNthFromEnd(ListNode head, int n) {
    ListNode dummy = new ListNode(0, head);
    ListNode fast = dummy, slow = dummy;
    for (int i = 0; i <= n; i++) fast = fast.next;     // n+1 steps -> gap of n nodes
    while (fast != null) {
        fast = fast.next;
        slow = slow.next;
    }
    slow.next = slow.next.next;                        // unlink the target
    return dummy.next;
}
```

### Line by line

| Line | What it does |
|---|---|
| `ListNode dummy = new ListNode(0, head);` | a node before the head |
| `ListNode fast = dummy, slow = dummy;` | both start at the dummy |
| `for (i = 0; i <= n; i++) fast = fast.next;` | fast gets an n + 1 step head start |
| `while (fast != null) { fast = fast.next; slow = slow.next; }` | slide the "ruler" until fast falls off the end |
| `slow.next = slow.next.next;` | slow is just before the target → unlink it |
| `return dummy.next;` | the head (it may have changed) |

### Dry run 1: `1 → 2 → 3 → 4 → 5`, n = 2

After the head start (3 steps from D): fast = 3, slow = D.

| move | slow | fast |
|---|---|---|
| start | D | 3 |
| 1 | 1 | 4 |
| 2 | 2 | 5 |
| 3 | **3** | null → stop |

`slow.next` = 4 → unlink → **`1 → 2 → 3 → 5`** ✓

### Dry run 2: `1 → 2`, n = 2 (remove the head)

Head start of 3 steps: D → 1 → 2 → null, so fast = null right away. The while loop doesn't run; slow = D → `D.next = 2` → **return `2`** ✓. The dummy made removing the head a normal case.

### Dry run 3: `1`, n = 1
fast: D → 1 → null; slow = D → `D.next = null` → **return null** ✓

### Complexity
- **Time O(L):** one pass.
- **Space O(1).**

---

## 6. Comparing the approaches

| | Two passes | One pass with a gap |
|---|---|---|
| Time | O(L), 2 passes | O(L), 1 pass |
| Space | O(1) | O(1) |
| Needs a dummy | yes (for removing the head) | yes |

---

## 7. Edge cases and common mistakes

**Edge cases:** n = length (remove the head), n = 1 (remove the tail), a single-node list.

**Mistakes:**
1. Moving fast only n steps, so slow stops **on** the target instead of before it.
2. No dummy, which crashes or fails when removing the head.
3. Returning `head` instead of `dummy.next` (the head may have been removed).

---

## 8. Interview questions and answers

**Q1. Why move fast n + 1 steps instead of n?**
> To delete a node you need its predecessor. A gap of n + 1 leaves slow one node before the n-th from the end when fast reaches null.

**Q2. Is the one-pass version really better?**
> It's the same O(L). It's "one pass" in that each node is visited by fast once. It's useful when the list is a stream you can only read once (but then you'd need to buffer n + 1 nodes anyway).

### Follow-up questions

**F1. Return the k-th node from the end (without removing it).**
> A gap of k: move fast k steps, then move both until fast is null; slow is the answer.

**F2. Swapping Nodes in a Linked List (LC 1721): swap the k-th from the start with the k-th from the end.**
> Find the first with k − 1 steps; find the second with the gap technique; swap their values (or relink them).

**F3. Rotate List (LC 61).** → [M06](M06_RotateList.md)
> Uses "the k-th from the end" to find the new tail.

---

## 9. Running the file

```bash
java M02_RemoveNthNodeFromEnd.java      # 5/5 tests passed
```

**One-line summary:** *"Dummy; fast goes n + 1 ahead; move both until fast is null; slow.next is the target."*
