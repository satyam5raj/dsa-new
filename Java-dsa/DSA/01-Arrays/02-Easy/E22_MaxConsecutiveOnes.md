# E22 · Max Consecutive Ones

| | |
|---|---|
| **LeetCode** | [#485 Max Consecutive Ones](https://leetcode.com/problems/max-consecutive-ones/) |
| **Difficulty** | Easy |
| **Pattern** | Running counter (reset on a break) |
| **Code** | [E22_MaxConsecutiveOnes.java](E22_MaxConsecutiveOnes.java) |
| **Run** | `java E22_MaxConsecutiveOnes.java` |

---

## 1. The problem

Given a **binary** array (only 0s and 1s), return the length of the **longest run of consecutive 1s**.

| nums | answer |
|---|---|
| `[1, 1, 0, 1, 1, 1]` | 3 |
| `[1, 0, 1, 1, 0, 1]` | 2 |
| `[0, 0]` | 0 |
| `[1, 1, 1]` | 3 |

**Constraints:** `1 ≤ n ≤ 10⁵`, each value is 0 or 1.

---

## 2. How to think about it

Walk left to right and keep two numbers:
- `current` = the length of the run of 1s **ending at the current position**
- `best` = the longest run seen so far

At each element:
- **1** → the run continues: `current++`, then update `best`.
- **0** → the run is broken: `current = 0`.

This "**running counter that resets**" is the simplest version of a pattern that grows into Kadane's algorithm (M01) and the sliding window (M12 Max Consecutive Ones III).

---

## 3. Java concepts used in this problem

### 3.1 Two related variables in one declaration

```java
int best = 0, current = 0;
```

### 3.2 Updating the max inside the "1" branch only

`best` can only grow while a run is growing, so we only call `Math.max` after `current++`. Updating it on every element would also be correct, just slightly wasteful.

### 3.3 Why `if / else` here

`x` is either 1 or 0, so `else` safely means "x is 0". If other values were possible, you'd write `else if (x == 0)` or treat "anything but 1" as a break.

---

## 4. Approach 1: Brute force (start a count at every index)

```java
static int maxOnesBrute(int[] nums) {
    int best = 0;
    for (int i = 0; i < nums.length; i++) {
        int len = 0;
        for (int j = i; j < nums.length && nums[j] == 1; j++) len++;   // run starting at i
        best = Math.max(best, len);
    }
    return best;
}
```

- The inner `for` has **two** conditions: stay inside the array **and** keep seeing 1s.
- **O(n²)** worst case (an all-ones array). It re-counts the same run from every starting point.

---

## 5. Approach 2: Running counter (the file's solution)

### Code ([E22_MaxConsecutiveOnes.java:37-48](E22_MaxConsecutiveOnes.java#L37-L48))

```java
static int findMaxConsecutiveOnes(int[] nums) {
    int best = 0, current = 0;
    for (int x : nums) {
        if (x == 1) {
            current++;
            best = Math.max(best, current);
        } else {
            current = 0;
        }
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int best = 0, current = 0;` | no 1s seen yet. `best = 0` is also the right answer for an all-zero array |
| `for (int x : nums)` | one pass; indices aren't needed |
| `if (x == 1) { current++; ... }` | extend the current run |
| `best = Math.max(best, current);` | record a new longest run |
| `else current = 0;` | a 0 ends the run |
| `return best;` | |

### Dry run 1: `[1, 1, 0, 1, 1, 1]`

| x | current | best |
|---|---|---|
| 1 | 1 | 1 |
| 1 | 2 | 2 |
| 0 | 0 | 2 |
| 1 | 1 | 2 |
| 1 | 2 | 2 |
| 1 | 3 | **3** |

**Return 3** ✓

### Dry run 2: `[1, 0, 1, 1, 0, 1]`

| x | current | best |
|---|---|---|
| 1 | 1 | 1 |
| 0 | 0 | 1 |
| 1 | 1 | 1 |
| 1 | 2 | **2** |
| 0 | 0 | 2 |
| 1 | 1 | 2 |

**Return 2** ✓

### Dry run 3: `[0, 0]`
`current` stays 0 and `best` stays 0 → **return 0** ✓

### Complexity
- **Time O(n)**, **Space O(1).**

---

## 6. (Bonus) Track the last zero's index

Another way to think about it: the run ending at index i has length `i − (index of the last 0)`.

```java
static int maxOnesLastZero(int[] nums) {
    int best = 0, lastZero = -1;              // a virtual 0 just before the array
    for (int i = 0; i < nums.length; i++) {
        if (nums[i] == 0) lastZero = i;
        else best = Math.max(best, i - lastZero);
    }
    return best;
}
```

**Dry run** `[1,1,0,1,1,1]`: i=0 → 0−(−1) = 1; i=1 → 2; i=2 → lastZero = 2; i=3 → 1; i=4 → 2; i=5 → 3 → **3** ✓.
Thinking about window boundaries like this leads directly into the sliding window for M12.

---

## 7. Comparing the approaches

| | Brute force | Running counter | Last-zero index |
|---|---|---|---|
| Time | O(n²) | **O(n)** | O(n) |
| Space | O(1) | O(1) | O(1) |

---

## 8. Edge cases and common mistakes

**Edge cases:** all zeros, all ones, a single element, the longest run at the very end.

**Mistakes:**
1. Updating `best` only when a 0 appears. Then a run that ends at the **end of the array** is never recorded (e.g. `[1,1,1]` would return 0). Either update on every 1 (as here) or add a final `max` after the loop.
2. Forgetting to reset `current` on a 0.

---

## 9. Interview questions and answers

**Q1. Why update `best` inside the loop rather than when a run ends?**
> A run can end at the array's end without a terminating 0. Updating on every 1 handles that with no extra code after the loop.

**Q2. What's the invariant?**
> After processing index i, `current` is the length of the run of 1s ending at i, and `best` is the maximum such length over indices 0..i.

### Follow-up questions

**F1. Max Consecutive Ones II (LC 487): you may flip at most ONE 0.**
> Keep `prevRun` (the run before the last 0) and `curRun`. On a 0: `prevRun = curRun + 1` (counting the flipped 0), `curRun = 0`. Answer = max(prevRun + curRun). Or a sliding window that holds at most one 0.

**F2. Max Consecutive Ones III (LC 1004): flip at most k zeros.** → [M12](../03-Medium/M12_MaxConsecutiveOnesIII.md)
> A variable-size sliding window: expand right, count the zeros inside, and shrink from the left while zeros > k. The answer is the largest window. O(n).

**F3. The longest run of any equal values.**
> Compare each element with the previous one instead of with 1: if equal, `current++`, else `current = 1`.

**F4. The data is a huge stream.**
> The running-counter solution is already one pass with O(1) memory, so it works on a stream as is.

**F5. Longest subarray of 1s after deleting exactly one element (LC 1493).**
> A sliding window with at most one 0; the answer is the window length − 1 (one element must be deleted).

---

## 10. Running the file

```bash
java E22_MaxConsecutiveOnes.java      # 4/4 tests passed
```

**One-line summary:** *"Count the current run of 1s, reset on 0, keep the best."*
