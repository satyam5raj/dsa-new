# E01 · Valid Anagram

| | |
|---|---|
| **LeetCode** | [#242 Valid Anagram](https://leetcode.com/problems/valid-anagram/) |
| **Difficulty** | Easy |
| **Pattern** | Frequency array |
| **Code** | [E01_ValidAnagram.java](E01_ValidAnagram.java) |
| **Run** | `java E01_ValidAnagram.java` |

---

## 1. The problem

Is `t` an **anagram** of `s`? That means it has the same letters with the same counts, in any order. The input is lowercase English letters only.

| s | t | answer | why |
|---|---|---|---|
| `"anagram"` | `"nagaram"` | true | both have a×3, g, m, n, r |
| `"rat"` | `"car"` | false | s has a `t`, t has a `c` |
| `"a"` | `"ab"` | false | different lengths |
| `"aacc"` | `"ccac"` | false | a×2 vs a×1 |

**Constraints:** `1 ≤ s.length, t.length ≤ 5 × 10⁴`, lowercase letters. Follow-up: *what if the inputs contain Unicode characters?*

---

## 2. How to think about it

"Same letters, same counts, any order" means **the order doesn't matter, only the counts do**. Two ways to ignore order:

1. **Sort both strings.** Anagrams become identical: `sorted("anagram") = "aaagmnr" = sorted("nagaram")`. O(n log n).
2. **Count the letters.** With only 26 possible letters, keep `int[26]`:
   - `count[c]++` for each character of s
   - `count[c]--` for each character of t
   - they're anagrams ⇔ **every counter ends at 0**

**Quick reject:** strings of different lengths can never be anagrams. Checking this first also lets one loop walk both strings at the same index.

---

## 3. Java concepts used in this problem

This is the first string problem, so here are the string basics you'll use in every problem after this.

### 3.1 `String` is immutable

A Java `String` **can never change** after it's created. Methods that look like they modify it (`toUpperCase`, `replace`, `substring`, `+`) always return a **new** String.

```java
String s = "hello";
s.toUpperCase();          // creates "HELLO" and throws it away. s is still "hello"
s = s.toUpperCase();      // now s points to the new string "HELLO"
```

That's why building a string in a loop with `+=` is slow (O(n²) copying) and why we use `StringBuilder` instead (see the StringBuilder notes from the Basics). To **edit** characters, convert the string to a `char[]`.

### 3.2 Reading characters: `length()` and `charAt(i)`

```java
String s = "rat";
s.length()      // 3   ← a METHOD with (), unlike array.length
s.charAt(0)     // 'r' ← a char, O(1)
s.charAt(3)     // StringIndexOutOfBoundsException
```

### 3.3 `char` is a number: `c - 'a'`

A `char` is a 16-bit **unsigned number** (its Unicode code). The lowercase letters are consecutive: `'a'` = 97, `'b'` = 98, …, `'z'` = 122. So:

```java
'a' - 'a'   // 0
'c' - 'a'   // 2
'z' - 'a'   // 25
```

`c - 'a'` turns a lowercase letter into an index 0..25, which is perfect for an `int[26]`. You'll use it in almost every string problem. Related tricks:

| Expression | Meaning |
|---|---|
| `c - 'a'` | letter → 0..25 |
| `(char) ('a' + i)` | 0..25 → letter (the cast is needed: `'a' + i` is an `int`) |
| `c - '0'` | digit character → its number (`'7' - '0'` = 7) |
| `int[128]` | counts for any ASCII character, indexed by `c` directly |

### 3.4 A frequency array: `int[26]`

```java
int[] count = new int[26];        // all zeros
count[s.charAt(i) - 'a']++;       // one more of this letter
```

It's an array version of `HashMap<Character, Integer>`, but much faster (no hashing, no boxing) and only 26 slots. It works whenever the alphabet is small and known in advance.

### 3.5 `toCharArray()` and sorting characters

```java
char[] a = s.toCharArray();   // a NEW char array copy of the string, O(n)
Arrays.sort(a);               // sorts the characters (by their numeric codes)
Arrays.equals(a, b);          // true if same length and same elements in order
```

`a == b` would compare **references** (are they the same array object?), which is false for two different arrays even with the same contents. `Arrays.equals` compares the contents.

### 3.6 Comparing strings: `.equals`, never `==`

```java
String x = "abc", y = new String("abc");
x == y          // false: two different objects
x.equals(y)     // true: same characters
```

`==` on objects compares **references**. It sometimes appears to work for string literals (Java reuses identical literals from the "string pool"), which makes the bug sneaky. Always use `.equals()` (or `equalsIgnoreCase`).

### 3.7 `Map<Character, Integer>`: for the Unicode follow-up

`char` values get autoboxed into `Character` objects. A map handles **any** character, not just a–z.

---

## 4. Approach 1: Sort both and compare

### Code ([E01_ValidAnagram.java:47-52](E01_ValidAnagram.java#L47-L52))

```java
static boolean isAnagramSort(String s, String t) {
    char[] a = s.toCharArray(), b = t.toCharArray();
    Arrays.sort(a);
    Arrays.sort(b);
    return Arrays.equals(a, b);
}
```

| Line | What it does |
|---|---|
| `char[] a = s.toCharArray(), b = t.toCharArray();` | strings are immutable, so copy them into arrays we can sort |
| `Arrays.sort(a); Arrays.sort(b);` | anagrams become identical after sorting |
| `return Arrays.equals(a, b);` | compare contents (this also handles different lengths) |

**Dry run** `"listen"`, `"silent"`: both sort to `[e, i, l, n, s, t]` → **true**.
**Dry run** `"rat"`, `"car"`: `[a, r, t]` vs `[a, c, r]` → **false**.

**O(n log n) time, O(n) space** for the arrays.

---

## 5. Approach 2: Frequency array (the file's main solution)

### Code ([E01_ValidAnagram.java:34-45](E01_ValidAnagram.java#L34-L45))

```java
static boolean isAnagram(String s, String t) {
    if (s.length() != t.length()) return false;
    int[] count = new int[26];
    for (int i = 0; i < s.length(); i++) {
        count[s.charAt(i) - 'a']++;
        count[t.charAt(i) - 'a']--;
    }
    for (int c : count) {
        if (c != 0) return false;
    }
    return true;
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (s.length() != t.length()) return false;` | a quick reject; it also guarantees `t.charAt(i)` is valid in the loop |
| `int[] count = new int[26];` | one counter per letter, all 0 |
| `count[s.charAt(i) - 'a']++;` | s **adds** its letters |
| `count[t.charAt(i) - 'a']--;` | t **removes** its letters |
| `for (int c : count) if (c != 0) return false;` | any leftover means a letter count differs |
| `return true;` | all 26 counters balanced |

Doing both in the same loop works because the order of the `+1`s and `−1`s doesn't matter: only the final totals do.

### Dry run 1: `"rat"` vs `"car"`

| i | s[i] | t[i] | changes | non-zero counters after |
|---|---|---|---|---|
| 0 | r | c | r +1, c −1 | c:−1, r:+1 |
| 1 | a | a | a +1, a −1 | c:−1, r:+1 |
| 2 | t | r | t +1, r −1 | c:−1, t:+1 |

Counters c = −1 and t = +1 aren't zero → **false** ✓

### Dry run 2: `"anagram"` vs `"nagaram"`

Counting s: a:3, g:1, m:1, n:1, r:1. Counting t subtracts a:3, g:1, m:1, n:1, r:1. Every counter returns to 0 → **true** ✓

### Dry run 3: `"aacc"` vs `"ccac"`
a: +2 −1 = **+1**, c: +2 −3 = **−1** → **false** ✓

### Complexity
- **Time O(n):** one pass over both strings, plus a 26-step check.
- **Space O(1):** 26 counters, whatever n is.

---

## 6. Approach 3: HashMap (the Unicode follow-up)

### Code ([E01_ValidAnagram.java:54-62](E01_ValidAnagram.java#L54-L62))

```java
static boolean isAnagramUnicode(String s, String t) {
    if (s.length() != t.length()) return false;
    Map<Character, Integer> count = new HashMap<>();
    for (char c : s.toCharArray()) count.merge(c, 1, Integer::sum);
    for (char c : t.toCharArray()) {
        if (count.merge(c, -1, Integer::sum) < 0) return false;   // t has more of c than s
    }
    return true;
}
```

### Line by line

| Line | What it does |
|---|---|
| `for (char c : s.toCharArray())` | for-each over the characters |
| `count.merge(c, 1, Integer::sum)` | count s's characters (any Unicode char works as a key) |
| `count.merge(c, -1, Integer::sum) < 0` | use one up for t. **Below zero** means t has more of c than s, so stop early |
| `return true;` | the lengths are equal and no counter went negative, so all counters must be exactly 0 |

**Why is "never negative" enough?** The total of all counters starts at n (from s) and drops by exactly n (for t), ending at 0. If none of them is negative, they must all be 0.

**Dry run** `"héllo"` vs `"olléh"`: counts h:1, é:1, l:2, o:1. Then o → 0, l → 1, l → 0, é → 0, h → 0, never negative → **true** ✓

**O(n) time, O(k) space** for k distinct characters.

> **Unicode detail:** some characters (like emoji 😀) take **two** `char`s in Java (a "surrogate pair"). For full correctness, count **code points**: `s.codePoints()` gives an `IntStream` of real characters.

---

## 7. Comparing the approaches

| | Sort | `int[26]` | HashMap |
|---|---|---|---|
| Time | O(n log n) | **O(n)** | O(n) |
| Space | O(n) | **O(1)** | O(k) |
| Works for Unicode | yes | **no** | **yes** |

---

## 8. Edge cases and common mistakes

**Edge cases:** different lengths, the same string, a single character, repeated letters with different counts (`"aacc"` / `"ccac"`).

**Mistakes:**
1. Comparing strings with `==` instead of `.equals`.
2. Forgetting the length check. With a single loop, you'd read past the end of the shorter string.
3. Using a `Set` instead of counts: `"aab"` and `"abb"` have the same letter set but aren't anagrams.
4. Using `int[26]` when uppercase letters, digits or Unicode can appear (`'A' - 'a'` is negative → crash).

---

## 9. Interview questions and answers

**Q1. Why is the counting solution O(1) space?**
> The array has 26 slots no matter how long the strings are. The space depends on the alphabet size, not on n.

**Q2. What changes for Unicode?**
> A fixed `int[26]` doesn't cover other characters. Use a `HashMap<Character, Integer>` (or count code points to handle characters made of two chars).

**Q3. Can you do it with one array and one loop?**
> Yes: after checking the lengths, increment for s and decrement for t at the same index, then check that every counter is 0.

### Follow-up questions

**F1. Group Anagrams (LC 49).** → [M06](../03-Medium/M06_GroupAnagrams.md)
> Give each word a canonical key (its sorted letters, or its 26 counts as a string) and group by the key in a HashMap.

**F2. Find All Anagrams in a String (LC 438).** → [M05](../03-Medium/M05_FindAllAnagrams.md)
> A fixed-size sliding window of length |p| with a 26-counter array; compare the window's counts with p's counts.

**F3. Minimum number of steps to make two strings anagrams (LC 1347).**
> Count s minus t; the answer is the sum of the positive counters.

**F4. Is one string a permutation of another, ignoring spaces and case?**
> Normalise first (lowercase, skip spaces), then count.

---

## 10. Running the file

```bash
cd Java-dsa/DSA/02-Strings/02-Easy
java E01_ValidAnagram.java      # 6/6 tests passed
```

**One-line summary:** *"Same letters, same counts: +1 for s, −1 for t in an int[26]; anagram ⇔ all zeros."*
