# M03 · Permutation in String

| | |
|---|---|
| **LeetCode** | [#567 Permutation in String](https://leetcode.com/problems/permutation-in-string/) |
| **Difficulty** | Medium |
| **Pattern** | Fixed-size sliding window + frequency comparison |
| **Code** | [M03_PermutationInString.java](M03_PermutationInString.java) |
| **Run** | `java M03_PermutationInString.java` |

---

## 1. The problem

Does `s2` contain a **permutation** of `s1` as a **substring**? In other words, is some window of `s2` an anagram of `s1`?

| s1 | s2 | answer | why |
|---|---|---|---|
| `"ab"` | `"eidbaooo"` | true | `"ba"` at index 3 |
| `"ab"` | `"eidboaoo"` | false | b and a are never adjacent |
| `"adc"` | `"dcda"` | true | `"cda"` |
| `"abc"` | `"ab"` | false | s1 is longer |

**Constraints:** `1 ≤ lengths ≤ 10⁴`, lowercase letters.

---

## 2. How to think about it

1. **Brute force:** generate every permutation of s1 (m! of them!) and search for each. Hopeless.
2. **Reframe:** a permutation of s1 = a substring with **exactly s1's letter counts** (an anagram, [E01](../02-Easy/E01_ValidAnagram.md)). Any such substring has length **m = |s1|**.
3. So **slide a window of size m** over s2 and compare its letter counts with s1's counts.
   - Each slide: one character enters on the right and one leaves on the left (the fixed-window pattern from [Arrays E23](../../01-Arrays/02-Easy/E23_MaximumAverageSubarrayI.md)).
   - Comparing two `int[26]` arrays is 26 steps, so it's O(26n), which is already linear.
4. **An O(n) refinement:** keep `matches` = how many of the 26 letters currently have **equal** counts in the window and in s1. Each slide changes only **2** letters, so only update `matches` for those two. When `matches == 26`, the window is a permutation.

---

## 3. Java concepts used in this problem

### 3.1 `Arrays.equals(int[], int[])`

Compares two arrays **element by element** (O(length)). `need == window` would compare references (always false for two different arrays).

### 3.2 The fixed-size window update

```java
window[s2.charAt(i) - 'a']++;        // the character entering on the right
window[s2.charAt(i - m) - 'a']--;    // the character leaving on the left
```

After this, the window covers `s2[i−m+1 .. i]`.

### 3.3 A loop with no condition: `for (int i = m; ; i++)`

An empty condition means "loop forever"; the body leaves with `return`. It's used here because the check (`matches == 26`) has to happen **before** each slide **and** once after the last slide.

### 3.4 A "diff" array instead of two arrays

`diff[c] = window count − needed count`. The window matches s1 exactly when **all diffs are 0**. One array halves the bookkeeping.

---

## 4. Approach 1: Compare count arrays at each position (the file's main version)

### Code ([M03_PermutationInString.java:39-54](M03_PermutationInString.java#L39-L54))

```java
static boolean checkInclusion(String s1, String s2) {
    int m = s1.length(), n = s2.length();
    if (m > n) return false;
    int[] need = new int[26], window = new int[26];
    for (int i = 0; i < m; i++) {
        need[s1.charAt(i) - 'a']++;
        window[s2.charAt(i) - 'a']++;
    }
    if (Arrays.equals(need, window)) return true;
    for (int i = m; i < n; i++) {
        window[s2.charAt(i) - 'a']++;          // char entering on the right
        window[s2.charAt(i - m) - 'a']--;      // char leaving on the left
        if (Arrays.equals(need, window)) return true;
    }
    return false;
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (m > n) return false;` | the window can't fit |
| first loop | count s1 (`need`) and the **first window** `s2[0..m−1]` |
| `if (Arrays.equals(need, window)) return true;` | check the first window |
| second loop | slide: add `s2[i]`, remove `s2[i − m]`, then compare |
| `return false;` | no window matched |

### Dry run: `s1 = "ab"`, `s2 = "eidbaooo"`

`need = {a:1, b:1}`

| window | counts | equals need? |
|---|---|---|
| `ei` | e:1, i:1 | no |
| `id` | i:1, d:1 | no |
| `db` | d:1, b:1 | no |
| `ba` | b:1, a:1 | **yes → true** |

### Complexity
- **Time O(m + 26·n)**, **Space O(1).**

---

## 5. Approach 2: Track the number of matching letters (the file's O(n) version)

### Code ([M03_PermutationInString.java:57-76](M03_PermutationInString.java#L57-L76))

```java
static boolean checkInclusionMatches(String s1, String s2) {
    int m = s1.length(), n = s2.length();
    if (m > n) return false;
    int[] diff = new int[26];                  // diff = window count - needed count
    for (int i = 0; i < m; i++) {
        diff[s1.charAt(i) - 'a']--;
        diff[s2.charAt(i) - 'a']++;
    }
    int matches = 0;
    for (int d : diff) if (d == 0) matches++;
    for (int i = m; ; i++) {
        if (matches == 26) return true;
        if (i == n) return false;
        int in = s2.charAt(i) - 'a', out = s2.charAt(i - m) - 'a';
        if (diff[in] == 0) matches--;          // was balanced, about to change
        if (++diff[in] == 0) matches++;
        if (diff[out] == 0) matches--;
        if (--diff[out] == 0) matches++;
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `diff[...]--` for s1, `diff[...]++` for the first window | `diff[c] = window − need` for the first window |
| `matches` = the number of zero diffs | how many letters are already balanced |
| `if (matches == 26) return true;` | every letter balanced → the window is a permutation |
| `if (i == n) return false;` | no more windows |
| `if (diff[in] == 0) matches--;` | the entering letter was balanced; changing it breaks that |
| `if (++diff[in] == 0) matches++;` | …or the change makes it balanced |
| the same two lines for `out` with `--` | the leaving letter |

The update rule: **before** changing a letter's diff, if it was 0, drop a match; **after** changing it, if it's 0, add a match.

### Dry run: `s1 = "ab"`, `s2 = "eidbaooo"`

First window `"ei"`: diff a = −1, b = −1, e = +1, i = +1, and the other 22 letters are 0 → **matches = 22**.

| window | entering / leaving | matches after |
|---|---|---|
| `ei` | (start) | 22 |
| `id` | +d (0 → 1: −1), −e (1 → 0: +1) | 22 |
| `db` | +b (−1 → 0: +1), −i (1 → 0: +1) | 24 |
| `ba` | +a (−1 → 0: +1), −d (1 → 0: +1) | **26 → true** |

### Complexity
- **Time O(m + n)**: constant work per slide.
- **Space O(1).**

---

## 6. Comparing the approaches

| | Permutations | Compare arrays per slide | Matches counter |
|---|---|---|---|
| Time | O(m! · n) | O(26n) | **O(n)** |
| Space | huge | O(1) | O(1) |
| Bug risk | — | **low** | medium |

In an interview, the array-comparison version is perfectly fine (26 is a constant). Mention the matches trick as an optimisation.

---

## 7. Edge cases and common mistakes

**Edge cases:** s1 longer than s2, s1 equal to s2, a match in the first window, a match in the last window, repeated letters in s1.

**Mistakes:**
1. Forgetting to check the **first** window before sliding.
2. Removing `s2[i − m + 1]` instead of `s2[i − m]` (off by one).
3. Comparing arrays with `==`.
4. In the matches version, forgetting the "was balanced" decrement before changing a count.

---

## 8. Interview questions and answers

**Q1. Why a fixed window size?**
> Any permutation of s1 has exactly |s1| characters, so only windows of that size can match.

**Q2. How do you make each step O(1) instead of O(26)?**
> Keep a count of how many letters are balanced. A slide changes only two letters, so update the count for those two.

**Q3. Relation to Valid Anagram?**
> Each window check *is* an anagram check; the sliding window just reuses the counts between neighbouring windows.

### Follow-up questions

**F1. Find All Anagrams in a String (LC 438).** → [M05](M05_FindAllAnagrams.md)
> The same window, but collect every start index instead of returning on the first.

**F2. Minimum Window Substring (LC 76): the smallest window containing all of t's characters (extras allowed).** → [H01](../04-Hard/H01_MinimumWindowSubstring.md)
> A variable-size window, with "have / need" counts.

**F3. Unicode or large alphabets.**
> Use `HashMap` counts and track the number of matching keys the same way.

---

## 9. Running the file

```bash
java M03_PermutationInString.java      # 6/6 tests passed
```

**One-line summary:** *"Slide a window of size |s1| over s2; a permutation = a window whose letter counts equal s1's."*
