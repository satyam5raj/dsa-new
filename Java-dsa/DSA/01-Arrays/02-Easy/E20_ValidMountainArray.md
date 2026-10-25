# E20 · Valid Mountain Array

| | |
|---|---|
| **LeetCode** | [#941 Valid Mountain Array](https://leetcode.com/problems/valid-mountain-array/) |
| **Difficulty** | Easy |
| **Pattern** | Walk up, then walk down (simulation with one pointer) |
| **Code** | [E20_ValidMountainArray.java](E20_ValidMountainArray.java) |
| **Run** | `java E20_ValidMountainArray.java` |

---

## 1. The problem

`arr` is a **mountain** if:
1. `arr.length ≥ 3`
2. it **strictly increases** up to some peak index `p`,
3. then **strictly decreases** from `p` to the end,
4. and the peak is **not** the first or last element (you must actually go up **and** come down).

```
valid:      [0, 3, 2, 1]          3
                                 / \
                                0   2
                                     \
                                      1
```

| arr | answer | why |
|---|---|---|
| `[0, 3, 2, 1]` | true | up 0→3, down 3→2→1 |
| `[3, 5, 5]` | false | flat top (not strict) |
| `[0, 1, 2, 3]` | false | never comes down |
| `[3, 2, 1]` | false | never goes up |
| `[2, 1]` | false | too short |
| `[1, 3, 2, 2]` | false | flat at the bottom |

**Constraints:** `1 ≤ n ≤ 10⁴`.

---

## 2. How to think about it

**Simulate a hiker** with one pointer `i`:
1. Start at `i = 0` and **climb** while the next element is strictly higher.
2. Now `i` is the peak. If the peak is at index 0 (we never climbed) or at the last index (we never descended), it's not a mountain.
3. **Descend** while the next element is strictly lower.
4. It's a mountain **only if we reached the last index**. If we stopped early, something broke the descent: a plateau or a climb.

Plateaus (`5, 5`) stop both walks, because `<` and `>` are both false for equal values. So they automatically fail.

---

## 3. Java concepts used in this problem

### 3.1 `while` with a bounds check first

```java
while (i + 1 < n && arr[i] < arr[i + 1]) i++;
```

- `i + 1 < n` comes **first**, so `arr[i + 1]` is only read when it exists (short-circuit `&&`, see E07). Swapping the order would crash at the end of the array.
- The body is just `i++`. All the logic is in the condition.

### 3.2 Guard clauses

```java
if (n < 3) return false;
if (i == 0 || i == n - 1) return false;
```

Rule out invalid cases early, one simple condition at a time. The main logic stays flat and readable instead of being deeply nested in `if`s.

---

## 4. Approach 1: Find the peak with a max, then check both sides

```java
static boolean validMountainPeak(int[] arr) {
    int n = arr.length;
    if (n < 3) return false;
    int peak = 0;
    for (int i = 1; i < n; i++) if (arr[i] > arr[peak]) peak = i;   // index of the max
    if (peak == 0 || peak == n - 1) return false;
    for (int i = 0; i < peak; i++) if (arr[i] >= arr[i + 1]) return false;      // strictly up
    for (int i = peak; i < n - 1; i++) if (arr[i] <= arr[i + 1]) return false;  // strictly down
    return true;
}
```

- First find the **maximum**; in a valid mountain it's the peak.
- Then check strict increase before it and strict decrease after it.
- `>=` / `<=` reject equal neighbours (plateaus).
- **O(n) time** (three passes), **O(1) space.**

---

## 5. Approach 2: One pointer, walk up then down (the file's solution)

### Code ([E20_ValidMountainArray.java:33-41](E20_ValidMountainArray.java#L33-L41))

```java
static boolean validMountainArray(int[] arr) {
    int n = arr.length;
    if (n < 3) return false;
    int i = 0;
    while (i + 1 < n && arr[i] < arr[i + 1]) i++;    // walk up
    if (i == 0 || i == n - 1) return false;          // never went up, or never came down
    while (i + 1 < n && arr[i] > arr[i + 1]) i++;    // walk down
    return i == n - 1;
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (n < 3) return false;` | a mountain needs at least a base, a peak and a base |
| `int i = 0;` | the hiker starts at the left |
| `while (i + 1 < n && arr[i] < arr[i + 1]) i++;` | climb while the next step is strictly higher. Afterwards `i` is the peak (the last point of the climb) |
| `if (i == 0 \|\| i == n - 1) return false;` | `i == 0`: the first step wasn't up. `i == n−1`: we climbed all the way to the end, with no descent |
| `while (i + 1 < n && arr[i] > arr[i + 1]) i++;` | descend while the next step is strictly lower |
| `return i == n - 1;` | a valid mountain lets the descent reach the very end. Stopping earlier means a plateau or a second climb |

### Dry run 1: `[0, 3, 2, 1]` (n = 4)

| phase | i | compare | continue? |
|---|---|---|---|
| climb | 0 | 0 < 3 | yes → i = 1 |
| climb | 1 | 3 < 2 | no → peak = 1 |
| check | 1 | 1 ≠ 0 and 1 ≠ 3 | OK |
| descend | 1 | 3 > 2 | yes → i = 2 |
| descend | 2 | 2 > 1 | yes → i = 3 |
| descend | 3 | i + 1 < 4? no | stop |
| result | 3 | 3 == n − 1 | **true** ✓ |

### Dry run 2: `[3, 5, 5]` (plateau at the top)

| phase | i | compare | |
|---|---|---|---|
| climb | 0 | 3 < 5 | i = 1 |
| climb | 1 | 5 < 5? no | peak = 1 |
| check | 1 | not an end | OK |
| descend | 1 | 5 > 5? no | stop at i = 1 |
| result | 1 | 1 ≠ 2 | **false** ✓ |

### Dry run 3: `[0, 1, 2, 3]` (only up)
The climb reaches i = 3 = n − 1, so the check fails → **false** ✓

### Dry run 4: `[3, 2, 1]` (only down)
Climb: 3 < 2? no → i stays 0 → the check `i == 0` → **false** ✓

### Dry run 5: `[1, 3, 2, 2]` (flat at the bottom)
Climb to i = 1. Descend: 3 > 2 → i = 2; 2 > 2? no → stop at 2. `2 ≠ 3` → **false** ✓

### Complexity
- **Time O(n):** `i` only moves forward, n steps at most.
- **Space O(1).**

---

## 6. (Bonus) Approach 3: Two climbers from both ends

One climber goes up from the left, another goes up from the right. It's a mountain if they meet at the **same** peak, and that peak isn't at an end:

```java
static boolean validMountainTwoClimbers(int[] arr) {
    int n = arr.length, l = 0, r = n - 1;
    while (l + 1 < n && arr[l] < arr[l + 1]) l++;
    while (r > 0 && arr[r - 1] > arr[r]) r--;
    return l > 0 && r < n - 1 && l == r;
}
```

**Dry run** `[0, 3, 2, 1]`: l climbs to 1; r climbs from 3 → 2 → 1; `l == r == 1`, not at an end → **true**.
**Dry run** `[3, 5, 5]`: l = 1; r: is arr[1] = 5 > arr[2] = 5? No → r = 2; `1 ≠ 2` → **false**.

---

## 7. Comparing the approaches

| | Max + check sides | Walk up/down | Two climbers |
|---|---|---|---|
| Time | O(n), 3 passes | **O(n), 1 pass** | O(n) |
| Space | O(1) | O(1) | O(1) |

---

## 8. Edge cases and common mistakes

**Edge cases:** length < 3, only increasing, only decreasing, plateau at the top, plateau on a slope, two peaks (`[1,3,2,4,1]` → false).

**Mistakes:**
1. Forgetting the `i == 0` / `i == n − 1` check, so strictly increasing or decreasing arrays pass.
2. Using `<=` in the climb, which lets plateaus through.
3. Reading `arr[i + 1]` before checking `i + 1 < n`.
4. Returning true right after finding a peak, without checking the descent all the way to the end.

---

## 9. Interview questions and answers

**Q1. Why check the peak position right after climbing?**
> If the climb never moved (i = 0), there's no uphill. If it reached the end (i = n−1), there's no downhill. Both are invalid, and checking here keeps the descent logic simple.

**Q2. How are plateaus handled without a special case?**
> Both loops use strict comparisons, so an equal neighbour stops the walk. The final `i == n − 1` check then fails.

**Q3. What does `i` hold after the first loop?**
> The index of the first local maximum: the last position of the strictly increasing prefix.

### Follow-up questions

**F1. Peak Index in a Mountain Array (LC 852): it is guaranteed to be a mountain, find the peak in O(log n).**
> Binary search: if `arr[mid] < arr[mid+1]` we're on the uphill, so `lo = mid + 1`; otherwise `hi = mid`. It ends at the peak.

**F2. Longest Mountain in an Array (LC 845): the longest subarray that is a mountain.**
> For each position, count the uphill length ending there and the downhill length starting there (two passes, or one pass with up/down counters). The answer is max(up + down + 1) where both are > 0.

**F3. Minimum removals to make a mountain array (LC 1671).**
> LIS from the left and LDS from the right at each index; the answer is n − max(lis[i] + lds[i] − 1) over the valid peaks. O(n log n) with patience sorting.

**F4. Find Peak Element (LC 162): any local peak, in O(log n).**
> The same binary search as F1. It works even without a single mountain shape, because moving toward a higher neighbour always leads to some peak.

---

## 10. Running the file

```bash
java E20_ValidMountainArray.java      # 6/6 tests passed
```

**One-line summary:** *"Climb while strictly up; the peak can't be at an end; descend while strictly down; you must reach the end."*
