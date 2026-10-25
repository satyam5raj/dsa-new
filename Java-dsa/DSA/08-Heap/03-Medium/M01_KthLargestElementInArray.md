# M01 · Kth Largest Element in an Array

| | |
|---|---|
| **LeetCode** | [#215 Kth Largest Element in an Array](https://leetcode.com/problems/kth-largest-element-in-an-array/) |
| **Difficulty** | Medium |
| **Pattern** | A size-k min-heap vs Quickselect (O(n) on average) |
| **Code** | [M01_KthLargestElementInArray.java](M01_KthLargestElementInArray.java) |
| **Run** | `java M01_KthLargestElementInArray.java` |

PriorityQueue basics are explained in [E01](../02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Return the **k-th largest** element in sorted order (**not** the k-th distinct).

| nums | k | answer |
|---|---|---|
| `[3,2,1,5,6,4]` | 2 | 5 |
| `[3,2,3,1,2,4,5,5,6]` | 4 | 4 (sorted descending: 6 5 5 **4** 3 3 2 2 1) |
| `[1]` | 1 | 1 |

**Constraints:** `1 ≤ k ≤ n ≤ 10⁵`.

---

## 2. How to think about it

### 1. Sort
`sort(nums); return nums[n − k];`: O(n log n). Fine, but the interviewer wants better.

### 2. A min-heap of size k
[E01](../02-Easy/E01_KthLargestInStream.md) on a fixed array: O(n log k) time, O(k) space. Great when k is small or the data arrives as a stream.

### 3. Quickselect: O(n) on average, O(1) extra space
Quicksort's **partition** puts a pivot at its final sorted position p, with the smaller elements to its left and the larger ones to its right. The k-th largest sits at sorted index **target = n − k**. Then:
- p == target → done
- p < target → the answer is in the **right** part only
- p > target → the **left** part only

Unlike quicksort, it recurses into **one** side: n + n/2 + n/4 + … ≈ 2n on average.

**The worst case is O(n²)** with bad pivots (e.g. always the maximum), so pick a **random** pivot. Also, arrays with many duplicates make a 2-way partition slow: use a **3-way partition** (less / equal / greater, the Dutch national flag), which the file does.

### Dry run (suppose the random pivot is 4, then 5)
```
[3, 2, 1, 5, 6, 4], k = 2 → target index 4
pivot 4: → [3, 2, 1 | 4 | 6, 5]   the equal block is index 3 < 4 → go right: indices 4..5
pivot 5: → [ … 4 | 5 | 6]         the equal block is index 4 = target → answer 5
```
(The arrays are what the partition really produces for those pivots.)

---

## 3. Java concepts used in this problem

### 3.1 The 3-way partition (Dutch national flag)

```
invariant while scanning a[lo..hi]:
[ lo .. lt−1 ]  < pivot
[ lt .. i−1  ]  == pivot
[ i  .. gt   ]  not yet examined
[ gt+1 .. hi ]  > pivot
```

- `a[i] < pivot` → swap it into the "less" zone: `swap(lt++, i++)`
- `a[i] > pivot` → swap it to the end: `swap(i, gt--)`, and **don't** advance i (the swapped-in element is unexamined)
- equal → `i++`

At the end, `a[lt..gt]` all equal the pivot, and they're in their final sorted positions. (The same Dutch-flag technique as in [ArrayPatterns](../../01-Arrays/01-Basics/ArrayPatterns.java) in the Arrays topic.)

### 3.2 A `static final Random` with a fixed seed

```java
static final Random RND = new Random(42);
// RND.nextInt(hi - lo + 1)  →  a random offset in [0, hi − lo]
```

A fixed seed makes the runs reproducible (handy for tests). In production you'd use an unseeded Random or `ThreadLocalRandom`.

### 3.3 `nums.clone()`

Quickselect rearranges the array; cloning keeps the caller's input unchanged.

### 3.4 `while (true)` with a `return` inside

The loop always narrows the range around the target, so it eventually lands inside an equal block and returns.

---

## 4. Approach 1: Sort

```java
static int findKthLargestSort(int[] nums, int k) {
    int[] a = nums.clone();
    Arrays.sort(a);
    return a[a.length - k];
}
```

**O(n log n)** time. (`Arrays.sort` on an `int[]` is a dual-pivot quicksort.)

---

## 5. Approach 2: Min-heap of size k

### Code ([M01_KthLargestElementInArray.java:42-49](M01_KthLargestElementInArray.java#L42-L49))

```java
static int findKthLargestHeap(int[] nums, int k) {
    PriorityQueue<Integer> heap = new PriorityQueue<>();
    for (int x : nums) {
        heap.offer(x);
        if (heap.size() > k) heap.poll();
    }
    return heap.peek();
}
```

| Line | What it does |
|---|---|
| the min-heap | holds the k largest values seen so far |
| `if (heap.size() > k) heap.poll();` | evict the smallest of them |
| `heap.peek()` | the k-th largest |

**Dry run** `[3, 2, 1, 5, 6, 4]`, k = 2: {3} → {2, 3} → (1 evicted) → {3, 5} after evicting 2 → {5, 6} after evicting 3 → {5, 6} after evicting 4 → **5**.

**O(n log k)** time, **O(k)** space.

---

## 6. Approach 3: Quickselect with a random pivot and a 3-way partition (the file's main solution)

### Code ([M01_KthLargestElementInArray.java:51-72](M01_KthLargestElementInArray.java#L51-L72))

```java
static final Random RND = new Random(42);

static int findKthLargest(int[] nums, int k) {
    int[] a = nums.clone();
    int target = a.length - k;                       // index in ASCENDING order
    int lo = 0, hi = a.length - 1;
    while (true) {
        int pivot = a[lo + RND.nextInt(hi - lo + 1)];
        // 3-way partition of a[lo..hi]: [ < pivot | == pivot | > pivot ]
        int lt = lo, i = lo, gt = hi;
        while (i <= gt) {
            if (a[i] < pivot) swap(a, lt++, i++);
            else if (a[i] > pivot) swap(a, i, gt--);
            else i++;
        }
        if (target < lt) hi = lt - 1;                // answer among the smaller ones
        else if (target > gt) lo = gt + 1;           // among the larger ones
        else return pivot;                           // inside the "equal" block
    }
}

static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }
```

### Line by line

| Line | What it does |
|---|---|
| `target = a.length − k` | the k-th largest is at this index of the ascending order |
| `pivot = a[lo + random]` | a random element of the current range |
| the partition loop | split a[lo..hi] into < / == / > pivot |
| `target < lt` | the answer is smaller than the pivot → keep only the left part |
| `target > gt` | larger → keep only the right part |
| else | the target index falls in the equal block → the answer is the pivot |

### Dry run: `[3, 2, 3, 1, 2, 4, 5, 5, 6]`, k = 4 → target 5 (suppose the pivots are 3, then 5)

| range | pivot | after the partition | equal block | decision |
|---|---|---|---|---|
| 0..8 | 3 | [2, 1, 2 \| 3, 3 \| 4, 5, 5, 6] (in some order) | indices 3..4 | 5 > 4 → lo = 5 |
| 5..8 | 5 | [4 \| 5, 5 \| 6] | indices 6..7 | 5 < 6 → hi = 5 |
| 5..5 | 4 | [4] | index 5 | **return 4** |

### Complexity
- **Time O(n)** on average; O(n²) in the (vanishingly unlikely) worst case with random pivots.
- **Space O(1)** extra (plus the clone).

The file runs n = 2,000,000 mostly-duplicate values: the 3-way partition handles the duplicates in one pass.

---

## 7. Comparing the approaches

| | Sort | Min-heap of size k | Quickselect |
|---|---|---|---|
| Time | O(n log n) | O(n log k) | **O(n)** average |
| Space | O(1)–O(n) | O(k) | O(1) |
| Streaming? | no | **yes** | no |
| Modifies the input? | (a copy) | no | (a copy) |

---

## 8. Edge cases and common mistakes

**Edge cases:** k = 1 (the max), k = n (the min), all equal values, negative values.

**Mistakes:**
1. Using the k-th **distinct** value.
2. target = k − 1 (that's the k-th **smallest**).
3. A fixed pivot (first or last) → O(n²) on sorted input.
4. A 2-way partition on many duplicates → quadratic behaviour.
5. In the 3-way partition, advancing i after swapping with gt.

---

## 9. Interview questions and answers

**Q1. Why is Quickselect O(n) on average?**
> Each round keeps one side, about half of the range on average: n + n/2 + n/4 + … = 2n.

**Q2. Heap or Quickselect?**
> Quickselect for a one-off query on an in-memory array (O(n)). The heap when the data is a stream, k is small, or the input must not be modified.

**Q3. How do you guarantee O(n) in the worst case?**
> Median-of-medians pivot selection, which is linear in the worst case but slow in practice. Random pivots are the standard answer.

### Follow-up questions

**F1. The k-th largest in a stream.** → [E01](../02-Easy/E01_KthLargestInStream.md)

**F2. The k closest points.** → [M02](M02_KClosestPointsToOrigin.md)
> The same top-k, with a key (the distance).

**F3. The median of an array.**
> Quickselect with k = n/2 (and the neighbouring order statistic for even n).

---

## 10. Running the file

```bash
java M01_KthLargestElementInArray.java      # 6/6 tests passed (including 1,000 random arrays vs sorting, and a 2,000,000-element run)
```

**One-line summary:** *"A size-k min-heap gives O(n log k); Quickselect with a random pivot and a 3-way partition narrows to the index n − k in O(n) on average."*
