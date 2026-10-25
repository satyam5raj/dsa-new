# M05 · Find All Anagrams in a String

| | |
|---|---|
| **LeetCode** | [#438 Find All Anagrams in a String](https://leetcode.com/problems/find-all-anagrams-in-a-string/) |
| **Difficulty** | Medium |
| **Pattern** | Fixed-size sliding window + frequency ([M03](M03_PermutationInString.md), collecting every match) |
| **Code** | [M05_FindAllAnagrams.java](M05_FindAllAnagrams.java) |
| **Run** | `java M05_FindAllAnagrams.java` |

---

## 1. The problem

Return the **start indices** of every anagram of `p` inside `s`.

| s | p | answer | why |
|---|---|---|---|
| `"cbaebabacd"` | `"abc"` | `[0, 6]` | `"cba"` at 0, `"bac"` at 6 |
| `"abab"` | `"ab"` | `[0, 1, 2]` | windows can **overlap** |
| `"aaaa"` | `"aa"` | `[0, 1, 2]` | |
| `"abc"` | `"abcd"` | `[]` | p is longer |

**Constraints:** `1 ≤ lengths ≤ 3 × 10⁴`, lowercase letters.

---

## 2. How to think about it

The same as [M03 Permutation in String](M03_PermutationInString.md): slide a window of size `m = |p|` over s and check whether its letter counts equal p's. Here we **record** each matching start instead of stopping at the first one.

The file uses a neat variant with a **single** array and a **"missing" counter**:
- `need[c]` = how many more c's the window needs (it starts as p's counts)
- `missing` = the total number of characters still missing (it starts as m)
- **a character enters:** if `need[c] > 0`, it was really needed → `missing--`. Then `need[c]--`.
- **a character leaves:** `need[c]++`. If `need[c] > 0` now, we lost a needed character → `missing++`.
- `missing == 0` and the window size is m → **an anagram**.

`need[c]` can go **negative**: that means the window has **extra** c's, which is fine. Extras are reflected by the fact that `missing` can't reach 0 while the window size is fixed at m.

---

## 3. Java concepts used in this problem

### 3.1 Returning a `List<Integer>`

The number of matches isn't known in advance, so we collect them in an `ArrayList`. The test compares `list.toString()`, e.g. `"[0, 6]"`.

### 3.2 Early return of an empty list

```java
if (m > s.length()) return result;   // result is an empty ArrayList
```

Return an empty collection, not `null`: callers can loop over it safely.

### 3.3 One counter replacing a 26-element comparison

The `missing` counter summarises "how far the window is from p" in **one number**. That's the same idea as the `matches` counter in M03, and as `need`/`have` in Minimum Window Substring ([H01](../04-Hard/H01_MinimumWindowSubstring.md)).

---

## 4. Approach 1: Compare count arrays at each window

```java
static List<Integer> findAnagramsArrays(String s, String p) {
    List<Integer> res = new ArrayList<>();
    int m = p.length(), n = s.length();
    if (m > n) return res;
    int[] need = new int[26], win = new int[26];
    for (int i = 0; i < m; i++) { need[p.charAt(i) - 'a']++; win[s.charAt(i) - 'a']++; }
    if (Arrays.equals(need, win)) res.add(0);
    for (int i = m; i < n; i++) {
        win[s.charAt(i) - 'a']++;
        win[s.charAt(i - m) - 'a']--;
        if (Arrays.equals(need, win)) res.add(i - m + 1);   // the start of the window ending at i
    }
    return res;
}
```

M03's code, with `res.add(...)` instead of `return true`. **O(26n) time, O(1) space.**

---

## 5. Approach 2: One array + a "missing" counter (the file's solution)

### Code ([M05_FindAllAnagrams.java:40-61](M05_FindAllAnagrams.java#L40-L61))

```java
static List<Integer> findAnagrams(String s, String p) {
    List<Integer> result = new ArrayList<>();
    int m = p.length();
    if (m > s.length()) return result;
    int[] need = new int[26];
    for (char c : p.toCharArray()) need[c - 'a']++;
    int missing = m;

    for (int right = 0; right < s.length(); right++) {
        int in = s.charAt(right) - 'a';
        if (need[in] > 0) missing--;               // this char was actually needed
        need[in]--;

        if (right >= m) {                          // window too big: drop the left char
            int out = s.charAt(right - m) - 'a';
            need[out]++;
            if (need[out] > 0) missing++;          // we lost a needed char
        }
        if (missing == 0) result.add(right - m + 1);
    }
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| `need[c - 'a']++` over p | the requirement |
| `int missing = m;` | nothing has been matched yet |
| `if (need[in] > 0) missing--; need[in]--;` | the entering character fills a need (if one was open), then the need goes down (possibly below 0 = an extra) |
| `if (right >= m)` | the window now has m + 1 characters → remove `s[right − m]` |
| `need[out]++; if (need[out] > 0) missing++;` | giving the character back; if it was a needed one (not an extra), we're missing one again |
| `if (missing == 0) result.add(right - m + 1);` | the window `[right−m+1, right]` is an anagram. (Before the first full window, `missing` can't be 0, because fewer than m characters have entered) |

### Dry run 1: `s = "cbaebabacd"`, `p = "abc"`

| right | in | out | window | missing | record |
|---|---|---|---|---|---|
| 0 | c | — | c | 2 | |
| 1 | b | — | cb | 1 | |
| 2 | a | — | cba | **0** | **0** |
| 3 | e | c | bae | 1 | |
| 4 | b | b | aeb | 1 | |
| 5 | a | a | eba | 1 | |
| 6 | b | e | bab | 1 | |
| 7 | a | b | aba | 1 | |
| 8 | c | a | bac | **0** | **6** |
| 9 | d | b | acd | 1 | |

**Result `[0, 6]`** ✓

### Dry run 2: `s = "abab"`, `p = "ab"` (overlaps)

| right | in | out | need after | missing | record |
|---|---|---|---|---|---|
| 0 | a | — | a:0, b:1 | 1 | |
| 1 | b | — | a:0, b:0 | 0 | **0** |
| 2 | a | a | a: 0 → −1 → 0 (both steps change nothing in missing) | 0 | **1** |
| 3 | b | b | b: 0 → −1 → 0 | 0 | **2** |

**Result `[0, 1, 2]`** ✓. At right = 2, the entering `a` was an extra (need was 0, no change to missing) and the leaving `a` gave back that extra (need went back to 0, not > 0, so no change).

### Complexity
- **Time O(n + m)**: constant work per character.
- **Space O(1)**: 26 counters (the output aside).

---

## 6. Comparing the approaches

| | Compare arrays | Missing counter |
|---|---|---|
| Time | O(26n) | **O(n)** |
| Space | O(1) | O(1) |
| Clarity | **very clear** | needs explaining |

---

## 7. Edge cases and common mistakes

**Edge cases:** p longer than s, overlapping matches, p of length 1, s made of one repeated letter.

**Mistakes:**
1. Recording `right` instead of the window's **start** `right − m + 1`.
2. Forgetting the overlapping matches by jumping ahead after a match.
3. Treating a negative `need` as an error: negative just means "extra copies in the window".
4. Removing the leaving character **before** adding the entering one, then mis-sizing the window (it works if done consistently, but be careful).

---

## 8. Interview questions and answers

**Q1. Why does `missing == 0` mean an anagram?**
> The window always has exactly m characters, and `missing == 0` means all m needed characters are present. With exactly m characters, there's no room for extras, so the counts match exactly.

**Q2. How do overlapping matches work?**
> The window slides one character at a time, and each position is checked independently, so overlapping windows are all found.

### Follow-up questions

**F1. Permutation in String (LC 567).** → [M03](M03_PermutationInString.md)
> Return true on the first match.

**F2. Minimum Window Substring (LC 76).** → [H01](../04-Hard/H01_MinimumWindowSubstring.md)
> A variable-size window with the same need/missing bookkeeping; shrink while missing == 0.

**F3. Find anagrams of several patterns.**
> Group the patterns by length; for each length, slide one window and look up its canonical count key in a hash set of the pattern keys.

---

## 9. Running the file

```bash
java M05_FindAllAnagrams.java      # 4/4 tests passed
```

**One-line summary:** *"Fixed window of |p|; one need[] array and a missing counter; missing == 0 → record the start."*
