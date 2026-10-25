# E27 · Height Checker

| | |
|---|---|
| **LeetCode** | [#1051 Height Checker](https://leetcode.com/problems/height-checker/) |
| **Difficulty** | Easy |
| **Pattern** | Compare with the sorted version (counting sort for small ranges) |
| **Code** | [E27_HeightChecker.java](E27_HeightChecker.java) |
| **Run** | `java E27_HeightChecker.java` |

---

## 1. The problem

Students should stand in **non-decreasing** order of height. Given their current heights, return **how many positions don't match** the expected (sorted) order.

| heights | expected (sorted) | mismatching positions | answer |
|---|---|---|---|
| `[1, 1, 4, 2, 1, 3]` | `[1, 1, 1, 2, 3, 4]` | 2, 4, 5 | 3 |
| `[5, 1, 2, 3, 4]` | `[1, 2, 3, 4, 5]` | all 5 | 5 |
| `[1, 2, 3, 4, 5]` | same | none | 0 |

**Constraints:** `1 ≤ n ≤ 100`, `1 ≤ heights[i] ≤ 100`.

Note: it's **not** asking for the minimum number of moves; it only counts the positions that differ.

---

## 2. How to think about it

1. **Direct:** make a sorted copy and compare position by position. O(n log n).
2. **Clue: heights are only 1..100.** Use **counting sort** (as in E26). Even better, you don't need to **build** the sorted array: walk the original array and generate the expected sequence **on the fly** from the counts (the smallest remaining height first).

```
counts:  1→3, 2→1, 3→1, 4→1
expected stream (from the counts): 1 1 1 2 3 4
actual:                            1 1 4 2 1 3
                                       ✗   ✗ ✗   → 3
```

---

## 3. Java concepts used in this problem

### 3.1 `clone()` before sorting

```java
int[] expected = heights.clone();
Arrays.sort(expected);
```

We need **both** the original order and the sorted order, so we sort a copy. Sorting `heights` directly would destroy the very thing we're comparing against. (See E02 for clone vs reference copying.)

### 3.2 A "cursor" over the count table

```java
int current = 1;                          // the smallest height that might still be available
while (count[current] == 0) current++;    // skip heights that are used up
...
count[current]--;                         // use one student of that height
```

`current` only moves **forward**, which is how the counts produce the sorted sequence one element at a time. Across the whole run `current` moves at most 100 times, so the total cost is O(n + R).

### 3.3 A one-line `for` with an `if`

```java
for (int i = 0; i < heights.length; i++) if (heights[i] != expected[i]) diff++;
```

That's fine for very short bodies. Add braces when it grows.

---

## 4. Approach 1: Sort a copy and compare

### Code ([E27_HeightChecker.java:34-40](E27_HeightChecker.java#L34-L40))

```java
static int heightCheckerSort(int[] heights) {
    int[] expected = heights.clone();
    Arrays.sort(expected);
    int diff = 0;
    for (int i = 0; i < heights.length; i++) if (heights[i] != expected[i]) diff++;
    return diff;
}
```

| Line | What it does |
|---|---|
| `int[] expected = heights.clone(); Arrays.sort(expected);` | what the line should look like |
| `for ... if (heights[i] != expected[i]) diff++;` | count the positions where reality differs |
| `return diff;` | |

**Dry run** `[1, 1, 4, 2, 1, 3]` → expected `[1, 1, 1, 2, 3, 4]`:

| i | actual | expected | differ? | diff |
|---|---|---|---|---|
| 0 | 1 | 1 | no | 0 |
| 1 | 1 | 1 | no | 0 |
| 2 | 4 | 1 | **yes** | 1 |
| 3 | 2 | 2 | no | 1 |
| 4 | 1 | 3 | **yes** | 2 |
| 5 | 3 | 4 | **yes** | 3 |

**Return 3** ✓. **O(n log n) time, O(n) space.**

---

## 5. Approach 2: Counting sort, expected values on the fly (the file's solution)

### Code ([E27_HeightChecker.java:42-52](E27_HeightChecker.java#L42-L52))

```java
static int heightChecker(int[] heights) {
    int[] count = new int[101];
    for (int h : heights) count[h]++;
    int diff = 0, current = 1;                       // current = next expected height
    for (int h : heights) {
        while (count[current] == 0) current++;        // skip heights we've used up
        if (h != current) diff++;
        count[current]--;                             // consume one of the expected height
    }
    return diff;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int[] count = new int[101];` | heights 1..100 (index 0 is unused) |
| `for (int h : heights) count[h]++;` | how many students of each height |
| `int diff = 0, current = 1;` | `current` = the height the next position **should** have |
| `for (int h : heights)` | walk the **actual** line, position by position |
| `while (count[current] == 0) current++;` | find the smallest height not yet "placed" in the expected line |
| `if (h != current) diff++;` | the actual height doesn't match the expected one |
| `count[current]--;` | the expected line used one student of height `current` |

**Why can't the `while` run past 100?** The total of the counts equals n, and we take exactly one per position. So whenever we enter the `while`, at least one count is still positive at or after `current`.

### Dry run: `[1, 1, 4, 2, 1, 3]`

Counts: `1→3, 2→1, 3→1, 4→1`

| pos | actual h | current after skipping | counts before | match? | diff | counts after |
|---|---|---|---|---|---|---|
| 0 | 1 | 1 | 1:3 | ✓ | 0 | 1:2 |
| 1 | 1 | 1 | 1:2 | ✓ | 0 | 1:1 |
| 2 | 4 | 1 | 1:1 | ✗ | 1 | 1:0 |
| 3 | 2 | 1 → **2** (count[1] = 0) | 2:1 | ✓ | 1 | 2:0 |
| 4 | 1 | 2 → **3** | 3:1 | ✗ | 2 | 3:0 |
| 5 | 3 | 3 → **4** | 4:1 | ✗ | 3 | 4:0 |

**Return 3** ✓

### Dry run: `[5, 1, 2, 3, 4]`
Expected stream: 1, 2, 3, 4, 5. Actual: 5, 1, 2, 3, 4. Every position differs → **5** ✓

### Complexity
- **Time O(n + R)**, R = 100: counting is O(n), and `current` sweeps the range once.
- **Space O(R)**: the count array. No sorted copy needed.

---

## 6. Comparing the approaches

| | Sort a copy | Counting, on the fly |
|---|---|---|
| Time | O(n log n) | **O(n + R)** |
| Extra space | O(n) | **O(R)** (101 ints) |
| Works for any range? | **yes** | only small R |

With n ≤ 100, both are instant. The point is to recognise when counting sort applies.

---

## 7. Edge cases and common mistakes

**Edge cases:** already sorted (0), reverse sorted, all equal heights (0), a single student.

**Mistakes:**
1. Sorting `heights` itself instead of a copy, which then compares the array with itself and always gives 0.
2. A count array of size 100, so height 100 overflows it.
3. Confusing the question with "the minimum number of swaps to sort". That's a different problem (counting cycles in a permutation).

---

## 8. Interview questions and answers

**Q1. Why can you avoid building the sorted array?**
> The counts implicitly describe the sorted order: the smallest height with a remaining count comes next. A forward-moving pointer over the counts generates the expected value for each position in turn.

**Q2. What's the complexity of the counting approach, and when is it worth it?**
> O(n + R) time, O(R) space. It's worth it when R is small compared with n log n. Here R = 100.

**Q3. How is this different from "minimum swaps to sort"?**
> Height Checker only counts the mismatched positions. Minimum swaps needs a cycle decomposition: for each cycle of length L in the "where should this element go" permutation, you need L − 1 swaps. Duplicates make it harder.

### Follow-up questions

**F1. Minimum swaps to sort an array of distinct values.**
> Sort `(value, index)` pairs to learn each element's target position, then count the cycles: answer = Σ(cycle length − 1). O(n log n).

**F2. Minimum adjacent swaps to sort (= the number of inversions).**
> Count the inversions with merge sort or a Fenwick tree: O(n log n).

**F3. The smallest subarray that, once sorted, sorts everything (LC 581).**
> The leftmost and rightmost mismatching positions between the array and its sorted copy (O(n log n)), or an O(n) two-pass scan using running max and min.

**F4. Heights are real numbers or a huge range.**
> Fall back to sorting a copy.

---

## 9. Running the file

```bash
java E27_HeightChecker.java      # 4/4 tests passed
```

**One-line summary:** *"Compare each position with the sorted order, generated from a count table when heights are small."*
