# 02 · Maths for ML

> You don't need to be a mathematician. You need about **seven ideas** well enough to read a
> formula, code it in NumPy, and explain why a model behaves the way it does.

Run it: `../.venv/bin/python math_for_ml.py` → **43/43 checks**.
For the full, slower treatment see this repo's [Java-dsa/Maths](../../Java-dsa/Maths/README.md) course.

| Idea | Where ML uses it |
|---|---|
| Vectors, dot product, cosine | every model is `w · x`; embeddings and search |
| Matrices | a whole layer of a neural network is `X @ W` |
| SVD / PCA | compression, denoising, visualisation, LoRA |
| Derivatives, chain rule | gradient descent, backpropagation |
| Probability, Bayes, MLE | naive Bayes, logistic regression, why losses look the way they do |
| Statistics | is model B *really* better than model A? |
| Entropy, cross-entropy, KL | classification loss, decision-tree splits, distillation, VAEs, RLHF |

---

## 1. Vectors

A vector is a list of numbers: a point, or an arrow from the origin. One row of your dataset
(age, income, tickets) is a vector.

| Operation | Formula | Example from the run |
|---|---|---|
| Dot product | `a·b = Σ aᵢbᵢ` | `(3,4)·(4,3) = 24` |
| L2 norm (length) | `‖a‖₂ = √(Σ aᵢ²)` | `‖(3,4)‖ = 5` |
| L1 norm | `‖a‖₁ = Σ |aᵢ|` | `7` |
| Cosine similarity | `a·b / (‖a‖‖b‖)` | `24/25 = 0.96` |

**Cosine** measures the *angle* only:

- `1` = same direction;
- `0` = perpendicular (unrelated);
- `−1` = opposite.

`(1,1)` and `(5,5)` have cosine **1**: length is ignored. That's why embedding search uses
cosine: a long document and a short one about the same topic should match.

**Projection** of `a` onto `u`: `(a·u / u·u) u`. The "shadow" of `(3,4)` on the x-axis is
`(3,0)`, and what's left over, `(0,4)`, is perpendicular to the axis. Linear regression is exactly
this: project `y` onto the space spanned by your features.

**L1 vs L2 as penalties:** ridge regression penalises `‖w‖₂²` (shrinks all weights a bit);
lasso penalises `‖w‖₁` (pushes some weights to exactly 0). See chapter 03.

## 2. Matrices = transformations

A matrix **moves** vectors. `[[0,−1],[1,0]]` rotates `(1,0)` to `(0,1)`. A neural-network layer
`X @ W` transforms every input vector at once.

- **Solve `Ax = y`** with `np.linalg.solve(A, y)` → `[1, 2]`. Don't compute `inv(A) @ y`: it's
  slower and less accurate.
- **Determinant** `det([[2,1],[1,3]]) = 5`: how much the matrix scales area. `0` means it squashes
  space flat and can't be inverted.
- **Rank:** the number of truly independent directions. `[[1,2],[2,4]]` has rank **1** (row 2 =
  2 × row 1). In data, rank deficiency means **redundant features** (multicollinearity).
- **Eigenvectors:** special directions the matrix only *stretches*, never turns:
  `Av = λv`. For `[[2,1],[1,3]]` the largest eigenvalue is `(5+√5)/2 ≈ 3.618`.

## 3. SVD and PCA: finding the real dimensions

Any matrix factors as `X = U S Vᵀ` (**singular value decomposition**):

- the rows of `Vᵀ` are **directions** in feature space (the principal components);
- `S` holds **how much** the data spreads along each one;
- `U` gives **where** each sample sits along them.

The script builds 3-D data that is secretly 1-D (feature 2 ≈ 2 × feature 1, feature 3 is tiny noise):

```
explained variance ratio: [0.9977, 0.002, 0.0003]
```

One component holds **99.8 %** of the variance, and it points along `(1,2,0)/√5`
(x-component **0.4465** ≈ 1/√5 = 0.447). Keeping only that component turns 3 numbers into 1 with a
relative error of **0.048**, which is exactly `√(1 − 0.9977)`: **the error is the square root of
the variance you threw away.**

**PCA recipe:**

1. Centre the data (subtract the column means). Scale too, if the units differ.
2. SVD, or eigen-decompose the covariance matrix. The script checks the two agree:
   `eigenvalues = S²/(n−1)`.
3. Keep the top `k` components covering, say, 95 % of the variance.

ML uses: compression, removing correlated features, 2-D plots of high-dimensional data, and the
idea behind **LoRA** (weight updates are approximately low-rank, chapter 12).

## 4. Calculus: which way is downhill?

The **derivative** `f′(x)` is the slope. For `x³` at 2 it is `3·2² = 12`. The **gradient** `∇f`
is the vector of slopes, one per parameter. It points uphill, so gradient descent steps the other way:

```
w ← w − η ∇L(w)            η = learning rate
```

### Gradient check (do this whenever you hand-write a gradient)

Compare your formula against the **central difference** `(f(x+h) − f(x−h)) / 2h`.

For `L = (w₀−3)² + 10(w₁+1)²`, the analytic gradient at `(0,0)` is `(−6, 20)`, and the numerical
one agrees. Chapter 08 uses the same trick to verify backpropagation.

### Chain rule: the heart of backprop

If `y = f(g(x))` then `dy/dx = f′(g(x)) · g′(x)`. For `σ(3x+1)`:

```
dy/dx = σ(z)(1−σ(z)) · 3,    z = 3x + 1
```

At `x = 0.5` that's **0.2103**, matching the numerical derivative. A neural network is a long
chain of such functions; backprop applies the chain rule from the loss backwards.

### Learning rate

On this bowl-shaped loss, gradient descent with `η = 0.04` reaches `(3, −1)` in 200 steps.
With `η = 0.11` the steep direction (curvature 20) **explodes**: each step overshoots further,
because `η > 2/20`.

> Rule: the stable learning rate is limited by the **steepest** direction. That's why badly scaled
> features (one in metres, one in millimetres) make gradient descent slow or unstable, and why we
> standardise.

## 5. Probability

### Bayes' rule

```
P(A | B) = P(B | A) · P(A) / P(B)
```

**Classic trap:** a disease affects 1 % of people. The test catches 99 % of cases and gives false
positives 5 % of the time. You test positive. What is the chance you're sick?

```
P(+) = 0.99·0.01 + 0.05·0.99 = 0.0594
P(sick | +) = 0.0099 / 0.0594 ≈ 16.7 %
```

Only **16.7 %**, because healthy people vastly outnumber sick ones. Exactly the same thing happens
in **fraud detection**: with 0.1 % fraud, even a good model's alerts are mostly false alarms.
That's the base-rate effect, and why precision matters (chapter 04).

### Maximum likelihood estimation (MLE)

"Which parameter value makes the data I saw most probable?"

- Coin: 1,000 flips with true bias 0.3. The bias that maximises the likelihood (found by grid
  search in the script) is the **fraction of heads**, 0.303.
- Normal distribution: the MLE of the mean is the **sample mean** (169.93 for true 170).

**Why this matters:** minimising **MSE** = MLE under Gaussian noise; minimising **log loss** =
MLE for a Bernoulli outcome. Loss functions aren't arbitrary; they're likelihoods.

### The normal distribution

About 68 % of values lie within 1 standard deviation (the run gave 0.687) and 95 % within 1.96 std
(0.952). The **law of large numbers**: the mean of 100,000 dice rolls is 3.4996 → 3.5.
The **central limit theorem**: averages of almost anything are approximately normal. That's why
confidence intervals work.

## 6. Statistics: is the improvement real?

Model A scores ~0.70, model B ~0.73, each on 200 noisy evaluation runs.

| Method | Result | Meaning |
|---|---|---|
| **Bootstrap** (resample with replacement 2,000×) | 95 % CI for B − A = `[0.0073, 0.0465]` | the interval excludes 0 → B is better |
| **t-test** | `t = 2.596`, `p = 0.0098` | if there were no difference, a gap this big would happen < 1 % of the time |

**Two warnings, both from the real run:**

1. Two samples from the **same** distribution gave `p = 0.035`, which is "significant" by
   accident. At `p < 0.05`, a false positive happens 1 time in 20.
2. 100 t-tests on pure noise gave **8** "significant" results. Try 100 hyperparameter configs or
   100 features and some will look great by luck. Fixes: a held-out test set touched once,
   **Bonferroni** correction (`α / number of tests`), or replicating the result.

A **p-value** is *not* "the probability the null is true". It's the probability of data at least
this extreme **if** the null were true.

## 7. Information theory

**Entropy** = average surprise = how unpredictable something is, in bits:

```
H(p) = −Σ pᵢ log₂ pᵢ
```

| Distribution | Entropy |
|---|---|
| fair coin | **1 bit** |
| fair 8-sided die | **3 bits** |
| 90/10 coin | **0.469 bits** (more predictable) |

Decision trees choose the split that reduces entropy (or Gini impurity) the most: **information gain**.

**Cross-entropy** `H(p, q) = −Σ pᵢ ln qᵢ` is the classification loss. With true class 1:

| Prediction for class 1 | Loss |
|---|---|
| 0.90 (confident, right) | `−ln 0.9 = 0.105` |
| 0.05 (confident, **wrong**) | `−ln 0.05 = 2.996` |

Confidently wrong predictions are punished hard. That's why the loss pushes models to be calibrated.

**KL divergence** `KL(p‖q) = Σ pᵢ ln(pᵢ/qᵢ)` measures how far `q` is from `p`:

- it is ≥ 0, and `KL(p‖p) = 0`;
- it is **not symmetric**;
- **cross-entropy = entropy + KL**. Since the entropy of the labels is fixed, minimising
  cross-entropy minimises KL.

It's used in knowledge distillation, VAEs, and the RLHF/DPO penalty that keeps a fine-tuned model
close to the original.

---

## Exercises

1. Cosine similarity of `(1, 2, 3)` and `(−1, −2, −3)`?
2. Gradient-descent learning rate for `L = 50 w²`: what is the largest stable `η`?
3. Fraud is 0.1 % of transactions. A model flags 90 % of fraud with a 1 % false-positive rate.
   What fraction of alerts are real fraud?
4. Why would you standardise features before PCA?

<details><summary>Answers</summary>

1. `−1`: same line, opposite direction.
2. The gradient is `100w`, so the update is `w(1 − 100η)`. It's stable when `|1 − 100η| < 1`,
   i.e. `η < 0.02` (= 2 / curvature).
3. `0.9·0.001 / (0.9·0.001 + 0.01·0.999) = 0.0009 / 0.01089 ≈ 8.3 %`. Over 90 % of alerts are
   false alarms.
4. PCA finds the directions of largest **variance**. A feature measured in millimetres has a
   million times the variance of the same thing in kilometres, and would dominate PC1 just
   because of its units.
</details>

## Interview questions

**Q: Why does MSE correspond to Gaussian noise?** The Gaussian log-likelihood is
`−(y − ŷ)²/2σ² + const`. Maximising it means minimising the squared error.

**Q: What does an eigenvalue of 0 in the covariance matrix tell you?** A direction with no
variance: one feature is an exact linear combination of others (perfect multicollinearity).
`XᵀX` is then singular; ridge regularisation fixes this.

**Q: Explain PCA in one minute.** Centre the data, find the orthogonal directions of maximum
variance (eigenvectors of the covariance, or the right singular vectors from SVD), and project onto
the top k. You keep most of the information in fewer dimensions. It's linear and unsupervised, so
it can throw away a low-variance direction that happens to predict the label.

**Q: Cross-entropy vs KL?** `H(p,q) = H(p) + KL(p‖q)`. With fixed labels `p`, they have the same
gradient with respect to the model.

---

**Next:** [03 · Regression](../03-Regression/README.md)
