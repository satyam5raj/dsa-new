# M11 · Minimum Size Subarray Sum

| | |
|---|---|
| **LeetCode** | [#209 Minimum Size Subarray Sum](https://leetcode.com/problems/minimum-size-subarray-sum/) |
| **Difficulty** | Medium |
| **Pattern** | Sliding window (variable size, shrink while valid) |
| **Code** | [M11_MinimumSizeSubarraySum.java](M11_MinimumSizeSubarraySum.java) |
| **Run** | `java M11_MinimumSizeSubarraySum.java` |

---

## 1. The problem

All numbers are **positive**. Return the **minimal length** of a contiguous subarray whose sum is **≥ target**, or `0` if there's none.

| target | nums | answer | subarray |
|---|---|---|---|
| 7 | `[2, 3, 1, 2, 4, 3]` | 2 | `[4, 3]` |
| 4 | `[1, 4, 4]` | 1 | `[4]` |
| 11 | `[1, 1, 1, 1, 1]` | 0 | impossible (the total is 5) |
| 15 | `[1, 2, 3, 4, 5]` | 5 | the whole array |

**Constraints:** `1 ≤ n ≤ 10⁵`, `1 ≤ nums[i] ≤ 10⁴`, `1 ≤ target ≤ 10⁹`. Follow-up: *if you have O(n), try O(n log n)* (the binary search version).

---

## 2. How to think about it

1. **Brute force:** every start index, extend the end until the sum ≥ target: O(n²).
2. **Why a sliding window works here:** all numbers are **positive**, so
   - adding an element on the right **always increases** the sum
   - removing an element on the left **always decreases** it

   The sum is **monotonic** in the window's edges, so we always know which way to move: too small → expand right; big enough → record it and try to shrink from the left.
3. **Template (minimum-length version):**

```
for right in 0..n-1:
    sum += nums[right]                    // EXPAND
    while sum >= target:                  // the window is VALID
        best = min(best, right - left + 1)  //   record it
        sum -= nums[left++]               //   SHRINK to look for a shorter one
```

With negatives this breaks (see [M07](M07_SubarraySumEqualsK.md)). The negative version of this problem needs prefix sums + a monotonic deque ([H04](../04-Hard/H04_ShortestSubarrayWithSumAtLeastK.md)).

---

## 3. Java concepts used in this problem

### 3.1 `Integer.MAX_VALUE` as "not found yet", and a ternary for the result

```java
int best = Integer.MAX_VALUE;                       // "infinity": any real length is smaller
...
return best == Integer.MAX_VALUE ? 0 : best;        // never updated → no valid window → 0
```

A sentinel value plus a final check is a common way to handle "maybe there's no answer".

### 3.2 `sum -= nums[left++];`

Uses the **current** `left` (removes that element from the sum), **then** moves `left` forward. It's the same as `sum -= nums[left]; left++;`.

### 3.3 Window length: `right - left + 1`

For an **inclusive** window `[left, right]`, the number of elements is `right − left + 1`. With left = 2 and right = 4 that's 3 elements (2, 3, 4).

### 3.4 Amortised analysis: why a `while` inside a `for` is still O(n)

It looks like nested loops (O(n²)), but `left` **only moves forward** and never past `right`. Across the **whole** run, the inner `while` executes at most n times **in total**. Each element is added once (by `right`) and removed at most once (by `left`), so the total is about 2n steps = **O(n)**. This "**amortised**" argument applies to every sliding window.

### 3.5 `Arrays.binarySearch` on prefix sums (approach 2)

Prefix sums of positive numbers are **strictly increasing**, so they can be binary searched. `Arrays.binarySearch` returns `-(insertionPoint) − 1` when the key is missing. The insertion point is the index of the first element **greater** than the key. See [M03](M03_TwoSumII_SortedInput.md).

---

## 4. Approach 1: Brute force

```java
static int minSubArrayLenBrute(int target, int[] nums) {
    int best = Integer.MAX_VALUE;
    for (int i = 0; i < nums.length; i++) {
        int sum = 0;
        for (int j = i; j < nums.length; j++) {
            sum += nums[j];
            if (sum >= target) { best = Math.min(best, j - i + 1); break; }   // longer ends can't be shorter
        }
    }
    return best == Integer.MAX_VALUE ? 0 : best;
}
```

The `break` is valid **because** the values are positive: once the window starting at i is valid, extending it only makes it longer. **O(n²) worst case.**

---

## 5. Approach 2: Prefix sums + binary search, O(n log n)

```java
static int minSubArrayLenBinary(int target, int[] nums) {
    int n = nums.length;
    long[] prefix = new long[n + 1];                     // prefix[i] = sum of the first i elements
    for (int i = 0; i < n; i++) prefix[i + 1] = prefix[i] + nums[i];
    int best = Integer.MAX_VALUE;
    for (int i = 0; i < n; i++) {
        long need = prefix[i] + target;                  // we want prefix[j] >= need, with j as small as possible
        int j = Arrays.binarySearch(prefix, need);
        if (j < 0) j = -j - 1;                           // not found → insertion point = first index with a value > need
        if (j <= n) best = Math.min(best, j - i);
    }
    return best == Integer.MAX_VALUE ? 0 : best;
}
```

- The subarray `nums[i..j−1]` has sum `prefix[j] − prefix[i]`. We want the smallest j with `prefix[j] ≥ prefix[i] + target`.
- Since the values are positive, the prefix sums are strictly increasing, so binary search finds that j in O(log n).
- `long` is used because target + prefix can reach about 10⁹ + 10⁹.
- **O(n log n) time, O(n) space.** This is the follow-up LeetCode asks for.

---

## 6. Approach 3: Sliding window (the file's solution)

### Code ([M11_MinimumSizeSubarraySum.java:48-58](M11_MinimumSizeSubarraySum.java#L48-L58))

```java
static int minSubArrayLen(int target, int[] nums) {
    int left = 0, sum = 0, best = Integer.MAX_VALUE;
    for (int right = 0; right < nums.length; right++) {
        sum += nums[right];
        while (sum >= target) {
            best = Math.min(best, right - left + 1);
            sum -= nums[left++];
        }
    }
    return best == Integer.MAX_VALUE ? 0 : best;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int left = 0, sum = 0, best = Integer.MAX_VALUE;` | an empty window; no answer yet |
| `for (int right = 0; ...)` | `right` is the window's right edge; each step adds one element |
| `sum += nums[right];` | **expand** |
| `while (sum >= target)` | the window is valid → try to make it shorter (it might still be valid after removing more) |
| `best = Math.min(best, right - left + 1);` | record the current valid window's length |
| `sum -= nums[left++];` | **shrink** from the left |
| `return best == MAX ? 0 : best;` | 0 if no valid window was ever found |

### Dry run 1: target = 7, `[2, 3, 1, 2, 4, 3]`

| right | add | window (after the shrinks) | sum before the while | while iterations | best |
|---|---|---|---|---|---|
| 0 | 2 | [2] | 2 | — | ∞ |
| 1 | 3 | [2,3] | 5 | — | ∞ |
| 2 | 1 | [2,3,1] | 6 | — | ∞ |
| 3 | 2 | [2,3,1,2] → [3,1,2] | 8 | record 4; drop 2 → sum 6 | 4 |
| 4 | 4 | [3,1,2,4] → [2,4] | 10 | record 4; drop 3 → 7: record 3; drop 1 → 6 | 3 |
| 5 | 3 | [2,4,3] → [3] | 9 | record 3; drop 2 → 7: record **2**; drop 4 → 3 | **2** |

**Return 2** (`[4, 3]`) ✓

### Dry run 2: target = 4, `[1, 4, 4]`

| right | sum after adding | shrinks | best |
|---|---|---|---|
| 0 | 1 | — | ∞ |
| 1 | 5 | record 2; drop 1 → 4: record **1**; drop 4 → 0 | 1 |
| 2 | 4 | record 1; drop 4 → 0 | 1 |

**Return 1** ✓

### Dry run 3: target = 11, `[1,1,1,1,1]`
The sum never reaches 11, so `best` stays MAX → **return 0** ✓

### Complexity
- **Time O(n)** (amortised: each element enters and leaves the window at most once).
- **Space O(1).**

---

## 7. Comparing the approaches

| | Brute force | Prefix + binary search | Sliding window |
|---|---|---|---|
| Time | O(n²) | O(n log n) | **O(n)** |
| Space | O(1) | O(n) | **O(1)** |
| Needs positive numbers | for the `break` | **yes** (sorted prefixes) | **yes** |

---

## 8. Edge cases and common mistakes

**Edge cases:** no valid window (return 0), a single element ≥ target (answer 1), the whole array needed, a huge target.

**Mistakes:**
1. Using `if` instead of `while` for shrinking, which misses shorter windows after removing several elements.
2. Recording the length **after** shrinking (an off-by-one: the recorded window is no longer valid).
3. Returning `Integer.MAX_VALUE` instead of 0 when there's no answer.
4. Applying this to arrays with negatives, which gives wrong answers.

---

## 9. Interview questions and answers

**Q1. Why is the sliding window correct?**
> For each right edge, the window shrinks until it is no longer valid, so we record the **shortest** valid window ending at `right`. Positivity guarantees that a window starting further left (and ending at `right`) is longer and has a larger sum, so we never need to move left backwards.

**Q2. Why is it O(n) despite the nested loops?**
> `left` and `right` each move forward at most n times in total. The inner loop's total work across the whole run is O(n) (an amortised argument).

**Q3. What changes if the numbers can be negative?**
> The window sum isn't monotonic anymore. Use prefix sums with a **monotonic deque** of increasing prefix values: for each j, pop from the front while `P[j] − P[front] ≥ k` (recording lengths), and pop from the back while `P[back] ≥ P[j]`. O(n). That's LC 862, see [H04](../04-Hard/H04_ShortestSubarrayWithSumAtLeastK.md).

### Follow-up questions

**F1. The maximum-length subarray with sum ≤ target (positive numbers).**
> The "longest" template: expand, shrink while **invalid** (sum > target), then record. See [M12](M12_MaxConsecutiveOnesIII.md) for that shape.

**F2. Count the subarrays with sum < k (positive numbers, LC 713-style for products).** → [M14](M14_SubarrayProductLessThanK.md)
> For each right edge, after shrinking until valid, every window `[l..right]` with l from left to right is valid: add `right − left + 1`.

**F3. The minimum window substring that contains all the characters of t.** → [02-Strings H01](../../02-Strings/04-Hard/H01_MinimumWindowSubstring.md)
> The same "shrink while valid" template, with character counts as the window state.

**F4. Many queries with different targets.**
> Build the prefix sums once; each query is O(n log n) with binary search, or O(n) with a fresh sliding window.

---

## 10. Running the file

```bash
java M11_MinimumSizeSubarraySum.java      # 4/4 tests passed
```

**One-line summary:** *"Positive numbers ⇒ the window sum is monotonic: expand right, and while valid, record and shrink left."*
