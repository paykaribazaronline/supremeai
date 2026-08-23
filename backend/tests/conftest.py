"""
SupremeAI Test Configuration — Shared Fixtures & Utilities
v4.0: Comprehensive fixtures for unit, integration, and E2E tests
"""

from __future__ import annotations

import asyncio
import os
from typing import AsyncGenerator, Generator
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient

# Set test environment BEFORE importing app
os.environ.setdefault("ENV", "testing")
os.environ.setdefault("JWT_SECRET", "test-secret-key-for-testing-purpose-minimum-32")
os.environ.setdefault("DATABASE_URL", "sqlite+aiosqlite:///./test.db")

os.environ.setdefault("HF_API_KEY", "hf_test_mock_key")
os.environ.setdefault("DEEPSEEK_API_KEY", "mock_key")
os.environ.setdefault("GROQ_API_KEY", "mock_key")
os.environ.setdefault("GEMINI_API_KEY", "mock_key")
os.environ.setdefault("OPENROUTER_API_KEY", "mock_key")



# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

@pytest.fixture(scope="session")
def event_loop() -> Generator[asyncio.AbstractEventLoop, None, None]:
    """Create a session-scoped event loop."""
    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


@pytest_asyncio.fixture
async def client() -> AsyncGenerator[AsyncClient, None]:
    """Create an async HTTP client for FastAPI testing."""
    from main import app

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac


@pytest_asyncio.fixture
async def db_session() -> AsyncGenerator:
    """Create a fresh database session for each test."""
    from core.db import async_session_factory, Base
    from sqlalchemy.ext.asyncio import create_async_engine

    # Use in-memory SQLite for tests
    engine = create_async_engine("sqlite+aiosqlite:///:memory:")

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    async_session = async_sessionmaker(engine, expire_on_commit=False)

    async with async_session() as session:
        yield session

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)

    await engine.dispose()


@pytest.fixture
def mock_llm_provider() -> MagicMock:
    """Mock LLM provider for isolated unit tests."""
    mock = MagicMock()
    mock.generate.return_value = "Test response"
    mock.embed.return_value = [0.1] * 1536  # Mock embedding
    mock.health_check.return_value = True
    return mock


@pytest.fixture
def sample_user_data() -> dict:
    """Sample valid user data for tests."""
    return {
        "email": "test@example.com",
        "username": "testuser",
        "full_name": "Test User",
        "password": "SecurePass123!",
    }


@pytest.fixture
def admin_headers() -> dict[str, str]:
    """Headers with admin authentication token."""
    import jwt
    from datetime import datetime, timezone

    token = jwt.encode(
        {
            "sub": "admin-id",
            "role": "admin",
            "exp": datetime.now(tz=timezone.utc).timestamp() + 3600,
        },
        os.getenv("JWT_SECRET", "test-secret"),
        algorithm="HS256",
    )
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture
def user_headers() -> dict[str, str]:
    """Headers with regular user authentication token."""
    import jwt
    from datetime import datetime, timezone

    token = jwt.encode(
        {
            "sub": "user-id",
            "role": "user",
            "exp": datetime.now(tz=timezone.utc).timestamp() + 3600,
        },
        os.getenv("JWT_SECRET", "test-secret"),
        algorithm="HS256",
    )
    return {"Authorization": f"Bearer {token}"}


# ---------------------------------------------------------------------------
# Pytest Configuration
# ---------------------------------------------------------------------------

def pytest_configure(config):
    """Custom pytest markers."""
    config.addinivalue_line("markers", "slow: marks tests as slow (deselect with '-m \"not slow\"')")
    config.addinivalue_line("markers", "integration: marks tests as integration tests")
    config.addinivalue_line("markers", "e2e: marks tests as end-to-end tests")
    config.addinivalue_line("markers", "unit: marks tests as unit tests")


# -----------------------------------------------------------------------------
# FILE 9: tests/test_task_router.py — Cost Guard Tests (NEW)
# -----------------------------------------------------------------------------
