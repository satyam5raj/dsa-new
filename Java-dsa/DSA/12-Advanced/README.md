# 12 · Advanced Data Structures & Design

The last DSA topic covers three classic "advanced" structures and the **design problems** that combine earlier structures to hit O(1) or O(log n) targets:
- **LRU Cache**: HashMap + doubly linked list.
- **Skip List**: a sorted linked list with probabilistic express lanes, as used in Redis and in Java's `ConcurrentSkipListMap`.
- **Bloom Filter**: a probabilistic set. It never gives false negatives, uses about 10 bits per item, and is used by databases and crawlers.

Run any file with `java FileName.java`, or every problem with `./run-all.sh`. ⭐ = not in `leetcode.md`, added for a key technique.

**Study notes:** every problem file has a companion `.md` with the same name (e.g. `02-Easy/E01_LoggerRateLimiter.md`). Each note covers the problem, how to think about it, the Java concepts it uses (E01 starts with how design problems are written: classes with state, `private final` fields, `getOrDefault`), a line-by-line walkthrough of every approach, dry-run tables, edge cases, and interview questions with follow-ups.

## 01-Basics: start here

| File | What you learn |
|---|---|
| `LRUCacheBasics` | why a HashMap **and** a doubly linked list; generic LRU from scratch with sentinels; the `LinkedHashMap(accessOrder=true)` + `removeEldestEntry` shortcut; both checked against each other |
| `SkipListBasics` | levels, search (right, then down), insert/delete with `update[]`; checked against `TreeSet` over 200k ops; **measured** search cost ≈ log₂n (18 moves for a million keys) |
| `BloomFilterBasics` | bit array + k hashes via double hashing; sizing formulas (9.6 bits/item for 1%); **measured** 1.1% false positives and zero false negatives; what overfilling does; a counting Bloom filter that supports delete |

## 02-Easy (1)

| # | Problem | LC | Pattern |
|---|---|---|---|
| E01 | ⭐ Logger Rate Limiter | 359 | Next-allowed-time map, plus a bounded-memory version (TTL) |

## 03-Medium (4)

| # | Problem | LC | Pattern |
|---|---|---|---|
| M01 | LRU Cache | 146 | HashMap + doubly linked list |
| M02 | ⭐ Time Based Key-Value Store | 981 | Versioned values + binary search (or `TreeMap.floorEntry`) |
| M03 | ⭐ Snapshot Array | 1146 | Store only **changes** per index (copy-on-write idea) |
| M04 | ⭐ Design Twitter | 355 | Per-user lists + **k-way merge** for the feed |

## 04-Hard (4)

| # | Problem | LC | Pattern |
|---|---|---|---|
| H01 | ⭐ LFU Cache | 460 | count → LinkedHashSet buckets + `minFreq` |
| H02 | ⭐ Design Skiplist | 1206 | Skip list with duplicates |
| H03 | ⭐ All O`one Data Structure | 432 | Linked list of **count buckets** |
| H04 | ⭐ Maximum Frequency Stack | 895 | One stack per frequency level |

## Advance: problems that mix data structures

Each problem below needs **two or more** structures or techniques working together. Every one has a runnable `.java` (a brute-force referee plus random tests) and a `.md` note in the same format as the rest of the course. They are roughly in order of difficulty.

| # | Problem | LC | Mix |
|---|---|---|---|
| A01 | [Design A Leaderboard](Advance/A01_DesignALeaderboard.md) | 1244 | HashMap + TreeMap of score counts |
| A02 | [Stock Price Fluctuation](Advance/A02_StockPriceFluctuation.md) | 2034 | HashMap + two heaps with lazy deletion (or TreeMap) |
| A03 | [Single-Threaded CPU](Advance/A03_SingleThreadedCPU.md) | 1834 | sort by arrival + min-heap + clock |
| A04 | [Process Tasks Using Servers](Advance/A04_ProcessTasksUsingServers.md) | 1882 | two heaps (free by weight, busy by free time) |
| A05 | [Data Stream as Disjoint Intervals](Advance/A05_DataStreamAsDisjointIntervals.md) | 352 | TreeMap + floor/higher neighbours |
| A06 | [Range Module](Advance/A06_RangeModule.md) | 715 | TreeMap intervals: merge on add, split on remove |
| A07 | [Making A Large Island](Advance/A07_MakingALargeIsland.md) | 827 | DFS labelling + HashMap sizes + HashSet of neighbours |
| A08 | [Shortest Path with Obstacles Elimination](Advance/A08_ShortestPathWithObstaclesElimination.md) | 1293 | BFS over (cell, eliminations left) states |
| A09 | [Shortest Path Visiting All Nodes](Advance/A09_ShortestPathVisitingAllNodes.md) | 847 | BFS + bitmask state (or Held-Karp DP) |
| A10 | [Trapping Rain Water II](Advance/A10_TrappingRainWaterII.md) | 407 | min-heap + grid BFS (Dijkstra-like) |
| A11 | [Minimum Interval to Include Each Query](Advance/A11_MinimumIntervalToIncludeEachQuery.md) | 1851 | offline sorted queries + min-heap |
| A12 | [Count of Range Sum](Advance/A12_CountOfRangeSum.md) | 327 | prefix sums + merge sort / Fenwick + compression |
| A13 | [Reverse Pairs](Advance/A13_ReversePairs.md) | 493 | merge sort / Fenwick + compression |
| A14 | [Critical and Pseudo-Critical MST Edges](Advance/A14_CriticalAndPseudoCriticalEdges.md) | 1489 | sorting + union-find (Kruskal with skip/force) |
| A15 | [Design In-Memory File System](Advance/A15_DesignInMemoryFileSystem.md) | 588 | trie of path parts + TreeMap children |
| A16 | [Design Search Autocomplete System](Advance/A16_DesignSearchAutocompleteSystem.md) | 642 | trie + size-3 heap |
| A17 | [Design Underground System](Advance/A17_DesignUndergroundSystem.md) | 1396 | two HashMaps (open trips + route totals), record keys |
| A18 | [Design Snake Game](Advance/A18_DesignSnakeGame.md) | 353 | deque (body) + HashSet (occupied cells) |
| A19 | [Design a Food Rating System](Advance/A19_DesignFoodRatingSystem.md) | 2353 | HashMaps + TreeSet per cuisine (or heap + lazy deletion) |
| A20 | [Exam Room](Advance/A20_ExamRoom.md) | 855 | TreeSet of seats + max-heap of gaps, lazy deletion |
| A21 | [Longest Increasing Path in a Matrix](Advance/A21_LongestIncreasingPathInMatrix.md) | 329 | grid DAG: DFS + memo, or topological levels |
| A22 | [Word Break II](Advance/A22_WordBreakII.md) | 140 | backtracking + memo HashMap + HashSet |
| A23 | [Split Array Largest Sum](Advance/A23_SplitArrayLargestSum.md) | 410 | binary search on the answer + greedy (or DP) |
| A24 | [K-th Smallest Pair Distance](Advance/A24_KthSmallestPairDistance.md) | 719 | sort + binary search on the answer + two pointers (or heap) |
| A25 | [Max Sum of Rectangle No Larger Than K](Advance/A25_MaxSumOfRectangleNoLargerThanK.md) | 363 | row compression + prefix sums + TreeSet.ceiling |
| A26 | [Number of Ways to Arrive at Destination](Advance/A26_NumberOfWaysToArriveAtDestination.md) | 1976 | Dijkstra + counting DP |
| A27 | [Largest Color Value in a Directed Graph](Advance/A27_LargestColorValueInDirectedGraph.md) | 1857 | topological sort + DP per colour + cycle check |
| A28 | [Minimum Cost to Make a Valid Path](Advance/A28_MinimumCostToMakeValidPath.md) | 1368 | 0-1 BFS with a deque (or Dijkstra) |
| A29 | [Bricks Falling When Hit](Advance/A29_BricksFallingWhenHit.md) | 803 | reverse time (offline) + union-find with a roof node |

## Design-problem toolkit: "O(1) everything" recipes

| Need | Combine |
|---|---|
| lookup + recency order | HashMap + doubly linked list (LRU) |
| lookup + frequency order | HashMap + frequency buckets (LFU, All O`one, Freq Stack) |
| lookup + random element | HashMap + ArrayList, swap with the last ([Hash Table M06](../06-Hash-Table/03-Medium/M06_InsertDeleteGetRandom.java)) |
| values over time / versions | per-key sorted list + binary search, or TreeMap |
| sorted order with fast updates | TreeMap / skip list / balanced BST |
| "have I seen this?" over billions, little memory, some error OK | Bloom filter |
| top items across many sorted sources | k-way merge with a heap |

## Interview questions

1. Why does an LRU cache need a **doubly** linked list, and why must each node store its key?
2. How does `LinkedHashMap` implement LRU? What does `accessOrder=true` change?
3. LRU vs LFU: which workloads favour each? What's LFU's weakness? (old popular items never leave → add aging)
4. Skip list vs balanced BST: complexity, implementation effort, concurrency.
5. Why can't a Bloom filter have false negatives? How do you pick m and k? How would you delete?
6. How would you make the LRU cache thread-safe? (synchronized methods / locks, `ConcurrentHashMap` + a concurrent queue, Caffeine's approach)
