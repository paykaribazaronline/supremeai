import os
import signal
import sys

import uvicorn
from loguru import logger

from core.app import app  # noqa: F401
from core.config import settings
from core.logging_config import setup_logging


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

if __name__ == "__main__":
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
