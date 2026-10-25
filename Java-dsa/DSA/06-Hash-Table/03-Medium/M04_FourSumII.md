# M04 · 4Sum II ⭐

| | |
|---|---|
| **LeetCode** | [#454 4Sum II](https://leetcode.com/problems/4sum-ii/) |
| **Difficulty** | Medium |
| **Pattern** | Meet in the middle (hash the pair sums of one half) |
| **Code** | [M04_FourSumII.java](M04_FourSumII.java) |
| **Run** | `java M04_FourSumII.java` |

⭐ = a bonus classic.

---

## 1. The problem

Four arrays A, B, C, D, each of length n. Count the tuples (i, j, k, l) with

```
A[i] + B[j] + C[k] + D[l] == 0
```

| A | B | C | D | answer |
|---|---|---|---|---|
| `[1, 2]` | `[-2, -1]` | `[-1, 2]` | `[0, 2]` | 2 |
| `[0]` | `[0]` | `[0]` | `[0]` | 1 |

The two tuples in the first example: (0, 0, 0, 1): 1 − 2 − 1 + 2 = 0, and (1, 1, 0, 0): 2 − 1 − 1 + 0 = 0.

**Constraints:** `1 ≤ n ≤ 200`, `|values| ≤ 2²⁸`.

---

## 2. How to think about it

### Brute force
Four nested loops: O(n⁴). With n = 200 that's 1.6 billion. Too slow.

### Meet in the middle
Split the sum into two halves:

```
(A[i] + B[j])  +  (C[k] + D[l])  ==  0
  left half         right half
```

1. Count every left sum `a + b` in a HashMap: **sum → how many (i, j) pairs give it**. O(n²)
2. For every right sum `c + d`, the left half must be `−(c + d)`; add `map[−(c + d)]`. O(n²)

O(n⁴) becomes **O(n²)**. It's Two Sum where each "number" is a pair sum.

```
A + B sums:  1−2 = −1,  1−1 = 0,  2−2 = 0,  2−1 = 1     →  {−1: 1, 0: 2, 1: 1}

C + D:  −1+0 = −1  → need  1 → 1 way
        −1+2 =  1  → need −1 → 1 way
         2+0 =  2  → need −2 → 0
         2+2 =  4  → need −4 → 0
total = 2
```

### Why the map stores counts, not just "seen"
Several (i, j) pairs can give the same sum (here 0 appears twice), and each one forms a separate tuple. So the map counts them.

### The general idea
Meet in the middle: split the work in half, enumerate each half, and join the halves with a hash lookup. It also turns subset-sum with n ≈ 40 from 2⁴⁰ into 2 · 2²⁰.

---

## 3. Java concepts used in this problem

### 3.1 Nested for-each on one line

```java
for (int x : a) for (int y : b) pairSums.merge(x + y, 1, Integer::sum);
```

Two loops without braces: the inner loop is the body of the outer one. It's compact, but use braces once the body grows.

### 3.2 `getOrDefault` for "how many ways"

```java
count += pairSums.getOrDefault(-(x + y), 0);
```

A missing sum means 0 ways. `get` would return `null`, and `count += null` would throw a NullPointerException.

### 3.3 Overflow check

|values| ≤ 2²⁸, so a pair sum is at most 2²⁹ and fits easily in `int`. The final count is at most n⁴ = 1.6 · 10⁹, which is still below 2³¹ − 1 ≈ 2.1 · 10⁹. (LeetCode guarantees the answer fits in an int.)

---

## 4. Approach 1: Brute force

### Code ([M04_FourSumII.java:46-50](M04_FourSumII.java#L46-L50))

```java
static int brute(int[] a, int[] b, int[] c, int[] d) {
    int n = 0;
    for (int w : a) for (int x : b) for (int y : c) for (int z : d) if (w + x + y + z == 0) n++;
    return n;
}
```

Every combination: **O(n⁴)**. It's the file's test oracle.

---

## 5. Approach 2: Three loops + a map of the last array

```java
static int fourSumCountN3(int[] a, int[] b, int[] c, int[] d) {
    Map<Integer, Integer> dCount = new HashMap<>();
    for (int z : d) dCount.merge(z, 1, Integer::sum);
    int count = 0;
    for (int w : a) for (int x : b) for (int y : c) count += dCount.getOrDefault(-(w + x + y), 0);
    return count;
}
```

Hashing just one array removes one loop: **O(n³)**, 8 million for n = 200. It's better, but not balanced. Splitting **2 + 2** is optimal.

---

## 6. Approach 3: Meet in the middle (the file's solution)

### Code ([M04_FourSumII.java:38-44](M04_FourSumII.java#L38-L44))

```java
static int fourSumCount(int[] a, int[] b, int[] c, int[] d) {
    Map<Integer, Integer> pairSums = new HashMap<>();
    for (int x : a) for (int y : b) pairSums.merge(x + y, 1, Integer::sum);
    int count = 0;
    for (int x : c) for (int y : d) count += pairSums.getOrDefault(-(x + y), 0);
    return count;
}
```

### Line by line

| Line | What it does |
|---|---|
| `pairSums.merge(x + y, 1, Integer::sum)` | count how many (i, j) pairs give each left sum |
| `for (int x : c) for (int y : d)` | every right pair |
| `pairSums.getOrDefault(-(x + y), 0)` | the number of left pairs that cancel this right pair |
| `count += …` | each such left pair is one tuple |

### Dry run: the example

Phase 1 (A × B):

| a | b | a + b | map after |
|---|---|---|---|
| 1 | −2 | −1 | {−1: 1} |
| 1 | −1 | 0 | {−1: 1, 0: 1} |
| 2 | −2 | 0 | {−1: 1, 0: 2} |
| 2 | −1 | 1 | {−1: 1, 0: 2, 1: 1} |

Phase 2 (C × D):

| c | d | c + d | need | ways | count |
|---|---|---|---|---|---|
| −1 | 0 | −1 | 1 | 1 | 1 |
| −1 | 2 | 1 | −1 | 1 | 2 |
| 2 | 0 | 2 | −2 | 0 | 2 |
| 2 | 2 | 4 | −4 | 0 | 2 |

**Return 2** ✓

### Dry run: all zeros, n = 1
map = {0: 1}; c + d = 0 → need 0 → 1 → **1** ✓

### Complexity
- **Time O(n²):** two phases of n² operations each.
- **Space O(n²):** up to n² distinct pair sums.

---

## 7. Comparing the approaches

| | Brute force | Hash one array | Meet in the middle |
|---|---|---|---|
| Time | O(n⁴) | O(n³) | **O(n²)** |
| Space | O(1) | O(n) | O(n²) |
| n = 200 | 1.6 · 10⁹ | 8 · 10⁶ | 8 · 10⁴ |

---

## 8. Edge cases and common mistakes

**Edge cases:** n = 1, all zeros (every tuple counts: n⁴), no solution, many equal pair sums.

**Mistakes:**
1. Storing a `Set` of the sums instead of counts (it misses multiplicities).
2. Looking up `x + y` instead of `−(x + y)`.
3. Confusing this with 4Sum (LC 18): there, the elements come from **one** array and you need distinct **quadruplets of values**, which calls for sorting + two pointers.

---

## 9. Interview questions and answers

**Q1. Why split 2 + 2 rather than 1 + 3?**
> The cost is the size of the larger half. 1 + 3 means n³ for the bigger half; 2 + 2 balances both at n².

**Q2. How does this relate to Two Sum?**
> Two Sum: for each x, look up target − x in a hash of the elements seen so far. Here, each "element" is a pair sum from A × B, and the target is −(c + d).

### Follow-up questions

**F1. k arrays instead of 4.**
> Split them into two groups of k/2 arrays; enumerate each group's sums (n^(k/2) each) and join with a hash map: O(n^(k/2)).

**F2. Memory is tight.**
> Sort the A + B sums and the C + D sums into two arrays and count matches with two pointers (one from each end): O(n² log n) time, but no hash-map overhead.

**F3. Subset Sum with n = 40 (meet in the middle).**
> Enumerate all 2²⁰ subset sums of each half, sort one half, and binary-search for complements.

---

## 10. Running the file

```bash
java M04_FourSumII.java      # 3/3 tests passed (including 200 random cases vs O(n⁴))
```

**One-line summary:** *"Hash the counts of all A + B sums, then for each C + D add the count of −(C + D): O(n⁴) becomes O(n²)."*
