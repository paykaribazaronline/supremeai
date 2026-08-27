"""
SuperAI Health Check Endpoints
===============================
Comprehensive system health monitoring.

Author: SuperAI Transformation Patch
Version: 1.0.0
"""

import logging
import time
from datetime import datetime

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from sqlalchemy import text

from core.cache import get_cache

router = APIRouter()
logger = logging.getLogger(__name__)

class HealthStatus(BaseModel):
    """Health check response model."""
    status: str
    timestamp: str
    version: str
    uptime_seconds: float
    services: dict
    cache_stats: dict | None = None


_start_time = time.time()


@router.get("/health")
async def health_check():
    """
    Comprehensive health check endpoint.
    """
    cache = get_cache()

    return HealthStatus(
        status="healthy",
        timestamp=datetime.utcnow().isoformat(),
        version="3.0.0-superai",
        uptime_seconds=round(time.time() - _start_time, 2),
        services={
            "database": await _check_database(),
            "redis": await _check_redis(),
            "cache": "connected" if cache.enabled else "disabled"
        },
        cache_stats=cache.get_stats() if cache else None
    )


@router.get("/ready")
async def readiness_check():
    """Kubernetes-style readiness probe."""
    db_ok = await _check_database()
    if db_ok != "healthy":
        raise HTTPException(status_code=503, detail="Database not ready")
    return {"status": "ready"}


@router.get("/live")
async def liveness_check():
    """Kubernetes-style liveness probe."""
    return {"status": "alive"}


async def _check_database() -> str:
    """Check database connectivity."""
    try:
        from database.session import engine
        async with engine.connect() as conn:
            await conn.execute(text("SELECT 1"))
        return "healthy"
    except Exception as e:
        logger.warning(f"Database health check failed: {e}")
        return f"unhealthy: {str(e)}"


async def _check_redis() -> str:
    """Check Redis connectivity."""
    try:
        cache = get_cache()
        if cache._redis:
            await cache._redis.ping()
            return "healthy"
        return "not_configured"
    except Exception as e:
        return f"unhealthy: {str(e)}"
