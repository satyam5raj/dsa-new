# H09 · Maximum Profit in Job Scheduling

| | |
|---|---|
| **LeetCode** | [#1235 Maximum Profit in Job Scheduling](https://leetcode.com/problems/maximum-profit-in-job-scheduling/) |
| **Difficulty** | Hard |
| **Pattern** | **Weighted interval scheduling**: sort by end + DP + binary search |
| **Code** | [H09_MaximumProfitInJobScheduling.java](H09_MaximumProfitInJobScheduling.java) |
| **Run** | `java H09_MaximumProfitInJobScheduling.java` |

The take-or-skip DP is [M01 House Robber](../03-Medium/M01_HouseRobber.md); binary search with "upper bound" is like the lower bound in [M19](../03-Medium/M19_LongestIncreasingSubsequence.md#31-lower-bound-binary-search).

---

## 1. The problem

Each job has a start time, an end time and a profit. Choose non-overlapping jobs (a job may start exactly when another ends) to maximise the total profit.

| start | end | profit | answer |
|---|---|---|---|
| [1,2,3,3] | [3,4,5,6] | [50,10,40,70] | 120 (50 + 70) |
| [1,2,3,4,6] | [3,5,10,6,9] | [20,20,100,70,60] | 150 (20 + 70 + 60) |
| [1,1,1] | [2,3,4] | [5,6,4] | 6 |

---

## 2. How to think about it

### Greedy fails
"Earliest end first" is correct for **counting** jobs (unweighted), not for profit. "Highest profit first" fails too.

### Sort by end time, then take or skip each job
For job i (in end-time order), either:
- **skip it**: the best of the first i − 1 jobs, dp[i − 1];
- **take it**: profit[i] plus the best among the jobs that **end ≤ start[i]**.

Because the jobs are sorted by end, "the jobs ending ≤ start[i]" form a **prefix** of the list. Its length p can be found with a binary search (an "upper bound" on the end times).

| step | for this problem |
|---|---|
| **State** | dp[i] = the best profit using the first i jobs (sorted by end) |
| **Transition** | dp[i] = max(dp[i − 1], profit[i − 1] + dp[p]) |
| **Base** | dp[0] = 0 |
| **Answer** | dp[n] |

It's House Robber, where "the previous compatible job" replaces "i − 2".

### Dry run: example 1
Sorted by end: [1,3,50], [2,4,10], [3,5,40], [3,6,70]. End times: 3, 4, 5, 6.

| i | job | p = jobs ending ≤ start | dp[i] |
|---|---|---|---|
| 1 | [1,3,50] | 0 | max(0, 50 + 0) = 50 |
| 2 | [2,4,10] | 0 (no end ≤ 2) | max(50, 10 + 0) = 50 |
| 3 | [3,5,40] | 1 (end 3 ≤ 3) | max(50, 40 + dp[1]) = 90 |
| 4 | [3,6,70] | 1 | max(90, 70 + dp[1]) = **120** |

---

## 3. Java concepts used in this problem

### 3.1 Sorting indices by another array

```java
Integer[] idx = new Integer[n];
for (int i = 0; i < n; i++) idx[i] = i;
Arrays.sort(idx, (a, b) -> Integer.compare(endTime[a], endTime[b]));
```

The three input arrays are parallel, so instead of moving their elements around, the code sorts **indices** by end time and reads each job through `idx`. (The `Integer[]` is needed for a comparator, as in [Union-Find H03](../../11-Union-Find/04-Hard/H03_EdgeLengthLimitedPaths.md#32-sorting-indices-instead-of-the-queries-themselves).)

### 3.2 Upper-bound binary search

```java
static int upperBound(int[] a, int len, int key) {           // first index in a[0..len) with a[idx] > key
    int lo = 0, hi = len;
    while (lo < hi) { int mid = (lo + hi) >>> 1; if (a[mid] <= key) lo = mid + 1; else hi = mid; }
    return lo;
}
```

It returns the number of elements ≤ key in the sorted prefix. Using `≤` (not `<`) allows a job to start exactly when another ends.

### 3.3 Searching only the earlier jobs

`upperBound(ends, i − 1, …)` searches only the jobs before job i, because a job can't be combined with itself or later jobs in this ordering.

### 3.4 Overlap test (in the referee)

```java
s[i] < e[j] && s[j] < e[i]
```

Two half-open intervals [s, e) overlap exactly when each starts before the other ends.

---

## 4. Approach 1: Every subset of jobs (the referee)

### Code ([H09_MaximumProfitInJobScheduling.java:63-77](H09_MaximumProfitInJobScheduling.java#L63-L77))

```java
static int brute(int[] s, int[] e, int[] p) {
    int n = s.length, best = 0;
    for (int mask = 0; mask < 1 << n; mask++) {
        boolean ok = true;
        int sum = 0;
        for (int i = 0; i < n && ok; i++) {
            if ((mask >> i & 1) == 0) continue;
            sum += p[i];
            for (int j = i + 1; j < n && ok; j++)
                if ((mask >> j & 1) == 1 && s[i] < e[j] && s[j] < e[i]) ok = false;   // overlap
        }
        if (ok) best = Math.max(best, sum);
    }
    return best;
}
```

- **Time O(n² · 2ⁿ).** The test keeps n ≤ 12.

---

## 5. Approach 2: Sort by end + DP + binary search (the file's main solution)

### Code ([H09_MaximumProfitInJobScheduling.java:41-61](H09_MaximumProfitInJobScheduling.java#L41-L61))

```java
static int jobScheduling(int[] startTime, int[] endTime, int[] profit) {
    int n = startTime.length;
    Integer[] idx = new Integer[n];
    for (int i = 0; i < n; i++) idx[i] = i;
    Arrays.sort(idx, (a, b) -> Integer.compare(endTime[a], endTime[b]));
    int[] ends = new int[n];
    for (int i = 0; i < n; i++) ends[i] = endTime[idx[i]];
    int[] dp = new int[n + 1];
    for (int i = 1; i <= n; i++) {
        int job = idx[i - 1];
        int p = upperBound(ends, i - 1, startTime[job]);        // # of earlier jobs with end <= start
        dp[i] = Math.max(dp[i - 1], profit[job] + dp[p]);
    }
    return dp[n];
}
```

| Line | What it does |
|---|---|
| the index sort | jobs in end-time order |
| `ends` | the sorted end times, for binary search |
| `p` | how many earlier jobs are compatible with this one |
| `dp[i]` | skip it, or take it on top of the best compatible prefix |

### Complexity
- **Time O(n log n)** (the sort plus n binary searches), **space O(n).**

---

## 6. Comparing the approaches

| | Every subset | O(n²) DP (scan for p) | Sort + binary search |
|---|---|---|---|
| Time | O(n² · 2ⁿ) | O(n²) | **O(n log n)** |

The file checks the fast version against every subset on 500 random job sets.

---

## 7. Edge cases and common mistakes

**Edge cases:** one job, all jobs overlapping (the most profitable one), back-to-back jobs (end = next start, allowed).

**Mistakes:**
1. Sorting by start time (then "compatible earlier jobs" isn't a prefix).
2. Using `<` in the binary search (that forbids back-to-back jobs).
3. Greedy by profit or by end.

---

## 8. Interview questions and answers

**Q1. Why sort by end time?**
> Then the jobs that finish before job i starts are exactly a prefix of the sorted list, so the best compatible profit is a single dp value, found by binary search.

**Q2. How is this House Robber?**
> Take or skip each item, where taking it forbids its conflicting neighbours. In House Robber the last compatible house is always i − 2; here it's found by binary search.

### Follow-up questions

**F1. Return the chosen jobs.**
> Walk back: if dp[i] == dp[i − 1], job i was skipped; otherwise it was taken, so jump to p.

**F2. Non-overlapping Intervals (LC 435).**
> The unweighted version: greedy by earliest end.

**F3. Maximum Number of Events That Can Be Attended II (LC 1751).**
> The same DP with an extra dimension: at most k events.

---

## 9. Running the file

```bash
java H09_MaximumProfitInJobScheduling.java      # 4/4 tests passed (including 500 random vs every subset)
```

**One-line summary:** *"Sort the jobs by end; dp[i] = max(dp[i − 1], profit + dp[p]), where p is the number of earlier jobs ending ≤ this job's start, found by binary search."*
