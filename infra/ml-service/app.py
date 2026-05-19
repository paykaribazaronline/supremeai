"""
BERT Sentiment ML Inference Service
Provides a rate-limited, authentication-gated prediction endpoint backed by
a pre-trained distilled BERT model from HuggingFace.

Endpoints
---------
POST /predict   – return sentiment (json)
GET  /health     – liveness probe
GET  /ready      – readiness probe (model loaded)
"""

import hashlib
import json
import logging
import os
import time
from collections import defaultdict
from datetime import datetime, timezone
from threading import Lock, local

import torch
import torch.nn.functional as F
from flask import Flask, jsonify, request, g
from google.cloud import secretmanager
from prometheus_client import Counter, Histogram, generate_latest, REGISTRY
from transformers import AutoModelForSequenceClassification, AutoTokenizer

# ── Configuration ───────────────────────────────────────────────────────────
MODEL_NAME         = os.getenv("MODEL_NAME", "distilbert-base-uncased-finetuned-sst-2-english")
MAX_TEXT_LENGTH    = int(os.getenv("MAX_TEXT_LENGTH", "5000"))
PORT               = int(os.getenv("PORT", "8080"))
RATE_LIMIT         = int(os.getenv("RATE_LIMIT_RPS", "100"))  # req/min per IP
GCP_PROJECT        = os.getenv("GCP_PROJECT_ID", "")
SECRET_NAME        = os.getenv("ML_API_KEY_SECRET", "ml-api-gateway-key")

# ── Metrics ─────────────────────────────────────────────────────────────────
REQUEST_COUNTER    = Counter("ml_inference_requests_total",  "Total inference requests",   ["status"])
LATENCY_HISTOGRAM  = Histogram("ml_inference_latency_seconds", "Inference latency (s)",      ["status"])
CONFIDENCE_HIST    = Histogram("ml_prediction_confidence",       "Prediction confidence bucket", ["sentiment"])

# ── Logging ─────────────────────────────────────────────────────────────────
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
log = logging.getLogger("sentiment-ml")

# ── Token-bucket rate limiter (per IP) ───────────────────────────────────────
class TokenBucket:
    def __init__(self, capacity: int, refill_per_min: float):
        self._capacity   = capacity
        self._tokens     = defaultdict(lambda: capacity)
        self._last       = defaultdict(time.time)
        self._lock       = Lock()

    def consume(self, key: str) -> bool:
        now   = time.time()
        delta = now - self._last[key]
        self._tokens[key] = min(self._capacity,
                                self._tokens[key] + delta * (self._capacity / 60.0))
        self._last[key] = now
        if self._tokens[key] >= 1:
            self._tokens[key] -= 1
            return True
        return False

rate_limiter = TokenBucket(capacity=RATE_LIMIT, refill_per_min=RATE_LIMIT)

# ── API-Key cache ────────────────────────────────────────────────────────────
api_key: str | None = None


def fetch_api_key() -> str:
    global api_key
    if api_key:
        return api_key
    client = secretmanager.SecretManagerServiceClient()
    path   = f"projects/{GCP_PROJECT}/secrets/{SECRET_NAME}/versions/latest"
    payload = client.access_secret_version(request={"name": path}).payload.data
    api_key = payload.decode("utf-8")
    return api_key


# ── Model ────────────────────────────────────────────────────────────────────
tokenizer: AutoTokenizer | None  = None
model:    AutoModelForSequenceClassification | None = None
model_ready = False


def load_model() -> None:
    global tokenizer, model, model_ready
    log.info("Downloading model %s …", MODEL_NAME)
    tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
    model     = AutoModelForSequenceClassification.from_pretrained(MODEL_NAME)
    model.eval()
    model_ready = True
    log.info("Model loaded and ready.")


# ── Inference ────────────────────────────────────────────────────────────────
LABELS = {0: "negative", 1: "positive"}


def predict_sentiment(text: str) -> dict:
    inputs = tokenizer(text, truncation=True, padding=True,
                       max_length=512, return_tensors="pt")
    with torch.no_grad():
        logits   = model(**inputs).logits
        probs    = F.softmax(logits, dim=-1)[0]
        conf_idx  = int(torch.argmax(probs).item())
        confidence = float(probs[conf_idx].item())
        label     = "positive" if conf_idx == 1 else "negative"
    return {
        "sentiment":     label,
        "confidence":    round(confidence, 6),
        "processed_at":  datetime.now(timezone.utc).isoformat(),
        "model":         MODEL_NAME,
    }


# ── Flask App ────────────────────────────────────────────────────────────────
app = Flask(__name__)


@app.before_request
def startup_load():
    """Lazy-load model on first request (allows Cloud Run startup probe to pass)."""
    if not model_ready:
        load_model()


@app.route("/health")
def health():
    return jsonify({"status": "healthy", "service": "sentiment-ml", "timestamp":
                    datetime.now(timezone.utc).isoformat()}), 200


@app.route("/ready")
def ready():
    return jsonify({
        "ready":    model_ready,
        "service":  "sentiment-ml",
        "model":    MODEL_NAME,
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }), 200 if model_ready else 503


@app.route("/metrics")
def metrics():
    return generate_latest(REGISTRY), 200, {"Content-Type": "text/plain; version=0.0.4"}


@app.route("/predict", methods=["POST"])
def predict():
    # ── Auth ─────────────────────────────────────────────────────────────────
    provided = request.headers.get("X-API-Key", "")
    if provided != fetch_api_key():
        REQUEST_COUNTER.labels(status="unauthorized").inc()
        return jsonify({"error": "Unauthorized"}), 401

    # JSON missing — 400 before rate limiting so legit callers aren't penalised
    if not request.is_json:
        REQUEST_COUNTER.labels(status="bad_request").inc()
        return jsonify({"error": "Request body must be JSON."}), 400

    # ── Rate limit ───────────────────────────────────────────────────────────
    client_ip = request.remote_addr or "unknown"
    if not rate_limiter.consume(client_ip):
        REQUEST_COUNTER.labels(status="rate_limited").inc()
        return jsonify({"error": "Rate limit exceeded. Try again in 60s.",
                        "limit_per_min": RATE_LIMIT}), 429

    # ── Payload validation ───────────────────────────────────────────────────
    payload = request.get_json(silent=True)
    if not payload:
        REQUEST_COUNTER.labels(status="bad_request").inc()
        return jsonify({"error": "Empty or invalid JSON body."}), 400

    text = payload.get("text")
    if not isinstance(text, str) or not text.strip():
        REQUEST_COUNTER.labels(status="bad_request").inc()
        return jsonify({
            "error": "Field 'text' is required and must be a non-empty string."
        }), 400

    if len(text) > MAX_TEXT_LENGTH:
        REQUEST_COUNTER.labels(status="bad_request").inc()
        return jsonify({
            "error": f"'text' exceeds maximum length of {MAX_TEXT_LENGTH} characters.",
            "max_length": MAX_TEXT_LENGTH,
            "received":   len(text),
        }), 400

    # ── Inference ────────────────────────────────────────────────────────────
    try:
        with LATENCY_HISTOGRAM.labels(status="success").time():
            result = predict_sentiment(text)

        REQUEST_COUNTER.labels(status="success").inc()
        CONFIDENCE_HIST.labels(sentiment=result["sentiment"]).observe(result["confidence"])
        return jsonify(result), 200

    except Exception as exc:
        log.exception("Inference failed: %s", exc)
        re = {"error": "Inference failed.", "detail": str(exc)}
        REQUEST_COUNTER.labels(status="error").inc()
        return jsonify(re), 500


if __name__ == "__main__":
    load_model()
    app.run(host="0.0.0.0", port=PORT)
