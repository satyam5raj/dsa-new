# 06 · Hash Table

Turn a **key** into an **array index** with a hash function, so lookups, inserts and deletes are **O(1) on average**. The hard part is **collisions** (two keys landing in the same slot). There are two classic fixes: **separate chaining** (a list per bucket, which is what `HashMap` does) and **open addressing** (probe for the next free slot, with tombstones for deletes).
In Java use `HashMap`/`HashSet`. Use `TreeMap`/`TreeSet` when you need sorted order (O(log n)). Use a plain `int[26]` or `int[128]` when the keys are small integers or characters.

Run any file with `java FileName.java`, or everything with `./run-all.sh`. ⭐ = not in `leetcode.md`, added for a key technique.

## 01-Basics: start here

| File | What you learn |
|---|---|
| `HashFunctions` | polynomial string hash, compressing a hash into an index (the negative `%` trap → `Math.floorMod` / `& (n-1)`), good vs bad hash distribution, the **equals/hashCode contract** for custom keys, HashSet vs TreeSet |
| `HashTableChaining` | a generic map from scratch: bucket lists, **load factor 0.75 → resize and rehash**. Checked against `HashMap` with 100k random operations |
| `HashTableOpenAddressing` | **linear probing**, **tombstones** for delete, measured probe counts as the load rises (1.2 probes at 25% full → 16 at 97%) |

## 02-Easy (6)

| # | Problem | LC | Pattern |
|---|---|---|---|
| E01 | Happy Number | 202 | Seen-set cycle detection (or fast/slow) |
| E02 | Contains Duplicate II | 219 | Last index map / sliding window set |
| E03 | Design HashSet | 705 | Chaining from scratch |
| E04 | Design HashMap | 706 | Chaining with dummy heads |
| E05 | Minimum Index Sum of Two Lists | 599 | Map one list, scan the other |
| E06 | ⭐ Unique Number of Occurrences | 1207 | Count, then a set of the counts |

## 03-Medium (7)

| # | Problem | LC | Pattern |
|---|---|---|---|
| M01 | Top K Frequent Elements | 347 | Count + **bucket sort by frequency** O(n) (or a size-k heap) |
| M02 | Count Primes | 204 | **Sieve of Eratosthenes** (a direct-address table) |
| M03 | Check If Array Pairs Are Divisible by k | 1497 | Count remainders, match r with k−r |
| M04 | ⭐ 4Sum II | 454 | **Meet in the middle**: O(n⁴) → O(n²) |
| M05 | ⭐ Continuous Subarray Sum | 523 | Prefix sum **mod k** → first index |
| M06 | ⭐ Insert Delete GetRandom O(1) | 380 | Map + array, **swap with the last** to delete |
| M07 | ⭐ Contiguous Array | 525 | 0 → −1, longest sum-0 subarray, first index |

## 04-Hard (3)

| # | Problem | LC | Pattern |
|---|---|---|---|
| H01 | Contains Duplicate III | 220 | **Value buckets** of width t+1 + sliding window (`floorDiv` for negatives) |
| H02 | ⭐ Insert Delete GetRandom – Duplicates | 381 | M06 with value → *set* of indices |
| H03 | ⭐ Longest Duplicate Substring | 1044 | **Binary search on the length + rolling hash** set |

## From `leetcode.md` Hashing, solved elsewhere

Hashing appears everywhere, so most of the list was already solved in Arrays and Strings:

| Problem | Where |
|---|---|
| Two Sum | [Arrays E01](../01-Arrays/02-Easy/E01_TwoSum.java) |
| Intersection of Two Arrays II | [Arrays E11](../01-Arrays/02-Easy/E11_IntersectionOfTwoArrays.java) |
| Subarray Sum Equals K | [Arrays M07](../01-Arrays/03-Medium/M07_SubarraySumEqualsK.java) |
| Longest Consecutive Sequence | [Arrays M13](../01-Arrays/03-Medium/M13_LongestConsecutiveSequence.java) |
| Majority Element II | [Arrays M15](../01-Arrays/03-Medium/M15_MajorityElementII.java) |
| Find All Duplicates in an Array | [Arrays M16](../01-Arrays/03-Medium/M16_FindAllDuplicates.java) |
| Subarrays with K Different Integers | [Arrays H05](../01-Arrays/04-Hard/H05_SubarraysWithKDifferentIntegers.java) |
| Valid Anagram | [Strings E01](../02-Strings/02-Easy/E01_ValidAnagram.java) |
| First Unique Character | [Strings E06](../02-Strings/02-Easy/E06_FirstUniqueCharacter.java) |
| Isomorphic Strings | [Strings E07](../02-Strings/02-Easy/E07_IsomorphicStrings.java) |
| Word Pattern | [Strings E08](../02-Strings/02-Easy/E08_WordPattern.java) |
| Ransom Note | [Strings E09](../02-Strings/02-Easy/E09_RansomNote.java) |
| Find the Difference | [Strings E10](../02-Strings/02-Easy/E10_FindTheDifference.java) |
| Group Anagrams | [Strings M06](../02-Strings/03-Medium/M06_GroupAnagrams.java) |
| Encode and Decode Strings | [Strings M07](../02-Strings/03-Medium/M07_EncodeDecodeStrings.java) |
| Bulls and Cows | [Strings M08](../02-Strings/03-Medium/M08_BullsAndCows.java) |
| Sort Characters by Frequency | [Strings M09](../02-Strings/03-Medium/M09_SortCharactersByFrequency.java) |
| Minimum Window Substring | [Strings H01](../02-Strings/04-Hard/H01_MinimumWindowSubstring.java) |

LRU Cache (hash map + doubly linked list) → **12-Advanced**.

## What to store in the map: the decision table

| Question | Map holds | Example |
|---|---|---|
| "have I seen x?" | a set | Happy Number, Contains Duplicate |
| "where did I see x?" | x → **last** index | Two Sum, Contains Duplicate II |
| "longest stretch since x?" | x → **first** index (never overwrite) | Contiguous Array, Continuous Subarray Sum |
| "how many times?" | x → count | Top K Frequent, Subarray Sum = K (prefix → count) |
| "which items share a key?" | canonical key → list | Group Anagrams |
| "close values, not equal ones" | value range → bucket | Contains Duplicate III |
| "find x, then delete it in O(1)" | x → index into an array | Insert Delete GetRandom |

## Interview questions

1. How does `HashMap` work internally? (array of buckets, `hash ^ (hash >>> 16)`, index `= hash & (n-1)`, resize at 0.75, and since Java 8 a bucket turns into a red-black **tree** once it holds 8 entries.)
2. Why must `hashCode` change whenever `equals` does? What breaks if you mutate a key after inserting it?
3. Chaining vs open addressing: trade-offs? (cache locality, deletes need tombstones, load factor limits)
4. Why is the worst case O(n), and how do you defend against hash flooding attacks? (treeified buckets, randomized seeds)
5. `HashMap` vs `LinkedHashMap` vs `TreeMap` vs `ConcurrentHashMap`: when to use each?
6. Rolling hash: why verify a match? (collisions, as in H03)
