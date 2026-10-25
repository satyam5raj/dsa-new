# M07 · Count Good Numbers ⭐

| | |
|---|---|
| **LeetCode** | [#1922 Count Good Numbers](https://leetcode.com/problems/count-good-numbers/) |
| **Difficulty** | Medium |
| **Pattern** | Counting → a product of powers → **fast modular exponentiation** (recursive) |
| **Code** | [M07_CountGoodNumbers.java](M07_CountGoodNumbers.java) |
| **Run** | `java M07_CountGoodNumbers.java` |

⭐ = not in `leetcode.md`, added for a key technique. Fast power is explained in [M01 Pow(x, n)](M01_PowXN.md).

---

## 1. The problem

A digit string (leading zeros allowed) is **good** if:
- every digit at an **even** index (0, 2, 4, …) is even: 0, 2, 4, 6 or 8, which is **5** choices;
- every digit at an **odd** index is prime: 2, 3, 5 or 7, which is **4** choices.

Count the good strings of length n, modulo 10⁹ + 7. n can be as large as 10¹⁵.

| n | answer | why |
|---|---|---|
| 1 | 5 | one even position |
| 4 | 400 | 5 · 4 · 5 · 4 |
| 50 | 564908303 | 5²⁵ · 4²⁵ mod 10⁹ + 7 |

---

## 2. How to think about it

### Counting
The positions are independent, so **multiply** the choices:
- there are ⌈n/2⌉ even positions and ⌊n/2⌋ odd positions;
- answer = 5^⌈n/2⌉ · 4^⌊n/2⌋ (mod 10⁹ + 7).

### The problem: huge exponents
The exponents can be 5 · 10¹⁴, so multiplying one at a time is impossible. Use fast power from [M01](M01_PowXN.md), taking the modulus at every step:

```
pow(b, e) = pow(b, e/2)²           if e is even
pow(b, e) = pow(b, e/2)² · b       if e is odd        (every multiplication mod M)
```

### Why `long`, and why mod after every multiplication
Every intermediate value is below M = 10⁹ + 7, so a product of two of them is below about 10¹⁸, which fits in a `long` (max ≈ 9.2 · 10¹⁸). But a product of **three** could overflow, so reduce after **each** multiplication.

### Dry run: n = 4
evens = (4 + 1) / 2 = 2, odds = 4 / 2 = 2.

| call | half | sq = half² mod M | result |
|---|---|---|---|
| modPow(5, 0) | | | 1 |
| modPow(5, 1) | 1 | 1 | odd → 1 · 5 = 5 |
| modPow(5, 2) | 5 | 25 | even → 25 |
| modPow(4, 2) | (same steps) | | 16 |

Answer = 25 · 16 mod M = **400** ✓

---

## 3. Java concepts used in this problem

### 3.1 `long` literals and constants

```
static final long MOD = 1_000_000_007L;
countGoodNumbers(1_000_000_000_000_000L)
```

The `L` suffix makes a `long` literal. Without it, a number above 2,147,483,647 doesn't compile as an `int`. Underscores are just for readability.

### 3.2 Rounding up with integer division

`(n + 1) / 2` is ⌈n/2⌉ and `n / 2` is ⌊n/2⌋. Together they always add up to n.

### 3.3 Narrowing with a cast

```java
return (int) (modPow(5, evens) * modPow(4, odds) % MOD);
```

The result is below M, which fits in an `int`, so the explicit `(int)` cast is safe. Java requires the cast because `long → int` can lose information in general. The parentheses make the cast apply to the whole expression, not just the first factor.

### 3.4 `%` binds like `*`

`half * half % MOD` is `(half * half) % MOD`: `*` and `%` have the same precedence and are evaluated left to right.

### 3.5 `BigInteger` (in the test)

```java
BigInteger.valueOf(b).modPow(BigInteger.valueOf(e), BigInteger.valueOf(MOD)).longValue()
```

`java.math.BigInteger` represents arbitrarily large integers and has a built-in `modPow`. It's slower, but it can never overflow, which makes it a reliable referee.

### 3.6 An "odometer" loop to enumerate digit strings (the referee)

```java
int i = n - 1;
while (i >= 0 && digits[i] == 9) digits[i--] = 0;     // roll the 9s over to 0
if (i < 0) break;                                     // every digit was 9: done
digits[i]++;
```

It counts through 000…0 to 999…9 like a car's odometer.

---

## 4. Approach 1: Enumerate every string (the referee)

### Code ([M07_CountGoodNumbers.java:50-66](M07_CountGoodNumbers.java#L50-L66))

```java
static int brute(int n) {
    int count = 0;
    int[] digits = new int[n];
    while (true) {
        boolean good = true;
        for (int i = 0; i < n && good; i++) {
            int d = digits[i];
            good = i % 2 == 0 ? d % 2 == 0 : (d == 2 || d == 3 || d == 5 || d == 7);
        }
        if (good) count++;
        int i = n - 1;
        while (i >= 0 && digits[i] == 9) digits[i--] = 0;
        if (i < 0) break;
        digits[i]++;
    }
    return count;
}
```

| Line | What it does |
|---|---|
| `digits` | the current string, starting at all zeros |
| the check loop | stops at the first digit that breaks a rule |
| the odometer | move to the next string |

- **Time O(10ⁿ · n).** The test uses it for n ≤ 6.

---

## 5. Approach 2: Count + fast modular power (the file's main solution)

### Code ([M07_CountGoodNumbers.java:35-47](M07_CountGoodNumbers.java#L35-L47))

```java
static final long MOD = 1_000_000_007L;

static int countGoodNumbers(long n) {
    long evens = (n + 1) / 2, odds = n / 2;
    return (int) (modPow(5, evens) * modPow(4, odds) % MOD);
}

static long modPow(long base, long exp) {
    if (exp == 0) return 1;
    long half = modPow(base, exp / 2);
    long sq = half * half % MOD;                       // reduce after EVERY multiply
    return exp % 2 == 0 ? sq : sq * (base % MOD) % MOD;
}
```

### Line by line

| Line | What it does |
|---|---|
| `evens`, `odds` | how many positions of each kind |
| the return | 5^evens · 4^odds, reduced mod M |
| `exp == 0` | anything to the power 0 is 1 |
| `half` | the recursive call, made **once** |
| `sq` | half², reduced |
| the odd case | one more factor of `base`, reduced first in case base ≥ M |

### Complexity
- **Time O(log n):** about 50 calls for n = 10¹⁵.
- **Space O(log n)** recursion.

---

## 6. Comparing the approaches

| | Enumerate | Count + fast power |
|---|---|---|
| Time | O(10ⁿ · n) | **O(log n)** |
| Works for n = 10¹⁵ | no | yes, instantly |

The file checks n = 1..6 against the enumeration and checks `modPow` against `BigInteger.modPow` on 1,000 random cases.

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 1 (5), even and odd n, n = 10¹⁵.

**Mistakes:**
1. Taking `% MOD` only at the end (the intermediate products overflow `long`).
2. Using `int` for the exponent or the intermediate values.
3. Swapping the counts (even positions are ⌈n/2⌉, because index 0 is even).
4. Calling `modPow(base, exp/2)` twice (O(n)).
5. Writing `(int) x * y % MOD` (the cast applies only to `x`).

---

## 8. Interview questions and answers

**Q1. Why is the answer a product?**
> Each position's choice is independent of the others (the multiplication principle), so the total number of strings is the product of the choices per position.

**Q2. Why does the modulus keep everything in range?**
> (a · b) mod M = ((a mod M) · (b mod M)) mod M, so reducing after each multiplication keeps every value below M, and every product below M², which fits in a `long`.

### Follow-up questions

**F1. The iterative version.**
> The binary-exponentiation loop from [M01](M01_PowXN.md#5-approach-2-iterative-binary-exponentiation), with `% MOD` after each multiplication.

**F2. Modular inverse (for division under a prime modulus).**
> By Fermat's little theorem, a⁻¹ ≡ a^(M − 2) mod M, which is the same `modPow`.

**F3. What if the products could exceed a `long`?**
> Use `Math.multiplyHigh`, `BigInteger`, or "multiply by doubling" (the same halving trick applied to multiplication).

---

## 9. Running the file

```bash
java M07_CountGoodNumbers.java      # 6/6 tests passed (including n = 10^15 and 1,000 random modPow vs BigInteger)
```

**One-line summary:** *"There are 5 choices at each of the ⌈n/2⌉ even positions and 4 at each of the ⌊n/2⌋ odd ones, so compute 5^⌈n/2⌉ · 4^⌊n/2⌋ with recursive fast power, reducing mod 10⁹ + 7 after every multiplication."*
