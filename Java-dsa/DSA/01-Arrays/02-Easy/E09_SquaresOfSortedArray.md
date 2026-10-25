# E09 · Squares of a Sorted Array

| | |
|---|---|
| **LeetCode** | [#977 Squares of a Sorted Array](https://leetcode.com/problems/squares-of-a-sorted-array/) |
| **Difficulty** | Easy |
| **Pattern** | Two pointers (opposite ends), fill the result from the back |
| **Code** | [E09_SquaresOfSortedArray.java](E09_SquaresOfSortedArray.java) |
| **Run** | `java E09_SquaresOfSortedArray.java` |

---

## 1. The problem

`nums` is sorted in ascending order and **may contain negatives**. Return an array of the **squares** of each number, also sorted in ascending order.

| nums | answer |
|---|---|
| `[-4, -1, 0, 3, 10]` | `[0, 1, 9, 16, 100]` |
| `[-7, -3, 2, 3, 11]` | `[4, 9, 9, 49, 121]` |
| `[-5, -3, -1]` | `[1, 9, 25]` |
| `[1, 2, 3]` | `[1, 4, 9]` |
| `[-2]` | `[4]` |

**Constraints:** `1 ≤ n ≤ 10⁴`, `-10⁴ ≤ nums[i] ≤ 10⁴`, sorted. LeetCode asks: *can you do it in O(n)?*

---

## 2. How to think about it

1. **Simple:** square everything, then sort. O(n log n).
2. **Why isn't the squared array already sorted?** Negatives. `-4 < 3`, but `(-4)² = 16 > 3² = 9`. Squaring "folds" the number line at 0:

```
values:  -4  -1   0   3   10
squares: 16   1   0   9  100     ← decreasing, then increasing (a "V" shape)
```

3. **Insight:** in a V shape, the **largest** value is always at one of the two **ends** (the biggest absolute value is either the most negative or the most positive number).
4. So put pointers at both ends, compare their squares, place the **bigger** square at the **end** of the result, and move that pointer inward. We fill the result from back to front.

---

## 3. Java concepts used in this problem

### 3.1 A new output array: `int[] result = new int[n];`

Unlike E03/E06, we can't easily work in place here (we'd overwrite values we still need), so we create an output array. New `int` arrays start filled with `0`. Other default values: `boolean` → `false`, `double` → `0.0`, `char` → `'\u0000'`, objects (`String`, `Integer`...) → `null`.

The output array **doesn't count as extra space** in the usual analysis, because the problem requires returning it. So we say "O(1) extra space beyond the output".

### 3.2 Squaring: `x * x` vs `Math.pow(x, 2)`

```java
int sq = x * x;              // ✓ exact int arithmetic, fast
double d = Math.pow(x, 2);   // returns a double, slower, needs a cast back to int
```

Prefer `x * x` for integers. **Overflow check:** max |x| = 10⁴ → x² = 10⁸, which fits in an `int` (max about 2.1 × 10⁹). If |x| could reach 10⁵, then x² = 10¹⁰ would overflow. Then you'd use `(long) x * x`. Note: the cast must come **before** the multiplication; `(long)(x * x)` is too late, because the overflow has already happened.

### 3.3 A `for` loop that counts down

```java
for (int w = n - 1; w >= 0; w--) { ... }   // w = n-1, n-2, ..., 0
```

`w` is the **write** position, moving from the back to the front.

### 3.4 `Math.abs`

`Math.abs(-4)` is 4. Comparing `Math.abs(nums[left]) > Math.abs(nums[right])` gives the same answer as comparing the squares. (Watch out: `Math.abs(Integer.MIN_VALUE)` is still negative because of overflow. That can't happen with these constraints.)

---

## 4. Approach 1: Square, then sort

```java
static int[] sortedSquaresSimple(int[] nums) {
    int[] result = new int[nums.length];
    for (int i = 0; i < nums.length; i++) result[i] = nums[i] * nums[i];
    Arrays.sort(result);
    return result;
}
```

Or as a one-liner with streams: `Arrays.stream(nums).map(x -> x * x).sorted().toArray();`

- `.map(x -> x * x)` applies the **lambda** `x -> x * x` to each value.
- **Time O(n log n), space O(n)** for the output.

**Dry run** `[-4,-1,0,3,10]` → squares `[16,1,0,9,100]` → sorted `[0,1,9,16,100]`.

---

## 5. Approach 2: Two pointers from the ends (optimal)

### Code ([E09_SquaresOfSortedArray.java:37-53](E09_SquaresOfSortedArray.java#L37-L53))

```java
static int[] sortedSquares(int[] nums) {
    int n = nums.length;
    int[] result = new int[n];
    int left = 0, right = n - 1;
    for (int w = n - 1; w >= 0; w--) {                 // biggest square goes last
        int ls = nums[left] * nums[left];
        int rs = nums[right] * nums[right];
        if (ls > rs) {
            result[w] = ls;
            left++;
        } else {
            result[w] = rs;
            right--;
        }
    }
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int[] result = new int[n];` | the output, filled from the back |
| `int left = 0, right = n - 1;` | the two candidates for the largest remaining square |
| `for (int w = n - 1; w >= 0; w--)` | exactly n iterations, one result slot each, biggest first |
| `int ls = nums[left] * nums[left];` | square at the left end |
| `int rs = nums[right] * nums[right];` | square at the right end |
| `if (ls > rs) { result[w] = ls; left++; }` | the left square is bigger → it goes in slot w; that element is used, so move left inward |
| `else { result[w] = rs; right--; }` | the right square is bigger or equal → place it and move right inward |
| `return result;` | |

**Why the loop never goes wrong when `left == right`:** in the last iteration both pointers are on the same element, so `ls == rs`, the else branch places it, and the loop ends because `w` reaches −1. We never read outside the range.

### Dry run 1: `[-4, -1, 0, 3, 10]`

| w | left (val) | right (val) | ls | rs | bigger | result after | move |
|---|---|---|---|---|---|---|---|
| 4 | 0 (−4) | 4 (10) | 16 | 100 | rs | `[_,_,_,_,100]` | right=3 |
| 3 | 0 (−4) | 3 (3) | 16 | 9 | ls | `[_,_,_,16,100]` | left=1 |
| 2 | 1 (−1) | 3 (3) | 1 | 9 | rs | `[_,_,9,16,100]` | right=2 |
| 1 | 1 (−1) | 2 (0) | 1 | 0 | ls | `[_,1,9,16,100]` | left=2 |
| 0 | 2 (0) | 2 (0) | 0 | 0 | tie → rs | `[0,1,9,16,100]` ✓ | right=1 |

### Dry run 2: `[-7, -3, 2, 3, 11]`

| w | left (val) | right (val) | ls | rs | place | move |
|---|---|---|---|---|---|---|
| 4 | −7 | 11 | 49 | 121 | 121 | right → 3 |
| 3 | −7 | 3 | 49 | 9 | 49 | left → 1 |
| 2 | −3 | 3 | 9 | 9 | tie → 9 (right) | right → 2 |
| 1 | −3 | 2 | 9 | 4 | 9 | left → 2 |
| 0 | 2 | 2 | 4 | 4 | 4 | right → 1 |

Result: `[4, 9, 9, 49, 121]` ✓

### Dry run 3: `[-5, -3, -1]` (all negative)

| w | left | right | ls | rs | place |
|---|---|---|---|---|---|
| 2 | −5 | −1 | 25 | 1 | 25 (left++) |
| 1 | −3 | −1 | 9 | 1 | 9 (left++) |
| 0 | −1 | −1 | 1 | 1 | 1 |

Result: `[1, 9, 25]` ✓. With all negatives, the left side always wins.

### Complexity
- **Time O(n):** each iteration fills one slot.
- **Space O(n)** for the output, O(1) extra beyond it.

---

## 6. (Bonus) Approach 3: Find the split, then merge outward

Another O(n) way: find the first non-negative index `p`. Then `nums[p-1], nums[p-2], ...` (negatives, read leftward) and `nums[p], nums[p+1], ...` (non-negatives) are **two sorted lists of squares**. Merge them from the **smallest** end, like E08:

```java
static int[] sortedSquaresMerge(int[] nums) {
    int n = nums.length, p = 0;
    while (p < n && nums[p] < 0) p++;      // first non-negative
    int i = p - 1, j = p, w = 0;
    int[] res = new int[n];
    while (i >= 0 && j < n) {
        int a = nums[i] * nums[i], b = nums[j] * nums[j];
        if (a < b) { res[w++] = a; i--; } else { res[w++] = b; j++; }
    }
    while (i >= 0) { res[w++] = nums[i] * nums[i]; i--; }
    while (j < n)  { res[w++] = nums[j] * nums[j]; j++; }
    return res;
}
```

Same complexity, but more code. The "from both ends, fill from the back" version is simpler because it needs no split search.

---

## 7. Comparing the approaches

| | Square + sort | Two ends, fill back | Split + merge |
|---|---|---|---|
| Time | O(n log n) | **O(n)** | O(n) |
| Extra space | O(log n) sort | O(1) | O(1) |
| Code size | tiny | small | larger |

---

## 8. Edge cases and common mistakes

**Edge cases:** all negative, all positive, one element, zeros, equal absolute values (`[-3, 3]`).

**Mistakes:**
1. Filling the result from the **front** with the bigger square, which produces a descending order.
2. Using `while (left < right)` and forgetting the last element when they meet. The `for w` version avoids this.
3. Thinking the smallest square is at an end. It's in the **middle** (near 0); only the **largest** is guaranteed to be at an end.

---

## 9. Interview questions and answers

**Q1. Why is the largest square always at an end?**
> The array is sorted, so the element with the largest absolute value is either the minimum (most negative, at the left) or the maximum (at the right). Squares grow with absolute value.

**Q2. Can you do it in place, without an output array?**
> Not easily in O(n). The output order mixes elements from both ends, so writing into nums would overwrite values still needed. In-place would mean O(n log n) (square in place, then sort).

**Q3. Where else does this "two ends shrink inward" pattern appear?**
> Two Sum II on sorted input, Container With Most Water, 3Sum, Valid Palindrome, Trapping Rain Water.

### Follow-up questions

**F1. Return the squares in descending order.**
> Fill the result from the front: `w` goes from 0 to n−1.

**F2. Apply any function `f(x) = ax² + bx + c` and return the sorted results (LC 360, Sort Transformed Array).**
> A parabola is V-shaped too. If `a ≥ 0`, the biggest values are at the ends: fill from the back. If `a < 0`, the smallest values are at the ends: fill from the front. Same two pointers.

**F3. Count the distinct absolute values in a sorted array.**
> Two pointers from the ends: compare `|left|` with `|right|`, count each new absolute value, and skip duplicates on both sides.

**F4. Find the k elements closest to x in a sorted array (LC 658).**
> Binary search for the left boundary of a window of size k, or shrink a window from both ends by dropping whichever end is farther from x.

---

## 10. Running the file

```bash
java E09_SquaresOfSortedArray.java      # 5/5 tests passed
```

**One-line summary:** *"The biggest square is at one of the ends; compare the ends and fill the result from the back."*
