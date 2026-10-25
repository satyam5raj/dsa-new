# M14 · Subarray Product Less Than K

| | |
|---|---|
| **LeetCode** | [#713 Subarray Product Less Than K](https://leetcode.com/problems/subarray-product-less-than-k/) |
| **Difficulty** | Medium |
| **Pattern** | Sliding window + "count the subarrays ending at right" |
| **Code** | [M14_SubarrayProductLessThanK.java](M14_SubarrayProductLessThanK.java) |
| **Run** | `java M14_SubarrayProductLessThanK.java` |

---

## 1. The problem

The array holds **positive** integers. Count the **contiguous subarrays** whose product is **strictly less than `k`**.

| nums | k | answer | subarrays |
|---|---|---|---|
| `[10, 5, 2, 6]` | 100 | 8 | `[10]`, `[5]`, `[2]`, `[6]`, `[10,5]`, `[5,2]`, `[2,6]`, `[5,2,6]` |
| `[1, 2, 3]` | 0 | 0 | nothing is < 0 |
| `[1, 1, 1]` | 2 | 6 | all 6 subarrays have product 1 |
| `[100]` | 100 | 0 | 100 is not **strictly** less |

**Constraints:** `1 ≤ n ≤ 3 × 10⁴`, `1 ≤ nums[i] ≤ 1000`, `0 ≤ k ≤ 10⁶`.

---

## 2. How to think about it

1. **Brute force:** every start, extend while the product < k (positive values let you stop early). O(n²) in the worst case.
2. **Sliding window:** all the values are **positive integers ≥ 1**, so the product **can only grow** when the window expands and **only shrink** when it contracts. Keep the window `[left..right]` with product < k.
3. **The counting trick (very reusable!):** when `[left..right]` is valid, **every** subarray that **ends at `right`** and starts at or after `left` is also valid (it's a sub-window with a smaller or equal product):

```
[left..right], [left+1..right], ..., [right..right]   →   right − left + 1 subarrays
```

   Add `right − left + 1` for each `right`. Each subarray is counted **exactly once**, by its **end** position.

### Why counting by the end works
A subarray `[i..j]` with product < k is counted when `right = j`, as long as `i ≥ left` at that moment. Could it have `i < left`? No: `left` only moved past i because `[i..j']` with some `j' ≤ j` was already ≥ k, and extending it to j only makes it bigger. So everything valid is counted, and nothing invalid is.

### Edge case: k ≤ 1
Positive integers have product ≥ 1, so nothing is < 1. Return 0 right away. Without this guard, the shrink loop would push `left` past `right` (the product of an empty window is 1, which is still ≥ k), causing a negative count or an out-of-bounds error.

---

## 3. Java concepts used in this problem

### 3.1 `long` for products

```java
long product = 1;
product *= nums[right];
```

The window product stays < k ≤ 10⁶ after shrinking, but **before** shrinking it can be up to 10⁶ × 1000 = 10⁹, which fits in an `int`. Using `long` costs nothing and protects against overflow if the constraints change.

### 3.2 Division to "remove" an element from a product

```java
product /= nums[left++];
```

Just like subtracting removes an element from a running **sum**, dividing removes it from a running **product**. It's exact here because the product was built by multiplying that same element in. (This only works because there are no zeros. A zero would make the product 0 forever.)

### 3.3 Counting with `count += right - left + 1`

This "add the number of valid windows ending here" step turns a **max-length** window into a **count-of-subarrays** algorithm. Remember it.

---

## 4. Approach 1: Brute force with early stopping

```java
static int numSubarrayProductLessThanKBrute(int[] nums, int k) {
    int count = 0;
    for (int i = 0; i < nums.length; i++) {
        long p = 1;
        for (int j = i; j < nums.length; j++) {
            p *= nums[j];
            if (p >= k) break;              // positive values: extending can't help
            count++;
        }
    }
    return count;
}
```

**Dry run** `[10, 5, 2, 6]`, k = 100:

| i | subarrays counted | count |
|---|---|---|
| 0 | [10] (10), [10,5] (50); [10,5,2] = 100 → stop | 2 |
| 1 | [5], [5,2] (10), [5,2,6] (60) | 5 |
| 2 | [2], [2,6] (12) | 7 |
| 3 | [6] | 8 |

**Return 8.** **O(n²) worst case** (e.g. all 1s).

---

## 5. Approach 2: Logarithms + binary search (a curiosity)

Taking logarithms turns products into sums: `prod < k ⇔ Σ log(nums) < log(k)`. Build prefix sums of logs (increasing, since every log(x) ≥ 0) and, for each start, binary-search the furthest valid end. **O(n log n)**. But floating-point rounding makes the boundary checks fragile, so it's rarely worth it. Mention it only as an alternative idea.

---

## 6. Approach 3: Sliding window with counting (the file's solution)

### Code ([M14_SubarrayProductLessThanK.java:42-54](M14_SubarrayProductLessThanK.java#L42-L54))

```java
static int numSubarrayProductLessThanK(int[] nums, int k) {
    if (k <= 1) return 0;
    long product = 1;
    int left = 0, count = 0;
    for (int right = 0; right < nums.length; right++) {
        product *= nums[right];
        while (product >= k) {
            product /= nums[left++];        // shrink until valid again
        }
        count += right - left + 1;          // subarrays ending at 'right'
    }
    return count;
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (k <= 1) return 0;` | no product of positive integers is < 1 |
| `long product = 1;` | the empty product |
| `product *= nums[right];` | **expand** the window |
| `while (product >= k) product /= nums[left++];` | **shrink** until the product is valid again. Worst case (when `nums[right] ≥ k`), the window becomes empty (`left = right + 1`) with product 1, which is < k because k > 1, so the loop always stops |
| `count += right - left + 1;` | the number of valid subarrays ending at `right` (0 if the window is empty) |

### Dry run 1: `[10, 5, 2, 6]`, k = 100

| right | add | product | shrink? | window | + count | total |
|---|---|---|---|---|---|---|
| 0 | 10 | 10 | no | [10] | 1 | 1 |
| 1 | 5 | 50 | no | [10, 5] | 2 | 3 |
| 2 | 2 | 100 | **yes**: ÷10 → 10, left = 1 | [5, 2] | 2 | 5 |
| 3 | 6 | 60 | no | [5, 2, 6] | 3 | 8 |

**Return 8** ✓. At right = 3 the three new subarrays are `[5,2,6]`, `[2,6]`, `[6]`.

### Dry run 2: `[100]`, k = 100

| right | product | shrink | window | + count |
|---|---|---|---|---|
| 0 | 100 | ÷100 → 1, left = 1 | empty | 0 − 1 + 1 = 0 |

**Return 0** ✓. The window can become **empty**, and the formula gives 0.

### Dry run 3: `[1, 1, 1]`, k = 2
The product stays 1, and there's never any shrinking. Counts: +1, +2, +3 → **6** ✓

### Complexity
- **Time O(n):** each element is multiplied in once and divided out at most once.
- **Space O(1).**

---

## 7. Comparing the approaches

| | Brute force | Log + binary search | Sliding window |
|---|---|---|---|
| Time | O(n²) | O(n log n) | **O(n)** |
| Space | O(1) | O(n) | **O(1)** |
| Exact arithmetic | yes | no (floating point) | yes |

---

## 8. Edge cases and common mistakes

**Edge cases:** k = 0 or 1 (answer 0), an element ≥ k (the window empties), all ones, a single element.

**Mistakes:**
1. Forgetting the `k <= 1` guard, which leads to `left > right` and wrong counts (or division errors).
2. Using `<=` instead of `<` (the problem says **strictly** less).
3. Counting only the maximal windows (`count++`) instead of `right − left + 1`.
4. Overflowing an `int` product in variants with bigger values.

---

## 9. Interview questions and answers

**Q1. Why does adding `right − left + 1` count every valid subarray exactly once?**
> Every subarray has exactly one end index. For a fixed end `right`, the valid starts are exactly `left..right` (the window is the longest valid one ending there, and its sub-windows are valid too, because the values are ≥ 1). Summing over the end positions counts each subarray once.

**Q2. What if the array could contain zeros or negatives?**
> A zero makes every product containing it 0 (always < k for k > 0), so you'd count subarrays containing a zero separately, and you can't divide by zero. Negatives break monotonicity (the sign flips). The sliding window no longer applies; you'd need segment-based counting.

**Q3. Where else is "count by right endpoint" used?**
> Counting subarrays with at most K distinct values (and then "exactly K = atMost(K) − atMost(K−1)", [H05](../04-Hard/H05_SubarraysWithKDifferentIntegers.md)), counting binary subarrays with a given sum, and counting the subarrays with sum < k.

### Follow-up questions

**F1. Count the subarrays with sum < k (positive numbers).**
> The same code with `+=` / `-=` instead of `*=` / `/=`.

**F2. Count the subarrays with exactly K distinct integers (LC 992).** → [04-Hard H05](../04-Hard/H05_SubarraysWithKDifferentIntegers.md)
> `exactly(K) = atMost(K) − atMost(K − 1)`, where each `atMost` uses this counting trick.

**F3. Count the subarrays where max − min ≤ limit.**
> A sliding window with monotonic deques for the max and min ([05-Queue M07](../../05-Queue/03-Medium/M07_LongestSubarrayAbsDiffLimit.md)), plus the same `right − left + 1` counting.

**F4. Return the subarrays themselves.**
> The output can be O(n²) in size; list `[i..right]` for each i in `[left, right]`.

---

## 10. Running the file

```bash
java M14_SubarrayProductLessThanK.java      # 4/4 tests passed
```

**One-line summary:** *"Keep the product < k by shrinking from the left; each right end adds (right − left + 1) new subarrays."*
