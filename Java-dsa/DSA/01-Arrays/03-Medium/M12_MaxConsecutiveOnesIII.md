# M12 · Max Consecutive Ones III

| | |
|---|---|
| **LeetCode** | [#1004 Max Consecutive Ones III](https://leetcode.com/problems/max-consecutive-ones-iii/) |
| **Difficulty** | Medium |
| **Pattern** | Sliding window (the longest window with at most k "bad" elements) |
| **Code** | [M12_MaxConsecutiveOnesIII.java](M12_MaxConsecutiveOnesIII.java) |
| **Run** | `java M12_MaxConsecutiveOnesIII.java` |

---

## 1. The problem

A binary array (0s and 1s). You may **flip at most `k` zeros** to ones. Return the length of the **longest run of 1s** you can get.

| nums | k | answer | how |
|---|---|---|---|
| `[1,1,1,0,0,0,1,1,1,1,0]` | 2 | 6 | flip the zeros at 4, 5 → `0,0,1,1,1,1` becomes six 1s (indices 4..9) |
| `[0,0,1,1,0,0,1,1,1,0,1,1,0,0,0,1,1,1,1]` | 3 | 10 | |
| `[1, 1, 0, 1]` | 0 | 2 | no flips allowed |
| `[0, 0, 0]` | 2 | 2 | flip two zeros |

**Constraints:** `1 ≤ n ≤ 10⁵`, `0 ≤ k ≤ n`.

---

## 2. How to think about it

### Reframe (the key step!)
"Flip at most k zeros and find the longest run of 1s" is the same as:

> **Find the longest window (contiguous subarray) that contains at most k zeros.**

Any such window can become all 1s by flipping its zeros. That turns a vague "which zeros should I flip?" search into a clean sliding window.

### The template: the maximum-length version, "shrink while INVALID"

```
for right:
    if nums[right] == 0: zeros++               // expand
    while zeros > k:                           // invalid → shrink
        if nums[left] == 0: zeros--
        left++
    best = max(best, right - left + 1)         // here the window is valid
```

Compare it with [M11](M11_MinimumSizeSubarraySum.md):

| | M11 (minimum length) | M12 (maximum length) |
|---|---|---|
| shrink while | the window is **valid** | the window is **invalid** |
| record the answer | **inside** the shrink loop | **after** the shrink loop |

Know both forms by heart. Almost every sliding-window problem is one of them.

---

## 3. Java concepts used in this problem

### 3.1 The window state is a counter

The window doesn't need to remember **which** zeros it contains, only **how many** (`zeros`). Picking the smallest state that lets you check validity in O(1) is the main design decision in sliding-window problems. (In string problems, the state is often an `int[26]` of character counts.)

### 3.2 `if` inside `while`

```java
while (zeros > k) {
    if (nums[left] == 0) zeros--;   // only zeros leaving the window change the count
    left++;
}
```

Every left element leaves the window, but only zeros affect the state.

---

## 4. Approach 1: Brute force

```java
static int longestOnesBrute(int[] nums, int k) {
    int best = 0;
    for (int i = 0; i < nums.length; i++) {
        int zeros = 0;
        for (int j = i; j < nums.length; j++) {
            if (nums[j] == 0) zeros++;
            if (zeros > k) break;                // extending further can't fix it
            best = Math.max(best, j - i + 1);
        }
    }
    return best;
}
```

**O(n²) time.** It uses the same reframing, but restarts the count for every start index.

---

## 5. Approach 2: Sliding window (the file's solution)

### Code ([M12_MaxConsecutiveOnesIII.java:45-56](M12_MaxConsecutiveOnesIII.java#L45-L56))

```java
static int longestOnes(int[] nums, int k) {
    int left = 0, zeros = 0, best = 0;
    for (int right = 0; right < nums.length; right++) {
        if (nums[right] == 0) zeros++;
        while (zeros > k) {
            if (nums[left] == 0) zeros--;
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
| `int left = 0, zeros = 0, best = 0;` | an empty window |
| `for (int right = 0; ...)` | grow the window by one element on the right |
| `if (nums[right] == 0) zeros++;` | update the state for the new element |
| `while (zeros > k)` | too many zeros → the window is invalid |
| `if (nums[left] == 0) zeros--; left++;` | drop the leftmost element; if it was a zero, the count goes down |
| `best = Math.max(best, right - left + 1);` | the window `[left, right]` is now valid; record its length |

### Dry run 1: `[1,1,1,0,0,0,1,1,1,1,0]`, k = 2

| right | value | zeros after adding | shrinking | left | window length | best |
|---|---|---|---|---|---|---|
| 0 | 1 | 0 | — | 0 | 1 | 1 |
| 1 | 1 | 0 | — | 0 | 2 | 2 |
| 2 | 1 | 0 | — | 0 | 3 | 3 |
| 3 | 0 | 1 | — | 0 | 4 | 4 |
| 4 | 0 | 2 | — | 0 | 5 | 5 |
| 5 | 0 | 3 → invalid | drop idx 0,1,2 (ones), drop idx 3 (zero → 2) | 4 | 2 | 5 |
| 6 | 1 | 2 | — | 4 | 3 | 5 |
| 7 | 1 | 2 | — | 4 | 4 | 5 |
| 8 | 1 | 2 | — | 4 | 5 | 5 |
| 9 | 1 | 2 | — | 4 | **6** | **6** |
| 10 | 0 | 3 → invalid | drop idx 4 (zero → 2) | 5 | 6 | 6 |

**Return 6** ✓ (the window [4..9] = `0,0,1,1,1,1`, with both zeros flipped).

### Dry run 2: `[1, 1, 0, 1]`, k = 0

| right | value | zeros | shrinking | left | length | best |
|---|---|---|---|---|---|---|
| 0 | 1 | 0 | | 0 | 1 | 1 |
| 1 | 1 | 0 | | 0 | 2 | 2 |
| 2 | 0 | 1 → invalid | drop 1, drop 1, drop 0 (→ 0) | 3 | 0 | 2 |
| 3 | 1 | 0 | | 3 | 1 | 2 |

**Return 2** ✓. Note that at right = 2 the window becomes **empty** (left = 3 > right = 2, length 0). That's fine.

### Dry run 3: `[0, 0, 0]`, k = 2
right = 0, 1 → zeros 1, 2, lengths 1, 2. right = 2 → zeros 3, drop idx 0 → zeros 2, left = 1, length 2. **Return 2** ✓

### Complexity
- **Time O(n)** (amortised; `left` moves at most n times in total).
- **Space O(1).**

---

## 6. (Bonus) The "non-shrinking window" trick

You never need the window to get **smaller** than the best size found so far. Replace `while` with `if`, so the window **slides** (left and right move together) instead of shrinking:

```java
static int longestOnesNoShrink(int[] nums, int k) {
    int left = 0, zeros = 0, right;
    for (right = 0; right < nums.length; right++) {
        if (nums[right] == 0) zeros++;
        if (zeros > k) {                       // slide: drop exactly one from the left
            if (nums[left] == 0) zeros--;
            left++;
        }
    }
    return right - left;                       // the window size never decreased
}
```

The window's size only grows when a bigger valid window appears, and otherwise stays the same. The final size is the answer. It's a neat optimisation, but explain the `while` version first, because it's easier to justify.

---

## 7. Comparing the approaches

| | Brute force | Sliding window | Non-shrinking window |
|---|---|---|---|
| Time | O(n²) | **O(n)** | O(n) |
| Space | O(1) | O(1) | O(1) |
| Window is always valid? | — | yes | no (only its **size** matters) |

---

## 8. Edge cases and common mistakes

**Edge cases:** k = 0 (plain max consecutive ones, [E22](../02-Easy/E22_MaxConsecutiveOnes.md)), k ≥ the number of zeros (the answer is n), all zeros, all ones.

**Mistakes:**
1. Recording `best` **before** shrinking, which records an invalid window.
2. Decrementing `zeros` for every element leaving the window (only zeros should).
3. Trying to decide **which** zeros to flip greedily, without the window reframing.

---

## 9. Interview questions and answers

**Q1. What's the reframing that makes this easy?**
> Flipping at most k zeros is possible for a window exactly when it contains at most k zeros. So the answer is the longest subarray with at most k zeros.

**Q2. Why record the answer after the while loop and not inside it?**
> After shrinking, the window is guaranteed valid (zeros ≤ k), and for this right edge it's the longest valid window. Inside the loop the window is still invalid.

**Q3. Contrast the minimum-length and maximum-length templates.**
> Minimum: shrink while the window is valid, recording inside the loop. Maximum: shrink while it's invalid, recording after the loop.

### Follow-up questions

**F1. Longest Repeating Character Replacement (LC 424): replace at most k characters so that the window is a single repeated letter.** → [02-Strings M02](../../02-Strings/03-Medium/M02_LongestRepeatingCharacterReplacement.md)
> The window is valid when `length − maxFreq ≤ k`. The same "maximum" template, with a `count[26]` state.

**F2. Longest substring with at most k distinct characters (LC 340).** → [02-Strings M04](../../02-Strings/03-Medium/M04_LongestSubstringAtMostKDistinct.md)
> The state is a count map; it's invalid when `map.size() > k`.

**F3. Fruit Into Baskets (LC 904): at most 2 distinct values.** → [M17](M17_FruitIntoBaskets.md)

**F4. Flips arrive as a stream, or k changes per query.**
> For many k values, precompute the positions of the zeros. For a given k, the best window spans from just after zero i−1 to just before zero i+k (for each i). That's O(z) per query.

**F5. Return the actual range or which positions to flip.**
> Store `left` and `right` when `best` improves; the zeros inside that range are the ones to flip.

---

## 10. Running the file

```bash
java M12_MaxConsecutiveOnesIII.java      # 4/4 tests passed
```

**One-line summary:** *"The longest window with at most k zeros: expand, shrink while zeros > k, then record."*
