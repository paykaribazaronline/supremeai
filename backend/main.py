# FILE_PATH: main.py
import os
import signal
import sys

import uvicorn
from loguru import logger

# Essential imports that are not reported as missing, keeping them outside the main try block
from core.config import settings
from core.logging_config import setup_logging


# Configure logging early, as it's needed for potential error handling.
setup_logging()

# Global variable to hold the FastAPI app instance
# Initialized to None and populated if imports are successful
app = None
app_initialized_successfully = False

try:
    # Attempt to import core.app and its related modules.
    # This is where the ModuleNotFoundError for 'slowapi' is indirectly encountered
    # when 'core.app' itself tries to import 'slowapi'.
    from api.routes import websocket_agent
    from api.routes.admin import router as admin_router
    from api.routes.agent_workspace import router as agent_router
    from api.routes.integrations import router as integrations_router
    from api.routes.public_config import router as public_config_router
    from api.routes.task_workspace import router as workspace_task_router
    from api.routes.traffic_monitor import router as traffic_monitor_router
    from core.app import app as imported_app  # noqa: F401

    # Assign the successfully imported app to the global 'app' variable
    app = imported_app

    # Application router inclusions
    app.include_router(workspace_task_router)
    app.include_router(websocket_agent.router)
    app.include_router(agent_router, prefix="/api/v1")
    app.include_router(integrations_router, prefix="/api/v1")
    app.include_router(admin_router)
    app.include_router(public_config_router, prefix="/api")
    app.include_router(traffic_monitor_router)

    if settings.env.lower() == "production":
        try:
            settings.validate_config()
        except RuntimeError as exc:
            logger.error(f"Production config validation failed: {exc}. Booting in resilient mode.")
            # sys.exit(1) was removed for Cloud Run Resilient Boot, respecting original change.

    # If we reached here, the application core was initialized successfully
    app_initialized_successfully = True

except ImportError as e:
    # Catch ModuleNotFoundError specifically to provide a more actionable message
    # for common missing dependencies that cause CI pipeline failures.
    if "slowapi" in str(e):
        logger.critical(f"FATAL: Missing critical dependency 'slowapi'. Please install it (e.g., pip install slowapi). Original error: {e}")
    elif "pinecone" in str(e):
        # This catch is included for completeness based on the error log,
        # even though main.py's direct import chain might not trigger it.
        logger.critical(f"FATAL: Missing critical dependency 'pinecone'. Please install it (e.g., pip install pinecone). Original error: {e}")
    else:
        logger.critical(f"FATAL: An unhandled ImportError occurred during application startup: {e}. Exiting.")
    sys.exit(1)


def _handle_sigterm(signum, frame):
    logger.info("Received shutdown signal. Performing graceful shutdown...")
    sys.exit(0)


signal.signal(signal.SIGTERM, _handle_sigterm)
signal.signal(signal.SIGINT, _handle_sigterm)


def run_server() -> None:
    # Only attempt to run the Uvicorn server if the app was successfully initialized
    if not app_initialized_successfully or app is None:
        logger.critical("Application was not initialized successfully due to missing dependencies. Cannot run server.")
        sys.exit(1)

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
        # Uvicorn will pick up the global 'app' variable defined in this module
        uvicorn.run("main:app", **uvicorn_kwargs)
    except Exception as exc:  # noqa: BLE001
        logger.critical(f"Server failed to start: {exc}") # Changed to critical for better visibility
        sys.exit(1)


if __name__ == "__main__":
    run_server()
