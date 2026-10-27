"""
ML 02 : MATHS FOR ML (the parts you use every week)
Run:  ../.venv/bin/python math_for_ml.py        (explanations: README.md)

  1. Vectors       dot product, norms, cosine similarity, projection
  2. Matrices      matrix as a transformation, inverse, eigenvectors
  3. SVD and PCA   compress data, explained variance
  4. Calculus      derivatives, numerical gradient check, chain rule, gradient descent
  5. Probability   Bayes' rule, MLE, the normal distribution
  6. Statistics    bootstrap confidence interval, hypothesis test
  7. Information   entropy, cross-entropy, KL divergence
"""
import sys
from pathlib import Path

import numpy as np
from scipy import stats

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from mlcheck import check, near, section, show, summary  # noqa: E402


# ---------------------------------------------------------------------------
# 1. vectors
# ---------------------------------------------------------------------------
def cosine(a, b):
    return float(a @ b / (np.linalg.norm(a) * np.linalg.norm(b)))


def vectors():
    section("1. Vectors")
    a, b = np.array([3.0, 4.0]), np.array([4.0, 3.0])
    check("dot product 3*4 + 4*3", 24.0, a @ b)
    check("L2 norm of (3,4) (Pythagoras)", 5.0, np.linalg.norm(a))
    check("L1 norm of (3,4)", 7.0, np.linalg.norm(a, 1))
    near("cosine((3,4),(4,3)) = 24/25", 0.96, cosine(a, b), 1e-9)
    check("cosine of perpendicular vectors", 0.0, cosine(np.array([1.0, 0]), np.array([0, 2.0])))
    check("cosine ignores length: (1,1) vs (5,5)", 1.0, round(cosine(np.array([1.0, 1]), np.array([5.0, 5])), 12))
    u = np.array([1.0, 0.0])
    proj = (a @ u) / (u @ u) * u                          # shadow of a on u
    check("projection of (3,4) onto the x-axis", [3.0, 0.0], proj.tolist())
    check("the leftover (a - proj) is perpendicular to u", 0.0, (a - proj) @ u)


# ---------------------------------------------------------------------------
# 2. matrices
# ---------------------------------------------------------------------------
def matrices():
    section("2. Matrices")
    rotate90 = np.array([[0.0, -1.0], [1.0, 0.0]])
    check("a matrix is a transformation: rotate (1,0) by 90 degrees", [0.0, 1.0], (rotate90 @ np.array([1.0, 0.0])).tolist())
    A = np.array([[2.0, 1.0], [1.0, 3.0]])
    x_true = np.array([1.0, 2.0])
    y = A @ x_true
    check("solve A x = y (never compute inv(A) @ y in practice)", [1.0, 2.0], np.round(np.linalg.solve(A, y), 12).tolist())
    check("det(A) = 2*3 - 1*1", 5.0, round(np.linalg.det(A), 12))
    vals, vecs = np.linalg.eigh(A)
    v = vecs[:, 1]
    check("eigenvector: A v = lambda v (only stretched, not turned)", True, np.allclose(A @ v, vals[1] * v))
    near("largest eigenvalue of [[2,1],[1,3]] = (5+sqrt5)/2", (5 + 5 ** 0.5) / 2, vals[1], 1e-9)
    singular = np.array([[1.0, 2.0], [2.0, 4.0]])
    check("rank of a matrix whose rows are multiples", 1, np.linalg.matrix_rank(singular))


# ---------------------------------------------------------------------------
# 3. SVD and PCA
# ---------------------------------------------------------------------------
def svd_pca():
    section("3. SVD and PCA")
    rng = np.random.default_rng(0)
    t = rng.normal(size=500)
    X = np.column_stack([t, 2 * t + 0.1 * rng.normal(size=500), rng.normal(scale=0.1, size=500)])
    Xc = X - X.mean(axis=0)                               # PCA needs centred data
    U, S, Vt = np.linalg.svd(Xc, full_matrices=False)
    check("SVD reconstructs the data exactly: U S Vt = X", True, np.allclose(U @ np.diag(S) @ Vt, Xc))
    explained = S ** 2 / np.sum(S ** 2)
    show(f"explained variance ratio per component: {np.round(explained, 4).tolist()}")
    near("first principal component explains ~99.8% (the 3D data is really 1D)", 0.998, explained[0], 0.002)
    pc1 = Vt[0] * np.sign(Vt[0, 0])
    near("PC1 points along (1,2,0)/sqrt5: x-component", 1 / 5 ** 0.5, pc1[0], 0.01)
    X1 = (U[:, :1] * S[:1]) @ Vt[:1]                      # keep only 1 component
    err = np.linalg.norm(Xc - X1) / np.linalg.norm(Xc)
    near("compression error = sqrt(variance thrown away) = sqrt(1-0.9977)", np.sqrt(1 - explained[0]), err, 1e-9)
    cov_eig = np.sort(np.linalg.eigvalsh(np.cov(Xc.T)))[::-1]
    check("PCA two ways: eigenvalues of the covariance = S^2/(n-1)", True, np.allclose(cov_eig, S ** 2 / (len(X) - 1)))


# ---------------------------------------------------------------------------
# 4. calculus
# ---------------------------------------------------------------------------
def numerical_grad(f, x, h=1e-5):
    g = np.zeros_like(x)
    for i in range(len(x)):
        e = np.zeros_like(x)
        e[i] = h
        g[i] = (f(x + e) - f(x - e)) / (2 * h)            # central difference
    return g


def calculus():
    section("4. Calculus")
    f = lambda x: x ** 3                                   # noqa: E731
    near("derivative of x^3 at 2 is 3*2^2 = 12 (numerically)", 12.0, (f(2 + 1e-5) - f(2 - 1e-5)) / 2e-5, 1e-6)
    loss = lambda w: (w[0] - 3) ** 2 + 10 * (w[1] + 1) ** 2  # noqa: E731
    w = np.array([0.0, 0.0])
    analytic = np.array([2 * (w[0] - 3), 20 * (w[1] + 1)])
    check("gradient check: analytic == numerical", True, np.allclose(analytic, numerical_grad(loss, w), atol=1e-6))
    check("gradient at (0,0) = (-6, 20)", [-6.0, 20.0], analytic.tolist())
    # chain rule: d/dx sigmoid(3x + 1) = sigmoid'(z) * 3
    sig = lambda z: 1 / (1 + np.exp(-z))                    # noqa: E731
    x0 = 0.5
    z = 3 * x0 + 1
    chain = sig(z) * (1 - sig(z)) * 3
    near("chain rule: d/dx sigmoid(3x+1) at 0.5", chain, (sig(3 * (x0 + 1e-6) + 1) - sig(3 * (x0 - 1e-6) + 1)) / 2e-6, 1e-8)
    # gradient descent
    w = np.array([0.0, 0.0])
    for _ in range(200):
        w = w - 0.04 * np.array([2 * (w[0] - 3), 20 * (w[1] + 1)])
    check("gradient descent finds the minimum (3, -1)", [3.0, -1.0], np.round(w, 4).tolist())
    w_big = np.array([0.0, 0.0])
    for _ in range(20):
        w_big = w_big - 0.11 * np.array([2 * (w_big[0] - 3), 20 * (w_big[1] + 1)])
    check("learning rate 0.11 > 2/20: the steep direction explodes", True, abs(w_big[1]) > 10)


# ---------------------------------------------------------------------------
# 5. probability
# ---------------------------------------------------------------------------
def probability():
    section("5. Probability")
    # medical test: 1% have the disease, test catches 99%, false positive rate 5%
    p_d, sens, fpr = 0.01, 0.99, 0.05
    p_pos = sens * p_d + fpr * (1 - p_d)
    near("Bayes: P(disease | positive test) is only ~16.7%", 0.1667, sens * p_d / p_pos, 0.001)
    rng = np.random.default_rng(1)
    flips = rng.random(1000) < 0.3
    grid = np.linspace(0.01, 0.99, 99)
    log_lik = flips.sum() * np.log(grid) + (~flips).sum() * np.log(1 - grid)
    check("MLE: the bias that maximises the likelihood = fraction of heads", round(flips.mean(), 2), round(grid[np.argmax(log_lik)], 2))
    near("...which is close to the true 0.3", 0.3, flips.mean(), 0.03)
    heights = rng.normal(170, 8, 10_000)
    near("MLE of a normal's mean = sample mean", 170, heights.mean(), 0.3)
    near("about 68% of a normal lies within 1 std", 0.6827, np.mean(np.abs(heights - 170) < 8), 0.01)
    near("about 95% within 1.96 std", 0.95, np.mean(np.abs(heights - 170) < 1.96 * 8), 0.01)
    # law of large numbers: average of dice rolls -> 3.5
    rolls = rng.integers(1, 7, 100_000)
    near("law of large numbers: mean of 100K dice rolls -> 3.5", 3.5, rolls.mean(), 0.02)


# ---------------------------------------------------------------------------
# 6. statistics
# ---------------------------------------------------------------------------
def statistics():
    section("6. Statistics")
    rng = np.random.default_rng(2)
    a = rng.normal(0.70, 0.10, 200)          # model A accuracy on 200 cross-validated runs
    b = rng.normal(0.73, 0.10, 200)          # model B
    boots = [rng.choice(b, 200).mean() - rng.choice(a, 200).mean() for _ in range(2000)]
    lo, hi = np.percentile(boots, [2.5, 97.5])
    show(f"bootstrap 95% CI for (B - A): [{lo:.4f}, {hi:.4f}]")
    check("the CI excludes 0, so B is genuinely better", True, lo > 0)
    t, p = stats.ttest_ind(b, a)
    show(f"two-sample t-test: t = {t:.3f}, p = {p:.6f}")
    check("p-value < 0.05", True, p < 0.05)
    same = stats.ttest_ind(rng.normal(0.7, 0.1, 200), rng.normal(0.7, 0.1, 200)).pvalue
    show(f"two samples from the SAME distribution: p = {same:.3f}")
    # multiple testing: 100 tests on pure noise -> ~5 'significant' by chance
    false_hits = sum(stats.ttest_ind(rng.normal(size=50), rng.normal(size=50)).pvalue < 0.05 for _ in range(100))
    show(f"100 t-tests on pure noise: {false_hits} came out 'significant' at p<0.05")
    near("~5% false positives when nothing is real", 5, false_hits, 4)


# ---------------------------------------------------------------------------
# 7. information theory
# ---------------------------------------------------------------------------
def entropy(p):
    p = np.asarray(p, dtype=float)
    p = p[p > 0]
    return float(-(p * np.log2(p)).sum())


def cross_entropy(p, q):
    return float(-(np.asarray(p) * np.log(np.asarray(q))).sum())


def kl(p, q):
    p, q = np.asarray(p, float), np.asarray(q, float)
    return float((p * np.log(p / q)).sum())


def information():
    section("7. Information theory")
    check("entropy of a fair coin = 1 bit", 1.0, entropy([0.5, 0.5]))
    check("entropy of a fair 8-sided die = 3 bits", 3.0, entropy([1 / 8] * 8))
    near("entropy of a 90/10 coin (more predictable)", 0.469, entropy([0.9, 0.1]), 0.001)
    y = [0, 1, 0]                                           # true class = 1 (one-hot)
    near("cross-entropy loss, confident and right: -ln 0.9", 0.1054, cross_entropy(y, [0.05, 0.9, 0.05]), 1e-4)
    near("cross-entropy loss, confident and WRONG: -ln 0.05", 2.9957, cross_entropy(y, [0.9, 0.05, 0.05]), 1e-4)
    p, q = [0.5, 0.5], [0.9, 0.1]
    check("KL(p||p) = 0", 0.0, kl(p, p))
    check("KL is not symmetric: KL(p||q) != KL(q||p)", True, abs(kl(p, q) - kl(q, p)) > 0.1)
    near("cross-entropy = entropy + KL (in nats)", cross_entropy(p, q), entropy(p) * np.log(2) + kl(p, q), 1e-12)


if __name__ == "__main__":
    vectors()
    matrices()
    svd_pca()
    calculus()
    probability()
    statistics()
    information()
    summary()
