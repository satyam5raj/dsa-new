# H02 · Max Value of Equation ⭐

| | |
|---|---|
| **LeetCode** | [#1499 Max Value of Equation](https://leetcode.com/problems/max-value-of-equation/) |
| **Difficulty** | Hard |
| **Pattern** | Rewrite the formula as f(i) + g(j) → a sliding-window maximum with a monotonic deque |
| **Code** | [H02_MaxValueOfEquation.java](H02_MaxValueOfEquation.java) |
| **Run** | `java H02_MaxValueOfEquation.java` |

⭐ = a bonus classic.

---

## 1. The problem

`points[i] = [xᵢ, yᵢ]`, sorted by x (strictly increasing). Find the maximum of

```
yᵢ + yⱼ + |xᵢ − xⱼ|      over pairs i < j with  xⱼ − xᵢ ≤ k
```

At least one valid pair is guaranteed.

| points | k | answer | pair |
|---|---|---|---|
| `[[1,3],[2,0],[5,10],[6,-10]]` | 1 | 4 | points 0 and 1: 3 + 0 + 1 |
| `[[0,0],[3,0],[9,2]]` | 3 | 3 | points 0 and 1: 0 + 0 + 3 |

**Constraints:** `2 ≤ n ≤ 10⁵`, `|x|, |y| ≤ 10⁸`.

---

## 2. How to think about it

### Brute force
All pairs within distance k: O(n²).

### The algebra trick (the real insight)
x is sorted, so for i < j: `|xᵢ − xⱼ| = xⱼ − xᵢ`. The expression becomes

```
(yᵢ − xᵢ)  +  (yⱼ + xⱼ)
 depends     depends
 only on i   only on j
```

For a **fixed j**, the second part is a constant. We just need the **max of (yᵢ − xᵢ)** over the earlier points with `xᵢ ≥ xⱼ − k`. That's a **sliding-window maximum**.

### A monotonic deque of indices with decreasing (y − x)
```
for each j:
    drop from the front the points that are too far away (xⱼ − x_front > k)
    if the deque isn't empty: candidate = (y − x of the front) + yⱼ + xⱼ
    drop from the back every point with (y − x) ≤ yⱼ − xⱼ   (j is at least as good, and newer)
    push j
```

**The lesson:** when a formula mixes i and j, try to **separate** it into f(i) + g(j). Then "best pair" becomes "best f(i) in a window, for each j".

---

## 3. Java concepts used in this problem

### 3.1 `int[][]` as a list of points

`points[j][0]` is x and `points[j][1]` is y. The deque stores **indices** into this array, so we can read both x (for the distance check) and y − x (for the max).

### 3.2 The window is defined by x, not by the index count

Unlike [Arrays H02](../../01-Arrays/04-Hard/H02_SlidingWindowMaximum.md) (a fixed number of elements), here the window is "points whose x is within k". Several points can expire at once, so the front removal is a **`while`**, not an `if`.

### 3.3 Overflow check

`y − x + y + x` with |values| ≤ 10⁸ stays within about 4·10⁸, safely inside `int`. With larger limits you'd use `long`.

---

## 4. Approach 1: Brute force

### Code ([H02_MaxValueOfEquation.java:63-69](H02_MaxValueOfEquation.java#L63-L69))

```java
static int brute(int[][] p, int k) {
    int best = Integer.MIN_VALUE;
    for (int i = 0; i < p.length; i++)
        for (int j = i + 1; j < p.length && p[j][0] - p[i][0] <= k; j++)
            best = Math.max(best, p[i][1] + p[j][1] + p[j][0] - p[i][0]);
    return best;
}
```

| Line | What it does |
|---|---|
| the outer loop | the left point i |
| `j < p.length && p[j][0] - p[i][0] <= k` | the right points within distance k. Since x is sorted, we can **stop** at the first one that's too far |
| `best = Math.max(...)` | evaluate the equation (with \|xᵢ − xⱼ\| = xⱼ − xᵢ) |

O(n²) in the worst case (when every point is within k of every other).

---

## 5. Approach 2: A max-heap of (y − x)

```java
static int findMaxValueHeap(int[][] points, int k) {
    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(b[0], a[0]));   // {y - x, x}
    int best = Integer.MIN_VALUE;
    for (int[] p : points) {
        int x = p[0], y = p[1];
        while (!pq.isEmpty() && x - pq.peek()[1] > k) pq.poll();       // lazy removal
        if (!pq.isEmpty()) best = Math.max(best, pq.peek()[0] + y + x);
        pq.offer(new int[]{y - x, x});
    }
    return best;
}
```

The heap always gives the largest y − x. Entries that are too far are only removed when they reach the top: an old entry buried in the heap does no harm until it would be used. **O(n log n).** `Integer.compare` is used instead of `b[0] - a[0]`, which is safer if the values were large.

---

## 6. Approach 3: A monotonic deque (the file's solution)

### Code ([H02_MaxValueOfEquation.java:47-61](H02_MaxValueOfEquation.java#L47-L61))

```java
static int findMaxValueOfEquation(int[][] points, int k) {
    Deque<Integer> dq = new ArrayDeque<>();                 // indices, (y - x) decreasing
    int best = Integer.MIN_VALUE;
    for (int j = 0; j < points.length; j++) {
        int xj = points[j][0], yj = points[j][1];
        while (!dq.isEmpty() && xj - points[dq.peekFirst()][0] > k) dq.pollFirst();   // too far
        if (!dq.isEmpty()) {
            int i = dq.peekFirst();
            best = Math.max(best, points[i][1] - points[i][0] + yj + xj);
        }
        while (!dq.isEmpty() && points[dq.peekLast()][1] - points[dq.peekLast()][0] <= yj - xj) dq.pollLast();
        dq.offerLast(j);
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| the first `while` | expire the points more than k to the left of xⱼ (possibly several) |
| `if (!dq.isEmpty())` | there is at least one valid partner i |
| `best = Math.max(best, (yᵢ − xᵢ) + yⱼ + xⱼ)` | the front has the largest yᵢ − xᵢ in range |
| the second `while` | remove from the back the points whose y − x ≤ yⱼ − xⱼ: j beats them and stays in range longer |
| `dq.offerLast(j);` | j becomes a candidate partner for later points |

Note the **order**: j is used as the right point **before** it's pushed, so it never pairs with itself.

### Dry run 1: `[[1,3],[2,0],[5,10],[6,-10]]`, k = 1

| j | (x, y) | y − x | expire | candidate | dq after (as (x, y−x)) | best |
|---|---|---|---|---|---|---|
| 0 | (1, 3) | 2 | | — (empty) | [(1, 2)] | — |
| 1 | (2, 0) | −2 | 2 − 1 = 1 ≤ 1, keep | 2 + 0 + 2 = **4** | [(1, 2), (2, −2)] | 4 |
| 2 | (5, 10) | 5 | 5 − 1 = 4 > 1 ✗, 5 − 2 = 3 > 1 ✗ → empty | — | [(5, 5)] | 4 |
| 3 | (6, −10) | −16 | 6 − 5 = 1 ≤ 1, keep | 5 − 10 + 6 = 1 | [(5, 5), (6, −16)] | 4 |

**Return 4** ✓

### Dry run 2: `[[0,0],[3,0],[9,2]]`, k = 3

| j | (x, y) | y − x | expire | candidate | dq after | best |
|---|---|---|---|---|---|---|
| 0 | (0, 0) | 0 | | — | [(0, 0)] | — |
| 1 | (3, 0) | −3 | 3 − 0 = 3 ≤ 3, keep | 0 + 0 + 3 = **3** | [(0, 0), (3, −3)] | 3 |
| 2 | (9, 2) | −7 | both too far → empty | — | [(9, −7)] | 3 |

**Return 3** ✓

### Complexity
- **Time O(n):** each index enters and leaves the deque at most once.
- **Space O(n).**

---

## 7. Comparing the approaches

| | Brute force | Max-heap | Monotonic deque |
|---|---|---|---|
| Time | O(n²) | O(n log n) | **O(n)** |
| Space | O(1) | O(n) | O(n) |

---

## 8. Edge cases and common mistakes

**Edge cases:** exactly one valid pair, large negative y values (the answer can be negative), all the points within k, the points far apart except one pair.

**Mistakes:**
1. Not separating the formula (trying to track yᵢ and xᵢ separately).
2. Pushing j before computing its candidate (pairing a point with itself).
3. An `if` instead of a `while` when expiring (several points can fall out at once).
4. Starting best at 0 (the answer can be negative).

---

## 9. Interview questions and answers

**Q1. Why can |xᵢ − xⱼ| be dropped?**
> The points are sorted by x and i < j, so xⱼ > xᵢ and the absolute value is just xⱼ − xᵢ. That's what makes the separation into (yᵢ − xᵢ) + (yⱼ + xⱼ) possible.

**Q2. Why is removing the dominated points from the back safe?**
> If i < j and yᵢ − xᵢ ≤ yⱼ − xⱼ, then for any later point, j is in range whenever i is (j is further right), and j's value is at least as large. So i will never be the best partner again.

### Follow-up questions

**F1. The points are not sorted.**
> Sort them by x first: O(n log n). The equation is symmetric in i and j, so order doesn't change the answer.

**F2. Maximise yᵢ + yⱼ − |xᵢ − xⱼ| instead.**
> It separates into (yᵢ + xᵢ) + (yⱼ − xⱼ): the same deque with a different key.

**F3. The same separation trick elsewhere.**
> Best Sightseeing Pair (LC 1014): `values[i] + i + values[j] − j`, with no window limit, just a running max of `values[i] + i`.

---

## 10. Running the file

```bash
java H02_MaxValueOfEquation.java      # 3/3 tests passed (including ~970 random cases vs brute force)
```

**One-line summary:** *"Rewrite yᵢ + yⱼ + xⱼ − xᵢ as (yᵢ − xᵢ) + (yⱼ + xⱼ); for each j, a decreasing deque gives the best yᵢ − xᵢ within distance k."*
