# 17 · Probabilistic models

> Some problems need more than a single prediction. They need **hidden structure** (what state is
> the system in?), **tracking over time**, or an honest statement of **uncertainty**. This chapter
> builds the classic probabilistic tools by hand: HMMs, Baum–Welch, the Kalman filter, Bayesian
> regression, Gaussian processes, MCMC and Bayesian optimisation.

Run it: `../.venv/bin/python probabilistic.py` → **27/27 checks** (~35 s; Baum–Welch on 20,000
steps is the slow part).

| Model | Question it answers | Real uses |
|---|---|---|
| HMM (forward, Viterbi, forward-backward) | which hidden states produced these observations? | speech (pre-deep-learning), part-of-speech tagging, gene finding, user-intent sessions |
| Baum–Welch | learn the HMM without ever seeing the hidden states | unsupervised sequence modelling |
| Kalman filter | where is it **now**, given noisy measurements and a motion model? | GPS/IMU fusion, robotics, finance, time series |
| Bayesian linear regression | weights **with error bars**; how sure is a prediction? | small data, decisions under uncertainty |
| Gaussian process | a flexible function with uncertainty everywhere | surrogate models, spatial data, small-data regression |
| MCMC (Metropolis–Hastings, Gibbs) | sample from a posterior you can't compute in closed form | Bayesian statistics (Stan, PyMC) |
| Bayesian optimisation | find the best input of an **expensive** black box in few tries | hyperparameter tuning, A/B parameter search, experiment design |

---

## 1. Hidden Markov models

**Setup:** the weather is hidden (Sunny / Rainy). Every day you only see what a friend did (walk,
shop, clean).

| | |
|---|---|
| start probabilities π | Sunny 0.6, Rainy 0.4 |
| transitions A | Sunny → Sunny 0.7, Sunny → Rainy 0.3, Rainy → Sunny 0.4, Rainy → Rainy 0.6 |
| emissions B | Sunny: walk 0.6, shop 0.3, clean 0.1. Rainy: walk 0.1, shop 0.4, clean 0.5 |

**Two assumptions:** tomorrow's state depends only on today's (Markov), and each observation
depends only on that day's state.

The observations are `walk, shop, clean, clean, walk`. There are `2⁵ = 32` possible hidden weather
sequences; the script enumerates all of them as ground truth.

### Forward algorithm: P(observations)

Summing over all Kᵀ paths is exponential. **Dynamic programming** reuses partial sums:

```
α₁(s) = π(s) B(s, o₁)
αₜ(s) = [Σ_s' αₜ₋₁(s') A(s', s)] · B(s, oₜ)          ← O(T·K²) instead of O(Kᵀ)
P(o₁…o_T) = Σ_s α_T(s)
```

It gives exactly the brute-force sum, **0.0036**.

### Viterbi: the single most likely hidden path

The same recursion with **max** instead of sum, plus back-pointers (in log space to avoid
underflow). The answer is `Sunny, Rainy, Rainy, Rainy, Sunny`, identical to the brute-force best
of 32 paths, with the same log-probability (−6.977). Viterbi is the same idea as shortest-path DP.

### Forward–backward: P(state at day t | all observations)

`βₜ(s)` = the probability of the *future* observations given state s at t, computed backwards.
Then `P(sₜ | o) ∝ αₜ(s) βₜ(s)`.

| Day | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|
| P(Rainy) | 0.123 | 0.519 | **0.836** | 0.793 | 0.172 |

Day 3's value matches brute force exactly.

> **Viterbi ≠ forward–backward:** Viterbi gives the best *whole sequence*; forward–backward gives
> the most likely state *each day*. The two can disagree.

## 2. Baum–Welch: learning an HMM (EM again)

Only the observations are given. Alternate between:

- **E-step:** forward–backward gives `γₜ(s)` (P(state s at t)) and `ξₜ(s, s')` (P(s at t and s'
  at t+1)).
- **M-step:** re-estimate the parameters as expected counts:

```
A(s, s') = Σₜ ξₜ(s, s') / Σₜ γₜ(s)        B(s, m) = Σ_{t: oₜ = m} γₜ(s) / Σₜ γₜ(s)
```

On long sequences, α and β underflow to 0, so the forward pass is **rescaled** at every step
(`cₜ`). The log-likelihood is then `Σ log cₜ`.

**Results on 20,000 observations:**

| | Final log-likelihood |
|---|---|
| restart 1 | **−21,622.9** |
| restart 2 | −21,740.1: stuck where both hidden states look the same |
| restart 3 | −21,740.1 |
| the true parameters | −21,626.0 |

- The log-likelihood never decreased, in every run (the EM guarantee).
- **Restarts matter**: two of three got stuck in a poor local optimum. Keep the best.
- The best restart learned `A = [[0.69, 0.31], [0.31, 0.69]]` (true `[[0.7, 0.3], [0.4, 0.6]]`)
  and `B = [[0.63, 0.29, 0.08], [0.16, 0.39, 0.45]]`, all within 0.1.
- The hidden states come out **unnamed**. You have to match "state 0" to "Sunny" yourself (here by
  the walk probability). This is *label switching*.

> **A surprise while building this:** with only 5,000 observations the learned model's
> log-likelihood (−5,418.6) is **higher than the true parameters'** (−5,424.0), and the learned
> matrices were off by up to 0.2. That isn't a bug. The maximum-likelihood estimate fits *this
> sample*, noise included, so on small data it isn't the truth. More data (20,000) pulled it close.
> The same holds for every model you fit.

## 3. Kalman filter

**Problem:** an object moves at roughly constant velocity; GPS reports its position with noise
(std 5). Velocity is never measured.

The state is `x = [position, velocity]`. Repeat two steps:

```
PREDICT  x ← F x               P ← F P Fᵀ + Q          move the belief with the physics; uncertainty grows
UPDATE   K = P Hᵀ (H P Hᵀ + R)⁻¹                       Kalman gain: trust in the measurement vs the model
         x ← x + K (z − H x)    P ← (I − K H) P        correct by the "surprise" (the innovation)
```

| | Position RMSE (after warm-up) |
|---|---|
| raw GPS | 5.82 |
| **Kalman filter** | **1.98** |

- **Velocity estimate 0.24** (true 0.34), though velocity is never observed: it's inferred from how
  positions change.
- **Kalman gain: 0.89 at the first step** (no idea yet, so trust the data), **0.18 at step 100**
  (confident, so trust the model and smooth out the noise).

It is the **optimal** estimator for linear dynamics with Gaussian noise. For non-linear systems use
the extended or unscented Kalman filter, or particle filters. It is exactly an HMM with continuous
Gaussian states, where the forward algorithm has a closed form.

## 4. Bayesian linear regression

Instead of one best weight vector, keep a **distribution** over weights:

```
prior      w ~ N(0, τ² I)                      noise  y = Xw + ε,  ε ~ N(0, σ²)
posterior  precision  S⁻¹ = I/τ² + XᵀX/σ²      mean  m = S Xᵀ y / σ²
predictive variance at x*:  σ² + x*ᵀ S x*      ← noise + uncertainty about w
```

- On 20 points from `y = 0.5 + 2x`: posterior mean `[0.521, 2.04]`, posterior std
  `[0.112, 0.219]`.
- **The posterior mean is exactly ridge regression** with `α = σ²/τ²` (checked). Regularisation
  *is* a prior: L2 = Gaussian prior, L1 = Laplace prior.
- **Uncertainty knows where the data is:** predictive std **0.512** at x = 0 (inside the data)
  vs **0.846** at x = 3 (far outside). Ordinary regression would give the same ± everywhere.
- With 2,000 points the posterior std shrinks more than 5 times.

## 5. Gaussian process regression

A GP is a **prior over functions**: any set of function values is jointly Gaussian, with
covariance given by a **kernel**. The RBF kernel says "nearby inputs have similar outputs":

```
k(x, x') = σ² exp(−(x − x')² / 2ℓ²)                         ℓ = length-scale
posterior mean   μ* = K*ᵀ (K + σₙ² I)⁻¹ y                   ← weighted combination of training targets
posterior cov    Σ* = K** − K*ᵀ (K + σₙ² I)⁻¹ K*            ← prior uncertainty minus what the data explains
```

Solved with a Cholesky factorisation (never an explicit inverse).

- The scratch posterior mean and std **match sklearn's** `GaussianProcessRegressor` exactly (with
  fixed hyperparameters).
- From **12 noisy points** of `sin(x)`: RMSE 0.159 between the points.
- **Std 0.068 at x = 5** (data nearby) vs **1.000 at x = −2**: far from data the GP honestly
  reverts to its prior.
- The **length-scale** controls smoothness: ℓ = 0.2 gives a far wigglier function than ℓ = 1.
  In practice ℓ and the noise level are learned by maximising the marginal likelihood (sklearn's
  default optimiser).
- **Cost** is `O(n³)`, so plain GPs suit up to ~10k points; sparse/inducing-point GPs scale further.

## 6. MCMC: sampling a posterior you can't write down

**Metropolis–Hastings:**

```
propose  p' = p + N(0, 0.1²)
accept with probability min(1, posterior(p') / posterior(p))     ← the normalising constant cancels!
otherwise stay at p
```

It only needs the posterior **up to a constant**, which is exactly what Bayes' rule gives
(likelihood × prior).

The test case: 7 heads in 20 flips with a Beta(2, 2) prior. The exact posterior is Beta(9, 15), so
we can verify.

| | MH samples | Exact Beta(9, 15) |
|---|---|---|
| mean | 0.3747 | 0.3750 |
| std | 0.0959 | ≈ 0.0968 |
| 97.5 % quantile | 0.5679 | ≈ 0.57 |

**Diagnostics:**

- **Burn-in:** discard the first 5,000 samples, taken before the chain reached the posterior.
- **Acceptance rate 70 %**. Too high means steps that are too small (slow exploration); too low
  means steps that are too big (stuck). Aim for ~25–50 % in many dimensions.
- **Autocorrelation 0.76** at lag 1. Consecutive samples are correlated, so 55,000 samples carry
  far less information than 55,000 independent draws (the *effective sample size*). Run several
  chains and check they agree (R-hat).

**Gibbs sampling:** sample each variable from its conditional given the others. For a bivariate
normal with correlation 0.8, `x | y ~ N(0.8y, 0.36)` and vice versa. The recovered correlation is
**0.799**. Modern samplers (**HMC/NUTS** in Stan and PyMC) use gradients to make big, efficient
moves.

## 7. Bayesian optimisation

**Problem:** maximise a function where each evaluation is **expensive**, like training a model with
some hyperparameters (hours). Random or grid search wastes evaluations.

**Loop:**

1. Fit a **GP surrogate** to the evaluations so far (§5): a mean prediction **and** uncertainty.
2. Choose the next point by maximising **expected improvement**:

```
EI(x) = (μ − best) Φ(z) + σ φ(z),     z = (μ − best) / σ
        ─────────────────   ─────
        exploit: likely better   explore: uncertain, could be much better
```

3. Evaluate it, add it to the data, and repeat.

On a wiggly 1-D function with several local maxima (true max 1.4366), with **12 evaluations each**
(the mean of 10 runs):

| | Best value found |
|---|---|
| random search | 1.0004 |
| **Bayesian optimisation** | **1.4349**: within 0.002 of the optimum |

That's the engine behind Optuna's GP and TPE samplers, Vertex AI Vizier and SigOpt. Other
acquisition functions: UCB (`μ + κσ`, chapter 13's bandits again), probability of improvement,
Thompson sampling.

---

## Exercises

1. Why does the forward algorithm need scaling (or log space) for a sequence of 10,000 steps?
2. In the Kalman filter, what happens to the gain if you set R (measurement noise) very large?
3. Ridge regression with α = 10. What prior does that correspond to, if σ = 1?
4. Why can't you just use the GP posterior mean to choose the next hyperparameter in Bayesian
   optimisation?

<details><summary>Answers</summary>

1. α multiplies probabilities < 1 at every step: 0.3¹⁰⁰⁰⁰ underflows to 0.0 in float64. Rescale α
   each step (and keep `Σ log cₜ`), or work with log-sum-exp.
2. The gain → 0: the filter ignores the measurements and just follows the motion model. With
   R → 0 the gain → 1 and it copies the measurements.
3. `α = σ²/τ²`, so `τ² = 0.1`: each weight ~ N(0, 0.1), i.e. a prior std of ≈ 0.32.
4. Pure exploitation: it would keep sampling near the current best and never explore uncertain
   regions that might contain the global maximum. EI and UCB add the uncertainty term σ.
</details>

## Interview questions

**Q: Viterbi vs forward algorithm?** Same DP over a trellis. Forward **sums** over previous states
to give the total probability of the observations; Viterbi takes the **max** and keeps
back-pointers to recover the best path. Both are `O(T·K²)`.

**Q: What are the Kalman filter's assumptions?** Linear dynamics and observation models, Gaussian
process and measurement noise, known covariances Q and R. Under these it's the optimal (minimum
mean-squared-error) estimator. Otherwise use EKF/UKF or particle filters.

**Q: Frequentist vs Bayesian regression?** Frequentist gives a point estimate (plus confidence
intervals from the sampling distribution). Bayesian gives a posterior over parameters and a
predictive distribution; regularisation appears as a prior, and uncertainty is explicit and grows
away from the data.

**Q: When would you use a Gaussian process?** Small to medium data, smooth functions, and when
calibrated uncertainty matters: surrogate models in Bayesian optimisation, active learning,
spatial interpolation (kriging). Not for large n without approximations.

**Q: Why does Metropolis–Hastings not need the normalising constant?** Acceptance depends on the
**ratio** of posteriors at two points, and the evidence `P(data)` cancels. That's what makes MCMC
usable for any model where you can evaluate likelihood × prior.

---

**Next:** [18 · Representation learning and LLM inference](../18-Representation-and-LLM-Inference/README.md)
