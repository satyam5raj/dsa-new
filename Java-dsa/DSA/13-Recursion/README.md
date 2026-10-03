# 13 · Recursion

A function that solves a problem by calling itself on a **smaller** version, until a **base case** answers directly. Recursion is the foundation of everything left in the course:
- **Divide and conquer** (merge sort, quick sort, fast power)
- **Backtracking** (topic 14) = recursion plus "undo"
- **Dynamic programming** (topic 15) = recursion plus memory

It also already powered tree and graph DFS.

Run any file with `java FileName.java`, or every problem with `./run-all.sh`. ⭐ = not in `leetcode.md`, added for a key technique.

## 01-Basics: start here

| File | What you learn |
|---|---|
| `RecursionBasics` | base case + recursive case, the **leap of faith**, the call stack (traced live), work before vs after the call, tail recursion (not optimized in Java), all 10 warm-ups from `leetcode.md` (print 1..n / n..1, sum, factorial, Fibonacci, power, reverse number, digit sum, GCD, Tower of Hanoi), and a real `StackOverflowError` (~35k frames) |
| `RecursionPatterns` | the 5 shapes: linear, index-based (no copies), **divide & conquer**, **tree recursion** (exponential: 2.7M calls for fib(30)), **pick / not pick**; finding the complexity from the **recursion tree** and the Master Theorem |

## 02-Easy (4)

| # | Problem | LC | Pattern |
|---|---|---|---|
| E01 | ⭐ Fibonacci Number | 509 | Plain → memo → table → two variables (the road to DP) |
| E02 | ⭐ Reverse String | 344 | Two indices moving inward |
| E03 | ⭐ Power of Two / Three | 231 / 326 | Divide the input (log depth) vs `n & (n-1)` |
| E04 | Tower of Hanoi | (`leetcode.md` #70) | Trust the smaller case; 2ⁿ − 1 moves, validated by simulation |

## 03-Medium (7)

| # | Problem | LC | Pattern |
|---|---|---|---|
| M01 | ⭐ Pow(x, n) | 50 | **Fast exponentiation** (and the `MIN_VALUE` trap) |
| M02 | ⭐ K-th Symbol in Grammar | 779 | Recurse on the parent, never build the row |
| M03 | ⭐ Find the Winner of the Circular Game | 1823 | **Josephus**: solve n−1, shift the answer |
| M04 | ⭐ Sort an Array | 912 | **Merge sort** + **quick sort** (random pivot, 3-way), O(n²) worst case shown |
| M05 | Different Ways to Add Parentheses | 241 | Divide at every operator + memo |
| M06 | Gray Code | 89 | Build n from n−1: reflect + prefix |
| M07 | ⭐ Count Good Numbers | 1922 | Counting → **modular fast power** |

## 04-Hard (3)

| # | Problem | LC | Pattern |
|---|---|---|---|
| H01 | ⭐ Parsing a Boolean Expression | 1106 | **Recursive descent** parser |
| H02 | ⭐ K-th Smallest in Lexicographical Order | 440 | Walk the 10-ary prefix tree, skip whole subtrees |
| H03 | ⭐ Number of Digit One | 233 | Recurse on the leading digit |

**Recursion elsewhere in the course:** tree DFS (all of [07-Trees](../07-Trees/)), graph DFS ([09-Graph](../09-Graph/)), Reverse Linked List recursively ([Linked Lists E01](../03-Linked-Lists/02-Easy/E01_ReverseLinkedList.java)), Decode String ([Stack M05](../04-Stack/03-Medium/M05_DecodeString.java)). Subsets/permutations → **14-Backtracking**. Memoized recursion → **15-Dynamic-Programming**.

## How to write any recursive function

1. **Define** what `f(input)` returns, in one sentence.
2. **Base case**: the smallest input. Make sure every path reaches it.
3. **Leap of faith**: assume `f(smaller)` is correct. How do you build `f(input)` from it?
4. **Shrink** the input on every call (n−1, n/2, a shorter range).
5. **Cost** = number of calls × work per call. **Space** = maximum depth (plus any copies made).
6. **Repeated subproblems?** Memoize them (that's dynamic programming). **Very deep?** Convert to a loop or an explicit stack.

## Interview questions

1. What happens in memory during a recursive call? Why does deep recursion throw `StackOverflowError`?
2. Does Java optimize tail recursion? (No.) How do you convert recursion to iteration?
3. Why is naive recursive Fibonacci O(φⁿ), and how does memoization make it O(n)?
4. Merge sort vs quick sort: stability, worst case, memory, which one Java uses where?
5. Solve T(n) = 2T(n/2) + O(n) and T(n) = T(n/2) + O(1). What are the Master Theorem cases?
6. Why is passing indices better than passing `substring`/`Arrays.copyOfRange` in recursive calls?
