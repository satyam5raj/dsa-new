# 16 · Classical algorithms from scratch

> Chapters 03–06 *used* several algorithms through scikit-learn. This chapter opens the box:
> 13 algorithms written by hand in NumPy, each checked against the library version, with the
> idea, the maths and the key lines of code explained.

Run it: `../.venv/bin/python classical_from_scratch.py` → **35/35 checks** (~5 s).

| # | Algorithm | Checked against | Where it's used |
|---|---|---|---|
| 1 | Perceptron | convergence / XOR failure | history; the building block of neural nets |
| 2 | Linear SVM (Pegasos) | `LinearSVC` (same accuracy) | text classification, high-dim sparse data |
| 3 | Kernel trick + kernel perceptron | `SVC(kernel="rbf")` | non-linear boundaries without features |
| 4 | LDA | `LinearDiscriminantAnalysis` (identical predictions) | classification, supervised dim. reduction |
| 5 | Multinomial naive Bayes | `MultinomialNB` (identical log-probabilities) | spam, document classification |
| 6 | CART regression tree | `DecisionTreeRegressor` (identical predictions) | the base of every tree ensemble |
| 7 | XGBoost-style boosting | `HistGradientBoostingClassifier` (AUC 0.970 vs 0.971) | tabular-data winner |
| 8 | DBSCAN | `DBSCAN` (identical clusters + noise) | arbitrary-shape clustering |
| 9 | Gaussian mixture via EM | `GaussianMixture` (same log-likelihood) | soft clustering, density estimation |
| 10 | Agglomerative clustering | `AgglomerativeClustering` (identical) | dendrograms, small data |
| 11 | Isolation Forest | AUC on planted anomalies | anomaly detection |
| 12 | Apriori | brute-force enumeration (identical) | market-basket analysis |
| 13 | t-SNE | PCA (trustworthiness, 5-NN accuracy) | visualising embeddings |

---

## 1. Perceptron (1958)

**Idea:** a linear classifier `sign(w·x + b)` that learns **only from its mistakes**. When it gets
an example wrong, it nudges the boundary towards getting it right.

```python
if yi * (xi @ w + b) <= 0:      # misclassified (labels are −1 / +1)
    w += yi * xi                # move w towards positives, away from negatives
    b += yi
```

**Why it works:** after a mistake on a positive example, `w·x` increases by `‖x‖²`. The
**perceptron convergence theorem** says: if the data is linearly separable with margin γ and
`‖x‖ ≤ R`, the perceptron makes at most `(R/γ)²` mistakes in total.

| Data | Result |
|---|---|
| two separated blobs | mistakes per epoch `[2, 0]`: converged, 100 % training accuracy |
| XOR | never converges: no line separates XOR (Minsky & Papert's famous 1969 critique) |

The fix for XOR is a **hidden layer** (chapter 08) or a **kernel** (§3). The perceptron is the
ancestor of both neural networks and SVMs.

## 2. Linear SVM with Pegasos

**Idea:** of all separating lines, pick the one with the **widest margin**. Points far from the
boundary don't matter; only the ones near it (the **support vectors**) do.

**Objective (soft-margin SVM):**

```
minimise   λ/2 ‖w‖²  +  (1/n) Σ max(0, 1 − yᵢ (w·xᵢ + b))
           ─────────     ──────────────────────────────────
           wide margin    hinge loss: zero for points beyond the margin
```

- `‖w‖` small ⇔ margin `2/‖w‖` wide.
- **Hinge loss:** 0 when `y·f(x) ≥ 1` (correct and confident), and linear otherwise. Compare log
  loss (chapter 04), which never reaches exactly 0, so every point always matters a little.
- `λ` ↔ sklearn's `C = 1/(λn)`. Big C (small λ) means fewer training errors and a narrower margin.

**Pegasos** (Shalev-Shwartz et al., 2007) = stochastic sub-gradient descent with step `1/(λt)`:

```python
if y[i] * (X1[i] @ w) < 1:                 # hinge active
    w = (1 - eta*lam) * w + eta * y[i] * X1[i]
else:
    w = (1 - eta*lam) * w                  # only shrink
```

The bias is handled by appending a constant feature of 1.

| | Test accuracy |
|---|---|
| Pegasos (scratch) | **0.953** |
| sklearn `LinearSVC` | **0.953** |

Only **11.1 %** of training points have margin ≤ 1. Those are the support vectors; deleting all
the other points wouldn't change the solution.

> While building this, my first version updated the bias with an ad-hoc separate step and scored
> 0.920. Folding the bias into `w` as a constant feature made it match exactly. Small details of an
> optimiser matter.

## 3. The kernel trick and the kernel perceptron

**Problem:** concentric circles. No straight line works: the linear perceptron scores **0.550**.

**Feature-map idea:** map `x → φ(x)` into a space where the classes *are* linearly separable (for
circles, adding `x₁² + x₂²` is enough). But for rich maps φ is huge or even infinite-dimensional.

**Kernel trick:** many algorithms only ever use **dot products** `φ(x)·φ(z)`. A **kernel**
`K(x, z)` computes that dot product directly, without building φ.

```
(x·z + 1)²  ==  φ(x)·φ(z)    with φ(v) = [1, √2v₁, √2v₂, v₁², √2v₁v₂, v₂²]     ← checked numerically
RBF:  K(x, z) = exp(−γ ‖x − z‖²)                                              ← an infinite-dimensional φ
```

**Kernel perceptron (dual form):** instead of `w`, keep a count `αᵢ` of how often example i was a
mistake:

```
f(x) = Σᵢ αᵢ yᵢ K(xᵢ, x)          on a mistake on example i:  αᵢ += 1
```

| Model on circles | Test accuracy |
|---|---|
| linear perceptron | 0.550 |
| **RBF kernel perceptron** | **1.000** |
| sklearn RBF `SVC` | 1.000 |

The kernel perceptron uses just **13 of 280** training points (non-zero α). That sparsity is the
support-vector idea again. The **kernel SVM** solves the margin-maximising version of this dual
(with SMO or a QP solver). Costs: the kernel matrix is `n × n`, so training scales ~`O(n²–n³)`,
which is why kernel SVMs fade beyond ~100k rows.

## 4. Linear discriminant analysis (LDA)

**Generative idea:** model each class as a Gaussian with its own mean `μₖ` and a **shared**
covariance `Σ`, then apply Bayes' rule. With a shared covariance the quadratic terms cancel and
the boundary is **linear**:

```
δₖ(x) = xᵀ Σ⁻¹ μₖ − ½ μₖᵀ Σ⁻¹ μₖ + log πₖ          predict argmaxₖ δₖ(x)
```

```python
S = Σ_k (X_k − μ_k)ᵀ(X_k − μ_k) / (n − K)          # pooled within-class covariance
W = means @ inv(S);  b = −½ diag(W μᵀ) + log(priors)
```

- **Identical predictions** to sklearn's `LinearDiscriminantAnalysis` on 3-class data.
- **Pitfall found while building this:** with 2 *redundant* features (exact linear combinations of
  others, sklearn's default in `make_classification`), the covariance has condition number
  **6.4e16**, i.e. it's singular, and `inv()` returns numerical garbage (3 % of predictions
  differed). sklearn uses an SVD solver; in your own code use `pinv`, add a small ridge
  (`S + εI`), or drop the redundant features.
- **QDA** gives each class its own `Σₖ`, so the boundaries are quadratic, at the cost of many more
  parameters.
- LDA also gives a **supervised projection** to at most K − 1 dimensions that best separates the
  classes (Fisher's discriminant), unlike PCA, which ignores the labels.

## 5. Multinomial naive Bayes for text

**Model:** a document is a bag of words drawn independently given the class:

```
log P(class | doc) = log P(class) + Σ_words count(w) · log P(w | class) + const
P(w | class) = (count of w in class + α) / (total words in class + α·V)      ← Laplace smoothing, α = 1
```

8 tiny training messages (4 spam, 4 ham):

- New messages `"free prize meeting"`, `"team report review"`, `"money money money"` → **spam,
  ham, spam**.
- Log-probabilities identical to sklearn's `MultinomialNB`.
- **Why smoothing:** without it, **11 words** have `P(word | ham) = 0`. A single such word in a
  message would make `P(ham | message) = 0` no matter what else it says. Smoothing pretends every
  word was seen α extra times.

Still a strong, fast baseline for text: training is one counting pass, and it works with very
little data. Use **log-probabilities** (sums), never raw products, which underflow.

## 6. CART regression tree

**Greedy recursive partitioning:** at each node try every feature and every threshold; pick the
split that most reduces the **sum of squared errors**; recurse until max depth or minimum leaf
size; predict the leaf mean.

**The speed trick:** sort each feature once, then compute every split's SSE in O(1) from prefix
sums:

```
SSE(left) = Σy² − (Σy)² / n_left          ← from cumulative sums of y and y²
```

That makes each node `O(d · n log n)` instead of `O(d · n²)`.

- **Identical predictions** to sklearn's `DecisionTreeRegressor(max_depth=4, min_samples_leaf=5)`
  on 200 new points.
- Thresholds are **midpoints** between consecutive distinct values (as in sklearn).
- Classification trees are the same with Gini or entropy instead of SSE (chapter 15, D7).

## 7. XGBoost-style second-order boosting

Plain gradient boosting (chapter 06) fits each tree to the **gradient**. XGBoost uses a
**second-order Taylor expansion** of the loss around the current prediction, with per-example
gradient `gᵢ` and hessian `hᵢ`:

```
log loss:   gᵢ = pᵢ − yᵢ          hᵢ = pᵢ (1 − pᵢ)
objective for a tree:  Σ_leaves [ G·w + ½ (H + λ) w² ] + γ · (number of leaves)
```

Minimising each leaf's quadratic gives closed forms:

```
best leaf weight     w* = −G / (H + λ)                    ← a Newton step, shrunk by λ
best leaf objective     = −G² / (2 (H + λ))
split gain = ½ [ G_L²/(H_L+λ) + G_R²/(H_R+λ) − G²/(H+λ) ] − γ      ← split only if gain > 0
```

For `g = [0.4, −0.2, 0.3]`, `h = [0.24, 0.16, 0.21]`, λ = 1: `w* = −0.5/1.61 = −0.3106` (checked),
and the objective formula is checked too.

| | Test ROC-AUC |
|---|---|
| scratch XGBoost-style (60 rounds, η 0.3, depth 3) | **0.970** |
| sklearn `HistGradientBoostingClassifier` (same settings) | 0.971 |

The regularisation knobs map to the formulas:

- **λ** (L2 on leaf weights): shrinks the weights, and reduces gain for leaves with little hessian
  mass (few or confident examples);
- **γ** (min split gain): prunes. With γ = 10⁶ the tree is a single leaf (checked);
- **min_child_weight** = minimum H in a child;
- **η** (learning rate) and subsampling, as in chapter 06.

LightGBM and CatBoost use the same gain formula, plus histogram binning (LightGBM) and ordered
boosting (CatBoost).

## 8. DBSCAN

```
1. neighbours(i) = points within eps of i (including i)
2. core point: |neighbours| ≥ min_samples
3. for each unvisited core point: start a new cluster and grow it with a stack:
   pop p, label all its neighbours; push the neighbours that are core points
4. anything never reached = noise (−1)
```

- On two moons plus 2 far-away points: **identical** clusters and noise to sklearn (ARI 1.0):
  2 clusters, 399 core points, 2 noise points.
- Border points (non-core, but within eps of a core point) join the first cluster that reaches
  them, so their label can depend on visiting order. That's DBSCAN's only non-determinism.
- The naive neighbour search is `O(n²)`; libraries use KD-trees or ball trees.

## 9. Gaussian mixture via expectation–maximisation (EM)

**Chicken and egg:** if we knew which Gaussian generated each point, fitting the Gaussians would be
easy; if we knew the Gaussians, assigning points would be easy. EM alternates:

```
E-step: responsibility r_ik = πₖ N(xᵢ; μₖ, Σₖ) / Σⱼ πⱼ N(xᵢ; μⱼ, Σⱼ)      (computed in log space)
M-step: Nₖ = Σᵢ r_ik,  πₖ = Nₖ/n,  μₖ = Σᵢ r_ik xᵢ / Nₖ,  Σₖ = Σᵢ r_ik (xᵢ−μₖ)(xᵢ−μₖ)ᵀ / Nₖ
```

**Implementation details that matter:**

- Compute densities in **log space** with the log-sum-exp trick; raw densities underflow in high
  dimensions.
- Use a **Cholesky** factor for `log|Σ|` and the Mahalanobis term.
- Add `1e-6·I` to the covariances so a component can't collapse onto one point (likelihood → ∞).

Results:

- The log-likelihood **never decreased** across iterations (a theorem for EM, checked).
- Converged in **30 iterations**.
- Recovered the clusters (ARI > 0.95), with weights `[0.406, 0.594]` (true 0.4/0.6).
- The final average log-likelihood matches sklearn (−3.651).
- EM finds a **local** optimum, so use several initialisations (sklearn's `n_init`).
- K-Means is the limit of EM with equal spherical covariances and hard assignments.

## 10. Agglomerative clustering (average linkage)

```
start: every point is its own cluster
repeat: merge the two clusters with the smallest average pairwise distance
```

- **Identical** to sklearn's `AgglomerativeClustering(linkage="average")` on 3 blobs.
- **Merge heights only grow** (true for single, complete, average and Ward linkage), so they form a
  **dendrogram**. The last four heights were `[1.64, 1.66, 6.10, 17.29]`. The jump from 1.66 to
  6.10 means "merging to 2 clusters would join genuinely different groups", so **cut at 3**.
- The naive version here is `O(n³)`; real implementations keep a priority queue or use the
  Lance–Williams update for `O(n² log n)`. It is still memory-bound at `O(n²)`, so use it for
  thousands of points, not millions.

## 11. Isolation Forest

**Idea:** anomalies are **few and different**, so random splits isolate them quickly.

```
build tree on a subsample of 256 points: pick a random feature and a random threshold
                                         between its min and max; recurse until depth log₂(256) = 8
path length h(x) = depth reached + c(size of the leaf)        ← estimate for unfinished subtrees
score(x) = 2^( −mean h(x) / c(256) )      ≈ 1 → anomaly,  ≤ 0.5 → normal
c(n) = 2 (ln(n−1) + 0.5772) − 2(n−1)/n    ← the average path of an unsuccessful BST search; c(256) = 10.24
```

| | Mean score |
|---|---|
| 500 normal points | 0.429 |
| 10 planted anomalies | **0.776** |

ROC-AUC is > 0.95.

**Why subsample?** Small samples reduce **swamping** (normal points near anomalies get flagged) and
**masking** (clusters of anomalies hide each other), and make it very fast: `O(trees · 256 · log 256)`
to build, regardless of n.

## 12. Apriori

**Apriori principle:** every subset of a frequent itemset is frequent. So size-k candidates are
built only from frequent (k−1)-itemsets, and a candidate is dropped if **any** of its (k−1)-subsets
is infrequent:

```python
cands = {a | b for a in level for b in level if len(a | b) == k}
cands = {c for c in cands if all(frozenset(s) in prev for s in combinations(c, k-1))}   # pruning
```

The data is 300 baskets from 10 items, with a planted trio {A, B, C} in ~30 % of them.

- At support ≥ 0.2, Apriori finds **exactly** the same frequent itemsets as brute force, including
  {A, B, C}.
- It only counted **46** candidates of size ≥ 2, where brute force over sizes 2–4 checks **375**.
  The gap grows exponentially with the catalogue size.
- **FP-Growth** avoids candidate generation altogether by compressing the baskets into a prefix
  tree. Rules and **lift** are then computed from these supports (chapter 05 §8).

## 13. t-SNE

**Goal:** place points in 2-D so that **neighbours stay neighbours**.

1. **High-dimensional affinities.** For each point, a Gaussian over the others, its width chosen by
   binary search so the distribution's **perplexity** (`e^entropy`) is 30 ("about 30 effective
   neighbours"). Then symmetrise: `P = (P + Pᵀ)/2n`.
2. **Low-dimensional affinities** with a heavy-tailed **Student-t** kernel:
   `Q ∝ 1/(1 + ‖yᵢ − yⱼ‖²)`. The heavy tails let dissimilar points sit far apart, which solves the
   "crowding problem".
3. **Minimise `KL(P ‖ Q)`** by gradient descent with momentum:
   `∂KL/∂yᵢ = 4 Σⱼ (pᵢⱼ − qᵢⱼ)(yᵢ − yⱼ)/(1 + ‖yᵢ − yⱼ‖²)`.
4. **Early exaggeration** (P × 12 for the first 100 steps) forms tight clusters first.

On 400 digit images (64-D):

| 2-D map | Trustworthiness (neighbours preserved) | 5-NN accuracy in 2-D |
|---|---|---|
| PCA | 0.839 | 0.640 |
| **scratch t-SNE** | **0.925** | **0.887** |

The final KL is 0.967. The cost is `O(n²)` per iteration; Barnes–Hut (sklearn's default) and FFT
approximations make it `O(n log n)`. Chapter 05 lists the interpretation pitfalls: cluster sizes
and the distances between clusters aren't meaningful.

---

## Exercises

1. Why does the perceptron converge on separable data but SVM training is preferred?
2. What do λ and γ do in XGBoost's split-gain formula, in words?
3. Your EM run produces a log-likelihood of +∞. What happened?
4. In DBSCAN, why can a border point's label change if you shuffle the data, while core points'
   labels can't?

<details><summary>Answers</summary>

1. The perceptron stops at *any* separating line, often one hugging the data, and it never
   converges on non-separable data. The SVM finds the unique **maximum-margin** boundary (better
   generalisation) and handles overlap through the hinge loss.
2. λ adds to the hessian sum in every denominator. It shrinks the leaf weights and reduces the gain
   of splits whose children have little hessian mass (few or confident examples), so the model is
   less eager to fit them. γ is a fixed cost per new leaf: a split must improve the objective by
   more than γ.
3. A component collapsed onto a single point (or a few identical ones): its variance → 0, so its
   density → ∞. Add a small ridge to the covariances (`reg_covar`), restart from another
   initialisation, or reduce k.
4. Core points are connected through chains of core points regardless of order, so whole clusters
   are well defined. A border point within eps of core points from **two** clusters joins whichever
   cluster's expansion reaches it first.
</details>

## Interview questions

**Q: Hinge loss vs log loss?** Both are convex surrogates for the 0–1 loss. Hinge is exactly zero
beyond the margin, which gives sparse support vectors and no probabilities. Log loss is never zero:
every point contributes, and the outputs are calibrated-ish probabilities.

**Q: What is the kernel trick?** If an algorithm uses data only through dot products, replace them
with a kernel `K(x, z) = φ(x)·φ(z)` and you implicitly work in φ's (possibly infinite) feature
space without computing it. Valid kernels are positive semi-definite (Mercer's condition).

**Q: Why does XGBoost use second derivatives?** The Newton step `−G/(H + λ)` gives an optimal leaf
value and a principled split gain for any twice-differentiable loss, converging in fewer, better
trees than first-order fitting of residuals. The hessian also weights the examples (confident
predictions have small `p(1 − p)`).

**Q: Why does EM never decrease the likelihood?** Each E-step builds a lower bound on the
log-likelihood that touches it at the current parameters (via Jensen's inequality); the M-step
maximises that bound. So the new likelihood ≥ the bound at the new parameters ≥ the old
likelihood.

---

**Next:** [17 · Probabilistic models](../17-Probabilistic-Models/README.md)
