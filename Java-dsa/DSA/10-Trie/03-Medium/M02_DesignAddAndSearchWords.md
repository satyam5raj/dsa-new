# M02 · Design Add and Search Words Data Structure ⭐

| | |
|---|---|
| **LeetCode** | [#211 Design Add and Search Words Data Structure](https://leetcode.com/problems/design-add-and-search-words-data-structure/) |
| **Difficulty** | Medium |
| **Pattern** | A trie + wildcard DFS ('.' = try every child) |
| **Code** | [M02_DesignAddAndSearchWords.java](M02_DesignAddAndSearchWords.java) |
| **Run** | `java M02_DesignAddAndSearchWords.java` |

⭐ = a bonus classic. The plain trie is [M01](M01_ImplementTrie.md).

---

## 1. The problem

`addWord(word)`; `search(pattern)`, where '.' matches **any one** letter.

```
add bad, dad, mad
search("pad")  → false
search("bad")  → true
search(".ad")  → true
search("b..")  → true
search("...")  → true
search("....") → false   (too long)
```

---

## 2. How to think about it

### A HashSet + brute force
For every stored word of the same length, compare character by character: O(N · L) per search.

### The trie
A normal letter follows **exactly one** child. A '.' must try **all** the existing children: recursion with branching.

```
dfs(node, i):
    if i == len:  return node.isEnd
    c = pattern[i]
    if c == '.':  return any child for which dfs(child, i + 1) is true
    else:         return child[c] exists && dfs(child[c], i + 1)
```

Without dots it's O(L). With dots it can branch up to 26 ways per dot, but the trie only contains children that actually exist, so in practice it's fast.

```
root ─┬─ b ─ a ─ d*
      ├─ d ─ a ─ d*
      └─ m ─ a ─ d*

search(".ad"): '.' tries b → a → d* ✓ (stops at the first success)
```

### An optimisation
Group the words by **length** (a separate trie per length, or check the length first): a pattern of length 3 never needs to look at longer words.

---

## 3. Java concepts used in this problem

### 3.1 Recursion with an index instead of substrings

```java
private boolean dfs(Node n, String w, int i)
```

Passing the position `i` avoids creating substrings (`w.substring(1)` would cost O(L) at every step).

### 3.2 Short-circuiting the branching

```java
for (Node child : n.next) if (child != null && dfs(child, w, i + 1)) return true;
```

The first successful branch returns immediately; the remaining children aren't explored.

### 3.3 A labelled `continue` (in the brute force)

```java
outer:
for (String w : words) {
    ...
    for (...) if (mismatch) continue outer;      // skip to the next WORD, not the next character
    return true;
}
```

`continue outer` jumps to the next iteration of the **outer** loop, which a plain `continue` can't do from inside the inner loop.

---

## 4. Approach 1: Match against every stored word (the referee)

### Code ([M02_DesignAddAndSearchWords.java:69-77](M02_DesignAddAndSearchWords.java#L69-L77))

```java
static boolean bruteMatch(Set<String> words, String p) {
    outer:
    for (String w : words) {
        if (w.length() != p.length()) continue;
        for (int i = 0; i < w.length(); i++) if (p.charAt(i) != '.' && p.charAt(i) != w.charAt(i)) continue outer;
        return true;
    }
    return false;
}
```

| Line | What it does |
|---|---|
| the length check | a different length can never match |
| the inner loop | a '.' matches anything; any other letter must be equal |
| `continue outer` | a mismatch → try the next stored word |

**O(N · L)** per search.

---

## 5. Approach 2: Trie + wildcard DFS (the file's solution)

### Code ([M02_DesignAddAndSearchWords.java:38-67](M02_DesignAddAndSearchWords.java#L38-L67))

```java
static class WordDictionary {
    private static class Node {
        Node[] next = new Node[26];
        boolean end;
    }

    private final Node root = new Node();

    public void addWord(String word) {
        Node n = root;
        for (char c : word.toCharArray()) {
            if (n.next[c - 'a'] == null) n.next[c - 'a'] = new Node();
            n = n.next[c - 'a'];
        }
        n.end = true;
    }

    public boolean search(String word) { return dfs(root, word, 0); }

    private boolean dfs(Node n, String w, int i) {
        if (i == w.length()) return n.end;
        char c = w.charAt(i);
        if (c == '.') {
            for (Node child : n.next) if (child != null && dfs(child, w, i + 1)) return true;   // try every branch
            return false;
        }
        Node child = n.next[c - 'a'];
        return child != null && dfs(child, w, i + 1);
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `addWord` | a standard trie insert |
| `i == w.length()` | the whole pattern is consumed: a match only if a word ends here |
| `c == '.'` | try every existing child for this position |
| a letter | follow exactly that child (if it exists) |

### Dry run: search("b..")

| call | node | char | action |
|---|---|---|---|
| dfs(root, 0) | root | 'b' | follow b |
| dfs(b, 1) | b | '.' | try b's children: only a |
| dfs(a, 2) | a | '.' | try a's children: only d |
| dfs(d, 3) | d | (end) | d.end = true → **true** |

### Dry run: search("....")
'.' → b → '.' → a → '.' → d → '.' → d has no children → false for every branch → **false** ✓

### Complexity
- **addWord O(L).**
- **search O(L)** without dots; up to **O(26^dots · L)** in the worst case.

---

## 6. Comparing the approaches

| | Match every word | Trie + DFS |
|---|---|---|
| add | O(1) | O(L) |
| search without dots | O(N · L) | **O(L)** |
| search with dots | O(N · L) | O(26^dots · L) worst case, usually much less |

The file checks the trie against brute matching on 5,000 random operations.

---

## 7. Edge cases and common mistakes

**Edge cases:** a pattern of only dots, a pattern longer than every word, words that are prefixes of other words.

**Mistakes:**
1. Treating '.' as "zero or more letters" (it's exactly one).
2. Returning true at the end of the pattern without checking `end`.
3. Creating nodes while searching.

---

## 8. Interview questions and answers

**Q1. What's the worst case for a search?**
> A pattern of all dots on a dense trie visits every node up to that depth: O(26^L) in theory, bounded by the number of stored nodes in practice.

**Q2. How would you speed up patterns with many dots?**
> Keep separate tries (or word lists) per length; or, for patterns that are mostly dots, scan only the words of that length.

### Follow-up questions

**F1. Support '*' (any sequence).**
> At a '*', try "matches nothing" (advance the pattern) and "matches one more letter" (follow every child, keeping the '*'), like regex DP on a trie.

**F2. Word Search II.** → [H01](../04-Hard/H01_WordSearchII.md)
> DFS on a grid guided by a trie.

**F3. Count the matching words instead of a boolean.**
> Sum the counts over all the successful branches (no short-circuiting).

---

## 9. Running the file

```bash
java M02_DesignAddAndSearchWords.java      # 7/7 tests passed (including 5,000 random ops vs brute matching)
```

**One-line summary:** *"Store the words in a trie; search with DFS that follows one child for a letter and tries every existing child for a '.', succeeding at the pattern's end only on an end-of-word node."*
