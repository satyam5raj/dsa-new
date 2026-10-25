# E02 · Check If a Word Occurs As a Prefix of Any Word in a Sentence ⭐

| | |
|---|---|
| **LeetCode** | [#1455 Check If a Word Occurs As a Prefix of Any Word in a Sentence](https://leetcode.com/problems/check-if-a-word-occurs-as-a-prefix-of-any-word-in-a-sentence/) |
| **Difficulty** | Easy |
| **Pattern** | A prefix test per word (and when a trie would be the better tool) |
| **Code** | [E02_IsPrefixOfWordInSentence.java](E02_IsPrefixOfWordInSentence.java) |
| **Run** | `java E02_IsPrefixOfWordInSentence.java` |

⭐ = not in `leetcode.md`, added for a key technique. Trie basics are explained in [E01](E01_CountingWordsWithPrefix.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Return the **1-based** index of the **first** word in `sentence` that starts with `searchWord`, or −1.

| sentence | searchWord | answer |
|---|---|---|
| "i love eating burger" | "burg" | 4 |
| "this problem is an easy problem" | "pro" | 2 (the first match wins) |
| "i am tired" | "you" | −1 |

---

## 2. How to think about it

### One searchWord
Split the sentence and test `startsWith` on each word: O(length of the sentence). Done.

### When a trie helps
If the sentence is fixed and **many** searchWords arrive, build a trie of the sentence's words where each node remembers the **smallest word index** passing through it. Each query is then O(|searchWord|).

```
"this problem is an easy problem"   (words 1..6)

p (min index 2) ── r (2) ── o (2) ── b … (both "problem"s pass here: indices 2 and 6 → min 2)
e (5) ── a … 
query "pro" → walk p, r, o → 2
```

This "store the answer in the node" idea is used again in [Search Suggestions System (M04)](../03-Medium/M04_SearchSuggestionsSystem.md).

---

## 3. Java concepts used in this problem

### 3.1 `String.split(" ")`

`"i love eating burger".split(" ")` → `["i", "love", "eating", "burger"]`. The argument is a regular expression; a single space is fine here because the words are separated by exactly one space.

### 3.2 1-based answers

`return i + 1;`: the loop index is 0-based, the problem's is 1-based.

### 3.3 `Integer.MAX_VALUE` as the initial minimum

```java
int firstIndex = Integer.MAX_VALUE;
n.firstIndex = Math.min(n.firstIndex, i + 1);
```

Every real index is smaller, so the first word through a node sets it. Since the words are inserted in order, this simply keeps the first one.

---

## 4. Approach 1: Split and test each word (the file's main solution)

### Code ([E02_IsPrefixOfWordInSentence.java:32-36](E02_IsPrefixOfWordInSentence.java#L32-L36))

```java
static int isPrefixOfWord(String sentence, String searchWord) {
    String[] words = sentence.split(" ");
    for (int i = 0; i < words.length; i++) if (words[i].startsWith(searchWord)) return i + 1;
    return -1;
}
```

| Line | What it does |
|---|---|
| `split(" ")` | the words of the sentence |
| `startsWith` | is searchWord a prefix of this word? |
| `return i + 1` | the first match, 1-based |

**Dry run** "pro" on "this problem is an easy problem": this ✗, **problem ✓** → **2** (the second "problem" is never checked).

**O(length of the sentence).**

---

## 5. Approach 2: A trie storing the first index (many queries)

### Code ([E02_IsPrefixOfWordInSentence.java:38-62](E02_IsPrefixOfWordInSentence.java#L38-L62))

```java
static class SentenceIndex {
    static class Node { Node[] next = new Node[26]; int firstIndex = Integer.MAX_VALUE; }
    final Node root = new Node();

    SentenceIndex(String sentence) {
        String[] words = sentence.split(" ");
        for (int i = 0; i < words.length; i++) {
            Node n = root;
            for (char c : words[i].toCharArray()) {
                if (n.next[c - 'a'] == null) n.next[c - 'a'] = new Node();
                n = n.next[c - 'a'];
                n.firstIndex = Math.min(n.firstIndex, i + 1);   // the answer, stored in the node
            }
        }
    }

    int query(String w) {
        Node n = root;
        for (char c : w.toCharArray()) {
            n = n.next[c - 'a'];
            if (n == null) return -1;
        }
        return n.firstIndex;
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `firstIndex` | the smallest word index whose prefix reaches this node |
| `Math.min(..., i + 1)` | keep the earliest word through this node |
| `query`: the walk | a missing node → no word has this prefix → −1 |
| `return n.firstIndex` | the first word with this prefix |

### Dry run: query("e") on "this problem is an easy problem"
root → `e`: only "easy" (word 5) starts with e → **5** ✓

### Complexity
- **Build O(sentence length);** **each query O(|searchWord|).**
- **Space O(sentence length × 26).**

---

## 6. Comparing the approaches

| | Split + startsWith | Trie with firstIndex |
|---|---|---|
| One query | **O(n)** | O(n) build + O(L) |
| Q queries | O(Q · n) | O(n + Q · L) |

---

## 7. Edge cases and common mistakes

**Edge cases:** the searchWord longer than every word (−1), several matches (the first one), the searchWord equal to a whole word (a match).

**Mistakes:**
1. Returning a 0-based index.
2. Using `contains` instead of `startsWith` (a match in the middle of a word doesn't count).
3. Returning the last match instead of the first.

---

## 8. Interview questions and answers

**Q1. Why store the minimum index in each node?**
> The question is "the first word with this prefix". Every word with the prefix passes through the prefix's node, so the minimum over them is the answer, precomputed.

**Q2. What if the sentence changes often?**
> Updating the trie is O(word length) per insertion, but deletions would need per-node sets of indices (or counts) to know the new minimum.

### Follow-up questions

**F1. Return all the matching indices.**
> Store a list of indices per node (more memory), or collect them by DFS below the prefix node.

**F2. Count the matches.** → [E01](E01_CountingWordsWithPrefix.md)
> Store a pass count instead.

**F3. Search Suggestions System.** → [M04](../03-Medium/M04_SearchSuggestionsSystem.md)
> Store the best 3 words per node.

---

## 9. Running the file

```bash
java E02_IsPrefixOfWordInSentence.java      # 6/6 tests passed
```

**One-line summary:** *"Split the sentence and return the first 1-based index whose word startsWith the search word; for many queries, store each node's first word index in a trie."*
