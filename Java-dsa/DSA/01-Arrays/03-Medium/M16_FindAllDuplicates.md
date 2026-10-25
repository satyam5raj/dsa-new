# M16 · Find All Duplicates in an Array

| | |
|---|---|
| **LeetCode** | [#442 Find All Duplicates in an Array](https://leetcode.com/problems/find-all-duplicates-in-an-array/) |
| **Difficulty** | Medium |
| **Pattern** | Index marking (sign flip), O(1) extra space |
| **Code** | [M16_FindAllDuplicates.java](M16_FindAllDuplicates.java) |
| **Run** | `java M16_FindAllDuplicates.java` |

---

## 1. The problem

`nums` has `n` integers, each in `[1, n]`. Each value appears **once or twice**. Return all the values that appear **twice**. Required: O(n) time, **O(1) extra space** (the output doesn't count).

| nums | answer |
|---|---|
| `[4, 3, 2, 7, 8, 2, 3, 1]` | `[2, 3]` |
| `[1, 1, 2]` | `[1]` |
| `[1]` | `[]` |
| `[2, 2, 1, 1]` | `[2, 1]` (in the order the second copy is found) |

**Constraints:** `1 ≤ n ≤ 10⁵`.

---

## 2. How to think about it

This is the twin of [E16 Find All Numbers Disappeared](../02-Easy/E16_FindAllNumbersDisappeared.md). The structure is the same: values `1..n`, `n` slots, so **value v has a home slot at index v − 1**, and we can use the **sign** of that slot as a "seen" flag.

- **First visit** of v: its home slot is positive → flip it negative (mark it seen).
- **Second visit** of v: its home slot is **already negative** → v is a **duplicate**.

```
for each x:
    idx = |x| − 1
    if nums[idx] < 0 → x is a duplicate
    else             → nums[idx] = −nums[idx]
```

`|x|` is needed because x itself may already have been flipped negative by an earlier element.

---

## 3. Java concepts used in this problem

### 3.1 Reusing a technique

This problem changes **one line** of E16's marking loop: instead of only marking, we **check before marking**. Recognising "this is the same pattern as..." is the most valuable skill in DSA prep. Review E16's notes on `Math.abs`, sign flipping, and why the trick needs the values to be in 1..n.

### 3.2 `dups.add(Math.abs(x))`

We add `|x|`, the real value, because `x` as read might be negative (it was flipped when its own home slot was visited earlier).

### 3.3 Cyclic sort (approach 3)

"Cyclic sort" places every value v at index v − 1 by repeatedly swapping. After it finishes, every slot whose value doesn't match its index holds an extra copy (a duplicate), and the value that **should** be there is missing. The technique also solves First Missing Positive (LC 41).

---

## 4. Approach 1: HashSet (O(n) space)

```java
static List<Integer> findDuplicatesSet(int[] nums) {
    Set<Integer> seen = new HashSet<>();
    List<Integer> dups = new ArrayList<>();
    for (int x : nums) if (!seen.add(x)) dups.add(x);
    return dups;
}
```

The [E02](../02-Easy/E02_ContainsDuplicate.md) trick: `add` returns false for a value already present. **O(n) time, O(n) space.**

---

## 5. Approach 2: Sign marking (the file's solution)

### Code ([M16_FindAllDuplicates.java:43-51](M16_FindAllDuplicates.java#L43-L51))

```java
static List<Integer> findDuplicates(int[] nums) {
    List<Integer> dups = new ArrayList<>();
    for (int x : nums) {
        int idx = Math.abs(x) - 1;
        if (nums[idx] < 0) dups.add(Math.abs(x));   // second visit
        else nums[idx] = -nums[idx];                // first visit: mark
    }
    return dups;
}
```

### Line by line

| Line | What it does |
|---|---|
| `for (int x : nums)` | read each value; it may already be negative (marked by someone else) |
| `int idx = Math.abs(x) - 1;` | the home slot of the value \|x\| |
| `if (nums[idx] < 0) dups.add(Math.abs(x));` | the home slot is already marked → this is the second time we see \|x\| |
| `else nums[idx] = -nums[idx];` | the first time → mark it |
| `return dups;` | |

Since each value appears at most twice, every duplicate is reported exactly once (on its second visit).

### Dry run 1: `[4, 3, 2, 7, 8, 2, 3, 1]`

| step | x (as read) | \|x\| | idx | nums[idx] | action | array after |
|---|---|---|---|---|---|---|
| 1 | 4 | 4 | 3 | 7 > 0 | flip | `[4,3,2,-7,8,2,3,1]` |
| 2 | 3 | 3 | 2 | 2 > 0 | flip | `[4,3,-2,-7,8,2,3,1]` |
| 3 | −2 | 2 | 1 | 3 > 0 | flip | `[4,-3,-2,-7,8,2,3,1]` |
| 4 | −7 | 7 | 6 | 3 > 0 | flip | `[4,-3,-2,-7,8,2,-3,1]` |
| 5 | 8 | 8 | 7 | 1 > 0 | flip | `[4,-3,-2,-7,8,2,-3,-1]` |
| 6 | 2 | 2 | 1 | **−3 < 0** | **duplicate 2** | unchanged |
| 7 | −3 | 3 | 2 | **−2 < 0** | **duplicate 3** | unchanged |
| 8 | −1 | 1 | 0 | 4 > 0 | flip | `[-4,-3,-2,-7,8,2,-3,-1]` |

**Return `[2, 3]`** ✓

### Dry run 2: `[2, 2, 1, 1]`

| x | idx | nums[idx] | action | array |
|---|---|---|---|---|
| 2 | 1 | 2 | flip | `[2,-2,1,1]` |
| −2 | 1 | −2 | **dup 2** | |
| 1 | 0 | 2 | flip | `[-2,-2,1,1]` |
| 1 | 0 | −2 | **dup 1** | |

**Return `[2, 1]`** ✓

### Complexity
- **Time O(n):** one pass.
- **Space O(1) extra.** The input is modified; restore it with `nums[i] = Math.abs(nums[i])` if needed.

---

## 6. Approach 3: Cyclic sort

```java
static List<Integer> findDuplicatesCyclic(int[] nums) {
    int i = 0;
    while (i < nums.length) {
        int home = nums[i] - 1;
        if (nums[i] != nums[home]) {                  // not home yet, and home holds something else
            int t = nums[i]; nums[i] = nums[home]; nums[home] = t;
        } else {
            i++;
        }
    }
    List<Integer> dups = new ArrayList<>();
    for (int k = 0; k < nums.length; k++) if (nums[k] != k + 1) dups.add(nums[k]);
    return dups;
}
```

- After sorting, value v sits at index v − 1 whenever possible. A slot k with `nums[k] != k + 1` holds an extra copy of `nums[k]`, which is a duplicate (and value k + 1 is missing).
- **O(n) time** (each swap places a value at home for good, so there are at most n swaps), **O(1) space**.

**Dry run** `[4,3,2,7,8,2,3,1]` → after the cyclic sort: `[1,2,3,4,3,2,7,8]`. Index 4 holds 3 (≠ 5) and index 5 holds 2 (≠ 6) → the duplicates are **[3, 2]** (the same set, in a different order).

---

## 7. Comparing the approaches

| | HashSet | Sign marking | Cyclic sort |
|---|---|---|---|
| Time | O(n) | O(n) | O(n) |
| Extra space | O(n) | **O(1)** | **O(1)** |
| Input after | unchanged | negated (restorable) | reordered |
| Output order | order of the 2nd occurrence | order of the 2nd occurrence | by index |

---

## 8. Edge cases and common mistakes

**Edge cases:** no duplicates, all values duplicated (`[1,1,2,2]`), n = 1, the duplicate at the very end.

**Mistakes:**
1. Forgetting `Math.abs` on x, which gives a negative index.
2. Adding `x` instead of `|x|` to the result (it can be negative).
3. Applying this to values that can appear **three or more** times: the sign approach would report a value at the 2nd and the 4th visit. (With at most twice, it's exact.)
4. Using the trick when values aren't guaranteed to be in 1..n.

---

## 9. Interview questions and answers

**Q1. How do you get O(1) space?**
> The values map to indices (v → v−1) and are all positive, so the sign of `nums[v−1]` can store "seen v". The second time I see v, its slot is already negative.

**Q2. How is this different from E16?**
> E16 marks, then reports the **unmarked** slots (the missing values). Here I report a value at the moment I find its slot **already marked** (the duplicates). It's the same marking pass.

**Q3. Can you avoid modifying the input?**
> Not in O(1) space and O(n) time for this problem. With a single duplicate (LC 287), Floyd's cycle detection works without modifying anything.

### Follow-up questions

**F1. Exactly one duplicate, which may repeat many times; don't modify the array; O(1) space (LC 287).**
> Treat `i → nums[i]` as a linked list; the duplicate is the entry point of the cycle. Use Floyd's tortoise and hare: O(n) time, O(1) space.

**F2. Set Mismatch (LC 645): one value duplicated, one missing.**
> Marking: an already-negative slot gives the duplicate; the slot that stays positive gives the missing value.

**F3. First Missing Positive (LC 41).**
> Cyclic sort over the values in 1..n (ignore the rest), then the first index with `nums[i] != i+1` gives the answer.

**F4. Values appear up to k times; report those appearing more than once.**
> Use the "add n" marking: `nums[|x|−1] += n` per occurrence, and then a slot ≥ 2n (after accounting for its own value) means 2+ occurrences. Or just use a count map.

---

## 10. Running the file

```bash
java M16_FindAllDuplicates.java      # 4/4 tests passed
```

**One-line summary:** *"Value v lives at index v−1: flip it negative on the first visit; already negative means v is a duplicate."*
