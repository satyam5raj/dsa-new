# E01 · Kth Largest Element in a Stream

| | |
|---|---|
| **LeetCode** | [#703 Kth Largest Element in a Stream](https://leetcode.com/problems/kth-largest-element-in-a-stream/) |
| **Difficulty** | Easy |
| **Pattern** | Top-k with a **min**-heap of size k (the root **is** the answer) |
| **Code** | [E01_KthLargestInStream.java](E01_KthLargestInStream.java) |
| **Run** | `java E01_KthLargestInStream.java` |

This is the first note of the Heap topic, so section 3 covers **`PriorityQueue` from the ground up**: how a heap is stored, the costs, min vs max, comparators and the traps. Later notes give only short recaps.

---

## 1. The problem

`KthLargest(k, nums)` is built from some starting numbers. `add(val)` adds a number and returns the **k-th largest** of everything seen so far.

```
k = 3, nums = [4, 5, 8, 2]
add(3)  → 4     (sorted descending: 8 5 4 3 2)
add(5)  → 5     (8 5 5 4 3 2)
add(10) → 5     (10 8 5 5 4 3 2)
add(9)  → 8     (10 9 8 5 5 4 3 2)
add(4)  → 8
```

**Constraints:** up to 10⁴ adds; there are always at least k numbers when `add` returns.

---

## 2. How to think about it

### Brute force
Keep a sorted list and insert into it (O(n) per add), or re-sort every time (O(n log n) per add).

### Only the k largest matter
Values are only ever **added**. Anything smaller than the current k-th largest can never become the answer again (new values can only push it further down). So keep **exactly the k largest values** in a **min-heap**:
- the root = the smallest of the top k = **the k-th largest** → O(1) answer
- `add(val)`: offer it; if the size exceeds k, poll the root (it just fell out of the top k)

### Why a *min*-heap for "largest"?
The value we throw away is always the **smallest** one we're keeping, so it must be the one that's cheap to remove: the root of a min-heap.

```
init [4, 5, 8, 2] → keeps {4, 5, 8} (2 is polled)
add 3:  {3, 4, 5, 8} → poll 3 → {4, 5, 8}   root 4
add 5:  {4, 5, 5, 8} → poll 4 → {5, 5, 8}   root 5
add 10: → poll 5 → {5, 8, 10}               root 5
```

---

## 3. Java concepts used in this problem

### 3.1 What a heap is

A **binary heap** is a complete binary tree stored in an **array**, where every parent is ≤ its children (for a min-heap). For the node at index i:

```
parent = (i − 1) / 2       left child = 2i + 1       right child = 2i + 2

min-heap of {1, 2, 3, 4, 5} after offering 5, 1, 4, 2, 3:
array: [1, 2, 4, 5, 3]
             1
           /   \
          2     4
         / \
        5   3
```

Only the **root** is guaranteed to be the minimum. The rest is just "partially ordered": `[1, 2, 4, 5, 3]` is a valid heap but not sorted. (That's the real internal order, printed by running the code.) The heap is built from scratch in [Queue 01-Basics](../../05-Queue/01-Basics/PriorityQueueFromScratch.java).

### 3.2 `PriorityQueue<E>`: Java's heap

```java
PriorityQueue<Integer> minHeap = new PriorityQueue<>();                             // smallest first
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());    // largest first
minHeap.offer(5);       // insert
minHeap.peek();         // look at the smallest (null if empty)
minHeap.poll();         // remove and return the smallest (null if empty)
minHeap.size();
```

| Operation | Cost | How |
|---|---|---|
| `offer(x)` / `add(x)` | O(log n) | append at the end, **sift up** |
| `peek()` | O(1) | the root |
| `poll()` | O(log n) | move the last element to the root, **sift down** |
| `remove(x)` (an arbitrary element) | **O(n)** | it must search for x first |
| `contains(x)` | **O(n)** | a linear scan |
| `new PriorityQueue<>(collection)` | O(n) | bottom-up heapify |

### 3.3 Comparators: deciding what "smallest" means

```java
new PriorityQueue<>(Comparator.reverseOrder())               // max-heap of Integers
new PriorityQueue<>((a, b) -> Integer.compare(a[1], b[1]))   // int[] pairs by their second field
new PriorityQueue<>(Comparator.comparingInt(s -> s.length())) // Strings by length
```

The heap always polls the element that the comparator considers **smallest**. A max-heap is just a min-heap with a reversed comparator.

**Trap:** `(a, b) -> a - b` can **overflow** for large or negative values (e.g. `Integer.MIN_VALUE − 1`). `Integer.compare(a, b)` is always safe.

### 3.4 Traps to know

- **Iterating or printing a PriorityQueue does NOT give sorted order.** `for (int x : pq)` walks the internal array. To get sorted output, poll repeatedly.
- `peek()` and `poll()` return **null** on an empty queue; unboxing that into an `int` throws a NullPointerException.
- PriorityQueue is **not** thread-safe (use `PriorityBlockingQueue` for that).

### 3.5 The "top-k" pattern

| want | heap | keep size | answer |
|---|---|---|---|
| the k **largest** | **min**-heap | k | the root = the k-th largest |
| the k **smallest** | **max**-heap | k | the root = the k-th smallest |

The heap holds the "club" of the best k, with the weakest member at the root, ready to be evicted. O(n log k) for n elements.

---

## 4. Approach 1: Sort on every add

```java
static class KthLargestSorted {
    private final int k;
    private final List<Integer> all = new ArrayList<>();
    KthLargestSorted(int k, int[] nums) { this.k = k; for (int x : nums) all.add(x); }
    int add(int val) {
        all.add(val);
        all.sort(Collections.reverseOrder());        // largest first
        return all.get(k - 1);
    }
}
```

Simple and obviously correct (the file's random test uses this idea as its referee), but **O(n log n) per add** and O(n) memory.

---

## 5. Approach 2: A min-heap of size k (the file's solution)

### Code ([E01_KthLargestInStream.java:41-55](E01_KthLargestInStream.java#L41-L55))

```java
static class KthLargest {
    private final int k;
    private final PriorityQueue<Integer> topK = new PriorityQueue<>();   // min-heap

    KthLargest(int k, int[] nums) {
        this.k = k;
        for (int x : nums) add(x);
    }

    int add(int val) {
        topK.offer(val);
        if (topK.size() > k) topK.poll();        // the smallest is no longer in the top k
        return topK.peek();
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `new PriorityQueue<>()` | a min-heap: the smallest of the kept values sits at the root |
| the constructor | feeds the initial numbers through `add`, so the size rule is applied to them too |
| `topK.offer(val);` | the new value joins the candidates |
| `if (topK.size() > k) topK.poll();` | one value too many → evict the smallest |
| `return topK.peek();` | the smallest of the top k = the k-th largest |

The constructor ignores `add`'s return value. With fewer than k initial numbers, the heap is simply smaller than k until enough values arrive.

### Dry run: k = 3, init [4, 5, 8, 2], then the adds (internal arrays from the real run)

| step | heap after offer | size > 3? | heap after | peek |
|---|---|---|---|---|
| init 4, 5, 8 | [4, 5, 8] | no | [4, 5, 8] | |
| init 2 | [2, 4, 8, 5] | yes → poll 2 | [4, 5, 8] | |
| add(3) | [3, 4, 8, 5] | poll 3 | [4, 5, 8] | **4** |
| add(5) | [4, 5, 8, 5] | poll 4 | [5, 5, 8] | **5** |
| add(10) | [5, 5, 8, 10] | poll 5 | [5, 10, 8] | **5** |
| add(9) | [5, 9, 8, 10] | poll 5 | [8, 9, 10] | **8** |
| add(4) | [4, 8, 10, 9] | poll 4 | [8, 9, 10] | **8** |

Note how a newly added value smaller than the root (3, 4) goes straight to the root and is immediately evicted.

### Complexity
- **add: O(log k).**
- **Space O(k).**
- The constructor: O(n log k).

---

## 6. Comparing the approaches

| | Sort on every add | Min-heap of size k |
|---|---|---|
| add | O(n log n) | **O(log k)** |
| Space | O(n) | **O(k)** |

The file checks the heap version against sorting on 100 random streams of 50 adds each.

---

## 7. Edge cases and common mistakes

**Edge cases:** k = 1 (the running maximum), fewer than k initial values, duplicates, negative values.

**Mistakes:**
1. Using a **max**-heap of everything (O(n) memory, and the k-th element isn't at the root).
2. Evicting **before** offering (then a large new value is compared against the wrong set).
3. Expecting `topK.toString()` or iteration to be sorted.

---

## 8. Interview questions and answers

**Q1. Why a min-heap rather than a max-heap?**
> We keep the k largest values and must repeatedly discard the smallest of them. The min-heap's root is exactly that element, and it's also the answer.

**Q2. What if elements could also be removed from the stream?**
> A heap can't remove arbitrary elements efficiently. Use a balanced BST / `TreeMap<value, count>` (an order-statistic structure), or two heaps with lazy deletion.

**Q3. What's the cost of building the initial heap?**
> O(n log k) with repeated adds. (Heapifying all n with `new PriorityQueue<>(list)` would be O(n), but that keeps n elements, not k.)

### Follow-up questions

**F1. The k-th largest of a fixed array.** → [M01](../03-Medium/M01_KthLargestElementInArray.md)
> The same heap (O(n log k)), or Quickselect (O(n) on average).

**F2. The median of a stream.** → [H01](../04-Hard/H01_FindMedianFromDataStream.md)
> Two heaps: a max-heap for the lower half and a min-heap for the upper half.

**F3. Top k frequent elements.** → [Hash Table M01](../../06-Hash-Table/03-Medium/M01_TopKFrequentElements.md)
> Count first, then the same size-k min-heap keyed by frequency.

---

## 9. Running the file

```bash
java E01_KthLargestInStream.java      # 7/7 tests passed (including 100 random streams vs sorting)
```

**One-line summary:** *"Keep the k largest values in a min-heap: offer, poll if the size exceeds k, and the root is the k-th largest."*
