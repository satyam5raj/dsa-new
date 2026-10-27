"""
ML 15 : ML CODING-ROUND DRILLS
Run:  ../.venv/bin/python ml_coding_drills.py        (explanations: README.md)

The classic "implement it from scratch in 20-40 minutes" questions, written the way you'd write
them in an interview: NumPy only, short, correct, then checked against a library.

  D1  linear regression (gradient descent)      D7  decision tree classifier (recursive, Gini)
  D2  logistic regression (+ L2)                D8  PCA via eigen-decomposition
  D3  K-Means                                   D9  softmax cross-entropy + its gradient
  D4  KNN classifier                            D10 scaled dot-product attention
  D5  ROC-AUC (O(n log n), with ties)           D11 IoU + non-max suppression (vision)
  D6  stratified train/test split               D12 TF-IDF + cosine top-k search
"""
import sys
from collections import Counter
from pathlib import Path

import numpy as np
from sklearn.cluster import KMeans
from sklearn.datasets import load_iris, make_blobs, make_classification
from sklearn.decomposition import PCA
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LinearRegression, LogisticRegression
from sklearn.metrics import adjusted_rand_score, roc_auc_score
from sklearn.neighbors import KNeighborsClassifier
from sklearn.tree import DecisionTreeClassifier

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from mlcheck import check, near, section, show, summary  # noqa: E402


# D1 ------------------------------------------------------------------------
def linear_regression_gd(X, y, lr=0.1, epochs=1000):
    n, d = X.shape
    w, b = np.zeros(d), 0.0
    for _ in range(epochs):
        err = X @ w + b - y
        w -= lr * 2 / n * X.T @ err
        b -= lr * 2 / n * err.sum()
    return w, b


# D2 ------------------------------------------------------------------------
def logistic_regression(X, y, lr=0.5, epochs=2000, l2=0.0):
    n, d = X.shape
    w, b = np.zeros(d), 0.0
    for _ in range(epochs):
        p = 1 / (1 + np.exp(-(X @ w + b)))
        w -= lr * (X.T @ (p - y) / n + l2 * w)
        b -= lr * np.mean(p - y)
    return w, b


# D3 ------------------------------------------------------------------------
def kmeans(X, k, iters=100, seed=0):
    rng = np.random.default_rng(seed)
    C = X[rng.choice(len(X), k, replace=False)]
    for _ in range(iters):
        labels = ((X[:, None] - C[None]) ** 2).sum(-1).argmin(1)
        new = np.array([X[labels == j].mean(0) if (labels == j).any() else C[j] for j in range(k)])
        if np.allclose(new, C):
            break
        C = new
    return labels, C


# D4 ------------------------------------------------------------------------
def knn_predict(X_tr, y_tr, X_q, k):
    d = (X_q ** 2).sum(1)[:, None] - 2 * X_q @ X_tr.T + (X_tr ** 2).sum(1)[None]   # ||a-b||^2 without a 3-D tensor
    nn_idx = np.argpartition(d, k, axis=1)[:, :k]                                  # O(n) partial sort
    return np.array([Counter(y_tr[row]).most_common(1)[0][0] for row in nn_idx])


# D5 ------------------------------------------------------------------------
def roc_auc(y, s):
    """Mann-Whitney U via average ranks: handles ties, O(n log n)."""
    order = np.argsort(s, kind="mergesort")
    ranks = np.empty(len(s))
    sorted_s = s[order]
    i = 0
    while i < len(s):
        j = i
        while j + 1 < len(s) and sorted_s[j + 1] == sorted_s[i]:
            j += 1
        ranks[order[i:j + 1]] = (i + j) / 2 + 1          # average rank for a tie block
        i = j + 1
    n_pos, n_neg = y.sum(), len(y) - y.sum()
    return (ranks[y == 1].sum() - n_pos * (n_pos + 1) / 2) / (n_pos * n_neg)


# D6 ------------------------------------------------------------------------
def stratified_split(y, test_frac, seed=0):
    rng = np.random.default_rng(seed)
    test = []
    for c in np.unique(y):
        idx = rng.permutation(np.flatnonzero(y == c))
        test.extend(idx[:int(round(len(idx) * test_frac))])
    test = np.array(sorted(test))
    train = np.setdiff1d(np.arange(len(y)), test)
    return train, test


# D7 ------------------------------------------------------------------------
def gini(y):
    p = np.bincount(y) / len(y)
    return 1 - (p ** 2).sum()


def build_tree(X, y, depth=0, max_depth=3, min_samples=2):
    if depth == max_depth or len(y) < min_samples or gini(y) == 0:
        return {"leaf": np.bincount(y).argmax()}
    best = None
    for f in range(X.shape[1]):
        values = np.unique(X[:, f])
        for t in (values[:-1] + values[1:]) / 2:           # midpoints, like sklearn
            left = X[:, f] <= t
            score = (left.sum() * gini(y[left]) + (~left).sum() * gini(y[~left])) / len(y)
            if best is None or score < best[0]:
                best = (score, f, t, left)
    if best is None:
        return {"leaf": np.bincount(y).argmax()}
    _, f, t, left = best
    return {"f": f, "t": t, "l": build_tree(X[left], y[left], depth + 1, max_depth, min_samples),
            "r": build_tree(X[~left], y[~left], depth + 1, max_depth, min_samples)}


def tree_predict(node, x):
    while "leaf" not in node:
        node = node["l"] if x[node["f"]] <= node["t"] else node["r"]
    return node["leaf"]


# D8 ------------------------------------------------------------------------
def pca(X, k):
    Xc = X - X.mean(0)
    vals, vecs = np.linalg.eigh(np.cov(Xc.T))
    order = np.argsort(vals)[::-1][:k]
    return Xc @ vecs[:, order], vals[order] / vals.sum()


# D9 ------------------------------------------------------------------------
def softmax_ce(logits, y):
    z = logits - logits.max(1, keepdims=True)
    p = np.exp(z) / np.exp(z).sum(1, keepdims=True)
    loss = -np.log(p[np.arange(len(y)), y]).mean()
    grad = p.copy()
    grad[np.arange(len(y)), y] -= 1
    return loss, grad / len(y)


# D10 -----------------------------------------------------------------------
def attention(Q, K, V, causal=False):
    s = Q @ K.T / np.sqrt(Q.shape[1])
    if causal:
        s = np.where(np.tril(np.ones_like(s, dtype=bool)), s, -np.inf)
    w = np.exp(s - s.max(1, keepdims=True))
    w /= w.sum(1, keepdims=True)
    return w @ V


# D11 -----------------------------------------------------------------------
def iou(a, b):
    """boxes as [x1, y1, x2, y2]"""
    ix = max(0.0, min(a[2], b[2]) - max(a[0], b[0]))
    iy = max(0.0, min(a[3], b[3]) - max(a[1], b[1]))
    inter = ix * iy
    union = (a[2] - a[0]) * (a[3] - a[1]) + (b[2] - b[0]) * (b[3] - b[1]) - inter
    return inter / union


def nms(boxes, scores, thresh=0.5):
    order = list(np.argsort(-scores))
    keep = []
    while order:
        i = order.pop(0)
        keep.append(int(i))
        order = [j for j in order if iou(boxes[i], boxes[j]) < thresh]   # drop boxes overlapping the winner
    return keep


# D12 -----------------------------------------------------------------------
def tfidf(docs):
    vocab = sorted({w for d in docs for w in d.split()})
    index = {w: i for i, w in enumerate(vocab)}
    tf = np.zeros((len(docs), len(vocab)))
    for r, d in enumerate(docs):
        for w in d.split():
            tf[r, index[w]] += 1
    df = (tf > 0).sum(0)
    idf = np.log((1 + len(docs)) / (1 + df)) + 1          # sklearn's smooth idf
    m = tf * idf
    return m / np.linalg.norm(m, axis=1, keepdims=True), index, idf


def top_k(query_vec, M, k):
    sims = M @ query_vec
    return np.argsort(-sims)[:k].tolist()


# ---------------------------------------------------------------------------
def main():
    rng = np.random.default_rng(0)
    section("D1-D2  linear and logistic regression")
    X = rng.normal(size=(300, 3))
    y = X @ [1.5, -2.0, 0.7] + 3 + rng.normal(0, 0.1, 300)
    w, b = linear_regression_gd(X, y)
    sk = LinearRegression().fit(X, y)
    check("D1 linear regression == sklearn", True, np.allclose(w, sk.coef_, atol=1e-4) and abs(b - sk.intercept_) < 1e-4)
    Xc, yc = make_classification(500, n_features=4, random_state=0)
    w, b = logistic_regression(Xc, yc, epochs=5000)
    skl = LogisticRegression(penalty=None, max_iter=5000).fit(Xc, yc)
    check("D2 logistic regression == sklearn (no penalty)", True, np.allclose(w, skl.coef_[0], atol=0.02))

    section("D3-D4  K-Means and KNN")
    Xb, yb = make_blobs(400, centers=3, cluster_std=0.6, random_state=1)
    labels, _ = kmeans(Xb, 3)
    check("D3 K-Means recovers 3 blobs (ARI ~ 1)", True, adjusted_rand_score(yb, labels) > 0.98)
    near("D3 ...same inertia as sklearn", KMeans(3, n_init=10, random_state=0).fit(Xb).inertia_,
         ((Xb - np.array([Xb[labels == j].mean(0) for j in range(3)])[labels]) ** 2).sum(), 1.0)
    Xi, yi = load_iris(return_X_y=True)
    tr, te = stratified_split(yi, 0.3)
    ours = knn_predict(Xi[tr], yi[tr], Xi[te], 5)
    check("D4 KNN == sklearn KNeighborsClassifier", True, np.array_equal(ours, KNeighborsClassifier(5).fit(Xi[tr], yi[tr]).predict(Xi[te])))

    section("D5-D6  ROC-AUC with ties, stratified split")
    yy = rng.integers(0, 2, 2000)
    ss = np.round(rng.random(2000) + 0.3 * yy, 1)                       # rounding creates many ties
    check("D5 rank-based AUC == sklearn, even with many ties", True, np.isclose(roc_auc(yy, ss), roc_auc_score(yy, ss)))
    check("D6 stratified split keeps each class at 30% (15 of 50)", [15, 15, 15], np.bincount(yi[te]).tolist())
    check("D6 ...train and test don't overlap and cover everything", (0, 150), (len(np.intersect1d(tr, te)), len(tr) + len(te)))

    section("D7-D8  decision tree, PCA")
    tree = build_tree(Xi[tr], yi[tr], max_depth=3)
    ours = np.array([tree_predict(tree, x) for x in Xi[te]])
    sk_tree = DecisionTreeClassifier(max_depth=3, random_state=0).fit(Xi[tr], yi[tr])
    show(f"D7 tree accuracy: ours {np.mean(ours == yi[te]):.3f}, sklearn {sk_tree.score(Xi[te], yi[te]):.3f}")
    t_ = sk_tree.tree_
    l_, r_ = t_.children_left[0], t_.children_right[0]
    sk_root = (t_.n_node_samples[l_] * t_.impurity[l_] + t_.n_node_samples[r_] * t_.impurity[r_]) / t_.n_node_samples[0]
    left = Xi[tr][:, tree["f"]] <= tree["t"]
    our_root = (left.sum() * gini(yi[tr][left]) + (~left).sum() * gini(yi[tr][~left])) / len(tr)
    show(f"D7 root split: ours feature {tree['f']} <= {tree['t']:.2f}, sklearn feature {t_.feature[0]} <= {t_.threshold[0]:.2f} "
         "(petal length and width both isolate setosa: a tie, and sklearn shuffles features)")
    check("D7 our root split is exactly as good as sklearn's (same weighted Gini)", round(sk_root, 10), round(our_root, 10))
    check("D7 our tree classifies iris well (> 0.9)", True, np.mean(ours == yi[te]) > 0.9)
    Z, ratio = pca(Xi, 2)
    skp = PCA(2).fit(Xi)
    check("D8 explained-variance ratios == sklearn PCA", True, np.allclose(ratio, skp.explained_variance_ratio_))
    check("D8 components agree up to sign", True, np.allclose(np.abs(Z), np.abs(skp.transform(Xi)), atol=1e-8))

    section("D9-D10  softmax cross-entropy, attention")
    logits, y = rng.normal(size=(4, 5)), np.array([0, 3, 1, 4])
    loss, grad = softmax_ce(logits, y)
    num = np.zeros_like(logits)
    for i in range(4):
        for j in range(5):
            e = np.zeros_like(logits)
            e[i, j] = 1e-6
            num[i, j] = (softmax_ce(logits + e, y)[0] - softmax_ce(logits - e, y)[0]) / 2e-6
    check("D9 analytic gradient (p - onehot)/n == numerical", True, np.allclose(grad, num, atol=1e-7))
    Q = rng.normal(size=(6, 8))
    out = attention(Q, Q, Q, causal=True)
    check("D10 causal attention: first output = first value", True, np.allclose(out[0], Q[0]))
    import torch
    ref = torch.nn.functional.scaled_dot_product_attention(*(torch.tensor(Q)[None] for _ in range(3)), is_causal=True)[0].numpy()
    check("D10 == torch scaled_dot_product_attention", True, np.allclose(out, ref))

    section("D11-D12  IoU + NMS, TF-IDF search")
    check("D11 IoU of two half-overlapping unit squares = 1/3", round(1 / 3, 6), round(iou([0, 0, 2, 2], [1, 0, 3, 2]), 6))
    check("D11 IoU of disjoint boxes = 0", 0.0, iou([0, 0, 1, 1], [2, 2, 3, 3]))
    boxes = np.array([[0, 0, 10, 10], [1, 1, 11, 11], [20, 20, 30, 30], [21, 19, 31, 29], [50, 50, 55, 55]], float)
    scores = np.array([0.9, 0.8, 0.75, 0.95, 0.3])
    check("D11 NMS keeps the best box of each cluster", [3, 0, 4], nms(boxes, scores))
    docs = ["the cat sat on the mat", "dogs chase cats", "the stock market fell", "cat food and cat toys", "market prices rise"]
    M, index, idf = tfidf(docs)
    sk_m = TfidfVectorizer(token_pattern=r"\S+").fit_transform(docs).toarray()
    check("D12 TF-IDF matrix == sklearn TfidfVectorizer", True, np.allclose(M, sk_m))
    q = np.zeros(len(index))
    for w in "cat toys".split():
        q[index[w]] += idf[index[w]]
    q /= np.linalg.norm(q)
    check("D12 query 'cat toys' -> best match is the cat-toys document", 3, top_k(q, M, 1)[0])


if __name__ == "__main__":
    main()
    summary()
