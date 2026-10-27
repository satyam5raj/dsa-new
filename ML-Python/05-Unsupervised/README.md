# 05 · Unsupervised learning: finding structure without labels

> No one tells the model the right answer. It has to discover groups (clustering), simpler
> descriptions (dimensionality reduction), oddities (anomaly detection) or co-occurrences
> (association rules) on its own.

Run it: `../.venv/bin/python unsupervised.py` → **28/28 checks**.

Typical uses:

- customer segmentation;
- grouping similar documents or images;
- compressing features before another model;
- 2-D plots of high-dimensional data;
- fraud and defect detection when labels are scarce;
- "people who bought X also bought Y".

---

## 1. K-Means from scratch

### Intuition

Place k "centres". Repeat two steps until nothing changes:

1. **Assign:** every point joins its nearest centre.
2. **Update:** every centre moves to the mean of its points.

```python
labels = np.argmin(((X[:, None] - C[None]) ** 2).sum(-1), axis=1)   # assign
C = np.array([X[labels == j].mean(axis=0) for j in range(k)])        # update
```

Each step can only lower the **inertia** (the within-cluster sum of squared distances), so it
always converges, but to a **local** minimum. On 4 clean blobs the scratch version converged in
**5 iterations**, recovered the blobs perfectly, and matched sklearn's inertia (707.1).

### Initialisation matters

On 8 tighter blobs, out of 20 seeds, the run that got stuck in a clearly bad solution (inertia
over 1.2 × the good one):

| Init | Stuck runs |
|---|---|
| random points | **20 / 20** |
| **k-means++** (next centre chosen with probability ∝ distance² from existing centres) | 12 / 20 |
| **best of 10 k-means++ restarts** | matches sklearn's `KMeans(n_init=10)`: inertia **370.7** |

So k-means++ helps, but restarts are what make K-Means dependable. That's why sklearn defaults to
several `n_init` runs and keeps the best.

**K-Means assumptions:**

- clusters are **round** and of similar size;
- features are **scaled** (it uses Euclidean distance);
- you know k;
- each point belongs to exactly one cluster.

Complexity is `O(n·k·d)` per iteration, so it scales well; MiniBatchKMeans scales further.

## 2. Choosing k

| k | 2 | 3 | **4** | 5 | 6 | 7 |
|---|---|---|---|---|---|---|
| inertia | 2618 | 1337 | **707** | 631 | 571 | 512 |
| silhouette | 0.509 | 0.528 | **0.582** | 0.511 | 0.429 | 0.353 |

- **Inertia always falls** as k grows (k = n gives 0), so you can't just minimise it. Look for
  the **elbow**: the big drops stop after k = 4 (707 → 631 is small).
- **Silhouette** for each point is `(b − a) / max(a, b)`, where `a` is the mean distance to its
  own cluster and `b` the mean distance to the nearest other cluster. It ranges from −1 to 1.
  The average **peaks at k = 4**, the true value.
- In practice, also ask whether the segments are **useful** to the business.

## 3. Hierarchical clustering

Start with every point as its own cluster and repeatedly **merge the two closest clusters**
(agglomerative). The history is a tree, the **dendrogram**; cut it at any height to get any
number of clusters, with no need to fix k up front.

"Closest" depends on the **linkage**:

| Linkage | Distance between clusters | Blobs (ARI) | Behaviour |
|---|---|---|---|
| **ward** | increase in within-cluster variance | **1.000** | compact, K-Means-like |
| average | mean pairwise distance | 0.990 | balanced |
| single | closest pair | 0.560 on blobs, **1.000 on moons** | follows chains; one noisy bridge merges clusters |

(ARI = adjusted Rand index: 1 means it matches the true labels perfectly, 0 means it's random.)

Cost is `O(n²)` memory, so it suits thousands of points, not millions.

## 4. DBSCAN: clusters are dense regions

Parameters: `eps` (neighbourhood radius) and `min_samples`.

- A **core point** has at least `min_samples` neighbours within `eps`.
- Clusters are core points chained together, plus their border points.
- Everything else is **noise** (label `−1`).

On two interlocking moons:

| Method | ARI |
|---|---|
| K-Means (k=2) | **0.253**: cuts the moons with a straight line |
| **DBSCAN** (eps 0.2) | **1.000**, and it found the number of clusters (2) by itself |

Add 3 far-away points and DBSCAN labels them `[−1, −1, −1]`: noise detection for free.

**But `eps` is touchy.** At `eps = 0.05` the same two moons shatter into **31 clusters** with
43 % of points called noise. A standard way to set it: plot each point's distance to its
`min_samples`-th nearest neighbour, sorted, and pick the knee. DBSCAN also struggles when clusters
have **different densities** (HDBSCAN handles that).

## 5. Gaussian mixture models: soft clustering

Assume the data comes from k Gaussian blobs, each with its own mean, **covariance** (shape and
tilt) and weight. Fit them with **EM** (expectation–maximisation):

- **E-step:** for every point, compute the probability it came from each Gaussian.
- **M-step:** re-estimate each Gaussian from the points, weighted by those probabilities.

K-Means is the special case with round, equal clusters and hard assignments.

On two flat ellipses the GMM recovers both (ARI > 0.95) with weights ~50/50. A point exactly
between them gets **soft membership `[0.41, 0.59]`**, an honest "not sure" where K-Means would
force a hard choice. **BIC** (likelihood penalised by the number of parameters) picks **k = 2**.

Uses: soft segmentation, density estimation, anomaly scores (low likelihood = unusual).

## 6. Dimensionality reduction: PCA vs t-SNE

**sklearn digits:** 1,797 handwritten digits, 8 × 8 pixels = 64 dimensions.

**PCA** (linear, chapter 02):

- 2 components keep only **21.6 %** of the variance;
- **40** components keep 95 %.

| Input to KNN | Accuracy |
|---|---|
| all 64 dims | 0.946 |
| PCA-30 | **0.945**: half the dimensions, almost no loss |
| PCA-2 | 0.501: two straight-line directions can't separate 10 digits |

**t-SNE** (non-linear) places points in 2-D so that **neighbours stay neighbours**: it matches
probability distributions of pairwise similarities.

| 2-D embedding | Silhouette of the true digit labels |
|---|---|
| PCA | 0.055 (heavily overlapping) |
| **t-SNE** | **0.485** (clear islands per digit) |

| | PCA | t-SNE | UMAP |
|---|---|---|---|
| Type | linear | non-linear | non-linear |
| Preserves | global variance | local neighbourhoods | local + more global structure |
| Transform new data? | **yes** | no (re-run) | yes |
| Speed | very fast | slow | fast |
| Use for | compression, preprocessing, denoising | **visualisation only** | visualisation, sometimes features |

> **t-SNE pitfalls:** cluster *sizes* and the *distances between* clusters in the plot mean
> little. Results change with `perplexity` and the seed. Don't feed t-SNE output to a model.
> (UMAP isn't installed here; `umap-learn` has a sklearn-style API.)

## 7. Anomaly detection

The setup: 1,000 normal points plus 20 clearly far-away ones.

### Isolation Forest

Build random trees that split on random features at random thresholds. **Anomalies are easy to
isolate**: they need few splits, so they sit near the root. Score = average path length (shorter
= more anomalous).

With `contamination=0.02` it caught **95 %** of the anomalies at **90 %** precision. It's fast,
handles many dimensions, and needs no scaling.

### One-Class SVM

Trained on **normal data only**, it learns a boundary around it; anything outside is an anomaly.
Here it caught **100 %**, but also flagged 4.0 % of normal points. It's sensitive to `gamma` and
`nu`, and needs scaling.

Other options:

- z-score / IQR for single features (chapter 13);
- **Local Outlier Factor** (density compared to neighbours);
- **autoencoder** reconstruction error (chapter 10);
- GMM likelihood.

> With labels, even a few, supervised models usually win (chapter 04). Unsupervised detectors
> find *unusual*, which isn't always *bad*. A big legitimate purchase is unusual.

## 8. Association rules: market-basket analysis

5 baskets:

```
{bread, milk}  {bread, diapers, beer, eggs}  {milk, diapers, beer, cola}
{bread, milk, diapers, beer}  {bread, milk, diapers, cola}
```

| Measure | Formula | diapers → beer |
|---|---|---|
| **support** | fraction of baskets containing the items | `{diapers, beer}`: 3/5 = **0.6** |
| **confidence** | `support(A ∪ B) / support(A)` = P(B given A) | 0.6 / 0.8 = **0.75** |
| **lift** | `confidence / support(B)` | 0.75 / 0.6 = **1.25** (> 1: bought together more than by chance) |

**Apriori principle:** if an itemset is frequent, all its subsets are too. So you only extend
frequent itemsets, which prunes the search enormously. The frequent pairs at support ≥ 0.6 are
`(beer, diapers)`, `(bread, diapers)`, `(bread, milk)`, `(diapers, milk)`, all built from frequent
single items. (FP-Growth does the same without generating candidates; `mlxtend` implements both.)

Watch out for high confidence with lift ≈ 1. If everyone buys milk, "X → milk" has high
confidence but tells you nothing.

---

## Exercises

1. Why must you scale features before K-Means but not before Isolation Forest?
2. You run K-Means with k = 3 on data that's really two long, parallel stripes. What goes wrong,
   and what would you use instead?
3. A rule A → B has confidence 0.9 and lift 0.95. Is it useful?
4. Your t-SNE plot shows two clusters far apart. Can you say they're "very different"?

<details><summary>Answers</summary>

1. K-Means uses Euclidean distance, so a feature with large units dominates. Isolation Forest
   splits one feature at a time at random thresholds within that feature's own range, so scale
   doesn't matter.
2. K-Means makes round, Voronoi-shaped clusters and cuts across the stripes. Use DBSCAN or single
   linkage (shape-following), or a GMM with full covariance (elongated ellipses).
3. No. Lift < 1 means people who buy A are slightly **less** likely than average to buy B; B is
   just very popular.
4. Not reliably. t-SNE preserves local neighbourhoods; distances between clusters and cluster
   sizes are not faithful. Check in the original space or with PCA.
</details>

## Interview questions

**Q: Implement K-Means.** (Coding round favourite.) Init (k-means++); loop: assign by argmin
distance, recompute means, stop when centres stop moving; handle empty clusters; return labels,
centres and inertia. Mention restarts, scaling, choosing k, and the `O(nkd)` cost.

**Q: K-Means vs GMM vs DBSCAN?** K-Means: fast, round equal clusters, hard labels, needs k. GMM:
elliptical clusters, soft probabilities, needs k (BIC helps). DBSCAN: arbitrary shapes, finds k
and noise by itself, but is sensitive to `eps` and to varying densities.

**Q: How do you evaluate clustering without labels?** Internal metrics (silhouette,
Davies–Bouldin, inertia elbow), stability across seeds and subsamples, and above all whether the
clusters are actionable or interpretable. If some labels exist, ARI or NMI.

**Q: PCA vs t-SNE?** PCA is linear, fast, keeps global variance, and transforms new data: use it
for preprocessing. t-SNE is non-linear and keeps local neighbourhoods: use it for visualisation
only, and don't trust its global distances.

---

**Next:** [06 · Ensembles](../06-Ensembles/README.md)
