# M15 · Sum of Subarray Minimums ⭐

| | |
|---|---|
| **LeetCode** | [#907 Sum of Subarray Minimums](https://leetcode.com/problems/sum-of-subarray-minimums/) |
| **Difficulty** | Medium |
| **Pattern** | Monotonic stack → "in how many subarrays is arr[i] the minimum?" (the contribution technique) |
| **Code** | [M15_SumOfSubarrayMinimums.java](M15_SumOfSubarrayMinimums.java) |
| **Run** | `java M15_SumOfSubarrayMinimums.java` |

⭐ = a bonus classic.

---

## 1. The problem

Return the sum of `min(subarray)` over **every** contiguous subarray, modulo 10⁹ + 7.

```
[3, 1, 2, 4]
subarrays: [3]3 [1]1 [2]2 [4]4 [3,1]1 [1,2]1 [2,4]2 [3,1,2]1 [1,2,4]1 [3,1,2,4]1   → 17
```

| arr | answer |
|---|---|
| `[3, 1, 2, 4]` | 17 |
| `[11, 81, 94, 43, 3]` | 444 |
| `[2, 2]` | 6 (`[2]` + `[2]` + `[2,2]` = 2 + 2 + 2) |

**Constraints:** `1 ≤ n ≤ 3 × 10⁴`, `1 ≤ arr[i] ≤ 3 × 10⁴`.

---

## 2. How to think about it

### Brute force
Every subarray, with a running minimum: O(n²). That's 9 × 10⁸ for n = 3 × 10⁴, which is too slow.

### Flip the question (the "contribution technique")
Instead of "for each subarray, what's its min?", ask:

> **"For each element arr[i], in HOW MANY subarrays is it the minimum?"**

Then `answer = Σ arr[i] × count(i)`.

### Counting
arr[i] is the minimum of every subarray that:
- **starts** anywhere after the **previous smaller** element (on the left), and
- **ends** anywhere before the **next smaller** element (on the right).

```
left[i]  = i − (the index of the previous smaller)     the choices for the start
right[i] = (the index of the next smaller) − i         the choices for the end
count(i) = left[i] × right[i]
```

Both "nearest smaller" arrays come from a **monotonic stack** in O(n), the same tool as [Largest Rectangle in Histogram](../../01-Arrays/04-Hard/H03_LargestRectangleInHistogram.md).

### The duplicates trap
With equal values, a subarray like `[2, 2]` must be counted **once**, not once for each 2. The fix: use a **strict** `<` on one side and `≤` on the other: "the previous **strictly** smaller" versus "the next smaller **or equal**". Then a tie is credited to just one of the equal elements (here, the leftmost one).

---

## 3. Java concepts used in this problem

### 3.1 Modular arithmetic with `long`

```java
static final int MOD = 1_000_000_007;
// ...inside the loop:   sum = (sum + (long) arr[i] * left[i] * right[i]) % MOD;
```

- `1_000_000_007`: underscores in numeric literals (Java 7+) are just for readability.
- `arr[i] · left · right` can reach about 3×10⁴ · 3×10⁴ · 3×10⁴ = 2.7 × 10¹³, which overflows an `int`. Casting the **first** factor to `long` makes the whole product `long`.
- Applying `% MOD` at each step keeps `sum` small. 10⁹ + 7 is a common prime modulus in competitive programming.

### 3.2 Two monotonic-stack passes

The first pass (left to right) finds the previous smaller elements; the second (right to left, after `st.clear()`) finds the next smaller-or-equal ones. The **strictness** of each while condition (`>=` versus `>`) is what implements the duplicates fix.

### 3.3 `static final` constants

`static final int MOD` is a class-level constant (by convention in UPPER_CASE). `final` means it can't be reassigned.

---

## 4. Approach 1: Brute force

### Code ([M15_SumOfSubarrayMinimums.java:71-78](M15_SumOfSubarrayMinimums.java#L71-L78))

```java
static int brute(int[] a) {
    long s = 0;
    for (int i = 0; i < a.length; i++) {
        int m = Integer.MAX_VALUE;
        for (int j = i; j < a.length; j++) { m = Math.min(m, a[j]); s += m; }
    }
    return (int) (s % MOD);
}
```

For each start i, extend j and keep the running min. **O(n²).** It's the reference for 500 random tests with many duplicates.

---

## 5. Approach 2: Contribution via the nearest smaller elements (the file's solution)

### Code ([M15_SumOfSubarrayMinimums.java:51-69](M15_SumOfSubarrayMinimums.java#L51-L69))

```java
static int sumSubarrayMins(int[] arr) {
    int n = arr.length;
    int[] left = new int[n], right = new int[n];
    Deque<Integer> st = new ArrayDeque<>();
    for (int i = 0; i < n; i++) {                               // previous STRICTLY smaller
        while (!st.isEmpty() && arr[st.peek()] >= arr[i]) st.pop();
        left[i] = i - (st.isEmpty() ? -1 : st.peek());
        st.push(i);
    }
    st.clear();
    for (int i = n - 1; i >= 0; i--) {                          // next smaller OR EQUAL
        while (!st.isEmpty() && arr[st.peek()] > arr[i]) st.pop();
        right[i] = (st.isEmpty() ? n : st.peek()) - i;
        st.push(i);
    }
    long sum = 0;
    for (int i = 0; i < n; i++) sum = (sum + (long) arr[i] * left[i] * right[i]) % MOD;
    return (int) sum;
}
```

### Line by line

| Line | What it does |
|---|---|
| pass 1: pop while `arr[top] >= arr[i]` | the remaining top is **strictly smaller**: the previous smaller element |
| `left[i] = i − (that index, or −1)` | how many start positions there are (from just after the smaller element up to i) |
| pass 2: pop while `arr[top] > arr[i]` | the remaining top is **≤** arr[i]: the next smaller-or-equal element |
| `right[i] = (that index, or n) − i` | how many end positions there are |
| `sum += arr[i] · left[i] · right[i]` | arr[i]'s total contribution |

### Dry run: `[3, 1, 2, 4]`

| i | val | prev strictly smaller | next smaller-or-equal | left | right | contribution |
|---|---|---|---|---|---|---|
| 0 | 3 | −1 (none) | 1 | 1 | 1 | 3 × 1 = 3 |
| 1 | 1 | −1 | 4 (none) | 2 | 3 | 1 × 6 = 6 |
| 2 | 2 | 1 | 4 | 1 | 2 | 2 × 2 = 4 |
| 3 | 4 | 2 | 4 | 1 | 1 | 4 × 1 = 4 |

Sum = **17** ✓. Check: 1 is the min of 6 subarrays (any start in {0,1} × any end in {1,2,3}) ✓

### Dry run 2: `[2, 2]` (the duplicates fix)

| i | prev strictly smaller | next smaller-or-equal | left | right | contribution |
|---|---|---|---|---|---|
| 0 | −1 | 1 (equal!) | 1 | 1 | 2 |
| 1 | −1 (the 2 at index 0 is not strictly smaller) | 2 | 2 | 1 | 4 |

Total **6** ✓. `[2, 2]` is credited to index 1 only (index 0's range stops at the equal element). With strict `<` on **both** sides, both 2s would claim `[2, 2]` → 8 (wrong).

### Complexity
- **Time O(n):** two stack passes plus a sum.
- **Space O(n).**

---

## 6. Comparing the approaches

| | Brute force | Contribution + monotonic stack |
|---|---|---|
| Time | O(n²) | **O(n)** |
| Space | O(1) | O(n) |
| Handles duplicates | trivially | needs the `<` / `≤` asymmetry |

---

## 7. Edge cases and common mistakes

**Edge cases:** all equal values, strictly increasing/decreasing arrays, a single element, large values (overflow).

**Mistakes:**
1. The same strictness on both sides, which double-counts subarrays with equal minimums.
2. `int` overflow in `arr[i] * left * right`.
3. Forgetting `% MOD`, or applying it only at the end (the `long` would overflow for big inputs).
4. Using −1 / n incorrectly as the defaults (they're the virtual boundaries).

---

## 8. Interview questions and answers

**Q1. Explain the contribution technique.**
> Instead of summing over the subarrays, sum over the elements: each element contributes its value times the number of subarrays where it's the minimum. That count is (the choices for the left end) × (the choices for the right end), bounded by the nearest smaller elements.

**Q2. Why the asymmetric comparison?**
> When several equal values could be "the minimum" of a subarray, we must pick exactly one owner. A strict boundary on the left and a non-strict one on the right assigns each such subarray to the leftmost of the equal minimums.

### Follow-up questions

**F1. Sum of Subarray Ranges (LC 2104): the sum of (max − min) over all subarrays.**
> The sum of the maxima (the same technique with greater elements) minus the sum of the minima.

**F2. Largest Rectangle in Histogram.** → [Arrays H03](../../01-Arrays/04-Hard/H03_LargestRectangleInHistogram.md)
> The same nearest-smaller boundaries; the area = height × (right − left − 1).

**F3. Sum of the subarray minimums for subarrays of length ≤ k.**
> Cap the left and right counts at k, and subtract the over-long combinations (more involved counting).

**F4. Maximum Subarray Min-Product (LC 1856).**
> For each element as the minimum, use the boundaries and prefix sums to get the sum of its widest subarray.

---

## 9. Running the file

```bash
java M15_SumOfSubarrayMinimums.java      # 4/4 tests passed (including 500 random arrays with duplicates)
```

**One-line summary:** *"Each arr[i] is the min of left[i] × right[i] subarrays (the nearest strictly-smaller on the left, smaller-or-equal on the right); sum arr[i] × count."*
