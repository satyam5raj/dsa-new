# M02 · Longest Repeating Character Replacement

| | |
|---|---|
| **LeetCode** | [#424 Longest Repeating Character Replacement](https://leetcode.com/problems/longest-repeating-character-replacement/) |
| **Difficulty** | Medium |
| **Pattern** | Sliding window + "window length − max frequency ≤ k" |
| **Code** | [M02_LongestRepeatingCharacterReplacement.java](M02_LongestRepeatingCharacterReplacement.java) |
| **Run** | `java M02_LongestRepeatingCharacterReplacement.java` |

---

## 1. The problem

`s` contains uppercase letters. You may **change at most k characters** (each into any letter). Return the length of the longest substring that can then consist of **one repeated letter**.

| s | k | answer | how |
|---|---|---|---|
| `"ABAB"` | 2 | 4 | change both A's (or both B's) |
| `"AABABBA"` | 1 | 4 | `"AABA"` → change the B → `"AAAA"` |
| `"AAAA"` | 0 | 4 | already uniform |
| `"ABCDE"` | 1 | 2 | e.g. `"AB"` → `"AA"` |

**Constraints:** `1 ≤ n ≤ 10⁵`, `0 ≤ k ≤ n`.

---

## 2. How to think about it

### How many changes does a window need?
To make a window uniform, **keep its most frequent letter** and change everything else:

```
changes needed = windowLength − maxFrequency
valid  ⇔  windowLength − maxFrequency ≤ k
```

For example, `"AABA"`: length 4, max frequency 3 (A) → 1 change.

### Sliding window (the maximum-length template)
- expand `right`, update `count[s[right]]` and `maxFreq`
- while the window needs more than k changes: remove `s[left]`, `left++`
- record the window length

### The famous subtlety: a stale `maxFreq`
When the window shrinks, the code does **not** decrease `maxFreq`, even though the true maximum frequency in the smaller window might be lower. Why is that OK?

- The answer only **grows** when we find a window whose `maxFreq` is **higher than ever before** (length = maxFreq + k).
- A stale, too-large `maxFreq` can only make the window look valid at its **current** size, which we've already achieved. It can never report a size **bigger** than some truly valid window's size.

So we skip recomputing it. (Recomputing the max over the 26 counts each step is also correct and easier to defend: O(26n).) The file verifies the result against brute force on 500 random strings.

---

## 3. Java concepts used in this problem

### 3.1 Pre-increment inside `Math.max`

```java
maxFreq = Math.max(maxFreq, ++count[s.charAt(right) - 'A']);
```

`++count[...]` increments the count **and** produces the new value, which is compared with `maxFreq`, all in one line.

### 3.2 `c - 'A'` for uppercase letters

The same trick as `c − 'a'` (see E01), but the input is uppercase: `'A'` → 0, …, `'Z'` → 25.

### 3.3 Brute force as a test oracle

`bruteForce` tries every substring with a running count; `main` compares it with the fast version on 500 random strings over {A, B, C}. When a solution relies on a subtle argument (like the stale maxFreq), test it this way.

---

## 4. Approach 1: Brute force (every substring)

### Code ([M02_LongestRepeatingCharacterReplacement.java:58-69](M02_LongestRepeatingCharacterReplacement.java#L58-L69))

```java
static int bruteForce(String s, int k) {
    int best = 0;
    for (int i = 0; i < s.length(); i++) {
        int[] c = new int[26];
        int mx = 0;
        for (int j = i; j < s.length(); j++) {
            mx = Math.max(mx, ++c[s.charAt(j) - 'A']);
            if (j - i + 1 - mx <= k) best = Math.max(best, j - i + 1);
        }
    }
    return best;
}
```

For each start i, extend j while keeping counts and the max frequency; a substring is valid when `length − mx ≤ k`. **O(n²) time.**

---

## 5. Approach 2: Sliding window, recomputing the max (the safe version)

```java
static int characterReplacementSafe(String s, int k) {
    int[] count = new int[26];
    int left = 0, best = 0;
    for (int right = 0; right < s.length(); right++) {
        count[s.charAt(right) - 'A']++;
        while ((right - left + 1) - maxOf(count) > k) {
            count[s.charAt(left) - 'A']--;
            left++;
        }
        best = Math.max(best, right - left + 1);
    }
    return best;
}
static int maxOf(int[] a) {
    int m = 0;
    for (int x : a) m = Math.max(m, x);
    return m;
}
```

It's always exact: the window is valid after shrinking. **O(26n) time.** Present this first if you're unsure about the stale-maxFreq argument.

---

## 6. Approach 3: Sliding window with a never-decreasing maxFreq (the file's solution)

### Code ([M02_LongestRepeatingCharacterReplacement.java:44-56](M02_LongestRepeatingCharacterReplacement.java#L44-L56))

```java
static int characterReplacement(String s, int k) {
    int[] count = new int[26];
    int left = 0, maxFreq = 0, best = 0;
    for (int right = 0; right < s.length(); right++) {
        maxFreq = Math.max(maxFreq, ++count[s.charAt(right) - 'A']);
        while ((right - left + 1) - maxFreq > k) {           // needs more than k changes
            count[s.charAt(left) - 'A']--;
            left++;
        }
        best = Math.max(best, right - left + 1);
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| `maxFreq = Math.max(maxFreq, ++count[...]);` | add the new letter; the most frequent count can only grow through the letter that just entered |
| `while ((right - left + 1) - maxFreq > k)` | the window needs more than k changes (according to `maxFreq`) |
| `count[s.charAt(left) - 'A']--; left++;` | shrink. `maxFreq` is deliberately **not** decreased |
| `best = Math.max(best, right - left + 1);` | record the window length |

(Because `maxFreq` never decreases, the `while` actually runs at most **once** per step. The window slides instead of shrinking, like the "non-shrinking window" trick in [Arrays M12](../../01-Arrays/03-Medium/M12_MaxConsecutiveOnesIII.md).)

### Dry run: `"AABABBA"`, k = 1

| right | char | maxFreq | window before shrink | needs (len − maxFreq) | shrink | window after | best |
|---|---|---|---|---|---|---|---|
| 0 | A | 1 | A | 0 | — | A | 1 |
| 1 | A | 2 | AA | 0 | — | AA | 2 |
| 2 | B | 2 | AAB | 1 | — | AAB | 3 |
| 3 | A | 3 | AABA | 1 | — | AABA | **4** |
| 4 | B | 3 | AABAB | 2 > 1 | drop A | ABAB | 4 |
| 5 | B | 3 | ABABB | 2 > 1 | drop A | BABB | 4 |
| 6 | A | 3 | BABBA | 2 > 1 | drop B | ABBA | 4 |

**Return 4** ✓. Notice that at right = 6, the window `ABBA` has a true max frequency of 2 (needs 2 changes), but the stale `maxFreq = 3` says 1. That's fine: its length 4 equals the best we've **already** truly achieved (`AABA`), so no wrong answer comes out.

### Dry run 2: `"ABAB"`, k = 2
maxFreq reaches 2; every window needs ≤ 2 changes; the window grows to 4 → **return 4** ✓

### Complexity
- **Time O(n)**, **Space O(26).**

---

## 7. Comparing the approaches

| | Brute | Window, recompute max | Window, stale max |
|---|---|---|---|
| Time | O(n²) | O(26n) | **O(n)** |
| Space | O(26) | O(26) | O(26) |
| Window always truly valid | — | **yes** | no (only the size matters) |

---

## 8. Edge cases and common mistakes

**Edge cases:** k = 0 (the longest run of one letter), k ≥ n (answer n), all the same letter, all different letters.

**Mistakes:**
1. Counting the **distinct** letters instead of `length − maxFreq`.
2. Assuming the window is truly valid in the stale version, and then reporting its contents as the substring (wrong). Use the safe version if you need the actual substring.
3. Using `c − 'a'` on uppercase input.

---

## 9. Interview questions and answers

**Q1. What's the validity condition?**
> `windowLength − maxFrequency ≤ k`: keep the most common letter and change the rest.

**Q2. Why can maxFreq stay stale?**
> The answer can only improve through a window whose max frequency exceeds every previous one, and then the formula uses that new, correct maximum. A stale value can only keep the window at a size we've already achieved, so the reported maximum is still correct.

**Q3. What's a simpler, fully correct alternative?**
> Recompute the max of the 26 counts in the while condition: O(26n), still linear.

### Follow-up questions

**F1. Max Consecutive Ones III (flip at most k zeros).** → [Arrays M12](../../01-Arrays/03-Medium/M12_MaxConsecutiveOnesIII.md)
> The special case with a two-letter alphabet, where the target letter is fixed to 1.

**F2. Return the substring.**
> Use the safe version, recording `left` and `right` on each improvement.

**F3. Any characters (not just A–Z).**
> Use `int[128]` or a map; the logic is the same.

**F4. Lowercase and uppercase must be treated as the same letter.**
> Normalise with `Character.toUpperCase` before counting.

---

## 10. Running the file

```bash
java M02_LongestRepeatingCharacterReplacement.java      # 5/5 tests passed (including 500 random checks)
```

**One-line summary:** *"A window is fixable if length − maxFreq ≤ k; slide it, and maxFreq never needs to decrease."*
