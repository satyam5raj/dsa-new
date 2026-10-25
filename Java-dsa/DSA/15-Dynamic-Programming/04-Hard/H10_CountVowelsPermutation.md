# H10 · Count Vowels Permutation

| | |
|---|---|
| **LeetCode** | [#1220 Count Vowels Permutation](https://leetcode.com/problems/count-vowels-permutation/) |
| **Difficulty** | Hard |
| **Pattern** | **State-machine counting DP**: dp[last letter], one step per length |
| **Code** | [H10_CountVowelsPermutation.java](H10_CountVowelsPermutation.java) |
| **Run** | `java H10_CountVowelsPermutation.java` |

Matrix-power speed-ups are discussed in [Recursion E01 Fibonacci](../../13-Recursion/02-Easy/E01_FibonacciNumber.md) and fast power in [Recursion M01](../../13-Recursion/03-Medium/M01_PowXN.md).

---

## 1. The problem

Count the strings of length n over {a, e, i, o, u} that follow these rules (x → y means "y may follow x"):

| letter | may be followed by |
|---|---|
| a | e |
| e | a, i |
| i | anything except i |
| o | i, u |
| u | a |

Return the count mod 10⁹ + 7.

| n | 1 | 2 | 5 | 144 |
|---|---|---|---|---|
| answer | 5 | 10 | 68 | 18,208,803 |

---

## 2. How to think about it

### Brute force
Build every valid string: up to 5ⁿ of them.

### Only the last letter matters
To extend a string, you only need to know its last letter.

| step | for this problem |
|---|---|
| **State** | cnt[v] = the number of valid strings of the current length ending in v |
| **Transition** | flip the rules into "who may come **before** v": a ← e, i, u; e ← a, i; i ← e, o; o ← i; u ← i, o |
| **Base** | length 1: every cnt = 1 |
| **Answer** | the sum of the counts after n − 1 steps |

It's counting walks of length n − 1 on a 5-node graph. For a huge n (like 10¹⁸), raise the 5 × 5 transition matrix to the power n − 1 in O(5³ log n), the same trick as fast Fibonacci.

### Dry run (from a real run)

| length | a | e | i | o | u | total |
|---|---|---|---|---|---|---|
| 1 | 1 | 1 | 1 | 1 | 1 | 5 |
| 2 | e+i+u = 3 | a+i = 2 | e+o = 2 | i = 1 | i+o = 2 | 10 |
| 3 | 6 | 5 | 3 | 2 | 3 | 19 |
| 4 | 11 | 9 | 7 | 3 | 5 | 35 |
| 5 | 21 | 18 | 12 | 7 | 10 | **68** |

---

## 3. Java concepts used in this problem

### 3.1 Five rolling variables updated together

```java
long na = (e + i + u) % MOD;
long ne = (a + i) % MOD;
...
a = na; e = ne; i = ni; o = no; u = nu;
```

All five new counts must come from the previous length, so they're computed into temporaries first, then assigned.

### 3.2 Matrix multiplication and fast power

```java
static long[][] mul(long[][] x, long[][] y) { ... z[i][j] = (z[i][j] + x[i][k] * y[k][j]) % MOD; ... }
static long[][] pow(long[][] m, long p) { ... if ((p & 1) == 1) res = mul(res, m); m = mul(m, m); p >>= 1; ... }
```

`M[x][y] = 1` if y may follow x. The entry (x, y) of Mᵏ counts the walks of k steps from x to y, so the sum of all entries of Mⁿ⁻¹ counts all valid strings of length n. `pow` is the binary-exponentiation loop from [Recursion M01](../../13-Recursion/03-Medium/M01_PowXN.md#5-approach-2-iterative-binary-exponentiation), applied to matrices; `res` starts as the identity matrix.

### 3.3 `static final String NEXT[]`

`String NEXT[]` is the C-style way to declare an array; `String[] NEXT` is the usual Java style. They mean the same.

### 3.4 `"aeiou".indexOf(nx)`

It turns a vowel character into its index 0..4 for the next recursive call.

---

## 4. Approach 1: List every string (the referee)

### Code ([H10_CountVowelsPermutation.java:84-91](H10_CountVowelsPermutation.java#L84-L91))

```java
static final String NEXT[] = {"e", "ai", "aeou", "iu", "a"};

static long brute(int remaining, int last) {         // DFS over all strings
    if (remaining == 0) return 1;
    long c = 0;
    for (char nx : NEXT[last].toCharArray()) c += brute(remaining - 1, "aeiou".indexOf(nx));
    return c;
}
```

A DFS that appends every allowed next vowel. The test sums it over all 5 starting letters for n ≤ 10.

---

## 5. Approach 2: Rolling counts (the file's main solution)

### Code ([H10_CountVowelsPermutation.java:38-51](H10_CountVowelsPermutation.java#L38-L51))

```java
static final int MOD = 1_000_000_007;

static int countVowelPermutation(int n) {
    long a = 1, e = 1, i = 1, o = 1, u = 1;
    for (int len = 2; len <= n; len++) {
        long na = (e + i + u) % MOD;                  // a can follow e, i, u
        long ne = (a + i) % MOD;
        long ni = (e + o) % MOD;
        long no = i;
        long nu = (i + o) % MOD;
        a = na; e = ne; i = ni; o = no; u = nu;
    }
    return (int) ((a + e + i + o + u) % MOD);
}
```

- **Time O(n), space O(1).**

---

## 6. Approach 3: Matrix power

### Code ([H10_CountVowelsPermutation.java:54-65](H10_CountVowelsPermutation.java#L54-L65))

```java
static int matrixVersion(long n) {
    long[][] m = {
        {0, 1, 0, 0, 0},     // a -> e
        {1, 0, 1, 0, 0},     // e -> a, i
        {1, 1, 0, 1, 1},     // i -> a, e, o, u
        {0, 0, 1, 0, 1},     // o -> i, u
        {1, 0, 0, 0, 0}};    // u -> a
    long[][] r = pow(m, n - 1);
    long sum = 0;
    for (long[] row : r) for (long x : row) sum += x;
    return (int) (sum % MOD);
}
```

- **Time O(5³ · log n), space O(1).** The file computes n = 10¹⁸ this way: 464,137,718.

---

## 7. Comparing the approaches

| | List every string | Rolling counts | Matrix power |
|---|---|---|---|
| Time | O(5ⁿ) | O(n) | **O(125 log n)** |
| n = 10¹⁸ | impossible | too slow | instant |

The file checks the rolling counts against the listing for n = 1..10, and against the matrix power for many n up to 2,000.

---

## 8. Edge cases and common mistakes

**Edge cases:** n = 1 (5), large n (needs the modulus at every step).

**Mistakes:**
1. Using the "followed by" rules directly when computing "ending in" counts (the arrows must be reversed).
2. Updating the five counts in place.
3. Forgetting `% MOD`.

---

## 9. Interview questions and answers

**Q1. Why reverse the rules?**
> The new count for strings ending in v sums the old counts of every letter that may come **before** v.

**Q2. How does the matrix version work?**
> The transition is linear: the new count vector is a fixed 5 × 5 matrix times the old one. Applying it n − 1 times is the matrix to the power n − 1, which fast exponentiation computes with O(log n) multiplications.

### Follow-up questions

**F1. Knight Dialer (LC 935).**
> The same "count walks on a small graph" DP over the 10 phone digits.

**F2. Count strings that avoid a forbidden substring.**
> Build an automaton of the forbidden pattern (KMP) and count walks on its states.

**F3. Climbing Stairs with matrix power.** → [E01](../02-Easy/E01_ClimbingStairs.md)

---

## 10. Running the file

```bash
java H10_CountVowelsPermutation.java      # 6/6 tests passed; prints n = 10^18 via matrix power: 464137718
```

**One-line summary:** *"Keep a count of valid strings ending in each vowel and update all five each step from the letters allowed before them; for huge n, raise the 5 × 5 transition matrix to the power n − 1."*
