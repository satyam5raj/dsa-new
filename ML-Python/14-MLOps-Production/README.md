# 14 · MLOps and production

> A model in a notebook is worth nothing. A model in production is worth something only while
> it keeps working, and models **decay silently**. MLOps is the engineering that makes models
> reproducible, deployable, observable and fixable.

Run it: `../.venv/bin/python mlops.py` → **29/29 checks** (~3 s). It starts a real HTTP server on
localhost and cleans up its temporary files.

Reference files (not executed: FastAPI isn't installed and nothing is deployed):

- [app_fastapi.py](app_fastapi.py): the production-style server;
- [Dockerfile](Dockerfile);
- [requirements-serving.txt](requirements-serving.txt).

```
        ┌──────────── offline ─────────────┐        ┌────────────── online ──────────────┐
 data ─►│ validate → features → train → eval│─ gate ─►│ registry → serve (API / batch)     │
 (versioned)        (tracked experiments)   │        │   │                                │
        └───────────────▲──────────────────┘        │   ▼ logs: inputs, predictions,     │
                        │                            │       latency, (delayed) labels    │
                        └──── retrain (scheduled / ◄─┤ monitor: drift, performance, SLOs  │
                              triggered by alerts)   └────────────────────────────────────┘
```

The demo is a fraud detector on synthetic transactions (amount, hour, country, account age,
transactions in the last 24 h; 2 % fraud).

---

## 1. Reproducible artefacts

| Practice | In the script | Real tools |
|---|---|---|
| Save the **whole pipeline** (preprocessing + model), not just the model | `joblib.dump(pipe)`, reloads with identical outputs | joblib / pickle, ONNX, TorchScript, `save_pretrained` |
| **Version the data** | a SHA-256 fingerprint: same data → same hash; one changed value → a new hash | DVC, lakeFS, Delta/Iceberg time travel, dataset IDs in the warehouse |
| **Pin environments** | `requirements.txt` with exact versions | lock files, Docker images |
| **Model card** | name, version, data fingerprint, features, intended use, **limitations** | model cards in the registry |

The card records *"trained on the night-time fraud pattern only"*. In §7 that's exactly how the
model fails, so writing down limitations isn't bureaucracy.

> A pickled scikit-learn pipeline may not load in a different scikit-learn version. Pin the
> training and serving versions together (see `requirements-serving.txt`), or export to a
> portable format.

## 2. Experiment tracking

Every training run logs its **params, metrics, data version and artefact path**, append-only:

| Run | Model | Settings | Validation PR-AUC |
|---|---|---|---|
| 3a273804 | logreg | C = 1.0 | 0.9856 |
| 671c6892 | logreg | C = 0.01 | 0.9696 |
| **03272533** | **gbm** | **lr = 0.1** | **0.9939** |
| e1683261 | gbm | lr = 0.03 | 0.9863 |

Six months later, "which run produced the production model, on which data, with what settings?"
has an answer. The tracker here is ~20 lines; **MLflow** and **Weights & Biases** add a UI,
artefact storage, comparisons and lineage. The API shape is the same:

```python
import mlflow                                    # not installed here; check the current docs
with mlflow.start_run():
    mlflow.log_params({"model": "gbm", "lr": 0.1})
    mlflow.log_metric("pr_auc", 0.9939)
    mlflow.sklearn.log_model(pipe, "model", registered_model_name="fraud-detector")
```

## 3. Model registry: versions, stages, gates, rollback

| Step | Result |
|---|---|
| v1 (logreg baseline) registered | first model → **production** |
| v2 (over-regularised) | holdout PR-AUC 0.9394 vs production 0.9681 → **blocked by the gate** |
| v3 (best tracked gbm) | 0.9997 vs 0.9681 → **promoted** |
| incident! | **rollback** → v1 back in production in one step |

The **promotion gate**: the candidate must beat production **on the same holdout** by a margin.
Real gates also check:

- latency and memory budgets;
- performance per segment (no country gets worse) and fairness metrics;
- calibration;
- that the output schema is unchanged;
- a human sign-off for high-risk models.

## 4. Serving

The script runs a real **HTTP server** (Python stdlib) and calls it.

| Request | Response |
|---|---|
| `GET /health` | `200 {"status": "ok"}` |
| `POST /predict` (a night-time 900 transaction from a 3-day-old account, and a normal one) | `200 {"fraud_probability": [0.99972, 2.1e-05], "model_version": 3}` |
| `hour = 31` | **`400 "value out of range"`**: rejected before the model sees it |
| missing `country` | **400** with a clear message |

- Online predictions **equal** offline predictions for the same rows (checked). Run that test in
  CI for every release.
- Latency over 201 requests: **p50 2.42 ms, p99 12.31 ms**. Always report **tail** latency; users
  feel the p99.

| Serving mode | When | Notes |
|---|---|---|
| **online** (REST/gRPC) | per-request decisions: fraud, ranking, chat | latency SLOs, autoscaling, the model loaded once |
| **batch** | nightly scores (churn lists, risk reports) | cheap, simple; results go to a table |
| **streaming** | events on Kafka/Kinesis | near-real-time features and scores |
| **edge / on-device** | mobile, IoT | quantised or distilled models (chapter 12) |

`app_fastapi.py` is the production-shaped version:

- **pydantic** typed validation (`hour: int = Field(ge=0, le=23)`, `country: Literal[...]`);
- the model loaded once at startup;
- `/health` (liveness) and `/ready` (readiness) for Kubernetes or Cloud Run;
- a model version on every response;
- **one structured log line per request** (mean score, latency, version, never raw PII), which
  feeds monitoring.

The `Dockerfile` installs dependencies in a cached layer, runs as a **non-root** user, and declares
a `HEALTHCHECK`. LLM serving uses specialised engines: vLLM, TGI, Triton, llama.cpp.

**Release strategies:**

- **shadow** (the new model scores live traffic, nobody sees its output; compare offline);
- **canary** (1 % → 10 % → 100 % with automatic rollback);
- **A/B test** (measure business impact);
- **blue/green** (instant switch).

## 5. Training–serving skew: the silent killer

The training pipeline applies `log1p(amount)`. Imagine the serving team re-implements the features
(in Java, say) and forgets that step:

| Same model, same rows | ROC-AUC | Share flagged at 0.5 |
|---|---|---|
| correct features | **1.000** | 1.80 % |
| serving forgot `log1p` | **0.656** | **89.34 %** |

**No error is raised.** The model happily scores garbage and 89 % of customers get blocked.

**Prevention:**

1. Ship **one pipeline object** for training and serving (§1), or a **feature store** that
   computes features once for both (Feast, Tecton, Vertex/SageMaker feature stores), with
   point-in-time-correct training joins.
2. A CI test comparing online vs offline predictions on the same rows (§4 does exactly this).
3. Monitor the **prediction distribution**: a jump from 2 % to 89 % flagged would page someone in
   minutes.

## 6. Data drift: PSI and KS (from scratch)

**Population Stability Index:**

```
PSI = Σ (actual% − expected%) · ln(actual% / expected%)     over quantile bins of the reference data
```

Rule of thumb: **< 0.1 stable**, **0.1–0.25 investigate**, **> 0.25 act**.

| Live data vs reference | PSI | KS statistic | KS p-value |
|---|---|---|---|
| same distribution | 0.003 | 0.024 | **0.016** (!) |
| mean +0.4 std | **0.150** | 0.157 | 1e-86 |
| mean +1.5 std, wider | **1.286** | 0.472 | 0 |
| tiny +0.02 std shift, 500k rows | 0.0005 | | **1.2e-19** |

**The p-value trap, twice:**

- Two samples from the **same** distribution gave KS p = 0.016, which is "significant" by chance
  (chapter 02's 1-in-20).
- With 500k rows, a negligible shift is "astronomically significant".

With big data every test rejects. Alert on an **effect size** (PSI, the KS statistic, Wasserstein
distance) with thresholds tuned to what matters, not on p-values.

| Drift type | What changes | Example | Detect with |
|---|---|---|---|
| **data / covariate drift** | P(X) | new marketing brings younger users | PSI/KS on inputs |
| **prediction drift** | P(ŷ) | share flagged jumps | the same stats on model outputs |
| **concept drift** | **P(y given X)** | fraudsters change tactics | **labels** (often delayed), proxy outcomes |
| label drift | P(y) | the fraud rate doubles | label rates |

## 7. Scenario: the fraud model that quietly degrades

This is the classic interview question: *"Your fraud model's performance dropped. What do you
do?"*

**Simulation:** weeks 0–5 have the training-time fraud pattern (big night-time transactions from
new accounts). In **week 6** fraudsters switch to **card testing**: many tiny, rapid transactions
from old accounts. The old pattern continues at a lower rate.

| Week | PR-AUC | Recall in top 1 % | PSI(amount) | PSI(txns_24h) | Mean score |
|---|---|---|---|---|---|
| 0–5 | 0.996–1.000 | 0.42–0.54 | ≤ 0.002 | ≤ 0.003 | ~0.020 |
| **6** | **0.316** | 0.21 | 0.004 | 0.001 | **0.0028** |
| 7–11 | 0.275–0.365 | 0.18–0.26 | ≤ 0.005 | ≤ 0.002 | ~0.003 |

What the monitoring shows:

1. **The input features barely drift** (PSI ≤ 0.005 every week). Fraud is ~1 % of traffic, so
   the fraudsters' new behaviour hardly moves the overall distributions. **Input-drift monitoring
   alone would miss this.**
2. **The prediction distribution does move:** the mean fraud score falls **7×**. The model
   "believes" fraud dropped from ~2 % to ~0.3 % while chargebacks haven't fallen. Monitoring
   predictions is a cheap **label-free early warning**.
3. **Labels confirm it.** A rule "alert when weekly PR-AUC falls 30 % below its trailing 4-week
   mean" fires in **week 6**, the week of the change. (It keeps firing in weeks 7–9, then goes
   quiet as the trailing window fills with bad weeks. Alerting on a relative drop eventually
   accepts the new normal, so also keep an **absolute** floor.)

**Response:**

- retrain on recent labelled data (weeks 6–7) **plus** the original training data;
- PR-AUC on weeks 8–11 goes from **0.326** (old) to **1.000** (retrained);
- performance on the **old** pattern stays at **0.993**, so no forgetting;
- the new version passes the registry gate and becomes production (v4), with v3 kept for rollback.

### The full answer to "the fraud model degraded"

1. **Confirm it's real:** check the data pipeline and the monitoring itself (broken joins, missing
   features, a schema change, a logging bug). Compare online vs offline predictions on the same
   rows (training–serving skew, §5).
2. **Localise it:** which segments, countries, merchants, channels? Since when? Did a release,
   upstream data change or business change line up with it?
3. **Diagnose the type:** input drift (PSI by feature), prediction drift, or concept drift
   (performance on labelled recent data). In fraud, labels arrive late (chargebacks take weeks), so
   use proxies: manual-review outcomes, customer disputes, rules hits.
4. **Mitigate now:** roll back if a release caused it; add **rules** for the new pattern (e.g. "15+
   transactions under 5 in 24 h"); adjust thresholds; route more cases to manual review.
5. **Fix properly:** retrain with recent labelled data (keeping older patterns), add features that
   capture the new behaviour (velocity, small-amount counts, device sharing), and validate on the
   latest weeks with a time-based split.
6. **Release safely:** shadow → canary → full, behind the registry gate.
7. **Prevent:** monitor prediction distributions and per-segment metrics, schedule plus
   trigger-based retraining, fast label feedback loops, alerting with absolute and relative
   thresholds, and a written runbook.

## 8. CI/CD for ML: what to automate

| Stage | Checks |
|---|---|
| on every commit | unit tests (feature code), data-validation tests (schema, ranges, null rates: Great Expectations / pandera), a training smoke test on a small sample |
| on training | reproducible run logged to the tracker; evaluation report; **gate vs production** |
| on release | build the image, online-vs-offline prediction parity test, load test (p99), security scan |
| in production | canary with automatic rollback; dashboards for latency, errors, inputs, predictions, labels; scheduled retraining |

This repo's own `run_all.sh` (every chapter's self-checks) is a small CI suite in the same spirit.

---

## Exercises

1. Why is "accuracy dropped" often discovered weeks late in fraud and credit models, and what can
   you monitor in the meantime?
2. A feature's PSI is 0.4 but model performance is unchanged. Retrain?
3. Your p50 latency is 5 ms and p99 is 900 ms. Name three likely causes.
4. What's the difference between a shadow deployment and a canary?

<details><summary>Answers</summary>

1. Labels (chargebacks, defaults) arrive weeks or months later. Meanwhile monitor input drift, the
   **prediction distribution** (§7's 7× drop), proxy labels (manual reviews, disputes), rule-hit
   rates, and business KPIs.
2. Not automatically. Drift in an unimportant feature, or drift the model handles well, needs no
   action. Investigate why it moved (a pipeline bug?), check per-segment performance, and keep
   watching. Retrain when performance (or a reliable proxy) degrades, or on a schedule.
3. Garbage-collection pauses or cold starts (new containers loading the model), slow feature
   lookups or network calls on some requests, oversized batch requests, CPU throttling or noisy
   neighbours, lock contention, a long tail of expensive inputs (very long texts).
4. A **shadow** model receives a copy of live traffic but its outputs aren't used: zero user risk,
   used to compare predictions and latency. A **canary** serves real users a small share of the
   traffic, ramping up while metrics stay healthy, with automatic rollback.
</details>

## Interview questions

**Q: What is training–serving skew and how do you prevent it?** A difference between the features
or preprocessing used in training and at serving: re-implemented logic, different data sources,
time-travel leakage in training joins. Prevent it with one shared pipeline or a feature store,
point-in-time-correct joins, parity tests (online vs offline predictions on the same rows), and
prediction-distribution monitoring.

**Q: Data drift vs concept drift?** Data drift: the input distribution P(X) changes. Concept drift:
the relationship P(y | X) changes, so the same inputs now mean something different. Data drift is
detectable without labels; concept drift usually needs labels or proxies. Either can degrade the
model, or not.

**Q: How do you decide when to retrain?** A combination of a schedule (weekly or monthly, based on
how fast the domain changes), triggers (a performance drop on labelled data, a significant
prediction or input drift on important features), and new data or new features becoming available.
Every retrain goes through the same evaluation gate and a safe rollout.

**Q: What do you log at inference time?** Request ID, model version, the features used (or a hash,
and never raw PII where not allowed), the prediction and score, latency, and errors, so you can
join to labels later, compute drift and debug incidents.

**Q: What does a model registry give you?** A single source of truth for model versions with
lineage (data, code, params, metrics), stage transitions with approvals, reproducible deployment
artefacts, and one-step rollback.

---

**Next:** [15 · Interview prep and projects](../15-Interview-Prep-and-Projects/README.md)
