# E01 · Counting Words With a Given Prefix ⭐

| | |
|---|---|
| **LeetCode** | [#2185 Counting Words With a Given Prefix](https://leetcode.com/problems/counting-words-with-a-given-prefix/) |
| **Difficulty** | Easy |
| **Pattern** | Prefix counts stored in trie nodes (pays off with **many** queries) |
| **Code** | [E01_CountingWordsWithPrefix.java](E01_CountingWordsWithPrefix.java) |
| **Run** | `java E01_CountingWordsWithPrefix.java` |

⭐ = not in `leetcode.md`, added for a key technique. This is the first note of the Trie topic, so section 3 covers **what a trie is and how to build one in Java** in full. Later notes give only short recaps.

---

## 1. The problem

How many words in `words` start with `pref`?

| words | pref | answer |
|---|---|---|
| ["pay","attention","practice","attend"] | "at" | 2 (attention, attend) |
| ["leetcode","win","loops","success"] | "code" | 0 |

---

## 2. How to think about it

### One query: just scan
Check each word with `startsWith`: O(N · |pref|). That's the right answer for this exact problem; building a trie for a single query is overkill.

### Many queries (the real-world version)
An autocomplete box asks this on **every keystroke**. Build a trie **once**, where every node stores `pass` = the number of words going **through** it. Then each query is O(|pref|), **independent of N**.

```
words: pay, attention, practice, attend         (pass counts in brackets)

root
├── p [2] ── a [1] ── y [1]
│        └── r [1] ── a ── c ── t ── i ── c ── e
└── a [2] ── t [2] ── t [2] ──┬── e [2] ──┬── n [2] ──┬── t [1] ── i ── o ── n     (attention)
                                          …           └── d [1]                     (attend)

query "at": a (2) → t (2) → answer 2
```

---

## 3. Java concepts used in this problem

### 3.1 What a trie is

A **trie** (prefix tree) stores strings character by character along root-to-node paths. Each edge is a letter; the path from the root to a node spells a **prefix**. Words sharing a prefix share the same nodes.

```
insert "car", "cat", "cart":

root ── c ── a ──┬── r* ── t*         * = a word ends here
                 └── t*
```

- **insert / search / startsWith** all cost **O(L)** for a word of length L, regardless of how many words are stored.
- Unlike a HashSet, a trie answers **prefix** questions directly: "how many words start with 'ca'?" or "list all words starting with 'ca'".

The structure is built step by step in [01-Basics/TrieBasics](../01-Basics/TrieBasics.java).

### 3.2 The node: an array of 26 children

```java
static class Node {
    Node[] next = new Node[26];     // next[c - 'a'] = the child for letter c (null if none)
    int pass;                       // how many inserted words pass through this node
}
```

- `c - 'a'` maps 'a'..'z' to 0..25.
- `null` means "no word continues with this letter".
- Extra fields hold whatever the problem needs: `isEnd` (a word ends here), counts, an index, the word itself, …

**Array vs map children:**

| | `Node[26]` | `Map<Character, Node>` |
|---|---|---|
| child lookup | O(1), very fast | O(1) average, hashing overhead |
| memory per node | 26 references, even if mostly null | only the children that exist |
| alphabet | fixed (lowercase letters) | any characters |

### 3.3 The standard walk

```java
Node n = root;
for (char c : word.toCharArray()) {
    if (n.next[c - 'a'] == null) n.next[c - 'a'] = new Node();   // insert: create missing nodes
    n = n.next[c - 'a'];
}
```

To **search**, use the same loop, but return "not found" as soon as a child is null instead of creating it.

### 3.4 Storing the answer in the nodes

```java
n.pass++;                           // during insert: every node on the path gets +1
```

This is the key trie technique: do the work once at insert time, so that a query only has to **walk to the prefix's node and read a field**.

### 3.5 Memory

The worst case is one node per inserted character (no shared prefixes), each with a 26-slot array: about `total characters × 26` references. Shared prefixes make real tries much smaller.

---

## 4. Approach 1: Scan with `startsWith` (the file's single-query solution)

### Code ([E01_CountingWordsWithPrefix.java:31-35](E01_CountingWordsWithPrefix.java#L31-L35))

```java
static int prefixCount(String[] words, String pref) {          // single query: just scan
    int count = 0;
    for (String w : words) if (w.startsWith(pref)) count++;
    return count;
}
```

| Line | What it does |
|---|---|
| `w.startsWith(pref)` | compares the first \|pref\| characters (false if w is shorter) |
| `count++` | one more matching word |

**Dry run** `"at"`: pay ✗, attention ✓, practice ✗, attend ✓ → **2**.

**O(N · |pref|)** per query.

---

## 5. Approach 2: A trie with pass counts (many queries)

### Code ([E01_CountingWordsWithPrefix.java:37-60](E01_CountingWordsWithPrefix.java#L37-L60))

```java
static class PrefixCounter {                                   // many queries: trie with counts
    static class Node { Node[] next = new Node[26]; int pass; }
    final Node root = new Node();

    PrefixCounter(String[] words) {
        for (String w : words) {
            Node n = root;
            for (char c : w.toCharArray()) {
                if (n.next[c - 'a'] == null) n.next[c - 'a'] = new Node();
                n = n.next[c - 'a'];
                n.pass++;                                       // one more word passes through
            }
        }
    }

    int count(String pref) {
        Node n = root;
        for (char c : pref.toCharArray()) {
            n = n.next[c - 'a'];
            if (n == null) return 0;
        }
        return n.pass;
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| the constructor | insert every word, adding 1 to `pass` on each node along its path |
| `n.pass++` after moving | the node for the first i letters of w counts w |
| `count`: the walk | follow the prefix letter by letter |
| `n == null → 0` | no word has this prefix |
| `return n.pass` | the number of words that went through the prefix's last node |

### Dry run: count("p") on the example
root → `p`: the words through `p` are pay and practice → pass = **2** ✓

### Complexity
- **Build O(total characters);** **each query O(|pref|).**
- **Space O(total characters × 26)** worst case.

---

## 6. Comparing the approaches

| | Scan | Trie with counts |
|---|---|---|
| One query | **O(N · P)** (no setup) | O(total) build + O(P) |
| Q queries | O(Q · N · P) | O(total + Q · P) |
| Memory | none | the trie |

The file checks the trie against the scan on 6,000 random queries.

---

## 7. Edge cases and common mistakes

**Edge cases:** a prefix longer than every word (0), the prefix equal to a whole word (it counts), an empty prefix (the root's count; this file doesn't count words at the root, so it would return 0 there).

**Mistakes:**
1. Incrementing `pass` **before** moving to the child (then the root counts everything and each node counts one level too early).
2. Building a trie for a single query (correct, but needless work).
3. `c - 'A'` with lowercase input.

---

## 8. Interview questions and answers

**Q1. When is a trie better than a HashSet?**
> For prefix questions: "how many / which words start with p?". A HashSet can only answer whole-word membership.

**Q2. What if words can be deleted?**
> Walk the word's path decrementing `pass`; a node whose pass reaches 0 can be detached.

### Follow-up questions

**F1. Count the words with a given suffix.**
> Insert the reversed words and query the reversed suffix.

**F2. Count the prefixes of a word that are words (LC 2416-style sum of prefix scores).**
> Walk the word and add up the `pass` values along the way.

**F3. Implement a full trie.** → [M01 Implement Trie](../03-Medium/M01_ImplementTrie.md)

---

## 9. Running the file

```bash
java E01_CountingWordsWithPrefix.java      # 5/5 tests passed (including 6,000 random queries: trie == scan)
```

**One-line summary:** *"For one query, scan with startsWith; for many, build a trie where each node counts the words passing through it, and answer each prefix by walking to its node."*
