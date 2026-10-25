# E19 · Monotonic Array

| | |
|---|---|
| **LeetCode** | [#896 Monotonic Array](https://leetcode.com/problems/monotonic-array/) |
| **Difficulty** | Easy |
| **Pattern** | Single pass with flags (rule possibilities out) |
| **Code** | [E19_MonotonicArray.java](E19_MonotonicArray.java) |
| **Run** | `java E19_MonotonicArray.java` |

---

## 1. The problem

An array is **monotonic** if it is entirely **non-decreasing** (each element ≥ the previous one) **or** entirely **non-increasing** (each element ≤ the previous one). Return whether `nums` is monotonic.

| nums | answer | why |
|---|---|---|
| `[1, 2, 2, 3]` | true | non-decreasing (equal neighbours are allowed) |
| `[6, 5, 4, 4]` | true | non-increasing |
| `[1, 3, 2]` | false | goes up, then down |
| `[5, 5, 5]` | true | both at once! |
| `[1]` | true | trivially |

**Constraints:** `1 ≤ n ≤ 10⁵`.

"Non-decreasing" is not quite "increasing": `[1, 2, 2]` is non-decreasing, but not strictly increasing.

---

## 2. How to think about it

- **Two-pass idea:** check "is it non-decreasing?" with one loop, then "is it non-increasing?" with another. That works, and it's O(n).
- **One-pass idea:** **assume both are possible**, and **rule them out** as evidence appears:
  - a step **up** (`a[i] > a[i−1]`) proves it's **not non-increasing** → `decreasing = false`
  - a step **down** (`a[i] < a[i−1]`) proves it's **not non-decreasing** → `increasing = false`
  - an **equal** step proves nothing
- At the end, the answer is `increasing || decreasing`. Once both are false, we can stop early.

"Start with all options open and eliminate them" is a handy pattern for yes/no classification problems.

---

## 3. Java concepts used in this problem

### 3.1 `boolean` flags

```java
boolean increasing = true, decreasing = true;
```

A `boolean` is either `true` or `false`. Flags like these record facts discovered during a scan.

### 3.2 Logical operators

| Operator | Meaning | Example |
|---|---|---|
| `!a` | NOT | `!true` → false |
| `a && b` | AND, short-circuit (skips b if a is false) | |
| `a \|\| b` | OR, short-circuit (skips b if a is true) | |

`!increasing && !decreasing` is "neither is possible". By **De Morgan's law** it equals `!(increasing || decreasing)`.

### 3.3 Two separate `if`s (not `if / else if`)

```java
if (nums[i] > nums[i - 1]) decreasing = false;
if (nums[i] < nums[i - 1]) increasing = false;
```

Here they can never both be true, so `else if` would behave the same and skip one comparison. Independent `if`s are fine when the conditions are exclusive anyway. Just know the difference: with `else if`, the second test only runs when the first one failed.

---

## 4. Approach 1: Two passes

```java
static boolean isMonotonicTwoPass(int[] nums) {
    return isNonDecreasing(nums) || isNonIncreasing(nums);
}
static boolean isNonDecreasing(int[] a) {
    for (int i = 1; i < a.length; i++) if (a[i] < a[i - 1]) return false;
    return true;
}
static boolean isNonIncreasing(int[] a) {
    for (int i = 1; i < a.length; i++) if (a[i] > a[i - 1]) return false;
    return true;
}
```

- Helper methods make the logic very clear.
- `||` short-circuits: if the array is non-decreasing, the second check never runs.
- **O(n) time** (at most 2n comparisons), **O(1) space.**

**Dry run** `[6, 5, 4, 4]`: non-decreasing? 5 < 6 → false. Non-increasing? 5 > 6 no, 4 > 5 no, 4 > 4 no → true → **true**.

---

## 5. Approach 2: One pass with two flags (the file's solution)

### Code ([E19_MonotonicArray.java:33-41](E19_MonotonicArray.java#L33-L41))

```java
static boolean isMonotonic(int[] nums) {
    boolean increasing = true, decreasing = true;
    for (int i = 1; i < nums.length; i++) {
        if (nums[i] > nums[i - 1]) decreasing = false;
        if (nums[i] < nums[i - 1]) increasing = false;
        if (!increasing && !decreasing) return false;    // early exit
    }
    return true;
}
```

### Line by line

| Line | What it does |
|---|---|
| `boolean increasing = true, decreasing = true;` | with no evidence yet, both shapes are possible |
| `for (int i = 1; ...)` | look at every neighbouring pair `(i−1, i)`. Starting at 1 avoids `nums[-1]` |
| `if (nums[i] > nums[i - 1]) decreasing = false;` | a step up → can't be non-increasing |
| `if (nums[i] < nums[i - 1]) increasing = false;` | a step down → can't be non-decreasing |
| `if (!increasing && !decreasing) return false;` | both ruled out → stop early |
| `return true;` | at least one shape survived. (If both had died, we'd have returned already, so returning `true` is the same as `increasing \|\| decreasing`) |

### Dry run 1: `[1, 3, 2]`

| i | pair | step | increasing | decreasing | both false? |
|---|---|---|---|---|---|
| start | | | true | true | |
| 1 | 1 → 3 | up | true | **false** | no |
| 2 | 3 → 2 | down | **false** | false | **yes → return false** |

### Dry run 2: `[1, 2, 2, 3]`

| i | pair | step | increasing | decreasing |
|---|---|---|---|---|
| 1 | 1 → 2 | up | true | false |
| 2 | 2 → 2 | equal | true | false |
| 3 | 2 → 3 | up | true | false |

**Return true** (increasing survived).

### Dry run 3: `[5, 5, 5]`
All steps are equal. Both flags stay `true` → **return true**.

### Dry run 4: `[1]`
The loop never runs → **return true**.

### Complexity
- **Time O(n)** (one pass, with an early exit), **Space O(1).**

---

## 6. (Bonus) Approach 3: Decide the direction from the endpoints

If the array is monotonic, its direction is fixed by `first` versus `last`. Check every step against that direction:

```java
static boolean isMonotonicByEnds(int[] nums) {
    int n = nums.length;
    if (n <= 2) return true;
    boolean up = nums[n - 1] >= nums[0];         // expected direction (equal → flat; any step breaks it)
    for (int i = 1; i < n; i++) {
        if (up && nums[i] < nums[i - 1]) return false;
        if (!up && nums[i] > nums[i - 1]) return false;
    }
    return true;
}
```

Why is this correct? If first == last and anything changes in between, then the array must go both up and down somewhere, so it's not monotonic. With `up = true`, the "down" check catches that. **O(n), O(1).** It's neat, but the two-flag version is easier to explain.

---

## 7. Comparing the approaches

| | Two passes | Two flags | Endpoints |
|---|---|---|---|
| Time | O(n), up to 2 passes | **O(n), 1 pass** | O(n), 1 pass |
| Space | O(1) | O(1) | O(1) |
| Clarity | very clear | clear | needs an argument |

---

## 8. Edge cases and common mistakes

**Edge cases:** one element, two elements, all equal, plateaus in the middle (`[1,2,2,1]` → false), negative numbers.

**Mistakes:**
1. Using strict checks (`>=` to rule out), which treats equal neighbours as breaking monotonicity.
2. Deciding the direction from the **first pair** only: `[1, 1, 2, 0]` starts flat, so the first pair tells you nothing.
3. Returning `increasing && decreasing` instead of `||`.

---

## 9. Interview questions and answers

**Q1. Why start both flags as true?**
> With no evidence, both shapes are possible. Each step can only rule a shape out, never prove one, so we eliminate as we go and see what's left.

**Q2. What's the difference between "non-decreasing" and "increasing"?**
> Non-decreasing allows equal neighbours (`a[i] ≥ a[i−1]`); strictly increasing doesn't (`a[i] > a[i−1]`).

**Q3. Can you stop early?**
> Yes, as soon as both flags are false. In the best case for "false" that's after 2 steps.

### Follow-up questions

**F1. Strictly monotonic.**
> Equal neighbours rule out **both** flags: `if (a[i] >= a[i−1]) decreasing = false; if (a[i] <= a[i−1]) increasing = false;`.

**F2. Can it become monotonic by changing at most one element? (LC 665 Non-decreasing Array, for the non-decreasing direction)**
> Count the "drops" `a[i] < a[i−1]`. More than one → false. For a single drop at i, fix it by lowering `a[i−1]` (if `i < 2 || a[i−2] ≤ a[i]`) or raising `a[i]`; then make sure the rest still holds.

**F3. The longest monotonic subarray.**
> Two running counters (the up-run and the down-run lengths), reset whenever the direction breaks; track the max. O(n).

**F4. Is a mountain (up, then down)?** → [E20 Valid Mountain Array](E20_ValidMountainArray.md)

**F5. A bitonic array: find the peak in O(log n).**
> Binary search: if `a[mid] < a[mid+1]` the peak is to the right, otherwise it's at mid or to the left (LC 852 / 162).

---

## 10. Running the file

```bash
java E19_MonotonicArray.java      # 5/5 tests passed
```

**One-line summary:** *"Assume both directions; a step up rules out 'decreasing', a step down rules out 'increasing'."*
