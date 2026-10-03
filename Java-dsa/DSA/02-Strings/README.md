# 02 · Strings

A string is an **array of characters**, so every array pattern carries over. Strings add three things: **character counting** (`int[26]` / `int[128]`), **immutability** (build with `StringBuilder`, edit with `char[]`), and **text-search algorithms**.

Run any file with `java FileName.java`, or everything at once with `./run-all.sh`.
⭐ = a classic not in `leetcode.md`, added because it teaches a key technique.

## 01-Basics: start here

| File | What you learn |
|---|---|
| `StringFundamentals` | chars as numbers (`c - 'a'`), frequency arrays vs HashMap, `Character` helpers, char[] / StringBuilder building, run-length encoding, splitting, comparing, the cost of each operation, the `+=` trap (measured: 31 ms vs 0.3 ms) |
| `StringPatterns` | the 6 string patterns with examples: frequency array, two pointers, **sliding window + counts template**, character mapping (bijection), canonical key, expand around center. Also substring vs subsequence |
| `StringMatchingAlgorithms` | **naive, KMP (LPS array explained), Rabin-Karp (rolling hash), Z-algorithm**, all checked against each other on 2,000 random cases |

## 02-Easy (12)

| # | Problem | LC | Pattern |
|---|---|---|---|
| E01 | Valid Anagram (+ Unicode follow-up) | 242 | Frequency array |
| E02 | Valid Palindrome | 125 | Two pointers, skip non-alphanumerics |
| E03 | Reverse String | 344 | Two pointers (+ recursive version) |
| E04 | Length of Last Word | 58 | Scan from the end |
| E05 | Valid Palindrome II | 680 | Two pointers + one skip |
| E06 | First Unique Character (+ streaming follow-up) | 387 | Count, then a second pass |
| E07 | Isomorphic Strings | 205 | Bijection (two maps) |
| E08 | Word Pattern | 290 | Bijection char ↔ word |
| E09 | Ransom Note | 383 | Supply vs demand counts |
| E10 | Find the Difference | 389 | XOR / char sum |
| E11 | ⭐ Longest Common Prefix | 14 | Vertical scan / sort trick |
| E12 | ⭐ Find the Index of the First Occurrence | 28 | Naive → KMP |

## 03-Medium (12)

| # | Problem | LC | Pattern |
|---|---|---|---|
| M01 | Longest Substring Without Repeating Characters | 3 | Sliding window (counts / last-index jump) |
| M02 | Longest Repeating Character Replacement | 424 | Window − maxFreq ≤ k |
| M03 | Permutation in String | 567 | Fixed window + counts (+ O(n) "matches" version) |
| M04 | Longest Substring with At Most K Distinct | 340 | Window + distinct counter |
| M05 | Find All Anagrams in a String | 438 | Fixed window + "missing" counter |
| M06 | Group Anagrams | 49 | Canonical key (sorted / counts) |
| M07 | Encode and Decode Strings | 271 | Length-prefix framing |
| M08 | Bulls and Cows | 299 | One pass, signed counts |
| M09 | Sort Characters by Frequency | 451 | Count + bucket sort |
| M10 | Longest Palindromic Substring | 5 | Expand around center (+ DP) |
| M11 | Palindromic Substrings | 647 | Expand around center (count) |
| M12 | ⭐ Reverse Words in a String | 151 | Split / double reverse in place |

## 04-Hard (3)

| # | Problem | LC | Pattern |
|---|---|---|---|
| H01 | Minimum Window Substring | 76 | **Sliding window: expand until valid, shrink while valid** |
| H02 | ⭐ Shortest Palindrome | 214 | KMP LPS on `s + '#' + reverse(s)` |
| H03 | ⭐ Substring with Concatenation of All Words | 30 | Sliding window over word-sized tokens |

## The sliding-window family (learn them as one idea)

| Variant | Window is valid when… | Shrink while… | Problems |
|---|---|---|---|
| Longest | no repeats / ≤ k distinct / changes ≤ k | **invalid** | M01, M02, M04 |
| Shortest | it covers all required chars | **valid** (record first) | H01 |
| Fixed size m | always size m | size > m | M03, M05, H03 |

## Moved to other topics on purpose
Valid Parentheses and Decode String → **Stack**. Reorganize String → **Heap**. Word Break, Longest Common Subsequence, Distinct Subsequences, Decode Ways → **Dynamic Programming**. Word Search, Palindrome Partitioning, Letter Combinations → **Backtracking**. Word Ladder → **Graphs**.
