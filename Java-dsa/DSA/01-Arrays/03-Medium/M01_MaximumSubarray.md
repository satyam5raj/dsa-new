# M01 · Maximum Subarray (Kadane's Algorithm)

| | |
|---|---|
| **LeetCode** | [#53 Maximum Subarray](https://leetcode.com/problems/maximum-subarray/) |
| **Difficulty** | Medium |
| **Pattern** | Kadane's algorithm (dynamic programming in O(1) space) |
| **Code** | [M01_MaximumSubarray.java](M01_MaximumSubarray.java) |
| **Run** | `java M01_MaximumSubarray.java` |

---

## 1. The problem

Find the **contiguous** subarray (at least one element) with the **largest sum**, and return that sum.

| nums | best subarray | answer |
|---|---|---|
| `[-2, 1, -3, 4, -1, 2, 1, -5, 4]` | `[4, -1, 2, 1]` | 6 |
| `[5, 4, -1, 7, 8]` | the whole array | 23 |
| `[-3, -1, -2]` | `[-1]` | −1 (all negative: the single largest element) |
| `[1]` | `[1]` | 1 |

**Constraints:** `1 ≤ n ≤ 10⁵`, `-10⁴ ≤ nums[i] ≤ 10⁴`.

This is one of the **most asked** interview problems. Know Kadane's algorithm by heart, **and know why it works**.

---

## 2. How to think about it

1. **Brute force:** try every `(start, end)` pair. With a running sum, that's O(n²).
2. **Change the question** (the key DP move): instead of "what's the best subarray anywhere?", ask:

   > **`cur(i)`** = the best sum of a subarray that **ends exactly at index i**.

   A subarray ending at i is one of two things:
   - **(a)** just `nums[i]` by itself (start fresh here), or
   - **(b)** the best subarray ending at `i − 1`, **extended** by `nums[i]` → `cur(i−1) + nums[i]`

   So:

   ```
   cur(i) = max(nums[i], cur(i−1) + nums[i])
   ```

   In words: **"If what I've accumulated so far is negative, it only drags me down: drop it and restart here."**
3. The answer is the **maximum `cur(i)` over all i** (the best subarray must end *somewhere*).
4. `cur(i)` depends only on `cur(i−1)`, so one variable is enough: O(1) space.

That's **dynamic programming**: define a subproblem (best ending here), find a recurrence, and keep only what you need.

---

## 3. Java concepts used in this problem

### 3.1 DP state in a single variable

A DP solution often starts as an array `dp[i]`. When `dp[i]` only depends on `dp[i−1]`, you can replace the array with **one variable** that you overwrite each step. That's "space optimisation" and it's very common:

```java
// array version                         // O(1) version
int[] dp = new int[n];                   int cur = nums[0];
dp[0] = nums[0];                         for (i = 1..n-1)
for (i = 1..n-1)                             cur = Math.max(nums[i], cur + nums[i]);
    dp[i] = Math.max(nums[i], dp[i-1] + nums[i]);
```

### 3.2 Returning several values: `new int[]{best, bestStart, bestEnd}`

Java methods return **one** value. To return several, either pack them into an array (as the file does), or create a small class/record:

```java
record Result(int sum, int start, int end) {}     // Java 16+: an immutable data holder
// ...then, inside the method:   return new Result(best, bestStart, bestEnd);
```

A **record** automatically gets a constructor, getters (`r.sum()`), `equals`, `hashCode` and `toString`.

### 3.3 Recursion (used in the divide & conquer bonus)

A **recursive** method calls itself on a smaller part of the problem, and has a **base case** that stops the recursion. Each call gets its own local variables on the **call stack**. Recursion depth matters: about 10⁴ nested calls can trigger a `StackOverflowError`, but divide & conquer only goes log₂(n) deep (about 17 for n = 10⁵).

---

## 4. Approach 1: Brute force with a running sum

```java
static int maxSubArrayBrute(int[] nums) {
    int best = Integer.MIN_VALUE;
    for (int start = 0; start < nums.length; start++) {
        int sum = 0;
        for (int end = start; end < nums.length; end++) {
            sum += nums[end];                 // the sum of nums[start..end], built incrementally
            best = Math.max(best, sum);
        }
    }
    return best;
}
```

| Line | What it does |
|---|---|
| `best = Integer.MIN_VALUE` | sums can be negative, so we can't start at 0 |
| outer `start` | every possible left end |
| `sum += nums[end]` | extend the subarray by one element: no need to re-add from start |
| `best = Math.max(best, sum)` | every subarray `[start..end]` is a candidate |

**Dry run** `[-2, 1, -3]`:

| start | end | sum | best |
|---|---|---|---|
| 0 | 0 | −2 | −2 |
| 0 | 1 | −1 | −1 |
| 0 | 2 | −4 | −1 |
| 1 | 1 | 1 | 1 |
| 1 | 2 | −2 | 1 |
| 2 | 2 | −3 | 1 |

**Return 1.** **O(n²) time** (about 5 × 10⁹ for n = 10⁵, which times out), **O(1) space.**

---

## 5. Approach 2: Kadane's algorithm (optimal)

### Code ([M01_MaximumSubarray.java:51-58](M01_MaximumSubarray.java#L51-L58))

```java
static int maxSubArray(int[] nums) {
    int cur = nums[0], best = nums[0];
    for (int i = 1; i < nums.length; i++) {
        cur = Math.max(nums[i], cur + nums[i]);   // extend or restart
        best = Math.max(best, cur);
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int cur = nums[0], best = nums[0];` | the only subarray ending at index 0 is `[nums[0]]` |
| `for (int i = 1; ...)` | process the rest one by one |
| `cur = Math.max(nums[i], cur + nums[i]);` | the best subarray ending at i: restart (`nums[i]` alone) or extend (`cur + nums[i]`). Restarting wins exactly when the old `cur` is negative |
| `best = Math.max(best, cur);` | the best answer seen anywhere so far |
| `return best;` | |

**Why start with `nums[0]` and not 0?** For all-negative input, the answer is negative (`[-3,-1,-2]` → −1). Starting `best` at 0 would wrongly return 0, the sum of an **empty** subarray, which isn't allowed.

### Dry run 1: `[-2, 1, -3, 4, -1, 2, 1, -5, 4]`

| i | x | cur + x | cur = max(x, cur+x) | decision | best |
|---|---|---|---|---|---|
| 0 | −2 | — | −2 | start | −2 |
| 1 | 1 | −1 | **1** | restart (old sum −2 < 0) | 1 |
| 2 | −3 | −2 | −2 | extend | 1 |
| 3 | 4 | 2 | **4** | restart (old sum −2 < 0) | 4 |
| 4 | −1 | 3 | 3 | extend | 4 |
| 5 | 2 | 5 | 5 | extend | 5 |
| 6 | 1 | 6 | **6** | extend | **6** |
| 7 | −5 | 1 | 1 | extend | 6 |
| 8 | 4 | 5 | 5 | extend | 6 |

**Return 6**: the subarray `[4, -1, 2, 1]` (indices 3..6) ✓

### Dry run 2: `[-3, -1, -2]` (all negative)

| i | x | cur + x | cur | best |
|---|---|---|---|---|
| 0 | −3 | — | −3 | −3 |
| 1 | −1 | −4 | −1 (restart) | −1 |
| 2 | −2 | −3 | −2 (restart) | −1 |

**Return −1** ✓

### Dry run 3: `[5, 4, -1, 7, 8]`
cur: 5 → 9 → 8 → 15 → 23; it never restarts. **best = 23** ✓

### Complexity
- **Time O(n)**, **Space O(1)**.

---

## 6. Variant: also return where the best subarray starts and ends

### Code ([M01_MaximumSubarray.java:61-78](M01_MaximumSubarray.java#L61-L78))

```java
static int[] maxSubArrayWithIndices(int[] nums) {
    int cur = nums[0], best = nums[0];
    int start = 0, bestStart = 0, bestEnd = 0;
    for (int i = 1; i < nums.length; i++) {
        if (nums[i] > cur + nums[i]) {             // restarting is better
            cur = nums[i];
            start = i;
        } else {
            cur += nums[i];
        }
        if (cur > best) {
            best = cur;
            bestStart = start;
            bestEnd = i;
        }
    }
    return new int[]{best, bestStart, bestEnd};
}
```

| Line | What it does |
|---|---|
| `start` | where the **current** run began |
| `if (nums[i] > cur + nums[i])` | the same as `cur < 0`: restart here, and remember the new start |
| `else cur += nums[i];` | extend the current run |
| `if (cur > best) {...}` | a new best: save the current run's start and this end |

**Dry run** on the main example: the restarts happen at i = 1 (start = 1) and i = 3 (start = 3). The best improves at i = 3 (4), 5 (5), 6 (6), each time with start = 3. Result **`[6, 3, 6]`** ✓

---

## 7. (Bonus) Approach 3: Divide and conquer, O(n log n)

A classic follow-up: *"Can you do it with divide and conquer?"* Split at `mid`. The best subarray is either entirely in the **left** half, entirely in the **right** half, or **crosses** the middle. The crossing case = (the best suffix of the left half) + (the best prefix of the right half).

```java
static int maxSubArrayDC(int[] nums) { return solve(nums, 0, nums.length - 1); }

static int solve(int[] a, int lo, int hi) {
    if (lo == hi) return a[lo];                       // base case: one element
    int mid = (lo + hi) >>> 1;
    int leftBest = solve(a, lo, mid);
    int rightBest = solve(a, mid + 1, hi);

    int sum = 0, bestLeftSuffix = Integer.MIN_VALUE;  // must include a[mid]
    for (int i = mid; i >= lo; i--) { sum += a[i]; bestLeftSuffix = Math.max(bestLeftSuffix, sum); }
    sum = 0;
    int bestRightPrefix = Integer.MIN_VALUE;          // must include a[mid+1]
    for (int i = mid + 1; i <= hi; i++) { sum += a[i]; bestRightPrefix = Math.max(bestRightPrefix, sum); }

    return Math.max(Math.max(leftBest, rightBest), bestLeftSuffix + bestRightPrefix);
}
```

- `(lo + hi) >>> 1` is the midpoint without overflow (an unsigned shift right = ÷2). `(lo + hi) / 2` can overflow when lo + hi > 2³¹ − 1.
- **T(n) = 2T(n/2) + O(n) = O(n log n)**, recursion depth O(log n).
- It's slower than Kadane, but this structure (each segment storing total, best prefix, best suffix and best sum) is how a **segment tree** answers "max subarray in a range [l, r]" queries.

---

## 8. Comparing the approaches

| | Brute force | Kadane | Divide & conquer |
|---|---|---|---|
| Time | O(n²) | **O(n)** | O(n log n) |
| Space | O(1) | **O(1)** | O(log n) recursion |
| Extends to range queries | no | no | **yes (segment tree)** |

---

## 9. Edge cases and common mistakes

**Edge cases:** all negative, a single element, all positive (the whole array), zeros, the best subarray at the very start or end.

**Mistakes:**
1. Initialising `best = 0` (or `cur = 0` and restarting with `cur = max(0, cur + x)`): it returns 0 for all-negative input. (The `max(0, ...)` form is fine only if empty subarrays are allowed, as in the stock problem E05.)
2. Updating `best` **before** updating `cur`.
3. Restarting when `cur + x < 0` instead of when `cur < 0`. Think it through: what matters is whether the **past** helps.
4. Overflow for large inputs (use `long` if `n × max|x|` exceeds the int range).

---

## 10. Interview questions and answers

**Q1. Explain Kadane's in one sentence.**
> For each index, the best subarray ending there either extends the previous best or starts fresh, whichever is larger; the answer is the best of those.

**Q2. Why is it correct?**
> Every subarray ends at some index i. `cur(i)` is provably the best sum among subarrays ending at i (induction: any subarray ending at i is `[i]` alone or a subarray ending at i−1 plus `nums[i]`, and the best of the latter uses `cur(i−1)`). Taking the max over all i covers every subarray.

**Q3. Is this greedy or DP?**
> It's DP (the subproblem "best sum ending at i" with a recurrence), optimised to O(1) space. The "drop a negative prefix" rule makes it look greedy.

### Follow-up questions

**F1. Return the subarray itself.**
> Track `start` at each restart, and `bestStart` / `bestEnd` at each improvement (see section 6).

**F2. Maximum Product Subarray (LC 152).** → [15-DP M04](../../15-Dynamic-Programming/03-Medium/M04_MaximumProductSubarray.md)
> Track both the max **and** the min product ending here: a negative number times the most negative product can become the largest. `newMax = max(x, x·max, x·min)`, `newMin = min(x, x·max, x·min)`.

**F3. Maximum Sum Circular Subarray (LC 918).**
> The answer is either the normal Kadane max, or `total − (minimum subarray sum)` (the best subarray wraps around). Careful: if all elements are negative, return the normal max (otherwise the "wrap" would be empty).

**F4. Maximum subarray sum with at most one deletion (LC 1186).**
> Two DP states: `keep` (no deletion used) and `del` (one deletion used): `del = max(del + x, keep)`, `keep = max(x, keep + x)`.

**F5. Max subarray in a range [l, r], with updates.**
> A segment tree whose nodes store `(sum, bestPrefix, bestSuffix, best)`; merge two children as in divide & conquer. O(log n) per query and update.

**F6. The maximum sum rectangle in a 2D matrix.**
> Fix a pair of rows (top, bottom), collapse the columns into a 1D array of column sums, and run Kadane. O(rows² × cols).

---

## 11. Running the file

```bash
java M01_MaximumSubarray.java      # 5/5 tests passed
```

**One-line summary:** *"cur = max(x, cur + x): extend if the past helps, otherwise restart; the answer is the max cur seen."*
