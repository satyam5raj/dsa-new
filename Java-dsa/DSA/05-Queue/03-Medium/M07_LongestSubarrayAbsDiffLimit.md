# M07 · Longest Continuous Subarray With Absolute Diff ≤ Limit

| | |
|---|---|
| **LeetCode** | [#1438 Longest Continuous Subarray With Absolute Diff Less Than or Equal to Limit](https://leetcode.com/problems/longest-continuous-subarray-with-absolute-diff-less-than-or-equal-to-limit/) |
| **Difficulty** | Medium |
| **Pattern** | Sliding window + two monotonic deques (the window max **and** the window min) |
| **Code** | [M07_LongestSubarrayAbsDiffLimit.java](M07_LongestSubarrayAbsDiffLimit.java) |
| **Run** | `java M07_LongestSubarrayAbsDiffLimit.java` |

---

## 1. The problem

Find the longest subarray in which `|a − b| ≤ limit` for **every pair** of its elements.

| nums | limit | answer | subarray |
|---|---|---|---|
| `[8, 2, 4, 7]` | 4 | 2 | `[2,4]` or `[4,7]` |
| `[10, 1, 2, 4, 7, 2]` | 5 | 4 | `[2,4,7,2]` |
| `[4,2,2,2,4,4,2,2]` | 0 | 3 | `[2,2,2]` |

**Constraints:** `1 ≤ n ≤ 10⁵`.

---

## 2. How to think about it

### Reframe
"Every pair differs by at most `limit`" ⇔ **`max(window) − min(window) ≤ limit`**. So we want the longest window whose max − min ≤ limit: a variable sliding window (the "maximum length" template from [Arrays M12](../../01-Arrays/03-Medium/M12_MaxConsecutiveOnesIII.md)).

The hard part: knowing the window's **max and min** quickly while elements leave from the **left**.

### Two monotonic deques (storing values)
- `maxDq`: values **decreasing** → its front is the window **max**
- `minDq`: values **increasing** → its front is the window **min**

```
expand with x:   pop from maxDq's back while back < x;   push x
                 pop from minDq's back while back > x;   push x
while maxDq.front − minDq.front > limit:      shrink
    for the leaving element nums[left]: if it equals a deque's front, poll that front
    left++
best = max(best, right − left + 1)
```

**Keep equal values** in the deques (pop only those strictly smaller/larger), so that when one copy leaves, its duplicates are still represented.

### Alternative
A `TreeMap<value, count>` used as a multiset gives the max/min in O(log n), for O(n log n) overall.

---

## 3. Java concepts used in this problem

### 3.1 Storing values versus indices in a monotonic deque

Here the deques store **values**, and expiry is detected by comparing `nums[left]` with the front. That works **only** because equal values are kept (the `<` / `>` pops are strict): every element still "represented" in a deque appears there as its own copy. Storing indices would also work, and it's the more general option.

### 3.2 Unboxing in the comparisons

```java
if (nums[left] == maxDq.peekFirst()) maxDq.pollFirst();
```

`int == Integer` unboxes the Integer, so it's a value comparison. Comparing two `Integer`s with `==` would be a bug.

### 3.3 Three nested conditions in one loop

The window logic (shrink while invalid) is combined with deque maintenance on both expand and shrink. Each element enters each deque once and leaves once, so it's still O(n).

---

## 4. Approach 1: Brute force

### Code ([M07_LongestSubarrayAbsDiffLimit.java:68-79](M07_LongestSubarrayAbsDiffLimit.java#L68-L79))

```java
static int brute(int[] a, int limit) {
    int best = 0;
    for (int i = 0; i < a.length; i++) {
        int mx = a[i], mn = a[i];
        for (int j = i; j < a.length; j++) {
            mx = Math.max(mx, a[j]);
            mn = Math.min(mn, a[j]);
            if (mx - mn <= limit) best = Math.max(best, j - i + 1);
        }
    }
    return best;
}
```

Every start, extending with a running max/min: **O(n²).** (A `break` once `mx − mn > limit` would be valid too, because the spread only grows.)

---

## 5. Approach 2: TreeMap multiset, O(n log n)

```java
static int longestSubarrayTreeMap(int[] nums, int limit) {
    TreeMap<Integer, Integer> window = new TreeMap<>();     // value → count
    int left = 0, best = 0;
    for (int right = 0; right < nums.length; right++) {
        window.merge(nums[right], 1, Integer::sum);
        while (window.lastKey() - window.firstKey() > limit) {
            if (window.merge(nums[left], -1, Integer::sum) == 0) window.remove(nums[left]);
            left++;
        }
        best = Math.max(best, right - left + 1);
    }
    return best;
}
```

`firstKey()` / `lastKey()` give the window's min and max in O(log n). It's simpler to get right than two deques.

---

## 6. Approach 3: Two monotonic deques (the file's solution)

### Code ([M07_LongestSubarrayAbsDiffLimit.java:48-66](M07_LongestSubarrayAbsDiffLimit.java#L48-L66))

```java
static int longestSubarray(int[] nums, int limit) {
    Deque<Integer> maxDq = new ArrayDeque<>(), minDq = new ArrayDeque<>();
    int left = 0, best = 0;
    for (int right = 0; right < nums.length; right++) {
        int x = nums[right];
        while (!maxDq.isEmpty() && maxDq.peekLast() < x) maxDq.pollLast();
        maxDq.offerLast(x);
        while (!minDq.isEmpty() && minDq.peekLast() > x) minDq.pollLast();
        minDq.offerLast(x);

        while (maxDq.peekFirst() - minDq.peekFirst() > limit) {
            if (nums[left] == maxDq.peekFirst()) maxDq.pollFirst();
            if (nums[left] == minDq.peekFirst()) minDq.pollFirst();
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
| maxDq maintenance | remove the smaller values from the back (they can't be the max while x is in the window); keep the equal ones |
| minDq maintenance | the mirror image |
| `while (max − min > limit)` | the window is invalid → shrink |
| `if (nums[left] == maxDq.peekFirst()) maxDq.pollFirst();` | if the leaving element is the current max (copy), drop it from maxDq |
| the same for minDq | |
| `best = Math.max(best, right − left + 1)` | the window is valid → record its length |

### Dry run: `[10, 1, 2, 4, 7, 2]`, limit 5

| right | x | maxDq | minDq | shrink | left | length | best |
|---|---|---|---|---|---|---|---|
| 0 | 10 | [10] | [10] | — | 0 | 1 | 1 |
| 1 | 1 | [10, 1] → after the drop [1] | [1] | 10 − 1 = 9 > 5 → drop 10 (the max front) | 1 | 1 | 1 |
| 2 | 2 | [2] | [1, 2] | — | 1 | 2 | 2 |
| 3 | 4 | [4] | [1, 2, 4] | — | 1 | 3 | 3 |
| 4 | 7 | [7] | [1, 2, 4, 7] → after the drop [2, 4, 7] | 7 − 1 = 6 > 5 → drop 1 (the min front) | 2 | 3 | 3 |
| 5 | 2 | [7, 2] | [2, 2] | 7 − 2 = 5 ok | 2 | **4** | **4** |

**Return 4** (`[2, 4, 7, 2]`) ✓. In the last row, minDq keeps **both** 2s, thanks to the strict `>` pop.

### Complexity
- **Time O(n):** each element enters and leaves each deque at most once.
- **Space O(n).**

---

## 7. Comparing the approaches

| | Brute force | TreeMap multiset | Two monotonic deques |
|---|---|---|---|
| Time | O(n²) | O(n log n) | **O(n)** |
| Space | O(1) | O(n) | O(n) |
| Bug risk | low | low | medium (duplicates) |

---

## 8. Edge cases and common mistakes

**Edge cases:** limit = 0 (runs of equal values), all equal, strictly increasing, a single element.

**Mistakes:**
1. Popping **equal** values from the deques (then a duplicate leaving the window removes the only copy, and the max/min are wrong).
2. Comparing two `Integer` objects with `==`.
3. Shrinking with `if` instead of `while`.
4. Tracking only the max or only the min.

---

## 9. Interview questions and answers

**Q1. Why two deques?**
> The validity condition needs both the window max and the window min. Each monotonic deque maintains one of them in O(1) amortised as the window slides.

**Q2. Why keep the duplicates?**
> If equal values were collapsed, removing the leftmost copy would empty the front even though another copy is still inside the window, giving a wrong max/min.

### Follow-up questions

**F1. Count the subarrays with max − min ≤ limit.**
> The same window; add `right − left + 1` per right end (the counting trick from [Arrays M14](../../01-Arrays/03-Medium/M14_SubarrayProductLessThanK.md)).

**F2. Sliding Window Maximum.** → [Arrays H02](../../01-Arrays/04-Hard/H02_SlidingWindowMaximum.md)
> A single decreasing deque for a fixed-size window.

**F3. Continuous Subarrays (LC 2762): count the subarrays with max − min ≤ 2.**
> Exactly F1 with limit 2.

---

## 10. Running the file

```bash
java M07_LongestSubarrayAbsDiffLimit.java      # 4/4 tests passed (including 500 random arrays with duplicates)
```

**One-line summary:** *"Window max − min ≤ limit: a decreasing deque for the max, an increasing one for the min (keep the equal values); shrink while invalid."*
