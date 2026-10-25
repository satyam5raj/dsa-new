# M02 · K Closest Points to Origin

| | |
|---|---|
| **LeetCode** | [#973 K Closest Points to Origin](https://leetcode.com/problems/k-closest-points-to-origin/) |
| **Difficulty** | Medium |
| **Pattern** | Top-k smallest → a **max**-heap of size k (compare **squared** distances) |
| **Code** | [M02_KClosestPointsToOrigin.java](M02_KClosestPointsToOrigin.java) |
| **Run** | `java M02_KClosestPointsToOrigin.java` |

PriorityQueue basics are explained in [E01](../02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Return the k points closest to (0, 0), in any order.

| points | k | answer | squared distances |
|---|---|---|---|
| `[[1,3],[-2,2]]` | 1 | `[[-2,2]]` | 10 vs 8 |
| `[[3,3],[5,-1],[-2,4]]` | 2 | `[[3,3],[-2,4]]` | 18, 26, 20 |

**Constraints:** `1 ≤ k ≤ n ≤ 10⁴`, coordinates in [−10⁴, 10⁴].

---

## 2. How to think about it

### Don't compute the square root
Distance is √(x² + y²). Since √ is monotonic, comparing **x² + y²** gives the same order, exactly, with no floating point.

### "k smallest" → a max-heap of size k
The mirror of [E01](../02-Easy/E01_KthLargestInStream.md)'s "k largest → min-heap":
- the root is the **farthest** of the k points kept
- a new point closer than the root replaces it

```
for each point: offer; if size > k: poll   (drops the farthest)
```

O(n log k). Quickselect on the distances ([M01](M01_KthLargestElementInArray.md)) gives O(n) on average if needed.

### Dry run: `[[3,3],[5,-1],[-2,4]]`, k = 2 (squared distances 18, 26, 20)
```
offer 18 → {18}
offer 26 → {26, 18}
offer 20 → {26, 20, 18} → size 3 > 2 → poll 26
→ [[3, 3], [−2, 4]]
```

### Overflow note
Coordinates up to 10⁴ → x² + y² up to 2 · 10⁸, which fits in an int. For bigger coordinates, use `long`.

---

## 3. Java concepts used in this problem

### 3.1 A heap of `int[]` with a computed key

```java
PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> Integer.compare(dist(b), dist(a)));
```

The heap stores the points themselves. The comparator computes each point's distance on demand. Writing `dist(b), dist(a)` (b first) makes it a **max**-heap.

(It recomputes the distances on every comparison. Storing `{dist, x, y}` triples avoids that, at the cost of a bit more memory.)

### 3.2 `heap.toArray(new int[0][])`

It converts the heap's contents to an `int[][]` (in the heap's internal order, which is fine since any order is accepted). `new int[0][]` tells Java the element type.

### 3.3 Method references as comparator keys

```java
Arrays.sort(byDist, Comparator.comparingInt(M02_KClosestPointsToOrigin::dist));
```

`Class::method` refers to the static `dist` method; `comparingInt` builds a comparator that sorts by it.

### 3.4 `Arrays.deepToString`

It prints nested arrays (`[[1, 2], [3, 4]]`); plain `Arrays.toString` would print the inner arrays' memory addresses.

---

## 4. Approach 1: Sort by distance

```java
static int[][] kClosestSort(int[][] points, int k) {
    int[][] c = points.clone();
    Arrays.sort(c, Comparator.comparingInt(p -> p[0] * p[0] + p[1] * p[1]));
    return Arrays.copyOf(c, k);
}
```

Sort all n points, then take the first k: **O(n log n)**. (`points.clone()` copies the outer array only; the inner points are shared, which is fine since they aren't modified.)

---

## 5. Approach 2: Max-heap of size k (the file's solution)

### Code ([M02_KClosestPointsToOrigin.java:37-46](M02_KClosestPointsToOrigin.java#L37-L46))

```java
static int dist(int[] p) { return p[0] * p[0] + p[1] * p[1]; }

static int[][] kClosest(int[][] points, int k) {
    PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> Integer.compare(dist(b), dist(a)));   // max-heap by distance
    for (int[] p : points) {
        heap.offer(p);
        if (heap.size() > k) heap.poll();             // drop the farthest
    }
    return heap.toArray(new int[0][]);
}
```

### Line by line

| Line | What it does |
|---|---|
| `dist(p)` | the squared distance (no sqrt) |
| the comparator | the farthest point sits at the root |
| `heap.offer(p)` | every point is a candidate |
| `if (heap.size() > k) heap.poll()` | evict the farthest, keeping the k closest |
| `toArray` | the answer, in any order |

### Dry run: `[[1, 3], [−2, 2]]`, k = 1

| point | dist² | heap after offer | evict? | heap |
|---|---|---|---|---|
| [1, 3] | 10 | {10} | | {[1, 3]} |
| [−2, 2] | 8 | {10, 8} | poll 10 | {[−2, 2]} |

→ `[[-2, 2]]` ✓

### Complexity
- **Time O(n log k).**
- **Space O(k).**

---

## 6. Approach 3: Quickselect on the distances

```java
static int[][] kClosestQuickselect(int[][] points, int k) {
    int[][] a = points.clone();
    Random rnd = new Random();
    int lo = 0, hi = a.length - 1;
    while (lo < hi) {
        int pivot = dist(a[lo + rnd.nextInt(hi - lo + 1)]);
        int lt = lo, i = lo, gt = hi;
        while (i <= gt) {
            int d = dist(a[i]);
            if (d < pivot) { int[] t = a[lt]; a[lt++] = a[i]; a[i++] = t; }
            else if (d > pivot) { int[] t = a[gt]; a[gt--] = a[i]; a[i] = t; }
            else i++;
        }
        if (k - 1 < lt) hi = lt - 1;
        else if (k - 1 > gt) lo = gt + 1;
        else break;                                   // the k-th closest is in the equal block
    }
    return Arrays.copyOf(a, k);
}
```

M01's 3-way partition on distances. Once the k-th position is settled, the first k slots hold the k closest points: **O(n)** on average.

---

## 7. Comparing the approaches

| | Sort | Max-heap of size k | Quickselect |
|---|---|---|---|
| Time | O(n log n) | O(n log k) | O(n) average |
| Space | O(n) | O(k) | O(1) extra |
| Streaming? | no | yes | no |

The file checks the heap version against sorting on 500 random cases (with distinct distances, so the answer is unique).

---

## 8. Edge cases and common mistakes

**Edge cases:** k = n (all the points), ties in distance (any of them may be returned), points at the origin.

**Mistakes:**
1. A min-heap of all points and k polls: it works, but costs O(n + k log n) memory and time.
2. Using `Math.sqrt` (floating point; slower; possible precision issues).
3. Integer overflow for large coordinates.

---

## 9. Interview questions and answers

**Q1. Why a max-heap for the closest points?**
> We keep the k closest and repeatedly discard the farthest of them. The max-heap's root is exactly the one to discard.

**Q2. Which approach for a stream of points?**
> The size-k max-heap: it needs only O(k) memory and processes each point in O(log k).

### Follow-up questions

**F1. The k closest points to an arbitrary point (a, b).**
> Use `(x − a)² + (y − b)²` as the key.

**F2. Find K Closest Elements in a sorted array (LC 658).**
> Binary search for the window start in [0, n − k], or two pointers shrinking from both ends.

**F3. Many queries on a static set.**
> A spatial index such as a k-d tree.

---

## 10. Running the file

```bash
java M02_KClosestPointsToOrigin.java      # 3/3 tests passed (including 500 random cases vs sorting)
```

**One-line summary:** *"Keep a max-heap of size k by squared distance: offer each point and poll whenever the size exceeds k; the heap ends with the k closest."*
