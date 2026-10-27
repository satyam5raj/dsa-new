"""
ML 19 : EXPLAINABILITY, EXPERIMENTS, CAUSAL INFERENCE, UNCERTAINTY, FEW LABELS
Run:  ../.venv/bin/python explain_experiment_causal.py        (explanations: README.md)

  1. Permutation importance        model-agnostic; vs impurity importance's bias
  2. Shapley values (exact)        fair credit assignment, checked against its axioms
  3. Partial dependence + LIME     global shape of a feature's effect; a local linear surrogate
  4. A/B testing                   sample size, z-test, the peeking problem, Bayesian A/B
  5. Causal inference              confounding; regression adjustment and inverse propensity weighting
  6. Uplift modelling              T-learner: who should get the treatment?
  7. Conformal prediction          prediction intervals with guaranteed coverage
  8. Few labels                    label propagation (semi-supervised) and active learning
"""
import sys
from itertools import combinations
from math import factorial
from pathlib import Path

import numpy as np
from scipy import stats
from sklearn.datasets import make_moons
from sklearn.ensemble import GradientBoostingRegressor, RandomForestClassifier, RandomForestRegressor
from sklearn.inspection import partial_dependence, permutation_importance
from sklearn.linear_model import LinearRegression, LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.semi_supervised import LabelSpreading

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from mlcheck import check, near, section, show, summary  # noqa: E402


# ---------------------------------------------------------------------------
# 1. permutation importance
# ---------------------------------------------------------------------------
def perm_importance(model, X, y, metric, n_repeats=10, seed=0):
    rng = np.random.default_rng(seed)
    base = metric(y, model.predict(X))
    out = np.zeros((X.shape[1], n_repeats))
    for j in range(X.shape[1]):
        for r in range(n_repeats):
            Xp = X.copy()
            Xp[:, j] = rng.permutation(Xp[:, j])                 # break feature j's link to y, keep its distribution
            out[j, r] = base - metric(y, model.predict(Xp))
    return out.mean(1)


def permutation_demo():
    section("1. Permutation importance vs impurity importance")
    rng = np.random.default_rng(0)
    n = 2000
    signal = rng.normal(size=n)
    id_like = rng.integers(0, 1000, n).astype(float)             # high-cardinality noise (like a user id)
    small = rng.normal(size=n)
    y = (signal + 0.3 * small + rng.normal(0, 0.5, n) > 0).astype(int)
    X = np.column_stack([signal, id_like, small])
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.5, random_state=0)
    rf = RandomForestClassifier(200, random_state=0, n_jobs=-1).fit(X_tr, y_tr)
    acc = lambda a, b: np.mean(a == b)                          # noqa: E731
    ours = perm_importance(rf, X_te, y_te, acc)
    sk = permutation_importance(rf, X_te, y_te, n_repeats=10, random_state=0).importances_mean
    names = ["signal", "id_like (noise)", "small effect"]
    show("impurity importance:    " + ", ".join(f"{k} {v:.3f}" for k, v in zip(names, rf.feature_importances_)))
    show("permutation importance: " + ", ".join(f"{k} {v:.3f}" for k, v in zip(names, ours)))
    check("impurity importance is fooled: the pure-noise id gets > 10% of the credit", True, rf.feature_importances_[1] > 0.1)
    check("permutation importance on held-out data gives the noise id ~0", True, abs(ours[1]) < 0.01)
    check("scratch permutation importance ranks features like sklearn's", np.argsort(-sk).tolist(), np.argsort(-ours).tolist())


# ---------------------------------------------------------------------------
# 2. exact Shapley values
# ---------------------------------------------------------------------------
def shapley(f, x, background):
    """phi_j = sum over subsets S without j of |S|!(d-|S|-1)!/d! * [v(S + j) - v(S)],
    v(S) = mean prediction when features in S are fixed to x and the rest come from the background data."""
    d = len(x)
    def v(S):
        Z = background.copy()
        Z[:, list(S)] = x[list(S)]
        return f(Z).mean()
    phi = np.zeros(d)
    for j in range(d):
        others = [k for k in range(d) if k != j]
        for size in range(d):
            for S in combinations(others, size):
                w = factorial(size) * factorial(d - size - 1) / factorial(d)
                phi[j] += w * (v(S + (j,)) - v(S))
    return phi, v(())


def shapley_demo():
    section("2. Shapley values (exact, by enumerating feature subsets)")
    rng = np.random.default_rng(1)
    X = rng.normal(size=(500, 4))
    w = np.array([2.0, -1.0, 0.5, 0.0])
    lin = lambda Z: Z @ w + 3                                   # noqa: E731
    x = np.array([1.0, 2.0, -1.0, 5.0])
    phi, base = shapley(lin, x, X)
    check("linear model: phi_j = w_j * (x_j - mean_j)", True, np.allclose(phi, w * (x - X.mean(0))))
    near("efficiency: base value + sum(phi) = the prediction", lin(x[None])[0], base + phi.sum(), 1e-9)
    check("a feature the model ignores gets exactly 0 ('dummy' axiom)", 0.0, round(phi[3], 12))
    inter = lambda Z: Z[:, 0] * Z[:, 1]                        # noqa: E731  pure interaction
    Xi = rng.choice([0.0, 1.0], size=(2000, 2))
    phi_i, base_i = shapley(inter, np.array([1.0, 1.0]), Xi)
    show(f"f = x0 * x1 at (1, 1), binary background: phi = {np.round(phi_i, 3).tolist()}, base {base_i:.3f}")
    check("symmetry: an interaction's credit is split equally", True, np.isclose(phi_i[0], phi_i[1], atol=0.02))
    show(f"exact Shapley for d features needs 2^d coalitions: d=4 -> 16, d=30 -> {2 ** 30:,} (hence KernelSHAP/TreeSHAP)")


# ---------------------------------------------------------------------------
# 3. partial dependence + LIME
# ---------------------------------------------------------------------------
def pdp_lime_demo():
    section("3. Partial dependence and a LIME-style local surrogate")
    rng = np.random.default_rng(2)
    X = rng.uniform(-3, 3, (1500, 3))
    y = np.sin(X[:, 0]) * 2 + X[:, 1] ** 2 * 0.5 + rng.normal(0, 0.2, 1500)
    gbm = GradientBoostingRegressor(random_state=0).fit(X, y)
    sk = partial_dependence(gbm, X, [0], grid_resolution=11, percentiles=(0, 1), kind="average", method="brute")
    grid = sk["grid_values"][0]                                # evaluate on sklearn's own grid
    ours = []
    for g in grid:                                             # PD(g) = average prediction with feature 0 set to g for EVERY row
        Z = X.copy()
        Z[:, 0] = g
        ours.append(gbm.predict(Z).mean())
    ours = np.array(ours)
    corr = np.corrcoef(ours, 2 * np.sin(grid))[0, 1]
    show(f"PD of feature 0 at {grid[[1, 4, 5, 8]].round(1).tolist()}: {ours[[1, 4, 5, 8]].round(2).tolist()} (shape of 2 sin(x))")
    check("scratch partial dependence follows the true effect 2 sin(x0)", True, corr > 0.98)
    check("...and equals sklearn's partial_dependence(method='brute')", True, np.allclose(ours, sk["average"][0]))
    rec = partial_dependence(gbm, X, [0], grid_resolution=11, percentiles=(0, 1), kind="average", method="recursion")["average"][0]
    show(f"sklearn's default 'recursion' method for boosting differs by a constant offset ~{np.mean(rec - ours):.2f} (same shape: corr {np.corrcoef(rec, ours)[0, 1]:.3f})")

    x0 = np.array([0.0, 2.0, 0.0])                             # LIME: perturb around one point, fit a weighted linear model
    P = x0 + rng.normal(0, 0.3, (2000, 3))
    wts = np.exp(-((P - x0) ** 2).sum(1) / 0.5)
    local = LinearRegression().fit(P, gbm.predict(P), sample_weight=wts).coef_
    show(f"LIME local slopes at x = (0, 2, 0): {np.round(local, 2).tolist()}  (true local gradient: [2cos(0)=2, x1=2, 0])")
    check("the local surrogate recovers the local gradient (to 0.5)", True, np.allclose(local, [2, 2, 0], atol=0.5))


# ---------------------------------------------------------------------------
# 4. A/B testing
# ---------------------------------------------------------------------------
def ab_demo():
    section("4. A/B testing")
    p0, mde, alpha, power = 0.10, 0.01, 0.05, 0.80             # baseline conversion 10%, detect +1 point
    za, zb = stats.norm.ppf(1 - alpha / 2), stats.norm.ppf(power)
    p1 = p0 + mde
    n = int(np.ceil((za + zb) ** 2 * (p0 * (1 - p0) + p1 * (1 - p1)) / mde ** 2))
    show(f"sample size per arm to detect 10% -> 11% with 80% power at alpha 0.05: {n:,}")
    check("sample size formula (two-proportion z-test)", 14749, n)
    rng = np.random.default_rng(3)

    def z_test(ca, na, cb, nb):
        pa, pb = ca / na, cb / nb
        pp = (ca + cb) / (na + nb)
        z = (pb - pa) / np.sqrt(pp * (1 - pp) * (1 / na + 1 / nb))
        return 2 * (1 - stats.norm.cdf(abs(z)))

    sims = 2000
    fp = np.mean([z_test(rng.binomial(n, p0), n, rng.binomial(n, p0), n) < alpha for _ in range(sims)])
    pw = np.mean([z_test(rng.binomial(n, p0), n, rng.binomial(n, p1), n) < alpha for _ in range(sims)])
    show(f"simulated: false-positive rate with no real effect {fp:.3f}; power with a real +1 point {pw:.3f}")
    near("the test's false-positive rate is alpha = 5%", 0.05, fp, 0.015)
    near("the planned sample size gives ~80% power", 0.80, pw, 0.03)

    peek_fp = 0                                                # peeking: check after every 1,000 users per arm, stop at the first p < 0.05
    for _ in range(1000):
        a = rng.random(n) < p0
        b = rng.random(n) < p0
        for k in range(1000, n + 1, 1000):
            if z_test(a[:k].sum(), k, b[:k].sum(), k) < alpha:
                peek_fp += 1
                break
    show(f"PEEKING at 14 interim looks with no real effect: false-positive rate {peek_fp / 1000:.3f}")
    check("peeking inflates false positives far above 5%", True, peek_fp / 1000 > 0.12)

    ca, cb = 1000, 1080                                        # Bayesian A/B: Beta posteriors, P(B > A)
    na = nb = 10000
    A = rng.beta(1 + ca, 1 + na - ca, 200000)
    B = rng.beta(1 + cb, 1 + nb - cb, 200000)
    p_better = np.mean(B > A)
    show(f"Bayesian A/B with 10.0% vs 10.8% on 10,000 each: P(B > A) = {p_better:.3f}; expected lift {np.mean(B - A):.4f}")
    near("P(B > A) agrees with the frequentist one-sided p-value", 1 - z_test(ca, na, cb, nb) / 2, p_better, 0.01)


# ---------------------------------------------------------------------------
# 5-6. causal inference + uplift
# ---------------------------------------------------------------------------
def make_observational(n=20000, seed=4):
    """Engaged customers are both more likely to get a coupon AND more likely to buy. True effect of the coupon: +0.05."""
    rng = np.random.default_rng(seed)
    engagement = rng.normal(size=n)
    age = rng.normal(size=n)
    p_treat = 1 / (1 + np.exp(-(1.5 * engagement)))
    t = (rng.random(n) < p_treat).astype(int)
    uplift = 0.05 + 0.08 * (age > 0.5)                         # the coupon works better for older customers
    p_buy = np.clip(0.2 + 0.15 * engagement + t * uplift, 0.01, 0.99)
    yb = (rng.random(n) < p_buy).astype(int)
    return np.column_stack([engagement, age]), t, yb, uplift


def causal_demo():
    section("5. Causal inference: confounding, adjustment, IPW")
    X, t, y, uplift = make_observational()
    true_ate = uplift.mean()
    naive = y[t == 1].mean() - y[t == 0].mean()
    reg = LinearRegression().fit(np.column_stack([t, X]), y).coef_[0]       # regression adjustment
    e = LogisticRegression().fit(X, t).predict_proba(X)[:, 1]              # propensity score P(T=1 | X)
    ipw = np.mean(t * y / e) - np.mean((1 - t) * y / (1 - e))               # re-weight to a pseudo-randomised population
    show(f"true average effect {true_ate:.4f}; naive difference {naive:.4f}; regression adjustment {reg:.4f}; IPW {ipw:.4f}")
    check("the naive comparison is badly biased by confounding (> 2x the truth)", True, naive > 2 * true_ate)
    near("regression adjustment recovers the effect", true_ate, reg, 0.015)
    near("inverse propensity weighting recovers the effect", true_ate, ipw, 0.015)
    show(f"propensity scores range {e.min():.3f} - {e.max():.3f}; extreme weights (1/e) make IPW noisy: check overlap")


def uplift_demo():
    section("6. Uplift modelling (T-learner): who should get the coupon?")
    X, t, y, uplift = make_observational(n=60000, seed=5)
    rng = np.random.default_rng(6)
    t = rng.integers(0, 2, len(t))                             # an RANDOMISED experiment this time (no confounding)
    p_buy = np.clip(0.2 + 0.15 * X[:, 0] + t * uplift, 0.01, 0.99)
    y = (rng.random(len(t)) < p_buy).astype(int)
    # big leaves: uplift (a difference of two noisy predictions) needs heavy smoothing and lots of data
    m1 = RandomForestRegressor(200, min_samples_leaf=500, random_state=0, n_jobs=-1).fit(X[t == 1], y[t == 1])
    m0 = RandomForestRegressor(200, min_samples_leaf=500, random_state=0, n_jobs=-1).fit(X[t == 0], y[t == 0])
    tau = m1.predict(X) - m0.predict(X)                       # predicted individual uplift
    top = tau >= np.quantile(tau, 0.7)
    show(f"true uplift: targeted top-30% by predicted uplift {uplift[top].mean():.4f} vs everyone {uplift.mean():.4f}")
    check("the T-learner finds the customers the coupon helps most", True, uplift[top].mean() > uplift.mean() + 0.03)
    check("...who are mostly the older customers (age > 0.5)", True, np.mean(X[top, 1] > 0.5) > 0.75)
    show(f"{np.mean(X[top, 1] > 0.5):.0%} of the targeted group is older (31% of all customers are)")


# ---------------------------------------------------------------------------
# 7. conformal prediction
# ---------------------------------------------------------------------------
def conformal_demo():
    section("7. Split conformal prediction")
    rng = np.random.default_rng(7)
    n = 6000
    x = rng.uniform(0, 10, n)
    y = np.sin(x) * 3 + rng.normal(0, 0.3 + 0.2 * x, n)          # noise grows with x
    X = x[:, None]
    idx = rng.permutation(n)
    tr, cal, te = idx[:3000], idx[3000:4500], idx[4500:]
    model = GradientBoostingRegressor(random_state=0).fit(X[tr], y[tr])
    scores = np.abs(y[cal] - model.predict(X[cal]))            # conformity scores on held-out calibration data
    q = np.quantile(scores, np.ceil((len(cal) + 1) * 0.9) / len(cal))
    pred = model.predict(X[te])
    covered = np.abs(y[te] - pred) <= q
    show(f"90% interval: prediction +- {q:.2f}; empirical coverage on new data {covered.mean():.3f}")
    near("conformal intervals hit the promised 90% coverage (no distribution assumptions)", 0.90, covered.mean(), 0.025)
    low, high = x[te] < 3, x[te] > 7
    show(f"...but coverage is {covered[low].mean():.2f} where noise is small and {covered[high].mean():.2f} where it is large")
    check("a constant-width interval over-covers easy regions and under-covers hard ones", True, covered[low].mean() > covered[high].mean() + 0.1)
    q_lo = GradientBoostingRegressor(loss="quantile", alpha=0.05, random_state=0).fit(X[tr], y[tr])
    q_hi = GradientBoostingRegressor(loss="quantile", alpha=0.95, random_state=0).fit(X[tr], y[tr])
    s = np.maximum(q_lo.predict(X[cal]) - y[cal], y[cal] - q_hi.predict(X[cal]))   # CQR conformity score
    qq = np.quantile(s, np.ceil((len(cal) + 1) * 0.9) / len(cal))
    lo, hi = q_lo.predict(X[te]) - qq, q_hi.predict(X[te]) + qq
    cov = (y[te] >= lo) & (y[te] <= hi)
    show(f"conformalised quantile regression: coverage {cov.mean():.3f}; low-noise {cov[low].mean():.2f}, high-noise {cov[high].mean():.2f}")
    near("CQR keeps 90% overall", 0.90, cov.mean(), 0.025)
    check("...with adaptive widths: coverage is far more even across regions", True, abs(cov[low].mean() - cov[high].mean()) < abs(covered[low].mean() - covered[high].mean()) / 2)


# ---------------------------------------------------------------------------
# 8. few labels: label propagation + active learning
# ---------------------------------------------------------------------------
def label_propagation(X, y_partial, sigma=0.1, alpha=0.99, iters=200):
    """Spread labels over a similarity graph: F <- alpha * S F + (1 - alpha) * Y."""
    W = np.exp(-((X[:, None] - X[None]) ** 2).sum(-1) / (2 * sigma ** 2))
    np.fill_diagonal(W, 0)
    d = W.sum(1)
    S = W / np.sqrt(d)[:, None] / np.sqrt(d)[None]
    Y = np.zeros((len(X), 2))
    Y[y_partial >= 0, y_partial[y_partial >= 0]] = 1
    Fm = Y.copy()
    for _ in range(iters):
        Fm = alpha * S @ Fm + (1 - alpha) * Y
    return Fm.argmax(1)


def few_labels_demo():
    section("8. Few labels: label propagation and active learning")
    X, y = make_moons(400, noise=0.08, random_state=0)
    y_partial = np.full(len(y), -1)
    y_partial[[np.flatnonzero(y == 0)[0], np.flatnonzero(y == 1)[0]]] = [0, 1]     # ONE label per class
    lp = label_propagation(X, y_partial)
    sup = LogisticRegression().fit(X[y_partial >= 0], y[y_partial >= 0]).predict(X)
    sk = LabelSpreading(kernel="rbf", gamma=1 / (2 * 0.1 ** 2), alpha=0.99, max_iter=200).fit(X, y_partial).transduction_
    sweep = {sg: np.mean(label_propagation(X, y_partial, sigma=sg) == y) for sg in (0.05, 0.1, 0.2, 0.3)}
    show("kernel width sigma -> accuracy: " + ", ".join(f"{k}: {v:.3f}" for k, v in sweep.items()) + "  (too wide = labels leak across the gap)")
    check("the graph's kernel width is the key hyperparameter", True, sweep[0.1] - sweep[0.3] > 0.1)
    show(f"two moons with 2 labels out of 400: supervised logistic regression {np.mean(sup == y):.3f}, label propagation {np.mean(lp == y):.3f}")
    check("label propagation follows the moon shapes from just 2 labels", True, np.mean(lp == y) > 0.95)
    check("scratch label spreading == sklearn LabelSpreading", True, np.mean(lp == sk) > 0.99)

    rng = np.random.default_rng(8)
    Xp, yp = make_moons(2000, noise=0.25, random_state=1)
    X_pool, X_te, y_pool, y_te = Xp[:1500], Xp[1500:], yp[:1500], yp[1500:]
    def learn(strategy, budget=40):
        labelled = list(rng.choice(1500, 6, replace=False))
        while len(labelled) < budget:
            m = RandomForestClassifier(100, random_state=0, n_jobs=-1).fit(X_pool[labelled], y_pool[labelled])
            rest = np.setdiff1d(np.arange(1500), labelled)
            if strategy == "uncertainty":
                p = m.predict_proba(X_pool[rest])[:, 1]
                labelled.append(int(rest[np.argmin(np.abs(p - 0.5))]))   # ask about the most uncertain point
            else:
                labelled.append(int(rng.choice(rest)))
        m = RandomForestClassifier(100, random_state=0, n_jobs=-1).fit(X_pool[labelled], y_pool[labelled])
        return m.score(X_te, y_te)
    unc = np.mean([learn("uncertainty") for _ in range(5)])
    rnd = np.mean([learn("random") for _ in range(5)])
    show(f"accuracy with 40 labels (mean of 5 runs): random labelling {rnd:.3f}, uncertainty sampling {unc:.3f}")
    check("active learning: choosing WHICH points to label beats random labelling", True, unc > rnd)


if __name__ == "__main__":
    permutation_demo()
    shapley_demo()
    pdp_lime_demo()
    ab_demo()
    causal_demo()
    uplift_demo()
    conformal_demo()
    few_labels_demo()
    summary()
