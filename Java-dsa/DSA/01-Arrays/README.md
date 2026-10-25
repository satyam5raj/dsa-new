# 01 · Arrays

Go through the folders in order. Every file runs with `java FileName.java` and checks its own answers (PASS/FAIL).
`./run-all.sh` runs every problem at once.

**Study notes:** every problem file has a companion `.md` with the same name (e.g. `02-Easy/E01_TwoSum.md`). Each note covers the problem, how to think about it, the new Java concepts it uses, a line-by-line walkthrough of every approach, dry-run tables, edge cases, and interview questions with follow-ups.

## How to study each problem
1. Read the **PROBLEM** and examples at the top, then **close the file and try it yourself** (15–30 min).
2. Read the explanation: brute force → bottleneck → key insight → optimal.
3. Follow the **DRY RUN** with a pen. This is where the logic actually clicks.
4. Run the file, then change an input and predict the output before running again.
5. Revisit after 3 days and after a week. Can you explain the key insight in one sentence?

## 01-Basics: start here

| File | What you learn |
|---|---|
| `StaticArrayOperations` | memory layout, why access is O(1), traverse / insert / delete / search / reverse / rotate, copying pitfalls, 2D arrays |
| `DynamicArray` | **build ArrayList yourself**: resize by doubling, shrinking, amortized O(1), generics, iterator |
| `ArrayPatterns` | the 7 patterns behind almost every problem: hashing, prefix sum, two pointers (2 kinds), sliding window (fixed & variable), Kadane, index marking, Dutch flag |

## 02-Easy (27 problems)

| # | Problem | LC | Pattern | Time / Space |
|---|---|---|---|---|
| E01 | Two Sum | 1 | Hashing (complement) | O(n) / O(n) |
| E02 | Contains Duplicate | 217 | Hashing / sorting | O(n) / O(n) |
| E03 | Running Sum of 1D Array | 1480 | Prefix sum | O(n) / O(1) |
| E04 | Find Pivot Index | 724 | Prefix sum (left vs right) | O(n) / O(1) |
| E05 | Best Time to Buy and Sell Stock | 121 | Min so far | O(n) / O(1) |
| E06 | Move Zeroes | 283 | Two pointers (slow/fast) | O(n) / O(1) |
| E07 | Remove Duplicates from Sorted Array (+#80) | 26 | Two pointers (slow/fast) | O(n) / O(1) |
| E08 | Merge Sorted Array | 88 | Two pointers from the back | O(m+n) / O(1) |
| E09 | Squares of a Sorted Array | 977 | Two pointers (ends) | O(n) / O(n) |
| E10 | Missing Number | 268 | Math / XOR | O(n) / O(1) |
| E11 | Intersection of Two Arrays (+#350) | 349 | Hashing / sort + two pointers | O(n+m) |
| E12 | Plus One | 66 | Carry simulation | O(n) / O(1) |
| E13 | Majority Element | 169 | Boyer-Moore voting | O(n) / O(1) |
| E14 | Single Number | 136 | XOR | O(n) / O(1) |
| E15 | Pascal's Triangle (+ II) | 118, 119 | Row from previous row | O(n²) / O(k) |
| E16 | Find All Numbers Disappeared | 448 | Index marking | O(n) / O(1) |
| E17 | Third Maximum Number | 414 | Top-3 variables | O(n) / O(1) |
| E18 | Sort Array by Parity | 905 | Partition (two pointers) | O(n) / O(1) |
| E19 | Monotonic Array | 896 | Flags | O(n) / O(1) |
| E20 | Valid Mountain Array | 941 | Walk up, walk down | O(n) / O(1) |
| E21 | Range Sum Query – Immutable | 303 | Prefix sum (O(1) queries) | O(1) query |
| E22 | Max Consecutive Ones | 485 | Running counter | O(n) / O(1) |
| E23 | Maximum Average Subarray I | 643 | Sliding window (fixed) | O(n) / O(1) |
| E24 | Remove Element | 27 | Two pointers (slow/fast) | O(n) / O(1) |
| E25 | Assign Cookies | 455 | Greedy + sort | O(n log n) |
| E26 | Relative Sort Array | 1122 | Counting sort | O(n + R) |
| E27 | Height Checker | 1051 | Counting sort | O(n + R) |

## 03-Medium (17 problems)

| # | Problem | LC | Pattern | Time / Space |
|---|---|---|---|---|
| M01 | Maximum Subarray | 53 | **Kadane** | O(n) / O(1) |
| M02 | Rotate Array | 189 | Triple reverse | O(n) / O(1) |
| M03 | Two Sum II – Sorted Input | 167 | Two pointers (ends) | O(n) / O(1) |
| M04 | Container With Most Water | 11 | Two pointers (move the shorter side) | O(n) / O(1) |
| M05 | 3Sum | 15 | Sort + fix one + two pointers | O(n²) / O(1) |
| M06 | Product of Array Except Self | 238 | Prefix × suffix | O(n) / O(1) |
| M07 | Subarray Sum Equals K | 560 | **Prefix sum + HashMap** | O(n) / O(n) |
| M08 | Rotate Image | 48 | Transpose + reverse rows | O(n²) / O(1) |
| M09 | Set Matrix Zeroes | 73 | First row/column as markers | O(mn) / O(1) |
| M10 | Spiral Matrix | 54 | Shrinking boundaries | O(mn) / O(1) |
| M11 | Minimum Size Subarray Sum | 209 | Sliding window (shrink while valid) | O(n) / O(1) |
| M12 | Max Consecutive Ones III | 1004 | Sliding window (shrink while invalid) | O(n) / O(1) |
| M13 | Longest Consecutive Sequence | 128 | HashSet + start-of-run trick | O(n) / O(n) |
| M14 | Subarray Product Less Than K | 713 | Sliding window + count ending at right | O(n) / O(1) |
| M15 | Majority Element II | 229 | Boyer-Moore with 2 candidates | O(n) / O(1) |
| M16 | Find All Duplicates in an Array | 442 | Index marking | O(n) / O(1) |
| M17 | Fruit Into Baskets | 904 | Sliding window + frequency map | O(n) / O(1) |

## 04-Hard (5 problems)

| # | Problem | LC | Pattern | Time / Space |
|---|---|---|---|---|
| H01 | Trapping Rain Water | 42 | Prefix/suffix max → two pointers | O(n) / O(1) |
| H02 | Sliding Window Maximum | 239 | **Monotonic deque** | O(n) / O(k) |
| H03 | Largest Rectangle in Histogram | 84 | **Monotonic stack** | O(n) / O(n) |
| H04 | Shortest Subarray with Sum ≥ K | 862 | Prefix sums + monotonic deque | O(n) / O(n) |
| H05 | Subarrays with K Different Integers | 992 | exactly(k) = atMost(k) − atMost(k−1) | O(n) / O(k) |

## Pattern → problems (for revision)

| Pattern | Problems |
|---|---|
| Hashing | E01, E02, E11, M07, M13 |
| Prefix sum | E03, E04, E21, M06, M07, H04 |
| Two pointers: opposite ends | E09, E18, M03, M04, M05, H01 |
| Two pointers: slow/fast | E06, E07, E24 |
| Sliding window: fixed | E23, H02 |
| Sliding window: variable | M11, M12, M14, M17, H05 |
| Kadane / running best | E05, M01 |
| Index marking / in-place hashing | E16, M16 |
| Voting / bit tricks | E13, M15, E10, E14 |
| Matrix | M08, M09, M10 |
| Monotonic stack / deque | H02, H03, H04 |
| Greedy / counting sort | E25, E26, E27 |

String problems from `leetcode.md` (Valid Anagram, Valid Palindrome, Longest Substring…) are in [02-Strings](../02-Strings/). They use these same patterns.
