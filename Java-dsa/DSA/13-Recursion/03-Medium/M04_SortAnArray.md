# M04 · Sort an Array ⭐

| | |
|---|---|
| **LeetCode** | [#912 Sort an Array](https://leetcode.com/problems/sort-an-array/) |
| **Difficulty** | Medium |
| **Pattern** | Divide and conquer: **merge sort** and **quick sort** |
| **Code** | [M04_SortAnArray.java](M04_SortAnArray.java) |
| **Run** | `java M04_SortAnArray.java` |

⭐ = not in `leetcode.md`, added for a key technique. Recursion basics are explained in [E01](../02-Easy/E01_FibonacciNumber.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Sort an integer array in O(n log n) **without** using a built-in sort.

| input | output |
|---|---|
| [5, 2, 3, 1] | [1, 2, 3, 5] |
| [5, 1, 1, 2, 0, 0] | [0, 0, 1, 1, 2, 5] |

---

## 2. How to think about it

Both algorithms are divide and conquer, but they put the hard work in different places.

### Merge sort: easy split, hard combine
```
sort(lo, hi):  split at mid, sort both halves recursively, then MERGE them
```

Merging two sorted halves uses two pointers, as in [Merge Sorted Array](../../01-Arrays/02-Easy/E08_MergeSortedArray.md).

```
         [5 2 3 1]
        /         \
     [5 2]       [3 1]        split down to single elements
     /   \       /   \
   [5]  [2]    [3]  [1]
     \   /       \   /
     [2 5]       [1 3]        merge on the way back up
        \         /
        [1 2 3 5]
```

- T(n) = 2T(n/2) + O(n) = **O(n log n), always**: there are log n levels, and each level merges n elements in total.
- **Stable**: equal keys keep their original order.
- Needs an O(n) buffer. Java's `Arrays.sort` for **objects** uses TimSort, a merge-sort variant.

### Quick sort: hard split, easy combine
```
pick a PIVOT; PARTITION into  < pivot | == pivot | > pivot; recurse on the two outer parts
```

There's nothing to combine: after partitioning, the pivot block is already in its final place.

- **Average O(n log n)**, but the **worst case is O(n²)** when the pivot is always the smallest or largest element (for example, sorted input with the first element as the pivot). A **random pivot** makes that practically impossible.
- Many duplicates also hurt a two-way partition, so the file uses a **three-way** partition (<, =, >).
- In place, O(log n) stack on average, **not** stable. Java's `Arrays.sort` for **primitives** uses dual-pivot quicksort.

### Dry run: merge sort on [5, 2, 3, 1]

| call | after the two recursive sorts | `a[mid] <= a[mid+1]`? | after merging |
|---|---|---|---|
| sort(0, 1) | [5] [2] | 5 ≤ 2? no | [2, 5] |
| sort(2, 3) | [3] [1] | 3 ≤ 1? no | [1, 3] |
| sort(0, 3) | [2, 5] [1, 3] | 5 ≤ 1? no | merge: 1 (right), 2 (left), 3 (right), 5 (left) → [1, 2, 3, 5] |

### Dry run: one three-way partition of [5, 1, 1, 2, 0, 0] with pivot 1
(The file picks the pivot at random; pivot 1 is shown here because it exercises all three cases.) The invariant: `[lo..lt−1] < p`, `[lt..i−1] == p`, `[gt+1..hi] > p`, and `[i..gt]` is still unseen.

| a[i] | action | array | lt | i | gt |
|---|---|---|---|---|---|
| | start | 5 1 1 2 0 0 | 0 | 0 | 5 |
| 5 > 1 | swap(i, gt), gt−− | 0 1 1 2 0 5 | 0 | 0 | 4 |
| 0 < 1 | swap(lt, i), lt++, i++ | 0 1 1 2 0 5 | 1 | 1 | 4 |
| 1 == 1 | i++ | | 1 | 2 | 4 |
| 1 == 1 | i++ | | 1 | 3 | 4 |
| 2 > 1 | swap(i, gt), gt−− | 0 1 1 0 2 5 | 1 | 3 | 3 |
| 0 < 1 | swap(lt, i), lt++, i++ | 0 0 1 1 2 5 | 2 | 4 | 3 |

i > gt, so stop. The `<` block is [0, 0] (indices 0–1), the `=` block is [1, 1] (indices 2–3, final), and the `>` block is [2, 5] (indices 4–5). Recurse on 0–1 and 4–5 only.

Note that after `swap(i, gt)`, `i` does **not** advance: the element that came from `gt` hasn't been examined yet.

---

## 3. Java concepts used in this problem

### 3.1 Method overloading

```java
static int[] mergeSort(int[] nums) { ... }                               // the public entry point
static void mergeSort(int[] a, int[] buf, int lo, int hi) { ... }       // the recursive worker
```

Two methods can share a name if their parameter lists differ. Java picks the right one from the arguments.

### 3.2 `clone()` to avoid changing the input

`int[] a = nums.clone()` copies the array, so the caller's array is untouched. (For an array of primitives, `clone()` copies the values themselves.)

### 3.3 One shared buffer

`buf` is allocated **once** and passed down. Allocating a new temporary array inside every merge would work but create O(n log n) garbage.

### 3.4 `System.arraycopy`

```java
System.arraycopy(buf, lo, a, lo, hi - lo + 1);    // (source, srcStart, dest, destStart, length)
```

A fast native copy of a range from one array to another.

### 3.5 Post-increment inside an expression

```java
buf[k++] = a[i] <= a[j] ? a[i++] : a[j++];
```

`k++` uses the current `k` and then increments it. The ternary picks the smaller front element and advances that pointer. Using `<=` (not `<`) takes from the left half on ties, which is what makes merge sort **stable**.

### 3.6 A `static final Random` with a seed

`static final Random RND = new Random(42)` gives one random source for all calls, with reproducible results. `RND.nextInt(hi − lo + 1)` picks a random offset in the range.

### 3.7 `printf` with `%,d`

`%,d` prints a number with thousands separators: `2,999`.

---

## 4. Approach 1: Merge sort

### Code ([M04_SortAnArray.java:46-63](M04_SortAnArray.java#L46-L63))

```java
static int[] mergeSort(int[] nums) {
    int[] a = nums.clone(), buf = new int[a.length];
    mergeSort(a, buf, 0, a.length - 1);
    return a;
}

static void mergeSort(int[] a, int[] buf, int lo, int hi) {
    if (lo >= hi) return;                                   // 0 or 1 element: sorted
    int mid = (lo + hi) >>> 1;
    mergeSort(a, buf, lo, mid);                             // sort the left half
    mergeSort(a, buf, mid + 1, hi);                         // sort the right half
    if (a[mid] <= a[mid + 1]) return;                       // already in order: skip the merge
    int i = lo, j = mid + 1, k = lo;
    while (i <= mid && j <= hi) buf[k++] = a[i] <= a[j] ? a[i++] : a[j++];   // <= keeps it stable
    while (i <= mid) buf[k++] = a[i++];
    while (j <= hi) buf[k++] = a[j++];
    System.arraycopy(buf, lo, a, lo, hi - lo + 1);
}
```

### Line by line

| Line | What it does |
|---|---|
| `lo >= hi` | a range of 0 or 1 elements is sorted |
| `mid` | split point; `>>> 1` is a safe halving |
| the two recursive calls | sort each half (trust them) |
| the skip check | if the left half's max ≤ the right half's min, they're already in order (this makes sorted input O(n)) |
| the main `while` | take the smaller front element into `buf` |
| the two leftover loops | copy whatever remains of either half |
| `arraycopy` | put the merged range back into `a` |

### Complexity
- **Time O(n log n)** in every case (O(n) for already-sorted input, thanks to the skip check).
- **Space O(n)** for the buffer, plus O(log n) stack.

---

## 5. Approach 2: Quick sort with a random pivot and a three-way partition

### Code ([M04_SortAnArray.java:66-87](M04_SortAnArray.java#L66-L87))

```java
static final Random RND = new Random(42);

static int[] quickSort(int[] nums) {
    int[] a = nums.clone();
    quickSort(a, 0, a.length - 1);
    return a;
}

static void quickSort(int[] a, int lo, int hi) {
    if (lo >= hi) return;
    int pivot = a[lo + RND.nextInt(hi - lo + 1)];
    int lt = lo, i = lo, gt = hi;                            // [lo..lt-1] < p, [lt..i-1] == p, [gt+1..hi] > p
    while (i <= gt) {
        if (a[i] < pivot) swap(a, lt++, i++);
        else if (a[i] > pivot) swap(a, i, gt--);
        else i++;
    }
    quickSort(a, lo, lt - 1);                                // the "equal" block is final
    quickSort(a, gt + 1, hi);
}

static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }
```

### Line by line

| Line | What it does |
|---|---|
| `pivot` | a random element's **value** from the range |
| `lt, i, gt` | the boundaries of the three regions (see the invariant) |
| `< pivot` | grow the left region; both `lt` and `i` advance |
| `> pivot` | send it to the right region; `i` stays, because the swapped-in element is unseen |
| `== pivot` | it's already in the middle region |
| the two calls | recurse on the `<` and `>` regions only |

This is the Dutch National Flag algorithm (also used for Sort Colors).

### Complexity
- **Time O(n log n)** expected; O(n²) only with astronomically bad luck.
- **Space O(log n)** expected stack.

---

## 6. Approach 3: Naive quick sort (to show the worst case)

### Code ([M04_SortAnArray.java:90-100](M04_SortAnArray.java#L90-L100))

```java
static int naiveDepth;

static void naiveQuick(int[] a, int lo, int hi, int depth) {
    if (lo >= hi) return;
    naiveDepth = Math.max(naiveDepth, depth);
    int p = a[lo], i = lo;
    for (int j = lo + 1; j <= hi; j++) if (a[j] < p) swap(a, ++i, j);
    swap(a, lo, i);
    naiveQuick(a, lo, i - 1, depth + 1);
    naiveQuick(a, i + 1, hi, depth + 1);
}
```

The pivot is always the first element (Lomuto-style partition). On **already-sorted** input, the pivot is the minimum every time, so one side is empty and the other has n − 1 elements. The file measures a recursion depth of **2,999** for 3,000 elements: depth n, so O(n²) work. With n = 10⁵ this would also overflow the stack.

---

## 7. Comparing the approaches

| | Merge sort | Quick sort (random, 3-way) | Naive quick sort |
|---|---|---|---|
| Time | O(n log n) always | O(n log n) expected | O(n²) on sorted input |
| Extra space | O(n) | O(log n) | O(n) stack worst case |
| Stable | **yes** | no | no |
| Used by Java for | objects (TimSort) | primitives (dual-pivot) | |

The file checks both real sorts against `Arrays.sort` on 2,000 random arrays (with many duplicates), and times them on 2,000,000 random ints: in one run, about 137 ms for merge sort and 133 ms for quick sort.

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty array, one element, all elements equal (the three-way partition handles it in one pass), already sorted, reverse sorted, negative numbers.

**Mistakes:**
1. Merge sort: `<` instead of `<=` when merging (it loses stability).
2. Merge sort: allocating a new buffer in every call.
3. Quick sort: a fixed first or last pivot (O(n²) on sorted input).
4. Quick sort: advancing `i` after swapping with `gt`.
5. Quick sort: recursing on the pivot block (with many duplicates this can loop forever in two-way variants).

---

## 9. Interview questions and answers

**Q1. Merge sort or quick sort?**
> Merge sort: guaranteed O(n log n) and stable, but O(n) extra space. Quick sort: in place and usually faster in practice (cache-friendly), but O(n²) worst case without a random pivot, and not stable.

**Q2. Why does Java use different algorithms for objects and primitives?**
> For objects, stability matters (sorting by one field shouldn't scramble another), so it uses TimSort. For primitives, equal values are indistinguishable, so stability doesn't matter and the faster dual-pivot quicksort is used.

**Q3. What's the recurrence for merge sort?**
> T(n) = 2T(n/2) + O(n): two halves, plus a linear merge. The recursion tree has log n levels of O(n) work each.

### Follow-up questions

**F1. Count inversions.**
> During the merge, whenever an element is taken from the right half, all remaining left-half elements form inversions with it: add `mid − i + 1`.

**F2. Kth Largest Element (LC 215).**
> Quickselect: partition, then recurse into only the side that contains index k. Expected O(n).

**F3. Sort Colors (LC 75).**
> One three-way partition with pivot 1: exactly the inner loop above.

---

## 10. Running the file

```bash
java M04_SortAnArray.java      # 5/5 tests passed; prints the naive depth (2,999) and the timings
```

**One-line summary:** *"Merge sort splits in half, sorts each half and merges with two pointers (always O(n log n), stable); quick sort partitions around a random pivot into <, = and > and recurses on the outer parts (expected O(n log n), in place)."*
