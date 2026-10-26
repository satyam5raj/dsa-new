# Maths for Computer Science, ML and AI

A from-scratch course in the maths behind computer science, machine learning and AI, written so that **anyone with school arithmetic can follow it**. It goes from logs and sums up to backpropagation, KL divergence, Q-learning and the maths of attention.

Every chapter has two files:
- **`README.md`**: the explanation, written for complete beginners. Intuition and pictures in words come first, then the formula, worked examples, where it's used in CS/ML, exercises with answers, and interview questions.
- **One runnable `.java` file**: it computes **every worked example in the README** and checks it. Run it with `java File.java`. If a number appears in the notes, the code printed it. Formulas are cross-checked against simulations, brute force or an independent method.

Run everything with `./run-all.sh` (10 files, 459 checks).

---

## The chapters

| # | Chapter | What you'll learn | Checks |
|---|---|---|---|
| 00 | [Foundations](00-Foundations/README.md) | functions, exponents and **logs** (why binary search is log n), Σ notation and the key sums, floor/mod (and Java's `%` trap), logic, **proofs** (induction, contradiction, loop invariants) | 51 |
| 01 | [Discrete Mathematics](01-Discrete-Math/README.md) | sets and power sets, inclusion–exclusion, functions and pigeonhole, **counting** (permutations, combinations, Pascal, Catalan), **recurrences** and the Master theorem, **graph theory**, **number theory** (GCD, mod arithmetic, fast power, inverses, RSA), Big-O | 56 |
| 02 | [Linear Algebra](02-Linear-Algebra/README.md) | vectors, dot product and cosine similarity, matrices as transformations, Gaussian elimination, rank, determinants, inverses, **eigenvalues**, orthogonality, **SVD** and low-rank approximation, positive definiteness, least squares, **PCA**, tensors | 64 |
| 03 | [Calculus](03-Calculus/README.md) | limits, derivatives and their rules, the **chain rule**, optimisation, integrals, **gradients**, Jacobians and Hessians, **backpropagation by hand**, Taylor series, Newton's method, the derivatives used in ML | 56 |
| 04 | [Probability](04-Probability/README.md) | conditional probability, **Bayes' theorem** (the medical-test paradox, naive Bayes), birthday paradox, Monty Hall, expectation and variance, **distributions**, covariance, **LLN and CLT**, Markov chains and **PageRank**, randomised algorithms | 45 |
| 05 | [Statistics](05-Statistics/README.md) | descriptive statistics, estimators, **MLE** (where loss functions come from), Bayesian/MAP (= regularisation), confidence intervals, hypothesis and **A/B tests**, regression, the **bias–variance trade-off**, cross-validation, precision/recall/F1 | 46 |
| 06 | [Optimisation](06-Optimization/README.md) | convexity, **gradient descent** and the learning rate, SGD vs mini-batch, **momentum, RMSProp, Adam**, Newton's method, Lagrange multipliers and KKT, linear programming, **L1 vs L2** regularisation, saddle points | 27 |
| 07 | [Information Theory](07-Information-Theory/README.md) | information and **entropy**, Huffman coding and compression limits, **cross-entropy** (the classification loss), **KL divergence**, mutual information, decision-tree information gain, **perplexity**, softmax temperature | 39 |
| 08 | [Theory of Computation](08-Theory-of-Computation/README.md) | DFAs, NFAs and regular expressions, why regex can't parse nesting, context-free grammars and parsers, **Turing machines**, the **halting problem**, **P vs NP**, NP-completeness, strategies for hard problems | 32 |
| 09 | [Advanced Topics](09-Advanced-Topics/README.md) | **numerical stability** (log-sum-exp, Kahan), spectral graph theory, **MDPs and reinforcement learning** (value iteration, Q-learning), Fourier transforms, convolution, high-dimensional geometry, kernel methods, **the maths of attention** | 43 |

---

## How to study

### Suggested order
| Phase | Chapters | Why |
|---|---|---|
| 1 | 00 → 01 | supports DSA and interviews immediately (logs, sums, counting, recurrences, graphs, mod arithmetic) |
| 2 | 02 | everything in ML is vectors and matrices |
| 3 | 03 | how models learn: gradients and backprop |
| 4 | 04 → 05 | uncertainty, loss functions, evaluating models |
| 5 | 06 → 07 | training (optimisers) and what the losses mean |
| 6 | 08 → 09 | CS theory, and the topics behind modern AI |

**Short on time?** Read **02 (sections 1–2, 7, 9, 11–12), 03 (sections 2, 5, 6), 04 (sections 3, 5, 6), 05 (sections 3, 8, 9), 06 (sections 3–6)**. That's the core of ML maths.

### For each chapter
1. Read the README once for the ideas; don't stop at a formula you don't fully get yet.
2. **Run the Java file** and match each printed number to the README.
3. Change a number in the code (a learning rate, a probability, a matrix entry) and predict the new output before running it.
4. Do the exercises without looking, then check the answers.
5. Answer the interview questions out loud.

---

## Which maths powers which ML idea

| ML / AI concept | Maths behind it | Chapter |
|---|---|---|
| a neuron, a layer | dot products, matrix × vector | 02 |
| embeddings, semantic search, RAG | vectors, cosine similarity, high-dimensional geometry | 02, 09 |
| training | gradients, gradient descent, Adam | 03, 06 |
| backpropagation | the chain rule on a computation graph | 03 |
| MSE / cross-entropy losses | maximum likelihood | 05, 07 |
| softmax, LLM sampling, temperature | exponentials, probability, entropy | 03, 07 |
| regularisation, weight decay | priors (MAP), L1/L2 norms | 05, 06 |
| overfitting, validation | the bias–variance trade-off, cross-validation | 05 |
| PCA, compression, LoRA | eigenvectors, SVD, low rank | 02 |
| evaluating classifiers | precision, recall, F1, the accuracy trap | 05 |
| A/B testing | hypothesis tests, p-values, the CLT | 04, 05 |
| naive Bayes, spam filters | Bayes' theorem | 04 |
| language-model evaluation | cross-entropy, perplexity | 07 |
| VAEs, distillation, RLHF | KL divergence | 07, 09 |
| decision trees | entropy, information gain | 07 |
| CNNs | convolution, Fourier | 09 |
| GNNs, PageRank | Laplacians, eigenvectors, Markov chains | 04, 09 |
| reinforcement learning | MDPs, Bellman equation, Q-learning | 09 |
| transformers / attention | dot products, softmax, the √d variance argument | 02, 07, 09 |
| numerically stable code | floating point, log-sum-exp | 00, 09 |

## Which maths powers which CS idea

| CS concept | Maths | Chapter |
|---|---|---|
| Big-O, analysing loops | sums, logs, asymptotics | 00, 01 |
| recursion, divide and conquer | induction, recurrences, the Master theorem | 00, 01 |
| correctness proofs | loop invariants, induction | 00 |
| subsets, bitmask DP | power sets, binary | 01 |
| hashing, collisions | mod arithmetic, pigeonhole, the birthday paradox | 01, 04 |
| graph algorithms | graph theory, trees, bipartiteness | 01 |
| cryptography (RSA) | primes, modular inverses, fast exponentiation | 01 |
| randomised algorithms | expectation, probability | 04 |
| compression | entropy, Huffman coding | 07 |
| regex, lexers, parsers | automata, grammars | 08 |
| "this problem is NP-hard" | complexity classes, reductions | 08 |

---

## Recommended companions
- **3Blue1Brown** (YouTube): *Essence of Linear Algebra*, *Essence of Calculus*, and the neural-network series, for visual intuition.
- ***Mathematics for Machine Learning*** (Deisenroth, Faisal, Ong): a free PDF; a good next step after chapters 02–06.
- **MIT 18.06** (Gilbert Strang): linear algebra lectures.
- **MIT 6.042J**, *Mathematics for Computer Science*: discrete maths, with a free textbook.
- **StatQuest** (YouTube): statistics and ML concepts explained simply.
- ***Introduction to the Theory of Computation*** (Sipser): for chapter 08 in depth.
