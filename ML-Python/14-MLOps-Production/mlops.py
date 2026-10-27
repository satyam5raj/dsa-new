"""
ML 14 : MLOPS AND PRODUCTION
Run:  ../.venv/bin/python mlops.py        (explanations: README.md)

  1. Reproducible artefacts   save/load a whole pipeline, data fingerprint, model card
  2. Experiment tracking      a tiny MLflow-style tracker: params, metrics, best run
  3. Model registry           versions, stages, a promotion gate, rollback
  4. Serving                  a real HTTP prediction server (stdlib), validation, latency p50/p99
  5. Training-serving skew    a re-implemented feature silently changes predictions
  6. Data drift               PSI and the KS test, from scratch
  7. Concept drift scenario   the fraud model that quietly degrades: monitor, detect, retrain, canary
"""
import hashlib
import json
import shutil
import sys
import tempfile
import threading
import time
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from scipy import stats
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import average_precision_score, roc_auc_score
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import FunctionTransformer, OneHotEncoder, StandardScaler

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from mlcheck import check, near, section, show, summary  # noqa: E402

WORK = Path(tempfile.mkdtemp(prefix="mlops-"))
FEATURES = ["amount", "hour", "country", "account_age_days", "txns_last_24h"]


# ---------------------------------------------------------------------------
# synthetic fraud data with a switchable fraud pattern
# ---------------------------------------------------------------------------
def make_transactions(n, seed, pattern="night", fraud_rate=0.02):
    rng = np.random.default_rng(seed)
    df = pd.DataFrame({
        "amount": rng.lognormal(3.5, 1.0, n).round(2),
        "hour": rng.integers(0, 24, n),
        "country": rng.choice(["IN", "US", "UK", "DE"], n, p=[0.5, 0.3, 0.1, 0.1]),
        "account_age_days": rng.integers(1, 3000, n),
        "txns_last_24h": rng.poisson(2, n),
    })
    fraud = rng.random(n) < fraud_rate
    k = int(fraud.sum())
    if pattern == "night":            # old pattern: big amounts at night from new accounts
        df.loc[fraud, "amount"] = rng.lognormal(6.0, 0.5, k).round(2)
        df.loc[fraud, "hour"] = rng.integers(0, 5, k)
        df.loc[fraud, "account_age_days"] = rng.integers(1, 30, k)
    else:                             # new pattern: card testing -- many SMALL rapid transactions, any hour, old accounts
        df.loc[fraud, "amount"] = rng.uniform(1, 5, k).round(2)
        df.loc[fraud, "txns_last_24h"] = rng.integers(15, 40, k)
    df["is_fraud"] = fraud.astype(int)
    return df


def log_amount(X):
    X = X.copy()
    X["amount"] = np.log1p(X["amount"])
    return X


def build_pipeline(model):
    pre = ColumnTransformer([
        ("num", StandardScaler(), ["amount", "hour", "account_age_days", "txns_last_24h"]),
        ("cat", OneHotEncoder(handle_unknown="ignore"), ["country"]),
    ])
    return Pipeline([("log", FunctionTransformer(log_amount)), ("prep", pre), ("model", model)])


def fingerprint(df):
    return hashlib.sha256(pd.util.hash_pandas_object(df, index=True).values.tobytes()).hexdigest()[:12]


# ---------------------------------------------------------------------------
# 1. reproducible artefacts
# ---------------------------------------------------------------------------
def artefacts():
    section("1. Reproducible artefacts")
    train = make_transactions(20000, seed=1)
    pipe = build_pipeline(LogisticRegression(max_iter=1000)).fit(train[FEATURES], train["is_fraud"])
    path = WORK / "fraud_v1.joblib"
    joblib.dump(pipe, path)
    loaded = joblib.load(path)
    sample = train[FEATURES].head(50)
    check("the whole pipeline (preprocessing + model) reloads with identical outputs", True,
          np.array_equal(pipe.predict_proba(sample), loaded.predict_proba(sample)))
    check("data fingerprint is deterministic (same data -> same hash)", fingerprint(train), fingerprint(make_transactions(20000, seed=1)))
    check("...and changes if a single value changes", False, fingerprint(train) == fingerprint(train.assign(amount=train["amount"].where(train.index != 0, 0.0))))
    card = {"name": "fraud-detector", "version": 1, "data_fingerprint": fingerprint(train), "features": FEATURES,
            "trained_on": "synthetic transactions, 20k rows, 2% fraud", "intended_use": "rank transactions for manual review",
            "limitations": "trained on night-time fraud pattern only"}
    (WORK / "model_card.json").write_text(json.dumps(card, indent=2))
    check("a model card travels with the model", "fraud-detector", json.loads((WORK / "model_card.json").read_text())["name"])
    return pipe


# ---------------------------------------------------------------------------
# 2. experiment tracking
# ---------------------------------------------------------------------------
class Tracker:
    """The core of MLflow/W&B: every run records params, metrics and artefacts, append-only."""

    def __init__(self, root):
        self.file = Path(root) / "runs.jsonl"

    def log_run(self, params, metrics, artifact=None):
        run = {"run_id": hashlib.md5(json.dumps(params, sort_keys=True).encode()).hexdigest()[:8],
               "params": params, "metrics": metrics, "artifact": artifact}
        with self.file.open("a") as f:
            f.write(json.dumps(run) + "\n")
        return run["run_id"]

    def runs(self):
        return [json.loads(line) for line in self.file.read_text().splitlines()]

    def best(self, metric):
        return max(self.runs(), key=lambda r: r["metrics"][metric])


def tracking():
    section("2. Experiment tracking")
    train, valid = make_transactions(20000, seed=1), make_transactions(8000, seed=2)
    tr = Tracker(WORK)
    configs = [{"model": "logreg", "C": 1.0}, {"model": "logreg", "C": 0.01}, {"model": "gbm", "lr": 0.1}, {"model": "gbm", "lr": 0.03}]
    for cfg in configs:
        model = LogisticRegression(C=cfg["C"], max_iter=1000) if cfg["model"] == "logreg" else HistGradientBoostingClassifier(learning_rate=cfg["lr"], random_state=0)
        pipe = build_pipeline(model).fit(train[FEATURES], train["is_fraud"])
        p = pipe.predict_proba(valid[FEATURES])[:, 1]
        art = WORK / f"model_{len(tr.runs()) if tr.file.exists() else 0}.joblib"
        joblib.dump(pipe, art)
        tr.log_run({**cfg, "data": fingerprint(train)}, {"pr_auc": round(average_precision_score(valid["is_fraud"], p), 4),
                                                          "roc_auc": round(roc_auc_score(valid["is_fraud"], p), 4)}, str(art))
    for r in tr.runs():
        show(f"run {r['run_id']}: {r['params']['model']:<6} {({k: v for k, v in r['params'].items() if k not in ('model', 'data')})}  PR-AUC {r['metrics']['pr_auc']:.4f}")
    check("every run is logged with its data fingerprint", 4, sum(1 for r in tr.runs() if r["params"]["data"] == fingerprint(train)))
    best = tr.best("pr_auc")
    show(f"best run by PR-AUC: {best['run_id']} ({best['params']['model']})")
    check("the tracker can answer 'which run was best, and with what settings?'", True, best["metrics"]["pr_auc"] >= max(r["metrics"]["pr_auc"] for r in tr.runs()))
    return joblib.load(best["artifact"]), best


# ---------------------------------------------------------------------------
# 3. model registry
# ---------------------------------------------------------------------------
class Registry:
    def __init__(self):
        self.versions, self.production = [], None

    def register(self, model, metrics):
        self.versions.append({"version": len(self.versions) + 1, "model": model, "metrics": metrics, "stage": "staging"})
        return len(self.versions)

    def promote(self, version, holdout, min_gain=0.01):
        """Promotion gate: the candidate must beat production on the SAME holdout by a margin."""
        cand = self.versions[version - 1]
        score = average_precision_score(holdout["is_fraud"], cand["model"].predict_proba(holdout[FEATURES])[:, 1])
        if self.production is not None:
            prod = self.versions[self.production - 1]
            prod_score = average_precision_score(holdout["is_fraud"], prod["model"].predict_proba(holdout[FEATURES])[:, 1])
            if score < prod_score + min_gain:
                return False, score, prod_score
            prod["stage"] = "archived"
        cand["stage"] = "production"
        self.production = version
        return True, score, None

    def rollback(self):
        current = self.versions[self.production - 1]
        current["stage"] = "archived"
        previous = max(v["version"] for v in self.versions if v["version"] < self.production and v["stage"] == "archived")
        self.versions[previous - 1]["stage"] = "production"
        self.production = previous


def registry(baseline, best):
    section("3. Model registry")
    holdout = make_transactions(8000, seed=3)
    reg = Registry()
    v1 = reg.register(baseline, {"note": "logreg baseline"})
    ok, s1, _ = reg.promote(v1, holdout)
    check("first model goes straight to production", (True, "production"), (ok, reg.versions[0]["stage"]))
    weak = build_pipeline(LogisticRegression(C=1e-4, max_iter=1000)).fit(*(lambda d: (d[FEATURES], d["is_fraud"]))(make_transactions(20000, seed=1)))
    v2 = reg.register(weak, {"note": "over-regularised"})
    ok2, s2, prod_s = reg.promote(v2, holdout)
    show(f"candidate v2 PR-AUC {s2:.4f} vs production {prod_s:.4f}")
    check("the gate blocks a model that isn't clearly better", False, ok2)
    v3 = reg.register(best[0], best[1])
    ok3, s3, _ = reg.promote(v3, holdout)
    show(f"candidate v3 PR-AUC {s3:.4f} vs production v1 {s1:.4f}")
    check("a clearly better model is promoted", (True, 3), (ok3, reg.production))
    reg.rollback()
    check("rollback restores the previous production model in one step", 1, reg.production)
    reg.promote(v3, holdout)
    return reg


# ---------------------------------------------------------------------------
# 4. serving
# ---------------------------------------------------------------------------
SCHEMA = {"amount": (float, int), "hour": (int,), "country": (str,), "account_age_days": (int,), "txns_last_24h": (int,)}


def make_handler(model, latencies):
    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *args):                       # keep the test output quiet
            pass

        def _send(self, code, body):
            data = json.dumps(body).encode()
            self.send_response(code)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)

        def do_GET(self):
            if self.path == "/health":
                self._send(200, {"status": "ok"})
            else:
                self._send(404, {"error": "not found"})

        def do_POST(self):
            t0 = time.perf_counter()
            try:
                payload = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
                rows = payload["instances"]
                for r in rows:                                    # validate BEFORE the model sees it
                    for f, types in SCHEMA.items():
                        if f not in r or not isinstance(r[f], types):
                            raise ValueError(f"field '{f}' missing or wrong type")
                    if not 0 <= r["hour"] <= 23 or r["amount"] < 0:
                        raise ValueError("value out of range")
            except (ValueError, KeyError, json.JSONDecodeError) as e:
                self._send(400, {"error": str(e)})
                return
            p = model.predict_proba(pd.DataFrame(rows)[FEATURES])[:, 1]
            latencies.append(time.perf_counter() - t0)
            self._send(200, {"fraud_probability": [round(float(v), 6) for v in p], "model_version": 3})
    return Handler


def call(url, body=None):
    req = urllib.request.Request(url, data=None if body is None else json.dumps(body).encode(),
                                 headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=5) as r:
            return r.status, json.loads(r.read())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read())


def serving(reg):
    section("4. Serving over HTTP")
    model = reg.versions[reg.production - 1]["model"]
    latencies = []
    server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(model, latencies))
    port = server.server_address[1]
    threading.Thread(target=server.serve_forever, daemon=True).start()
    base = f"http://127.0.0.1:{port}"
    try:
        check("GET /health", (200, {"status": "ok"}), call(base + "/health"))
        night = {"amount": 900.0, "hour": 2, "country": "IN", "account_age_days": 3, "txns_last_24h": 1}
        normal = {"amount": 30.0, "hour": 14, "country": "US", "account_age_days": 1500, "txns_last_24h": 2}
        code, body = call(base + "/predict", {"instances": [night, normal]})
        show(f"POST /predict -> {code} {body}")
        check("the server scores a suspicious transaction far above a normal one", True, code == 200 and body["fraud_probability"][0] > 10 * body["fraud_probability"][1])
        offline = model.predict_proba(pd.DataFrame([night, normal])[FEATURES])[:, 1]
        check("online predictions == offline predictions (no skew in this path)", True, np.allclose(body["fraud_probability"], offline, atol=1e-6))
        code, body = call(base + "/predict", {"instances": [{**normal, "hour": 31}]})
        check("bad input is rejected with 400 before reaching the model", (400, "value out of range"), (code, body["error"]))
        code, body = call(base + "/predict", {"instances": [{k: v for k, v in normal.items() if k != "country"}]})
        check("a missing field is rejected with a clear message", 400, code)
        for _ in range(200):
            call(base + "/predict", {"instances": [normal]})
        p50, p99 = np.percentile(latencies, [50, 99]) * 1000
        show(f"model latency over {len(latencies)} requests: p50 {p50:.2f} ms, p99 {p99:.2f} ms")
        check("p99 >= p50 (always report tail latency, not just the average)", True, p99 >= p50)
    finally:
        server.shutdown()
        server.server_close()


# ---------------------------------------------------------------------------
# 5. training-serving skew
# ---------------------------------------------------------------------------
def skew():
    section("5. Training-serving skew")
    train = make_transactions(20000, seed=1)
    pipe = build_pipeline(LogisticRegression(max_iter=1000)).fit(train[FEATURES], train["is_fraud"])
    test = make_transactions(5000, seed=4)
    good = pipe.predict_proba(test[FEATURES])[:, 1]
    # a serving team re-implements the features in another language and forgets that training used log1p(amount)
    pre, clf = pipe.named_steps["prep"], pipe.named_steps["model"]
    skewed = clf.predict_proba(pre.transform(test[FEATURES]))[:, 1]          # skips the log step
    auc_good, auc_skew = roc_auc_score(test["is_fraud"], good), roc_auc_score(test["is_fraud"], skewed)
    flagged_good, flagged_skew = np.mean(good > 0.5), np.mean(skewed > 0.5)
    show(f"same model, same rows: ROC-AUC {auc_good:.3f} vs {auc_skew:.3f}; share flagged at 0.5: {flagged_good:.2%} vs {flagged_skew:.2%}")
    check("the skewed feature silently changes predictions (no error raised!)", True, flagged_skew > 3 * flagged_good)
    check("comparing online vs offline predictions on the same rows catches it", True, np.max(np.abs(good - skewed)) > 0.5)


# ---------------------------------------------------------------------------
# 6. data drift
# ---------------------------------------------------------------------------
def psi(expected, actual, bins=10):
    """Population Stability Index: sum (a - e) * ln(a / e) over quantile bins of the reference data."""
    edges = np.unique(np.quantile(expected, np.linspace(0, 1, bins + 1)))   # unique: discrete features have tied quantiles
    edges[0], edges[-1] = -np.inf, np.inf
    e = np.histogram(expected, edges)[0] / len(expected)
    a = np.histogram(actual, edges)[0] / len(actual)
    e, a = np.clip(e, 1e-6, None), np.clip(a, 1e-6, None)
    return float(np.sum((a - e) * np.log(a / e)))


def data_drift():
    section("6. Data drift: PSI and KS")
    rng = np.random.default_rng(5)
    ref = rng.normal(50, 10, 20000)
    same = rng.normal(50, 10, 5000)
    shifted = rng.normal(54, 10, 5000)
    big = rng.normal(65, 15, 5000)
    for name, cur in (("same distribution", same), ("mean +0.4 std", shifted), ("mean +1.5 std, wider", big)):
        ks = stats.ks_2samp(ref, cur)
        show(f"{name:<22} PSI {psi(ref, cur):.3f}   KS statistic {ks.statistic:.3f}, p = {ks.pvalue:.2g}")
    check("PSI ~0 when nothing changed (< 0.1 = stable)", True, psi(ref, same) < 0.1)
    check("PSI 0.1-0.25 = moderate shift: investigate", True, 0.1 <= psi(ref, shifted) < 0.25)
    check("PSI > 0.25 = major shift: act", True, psi(ref, big) > 0.25)
    huge_a, huge_b = rng.normal(50, 10, 500_000), rng.normal(50.2, 10, 500_000)
    show(f"tiny shift (+0.02 std) with 500k rows: KS p = {stats.ks_2samp(huge_a, huge_b).pvalue:.1e}, PSI {psi(huge_a, huge_b):.4f}")
    check("with huge samples KS calls a negligible shift 'significant'; PSI (effect size) does not", True,
          stats.ks_2samp(huge_a, huge_b).pvalue < 0.01 and psi(huge_a, huge_b) < 0.01)


# ---------------------------------------------------------------------------
# 7. the degrading fraud model
# ---------------------------------------------------------------------------
def concept_drift(reg):
    section("7. Scenario: the fraud model that quietly degrades")
    model = reg.versions[reg.production - 1]["model"]
    train = make_transactions(20000, seed=1)
    weeks = []
    for w in range(12):
        pattern = "night" if w < 6 else "card-testing"             # fraudsters change tactics in week 6
        mix = make_transactions(6000, seed=100 + w, pattern=pattern)
        if w >= 6:                                                 # the old pattern doesn't vanish overnight
            mix = pd.concat([mix, make_transactions(6000, seed=200 + w, pattern="night", fraud_rate=0.005)], ignore_index=True)
        p = model.predict_proba(mix[FEATURES])[:, 1]
        weeks.append({"week": w, "pr_auc": average_precision_score(mix["is_fraud"], p),
                      "recall@1%": recall_at_top(mix["is_fraud"].values, p, 0.01),
                      "psi_amount": psi(np.log1p(train["amount"]), np.log1p(mix["amount"])),
                      "psi_txns": psi(train["txns_last_24h"], mix["txns_last_24h"]),
                      "mean_score": float(p.mean()), "data": mix})
    for r in weeks:
        show(f"week {r['week']:2d}: PR-AUC {r['pr_auc']:.3f}  recall in top 1% {r['recall@1%']:.2f}  "
             f"PSI(amount) {r['psi_amount']:.3f}  PSI(txns_24h) {r['psi_txns']:.3f}  mean score {r['mean_score']:.4f}")
    before, after = np.mean([r["pr_auc"] for r in weeks[:6]]), np.mean([r["pr_auc"] for r in weeks[6:]])
    check("PR-AUC collapses after fraudsters switch tactics (concept drift)", True, after < before / 2)
    check("input-feature drift is invisible (PSI < 0.1 every week): fraud is ~1% of traffic", True,
          max(max(r["psi_amount"], r["psi_txns"]) for r in weeks) < 0.1)
    s_before, s_after = np.mean([r["mean_score"] for r in weeks[:6]]), np.mean([r["mean_score"] for r in weeks[6:]])
    check("...but the PREDICTION distribution moved (mean score fell > 3x): a label-free early warning", True, s_after < s_before / 3)

    # alerting rule on labelled weekly metrics: alert when PR-AUC drops 30% below the trailing 4-week mean
    alerts = [r["week"] for i, r in enumerate(weeks) if i >= 4 and r["pr_auc"] < 0.7 * np.mean([x["pr_auc"] for x in weeks[i - 4:i]])]
    show(f"alert fired in week(s): {alerts}")
    check("the monitoring rule fires in the week the pattern changed", 6, alerts[0] if alerts else None)

    # retrain on recent labelled data (weeks 6-7), evaluate on week 8+, canary against the old model
    recent = pd.concat([r["data"] for r in weeks[6:8]] + [train], ignore_index=True)
    new = build_pipeline(HistGradientBoostingClassifier(random_state=0)).fit(recent[FEATURES], recent["is_fraud"])
    future = pd.concat([r["data"] for r in weeks[8:]], ignore_index=True)
    old_s = average_precision_score(future["is_fraud"], model.predict_proba(future[FEATURES])[:, 1])
    new_s = average_precision_score(future["is_fraud"], new.predict_proba(future[FEATURES])[:, 1])
    old_night = make_transactions(10000, seed=999, pattern="night")
    new_on_old = average_precision_score(old_night["is_fraud"], new.predict_proba(old_night[FEATURES])[:, 1])
    show(f"weeks 8-11 PR-AUC: old model {old_s:.3f}, retrained model {new_s:.3f}; retrained model on the OLD pattern {new_on_old:.3f}")
    check("retraining on recent labelled data recovers performance", True, new_s > old_s + 0.3)
    check("...without forgetting the old pattern (training data kept both)", True, new_on_old > 0.8)
    v = reg.register(new, {"note": "retrained after week-6 drift"})
    ok, _, _ = reg.promote(v, future)
    check("it passes the registry gate and becomes production", (True, v), (ok, reg.production))


def recall_at_top(y, p, frac):
    k = max(1, int(len(p) * frac))
    top = np.argsort(-p)[:k]
    return float(y[top].sum() / max(y.sum(), 1))


if __name__ == "__main__":
    try:
        baseline = artefacts()
        best = tracking()
        reg = registry(baseline, best)
        serving(reg)
        skew()
        data_drift()
        concept_drift(reg)
    finally:
        shutil.rmtree(WORK, ignore_errors=True)
    summary()
