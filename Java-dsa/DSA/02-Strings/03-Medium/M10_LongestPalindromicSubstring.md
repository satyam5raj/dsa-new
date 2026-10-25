# M10 · Longest Palindromic Substring

| | |
|---|---|
| **LeetCode** | [#5 Longest Palindromic Substring](https://leetcode.com/problems/longest-palindromic-substring/) |
| **Difficulty** | Medium |
| **Pattern** | Expand around the centre (also DP, and Manacher's O(n)) |
| **Code** | [M10_LongestPalindromicSubstring.java](M10_LongestPalindromicSubstring.java) |
| **Run** | `java M10_LongestPalindromicSubstring.java` |

---

## 1. The problem

Return the longest **substring** of `s` that is a palindrome.

| s | answer |
|---|---|
| `"babad"` | `"bab"` (`"aba"` is also valid) |
| `"cbbd"` | `"bb"` |
| `"a"` | `"a"` |
| `"forgeeksskeegfor"` | `"geeksskeeg"` |
| `"racecar"` | `"racecar"` |

**Constraints:** `1 ≤ n ≤ 1000`, digits and English letters.

---

## 2. How to think about it

1. **Brute force:** every substring (about n²/2) × an O(n) palindrome check = **O(n³)**.
2. **DP:** "a substring is a palindrome if its two ends match **and** its inside is a palindrome":
   ```
   pal[i][j] = s[i] == s[j]  &&  (j − i < 2  ||  pal[i+1][j−1])
   ```
   Fill the table by increasing length: O(n²) time, **O(n²) space**.
3. **Expand around the centre** (the usual interview answer): every palindrome mirrors around a **centre**:
   - **odd** length, `"aba"`: the centre is a **character**
   - **even** length, `"abba"`: the centre is the **gap** between two characters

   There are n character centres and n − 1 gaps, so **2n − 1 centres**. From each one, expand outward while `s[l] == s[r]`. Track the longest. **O(n²) time, O(1) space.**

Thinking "**from the centre outward**" is much easier than checking from the ends inward.

---

## 3. Java concepts used in this problem

### 3.1 A helper returning a length, then computing the start

```java
int len = Math.max(odd, even);
bestStart = center - (len - 1) / 2;   // works for both odd and even lengths
```

Why does this formula work for both?
- **Odd** length 3 centred at c: the start is c − 1 = c − (3 − 1)/2 ✓
- **Even** length 4, with the centre gap between c and c+1: the start is c − 1 = c − (4 − 1)/2 = c − 1 ✓ (integer division: 3/2 = 1)

### 3.2 Why `expand` returns `r - l - 1`

```java
while (l >= 0 && r < s.length() && s.charAt(l) == s.charAt(r)) { l--; r++; }
return r - l - 1;
```

The loop stops **one step too far** on each side (at the first mismatch or past the ends). The palindrome is `s[l+1 .. r−1]`, whose length is `(r − 1) − (l + 1) + 1 = r − l − 1`. For an even centre with `s[c] != s[c+1]`, the loop doesn't run: `(c + 1) − c − 1 = 0` ✓

### 3.3 A 2D `boolean` table for DP

```java
boolean[][] pal = new boolean[n][n];   // all false at first
```

It's an n × n table: 10⁶ booleans for n = 1000, about 1 MB. That's fine here, but it's why the O(1)-space expansion is preferred.

---

## 4. Approach 1: Brute force

```java
static String longestPalindromeBrute(String s) {
    String best = "";
    for (int i = 0; i < s.length(); i++)
        for (int j = i; j < s.length(); j++)
            if (j - i + 1 > best.length() && isPal(s, i, j)) best = s.substring(i, j + 1);
    return best;
}
static boolean isPal(String s, int i, int j) {
    while (i < j) if (s.charAt(i++) != s.charAt(j--)) return false;
    return true;
}
```

**O(n³)**: about 10⁹ for n = 1000, which is too slow. (Checking the length first skips some palindrome checks, but not the worst case.)

---

## 5. Approach 2: Dynamic programming (the file's second method)

### Code ([M10_LongestPalindromicSubstring.java:67-78](M10_LongestPalindromicSubstring.java#L67-L78))

```java
static String longestPalindromeDp(String s) {
    int n = s.length(), start = 0, best = n == 0 ? 0 : 1;
    boolean[][] pal = new boolean[n][n];
    for (int len = 1; len <= n; len++) {
        for (int i = 0; i + len - 1 < n; i++) {
            int j = i + len - 1;
            pal[i][j] = s.charAt(i) == s.charAt(j) && (len <= 2 || pal[i + 1][j - 1]);
            if (pal[i][j] && len > best) { best = len; start = i; }
        }
    }
    return s.substring(start, start + best);
}
```

### Line by line

| Line | What it does |
|---|---|
| `boolean[][] pal = new boolean[n][n];` | `pal[i][j]` = "is `s[i..j]` a palindrome?" |
| `for (int len = 1; len <= n; len++)` | **shortest first**, so the inner part `pal[i+1][j−1]` (length − 2) is already computed |
| `int j = i + len - 1;` | the end of the substring of length `len` starting at i |
| `s[i] == s[j] && (len <= 2 \|\| pal[i+1][j-1])` | the ends match, and the inside is a palindrome (lengths 1 and 2 have no real inside) |
| `if (pal[i][j] && len > best)` | record the longest |

**Dry run** `"cbbd"` (only the true entries):

| len | true entries | best |
|---|---|---|
| 1 | (0,0) c, (1,1) b, (2,2) b, (3,3) d | 1 |
| 2 | (1,2) `"bb"` | **2**, start 1 |
| 3 | (0,2) c≠b ✗, (1,3) b≠d ✗ | |
| 4 | (0,3) c≠d ✗ | |

**Return `"bb"`** ✓. **O(n²) time, O(n²) space.** (The DP topic covers this in depth.)

---

## 6. Approach 3: Expand around the centre (the file's main solution)

### Code ([M10_LongestPalindromicSubstring.java:43-65](M10_LongestPalindromicSubstring.java#L43-L65))

```java
static String longestPalindrome(String s) {
    if (s.isEmpty()) return "";
    int bestStart = 0, bestLen = 1;
    for (int center = 0; center < s.length(); center++) {
        int odd = expand(s, center, center);          // "aba"
        int even = expand(s, center, center + 1);     // "abba"
        int len = Math.max(odd, even);
        if (len > bestLen) {
            bestLen = len;
            bestStart = center - (len - 1) / 2;       // works for both odd and even lengths
        }
    }
    return s.substring(bestStart, bestStart + bestLen);
}

// returns the length of the longest palindrome centered between l and r
static int expand(String s, int l, int r) {
    while (l >= 0 && r < s.length() && s.charAt(l) == s.charAt(r)) {
        l--;
        r++;
    }
    return r - l - 1;                                 // the loop overshot by one on each side
}
```

### Line by line

| Line | What it does |
|---|---|
| `int bestStart = 0, bestLen = 1;` | any single character is a palindrome of length 1 |
| `expand(s, center, center)` | the odd palindrome centred on the character |
| `expand(s, center, center + 1)` | the even palindrome centred on the gap after it |
| `if (len > bestLen) {...}` | remember where the best one starts |
| `return s.substring(bestStart, bestStart + bestLen);` | build the answer **once**, at the end (no substring creation inside the loop) |
| `expand`: `while (in bounds && s[l] == s[r]) { l--; r++; }` | grow outward while the mirror characters match |

### Dry run 1: `"babad"`

| center | odd length | even length | best so far |
|---|---|---|---|
| 0 (b) | 1 (`b`) | 0 (b ≠ a) | 1 |
| 1 (a) | **3** (`bab`) | 0 | **3**, start = 1 − 1 = 0 → `"bab"` |
| 2 (b) | 3 (`aba`), not > 3 | 0 | 3 |
| 3 (a) | 1 | 0 | 3 |
| 4 (d) | 1 | 0 | 3 |

**Return `"bab"`** ✓ (`"aba"` has the same length but isn't strictly longer, so the first one found stays).

### Dry run 2: `"cbbd"`

| center | odd | even | best |
|---|---|---|---|
| 0 (c) | 1 | 0 (c ≠ b) | 1 |
| 1 (b) | 1 | **2** (`bb`; then c ≠ d stops it) | **2**, start = 1 − (2−1)/2 = 1 → `"bb"` |
| 2 (b) | 1 | 0 (b ≠ d) | 2 |
| 3 (d) | 1 | 0 (out of bounds) | 2 |

**Return `"bb"`** ✓

### Complexity
- **Time O(n²):** 2n − 1 centres, each expanding up to O(n).
- **Space O(1).**

---

## 7. (Bonus) Manacher's algorithm, O(n)

Manacher's algorithm reuses the **mirror** information from palindromes it has already found: inside a big palindrome, the palindrome around position i mirrors the one around its reflection. It reaches O(n), much like the Z-algorithm. It's rarely expected in interviews; it's enough to know it exists and what idea it's built on.

---

## 8. Comparing the approaches

| | Brute | DP table | Expand around centre | Manacher |
|---|---|---|---|---|
| Time | O(n³) | O(n²) | O(n²) | **O(n)** |
| Space | O(1) | O(n²) | **O(1)** | O(n) |
| Interview default | — | ok | **✓** | rarely |

---

## 9. Edge cases and common mistakes

**Edge cases:** a single character, all characters equal (`"aaaa"`), an even-length answer (`"bb"`), the whole string is a palindrome.

**Mistakes:**
1. Only checking **odd** centres, which misses `"abba"`-type answers.
2. A wrong start index for even lengths (use `center − (len − 1) / 2`).
3. Calling `substring` inside the loop for every candidate (it allocates; only build the final one).
4. In the DP, filling the table in the wrong order (the inside must be computed first).

---

## 10. Interview questions and answers

**Q1. Why 2n − 1 centres?**
> Odd palindromes centre on one of the n characters; even ones centre on one of the n − 1 gaps between characters.

**Q2. DP versus expansion: which, and why?**
> Both are O(n²) time, but expansion uses O(1) space and is simpler. The DP table costs O(n²) memory.

**Q3. Can it be done faster than O(n²)?**
> Yes, with Manacher's algorithm in O(n), using the symmetry of palindromes to avoid re-expanding. A suffix-structure approach also works.

### Follow-up questions

**F1. Count all palindromic substrings (LC 647).** → [M11](M11_PalindromicSubstrings.md)
> The same expansion, adding 1 per successful step.

**F2. Longest palindromic **subsequence** (LC 516).**
> A different problem (not contiguous): DP `dp[i][j] = dp[i+1][j−1] + 2` if the ends match, else `max(dp[i+1][j], dp[i][j−1])`. See [15-DP](../../15-Dynamic-Programming/README.md).

**F3. Shortest Palindrome by adding characters in front (LC 214).** → [H02](../04-Hard/H02_ShortestPalindrome.md)
> Find the longest palindromic **prefix** with KMP.

**F4. Palindrome Partitioning (LC 131).** → [14-Backtracking M10](../../14-Backtracking/03-Medium/M10_PalindromePartitioning.md)
> Precompute `pal[i][j]` (this DP), then backtrack over the cut positions.

---

## 11. Running the file

```bash
java M10_LongestPalindromicSubstring.java      # 6/6 tests passed
```

**One-line summary:** *"Expand around each of the 2n − 1 centres (characters and gaps); keep the longest."*
