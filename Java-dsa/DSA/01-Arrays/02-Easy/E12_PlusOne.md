# E12 · Plus One

| | |
|---|---|
| **LeetCode** | [#66 Plus One](https://leetcode.com/problems/plus-one/) |
| **Difficulty** | Easy |
| **Pattern** | Simulation (carry from the right, like adding by hand) |
| **Code** | [E12_PlusOne.java](E12_PlusOne.java) |
| **Run** | `java E12_PlusOne.java` |

---

## 1. The problem

A non-negative integer is stored as an array of digits, **most significant digit first** (no leading zeros except for the number 0 itself). Add **one** and return the resulting digits.

| digits | number | +1 | answer |
|---|---|---|---|
| `[1, 2, 3]` | 123 | 124 | `[1, 2, 4]` |
| `[4, 3, 2, 9]` | 4329 | 4330 | `[4, 3, 3, 0]` |
| `[9, 9]` | 99 | 100 | `[1, 0, 0]` (the array grows!) |
| `[0]` | 0 | 1 | `[1]` |
| `[1, 9, 9]` | 199 | 200 | `[2, 0, 0]` |

**Constraints:** `1 ≤ digits.length ≤ 100`, each digit is 0–9.

---

## 2. How to think about it

- **Tempting:** convert to a number, add 1, convert back. That **fails**: 100 digits is far beyond `long` (max about 9.2 × 10¹⁸, which is 19 digits). You'd need `BigInteger`.
- **Do it like on paper:** start at the **last** digit.
  - If the digit is **< 9**: add 1 and you're **done**. No carry, so nothing further left changes.
  - If the digit is **9**: it becomes **0** and a carry of 1 moves one place left.
  - If the carry runs off the **front**, every digit was 9 (99…9). The answer is `1` followed by n zeros: a **new, longer array**.

Key observation: the carry only continues while the digits are 9. So the moment you see a non-9, you can return.

---

## 3. Java concepts used in this problem

### 3.1 Incrementing an array element: `digits[i]++`

`digits[i]++` adds 1 to the value stored at index i (the same as `digits[i] = digits[i] + 1`). The `++` applies to the **element**, not to the index.

### 3.2 Early `return` inside a loop

```java
if (digits[i] < 9) {
    digits[i]++;
    return digits;    // leaves the method right away; the rest of the loop never runs
}
```

This makes the common case very fast. For a random number, the last digit is a 9 only 10% of the time, so most calls finish after **one** step.

### 3.3 Default values of a new array

```java
int[] result = new int[digits.length + 1];   // [0, 0, 0, ..., 0]
result[0] = 1;                               // [1, 0, 0, ..., 0]
```

New `int` arrays are already zero-filled, so we only set the first digit.

### 3.4 `long` limits and `BigInteger`

| type | max value | digits |
|---|---|---|
| `int` | 2,147,483,647 | 10 |
| `long` | 9,223,372,036,854,775,807 | 19 |
| `java.math.BigInteger` | unlimited (limited only by memory) | any |

```java
import java.math.BigInteger;
BigInteger big = new BigInteger("99999999999999999999999");
big = big.add(BigInteger.ONE);       // immutable: add() returns a NEW BigInteger
```

`BigInteger` is how Java handles arbitrarily large numbers. Its operations are methods (`add`, `multiply`, `mod`...), because Java has no operator overloading.

---

## 4. Approach 1: Using BigInteger (works, but misses the point)

```java
static int[] plusOneBig(int[] digits) {
    StringBuilder sb = new StringBuilder();
    for (int d : digits) sb.append(d);                         // "4329"
    String s = new java.math.BigInteger(sb.toString())
                   .add(java.math.BigInteger.ONE).toString();  // "4330"
    int[] res = new int[s.length()];
    for (int i = 0; i < s.length(); i++) res[i] = s.charAt(i) - '0';
    return res;
}
```

- `StringBuilder` builds the digit string efficiently (see the StringBuilder notes from the Basics).
- `s.charAt(i) - '0'` turns the character `'7'` into the number 7. Characters are numbers underneath (`'0'` is 48, `'7'` is 55), so subtracting `'0'` gives the digit's value. You'll use this trick constantly in string problems.
- **O(n)** time and space, but the interviewer wants the digit logic.

---

## 5. Approach 2: Carry simulation (optimal)

### Code ([E12_PlusOne.java:35-46](E12_PlusOne.java#L35-L46))

```java
static int[] plusOne(int[] digits) {
    for (int i = digits.length - 1; i >= 0; i--) {
        if (digits[i] < 9) {
            digits[i]++;
            return digits;                  // no carry left -> done early
        }
        digits[i] = 0;                      // 9 + 1 = 10 -> write 0, carry 1 to the left
    }
    int[] result = new int[digits.length + 1];  // all digits were 9
    result[0] = 1;                               // Java arrays start as zeros: [1,0,0,...]
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| `for (int i = digits.length - 1; i >= 0; i--)` | go from the **last** digit (the ones place) to the first |
| `if (digits[i] < 9)` | this digit can absorb the +1 without a carry |
| `digits[i]++; return digits;` | add the 1 (from the original +1, or from a carry) and stop |
| `digits[i] = 0;` | the digit was 9: 9 + 1 = 10, so write 0 and let the loop continue left (the "carry") |
| *(loop ends without returning)* | every digit was 9 and is now 0 |
| `int[] result = new int[digits.length + 1]; result[0] = 1;` | 999 + 1 = 1000: one more digit, a 1 followed by zeros |

**Notice:** there's no `carry` variable. Reaching the next loop iteration *means* there's a carry, because we only continue after writing a 0.

### Dry run 1: `[1, 2, 3]`

| i | digits[i] | < 9? | action | digits |
|---|---|---|---|---|
| 2 | 3 | yes | 3 → 4, **return** | `[1, 2, 4]` ✓ |

### Dry run 2: `[4, 3, 2, 9]`

| i | digits[i] | < 9? | action | digits |
|---|---|---|---|---|
| 3 | 9 | no | → 0, carry left | `[4, 3, 2, 0]` |
| 2 | 2 | yes | 2 → 3, **return** | `[4, 3, 3, 0]` ✓ |

### Dry run 3: `[9, 9]` (the array grows)

| i | digits[i] | < 9? | action | digits |
|---|---|---|---|---|
| 1 | 9 | no | → 0 | `[9, 0]` |
| 0 | 9 | no | → 0 | `[0, 0]` |
| loop ends | | | new array of length 3, result[0] = 1 | `[1, 0, 0]` ✓ |

### Dry run 4: `[1, 9, 9]`

| i | digits[i] | action | digits |
|---|---|---|---|
| 2 | 9 | → 0 | `[1, 9, 0]` |
| 1 | 9 | → 0 | `[1, 0, 0]` |
| 0 | 1 | → 2, **return** | `[2, 0, 0]` ✓ |

### Complexity
- **Time:** O(n) in the worst case (all 9s), O(1) in the typical case.
- **Space:** O(1) extra, except O(n) for the new array in the all-9s case.

---

## 6. (Bonus) A general version with an explicit carry

Useful when adding an arbitrary number `k` (LC 989, Add to Array-Form of Integer) or two digit arrays:

```java
static int[] addK(int[] digits, int k) {
    LinkedList<Integer> out = new LinkedList<>();
    int i = digits.length - 1, carry = k;
    while (i >= 0 || carry > 0) {
        int sum = carry + (i >= 0 ? digits[i--] : 0);
        out.addFirst(sum % 10);     // last digit of the sum
        carry = sum / 10;           // everything else carries over
    }
    return out.stream().mapToInt(Integer::intValue).toArray();
}
```

- `sum % 10` → the remainder, i.e. the last digit (`13 % 10 = 3`).
- `sum / 10` → **integer division** drops the decimal part (`13 / 10 = 1`).
- `addFirst` on a `LinkedList` is O(1) (on an ArrayList it would be O(n)).
- With `k = 1`, this is Plus One.

---

## 7. Comparing the approaches

| | BigInteger | Carry simulation |
|---|---|---|
| Time | O(n) | **O(n) worst, O(1) typical** |
| Extra space | O(n) | **O(1)** (O(n) only for all 9s) |
| Interview-friendly | no | **yes** |

---

## 8. Edge cases and common mistakes

**Edge cases:** all 9s (`[9]`, `[9,9,9]`), a single digit `[0]`, a 9 only at the end, a long array (100 digits).

**Mistakes:**
1. Converting to `int`/`long`, which overflows for long inputs.
2. Forgetting the all-9s case and returning `[0, 0]` for `[9, 9]`.
3. Looping from the front (index 0). Addition starts at the **least** significant digit, which is at the end.
4. Using a carry variable but not stopping when it becomes 0, which does needless work (not wrong, just slower).

---

## 9. Interview questions and answers

**Q1. Why not convert to a number?**
> Up to 100 digits doesn't fit in `long` (19 digits). Working digit by digit has no size limit.

**Q2. Why can you return as soon as a digit is less than 9?**
> Adding 1 to a digit below 9 creates no carry, so every digit to its left stays the same.

**Q3. In the all-9s case, why can you just build `[1, 0, ..., 0]`?**
> The only number made of all 9s plus one is a power of ten: 99…9 + 1 = 10ⁿ. Its digits are a 1 followed by n zeros, and Java's new arrays are already zero-filled.

### Follow-up questions

**F1. Add two numbers given as digit arrays (or strings) (LC 415 Add Strings).**
> Two pointers from the right, `sum = a[i] + b[j] + carry`, write `sum % 10`, `carry = sum / 10`, and continue while either pointer or the carry remains. Reverse the output at the end, or fill from the back.

**F2. Digits stored least significant first, in a linked list (LC 2 Add Two Numbers).** → [03-Linked-Lists M03](../../03-Linked-Lists/03-Medium/M03_AddTwoNumbers.md)
> Same carry loop, walking both lists forward.

**F3. Plus one on a linked list, most significant digit first (LC 369).**
> Find the **last non-9** node, increment it, and set everything after it to 0. If there's no non-9 node, add a new head with 1. Or recurse to the end and pass the carry back up.

**F4. Multiply two big numbers as strings (LC 43).**
> Grade-school multiplication: `res[i+j+1] += a[i] × b[j]`, then propagate carries. O(n × m).

**F5. Subtract one instead.**
> Mirror it: from the right, a digit > 0 is decremented and you return; a 0 becomes 9 and the borrow continues. Then remove a leading zero if one appears (e.g. 100 − 1 = 099 → 99).

---

## 10. Running the file

```bash
java E12_PlusOne.java      # 5/5 tests passed
```

**One-line summary:** *"From the right: a non-9 increments and stops; a 9 becomes 0 and carries; all 9s → [1, 0…0]."*
