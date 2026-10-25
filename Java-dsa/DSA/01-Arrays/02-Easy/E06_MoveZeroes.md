# E06 · Move Zeroes

| | |
|---|---|
| **LeetCode** | [#283 Move Zeroes](https://leetcode.com/problems/move-zeroes/) |
| **Difficulty** | Easy |
| **Pattern** | Two pointers (slow/fast, in place) |
| **Code** | [E06_MoveZeroes.java](E06_MoveZeroes.java) |
| **Run** | `java E06_MoveZeroes.java` |

---

## 1. The problem

Move all `0`s to the **end** of the array while keeping the **relative order** of the non-zero elements. Do it **in place**, without making a copy of the array.

| before | after |
|---|---|
| `[0, 1, 0, 3, 12]` | `[1, 3, 12, 0, 0]` |
| `[0]` | `[0]` |
| `[1, 2, 3]` | `[1, 2, 3]` |
| `[0, 0, 1]` | `[1, 0, 0]` |
| `[4, 0, -2, 0, 5]` | `[4, -2, 5, 0, 0]` |

"Relative order" means 1 must still come before 3, and 3 before 12. You can't just sort.

**Constraints:** `1 ≤ n ≤ 10⁴`. The method returns nothing (`void`); you change `nums` itself.

---

## 2. How to think about it

- **Easy but not allowed:** make a new array, copy the non-zeros, then fill the rest with zeros. That uses O(n) extra space.
- **Two pointers on the same array:**
  - `i` (**fast**) visits every element.
  - `insert` (**slow**) marks where the **next non-zero** should go.
  - When `nums[i]` is non-zero, put it at `insert` and move `insert` forward.
- **The invariant** (always true): everything before `insert` is a non-zero, in its original order. Everything from `insert` to `i−1` is a zero.

```
   [ non-zeros in order | zeros |  not looked at yet  ]
     0 ........ insert-1  insert..i-1   i ........ n-1
```

This **slow/fast "write pointer"** pattern also solves E07 (Remove Duplicates) and E24 (Remove Element). Learn it once, use it everywhere.

---

## 3. Java concepts used in this problem

### 3.1 Swapping two array elements

Java has no built-in `swap` for arrays, so you use a temporary variable:

```java
int tmp = nums[a];   // 1. save a's value
nums[a] = nums[b];   // 2. overwrite a with b
nums[b] = tmp;       // 3. put the saved value into b
```

Without `tmp`, step 2 would destroy `nums[a]` before you could copy it. It's like swapping the contents of two glasses: you need a third glass.

Why can't you write a `swap(int x, int y)` helper for two `int` variables? Java passes **copies** of primitives, so swapping the copies changes nothing for the caller. A helper that takes the **array** and two indices does work: `swap(int[] a, int i, int j)`, because the array reference points to the caller's array (see E03).

### 3.2 `void` methods that change their input

`static void moveZeroes(int[] nums)` returns nothing. The result is the changed array itself. The caller sees the change because both share the same array object.

### 3.3 Varargs: `int... nums`

The test helper in this file uses:

```java
static int[] run(int... nums) {   // "any number of int arguments"
    moveZeroes(nums);
    return nums;
}
run(0, 1, 0, 3, 12);  // Java packs the arguments into new int[]{0, 1, 0, 3, 12}
```

Inside the method, `nums` is an ordinary `int[]`. A varargs parameter must be the **last** parameter. You've already used one without noticing: `System.out.printf(format, args...)`.

### 3.4 `!=` (not equal)

`nums[i] != 0` is true when the value isn't zero. Like `==`, it compares values for primitives and references for objects.

---

## 4. Approach 1: Extra array (simple, but breaks the in-place rule)

```java
static void moveZeroesExtra(int[] nums) {
    int[] out = new int[nums.length];          // all zeros by default
    int k = 0;
    for (int x : nums) if (x != 0) out[k++] = x;   // copy non-zeros in order
    System.arraycopy(out, 0, nums, 0, nums.length); // copy back into nums
}
```

- `out[k++] = x;` → **post-increment**: it uses the *current* `k` as the index, *then* adds 1 to it. It's the same as `out[k] = x; k++;`.
- The tail of `out` is already 0, because new `int` arrays are filled with zeros.
- `System.arraycopy(src, srcPos, dest, destPos, length)` is a fast built-in copy.

**O(n) time, O(n) space.** It's correct, but the problem forbids the extra array. It's still a good thing to mention first in an interview.

---

## 5. Approach 2: Two passes, copy forward then fill zeros (in place)

Mentioned in the `.java` header as the alternative:

```java
static void moveZeroesTwoPass(int[] nums) {
    int insert = 0;
    for (int x : nums) {
        if (x != 0) nums[insert++] = x;       // pack non-zeros at the front
    }
    while (insert < nums.length) {
        nums[insert++] = 0;                   // the rest must be zeros
    }
}
```

| Line | What it does |
|---|---|
| `nums[insert++] = x;` | write the non-zero at the next free front slot. This is safe because `insert ≤` the read position, so we never overwrite a value we haven't read yet |
| `while (insert < n) nums[insert++] = 0;` | overwrite the leftover tail with zeros |

**Dry run** `[0, 1, 0, 3, 12]`:

| x | non-zero? | write | insert after | array |
|---|---|---|---|---|
| 0 | no | — | 0 | `[0,1,0,3,12]` |
| 1 | yes | nums[0]=1 | 1 | `[1,1,0,3,12]` |
| 0 | no | — | 1 | |
| 3 | yes | nums[1]=3 | 2 | `[1,3,0,3,12]` |
| 12 | yes | nums[2]=12 | 3 | `[1,3,12,3,12]` |
| fill | | nums[3]=0, nums[4]=0 | 5 | `[1,3,12,0,0]` |

**O(n) time, O(1) space.** It always writes n values in total.

---

## 6. Approach 3: One pass with swaps (the file's solution)

### Code ([E06_MoveZeroes.java:41-51](E06_MoveZeroes.java#L41-L51))

```java
static void moveZeroes(int[] nums) {
    int insert = 0;
    for (int i = 0; i < nums.length; i++) {
        if (nums[i] != 0) {
            int tmp = nums[insert];        // swap nums[i] into its final place
            nums[insert] = nums[i];
            nums[i] = tmp;
            insert++;
        }
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `int insert = 0;` | the next non-zero belongs at the front |
| `for (int i = 0; ...)` | fast pointer: look at every element |
| `if (nums[i] != 0)` | only non-zeros move. Zeros are skipped, and they drift right as the swaps happen |
| `tmp = nums[insert]; nums[insert] = nums[i]; nums[i] = tmp;` | swap the non-zero into slot `insert`. Whatever was at `insert` (a zero, or the same element when `insert == i`) goes to `i` |
| `insert++;` | the next free front slot |

**Why swap and not just copy?** The swap moves the zero that was sitting at `insert` back to position `i`, so the zeros end up at the end **in the same pass**, with no fill step.

**When `insert == i`** (no zeros seen yet), the element swaps with itself. That's harmless, just a wasted write. You can add `if (insert != i)` to skip it.

### Dry run 1: `[0, 1, 0, 3, 12]`

| i | nums[i] | action | insert after | array after |
|---|---|---|---|---|
| start | | | 0 | `[0, 1, 0, 3, 12]` |
| 0 | 0 | skip | 0 | `[0, 1, 0, 3, 12]` |
| 1 | 1 | swap(0, 1) | 1 | `[1, 0, 0, 3, 12]` |
| 2 | 0 | skip | 1 | `[1, 0, 0, 3, 12]` |
| 3 | 3 | swap(1, 3) | 2 | `[1, 3, 0, 0, 12]` |
| 4 | 12 | swap(2, 4) | 3 | `[1, 3, 12, 0, 0]` ✓ |

### Dry run 2: `[4, 0, -2, 0, 5]`

| i | nums[i] | action | insert after | array after |
|---|---|---|---|---|
| 0 | 4 | swap(0,0), a self-swap | 1 | `[4, 0, -2, 0, 5]` |
| 1 | 0 | skip | 1 | |
| 2 | −2 | swap(1, 2) | 2 | `[4, -2, 0, 0, 5]` |
| 3 | 0 | skip | 2 | |
| 4 | 5 | swap(2, 4) | 3 | `[4, -2, 5, 0, 0]` ✓ |

### Dry run 3: `[1, 2, 3]` (no zeros)
Every step is a self-swap (`insert == i`). The array stays `[1, 2, 3]`.

### Complexity
- **Time O(n):** one pass.
- **Space O(1).**
- **Writes:** 2 per non-zero (swap). Compared with the two-pass version: if zeros are **rare**, the swap version (with the `insert != i` check) writes very little; if zeros are **common**, the two-pass version can write less.

---

## 7. Comparing the approaches

| | Extra array | Copy + fill | Swap (one pass) |
|---|---|---|---|
| Time | O(n) | O(n) | O(n) |
| Extra space | O(n) ✗ | **O(1)** | **O(1)** |
| Passes | 2 | 2 | 1 |
| Keeps order | yes | yes | yes |

---

## 8. Edge cases and common mistakes

**Edge cases:** no zeros, all zeros, zeros only at the start/end, a single element, negative numbers (they are non-zero!).

**Mistakes:**
1. Sorting the array, which destroys the relative order.
2. Removing zeros from an `ArrayList` in a loop: each remove is O(n), so O(n²) overall, and removing while iterating causes index bugs.
3. Two pointers from **both ends** (swap a zero at the left with a non-zero at the right). It moves the zeros, but **breaks the order**.

---

## 9. Interview questions and answers

**Q1. Why does the relative order of non-zeros stay the same?**
> Non-zeros are written to positions 0, 1, 2… in the same order the fast pointer meets them. A non-zero never jumps over another non-zero.

**Q2. Why is it safe to write at `insert` while reading at `i`?**
> `insert ≤ i` always, because insert only moves when i finds a non-zero. So we only overwrite positions we've already read.

**Q3. How would you minimise the number of writes?**
> Skip the self-swap when `insert == i`. Each remaining swap then moves a non-zero exactly once into its final place, which is the minimum number of moves.

### Follow-up questions

**F1. Move all zeros to the FRONT, keeping order.**
> Mirror it: scan from right to left with `insert = n−1`, and swap non-zeros to the back.

**F2. Remove all occurrences of a value `val` and return the new length.** → [E24 Remove Element](E24_RemoveElement.md)
> Same write pointer: copy elements `!= val` forward and return `insert`.

**F3. Remove duplicates from a sorted array.** → [E07](E07_RemoveDuplicatesFromSortedArray.md)
> Same pattern: write `nums[i]` only when it differs from the last written value.

**F4. Order doesn't matter, minimise operations.**
> Use two pointers from both ends: swap a left zero with a right non-zero. Each element moves at most once.

**F5. Group the array into three parts (e.g. negatives, zeros, positives) in one pass.**
> That's the **Dutch National Flag** algorithm with three pointers (`low`, `mid`, `high`). It's Sort Colors (LC 75), covered in [ArrayPatterns](../01-Basics/ArrayPatterns.java).

**F6. The same task on a linked list.**
> Build two chains (non-zeros and zeros) with dummy heads, then connect the non-zero chain to the zero chain. O(n), O(1).

---

## 10. Running the file

```bash
java E06_MoveZeroes.java      # 5/5 tests passed
```

**One-line summary:** *"Fast pointer finds non-zeros, slow pointer marks where the next one goes; swap them."*
