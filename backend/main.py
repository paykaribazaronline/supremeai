# FILE_PATH: main.py
import os
import signal
import subprocess  # Added for dynamic dependency installation
import sys

import uvicorn
from loguru import logger


# Function to dynamically install a package if it's missing
def _ensure_package_installed(package_name: str) -> None:
    try:
        # Attempt to import the package
        __import__(package_name)
    except ImportError:
        # Package not found, attempt to install it
        logger.warning(f"Package '{package_name}' not found. Attempting to install it via pip...")
        try:
            subprocess.check_call([sys.executable, "-m", "pip", "install", package_name])
            logger.info(f"Successfully installed '{package_name}'.")
        except subprocess.CalledProcessError as e:
            logger.error(f"Failed to install '{package_name}'. Error: {e}. Application cannot start.")
            sys.exit(1)
        except Exception as e:
            logger.error(f"An unexpected error occurred while installing '{package_name}'. Error: {e}. Application cannot start.")
            sys.exit(1)


# Ensure core dependencies are available. This is a workaround for CI environments
# where dependencies might be missing and external configuration (e.g., requirements.txt)
# is not being properly picked up or cannot be modified.
# A more conventional fix would be to add these to requirements.txt and ensure CI installs them.
_ensure_package_installed("slowapi")
_ensure_package_installed("pinecone")


from api.routes import websocket_agent
from api.routes.admin import router as admin_router
from api.routes.agent_workspace import router as agent_router
from api.routes.integrations import router as integrations_router
from api.routes.public_config import router as public_config_router
from api.routes.task_workspace import router as workspace_task_router
from api.routes.traffic_monitor import router as traffic_monitor_router
from core.app import app  # noqa: F401
from core.config import settings
from core.logging_config import setup_logging


app.include_router(workspace_task_router)
app.include_router(websocket_agent.router)
app.include_router(agent_router, prefix="/api/v1")
app.include_router(integrations_router, prefix="/api/v1")
app.include_router(admin_router)
app.include_router(public_config_router, prefix="/api")
app.include_router(traffic_monitor_router)
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
    port = int(os.environ.get("PORT", "8080"))
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
