# 04 · Classification: predicting a category

> Spam or not? Fraud or not? Which of 10 digits? Will this customer churn? Classification
> predicts a **class**, and ideally a trustworthy **probability** for it.

Run it: `../.venv/bin/python classification.py` → **42/42 checks**.

---

## 1. Logistic regression from scratch

### Intuition

Linear regression outputs any number. For a probability you need something between 0 and 1, so
squash the linear score with the **sigmoid**:

```
z = w·x + b          p = σ(z) = 1 / (1 + e^(−z))

     p
   1 │                 ________
     │              _/
 0.5 │ ………………………/………………   ← z = 0 is the decision boundary
     │       ___/
   0 │______/
     └────────────────────────── z
```

`σ(0) = 0.5`, `σ(2) = 0.881`. Predict class 1 when `p ≥ threshold` (0.5 by default, though
that's often the wrong choice; see §6).

### The loss: log loss (binary cross-entropy)

```
L = −(1/n) Σ [ y log p + (1−y) log(1−p) ]
```

Why not MSE? Combined with a sigmoid, MSE is non-convex and has tiny gradients when the model is
confidently wrong. Log loss is convex and is the **maximum likelihood** for a Bernoulli outcome.
Before training (all `p = 0.5`) it equals `ln 2 = 0.693`.

### The gradient is beautifully simple

```
∂L/∂w = (1/n) Xᵀ (p − y)        ∂L/∂b = mean(p − y)
```

It's the same form as linear regression: *features × error*. Trained on data with true
`w = [2, −3]`, `b = 0.5`, the scratch model learns `w = [2.077, −2.941]`, `b = 0.524`, and matches
scikit-learn to 0.005.

### Interpreting weights: odds

`log(p/(1−p)) = w·x + b`. So **one more unit of x₁ multiplies the odds by `e^w₁`**:
`e^2.077 ≈ 8`. "Each extra support ticket multiplies the odds of churn by 1.4" is how you
explain a logistic model to a business team.

**Multiclass:** **softmax** `pₖ = e^zₖ / Σⱼ e^zⱼ` with cross-entropy loss (multinomial logistic
regression), or one-vs-rest.

## 2. KNN and naive Bayes

### K-nearest neighbours: "you are like your neighbours"

To classify a point, find the k closest training points and take a majority vote. The scratch
version uses broadcasting to compute all distances at once:

```python
d = ((X_query[:, None, :] - X_train[None, :, :]) ** 2).sum(-1)   # (queries, train) distances
```

It matches sklearn exactly. On the two-moons data:

| k | Train accuracy | Test accuracy |
|---|---|---|
| 1 | **1.000** (each point is its own neighbour) | 0.900 |
| 15 | | **0.906** |

- Small k → wiggly boundary (high variance). Large k → smooth (high bias).
- **Must scale features:** distance is dominated by the largest-unit feature.
- No training, but prediction is `O(n·d)` per query; use approximate nearest-neighbour indexes
  at scale.
- Curse of dimensionality: in 1,000 dimensions everything is roughly equally far away.

### Gaussian naive Bayes: Bayes' rule plus one bold assumption

```
P(class | x) ∝ P(class) · Π_j P(x_j | class)      ← "naive": features independent given the class
```

Each `P(x_j | class)` is a 1-D normal with that class's mean and variance. Training is just
counting means and variances, so it's **extremely fast** and works with tiny data. It's the classic
baseline for text (multinomial NB on word counts). The scratch version matches sklearn's
`GaussianNB` exactly. On the moons it reaches 0.861: its boundary is a smooth curve and can't
follow the interlocking shapes.

## 3. Decision trees: one split from scratch

A tree asks yes/no questions: "hours studied ≤ 3?". It picks the question that makes the child
nodes **purest**.

**Gini impurity** = the probability of mislabelling a random item if you label it randomly by the
node's class mix:

```
Gini = 1 − Σ pₖ²         pure node → 0       50/50 node → 0.5
```

| hours | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
|---|---|---|---|---|---|---|---|---|
| passed | 0 | 0 | 0 | 1 | 0 | 1 | 1 | 1 |

The scratch search tries every threshold:

- the best is **≤ 3**: left `[0,0,0]` has Gini 0; right `[1,0,1,1,1]` has Gini 0.32;
- weighted Gini = `(3·0 + 5·0.32)/8 = 0.2`;
- sklearn places the same split at the **midpoint, 3.5**.

The tree then recurses into each child until a stopping rule: `max_depth`,
`min_samples_leaf`, or no improvement. **Entropy** (information gain, chapter 02) works the same
way and usually picks similar splits.

Trees: no scaling needed, handle non-linearity and interactions, readable. But a deep tree
**memorises** (chapter 03 showed train R² = 1), so prune it or use ensembles.

## 4. Metrics: the most-asked interview topic

```
                      predicted
                     1        0
  actual   1        TP       FN      ← recall = TP/(TP+FN)
           0        FP       TN      ← specificity = TN/(TN+FP)
                     ↑
                precision = TP/(TP+FP)
```

Example: 4 positives, 6 negatives, threshold 0.5.

| Metric | Question it answers | Value |
|---|---|---|
| confusion (TP, FP, FN, TN) | | (3, 1, 1, 5) |
| **Precision** | of my alarms, how many were real? | 0.75 |
| **Recall** (sensitivity, TPR) | of the real ones, how many did I catch? | 0.75 |
| **F1** | harmonic mean of the two (low if either is low) | 0.75 |
| Specificity | of the negatives, how many did I leave alone? | 0.833 |
| **ROC-AUC** | P(random positive scores above random negative) | **0.875** |
| **PR-AUC** (average precision) | mean precision at each true positive | **0.854** |

Lower the threshold to 0.25 and recall goes to 1.0 (all 4 caught), but there are now 2 false
alarms. **Every threshold is a trade-off**; ROC and PR curves show all of them at once.

**ROC-AUC computed as ranking:** of the 4 × 6 = 24 (positive, negative) pairs, the positive
scores higher in 21 → 21/24 = 0.875. That's literally the scratch implementation, and it matches
sklearn.

**Average precision:** walk down the ranked list, and at each true positive record the precision
so far: `1/1, 2/2, 3/4, 4/6` → mean **0.854**.

| Use | When |
|---|---|
| accuracy | balanced classes and equal error costs only |
| precision | false alarms are expensive (spam filter deleting real email) |
| recall | misses are expensive (cancer screening, fraud) |
| F1 / Fβ | need one number balancing both; β > 1 weights recall |
| ROC-AUC | ranking quality; threshold-free; fine when classes are balanced |
| **PR-AUC** | **rare positives**: ROC-AUC looks good even when precision is awful |
| log loss | you need good **probabilities** (pricing, risk, combining models) |

## 5. The model zoo

3,000 rows, 20 features (8 informative), on the same split:

| Model | Accuracy | ROC-AUC | Boundary |
|---|---|---|---|
| logistic regression | 0.831 | 0.899 | linear |
| LDA | 0.830 | 0.900 | linear (assumes Gaussian classes with shared covariance) |
| naive Bayes | 0.839 | 0.937 | quadratic, axis-aligned |
| decision tree (depth 8) | 0.876 | 0.894 | axis-aligned boxes |
| KNN (k=15) | 0.923 | 0.978 | anything, local |
| QDA | 0.940 | 0.985 | quadratic (each class gets its own covariance) |
| SVM (RBF) | 0.940 | 0.983 | smooth non-linear |
| random forest | 0.947 | 0.986 | many boxes averaged |
| **gradient boosting** | **0.947** | **0.989** | many small boxes added |

- **LDA ≈ logistic regression:** both draw a straight line; they differ only in how they
  estimate it.
- `make_classification` builds each class from Gaussian clusters, which suits **QDA**.
  (`reg_param=0.01` stabilises the covariance estimate, because 4 features are redundant linear
  combinations of others.)
- A single tree is the weakest tree model (AUC 0.894); averaging (RF) or boosting fixes it.

**SVM in one paragraph:** find the boundary with the **widest margin** between classes; only the
closest points (**support vectors**) matter. `C` trades margin width against training errors.
The **kernel trick** computes similarity in a high-dimensional space without building it, so an
RBF kernel gives a smooth non-linear boundary. Strong on small/medium data, scales badly past
~100k rows, and `probability=True` needs extra internal cross-validation.

## 6. Project: imbalanced fraud detection

40,000 transactions, **1.08 % fraud**.

**Trap 1: accuracy.** "Always predict legit" scores **~99 %** and catches zero fraud.

| Model @ threshold 0.5 | Recall | Precision | ROC-AUC | PR-AUC |
|---|---|---|---|---|
| plain logistic regression | 0.000 | 0.000 | 0.637 | 0.029 |
| `class_weight="balanced"` LR | 0.569 | 0.016 | 0.656 | 0.017 |
| gradient boosting | 0.331 | 0.782 | **0.949** | **0.542** |

What to read from this table:

1. Plain LR never crosses 0.5 for a fraud case: **recall 0**.
2. **Class weights** (each fraud counts ~92 times as much in the loss) push probabilities up, and
   recall jumps to 0.569. But precision is 1.6 %, so 98 % of alarms are false. Weighting shifts
   the probabilities; it doesn't make the model better at **ranking** (the AUCs barely move).
3. This data is non-linear, and **boosting** ranks far better.
4. **PR-AUC (0.542) is much lower than ROC-AUC (0.949).** ROC's false-positive rate divides by
   the huge number of negatives, so thousands of false alarms barely register. PR-AUC shows the
   real pain. With rare positives, report PR-AUC.

### Choosing the threshold by money, not by 0.5

A missed fraud costs **200**; a false alarm (a manual review) costs **5**.

1. Split off a **validation** set from training (never tune on test).
2. Try thresholds 0.01–0.9 and compute `200·FN + 5·FP` for each.
3. The best is **0.03**.
4. On the untouched test set the cost drops from **18,440** (at 0.5) to **10,675**.

Theory check: for **calibrated** probabilities, flag when `p · C_FN > (1−p) · C_FP`, i.e.
`p > C_FP/(C_FP + C_FN) = 5/205 ≈ 0.024`. The data-driven 0.03 agrees.

Other tools for imbalance:

- resampling: undersample the majority class, or oversample/SMOTE the minority, **on the
  training fold only**;
- focal loss;
- anomaly-detection framing (chapter 05);
- for evaluation, **stratified** splits and, for fraud, **time-based** splits (chapter 07).

## 7. Calibration: is "80 %" really 80 %?

A model is **calibrated** if, among all the cases where it says 0.8, about 80 % are positive.
Ranking metrics (AUC) don't care, but pricing, risk scores, expected-cost thresholds (§6) and
combining models all do.

**Expected calibration error (ECE):** bucket the predictions by probability and average
`|mean predicted − fraction actually positive|`.

The demo feeds the **same evidence 5 times** (near-copies of two features):

| Model | ECE |
|---|---|
| naive Bayes | **0.185**: badly over-confident; it multiplies the same evidence 5 times as if independent |
| logistic regression | 0.057: its weights share the credit among correlated copies |
| naive Bayes + isotonic calibration | **0.020** |

The calibrators:

- **Platt scaling:** fit a logistic regression on the scores;
- **isotonic regression:** a monotone step function; needs more data.

Fit them on held-out data (`CalibratedClassifierCV` does this with internal CV). Because they're
monotone, they barely change AUC (checked: < 0.02).

Typical behaviour:

- naive Bayes: over-confident;
- random forest: under-confident (averaging pulls probabilities towards the middle);
- boosting and deep nets: often over-confident;
- logistic regression: usually fine.

---

## Exercises

1. Precision is 0.9 and recall is 0.1. What is F1, and what does the model do?
2. Why does the logistic regression gradient reduce to `Xᵀ(p − y)/n`? (Hint: `σ′ = σ(1−σ)`.)
3. Your fraud model has ROC-AUC 0.98 and the business says "it's useless". How can both be true?
4. When would you use F2 instead of F1?

<details><summary>Answers</summary>

1. `F1 = 2·0.9·0.1/(1.0) = 0.18`. The model is very cautious: it flags few cases, almost all
   correct, but misses 90 % of positives.
2. For one example, `∂L/∂z = −y/p · σ′ + (1−y)/(1−p) · σ′`, with `σ′ = p(1−p)`. That gives
   `−y(1−p) + (1−y)p = p − y`. Then `z = w·x + b`, so multiply by `x`.
3. With 0.1 % fraud, a 1 % false-positive rate on 1 million legit transactions is 10,000 false
   alarms against ~1,000 frauds: precision < 10 %. ROC-AUC hides this; look at PR-AUC and
   precision at the operating threshold.
4. When recall matters more than precision (F2 weights recall 2 times more): disease screening,
   fraud pre-filters, safety-critical detection.
</details>

## Interview questions

**Q: Why is it called logistic *regression* if it classifies?** It regresses the **log-odds**
linearly on the features; the classification comes from thresholding the probability.

**Q: How do you handle class imbalance?** First pick the right metric (PR-AUC, recall at a fixed
precision, expected cost). Then: class weights or resampling (training fold only), a model that
ranks well (boosting), a threshold chosen on validation by business cost, and calibration if the
probabilities are used downstream. Collect more positives if you can.

**Q: ROC-AUC vs PR-AUC?** ROC-AUC is the probability a random positive outranks a random
negative; it is insensitive to class balance. PR-AUC focuses on the positive class and drops
sharply when false positives swamp a rare class. Use PR-AUC for rare positives.

**Q: Generative vs discriminative?** Generative models learn `P(x | y)` and `P(y)`, then apply
Bayes (naive Bayes, LDA, QDA). They work with little data and can generate samples. Discriminative
models learn `P(y | x)` directly (logistic regression, SVM, trees); usually more accurate with
enough data.

**Q: How does the decision threshold relate to costs?** For calibrated `p`, predict positive when
`p > C_FP/(C_FP + C_FN)`. If misses cost 40 times more than false alarms, the threshold is ~0.024,
not 0.5.

---

**Next:** [05 · Unsupervised learning](../05-Unsupervised/README.md)
