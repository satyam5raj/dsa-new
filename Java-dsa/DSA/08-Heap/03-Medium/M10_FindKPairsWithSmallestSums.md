# M10 · Find K Pairs with Smallest Sums

| | |
|---|---|
| **LeetCode** | [#373 Find K Pairs with Smallest Sums](https://leetcode.com/problems/find-k-pairs-with-smallest-sums/) |
| **Difficulty** | Medium |
| **Pattern** | A k-way merge over an implicit sorted matrix (only expand what you need) |
| **Code** | [M10_FindKPairsWithSmallestSums.java](M10_FindKPairsWithSmallestSums.java) |
| **Run** | `java M10_FindKPairsWithSmallestSums.java` |

PriorityQueue basics are explained in [E01](../02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem). The k-way merge was introduced in [M06](M06_KthSmallestInSortedMatrix.md).

---

## 1. The problem

Two **sorted** arrays. Return the k pairs (u from nums1, v from nums2) with the smallest sums u + v.

| nums1 | nums2 | k | answer |
|---|---|---|---|
| `[1,7,11]` | `[2,4,6]` | 3 | `[[1,2],[1,4],[1,6]]` |
| `[1,1,2]` | `[1,2,3]` | 2 | `[[1,1],[1,1]]` |
| `[1,2]` | `[3,4]` | 10 | all 4 pairs |

**Constraints:** lengths up to 10⁵, k up to 10⁴.

---

## 2. How to think about it

### Brute force
All n · m pairs, sorted by sum: O(nm log(nm)). With n = m = 10⁵, that's 10¹⁰ pairs!

### An implicit sorted matrix
Picture the pair sums as a matrix `S[i][j] = nums1[i] + nums2[j]`:

```
           nums2:   2    4    6
nums1  1           3    5    7
       7           9   11   13
      11          13   15   17
```

Every **row** i is sorted (nums2 is sorted). That's "k sorted lists" → a **k-way merge**:
- start with `S[i][0]` for each row i, but only the first **min(n, k)** rows: rows k and beyond can never be among the k smallest, because `S[0..k−1][0]` are all smaller or equal
- poll the smallest `{i, j}`; push its right neighbour `{i, j + 1}`
- stop after k polls

We never touch more than about 2k cells of the matrix.

```
heap {(0,0)=3, (1,0)=9, (2,0)=13}
poll (0,0)=3 → push (0,1)=5
poll (0,1)=5 → push (0,2)=7
poll (0,2)=7 → done: [1,2], [1,4], [1,6]
```

---

## 3. Java concepts used in this problem

### 3.1 A heap of index pairs with a computed key

```java
new PriorityQueue<int[]>(Comparator.comparingInt(e -> nums1[e[0]] + nums2[e[1]]))
```

The heap stores `{i, j}` positions; the comparator computes the sum on demand from the two arrays (captured by the lambda).

### 3.2 `List.of(a, b)`

It creates an **immutable** two-element list, which is a compact way to build each output pair. (Calling `add` on it would throw.)

### 3.3 Comparing results when ties make the pairs ambiguous

With equal sums, several sets of pairs are valid. The test therefore compares the **sums** of the returned pairs with the k smallest sums of all pairs.

---

## 4. Approach 1: All pairs, sorted

```java
static List<List<Integer>> kSmallestPairsBrute(int[] nums1, int[] nums2, int k) {
    List<int[]> all = new ArrayList<>();
    for (int a : nums1) for (int b : nums2) all.add(new int[]{a, b});
    all.sort(Comparator.comparingInt(p -> p[0] + p[1]));
    List<List<Integer>> out = new ArrayList<>();
    for (int i = 0; i < Math.min(k, all.size()); i++) out.add(List.of(all.get(i)[0], all.get(i)[1]));
    return out;
}
```

**O(nm log(nm))** time and O(nm) memory. Only viable for tiny inputs.

---

## 5. Approach 2: K-way merge over the rows (the file's solution)

### Code ([M10_FindKPairsWithSmallestSums.java:38-48](M10_FindKPairsWithSmallestSums.java#L38-L48))

```java
static List<List<Integer>> kSmallestPairs(int[] nums1, int[] nums2, int k) {
    PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(e -> nums1[e[0]] + nums2[e[1]]));
    for (int i = 0; i < Math.min(nums1.length, k); i++) heap.offer(new int[]{i, 0});   // column 0 of each row
    List<List<Integer>> result = new ArrayList<>();
    while (result.size() < k && !heap.isEmpty()) {
        int[] e = heap.poll();
        result.add(List.of(nums1[e[0]], nums2[e[1]]));
        if (e[1] + 1 < nums2.length) heap.offer(new int[]{e[0], e[1] + 1});           // next in the row
    }
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| the comparator | the pair with the smallest sum comes out first |
| `Math.min(nums1.length, k)` rows | the head of each row that could matter |
| `{i, 0}` | row i, first column |
| `while (result.size() < k && !heap.isEmpty())` | stop at k pairs, or when every pair is used up |
| `result.add(List.of(...))` | the next smallest pair |
| push `{i, j + 1}` | that row's next candidate |

### Dry run: `[1, 1, 2]` × `[1, 2, 3]`, k = 2 (from the real run)

| step | heap (cells = sum) | polled | pair | pushed |
|---|---|---|---|---|
| start | (0,0)=2, (1,0)=2 | | | |
| 1 | | (0, 0) | [1, 1] | (0, 1)=3 |
| 2 | | (1, 0) | [1, 1] | (1, 1)=3 |

→ `[[1, 1], [1, 1]]` ✓ (only 2 rows were seeded, since k = 2).

### Complexity
- **Time O(k log min(n, k)).**
- **Space O(min(n, k))** for the heap (plus the output).

---

## 6. Comparing the approaches

| | All pairs + sort | K-way merge |
|---|---|---|
| Time | O(nm log(nm)) | **O(k log min(n, k))** |
| Space | O(nm) | O(min(n, k)) |

The file checks the merge on 500 random cases (with negatives and ties) by comparing the sums against the k smallest sums of all pairs.

---

## 7. Edge cases and common mistakes

**Edge cases:** k larger than n · m (return all pairs), duplicate values, negative values, one array of length 1.

**Mistakes:**
1. Seeding every row (n can be 10⁵ even when k is small).
2. Pushing both the right **and** the down neighbour without a visited set (duplicates).
3. Comparator overflow: sums up to 2 · 10⁹ could overflow `int` for extreme values; the constraints here keep them in range (use `long` otherwise).

---

## 8. Interview questions and answers

**Q1. Why only seed min(n, k) rows?**
> Row r's smallest sum is S[r][0], and S[0][0] ≤ S[1][0] ≤ … (nums1 is sorted). The k cells S[0..k−1][0] are all ≤ S[k][0], so row k can never contribute to the k smallest.

**Q2. An alternative that seeds only (0, 0)?**
> Push (0, 0); on each poll, push (i + 1, j) and (i, j + 1), with a visited set to avoid duplicates. It's the same complexity, just a bit more bookkeeping.

### Follow-up questions

**F1. The k-th smallest sum only.**
> The same merge, polling k times; or binary search the sum with a two-pointer count, like [M06](M06_KthSmallestInSortedMatrix.md).

**F2. Three arrays.**
> Merge two of them into their k smallest sums first, then pair that list with the third array.

**F3. Kth Smallest Prime Fraction (LC 786).**
> The same implicit sorted matrix idea with the fractions arr[i] / arr[j].

---

## 9. Running the file

```bash
java M10_FindKPairsWithSmallestSums.java      # 4/4 tests passed (including 500 random cases: sums == k smallest)
```

**One-line summary:** *"Treat the sums as a matrix with sorted rows: seed the first column of the first min(n, k) rows, then poll the smallest pair and push its right neighbour, k times."*
