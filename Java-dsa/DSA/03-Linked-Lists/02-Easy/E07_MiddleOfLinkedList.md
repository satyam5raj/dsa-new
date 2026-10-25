# E07 · Middle of the Linked List

| | |
|---|---|
| **LeetCode** | [#876 Middle of the Linked List](https://leetcode.com/problems/middle-of-the-linked-list/) |
| **Difficulty** | Easy |
| **Pattern** | Fast & slow pointers |
| **Code** | [E07_MiddleOfLinkedList.java](E07_MiddleOfLinkedList.java) |
| **Run** | `java E07_MiddleOfLinkedList.java` |

---

## 1. The problem

Return the **middle node**. If there are two middles (even length), return the **second** one.

| list | middle | returned list (from the middle) |
|---|---|---|
| `1 → 2 → 3 → 4 → 5` | 3 | `[3, 4, 5]` |
| `1 → 2 → 3 → 4 → 5 → 6` | 4 (second middle) | `[4, 5, 6]` |
| `1` | 1 | `[1]` |
| `1 → 2` | 2 | `[2]` |

**Constraints:** `1 ≤ n ≤ 100`.

---

## 2. How to think about it

1. **Two passes:** count the length n, then walk n/2 steps. O(n), and perfectly fine.
2. **One pass with fast/slow:** fast moves 2 steps, slow moves 1. When fast can't move any further, slow has gone **half as far**, so it's at the middle.

Which middle you get depends on the **loop condition**:

| condition | n = 6 (1..6) gives | used for |
|---|---|---|
| `fast != null && fast.next != null` | 4 (the **second** middle) | this problem, palindrome check |
| `fast.next != null && fast.next.next != null` | 3 (the **first** middle) | **splitting** a list in half (merge sort, M14) |

---

## 3. Java concepts used in this problem

### 3.1 Two pointers moving at different speeds

```java
slow = slow.next;          // 1 step
fast = fast.next.next;     // 2 steps
```

After k iterations, slow is at position k and fast at position 2k. When fast reaches the end (about n), slow is at about n/2. The same "runner" idea detects cycles ([E03](E03_LinkedListCycle.md)) and finds the k-th node from the end ([M02](../03-Medium/M02_RemoveNthNodeFromEnd.md), with a fixed gap instead of a speed difference).

### 3.2 Returning a node returns the rest of the list

The method returns a `ListNode`; printing it shows the list **from that node on** (`[3, 4, 5]`), because `toString` follows `next` links.

---

## 4. Approach 1: Count, then walk

```java
static ListNode middleByCount(ListNode head) {
    int n = 0;
    for (ListNode c = head; c != null; c = c.next) n++;
    ListNode c = head;
    for (int i = 0; i < n / 2; i++) c = c.next;      // n/2 steps → the second middle for even n
    return c;
}
```

For n = 6, it takes 3 steps from node 1 → node 4 ✓. For n = 5, it takes 2 steps → node 3 ✓. **O(n), two passes.**

---

## 5. Approach 2: Fast & slow (the file's solution)

### Code ([E07_MiddleOfLinkedList.java:36-52](E07_MiddleOfLinkedList.java#L36-L52))

```java
static ListNode middleNode(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
    }
    return slow;
}

static ListNode firstMiddle(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast.next != null && fast.next.next != null) {
        slow = slow.next;
        fast = fast.next.next;
    }
    return slow;
}
```

### Line by line (`middleNode`)

| Line | What it does |
|---|---|
| `ListNode slow = head, fast = head;` | both start at the head |
| `while (fast != null && fast.next != null)` | fast can take two more steps |
| `slow = slow.next; fast = fast.next.next;` | slow 1, fast 2 |
| `return slow;` | the middle (the second middle for even n) |

`firstMiddle` stops one step earlier for even lengths, because it requires fast to have **two more nodes** ahead. (It assumes a non-empty list: `fast.next` on null would throw.)

### Dry run 1: `1 → 2 → 3 → 4 → 5 → 6` with `middleNode`

| iteration | slow | fast | continue? (fast ≠ null && fast.next ≠ null) |
|---|---|---|---|
| start | 1 | 1 | yes |
| 1 | 2 | 3 | yes |
| 2 | 3 | 5 | yes |
| 3 | **4** | null (past 6) | **no** |

**Return 4** (`[4, 5, 6]`) ✓

### Dry run 2: the same list with `firstMiddle`

| iteration | slow | fast | continue? (fast.next ≠ null && fast.next.next ≠ null) |
|---|---|---|---|
| start | 1 | 1 | yes (2, 3 exist) |
| 1 | 2 | 3 | yes (4, 5) |
| 2 | **3** | 5 | 6 exists, but 6.next is null → **no** |

**Return 3** ✓

### Dry run 3: `1 → 2 → 3 → 4 → 5` with `middleNode`
slow: 1 → 2 → 3; fast: 1 → 3 → 5, and `5.next` is null → stop. **Return 3** ✓

### Complexity
- **Time O(n)** (one pass, n/2 iterations), **Space O(1).**

---

## 6. Comparing the approaches

| | Count + walk | Fast & slow |
|---|---|---|
| Time | O(n), 1.5 passes | O(n), 1 pass |
| Space | O(1) | O(1) |
| Works on a stream / unknown length | no | **yes** |

---

## 7. Edge cases and common mistakes

**Edge cases:** one node, two nodes, odd vs even length.

**Mistakes:**
1. Using the wrong loop condition and getting the wrong middle for even lengths.
2. Calling `firstMiddle` on an empty list (NPE).
3. Checking only `fast.next != null`, which skips the null check on fast itself.

---

## 8. Interview questions and answers

**Q1. Why does fast/slow find the middle?**
> Fast covers twice the distance of slow. When fast reaches the end, slow has covered half the list.

**Q2. How do you get the first middle instead of the second?**
> Change the loop condition to `fast.next != null && fast.next.next != null`, so it stops one iteration earlier on even lengths.

### Follow-up questions

**F1. Split a list into two halves (for merge sort).** → [M14 Sort List](../03-Medium/M14_SortList.md)
> Use the first middle, then `second = mid.next; mid.next = null;`.

**F2. Delete the middle node (LC 2095).**
> Stop slow one node **before** the middle (start fast one step ahead, or keep a `prev`), then unlink it.

**F3. Find the node at one third / the k-th from the end.**
> One third: move fast 3 steps per slow step. The k-th from the end: give fast a head start of k nodes ([M02](../03-Medium/M02_RemoveNthNodeFromEnd.md)).

---

## 9. Running the file

```bash
java E07_MiddleOfLinkedList.java      # 5/5 tests passed
```

**One-line summary:** *"Fast 2, slow 1: when fast can't continue, slow is at the middle; the loop condition decides which middle."*
