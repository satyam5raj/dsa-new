# 00 · Foundations

| | |
|---|---|
| **Code** | [Foundations.java](Foundations.java): every example below is computed and checked (`java Foundations.java`, 51 checks) |
| **You need** | school arithmetic |
| **Used later in** | everything: logs in complexity and ML losses, sums in algorithm analysis, logic in proofs and code, induction in recursion |

This chapter is the toolbox. None of it is hard, but if any piece is shaky, the later chapters feel harder than they are. Read it once, run the code, do the exercises.

---

## 1. Functions

### What a function is
A **function** is a rule that takes an input and gives **exactly one** output.

```
f(x) = 2x + 3          f(4) = 2·4 + 3 = 11
```

Think of a vending machine: you put in a code, you get one specific snack. The same code always gives the same snack. That "always the same output for the same input" is what makes something a function. A Java method with no side effects is exactly a mathematical function.

| Word | Meaning | Example for f(x) = 2x + 3 |
|---|---|---|
| **domain** | allowed inputs | all real numbers |
| **range** (image) | outputs that actually happen | all real numbers |
| **graph** | all points (x, f(x)) drawn on a plane | a straight line |

### Composition: functions in a pipeline
`g(f(x))`, written **(g ∘ f)(x)**, means "apply f first, then g".

```
f(x) = 2x + 3,  g(x) = x²
g(f(4)) = g(11) = 121
f(g(4)) = f(16) = 35          ← different: the ORDER matters
```

This is exactly a chain of method calls, or a Unix pipe. A **neural network is one big composition**: layer 3 of (layer 2 of (layer 1 of the input)). Calculus's chain rule (chapter 03) is how you differentiate compositions, and that's what backpropagation is.

### Inverse: undoing a function
f⁻¹ undoes f: `f⁻¹(f(x)) = x`. To find it, solve `y = 2x + 3` for x: `x = (y − 3) / 2`. A function has an inverse only if no two inputs give the same output (it's **one-to-one**); `x²` has none over all reals, because 2 and −2 both give 4.

**In CS:** encryption/decryption, encoding/decoding, `exp` and `log` (next section).

### Common function shapes
| Function | Shape | Where you meet it |
|---|---|---|
| constant `c` | flat line | O(1) |
| linear `ax + b` | straight line | O(n), linear regression |
| quadratic `x²` | U-shaped parabola | O(n²), squared error loss |
| polynomial `x³ + …` | wiggly curves | O(n³) |
| exponential `2ˣ` | explodes upward | O(2ⁿ), brute-force subsets |
| logarithm `log x` | rises slower and slower | O(log n), binary search |

---

## 2. Exponents and logarithms

### Exponents
`2⁵ = 2·2·2·2·2 = 32`: the exponent says how many times to multiply.

| Rule | Example | Why |
|---|---|---|
| `aᵐ · aⁿ = aᵐ⁺ⁿ` | 2³·2⁴ = 2⁷ = 128 | 3 twos times 4 twos = 7 twos |
| `(aᵐ)ⁿ = aᵐⁿ` | (2³)² = 2⁶ = 64 | 2³ multiplied by itself twice |
| `aᵐ / aⁿ = aᵐ⁻ⁿ` | 2⁵ / 2² = 2³ | cancel 2 twos |
| `a⁰ = 1` | 7⁰ = 1 | aⁿ / aⁿ = a⁰ = 1 |
| `a⁻ⁿ = 1 / aⁿ` | 2⁻³ = 1/8 = 0.125 | |
| `a^(1/2) = √a` | 9^(1/2) = 3 | |

### Logarithms: "which power?"
A logarithm answers the question: **"what power do I raise the base to, to get this number?"**

```
log₂(8) = 3      because 2³ = 8
log₁₀(1000) = 3  because 10³ = 1000
log₂(1024) = 10
```
`log` is the **inverse** of the exponential: `log₂(2ˣ) = x` and `2^(log₂ x) = x`.

### The most important intuition in CS
**log₂ n = how many times you can halve n before you reach 1.**

```
1,000,000 → 500,000 → 250,000 → … → 1      19 halvings,   log₂(1,000,000) ≈ 19.93
```
That's why:
- **binary search** takes about log₂ n steps (each step halves the search range);
- a **balanced binary tree** with n nodes has height about log₂ n;
- **merge sort** has log₂ n levels of splitting;
- writing n in binary takes **⌊log₂ n⌋ + 1 bits** (1,000,000 needs 20 bits).

log grows **extremely slowly**: log₂ of a billion is only about 30. That's why O(log n) algorithms feel instant.

### Log rules (they turn multiplication into addition)
| Rule | Example |
|---|---|
| `log(a·b) = log a + log b` | log₂(8·32) = 3 + 5 = 8 |
| `log(a/b) = log a − log b` | log₂(64/4) = 6 − 2 = 4 |
| `log(aᵏ) = k · log a` | log₂(5³) = 3 · log₂5 |
| change of base: `log_b x = ln x / ln b` | log₂ x = ln x / ln 2 (how Java computes it) |

### e and the natural log
`e ≈ 2.71828` is the number where the function `eˣ` is its own slope (chapter 03 explains why that matters). `ln x = log_e x`. In ML, "log" almost always means `ln`. `ln(e) = 1` and `e^(ln 7) = 7`.

### Why ML loves logs: products become sums
A model's probability for a dataset is a **product** of many small numbers. Multiply 400 values of 0.1 and a computer gets exactly **0.0**, because the true value 10⁻⁴⁰⁰ is far below what a `double` can hold (about 10⁻³⁰⁸). This is called **underflow**. The **log** of the product is a **sum**: 400 × log₁₀(0.1) = **−400**, perfectly representable. This is why ML maximises **log-likelihood** and minimises **log-loss** (chapters 05 and 07). The code shows both numbers.

---

## 3. Sums and products (Σ and Π notation)

### Reading Σ
```
  n
  Σ  i   =  1 + 2 + 3 + … + n
 i=1
```
Read it as a **for loop**: `for (i = 1; i <= n; i++) sum += i;`. The bottom says where to start, the top where to stop, and the right side what to add. **Π** is the same with multiplication: `Π i = 1·2·…·n = n!`.

### The sums you'll use constantly
| Sum | Closed form | Example | Where |
|---|---|---|---|
| `1 + 2 + … + n` | **n(n+1)/2** | n = 100 → 5050 | nested loops, pairs |
| `1² + 2² + … + n²` | **n(n+1)(2n+1)/6** | n = 10 → 385 | triple-loop analysis |
| `1 + 2 + 4 + … + 2ⁿ⁻¹` | **2ⁿ − 1** | n = 10 → 1023 | binary trees, doubling arrays |
| `1/2 + 1/4 + 1/8 + …` | **→ 1** | | halving work |
| `1 + 1/2 + 1/3 + … + 1/n` | **≈ ln n + 0.5772** | n = 10⁶ → 14.392727 | quicksort analysis, coupon collector |
| pairs `i < j` among n | **n(n−1)/2** | n = 50 → 1225 | comparing all pairs |

### Why 1 + 2 + … + n = n(n+1)/2 (Gauss's trick)
Write the sum forwards and backwards and add:
```
  1   +   2   + … +   n
  n   + (n−1) + … +   1
(n+1) + (n+1) + … + (n+1)   = n copies of (n+1)
```
Two copies of the sum equal n(n+1), so one copy is n(n+1)/2.

**CS connection:** a loop `for i in 0..n: for j in i+1..n` runs `n(n−1)/2` times, which is O(n²). The code counts it: 1225 for n = 50.

### Why 1 + 2 + 4 + … + 2ⁿ⁻¹ = 2ⁿ − 1
In binary, the sum is `111…1` (n ones), and adding 1 gives `1000…0` = 2ⁿ. **CS connection:** a full binary tree with n levels has 2ⁿ − 1 nodes; a dynamic array that doubles has done less than 2n copies in total, so `ArrayList.add` is amortised O(1).

### Rules for manipulating sums
- constants come out: `Σ c·aᵢ = c · Σ aᵢ`;
- sums split: `Σ (aᵢ + bᵢ) = Σ aᵢ + Σ bᵢ`;
- double sums are nested loops: `Σᵢ Σⱼ aᵢⱼ`.

---

## 4. Floor, ceiling, absolute value, mod

| Notation | Meaning | Examples |
|---|---|---|
| `⌊x⌋` floor | largest integer ≤ x | ⌊3.7⌋ = 3, **⌊−3.7⌋ = −4** |
| `⌈x⌉` ceiling | smallest integer ≥ x | ⌈3.2⌉ = 4 |
| `|x|` | distance from 0 | |−5| = 5 |
| `a mod b` | remainder after dividing by b | 17 mod 5 = 2 |

**Ceiling with integers**: `⌈a/b⌉ = (a + b − 1) / b` for positive integers, e.g. ⌈7/2⌉ = (7 + 1)/2 = 4. It's used for "how many pages/batches do I need?".

### A real Java trap: `%` and `/` with negative numbers
In maths, `−7 mod 3 = 2` (the remainder is never negative: −7 = −3·3 + 2). Java's `%` follows the sign of the left operand:

| Expression | Java | Maths |
|---|---|---|
| `-7 / 3` | **−2** (rounds toward zero) | `Math.floorDiv(-7, 3)` = −3 |
| `-7 % 3` | **−1** | `Math.floorMod(-7, 3)` = 2 |

This bites in hashing (`hashCode() % n` can be negative and crash an array index) and in circular buffers. Use `Math.floorMod`.

---

## 5. Logic

### Propositions and connectives
A **proposition** is a statement that is true or false ("7 is prime").

| Symbol | Name | Java | True when |
|---|---|---|---|
| ¬p | NOT | `!p` | p is false |
| p ∧ q | AND | `p && q` | both are true |
| p ∨ q | OR | `p \|\| q` | at least one is true |
| p ⊕ q | XOR | `p ^ q` | exactly one is true |
| p → q | IMPLIES | `!p \|\| q` | p is false, or q is true |
| p ↔ q | IFF | `p == q` | both have the same value |

### Implication: the one that confuses everyone
"If it rains (p), the ground is wet (q)."

| p | q | p → q | Why |
|---|---|---|---|
| T | T | **T** | promise kept |
| T | F | **F** | the only way to break the promise |
| F | T | **T** | no rain, wet ground anyway: the promise says nothing |
| F | F | **T** | no rain, dry ground: fine |

The code prints `TFTT`. Remember: **an implication is only false when the "if" part is true and the "then" part is false.**

### Contrapositive vs converse
| | Statement | Same meaning as p → q? |
|---|---|---|
| contrapositive | ¬q → ¬p ("if the ground isn't wet, it didn't rain") | **yes, always** |
| converse | q → p ("if the ground is wet, it rained") | **no** (sprinklers exist) |

The code checks all four cases of both. Proof by contrapositive (section 6) uses this.

### De Morgan's laws
```
¬(p ∧ q)  =  ¬p ∨ ¬q        "not (both)" = "at least one is not"
¬(p ∨ q)  =  ¬p ∧ ¬q        "not (either)" = "neither"
```
You use these to simplify `if` conditions: `!(a && b)` becomes `!a || !b`.

### Quantifiers
| Symbol | Meaning | Java (streams) |
|---|---|---|
| ∀x | "for all x" | `allMatch` |
| ∃x | "there exists an x" | `anyMatch` |

**Negating them swaps them:** ¬(∀x: x is even) = ∃x: x is odd. To disprove "every x is even", you only need **one** odd x: a **counterexample**. For {2, 4, 6, 7}, the 7 does it.

### Logic in bits
The same operators work on every bit of an integer: `5 ^ 3 = 6` because `101 XOR 011 = 110`. Bit manipulation problems, Bloom filters and hashing all build on this.

---

## 6. Proofs

### Why bother with proofs?
Testing shows a claim holds for the cases you tried; a proof shows it holds for **all** cases. Example from the code: `n² + n + 41` is prime for n = 0, 1, 2, …, 39 (40 cases in a row!), and then **fails at n = 40** (40² + 40 + 41 = 41²). Lots of examples are not a proof.

### Direct proof
Start from what you know and reason step by step.
> **Claim:** the sum of two even numbers is even.
> **Proof:** even numbers are 2a and 2b. Their sum is 2a + 2b = 2(a + b), which is 2 times an integer, so it's even. ∎

### Proof by contrapositive
To prove p → q, prove ¬q → ¬p instead (it's equivalent).
> **Claim:** if n² is even, then n is even.
> **Proof (contrapositive):** suppose n is odd, n = 2k + 1. Then n² = 4k² + 4k + 1 = 2(2k² + 2k) + 1, which is odd. ∎

### Proof by contradiction
Assume the claim is false, and show that leads to something impossible.
> **Claim:** √2 is not a fraction.
> **Proof:** suppose √2 = p/q in lowest terms. Then 2q² = p², so p² is even, so p is even (by the previous claim): p = 2k. Then 2q² = 4k², so q² = 2k², and q is even too. But then p/q wasn't in lowest terms. Contradiction. ∎
> (The code checks that no fraction with denominator up to 10,000 works, which supports the claim but doesn't prove it.)

### Proof by induction: the domino effect
To prove a statement P(n) for every n ≥ 1:
1. **Base case:** prove P(1). (The first domino falls.)
2. **Inductive step:** prove that **if** P(k) is true, **then** P(k+1) is true. (Each domino knocks over the next.)

Then every domino falls.

> **Claim:** 1 + 2 + … + n = n(n+1)/2.
> **Base:** n = 1: 1 = 1·2/2 ✓.
> **Step:** assume 1 + … + k = k(k+1)/2. Then
> 1 + … + k + (k+1) = k(k+1)/2 + (k+1) = (k+1)(k/2 + 1) = (k+1)(k+2)/2,
> which is the formula for n = k + 1 ✓. ∎

**Induction = recursion.** A recursive function trusts that the smaller call works (the inductive hypothesis) and handles a base case. Proving a recursive algorithm correct is literally an induction proof.

**Strong induction:** assume P(1), …, P(k) are all true and prove P(k+1). That's useful when a problem splits into smaller pieces of any size (like merge sort).

### Loop invariants: induction for loops
A **loop invariant** is a statement that's true before the loop, stays true after each iteration, and gives you the answer at the end.

Binary search's invariant: *"if the target is in the array, it is inside `a[lo..hi]`"*.
- **Before:** lo = 0, hi = n − 1 covers everything ✓.
- **Each step:** if `a[mid] < target`, the target (if present) is right of mid, so `lo = mid + 1` keeps the invariant; the other case is symmetric.
- **End:** if `lo > hi`, the range is empty, so the target isn't there.

The code tests binary search on 3,010 targets. The invariant is what **proves** it's right for every input.

---

## 7. Exercises (answers below)

1. Compute (f ∘ g)(2) and (g ∘ f)(2) for f(x) = x + 1, g(x) = 3x.
2. Simplify log₂(64) − log₂(8) + log₂(1).
3. How many bits does 4096 need? How many halvings until 1?
4. Evaluate Σ_{i=1}^{20} (2i + 1).
5. What does Java print for `-10 % 4`? What's the maths answer?
6. Negate: "every user has a password longer than 8 characters".
7. Prove by induction: 1 + 3 + 5 + … + (2n − 1) = n².
8. How many times does the inner statement run? `for i in 0..n-1: for j in 0..i-1: work()`

<details>
<summary>Answers</summary>

1. f(g(2)) = f(6) = 7; g(f(2)) = g(3) = 9.
2. 6 − 3 + 0 = 3.
3. 4096 = 2¹², so 13 bits (`1` followed by 12 zeros); 12 halvings.
4. 2·(20·21/2) + 20 = 420 + 20 = 440.
5. Java: −2. Maths (`Math.floorMod`): 2.
6. "There exists a user whose password has at most 8 characters."
7. Base: n = 1: 1 = 1² ✓. Step: if 1 + … + (2k−1) = k², add the next odd number 2k + 1: k² + 2k + 1 = (k+1)² ✓.
8. 0 + 1 + … + (n−1) = n(n−1)/2, so O(n²).
</details>

---

## 8. Interview questions

**Q1. Why is binary search O(log n)?**
> Each comparison halves the remaining range; the number of halvings until one element is left is about log₂ n.

**Q2. Why do ML models use log-probabilities?**
> Products of many probabilities underflow to 0 in floating point; logs turn products into sums, which are numerically stable and easier to differentiate.

**Q3. What is a loop invariant, and why is it useful?**
> A property true before and after every iteration. It's the induction argument for a loop, and the standard way to prove an algorithm correct (and to write it correctly in the first place).

**Q4. Is 1 + 2 + … + n O(n) or O(n²)?**
> The value is n(n+1)/2 = Θ(n²). Computing it with a loop takes O(n) time; with the formula, O(1).

**Q5. Why is `hashCode() % size` dangerous in Java?**
> `hashCode()` can be negative, and Java's `%` then gives a negative remainder, which is an invalid array index. Use `Math.floorMod` or mask the sign bit.

---

**Next:** [01 · Discrete Mathematics](../01-Discrete-Math/README.md): sets, counting, graphs, recurrences, number theory.
