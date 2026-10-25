# E04 · Length of Last Word

| | |
|---|---|
| **LeetCode** | [#58 Length of Last Word](https://leetcode.com/problems/length-of-last-word/) |
| **Difficulty** | Easy |
| **Pattern** | Scan from the end |
| **Code** | [E04_LengthOfLastWord.java](E04_LengthOfLastWord.java) |
| **Run** | `java E04_LengthOfLastWord.java` |

---

## 1. The problem

`s` consists of words and spaces. Return the length of the **last word** (a word is a maximal run of non-space characters).

| s | last word | answer |
|---|---|---|
| `"Hello World"` | World | 5 |
| `"   fly me   to   the moon  "` | moon | 4 (watch the trailing spaces!) |
| `"luffy is still joyboy"` | joyboy | 6 |
| `"a"` | a | 1 |
| `"word   "` | word | 4 |

**Constraints:** `1 ≤ s.length ≤ 10⁴`; there's at least one word.

---

## 2. How to think about it

- **Easy way:** split into words and take the last one. It works, but it builds an array of **every** word just to read one.
- **Scan from the end:** the answer is at the end of the string, so start there:
  1. skip the trailing spaces
  2. count characters until you hit a space (or the start of the string)

We never even look at the earlier words.

---

## 3. Java concepts used in this problem

### 3.1 `trim()`, `strip()` and `split`

```java
"  hi there  ".trim()          // "hi there"   (removes leading/trailing whitespace ≤ ' ')
"  hi there  ".strip()         // "hi there"   (Java 11+, Unicode-aware whitespace)
"a  b c".split(" ")            // ["a", "", "b", "c"]   ← empty strings between double spaces!
"a  b c".split(" +")           // ["a", "b", "c"]       ← regex: one or more spaces
"a  b c".split("\\s+")         // the same for any whitespace (tabs, newlines too)
```

- `split` takes a **regular expression** (regex), not a plain string. `" +"` means "one or more spaces". In Java string literals, a backslash must be doubled, hence `"\\s+"`.
- Leading separators produce an **empty first element**: `" a b".split(" +")` → `["", "a", "b"]`. Trailing empty strings are dropped automatically. That's why the "easy way" trims first.
- `split` and `trim` both create new strings (strings are immutable): O(n) extra memory.

### 3.2 A loop with a bounds check first

```java
while (i >= 0 && s.charAt(i) == ' ') i--;
```

`i >= 0` comes first so `charAt(-1)` is never called (short-circuit `&&`). This is the same guard pattern as in the arrays notes.

### 3.3 `lastIndexOf` (alternative)

`s.lastIndexOf(' ')` returns the index of the last space, or −1. Combined with `strip()`: `t = s.strip(); return t.length() - t.lastIndexOf(' ') - 1;`. It's short, but still creates a copy.

---

## 4. Approach 1: Trim + split

```java
static int lengthOfLastWordSplit(String s) {
    String[] words = s.trim().split(" +");
    return words[words.length - 1].length();
}
```

| Line | What it does |
|---|---|
| `s.trim()` | remove the leading/trailing spaces, so there are no empty words at the ends |
| `.split(" +")` | split on runs of spaces |
| `words[words.length - 1].length()` | the length of the last word |

**Dry run** `"   fly me   to   the moon  "` → trimmed `"fly me   to   the moon"` → `["fly","me","to","the","moon"]` → `"moon".length()` = **4**.

**O(n) time and O(n) space** (the trimmed copy plus all the words).

---

## 5. Approach 2: Scan from the end (the file's solution)

### Code ([E04_LengthOfLastWord.java:34-43](E04_LengthOfLastWord.java#L34-L43))

```java
static int lengthOfLastWord(String s) {
    int i = s.length() - 1;
    while (i >= 0 && s.charAt(i) == ' ') i--;          // 1. skip trailing spaces
    int length = 0;
    while (i >= 0 && s.charAt(i) != ' ') {             // 2. count the last word
        length++;
        i--;
    }
    return length;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int i = s.length() - 1;` | start at the last character |
| `while (i >= 0 && s.charAt(i) == ' ') i--;` | phase 1: walk left over the trailing spaces |
| `int length = 0;` | the length of the word |
| `while (i >= 0 && s.charAt(i) != ' ') { length++; i--; }` | phase 2: count the non-space characters until a space or the start |
| `return length;` | |

### Dry run 1: `"   fly me   to   the moon  "` (length 27, indices 0..26)

| phase | i | char | action | length |
|---|---|---|---|---|
| 1 | 26 | ␣ | skip | 0 |
| 1 | 25 | ␣ | skip | 0 |
| 1 | 24 | n | stop skipping | 0 |
| 2 | 24 | n | count | 1 |
| 2 | 23 | o | count | 2 |
| 2 | 22 | o | count | 3 |
| 2 | 21 | m | count | 4 |
| 2 | 20 | ␣ | stop | 4 |

**Return 4** ✓

### Dry run 2: `"a"`
Phase 1: `'a'` isn't a space, so nothing to skip. Phase 2: count `a` → i = −1 → stop. **Return 1** ✓. The `i >= 0` guard stops it at the start of the string.

### Dry run 3: `"Hello World"`
No trailing spaces; count d, l, r, o, W (5), then a space at index 5 → **return 5** ✓

### Complexity
- **Time O(trailing spaces + last word length)**, which is O(n) in the worst case (one long word), but often far less.
- **Space O(1).**

---

## 6. Comparing the approaches

| | Trim + split | Scan from the end |
|---|---|---|
| Time | O(n) always | **O(k)**: only the tail |
| Extra space | O(n) | **O(1)** |
| Code length | 2 lines | 7 lines |

---

## 7. Edge cases and common mistakes

**Edge cases:** trailing spaces, leading spaces, a single word with no spaces, a single character, multiple spaces between words.

**Mistakes:**
1. `split(" ")` without trimming or using `" +"` → empty strings, so you can get length 0.
2. Not skipping trailing spaces before counting.
3. Forgetting `i >= 0`, which crashes when the whole string is one word.

---

## 8. Interview questions and answers

**Q1. Why scan from the end?**
> The answer depends only on the tail of the string. Scanning from the end touches only the trailing spaces and the last word, with O(1) extra space.

**Q2. What does `split(" +")` do differently from `split(" ")`?**
> `split` takes a regex. `" +"` matches one or more spaces, so runs of spaces act as a single separator. `" "` splits on every single space, producing empty strings between consecutive spaces.

### Follow-up questions

**F1. The length of the first word.**
> Scan from the start: skip the leading spaces, then count.

**F2. Count the words in a string (LC 434).**
> Count positions where `s[i] != ' '` and (`i == 0` or `s[i−1] == ' '`): each word has exactly one start.

**F3. Words separated by any whitespace (tabs, newlines).**
> Use `Character.isWhitespace(c)` instead of `c == ' '`.

**F4. Reverse Words in a String (LC 151).** → [M12](../03-Medium/M12_ReverseWordsInString.md)
> The same "skip spaces, then take the word" scanning, applied to every word.

---

## 9. Running the file

```bash
java E04_LengthOfLastWord.java      # 5/5 tests passed
```

**One-line summary:** *"From the end: skip the trailing spaces, then count characters until a space."*
