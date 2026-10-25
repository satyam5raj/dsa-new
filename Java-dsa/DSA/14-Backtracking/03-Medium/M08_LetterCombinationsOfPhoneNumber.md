# M08 · Letter Combinations of a Phone Number

| | |
|---|---|
| **LeetCode** | [#17 Letter Combinations of a Phone Number](https://leetcode.com/problems/letter-combinations-of-a-phone-number/) |
| **Difficulty** | Medium |
| **Pattern** | One **level** per input position; branch over that digit's letters (a cartesian product) |
| **Code** | [M08_LetterCombinationsOfPhoneNumber.java](M08_LetterCombinationsOfPhoneNumber.java) |
| **Run** | `java M08_LetterCombinationsOfPhoneNumber.java` |

Backtracking basics are explained in [E01](../02-Easy/E01_BinaryWatch.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

The digits 2–9 on a phone keypad each map to 3 or 4 letters (2 → abc, 3 → def, …, 7 → pqrs, 9 → wxyz). Return every letter string the digits could spell.

| digits | answer |
|---|---|
| "23" | ["ad","ae","af","bd","be","bf","cd","ce","cf"] |
| "7" | ["p","q","r","s"] |
| "" | [] |

---

## 2. How to think about it

This isn't subsets or permutations. The tree has exactly `len(digits)` levels, and level i branches over the letters of `digits[i]`. Every leaf is an answer: it's a **cartesian product**.

```
bt(i, sb):
    if i == len: record sb
    for ch in letters(digits[i]):
        sb.append(ch);  bt(i + 1);  delete the last char
```

Appending to a `StringBuilder` and deleting the last character is the same choose / un-choose as `path.add` / `path.remove`.

### The tree for "23"

```
                 ""
        /         |         \
       a          b          c          level 0: letters of '2'
     / | \      / | \      / | \
   ad ae af   bd be bf   cd ce cf       level 1: letters of '3'
```

### The iterative alternative
Start with [""]. For each digit, extend every string so far with each of the digit's letters (a level-by-level, BFS-style build).

| after digit | strings |
|---|---|
| (start) | [""] |
| 2 | [a, b, c] |
| 3 | [ad, ae, af, bd, be, bf, cd, ce, cf] |

### Edge case
An empty input returns `[]`, not `[""]`. Both versions check this explicitly.

---

## 3. Java concepts used in this problem

### 3.1 A lookup array indexed by digit

```
static final String[] KEYS = {"", "", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz"};
KEYS[d.charAt(i) - '0']
```

`charAt(i) − '0'` turns the digit character into its number, which indexes straight into the table. Indices 0 and 1 are empty placeholders.

### 3.2 `StringBuilder` as the path

```java
sb.append(ch);                       // choose
bt(d, i + 1, sb, out);
sb.deleteCharAt(sb.length() - 1);    // un-choose
```

A `StringBuilder` is mutable, so appending and deleting are cheap and in place. At a leaf, `sb.toString()` makes an independent `String` copy (the backtracking equivalent of `new ArrayList<>(path)`). `StringBuilder` itself is explained in [Strings E03](../../02-Strings/02-Easy/E03_ReverseString.md#3-java-concepts-used-in-this-problem).

### 3.3 `List.of()` and a mutable copy

- `List.of()` is an empty immutable list, fine to return when there's nothing to add.
- `new ArrayList<>(List.of(""))` is a mutable list holding one empty string.

### 3.4 `s + ch` creates a new string

In the iterative version, `next.add(s + ch)` builds a brand-new `String` every time, because strings are immutable. That's simple, but costs O(length) per string.

---

## 4. Approach 1: Backtracking with a `StringBuilder` (the file's main solution)

### Code ([M08_LetterCombinationsOfPhoneNumber.java:32-48](M08_LetterCombinationsOfPhoneNumber.java#L32-L48))

```java
static final String[] KEYS = {"", "", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz"};

static List<String> letterCombinations(String digits) {
    List<String> out = new ArrayList<>();
    if (digits.isEmpty()) return out;
    bt(digits, 0, new StringBuilder(), out);
    return out;
}

static void bt(String d, int i, StringBuilder sb, List<String> out) {
    if (i == d.length()) { out.add(sb.toString()); return; }
    for (char ch : KEYS[d.charAt(i) - '0'].toCharArray()) {
        sb.append(ch);
        bt(d, i + 1, sb, out);
        sb.deleteCharAt(sb.length() - 1);
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| the empty check | the problem wants [] for "" |
| `i == d.length()` | one letter has been chosen for every digit |
| the loop | the letters of digit i |
| append / recurse / delete | choose, move to the next digit, un-choose |

### Complexity
- **Time O(4ⁿ · n):** up to 4ⁿ strings, each of length n.
- **Space O(n)** recursion and builder, plus the output.

---

## 5. Approach 2: Iterative expansion

### Code ([M08_LetterCombinationsOfPhoneNumber.java:50-59](M08_LetterCombinationsOfPhoneNumber.java#L50-L59))

```java
static List<String> iterative(String d) {
    if (d.isEmpty()) return List.of();
    List<String> cur = new ArrayList<>(List.of(""));
    for (char digit : d.toCharArray()) {
        List<String> next = new ArrayList<>();
        for (String s : cur) for (char ch : KEYS[digit - '0'].toCharArray()) next.add(s + ch);
        cur = next;
    }
    return cur;
}
```

| Line | What it does |
|---|---|
| `cur = [""]` | one empty prefix to extend |
| the outer loop | process the digits left to right |
| the inner loops | every prefix × every letter of this digit |
| `cur = next` | the new, longer prefixes |

- **Time O(4ⁿ · n), space O(4ⁿ · n)** for the lists of each level.

---

## 6. Comparing the approaches

| | Backtracking | Iterative |
|---|---|---|
| Time | O(4ⁿ · n) | O(4ⁿ · n) |
| Extra memory | O(n) | the previous level's whole list |
| Output order | the same | the same |

The file checks that both produce exactly the same list (same order) for 200 random digit strings of length 0–4.

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty string, the 4-letter digits 7 and 9, a single digit.

**Mistakes:**
1. Returning [""] for an empty input.
2. Forgetting `deleteCharAt` (the builder keeps growing).
3. Adding `sb` itself instead of `sb.toString()` (it isn't a `String`, and it keeps changing).

---

## 8. Interview questions and answers

**Q1. How is this different from subsets or permutations?**
> There's no choice of **which** element to use: each level corresponds to a fixed position, and the branching is over that position's options. Every leaf is valid, so there's nothing to prune.

**Q2. Why use a `StringBuilder` instead of string concatenation in the recursion?**
> Concatenation creates a new string at every node. The builder is modified in place and copied only at the leaves.

### Follow-up questions

**F1. Only return words that are in a dictionary.**
> Walk a trie of the dictionary alongside the recursion and prune as soon as the prefix isn't in it (see [Trie](../../10-Trie/02-Easy/E01_CountingWordsWithPrefix.md)).

**F2. Letter Case Permutation (LC 784).**
> The same one-level-per-position shape, with 1 or 2 options per character.

**F3. Generate Parentheses.** → [M09](M09_GenerateParentheses.md)
> One level per position again, but with constraints on which characters are allowed.

---

## 9. Running the file

```bash
java M08_LetterCombinationsOfPhoneNumber.java      # 4/4 tests passed (including 200 random inputs: backtracking == iterative)
```

**One-line summary:** *"One recursion level per digit, branching over that digit's letters with a StringBuilder (append, recurse, delete); return [] for empty input."*
