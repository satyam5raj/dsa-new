# E14 · Single Number

| | |
|---|---|
| **LeetCode** | [#136 Single Number](https://leetcode.com/problems/single-number/) |
| **Difficulty** | Easy |
| **Pattern** | Bit manipulation (XOR cancels pairs) |
| **Code** | [E14_SingleNumber.java](E14_SingleNumber.java) |
| **Run** | `java E14_SingleNumber.java` |

---

## 1. The problem

Every element appears **exactly twice**, except **one** element that appears once. Find it.
**Required:** O(n) time and **O(1) extra space**.

| nums | answer |
|---|---|
| `[2, 2, 1]` | 1 |
| `[4, 1, 2, 1, 2]` | 4 |
| `[1]` | 1 |
| `[-3, 7, 7]` | −3 |
| `[0, 5, 5]` | 0 |

**Constraints:** `1 ≤ n ≤ 3 × 10⁴`, values fit in an `int`.

---

## 2. How to think about it

- **Count map** or **HashSet**: O(n) time, but **O(n) space**, which breaks the requirement.
- **Sort and compare pairs**: O(n log n), which breaks the time requirement.
- We need something that "remembers" all the numbers in **one variable** and makes pairs **disappear**. That's **XOR**:
  - `x ^ x = 0` (a number cancels itself)
  - `x ^ 0 = x` (0 changes nothing)
  - order doesn't matter
- So XOR **everything**: every pair cancels to 0, and the single number is what's left.

---

## 3. Java concepts used in this problem

### 3.1 XOR (`^`) recap and a bit-level picture

XOR compares bits: the result bit is **1 when the bits differ**. (Full explanation in [E10 Missing Number](E10_MissingNumber.md).)

```
4 = 100
1 = 001
2 = 010
```

XOR is like a **light switch per bit**: each number flips the bits where it has a 1. A pair flips the same switches twice, so they end up back where they started. Only the single number's flips remain.

### 3.2 How negative numbers look in binary (two's complement)

Why does XOR also work for `-3`? Java stores `int` values as 32 bits in **two's complement**:

```
 3 = 00000000 00000000 00000000 00000011
-3 = 11111111 11111111 11111111 11111101   (flip all bits of 3, then add 1)
```

XOR works on the raw bits, whatever they "mean". `-3 ^ 7 ^ 7` = `-3 ^ 0` = `-3`. No special case needed.

Useful: `Integer.toBinaryString(x)` prints the bits, so you can check things like this yourself:

```java
System.out.println(Integer.toBinaryString(-3));  // 11111111111111111111111111111101
```

### 3.3 HashSet add/remove toggling (used in the alternative)

`set.add(x)` returns `false` if `x` was already there (E02). We can use that to "toggle" membership: the first time we see x we add it, the second time we remove it.

---

## 4. Approach 1: HashSet toggle (O(n) space)

```java
static int singleNumberSet(int[] nums) {
    Set<Integer> set = new HashSet<>();
    for (int x : nums) {
        if (!set.add(x)) set.remove(x);   // second time: remove it again
    }
    return set.iterator().next();          // exactly one value is left
}
```

| Line | What it does |
|---|---|
| `if (!set.add(x)) set.remove(x);` | first sighting → it gets added; second sighting → `add` fails → remove it |
| `set.iterator().next()` | get the only remaining element (a Set has no `get(0)`) |

**Dry run** `[4, 1, 2, 1, 2]`: {4} → {4,1} → {4,1,2} → {4,2} → {4} → **return 4**.

**O(n) time, O(n) space** (up to n/2 values at once).

---

## 5. Approach 2: Math trick (O(n) space)

`2 × (sum of distinct values) − (sum of all values) = the single number`, because every pair adds its value twice.

```java
static int singleNumberMath(int[] nums) {
    Set<Integer> distinct = new HashSet<>();
    long all = 0, uniq = 0;
    for (int x : nums) {
        all += x;
        if (distinct.add(x)) uniq += x;
    }
    return (int) (2 * uniq - all);
}
```

**Dry run** `[4,1,2,1,2]`: distinct sum = 4+1+2 = 7, all = 10 → 2 × 7 − 10 = **4** ✓
Still O(n) space (the set). `long` avoids overflow.

---

## 6. Approach 3: XOR everything (optimal)

### Code ([E14_SingleNumber.java:38-42](E14_SingleNumber.java#L38-L42))

```java
static int singleNumber(int[] nums) {
    int result = 0;
    for (int x : nums) result ^= x;
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int result = 0;` | 0 is the identity for XOR (`0 ^ x = x`), just like 0 for addition |
| `for (int x : nums) result ^= x;` | fold every number into result |
| `return result;` | all pairs cancelled; the single number remains |

### Dry run 1: `[4, 1, 2, 1, 2]`

| x | result before (bin) | x (bin) | result after (bin) | decimal |
|---|---|---|---|---|
| 4 | 000 | 100 | 100 | 4 |
| 1 | 100 | 001 | 101 | 5 |
| 2 | 101 | 010 | 111 | 7 |
| 1 | 111 | 001 | 110 | 6 |
| 2 | 110 | 010 | 100 | **4** ✓ |

The middle values (5, 7, 6) look random, but that's fine. Rearranged, the whole thing is `4 ^ (1^1) ^ (2^2) = 4 ^ 0 ^ 0 = 4`.

### Dry run 2: `[2, 2, 1]`
`0^2 = 2` → `2^2 = 0` → `0^1 = 1` → **return 1** ✓

### Dry run 3: `[0, 5, 5]`
`0^0 = 0` → `0^5 = 5` → `5^5 = 0` → **return 0** ✓. The single number really is 0.

### Complexity
- **Time O(n)**, **Space O(1)**. This meets the requirement.

---

## 7. Comparing the approaches

| | HashSet toggle | Math (2×distinct − all) | XOR |
|---|---|---|---|
| Time | O(n) | O(n) | **O(n)** |
| Space | O(n) | O(n) | **O(1)** |
| Meets the requirement | no | no | **yes** |

---

## 8. Edge cases and common mistakes

**Edge cases:** a single element, the single number is 0, negative numbers, the single number first or last.

**Mistakes:**
1. Starting `result` at `nums[0]` and then XORing from index 0 again, which cancels the first element.
2. Assuming XOR fails on negatives (it doesn't).
3. Using XOR when the "others" appear **three** times. XOR then doesn't cancel them (`x^x^x = x`). See F1.

---

## 9. Interview questions and answers

**Q1. Why does XOR solve this?**
> XOR is commutative and associative, `x ^ x = 0` and `x ^ 0 = x`. Reordered, the total XOR groups each pair together; each pair is 0, so only the unpaired number remains.

**Q2. Is O(1) space possible without bit tricks?**
> Not within O(n) time with a simple method. Sorting in place would be O(1) space but O(n log n) time. XOR is the intended solution.

**Q3. What other problems use the XOR-cancel idea?**
> Missing Number (XOR indices with values), Find the Difference (one extra character in a string), finding the two unique numbers (LC 260), and checking a duplicate where n+1 numbers come from 1..n.

### Follow-up questions

**F1. Single Number II: every other element appears **three** times (LC 137).**
> Count each bit position mod 3: for bits b = 0..31, `count = Σ ((x >> b) & 1)`; if `count % 3 == 1`, set bit b in the answer. O(32n), O(1). An O(n) version uses two bitmasks, `ones` and `twos`: `ones = (ones ^ x) & ~twos; twos = (twos ^ x) & ~ones;`, and the answer is `ones`.

**F2. Single Number III: **two** elements appear once, the rest twice (LC 260).**
> XOR everything to get `a ^ b` (non-zero, since a ≠ b). Isolate one set bit: `diff = xor & -xor` (the lowest set bit). Split the numbers into two groups by `(x & diff) == 0`; XOR each group to get a and b.

**F3. Every element appears k times except one, which appears once.**
> Bit counting mod k, generalising F1.

**F4. Find the extra letter in t, where t = s shuffled plus one letter (LC 389).** → [02-Strings E10](../../02-Strings/02-Easy/E10_FindTheDifference.md)
> XOR every character of s and t; the extra letter remains.

**F5. The array is sorted: find the single element in O(log n) (LC 540).**
> Binary search on pairs: before the single element, pairs start at even indices; after it, at odd indices. Check whether `nums[mid] == nums[mid ^ 1]` to decide which side to search. `mid ^ 1` is mid's pair partner (even → +1, odd → −1).

---

## 10. Running the file

```bash
java E14_SingleNumber.java      # 5/5 tests passed
```

**One-line summary:** *"XOR everything: pairs cancel to 0, the single number survives."*
