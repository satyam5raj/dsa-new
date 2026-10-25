# M04 · Container With Most Water

| | |
|---|---|
| **LeetCode** | [#11 Container With Most Water](https://leetcode.com/problems/container-with-most-water/) |
| **Difficulty** | Medium |
| **Pattern** | Two pointers (opposite ends, greedily move the shorter side) |
| **Code** | [M04_ContainerWithMostWater.java](M04_ContainerWithMostWater.java) |
| **Run** | `java M04_ContainerWithMostWater.java` |

---

## 1. The problem

`height[i]` is a vertical line at x = i. Pick **two** lines which, together with the x-axis, form a container that holds the **most water**.

```
area(i, j) = min(height[i], height[j]) × (j − i)
                 └── water level ──┘     └ width ┘
```

The shorter line decides the water level (water would spill over it).

```
height = [1, 8, 6, 2, 5, 4, 8, 3, 7]

  8 |   █         █
  7 |   █ ~ ~ ~ ~ █ ~ █        ~ = water between lines 1 and 8
  6 |   █ █ ~ ~ ~ █ ~ █            (the level is 7, set by the shorter line)
  5 |   █ █ ~ █ ~ █ ~ █
  4 |   █ █ ~ █ █ █ ~ █
  3 |   █ █ ~ █ █ █ █ █
  2 |   █ █ █ █ █ █ █ █
  1 | █ █ █ █ █ █ █ █ █
      0 1 2 3 4 5 6 7 8
lines 1 and 8: min(8, 7) × (8 − 1) = 49   (the bars in between don't matter: only the two chosen lines count)
```

| height | answer |
|---|---|
| `[1,8,6,2,5,4,8,3,7]` | 49 |
| `[1, 1]` | 1 |
| `[4, 3, 2, 1, 4]` | 16 (the two 4s, width 4) |
| `[1, 2, 1]` | 2 |

**Constraints:** `2 ≤ n ≤ 10⁵`, `0 ≤ height[i] ≤ 10⁴`.

---

## 2. How to think about it

1. **Brute force:** every pair, O(n²). Too slow for n = 10⁵.
2. **Two pointers:** start with the **widest** container (`left = 0`, `right = n − 1`). Every move shrinks the width by 1, so to have **any** chance of a larger area, the height must go up. **Which side should move?**

   > **Always move the shorter line.**

### Why moving the shorter line is safe (the key proof)
Say `height[left] < height[right]`. Consider **every** container that uses `left` with some partner `k` between left and right:
- its width `k − left` is **smaller** than the current width,
- its height `min(height[left], height[k])` is **at most** `height[left]`, the current height.

So every such container is **no better than the current one**, which we have already recorded. That means `left` has nothing more to offer, and we can discard it forever. (Moving the **taller** line instead could never help: the shorter line would still cap the height while the width shrinks.)

When the heights are equal, moving either side is fine: the same argument applies to both.

---

## 3. Java concepts used in this problem

### 3.1 Randomised cross-checking against brute force

The file's `main` does something very useful when you practise:

```java
Random rnd = new Random(7);                          // a seeded random generator → the same numbers every run
for (int t = 0; t < 200; t++) {
    int[] h = rnd.ints(2 + rnd.nextInt(30), 0, 50).toArray();   // random length 2..31, values 0..49
    if (maxArea(h) != maxAreaBrute(h)) allMatch = false;
}
```

| Piece | Meaning |
|---|---|
| `new Random(7)` | a pseudo-random generator with **seed** 7. The same seed gives the same sequence, so a failure is reproducible |
| `rnd.nextInt(30)` | a random int in `[0, 30)` |
| `rnd.ints(count, lo, hi)` | an `IntStream` of `count` random ints in `[lo, hi)` |
| `.toArray()` | turn the stream into `int[]` |

This is called **stress testing** or **differential testing**: compare your clever solution with a slow but obviously correct one on many random inputs. It catches edge cases you didn't think of. Use it whenever a greedy argument feels slippery.

### 3.2 `for` loops without braces, nested

```java
for (int i = 0; i < n; i++)
    for (int j = i + 1; j < n; j++)
        best = Math.max(best, ...);
```

Each `for` without braces controls exactly **one** statement, which here is the next `for`. It's legal, but easy to break when you add a line, so prefer braces.

---

## 4. Approach 1: Brute force

### Code ([M04_ContainerWithMostWater.java:54-60](M04_ContainerWithMostWater.java#L54-L60))

```java
static int maxAreaBrute(int[] height) {
    int best = 0;
    for (int i = 0; i < height.length; i++)
        for (int j = i + 1; j < height.length; j++)
            best = Math.max(best, Math.min(height[i], height[j]) * (j - i));
    return best;
}
```

Every pair (i < j), computing the area each time. **O(n²) time, O(1) space.** In this file it serves as the **reference** for the random tests.

---

## 5. Approach 2: Two pointers (optimal)

### Code ([M04_ContainerWithMostWater.java:43-52](M04_ContainerWithMostWater.java#L43-L52))

```java
static int maxArea(int[] height) {
    int left = 0, right = height.length - 1, best = 0;
    while (left < right) {
        int h = Math.min(height[left], height[right]);
        best = Math.max(best, h * (right - left));
        if (height[left] < height[right]) left++;     // move the SHORTER side
        else right--;
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int left = 0, right = n - 1, best = 0;` | the widest container first; area 0 so far |
| `while (left < right)` | we need two different lines |
| `int h = Math.min(height[left], height[right]);` | the water level is set by the shorter line |
| `best = Math.max(best, h * (right - left));` | record this container's area |
| `if (height[left] < height[right]) left++;` | left is shorter → nothing better can use it → discard it |
| `else right--;` | right is shorter (or they're equal) → discard right |

### Dry run 1: `[1, 8, 6, 2, 5, 4, 8, 3, 7]`

| L (h) | R (h) | width | min h | area | best | move |
|---|---|---|---|---|---|---|
| 0 (1) | 8 (7) | 8 | 1 | 8 | 8 | L (1 < 7) |
| 1 (8) | 8 (7) | 7 | 7 | **49** | **49** | R |
| 1 (8) | 7 (3) | 6 | 3 | 18 | 49 | R |
| 1 (8) | 6 (8) | 5 | 8 | 40 | 49 | R (equal → else) |
| 1 (8) | 5 (4) | 4 | 4 | 16 | 49 | R |
| 1 (8) | 4 (5) | 3 | 5 | 15 | 49 | R |
| 1 (8) | 3 (2) | 2 | 2 | 4 | 49 | R |
| 1 (8) | 2 (6) | 1 | 6 | 6 | 49 | R |
| 1 | 1 | | | stop | | |

**Return 49** ✓

### Dry run 2: `[4, 3, 2, 1, 4]`

| L (h) | R (h) | width | area | best | move |
|---|---|---|---|---|---|
| 0 (4) | 4 (4) | 4 | **16** | 16 | R (equal) |
| 0 (4) | 3 (1) | 3 | 3 | 16 | R |
| 0 (4) | 2 (2) | 2 | 4 | 16 | R |
| 0 (4) | 1 (3) | 1 | 3 | 16 | R |

**Return 16** ✓

### Dry run 3: `[1, 2, 1]`

| L | R | area | move |
|---|---|---|---|
| 0 (1) | 2 (1) | 1 × 2 = 2 | R |
| 0 (1) | 1 (2) | 1 × 1 = 1 | L |

**Return 2** ✓

### Complexity
- **Time O(n):** each step moves one pointer; n − 1 steps.
- **Space O(1).**

### (Optional speed-up) Skip lines that can't help
After moving away from a short line, any new line that's **not taller** than the one you discarded can't do better (the width is smaller and the height is no higher), so you can skip such lines in a tight inner loop. It's the same O(n) complexity with fewer area calculations.

---

## 6. Comparing the approaches

| | Brute force | Two pointers |
|---|---|---|
| Time | O(n²) | **O(n)** |
| Space | O(1) | O(1) |

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 2, all heights equal, zeros, the tallest lines in the middle, a strictly increasing or decreasing array.

**Mistakes:**
1. Moving the **taller** side: it can never increase the area, and it can miss the optimum.
2. Using `max` instead of `min` for the water level.
3. Width `j − i + 1` (off by one: the width between x = i and x = j is `j − i`).
4. Confusing this with **Trapping Rain Water** (H01), where water collects above **every** bar, not just between two chosen lines.

---

## 8. Interview questions and answers

**Q1. Why move the shorter line?**
> The shorter line caps the height. Any container that keeps it and moves the other side has a smaller width and a height no greater than now, so it's never better. That line can't be part of a better answer, so we drop it.

**Q2. What if both are equal?**
> Moving either is safe. Any better container would need both lines to be replaced (keeping either one caps the height at the current value while the width shrinks), so neither of the current lines is in a better solution.

**Q3. How would you convince yourself the greedy is right?**
> The proof above, plus a randomised comparison against brute force (like this file's 200 random tests).

### Follow-up questions

**F1. Trapping Rain Water (LC 42).** → [04-Hard H01](../04-Hard/H01_TrappingRainWater.md)
> The water above bar i = `min(maxLeft, maxRight) − height[i]`. Two pointers again: move the side with the smaller max.

**F2. Return the indices of the best pair.**
> Store `left` and `right` whenever `best` improves.

**F3. Lines also have thickness, or the container must have at least width w.**
> For a minimum width: stop when `right − left < w`. More complex constraints may need other techniques.

**F4. Largest rectangle in a histogram (LC 84).** → [04-Hard H03](../04-Hard/H03_LargestRectangleInHistogram.md)
> A different problem: the rectangle's height is the **minimum of all bars in the range**, not just the two ends. Use a monotonic stack, O(n).

---

## 9. Running the file

```bash
java M04_ContainerWithMostWater.java      # 5/5 tests passed (including 200 random cross-checks)
```

**One-line summary:** *"Start widest; the shorter line limits the area, so it can never do better: move it inward."*
