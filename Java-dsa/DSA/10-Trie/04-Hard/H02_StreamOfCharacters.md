# H02 · Stream of Characters ⭐

| | |
|---|---|
| **LeetCode** | [#1032 Stream of Characters](https://leetcode.com/problems/stream-of-characters/) |
| **Difficulty** | Hard |
| **Pattern** | A reversed trie: match **suffixes** by walking the stream backwards |
| **Code** | [H02_StreamOfCharacters.java](H02_StreamOfCharacters.java) |
| **Run** | `java H02_StreamOfCharacters.java` |

⭐ = a bonus classic. Trie basics are explained in [E01](../02-Easy/E01_CountingWordsWithPrefix.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

`StreamChecker(words)`; `query(letter)` returns true if some word is a **suffix** of all the letters queried so far.

```
words [cd, f, kl]
query: a b c d e f g h i j k l
       . . . T . T . . . . . T          ("cd" ends at d, "f" at f, "kl" at l)
```

---

## 2. How to think about it

### Naive
After each letter, check every word with `endsWith`: O(W · L) per query.

### Flip the direction
A forward trie answers "which words **start** here?". We need "which words **end** at the newest character?". So flip everything:
- insert every word **reversed** into the trie ("cd" → d, c)
- keep the stream in a StringBuilder; on each query, walk the trie starting from the **newest** character, going backwards
- reach an end-of-word node → true; fall off the trie → false

Only the last `maxLen` characters can ever matter (maxLen = the longest word's length), so the walk is at most maxLen steps, and the buffer can be trimmed.

```
reversed trie for [cd, f, kl]:
root ─┬─ d ─ c*
      ├─ f*
      └─ l ─ k*

stream "abcd": walk d → c (end) → true
```

### The alternative: Aho–Corasick
A forward trie with failure links processes each character in amortised O(1). It's more complex; the reversed trie is interview-sized.

---

## 3. Java concepts used in this problem

### 3.1 Inserting a word in reverse

```java
for (int i = w.length() - 1; i >= 0; i--) { ... }       // insert REVERSED
```

The same insert loop, iterating the characters from last to first.

### 3.2 Trimming the buffer: `StringBuilder.delete`

```java
if (stream.length() > 2 * maxLen) stream.delete(0, stream.length() - maxLen);   // only the tail matters
```

It deletes the old prefix once the buffer is twice as long as needed. Doing it only at 2 · maxLen (instead of every query) keeps the deletion cost amortised O(1) per character.

### 3.3 A loop with two counters

```java
for (int i = stream.length() - 1, steps = 0; i >= 0 && steps < maxLen; i--, steps++)
```

`i` walks backwards through the stream, and `steps` limits the walk to maxLen characters. Both are declared and updated in the `for` header.

---

## 4. Approach 1: `endsWith` for every word

```java
static class StreamCheckerNaive {
    private final String[] words;
    private final StringBuilder stream = new StringBuilder();
    StreamCheckerNaive(String[] words) { this.words = words; }
    public boolean query(char letter) {
        stream.append(letter);
        String s = stream.toString();
        for (String w : words) if (s.endsWith(w)) return true;
        return false;
    }
}
```

That's O(W · L) per query (plus the `toString` copy). It's the file's referee idea.

---

## 5. Approach 2: Reversed trie + a backward walk (the file's solution)

### Code ([H02_StreamOfCharacters.java:39-71](H02_StreamOfCharacters.java#L39-L71))

```java
static class StreamChecker {
    private static class Node { Node[] next = new Node[26]; boolean end; }
    private final Node root = new Node();
    private final StringBuilder stream = new StringBuilder();
    private final int maxLen;

    StreamChecker(String[] words) {
        int m = 0;
        for (String w : words) {
            m = Math.max(m, w.length());
            Node n = root;
            for (int i = w.length() - 1; i >= 0; i--) {          // insert REVERSED
                int c = w.charAt(i) - 'a';
                if (n.next[c] == null) n.next[c] = new Node();
                n = n.next[c];
            }
            n.end = true;
        }
        maxLen = m;
    }

    public boolean query(char letter) {
        stream.append(letter);
        if (stream.length() > 2 * maxLen) stream.delete(0, stream.length() - maxLen);   // only the tail matters
        Node n = root;
        for (int i = stream.length() - 1, steps = 0; i >= 0 && steps < maxLen; i--, steps++) {   // newest first
            n = n.next[stream.charAt(i) - 'a'];
            if (n == null) return false;
            if (n.end) return true;                              // a word ends exactly at the newest char
        }
        return false;
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| the constructor | insert each word reversed; remember the longest length |
| `stream.append(letter)` | record the new character |
| the trim | keep only the useful tail |
| the backward walk | follow the reversed trie from the newest character |
| `n == null` | no word ends with this suffix → false |
| `n.end` | a whole (reversed) word matched → it's a suffix of the stream |

### Dry run: words [cd, f, kl], the stream "abcdefghijkl"

| query | backward walk | result |
|---|---|---|
| a, b, c | 'a'/'b' aren't children of the root; 'c' isn't either | F |
| d | d → c* | **T** |
| e | e: no child | F |
| f | f* | **T** |
| g … k | g, h, i, j: no child; k → nothing (the root has no 'k' child; "kl" is stored as l → k) | F |
| l | l → k* | **T** |

→ `...T.T.....T` ✓

### Complexity
- **query O(maxLen).**
- **Space O(total characters)** for the trie, plus O(maxLen) for the buffer.

---

## 6. Comparing the approaches

| | endsWith per word | Reversed trie | Aho–Corasick |
|---|---|---|---|
| Per query | O(W · L) | O(maxLen) | O(1) amortised |
| Code | trivial | short | long |

The file checks the trie against `endsWith` on 18,000 random queries.

---

## 7. Edge cases and common mistakes

**Edge cases:** a one-letter word, words that are suffixes of other words ("d" and "cd"), a long stream (the buffer trim).

**Mistakes:**
1. Inserting the words forwards and walking backwards (or vice versa).
2. Walking the whole stream each time (no maxLen limit).
3. Deleting from the buffer on every query (O(maxLen) each time).

---

## 8. Interview questions and answers

**Q1. Why reverse the words?**
> A suffix of the stream read backwards from the newest character is a prefix of the reversed stream, and tries answer prefix questions. So "does some word end here?" becomes "does some reversed word start here?".

**Q2. Why can the stream be trimmed to maxLen characters?**
> No word is longer than maxLen, so a match never involves characters further back.

### Follow-up questions

**F1. Report which word matched.**
> Store the word in its end node instead of a boolean.

**F2. Match patterns anywhere in a big text.**
> Aho–Corasick: a trie of the patterns plus failure links, linear in the text plus the matches.

**F3. Design Search Autocomplete System (LC 642).**
> A forward trie with per-node rankings ([M04](../03-Medium/M04_SearchSuggestionsSystem.md)).

---

## 9. Running the file

```bash
java H02_StreamOfCharacters.java      # 2/2 tests passed (including 18,000 random queries vs endsWith)
```

**One-line summary:** *"Insert every word reversed into a trie; on each query, walk the trie from the newest character backwards (at most maxLen steps) and report true at the first end-of-word node."*
