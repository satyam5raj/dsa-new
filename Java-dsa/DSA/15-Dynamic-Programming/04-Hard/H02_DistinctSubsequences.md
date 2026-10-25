# H02 · Distinct Subsequences

| | |
|---|---|
| **LeetCode** | [#115 Distinct Subsequences](https://leetcode.com/problems/distinct-subsequences/) |
| **Difficulty** | Hard |
| **Pattern** | Two-sequence **counting** DP (the LCS table, with sums instead of max) |
| **Code** | [H02_DistinctSubsequences.java](H02_DistinctSubsequences.java) |
| **Run** | `java H02_DistinctSubsequences.java` |

The two-string table is explained in [M21 LCS](../03-Medium/M21_LongestCommonSubsequence.md); the "downward" one-row trick is from [M11](../03-Medium/M11_PartitionEqualSubsetSum.md).

---

## 1. The problem

Count the distinct ways (sets of positions in s) to pick t as a subsequence of s.

| s | t | answer | why |
|---|---|---|---|
| "rabbbit" | "rabbit" | 3 | drop any one of the three b's |
| "babgbag" | "bag" | 5 | |
| "abc" | "" | 1 | pick nothing |
| "a" | "aa" | 0 | |

---

## 2. How to think about it

### Brute force
Choose |t| positions of s: C(n, m) choices, which is exponential.

### Look at s's last character

| step | for this problem |
|---|---|
| **State** | dp[i][j] = the number of ways to form t[0..j) from s[0..i) |
| **Transition** | dp[i][j] = dp[i − 1][j] (don't use s[i − 1]) + (s[i − 1] == t[j − 1] ? dp[i − 1][j − 1] : 0) (use it as t's last character) |
| **Base** | dp[i][0] = 1 (an empty t: one way, pick nothing); dp[0][j > 0] = 0 |
| **Answer** | dp[n][m] |

### One row, iterating j downward
With one array, iterate j **downward** so that dp[j − 1] still holds row i − 1 when dp[j] reads it. It's the same reason as in 0/1 knapsack: each character of s is used at most once per way.

### Overflow
LeetCode promises the final answer fits in 32 bits, but intermediate cells may not. The file uses `long`.

### Dry run: "babgbag" / "bag" (dp over j = "", b, a, g)

| s char | dp |
|---|---|
| (start) | [1, 0, 0, 0] |
| b | [1, 1, 0, 0] |
| a | [1, 1, 1, 0] |
| b | [1, 2, 1, 0] |
| g | [1, 2, 1, 1] |
| b | [1, 3, 1, 1] |
| a | [1, 3, 4, 1] |
| g | [1, 3, 4, **5**] |

### Dry run: "rabbbit" / "rabbit" (from a real run; dp over j = "", r, a, b, b, i, t)

| s char | dp |
|---|---|
| r | [1, 1, 0, 0, 0, 0, 0] |
| a | [1, 1, 1, 0, 0, 0, 0] |
| b | [1, 1, 1, 1, 0, 0, 0] |
| b | [1, 1, 1, 2, 1, 0, 0] |
| b | [1, 1, 1, 3, 3, 0, 0] |
| i | [1, 1, 1, 3, 3, 3, 0] |
| t | [1, 1, 1, 3, 3, 3, **3**] |

---

## 3. Java concepts used in this problem

### 3.1 Why the inner loop goes down

```java
for (int j = Math.min(i, m); j >= 1; j--)
    if (s.charAt(i - 1) == t.charAt(j - 1)) dp[j] += dp[j - 1];
```

Going down, dp[j − 1] hasn't been updated for this character yet, so it's still "ways using s[0..i − 1)". Going up would let one character of s match two positions of t. (`Math.min(i, m)` skips the j > i cells, which are always 0: a prefix of length i can't form a longer string.)

### 3.2 `long` and a final cast

`long[] dp` avoids overflow in the middle; `(int) dp[m]` narrows the final answer, which the problem guarantees fits.

### 3.3 `Integer.bitCount(mask) != t.length()` (in the referee)

Only masks that pick exactly |t| positions of s can form t, so the others are skipped quickly.

---

## 4. Approach 1: Every set of positions (the referee)

### Code ([H02_DistinctSubsequences.java:48-57](H02_DistinctSubsequences.java#L48-L57))

```java
static int brute(String s, String t) {                         // every subset of positions
    int count = 0;
    for (int mask = 0; mask < 1 << s.length(); mask++) {
        if (Integer.bitCount(mask) != t.length()) continue;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) if ((mask >> i & 1) == 1) sb.append(s.charAt(i));
        if (sb.toString().equals(t)) count++;
    }
    return count;
}
```

Each mask is a set of positions; count those that spell t. **O(n · 2ⁿ).**

---

## 5. Approach 2: Counting DP in one row (the file's main solution)

### Code ([H02_DistinctSubsequences.java:38-46](H02_DistinctSubsequences.java#L38-L46))

```java
static int numDistinct(String s, String t) {
    int m = t.length();
    long[] dp = new long[m + 1];
    dp[0] = 1;
    for (int i = 1; i <= s.length(); i++)
        for (int j = Math.min(i, m); j >= 1; j--)               // downward: dp[j-1] is still row i-1
            if (s.charAt(i - 1) == t.charAt(j - 1)) dp[j] += dp[j - 1];
    return (int) dp[m];
}
```

| Line | What it does |
|---|---|
| `dp[0] = 1` | the empty t can always be formed, in one way |
| the outer loop | add s's characters one at a time |
| the inner loop | if s[i − 1] matches t[j − 1], it can finish any way of forming t[0..j − 1) |
| (no `else`) | "don't use s[i − 1]" is just keeping dp[j] |

### Complexity
- **Time O(n · m), space O(m).**

---

## 6. Comparing the approaches

| | Every position set | Counting DP |
|---|---|---|
| Time | O(n · 2ⁿ) | **O(n · m)** |
| Space | O(n) | O(m) |

The file checks the DP against the brute force on 500 random strings over {a, b}.

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty t (1), t longer than s (0), s equal to t (1), many repeated characters.

**Mistakes:**
1. Iterating j upward with one row.
2. dp[0] = 0.
3. Using `int` for the table (intermediate overflow).
4. Counting distinct **strings** instead of distinct position sets (the problem counts position sets).

---

## 8. Interview questions and answers

**Q1. Why is the recurrence a sum, not a max?**
> We're counting all the ways, and "use s[i − 1]" and "don't use it" are disjoint sets of ways, so their counts add.

**Q2. How is this related to LCS?**
> Same table, same "look at the last character" reasoning. LCS maximises a length over the two choices; this counts the ways.

### Follow-up questions

**F1. Distinct Subsequences II (LC 940): count the distinct subsequences of s.**
> dp over positions with "subtract the count from the previous occurrence of the same character" to avoid duplicates.

**F2. Number of Matching Subsequences (LC 792).**
> Boolean "is subsequence" for many words: bucket the words by the next character they need.

**F3. Interleaving String.** → [M23](../03-Medium/M23_InterleavingString.md)

---

## 9. Running the file

```bash
java H02_DistinctSubsequences.java      # 5/5 tests passed (including 500 random vs every position subset)
```

**One-line summary:** *"dp[i][j] = dp[i−1][j] + (s[i−1] == t[j−1] ? dp[i−1][j−1] : 0) with dp[·][0] = 1; in one long array, iterate j downward so each character of s is used once."*
