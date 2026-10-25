# M08 · Ugly Number II

| | |
|---|---|
| **LeetCode** | [#264 Ugly Number II](https://leetcode.com/problems/ugly-number-ii/) |
| **Difficulty** | Medium |
| **Pattern** | Generate in order with a min-heap (+ a set against duplicates), then the 3-pointer merge |
| **Code** | [M08_UglyNumberII.java](M08_UglyNumberII.java) |
| **Run** | `java M08_UglyNumberII.java` |

PriorityQueue basics are explained in [E01](../02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

**Ugly numbers** have no prime factors other than 2, 3 and 5: 1, 2, 3, 4, 5, 6, 8, 9, 10, 12, … Return the n-th one (1 is the first).

| n | answer |
|---|---|
| 10 | 12 |
| 1 | 1 |
| 1690 | 2,123,366,400 (the largest allowed) |

---

## 2. How to think about it

### Brute force: test every number
The 1690th ugly number is about 2.1 · 10⁹: far too many numbers to test one by one.

### Key insight: generate, don't test
Every ugly number except 1 is **2x, 3x or 5x of a smaller ugly number**. So generate them in increasing order.

### Approach 1: a min-heap
```
heap = {1}
repeat n times:  x = poll;  push 2x, 3x, 5x   (skip the ones already seen)
the i-th poll is the i-th ugly number
```
6 = 2 · 3 = 3 · 2 would appear twice, hence the "seen" set. Use `long`: 5x overflows `int` near the end.

### Approach 2: three pointers, O(n)
`ugly[0] = 1`. There are three sorted "streams": 2 · ugly[i2], 3 · ugly[i3], 5 · ugly[i5]. This is a **3-way merge**: take the smallest head,

```
next = min(2·ugly[i2], 3·ugly[i3], 5·ugly[i5])
```

and advance **every** pointer that produced `next`. That's how duplicates like 6 = 2·3 = 3·2 are skipped: both pointers move.

```
candidates 2, 3, 5    → 2  (i2++)            ugly [1, 2]
           4, 3, 5    → 3  (i3++)            [1, 2, 3]
           4, 6, 5    → 4  (i2++)            [1, 2, 3, 4]
           6, 6, 5    → 5  (i5++)            [1, 2, 3, 4, 5]
           6, 6, 10   → 6  (i2++ AND i3++)   [1, 2, 3, 4, 5, 6]
```

---

## 3. Java concepts used in this problem

### 3.1 `PriorityQueue<Long>` and `long` arithmetic

```java
PriorityQueue<Long> heap = new PriorityQueue<>();
heap.add(1L);                     // the L suffix makes it a long literal
```

The heap version pushes candidates larger than the answer (up to 5 × 2.1 · 10⁹), which overflows `int`. `long` holds them.

### 3.2 Looping over an array literal

```java
for (long f : new long[]{2, 3, 5}) if (seen.add(x * f)) heap.add(x * f);
```

`seen.add` returns false for duplicates (the same idiom as in the Hash Table topic), so each value enters the heap once.

### 3.3 `(int) x` at the end

The answer fits in an int (the problem guarantees it), so the final long is cast back.

### 3.4 Several independent `if`s (not `else if`)

```java
if (ugly[i] == a) i2++;
if (ugly[i] == b) i3++;
if (ugly[i] == c) i5++;
```

Separate `if`s, so **every** matching pointer advances. With `else if`, duplicates like 6 would be generated twice.

---

## 4. Approach 1: Min-heap + seen set

### Code ([M08_UglyNumberII.java:47-58](M08_UglyNumberII.java#L47-L58))

```java
static int nthUglyNumberHeap(int n) {
    PriorityQueue<Long> heap = new PriorityQueue<>();
    Set<Long> seen = new HashSet<>();
    heap.add(1L);
    seen.add(1L);
    long x = 1;
    for (int i = 0; i < n; i++) {
        x = heap.poll();                                      // the i-th ugly number
        for (long f : new long[]{2, 3, 5}) if (seen.add(x * f)) heap.add(x * f);
    }
    return (int) x;
}
```

### Line by line

| Line | What it does |
|---|---|
| `heap = {1}` | the smallest ugly number |
| `x = heap.poll()` | the next ugly number in increasing order |
| push 2x, 3x, 5x | its "children" (each is ugly too) |
| `seen.add(...)` | add each value only once |

### Dry run (the first polls)

| poll | heap after pushing its multiples |
|---|---|
| 1 | {2, 3, 5} |
| 2 | {3, 4, 5, 6, 10} |
| 3 | {4, 5, 6, 9, 10, 15} (6 is already there) |
| 4 | {5, 6, 8, 9, 10, 12, 15, 20} |
| … | the 10th poll is **12** |

### Complexity
- **Time O(n log n).**
- **Space O(n).**

---

## 5. Approach 2: Three pointers (the file's main solution)

### Code ([M08_UglyNumberII.java:60-72](M08_UglyNumberII.java#L60-L72))

```java
static int nthUglyNumber(int n) {
    int[] ugly = new int[n];
    ugly[0] = 1;
    int i2 = 0, i3 = 0, i5 = 0;
    for (int i = 1; i < n; i++) {
        int a = 2 * ugly[i2], b = 3 * ugly[i3], c = 5 * ugly[i5];
        ugly[i] = Math.min(a, Math.min(b, c));
        if (ugly[i] == a) i2++;                               // advance ALL that match
        if (ugly[i] == b) i3++;
        if (ugly[i] == c) i5++;
    }
    return ugly[n - 1];
}
```

### Line by line

| Line | What it does |
|---|---|
| `ugly[0] = 1` | the start of the sequence |
| `i2, i3, i5` | the next ugly number to multiply by 2, 3 and 5 |
| `a, b, c` | the three candidates |
| `ugly[i] = min(...)` | the next ugly number |
| the three `if`s | advance every stream that produced it |

**Can `5 * ugly[i5]` overflow here?** The pointers lag behind i, so `ugly[i5]` is far below the final answer; for n ≤ 1690 every candidate stays within int (the file's test for n = 1690 passes).

### Dry run: n = 10 (continuing the table above)

| i | candidates (2·, 3·, 5·) | ugly[i] | pointers advanced |
|---|---|---|---|
| 6 | 8, 9, 10 | 8 | i2 |
| 7 | 10, 9, 10 | 9 | i3 |
| 8 | 10, 12, 10 | 10 | i2 **and** i5 |
| 9 | 12, 12, 15 | 12 | i2 **and** i3 |

ugly = [1, 2, 3, 4, 5, 6, 8, 9, 10, **12**] ✓

### Complexity
- **Time O(n).**
- **Space O(n).**

---

## 6. Comparing the approaches

| | Test every number | Min-heap + set | Three pointers |
|---|---|---|---|
| Time | far too slow | O(n log n) | **O(n)** |
| Space | O(1) | O(n) | O(n) |
| Generalises to k primes | | yes | yes ([M09](M09_SuperUglyNumber.md)) |

The file checks both against testing every number for the first 300 ugly numbers.

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 1 (answer 1), n = 1690 (near the int limit).

**Mistakes:**
1. `else if` between the pointer updates (duplicates appear).
2. `int` overflow in the heap version.
3. Forgetting the seen set in the heap version (duplicates make the count wrong).

---

## 8. Interview questions and answers

**Q1. Why does every ugly number appear in one of the three streams?**
> An ugly number u > 1 has a factor 2, 3 or 5, so u = p · v with v a smaller ugly number. When the p-pointer reaches v, the stream produces u.

**Q2. Why advance all the matching pointers?**
> If two streams produce the same value (e.g. 6), it must be written once and both streams must move past it.

### Follow-up questions

**F1. Super Ugly Number: k primes.** → [M09](M09_SuperUglyNumber.md)
> k pointers, with a heap to pick the minimum in O(log k).

**F2. Is a single number ugly (LC 263)?**
> Divide out 2, 3 and 5 while possible; it's ugly iff the result is 1 (`isUgly` in the file).

**F3. Ugly Number III (LC 1201): divisible by a, b or c.**
> A different problem: binary search the answer with inclusion–exclusion counting.

---

## 9. Running the file

```bash
java M08_UglyNumberII.java      # 5/5 tests passed (including n = 1690 and the first 300 vs testing every number)
```

**One-line summary:** *"Merge the three sorted streams 2·ugly, 3·ugly, 5·ugly with three pointers, advancing every pointer that matches the minimum; or generate with a min-heap and a seen set."*
