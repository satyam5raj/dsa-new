"""
ML 01 : PYTHON AND DATA FUNDAMENTALS
Run:  ../.venv/bin/python data_fundamentals.py        (explanations: README.md)

  1. Python essentials   generators, dataclasses, type hints, exceptions
  2. NumPy               vectorisation, broadcasting, boolean masks, matrix maths
  3. pandas              a messy dataset: missing values, outliers, groupby, joins
  4. SQL                 window functions with the built-in sqlite3
  5. EDA                 summary stats, correlation, IQR outliers
  6. scikit-learn        split -> pipeline -> baseline -> evaluate
"""
import sqlite3
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Iterator

import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.dummy import DummyClassifier
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, roc_auc_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from mlcheck import check, near, section, show, summary  # noqa: E402

RNG = np.random.default_rng(0)


# ---------------------------------------------------------------------------
# 1. Python essentials
# ---------------------------------------------------------------------------
@dataclass
class Transaction:
    user_id: int
    amount: float
    country: str


def read_batches(n_rows: int, batch_size: int) -> Iterator[list]:
    """A generator: yields one batch at a time instead of loading everything (how big datasets are streamed)."""
    for start in range(0, n_rows, batch_size):
        yield list(range(start, min(start + batch_size, n_rows)))


def safe_ratio(a: float, b: float) -> float:
    try:
        return a / b
    except ZeroDivisionError:
        return float("nan")


def python_essentials():
    section("1. Python essentials")
    batches = read_batches(10, 4)
    check("a generator yields lazily: first batch", [0, 1, 2, 3], next(batches))
    check("...and the rest on demand", [[4, 5, 6, 7], [8, 9]], list(batches))
    t = Transaction(user_id=7, amount=19.99, country="IN")
    check("dataclass gives a readable repr for free", "Transaction(user_id=7, amount=19.99, country='IN')", repr(t))
    check("handled exception returns NaN instead of crashing", "nan", str(safe_ratio(1, 0)))
    squares = [x * x for x in range(6) if x % 2 == 0]
    check("list comprehension with a filter", [0, 4, 16], squares)


# ---------------------------------------------------------------------------
# 2. NumPy
# ---------------------------------------------------------------------------
def numpy_basics():
    section("2. NumPy")
    x = RNG.normal(size=1_000_000)
    t0 = time.perf_counter()
    loop_sum = 0.0
    for v in x[:200_000]:                       # pure-Python loop over 200K values
        loop_sum += v * v
    loop_time = (time.perf_counter() - t0) * 5  # scaled to 1M values
    t0 = time.perf_counter()
    vec_sum = float(np.dot(x, x))               # one vectorised call over 1M values
    vec_time = time.perf_counter() - t0
    show(f"sum of squares of 1M numbers: python loop ~{loop_time*1000:.0f} ms, numpy {vec_time*1000:.2f} ms")
    check("vectorised NumPy is >20x faster than a Python loop", True, loop_time > 20 * vec_time)

    data = np.array([[1.0, 200.0], [2.0, 300.0], [3.0, 400.0]])   # rows = samples, cols = features
    standardised = (data - data.mean(axis=0)) / data.std(axis=0)  # broadcasting: (3,2) - (2,)
    check("broadcasting standardises each column: means", "[0. 0.]", np.round(standardised.mean(axis=0), 6) + 0.0)
    check("...and standard deviations", "[1. 1.]", np.round(standardised.std(axis=0), 6))
    check("shape rules: (3,2) @ (2,4) -> ", (3, 4), (data @ np.ones((2, 4))).shape)
    ages = np.array([12, 25, 37, 8, 61])
    check("boolean mask selects adults", [25, 37, 61], ages[ages >= 18].tolist())
    check("np.where for vectorised if/else", ["child", "adult", "adult", "child", "adult"], np.where(ages >= 18, "adult", "child").tolist())
    w = np.array([0.5, -1.0])
    check("a linear model on all rows at once: X @ w", [-199.5, -299.0, -398.5], (data @ w).tolist())


# ---------------------------------------------------------------------------
# 3. pandas on a messy dataset
# ---------------------------------------------------------------------------
def make_customers(n=1000) -> pd.DataFrame:
    rng = np.random.default_rng(1)
    df = pd.DataFrame({
        "customer_id": np.arange(1, n + 1),
        "age": rng.integers(18, 75, n).astype(float),
        "income": rng.lognormal(10.8, 0.5, n).round(0),
        "plan": rng.choice(["basic", "pro", "enterprise"], n, p=[0.6, 0.3, 0.1]),
        "country": rng.choice(["IN", "US", "UK", "DE"], n),
        "support_tickets": rng.poisson(1.5, n),
    })
    logit = -2.0 + 0.6 * df["support_tickets"] - 0.00001 * df["income"] + (df["plan"] == "basic") * 0.8
    df["churned"] = (rng.random(n) < 1 / (1 + np.exp(-logit))).astype(int)
    df.loc[rng.choice(n, 80, replace=False), "age"] = np.nan          # missing values
    df.loc[rng.choice(n, 30, replace=False), "income"] = np.nan
    df.loc[5, "income"] = 9_999_999                                   # an obvious data-entry outlier
    return df


def pandas_basics():
    section("3. pandas")
    df = make_customers()
    check("shape (rows, columns)", (1000, 7), df.shape)
    missing = df.isna().sum()
    check("missing values per column (age, income)", "80, 30", f"{missing['age']}, {missing['income']}")
    df["age_missing"] = df["age"].isna().astype(int)                    # keep the fact that it WAS missing
    df["age"] = df["age"].fillna(df["age"].median())
    check("after median imputation: no missing ages", 0, int(df["age"].isna().sum()))
    by_plan = df.groupby("plan")["churned"].mean().round(3)
    show(f"churn rate by plan: {by_plan.to_dict()}")
    check("basic plan churns more than enterprise", True, by_plan["basic"] > by_plan["enterprise"])
    orders = pd.DataFrame({"customer_id": [1, 1, 2, 5000], "amount": [50.0, 20.0, 35.0, 10.0]})  # 5000 = unknown customer
    left = df[["customer_id", "plan"]].merge(orders, on="customer_id", how="left")
    inner = df[["customer_id", "plan"]].merge(orders, on="customer_id", how="inner")
    check("inner join keeps only matching customers (1 has 2 orders)", 3, len(inner))
    check("left join keeps every customer (customer 1 appears twice)", 1001, len(left))
    pivot = df.pivot_table(index="plan", columns="country", values="churned", aggfunc="count")
    check("pivot table: plans x countries", (3, 4), pivot.shape)


# ---------------------------------------------------------------------------
# 4. SQL window functions
# ---------------------------------------------------------------------------
def sql_basics():
    section("4. SQL (window functions)")
    con = sqlite3.connect(":memory:")
    pd.DataFrame({"user": ["a", "a", "a", "b", "b"], "day": [1, 2, 3, 1, 2], "spend": [10, 30, 20, 5, 50]}).to_sql("events", con, index=False)
    q = """SELECT user, day, spend,
                  SUM(spend) OVER (PARTITION BY user ORDER BY day) AS running_total,
                  RANK()     OVER (PARTITION BY user ORDER BY spend DESC) AS rank_in_user
           FROM events ORDER BY user, day"""
    out = pd.read_sql(q, con)
    check("running total per user", [10, 40, 60, 5, 55], out["running_total"].tolist())
    check("rank of each day's spend within its user", [3, 1, 2, 2, 1], out["rank_in_user"].tolist())
    top = pd.read_sql("SELECT user, SUM(spend) AS total FROM events GROUP BY user HAVING total > 56", con)
    check("GROUP BY + HAVING", "[('a', 60)]", str(list(top.itertuples(index=False, name=None))))


# ---------------------------------------------------------------------------
# 5. exploratory data analysis
# ---------------------------------------------------------------------------
def eda():
    section("5. Exploratory data analysis")
    df = make_customers()
    inc = df["income"].dropna()
    q1, q3 = inc.quantile([0.25, 0.75])
    fence = q3 + 1.5 * (q3 - q1)
    outliers = inc[inc > fence]
    check("the planted 9,999,999 income is flagged by the IQR rule", True, 9_999_999 in outliers.values)
    show(f"income: median {inc.median():,.0f}, mean {inc.mean():,.0f} (the mean is pulled up by outliers)")
    check("skewed data: mean > median", True, inc.mean() > inc.median())
    corr = df[["support_tickets", "churned"]].corr().iloc[0, 1]
    near("correlation(support tickets, churn) is positive", 0.37, corr, 0.05)


# ---------------------------------------------------------------------------
# 6. a scikit-learn baseline
# ---------------------------------------------------------------------------
def sklearn_baseline():
    section("6. scikit-learn: split -> pipeline -> baseline")
    df = make_customers()
    X, y = df.drop(columns=["churned", "customer_id"]), df["churned"]
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, stratify=y, random_state=42)
    check("stratified split keeps the churn rate similar", True, abs(y_train.mean() - y_test.mean()) < 0.01)
    numeric, categorical = ["age", "income", "support_tickets"], ["plan", "country"]
    pre = ColumnTransformer([
        ("num", Pipeline([("impute", SimpleImputer(strategy="median")), ("scale", StandardScaler())]), numeric),
        ("cat", OneHotEncoder(handle_unknown="ignore"), categorical),
    ])
    model = Pipeline([("prep", pre), ("clf", LogisticRegression(max_iter=1000))])
    model.fit(X_train, y_train)                                       # imputer/scaler learn from TRAIN only
    dummy = DummyClassifier(strategy="most_frequent").fit(X_train, y_train)
    acc_model, acc_dummy = accuracy_score(y_test, model.predict(X_test)), accuracy_score(y_test, dummy.predict(X_test))
    auc = roc_auc_score(y_test, model.predict_proba(X_test)[:, 1])
    show(f"accuracy: always-'no churn' baseline {acc_dummy:.3f}, logistic regression {acc_model:.3f}; ROC-AUC {auc:.3f}")
    check("the model beats the dumb baseline on ROC-AUC (dummy = 0.5)", True, auc > 0.6)
    check("one-hot created columns for every plan and country", 7, len(model.named_steps["prep"].named_transformers_["cat"].get_feature_names_out()))


if __name__ == "__main__":
    python_essentials()
    numpy_basics()
    pandas_basics()
    sql_basics()
    eda()
    sklearn_baseline()
    summary()
