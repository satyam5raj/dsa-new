# M07 · Subarray Sum Equals K

| | |
|---|---|
| **LeetCode** | [#560 Subarray Sum Equals K](https://leetcode.com/problems/subarray-sum-equals-k/) |
| **Difficulty** | Medium |
| **Pattern** | Prefix sum + HashMap (counts of earlier prefix sums) |
| **Code** | [M07_SubarraySumEqualsK.java](M07_SubarraySumEqualsK.java) |
| **Run** | `java M07_SubarraySumEqualsK.java` |

---

## 1. The problem

Count the **contiguous** subarrays whose sum equals `k`. Values can be **negative**.

| nums | k | answer | subarrays |
|---|---|---|---|
| `[1, 1, 1]` | 2 | 2 | `[1,1]` at 0..1 and 1..2 |
| `[1, 2, 3]` | 3 | 2 | `[1,2]`, `[3]` |
| `[1, -1, 0]` | 0 | 3 | `[1,-1]`, `[0]`, `[1,-1,0]` |
| `[3,4,7,2,-3,1,4,2]` | 7 | 4 | `[3,4]`, `[7]`, `[7,2,-3,1]`, `[1,4,2]` |

**Constraints:** `1 ≤ n ≤ 2 × 10⁴`, `-1000 ≤ nums[i] ≤ 1000`.

This is one of the most important array patterns. Master it.

---

## 2. How to think about it

1. **Brute force:** every start index, extend the end with a running sum: O(n²).
2. **Why not a sliding window?** A sliding window relies on "adding an element makes the sum bigger, removing one makes it smaller", so you know when to shrink. **Negative numbers break that**, so you can't decide when to move the left edge.
3. **Insight (prefix sums):** let `P(j)` = the sum of `nums[0..j]`. The sum of the subarray **after index i, up to index j** is:

```
sum(i+1 .. j) = P(j) − P(i)
```

   We want `P(j) − P(i) = k`  ⇔  **`P(i) = P(j) − k`**.

   So when we're at position j, the question becomes: **"How many earlier prefix sums equal `currentPrefix − k`?"** Each one marks the start of a valid subarray ending at j.
4. Keep a **HashMap: prefix sum → how many times it has appeared**. One pass, O(n).

### The `{0: 1}` seed
Before index 0 there's an **empty prefix** with sum 0. Without it, subarrays that **start at index 0** are missed. Example: `[1, 2]`, k = 3. At j = 1, prefix = 3, need 3 − 3 = **0**. Only the empty prefix can provide it.

### Look up **before** inserting the current prefix
If you inserted first, then with k = 0 the current prefix would match itself, counting an **empty** subarray.

---

## 3. Java concepts used in this problem

### 3.1 HashMap as a counter: `getOrDefault` and `merge`

```java
count += countOfPrefix.getOrDefault(prefix - k, 0);   // how many earlier prefixes equal prefix − k (0 if none)
countOfPrefix.merge(prefix, 1, Integer::sum);         // increment the count of this prefix
```

(Full explanation of `merge` in [E11](../02-Easy/E11_IntersectionOfTwoArrays.md).) Note that we store **counts**, not just "seen": the same prefix sum can appear several times (thanks to negatives and zeros), and **each** occurrence is a different starting point.

### 3.2 Why the maps in the dry runs below look sorted

The dry-run tables show the map in sorted key order for readability. The real code uses a `HashMap`, whose printing order is unspecified (a `TreeMap` would print sorted, but costs O(log n) per operation instead of O(1)).

### 3.3 Random stress testing

Like M04, `main` compares the fast solution with brute force on 300 random arrays with values in −5..5. That's a great habit for prefix/hash tricks, where an off-by-one in the seed or in the update order is easy to miss.

---

## 4. Approach 1: Brute force

### Code ([M07_SubarraySumEqualsK.java:62-72](M07_SubarraySumEqualsK.java#L62-L72))

```java
static int subarraySumBrute(int[] nums, int k) {
    int count = 0;
    for (int i = 0; i < nums.length; i++) {
        int sum = 0;
        for (int j = i; j < nums.length; j++) {
            sum += nums[j];
            if (sum == k) count++;
        }
    }
    return count;
}
```

| Line | What it does |
|---|---|
| outer `i` | the start of the subarray |
| `sum += nums[j]` | the running sum of `nums[i..j]` |
| `if (sum == k) count++;` | each matching end counts. **No early break**: with negatives, a later end can match again |

**Dry run** `[1, 1, 1]`, k = 2:

| i | j | sum | match? | count |
|---|---|---|---|---|
| 0 | 0 | 1 | | 0 |
| 0 | 1 | 2 | ✓ | 1 |
| 0 | 2 | 3 | | 1 |
| 1 | 1 | 1 | | 1 |
| 1 | 2 | 2 | ✓ | 2 |
| 2 | 2 | 1 | | 2 |

**Return 2.** **O(n²) time, O(1) space.** With n = 2 × 10⁴, that's 2 × 10⁸ operations: borderline.

---

## 5. Approach 2: Prefix sums + HashMap (optimal)

### Code ([M07_SubarraySumEqualsK.java:50-60](M07_SubarraySumEqualsK.java#L50-L60))

```java
static int subarraySum(int[] nums, int k) {
    Map<Integer, Integer> countOfPrefix = new HashMap<>();
    countOfPrefix.put(0, 1);                       // the empty prefix
    int prefix = 0, count = 0;
    for (int x : nums) {
        prefix += x;
        count += countOfPrefix.getOrDefault(prefix - k, 0);   // subarrays ending here
        countOfPrefix.merge(prefix, 1, Integer::sum);         // record AFTER looking up
    }
    return count;
}
```

### Line by line

| Line | What it does |
|---|---|
| `Map<Integer, Integer> countOfPrefix = new HashMap<>();` | prefix sum → how many times it has appeared so far |
| `countOfPrefix.put(0, 1);` | the empty prefix (before index 0) has sum 0 |
| `int prefix = 0, count = 0;` | the running prefix sum and the answer |
| `prefix += x;` | now `prefix` = the sum of everything up to and including x |
| `count += countOfPrefix.getOrDefault(prefix - k, 0);` | every earlier prefix equal to `prefix − k` starts a subarray (ending here) that sums to k |
| `countOfPrefix.merge(prefix, 1, Integer::sum);` | record this prefix for future positions, **after** the lookup |

### Dry run 1: `[1, 2, 3]`, k = 3

| x | prefix | need = prefix − k | found | count | map after |
|---|---|---|---|---|---|
| — | 0 | | | 0 | `{0:1}` |
| 1 | 1 | −2 | 0 | 0 | `{0:1, 1:1}` |
| 2 | 3 | **0** | **1** (the empty prefix → `[1,2]`) | 1 | `{0:1, 1:1, 3:1}` |
| 3 | 6 | **3** | **1** (the prefix after `[1,2]` → `[3]`) | 2 | `{0:1, 1:1, 3:1, 6:1}` |

**Return 2** ✓

### Dry run 2: `[1, -1, 0]`, k = 0 (negatives and zero; counts > 1)

| x | prefix | need | found | count | map after |
|---|---|---|---|---|---|
| — | 0 | | | 0 | `{0:1}` |
| 1 | 1 | 1 | 0 | 0 | `{0:1, 1:1}` |
| −1 | 0 | 0 | **1** (`[1,-1]`) | 1 | `{0:2, 1:1}` |
| 0 | 0 | 0 | **2** (`[1,-1,0]` and `[0]`) | 3 | `{0:3, 1:1}` |

**Return 3** ✓. The last step found **two** earlier prefixes with sum 0 (the empty prefix and the one after `[1,-1]`), so two subarrays end there. That's why we store **counts**.

### Dry run 3: `[3, 4, 7, 2, -3, 1, 4, 2]`, k = 7

| x | prefix | need | found | count | subarray(s) found |
|---|---|---|---|---|---|
| 3 | 3 | −4 | 0 | 0 | |
| 4 | 7 | 0 | 1 | 1 | `[3,4]` |
| 7 | 14 | 7 | 1 | 2 | `[7]` |
| 2 | 16 | 9 | 0 | 2 | |
| −3 | 13 | 6 | 0 | 2 | |
| 1 | 14 | 7 | 1 | 3 | `[7,2,-3,1]` |
| 4 | 18 | 11 | 0 | 3 | |
| 2 | 20 | 13 | 1 | 4 | `[1,4,2]` |

**Return 4** ✓ (the prefix 14 shows up twice, which is how the negative −3 creates a second chance).

### Complexity
- **Time O(n):** one pass, with O(1) average map operations.
- **Space O(n):** up to n + 1 distinct prefix sums.

---

## 6. Comparing the approaches

| | Brute force | Prefix + HashMap |
|---|---|---|
| Time | O(n²) | **O(n)** |
| Space | O(1) | O(n) |
| Works with negatives | yes | **yes** |
| Sliding window? | — | not applicable (negatives) |

**When CAN you use a sliding window instead?** When all numbers are **positive**: then the window sum is monotonic. That gives O(1) space. See [M11 Minimum Size Subarray Sum](M11_MinimumSizeSubarraySum.md).

---

## 7. Edge cases and common mistakes

**Edge cases:** k = 0, negative k, the subarray starting at index 0, many zeros (`[0,0,0]` with k=0 gives 6), a single element.

**Mistakes:**
1. Forgetting the `{0: 1}` seed, which misses subarrays starting at index 0.
2. Inserting the current prefix **before** the lookup (wrong for k = 0).
3. Using a `Set` instead of counts, which undercounts repeated prefix sums.
4. Trying a sliding window with negative numbers.
5. Overflow with big values/lengths: use `long` prefixes (and `Map<Long, Integer>`).

---

## 8. Interview questions and answers

**Q1. Explain the core idea in one sentence.**
> A subarray (i, j] sums to k exactly when `P(j) − P(i) = k`, so at each j I count how many earlier prefix sums equal `P(j) − k`, using a hash map of prefix counts.

**Q2. Why the initial `{0: 1}`?**
> It represents the empty prefix before the array. A subarray that starts at index 0 has sum `P(j) − 0`, so the lookup needs prefix 0 to exist once.

**Q3. Why doesn't a sliding window work?**
> With negatives, extending the window can decrease the sum and shrinking it can increase the sum, so there's no rule for when to move the left edge.

### Follow-up questions (the prefix + hashmap family)

**F1. The longest subarray with sum k (LC 325).**
> Store the **first index** at which each prefix appears (`putIfAbsent`). At j, if `P(j) − k` was seen at index i, the length is `j − i`. Seed with `{0: −1}`.

**F2. Continuous Subarray Sum (LC 523): a subarray of length ≥ 2 whose sum is a multiple of k.** → [06-Hash-Table M05](../../06-Hash-Table/03-Medium/M05_ContinuousSubarraySum.md)
> Store `prefix % k` → first index. Two equal remainders at distance ≥ 2 mean the sum between them is divisible by k.

**F3. Subarray Sums Divisible by K (LC 974): count them.**
> Count the remainders `((prefix % k) + k) % k` (normalised for negatives), and for each one add how many times that remainder appeared before.

**F4. Contiguous Array (LC 525): the longest subarray with equal 0s and 1s.** → [06-Hash-Table M07](../../06-Hash-Table/03-Medium/M07_ContiguousArray.md)
> Map 0 → −1, then it's "the longest subarray with sum 0": the first index of each prefix.

**F5. Count the submatrices with sum k (LC 1074).**
> Fix a pair of rows, collapse the columns into a 1D array of sums, and run this exact algorithm. O(rows² × cols).

**F6. All numbers positive: use O(1) space.**
> A sliding window: expand right, and shrink left while the sum > k; count when the sum == k.

**F7. Path Sum III (LC 437): count the tree paths (downward) that sum to k.**
> The same prefix-count map during DFS: add the current prefix when entering a node, and **remove** it when backtracking.

---

## 9. Running the file

```bash
java M07_SubarraySumEqualsK.java      # 5/5 tests passed (including 300 random cross-checks)
```

**One-line summary:** *"Count the earlier prefixes equal to (current prefix − k); seed {0:1}; look up before inserting."*
