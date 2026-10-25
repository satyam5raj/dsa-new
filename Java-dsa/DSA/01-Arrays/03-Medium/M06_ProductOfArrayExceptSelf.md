# M06 · Product of Array Except Self

| | |
|---|---|
| **LeetCode** | [#238 Product of Array Except Self](https://leetcode.com/problems/product-of-array-except-self/) |
| **Difficulty** | Medium |
| **Pattern** | Prefix × suffix products (no division) |
| **Code** | [M06_ProductOfArrayExceptSelf.java](M06_ProductOfArrayExceptSelf.java) |
| **Run** | `java M06_ProductOfArrayExceptSelf.java` |

---

## 1. The problem

Return `answer` where `answer[i]` = the **product of all elements except `nums[i]`**.

**Rules:** O(n) time and **no division**. Follow-up: O(1) extra space (the output array doesn't count).

| nums | answer |
|---|---|
| `[1, 2, 3, 4]` | `[24, 12, 8, 6]` |
| `[-1, 1, 0, -3, 3]` | `[0, 0, 9, 0, 0]` |
| `[0, 0, 2]` | `[0, 0, 0]` |
| `[2, 3]` | `[3, 2]` |

**Constraints:** `2 ≤ n ≤ 10⁵`, `-30 ≤ nums[i] ≤ 30`, and every product of any prefix or suffix fits in an `int` (guaranteed).

---

## 2. How to think about it

1. **Brute force:** for each i, multiply everything else together. O(n²).
2. **Division trick:** `total / nums[i]`. It's **forbidden**, and it also breaks on zeros (dividing by 0).
3. **Insight:** "everything except i" = **(everything to the LEFT of i) × (everything to the RIGHT of i)**:

```
nums   = [ 1,  2,  3,  4]
left   = [ 1,  1,  2,  6]    product of the elements BEFORE i (an empty product = 1)
right  = [24, 12,  4,  1]    product of the elements AFTER i
answer = [24, 12,  8,  6]    left[i] × right[i]
```

  Both `left` and `right` are **running products** (prefix and suffix), like the running sums in E03, but with multiplication.
4. **O(1) extra space:** store `left` directly in `answer`, then walk **backwards** with **one variable** `suffix` and multiply it in.

**An empty product is 1**, just as an empty sum is 0, because 1 is the identity for multiplication.

---

## 3. Java concepts used in this problem

### 3.1 Compound multiplication `*=`

```java
answer[i] *= suffix;   // answer[i] = answer[i] * suffix
suffix *= nums[i];     // suffix   = suffix * nums[i]
```

### 3.2 The order of two statements matters

```java
answer[i] *= suffix;     // 1. use the suffix of the elements AFTER i
suffix *= nums[i];       // 2. THEN include nums[i] for the next position (i−1)
```

Swapping them would include `nums[i]` in its own answer. The same "use, then update" order appeared in E04 (pivot index) and E01 (two sum: check, then insert).

### 3.3 Integer overflow, and why the problem guarantees fit

The product of up to 10⁵ numbers would normally overflow even `long`. LeetCode **guarantees** that every prefix/suffix product fits in a 32-bit int. In real code without that guarantee, you'd use `long`, `Math.multiplyExact` (which throws on overflow), or `BigInteger`.

---

## 4. Approach 1: Brute force

```java
static int[] productExceptSelfBrute(int[] nums) {
    int n = nums.length;
    int[] ans = new int[n];
    for (int i = 0; i < n; i++) {
        int p = 1;
        for (int j = 0; j < n; j++) if (j != i) p *= nums[j];
        ans[i] = p;
    }
    return ans;
}
```

**O(n²) time.** For n = 10⁵ that's 10¹⁰ multiplications, which times out.

---

## 5. Approach 2: Division (not allowed, but worth discussing)

```java
static int[] productExceptSelfDivision(int[] nums) {
    int zeros = 0, productNonZero = 1;
    for (int x : nums) { if (x == 0) zeros++; else productNonZero *= x; }
    int[] ans = new int[nums.length];
    for (int i = 0; i < nums.length; i++) {
        if (zeros > 1) ans[i] = 0;                                  // at least one other zero remains
        else if (zeros == 1) ans[i] = (nums[i] == 0) ? productNonZero : 0;
        else ans[i] = productNonZero / nums[i];
    }
    return ans;
}
```

To use division safely you'd need this **zero bookkeeping**: with 2+ zeros everything is 0; with exactly one zero only that position is non-zero. It's O(n), but it breaks the rules. Mentioning it, together with the zero issue, shows depth.

---

## 6. Approach 3: Separate left and right arrays

```java
static int[] productExceptSelfTwoArrays(int[] nums) {
    int n = nums.length;
    int[] left = new int[n], right = new int[n], ans = new int[n];
    left[0] = 1;
    for (int i = 1; i < n; i++) left[i] = left[i - 1] * nums[i - 1];
    right[n - 1] = 1;
    for (int i = n - 2; i >= 0; i--) right[i] = right[i + 1] * nums[i + 1];
    for (int i = 0; i < n; i++) ans[i] = left[i] * right[i];
    return ans;
}
```

The clearest version: **O(n) time, O(n) extra space** (two helper arrays).

---

## 7. Approach 4: Prefix in the output + a suffix variable (the file's solution)

### Code ([M06_ProductOfArrayExceptSelf.java:43-58](M06_ProductOfArrayExceptSelf.java#L43-L58))

```java
static int[] productExceptSelf(int[] nums) {
    int n = nums.length;
    int[] answer = new int[n];

    answer[0] = 1;                                // nothing to the left of index 0
    for (int i = 1; i < n; i++) {
        answer[i] = answer[i - 1] * nums[i - 1];  // prefix product
    }

    int suffix = 1;                               // nothing to the right of the last index
    for (int i = n - 1; i >= 0; i--) {
        answer[i] *= suffix;                      // left part * right part
        suffix *= nums[i];                        // include nums[i] for the next (left) position
    }
    return answer;
}
```

### Line by line

| Line | What it does |
|---|---|
| `answer[0] = 1;` | the product of the zero elements to the left of index 0 |
| `answer[i] = answer[i - 1] * nums[i - 1];` | pass 1: `answer[i]` = the product of `nums[0..i−1]`. It builds on the previous prefix, so it's O(1) per step |
| `int suffix = 1;` | the product of the zero elements to the right of the last index |
| `for (int i = n - 1; i >= 0; i--)` | pass 2, right to left |
| `answer[i] *= suffix;` | the left product (already stored) × the right product (in `suffix`) = the answer |
| `suffix *= nums[i];` | now `suffix` covers `nums[i..n−1]`, which is "everything to the right of i−1" |

### Dry run 1: `[1, 2, 3, 4]`

**Pass 1 (prefix):**

| i | answer[i−1] | nums[i−1] | answer[i] |
|---|---|---|---|
| 0 | — | — | 1 |
| 1 | 1 | 1 | 1 |
| 2 | 1 | 2 | 2 |
| 3 | 2 | 3 | 6 |

`answer = [1, 1, 2, 6]`

**Pass 2 (suffix):**

| i | answer[i] before | suffix (product after i) | answer[i] after | suffix becomes |
|---|---|---|---|---|
| 3 | 6 | 1 | 6 | 1 × 4 = 4 |
| 2 | 2 | 4 | 8 | 4 × 3 = 12 |
| 1 | 1 | 12 | 12 | 12 × 2 = 24 |
| 0 | 1 | 24 | 24 | 24 × 1 = 24 |

**Result** `[24, 12, 8, 6]` ✓

### Dry run 2: `[-1, 1, 0, -3, 3]` (one zero)

**Pass 1:** `answer = [1, -1, -1, 0, 0]`
(1 → 1×(−1) = −1 → −1×1 = −1 → −1×0 = 0 → 0×(−3) = 0)

**Pass 2:**

| i | answer before | suffix | answer after | suffix becomes |
|---|---|---|---|---|
| 4 | 0 | 1 | 0 | 3 |
| 3 | 0 | 3 | 0 | −9 |
| 2 | −1 | −9 | **9** | 0 |
| 1 | −1 | 0 | 0 | 0 |
| 0 | 1 | 0 | 0 | 0 |

**Result** `[0, 0, 9, 0, 0]` ✓. Only the zero's own position is non-zero: (−1) × 1 × (−3) × 3 = 9. Zeros need **no special case**.

### Dry run 3: `[2, 3]`
Pass 1: `[1, 2]`. Pass 2: i=1: 2 × 1 = 2, suffix = 3; i=0: 1 × 3 = 3. **Result** `[3, 2]` ✓

### Complexity
- **Time O(n):** two passes.
- **Space O(1) extra:** only `suffix` (the output doesn't count).

---

## 8. Comparing the approaches

| | Brute | Division | Two arrays | Prefix-in-output + suffix |
|---|---|---|---|---|
| Time | O(n²) | O(n) | O(n) | **O(n)** |
| Extra space | O(1) | O(1) | O(n) | **O(1)** |
| Allowed? | yes (slow) | **no** | yes | **yes** |
| Zero handling | automatic | needs counting | automatic | automatic |

---

## 9. Edge cases and common mistakes

**Edge cases:** one zero, two or more zeros (all 0), negative numbers, n = 2, all ones.

**Mistakes:**
1. Multiplying `suffix *= nums[i]` **before** `answer[i] *= suffix` (includes nums[i] in its own product).
2. Prefix off-by-one: `answer[i] = answer[i−1] * nums[i]` (should be `nums[i−1]`).
3. Starting the prefix or suffix at 0 instead of 1.
4. Using division "because it's simpler": it's against the rules, and it breaks with zeros.

---

## 10. Interview questions and answers

**Q1. Why is division a bad idea even if it were allowed?**
> Zeros: dividing by zero crashes, and you'd need separate logic for one zero versus several. Division with integers can also lose information, e.g. with overflowed products.

**Q2. How do you get O(1) extra space?**
> Store the prefix products directly in the output array, then sweep right to left with one running suffix product, multiplying it into each slot.

**Q3. Where else does the prefix/suffix idea show up?**
> Trapping Rain Water (prefix max and suffix max), Candy (left and right passes), Best Time to Buy and Sell with two transactions (best profit to the left and to the right of each split), and Pivot Index (prefix sums).

### Follow-up questions

**F1. Answer many "product of range [l, r]" queries.**
> Prefix products work only without zeros (`P[r+1] / P[l]`, which uses division). Otherwise use a segment tree of products (O(log n) per query), or count the zeros in a range with a prefix count plus prefix products of the non-zero values.

**F2. Modular products (mod a prime).**
> The same prefix × suffix trick works under mod. Division would need modular inverses, and zeros mod p are still a problem, so prefix × suffix is the robust choice.

**F3. Maximum product subarray.** → [15-DP M04](../../15-Dynamic-Programming/03-Medium/M04_MaximumProductSubarray.md)
> Track the max and min products ending at each index (negatives flip them).

**F4. A 2D version: the product of all cells except row i or column j, etc.**
> Generalise with prefix/suffix products per row and per column.

---

## 11. Running the file

```bash
java M06_ProductOfArrayExceptSelf.java      # 4/4 tests passed
```

**One-line summary:** *"answer[i] = (product of everything left of i) × (product of everything right of i): prefix in the array, suffix in one variable."*
