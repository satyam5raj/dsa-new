# H06 · Scramble String

| | |
|---|---|
| **LeetCode** | [#87 Scramble String](https://leetcode.com/problems/scramble-string/) |
| **Difficulty** | Hard |
| **Pattern** | Interval DP over (start₁, start₂, length): a 3-D state, as memoized recursion |
| **Code** | [H06_ScrambleString.java](H06_ScrambleString.java) |
| **Run** | `java H06_ScrambleString.java` |

Memoization is explained in [Recursion E01](../../13-Recursion/02-Easy/E01_FibonacciNumber.md#34-integer-as-a-memo-null-means-not-computed-yet); interval DP in [M24](../03-Medium/M24_PalindromicSubstringsDP.md).

---

## 1. The problem

To scramble a string: split it into two non-empty parts x + y, optionally swap them to get y + x, then scramble each part recursively. Is s2 a scramble of s1?

| s1 | s2 | answer |
|---|---|---|
| "great" | "rgeat" | true |
| "abcde" | "caebd" | false |
| "a" | "a" | true |

---

## 2. How to think about it

### The recursion
s1 and s2 (of the same length n) are scrambles of each other iff for some split k (1..n − 1):

| case | condition |
|---|---|
| **no swap** | scr(s1[0..k), s2[0..k)) and scr(s1[k..n), s2[k..n)) |
| **swap** | scr(s1[0..k), s2[n − k..n)) and scr(s1[k..n), s2[0..n − k)) |

Without memoization this is exponential: the same pairs of substrings are compared again and again.

### The state
A pair of substrings is determined by their two start positions and their (shared) length:

```
dp[i][j][len] = s1[i..i+len) is a scramble of s2[j..j+len)
```

There are only O(n³) states, each trying O(n) splits: **O(n⁴)** total.

**Pruning:** if the two substrings don't have the same letter counts, the answer is false immediately. This cuts most branches.

### Dry run: "great" / "rgeat"

```
scr("great", "rgeat")
  k = 1: no swap: "g" vs "r" ✗;   swap: "g" vs "t" ✗
  k = 2: no swap: scr("gr", "rg") and scr("eat", "eat")
         scr("gr", "rg"): k = 1, swap: "g" vs "g" ✓ and "r" vs "r" ✓ → true
         scr("eat", "eat"): identical → true
  → true
```

---

## 3. Java concepts used in this problem

### 3.1 A 3-D `Boolean` memo

```java
Boolean[][][] memo = new Boolean[n][n][n + 1];
if (memo[i][j][len] != null) return memo[i][j][len];
```

`Boolean` (the wrapper) can be `null`, meaning "not computed yet"; a primitive `boolean[][][]` couldn't tell "false" from "unknown".

### 3.2 `regionMatches` for a quick equality check

```java
a.regionMatches(i, b, j, len)
```

It compares a[i..i+len) with b[j..j+len) without creating substrings. Identical substrings are trivially scrambles.

### 3.3 Letter counts with one array

```java
int[] cnt = new int[26];
for (int x = 0; x < len; x++) { cnt[a.charAt(i + x) - 'a']++; cnt[b.charAt(j + x) - 'a']--; }
```

Adding for one string and subtracting for the other leaves all zeros exactly when they're anagrams.

### 3.4 Stopping the split loop early

`for (int k = 1; k < len && !res; k++)` stops at the first split that works.

---

## 4. Approach 1: Generate every scramble (the referee)

### Code ([H06_ScrambleString.java:63-72](H06_ScrambleString.java#L63-L72))

```java
static Set<String> allScrambles(String s, Map<String, Set<String>> cache) {
    if (cache.containsKey(s)) return cache.get(s);
    Set<String> out = new HashSet<>();
    out.add(s);
    for (int k = 1; k < s.length(); k++)
        for (String x : allScrambles(s.substring(0, k), cache))
            for (String y : allScrambles(s.substring(k), cache)) { out.add(x + y); out.add(y + x); }
    cache.put(s, out);
    return out;
}
```

It builds the set of **all** scrambles directly from the definition. The test checks that a randomly chosen member is accepted and that a random shuffle is accepted exactly when it's in the set. Only practical for short strings (n ≤ 6 in the test).

---

## 5. Approach 2: Memoized recursion with pruning (the file's main solution)

### Code ([H06_ScrambleString.java:36-60](H06_ScrambleString.java#L36-L60))

```java
static boolean isScramble(String s1, String s2) {
    int n = s1.length();
    if (n != s2.length()) return false;
    Boolean[][][] memo = new Boolean[n][n][n + 1];
    return scr(s1, s2, 0, 0, n, memo);
}

static boolean scr(String a, String b, int i, int j, int len, Boolean[][][] memo) {
    if (memo[i][j][len] != null) return memo[i][j][len];
    boolean res = false;
    if (a.regionMatches(i, b, j, len)) res = true;                    // identical: trivially a scramble
    else if (sameLetters(a, b, i, j, len)) {                           // else not even an anagram -> false
        for (int k = 1; k < len && !res; k++)
            res = (scr(a, b, i, j, k, memo) && scr(a, b, i + k, j + k, len - k, memo))            // no swap
               || (scr(a, b, i, j + len - k, k, memo) && scr(a, b, i + k, j, len - k, memo));     // swap
    }
    return memo[i][j][len] = res;
}

static boolean sameLetters(String a, String b, int i, int j, int len) {
    int[] cnt = new int[26];
    for (int x = 0; x < len; x++) { cnt[a.charAt(i + x) - 'a']++; cnt[b.charAt(j + x) - 'a']--; }
    for (int c : cnt) if (c != 0) return false;
    return true;
}
```

### Line by line

| Line | What it does |
|---|---|
| the memo check | each (i, j, len) is solved once |
| `regionMatches` | equal substrings: true |
| `sameLetters` | not anagrams: false without trying splits |
| no swap | a's first k with b's first k, and the rests |
| swap | a's first k with b's **last** k, and a's rest with b's first len − k |

A single character is handled by `regionMatches` (it's either identical or not an anagram), so there's no explicit base case.

### Complexity
- **Time O(n⁴)** (O(n³) states × O(n) splits, plus the O(n) letter check).
- **Space O(n³)** for the memo.

---

## 6. Comparing the approaches

| | All scrambles | Memo + pruning |
|---|---|---|
| Time | exponential (the number of scrambles) | **O(n⁴)** |
| Practical length | ~6 | 30 (LeetCode's limit) easily |

The file runs 300 random checks against the generated sets.

---

## 7. Edge cases and common mistakes

**Edge cases:** single characters, identical strings, different lengths (false), anagrams that aren't scrambles ("abcde" / "caebd").

**Mistakes:**
1. Getting the swap indices wrong (it's s2's **last** k characters).
2. Memoizing on substrings instead of indices (works, but slower: hashing strings).
3. Skipping the anagram pruning (correct but much slower).

---

## 8. Interview questions and answers

**Q1. Why is (i, j, len) enough state?**
> Both substrings always have the same length, so their two start positions plus the length identify them completely.

**Q2. Why prune with letter counts?**
> Scrambling only rearranges characters, so two strings with different letter counts can never be scrambles. The check is O(len) and eliminates most splits early.

### Follow-up questions

**F1. Bottom-up version.**
> Fill dp[len][i][j] for len from 1 to n, trying every split for each; same O(n⁴).

**F2. Burst Balloons.** → [H01](H01_BurstBalloons.md)
> Another interval DP with a split point inside the interval.

**F3. Different Ways to Add Parentheses.** → [Recursion M05](../../13-Recursion/03-Medium/M05_DifferentWaysToAddParentheses.md)
> Recursion over every split, with memoization by substring.

---

## 9. Running the file

```bash
java H06_ScrambleString.java      # 4/4 tests passed (including 300 random vs generating all scrambles)
```

**One-line summary:** *"Memoize scr(i, j, len): true if the substrings are equal; false if they aren't anagrams; otherwise try every split k, both without a swap and with one."*
