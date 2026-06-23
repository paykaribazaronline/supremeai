#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ============================================================================
# file >> internal.py
# project >> SupremeAI 2.0
# purpose >> General utility
# module >> api
# ============================================================================
import os
import secrets
from typing import Dict, Any
from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel
from loguru import logger

from core.evolution_engine import EvolutionEngine
from core.config import settings

router = APIRouter()

def _require_admin(request: Request):
    secret = request.headers.get("X-Admin-Secret")
    expected = os.getenv("SUPREMEAI_ADMIN_SECRET", "") or getattr(settings, "docs_password", "") or ""
    if not expected:
        raise HTTPException(status_code=500, detail="Admin secret not configured on server.")
    if not secrets.compare_digest(secret or "", expected):
        raise HTTPException(status_code=403, detail="Forbidden: Invalid admin secret.")


class RunEvolutionRequest(BaseModel):
    task_history: list[Dict[str, Any]] | None = None
    days: int | None = 7


@router.post("/internal/run-daily-evolution")
async def run_daily_evolution(request: Request, payload: RunEvolutionRequest):
    _require_admin(request)
    engine = EvolutionEngine()
    task_history = payload.task_history or []
    try:
        report = engine.run_daily_evolution(task_history)
    except Exception as exc:
        logger.error(f"EvolutionEngine failed: {exc}")
        raise HTTPException(status_code=500, detail=f"Evolution failed: {exc}")
    try:
        from core.gcp_firestore import GCPFirestoreVerificationQueue
        fq = GCPFirestoreVerificationQueue()
        if hasattr(fq, "provider") and fq.provider != "disabled":
            db = getattr(fq, "client", None)
            if db:
                db.collection("evolution_logs").add(report)
    except Exception as exc:
        logger.debug(f"Failed to persist evolution log to Firestore: {exc}")
    return report
