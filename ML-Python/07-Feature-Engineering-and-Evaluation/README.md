# 07 · Feature engineering, validation, tuning and data leakage

> Better features beat fancier models. And an honest validation setup beats both, because
> without it you can't tell which model is actually better.

Run it: `../.venv/bin/python features_and_evaluation.py` → **37/37 checks**.

---

## 1. Missing values

First ask **why** a value is missing:

| Type | Meaning | Example | Consequence |
|---|---|---|---|
| MCAR | missing completely at random | a sensor glitch | any sensible imputation is fine |
| MAR | missingness depends on *other* observed features | older people skip the "app usage" question | model-based imputation using those features |
| **MNAR** | missingness depends on the **value itself** | rich people skip the income question | **the fact that it's missing is a signal**: add an indicator |

**Imputing 30 % hidden incomes** (income ≈ 1000 × age + noise):

| Method | RMSE on the hidden values |
|---|---|
| mean | 15,393 |
| median | 15,394 |
| **iterative** (predicts income from age) | **5,016** |

**MNAR demo** (high earners usually skip income; predict "income > 50k"):

| | AUC |
|---|---|
| median imputation only | 0.985 |
| median + `add_indicator=True` | **1.000**: the "was missing" column says "probably rich" |
| `HistGradientBoosting` on the raw NaNs, no imputer | 1.000 (trees learn a default direction for missing values) |

> **Rules:** fit imputers on training data only (inside the `Pipeline`); keep a missing indicator
> when missingness might mean something; never impute the **target**.

## 2. Encoding categorical features

| Encoder | Output for `city ∈ {Delhi, Goa, Pune}` | Use for |
|---|---|---|
| **One-hot** | `Pune → [0, 0, 1]` | few categories, linear models, neural nets |
| **Ordinal** | `S, M, L → 0, 1, 2` | categories with a real order |
| **Target encoding** | each category → its (smoothed) mean target | **high cardinality** (zip codes, merchant IDs) |
| Hashing | category → hash bucket | huge or unbounded vocabularies, streaming |
| Embeddings | learned dense vector | deep learning, recommenders (chapter 13) |

`OneHotEncoder(handle_unknown="ignore")` turns an unseen category (Mumbai) into all zeros instead
of crashing in production.

### Target encoding leaks, unless you cross-fit

1,000 merchant IDs, plus a **pure-noise ID column** that has nothing to do with fraud:

| Encoding of the **noise** ID | Train AUC | Test AUC |
|---|---|---|
| **naive**: per-category mean of y computed on all training rows | **0.821** | 0.497 |
| **cross-fitted** `TargetEncoder` (each row encoded by folds that exclude it, plus smoothing) | 0.537 | |

Naively, each row's own label is baked into its feature. Categories with 1–5 rows get a mean
that is basically their own label, so the model "learns" noise that vanishes on test.
Cross-fitting removes the fake signal. On the **real** merchant ID, the cross-fitted encoding still
gives test AUC **0.645**, from 1 column instead of 1,000 one-hot columns.

## 3. Scaling: who cares?

Feature 0 multiplied by 1,000 (think "metres vs millimetres"):

| Model | Raw | Scaled |
|---|---|---|
| KNN | **0.573** | **0.940** |
| Decision tree | 0.920 | 0.920 (identical) |

| Needs scaling | Doesn't |
|---|---|
| KNN, K-Means, SVM, PCA, linear/logistic with regularisation, neural nets, anything using gradient descent | trees, random forests, gradient boosting (they split on order, not distance) |

**Which scaler** (for `x = [1, 2, 3, 4, 1000]`):

| Scaler | Behaviour |
|---|---|
| `StandardScaler` (mean/std) | the outlier inflates the std, so the four normal values squash into a range < 0.1 |
| `MinMaxScaler` (to [0, 1]) | same problem: 4 maps to 0.003 |
| **`RobustScaler`** (median/IQR) | **`[−1, −0.5, 0, 0.5]`**: the normal values stay spread out |

## 4. Creating features (where domain knowledge pays)

**Interactions: a linear model can't invent them.** Customers churn when their plan doesn't fit:
a heavy user on the basic plan, or a light user on premium. That's an XOR.

| Features | Logistic regression AUC |
|---|---|
| usage, plan | **0.497**: no straight line separates an XOR |
| + `usage × plan` (plan coded ±1) | **0.899** |

(A tree model would find the interaction itself by splitting on both features. Engineered features
matter most for linear models and when data is limited.)

**Useful feature types:**

- **Ratios and differences:** debt / income, price per m², spend this week − last week.
- **Aggregations:** a user's mean transaction amount, count in the last 24 h (watch for time
  leakage, §7d).
- **Dates:** day-of-week (`2026-01-05` → Monday = 0), hour, `is_weekend`, holidays, days since
  last event.
- **Cyclical encoding:** `hour → (sin(2πh/24), cos(2πh/24))`, so 23:00 and 00:00 end up
  neighbours, not 23 apart.
- **Transforms:** `log1p` for skewed money values; binning; polynomial terms.
- **Text and images:** TF-IDF and embeddings (chapters 10–11).

## 5. Cross-validation: the right split for your data

| Splitter | Use when | From the run |
|---|---|---|
| `KFold` | i.i.d. rows, balanced classes | without shuffling, positives per fold were `[14, 6, 10, 11, 12]` |
| **`StratifiedKFold`** | classification, especially imbalanced | `[10, 10, 11, 11, 11]` |
| **`GroupKFold`** | several rows per user/patient/device | see below |
| **`TimeSeriesSplit`** | anything with time order | train sizes `[4, 8, 12, 16]`, always past → future |

5-fold AUC = **0.918 ± 0.036**. Report the spread; a 0.005 "improvement" inside ±0.036 noise is
not an improvement.

**Group leakage, measured:** 300 patients × 10 rows each. Each patient has a unique signature in
the features, and the label is per patient but **unrelated** to the features.

| Split | Accuracy |
|---|---|
| shuffled `KFold` | **0.999**: it recognises the patient, not the disease |
| `GroupKFold` | **0.548**, close to the 0.513 majority-class rate: the truth |

## 6. Hyperparameter tuning

| Method | Idea | Run |
|---|---|---|
| Grid search | try every combination | 9 combos: best `max_depth=None, min_samples_leaf=1`, CV AUC 0.967 |
| **Random search** | sample combinations | 9 draws from a 928-combo space: 0.966 |
| Bayesian optimisation (Optuna, etc.) | model the score surface, try promising areas next | best for expensive models |
| Successive halving / Hyperband | give many configs a small budget, keep the best | `HalvingGridSearchCV` |

Random search matched the grid on the same budget while searching a space 100 times bigger.
It works because usually only 1–2 hyperparameters matter, and a grid wastes its points repeating
the unimportant ones.

**Early stopping:** `HistGradientBoosting` with `max_iter=2000` stopped after **86** trees when the
validation loss hadn't improved for 20 rounds (test AUC 0.978). It's free regularisation and saves
time. Neural networks use the same trick (chapter 09).

### Selection bias: the best CV score is optimistic

On **pure noise** (true accuracy 0.5), tuning 90 tree configs:

| Estimate | Accuracy |
|---|---|
| best inner-CV score | **0.608**: looks like skill |
| **nested CV** (tune inside each outer fold, score on the outer fold) | 0.575: lower, more honest |

Try enough configs and one will get lucky on your folds (the same multiple-testing problem as in
chapter 02). Fixes: **nested CV**, or a final **test set touched exactly once**. With only 120 rows,
even the nested estimate is noisy, which is another reason to get more data.

## 7. Data leakage: five ways to fool yourself

**Leakage = information at training or validation time that won't exist at prediction time.** It
makes offline metrics look great and production fail.

| | Leak | Offline score | Honest score |
|---|---|---|---|
| a | **feature selection before CV**: pick 20 of 5,000 pure-noise features using all labels, then cross-validate | **0.850** accuracy | **0.540** with selection inside the pipeline |
| b | **target leakage**: "called to cancel" is recorded *because* the customer churned | AUC **0.986** | 0.859 without it |
| c | **preprocessing on all rows**: scaler fit on train + test | drifted test feature looks like **0.83** std from normal | **1.49** with a train-only fit: the leak hid half the drift |
| d | **time leakage**: shuffled split on a random walk, predicting from the date | R² **0.992** | R² **−3.359** with `TimeSeriesSplit`: it can't forecast at all |
| e | **duplicates** across train and test (every row twice) | tree accuracy **0.938** | 0.732 deduplicated |

How each one fools you:

- **(a)** With 5,000 noise features and 100 rows, some correlate with the labels by chance.
  Selecting them on all rows means the test folds helped pick them.
- **(d)** The shuffled model just interpolates between yesterday and tomorrow, both of which it
  has seen.

**Leakage checklist (use it in every project and every interview design answer):**

1. Split **first**. Every `fit` (imputer, scaler, encoder, selector, model) goes inside a
   `Pipeline` and runs on training folds only.
2. For each feature, ask: **would I know this at prediction time?** Check timestamps.
3. Time-ordered data → time-based splits; features computed only from data **before** the
   prediction time ("point-in-time correct" joins).
4. Several rows per entity → group splits.
5. Deduplicate (including near-duplicates) **before** splitting.
6. A score that's too good to be true usually is. Inspect the top feature importances.

---

## Exercises

1. You predict hospital readmission and the top feature is "discharge_disposition = transferred
   to hospice". Leak or not?
2. Why can't you use `StandardScaler` statistics from the whole dataset, even if the effect
   seems tiny?
3. Model A beats model B by 0.004 AUC; the 5-fold std is 0.03. Ship A?
4. A user-level churn model has 50 sessions per user. Which CV splitter?

<details><summary>Answers</summary>

1. It depends on **when** the prediction is made. If you predict at discharge, the disposition
   is known, so it's legitimate (if obvious). If you predict at admission, it's a leak, because
   it's decided later. Time of prediction defines leakage.
2. The validation estimate must simulate the future, when unseen data arrives and you only have
   training statistics. With drift or small data the effect isn't tiny (§7c hid half the drift),
   and it's the same habit that causes the big leaks.
3. No evidence A is better: the difference is far inside the noise. Use paired comparisons on the
   same folds, repeated CV, or a significance test, and weigh cost, latency and simplicity.
4. `GroupKFold` (or `StratifiedGroupKFold`) grouped by user, and time-ordered if the churn date
   matters.
</details>

## Interview questions

**Q: What is data leakage? Give examples.** Information available during training that won't be
available at prediction time, which inflates offline metrics: target leakage (post-outcome
features), train/test contamination (fitting preprocessing or feature selection on all data),
temporal leakage (random splits on time series, future data in aggregates), group leakage (the
same user in train and test), duplicates.

**Q: How do you choose a validation strategy?** Mirror production. If you'll predict future
transactions, validate on later time periods. If you'll score new users, hold out whole users.
Stratify rare classes. Keep a final untouched test set.

**Q: Grid vs random search?** Same budget, but random search covers more distinct values of the
hyperparameters that matter. For expensive models use Bayesian optimisation or Hyperband, with
early stopping.

**Q: How do you handle high-cardinality categoricals?** Cross-fitted, smoothed target encoding;
hashing; frequency encoding; learned embeddings; or CatBoost's ordered target statistics. Avoid
one-hot with thousands of levels for linear models, and never target-encode using the row's own
label.

---

**Next:** [08 · Neural networks from scratch](../08-Neural-Networks-from-Scratch/README.md)
