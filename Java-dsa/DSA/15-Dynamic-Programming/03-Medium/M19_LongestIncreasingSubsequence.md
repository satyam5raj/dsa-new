# M19 · Longest Increasing Subsequence

| | |
|---|---|
| **LeetCode** | [#300 Longest Increasing Subsequence](https://leetcode.com/problems/longest-increasing-subsequence/) |
| **Difficulty** | Medium |
| **Pattern** | LIS: the O(n²) "ending at i" DP, then O(n log n) **patience sorting** ("tails") |
| **Code** | [M19_LongestIncreasingSubsequence.java](M19_LongestIncreasingSubsequence.java) |
| **Run** | `java M19_LongestIncreasingSubsequence.java` |

The DP recipe is explained in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe).

---

## 1. The problem

Return the length of the longest **strictly** increasing subsequence (the elements don't have to be contiguous).

| nums | answer | one LIS |
|---|---|---|
| [10, 9, 2, 5, 3, 7, 101, 18] | 4 | [2, 3, 7, 18] or [2, 5, 7, 101] |
| [0, 1, 0, 3, 2, 3] | 4 | [0, 1, 2, 3] |
| [7, 7, 7, 7] | 1 | strictly increasing |

---

## 2. How to think about it

### Approach 1: O(n²), "ending at i"

| step | for this problem |
|---|---|
| **State** | dp[i] = the length of the longest increasing subsequence **ending at** i |
| **Transition** | dp[i] = 1 + max(dp[j] for j < i with a[j] < a[i]), or 1 if there's none |
| **Answer** | the **maximum** over all dp[i], not dp[n − 1]: the best subsequence may end anywhere |

"Ending at i" is the trick: it fixes the last value, so whether a[i] can extend a subsequence is easy to check.

### Approach 2: O(n log n), "tails"
`tails[k]` = the **smallest** possible tail of an increasing subsequence of length k + 1. `tails` is always sorted. For each x:
- binary-search the first `tails[k] ≥ x` and replace it with x (a better, lower tail for length k + 1);
- if x is bigger than every tail, append it (a longer subsequence now exists).

The answer is the length of `tails`. **Note:** `tails` itself is generally **not** a valid subsequence; only its length means something. To get an actual LIS, use parent pointers (shown below with the O(n²) version).

### Why keep the smallest tail?
A smaller tail for a given length can be extended by more future values. Replacing a tail with a smaller one never hurts and might help.

### Dry run: tails on [10, 9, 2, 5, 3, 7, 101, 18]

| x | action | tails |
|---|---|---|
| 10 | append | [10] |
| 9 | replace 10 | [9] |
| 2 | replace 9 | [2] |
| 5 | append | [2, 5] |
| 3 | replace 5 | [2, 3] |
| 7 | append | [2, 3, 7] |
| 101 | append | [2, 3, 7, 101] |
| 18 | replace 101 | [2, 3, 7, 18] |

Length **4**.

### Dry run: the O(n²) DP on the same array

| i | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
|---|---|---|---|---|---|---|---|---|
| a[i] | 10 | 9 | 2 | 5 | 3 | 7 | 101 | 18 |
| dp[i] | 1 | 1 | 1 | 2 | 2 | 3 | 4 | 4 |
| parent | − | − | − | 2 | 2 | 3 | 5 | 5 |

The best end is index 6 (the first with dp = 4). Following parents: 101 ← 7 ← 5 ← 2, giving **[2, 5, 7, 101]**.

---

## 3. Java concepts used in this problem

### 3.1 Lower-bound binary search

```java
int lo = 0, hi = len;                         // first index with tails[idx] >= x
while (lo < hi) {
    int mid = (lo + hi) >>> 1;
    if (tails[mid] < x) lo = mid + 1; else hi = mid;
}
```

This finds the first position whose value is ≥ x (the "lower bound"). Searching `[0, len)` with `hi = len` means "x is bigger than everything" lands at `lo == len`, which is the append case. Using `≥` (not `>`) makes the subsequence **strictly** increasing: an equal value replaces instead of extending.

### 3.2 A fixed array plus a length

`int[] tails = new int[nums.length]; int len = 0;` uses only the first `len` slots. It's cheaper than an `ArrayList<Integer>` (no boxing).

### 3.3 Parent pointers and `addFirst`

`parent[i]` stores the j that gave dp[i] its value. Walking `i = parent[i]` from the best end visits the subsequence backwards; `LinkedList.addFirst` puts it back in order.

### 3.4 Timing a big run

The test runs the tails version on 200,000 random numbers: about 7 ms in one run, where the O(n²) version would need about 4 · 10¹⁰ steps.

---

## 4. Approach 1: O(n²) DP

### Code ([M19_LongestIncreasingSubsequence.java:55-64](M19_LongestIncreasingSubsequence.java#L55-L64))

```java
static int lisQuadratic(int[] a) {
    int[] dp = new int[a.length];
    int best = 0;
    for (int i = 0; i < a.length; i++) {
        dp[i] = 1;
        for (int j = 0; j < i; j++) if (a[j] < a[i]) dp[i] = Math.max(dp[i], dp[j] + 1);
        best = Math.max(best, dp[i]);
    }
    return best;
}
```

| Line | What it does |
|---|---|
| `dp[i] = 1` | a[i] alone |
| the inner loop | extend any earlier, smaller ending |
| `best` | the maximum over all endings |

- **Time O(n²), space O(n).**

---

## 5. Approach 2: O(n²) with reconstruction

### Code ([M19_LongestIncreasingSubsequence.java:67-78](M19_LongestIncreasingSubsequence.java#L67-L78))

```java
static List<Integer> oneLIS(int[] a) {
    int n = a.length, end = 0;
    int[] dp = new int[n], parent = new int[n];
    for (int i = 0; i < n; i++) {
        dp[i] = 1; parent[i] = -1;
        for (int j = 0; j < i; j++) if (a[j] < a[i] && dp[j] + 1 > dp[i]) { dp[i] = dp[j] + 1; parent[i] = j; }
        if (dp[i] > dp[end]) end = i;
    }
    LinkedList<Integer> out = new LinkedList<>();
    for (int i = end; i != -1; i = parent[i]) out.addFirst(a[i]);
    return out;
}
```

The same DP, also recording which j each dp[i] came from, and the index where the best subsequence ends. The dry run is in section 2.

---

## 6. Approach 3: Tails + binary search (the file's main solution)

### Code ([M19_LongestIncreasingSubsequence.java:40-53](M19_LongestIncreasingSubsequence.java#L40-L53))

```java
static int lengthOfLIS(int[] nums) {
    int[] tails = new int[nums.length];
    int len = 0;
    for (int x : nums) {
        int lo = 0, hi = len;                         // first index with tails[idx] >= x
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (tails[mid] < x) lo = mid + 1; else hi = mid;
        }
        tails[lo] = x;
        if (lo == len) len++;
    }
    return len;
}
```

| Line | What it does |
|---|---|
| the binary search | where x goes among the current tails |
| `tails[lo] = x` | replace (or, at `lo == len`, append) |
| `len++` | a new, longer length was reached |

- **Time O(n log n), space O(n).**

---

## 7. Comparing the approaches

| | O(n²) DP | Tails |
|---|---|---|
| Time | O(n²) | **O(n log n)** |
| Gives an actual subsequence? | yes, with parents | not directly |
| n = 200,000 | ~4 · 10¹⁰ steps | ~7 ms (measured) |

The file checks, on 1,000 random arrays, that the tails length equals the O(n²) length and that the reconstructed subsequence has that length and is increasing.

---

## 8. Edge cases and common mistakes

**Edge cases:** all equal (1), strictly decreasing (1), already increasing (n), a single element.

**Mistakes:**
1. Returning dp[n − 1] instead of the maximum.
2. Using `>` instead of `≥` in the search (that allows equal elements: non-decreasing).
3. Presenting `tails` as the LIS itself.

---

## 9. Interview questions and answers

**Q1. Why is `tails` sorted?**
> A subsequence of length k + 1 contains one of length k with a smaller last element, so the smallest tail for length k + 1 is bigger than the smallest tail for length k.

**Q2. How do you get the actual LIS in O(n log n)?**
> Store, for each element, the index of the tail it replaced or extended, plus a parent pointer to the element in `tails[lo − 1]` at that moment; walk the parents back from the last tail.

### Follow-up questions

**F1. Number of LIS (LC 673).**
> The O(n²) DP with a second array counting how many subsequences achieve dp[i].

**F2. Russian Doll Envelopes.** → [H03](../04-Hard/H03_RussianDollEnvelopes.md)
> Sort by width ascending and height descending, then LIS on the heights.

**F3. Largest Divisible Subset.** → [M20](M20_LargestDivisibleSubset.md)
> LIS where "increasing" becomes "divides".

---

## 10. Running the file

```bash
java M19_LongestIncreasingSubsequence.java      # 5/5 tests passed; prints n = 200,000 solved in a few ms
```

**One-line summary:** *"dp[i] = 1 + the best dp[j] with a[j] < a[i] gives O(n²); keeping the smallest tail for each length and binary-searching where each x goes gives the length in O(n log n)."*
