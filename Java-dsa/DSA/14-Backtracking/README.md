# 14 · Backtracking

**Recursion + undo.** Build a solution one choice at a time. Explore each choice depth-first, then **undo** it so the next option starts from a clean state. It enumerates a decision tree, so it is exponential by nature. **Pruning** (cutting branches that can't succeed) is what makes it usable.

```
backtrack(state, path):
    if path is complete: record a COPY of path
    for each choice:
        if not allowed: continue          // prune
        choose  → backtrack(...) → un-choose
```

Run any file with `java FileName.java`, or every problem with `./run-all.sh`. ⭐ = not in `leetcode.md`, added for a key technique.

**Study notes:** every problem file has a companion `.md` with the same name (e.g. `02-Easy/E01_BinaryWatch.md`). Each note covers the problem, how to think about it, the Java concepts it uses (E01 starts with the backtracking template, the three families, recording a copy and pruning), a line-by-line walkthrough of every approach, dry-run tables, edge cases, and interview questions with follow-ups.

## 01-Basics: start here

| File | What you learn |
|---|---|
| `BacktrackingTemplate` | the template; the **3 families**: subsets (record at every node), combinations (fixed size / target, `start` index), permutations (`used[]`); **duplicates** via sort + skip; the missing-copy bug, demonstrated |
| `PruningAndConstraints` | the kinds of pruning (constraint check, bounds, feasibility, ordering, symmetry); **measured**: N-Queens n=8 visits 19 million nodes without pruning vs 2,057 with; `break` vs `continue` on sorted candidates |

## 02-Easy (2)

| # | Problem | LC | Pattern |
|---|---|---|---|
| E01 | ⭐ Binary Watch | 401 | Choose k LEDs (vs just enumerating 720 times) |
| E02 | ⭐ Sum of All Subset XOR Totals | 1863 | Pick / not pick carrying a value (+ O(n) trick) |

## 03-Medium (14)

| # | Problem | LC | Pattern |
|---|---|---|---|
| M01 | Subsets | 78 | Subsets family (+ bitmask, iterative) |
| M02 | Subsets II | 90 | Sort + `i > start && a[i] == a[i-1]` skip |
| M03 | ⭐ Combinations | 77 | Fixed size k + "enough numbers left" pruning |
| M04 | Permutations | 46 | `used[]` (+ swap version) |
| M05 | Permutations II | 47 | Sort + `!used[i-1]` skip |
| M06 | Combination Sum | 39 | Reuse (recurse with `i`) + `break` on overshoot |
| M07 | Combination Sum II | 40 | Once each (`i+1`) + duplicate skip |
| M08 | Letter Combinations of a Phone Number | 17 | One level per digit (cartesian product) |
| M09 | Generate Parentheses | 22 | Only valid moves (`open < n`, `close < open`) → Catalan |
| M10 | Palindrome Partitioning | 131 | **Partition** template + palindrome table |
| M11 | Restore IP Addresses | 93 | Partition into 4 with validity + length pruning |
| M12 | Partition to K Equal Sum Subsets | 698 | **Buckets**, biggest first, symmetry pruning |
| M13 | Beautiful Arrangement | 526 | Permutations checked while building |
| M14 | Matchsticks to Square | 473 | Buckets with k = 4 |

## 04-Hard (4)

| # | Problem | LC | Pattern |
|---|---|---|---|
| H01 | N-Queens | 51 | One queen per row + O(1) column/diagonal arrays |
| H02 | N-Queens II | 52 | **Bitmask** backtracking (8× faster at n = 13) |
| H03 | Sudoku Solver | 37 | Bitmask constraints + **most constrained cell first** (6× fewer nodes) |
| H04 | Expression Add Operators | 282 | Evaluate on the fly; `*` undoes the last term |

**From `leetcode.md`, solved elsewhere:** Word Search → [Trie M08](../10-Trie/03-Medium/M08_WordSearch.java) (and Word Search II → [Trie H01](../10-Trie/04-Hard/H01_WordSearchII.java)), Different Ways to Add Parentheses → [Recursion M05](../13-Recursion/03-Medium/M05_DifferentWaysToAddParentheses.java), Gray Code → [Recursion M06](../13-Recursion/03-Medium/M06_GrayCode.java), Path Sum II → [Trees M06](../07-Trees/03-Medium/M06_PathSumII.java), All Paths From Source to Target → [Graph M23](../09-Graph/03-Medium/M23_AllPathsFromSourceToTarget.java).

## Which family? The cheat sheet

| The problem says… | Loop | Recurse with | Record when |
|---|---|---|---|
| all subsets / subsequences | `i = start..n-1` | `i + 1` | every node |
| combinations of size k / sum = target | `i = start..n-1` | `i + 1` (or `i` if reuse) | size k / remaining 0 |
| all orderings | `i = 0..n-1`, skip `used` | same depth + 1 | path size n |
| split a string into valid pieces | `end = start..n-1` | `end + 1` | `start == n` |
| fill a board / grid | each candidate for the next cell | next cell | board full |
| input has duplicates | sort, skip equal neighbours at the same level | | |

## Interview questions

1. Backtracking vs plain recursion vs DP: what's the difference? (undo choices / exhaustive search / overlapping subproblems)
2. Why must you add a **copy** of the path to the results?
3. How do you avoid duplicate results when the input has duplicates, without a Set?
4. Time complexity of subsets, permutations, N-Queens? Why is it fine to be exponential here?
5. What pruning techniques do you know? Give an example of each.
6. How would you count solutions instead of listing them, and when does that turn into DP?
