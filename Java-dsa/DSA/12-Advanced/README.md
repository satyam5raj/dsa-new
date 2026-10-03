# 12 · Advanced Data Structures & Design

The last DSA topic covers three classic "advanced" structures and the **design problems** that combine earlier structures to hit O(1) or O(log n) targets:
- **LRU Cache**: HashMap + doubly linked list.
- **Skip List**: a sorted linked list with probabilistic express lanes, as used in Redis and in Java's `ConcurrentSkipListMap`.
- **Bloom Filter**: a probabilistic set. It never gives false negatives, uses about 10 bits per item, and is used by databases and crawlers.

Run any file with `java FileName.java`, or every problem with `./run-all.sh`. ⭐ = not in `leetcode.md`, added for a key technique.

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
