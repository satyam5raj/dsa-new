# 08 · Theory of Computation

| | |
|---|---|
| **Code** | [TheoryOfComputation.java](TheoryOfComputation.java): a DFA, an NFA, a parser, a Turing machine, SAT, subset sum and TSP, all built and tested (`java TheoryOfComputation.java`, 32 checks) |
| **You need** | sets, logic and graphs from [00](../00-Foundations/README.md) and [01](../01-Discrete-Math/README.md) |
| **Used in** | regular expressions, lexers and compilers, parsers (JSON, SQL, programming languages), protocol and UI state machines, input validation, understanding why some problems have no fast exact algorithm (scheduling, routing, NP-hardness in interviews), cryptography |

This chapter asks the deepest questions in CS: **what can a computer compute at all, and what can it compute quickly?** The answers are organised as a ladder of machines, each more powerful than the last:

```
finite automaton  <  pushdown automaton  <  Turing machine
(no memory)          (one stack)            (unlimited tape) = any computer
regular languages    context-free languages  everything computable
regex                grammars, parsers       programs
```

---

## 1. Finite automata (DFA)

### What a DFA is
A **deterministic finite automaton** reads a string one symbol at a time, moving between a **finite** number of states according to fixed rules. When the input ends, it **accepts** if it's in an accepting state. It has no memory other than which state it's in.

| Part | Meaning |
|---|---|
| states Q | a finite set, e.g. {0, 1, 2} |
| alphabet Σ | the symbols it reads, e.g. {0, 1} |
| transition δ(state, symbol) | the next state |
| start state | where it begins |
| accepting states | where it must end to say "yes" |

### Example: is a binary number divisible by 3?
Keep the **remainder mod 3** as the state. Reading bit b turns value v into 2v + b, so remainder r becomes **(2r + b) mod 3**:

| state (remainder) | read 0 | read 1 |
|---|---|---|
| **0** (start, accept) | 0 | 1 |
| 1 | 2 | 0 |
| 2 | 1 | 2 |

`110` (6): 0 → 1 → 0 → 0, accepted. `111` (7): 0 → 1 → 0 → 1, rejected. The code checks it against `v % 3` on **all 32,766** binary strings up to length 14.

**Three states can check divisibility of numbers with millions of digits**, because the machine only needs to remember the remainder, not the number.

### Where DFAs are used
- **Lexers** in compilers split source code into tokens (identifiers, numbers, keywords).
- **Protocol and UI state machines:** TCP connection states, vending machines, traffic lights. [LLD 11 Vending Machine](../../LLD/11-Vending-Machine/) and [LLD 12 ATM](../../LLD/12-ATM-System/) use the State pattern, which is a DFA in code.
- Input validation, network packet filters, string search (KMP is a DFA).

---

## 2. Nondeterminism (NFA)

An **NFA** may have **several possible moves** (or none) for a symbol, and accepts if **any** sequence of choices leads to acceptance. Think of it as "magically guessing" or exploring all branches in parallel.

**Example:** strings over {a, b} ending in "abb", i.e. `(a|b)*abb`. The NFA stays in state 0 reading anything, and at some point "guesses" that the final "abb" has started: 0 →a 1 →b 2 →b 3 (accept).

**Simulating it:** keep the **set of all states the NFA could be in**. That's exactly what the code does, and it agrees with Java's regex `(a|b)*abb` on all 8,191 strings up to length 12.

**Key theorem:** NFAs are **not more powerful** than DFAs. Any NFA with n states can be turned into a DFA (the **subset construction**: each DFA state = a set of NFA states), though the DFA can need up to 2ⁿ states. NFAs are just easier to write.

---

## 3. Regular expressions

A **regular expression** describes a set of strings with three operations:
| Operation | Syntax | Meaning |
|---|---|---|
| concatenation | `ab` | a then b |
| union (alternation) | `a\|b` | a or b |
| Kleene star | `a*` | zero or more a's |

Everything else is shorthand: `a+` = `aa*`, `a?` = `(a|ε)`, `[a-z]` = `a|b|…|z`.

**Kleene's theorem:** regular expressions, NFAs and DFAs describe **exactly the same languages**, the **regular languages**. A regex engine compiles your pattern into an automaton (Thompson's construction → NFA → optionally a DFA).

**Practical warning:** Java, Python and JavaScript regex engines use **backtracking**, so some patterns like `(a+)+b` take **exponential time** on inputs like `aaaa…a` (ReDoS attacks). DFA-based engines such as RE2 guarantee linear time. Also, "regexes" with back-references (`(a+)\1`) are more powerful than true regular expressions.

---

## 4. The limits of finite automata

### What DFAs can't do
A DFA has finitely many states, so it can only "remember" a bounded amount. Languages that need **unbounded counting** are not regular:
- `aⁿbⁿ` (n a's followed by exactly n b's);
- **balanced brackets** `( [ { } ] )`;
- palindromes.

**Intuition:** to check aⁿbⁿ you must count the a's. A machine with k states can only count up to k. The code builds a 5-state counter and shows it wrongly **accepts** `aaaaabbbb` (5 a's, 4 b's). **You can't parse HTML or JSON with a regex** for exactly this reason: nesting needs a stack.

**The pumping lemma** turns this intuition into a proof: any long enough string in a regular language has a middle piece that can be repeated ("pumped") and still be in the language. Pumping the a's in aⁿbⁿ breaks the balance, so the language isn't regular.

### Add a stack: pushdown automata
A **pushdown automaton (PDA)** is a finite automaton plus an unbounded **stack**. Now it can match nesting: push on `(`, pop on `)`, and check that the stack is empty at the end. That's exactly [LeetCode Valid Parentheses](../../DSA/04-Stack/02-Easy/E01_ValidParentheses.md). `([]{()})` is accepted and `([)]` is rejected.

---

## 5. Context-free grammars (CFGs)

### What a grammar is
A **grammar** generates a language with **rewriting rules**:
```
expr   → term (('+' | '−') term)*
term   → factor (('*' | '/') factor)*
factor → NUMBER | '(' expr ')'
```
Languages generated by such rules are **context-free**, exactly what PDAs recognise. Almost every programming language's syntax, plus JSON, SQL and arithmetic, is described by a CFG (written in BNF notation in language specifications).

### Parsing: recursive descent
Write **one method per rule**; the call stack plays the role of the PDA's stack. The code's `Parser`:
| Input | Result | Why |
|---|---|---|
| `2 + 3 * 4` | **14** | `*` lives deeper in the grammar (term), so it binds tighter |
| `(2 + 3) * 4` | **20** | parentheses restart at `expr` |
| `2 + 3 * (4 − 1) / 3` | **5** | |
| `10 − 4 − 3` | **3** | the `*` loop makes it left-associative |

**Operator precedence comes from the grammar's nesting**: no special rules needed. Compilers, query engines, config-file readers and calculators (like [Basic Calculator](../../DSA/04-Stack/04-Hard/H03_BasicCalculator.md)) work this way. A **parse tree** (the shape of the method calls) becomes the **abstract syntax tree (AST)** that a compiler or interpreter walks.

### The Chomsky hierarchy
| Type | Grammar | Machine | Example |
|---|---|---|---|
| 3 | regular | finite automaton | identifiers, numbers |
| 2 | context-free | pushdown automaton | nested brackets, programming-language syntax |
| 1 | context-sensitive | linear-bounded automaton | aⁿbⁿcⁿ |
| 0 | unrestricted | **Turing machine** | anything computable |

---

## 6. Turing machines: the definition of "computable"

### The machine
Alan Turing's 1936 model of computation:
- an infinite **tape** of cells holding symbols;
- a **head** that reads and writes one cell and moves left or right;
- a finite set of **states**, with rules: (state, symbol read) → (symbol to write, direction, next state).

That's all, yet it can compute **anything any computer can**.

### Example: adding 1 to a binary number
1. Move **right** to the end of the number.
2. Move **left**, turning 1s into 0s (carrying), until you hit a 0 or a blank; turn it into 1 and stop.

`1011` (11) becomes **`1100`** (12) in 8 steps; `111` becomes **`1000`** (the tape grows). The code checks every number from 0 to 1999.

### The Church–Turing thesis
Everything we'd call "computable by an algorithm" can be computed by a Turing machine. Java, Python, your laptop, a GPU and the cloud are all equivalent in **what** they can compute (given enough memory and time); they only differ in **speed**. A language that can simulate a Turing machine is **Turing-complete**: almost every programming language is, and so are, surprisingly, Excel formulas, SQL with recursive CTEs, and Conway's Game of Life.

---

## 7. The halting problem: what no computer can do

### The question
Is there a program `halts(P, x)` that always answers correctly whether program P stops on input x?

### Turing's answer: no (proof by contradiction)
Suppose `halts` exists. Build:
```
paradox(P):  if halts(P, P): loop forever
             else:           stop
```
Now ask: does `paradox(paradox)` halt?
- If it halts, then `halts(paradox, paradox)` was true, so it loops forever. Contradiction.
- If it loops, then `halts` said false, so it stops. Contradiction.

So `halts` can't exist. ∎ (It's the same self-reference trick as "this sentence is false".)

### A three-line program nobody can analyse
```
while n != 1:  n = n/2 if n is even else 3n + 1
```
The **Collatz conjecture** says this halts for every n ≥ 1. Nobody has proved it. The code checks every n below 1,000,000 (they all halt; the longest run starts at **837,799** and takes **524** steps; n = 27 takes 111). But checking examples isn't a proof.

### Consequences (Rice's theorem)
**Every non-trivial question about what a program does is undecidable** in general: "does it ever crash?", "are these two programs equivalent?", "is this code malware?". That's why compilers' warnings and static analysers are approximations (they report false alarms or miss bugs), and why perfect antivirus is impossible.

---

## 8. Complexity: P vs NP

Computability asks "*can* it be solved?". Complexity asks "can it be solved **fast**?" (in polynomial time, like n² or n³, rather than 2ⁿ).

### The classes
| Class | Meaning | Examples |
|---|---|---|
| **P** | solvable in polynomial time | sorting, shortest paths, MST, matching, linear programming, primality testing |
| **NP** | a proposed answer (a "certificate") can be **checked** in polynomial time | SAT, Sudoku, TSP (decision version), graph colouring, subset sum |
| **NP-complete** | the hardest problems in NP: if one has a fast algorithm, they all do | SAT, 3-SAT, TSP, vertex cover, clique, knapsack, Hamiltonian path |
| **NP-hard** | at least as hard as NP-complete (may not be in NP itself) | optimisation TSP, the halting problem |

### Checking vs finding: SAT
**Satisfiability:** given a Boolean formula like `(x₁ ∨ x₂) ∧ (¬x₁ ∨ x₃) ∧ (¬x₂ ∨ ¬x₃) ∧ (x₂ ∨ x₃)`, is there an assignment that makes it true?
- **Checking** a proposed assignment: evaluate each clause, which is fast (linear).
- **Finding** one: brute force tries up to **2ⁿ** assignments. For a random 3-SAT formula with 20 variables and 85 clauses, the code tried **575,537** of the 1,048,576 assignments before finding one. Checking an answer takes 85 clause tests.

**P vs NP asks: if an answer is easy to check, is it also easy to find?** Almost everyone believes **P ≠ NP**, but nobody has proved it. It's one of the seven Millennium Prize problems ($1,000,000). Modern cryptography depends on some problems being hard to solve but easy to check.

### Reductions: how hardness spreads
To show a new problem B is NP-hard, **reduce** a known NP-hard problem A to it: show any instance of A can be quickly rewritten as an instance of B. Then a fast algorithm for B would solve A. (Cook and Levin proved SAT is NP-complete in 1971; thousands of problems have since been reduced from it.)

---

## 9. Living with hard problems

When a problem is NP-hard, you don't give up. You pick a strategy:

| Strategy | Idea | Example from the code / course |
|---|---|---|
| **exact, exponential, but smarter** | DP over subsets beats brute force | TSP on 9 cities: brute force checks **8! = 40,320** routes; **Held–Karp** DP (O(2ⁿn²)) gives the same optimum, **270.7181** |
| **pseudo-polynomial** | polynomial in the *value*, not the input size | subset sum with a DP table over sums: is there a subset of {3, 34, 4, 12, 5, 2} summing to 9? **yes** (2 subsets: 4+5 and 3+4+2) |
| **heuristics** | fast, usually good, no guarantee | nearest-neighbour TSP: **310.26**, 14.6% worse than optimal |
| **approximation algorithms** | provably within a factor of optimal | the MST-based TSP 2-approximation |
| **special cases** | the general problem is hard, the instance isn't | 2-SAT is in P; trees, bipartite graphs, small parameters |
| **SAT/ILP solvers** | heavily engineered search that's often fast in practice | chip verification, scheduling, package dependency resolution |

**In interviews:** if a problem smells NP-hard (all subsets, all orders, all colourings), expect small constraints (n ≤ 20 means bitmask DP; n ≤ 10 means permutations), or look for special structure. See [Backtracking](../../DSA/14-Backtracking/), [DP](../../DSA/15-Dynamic-Programming/) and [Advance A09](../../DSA/12-Advanced/Advance/A09_ShortestPathVisitingAllNodes.md) (Held–Karp).

### Beyond P and NP
- **PSPACE:** solvable with polynomial memory (generalised chess and Go are PSPACE-hard).
- **EXPTIME:** needs exponential time, provably.
- **BPP:** fast with randomness (Miller–Rabin primality testing).
- **BQP:** fast on a quantum computer (Shor's algorithm factors integers, which would break RSA).

---

## 10. Exercises (answers below)

1. Design a DFA (draw a state table) for binary strings with an even number of 1s.
2. Write a regular expression for strings over {a, b} that contain "aa" somewhere.
3. Why can't a regular expression check balanced parentheses?
4. Write a grammar for balanced parentheses.
5. Which are in P, and which are NP-complete? (a) shortest path (b) longest simple path (c) 2-colouring a graph (d) 3-colouring a graph.
6. Explain in two sentences why the halting problem is undecidable.
7. An interview problem asks for the best ordering of n ≤ 15 tasks. Which technique does the constraint suggest?

<details>
<summary>Answers</summary>

1. Two states, EVEN (start, accept) and ODD: reading 1 swaps them; reading 0 stays put.
2. `(a|b)*aa(a|b)*`.
3. Balancing needs counting to arbitrary depth, but a regular expression is equivalent to a finite automaton, which can only remember a bounded amount (pumping lemma).
4. S → (S)S | ε.
5. (a) P (Dijkstra). (b) NP-hard (it contains Hamiltonian path). (c) P (bipartite check with BFS). (d) NP-complete.
6. If a halting-decider existed, you could build a program that does the opposite of whatever the decider predicts about itself, which is a contradiction. So no general decider can exist.
7. Bitmask DP over subsets, O(2ⁿ · n²), like Held–Karp; 2¹⁵ · 15² ≈ 7 million operations.
</details>

---

## 11. Interview questions

**Q1. What's the difference between a DFA and an NFA?**
> A DFA has exactly one move per (state, symbol); an NFA can have several and accepts if any path accepts. They recognise the same languages (subset construction), but an NFA can be exponentially smaller.

**Q2. Why can't you parse HTML or JSON with regular expressions?**
> They have arbitrarily deep nesting, which needs a stack (a context-free grammar / pushdown automaton); regular expressions only have finite memory.

**Q3. What does NP-complete mean?**
> In NP (solutions are checkable in polynomial time), and every NP problem reduces to it, so a polynomial algorithm for it would give one for all of NP.

**Q4. How do you handle an NP-hard problem in practice?**
> Exploit small input sizes (bitmask DP, branch and bound), special structure, approximation algorithms, heuristics or local search, or a SAT/ILP solver.

**Q5. What is the halting problem, and why does it matter?**
> No algorithm can decide for every program and input whether it halts. It implies (Rice's theorem) that any non-trivial property of program behaviour is undecidable, so static analysis is always approximate.

**Q6. What does Turing-complete mean?**
> The system can simulate a Turing machine, so it can express any computation, given enough time and memory.

---

**Next:** [09 · Advanced Topics](../09-Advanced-Topics/README.md): numerical stability, spectral graphs, RL, Fourier, high dimensions, kernels and attention.
