# E17 · Third Maximum Number

| | |
|---|---|
| **LeetCode** | [#414 Third Maximum Number](https://leetcode.com/problems/third-maximum-number/) |
| **Difficulty** | Easy |
| **Pattern** | Track the top-k in variables (one pass) |
| **Code** | [E17_ThirdMaximumNumber.java](E17_ThirdMaximumNumber.java) |
| **Run** | `java E17_ThirdMaximumNumber.java` |

---

## 1. The problem

Return the **third distinct maximum** number in the array. If it doesn't exist, return the **maximum**.

| nums | distinct values (descending) | answer |
|---|---|---|
| `[3, 2, 1]` | 3, 2, 1 | 1 |
| `[1, 2]` | 2, 1 | 2 (no third → return the max) |
| `[2, 2, 3, 1]` | 3, 2, 1 | 1 (the two 2s count once) |
| `[1, 2, MIN_VALUE]` | 2, 1, −2147483648 | −2147483648 |
| `[5, 5, 5]` | 5 | 5 |

**Constraints:** `1 ≤ n ≤ 10⁴`, values are any `int`, **including `Integer.MIN_VALUE`**. Follow-up: *O(n)?*

---

## 2. How to think about it

1. **Sort**, then walk from the largest down, counting distinct values. O(n log n).
2. **TreeSet capped at 3:** keep only the 3 largest distinct values. O(n log 3) = O(n).
3. **Three variables** `first > second > third`, updated as each value arrives, like a tiny leaderboard:
   - **skip** x if it equals one of them (distinct values only)
   - x beats `first` → everyone shifts down one place
   - x beats only `second` → `second` drops to third, x takes second
   - x beats only `third` → x replaces third

**The trap:** the natural "empty" marker would be `Integer.MIN_VALUE`, but that's a **legal input value**. If the array contains MIN_VALUE, you couldn't tell "third = MIN_VALUE" apart from "third not set". The fix is to use `null` for "not set" (a boxed `Long`/`Integer`), or a `long` sentinel like `Long.MIN_VALUE` that can never be an `int` input.

---

## 3. Java concepts used in this problem

### 3.1 Wrapper types can be `null`: `Long first = null`

Primitives (`int`, `long`) always hold a number. **Wrapper objects** (`Integer`, `Long`) can also be `null`, meaning "no value". Here `null` cleanly means "this rank isn't filled yet".

```java
Long first = null;           // nothing yet
first = 5L;                  // autoboxing long → Long
if (first != null && x > first) ...   // unboxing Long → long for the comparison
```

> ⚠ Unboxing a `null` throws **NullPointerException**. That's why every comparison checks `first != null` **first**: short-circuit `&&` (see E07) skips the unboxing when it's null.

Why `Long` instead of `Integer`? Either works with null. The code converts each value to `long x` and compares it with `Long`s. A `long` also allows the alternative of a `Long.MIN_VALUE` sentinel.

### 3.2 `continue`

```java
if (duplicate) continue;   // skip the REST of this iteration, go to the next element
```

`break` leaves the loop completely; `continue` only skips to the next iteration.

### 3.3 Shifting values down (order of assignment matters)

```java
third = second;   // 1. move the old second down first
second = first;   // 2. then the old first
first = x;        // 3. finally the new value
```

Doing it top-down (`first = x` first) would lose the old `first`. Always move the **lowest** rank first.

### 3.4 The final line

```java
return (int) (third != null ? (long) third : (long) first);
```

The ternary picks `third` if it exists, otherwise `first`. The `(long)` casts unbox both branches to the same primitive type, and the `(int)` turns the result back into the return type. A simpler way to write it:

```java
return third != null ? third.intValue() : first.intValue();
```

---

## 4. Approach 1: Sort and count distinct values from the top

```java
static int thirdMaxSort(int[] nums) {
    int[] a = nums.clone();
    Arrays.sort(a);                          // ascending
    int distinct = 1;
    for (int i = a.length - 2; i >= 0; i--) {
        if (a[i] != a[i + 1]) {              // a new distinct value
            distinct++;
            if (distinct == 3) return a[i];
        }
    }
    return a[a.length - 1];                  // fewer than 3 distinct → the max
}
```

**Dry run** `[2, 2, 3, 1]` → sorted `[1, 2, 2, 3]`:

| i | a[i] | a[i+1] | new? | distinct |
|---|---|---|---|---|
| 2 | 2 | 3 | yes | 2 |
| 1 | 2 | 2 | no | 2 |
| 0 | 1 | 2 | yes | **3 → return 1** ✓ |

**O(n log n).**

---

## 5. Approach 2: TreeSet with at most 3 elements

```java
static int thirdMaxTreeSet(int[] nums) {
    TreeSet<Integer> top = new TreeSet<>();
    for (int x : nums) {
        top.add(x);                       // duplicates are ignored automatically
        if (top.size() > 3) top.pollFirst();   // drop the smallest
    }
    return top.size() == 3 ? top.first() : top.last();
}
```

- `pollFirst()` removes and returns the **smallest** element.
- After the loop, the set holds the 3 largest distinct values (or fewer); `first()` is the smallest of them, which is the 3rd max.
- Each operation is O(log 3) = O(1), so this is **O(n)** overall. No sentinel problem, because the set holds real values.

**Dry run** `[3, 2, 1, 5]`: {3} → {2,3} → {1,2,3} → add 5 = {1,2,3,5}, poll 1 → {2,3,5} → 3 elements → `first()` = **2** ✓

---

## 6. Approach 3: Three variables (the file's solution)

### Code ([E17_ThirdMaximumNumber.java:42-61](E17_ThirdMaximumNumber.java#L42-L61))

```java
static int thirdMax(int[] nums) {
    Long first = null, second = null, third = null;
    for (int v : nums) {
        long x = v;
        if ((first != null && x == first) || (second != null && x == second) || (third != null && x == third)) {
            continue;                                  // only DISTINCT values count
        }
        if (first == null || x > first) {
            third = second;
            second = first;
            first = x;
        } else if (second == null || x > second) {
            third = second;
            second = x;
        } else if (third == null || x > third) {
            third = x;
        }
    }
    return (int) (third != null ? (long) third : (long) first);
}
```

### Line by line

| Line | What it does |
|---|---|
| `Long first = null, second = null, third = null;` | the top 3 distinct values; `null` = rank not filled |
| `long x = v;` | widen to `long` so comparisons with the `Long`s unbox cleanly |
| the big `if (... == first) \|\| ...` | x equals one of the current top 3 → it adds no new distinct value → `continue`. Each part checks `!= null` before unboxing |
| `if (first == null \|\| x > first)` | new champion: shift `second → third`, `first → second`, then `first = x` |
| `else if (second == null \|\| x > second)` | between first and second: `second → third`, `second = x` |
| `else if (third == null \|\| x > third)` | between second and third: just replace `third` |
| *(otherwise)* | x is smaller than all of the top 3 → ignore it |
| `return ... third != null ? third : first` | the 3rd max if it exists, otherwise the max |

### Dry run 1: `[2, 2, 3, 1]`

| x | duplicate? | which branch | first | second | third |
|---|---|---|---|---|---|
| 2 | no | first == null | 2 | null | null |
| 2 | **yes (== first)** | continue | 2 | null | null |
| 3 | no | 3 > first | 3 | 2 | null |
| 1 | no | second? 1 > 2 no; third == null → yes | 3 | 2 | 1 |

`third = 1` → **return 1** ✓

### Dry run 2: `[1, 2, Integer.MIN_VALUE]` (why we need null)

| x | branch | first | second | third |
|---|---|---|---|---|
| 1 | first == null | 1 | null | null |
| 2 | 2 > first | 2 | 1 | null |
| MIN | third == null | 2 | 1 | **MIN** |

`third` is not null → **return MIN_VALUE** ✓.
If we had used `Integer.MIN_VALUE` as "empty", `third` would look unset and we'd wrongly return 2.

### Dry run 3: `[5, 5, 5]`
first = 5, then two duplicates skipped. `third == null` → **return first = 5** ✓

### Dry run 4: `[3, 2, 1]` (arriving in descending order)

| x | branch | first | second | third |
|---|---|---|---|---|
| 3 | first null | 3 | | |
| 2 | second null | 3 | 2 | |
| 1 | third null | 3 | 2 | 1 |

**Return 1** ✓

### Complexity
- **Time O(n):** constant work per element.
- **Space O(1).**

---

## 7. Comparing the approaches

| | Sort | TreeSet (≤ 3) | 3 variables |
|---|---|---|---|
| Time | O(n log n) | O(n) | **O(n)** |
| Space | O(n) clone | O(1) | **O(1)** |
| Generalises to k | yes | **yes (a set of size k)** | messy for big k |
| MIN_VALUE trap | none | none | **needs null / long** |

---

## 8. Edge cases and common mistakes

**Edge cases:** fewer than 3 distinct values, duplicates of the top value, `Integer.MIN_VALUE` in the input, all equal, negative numbers only.

**Mistakes:**
1. Using `Integer.MIN_VALUE` as the "empty" marker.
2. Not skipping duplicates, so `[2, 2, 3, 1]` would give third = 2.
3. Shifting in the wrong order and losing a value.
4. Comparing `Long` objects with `==` (`first == second`). That compares references; here we always compare with a primitive `x`, so the values are unboxed.

---

## 9. Interview questions and answers

**Q1. Why not initialise the three variables to `Integer.MIN_VALUE`?**
> MIN_VALUE is a legal input, so "third = MIN_VALUE" would be ambiguous: is it set, or just the default? `null` (or a `long` sentinel outside the int range, like `Long.MIN_VALUE`) removes the ambiguity.

**Q2. How do you generalise to the k-th distinct maximum?**
> Keep a TreeSet (or a min-heap plus a set for uniqueness) of size k: add each value, and evict the smallest when the size exceeds k. O(n log k). The answer is the set's minimum when its size is k.

**Q3. Why is the TreeSet solution still O(n)?**
> The set never holds more than 4 elements, so each `add`/`pollFirst` is O(log 4), which is a constant.

### Follow-up questions

**F1. The k-th largest element, duplicates counted (LC 215).** → [08-Heap M01](../../08-Heap/03-Medium/M01_KthLargestElementInArray.md)
> A min-heap of size k (O(n log k)), or Quickselect (average O(n)).

**F2. The k-th largest in a stream (LC 703).** → [08-Heap E01](../../08-Heap/02-Easy/E01_KthLargestInStream.md)
> A min-heap of size k; its top is the answer after each `add`.

**F3. The second-largest distinct value in one pass.**
> The same idea with two variables, `first` and `second`, plus the duplicate check.

**F4. Return the top 3 values themselves, in order.**
> Return `[first, second, third]`, dropping nulls.

---

## 10. Running the file

```bash
java E17_ThirdMaximumNumber.java      # 5/5 tests passed
```

**One-line summary:** *"Keep first > second > third with null for 'empty', skip duplicates, shift down on each new top value."*
