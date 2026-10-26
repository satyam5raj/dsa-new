# 01 · Discrete Mathematics

| | |
|---|---|
| **Code** | [DiscreteMath.java](DiscreteMath.java): every example below is computed and checked (`java DiscreteMath.java`, 56 checks) |
| **You need** | [00 · Foundations](../00-Foundations/README.md) |
| **Used in** | algorithms and complexity, DSA interviews, databases (sets), networks (graphs), cryptography and hashing (number theory), probability (counting) |

"Discrete" means **separate, countable things**: integers, nodes, bits, people. No smooth curves. It's the maths computers actually run on, which is why it's the core of computer science.

---

## 1. Sets

### What a set is
A **set** is a collection of distinct things, where order doesn't matter: `{1, 2, 3}` = `{3, 1, 2}`, and `{1, 1, 2}` is just `{1, 2}`. Java's `HashSet` is exactly this.

| Notation | Meaning | Example (A = {1,2,3,4}, B = {3,4,5}) |
|---|---|---|
| `x ∈ A` | x is in A | 2 ∈ A |
| `A ∪ B` | **union**: in A or B | {1, 2, 3, 4, 5} |
| `A ∩ B` | **intersection**: in both | {3, 4} |
| `A \ B` | **difference**: in A, not B | {1, 2} |
| `Aᶜ` | **complement**: everything not in A | |
| `A ⊆ B` | A is a **subset** of B | {3} ⊆ B |
| `∅` | the empty set | |
| `|A|` | **size** (cardinality) | |A| = 4 |
| `A × B` | **Cartesian product**: all pairs (a, b) | |A × B| = 4·3 = 12 |

In Java: `addAll` = union, `retainAll` = intersection, `removeAll` = difference. In SQL: `UNION`, `INTERSECT`, `EXCEPT`, and a `JOIN` without a condition is a Cartesian product.

### The power set: all subsets
The **power set** of S is the set of all its subsets. For {a, b, c} there are 8:

```
{}  {a}  {b}  {ab}  {c}  {ac}  {bc}  {abc}
```
A set of n elements has **2ⁿ subsets**: each element is either in or out, 2 choices n times. **The CS trick:** the numbers 0 … 2ⁿ − 1 in binary *are* the subsets (bit i = "take element i"). Mask `101` = {a, c}. That's how bitmask DP and the "Subsets" backtracking problem work.

### Inclusion–exclusion: counting unions without double counting
```
|A ∪ B| = |A| + |B| − |A ∩ B|                      (the overlap was counted twice)
|A ∪ B ∪ C| = |A| + |B| + |C| − |A∩B| − |A∩C| − |B∩C| + |A∩B∩C|
```
**Example:** how many numbers in 1..100 are divisible by 2, 3 or 5?
```
by 2: 50    by 3: 33    by 5: 20
by 6: 16    by 10: 10   by 15: 6
by 30: 3
50 + 33 + 20 − 16 − 10 − 6 + 3 = 74
```
The code counts them directly too: 74.

---

## 2. Relations and functions

### Relations
A **relation** on a set is a collection of pairs (a, b) saying "a is related to b": `<`, `=`, "is a friend of", "is a prerequisite of".

| Property | Meaning | Example |
|---|---|---|
| reflexive | every a relates to itself | `=`, `≤` |
| symmetric | a~b means b~a | "is a sibling of" |
| antisymmetric | a~b and b~a means a = b | `≤` |
| transitive | a~b and b~c means a~c | `<`, "is an ancestor of" |

- An **equivalence relation** (reflexive + symmetric + transitive) splits a set into **groups** (equivalence classes). Example: "same remainder mod 3" splits the integers into {…0, 3, 6…}, {…1, 4, 7…}, {…2, 5, 8…}. **Union-find** maintains exactly this.
- A **partial order** (reflexive + antisymmetric + transitive) is "comes before". Example: course prerequisites. A **topological sort** lines up a partial order in a sequence.

### Kinds of functions
| Kind | Meaning | Picture |
|---|---|---|
| **injective** (one-to-one) | different inputs give different outputs | no two arrows hit the same target |
| **surjective** (onto) | every possible output is hit | every target gets an arrow |
| **bijective** | both | a perfect pairing; it has an inverse |

In the code, `f = [2, 0, 1]` (f(0)=2, f(1)=0, f(2)=1) is a bijection, a **permutation**. `g = [1, 1, 0]` is neither: g(0) = g(1), and 2 is never hit.

**CS connections:** a hash function from a huge key space into a small table can't be injective, so collisions are unavoidable. A perfect hash is an injection. Encryption must be a bijection, or you couldn't decrypt.

### The pigeonhole principle
> If you put **n + 1** pigeons into **n** holes, some hole has at least two pigeons.

It sounds obvious, but it proves surprising things:
- Any 13 people include two born in the same month.
- Hashing 1,000 keys into 999 buckets **must** create a collision (the code tries 100 random hashings: every one collides).
- Lossless compression can't shrink **every** file: there are fewer short files than long ones.
- In an array of n + 1 numbers from 1..n, some number repeats ("Find the Duplicate Number").

---

## 3. Counting (combinatorics)

Counting is the basis of probability (chapter 04) and of complexity analysis ("how many subsets/paths/arrangements are there?").

### The two basic rules
- **Rule of product:** if you choose one thing in a ways **and then** another in b ways, there are **a × b** ways. 3 shirts × 4 trousers = 12 outfits.
- **Rule of sum:** if you choose one thing in a ways **or** another in b ways (not both), there are **a + b** ways.

### Factorials and permutations (order matters)
- `n! = n × (n−1) × … × 1` = the ways to arrange n things in a row. 5! = 120. Factorials explode: **21! doesn't fit in a `long`** (the code uses `BigInteger`).
- Arranging **r of n** things in order: **P(n, r) = n! / (n − r)!**. Gold, silver and bronze among 5 runners: 5·4·3 = 60.

### Combinations (order doesn't matter)
Choosing **r of n** things as a group:
```
C(n, r) = n! / (r! · (n − r)!)          "n choose r"
```
- choose 2 of 5: C(5, 2) = 10;
- poker hands: C(52, 5) = **2,598,960**.

**Why divide by r!?** P(n, r) counts each group r! times, once per ordering of the same r items.

| | Order matters | Order doesn't matter |
|---|---|---|
| **no repetition** | P(n, r) = n!/(n−r)! | C(n, r) |
| **repetition allowed** | nʳ (PIN codes: 10⁴) | C(n + r − 1, r) ("stars and bars") |

### Pascal's triangle
```
            1
          1   1
        1   2   1
      1   3   3   1
    1   4   6   4   1
  1   5  10  10   5   1
```
Each number is the sum of the two above it: **C(n, k) = C(n−1, k−1) + C(n−1, k)**. Why: either the n-th item is in the group (then choose k−1 from the other n−1) or it isn't (choose k from n−1). This recurrence is a DP table (LeetCode "Pascal's Triangle", "Unique Paths"). Each row sums to **2ⁿ** (all subsets).

### The binomial theorem
```
(x + y)ⁿ = Σ C(n, k) · xᵏ · yⁿ⁻ᵏ          (x + y)⁴ = x⁴ + 4x³y + 6x²y² + 4xy³ + y⁴
```
The coefficients are row n of Pascal's triangle. It's used for probabilities of "k successes in n tries" (the binomial distribution, chapter 04).

### Stars and bars: splitting identical things
How many ways can 10 identical sweets go to 3 children? Line up 10 stars and 2 bars, `★★★|★★★★★|★★`, where the bars split the stars into 3 groups. Choose where the 2 bars go among 12 positions: **C(12, 2) = 66**. The code also counts the solutions of x + y + z = 10 directly: 66.

### Derangements: nobody gets their own hat
4 people check hats and get them back randomly. In how many arrangements does **nobody** get their own? The code tries all 4! = 24 permutations: **9**. (By inclusion–exclusion, the fraction tends to 1/e ≈ 36.8% as n grows.)

### Catalan numbers
1, 1, 2, 5, 14, **42**, 132, … count:
- binary search trees with n keys;
- valid strings of n pairs of brackets;
- ways to triangulate a polygon.

Recurrence: `Cat(n) = Σ Cat(i) · Cat(n−1−i)` (pick the root; i keys go left). Formula: `Cat(n) = C(2n, n)/(n+1)`. The code checks both give 42 for n = 5. LeetCode "Unique Binary Search Trees" is exactly this.

---

## 4. Recurrences

A **recurrence** defines a sequence using earlier values. Recursive algorithms produce them naturally: "the time for n = the time for smaller pieces + the work to combine".

| Recurrence | Algorithm | Solution |
|---|---|---|
| F(n) = F(n−1) + F(n−2) | Fibonacci | grows like φⁿ, φ ≈ 1.618 |
| T(n) = T(n/2) + 1 | binary search | **log₂ n** (T(1024) = 10) |
| T(n) = 2T(n/2) + n | merge sort | **n log₂ n** (T(1024) = 10,240) |
| T(n) = 2T(n−1) + 1 | Tower of Hanoi | **2ⁿ − 1** (10 discs: 1023 moves) |
| T(n) = T(n−1) + n | selection sort | n(n+1)/2 = O(n²) |

### Solving by unrolling (merge sort)
```
T(n) = 2T(n/2) + n
     = 2[2T(n/4) + n/2] + n = 4T(n/4) + 2n
     = 8T(n/8) + 3n
     = …  = 2ᵏ T(n/2ᵏ) + k·n
```
Stop when n/2ᵏ = 1, i.e. k = log₂ n: T(n) = n·T(1) + n log₂ n = **Θ(n log n)**.

### The recursion-tree picture
Merge sort: the top level does n work; the next level has 2 halves doing n/2 each, n in total; every level does n work, and there are log₂ n levels. Total n log n.

### The Master theorem: a shortcut
For **T(n) = a·T(n/b) + O(nᵈ)** (a subproblems of size n/b, plus nᵈ work to split and combine), compare `a` with `bᵈ`:

| Case | Condition | Result | Example |
|---|---|---|---|
| 1. leaves dominate | a > bᵈ | **Θ(n^(log_b a))** | T = 4T(n/2) + n → n² |
| 2. balanced | a = bᵈ | **Θ(nᵈ log n)** | merge sort: 2 = 2¹ → n log n |
| 3. root dominates | a < bᵈ | **Θ(nᵈ)** | T = 2T(n/2) + n² → n² |

The code computes T(n) = 4T(n/2) + n exactly and shows T(n)/n² settles at about 2, confirming Θ(n²).

### Fibonacci's closed form
Fibonacci grows like φⁿ: **Binet's formula** F(n) = round(φⁿ/√5), where φ = (1 + √5)/2 is the golden ratio. The code checks F(50) = 12,586,269,025 both ways. This is why naive recursive Fibonacci (which makes F(n) calls) is exponential, and why memoisation matters.

---

## 5. Graph theory

### Vocabulary
A **graph** G = (V, E) is a set of **vertices** (nodes) and **edges** (connections).

| Term | Meaning |
|---|---|
| directed / undirected | edges have a direction (Twitter follows) or not (Facebook friends) |
| weighted | edges carry a number (distance, cost) |
| degree | number of edges at a vertex (in-degree / out-degree when directed) |
| path | a sequence of vertices joined by edges |
| cycle | a path that returns to its start |
| connected | there's a path between every pair |
| tree | connected with **no cycles** |
| DAG | directed acyclic graph (dependencies, build systems) |
| bipartite | vertices split into two sides; edges only go across |
| complete graph Kₙ | every pair is connected: **n(n−1)/2** edges (K₁₀ has 45) |

### Facts worth knowing
- **Handshake lemma:** the sum of all degrees = **2|E|** (every edge has two ends). So the number of odd-degree vertices is always even.
- **A tree with n vertices has exactly n − 1 edges.** The code builds 100 random spanning trees (adding random edges and skipping any that would close a cycle) and every one ends with n − 1 edges. Fewer edges can't connect n vertices; one more creates a cycle.
- **Bipartite ⟺ no odd cycle.** Test it by 2-colouring with BFS: if an edge ever joins two same-coloured vertices, it's not bipartite. C₆ is bipartite; C₅ is not.
- **Euler path** (uses every **edge** once): exists in a connected graph iff 0 or 2 vertices have odd degree. Euler proved this for the Königsberg bridges in 1736: all 4 land masses had odd degree, so the famous walk is impossible. That proof started graph theory.
- **Hamiltonian path** (visits every **vertex** once): no simple test is known; it's NP-complete (chapter 08). The two definitions are almost the same, yet one is easy and one is hard.
- **Planar graphs** can be drawn without crossing edges; any planar map can be coloured with **4 colours** so that neighbours differ (the Four Colour Theorem).

### Representing graphs
| | Space | Edge check | List neighbours |
|---|---|---|---|
| adjacency matrix | O(V²) | O(1) | O(V) |
| adjacency list | O(V + E) | O(degree) | O(degree) |

Use lists for sparse graphs (most real graphs); matrices are for dense graphs and for linear algebra on graphs (chapter 09).

---

## 6. Number theory

### Divisibility, primes, GCD
- `a | b` means "a divides b" (b = a·k).
- A **prime** has exactly two divisors: 1 and itself. Every integer > 1 factors uniquely into primes (the Fundamental Theorem of Arithmetic). There are 25 primes below 100 and **78,498** up to 10⁶ (computed with the **Sieve of Eratosthenes**: cross out multiples of each prime, O(n log log n)).
- **GCD** (greatest common divisor) by **Euclid's algorithm**: `gcd(a, b) = gcd(b, a mod b)`, and gcd(a, 0) = a.
  ```
  gcd(48, 18) = gcd(18, 12) = gcd(12, 6) = gcd(6, 0) = 6
  ```
  Why it works: any number dividing a and b also divides a − kb = a mod b. It's O(log min(a, b)) steps.
- **LCM**: lcm(a, b) = a·b / gcd(a, b); lcm(4, 6) = 12.

### Modular arithmetic: clock maths
`a ≡ b (mod m)` means a and b have the same remainder mod m. Like a clock: 14:00 ≡ 2 o'clock (mod 12).

You can reduce **at every step** of addition, subtraction and multiplication:
```
(a + b) mod m = ((a mod m) + (b mod m)) mod m
(a · b) mod m = ((a mod m) · (b mod m)) mod m
```
That's why LeetCode says "return the answer mod 10⁹ + 7": you keep numbers small throughout and never overflow. (**Division doesn't work this way**; you need the modular inverse below.)

### Fast exponentiation (square and multiply)
Computing 2¹⁰⁰ mod p by multiplying 100 times is slow for huge exponents. Instead, square repeatedly and use the exponent's binary digits:
```
x¹³ = x⁸ · x⁴ · x¹          (13 = 1101 in binary)
```
This takes O(log exponent) multiplications. `modPow` in the code computes 2¹⁰⁰ mod (10⁹+7) = 976,371,285, matching `BigInteger.modPow`.

### Modular inverse
The inverse of a mod m is the number a⁻¹ with `a · a⁻¹ ≡ 1 (mod m)`. It exists iff gcd(a, m) = 1. Two ways to find it:
- **Fermat's little theorem:** for a prime p, `aᵖ⁻¹ ≡ 1`, so **a⁻¹ = aᵖ⁻² mod p**. The code checks 3 · 3⁻¹ ≡ 1 mod 10⁹+7.
- **Extended Euclid:** it finds x, y with **a·x + b·y = gcd(a, b)** (Bézout's identity). For 240 and 46: 240x + 46y = 2. If gcd(a, m) = 1, then x is a⁻¹ mod m.

Uses: computing C(n, k) mod p (division by k!), hashing, cryptography.

### RSA in 6 lines (the real algorithm, with tiny numbers)
1. Pick primes p = 61, q = 53. Then **n = p·q = 3233**.
2. φ(n) = (p−1)(q−1) = 3120 (how many numbers below n share no factor with it).
3. Public exponent **e = 17** (must have gcd(e, φ) = 1).
4. Private key **d = e⁻¹ mod φ(n) = 2753** (extended Euclid).
5. **Encrypt** the message 65: 65¹⁷ mod 3233 = **2790**.
6. **Decrypt**: 2790²⁷⁵³ mod 3233 = **65** ✓.

Anyone can encrypt with (n, e). Only someone who knows d can decrypt, and finding d requires factoring n, which is believed to be infeasible for 2048-bit numbers. The code runs all six steps.

---

## 7. Asymptotic notation (Big-O)

### Definitions
| Notation | Meaning | Reads as |
|---|---|---|
| f = **O(g)** | f grows **at most** like g (eventually f ≤ c·g) | upper bound |
| f = **Ω(g)** | f grows **at least** like g | lower bound |
| f = **Θ(g)** | both: f grows **exactly** like g | tight bound |

**Rules:** drop constants and lower-order terms. 3n² + 100n = Θ(n²): at n = 10⁶, (3n² + 100n)/n² = 3.0001, so the 100n hardly matters.

### Why it matters: growth table (from the code)
| n | log₂ n | n log₂ n | n² | 2ⁿ |
|---|---|---|---|---|
| 10 | 3 | 33 | 100 | 1,024 |
| 100 | 7 | 664 | 10,000 | ~10³⁰ |
| 1,000 | 10 | 9,966 | 1,000,000 | ~10³⁰¹ |

A computer does roughly 10⁸–10⁹ simple steps per second, so O(n log n) handles millions of items, O(n²) about tens of thousands, and O(2ⁿ) only about n = 30 (2³⁰ is already over a billion).

The full treatment is in [DSA 00 · Time and Space Complexity](../../DSA/00-Time-Space-Complexity/).

---

## 8. Exercises (answers below)

1. |A| = 20, |B| = 15, |A ∩ B| = 6. Find |A ∪ B|.
2. How many 8-character passwords use only lowercase letters? With no letter repeated?
3. A team of 3 is chosen from 10 people. How many teams? How many if one person must be captain?
4. Solve T(n) = 3T(n/3) + n with the Master theorem.
5. Compute gcd(252, 105) with Euclid's algorithm.
6. What is 7²²² mod 10? (Hint: look at the last digits of powers of 7.)
7. A graph has degrees 3, 3, 2, 2, 2. How many edges does it have?
8. How many subsets of {1, …, 10} contain the number 1?

<details>
<summary>Answers</summary>

1. 20 + 15 − 6 = 29.
2. 26⁸ ≈ 2.09 × 10¹¹. Without repeats: P(26, 8) = 26·25·…·19 ≈ 6.3 × 10¹⁰.
3. C(10, 3) = 120. With a captain: 120 × 3 = 360 (or 10 × C(9, 2) = 360).
4. a = 3, b = 3, d = 1: a = bᵈ, so Θ(n log n).
5. gcd(252, 105) = gcd(105, 42) = gcd(42, 21) = gcd(21, 0) = 21.
6. Powers of 7 end in 7, 9, 3, 1, repeating every 4. 222 mod 4 = 2, so the last digit is 9.
7. (3 + 3 + 2 + 2 + 2)/2 = 6 (handshake lemma).
8. 1 is in; each of the other 9 is in or out: 2⁹ = 512.
</details>

---

## 9. Interview questions

**Q1. How many subsets does a set of n elements have, and how do you enumerate them?**
> 2ⁿ. Loop a mask from 0 to 2ⁿ − 1; bit i of the mask says whether element i is in the subset. Or use backtracking (include / exclude each element).

**Q2. Why is the answer often required "mod 10⁹ + 7"?**
> The true counts are astronomically large. Taking mod at every addition and multiplication keeps numbers in 64-bit range, and 10⁹ + 7 is a prime, so modular inverses exist (needed for division, e.g. in C(n, k)).

**Q3. How do you prove a tree with n nodes has n − 1 edges?**
> Induction: a tree with n ≥ 2 nodes has a leaf; removing it and its edge leaves a tree with n − 1 nodes, which has n − 2 edges by the hypothesis; add the leaf back: n − 1 edges.

**Q4. How do you check if a graph is bipartite?**
> BFS/DFS 2-colouring; if an edge connects two vertices of the same colour, there's an odd cycle and it isn't bipartite. O(V + E).

**Q5. Explain the Master theorem in one sentence.**
> For T(n) = aT(n/b) + nᵈ, compare the number of subproblems a with the shrink factor bᵈ: the leaves dominate (n^log_b a), every level costs the same (nᵈ log n), or the root dominates (nᵈ).

**Q6. Why is RSA secure?**
> Encryption uses n = pq and e publicly; decryption needs d = e⁻¹ mod (p−1)(q−1), which requires knowing p and q. Factoring a 2048-bit n is believed to be computationally infeasible.

---

**Next:** [02 · Linear Algebra](../02-Linear-Algebra/README.md): vectors, matrices, eigenvalues, SVD: the language of ML.
