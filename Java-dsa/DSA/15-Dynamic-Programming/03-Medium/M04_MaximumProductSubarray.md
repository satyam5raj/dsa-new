# M04 · Maximum Product Subarray

| | |
|---|---|
| **LeetCode** | [#152 Maximum Product Subarray](https://leetcode.com/problems/maximum-product-subarray/) |
| **Difficulty** | Medium |
| **Pattern** | Kadane with **two** states: track both the max and the min product ending here |
| **Code** | [M04_MaximumProductSubarray.java](M04_MaximumProductSubarray.java) |
| **Run** | `java M04_MaximumProductSubarray.java` |

The DP recipe is explained in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe); Kadane's algorithm for sums is in [Arrays M01 Maximum Subarray](../../01-Arrays/03-Medium/M01_MaximumSubarray.md).

---

## 1. The problem

Find the largest product of a contiguous, non-empty subarray.

| nums | answer | subarray |
|---|---|---|
| [2, 3, −2, 4] | 6 | [2, 3] |
| [−2, 0, −1] | 0 | [0] |
| [−2, 3, −4] | 24 | the whole array: the two negatives cancel |
| [−2] | −2 | |

---

## 2. How to think about it

### Brute force
Every subarray, with a running product: O(n²).

### Why plain Kadane fails
With **sums**, a bad (negative) prefix is simply dropped. With **products**, a negative number flips the sign: the **smallest** (most negative) product so far can become the **largest** after multiplying by a negative. So the best product ending at i might come from the worst product ending at i − 1.

### Two DP states
For subarrays **ending** at i, keep:
- hi[i] = the maximum product ending at i;
- lo[i] = the minimum product ending at i.

The candidates for both are:
1. x alone (start fresh at i);
2. hi[i − 1] · x;
3. lo[i − 1] · x.

hi[i] is the max of the three and lo[i] is the min. Starting fresh also handles 0: after a 0, both states become 0 and then restart from the next element.

| step | for this problem |
|---|---|
| **State** | hi[i], lo[i] = the max and min product of a subarray ending at i |
| **Transition** | max / min of {x, hi[i−1]·x, lo[i−1]·x} |
| **Base** | hi[0] = lo[0] = nums[0] |
| **Answer** | the maximum hi[i] over all i |

### Dry run: [−2, 3, −4]

| x | candidates (x, hi·x, lo·x) | hi | lo | best |
|---|---|---|---|---|
| −2 | (start) | −2 | −2 | −2 |
| 3 | 3, −6, −6 | 3 | −6 | 3 |
| −4 | −4, −12, **24** | 24 | −12 | **24** |

The 24 came from **lo** · x: the most negative product (−6) times a negative.

### Dry run: [2, 3, −2, 4]

| x | candidates | hi | lo | best |
|---|---|---|---|---|
| 2 | (start) | 2 | 2 | 2 |
| 3 | 3, 6, 6 | 6 | 3 | 6 |
| −2 | −2, −12, −6 | −2 | −12 | 6 |
| 4 | 4, −8, −48 | 4 | −48 | **6** |

---

## 3. Java concepts used in this problem

### 3.1 Compute from the old values before overwriting

```java
int a = hi * x, b = lo * x;                        // compute from the OLD hi/lo before overwriting
hi = Math.max(x, Math.max(a, b));
lo = Math.min(x, Math.min(a, b));
```

If `hi` were updated first, the `lo` line would read the **new** `hi`. Saving both products first avoids that bug.

### 3.2 Nested `Math.max` / `Math.min`

`Math.max` takes two arguments, so the max of three is `Math.max(x, Math.max(a, b))`.

### 3.3 `Integer.MIN_VALUE` as the starting best (in the referee)

The answer can be negative ([−2]), so the brute force can't start its best at 0. Starting at the smallest `int` means any real product replaces it.

---

## 4. Approach 1: Every subarray (the referee)

### Code ([M04_MaximumProductSubarray.java:52-59](M04_MaximumProductSubarray.java#L52-L59))

```java
static int brute(int[] a) {
    int best = Integer.MIN_VALUE;
    for (int i = 0; i < a.length; i++) {
        int p = 1;
        for (int j = i; j < a.length; j++) { p *= a[j]; best = Math.max(best, p); }
    }
    return best;
}
```

For each start i, extend the product one element at a time. **O(n²).**

---

## 5. Approach 2: Kadane with max and min (the file's main solution)

### Code ([M04_MaximumProductSubarray.java:40-50](M04_MaximumProductSubarray.java#L40-L50))

```java
static int maxProduct(int[] nums) {
    int hi = nums[0], lo = nums[0], best = nums[0];
    for (int i = 1; i < nums.length; i++) {
        int x = nums[i];
        int a = hi * x, b = lo * x;                        // compute from the OLD hi/lo before overwriting
        hi = Math.max(x, Math.max(a, b));
        lo = Math.min(x, Math.min(a, b));
        best = Math.max(best, hi);
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| the initialisation | the first element is the only subarray ending at 0 |
| `a`, `b` | extend the previous best and worst by x |
| `hi`, `lo` | the best and worst ending at i, including starting fresh |
| `best` | the best ending anywhere so far |

### Complexity
- **Time O(n), space O(1).**

---

## 6. Comparing the approaches

| | Every subarray | Max/min Kadane |
|---|---|---|
| Time | O(n²) | **O(n)** |
| Handles negatives and zeros | yes | yes |

The file checks the fast version against the brute force on 1,000 random arrays with values from −4 to 4 (plenty of zeros and negatives).

---

## 7. Edge cases and common mistakes

**Edge cases:** a single negative element, zeros splitting the array, an even number of negatives (the whole array), an odd number (drop one end).

**Mistakes:**
1. Tracking only the maximum (misses negative × negative).
2. Updating `hi` before computing `lo` from the old values.
3. Starting `best` at 0 (wrong for [−2]).
4. Overflow on long inputs (LeetCode guarantees the answer fits in 32 bits).

---

## 8. Interview questions and answers

**Q1. Why track the minimum?**
> A negative number turns the most negative product into the most positive one. Without the minimum, that candidate is lost.

**Q2. Is there another O(n) approach?**
> Yes: compute running products from the left and from the right (resetting to 1 after a 0), and take the maximum seen. The best subarray, between zeros, either starts at a segment's left edge or ends at its right edge, depending on the parity of negatives.

### Follow-up questions

**F1. Maximum Subarray (sums).** → [Arrays M01](../../01-Arrays/03-Medium/M01_MaximumSubarray.md)
> One state is enough because adding never flips the sign.

**F2. Return the subarray itself.**
> Track the start index alongside hi and lo (resetting it when "start fresh" wins).

**F3. Maximum Product of Three Numbers (LC 628).**
> Not a subarray problem: sort (or track) the three largest and two smallest values.

---

## 9. Running the file

```bash
java M04_MaximumProductSubarray.java      # 6/6 tests passed (including 1000 random with 0s and negatives)
```

**One-line summary:** *"Track the max and the min product ending at each position (each the best of x, hi·x and lo·x), because a negative x turns the min into the max, and keep the best max seen."*
