# E16 · Find All Numbers Disappeared in an Array

| | |
|---|---|
| **LeetCode** | [#448 Find All Numbers Disappeared in an Array](https://leetcode.com/problems/find-all-numbers-disappeared-in-an-array/) |
| **Difficulty** | Easy |
| **Pattern** | Index marking (use the array itself as a hash set) |
| **Code** | [E16_FindAllNumbersDisappeared.java](E16_FindAllNumbersDisappeared.java) |
| **Run** | `java E16_FindAllNumbersDisappeared.java` |

---

## 1. The problem

`nums` has `n` integers, each in the range `[1, n]`. Some appear twice, others not at all. Return **all numbers in `[1, n]` that do not appear**.

**Follow-up:** do it without extra space (the returned list doesn't count) in O(n) time.

| nums | n | answer |
|---|---|---|
| `[4, 3, 2, 7, 8, 2, 3, 1]` | 8 | `[5, 6]` |
| `[1, 1]` | 2 | `[2]` |
| `[1, 2, 3]` | 3 | `[]` |
| `[2, 2, 2]` | 3 | `[1, 3]` |

**Constraints:** `1 ≤ n ≤ 10⁵`, `1 ≤ nums[i] ≤ n`.

---

## 2. How to think about it

1. **Easy:** a `boolean[] seen` of size n+1 → O(n) extra space.
2. **Notice the special structure:** the values are `1..n`, and the array has exactly `n` slots (indices `0..n−1`). So **each value v has its own "home" slot: index v−1**.
3. **Insight:** we can store one bit of information ("v was seen") in the array itself, without losing the original numbers. We use the **sign**: make `nums[v−1]` **negative** when v is present. Every number is positive (≥ 1) to begin with, so the sign is free to use.
   - The original value is still there: `Math.abs(nums[i])`.
4. After marking, any index `i` that is still **positive** means value `i + 1` never appeared.

This **index marking** trick (also called "the array as its own hash map" or "sign flipping") works whenever the values are in the range `1..n` (or `0..n−1`). It appears in Find All Duplicates (M16), Set Mismatch, First Missing Positive and more.

---

## 3. Java concepts used in this problem

### 3.1 `Math.abs(x)`: absolute value

`Math.abs(-7)` is 7, `Math.abs(7)` is 7. We need it because by the time we read `x`, its slot may already have been flipped negative by an **earlier** element. The original value is still `|x|`.

### 3.2 Negating: `nums[idx] = -nums[idx]`

Unary minus flips the sign. We check `if (nums[idx] > 0)` first so we never flip a slot **twice**. A value appearing twice would otherwise flip it back to positive, wrongly meaning "not seen".

### 3.3 Returning a `List<Integer>`

The number of missing values isn't known in advance, so we collect them in an `ArrayList` (it grows automatically) and return the list. LeetCode's method signature asks for `List<Integer>`.

### 3.4 Modifying the input: side effects

This solution **changes** `nums` (it leaves negative signs). That's called a **side effect**. In interviews, say so, and offer to restore the array afterwards: `for (int i = 0; i < n; i++) nums[i] = Math.abs(nums[i]);`.

---

## 4. Approach 1: boolean array (O(n) extra space)

```java
static List<Integer> findDisappearedSeen(int[] nums) {
    int n = nums.length;
    boolean[] seen = new boolean[n + 1];         // index 0 unused; all false at first
    for (int x : nums) seen[x] = true;
    List<Integer> missing = new ArrayList<>();
    for (int v = 1; v <= n; v++) {
        if (!seen[v]) missing.add(v);
    }
    return missing;
}
```

- `new boolean[n + 1]` starts with all values `false`. We use indices 1..n directly, so index 0 is wasted. That's simpler than writing `x − 1` everywhere.
- A `boolean[]` is much lighter than a `HashSet<Integer>`: about 1 byte per entry versus about 40+ bytes per boxed entry.

**Dry run** `[4,3,2,7,8,2,3,1]`: seen = {1,2,3,4,7,8} → missing 5, 6 → **`[5, 6]`**.
**O(n) time, O(n) space.**

---

## 5. Approach 2: Index marking with negative signs (optimal)

### Code ([E16_FindAllNumbersDisappeared.java:43-53](E16_FindAllNumbersDisappeared.java#L43-L53))

```java
static List<Integer> findDisappearedNumbers(int[] nums) {
    for (int x : nums) {
        int idx = Math.abs(x) - 1;
        if (nums[idx] > 0) nums[idx] = -nums[idx];     // mark value idx+1 as seen
    }
    List<Integer> missing = new ArrayList<>();
    for (int i = 0; i < nums.length; i++) {
        if (nums[i] > 0) missing.add(i + 1);
    }
    return missing;
}
```

### Line by line

| Line | What it does |
|---|---|
| `for (int x : nums)` | visit every value. Note: `x` is read **when we reach it**, so it may already be negative (marked by an earlier element) |
| `int idx = Math.abs(x) - 1;` | the home slot of the value \|x\| |
| `if (nums[idx] > 0) nums[idx] = -nums[idx];` | mark it "seen" by making it negative, only once |
| `for (int i = 0; ...) if (nums[i] > 0) missing.add(i + 1);` | a slot that is still positive was never marked → value `i+1` is missing |
| `return missing;` | |

**Important:** we flip the sign of the **slot `idx`**, not of `x` itself. `nums[idx]` still holds some other value in its magnitude, which we'll read correctly later with `Math.abs`.

### Dry run 1: `[4, 3, 2, 7, 8, 2, 3, 1]` (n = 8)

| step | x (as read) | \|x\| | idx | nums[idx] > 0? | array after |
|---|---|---|---|---|---|
| 1 | 4 | 4 | 3 | yes → flip | `[4, 3, 2, -7, 8, 2, 3, 1]` |
| 2 | 3 | 3 | 2 | yes → flip | `[4, 3, -2, -7, 8, 2, 3, 1]` |
| 3 | −2 | 2 | 1 | yes → flip | `[4, -3, -2, -7, 8, 2, 3, 1]` |
| 4 | −7 | 7 | 6 | yes → flip | `[4, -3, -2, -7, 8, 2, -3, 1]` |
| 5 | 8 | 8 | 7 | yes → flip | `[4, -3, -2, -7, 8, 2, -3, -1]` |
| 6 | 2 | 2 | 1 | no (−3, already marked) | unchanged |
| 7 | −3 | 3 | 2 | no (−2) | unchanged |
| 8 | −1 | 1 | 0 | yes → flip | `[-4, -3, -2, -7, 8, 2, -3, -1]` |

Notice steps 3, 4, 7 and 8: we read a **negative** x (flipped earlier), and `Math.abs` recovers the real value.

**Scan:** positive at index 4 (8) and index 5 (2) → missing values **5 and 6** ✓

### Dry run 2: `[2, 2, 2]` (n = 3)

| x | idx | action | array |
|---|---|---|---|
| 2 | 1 | flip | `[2, -2, 2]` |
| −2 | 1 | already negative | `[2, -2, 2]` |
| 2 | 1 | already negative | `[2, -2, 2]` |

Positive at indices 0 and 2 → missing **`[1, 3]`** ✓

### Dry run 3: `[1, 1]`
x=1 → idx 0 → `[-1, 1]`; x=−1 → idx 0 already negative. Positive at index 1 → **`[2]`** ✓

### Complexity
- **Time O(n):** two passes.
- **Space O(1) extra** (the output list doesn't count, per the problem).

---

## 6. (Bonus) Approach 3: Cyclic sort (put every value in its home)

Swap each value into its home slot `v−1` until every slot holds either its own value or a duplicate. Then the slots where `nums[i] != i + 1` are missing `i + 1`.

```java
static List<Integer> findDisappearedCyclic(int[] nums) {
    int i = 0;
    while (i < nums.length) {
        int home = nums[i] - 1;
        if (nums[i] != nums[home]) {           // its home doesn't already hold this value
            int t = nums[i]; nums[i] = nums[home]; nums[home] = t;
        } else {
            i++;                               // in place, or a duplicate: move on
        }
    }
    List<Integer> missing = new ArrayList<>();
    for (int k = 0; k < nums.length; k++) if (nums[k] != k + 1) missing.add(k + 1);
    return missing;
}
```

- Each swap places one value permanently at home, so there are at most n swaps in total. **O(n) time, O(1) space.**
- The condition `nums[i] != nums[home]` (instead of `home != i`) prevents an endless loop when a duplicate is already at home.

**Dry run** `[4,3,2,7,8,2,3,1]` ends as `[1,2,3,4,3,2,7,8]`. Index 4 holds 3 (≠ 5) and index 5 holds 2 (≠ 6) → `[5, 6]` ✓

---

## 7. Comparing the approaches

| | boolean[] | Sign marking | Cyclic sort |
|---|---|---|---|
| Time | O(n) | O(n) | O(n) |
| Extra space | O(n) | **O(1)** | **O(1)** |
| Input after the call | unchanged | negated (restorable) | reordered |
| Reusable trick for | anything | values 1..n, need a "seen" flag | values 1..n, need positions |

---

## 8. Edge cases and common mistakes

**Edge cases:** nothing missing, all elements the same, n = 1, the missing values at the start or end.

**Mistakes:**
1. Forgetting `Math.abs(x)`, which makes the index negative → `ArrayIndexOutOfBoundsException`.
2. Flipping without the `> 0` check, so duplicates flip the slot back.
3. Writing `missing.add(i)` instead of `i + 1` (an off-by-one mix-up between indices and values).
4. Using this trick when values could be 0 or negative (the sign is no longer free to use), or out of the 1..n range.

---

## 9. Interview questions and answers

**Q1. Why does the sign trick work here and not in general?**
> Two properties: every value maps to a valid index (1..n → 0..n−1), and every value is positive, so the sign bit is unused and can store a "seen" flag without losing the original number.

**Q2. Is this really O(1) space? You modify the input.**
> Yes, extra space is O(1). But it has a side effect on the input; I can restore it with a final pass of `Math.abs`. If modifying the input isn't allowed, I'd need the O(n) boolean array.

**Q3. What other "marks" could you use instead of the sign?**
> Add n to the slot (`nums[idx] += n`) and later read the original as `nums[i] % n` (careful with the value n). The seen check becomes `nums[i] > n`. It's useful when the sign is already taken.

### Follow-up questions

**F1. Find all duplicates instead (LC 442).** → [M16 Find All Duplicates](../03-Medium/M16_FindAllDuplicates.md)
> Same marking: when you're about to flip a slot that is **already negative**, `|x|` is a duplicate.

**F2. Set Mismatch (LC 645): exactly one duplicate and one missing value.**
> Marking: the already-negative slot gives the duplicate; the positive slot after the pass gives the missing value.

**F3. First Missing Positive (LC 41): any integers (negative, 0, big); find the smallest missing positive.**
> Cyclic sort, ignoring values outside 1..n. The first index with `nums[i] != i + 1` gives the answer `i + 1`; if there's none, the answer is `n + 1`. O(n), O(1).

**F4. Find the duplicate without modifying the array and with O(1) space (LC 287).**
> Floyd's cycle detection on `i → nums[i]`.

**F5. Values are 0..n−1 instead of 1..n.**
> The home index is the value itself. But 0 has no sign, so add n instead of negating.

---

## 10. Running the file

```bash
java E16_FindAllNumbersDisappeared.java      # 4/4 tests passed
```

**One-line summary:** *"Value v lives at index v−1; flip that slot negative to mark v seen; positive slots are the missing values."*
