# M13 · Word Break

| | |
|---|---|
| **LeetCode** | [#139 Word Break](https://leetcode.com/problems/word-break/) |
| **Difficulty** | Medium |
| **Pattern** | 1-D DP over **prefixes** ("can the prefix of length i be segmented?") |
| **Code** | [M13_WordBreak.java](M13_WordBreak.java) |
| **Run** | `java M13_WordBreak.java` |

The DP recipe is explained in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe).

---

## 1. The problem

Can `s` be split into a sequence of dictionary words? Words may be reused.

| s | dictionary | answer |
|---|---|---|
| "leetcode" | [leet, code] | true |
| "applepenapple" | [apple, pen] | true |
| "catsandog" | [cats, dog, sand, and, cat] | false |

---

## 2. How to think about it

### Brute force
Try every possible first word and recurse on the rest. That's exponential on inputs like "aaaa…ab" with the dictionary [a, aa, aaa, …], because the same suffixes are tried again and again. The file measured **3,147,216** calls for "a" × 22 + "b".

### The state is just the position
Whether the suffix starting at position i can be segmented doesn't depend on **how** we got to i. So:

| step | for this problem |
|---|---|
| **State** | ok[i] = the prefix s[0..i) can be segmented |
| **Transition** | ok[i] = some j < i with ok[j] and s[j..i) in the dictionary (s[j..i) is the **last** word) |
| **Base** | ok[0] = true (the empty prefix) |
| **Answer** | ok[n] |

**Speed-up:** only try j with i − j ≤ the longest word's length.

### Dry run: "leetcode"

| i | prefix | how | ok[i] |
|---|---|---|---|
| 0 | "" | base | T |
| 1–3 | "l", "le", "lee" | no word ends here | F |
| 4 | "leet" | ok[0] and "leet" | **T** |
| 5–7 | | no | F |
| 8 | "leetcode" | ok[4] and "code" | **T** |

### Dry run: "catsandog"

| i | ok[i] | reason |
|---|---|---|
| 0 | T | base |
| 3 | T | "cat" |
| 4 | T | "cats" |
| 7 | T | ok[3] + "sand", or ok[4] + "and" |
| 9 | **F** | the last word would have to be "dog" (needs ok[6], false) or "og"/"g" (not words) |

---

## 3. Java concepts used in this problem

### 3.1 A `HashSet` for O(1) word lookups

`Set<String> dict = new HashSet<>(wordDict);` turns the list into a set, so `dict.contains(...)` is O(1) on average (plus the cost of hashing the substring) instead of a linear scan.

### 3.2 The loop bound with `Math.max(0, i − maxLen)`

```java
for (int j = i - 1; j >= Math.max(0, i - maxLen) && !ok[i]; j--)
```

- j only goes back as far as the longest word allows (and never below 0).
- `&& !ok[i]` stops as soon as one way to segment the prefix is found.

### 3.3 `String.repeat`

`"a".repeat(22)` builds a string of 22 a's (Java 11+).

### 3.4 Counting calls with a static field

`static long bruteCalls` is incremented at the top of the recursion, so the test can print how much work the plain recursion does.

---

## 4. Approach 1: Plain recursion (the referee)

### Code ([M13_WordBreak.java:49-57](M13_WordBreak.java#L49-L57))

```java
static long bruteCalls;

static boolean brute(String s, int start, Set<String> dict) {
    bruteCalls++;
    if (start == s.length()) return true;
    for (int end = start + 1; end <= s.length(); end++)
        if (dict.contains(s.substring(start, end)) && brute(s, end, dict)) return true;
    return false;
}
```

| Line | What it does |
|---|---|
| `start == s.length()` | everything was matched |
| the loop | try every first word s[start..end) |
| `&&` | only recurse if that piece is a word; stop at the first success |

- **Time exponential** in the worst case (3.1 million calls for a 23-character string). Memoizing on `start` would make it O(n²).

---

## 5. Approach 2: Prefix DP (the file's main solution)

### Code ([M13_WordBreak.java:37-47](M13_WordBreak.java#L37-L47))

```java
static boolean wordBreak(String s, List<String> wordDict) {
    Set<String> dict = new HashSet<>(wordDict);
    int maxLen = 0;
    for (String w : wordDict) maxLen = Math.max(maxLen, w.length());
    boolean[] ok = new boolean[s.length() + 1];
    ok[0] = true;
    for (int i = 1; i <= s.length(); i++)
        for (int j = i - 1; j >= Math.max(0, i - maxLen) && !ok[i]; j--)
            ok[i] = ok[j] && dict.contains(s.substring(j, i));   // last word = s[j..i)
    return ok[s.length()];
}
```

### Line by line

| Line | What it does |
|---|---|
| `dict`, `maxLen` | fast lookups and the longest word |
| `ok[0] = true` | the empty prefix |
| the outer loop | every prefix length i |
| the inner loop | every possible start j of the last word |
| the assignment | the prefix up to j works and s[j..i) is a word |

`ok[j] &&` short-circuits, so the substring is only built when the prefix before it is valid.

### Complexity
- **Time O(n · L)** substring checks, each O(L) to build and hash (L = the longest word): O(n · L²) overall.
- **Space O(n).**

---

## 6. Comparing the approaches

| | Plain recursion | Prefix DP |
|---|---|---|
| "a" × 22 + "b" | 3,147,216 calls | 24 cells |
| Time | exponential | O(n · L²) |

The file checks the DP against the recursion on the "evil" input and on 500 random strings and dictionaries over {a, b}.

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty string (true), a dictionary word equal to s, no possible segmentation, very long words in the dictionary.

**Mistakes:**
1. ok[0] = false.
2. Off-by-one in `substring(j, i)` (it's s[j..i), end exclusive).
3. Greedy matching of the longest word first (fails: "aaab" style inputs or "cars" with [car, ca, rs]).

---

## 8. Interview questions and answers

**Q1. Why does the DP only need the position as its state?**
> The remaining question, "can s[i..] be segmented?", depends only on i, not on which words were used before. So each position is solved once.

**Q2. How would a trie help?**
> Instead of building and hashing substrings, walk a trie of the dictionary forward from each valid position j; every word end reached marks a new valid position. That avoids repeated substring work.

### Follow-up questions

**F1. Word Break II (LC 140): return all sentences.**
> Backtracking, memoized on the start position (store the list of sentences for each suffix).

**F2. Concatenated Words.** → [Trie H04](../../10-Trie/04-Hard/H04_ConcatenatedWords.md)
> This DP run for every word, using the other words as the dictionary, with a trie.

**F3. Palindrome Partitioning.** → [Backtracking M10](../../14-Backtracking/03-Medium/M10_PalindromePartitioning.md)
> The same "where does the next piece end" structure, listing every split.

---

## 9. Running the file

```bash
java M13_WordBreak.java      # 5/5 tests passed; prints 3,147,216 recursion calls vs 24 DP cells on the evil case
```

**One-line summary:** *"ok[i] is true if some j has ok[j] and s[j..i) in the dictionary, with ok[0] = true; checking only j within the longest word's length keeps it fast."*
