# M08 · Swap Nodes in Pairs

| | |
|---|---|
| **LeetCode** | [#24 Swap Nodes in Pairs](https://leetcode.com/problems/swap-nodes-in-pairs/) |
| **Difficulty** | Medium |
| **Pattern** | Dummy head + rewire three pointers per pair (or recursion) |
| **Code** | [M08_SwapNodesInPairs.java](M08_SwapNodesInPairs.java) |
| **Run** | `java M08_SwapNodesInPairs.java` |

---

## 1. The problem

Swap every two adjacent **nodes** (relink them; don't swap the values).

| list | result |
|---|---|
| `1 → 2 → 3 → 4` | `2 → 1 → 4 → 3` |
| `1 → 2 → 3` | `2 → 1 → 3` (an odd node stays put) |
| `1` | `1` |
| `[]` | `[]` |

**Constraints:** `0 ≤ n ≤ 100`.

---

## 2. How to think about it

### Iterative
A pointer `prev` sits **before** each pair (it starts at a dummy):

```
prev → first → second → rest          becomes          prev → second → first → rest
```

Three assignments, **in this order**:
1. `first.next = second.next`: first now points to the rest
2. `second.next = first`: second points back to first
3. `prev.next = second`: connect the pair to what's before it

Then `prev = first` (first is now the **second** node of the pair, right before the next pair).

### Recursive (very clean)
```
swap(head): if there are fewer than 2 nodes → return head
            second = head.next
            head.next = swap(second.next)   // the rest, already swapped
            second.next = head
            return second
```

Generalisation: [Reverse Nodes in k-Group](../04-Hard/H01_ReverseNodesInKGroup.md) is this problem with k instead of 2.

---

## 3. Java concepts used in this problem

### 3.1 Why the order of the three assignments matters

Each assignment overwrites a `next` pointer. If you overwrite one before reading what you still need, you lose part of the list. For example, doing `second.next = first` first would lose `second.next` (the rest of the list). Draw boxes and arrows, and only change an arrow once its old target is saved or no longer needed.

### 3.2 Declaring two variables from each other

```java
ListNode first = prev.next, second = first.next;
```

Declarations are evaluated left to right, so `second` can use the just-initialised `first`.

### 3.3 Swapping values (not allowed here)

Swapping `first.val` and `second.val` would be much simpler, but the problem forbids it. In real code, nodes often carry big payloads or are referenced elsewhere, so you relink them instead of copying their contents.

---

## 4. Approach 1: Iterative with a dummy (the file's main solution)

### Code ([M08_SwapNodesInPairs.java:44-54](M08_SwapNodesInPairs.java#L44-L54))

```java
static ListNode swapPairs(ListNode head) {
    ListNode dummy = new ListNode(0, head), prev = dummy;
    while (prev.next != null && prev.next.next != null) {
        ListNode first = prev.next, second = first.next;
        first.next = second.next;
        second.next = first;
        prev.next = second;
        prev = first;
    }
    return dummy.next;
}
```

### Line by line

| Line | What it does |
|---|---|
| `ListNode dummy = new ListNode(0, head), prev = dummy;` | the first pair's predecessor is the dummy, so the head swap isn't a special case |
| `while (prev.next != null && prev.next.next != null)` | there's a complete pair after prev |
| `first.next = second.next;` | first → the rest |
| `second.next = first;` | second → first |
| `prev.next = second;` | the predecessor → second |
| `prev = first;` | first is now the last node of the swapped pair |
| `return dummy.next;` | the new head (the old second node) |

### Dry run: `1 → 2 → 3 → 4`

| pair | before | after the 3 assignments | prev becomes |
|---|---|---|---|
| (1, 2) | D → 1 → 2 → 3 → 4 | D → 2 → 1 → 3 → 4 | 1 |
| (3, 4) | 1 → 3 → 4 → null | 1 → 4 → 3 → null | 3 |
| — | prev.next is null → stop | | |

**Result `2 → 1 → 4 → 3`** ✓

### Dry run 2: `1 → 2 → 3`
Swap (1, 2) → D → 2 → 1 → 3; prev = 1; `prev.next.next` is null → stop. **Result `2 → 1 → 3`** ✓

### Complexity
- **Time O(n)**, **Space O(1).**

---

## 5. Approach 2: Recursive (the file's second method)

### Code ([M08_SwapNodesInPairs.java:56-62](M08_SwapNodesInPairs.java#L56-L62))

```java
static ListNode swapPairsRecursive(ListNode head) {
    if (head == null || head.next == null) return head;
    ListNode second = head.next;
    head.next = swapPairsRecursive(second.next);
    second.next = head;
    return second;
}
```

| Line | What it does |
|---|---|
| `if (head == null \|\| head.next == null) return head;` | 0 or 1 nodes: nothing to swap |
| `ListNode second = head.next;` | the partner |
| `head.next = swapPairsRecursive(second.next);` | head (which becomes the second node of the pair) points to the swapped rest |
| `second.next = head;` | second comes first |
| `return second;` | the new head of this part |

### Dry run: `1 → 2 → 3 → 4 → 5`

| call | head | second | recursive result | returns |
|---|---|---|---|---|
| swap(5) | 5 | — | base case | 5 |
| swap(3) | 3 | 4 | swap(5) = 5 → `3 → 5` | `4 → 3 → 5` |
| swap(1) | 1 | 2 | `4 → 3 → 5` → `1 → 4 …` | `2 → 1 → 4 → 3 → 5` |

**Result `2 → 1 → 4 → 3 → 5`** ✓

**O(n) time, O(n) stack** (n/2 frames).

---

## 6. Comparing the approaches

| | Iterative | Recursive |
|---|---|---|
| Time | O(n) | O(n) |
| Space | **O(1)** | O(n) stack |
| Readability | needs a drawing | **very short** |

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty list, one node, odd length, two nodes.

**Mistakes:**
1. Assigning in the wrong order and losing the rest of the list.
2. Forgetting `prev.next = second`, so the previous pair still points to the old first node (and nodes are lost).
3. Setting `prev = second` instead of `prev = first`.
4. Swapping values instead of nodes (not allowed).

---

## 8. Interview questions and answers

**Q1. Why use a dummy?**
> The first pair's swap changes the head. With a dummy, every pair has a predecessor whose `next` gets updated in the same way.

**Q2. Which pointers change per pair?**
> Three: `first.next`, `second.next`, and `prev.next`.

### Follow-up questions

**F1. Reverse Nodes in k-Group (LC 25).** → [H01](../04-Hard/H01_ReverseNodesInKGroup.md)
> Check that k nodes exist, reverse those k with the standard loop, connect them to the previous group, and continue.

**F2. Swap the k-th node from the start with the k-th from the end (LC 1721).**
> Find both with the fixed-gap technique ([M02](M02_RemoveNthNodeFromEnd.md)) and swap them (values or links).

**F3. Swap pairs in a doubly linked list.**
> The same idea, also fixing four `prev` pointers per pair.

---

## 9. Running the file

```bash
java M08_SwapNodesInPairs.java      # 5/5 tests passed
```

**One-line summary:** *"prev → first → second → rest becomes prev → second → first → rest (three assignments), then prev = first."*
