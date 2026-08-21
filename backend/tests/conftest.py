import os
import sys

from loguru import logger

# বাংলা মন্তব্য: pytest কালেকশনের সময় loguru-এর ডিফল্ট stderr হ্যান্ডলার যেন I/O error না দেয়, তাই প্রথমেই সেটি রিমুভ করা হলো।
logger.remove()

# বাংলা মন্তব্য: এই ব্লকটা একটা আগের কমিটে হারিয়ে গিয়েছিল, যার ফলে --import-mode=importlib
# ব্যবহার করার সময় `from core...` ইমপোর্ট সব টেস্ট ফাইলে ModuleNotFoundError দিচ্ছিল (৩২টা
# collection error)। backend/ রুট এবং রিপো-রুট + scripts/ ডিরেক্টরি sys.path-এ ফেরত যোগ করা হলো।
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if ROOT not in sys.path:
    sys.path.insert(0, ROOT)
REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SCRIPTS_DIR = os.path.join(REPO_ROOT, "scripts")
if REPO_ROOT not in sys.path:
    sys.path.append(REPO_ROOT)
if os.path.isdir(SCRIPTS_DIR) and SCRIPTS_DIR not in sys.path:
    sys.path.append(SCRIPTS_DIR)

# Mock external dependencies that are not installed
import importlib.machinery
from unittest.mock import MagicMock


def create_mock_module(name, is_package=False):
    m = MagicMock()
    m.__spec__ = importlib.machinery.ModuleSpec(name=name, loader=MagicMock(), is_package=is_package)
    if is_package:
        m.__path__ = []
    return m


import pytest


def pytest_configure(config):
    worker_id = os.environ.get("PYTEST_XDIST_WORKER")
    if worker_id is not None:
        db_url = os.environ.get("SUPABASE_DATABASE_URL", "")
        if "supreme_test_db" in db_url:
            os.environ["SUPABASE_DATABASE_URL"] = db_url.replace("supreme_test_db", f"supreme_test_db_{worker_id}")
            os.environ["SUPABASE_DATABASE_URL_POOLER"] = db_url.replace("supreme_test_db", f"supreme_test_db_{worker_id}")

        try:
            worker_idx = int(worker_id.replace("gw", ""))
        except ValueError:
            worker_idx = 1

        redis_url = os.environ.get("REDIS_URL", "redis://127.0.0.1:6379/0")
        if redis_url.endswith("/0"):
            os.environ["REDIS_URL"] = redis_url[:-1] + str(worker_idx)

@pytest.fixture
def valid_auth_headers():
    return {"Authorization": "Bearer mock_test_jwt_token", "Content-Type": "application/json"}


@pytest.fixture
async def async_session():
    from unittest.mock import AsyncMock

    yield AsyncMock()


@pytest.fixture(autouse=True)
def disable_honeypot(request, monkeypatch):
    if os.environ.get("ENABLE_HONEYPOT_TEST") == "true" or "test_honeypot_middleware" in getattr(
        request.node, "nodeid", ""
    ):
        yield
        return

    async def mock_bypass(self, scope, receive, send):
        await self.app(scope, receive, send)

    monkeypatch.setattr("core.security.honeypot_middleware.HoneypotMiddleware.__call__", mock_bypass)
    # বাংলা মন্তব্য: BUG FIX - `backend.core.*` prefix দিয়ে import হওয়া app-এর
    # HoneypotMiddleware একটা সম্পূর্ণ আলাদা ক্লাস অবজেক্ট (module identity duplication,
    # secret_vault-এ আগে পাওয়া একই সমস্যা) -- শুধু bare `core.security...` patch করলে
    # ওই ক্লাসটা অপরিবর্তিত থেকে যায় এবং real honeypot চালু থাকে, যেটা মাঝেমধ্যে
    # RulesMutator().block_ip() সত্যিই কল করে ফেলত এবং পরের টেস্টগুলো (যেমন
    # test_byoc_endpoints.py) কে corrupt করত। উভয় identity patch করা হলো।
    backend_module = sys.modules.get("backend.core.security.honeypot_middleware")
    if backend_module is not None and hasattr(backend_module, "HoneypotMiddleware"):
        monkeypatch.setattr(backend_module.HoneypotMiddleware, "__call__", mock_bypass)
    yield


@pytest.fixture(autouse=True)
def disable_chaos_injector(monkeypatch):
    monkeypatch.setenv("ENABLE_CHAOS_INJECTOR", "false")
    monkeypatch.setenv("CHAOS_MODE", "disabled")
    monkeypatch.setenv("CHAOS_ENGINE_ENABLED", "false")
    yield


# বাংলা মন্তব্য: BUG FIX - একাধিক টেস্ট ফাইল (test_admin.py, test_swarm_routes.py,
# test_api_new_endpoints.py) app.dependency_overrides সেট করে কিন্তু try/finally
# ছাড়া প্লেইন কোডে শেষে ক্লিয়ার করে। কোনো assertion/request মাঝপথে fail করলে
# ক্লিয়ার-করার লাইন কখনো রান হয় না, ফলে override একই xdist worker-এ পরের যেকোনো
# টেস্টে leak করে -- এটাই test_task_execute_* টেস্টগুলোর non-deterministic
# auth/admin bypass ফেইলিউরের আসল কারণ ছিল, শুধু worker-এ কোন ফাইল আগে/পরে
# চলছে তার উপর নির্ভর করে। প্রতিটা টেস্টের পর unconditionally ক্লিয়ার করে দিলে
# leak-এর সম্ভাবনাই থাকে না, কোন টেস্ট কী রেখে গেছে তা যাচাই করারও দরকার নেই।
@pytest.fixture(autouse=True)
def _reset_fastapi_dependency_overrides():
    yield
    try:
        from core.app import app

        app.dependency_overrides.clear()
    except Exception:
        # App import can fail in minimal/headless test environments (e.g. missing
        # optional native deps). Teardown only exists to clear overrides, so any
        # failure here must not mask the actual test result.
        pass


@pytest.fixture(autouse=True)
def _reset_secret_vault_cache():
    """বাংলা: টেস্ট আইসোলেশন নিশ্চিত করার জন্য Secret Vault এবং Settings ক্যাশ রিসেট"""
    try:
        from core.security.secret_vault import reset_secret_vault

        reset_secret_vault()
    except Exception:
        pass
    try:
        from core.config import settings

        if hasattr(settings, "_cached_secrets"):
            settings._cached_secrets.clear()
            settings._secrets_batch_loaded = False
    except Exception:
        pass
    yield
    try:
        from core.security.secret_vault import reset_secret_vault

        reset_secret_vault()
    except Exception:
        pass


# বাংলা মন্তব্য: BUG FIX - ALLOW_TEST_AUTH_BYPASS আগে fixture-এর ভেতরে সেট করা হতো,
# কিন্তু pydantic Settings() env var একবারই পড়ে module import-এর সময়ে, fixture রান
# হওয়ার অনেক আগে। ফলে settings.allow_test_auth_bypass সবসময় False থাকতো এবং
# AuthMiddleware সব রিকোয়েস্ট 401 দিয়ে ব্লক করতো (৩৪+ টেস্ট ফেইল)। মডিউল-লেভেলে সেট করা হলো।
os.environ["ALLOW_TEST_AUTH_BYPASS"] = "true"

# বাংলা মন্তব্য: টেস্টে Supabase নেটওয়ার্ক রিকোয়েস্ট আটকাতে module import এর আগেই
# SUPABASE_URL ও SUPABASE_KEY খালি করা হচ্ছে। SupabaseDB.__init__() এ শর্ত আছে:
# "if self.url and self.key: create_client()" — ফলে url বা key না থাকলে create_client কল হবে না।
os.environ["SUPABASE_URL"] = ""
os.environ["SUPABASE_KEY"] = ""

sys.modules["slowapi"] = create_mock_module("slowapi", is_package=True)
sys.modules["slowapi.util"] = create_mock_module("slowapi.util")
sys.modules["locust"] = create_mock_module("locust", is_package=True)


class RateLimitExceeded(Exception):
    pass


slowapi_errors_mock = create_mock_module("slowapi.errors")
slowapi_errors_mock.RateLimitExceeded = RateLimitExceeded
sys.modules["slowapi.errors"] = slowapi_errors_mock
sys.modules["pinecone"] = create_mock_module("pinecone", is_package=True)
sys.modules["chromadb"] = create_mock_module("chromadb", is_package=True)
sys.modules["chromadb.config"] = create_mock_module("chromadb.config")
sys.modules["chromadb.utils"] = create_mock_module("chromadb.utils", is_package=True)
sys.modules["chromadb.utils.embedding_functions"] = create_mock_module("chromadb.utils.embedding_functions")
sys.modules["cachetools"] = create_mock_module("cachetools", is_package=True)
sys.modules["nats"] = create_mock_module("nats", is_package=True)
sys.modules["nats.aio"] = create_mock_module("nats.aio", is_package=True)
sys.modules["nats.aio.client"] = create_mock_module("nats.aio.client")
sys.modules["nats.errors"] = create_mock_module("nats.errors")
sys.modules["docker"] = create_mock_module("docker", is_package=True)
sys.modules["docker.errors"] = create_mock_module("docker.errors")
sys.modules["typer"] = create_mock_module("typer", is_package=True)
sys.modules["rich"] = create_mock_module("rich", is_package=True)
sys.modules["rich.console"] = create_mock_module("rich.console")
sys.modules["rich.table"] = create_mock_module("rich.table")
sys.modules["rich.panel"] = create_mock_module("rich.panel")
sys.modules["rich.prompt"] = create_mock_module("rich.prompt")
sys.modules["tools.code.image_to_code_react"] = create_mock_module("tools.code.image_to_code_react")

# Mock external SDKs
sys.modules["analytics"] = create_mock_module("analytics")
sys.modules["sentry_sdk"] = create_mock_module("sentry_sdk")
sys.modules["sentry_sdk.integrations"] = create_mock_module("sentry_sdk.integrations", is_package=True)
sys.modules["sentry_sdk.integrations.loguru"] = create_mock_module("sentry_sdk.integrations.loguru")
sys.modules["supabase"] = create_mock_module("supabase", is_package=True)
sys.modules["supabase.client"] = create_mock_module("supabase.client")
sys.modules["alembic"] = create_mock_module("alembic", is_package=True)
sys.modules["alembic.config"] = create_mock_module("alembic.config")
sys.modules["alembic.migration"] = create_mock_module("alembic.migration", is_package=True)
sys.modules["alembic.operations"] = create_mock_module("alembic.operations")
sys.modules["alembic.runtime"] = create_mock_module("alembic.runtime", is_package=True)
sys.modules["alembic.runtime.migration"] = create_mock_module("alembic.runtime.migration")
sys.modules["redis"] = create_mock_module("redis", is_package=True)
sys.modules["redis.asyncio"] = create_mock_module("redis.asyncio", is_package=True)
sys.modules["redis.exceptions"] = create_mock_module("redis.exceptions")
sys.modules["stripe"] = create_mock_module("stripe", is_package=True)
sys.modules["stripe.error"] = create_mock_module("stripe.error")
sys.modules["resend"] = create_mock_module("resend", is_package=True)
sys.modules["resend.emails"] = create_mock_module("resend.emails")
sys.modules["websockets"] = create_mock_module("websockets", is_package=True)
sys.modules["litellm"] = create_mock_module("litellm", is_package=True)
sys.modules["opentelemetry"] = create_mock_module("opentelemetry", is_package=True)
sys.modules["opentelemetry.trace"] = create_mock_module("opentelemetry.trace", is_package=True)
sys.modules["opentelemetry.sdk"] = create_mock_module("opentelemetry.sdk", is_package=True)
sys.modules["opentelemetry.sdk.trace"] = create_mock_module("opentelemetry.sdk.trace", is_package=True)
sys.modules["opentelemetry.sdk.trace.export"] = create_mock_module("opentelemetry.sdk.trace.export")
sys.modules["opentelemetry.instrumentation"] = create_mock_module("opentelemetry.instrumentation", is_package=True)
sys.modules["opentelemetry.instrumentation.fastapi"] = create_mock_module("opentelemetry.instrumentation.fastapi")
sys.modules["opentelemetry.exporter"] = create_mock_module("opentelemetry.exporter", is_package=True)
sys.modules["opentelemetry.exporter.otlp"] = create_mock_module("opentelemetry.exporter.otlp", is_package=True)
sys.modules["opentelemetry.exporter.otlp.proto"] = create_mock_module(
    "opentelemetry.exporter.otlp.proto", is_package=True
)
sys.modules["opentelemetry.exporter.otlp.proto.grpc"] = create_mock_module("opentelemetry.exporter.otlp.proto.grpc")
sys.modules["asyncpg"] = create_mock_module("asyncpg", is_package=True)
sys.modules["asyncpg.connection"] = create_mock_module("asyncpg.connection")
sys.modules["tenacity"] = create_mock_module("tenacity", is_package=True)
sys.modules["posthog"] = create_mock_module("posthog", is_package=True)
sys.modules["pandas"] = create_mock_module("pandas", is_package=True)
sys.modules["neo4j"] = create_mock_module("neo4j", is_package=True)
