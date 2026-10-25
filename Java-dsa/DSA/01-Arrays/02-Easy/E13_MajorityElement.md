# E13 · Majority Element

| | |
|---|---|
| **LeetCode** | [#169 Majority Element](https://leetcode.com/problems/majority-element/) |
| **Difficulty** | Easy |
| **Pattern** | Boyer-Moore voting (a famous O(1)-space trick) |
| **Code** | [E13_MajorityElement.java](E13_MajorityElement.java) |
| **Run** | `java E13_MajorityElement.java` |

---

## 1. The problem

Return the element that appears **more than ⌊n/2⌋ times**. It's **guaranteed** to exist.

| nums | n/2 | answer | count |
|---|---|---|---|
| `[3, 2, 3]` | 1 | 3 | 2 > 1 |
| `[2, 2, 1, 1, 1, 2, 2]` | 3 | 2 | 4 > 3 |
| `[1]` | 0 | 1 | 1 > 0 |
| `[6, 5, 5]` | 1 | 5 | 2 > 1 |

**Constraints:** `1 ≤ n ≤ 5 × 10⁴`. LeetCode's follow-up: *O(n) time and O(1) space?*

---

## 2. How to think about it

1. **Count everything:** a HashMap of `value → count`. Return the one with count > n/2. O(n) time, O(n) space.
2. **Sort:** the majority element fills more than half the array, so after sorting it **must cover the middle index n/2**, whatever value range it sits in. O(n log n).
3. **Boyer-Moore voting:** pair each majority element with a **different** element and let them cancel out. Because the majority has more than half of all elements, it can't be fully cancelled; something of it always survives. O(n) time, **O(1)** space.

### The intuition behind Boyer-Moore
Picture an election where each voter for candidate X can "knock out" one voter for someone else (both leave). If X has more than half the votes, then even if every other voter fights X, X's supporters outnumber them all, so X is the last one standing.

The algorithm simulates this with a `candidate` and a `count`:
- `count == 0` → nobody is "standing", so the current element becomes the new candidate.
- the same as the candidate → `count++` (one more supporter).
- different → `count--` (one supporter and one opponent cancel each other).

---

## 3. Java concepts used in this problem

### 3.1 Ternary operator `cond ? a : b`

```java
count += (x == candidate) ? 1 : -1;
// is the same as:
if (x == candidate) count += 1; else count -= 1;
```

The ternary is an **expression** (it produces a value), so it fits inside an assignment. Use it for short, simple choices; nested ternaries become hard to read.

### 3.2 `Map.merge` returns the new value

```java
if (count.merge(x, 1, Integer::sum) > nums.length / 2) return x;
```

`merge` stores the updated count **and returns it**, so we can check the threshold in the same line and stop early as soon as some value crosses n/2. (See E11 for how `merge` works.)

### 3.3 Integer division

`nums.length / 2` with `int`s **drops the decimal part**: `7 / 2 = 3`, `3 / 2 = 1`. That's exactly ⌊n/2⌋ (the floor). To get the ceiling instead: `(n + 1) / 2`.

### 3.4 Throwing an exception: `throw new IllegalArgumentException("no majority");`

When a method can't return a meaningful value, it can **throw** an exception, which stops the method and reports the error to the caller.
- `IllegalArgumentException` is the standard "the input breaks the rules" error.
- It's an **unchecked** exception (a subclass of `RuntimeException`), so the method doesn't need to declare it with `throws`.
- Java also requires every code path to return or throw. Here, after the loop, we "know" that can't happen, and `throw` satisfies the compiler.

---

## 4. Approach 1: Count map

### Code ([E13_MajorityElement.java:48-54](E13_MajorityElement.java#L48-L54))

```java
static int majorityElementMap(int[] nums) {
    Map<Integer, Integer> count = new HashMap<>();
    for (int x : nums) {
        if (count.merge(x, 1, Integer::sum) > nums.length / 2) return x;
    }
    throw new IllegalArgumentException("no majority");
}
```

| Line | What it does |
|---|---|
| `Map<Integer, Integer> count = new HashMap<>();` | value → how many times we've seen it |
| `count.merge(x, 1, Integer::sum)` | increment x's count and get the new count |
| `> nums.length / 2` | crossed the majority threshold → return immediately |
| `throw ...` | only if no majority exists (not possible with this problem's guarantee) |

**Dry run** `[2, 2, 1, 1, 1, 2, 2]`, threshold n/2 = 3:

| x | count after | > 3? |
|---|---|---|
| 2 | {2:1} | no |
| 2 | {2:2} | no |
| 1 | {2:2, 1:1} | no |
| 1 | {2:2, 1:2} | no |
| 1 | {2:2, 1:3} | no (3 is not > 3) |
| 2 | {2:3, 1:3} | no |
| 2 | {2:**4**, 1:3} | **yes → return 2** |

**O(n) time, O(n) space** (up to about n/2 distinct keys).

---

## 5. Approach 2: Sort, take the middle

```java
static int majorityElementSort(int[] nums) {
    int[] a = nums.clone();
    Arrays.sort(a);
    return a[a.length / 2];
}
```

**Why the middle?** The majority value takes up **more than n/2 consecutive** slots after sorting. Wherever that block starts (as far left as index 0, or as far right as the end), a block longer than half the array has to cover index n/2.

```
n = 7, majority count = 4 (needs > 3)
block at the far left : [M M M M x x x]   index 3 = M
block at the far right: [x x x M M M M]   index 3 = M
```

**Dry run** `[2,2,1,1,1,2,2]` → sorted `[1,1,1,2,2,2,2]` → `a[3] = 2` ✓
**O(n log n) time.**

---

## 6. Approach 3: Boyer-Moore voting (optimal)

### Code ([E13_MajorityElement.java:56-63](E13_MajorityElement.java#L56-L63))

```java
static int majorityElement(int[] nums) {
    int candidate = 0, count = 0;
    for (int x : nums) {
        if (count == 0) candidate = x;
        count += (x == candidate) ? 1 : -1;
    }
    return candidate;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int candidate = 0, count = 0;` | no candidate yet. The 0 is a placeholder; it gets replaced on the first element because count is 0 |
| `for (int x : nums)` | one pass |
| `if (count == 0) candidate = x;` | everything before has cancelled out → start fresh with x as the candidate |
| `count += (x == candidate) ? 1 : -1;` | a supporter adds one; an opponent cancels one |
| `return candidate;` | the survivor. Guaranteed to be the majority **if one exists** |

### Dry run 1: `[2, 2, 1, 1, 1, 2, 2]`

| x | count == 0? | candidate | x == candidate? | count after |
|---|---|---|---|---|
| 2 | yes → candidate = 2 | 2 | yes | 1 |
| 2 | no | 2 | yes | 2 |
| 1 | no | 2 | no | 1 |
| 1 | no | 2 | no | 0 |
| 1 | **yes → candidate = 1** | 1 | yes | 1 |
| 2 | no | 1 | no | 0 |
| 2 | **yes → candidate = 2** | 2 | yes | 1 |

**Return 2** ✓. Notice the candidate was temporarily 1, but the majority won in the end.

### Dry run 2: `[3, 2, 3]`

| x | candidate | count after |
|---|---|---|
| 3 | 3 (new) | 1 |
| 2 | 3 | 0 |
| 3 | 3 (new, because count was 0) | 1 |

**Return 3** ✓

### Dry run 3: `[6, 5, 5]`

| x | candidate | count after |
|---|---|---|
| 6 | 6 (new) | 1 |
| 5 | 6 | 0 |
| 5 | 5 (new) | 1 |

**Return 5** ✓

### Why is it correct? (the proof interviewers like)
Split the array into the segments where `count` drops back to 0. In each finished segment, the candidate had exactly as many supporters as opponents, so **at most half** of that segment is the true majority M. Since M is **more** than half of the whole array, but at most half of every finished segment, M must be **more** than half of the **last, unfinished** segment. And in that segment, the candidate is the value that appears most (count > 0), which must be M.

### Complexity
- **Time O(n):** one pass.
- **Space O(1):** two integers.

### ⚠ When a majority is NOT guaranteed
The candidate might be wrong: `[1, 2, 3]` → the candidate ends as 3, but nothing is a majority. Add a **verification pass**:

```java
static int majorityOrMinusOne(int[] nums) {
    int cand = majorityElement(nums), c = 0;
    for (int x : nums) if (x == cand) c++;
    return c > nums.length / 2 ? cand : -1;
}
```

---

## 7. (Bonus) Bit counting

For each of the 32 bit positions, count how many numbers have that bit set. The majority's bits win every position (more than n/2):

```java
static int majorityBits(int[] nums) {
    int res = 0;
    for (int b = 0; b < 32; b++) {
        int ones = 0;
        for (int x : nums) if (((x >> b) & 1) == 1) ones++;
        if (ones > nums.length / 2) res |= (1 << b);
    }
    return res;
}
```

- `(x >> b) & 1` reads bit `b` of `x`.
- `res |= (1 << b)` sets bit `b` in `res`.
- **O(32n) = O(n) time, O(1) space.** It works for negative numbers too, because bit 31 is the sign bit.

---

## 8. Comparing the approaches

| | Count map | Sort | Boyer-Moore | Bits |
|---|---|---|---|---|
| Time | O(n) | O(n log n) | **O(n)** | O(32n) |
| Space | O(n) | O(log n) | **O(1)** | O(1) |
| Needs the guarantee? | no | yes (or verify) | yes (or verify) | yes |

---

## 9. Edge cases and common mistakes

**Edge cases:** a single element, n = 2 (`[1, 1]`), the majority only at the end, negative values.

**Mistakes:**
1. Setting the candidate **after** the count update, or forgetting to reset it when count reaches 0.
2. Returning the candidate without verifying when there's no guarantee.
3. Using `>=` n/2 instead of `>` n/2. With n = 4, a value appearing exactly twice is **not** a majority.

---

## 10. Interview questions and answers

**Q1. Explain Boyer-Moore voting intuitively.**
> Each element of the majority can cancel one non-majority element. Because the majority is more than half, after all the cancellations some majority elements remain. The algorithm does this cancelling with a single counter.

**Q2. What does the candidate mean in the middle of the scan?**
> Nothing definitive. It's only the survivor of the current segment. Only the final candidate matters, and only if a majority exists.

**Q3. Can you use it to find the element that appears most often (the mode)?**
> No. Boyer-Moore only works for a strict majority (> n/2). For the mode, use a count map.

### Follow-up questions

**F1. Majority Element II: all elements appearing more than n/3 times (LC 229).** → [M15](../03-Medium/M15_MajorityElementII.md)
> At most **two** such elements can exist. Keep two candidates and two counters, then verify both with a second pass. In general, for more than n/k, keep k−1 candidates.

**F2. A majority is not guaranteed.**
> Run Boyer-Moore, then count the candidate in a second pass; return it only if its count > n/2.

**F3. The data is a stream or is distributed across machines.**
> Boyer-Moore is streaming-friendly (one pass, O(1) memory). For distributed data, each machine computes its local candidate and count; the results can be **merged** like votes (the same candidates add up, different ones cancel), then a final verification pass.

**F4. Check whether a given target is the majority in a sorted array (LC 1150).**
> Binary search the first occurrence `i` of the target; it's a majority iff `i + n/2 < n` and `nums[i + n/2] == target`. O(log n).

**F5. Majority element in a range query `[l, r]`, asked many times (LC 1157).**
> Randomised: pick random positions in the range and check candidates with binary search over per-value position lists. Or a segment tree that stores Boyer-Moore (candidate, count) pairs, which **merge** like votes.

---

## 11. Running the file

```bash
java E13_MajorityElement.java      # 5/5 tests passed
```

**One-line summary:** *"Same value → +1, different → −1, count 0 → new candidate; the survivor is the majority."*
