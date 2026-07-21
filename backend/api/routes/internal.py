import secrets
from typing import Any

from core.config import settings
from core.evolution.evolution_engine import EvolutionEngine
from fastapi import APIRouter, HTTPException, Request
from loguru import logger
from pydantic import BaseModel

router = APIRouter()


def _require_admin(request: Request):
    secret = request.headers.get("X-Admin-Secret")
    expected = (
        getattr(settings, "supremeai_admin_secret", "")
        or getattr(settings, "docs_password", "")
        or ""
    )
    if not expected:
        raise HTTPException(
            status_code=500, detail="Admin secret not configured on server."
        )
    if not secrets.compare_digest(secret or "", expected):
        raise HTTPException(status_code=403, detail="Forbidden: Invalid admin secret.")


class RunEvolutionRequest(BaseModel):
    task_history: list[dict[str, Any]] | None = None
    days: int | None = 7


@router.post("/internal/run-daily-evolution")
async def run_daily_evolution(request: Request, payload: RunEvolutionRequest):
    _require_admin(request)
    engine = EvolutionEngine()
    task_history = payload.task_history or []
    try:
        # বাংলা মন্তব্য: run_daily_evolution অ্যাসিঙ্ক হওয়ায় এখানে await ব্যবহার করা হলো।
        report = await engine.run_daily_evolution(task_history)
    except Exception as exc:  # noqa: BLE001
        logger.error(f"EvolutionEngine failed: {exc}")
        raise HTTPException(status_code=500, detail=f"Evolution failed: {exc}") from exc
    try:
        from core.gcp_firestore import GCPFirestoreVerificationQueue

        fq = GCPFirestoreVerificationQueue()
        if hasattr(fq, "provider") and fq.provider != "disabled":
            db = getattr(fq, "client", None)
            if db:
                db.collection("evolution_logs").add(report)
    except Exception as exc:  # noqa: BLE001
        logger.debug(f"Failed to persist evolution log to Firestore: {exc}")
    try:
        from database.supabase_client import db as supabase_db

        if supabase_db.client:
            supabase_db.append_evolution_log(report)
    except Exception as exc:  # noqa: BLE001
        logger.debug(f"Failed to persist evolution log to Supabase: {exc}")
    return report
