# E08 · Merge Sorted Array

| | |
|---|---|
| **LeetCode** | [#88 Merge Sorted Array](https://leetcode.com/problems/merge-sorted-array/) |
| **Difficulty** | Easy |
| **Pattern** | Two pointers from the back |
| **Code** | [E08_MergeSortedArray.java](E08_MergeSortedArray.java) |
| **Run** | `java E08_MergeSortedArray.java` |

---

## 1. The problem

- `nums1` has length `m + n`. Its first `m` elements are real; the last `n` are `0` **placeholders** (empty space).
- `nums2` has `n` elements.
- Both are sorted in ascending order.
- Merge `nums2` **into** `nums1` so that `nums1` becomes one sorted array. Return nothing.

| nums1 | m | nums2 | n | nums1 after |
|---|---|---|---|---|
| `[1,2,3,0,0,0]` | 3 | `[2,5,6]` | 3 | `[1,2,2,3,5,6]` |
| `[1]` | 1 | `[]` | 0 | `[1]` |
| `[0]` | 0 | `[1]` | 1 | `[1]` |
| `[4,5,6,0,0,0]` | 3 | `[1,2,3]` | 3 | `[1,2,3,4,5,6]` |
| `[-1,0,0]` | 2 | `[-3]` | 1 | `[-3,-1,0]` |

**Constraints:** `0 ≤ m, n ≤ 200`, `1 ≤ m + n`.

---

## 2. How to think about it

1. **Simple:** copy `nums2` into the empty tail, then sort. O((m+n) log(m+n)). It works, but ignores that both inputs are already sorted.
2. **Classic merge (from merge sort), front to back:** compare the smallest remaining elements and write the smaller one at the front. Problem: writing at `nums1[0]` **overwrites** a value of `nums1` that we haven't processed yet. You'd need a copy of `nums1`, which is O(m) extra space.
3. **Insight: the free space is at the END.** So merge **from the back**: compare the **largest** remaining elements and write the bigger one into the last empty slot. The write position `w` is always ≥ `i` (the read position in nums1), so we never overwrite anything we still need.

Three pointers:
- `i = m − 1`: the last real element of nums1
- `j = n − 1`: the last element of nums2
- `w = m + n − 1`: where to write next

**When do we stop?** When `nums2` is used up (`j < 0`). Any remaining nums1 elements are **already** in their correct place at the front.

---

## 3. Java concepts used in this problem

### 3.1 Several declarations in one line

```java
int i = m - 1, j = n - 1, w = m + n - 1;
```

Three `int` variables in one statement. It's fine for closely related variables like these pointers.

### 3.2 Post-decrement on both sides: `nums1[w--] = nums1[i--];`

```java
nums1[w--] = nums1[i--];
// is exactly:
nums1[w] = nums1[i];
w--;
i--;
```

Both indices use their **current** value, then both shrink by one. See E07 for `k++` versus `++k`.

### 3.3 `while` loop

```java
while (j >= 0) { ... }   // keep going as long as the condition is true
```

Use `while` when you don't know in advance how many iterations you need. Here each iteration moves **either** `i` **or** `j`, so we can't use a simple counting `for`.

### 3.4 The `&&` guard: `i >= 0 && nums1[i] > nums2[j]`

If `i < 0` (nums1's real elements are used up), `&&` short-circuits and `nums1[-1]` is **never read**. We then fall to the `else` branch and take from nums2.

---

## 4. Approach 1: Copy, then sort

```java
static void mergeSort(int[] nums1, int m, int[] nums2, int n) {
    for (int k = 0; k < n; k++) nums1[m + k] = nums2[k];   // fill the empty tail
    Arrays.sort(nums1);
}
```

| Line | What it does |
|---|---|
| `nums1[m + k] = nums2[k]` | nums2's element k goes to the k-th empty slot (slots start at index m). `System.arraycopy(nums2, 0, nums1, m, n)` does the same |
| `Arrays.sort(nums1)` | sort the whole thing |

**Time O((m+n) log(m+n)), space O(log(m+n))** for the sort. Mention it, then improve it.

---

## 5. Approach 2: Front-to-back merge with a copy

```java
static void mergeFront(int[] nums1, int m, int[] nums2, int n) {
    int[] a = Arrays.copyOf(nums1, m);       // save nums1's real values
    int i = 0, j = 0, w = 0;
    while (i < m && j < n) {
        nums1[w++] = (a[i] <= nums2[j]) ? a[i++] : nums2[j++];
    }
    while (i < m) nums1[w++] = a[i++];       // leftovers from a
    while (j < n) nums1[w++] = nums2[j++];   // leftovers from nums2
}
```

- `cond ? x : y` is the **ternary operator**: "if cond then x else y", in a single expression.
- Using `<=` takes from `a` on ties. That keeps the merge **stable** (equal elements keep their original order), which matters when sorting objects.
- **O(m+n) time, O(m) extra space** because of the copy. This is the standard merge-sort merge.

---

## 6. Approach 3: Merge from the back (optimal, no extra space)

### Code ([E08_MergeSortedArray.java:43-52](E08_MergeSortedArray.java#L43-L52))

```java
static void merge(int[] nums1, int m, int[] nums2, int n) {
    int i = m - 1, j = n - 1, w = m + n - 1;
    while (j >= 0) {                                   // only nums2 needs to be fully placed
        if (i >= 0 && nums1[i] > nums2[j]) {
            nums1[w--] = nums1[i--];
        } else {
            nums1[w--] = nums2[j--];
        }
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `int i = m - 1, j = n - 1, w = m + n - 1;` | last real element of nums1, last of nums2, last slot overall |
| `while (j >= 0)` | keep going until **every nums2 element** is placed. We don't loop on `i`, because nums1's leftovers are already in the right place |
| `if (i >= 0 && nums1[i] > nums2[j])` | nums1 still has elements **and** its largest one is bigger → it goes last |
| `nums1[w--] = nums1[i--];` | move nums1's element to slot w, then move both pointers back |
| `else nums1[w--] = nums2[j--];` | nums2's element is bigger or equal (or nums1 is used up) → place it |

**Why can't we overwrite unread data?** At any moment, the number of free slots at the back is `w − i = j + 1`, which is the count of nums2 elements still to place. That's ≥ 1 while the loop runs, so `w > i`, and we only ever write to slots that are empty or already processed.

### Dry run 1: `nums1 = [1,2,3,_,_,_]`, `m = 3`, `nums2 = [2,5,6]`, `n = 3`

(`_` = placeholder 0)

| step | i (nums1[i]) | j (nums2[j]) | w | compare | write | nums1 after |
|---|---|---|---|---|---|---|
| 1 | 2 (3) | 2 (6) | 5 | 3 > 6? no | nums1[5] = 6, j=1 | `[1,2,3,_,_,6]` |
| 2 | 2 (3) | 1 (5) | 4 | 3 > 5? no | nums1[4] = 5, j=0 | `[1,2,3,_,5,6]` |
| 3 | 2 (3) | 0 (2) | 3 | 3 > 2? **yes** | nums1[3] = 3, i=1 | `[1,2,3,3,5,6]` |
| 4 | 1 (2) | 0 (2) | 2 | 2 > 2? no (tie) | nums1[2] = 2, j=−1 | `[1,2,2,3,5,6]` |
| — | | j = −1 | | loop ends | | `[1,2,2,3,5,6]` ✓ |

The `1, 2` at indices 0 and 1 were never touched. They were already in place.

### Dry run 2: `nums1 = [4,5,6,_,_,_]`, `nums2 = [1,2,3]` (all of nums1 is bigger)

| step | i (val) | j (val) | w | nums1[i] > nums2[j]? | write | nums1 after |
|---|---|---|---|---|---|---|
| 1 | 2 (6) | 2 (3) | 5 | yes | 6 → [5], i=1 | `[4,5,6,_,_,6]` |
| 2 | 1 (5) | 2 (3) | 4 | yes | 5 → [4], i=0 | `[4,5,6,_,5,6]` |
| 3 | 0 (4) | 2 (3) | 3 | yes | 4 → [3], i=−1 | `[4,5,6,4,5,6]` |
| 4 | −1 | 2 (3) | 2 | i < 0 → else | 3 → [2], j=1 | `[4,5,3,4,5,6]` |
| 5 | −1 | 1 (2) | 1 | else | 2 → [1], j=0 | `[4,2,3,4,5,6]` |
| 6 | −1 | 0 (1) | 0 | else | 1 → [0], j=−1 | `[1,2,3,4,5,6]` ✓ |

This is where the `i >= 0` guard matters. Without it, step 4 would read `nums1[-1]`.

### Dry run 3: `m = 0` → `nums1 = [_]`, `nums2 = [1]`
`i = −1`, `j = 0`, `w = 0`. The guard sends us to the else branch: `nums1[0] = 1`. Result `[1]` ✓

### Dry run 4: `n = 0` → `nums2 = []`
`j = −1`, so the loop never runs. nums1 stays as it is ✓

### Complexity
- **Time O(m + n):** each step places one element.
- **Space O(1).**

---

## 7. Comparing the approaches

| | Copy + sort | Front merge with copy | Back merge |
|---|---|---|---|
| Time | O((m+n) log(m+n)) | O(m+n) | **O(m+n)** |
| Extra space | O(log(m+n)) | O(m) | **O(1)** |
| Uses "already sorted"? | no | yes | yes |

---

## 8. Edge cases and common mistakes

**Edge cases:** `m = 0`, `n = 0`, everything in nums2 smaller (or larger) than nums1, duplicates across both arrays, negative numbers.

**Mistakes:**
1. Looping `while (i >= 0 && j >= 0)` and then forgetting to copy the nums2 leftovers. (nums1 leftovers don't need copying; nums2 leftovers do.)
2. Forgetting the `i >= 0` check, which reads `nums1[-1]`.
3. Merging from the front without a copy, which overwrites unread nums1 values.
4. Using `nums1.length` as `m` (it's m + n).

---

## 9. Interview questions and answers

**Q1. Why merge from the back?**
> The free space is at the end of nums1. Writing the largest element there can never overwrite an unread value, because the write pointer always stays ahead of nums1's read pointer. That gives O(1) extra space.

**Q2. Why does the loop only check `j >= 0`?**
> Once nums2 is used up, the remaining nums1 elements are the smallest ones and they're already in their final positions at the front. Nothing left to move.

**Q3. Is this merge stable?**
> We take nums2 on ties when merging from the back, which means nums2's copy ends up **after** nums1's copy. So equal elements keep the "nums1 before nums2" order: it's stable. Using `>=` instead would flip that.

### Follow-up questions

**F1. Merge two sorted linked lists.** → [03-Linked-Lists E02](../../03-Linked-Lists/02-Easy/E02_MergeTwoSortedLists.md)
> Front-to-back merge with a dummy head node. No space issue there, because we relink nodes instead of overwriting them.

**F2. Merge k sorted arrays/lists.** → [03-Linked-Lists H02](../../03-Linked-Lists/04-Hard/H02_MergeKSortedLists.md)
> Use a min-heap holding the current head of each list. Pop the smallest, push its successor. O(N log k) for N total elements.

**F3. Find the median of two sorted arrays (LC 4).**
> Merging would be O(m+n). The optimal answer uses binary search on the partition of the smaller array: O(log(min(m, n))).

**F4. Intersection of two sorted arrays.** → [E11](E11_IntersectionOfTwoArrays.md)
> Two pointers from the front: advance the pointer at the smaller value; on equal values, record and advance both.

**F5. What if nums1 had no extra space?**
> Then you need an output array (O(m+n) space). Or, for a true in-place merge, there are complex O(1)-space algorithms (e.g. gap/shell-sort based, O((m+n) log(m+n))), which are rarely expected.

---

## 10. Running the file

```bash
java E08_MergeSortedArray.java      # 5/5 tests passed
```

**One-line summary:** *"The free space is at the end, so place the biggest remaining element there, walking backwards."*
