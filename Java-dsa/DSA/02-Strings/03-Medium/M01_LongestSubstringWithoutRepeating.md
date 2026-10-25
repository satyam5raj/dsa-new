# M01 · Longest Substring Without Repeating Characters

| | |
|---|---|
| **LeetCode** | [#3 Longest Substring Without Repeating Characters](https://leetcode.com/problems/longest-substring-without-repeating-characters/) |
| **Difficulty** | Medium |
| **Pattern** | Sliding window (variable size): counts + shrink, or "jump" with the last index |
| **Code** | [M01_LongestSubstringWithoutRepeating.java](M01_LongestSubstringWithoutRepeating.java) |
| **Run** | `java M01_LongestSubstringWithoutRepeating.java` |

---

## 1. The problem

Return the length of the longest **substring** (contiguous characters) whose characters are all **distinct**.

| s | answer | substring |
|---|---|---|
| `"abcabcbb"` | 3 | `"abc"` |
| `"bbbbb"` | 1 | `"b"` |
| `"pwwkew"` | 3 | `"wke"` (`"pwke"` is a subsequence, not a substring) |
| `""` | 0 | |
| `"abba"` | 2 | `"ab"` or `"ba"` |
| `"a b c"` | 3 | `"a b"` (spaces are characters too) |

**Constraints:** `0 ≤ n ≤ 5 × 10⁴`; English letters, digits, symbols and spaces.

This is probably **the** most asked sliding-window question.

---

## 2. How to think about it

1. **Brute force:** check every substring for uniqueness: O(n³), or O(n²) if you extend each start with a set.
2. **Sliding window:** keep a window `[left..right]` that **always** has unique characters.
   - Expand `right` by one character.
   - If that character is now **repeated** inside the window, shrink from the left until it isn't.
   - Record the window length.

   This is the "maximum-length" template from [Arrays M12](../../01-Arrays/03-Medium/M12_MaxConsecutiveOnesIII.md): the window is invalid when some count is > 1.
3. **Optimisation: jump instead of shrinking step by step.** Remember `lastIndex[c]`, where c was last seen. If `s[right] = c` was seen **inside** the window, the new window must start right after that occurrence:

   ```
   left = max(left, lastIndex[c] + 1)
   ```

   **Why `max`?** If c's last occurrence is **before** `left` (already outside the window), it's not a duplicate in the window, and `left` must **not move backwards**. In `"abba"`, at the last `a`: `lastIndex[a] = 0`, but `left` is already 2.

---

## 3. Java concepts used in this problem

### 3.1 `int[128]` indexed by a `char`

```java
int[] count = new int[128];   // one slot for every ASCII character code 0..127
count[c]++;                   // c is a char, automatically widened to an int index
```

The input can include digits, symbols and spaces, so `int[26]` with `c − 'a'` isn't enough; we index by the whole ASCII code. (Use `int[256]` for extended ASCII, or a `HashMap` for full Unicode.)

### 3.2 `Arrays.fill(array, value)`

```java
int[] lastIndex = new int[128];
Arrays.fill(lastIndex, -1);   // every slot = -1, meaning "never seen"
```

`new int[...]` starts with zeros, but 0 is a **valid index**, so we need a different "not seen" marker. `-1` works nicely, because `-1 + 1 = 0` keeps `left` at 0.

### 3.3 Substring vs subsequence

| | must be contiguous? | example in `"pwwkew"` |
|---|---|---|
| **substring** | **yes** | `"wke"` |
| **subsequence** | no (keeps the order, may skip) | `"pwke"` |

Read the problem statement carefully: they need completely different algorithms.

---

## 4. Approach 1: Brute force with a set

```java
static int lengthOfLongestSubstringBrute(String s) {
    int best = 0;
    for (int i = 0; i < s.length(); i++) {
        Set<Character> seen = new HashSet<>();
        for (int j = i; j < s.length(); j++) {
            if (!seen.add(s.charAt(j))) break;      // a repeat: the substrings starting at i end here
            best = Math.max(best, j - i + 1);
        }
    }
    return best;
}
```

From each start, extend until the first repeat. **O(n²) time** (n · alphabet at most), **O(alphabet) space**.

---

## 5. Approach 2: Sliding window with counts (the file's version 1)

### Code ([M01_LongestSubstringWithoutRepeating.java:44-54](M01_LongestSubstringWithoutRepeating.java#L44-L54))

```java
static int lengthOfLongestSubstringCounts(String s) {
    int[] count = new int[128];
    int left = 0, best = 0;
    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        count[c]++;
        while (count[c] > 1) count[s.charAt(left++)]--;   // shrink until c is unique again
        best = Math.max(best, right - left + 1);
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int[] count = new int[128];` | how many of each character are in the window |
| `count[c]++;` | **expand**: c enters |
| `while (count[c] > 1) count[s.charAt(left++)]--;` | c is duplicated → remove characters from the left until its earlier copy is gone. Only c can be duplicated, because the window was unique before c arrived |
| `best = Math.max(best, right - left + 1);` | the window is unique again; record it |

### Dry run: `"abcabcbb"`

| right | c | dropped from the left | window | best |
|---|---|---|---|---|
| 0 | a | — | a | 1 |
| 1 | b | — | ab | 2 |
| 2 | c | — | abc | 3 |
| 3 | a | a | bca | 3 |
| 4 | b | b | cab | 3 |
| 5 | c | c | abc | 3 |
| 6 | b | a, b | cb | 3 |
| 7 | b | c, b | b | 3 |

**Return 3** ✓

**O(n) time** (each character enters and leaves once), **O(128) space**.

---

## 6. Approach 3: Jump with the last index (the file's main version)

### Code ([M01_LongestSubstringWithoutRepeating.java:57-68](M01_LongestSubstringWithoutRepeating.java#L57-L68))

```java
static int lengthOfLongestSubstring(String s) {
    int[] lastIndex = new int[128];
    Arrays.fill(lastIndex, -1);
    int left = 0, best = 0;
    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        left = Math.max(left, lastIndex[c] + 1);           // never move left backwards
        lastIndex[c] = right;
        best = Math.max(best, right - left + 1);
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| `Arrays.fill(lastIndex, -1);` | no character has been seen yet |
| `left = Math.max(left, lastIndex[c] + 1);` | if c's previous occurrence is inside the window, start the window right after it; otherwise keep `left` |
| `lastIndex[c] = right;` | remember c's newest position (**after** using the old one) |
| `best = Math.max(best, right - left + 1);` | the window `[left, right]` is unique |

### Dry run 1: `"pwwkew"`

| right | c | lastIndex[c] | left: max(old, last + 1) | window | best |
|---|---|---|---|---|---|
| 0 | p | −1 | max(0, 0) = 0 | p | 1 |
| 1 | w | −1 | 0 | pw | 2 |
| 2 | w | 1 | max(0, 2) = **2** | w | 2 |
| 3 | k | −1 | 2 | wk | 2 |
| 4 | e | −1 | 2 | wke | **3** |
| 5 | w | 2 | max(2, 3) = **3** | kew | 3 |

**Return 3** ✓

### Dry run 2: `"abba"` (why `max` is needed)

| right | c | lastIndex[c] | left | window | best |
|---|---|---|---|---|---|
| 0 | a | −1 | 0 | a | 1 |
| 1 | b | −1 | 0 | ab | 2 |
| 2 | b | 1 | max(0, 2) = 2 | b | 2 |
| 3 | a | **0** | max(2, **1**) = **2** | ba | 2 |

Without `max`, the last step would set `left = 1` and count `"bba"` (length 3, with a repeated b): **wrong**. With `max`: **return 2** ✓

### Complexity
- **Time O(n):** one pass, with no inner loop at all.
- **Space O(128).**

---

## 7. Comparing the approaches

| | Brute + set | Window + counts | Jump with last index |
|---|---|---|---|
| Time | O(n²) | O(n) (≤ 2n steps) | **O(n) (n steps)** |
| Space | O(alphabet) | O(128) | O(128) |
| Easiest to adapt (e.g. "at most 2 repeats") | — | **yes** | no |

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty string, all the same character, all distinct, spaces and symbols, `"abba"` (a stale last index).

**Mistakes:**
1. Forgetting `max` in the jump version, which moves `left` backwards (`"abba"`).
2. Using `int[26]`, which crashes on spaces, digits and uppercase.
3. Updating `lastIndex[c]` **before** reading the old value.
4. Initialising `lastIndex` with 0 instead of −1, which wrongly jumps `left` to 1 for a character seen "at index 0".

---

## 9. Interview questions and answers

**Q1. Walk me through the sliding window.**
> The window always holds unique characters. Each new character is added; if it duplicates something inside the window, I move the left edge just past the earlier copy. The answer is the largest window seen.

**Q2. Why `max(left, lastIndex[c] + 1)`?**
> The last occurrence might be outside the current window (before `left`). Then it isn't a duplicate, and moving `left` back would re-include characters we already dropped.

**Q3. What if the input is full Unicode?**
> Replace the array with a `HashMap<Character, Integer>` (or iterate over code points for characters outside the BMP).

### Follow-up questions

**F1. Return the substring itself.**
> Track `bestStart` when `best` improves; return `s.substring(bestStart, bestStart + best)`.

**F2. At most k distinct characters (LC 340).** → [M04](M04_LongestSubstringAtMostKDistinct.md)
> A count map and a "distinct" counter; shrink while distinct > k.

**F3. Each character may appear at most twice.**
> The counts version, with the condition `count[c] > 2`.

**F4. Longest substring with all characters appearing at least k times (LC 395).**
> Divide and conquer on characters with fewer than k occurrences, or a sliding window for each possible number of unique characters (1..26).

---

## 10. Running the file

```bash
java M01_LongestSubstringWithoutRepeating.java      # 7/7 tests passed
```

**One-line summary:** *"Keep a unique window: left = max(left, lastIndex[c] + 1), then record right − left + 1."*
