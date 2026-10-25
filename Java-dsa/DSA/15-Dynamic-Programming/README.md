# 15 · Dynamic Programming

**Recursion + a cache.** When the brute-force recursion solves the *same subproblem* again and again (overlapping subproblems), and the best answer is built from best answers to smaller pieces (optimal substructure), store each answer once. Exponential becomes polynomial.

```
1. STATE       what does dp[i][j] MEAN? (one English sentence)
2. TRANSITION  how is it built from smaller states? (look at the LAST choice)
3. BASE        the smallest states you know directly
4. ORDER       fill so every input is ready first
5. ANSWER      which cell? (dp[n], max over all dp[i], dp[0][n-1], ...)
then: memo → table → keep only the rows/values the transition reads
```

Run any file with `java FileName.java`, or every problem with `./run-all.sh`. ⭐ = not in `leetcode.md`, added for a key technique.

**Study notes:** every problem file has a companion `.md` with the same name (e.g. `02-Easy/E01_ClimbingStairs.md`). Each note covers the problem, how to think about it (state, transition, base, order, answer), the Java concepts it uses (E01 starts with the DP recipe and memo vs table vs rolling variables), a line-by-line walkthrough of every approach, dry-run tables, edge cases, and interview questions with follow-ups.

## 01-Basics: start here

| File | What you learn |
|---|---|
| `DPFoundations` | the 5-step recipe; one problem in **4 versions** (recursion 9.1 M calls → memo 76 → table → O(1) space); top-down's stack-overflow limit; the **wrong-sentinel** memo bug (7 M calls vs 63); **reconstructing** the choices with parent pointers |
| `KnapsackPatterns` | 0/1 knapsack 2-D → 1-D; why the capacity loop runs **backwards** (forwards = unbounded, shown); unbounded knapsack; **combinations vs permutations** (which loop is outside), with both lists printed; walking the table back to find the chosen items |
| `DPPatterns` | the 8 shapes: linear, grid, two sequences (LCS), LIS (O(n²) and O(n log n)), knapsack, **interval** (matrix chain), **state machine** (stock), **bitmask** (travelling salesman). Each one is checked against brute force |

## 02-Easy (3)

| # | Problem | LC | Pattern |
|---|---|---|---|
| E01 | Climbing Stairs | 70 | dp[i] = dp[i-1] + dp[i-2] (Fibonacci) |
| E02 | ⭐ Min Cost Climbing Stairs | 746 | Same shape with `min`: the state definition decides the base case |
| E03 | ⭐ Counting Bits | 338 | Smaller subproblem = smaller number: `dp[i >> 1] + (i & 1)` |

## 03-Medium (28)

| # | Problem | LC | Pattern |
|---|---|---|---|
| M01 | House Robber | 198 | Take or skip: `max(dp[i-1], dp[i-2] + a[i])` |
| M02 | House Robber II | 213 | Circle → two lines (skip the first or the last) |
| M03 | Decode Ways | 91 | Climbing Stairs with validity rules (zeros!) |
| M04 | Maximum Product Subarray | 152 | Kadane with **max and min** (a negative flips them) |
| M05 | Jump Game | 55 | DP that collapses into greedy (farthest reach) |
| M06 | Jump Game II | 45 | Min-steps DP → BFS by windows, O(n) |
| M07 | Integer Break | 343 | `j * max(i-j, dp[i-j])` + the "use 3s" math |
| M08 | Perfect Squares | 279 | Unbounded knapsack, min count (+ Lagrange/Legendre check) |
| M09 | Coin Change | 322 | Unbounded knapsack, min coins; greedy fails |
| M10 | Coin Change II | 518 | Count **combinations**: coins in the outer loop |
| M11 | Partition Equal Subset Sum | 416 | 0/1 subset sum to total/2 (+ bitset trick) |
| M12 | Target Sum | 494 | Algebra → count subsets with sum (total+target)/2 |
| M13 | Word Break | 139 | `ok[i]` over prefixes (the "evil" input: 3.1 M recursive calls vs 24 cells) |
| M14 | Unique Paths | 62 | Grid counting, one row; C(m+n-2, m-1) |
| M15 | Unique Paths II | 63 | Obstacles = 0 ways (the first-row bug) |
| M16 | Minimum Path Sum | 64 | Grid min + path reconstruction |
| M17 | Triangle | 120 | Fill **bottom-up**: no edge cases, answer at the apex |
| M18 | Minimum Falling Path Sum | 931 | Three predecessors → two rows |
| M19 | Longest Increasing Subsequence | 300 | "Ending at i" O(n²), then **tails + binary search** O(n log n) |
| M20 | Largest Divisible Subset | 368 | Sort → LIS with "divides" + parent pointers |
| M21 | Longest Common Subsequence | 1143 | **The two-sequence table** (+ one row + diagonal, + the LCS itself) |
| M22 | Edit Distance | 72 | Diagonal / up / left = replace / delete / insert |
| M23 | Interleaving String | 97 | State (i, j); the third index is i + j |
| M24 | Palindromes the DP way | 5, 647 | Interval DP `pal[i][j]` (+ longest palindromic subsequence) |
| M25 | Stock with Cooldown | 309 | **State machine**: hold / sold / rest |
| M26 | Stock with Transaction Fee | 714 | State machine: hold / free |
| M27 | Minimum Cost Tree From Leaf Values | 1130 | Interval DP over the root split → O(n) monotonic stack |
| M28 | Stone Game | 877 | Game DP: store **my score − yours** |

## 04-Hard (11)

| # | Problem | LC | Pattern |
|---|---|---|---|
| H01 | Burst Balloons | 312 | Interval DP where k is the **last** balloon burst |
| H02 | Distinct Subsequences | 115 | Two-sequence counting, one row going backwards |
| H03 | Russian Doll Envelopes | 354 | Sort (w ↑, h ↓), then LIS on heights |
| H04 | Regular Expression Matching | 10 | `x*` = zero copies `dp[i][j-2]` or one more `dp[i-1][j]` (checked against java.util.regex) |
| H05 | Wildcard Matching | 44 | `*` = empty or eat one; + O(1)-space greedy |
| H06 | Scramble String | 87 | 3-D interval state (i, j, len) + anagram pruning |
| H07 | Dungeon Game | 174 | Grid DP **backwards** (the constraint is a running minimum) |
| H08 | Decode Ways II | 639 | Decode Ways × "how many codes fit here", mod 1e9+7 |
| H09 | Maximum Profit in Job Scheduling | 1235 | Sort by end + binary search for the last compatible job |
| H10 | Count Vowels Permutation | 1220 | State = last letter (+ matrix power for n = 10¹⁸) |
| H11 | ⭐ Best Time to Buy and Sell Stock IV | 188 | `buy[j]` / `sell[j]` for j ≤ k transactions: covers the whole stock family |

**From `leetcode.md`, solved elsewhere:**
- Palindromic Substrings / Longest Palindromic Substring with expand-around-center → [Strings M10](../02-Strings/03-Medium/M10_LongestPalindromicSubstring.java) / [M11](../02-Strings/03-Medium/M11_PalindromicSubstrings.java) (the DP view is M24 here)
- House Robber III (DP on trees) → [Trees M13](../07-Trees/03-Medium/M13_HouseRobberIII.java)
- Maximum Subarray (Kadane) → [Arrays M01](../01-Arrays/03-Medium/M01_MaximumSubarray.java)
- Jump Game VI and Constrained Subsequence Sum (DP + monotonic deque) → [Queue](../05-Queue/)
- Digit DP → [Recursion H03 Number of Digit One](../13-Recursion/04-Hard/H03_NumberOfDigitOne.java)
- Word Break for every word → [Trie H04 Concatenated Words](../10-Trie/04-Hard/H04_ConcatenatedWords.java)

## Which pattern? The cheat sheet

| The problem says… | State | Typical transition |
|---|---|---|
| ways / best up to index i, can't take neighbours | `dp[i]` | `max(dp[i-1], dp[i-2] + a[i])` |
| move right/down in a grid | `dp[r][c]` | from above + from the left |
| two strings: match / align / transform | `dp[i][j]` on prefixes | equal → diagonal; else combine up / left |
| longest increasing / chain | `dp[i]` = best **ending at** i | scan j < i, or tails + binary search |
| pick items under a capacity / reach a sum | `dp[c]` | 0/1: c going **down**; unbounded: c going **up** |
| best for a range i..j, by splitting | `dp[i][j]` | try every split k, shorter ranges first |
| days with modes (holding, cooldown, k trades) | one variable per mode | each arrow in the state diagram = one term |
| choose a subset of n ≤ 20 things | `dp[mask][last]` | add one unvisited element |
| two players, optimal play | `dp[i][j]` = my score − yours | `max(take - dp[rest])` |

## Interview questions

1. DP vs divide & conquer vs greedy: what's the difference? When does greedy fail? Give an example.
2. Memoization vs tabulation: pros and cons? When would top-down visit far fewer states?
3. Why does the 0/1 knapsack's 1-D loop go backwards? What happens if it goes forwards?
4. Coin Change II vs Combination Sum IV: why does swapping the loops change the answer?
5. How do you reduce O(n·m) space to O(m)? When do you need an extra "diagonal" variable?
6. How do you recover the actual solution (path, items, subsequence), not just its value?
7. LIS in O(n log n): what does `tails` hold, and why is it not the subsequence itself?
8. In interval DP, why choose the *last* operation (Burst Balloons) instead of the first?
9. Dungeon Game: why can't you fill the table from the start?
10. Derive the stock state machine for "at most k transactions with a cooldown".
