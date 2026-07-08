# FILE_PATH: main.py
import os
import signal
import sys

import uvicorn
from loguru import logger

from api.routes import websocket_agent
from api.routes.admin import router as admin_router
from api.routes.agent_workspace import router as agent_router
from api.routes.integrations import router as integrations_router
from api.routes.public_config import router as public_config_router
from api.routes.task_workspace import router as workspace_task_router
from core.config import settings


# Fix for CI/test environment:
# The `sqlite3.OperationalError: unable to open database file` occurs during test collection
# because `AdminGodLayer` (imported via `core.app -> core.lifespan -> core.services`)
# attempts to connect to a SQLite database file specified by `settings.admin_rules_db`
# at module import time. In a CI environment, this path might not exist or be writable.
# The logs indicate "Firestore unavailable or in test mode. AdminGodLayer using local SQLite fallback."
# which means `settings.test_mode` is likely active.
#
# By overriding `settings.admin_rules_db` to `":memory:"` when `settings.test_mode` is true,
# we ensure that an in-memory SQLite database is used for tests, preventing filesystem errors.
if settings.test_mode:
    logger.info("Detected test mode. Overriding settings.admin_rules_db to use in-memory SQLite.")
    settings.admin_rules_db = ":memory:"

from core.app import app  # noqa: F401
from core.logging_config import setup_logging


app.include_router(workspace_task_router)
app.include_router(websocket_agent.router)
app.include_router(agent_router, prefix="/api/v1")
app.include_router(integrations_router, prefix="/api/v1")
app.include_router(admin_router)
app.include_router(public_config_router, prefix="/api")

setup_logging()

if settings.env.lower() == "production":
    try:
        settings.validate_config()
    except RuntimeError as exc:
        logger.error(f"Production config validation failed: {exc}. Booting in resilient mode.")
        # sys.exit(1) রিমুভ করা হলো (Cloud Run Resilient Boot)


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
    except Exception as exc:  # noqa: BLE001
        logger.error(f"Server failed to start: {exc}")
        sys.exit(1)


if __name__ == "__main__":
    run_server()
