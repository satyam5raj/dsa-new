# M27 · Minimum Cost Tree From Leaf Values

| | |
|---|---|
| **LeetCode** | [#1130 Minimum Cost Tree From Leaf Values](https://leetcode.com/problems/minimum-cost-tree-from-leaf-values/) |
| **Difficulty** | Medium |
| **Pattern** | **Interval DP** (try every split), then a **monotonic-stack** greedy in O(n) |
| **Code** | [M27_MinimumCostTreeFromLeafValues.java](M27_MinimumCostTreeFromLeafValues.java) |
| **Run** | `java M27_MinimumCostTreeFromLeafValues.java` |

The interval-DP loop order is explained in [M24](M24_PalindromicSubstringsDP.md#31-the-interval-dp-loop-order); monotonic stacks are in [Stack M02 Daily Temperatures](../../04-Stack/03-Medium/M02_DailyTemperatures.md).

---

## 1. The problem

`arr` holds the leaves of a binary tree, in in-order. Every non-leaf node has exactly two children, and its value is (the largest leaf in its left subtree) × (the largest leaf in its right subtree). What's the minimum possible sum of the non-leaf nodes?

```
[6, 2, 4]:          24                    24
                   /  \                  /  \
                  6    8      vs.      12    4
                      / \             /  \
                     2   4           6    2
               cost 8 + 24 = 32    cost 12 + 24 = 36
```

| arr | answer |
|---|---|
| [6, 2, 4] | 32 |
| [4, 11] | 44 |

---

## 2. How to think about it

### Interval DP: choose the root's split
The root of a tree over leaves i..j splits them into a left part i..k and a right part k + 1..j.

| step | for this problem |
|---|---|
| **State** | dp[i][j] = the minimum cost of a tree over leaves i..j |
| **Transition** | dp[i][j] = min over k in [i, j) of dp[i][k] + dp[k + 1][j] + max(i..k) · max(k + 1..j) |
| **Base** | dp[i][i] = 0 (a single leaf has no internal node) |
| **Order** | by increasing interval length |
| **Answer** | dp[0][n − 1] |

Precompute mx[i][j] = max(arr[i..j]) so each split is O(1). Total **O(n³)**.

### Dry run: [6, 2, 4]

| interval | splits | dp |
|---|---|---|
| [6, 2] | 0 + 0 + 6·2 | 12 |
| [2, 4] | 0 + 0 + 2·4 | 8 |
| [6, 2, 4] | k = 0: 0 + 8 + 6·4 = 32; k = 1: 12 + 0 + 6·4 = 36 | **32** |

### The O(n) greedy
Each internal node "uses up" the smaller of two neighbouring values. The cheapest way to remove a leaf x is to pair it with the **smaller** of its two neighbours that are larger than it. A **decreasing stack** finds those neighbours:

```
for x in arr:
    while stack.top ≤ x:
        mid = pop
        cost += mid · min(new top, x)       mid is removed, paired with its cheaper bigger neighbour
    push x
finally, the stack is decreasing: pair each remaining element with the one below it
```

### Dry run: the stack on [6, 2, 4] (from a real run)

| x | action | stack (top first) | cost |
|---|---|---|---|
| 6 | push | 6, ∞ | 0 |
| 2 | push | 2, 6, ∞ | 0 |
| 4 | pop 2 (2 ≤ 4): 2 · min(6, 4) = 8; push 4 | 4, 6, ∞ | 8 |
| end | pop 4: 4 · 6 = 24 | 6, ∞ | **32** |

---

## 3. Java concepts used in this problem

### 3.1 A range-maximum table

```java
mx[i][i] = arr[i];
for (int j = i + 1; j < n; j++) mx[i][j] = Math.max(mx[i][j - 1], arr[j]);
```

Each row extends the previous maximum by one element: O(n²) to build, O(1) per lookup.

### 3.2 Looping by interval length

```java
for (int len = 2; len <= n; len++)
    for (int i = 0; i + len - 1 < n; i++) { int j = i + len - 1; ... }
```

Every interval of length `len` only depends on shorter intervals, which are already done.

### 3.3 A sentinel at the bottom of the stack

`st.push(Integer.MAX_VALUE);` means `st.peek()` is never empty inside the loop, and `min(st.peek(), x)` falls back to x when there's no real left neighbour. The final loop stops at `size() > 2` so the sentinel is never multiplied.

### 3.4 `Integer` from `peek()` compared with `<=`

`st.peek() <= x` unboxes the `Integer`, so it's a value comparison.

---

## 4. Approach 1: Recursion over every root split (the referee)

### Code ([M27_MinimumCostTreeFromLeafValues.java:73-83](M27_MinimumCostTreeFromLeafValues.java#L73-L83))

```java
static int brute(int[] a, int i, int j) {                         // plain recursion over every root split
    if (i == j) return 0;
    int best = Integer.MAX_VALUE;
    for (int k = i; k < j; k++) {
        int l = 0, r = 0;
        for (int x = i; x <= k; x++) l = Math.max(l, a[x]);
        for (int x = k + 1; x <= j; x++) r = Math.max(r, a[x]);
        best = Math.min(best, brute(a, i, k) + brute(a, k + 1, j) + l * r);
    }
    return best;
}
```

The recurrence without memo (Catalan-many trees). The test keeps n ≤ 9.

---

## 5. Approach 2: Interval DP (the file's main solution)

### Code ([M27_MinimumCostTreeFromLeafValues.java:41-56](M27_MinimumCostTreeFromLeafValues.java#L41-L56))

```java
static int mctFromLeafValues(int[] arr) {
    int n = arr.length;
    int[][] mx = new int[n][n], dp = new int[n][n];
    for (int i = 0; i < n; i++) {
        mx[i][i] = arr[i];
        for (int j = i + 1; j < n; j++) mx[i][j] = Math.max(mx[i][j - 1], arr[j]);
    }
    for (int len = 2; len <= n; len++)
        for (int i = 0; i + len - 1 < n; i++) {
            int j = i + len - 1;
            dp[i][j] = Integer.MAX_VALUE;
            for (int k = i; k < j; k++)                             // root splits into i..k | k+1..j
                dp[i][j] = Math.min(dp[i][j], dp[i][k] + dp[k + 1][j] + mx[i][k] * mx[k + 1][j]);
        }
    return dp[0][n - 1];
}
```

- **Time O(n³), space O(n²).**

---

## 6. Approach 3: Monotonic stack

### Code ([M27_MinimumCostTreeFromLeafValues.java:58-71](M27_MinimumCostTreeFromLeafValues.java#L58-L71))

```java
static int mctStack(int[] arr) {
    Deque<Integer> st = new ArrayDeque<>();
    st.push(Integer.MAX_VALUE);                                     // sentinel
    int cost = 0;
    for (int x : arr) {
        while (st.peek() <= x) {
            int mid = st.pop();                                     // remove mid, paired with its smaller bigger-neighbour
            cost += mid * Math.min(st.peek(), x);
        }
        st.push(x);
    }
    while (st.size() > 2) cost += st.pop() * st.peek();             // remaining stack is decreasing
    return cost;
}
```

- **Time O(n)** (each element is pushed and popped once), **space O(n).**

---

## 7. Comparing the approaches

| | Recursion | Interval DP | Stack |
|---|---|---|---|
| Time | exponential | O(n³) | **O(n)** |
| Generalises to other costs | yes | **yes** | only this cost structure |

The file checks all three agree on 400 random arrays.

---

## 8. Edge cases and common mistakes

**Edge cases:** two leaves (their product), increasing or decreasing arrays, equal values.

**Mistakes:**
1. Forgetting that a single leaf costs 0.
2. Recomputing the range maximum inside the k loop (O(n⁴)).
3. Multiplying the sentinel in the stack version.

---

## 9. Interview questions and answers

**Q1. Why is the root's split the right thing to choose?**
> The root's value depends only on the maximum of each side, and the two subtrees are independent problems on contiguous ranges, so choosing the split decomposes the problem.

**Q2. Why does the stack greedy work?**
> The smallest leaf should be combined first, and with its smaller neighbour: it never contributes to a larger product later. The stack processes leaves so that each one is removed when its larger neighbours are known.

### Follow-up questions

**F1. Burst Balloons.** → [H01](../04-Hard/H01_BurstBalloons.md)
> Interval DP choosing the **last** balloon in each range.

**F2. Matrix Chain Multiplication.**
> The same split DP: dp[i][j] = min over k of dp[i][k] + dp[k+1][j] + rows[i] · cols[k] · cols[j].

**F3. Daily Temperatures.** → [Stack M02](../../04-Stack/03-Medium/M02_DailyTemperatures.md)
> The same monotonic-stack "next greater" pattern.

---

## 10. Running the file

```bash
java M27_MinimumCostTreeFromLeafValues.java      # 4/4 tests passed (including 400 random: DP == stack == recursion)
```

**One-line summary:** *"dp[i][j] = min over the root's split k of dp[i][k] + dp[k+1][j] + max(left) · max(right); or, in O(n), remove each leaf with a decreasing stack, multiplying it by its smaller larger neighbour."*
