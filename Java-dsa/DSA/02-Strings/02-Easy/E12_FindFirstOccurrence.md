# E12 · Find the Index of the First Occurrence in a String ⭐

| | |
|---|---|
| **LeetCode** | [#28 Find the Index of the First Occurrence in a String](https://leetcode.com/problems/find-the-index-of-the-first-occurrence-in-a-string/) |
| **Difficulty** | Easy (KMP makes it a classic) |
| **Pattern** | String matching: naive → KMP |
| **Code** | [E12_FindFirstOccurrence.java](E12_FindFirstOccurrence.java) |
| **Run** | `java E12_FindFirstOccurrence.java` |

⭐ = a bonus classic.

---

## 1. The problem

Return the index of the **first occurrence** of `needle` in `haystack`, or `-1`. This is what `String.indexOf` does, and interviewers want you to implement it.

| haystack | needle | answer |
|---|---|---|
| `"sadbutsad"` | `"sad"` | 0 |
| `"leetcode"` | `"leeto"` | −1 |
| `"aaaaaaaaab"` | `"aaab"` | 6 |
| `"mississippi"` | `"issip"` | 4 |

**Constraints:** `1 ≤ lengths ≤ 10⁴`. Let n = |haystack|, m = |needle|.

---

## 2. How to think about it

### Naive: try every start
For each start `i` in `0..n−m`, compare the needle with `haystack[i..i+m−1]`. It's usually fast, because mismatches happen early, but the worst case is **O(n·m)**:

```
haystack = a a a a a a a a a b
needle   = a a a b               ← at every start: 3 matching a's, then a mismatch
```

### KMP: never re-read the haystack
When a mismatch happens after matching `j` characters, the naive method throws that knowledge away and restarts one position later. **KMP (Knuth–Morris–Pratt)** asks:

> *"Of the j characters I just matched, how much can I keep?"*

The answer depends **only on the needle**, so it can be precomputed in an array called **LPS** ("longest proper prefix which is also a suffix"):

```
lps[k] = the length of the longest proper prefix of needle[0..k]
         that is also a suffix of needle[0..k]
```

On a mismatch after matching j characters, jump to `j = lps[j − 1]`: those characters are already known to match, so there's no need to re-check them. The haystack pointer **never moves backwards**, which gives **O(n + m)**.

**Example:** needle `"aaab"` has `lps = [0, 1, 2, 0]`. After matching `"aaa"` and seeing another `a` instead of `b`, keep `lps[2] = 2` characters (`"aa"`) and continue. The naive method would restart and re-compare them.

---

## 3. Java concepts used in this problem

### 3.1 `String.indexOf` (the built-in)

```java
"mississippi".indexOf("issip")      // 4
"mississippi".indexOf("issip", 5)   // -1: search starting from index 5
"abc".indexOf('b')                  // 1: also works with a char
```

The JDK's implementation is essentially the naive algorithm, heavily optimised. The test in `main` uses it as the reference.

### 3.2 A `for` loop with an empty update part

```java
for (int i = 1, len = 0; i < m; ) { ... }   // i is advanced inside the body, only in some branches
```

All three parts of a `for` header are optional. Here `i` only advances in two of the three branches, so the update section is left empty.

### 3.3 `lps[i++] = ++len;`

It increments `len` first (pre-increment), stores it in `lps[i]`, then increments `i` (post-increment). It's dense: read it as `len++; lps[i] = len; i++;`.

### 3.4 Random tests against the library

`main` runs 1000 random haystacks/needles over the alphabet {a, b} (a small alphabet creates many partial matches) and compares both methods with `String.indexOf`.

---

## 4. Approach 1: Naive sliding comparison

### Code ([E12_FindFirstOccurrence.java:34-42](E12_FindFirstOccurrence.java#L34-L42))

```java
static int strStrNaive(String haystack, String needle) {
    int n = haystack.length(), m = needle.length();
    for (int i = 0; i + m <= n; i++) {
        int j = 0;
        while (j < m && haystack.charAt(i + j) == needle.charAt(j)) j++;
        if (j == m) return i;
    }
    return -1;
}
```

| Line | What it does |
|---|---|
| `for (int i = 0; i + m <= n; i++)` | each possible start; the needle must fit (`i + m ≤ n`) |
| `while (j < m && haystack[i+j] == needle[j]) j++;` | count how many characters match from this start |
| `if (j == m) return i;` | all m matched |
| `return -1;` | no start worked |

**Dry run** `"sadbutsad"` / `"sad"`: i = 0: s = s, a = a, d = d → j = 3 → **return 0**.

**Time O(n·m) worst case, O(1) space.**

---

## 5. Approach 2: KMP (the file's main solution)

### Code ([E12_FindFirstOccurrence.java:44-59](E12_FindFirstOccurrence.java#L44-L59))

```java
static int strStrKmp(String haystack, String needle) {
    int m = needle.length();
    if (m == 0) return 0;
    int[] lps = new int[m];
    for (int i = 1, len = 0; i < m; ) {                       // build LPS
        if (needle.charAt(i) == needle.charAt(len)) lps[i++] = ++len;
        else if (len > 0) len = lps[len - 1];
        else lps[i++] = 0;
    }
    for (int i = 0, j = 0; i < haystack.length(); i++) {     // search
        while (j > 0 && haystack.charAt(i) != needle.charAt(j)) j = lps[j - 1];
        if (haystack.charAt(i) == needle.charAt(j)) j++;
        if (j == m) return i - m + 1;
    }
    return -1;
}
```

### Part 1: building the LPS array

| Line | What it does |
|---|---|
| `int[] lps = new int[m];` | `lps[0] = 0` always (a single character has no proper prefix that is also a suffix) |
| `i = 1, len = 0` | `len` = the length of the current matching prefix/suffix |
| `if (needle[i] == needle[len]) lps[i++] = ++len;` | the border grows by one character |
| `else if (len > 0) len = lps[len - 1];` | a mismatch: fall back to the next shorter border and retry the same `i` (no `i++`) |
| `else lps[i++] = 0;` | no border at all at position i |

**LPS of `"issip"`:**

| i | needle[i] | len before | compare with needle[len] | action | lps |
|---|---|---|---|---|---|
| 1 | s | 0 | i | mismatch, len = 0 → lps[1] = 0 | [0,0,_,_,_] |
| 2 | s | 0 | i | mismatch → lps[2] = 0 | [0,0,0,_,_] |
| 3 | i | 0 | i | **match** → len = 1, lps[3] = 1 | [0,0,0,1,_] |
| 4 | p | 1 | s | mismatch, len > 0 → len = lps[0] = 0 (retry i = 4) | |
| 4 | p | 0 | i | mismatch → lps[4] = 0 | [0,0,0,1,0] |

`lps = [0, 0, 0, 1, 0]`: for example, in `"issi"` the prefix `"i"` is also a suffix.

**LPS of `"aaab"`:** `[0, 1, 2, 0]`.

### Part 2: searching

| Line | What it does |
|---|---|
| `for (int i = 0, j = 0; i < n; i++)` | `i` walks the haystack once, **never moving back**; `j` = how many needle characters are currently matched |
| `while (j > 0 && h[i] != needle[j]) j = lps[j - 1];` | a mismatch: keep the longest border of what we'd matched, and retry |
| `if (h[i] == needle[j]) j++;` | extend the match |
| `if (j == m) return i - m + 1;` | the whole needle matched, ending at i |

### Dry run 1: `"mississippi"` / `"issip"` (lps = [0,0,0,1,0])

| i | h[i] | j before | fallback | match? | j after |
|---|---|---|---|---|---|
| 0 | m | 0 | — | no | 0 |
| 1 | i | 0 | — | yes | 1 |
| 2 | s | 1 | — | yes | 2 |
| 3 | s | 2 | — | yes | 3 |
| 4 | i | 3 | — | yes | 4 |
| 5 | s | 4 | needle[4] = p ≠ s → j = lps[3] = **1** | needle[1] = s ✓ | 2 |
| 6 | s | 2 | — | yes | 3 |
| 7 | i | 3 | — | yes | 4 |
| 8 | p | 4 | — | yes | **5 = m** |

**Return 8 − 5 + 1 = 4** ✓. At i = 5, KMP **kept** the `"i"` it had already matched (index 4) instead of restarting.

### Dry run 2: `"aaaaaaaaab"` / `"aaab"` (lps = [0,1,2,0]), the naive worst case

| i | h[i] | j before | action | j after |
|---|---|---|---|---|
| 0–2 | a | 0, 1, 2 | match | 3 |
| 3 | a | 3 | needle[3] = b ≠ a → j = lps[2] = 2; needle[2] = a ✓ | 3 |
| 4–8 | a | 3 | the same: fall back to 2, match → 3 | 3 |
| 9 | b | 3 | needle[3] = b ✓ | **4 = m** |

**Return 9 − 4 + 1 = 6** ✓. Each haystack character was examined a constant number of times, while the naive method re-compares about 3 characters at each of 7 starts.

### Complexity
- **LPS build O(m)**, **search O(n)** (amortised: `j` only increases by one per step, so all the fallbacks together are bounded by n) → **O(n + m)**.
- **Space O(m)** for `lps`.

---

## 6. Other algorithms (just know they exist)

| Algorithm | Idea | Time |
|---|---|---|
| **Rabin-Karp** | a rolling hash of each window; compare hashes, verify on a hash match | O(n + m) average |
| **Z-algorithm** | Z-array of `needle + "$" + haystack` | O(n + m) |
| **Boyer-Moore** | compare from the right; skip ahead using the mismatched character | sublinear in practice |

All are implemented in [StringMatchingAlgorithms](../01-Basics/StringMatchingAlgorithms.java).

---

## 7. Comparing the approaches

| | Naive | KMP |
|---|---|---|
| Time | O(n·m) worst, fast on average | **O(n + m) guaranteed** |
| Space | O(1) | O(m) |
| Interview | write it first, cleanly | explain why LPS avoids re-comparisons |

---

## 8. Edge cases and common mistakes

**Edge cases:** the needle is longer than the haystack (−1), the needle equals the haystack (0), the needle at the very end, overlapping partial matches (`"aaaaab"` / `"aab"`).

**Mistakes:**
1. Naive: the loop bound `i < n` instead of `i + m <= n` → `charAt` out of bounds.
2. KMP: incrementing `i` after a fallback while building the LPS (you must retry the same i).
3. KMP: returning `i − m` instead of `i − m + 1`.
4. Confusing "proper prefix" (not the whole string) with any prefix: `lps[k]` can't be k + 1.

---

## 9. Interview questions and answers

**Q1. What does `lps[k]` mean?**
> The length of the longest proper prefix of `needle[0..k]` that is also a suffix of it. After a mismatch with j characters matched, the last `lps[j−1]` characters of the haystack are guaranteed to equal the needle's first `lps[j−1]` characters, so we continue from there.

**Q2. Why is KMP O(n + m)?**
> The haystack index never decreases. `j` increases by at most one per character, and each fallback decreases it, so the total number of fallbacks over the whole search is at most n. Building the LPS has the same argument over m.

**Q3. When is the naive approach fine?**
> In practice with natural text, where mismatches come quickly. Library `indexOf` implementations are naive-style for exactly that reason.

### Follow-up questions

**F1. Find all occurrences.**
> In KMP, on `j == m` record `i − m + 1` and continue with `j = lps[m − 1]` (this allows overlapping matches).

**F2. Repeated Substring Pattern (LC 459): is s made of a repeated block?**
> With LPS: let `p = n − lps[n−1]`; s is repeated iff `lps[n−1] > 0` and `n % p == 0`. Or check whether s is inside `(s + s)` with the first and last characters removed.

**F3. Shortest Palindrome (LC 214).** → [04-Hard H02](../04-Hard/H02_ShortestPalindrome.md)
> The LPS of `s + "#" + reverse(s)` gives the longest palindromic prefix.

**F4. Search for many patterns at once.**
> Aho-Corasick (a trie + failure links, KMP generalised to many patterns).

---

## 10. Running the file

```bash
java E12_FindFirstOccurrence.java      # 6/6 tests passed (including 1000 random cases vs indexOf)
```

**One-line summary:** *"Naive: try every start. KMP: precompute borders (LPS) so a mismatch never re-reads the haystack: O(n + m)."*
