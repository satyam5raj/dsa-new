# M05 · Continuous Subarray Sum ⭐

| | |
|---|---|
| **LeetCode** | [#523 Continuous Subarray Sum](https://leetcode.com/problems/continuous-subarray-sum/) |
| **Difficulty** | Medium |
| **Pattern** | Prefix sum mod k + a HashMap of the **first** index |
| **Code** | [M05_ContinuousSubarraySum.java](M05_ContinuousSubarraySum.java) |
| **Run** | `java M05_ContinuousSubarraySum.java` |

⭐ = a bonus classic.

---

## 1. The problem

Is there a subarray of **length ≥ 2** whose sum is a **multiple of k** (0 counts as a multiple)?

| nums | k | answer | why |
|---|---|---|---|
| `[23, 2, 4, 6, 7]` | 6 | true | [2, 4] sums to 6 |
| `[23, 2, 6, 4, 7]` | 6 | true | [2, 6, 4] sums to 12 (the whole array, 42, also works) |
| `[23, 2, 6, 4, 7]` | 13 | false | |
| `[5, 0, 0, 0]` | 3 | true | [0, 0] sums to 0 |
| `[6]` | 6 | false | length 1 doesn't count |

**Constraints:** `1 ≤ n ≤ 10⁵`, `0 ≤ nums[i] ≤ 10⁹`, `1 ≤ k ≤ 2³¹ − 1`.

---

## 2. How to think about it

### Prefix sums, from Subarray Sum Equals K ([Arrays M07](../../01-Arrays/03-Medium/M07_SubarraySumEqualsK.md))
Let `P[j]` = nums[0] + … + nums[j], and `P[−1] = 0` (the empty prefix). Then

```
sum(i+1 .. j) = P[j] − P[i]
```

### The modulo twist
`P[j] − P[i]` is a multiple of k  ⇔  **`P[j] % k == P[i] % k`**.

So the question becomes: **have I seen this same remainder before, at least 2 positions back?** (A gap of 2 between the prefix indices i and j means the subarray i+1..j has length ≥ 2.)

### Why store the *first* index of each remainder?
We want the **longest** possible gap, to give the length ≥ 2 condition its best chance. If a remainder reappears right next to its last occurrence (gap 1), an earlier occurrence might still be far enough. So **never overwrite** an entry.

### The seed `{0: −1}`
The empty prefix has remainder 0 at "index −1". Without it, a valid subarray **starting at index 0** (like `[6, 6]` with k = 6) would be missed.

---

## 3. Java concepts used in this problem

### 3.1 Keeping only the remainder: `prefix = (prefix + nums[j]) % k`

Since only `P % k` matters, we can store the remainder instead of the full sum. That also **prevents overflow**: the full prefix sum could reach 10⁵ · 10⁹ = 10¹⁴, far beyond `int`. The remainder stays below k.

Is `prefix + nums[j]` itself safe in `int`? prefix < k ≤ 2³¹ − 1 and nums[j] ≤ 10⁹, so the sum can exceed 2³¹ − 1 when k is huge. The file's tests use small values. A fully safe version would use `long prefix`. Worth mentioning in an interview.

### 3.2 `Integer i = map.get(...)` and the null check

The same pattern as [E02](../02-Easy/E02_ContainsDuplicateII.md): `null` means "this remainder is new".

### 3.3 `long` in the brute force

```java
long s = a[i];
```

The brute force accumulates the real sum, which can exceed `int`, so it uses a `long`.

---

## 4. Approach 1: Brute force

### Code ([M05_ContinuousSubarraySum.java:59-68](M05_ContinuousSubarraySum.java#L59-L68))

```java
static boolean brute(int[] a, int k) {
    for (int i = 0; i < a.length; i++) {
        long s = a[i];
        for (int j = i + 1; j < a.length; j++) {
            s += a[j];
            if (s % k == 0) return true;
        }
    }
    return false;
}
```

| Line | What it does |
|---|---|
| `long s = a[i];` | start a subarray at i |
| `for (int j = i + 1; …)` | extend it; j starts at i + 1, so every checked subarray has length ≥ 2 |
| `if (s % k == 0)` | a multiple of k |

**O(n²).** It's the file's test oracle.

---

## 5. Approach 2: Prefix remainders + first-index map (the file's solution)

### Code ([M05_ContinuousSubarraySum.java:43-57](M05_ContinuousSubarraySum.java#L43-L57))

```java
static boolean checkSubarraySum(int[] nums, int k) {
    Map<Integer, Integer> firstIndex = new HashMap<>();
    firstIndex.put(0, -1);
    int prefix = 0;
    for (int j = 0; j < nums.length; j++) {
        prefix = (prefix + nums[j]) % k;                 // keep it small: only the remainder matters
        Integer i = firstIndex.get(prefix);
        if (i != null) {
            if (j - i >= 2) return true;
        } else {
            firstIndex.put(prefix, j);                   // only the FIRST occurrence is stored
        }
    }
    return false;
}
```

### Line by line

| Line | What it does |
|---|---|
| `firstIndex.put(0, -1);` | the empty prefix has remainder 0 at index −1 |
| `prefix = (prefix + nums[j]) % k;` | the remainder of P[j] |
| `Integer i = firstIndex.get(prefix);` | the earliest index with the same remainder |
| `if (j - i >= 2) return true;` | the subarray i+1..j has length j − i ≥ 2 and a sum that's a multiple of k |
| `else firstIndex.put(prefix, j);` | a new remainder: remember where it first appeared |

Note: when the remainder **was** seen but the gap is only 1, we do **not** update the map. The older index stays, because it's better.

### Dry run 1: `[23, 2, 4, 6, 7]`, k = 6

| j | nums[j] | remainder | seen at | gap | map after |
|---|---|---|---|---|---|
| | | | | | {0: −1} |
| 0 | 23 | 5 | — | | {0: −1, 5: 0} |
| 1 | 2 | (5 + 2) % 6 = 1 | — | | {0: −1, 5: 0, 1: 1} |
| 2 | 4 | (1 + 4) % 6 = 5 | 0 | **2** → true | |

The subarray is indices 1..2 = [2, 4] ✓

### Dry run 2: `[5, 0, 0, 0]`, k = 3 (why "first index" matters)

| j | remainder | seen at | gap | action |
|---|---|---|---|---|
| 0 | 2 | — | | store {2: 0} |
| 1 | 2 | 0 | 1 | too short; **keep** index 0 |
| 2 | 2 | 0 | **2** | true ([0, 0]) |

If j = 1 had overwritten the entry with index 1, j = 2 would see a gap of 1 and would have to wait until j = 3.

### Dry run 3: `[23, 2, 6, 4, 7]`, k = 13

| j | remainder | |
|---|---|---|
| 0 | 10 | new |
| 1 | 12 | new |
| 2 | 5 | new |
| 3 | 9 | new |
| 4 | 3 | new |

No remainder repeats (and none is 0 at j ≥ 1) → **false** ✓

### Complexity
- **Time O(n).**
- **Space O(min(n, k)):** at most k distinct remainders.

---

## 6. Comparing the approaches

| | Brute force | Prefix remainders |
|---|---|---|
| Time | O(n²) | **O(n)** |
| Space | O(1) | O(min(n, k)) |

---

## 7. Edge cases and common mistakes

**Edge cases:** a length-1 array, zeros (`[0, 0]` → true for any k), a subarray starting at index 0, k = 1 (any length-2 subarray works).

**Mistakes:**
1. Forgetting the seed `{0: −1}`.
2. Overwriting the stored index (losing the longer gap).
3. Checking `j − i >= 1` (which allows length-1 subarrays).
4. Storing the full prefix sum (overflow, and unbounded keys).

---

## 8. Interview questions and answers

**Q1. Why do equal remainders mean a multiple of k?**
> If P[j] = q₁k + r and P[i] = q₂k + r, then P[j] − P[i] = (q₁ − q₂)k, a multiple of k.

**Q2. Why the first index, not the last?**
> The only extra constraint is the length (≥ 2). The earliest index gives the longest subarray for that remainder, so it satisfies the length constraint whenever any index would.

### Follow-up questions

**F1. Count the subarrays with sum divisible by k (LC 974).**
> Store **counts** of the remainders instead of the first index, and add `count[r]` at each step. With negative numbers, normalise using `Math.floorMod`.

**F2. The longest subarray with sum divisible by k.**
> The same first-index map; take `max(j − i)` instead of returning early.

**F3. Contiguous Array (equal 0s and 1s).** → [M07](M07_ContiguousArray.md)
> The same "first index of each prefix value" map, with the sum-0 condition.

---

## 9. Running the file

```bash
java M05_ContinuousSubarraySum.java      # 6/6 tests passed (including 1,000 random cases vs brute force)
```

**One-line summary:** *"Equal prefix remainders mean a multiple of k in between; store each remainder's first index (seeded with 0 → −1) and look for a gap ≥ 2."*
