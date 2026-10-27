"""
ML 16 : CLASSICAL ALGORITHMS FROM SCRATCH (the ones earlier chapters only called from a library)
Run:  ../.venv/bin/python classical_from_scratch.py        (explanations: README.md)

  1. Perceptron                       the first learning algorithm; mistake-driven updates
  2. Linear SVM (Pegasos)             hinge loss + L2 by stochastic sub-gradient descent
  3. Kernel trick + kernel perceptron explicit feature map == kernel; non-linear boundary
  4. LDA                              shared-covariance Gaussian classes, closed form
  5. Multinomial naive Bayes          text classification with Laplace smoothing
  6. CART regression tree             recursive variance-reduction splits
  7. XGBoost-style boosting           gradients AND hessians, regularised leaf weights, split gain
  8. DBSCAN                           core points, region growing, noise
  9. Gaussian mixture via EM          E-step / M-step, log-likelihood never decreases
 10. Agglomerative clustering         average linkage, merge until k clusters
 11. Isolation Forest                 random splits, path length, c(n) normalisation
 12. Apriori                          frequent itemsets of any size with candidate pruning
 13. t-SNE                            pairwise affinities, KL gradient descent
"""
import sys
from collections import Counter
from itertools import combinations
from pathlib import Path

import numpy as np
from sklearn.cluster import DBSCAN, AgglomerativeClustering
from sklearn.datasets import load_digits, make_blobs, make_circles, make_classification, make_moons
from sklearn.decomposition import PCA
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.manifold import trustworthiness
from sklearn.metrics import adjusted_rand_score, roc_auc_score
from sklearn.mixture import GaussianMixture
from sklearn.model_selection import train_test_split
from sklearn.naive_bayes import MultinomialNB
from sklearn.svm import SVC, LinearSVC
from sklearn.tree import DecisionTreeRegressor

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from mlcheck import check, near, section, show, summary  # noqa: E402


# ---------------------------------------------------------------------------
# 1. perceptron
# ---------------------------------------------------------------------------
def perceptron(X, y, epochs=100):
    """y in {-1,+1}. On every mistake: w += y x. Stops when an epoch has no mistakes."""
    w, b = np.zeros(X.shape[1]), 0.0
    mistakes = []
    for _ in range(epochs):
        m = 0
        for xi, yi in zip(X, y):
            if yi * (xi @ w + b) <= 0:                  # wrong side (or on the line)
                w += yi * xi
                b += yi
                m += 1
        mistakes.append(m)
        if m == 0:
            break
    return w, b, mistakes


def perceptron_demo():
    section("1. Perceptron")
    X, y01 = make_blobs(200, centers=[[-2, -2], [2, 2]], cluster_std=0.8, random_state=0)
    y = 2 * y01 - 1
    w, b, mistakes = perceptron(X, y)
    show(f"mistakes per epoch: {mistakes}")
    check("on separable data it converges to zero mistakes", 0, mistakes[-1])
    check("...and classifies every training point correctly", 1.0, np.mean(np.sign(X @ w + b) == y))
    Xx = np.array([[0, 0], [0, 1], [1, 0], [1, 1]], float)
    _, _, m_xor = perceptron(Xx, np.array([-1, 1, 1, -1]), epochs=50)
    check("on XOR it never converges (no line separates it)", True, m_xor[-1] > 0)


# ---------------------------------------------------------------------------
# 2. linear SVM with Pegasos
# ---------------------------------------------------------------------------
def pegasos(X, y, lam=0.01, epochs=50, seed=0):
    """minimise lam/2 ||w||^2 + mean(max(0, 1 - y w.x)) by SGD with step 1/(lam t).
    The bias is folded in as a constant feature of 1 (so it is lightly regularised too)."""
    rng = np.random.default_rng(seed)
    X1 = np.column_stack([X, np.ones(len(X))])
    w, t = np.zeros(X1.shape[1]), 0
    for _ in range(epochs):
        for i in rng.permutation(len(X1)):
            t += 1
            eta = 1.0 / (lam * t)
            if y[i] * (X1[i] @ w) < 1:                  # inside the margin or wrong: hinge is active
                w = (1 - eta * lam) * w + eta * y[i] * X1[i]
            else:
                w = (1 - eta * lam) * w                 # only the L2 shrink
    return w[:-1], w[-1]


def svm_demo():
    section("2. Linear SVM (Pegasos: hinge loss + L2)")
    X, y01 = make_classification(1000, n_features=5, n_informative=3, class_sep=1.5, random_state=1)
    y = 2 * y01 - 1
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.3, random_state=0)
    w, b = pegasos(X_tr, y_tr)
    ours = np.mean(np.sign(X_te @ w + b) == y_te)
    sk = LinearSVC(C=1 / (0.01 * len(X_tr)), max_iter=20000).fit(X_tr, y_tr).score(X_te, y_te)
    show(f"test accuracy: Pegasos {ours:.3f}, sklearn LinearSVC {sk:.3f}")
    near("Pegasos matches LinearSVC", sk, ours, 0.03)
    margins = y_tr * (X_tr @ w + b)
    sv = np.mean(margins <= 1 + 1e-9)
    show(f"{sv:.1%} of training points have margin <= 1 (the support vectors decide the boundary)")
    check("the hinge loss ignores points beyond the margin: they are a minority that matter", True, 0 < sv < 0.6)
    near("geometric margin width = 2 / ||w||", 2 / np.linalg.norm(w), 2 / np.sqrt(w @ w), 1e-12)


# ---------------------------------------------------------------------------
# 3. the kernel trick + kernel perceptron
# ---------------------------------------------------------------------------
def rbf(A, B, gamma):
    return np.exp(-gamma * ((A[:, None] - B[None]) ** 2).sum(-1))


def kernel_perceptron(X, y, gamma, epochs=20):
    """Dual form: f(x) = sum_i alpha_i y_i K(x_i, x). Only kernel values are needed, never phi(x)."""
    K = rbf(X, X, gamma)
    alpha = np.zeros(len(X))
    for _ in range(epochs):
        for i in range(len(X)):
            if y[i] * ((alpha * y) @ K[:, i]) <= 0:
                alpha[i] += 1
    return alpha


def kernel_demo():
    section("3. Kernel trick + kernel perceptron")
    x, z = np.array([1.0, 2.0]), np.array([3.0, -1.0])
    phi = lambda v: np.array([1, np.sqrt(2) * v[0], np.sqrt(2) * v[1], v[0] ** 2, np.sqrt(2) * v[0] * v[1], v[1] ** 2])  # noqa: E731
    check("polynomial kernel (x.z + 1)^2 == phi(x).phi(z) with the explicit 6-D map", True, np.isclose((x @ z + 1) ** 2, phi(x) @ phi(z)))
    X, y01 = make_circles(400, noise=0.08, factor=0.4, random_state=0)
    y = 2 * y01 - 1
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.3, random_state=0)
    _, _, m = perceptron(X_tr, y_tr, epochs=30)
    alpha = kernel_perceptron(X_tr, y_tr, gamma=2.0)
    pred = np.sign((alpha * y_tr) @ rbf(X_tr, X_te, 2.0))
    lin_w, lin_b, _ = perceptron(X_tr, y_tr, epochs=30)
    lin_acc = np.mean(np.sign(X_te @ lin_w + lin_b) == y_te)
    k_acc = np.mean(pred == y_te)
    svc = SVC(kernel="rbf", gamma=2.0).fit(X_tr, y_tr).score(X_te, y_te)
    show(f"concentric circles test accuracy: linear perceptron {lin_acc:.3f}, RBF kernel perceptron {k_acc:.3f}, sklearn RBF SVC {svc:.3f}")
    check("a linear boundary fails on circles", True, lin_acc < 0.7)
    check("the kernel version separates them", True, k_acc > 0.95)
    show(f"kernel perceptron uses {int(np.sum(alpha > 0))} of {len(X_tr)} training points (non-zero alpha)")


# ---------------------------------------------------------------------------
# 4. LDA
# ---------------------------------------------------------------------------
def lda_fit(X, y):
    classes = np.unique(y)
    means = np.array([X[y == c].mean(0) for c in classes])
    priors = np.array([np.mean(y == c) for c in classes])
    S = sum((X[y == c] - means[i]).T @ (X[y == c] - means[i]) for i, c in enumerate(classes)) / (len(X) - len(classes))
    S_inv = np.linalg.inv(S)
    W = means @ S_inv                                          # linear discriminant: x^T S^-1 mu_k - 1/2 mu_k^T S^-1 mu_k + log pi_k
    b = -0.5 * np.einsum("kd,kd->k", W, means) + np.log(priors)
    return classes, W, b


def lda_demo():
    section("4. Linear discriminant analysis")
    X, y = make_classification(900, n_features=6, n_informative=4, n_redundant=0, n_classes=3, n_clusters_per_class=1, random_state=2)
    classes, W, b = lda_fit(X, y)
    ours = classes[np.argmax(X @ W.T + b, axis=1)]
    sk = LinearDiscriminantAnalysis().fit(X, y).predict(X)
    check("LDA from scratch makes the same predictions as sklearn", 1.0, np.mean(ours == sk))
    show(f"training accuracy {np.mean(ours == y):.3f}")
    Xr, _ = make_classification(900, n_features=6, n_informative=4, n_redundant=2, random_state=2)   # 2 features = combos of others
    Xr_c = Xr - Xr.mean(0)
    cond = np.linalg.cond(Xr_c.T @ Xr_c)
    show(f"with 2 redundant (linearly dependent) features the covariance condition number is {cond:.1e}")
    check("redundant features make the covariance singular: inv() is meaningless (sklearn uses SVD)", True, cond > 1e12)


# ---------------------------------------------------------------------------
# 5. multinomial naive Bayes
# ---------------------------------------------------------------------------
def mnb_fit(counts, y, alpha=1.0):
    classes = np.unique(y)
    log_prior = np.log(np.array([np.mean(y == c) for c in classes]))
    word_counts = np.array([counts[y == c].sum(0) for c in classes]) + alpha      # Laplace smoothing
    log_lik = np.log(word_counts / word_counts.sum(1, keepdims=True))
    return classes, log_prior, log_lik


def naive_bayes_text():
    section("5. Multinomial naive Bayes for text")
    docs = ["free money win prize now", "win a free prize today", "claim your free money", "cheap pills free offer",
            "meeting at noon tomorrow", "project report due monday", "lunch meeting with the team", "please review the report"]
    y = np.array([1, 1, 1, 1, 0, 0, 0, 0])                      # 1 = spam
    vec = CountVectorizer()
    C = vec.fit_transform(docs).toarray()
    classes, lp, ll = mnb_fit(C, y)
    test = vec.transform(["free prize meeting", "team report review", "money money money"]).toarray()
    ours = classes[np.argmax(test @ ll.T + lp, axis=1)]
    sk = MultinomialNB(alpha=1.0).fit(C, y)
    check("scratch multinomial NB == sklearn MultinomialNB", True, np.allclose(test @ ll.T + lp, sk.predict_joint_log_proba(test)))
    check("predictions for 3 new messages (spam, ham, spam)", [1, 0, 1], ours.tolist())
    no_smooth = np.array([C[y == c].sum(0) for c in classes])
    zero = int(np.sum(no_smooth[0] == 0))
    show(f"without smoothing {zero} words have P(word | ham) = 0, so ONE such word would make P(ham | message) = 0")
    check("Laplace smoothing keeps every word probability > 0", True, bool(np.all(np.isfinite(ll))))


# ---------------------------------------------------------------------------
# 6. CART regression tree
# ---------------------------------------------------------------------------
def cart(X, y, depth=0, max_depth=3, min_leaf=5):
    if depth == max_depth or len(y) < 2 * min_leaf:
        return {"value": y.mean()}
    best = (np.sum((y - y.mean()) ** 2), None, None)
    for f in range(X.shape[1]):
        order = np.argsort(X[:, f])
        xs, ys = X[order, f], y[order]
        csum, csq = np.cumsum(ys), np.cumsum(ys ** 2)          # prefix sums -> O(n) per feature after sorting
        for i in range(min_leaf, len(y) - min_leaf + 1):
            if xs[i - 1] == xs[i]:
                continue
            nl, nr = i, len(y) - i
            sse_l = csq[i - 1] - csum[i - 1] ** 2 / nl
            sse_r = (csq[-1] - csq[i - 1]) - (csum[-1] - csum[i - 1]) ** 2 / nr
            if sse_l + sse_r < best[0] - 1e-12:
                best = (sse_l + sse_r, f, (xs[i - 1] + xs[i]) / 2)
    if best[1] is None:
        return {"value": y.mean()}
    _, f, t = best
    m = X[:, f] <= t
    return {"f": f, "t": t, "l": cart(X[m], y[m], depth + 1, max_depth, min_leaf), "r": cart(X[~m], y[~m], depth + 1, max_depth, min_leaf)}


def cart_predict(node, X):
    out = np.empty(len(X))
    for i, x in enumerate(X):
        n = node
        while "value" not in n:
            n = n["l"] if x[n["f"]] <= n["t"] else n["r"]
        out[i] = n["value"]
    return out


def cart_demo():
    section("6. CART regression tree")
    rng = np.random.default_rng(3)
    X = rng.uniform(0, 10, (400, 3))
    y = np.sin(X[:, 0]) * 3 + (X[:, 1] > 5) * 2 + rng.normal(0, 0.3, 400)
    tree = cart(X, y, max_depth=4, min_leaf=5)
    sk = DecisionTreeRegressor(max_depth=4, min_samples_leaf=5, random_state=0).fit(X, y)
    Xq = rng.uniform(0, 10, (200, 3))
    check("CART from scratch == sklearn DecisionTreeRegressor (same depth, min leaf)", True, np.allclose(cart_predict(tree, Xq), sk.predict(Xq)))
    check("root split uses the most useful feature (x0 or x1)", True, tree["f"] in (0, 1))


# ---------------------------------------------------------------------------
# 7. XGBoost-style second-order boosting
# ---------------------------------------------------------------------------
def xgb_tree(X, g, h, depth, max_depth, lam, gamma, min_child_h=1.0):
    G, H = g.sum(), h.sum()
    if depth == max_depth:
        return {"w": -G / (H + lam)}                          # Newton step for this leaf
    best = (0.0, None, None)
    for f in range(X.shape[1]):
        order = np.argsort(X[:, f])
        xs, gs, hs = X[order, f], np.cumsum(g[order]), np.cumsum(h[order])
        for i in range(1, len(xs)):
            if xs[i - 1] == xs[i] or hs[i - 1] < min_child_h or H - hs[i - 1] < min_child_h:
                continue
            GL, HL = gs[i - 1], hs[i - 1]
            gain = 0.5 * (GL ** 2 / (HL + lam) + (G - GL) ** 2 / (H - HL + lam) - G ** 2 / (H + lam)) - gamma
            if gain > best[0]:
                best = (gain, f, (xs[i - 1] + xs[i]) / 2)
    if best[1] is None:
        return {"w": -G / (H + lam)}
    _, f, t = best
    m = X[:, f] <= t
    return {"f": f, "t": t, "l": xgb_tree(X[m], g[m], h[m], depth + 1, max_depth, lam, gamma, min_child_h),
            "r": xgb_tree(X[~m], g[~m], h[~m], depth + 1, max_depth, lam, gamma, min_child_h)}


def xgb_predict_tree(node, X):
    out = np.empty(len(X))
    for i, x in enumerate(X):
        n = node
        while "w" not in n:
            n = n["l"] if x[n["f"]] <= n["t"] else n["r"]
        out[i] = n["w"]
    return out


def xgb_fit(X, y, rounds=60, eta=0.3, max_depth=3, lam=1.0, gamma=0.0):
    F = np.zeros(len(y))                                       # log-odds
    trees = []
    for _ in range(rounds):
        p = 1 / (1 + np.exp(-F))
        g, h = p - y, p * (1 - p)                              # 1st and 2nd derivative of log loss wrt F
        t = xgb_tree(X, g, h, 0, max_depth, lam, gamma)
        F += eta * xgb_predict_tree(t, X)
        trees.append(t)
    return trees


def xgb_demo():
    section("7. XGBoost-style boosting (second-order)")
    g, h, lam = np.array([0.4, -0.2, 0.3]), np.array([0.24, 0.16, 0.21]), 1.0
    check("leaf weight w* = -G/(H + lambda)", round(-0.5 / (0.61 + 1.0), 6), round(xgb_tree(np.zeros((3, 1)), g, h, 0, 0, lam, 0)["w"], 6))
    near("best objective of a leaf = -G^2 / (2 (H + lambda))", -0.5 ** 2 / (2 * 1.61), np.sum(g) * (-0.5 / 1.61) + 0.5 * (np.sum(h) + lam) * (-0.5 / 1.61) ** 2, 1e-12)
    X, y = make_classification(1200, n_features=6, n_informative=4, random_state=4)
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.3, random_state=0)
    trees = xgb_fit(X_tr, y_tr)
    F = sum(0.3 * xgb_predict_tree(t, X_te) for t in trees)
    auc = roc_auc_score(y_te, F)
    sk = roc_auc_score(y_te, HistGradientBoostingClassifier(max_iter=60, learning_rate=0.3, max_depth=3, random_state=0).fit(X_tr, y_tr).predict_proba(X_te)[:, 1])
    show(f"test ROC-AUC: scratch XGBoost-style {auc:.3f}, sklearn HistGradientBoosting {sk:.3f}")
    near("scratch second-order boosting is competitive with the library", sk, auc, 0.03)
    big_gamma = xgb_tree(X_tr, *(lambda p: (p - y_tr, p * (1 - p)))(np.full(len(y_tr), 0.5)), 0, 3, 1.0, gamma=1e6)
    check("a huge gamma (min split gain) prunes the tree to a single leaf", True, "w" in big_gamma)


# ---------------------------------------------------------------------------
# 8. DBSCAN
# ---------------------------------------------------------------------------
def dbscan(X, eps, min_samples):
    n = len(X)
    D = np.sqrt(((X[:, None] - X[None]) ** 2).sum(-1))
    neighbours = [np.flatnonzero(D[i] <= eps) for i in range(n)]   # includes the point itself (as sklearn does)
    core = np.array([len(nb) >= min_samples for nb in neighbours])
    labels = np.full(n, -1)
    cluster = 0
    for i in range(n):
        if labels[i] != -1 or not core[i]:
            continue
        labels[i] = cluster
        stack = [i]
        while stack:                                           # grow the region through core points
            p = stack.pop()
            for q in neighbours[p]:
                if labels[q] == -1:
                    labels[q] = cluster
                    if core[q]:
                        stack.append(q)
        cluster += 1
    return labels, core


def dbscan_demo():
    section("8. DBSCAN")
    X, _ = make_moons(400, noise=0.07, random_state=1)
    X = np.vstack([X, [[2.5, 2.0], [-1.8, -1.2]]])
    ours, core = dbscan(X, 0.2, 5)
    sk = DBSCAN(eps=0.2, min_samples=5).fit_predict(X)
    check("scratch DBSCAN == sklearn (ARI = 1)", 1.0, round(adjusted_rand_score(ours, sk), 6))
    check("same noise points", True, np.array_equal(ours == -1, sk == -1))
    show(f"{len(set(ours) - {-1})} clusters, {int(core.sum())} core points, {int(np.sum(ours == -1))} noise points")


# ---------------------------------------------------------------------------
# 9. GMM via EM
# ---------------------------------------------------------------------------
def gauss_logpdf(X, mu, cov):
    d = X.shape[1]
    L = np.linalg.cholesky(cov)
    z = np.linalg.solve(L, (X - mu).T)
    return -0.5 * (z ** 2).sum(0) - np.log(np.diag(L)).sum() - 0.5 * d * np.log(2 * np.pi)


def gmm_em(X, k, iters=100, seed=0):
    rng = np.random.default_rng(seed)
    n, d = X.shape
    mu = X[rng.choice(n, k, replace=False)]
    cov = np.array([np.cov(X.T) for _ in range(k)])
    pi = np.full(k, 1 / k)
    lls = []
    for _ in range(iters):
        logp = np.column_stack([np.log(pi[j]) + gauss_logpdf(X, mu[j], cov[j]) for j in range(k)])   # E-step
        mx = logp.max(1, keepdims=True)
        log_norm = mx[:, 0] + np.log(np.exp(logp - mx).sum(1))
        lls.append(float(log_norm.sum()))
        r = np.exp(logp - log_norm[:, None])                   # responsibilities
        Nk = r.sum(0)                                          # M-step
        pi = Nk / n
        mu = (r.T @ X) / Nk[:, None]
        cov = np.array([((r[:, j, None] * (X - mu[j])).T @ (X - mu[j])) / Nk[j] + 1e-6 * np.eye(d) for j in range(k)])
        if len(lls) > 1 and abs(lls[-1] - lls[-2]) < 1e-8:
            break
    return pi, mu, cov, r, lls


def gmm_demo():
    section("9. Gaussian mixture via EM")
    rng = np.random.default_rng(5)
    X = np.vstack([rng.multivariate_normal([0, 0], [[2, 1.5], [1.5, 2]], 300), rng.multivariate_normal([5, 0], [[0.5, 0], [0, 2]], 200)])
    y = np.repeat([0, 1], [300, 200])
    pi, mu, cov, r, lls = gmm_em(X, 2)
    check("EM's log-likelihood never decreases", True, all(b >= a - 1e-8 for a, b in zip(lls, lls[1:])))
    check("it recovers the two clusters", True, adjusted_rand_score(y, r.argmax(1)) > 0.95)
    sk = GaussianMixture(2, covariance_type="full", random_state=0, tol=1e-8, max_iter=500).fit(X)
    near("final average log-likelihood == sklearn GaussianMixture", sk.score(X), lls[-1] / len(X), 1e-3)
    show(f"converged in {len(lls)} iterations; weights {np.round(np.sort(pi), 3).tolist()} (true 0.4 / 0.6)")


# ---------------------------------------------------------------------------
# 10. agglomerative clustering (average linkage)
# ---------------------------------------------------------------------------
def agglomerative_average(X, k):
    clusters = {i: [i] for i in range(len(X))}
    D = np.sqrt(((X[:, None] - X[None]) ** 2).sum(-1))
    merges = []
    while len(clusters) > k:
        keys = list(clusters)
        best = (np.inf, None, None)
        for a, b in combinations(keys, 2):
            dist = D[np.ix_(clusters[a], clusters[b])].mean()   # average linkage
            if dist < best[0]:
                best = (dist, a, b)
        d, a, b = best
        clusters[a] = clusters[a] + clusters.pop(b)
        merges.append(d)
    labels = np.empty(len(X), int)
    for c, members in enumerate(clusters.values()):
        labels[members] = c
    return labels, merges


def agglomerative_demo():
    section("10. Agglomerative clustering (average linkage)")
    X, _ = make_blobs(60, centers=3, cluster_std=0.6, random_state=6)
    ours, _ = agglomerative_average(X, 3)
    _, merges = agglomerative_average(X, 1)                    # merge all the way to one cluster: the full dendrogram
    sk = AgglomerativeClustering(3, linkage="average").fit_predict(X)
    check("scratch == sklearn AgglomerativeClustering(linkage='average')", 1.0, round(adjusted_rand_score(ours, sk), 6))
    check("merge distances grow (monotone for average linkage): a dendrogram", True, all(b >= a - 1e-9 for a, b in zip(merges, merges[1:])))
    show(f"last four merge heights: {np.round(merges[-4:], 2).tolist()}")
    check("the big jump in merge height is just before 3 -> 2 clusters: cut there for k = 3", True, merges[-2] > 2 * merges[-3])


# ---------------------------------------------------------------------------
# 11. Isolation Forest
# ---------------------------------------------------------------------------
def c_factor(n):
    """average path length of an unsuccessful BST search: normalises path lengths for sample size n"""
    return 0.0 if n <= 1 else 2 * (np.log(n - 1) + 0.5772156649) - 2 * (n - 1) / n


def build_itree(X, depth, max_depth, rng):
    if depth >= max_depth or len(X) <= 1:
        return {"size": len(X)}
    f = rng.integers(X.shape[1])
    lo, hi = X[:, f].min(), X[:, f].max()
    if lo == hi:
        return {"size": len(X)}
    t = rng.uniform(lo, hi)                                    # random feature, random threshold
    m = X[:, f] < t
    return {"f": f, "t": t, "l": build_itree(X[m], depth + 1, max_depth, rng), "r": build_itree(X[~m], depth + 1, max_depth, rng)}


def path_length(node, x, depth=0):
    if "size" in node:
        return depth + c_factor(node["size"])                  # unfinished subtree: add its expected depth
    return path_length(node["l"] if x[node["f"]] < node["t"] else node["r"], x, depth + 1)


def isolation_forest(X, n_trees=100, sample=256, seed=0):
    rng = np.random.default_rng(seed)
    max_depth = int(np.ceil(np.log2(sample)))
    trees = [build_itree(X[rng.choice(len(X), min(sample, len(X)), replace=False)], 0, max_depth, rng) for _ in range(n_trees)]
    def score(Q):
        E = np.array([[path_length(t, q) for t in trees] for q in Q]).mean(1)
        return 2 ** (-E / c_factor(min(sample, len(X))))       # ~1 = anomaly, ~0.5 or less = normal
    return score


def iforest_demo():
    section("11. Isolation Forest")
    near("c(256) ~ 10.24 (expected path length for 256 points)", 10.24, c_factor(256), 0.01)
    rng = np.random.default_rng(7)
    X = np.vstack([rng.normal(0, 1, (500, 2)), rng.uniform(4, 6, (10, 2)) * rng.choice([-1, 1], (10, 2))])
    y = np.r_[np.zeros(500), np.ones(10)]
    s = isolation_forest(X)(X)
    show(f"mean anomaly score: normal points {s[y == 0].mean():.3f}, anomalies {s[y == 1].mean():.3f}")
    check("anomalies score higher (shorter paths)", True, s[y == 1].mean() > s[y == 0].mean() + 0.15)
    check("ROC-AUC of the scratch Isolation Forest", True, roc_auc_score(y, s) > 0.95)


# ---------------------------------------------------------------------------
# 12. Apriori
# ---------------------------------------------------------------------------
def apriori(baskets, min_support):
    n = len(baskets)
    support = lambda s: sum(s <= b for b in baskets) / n      # noqa: E731
    items = sorted(set().union(*baskets))
    level = [frozenset([i]) for i in items if support(frozenset([i])) >= min_support]
    frequent = {s: support(s) for s in level}
    k = 2
    candidates_tried = 0
    while level:
        prev = set(level)
        cands = {a | b for a in level for b in level if len(a | b) == k}
        cands = {c for c in cands if all(frozenset(sub) in prev for sub in combinations(c, k - 1))}   # Apriori pruning
        candidates_tried += len(cands)
        level = [c for c in cands if support(c) >= min_support]
        frequent.update({c: support(c) for c in level})
        k += 1
    return frequent, candidates_tried


def apriori_demo():
    section("12. Apriori (itemsets of any size)")
    rng = np.random.default_rng(8)
    catalogue = list("ABCDEFGHIJ")
    baskets = []
    for _ in range(300):
        b = set(rng.choice(catalogue, rng.integers(1, 5), replace=False))
        if rng.random() < 0.3:
            b |= {"A", "B", "C"}                              # a frequently co-bought trio
        baskets.append(frozenset(b))
    freq, tried = apriori(baskets, 0.2)
    brute = {frozenset(c): sum(frozenset(c) <= b for b in baskets) / 300 for r in range(1, 5) for c in combinations(catalogue, r)}
    brute = {s: v for s, v in brute.items() if v >= 0.2}
    check("Apriori finds exactly the same frequent itemsets as brute force", True, freq == brute)
    check("...including the trio {A, B, C}", True, frozenset("ABC") in freq)
    show(f"Apriori counted {tried} candidates of size >= 2; brute force over sizes 2-4 checks {sum(1 for r in range(2, 5) for _ in combinations(catalogue, r))}")


# ---------------------------------------------------------------------------
# 13. t-SNE
# ---------------------------------------------------------------------------
def tsne(X, perplexity=30, iters=500, seed=0):
    n = len(X)
    D = ((X[:, None] - X[None]) ** 2).sum(-1)
    P = np.zeros((n, n))
    target = np.log(perplexity)
    for i in range(n):                                         # binary-search each point's Gaussian width
        lo, hi, beta = 1e-20, 1e20, 1.0
        for _ in range(50):
            p = np.exp(-D[i] * beta)
            p[i] = 0
            p /= p.sum()
            H = -np.sum(p[p > 0] * np.log(p[p > 0]))
            if abs(H - target) < 1e-5:
                break
            if H > target:
                lo = beta
                beta = beta * 2 if hi == 1e20 else (beta + hi) / 2
            else:
                hi = beta
                beta = (beta + lo) / 2
        P[i] = p
    P = (P + P.T) / (2 * n)
    P = np.maximum(P, 1e-12)
    rng = np.random.default_rng(seed)
    Y = rng.normal(0, 1e-4, (n, 2))
    vel = np.zeros_like(Y)
    for it in range(iters):
        num = 1 / (1 + ((Y[:, None] - Y[None]) ** 2).sum(-1))  # Student-t kernel in 2-D: heavy tails
        np.fill_diagonal(num, 0)
        Q = np.maximum(num / num.sum(), 1e-12)
        exag = 12 if it < 100 else 1                           # early exaggeration forms clusters first
        grad = 4 * (((exag * P - Q) * num)[:, :, None] * (Y[:, None] - Y[None])).sum(1)
        vel = (0.5 if it < 100 else 0.8) * vel - 200 * grad
        Y += vel
    kl = float(np.sum(P * np.log(P / Q)))
    return Y, kl


def tsne_demo():
    section("13. t-SNE")
    X, y = load_digits(return_X_y=True)
    idx = np.random.default_rng(9).choice(len(X), 400, replace=False)
    X, y = X[idx] / 16, y[idx]
    Y, kl = tsne(X)
    pca = PCA(2).fit_transform(X)
    tw_t, tw_p = trustworthiness(X, Y, n_neighbors=10), trustworthiness(X, pca, n_neighbors=10)
    from sklearn.neighbors import KNeighborsClassifier
    from sklearn.model_selection import cross_val_score
    knn_t = cross_val_score(KNeighborsClassifier(5), Y, y, cv=5).mean()
    knn_p = cross_val_score(KNeighborsClassifier(5), pca, y, cv=5).mean()
    show(f"final KL(P||Q) = {kl:.3f}; trustworthiness t-SNE {tw_t:.3f} vs PCA {tw_p:.3f}; 5-NN accuracy in 2-D: t-SNE {knn_t:.3f} vs PCA {knn_p:.3f}")
    check("scratch t-SNE preserves neighbourhoods better than PCA", True, tw_t > tw_p + 0.05)
    check("digits form separable islands in the 2-D map", True, knn_t > knn_p + 0.2)


if __name__ == "__main__":
    perceptron_demo()
    svm_demo()
    kernel_demo()
    lda_demo()
    naive_bayes_text()
    cart_demo()
    xgb_demo()
    dbscan_demo()
    gmm_demo()
    agglomerative_demo()
    iforest_demo()
    apriori_demo()
    tsne_demo()
    summary()
