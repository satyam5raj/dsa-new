# H02 · Shortest Palindrome ⭐

| | |
|---|---|
| **LeetCode** | [#214 Shortest Palindrome](https://leetcode.com/problems/shortest-palindrome/) |
| **Difficulty** | Hard |
| **Pattern** | KMP's LPS array on `s + '#' + reverse(s)` |
| **Code** | [H02_ShortestPalindrome.java](H02_ShortestPalindrome.java) |
| **Run** | `java H02_ShortestPalindrome.java` |

⭐ = a bonus classic: KMP in action.

---

## 1. The problem

You may add characters **only at the front** of `s`. Return the **shortest** palindrome you can make.

| s | answer | added |
|---|---|---|
| `"aacecaaa"` | `"aaacecaaa"` | one `a` |
| `"abcd"` | `"dcbabcd"` | `dcb` |
| `""` | `""` | |
| `"racecar"` | `"racecar"` | nothing (already a palindrome) |
| `"abab"` | `"babab"` | `b` |

**Constraints:** `0 ≤ n ≤ 5 × 10⁴`, lowercase letters.

---

## 2. How to think about it

### Key observation: the longest palindromic prefix
Split s into **[the longest prefix that is a palindrome] + [the rest]**. The rest has no partner on the left, so it must be **mirrored in front**:

```
s      = [ palindromic prefix ][ rest ]
answer = reverse(rest) + s
```

| s | longest palindromic prefix | rest | answer |
|---|---|---|---|
| `aacecaaa` | `aacecaa` | `a` | `a` + `aacecaaa` |
| `abcd` | `a` | `bcd` | `dcb` + `abcd` |
| `abab` | `aba` | `b` | `b` + `abab` |

Adding fewer characters is impossible: the characters in front have to mirror everything that isn't covered by a palindromic prefix, so the longest such prefix gives the shortest result.

So the whole problem is: **find the longest palindromic prefix**.

### Brute force: O(n²)
Try the prefix lengths from n down to 0 and return at the first palindrome.

### The KMP trick: O(n)
A prefix P of s is a palindrome ⇔ **P equals the matching suffix of reverse(s)** (reading P backwards gives the end of `reverse(s)`).

So we want the **longest prefix of s that is also a suffix of reverse(s)**. "The longest prefix that is also a suffix" is **exactly what KMP's LPS array computes** ([E12](../02-Easy/E12_FindFirstOccurrence.md)). Build:

```
combined = s + "#" + reverse(s)
```

and take `lps[last]`. The `#` separator (a character that isn't in s) stops a match from running across the two halves, so the result can't exceed |s|.

---

## 3. Java concepts used in this problem

### 3.1 Reversing a string

```java
String rev = new StringBuilder(s).reverse().toString();
```

Strings are immutable, so we reverse a `StringBuilder` copy.

### 3.2 `StringBuilder + String`

```java
return new StringBuilder(rest).reverse() + s;
```

`+` with a String operand calls `toString()` on the other side (the StringBuilder), then concatenates.

### 3.3 Reusing the LPS builder

`buildLps` is the same code as in [E12](../02-Easy/E12_FindFirstOccurrence.md): `lps[i]` = the length of the longest proper prefix of `p[0..i]` that is also a suffix of it. Here we only need its **last** value.

---

## 4. Approach 1: Brute force

### Code ([H02_ShortestPalindrome.java:63-73](H02_ShortestPalindrome.java#L63-L73))

```java
static String shortestPalindromeBrute(String s) {
    for (int len = s.length(); len >= 0; len--) {
        if (isPal(s, len)) return new StringBuilder(s.substring(len)).reverse() + s;
    }
    return s;
}

static boolean isPal(String s, int len) {
    for (int i = 0, j = len - 1; i < j; i++, j--) if (s.charAt(i) != s.charAt(j)) return false;
    return true;
}
```

| Line | What it does |
|---|---|
| `for (int len = n; len >= 0; len--)` | the longest prefix first |
| `isPal(s, len)` | is `s[0..len−1]` a palindrome? |
| `reverse(s.substring(len)) + s` | mirror the rest in front |

**O(n²)** (e.g. `"aaaa…ab"` checks many long prefixes). It's the test reference.

---

## 5. Approach 2: KMP on `s + "#" + reverse(s)` (the file's solution)

### Code ([H02_ShortestPalindrome.java:44-61](H02_ShortestPalindrome.java#L44-L61))

```java
static String shortestPalindrome(String s) {
    String rev = new StringBuilder(s).reverse().toString();
    String combined = s + "#" + rev;
    int[] lps = buildLps(combined);
    int palPrefixLen = lps[combined.length() - 1];          // longest palindromic prefix of s
    String rest = s.substring(palPrefixLen);
    return new StringBuilder(rest).reverse() + s;
}

static int[] buildLps(String p) {
    int[] lps = new int[p.length()];
    for (int i = 1, len = 0; i < p.length(); ) {
        if (p.charAt(i) == p.charAt(len)) lps[i++] = ++len;
        else if (len > 0) len = lps[len - 1];
        else lps[i++] = 0;
    }
    return lps;
}
```

### Line by line

| Line | What it does |
|---|---|
| `rev = reverse(s)` | the mirror image |
| `combined = s + "#" + rev` | a prefix of `combined` is a prefix of s; a suffix of `combined` is a suffix of rev |
| `lps[last]` | the longest prefix of s that is also a suffix of rev, i.e. the longest palindromic prefix |
| `rest = s.substring(palPrefixLen)` | the part with no mirror |
| `reverse(rest) + s` | the answer |

### Dry run 1: `s = "abab"`

`combined = "abab#baba"`

| i | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
|---|---|---|---|---|---|---|---|---|---|
| char | a | b | a | b | # | b | a | b | a |
| lps | 0 | 0 | 1 | 2 | 0 | 0 | 1 | 2 | **3** |

`lps[8] = 3`: `"aba"` is both a prefix of combined and a suffix of it → the longest palindromic prefix is `"aba"` → rest = `"b"` → **`"b" + "abab"` = `"babab"`** ✓

### Dry run 2: `s = "abcd"`

`combined = "abcd#dcba"` → `lps = [0,0,0,0,0,0,0,0,1]` → `lps[8] = 1` (`"a"`) → rest = `"bcd"` → **`"dcb" + "abcd"` = `"dcbabcd"`** ✓

### Dry run 3: `s = "aacecaaa"`

`combined = "aacecaaa#aaacecaa"`, and the last LPS value is **7** → the palindromic prefix is `"aacecaa"` → rest = `"a"` → **`"aaacecaaa"`** ✓

### Why the `#` matters
Without it, for `s = "aaa"`, `combined = "aaaaaa"` would have `lps[last] = 5`, longer than s itself. The separator caps the match at |s|.

### Complexity
- **Time O(n):** the LPS of a string of length 2n + 1.
- **Space O(n).**

---

## 6. Comparing the approaches

| | Brute force | KMP |
|---|---|---|
| Time | O(n²) | **O(n)** |
| Space | O(n) | O(n) |

(Rolling hashes also work in O(n) on average: compare the forward and backward hashes of each prefix.)

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty string, an already-palindromic string (nothing to add), a single character, all characters equal.

**Mistakes:**
1. Forgetting the separator, so the LPS can exceed |s|.
2. Adding characters at the **end** (the problem allows only the front).
3. Searching for the longest palindromic **substring** instead of the longest palindromic **prefix**.

---

## 8. Interview questions and answers

**Q1. Why is the answer determined by the longest palindromic prefix?**
> The characters we add in front must mirror the end of s. The part of s that's already a palindrome at the front needs no mirror; everything after it must be mirrored. A longer palindromic prefix means fewer added characters.

**Q2. How does KMP find the longest palindromic prefix?**
> A prefix of s is a palindrome iff it equals the corresponding suffix of reverse(s). The LPS of `s + '#' + reverse(s)` at the last position is exactly the longest such prefix-suffix match.

### Follow-up questions

**F1. Add characters only at the end.**
> Symmetric: find the longest palindromic **suffix** (the LPS of `reverse(s) + "#" + s`), then append the reverse of the remaining prefix.

**F2. The minimum insertions anywhere (LC 1312).**
> `n − LPS(s)`, where LPS is the longest palindromic **subsequence**: an O(n²) DP.

**F3. Other uses of the LPS array.**
> Repeated Substring Pattern (period = n − lps[n−1]), counting the occurrences of a pattern, and string periodicity.

---

## 9. Running the file

```bash
java H02_ShortestPalindrome.java      # 6/6 tests passed (including 500 random cross-checks)
```

**One-line summary:** *"Answer = reverse(s after its longest palindromic prefix) + s; get that prefix from the LPS of s + '#' + reverse(s)."*
