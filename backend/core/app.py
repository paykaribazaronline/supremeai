from __future__ import annotations

import logging
import os
import sys

# Ensure backend root is in sys.path to resolve top-level packages (api, core, utils)
_backend_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if _backend_dir not in sys.path:
    sys.path.insert(0, _backend_dir)

from fastapi import HTTPException

from api.routers import register_all_routers
from core.admin_routes import router as admin_router
from core.app_builder import create_app
from core.health_check import health_checker

logger = logging.getLogger(__name__)
app = create_app()

# Import and add MemoryAwareMiddleware for Render Free Tier optimization
from core.memory_manager import MemoryAwareMiddleware
app.add_middleware(MemoryAwareMiddleware)


@app.get("/health/aggregated")
async def aggregated_health_check():
    try:
        health_data = await health_checker.check_all()
        return health_data
    except Exception as e:
        logger.error(f"Aggregated health check failed: {e}")
        raise HTTPException(status_code=503, detail=f"Health check service unavailable: {e!s}")


app.include_router(admin_router)
register_all_routers(app)
