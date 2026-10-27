"""
ML 04 : CLASSIFICATION (predicting a category)
Run:  ../.venv/bin/python classification.py        (explanations: README.md)

  1. Logistic regression from scratch     sigmoid, log loss, gradient descent
  2. KNN and Gaussian naive Bayes          from scratch
  3. Decision tree split from scratch      Gini impurity, best threshold
  4. Metrics from scratch                  confusion matrix, precision, recall, F1, ROC-AUC, PR-AUC, log loss
  5. Model zoo                             LR, KNN, NB, tree, RF, SVM, boosting, LDA, QDA on one dataset
  6. Imbalanced fraud project              class weights, threshold choice by cost, PR-AUC
  7. Calibration                           are predicted probabilities honest?
"""
import sys
from pathlib import Path

import numpy as np
from sklearn.calibration import CalibratedClassifierCV
from sklearn.datasets import make_classification, make_moons
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis, QuadraticDiscriminantAnalysis
from sklearn.ensemble import HistGradientBoostingClassifier, RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import average_precision_score, log_loss, roc_auc_score
from sklearn.model_selection import train_test_split
from sklearn.naive_bayes import GaussianNB
from sklearn.neighbors import KNeighborsClassifier
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.svm import SVC
from sklearn.tree import DecisionTreeClassifier

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from mlcheck import check, near, section, show, summary  # noqa: E402


def sigmoid(z):
    return 1 / (1 + np.exp(-z))


# ---------------------------------------------------------------------------
# 1. logistic regression from scratch
# ---------------------------------------------------------------------------
class LogisticRegressionScratch:
    def fit(self, X, y, lr=0.5, epochs=3000, l2=0.0):
        n, d = X.shape
        self.w, self.b = np.zeros(d), 0.0
        self.losses = []
        for _ in range(epochs):
            p = sigmoid(X @ self.w + self.b)
            self.losses.append(binary_log_loss(y, p))
            grad_w = X.T @ (p - y) / n + l2 * self.w            # the famous (p - y) gradient
            grad_b = np.mean(p - y)
            self.w -= lr * grad_w
            self.b -= lr * grad_b
        return self

    def predict_proba(self, X):
        return sigmoid(X @ self.w + self.b)

    def predict(self, X, threshold=0.5):
        return (self.predict_proba(X) >= threshold).astype(int)


def binary_log_loss(y, p, eps=1e-15):
    p = np.clip(p, eps, 1 - eps)
    return float(-np.mean(y * np.log(p) + (1 - y) * np.log(1 - p)))


def logistic_from_scratch():
    section("1. Logistic regression from scratch")
    check("sigmoid(0) = 0.5 (on the decision boundary)", 0.5, sigmoid(0.0))
    near("sigmoid(2) = 0.881", 0.8808, sigmoid(2.0), 1e-4)
    rng = np.random.default_rng(0)
    X = rng.normal(size=(1000, 2))
    true_w, true_b = np.array([2.0, -3.0]), 0.5
    y = (rng.random(1000) < sigmoid(X @ true_w + true_b)).astype(int)
    model = LogisticRegressionScratch().fit(X, y)
    show(f"learned w = {np.round(model.w, 3).tolist()}, b = {model.b:.3f}   (true w = [2, -3], b = 0.5)")
    check("recovers the true weights (to 0.3)", True, np.allclose(model.w, true_w, atol=0.3))
    sk = LogisticRegression(penalty=None).fit(X, y)
    check("matches sklearn (no penalty) to 0.005", True, np.allclose(model.w, sk.coef_[0], atol=5e-3))
    check("log loss decreases during training", True, model.losses[-1] < model.losses[0])
    near("starting log loss (all p = 0.5) = ln 2", np.log(2), model.losses[0], 1e-9)
    near("an odds interpretation: e^w1 = odds multiply per unit of x1", np.exp(2.0), np.exp(model.w[0]), 2.0)
    acc = np.mean(model.predict(X) == y)
    show(f"training accuracy {acc:.3f}")
    check("accuracy well above 50%", True, acc > 0.8)


# ---------------------------------------------------------------------------
# 2. KNN and naive Bayes from scratch
# ---------------------------------------------------------------------------
def knn_predict(X_train, y_train, X_query, k):
    d = ((X_query[:, None, :] - X_train[None, :, :]) ** 2).sum(-1)   # (q, n) squared distances
    nearest = np.argsort(d, axis=1)[:, :k]
    return (y_train[nearest].mean(axis=1) >= 0.5).astype(int)        # majority vote (binary)


class GaussianNBScratch:
    """P(class | x) ∝ P(class) * prod_j N(x_j; mean_cj, var_cj)  -- 'naive' = features independent given the class."""

    def fit(self, X, y):
        self.classes = np.unique(y)
        self.prior = np.array([np.mean(y == c) for c in self.classes])
        self.mean = np.array([X[y == c].mean(axis=0) for c in self.classes])
        self.var = np.array([X[y == c].var(axis=0) + 1e-9 for c in self.classes])
        return self

    def predict(self, X):
        log_lik = -0.5 * (np.log(2 * np.pi * self.var)[None] + (X[:, None, :] - self.mean[None]) ** 2 / self.var[None]).sum(-1)
        return self.classes[np.argmax(np.log(self.prior)[None] + log_lik, axis=1)]


def knn_and_nb():
    section("2. KNN and Gaussian naive Bayes from scratch")
    X, y = make_moons(600, noise=0.25, random_state=0)
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.3, random_state=0)
    for k in (1, 15):
        ours = knn_predict(X_tr, y_tr, X_te, k)
        sk = KNeighborsClassifier(k).fit(X_tr, y_tr).predict(X_te)
        check(f"KNN k={k}: scratch == sklearn", True, np.array_equal(ours, sk))
    acc1 = np.mean(knn_predict(X_tr, y_tr, X_tr, 1) == y_tr)
    check("k=1 is perfect on the training set (each point is its own neighbour)", 1.0, acc1)
    te1, te15 = (np.mean(knn_predict(X_tr, y_tr, X_te, k) == y_te) for k in (1, 15))
    show(f"KNN test accuracy: k=1 {te1:.3f}, k=15 {te15:.3f}")
    check("a larger k smooths the boundary and generalises better here", True, te15 > te1)
    nb = GaussianNBScratch().fit(X_tr, y_tr)
    check("naive Bayes scratch == sklearn GaussianNB", True, np.array_equal(nb.predict(X_te), GaussianNB().fit(X_tr, y_tr).predict(X_te)))
    show(f"naive Bayes test accuracy {np.mean(nb.predict(X_te) == y_te):.3f} (its boundary is a smooth curve, so it can't follow the moons)")


# ---------------------------------------------------------------------------
# 3. one decision-tree split from scratch
# ---------------------------------------------------------------------------
def gini(y):
    if len(y) == 0:
        return 0.0
    p = np.bincount(y, minlength=2) / len(y)
    return float(1 - np.sum(p ** 2))


def best_split(x, y):
    best = (None, gini(y))
    for t in np.unique(x)[:-1]:
        left, right = y[x <= t], y[x > t]
        weighted = (len(left) * gini(left) + len(right) * gini(right)) / len(y)
        if weighted < best[1]:
            best = (float(t), weighted)
    return best


def tree_split():
    section("3. Decision-tree split from scratch (Gini)")
    check("Gini of a pure node", 0.0, gini(np.array([1, 1, 1, 1])))
    check("Gini of a 50/50 node (the worst for 2 classes)", 0.5, gini(np.array([0, 1, 0, 1])))
    hours = np.array([1, 2, 3, 4, 5, 6, 7, 8])
    passed = np.array([0, 0, 0, 1, 0, 1, 1, 1])
    t, g = best_split(hours, passed)
    check("best threshold for 'hours studied -> passed'", 3.0, t)
    near("weighted Gini after that split", 0.2, g, 1e-9)
    stump = DecisionTreeClassifier(max_depth=1).fit(hours[:, None], passed)
    check("sklearn's stump splits between 3 and 4 (it uses the midpoint)", 3.5, stump.tree_.threshold[0])


# ---------------------------------------------------------------------------
# 4. metrics from scratch
# ---------------------------------------------------------------------------
def confusion(y, pred):
    tp = int(np.sum((pred == 1) & (y == 1)))
    fp = int(np.sum((pred == 1) & (y == 0)))
    fn = int(np.sum((pred == 0) & (y == 1)))
    tn = int(np.sum((pred == 0) & (y == 0)))
    return tp, fp, fn, tn


def roc_auc_scratch(y, score):
    """AUC = P(a random positive scores higher than a random negative). Ties count half."""
    pos, neg = score[y == 1], score[y == 0]
    wins = (pos[:, None] > neg[None, :]).sum() + 0.5 * (pos[:, None] == neg[None, :]).sum()
    return wins / (len(pos) * len(neg))


def average_precision_scratch(y, score):
    order = np.argsort(-score, kind="stable")
    y_sorted = y[order]
    tp_cum = np.cumsum(y_sorted)
    precision_at_k = tp_cum / np.arange(1, len(y) + 1)
    return float(np.sum(precision_at_k * y_sorted) / y.sum())     # mean precision at each positive


def metrics():
    section("4. Classification metrics from scratch")
    y = np.array([1, 1, 1, 1, 0, 0, 0, 0, 0, 0])
    score = np.array([0.9, 0.8, 0.6, 0.3, 0.7, 0.4, 0.2, 0.2, 0.1, 0.05])
    pred = (score >= 0.5).astype(int)
    tp, fp, fn, tn = confusion(y, pred)
    check("confusion matrix (TP, FP, FN, TN) at threshold 0.5", (3, 1, 1, 5), (tp, fp, fn, tn))
    precision, recall = tp / (tp + fp), tp / (tp + fn)
    check("precision = TP/(TP+FP)  'of my alarms, how many were real?'", 0.75, precision)
    check("recall    = TP/(TP+FN)  'of the real ones, how many did I catch?'", 0.75, recall)
    check("F1 = harmonic mean of precision and recall", 0.75, round(2 * precision * recall / (precision + recall), 10))
    check("specificity = TN/(TN+FP)", 0.8333, round(tn / (tn + fp), 4))
    check("ROC-AUC from scratch (pairwise ranking)", 0.875, roc_auc_scratch(y, score))
    check("...matches sklearn", True, np.isclose(roc_auc_scratch(y, score), roc_auc_score(y, score)))
    ap = average_precision_scratch(y, score)
    check("PR-AUC (average precision) from scratch == sklearn", True, np.isclose(ap, average_precision_score(y, score)))
    show(f"average precision = {ap:.4f}  (precision at each positive: 1/1, 2/2, 3/4, 4/6)")
    check("log loss from scratch == sklearn", True, np.isclose(binary_log_loss(y, score), log_loss(y, score)))
    t_low = confusion(y, (score >= 0.25).astype(int))
    check("lower threshold 0.25 -> recall 1.0 (TP 4) but 2 false alarms", (4, 2), (t_low[0], t_low[1]))


# ---------------------------------------------------------------------------
# 5. a model zoo
# ---------------------------------------------------------------------------
def model_zoo():
    section("5. Model zoo on one dataset")
    X, y = make_classification(3000, n_features=20, n_informative=8, n_redundant=4, class_sep=0.8, random_state=1)
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.3, stratify=y, random_state=0)
    zoo = {
        "logistic regression": make_pipeline(StandardScaler(), LogisticRegression(max_iter=1000)),
        "KNN (k=15)": make_pipeline(StandardScaler(), KNeighborsClassifier(15)),
        "naive Bayes": GaussianNB(),
        "LDA": LinearDiscriminantAnalysis(),
        "QDA": QuadraticDiscriminantAnalysis(reg_param=0.01),
        "decision tree (depth 8)": DecisionTreeClassifier(max_depth=8, random_state=0),
        "random forest": RandomForestClassifier(300, random_state=0, n_jobs=-1),
        "SVM (RBF)": make_pipeline(StandardScaler(), SVC(C=3, probability=True, random_state=0)),
        "gradient boosting": HistGradientBoostingClassifier(random_state=0),
    }
    aucs = {}
    show(f"{'model':<26}{'accuracy':>9}{'ROC-AUC':>9}")
    for name, m in zoo.items():
        m.fit(X_tr, y_tr)
        proba = m.predict_proba(X_te)[:, 1]
        aucs[name] = roc_auc_score(y_te, proba)
        show(f"{name:<26}{np.mean(m.predict(X_te) == y_te):>9.3f}{aucs[name]:>9.3f}")
    check("all models beat chance (AUC > 0.75)", True, min(aucs.values()) > 0.75)
    check("LDA ~= logistic regression (both draw a linear boundary)", True, abs(aucs["LDA"] - aucs["logistic regression"]) < 0.01)
    check("the non-linear models (SVM/RF/boosting) beat logistic regression", True,
          min(aucs["SVM (RBF)"], aucs["random forest"], aucs["gradient boosting"]) > aucs["logistic regression"])
    check("a single tree is the weakest of the tree models", True, aucs["decision tree (depth 8)"] < min(aucs["random forest"], aucs["gradient boosting"]))


# ---------------------------------------------------------------------------
# 6. imbalanced fraud detection
# ---------------------------------------------------------------------------
def fraud_project():
    section("6. Imbalanced fraud detection (1% positives)")
    X, y = make_classification(40000, n_features=12, n_informative=6, weights=[0.99], class_sep=1.0, flip_y=0.002, random_state=7)
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.3, stratify=y, random_state=0)
    show(f"fraud rate: train {y_tr.mean():.4f}, test {y_te.mean():.4f}")
    always_legit = np.mean(y_te == 0)
    check("'always predict legit' already gets ~99% accuracy (useless!)", True, always_legit > 0.985)

    plain = make_pipeline(StandardScaler(), LogisticRegression(max_iter=1000)).fit(X_tr, y_tr)
    weighted = make_pipeline(StandardScaler(), LogisticRegression(max_iter=1000, class_weight="balanced")).fit(X_tr, y_tr)
    gb = HistGradientBoostingClassifier(random_state=0).fit(X_tr, y_tr)
    for name, m in (("plain LR", plain), ("balanced LR", weighted), ("boosting", gb)):
        tp, fp, fn, tn = confusion(y_te, m.predict(X_te))
        p = m.predict_proba(X_te)[:, 1]
        show(f"{name:<12} @0.5: recall {tp/(tp+fn):.3f} precision {tp/max(tp+fp,1):.3f} | ROC-AUC {roc_auc_score(y_te, p):.3f} PR-AUC {average_precision_score(y_te, p):.3f}")
    rec = lambda m: confusion(y_te, m.predict(X_te))  # noqa: E731
    r_plain, r_w = rec(plain), rec(weighted)
    check("class_weight='balanced' catches far more fraud at threshold 0.5", True, r_w[0] / (r_w[0] + r_w[2]) > r_plain[0] / (r_plain[0] + r_plain[2]) + 0.2)
    check("...at the price of many more false alarms", True, r_w[1] > 5 * r_plain[1])
    p_gb = gb.predict_proba(X_te)[:, 1]
    check("PR-AUC is far below ROC-AUC on rare positives (it's the honest one)", True, average_precision_score(y_te, p_gb) < roc_auc_score(y_te, p_gb) - 0.1)

    # choose the threshold by business cost: missed fraud costs 200, a false alarm (manual review) costs 5
    COST_FN, COST_FP = 200, 5
    X_fit, X_val, y_fit, y_val = train_test_split(X_tr, y_tr, test_size=0.3, stratify=y_tr, random_state=1)
    gb_v = HistGradientBoostingClassifier(random_state=0).fit(X_fit, y_fit)
    p_val = gb_v.predict_proba(X_val)[:, 1]
    thresholds = np.linspace(0.01, 0.9, 90)
    costs = [COST_FN * confusion(y_val, (p_val >= t).astype(int))[2] + COST_FP * confusion(y_val, (p_val >= t).astype(int))[1] for t in thresholds]
    best_t = thresholds[int(np.argmin(costs))]
    p_te = gb_v.predict_proba(X_te)[:, 1]
    cost_at = lambda t: COST_FN * confusion(y_te, (p_te >= t).astype(int))[2] + COST_FP * confusion(y_te, (p_te >= t).astype(int))[1]  # noqa: E731
    show(f"threshold chosen on VALIDATION data by cost: {best_t:.2f}; test cost {cost_at(best_t):,} vs {cost_at(0.5):,} at 0.5")
    check("the cost-optimal threshold is well below 0.5 (misses are 40x pricier)", True, best_t < 0.5)
    check("...and it lowers the real cost on the untouched test set", True, cost_at(best_t) < cost_at(0.5))
    near("theory: optimal threshold for calibrated p = C_FP/(C_FP+C_FN) = 5/205", 5 / 205, best_t, 0.1)


# ---------------------------------------------------------------------------
# 7. calibration
# ---------------------------------------------------------------------------
def calibration():
    section("7. Calibration")
    X, y = make_classification(6000, n_features=6, n_informative=4, n_redundant=0, random_state=3)
    rng = np.random.default_rng(0)
    copies = [X[:, :2] + 0.1 * rng.normal(size=(6000, 2)) for _ in range(4)]   # the same evidence, 5 times over
    X = np.hstack([X] + copies)
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.5, random_state=0)

    def ece(y_true, p, bins=10):
        """expected calibration error: average |predicted - observed| over probability bins."""
        idx = np.minimum((p * bins).astype(int), bins - 1)
        return float(sum(np.mean(idx == b) * abs(p[idx == b].mean() - y_true[idx == b].mean()) for b in range(bins) if np.any(idx == b)))

    nb = GaussianNB().fit(X_tr, y_tr)
    lr = LogisticRegression(max_iter=1000).fit(X_tr, y_tr)
    nb_cal = CalibratedClassifierCV(GaussianNB(), method="isotonic", cv=5).fit(X_tr, y_tr)
    e_nb, e_lr, e_cal = (ece(y_te, m.predict_proba(X_te)[:, 1]) for m in (nb, lr, nb_cal))
    show(f"expected calibration error: naive Bayes {e_nb:.3f}, logistic {e_lr:.3f}, naive Bayes + isotonic {e_cal:.3f}")
    check("naive Bayes is over-confident (independence assumption double-counts evidence)", True, e_nb > 2 * e_lr)
    check("isotonic calibration fixes most of it", True, e_cal < e_nb / 2)
    check("calibration barely changes the ranking (AUC)", True,
          abs(roc_auc_score(y_te, nb.predict_proba(X_te)[:, 1]) - roc_auc_score(y_te, nb_cal.predict_proba(X_te)[:, 1])) < 0.02)


if __name__ == "__main__":
    logistic_from_scratch()
    knn_and_nb()
    tree_split()
    metrics()
    model_zoo()
    fraud_project()
    calibration()
    summary()
