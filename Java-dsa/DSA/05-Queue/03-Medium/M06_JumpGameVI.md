# M06 · Jump Game VI ⭐

| | |
|---|---|
| **LeetCode** | [#1696 Jump Game VI](https://leetcode.com/problems/jump-game-vi/) |
| **Difficulty** | Medium |
| **Pattern** | DP where each state needs the max of the previous k states → a monotonic deque |
| **Code** | [M06_JumpGameVI.java](M06_JumpGameVI.java) |
| **Run** | `java M06_JumpGameVI.java` |

⭐ = a bonus classic (DP + monotonic deque).

---

## 1. The problem

Start at index 0; each move jumps **1 to k** steps forward. Your score is the **sum of nums at every index you land on** (including the first and last). Maximise the score on reaching the last index.

| nums | k | answer | best path |
|---|---|---|---|
| `[1,-1,-2,4,-7,3]` | 2 | 7 | 0 → 1 → 3 → 5: 1 − 1 + 4 + 3 |
| `[10,-5,-2,4,0,3]` | 3 | 17 | 0 → 3 → 4 → 5: 10 + 4 + 0 + 3 |
| `[1,-5,-20,4,-1,3,-6,-3]` | 2 | 0 | |

**Constraints:** `1 ≤ n, k ≤ 10⁵`.

---

## 2. How to think about it

### The DP definition
```
dp[i] = the best score of a path that ENDS at index i
dp[i] = nums[i] + max(dp[i−k], …, dp[i−1])     (we came from one of the k previous indices)
```
Computing that max naively is O(k) per index → **O(n · k)**, which is too slow for n = k = 10⁵.

### "The max of the last k values, as the window slides" = Sliding Window Maximum ([Arrays H02](../../01-Arrays/04-Hard/H02_SlidingWindowMaximum.md))
Keep a **monotonic deque of indices whose dp values are decreasing**:
- drop the front if it's out of reach (index < i − k)
- `dp[i] = nums[i] + dp[front]` (the front is the best reachable dp)
- drop from the **back** every index with `dp ≤ dp[i]` (they'll never be better than i: they're older **and** not larger)
- push i

**The general lesson:** when a DP transition takes the max/min over a **sliding range** of previous states, a monotonic deque makes it O(1) amortised.

---

## 3. Java concepts used in this problem

### 3.1 A DP array

`int[] dp = new int[n]`: each entry is computed from earlier entries, filled left to right. (DP gets a full topic later: [15-Dynamic-Programming](../../15-Dynamic-Programming/README.md).)

### 3.2 A monotonic deque storing indices

```java
Deque<Integer> dq = new ArrayDeque<>();     // indices, with dp[...] decreasing from front to back
dq.peekFirst() < i - k                      // the front is too far behind to jump from
dp[dq.peekLast()] <= dp[i]                  // the back is dominated by i
```

We store **indices** because both the value (`dp[idx]`) and the position (for "out of reach") matter.

### 3.3 Brute-force DP as the test oracle

`brute` is the O(n·k) DP; 500 random cases confirm that the deque version matches it.

---

## 4. Approach 1: Plain DP, O(n · k)

### Code ([M06_JumpGameVI.java:60-69](M06_JumpGameVI.java#L60-L69))

```java
static int brute(int[] nums, int k) {                        // O(n*k) DP
    int[] dp = new int[nums.length];
    dp[0] = nums[0];
    for (int i = 1; i < nums.length; i++) {
        int best = Integer.MIN_VALUE;
        for (int j = Math.max(0, i - k); j < i; j++) best = Math.max(best, dp[j]);
        dp[i] = nums[i] + best;
    }
    return dp[nums.length - 1];
}
```

| Line | What it does |
|---|---|
| `dp[0] = nums[0];` | the start is always visited |
| the inner loop | the best dp among the up-to-k reachable previous indices |
| `dp[i] = nums[i] + best;` | land on i |

It's correct and simple. **O(n · k)**: up to 10¹⁰ operations at the limits.

---

## 5. Approach 2: DP + a monotonic deque (the file's solution)

### Code ([M06_JumpGameVI.java:45-58](M06_JumpGameVI.java#L45-L58))

```java
static int maxResult(int[] nums, int k) {
    int n = nums.length;
    int[] dp = new int[n];
    dp[0] = nums[0];
    Deque<Integer> dq = new ArrayDeque<>();                  // indices, dp values decreasing
    dq.offer(0);
    for (int i = 1; i < n; i++) {
        if (dq.peekFirst() < i - k) dq.pollFirst();          // out of jumping range
        dp[i] = nums[i] + dp[dq.peekFirst()];
        while (!dq.isEmpty() && dp[dq.peekLast()] <= dp[i]) dq.pollLast();
        dq.offerLast(i);
    }
    return dp[n - 1];
}
```

### Line by line

| Line | What it does |
|---|---|
| `dq.offer(0);` | index 0 is the only candidate at first |
| `if (dq.peekFirst() < i - k) dq.pollFirst();` | the front can't reach i anymore. An `if` is enough: the indices enter in order, so at most one expires per step |
| `dp[i] = nums[i] + dp[dq.peekFirst()];` | the front has the largest dp among the reachable indices |
| `while (... dp[dq.peekLast()] <= dp[i]) dq.pollLast();` | the back indices with smaller-or-equal dp are useless from now on |
| `dq.offerLast(i);` | i is a candidate for the next k positions |

### Dry run: `nums = [1, -1, -2, 4, -7, 3]`, k = 2

| i | expire? | dp[i] | pops from the back | dq after (indices) |
|---|---|---|---|---|
| 0 | | 1 | | [0] |
| 1 | 0 < −1? no | −1 + dp[0] = 0 | — (dp[0] = 1 > 0) | [0, 1] |
| 2 | 0 < 0? **no** (still reachable) | −2 + dp[0] = −1 | — | [0, 1, 2] |
| 3 | 0 < 1? **yes** → expire 0 | 4 + dp[1] = 4 | 2 (−1), 1 (0) | [3] |
| 4 | | −7 + dp[3] = −3 | — | [3, 4] |
| 5 | 3 < 3? no | 3 + dp[3] = **7** | 4, 3 | [5] |

**Return 7** ✓

### Complexity
- **Time O(n):** each index is added and removed at most once.
- **Space O(n)** for dp (it could be O(k) by storing only the dp values in the deque).

---

## 6. Comparing the approaches

| | Plain DP | DP + monotonic deque | DP + max-heap |
|---|---|---|---|
| Time | O(n · k) | **O(n)** | O(n log n) |
| Space | O(n) | O(n) | O(n) |

(A max-heap of (dp, index) with lazy removal of the out-of-range entries also works: O(n log n).)

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 1 (answer nums[0]), k ≥ n (you can jump anywhere), all negative values, k = 1 (you must step on everything).

**Mistakes:**
1. Expiring with `<=` instead of `<` (the index i − k is still reachable).
2. Storing dp values instead of indices (you can't tell when they expire).
3. Greedy "jump to the largest value", which fails because a path may need to step on negatives.

---

## 8. Interview questions and answers

**Q1. Why can dominated indices be removed?**
> If j < i and dp[j] ≤ dp[i], then for every future position, i is reachable whenever j is (i is newer) and gives at least as much. So j will never be the best choice.

**Q2. How do you recognise this pattern?**
> A DP whose transition is "max/min over a sliding window of previous states". That's the trigger for a monotonic deque (or a heap).

### Follow-up questions

**F1. Constrained Subsequence Sum (LC 1425).** → [H01](../04-Hard/H01_ConstrainedSubsequenceSum.md)
> `dp[i] = nums[i] + max(0, the max of the dp window)`: the same deque, but you can also start fresh.

**F2. The minimum cost to the end with k-step jumps.**
> The same idea with a min-deque.

**F3. Jump Game I/II (any reachable jump, the fewest jumps).** → [15-DP M05](../../15-Dynamic-Programming/03-Medium/M05_JumpGame.md), [M06](../../15-Dynamic-Programming/03-Medium/M06_JumpGameII.md)
> Greedy reach tracking, not a deque.

---

## 9. Running the file

```bash
java M06_JumpGameVI.java      # 4/4 tests passed (including 500 random cases vs the O(n·k) DP)
```

**One-line summary:** *"dp[i] = nums[i] + the max dp of the last k indices; keep that max with a decreasing deque of indices."*
