import os
import signal
import sys

import uvicorn
from loguru import logger

from api.routes import websocket_agent
from api.routes.admin import router as admin_router
from api.routes.agent_workspace import router as agent_router

# from api.routes.integrations import router as integrations_router # Original line causing the error
from api.routes.task_workspace import router as workspace_task_router
from core.app import app  # noqa: F401
from core.config import settings
from core.logging_config import setup_logging


# Attempt to import integrations router, handling missing ENCRYPTION_KEY for CI/dev environments.
# The core.security_vault module raises a ValueError if ENCRYPTION_KEY is not set.
# This conditional import prevents test collection from failing entirely when the key isn't provided,
# while still enforcing its presence in production environments.
integrations_router = None
if settings.env.lower() == "production":
    # In production, the ENCRYPTION_KEY is critical and must be present.
    # Allow the ValueError to propagate if it's missing.
    from api.routes.integrations import router as _integrations_router
    integrations_router = _integrations_router
else:
    # In other environments (e.g., 'local', 'test', 'development'),
    # we can be more lenient to allow application startup/test collection
    # even if ENCRYPTION_KEY is not explicitly set for all components.
    # Functionality relying on encryption will be disabled or limited.
    try:
        from api.routes.integrations import router as _integrations_router
        integrations_router = _integrations_router
    except ValueError as e:
        # Check if the error is specifically about the ENCRYPTION_KEY.
        if "ENCRYPTION_KEY" in str(e):
            logger.warning(
                f"Integrations router not loaded due to missing ENCRYPTION_KEY in '{settings.env}' environment: {e}. "
                "Integrations functionality will be limited or unavailable."
            )
        else:
            # Re-raise other ValueErrors that are not related to ENCRYPTION_KEY
            # as they might indicate different critical issues.
            raise
    except Exception as e:
        logger.error(f"Failed to import integrations router for an unexpected reason in '{settings.env}' environment: {e}")


app.include_router(workspace_task_router)
app.include_router(websocket_agent.router)
app.include_router(agent_router, prefix="/api/v1")
if integrations_router:  # Only include the router if it was successfully loaded
    app.include_router(integrations_router, prefix="/api/v1")
app.include_router(admin_router)

setup_logging()

if settings.env.lower() == "production":
    try:
        settings.validate_config()
    except RuntimeError as exc:
        logger.error(f"Production config validation failed: {exc}")
        sys.exit(1)


def _handle_sigterm(signum, frame):
    logger.info("Received shutdown signal. Performing graceful shutdown...")
    sys.exit(0)


signal.signal(signal.SIGTERM, _handle_sigterm)
signal.signal(signal.SIGINT, _handle_sigterm)


def run_server() -> None:
    port = int(os.environ.get("PORT", 8080))
    is_local = settings.env == "local"
    uvicorn_kwargs = {
        "host": settings.host,
        "port": port,
        "log_level": "info",
        "access_log": True,
        "timeout_keep_alive": 30,
    }
    if is_local:
        uvicorn_kwargs["reload"] = True
    else:
        uvicorn_kwargs["reload"] = False
        workers = int(os.environ.get("GUNICORN_WORKERS", "4"))
        if workers > 1:
            uvicorn_kwargs["workers"] = workers

    try:
        uvicorn.run("main:app", **uvicorn_kwargs)
    except Exception as exc:
        logger.error(f"Server failed to start: {exc}")
        sys.exit(1)


if __name__ == "__main__":
    run_server()
