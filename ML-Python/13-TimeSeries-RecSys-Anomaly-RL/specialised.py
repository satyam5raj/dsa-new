"""
ML 13 : TIME SERIES, RECOMMENDER SYSTEMS, ANOMALY DETECTION, REINFORCEMENT LEARNING
Run:  ../.venv/bin/python specialised.py        (explanations: README.md)

  1. Time series      baselines, moving average, exponential smoothing, Holt-Winters, AR, lag features
  2. Backtesting      walk-forward evaluation, MAE / MAPE / MASE
  3. Recommenders     popularity, matrix factorisation (ALS from scratch), precision@k, NDCG@k, cold start
  4. Anomalies        z-score vs robust z (MAD), rolling detection, LOF
  5. RL: bandits      epsilon-greedy vs UCB, regret
  6. RL: Q-learning   grid world, compared against value iteration
  7. RL: policy grad  REINFORCE, and PPO's clipped objective
"""
import sys
from pathlib import Path

import numpy as np
from sklearn.ensemble import HistGradientBoostingRegressor
from sklearn.neighbors import LocalOutlierFactor

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from mlcheck import check, near, section, show, summary  # noqa: E402


# ---------------------------------------------------------------------------
# 1-2. time series
# ---------------------------------------------------------------------------
def make_series(n=730, seed=0):
    """Two years of daily sales: trend + weekly pattern + yearly wave + noise."""
    rng = np.random.default_rng(seed)
    t = np.arange(n)
    weekly = np.array([0, -5, -3, 0, 8, 25, 18])[t % 7]          # weekends sell more
    yearly = 15 * np.sin(2 * np.pi * t / 365)
    return 200 + 0.1 * t + weekly + yearly + rng.normal(0, 6, n)


def holt_winters(y, season=7, alpha=0.3, beta=0.01, gamma=0.2, horizon=1):
    """Additive Holt-Winters: level + trend + seasonal component, each updated by exponential smoothing."""
    level, trend = y[:season].mean(), (y[season:2 * season].mean() - y[:season].mean()) / season
    seas = list(y[:season] - level)
    for t in range(len(y)):
        s = seas[t % season] if t >= season else seas[t]
        new_level = alpha * (y[t] - s) + (1 - alpha) * (level + trend)
        trend = beta * (new_level - level) + (1 - beta) * trend
        seas[t % season] = gamma * (y[t] - new_level) + (1 - gamma) * s
        level = new_level
    n = len(y)
    return np.array([level + (h + 1) * trend + seas[(n + h) % season] for h in range(horizon)])


def lag_features(y, lags=(1, 7, 14), window=7):
    rows, targets = [], []
    for t in range(max(lags) + window, len(y)):
        rows.append([y[t - L] for L in lags] + [y[t - window:t].mean(), t % 7])
        targets.append(y[t])
    return np.array(rows), np.array(targets)


def time_series():
    section("1-2. Time series forecasting + walk-forward backtest")
    y = make_series()
    lag1 = np.corrcoef(y[1:], y[:-1])[0, 1]
    lag7 = np.corrcoef(np.diff(y)[7:], np.diff(y)[:-7])[0, 1]
    show(f"autocorrelation: lag 1 {lag1:.3f}; of the differenced series at lag 7 {lag7:.3f}")
    check("strong weekly seasonality shows up as lag-7 autocorrelation", True, lag7 > 0.3)
    check("differencing removes the trend (mean of the diff ~ the slope 0.1)", True, abs(np.diff(y).mean() - 0.1) < 0.1)

    # walk-forward: for each of the last 120 days, forecast tomorrow using ONLY the past
    start = 610
    preds = {k: [] for k in ("naive", "seasonal naive", "moving avg 7", "exp smoothing", "Holt-Winters", "AR(14)", "boosting on lags")}
    Xl, yl = lag_features(y)
    offset = len(y) - len(yl)
    for t in range(start, len(y)):
        hist = y[:t]
        preds["naive"].append(hist[-1])
        preds["seasonal naive"].append(hist[-7])
        preds["moving avg 7"].append(hist[-7:].mean())
        s = hist[0]
        for v in hist[1:]:
            s = 0.3 * v + 0.7 * s
        preds["exp smoothing"].append(s)
        preds["Holt-Winters"].append(holt_winters(hist)[0])
        if (t - start) % 30 == 0:                                 # refit monthly (as in production)
            p = 14
            A = np.column_stack([np.ones(t - p)] + [hist[p - k:t - k] for k in range(1, p + 1)])
            ar_coef = np.linalg.lstsq(A, hist[p:], rcond=None)[0]
            gbm = HistGradientBoostingRegressor(max_iter=200, random_state=0).fit(Xl[:t - offset], yl[:t - offset])
        preds["AR(14)"].append(ar_coef[0] + ar_coef[1:] @ hist[-1:-15:-1])
        preds["boosting on lags"].append(gbm.predict(Xl[t - offset:t - offset + 1])[0])
    actual = y[start:]
    naive_mae_train = np.mean(np.abs(y[7:start] - y[:start - 7]))  # MASE scale: seasonal-naive (y[t-7]) error on training data
    res = {}
    for k, v in preds.items():
        v = np.array(v)
        res[k] = (np.mean(np.abs(actual - v)), 100 * np.mean(np.abs((actual - v) / actual)), np.mean(np.abs(actual - v)) / naive_mae_train)
    show(f"{'model':<20}{'MAE':>8}{'MAPE %':>8}{'MASE':>7}")
    for k, (a, b, c) in res.items():
        show(f"{k:<20}{a:>8.2f}{b:>8.2f}{c:>7.2f}")
    check("models that know the weekly pattern beat the plain naive forecast", True,
          all(res[k][0] < res["naive"][0] for k in ("Holt-Winters", "AR(14)", "boosting on lags")))
    check("the moving average blurs the weekend peak (worse than seasonal naive)", True, res["moving avg 7"][0] > res["seasonal naive"][0])
    best = min(res, key=lambda k: res[k][0])
    show(f"best: {best}")
    check("MASE < 1 means better than the seasonal-naive baseline", True, res[best][2] < 1)
    near("seasonal naive itself scores MASE ~1 by construction", 1.0, res["seasonal naive"][2], 0.1)
    check("the best model's error is near the noise level (std 6 -> MAE ~4.8)", True, res[best][0] < 6.5)


# ---------------------------------------------------------------------------
# 3. recommender systems
# ---------------------------------------------------------------------------
def make_ratings(n_users=300, n_items=200, k=4, density=0.1, seed=1):
    rng = np.random.default_rng(seed)
    U, V = rng.normal(size=(n_users, k)), rng.normal(size=(n_items, k))
    item_bias = rng.normal(0, 0.5, n_items)
    full = 3 + U @ V.T * 0.5 + item_bias + rng.normal(0, 0.3, (n_users, n_items))
    mask = rng.random((n_users, n_items)) < density
    return np.clip(full, 1, 5), mask


def als(R, mask, k=4, lam=1.0, iters=15, seed=0):
    """Alternating least squares: fix items -> solve each user exactly (ridge); fix users -> solve each item."""
    rng = np.random.default_rng(seed)
    n_u, n_i = R.shape
    mu = R[mask].mean()
    U, V = rng.normal(0, 0.1, (n_u, k)), rng.normal(0, 0.1, (n_i, k))
    Rc = R - mu
    for _ in range(iters):
        for u in range(n_u):
            idx = mask[u]
            Vi = V[idx]
            U[u] = np.linalg.solve(Vi.T @ Vi + lam * np.eye(k), Vi.T @ Rc[u, idx])
        for i in range(n_i):
            idx = mask[:, i]
            Uu = U[idx]
            V[i] = np.linalg.solve(Uu.T @ Uu + lam * np.eye(k), Uu.T @ Rc[idx, i])
    return mu, U, V


def ndcg_at_k(ranked, relevant, k):
    dcg = sum(1 / np.log2(i + 2) for i, item in enumerate(ranked[:k]) if item in relevant)
    ideal = sum(1 / np.log2(i + 2) for i in range(min(k, len(relevant))))
    return dcg / ideal if ideal else 0.0


def recommenders():
    section("3. Recommender systems")
    check("NDCG@3: hits at ranks 1 and 3 out of 2 relevant", round((1 + 1 / 2) / (1 + 1 / np.log2(3)), 4), round(ndcg_at_k([7, 9, 4], {7, 4}, 3), 4))
    R, mask = make_ratings()
    rng = np.random.default_rng(2)
    test = mask & (rng.random(mask.shape) < 0.2)
    train = mask & ~test
    mu, U, V = als(R, train)
    pred = mu + U @ V.T
    item_mean = np.array([R[train[:, i], i].mean() if train[:, i].any() else mu for i in range(R.shape[1])])
    rmse = lambda P: float(np.sqrt(np.mean((P[test] - R[test]) ** 2)))   # noqa: E731
    r_global, r_item, r_als = rmse(np.full_like(R, mu)), rmse(np.tile(item_mean, (R.shape[0], 1))), rmse(pred)
    show(f"held-out rating RMSE: global mean {r_global:.3f}, item mean (popularity) {r_item:.3f}, ALS matrix factorisation {r_als:.3f}")
    check("matrix factorisation beats the popularity baseline", True, r_als < r_item - 0.1)

    # ranking evaluation: order each user's HELD-OUT items; graded relevance gain = 2^rating - 1
    def ndcg_graded(scores, ratings, k=5):
        gains = 2 ** ratings - 1
        dcg = sum(g / np.log2(i + 2) for i, g in enumerate(gains[np.argsort(-scores)][:k]))
        ideal = sum(g / np.log2(i + 2) for i, g in enumerate(np.sort(gains)[::-1][:k]))
        return dcg / ideal

    r_rng = np.random.default_rng(0)
    nd = {"random": [], "popularity": [], "MF": []}
    for u in range(R.shape[0]):
        items = np.flatnonzero(test[u])
        if len(items) < 5:
            continue
        for name, sc in (("random", r_rng.random(len(items))), ("popularity", item_mean[items]), ("MF", pred[u, items])):
            nd[name].append(ndcg_graded(sc, R[u, items]))
    show(f"NDCG@5 ranking each user's held-out items ({len(nd['MF'])} users): " + ", ".join(f"{k} {np.mean(v):.3f}" for k, v in nd.items()))
    check("personalised ranking beats 'most popular for everyone', which beats random", True, np.mean(nd["MF"]) > np.mean(nd["popularity"]) > np.mean(nd["random"]))

    # cold start: a brand-new user with no ratings gets a zero vector -> popularity is all MF can offer
    U_new = np.linalg.solve(V[[]].T @ V[[]] + 1.0 * np.eye(4), np.zeros(4))
    check("cold start: a user with no history gets a zero embedding (needs a fallback)", [0.0] * 4, U_new.tolist())
    # two-tower retrieval = the same dot product, served by nearest-neighbour search over item vectors
    scores = V @ U[0]
    top5_exact = np.argsort(-scores)[:5]
    check("retrieval: top items by user . item dot product", top5_exact.tolist(), np.argsort(-(U[0] @ V.T))[:5].tolist())


# ---------------------------------------------------------------------------
# 4. anomaly detection
# ---------------------------------------------------------------------------
def anomalies():
    section("4. Anomaly detection")
    rng = np.random.default_rng(3)
    x = rng.normal(100, 10, 1000)
    x[:20] = rng.uniform(300, 400, 20)                               # 20 big anomalies (2% contamination)
    z = (x - x.mean()) / x.std()
    robust = 0.6745 * (x - np.median(x)) / np.median(np.abs(x - np.median(x)))
    caught_z, caught_r = np.mean(np.abs(z[:20]) > 3), np.mean(np.abs(robust[:20]) > 3.5)
    show(f"std inflated by the anomalies: {x.std():.1f} (true 10); median absolute deviation stays honest")
    show(f"caught: plain z-score > 3: {caught_z:.0%}; robust (MAD) z > 3.5: {caught_r:.0%}")
    check("robust z-score catches all the anomalies", 1.0, caught_r)
    check("robust z-score has few false alarms on normal points", True, np.mean(np.abs(robust[20:]) > 3.5) < 0.01)

    # rolling detection on a seasonal series: a global threshold misses a spike that is only unusual LOCALLY
    y = make_series(400, seed=4)
    spike_day = 300
    y[spike_day] += 35
    window = 28
    resid = np.array([y[t] - np.median(y[t - window:t][(np.arange(t - window, t) % 7) == t % 7]) for t in range(window, len(y))])
    mad = np.median(np.abs(resid - np.median(resid)))
    rz = 0.6745 * (resid - np.median(resid)) / mad
    flagged = np.flatnonzero(np.abs(rz) > 4) + window
    global_z = np.abs((y - y.mean()) / y.std())
    show(f"injected +35 on day {spike_day}: global z there = {global_z[spike_day]:.2f}; seasonal residual z = {rz[spike_day - window]:.2f}; flagged days {flagged.tolist()}")
    check("a global threshold misses the local spike", True, global_z[spike_day] < 3)
    check("comparing with the same weekday's recent median catches it", True, spike_day in flagged.tolist())

    pts = np.vstack([rng.normal(0, 0.3, (200, 2)), rng.normal(5, 2.0, (200, 2)), [[1.5, 1.5]]])
    lof = LocalOutlierFactor(n_neighbors=20).fit(pts)
    score = -lof.negative_outlier_factor_
    show(f"LOF score of a point just outside the TIGHT cluster: {score[-1]:.2f} (typical point ~1)")
    check("LOF flags points that are unusual relative to their LOCAL density", True, score[-1] > 2)


# ---------------------------------------------------------------------------
# 5. bandits
# ---------------------------------------------------------------------------
def bandits():
    section("5. Multi-armed bandits: explore vs exploit")
    true_p = np.array([0.05, 0.04, 0.06, 0.10, 0.045])                # click rates of 5 ad variants
    T = 20000

    def run(policy, seed):
        rng = np.random.default_rng(seed)
        counts, wins, regret = np.zeros(5), np.zeros(5), 0.0
        for t in range(T):
            if policy == "random":
                a = rng.integers(5)
            elif policy == "thompson":                                # sample a plausible click rate per arm, play the best sample
                a = int(np.argmax(rng.beta(1 + wins, 1 + counts - wins)))
            elif t < 5:
                a = t                                                 # try each arm once first
            elif policy == "greedy":
                a = int(np.argmax(wins / counts))
            elif policy == "eps":
                a = rng.integers(5) if rng.random() < 0.1 else int(np.argmax(wins / counts))
            else:                                                     # UCB1
                a = int(np.argmax(wins / counts + np.sqrt(2 * np.log(t) / counts)))
            counts[a] += 1
            wins[a] += rng.random() < true_p[a]
            regret += true_p.max() - true_p[a]
        return regret, counts

    runs = {p: [run(p, s)[0] for s in range(10)] for p in ("random", "greedy", "eps", "ucb", "thompson")}
    res = {p: float(np.mean(v)) for p, v in runs.items()}
    show("mean regret over 20,000 rounds, 10 seeds (clicks lost vs always showing the best ad): " + ", ".join(f"{k} {v:.0f}" for k, v in res.items()))
    show(f"greedy regret per seed: {[round(v) for v in runs['greedy']]}")
    check("pure greedy is all-or-nothing: it locks onto a wrong arm on most seeds", True, sum(v > 500 for v in runs["greedy"]) >= 5)
    check("UCB1 over-explores when the click-rate gaps are tiny (worse than eps-greedy here)", True, res["ucb"] > res["eps"])
    check("Thompson sampling has the lowest regret", "thompson", min(res, key=res.get))
    _, counts = run("thompson", 0)
    check("Thompson ends up playing the best arm (ad 3) most", 3, int(np.argmax(counts)))


# ---------------------------------------------------------------------------
# 6. Q-learning vs value iteration
# ---------------------------------------------------------------------------
GRID = ["S..#....",
        ".#.#.##.",
        ".#...#..",
        ".####.#.",
        "......#G"]
MOVES = [(-1, 0), (1, 0), (0, -1), (0, 1)]


def step(state, a):
    r, c = state
    dr, dc = MOVES[a]
    nr, nc = r + dr, c + dc
    if not (0 <= nr < len(GRID) and 0 <= nc < len(GRID[0])) or GRID[nr][nc] == "#":
        nr, nc = r, c                                                # bump into a wall: stay put
    done = GRID[nr][nc] == "G"
    return (nr, nc), (10.0 if done else -1.0), done


def greedy_path_length(Q):
    s, steps = (0, 0), 0
    while GRID[s[0]][s[1]] != "G" and steps < 100:
        s, _, _ = step(s, int(np.argmax(Q[s])))
        steps += 1
    return steps


def q_learning():
    section("6. Q-learning on a grid world")
    H, W = len(GRID), len(GRID[0])
    gamma = 0.95
    V = np.zeros((H, W))                                             # value iteration (needs the model)
    for _ in range(200):
        for r in range(H):
            for c in range(W):
                if GRID[r][c] in "#G":
                    continue
                V[r, c] = max(rew + gamma * (0 if done else V[ns]) for ns, rew, done in (step((r, c), a) for a in range(4)))
    Q_opt = np.zeros((H, W, 4))
    for r in range(H):
        for c in range(W):
            for a in range(4):
                ns, rew, done = step((r, c), a)
                Q_opt[r, c, a] = rew + gamma * (0 if done else V[ns])
    opt_len = greedy_path_length(Q_opt)

    rng = np.random.default_rng(0)
    Q = np.zeros((H, W, 4))
    alpha, eps = 0.5, 0.2
    for episode in range(800):
        s = (0, 0)
        for _ in range(200):
            a = rng.integers(4) if rng.random() < eps else int(np.argmax(Q[s]))
            ns, rew, done = step(s, a)
            target = rew + (0 if done else gamma * Q[ns].max())      # Bellman target
            Q[s][a] += alpha * (target - Q[s][a])                    # move towards it
            s = ns
            if done:
                break
    learned_len = greedy_path_length(Q)
    show(f"shortest path found: value iteration {opt_len} steps, Q-learning {learned_len} steps")
    check("Q-learning (no model, just trial and error) finds the optimal path", opt_len, learned_len)
    near("Q-learning's value of the start state ~ the optimal value", V[0, 0], Q[0, 0].max(), 0.5)


# ---------------------------------------------------------------------------
# 7. policy gradients
# ---------------------------------------------------------------------------
def policy_gradient():
    section("7. Policy gradients: REINFORCE and PPO's clip")
    rng = np.random.default_rng(1)
    true_reward = np.array([1.0, 2.0, 5.0, 0.5])                    # the policy should learn to pick action 2
    theta = np.zeros(4)
    baseline = 0.0
    for _ in range(2000):
        p = np.exp(theta - theta.max())
        p /= p.sum()
        a = rng.choice(4, p=p)
        R = true_reward[a] + rng.normal(0, 1)
        baseline = 0.99 * baseline + 0.01 * R                       # baseline reduces variance
        grad_logp = -p
        grad_logp[a] += 1                                            # d log softmax / d theta
        theta += 0.05 * (R - baseline) * grad_logp                  # REINFORCE: push up actions that beat the baseline
    p = np.exp(theta - theta.max())
    p /= p.sum()
    show(f"REINFORCE policy after 2000 episodes: {np.round(p, 3).tolist()}")
    check("the policy concentrates on the best action", 2, int(np.argmax(p)))
    check("...with high probability", True, p[2] > 0.9)

    def ppo_clip(ratio, adv, eps=0.2):
        return np.minimum(ratio * adv, np.clip(ratio, 1 - eps, 1 + eps) * adv)
    check("PPO: positive advantage, ratio 1.5 -> objective capped at 1.2*A", 1.2, ppo_clip(1.5, 1.0))
    check("PPO: positive advantage, ratio 0.5 -> not clipped (min keeps 0.5*A)", 0.5, ppo_clip(0.5, 1.0))
    check("PPO: negative advantage, ratio 0.5 -> capped at 0.8*A (no reward for over-shrinking)", -0.8, ppo_clip(0.5, -1.0))
    check("PPO: negative advantage, ratio 1.5 -> full penalty 1.5*A (pessimistic)", -1.5, ppo_clip(1.5, -1.0))


if __name__ == "__main__":
    time_series()
    recommenders()
    anomalies()
    bandits()
    q_learning()
    policy_gradient()
    summary()
