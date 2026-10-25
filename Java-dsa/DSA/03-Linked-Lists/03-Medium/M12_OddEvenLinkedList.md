# M12 · Odd Even Linked List

| | |
|---|---|
| **LeetCode** | [#328 Odd Even Linked List](https://leetcode.com/problems/odd-even-linked-list/) |
| **Difficulty** | Medium |
| **Pattern** | Two interleaved chains, then join |
| **Code** | [M12_OddEvenLinkedList.java](M12_OddEvenLinkedList.java) |
| **Run** | `java M12_OddEvenLinkedList.java` |

---

## 1. The problem

Group the nodes at **odd positions** (1st, 3rd, 5th…) first, then the nodes at **even positions**, keeping the order inside each group. This is about **positions, not values**! O(1) extra space, O(n) time.

| list | result |
|---|---|
| `1 → 2 → 3 → 4 → 5` | `1 → 3 → 5 → 2 → 4` |
| `2 → 1 → 3 → 5 → 6 → 4 → 7` | `2 → 3 → 6 → 7 → 1 → 5 → 4` |
| `1` | `1` |
| `1 → 2` | `1 → 2` |

**Constraints:** `0 ≤ n ≤ 10⁴`.

---

## 2. How to think about it

The list alternates odd, even, odd, even… **Unzip it like a zipper** into two chains, then attach the even chain after the odd chain:

```
odd = head, even = head.next, evenHead = even     (remember where the evens start)
while even != null && even.next != null:
    odd.next  = even.next;  odd  = odd.next       (skip over an even node)
    even.next = odd.next;   even = even.next      (skip over an odd node)
odd.next = evenHead                               (the evens go after the odds)
```

It's the same family as [Partition List (M07)](M07_PartitionList.md): split into two chains and join. Here the split is by **position**, and it's done without dummies, because the first node of each chain is known (head and head.next).

---

## 3. Java concepts used in this problem

### 3.1 Remembering a head before you lose it

`evenHead = even` saves the start of the even chain **before** the loop rewires everything. At the end it's the only way to reach the even chain.

### 3.2 Leapfrogging pointers

`odd` and `even` take turns: each jumps **over** the other's node. Each assignment overwrites a `next` that was pointing at a node of the other parity.

### 3.3 Why the loop condition is about `even`

`even != null && even.next != null` means "there's another odd node after the current even node". If even is null (odd length) or `even.next` is null (even length), the odd chain has no more nodes to take.

---

## 4. Approach 1: Collect by index, rebuild values

```java
static ListNode oddEvenValues(ListNode head) {
    List<Integer> vals = new ArrayList<>();
    for (ListNode c = head; c != null; c = c.next) vals.add(c.val);
    ListNode c = head;
    for (int i = 0; i < vals.size(); i += 2) { c.val = vals.get(i); c = c.next; }   // positions 1, 3, 5... (0-based even)
    for (int i = 1; i < vals.size(); i += 2) { c.val = vals.get(i); c = c.next; }
    return head;
}
```

**O(n) time, O(n) space.** It breaks the O(1) space requirement, and it changes values instead of nodes.

---

## 5. Approach 2: The zipper (the file's solution)

### Code ([M12_OddEvenLinkedList.java:38-49](M12_OddEvenLinkedList.java#L38-L49))

```java
static ListNode oddEvenList(ListNode head) {
    if (head == null) return null;
    ListNode odd = head, even = head.next, evenHead = even;
    while (even != null && even.next != null) {
        odd.next = even.next;
        odd = odd.next;
        even.next = odd.next;
        even = even.next;
    }
    odd.next = evenHead;
    return head;
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (head == null) return null;` | an empty list |
| `odd = head, even = head.next, evenHead = even` | the two chains' current tails, plus the even chain's head |
| `odd.next = even.next; odd = odd.next;` | the next odd node is right after the current even node |
| `even.next = odd.next; even = even.next;` | the next even node is right after the new odd node |
| `odd.next = evenHead;` | glue the even chain after the last odd node |
| `return head;` | the head is unchanged (position 1 is odd) |

### Dry run: `1 → 2 → 3 → 4 → 5`

| iteration | odd | even | odd chain | even chain |
|---|---|---|---|---|
| start | 1 | 2 | 1 | 2 |
| 1 | 3 | 4 | 1 → 3 | 2 → 4 |
| 2 | 5 | null (4.next = 5.next = null) | 1 → 3 → 5 | 2 → 4 |
| — | `even == null` → stop | | | |

Join: `5.next = evenHead (2)` → **`1 → 3 → 5 → 2 → 4`** ✓

### Dry run 2: `1 → 2` (even length)
odd = 1, even = 2; `even.next` is null → the loop doesn't run → `1.next = 2` (unchanged) → **`1 → 2`** ✓

### Complexity
- **Time O(n)**, **Space O(1).** The file checks every length from 1 to 20 against the array version.

---

## 6. Comparing the approaches

| | Values by index | Zipper |
|---|---|---|
| Time | O(n) | O(n) |
| Extra space | O(n) | **O(1)** |
| Relinks the nodes | no | **yes** |

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty list, 1 node, 2 nodes, odd vs even length.

**Mistakes:**
1. Splitting by **value** parity instead of **position**.
2. Not saving `evenHead`, which loses the even chain.
3. A wrong loop condition, causing an NPE on `even.next` when even is null.

---

## 8. Interview questions and answers

**Q1. Why don't you need dummy nodes here?**
> Both chains' first nodes are known up front (head and head.next), and the head never changes, so there's nothing to special-case.

**Q2. Why does the even chain end with null automatically?**
> The last update `even.next = odd.next` copies null when the odd node was the last one; or, for even length, the last even node's `next` was already null.

### Follow-up questions

**F1. Partition by value.** → [M07](M07_PartitionList.md)

**F2. Split a list into k round-robin lists (node i goes to list i mod k).**
> k tail pointers (with k dummies), appending node i to chain `i % k`.

**F3. Reverse the even-positioned nodes, then merge.**
> Unzip, reverse the even chain ([E01](../02-Easy/E01_ReverseLinkedList.md)), then weave as in [M04](M04_ReorderList.md).

---

## 9. Running the file

```bash
java M12_OddEvenLinkedList.java      # 5/5 tests passed (including lengths 1..20)
```

**One-line summary:** *"Leapfrog odd and even pointers to unzip the list, then attach the saved even head after the last odd node."*
