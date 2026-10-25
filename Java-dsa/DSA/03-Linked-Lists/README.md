# 03 · Linked Lists

Nodes connected by pointers: O(1) insert/delete when you hold the right node, O(n) to find anything.
Linked list problems are really **pointer-manipulation drills**. Draw boxes and arrows for every problem.

Run any file with `java FileName.java`, or everything with `./run-all.sh`. ⭐ = a classic not in `leetcode.md`, added for a key technique.

**Study notes:** every problem file has a companion `.md` with the same name (e.g. `02-Easy/E01_ReverseLinkedList.md`). Each note covers the problem, how to think about it, the Java concepts it uses (E01 starts with the `ListNode` basics: references, `null`, dummy nodes), a line-by-line walkthrough of every approach, dry-run tables, edge cases, and interview questions with follow-ups.

## 01-Basics: start here

| File | What you learn |
|---|---|
| `SinglyLinkedList` | built from scratch: add/remove at head, tail and index, search, **iterative and recursive reverse**, iterator, array vs linked list trade-offs. Checked against `java.util.LinkedList` with 5,000 random operations |
| `DoublyLinkedList` | **sentinel head/tail**, O(1) remove(node) and moveToFront (the core of an LRU cache), reverse, forward/backward traversal. Checked with 5,000 random operations (this test caught an empty-list bug, explained in the code) |
| `CircularLinkedList` | tail-only representation, O(1) rotate (round robin), safe traversal with do-while, the **Josephus problem** (simulation vs formula) |
| `LinkedListPatterns` | the 6 patterns: dummy head, fast & slow pointers, in-place reversal, merge, split + reconnect, node → node map |

## 02-Easy (8)

| # | Problem | LC | Pattern |
|---|---|---|---|
| E01 | Reverse Linked List | 206 | **Reversal** (iterative + recursive) |
| E02 | Merge Two Sorted Lists | 21 | Dummy + tail pointer |
| E03 | Linked List Cycle | 141 | Fast & slow (Floyd) |
| E04 | Intersection of Two Linked Lists | 160 | Switching pointers |
| E05 | Palindrome Linked List | 234 | Middle + reverse half + compare |
| E06 | Remove Linked List Elements | 203 | Dummy head |
| E07 | Middle of the Linked List | 876 | Fast & slow (first/second middle) |
| E08 | ⭐ Remove Duplicates from Sorted List (+ #82) | 83 | Compare with next / dummy |

## 03-Medium (14)

| # | Problem | LC | Pattern |
|---|---|---|---|
| M01 | Linked List Cycle II (+ Find the Duplicate Number) | 142 | Floyd phase 2 (with the proof) |
| M02 | Remove Nth Node From End | 19 | Fixed gap + dummy |
| M03 | Add Two Numbers | 2 | Digit addition with carry |
| M04 | Reorder List | 143 | Middle + reverse + weave |
| M05 | Copy List with Random Pointer | 138 | HashMap / interleaving (O(1) space) |
| M06 | Rotate List | 61 | Make a circle, cut |
| M07 | Partition List | 86 | Two dummy chains (+ the cycle bug) |
| M08 | Swap Nodes in Pairs | 24 | Rewire 3 pointers per pair |
| M09 | Flatten a Multilevel Doubly Linked List | 430 | DFS with a stack |
| M10 | Design Linked List | 707 | Sentinel + size |
| M11 | Delete Node in a Linked List | 237 | Copy-next trick |
| M12 | Odd Even Linked List | 328 | Two interleaved chains |
| M13 | ⭐ Reverse Linked List II | 92 | Sub-list reversal (head insertion) |
| M14 | ⭐ Sort List | 148 | Merge sort on a list |

## 04-Hard (2)

| # | Problem | LC | Pattern |
|---|---|---|---|
| H01 | Reverse Nodes in k-Group | 25 | Check k ahead → reverse group → reconnect |
| H02 | Merge k Sorted Lists | 23 | Min-heap / divide & conquer, O(N log k) |

## Checklist before you run any linked-list code
1. Empty list? One node? Two nodes?
2. Can the **head** change? Then use a **dummy** node.
3. Before overwriting `x.next`, did you **save** what it pointed to?
4. After splitting, did you **cut** (`tail.next = null`) so no stale link creates a cycle?
5. Compare **nodes** (`==`), not values, when asking "same node?"

LRU Cache (HashMap + doubly linked list) comes in **12-Advanced**, built on `DoublyLinkedList`.
