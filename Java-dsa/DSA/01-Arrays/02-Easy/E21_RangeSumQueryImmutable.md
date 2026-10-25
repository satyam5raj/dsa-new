# E21 · Range Sum Query – Immutable

| | |
|---|---|
| **LeetCode** | [#303 Range Sum Query - Immutable](https://leetcode.com/problems/range-sum-query-immutable/) |
| **Difficulty** | Easy |
| **Pattern** | Prefix sum (precompute once, answer many queries in O(1)) |
| **Code** | [E21_RangeSumQueryImmutable.java](E21_RangeSumQueryImmutable.java) |
| **Run** | `java E21_RangeSumQueryImmutable.java` |

---

## 1. The problem

This is a **design** problem: write a class.
- `NumArray(int[] nums)`: the constructor receives the array (which never changes afterwards).
- `int sumRange(int left, int right)`: returns `nums[left] + … + nums[right]` (inclusive).
- `sumRange` is called **many** times (up to 10⁴).

```
nums = [-2, 0, 3, -5, 2, -1]
sumRange(0, 2) =  1      (-2 + 0 + 3)
sumRange(2, 5) = -1      (3 - 5 + 2 - 1)
sumRange(0, 5) = -3
sumRange(3, 3) = -5
```

**Constraints:** `1 ≤ n ≤ 10⁴`, at most 10⁴ calls.

---

## 2. How to think about it

- **Naive:** loop from `left` to `right` on every call. That's O(n) per query; 10⁴ queries × 10⁴ elements = 10⁸ operations.
- **Key idea: do the work once, up front.** The array never changes, so precompute something that makes every query O(1).
- **Prefix sums** (from E03): `prefix[i]` = the sum of the first `i` elements (`nums[0..i−1]`). Then:

```
sum(left..right) = (sum of the first right+1 elements) − (sum of the first left elements)
                 = prefix[right + 1] − prefix[left]
```

- **Why give `prefix` length n + 1 with `prefix[0] = 0`?** So that `left = 0` needs no special case: `prefix[0] = 0` means "the sum of zero elements".

```
index:        0    1    2    3    4    5    6
nums:            -2    0    3   -5    2   -1
prefix:       0   -2   -2    1   -4   -2   -3
```

`sumRange(2, 5) = prefix[6] − prefix[2] = −3 − (−2) = −1` ✓

This trade-off (spend O(n) time and memory once so that every query is fast) is called **precomputation**. It's the reason databases build indexes.

---

## 3. Java concepts used in this problem

### 3.1 Classes, objects, constructors

```java
static class NumArray {
    private final int[] prefix;          // a FIELD: data each object carries

    NumArray(int[] nums) { ... }         // the CONSTRUCTOR: runs once on `new NumArray(...)`

    int sumRange(int left, int right) { ... }   // an INSTANCE METHOD: uses this object's data
}

NumArray na = new NumArray(new int[]{-2, 0, 3, -5, 2, -1});   // create an object
na.sumRange(0, 2);                                              // call a method on it
```

| Term | Meaning |
|---|---|
| **class** | a blueprint: which data and methods objects of this type have |
| **object / instance** | one concrete thing built from the blueprint (`na`) |
| **field** | a variable stored inside each object (`prefix`) |
| **constructor** | a special method with the class's name and **no return type**; it sets up the fields |
| **instance method** | a method **without** `static`; it runs on a specific object and can read its fields |

Unlike the earlier problems' `static` methods, `sumRange` needs the object's `prefix`, so it isn't static.

### 3.2 `static class` (a static nested class)

The file is `E21_RangeSumQueryImmutable.java`, so `NumArray` is placed **inside** it. `static` here means the nested class doesn't need an instance of the outer class, so `main` can simply do `new NumArray(...)`. (Without `static`, it would be an **inner class**, tied to an outer object.) This course nests helper classes this way so that each file stays self-contained.

### 3.3 `private final`

- `private`: only code inside `NumArray` can touch `prefix`. That's **encapsulation**: callers can only use the public behaviour (`sumRange`).
- `final`: the field must be assigned **exactly once** (in the constructor) and can never be reassigned. Note: the **array contents** could still be changed; `final` only locks the reference. That fits "immutable": nobody can swap the prefix array out.

---

## 4. Approach 1: Naive (loop on each query)

```java
static class NumArrayNaive {
    private final int[] nums;
    NumArrayNaive(int[] nums) { this.nums = nums; }

    int sumRange(int left, int right) {
        int sum = 0;
        for (int i = left; i <= right; i++) sum += nums[i];
        return sum;
    }
}
```

- `this.nums = nums;` → `this.nums` is the **field**; the plain `nums` is the constructor's **parameter**. They share a name, so `this.` tells them apart.
- **Build O(1), query O(n), space O(1)** beyond the reference.

**Dry run** `sumRange(2, 5)`: 3 + (−5) + 2 + (−1) = −1.

---

## 5. Approach 2: Prefix sums (the file's solution)

### Code ([E21_RangeSumQueryImmutable.java:39-52](E21_RangeSumQueryImmutable.java#L39-L52))

```java
static class NumArray {
    private final int[] prefix;

    NumArray(int[] nums) {
        prefix = new int[nums.length + 1];
        for (int i = 0; i < nums.length; i++) {
            prefix[i + 1] = prefix[i] + nums[i];
        }
    }

    int sumRange(int left, int right) {
        return prefix[right + 1] - prefix[left];
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `private final int[] prefix;` | the only state we need. We don't even keep `nums` |
| `prefix = new int[nums.length + 1];` | one extra slot; `prefix[0]` is 0 by default |
| `prefix[i + 1] = prefix[i] + nums[i];` | the running sum, shifted by one: `prefix[i+1]` = the sum of `nums[0..i]` |
| `return prefix[right + 1] - prefix[left];` | (sum of `nums[0..right]`) − (sum of `nums[0..left−1]`) = the sum of `nums[left..right]` |

### Dry run: building the prefix for `[-2, 0, 3, -5, 2, -1]`

| i | nums[i] | prefix[i] | prefix[i+1] = prefix[i] + nums[i] |
|---|---|---|---|
| 0 | −2 | 0 | −2 |
| 1 | 0 | −2 | −2 |
| 2 | 3 | −2 | 1 |
| 3 | −5 | 1 | −4 |
| 4 | 2 | −4 | −2 |
| 5 | −1 | −2 | −3 |

`prefix = [0, -2, -2, 1, -4, -2, -3]`

### Dry run: the queries

| query | formula | value |
|---|---|---|
| sumRange(0, 2) | prefix[3] − prefix[0] = 1 − 0 | **1** ✓ |
| sumRange(2, 5) | prefix[6] − prefix[2] = −3 − (−2) | **−1** ✓ |
| sumRange(0, 5) | prefix[6] − prefix[0] = −3 − 0 | **−3** ✓ |
| sumRange(3, 3) | prefix[4] − prefix[3] = −4 − 1 | **−5** ✓ |

### Complexity
- **Constructor O(n)**, **each query O(1)**, **space O(n)**.
- q queries cost O(n + q) in total, versus O(n × q) for the naive version.

---

## 6. Comparing the approaches

| | Naive | Prefix sum |
|---|---|---|
| Build | O(1) | O(n) |
| Query | O(n) | **O(1)** |
| Space | O(1) | O(n) |
| 10⁴ queries on n = 10⁴ | about 10⁸ ops | **about 2 × 10⁴ ops** |

---

## 7. Edge cases and common mistakes

**Edge cases:** `left == right` (a single element), `left = 0`, `right = n − 1`, negative numbers, large sums (use `long` if the sums could exceed int).

**Mistakes:**
1. A prefix of length n with `prefix[i] = sum of nums[0..i]`, then forgetting the special case `left == 0` (`prefix[left − 1]` → index −1).
2. Off-by-one: `prefix[right] − prefix[left]` (wrong: it leaves out `nums[right]`).
3. Recomputing the prefix inside `sumRange`.
4. Integer overflow for big inputs; use `long[] prefix`.

---

## 8. Interview questions and answers

**Q1. Why the extra leading 0 in the prefix array?**
> So `prefix[i]` means "the sum of the first i elements". Then every range is `prefix[r+1] − prefix[l]`, including l = 0, with no special case.

**Q2. When is the naive approach better?**
> If there are only one or two queries, or memory is very tight. The prefix approach pays off once there are about as many queries as elements.

**Q3. What changes if the array can be updated? (LC 307)**
> One update would force an O(n) prefix rebuild. Use a **Fenwick tree** (BIT) or a **segment tree**: O(log n) update and O(log n) query. See [07-Trees M16 Range Sum Query Mutable](../../07-Trees/03-Medium/M16_RangeSumQueryMutable.md).

### Follow-up questions

**F1. 2D: the sum of a sub-rectangle (LC 304).**
> `P[i+1][j+1] = grid[i][j] + P[i][j+1] + P[i+1][j] − P[i][j]`. A query `(r1,c1)–(r2,c2)` = `P[r2+1][c2+1] − P[r1][c2+1] − P[r2+1][c1] + P[r1][c1]` (inclusion–exclusion). Build O(mn), query O(1).

**F2. Many range **increments**, then read the final array (difference array).**
> To add v to `[l, r]`: `diff[l] += v; diff[r+1] −= v`. Afterwards, the prefix sum of `diff` gives the final array. Each update is O(1). (Used in "Corporate Flight Bookings", "Car Pooling".)

**F3. Count subarrays with sum k.** → [M07 Subarray Sum Equals K](../03-Medium/M07_SubarraySumEqualsK.md)
> Prefix sums plus a HashMap of how often each prefix value has appeared.

**F4. Range min/max queries instead of sums.**
> Subtraction doesn't work for min/max. Use a **sparse table** (O(n log n) build, O(1) query, static data) or a segment tree (O(log n), supports updates).

**F5. Range XOR queries (LC 1310).**
> Prefix XOR: `px[r+1] ^ px[l]`, because XOR is its own inverse.

---

## 9. Running the file

```bash
java E21_RangeSumQueryImmutable.java      # 4/4 tests passed
```

**One-line summary:** *"Build prefix[i] = sum of the first i elements once; any range is prefix[r+1] − prefix[l]."*
