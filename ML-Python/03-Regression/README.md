# 03 · Regression: predicting a number

> House price, delivery time, demand next week, a customer's lifetime value: whenever the
> answer is a **number**, it's regression.

Run it: `../.venv/bin/python regression.py` → **31/31 checks**.

---

## 1. Linear regression from scratch

### Intuition

Fit the straight line (or, with many features, the flat "hyperplane") that is closest to all the points:

```
ŷ = w₁x₁ + w₂x₂ + … + b        or, for all rows at once:   ŷ = X w + b
```

"Closest" means **smallest mean squared error**: `MSE = (1/n) Σ (ŷᵢ − yᵢ)²`.
Why squared? Because it's smooth (easy to differentiate), punishes big misses, and equals maximum
likelihood under Gaussian noise (chapter 02).

### Way 1: the normal equation (exact, one shot)

Set the gradient to zero and solve:

```
(XᵀX) θ = Xᵀy           θ = [b, w₁, …, w_d]     (X gets a column of 1s for b)
```

```python
Xb = np.column_stack([np.ones(len(X)), X])
theta = np.linalg.solve(Xb.T @ Xb, Xb.T @ y)
```

Data generated with `w = [3, −2, 0.5]`, `b = 4` plus noise. Recovered:
`w = [3.021, −1.993, 0.531]`, `b = 3.969`.

### Way 2: gradient descent (iterative, scales to anything)

```
error = Xw + b − y
∂L/∂w = (2/n) Xᵀ error        ∂L/∂b = (2/n) Σ error
w ← w − η ∂L/∂w
```

After 2,000 steps it matches the normal equation to `1e-6`. The loss never goes up (apart from
`1e-16` rounding wobble once it has converged) and ends at **0.265**, close to the noise
variance `0.5² = 0.25`. **No model can beat the noise floor.** That leftover error is
*irreducible*.

| | Normal equation | Gradient descent |
|---|---|---|
| Cost | `O(d³)` to solve a d × d system | `O(n·d)` per step |
| Good for | small/medium d | huge n or d, online learning, neural nets |
| Needs scaling? | no | **yes**: badly scaled features make it zig-zag |

### Assumptions (interviewers ask)

1. **Linearity:** y depends linearly on the features (you can add transformed features).
2. **Independent errors.**
3. **Constant error variance** (homoscedasticity).
4. **No perfect multicollinearity:** otherwise `XᵀX` is singular.
5. **Normal errors:** needed only for confidence intervals and p-values, not for predictions.

Coefficient meaning: "holding the other features fixed, one more unit of `xⱼ` changes `ŷ` by
`wⱼ`". This only holds if the features aren't strongly correlated.

## 2. Metrics

`y = [100, 200, 300, 400]`, `ŷ = [110, 190, 330, 370]`:

| Metric | Formula | Value | When to use |
|---|---|---|---|
| **MAE** | mean `|y − ŷ|` | **20** | robust to outliers; easy to explain ("off by 20 on average") |
| **MSE** | mean `(y − ŷ)²` | **500** | the training loss; squared units |
| **RMSE** | `√MSE` | **22.36** | same unit as y; punishes big misses |
| **R²** | `1 − SS_res / SS_tot` | **0.96** | fraction of variance explained; unitless |

- R² = **0** for always predicting the mean, and it **can be negative** (worse than the mean).
- Make one prediction off by 500: MAE goes 20 → 142 (×7), RMSE 22.4 → 250.9 (×11).
  RMSE reacts more to the big miss.

> **Choose the metric from the business cost.** Missing a delivery by 2 hours is more than twice as
> bad as by 1 hour → RMSE. Every unit of error costs the same → MAE. Errors relative to size
> matter (selling 10 vs 1,000 units) → MAPE, or train on `log(y)`.

## 3. Polynomial features, and bias vs variance

A linear model can fit curves if you give it `x, x², x³, …` as features. 40 noisy points from
`sin(x)`:

| Degree | Train MSE | Test MSE | Diagnosis |
|---|---|---|---|
| 1 | 0.162 | 0.177 | **underfits**: high bias, a line can't bend |
| 3 | 0.021 | **0.004** | just right |
| 15 | **0.015** | 0.160 | **overfits**: high variance, it wiggles through the noise |

```
error
  │ \                                   / test error
  │  \                               /
  │   \___                      __/
  │       \____       ______/
  │            \_____/  ← sweet spot
  │ ‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾ train error keeps falling
  └──────────────────────────────────────── model complexity
     underfit (bias)          overfit (variance)
```

**Expected test error = bias² + variance + irreducible noise.**

| Symptom | Diagnosis | Fixes |
|---|---|---|
| train error high, test ≈ train | high bias | more features, more complex model, less regularisation |
| train error low, test ≫ train | high variance | more data, regularisation, simpler model, ensembles |

## 4. Regularisation: shrink weights to fight overfitting

Setup: 60 rows, **20 features, only 3 real** (`w = [4, −3, 2, 0, …, 0]`). Plain least squares
gives all 20 features non-zero weights, fitting noise.

### Ridge (L2): `minimise ‖y − Xw‖² + α‖w‖²`

Closed form: `w = (XᵀX + αI)⁻¹ Xᵀy`. Adding `αI` also makes the matrix invertible, which fixes
multicollinearity.

- Matches sklearn `Ridge` exactly.
- Shrinks `‖w‖` but sets **0** weights exactly to zero.

### Lasso (L1): `minimise (1/2n)‖y − Xw‖² + α‖w‖₁`

The absolute value has a corner at 0, so the optimum often sits **exactly** at 0:
**automatic feature selection**. There's no closed form; use **coordinate descent**, optimising
one weight at a time:

```python
rho  = X[:, j] @ (residual without feature j) / n
w[j] = soft_threshold(rho, alpha) / (X[:, j] @ X[:, j] / n)
# soft_threshold(rho, λ) = sign(rho) · max(|rho| − λ, 0)   ← snaps small values to 0
```

- Matches sklearn `Lasso` to `1e-4`.
- Kept exactly features **[0, 1, 2]** with weights `[3.43, −2.80, 1.70]`: the real ones,
  shrunk towards 0.
- The non-zero count falls as α grows: `{0.01: 19, 0.1: 7, 0.5: 3, 2.0: 2}`. At α = 2 it
  drops a real feature too, so too much regularisation means underfitting.

### Why L1 gives zeros and L2 doesn't (the picture interviewers want)

The L2 constraint `‖w‖₂ ≤ t` is a **circle**; the L1 constraint `|w₁| + |w₂| ≤ t` is a
**diamond** with corners on the axes. The loss contours usually touch the diamond at a corner,
where one weight is 0. They touch the circle at a smooth point, where both weights are non-zero.

### Elastic net: `α (ρ‖w‖₁ + (1−ρ)/2 ‖w‖₂²)`

Gets L1's sparsity plus L2's stability. With **correlated** features, lasso picks one at random;
elastic net keeps the group.

> Always **standardise** before ridge or lasso. The penalty treats all weights equally, so a
> feature in millimetres (tiny weight) would be penalised less than the same feature in km.

## 5. Non-linear regressors

| Model | Idea | Scale features? | Strengths | Weaknesses |
|---|---|---|---|---|
| **Decision tree** | split on `feature < threshold` to reduce variance; predict the leaf mean | no | interpretable, non-linear, handles interactions | overfits; step-shaped predictions |
| **Random forest** | average many deep trees, each on a bootstrap sample and random feature subsets | no | strong default, hard to overfit | big, slower to predict; can't extrapolate |
| **Gradient boosting** | add small trees one by one, each fitting the *residuals* of the current model | no | usually best on tabular data | needs tuning; sequential training |
| **XGBoost / LightGBM / CatBoost** | optimised boosting (regularisation, histograms, native categoricals) | no | Kaggle and industry workhorses | as above |
| **KNN** | average the y of the k nearest training points | **yes** | no training, simple | slow at prediction; curse of dimensionality |
| **SVR** | fit a tube of width ε; only points outside it matter; kernels for non-linearity | **yes** | strong on small/medium data | `O(n²)`+ training; tuning C, ε, γ |

`HistGradientBoostingRegressor` is scikit-learn's LightGBM-style implementation (it bins
features into histograms). XGBoost, LightGBM and CatBoost aren't installed in this course's venv:
their APIs follow the same `fit`/`predict` pattern (check their current docs).

## 6. The comparison: synthetic house prices (in thousands)

The price has non-linear parts on purpose: the location premium decays **exponentially** with
distance, and good schools add a **step** above rating 7.

| Model | Test MAE | Test RMSE | Test R² | Train R² |
|---|---|---|---|---|
| mean baseline | 172.3 | 205.6 | −0.005 | 0.000 |
| linear | 48.8 | 60.8 | 0.912 | 0.907 |
| ridge | 48.8 | 60.8 | 0.912 | 0.907 |
| KNN (k=10, scaled) | 37.9 | 48.4 | 0.944 | 0.944 |
| SVR (RBF) | 25.6 | 34.0 | 0.973 | 0.979 |
| decision tree (depth 6) | 38.4 | 47.8 | 0.946 | 0.968 |
| decision tree (no limit) | 42.3 | 53.4 | 0.932 | **1.000** |
| random forest | 28.4 | 35.2 | 0.971 | 0.996 |
| **gradient boosting** | **25.0** | **31.4** | **0.977** | 0.987 |
| hist gradient boosting | 26.9 | 34.2 | 0.972 | 0.996 |

What to read from it:

1. **Linear = ridge** here: plenty of data and few features, so there's nothing to regularise.
   Both cap at R² 0.91 because the truth isn't linear.
2. The **unlimited tree** scores train R² = 1.000 (it memorised) but is worse on test than the
   depth-6 tree. That's overfitting, measured.
3. **Averaging** 300 such trees (random forest) lifts test R² from 0.932 to 0.971: variance
   reduction (chapter 06).
4. **Gradient boosting wins**, which is normal for tabular data. A well-tuned **SVR** comes close
   on 2,000 rows; on 2 million it would be far too slow.

**Feature importances (random forest):**

```
area 0.799   dist 0.182   age 0.009   school 0.006   rooms 0.005
```

Careful: `rooms` really adds 15k per room to the price, yet it scores 0.005. Rooms is **strongly
correlated with area**, so the trees split on area and rooms has little left to explain.
Impurity-based importance spreads credit unevenly among correlated features. Use **permutation
importance** or **SHAP** for explanations, and never read importance as causation.

---

## Exercises

1. Derive `∂MSE/∂b` for `ŷ = Xw + b`.
2. Change `lr=0.1` to `lr=1.5` in `fit_gradient_descent`. What happens and why?
3. Add 10 % outliers (y × 10) to the house training set. Which metric (MAE or RMSE) and which loss
   would you switch to?
4. A tree is trained on houses up to 250 m². What does it predict for 400 m²? And linear regression?

<details><summary>Answers</summary>

1. `L = (1/n) Σ (xᵢ·w + b − yᵢ)²`, so `∂L/∂b = (2/n) Σ (xᵢ·w + b − yᵢ)`: twice the mean error.
2. It diverges (the loss reaches about 1e150 within 200 steps). The MSE's steepest curvature is
   the largest eigenvalue of `2·XbᵀXb/n` (Xb includes the column of 1s for the bias), which is
   **2.26** here. Gradient descent is stable only when `η < 2/2.26 ≈ 0.886`: `η = 0.5` converges,
   while `η = 0.9` already blows up.
3. Report MAE (and median absolute error), and train with **Huber loss** (quadratic for small
   errors, linear for big ones) or MAE loss. `HistGradientBoostingRegressor(loss="absolute_error")`
   is one option.
4. The tree predicts roughly the **largest leaf value** (~ a 250 m² price): **trees can't
   extrapolate**. Linear regression keeps the line going, which may be right or absurd. Matters for
   time series with trends.
</details>

## Interview questions

**Q: Ridge vs lasso?** Both shrink weights. Ridge (L2) shrinks smoothly and keeps all features;
good with many small effects and multicollinearity. Lasso (L1) sets some to exactly zero (feature
selection) because of the diamond-shaped constraint. Elastic net mixes them and handles correlated
groups.

**Q: Why might R² be misleading?** It always rises when you add features (use adjusted R² or a
validation set). It's scale-free, so it hides whether errors are business-acceptable. And a high R²
on a trend-heavy time series can come from the trend alone.

**Q: How does gradient boosting work?** Start with a constant (the mean). Repeat: compute the
residuals (the negative gradient of the loss), fit a small tree to them, add it scaled by a
learning rate. Each tree fixes the mistakes of the ensemble so far. Small learning rate + many
trees + early stopping generalises best.

**Q: Random forest vs gradient boosting?** RF: deep independent trees averaged → **reduces
variance**; robust, parallel, little tuning. GB: shallow trees added sequentially → **reduces
bias**; usually more accurate, but more sensitive to hyperparameters and noise.

**Q: When is linear regression still the right answer?** Small data, a need for interpretability
or coefficients, extrapolation, very fast inference, or as the baseline every other model must beat.

---

**Next:** [04 · Classification](../04-Classification/README.md)
