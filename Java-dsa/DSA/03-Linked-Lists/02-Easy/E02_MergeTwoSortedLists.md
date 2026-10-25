# E02 · Merge Two Sorted Lists

| | |
|---|---|
| **LeetCode** | [#21 Merge Two Sorted Lists](https://leetcode.com/problems/merge-two-sorted-lists/) |
| **Difficulty** | Easy |
| **Pattern** | Dummy head + tail pointer (splice nodes, don't copy) |
| **Code** | [E02_MergeTwoSortedLists.java](E02_MergeTwoSortedLists.java) |
| **Run** | `java E02_MergeTwoSortedLists.java` |

---

## 1. The problem

Merge two **sorted** lists into one sorted list by **splicing** their nodes together (reuse the existing nodes; don't create new ones).

| a | b | merged |
|---|---|---|
| `1 → 2 → 4` | `1 → 3 → 4` | `1 → 1 → 2 → 3 → 4 → 4` |
| `[]` | `[]` | `[]` |
| `[]` | `0` | `0` |
| `5` | `1 → 2 → 3` | `1 → 2 → 3 → 5` |

**Constraints:** `0 ≤ lengths ≤ 50`, both sorted in non-decreasing order.

---

## 2. How to think about it

It's the **merge** step of merge sort (see [Arrays E08](../../01-Arrays/02-Easy/E08_MergeSortedArray.md)), but on lists it's easier: there's no "overwriting" problem, because we just **relink** nodes.

### The dummy head trick
We don't know in advance which list gives the first node. Without a dummy, you'd need special code for "the merged list is still empty". A **dummy** node gives a fixed starting point, and `tail` always points at the **last node of the merged list so far**:

```
dummy → (merged so far) → tail
```

- While both lists have nodes: attach the **smaller** head to `tail.next`, advance that list, advance `tail`.
- When one list runs out, attach the **rest of the other in one step** (it's already sorted).
- Return `dummy.next`.

---

## 3. Java concepts used in this problem

### 3.1 A dummy (sentinel) node

```java
ListNode dummy = new ListNode(0), tail = dummy;
...
return dummy.next;     // skip the fake node
```

The dummy's value is never used; it only exists so that `tail.next = ...` works even before the first real node is attached. This pattern removes many `if (head == null)` special cases. (See the `build` helper in [E01](E01_ReverseLinkedList.md).)

### 3.2 Splicing versus copying

`tail.next = a;` doesn't copy node `a`. It points the merged list at the **existing** node. The lists are taken apart and reassembled. That's why the problem says "splice", and why the extra space is O(1).

### 3.3 The ternary for "whichever is non-null"

```java
tail.next = (a != null) ? a : b;
```

At most one list still has nodes; attach it (or `null` if both are empty).

### 3.4 `<=` for stability

On equal values, taking from `a` first keeps the nodes in their original relative order (a **stable** merge). That matters when the nodes carry more data than their value.

---

## 4. Approach 1: Collect, sort, rebuild (not splicing)

```java
static ListNode mergeBySorting(ListNode a, ListNode b) {
    List<Integer> vals = new ArrayList<>();
    for (ListNode c = a; c != null; c = c.next) vals.add(c.val);
    for (ListNode c = b; c != null; c = c.next) vals.add(c.val);
    Collections.sort(vals);
    ListNode dummy = new ListNode(0), t = dummy;
    for (int v : vals) { t.next = new ListNode(v); t = t.next; }
    return dummy.next;
}
```

**O((n + m) log(n + m)) time, O(n + m) space.** It ignores that the lists are already sorted and creates new nodes.

---

## 5. Approach 2: Iterative merge with a dummy (the file's main solution)

### Code ([E02_MergeTwoSortedLists.java:42-56](E02_MergeTwoSortedLists.java#L42-L56))

```java
static ListNode mergeTwoLists(ListNode a, ListNode b) {
    ListNode dummy = new ListNode(0), tail = dummy;
    while (a != null && b != null) {
        if (a.val <= b.val) {
            tail.next = a;
            a = a.next;
        } else {
            tail.next = b;
            b = b.next;
        }
        tail = tail.next;
    }
    tail.next = (a != null) ? a : b;       // attach the rest in one step
    return dummy.next;
}
```

### Line by line

| Line | What it does |
|---|---|
| `ListNode dummy = new ListNode(0), tail = dummy;` | an empty merged list; `tail` is its last node |
| `while (a != null && b != null)` | both lists still have candidates |
| `if (a.val <= b.val) { tail.next = a; a = a.next; }` | the smaller head goes next; advance in that list |
| `else { tail.next = b; b = b.next; }` | otherwise take from b |
| `tail = tail.next;` | the node just attached is the new tail |
| `tail.next = (a != null) ? a : b;` | attach whatever remains, already sorted |
| `return dummy.next;` | the real head |

### Dry run: `a = 1 → 2 → 4`, `b = 1 → 3 → 4`

| step | a | b | compare | attach | merged so far |
|---|---|---|---|---|---|
| 1 | 1 | 1 | 1 ≤ 1 | a's 1 | 1 |
| 2 | 2 | 1 | 2 > 1 | b's 1 | 1 → 1 |
| 3 | 2 | 3 | 2 ≤ 3 | 2 | 1 → 1 → 2 |
| 4 | 4 | 3 | 4 > 3 | 3 | … → 3 |
| 5 | 4 | 4 | 4 ≤ 4 | a's 4 | … → 4 |
| end | null | 4 | a is empty | attach the rest of b | … → 4 → **4** |

**Result `1 → 1 → 2 → 3 → 4 → 4`** ✓

### Dry run 2: `a = 5`, `b = 1 → 2 → 3`
5 > 1, 5 > 2, 5 > 3 → attach 1, 2, 3; b runs out → attach `5` → `1 → 2 → 3 → 5` ✓

### Complexity
- **Time O(n + m)**, **Space O(1)** (only pointers; the nodes are reused).

---

## 6. Approach 3: Recursive merge (the file's second method)

### Code ([E02_MergeTwoSortedLists.java:58-67](E02_MergeTwoSortedLists.java#L58-L67))

```java
static ListNode mergeRecursive(ListNode a, ListNode b) {
    if (a == null) return b;
    if (b == null) return a;
    if (a.val <= b.val) {
        a.next = mergeRecursive(a.next, b);
        return a;
    }
    b.next = mergeRecursive(a, b.next);
    return b;
}
```

| Line | What it does |
|---|---|
| `if (a == null) return b;` / `if (b == null) return a;` | base cases: one list is empty, so the answer is the other |
| `if (a.val <= b.val) { a.next = merge(a.next, b); return a; }` | a's head is the smallest, so it comes first, followed by the merge of everything else |
| `b.next = merge(a, b.next); return b;` | the symmetric case |

**Dry run** (the same input): `merge(1a, 1b)` → returns 1a with `1a.next = merge(2, 1b)` → returns 1b with `1b.next = merge(2, 3)` → … and the results build up as the calls return. Final: `1 → 1 → 2 → 3 → 4 → 4`.

**O(n + m) time, O(n + m) stack space.** It's elegant, but deep recursion on long lists can overflow the stack.

---

## 7. Comparing the approaches

| | Sort + rebuild | Iterative | Recursive |
|---|---|---|---|
| Time | O((n+m) log(n+m)) | **O(n + m)** | O(n + m) |
| Extra space | O(n + m) | **O(1)** | O(n + m) stack |
| Reuses the nodes | no | **yes** | yes |

---

## 8. Edge cases and common mistakes

**Edge cases:** both empty, one empty, one list entirely smaller than the other, duplicates across the lists.

**Mistakes:**
1. Forgetting to advance `tail`, which overwrites the same `next` again and again.
2. Looping until **both** lists are empty and then comparing with null values (an NPE). Attach the leftover in one step instead.
3. Returning `dummy` instead of `dummy.next`.
4. Creating new nodes when the problem asks for splicing.

---

## 9. Interview questions and answers

**Q1. Why use a dummy node?**
> It removes the special case of choosing the head: the merged list always has a node to attach to. At the end, `dummy.next` is the real head.

**Q2. Why can you attach the leftover list in one step?**
> It's already sorted, and every remaining node is ≥ everything already merged.

**Q3. Iterative or recursive?**
> Iterative: O(1) space, and no risk of stack overflow. The recursive version is shorter but uses O(n + m) stack.

### Follow-up questions

**F1. Merge k sorted lists (LC 23).** → [H02](../04-Hard/H02_MergeKSortedLists.md)
> A min-heap of the k heads (O(N log k)), or merge pairs of lists in rounds (divide and conquer, also O(N log k)).

**F2. Sort a linked list (LC 148).** → [M14 Sort List](../03-Medium/M14_SortList.md)
> Merge sort: split at the middle (fast/slow), sort each half, merge with this function.

**F3. Merge into descending order.**
> Merge ascending and then reverse ([E01](E01_ReverseLinkedList.md)), or merge by prepending each chosen node to the result.

**F4. Merge Sorted Array.** → [Arrays E08](../../01-Arrays/02-Easy/E08_MergeSortedArray.md)
> The array version, done from the back to avoid overwriting.

---

## 10. Running the file

```bash
java E02_MergeTwoSortedLists.java      # 5/5 tests passed
```

**One-line summary:** *"Dummy + tail: attach the smaller head each time, then attach the leftover list in one step."*
