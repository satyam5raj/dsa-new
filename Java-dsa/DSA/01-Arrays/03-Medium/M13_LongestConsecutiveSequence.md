# M13 · Longest Consecutive Sequence

| | |
|---|---|
| **LeetCode** | [#128 Longest Consecutive Sequence](https://leetcode.com/problems/longest-consecutive-sequence/) |
| **Difficulty** | Medium |
| **Pattern** | HashSet + "only start counting at the beginning of a run" |
| **Code** | [M13_LongestConsecutiveSequence.java](M13_LongestConsecutiveSequence.java) |
| **Run** | `java M13_LongestConsecutiveSequence.java` |

---

## 1. The problem

Given an **unsorted** array, return the length of the longest run of **consecutive integers** (x, x+1, x+2, …). The numbers **don't** need to be next to each other in the array. **It must run in O(n).**

| nums | longest run | answer |
|---|---|---|
| `[100, 4, 200, 1, 3, 2]` | 1, 2, 3, 4 | 4 |
| `[0,3,7,2,5,8,4,6,0,1]` | 0..8 | 9 |
| `[]` | — | 0 |
| `[1, 2, 0, 1]` | 0, 1, 2 (duplicates count once) | 3 |
| `[-2, -1, 0, 5]` | −2, −1, 0 | 3 |

**Constraints:** `0 ≤ n ≤ 10⁵`, values are any int.

---

## 2. How to think about it

1. **Sort:** sort, then count runs (skipping duplicates). O(n log n), which is **not allowed**.
2. **HashSet:** answers "does x exist?" in O(1).
3. **Naive with a set:** from **every** x, count x+1, x+2, … That re-walks the same run from each of its members (a run of length L is walked L times), so for one long run it's O(n²).
4. **The trick:** only start counting from x if **x is the start of a run**, i.e. **`x − 1` is not in the set**. Then each run is walked **exactly once**, from its first element.

```
set = {1, 2, 3, 4, 100, 200}
1: is 0 in the set? no  → START → count 2, 3, 4 → length 4
2: is 1 in the set? yes → not a start, skip
3: is 2 in the set? yes → skip
4: is 3 in the set? yes → skip
100: start → length 1
200: start → length 1
```

### Why O(n)?
The `while` loop only runs from run **starts**, and the runs don't overlap. So across all starts, the total number of `while` steps = the total of all run lengths = the number of distinct elements. Every element is touched O(1) times overall.

---

## 3. Java concepts used in this problem

### 3.1 Iterating over a `HashSet`

```java
for (int x : set) { ... }
```

You can for-each over any `Collection`. The **order is unspecified** for a `HashSet` (it depends on the hash buckets), but this algorithm doesn't care about order.

**Why iterate over the set rather than the array?** If the array had many duplicates of a run start (e.g. a thousand 1s), iterating over the array would walk that run a thousand times. The set has each value once.

### 3.2 Unboxing in `for (int x : set)`

The set holds `Integer` objects; declaring the loop variable as `int` unboxes each one automatically.

### 3.3 `set.contains(x - 1)` and overflow

If `x` is `Integer.MIN_VALUE`, then `x − 1` overflows to `Integer.MAX_VALUE`. That could make MIN look like it isn't a start if MAX is in the set, a rare edge case. Similarly, `x + length` can overflow past MAX. For a fully robust version, use `long` arithmetic. LeetCode's tests pass with `int`.

---

## 4. Approach 1: Sort and count runs

```java
static int longestConsecutiveSort(int[] nums) {
    if (nums.length == 0) return 0;
    int[] a = nums.clone();
    Arrays.sort(a);
    int best = 1, cur = 1;
    for (int i = 1; i < a.length; i++) {
        if (a[i] == a[i - 1]) continue;          // a duplicate: neither extends nor breaks the run
        if (a[i] == a[i - 1] + 1) cur++;         // consecutive: extend
        else cur = 1;                            // a gap: restart
        best = Math.max(best, cur);
    }
    return best;
}
```

**Dry run** `[1, 2, 0, 1]` → sorted `[0, 1, 1, 2]`:

| i | a[i−1] → a[i] | action | cur | best |
|---|---|---|---|---|
| 1 | 0 → 1 | consecutive | 2 | 2 |
| 2 | 1 → 1 | duplicate, skip | 2 | 2 |
| 3 | 1 → 2 | consecutive | 3 | 3 |

**Return 3.** **O(n log n)**: simple, but it breaks the O(n) requirement.

---

## 5. Approach 2: HashSet, counting only from run starts (the file's solution)

### Code ([M13_LongestConsecutiveSequence.java:48-61](M13_LongestConsecutiveSequence.java#L48-L61))

```java
static int longestConsecutive(int[] nums) {
    Set<Integer> set = new HashSet<>();
    for (int x : nums) set.add(x);

    int best = 0;
    for (int x : set) {
        if (!set.contains(x - 1)) {                 // x is the first number of a run
            int length = 1;
            while (set.contains(x + length)) length++;
            best = Math.max(best, length);
        }
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| `Set<Integer> set = new HashSet<>(); for ... set.add(x);` | all distinct values, with O(1) membership checks |
| `int best = 0;` | an empty array gives 0 |
| `for (int x : set)` | each distinct value once |
| `if (!set.contains(x - 1))` | there's no predecessor, so x **starts** a run. Otherwise skip x, because its run will be counted from the real start |
| `int length = 1; while (set.contains(x + length)) length++;` | walk forward: x+1, x+2, … while they exist |
| `best = Math.max(best, length);` | keep the longest |

### Dry run 1: `[100, 4, 200, 1, 3, 2]`

`set = {1, 2, 3, 4, 100, 200}` (shown sorted; the real iteration order may differ, and the result is the same)

| x | x−1 in set? | start? | walk | length | best |
|---|---|---|---|---|---|
| 1 | 0: no | **yes** | 2 ✓, 3 ✓, 4 ✓, 5 ✗ | 4 | 4 |
| 2 | 1: yes | no | — | | 4 |
| 3 | 2: yes | no | — | | 4 |
| 4 | 3: yes | no | — | | 4 |
| 100 | 99: no | yes | 101 ✗ | 1 | 4 |
| 200 | 199: no | yes | 201 ✗ | 1 | 4 |

**Return 4** ✓. The `while` loop ran 3 + 0 + 0 = 3 successful steps in total.

### Dry run 2: `[0, 3, 7, 2, 5, 8, 4, 6, 0, 1]`

`set = {0, 1, 2, 3, 4, 5, 6, 7, 8}`. Only 0 is a start (−1 isn't present). From 0, walk 1..8, then 9 is missing → length **9**. Every other value has a predecessor, so it's skipped. **Return 9** ✓

### Dry run 3: `[-2, -1, 0, 5]`
Starts: −2 (−3 absent) → −1 ✓, 0 ✓, 1 ✗ → length 3; 5 → length 1. **Return 3** ✓

### Complexity
- **Time O(n):** O(n) to build the set; each element is checked as a start once and walked at most once.
- **Space O(n)** for the set.

---

## 6. (Bonus) Approach 3: HashMap of run boundaries (union-style)

Process the numbers one by one, keeping, for each **endpoint** of a known run, that run's length:

```java
static int longestConsecutiveMap(int[] nums) {
    Map<Integer, Integer> len = new HashMap<>();   // value → length of the run it belongs to (correct at endpoints)
    int best = 0;
    for (int x : nums) {
        if (len.containsKey(x)) continue;          // a duplicate
        int left = len.getOrDefault(x - 1, 0);     // the length of the run ending at x−1
        int right = len.getOrDefault(x + 1, 0);    // the length of the run starting at x+1
        int total = left + 1 + right;              // x joins them
        len.put(x, total);
        len.put(x - left, total);                  // update the new left endpoint
        len.put(x + right, total);                 // update the new right endpoint
        best = Math.max(best, total);
    }
    return best;
}
```

It's O(n) and handles a **stream** of numbers (insertions one at a time). Only the endpoints need to be correct, because a new number can only attach at an endpoint. Union-Find ([11-Union-Find](../../11-Union-Find/README.md)) is another way to solve it.

---

## 7. Comparing the approaches

| | Sort | Set + run starts | Boundary map |
|---|---|---|---|
| Time | O(n log n) | **O(n)** | O(n) |
| Space | O(1)–O(n) | O(n) | O(n) |
| Works on a stream | no | no (needs all the data) | **yes** |

---

## 8. Edge cases and common mistakes

**Edge cases:** an empty array (0), all duplicates (`[5,5,5]` → 1), negatives, a single element, the extreme values MIN/MAX.

**Mistakes:**
1. Counting from **every** element without the start check: O(n²) for one long run.
2. Iterating over `nums` instead of the set, which redoes work for duplicated starts.
3. In the sort approach, treating duplicates as breaks (`[1,2,2,3]` should give 3, not 2).
4. Returning 1 for an empty array.

---

## 9. Interview questions and answers

**Q1. Why is the algorithm O(n) even though there's a while loop inside a for loop?**
> The while loop runs only from run starts, and runs don't overlap, so its total number of iterations across the whole execution equals the number of distinct values. An amortised O(n).

**Q2. Why check `x − 1` instead of `x + 1`?**
> Checking `x − 1` finds the smallest element of each run, and we walk upward from there. Checking `x + 1` (and walking downward) works symmetrically. You just need a consistent direction.

**Q3. Is HashSet O(1) here in the worst case?**
> On average. Adversarial hash collisions could degrade it; Java 8+ treeifies crowded buckets, so the worst case is O(log n) per operation.

### Follow-up questions

**F1. Return the sequence itself, not just its length.**
> Remember the `start` with the best length; the sequence is `start, start+1, …, start+best−1`.

**F2. Numbers arrive as a stream: report the longest run after each insertion.**
> The boundary-map approach (section 6) or Union-Find, with O(1)/near-O(1) per insertion.

**F3. The longest consecutive sequence in a binary tree (parent → child).**
> DFS, passing down (the parent's value, the current length); reset the length when `child != parent + 1`.

**F4. Consecutive in the array's ORDER (a contiguous increasing-by-1 subarray).**
> A different problem: a single pass with a running counter (like [E22](../02-Easy/E22_MaxConsecutiveOnes.md)).

**F5. Allow up to k gaps (missing numbers).**
> Sort the distinct values, then use a sliding window where the gaps counted are `(a[r] − a[l]) − (r − l)` ≤ k.

---

## 10. Running the file

```bash
java M13_LongestConsecutiveSequence.java      # 5/5 tests passed
```

**One-line summary:** *"Put everything in a set; only count upward from numbers whose predecessor is missing."*
