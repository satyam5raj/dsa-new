# M04 · Longest Substring with At Most K Distinct Characters

| | |
|---|---|
| **LeetCode** | [#340 Longest Substring with At Most K Distinct Characters](https://leetcode.com/problems/longest-substring-with-at-most-k-distinct-characters/) (Premium; #159 is the k = 2 version) |
| **Difficulty** | Medium |
| **Pattern** | Sliding window + counts + a "distinct" counter |
| **Code** | [M04_LongestSubstringAtMostKDistinct.java](M04_LongestSubstringAtMostKDistinct.java) |
| **Run** | `java M04_LongestSubstringAtMostKDistinct.java` |

---

## 1. The problem

Return the length of the longest substring with **at most k distinct** characters.

| s | k | answer | substring |
|---|---|---|---|
| `"eceba"` | 2 | 3 | `"ece"` |
| `"aa"` | 1 | 2 | `"aa"` |
| `"abcadcacacaca"` | 3 | 11 | `"cadcacacaca"` |
| `"abc"` | 0 | 0 | no character is allowed |
| `"abc"` | 5 | 3 | the whole string |

**Constraints:** `1 ≤ n ≤ 5 × 10⁴`, `0 ≤ k ≤ 50`.

---

## 2. How to think about it

This is [Arrays M17 Fruit Into Baskets](../../01-Arrays/03-Medium/M17_FruitIntoBaskets.md) on characters, with any k instead of 2. It's the maximum-length sliding window:

```
for right:  add s[right]
    while distinct > k:  remove s[left], left++
    best = max(best, right − left + 1)
```

Instead of a `HashMap` and `map.size()`, the file uses an **`int[128]` plus a `distinct` counter**:
- adding c: if `count[c]` **was** 0 → a new character entered → `distinct++`
- removing c: if `count[c]` **becomes** 0 → the character completely left → `distinct--`

That avoids the HashMap overhead and the classic "forgot to remove the zero key" bug.

---

## 3. Java concepts used in this problem

### 3.1 Post-increment in a test: `count[c]++ == 0`

```java
if (count[s.charAt(right)]++ == 0) distinct++;
```

The comparison uses the **old** value (before the increment): "was it absent before this character arrived?"

### 3.2 Pre-decrement in a test: `--count[c] == 0`

```java
if (--count[s.charAt(left)] == 0) distinct--;
```

The comparison uses the **new** value (after the decrement): "is it absent now?" Choosing between `x++` and `++x` (or `x--` and `--x`) changes which value is tested. That's a classic source of off-by-one bugs, so read these lines slowly.

### 3.3 `int[128]` vs `HashMap`

| | `int[128]` + counter | `HashMap<Character,Integer>` |
|---|---|---|
| speed | very fast (array indexing) | hashing + boxing |
| distinct count | maintained manually | `map.size()` (only if zero keys are removed!) |
| alphabet | ASCII only | anything |

---

## 4. Approach 1: Brute force

```java
static int longestKDistinctBrute(String s, int k) {
    int best = 0;
    for (int i = 0; i < s.length(); i++) {
        Set<Character> seen = new HashSet<>();
        for (int j = i; j < s.length(); j++) {
            seen.add(s.charAt(j));
            if (seen.size() > k) break;           // longer substrings from i only add characters
            best = Math.max(best, j - i + 1);
        }
    }
    return best;
}
```

**O(n²) time.** The `break` is valid because extending a substring can never reduce its number of distinct characters.

---

## 5. Approach 2: Sliding window with a count array (the file's solution)

### Code ([M04_LongestSubstringAtMostKDistinct.java:42-54](M04_LongestSubstringAtMostKDistinct.java#L42-L54))

```java
static int lengthOfLongestSubstringKDistinct(String s, int k) {
    int[] count = new int[128];
    int left = 0, distinct = 0, best = 0;
    for (int right = 0; right < s.length(); right++) {
        if (count[s.charAt(right)]++ == 0) distinct++;       // a new character entered
        while (distinct > k) {
            if (--count[s.charAt(left)] == 0) distinct--;    // a character fully left
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
| `int[] count = new int[128];` | per-character counts in the window |
| `if (count[c]++ == 0) distinct++;` | **expand**; a new character means one more distinct |
| `while (distinct > k)` | too many distinct characters |
| `if (--count[left char] == 0) distinct--; left++;` | **shrink**; when the last copy of a character leaves, there's one fewer distinct |
| `best = Math.max(best, right - left + 1);` | the window is valid → record it |

### Dry run 1: `"eceba"`, k = 2

| right | char | dropped from the left | window | distinct | best |
|---|---|---|---|---|---|
| 0 | e | — | e | 1 | 1 |
| 1 | c | — | ec | 2 | 2 |
| 2 | e | — | ece | 2 | **3** |
| 3 | b | e, c (only when c leaves does distinct drop) | eb | 2 | 3 |
| 4 | a | e | ba | 2 | 3 |

**Return 3** ✓. At right = 3, dropping the first `e` doesn't reduce `distinct` (another `e` is still inside); only dropping `c` does.

### Dry run 2: `"abc"`, k = 0
Each added character makes distinct = 1 > 0, so the window shrinks to empty every time; the length is always 0 → **return 0** ✓

### Complexity
- **Time O(n):** each character enters and leaves at most once.
- **Space O(128).**

---

## 6. Comparing the approaches

| | Brute | Window + array |
|---|---|---|
| Time | O(n²) | **O(n)** |
| Space | O(k) | O(128) |

---

## 7. Edge cases and common mistakes

**Edge cases:** k = 0, k ≥ the number of distinct characters (the whole string), a single character, all characters identical.

**Mistakes:**
1. Mixing up `count[c]++ == 0` and `++count[c] == 1`. Both are correct, but `count[c]++ == 1` would be wrong.
2. Decrementing `distinct` whenever **any** character leaves.
3. With a HashMap, forgetting to remove keys at 0.

---

## 8. Interview questions and answers

**Q1. How do you track the number of distinct characters in O(1)?**
> Increment the counter when a character's count goes 0 → 1, and decrement it when the count goes 1 → 0.

**Q2. What's the relationship with "exactly k distinct"?**
> `exactly(k) = atMost(k) − atMost(k−1)` when **counting** substrings ([Arrays H05](../../01-Arrays/04-Hard/H05_SubarraysWithKDifferentIntegers.md)). For the **longest** with exactly k, run this window and only record when `distinct == k`.

### Follow-up questions

**F1. The input is a stream (you can't store the whole string).**
> You still need the window's characters to know what leaves. Store the **last index** of each character in a `LinkedHashMap` (ordered by last occurrence); when distinct > k, remove the entry with the smallest last index and set `left` just after it. Memory O(k).

**F2. Longest substring with at most 2 distinct characters (LC 159).**
> Call this with k = 2.

**F3. Count the substrings with at most k distinct characters.**
> Replace `best = max(...)` with `total += right − left + 1`.

---

## 9. Running the file

```bash
java M04_LongestSubstringAtMostKDistinct.java      # 5/5 tests passed
```

**One-line summary:** *"Window with counts and a distinct counter (0→1 adds, 1→0 removes); shrink while distinct > k."*
