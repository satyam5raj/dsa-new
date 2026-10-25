# E10 · Missing Number

| | |
|---|---|
| **LeetCode** | [#268 Missing Number](https://leetcode.com/problems/missing-number/) |
| **Difficulty** | Easy |
| **Pattern** | Math (sum formula) or bit manipulation (XOR) |
| **Code** | [E10_MissingNumber.java](E10_MissingNumber.java) |
| **Run** | `java E10_MissingNumber.java` |

---

## 1. The problem

`nums` contains `n` **distinct** numbers taken from the range `[0, n]`. That range has n + 1 numbers, so exactly **one** is missing. Find it.

| nums | n | range | answer |
|---|---|---|---|
| `[3, 0, 1]` | 3 | 0..3 | 2 |
| `[0, 1]` | 2 | 0..2 | 2 (the missing one can be n itself) |
| `[9,6,4,2,3,5,7,0,1]` | 9 | 0..9 | 8 |
| `[1]` | 1 | 0..1 | 0 |

**Constraints:** `1 ≤ n ≤ 10⁴`, all values distinct. LeetCode's follow-up: *O(1) extra space and O(n) time?*

---

## 2. How to think about it

Four ways, from obvious to clever:

1. **HashSet:** put every value in a set, then check 0..n for the first one that isn't there. O(n) time, O(n) space.
2. **Sort:** after sorting, `nums[i]` should equal `i`. The first index where it doesn't is the missing number (or n if all match). O(n log n).
3. **Sum formula:** the sum of 0..n is `n(n+1)/2`. Subtract the actual sum, and what's left is the missing number. O(n), O(1).
4. **XOR:** XOR every index and every value together. Present numbers cancel out; the missing one remains. O(n), O(1), and no overflow risk.

---

## 3. Java concepts used in this problem

### 3.1 `long` and casting `(int)`

```java
long n = nums.length;                 // int → long is automatic (widening, always safe)
long expected = n * (n + 1) / 2;      // computed in 64-bit, so no overflow
return (int) (expected - actual);     // long → int needs an explicit cast (narrowing)
```

- **Widening** (int → long, int → double) happens automatically, because no information is lost.
- **Narrowing** (long → int) needs a cast `(int)`, because high bits **could** be lost. You're promising Java that the value fits. Here the result is at most n, so it does.
- Why `long` here? If n were about 70,000, `n * (n + 1)` would be about 4.9 × 10⁹ > `Integer.MAX_VALUE` (about 2.1 × 10⁹), so it would overflow. With the given constraints (n ≤ 10⁴), `int` would work, but `long` is the safe habit.

> **Trap:** `long x = a * b;` where `a` and `b` are `int` still **overflows**, because the multiplication happens in `int` *before* the result is widened. Make one operand `long` first: `(long) a * b`. That's why the code declares `n` as `long`.

### 3.2 Bitwise XOR: `^`

XOR ("exclusive or") compares two numbers **bit by bit**. A result bit is 1 when the two bits are **different**:

```
  5  =  101
^ 3  =  011
-----------
  6  =  110
```

The three facts that make the trick work:

| Fact | Example | Why |
|---|---|---|
| `a ^ a = 0` | `5 ^ 5 = 0` | every bit equals itself, so all bits are 0 |
| `a ^ 0 = a` | `5 ^ 0 = 5` | XOR with 0 keeps each bit |
| order doesn't matter | `a^b^c = c^a^b` | XOR is commutative and associative |

So in a big XOR of many numbers, **anything that appears twice cancels**, and only the numbers that appear an odd number of times survive.

`xor ^= v` is the compound form, the same as `xor = xor ^ v`.

Other bitwise operators you'll meet: `&` (AND), `|` (OR), `~` (NOT), `<<` (shift left = ×2), `>>` (shift right = ÷2, keeps the sign), `>>>` (unsigned shift right). Note: `^` is **not** "power" in Java. `2 ^ 3` is 1, not 8.

### 3.3 Operator precedence in `xor ^= i ^ nums[i];`

The right side `i ^ nums[i]` is computed first, then XORed into `xor`. It's the same as `xor = xor ^ (i ^ nums[i])`, and because order doesn't matter in XOR, that's also `xor ^ i ^ nums[i]`.

---

## 4. Approach 1: HashSet

```java
static int missingNumberSet(int[] nums) {
    Set<Integer> set = new HashSet<>();
    for (int x : nums) set.add(x);
    for (int v = 0; v <= nums.length; v++) {      // note <= : the range is 0..n inclusive
        if (!set.contains(v)) return v;
    }
    return -1;                                    // unreachable for valid input
}
```

**Dry run** `[3,0,1]`: set = {0,1,3}. Check 0 ✓, 1 ✓, 2 ✗ → **return 2**.
**O(n) time, O(n) space.**

---

## 5. Approach 2: Sort, then find the first mismatch

```java
static int missingNumberSort(int[] nums) {
    int[] a = nums.clone();
    Arrays.sort(a);
    for (int i = 0; i < a.length; i++) {
        if (a[i] != i) return i;     // value i should be at index i
    }
    return a.length;                 // 0..n-1 all present → n is missing
}
```

**Dry run** `[3,0,1]` → sorted `[0,1,3]`: i=0 ✓, i=1 ✓, i=2: a[2]=3 ≠ 2 → **return 2**.
**Dry run** `[0,1]` → all match → **return 2** (= n).
**O(n log n) time.**

---

## 6. Approach 3: Sum formula

### Code ([E10_MissingNumber.java:37-43](E10_MissingNumber.java#L37-L43))

```java
static int missingNumberSum(int[] nums) {
    long n = nums.length;
    long expected = n * (n + 1) / 2;           // long: avoids overflow
    long actual = 0;
    for (int x : nums) actual += x;
    return (int) (expected - actual);
}
```

### Line by line

| Line | What it does |
|---|---|
| `long n = nums.length;` | n numbers are given, so the range is 0..n |
| `long expected = n * (n + 1) / 2;` | Gauss's formula: 0 + 1 + … + n. (Adding 0 changes nothing, so it's the same as 1..n) |
| `long actual = 0; for (...) actual += x;` | the sum of what we actually have |
| `return (int) (expected - actual);` | the difference is exactly the one missing number |

**Why `n(n+1)/2`?** Pair the numbers up: 1 + n, 2 + (n−1), … Each pair sums to n + 1, and there are n/2 pairs.

### Dry run 1: `[3, 0, 1]`
- n = 3, expected = 3 × 4 / 2 = **6** (0+1+2+3)
- actual = 3 + 0 + 1 = **4**
- missing = 6 − 4 = **2** ✓

### Dry run 2: `[9,6,4,2,3,5,7,0,1]`
- n = 9, expected = 9 × 10 / 2 = **45**
- actual = 9+6+4+2+3+5+7+0+1 = **37**
- missing = **8** ✓

### Dry run 3: `[1]`
- n = 1, expected = 1, actual = 1 → missing **0** ✓

**O(n) time, O(1) space.**

---

## 7. Approach 4: XOR (no overflow, ever)

### Code ([E10_MissingNumber.java:45-51](E10_MissingNumber.java#L45-L51))

```java
static int missingNumberXor(int[] nums) {
    int xor = nums.length;                     // start with n (the index that has no array slot)
    for (int i = 0; i < nums.length; i++) {
        xor ^= i ^ nums[i];                    // pairs cancel out
    }
    return xor;
}
```

### Idea
XOR together **all indices 0..n** and **all values**. Every number that is present appears **twice** (once as an index, once as a value) and cancels to 0. The missing number appears only once, as an index, so it survives.

The loop only produces indices 0..n−1. Index **n** has no array slot, so we **start** `xor` at `n` to include it.

### Line by line

| Line | What it does |
|---|---|
| `int xor = nums.length;` | include the index n up front |
| `for (int i = 0; i < nums.length; i++)` | for each position |
| `xor ^= i ^ nums[i];` | add both the index and the value to the running XOR |
| `return xor;` | everything else cancelled; the missing number is left |

### Dry run 1: `[3, 0, 1]` (n = 3)

| step | i | nums[i] | xor before | `i ^ nums[i]` | xor after (decimal / binary) |
|---|---|---|---|---|---|
| start | | | | | 3 / `11` |
| 1 | 0 | 3 | 3 | 0 ^ 3 = 3 | 3 ^ 3 = **0** / `00` |
| 2 | 1 | 0 | 0 | 1 ^ 0 = 1 | 0 ^ 1 = **1** / `01` |
| 3 | 2 | 1 | 1 | 2 ^ 1 = 3 | 1 ^ 3 = **2** / `10` |

**Return 2** ✓

**Why it works, written out:** `3 ^ (0^3) ^ (1^0) ^ (2^1)` = rearranged: `(0^0) ^ (1^1) ^ (3^3) ^ 2` = `0 ^ 0 ^ 0 ^ 2` = **2**.

### Dry run 2: `[0, 1]` (missing n = 2)
`xor = 2` → `2 ^ (0^0) = 2` → `2 ^ (1^1) = 2` → **return 2** ✓

**O(n) time, O(1) space, no overflow** (XOR never produces a carry).

---

## 8. Comparing the approaches

| | HashSet | Sort | Sum | XOR |
|---|---|---|---|---|
| Time | O(n) | O(n log n) | **O(n)** | **O(n)** |
| Extra space | O(n) | O(log n)–O(n) | **O(1)** | **O(1)** |
| Overflow risk | no | no | yes, use `long` | **none** |
| Modifies input | no | yes (unless cloned) | no | no |

---

## 9. Edge cases and common mistakes

**Edge cases:** the missing number is 0 (`[1]`), the missing number is n (`[0,1]`), a single element, an unsorted array.

**Mistakes:**
1. Using `n = nums.length − 1`: the range is 0..**n** where n = length.
2. Forgetting index n in the XOR (starting `xor` at 0).
3. Looping `v < n` instead of `v <= n` in the set approach, which misses the case where n is the answer.
4. Writing `^` intending "power".
5. `int * int` overflow in the sum formula for big n.

---

## 10. Interview questions and answers

**Q1. Which solution would you pick, and why?**
> XOR or the sum: both are O(n) time and O(1) space. XOR has no overflow risk; the sum formula is easier to explain. I'd present the sum first and mention XOR as the overflow-safe version.

**Q2. Explain why XOR finds the missing number.**
> `a ^ a = 0` and `a ^ 0 = a`, and order doesn't matter. XORing all indices 0..n and all values means every present number appears twice and cancels, and the missing number appears once.

**Q3. How do you avoid overflow in the sum approach without `long`?**
> Subtract as you go: `res = n; for i: res += i − nums[i];`. The running value stays small. Or use XOR.

### Follow-up questions

**F1. Two numbers are missing from 1..n.**
> XOR everything (indices 1..n and values) to get `a ^ b`. Take any set bit of it (`diff = x & −x`, the lowest set bit), which tells you a and b differ there. Split all numbers into two groups by that bit and XOR each group separately → a and b. Or use the sum and the sum of squares to get two equations.

**F2. One number is missing and one is duplicated (LC 645, Set Mismatch).**
> Index marking: for each `|x|`, flip `nums[|x|−1]` negative; an already-negative slot means x is the duplicate; the slot that stays positive is the missing one. Or use math with sum and sum of squares.

**F3. Find all missing numbers in 1..n where values can repeat.** → [E16](E16_FindAllNumbersDisappeared.md)
> Index marking: negate `nums[|x|−1]` for every x; the indices that stay positive are the missing values.

**F4. First missing positive (LC 41, Hard): any integers, find the smallest missing positive in O(n) time and O(1) space.**
> **Cyclic sort:** place each value v in 1..n at index v−1 by swapping. Then the first index i with `nums[i] != i+1` gives the answer i+1, or n+1 if every slot matches.

**F5. Find the single number when every other number appears twice.** → [E14 Single Number](E14_SingleNumber.md)
> XOR all of them. It's the same cancellation trick.

**F6. The numbers come as a huge stream.**
> Sum or XOR work as is, in one pass with O(1) memory.

---

## 11. Running the file

```bash
java E10_MissingNumber.java      # 6/6 tests passed
```

**One-line summary:** *"Expected sum minus actual sum, or XOR all indices and values so the pairs cancel."*
