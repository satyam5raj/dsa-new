# 01 · Python and data fundamentals

> Most of an ML engineer's time goes on **getting data into shape**, not on models.
> This chapter covers the five tools you use every day: Python, NumPy, pandas, SQL and scikit-learn.

Run it: `../.venv/bin/python data_fundamentals.py` → **28/28 checks**.

---

## 1. Python essentials you will actually use

| Feature | Why ML code uses it |
|---|---|
| **Generators** (`yield`) | Stream a 50 GB file batch by batch instead of loading it into RAM. PyTorch `DataLoader`s work the same way |
| **List/dict comprehensions** | Short, readable data transforms |
| **`@dataclass`** | Config objects and records with a free `__init__` and `__repr__` |
| **Type hints** | Pipelines get long; hints catch "passed a DataFrame where an array was expected" early |
| **Exceptions** | One bad row shouldn't kill a 6-hour job: catch, log, continue |

```python
def read_batches(n_rows, batch_size):
    for start in range(0, n_rows, batch_size):
        yield list(range(start, min(start + batch_size, n_rows)))
```

`next(batches)` gives `[0, 1, 2, 3]`. Nothing else has been computed yet. `list(batches)` pulls the
rest: `[[4, 5, 6, 7], [8, 9]]`. **Lazy** means the work happens only when someone asks.

## 2. NumPy: think in arrays, not loops

### 2.1 Vectorisation

A Python `for` loop handles one number at a time, and each step is slow interpreter work.
NumPy hands the whole array to compiled C/BLAS code in one call.

From the run: sum of squares of 1 million numbers took **~48 ms** in a Python loop and **0.16 ms**
with `np.dot(x, x)`. That's about 300 times faster. (The exact times vary by machine; the check
only asks for more than 20 times.)

> **Rule:** if you write `for i in range(len(array))` in ML code, there's probably a vectorised
> one-liner.

### 2.2 Shapes and broadcasting

Data is a matrix `X` of shape `(n_samples, n_features)`: **rows = examples, columns = features**.

**Broadcasting** lets you combine arrays of different shapes by "stretching" the smaller one:

```
data (3,2)          data.mean(axis=0)  (2,)  → treated as (1,2) → repeated to (3,2)
[[1, 200],          [2, 300]
 [2, 300],
 [3, 400]]
```

`(data - data.mean(axis=0)) / data.std(axis=0)` standardises **every column at once**. The result
has column means `[0, 0]` and standard deviations `[1, 1]`.

Broadcasting rule: compare shapes **from the right**. Two dimensions are compatible if they are
equal or one of them is 1.

| Shapes | Result |
|---|---|
| `(3,2)` and `(2,)` | `(3,2)` ✓ |
| `(3,2)` and `(3,1)` | `(3,2)` ✓ (per-row operation) |
| `(3,2)` and `(3,)` | **error**: 2 ≠ 3. A classic bug. Use `x[:, None]` to get `(3,1)` |

**Matrix multiply:** `(3,2) @ (2,4) → (3,4)`. The inner dimensions must match.
A linear model on all rows at once is `X @ w`: with `w = [0.5, -1]` you get `[-199.5, -299, -398.5]`.

### 2.3 Masks and `np.where`

```python
ages[ages >= 18]                         # [25, 37, 61]   boolean indexing
np.where(ages >= 18, "adult", "child")   # vectorised if/else
```

## 3. pandas: cleaning a messy table

The script generates 1,000 customers with planted problems: **80 missing ages, 30 missing incomes,
one income of 9,999,999** (a typo-style outlier), and a `churned` label.

### 3.1 The cleaning checklist

1. `df.shape`, `df.dtypes`, `df.head()`: what do I have?
2. `df.isna().sum()`: where are the holes? (found `80, 30`)
3. Decide per column: **drop**, **impute** (median for skewed numbers, mode for categories), or
   **flag**. The script does both: `age_missing` remembers *that* a value was missing (often
   predictive in itself), then the median fills it.
4. Look for impossible values (negative ages, a 9,999,999 income).
5. Fix types (dates as `datetime`, categories as `category`).

### 3.2 groupby: split → apply → combine

```python
df.groupby("plan")["churned"].mean()
# basic 0.312, pro 0.193, enterprise 0.111
```

One line answers a business question: *basic-plan customers churn almost 3 times as often as
enterprise ones.* That is EDA producing a feature idea.

### 3.3 Joins (the source of many silent bugs)

| Join | Keeps | Here |
|---|---|---|
| `inner` | only keys in **both** tables | **3** rows (customer 1 twice, customer 2 once; order for unknown 5000 dropped) |
| `left` | every left row, NaN where no match | **1001** rows: customer 1 has two orders, so it **duplicates** |

> **Pitfall:** after a join, always check the row count. A one-to-many join silently multiplies
> rows, and then your "1,000 customers" model trains on 1,001 rows with one customer counted twice.
> Use `merge(..., validate="one_to_one")` to make pandas raise instead.

(I first wrote the unknown order as customer `999`, but IDs run 1–1000, so it matched. The check
caught it. Exactly the kind of mistake the pitfall describes.)

`pivot_table(index="plan", columns="country", ...)` reshapes long data into a `3 × 4` grid.

## 4. SQL: window functions

Interviewers love these, and feature pipelines are full of them. A window function computes over
a group of rows **without collapsing them** (unlike `GROUP BY`).

```sql
SUM(spend) OVER (PARTITION BY user ORDER BY day)       AS running_total  -- 10, 40, 60 | 5, 55
RANK()     OVER (PARTITION BY user ORDER BY spend DESC) AS rank_in_user  -- 3, 1, 2    | 2, 1
```

- `PARTITION BY` = "restart for each user".
- `ORDER BY` inside `OVER` = the order the running total accumulates in.
- `GROUP BY user HAVING total > 56` filters **after** aggregation (`WHERE` filters before). Result: `[('a', 60)]`.

Typical ML features from SQL: "spend in the last 7 days" (`ROWS BETWEEN 6 PRECEDING AND CURRENT ROW`),
"days since last order" (`LAG`), "rank of this transaction amount for the user".

## 5. Exploratory data analysis (EDA)

| Question | Tool | Result here |
|---|---|---|
| Is it skewed? | mean vs median | median **48,281**, mean **65,019**: right-skewed (incomes usually are) |
| Outliers? | IQR rule: above `Q3 + 1.5·IQR` | the planted 9,999,999 is flagged |
| Which features relate to the target? | correlation | `corr(support_tickets, churned)` = **0.37** |

> When mean ≫ median, use the **median** for imputation and consider a **log transform**:
> one billionaire shouldn't move everybody's "typical income".

Correlation only measures **linear** relationships and says nothing about causation. More tickets
might cause churn, or unhappy customers might do both.

## 6. scikit-learn: the baseline workflow

```
raw DataFrame
   │ train_test_split(stratify=y)       ← split FIRST, before any fitting
   ▼
ColumnTransformer
   ├─ numeric:     SimpleImputer(median) → StandardScaler
   └─ categorical: OneHotEncoder(handle_unknown="ignore")
   ▼
LogisticRegression
```

Put everything in one `Pipeline`:

- `model.fit(X_train, y_train)` learns the imputer medians, scaler means and model weights **from
  the training set only**. That prevents **data leakage** (chapter 07).
- The same object transforms test or production data identically. No train/serve skew.
- One-hot encoding made **7** columns (3 plans + 4 countries).

### Always compare to a dumb baseline

| Model | Accuracy | ROC-AUC |
|---|---|---|
| `DummyClassifier` (always "no churn") | **0.740** | 0.5 |
| Logistic regression | **0.755** | **0.737** |

The accuracy gap looks tiny because 74 % of customers don't churn: predicting "no" for everyone
already scores 0.74. **ROC-AUC** (chapter 04) shows the model really does rank churners above
non-churners. Lesson: **on imbalanced data, accuracy lies.**

---

## Exercises

1. Make `(data - data.mean(axis=1))` work on the `(3,2)` array (subtract each **row's** mean).
2. Write SQL for each user's spend on the **previous** day (`NULL` for the first day).
3. Remove `stratify=y` from the split and re-run with several `random_state`s. What happens to the
   churn-rate difference between train and test?
4. Add `log_income = np.log1p(income)` as a feature. Does ROC-AUC change? Why might it for a linear
   model but not for a tree?

<details><summary>Answers</summary>

1. `data - data.mean(axis=1, keepdims=True)` (or `[:, None]`). The mean has shape `(3,)`, which
   broadcasting lines up against the **last** axis (size 2) and fails. `keepdims` gives `(3,1)`.
2. `LAG(spend) OVER (PARTITION BY user ORDER BY day)`.
3. The rates drift apart by a percent or two depending on the seed. With rare classes (fraud at
   0.1 %) an unstratified test set can end up with almost no positives.
4. A linear model sees income's effect as a straight line, so the outlier dominates the scale;
   the log compresses it. Trees split on thresholds, which any monotonic transform preserves, so
   trees are unaffected.
</details>

## Interview questions

**Q: `apply` vs vectorised operations in pandas?**
`apply` with a Python function runs row by row in the interpreter (slow). Vectorised column
operations (`df.a * df.b`, `.str`, `.dt`, `np.where`) run in C. Use `apply` only when nothing
vectorised exists.

**Q: How do you handle missing values?**
First ask *why* they're missing: at random, or meaningful (no income reported → unemployed)?
Options: drop (if rare), impute (median, mode, model-based), add a missing-flag column, or use a
model that handles NaN natively (gradient-boosted trees). Always fit the imputer on the training
set only.

**Q: `WHERE` vs `HAVING`?** `WHERE` filters rows before grouping; `HAVING` filters groups after
aggregation.

**Q: Why put preprocessing inside a `Pipeline`?** It fits on the training fold only (no leakage
during cross-validation), and you ship one object, so production transforms match training.

---

**Next:** [02 · Maths for ML](../02-Math-for-ML/README.md)
