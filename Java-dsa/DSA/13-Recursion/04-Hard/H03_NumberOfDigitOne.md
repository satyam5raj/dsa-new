# H03 · Number of Digit One ⭐

| | |
|---|---|
| **LeetCode** | [#233 Number of Digit One](https://leetcode.com/problems/number-of-digit-one/) |
| **Difficulty** | Hard |
| **Pattern** | Recursion on the **leading digit**: split n into full blocks plus a remainder |
| **Code** | [H03_NumberOfDigitOne.java](H03_NumberOfDigitOne.java) |
| **Run** | `java H03_NumberOfDigitOne.java` |

⭐ = not in `leetcode.md`, added for a key technique. Recursion basics are explained in [E01](../02-Easy/E01_FibonacciNumber.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Count how many times the digit 1 appears when you write out every number from 0 to n.

| n | answer | why |
|---|---|---|
| 13 | 6 | 1, 10, 11 (two), 12, 13 |
| 0 | 0 | |
| 100 | 21 | 1 + 10–19 (11) + 21, 31, …, 91 (8) + 100 (1) |
| 10⁹ | 900000001 | |

n goes up to 10⁹, so counting number by number is too slow.

---

## 2. How to think about it

### Split by the leading digit
Write n = a · p + r, where p is the largest power of 10 that is ≤ n, a is the leading digit and r is the rest. For n = 3456: p = 1000, a = 3, r = 456.

Let f(n) be the number of 1s in 0..n. Split 0..n into three parts:

1. **a full blocks** [0, p), [p, 2p), …, [(a − 1)p, ap). In each block, the **lower** digits run through every value 0..p − 1, so they contribute **a · f(p − 1)** ones.
2. **The leading digit's 1s:**
   - if a > 1, the whole block [p, 2p) starts with a 1: **p** ones;
   - if a = 1, only [p, n] does: **r + 1** ones.
3. **The final partial block** [ap, n]: its lower digits run 0..r, contributing **f(r)**.

```
f(n) = a · f(p − 1) + (a == 1 ? r + 1 : p) + f(r)
f(0) = 0,  and f(n) = 1 for 1 ≤ n ≤ 9
```

f(p − 1) for p = 10ᵈ has a closed form, d · 10ᵈ⁻¹ (each of the d positions is a 1 in a tenth of the numbers). The recursion computes it anyway: f(9) = 1, f(99) = 20, f(999) = 300.

### Dry run: n = 13
p = 10, a = 1, r = 3 → f(9) + (3 + 1) + f(3) = 1 + 4 + 1 = **6** ✓

### Dry run: n = 3456 (from a real run)

| call | p | a | r | a · f(p − 1) | leading | f(r) | result |
|---|---|---|---|---|---|---|---|
| f(99) | 10 | 9 | 9 | 9 · 1 | 10 | 1 | 20 |
| f(999) | 100 | 9 | 99 | 9 · 20 | 100 | 20 | 300 |
| f(56) | 10 | 5 | 6 | 5 · 1 | 10 | 1 | 16 |
| f(456) | 100 | 4 | 56 | 4 · 20 | 100 | 16 | 196 |
| f(3456) | 1000 | 3 | 456 | 3 · 300 | 1000 | 196 | **2096** |

---

## 3. Java concepts used in this problem

### 3.1 Finding the highest power of 10

```java
long p = 1;
while (p * 10 <= n) p *= 10;
```

It multiplies by 10 until the next step would pass n. Using `long` matters: for n close to `Integer.MAX_VALUE`, `p * 10` reaches 10¹⁰, which overflows an `int`.

### 3.2 `/` and `%` to split off digits

`n / p` is the leading digit and `n % p` is everything after it. `n / m % 10` picks out the digit at position m (used by the per-position formula).

### 3.3 An `int` API over a `long` helper

```java
static int countDigitOne(int n) { return (int) f(n); }
```

The intermediate values need `long`, and the final count fits in an `int` for the problem's range, so the wrapper narrows it at the end.

### 3.4 A running total in the test

The test counts the 1s in n and adds them to `running` as n goes from 1 to 20,000, so it can compare f(n) for **every** n in O(total digits) instead of re-counting from scratch each time.

---

## 4. Approach 1: Count digit by digit (the brute force)

### Code ([H03_NumberOfDigitOne.java:65-69](H03_NumberOfDigitOne.java#L65-L69))

```java
static long brute(int n) {
    long c = 0;
    for (int i = 1; i <= n; i++) for (int x = i; x > 0; x /= 10) if (x % 10 == 1) c++;
    return c;
}
```

`x % 10` is the last digit and `x /= 10` drops it. **O(n log n)**: far too slow for 10⁹, but obviously correct.

---

## 5. Approach 2: Recursion on the leading digit (the file's main solution)

### Code ([H03_NumberOfDigitOne.java:41-51](H03_NumberOfDigitOne.java#L41-L51))

```java
static int countDigitOne(int n) { return (int) f(n); }

static long f(long n) {
    if (n <= 0) return 0;
    if (n < 10) return 1;                         // only the number 1
    long p = 1;
    while (p * 10 <= n) p *= 10;                  // highest power of 10 <= n
    long a = n / p, r = n % p;
    long leading = a == 1 ? r + 1 : p;            // numbers whose LEADING digit is 1
    return a * f(p - 1) + leading + f(r);
}
```

### Line by line

| Line | What it does |
|---|---|
| `n <= 0` | no numbers with a 1 |
| `n < 10` | among 0..n with 1 ≤ n ≤ 9, only the number 1 has a 1 |
| the `while` | p = the highest power of 10 that is ≤ n |
| `a`, `r` | the leading digit and the remainder |
| `leading` | how many numbers in 0..n have a leading 1 at position p |
| the return | the full blocks + the leading 1s + the partial block |

### Complexity
- **Time O(log² n)** at worst: each level finds p in O(log n) and recurses on f(p − 1) and f(r), both with fewer digits.
- **Space O(log n)** recursion depth.

---

## 6. Approach 3: Count per digit position (the file's second referee)

### Code ([H03_NumberOfDigitOne.java:54-63](H03_NumberOfDigitOne.java#L54-L63))

```java
static long perPosition(long n) {
    long count = 0;
    for (long m = 1; m <= n; m *= 10) {
        long high = n / (m * 10), cur = n / m % 10, low = n % m;
        if (cur == 0) count += high * m;
        else if (cur == 1) count += high * m + low + 1;
        else count += (high + 1) * m;
    }
    return count;
}
```

For each position m (1s, 10s, 100s, …), split n into `high` (the digits above m), `cur` (the digit at m) and `low` (the digits below). Count how many numbers in 0..n have a 1 at position m:

| cur | count at this position | why |
|---|---|---|
| 0 | high · m | only the `high` full cycles reach a 1 here |
| 1 | high · m + low + 1 | the full cycles, plus the partial run from …1000 to …1low |
| ≥ 2 | (high + 1) · m | the current cycle's 1-run is complete |

### Dry run: n = 3456 (from a real run)

| m | high | cur | low | adds |
|---|---|---|---|---|
| 1 | 345 | 6 | 0 | (345 + 1) · 1 = 346 |
| 10 | 34 | 5 | 6 | (34 + 1) · 10 = 350 |
| 100 | 3 | 4 | 56 | (3 + 1) · 100 = 400 |
| 1000 | 0 | 3 | 456 | (0 + 1) · 1000 = 1000 |

Total **2096**, matching the recursion.

### Dry run: n = 1013 (exercises cur = 0 and cur = 1)

| m | high | cur | low | adds |
|---|---|---|---|---|
| 1 | 101 | 3 | 0 | 102 |
| 10 | 10 | 1 | 3 | 10 · 10 + 3 + 1 = 104 |
| 100 | 1 | 0 | 13 | 1 · 100 = 100 |
| 1000 | 0 | 1 | 13 | 0 + 13 + 1 = 14 |

Total **320**.

- **Time O(log n), space O(1).**

---

## 7. Comparing the approaches

| | Brute force | Leading-digit recursion | Per position |
|---|---|---|---|
| Time | O(n log n) | O(log² n) | **O(log n)** |
| Space | O(1) | O(log n) | O(1) |

The file checks the recursion against a running digit count for every n from 1 to 20,000, and against the per-position formula on 10,000 large random n.

---

## 8. Edge cases and common mistakes

**Edge cases:** n = 0, n < 10, powers of 10 (r = 0 with a = 1), n = 10⁹, n near `Integer.MAX_VALUE`.

**Mistakes:**
1. `int` for p (overflows when n > 2.1 · 10⁸ and p · 10 is computed).
2. Using p for the leading 1s when a = 1 (it should be r + 1).
3. Forgetting the f(r) term for the partial block.
4. In the per-position formula, mixing up the cur = 0 and cur ≥ 2 cases.

---

## 9. Interview questions and answers

**Q1. Why does each full block contribute f(p − 1)?**
> Within a block like [2000, 3000), the lower three digits run through every value 000..999 exactly once, so they contain exactly as many 1s as 0..999 does.

**Q2. Which approach would you present?**
> The per-position formula: it's O(log n), O(1) space and easy to explain with the high/cur/low split. The recursion is a good way to show divide-and-conquer thinking.

### Follow-up questions

**F1. Count any digit d from 1 to 9.**
> Replace 1 with d in the per-position formula: cur < d → high · m, cur == d → high · m + low + 1, cur > d → (high + 1) · m.

**F2. Count the digit 0.**
> Trickier, because leading zeros don't count: the cur = 0 case becomes (high − 1) · m + low + 1, and the top position is skipped.

**F3. Digit DP.**
> The general technique for "count numbers ≤ n with some digit property": walk the digits from the top while tracking whether you're still "tight" against n. It's covered in topic 15.

---

## 10. Running the file

```bash
java H03_NumberOfDigitOne.java      # 6/6 tests passed (including n = 10^9, n = 1..20,000, and 10,000 random n vs per-position)
```

**One-line summary:** *"Split n = a·p + r by its leading digit: f(n) = a·f(p − 1) + (a == 1 ? r + 1 : p) + f(r), or count each digit position with the high/cur/low formula."*
