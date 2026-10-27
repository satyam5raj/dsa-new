# 13 · Time series, recommenders, anomaly detection and reinforcement learning

> Four problem families that show up constantly in industry and in system-design interviews.
> Each has its own traps: time series break random splits, recommenders need ranking metrics,
> anomaly detectors drown in false alarms, and RL has to explore.

Run it: `../.venv/bin/python specialised.py` → **29/29 checks** (~5 s).

---

## 1. Time series forecasting

**Data:** two years of synthetic daily sales = trend (+0.1/day) + **weekly** pattern (weekends
high) + yearly wave + noise (std 6).

**Look before you model:**

- Lag-1 autocorrelation is **0.833**: today ≈ yesterday.
- After **differencing** (`y[t] − y[t−1]`, which removes the trend; the mean difference ≈ the
  0.1 slope), the lag-7 autocorrelation is **0.620**. That's the weekly seasonality.
- Many classical models want a **stationary** series (constant mean and variance). Differencing
  and seasonal differencing get you there. (The ADF test in `statsmodels` checks this formally.)

| Model | Idea |
|---|---|
| **naive** | tomorrow = today |
| **seasonal naive** | tomorrow = same weekday last week |
| moving average | mean of the last k days |
| **simple exponential smoothing** | `s = α·y + (1−α)·s`: recent days weigh more |
| **Holt–Winters** (from scratch) | separate smoothed **level**, **trend** and **seasonal** components |
| **AR(p)** | linear regression on the last p values (ARIMA = AR + differencing + MA terms on past errors) |
| **ML on lag features** | gradient boosting on `y[t−1], y[t−7], y[t−14]`, a rolling mean, day-of-week |
| deep models | LSTM, temporal CNN, transformers (PatchTST), N-BEATS, foundation models: worth it at scale or with many related series |

## 2. Backtesting: walk forward, never shuffle

For each of the last 120 days, forecast tomorrow using **only data up to today**. Models are refit
monthly, as they would be in production. (Chapter 07's leakage demo showed a shuffled split giving
R² 0.99 on a random walk that can't be forecast at all.)

| Model | MAE | MAPE % | **MASE** |
|---|---|---|---|
| naive | 10.48 | 3.99 | 1.55 |
| seasonal naive | 6.92 | 2.66 | 1.02 |
| moving avg 7 | 10.22 | 3.88 | 1.51 |
| exp smoothing | 10.91 | 4.15 | 1.61 |
| **Holt–Winters** | **5.52** | **2.11** | **0.82** |
| AR(14) | 6.07 | 2.31 | 0.90 |
| boosting on lags | 6.93 | 2.63 | 1.02 |

What to read from the table:

1. **Knowing the season is everything here.** The models that see the weekly pattern
   (Holt–Winters, AR(14), seasonal naive) beat naive, the moving average and plain smoothing.
2. A 7-day moving average **blurs the weekend peak**, which is worse than just copying last week.
3. **Holt–Winters wins**, with MAE 5.52 close to the noise floor (std 6 → best possible MAE ≈
   6·√(2/π) ≈ 4.8). Simple, well-specified classical models are hard to beat on a single clean
   series.
4. Boosting on lags only ties seasonal naive: trees **can't extrapolate the trend** (chapter 03).
   The fix is to model the differences (or detrend) and add calendar features. ML shines with many
   related series, rich covariates (prices, promotions, weather) and non-linear effects.

**Metrics:**

- **MAE / RMSE:** in the units of the series.
- **MAPE:** a percentage, but it explodes near zero and is asymmetric.
- **MASE:** MAE divided by the seasonal-naive MAE on the training data. **< 1 beats the naive
  baseline** (seasonal naive itself scores ≈ 1.02, as it should).
- For probabilistic forecasts: **quantile (pinball) loss**. Inventory planning needs P90, not just
  the mean.

> I first computed the MASE scale with `np.diff(y, 7)`, which is a *7th-order* difference, not
> "the value 7 days ago". MASE came out at 0.02–0.04, which is absurd for a model that's barely
> better than naive. Sanity-check every metric against a baseline you understand.

## 3. Recommender systems

**Data:** 300 users × 200 items, ratings generated from hidden 4-D tastes plus item quality; only
10 % of ratings are observed (real data is sparser still).

### Matrix factorisation with ALS (from scratch)

Approximate the rating matrix as `R ≈ μ + U Vᵀ`, where each user and each item gets a k-dim
embedding.

**Alternating least squares:**

- fix V and solve every user exactly (a small ridge regression);
- fix U and solve every item;
- repeat.

Each step is a closed form, it parallelises easily, and it handles implicit feedback well
(Spark's ALS).

| Predicting held-out ratings | RMSE |
|---|---|
| global mean | 1.025 |
| item mean (popularity) | 0.939 |
| **ALS matrix factorisation** | **0.660** |

### Rank, don't just predict

Users see a **ranked list**, so evaluate ranking. For each user, order their held-out items and
score with **NDCG@5**, using gain `2^rating − 1`:

| Ranker | NDCG@5 |
|---|---|
| random | 0.720 |
| popularity | 0.836 |
| **MF** | **0.929** |

**NDCG** = DCG / ideal DCG, with `DCG = Σ gainᵢ / log₂(i + 1)`, so mistakes at the top cost the
most. Example: hits at ranks 1 and 3 out of 2 relevant items give `(1 + ½)/(1 + 1/log₂3) = 0.920`.

Other ranking metrics:

- precision@k and recall@k;
- MAP;
- MRR (the rank of the first hit);
- hit rate.

Beyond accuracy, track **coverage, diversity, novelty** and business metrics in A/B tests.

> While building this I first scored MF against *every unrated item*, and popularity won. With
> 10 % observed ratings, "items in the test split" is mostly random, so that evaluation rewarded
> popular items for appearing more often. Evaluation design can flip a conclusion.

### Production architecture (the system-design answer)

```
millions of items ──► RETRIEVAL (cheap, high recall)  ──► ~1,000 candidates
                      two-tower model: user tower → u, item tower → v, score = u · v
                      served by approximate nearest-neighbour search (FAISS, ScaNN, HNSW)
                    ──► RANKING (expensive, precise)  ──► ~100
                      gradient boosting or a deep model on rich user × item × context features
                    ──► RE-RANKING / business rules ──► final 10–20
                      diversity, freshness, dedupe, ads, fairness
```

- **Cold start:** a brand-new user has no ratings. ALS gives them a **zero embedding** (checked),
  which is useless. Fall back to popularity, onboarding questions, content features (item text and
  images through the item tower), or contextual bandits (§5).
- **Implicit feedback** (clicks, watch time) is far more plentiful than ratings, but "no click" ≠
  "dislike". Use weighted ALS or sampled negatives.
- **Feedback loops:** you only observe what you showed. Add exploration and log propensities.

## 4. Anomaly detection in practice

### Robust statistics

1,000 normal values (mean 100, std 10) plus 20 huge anomalies (300–400).

- The anomalies **inflate the standard deviation** to 36.0 (true 10), which hides more anomalies,
  especially as contamination grows.
- The **robust z-score** uses the median and the MAD (median absolute deviation):
  `0.6745·(x − median)/MAD`. It caught **100 %** of the anomalies with < 1 % false alarms.

Here the anomalies were extreme enough for the plain z-score to catch too. The robust version is
the one that keeps working when there are many anomalies or they are moderate.

### Context matters: seasonal residuals

Add +35 to one day of the seasonal sales series:

| Detector | Score on that day |
|---|---|
| global z-score | **2.49**: missed (normal weekends are higher than that spike) |
| residual vs the **median of the same weekday over the last 4 weeks** (robust z) | **5.06**: flagged, and it was the **only** day flagged |

> An anomaly is "unusual **given the context**" (time of day, weekday, season, user). Remove the
> expected pattern first, then threshold the residual.

### Local density: LOF

The **Local Outlier Factor** compares a point's density with its neighbours' densities. A point
just outside a **tight** cluster scores **5.17** (normal ≈ 1), even though it would look normal
next to a wide cluster. See chapter 05 for Isolation Forest, One-Class SVM and autoencoders.

**In production:**

- The **alert budget** matters more than AUC: on-call people ignore detectors with many false
  alarms.
- Tune thresholds to a tolerable alerts-per-day.
- Group related alerts.
- Feed confirmed labels back to train a **supervised** model.

## 5. Bandits: explore vs exploit

5 ad variants with click rates `[5 %, 4 %, 6 %, **10 %**, 4.5 %]`, 20,000 impressions, 10 seeds.
**Regret** = clicks lost compared with always showing the best ad.

| Policy | Mean regret | Notes |
|---|---|---|
| random | 820 | explores forever |
| **greedy** | 700 | **all or nothing**: per seed `[3, 1000, 0, 1000, 0, 1000, 1000, …]`. It locks onto whichever ad looked best early, on 7 of 10 seeds a wrong one |
| ε-greedy (ε = 0.1) | 105 | explores 10 % of the time forever |
| UCB1 | 451 | the bonus `√(2 ln t / n)` is large next to gaps of 0.01–0.05, so it **over-explores** |
| **Thompson sampling** | **49** | sample a plausible click rate from each arm's Beta posterior, play the best sample |

Thompson sampling explores in proportion to uncertainty: it stops wasting impressions on arms that
are clearly worse. It's the practical default for A/B/n testing with adaptive traffic, and for
**contextual bandits** (personalised recommendations, cold start).

## 6. Q-learning (value-based RL)

An agent moves on a grid with walls (`#`): reward −1 per step and +10 at the goal, discount γ = 0.95.

- **Value iteration** (needs the environment model) finds the shortest path: **15 steps**.
- **Q-learning** (no model, just trial and error with ε-greedy exploration):

```
Q(s, a) ← Q(s, a) + α · [ r + γ · max_a' Q(s', a') − Q(s, a) ]        ← the Bellman update
```

After 800 episodes its greedy policy also takes **15 steps**, and its value of the start state
matches the optimal value (−5.37).

**DQN** replaces the Q-table with a neural network for huge state spaces (Atari pixels). It
stabilises training with:

- a **replay buffer** (break the correlation between consecutive samples);
- a **target network** (a slowly updated copy for the `max Q(s′)` target);
- improvements: Double DQN (reduces overestimation), dueling heads, prioritised replay.

## 7. Policy gradients and PPO

Instead of learning values, directly adjust the policy's probabilities.

**REINFORCE:** `θ ← θ + η · (R − baseline) · ∇ log π(a)`. Push up actions that did better than
average. The **baseline** (here a running mean reward) doesn't change the expected gradient but
cuts variance a lot.

On a 4-action problem with mean rewards `[1, 2, 5, 0.5]` plus noise, the policy converged to
`[0.001, 0.001, 0.998, 0.0]`.

**PPO** (used for RLHF, chapter 12) prevents destructively large policy updates by clipping the
probability ratio `ρ = π_new(a)/π_old(a)` to [1 − ε, 1 + ε]:

```
L = min( ρ·A, clip(ρ, 1−ε, 1+ε)·A )         ε = 0.2
```

| Advantage A | Ratio ρ | Objective | Why |
|---|---|---|---|
| +1 | 1.5 | **1.2** | capped: no extra reward for moving too far towards a good action |
| +1 | 0.5 | 0.5 | not clipped: the `min` keeps the worse value |
| −1 | 0.5 | **−0.8** | capped: no extra credit for over-shrinking a bad action |
| −1 | 1.5 | −1.5 | full penalty: pessimistic when a bad action became more likely |

The `min` makes the objective a **pessimistic bound**: the update can only look good if it is
good **and** small. Actor–critic methods (A2C, PPO) learn a value function as the baseline and use
GAE for advantages.

---

## Exercises

1. Why is MAPE a bad metric for a product that sometimes sells 0–2 units a day?
2. Your recommender's offline NDCG improved, but the A/B test shows fewer purchases. Give two
   reasons.
3. A transaction-amount alert fires every Black Friday. How do you fix the detector?
4. Q-learning vs SARSA: what's the difference?

<details><summary>Answers</summary>

1. Dividing by tiny actuals explodes the percentage (a 1-unit miss on 1 sale is 100 %), and zeros
   are undefined. Use MAE, MASE, or a weighted MAPE (`Σ|error| / Σ actual`).
2. Offline evaluation only scores items the old system showed (selection bias), it ignores
   position and presentation effects, the metric may reward clicks over purchases, and popular
   items can win offline while reducing discovery. Align the offline metric with the business goal
   and use counterfactual evaluation (inverse propensity scoring).
3. Make the baseline seasonal and calendar-aware (holiday features, the same day last year), or
   model the expected value with a forecaster and threshold the residual relative to the forecast
   uncertainty.
4. Q-learning is **off-policy**: its target uses `max Q(s′, a′)` (the best action) regardless of
   what the agent actually does next. SARSA is **on-policy**: its target uses the action actually
   taken (`Q(s′, a′)` with a′ from the ε-greedy policy), so it learns safer paths near hazards
   when exploring.
</details>

## Interview questions

**Q: How do you validate a forecasting model?** Walk-forward (rolling-origin) backtesting with the
same refit cadence and forecast horizon as production; features computed only from the past; report
MAE/MASE and quantile loss against naive and seasonal-naive baselines; break the errors down by
horizon and segment.

**Q: Design a recommendation system.** Two-stage: retrieval (two-tower embeddings + ANN, plus
heuristics like co-visitation) for recall, then a ranking model on rich features for precision,
then re-ranking for diversity and business rules. Handle cold start (content features,
popularity, bandits), implicit feedback, feedback loops (exploration, logging propensities).
Evaluate offline with recall@k/NDCG, online with A/B tests on business metrics.

**Q: Explore vs exploit?** Exploiting the current best wastes nothing short-term but may never
discover something better. Exploring costs now but informs. ε-greedy explores at a fixed rate; UCB
explores optimistically where uncertain; Thompson sampling explores in proportion to the posterior
probability of being best, and is usually the most efficient in practice.

**Q: Why does PPO clip?** Policy-gradient steps computed on old samples are only reliable near the
old policy. Clipping the probability ratio removes the incentive to move far in one update, which
gives stable, monotonic-ish improvement without TRPO's expensive constraint.

---

**Next:** [14 · MLOps and production](../14-MLOps-Production/README.md)
