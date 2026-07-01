import os
os.environ["SUPREMEAI_ENCRYPTION_KEY"] = "Y2hvc2VuX3JhbmRvbV9iYXNlNjRfa2V5XzMyX2J5dGVz="
import sys


ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if ROOT not in sys.path:
    sys.path.insert(0, ROOT)
os.environ.setdefault("OPENROUTER_API_KEY", "mock-key-value")

# বাংলা মন্তব্য: টেস্ট রান করার সময় রিয়াল ডাটাবেস এড়াতে এবং লক হওয়া রোধ করতে ইন-মেমোরি ডাটাবেস সেট করা হলো
os.environ["DATABASE_URL"] = "sqlite+aiosqlite:///:memory:"
os.environ["SUPABASE_DATABASE_URL"] = "sqlite+aiosqlite:///:memory:"
os.environ["SUPABASE_DATABASE_URL_POOLER"] = "sqlite+aiosqlite:///:memory:"


# Mock Google Auth credentials and services globally during tests
from unittest.mock import MagicMock


try:
    import google.auth

    google.auth.default = lambda *args, **kwargs: (MagicMock(), "mock-project-id")
except ImportError:
    sys.modules["google.auth"] = MagicMock()

try:
    import google.cloud.firestore

    google.cloud.firestore.Client = MagicMock
except ImportError:
    sys.modules["google.cloud.firestore"] = MagicMock()

try:
    import google.cloud.secretmanager

    google.cloud.secretmanager.SecretManagerServiceClient = MagicMock
except ImportError:
    sys.modules["google.cloud.secretmanager"] = MagicMock()

os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = "/dev/null"

import contextlib

import pytest

from core.rbac import RoleBasedAccessControl


_TEST_ENV_DEFAULTS = {
    "ENV": "test",
    "OPENROUTER_API_KEY": "",
    "HF_API_KEY": "",
    "GEMINI_API_KEY": "",
    "DEEPSEEK_API_KEY": "",
    "GROQ_API_KEY": "",
    "NVIDIA_API_KEY": "",
    "FIRECRAWL_API_KEY": "",
    "OLLAMA_URL": "http://127.0.0.1:11434",
    "SUPREMEAI_API_TOKEN": "",
    "SENTRY_DSN": "",
    "GCP_PROJECT_ID": "",
    "GCP_REGION": "",
    "EXPERIENCE_DB_PATH": f"data/test_experience_{os.getpid()}.db",
}


@pytest.fixture
def rbac():
    return RoleBasedAccessControl()


@pytest.fixture(autouse=True)
def isolate_env(monkeypatch: pytest.MonkeyPatch):
    for key, value in _TEST_ENV_DEFAULTS.items():
        monkeypatch.setenv(key, value)


@pytest.fixture(autouse=True, scope="session")
def bypass_jwt_auth():
    from unittest.mock import patch

    patches = []
    targets = [
        "backend.middleware.auth_middleware.verify_token",
        "middleware.auth_middleware.verify_token",
        "backend.core.security.verify_token",
        "core.security.verify_token",
    ]
    for target in targets:
        try:
            p = patch(target)
            mock = p.start()
            mock.return_value = {"sub": "test_admin@supremeai.com", "role": "admin"}
            patches.append(p)
        except Exception:
            pass
    yield
    for p in patches:
        with contextlib.suppress(Exception):
            p.stop()
