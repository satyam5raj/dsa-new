# 10 · Trie (Prefix Tree)

A trie stores strings character by character along paths from a root, so words that share a prefix **share nodes**. Insert, search and startsWith all cost O(length of the word), **no matter how many words are stored**. That makes it the structure for prefix questions: autocomplete, spell check, word games, longest-prefix routing. A **binary trie**, built over the bits of numbers, solves max-XOR problems.

Run any file with `java FileName.java`, or every problem with `./run-all.sh`. ⭐ = not in `leetcode.md`, added for a key technique. (`leetcode.md` only lists Implement Trie, Word Search and Word Search II, so most problems here are ⭐.)

**Study notes:** every problem file has a companion `.md` with the same name (e.g. `02-Easy/E01_CountingWordsWithPrefix.md`). Each note covers the problem, how to think about it, the Java concepts it uses (E01 starts with what a trie is, the node layout, the standard walk, and storing answers in nodes), a line-by-line walkthrough of every approach, dry-run tables, edge cases, and interview questions with follow-ups.

## 01-Basics: start here

| File | What you learn |
|---|---|
| `TrieBasics` | array-children trie with `isEnd` and **prefix counts**; search vs startsWith; **delete with pruning**; **autocomplete** in sorted order; longest common prefix; a HashMap-children trie for any alphabet. Checked against a `TreeSet` with 20k random operations |
| `TriePatterns` | the 5 trie patterns, with the two less obvious ones coded: **wildcard search** ('.' = try every child) and the **binary trie for max XOR** |

## 02-Easy (2)

| # | Problem | LC | Pattern |
|---|---|---|---|
| E01 | ⭐ Counting Words With a Given Prefix | 2185 | Prefix counts in nodes (pays off with many queries) |
| E02 | ⭐ Check If a Word Occurs As a Prefix | 1455 | Store the answer (first index) in each node |

## 03-Medium (8)

| # | Problem | LC | Pattern |
|---|---|---|---|
| M01 | Implement Trie | 208 | The trie itself: `children[26]` + `isEnd` |
| M02 | ⭐ Design Add and Search Words | 211 | **Wildcard DFS** |
| M03 | ⭐ Replace Words | 648 | Walk the word, stop at the **first** word end |
| M04 | ⭐ Search Suggestions System | 1268 | **Top-3 cached per node** (autocomplete) / sort + binary search |
| M05 | ⭐ Longest Word in Dictionary | 720 | DFS only through word-ending nodes |
| M06 | ⭐ Map Sum Pairs | 677 | Sums in nodes, update by the **delta** |
| M07 | ⭐ Maximum XOR of Two Numbers | 421 | **Binary trie**, greedy opposite bit |
| M08 | Word Search | 79 | Grid backtracking (mark → recurse → unmark) |

## 04-Hard (5)

| # | Problem | LC | Pattern |
|---|---|---|---|
| H01 | Word Search II | 212 | **Trie + grid DFS**, prune dead branches |
| H02 | ⭐ Stream of Characters | 1032 | **Reversed trie**, walk the stream backwards |
| H03 | ⭐ Palindrome Pairs | 336 | Split each word; look up the reversed piece |
| H04 | ⭐ Concatenated Words | 472 | Word break with a trie, shortest words first |
| H05 | ⭐ Maximum XOR With an Element | 1707 | **Offline queries** sorted by limit + growing binary trie |

**Related, solved elsewhere:** Longest Common Prefix → [Strings E11](../02-Strings/02-Easy/E11_LongestCommonPrefix.java) (also in `TrieBasics`).

## Trie or HashSet?

| Need | Use |
|---|---|
| exact membership only | `HashSet` (simpler, often less memory) |
| "any word starting with…", counts per prefix, autocomplete | **trie** |
| words in sorted order | trie (DFS a→z) or `TreeSet` |
| "which stored word is a prefix of this text?" (shortest/longest match) | **trie** (walk once, O(L)) |
| suffix questions on a stream | **reversed trie** |
| max/min XOR between numbers | **binary trie** |
| many words searched in one grid | **trie + DFS** |

## Interview questions

1. Time and space complexity of a trie with N words of average length L? Why is the space often worse than a HashSet?
2. Array children vs HashMap children: trade-offs?
3. How would you implement autocomplete returning the top-k most **popular** completions? (store top-k per node, or a count + heap during DFS)
4. Why does Word Search II need a trie instead of running Word Search for each word?
5. How does a binary trie find the max XOR in O(31) per number?
6. How would you delete a word, and when can nodes be removed?
