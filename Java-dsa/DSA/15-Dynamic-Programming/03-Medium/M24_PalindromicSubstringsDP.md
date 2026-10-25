# M24 · Palindromes the DP Way

| | |
|---|---|
| **LeetCode** | [#5 Longest Palindromic Substring](https://leetcode.com/problems/longest-palindromic-substring/) and [#647 Palindromic Substrings](https://leetcode.com/problems/palindromic-substrings/) (+ #516) |
| **Difficulty** | Medium |
| **Pattern** | **Interval DP**: pal[i][j] from the inside, pal[i + 1][j − 1] |
| **Code** | [M24_PalindromicSubstringsDP.java](M24_PalindromicSubstringsDP.java) |
| **Run** | `java M24_PalindromicSubstringsDP.java` |

Both problems were solved with expand-around-centre in [Strings M10](../../02-Strings/03-Medium/M10_LongestPalindromicSubstring.md) and [Strings M11](../../02-Strings/03-Medium/M11_PalindromicSubstrings.md) (O(n²) time, O(1) space: still the one to use in interviews). This note is the DP view, because the palindrome table is the simplest **interval DP** and it's reused by harder problems ([Palindrome Partitioning](../../14-Backtracking/03-Medium/M10_PalindromePartitioning.md), minimum cuts, …).

---

## 1. The problems

| problem | input | answer |
|---|---|---|
| #5 the longest palindromic substring | "babad" | "aba" (or "bab") |
| | "cbbd" | "bb" |
| #647 count the palindromic substrings | "abc" | 3 |
| | "aaa" | 6 |
| #516 the longest palindromic **subsequence** (bonus) | "bbbab" | 4 ("bbbb") |

---

## 2. How to think about it

| step | for #5 and #647 |
|---|---|
| **State** | pal[i][j] = s[i..j] is a palindrome |
| **Transition** | pal[i][j] = s[i] == s[j] and (j − i < 2 or pal[i + 1][j − 1]): "the ends match and the inside is a palindrome" (lengths 1 and 2 have no inside) |
| **Order** | pal[i][j] reads pal[i + 1][j − 1], a **shorter** interval that starts **later**. So loop i from n − 1 **down** to 0, and j from i **up** to n − 1 (or loop by increasing length, the general interval-DP order) |
| **Answer** | #647: the number of true cells; #5: the widest true cell |

### The table for "babad" (from a real run)

| i \ j | 0 b | 1 a | 2 b | 3 a | 4 d |
|---|---|---|---|---|---|
| 0 b | T | F | **T** ("bab") | F | F |
| 1 a | | T | F | **T** ("aba") | F |
| 2 b | | | T | F | F |
| 3 a | | | | T | F |
| 4 d | | | | | T |

7 palindromic substrings (5 singles + "bab" + "aba"). Because i goes downward, (1, 3) "aba" is found before (0, 2) "bab"; the code only replaces the best on a strictly longer one, so it returns **"aba"**.

### Bonus: the longest palindromic subsequence (#516)
Also an interval DP:

```
lps[i][j] = s[i] == s[j] ? lps[i + 1][j − 1] + 2 : max(lps[i + 1][j], lps[i][j − 1])
```

It equals LCS(s, reverse(s)) ([M21](M21_LongestCommonSubsequence.md)), and the file checks that.

---

## 3. Java concepts used in this problem

### 3.1 The interval-DP loop order

```java
for (int i = n - 1; i >= 0; i--)
    for (int j = i; j < n; j++)
```

Each cell depends on cells with a larger i (the row below) and a smaller j. Iterating i downward and j upward guarantees they're ready. The alternative, `for (len = 1..n) for (i = 0; i + len − 1 < n; i++)`, fills by interval length, which works for every interval DP.

### 3.2 Short-circuiting protects the diagonal read

`j - i < 2 || pal[i + 1][j - 1]` only reads `pal[i + 1][j − 1]` when the interval has length ≥ 3, so i + 1 ≤ j − 1 and the indices are valid.

### 3.3 Tracking the best interval

```java
if (pal[i][j] && j - i > bestR - bestL) { bestL = i; bestR = j; }
...
return s.substring(bestL, bestR + 1);
```

Keeping the bounds instead of building strings avoids creating a substring for every palindrome.

### 3.4 `lps[i][i] = 1` set inside the loop

The subsequence version sets the diagonal at the start of each row, then fills j > i.

---

## 4. Approach 1: Check every substring (the referee)

The test counts the palindromic substrings and the longest one by checking every (i, j) with a two-pointer `isPal` ([M24_PalindromicSubstringsDP.java:81](M24_PalindromicSubstringsDP.java#L81)): **O(n³).**

```java
class Check {
    static boolean isPal(String s, int i, int j) { while (i < j) if (s.charAt(i++) != s.charAt(j--)) return false; return true; }
}
```

---

## 5. Approach 2: The palindrome table (the file's main solutions)

### Code ([M24_PalindromicSubstringsDP.java:40-60](M24_PalindromicSubstringsDP.java#L40-L60))

```java
static String longestPalindrome(String s) {
    int n = s.length(), bestL = 0, bestR = 0;
    boolean[][] pal = new boolean[n][n];
    for (int i = n - 1; i >= 0; i--)
        for (int j = i; j < n; j++) {
            pal[i][j] = s.charAt(i) == s.charAt(j) && (j - i < 2 || pal[i + 1][j - 1]);
            if (pal[i][j] && j - i > bestR - bestL) { bestL = i; bestR = j; }
        }
    return s.substring(bestL, bestR + 1);
}

static int countSubstrings(String s) {
    int n = s.length(), count = 0;
    boolean[][] pal = new boolean[n][n];
    for (int i = n - 1; i >= 0; i--)
        for (int j = i; j < n; j++) {
            pal[i][j] = s.charAt(i) == s.charAt(j) && (j - i < 2 || pal[i + 1][j - 1]);
            if (pal[i][j]) count++;
        }
    return count;
}
```

The same table, with different bookkeeping: the widest true cell, or the number of true cells.

- **Time O(n²), space O(n²).**

---

## 6. Approach 3: Longest palindromic subsequence

### Code ([M24_PalindromicSubstringsDP.java:62-71](M24_PalindromicSubstringsDP.java#L62-L71))

```java
static int longestPalindromeSubseq(String s) {
    int n = s.length();
    int[][] lps = new int[n][n];
    for (int i = n - 1; i >= 0; i--) {
        lps[i][i] = 1;
        for (int j = i + 1; j < n; j++)
            lps[i][j] = s.charAt(i) == s.charAt(j) ? lps[i + 1][j - 1] + 2 : Math.max(lps[i + 1][j], lps[i][j - 1]);
    }
    return n == 0 ? 0 : lps[0][n - 1];
}
```

| Case | Meaning |
|---|---|
| matching ends | both ends join the subsequence: +2 around the inside |
| otherwise | drop one end, whichever is better |

For two adjacent equal characters, `lps[i + 1][j − 1]` is an "empty" interval (i + 1 > j − 1), whose cell holds the default 0, so the result is 2.

- **Time O(n²), space O(n²).**

---

## 7. Comparing the approaches

| | Check every substring | Palindrome table | Expand around centre (Strings M10/M11) |
|---|---|---|---|
| Time | O(n³) | O(n²) | O(n²) |
| Space | O(1) | O(n²) | **O(1)** |
| Reusable table | no | **yes** | no |

The file checks the count, the longest substring and the subsequence (against LCS with the reverse) on 500 random strings over {a, b, c}.

---

## 8. Edge cases and common mistakes

**Edge cases:** a single character, all characters equal (n(n + 1)/2 palindromes), no repeated characters (n palindromes of length 1).

**Mistakes:**
1. Filling i upward (pal[i + 1] isn't ready).
2. Forgetting the length-2 case (`j − i < 2`).
3. Confusing substring (contiguous, #5/#647) with subsequence (#516).

---

## 9. Interview questions and answers

**Q1. Why does the loop order matter?**
> Each cell depends on a strictly shorter interval nested inside it. Either fill by increasing length, or with i decreasing and j increasing, so the inner interval is always computed first.

**Q2. Which approach would you use in an interview?**
> Expand around centre: the same O(n²) time with O(1) space. But mention the table, since harder problems (like palindrome partitioning) need it.

### Follow-up questions

**F1. Palindrome Partitioning II (LC 132): the minimum cuts.**
> Build this table, then cuts[j] = min over i with pal[i][j] of cuts[i − 1] + 1.

**F2. Manacher's algorithm.**
> The longest palindromic substring in O(n), by reusing mirror information around the current rightmost palindrome.

**F3. Burst Balloons.** → [H01](../04-Hard/H01_BurstBalloons.md)
> A harder interval DP, choosing the **last** element in each interval.

---

## 10. Running the file

```bash
java M24_PalindromicSubstringsDP.java      # 6/6 tests passed (including 500 random vs checking every substring)
```

**One-line summary:** *"pal[i][j] = (s[i] == s[j]) and (j − i < 2 or pal[i + 1][j − 1]), filled with i decreasing and j increasing; count the true cells for #647 and take the widest for #5."*
