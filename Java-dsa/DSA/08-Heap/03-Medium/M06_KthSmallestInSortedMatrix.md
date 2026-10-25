# M06 · Kth Smallest Element in a Sorted Matrix

| | |
|---|---|
| **LeetCode** | [#378 Kth Smallest Element in a Sorted Matrix](https://leetcode.com/problems/kth-smallest-element-in-a-sorted-matrix/) |
| **Difficulty** | Medium |
| **Pattern** | A k-way merge of the rows, or binary search on the value |
| **Code** | [M06_KthSmallestInSortedMatrix.java](M06_KthSmallestInSortedMatrix.java) |
| **Run** | `java M06_KthSmallestInSortedMatrix.java` |

PriorityQueue basics are explained in [E01](../02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

An n × n matrix has every row **and** every column sorted ascending. Return the k-th smallest element overall (counting duplicates).

```
[[ 1,  5,  9],
 [10, 11, 13],        k = 8 → 13     (1 5 9 10 11 12 13 [13] 15)
 [12, 13, 15]]
```

**Constraints:** `1 ≤ n ≤ 300`, `1 ≤ k ≤ n²`.

---

## 2. How to think about it

### Brute force
Flatten and sort: O(n² log n).

### Approach 1: a k-way merge (the HeapPatterns technique)
Each row is a sorted list. Put the first element of every row into a min-heap as `{value, row, col}`. Poll k times; each poll pushes the **next element of that row**. The k-th poll is the answer.

```
heap {1, 10, 12} → polls: 1, 5, 9, 10, 11, 12, 13, 13 → the 8th is 13
```

O(k log n). Only min(n, k) rows are ever needed.

### Approach 2: binary search on the answer value
For a value x, count how many entries are ≤ x in **O(n)** with a **staircase walk**:
- start at the bottom-left
- if `a[r][c] ≤ x`, the whole column above it is ≤ x → `count += r + 1`, move right
- otherwise, move up

`count(x)` grows with x, and the answer is the **smallest x with count(x) ≥ k**. That x is always a value in the matrix (the smallest one where the count reaches k).

```
lo 1, hi 15:  mid 8 → count 2 < 8 → lo 9
              mid 12 → count 6 < 8 → lo 13
              mid 14 → count 8 ≥ 8 → hi 14
              mid 13 → count 8 ≥ 8 → hi 13
lo = hi = 13 → answer 13
```

(Traced by running the code.)

---

## 3. Java concepts used in this problem

### 3.1 A heap of `{value, row, col}` triples

```java
new PriorityQueue<int[]>(Comparator.comparingInt(e -> e[0]))
```

It orders by value; the row and column say where the next element comes from.

### 3.2 Flattening a 2D array with streams (in the test)

```java
Arrays.stream(a).flatMapToInt(Arrays::stream).sorted().toArray()
```

`Arrays.stream(a)` is a stream of rows; `flatMapToInt(Arrays::stream)` turns each row into its ints and concatenates them.

### 3.3 The "smallest x such that …" binary search

```java
while (lo < hi) {
    int mid = lo + (hi - lo) / 2;
    if (condition(mid)) hi = mid; else lo = mid + 1;
}
```

`hi = mid` (not `mid − 1`) because mid itself might be the answer. The loop ends with lo == hi. (`lo + (hi − lo)/2` also avoids overflow with negative values.)

---

## 4. Approach 1: Flatten and sort

```java
static int kthSmallestSort(int[][] m, int k) {
    int[] flat = Arrays.stream(m).flatMapToInt(Arrays::stream).sorted().toArray();
    return flat[k - 1];
}
```

**O(n² log n)** time, O(n²) space. It ignores the sortedness entirely; it's the file's referee.

---

## 5. Approach 2: K-way merge with a min-heap

### Code ([M06_KthSmallestInSortedMatrix.java:40-50](M06_KthSmallestInSortedMatrix.java#L40-L50))

```java
static int kthSmallestHeap(int[][] m, int k) {
    int n = m.length;
    PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(e -> e[0]));
    for (int r = 0; r < Math.min(n, k); r++) heap.offer(new int[]{m[r][0], r, 0});
    int[] e = null;
    for (int i = 0; i < k; i++) {
        e = heap.poll();
        if (e[2] + 1 < n) heap.offer(new int[]{m[e[1]][e[2] + 1], e[1], e[2] + 1});   // next in that row
    }
    return e[0];
}
```

### Line by line

| Line | What it does |
|---|---|
| `Math.min(n, k)` rows | rows beyond k can't contain the k-th smallest (each row's first element is already ≥ the row above's) |
| `{m[r][0], r, 0}` | the head of each row |
| `e = heap.poll()` | the next smallest overall |
| push the row's next element | keeps exactly one "frontier" element per row in the heap |
| `return e[0]` | the k-th polled value |

### Dry run: k = 8

| poll # | value polled (row, col) | pushed |
|---|---|---|
| 1 | 1 (0, 0) | 5 |
| 2 | 5 (0, 1) | 9 |
| 3 | 9 (0, 2) | — (end of row) |
| 4 | 10 (1, 0) | 11 |
| 5 | 11 (1, 1) | 13 |
| 6 | 12 (2, 0) | 13 |
| 7 | 13 (one of the two 13s: (1, 2) or (2, 1)) | its row's next element, if any |
| 8 | **13** (the other 13) | |

### Complexity
- **Time O(k log n)** (the heap holds at most n entries).
- **Space O(n).**

---

## 6. Approach 3: Binary search on the value (the file's main solution)

### Code ([M06_KthSmallestInSortedMatrix.java:52-70](M06_KthSmallestInSortedMatrix.java#L52-L70))

```java
static int kthSmallest(int[][] m, int k) {
    int lo = m[0][0], hi = m[m.length - 1][m.length - 1];
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;
        if (countAtMost(m, mid) >= k) hi = mid;     // mid could be the answer, look lower
        else lo = mid + 1;
    }
    return lo;
}

// how many entries are <= x, walking a staircase from the bottom-left
static int countAtMost(int[][] m, int x) {
    int n = m.length, r = n - 1, c = 0, count = 0;
    while (r >= 0 && c < n) {
        if (m[r][c] <= x) { count += r + 1; c++; }  // this column's rows 0..r are all <= x
        else r--;
    }
    return count;
}
```

### Line by line

| Line | What it does |
|---|---|
| `lo = m[0][0], hi = m[n−1][n−1]` | the smallest and largest values bound the answer |
| `countAtMost(m, mid) >= k` | at least k entries are ≤ mid → the answer is ≤ mid |
| `else lo = mid + 1` | fewer than k → the answer is larger |
| the staircase: `m[r][c] <= x` | column c, rows 0..r are all ≤ x (the column is sorted) → count them, move right |
| `else r--` | this entry is too big, and so is everything to its right in this row → move up |

### Dry run: `countAtMost(m, 12)`

| r, c | m[r][c] | ≤ 12? | count |
|---|---|---|---|
| 2, 0 | 12 | yes → +3 | 3 |
| 2, 1 | 13 | no → up | 3 |
| 1, 1 | 11 | yes → +2 | 5 |
| 1, 2 | 13 | no → up | 5 |
| 0, 2 | 9 | yes → +1 | 6 |
| c = 3 → stop | | | **6** |

### Why the result is always in the matrix
lo ends as the smallest value with count ≥ k. If it weren't in the matrix, then lo − 1 would have the same count (no entries equal lo), contradicting minimality.

### Complexity
- **Time O(n log(max − min)).**
- **Space O(1).**

---

## 7. Comparing the approaches

| | Flatten + sort | K-way merge | Binary search on value |
|---|---|---|---|
| Time | O(n² log n) | O(k log n) | O(n log(range)) |
| Space | O(n²) | O(n) | **O(1)** |
| Best when | it's simple enough | k is small | k is large / memory matters |

The file checks both against flatten + sort on 300 random matrices (with duplicates).

---

## 8. Edge cases and common mistakes

**Edge cases:** a 1 × 1 matrix, k = 1 or n², duplicate values, negative values.

**Mistakes:**
1. `hi = mid − 1` when the condition holds (it skips the answer).
2. A staircase starting at the top-left (both directions increase there, so it doesn't work).
3. Treating the matrix as one sorted array (the rows don't continue into each other: 9 < 10 here, but not in general).

---

## 9. Interview questions and answers

**Q1. Why does the staircase walk run in O(n)?**
> Each step moves either up or right, and there are at most n of each, so at most 2n steps.

**Q2. Why is the binary search answer guaranteed to be an element?**
> It's the smallest value whose count reaches k. Values not present in the matrix don't change the count, so the minimum must be a present value.

### Follow-up questions

**F1. Find K Pairs with Smallest Sums.** → [M10](M10_FindKPairsWithSmallestSums.md)
> The same k-way merge over an implicit sorted matrix of sums.

**F2. Merge k sorted lists.** → [Linked Lists H02](../../03-Linked-Lists/04-Hard/H02_MergeKSortedLists.md)
> The original k-way merge.

**F3. Search a 2D Matrix II (LC 240).**
> The same staircase walk, looking for a single target in O(n + m).

---

## 10. Running the file

```bash
java M06_KthSmallestInSortedMatrix.java      # 4/4 tests passed (including 300 random matrices vs flatten+sort)
```

**One-line summary:** *"Either merge the rows with a min-heap and poll k times, or binary search the value, counting the entries ≤ mid with a bottom-left staircase walk."*
