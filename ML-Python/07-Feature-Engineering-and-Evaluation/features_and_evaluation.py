"""
ML 07 : FEATURE ENGINEERING, VALIDATION, TUNING AND DATA LEAKAGE
Run:  ../.venv/bin/python features_and_evaluation.py        (explanations: README.md)

  1. Missing values      mean/median/indicator/iterative imputation
  2. Encoding            one-hot, ordinal, target encoding (and how it leaks)
  3. Scaling             which models care
  4. Feature creation    ratios, logs, dates, interactions
  5. Cross-validation    k-fold, stratified, group, time-series
  6. Tuning              grid vs random search, early stopping, nested CV
  7. Data leakage        five ways to fool yourself, measured
"""
import sys
import warnings
from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.datasets import make_classification
from sklearn.ensemble import HistGradientBoostingClassifier, RandomForestClassifier, RandomForestRegressor
from sklearn.experimental import enable_iterative_imputer  # noqa: F401
from sklearn.feature_selection import SelectKBest, f_classif
from sklearn.impute import IterativeImputer, SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import roc_auc_score
from sklearn.model_selection import (GridSearchCV, GroupKFold, KFold, RandomizedSearchCV, StratifiedKFold,
                                     TimeSeriesSplit, cross_val_score, train_test_split)
from sklearn.neighbors import KNeighborsClassifier
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import MinMaxScaler, OneHotEncoder, OrdinalEncoder, RobustScaler, StandardScaler, TargetEncoder
from sklearn.tree import DecisionTreeClassifier

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from mlcheck import check, near, section, show, summary  # noqa: E402

warnings.filterwarnings("ignore", category=UserWarning)


# ---------------------------------------------------------------------------
# 1. missing values
# ---------------------------------------------------------------------------
def missing_values():
    section("1. Missing values")
    rng = np.random.default_rng(0)
    n = 2000
    age = rng.uniform(20, 70, n)
    income = 1000 * age + rng.normal(0, 5000, n)                  # income strongly related to age
    X = np.column_stack([age, income])
    X_miss = X.copy()
    mask = rng.random(n) < 0.3
    X_miss[mask, 1] = np.nan                                      # 30% of incomes missing (at random)
    true = X[mask, 1]
    err = {}
    for name, imp in (("mean", SimpleImputer(strategy="mean")), ("median", SimpleImputer(strategy="median")),
                      ("iterative (uses age)", IterativeImputer(random_state=0))):
        err[name] = float(np.sqrt(np.mean((imp.fit_transform(X_miss)[mask, 1] - true) ** 2)))
    show("imputation RMSE on the hidden incomes: " + ", ".join(f"{k} {v:,.0f}" for k, v in err.items()))
    check("model-based imputation beats mean/median when features are related", True, err["iterative (uses age)"] < 0.5 * err["mean"])
    ind = SimpleImputer(strategy="median", add_indicator=True).fit_transform(X_miss)
    check("add_indicator=True appends a 'was missing' column", (n, 3), ind.shape)
    # missing NOT at random: rich people skip the income question -> missingness itself is a signal
    y = (income > 50000).astype(int)
    mnar = X.copy()
    mnar[(income > 50000) & (rng.random(n) < 0.8), 1] = np.nan
    a, b, c, d = train_test_split(mnar, y, test_size=0.3, random_state=0)
    plain = make_pipeline(SimpleImputer(strategy="median"), LogisticRegression(max_iter=1000)).fit(a, c)
    flag = make_pipeline(SimpleImputer(strategy="median", add_indicator=True), LogisticRegression(max_iter=1000)).fit(a, c)
    auc_p, auc_f = roc_auc_score(d, plain.predict_proba(b)[:, 1]), roc_auc_score(d, flag.predict_proba(b)[:, 1])
    show(f"missing-not-at-random: AUC without indicator {auc_p:.3f}, with indicator {auc_f:.3f}")
    check("the missing-indicator captures informative missingness", True, auc_f > auc_p + 0.01)
    hgb = HistGradientBoostingClassifier(random_state=0).fit(a, c)            # handles NaN natively
    show(f"HistGradientBoosting on raw NaNs (no imputer): AUC {roc_auc_score(d, hgb.predict_proba(b)[:, 1]):.3f}")


# ---------------------------------------------------------------------------
# 2. encoding categoricals
# ---------------------------------------------------------------------------
def encoding():
    section("2. Encoding categorical features")
    df = pd.DataFrame({"size": ["S", "M", "L", "M"], "city": ["Pune", "Delhi", "Pune", "Goa"]})
    oh = OneHotEncoder(sparse_output=False).fit(df[["city"]])
    check("one-hot: one column per category", ["city_Delhi", "city_Goa", "city_Pune"], oh.get_feature_names_out().tolist())
    check("one-hot row for 'Pune'", [0.0, 0.0, 1.0], oh.transform(pd.DataFrame({"city": ["Pune"]}))[0].tolist())
    unseen = OneHotEncoder(handle_unknown="ignore", sparse_output=False).fit(df[["city"]])
    check("an unseen category (Mumbai) becomes all zeros, not a crash", [0.0, 0.0, 0.0], unseen.transform(pd.DataFrame({"city": ["Mumbai"]}))[0].tolist())
    ordinal = OrdinalEncoder(categories=[["S", "M", "L"]]).fit_transform(df[["size"]]).ravel()
    check("ordinal encoding keeps the order S < M < L", [0.0, 1.0, 2.0, 1.0], ordinal.tolist())

    # high-cardinality: 1000 merchant ids; target encoding replaces each id by its (smoothed) fraud rate
    rng = np.random.default_rng(1)
    n, k = 20000, 1000
    merchant = rng.integers(0, k, n)
    risk = rng.beta(1, 20, k)                                     # each merchant has its own fraud rate
    y = (rng.random(n) < risk[merchant]).astype(int)
    noise_ids = rng.integers(0, k, n)                             # a feature with NO relation to y
    Xdf = pd.DataFrame({"merchant": merchant.astype(str), "noise_id": noise_ids.astype(str)})
    a, b, c, d = train_test_split(Xdf, y, test_size=0.3, random_state=0)

    # naive target encoding: mean of y per category computed on the training set, then used to train
    naive_map = pd.Series(c).groupby(a["noise_id"].values).mean()
    naive_tr = a["noise_id"].map(naive_map).values[:, None]
    naive_te = b["noise_id"].map(naive_map).fillna(c.mean()).values[:, None]
    tree = DecisionTreeClassifier(max_depth=4, random_state=0).fit(naive_tr, c)
    auc_naive_train = roc_auc_score(c, tree.predict_proba(naive_tr)[:, 1])
    auc_naive_test = roc_auc_score(d, tree.predict_proba(naive_te)[:, 1])
    show(f"NAIVE target encoding of a pure-noise id: train AUC {auc_naive_train:.3f}, test AUC {auc_naive_test:.3f}")
    check("naive target encoding leaks: noise looks predictive on train", True, auc_naive_train > 0.6)
    check("...and the illusion vanishes on test", True, auc_naive_test < 0.55)
    te = TargetEncoder(random_state=0)                            # cross-fitted (out-of-fold) + smoothed
    te_tr = te.fit_transform(a[["noise_id"]], c)
    tree2 = DecisionTreeClassifier(max_depth=4, random_state=0).fit(te_tr, c)
    show(f"cross-fitted TargetEncoder on the same noise id: train AUC {roc_auc_score(c, tree2.predict_proba(te_tr)[:, 1]):.3f}")
    check("cross-fitting removes the fake signal", True, roc_auc_score(c, tree2.predict_proba(te_tr)[:, 1]) < 0.56)
    te_m = TargetEncoder(random_state=0)
    m_tr = te_m.fit_transform(a[["merchant"]], c)
    lr = LogisticRegression().fit(m_tr, c)
    auc_m = roc_auc_score(d, lr.predict_proba(te_m.transform(b[["merchant"]]))[:, 1])
    show(f"cross-fitted target encoding of the REAL merchant id: test AUC {auc_m:.3f} (1 column instead of 1000)")
    check("...while keeping the real signal", True, auc_m > 0.6)


# ---------------------------------------------------------------------------
# 3. scaling
# ---------------------------------------------------------------------------
def scaling():
    section("3. Scaling: who cares?")
    X, y = make_classification(2000, n_features=5, n_informative=5, n_redundant=0, random_state=2)
    X[:, 0] *= 1000                                               # one feature in different units
    a, b, c, d = train_test_split(X, y, test_size=0.3, random_state=0)
    acc = lambda m: m.fit(a, c).score(b, d)  # noqa: E731
    knn_raw, knn_scaled = acc(KNeighborsClassifier(15)), acc(make_pipeline(StandardScaler(), KNeighborsClassifier(15)))
    tree_raw, tree_scaled = acc(DecisionTreeClassifier(random_state=0)), acc(make_pipeline(StandardScaler(), DecisionTreeClassifier(random_state=0)))
    show(f"KNN accuracy: raw {knn_raw:.3f}, scaled {knn_scaled:.3f}   |   tree: raw {tree_raw:.3f}, scaled {tree_scaled:.3f}")
    check("KNN (distance-based) collapses without scaling", True, knn_scaled > knn_raw + 0.1)
    check("a tree doesn't care about scale (identical accuracy)", tree_raw, tree_scaled)
    x = np.array([[1.0], [2.0], [3.0], [4.0], [1000.0]])
    check("StandardScaler: (x - mean)/std is wrecked by one outlier", True, np.ptp(StandardScaler().fit_transform(x)[:4]) < 0.1)
    check("MinMaxScaler squeezes the normal values together too", True, MinMaxScaler().fit_transform(x)[3, 0] < 0.01)
    check("RobustScaler (median/IQR) keeps them spread out", [-1.0, -0.5, 0.0, 0.5], RobustScaler().fit_transform(x)[:4, 0].tolist())


# ---------------------------------------------------------------------------
# 4. creating features
# ---------------------------------------------------------------------------
def feature_creation():
    section("4. Creating features")
    rng = np.random.default_rng(3)
    n = 3000
    usage = rng.normal(size=n)                                   # standardised monthly usage
    premium = rng.integers(0, 2, n)                              # 1 = premium plan
    misfit = (usage > 0) != (premium == 1)                       # heavy user on basic OR light user on premium
    y = (misfit ^ (rng.random(n) < 0.1)).astype(int)             # churn when the plan doesn't fit (10% label noise)
    raw = np.column_stack([usage, premium])
    with_inter = np.column_stack([usage, premium, usage * (2 * premium - 1)])
    auc = lambda X: cross_val_score(LogisticRegression(max_iter=2000), X, y, cv=5, scoring="roc_auc").mean()  # noqa: E731
    a_raw, a_int = auc(raw), auc(with_inter)
    show(f"logistic regression AUC: raw usage+plan {a_raw:.3f}, plus interaction usage x plan {a_int:.3f}")
    check("without the interaction a linear model is at chance (it's an XOR)", True, a_raw < 0.6)
    check("one engineered interaction feature fixes it", True, a_int > 0.85)
    ts = pd.Series(pd.to_datetime(["2026-01-05 09:30", "2026-01-10 23:15", "2026-03-01 14:00"]))
    feats = pd.DataFrame({"dow": ts.dt.dayofweek, "hour": ts.dt.hour, "is_weekend": ts.dt.dayofweek >= 5, "month": ts.dt.month})
    check("datetime -> day-of-week (Mon=0)", [0, 5, 6], feats["dow"].tolist())
    check("datetime -> is_weekend", [False, True, True], feats["is_weekend"].tolist())
    hour = np.arange(24)
    enc = np.column_stack([np.sin(2 * np.pi * hour / 24), np.cos(2 * np.pi * hour / 24)])
    check("cyclical encoding: 23:00 and 00:00 are neighbours", True, np.linalg.norm(enc[23] - enc[0]) < np.linalg.norm(enc[12] - enc[0]) / 5)


# ---------------------------------------------------------------------------
# 5. cross-validation variants
# ---------------------------------------------------------------------------
def cross_validation():
    section("5. Cross-validation variants")
    X, y = make_classification(1000, weights=[0.95], random_state=4)
    pos_per_fold = lambda cv: [int(y[te].sum()) for _, te in cv.split(X, y)]  # noqa: E731
    show(f"positives per fold: KFold(no shuffle) {pos_per_fold(KFold(5))}, StratifiedKFold {pos_per_fold(StratifiedKFold(5, shuffle=True, random_state=0))}")
    strat = pos_per_fold(StratifiedKFold(5, shuffle=True, random_state=0))
    check("stratified folds keep the rare class balanced (max - min <= 1)", True, max(strat) - min(strat) <= 1)
    scores = cross_val_score(LogisticRegression(max_iter=1000), X, y, cv=StratifiedKFold(5, shuffle=True, random_state=0), scoring="roc_auc")
    show(f"5-fold AUC = {scores.mean():.3f} +- {scores.std():.3f}  (report the spread, not just the mean)")

    # group k-fold: many rows per patient
    rng = np.random.default_rng(5)
    patients = np.repeat(np.arange(300), 10)
    signature = rng.normal(size=(300, 3))[patients]               # each patient has a personal signature
    Xg = signature + 0.01 * rng.normal(size=(3000, 3))
    yg = (rng.normal(size=300) > 0)[patients].astype(int)        # label is per patient, unrelated to features
    majority = max(yg.mean(), 1 - yg.mean())
    rf = RandomForestClassifier(100, random_state=0, n_jobs=-1)
    leaky = cross_val_score(rf, Xg, yg, cv=KFold(5, shuffle=True, random_state=0)).mean()
    honest = cross_val_score(rf, Xg, yg, cv=GroupKFold(5), groups=patients).mean()
    show(f"same patient in train and test: KFold accuracy {leaky:.3f}  vs  GroupKFold {honest:.3f} (majority-class rate {majority:.3f})")
    check("plain KFold is fooled: it memorises patients", True, leaky > 0.9)
    check("GroupKFold shows the truth: no better than guessing", True, honest < majority + 0.05)

    tss = list(TimeSeriesSplit(4).split(np.arange(20)))
    check("TimeSeriesSplit: always train on the past, test on the future", True, all(tr.max() < te.min() for tr, te in tss))
    check("...with an expanding training window", [4, 8, 12, 16], [len(tr) for tr, _ in tss])


# ---------------------------------------------------------------------------
# 6. tuning
# ---------------------------------------------------------------------------
def tuning():
    section("6. Hyperparameter tuning")
    X, y = make_classification(1500, n_features=20, n_informative=6, random_state=6)
    a, b, c, d = train_test_split(X, y, test_size=0.3, random_state=0)
    grid = GridSearchCV(RandomForestClassifier(random_state=0, n_jobs=-1),
                        {"max_depth": [3, 6, None], "min_samples_leaf": [1, 5, 20]}, cv=3, scoring="roc_auc").fit(a, c)
    rand = RandomizedSearchCV(RandomForestClassifier(random_state=0, n_jobs=-1),
                              {"max_depth": [3, 4, 5, 6, 8, 10, 15, None], "min_samples_leaf": list(range(1, 30)), "max_features": [0.2, 0.4, "sqrt", 0.8]},
                              n_iter=9, cv=3, scoring="roc_auc", random_state=0).fit(a, c)
    show(f"grid search (9 combos): best {grid.best_params_}, CV AUC {grid.best_score_:.3f}")
    show(f"random search (9 draws from 928 combos): best CV AUC {rand.best_score_:.3f}")
    check("random search matches grid search on the same budget of 9 fits", True, abs(rand.best_score_ - grid.best_score_) < 0.02)
    es = HistGradientBoostingClassifier(max_iter=2000, learning_rate=0.1, early_stopping=True, validation_fraction=0.15,
                                        n_iter_no_change=20, random_state=0).fit(a, c)
    show(f"early stopping: stopped after {es.n_iter_} of 2000 allowed trees, test AUC {roc_auc_score(d, es.predict_proba(b)[:, 1]):.3f}")
    check("early stopping halts long before the maximum", True, es.n_iter_ < 500)
    # selection bias: on PURE NOISE (true accuracy 0.5) the best of 90 configs still looks good
    rng = np.random.default_rng(10)
    Xn, yn = rng.normal(size=(120, 20)), rng.integers(0, 2, 120)
    space = {"max_depth": [1, 2, 3, 4, 5, None], "min_samples_leaf": [1, 2, 5, 10, 20], "max_features": [0.2, 0.5, 1.0]}
    inner = GridSearchCV(DecisionTreeClassifier(random_state=0), space, cv=StratifiedKFold(5, shuffle=True, random_state=0))
    nested = cross_val_score(inner, Xn, yn, cv=StratifiedKFold(5, shuffle=True, random_state=1)).mean()
    inner.fit(Xn, yn)
    show(f"pure noise, 90 configs: best inner-CV accuracy {inner.best_score_:.3f} (looks like skill!) vs nested-CV estimate {nested:.3f}")
    check("the best CV score after tuning is optimistic (truth is 0.5)", True, inner.best_score_ > 0.58)
    check("nested CV (tuning inside each outer fold) is lower, i.e. more honest", True, nested < inner.best_score_)


# ---------------------------------------------------------------------------
# 7. data leakage
# ---------------------------------------------------------------------------
def leakage():
    section("7. Data leakage: five ways to fool yourself")
    rng = np.random.default_rng(7)

    # (a) feature selection BEFORE cross-validation on pure noise
    Xn, yn = rng.normal(size=(100, 5000)), rng.integers(0, 2, 100)
    Xsel = SelectKBest(f_classif, k=20).fit_transform(Xn, yn)                 # sees ALL labels -> leak
    leaky = cross_val_score(LogisticRegression(max_iter=1000), Xsel, yn, cv=5).mean()
    honest = cross_val_score(make_pipeline(SelectKBest(f_classif, k=20), LogisticRegression(max_iter=1000)), Xn, yn, cv=5).mean()
    show(f"(a) select 20 of 5000 noise features, THEN cross-validate: accuracy {leaky:.3f}; selection inside the pipeline: {honest:.3f}")
    check("(a) selection before CV invents signal from pure noise", True, leaky > 0.8)
    check("(a) inside the pipeline it's correctly ~chance", True, abs(honest - 0.5) < 0.15)

    # (b) target leakage: a feature only known AFTER the outcome
    n = 3000
    X = rng.normal(size=(n, 5))
    churn = (X[:, 0] + rng.normal(size=n) > 1).astype(int)
    cancellation_call = churn * (rng.random(n) < 0.9)            # "called to cancel" happens because they churned
    Xl = np.column_stack([X, cancellation_call])
    with_leak = cross_val_score(LogisticRegression(max_iter=1000), Xl, churn, cv=5, scoring="roc_auc").mean()
    without = cross_val_score(LogisticRegression(max_iter=1000), X, churn, cv=5, scoring="roc_auc").mean()
    show(f"(b) with a post-outcome feature: AUC {with_leak:.3f}; without it: {without:.3f}")
    check("(b) target leakage gives a too-good-to-be-true score", True, with_leak > 0.95 and with_leak - without > 0.1)

    # (c) preprocessing fit on all data: test-set statistics leak into training and can hide drift
    Xp = rng.normal(size=(300, 1))
    Xp[200:] += 1.5                                              # the last 100 rows (the "future") have drifted
    train_only = StandardScaler().fit(Xp[:200]).transform(Xp[200:]).mean()
    all_rows = StandardScaler().fit(Xp).transform(Xp[200:]).mean()
    show(f"(c) mean of the drifted test feature after scaling: scaler fit on train {train_only:.2f}, fit on ALL rows {all_rows:.2f}")
    check("(c) fitting on all rows uses test statistics and shrinks the visible drift", True, all_rows < 0.7 * train_only)

    # (d) time leakage: random split on a time series
    series = np.cumsum(rng.normal(size=1000))                    # random walk: tomorrow ~= today
    Xt, yt = np.arange(1000)[:, None], series
    rf = RandomForestRegressor(100, random_state=0, n_jobs=-1)
    r2_random = cross_val_score(rf, Xt, yt, cv=KFold(5, shuffle=True, random_state=0), scoring="r2").mean()
    r2_time = cross_val_score(rf, Xt, yt, cv=TimeSeriesSplit(5), scoring="r2").mean()
    show(f"(d) predict a random walk from the date: shuffled KFold R2 {r2_random:.3f}, TimeSeriesSplit R2 {r2_time:.3f}")
    check("(d) shuffled split interpolates between neighbours -> fake skill", True, r2_random > 0.95)
    check("(d) a time split shows it can't forecast at all", True, r2_time < 0.3)

    # (e) duplicates across train and test
    Xd, yd = make_classification(500, n_features=10, flip_y=0.3, random_state=9)
    dup = np.vstack([Xd, Xd])
    dup_y = np.r_[yd, yd]
    tree = DecisionTreeClassifier(random_state=0)
    acc_dup = cross_val_score(tree, dup, dup_y, cv=KFold(5, shuffle=True, random_state=0)).mean()
    acc_dedup = cross_val_score(tree, Xd, yd, cv=KFold(5, shuffle=True, random_state=0)).mean()
    show(f"(e) every row duplicated: tree accuracy {acc_dup:.3f}; deduplicated: {acc_dedup:.3f}")
    check("(e) duplicates make memorisation look like skill", True, acc_dup > acc_dedup + 0.1)


if __name__ == "__main__":
    missing_values()
    encoding()
    scaling()
    feature_creation()
    cross_validation()
    tuning()
    leakage()
    summary()
