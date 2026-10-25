# A23 · Split Array Largest Sum ⭐

| | |
|---|---|
| **LeetCode** | [#410 Split Array Largest Sum](https://leetcode.com/problems/split-array-largest-sum/) |
| **Difficulty** | Hard |
| **Mix of structures** | **Binary search on the answer** + a **greedy** feasibility check (or **DP** with prefix sums) |
| **Code** | [A23_SplitArrayLargestSum.java](A23_SplitArrayLargestSum.java) |
| **Run** | `java A23_SplitArrayLargestSum.java` |

⭐ = not in `leetcode.md`, added because it's the clearest example of "binary search on the answer", a pattern behind many hard problems.

---

## 1. The problem

Split `nums` (non-negative integers) into `k` non-empty **contiguous** parts so that the **largest part-sum is as small as possible**. Return that smallest possible largest sum.

```
nums = [7,2,5,10,8], k = 2   →  18     [7,2,5] = 14 and [10,8] = 18
nums = [1,2,3,4,5],  k = 2   →  9      [1,2,3] = 6 and [4,5] = 9
nums = [1,4,4],      k = 3   →  4      each number alone
```

This is the same as "Painter's Partition" and "Book Allocation".

---

## 2. How to think about it

### Flip the question
"What's the best split?" is hard. A yes/no version is easy:

> **Can we split into at most k parts if no part may exceed `cap`?**

### Greedy answers the yes/no question in O(n)
Walk left to right, adding numbers to the current part; when the next number would push it over `cap`, start a new part. This uses the **fewest** parts possible for that cap, because closing a part early never helps: the parts after it would just have more to hold.

### The answers are monotonic
If a cap works, any **bigger** cap also works. So over all caps the answers look like:

```
cap:     10   11  ...  17   18   19  ...  32
works?   no   no  ...  no   YES  YES ...  YES
```

Binary search for the **first YES**:
- `lo = max(nums)`: every part holds at least one number, so the answer can't be smaller;
- `hi = sum(nums)`: one part holding everything always works.

"At most k" is enough because a split with fewer parts can always be cut further, as long as k ≤ n, which is guaranteed.

### Dry run: `[7,2,5,10,8]`, k = 2 (lo = 10, hi = 32)
| lo | hi | mid | greedy parts | count | action |
|---|---|---|---|---|---|
| 10 | 32 | 21 | [7,2,5] [10,8] | 2 ≤ 2 | hi = 21 |
| 10 | 21 | 15 | [7,2,5] [10] [8] | 3 | lo = 16 |
| 16 | 21 | 18 | [7,2,5] [10,8] | 2 | hi = 18 |
| 16 | 18 | 17 | [7,2,5] [10] [8] | 3 | lo = 18 |
| 18 | 18 | | | | **18** |

### The DP alternative
`dp[j][i]` = the best largest-sum when the first `i` numbers are split into `j` parts:
```
dp[j][i] = min over p of max(dp[j−1][p], sum(nums[p..i−1]))      (the last part is nums[p..i−1])
```
Prefix sums give `sum(p..i−1)` in O(1). It's correct, but **O(k · n²)**.

---

## 3. Java concepts used in this problem

### 3.1 Binary search for "the first value that works"
```java
while (lo < hi) {
    long mid = (lo + hi) / 2;
    if (partsNeeded(nums, mid) <= k) hi = mid;      // mid works: the answer is mid or smaller
    else lo = mid + 1;                              // mid fails: the answer is bigger
}
```
- `hi = mid` (not `mid − 1`) because `mid` itself might be the answer.
- `lo = mid + 1` because `mid` definitely isn't.
- The loop ends with `lo == hi`, the first value that works. This template finds the lower bound of any monotonic yes/no condition.

### 3.2 `long` for sums
LeetCode allows 1,000 numbers up to 10⁶, so the sum reaches 10⁹, close to `int`'s limit (about 2.1 × 10⁹). `lo + hi` could then overflow an `int`. Using `long` for `lo`, `hi`, `mid` and `current` removes the risk.

### 3.3 A 2-D DP table filled with "infinity"
```java
long[][] dp = new long[k + 1][n + 1];
for (long[] row : dp) Arrays.fill(row, Long.MAX_VALUE);
dp[0][0] = 0;
```
`MAX_VALUE` marks impossible states (like 3 numbers in 5 non-empty parts). The code skips them before using `max(...)`.

---

## 4. The referee: try every way to place the cuts

### Code ([A23_SplitArrayLargestSum.java:45-55](A23_SplitArrayLargestSum.java#L45-L55))
```java
static int splitArrayBrute(int[] nums, int k) { return brute(nums, 0, k); }

private static int brute(int[] nums, int start, int k) {
    if (k == 1) { int s = 0; for (int i = start; i < nums.length; i++) s += nums[i]; return s; }
    int best = Integer.MAX_VALUE, first = 0;
    for (int end = start; end <= nums.length - k; end++) {           // the first part is nums[start..end]
        first += nums[end];
        best = Math.min(best, Math.max(first, brute(nums, end + 1, k - 1)));
    }
    return best;
}
```

| Line | What it does |
|---|---|
| `k == 1` | the last part takes everything that's left |
| `end <= n − k` | leave at least one number for each remaining part |
| `max(first, rest)` | the largest part of this split |
| `min` over all first cuts | the best split |

**Exponential**: the tests use n ≤ 9.

---

## 5. Approach 1: DP over (parts, prefix length)

### Code ([A23_SplitArrayLargestSum.java:58-71](A23_SplitArrayLargestSum.java#L58-L71))
```java
static int splitArrayDP(int[] nums, int k) {
    int n = nums.length;
    long[] pre = new long[n + 1];
    for (int i = 0; i < n; i++) pre[i + 1] = pre[i] + nums[i];
    long[][] dp = new long[k + 1][n + 1];
    for (long[] row : dp) Arrays.fill(row, Long.MAX_VALUE);
    dp[0][0] = 0;
    for (int j = 1; j <= k; j++)
        for (int i = j; i <= n; i++)                                  // i numbers into j non-empty parts
            for (int p = j - 1; p < i; p++)                           // the last part is nums[p..i-1]
                if (dp[j - 1][p] != Long.MAX_VALUE)
                    dp[j][i] = Math.min(dp[j][i], Math.max(dp[j - 1][p], pre[i] - pre[p]));
    return (int) dp[k][n];
}
```

| Line | What it does |
|---|---|
| `pre` | prefix sums: `sum(p..i−1) = pre[i] − pre[p]` |
| `dp[0][0] = 0` | zero numbers in zero parts |
| `i >= j`, `p >= j − 1` | each part needs at least one number |
| the update | best of: the first p numbers in j − 1 parts, then one last part |

**O(k · n²)** time, O(k · n) space.

---

## 6. Approach 2: Binary search on the answer + greedy (main solution)

### Code ([A23_SplitArrayLargestSum.java:74-94](A23_SplitArrayLargestSum.java#L74-L94))
```java
static int splitArray(int[] nums, int k) {
    long lo = 0, hi = 0;
    for (int x : nums) { lo = Math.max(lo, x); hi += x; }
    while (lo < hi) {
        long mid = (lo + hi) / 2;
        if (partsNeeded(nums, mid) <= k) hi = mid;                    // mid works: try smaller
        else lo = mid + 1;                                            // too small: need a bigger cap
    }
    return (int) lo;
}

/** Fewest contiguous parts with every part-sum <= cap (greedy: fill each part as much as possible). */
static int partsNeeded(int[] nums, long cap) {
    int parts = 1;
    long current = 0;
    for (int x : nums) {
        if (current + x > cap) { parts++; current = 0; }
        current += x;
    }
    return parts;
}
```

### Line by line
| Line | What it does |
|---|---|
| `lo = max`, `hi = sum` | the answer is somewhere in this range |
| `partsNeeded(mid) <= k` | mid is feasible, so the best answer is ≤ mid |
| `partsNeeded`: `current + x > cap` | x doesn't fit, so close this part and start a new one with x |
| why x always fits a fresh part | `cap ≥ lo = max(nums)` |

### Complexity
- Binary search: log₂(sum) ≈ 30 rounds; each greedy check is O(n).
- **O(n log(sum))** time, **O(1)** extra space.

---

## 7. Comparing the approaches

| | Time | Space |
|---|---|---|
| All cuts | exponential | O(k) recursion |
| DP | O(k · n²) | O(k · n) |
| **Binary search + greedy** | **O(n log(sum))** | O(1) |

The file checks all three on 2,000 random arrays (including zeros) and k values.

---

## 8. Edge cases and common mistakes

**Edge cases:** k = 1 (the whole sum), k = n (the largest element), zeros in the array, one huge element dominating.

**Mistakes:**
1. Starting `lo` at 0 or 1: a cap below the largest element makes the greedy check wrong (that element can never fit).
2. `hi = mid − 1` when mid works (it can skip the answer).
3. Requiring **exactly** k parts in the check instead of **at most** k.
4. `int` sums and `lo + hi` near the limit.
5. Greedy that closes a part when `current + x >= cap` (off by one: equal to cap is allowed).

---

## 9. Interview questions and answers

**Q1. What is "binary search on the answer"?**
> When the answer is a number, and "can we achieve at most X?" is monotonic in X and easy to check, binary-search X over its possible range instead of constructing the optimum directly.

**Q2. Why is the greedy check correct?**
> For a fixed cap, extending the current part as far as possible never increases the number of parts: any split can be transformed into the greedy one without adding parts.

**Q3. Why is "at most k parts" good enough?**
> With fewer parts you can always split a part further (each split keeps every part-sum ≤ cap), until you have exactly k, since k ≤ n.

### Follow-up questions

**F1. "Other problems with this pattern?"**
> Koko Eating Bananas (875), Capacity To Ship Packages Within D Days (1011), Minimum Number of Days to Make m Bouquets (1482), Magnetic Force Between Two Balls (1552), and [A24 K-th Smallest Pair Distance](A24_KthSmallestPairDistance.md).

**F2. "Return the actual split."**
> Run the greedy once more with the final cap, recording where each part starts. If it produces fewer than k parts, split some parts further.

**F3. "Negative numbers allowed."**
> Monotonicity breaks (adding a negative number shrinks a sum), so the greedy check fails; fall back to the DP.

---

## 10. Running the file

```bash
java A23_SplitArrayLargestSum.java     # 7/7 tests passed (examples, k = n, k = 1, DP, a sum of 10^9, 2,000 random cases)
```

**One-line summary:** *"Binary-search the cap between max(nums) and sum(nums); a greedy pass counts the fewest parts needed under a cap, and the first cap needing at most k parts is the answer."*
