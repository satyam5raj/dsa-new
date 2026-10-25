# E03 · Running Sum of 1D Array

| | |
|---|---|
| **LeetCode** | [#1480 Running Sum of 1d Array](https://leetcode.com/problems/running-sum-of-1d-array/) |
| **Difficulty** | Easy |
| **Pattern** | Prefix sum (the simplest form) |
| **Code** | [E03_RunningSum.java](E03_RunningSum.java) |
| **Run** | `java E03_RunningSum.java` |

---

## 1. The problem

Return an array where each position holds the sum of all elements **up to and including** that position:

```
runningSum[i] = nums[0] + nums[1] + ... + nums[i]
```

| nums | answer | how |
|---|---|---|
| `[1, 2, 3, 4]` | `[1, 3, 6, 10]` | 1, 1+2, 1+2+3, 1+2+3+4 |
| `[1, 1, 1, 1, 1]` | `[1, 2, 3, 4, 5]` | |
| `[3, 1, 2, 10, 1]` | `[3, 4, 6, 16, 17]` | |
| `[-1, 2, -3]` | `[-1, 1, -2]` | negatives work the same way |

**Constraints:** `1 ≤ nums.length ≤ 1000`, `-10⁶ ≤ nums[i] ≤ 10⁶`.

---

## 2. How to think about it

- **Brute force:** for each `i`, add up `nums[0..i]` from scratch. That re-adds the same numbers again and again.
- **Notice the repetition:** `sum(0..3) = sum(0..2) + nums[3]`. Each answer is **the previous answer plus the current element**:

```
runningSum[i] = runningSum[i-1] + nums[i]
```

- So one pass is enough, and we can even overwrite `nums` itself, because by the time we're at `i`, `nums[i-1]` already holds the running sum up to `i-1`.

**Why this tiny problem matters:** this array is called a **prefix sum** array. It's the foundation of Find Pivot Index (E04), Range Sum Query (E21), Subarray Sum Equals K (M07) and many more. Once you have prefix sums, the sum of any range `l..r` is `prefix[r] − prefix[l−1]` in O(1).

---

## 3. Java concepts used in this problem

### 3.1 Compound assignment `+=`

```java
nums[i] += nums[i - 1];     // same as: nums[i] = nums[i] + nums[i - 1];
```

There are also `-=`, `*=`, `/=`, `%=`. A small detail: `x += y` includes a hidden **cast** back to x's type. So with `byte b = 10; b += 5;` it compiles, but `b = b + 5;` doesn't (because `b + 5` is an `int`).

### 3.2 Arrays are passed "by reference value": in-place changes are visible to the caller

```java
static int[] runningSum(int[] nums) {
    nums[0] = 99;     // changes the CALLER'S array too
    return nums;
}
```

Java is always **pass-by-value**. But for an array, the "value" that gets copied is the **reference** (the address of the array). So the method and the caller look at the **same** array in memory:

```
caller:  int[] a = {1,2,3};   a ─────┐
                                     ├──► [1, 2, 3]   (one array in the heap)
method:  runningSum(a)     nums ─────┘
```

- Changing `nums[i]` inside the method **does** change the caller's array.
- Reassigning `nums = new int[5]` inside the method does **not** affect the caller; it only changes where the local `nums` points.

That's why this solution is "in place": it reuses the input array's memory, and `return nums;` returns that same array.

> In real projects, silently modifying an input can surprise the caller. LeetCode allows it here. If you need to keep the original, use `int[] out = nums.clone();` and work on `out`.

### 3.3 Integer overflow (good habit)

An `int` holds values from about −2.1 × 10⁹ to +2.1 × 10⁹. Here the worst sum is 1000 × 10⁶ = 10⁹, which fits. If the constraints were bigger, the sum could **overflow** and silently wrap around to a negative number. Then you would use `long` (up to about 9.2 × 10¹⁸):

```java
long[] prefix = new long[n];
```

Always do a quick "max possible sum" check against the constraints.

---

## 4. Approach 1: Brute force (recompute every sum)

The `.java` header describes this one; here's the code:

```java
static int[] runningSumBrute(int[] nums) {
    int n = nums.length;
    int[] result = new int[n];            // new array, filled with zeros by default
    for (int i = 0; i < n; i++) {
        int sum = 0;
        for (int j = 0; j <= i; j++) {    // add nums[0..i]
            sum += nums[j];
        }
        result[i] = sum;
    }
    return result;
}
```

| Line | What it does |
|---|---|
| `int[] result = new int[n];` | a separate output array. Java fills new `int` arrays with `0` |
| `for (int i = 0; i < n; i++)` | for each output position |
| `int sum = 0;` | start the sum from zero every time. **This is the waste** |
| `for (int j = 0; j <= i; j++) sum += nums[j];` | add every element from 0 up to and **including** `i` (note the `<=`) |
| `result[i] = sum;` | store it |

**Dry run** `[1, 2, 3, 4]`:

| i | elements added | sum | result |
|---|---|---|---|
| 0 | 1 | 1 | `[1,0,0,0]` |
| 1 | 1+2 | 3 | `[1,3,0,0]` |
| 2 | 1+2+3 | 6 | `[1,3,6,0]` |
| 3 | 1+2+3+4 | 10 | `[1,3,6,10]` |

Total additions: 1 + 2 + 3 + 4 = 10, which is n(n+1)/2. **Time O(n²), Space O(n)** for the output.

---

## 5. Approach 2: One pass, in place (optimal)

### Code ([E03_RunningSum.java:37-42](E03_RunningSum.java#L37-L42))

```java
static int[] runningSum(int[] nums) {
    for (int i = 1; i < nums.length; i++) {
        nums[i] += nums[i - 1];        // nums[i-1] already holds the sum up to i-1
    }
    return nums;
}
```

### Line by line

| Line | What it does |
|---|---|
| `for (int i = 1; ...)` | start at **1**: position 0's running sum is just `nums[0]`, which is already correct. Starting at 0 would read `nums[-1]` and crash |
| `nums[i] += nums[i - 1];` | `nums[i-1]` was **already updated** on the previous iteration, so it holds `nums[0] + ... + nums[i-1]`. Adding the original `nums[i]` gives the running sum up to `i` |
| `return nums;` | the same array, now holding the running sums |

**The invariant** (a fact that's true at every step): *at the start of iteration `i`, `nums[0..i-1]` already holds running sums, and `nums[i..]` still holds the original values.*

### Dry run 1: `[1, 2, 3, 4]`

| i | nums[i-1] (already a running sum) | nums[i] (original) | new nums[i] | array after |
|---|---|---|---|---|
| start | | | | `[1, 2, 3, 4]` |
| 1 | 1 | 2 | 3 | `[1, 3, 3, 4]` |
| 2 | 3 | 3 | 6 | `[1, 3, 6, 4]` |
| 3 | 6 | 4 | 10 | `[1, 3, 6, 10]` |

### Dry run 2: `[-1, 2, -3]` (negatives)

| i | nums[i-1] | nums[i] | new nums[i] | array |
|---|---|---|---|---|
| 1 | −1 | 2 | 1 | `[-1, 1, -3]` |
| 2 | 1 | −3 | −2 | `[-1, 1, -2]` |

### Dry run 3: `[5]` (single element)
The loop condition `1 < 1` is false, so the loop never runs. The result is `[5]`. No special case needed.

### Complexity
- **Time O(n):** n − 1 additions.
- **Space O(1) extra:** we reuse the input array.

---

## 6. Comparing the approaches

| | Brute force | One pass in place |
|---|---|---|
| Time | O(n²) | **O(n)** |
| Extra space | O(n) (new array) | **O(1)** |
| Keeps the input unchanged? | yes | no (use `clone()` first if needed) |

---

## 7. Edge cases and common mistakes

**Edge cases:** single element, all zeros, negatives, large values (overflow → use `long`).

**Mistakes:**
1. Starting the loop at `i = 0`, which reads `nums[-1]` → `ArrayIndexOutOfBoundsException`.
2. In brute force, writing `j < i` instead of `j <= i`, which leaves out the current element.
3. Assuming the caller's array is unchanged after the call.

---

## 8. Interview questions and answers

**Q1. Why can you overwrite `nums` safely?**
> At step `i` I only need the original `nums[i]` (not yet overwritten) and the running sum at `i−1` (already computed and stored in `nums[i−1]`). I never need the original `nums[i−1]` again.

**Q2. What's a prefix sum and why is it useful?**
> `prefix[i]` = sum of the first i+1 elements. Building it costs O(n) once. After that, the sum of any range `l..r` is `prefix[r] − prefix[l−1]` in O(1), instead of O(r − l).

**Q3. What if the sums could exceed the `int` range?**
> Store them in a `long[]`. Java doesn't throw an error on overflow; the value silently wraps around. `Math.addExact(a, b)` throws an `ArithmeticException` on overflow if you want to detect it.

### Follow-up questions

**F1. Answer many "sum of range l..r" queries.** → [E21 Range Sum Query](E21_RangeSumQueryImmutable.md)
> Build a prefix array of length n+1 with `prefix[0] = 0` (this avoids the `l−1` special case). Then each query is `prefix[r+1] − prefix[l]`.

**F2. Count subarrays whose sum equals k.** → [M07 Subarray Sum Equals K](../03-Medium/M07_SubarraySumEqualsK.md)
> Sum of `l..r` = k means `prefix[r] − prefix[l−1] = k`. So for each `r`, count how many earlier prefixes equal `prefix[r] − k`, using a HashMap. O(n).

**F3. 2D version: the sum of a rectangle in a matrix.**
> 2D prefix: `P[i][j] = grid[i][j] + P[i-1][j] + P[i][j-1] − P[i-1][j-1]`. A rectangle sum then combines four corners. Build O(rows × cols), query O(1).

**F4. The array keeps changing (updates) while you also answer range sums.**
> A plain prefix array would need an O(n) rebuild after each update. Use a **Fenwick tree** (Binary Indexed Tree) or a **segment tree**: O(log n) update and O(log n) query. See 07-Trees [SegmentTreeAndFenwick](../../07-Trees/01-Basics/SegmentTreeAndFenwick.java).

**F5. Running product instead of sum?**
> Same idea with `*=`. Watch out for zeros and overflow. Prefix products are the basis of [M06 Product of Array Except Self](../03-Medium/M06_ProductOfArrayExceptSelf.md).

---

## 9. Running the file

```bash
java E03_RunningSum.java      # 5/5 tests passed
```

**One-line summary:** *"Each running sum = the previous running sum + the current element."*
