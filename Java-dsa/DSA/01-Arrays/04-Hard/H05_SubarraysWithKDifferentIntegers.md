# H05 · Subarrays with K Different Integers

| | |
|---|---|
| **LeetCode** | [#992 Subarrays with K Different Integers](https://leetcode.com/problems/subarrays-with-k-different-integers/) |
| **Difficulty** | Hard |
| **Pattern** | "Exactly K" = "at most K" − "at most K−1" (sliding-window counting) |
| **Code** | [H05_SubarraysWithKDifferentIntegers.java](H05_SubarraysWithKDifferentIntegers.java) |
| **Run** | `java H05_SubarraysWithKDifferentIntegers.java` |

---

## 1. The problem

Count the subarrays that contain **exactly k distinct** integers.

| nums | k | answer | subarrays |
|---|---|---|---|
| `[1, 2, 1, 2, 3]` | 2 | 7 | `[1,2]`, `[2,1]`, `[1,2]`, `[2,3]`, `[1,2,1]`, `[2,1,2]`, `[1,2,1,2]` |
| `[1, 2, 1, 3, 4]` | 3 | 3 | `[1,2,1,3]`, `[2,1,3]`, `[1,3,4]` |
| `[1, 1, 1]` | 1 | 6 | every subarray |
| `[1, 2]` | 3 | 0 | impossible |

**Constraints:** `1 ≤ n ≤ 2 × 10⁴`, `1 ≤ nums[i], k ≤ n`.

---

## 2. How to think about it

### Why a direct sliding window fails
For "**at most** k distinct", a window has a clean rule: shrink while there are more than k distinct values, and then **every** start from `left` to `right` works for this end (the counting trick from [M14](../03-Medium/M14_SubarrayProductLessThanK.md)).

For "**exactly** k", for a fixed right end, the valid starts form a **range in the middle**:
- start too far left → more than k distinct
- start too far right → fewer than k distinct

A single window `[left, right]` can't represent that middle range.

### The trick: exactly = at most minus at most

```
exactly(k) = atMost(k) − atMost(k − 1)
```

The subarrays with ≤ k distinct values, minus those with ≤ k − 1, leave exactly the ones with exactly k. And `atMost` is easy: it's [M17](../03-Medium/M17_FruitIntoBaskets.md)'s window combined with M14's counting.

This **"exactly = atMost(K) − atMost(K−1)"** trick shows up in many counting problems: binary subarrays with sum S, "nice" subarrays with exactly k odd numbers, and so on.

---

## 3. Java concepts used in this problem

### 3.1 Splitting the problem into a reusable helper

```java
static int subarraysWithKDistinct(int[] nums, int k) {
    return atMost(nums, k) - atMost(nums, k - 1);
}
```

The main method becomes **one line** that states the math. All the logic lives in `atMost`, which is called twice. With `k − 1 = 0`, `atMost(nums, 0)` correctly returns 0 (every window shrinks to empty).

### 3.2 A HashMap of counts with removal at zero

See [M17](../03-Medium/M17_FruitIntoBaskets.md): `merge(v, -1, Integer::sum)` returns the new count, and the key is removed when it reaches 0, so `size()` = the number of distinct values in the window. Since the values are in 1..n, an `int[n + 1]` count array plus a `distinct` counter would be faster.

### 3.3 Random stress testing

`main` compares against `exactlyBrute` on 300 random arrays with values in 1..4. Small value ranges make "exactly k" cases common, which makes the test effective.

---

## 4. Approach 1: Brute force

### Code ([H05_SubarraysWithKDifferentIntegers.java:66-76](H05_SubarraysWithKDifferentIntegers.java#L66-L76))

```java
static int exactlyBrute(int[] a, int k) {
    int c = 0;
    for (int i = 0; i < a.length; i++) {
        Set<Integer> s = new HashSet<>();
        for (int j = i; j < a.length; j++) {
            s.add(a[j]);
            if (s.size() == k) c++;
        }
    }
    return c;
}
```

For each start, grow the end and track the distinct values in a set; count when it's exactly k. (Optimisation: `break` once `s.size() > k`.) **O(n²)**.

---

## 5. Approach 2: atMost(k) − atMost(k−1) (the file's solution)

### Code ([H05_SubarraysWithKDifferentIntegers.java:47-64](H05_SubarraysWithKDifferentIntegers.java#L47-L64))

```java
static int subarraysWithKDistinct(int[] nums, int k) {
    return atMost(nums, k) - atMost(nums, k - 1);
}

// number of subarrays with AT MOST k distinct values
static int atMost(int[] nums, int k) {
    Map<Integer, Integer> count = new HashMap<>();
    int left = 0, total = 0;
    for (int right = 0; right < nums.length; right++) {
        count.merge(nums[right], 1, Integer::sum);
        while (count.size() > k) {
            int v = nums[left++];
            if (count.merge(v, -1, Integer::sum) == 0) count.remove(v);
        }
        total += right - left + 1;                     // all subarrays ending at 'right'
    }
    return total;
}
```

### Line by line (`atMost`)

| Line | What it does |
|---|---|
| `count.merge(nums[right], 1, ...)` | **expand**: add the new element |
| `while (count.size() > k) {...}` | **shrink** until at most k distinct values remain |
| `if (count.merge(v, -1, ...) == 0) count.remove(v);` | a value that's completely gone from the window is removed |
| `total += right - left + 1;` | every subarray `[s..right]` with `left ≤ s ≤ right` has ≤ k distinct values (it's a sub-window of a valid window) |

### Dry run: `[1, 2, 1, 2, 3]`, k = 2

**atMost(2):**

| right | value | window after shrinking | left | + (right − left + 1) | total |
|---|---|---|---|---|---|
| 0 | 1 | [1] | 0 | 1 | 1 |
| 1 | 2 | [1, 2] | 0 | 2 | 3 |
| 2 | 1 | [1, 2, 1] | 0 | 3 | 6 |
| 3 | 2 | [1, 2, 1, 2] | 0 | 4 | 10 |
| 4 | 3 | 3 distinct → drop 1, 2, 1 → [2, 3] | 3 | 2 | **12** |

**atMost(1):**

| right | value | window | left | + | total |
|---|---|---|---|---|---|
| 0 | 1 | [1] | 0 | 1 | 1 |
| 1 | 2 | drop 1 → [2] | 1 | 1 | 2 |
| 2 | 1 | drop 2 → [1] | 2 | 1 | 3 |
| 3 | 2 | drop 1 → [2] | 3 | 1 | 4 |
| 4 | 3 | drop 2 → [3] | 4 | 1 | **5** |

**exactly(2) = 12 − 5 = 7** ✓

### Why the subtraction is correct, using the end position r = 3
- atMost(2) counted 4 subarrays ending at 3: `[1,2,1,2]`, `[2,1,2]`, `[1,2]`, `[2]`
- atMost(1) counted 1: `[2]`
- The difference, 3, is exactly the subarrays ending at 3 with exactly 2 distinct values: `[1,2,1,2]`, `[2,1,2]`, `[1,2]` ✓

### Dry run 2: `[1, 1, 1]`, k = 1
atMost(1) = 1 + 2 + 3 = 6; atMost(0) = 0 (every window shrinks to empty, adding 0 each time). **6 − 0 = 6** ✓

### Complexity
- **Time O(n):** two linear passes.
- **Space O(k):** the count map holds at most k + 1 keys.

---

## 6. (Bonus) Approach 3: One pass with two left pointers

Keep **two** windows ending at `right`: `l1` = the leftmost start with ≤ k distinct values, `l2` = the leftmost start with ≤ k − 1 distinct values. The starts in `[l1, l2)` have **exactly** k distinct values:

```java
static int exactlyOnePass(int[] nums, int k) {
    Map<Integer, Integer> big = new HashMap<>(), small = new HashMap<>();
    int l1 = 0, l2 = 0, res = 0;
    for (int r = 0; r < nums.length; r++) {
        big.merge(nums[r], 1, Integer::sum);
        small.merge(nums[r], 1, Integer::sum);
        while (big.size() > k) { int v = nums[l1++]; if (big.merge(v, -1, Integer::sum) == 0) big.remove(v); }
        while (small.size() > k - 1) { int v = nums[l2++]; if (small.merge(v, -1, Integer::sum) == 0) small.remove(v); }
        res += l2 - l1;                // the starts that give exactly k distinct
    }
    return res;
}
```

It's the same O(n) and does the same work as the two-call version, but makes the "range of valid starts" idea visible. (Verified on the same examples: 7, 3, 6, 0.)

---

## 7. Comparing the approaches

| | Brute | atMost difference | Two left pointers |
|---|---|---|---|
| Time | O(n²) | **O(n)** | O(n) |
| Space | O(n) | O(k) | O(k) |
| Simplicity | ✓ | **✓✓** | ✓ |

---

## 8. Edge cases and common mistakes

**Edge cases:** k larger than the number of distinct values (0), k = 1 with runs of equal values, all elements equal, a single element.

**Mistakes:**
1. Trying a single "exactly k" window with `count.size() == k`: it undercounts, because many valid starts are skipped.
2. Forgetting to remove keys whose count reaches 0.
3. Adding `count.size()` or 1 instead of `right − left + 1`.
4. Not handling `atMost(k − 1)` with k − 1 = 0 (it works naturally here, but check your loop guards).

---

## 9. Interview questions and answers

**Q1. Why does "exactly K" not work directly with a sliding window?**
> For a fixed right end, the valid starts form a middle range: starting too early adds distinct values, starting too late removes them. A single window boundary can only describe a prefix of starts.

**Q2. Prove `exactly(k) = atMost(k) − atMost(k−1)`.**
> Each subarray has some number of distinct values d. It's counted in atMost(k) iff d ≤ k, and in atMost(k−1) iff d ≤ k−1. So it appears in the difference iff d = k.

**Q3. Why does `atMost` add `right − left + 1`?**
> After shrinking, `[left..right]` has ≤ k distinct values, so every sub-window ending at `right` does too. Those are exactly `right − left + 1` subarrays, each counted once by its end.

### Follow-up questions

**F1. Binary Subarrays With Sum (LC 930): count the subarrays with sum exactly S in a 0/1 array.**
> `atMost(S) − atMost(S − 1)` with a sum-based window, or prefix sums + a HashMap ([M07](../03-Medium/M07_SubarraySumEqualsK.md)).

**F2. Count Number of Nice Subarrays (LC 1248): exactly k odd numbers.**
> Map each number to 1 if it's odd and 0 if it's even, then it's F1.

**F3. Count the subarrays with at least k distinct values.**
> `total − atMost(k − 1)`, where `total = n(n+1)/2`.

**F4. The longest subarray with exactly k distinct values.**
> Run the "longest with at most k" window, but only record the length when `size() == k`.

**F5. Count the substrings containing all three characters a, b, c (LC 1358).**
> An "at least" window: for each right end, once the window contains all three, every start ≤ left works. Or use total − atMost(2).

---

## 10. Running the file

```bash
java H05_SubarraysWithKDifferentIntegers.java      # 5/5 tests passed (including 300 random cross-checks)
```

**One-line summary:** *"Exactly K = atMost(K) − atMost(K−1); atMost counts right − left + 1 per right end."*
