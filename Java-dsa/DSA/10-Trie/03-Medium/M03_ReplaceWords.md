# M03 · Replace Words ⭐

| | |
|---|---|
| **LeetCode** | [#648 Replace Words](https://leetcode.com/problems/replace-words/) |
| **Difficulty** | Medium |
| **Pattern** | Walk each word through the trie and stop at the first word end (the shortest root) |
| **Code** | [M03_ReplaceWords.java](M03_ReplaceWords.java) |
| **Run** | `java M03_ReplaceWords.java` |

⭐ = a bonus classic. Trie basics are explained in [E01](../02-Easy/E01_CountingWordsWithPrefix.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

A dictionary of **roots**. In the sentence, replace every word that starts with a root by the **shortest** such root.

| dictionary | sentence | result |
|---|---|---|
| ["cat","bat","rat"] | "the cattle was rattled by the battery" | "the cat was rat by the bat" |
| ["a","b","c"] | "aadsfasf absbs bbab cadsfafs" | "a a b c" |
| ["abc","a","ab"] | "abcd" | "a" (the shortest root wins) |

---

## 2. How to think about it

### Brute force
For each word, try every root with `startsWith`: O(words · roots · L).

### A HashSet of roots
For each word, check its prefixes of length 1, 2, 3, … in the set; the first hit is the shortest root. O(L²) per word (each `substring` and hash costs O(L)).

### A trie of the roots
Walk the word's characters down the trie:
- reach a node with `end` set → the prefix so far is the **shortest** root: replace the word
- fall off the trie → no root is a prefix: keep the word

O(L) per word, and the walk stops early at the shortest root.

```
roots: cat, bat, rat

"cattle":  c → a → t (end: "cat")  → "cat"
"the":     t → no child under the root for 't'  → keep "the"
"battery": b → a → t (end)  → "bat"
```

---

## 3. Java concepts used in this problem

### 3.1 `substring(0, i + 1)`

```java
if (n.end) return word.substring(0, i + 1);
```

After consuming characters 0..i, the root is the first i + 1 characters. `substring(begin, end)` excludes `end`.

### 3.2 Building the output with a `StringBuilder`

```java
if (out.length() > 0) out.append(' ');
out.append(shortestRoot(root, word));
```

Adding a space **before** every word except the first avoids a trailing space. (`String.join(" ", list)` is the alternative.)

### 3.3 Three possible outcomes of the walk

| outcome | meaning | return |
|---|---|---|
| a node with `end` | a root found | the root |
| a null child | no root matches | the whole word |
| the loop finishes | the word is a prefix of some longer root, but not a root itself | the whole word |

---

## 4. Approach 1: Try every root (the referee)

### Code ([M03_ReplaceWords.java:66-75](M03_ReplaceWords.java#L66-L75))

```java
static String brute(List<String> dict, String sentence) {
    StringBuilder out = new StringBuilder();
    for (String w : sentence.split(" ")) {
        String best = w;
        for (String r : dict) if (w.startsWith(r) && r.length() < best.length()) best = r;
        if (out.length() > 0) out.append(' ');
        out.append(best);
    }
    return out.toString();
}
```

For each word, keep the shortest root it starts with. **O(words · roots · L).**

---

## 5. Approach 2: A HashSet of roots, checking prefixes by length

```java
static String replaceWithSet(List<String> dictionary, String sentence) {
    Set<String> roots = new HashSet<>(dictionary);
    StringBuilder out = new StringBuilder();
    for (String w : sentence.split(" ")) {
        String rep = w;
        for (int len = 1; len <= w.length(); len++) {
            if (roots.contains(w.substring(0, len))) { rep = w.substring(0, len); break; }   // the shortest first
        }
        if (out.length() > 0) out.append(' ');
        out.append(rep);
    }
    return out.toString();
}
```

Trying the prefix lengths in increasing order finds the shortest root first. **O(L²) per word.**

---

## 6. Approach 3: A trie of the roots (the file's main solution)

### Code ([M03_ReplaceWords.java:36-64](M03_ReplaceWords.java#L36-L64))

```java
static class Node { Node[] next = new Node[26]; boolean end; }

static String replaceWords(List<String> dictionary, String sentence) {
    Node root = new Node();
    for (String r : dictionary) {
        Node n = root;
        for (char c : r.toCharArray()) {
            if (n.next[c - 'a'] == null) n.next[c - 'a'] = new Node();
            n = n.next[c - 'a'];
        }
        n.end = true;
    }
    StringBuilder out = new StringBuilder();
    for (String word : sentence.split(" ")) {
        if (out.length() > 0) out.append(' ');
        out.append(shortestRoot(root, word));
    }
    return out.toString();
}

static String shortestRoot(Node root, String word) {
    Node n = root;
    for (int i = 0; i < word.length(); i++) {
        n = n.next[word.charAt(i) - 'a'];
        if (n == null) return word;                    // no root is a prefix of this word
        if (n.end) return word.substring(0, i + 1);   // the first (shortest) root
    }
    return word;                                       // the word is a prefix of a longer root only
}
```

### Line by line

| Line | What it does |
|---|---|
| the insert loop | a trie of all the roots |
| `shortestRoot` | walk the word through the trie |
| `n == null` | the path breaks: no root fits → keep the word |
| `n.end` | the first root met on the way down is the shortest → return it |
| the final `return word` | ran out of characters without completing a root |

### Dry run: roots ["abc","a","ab"], word "abcd"
a → the node for "a" has `end` → return **"a"** immediately (ab and abc are never reached) ✓

### Complexity
- **Time O(total characters of the roots + the sentence).**
- **Space O(the roots' characters).**

---

## 7. Comparing the approaches

| | Every root | HashSet of prefixes | Trie |
|---|---|---|---|
| Per word | O(roots · L) | O(L²) | **O(L)** |
| Stops at the shortest root? | no (scans all) | yes | yes |

The file checks the trie against the brute force on 500 random cases.

---

## 8. Edge cases and common mistakes

**Edge cases:** a root equal to a whole word, nested roots (a, ab, abc), words with no root, a word shorter than every root.

**Mistakes:**
1. Returning the **longest** matching root (walking to the end of the word).
2. Checking `end` before moving to the child (it would test the previous prefix).
3. A trailing space in the output.

---

## 9. Interview questions and answers

**Q1. Why does the trie give the shortest root automatically?**
> The walk visits the word's prefixes in increasing length, so the first node marked `end` is the shortest root that is a prefix.

**Q2. HashSet or trie?**
> The HashSet is simple but rehashes O(L) prefixes of length up to L: O(L²) per word. The trie does a single O(L) walk and stops early.

### Follow-up questions

**F1. Replace with the longest root instead.**
> Keep walking, remembering the last `end` position seen.

**F2. Count how many words get replaced.**
> Count the walks that end at an `end` node.

**F3. Implement Trie.** → [M01](M01_ImplementTrie.md)

---

## 10. Running the file

```bash
java M03_ReplaceWords.java      # 4/4 tests passed (including 500 random cases vs brute force)
```

**One-line summary:** *"Put the roots in a trie and walk each sentence word down it, returning the prefix at the first end-of-root node (or the word itself if the path breaks)."*
