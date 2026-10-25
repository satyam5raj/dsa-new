# M01 · Implement Trie (Prefix Tree)

| | |
|---|---|
| **LeetCode** | [#208 Implement Trie (Prefix Tree)](https://leetcode.com/problems/implement-trie-prefix-tree/) |
| **Difficulty** | Medium |
| **Pattern** | The trie itself: children[26] + an isEnd flag |
| **Code** | [M01_ImplementTrie.java](M01_ImplementTrie.java) |
| **Run** | `java M01_ImplementTrie.java` |

Trie basics are explained in [E01](../02-Easy/E01_CountingWordsWithPrefix.md#3-java-concepts-used-in-this-problem); the structure in detail in [01-Basics/TrieBasics](../01-Basics/TrieBasics.java).

---

## 1. The problem

Implement `insert(word)`, `search(word)` and `startsWith(prefix)`, for lowercase letters.

```
insert("apple")
search("apple")     → true
search("app")       → false     ("app" is only a prefix so far)
startsWith("app")   → true
insert("app")
search("app")       → true
```

---

## 2. How to think about it

This is the interview-sized trie you should be able to write in 5 minutes:

```
Node:       Node[26] children, boolean isEnd
insert:     walk, creating the missing nodes; set isEnd at the last node
search:     walk; false if a node is missing; at the end return isEnd
startsWith: walk; false if a node is missing; at the end return true
```

`search` and `startsWith` share the walk, so one helper `find(s)` returns the last node or null. **The only difference between them is the isEnd check.**

```
after insert("apple"):                after insert("app"):
root ─ a ─ p ─ p ─ l ─ e*             root ─ a ─ p ─ p* ─ l ─ e*

search("app"): reaches the 2nd p, isEnd false → false      → later true
```

### Common bugs
- forgetting `isEnd`: `search("app")` returns true after only inserting "apple"
- using `ch - 'A'`, or otherwise getting the index offset wrong

---

## 3. Java concepts used in this problem

### 3.1 A private static nested node class

```java
private static class Node {
    Node[] children = new Node[26];
    boolean isEnd;
}
```

`private` hides the node from users of the Trie; `static` means a Node doesn't hold a reference to its Trie. Fields default to `null` / `false`.

### 3.2 A private helper that returns null for "not found"

```java
private Node find(String s)           // the last node of the path, or null
```

Both public queries reuse it, which removes duplicated walking code.

### 3.3 `startsWith("")` is true

The empty prefix's node is the root, which always exists. The test checks this.

---

## 4. Approach 1: A HashSet of words (brute force)

```java
static class TrieWithSet {
    private final Set<String> words = new HashSet<>();
    public void insert(String w) { words.add(w); }
    public boolean search(String w) { return words.contains(w); }
    public boolean startsWith(String p) {
        for (String w : words) if (w.startsWith(p)) return true;      // O(N · L)
        return false;
    }
}
```

`search` is O(L), but `startsWith` scans every word: **O(N · L)**. That's exactly the weakness a trie fixes. (The file's random test uses this kind of scan as its referee.)

---

## 5. Approach 2: The trie (the file's solution)

### Code ([M01_ImplementTrie.java:36-71](M01_ImplementTrie.java#L36-L71))

```java
static class Trie {
    private static class Node {
        Node[] children = new Node[26];
        boolean isEnd;
    }

    private final Node root = new Node();

    public void insert(String word) {
        Node n = root;
        for (char c : word.toCharArray()) {
            int i = c - 'a';
            if (n.children[i] == null) n.children[i] = new Node();
            n = n.children[i];
        }
        n.isEnd = true;
    }

    public boolean search(String word) {
        Node n = find(word);
        return n != null && n.isEnd;
    }

    public boolean startsWith(String prefix) {
        return find(prefix) != null;
    }

    private Node find(String s) {
        Node n = root;
        for (char c : s.toCharArray()) {
            n = n.children[c - 'a'];
            if (n == null) return null;
        }
        return n;
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `insert`: the loop | follow or create a child for each letter |
| `n.isEnd = true` | mark that a whole word ends here |
| `search` | the path exists **and** a word ends there |
| `startsWith` | the path exists |
| `find` | walk; stop with null at the first missing child |

### Dry run

| operation | path | result |
|---|---|---|
| insert("apple") | creates a, p, p, l, e; e.isEnd = true | |
| search("apple") | reaches e, isEnd true | **true** |
| search("app") | reaches the 2nd p, isEnd false | **false** |
| startsWith("app") | reaches the 2nd p | **true** |
| insert("app") | nodes exist; set p.isEnd = true | |
| search("app") | isEnd true | **true** |

### Complexity
- **Each operation O(L).**
- **Space O(total characters × 26)** worst case.

---

## 6. Comparing the approaches

| | HashSet | Trie |
|---|---|---|
| insert | O(L) | O(L) |
| search | O(L) | O(L) |
| startsWith | **O(N · L)** | **O(L)** |
| Memory | the strings | nodes × 26 references |

The file checks the trie against a HashSet scan on 20,000 random operations.

---

## 7. Edge cases and common mistakes

**Edge cases:** the empty prefix (true), searching a prefix of an inserted word (false unless inserted), inserting the same word twice, a word that's a prefix of another.

**Mistakes:**
1. Forgetting `isEnd` (prefixes look like words).
2. Returning true from `startsWith` only when isEnd is set.
3. Creating nodes during `search` (pollutes the trie).

---

## 8. Interview questions and answers

**Q1. Why `Node[26]` rather than a HashMap?**
> For a fixed lowercase alphabet, array indexing is the fastest and simplest. A map saves memory when the alphabet is large or the trie is sparse.

**Q2. How would you delete a word?**
> Clear `isEnd` at its last node; optionally remove the nodes that no longer lead to any word, recursively on the way back (that needs a child count or a scan of the children).

### Follow-up questions

**F1. Wildcard search with '.'.** → [M02](M02_DesignAddAndSearchWords.md)

**F2. Count the words with a prefix.** → [E01](../02-Easy/E01_CountingWordsWithPrefix.md)
> Add a `pass` counter per node.

**F3. Implement Trie II (LC 1804): countWordsEqualTo, countWordsStartingWith, erase.**
> Store an `endCount` and a `passCount` per node; erase decrements them along the path.

---

## 9. Running the file

```bash
java M01_ImplementTrie.java      # 6/6 tests passed (including 20,000 random ops vs HashSet scan)
```

**One-line summary:** *"Node = children[26] + isEnd; insert creates the path and marks the end; search and startsWith share one walk and differ only in checking isEnd."*
