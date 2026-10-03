# 05 · Queue

FIFO: offer at the back, poll from the front, both O(1). Variants: **circular queue** (ring buffer), **deque** (both ends), **priority queue** (serves the smallest or largest first, built on a heap).
In Java use `ArrayDeque` for queues and deques, and `PriorityQueue` for heaps. Never `LinkedList` as a queue: it's slower.

Run any file with `java FileName.java`, or everything with `./run-all.sh`. ⭐ = not in `leetcode.md`, added for a key technique.

## 01-Basics: start here

| File | What you learn |
|---|---|
| `QueueImplementations` | linked queue, **circular array queue** (watch the indices wrap, then grow), circular **deque**, all from scratch and checked against `ArrayDeque` with 20,000 random operations |
| `PriorityQueueFromScratch` | **binary heap** in an array (parent/child index math), sift up/down, O(n) heapify, heap sort as a side effect, a priority queue of objects (ER triage). Checked against `java.util.PriorityQueue` |
| `QueuePatterns` | **BFS** level by level (shortest path in a grid), **multi-source BFS**, sliding time window (a rate limiter), monotonic deque, round robin scheduling |

## 02-Easy (4)

| # | Problem | LC | Pattern |
|---|---|---|---|
| E01 | Implement Stack using Queues | 225 | Rotate after push |
| E02 | Moving Average from Data Stream | 346 | Queue + running sum / ring buffer |
| E03 | ⭐ Number of Recent Calls | 933 | Sliding time window |
| E04 | ⭐ Time Needed to Buy Tickets | 2073 | Simulation → formula |

## 03-Medium (7)

| # | Problem | LC | Pattern |
|---|---|---|---|
| M01 | Design Circular Queue | 622 | Ring buffer (head + size) |
| M02 | Design Circular Deque | 641 | Ring buffer both ends (`-1 % k` trap) |
| M03 | Design Hit Counter | 362 | Queue → 300 fixed buckets (constant memory) |
| M04 | ⭐ Dota2 Senate | 649 | Two queues of turn order |
| M05 | ⭐ Reveal Cards in Increasing Order | 950 | Simulate on positions, then assign |
| M06 | ⭐ Jump Game VI | 1696 | **DP + monotonic deque** |
| M07 | Longest Subarray with Abs Diff ≤ Limit | 1438 | Window + **two** monotonic deques (max and min) |

## 04-Hard (2)

| # | Problem | LC | Pattern |
|---|---|---|---|
| H01 | ⭐ Constrained Subsequence Sum | 1425 | Kadane + DP + monotonic deque |
| H02 | ⭐ Max Value of Equation | 1499 | Split the formula into f(i) + g(j) → window max |

**From `leetcode.md`, solved elsewhere:** Implement Queue using Stacks → [Stack E02](../04-Stack/02-Easy/E02_ImplementQueueUsingStacks.java), Sliding Window Maximum → [Arrays H02](../01-Arrays/04-Hard/H02_SlidingWindowMaximum.java), Shortest Subarray with Sum ≥ K (also a monotonic deque) → [Arrays H04](../01-Arrays/04-Hard/H04_ShortestSubarrayWithSumAtLeastK.java). Kth Largest, Task Scheduler and Median from Data Stream → **08-Heap**. Rotting Oranges (multi-source BFS) → **09-Graph**.

## The monotonic deque, in one place

| Problem | What the deque holds | Front gives |
|---|---|---|
| Sliding Window Maximum | indices, values decreasing | the window max |
| Abs Diff ≤ Limit (M07) | two deques: decreasing + increasing | window max and min |
| Jump Game VI (M06) | indices, **dp** values decreasing | the best previous state within k |
| Constrained Subsequence Sum (H01) | the same, only positive dp | the best predecessor, or start fresh |
| Max Value of Equation (H02) | indices, (y − x) decreasing | the best partner within distance k |
| Shortest Subarray ≥ K (Arrays H04) | indices, prefix sums increasing | candidate starts |

Rule of thumb: **"the max/min over a range that slides forward" → monotonic deque, O(n).**
