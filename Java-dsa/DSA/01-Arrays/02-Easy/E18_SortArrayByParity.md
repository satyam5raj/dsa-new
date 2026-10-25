# E18 · Sort Array by Parity

| | |
|---|---|
| **LeetCode** | [#905 Sort Array By Parity](https://leetcode.com/problems/sort-array-by-parity/) |
| **Difficulty** | Easy |
| **Pattern** | Two pointers (partitioning from opposite ends) |
| **Code** | [E18_SortArrayByParity.java](E18_SortArrayByParity.java) |
| **Run** | `java E18_SortArrayByParity.java` |

---

## 1. The problem

Move all **even** numbers before all **odd** numbers. Any order inside each group is fine.

| nums | one valid answer |
|---|---|
| `[3, 1, 2, 4]` | `[4, 2, 1, 3]` (also OK: `[2, 4, 3, 1]`, `[4, 2, 3, 1]`, …) |
| `[0]` | `[0]` |
| `[1, 3, 5, 2]` | `[2, 3, 5, 1]` |

**Constraints:** `1 ≤ n ≤ 5000`, `0 ≤ nums[i] ≤ 5000`.

"Parity" just means even or odd.

---

## 2. How to think about it

This is **partitioning**: split the array into two zones by a yes/no property. It's the same step **quicksort** uses (elements < pivot | elements ≥ pivot).

**Two pointers from opposite ends:**
- `left` walks right **as long as** it sees evens (they're already in the correct zone).
- `right` walks left **as long as** it sees odds (also already in place).
- When `left` is on an **odd** and `right` on an **even**, both are in the wrong zone → **swap** them.
- Stop when the pointers meet.

```
 [ evens ✓ | unknown ... | odds ✓ ]
            left     right
```

Since the order inside each group doesn't matter, this is the fastest approach (each misplaced pair is fixed with one swap). If the order **did** matter, you'd use the slow/fast pattern from Move Zeroes (E06) or an output array.

---

## 3. Java concepts used in this problem

### 3.1 The modulo operator `%` and parity

```java
x % 2 == 0   // even
x % 2 == 1   // odd... but ONLY for non-negative x!
```

`%` gives the **remainder**. In Java, the remainder **keeps the sign of the left operand**:

```java
 7 % 2  ==  1
-7 % 2  == -1    // not 1!
```

So for negative numbers, `x % 2 == 1` is **false** even when x is odd. Safer checks:
- `x % 2 != 0` → odd, for any sign
- `(x & 1) == 1` → odd, for any sign. The lowest bit is 1 for odd numbers, even in two's complement.

This problem guarantees `nums[i] ≥ 0`, so the code's `% 2 == 1` is safe, but remember the trap.

### 3.2 Testing a property instead of an exact answer

Many valid outputs exist, so the test can't compare against one fixed array. The file's `evensFirst` helper checks the **rule** instead: once an odd number has appeared, no even number may follow.

```java
static boolean evensFirst(int[] a) {
    boolean seenOdd = false;
    for (int x : a) {
        if (x % 2 != 0) seenOdd = true;
        else if (seenOdd) return false;          // an even AFTER an odd -> wrong
    }
    return true;
}
```

A second test sorts the output and compares it with the sorted input, to make sure no element was lost or duplicated. Checking properties like this is a great habit for problems where "any valid answer" is accepted.

---

## 4. Approach 1: Output array (two passes)

```java
static int[] sortByParityExtra(int[] nums) {
    int[] res = new int[nums.length];
    int k = 0;
    for (int x : nums) if (x % 2 == 0) res[k++] = x;   // evens first
    for (int x : nums) if (x % 2 != 0) res[k++] = x;   // then odds
    return res;
}
```

- **Stable:** keeps the original relative order inside each group.
- **O(n) time, O(n) space.**

**Dry run** `[3,1,2,4]`: pass 1 → `[2,4,_,_]`; pass 2 → `[2,4,3,1]`.

---

## 5. Approach 2: Slow/fast write pointer (in place, evens keep their order)

The Move Zeroes pattern, with "non-zero" replaced by "even":

```java
static int[] sortByParitySlowFast(int[] nums) {
    int insert = 0;
    for (int i = 0; i < nums.length; i++) {
        if (nums[i] % 2 == 0) {
            int t = nums[insert]; nums[insert] = nums[i]; nums[i] = t;
            insert++;
        }
    }
    return nums;
}
```

The evens keep their relative order; the odds might not. **O(n), O(1).**

---

## 6. Approach 3: Two pointers from opposite ends (the file's solution)

### Code ([E18_SortArrayByParity.java:34-50](E18_SortArrayByParity.java#L34-L50))

```java
static int[] sortArrayByParity(int[] nums) {
    int left = 0, right = nums.length - 1;
    while (left < right) {
        if (nums[left] % 2 == 0) {
            left++;                              // even on the left: correct
        } else if (nums[right] % 2 == 1) {
            right--;                             // odd on the right: correct
        } else {
            int t = nums[left];                  // odd on the left, even on the right: swap
            nums[left] = nums[right];
            nums[right] = t;
            left++;
            right--;
        }
    }
    return nums;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int left = 0, right = nums.length - 1;` | one pointer at each end |
| `while (left < right)` | there's still an unknown middle region |
| `if (nums[left] % 2 == 0) left++;` | left already holds an even, which belongs on the left → move on |
| `else if (nums[right] % 2 == 1) right--;` | (we now know left is odd) right holds an odd, which belongs on the right → move on |
| `else { swap; left++; right--; }` | left is odd **and** right is even → both are misplaced, so one swap fixes both. Then move both inward |
| `return nums;` | partitioned in place |

Each loop iteration moves **at least one** pointer, so the loop ends after at most n iterations.

### Dry run 1: `[3, 1, 2, 4]`

| step | left (val) | right (val) | branch | array after |
|---|---|---|---|---|
| 1 | 0 (3, odd) | 3 (4, even) | swap | `[4, 1, 2, 3]`, left=1, right=2 |
| 2 | 1 (1, odd) | 2 (2, even) | swap | `[4, 2, 1, 3]`, left=2, right=1 |
| — | 2 | 1 | left < right fails → stop | `[4, 2, 1, 3]` ✓ |

### Dry run 2: `[1, 3, 5, 2]`

| step | left (val) | right (val) | branch | array after |
|---|---|---|---|---|
| 1 | 0 (1, odd) | 3 (2, even) | swap | `[2, 3, 5, 1]`, left=1, right=2 |
| 2 | 1 (3, odd) | 2 (5, odd) | right is odd → right-- | right=1 |
| — | 1 | 1 | stop | `[2, 3, 5, 1]` ✓ |

### Dry run 3: `[2, 4, 6]` (all even)
left moves 0 → 1 → 2, and stops when `left < right` fails (2 < 2 is false). No swaps. `[2, 4, 6]` ✓

### Complexity
- **Time O(n):** each element is looked at about once.
- **Space O(1).**
- **Swaps:** at most n/2. That's the fewest possible, because each swap fixes two misplaced elements.

---

## 7. Comparing the approaches

| | Output array | Slow/fast | Opposite ends |
|---|---|---|---|
| Time | O(n) | O(n) | **O(n)** |
| Extra space | O(n) | **O(1)** | **O(1)** |
| Stable (keeps order)? | **yes, both groups** | evens only | no |
| Writes | n | up to 2n | **fewest** |

---

## 8. Edge cases and common mistakes

**Edge cases:** all even, all odd, a single element, already partitioned, zeros (0 is even!).

**Mistakes:**
1. Using `x % 2 == 1` for odd when negatives are possible.
2. Forgetting to move the pointers after a swap, which loops forever on the same pair.
3. `while (left <= right)`: harmless here, but understand why `<` is enough (a single middle element is already in a valid zone).
4. Assuming the output must match the example exactly. Any valid partition is accepted.

---

## 9. Interview questions and answers

**Q1. How is this related to quicksort?**
> Quicksort's partition step splits an array around a pivot using the same opposite-ends two-pointer swap (Hoare partition). Here the "pivot test" is "is it even?".

**Q2. Which approach would you use if order must be preserved?**
> An output array with two passes (fully stable, O(n) space). In-place stable partitioning in O(n) time and O(1) space is hard; the practical in-place stable options are O(n log n).

**Q3. How do you check parity safely for negative numbers?**
> `x % 2 != 0` or `(x & 1) != 0`. Not `x % 2 == 1`, because Java's `%` returns −1 for negative odd numbers.

### Follow-up questions

**F1. Sort Array by Parity II (LC 922): evens at even indices, odds at odd indices.**
> Two pointers: `i` over even indices, `j` over odd indices. When `nums[i]` is odd, advance `j` until `nums[j]` is even, then swap. O(n), O(1).

**F2. Three groups (e.g. < pivot, == pivot, > pivot / Sort Colors LC 75).**
> The Dutch National Flag algorithm: `low`, `mid`, `high` pointers. See [ArrayPatterns](../01-Basics/ArrayPatterns.java).

**F3. Partition by any predicate (e.g. negatives first).**
> The same code with the test replaced. Java's `Collections` has no partition method, but `Collectors.partitioningBy` does this for streams, though not in place.

**F4. Odd numbers first instead.**
> Swap the two tests.

---

## 10. Running the file

```bash
java E18_SortArrayByParity.java      # 5/5 tests passed
```

**One-line summary:** *"Left skips evens, right skips odds; when both are stuck, swap: quicksort's partition."*
