# M14 · Sort List ⭐

| | |
|---|---|
| **LeetCode** | [#148 Sort List](https://leetcode.com/problems/sort-list/) |
| **Difficulty** | Medium |
| **Pattern** | Split (fast/slow) + recurse + merge (merge sort on a linked list) |
| **Code** | [M14_SortList.java](M14_SortList.java) |
| **Run** | `java M14_SortList.java` |

⭐ = a bonus classic.

---

## 1. The problem

Sort a linked list in **O(n log n)** time. Follow-up: *O(1) extra space?*

| list | result |
|---|---|
| `4 → 2 → 1 → 3` | `1 → 2 → 3 → 4` |
| `-1 → 5 → 3 → 4 → 0` | `-1 → 0 → 3 → 4 → 5` |
| `[]` | `[]` |
| `3 → 1 → 3 → 1` | `1 → 1 → 3 → 3` |

**Constraints:** `0 ≤ n ≤ 5 × 10⁴`.

---

## 2. How to think about it

### Which sort fits a linked list?
- **Quick sort** needs good pivots and partitioning with random access, which is awkward on a list (and its worst case is O(n²)).
- **Heap sort** needs random access.
- **Merge sort** only needs to **split** and **merge**, both natural on lists, and the merge needs **no extra array**: we just relink the nodes ([E02](../02-Easy/E02_MergeTwoSortedLists.md)). It's the perfect fit, with a guaranteed O(n log n) and a stable result.

### The algorithm
```
sort(head):
    if there are 0 or 1 nodes: return head
    mid = the FIRST middle (fast/slow)       // 4 → 2 | 1 → 3
    right = mid.next; mid.next = null        // cut into two halves
    return merge(sort(head), sort(right))
```

### Why the **first** middle?
With 2 nodes, the "second middle" would be the 2nd node, so the left half would keep **both** nodes → **infinite recursion**. Splitting at the first middle always makes both halves smaller.

---

## 3. Java concepts used in this problem

### 3.1 Divide and conquer recursion

Each call splits the problem in half, solves both halves recursively, and combines the results in O(n): `T(n) = 2T(n/2) + O(n) = O(n log n)`. The recursion depth is log₂ n (about 16 for 5 × 10⁴), so stack overflow is no concern.

### 3.2 The merge from E02

`merge` is exactly the iterative dummy-and-tail merge from [E02](../02-Easy/E02_MergeTwoSortedLists.md). Building solutions from small, proven helpers is how linked-list problems become manageable.

### 3.3 Random tests against `Arrays.sort`

300 random lists (lengths 1–60, with duplicates and negatives) are compared with the sorted array. A good reference implementation makes testing trivial.

---

## 4. Approach 1: Copy into an array, sort, write back

```java
static ListNode sortByArray(ListNode head) {
    List<Integer> vals = new ArrayList<>();
    for (ListNode c = head; c != null; c = c.next) vals.add(c.val);
    Collections.sort(vals);
    ListNode c = head;
    for (int v : vals) { c.val = v; c = c.next; }
    return head;
}
```

**O(n log n) time, O(n) space**, and it overwrites values instead of relinking the nodes. It's a reasonable baseline to mention.

---

## 5. Approach 2: Top-down merge sort (the file's solution)

### Code ([M14_SortList.java:42-63](M14_SortList.java#L42-L63))

```java
static ListNode sortList(ListNode head) {
    if (head == null || head.next == null) return head;
    ListNode slow = head, fast = head;
    while (fast.next != null && fast.next.next != null) {   // FIRST middle
        slow = slow.next;
        fast = fast.next.next;
    }
    ListNode right = slow.next;
    slow.next = null;                                        // cut
    return merge(sortList(head), sortList(right));
}

static ListNode merge(ListNode a, ListNode b) {
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

### Line by line

| Line | What it does |
|---|---|
| `if (head == null \|\| head.next == null) return head;` | base case: 0 or 1 nodes is already sorted |
| the first-middle loop | slow = the end of the left half |
| `right = slow.next; slow.next = null;` | cut into two independent lists |
| `merge(sortList(head), sortList(right))` | sort each half, then merge them |
| `merge` | the standard two-way merge with a dummy and a tail; `<=` keeps it stable |

### Dry run: `4 → 2 → 1 → 3`

```
sort(4,2,1,3)
├── split at the first middle (2): [4,2] | [1,3]
├── sort(4,2)
│   ├── split: [4] | [2]
│   └── merge([4], [2]) = 2 → 4
├── sort(1,3)
│   ├── split: [1] | [3]
│   └── merge([1], [3]) = 1 → 3
└── merge(2→4, 1→3):
       1 (from b), 2 (a), 3 (b), then the rest of a: 4   →   1 → 2 → 3 → 4
```

**Result `1 → 2 → 3 → 4`** ✓

### Complexity
- **Time O(n log n):** log n levels, O(n) merge work per level.
- **Space O(log n)** for the recursion stack.

---

## 6. Approach 3: Bottom-up merge sort (O(1) extra space, the follow-up)

Instead of recursing, merge sublists of size 1, then 2, then 4, … in place:

```java
static ListNode sortListBottomUp(ListNode head) {
    int n = 0;
    for (ListNode c = head; c != null; c = c.next) n++;
    ListNode dummy = new ListNode(0, head);
    for (int size = 1; size < n; size *= 2) {
        ListNode prev = dummy, cur = dummy.next;
        while (cur != null) {
            ListNode left = cur;
            ListNode right = split(left, size);       // cut off `size` nodes, return the rest
            cur = split(right, size);                 // cut off the next `size`, cur = the remainder
            prev.next = merge(left, right);           // merge the two runs
            while (prev.next != null) prev = prev.next;   // move prev to the end of the merged run
        }
    }
    return dummy.next;
}

// cut the list after `size` nodes; return the head of the remainder
static ListNode split(ListNode head, int size) {
    for (int i = 1; head != null && i < size; i++) head = head.next;
    if (head == null) return null;
    ListNode rest = head.next;
    head.next = null;
    return rest;
}
```

- Pass 1 merges pairs of single nodes, pass 2 merges pairs of 2-runs, and so on: log n passes.
- **O(n log n) time, O(1) extra space** (no recursion). It's harder to write, but it answers the follow-up.

---

## 7. Comparing the approaches

| | Array copy | Top-down merge sort | Bottom-up merge sort |
|---|---|---|---|
| Time | O(n log n) | O(n log n) | O(n log n) |
| Extra space | O(n) | O(log n) stack | **O(1)** |
| Relinks the nodes | no | **yes** | **yes** |
| Code size | small | **small** | larger |

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty list, one node, two nodes, all equal values, already sorted, reverse sorted.

**Mistakes:**
1. Splitting at the **second** middle, which loops forever on 2-node lists.
2. Forgetting to cut (`slow.next = null`), so the left half still includes the right half.
3. Using quicksort with the first node as the pivot: O(n²) on sorted input.

---

## 9. Interview questions and answers

**Q1. Why merge sort for linked lists?**
> It needs only sequential access. Splitting is O(n) with fast/slow, and merging relinks the nodes without an extra array. It's guaranteed O(n log n) and stable.

**Q2. How do you get O(1) extra space?**
> Bottom-up merge sort: iteratively merge runs of size 1, 2, 4, … using a dummy head and a tail pointer, with no recursion.

**Q3. Why must the split use the first middle?**
> For two nodes, the second middle is the last node, so the left half would be the whole list, and the recursion would never shrink.

### Follow-up questions

**F1. Insertion Sort List (LC 147).**
> O(n²): insert each node into a growing sorted list (with a dummy). Good for nearly sorted input.

**F2. Merge k sorted lists.** → [H02](../04-Hard/H02_MergeKSortedLists.md)
> Merge pairs of lists in rounds, the same merge step at a larger scale.

**F3. Sort a list of 0s, 1s and 2s.**
> Three chains (as in [M07](M07_PartitionList.md)) joined in order: O(n), no comparison sort needed.

---

## 10. Running the file

```bash
java M14_SortList.java      # 5/5 tests passed (including 300 random lists vs Arrays.sort)
```

**One-line summary:** *"Merge sort: cut at the first middle, sort both halves, merge (bottom-up for O(1) space)."*
