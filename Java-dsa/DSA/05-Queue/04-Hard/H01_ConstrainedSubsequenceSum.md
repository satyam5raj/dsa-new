# H01 · Constrained Subsequence Sum ⭐

| | |
|---|---|
| **LeetCode** | [#1425 Constrained Subsequence Sum](https://leetcode.com/problems/constrained-subsequence-sum/) |
| **Difficulty** | Hard |
| **Pattern** | DP + monotonic deque ([Jump Game VI](../03-Medium/M06_JumpGameVI.md), with "start fresh" allowed) |
| **Code** | [H01_ConstrainedSubsequenceSum.java](H01_ConstrainedSubsequenceSum.java) |
| **Run** | `java H01_ConstrainedSubsequenceSum.java` |

⭐ = a bonus classic.

---

## 1. The problem

Choose a **non-empty subsequence** in which every two **consecutive chosen indices** are at most k apart (`j − i ≤ k`). Maximise its sum.

| nums | k | answer | chosen |
|---|---|---|---|
| `[10, 2, -10, 5, 20]` | 2 | 37 | 10, 2, 5, 20 (skips −10: the gap from index 1 to 3 is 2 ≤ k) |
| `[-1, -2, -3]` | 1 | −1 | just −1 |
| `[10, -2, -10, -5, 20]` | 2 | 23 | 10, −2, −5, 20 |

**Constraints:** `1 ≤ k ≤ n ≤ 10⁵`.

---

## 2. How to think about it

### The DP
```
dp[i] = the best sum of a valid subsequence that ENDS by choosing index i
dp[i] = nums[i] + max(0, max(dp[i−k .. i−1]))
answer = max over all dp[i]            (the subsequence can end anywhere)
```

Two differences from Jump Game VI ([M06](../03-Medium/M06_JumpGameVI.md)):
1. **`max(0, …)`**: we may **start a new subsequence** at i if every reachable previous sum is negative. That's exactly Kadane's "restart" idea from [Arrays M01](../../01-Arrays/03-Medium/M01_MaximumSubarray.md).
2. The answer is the **max of all dp[i]**, not dp[n−1], because we don't have to start at 0 or end at n−1.

### The window max → a monotonic deque
`max(dp[i−k .. i−1])` is a sliding-window maximum, so keep a deque of indices with decreasing dp.

**Optimisation:** only push i if `dp[i] > 0`. A non-positive dp would never be chosen as a predecessor, because `max(0, …)` already beats it. So an **empty deque means "start fresh"** (use 0).

---

## 3. Java concepts used in this problem

### 3.1 The ternary for the "start fresh" case

```java
dp[i] = nums[i] + (dq.isEmpty() ? 0 : dp[dq.peekFirst()]);
```

The parentheses matter: without them, `+` would bind tighter than `?:` and the expression would mean `(nums[i] + dq.isEmpty()) ? …`, which doesn't even compile.

### 3.2 `Integer.MIN_VALUE` as the starting best

The answer can be negative (when all the numbers are negative), so `best` must start below any possible value, not at 0.

### 3.3 A conditional push in a monotonic deque

`if (dp[i] > 0) dq.offerLast(i);`: the deque only holds **useful** predecessors. The pop loop still runs first, but when `dp[i] ≤ 0` it pops nothing, because everything in the deque is positive.

---

## 4. Approach 1: O(n · k) DP

### Code ([H01_ConstrainedSubsequenceSum.java:56-66](H01_ConstrainedSubsequenceSum.java#L56-L66))

```java
static int brute(int[] nums, int k) {
    int n = nums.length, best = Integer.MIN_VALUE;
    int[] dp = new int[n];
    for (int i = 0; i < n; i++) {
        int prev = 0;
        for (int j = Math.max(0, i - k); j < i; j++) prev = Math.max(prev, dp[j]);
        dp[i] = nums[i] + prev;
        best = Math.max(best, dp[i]);
    }
    return best;
}
```

| Line | What it does |
|---|---|
| `int prev = 0;` | starting at 0 is the "start fresh" option |
| the inner loop | the best dp among the k previous indices (if that beats 0) |
| `dp[i] = nums[i] + prev;` | choose i |
| `best = Math.max(best, dp[i]);` | the subsequence may end at i |

**Dry run** `[10, 2, -10, 5, 20]`, k = 2: dp = [10, 12, 2, 17, 37] → best **37**.

**O(n · k)** time, too slow for n = k = 10⁵.

---

## 5. Approach 2: DP + a max-heap

```java
static int constrainedSubsetSumHeap(int[] nums, int k) {
    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> b[0] - a[0]);   // {dp, index}, max dp first
    int best = Integer.MIN_VALUE;
    for (int i = 0; i < nums.length; i++) {
        while (!pq.isEmpty() && pq.peek()[1] < i - k) pq.poll();            // lazy removal: out of range
        int dp = nums[i] + (pq.isEmpty() ? 0 : Math.max(0, pq.peek()[0]));
        best = Math.max(best, dp);
        pq.offer(new int[]{dp, i});
    }
    return best;
}
```

The heap's top is the largest dp; entries that are too old are removed only when they reach the top ("lazy deletion"). That's **O(n log n)**. It's a good fallback if you don't remember the deque trick. (`b[0] - a[0]` is safe here: the dp values stay within about ±10⁹.)

---

## 6. Approach 3: DP + a monotonic deque (the file's solution)

### Code ([H01_ConstrainedSubsequenceSum.java:42-54](H01_ConstrainedSubsequenceSum.java#L42-L54))

```java
static int constrainedSubsetSum(int[] nums, int k) {
    int n = nums.length;
    int[] dp = new int[n];
    Deque<Integer> dq = new ArrayDeque<>();                  // indices, dp decreasing (all > 0)
    int best = Integer.MIN_VALUE;
    for (int i = 0; i < n; i++) {
        if (!dq.isEmpty() && dq.peekFirst() < i - k) dq.pollFirst();
        dp[i] = nums[i] + (dq.isEmpty() ? 0 : dp[dq.peekFirst()]);   // 0 = start fresh
        best = Math.max(best, dp[i]);
        while (!dq.isEmpty() && dp[dq.peekLast()] <= dp[i]) dq.pollLast();
        if (dp[i] > 0) dq.offerLast(i);                      // only useful as a predecessor if positive
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (... dq.peekFirst() < i - k) dq.pollFirst();` | expire the front if it's more than k behind. An `if` is enough, since at most one index expires per step |
| `dp[i] = nums[i] + (dq.isEmpty() ? 0 : dp[front])` | the best positive predecessor, or start fresh |
| `best = Math.max(best, dp[i]);` | the subsequence may end here |
| the `while` pop | the back indices with dp ≤ dp[i] are dominated |
| `if (dp[i] > 0) dq.offerLast(i);` | keep i only if it could help a later index |

### Dry run 1: `[10, 2, -10, 5, 20]`, k = 2

| i | nums[i] | expire? | dp[i] | pops | push? | dq after | best |
|---|---|---|---|---|---|---|---|
| 0 | 10 | | 10 + 0 = 10 | | yes | [0] | 10 |
| 1 | 2 | | 2 + 10 = 12 | 0 (10 ≤ 12) | yes | [1] | 12 |
| 2 | −10 | | −10 + 12 = 2 | | yes | [1, 2] | 12 |
| 3 | 5 | 1 < 1? no (still in range) | 5 + 12 = 17 | 2, 1 | yes | [3] | 17 |
| 4 | 20 | 3 < 2? no | 20 + 17 = **37** | 3 | yes | [4] | **37** |

### Dry run 2: `[10, -2, -10, -5, 20]`, k = 2

| i | nums[i] | expire? | dp[i] | pops | push? | dq after | best |
|---|---|---|---|---|---|---|---|
| 0 | 10 | | 10 | | yes | [0] | 10 |
| 1 | −2 | | −2 + 10 = 8 | | yes | [0, 1] | 10 |
| 2 | −10 | 0 < 0? no | −10 + 10 = 0 | | **no** (0 isn't > 0) | [0, 1] | 10 |
| 3 | −5 | 0 < 1? **yes** | −5 + 8 = 3 | | yes | [1, 3] | 10 |
| 4 | 20 | 1 < 2? **yes** | 20 + 3 = **23** | 3 | yes | [4] | **23** |

The best path is 10 → −2 → −5 → 20: it has to step on −5, because jumping from index 1 straight to 4 is a gap of 3 > k.

### Dry run 3: `[-1, -2, -3]`, k = 1
Every dp is negative and never pushed, so each index starts fresh: dp = [−1, −2, −3], best = **−1**.

### Complexity
- **Time O(n):** each index enters and leaves the deque at most once.
- **Space O(n)** (dp; O(k) if you store the dp values in the deque).

---

## 7. Comparing the approaches

| | O(n·k) DP | DP + heap | DP + monotonic deque |
|---|---|---|---|
| Time | O(n · k) | O(n log n) | **O(n)** |
| Space | O(n) | O(n) | O(n) |

---

## 8. Edge cases and common mistakes

**Edge cases:** all negative (the answer is the largest element), k = 1 (contiguous: plain Kadane), k ≥ n (take every positive, or the max element if none are positive), n = 1.

**Mistakes:**
1. Forgetting `max(0, …)`, which forces you to extend a negative sum.
2. Returning dp[n−1] instead of the max over all dp.
3. Starting best at 0 (wrong when everything is negative).
4. Missing the parentheses around the ternary.

---

## 9. Interview questions and answers

**Q1. How does this relate to Kadane's algorithm?**
> With k = 1 it is Kadane: `dp[i] = nums[i] + max(0, dp[i−1])`. A larger k just widens "the previous element" to "the best of the previous k", which is a sliding-window maximum.

**Q2. Why can you skip pushing non-positive dp values?**
> A predecessor is only used through `max(0, …)`. A dp ≤ 0 never beats 0, so it's never useful. Keeping only positives means "empty deque" equals "start fresh".

### Follow-up questions

**F1. Return the subsequence itself.**
> Store a `parent[i]` (the deque front used, or −1 for a fresh start) and walk back from the argmax.

**F2. The gap must be at least m and at most k.**
> The window becomes `dp[i−k .. i−m]`: add index `i − m` to the deque when you reach i, instead of adding i itself.

**F3. Minimise instead of maximise.**
> A min-deque, with `min(0, …)`.

---

## 10. Running the file

```bash
java H01_ConstrainedSubsequenceSum.java      # 4/4 tests passed (including 500 random cases vs the O(n·k) DP)
```

**One-line summary:** *"dp[i] = nums[i] + max(0, the best dp in the last k); a decreasing deque of the positive dp indices gives that max in O(1)."*
