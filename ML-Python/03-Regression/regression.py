"""
ML 03 : REGRESSION (predicting a number)
Run:  ../.venv/bin/python regression.py        (explanations: README.md)

  1. Linear regression from scratch   normal equation AND gradient descent
  2. Metrics                          MAE, MSE, RMSE, R^2 (from scratch, checked against sklearn)
  3. Polynomial features              underfitting vs overfitting (bias vs variance)
  4. Regularisation                   ridge (closed form), lasso (coordinate descent), elastic net
  5. Non-linear models                decision tree, random forest, gradient boosting, KNN, SVR
  6. Model comparison                 a house-price dataset, every model, one table
"""
import sys
from pathlib import Path

import numpy as np
from sklearn.ensemble import GradientBoostingRegressor, HistGradientBoostingRegressor, RandomForestRegressor
from sklearn.linear_model import ElasticNet, Lasso, LinearRegression, Ridge
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import train_test_split
from sklearn.neighbors import KNeighborsRegressor
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import PolynomialFeatures, StandardScaler
from sklearn.svm import SVR
from sklearn.tree import DecisionTreeRegressor

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from mlcheck import check, near, section, show, summary  # noqa: E402


# ---------------------------------------------------------------------------
# 1. linear regression from scratch
# ---------------------------------------------------------------------------
class LinearRegressionScratch:
    """y = X w + b.  Two ways to find w: solve the normal equation, or walk downhill."""

    def fit_normal_equation(self, X, y):
        Xb = np.column_stack([np.ones(len(X)), X])           # column of 1s = the bias term
        theta = np.linalg.solve(Xb.T @ Xb, Xb.T @ y)         # (X^T X) theta = X^T y
        self.b, self.w = theta[0], theta[1:]
        return self

    def fit_gradient_descent(self, X, y, lr=0.1, epochs=2000):
        n, d = X.shape
        self.w, self.b = np.zeros(d), 0.0
        self.losses = []
        for _ in range(epochs):
            error = X @ self.w + self.b - y                  # (n,)
            self.losses.append(float(np.mean(error ** 2)))
            self.w -= lr * (2 / n) * (X.T @ error)           # dL/dw = 2/n X^T (Xw + b - y)
            self.b -= lr * (2 / n) * error.sum()             # dL/db = 2/n sum(error)
        return self

    def predict(self, X):
        return X @ self.w + self.b


def from_scratch():
    section("1. Linear regression from scratch")
    rng = np.random.default_rng(0)
    X = rng.normal(size=(500, 3))
    true_w, true_b = np.array([3.0, -2.0, 0.5]), 4.0
    y = X @ true_w + true_b + rng.normal(scale=0.5, size=500)
    ne = LinearRegressionScratch().fit_normal_equation(X, y)
    show(f"normal equation: w = {np.round(ne.w, 3).tolist()}, b = {ne.b:.3f}   (true w = [3, -2, 0.5], b = 4)")
    check("normal equation recovers the true weights (to 0.1)", True, np.allclose(ne.w, true_w, atol=0.1))
    gd = LinearRegressionScratch().fit_gradient_descent(X, y)
    check("gradient descent reaches the same answer", True, np.allclose(gd.w, ne.w, atol=1e-6))
    check("the training loss never goes up (beyond 1e-12 rounding)", True, all(b <= a + 1e-12 for a, b in zip(gd.losses, gd.losses[1:])))
    near("final MSE ~ noise variance 0.5^2 = 0.25 (cannot do better)", 0.25, gd.losses[-1], 0.03)
    sk = LinearRegression().fit(X, y)
    check("matches scikit-learn's LinearRegression", True, np.allclose(sk.coef_, ne.w) and np.isclose(sk.intercept_, ne.b))


# ---------------------------------------------------------------------------
# 2. metrics
# ---------------------------------------------------------------------------
def mae(y, p): return float(np.mean(np.abs(y - p)))
def mse(y, p): return float(np.mean((y - p) ** 2))
def rmse(y, p): return mse(y, p) ** 0.5
def r2(y, p): return 1 - np.sum((y - p) ** 2) / np.sum((y - np.mean(y)) ** 2)


def metrics():
    section("2. Regression metrics")
    y = np.array([100.0, 200.0, 300.0, 400.0])
    p = np.array([110.0, 190.0, 330.0, 370.0])
    check("MAE  = mean(|10|,|10|,|30|,|30|)", 20.0, mae(y, p))
    check("MSE  = mean(100,100,900,900)", 500.0, mse(y, p))
    near("RMSE = sqrt(500) (same unit as y)", 22.3607, rmse(y, p), 1e-4)
    check("R^2  = 1 - 2000/50000", 0.96, round(r2(y, p), 10))
    check("scratch metrics == sklearn", True, np.isclose(mae(y, p), mean_absolute_error(y, p)) and np.isclose(r2(y, p), r2_score(y, p)))
    check("R^2 of always predicting the mean = 0", 0.0, r2(y, np.full(4, y.mean())))
    check("R^2 can be negative (worse than the mean)", True, r2(y, np.array([400.0, 300, 200, 100])) < 0)
    p_outlier = p.copy()
    p_outlier[0] = 600.0                                     # one terrible prediction
    show(f"one prediction off by 500: MAE {mae(y, p):.0f} -> {mae(y, p_outlier):.0f}, RMSE {rmse(y, p):.1f} -> {rmse(y, p_outlier):.1f}")
    check("RMSE punishes the big miss more than MAE does", True, rmse(y, p_outlier) / rmse(y, p) > mae(y, p_outlier) / mae(y, p))


# ---------------------------------------------------------------------------
# 3. polynomial features: bias vs variance
# ---------------------------------------------------------------------------
def polynomial():
    section("3. Polynomial features: underfit vs overfit")
    rng = np.random.default_rng(1)
    x = np.sort(rng.uniform(-3, 3, 40))
    y = np.sin(x) + rng.normal(scale=0.2, size=40)
    x_test = np.linspace(-3, 3, 200)
    y_test = np.sin(x_test)
    results = {}
    for degree in (1, 3, 15):
        model = make_pipeline(PolynomialFeatures(degree), StandardScaler(), LinearRegression())
        model.fit(x[:, None], y)
        results[degree] = (mse(y, model.predict(x[:, None])), mse(y_test, model.predict(x_test[:, None])))
        show(f"degree {degree:2d}: train MSE {results[degree][0]:.4f}   test MSE {results[degree][1]:.4f}")
    check("degree 1 underfits: high train AND test error (high bias)", True, results[1][0] > 0.1 and results[1][1] > 0.1)
    check("degree 3 is the sweet spot: lowest test error", 3, min(results, key=lambda d: results[d][1]))
    check("degree 15 has the lowest TRAIN error...", 15, min(results, key=lambda d: results[d][0]))
    check("...but a higher test error than degree 3 (high variance)", True, results[15][1] > results[3][1])


# ---------------------------------------------------------------------------
# 4. regularisation
# ---------------------------------------------------------------------------
def ridge_closed_form(X, y, alpha):
    """minimise ||y - Xw||^2 + alpha ||w||^2   ->   w = (X^T X + alpha I)^-1 X^T y  (X, y centred)."""
    return np.linalg.solve(X.T @ X + alpha * np.eye(X.shape[1]), X.T @ y)


def soft_threshold(rho, lam):
    return np.sign(rho) * max(abs(rho) - lam, 0.0)


def lasso_coordinate_descent(X, y, alpha, iters=200):
    """minimise 1/(2n) ||y - Xw||^2 + alpha ||w||_1 (sklearn's objective), one weight at a time."""
    n, d = X.shape
    w = np.zeros(d)
    for _ in range(iters):
        for j in range(d):
            residual = y - X @ w + X[:, j] * w[j]                 # prediction error ignoring feature j
            rho = X[:, j] @ residual / n
            w[j] = soft_threshold(rho, alpha) / (X[:, j] @ X[:, j] / n)
    return w


def regularisation():
    section("4. Regularisation: ridge, lasso, elastic net")
    rng = np.random.default_rng(2)
    n, d = 60, 20
    X = rng.normal(size=(n, d))
    true_w = np.zeros(d)
    true_w[:3] = [4.0, -3.0, 2.0]                                 # only 3 of 20 features matter
    y = X @ true_w + rng.normal(scale=1.0, size=n)
    X = X - X.mean(axis=0)
    y = y - y.mean()

    w_ridge = ridge_closed_form(X, y, alpha=10.0)
    sk_ridge = Ridge(alpha=10.0, fit_intercept=False).fit(X, y)
    check("ridge closed form == sklearn Ridge", True, np.allclose(w_ridge, sk_ridge.coef_))
    w_ols = np.linalg.lstsq(X, y, rcond=None)[0]
    check("ridge shrinks the weight vector: ||w_ridge|| < ||w_ols||", True, np.linalg.norm(w_ridge) < np.linalg.norm(w_ols))
    check("...but ridge sets no weight exactly to 0", 0, int(np.sum(w_ridge == 0)))

    w_lasso = lasso_coordinate_descent(X, y, alpha=0.3)
    sk_lasso = Lasso(alpha=0.3, fit_intercept=False).fit(X, y)
    check("lasso coordinate descent == sklearn Lasso", True, np.allclose(w_lasso, sk_lasso.coef_, atol=1e-4))
    kept = np.flatnonzero(np.abs(w_lasso) > 1e-10).tolist()
    show(f"lasso kept features {kept} with weights {np.round(w_lasso[kept], 2).tolist()}")
    check("lasso zeroes out most useless features (feature selection)", True, np.sum(w_lasso == 0) >= 12)
    check("lasso keeps the 3 real features", True, {0, 1, 2} <= set(kept))

    path = {a: int(np.sum(Lasso(alpha=a, fit_intercept=False).fit(X, y).coef_ != 0)) for a in (0.01, 0.1, 0.5, 2.0)}
    show(f"lasso: non-zero weights as alpha grows {path}")
    check("bigger alpha -> fewer features", True, list(path.values()) == sorted(path.values(), reverse=True))
    en = ElasticNet(alpha=0.3, l1_ratio=0.5, fit_intercept=False).fit(X, y)
    check("elastic net (half L1, half L2) also produces zeros", True, int(np.sum(en.coef_ == 0)) > 0)


# ---------------------------------------------------------------------------
# 5 + 6. non-linear models and a comparison table
# ---------------------------------------------------------------------------
def make_houses(n=2000, seed=3):
    """Synthetic house prices with the non-linearities real data has."""
    rng = np.random.default_rng(seed)
    area = rng.uniform(40, 250, n)                # square metres
    rooms = np.clip(np.round(area / 30 + rng.normal(0, 1, n)), 1, 8)
    age = rng.uniform(0, 60, n)
    dist = rng.exponential(8, n)                  # km to the city centre
    school = rng.uniform(1, 10, n)
    price = (2500 * area + 15000 * rooms - 800 * age
             + 300000 * np.exp(-dist / 5)          # location premium decays with distance
             + 20000 * (school > 7)                # good-school threshold effect
             + rng.normal(0, 30000, n))
    X = np.column_stack([area, rooms, age, dist, school])
    return X, price / 1000                         # in thousands


def compare_models():
    section("5-6. Non-linear models on house prices")
    X, y = make_houses()
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.25, random_state=0)
    models = {
        "mean baseline": None,
        "linear": make_pipeline(StandardScaler(), LinearRegression()),
        "ridge": make_pipeline(StandardScaler(), Ridge(alpha=1.0)),
        "KNN (k=10, scaled)": make_pipeline(StandardScaler(), KNeighborsRegressor(10)),
        "SVR (RBF)": make_pipeline(StandardScaler(), SVR(C=500, epsilon=5)),
        "decision tree (depth 6)": DecisionTreeRegressor(max_depth=6, random_state=0),
        "decision tree (no limit)": DecisionTreeRegressor(random_state=0),
        "random forest": RandomForestRegressor(300, random_state=0, n_jobs=-1),
        "gradient boosting": GradientBoostingRegressor(n_estimators=400, learning_rate=0.05, max_depth=3, random_state=0),
        "hist gradient boosting": HistGradientBoostingRegressor(max_iter=400, learning_rate=0.05, random_state=0),
    }
    rows = {}
    for name, model in models.items():
        if model is None:
            pred_tr, pred_te = np.full(len(y_tr), y_tr.mean()), np.full(len(y_te), y_tr.mean())
        else:
            model.fit(X_tr, y_tr)
            pred_tr, pred_te = model.predict(X_tr), model.predict(X_te)
        rows[name] = (mae(y_te, pred_te), rmse(y_te, pred_te), r2(y_te, pred_te), r2(y_tr, pred_tr))
    show(f"{'model':<26}{'test MAE':>10}{'test RMSE':>11}{'test R2':>9}{'train R2':>10}")
    for name, (a, b, c, d) in rows.items():
        show(f"{name:<26}{a:>10.1f}{b:>11.1f}{c:>9.3f}{d:>10.3f}")
    check("every real model beats the mean baseline", True, all(v[2] > 0.5 for k, v in rows.items() if k != "mean baseline"))
    check("an unlimited tree memorises the training set (train R2 = 1)", 1.0, round(rows["decision tree (no limit)"][3], 6))
    check("...and the forest of such trees generalises better than one", True, rows["random forest"][2] > rows["decision tree (no limit)"][2])
    check("boosting beats plain linear regression on non-linear data", True, rows["gradient boosting"][2] > rows["linear"][2])
    best = max(rows, key=lambda k: rows[k][2])
    show(f"best test R2: {best}")
    check("the best model is a tree ensemble", True, "boost" in best or "forest" in best)
    rf = models["random forest"]
    imp = dict(zip(["area", "rooms", "age", "dist", "school"], [round(float(v), 3) for v in rf.feature_importances_]))
    show(f"random-forest feature importances: {imp}")
    check("area is the most important feature", "area", max(imp, key=imp.get))


if __name__ == "__main__":
    from_scratch()
    metrics()
    polynomial()
    regularisation()
    compare_models()
    summary()
