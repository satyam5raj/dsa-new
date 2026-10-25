# M09 · Super Ugly Number

| | |
|---|---|
| **LeetCode** | [#313 Super Ugly Number](https://leetcode.com/problems/super-ugly-number/) |
| **Difficulty** | Medium |
| **Pattern** | [Ugly Number II](M08_UglyNumberII.md) with k primes → a k-way merge with a heap of the k pointers |
| **Code** | [M09_SuperUglyNumber.java](M09_SuperUglyNumber.java) |
| **Run** | `java M09_SuperUglyNumber.java` |

PriorityQueue basics are explained in [E01](../02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

**Super ugly numbers** only have prime factors from `primes` (sorted, up to 100 primes). Return the n-th one (1 is the first).

```
n = 12, primes [2, 7, 13, 19] → 32
sequence: 1, 2, 4, 7, 8, 13, 14, 16, 19, 26, 28, 32
```

| n | primes | answer |
|---|---|---|
| 12 | [2,7,13,19] | 32 |
| 1 | [2,3,5] | 1 |
| 10 | [2,3,5] | 12 (= Ugly Number II) |

---

## 2. How to think about it

[M08](M08_UglyNumberII.md) had 3 pointers and took the min of 3 candidates per step. With k primes:

### Approach 1: k pointers, scanning all k every step: O(n · k)
```
next = min over j of primes[j] · ugly[idx[j]]
advance every j whose candidate equals next
```

### Approach 2: a heap of the k streams: O(n log k)
Heap entries are `{value, prime, idx}`: stream j's current candidate. Each step:
- the heap's minimum is the next super ugly number (if it **duplicates** the last one written, just skip it)
- then advance **that** stream: push `{prime · ugly[idx + 1], prime, idx + 1}`

It's exactly the k-way merge from Merge k Sorted Lists ([Linked Lists H02](../../03-Linked-Lists/04-Hard/H02_MergeKSortedLists.md)), where "list j" is primes[j] × (the ugly sequence itself).

Use `long` for the candidates: prime · ugly can exceed int even though the answer fits.

---

## 3. Java concepts used in this problem

### 3.1 `PriorityQueue<long[]>` with `comparingLong`

```java
new PriorityQueue<long[]>(Comparator.comparingLong(e -> e[0]))
```

Each entry is a small `long[]` tuple; the comparator orders by the value (index 0).

### 3.2 A sequence that feeds itself

Each stream multiplies **earlier** elements of the `ugly` array. `idx + 1 < i` always holds when it's used, because every stream's index lags behind the number of values written so far. So the value it reads has already been computed.

### 3.3 Duplicates are skipped, but their streams still advance

When a polled value equals the last one written (e.g. 14 = 2 · 7 = 7 · 2), it isn't written again, but its stream still moves forward. Otherwise it would come back with the same value forever.

---

## 4. Approach 1: k pointers, scan all of them

### Code ([M09_SuperUglyNumber.java:61-72](M09_SuperUglyNumber.java#L61-L72))

```java
static int pointers(int n, int[] primes) {          // O(n * k) version
    long[] ugly = new long[n];
    ugly[0] = 1;
    int[] idx = new int[primes.length];
    for (int i = 1; i < n; i++) {
        long next = Long.MAX_VALUE;
        for (int j = 0; j < primes.length; j++) next = Math.min(next, primes[j] * ugly[idx[j]]);
        ugly[i] = next;
        for (int j = 0; j < primes.length; j++) if (primes[j] * ugly[idx[j]] == next) idx[j]++;
    }
    return (int) ugly[n - 1];
}
```

| Line | What it does |
|---|---|
| `idx[j]` | stream j's position in the ugly array |
| the first inner loop | the minimum candidate |
| the second inner loop | advance every stream that matches (skips duplicates) |

**O(n · k)**, with k ≤ 100 and n ≤ 10⁵ that's 10⁷: fine, and the file's oracle.

---

## 5. Approach 2: Heap of streams (the file's main solution)

### Code ([M09_SuperUglyNumber.java:43-59](M09_SuperUglyNumber.java#L43-L59))

```java
static int nthSuperUglyNumber(int n, int[] primes) {
    long[] ugly = new long[n];
    ugly[0] = 1;
    PriorityQueue<long[]> heap = new PriorityQueue<>(Comparator.comparingLong(e -> e[0]));   // {value, prime, idx}
    for (int p : primes) heap.offer(new long[]{p, p, 0});
    int i = 1;
    while (i < n) {
        long[] e = heap.poll();
        if (e[0] != ugly[i - 1]) {                                    // skip duplicates
            ugly[i++] = e[0];
            if (i == n) break;                                        // done: don't advance past the end
        }
        int idx = (int) e[2] + 1;                                     // < i: always already computed
        heap.offer(new long[]{e[1] * ugly[idx], e[1], idx});         // advance this stream
    }
    return (int) ugly[n - 1];
}
```

### Line by line

| Line | What it does |
|---|---|
| `{p, p, 0}` | stream p starts at p · ugly[0] = p |
| `e = heap.poll()` | the smallest candidate overall |
| `e[0] != ugly[i − 1]` | a new value → write it |
| `if (i == n) break;` | stop before advancing (the next ugly index might not exist yet) |
| `idx = e[2] + 1` | the next element of this stream's base sequence |
| `e[1] * ugly[idx]` | its new candidate |

### Dry run: n = 12, primes [2, 7, 13, 19] (from the real run)

| poll | value (prime, idx) | action | push |
|---|---|---|---|
| 1 | 2 (2, 0) | ugly[1] = 2 | 2 · ugly[1] = 4 |
| 2 | 4 (2, 1) | ugly[2] = 4 | 8 |
| 3 | 7 (7, 0) | ugly[3] = 7 | 7 · 2 = 14 |
| 4 | 8 (2, 2) | ugly[4] = 8 | 2 · 7 = 14 |
| 5 | 13 (13, 0) | ugly[5] = 13 | 26 |
| 6 | 14 (2, 3) | ugly[6] = 14 | 16 |
| 7 | 14 (7, 1) | **duplicate → skip** | 28 |
| 8 | 16 (2, 4) | ugly[7] = 16 | 26 |
| 9 | 19 (19, 0) | ugly[8] = 19 | 38 |
| 10 | 26 (2, 5) | ugly[9] = 26 | 28 |
| 11 | 26 (13, 1) | duplicate → skip | 52 |
| 12 | 28 (2, 6) | ugly[10] = 28 | 32 |
| 13 | 28 (7, 2) | duplicate → skip | 49 |
| 14 | 32 (2, 7) | ugly[11] = **32** → i == n → break | |

### Complexity
- **Time O(n log k)** (each step is one poll and one offer; duplicates add at most k extra steps per value).
- **Space O(n + k).**

---

## 6. Comparing the approaches

| | k pointers | Heap of streams |
|---|---|---|
| Time | O(n · k) | **O(n log k)** |
| Space | O(n + k) | O(n + k) |
| Duplicates | advance every match | skip when equal to the last written |

The file checks the heap against the pointers on 200 random prime sets (always including 2, 3 and 5 so the answers fit in an int).

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 1, a single prime (the powers of that prime), many duplicate products.

**Mistakes:**
1. Not advancing the stream of a skipped duplicate.
2. Advancing past the end after writing the last value (reading `ugly[idx]` before it exists).
3. `int` candidates overflowing.

---

## 8. Interview questions and answers

**Q1. Why is this a k-way merge?**
> Each prime p defines the sorted sequence p · ugly[0], p · ugly[1], … The super ugly numbers are the sorted union of these sequences (plus 1), which is exactly merging k sorted lists.

**Q2. When does the heap beat the pointers?**
> When k is large: O(log k) per step instead of O(k).

### Follow-up questions

**F1. Ugly Number II.** → [M08](M08_UglyNumberII.md)
> k = 3: three explicit pointers are simplest.

**F2. Merge k sorted lists.** → [Linked Lists H02](../../03-Linked-Lists/04-Hard/H02_MergeKSortedLists.md)

**F3. Kth smallest in a sorted matrix.** → [M06](M06_KthSmallestInSortedMatrix.md)
> The same "heap of row frontiers" merge.

---

## 9. Running the file

```bash
java M09_SuperUglyNumber.java      # 4/4 tests passed (including 200 random prime sets: heap == pointers)
```

**One-line summary:** *"k sorted streams prime · ugly[idx]: repeatedly poll the smallest from a heap, write it unless it's a duplicate, and advance that stream."*
