# M05 · Longest Word in Dictionary ⭐

| | |
|---|---|
| **LeetCode** | [#720 Longest Word in Dictionary](https://leetcode.com/problems/longest-word-in-dictionary/) |
| **Difficulty** | Medium |
| **Pattern** | DFS the trie, only stepping into nodes that **end** a word |
| **Code** | [M05_LongestWordInDictionary.java](M05_LongestWordInDictionary.java) |
| **Run** | `java M05_LongestWordInDictionary.java` |

⭐ = a bonus classic. Trie basics are explained in [E01](../02-Easy/E01_CountingWordsWithPrefix.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Find the longest word that can be built **one character at a time** from other words in the list (every prefix must itself be a word). On a tie, return the lexicographically smallest.

| words | answer |
|---|---|
| ["w","wo","wor","worl","world"] | "world" |
| ["a","banana","app","appl","ap","apply","apple"] | "apple" (apply and apple both work; apple < apply) |
| ["ab","bc"] | "" (nothing can be built: "a" and "b" aren't words) |

---

## 2. How to think about it

### The trie insight
"Every prefix is a word" = **every node on the path has `isEnd`**. Insert all the words, then DFS from the root, but **only descend into children that end a word**. The deepest node reached is the answer.

```
a* ── p* ── p* ── l* ──┬── e*   (apple)
                       └── y*   (apply)
b ── a ── n ── a ── n ── a*      (banana: 'b' isn't a word, so the DFS never enters it)
```

Visiting the children in a..z order and replacing the best only on **strictly longer** words gives the smallest word among ties automatically: apple (via 'e') is found before apply (via 'y').

### The alternative: sort + a set
Sort the words alphabetically (each word's prefixes then come before it); keep a set of "buildable" words. A word is buildable if it has length 1 or its prefix minus the last character is in the set. O(n log n · L).

---

## 3. Java concepts used in this problem

### 3.1 Storing the word in its end node

```java
static class Node { Node[] next = new Node[26]; String word; }   // word != null means "ends here"
```

Instead of a boolean `isEnd`, the node stores the word itself (a reference, not a copy). That marks the end **and** gives the answer without rebuilding the string from the path.

### 3.2 `String[] best = {""}`: a mutable result across recursion

A one-element array lets the recursive `dfs` update the best word (a `String` parameter would be a local copy of the reference).

### 3.3 The order of the children array = alphabetical order

`for (Node child : n.next)` visits 'a' before 'b' before …, so a lexicographically smaller word of the same length is always found first.

---

## 4. Approach 1: Sort + a buildable set

### Code ([M05_LongestWordInDictionary.java:60-72](M05_LongestWordInDictionary.java#L60-L72))

```java
static String sortVersion(String[] words) {
    String[] s = words.clone();
    Arrays.sort(s);                                                   // alphabetical: prefixes come first
    Set<String> built = new HashSet<>();
    String best = "";
    for (String w : s) {
        if (w.length() == 1 || built.contains(w.substring(0, w.length() - 1))) {
            built.add(w);
            if (w.length() > best.length()) best = w;                 // alphabetical order -> smallest among ties
        }
    }
    return best;
}
```

| Line | What it does |
|---|---|
| alphabetical sort | "ap" < "app" < "appl" < "apple": each word's prefixes are processed before it |
| the buildable test | length 1, or the word minus its last letter was buildable |
| `>` (strictly longer) | among equal lengths, the first one alphabetically wins |

**O(n log n · L).**

---

## 5. Approach 2: Trie DFS through word ends (the file's main solution)

### Code ([M05_LongestWordInDictionary.java:35-58](M05_LongestWordInDictionary.java#L35-L58))

```java
static class Node { Node[] next = new Node[26]; String word; }   // word != null means "ends here"

static String longestWord(String[] words) {
    Node root = new Node();
    for (String w : words) {
        Node n = root;
        for (char c : w.toCharArray()) {
            if (n.next[c - 'a'] == null) n.next[c - 'a'] = new Node();
            n = n.next[c - 'a'];
        }
        n.word = w;
    }
    String[] best = {""};
    dfs(root, best);
    return best[0];
}

static void dfs(Node n, String[] best) {
    for (Node child : n.next) {                                       // a..z order
        if (child == null || child.word == null) continue;            // only through complete words
        if (child.word.length() > best[0].length()) best[0] = child.word;   // strictly longer only
        dfs(child, best);
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| the inserts | a trie with each word stored at its end node |
| `child.word == null → continue` | this prefix isn't a word, so nothing below it can be built |
| the strictly-longer check | update only for a longer word (ties keep the earlier, smaller one) |
| `dfs(child, best)` | continue building from this word |

### Dry run: ["a","banana","app","appl","ap","apply","apple"]

| visit | word | best |
|---|---|---|
| a | "a" | "a" |
| ap | "ap" | "ap" |
| app | "app" | "app" |
| appl | "appl" | "appl" |
| apple (child 'e') | "apple" | **"apple"** |
| apply (child 'y') | "apply" (same length, not longer) | "apple" |
| b | no word | skipped (banana unreachable) |

**"apple"** ✓

### Complexity
- **Time O(total characters).**
- **Space O(total characters × 26).**

---

## 6. Comparing the approaches

| | Sort + set | Trie DFS |
|---|---|---|
| Time | O(n log n · L) | **O(total chars)** |
| Space | O(total chars) | O(total chars × 26) |
| Tie-breaking | alphabetical order + strictly longer | a..z children + strictly longer |

The file checks both on 1,000 random word lists.

---

## 7. Edge cases and common mistakes

**Edge cases:** nothing buildable (""), several longest words (the smallest), a long word whose chain breaks in the middle.

**Mistakes:**
1. Allowing a word whose intermediate prefix is missing.
2. Using `>=` (a later, larger word would replace a smaller one of the same length).
3. Forgetting that single letters are buildable on their own.

---

## 8. Interview questions and answers

**Q1. Why only descend into nodes that end a word?**
> A word is buildable only if every prefix is a word, so a non-word node breaks every chain through it.

**Q2. How are ties handled for free?**
> The DFS visits children alphabetically and only replaces the best on a strictly longer word, so the first longest word found is the smallest.

### Follow-up questions

**F1. Longest Word With All Prefixes (LC 1858).**
> The same problem.

**F2. Return all the buildable words.**
> Collect every node visited by the DFS.

**F3. Building by appending OR prepending characters.**
> A BFS over the strings, checking both the prefix and the suffix in a HashSet.

---

## 9. Running the file

```bash
java M05_LongestWordInDictionary.java      # 4/4 tests passed (including 1,000 random lists: trie == sort)
```

**One-line summary:** *"Insert all the words, then DFS from the root, only stepping into children that end a word, in a..z order; keep the first strictly longest word reached."*
