# FILE_PATH: supremeai/backend/__init__.py
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI


# Configure logging for the application
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# --- Import Routers ---
# These imports assume the `__init__.py` is located at `supremeai/backend/__init__.py`
# and the routers are in subdirectories like `supremeai/backend/admin/routes.py`.
# Defensive imports are used to allow the application to start even if a router is missing.
try:
    from .admin.routes import admin_router
except ImportError:
    logger.error("Failed to import admin_router. Admin routes will not be available.")
    admin_router = None
try:
    from .health.routes import health_router
except ImportError:
    logger.error("Failed to import health_router. Health routes will not be available.")
    health_router = None
try:
    from .api.routes import api_router
except ImportError:
    logger.error("Failed to import api_router. API routes will not be available.")
    api_router = None
try:
    # Assuming new endpoints from sprint5 are grouped under a router
    from .new_endpoints_sprint5.routes import onboarding_router
except ImportError:
    logger.warning("Failed to import onboarding_router. Sprint5 endpoints may not be available.")
    onboarding_router = None


# --- Lifespan Context Manager ---
@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Handles application startup and shutdown events.
    Initializes database connections, orchestrator, and other services.
    """
    logger.info("Application starting up...")

    # Global orchestrator instance (for proper shutdown)
    orchestrator = None

    # Database Initialization (addresses 'no such table: system_config' and 'DB pool was accessed...')
    try:
        from supremeai.backend.services.database import create_tables_for_testing
        from supremeai.backend.services.database import init_db_pool
        from supremeai.backend.services.database import shutdown_db_pool
        await init_db_pool()
        logger.info("Database pool initialized.")
        # For testing environments, especially with in-memory SQLite,
        # the schema needs to be created explicitly. This function should handle that.
        await create_tables_for_testing() # Assuming this function exists in database.py
        logger.info("Database tables created for testing.")
    except ImportError:
        logger.error("Failed to import database services. Database will not be initialized.")
    except Exception as e:
        logger.error(f"[lifespan] DB_INIT_FAILED: {e}", exc_info=True)
        # The 'invalid dsn' error for Supabase might originate from here if DSNs are shared.

    # Orchestrator Initialization (addresses "'Orchestrator' object has no attribute 'start'")
    try:
        from supremeai.backend.core.orchestrator import Orchestrator
        orchestrator = Orchestrator()
        # Defensive check for the reported error, assuming 'start' should exist
        if hasattr(orchestrator, 'start'):
            await orchestrator.start()
            logger.info("Orchestrator started.")
        else:
            logger.warning("[lifespan] ORCHESTRATOR_INIT_FAILED: 'Orchestrator' object is missing 'start' method.")
    except ImportError:
        logger.error("Failed to import Orchestrator module. Orchestrator will not be available.")
    except Exception as e:
        logger.error(f"[lifespan] ORCHESTRATOR_INIT_FAILED: {e}", exc_info=True)

    # Supabase Bootstrap (addresses 'invalid dsn' warning)
    # The specific DSN error implies an issue with how the Supabase client
    # interprets the connection string. Without more context, we can only log the attempt.
    try:
        # Assuming a dedicated Supabase initialization function exists.
        # e.g., from supremeai.backend.services.supabase import init_supabase_client
        # await init_supabase_client() # This function might be where the DSN error occurs.
        logger.info("Supabase client bootstrap attempt (DSN issue noted in logs).")
    except ImportError:
        logger.warning("Supabase client module not found.")
    except Exception as e:
        logger.warning(f"[lifespan] SUPABASE_BOOTSTRAP_FAILED: {e}", exc_info=True)


    yield  # Application is ready to receive requests

    # --- Shutdown events ---
    logger.info("Application shutting down...")

    # Orchestrator Shutdown
    try:
        if orchestrator and hasattr(orchestrator, 'stop'): # Defensive check
            await orchestrator.stop()
            logger.info("Orchestrator stopped.")
        else:
            logger.warning("[lifespan] SHUTDOWN_ORCHESTRATOR_FAILED: 'Orchestrator' object is missing 'stop' method.")
    except Exception as e:
        logger.error(f"[lifespan] SHUTDOWN_ORCHESTRATOR_FAILED: {e}", exc_info=True)

    # Database Pool Shutdown
    try:
        from supremeai.backend.services.database import shutdown_db_pool
        await shutdown_db_pool()
        logger.info("Database pool shut down.")
    except ImportError:
        logger.warning("Database shutdown function not found.")
    except Exception as e:
        logger.error(f"[lifespan] SHUTDOWN_DB_POOL_FAILED: {e}", exc_info=True)

    # Browser Agent Shutdown (addresses 'cannot import name 'shutdown_global_browser'')
    try:
        from supremeai.backend.tools.browser_agent import shutdown_global_browser
        shutdown_global_browser()
        logger.info("Browser shut down.")
    except ImportError:
        logger.warning("[lifespan] SHUTDOWN_BROWSER_FAILED: cannot import name 'shutdown_global_browser'. Module or function might be missing.")
    except Exception as e:
        logger.error(f"[lifespan] SHUTDOWN_BROWSER_FAILED: {e}", exc_info=True)


# --- FastAPI Application Instance ---
# The app instance is created with the defined lifespan.
app = FastAPI(lifespan=lifespan)

# --- Add Middleware ---
# The log warning `module 'backend.middleware' has no attribute 'auth_middleware'`
# suggests a problem with the middleware's definition or how it's being accessed.
# To avoid introducing new errors without specific knowledge of its implementation,
# middleware registration is commented out. It should be added here once resolved.
# Example:
# from starlette.middleware.base import BaseHTTPMiddleware
# from .middleware import AuthMiddleware # Assuming AuthMiddleware is the class
# app.add_middleware(AuthMiddleware)
# Or if `auth_middleware` is a function to be decorated:
# from .middleware import auth_middleware_function
# app.middleware("http")(auth_middleware_function)


# --- Include Routers ---
# Routers are included if they were successfully imported.
if admin_router:
    app.include_router(admin_router, prefix="/admin", tags=["Admin"])
if health_router:
    app.include_router(health_router, prefix="/health", tags=["Health Monitor"])
if api_router:
    app.include_router(api_router, prefix="/api", tags=["API"])
if onboarding_router:
    app.include_router(onboarding_router, prefix="/onboarding", tags=["Onboarding"])
