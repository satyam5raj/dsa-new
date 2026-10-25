# M23 · Interleaving String

| | |
|---|---|
| **LeetCode** | [#97 Interleaving String](https://leetcode.com/problems/interleaving-string/) |
| **Difficulty** | Medium |
| **Pattern** | Two-sequence **boolean** DP (the third string's index is implied: i + j) |
| **Code** | [M23_InterleavingString.java](M23_InterleavingString.java) |
| **Run** | `java M23_InterleavingString.java` |

The two-string table layout is explained in [M21 LCS](M21_LongestCommonSubsequence.md).

---

## 1. The problem

Is s3 formed by interleaving s1 and s2, keeping the characters of each in their original order?

| s1 | s2 | s3 | answer |
|---|---|---|---|
| "aabcc" | "dbbca" | "aadbbcbcac" | true |
| "aabcc" | "dbbca" | "aadbbbaccc" | false |
| "" | "" | "" | true |
| "a" | "" | "aa" | false (lengths don't add up) |

---

## 2. How to think about it

### Greedy and two pointers fail
When s1[i] == s2[j] == s3[k], you don't know which string to take the character from. Branching on both is exponential, but the state turns out to be only (i, j).

### The third index is implied
After using i characters of s1 and j characters of s2, we've built exactly s3[0..i + j). So (i, j) is the whole state; k = i + j is not a third dimension.

| step | for this problem |
|---|---|
| **State** | dp[i][j] = s3[0..i+j) is an interleaving of s1[0..i) and s2[0..j) |
| **Transition** | (dp[i−1][j] and s1[i−1] == s3[i+j−1])  →  the last character came from s1 |
| | or (dp[i][j−1] and s2[j−1] == s3[i+j−1])  →  it came from s2 |
| **Base** | dp[0][0] = true; reject at once if \|s1\| + \|s2\| ≠ \|s3\| |
| **Answer** | dp[n][m] |

One row of booleans is enough.

### Dry run: "aabcc" / "dbbca" / "aadbbcbcac" (from a real run)
Rows are i (characters of s1 used), columns are j (characters of s2 used). T = true.

| i \ j | 0 | 1 d | 2 b | 3 b | 4 c | 5 a |
|---|---|---|---|---|---|---|
| 0 | T | . | . | . | . | . |
| 1 a | T | . | . | . | . | . |
| 2 a | T | T | T | T | T | . |
| 3 b | . | T | T | . | T | . |
| 4 c | . | . | T | T | T | T |
| 5 c | . | . | . | T | . | **T** |

For example dp[2][1] is true: s3[0..3) = "aad" is "aa" from s1 plus "d" from s2 (dp[2][0] is true and s2[0] = 'd' = s3[2]).

---

## 3. Java concepts used in this problem

### 3.1 One boolean row, read before and after its update

```java
boolean fromS1 = i > 0 && dp[j] && s1.charAt(i - 1) == c;          // dp[j] still = dp[i-1][j]
boolean fromS2 = j > 0 && dp[j - 1] && s2.charAt(j - 1) == c;      // dp[j-1] already = dp[i][j-1]
dp[j] = fromS1 || fromS2;
```

Before the assignment, dp[j] is still the previous row's value (above), while dp[j − 1] has already been updated (left). Those are exactly the two predecessors.

### 3.2 Guards with `&&` short-circuiting

`i > 0 && …` and `j > 0 && …` stop the expression before reading `s1.charAt(i − 1)` or `dp[j − 1]` out of range.

### 3.3 Building guaranteed interleavings (in the test)

`shuffleMerge` randomly takes the next character from a or b until both are used up, so half of the tests are real interleavings and half are random strings (mostly false).

---

## 4. Approach 1: Recursion over both choices (the referee)

### Code ([M23_InterleavingString.java:49-54](M23_InterleavingString.java#L49-L54))

```java
static boolean brute(String a, String b, String c, int i, int j) {
    if (i + j == c.length()) return i == a.length() && j == b.length();
    char x = c.charAt(i + j);
    return (i < a.length() && a.charAt(i) == x && brute(a, b, c, i + 1, j))
        || (j < b.length() && b.charAt(j) == x && brute(a, b, c, i, j + 1));
}
```

At each step, take the next character of s3 from a or from b if it matches. **Exponential** without memo; memoizing (i, j) gives the DP.

---

## 5. Approach 2: Boolean DP with one row (the file's main solution)

### Code ([M23_InterleavingString.java:34-47](M23_InterleavingString.java#L34-L47))

```java
static boolean isInterleave(String s1, String s2, String s3) {
    int n = s1.length(), m = s2.length();
    if (n + m != s3.length()) return false;
    boolean[] dp = new boolean[m + 1];
    for (int i = 0; i <= n; i++)
        for (int j = 0; j <= m; j++) {
            if (i == 0 && j == 0) { dp[0] = true; continue; }
            char c = s3.charAt(i + j - 1);
            boolean fromS1 = i > 0 && dp[j] && s1.charAt(i - 1) == c;          // dp[j] still = dp[i-1][j]
            boolean fromS2 = j > 0 && dp[j - 1] && s2.charAt(j - 1) == c;      // dp[j-1] already = dp[i][j-1]
            dp[j] = fromS1 || fromS2;
        }
    return dp[m];
}
```

| Line | What it does |
|---|---|
| the length check | an interleaving uses every character exactly once |
| `dp[0] = true` | the empty interleaving |
| `c` | the character of s3 that (i, j) must account for last |
| `fromS1`, `fromS2` | it came from s1 or from s2 |

### Complexity
- **Time O(n · m), space O(m).**

---

## 6. Comparing the approaches

| | Recursion | Boolean DP |
|---|---|---|
| Time | exponential | **O(n · m)** |
| Space | O(n + m) stack | O(m) |

The file checks the DP against the recursion on 2,000 random cases (about half of them real interleavings).

---

## 7. Edge cases and common mistakes

**Edge cases:** all three empty (true), one input empty (s3 must equal the other), length mismatch (false), lots of equal characters.

**Mistakes:**
1. Greedy two pointers.
2. Adding k as a third dimension (correct but O(n · m · (n + m))).
3. Forgetting the length check (dp could report true for a prefix).

---

## 8. Interview questions and answers

**Q1. Why is (i, j) enough state?**
> Using i characters of s1 and j of s2 always produces exactly i + j characters of s3, so the position in s3 is determined.

**Q2. Can you reduce memory further?**
> Use the shorter string for the row: O(min(n, m)).

### Follow-up questions

**F1. Return one interleaving split.**
> Keep the full table and walk back from (n, m), choosing whichever predecessor was true.

**F2. Edit Distance.** → [M22](M22_EditDistance.md)
> Another (i, j) two-string table.

**F3. Distinct Subsequences.** → [H02](../04-Hard/H02_DistinctSubsequences.md)
> A counting two-string DP.

---

## 9. Running the file

```bash
java M23_InterleavingString.java      # 5/5 tests passed (including 2000 random vs recursion)
```

**One-line summary:** *"dp[i][j] is true if s3's character at i + j − 1 can come from s1 (with dp[i−1][j]) or from s2 (with dp[i][j−1]); check the lengths first and keep one boolean row."*
