# H03 · Largest Rectangle in Histogram

| | |
|---|---|
| **LeetCode** | [#84 Largest Rectangle in Histogram](https://leetcode.com/problems/largest-rectangle-in-histogram/) |
| **Difficulty** | Hard |
| **Pattern** | Monotonic stack (increasing): the "nearest smaller element" |
| **Code** | [H03_LargestRectangleInHistogram.java](H03_LargestRectangleInHistogram.java) |
| **Run** | `java H03_LargestRectangleInHistogram.java` |

---

## 1. The problem

`heights[i]` is a bar of width 1. Return the area of the **largest rectangle** that fits inside the histogram.

```
heights = [2, 1, 5, 6, 2, 3]          answer = 10

         █
       ▓ ▓
       ▓ ▓
       ▓ ▓   █        ▓ = the best rectangle: height 5, width 2 (over bars 2 and 3)
   █   ▓ ▓ █ █
   █ █ ▓ ▓ █ █
   0 1 2 3 4 5
```

| heights | answer |
|---|---|
| `[2, 1, 5, 6, 2, 3]` | 10 |
| `[2, 4]` | 4 |
| `[3, 3, 3]` | 9 |
| `[1, 2, 3, 4, 5]` | 9 (height 3 × width 3, or 4 × 2 = 8, 5 × 1 = 5…) |
| `[0]` | 0 |

**Constraints:** `1 ≤ n ≤ 10⁵`, `0 ≤ heights[i] ≤ 10⁴`.

---

## 2. How to think about it

### The key question
The best rectangle's height equals **some bar's height** (otherwise you could make it taller). So for each bar i, ask:

> *"The rectangle whose height is exactly `heights[i]`: how far can it stretch?"*

It stretches left until the **nearest shorter bar on the left** (`L`), and right until the **nearest shorter bar on the right** (`R`):

```
width = R − L − 1        area = heights[i] × width
```

The answer is the maximum over all i.

### Computing "the nearest shorter bar" fast: a monotonic stack
- **Brute force:** for each i, walk left and right until a shorter bar: O(n²).
- **Monotonic stack:** scan left to right, keeping the stack's indices in **increasing** height order.
  - When the current bar `h` is **shorter** than the bar on top of the stack, that top bar has just found its **right boundary** (the current index i).
  - Its **left boundary** is the element **below it** in the stack (the stack is increasing, so the element below is the nearest shorter bar to its left).
  - So pop it and compute its area **right away**.
- At the end, process a **sentinel bar of height 0** so everything left in the stack gets popped.

---

## 3. Java concepts used in this problem

### 3.1 `Deque` as a stack: `push`, `pop`, `peek`

```java
Deque<Integer> stack = new ArrayDeque<>();
stack.push(i);       // add on top     (= addFirst)
stack.peek();        // read the top   (= peekFirst)
stack.pop();         // remove the top (= removeFirst; throws if empty)
```

A stack is **LIFO** (last in, first out). For a stack, `ArrayDeque` uses the **front** of the deque as the top. See H02 for the full Deque method table.

### 3.2 A sentinel via the ternary operator

```java
for (int i = 0; i <= n; i++) {                   // note: <= n, one extra iteration
    int h = (i == n) ? 0 : heights[i];           // a virtual bar of height 0 at the end
```

The extra virtual bar (height 0, shorter than every real bar) **flushes** the stack, so no special clean-up code is needed after the loop. **Sentinels** like this remove edge-case code in many algorithms (linked lists use dummy head nodes for the same reason).

### 3.3 Monotonic stack: the general tool

A **monotonic stack** keeps its elements in sorted order (increasing or decreasing) by popping anything that breaks the order before pushing. The moment an element is popped is the moment you learn its "**next smaller**" (for an increasing stack) or "**next greater**" (for a decreasing stack) element. The same tool solves Next Greater Element, Daily Temperatures, Stock Span and Trapping Rain Water. See [04-Stack](../../04-Stack/README.md).

---

## 4. Approach 1: Brute force (expand from every bar)

### Code ([H03_LargestRectangleInHistogram.java:72-81](H03_LargestRectangleInHistogram.java#L72-L81))

```java
static int largestRectangleBrute(int[] h) {
    int best = 0;
    for (int i = 0; i < h.length; i++) {
        int l = i, r = i;
        while (l > 0 && h[l - 1] >= h[i]) l--;
        while (r < h.length - 1 && h[r + 1] >= h[i]) r++;
        best = Math.max(best, h[i] * (r - l + 1));
    }
    return best;
}
```

| Line | What it does |
|---|---|
| `while (l > 0 && h[l - 1] >= h[i]) l--;` | extend left while the neighbour is at least as tall |
| `while (r < n − 1 && h[r + 1] >= h[i]) r++;` | extend right the same way |
| `h[i] * (r - l + 1)` | the rectangle with height `h[i]` over `[l, r]` |

**O(n²) worst case** (e.g. all bars equal). It's the reference for the random tests.

---

## 5. Approach 2: Nearest-smaller arrays (two stack passes)

Compute `left[i]` (the index of the nearest shorter bar on the left, or −1) and `right[i]` (the nearest shorter bar on the right, or n) separately, then combine:

```java
static int largestRectangleTwoPass(int[] h) {
    int n = h.length;
    int[] left = new int[n], right = new int[n];
    Deque<Integer> st = new ArrayDeque<>();
    for (int i = 0; i < n; i++) {
        while (!st.isEmpty() && h[st.peek()] >= h[i]) st.pop();
        left[i] = st.isEmpty() ? -1 : st.peek();
        st.push(i);
    }
    st.clear();
    for (int i = n - 1; i >= 0; i--) {
        while (!st.isEmpty() && h[st.peek()] >= h[i]) st.pop();
        right[i] = st.isEmpty() ? n : st.peek();
        st.push(i);
    }
    int best = 0;
    for (int i = 0; i < n; i++) best = Math.max(best, h[i] * (right[i] - left[i] - 1));
    return best;
}
```

**O(n) time, O(n) space.** It's easier to understand (each array answers one clear question), and it's the stepping stone to the one-pass version.

**Dry run** `[2, 1, 5, 6, 2, 3]`:

| i | 0 | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|---|
| h | 2 | 1 | 5 | 6 | 2 | 3 |
| left (nearest shorter on the left) | −1 | −1 | 1 | 2 | 1 | 4 |
| right (nearest shorter on the right) | 1 | 6 | 4 | 4 | 6 | 6 |
| width = right − left − 1 | 1 | 6 | 2 | 1 | 4 | 1 |
| area | 2 | 6 | **10** | 6 | 8 | 3 |

Max = **10** ✓

---

## 6. Approach 3: One-pass monotonic stack (the file's solution)

### Code ([H03_LargestRectangleInHistogram.java:55-70](H03_LargestRectangleInHistogram.java#L55-L70))

```java
static int largestRectangleArea(int[] heights) {
    int n = heights.length;
    Deque<Integer> stack = new ArrayDeque<>();           // indices, heights increasing
    int best = 0;
    for (int i = 0; i <= n; i++) {
        int h = (i == n) ? 0 : heights[i];               // sentinel 0 flushes the stack at the end
        while (!stack.isEmpty() && heights[stack.peek()] > h) {
            int height = heights[stack.pop()];           // this bar's rectangle ends here
            int left = stack.isEmpty() ? -1 : stack.peek();   // nearest shorter on the left
            int width = i - left - 1;                    // i = nearest shorter on the right
            best = Math.max(best, height * width);
        }
        stack.push(i);
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| `Deque<Integer> stack` | indices whose heights increase from bottom to top |
| `for (int i = 0; i <= n; i++)` | one extra iteration for the sentinel |
| `int h = (i == n) ? 0 : heights[i];` | the current height; 0 for the sentinel |
| `while (... heights[stack.peek()] > h)` | the top bar is taller than the current one, so the current index is the top's **right boundary** |
| `int height = heights[stack.pop()];` | the rectangle height = the popped bar's height |
| `int left = stack.isEmpty() ? -1 : stack.peek();` | the new top is the nearest shorter bar on the left; if the stack is empty, the rectangle extends all the way to the left edge (−1) |
| `int width = i - left - 1;` | the bars strictly between `left` and `i` |
| `best = Math.max(best, height * width);` | |
| `stack.push(i);` | the current bar waits for its own right boundary |

**Equal heights:** the condition is `>` (strictly taller), so equal bars stay on the stack. That's still correct: when an equal pair is eventually popped, the **later** one of them computes the full width (its left boundary reaches past the earlier equal bar, which is popped first). At least one of them always gets the correct area.

### Dry run: `[2, 1, 5, 6, 2, 3]` (+ the sentinel at index 6)

The stack is shown bottom → top as `index(height)`.

| i | h | pops (height × width) | best | stack after push |
|---|---|---|---|---|
| 0 | 2 | — | 0 | [0(2)] |
| 1 | 1 | pop 0(2): left = −1, width 1 − (−1) − 1 = 1 → **2** | 2 | [1(1)] |
| 2 | 5 | — | 2 | [1(1), 2(5)] |
| 3 | 6 | — | 2 | [1(1), 2(5), 3(6)] |
| 4 | 2 | pop 3(6): left = 2, width 4 − 2 − 1 = 1 → **6** | 6 | |
| | | pop 2(5): left = 1, width 4 − 1 − 1 = 2 → **10** | **10** | [1(1), 4(2)] |
| 5 | 3 | — | 10 | [1(1), 4(2), 5(3)] |
| 6 | 0 (sentinel) | pop 5(3): left = 4, width 1 → 3 | 10 | |
| | | pop 4(2): left = 1, width 6 − 1 − 1 = 4 → 8 | 10 | |
| | | pop 1(1): left = −1, width 6 → 6 | 10 | [6(0)] |

**Return 10** ✓

### Dry run 2: `[3, 3, 3]` (equal heights)

| i | h | pops | best |
|---|---|---|---|
| 0, 1, 2 | 3 | none (3 > 3 is false) | 0 |
| 3 | 0 | pop 2(3): left = 1, width 1 → 3; pop 1(3): left = 0, width 2 → 6; pop 0(3): left = −1, width 3 → **9** | **9** |

**Return 9** ✓. The **last** popped of the equal bars gets the full width.

### Dry run 3: `[1, 2, 3, 4, 5]` (increasing: everything waits for the sentinel)

At i = 5 (sentinel): pop 4(5): width 1 → 5; pop 3(4): width 2 → 8; pop 2(3): width 3 → **9**; pop 1(2): width 4 → 8; pop 0(1): width 5 → 5. **Return 9** ✓

### Complexity
- **Time O(n):** each index is pushed once and popped once.
- **Space O(n):** the stack (e.g. for increasing input).

---

## 7. (Bonus) Divide and conquer

The largest rectangle either uses the **minimum bar** of the range (then its width is the whole range), or lies entirely to the left or to the right of it. Recurse on both sides. **O(n log n) on average, O(n²) worst case** (sorted input), unless you find the minimum with a segment tree. It's elegant, but the stack is better.

---

## 8. Comparing the approaches

| | Brute | Two stack passes | One-pass stack | Divide & conquer |
|---|---|---|---|---|
| Time | O(n²) | O(n) | **O(n)** | O(n log n) avg |
| Space | O(1) | O(n) | O(n) | O(log n)–O(n) |
| Easiest to explain | ✓ | **✓** | needs care | medium |

---

## 9. Edge cases and common mistakes

**Edge cases:** a single bar, all equal, strictly increasing/decreasing, zeros (they split the histogram), a very tall single bar.

**Mistakes:**
1. Forgetting the sentinel (or a final flush loop), so the bars left in the stack are never evaluated.
2. Computing the width as `i − popped` instead of `i − newTop − 1`. The rectangle extends **left past the popped index**, back to the new top.
3. Using `left = 0` instead of −1 when the stack becomes empty.
4. Overflow: `height × width` ≤ 10⁴ × 10⁵ = 10⁹ fits in an int, but bigger constraints would need `long`.

---

## 10. Interview questions and answers

**Q1. Why does the optimal rectangle's height equal some bar's height?**
> If a rectangle's height were below every bar in its range, you could raise it to the shortest bar in that range and get a bigger area. So the optimum is "pinned" by its shortest bar.

**Q2. When a bar is popped, why is the new top its left boundary?**
> The stack is increasing, so the element below the popped bar is shorter than it. Any bars that were between them were popped earlier, which means they were taller. So the element below is the **nearest** shorter bar on the left.

**Q3. Why the sentinel?**
> Bars that never meet a shorter bar on their right stay on the stack. A height-0 bar at index n is shorter than every bar, so it pops them all with n as their right boundary.

### Follow-up questions

**F1. Maximal Rectangle in a 0/1 matrix (LC 85).** → [04-Stack H02](../../04-Stack/04-Hard/H02_MaximalRectangle.md)
> For each row, build a histogram (`h[j] = h[j] + 1` if the cell is 1, else 0) and run this algorithm. O(rows × cols).

**F2. The maximal square of 1s (LC 221).**
> DP: `dp[i][j] = 1 + min(top, left, top-left)`. Simpler than rectangles.

**F3. Count the submatrices of all 1s (LC 1504).**
> Histograms per row + a monotonic stack that counts the rectangles ending at each column.

**F4. Sum of subarray minimums (LC 907).** → [04-Stack M15](../../04-Stack/03-Medium/M15_SumOfSubarrayMinimums.md)
> The same "nearest smaller on the left/right" idea: each element is the minimum of `(i − L) × (R − i)` subarrays.

**F5. Next greater element / daily temperatures.** → [04-Stack M02](../../04-Stack/03-Medium/M02_DailyTemperatures.md)
> A decreasing monotonic stack; pop when a warmer day arrives.

---

## 11. Running the file

```bash
java H03_LargestRectangleInHistogram.java      # 6/6 tests passed (including 300 random cross-checks)
```

**One-line summary:** *"Increasing stack of indices; when a shorter bar arrives, pop: height = popped bar, width = i − newTop − 1; a 0 sentinel flushes the rest."*
