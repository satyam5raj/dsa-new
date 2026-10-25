# E08 · Word Pattern

| | |
|---|---|
| **LeetCode** | [#290 Word Pattern](https://leetcode.com/problems/word-pattern/) |
| **Difficulty** | Easy |
| **Pattern** | Character ↔ word bijection (Isomorphic Strings with words) |
| **Code** | [E08_WordPattern.java](E08_WordPattern.java) |
| **Run** | `java E08_WordPattern.java` |

---

## 1. The problem

Does the sentence `s` follow `pattern`? Each letter of the pattern maps to exactly **one** word, and each word maps to exactly **one** letter.

| pattern | s | answer | why |
|---|---|---|---|
| `"abba"` | `"dog cat cat dog"` | true | a↔dog, b↔cat |
| `"abba"` | `"dog cat cat fish"` | false | a would map to dog and fish |
| `"aaaa"` | `"dog cat cat dog"` | false | a can't be several words |
| `"abba"` | `"dog dog dog dog"` | false | a and b both map to dog |
| `"aaa"` | `"aa aa aa aa"` | false | 3 letters, 4 words |

**Constraints:** the pattern is lowercase letters; `s` is lowercase words separated by **single** spaces, with no leading/trailing spaces.

---

## 2. How to think about it

This is [E07 Isomorphic Strings](E07_IsomorphicStrings.md) with **words** instead of characters:
1. Split `s` into words. If the number of words ≠ the pattern length → false.
2. Keep **two maps**: letter → word and word → letter. Any conflict → false.

Why two maps? `"abba"` / `"dog dog dog dog"`: the letter→word map alone sees a→dog, b→dog, which is fine for it (each letter has one word). The word→letter map catches that `dog` comes from two letters.

---

## 3. Java concepts used in this problem

### 3.1 `split(" ")` into words

```java
String[] words = "dog cat cat dog".split(" ");   // ["dog", "cat", "cat", "dog"]
```

The problem guarantees single spaces, so `split(" ")` is safe here. With messy spacing, use `trim().split("\\s+")` (see E04).

### 3.2 Comparing strings with `.equals()`, not `==`

```java
!charToWord.get(c).equals(w)
```

`words[i]` is a new String object created by `split`, so `==` would compare **references** and could be false even for equal words. **Always use `.equals()` for Strings.** (This is the classic Java trap.)

### 3.3 Maps with different key and value types

```java
Map<Character, String> charToWord = new HashMap<>();
Map<String, Character> wordToChar = new HashMap<>();
```

Generics let each map have its own key and value types. Strings make good HashMap keys: they're immutable, and `String.hashCode()` is computed from the characters (and cached).

### 3.4 `Character != char`

`wordToChar.get(w) != c` compares a `Character` (from the map) with a `char` (c). The `Character` is unboxed, so it's a **value** comparison: safe. Two `Character` objects compared with `!=` would compare references (risky outside the cached range 0..127).

---

## 4. Approach: Two maps (the file's solution)

### Code ([E08_WordPattern.java:35-49](E08_WordPattern.java#L35-L49))

```java
static boolean wordPattern(String pattern, String s) {
    String[] words = s.split(" ");
    if (words.length != pattern.length()) return false;
    Map<Character, String> charToWord = new HashMap<>();
    Map<String, Character> wordToChar = new HashMap<>();
    for (int i = 0; i < words.length; i++) {
        char c = pattern.charAt(i);
        String w = words[i];
        if (charToWord.containsKey(c) && !charToWord.get(c).equals(w)) return false;
        if (wordToChar.containsKey(w) && wordToChar.get(w) != c) return false;
        charToWord.put(c, w);
        wordToChar.put(w, c);
    }
    return true;
}
```

### Line by line

| Line | What it does |
|---|---|
| `String[] words = s.split(" ");` | the sentence as an array of words |
| `if (words.length != pattern.length()) return false;` | one word per letter is required |
| `charToWord`, `wordToChar` | the two directions of the bijection |
| `if (charToWord.containsKey(c) && !charToWord.get(c).equals(w))` | this letter was already paired with a **different** word |
| `if (wordToChar.containsKey(w) && wordToChar.get(w) != c)` | this word was already paired with a **different** letter |
| `put` both | record (or re-record, harmlessly) the pair |

### Dry run 1: `"abba"` / `"dog cat cat dog"`

| i | c | w | charToWord check | wordToChar check | maps after |
|---|---|---|---|---|---|
| 0 | a | dog | new | new | a→dog, dog→a |
| 1 | b | cat | new | new | + b→cat, cat→b |
| 2 | b | cat | b→cat ✓ | cat→b ✓ | unchanged |
| 3 | a | dog | a→dog ✓ | dog→a ✓ | unchanged |

**Return true** ✓

### Dry run 2: `"abba"` / `"dog dog dog dog"`

| i | c | w | check | result |
|---|---|---|---|---|
| 0 | a | dog | new, new | record a↔dog |
| 1 | b | dog | b is new; but dog → **a** ≠ b | **false** ✓ |

### Dry run 3: `"abba"` / `"dog cat cat fish"`
i = 3: a → dog already, but w = fish → **false** ✓

### Dry run 4: `"aaa"` / `"aa aa aa aa"`
3 letters vs 4 words → **false** immediately ✓

### Complexity
- **Time O(n + L)**, where L is the total length of s (splitting, plus hashing each word).
- **Space O(number of distinct words + L)** for the words and maps.

---

## 5. Alternative: the "first index" pattern trick

As in E07, compare **where each letter and each word was last seen**:

```java
static boolean wordPatternIndex(String pattern, String s) {
    String[] words = s.split(" ");
    if (words.length != pattern.length()) return false;
    Map<Object, Integer> last = new HashMap<>();
    for (Integer i = 0; i < words.length; i++) {
        // put() returns the previous value; letters and words share one map (Character vs String keys never clash)
        if (!Objects.equals(last.put(pattern.charAt(i), i), last.put(words[i], i))) return false;
    }
    return true;
}
```

- **One** map with `Object` keys: a `Character` key never equals a `String` key, so they can't collide.
- `map.put(k, v)` **returns the old value** (or null). If the letter and the word have different "last seen" histories, they aren't consistently paired.
- `Objects.equals(a, b)` is null-safe equality.
- The returned old values are `Integer` objects, and `Objects.equals` compares them with `.equals` (by value), so it works for any index. Comparing them with `!=` would compare references and break for indices above 127, outside Java's small-Integer cache. (Verified on a 300-word input.)

It's a neat one-pass trick that interviewers enjoy, but the two-map version is clearer.

---

## 6. Comparing the approaches

| | Two maps | One map, last-index |
|---|---|---|
| Time | O(n + L) | O(n + L) |
| Space | O(distinct words) | O(distinct words) |
| Clarity | **high** | clever |

---

## 7. Edge cases and common mistakes

**Edge cases:** the number of words differs from the pattern length, a single letter and word, all letters the same, all words the same.

**Mistakes:**
1. Comparing words with `==`.
2. Only one direction of the mapping.
3. Not checking the lengths first, which leads to an index error.
4. With messy input (multiple spaces), `split(" ")` produces empty "words".

---

## 8. Interview questions and answers

**Q1. How is this related to Isomorphic Strings?**
> It's the same bijection check, except one side is words. Two maps (or matching last-seen positions) enforce the one-to-one mapping.

**Q2. Why `.equals` and not `==` for the words?**
> `==` compares object references. Words produced by `split` are separate objects, so equal words may not be the same object. `.equals` compares their characters.

### Follow-up questions

**F1. Word Pattern II (LC 291): no spaces in s, so you must decide where each word starts and ends.**
> Backtracking: for each pattern letter, either use its existing mapping (it must match at the current position) or try every possible new substring that isn't already mapped. Exponential in the worst case.

**F2. Words separated by arbitrary whitespace.**
> `s.trim().split("\\s+")`.

**F3. Group the sentences that follow the same pattern.**
> Map each sentence to its canonical shape (each word → the index of its first occurrence) and group by that key.

---

## 9. Running the file

```bash
java E08_WordPattern.java      # 5/5 tests passed
```

**One-line summary:** *"Split into words, check the counts, then enforce letter↔word one-to-one with two maps (and use .equals for the words)."*
