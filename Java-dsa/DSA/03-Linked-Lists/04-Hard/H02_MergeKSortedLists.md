# H02 · Merge k Sorted Lists

| | |
|---|---|
| **LeetCode** | [#23 Merge k Sorted Lists](https://leetcode.com/problems/merge-k-sorted-lists/) |
| **Difficulty** | Hard |
| **Pattern** | Min-heap of list heads, or divide-and-conquer pairwise merging |
| **Code** | [H02_MergeKSortedLists.java](H02_MergeKSortedLists.java) |
| **Run** | `java H02_MergeKSortedLists.java` |

---

## 1. The problem

Merge **k** sorted lists into one sorted list. Let **N** be the total number of nodes.

| lists | result |
|---|---|
| `[1→4→5, 1→3→4, 2→6]` | `1→1→2→3→4→4→5→6` |
| `[]` (no lists) | `[]` |
| `[[]]` (one empty list) | `[]` |

**Constraints:** `0 ≤ k ≤ 10⁴`, each list has up to 500 nodes, N ≤ 10⁴.

---

## 2. How to think about it

### Approach 1: merge one by one
`result = merge(result, list_i)` for each list. The growing result is re-scanned every time: about N/k + 2N/k + … ≈ **O(k · N)**. That's too slow for big k.

### Approach 2: a min-heap (PriorityQueue)
The next smallest node overall is **always one of the k current heads**. Keep those heads in a **min-heap**:
- push every non-null head
- while the heap isn't empty: `poll()` the smallest head and append it to the result; if it has a `next`, push that (it's the new head of its list)

Each of the N nodes is pushed and popped once, at O(log k) each → **O(N log k)**, with O(k) space.

### Approach 3: divide and conquer
Merge the lists **in pairs**, like the merge step of merge sort:
- round 1: (L0 + L1), (L2 + L3), … → k/2 lists
- round 2: pairs again → k/4 lists … → 1 list

There are log k rounds, and each touches all N nodes once → **O(N log k)**, with no heap needed.

---

## 3. Java concepts used in this problem

### 3.1 `PriorityQueue`: a binary heap

```java
PriorityQueue<ListNode> heap = new PriorityQueue<>(Comparator.comparingInt(n -> n.val));
heap.offer(node);     // add: O(log size)
heap.poll();          // remove and return the SMALLEST: O(log size)
heap.peek();          // look at the smallest: O(1)
heap.isEmpty();
```

- A **min-heap** keeps the smallest element at the top. Internally it's an array arranged as a complete binary tree in which every parent ≤ its children.
- It only guarantees access to the **minimum**, not a fully sorted order. Iterating over a PriorityQueue does **not** give sorted output.
- With **equal** keys, the order is unspecified (the heap isn't stable). That's fine here.

### 3.2 `Comparator.comparingInt(n -> n.val)`

`ListNode` has no natural ordering, so we tell the heap **how to compare**: by the `val` field. `comparingInt` takes a function that extracts an `int` key and builds a `Comparator` from it. Equivalent forms:

```java
new PriorityQueue<>((a, b) -> Integer.compare(a.val, b.val));
new PriorityQueue<>((a, b) -> a.val - b.val);     // works here, but can overflow for extreme values
```

(Heaps get their own topic later: [08-Heap](../../08-Heap/README.md).)

### 3.3 Recursion over an index range (divide and conquer)

```java
mergeRange(lists, lo, hi) = mergeTwo(mergeRange(lo, mid), mergeRange(mid + 1, hi))
```

It's the same shape as merge sort, but each "element" is a whole list.

---

## 4. Approach 1: Merge one by one

```java
static ListNode mergeOneByOne(ListNode[] lists) {
    ListNode result = null;
    for (ListNode l : lists) result = mergeTwo(result, l);
    return result;
}
```

It's simple, but **O(k · N)**: list i's nodes get re-merged (k − i) times.

---

## 5. Approach 2: Min-heap (the file's main solution)

### Code ([H02_MergeKSortedLists.java:47-59](H02_MergeKSortedLists.java#L47-L59))

```java
static ListNode mergeKLists(ListNode[] lists) {
    PriorityQueue<ListNode> heap = new PriorityQueue<>(Comparator.comparingInt(n -> n.val));
    for (ListNode head : lists) if (head != null) heap.offer(head);

    ListNode dummy = new ListNode(0), tail = dummy;
    while (!heap.isEmpty()) {
        ListNode smallest = heap.poll();
        tail.next = smallest;
        tail = smallest;
        if (smallest.next != null) heap.offer(smallest.next);   // that list's next head
    }
    return dummy.next;
}
```

### Line by line

| Line | What it does |
|---|---|
| `new PriorityQueue<>(Comparator.comparingInt(n -> n.val))` | a min-heap of nodes, ordered by value |
| `for (head : lists) if (head != null) heap.offer(head);` | the k starting candidates (empty lists are skipped) |
| `ListNode smallest = heap.poll();` | the globally smallest remaining node |
| `tail.next = smallest; tail = smallest;` | append it (splicing, no copying) |
| `if (smallest.next != null) heap.offer(smallest.next);` | its successor becomes that list's new candidate |
| `return dummy.next;` | the merged list |

### Dry run: `[1→4→5, 1→3→4, 2→6]`

(Call the lists a, b, c; `4b` means "the node with value 4 from list b". This is the order Java's `PriorityQueue` actually produced when run. With equal values another heap could pick either first, and the result would be the same.)

| step | heap before | poll | push (the polled node's next) | result so far |
|---|---|---|---|---|
| 1 | {1a, 1b, 2c} | 1a | 4a | 1 |
| 2 | {1b, 2c, 4a} | 1b | 3b | 1 → 1 |
| 3 | {2c, 3b, 4a} | 2c | 6c | … → 2 |
| 4 | {3b, 4a, 6c} | 3b | 4b | … → 3 |
| 5 | {4a, 4b, 6c} | 4a | 5a | … → 4 |
| 6 | {4b, 5a, 6c} | 4b | — (b is finished) | … → 4 |
| 7 | {5a, 6c} | 5a | — | … → 5 |
| 8 | {6c} | 6c | — | … → 6 |

**Result `1 → 1 → 2 → 3 → 4 → 4 → 5 → 6`** ✓. The heap never holds more than k = 3 nodes.

### Complexity
- **Time O(N log k)**, **Space O(k)** for the heap.

---

## 6. Approach 3: Divide and conquer (the file's second method)

### Code ([H02_MergeKSortedLists.java:62-82](H02_MergeKSortedLists.java#L62-L82))

```java
static ListNode mergeKListsDivide(ListNode[] lists) {
    if (lists.length == 0) return null;
    return mergeRange(lists, 0, lists.length - 1);
}

static ListNode mergeRange(ListNode[] lists, int lo, int hi) {
    if (lo == hi) return lists[lo];
    int mid = (lo + hi) / 2;
    return mergeTwo(mergeRange(lists, lo, mid), mergeRange(lists, mid + 1, hi));
}

static ListNode mergeTwo(ListNode a, ListNode b) {
    ListNode dummy = new ListNode(0), t = dummy;
    while (a != null && b != null) {
        if (a.val <= b.val) { t.next = a; a = a.next; }
        else { t.next = b; b = b.next; }
        t = t.next;
    }
    t.next = (a != null) ? a : b;
    return dummy.next;
}
```

| Line | What it does |
|---|---|
| `if (lists.length == 0) return null;` | no lists at all |
| `if (lo == hi) return lists[lo];` | a single list is already "merged" (it may be null, which `mergeTwo` handles) |
| `mergeTwo(mergeRange(lo, mid), mergeRange(mid + 1, hi))` | merge the left group, merge the right group, then merge the two results |
| `mergeTwo` | the [E02](../02-Easy/E02_MergeTwoSortedLists.md) merge |

**Dry run** `[a, b, c]`: `mergeRange(0, 2)` → `mergeTwo(mergeRange(0, 1), mergeRange(2, 2))` = `mergeTwo(mergeTwo(a, b), c)`:
- `mergeTwo(1→4→5, 1→3→4)` = `1→1→3→4→4→5`
- `mergeTwo(that, 2→6)` = `1→1→2→3→4→4→5→6` ✓

**O(N log k) time, O(log k) recursion stack**, no heap.

---

## 7. Comparing the approaches

| | One by one | Min-heap | Divide & conquer |
|---|---|---|---|
| Time | O(k · N) | **O(N log k)** | **O(N log k)** |
| Extra space | O(1) | O(k) | O(log k) stack |
| Works on streams (lists arriving lazily) | — | **yes** | no |

---

## 8. Edge cases and common mistakes

**Edge cases:** k = 0, all lists empty, some lists empty (nulls in the array), one list, lists of very different lengths.

**Mistakes:**
1. Pushing `null` heads into the heap (the comparator throws a NullPointerException).
2. Pushing **all** nodes into the heap up front: O(N log N), and O(N) space. Push only the current heads.
3. Merging one by one and claiming O(N log k).
4. Expecting a stable order for equal values from a PriorityQueue.

---

## 9. Interview questions and answers

**Q1. Why is the heap solution O(N log k)?**
> The heap never holds more than k nodes (one per list), so each offer/poll is O(log k), and each of the N nodes is offered and polled once.

**Q2. Heap or divide and conquer?**
> Both are O(N log k). The heap is natural when the lists are streams or iterators. Divide and conquer needs no extra data structure (O(log k) stack) and reuses the two-list merge.

**Q3. Why is merging one by one slow?**
> The result list grows, and each new merge re-walks it, so the first list's nodes are touched about k times: O(kN).

### Follow-up questions

**F1. External sort: merge k sorted files that don't fit in memory.**
> A k-way merge with a min-heap of (value, fileIndex), reading each file with a buffered stream. That's exactly this algorithm.

**F2. Kth Smallest Element in a Sorted Matrix (LC 378).** → [08-Heap M06](../../08-Heap/03-Medium/M06_KthSmallestInSortedMatrix.md)
> Treat each row as a sorted list; poll k times from a heap of row heads.

**F3. Smallest Range Covering Elements from K Lists (LC 632).** → [08-Heap H02](../../08-Heap/04-Hard/H02_SmallestRangeCoveringKLists.md)
> A heap of the current heads plus a running maximum; the range is [heap.min, max].

**F4. Find K Pairs with Smallest Sums (LC 373).** → [08-Heap M10](../../08-Heap/03-Medium/M10_FindKPairsWithSmallestSums.md)
> Implicit k sorted lists, merged with a heap.

---

## 10. Running the file

```bash
java H02_MergeKSortedLists.java      # 5/5 tests passed (including 300 random cases for both approaches)
```

**One-line summary:** *"Keep the k current heads in a min-heap: poll the smallest, push its next. Or merge the lists in pairs: O(N log k) either way."*
