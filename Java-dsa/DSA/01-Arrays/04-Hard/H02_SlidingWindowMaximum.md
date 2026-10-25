# H02 · Sliding Window Maximum

| | |
|---|---|
| **LeetCode** | [#239 Sliding Window Maximum](https://leetcode.com/problems/sliding-window-maximum/) |
| **Difficulty** | Hard |
| **Pattern** | Monotonic deque (decreasing) |
| **Code** | [H02_SlidingWindowMaximum.java](H02_SlidingWindowMaximum.java) |
| **Run** | `java H02_SlidingWindowMaximum.java` |

---

## 1. The problem

A window of size `k` slides from left to right over `nums`, one step at a time. Return the **maximum** of each window.

```
nums = [1, 3, -1, -3, 5, 3, 6, 7], k = 3

[1  3  -1] -3  5  3  6  7     max 3
 1 [3  -1  -3] 5  3  6  7     max 3
 1  3 [-1  -3  5] 3  6  7     max 5
 1  3  -1 [-3  5  3] 6  7     max 5
 1  3  -1  -3 [5  3  6] 7     max 6
 1  3  -1  -3  5 [3  6  7]    max 7
                       →  [3, 3, 5, 5, 6, 7]
```

| nums | k | answer |
|---|---|---|
| `[1,3,-1,-3,5,3,6,7]` | 3 | `[3,3,5,5,6,7]` |
| `[1]` | 1 | `[1]` |
| `[9,8,7,6]` | 2 | `[9,8,7]` |
| `[1,2,3,4]` | 4 | `[4]` |

**Constraints:** `1 ≤ n ≤ 10⁵`, `1 ≤ k ≤ n`. There are n − k + 1 windows.

---

## 2. How to think about it

1. **Brute force:** scan each window: O(n·k). With n = 10⁵ and k = 5 × 10⁴ that's 2.5 × 10⁹, which is too slow.
2. **Why not a running variable?** A running **sum** slides easily (add one, subtract one, as in [E23](../02-Easy/E23_MaximumAverageSubarrayI.md)). A running **max** doesn't: when the maximum leaves the window, what's the **next** maximum? You'd need to remember the candidates.
3. **Max-heap with lazy deletion:** O(n log n). Good, but O(n) is possible.
4. **Monotonic deque (the optimal solution):** keep a deque of **indices** whose values are **decreasing** from front to back. The front is always the current window's maximum.

### The deque rules, for each new index i
1. **Pop from the FRONT** if that index has left the window (`index ≤ i − k`).
2. **Pop from the BACK** while `nums[back] ≤ nums[i]`.
   *Why?* Those elements are **smaller and older** than `nums[i]`. As long as `nums[i]` is in the window, they can **never** be the maximum, and they'll leave the window **before** it does. They're useless forever, so throw them away.
3. **Push i** at the back.
4. Once `i ≥ k − 1` (the first full window exists), **record `nums[front]`**.

The deque holds exactly the elements that could still become the maximum of some future window.

---

## 3. Java concepts used in this problem

### 3.1 `Deque` and `ArrayDeque`: a double-ended queue

A **deque** ("deck") lets you add and remove at **both ends** in O(1).

```java
Deque<Integer> dq = new ArrayDeque<>();
```

| Method | Meaning | Also called |
|---|---|---|
| `offerLast(x)` | add at the back | `addLast`, `offer`, `add` |
| `offerFirst(x)` | add at the front | `addFirst`, `push` |
| `pollFirst()` | remove the front (null if empty) | `poll`, `pop`/`removeFirst` (those throw if empty) |
| `pollLast()` | remove the back | `removeLast` |
| `peekFirst()` | look at the front without removing it | `peek` |
| `peekLast()` | look at the back | |
| `isEmpty()`, `size()` | | |

- `ArrayDeque` is a **circular array** underneath (it grows by doubling), so all these operations are amortised O(1).
- It's the recommended class for **both** stacks and queues in Java (faster than `Stack` and `LinkedList`).
- It doesn't allow `null` elements.

### 3.2 Why store indices, not values?

We need to know **when an element leaves the window** (step 1). Values alone can't tell you that, especially with duplicates. With an index, the value is always `nums[index]`.

### 3.3 `dq.peekFirst() <= i - k` compares `Integer` with `int`

`peekFirst()` returns an `Integer`; it's unboxed for the comparison with `i − k`. Comparisons with primitives are safe. (Don't compare two `Integer`s with `==`.)

### 3.4 `PriorityQueue` (approach 2)

```java
PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> b[0] - a[0]);   // max-heap by a[0]
pq.offer(new int[]{value, index});
pq.peek();   // the largest
pq.poll();   // remove the largest
```

A binary heap: `offer` and `poll` are O(log n), `peek` is O(1). By default it's a **min-heap**; the comparator `(a, b) -> b[0] − a[0]` reverses it into a max-heap. (`b[0] − a[0]` can overflow for extreme values; `Integer.compare(b[0], a[0])` is the safe form.) Heaps are covered in depth in [08-Heap](../../08-Heap/README.md).

---

## 4. Approach 1: Brute force

### Code ([H02_SlidingWindowMaximum.java:76-84](H02_SlidingWindowMaximum.java#L76-L84))

```java
static int[] maxSlidingWindowBrute(int[] nums, int k) {
    int[] r = new int[nums.length - k + 1];
    for (int i = 0; i + k <= nums.length; i++) {
        int m = Integer.MIN_VALUE;
        for (int j = i; j < i + k; j++) m = Math.max(m, nums[j]);
        r[i] = m;
    }
    return r;
}
```

**O(n·k) time.** It's the reference for the random tests.

---

## 5. Approach 2: Max-heap with lazy deletion, O(n log n)

```java
static int[] maxSlidingWindowHeap(int[] nums, int k) {
    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(b[0], a[0]));
    int[] res = new int[nums.length - k + 1];
    for (int i = 0; i < nums.length; i++) {
        pq.offer(new int[]{nums[i], i});
        while (pq.peek()[1] <= i - k) pq.poll();          // the top is outside the window → discard it
        if (i >= k - 1) res[i - k + 1] = pq.peek()[0];
    }
    return res;
}
```

- **Lazy deletion:** we don't remove an element when it leaves the window (a heap can't remove arbitrary elements quickly). Instead, we discard it only when it reaches the **top** and turns out to be expired.
- **O(n log n) time, O(n) space.** It's a solid fallback if you don't remember the deque trick.

---

## 6. Approach 3: Monotonic deque (the file's solution)

### Code ([H02_SlidingWindowMaximum.java:56-74](H02_SlidingWindowMaximum.java#L56-L74))

```java
static int[] maxSlidingWindow(int[] nums, int k) {
    int n = nums.length;
    int[] result = new int[n - k + 1];
    Deque<Integer> dq = new ArrayDeque<>();              // indices, values decreasing

    for (int i = 0; i < n; i++) {
        if (!dq.isEmpty() && dq.peekFirst() <= i - k) {
            dq.pollFirst();                              // 1. out of the window
        }
        while (!dq.isEmpty() && nums[dq.peekLast()] <= nums[i]) {
            dq.pollLast();                               // 2. useless forever
        }
        dq.offerLast(i);                                 // 3.
        if (i >= k - 1) {
            result[i - k + 1] = nums[dq.peekFirst()];    // 4. the front holds the max
        }
    }
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int[] result = new int[n - k + 1];` | one answer per window |
| `Deque<Integer> dq` | indices; `nums` at those indices decreases from front to back |
| `if (... dq.peekFirst() <= i - k) dq.pollFirst();` | the window is now `[i−k+1 .. i]`; an index ≤ i−k has left. An `if` is enough, because at most **one** index expires per step (the deque is sorted by index, and only the oldest can expire) |
| `while (... nums[dq.peekLast()] <= nums[i]) dq.pollLast();` | remove the smaller-or-equal older elements from the back |
| `dq.offerLast(i);` | the new element is a candidate for future windows |
| `if (i >= k - 1) result[i - k + 1] = nums[dq.peekFirst()];` | from the first full window on, the front is the max. The window starting at `i−k+1` is stored at that position |

**Why `<=` in step 2 and not `<`?** For equal values, the **newer** one is at least as good (it stays in the window longer), so the older one can go. Using `<` would also be correct, but it keeps duplicates around.

### Dry run: `nums = [1, 3, -1, -3, 5, 3, 6, 7]`, k = 3

The deque is shown as `index(value)`, front on the left.

| i | nums[i] | step 1 (expire front) | step 2 (pop back while ≤) | deque after push | window | max |
|---|---|---|---|---|---|---|
| 0 | 1 | — | — | [0(1)] | not full | |
| 1 | 3 | — | pop 0(1) | [1(3)] | not full | |
| 2 | −1 | — | — | [1(3), 2(−1)] | [0..2] | **3** |
| 3 | −3 | 1 ≤ 0? no | — | [1(3), 2(−1), 3(−3)] | [1..3] | **3** |
| 4 | 5 | 1 ≤ 1? **yes** → pop 1(3) | pop 3(−3), pop 2(−1) | [4(5)] | [2..4] | **5** |
| 5 | 3 | — | — | [4(5), 5(3)] | [3..5] | **5** |
| 6 | 6 | 4 ≤ 3? no | pop 5(3), pop 4(5) | [6(6)] | [4..6] | **6** |
| 7 | 7 | — | pop 6(6) | [7(7)] | [5..7] | **7** |

**Result** `[3, 3, 5, 5, 6, 7]` ✓

### Dry run 2: `[9, 8, 7, 6]`, k = 2 (decreasing: nothing is ever popped from the back)

| i | value | expire | deque | max |
|---|---|---|---|---|
| 0 | 9 | — | [0(9)] | |
| 1 | 8 | — | [0(9), 1(8)] | **9** |
| 2 | 7 | 0 ≤ 0 → pop 0 | [1(8), 2(7)] | **8** |
| 3 | 6 | 1 ≤ 1 → pop 1 | [2(7), 3(6)] | **7** |

**Result** `[9, 8, 7]` ✓. Here the **front expiry** does all the work.

### Complexity
- **Time O(n):** each index is pushed once and popped at most once (from the front or the back). The inner `while` is "paid for" by earlier pushes (amortised).
- **Space O(k):** the deque holds at most k indices.

---

## 7. (Bonus) Approach 4: Block prefix/suffix maxima, O(n) without a deque

Split the array into blocks of size k. Compute, inside each block, the **prefix max from the left** and the **suffix max from the right**. Any window `[i, i+k−1]` spans at most two blocks, so its max is `max(suffixMax[i], prefixMax[i+k−1])`.

```java
static int[] maxSlidingWindowBlocks(int[] nums, int k) {
    int n = nums.length;
    int[] pre = new int[n], suf = new int[n];
    for (int i = 0; i < n; i++)
        pre[i] = (i % k == 0) ? nums[i] : Math.max(pre[i - 1], nums[i]);       // restart at each block start
    for (int i = n - 1; i >= 0; i--)
        suf[i] = (i == n - 1 || (i + 1) % k == 0) ? nums[i] : Math.max(suf[i + 1], nums[i]);
    int[] res = new int[n - k + 1];
    for (int i = 0; i + k <= n; i++) res[i] = Math.max(suf[i], pre[i + k - 1]);
    return res;
}
```

It's O(n) time and O(n) space. It's a clever alternative and the same prefix/suffix idea as Trapping Rain Water.

---

## 8. Comparing the approaches

| | Brute | Heap (lazy) | Monotonic deque | Blocks |
|---|---|---|---|---|
| Time | O(n·k) | O(n log n) | **O(n)** | O(n) |
| Extra space | O(1) | O(n) | **O(k)** | O(n) |
| Works on a stream | yes | yes | **yes** | no (needs the whole array) |

---

## 9. Edge cases and common mistakes

**Edge cases:** k = 1 (the output equals the input), k = n (one window), decreasing or increasing arrays, duplicates, negative numbers.

**Mistakes:**
1. Storing **values** in the deque, which makes it impossible to tell when an element expires.
2. The expiry check `< i − k` instead of `<= i − k` (off by one).
3. Recording a result before the first window is full (`i < k − 1`).
4. Popping from the wrong end (smaller elements go out at the **back**, expired ones at the **front**).

---

## 10. Interview questions and answers

**Q1. What invariant does the deque maintain?**
> It holds the indices of the current window, in increasing index order, with strictly decreasing values. So the front is the window's maximum, and every element in it could still be the maximum of a future window.

**Q2. Why is it O(n) when there's a while loop inside the for loop?**
> Each index enters the deque once and leaves at most once. So the total work of all the pops across the whole run is O(n) (amortised).

**Q3. Why can we discard smaller elements behind a new larger element?**
> They're older and smaller. Every future window that contains them also contains the new element (which leaves later), so they can never be the maximum again.

### Follow-up questions

**F1. Sliding window minimum.**
> Keep the deque **increasing** (pop from the back while `nums[back] ≥ nums[i]`).

**F2. Longest subarray where max − min ≤ limit (LC 1438).** → [05-Queue M07](../../05-Queue/03-Medium/M07_LongestSubarrayAbsDiffLimit.md)
> A variable window with two deques (one for the max, one for the min); shrink while `max − min > limit`.

**F3. Shortest subarray with sum ≥ k, with negative numbers (LC 862).** → [H04](H04_ShortestSubarrayWithSumAtLeastK.md)
> A monotonic deque over the **prefix sums**.

**F4. DP with a sliding-window max, e.g. Jump Game VI (LC 1696).** → [05-Queue M06](../../05-Queue/03-Medium/M06_JumpGameVI.md)
> `dp[i] = nums[i] + max(dp[i−k..i−1])`; keep that max with a monotonic deque, which gives O(n) instead of O(nk).

**F5. Sliding window median (LC 480).** → [08-Heap H04](../../08-Heap/04-Hard/H04_SlidingWindowMedian.md)
> Two heaps (a max-heap for the lower half, a min-heap for the upper half) with lazy deletion, or a balanced BST/TreeMap.

---

## 11. Running the file

```bash
java H02_SlidingWindowMaximum.java      # 5/5 tests passed (including 300 random cross-checks)
```

**One-line summary:** *"Deque of indices with decreasing values: expire the front, pop smaller ones at the back, the front is the max."*
