# 04 · Stack

LIFO: push, pop and peek, all O(1). Reach for a stack whenever you need **"the most recent unresolved thing"**.
In Java use `Deque<Integer> stack = new ArrayDeque<>()`, never `java.util.Stack`.

Run any file with `java FileName.java`, or everything with `./run-all.sh`. ⭐ = not in `leetcode.md`, added for a key technique.

**Study notes:** every problem file has a companion `.md` with the same name (e.g. `02-Easy/E01_ValidParentheses.md`). Each note covers the problem, how to think about it, the Java concepts it uses (E01 starts with stack basics and why `ArrayDeque` beats `Stack`), a line-by-line walkthrough of every approach, dry-run tables, edge cases, and interview questions with follow-ups.

## 01-Basics: start here

| File | What you learn |
|---|---|
| `StackImplementations` | array-backed (growing) and linked stacks from scratch, underflow, why `ArrayDeque` beats `java.util.Stack`. Both checked against `ArrayDeque` with 10,000 random operations |
| `StackPatterns` | the 7 stack patterns: matching, cancel/collapse, **monotonic stack (all 4 variants)**, expression evaluation (**shunting-yard** + postfix evaluation), nested structure, augmented stack, **recursion → explicit stack** (avoids a StackOverflowError at depth 1,000,000) |

## 02-Easy (5)

| # | Problem | LC | Pattern |
|---|---|---|---|
| E01 | Valid Parentheses | 20 | Matching |
| E02 | Implement Queue using Stacks | 232 | Two stacks, amortized O(1) |
| E03 | Next Greater Element I | 496 | **First monotonic stack** + map |
| E04 | ⭐ Remove All Adjacent Duplicates (+ #1209 k version) | 1047 | Cancel with the top |
| E05 | ⭐ Backspace String Compare | 844 | Stack → O(1) two pointers |

## 03-Medium (15)

| # | Problem | LC | Pattern |
|---|---|---|---|
| M01 | Min Stack | 155 | Augmented stack (value, min) |
| M02 | Daily Temperatures | 739 | Monotonic stack of indices |
| M03 | Next Greater Element II | 503 | Monotonic stack, circular (2n loop) |
| M04 | Evaluate Reverse Polish Notation | 150 | Operand stack |
| M05 | Decode String | 394 | Save/restore context (+ recursive) |
| M06 | Basic Calculator II | 227 | Stack of terms (+ O(1) version) |
| M07 | Online Stock Span | 901 | Monotonic stack merging spans |
| M08 | Simplify Path | 71 | Stack of directory names |
| M09 | Remove K Digits | 402 | Greedy + increasing stack |
| M10 | Score of Parentheses | 856 | Score per level / 2^depth |
| M11 | Car Fleet | 853 | Sort + stack of arrival times |
| M12 | Asteroid Collision | 735 | Fight the top, repeatedly |
| M13 | Exclusive Time of Functions | 636 | Simulate the call stack |
| M14 | Design Browser History | 1472 | Two stacks / array + cursor |
| M15 | ⭐ Sum of Subarray Minimums | 907 | **Contribution technique** (prev/next smaller) |

## 04-Hard (4)

| # | Problem | LC | Pattern |
|---|---|---|---|
| H01 | Max Stack | 716 | Doubly linked list + TreeMap (O(log n) popMax) |
| H02 | ⭐ Maximal Rectangle | 85 | Row → histogram → monotonic stack |
| H03 | ⭐ Basic Calculator | 224 | Save (result, sign) at each '(' |
| H04 | ⭐ Longest Valid Parentheses | 32 | Index stack with a barrier |

**From `leetcode.md`, solved elsewhere:** Largest Rectangle in Histogram → [Arrays H03](../01-Arrays/04-Hard/H03_LargestRectangleInHistogram.java), Sliding Window Maximum → [Arrays H02](../01-Arrays/04-Hard/H02_SlidingWindowMaximum.java). Implement Stack using Queues, Design Circular Queue/Deque, Moving Average and Hit Counter → **05-Queue**.

## Monotonic stack cheat sheet

| You want… | Scan | Pop while… | Stack holds |
|---|---|---|---|
| next **greater** | left → right | `top < x` | decreasing values |
| next **smaller** | left → right | `top > x` | increasing values |
| previous greater / smaller | left → right | same pops, then read the **top before pushing** | — |
| with duplicates | use `<` on one side and `<=` on the other so equal elements are counted once (M15) | | |

Store **indices** whenever the answer involves distances or widths (M02, H02).
