# backend/api/deps.py
"""Enhanced dependency injection with standardized error handling.

 replaces api/dependencies.py — integrates ErrorEventBus for all
dependency failures and provides typed request/tenant extraction helpers.
"""

from __future__ import annotations

from typing import Any

from api.errors import raise_unauthorized
from core.error_bus import with_error_bus
from core.evolution.fitness_engine import FitnessEngine
from core.tenant_db import TenantAwareFirestore
from fastapi import Depends, HTTPException, Request
from loguru import logger
from utils.environment import is_test_environment

_fitness_engine = FitnessEngine()


def get_fitness_engine() -> FitnessEngine:
    return _fitness_engine


@with_error_bus(component_name="AuthDependency")
async def get_current_user_token(request: Request) -> dict[str, Any]:
    user = getattr(request.state, "user", None)
    if user:
        return user

    if is_test_environment():
        return {"sub": "admin@supremeai.com", "role": "admin"}

    raise_unauthorized("Missing or invalid authentication token.")
    return None


def get_tenant_db(
    payload: dict[str, Any] = Depends(get_current_user_token),
) -> TenantAwareFirestore:
    """Extract tenant_id from JWT and return an isolated Firestore client."""
    tenant_id = payload.get("sub")
    if not tenant_id:
        logger.error("Token payload missing 'sub' (tenant_id) claim.")
        raise HTTPException(status_code=401, detail="Invalid token structure.")

    return TenantAwareFirestore(tenant_id=tenant_id)


__all__ = ["get_fitness_engine", "get_current_user_token", "get_tenant_db"]
