# M22 · Edit Distance

| | |
|---|---|
| **LeetCode** | [#72 Edit Distance](https://leetcode.com/problems/edit-distance/) |
| **Difficulty** | Medium |
| **Pattern** | Two-sequence DP with **three operations** (Levenshtein distance) |
| **Code** | [M22_EditDistance.java](M22_EditDistance.java) |
| **Run** | `java M22_EditDistance.java` |

The two-string table layout is explained in [M21 LCS](M21_LongestCommonSubsequence.md); the DP recipe is in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe).

---

## 1. The problem

Return the minimum number of single-character **insert**, **delete** or **replace** operations that turn word1 into word2.

| word1 | word2 | answer | one sequence |
|---|---|---|---|
| "horse" | "ros" | 3 | horse → rorse (replace h) → rose (delete r) → ros (delete e) |
| "intention" | "execution" | 5 | |
| "" | "abc" | 3 | insert 3 |
| "kitten" | "sitting" | 3 | |

---

## 2. How to think about it

### Decide what happens to the last characters

| step | for this problem |
|---|---|
| **State** | dp[i][j] = the edits needed to turn a[0..i) into b[0..j) |
| **Equal last characters** | nothing to do: dp[i − 1][j − 1] |
| **Otherwise, 1 + the min of** | **replace** a[i − 1] with b[j − 1]: dp[i − 1][j − 1] |
| | **delete** a[i − 1]: dp[i − 1][j] |
| | **insert** b[j − 1] at the end: dp[i][j − 1] |
| **Base** | dp[i][0] = i (delete everything), dp[0][j] = j (insert everything) |
| **Answer** | dp[n][m] |

The three neighbours of a cell (diagonal, up, left) are exactly the three operations.

### Dry run: "horse" → "ros"

| | "" | r | o | s |
|---|---|---|---|---|
| "" | 0 | 1 | 2 | 3 |
| h | 1 | 1 | 2 | 3 |
| o | 2 | 2 | **1** | 2 |
| r | 3 | 2 | 2 | 2 |
| s | 4 | 3 | 3 | **2** |
| e | 5 | 4 | 4 | **3** |

For example, (o, o) matches, so it copies the diagonal (h vs r) = 1. (e, s) doesn't match: 1 + min(diagonal 3, up 2, left 4) = 3, the "delete e" route.

---

## 3. Java concepts used in this problem

### 3.1 One row + diagonal, with a changing first column

```java
for (int j = 0; j <= m; j++) dp[j] = j;          // row 0
for (int i = 1; i <= a.length(); i++) {
    int diag = dp[0];                            // dp[i-1][0]
    dp[0] = i;                                   // column 0 for row i
    for (int j = 1; j <= m; j++) {
        int up = dp[j];                          // dp[i-1][j]
        ...
        diag = up;
    }
}
```

Unlike the LCS, column 0 isn't all zeros here (dp[i][0] = i), so before each row the old dp[0] is saved as the first diagonal, then replaced by i.

### 3.2 A 2-D `Integer[][]` memo (in the referee)

`new Integer[a.length() + 1][b.length() + 1]` starts as all `null`, meaning "not computed". The referee works on **suffixes** (i and j are starting positions), which is the mirror image of the prefix table.

### 3.3 Metric properties as a test

The test also checks that the distance is **symmetric** (d(a, b) = d(b, a)) and satisfies the **triangle inequality** (d(a, c) ≤ d(a, b) + d(b, c)). Those are properties any correct edit distance must have, so they catch bugs a single referee might share.

---

## 4. Approach 1: Memoized recursion on suffixes (the referee)

### Code ([M22_EditDistance.java:60-68](M22_EditDistance.java#L60-L68))

```java
static int memo(String a, String b, int i, int j, Integer[][] cache) {
    if (i == a.length()) return b.length() - j;
    if (j == b.length()) return a.length() - i;
    if (cache[i][j] != null) return cache[i][j];
    int r;
    if (a.charAt(i) == b.charAt(j)) r = memo(a, b, i + 1, j + 1, cache);
    else r = 1 + Math.min(memo(a, b, i + 1, j + 1, cache), Math.min(memo(a, b, i + 1, j, cache), memo(a, b, i, j + 1, cache)));
    return cache[i][j] = r;
}
```

| Line | What it does |
|---|---|
| `i == a.length()` | a is used up: insert the rest of b |
| `j == b.length()` | b is used up: delete the rest of a |
| equal first characters | skip both |
| otherwise | replace (i+1, j+1), delete (i+1, j) or insert (i, j+1) |

- **Time O(n · m), space O(n · m).**

---

## 5. Approach 2: Bottom-up with one row (the file's main solution)

### Code ([M22_EditDistance.java:42-57](M22_EditDistance.java#L42-L57))

```java
static int minDistance(String a, String b) {
    int m = b.length();
    int[] dp = new int[m + 1];
    for (int j = 0; j <= m; j++) dp[j] = j;          // row 0: insert j characters
    for (int i = 1; i <= a.length(); i++) {
        int diag = dp[0];                            // dp[i-1][0]
        dp[0] = i;                                   // column 0: delete i characters
        for (int j = 1; j <= m; j++) {
            int up = dp[j];                          // dp[i-1][j]
            if (a.charAt(i - 1) == b.charAt(j - 1)) dp[j] = diag;
            else dp[j] = 1 + Math.min(diag, Math.min(up, dp[j - 1]));   // replace / delete / insert
            diag = up;
        }
    }
    return dp[m];
}
```

### Line by line

| Line | What it does |
|---|---|
| row 0 | turning "" into b[0..j) takes j inserts |
| `diag`, `dp[0] = i` | set up the new row's first column |
| `up` | dp[i − 1][j], saved before it's overwritten |
| a match | free: copy the diagonal |
| a mismatch | 1 + the cheapest of the three operations |
| `diag = up` | the next cell's diagonal |

### Complexity
- **Time O(n · m), space O(m).**

---

## 6. Comparing the approaches

| | Memo on suffixes | One-row table |
|---|---|---|
| Time | O(n · m) | O(n · m) |
| Space | O(n · m) + stack | **O(m)** |

The file checks the table against the memo on 1,000 random string pairs, plus symmetry and the triangle inequality on 300 triples.

---

## 7. Edge cases and common mistakes

**Edge cases:** either string empty, identical strings (0), completely different strings of equal length (n replaces).

**Mistakes:**
1. Base cases of 0 instead of i and j.
2. Adding 1 even when the characters match.
3. Mixing up which neighbour is insert and which is delete (it doesn't change the answer, but it matters if you reconstruct the operations).
4. In the one-row version, not saving the old dp[0] as the first diagonal.

---

## 8. Interview questions and answers

**Q1. Why do the three neighbours correspond to the three operations?**
> Diagonal: both strings lose their last character (a replace, or a free match). Up: only a loses its last character (a delete). Left: only b loses its last character, meaning we inserted it into a.

**Q2. Where is edit distance used?**
> Spell checkers, DNA sequence alignment (with weighted operations), fuzzy search, and measuring how similar two texts are.

### Follow-up questions

**F1. Only insertions and deletions.**
> n + m − 2 · LCS(a, b) ([M21](M21_LongestCommonSubsequence.md)).

**F2. One Edit Distance (LC 161).**
> Just check whether the distance is exactly 1, in O(n) with two pointers.

**F3. Different costs per operation.**
> Replace the 1s with the costs: dp[i][j] = min(diag + replaceCost, up + deleteCost, left + insertCost).

---

## 9. Running the file

```bash
java M22_EditDistance.java      # 7/7 tests passed (including 1000 random: table == top-down, + metric checks)
```

**One-line summary:** *"dp[i][j] copies the diagonal when the last characters match, otherwise it's 1 + min(diagonal = replace, up = delete, left = insert), with dp[i][0] = i and dp[0][j] = j."*
