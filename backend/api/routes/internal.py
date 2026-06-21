import os
import time
import json
import secrets
from typing import Dict, Any
from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel
from loguru import logger

from core.evolution_engine import EvolutionEngine
from config import settings

router = APIRouter()


def _require_admin(request: Request):
    secret = request.headers.get("X-Admin-Secret")
    expected = os.getenv("SUPREMEAI_ADMIN_SECRET", "") or getattr(settings, "docs_password", "") or "supreme-god-password"
    if not secrets.compare_digest(secret or "", expected):
        raise HTTPException(status_code=403, detail="Forbidden: Invalid admin secret.")


class RunEvolutionRequest(BaseModel):
    task_history: list[Dict[str, Any]] | None = None


@router.post("/internal/run-daily-evolution")
async def run_daily_evolution(request: Request, payload: RunEvolutionRequest):
    _require_admin(request)
    engine = EvolutionEngine()
    task_history = payload.task_history or []
    report = engine.run_daily_evolution(task_history)
    try:
        from core.gcp_firestore import GCPFirestoreVerificationQueue
        fq = GCPFirestoreVerificationQueue()
        if hasattr(fq, "provider") and fq.provider != "disabled":
            db = fq._get_client()
            if db:
                db.collection("evolution_logs").add(report)
    except Exception as exc:
        logger.debug(f"Failed to persist evolution log to Firestore: {exc}")
    return report
