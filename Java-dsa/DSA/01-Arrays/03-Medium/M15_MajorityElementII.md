# M15 · Majority Element II

| | |
|---|---|
| **LeetCode** | [#229 Majority Element II](https://leetcode.com/problems/majority-element-ii/) |
| **Difficulty** | Medium |
| **Pattern** | Extended Boyer-Moore voting (two candidates) |
| **Code** | [M15_MajorityElementII.java](M15_MajorityElementII.java) |
| **Run** | `java M15_MajorityElementII.java` |

---

## 1. The problem

Return **all** elements that appear **more than ⌊n/3⌋ times**. Follow-up: O(n) time and **O(1) space**.

| nums | n/3 | answer |
|---|---|---|
| `[3, 2, 3]` | 1 | `[3]` |
| `[1, 2]` | 0 | `[1, 2]` |
| `[1]` | 0 | `[1]` |
| `[1, 2, 3]` | 1 | `[]` (each appears once, and 1 is not > 1) |
| `[2, 2, 1, 1, 1, 2, 2]` | 2 | `[1, 2]` (2 appears 4 times, 1 appears 3 times) |
| `[0, 0, 0]` | 1 | `[0]` |

**Constraints:** `1 ≤ n ≤ 5 × 10⁴`.

---

## 2. How to think about it

### How many answers can there be?
**At most two.** Three different values, each with more than n/3 copies, would need more than n elements in total. So we only ever need to track **two candidates**.

### Generalising Boyer-Moore ([E13](../02-Easy/E13_MajorityElement.md))
In E13, one candidate cancelled out against one other element. Here, every "otherwise" step removes **three different values at once** (x plus one vote from each candidate). A value with more than n/3 copies can't be wiped out by such triple removals, so it must survive as one of the two candidates.

### Voting rules (the order matters!)
For each x:

1. `x == cand1` → `count1++`
2. `x == cand2` → `count2++`
3. `count1 == 0` → `cand1 = x`, `count1 = 1` (reuse the empty slot)
4. `count2 == 0` → `cand2 = x`, `count2 = 1`
5. otherwise → `count1--`, `count2--` (x cancels one vote from **each** candidate)

**Why check "is x one of my candidates?" before reusing an empty slot?** Otherwise a value equal to `cand2` could be put into the empty slot 1 as well, and then **both slots would hold the same value**, splitting its votes.

### A second pass is REQUIRED
The candidates are only **possible** answers. For `[1, 2, 3]`, two candidates remain, but nothing appears more than n/3 times. Count them again for real.

---

## 3. Java concepts used in this problem

### 3.1 The `if / else if` chain picks exactly one action

```java
if (x == cand1) count1++;
else if (x == cand2) count2++;
else if (count1 == 0) { cand1 = x; count1 = 1; }
else if (count2 == 0) { cand2 = x; count2 = 1; }
else { count1--; count2--; }
```

Only the **first** true condition runs. That's exactly how the priority order of the voting rules is enforced.

### 3.2 Different initial candidates: `cand1 = 0, cand2 = 1`

The two candidates start as **different** placeholder values with count 0. If both started as 0, an input of 0 would match `cand1` and could never become `cand2`. That's harmless for correctness here, but keeping them distinct guarantees the two slots never hold the same value.

**A subtle effect:** a placeholder can "match" an input with count 0. In `[1, 2, 3]` the first x = 1 matches the placeholder `cand2 = 1`, so `count2` becomes 1. That's the same as taking the empty slot, so it's fine.

### 3.3 `Collections.sort(list)`

It sorts a `List` in place (TimSort, stable, O(k log k)). Here it's used only so the test output order is predictable. `Collections` (with an s) is the helper class for collections, like `Arrays` for arrays.

---

## 4. Approach 1: Count map

```java
static List<Integer> majorityElementMap(int[] nums) {
    Map<Integer, Integer> count = new HashMap<>();
    for (int x : nums) count.merge(x, 1, Integer::sum);
    List<Integer> res = new ArrayList<>();
    for (Map.Entry<Integer, Integer> e : count.entrySet())
        if (e.getValue() > nums.length / 3) res.add(e.getKey());
    Collections.sort(res);
    return res;
}
```

- `count.entrySet()` gives the key-value pairs; `Map.Entry` has `getKey()` and `getValue()`. That's the standard way to loop over a map.
- **O(n) time, O(n) space**, which breaks the follow-up.

---

## 5. Approach 2: Two-candidate Boyer-Moore (the file's solution)

### Code ([M15_MajorityElementII.java:41-62](M15_MajorityElementII.java#L41-L62))

```java
static List<Integer> majorityElement(int[] nums) {
    int cand1 = 0, cand2 = 1, count1 = 0, count2 = 0;    // different initial candidates
    for (int x : nums) {
        if (x == cand1) count1++;
        else if (x == cand2) count2++;
        else if (count1 == 0) { cand1 = x; count1 = 1; }
        else if (count2 == 0) { cand2 = x; count2 = 1; }
        else { count1--; count2--; }
    }
    // verify the candidates
    count1 = 0;
    count2 = 0;
    for (int x : nums) {
        if (x == cand1) count1++;
        else if (x == cand2) count2++;
    }
    List<Integer> result = new ArrayList<>();
    if (count1 > nums.length / 3) result.add(cand1);
    if (count2 > nums.length / 3) result.add(cand2);
    Collections.sort(result);                             // only for predictable test output
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int cand1 = 0, cand2 = 1, count1 = 0, count2 = 0;` | two empty candidate slots, with distinct placeholder values |
| pass 1 `if ... else if ...` | apply the voting rules in priority order |
| `count1 = 0; count2 = 0;` | reset for the verification pass |
| pass 2 | count the real occurrences of each candidate. `else if` is safe because cand1 ≠ cand2 always |
| `if (count1 > n / 3) result.add(cand1);` | keep only the real answers |
| `Collections.sort(result);` | a deterministic order for the tests |

### Dry run 1: `[2, 2, 1, 1, 1, 2, 2]` (n/3 = 2)

**Pass 1:**

| x | rule applied | cand1 (count1) | cand2 (count2) |
|---|---|---|---|
| start | | 0 (0) | 1 (0) |
| 2 | slot 1 empty → take it | 2 (1) | 1 (0) |
| 2 | x == cand1 | 2 (2) | 1 (0) |
| 1 | x == cand2 (the placeholder 1 matches) | 2 (2) | 1 (1) |
| 1 | x == cand2 | 2 (2) | 1 (2) |
| 1 | x == cand2 | 2 (2) | 1 (3) |
| 2 | x == cand1 | 2 (3) | 1 (3) |
| 2 | x == cand1 | 2 (4) | 1 (3) |

**Pass 2:** real count of 2 = 4 > 2 ✓, real count of 1 = 3 > 2 ✓ → **`[1, 2]`** ✓

### Dry run 2: `[1, 2, 3]` (n/3 = 1) (why verification is needed)

| x | rule | cand1 (count1) | cand2 (count2) |
|---|---|---|---|
| 1 | x == cand2 (placeholder) | 0 (0) | 1 (1) |
| 2 | slot 1 empty → take it | 2 (1) | 1 (1) |
| 3 | neither matches, no empty slot → **cancel both** | 2 (0) | 1 (0) |

The candidates are 2 and 1. **Pass 2:** each appears once, and 1 is not > 1 → **`[]`** ✓

### Dry run 3: `[3, 2, 3]` (n/3 = 1)

| x | rule | cand1 | cand2 |
|---|---|---|---|
| 3 | take slot 1 | 3 (1) | 1 (0) |
| 2 | take slot 2 | 3 (1) | 2 (1) |
| 3 | x == cand1 | 3 (2) | 2 (1) |

Pass 2: 3 appears 2 times > 1 ✓; 2 appears 1 time ✗ → **`[3]`** ✓

### Dry run 4: `[0, 0, 0]`
`cand1` starts as 0, so every x matches it: count1 = 3. Verification: 3 > 1 → **`[0]`** ✓ (this is the test for "the initial cand1 = 0 is a real value").

### Complexity
- **Time O(n):** two passes.
- **Space O(1):** four integers (the result has at most 2 elements).

---

## 6. Comparing the approaches

| | Count map | Two-candidate voting |
|---|---|---|
| Time | O(n) | **O(n)** |
| Space | O(n) | **O(1)** |
| Needs a verification pass | no | **yes** |

### Generalisation
For elements appearing **more than n/k** times: keep **k − 1** candidates (there can be at most k − 1 such elements). The "otherwise" step decrements all k − 1 counters (removing k distinct values at once). Then verify. This is the **Misra-Gries** algorithm, used for finding frequent items in data streams.

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 1 or 2, no answer, two answers, inputs equal to the placeholder values (0 or 1), negative numbers.

**Mistakes:**
1. Checking the empty slots **before** checking candidate matches, which can make both slots hold the same value.
2. Skipping the verification pass.
3. Using `>= n/3` instead of `> n/3`.
4. Starting both candidates with the same placeholder, then adding a duplicate value to the result.

---

## 8. Interview questions and answers

**Q1. Why at most two answers?**
> Each answer needs more than n/3 occurrences. Three answers would need more than n elements in total, which is impossible.

**Q2. Why does the voting work?**
> Each "cancel both" step removes three distinct values. A value with more than n/3 copies can be removed at most once per triple, and there are fewer than n/3 such triples involving it (each triple uses up 3 elements), so it survives as a candidate.

**Q3. Why is the second pass required?**
> Voting guarantees that the real answers are among the candidates, not that the candidates are answers. Inputs with no frequent element still leave candidates behind.

### Follow-up questions

**F1. Elements appearing more than n/k times.**
> Misra-Gries: k − 1 candidates, then verification. O(n·k) time, O(k) space.

**F2. Top-k frequent elements (LC 347).** → [06-Hash-Table M01](../../06-Hash-Table/03-Medium/M01_TopKFrequentElements.md)
> A count map plus a min-heap of size k, or bucket sort by frequency. O(n log k) or O(n).

**F3. Approximate heavy hitters in a huge stream.**
> Misra-Gries with a fixed number of counters, or Count-Min Sketch. These are probabilistic or approximate structures with bounded memory.

**F4. Majority element in a sorted array (more than n/3).**
> Check the candidates at positions n/3, 2n/3 (any value with more than n/3 copies must cover one of those positions), and verify each by binary-searching its first and last occurrence: O(log n).

---

## 9. Running the file

```bash
java M15_MajorityElementII.java      # 6/6 tests passed
```

**One-line summary:** *"At most two answers ⇒ two candidates; match, then fill an empty slot, else cancel both; always verify."*
