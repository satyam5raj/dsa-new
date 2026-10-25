# M02 · Count Primes

| | |
|---|---|
| **LeetCode** | [#204 Count Primes](https://leetcode.com/problems/count-primes/) |
| **Difficulty** | Medium |
| **Pattern** | Sieve of Eratosthenes (a boolean array used as a lookup table) |
| **Code** | [M02_CountPrimes.java](M02_CountPrimes.java) |
| **Run** | `java M02_CountPrimes.java` |

---

## 1. The problem

Count the primes **strictly less than** n.

| n | answer | primes |
|---|---|---|
| 10 | 4 | 2, 3, 5, 7 |
| 0, 1, 2 | 0 | |
| 3 | 1 | 2 |
| 20 | 8 | 2, 3, 5, 7, 11, 13, 17, 19 |

**Constraints:** `0 ≤ n ≤ 5 · 10⁶`.

It's in the hashing list because the sieve is a **direct address table**: a `boolean[]` indexed by the number itself, the simplest possible hash table.

---

## 2. How to think about it

### Brute force: test every number
Trial division up to √x for each x < n: O(n √n), about 3 · 10⁹ steps for n = 5 · 10⁶. Too slow.

### The sieve: cross out instead of testing
Instead of asking "is x prime?" for every x, **cross out the multiples of each prime**. Whatever is never crossed out is prime.

```
composite = new boolean[n]
for p = 2 while p·p < n:
    if p is not crossed out (so p is prime):
        cross out p·p, p·p + p, p·p + 2p, …
count the uncrossed numbers from 2 to n − 1
```

**Why start crossing at p·p?** Any smaller multiple p·m with m < p has a smaller prime factor, so it was already crossed out by that prime.

**Why stop at √n?** Every composite c < n has a prime factor ≤ √c < √n, so it's crossed out by then.

### Dry run: n = 20

```
p = 2: cross 4, 6, 8, 10, 12, 14, 16, 18
p = 3: cross 9, 12, 15, 18         (12 and 18 were already crossed)
p = 4: already crossed → skip
p = 5: 25 ≥ 20 → stop

 2  3  ✗4  5  ✗6  7  ✗8  ✗9  ✗10  11  ✗12  13  ✗14  ✗15  ✗16  17  ✗18  19
left: 2, 3, 5, 7, 11, 13, 17, 19 → 8
```

---

## 3. Java concepts used in this problem

### 3.1 A `boolean[]` as a lookup table

`new boolean[n]` is all `false` by default. Here `false` means "not crossed out yet" (prime so far). Indexing by the number itself gives O(1) lookups with no hashing at all.

Memory: one byte per boolean in practice, so n = 5 · 10⁶ uses 5 MB. A `java.util.BitSet` would use n/8 bytes.

### 3.2 Avoiding overflow with `(long) p * p`

```java
for (int p = 2; (long) p * p < n; p++)
```

`p * p` in `int` overflows once p > 46,340 (since 46,341² > 2³¹ − 1) and wraps to a negative number, so `p * p < n` would wrongly stay true. Casting **one** operand to `long` makes the multiplication happen in 64 bits. (With n ≤ 5 · 10⁶ the loop stops at p ≈ 2236, but the cast is the safe habit.)

The inner loop's `m = p * p` is safe because it only runs when `p * p < n`.

### 3.3 Number literals with underscores

`5_000_000` is just 5000000. The underscores are ignored by the compiler and are there for readability.

---

## 4. Approach 1: Trial division

### Code ([M02_CountPrimes.java:56-60](M02_CountPrimes.java#L56-L60))

```java
static boolean isPrime(int x) {
    if (x < 2) return false;
    for (int d = 2; (long) d * d <= x; d++) if (x % d == 0) return false;
    return true;
}
```

| Line | What it does |
|---|---|
| `if (x < 2) return false;` | 0 and 1 aren't prime |
| `d * d <= x` | a divisor pair (d, x/d) always has one member ≤ √x, so checking up to √x is enough |
| `x % d == 0` | d divides x → composite |

Counting with it: `for (int x = 2; x < n; x++) if (isPrime(x)) count++;`, which is **O(n √n)**. The file uses it as the test oracle for n = 0..3000.

---

## 5. Approach 2: The Sieve of Eratosthenes (the file's solution)

### Code ([M02_CountPrimes.java:43-54](M02_CountPrimes.java#L43-L54))

```java
static int countPrimes(int n) {
    if (n < 3) return 0;
    boolean[] composite = new boolean[n];
    for (int p = 2; (long) p * p < n; p++) {
        if (!composite[p]) {
            for (int m = p * p; m < n; m += p) composite[m] = true;
        }
    }
    int count = 0;
    for (int i = 2; i < n; i++) if (!composite[i]) count++;
    return count;
}
```

### Line by line

| Line | What it does |
|---|---|
| `if (n < 3) return 0;` | no primes below 2 or 3 (2 itself isn't **strictly less** than n = 2) |
| `boolean[] composite = new boolean[n];` | indices 0..n−1; everything starts as "maybe prime" |
| `(long) p * p < n` | only primes up to √n need to cross anything out |
| `if (!composite[p])` | p wasn't crossed out by a smaller prime → p is prime |
| `for (int m = p * p; m < n; m += p)` | cross out p², p² + p, … |
| the counting loop | count the survivors from 2 (0 and 1 are never counted) |

### Dry run: n = 10

| p | prime? | crossed out | composite after (indices 2..9) |
|---|---|---|---|
| 2 | yes | 4, 6, 8 | 2 3 ✗ 5 ✗ 7 ✗ 9 |
| 3 | yes | 9 | 2 3 ✗ 5 ✗ 7 ✗ ✗ |
| 4 | 16 ≥ 10 → the loop ends | | |

Survivors 2, 3, 5, 7 → **4** ✓

### Complexity
- **Time O(n log log n):** each prime p crosses out about n/p numbers, and the sum of 1/p over the primes up to n grows like log log n. In practice it's linear: the file counts the primes below 5,000,000 (348,513) in a few tens of milliseconds.
- **Space O(n).**

---

## 6. Comparing the approaches

| | Trial division | Sieve |
|---|---|---|
| Time | O(n √n) | **O(n log log n)** |
| Space | O(1) | O(n) |
| Good for | testing one number | all the primes up to n |

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 0, 1, 2 (answer 0), n = 3 (answer 1), large n.

**Mistakes:**
1. Counting primes ≤ n instead of < n.
2. Starting the inner loop at `2 * p` (correct, but slower) or at `p` (crosses out p itself!).
3. `p * p` overflowing in `int` in the loop condition.
4. Counting 0 and 1 as primes.

---

## 8. Interview questions and answers

**Q1. Why does the sieve start at p²?**
> Any multiple p·m with m < p has a prime factor smaller than p, so an earlier prime already crossed it out.

**Q2. Why is the outer loop bounded by √n?**
> A composite number below n always has a prime factor ≤ √n. After processing those primes, every composite is crossed out; the larger primes would only cross out numbers ≥ p² ≥ n.

**Q3. How would you reduce the memory?**
> Store only the odd numbers (half the memory), use a `BitSet` (1 bit per number), or use a **segmented sieve** that processes blocks of √n numbers at a time.

### Follow-up questions

**F1. List the primes, not just count them.**
> Collect the indices i ≥ 2 with `!composite[i]`.

**F2. Count the primes in a range [L, R] where R is huge but R − L is small.**
> A segmented sieve: sieve the primes up to √R, then cross out their multiples inside [L, R] only.

**F3. The smallest prime factor of every number (for fast factorisation).**
> A variant: `spf[m] = p` the first time m is crossed out. Then factorising x takes O(log x) by repeatedly dividing by spf[x].

**F4. A linear sieve.**
> Cross each composite out exactly once, using its smallest prime factor: O(n), and it yields spf for free.

---

## 9. Running the file

```bash
java M02_CountPrimes.java      # 7/7 tests passed (including n = 0..3000 vs trial division, and 348,513 primes below 5,000,000)
```

**One-line summary:** *"Cross out the multiples of each prime p from p² onward, for p up to √n; the numbers left uncrossed in 2..n−1 are the primes."*
