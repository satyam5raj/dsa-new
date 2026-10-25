# A24 · Find K-th Smallest Pair Distance ⭐

| | |
|---|---|
| **LeetCode** | [#719 Find K-th Smallest Pair Distance](https://leetcode.com/problems/find-k-th-smallest-pair-distance/) |
| **Difficulty** | Hard |
| **Mix of structures** | **Sorting** + **binary search on the answer** + **two pointers** to count (or a **min-heap** k-way merge) |
| **Code** | [A24_KthSmallestPairDistance.java](A24_KthSmallestPairDistance.java) |
| **Run** | `java A24_KthSmallestPairDistance.java` |

⭐ = not in `leetcode.md`. It's the second "binary search on the answer" problem, after [A23](A23_SplitArrayLargestSum.md), this time with a counting step instead of a greedy one.

---

## 1. The problem

The distance of a pair `(i < j)` is `|nums[i] − nums[j]|`. Return the **k-th smallest** distance among all `n(n−1)/2` pairs.

```
nums = [1,3,1], k = 1  →  0       distances: |1−3| = 2, |1−1| = 0, |3−1| = 2  → sorted 0, 2, 2
nums = [1,1,1], k = 2  →  0
nums = [1,6,1], k = 3  →  5       distances 5, 0, 5 → sorted 0, 5, 5
```

n can be 10⁴, so there are up to 5 × 10⁷ pairs.

---

## 2. How to think about it

### Brute force
List all ~n²/2 distances and sort: 5 × 10⁷ numbers. That's too slow and too much memory.

### Binary search on the answer
Instead of finding the k-th distance directly, ask:

> **How many pairs have distance ≤ D?**

That count only grows as D grows. So the answer is the **smallest D whose count is ≥ k**, found by binary search over D in `[0, max − min]`.

### Counting pairs ≤ D in O(n) with two pointers
Sort the array first. For each right end `r`, the valid left partners form a contiguous block `[left, r)`:

```
for each r:  move left forward while a[r] − a[left] > D
             count += r − left              (every index in [left, r) is within D of a[r])
```
As `r` grows, `left` never moves back, so the whole count is O(n).

### The heap alternative
After sorting, for each i the distances to `i+1, i+2, …` increase: n sorted lists. Merge them with a min-heap and pop k times. That's **O((n + k) log n)**: great when k is small, terrible when k is close to n².

### Dry run: `[1,6,1]`, k = 3 → sorted `[1,1,6]`, lo = 0, hi = 5
| lo | hi | mid | pairs ≤ mid (two pointers) | action |
|---|---|---|---|---|
| 0 | 5 | 2 | r=1: left 0 → +1; r=2: 6−1 > 2 → left = 2 → +0; total **1** | 1 < 3 → lo = 3 |
| 3 | 5 | 4 | the same → 1 | lo = 5 |
| 5 | 5 | | | **5** |

---

## 3. Java concepts used in this problem

### 3.1 `>>> 1` for the midpoint
`(lo + hi) >>> 1` halves using an unsigned shift. It's safe even if `lo + hi` overflows into the sign bit (not possible here, but it's a good habit).

### 3.2 `long` for the pair count
`pairsWithin` returns a `long`: with n = 10⁴ the count can reach 5 × 10⁷, which fits in an `int`, but counts like this grow as n² and overflow quickly in variants.

### 3.3 A heap ordered by values in another array
```java
PriorityQueue<int[]> heap = new PriorityQueue<>((x, y) -> Integer.compare(a[x[1]] - a[x[0]], a[y[1]] - a[y[0]]));   // {i, j}
```
Entries are index pairs `{i, j}`; the comparator computes their distance from the sorted array `a`.

### 3.4 `nums.clone()` before sorting
Both approaches sort a copy, so the caller's array isn't reordered.

---

## 4. The referee: every distance, sorted

### Code ([A24_KthSmallestPairDistance.java:44-50](A24_KthSmallestPairDistance.java#L44-L50))
```java
static int smallestDistancePairBrute(int[] nums, int k) {
    List<Integer> d = new ArrayList<>();
    for (int i = 0; i < nums.length; i++)
        for (int j = i + 1; j < nums.length; j++) d.add(Math.abs(nums[i] - nums[j]));
    Collections.sort(d);
    return d.get(k - 1);
}
```

This follows the definition directly: **O(n² log n)** time, O(n²) space.

---

## 5. Approach 1: Sort + min-heap merging n sorted lists

### Code ([A24_KthSmallestPairDistance.java:53-64](A24_KthSmallestPairDistance.java#L53-L64))
```java
static int smallestDistancePairHeap(int[] nums, int k) {
    int[] a = nums.clone();
    Arrays.sort(a);
    PriorityQueue<int[]> heap = new PriorityQueue<>((x, y) -> Integer.compare(a[x[1]] - a[x[0]], a[y[1]] - a[y[0]]));   // {i, j}
    for (int i = 0; i + 1 < a.length; i++) heap.add(new int[]{i, i + 1});   // the closest partner of each i
    int[] cur = null;
    for (int popped = 0; popped < k; popped++) {
        cur = heap.poll();
        if (cur[1] + 1 < a.length) heap.add(new int[]{cur[0], cur[1] + 1});   // next partner of the same i
    }
    return a[cur[1]] - a[cur[0]];
}
```

| Line | What it does |
|---|---|
| seed `(i, i+1)` | each list's smallest element |
| pop | the globally smallest remaining distance |
| push `(i, j+1)` | the next element of the same list |
| after k pops | `cur` is the k-th smallest pair |

This is the same pattern as [Kth Smallest Element in a Sorted Matrix](../../08-Heap/03-Medium/M06_KthSmallestInSortedMatrix.md). **O((n + k) log n).**

---

## 6. Approach 2: Binary search on the distance + two pointers (main solution)

### Code ([A24_KthSmallestPairDistance.java:67-88](A24_KthSmallestPairDistance.java#L67-L88))
```java
static int smallestDistancePair(int[] nums, int k) {
    int[] a = nums.clone();
    Arrays.sort(a);
    int lo = 0, hi = a[a.length - 1] - a[0];
    while (lo < hi) {
        int mid = (lo + hi) >>> 1;
        if (pairsWithin(a, mid) >= k) hi = mid;                       // at least k pairs are <= mid
        else lo = mid + 1;
    }
    return lo;
}

/** Number of pairs with distance <= d in a sorted array. */
static long pairsWithin(int[] a, int d) {
    long count = 0;
    int left = 0;
    for (int right = 0; right < a.length; right++) {
        while (a[right] - a[left] > d) left++;
        count += right - left;                                        // a[left..right-1] are all within d of a[right]
    }
    return count;
}
```

### Line by line
| Line | What it does |
|---|---|
| `hi = max − min` | the largest possible distance |
| `pairsWithin(mid) >= k` | the k-th distance is ≤ mid |
| `lo = mid + 1` | fewer than k pairs are ≤ mid, so the answer is bigger |
| `while (a[right] − a[left] > d)` | shrink from the left until the window fits |
| `count += right − left` | pairs ending at `right` |

### Why the answer is always a real distance
The search finds the smallest D with `count(D) ≥ k`. If D weren't an actual pair distance, then `count(D − 1) = count(D) ≥ k`, contradicting minimality. So the binary search always lands on a real distance.

### Complexity
- Sort: O(n log n). Search: log₂(max − min) rounds × O(n).
- **O(n log n + n log W)** for value range W; O(1) extra space.

---

## 7. Comparing the approaches

| | Time | Best when |
|---|---|---|
| All pairs | O(n² log n) | tiny n |
| Heap merge | O((n + k) log n) | k small |
| **Binary search + two pointers** | **O(n log n + n log W)** | always |

The file checks all three on 2,000 random cases with every possible k.

---

## 8. Edge cases and common mistakes

**Edge cases:** many duplicates (distance 0 pairs), k = 1 (the smallest), k = n(n−1)/2 (the largest), two elements.

**Mistakes:**
1. Forgetting to sort, so the two-pointer count is wrong.
2. Counting with `>=` vs `>` mixed up in the window condition.
3. `count > k` instead of `count >= k` in the search.
4. Resetting `left` for each `right` (O(n²) per count).
5. Building all distances "just to be safe" (memory blow-up).

---

## 9. Interview questions and answers

**Q1. Why binary search over distances rather than over indices?**
> We can't index the k-th distance directly, but we can count how many distances are ≤ D quickly, and that count is monotonic in D. Searching on the value turns a selection problem into counting problems.

**Q2. How do you count pairs ≤ D quickly?**
> Sort, then use a sliding window: for each right end, the left end only moves forward, so all counts take O(n).

**Q3. When would the heap be better?**
> When k is small: O((n + k) log n) can beat the O(n log W) search if k is much smaller than n log W.

### Follow-up questions

**F1. "K-th smallest element in a sorted matrix."**
> The same two choices: a heap merge, or binary search on the value with a staircase count from a corner.

**F2. "K-th smallest product/sum of pairs."**
> Binary search on the value, with a counting function (two pointers for sums; careful handling of signs for products).

**F3. "Many queries with different k."**
> Precompute counts over a distance histogram if values are small (bucket counting); otherwise answer each query with a binary search.

---

## 10. Running the file

```bash
java A24_KthSmallestPairDistance.java     # 6/6 tests passed (3 examples, heap version, the largest pair, 2,000 random cases)
```

**One-line summary:** *"Sort, then binary-search the distance D: a two-pointer pass counts the pairs within D in O(n), and the smallest D with at least k pairs is the answer; a min-heap merge of each element's partners works when k is small."*
