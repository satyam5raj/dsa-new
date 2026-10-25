# H04 · Regular Expression Matching

| | |
|---|---|
| **LeetCode** | [#10 Regular Expression Matching](https://leetcode.com/problems/regular-expression-matching/) |
| **Difficulty** | Hard |
| **Pattern** | Two-sequence **boolean** DP where `*` means "zero or more of the previous character" |
| **Code** | [H04_RegularExpressionMatching.java](H04_RegularExpressionMatching.java) |
| **Run** | `java H04_RegularExpressionMatching.java` |

The two-string table is explained in [M21 LCS](../03-Medium/M21_LongestCommonSubsequence.md).

---

## 1. The problem

The pattern has two special characters: `.` matches any single character, and `x*` matches zero or more x's. The match must cover the **whole** string.

| s | p | answer |
|---|---|---|
| "aa" | "a" | false |
| "aa" | "a*" | true |
| "ab" | ".*" | true |
| "aab" | "c*a*b" | true (zero c's, two a's, one b) |
| "mississippi" | "mis\*is\*p\*." | false |
| "" | "a\*b\*" | true |

---

## 2. How to think about it

### Treat "x*" as one unit, and look at the end of the pattern

| case | dp[i][j] (s[0..i) matches p[0..j)) |
|---|---|
| p[j − 1] is a normal character or `.` | i > 0 and s[i − 1] matches p[j − 1] and dp[i − 1][j − 1] |
| p[j − 1] is `*` (the unit is p[j − 2] followed by `*`) | **zero copies:** dp[i][j − 2] (drop "x*") |
| | **or one more copy:** i > 0 and s[i − 1] matches p[j − 2] and dp[i − 1][j] (eat one character of s, **keep** "x*" for more) |
| **Base** | dp[0][0] = true; dp[0][j] = (p[j − 1] == `*` and dp[0][j − 2]), so "a\*b\*" matches "" |

The `dp[i − 1][j]` term, staying on the same pattern column while consuming a character, is what makes `*` repeat.

### Dry run: "aab" / "c\*a\*b" (from a real run)
Rows are prefixes of s, columns are prefixes of p.

| | "" | c | \* | a | \* | b |
|---|---|---|---|---|---|---|
| "" | T | . | T | . | T | . |
| a | . | . | . | T | T | . |
| aa | . | . | . | . | T | . |
| aab | . | . | . | . | . | **T** |

- Row "": "c\*" and "c\*a\*" match the empty string (zero copies).
- "a" matches "c\*a" and "c\*a\*" (one a).
- "aa" matches "c\*a\*": dp[2][4] comes from "one more copy" of a, using dp[1][4].
- "aab" matches "c\*a\*b": b == b and dp[2][4].

---

## 3. Java concepts used in this problem

### 3.1 A helper for "does this character match?"

```java
static boolean matches(char sc, char pc) { return pc == '.' || pc == sc; }
```

Keeping the `.` rule in one place makes both cases of the transition read cleanly.

### 3.2 Reading p[j − 2] safely

In valid patterns, `*` always follows a character, so when p[j − 1] is `*`, j ≥ 2 and p.charAt(j − 2) exists. The base-case loop starts at j = 2 for the same reason.

### 3.3 `java.util.regex.Pattern.matches` as the referee

```java
Pattern.matches(p.toString(), s.toString())
```

Java's own regex engine supports `.` and `*` with the same meaning (and requires a full match with `matches`), so it's a perfect independent check.

---

## 4. Approach 1: Java's regex engine (the referee)

The test generates 3,000 random strings and patterns (with `*` always following a character) and compares `isMatch` with `Pattern.matches` ([H04_RegularExpressionMatching.java:68-80](H04_RegularExpressionMatching.java#L68-L80)).

Backtracking regex engines like Java's can take exponential time on some patterns; the DP never does.

---

## 5. Approach 2: Boolean DP (the file's main solution)

### Code ([H04_RegularExpressionMatching.java:41-59](H04_RegularExpressionMatching.java#L41-L59))

```java
static boolean isMatch(String s, String p) {
    int n = s.length(), m = p.length();
    boolean[][] dp = new boolean[n + 1][m + 1];
    dp[0][0] = true;
    for (int j = 2; j <= m; j++) dp[0][j] = p.charAt(j - 1) == '*' && dp[0][j - 2];
    for (int i = 1; i <= n; i++)
        for (int j = 1; j <= m; j++) {
            char pc = p.charAt(j - 1);
            if (pc == '*') {
                dp[i][j] = dp[i][j - 2]                                           // zero copies of p[j-2]
                        || (matches(s.charAt(i - 1), p.charAt(j - 2)) && dp[i - 1][j]);   // one more copy
            } else {
                dp[i][j] = matches(s.charAt(i - 1), pc) && dp[i - 1][j - 1];
            }
        }
    return dp[n][m];
}

static boolean matches(char sc, char pc) { return pc == '.' || pc == sc; }
```

### Line by line

| Line | What it does |
|---|---|
| `dp[0][0]` | empty matches empty |
| the base loop | an empty string matches patterns made only of "x\*" units |
| the `*` case | drop the unit, or use it for one more character and stay |
| the normal case | match one character and step back diagonally |

### Complexity
- **Time O(n · m), space O(n · m)** (O(m) with two rows).

---

## 6. Comparing the approaches

| | Backtracking regex engine | DP |
|---|---|---|
| Worst-case time | exponential | **O(n · m)** |
| Supports full regex | yes | only `.` and `*` |

---

## 7. Edge cases and common mistakes

**Edge cases:** an empty string with a pattern like "a\*b\*c\*" (true), ".\*" (matches anything), a pattern longer than the string.

**Mistakes:**
1. Treating `*` on its own instead of together with the previous character.
2. Using dp[i − 1][j − 2] for "one more copy" (that only allows exactly one copy; it must be dp[i − 1][j]).
3. Forgetting the base row (patterns like "a\*" matching "").
4. Comparing with p[j − 1] instead of p[j − 2] inside the `*` case.

---

## 8. Interview questions and answers

**Q1. Why does dp[i − 1][j] make `*` repeat?**
> It says: if s[0..i − 1) already matches the pattern **including** "x\*", and s[i − 1] is another x, then s[0..i) matches too. Applying that repeatedly consumes any number of x's.

**Q2. How is this different from Wildcard Matching?**
> In wildcard, `*` is standalone and matches any sequence. Here `*` modifies the previous character, so it's "zero or more of that character" (or of anything, for `.*`).

### Follow-up questions

**F1. Wildcard Matching.** → [H05](H05_WildcardMatching.md)

**F2. Add `+` (one or more).**
> "x+" is the same as "xx\*".

**F3. Top-down memo version.**
> match(i, j) on suffixes: if p[j + 1] is `*`, try match(i, j + 2) or (first characters match and match(i + 1, j)); otherwise match one character.

---

## 9. Running the file

```bash
java H04_RegularExpressionMatching.java      # 7/7 tests passed (including 3000 random vs java.util.regex)
```

**One-line summary:** *"dp[i][j]: for a normal or '.' character, match one character diagonally; for 'x*', either drop it (dp[i][j−2]) or use it for one more character and stay on the same column (dp[i−1][j])."*
