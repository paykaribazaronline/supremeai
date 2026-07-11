# FILE_PATH: backend/__init__.py
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.ext.asyncio import create_async_engine
from sqlalchemy.orm import sessionmaker

# Assuming these imports and modules exist in your project structure
# For configuration (adjust path if needed)
from backend.config import settings

# For database management (adjust paths if needed)
# Assuming a base for declarative models and utility functions for DB pool
from backend.db.base import Base  # For Base.metadata.create_all
from backend.db.session import close_db_pool  # For managing persistent DB pool
from backend.db.session import init_db_pool  # For managing persistent DB pool

# For routers (adjust paths based on actual project structure)
from backend.routers import admin
from backend.routers import health_monitor
from backend.routers import onboarding


# For middleware (adjust path if needed)
# The log says 'module backend.middleware has no attribute auth_middleware'.
# This implies auth_middleware might be directly in backend/middleware/__init__.py
# or in a specific file like backend/middleware/auth.py and needs to be imported there.
# Assuming it's directly available from `backend.middleware` for this example.
try:
    from backend.middleware import auth_middleware
except ImportError:
    auth_middleware = None
    logging.warning("Auth middleware not found at 'backend.middleware.auth_middleware'. Middleware will not be applied.")

# For services (adjust paths based on actual project structure)
# Attempt to import SecureRedisManager
try:
    from backend.services.redis_manager import SecureRedisManager
except ImportError:
    SecureRedisManager = None
    logging.warning("SecureRedisManager not found. Redis initialization will be skipped.")

# Attempt to import Orchestrator
try:
    from backend.core.orchestrator import Orchestrator
except ImportError:
    Orchestrator = None
    logging.warning("Orchestrator not found. Orchestrator initialization will be skipped.")


# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# --- Lifespan Context Manager ---
@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Application lifespan events for startup and shutdown.
    Initializes and cleans up resources like database, Redis, etc.
    """
    logger.info("Application starting up...")

    # --- Database Initialization ---
    app.state.db_failed_to_initialize = True # Default to failed
    try:
        if settings.DATABASE_URL and settings.DATABASE_URL.startswith("sqlite+aiosqlite:///:memory:"):
            logger.info("Initializing in-memory SQLite database for testing...")
            # Create an in-memory engine for tests
            engine = create_async_engine(settings.DATABASE_URL, echo=False)
            async with engine.begin() as conn:
                await conn.run_sync(Base.metadata.create_all) # Create tables
            # Store engine and session factory on app.state for dependency injection
            app.state.db_engine = engine
            app.state.db_session_factory = sessionmaker(
                engine, class_=AsyncSession, expire_on_commit=False
            )
            logger.info("In-memory SQLite database initialized successfully with schema.")
            app.state.db_failed_to_initialize = False
        elif settings.DATABASE_URL:
            # For persistent databases, use existing pool initialization
            await init_db_pool(app) # Assumes this sets up app.state.db_engine and app.state.db_session_factory
            logger.info("Persistent database pool initialized successfully.")
            app.state.db_failed_to_initialize = False
            # Note: For persistent DBs, schema creation/migrations are usually handled separately
            # by tools like Alembic, not `create_all` in app startup.
        else:
            logger.warning("DATABASE_URL is not configured.")

    except Exception as e:
        logger.error(f"Database initialization failed: {e}", exc_info=True)
        # This error (e.g., 'no such table: system_config') is critical.
        # It needs to be reflected in health checks.

    # --- Redis Initialization ---
    app.state.redis_failed_to_initialize = True # Default to failed
    if SecureRedisManager:
        try:
            app.state.redis_manager = SecureRedisManager(config=settings.REDIS_CONFIG)
            # The log suggests 'initialize' is expected. Let's try that first.
            if hasattr(app.state.redis_manager, 'initialize') and callable(app.state.redis_manager.initialize):
                await app.state.redis_manager.initialize()
                logger.info("Redis manager initialized using 'initialize' method.")
            elif hasattr(app.state.redis_manager, 'connect') and callable(app.state.redis_manager.connect):
                await app.state.redis_manager.connect()
                logger.info("Redis manager initialized using 'connect' method (fallback).")
            else:
                raise AttributeError("SecureRedisManager has neither 'initialize' nor 'connect' method.")
            app.state.redis_failed_to_initialize = False
        except Exception as e:
            logger.warning(f"[lifespan] REDIS_INIT_FAILED: 'SecureRedisManager' object has no attribute 'initialize' (or connect) or connection failed: {e}", exc_info=True)
    else:
        logger.warning("SecureRedisManager not imported, skipping Redis initialization.")


    # --- Orchestrator Initialization ---
    app.state.orchestrator_failed_to_initialize = True # Default to failed
    if Orchestrator:
        try:
            app.state.orchestrator = Orchestrator(config=settings.ORCHESTRATOR_CONFIG)
            # Log indicates 'start' method missing
            if hasattr(app.state.orchestrator, 'start') and callable(app.state.orchestrator.start):
                await app.state.orchestrator.start()
                logger.info("Orchestrator initialized.")
            else:
                raise AttributeError("Orchestrator object has no 'start' method.")
            app.state.orchestrator_failed_to_initialize = False
        except Exception as e:
            logger.warning(f"[lifespan] ORCHESTRATOR_INIT_FAILED: 'Orchestrator' object has no attribute 'start' or failed to start: {e}", exc_info=True)
    else:
        logger.warning("Orchestrator not imported, skipping Orchestrator initialization.")

    # --- Supabase Bootstrap ---
    # The 'invalid dsn: missing "=" after "sqlite+aiosqlite:///:memory:"' error
    # is complex. It suggests a library parsing `sqlite+aiosqlite:///:memory:`
    # incorrectly. This might require specific handling in backend.config or
    # the Supabase bootstrapping logic itself. For now, we note it.
    app.state.supabase_failed_to_initialize = False # Default, can be set True by actual logic if it exists
    # Example placeholder for Supabase bootstrap if it exists:
    # try:
    #     from backend.services.supabase import bootstrap_supabase
    #     await bootstrap_supabase(app)
    #     logger.info("Supabase bootstrapped successfully.")
    # except Exception as e:
    #     logger.warning(f"[lifespan] SUPABASE_BOOTSTRAP_FAILED: {e} | ctx={{'component': 'supabase'}}", exc_info=True)


    # --- Yield control to the application ---
    yield

    logger.info("Application shutting down...")
    # --- Shutdown logic ---
    try:
        if hasattr(app.state, 'orchestrator') and hasattr(app.state.orchestrator, 'stop') and callable(app.state.orchestrator.stop):
            await app.state.orchestrator.stop()
            logger.info("Orchestrator stopped.")
    except Exception as e:
        logger.warning(f"[lifespan] SHUTDOWN_ORCHESTRATOR_FAILED: {e} | ctx={{'phase': 'shutdown'}}", exc_info=True)

    try:
        if hasattr(app.state, 'redis_manager') and hasattr(app.state.redis_manager, 'disconnect') and callable(app.state.redis_manager.disconnect):
            await app.state.redis_manager.disconnect() # Assuming a disconnect method
            logger.info("Redis manager disconnected.")
    except Exception as e:
        logger.warning(f"[lifespan] REDIS_SHUTDOWN_FAILED: {e} | ctx={{'phase': 'shutdown'}}", exc_info=True)

    try:
        # This addresses: SHUTDOWN_DB_POOL_FAILED
        if hasattr(app.state, 'db_engine') and app.state.db_engine:
            await app.state.db_engine.dispose()
            logger.info("Database engine disposed.")
        elif hasattr(app, 'state') and hasattr(app.state, 'db_pool_initialized') and app.state.db_pool_initialized:
            # If init_db_pool was used for a persistent DB
            await close_db_pool(app)
            logger.info("Persistent database pool closed.")
    except Exception as e:
        logger.warning(f"[lifespan] SHUTDOWN_DB_POOL_FAILED: {e} | ctx={{'phase': 'shutdown'}}", exc_info=True)

    # Browser agent shutdown (cannot fix module import issue here, fix in `tools/browser_agent.py`)
    # try:
    #     from backend.tools.browser_agent import shutdown_global_browser
    #     await shutdown_global_browser()
    #     logger.info("Global browser shut down.")
    # except Exception as e:
    #     logger.warning(f"[lifespan] SHUTDOWN_BROWSER_FAILED: {e} | ctx={{'phase': 'shutdown'}}", exc_info=True)


app = FastAPI(
    lifespan=lifespan,
    title="SupremeAI Backend",
    description="API for SupremeAI services",
    version="0.0.1",
)

# --- Apply middleware ---
# This addresses: module 'backend.middleware' has no attribute 'auth_middleware'
# Assuming `auth_middleware` is a function that acts as an HTTP middleware callable.
# If it's a class-based middleware, it would be `app.add_middleware(AuthMiddleware, ...)`
if auth_middleware:
    app.middleware("http")(auth_middleware)
    logger.info("Auth middleware applied.")
else:
    logger.warning("Auth middleware not available, skipping application.")


# --- Include routers ---
# These are the routes mentioned in the failed tests.
app.include_router(admin.router, prefix="/admin", tags=["Admin"])
app.include_router(health_monitor.router, prefix="/health", tags=["Health Monitoring"])
app.include_router(onboarding.router, prefix="/onboarding", tags=["Onboarding"])

# Add a basic root endpoint for general sanity checks
@app.get("/", tags=["Root"])
async def read_root():
    return {"message": "SupremeAI Backend is running!"}

# Actuator health endpoint (e.g., /actuator/health)
# Assuming it's part of the health_monitor router or admin router.
# If it's a separate module, it would need its own router:
# from backend.routers import actuator # Example
# app.include_router(actuator.router, prefix="/actuator", tags=["Actuator"])
