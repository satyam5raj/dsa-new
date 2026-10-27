"""
ML 05 : UNSUPERVISED LEARNING (no labels)
Run:  ../.venv/bin/python unsupervised.py        (explanations: README.md)

  1. K-Means from scratch      Lloyd's algorithm, k-means++ init, inertia
  2. Choosing k                elbow + silhouette
  3. Hierarchical clustering   linkage, dendrogram cut
  4. DBSCAN                    density clusters + noise, where K-Means fails
  5. Gaussian mixture          soft assignments
  6. PCA and t-SNE             dimensionality reduction on handwritten digits
  7. Anomaly detection         Isolation Forest, One-Class SVM
  8. Association rules         support, confidence, lift (Apriori idea)
"""
import sys
from itertools import combinations
from pathlib import Path

import numpy as np
from sklearn.cluster import DBSCAN, AgglomerativeClustering, KMeans
from sklearn.datasets import load_digits, make_blobs, make_moons
from sklearn.decomposition import PCA
from sklearn.ensemble import IsolationForest
from sklearn.manifold import TSNE
from sklearn.metrics import adjusted_rand_score, silhouette_score
from sklearn.mixture import GaussianMixture
from sklearn.neighbors import KNeighborsClassifier
from sklearn.preprocessing import StandardScaler
from sklearn.svm import OneClassSVM

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from mlcheck import check, near, section, show, summary  # noqa: E402


# ---------------------------------------------------------------------------
# 1. K-Means from scratch
# ---------------------------------------------------------------------------
def kmeans_pp_init(X, k, rng):
    """k-means++: first centre at random, each next one far from the existing centres (prob ∝ distance^2)."""
    centres = [X[rng.integers(len(X))]]
    for _ in range(k - 1):
        d2 = np.min(((X[:, None] - np.array(centres)[None]) ** 2).sum(-1), axis=1)
        centres.append(X[rng.choice(len(X), p=d2 / d2.sum())])
    return np.array(centres)


def kmeans(X, k, init="++", seed=0, max_iter=100):
    rng = np.random.default_rng(seed)
    C = kmeans_pp_init(X, k, rng) if init == "++" else X[rng.choice(len(X), k, replace=False)]
    for it in range(max_iter):
        labels = np.argmin(((X[:, None] - C[None]) ** 2).sum(-1), axis=1)     # assignment step
        new_C = np.array([X[labels == j].mean(axis=0) if np.any(labels == j) else C[j] for j in range(k)])  # update step
        if np.allclose(new_C, C):
            break
        C = new_C
    inertia = float(((X - C[labels]) ** 2).sum())
    return labels, C, inertia, it + 1


def kmeans_section():
    section("1. K-Means from scratch")
    X, y = make_blobs(600, centers=4, cluster_std=0.8, random_state=0)
    labels, C, inertia, iters = kmeans(X, 4)
    show(f"converged in {iters} iterations, inertia {inertia:.1f}")
    check("finds the 4 true blobs (adjusted Rand index = 1 is perfect)", True, adjusted_rand_score(y, labels) > 0.95)
    sk = KMeans(4, n_init=10, random_state=0).fit(X)
    near("inertia matches sklearn KMeans", sk.inertia_, inertia, 1.0)
    # random init can get stuck in a bad local minimum; k-means++ is far more reliable
    X8, _ = make_blobs(800, centers=8, cluster_std=0.5, random_state=4)
    bad_random = sum(kmeans(X8, 8, init="random", seed=s)[2] > 1.2 * kmeans(X8, 8, seed=0)[2] for s in range(20))
    bad_pp = sum(kmeans(X8, 8, init="++", seed=s)[2] > 1.2 * kmeans(X8, 8, seed=0)[2] for s in range(20))
    show(f"out of 20 seeds, runs stuck in a bad local minimum: random init {bad_random}, k-means++ {bad_pp}")
    check("k-means++ gets stuck less often than random init", True, bad_pp < bad_random)
    best_of_10 = min(kmeans(X8, 8, seed=s)[2] for s in range(10))
    sk8 = KMeans(8, n_init=10, random_state=0).fit(X8).inertia_
    show(f"best of 10 k-means++ restarts: inertia {best_of_10:.1f}; sklearn KMeans(n_init=10): {sk8:.1f}")
    check("...so always keep the best of several restarts (sklearn's n_init)", True, best_of_10 <= 1.01 * sk8)


# ---------------------------------------------------------------------------
# 2. choosing k
# ---------------------------------------------------------------------------
def choose_k():
    section("2. Choosing k: elbow and silhouette")
    X, _ = make_blobs(600, centers=4, cluster_std=0.8, random_state=0)
    inertias, sils = {}, {}
    for k in range(2, 8):
        km = KMeans(k, n_init=10, random_state=0).fit(X)
        inertias[k], sils[k] = km.inertia_, silhouette_score(X, km.labels_)
    show("k: inertia / silhouette   " + "  ".join(f"{k}: {inertias[k]:.0f}/{sils[k]:.3f}" for k in inertias))
    check("inertia always falls as k grows (so it can't pick k alone)", True, all(inertias[k] > inertias[k + 1] for k in range(2, 7)))
    drops = {k: inertias[k - 1] - inertias[k] for k in range(3, 8)}
    check("elbow: the last big drop is at k=4", 4, max(k for k in drops if drops[k] > 0.25 * drops[3]))
    check("silhouette peaks at the true k=4", 4, max(sils, key=sils.get))


# ---------------------------------------------------------------------------
# 3-4. hierarchical and DBSCAN
# ---------------------------------------------------------------------------
def hierarchical_and_dbscan():
    section("3-4. Hierarchical clustering and DBSCAN")
    X, y = make_blobs(300, centers=3, cluster_std=0.7, random_state=2)
    for link in ("ward", "average", "single"):
        ari = adjusted_rand_score(y, AgglomerativeClustering(3, linkage=link).fit_predict(X))
        show(f"agglomerative ({link:<7}) ARI on blobs: {ari:.3f}")
    check("ward linkage recovers round blobs", True, adjusted_rand_score(y, AgglomerativeClustering(3, linkage="ward").fit_predict(X)) > 0.95)

    Xm, ym = make_moons(500, noise=0.06, random_state=0)
    km = KMeans(2, n_init=10, random_state=0).fit_predict(Xm)
    db = DBSCAN(eps=0.2, min_samples=5).fit_predict(Xm)
    single = AgglomerativeClustering(2, linkage="single").fit_predict(Xm)
    show(f"two moons ARI: K-Means {adjusted_rand_score(ym, km):.3f}, DBSCAN {adjusted_rand_score(ym, db):.3f}, single linkage {adjusted_rand_score(ym, single):.3f}")
    check("K-Means fails on moons (it assumes round clusters)", True, adjusted_rand_score(ym, km) < 0.6)
    check("DBSCAN follows the shape", True, adjusted_rand_score(ym, db) > 0.95)
    check("DBSCAN found exactly 2 clusters", 2, len(set(db) - {-1}))
    Xn = np.vstack([Xm, [[3.0, 3.0], [-2.5, 2.5], [3.5, -2.0]]])                # add 3 far-away points
    dbn = DBSCAN(eps=0.2, min_samples=5).fit_predict(Xn)
    check("DBSCAN labels far-away points as noise (-1)", [-1, -1, -1], dbn[-3:].tolist())
    too_small = DBSCAN(eps=0.05, min_samples=5).fit_predict(Xm)
    show(f"with eps too small (0.05): {len(set(too_small) - {-1})} clusters, {np.mean(too_small == -1):.0%} of points called noise")
    check("eps is the sensitive knob: too small shatters 2 moons into many clusters", True, len(set(too_small) - {-1}) > 10)


# ---------------------------------------------------------------------------
# 5. Gaussian mixture
# ---------------------------------------------------------------------------
def gmm():
    section("5. Gaussian mixture model (soft clustering)")
    rng = np.random.default_rng(0)
    X = np.vstack([rng.normal([0, 0], [1.0, 0.3], (300, 2)), rng.normal([0, 2], [1.0, 0.3], (300, 2))])  # two flat ellipses
    y = np.repeat([0, 1], 300)
    g = GaussianMixture(2, covariance_type="full", random_state=0).fit(X)
    check("GMM recovers the two elliptical clusters", True, adjusted_rand_score(y, g.predict(X)) > 0.95)
    near("learned weights are ~50/50", 0.5, g.weights_.min(), 0.05)
    p_mid = g.predict_proba([[0.0, 1.0]])[0]
    show(f"a point exactly between the clusters gets soft membership {np.round(p_mid, 2).tolist()}")
    near("...about 50/50 (K-Means would force a hard choice)", 0.5, p_mid.max(), 0.15)
    bics = {k: GaussianMixture(k, random_state=0).fit(X).bic(X) for k in (1, 2, 3, 4)}
    check("BIC (lower = better) picks k = 2", 2, min(bics, key=bics.get))


# ---------------------------------------------------------------------------
# 6. PCA and t-SNE
# ---------------------------------------------------------------------------
def dim_reduction():
    section("6. Dimensionality reduction on 8x8 digits (64 dimensions)")
    X, y = load_digits(return_X_y=True)
    Xs = StandardScaler().fit_transform(X)
    pca = PCA().fit(Xs)
    cum = np.cumsum(pca.explained_variance_ratio_)
    k95 = int(np.searchsorted(cum, 0.95) + 1)
    show(f"PCA: 2 components keep {cum[1]:.1%} of the variance; {k95} components keep 95%")
    check("95% of the variance needs far fewer than 64 dimensions", True, k95 < 45)
    knn = lambda Z: np.mean(KNeighborsClassifier(5).fit(Z[:1200], y[:1200]).predict(Z[1200:]) == y[1200:])  # noqa: E731
    acc_full = knn(Xs)
    acc_pca30 = knn(PCA(30, random_state=0).fit(Xs[:1200]).transform(Xs))
    acc_pca2 = knn(PCA(2, random_state=0).fit(Xs[:1200]).transform(Xs))
    show(f"KNN accuracy: 64 dims {acc_full:.3f}, PCA-30 {acc_pca30:.3f}, PCA-2 {acc_pca2:.3f}")
    check("PCA-30 loses almost nothing", True, acc_pca30 > acc_full - 0.02)
    check("2 linear components are not enough to separate 10 digits", True, acc_pca2 < 0.75)
    emb = TSNE(2, perplexity=30, random_state=0, init="pca").fit_transform(Xs)
    sil_tsne = silhouette_score(emb, y)
    sil_pca = silhouette_score(PCA(2).fit_transform(Xs), y)
    show(f"silhouette of the true digit labels in 2-D: PCA {sil_pca:.3f}, t-SNE {sil_tsne:.3f}")
    check("t-SNE separates the digits far better in 2-D (non-linear)", True, sil_tsne > sil_pca + 0.3)


# ---------------------------------------------------------------------------
# 7. anomaly detection
# ---------------------------------------------------------------------------
def anomalies():
    section("7. Anomaly detection")
    rng = np.random.default_rng(1)
    normal = rng.normal(0, 1, (1000, 2))
    weird = rng.uniform(-6, 6, (40, 2))
    weird = weird[np.linalg.norm(weird, axis=1) > 4][:20]                     # keep 20 clearly far points
    X = np.vstack([normal, weird])
    y = np.r_[np.zeros(1000), np.ones(len(weird))]
    iso = IsolationForest(n_estimators=200, contamination=0.02, random_state=0).fit(X)
    flagged = iso.predict(X) == -1
    recall, precision = flagged[y == 1].mean(), y[flagged].mean()
    show(f"Isolation Forest: caught {recall:.0%} of anomalies, precision {precision:.0%}")
    check("Isolation Forest catches most anomalies", True, recall >= 0.9)
    score = -iso.score_samples(X)
    check("anomalies get higher scores on average (fewer splits to isolate)", True, score[y == 1].mean() > score[y == 0].mean() + 0.1)
    oc = OneClassSVM(nu=0.02, gamma=0.3).fit(normal)                          # trained on normal data only
    oc_flag = oc.predict(X) == -1
    show(f"One-Class SVM (trained on normal only): caught {oc_flag[y == 1].mean():.0%}, flagged {oc_flag[y == 0].mean():.1%} of normal points")
    check("One-Class SVM catches the anomalies too", True, oc_flag[y == 1].mean() >= 0.9)


# ---------------------------------------------------------------------------
# 8. association rules
# ---------------------------------------------------------------------------
def association_rules():
    section("8. Association rules (market basket)")
    baskets = [
        {"bread", "milk"}, {"bread", "diapers", "beer", "eggs"}, {"milk", "diapers", "beer", "cola"},
        {"bread", "milk", "diapers", "beer"}, {"bread", "milk", "diapers", "cola"},
    ]
    n = len(baskets)
    support = lambda items: sum(set(items) <= b for b in baskets) / n   # noqa: E731
    check("support({diapers, beer}) = 3/5", 0.6, support({"diapers", "beer"}))
    conf = support({"diapers", "beer"}) / support({"diapers"})
    check("confidence(diapers -> beer) = 0.6/0.8", 0.75, round(conf, 10))
    lift = conf / support({"beer"})
    check("lift(diapers -> beer) = 0.75/0.6 (> 1 = bought together more than chance)", 1.25, round(lift, 10))
    items = sorted(set().union(*baskets))
    frequent_pairs = [p for p in combinations(items, 2) if support(p) >= 0.6]
    show(f"frequent pairs (support >= 0.6): {frequent_pairs}")
    check("Apriori idea: every frequent pair is made of frequent single items", True,
          all(support({a}) >= 0.6 and support({b}) >= 0.6 for a, b in frequent_pairs))


if __name__ == "__main__":
    kmeans_section()
    choose_k()
    hierarchical_and_dbscan()
    gmm()
    dim_reduction()
    anomalies()
    association_rules()
    summary()
