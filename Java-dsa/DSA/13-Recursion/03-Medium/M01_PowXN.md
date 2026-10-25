# M01 · Pow(x, n) ⭐

| | |
|---|---|
| **LeetCode** | [#50 Pow(x, n)](https://leetcode.com/problems/powx-n/) |
| **Difficulty** | Medium |
| **Pattern** | Divide and conquer: **halve the exponent** (fast exponentiation) |
| **Code** | [M01_PowXN.java](M01_PowXN.java) |
| **Run** | `java M01_PowXN.java` |

⭐ = not in `leetcode.md`, added for a key technique. Recursion basics are explained in [E01](../02-Easy/E01_FibonacciNumber.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Compute xⁿ for a `double` x and an `int` n, which may be negative.

| x | n | xⁿ |
|---|---|---|
| 2.0 | 10 | 1024.0 |
| 2.1 | 3 | 9.261 |
| 2.0 | −2 | 0.25 |
| −2.0 | 3 | −8.0 |
| 1.0 | −2³¹ | 1.0 |

---

## 2. How to think about it

### The naive way
Multiply x by itself n times: O(n). For n = 2³¹ − 1 that's two billion multiplications.

### The key identity
```
xⁿ = (x^(n/2))²        if n is even
xⁿ = (x^(n/2))² · x    if n is odd       (n/2 rounds down)
```

Compute x^(n/2) **once** and square it. The exponent halves with every call, so there are only O(log n) calls.

**The classic mistake:** writing `pow(x, n/2) * pow(x, n/2)` calls the function **twice**. Then T(n) = 2T(n/2) + O(1), which is O(n) again. Store the half in a variable.

### Negative n
x⁻ⁿ = 1 / xⁿ, so flip x to 1/x and make n positive.

**The overflow trap:** for n = `Integer.MIN_VALUE` (−2³¹), `-n` overflows and is **still** −2³¹, because +2³¹ doesn't fit in an `int`. Convert to `long` before negating.

### Dry run: 2¹⁰

| call | n | half | returns |
|---|---|---|---|
| pow(2, 0) | 0 | (base case) | 1 |
| pow(2, 1) | 1, odd | 1 | 1 · 1 · 2 = 2 |
| pow(2, 2) | 2, even | 2 | 2 · 2 = 4 |
| pow(2, 5) | 5, odd | 4 | 4 · 4 · 2 = 32 |
| pow(2, 10) | 10, even | 32 | 32 · 32 = **1024** |

Five calls instead of ten multiplications. For n = 2³¹ it's about 32 calls instead of two billion.

---

## 3. Java concepts used in this problem

### 3.1 Widening `int` to `long`

```java
long N = n;                 // an int converts to long automatically (no cast needed)
if (N < 0) { x = 1 / x; N = -N; }
```

Converting to a **wider** type is always safe and automatic. Negating the `long` −2³¹ gives +2³¹, which fits. The helper takes a `long` exponent for that reason.

### 3.2 Changing a parameter inside a method

`x = 1 / x` reassigns the local copy of `x`. Parameters of primitive types are copies, so this doesn't affect the caller's variable.

### 3.3 `double` arithmetic

- `1 / x` with a `double` x is floating-point division (with two `int`s, `1 / 2` would be 0).
- Results can **underflow** to 0.0 (2⁻²³¹ is far smaller than the smallest `double`) or **overflow** to `Infinity`.
- `1 / 0.0` is `Infinity`, not an exception, so 0⁻¹ returns `Infinity`.
- Comparing `double`s with `==` is risky because of rounding; the test compares 2.1³ as a formatted string, and the random test allows a relative error of 10⁻⁹.

### 3.4 `String.format("%.5f", value)`

It formats a number with 5 digits after the decimal point: `String.format("%.5f", 9.261000000000001)` is `"9.26100"`.

### 3.5 Bit operations for the iterative version

`(N & 1) == 1` tests the lowest bit (is N odd?), and `N >>= 1` shifts right by one (halves N). These come up in section 5.

---

## 4. Approach 1: Recursive fast power (the file's main solution)

### Code ([M01_PowXN.java:36-46](M01_PowXN.java#L36-L46))

```java
static double myPow(double x, int n) {
    long N = n;                                   // long: -Integer.MIN_VALUE fits
    if (N < 0) { x = 1 / x; N = -N; }
    return pow(x, N);
}

static double pow(double x, long n) {
    if (n == 0) return 1.0;
    double half = pow(x, n / 2);                  // computed ONCE
    return n % 2 == 0 ? half * half : half * half * x;
}
```

### Line by line

| Line | What it does |
|---|---|
| `long N = n` | avoid the `MIN_VALUE` negation overflow |
| the negative branch | x⁻ⁿ = (1/x)ⁿ |
| `n == 0` | x⁰ = 1 (including 0⁰ = 1, matching `Math.pow`) |
| `half = pow(x, n / 2)` | the one recursive call |
| the return | square it, and multiply by x once more if n is odd |

### Complexity
- **Time O(log n).**
- **Space O(log n)** for the recursion: at most about 32 frames.

---

## 5. Approach 2: Iterative binary exponentiation

Write n in binary. For example, 13 = 1101₂, so x¹³ = x⁸ · x⁴ · x¹. Walk the bits from the lowest, squaring x each step (x, x², x⁴, x⁸, …), and multiply the result by the current power whenever the bit is 1.

This version isn't in the `.java` file; it was checked against `Math.pow` on 100,000 random cases and the `MIN_VALUE` cases before being included here.

```java
class Iterative {
    static double myPow(double x, int n) {
        long N = n;
        if (N < 0) { x = 1 / x; N = -N; }
        double result = 1.0;
        while (N > 0) {
            if ((N & 1) == 1) result *= x;     // this bit of the exponent is set
            x *= x;                            // x, x^2, x^4, x^8, ...
            N >>= 1;
        }
        return result;
    }
}
```

### Dry run: 3¹³ (13 = 1101₂)

| N (binary) | lowest bit | result | x after squaring |
|---|---|---|---|
| 1101 | 1 | 1 · 3 = 3 | 9 |
| 110 | 0 | 3 | 81 |
| 11 | 1 | 3 · 81 = 243 | 6561 |
| 1 | 1 | 243 · 6561 = **1,594,323** | |

3¹³ = 1,594,323 ✓

- **Time O(log n), space O(1).**

---

## 6. Comparing the approaches

| | Multiply n times | Recursive halving | Iterative bits |
|---|---|---|---|
| Time | O(n) | O(log n) | O(log n) |
| Space | O(1) | O(log n) | **O(1)** |

The file checks the recursive version against `Math.pow` on 10,000 random cases, plus the special cases.

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 0, negative n, n = `Integer.MIN_VALUE`, x = 1 or −1, x = 0 with negative n (`Infinity`), negative x with odd n.

**Mistakes:**
1. `-n` on an `int` when n = `MIN_VALUE` (it stays negative, and the recursion never reaches 0 properly).
2. Calling the half power twice (O(n)).
3. Integer division in `1 / x` when x is an `int`.
4. Handling negative n as `1 / myPow(x, -n)` with an `int` (the same overflow).

---

## 8. Interview questions and answers

**Q1. Why is it O(log n)?**
> Each call halves the exponent and does O(1) work, so there are about log₂ n calls.

**Q2. Why convert to `long`?**
> `Integer.MIN_VALUE` has no positive `int` counterpart: negating it overflows back to itself. As a `long`, −(−2³¹) = 2³¹ fits.

**Q3. Where else is fast exponentiation used?**
> Modular exponentiation in cryptography (RSA), matrix powers for linear recurrences (Fibonacci in O(log n)), and counting problems like [M07 Count Good Numbers](M07_CountGoodNumbers.md).

### Follow-up questions

**F1. Compute xⁿ mod m for huge n.**
> The same loop, with `result = result * x % m` and `x = x * x % m` using `long` arithmetic.

**F2. Fibonacci in O(log n).** → [E01](../02-Easy/E01_FibonacciNumber.md)
> Raise the matrix [[1, 1], [1, 0]] to the n-th power with the same halving.

**F3. Super Pow (LC 372).**
> aᵇ mod 1337 where b is a huge array of digits: process the digits one by one using a^(10·k + d) = (aᵏ)¹⁰ · aᵈ.

---

## 9. Running the file

```bash
java M01_PowXN.java      # 7/7 tests passed (including the MIN_VALUE trap and 10,000 random cases vs Math.pow)
```

**One-line summary:** *"Convert n to long, flip x for negative n, and compute x^(n/2) once, squaring it (times x if n is odd), for O(log n)."*
