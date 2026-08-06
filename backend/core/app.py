from __future__ import annotations

import logging

from api.routers import register_all_routers
from core.admin_routes import router as admin_router
from core.app_builder import create_app
from core.health_check import health_checker
from fastapi import HTTPException

logger = logging.getLogger(__name__)
app = create_app()


@app.get("/health/aggregated")
async def aggregated_health_check():
    try:
        health_data = await health_checker.check_all()
        return health_data
    except Exception as e:
        logger.error(f"Aggregated health check failed: {e}")
        raise HTTPException(
            status_code=503, detail=f"Health check service unavailable: {e!s}"
        ) from e


app.include_router(admin_router)
register_all_routers(app)
