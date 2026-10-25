# M16 · Range Sum Query - Mutable ⭐

| | |
|---|---|
| **LeetCode** | [#307 Range Sum Query - Mutable](https://leetcode.com/problems/range-sum-query-mutable/) |
| **Difficulty** | Medium |
| **Pattern** | A segment tree or a Fenwick tree ([01-Basics/SegmentTreeAndFenwick](../01-Basics/SegmentTreeAndFenwick.java)) |
| **Code** | [M16_RangeSumQueryMutable.java](M16_RangeSumQueryMutable.java) |
| **Run** | `java M16_RangeSumQueryMutable.java` |

⭐ = a bonus classic.

---

## 1. The problem

Implement:
- `NumArray(nums)`
- `update(index, val)`: set `nums[index] = val`
- `sumRange(left, right)`: the sum of `nums[left..right]` (inclusive)

with up to 3 · 10⁴ calls of each, interleaved.

```
nums = [1, 3, 5]
sumRange(0, 2) → 9
update(1, 2)          nums = [1, 2, 5]
sumRange(0, 2) → 8
```

---

## 2. How to think about it

### Why the usual tools aren't enough

| | update | sumRange |
|---|---|---|
| loop over the range | O(1) | O(n) |
| prefix sums ([Arrays E21](../../01-Arrays/02-Easy/E21_RangeSumQueryImmutable.md)) | O(n) (rebuild) | O(1) |

With many of **both**, either is O(n · q). We want both in **O(log n)**.

### Fenwick tree (Binary Indexed Tree): about 10 lines
- `update(i, val)`: add the **difference** (`val − nums[i]`) along the low-bit climb, and remember the new `nums[i]`
- `sumRange(l, r) = prefix(r) − prefix(l − 1)`

### Iterative segment tree (bottom-up, size 2n): also short
- the leaves are at `tree[n .. 2n−1]`, the parent of i is i/2, and `tree[i] = tree[2i] + tree[2i+1]`
- **update:** set the leaf, then walk up with `i /= 2`
- **query [l, r]:** set `l += n`, `r += n + 1` (half-open), and while `l < r`:
  - if l is a **right** child (odd), take `tree[l++]` (its parent would cover too much)
  - if r is odd, take `tree[--r]`
  - then `l /= 2`, `r /= 2`

### Which one in an interview?
Fenwick for sums (the shortest code). A segment tree when the operation isn't invertible (min/max) or when you need range updates (lazy propagation).

---

## 3. Java concepts used in this problem

### 3.1 The lowest set bit: `x & -x`

In two's complement, `-x` = `~x + 1`, which flips every bit above the lowest 1 and keeps that 1. So `x & -x` isolates the **lowest set bit**:

```
x  = 6  = 0110
-x      = 1010
x & -x  = 0010 = 2
```

Fenwick node x covers the `x & -x` elements ending at position x (1-indexed):
- **prefix sum:** `x -= x & -x` jumps to the previous disjoint block
- **update:** `x += x & -x` jumps to the next block that also covers position x

Both loops take O(log n) steps.

### 3.2 1-indexing inside, 0-indexing outside

The Fenwick array has length n + 1 and index 0 is unused (`x & -x` is 0 for x = 0, which would loop forever). The public methods take 0-indexed positions and add 1 internally.

### 3.3 Bit tests: `(l & 1) == 1`

`l & 1` keeps only the last bit: 1 for odd (a right child in the segment-tree layout), 0 for even (a left child).

### 3.4 `nums.clone()` in the constructor

The Fenwick version keeps its own copy of the values (to compute the update differences), so outside changes to the caller's array can't corrupt it.

---

## 4. Approach 1: Plain array, loop on query

```java
static class NumArrayNaive {
    private final int[] a;
    NumArrayNaive(int[] nums) { a = nums.clone(); }
    void update(int i, int val) { a[i] = val; }
    int sumRange(int l, int r) {
        int s = 0;
        for (int i = l; i <= r; i++) s += a[i];
        return s;
    }
}
```

Update O(1), query **O(n)**. That's 3 · 10⁴ queries × 3 · 10⁴ elements ≈ 10⁹ steps at worst. Too slow.

---

## 5. Approach 2: Fenwick tree (the file's main solution)

### Code ([M16_RangeSumQueryMutable.java:43-71](M16_RangeSumQueryMutable.java#L43-L71))

```java
static class NumArray {                    // Fenwick tree version
    private final int[] nums;
    private final int[] tree;              // 1-indexed

    NumArray(int[] nums) {
        this.nums = nums.clone();
        tree = new int[nums.length + 1];
        for (int i = 0; i < nums.length; i++) add(i, nums[i]);
    }

    private void add(int i, int delta) {
        for (int x = i + 1; x < tree.length; x += x & -x) tree[x] += delta;
    }

    private int prefix(int i) {            // sum of nums[0..i]
        int s = 0;
        for (int x = i + 1; x > 0; x -= x & -x) s += tree[x];
        return s;
    }

    void update(int index, int val) {
        add(index, val - nums[index]);     // Fenwick stores sums: push the DIFFERENCE
        nums[index] = val;
    }

    int sumRange(int left, int right) {
        return prefix(right) - (left > 0 ? prefix(left - 1) : 0);
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| the constructor | n `add`s: O(n log n) (an O(n) build exists too) |
| `add(i, delta)` | add delta to every block that covers position i + 1 |
| `prefix(i)` | sum the disjoint blocks that make up positions 1..i+1 |
| `update` | the tree stores sums, so push the **change**, not the new value |
| `sumRange` | the difference of two prefixes (left = 0 has no prefix to subtract) |

### Dry run: build `[1, 3, 5]` (tree indices 1..3)

| add | x path | tree after |
|---|---|---|
| add(0, 1) | x = 1 → 2 → 4 (stop) | [_, 1, 1, 0] |
| add(1, 3) | x = 2 → 4 (stop) | [_, 1, 4, 0] |
| add(2, 5) | x = 3 → 4 (stop) | [_, 1, 4, 5] |

tree[1] = nums[0], tree[2] = nums[0..1], tree[3] = nums[2]. (Checked by running the code.)

**sumRange(0, 2) = prefix(2):** x = 3 → s = 5; x = 3 − 1 = 2 → s = 9; x = 0 → stop → **9** ✓

**update(1, 2):** delta = 2 − 3 = −1; add at x = 2 → tree[2] = 3; x = 4 → stop. tree = [_, 1, 3, 5].

**sumRange(0, 2):** tree[3] + tree[2] = 5 + 3 = **8** ✓

### Complexity
- **update, sumRange: O(log n).**
- Build: O(n log n) here. **Space O(n).**

---

## 6. Approach 3: Iterative segment tree

### Code ([M16_RangeSumQueryMutable.java:73-98](M16_RangeSumQueryMutable.java#L73-L98))

```java
static class NumArraySegment {             // iterative bottom-up segment tree
    private final int n;
    private final int[] tree;

    NumArraySegment(int[] nums) {
        n = nums.length;
        tree = new int[2 * n];
        for (int i = 0; i < n; i++) tree[n + i] = nums[i];           // leaves
        for (int i = n - 1; i > 0; i--) tree[i] = tree[2 * i] + tree[2 * i + 1];
    }

    void update(int index, int val) {
        int i = index + n;
        tree[i] = val;
        for (i /= 2; i > 0; i /= 2) tree[i] = tree[2 * i] + tree[2 * i + 1];
    }

    int sumRange(int left, int right) {
        int s = 0;
        for (int l = left + n, r = right + n + 1; l < r; l /= 2, r /= 2) {
            if ((l & 1) == 1) s += tree[l++];
            if ((r & 1) == 1) s += tree[--r];
        }
        return s;
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `tree[n + i] = nums[i]` | the leaves occupy the second half |
| the building loop | every internal node = the sum of its two children (built bottom-up) |
| `update` | set the leaf, then recompute each ancestor |
| `l = left + n, r = right + n + 1` | the half-open range [l, r) at the leaf level |
| `(l & 1) == 1` | l is a right child: its parent also covers l − 1 (outside the range), so take l alone and step right |
| `(r & 1) == 1` | r − 1 is a left child sticking out: take it alone |
| `l /= 2, r /= 2` | move up a level |

### Dry run: `[1, 3, 5]`, n = 3

Build: leaves tree[3] = 1, tree[4] = 3, tree[5] = 5; tree[2] = tree[4] + tree[5] = 8; tree[1] = tree[2] + tree[3] = 9.

(With n not a power of two, the internal nodes don't correspond to neat ranges, e.g. tree[2] = nums[1] + nums[2], but the bottom-up query still works.)

update(1, 2): tree[4] = 2 → tree[2] = 2 + 5 = 7 → tree[1] = 7 + 1 = 8.

sumRange(0, 2): l = 3, r = 6

| l | r | l odd? | r odd? | s |
|---|---|---|---|---|
| 3 | 6 | yes → s += tree[3] = 1, l = 4 | no | 1 |
| 2 | 3 | no | yes → s += tree[2] = 7, r = 2 | 8 |
| 1 | 1 | stop | | **8** ✓ |

### Complexity
- **update, sumRange: O(log n);** build O(n).
- **Space O(n)** (2n ints).

---

## 7. Comparing the approaches

| | Naive | Fenwick | Iterative segment tree |
|---|---|---|---|
| update | O(1) | O(log n) | O(log n) |
| sumRange | O(n) | O(log n) | O(log n) |
| Build | O(n) | O(n log n) (or O(n)) | O(n) |
| Supports min/max | | no (needs an inverse) | yes |

The file checks both trees against a plain array on 50 random arrays with 400 mixed operations each.

---

## 8. Edge cases and common mistakes

**Edge cases:** n = 1, updating to the same value (delta 0), a range of one element, left = 0.

**Mistakes:**
1. In Fenwick's `update`, adding the new value instead of the **difference**.
2. Off-by-one between the 0-indexed API and the 1-indexed Fenwick array.
3. In the segment tree, using the closed range `r = right + n` with the half-open loop.
4. `prefix(left − 1)` with left = 0 (index −1).

---

## 9. Interview questions and answers

**Q1. Why can't prefix sums handle updates efficiently?**
> Changing nums[i] changes every prefix from i to n − 1, so an update costs O(n).

**Q2. Fenwick or segment tree?**
> Fenwick is shorter and great for invertible operations like sums (a range = the difference of two prefixes). A segment tree handles any associative operation (min, max, gcd) and extends to lazy range updates.

**Q3. What does `x & -x` do?**
> It isolates the lowest set bit of x, which is the size of the block that Fenwick node x is responsible for.

### Follow-up questions

**F1. Range minimum query with updates.**
> A segment tree with `min` instead of `+` (Fenwick can't subtract mins).

**F2. Add v to every element in a range, and query single points.**
> A Fenwick tree over the **difference array**: add v at l and −v at r + 1; a point value is a prefix sum.

**F3. Count of Smaller Numbers After Self.** → [H05](../04-Hard/H05_CountOfSmallerNumbersAfterSelf.md)
> A Fenwick tree over the value ranks, scanning from right to left.

---

## 10. Running the file

```bash
java M16_RangeSumQueryMutable.java      # 4/4 tests passed (including 20,000 random ops on both trees)
```

**One-line summary:** *"Use a Fenwick tree (update with the difference via x += x & −x, prefix via x −= x & −x) or a bottom-up segment tree of size 2n: both operations in O(log n)."*
