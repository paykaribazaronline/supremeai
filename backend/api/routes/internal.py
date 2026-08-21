import secrets
from typing import Any

from fastapi import APIRouter, Depends, HTTPException, Request
from loguru import logger
from pydantic import BaseModel

from core.config import settings
from core.evolution.evolution_engine import EvolutionEngine
from core.security.rbac import get_current_admin

router = APIRouter(dependencies=[Depends(get_current_admin)])


def _require_admin(request: Request):
    secret = request.headers.get("X-Admin-Secret")
    expected = getattr(settings, "supremeai_admin_secret", "") or getattr(settings, "docs_password", "") or ""
    if not expected:
        raise HTTPException(status_code=500, detail="Admin secret not configured on server.")
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
    except Exception as exc:
        logger.error(f"EvolutionEngine failed: {exc}")
        raise HTTPException(status_code=500, detail=f"Evolution failed: {exc}") from exc
    try:
        from core.gcp_firestore import GCPFirestoreVerificationQueue

        fq = GCPFirestoreVerificationQueue()
        if hasattr(fq, "provider") and fq.provider != "disabled":
            db = getattr(fq, "client", None)
            if db:
                db.collection("evolution_logs").add(report)
    except Exception as exc:
        logger.debug(f"Failed to persist evolution log to Firestore: {exc}")
    try:
        from database.supabase_client import db as supabase_db

        if supabase_db.client:
            supabase_db.append_evolution_log(report)
    except Exception as exc:
        logger.debug(f"Failed to persist evolution log to Supabase: {exc}")
    return report

class SystemAlertPayload(BaseModel):
    level: str
    message: str

@router.post("/api/v1/admin/alerts")
async def report_system_alert(request: Request, payload: SystemAlertPayload):
    # Allow if valid API key is present
    if not hasattr(request.state, "api_key") or not request.state.api_key:
        # Fallback to Admin Secret if API key is missing
        _require_admin(request)

    logger.bind(alert_level=payload.level).warning(f"System Alert Received: {payload.message}")

    # Optionally store in DB/Redis or emit via ErrorEventBus
    from core.messaging.event_bus import ErrorEvent, error_event_bus, ErrorContext

    error_event_bus.emit(
        ErrorEvent(
            module="ClientMonitor",
            error_type="CLIENT_ALERT",
            message=payload.message,
            severity=payload.level.upper(),
            structured_context=ErrorContext(
                module="tests.e2e",
                env=settings.env,
            ),
        )
    )

    return {"status": "received", "level": payload.level}
