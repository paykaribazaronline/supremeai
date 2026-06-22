from fastapi import FastAPI, Body, Depends, HTTPException, status
from fastapi.security import HTTPBasic, HTTPBasicCredentials
from fastapi.middleware.cors import CORSMiddleware

from core.config import settings
security = HTTPBasic()
settings.validate_config()

from admin.god import AdminGodLayer
from brain.model_router import ModelRouter
from core.intent import IntentClassifier
from api.routes import (
    task_router,
    simulator_router,
    browser_router,
    stream_router,
    agent_router,
    media_router,
    knowledge_router,
    marketplace_router,
    metrics_router,
    auth_router,
    async_task_router,
    cdc_router,
    codeflow_router,
    feedback_router,
    memory_router,
    admin_dashboard_router,
    email_router,
    github_router,
    internal_router,
    marketplace_endpoints_router,
    onboarding_router,
    repos_router,
    tools_ops_router,
    tools_registry_router,
    preferences_router,
    usage_metrics_router,
    payments_router,
    sso_router,
    agents_router,
    markdown_router,
)
from core.auth_middleware import AuthMiddleware
from core.observability_middleware import ObservabilityMiddleware
from core.rate_limiter import RateLimitMiddleware
from core.idempotency_middleware import IdempotencyMiddleware
from core.honeypot_middleware import HoneypotMiddleware
from core.telemetry import setup_tracing
from core.upstash_redis_queue import UpstashRedisQueue
from loguru import logger
import sentry_sdk
import secrets

setup_tracing()

if settings.sentry_dsn:
    try:
        sentry_sdk.init(
            dsn=settings.sentry_dsn,
            traces_sample_rate=1.0,
            environment=settings.env,
        )
    except Exception:
        pass


def _docs_auth(credentials: HTTPBasicCredentials = Depends(security)):
    correct = secrets.compare_digest(credentials.username, settings.docs_username) and \
              secrets.compare_digest(credentials.password, settings.docs_password)
    if not correct:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials",
            headers={"WWW-Authenticate": "Basic"},
        )
    return credentials.username


def _maybe_docs_auth():
    if settings.docs_auth_enabled and not settings.debug:
        return [_docs_auth]
    return []


docs_auth_dep = _maybe_docs_auth()

is_prod = settings.env.lower() == "production"
docs_enabled = settings.debug or not is_prod or settings.docs_auth_enabled

tags_metadata = [
    {"name": "admin", "description": "God-mode admin operations."},
    {"name": "agent", "description": "Autonomous agents execution and planning."},
    {"name": "marketplace", "description": "Discover and manage AI skills and tools."},
    {"name": "tools", "description": "Registry and management of integrated tools."},
]

app = FastAPI(
    title=f"{settings.app_name} (Production Ready)",
    description="Multi-cloud AI orchestration platform with zero-cost edge computing.",
    version="2.0.0",
    openapi_tags=tags_metadata,
    debug=settings.debug,
    docs_url="/docs" if docs_enabled else None,
    redoc_url="/redoc" if docs_enabled else None,
    openapi_url="/openapi.json" if docs_enabled else None,
    dependencies=docs_auth_dep,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=False,
    allow_methods=["GET", "POST", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "X-Request-ID"],
)

app.add_middleware(HoneypotMiddleware)
app.add_middleware(RateLimitMiddleware, requests_per_minute=120, burst=20)
app.add_middleware(IdempotencyMiddleware)
app.add_middleware(AuthMiddleware)
app.add_middleware(ObservabilityMiddleware)

from brain.parallel_cloud_router import ParallelCloudRouter
from brain.gcp_router import GCPCloudRunRouter
from core.gcp_firestore import GCPFirestoreVerificationQueue
from core.gcp_pubsub_queue import GCPPubSubQueue
from tools.gcp_cloud_functions import GCPCloudFunctionClient

model_router = ModelRouter()
intent_clf = IntentClassifier()
admin_god = AdminGodLayer(settings.admin_rules_db)
parallel_router = ParallelCloudRouter()
gcp_router = GCPCloudRunRouter()
verification_queue = GCPFirestoreVerificationQueue()
gcp_pubsub_queue = GCPPubSubQueue()
cloud_function_client = GCPCloudFunctionClient()
redis_queue = UpstashRedisQueue()

from adaptive_engine.registry import PlatformRegistry
from adaptive_engine.intent_parser import IntentParser
from adaptive_engine.experience_db import ExperienceDatabase
from adaptive_engine.platform_learner import PlatformLearner

platform_registry = PlatformRegistry()
experience_db = ExperienceDatabase()
intent_parser = IntentParser(model_router)
platform_learner = PlatformLearner(model_router, platform_registry)



from contextlib import asynccontextmanager
import httpx

global_http_client: httpx.AsyncClient = None

@asynccontextmanager
async def app_lifespan(app: FastAPI):
    global global_http_client
    limits = httpx.Limits(max_keepalive_connections=50, max_connections=100)
    timeout = httpx.Timeout(10.0, connect=5.0, read=30.0)
    global_http_client = httpx.AsyncClient(limits=limits, timeout=timeout)
    app.state.http_client = global_http_client
    model_router._http_client = global_http_client
    try:
        from core.pgbouncer_pool import get_db_pool
        await get_db_pool()
        logger.info("PgBouncer connection pool initialized on startup")
    except Exception as e:
        logger.warning(f"PgBouncer pool initialization deferred: {e}")
    yield
    if global_http_client:
        try:
            await global_http_client.aclose()
        except Exception as exc:
            logger.warning(f"Shared HTTP client close failed: {exc}")
    try:
        await model_router._http_client.aclose()
    except Exception as exc:
        logger.warning(f"HTTP client close failed: {exc}")

app.router.lifespan_context = app_lifespan


import time
import hmac
import hashlib
import struct
import base64
import os

@app.post("/api/admin/login")
def admin_login(payload: dict = Body(...)):
    password = payload.get("password")
    expected_password = settings.docs_password
    if not expected_password:
        raise HTTPException(status_code=500, detail="Admin password not configured on server")
    if password != expected_password:
        raise HTTPException(status_code=401, detail="Invalid password")
    
    totp_secret = os.getenv("SUPREMEAI_ADMIN_TOTP_SECRET")
    if not totp_secret:
        raise HTTPException(status_code=500, detail="TOTP secret not configured on server")
    return {"status": "otp_required", "message": "Google Authenticator code required."}

@app.post("/api/admin/verify")
def admin_verify(payload: dict = Body(...)):
    password = payload.get("password")
    otp = payload.get("otp")
    
    expected_password = settings.docs_password
    if not expected_password:
        raise HTTPException(status_code=500, detail="Admin password not configured on server")
    if password != expected_password:
        raise HTTPException(status_code=401, detail="Invalid password")
    
    totp_secret = os.getenv("SUPREMEAI_ADMIN_TOTP_SECRET")
    if not totp_secret:
        raise HTTPException(status_code=500, detail="TOTP secret not configured on server")
    
    def verify_totp_code(user_otp: str, base32_secret: str) -> bool:
        try:
            missing_padding = len(base32_secret) % 8
            if missing_padding:
                base32_secret += '=' * (8 - missing_padding)
            key = base64.b32decode(base32_secret.upper())
            
            # Allow current, previous (-30s), and next (+30s) windows to handle clock drift
            current_time = int(time.time() // 30)
            for drift in [-1, 0, 1]:
                msg = struct.pack(">Q", current_time + drift)
                h = hmac.new(key, msg, hashlib.sha1).digest()
                o = h[19] & 15
                h_num = struct.unpack(">I", h[o:o+4])[0] & 0x7fffffff
                code = f"{h_num % 1000000:06d}"
                if code == user_otp:
                    return True
            return False
        except Exception:
            return False

    if not otp or not verify_totp_code(otp.strip(), totp_secret):
        raise HTTPException(status_code=401, detail="Invalid Google Authenticator code")
        
    # Issue backend session JWT for secure session management
    from jose import jwt
    jwt_payload = {
        "uid": "admin",
        "role": "admin",
        "exp": int(time.time()) + 3600 * 24
    }
    jwt_secret = settings.jwt_secret
    token = jwt.encode(jwt_payload, jwt_secret, algorithm="HS256")
    return {"status": "success", "token": token}


# --- Agentic Security: Firebase Authentication & Unique TOTP MFA ---
# Added by Agent Antigravity on 2026-06-21 to enable personalized admin role verification and unique TOTP secrets.

def get_firestore_client():
    # Helper function to dynamically initialize Firestore client based on project ID
    project_id = os.getenv("GCP_PROJECT_ID") or "supremeai-a"
    try:
        from google.cloud import firestore
        return firestore.Client(project=project_id)
    except Exception as e:
        logger.warning(f"Failed to initialize Firestore client: {e}")
        return None

auth = None
# Initialize Firebase Admin SDK — tries multiple credential sources
try:
    import firebase_admin
    from firebase_admin import auth as firebase_auth, credentials as fb_credentials
    if not firebase_admin._apps:
        # 1. GOOGLE_APPLICATION_CREDENTIALS env var (path to service account JSON)
        _gac = os.getenv("GOOGLE_APPLICATION_CREDENTIALS", "")
        # 2. FIREBASE_SERVICE_ACCOUNT_JSON env var (inline JSON string)
        _sa_json = os.getenv("FIREBASE_SERVICE_ACCOUNT_JSON", "")
        # 3. FIREBASE_SERVICE_ACCOUNT_PATH env var (path to JSON file, defaults to service-account.json)
        _sa_path = os.getenv("FIREBASE_SERVICE_ACCOUNT_PATH") or "service-account.json"
        
        if _sa_json:
            import json as _json
            _cred = fb_credentials.Certificate(_json.loads(_sa_json))
            firebase_admin.initialize_app(_cred)
            logger.info("Firebase Admin initialized from FIREBASE_SERVICE_ACCOUNT_JSON")
        elif _sa_path:
            # Try multiple locations to find the service account file robustly
            _resolved_path = None
            for p in [_sa_path, os.path.join("backend", _sa_path), os.path.join("..", _sa_path)]:
                # Strip backend/ prefix if we're already inside backend folder to prevent duplicate pathing
                clean_p = p.replace("backend/backend/", "backend/")
                if not os.path.exists(clean_p) and clean_p.startswith("backend/"):
                    clean_p = clean_p[8:]
                if os.path.exists(clean_p):
                    _resolved_path = clean_p
                    break
            
            if _resolved_path:
                _cred = fb_credentials.Certificate(_resolved_path)
                firebase_admin.initialize_app(_cred)
                logger.info(f"Firebase Admin initialized from file: {_resolved_path}")
            elif _sa_path != "service-account.json":
                # Only raise error if they explicitly configured a custom path that wasn't found
                logger.warning(f"Firebase service account file not found at {_sa_path}")
                raise RuntimeError(f"Service account file not found: {_sa_path}")
            elif _gac and os.path.exists(_gac):
                # Fallback to default credentials if service-account.json was not found
                firebase_admin.initialize_app()
                logger.info("Firebase Admin initialized via GOOGLE_APPLICATION_CREDENTIALS")
            else:
                logger.warning("Firebase Admin SDK: No credentials found.")
                raise RuntimeError("No Firebase credentials configured")
        elif _gac and os.path.exists(_gac):
            # GOOGLE_APPLICATION_CREDENTIALS is set — SDK picks it up automatically
            firebase_admin.initialize_app()
            logger.info("Firebase Admin initialized via GOOGLE_APPLICATION_CREDENTIALS")
        else:
            # No credentials found — Firebase verification will be unavailable
            logger.warning(
                "Firebase Admin SDK: No credentials found. "
                "Set FIREBASE_SERVICE_ACCOUNT_JSON or FIREBASE_SERVICE_ACCOUNT_PATH in .env"
            )
            raise RuntimeError("No Firebase credentials configured")
    auth = firebase_auth
    logger.info("Firebase Admin SDK ready ✅")
except Exception as e:
    logger.warning(f"Firebase Admin SDK not available: {e}")
    auth = None

@app.post("/api/admin/firebase-login")
def admin_firebase_login(payload: dict = Body(...)):
    id_token = payload.get("id_token")
    if not id_token:
        raise HTTPException(status_code=400, detail="Missing Firebase ID token")

    # ── Case 1: Firebase Admin SDK available — verify real token ─────────────
    if auth is not None:
        try:
            decoded_token = auth.verify_id_token(id_token)
            uid = decoded_token['uid']
            email = decoded_token.get('email', '')
        except Exception as e:
            logger.exception("Firebase token verification failed")
            raise HTTPException(status_code=401, detail=f"Firebase verification failed: {str(e)}")

        db = get_firestore_client()
        role = "user"
        totp_secret = None

        if db:
            try:
                doc_ref = db.collection("admin_users").document(uid)
                doc = doc_ref.get()
                if doc.exists:
                    data = doc.to_dict()
                    role = data.get("role", "user")
                    totp_secret = data.get("totp_secret")
                else:
                    if "admin" in email.lower() or email.endswith("@supremeai.dev"):
                        role = "admin"
                        doc_ref.set({"email": email, "role": "admin", "created_at": str(time.time())})
            except Exception as e:
                logger.error(f"Firestore admin lookup failed: {e}")
                role = "user"
        else:
            role = "user"

        if role != "admin":
            raise HTTPException(status_code=403, detail="Forbidden: Not authorized as an admin role user")

        if not totp_secret:
            return {"status": "totp_setup_required", "uid": uid, "email": email}

        return {"status": "totp_required", "uid": uid}

    # Firebase Admin SDK not configured — reject login
    logger.error("Firebase Admin SDK not initialized — admin login rejected")
    raise HTTPException(
        status_code=503,
        detail="Admin authentication service unavailable. Firebase credentials not configured on server."
    )

@app.post("/api/admin/firebase-totp-setup")
def admin_firebase_totp_setup(payload: dict = Body(...)):
    if not auth:
        raise HTTPException(status_code=500, detail="Firebase Admin SDK is not initialized")
    id_token = payload.get("id_token")
    if not id_token:
        raise HTTPException(status_code=400, detail="Missing Firebase ID token")
        
    try:
        decoded_token = auth.verify_id_token(id_token)
        uid = decoded_token['uid']
        email = decoded_token.get('email', '')
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Firebase verification failed: {str(e)}")
        
    # Generate unique 16-char base32 secret key using base64/base32 encoding
    secret = base64.b32encode(os.urandom(10)).decode('utf-8')
    
    db = get_firestore_client()
    if db:
        try:
            db.collection("admin_users").document(uid).update({
                "temp_totp_secret": secret
            })
        except Exception as e:
            logger.error(f"Failed to store temp TOTP secret in Firestore: {e}")
        
    provisioning_uri = f"otpauth://totp/SupremeAI:{email}?secret={secret}&issuer=SupremeAI"
    return {"secret": secret, "provisioning_uri": provisioning_uri}

@app.post("/api/admin/firebase-totp-verify")
def admin_firebase_totp_verify(payload: dict = Body(...)):
    if not auth:
        raise HTTPException(status_code=500, detail="Firebase Admin SDK is not initialized")
    id_token = payload.get("id_token")
    otp = payload.get("otp")
    if not id_token or not otp:
        raise HTTPException(status_code=400, detail="Missing credentials")
        
    try:
        decoded_token = auth.verify_id_token(id_token)
        uid = decoded_token['uid']
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Firebase verification failed: {str(e)}")
        
    db = get_firestore_client()
    totp_secret = None
    temp_totp_secret = None
    
    if db:
        try:
            doc = db.collection("admin_users").document(uid).get()
            if doc.exists:
                data = doc.to_dict()
                totp_secret = data.get("totp_secret")
                temp_totp_secret = data.get("temp_totp_secret")
        except Exception as e:
            logger.error(f"Failed to retrieve TOTP secret: {e}")
            
    # Verify OTP against our custom RFC 6238 implementation
    secret_to_use = totp_secret or temp_totp_secret
    if not secret_to_use:
        # Fallback to shared dev secret if none exists in Firestore
        secret_to_use = os.getenv("SUPREMEAI_ADMIN_TOTP_SECRET")
        if not secret_to_use:
            raise HTTPException(status_code=500, detail="TOTP secret not configured on server")
        
    def check_totp(user_otp: str, base32_secret: str) -> bool:
        try:
            missing_padding = len(base32_secret) % 8
            if missing_padding:
                base32_secret += '=' * (8 - missing_padding)
            key = base64.b32decode(base32_secret.upper())
            current_time = int(time.time() // 30)
            for drift in [-1, 0, 1]:
                msg = struct.pack(">Q", current_time + drift)
                h = hmac.new(key, msg, hashlib.sha1).digest()
                o = h[19] & 15
                h_num = struct.unpack(">I", h[o:o+4])[0] & 0x7fffffff
                code = f"{h_num % 1000000:06d}"
                if code == user_otp:
                    return True
            return False
        except Exception:
            return False

    if not check_totp(otp.strip(), secret_to_use):
        raise HTTPException(status_code=401, detail="Invalid verification code")
        
    # Promote temporary secret on successful verification
    if temp_totp_secret and not totp_secret:
        if db:
            try:
                from google.cloud import firestore
                db.collection("admin_users").document(uid).update({
                    "totp_secret": temp_totp_secret,
                    "temp_totp_secret": firestore.DELETE_FIELD
                })
            except Exception as e:
                logger.error(f"Failed to promote temp TOTP secret: {e}")
            
    # Issue backend session JWT
    from jose import jwt
    jwt_payload = {
        "uid": uid,
        "role": "admin",
        "exp": int(time.time()) + 3600 * 24
    }
    jwt_secret = settings.jwt_secret
    token = jwt.encode(jwt_payload, jwt_secret, algorithm="HS256")
    
    return {"status": "success", "token": token}



@app.get("/health")
async def health():
    redis_ok = False
    if redis_queue.configured:
        try:
            redis_queue.set("health", "ok", ex=5)
            redis_ok = redis_queue.get("health") == "ok"
        except Exception:
            redis_ok = False
    else:
        redis_ok = True
    api_keys_ok = bool(
        settings.openrouter_api_key
        or settings.gemini_api_key
        or settings.deepseek_api_key
        or settings.groq_api_key
        or settings.nvidia_api_key
    )
    checks = {
        "redis": redis_ok,
        "api_keys_configured": api_keys_ok,
    }
    all_ok = all(checks.values())
    return {
        "status": "ok" if all_ok else "degraded",
        "orchestrator": "online",
        "checks": checks
    }


@app.get("/actuator/health")
def actuator_health():
    return {
        "status": "UP",
        "orchestrator": "online",
    }


@app.get("/admin/cloud-distribution")
def cloud_distribution():
    return {
        "distribution": parallel_router.get_distribution_stats(),
        "total_requests": sum(p["current_requests"] for p in parallel_router.PROVIDERS.values()),
        "active_providers": sum(1 for p in parallel_router.PROVIDERS.values() if p["status"] == "active"),
        "strategy": "parallel_active_active",
        "rebalance_interval": "1 hour"
    }


# [Antigravity 2026-06-22] Free-tier usage monitoring endpoints
@app.get("/admin/free-tier-status")
def free_tier_status():
    """
    Returns real-time free-tier usage for all AI providers.
    Shows RPM/TPM/RPD used vs limits and remaining budget.
    """
    from core.free_tier_tracker import get_tracker
    tracker = get_tracker()
    return tracker.get_status()


@app.get("/admin/free-tier-status/{provider}")
def free_tier_provider_status(provider: str):
    """Returns free-tier status for a specific provider."""
    from core.free_tier_tracker import get_tracker
    tracker = get_tracker()
    status = tracker.get_provider_status(provider)
    if status is None:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail=f"Provider '{provider}' not tracked")
    return status


@app.post("/admin/free-tier-pause/{provider}")
def free_tier_pause_provider(provider: str, payload: dict = Body(default={"seconds": 60})):
    """Manually pause a provider for N seconds (useful after hitting external rate limits)."""
    from core.free_tier_tracker import get_tracker
    seconds = float(payload.get("seconds", 60))
    tracker = get_tracker()
    tracker.mark_rate_limited(provider, pause_seconds=seconds)
    return {"status": "paused", "provider": provider, "seconds": seconds}


@app.post("/admin/free-tier-override/{provider}")
def free_tier_override_limits(provider: str, payload: dict = Body(...)):
    """
    Override free-tier limits for a provider at runtime.
    Payload example: {"rpm": 50, "rpd": 1000}
    Useful after upgrading an OpenRouter account ($10 spend → 1000 RPD).
    """
    from core.free_tier_tracker import get_tracker
    tracker = get_tracker()
    tracker.override_limits(provider, payload)
    return {"status": "updated", "provider": provider, "new_limits": payload}


@app.get("/admin/token-budget-stats")
def token_budget_stats():
    """Returns token usage and compression stats per provider."""
    from core.token_budget import get_budget_manager
    manager = get_budget_manager()
    return manager.get_stats()


@app.get("/gcp/health")
def gcp_health():
    return {
        "status": "ok",
        "cloud_run": gcp_router.health_check(timeout=3),
        "firestore_mode": verification_queue.provider,
        "pubsub_mode": gcp_pubsub_queue.provider,
        "cloud_functions": cloud_function_client.get_config(),
    }


@app.get("/gcp/verification-queue/stats")
def gcp_verification_queue_stats():
    return verification_queue.stats()


@app.get("/gcp/pubsub/stats")
def gcp_pubsub_stats():
    return gcp_pubsub_queue.stats()


app.include_router(memory_router)
app.include_router(task_router)
if markdown_router is not None:
    app.include_router(markdown_router, prefix="/api/v1")
app.include_router(simulator_router)
app.include_router(browser_router)
app.include_router(stream_router)
app.include_router(agent_router)
app.include_router(async_task_router)
app.include_router(cdc_router)
app.include_router(media_router)
app.include_router(knowledge_router)
app.include_router(marketplace_router)
app.include_router(metrics_router)
app.include_router(auth_router)
if admin_dashboard_router is not None:
    app.include_router(admin_dashboard_router)
app.include_router(email_router)
app.include_router(github_router)
app.include_router(internal_router)
app.include_router(marketplace_endpoints_router)
if onboarding_router is not None:
    app.include_router(onboarding_router)
app.include_router(repos_router)
app.include_router(tools_ops_router)
if agents_router is not None:
    app.include_router(agents_router)
app.include_router(tools_registry_router)
app.include_router(preferences_router)
app.include_router(usage_metrics_router)
if payments_router is not None:
    app.include_router(payments_router)
if sso_router is not None:
    app.include_router(sso_router)
from tools.image_to_code import router as image_to_code_router
if image_to_code_router is not None:
    app.include_router(image_to_code_router)

# New tool routers (Sprint C fixes)
try:
    from tools.browser_agent import router as browser_agent_router
    app.include_router(browser_agent_router, prefix="/api")
except Exception as _e:
    logger.warning(f"browser_agent router not loaded: {_e}")

try:
    from tools.voice_coder import router as voice_coder_router
    app.include_router(voice_coder_router, prefix="/api")
except Exception as _e:
    logger.warning(f"voice_coder router not loaded: {_e}")

try:
    from tools.style_learner import router as style_learner_router
    app.include_router(style_learner_router, prefix="/api")
except Exception as _e:
    logger.warning(f"style_learner router not loaded: {_e}")

try:
    from tools.diagram_to_architecture import router as diagram_router
    app.include_router(diagram_router, prefix="/api")
except Exception as _e:
    logger.warning(f"diagram_to_architecture router not loaded: {_e}")

try:
    from tools.ai_pair_programmer import router as pair_router
    app.include_router(pair_router, prefix="/api")
except Exception as _e:
    logger.warning(f"ai_pair_programmer router not loaded: {_e}")

try:
    from api.routes.onboarding import router as onboarding_api_router
    app.include_router(onboarding_api_router, prefix="/api")
except Exception as _e:
    logger.warning(f"onboarding API router not loaded: {_e}")

if codeflow_router is not None:
    app.include_router(codeflow_router)
if feedback_router is not None:
    app.include_router(feedback_router)

# Sprint G routers
try:
    from tools.multilingual_tts import router as tts_router
    app.include_router(tts_router, prefix="/api")
except Exception as _e:
    logger.warning(f"multilingual_tts router not loaded: {_e}")

try:
    from tools.comment_thread_ai import router as comment_ai_router
    app.include_router(comment_ai_router, prefix="/api")
except Exception as _e:
    logger.warning(f"comment_thread_ai router not loaded: {_e}")

try:
    from tools.auto_test_generator import router as test_gen_router
    app.include_router(test_gen_router, prefix="/api")
except Exception as _e:
    logger.warning(f"auto_test_generator router not loaded: {_e}")

try:
    from api.routes.tenant_admin import router as tenant_admin_router
    app.include_router(tenant_admin_router, prefix="/api")
except Exception as _e:
    logger.warning(f"tenant_admin router not loaded: {_e}")

from core.universal_rules import UniversalRulesEngine
rules_engine = UniversalRulesEngine()

@app.get("/admin/rules")
def get_admin_rules():
    return rules_engine.rules

@app.post("/admin/rules")
def post_admin_rules(payload: dict = Body(...)):
    new_rules = payload.get("rules")
    if new_rules:
        success = rules_engine.save_rules(new_rules)
        if success:
            return {"status": "success"}
    return {"status": "error", "message": "Failed to save rules"}

@app.get("/skills")
def get_skills():
    return {
        "web_scraper": {
            "name": "web_scraper",
            "version": "1.0.0",
            "description": "Scrapes website contents using BeautifulSoup."
        },
        "csv_exporter": {
            "name": "csv_exporter",
            "version": "1.0.0",
            "description": "Exports tabular data to CSV using pandas."
        }
    }


