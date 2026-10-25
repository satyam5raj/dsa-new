# E03 · Power of Two ⭐

| | |
|---|---|
| **LeetCode** | [#231 Power of Two](https://leetcode.com/problems/power-of-two/) (also [#326 Power of Three](https://leetcode.com/problems/power-of-three/)) |
| **Difficulty** | Easy |
| **Pattern** | Recursion that **divides** the input (logarithmic depth), plus the O(1) bit trick |
| **Code** | [E03_PowerOfTwo.java](E03_PowerOfTwo.java) |
| **Run** | `java E03_PowerOfTwo.java` |

⭐ = not in `leetcode.md`, added for a key technique. Recursion basics are explained in [E01](E01_FibonacciNumber.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Is n a power of two (n = 2ᵏ for some k ≥ 0)?

| n | 1 | 16 | 3 | 0 | −16 | 2³⁰ |
|---|---|---|---|---|---|---|
| answer | true (2⁰) | true | false | false | false | true |

Power of Three asks the same with base 3: 27 → true, 45 → false.

---

## 2. How to think about it

### The recursive definition
```
n == 1            → true      (2⁰)
n ≤ 0 or n odd    → false
otherwise         → isPowerOfTwo(n / 2)
```

Each call **halves** n, so the depth is O(log n): at most 31 calls for an `int`, with no risk of stack overflow. (Compare with [E02](E02_ReverseString.md), where the input only shrinks by 2 per call, so the depth is O(n).)

The same shape works for any base: Power of Three divides by 3.

### The bit trick
A power of two has exactly **one** bit set: `1000…0`. Subtracting 1 clears that bit and sets every bit below it: `0111…1`. So the two numbers share no bits:

```
n > 0 && (n & (n − 1)) == 0
```

```
16 = 10000            12 = 1100
15 = 01111            11 = 1011
&  = 00000 → true     &  = 1000 → false
```

### Dry runs
```
16 → 8 → 4 → 2 → 1                → true
12 → 6 → 3 (odd)                  → false
0                                 → false (n ≤ 0)
```

---

## 3. Java concepts used in this problem

### 3.1 Integer division and `%`

`n / 2` on `int`s **truncates** (7 / 2 = 3). That's why the odd check `n % 2 != 0` comes first: only even numbers are halved, so nothing is lost.

### 3.2 Bitwise AND `&`

`a & b` keeps the bits that are 1 in **both** numbers. It works on the binary representation, unlike `&&`, which works on booleans.

### 3.3 Why the `n > 0` check matters for the bit trick

`Integer.MIN_VALUE` (−2³¹) is `1000…0` in two's complement: a single bit set. `MIN_VALUE − 1` overflows to `MAX_VALUE` (`0111…1`), so `n & (n − 1)` is 0. Without `n > 0`, the trick would wrongly say −2³¹ is a power of two. For 0, `0 & −1 = 0`, which would also be wrong.

### 3.4 `1 << k`

The left shift `1 << k` is 2ᵏ. The test uses `1 << rnd.nextInt(31)` to generate random powers of two, and `1 << 30` for the largest one that's a positive `int`.

---

## 4. Approach 1: Recursion (the file's main solution)

### Code ([E03_PowerOfTwo.java:33-43](E03_PowerOfTwo.java#L33-L43))

```java
static boolean isPowerOfTwo(int n) {
    if (n == 1) return true;
    if (n <= 0 || n % 2 != 0) return false;
    return isPowerOfTwo(n / 2);
}

static boolean isPowerOfThree(int n) {
    if (n == 1) return true;
    if (n <= 0 || n % 3 != 0) return false;
    return isPowerOfThree(n / 3);
}
```

### Line by line

| Line | What it does |
|---|---|
| `n == 1` | the success base case: 2⁰ |
| `n <= 0` | zero and negatives are never powers of two |
| `n % 2 != 0` | an odd number other than 1 can't be a power of two |
| `isPowerOfTwo(n / 2)` | n = 2ᵏ exactly when n / 2 = 2ᵏ⁻¹ |

The order matters: `n == 1` must be checked before the odd test, since 1 is odd.

- **Time O(log n), space O(log n)** for the stack.

---

## 5. Approach 2: The bit trick

### Code ([E03_PowerOfTwo.java:45](E03_PowerOfTwo.java#L45))

```java
static boolean bitTrick(int n) { return n > 0 && (n & (n - 1)) == 0; }
```

| Part | What it does |
|---|---|
| `n > 0` | rules out 0 and every negative number, including `MIN_VALUE` |
| `n & (n − 1)` | clears the lowest set bit |
| `== 0` | nothing was left, so there was exactly one set bit |

- **Time O(1), space O(1).**

---

## 6. Comparing the approaches

| | Recursion | Bit trick |
|---|---|---|
| Time | O(log n) | **O(1)** |
| Works for any base? | yes (divide by 3, 5, …) | only base 2 |

The file checks that the two agree on 100,000 random integers (a third of them actual powers of two).

For Power of Three there's also an O(1) trick: the largest power of 3 that fits in an `int` is 3¹⁹ = 1,162,261,467, and since 3 is prime, n > 0 is a power of 3 exactly when `1162261467 % n == 0`.

---

## 7. Edge cases and common mistakes

**Edge cases:** 0, 1, negative numbers, `Integer.MIN_VALUE`, 2³⁰.

**Mistakes:**
1. Forgetting `n > 0` in the bit trick (0 and `MIN_VALUE` slip through).
2. Checking odd before `n == 1`.
3. Using `Math.log(n) / Math.log(2)` and checking for an integer (floating-point error gives wrong answers).
4. Writing `n & n - 1 == 0` without parentheses: `==` binds tighter than `&`, so it means `n & ((n − 1) == 0)` and doesn't compile.

---

## 8. Interview questions and answers

**Q1. Why is the recursion depth only O(log n)?**
> Each call halves n, so after k calls n has shrunk by a factor of 2ᵏ. An `int` can only be halved about 31 times.

**Q2. Explain `n & (n − 1)`.**
> Subtracting 1 turns the lowest set bit into 0 and every bit below it into 1. ANDing with the original clears exactly that lowest set bit. If the result is 0, there was only one set bit.

### Follow-up questions

**F1. Count the set bits (LC 191).**
> Repeat `n &= n − 1` and count the iterations (Kernighan's trick): one iteration per set bit.

**F2. Power of Four (LC 342).**
> A power of two whose single bit is in an even position: `n > 0 && (n & (n − 1)) == 0 && (n & 0x55555555) != 0`.

**F3. Pow(x, n).** → [M01](../03-Medium/M01_PowXN.md)
> The same halving recursion, used to compute powers fast.

---

## 9. Running the file

```bash
java E03_PowerOfTwo.java      # 9/9 tests passed (including 100,000 random ints: recursion == bit trick)
```

**One-line summary:** *"Recursively halve n while it's even until it reaches 1 (O(log n)), or use the O(1) check n > 0 && (n & (n − 1)) == 0."*
