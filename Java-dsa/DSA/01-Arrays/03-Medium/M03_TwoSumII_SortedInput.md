# M03 · Two Sum II – Input Array Is Sorted

| | |
|---|---|
| **LeetCode** | [#167 Two Sum II - Input Array Is Sorted](https://leetcode.com/problems/two-sum-ii-input-array-is-sorted/) |
| **Difficulty** | Medium |
| **Pattern** | Two pointers (opposite ends) |
| **Code** | [M03_TwoSumII_SortedInput.java](M03_TwoSumII_SortedInput.java) |
| **Run** | `java M03_TwoSumII_SortedInput.java` |

---

## 1. The problem

`numbers` is sorted in ascending order. Return the **1-indexed** positions `[i, j]` (`i < j`) of the two numbers that add up to `target`. Exactly one solution exists; you can't use the same element twice. **Use only O(1) extra space.**

| numbers | target | answer (1-indexed) |
|---|---|---|
| `[2, 7, 11, 15]` | 9 | `[1, 2]` |
| `[2, 3, 4]` | 6 | `[1, 3]` |
| `[-1, 0]` | −1 | `[1, 2]` |
| `[1,2,3,4,4,9,56,90]` | 8 | `[4, 5]` (the two 4s) |

**Constraints:** `2 ≤ n ≤ 3 × 10⁴`, sorted, exactly one answer.

"1-indexed" means the first element is position 1, not 0. That's an easy point to lose.

---

## 2. How to think about it

- The **HashMap** solution from [E01 Two Sum](../02-Easy/E01_TwoSum.md) works, but uses O(n) space, and the problem wants O(1).
- **Use the sorted order.** Put `left` at the smallest element and `right` at the largest:
  - `sum == target` → found
  - `sum < target` → we need a **bigger** sum. The only way to increase it is `left++` (moving right down would make it smaller).
  - `sum > target` → we need a **smaller** sum → `right--`.

### Why we can never skip the answer (the proof interviewers like)
When `sum < target`, we discard `numbers[left]` for good. Could it still be part of the answer? Its **biggest** possible partner is `numbers[right]` (everything to the left of right is smaller, and everything to its right was already discarded), and even that partner gives a sum that's too small. So `numbers[left]` pairs with **nobody**, and dropping it is safe. The argument for `right--` is symmetric. Each step removes one element that **can't** be in the answer, so the answer survives until the pointers find it.

---

## 3. Java concepts used in this problem

### 3.1 Converting between 0-indexed and 1-indexed

Java arrays are **0-indexed**. The problem wants **1-indexed** output, so we add 1 when returning: `new int[]{left + 1, right + 1}`. Keep all the internal logic 0-indexed and convert only at the boundary (input/output).

### 3.2 Binary search: `Arrays.binarySearch` (used in approach 2)

```java
int idx = Arrays.binarySearch(arr, fromIndex, toIndex, key);  // searches arr[fromIndex..toIndex-1]
```

- The array (range) must be **sorted**.
- It returns the index of `key` if found. Otherwise it returns `-(insertionPoint) − 1`, which is always **negative**. So `idx >= 0` means found.
- With duplicates, it returns **any** matching index, not necessarily the first.
- **O(log n)**: it halves the search range each step.

---

## 4. Approach 1: HashMap (as in Two Sum), O(n) space

```java
static int[] twoSumMap(int[] numbers, int target) {
    Map<Integer, Integer> indexOf = new HashMap<>();
    for (int i = 0; i < numbers.length; i++) {
        Integer j = indexOf.get(target - numbers[i]);
        if (j != null) return new int[]{j + 1, i + 1};
        indexOf.put(numbers[i], i);
    }
    return new int[0];
}
```

It's correct, and O(n) time, but **O(n) space**, which breaks the requirement. It also ignores the sorting. (`Integer j` can be `null`: this is a single lookup that replaces `containsKey` + `get`.)

---

## 5. Approach 2: Binary search for each partner, O(n log n)

```java
static int[] twoSumBinarySearch(int[] numbers, int target) {
    for (int i = 0; i < numbers.length; i++) {
        int need = target - numbers[i];
        int j = Arrays.binarySearch(numbers, i + 1, numbers.length, need);   // search only to the right of i
        if (j >= 0) return new int[]{i + 1, j + 1};
    }
    return new int[0];
}
```

| Line | What it does |
|---|---|
| `int need = target - numbers[i];` | the partner value |
| `binarySearch(numbers, i + 1, n, need)` | look only at indices after i (so the element isn't reused, and i < j) |
| `if (j >= 0)` | found |

**Dry run** `[2,7,11,15]`, target 9: i = 0, need 7 → found at index 1 → `[1, 2]` ✓
**O(n log n) time, O(1) space.** It meets the space limit, but two pointers is faster.

---

## 6. Approach 3: Two pointers (the file's solution)

### Code ([M03_TwoSumII_SortedInput.java:43-52](M03_TwoSumII_SortedInput.java#L43-L52))

```java
static int[] twoSum(int[] numbers, int target) {
    int left = 0, right = numbers.length - 1;
    while (left < right) {
        int sum = numbers[left] + numbers[right];
        if (sum == target) return new int[]{left + 1, right + 1};   // 1-indexed
        if (sum < target) left++;
        else right--;
    }
    return new int[0];
}
```

### Line by line

| Line | What it does |
|---|---|
| `int left = 0, right = numbers.length - 1;` | the smallest and largest elements |
| `while (left < right)` | two different elements remain. `<`, not `<=`, so we never use one element twice |
| `int sum = numbers[left] + numbers[right];` | the current pair |
| `if (sum == target) return {left + 1, right + 1};` | found; convert to 1-indexed |
| `if (sum < target) left++;` | too small: `numbers[left]` can't pair with anyone, so drop it |
| `else right--;` | too big: `numbers[right]` can't pair with anyone, so drop it |
| `return new int[0];` | unreachable (an answer is guaranteed) |

### Dry run 1: `[2, 7, 11, 15]`, target 9

| left (val) | right (val) | sum | vs 9 | move |
|---|---|---|---|---|
| 0 (2) | 3 (15) | 17 | > | right-- |
| 0 (2) | 2 (11) | 13 | > | right-- |
| 0 (2) | 1 (7) | **9** | = | **return [1, 2]** |

### Dry run 2: `[1, 2, 3, 4, 4, 9, 56, 90]`, target 8

| left (val) | right (val) | sum | move |
|---|---|---|---|
| 0 (1) | 7 (90) | 91 | right-- |
| 0 (1) | 6 (56) | 57 | right-- |
| 0 (1) | 5 (9) | 10 | right-- |
| 0 (1) | 4 (4) | 5 | left++ |
| 1 (2) | 4 (4) | 6 | left++ |
| 2 (3) | 4 (4) | 7 | left++ |
| 3 (4) | 4 (4) | **8** | **return [4, 5]** ✓ |

### Dry run 3: `[-1, 0]`, target −1
−1 + 0 = −1 → **return [1, 2]** right away ✓

### Complexity
- **Time O(n):** each step moves one pointer inward; at most n − 1 steps.
- **Space O(1).**

---

## 7. Comparing the approaches

| | HashMap | Binary search | Two pointers |
|---|---|---|---|
| Time | O(n) | O(n log n) | **O(n)** |
| Space | O(n) ✗ | **O(1)** | **O(1)** |
| Uses the sortedness | no | yes | yes |

---

## 8. Edge cases and common mistakes

**Edge cases:** negative numbers, duplicates forming the answer (`[4,4]`), the answer at both ends, n = 2.

**Mistakes:**
1. Returning 0-indexed positions.
2. `while (left <= right)` → can pair an element with itself when `2 × numbers[mid] == target`.
3. Moving the wrong pointer (e.g. `right--` when the sum is too small).
4. Overflow: `numbers[left] + numbers[right]` can overflow for values near ±2³¹ (not here, since values are ≤ 1000). In general use `long sum`.

---

## 9. Interview questions and answers

**Q1. Prove that two pointers can't miss the answer.**
> Invariant: the answer pair lies within [left, right]. If sum < target, `numbers[left]` paired even with the largest remaining element is too small, so it's in no valid pair, and dropping it keeps the invariant. The case sum > target is symmetric. Every step shrinks the range while keeping the answer inside, so we eventually hit it.

**Q2. Why is two pointers better than binary search here?**
> Both use O(1) space, but two pointers is O(n) versus O(n log n), because each comparison eliminates one element permanently instead of starting a new search.

**Q3. When would you still use the HashMap version?**
> When the input isn't sorted and sorting would lose the original indices (Two Sum I), or when the data arrives as a stream.

### Follow-up questions

**F1. 3Sum (LC 15).** → [M05 3Sum](M05_ThreeSum.md)
> Sort, fix `nums[i]`, run this exact two-pointer loop on the rest for `−nums[i]`, and skip duplicates. O(n²).

**F2. Count all pairs with sum == target (with duplicates).**
> When the sum matches: if `numbers[left] == numbers[right]`, every pair inside the range counts, `C(len, 2)`. Otherwise count the run of equal values on each side and multiply.

**F3. Count pairs with sum < target (LC 2824).**
> If `numbers[l] + numbers[r] < target`, then **every** index in (l, r] pairs with l: add `r − l`, then `l++`. Otherwise `r--`. O(n).

**F4. Closest pair sum to the target.**
> The same loop, tracking `min |sum − target|`; move the pointers by the same rule.

**F5. Two Sum on a BST (LC 653).**
> In-order traversal gives sorted order. Either collect the values and use two pointers, or run two iterators (one ascending, one descending) with stacks, for O(h) space.

---

## 10. Running the file

```bash
java M03_TwoSumII_SortedInput.java      # 4/4 tests passed
```

**One-line summary:** *"Sorted ⇒ start at both ends; too small → left++, too big → right--; each step safely discards one element."*
