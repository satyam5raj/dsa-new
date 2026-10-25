# A12 · Count of Range Sum ⭐

| | |
|---|---|
| **LeetCode** | [#327 Count of Range Sum](https://leetcode.com/problems/count-of-range-sum/) |
| **Difficulty** | Hard |
| **Mix of structures** | **Prefix sums** + either **merge sort** counting or a **Fenwick tree** with **coordinate compression** (+ binary search) |
| **Code** | [A12_CountOfRangeSum.java](A12_CountOfRangeSum.java) |
| **Run** | `java A12_CountOfRangeSum.java` |

⭐ = not in `leetcode.md`, added because it's the classic "prefix sums + sorted counting" combination.

---

## 1. The problem

Given `nums`, `lower` and `upper`, count the subarrays `nums[i..j]` whose sum lies in `[lower, upper]` (inclusive).

```
nums = [-2, 5, -1], lower = -2, upper = 2   →  3
   subarrays: [-2] = -2 ✓   [5] = 5   [-1] = -1 ✓   [-2,5] = 3   [5,-1] = 4   [-2,5,-1] = 2 ✓
```

Up to 10⁵ numbers, so O(n²) is too slow.

---

## 2. How to think about it

### Step 1: prefix sums turn subarrays into pairs
```
pre[0] = 0,   pre[k] = nums[0] + … + nums[k−1]
sum(i..j) = pre[j+1] − pre[i]
```
So the question becomes: **count pairs a < b with `lower ≤ pre[b] − pre[a] ≤ upper`**. Rearranged:

```
pre[b] − upper  ≤  pre[a]  ≤  pre[b] − lower
```
For each b, count the **earlier** prefix values inside a window. (Use `long`: 10⁵ values of 2³¹ overflow an `int`.)

### Step 2: counting "earlier values in a window" fast
This is the same shape as [Count of Smaller Numbers After Self](../../07-Trees/04-Hard/H05_CountOfSmallerNumbersAfterSelf.md). There are two classic tools.

#### (A) Merge sort
Split `pre` in half. Pairs with both ends in one half are counted recursively. For pairs with `a` in the **left** half and `b` in the **right** half, the only thing that matters is "a is left of b", which is already guaranteed, so **both halves can be sorted**.

With both halves sorted, for each left value x (ascending), the right values y with `x + lower ≤ y ≤ x + upper` form a **contiguous window**, and as x grows the window only slides right. Two pointers count every pair in O(n) per level. Then merge, as usual.

#### (B) Fenwick tree (Binary Indexed Tree)
Walk b from left to right, keeping a structure of the prefix values seen so far. For each `pre[b]`, ask "how many seen values lie in `[pre[b] − upper, pre[b] − lower]`?", then insert `pre[b]`.

The values are huge and can be negative, so first **compress** them: sort the distinct prefix values and replace each with its **rank** (index in that sorted list). A Fenwick tree over ranks counts "how many seen with rank < i" in O(log n). Binary search turns the value window into a rank window.

### Dry run (B): `nums [-2,5,-1]`, `[-2, 2]`
`pre = [0, −2, 3, 2]`; sorted distinct = `[−2, 0, 2, 3]` (ranks 0..3).

| b | pre[b] | window [pre−2, pre+2] | seen so far | count | insert |
|---|---|---|---|---|---|
| 0 | 0 | [−2, 2] | {} | 0 | 0 |
| 1 | −2 | [−4, 0] | {0} | **1** (0) | −2 |
| 2 | 3 | [1, 5] | {0, −2} | 0 | 3 |
| 3 | 2 | [0, 4] | {0, −2, 3} | **2** (0, 3) | 2 |

Total **3** ✓ (b = 1 is the subarray [−2]; b = 3 pairs with 0 → [−2,5,−1] and with 3 → [−1].)

### Dry run (A): merge sort on `pre = [0, −2, 3, 2]`
| split | pairs counted | sorted result |
|---|---|---|
| [0] [−2] | −2 − 0 = −2 ∈ [−2,2] → **1** | [−2, 0] |
| [3] [2] | 2 − 3 = −1 → **1** | [2, 3] |
| [−2, 0] [2, 3] | x = −2: y ∈ [−4, 0] → none; x = 0: y ∈ [−2, 2] → {2} → **1** | [−2, 0, 2, 3] |

Total **3** ✓

---

## 3. Java concepts used in this problem

### 3.1 `long` everywhere for sums
`long[] pre` and `long sum`: the file's test `[MAX_VALUE, 1]` would wrap to `MIN_VALUE` with `int` and be wrongly counted inside `[MIN_VALUE, 0]`.

### 3.2 Coordinate compression with streams
```java
long[] sorted = Arrays.stream(pre).sorted().distinct().toArray();
```
`Arrays.stream(long[])` gives a `LongStream`; `sorted().distinct()` gives the distinct values in order. A value's rank is its index here, found by binary search.

### 3.3 `lowerBound`: binary search for "first ≥ x"
```java
static int lowerBound(long[] a, long x) {
    int lo = 0, hi = a.length;
    while (lo < hi) { int mid = (lo + hi) >>> 1; if (a[mid] < x) lo = mid + 1; else hi = mid; }
    return lo;
}
```
It returns the number of elements **< x**, which is also the index of the first element ≥ x. Then "values ≤ y" = `lowerBound(y + 1)` for integers. `>>> 1` is an unsigned shift: it halves without overflow even if `lo + hi` exceeds `int` range.

### 3.4 The Fenwick tree in a few lines
```java
static void add(int[] tree, int i) { for (; i < tree.length; i += i & -i) tree[i]++; }

static int prefix(int[] tree, int i) {                               // how many seen with rank < i (0-based)
    int s = 0;
    for (; i > 0; i -= i & -i) s += tree[i];
    return s;
}
```
`i & -i` is the lowest set bit of i. `add` climbs to every node that covers position i; `prefix(i)` sums positions 1..i by jumping down. Both are O(log n). It's explained in full in [Trees M16](../../07-Trees/03-Medium/M16_RangeSumQueryMutable.md). Here rank r is stored at position r + 1 (the tree is 1-indexed), so `prefix(tree, r)` = how many seen values have rank < r.

### 3.5 `System.arraycopy`
`System.arraycopy(src, srcPos, dest, destPos, length)` copies a block of an array quickly; the merge uses it to copy the merged range back.

---

## 4. Approach 1: Every subarray (the referee)

### Code ([A12_CountOfRangeSum.java:46-56](A12_CountOfRangeSum.java#L46-L56))
```java
static int countRangeSumBrute(int[] nums, int lower, int upper) {
    int count = 0;
    for (int i = 0; i < nums.length; i++) {
        long sum = 0;
        for (int j = i; j < nums.length; j++) {
            sum += nums[j];
            if (lower <= sum && sum <= upper) count++;
        }
    }
    return count;
}
```

A running sum per start i: **O(n²)**.

---

## 5. Approach 2: Merge sort on prefix sums

### Code ([A12_CountOfRangeSum.java:59-79](A12_CountOfRangeSum.java#L59-L79))
```java
static int countRangeSumMerge(int[] nums, int lower, int upper) {
    long[] pre = new long[nums.length + 1];
    for (int i = 0; i < nums.length; i++) pre[i + 1] = pre[i] + nums[i];
    return (int) sortCount(pre, new long[pre.length], 0, pre.length, lower, upper);
}

private static long sortCount(long[] s, long[] tmp, int lo, int hi, int lower, int upper) {   // range [lo, hi)
    if (hi - lo <= 1) return 0;
    int mid = (lo + hi) >>> 1;
    long count = sortCount(s, tmp, lo, mid, lower, upper) + sortCount(s, tmp, mid, hi, lower, upper);
    int from = mid, to = mid;                                        // window [from, to) in the sorted right half
    for (int i = lo; i < mid; i++) {
        while (from < hi && s[from] - s[i] < lower) from++;
        while (to < hi && s[to] - s[i] <= upper) to++;
        count += to - from;
    }
    int a = lo, b = mid, k = lo;                                     // standard merge
    while (a < mid || b < hi) tmp[k++] = (b >= hi || (a < mid && s[a] <= s[b])) ? s[a++] : s[b++];
    System.arraycopy(tmp, lo, s, lo, hi - lo);
    return count;
}
```

### Line by line
| Line | What it does |
|---|---|
| `pre` | n + 1 prefix sums, starting with 0 |
| `hi − lo <= 1` | one element: no pairs |
| the two recursive calls | count and **sort** each half |
| `from` | first right value with `y − x ≥ lower` |
| `to` | first right value with `y − x > upper` |
| `count += to − from` | right values in the window for this x |
| why the pointers never move back | the left half is sorted ascending, so as x grows both bounds grow |
| the merge | combine the two sorted halves (see [Recursion M04](../../13-Recursion/03-Medium/M04_SortAnArray.md)) |

**Cost:** O(n log n) time, O(n) extra space.

---

## 6. Approach 3: Fenwick tree + coordinate compression (main solution)

### Code ([A12_CountOfRangeSum.java:82-96](A12_CountOfRangeSum.java#L82-L96))
```java
static int countRangeSum(int[] nums, int lower, int upper) {
    long[] pre = new long[nums.length + 1];
    for (int i = 0; i < nums.length; i++) pre[i + 1] = pre[i] + nums[i];
    long[] sorted = Arrays.stream(pre).sorted().distinct().toArray();
    int[] tree = new int[sorted.length + 1];                         // Fenwick tree, 1-indexed by rank

    long count = 0;
    for (long p : pre) {
        int l = lowerBound(sorted, p - upper);                       // first rank with value >= p - upper
        int r = lowerBound(sorted, p - lower + 1);                   // first rank with value >  p - lower
        count += prefix(tree, r) - prefix(tree, l);                  // seen values with rank in [l, r)
        add(tree, lowerBound(sorted, p) + 1);
    }
    return (int) count;
}
```

### Line by line
| Line | What it does |
|---|---|
| `sorted` | every distinct prefix value in order; rank = index |
| `tree` | `tree` position r + 1 counts the seen values of rank r |
| `l` | ranks below l are too small (< p − upper) |
| `r` | ranks from r up are too big (> p − lower) |
| `prefix(r) − prefix(l)` | seen values with rank in [l, r): exactly those in the window |
| query **before** insert | so a value never pairs with itself (a must be < b) |
| `add(rank + 1)` | the Fenwick tree is 1-indexed |

**Cost:** O(n log n) time (sort + n binary searches + n Fenwick operations), O(n) space.

---

## 7. Comparing the approaches

| | Time | Space | Notes |
|---|---|---|---|
| Every subarray | O(n²) | O(1) | referee |
| Merge sort | O(n log n) | O(n) | no extra structure, but modifies a copy |
| **Fenwick + compression** | O(n log n) | O(n) | processes left to right, so it also works as a stream if values are known in advance |

The file checks all three on 2,000 random arrays.

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty array (0), `lower == upper`, all zeros (every subarray counts), sums overflowing `int`.

**Mistakes:**
1. Forgetting `pre[0] = 0`, which misses subarrays starting at index 0.
2. `int` prefix sums (overflow).
3. Inserting before querying in the Fenwick loop (pairs a value with itself).
4. Off-by-one in the window bounds (`< lower` vs `<= upper`).
5. In the merge version, merging **before** counting (the halves must be sorted but still separate when you count cross pairs).

---

## 9. Interview questions and answers

**Q1. How do prefix sums help?**
> A subarray sum becomes a difference `pre[b] − pre[a]`, so the problem becomes counting pairs (a < b) whose difference lies in a range, which sorted structures can count quickly.

**Q2. Why can merge sort count these pairs?**
> For pairs crossing the midpoint, only "left half vs right half" matters, not positions inside each half. So both halves can be sorted, and then the valid right values for each left value form a sliding window.

**Q3. Why coordinate compression?**
> A Fenwick tree needs small integer indices, but prefix sums can be huge or negative. Replacing each value with its rank among the distinct values keeps the order and makes indices 0..n.

### Follow-up questions

**F1. "Count subarrays with sum exactly k."**
> That's easier: a HashMap of prefix-sum counts ([Arrays M07](../../01-Arrays/03-Medium/M07_SubarraySumEqualsK.md)), O(n).

**F2. "Count pairs with nums[i] > 2·nums[j]."**
> The same merge-sort-counting idea: [A13 Reverse Pairs](A13_ReversePairs.md).

**F3. "Values arrive online and aren't known in advance."**
> Compression needs all values first. Use a balanced BST with subtree sizes, or a dynamic segment tree over the value range.

---

## 10. Running the file

```bash
java A12_CountOfRangeSum.java     # 5/5 tests passed (example both ways, single zero, overflow case, 2,000 random arrays)
```

**One-line summary:** *"Turn subarray sums into differences of prefix sums, then for each prefix count the earlier prefixes inside [pre − upper, pre − lower], either with merge sort (sorted halves plus a sliding two-pointer window) or with a Fenwick tree over compressed ranks."*
