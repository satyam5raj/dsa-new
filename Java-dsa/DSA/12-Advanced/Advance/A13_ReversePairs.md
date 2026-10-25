# A13 · Reverse Pairs ⭐

| | |
|---|---|
| **LeetCode** | [#493 Reverse Pairs](https://leetcode.com/problems/reverse-pairs/) |
| **Difficulty** | Hard |
| **Mix of structures** | **Merge sort** counting, or a **Fenwick tree** + **coordinate compression** + binary search |
| **Code** | [A13_ReversePairs.java](A13_ReversePairs.java) |
| **Run** | `java A13_ReversePairs.java` |

⭐ = not in `leetcode.md`. It uses the same two tools as [A12 Count of Range Sum](A12_CountOfRangeSum.md); doing both makes the pattern stick.

---

## 1. The problem

Count the pairs `i < j` with `nums[i] > 2 * nums[j]`.

```
[1,3,2,3,1]  →  2      pairs (index 1, index 4): 3 > 2·1,  (index 3, index 4): 3 > 2·1
[2,4,3,5,1]  →  3      4 > 2, 3 > 2, 5 > 2  (all against the final 1)
```

Up to 5 × 10⁴ numbers, anywhere in the `int` range.

---

## 2. How to think about it

### It's "count inversions" with a twist
Counting pairs `i < j` with `nums[i] > nums[j]` (inversions) is the classic merge-sort exercise. Here the condition is `nums[i] > 2·nums[j]`, but the trick still works.

### (A) Merge sort
Split the array in half. Pairs inside a half are counted recursively. For pairs with i in the **left** half and j in the **right** half, positions inside each half no longer matter, so both halves can be **sorted**. Then, for each left value x (ascending), count the right values y with `x > 2y`. Because the right half is sorted, those y's are a **prefix** of it, and as x grows the prefix only gets longer, so one pointer handles every x in O(n).

**Important:** count **before** merging, with its own pointer. The merge compares `x` with `y`, while the count compares `x` with `2y`; they're different conditions, so they can't share a loop.

### (B) Fenwick tree
Walk j from left to right. We need the number of **earlier** values greater than `2·nums[j]`:

```
(how many seen so far) − (how many seen are ≤ 2·nums[j])
```

Compress the values to ranks (they can be huge or negative), and a Fenwick tree counts "seen with rank ≤ r" in O(log n).

### Overflow
`2 * nums[j]` overflows when `nums[j]` is near ±2³¹. Write `2L * nums[j]`, which is computed in `long`.

### Dry run (A): `[2,4,3,5,1]`
| step | halves | cross pairs counted | sorted result |
|---|---|---|---|
| left half | [2] [4] | 2 > 8? no → 0 | [2, 4] |
| right half, inner | [5] [1] | 5 > 2 → **1** | [1, 5] |
| right half | [3] [1, 5] | 3 > 2 → **1**; 3 > 10? no | [1, 3, 5] |
| top | [2, 4] [1, 3, 5] | 2 > 2? no → 0; 4 > 2 → **1**, 4 > 6? no | [1, 2, 3, 4, 5] |

Total **3** ✓

### Dry run (B): the same array
Sorted distinct values `[1, 2, 3, 4, 5]`.

| j | nums[j] | 2·nums[j] | seen ≤ that | seen | earlier > 2·nums[j] |
|---|---|---|---|---|---|
| 0 | 2 | 4 | 0 | 0 | 0 |
| 1 | 4 | 8 | 1 | 1 | 0 |
| 2 | 3 | 6 | 2 | 2 | 0 |
| 3 | 5 | 10 | 3 | 3 | 0 |
| 4 | 1 | 2 | 1 (only the 2) | 4 | **3** |

Total **3** ✓

---

## 3. Java concepts used in this problem

### 3.1 `2L * x` vs `2 * x`
`2 * x` is `int` arithmetic and wraps around: `2 * 2147483647 = −2`. `2L * x` makes the left operand a `long`, so the whole multiplication happens in `long`. The comparison `a[i] > 2L * a[j]` then compares an `int` (widened) with a `long`. The file tests `[MAX, MAX]`, which would be counted wrongly with `int`.

### 3.2 `upperBound`: "how many values ≤ x"
```java
static int upperBound(long[] a, long x) {                             // number of values <= x
    int lo = 0, hi = a.length;
    while (lo < hi) { int mid = (lo + hi) >>> 1; if (a[mid] <= x) lo = mid + 1; else hi = mid; }
    return lo;
}
```
It's like `lowerBound` in [A12](A12_CountOfRangeSum.md#3-java-concepts-used-in-this-problem), but with `<=`, so it returns the number of elements ≤ x. With 1-based Fenwick positions, `upperBound(sorted, v)` is also the **position** of the value v itself.

### 3.3 `IntStream` → `LongStream`
```java
long[] sorted = Arrays.stream(nums).asLongStream().sorted().distinct().toArray();
```
`asLongStream()` converts the ints to longs, so the sorted array can be searched with `long` keys like `2L * nums[j]`.

### 3.4 `nums.clone()`
The merge sort reorders the array, so `reversePairs` sorts a copy and leaves the caller's array untouched.

---

## 4. Approach 1: Every pair (the referee)

### Code ([A13_ReversePairs.java:43-49](A13_ReversePairs.java#L43-L49))
```java
static int reversePairsBrute(int[] nums) {
    int count = 0;
    for (int i = 0; i < nums.length; i++)
        for (int j = i + 1; j < nums.length; j++)
            if ((long) nums[i] > 2L * nums[j]) count++;
    return count;
}
```

This follows the definition directly: **O(n²)**.

---

## 5. Approach 2: Merge sort (main solution)

### Code ([A13_ReversePairs.java:52-70](A13_ReversePairs.java#L52-L70))
```java
static int reversePairs(int[] nums) {
    int[] a = nums.clone();
    return sortCount(a, new int[a.length], 0, a.length);
}

private static int sortCount(int[] a, int[] tmp, int lo, int hi) {      // range [lo, hi)
    if (hi - lo <= 1) return 0;
    int mid = (lo + hi) >>> 1;
    int count = sortCount(a, tmp, lo, mid) + sortCount(a, tmp, mid, hi);
    int j = mid;
    for (int i = lo; i < mid; i++) {                                  // both halves are sorted now
        while (j < hi && a[i] > 2L * a[j]) j++;
        count += j - mid;                                             // a[mid..j) are all < a[i] / 2
    }
    int p = lo, q = mid, k = lo;                                      // standard merge
    while (p < mid || q < hi) tmp[k++] = (q >= hi || (p < mid && a[p] <= a[q])) ? a[p++] : a[q++];
    System.arraycopy(tmp, lo, a, lo, hi - lo);
    return count;
}
```

### Line by line
| Line | What it does |
|---|---|
| `hi − lo <= 1` | a single element has no pairs |
| the two recursive calls | count inside each half, and sort it |
| `j` starts at `mid` | the start of the sorted right half |
| `while (a[i] > 2L * a[j]) j++` | extend the prefix of right values small enough for `a[i]` |
| `count += j − mid` | every right value in `a[mid..j)` pairs with `a[i]` |
| why `j` never resets | the left half is ascending, so a bigger `a[i]` beats at least as many right values |
| the merge | a standard merge of two sorted halves into `tmp`, then copy back |

### Complexity
**O(n log n)** time: log n levels, each with O(n) counting and O(n) merging. **O(n)** extra space.

---

## 6. Approach 3: Fenwick tree + coordinate compression

### Code ([A13_ReversePairs.java:73-83](A13_ReversePairs.java#L73-L83))
```java
static int reversePairsFenwick(int[] nums) {
    long[] sorted = Arrays.stream(nums).asLongStream().sorted().distinct().toArray();
    int[] tree = new int[sorted.length + 1];
    int count = 0;
    for (int j = 0; j < nums.length; j++) {
        int atMost = upperBound(sorted, 2L * nums[j]);                // ranks whose value <= 2*nums[j]
        count += j - prefix(tree, atMost);                            // seen so far minus those small enough
        add(tree, upperBound(sorted, nums[j]));                       // rank of nums[j], 1-based
    }
    return count;
}
```

| Line | What it does |
|---|---|
| `sorted` | the distinct values; positions 1..d are their 1-based ranks |
| `atMost` | how many distinct values are ≤ 2·nums[j] = the Fenwick position to query up to |
| `j − prefix(atMost)` | j values have been seen; subtract those ≤ 2·nums[j] |
| `add(upperBound(nums[j]))` | record nums[j] at its 1-based rank, after the query |

`add` and `prefix` are the same small Fenwick tree as [A12](A12_CountOfRangeSum.md) ([lines 91-97](A13_ReversePairs.java#L91-L97)).

**Cost:** O(n log n).

---

## 7. Comparing the approaches

| | Time | Space |
|---|---|---|
| Every pair | O(n²) | O(1) |
| **Merge sort** | O(n log n) | O(n) |
| Fenwick + compression | O(n log n) | O(n) |

The file checks all three on 2,000 random arrays that include `Integer.MAX_VALUE` and `Integer.MIN_VALUE`.

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty array, negative numbers (−1 > 2·(−5) = −10 is a pair), extreme values, all equal values.

**Mistakes:**
1. `2 * nums[j]` in `int` (overflow).
2. Counting **inside** the merge loop with the merge's comparison (`x > y` instead of `x > 2y`).
3. Resetting the counting pointer for every `i` (O(n²) again).
4. In the Fenwick version, inserting before querying (pairs i = j).
5. Assuming non-negative input; with negatives, `x > 2y` doesn't follow from `x > y`.

---

## 9. Interview questions and answers

**Q1. Why does merge sort work for counting pairs?**
> Cross pairs only need "i is in the left half, j is in the right half". Sorting each half keeps that true, and sorted halves let a monotonic pointer count all cross pairs in linear time.

**Q2. Why count before merging instead of during?**
> The merge needs `x ≤ y` order, while the count needs `x > 2y`. A separate pass with its own pointer keeps both simple and correct.

**Q3. How would you do it with a BIT?**
> Compress the values, scan left to right, and for each element count earlier elements > 2·x as (seen) − (seen ≤ 2·x), using binary search to map 2·x into the compressed ranks.

### Follow-up questions

**F1. "Plain inversion count."**
> The same merge sort, counting `mid − p` whenever the right element is taken first in the merge.

**F2. "Count of smaller numbers after self (per element)."**
> Merge sort that carries original indices, or a BIT scanned right to left. See [Trees H05](../../07-Trees/04-Hard/H05_CountOfSmallerNumbersAfterSelf.md).

**F3. "Condition nums[i] > k·nums[j] for any k."**
> Replace 2 with k (in `long`). Both methods still work, as long as the counting pointer stays monotonic, which it does for k ≥ 0.

---

## 10. Running the file

```bash
java A13_ReversePairs.java     # 6/6 tests passed (examples, Fenwick, overflow, negatives, 2,000 random arrays)
```

**One-line summary:** *"Merge sort, and before merging each pair of sorted halves, slide one pointer through the right half to count values y with x > 2y for each left x (using long arithmetic); or scan with a Fenwick tree over compressed values, counting earlier elements greater than 2·x."*
