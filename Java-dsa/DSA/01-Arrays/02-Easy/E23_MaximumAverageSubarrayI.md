# E23 · Maximum Average Subarray I

| | |
|---|---|
| **LeetCode** | [#643 Maximum Average Subarray I](https://leetcode.com/problems/maximum-average-subarray-i/) |
| **Difficulty** | Easy |
| **Pattern** | Sliding window (fixed size k) |
| **Code** | [E23_MaximumAverageSubarrayI.java](E23_MaximumAverageSubarrayI.java) |
| **Run** | `java E23_MaximumAverageSubarrayI.java` |

---

## 1. The problem

Find the **contiguous** subarray of length **exactly `k`** that has the **maximum average**, and return that average.

| nums | k | best window | answer |
|---|---|---|---|
| `[1, 12, -5, -6, 50, 3]` | 4 | `[12, -5, -6, 50]` (sum 51) | 12.75 |
| `[5]` | 1 | `[5]` | 5.0 |
| `[-1, -2, -3]` | 2 | `[-1, -2]` (sum −3) | −1.5 |
| `[0, 4, 0, 3, 2]` | 1 | `[4]` | 4.0 |

**Constraints:** `1 ≤ k ≤ n ≤ 10⁵`, `-10⁴ ≤ nums[i] ≤ 10⁴`.

"Contiguous subarray" = elements next to each other, without gaps (unlike a "subsequence").

---

## 2. How to think about it

1. **Simplify:** `k` is fixed, so `average = sum / k`. The window with the largest **sum** also has the largest **average**. Compare integer sums and divide **once** at the end. No floating-point work in the loop.
2. **Brute force:** compute each window's sum from scratch: (n − k + 1) windows × k additions = O(n·k).
3. **Insight:** neighbouring windows share **k − 1** elements. When the window slides one step right, exactly **one element enters** and **one leaves**:

```
nums:     [ 1, 12, -5, -6, 50,  3 ]
window 1:  [1, 12, -5, -6]                 sum = 2
window 2:      [12, -5, -6, 50]            sum = 2 + 50 − 1  = 51
window 3:           [-5, -6, 50, 3]        sum = 51 + 3 − 12 = 42
```

```
newSum = oldSum + nums[i] − nums[i − k]
               (entering)   (leaving)
```

That's the **fixed-size sliding window** pattern: O(1) work per step, O(n) total.

---

## 3. Java concepts used in this problem

### 3.1 `double` and the integer-division trap

```java
int best = 51, k = 4;
best / k            // 12      ← int / int = int (the decimal part is DROPPED)
(double) best / k   // 12.75   ← cast first, so double / int = double
(double) (best / k) // 12.0    ← WRONG: the division already happened in int
```

Java decides the type of an operation **from its operands**. If both are `int`, the result is `int`, and any fraction is thrown away. Casting **one** operand to `double` before dividing makes the whole division `double`. This is one of the most common bugs in Java.

`double` is a 64-bit floating-point number (about 15–16 significant digits). Values like 12.75 are stored exactly (they're powers-of-two fractions), but 0.1 isn't. That's why you should never compare doubles with `==` after a calculation; compare `Math.abs(a − b) < 1e-9` instead.

### 3.2 Two loops with different jobs

```java
for (int i = 0; i < k; i++) sum += nums[i];   // build the FIRST window
for (int i = k; i < nums.length; i++) { ... } // SLIDE it, one step per iteration
```

The second loop starts at `k`: index k is the first element **entering** the window, and `i − k` (= 0 at first) is the first element **leaving** it.

### 3.3 The test helper compares doubles as text

`check("...", 12.75, result)` turns both values into text (`"12.75"`) and compares the strings. That works here because the averages are exact. For general floating-point results you'd compare with a tolerance.

---

## 4. Approach 1: Brute force (sum each window from scratch)

```java
static double findMaxAverageBrute(int[] nums, int k) {
    int best = Integer.MIN_VALUE;
    for (int start = 0; start + k <= nums.length; start++) {
        int sum = 0;
        for (int j = start; j < start + k; j++) sum += nums[j];
        best = Math.max(best, sum);
    }
    return (double) best / k;
}
```

| Line | What it does |
|---|---|
| `int best = Integer.MIN_VALUE;` | sums can be negative, so we can't start at 0 |
| `start + k <= nums.length` | the window `[start, start+k−1]` must fit inside the array |
| inner loop | add the k elements of this window |

**O(n·k) time.** With n = 10⁵ and k = 5 × 10⁴, that's about 2.5 × 10⁹ operations, which **times out**.

---

## 5. Approach 2: Sliding window (the file's solution)

### Code ([E23_MaximumAverageSubarrayI.java:35-44](E23_MaximumAverageSubarrayI.java#L35-L44))

```java
static double findMaxAverage(int[] nums, int k) {
    int sum = 0;
    for (int i = 0; i < k; i++) sum += nums[i];          // the first window
    int best = sum;
    for (int i = k; i < nums.length; i++) {
        sum += nums[i] - nums[i - k];                    // add the new one, drop the old one
        best = Math.max(best, sum);
    }
    return (double) best / k;                            // cast BEFORE dividing (integer division trap)
}
```

### Line by line

| Line | What it does |
|---|---|
| `int sum = 0; for (i < k) sum += nums[i];` | the sum of the first window `nums[0..k−1]` |
| `int best = sum;` | the first window is the best so far. (Starting `best` at 0 would be **wrong** for all-negative input) |
| `for (int i = k; i < nums.length; i++)` | `i` = the index of the element **entering** the window |
| `sum += nums[i] - nums[i - k];` | add the entering element, remove the one leaving (`i − k`) |
| `best = Math.max(best, sum);` | keep the biggest window sum |
| `return (double) best / k;` | convert to an average, with a floating-point division |

**Overflow check:** the largest window sum is 10⁵ × 10⁴ = 10⁹ < 2.1 × 10⁹, so `int` is fine.

### Dry run 1: `nums = [1, 12, -5, -6, 50, 3]`, `k = 4`

First window: 1 + 12 + (−5) + (−6) = **2**, best = 2

| i | entering nums[i] | leaving nums[i−k] | sum = sum + in − out | best | window |
|---|---|---|---|---|---|
| 4 | 50 | nums[0] = 1 | 2 + 50 − 1 = **51** | 51 | `[12,-5,-6,50]` |
| 5 | 3 | nums[1] = 12 | 51 + 3 − 12 = 42 | 51 | `[-5,-6,50,3]` |

**Return 51 / 4 = 12.75** ✓

### Dry run 2: `[-1, -2, -3]`, `k = 2` (all negative)

First window: −1 + (−2) = **−3**, best = −3

| i | in | out | sum | best |
|---|---|---|---|---|
| 2 | −3 | −1 | −3 − 3 + 1 = −5 | −3 |

**Return −3 / 2 = −1.5** ✓. Had `best` started at 0, the answer would wrongly be 0.0.

### Dry run 3: `[0, 4, 0, 3, 2]`, `k = 1`

The window is a single element; the sum just becomes each element in turn: 0 → 4 → 0 → 3 → 2. Best = 4 → **4.0** ✓

### Complexity
- **Time O(n):** k steps to build the first window, then n − k O(1) slides.
- **Space O(1).**

---

## 6. (Bonus) Prefix-sum version

With a prefix array (E21), each window sum is `prefix[i+k] − prefix[i]`:

```java
static double findMaxAveragePrefix(int[] nums, int k) {
    int n = nums.length;
    int[] prefix = new int[n + 1];
    for (int i = 0; i < n; i++) prefix[i + 1] = prefix[i] + nums[i];
    int best = Integer.MIN_VALUE;
    for (int i = 0; i + k <= n; i++) best = Math.max(best, prefix[i + k] - prefix[i]);
    return (double) best / k;
}
```

It's O(n) too, but uses O(n) extra space. The sliding window is the same idea using only one running variable.

---

## 7. Comparing the approaches

| | Brute force | Prefix sums | Sliding window |
|---|---|---|---|
| Time | O(n·k) | O(n) | **O(n)** |
| Space | O(1) | O(n) | **O(1)** |

---

## 8. Edge cases and common mistakes

**Edge cases:** `k == n` (only one window), `k == 1` (the max element), all negatives, large values.

**Mistakes:**
1. `best / k` with ints → the decimal part is lost.
2. `(double) (best / k)` → cast too late.
3. Starting `best` at 0 → wrong for negative arrays.
4. Removing `nums[i − k + 1]` instead of `nums[i − k]` (off by one). Check: when i = k, the leaving element must be index 0.
5. Computing averages inside the loop (unnecessary floating-point work, plus possible rounding issues).

---

## 9. Interview questions and answers

**Q1. Why compare sums instead of averages?**
> All windows have the same length k, so ordering by sum equals ordering by average. Integers are exact and faster, and I divide only once at the end.

**Q2. What's the general sliding-window template?**
> Keep some window state (a sum, counts…). Add the entering element, remove the leaving element, then update the answer. For a fixed size, the leaving element is always `i − k`.

**Q3. Why initialise `best` with the first window rather than `Integer.MIN_VALUE`?**
> Both work. Using the first window is natural, because it's a real candidate. MIN_VALUE also works since it's replaced right away; but 0 does not, for negative inputs.

### Follow-up questions

**F1. Maximum Average Subarray II (LC 644, Hard): length **at least** k.**
> **Binary search on the answer** (the average): for a guess `m`, subtract m from every element and ask "is there a subarray of length ≥ k with sum ≥ 0?" Answer that with prefix sums plus the running minimum of the prefix k positions back. O(n log(range/ε)).

**F2. The number of windows of size k whose average ≥ threshold (LC 1343).**
> The same sliding window; compare `sum >= threshold × k` (integers, no division).

**F3. Return the start index of the best window.**
> Record `i − k + 1` whenever `best` improves (and 0 for the first window).

**F4. The maximum element in every window of size k (LC 239).** → [H02 Sliding Window Maximum](../04-Hard/H02_SlidingWindowMaximum.md)
> Sums can be updated with + and −, but a max can't "un-add" an element. You need a **monotonic deque**: O(n).

**F5. Variable-size windows: the minimum length with sum ≥ target.** → [M11 Minimum Size Subarray Sum](../03-Medium/M11_MinimumSizeSubarraySum.md)
> Expand right, and shrink from the left while the condition holds.

---

## 10. Running the file

```bash
java E23_MaximumAverageSubarrayI.java      # 4/4 tests passed
```

**One-line summary:** *"Fixed window: add the entering element, subtract the leaving one, track the max sum, divide by k once (as a double!)."*
