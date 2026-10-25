# M12 · Partition to K Equal Sum Subsets

| | |
|---|---|
| **LeetCode** | [#698 Partition to K Equal Sum Subsets](https://leetcode.com/problems/partition-to-k-equal-sum-subsets/) |
| **Difficulty** | Medium |
| **Pattern** | Put each number into one of k **buckets**, with heavy pruning |
| **Code** | [M12_PartitionToKEqualSumSubsets.java](M12_PartitionToKEqualSumSubsets.java) |
| **Run** | `java M12_PartitionToKEqualSumSubsets.java` |

Backtracking basics are explained in [E01](../02-Easy/E01_BinaryWatch.md#3-java-concepts-used-in-this-problem). The kinds of pruning are measured in [01-Basics/PruningAndConstraints.java](../01-Basics/PruningAndConstraints.java).

---

## 1. The problem

Can `nums` be split into k non-empty groups with equal sums?

| nums | k | answer | why |
|---|---|---|---|
| [4, 3, 2, 3, 5, 2, 1] | 4 | true | (5), (1, 4), (2, 3), (2, 3) |
| [1, 2, 3, 4] | 3 | false | total 10 isn't divisible by 3 |
| [2, 2, 2, 2, 3, 4, 5] | 4 | false | total 20, target 5, but no valid grouping |

---

## 2. How to think about it

### Quick checks
- If `total % k != 0`, return false.
- target = total / k. If any number is bigger than the target, return false.

### Bucket backtracking
Place `nums[i]` into some bucket whose sum + `nums[i]` ≤ target, recurse on i + 1, then take it out again. If every number is placed, return true. Every bucket must then be **exactly** the target, because the total is k · target and no bucket exceeds it.

### Pruning (it's hopelessly exponential without it)
1. **Sort descending.** Big numbers have the fewest places to go, so they fail fast, near the root of the tree.
2. **Symmetry.** If two buckets currently have the same sum, putting `nums[i]` in either gives the same subtree. Try only the first of them. In particular, once a number fails in an **empty** bucket, it will fail in every other empty bucket too.

### The alternative
A bitmask DP over "which numbers are used" (2ⁿ states). It's the file's referee.

### Dry run: [5, 4, 3, 3, 2, 2, 1] (sorted), target 5

| number | bucket sums before | placed in | bucket sums after |
|---|---|---|---|
| 5 | [0, 0, 0, 0] | b0 | [5, 0, 0, 0] |
| 4 | [5, 0, 0, 0] | b1 (b0 would be 9) | [5, 4, 0, 0] |
| 3 | [5, 4, 0, 0] | b2 (b1 would be 7) | [5, 4, 3, 0] |
| 3 | [5, 4, 3, 0] | b3 (b2 would be 6) | [5, 4, 3, 3] |
| 2 | [5, 4, 3, 3] | b2 | [5, 4, 5, 3] |
| 2 | [5, 4, 5, 3] | b3 | [5, 4, 5, 5] |
| 1 | [5, 4, 5, 5] | b1 | [5, 5, 5, 5] → **true** |

This input happens to succeed without any backtracking. On [2, 2, 2, 2, 3, 4, 5] the search has to back up and eventually reports false.

---

## 3. Java concepts used in this problem

### 3.1 Sorting `int`s in descending order

```java
Integer[] a = Arrays.stream(nums).boxed().toArray(Integer[]::new);
Arrays.sort(a, Collections.reverseOrder());
```

`Arrays.sort` with a comparator only works on **object** arrays, so the `int[]` is first boxed into an `Integer[]`. `Collections.reverseOrder()` is a ready-made "largest first" comparator. (An alternative: sort the `int[]` ascending and iterate from the end.)

### 3.2 `Arrays.stream(nums).sum()`

Turns the array into an `IntStream` and adds it up in one call.

### 3.3 `Set.add` returns whether the element was new

```java
if (!triedSums.add(bucket[b])) continue;
```

`add` returns `false` if the value was already in the set. So this one line both records the sum **and** skips a bucket whose sum was already tried at this level: the symmetry pruning.

### 3.4 Changing an array in place and undoing it

`bucket[b] += a[i]` before the call and `bucket[b] -= a[i]` after it. The `int[] bucket` is shared by all calls, like the `path` list in other problems.

### 3.5 Early `return true`

`if (place(...)) return true;` stops the whole search as soon as one complete placement is found. A yes/no problem doesn't need to enumerate every solution.

---

## 4. Approach 1: Bitmask DP (the referee)

### Code ([M12_PartitionToKEqualSumSubsets.java:62-77](M12_PartitionToKEqualSumSubsets.java#L62-L77))

```java
static boolean bitmaskDp(int[] nums, int k) {
    int total = Arrays.stream(nums).sum(), n = nums.length;
    if (total % k != 0) return false;
    int target = total / k;
    int[] rem = new int[1 << n];
    Arrays.fill(rem, -1);
    rem[0] = 0;
    for (int mask = 0; mask < (1 << n); mask++) {
        if (rem[mask] < 0) continue;
        for (int i = 0; i < n; i++) {
            if ((mask >> i & 1) == 1 || rem[mask] + nums[i] > target) continue;   // fill buckets one at a time
            rem[mask | 1 << i] = (rem[mask] + nums[i]) % target;
        }
    }
    return rem[(1 << n) - 1] == 0;
}
```

| Line | What it does |
|---|---|
| `rem[mask]` | −1 if the numbers in `mask` can't be arranged as full buckets plus one partial bucket; otherwise the partial bucket's sum |
| `rem[0] = 0` | nothing used, the current bucket is empty |
| the inner loop | add an unused number to the current bucket if it fits |
| `% target` | when a bucket becomes exactly full, the next one starts at 0 |
| the answer | all numbers used, and the last bucket closed exactly |

Filling the buckets **one at a time** is enough: any valid partition can be listed bucket by bucket.

- **Time O(2ⁿ · n), space O(2ⁿ).** Good for n ≤ 16 (LeetCode's limit).

---

## 5. Approach 2: Bucket backtracking with pruning (the file's main solution)

### Code ([M12_PartitionToKEqualSumSubsets.java:38-59](M12_PartitionToKEqualSumSubsets.java#L38-L59))

```java
static boolean canPartitionKSubsets(int[] nums, int k) {
    int total = Arrays.stream(nums).sum();
    if (total % k != 0) return false;
    int target = total / k;
    Integer[] a = Arrays.stream(nums).boxed().toArray(Integer[]::new);
    Arrays.sort(a, Collections.reverseOrder());               // big numbers first
    if (a[0] > target) return false;
    return place(a, 0, new int[k], target);
}

static boolean place(Integer[] a, int i, int[] bucket, int target) {
    if (i == a.length) return true;
    Set<Integer> triedSums = new HashSet<>();
    for (int b = 0; b < bucket.length; b++) {
        if (bucket[b] + a[i] > target) continue;
        if (!triedSums.add(bucket[b])) continue;              // same sum as a bucket already tried: symmetric
        bucket[b] += a[i];
        if (place(a, i + 1, bucket, target)) return true;
        bucket[b] -= a[i];
    }
    return false;
}
```

### Line by line

| Line | What it does |
|---|---|
| the quick checks | divisibility, and the largest number fits |
| sort descending | fail fast |
| `i == a.length` | everything is placed: success |
| `triedSums` | the bucket sums already tried for this number, at this level |
| the first `continue` | this bucket would overflow |
| the second `continue` | symmetric with a bucket already tried |
| place / recurse / undo | the backtracking step; stop at the first success |

`triedSums` is a new set in **each** call, because the symmetry only applies among the choices for one number at one point in the search.

### Complexity
- **Time O(kⁿ)** in the worst case, but far less with the pruning.
- **Space O(n)** recursion, plus O(k) per level for `triedSums`.

---

## 6. Comparing the approaches

| | Bitmask DP | Bucket backtracking |
|---|---|---|
| Time | O(2ⁿ · n) always | O(kⁿ) worst, usually tiny |
| Space | O(2ⁿ) | O(n) |
| Gets faster with pruning? | no | yes |

The file checks the backtracking against the DP on 1,000 random cases (up to 12 numbers, k up to 5).

---

## 7. Edge cases and common mistakes

**Edge cases:** k = 1 (always true), k = n (all numbers must be equal), a number larger than the target.

**Mistakes:**
1. Not undoing `bucket[b] += a[i]` after a failed call.
2. Skipping the symmetry pruning (correct, but times out).
3. Sorting ascending (slow: small numbers spread out before the big ones fail).
4. Sharing one `triedSums` across levels.

---

## 8. Interview questions and answers

**Q1. Why does sorting descending help so much?**
> Large numbers have the fewest valid buckets. Placing them first means a dead end is discovered near the root of the tree, where cutting it removes the most work.

**Q2. Explain the symmetry pruning.**
> Buckets are interchangeable. If two buckets hold the same sum, putting the current number in either one leads to the same set of outcomes, so only one needs to be tried.

### Follow-up questions

**F1. Matchsticks to Square.** → [M14](M14_MatchsticksToSquare.md)
> Exactly this problem with k = 4.

**F2. Partition Equal Subset Sum (LC 416, k = 2).**
> A 0/1 knapsack DP for total / 2: O(n · total).

**F3. Fair Distribution of Cookies (LC 2305).**
> Buckets again, minimising the maximum bucket instead of requiring equality.

---

## 9. Running the file

```bash
java M12_PartitionToKEqualSumSubsets.java      # 4/4 tests passed (including 1,000 random cases vs bitmask DP)
```

**One-line summary:** *"Check divisibility, sort descending, and place each number into a bucket that stays ≤ target, skipping buckets whose sum was already tried for this number; success means everything was placed."*
