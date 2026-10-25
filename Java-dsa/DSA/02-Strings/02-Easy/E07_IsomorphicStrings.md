# E07 · Isomorphic Strings

| | |
|---|---|
| **LeetCode** | [#205 Isomorphic Strings](https://leetcode.com/problems/isomorphic-strings/) |
| **Difficulty** | Easy |
| **Pattern** | Character mapping (a bijection, with two maps) |
| **Code** | [E07_IsomorphicStrings.java](E07_IsomorphicStrings.java) |
| **Run** | `java E07_IsomorphicStrings.java` |

---

## 1. The problem

`s` and `t` (of equal length) are **isomorphic** if you can **replace** the characters of `s` to get `t`, where:
- every occurrence of a character is replaced by the **same** character,
- **no two different characters** map to the same character,
- a character may map to itself.

| s | t | answer | why |
|---|---|---|---|
| `"egg"` | `"add"` | true | e→a, g→d |
| `"foo"` | `"bar"` | false | `o` would have to map to both `a` and `r` |
| `"paper"` | `"title"` | true | p→t, a→i, e→l, r→e |
| `"badc"` | `"baba"` | false | b→b **and** d→b: two characters map to the same `b` |

**Constraints:** `1 ≤ n ≤ 5 × 10⁴`, any valid ASCII characters.

---

## 2. How to think about it

We need a **one-to-one** mapping (a **bijection**) between the characters of s and the characters of t.

### Why two maps?
- A map **s → t** catches "one character with two targets" (`foo`/`bar`: o→a, then o→r ✗).
- It **misses** "two characters with one target" (`badc`/`baba`: b→b, then d→b, each s-character only has one target, so the s→t map sees no problem).
- A reverse map **t → s** catches that: `b` would be mapped **from** both `b` and `d` ✗.

Together, the two maps enforce one-to-one in both directions.

### Elegant alternative: compare "first seen" patterns
Describe each string by **where each character was last seen**. `"egg"` and `"add"` have the same structure: new, new, repeat-of-previous. If the two strings' patterns agree at every position, they're isomorphic.

---

## 3. Java concepts used in this problem

### 3.1 A `char` used as an array index

```java
int a = s.charAt(i);      // char → int widening, automatically: 'a' becomes 97
sToT[a] = ...;            // an int[256] indexed directly by the character code
```

For ASCII (codes 0..127, or 0..255 for extended ASCII), `int[256]` is a fast, fixed-size "map" from character to anything.

### 3.2 Storing `char + 1` so that 0 means "unmapped"

```java
sToT[a] = b + 1;          // store b+1, so 0 means "nothing stored yet"
```

A new `int[]` is all zeros, but 0 is also a valid character code (`'\0'`). Storing `value + 1` reserves 0 as "empty". It's a common trick when you want a sentinel without a separate `boolean[]`.

### 3.3 `if / else if` for "new pair" versus "check the existing pair"

```java
if (sToT[a] == 0 && tToS[b] == 0) { record both }               // both sides are new
else if (sToT[a] != b + 1 || tToS[b] != a + 1) return false;    // something conflicts
// else: this exact pair was already recorded, which is consistent → nothing to do
```

---

## 4. Approach 1: Two HashMaps (the most readable)

```java
static boolean isIsomorphicMaps(String s, String t) {
    Map<Character, Character> sToT = new HashMap<>(), tToS = new HashMap<>();
    for (int i = 0; i < s.length(); i++) {
        char a = s.charAt(i), b = t.charAt(i);
        if (sToT.containsKey(a) && sToT.get(a) != b) return false;   // a already maps elsewhere
        if (tToS.containsKey(b) && tToS.get(b) != a) return false;   // b already comes from elsewhere
        sToT.put(a, b);
        tToS.put(b, a);
    }
    return true;
}
```

- `sToT.get(a) != b` compares a `Character` with a `char`. The Character is unboxed, so the comparison is by value (safe).
- **O(n) time, O(k) space** for k distinct characters. It works for any Unicode `char`.

---

## 5. Approach 2: Two arrays (the file's solution)

### Code ([E07_IsomorphicStrings.java:39-51](E07_IsomorphicStrings.java#L39-L51))

```java
static boolean isIsomorphic(String s, String t) {
    int[] sToT = new int[256], tToS = new int[256];       // 0 = "unmapped"; store char + 1
    for (int i = 0; i < s.length(); i++) {
        int a = s.charAt(i), b = t.charAt(i);
        if (sToT[a] == 0 && tToS[b] == 0) {
            sToT[a] = b + 1;
            tToS[b] = a + 1;
        } else if (sToT[a] != b + 1 || tToS[b] != a + 1) {
            return false;
        }
    }
    return true;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int[] sToT = new int[256], tToS = new int[256];` | forward and backward maps for ASCII |
| `int a = s.charAt(i), b = t.charAt(i);` | the characters at this position, as ints |
| `if (sToT[a] == 0 && tToS[b] == 0)` | neither side has a mapping yet → a new pair |
| `sToT[a] = b + 1; tToS[b] = a + 1;` | record a ↔ b (+1 so that 0 stays "empty") |
| `else if (sToT[a] != b + 1 \|\| tToS[b] != a + 1) return false;` | at least one side already has a mapping; if it isn't **exactly** a ↔ b, there's a conflict |
| `return true;` | every position was consistent |

### Dry run 1: `"badc"` vs `"baba"` (why the second map is needed)

| i | a → b | sToT[a] | tToS[b] | action |
|---|---|---|---|---|
| 0 | b → b | — | — | record b↔b |
| 1 | a → a | — | — | record a↔a |
| 2 | d → b | — (d is new) | **b** (b is already mapped from `b`) | not both new; `tToS[b]` ≠ d → **false** |

With only the s→t map, position 2 would look fine (d has no mapping yet), and the answer would wrongly be true.

### Dry run 2: `"foo"` vs `"bar"`

| i | a → b | action |
|---|---|---|
| 0 | f → b | record |
| 1 | o → a | record |
| 2 | o → r | sToT[o] = a, not r → **false** ✓ |

### Dry run 3: `"paper"` vs `"title"`
p↔t, a↔i, p→t (consistent), e↔l, r↔e → every step is new or consistent → **true** ✓

### Complexity
- **Time O(n)**, **Space O(1)** (two arrays of 256).

---

## 6. Approach 3: Compare "last seen" positions (the file's second method)

### Code ([E07_IsomorphicStrings.java:54-62](E07_IsomorphicStrings.java#L54-L62))

```java
static boolean isIsomorphicPattern(String s, String t) {
    int[] lastS = new int[256], lastT = new int[256];
    for (int i = 0; i < s.length(); i++) {
        if (lastS[s.charAt(i)] != lastT[t.charAt(i)]) return false;
        lastS[s.charAt(i)] = i + 1;
        lastT[t.charAt(i)] = i + 1;
    }
    return true;
}
```

### Idea
For each character, remember the (1-based) position where it was last seen (0 = never). At every position i, the s-character and the t-character must have the **same history**: both new, or both last seen at the same position. If the histories ever differ, the structures differ.

### Line by line

| Line | What it does |
|---|---|
| `lastS[...] != lastT[...]` | compare the histories of the two characters at position i |
| `lastS[s.charAt(i)] = i + 1;` | update the history (+1 so that 0 means "never seen") |

### Dry run: `"badc"` vs `"baba"`

| i | s char (last seen) | t char (last seen) | equal? |
|---|---|---|---|
| 0 | b (0) | b (0) | yes |
| 1 | a (0) | a (0) | yes |
| 2 | d (**0**: new) | b (**1**: seen at position 1) | **no → false** ✓ |

### Dry run: `"paper"` vs `"title"`

| i | s (last) | t (last) | equal? |
|---|---|---|---|
| 0 | p (0) | t (0) | ✓ |
| 1 | a (0) | i (0) | ✓ |
| 2 | p (1) | t (1) | ✓ |
| 3 | e (0) | l (0) | ✓ |
| 4 | r (0) | e (0) | ✓ |

**True** ✓. One comparison per position handles both directions at once.

---

## 7. Comparing the approaches

| | Two HashMaps | Two arrays | Last-seen pattern |
|---|---|---|---|
| Time | O(n) | O(n) | O(n) |
| Space | O(k) | O(1) (256 × 2) | O(1) |
| Any Unicode | **yes** | ASCII only | ASCII only (use maps for more) |
| Readability | **best** | good | clever |

---

## 8. Edge cases and common mistakes

**Edge cases:** a single character, identical strings, all characters the same (`"aaa"`/`"bbb"` → true), `"ab"`/`"aa"` → false.

**Mistakes:**
1. Using only one map, which misses many-to-one mappings (`badc`/`baba`).
2. Using 0 as "unmapped" without the +1 trick (if 0 can be a real character code).
3. Comparing two `Character` objects with `!=` (references). Comparing a `Character` with a `char` is fine.

---

## 9. Interview questions and answers

**Q1. Why do you need two maps?**
> One map enforces "each s-character has one target". The second enforces "each t-character has one source". A bijection needs both.

**Q2. Explain the last-seen-position approach.**
> Two strings are isomorphic iff they have the same "shape": at every index, the characters there were last seen at the same position (or both are new). Comparing those positions checks both directions at once.

**Q3. What's the space complexity?**
> O(1) for a fixed alphabet (256 slots each), O(k) with maps for k distinct characters.

### Follow-up questions

**F1. Word Pattern (LC 290).** → [E08](E08_WordPattern.md)
> The same bijection between characters and words.

**F2. Group the strings that are isomorphic to each other.**
> Map each string to a canonical "shape" key: replace each character with the index of its first occurrence, e.g. `"paper"` → `"0.1.0.3.4"`. Then group by the key.

**F3. Find and Replace Pattern (LC 890).**
> For each word, check the bijection with the pattern (or compare shape keys).

**F4. Can s be transformed into t when repeated conversions of one character into another are allowed (LC 1153)?**
> A harder graph problem; it needs a free "temporary" character if the mapping has cycles.

---

## 10. Running the file

```bash
java E07_IsomorphicStrings.java      # 6/6 tests passed
```

**One-line summary:** *"A bijection needs two maps (s→t and t→s), or matching last-seen positions at every index."*
