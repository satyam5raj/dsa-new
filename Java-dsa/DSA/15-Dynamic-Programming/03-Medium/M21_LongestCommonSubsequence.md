# M21 · Longest Common Subsequence

| | |
|---|---|
| **LeetCode** | [#1143 Longest Common Subsequence](https://leetcode.com/problems/longest-common-subsequence/) |
| **Difficulty** | Medium |
| **Pattern** | **Two-sequence DP**: the template for Edit Distance, diff, regex matching, … |
| **Code** | [M21_LongestCommonSubsequence.java](M21_LongestCommonSubsequence.java) |
| **Run** | `java M21_LongestCommonSubsequence.java` |

The DP recipe is explained in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe). This is the first two-string DP of the topic.

---

## 1. The problem

Return the length of the longest subsequence common to both strings.

| a | b | answer | LCS |
|---|---|---|---|
| "abcde" | "ace" | 3 | "ace" |
| "abc" | "abc" | 3 | "abc" |
| "abc" | "def" | 0 | |

---

## 2. How to think about it

### Brute force
Every subsequence of a (2ⁿ of them), each tested against b.

### Look at the last characters of the two prefixes

| step | for this problem |
|---|---|
| **State** | dp[i][j] = the LCS length of a[0..i) and b[0..j) |
| **Transition** | if a[i − 1] == b[j − 1]: dp[i − 1][j − 1] + 1 (use the matching pair) |
| | otherwise: max(dp[i − 1][j], dp[i][j − 1]) (drop one of the two last characters) |
| **Base** | dp[0][·] = dp[·][0] = 0 (an empty prefix) |
| **Answer** | dp[n][m] |

**Why is matching always safe?** If the last characters are equal, some optimal LCS uses them as its final pair: any LCS that doesn't can be rewired to end with them without getting shorter.

The (n + 1) × (m + 1) table with an empty-prefix row and column is **the** standard layout: it removes every "i − 1 < 0" special case.

### Dry run: "abcde" / "ace"

| | "" | a | c | e |
|---|---|---|---|---|
| "" | 0 | 0 | 0 | 0 |
| a | 0 | **1** | 1 | 1 |
| b | 0 | 1 | 1 | 1 |
| c | 0 | 1 | **2** | 2 |
| d | 0 | 1 | 2 | 2 |
| e | 0 | 1 | 2 | **3** |

The bold cells are the diagonal "+1" steps where the characters match.

### Reconstruction
Walk back from (n, m): on a match, take the character and move diagonally; otherwise move toward the larger neighbour (up or left). Reverse at the end. For the table above: (5,3) e matches → (4,2): d ≠ c, up is 2 → (3,2) c matches → (2,1): b ≠ a, up → (1,1) a matches → "ace".

### Saving space
dp[i][·] only needs row i − 1 and the diagonal, so one row plus a saved "diagonal" variable is enough for the length.

---

## 3. Java concepts used in this problem

### 3.1 One row plus a diagonal variable

```java
int diag = 0;                                  // dp[i-1][j-1]
for (int j = 1; j <= b.length(); j++) {
    int above = dp[j];                         // dp[i-1][j], about to be overwritten
    dp[j] = a.charAt(i - 1) == b.charAt(j - 1) ? diag + 1 : Math.max(above, dp[j - 1]);
    diag = above;
}
```

| variable | holds |
|---|---|
| `dp[j]` before the update | dp[i − 1][j] (above) |
| `dp[j − 1]` | dp[i][j − 1] (left, already updated) |
| `diag` | dp[i − 1][j − 1] (the old dp[j − 1], saved before it was overwritten) |

### 3.2 Swapping so the row is the shorter string

```java
if (a.length() < b.length()) { String t = a; a = b; b = t; }
```

The answer is symmetric, so making `b` the shorter string keeps the row at O(min(n, m)).

### 3.3 `StringBuilder.reverse()`

The reconstruction collects characters from the end backwards, then reverses once.

### 3.4 Checking a subsequence with two pointers (in the test)

```java
static boolean isSubseq(String s, String t) {
    int i = 0;
    for (int j = 0; j < t.length() && i < s.length(); j++) if (s.charAt(i) == t.charAt(j)) i++;
    return i == s.length();
}
```

Each character of t either matches the next unmatched character of s or is skipped.

---

## 4. Approach 1: Every subsequence of a (the referee)

### Code ([M21_LongestCommonSubsequence.java:75-89](M21_LongestCommonSubsequence.java#L75-L89))

```java
static int brute(String a, String b) {
    int best = 0;
    for (int mask = 0; mask < 1 << a.length(); mask++) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < a.length(); i++) if ((mask >> i & 1) == 1) s.append(a.charAt(i));
        if (isSubseq(s.toString(), b)) best = Math.max(best, s.length());
    }
    return best;
}
```

- **Time O(2ⁿ · (n + m)).** The test keeps strings ≤ 10 characters.

---

## 5. Approach 2: Full table + reconstruction

### Code ([M21_LongestCommonSubsequence.java:60-73](M21_LongestCommonSubsequence.java#L60-L73))

```java
static String oneLCS(String a, String b) {
    int n = a.length(), m = b.length();
    int[][] dp = new int[n + 1][m + 1];
    for (int i = 1; i <= n; i++)
        for (int j = 1; j <= m; j++)
            dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1) ? dp[i - 1][j - 1] + 1 : Math.max(dp[i - 1][j], dp[i][j - 1]);
    StringBuilder sb = new StringBuilder();
    for (int i = n, j = m; i > 0 && j > 0; ) {
        if (a.charAt(i - 1) == b.charAt(j - 1)) { sb.append(a.charAt(i - 1)); i--; j--; }
        else if (dp[i - 1][j] >= dp[i][j - 1]) i--;
        else j--;
    }
    return sb.reverse().toString();
}
```

| Part | What it does |
|---|---|
| the table | the recurrence, row by row |
| the walk | diagonal on a match, otherwise toward the larger neighbour |

- **Time O(n · m), space O(n · m).**

---

## 6. Approach 3: One row + diagonal (the file's main solution)

### Code ([M21_LongestCommonSubsequence.java:46-58](M21_LongestCommonSubsequence.java#L46-L58))

```java
static int longestCommonSubsequence(String a, String b) {
    if (a.length() < b.length()) { String t = a; a = b; b = t; }   // row length = the shorter string
    int[] dp = new int[b.length() + 1];
    for (int i = 1; i <= a.length(); i++) {
        int diag = 0;                                  // dp[i-1][j-1]
        for (int j = 1; j <= b.length(); j++) {
            int above = dp[j];                         // dp[i-1][j], about to be overwritten
            dp[j] = a.charAt(i - 1) == b.charAt(j - 1) ? diag + 1 : Math.max(above, dp[j - 1]);
            diag = above;
        }
    }
    return dp[b.length()];
}
```

`diag` starts at 0 for each row because dp[i − 1][0] is always 0.

- **Time O(n · m), space O(min(n, m)).**

---

## 7. Comparing the approaches

| | Brute force | Full table | One row |
|---|---|---|---|
| Time | O(2ⁿ · (n + m)) | O(n · m) | O(n · m) |
| Space | O(n) | O(n · m) | **O(min(n, m))** |
| Gives the LCS string | yes | **yes** | no |

The file checks, on 500 random string pairs over {a, b, c, d}, that the one-row length equals the brute force, and that the reconstructed string has that length and is a subsequence of both.

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty string (0), identical strings, no common characters, one string inside the other.

**Mistakes:**
1. Confusing subsequence with substring (contiguous): that's a different DP that resets to 0 on a mismatch.
2. Forgetting to save the diagonal before overwriting in the one-row version.
3. Indexing the strings with i and j instead of i − 1 and j − 1 in the (n + 1) × (m + 1) layout.

---

## 9. Interview questions and answers

**Q1. Why drop one character on a mismatch?**
> If the last characters differ, they can't both end the LCS, so the answer is the better of ignoring a's last character or ignoring b's.

**Q2. How does diff work?**
> It computes the LCS of the two files' lines; lines outside the LCS are reported as deleted or added. (Real tools use the faster Myers algorithm, which is based on the same idea.)

### Follow-up questions

**F1. Edit Distance.** → [M22](M22_EditDistance.md)
> The same table with insert, delete and replace costs.

**F2. Shortest Common Supersequence (LC 1092).**
> n + m − LCS, built by walking the LCS table.

**F3. Longest Palindromic Subsequence (LC 516).**
> The LCS of s and reverse(s).

---

## 10. Running the file

```bash
java M21_LongestCommonSubsequence.java      # 5/5 tests passed (including 500 random vs 2^n, + the path is common)
```

**One-line summary:** *"dp[i][j] = dp[i−1][j−1] + 1 when the last characters match, otherwise max(dp[i−1][j], dp[i][j−1]); one row plus a saved diagonal gives the length, and the full table lets you walk the LCS back."*
