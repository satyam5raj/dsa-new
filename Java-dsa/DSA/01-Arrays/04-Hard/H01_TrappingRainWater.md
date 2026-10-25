# H01 · Trapping Rain Water

| | |
|---|---|
| **LeetCode** | [#42 Trapping Rain Water](https://leetcode.com/problems/trapping-rain-water/) |
| **Difficulty** | Hard |
| **Pattern** | Prefix max / suffix max → two pointers (O(1) space) |
| **Code** | [H01_TrappingRainWater.java](H01_TrappingRainWater.java) |
| **Run** | `java H01_TrappingRainWater.java` |

---

## 1. The problem

`height[i]` is the height of a bar of width 1. After it rains, how much water is **trapped** between the bars?

```
height = [0,1,0,2,1,0,1,3,2,1,2,1]            answer = 6

                 █
         █ ~ ~ ~ █ █ ~ █             █ = bar
     █ ~ █ █ ~ █ █ █ █ █ █           ~ = trapped water
   0 1 2 3 4 5 6 7 8 9 10 11
```

| height | answer |
|---|---|
| `[0,1,0,2,1,0,1,3,2,1,2,1]` | 6 |
| `[4, 2, 0, 3, 2, 5]` | 9 |
| `[1, 2, 3]` | 0 (a staircase holds nothing) |
| `[]` | 0 |

**Constraints:** `1 ≤ n ≤ 2 × 10⁴`, `0 ≤ height[i] ≤ 10⁵`.

This is a **classic hard interview problem**, and it brings together prefix maxima, two pointers and greedy reasoning.

---

## 2. How to think about it

### Think about ONE column at a time (the core formula)
How high does the water rise above column i? It's held in by the **tallest wall on its left** and the **tallest wall on its right**, and it can only rise to the **lower** of the two (water spills over the shorter side, like a bucket):

```
water[i] = min(maxLeft(i), maxRight(i)) − height[i]
```

where `maxLeft(i)` = max of `height[0..i]` and `maxRight(i)` = max of `height[i..n−1]` (both include i itself, so the result is never negative).

The total is the sum over all columns. Everything else is about computing these maxima **efficiently**:

| Approach | How you get maxLeft and maxRight | Time | Space |
|---|---|---|---|
| Brute force | scan left and right for every i | O(n²) | O(1) |
| Precompute | two arrays: prefix max and suffix max | O(n) | O(n) |
| **Two pointers** | only the running maxima, with a clever argument | **O(n)** | **O(1)** |
| Monotonic stack | fill water layer by layer | O(n) | O(n) |

### The two-pointer insight
Keep `left` and `right` pointers with their running maxima, `leftMax` (the max of everything left of and including `left`) and `rightMax` (the same from the right).

If `leftMax < rightMax`, then the water at `left` is decided by `leftMax` **alone**. We don't know the exact `maxRight(left)`, but we know it's **at least `rightMax`** (because some bar of height `rightMax` sits to the right of `left`), and that's already bigger than `leftMax`. So `min(maxLeft, maxRight) = leftMax`. Add `leftMax − height[left]` and move `left++`. Otherwise the same argument works from the right side.

---

## 3. Java concepts used in this problem

### 3.1 Prefix maximum and suffix maximum arrays

```java
leftMax[0] = height[0];
for (int i = 1; i < n; i++) leftMax[i] = Math.max(leftMax[i - 1], height[i]);     // left → right
rightMax[n - 1] = height[n - 1];
for (int i = n - 2; i >= 0; i--) rightMax[i] = Math.max(rightMax[i + 1], height[i]);  // right → left
```

These are exactly like prefix **sums** (E03), with `max` instead of `+`. Any operation that "accumulates" (sum, product, max, min, gcd, OR…) can be prefixed this way.

### 3.2 Testing against a brute force with random inputs

As in M04 and M07, `main` checks 300 random arrays against `trapBrute`. With a subtle greedy argument like the two-pointer one, this is how you gain confidence.

### 3.3 The stack in approach 4: `Deque` as a stack

```java
Deque<Integer> st = new ArrayDeque<>();
st.push(i);        // put on top
st.peek();         // look at the top
st.pop();          // remove the top
st.isEmpty();
```

`ArrayDeque` is the recommended stack in Java. The old `Stack` class is synchronised and slower. More in H03.

---

## 4. Approach 1: Brute force

### Code ([H01_TrappingRainWater.java:89-98](H01_TrappingRainWater.java#L89-L98))

```java
static int trapBrute(int[] h) {
    int water = 0;
    for (int i = 0; i < h.length; i++) {
        int l = 0, r = 0;
        for (int j = 0; j <= i; j++) l = Math.max(l, h[j]);
        for (int j = i; j < h.length; j++) r = Math.max(r, h[j]);
        water += Math.min(l, r) - h[i];
    }
    return water;
}
```

| Line | What it does |
|---|---|
| `l` loop | the tallest bar in `[0..i]` (including i) |
| `r` loop | the tallest bar in `[i..n−1]` |
| `water += Math.min(l, r) - h[i];` | the column formula. Never negative, because l ≥ h[i] and r ≥ h[i] |

**O(n²) time, O(1) space.** It's the reference implementation for the tests.

---

## 5. Approach 2: Precomputed prefix/suffix maxima

### Code ([H01_TrappingRainWater.java:58-69](H01_TrappingRainWater.java#L58-L69))

```java
static int trapWithArrays(int[] height) {
    int n = height.length;
    if (n == 0) return 0;
    int[] leftMax = new int[n], rightMax = new int[n];
    leftMax[0] = height[0];
    for (int i = 1; i < n; i++) leftMax[i] = Math.max(leftMax[i - 1], height[i]);
    rightMax[n - 1] = height[n - 1];
    for (int i = n - 2; i >= 0; i--) rightMax[i] = Math.max(rightMax[i + 1], height[i]);
    int water = 0;
    for (int i = 0; i < n; i++) water += Math.min(leftMax[i], rightMax[i]) - height[i];
    return water;
}
```

### Dry run: `[0,1,0,2,1,0,1,3,2,1,2,1]`

| i | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| height | 0 | 1 | 0 | 2 | 1 | 0 | 1 | 3 | 2 | 1 | 2 | 1 |
| leftMax | 0 | 1 | 1 | 2 | 2 | 2 | 2 | 3 | 3 | 3 | 3 | 3 |
| rightMax | 3 | 3 | 3 | 3 | 3 | 3 | 3 | 3 | 2 | 2 | 2 | 1 |
| min | 0 | 1 | 1 | 2 | 2 | 2 | 2 | 3 | 2 | 2 | 2 | 1 |
| **water** | 0 | 0 | **1** | 0 | **1** | **2** | **1** | 0 | 0 | **1** | 0 | 0 |

Total = 1 + 1 + 2 + 1 + 1 = **6** ✓

**O(n) time, O(n) space.** It's the clearest correct O(n) solution. Present it before the two-pointer optimisation.

---

## 6. Approach 3: Two pointers, O(1) space (the file's main solution)

### Code ([H01_TrappingRainWater.java:72-87](H01_TrappingRainWater.java#L72-L87))

```java
static int trap(int[] height) {
    int left = 0, right = height.length - 1;
    int leftMax = 0, rightMax = 0, water = 0;
    while (left < right) {
        leftMax = Math.max(leftMax, height[left]);
        rightMax = Math.max(rightMax, height[right]);
        if (leftMax < rightMax) {
            water += leftMax - height[left];       // the left side is the bottleneck
            left++;
        } else {
            water += rightMax - height[right];     // the right side is the bottleneck
            right--;
        }
    }
    return water;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int left = 0, right = n − 1;` | pointers at both ends |
| `int leftMax = 0, rightMax = 0, water = 0;` | the running maxima seen from each side |
| `while (left < right)` | process columns until the pointers meet. The last column (where they meet) is the tallest seen, so it holds no water |
| `leftMax = Math.max(leftMax, height[left]);` | include the current left bar |
| `rightMax = Math.max(rightMax, height[right]);` | include the current right bar |
| `if (leftMax < rightMax)` | the left side is the bottleneck for column `left` |
| `water += leftMax - height[left]; left++;` | settle column `left` and move on |
| `else { water += rightMax - height[right]; right--; }` | otherwise settle column `right` (when they're equal, either side is safe) |

### Dry run 1: `[4, 2, 0, 3, 2, 5]` (answer 9)

| L (h) | R (h) | leftMax | rightMax | side | water added | total |
|---|---|---|---|---|---|---|
| 0 (4) | 5 (5) | 4 | 5 | left | 4 − 4 = 0 | 0 |
| 1 (2) | 5 (5) | 4 | 5 | left | 4 − 2 = 2 | 2 |
| 2 (0) | 5 (5) | 4 | 5 | left | 4 − 0 = 4 | 6 |
| 3 (3) | 5 (5) | 4 | 5 | left | 4 − 3 = 1 | 7 |
| 4 (2) | 5 (5) | 4 | 5 | left | 4 − 2 = 2 | **9** |
| 5 | 5 | | | stop | | |

### Dry run 2: `[0,1,0,2,1,0,1,3,2,1,2,1]` (answer 6)

| L (h) | R (h) | leftMax | rightMax | side | added | total |
|---|---|---|---|---|---|---|
| 0 (0) | 11 (1) | 0 | 1 | left | 0 | 0 |
| 1 (1) | 11 (1) | 1 | 1 | right (equal) | 0 | 0 |
| 1 (1) | 10 (2) | 1 | 2 | left | 0 | 0 |
| 2 (0) | 10 (2) | 1 | 2 | left | **1** | 1 |
| 3 (2) | 10 (2) | 2 | 2 | right | 0 | 1 |
| 3 (2) | 9 (1) | 2 | 2 | right | **1** | 2 |
| 3 (2) | 8 (2) | 2 | 2 | right | 0 | 2 |
| 3 (2) | 7 (3) | 2 | 3 | left | 0 | 2 |
| 4 (1) | 7 (3) | 2 | 3 | left | **1** | 3 |
| 5 (0) | 7 (3) | 2 | 3 | left | **2** | 5 |
| 6 (1) | 7 (3) | 2 | 3 | left | **1** | **6** |
| 7 | 7 | | | stop | | |

The water is added at columns 2, 9, 4, 5 and 6, the same columns as in the table in section 5, just in a different order.

### Complexity
- **Time O(n):** each step settles one column.
- **Space O(1).**

---

## 7. Approach 4: Monotonic stack (layer by layer)

Instead of columns, fill **horizontal layers**. Keep a stack of indices with **decreasing** heights. When a taller bar arrives, it closes a "basin": pop the bottom, and the water above it is bounded by the new top of the stack (the left wall) and the current bar (the right wall).

```java
static int trapStack(int[] height) {
    Deque<Integer> st = new ArrayDeque<>();       // indices, heights decreasing
    int water = 0;
    for (int i = 0; i < height.length; i++) {
        while (!st.isEmpty() && height[i] > height[st.peek()]) {
            int bottom = st.pop();                // the floor of this basin layer
            if (st.isEmpty()) break;              // no left wall → the water spills out
            int leftWall = st.peek();
            int width = i - leftWall - 1;
            int depth = Math.min(height[leftWall], height[i]) - height[bottom];
            water += width * depth;
        }
        st.push(i);
    }
    return water;
}
```

**Dry run** `[3, 0, 0, 2, 0, 4]` (answer 10). The stack is listed bottom → top.

| i (h) | popped bottom (h) | left wall (h) | width | depth = min(wall, h[i]) − bottom | added | total | stack after |
|---|---|---|---|---|---|---|---|
| 0 (3) | — | | | | | 0 | [0] |
| 1 (0) | — | | | | | 0 | [0, 1] |
| 2 (0) | — (0 is not > 0) | | | | | 0 | [0, 1, 2] |
| 3 (2) | 2 (0) | 1 (0) | 1 | min(0, 2) − 0 = 0 | 0 | 0 | |
| | 1 (0) | 0 (3) | 2 | min(3, 2) − 0 = 2 | **4** | 4 | [0, 3] |
| 4 (0) | — | | | | | 4 | [0, 3, 4] |
| 5 (4) | 4 (0) | 3 (2) | 1 | min(2, 4) − 0 = 2 | **2** | 6 | |
| | 3 (2) | 0 (3) | 4 | min(3, 4) − 2 = 1 | **4** | 10 | |
| | 0 (3) | none → break | | | | 10 | [5] |

Total **10** ✓. The last layer (+4) is the band at height 2–3 spanning columns 1–4.

**O(n) time, O(n) space.** It's useful to know because it's the same tool as H03 (Largest Rectangle).

---

## 8. Comparing the approaches

| | Brute | Prefix/suffix arrays | Two pointers | Monotonic stack |
|---|---|---|---|---|
| Time | O(n²) | O(n) | **O(n)** | O(n) |
| Space | O(1) | O(n) | **O(1)** | O(n) |
| Easiest to explain | ✓ | **✓✓** | needs the argument | medium |
| Thinks in | columns | columns | columns | layers |

---

## 9. Edge cases and common mistakes

**Edge cases:** n < 3 (no basin is possible), a monotonic staircase (0), a flat array (0), one tall bar in the middle, a V shape.

**Mistakes:**
1. Using `max(leftMax, rightMax)` instead of `min`.
2. Forgetting to include the current bar in the running maximum, which makes the water negative.
3. In two pointers, moving the side with the **larger** max.
4. In the stack version, forgetting the "no left wall" break.
5. Confusing this with Container With Most Water ([M04](../03-Medium/M04_ContainerWithMostWater.md)), where you choose just two lines and the bars in between don't matter.

---

## 10. Interview questions and answers

**Q1. Derive the formula for one column.**
> The water above column i is limited by the tallest wall on each side; it rises to the lower of the two. So `min(maxLeft, maxRight) − height[i]`, with both maxima including i itself, so it's never negative.

**Q2. Why can the two-pointer version decide a column without knowing the true max on the other side?**
> If `leftMax < rightMax`, the real max to the right of `left` is at least `rightMax`, so the min is `leftMax` no matter what lies in between. The column's water is fully determined.

**Q3. Which approach would you present first?**
> The prefix/suffix arrays version (clear, O(n)), then optimise to two pointers for O(1) space, with the bottleneck argument.

### Follow-up questions

**F1. Trapping Rain Water II (LC 407): a 2D height map.**
> Use a **min-heap** of boundary cells (a priority-queue BFS from the outside in). Always expand from the lowest boundary cell; a neighbour lower than the current water level traps `level − height`. O(mn log(mn)). See [08-Heap](../../08-Heap/README.md).

**F2. Pour V units of water at index K (LC 755).**
> Simulate drop by drop: each drop moves left to the lowest reachable point if it can, else right, else stays.

**F3. Return the water level per column.**
> `level[i] = min(leftMax[i], rightMax[i])`, from the arrays version.

**F4. Bars arrive one at a time (streaming), and you report the trapped water so far.**
> Water depends on future bars (a taller bar later can raise earlier levels), so you can't finalise columns early. The monotonic stack can add the newly closed layers incrementally, though.

---

## 11. Running the file

```bash
java H01_TrappingRainWater.java      # 6/6 tests passed (including 300 random cross-checks)
```

**One-line summary:** *"water[i] = min(maxLeft, maxRight) − height[i]; with two pointers, settle the side whose max is smaller."*
