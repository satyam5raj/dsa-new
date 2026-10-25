# M20 · Largest Divisible Subset

| | |
|---|---|
| **LeetCode** | [#368 Largest Divisible Subset](https://leetcode.com/problems/largest-divisible-subset/) |
| **Difficulty** | Medium |
| **Pattern** | **Sort**, then LIS where "increasing" becomes "divides" (+ reconstruction) |
| **Code** | [M20_LargestDivisibleSubset.java](M20_LargestDivisibleSubset.java) |
| **Run** | `java M20_LargestDivisibleSubset.java` |

The LIS DP and parent pointers are explained in [M19](M19_LongestIncreasingSubsequence.md).

---

## 1. The problem

Given distinct positive integers, return the largest subset in which every pair (a, b) satisfies a % b == 0 or b % a == 0.

| nums | answer |
|---|---|
| [1, 2, 3] | [1, 2] (or [1, 3]) |
| [1, 2, 4, 8] | [1, 2, 4, 8] |
| [1, 2, 4, 8, 9, 3] | [1, 2, 4, 8] |
| [5] | [5] |

---

## 2. How to think about it

### Brute force
Check all 2ⁿ subsets.

### After sorting, a valid subset is a chain
Sorted, a valid subset looks like a₁ | a₂ | a₃ | … (each divides the next). And divisibility is **transitive**: if a | b and b | c, then a | c. So to extend a chain that ends at b with c, you only need to check the **last** element: b | c.

That's exactly LIS with the comparison `a[j] < a[i]` replaced by `a[i] % a[j] == 0`:

```
dp[i] = 1 + max(dp[j] : j < i, a[i] % a[j] == 0),   parent[i] = that j
```

The problem asks for the subset itself, so follow the parent pointers from the best end.

### Dry run: [1, 2, 4, 8, 9, 3] → sorted [1, 2, 3, 4, 8, 9]

| i | a[i] | divisible by earlier | dp[i] | parent |
|---|---|---|---|---|
| 0 | 1 | | 1 | − |
| 1 | 2 | 1 | 2 | 1 |
| 2 | 3 | 1 | 2 | 1 |
| 3 | 4 | 1, 2 | 3 | 2 |
| 4 | 8 | 1, 2, 4 | 4 | 4 |
| 5 | 9 | 1, 3 | 3 | 3 |

The best end is 8 (dp = 4): 8 ← 4 ← 2 ← 1, giving **[1, 2, 4, 8]**.

---

## 3. Java concepts used in this problem

### 3.1 Sorting a copy

`int[] a = nums.clone(); Arrays.sort(a);` so that any divisor comes before its multiples.

### 3.2 `a[i] % a[j] == 0`

Since the array is sorted and distinct, a[j] < a[i], so only "a[j] divides a[i]" needs checking.

### 3.3 A `Set` to make distinct random input (in the test)

```java
while (set.size() < n) set.add(1 + rnd.nextInt(60));
int[] a = set.stream().mapToInt(Integer::intValue).toArray();
```

Adding to a set until it has n elements guarantees distinct values; `mapToInt(...).toArray()` turns the set into an `int[]`.

---

## 4. Approach 1: Every subset (the referee)

### Code ([M20_LargestDivisibleSubset.java:51-65](M20_LargestDivisibleSubset.java#L51-L65))

```java
static boolean valid(List<Integer> s) {
    for (int i = 0; i < s.size(); i++) for (int j = i + 1; j < s.size(); j++)
        if (s.get(i) % s.get(j) != 0 && s.get(j) % s.get(i) != 0) return false;
    return true;
}

static int bruteSize(int[] a) {
    int best = 0;
    for (int mask = 1; mask < 1 << a.length; mask++) {
        List<Integer> s = new ArrayList<>();
        for (int i = 0; i < a.length; i++) if ((mask >> i & 1) == 1) s.add(a[i]);
        if (valid(s)) best = Math.max(best, s.size());
    }
    return best;
}
```

`valid` checks every pair directly (no reliance on transitivity), and `bruteSize` finds the largest valid subset. **O(n² · 2ⁿ).**

---

## 5. Approach 2: Sort + divisibility LIS (the file's main solution)

### Code ([M20_LargestDivisibleSubset.java:35-49](M20_LargestDivisibleSubset.java#L35-L49))

```java
static List<Integer> largestDivisibleSubset(int[] nums) {
    int[] a = nums.clone();
    Arrays.sort(a);
    int n = a.length, end = 0;
    int[] dp = new int[n], parent = new int[n];
    for (int i = 0; i < n; i++) {
        dp[i] = 1; parent[i] = -1;
        for (int j = 0; j < i; j++)
            if (a[i] % a[j] == 0 && dp[j] + 1 > dp[i]) { dp[i] = dp[j] + 1; parent[i] = j; }
        if (dp[i] > dp[end]) end = i;
    }
    LinkedList<Integer> out = new LinkedList<>();
    for (int i = end; i != -1; i = parent[i]) out.addFirst(a[i]);
    return out;
}
```

### Line by line

| Line | What it does |
|---|---|
| sort | divisors before multiples |
| `dp[i] = 1` | a[i] alone |
| the inner loop | extend the longest chain whose last element divides a[i] |
| `end` | where the longest chain ends |
| the walk | follow the parents back to the smallest element |

### Complexity
- **Time O(n²), space O(n).**

---

## 6. Comparing the approaches

| | Every subset | Divisibility LIS |
|---|---|---|
| Time | O(n² · 2ⁿ) | **O(n²)** |
| Returns the subset | yes | yes |

The file checks, on 500 random sets of distinct numbers, that the result is valid and as large as the brute force's best.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single number, no two numbers divisible (any single element), 1 present (it divides everything).

**Mistakes:**
1. Not sorting (then a chain can't be extended by checking only its last element).
2. Checking `a[j] % a[i]` instead of `a[i] % a[j]`.
3. Returning only the size.

---

## 8. Interview questions and answers

**Q1. Why is checking only the last element enough?**
> After sorting, a chain a₁ | a₂ | … | aₖ has every element dividing the last one (by transitivity). If aₖ | x, then every element divides x too, so the extended set is still valid.

**Q2. Why is this LIS?**
> Sorting gives a fixed order, and we want the longest chain where each element is "compatible" with the previous one. LIS is the same with "compatible" meaning "smaller".

### Follow-up questions

**F1. Longest Increasing Subsequence.** → [M19](M19_LongestIncreasingSubsequence.md)

**F2. Longest String Chain (LC 1048).**
> Sort words by length; dp[word] = 1 + max dp over the words formed by deleting one letter.

**F3. Count the divisible subsets.**
> The same DP summing counts instead of taking the maximum (though "all pairs" means each counted set must still be a chain).

---

## 9. Running the file

```bash
java M20_LargestDivisibleSubset.java      # 5/5 tests passed (including 500 random: valid + max size vs 2^n)
```

**One-line summary:** *"Sort, then run LIS where a[j] extends a[i] if a[i] % a[j] == 0 (transitivity makes the last element enough), and follow parent pointers to return the chain."*
