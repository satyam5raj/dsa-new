# DSA: Data Structures, Patterns & Problem Solving

Each data structure goes from **beginner to advanced**:
1. **Basics:** how it works inside, plus a from-scratch implementation
2. **Patterns:** the techniques the problems use
3. **Easy → Medium → Hard problems** from [`leetcode.md`](../../leetcode.md). Each has the brute force, the key insight, the optimal solution, a dry run, the complexity, and tests that check themselves.

Run any file with `java FileName.java`. Every problem prints PASS/FAIL for its test cases.

## Roadmap

| # | Topic | Folder | Status |
|---|---|---|---|
| 0 | Time & Space Complexity | [00-Time-Space-Complexity/](00-Time-Space-Complexity/) | ✅ |
| 1 | Arrays: static + dynamic, patterns, 49 problems | [01-Arrays/](01-Arrays/) | ✅ |
| 2 | Strings: fundamentals, patterns, KMP / Rabin-Karp / Z, 27 problems | [02-Strings/](02-Strings/) | ✅ |
| 3 | Linked Lists: singly, doubly, circular, patterns, 24 problems | [03-Linked-Lists/](03-Linked-Lists/) | ✅ |
| 4 | Stack: implementations, monotonic stack, expression parsing, 24 problems | [04-Stack/](04-Stack/) | ✅ |
| 5 | Queue: linked, circular, deque, priority queue (heap), BFS, monotonic deque, 13 problems | [05-Queue/](05-Queue/) | ✅ |
| 6 | Hash Table: custom, with collision handling | `06-Hash-Table/` | ✅ |
| 7 | Trees: BT → BST → AVL → Segment Tree / Fenwick | `07-Trees/` | ✅ |
| 8 | Heap: min / max | `08-Heap/` | ✅ |
| 9 | Graph: adjacency list / matrix | `09-Graph/` | ✅ |
| 10 | Trie | `10-Trie/` | ✅ |
| 11 | Union-Find / Disjoint Set | `11-Union-Find/` | ✅ |
| 12 | Advanced: LRU Cache, Skip List, Bloom Filter | `12-Advanced/` | ✅ |
| 13 | Recursion: call stack, patterns, divide & conquer (merge / quick sort) | `13-Recursion/` | ✅ |
| 14 | Backtracking: subsets, permutations, combinations, N-Queens, Sudoku | `14-Backtracking/` | ✅ |
| 15 | Dynamic Programming: 1D, grid, knapsack, LIS/LCS, intervals, state machines, bitmask | `15-Dynamic-Programming/` | ✅ |

## 00 · Time & Space Complexity

| File | What you learn |
|---|---|
| `ComplexityBasics` | Big-O rules, O(1) → O(n!) with code for each, **measured** growth (when n doubles, O(n²) time grows 4×), reading constraints to guess the expected complexity |
| `SpaceAndAdvancedComplexity` | auxiliary space, recursion stack space, recursion time (T(n)=2T(n/2)+n), **amortized** analysis, data-structure cost cheat sheet, a 10-question practice quiz |
