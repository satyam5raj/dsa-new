# H04 · Shortest Subarray with Sum at Least K

| | |
|---|---|
| **LeetCode** | [#862 Shortest Subarray with Sum at Least K](https://leetcode.com/problems/shortest-subarray-with-sum-at-least-k/) |
| **Difficulty** | Hard |
| **Pattern** | Prefix sums + monotonic deque |
| **Code** | [H04_ShortestSubarrayWithSumAtLeastK.java](H04_ShortestSubarrayWithSumAtLeastK.java) |
| **Run** | `java H04_ShortestSubarrayWithSumAtLeastK.java` |

---

## 1. The problem

Return the length of the **shortest non-empty subarray** whose sum is **≥ k**, or `-1` if there's none. Values **can be negative**.

| nums | k | answer | subarray |
|---|---|---|---|
| `[1]` | 1 | 1 | `[1]` |
| `[1, 2]` | 4 | −1 | impossible |
| `[2, -1, 2]` | 3 | 3 | the whole array |
| `[84, -37, 32, 40, 95]` | 167 | 3 | `[32, 40, 95]` = 167 |

**Constraints:** `1 ≤ n ≤ 10⁵`, `-10⁵ ≤ nums[i] ≤ 10⁵`, `1 ≤ k ≤ 10⁹`.

This is [M11 Minimum Size Subarray Sum](../03-Medium/M11_MinimumSizeSubarraySum.md) **with negative numbers allowed**, and that one change makes it Hard.

---

## 2. How to think about it

### Why M11's sliding window fails
With negatives, **shrinking the window can increase the sum** (for example, dropping a −37), and growing it can decrease the sum. The "valid / invalid" boundary isn't monotonic anymore, so the rule "shrink while valid" can stop too early or too late.

### Reframe with prefix sums
`P[0] = 0`, and `P[j]` = the sum of the first j elements. The subarray `nums[i..j−1]` has sum `P[j] − P[i]`. We want:

```
the minimum (j − i)   such that   P[j] − P[i] ≥ k,   with i < j
```

So for each **end** j, we want the **latest** start i (the shortest subarray) with `P[i] ≤ P[j] − k`.

### A monotonic deque of candidate starts
Keep a deque of start indices whose **prefix values increase** from front to back. For each j:

1. **While `P[j] − P[front] ≥ k`:** we found a valid subarray `front..j−1`. Record `j − front`, then **pop the front**.
   *Why can we pop it?* Any later end j′ > j would give a **longer** subarray with this same start. This start can never do better than right now.
2. **While `P[back] ≥ P[j]`: pop the back.**
   *Why?* j is a **later** start with a **smaller or equal** prefix. For any future end, starting at j gives a sum that's at least as big **and** a subarray that's shorter. The old start is dominated and useless.
3. **Push j** as a candidate start for future ends.

Because of rule 2, the prefix values in the deque are increasing, so the front has the **smallest** prefix, the best chance to reach `P[j] − P[i] ≥ k`.

---

## 3. Java concepts used in this problem

### 3.1 `long[]` for prefix sums

```java
long[] prefix = new long[n + 1];
```

The worst prefix is 10⁵ × 10⁵ = 10¹⁰, which overflows an `int` (max about 2.1 × 10⁹). Use `long` for running sums whenever n × max|value| could exceed about 2 × 10⁹.

### 3.2 `best = Math.min(best, j - dq.pollFirst());`

`pollFirst()` removes **and returns** the front index (unboxed to an int here), so one line records the length and pops. It's compact, but make sure you understand that the pop happens.

### 3.3 Domination: a key idea in monotonic structures

"Element A is **dominated** by B if B is at least as good in every way for every future query." Dominated elements can be thrown away, and that's what keeps monotonic stacks and deques small and makes them O(n). In H02, older smaller values were dominated by a newer larger one; here, older larger prefixes are dominated by a newer smaller one.

---

## 4. Approach 1: Brute force

### Code ([H04_ShortestSubarrayWithSumAtLeastK.java:71-81](H04_ShortestSubarrayWithSumAtLeastK.java#L71-L81))

```java
static int shortestBrute(int[] a, int k) {
    int best = Integer.MAX_VALUE;
    for (int i = 0; i < a.length; i++) {
        long s = 0;
        for (int j = i; j < a.length; j++) {
            s += a[j];
            if (s >= k) { best = Math.min(best, j - i + 1); break; }
        }
    }
    return best == Integer.MAX_VALUE ? -1 : best;
}
```

The `break` is safe **for a fixed start**: once the sum is ≥ k, any longer subarray with the same start is longer, so it's not "shorter". (Unlike M11, we can't move the start forward cleverly; we just try every start.) **O(n²)**. It's the reference for 500 random tests.

---

## 5. Approach 2: Prefix sums + a sorted structure, O(n log n)

An intermediate idea: for each j, we need the **largest** i < j with `P[i] ≤ P[j] − k`. A `TreeMap<Long, Integer>` from prefix value to the latest index can answer "the largest key ≤ x" with `floorKey`, but "the latest index among all keys ≤ x" needs extra bookkeeping. That's why this approach is rarely used; the monotonic deque solves it more simply and in O(n). It's worth mentioning to show you considered it.

---

## 6. Approach 3: Monotonic deque (the file's solution)

### Code ([H04_ShortestSubarrayWithSumAtLeastK.java:52-69](H04_ShortestSubarrayWithSumAtLeastK.java#L52-L69))

```java
static int shortestSubarray(int[] nums, int k) {
    int n = nums.length;
    long[] prefix = new long[n + 1];
    for (int i = 0; i < n; i++) prefix[i + 1] = prefix[i] + nums[i];

    Deque<Integer> dq = new ArrayDeque<>();           // candidate starts, prefix increasing
    int best = Integer.MAX_VALUE;
    for (int j = 0; j <= n; j++) {
        while (!dq.isEmpty() && prefix[j] - prefix[dq.peekFirst()] >= k) {
            best = Math.min(best, j - dq.pollFirst());           // 1. found; this start is done
        }
        while (!dq.isEmpty() && prefix[dq.peekLast()] >= prefix[j]) {
            dq.pollLast();                                       // 2. j dominates it
        }
        dq.offerLast(j);                                         // 3.
    }
    return best == Integer.MAX_VALUE ? -1 : best;
}
```

### Line by line

| Line | What it does |
|---|---|
| `prefix[i + 1] = prefix[i] + nums[i];` | the prefix sums; `prefix[0] = 0` is the empty prefix |
| `for (int j = 0; j <= n; j++)` | j goes over all **prefix indices** (0..n). Each j acts first as an **end**, then becomes a **start** candidate |
| first `while` | from the front (the smallest prefixes): as long as the subarray `front..j−1` has sum ≥ k, record its length and drop that start |
| second `while` | from the back: drop the starts whose prefix ≥ `P[j]` (dominated by j) |
| `dq.offerLast(j);` | j becomes a candidate start |
| `return best == MAX ? -1 : best;` | −1 if nothing was found |

### Dry run 1: `[2, -1, 2]`, k = 3

`P = [0, 2, 1, 3]`

| j | P[j] | step 1 (front) | step 2 (back) | deque after |
|---|---|---|---|---|
| 0 | 0 | — | — | [0] |
| 1 | 2 | 2 − P[0] = 2 < 3 | P[0] = 0 < 2, keep | [0, 1] |
| 2 | 1 | 1 − 0 = 1 < 3 | P[1] = 2 ≥ 1 → **pop 1** | [0, 2] |
| 3 | 3 | 3 − P[0] = 3 ≥ 3 → **length 3**, pop 0; 3 − P[2] = 2 < 3 | P[2] = 1 < 3, keep | [2, 3] |

**Return 3** ✓. At j = 2, start 1 (prefix 2) was dominated by start 2 (prefix 1): later and smaller.

### Dry run 2: `[84, -37, 32, 40, 95]`, k = 167

`P = [0, 84, 47, 79, 119, 214]`

| j | P[j] | step 1 | step 2 | deque after | best |
|---|---|---|---|---|---|
| 0 | 0 | — | — | [0] | ∞ |
| 1 | 84 | 84 − 0 < 167 | — | [0, 1] | ∞ |
| 2 | 47 | 47 < 167 | P[1] = 84 ≥ 47 → **pop 1** | [0, 2] | ∞ |
| 3 | 79 | 79 < 167 | — | [0, 2, 3] | ∞ |
| 4 | 119 | 119 < 167 | — | [0, 2, 3, 4] | ∞ |
| 5 | 214 | 214 − 0 = 214 ≥ 167 → length 5, pop 0; 214 − P[2] = 167 ≥ 167 → **length 3**, pop 2; 214 − 79 = 135 < 167 stop | — | [3, 4, 5] | **3** |

**Return 3** ✓ (start 2, end 5 → `nums[2..4] = [32, 40, 95]`).

Notice how popping start 1 at j = 2 mattered: start 2 (after the −37) gives a **shorter** subarray with a **bigger** sum.

### Dry run 3: `[1, 2]`, k = 4
`P = [0, 1, 3]`. No difference ever reaches 4 → **return −1** ✓

### Complexity
- **Time O(n):** each index is pushed once and popped at most once.
- **Space O(n):** the prefix array and the deque.

---

## 7. Comparing the approaches

| | Brute | TreeMap idea | Monotonic deque | M11 sliding window |
|---|---|---|---|---|
| Time | O(n²) | O(n log n) | **O(n)** | O(n) |
| Space | O(1) | O(n) | O(n) | O(1) |
| Handles negatives | yes | yes | **yes** | **no** |

---

## 8. Edge cases and common mistakes

**Edge cases:** no valid subarray (−1), a single element ≥ k, all negative, a huge k, the answer being the whole array.

**Mistakes:**
1. Using M11's sliding window: it gives wrong answers with negatives.
2. Using `int` prefix sums: overflow.
3. Using `if` instead of `while` in step 1, which misses shorter subarrays from later starts in the same step.
4. Forgetting step 2 (the domination pop), which breaks the increasing order, and then step 1's early stop is no longer valid.
5. Looping j only up to n − 1. The prefix indices go up to **n**.

---

## 9. Interview questions and answers

**Q1. Why can we pop the front after recording?**
> For that start i, the current j is the earliest end that makes the sum ≥ k. Any later end gives a longer subarray, so start i can't improve on what we just recorded.

**Q2. Why can we pop back elements with `P[back] ≥ P[j]`?**
> Start j comes later and has a prefix that's no larger. For any future end, using j as the start gives a sum that's at least as large and a length that's strictly shorter, so the old start is never better.

**Q3. Why does the deque stay increasing, and why does that matter?**
> Rule 2 removes anything ≥ P[j] before pushing j. Increasing order means that if the front fails the test `P[j] − P[front] ≥ k`, every other element (with a larger prefix) fails too, so step 1 can stop at the first failure.

### Follow-up questions

**F1. All values positive.** → [M11](../03-Medium/M11_MinimumSizeSubarraySum.md)
> A simple sliding window, O(1) space.

**F2. The longest subarray with sum ≥ k (with negatives).**
> For each j, find the **earliest** i with `P[i] ≤ P[j] − k`. Keep a **decreasing**-prefix stack of candidate starts (only new minimums), and for each j binary-search it, or process j from right to left with a stack. O(n log n) or O(n).

**F3. The longest subarray with sum exactly k (with negatives).**
> Prefix sums + a HashMap of the **first** index of each prefix ([M07](../03-Medium/M07_SubarraySumEqualsK.md) F1).

**F4. Constrained Subsequence Sum (LC 1425) / Jump Game VI.** → [05-Queue H01](../../05-Queue/04-Hard/H01_ConstrainedSubsequenceSum.md)
> DP with a monotonic deque that keeps the best dp value in a sliding window, the same "drop dominated candidates" idea.

---

## 10. Running the file

```bash
java H04_ShortestSubarrayWithSumAtLeastK.java      # 5/5 tests passed (including 500 random cross-checks)
```

**One-line summary:** *"Prefix sums + a deque of starts with increasing prefixes: pop the front while P[j] − P[front] ≥ k (record), pop the back while P[back] ≥ P[j]."*
