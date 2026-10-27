"""
ML 06 : ENSEMBLES (many weak opinions -> one strong one)
Run:  ../.venv/bin/python ensembles.py        (explanations: README.md)

  1. Why averaging works      variance of an average of correlated models
  2. Bagging from scratch     bootstrap + average, out-of-bag error
  3. Random forest            feature subsampling decorrelates the trees
  4. Gradient boosting        from scratch: fit residuals, shrinkage, == sklearn
  5. AdaBoost                 from scratch: re-weight the mistakes
  6. Stacking and voting      a meta-model learns how to combine
"""
import sys
from pathlib import Path

import numpy as np
from sklearn.datasets import make_classification, make_friedman1
from sklearn.ensemble import (AdaBoostClassifier, BaggingRegressor, GradientBoostingRegressor,
                              RandomForestClassifier, RandomForestRegressor, StackingClassifier, VotingClassifier)
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import roc_auc_score
from sklearn.model_selection import train_test_split
from sklearn.naive_bayes import GaussianNB
from sklearn.neighbors import KNeighborsClassifier
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.tree import DecisionTreeClassifier, DecisionTreeRegressor

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from mlcheck import check, near, section, show, summary  # noqa: E402


def mse(a, b):
    return float(np.mean((a - b) ** 2))


# ---------------------------------------------------------------------------
# 1. the maths of averaging
# ---------------------------------------------------------------------------
def why_averaging():
    section("1. Why averaging works")
    rng = np.random.default_rng(0)
    B, sigma = 25, 1.0
    for rho in (0.0, 0.5):
        cov = sigma ** 2 * (rho * np.ones((B, B)) + (1 - rho) * np.eye(B))
        errors = rng.multivariate_normal(np.zeros(B), cov, size=200_000)   # 25 models' errors, correlated rho
        theory = rho * sigma ** 2 + (1 - rho) * sigma ** 2 / B
        near(f"var(average of 25 models), correlation {rho}: rho s^2 + (1-rho) s^2/B", theory, errors.mean(axis=1).var(), 0.01)
    show("independent models: variance / 25.  correlation 0.5: it can never drop below 0.5 -> decorrelate the models!")
    # majority vote of independent 60%-accurate voters
    votes = rng.random((100_000, 25)) < 0.6
    near("25 independent 60%-accurate voters, majority vote accuracy", 0.846, (votes.sum(1) > 12).mean(), 0.01)


# ---------------------------------------------------------------------------
# 2. bagging from scratch
# ---------------------------------------------------------------------------
def bagging():
    section("2. Bagging from scratch (+ out-of-bag error)")
    X, y = make_friedman1(1500, noise=1.0, random_state=0)
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.4, random_state=0)
    rng = np.random.default_rng(0)
    n, B = len(X_tr), 100
    preds_te, oob_sum, oob_cnt = [], np.zeros(n), np.zeros(n)
    in_bag_fraction = []
    for b in range(B):
        idx = rng.integers(0, n, n)                                       # bootstrap: n draws WITH replacement
        oob = np.setdiff1d(np.arange(n), idx)
        in_bag_fraction.append(len(np.unique(idx)) / n)
        tree = DecisionTreeRegressor(random_state=b).fit(X_tr[idx], y_tr[idx])
        preds_te.append(tree.predict(X_te))
        oob_sum[oob] += tree.predict(X_tr[oob])
        oob_cnt[oob] += 1
    near("a bootstrap sample contains ~63.2% of rows (1 - 1/e)", 1 - np.exp(-1), np.mean(in_bag_fraction), 0.01)
    single = mse(y_te, preds_te[0])
    bagged = mse(y_te, np.mean(preds_te, axis=0))
    show(f"test MSE: one deep tree {single:.2f}, bag of 100 trees {bagged:.2f}")
    check("bagging cuts the error of a single deep tree a lot", True, bagged < 0.6 * single)
    oob_mse = mse(y_tr[oob_cnt > 0], oob_sum[oob_cnt > 0] / oob_cnt[oob_cnt > 0])
    show(f"out-of-bag MSE {oob_mse:.2f} (a free validation estimate, no hold-out needed)")
    near("OOB error is close to the real test error", bagged, oob_mse, 0.25 * bagged)
    sk = BaggingRegressor(DecisionTreeRegressor(), n_estimators=100, random_state=0).fit(X_tr, y_tr)
    near("sklearn BaggingRegressor gives a similar test MSE", bagged, mse(y_te, sk.predict(X_te)), 0.3)


# ---------------------------------------------------------------------------
# 3. random forest: decorrelating the trees
# ---------------------------------------------------------------------------
def random_forest():
    section("3. Random forest = bagging + random feature subsets")
    X, y = make_friedman1(1500, n_features=10, noise=1.0, random_state=1)   # 5 useful features + 5 pure noise
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.4, random_state=0)
    res = {}
    for mf in (1.0, 0.33):
        rf = RandomForestRegressor(200, max_features=mf, random_state=0, n_jobs=-1, oob_score=True).fit(X_tr, y_tr)
        tree_preds = np.array([t.predict(X_te) for t in rf.estimators_])
        corr = np.corrcoef(tree_preds)[np.triu_indices(200, 1)].mean()
        res[mf] = (corr, mse(y_te, rf.predict(X_te)), rf.oob_score_)
        show(f"max_features={mf}: mean correlation between trees {corr:.3f}, test MSE {res[mf][1]:.2f}, OOB R2 {rf.oob_score_:.3f}")
    check("random feature subsets make the trees less correlated", True, res[0.33][0] < res[1.0][0])
    check("...but with 5 of 10 features pure noise, 0.33 makes each tree too weak", True, res[0.33][1] > res[1.0][1])
    # many redundant, noisy views of a few hidden factors: here decorrelation wins
    rng = np.random.default_rng(0)
    Z = rng.normal(size=(1500, 4))
    Xr = np.hstack([Z + 0.5 * rng.normal(size=Z.shape) for _ in range(5)])
    yr = np.sin(2 * Z[:, 0]) + Z[:, 1] * Z[:, 2] + np.abs(Z[:, 3]) + 0.3 * rng.normal(size=1500)
    a, b, c, d = train_test_split(Xr, yr, test_size=0.4, random_state=0)
    red = {mf: mse(d, RandomForestRegressor(200, max_features=mf, random_state=0, n_jobs=-1).fit(a, c).predict(b)) for mf in (1.0, 0.5)}
    show(f"20 redundant noisy features: test MSE max_features=1.0 {red[1.0]:.3f}, 0.5 {red[0.5]:.3f}")
    check("...while with many redundant useful features, subsampling helps", True, red[0.5] < red[1.0])
    sizes = {n: mse(y_te, RandomForestRegressor(n, max_features=0.33, random_state=0, n_jobs=-1).fit(X_tr, y_tr).predict(X_te)) for n in (1, 10, 100, 400)}
    show("test MSE by number of trees: " + ", ".join(f"{k}: {v:.2f}" for k, v in sizes.items()))
    check("more trees never hurt (they just stop helping)", True, sizes[400] <= sizes[100] * 1.02 and sizes[100] < sizes[10] < sizes[1])


# ---------------------------------------------------------------------------
# 4. gradient boosting from scratch
# ---------------------------------------------------------------------------
class GradientBoostingScratch:
    """Squared loss: the negative gradient is just the residual y - F(x). Fit a small tree to it, add a fraction."""

    def __init__(self, n_estimators=200, learning_rate=0.1, max_depth=3):
        self.n, self.lr, self.depth = n_estimators, learning_rate, max_depth

    def fit(self, X, y):
        self.f0 = float(np.mean(y))
        F = np.full(len(y), self.f0)
        self.trees, self.train_mse = [], []
        for m in range(self.n):
            residual = y - F
            tree = DecisionTreeRegressor(max_depth=self.depth, random_state=0).fit(X, residual)
            F += self.lr * tree.predict(X)
            self.trees.append(tree)
            self.train_mse.append(mse(y, F))
        return self

    def staged_predict(self, X):
        F = np.full(len(X), self.f0)
        for tree in self.trees:
            F = F + self.lr * tree.predict(X)
            yield F

    def predict(self, X):
        return list(self.staged_predict(X))[-1]


def gradient_boosting():
    section("4. Gradient boosting from scratch")
    X, y = make_friedman1(1500, noise=1.0, random_state=2)
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.4, random_state=0)
    ours = GradientBoostingScratch(300, 0.1, 3).fit(X_tr, y_tr)
    sk = GradientBoostingRegressor(n_estimators=300, learning_rate=0.1, max_depth=3, random_state=0,
                                   criterion="squared_error").fit(X_tr, y_tr)
    m_ours, m_sk = mse(y_te, ours.predict(X_te)), mse(y_te, sk.predict(X_te))
    show(f"test MSE: scratch {m_ours:.3f}, sklearn {m_sk:.3f}")
    near("scratch boosting == sklearn GradientBoostingRegressor", m_sk, m_ours, 0.05)
    check("training error falls with every tree", True, all(b <= a for a, b in zip(ours.train_mse, ours.train_mse[1:])))
    curve = [mse(y_te, F) for F in ours.staged_predict(X_te)]
    show(f"test MSE after 1, 10, 100, 300 trees: {curve[0]:.2f}, {curve[9]:.2f}, {curve[99]:.2f}, {curve[299]:.2f}")
    # learning rate vs number of trees
    big = GradientBoostingScratch(300, 1.0, 3).fit(X_tr, y_tr)
    big_curve = [mse(y_te, F) for F in big.staged_predict(X_te)]
    show(f"learning rate 1.0: best test MSE {min(big_curve):.2f} at tree {int(np.argmin(big_curve)) + 1}, final {big_curve[-1]:.2f}")
    check("shrinkage (lr 0.1) generalises better than lr 1.0", True, min(curve) < min(big_curve))
    check("with lr 1.0 the model overfits after its best point", True, big_curve[-1] > min(big_curve) * 1.1)
    deep = GradientBoostingScratch(300, 0.1, 10).fit(X_tr, y_tr)
    show(f"depth-10 trees: train MSE {deep.train_mse[-1]:.4f}, test MSE {mse(y_te, deep.predict(X_te)):.2f}")
    check("boosting deep trees memorises (weak learners should be shallow)", True, mse(y_te, deep.predict(X_te)) > m_ours)


# ---------------------------------------------------------------------------
# 5. AdaBoost from scratch
# ---------------------------------------------------------------------------
def adaboost_scratch(X, y, rounds):
    """y in {-1, +1}. Each round: fit a stump on weighted data, give it a vote alpha, up-weight its mistakes."""
    n = len(y)
    w = np.full(n, 1 / n)
    stumps, alphas = [], []
    for _ in range(rounds):
        stump = DecisionTreeClassifier(max_depth=1, random_state=0).fit(X, y, sample_weight=w)
        pred = stump.predict(X)
        err = np.sum(w * (pred != y)) / np.sum(w)
        alpha = 0.5 * np.log((1 - err) / max(err, 1e-12))
        w = w * np.exp(-alpha * y * pred)
        w /= w.sum()
        stumps.append(stump)
        alphas.append(alpha)
    return stumps, alphas


def adaboost():
    section("5. AdaBoost from scratch")
    X, y01 = make_classification(2000, n_features=10, n_informative=5, random_state=4)
    y = 2 * y01 - 1
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.4, random_state=0)
    stumps, alphas = adaboost_scratch(X_tr, y_tr, 200)
    score = lambda Xq, k: sum(a * s.predict(Xq) for a, s in zip(alphas[:k], stumps[:k]))  # noqa: E731
    acc1 = np.mean(np.sign(score(X_te, 1)) == y_te)
    acc200 = np.mean(np.sign(score(X_te, 200)) == y_te)
    show(f"test accuracy: 1 stump {acc1:.3f}, 200 boosted stumps {acc200:.3f}")
    check("boosting turns weak stumps into a strong model", True, acc200 > acc1 + 0.1)
    check("first stump's vote alpha > 0 (it is better than chance)", True, alphas[0] > 0)
    sk = AdaBoostClassifier(DecisionTreeClassifier(max_depth=1), n_estimators=200, random_state=0).fit(X_tr, y_tr)
    near("scratch AdaBoost ~= sklearn AdaBoostClassifier accuracy", sk.score(X_te, y_te), acc200, 0.02)


# ---------------------------------------------------------------------------
# 6. voting and stacking
# ---------------------------------------------------------------------------
def stacking():
    section("6. Voting and stacking")
    X, y = make_classification(3000, n_features=20, n_informative=8, class_sep=0.7, random_state=5)
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.3, stratify=y, random_state=0)
    base = [
        ("lr", make_pipeline(StandardScaler(), LogisticRegression(max_iter=1000))),
        ("knn", make_pipeline(StandardScaler(), KNeighborsClassifier(25))),
        ("nb", GaussianNB()),
        ("rf", RandomForestClassifier(300, random_state=0, n_jobs=-1)),
    ]
    aucs = {}
    for name, m in base:
        aucs[name] = roc_auc_score(y_te, m.fit(X_tr, y_tr).predict_proba(X_te)[:, 1])
    vote = VotingClassifier(base, voting="soft").fit(X_tr, y_tr)
    stack = StackingClassifier(base, final_estimator=LogisticRegression(), cv=5).fit(X_tr, y_tr)
    aucs["soft vote"] = roc_auc_score(y_te, vote.predict_proba(X_te)[:, 1])
    aucs["stacking"] = roc_auc_score(y_te, stack.predict_proba(X_te)[:, 1])
    show("ROC-AUC: " + ", ".join(f"{k} {v:.3f}" for k, v in aucs.items()))
    show(f"stacking meta-model weights (lr, knn, nb, rf): {np.round(stack.final_estimator_.coef_[0], 2).tolist()}")
    check("stacking beats the average base model", True, aucs["stacking"] > np.mean([aucs[k] for k in ("lr", "knn", "nb", "rf")]))
    check("stacking >= the best single model (within 0.005)", True, aucs["stacking"] >= max(aucs[k] for k in ("lr", "knn", "nb", "rf")) - 0.005)


if __name__ == "__main__":
    why_averaging()
    bagging()
    random_forest()
    gradient_boosting()
    adaboost()
    stacking()
    summary()
