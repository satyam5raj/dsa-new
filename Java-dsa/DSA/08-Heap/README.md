# 08 · Heap

A heap is a complete binary tree stored in an array, where every parent comes before its children (≤ for a min-heap, ≥ for a max-heap). You get the min (or max) in O(1), offer/poll in O(log n), and heapify in O(n). It is "partially sorted": cheaper than keeping everything sorted, and exactly enough for "give me the next best one".
In Java: `PriorityQueue<>()` is a min-heap, `new PriorityQueue<>(Comparator.reverseOrder())` a max-heap.

Run any file with `java FileName.java`, or every problem with `./run-all.sh`. ⭐ = not in `leetcode.md`, added for a key technique.
(The basic int heap was built in [05-Queue/01-Basics/PriorityQueueFromScratch](../05-Queue/01-Basics/PriorityQueueFromScratch.java). Start there if sift up/down is new.)

## 01-Basics: start here

| File | What you learn |
|---|---|
| `HeapDeepDive` | a generic heap with a Comparator. **Heapify is O(n)**, measured: 1.0 swaps per item vs 18 for one-by-one inserts. In-place **heap sort**, and an **indexed heap** with decrease-key/remove-any in O(log n) (used by Dijkstra). |
| `PriorityQueueInJava` | the API, comparators, and **5 traps**: `a - b` overflow, iteration isn't sorted, mutating elements inside the heap, ties aren't FIFO, unboxing null |
| `HeapPatterns` | the **6 patterns**: top-K, k-way merge, two heaps, sort + heap, most-frequent-first, lazy deletion |

## 02-Easy (3)

| # | Problem | LC | Pattern |
|---|---|---|---|
| E01 | Kth Largest Element in a Stream | 703 | Min-heap of size k: the root is the answer |
| E02 | ⭐ Last Stone Weight | 1046 | Max-heap simulation |
| E03 | ⭐ Relative Ranks | 506 | Heap of **indices** compared by value |

## 03-Medium (10)

| # | Problem | LC | Pattern |
|---|---|---|---|
| M01 | Kth Largest Element in an Array | 215 | Size-k heap vs **quickselect** (3-way partition, random pivot) |
| M02 | K Closest Points to Origin | 973 | k smallest → max-heap of size k, squared distances |
| M03 | Task Scheduler | 621 | Greedy max-heap + cooldown queue → the **formula** |
| M04 | Reorganize String | 767 | Most frequent first, hold back the last letter |
| M05 | Furthest Building You Can Reach | 1642 | Ladders on the k biggest climbs **so far** |
| M06 | Kth Smallest in a Sorted Matrix | 378 | K-way merge / binary search on the value |
| M07 | Meeting Rooms II | 253 | Sort by start + min-heap of end times |
| M08 | Ugly Number II | 264 | Generate in order: heap → 3 pointers |
| M09 | Super Ugly Number | 313 | K-way merge of k "prime streams" |
| M10 | Find K Pairs with Smallest Sums | 373 | K-way merge over an implicit matrix |

## 04-Hard (8)

| # | Problem | LC | Pattern |
|---|---|---|---|
| H01 | Find Median from Data Stream | 295 | **Two heaps** |
| H02 | Smallest Range Covering K Lists | 632 | K-way merge + current max |
| H03 | IPO | 502 | Sort by capital, heap by profit |
| H04 | Sliding Window Median | 480 | Two heaps + **lazy deletion** |
| H05 | Minimum Cost to Hire K Workers | 857 | Sort by ratio + max-heap of qualities |
| H06 | The Skyline Problem | 218 | **Sweep line** + max-heap with lazy deletion |
| H07 | Maximum Performance of a Team | 1383 | Sort by efficiency + min-heap of speeds |
| H08 | Course Schedule III | 630 | Earliest deadline + drop the longest |

**From `leetcode.md`, solved elsewhere:** Top K Frequent Elements → [Hash Table M01](../06-Hash-Table/03-Medium/M01_TopKFrequentElements.java) (bucket sort + heap), Merge k Sorted Lists → [Linked Lists H02](../03-Linked-Lists/04-Hard/H02_MergeKSortedLists.java). Dijkstra and Prim (heap-based graph algorithms) → **09-Graph**.

## Which heap? The quick rule

| You want… | Use |
|---|---|
| the k **largest** | **min**-heap of size k (throw out the smallest) |
| the k **smallest** | **max**-heap of size k |
| the median / middle | max-heap (low half) + min-heap (high half) |
| "always take the biggest/smallest and put something back" | one heap, simulate |
| the next item across k sorted sources | min-heap of one "head" per source |
| choose the best among things **unlocked so far** | sort by the unlock key, heap by the value key |
| remove arbitrary elements | lazy deletion (or an indexed heap / TreeMap) |

## Interview questions

1. Why is building a heap O(n) but inserting n items one by one O(n log n)?
2. Heap vs BST vs sorted array for a priority queue: costs of insert, delete-min, find-min, search?
3. Kth largest: heap O(n log k) vs quickselect O(n) average. When would you pick each? (stream, worst case, memory)
4. Why is heap sort rarely used in practice despite O(n log n) worst case and O(1) space? (cache locality, not stable)
5. How do you implement decrease-key with `java.util.PriorityQueue`? (you can't directly: re-insert + lazy skip, or an indexed heap)
6. Why does `(a, b) -> a - b` break a comparator?
