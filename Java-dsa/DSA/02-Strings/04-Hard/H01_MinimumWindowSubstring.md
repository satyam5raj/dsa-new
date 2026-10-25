# H01 · Minimum Window Substring

| | |
|---|---|
| **LeetCode** | [#76 Minimum Window Substring](https://leetcode.com/problems/minimum-window-substring/) |
| **Difficulty** | Hard |
| **Pattern** | Sliding window (expand until valid, shrink while valid) + a "missing" counter |
| **Code** | [H01_MinimumWindowSubstring.java](H01_MinimumWindowSubstring.java) |
| **Run** | `java H01_MinimumWindowSubstring.java` |

---

## 1. The problem

Find the **smallest substring** of `s` that contains **every character of `t`**, counting duplicates. Return `""` if there's none.

| s | t | answer | why |
|---|---|---|---|
| `"ADOBECODEBANC"` | `"ABC"` | `"BANC"` | the shortest window with an A, a B and a C |
| `"a"` | `"a"` | `"a"` | |
| `"a"` | `"aa"` | `""` | two a's are needed |
| `"bba"` | `"ab"` | `"ba"` | |

**Constraints:** `1 ≤ |s|, |t| ≤ 10⁵`, English letters (upper and lower case). Follow-up: *O(m + n)*.

This is **the** sliding-window interview problem. Learn it inside out.

---

## 2. How to think about it

1. **Brute force:** every substring (n²) × a coverage check (n) = O(n³).
2. **Sliding window, "minimum" template** (like [Arrays M11](../../01-Arrays/03-Medium/M11_MinimumSizeSubarraySum.md)), with character counts instead of a sum:
   - **expand** `right` until the window covers t
   - then **shrink** `left` as long as it still covers t, recording each valid window
   - once it stops covering t, go back to expanding

### The bookkeeping (the same idea as [M05](../03-Medium/M05_FindAllAnagrams.md))
- `need[c]` = how many more c's the window still needs (it starts as t's counts)
- `missing` = the total number of characters still needed (it starts as |t|)
- **a character enters:** if `need[c] > 0`, it was required → `missing--`. Then `need[c]--`.
- **a character leaves:** `need[c]++`. If `need[c] > 0` now, we lost a required character → `missing++`.
- **`missing == 0`** ⇔ the window covers t.

`need[c]` goes **negative** for surplus characters (more copies than t needs, or characters that aren't in t at all). When such a character leaves, `need` moves back towards 0 but doesn't go above it, so `missing` stays the same. Only losing a **required** character breaks validity.

---

## 3. Java concepts used in this problem

### 3.1 `int[128]` for upper- and lowercase letters

`'A'` = 65 and `'a'` = 97, so `int[26]` with `c − 'a'` won't work. Indexing directly by the ASCII code with `int[128]` covers both.

### 3.2 Remember the best window by its start and length, not by the string

```java
bestStart = left; bestLen = right - left + 1;      // cheap
...
return s.substring(bestStart, bestStart + bestLen); // build the string ONCE at the end
```

Calling `substring` for every candidate would allocate a new String every time.

### 3.3 `Integer.MAX_VALUE` as "no window found yet"

The final ternary returns `""` if `bestLen` was never updated.

### 3.4 Testing against brute force

`main` compares with `minWindowBrute` on 400 random cases (alphabet {a, b, c}). The brute force picks the shortest window and, among those of equal length, the **leftmost** one, which matches the sliding window, because it only records **strictly shorter** windows.

---

## 4. Approach 1: Brute force

### Code ([H01_MinimumWindowSubstring.java:75-90](H01_MinimumWindowSubstring.java#L75-L90))

```java
static String minWindowBrute(String s, String t) {
    String best = "";
    for (int i = 0; i < s.length(); i++) {
        for (int j = i + 1; j <= s.length(); j++) {
            if ((best.isEmpty() || j - i < best.length()) && covers(s.substring(i, j), t)) best = s.substring(i, j);
        }
    }
    return best;
}

static boolean covers(String w, String t) {
    int[] c = new int[128];
    for (char ch : w.toCharArray()) c[ch]++;
    for (char ch : t.toCharArray()) if (--c[ch] < 0) return false;
    return true;
}
```

`covers` is [Ransom Note](../02-Easy/E09_RansomNote.md): the window must supply every character that t demands. **O(n³)**. It's the test reference.

---

## 5. Approach 2: Sliding window with need/missing (the file's solution)

### Code ([H01_MinimumWindowSubstring.java:49-72](H01_MinimumWindowSubstring.java#L49-L72))

```java
static String minWindow(String s, String t) {
    if (t.isEmpty() || t.length() > s.length()) return "";
    int[] need = new int[128];
    for (char c : t.toCharArray()) need[c]++;
    int missing = t.length();
    int left = 0, bestStart = 0, bestLen = Integer.MAX_VALUE;

    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        if (need[c] > 0) missing--;            // a required char arrived
        need[c]--;

        while (missing == 0) {                 // valid: try to shrink
            if (right - left + 1 < bestLen) {
                bestLen = right - left + 1;
                bestStart = left;
            }
            char d = s.charAt(left++);
            need[d]++;
            if (need[d] > 0) missing++;        // we just dropped a required char
        }
    }
    return bestLen == Integer.MAX_VALUE ? "" : s.substring(bestStart, bestStart + bestLen);
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (t.isEmpty() \|\| t.length() > s.length()) return "";` | quick rejects |
| `need[c]++` over t | what's required |
| `int missing = t.length();` | nothing covered yet |
| `if (need[c] > 0) missing--; need[c]--;` | **expand**: a required copy fills a need; a surplus copy just pushes `need` below 0 |
| `while (missing == 0)` | the window covers t, so try to make it smaller |
| `if (right - left + 1 < bestLen) {...}` | record the window **before** shrinking it |
| `char d = s.charAt(left++); need[d]++;` | **shrink**: give back the leftmost character |
| `if (need[d] > 0) missing++;` | it was a required copy (not surplus), so the window no longer covers t, and the loop ends |
| final `return` | the best window, or `""` |

### Dry run: `s = "ADOBECODEBANC"`, `t = "ABC"`

| right | char | missing after expand | shrinking (record → drop) | best |
|---|---|---|---|---|
| 0 | A | 2 | — | |
| 1 | D | 2 | — | |
| 2 | O | 2 | — | |
| 3 | B | 1 | — | |
| 4 | E | 1 | — | |
| 5 | C | **0** | record `ADOBEC` (6) → drop A (**required**, missing = 1) | ADOBEC |
| 6–9 | O, D, E, B | 1 | — (the B is a surplus copy) | |
| 10 | A | **0** | window `DOBECODEBA` (10): drop D, O, B (surplus), E → `CODEBA` (6, not < 6) → drop C (**required**) | ADOBEC |
| 11 | N | 1 | — | |
| 12 | C | **0** | `ODEBANC` (7) → drop O; `DEBANC` (6) → drop D; record `EBANC` (5) → drop E; record **`BANC`** (4) → drop B (**required**) | **BANC** |

**Return `"BANC"`** ✓

### Dry run 2: `s = "a"`, `t = "aa"`
`t` is longer than `s` → **return ""** immediately ✓

### Dry run 3: `s = "bba"`, `t = "ab"`
right 0 (b): missing 1; right 1 (b): surplus, missing 1; right 2 (a): missing 0 → window `bba` (3) recorded → drop b: need[b] goes from −1 to 0, not > 0, so it's still valid → record `ba` (2) → drop b (**required**) → stop. **Return `"ba"`** ✓

### Complexity
- **Time O(|s| + |t|):** each index enters once (`right`) and leaves at most once (`left`).
- **Space O(1):** 128 counters.

---

## 6. Comparing the approaches

| | Brute force | Sliding window |
|---|---|---|
| Time | O(n³) | **O(n + m)** |
| Space | O(1) per check | O(128) |

---

## 7. Edge cases and common mistakes

**Edge cases:** t longer than s, t with repeated characters (`"aa"`), uppercase and lowercase, the answer being the whole string, no valid window.

**Mistakes:**
1. Treating t as a **set** (ignoring duplicates in t).
2. Recording the window **after** shrinking (you'd record an invalid window).
3. Using `int[26]`, which breaks on uppercase letters.
4. Decrementing `missing` for every character that enters, not just the required ones.
5. Building `substring`s inside the loop (slow).

---

## 8. Interview questions and answers

**Q1. Explain the two phases of the window.**
> Expand right until the window covers t (missing == 0). Then shrink from the left while it still covers t, recording each candidate. As soon as removing a character breaks coverage, go back to expanding.

**Q2. Why does `missing` only change for required characters?**
> `need[c] > 0` means the window still lacks c. Surplus copies make `need[c]` negative; removing them brings it back towards 0 but never above it, so coverage isn't affected.

**Q3. What's the complexity, and why?**
> O(n + m): building the counts is O(m); the two pointers each move at most n times.

**Q4. How would you optimise when t is tiny and s is huge, with many characters not in t?**
> Pre-filter s into a list of `(index, char)` for the characters that are in t only, and slide over that list. It's the same complexity, but much faster in practice (LeetCode's "optimised" solution).

### Follow-up questions

**F1. Find All Anagrams / Permutation in String.** → [M05](../03-Medium/M05_FindAllAnagrams.md), [M03](../03-Medium/M03_PermutationInString.md)
> The same counts, but with a **fixed** window size equal to |t| (no extra characters allowed).

**F2. Minimum Window Subsequence (LC 727): t must appear in order, not just be covered.**
> Different: for each window start, match t forward, then optimise backwards; or use DP. O(n·m).

**F3. The smallest window containing all the distinct characters of s itself.**
> t = the set of distinct characters of s; the same algorithm.

**F4. Smallest range covering elements from k lists (LC 632).** → [08-Heap H02](../../08-Heap/04-Hard/H02_SmallestRangeCoveringKLists.md)
> The same "cover everything, then shrink" idea, over merged sorted lists (with a heap or a sorted sweep).

---

## 9. Running the file

```bash
java H01_MinimumWindowSubstring.java      # 6/6 tests passed (including 400 random cross-checks)
```

**One-line summary:** *"Expand until missing == 0, then shrink while still valid (recording each window); only losing a required character breaks it."*
