# H05 · Count of Smaller Numbers After Self ⭐

| | |
|---|---|
| **LeetCode** | [#315 Count of Smaller Numbers After Self](https://leetcode.com/problems/count-of-smaller-numbers-after-self/) |
| **Difficulty** | Hard |
| **Pattern** | A Fenwick tree over values (+ coordinate compression), scanning right to left |
| **Code** | [H05_CountOfSmallerNumbersAfterSelf.java](H05_CountOfSmallerNumbersAfterSelf.java) |
| **Run** | `java H05_CountOfSmallerNumbersAfterSelf.java` |

⭐ = a bonus classic. Fenwick basics: [M16 Range Sum Query - Mutable](../03-Medium/M16_RangeSumQueryMutable.md).

---

## 1. The problem

`counts[i]` = how many elements to the **right** of nums[i] are **smaller** than it.

```
[5, 2, 6, 1]  →  [2, 1, 1, 0]
  5: {2, 1} are smaller → 2
  2: {1}               → 1
  6: {1}               → 1
  1: nothing           → 0
```

| nums | counts |
|---|---|
| `[5,2,6,1]` | `[2,1,1,0]` |
| `[-1]` | `[0]` |
| `[-1,-1]` | `[0,0]` (equal is not smaller) |
| `[3,2,1,0]` | `[3,2,1,0]` |

**Constraints:** `1 ≤ n ≤ 10⁵`, `−10⁴ ≤ nums[i] ≤ 10⁴`.

---

## 2. How to think about it

### Brute force
For each i, scan everything to its right: O(n²), about 5 · 10⁹ comparisons for n = 10⁵.

### Scan right to left with a "bag" of seen values
The values already seen are exactly the elements to the right of i. For each nums[i]:
- `counts[i]` = how many values in the bag are **< nums[i]** → a **prefix count** query
- then put nums[i] into the bag → a **point update**

"Prefix count + point update", both in O(log n), is exactly a **Fenwick tree** indexed by **value**: `tree[v]` = how many times v is in the bag.

### Coordinate compression
Values range from −10⁴ to 10⁴ (up to 10⁹ in other problems): too big or negative for array indices. Only their **order** matters, so replace each value by its **rank** among the distinct sorted values: 1, 2, 3, …

```
[5, 2, 6, 1] → sorted distinct [1, 2, 5, 6] → ranks [3, 2, 4, 1]
```

### Dry run (ranks [3, 2, 4, 1]), right to left
```
i = 3, rank 1: count(ranks < 1) = 0             add 1     bag {1}
i = 2, rank 4: count(< 4) = 1                   add 4     bag {1, 4}
i = 1, rank 2: count(< 2) = 1                   add 2     bag {1, 2, 4}
i = 0, rank 3: count(< 3) = 2 (ranks 1 and 2)   add 3
→ [2, 1, 1, 0]
```

### Other routes
- **Merge sort**: count how many elements from the right half jump over each left element (like counting inversions).
- **A BST with subtree sizes** ([M04](../03-Medium/M04_KthSmallestElementInBST.md)'s follow-up).

The Fenwick tree is the shortest to write.

---

## 3. Java concepts used in this problem

### 3.1 Streams for compression: `distinct().sorted().toArray()`

```java
int[] sorted = Arrays.stream(nums).distinct().sorted().toArray();
```

An `IntStream` pipeline: remove duplicates, sort, and collect back into an `int[]`. That's the list of distinct values in increasing order.

### 3.2 `Arrays.binarySearch` for the rank

```java
int rank = Arrays.binarySearch(sorted, nums[i]) + 1;     // 1-based rank
```

On a sorted array, it returns the index of the value (the value is guaranteed to be present here) in O(log m). The `+ 1` gives Fenwick's 1-based index.

### 3.3 "Strictly smaller" = prefix up to rank − 1

```java
for (int x = rank - 1; x > 0; x -= x & -x) smaller += tree[x];
```

Querying ranks 1..rank−1 excludes equal values automatically, which is why `[-1, -1]` gives `[0, 0]`.

### 3.4 `Integer[]` → `List<Integer>`

```java
Integer[] counts = new Integer[nums.length];
return Arrays.asList(counts);
```

`Arrays.asList` wraps an **object** array as a fixed-size List. (It doesn't work the same way on an `int[]`: that would become a `List<int[]>` with one element.)

---

## 4. Approach 1: Brute force

### Code ([H05_CountOfSmallerNumbersAfterSelf.java:64-72](H05_CountOfSmallerNumbersAfterSelf.java#L64-L72))

```java
static List<Integer> brute(int[] nums) {
    List<Integer> out = new ArrayList<>();
    for (int i = 0; i < nums.length; i++) {
        int c = 0;
        for (int j = i + 1; j < nums.length; j++) if (nums[j] < nums[i]) c++;
        out.add(c);
    }
    return out;
}
```

**O(n²).** It's the file's oracle on small random arrays.

---

## 5. Approach 2: Merge sort, counting the right-half elements that jump over each left element

```java
static int[] cnt, idx, tmp;

static List<Integer> countSmallerMerge(int[] nums) {
    int n = nums.length;
    cnt = new int[n]; idx = new int[n]; tmp = new int[n];
    for (int i = 0; i < n; i++) idx[i] = i;
    sort(nums, 0, n - 1);
    List<Integer> out = new ArrayList<>();
    for (int c : cnt) out.add(c);
    return out;
}

static void sort(int[] a, int lo, int hi) {
    if (lo >= hi) return;
    int mid = (lo + hi) >>> 1;
    sort(a, lo, mid);
    sort(a, mid + 1, hi);
    int i = lo, j = mid + 1, k = lo, movedRight = 0;
    while (i <= mid || j <= hi) {
        if (j > hi || (i <= mid && a[idx[i]] <= a[idx[j]])) {
            cnt[idx[i]] += movedRight;           // right-half elements smaller than a[idx[i]]
            tmp[k++] = idx[i++];
        } else {
            movedRight++;                        // a right-half element goes before the remaining left ones
            tmp[k++] = idx[j++];
        }
    }
    for (k = lo; k <= hi; k++) idx[k] = tmp[k];
}
```

| Line | What it does |
|---|---|
| `idx` | sort the **indices** (not the values), so we know which original element each count belongs to |
| `movedRight` | how many right-half elements have been placed so far; they're all smaller than the current left element |
| `<=` in the comparison | equal values go left first, so equals aren't counted as smaller |
| `cnt[idx[i]] += movedRight` | when a left element is placed, every right element placed before it was smaller and came from its right |

Every pair (i < j) with nums[j] < nums[i] is counted exactly once, in the merge where they're first in different halves. **O(n log n).** (Stress-tested against brute force on 3,000 random arrays.)

---

## 6. Approach 3: Fenwick tree over ranks (the file's solution)

### Code ([H05_CountOfSmallerNumbersAfterSelf.java:47-62](H05_CountOfSmallerNumbersAfterSelf.java#L47-L62))

```java
static List<Integer> countSmaller(int[] nums) {
    // 1. coordinate compression: value -> rank 1..m
    int[] sorted = Arrays.stream(nums).distinct().sorted().toArray();
    int m = sorted.length;

    int[] tree = new int[m + 1];                     // Fenwick tree over ranks
    Integer[] counts = new Integer[nums.length];
    for (int i = nums.length - 1; i >= 0; i--) {     // 2. right to left
        int rank = Arrays.binarySearch(sorted, nums[i]) + 1;
        int smaller = 0;
        for (int x = rank - 1; x > 0; x -= x & -x) smaller += tree[x];   // count of ranks < rank
        counts[i] = smaller;
        for (int x = rank; x <= m; x += x & -x) tree[x]++;              // add this value to the bag
    }
    return Arrays.asList(counts);
}
```

### Line by line

| Line | What it does |
|---|---|
| `sorted` | the distinct values in increasing order |
| `tree = new int[m + 1]` | a Fenwick tree with one slot per rank (1-indexed) |
| `i` from n − 1 down to 0 | the bag holds exactly the elements to the right of i |
| `rank = binarySearch + 1` | nums[i]'s position among the distinct values |
| the query loop | the number of bag values with rank ≤ rank − 1 (strictly smaller) |
| the update loop | add one occurrence of this rank to the bag |

### Dry run: `[5, 2, 6, 1]` (ranks 3, 2, 4, 1; m = 4)

| i | value | rank | query ranks < rank | counts[i] | tree after the update (indices 1..4) |
|---|---|---|---|---|---|
| 3 | 1 | 1 | prefix(0) = 0 | 0 | add at 1 → 2 → 4: [1, 1, 0, 1] |
| 2 | 6 | 4 | prefix(3) = tree[3] + tree[2] = 0 + 1 = 1 | 1 | add at 4: [1, 1, 0, 2] |
| 1 | 2 | 2 | prefix(1) = tree[1] = 1 | 1 | add at 2 → 4: [1, 2, 0, 3] |
| 0 | 5 | 3 | prefix(2) = tree[2] = 2 | 2 | add at 3 → 4: [1, 2, 1, 4] |

→ **[2, 1, 1, 0]** ✓

### Complexity
- **Time O(n log n):** the sort, plus two O(log m) Fenwick loops and a binary search per element.
- **Space O(n).**

The file runs n = 100,000 in a few tens of milliseconds.

---

## 7. Comparing the approaches

| | Brute force | Merge sort | Fenwick over ranks |
|---|---|---|---|
| Time | O(n²) | O(n log n) | O(n log n) |
| Space | O(1) | O(n) | O(n) |
| Code | trivial | tricky indexing | short |

---

## 8. Edge cases and common mistakes

**Edge cases:** a single element, all equal (all zeros), descending input (the maximum counts), negative values.

**Mistakes:**
1. Counting ≤ instead of < (querying up to `rank` instead of `rank − 1`).
2. Scanning left to right (that counts the elements to the **left**).
3. Using raw values as Fenwick indices (negative or huge).
4. In merge sort, sorting the values instead of the indices (the counts lose their owners).

---

## 9. Interview questions and answers

**Q1. Why scan from right to left?**
> Then the data structure contains exactly the elements to the right of the current one, and each answer is a single query.

**Q2. What is coordinate compression and why is it safe?**
> Replace each value with its rank among the distinct values. "Smaller than" only depends on the order, which ranks preserve, and the ranks fit into a small array.

**Q3. How does this relate to counting inversions?**
> The sum of all `counts[i]` is the number of inversions in the array, i.e. the number of pairs (i < j) with nums[i] > nums[j].

### Follow-up questions

**F1. Count Good Triplets / Reverse Pairs (LC 493: nums[i] > 2 · nums[j]).**
> The same merge-sort or Fenwick idea with a modified condition (for Fenwick, compress `2 · nums[j]` along with the values).

**F2. Count the larger numbers before self.**
> Scan left to right and query the count of ranks > rank (the total minus the prefix).

**F3. Range-count queries online.**
> A Fenwick tree over the compressed values, or a merge-sort tree / wavelet tree for offline range queries.

---

## 10. Running the file

```bash
java H05_CountOfSmallerNumbersAfterSelf.java      # 6/6 tests passed (including 500 random arrays vs O(n²), n = 100,000 timed)
```

**One-line summary:** *"Compress the values to ranks, scan from right to left, and for each element query a Fenwick tree for the count of ranks below it, then add its own rank."*
