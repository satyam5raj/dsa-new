"""
ML 17 : PROBABILISTIC MODELS
Run:  ../.venv/bin/python probabilistic.py        (explanations: README.md)

  1. Hidden Markov model        forward algorithm, Viterbi, forward-backward (checked by brute force)
  2. Baum-Welch                  learning an HMM's parameters from observations alone (EM)
  3. Kalman filter               tracking a moving object from noisy measurements
  4. Bayesian linear regression  posterior over weights, predictive uncertainty, == ridge
  5. Gaussian process regression kernels, posterior mean and uncertainty, == sklearn
  6. MCMC                        Metropolis-Hastings and Gibbs sampling, checked against exact answers
  7. Bayesian optimisation       GP surrogate + expected improvement to tune a black box
"""
import sys
from itertools import product
from pathlib import Path

import numpy as np
from scipy import stats
from sklearn.gaussian_process import GaussianProcessRegressor
from sklearn.gaussian_process.kernels import RBF, WhiteKernel
from sklearn.linear_model import Ridge

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from mlcheck import check, near, section, show, summary  # noqa: E402

# weather HMM: hidden state = Sunny/Rainy, observation = what a friend does: walk / shop / clean
STATES, OBS = ["Sunny", "Rainy"], ["walk", "shop", "clean"]
PI = np.array([0.6, 0.4])
A = np.array([[0.7, 0.3],          # P(next state | current state)
              [0.4, 0.6]])
B = np.array([[0.6, 0.3, 0.1],     # P(observation | state)
              [0.1, 0.4, 0.5]])


# ---------------------------------------------------------------------------
# 1. HMM inference
# ---------------------------------------------------------------------------
def forward(obs, pi, A, B):
    alpha = np.zeros((len(obs), len(pi)))
    alpha[0] = pi * B[:, obs[0]]
    for t in range(1, len(obs)):
        alpha[t] = (alpha[t - 1] @ A) * B[:, obs[t]]          # sum over the previous state: O(T K^2)
    return alpha


def backward(obs, A, B):
    beta = np.ones((len(obs), A.shape[0]))
    for t in range(len(obs) - 2, -1, -1):
        beta[t] = A @ (B[:, obs[t + 1]] * beta[t + 1])
    return beta


def viterbi(obs, pi, A, B):
    T, K = len(obs), len(pi)
    logd = np.log(pi) + np.log(B[:, obs[0]])
    back = np.zeros((T, K), int)
    for t in range(1, T):
        scores = logd[:, None] + np.log(A)                   # best path into each state: MAX instead of sum
        back[t] = scores.argmax(0)
        logd = scores.max(0) + np.log(B[:, obs[t]])
    path = [int(logd.argmax())]
    for t in range(T - 1, 0, -1):
        path.append(int(back[t, path[-1]]))
    return path[::-1], float(logd.max())


def hmm_inference():
    section("1. Hidden Markov model: forward, Viterbi, forward-backward")
    obs = [OBS.index(o) for o in ["walk", "shop", "clean", "clean", "walk"]]
    joint = {}
    for path in product(range(2), repeat=len(obs)):          # brute force: all 2^5 hidden paths
        p = PI[path[0]] * B[path[0], obs[0]]
        for t in range(1, len(obs)):
            p *= A[path[t - 1], path[t]] * B[path[t], obs[t]]
        joint[path] = p
    alpha = forward(obs, PI, A, B)
    near("forward algorithm P(observations) == sum over all 32 hidden paths", sum(joint.values()), alpha[-1].sum(), 1e-15)
    path, logp = viterbi(obs, PI, A, B)
    best = max(joint, key=joint.get)
    show(f"most likely weather for walk, shop, clean, clean, walk: {[STATES[s] for s in path]}")
    check("Viterbi == the brute-force most probable path", list(best), path)
    near("...with the same probability", np.log(joint[best]), logp, 1e-12)
    beta = backward(obs, A, B)
    post = alpha * beta / alpha[-1].sum()
    check("forward-backward posteriors sum to 1 at every step", True, np.allclose(post.sum(1), 1))
    brute_day3 = sum(p for pth, p in joint.items() if pth[2] == 1) / sum(joint.values())
    near("P(Rainy on day 3 | everything) == brute force", brute_day3, post[2, 1], 1e-12)
    show(f"P(Rainy) per day: {np.round(post[:, 1], 3).tolist()}")


# ---------------------------------------------------------------------------
# 2. Baum-Welch
# ---------------------------------------------------------------------------
def sample_hmm(T, pi, A, B, rng):
    s = rng.choice(2, p=pi)
    states, obs = [], []
    for _ in range(T):
        states.append(s)
        obs.append(rng.choice(3, p=B[s]))
        s = rng.choice(2, p=A[s])
    return np.array(states), np.array(obs)


def baum_welch(obs, K, M, iters=100, seed=0):
    rng = np.random.default_rng(seed)
    pi = np.full(K, 1 / K)
    A_ = rng.dirichlet(np.ones(K) * 5, K)
    B_ = rng.dirichlet(np.ones(M) * 5, K)
    lls = []
    for _ in range(iters):
        # scaled forward-backward (scaling avoids underflow on long sequences)
        T = len(obs)
        alpha, c = np.zeros((T, K)), np.zeros(T)
        alpha[0] = pi * B_[:, obs[0]]
        c[0] = alpha[0].sum()
        alpha[0] /= c[0]
        for t in range(1, T):
            alpha[t] = (alpha[t - 1] @ A_) * B_[:, obs[t]]
            c[t] = alpha[t].sum()
            alpha[t] /= c[t]
        beta = np.ones((T, K))
        for t in range(T - 2, -1, -1):
            beta[t] = A_ @ (B_[:, obs[t + 1]] * beta[t + 1]) / c[t + 1]
        lls.append(float(np.log(c).sum()))
        gamma = alpha * beta                                   # P(state_t | all obs)
        xi = alpha[:-1, :, None] * A_[None] * (B_[:, obs[1:]].T * beta[1:])[:, None, :] / c[1:, None, None]
        pi = gamma[0]
        A_ = xi.sum(0) / gamma[:-1].sum(0)[:, None]
        B_ = np.array([gamma[obs == m].sum(0) for m in range(M)]).T / gamma.sum(0)[:, None]
    return pi, A_, B_, lls


def hmm_loglik(obs, pi, A, B):
    alpha, ll = pi * B[:, obs[0]], 0.0
    for t in range(len(obs)):
        if t:
            alpha = (alpha @ A) * B[:, obs[t]]
        ll += np.log(alpha.sum())
        alpha = alpha / alpha.sum()
    return ll


def baum_welch_demo():
    section("2. Baum-Welch: learn the HMM from observations only")
    rng = np.random.default_rng(1)
    _, obs = sample_hmm(20000, PI, A, B, rng)
    runs = [baum_welch(obs, 2, 3, iters=200, seed=s) for s in range(3)]          # EM finds LOCAL optima: restart
    finals = [r[3][-1] for r in runs]
    show(f"final log-likelihood of 3 random restarts: {[round(v, 1) for v in finals]}  (true parameters: {hmm_loglik(obs, PI, A, B):.1f})")
    check("EM: the log-likelihood never decreases", True, all(all(b >= a - 1e-6 for a, b in zip(r[3], r[3][1:])) for r in runs))
    check("restarts end in different local optima", True, max(finals) - min(finals) > 10)
    _, A_, B_, _ = runs[int(np.argmax(finals))]
    order = np.argsort(-B_[:, 0])                              # hidden states have no names: match by 'walk' probability
    A_, B_ = A_[np.ix_(order, order)], B_[order]
    show(f"best restart: A {np.round(A_, 2).tolist()} (true {A.tolist()})")
    show(f"              B {np.round(B_, 2).tolist()} (true {B.tolist()})")
    check("the best restart recovers the transition matrix (to 0.1)", True, np.allclose(A_, A, atol=0.1))
    check("...and the emission matrix (to 0.1)", True, np.allclose(B_, B, atol=0.1))
    _, small = sample_hmm(5000, PI, A, B, np.random.default_rng(1))
    pi_s, A_s, B_s, ll_s = baum_welch(small, 2, 3, iters=200, seed=0)
    show(f"with only 5,000 observations: learned log-lik {ll_s[-1]:.1f} vs true parameters {hmm_loglik(small, PI, A, B):.1f}")
    check("on small data the MLE fits the sample BETTER than the truth (it's not the truth)", True, ll_s[-1] > hmm_loglik(small, PI, A, B))


# ---------------------------------------------------------------------------
# 3. Kalman filter
# ---------------------------------------------------------------------------
def kalman_demo():
    section("3. Kalman filter: track position + velocity from noisy GPS")
    rng = np.random.default_rng(2)
    dt, T = 1.0, 100
    F = np.array([[1, dt], [0, 1]])                            # constant-velocity motion model
    H = np.array([[1.0, 0.0]])                                 # we only measure position
    Q = np.diag([0.01, 0.01])                                  # process noise (the object wobbles)
    R = np.array([[25.0]])                                     # measurement noise: GPS std 5
    x_true = np.zeros((T, 2))
    x_true[0] = [0, 1]
    for t in range(1, T):
        x_true[t] = F @ x_true[t - 1] + rng.multivariate_normal([0, 0], Q)
    z = x_true[:, 0] + rng.normal(0, 5, T)
    x, P = np.array([0.0, 0.0]), np.eye(2) * 100
    est, gains = [], []
    for t in range(T):
        x, P = F @ x, F @ P @ F.T + Q                          # predict: move the belief, add uncertainty
        S = H @ P @ H.T + R
        K = P @ H.T @ np.linalg.inv(S)                         # Kalman gain: how much to trust the measurement
        x = x + (K @ (z[t] - H @ x)).ravel()                   # update with the innovation (surprise)
        P = (np.eye(2) - K @ H) @ P
        est.append(x.copy())
        gains.append(K[0, 0])
    est = np.array(est)
    rmse_raw = np.sqrt(np.mean((z[20:] - x_true[20:, 0]) ** 2))
    rmse_kf = np.sqrt(np.mean((est[20:, 0] - x_true[20:, 0]) ** 2))
    show(f"position RMSE after warm-up: raw GPS {rmse_raw:.2f}, Kalman {rmse_kf:.2f}; velocity estimate {est[-1, 1]:.2f} (true {x_true[-1, 1]:.2f})")
    check("the filter is far more accurate than the raw measurements", True, rmse_kf < rmse_raw / 2)
    check("it estimates velocity, which is never measured", True, abs(est[-1, 1] - x_true[-1, 1]) < 0.3)
    show(f"Kalman gain: first step {gains[0]:.2f} (trust the data), step 100 {gains[-1]:.2f} (trust the model)")
    check("the gain shrinks as the belief becomes confident", True, gains[-1] < gains[0] / 3)


# ---------------------------------------------------------------------------
# 4. Bayesian linear regression
# ---------------------------------------------------------------------------
def bayes_linreg_demo():
    section("4. Bayesian linear regression")
    rng = np.random.default_rng(3)
    sigma, tau = 0.5, 1.0                                      # noise std, prior std of each weight
    x = rng.uniform(-1, 1, 20)
    X = np.column_stack([np.ones_like(x), x])
    y = 0.5 + 2.0 * x + rng.normal(0, sigma, 20)
    S_inv = np.eye(2) / tau ** 2 + X.T @ X / sigma ** 2        # posterior precision = prior + data
    S = np.linalg.inv(S_inv)
    m = S @ X.T @ y / sigma ** 2                               # posterior mean
    show(f"posterior mean weights {np.round(m, 3).tolist()} (true [0.5, 2.0]); posterior std {np.round(np.sqrt(np.diag(S)), 3).tolist()}")
    ridge = Ridge(alpha=sigma ** 2 / tau ** 2, fit_intercept=False).fit(X, y).coef_
    check("posterior mean == ridge regression with alpha = sigma^2 / tau^2", True, np.allclose(m, ridge))
    xs = np.array([0.0, 3.0])
    Xs = np.column_stack([np.ones(2), xs])
    pred_std = np.sqrt(sigma ** 2 + np.einsum("nd,de,ne->n", Xs, S, Xs))
    show(f"predictive std at x=0 (inside the data) {pred_std[0]:.3f}, at x=3 (far outside) {pred_std[1]:.3f}")
    check("uncertainty grows away from the data (the model knows what it doesn't know)", True, pred_std[1] > 1.5 * pred_std[0])
    big = rng.uniform(-1, 1, 2000)
    Xb = np.column_stack([np.ones_like(big), big])
    Sb = np.linalg.inv(np.eye(2) / tau ** 2 + Xb.T @ Xb / sigma ** 2)
    check("more data -> the posterior narrows", True, np.all(np.sqrt(np.diag(Sb)) < np.sqrt(np.diag(S)) / 5))


# ---------------------------------------------------------------------------
# 5. Gaussian process regression
# ---------------------------------------------------------------------------
def rbf_k(a, b, ls=1.0, var=1.0):
    return var * np.exp(-0.5 * (a[:, None] - b[None]) ** 2 / ls ** 2)


def gp_posterior(x_tr, y_tr, x_te, ls=1.0, var=1.0, noise=1e-2):
    K = rbf_k(x_tr, x_tr, ls, var) + noise * np.eye(len(x_tr))
    Ks, Kss = rbf_k(x_tr, x_te, ls, var), rbf_k(x_te, x_te, ls, var)
    L = np.linalg.cholesky(K)
    alpha = np.linalg.solve(L.T, np.linalg.solve(L, y_tr))
    mean = Ks.T @ alpha                                        # weighted combination of training targets
    v = np.linalg.solve(L, Ks)
    cov = Kss - v.T @ v                                        # prior uncertainty minus what the data explains
    return mean, np.sqrt(np.clip(np.diag(cov), 0, None))


def gp_demo():
    section("5. Gaussian process regression")
    rng = np.random.default_rng(4)
    x_tr = np.sort(rng.uniform(0, 10, 12))
    y_tr = np.sin(x_tr) + rng.normal(0, 0.1, 12)
    x_te = np.linspace(-2, 12, 200)
    mean, std = gp_posterior(x_tr, y_tr, x_te, noise=0.01)
    sk = GaussianProcessRegressor(RBF(1.0) + WhiteKernel(0.01), optimizer=None).fit(x_tr[:, None], y_tr)
    sk_mean, sk_std = sk.predict(x_te[:, None], return_std=True)
    check("scratch GP posterior mean == sklearn", True, np.allclose(mean, sk_mean, atol=1e-8))
    check("scratch GP std == sklearn (sklearn's std includes the white-noise term)", True, np.allclose(np.sqrt(std ** 2 + 0.01), sk_std, atol=1e-6))
    inside = (x_te > 1) & (x_te < 9)
    rmse = np.sqrt(np.mean((mean[inside] - np.sin(x_te[inside])) ** 2))
    show(f"12 noisy points of sin(x): RMSE inside the data {rmse:.3f}; std at x=5 {std[np.argmin(abs(x_te - 5))]:.3f}, at x=-2 {std[0]:.3f}")
    check("12 points are enough to learn sin(x) between them", True, rmse < 0.2)
    check("far from the data the GP reverts to the prior (std -> 1)", True, std[0] > 0.9)
    m_short, _ = gp_posterior(x_tr, y_tr, x_te, ls=0.2)
    rough = lambda f: np.mean(np.abs(np.diff(f, 2)))           # noqa: E731
    check("a short length-scale gives a wigglier function", True, rough(m_short[inside]) > 3 * rough(mean[inside]))


# ---------------------------------------------------------------------------
# 6. MCMC
# ---------------------------------------------------------------------------
def mcmc_demo():
    section("6. MCMC: Metropolis-Hastings and Gibbs")
    heads, n = 7, 20                                           # coin: 7 heads in 20 flips, prior Beta(2, 2)
    log_post = lambda p: -np.inf if not 0 < p < 1 else (heads + 1) * np.log(p) + (n - heads + 1) * np.log(1 - p)  # noqa: E731
    rng = np.random.default_rng(5)
    p, samples, accepted = 0.5, [], 0
    for _ in range(60000):
        prop = p + rng.normal(0, 0.1)                          # symmetric random-walk proposal
        if np.log(rng.random()) < log_post(prop) - log_post(p):   # accept with prob min(1, ratio): no normaliser needed
            p, accepted = prop, accepted + 1
        samples.append(p)
    s = np.array(samples[5000:])                               # drop burn-in
    exact = stats.beta(heads + 2, n - heads + 2)               # conjugate posterior Beta(9, 15)
    show(f"MH posterior mean {s.mean():.4f} vs exact Beta(9, 15) {exact.mean():.4f}; acceptance rate {accepted / 60000:.0%}")
    near("Metropolis-Hastings recovers the posterior mean", exact.mean(), s.mean(), 0.005)
    near("...and the posterior std", exact.std(), s.std(), 0.005)
    near("...and a 95% credible-interval endpoint", exact.ppf(0.975), np.quantile(s, 0.975), 0.01)
    ac = np.corrcoef(s[:-1], s[1:])[0, 1]
    show(f"lag-1 autocorrelation of the chain {ac:.2f}: samples are correlated, so the effective sample size is smaller")

    rho = 0.8                                                  # Gibbs: bivariate normal, sample each coordinate given the other
    x = y = 0.0
    G = []
    for _ in range(40000):
        x = rng.normal(rho * y, np.sqrt(1 - rho ** 2))
        y = rng.normal(rho * x, np.sqrt(1 - rho ** 2))
        G.append((x, y))
    G = np.array(G[2000:])
    near("Gibbs sampling recovers the correlation 0.8", rho, np.corrcoef(G.T)[0, 1], 0.02)


# ---------------------------------------------------------------------------
# 7. Bayesian optimisation
# ---------------------------------------------------------------------------
def bayes_opt_demo():
    section("7. Bayesian optimisation (GP + expected improvement)")
    f = lambda x: np.sin(3 * x) + 0.5 * np.cos(7 * x) - 0.1 * (x - 2) ** 2   # noqa: E731  an "expensive" black box on [0, 4]
    grid = np.linspace(0, 4, 2001)
    true_best = f(grid).max()
    rng = np.random.default_rng(6)

    def run_bo(budget=12):
        X = list(rng.uniform(0, 4, 3))
        Y = [f(x) for x in X]
        for _ in range(budget - 3):
            mu, sd = gp_posterior(np.array(X), np.array(Y) - np.mean(Y), grid, ls=0.3, var=1.0, noise=1e-6)
            mu += np.mean(Y)
            best = max(Y)
            z = (mu - best) / np.maximum(sd, 1e-9)
            ei = (mu - best) * stats.norm.cdf(z) + sd * stats.norm.pdf(z)   # expected improvement
            x_next = grid[int(np.argmax(ei))]
            X.append(x_next)
            Y.append(f(x_next))
        return max(Y)

    bo = np.mean([run_bo() for _ in range(10)])
    rand = np.mean([f(rng.uniform(0, 4, 12)).max() for _ in range(10)])
    show(f"best value found with 12 evaluations (mean of 10 runs): Bayesian optimisation {bo:.4f}, random search {rand:.4f}, true max {true_best:.4f}")
    check("BO gets closer to the true maximum than random search on the same budget", True, true_best - bo < true_best - rand)
    near("BO is within 0.01 of the optimum", true_best, bo, 0.01)


if __name__ == "__main__":
    hmm_inference()
    baum_welch_demo()
    kalman_demo()
    bayes_linreg_demo()
    gp_demo()
    mcmc_demo()
    bayes_opt_demo()
    summary()
