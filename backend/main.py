import os
import signal
import sys

import uvicorn
from loguru import logger

from api.routes import websocket_agent
from api.routes.task_workspace import router as workspace_task_router
from core.app import app  # noqa: F401
from core.config import settings
from core.logging_config import setup_logging
from database import db as supabase_db


app.include_router(workspace_task_router)
app.include_router(websocket_agent.router)

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


def bootstrap_supabase_schema_if_configured() -> None:
    if os.environ.get("SUPABASE_DATABASE_URL") or os.environ.get(
        "SUPABASE_DATABASE_URL_POOLER"
    ):
        try:
            supabase_db.bootstrap_schema()
        except Exception as exc:
            logger.warning(
                f"Supabase bootstrap failed on startup: {exc}. Continuing without schema bootstrap."
            )


@app.on_event("startup")
def startup_bootstrap_supabase_schema() -> None:
    """Ensure Supabase schema bootstrap runs when the FastAPI app starts."""
    bootstrap_supabase_schema_if_configured()


def run_server() -> None:
    port = int(os.environ.get("PORT", settings.port))
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
    bootstrap_supabase_schema_if_configured()
    run_server()
