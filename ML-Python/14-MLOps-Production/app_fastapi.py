"""
Reference production server for the fraud model -- NOT run by run_all.sh (FastAPI isn't in the course venv).

    pip install fastapi uvicorn pydantic joblib scikit-learn pandas
    MODEL_PATH=model.joblib uvicorn app_fastapi:app --host 0.0.0.0 --port 8080

It mirrors mlops.py section 4, with the pieces a real service adds: typed request validation (pydantic),
the model loaded once at startup, health and readiness endpoints, a model version on every response,
and structured logs that a monitoring job can read (prediction distribution, latency). Check the current
FastAPI docs before copying -- APIs evolve.
"""
import json
import logging
import os
import time
from contextlib import asynccontextmanager
from typing import List, Literal

import joblib
import pandas as pd
from fastapi import FastAPI
from pydantic import BaseModel, Field

MODEL_PATH = os.environ.get("MODEL_PATH", "model.joblib")
MODEL_VERSION = os.environ.get("MODEL_VERSION", "dev")
FEATURES = ["amount", "hour", "country", "account_age_days", "txns_last_24h"]
log = logging.getLogger("fraud-api")
logging.basicConfig(level=logging.INFO, format="%(message)s")
state = {}


class Transaction(BaseModel):
    amount: float = Field(ge=0)
    hour: int = Field(ge=0, le=23)
    country: Literal["IN", "US", "UK", "DE"]          # unknown values get a 422, not a silent guess
    account_age_days: int = Field(ge=0)
    txns_last_24h: int = Field(ge=0)


class PredictRequest(BaseModel):
    instances: List[Transaction] = Field(min_length=1, max_length=1000)


@asynccontextmanager
async def lifespan(app):
    state["model"] = joblib.load(MODEL_PATH)        # load ONCE, not per request
    yield
    state.clear()


app = FastAPI(title="fraud-detector", lifespan=lifespan)


@app.get("/health")                                  # liveness: the process is up
def health():
    return {"status": "ok"}


@app.get("/ready")                                   # readiness: the model is loaded, send traffic
def ready():
    return {"ready": "model" in state, "model_version": MODEL_VERSION}


@app.post("/predict")
def predict(req: PredictRequest):
    t0 = time.perf_counter()
    df = pd.DataFrame([t.model_dump() for t in req.instances])[FEATURES]
    probs = state["model"].predict_proba(df)[:, 1].tolist()
    latency_ms = (time.perf_counter() - t0) * 1000
    # one structured log line per request -> feeds drift / latency dashboards (never log raw PII)
    log.info(json.dumps({"event": "prediction", "model_version": MODEL_VERSION, "n": len(probs),
                         "mean_score": sum(probs) / len(probs), "latency_ms": round(latency_ms, 2)}))
    return {"fraud_probability": probs, "model_version": MODEL_VERSION}
