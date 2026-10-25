# H01 · Reverse Nodes in k-Group

| | |
|---|---|
| **LeetCode** | [#25 Reverse Nodes in k-Group](https://leetcode.com/problems/reverse-nodes-in-k-group/) |
| **Difficulty** | Hard |
| **Pattern** | Check k nodes ahead → reverse the group → reconnect → repeat |
| **Code** | [H01_ReverseNodesInKGroup.java](H01_ReverseNodesInKGroup.java) |
| **Run** | `java H01_ReverseNodesInKGroup.java` |

---

## 1. The problem

Reverse the nodes **k at a time**. A final group with **fewer than k** nodes stays as it is. Relink the nodes (don't change their values), using **O(1) extra space**.

| list | k | result |
|---|---|---|
| `1 → 2 → 3 → 4 → 5` | 2 | `2 → 1 → 4 → 3 → 5` |
| `1 → 2 → 3 → 4 → 5` | 3 | `3 → 2 → 1 → 4 → 5` |
| `1 → 2 → 3 → 4` | 4 | `4 → 3 → 2 → 1` |
| `1 → 2 → 3` | 1 | `1 → 2 → 3` |
| `1 → 2` | 3 | `1 → 2` (too short) |

**Constraints:** `1 ≤ k ≤ n ≤ 5000`.

---

## 2. How to think about it

You already have every building block:
- reverse a list ([E01](../02-Easy/E01_ReverseLinkedList.md)) and a sublist ([M13](../03-Medium/M13_ReverseLinkedListII.md))
- [Swap Nodes in Pairs (M08)](../03-Medium/M08_SwapNodesInPairs.md) is exactly this problem with k = 2

### The algorithm (iterative, O(1) space)
```
dummy → head;  groupPrev = dummy        (the node BEFORE the current group)
loop:
  1. kth = the k-th node after groupPrev. If it doesn't exist → stop
  2. groupNext = kth.next                (the first node AFTER the group)
  3. reverse the group's nodes, starting `prev` at groupNext (not null!),
     so the group's new tail automatically links to the rest of the list
  4. reconnect the front:
       first = groupPrev.next            (the old first node, now the group's tail)
       groupPrev.next = kth              (kth is the new first node)
       groupPrev = first                 (the tail is the next group's "prev")
```

**The neat trick in step 3:** initialising the reversal's `prev` to `groupNext` means the old first node (which becomes the last of the reversed group) ends up pointing to `groupNext`. The back connection is made for free.

---

## 3. Java concepts used in this problem

### 3.1 `while (true)` with a `break`

```java
while (true) {
    ...find kth...
    if (kth == null) break;   // the exit condition is computed in the middle of the loop
    ...
}
```

When the "should we continue?" test can only be evaluated partway through an iteration, `while (true)` + `break` is clearer than duplicating the code before the loop.

### 3.2 A `for` loop with two conditions

```java
for (int i = 0; i < k && kth != null; i++) kth = kth.next;
```

It stops after k steps **or** when the list runs out, whichever comes first. If it ran out, `kth` is null and the group is incomplete.

### 3.3 Reversal bounded by a sentinel node

```java
while (cur != groupNext) { ... }
```

Instead of reversing "until null", we reverse "until we reach the node after the group". Any node can serve as the stopping point.

---

## 4. Approach 1: Recursive

```java
static ListNode reverseKGroupRec(ListNode head, int k) {
    ListNode check = head;
    for (int i = 0; i < k; i++) {               // are there k nodes?
        if (check == null) return head;         // no: leave this part as it is
        check = check.next;
    }
    ListNode prev = reverseKGroupRec(check, k); // the rest, already processed
    ListNode cur = head;
    for (int i = 0; i < k; i++) {               // reverse this group onto it
        ListNode next = cur.next;
        cur.next = prev;
        prev = cur;
        cur = next;
    }
    return prev;                                // the new head of this group
}
```

It's elegant: process the rest first, then reverse this group **onto** the result (the same "prev starts at the next part" trick). **O(n) time, O(n/k) stack.**

---

## 5. Approach 2: Iterative, O(1) space (the file's solution)

### Code ([H01_ReverseNodesInKGroup.java:45-66](H01_ReverseNodesInKGroup.java#L45-L66))

```java
static ListNode reverseKGroup(ListNode head, int k) {
    ListNode dummy = new ListNode(0, head);
    ListNode groupPrev = dummy;
    while (true) {
        ListNode kth = groupPrev;                         // 1. find the k-th node
        for (int i = 0; i < k && kth != null; i++) kth = kth.next;
        if (kth == null) break;                           //    fewer than k left

        ListNode groupNext = kth.next;                    // 2.
        ListNode prev = groupNext, cur = groupPrev.next;  // 3. reverse, tail -> groupNext
        while (cur != groupNext) {
            ListNode next = cur.next;
            cur.next = prev;
            prev = cur;
            cur = next;
        }
        ListNode first = groupPrev.next;                  // 4. reconnect
        groupPrev.next = kth;
        groupPrev = first;
    }
    return dummy.next;
}
```

### Line by line

| Line | What it does |
|---|---|
| `dummy → head; groupPrev = dummy` | the first group's predecessor |
| `kth` loop | walk k steps from groupPrev; null means there's no full group |
| `ListNode groupNext = kth.next;` | where the group ends |
| `prev = groupNext, cur = groupPrev.next` | reverse the group's nodes; the first one will point to groupNext |
| `while (cur != groupNext)` | the standard reversal, limited to this group |
| `first = groupPrev.next` | (still the old first node, because `groupPrev.next` wasn't changed yet) |
| `groupPrev.next = kth;` | the predecessor now points to the new first node |
| `groupPrev = first;` | the old first node is the new tail, which is the predecessor of the next group |

### Dry run: `1 → 2 → 3 → 4 → 5`, k = 2

| group | groupPrev | kth | groupNext | after the reversal + reconnect | new groupPrev |
|---|---|---|---|---|---|
| [1, 2] | D | 2 | 3 | D → **2 → 1** → 3 → 4 → 5 | 1 |
| [3, 4] | 1 | 4 | 5 | D → 2 → 1 → **4 → 3** → 5 | 3 |
| [5] | 3 | null (only 1 node) | — | stop | |

**Result `2 → 1 → 4 → 3 → 5`** ✓

Inside group [1, 2]: prev = 3 (groupNext), cur = 1 → `1.next = 3`, prev = 1, cur = 2 → `2.next = 1`, prev = 2, cur = 3 = groupNext → stop. So `2 → 1 → 3`, already connected to the rest.

### Dry run 2: k = 3
Group [1, 2, 3] → `3 → 2 → 1 → 4 → 5`; then only 2 nodes remain (< 3) → stop. **`3 → 2 → 1 → 4 → 5`** ✓

### Complexity
- **Time O(n):** each node is visited twice (once while finding kth, once while reversing).
- **Space O(1).** The file checks **every** n ≤ 20 with every k ≤ n + 1 against an array version.

---

## 6. Comparing the approaches

| | Recursive | Iterative |
|---|---|---|
| Time | O(n) | O(n) |
| Space | O(n/k) stack | **O(1)** |
| Meets the follow-up | no | **yes** |

---

## 7. Edge cases and common mistakes

**Edge cases:** k = 1 (no change), k = n (reverse everything), n < k (no change), n a multiple of k, a leftover group shorter than k.

**Mistakes:**
1. Reversing the final short group too (it must stay as it is).
2. Starting the reversal's `prev` at null, which disconnects the group from the rest.
3. Reading `first = groupPrev.next` **after** changing `groupPrev.next` (you'd get kth).
4. Forgetting the dummy, so the head can't change.

---

## 8. Interview questions and answers

**Q1. Walk me through one group.**
> Find the k-th node; if it's missing, stop. Remember the node after the group, reverse the group with `prev` starting at that node so the tail connects automatically, then point the previous group's tail at the new head (the old k-th node), and advance groupPrev to the new tail (the old first node).

**Q2. Why initialise `prev` to `groupNext` instead of null?**
> The first node of the group becomes its last, and it should point to the rest of the list. Starting `prev` at `groupNext` makes that link as part of the normal reversal.

**Q3. What's the complexity?**
> O(n) time: each node is counted once and reversed once. O(1) extra space.

### Follow-up questions

**F1. Also reverse the final short group.**
> Remove the "fewer than k" stop: reverse whatever remains.

**F2. Reverse alternate groups of k (reverse k, skip k).**
> After reconnecting a reversed group, walk groupPrev k more steps without reversing.

**F3. Reverse groups from the end (the remainder at the front).**
> Compute the length n; skip `n % k` nodes first, then run the same algorithm.

---

## 9. Running the file

```bash
java H01_ReverseNodesInKGroup.java      # 6/6 tests passed (including every n ≤ 20, k ≤ n + 1)
```

**One-line summary:** *"Find kth; reverse the group with prev = groupNext; groupPrev.next = kth; groupPrev = the old first node."*
