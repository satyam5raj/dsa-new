# H05 · Wildcard Matching

| | |
|---|---|
| **LeetCode** | [#44 Wildcard Matching](https://leetcode.com/problems/wildcard-matching/) |
| **Difficulty** | Hard |
| **Pattern** | Two-sequence boolean DP (`*` = any sequence) + a greedy backtracking version in O(1) space |
| **Code** | [H05_WildcardMatching.java](H05_WildcardMatching.java) |
| **Run** | `java H05_WildcardMatching.java` |

The regex version (where `*` repeats the previous character) is [H04](H04_RegularExpressionMatching.md).

---

## 1. The problem

`?` matches exactly one character, and `*` matches **any** sequence (including the empty one). The match must cover the whole string.

| s | p | answer |
|---|---|---|
| "aa" | "a" | false |
| "aa" | "\*" | true |
| "cb" | "?a" | false |
| "adceb" | "\*a\*b" | true |
| "acdcb" | "a\*c?b" | false |

---

## 2. How to think about it

### The DP
Unlike [H04](H04_RegularExpressionMatching.md), here `*` stands alone.

| case | dp[i][j] (s[0..i) matches p[0..j)) |
|---|---|
| p[j − 1] == `*` | dp[i][j − 1] (the `*` matches nothing) **or** dp[i − 1][j] (the `*` eats s[i − 1] and stays) |
| otherwise | (p[j − 1] == `?` or p[j − 1] == s[i − 1]) and dp[i − 1][j − 1] |
| **Base** | dp[0][0] = true; dp[0][j] = dp[0][j − 1] and p[j − 1] == `*` |

### Dry run: "adceb" / "\*a\*b" (from a real run)

| | "" | \* | a | \* | b |
|---|---|---|---|---|---|
| "" | T | T | . | . | . |
| a | . | T | T | T | . |
| ad | . | T | . | T | . |
| adc | . | T | . | T | . |
| adce | . | T | . | T | . |
| adceb | . | T | . | T | **T** |

The first `*` column is all true (a leading `*` matches any prefix); the second `*` keeps "ad…" alive after the "a" matched; finally "b" matches with dp[4][3].

### The greedy alternative (O(1) space)
Walk both strings. On a `*`, remember (star position, s position) and let it match nothing for now. On a mismatch, **backtrack**: let the **last** star eat one more character and retry.

Only the last star matters: any extra characters an earlier star might eat could equally be eaten by the later one.

### Dry run: greedy on "adceb" / "\*a\*b" (from a real run)

| step | action | i | j | star | mark |
|---|---|---|---|---|---|
| 1 | `*` at j = 0: remember it | 0 | 1 | 0 | 0 |
| 2 | 'a' == 'a' | 1 | 2 | 0 | 0 |
| 3 | `*` at j = 2: remember it | 1 | 3 | 2 | 1 |
| 4 | 'd' ≠ 'b': the last star eats 'd' | 2 | 3 | 2 | 2 |
| 5 | 'c' ≠ 'b': eat 'c' | 3 | 3 | 2 | 3 |
| 6 | 'e' ≠ 'b': eat 'e' | 4 | 3 | 2 | 4 |
| 7 | 'b' == 'b' | 5 | 4 | 2 | 4 |

s is used up and so is p: **true**.

---

## 3. Java concepts used in this problem

### 3.1 One boolean row with a saved diagonal

```java
boolean diag = dp[0];                         // dp[i-1][0]
dp[0] = false;                                // non-empty s vs empty pattern
for (int j = 1; j <= m; j++) {
    boolean up = dp[j];                       // dp[i-1][j]
    ...
    diag = up;
}
```

The same trick as [M22 Edit Distance](../03-Medium/M22_EditDistance.md#31-one-row--diagonal-with-a-changing-first-column): save "above" before overwriting it, and pass it on as the next cell's diagonal.

### 3.2 `star = j++` and `i = ++mark`

- `star = j++` stores the current j, **then** increments it (post-increment).
- `i = ++mark` increments mark **first**, then stores it (pre-increment): the star now covers one more character, and i resumes right after it.

### 3.3 Translating to a Java regex (in the test)

```java
p.replace("?", ".").replace("*", ".*")
```

`?` becomes `.` (any one character) and `*` becomes `.*` (any sequence), so `Pattern.matches` can act as the referee. (The random patterns only use letters, `?` and `*`, so there are no other special characters to escape.)

---

## 4. Approach 1: Java regex (the referee)

The test compares both solutions with `Pattern.matches` on the translated pattern, for 3,000 random cases ([H05_WildcardMatching.java:73-83](H05_WildcardMatching.java#L73-L83)).

---

## 5. Approach 2: One-row DP (the file's main solution)

### Code ([H05_WildcardMatching.java:36-53](H05_WildcardMatching.java#L36-L53))

```java
static boolean isMatch(String s, String p) {
    int m = p.length();
    boolean[] dp = new boolean[m + 1];                // row i-1, overwritten into row i
    dp[0] = true;
    for (int j = 1; j <= m; j++) dp[j] = dp[j - 1] && p.charAt(j - 1) == '*';
    for (int i = 1; i <= s.length(); i++) {
        boolean diag = dp[0];                         // dp[i-1][0]
        dp[0] = false;                                // non-empty s vs empty pattern
        for (int j = 1; j <= m; j++) {
            boolean up = dp[j];                       // dp[i-1][j]
            char pc = p.charAt(j - 1);
            if (pc == '*') dp[j] = dp[j - 1] || up;   // empty match || eat one char
            else dp[j] = (pc == '?' || pc == s.charAt(i - 1)) && diag;
            diag = up;
        }
    }
    return dp[m];
}
```

| Line | What it does |
|---|---|
| the base row | only leading `*`s match the empty string |
| `dp[0] = false` | a non-empty prefix of s can't match an empty pattern |
| the `*` case | left (match nothing) or up (eat one more) |
| the other case | `?` or an equal character, diagonally |

- **Time O(n · m), space O(m).**

---

## 6. Approach 3: Greedy with backtracking to the last star

### Code ([H05_WildcardMatching.java:55-65](H05_WildcardMatching.java#L55-L65))

```java
static boolean isMatchGreedy(String s, String p) {
    int i = 0, j = 0, star = -1, mark = 0;
    while (i < s.length()) {
        if (j < p.length() && (p.charAt(j) == '?' || p.charAt(j) == s.charAt(i))) { i++; j++; }
        else if (j < p.length() && p.charAt(j) == '*') { star = j++; mark = i; }      // star matches "" for now
        else if (star != -1) { j = star + 1; i = ++mark; }                             // last star eats one more
        else return false;
    }
    while (j < p.length() && p.charAt(j) == '*') j++;
    return j == p.length();
}
```

| Branch | Meaning |
|---|---|
| a match | advance both |
| a `*` | remember where it is and where s was |
| a mismatch after a star | the last star eats one more character; retry right after it |
| a mismatch with no star | impossible |
| the final loop | trailing `*`s can match nothing |

- **Time O(n · m)** in the worst case (usually much faster), **space O(1).**

---

## 7. Comparing the approaches

| | DP | Greedy |
|---|---|---|
| Time | O(n · m) always | O(n · m) worst, usually near linear |
| Space | O(m) | **O(1)** |
| Easy to prove | **yes** | needs the "last star" argument |

The file checks that the DP, the greedy and Java's regex agree on 3,000 random cases.

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty string with "\*\*\*" (true), an empty pattern (only matches ""), consecutive stars, a pattern of only `?`s.

**Mistakes:**
1. Treating `*` like regex's "previous character repeated".
2. Forgetting trailing stars at the end of the greedy.
3. In the greedy, backtracking to the first star instead of the last.

---

## 9. Interview questions and answers

**Q1. Why is backtracking only to the last star enough?**
> Any text an earlier star could absorb can be absorbed by the later star instead, since everything between them is fixed. So once a later star exists, earlier stars never need to change.

**Q2. DP or greedy in an interview?**
> The DP is easier to justify; the greedy is a strong follow-up for O(1) space.

### Follow-up questions

**F1. Regular Expression Matching.** → [H04](H04_RegularExpressionMatching.md)

**F2. Collapse consecutive stars first.**
> "a\*\*\*b" is the same as "a\*b", which shortens the pattern.

**F3. Glob matching with `[abc]` character classes.**
> The same DP; the single-character case checks set membership.

---

## 10. Running the file

```bash
java H05_WildcardMatching.java      # 6/6 tests passed (including 3000 random: DP == greedy == regex)
```

**One-line summary:** *"dp[i][j] for '*' is dp[i][j−1] (match nothing) or dp[i−1][j] (eat one more character); otherwise match '?' or the same character diagonally. The greedy version backtracks to the last star."*
