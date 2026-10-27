# 06 · Ensembles: many weak opinions, one strong one

> Ask one doctor and you might get an unlucky answer. Ask 25 independent doctors and take the
> majority, and you're far more likely to be right. Ensembles do this with models, and they win
> most tabular-data competitions and production systems.

Run it: `../.venv/bin/python ensembles.py` → **21/21 checks**.

| Family | How | Mainly reduces | Examples |
|---|---|---|---|
| **Bagging** | train models **in parallel** on bootstrap samples, average them | **variance** | bagged trees, random forest |
| **Boosting** | train models **in sequence**, each fixing the last one's errors | **bias** (and variance) | AdaBoost, gradient boosting, XGBoost, LightGBM, CatBoost |
| **Stacking** | a meta-model learns how to combine different models | both | Kaggle blends, ranking systems |

---

## 1. Why averaging works (the one formula to remember)

Average B models whose errors each have variance σ² and pairwise correlation ρ:

```
Var(average) = ρ σ² + (1 − ρ) σ² / B
```

| ρ | Simulated (B = 25, σ = 1) | Theory |
|---|---|---|
| 0 (independent) | **0.040** | 1/25 = 0.04 |
| 0.5 | **0.521** | 0.5 + 0.5/25 = 0.52 |

- With independent models, the error variance shrinks **25 times**.
- With correlation 0.5, it can **never** fall below 0.5, however many models you add.

> **The whole game is building models that are good *and* different.** Random forests add random
> feature subsets precisely to reduce ρ.

The same idea with votes: 25 independent voters, each right 60 % of the time, are right
**84.6 %** of the time as a majority (Condorcet's jury theorem).

## 2. Bagging from scratch (bootstrap aggregating)

```python
for b in range(100):
    idx = rng.integers(0, n, n)              # draw n rows WITH replacement
    tree = DecisionTreeRegressor().fit(X[idx], y[idx])
predict = mean of all trees
```

- Each bootstrap sample contains **63.2 %** of the distinct rows: `1 − (1 − 1/n)ⁿ → 1 − 1/e`.
- Deep trees have low bias but high variance, so they're perfect for averaging. Test MSE went
  from **12.74** (one tree) to **3.88** (100 bagged trees).

### Out-of-bag (OOB) error: free validation

Each row is left out of ~36.8 % of the trees. Predict each row using **only the trees that didn't
see it**. That OOB MSE (**3.84**) is very close to the real test MSE (3.88), and needed no
hold-out set. sklearn's `BaggingRegressor` gives 3.80.

## 3. Random forest = bagging + random feature subsets

At **every split**, a tree may only consider a random subset of features (`max_features`). Strong
features can't dominate every tree, so the trees become less correlated.

**Dataset 1: Friedman, 5 useful features + 5 pure noise**

| `max_features` | Correlation between trees | Test MSE | OOB R² |
|---|---|---|---|
| 1.0 (= plain bagging) | 0.700 | **3.62** | 0.839 |
| 0.33 | **0.542** | 4.08 | 0.814 |

Correlation dropped as promised, yet the error went **up**. With only 3 random candidates per
split, there's a good chance none of them is useful, so each tree gets much weaker. Decorrelating
only pays if the trees stay strong.

**Dataset 2: 20 noisy, redundant views of 4 hidden factors**

| `max_features` | Test MSE |
|---|---|
| 1.0 | 0.961 |
| **0.5** | **0.938** |

Here many features carry the same signal, so a random subset almost always contains a good one,
and decorrelation wins.

> **`max_features` is a bias–variance knob: tune it** (on OOB or CV). Common defaults: √d for
> classification, ~d/3 or 1.0 for regression. sklearn's regressor default is 1.0.

**Number of trees:** test MSE `1: 16.63 → 10: 5.11 → 100: 4.27 → 400: 4.06`. More trees
**never overfit**; they just stop helping, and they cost memory and latency.

Random forest's strengths: a strong default, hard to break, parallel, OOB estimate, handles mixed
features. Weaknesses: large models, slow prediction for low-latency serving, can't extrapolate,
importances biased towards high-cardinality features.

## 4. Gradient boosting from scratch

### Intuition

Start with a simple guess (the mean). Look at what you got wrong (the **residuals**). Fit a
**small** tree to those mistakes, add a fraction of it, and repeat. Each tree is a weak correction.

```python
F = mean(y)
for m in range(M):
    residual = y - F                          # = negative gradient of squared loss
    tree = DecisionTreeRegressor(max_depth=3).fit(X, residual)
    F += learning_rate * tree.predict(X)
```

"Gradient" because for any differentiable loss you fit the **negative gradient** of the loss with
respect to the current prediction. For squared loss that's the residual; for log loss it's
`y − p`; XGBoost also uses the second derivative.

| | Test MSE |
|---|---|
| scratch, 300 trees, lr 0.1, depth 3 | **1.992** |
| sklearn `GradientBoostingRegressor`, same settings | **2.001** |

Test MSE as trees are added: `1: 22.78 → 10: 10.86 → 100: 2.27 → 300: 1.99`.

### The knobs

| Knob | Effect seen in the run |
|---|---|
| **learning rate (shrinkage)** | lr 1.0 is best at tree **8** (MSE 4.32), then overfits to 5.37. lr 0.1 reaches **1.99**. Small steps + more trees generalise better |
| **tree depth** | depth-10 trees: train MSE **0.0000**, test **5.94**. Weak learners must be **shallow** (3–8) |
| n_estimators | pick it with **early stopping** on a validation set |
| subsample (< 1) | stochastic gradient boosting: each tree sees a random fraction of rows (adds randomness, like bagging) |

### XGBoost, LightGBM, CatBoost: what they add

| Library | Key ideas |
|---|---|
| **XGBoost** | 2nd-order (Newton) steps; L1/L2 penalties on leaf weights; sparsity-aware splits (missing values learn a default direction) |
| **LightGBM** | histogram binning; **leaf-wise** growth (grow the best leaf, not level by level); GOSS sampling; very fast on big data |
| **CatBoost** | **ordered target statistics** for categoricals (no target leakage); symmetric trees; strong defaults |
| sklearn `HistGradientBoosting*` | LightGBM-style histograms, native NaN and categorical support, already in your venv |

## 5. AdaBoost from scratch: re-weight the mistakes

The original boosting algorithm (1995), with labels in {−1, +1}:

```
weights w = 1/n for every row
repeat:
    fit a stump (depth-1 tree) on the weighted data
    err   = weighted error rate
    alpha = ½ ln((1 − err) / err)        ← better stumps get bigger votes
    w    *= exp(−alpha · y · pred)        ← misclassified rows get heavier
    normalise w
final = sign(Σ alpha · stump)
```

- 1 stump: **0.785** test accuracy.
- 200 boosted stumps: **0.926**, matching sklearn's `AdaBoostClassifier`.

AdaBoost is gradient boosting with the **exponential loss**. Its weakness: label noise.
Mislabelled points get ever-larger weights and the model chases them.

## 6. Voting and stacking

Four different base models on the same data (ROC-AUC):

| lr | knn | nb | rf | **soft vote** | **stacking** |
|---|---|---|---|---|---|
| 0.864 | 0.943 | 0.831 | **0.966** | 0.925 | **0.969** |

- **Soft voting** averages the probabilities equally. The two weak models drag it **below**
  the best single model (0.925 < 0.966).
- **Stacking** trains a **meta-model** (logistic regression) on **out-of-fold** predictions of
  the base models (`cv=5`, so the meta-model never sees predictions on data a base model trained
  on). It learned the weights `lr 1.05, knn 3.56, nb −2.0, rf 8.39`, so it leans on the forest and
  KNN and even subtracts naive Bayes's over-confidence. Result **0.969**, ≥ the best single model.

> **Stacking leakage:** if you fit the meta-model on in-sample predictions, the base models look
> perfect (they memorised the training rows) and the meta-model learns nonsense. Always use
> out-of-fold predictions.

---

## Exercises

1. 100 trees with correlation 0.3 and σ² = 4: what's the variance of their average? What's the
   floor as B → ∞?
2. Why does a random forest use **deep** trees while gradient boosting uses **shallow** ones?
3. Your gradient-boosting model's validation loss is best at tree 150 of 1,000. What should you do?
4. When would a simple average beat stacking?

<details><summary>Answers</summary>

1. `0.3·4 + 0.7·4/100 = 1.2 + 0.028 = 1.228`. The floor is `ρσ² = 1.2`.
2. Bagging reduces **variance**, so it needs low-bias, high-variance members (deep trees).
   Boosting reduces **bias** step by step, so it needs high-bias, low-variance members (shallow
   trees). Deep trees would memorise within a few rounds (as the depth-10 run did).
3. Use early stopping: keep ~150 trees (or retrain with that count), or lower the learning rate
   and allow more trees. Libraries do this with `early_stopping_rounds` / `n_iter_no_change`.
4. Small data (the meta-model overfits), or base models of similar quality with uncorrelated
   errors. Averaging has no parameters to overfit.
</details>

## Interview questions

**Q: Bagging vs boosting?** Bagging trains independent models in parallel on bootstrap samples
and averages them, reducing variance (random forest). Boosting trains sequentially, each model
fitting the errors of the ensemble so far, reducing bias (gradient boosting). Boosting is usually
more accurate on tabular data but more sensitive to noise and hyperparameters.

**Q: Why does a random forest subsample features?** To **decorrelate** the trees. With bagging
alone, every tree splits on the same strong feature first and they all make similar errors; the
averaging floor is `ρσ²`. Feature subsampling lowers ρ. It's a trade-off: too few features makes
each tree weak.

**Q: Explain gradient boosting.** Start from a constant. Each round, compute the negative gradient
of the loss at the current predictions (the residuals for MSE), fit a shallow tree to it, and add
it scaled by a learning rate. Control overfitting with the learning rate, depth, subsampling,
regularisation and early stopping.

**Q: Why can XGBoost handle missing values?** At each split it tries sending missing values left
and right and learns the better **default direction** from the training loss.

**Q: OOB error?** Each bootstrap leaves ~37 % of rows out. Predicting each row with only the trees
that didn't train on it gives an almost unbiased validation estimate for free.

---

**Next:** [07 · Feature engineering and evaluation](../07-Feature-Engineering-and-Evaluation/README.md)
