# E04 · Find Pivot Index

| | |
|---|---|
| **LeetCode** | [#724 Find Pivot Index](https://leetcode.com/problems/find-pivot-index/) (same as #1991 Find the Middle Index) |
| **Difficulty** | Easy |
| **Pattern** | Prefix sum (left sum vs right sum) |
| **Code** | [E04_FindPivotIndex.java](E04_FindPivotIndex.java) |
| **Run** | `java E04_FindPivotIndex.java` |

---

## 1. The problem

The **pivot index** is an index `i` where:

```
sum of everything STRICTLY LEFT of i  ==  sum of everything STRICTLY RIGHT of i
```

`nums[i]` itself belongs to neither side. Return the **leftmost** pivot, or `-1` if none exists.

| nums | answer | why |
|---|---|---|
| `[1, 7, 3, 6, 5, 6]` | 3 | left = 1+7+3 = 11, right = 5+6 = 11 |
| `[1, 2, 3]` | -1 | no index balances |
| `[2, 1, -1]` | 0 | left of index 0 is empty (sum 0); right = 1 + (−1) = 0 |
| `[-1, -1, 0, 1, 1, 0]` | 5 | left = −1−1+0+1+1 = 0, right is empty (0) |
| `[0]` | 0 | both sides empty, 0 == 0 |

**Constraints:** `1 ≤ n ≤ 10⁴`, `-1000 ≤ nums[i] ≤ 1000`.

---

## 2. How to think about it

- **Brute force:** for each `i`, add up the left part and the right part separately. O(n) per index → O(n²).
- **Bottleneck:** recomputing both sides from scratch each time.
- **Insight:** if you know the **total** and the **left sum**, you get the right sum for free:

```
rightSum = total − leftSum − nums[i]
```

  And `leftSum` only grows by one element as `i` moves right (a running prefix sum, like E03).

So: pass 1 computes `total`; pass 2 walks with `leftSum`, works out `rightSum` and compares.

---

## 3. Java concepts used in this problem

### 3.1 The for-each loop for summing

```java
int total = 0;
for (int x : nums) total += x;
```

A loop with a single-statement body can skip the `{ }`. It's fine for one line, but braces are safer when you add more lines later.

### 3.2 Returning `-1` as "not found"

Methods that return an **index** often use `-1` to mean "not found", because `-1` can never be a valid index. The Java library does the same: `String.indexOf("z")` returns `-1` if `"z"` isn't present, and so does `List.indexOf(x)`.

### 3.3 `return` inside a loop = early exit

`if (leftSum == rightSum) return i;` leaves the **whole method** immediately. Because we scan from left to right and return the first match, we automatically get the **leftmost** pivot.

---

## 4. Approach 1: Brute force

```java
static int pivotIndexBrute(int[] nums) {
    for (int i = 0; i < nums.length; i++) {
        int left = 0, right = 0;
        for (int j = 0; j < i; j++) left += nums[j];                 // strictly left
        for (int j = i + 1; j < nums.length; j++) right += nums[j];  // strictly right
        if (left == right) return i;
    }
    return -1;
}
```

| Line | What it does |
|---|---|
| `int left = 0, right = 0;` | declares two variables in one statement, both reset for each `i` |
| `for (j = 0; j < i; ...)` | indices **before** i (`<`, not `<=`, so nums[i] is left out) |
| `for (j = i + 1; ...)` | indices **after** i |
| `if (left == right) return i;` | first balance point = leftmost pivot |

**Dry run** `[1, 2, 3]`:

| i | left | right | equal? |
|---|---|---|---|
| 0 | 0 | 2+3 = 5 | no |
| 1 | 1 | 3 | no |
| 2 | 1+2 = 3 | 0 | no |
| | | | **return −1** |

**Time O(n²)** (every `i` scans the whole array), **Space O(1)**.

---

## 5. Approach 2: Total + running left sum (optimal)

### Code ([E04_FindPivotIndex.java:41-52](E04_FindPivotIndex.java#L41-L52))

```java
static int pivotIndex(int[] nums) {
    int total = 0;
    for (int x : nums) total += x;

    int leftSum = 0;
    for (int i = 0; i < nums.length; i++) {
        int rightSum = total - leftSum - nums[i];
        if (leftSum == rightSum) return i;     // compare FIRST...
        leftSum += nums[i];                    // ...then move nums[i] to the left side
    }
    return -1;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int total = 0; for (int x : nums) total += x;` | **pass 1**: sum of everything |
| `int leftSum = 0;` | before index 0 there's nothing on the left |
| `for (int i = 0; ...)` | **pass 2**: try each index as the pivot |
| `int rightSum = total - leftSum - nums[i];` | everything minus the left part minus the pivot itself = the right part |
| `if (leftSum == rightSum) return i;` | balanced → this is the leftmost pivot |
| `leftSum += nums[i];` | before moving on, `nums[i]` joins the left side for the **next** index |
| `return -1;` | no index balanced |

**Why compare *before* updating `leftSum`?** At index `i`, the left side must hold only `nums[0..i-1]`. If we added `nums[i]` first, the pivot element would be counted on the left, which is wrong. This is the most common bug in this problem.

### Dry run 1: `[1, 7, 3, 6, 5, 6]`, total = 28

| i | nums[i] | leftSum | rightSum = 28 − left − nums[i] | equal? | leftSum after |
|---|---|---|---|---|---|
| 0 | 1 | 0 | 27 | no | 1 |
| 1 | 7 | 1 | 20 | no | 8 |
| 2 | 3 | 8 | 17 | no | 11 |
| 3 | 6 | 11 | **11** | **yes → return 3** | |

### Dry run 2: `[2, 1, -1]`, total = 2 (pivot at index 0)

| i | nums[i] | leftSum | rightSum = 2 − 0 − 2 | equal? |
|---|---|---|---|---|
| 0 | 2 | 0 | 0 | **yes → return 0** |

### Dry run 3: `[-1, -1, 0, 1, 1, 0]`, total = 0 (pivot at the last index)

| i | nums[i] | leftSum | rightSum = 0 − left − nums[i] | equal? | leftSum after |
|---|---|---|---|---|---|
| 0 | −1 | 0 | 1 | no | −1 |
| 1 | −1 | −1 | 2 | no | −2 |
| 2 | 0 | −2 | 2 | no | −2 |
| 3 | 1 | −2 | 1 | no | −1 |
| 4 | 1 | −1 | 0 | no | 0 |
| 5 | 0 | 0 | 0 | **yes → return 5** | |

### Dry run 4: `[1, 2, 3]`, total = 6

| i | leftSum | rightSum | equal? | leftSum after |
|---|---|---|---|---|
| 0 | 0 | 5 | no | 1 |
| 1 | 1 | 3 | no | 3 |
| 2 | 3 | 0 | no | 6 |
| | | | **return −1** | |

### Complexity
- **Time O(n):** two passes, so 2n steps, which is still O(n).
- **Space O(1):** just `total`, `leftSum` and `rightSum`.

### A neat equivalent check
`leftSum == total − leftSum − nums[i]` ⇔ `2 × leftSum + nums[i] == total`. It's the same thing with one fewer variable.

---

## 6. Comparing the approaches

| | Brute force | Total + running left |
|---|---|---|
| Time | O(n²) | **O(n)** |
| Space | O(1) | O(1) |

---

## 7. Edge cases and common mistakes

**Edge cases:** pivot at index 0, pivot at the last index, single element (always pivot 0), negative numbers, several pivots (return the leftmost), all zeros (`[0,0,0]` → 0).

**Mistakes:**
1. Updating `leftSum` before the comparison.
2. Including `nums[i]` on one of the sides.
3. Assuming all values are positive and trying to "stop early" or use two pointers. Negative numbers break any "the sum only grows" reasoning.

---

## 8. Interview questions and answers

**Q1. Explain your approach in one sentence.**
> Compute the total once, then walk left to right keeping the left sum; the right sum is `total − left − nums[i]`, and the first index where they're equal is the answer.

**Q2. Why doesn't a two-pointer approach (moving from both ends) work?**
> Two pointers rely on monotonic behaviour: moving a pointer must always push the sum in one direction. Negative numbers break that, because adding an element can make a side smaller.

**Q3. Is there any problem with overflow?**
> The worst total is 10⁴ × 1000 = 10⁷, which easily fits in an `int`. With bigger constraints I would use `long`.

### Follow-up questions

**F1. Return all pivot indices.**
> Don't return at the first match; add each matching `i` to a `List<Integer>` and return the list at the end.

**F2. Find the index that splits the array into two parts with equal sums, where the element belongs to the left part.** (variation)
> Compare `leftSum + nums[i]` with `total − leftSum − nums[i]`, i.e. update first, then compare.

**F3. Minimise the absolute difference between the left and right sums** (LC 2574 / 2256 style).
> Same scan. At each `i` compute `|left − right|` and keep the minimum.

**F4. Split the array into three parts with equal sums** (LC 1013).
> The total must be divisible by 3. Walk with a running sum and count how many times it reaches total/3 (then reset it). You need at least 3 such parts.

**F5. Could we do it in one pass?**
> Not without knowing the total. You can't know the right side's sum without having seen everything. So two passes (or one pass that builds prefix sums and then a scan of them) is the minimum. It's still O(n).

---

## 9. Running the file

```bash
java E04_FindPivotIndex.java      # 5/5 tests passed
```

**One-line summary:** *"right = total − left − nums[i]; compare, then move nums[i] to the left."*
