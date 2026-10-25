# M04 · Reorder List

| | |
|---|---|
| **LeetCode** | [#143 Reorder List](https://leetcode.com/problems/reorder-list/) |
| **Difficulty** | Medium |
| **Pattern** | Middle (fast/slow) + reverse the second half + weave the two halves |
| **Code** | [M04_ReorderList.java](M04_ReorderList.java) |
| **Run** | `java M04_ReorderList.java` |

---

## 1. The problem

```
L0 → L1 → … → Ln−1 → Ln      becomes      L0 → Ln → L1 → Ln−1 → L2 → Ln−2 → …
```

Do it **in place**, by relinking the nodes (don't change their values).

| list | result |
|---|---|
| `1 → 2 → 3 → 4` | `1 → 4 → 2 → 3` |
| `1 → 2 → 3 → 4 → 5` | `1 → 5 → 2 → 4 → 3` |
| `1` | `1` |
| `1 → 2` | `1 → 2` |

**Constraints:** `1 ≤ n ≤ 5 × 10⁴`.

---

## 2. How to think about it

We need to take nodes **alternately from the front and from the back**.

1. **Easy way:** store the nodes in an `ArrayList`, then relink them with two indices i (front) and j (back). O(n) space.
2. **O(1) space:** a singly linked list can't walk backwards, so **reverse the back half** first:
   1. find the **first middle** and **cut** the list into two halves: `1 → 2 → 3` and `4 → 5`
   2. **reverse** the second half: `5 → 4`
   3. **weave** them together: `1 → 5 → 2 → 4 → 3`

This is the textbook "combine three patterns" problem: [E07](../02-Easy/E07_MiddleOfLinkedList.md) (middle), [E01](../02-Easy/E01_ReverseLinkedList.md) (reverse) and a merge like [E02](../02-Easy/E02_MergeTwoSortedLists.md) (but alternating instead of by value).

---

## 3. Java concepts used in this problem

### 3.1 Cutting a list: `slow.next = null`

To split one list into two independent lists, set the last node of the first half to point at `null`. Without the cut, the first half would still lead into the second half, and the weave would create a cycle.

### 3.2 A `void` method that relinks in place

`reorderList` returns nothing; the caller still holds `head`, which is still the first node (L0 stays first). The test helper `run` prints the list afterwards.

### 3.3 Saving two continuations before relinking

```java
ListNode n1 = first.next, n2 = second.next;   // save both before changing any arrow
first.next = second;
second.next = n1;
```

It's the "save before you overwrite" rule from reversal, applied to two lists at once.

### 3.4 A cross-check with a simple version

`expected(...)` builds the answer with plain array indices, and the test compares both on all lengths 0..25. Whenever pointer code is fiddly, test it against a dumb but obviously correct version.

---

## 4. Approach 1: Array of nodes + two indices

```java
static void reorderListArray(ListNode head) {
    if (head == null) return;
    List<ListNode> nodes = new ArrayList<>();
    for (ListNode c = head; c != null; c = c.next) nodes.add(c);
    int i = 0, j = nodes.size() - 1;
    while (i < j) {
        nodes.get(i).next = nodes.get(j);          // front → back
        i++;
        if (i == j) break;                          // they met (even length)
        nodes.get(j).next = nodes.get(i);          // back → next front
        j--;
    }
    nodes.get(i).next = null;                       // the new tail
}
```

**O(n) time, O(n) space.** It's simple and correct, and a fine first answer.

---

## 5. Approach 2: Middle + reverse + weave (the file's solution)

### Code ([M04_ReorderList.java:40-68](M04_ReorderList.java#L40-L68))

```java
static void reorderList(ListNode head) {
    if (head == null || head.next == null) return;
    // 1. first middle, then cut
    ListNode slow = head, fast = head;
    while (fast.next != null && fast.next.next != null) {
        slow = slow.next;
        fast = fast.next.next;
    }
    ListNode second = slow.next;
    slow.next = null;                               // cut: the first half ends here
    // 2. reverse the second half
    ListNode prev = null;
    while (second != null) {
        ListNode next = second.next;
        second.next = prev;
        prev = second;
        second = next;
    }
    second = prev;
    // 3. weave: first1 -> second1 -> first2 -> second2 ...
    ListNode first = head;
    while (second != null) {
        ListNode n1 = first.next, n2 = second.next;  // save both continuations
        first.next = second;
        second.next = n1;
        first = n1;
        second = n2;
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (head == null \|\| head.next == null) return;` | 0 or 1 nodes: nothing to do |
| the first-middle loop | slow stops at the **first** middle, so the first half is the same size or one longer |
| `second = slow.next; slow.next = null;` | cut into two lists |
| the reverse loop | the standard reversal; `prev` ends as the head of the reversed second half |
| `while (second != null)` | weave while the (shorter or equal) second half has nodes |
| `n1 = first.next, n2 = second.next` | save where each list continues |
| `first.next = second; second.next = n1;` | link first → second → (the next first) |
| `first = n1; second = n2;` | advance both |

### Dry run: `1 → 2 → 3 → 4 → 5`

**Step 1, the middle:** slow 1 → 2 → **3**; fast 1 → 3 → 5 (5.next is null → stop). Cut: first = `1 → 2 → 3`, second = `4 → 5`.

**Step 2, reverse:** second = `5 → 4`.

**Step 3, weave:**

| iteration | first | second | relink | result so far |
|---|---|---|---|---|
| 1 | 1 | 5 | 1 → 5 → 2 | `1 → 5 → 2 → 3`, `4` waiting |
| 2 | 2 | 4 | 2 → 4 → 3 | `1 → 5 → 2 → 4 → 3` |
| — | 3 | null | stop | |

**Result `1 → 5 → 2 → 4 → 3`** ✓ (node 3 keeps `next = null` from the cut, so it's the tail.)

### Dry run 2: `1 → 2 → 3 → 4`
Middle = 2 (the first middle): halves `1 → 2` and `3 → 4` → reverse → `4 → 3` → weave: 1 → 4 → 2 → 3 ✓

### Complexity
- **Time O(n):** three linear phases.
- **Space O(1).**

---

## 6. Comparing the approaches

| | Array of nodes | Middle + reverse + weave |
|---|---|---|
| Time | O(n) | O(n) |
| Extra space | O(n) | **O(1)** |
| Bug risk | low | medium (three phases) |

---

## 7. Edge cases and common mistakes

**Edge cases:** 1 node, 2 nodes, odd vs even length.

**Mistakes:**
1. Forgetting to **cut** (`slow.next = null`), which creates a cycle after the weave.
2. Using the second middle, so that the second half is longer than the first, and then breaking the weave loop logic.
3. Not saving both `next` pointers before relinking.
4. Weaving `while (first != null)`, which can read past the end of the second half.

---

## 8. Interview questions and answers

**Q1. Why reverse the second half?**
> We need the back nodes in front-to-back order of use: Ln first, then Ln−1… Reversing the second half turns "walk backwards from the tail" into "walk forwards", which a singly list can do.

**Q2. Which middle should you use, and why?**
> The first middle, so the first half is equal or one longer. Then the weave ends with the first half's last node, which is already the correct tail.

**Q3. What are the building blocks?**
> Fast/slow middle, in-place reversal, and an alternating merge.

### Follow-up questions

**F1. Palindrome Linked List.** → [E05](../02-Easy/E05_PalindromeLinkedList.md)
> The same first two steps, then **compare** instead of weaving.

**F2. Odd Even Linked List (LC 328).** → [M12](M12_OddEvenLinkedList.md)
> Another "split and reconnect" problem: odd positions first, then even.

**F3. Interleave in groups of k from the front and the back.**
> The same idea, taking k nodes from each half per round.

---

## 9. Running the file

```bash
java M04_ReorderList.java      # 5/5 tests passed (including lengths 0..25 vs the array version)
```

**One-line summary:** *"Cut at the first middle, reverse the second half, weave first/second alternately."*
