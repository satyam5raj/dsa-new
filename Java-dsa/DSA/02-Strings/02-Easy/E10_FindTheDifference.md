# E10 · Find the Difference

| | |
|---|---|
| **LeetCode** | [#389 Find the Difference](https://leetcode.com/problems/find-the-difference/) |
| **Difficulty** | Easy |
| **Pattern** | XOR / character sum (cancel the common part) |
| **Code** | [E10_FindTheDifference.java](E10_FindTheDifference.java) |
| **Run** | `java E10_FindTheDifference.java` |

---

## 1. The problem

`t` is `s` shuffled, with **one extra letter** added at a random position. Find that letter.

| s | t | answer |
|---|---|---|
| `"abcd"` | `"abcde"` | `'e'` |
| `""` | `"y"` | `'y'` |
| `"ab"` | `"bab"` | `'b'` (the extra letter can be a duplicate of an existing one) |

**Constraints:** `0 ≤ s.length ≤ 1000`, `t.length = s.length + 1`, lowercase letters.

---

## 2. How to think about it

Everything in `s` also appears in `t`; we want the leftover. Three ways to "cancel what's common":

1. **Count:** +1 for each letter of t, −1 for each letter of s; the letter left at +1 is the answer.
2. **Sum of character codes:** `sum(t) − sum(s)` = the code of the extra letter.
3. **XOR:** XOR all characters of s **and** t together. Every common letter appears an even number of times and cancels (`x ^ x = 0`). Only the extra letter survives.

This is the same trick as [Missing Number](../../01-Arrays/02-Easy/E10_MissingNumber.md) and [Single Number](../../01-Arrays/02-Easy/E14_SingleNumber.md) in the arrays section.

---

## 3. Java concepts used in this problem

### 3.1 `char` arithmetic and casting back

```java
int sum = 0;
sum += 'e';          // 101: a char is added as its numeric code (widened to int)
char c = (char) 101; // 'e': an explicit cast is needed going from int back to char
```

- `char` + `int` → `int`. To get a `char` back you need a **narrowing cast** `(char)`.
- `char x = 0; x ^= 'a';` compiles because **compound assignment** (`^=`, `+=`, …) casts back to the left-hand type automatically. Writing `x = x ^ 'a';` would **not** compile without a cast, because `x ^ 'a'` is an `int`.

### 3.2 XOR on characters

XOR works on the bits of the character codes, exactly as for ints. A `char` is a 16-bit number, so the result is a valid `char` code. See [Single Number](../../01-Arrays/02-Easy/E14_SingleNumber.md) for the XOR rules.

### 3.3 `char` in the test helper

`check("...", 'e', result)`: the expected value is a `char` literal (single quotes). `'e'` is a character; `"e"` is a String. They're different types.

---

## 4. Approach 1: Count array

```java
static char findTheDifferenceCount(String s, String t) {
    int[] count = new int[26];
    for (char c : t.toCharArray()) count[c - 'a']++;
    for (char c : s.toCharArray()) count[c - 'a']--;
    for (int i = 0; i < 26; i++) if (count[i] == 1) return (char) ('a' + i);
    return ' ';                                       // unreachable for valid input
}
```

- `(char) ('a' + i)` turns the index back into a letter.
- **Dry run** `"ab"` / `"bab"`: t gives a:1, b:2; s subtracts a:1, b:1 → b:1 → **'b'** ✓
- **O(n) time, O(1) space.**

---

## 5. Approach 2: Sum of character codes (the file's second method)

### Code ([E10_FindTheDifference.java:41-46](E10_FindTheDifference.java#L41-L46))

```java
static char findTheDifferenceSum(String s, String t) {
    int sum = 0;
    for (char c : t.toCharArray()) sum += c;
    for (char c : s.toCharArray()) sum -= c;
    return (char) sum;
}
```

| Line | What it does |
|---|---|
| `sum += c` for t | add all of t's codes |
| `sum -= c` for s | subtract all of s's codes; the common letters cancel |
| `return (char) sum;` | what's left is the extra letter's code |

**Dry run** `"abcd"` / `"abcde"`: t = 97+98+99+100+101 = 495; s = 394 → 495 − 394 = **101 = 'e'** ✓

**Overflow?** At most 1001 × 122 ≈ 122,000, which is tiny for an `int`. **O(n) time, O(1) space.**

---

## 6. Approach 3: XOR (the file's solution)

### Code ([E10_FindTheDifference.java:34-39](E10_FindTheDifference.java#L34-L39))

```java
static char findTheDifference(String s, String t) {
    char x = 0;
    for (int i = 0; i < s.length(); i++) x ^= s.charAt(i);
    for (int i = 0; i < t.length(); i++) x ^= t.charAt(i);
    return x;
}
```

### Line by line

| Line | What it does |
|---|---|
| `char x = 0;` | 0 is the XOR identity |
| `x ^= s.charAt(i);` | fold in every character of s (`^=` casts back to `char` automatically) |
| `x ^= t.charAt(i);` | fold in every character of t |
| `return x;` | the common letters appeared twice and cancelled; the extra one remains |

### Dry run: `"ab"` / `"bab"`

| step | char | x (binary, last 7 bits) | x |
|---|---|---|---|
| start | | 0000000 | 0 |
| s[0] | a (97) | 1100001 | 'a' |
| s[1] | b (98) | 0000011 | 3 |
| t[0] | b (98) | 1100001 | 'a' |
| t[1] | a (97) | 0000000 | 0 |
| t[2] | b (98) | 1100010 | **'b'** |

Rearranged: `(a^a) ^ (b^b) ^ b = b` ✓

### Dry run: `""` / `"y"`
`0 ^ 'y' = 'y'` ✓

### Complexity
- **Time O(n)**, **Space O(1)**, with no overflow concerns at all.

---

## 7. Comparing the approaches

| | Count | Sum | XOR |
|---|---|---|---|
| Time | O(n) | O(n) | O(n) |
| Space | O(26) | O(1) | O(1) |
| Overflow risk | none | tiny (huge inputs only) | **none** |
| Generalises to "find the k extra letters" | **yes** | no | no |

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty s, the extra letter duplicating an existing one, the extra letter at the start or end.

**Mistakes:**
1. Looking for a letter of t that's **absent** from s. That fails when the extra letter is a duplicate (`"ab"` / `"bab"`).
2. Forgetting the `(char)` cast in the sum version.
3. Comparing sorted strings position by position without handling the last extra position.

---

## 9. Interview questions and answers

**Q1. Why does XOR work even when the extra letter already exists in s?**
> XOR cancels in pairs regardless of position. The duplicate letter appears an odd number of times in total (s + t), so exactly one copy survives.

**Q2. Which approach would you choose?**
> XOR: one pass, O(1) space, no overflow. The counting approach generalises if several letters could be extra.

### Follow-up questions

**F1. t has k extra letters.**
> The count approach: the letters with a positive count (with their multiplicities).

**F2. Missing Number / Single Number.** → [Arrays E10](../../01-Arrays/02-Easy/E10_MissingNumber.md), [Arrays E14](../../01-Arrays/02-Easy/E14_SingleNumber.md)
> The same "cancel the common part" idea on integers.

**F3. Find the extra element between two large unsorted files.**
> Stream both files, XOR (or sum) everything: O(1) memory, two passes.

---

## 10. Running the file

```bash
java E10_FindTheDifference.java      # 4/4 tests passed
```

**One-line summary:** *"XOR every character of s and t: the pairs cancel, the extra letter remains."*
