# FILE_PATH: main.py
import importlib.util
import os
import signal
import subprocess
import sys


required_packages = {
    "slowapi": "slowapi",
    "pinecone": "pinecone-client",  # Module 'pinecone' is provided by pypi package 'pinecone-client'
}
missing_packages_to_install = []

for module_name, pip_package_name in required_packages.items():
    if importlib.util.find_spec(module_name) is None:
        missing_packages_to_install.append(pip_package_name)

if missing_packages_to_install:
    print(f"Detected missing packages: {', '.join(missing_packages_to_install)}. Attempting dynamic installation...", file=sys.stderr)
    try:
        subprocess.check_call([sys.executable, "-m", "pip", "install", "--no-input", *missing_packages_to_install])
        print(f"Successfully installed missing packages: {', '.join(missing_packages_to_install)}", file=sys.stderr)
    except subprocess.CalledProcessError as e:
        print(f"Error: Failed to dynamically install packages: {e}", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"An unexpected error occurred during dynamic package installation: {e}", file=sys.stderr)
        sys.exit(1)

import uvicorn
from loguru import logger

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
