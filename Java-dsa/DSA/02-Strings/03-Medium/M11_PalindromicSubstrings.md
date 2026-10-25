# M11 · Palindromic Substrings

| | |
|---|---|
| **LeetCode** | [#647 Palindromic Substrings](https://leetcode.com/problems/palindromic-substrings/) |
| **Difficulty** | Medium |
| **Pattern** | Expand around the centre (count instead of max) |
| **Code** | [M11_PalindromicSubstrings.java](M11_PalindromicSubstrings.java) |
| **Run** | `java M11_PalindromicSubstrings.java` |

---

## 1. The problem

**Count** the palindromic substrings of `s`. Substrings at different positions count separately, even if they look the same.

| s | answer | palindromes |
|---|---|---|
| `"abc"` | 3 | a, b, c |
| `"aaa"` | 6 | a, a, a, aa, aa, aaa |
| `"abba"` | 6 | a, b, b, a, bb, abba |
| `"a"` | 1 | a |

**Constraints:** `1 ≤ n ≤ 1000`, lowercase letters.

---

## 2. How to think about it

This uses the same engine as [M10 Longest Palindromic Substring](M10_LongestPalindromicSubstring.md): from each of the **2n − 1 centres** (characters for odd lengths, gaps for even lengths), expand outward.

**Every successful expansion step is one more palindrome.** Expanding from the middle `a` of `"aaa"`: `"a"` (+1), then `"aaa"` (+1). So instead of tracking the longest, **add 1 per successful step**.

Why does this count each palindrome exactly once? Every palindrome has exactly **one** centre (its middle character or middle gap), and the expansion from that centre reaches it exactly once.

---

## 3. Java concepts used in this problem

### 3.1 Two calls per centre, summed

```java
count += expandCount(s, center, center);       // odd
count += expandCount(s, center, center + 1);   // even
```

Calling one helper with different starting pointers handles both parities with the same code.

### 3.2 Counting inside the loop

```java
while (in bounds && s.charAt(l) == s.charAt(r)) {
    found++;         // s[l..r] is a palindrome
    l--; r++;
}
```

The count is incremented **before** expanding further: the current `s[l..r]` has just been confirmed as a palindrome.

---

## 4. Approach 1: Brute force

```java
static int countSubstringsBrute(String s) {
    int count = 0;
    for (int i = 0; i < s.length(); i++)
        for (int j = i; j < s.length(); j++)
            if (isPal(s, i, j)) count++;
    return count;
}
static boolean isPal(String s, int i, int j) {
    while (i < j) if (s.charAt(i++) != s.charAt(j--)) return false;
    return true;
}
```

**O(n³)**: every substring, each checked in O(n).

---

## 5. Approach 2: DP table

```java
static int countSubstringsDp(String s) {
    int n = s.length(), count = 0;
    boolean[][] pal = new boolean[n][n];
    for (int len = 1; len <= n; len++)
        for (int i = 0; i + len - 1 < n; i++) {
            int j = i + len - 1;
            pal[i][j] = s.charAt(i) == s.charAt(j) && (len <= 2 || pal[i + 1][j - 1]);
            if (pal[i][j]) count++;
        }
    return count;
}
```

The same table as M10, counting the `true` entries. **O(n²) time and space.**

---

## 6. Approach 3: Expand around the centre (the file's solution)

### Code ([M11_PalindromicSubstrings.java:36-53](M11_PalindromicSubstrings.java#L36-L53))

```java
static int countSubstrings(String s) {
    int count = 0;
    for (int center = 0; center < s.length(); center++) {
        count += expandCount(s, center, center);       // odd-length palindromes
        count += expandCount(s, center, center + 1);   // even-length palindromes
    }
    return count;
}

static int expandCount(String s, int l, int r) {
    int found = 0;
    while (l >= 0 && r < s.length() && s.charAt(l) == s.charAt(r)) {
        found++;                                       // s[l..r] is a palindrome
        l--;
        r++;
    }
    return found;
}
```

### Line by line

| Line | What it does |
|---|---|
| `for (int center = 0; ...)` | each character position |
| `expandCount(s, center, center)` | the palindromes with this character as their middle |
| `expandCount(s, center, center + 1)` | the palindromes with the gap after it as their middle |
| `found++` inside the loop | each matching step confirms one palindrome `s[l..r]` |
| `return found;` | the number of palindromes with this centre |

### Dry run 1: `"aaa"`

| centre | type | palindromes found | count |
|---|---|---|---|
| 0 | odd | `a` | 1 |
| 0\|1 | even | `aa` | 1 |
| 1 | odd | `a`, `aaa` | 2 |
| 1\|2 | even | `aa` | 1 |
| 2 | odd | `a` | 1 |
| 2\|3 | even | out of bounds | 0 |

Total = **6** ✓

### Dry run 2: `"abba"`

| centre | found |
|---|---|
| 0 odd | a |
| 0\|1 even | — (a ≠ b) |
| 1 odd | b |
| 1\|2 even | bb, then abba |
| 2 odd | b |
| 2\|3 even | — |
| 3 odd | a |

Total = 1 + 1 + 2 + 1 + 1 = **6** ✓

### Complexity
- **Time O(n²)** in the worst case (`"aaaa…"` really has about n²/2 palindromes, so you can't do better than counting them one by one with this method).
- **Space O(1).**

---

## 7. Comparing the approaches

| | Brute | DP | Expand |
|---|---|---|---|
| Time | O(n³) | O(n²) | **O(n²)** |
| Space | O(1) | O(n²) | **O(1)** |

(Manacher's algorithm counts them in O(n): the palindrome radius at each centre directly tells you how many palindromes it contributes.)

---

## 8. Edge cases and common mistakes

**Edge cases:** a single character, all the same character, no palindromes longer than 1 (the count = n).

**Mistakes:**
1. Forgetting the even centres.
2. Counting distinct palindromes instead of positions (`"aaa"` gives 6, not 3).
3. Breaking out of the loop too early: the expansion stops only at a mismatch or a boundary.

---

## 9. Interview questions and answers

**Q1. Why is every palindrome counted exactly once?**
> Each palindrome has a unique centre: its middle character (odd length) or its middle gap (even length). Expanding from that centre reaches it exactly once.

**Q2. Can this be faster than O(n²)?**
> With Manacher's algorithm, yes, O(n): the sum over all centres of ⌈radius/2⌉ gives the count. The expansion method can't be, since outputs like `"aaaa"` contain Θ(n²) palindromes and it visits each one.

### Follow-up questions

**F1. Count the distinct palindromic substrings.**
> Put each found palindrome into a `HashSet<String>` (expensive), or use a palindromic tree (eertree), O(n).

**F2. Palindrome Partitioning II (LC 132): the minimum cuts.**
> Precompute `pal[i][j]`, then `cuts[j] = min(cuts[i−1] + 1)` over all i with `pal[i][j]`. O(n²).

**F3. Count the palindromic subsequences.**
> An interval DP; it's different, because subsequences need not be contiguous.

---

## 10. Running the file

```bash
java M11_PalindromicSubstrings.java      # 4/4 tests passed
```

**One-line summary:** *"Expand from each of the 2n − 1 centres; every successful step is one palindrome."*
