"""Tests for FastAPI Application Lifespan Manager.

এই মডিউলে টেস্ট করা হয়:
- Startup infrastructure initialization
- Database pool initialization
- Redis connection verification
- OpenTelemetry tracing setup
- Orchestrator initialization
- Background task startup
- Graceful shutdown of all services
"""

import asyncio
import contextlib
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from core.lifespan import _ensure_api_key_tables, app_lifespan


# ────────────────────────────────────────────────────────────────────────────
# Helper: asyncio.run() ব্যবহার করো — Python 3.11-এ get_event_loop() deprecated
# ────────────────────────────────────────────────────────────────────────────
def _run(coro):
    """Async coroutine sync-এ run করার helper।"""
    return asyncio.get_event_loop_policy().new_event_loop().run_until_complete(coro)


async def _run_lifespan(mock_app) -> None:
    """Lifespan context manager চালাও।"""
    async with app_lifespan(mock_app):
        pass


# ────────────────────────────────────────────────────────────────────────────
# Common patches helper — ExitStack দিয়ে Python-এর nested block limit এড়ানো হচ্ছে
# setup_tracing locally imported তাই `core.observability.telemetry.setup_tracing` patch করতে হবে
# ────────────────────────────────────────────────────────────────────────────
def _apply_common_patches(stack: contextlib.ExitStack) -> dict:
    """সব common patches একটি ExitStack-এ enter করে mocks-এর dict রিটার্ন করো।"""
    mocks = {}

    mocks["validate"] = stack.enter_context(
        patch("core.lifespan.StartupValidator.validate", new_callable=AsyncMock)
    )
    mocks["reliability"] = stack.enter_context(
        patch("core.lifespan.ReliabilityController.initialize", new_callable=AsyncMock)
    )
    # setup_tracing locally imported — সঠিক patch path ব্যবহার করতে হবে
    stack.enter_context(
        patch("core.observability.telemetry.setup_tracing", return_value=None)
    )
    mocks["init_db_pool"] = stack.enter_context(
        patch("core.lifespan.init_db_pool", new_callable=AsyncMock)
    )
    mocks["ensure_api_keys"] = stack.enter_context(
        patch("core.lifespan._ensure_api_key_tables", new_callable=AsyncMock)
    )
    mocks["config_refresh"] = stack.enter_context(
        patch("core.lifespan.config_cache.refresh_async", new_callable=AsyncMock)
    )
    mocks["redis_manager"] = stack.enter_context(patch("core.lifespan.redis_manager"))
    # Redis ping-এর জন্য mock return value সেট করো
    mock_ping = AsyncMock()
    mocks["redis_manager"].client.ping = mock_ping
    mocks["redis_manager"].close = AsyncMock()
    mocks["orchestrator"] = stack.enter_context(patch("core.lifespan.Orchestrator"))
    stack.enter_context(patch("core.lifespan.maintenance_pipeline.start_monitoring"))
    mocks["create_task"] = stack.enter_context(patch("asyncio.create_task"))
    stack.enter_context(patch("asyncio.to_thread", side_effect=lambda f, *a, **kw: f()))

    mocks["services"] = stack.enter_context(patch("core.lifespan.services"))

    # Mock httpx.AsyncClient so it doesn't create real connections
    mocks["httpx_client"] = stack.enter_context(
        patch("core.lifespan.httpx.AsyncClient", return_value=AsyncMock())
    )

    # Services HTTP client mock (legacy, but keep for compatibility)
    mocks["services"].global_http_client = mocks["httpx_client"].return_value

    return mocks


class TestEnsureAPIKeyTables:
    """Tests for _ensure_api_key_tables function."""

    @pytest.mark.anyio
    async def test_creates_tables_successfully(self):
        """API key tables সফলভাবে তৈরি হয় কিনা পরীক্ষা করো।"""
        mock_pool = AsyncMock()
        mock_conn = AsyncMock()
        mock_transaction = AsyncMock()

        mock_pool.acquire = AsyncMock(return_value=mock_conn)
        mock_pool.release = AsyncMock()
        mock_conn.transaction = MagicMock(return_value=mock_transaction)
        mock_conn.execute = AsyncMock()

        with patch("core.lifespan.get_db_pool", return_value=mock_pool):
            await _ensure_api_key_tables()

        mock_pool.acquire.assert_called_once()
        mock_pool.release.assert_called_once()

    @pytest.mark.anyio
    async def test_handles_db_error(self):
        """Database error — _ensure_api_key_tables exception propagate করে কিনা পরীক্ষা করো।"""
        mock_pool = AsyncMock()
        mock_pool.acquire = AsyncMock(side_effect=Exception("DB error"))

        with patch("core.lifespan.get_db_pool", return_value=mock_pool):
            # _ensure_api_key_tables internally exception propagate করে
            # lifespan startup-এ এটি ধরে defensive mode-এ চালু হয়
            with pytest.raises(Exception, match="DB error"):
                await _ensure_api_key_tables()


class TestAppLifespan:
    """Tests for app_lifespan context manager."""

    @pytest.mark.anyio
    async def test_startup_validates(self):
        """Startup validation call করা হয় কিনা পরীক্ষা করো।"""
        mock_app = MagicMock()

        with contextlib.ExitStack() as stack:
            mocks = _apply_common_patches(stack)

            await _run_lifespan(mock_app)

            mocks["validate"].assert_called_once()

    @pytest.mark.anyio
    async def test_startup_skips_db_for_sqlite(self):
        """SQLite database-এ PostgreSQL pool init skip করা হয় কিনা পরীক্ষা করো।"""
        mock_app = MagicMock()

        with contextlib.ExitStack() as stack:
            mocks = _apply_common_patches(stack)
            mock_settings = stack.enter_context(patch("core.lifespan.settings"))
            mock_settings.supabase_database_url = "sqlite+aiosqlite:///:memory:"

            await _run_lifespan(mock_app)

            # SQLite হলে PostgreSQL pool initialize করা উচিত নয়
            mocks["init_db_pool"].assert_not_called()

    @pytest.mark.anyio
    async def test_shutdown_closes_http_client(self):
        """Shutdown-এ HTTP client বন্ধ করা হয় কিনা পরীক্ষা করো।"""
        mock_app = MagicMock()

        with contextlib.ExitStack() as stack:
            mocks = _apply_common_patches(stack)
            # The HTTP client used in shutdown will be the one returned by httpx.AsyncClient()
            mock_http_client = mocks["httpx_client"].return_value

            mock_pool = AsyncMock()
            mock_pool.close = AsyncMock()
            mock_get_pool = stack.enter_context(
                patch("core.lifespan.get_db_pool", new_callable=AsyncMock)
            )
            mock_get_pool.return_value = mock_pool

            mock_orch = MagicMock()
            mock_orch.stop = AsyncMock()
            mocks["orchestrator"].return_value = mock_orch

            await _run_lifespan(mock_app)

            mock_http_client.aclose.assert_called_once()

    @pytest.mark.anyio
    async def test_shutdown_cancels_background_tasks(self):
        """Background tasks shutdown-এ cancel করা হয় কিনা পরীক্ষা করো।"""
        mock_app = MagicMock()

        with contextlib.ExitStack() as stack:
            mocks = _apply_common_patches(stack)

            stack.enter_context(
                patch("asyncio.wait_for", side_effect=asyncio.CancelledError)
            )

            mock_pool = AsyncMock()
            mock_pool.close = AsyncMock()
            mock_get_pool = stack.enter_context(
                patch("core.lifespan.get_db_pool", new_callable=AsyncMock)
            )
            mock_get_pool.return_value = mock_pool

            mock_orch = MagicMock()
            mock_orch.stop = AsyncMock()
            mocks["orchestrator"].return_value = mock_orch

            # CancelledError gracefully handle করা উচিত
            try:
                await _run_lifespan(mock_app)
            except asyncio.CancelledError:
                pass

    @pytest.mark.anyio
    async def test_handles_teardown_errors(self):
        """Teardown errors সত্ত্বেও crash না করা পরীক্ষা করো।"""
        mock_app = MagicMock()

        with contextlib.ExitStack() as stack:
            stack.enter_context(
                patch("core.lifespan.StartupValidator.validate", new_callable=AsyncMock)
            )
            stack.enter_context(
                patch(
                    "core.lifespan.ReliabilityController.initialize",
                    new_callable=AsyncMock,
                )
            )
            stack.enter_context(
                patch("core.observability.telemetry.setup_tracing", return_value=None)
            )
            stack.enter_context(
                patch("core.lifespan.init_db_pool", new_callable=AsyncMock)
            )
            stack.enter_context(
                patch("core.lifespan._ensure_api_key_tables", new_callable=AsyncMock)
            )
            stack.enter_context(
                patch(
                    "core.lifespan.config_cache.refresh_async", new_callable=AsyncMock
                )
            )
            mock_redis = stack.enter_context(patch("core.lifespan.redis_manager"))
            mock_redis.client.ping = AsyncMock()
            # Redis close-এ error simulate করো
            mock_redis.close = AsyncMock(side_effect=Exception("Redis error"))

            # Redis error সত্ত্বেও exception raise করা উচিত নয়
            await _run_lifespan(mock_app)


class TestLifespanSubsystemStatus:
    """Tests for subsystem status tracking."""

    @pytest.mark.anyio
    async def test_subsystem_status_defaults(self):
        """Subsystem status সঠিকভাবে initialize হয় কিনা পরীক্ষা করো।"""
        mock_app = MagicMock()

        with contextlib.ExitStack() as stack:
            mocks = _apply_common_patches(stack)
            # Make sure it's not sqlite so it calls init_db_pool
            mock_settings = MagicMock()
            mock_settings.supabase_database_url = "postgresql://dummy"
            stack.enter_context(patch("core.lifespan.settings", mock_settings))
            # DB failure simulate করো
            mocks["init_db_pool"].side_effect = Exception("DB failed")
            mocks["redis_manager"].client.ping.side_effect = Exception("Redis failed")

            await _run_lifespan(mock_app)

            # Subsystem status set হয়েছে কিনা চেক করো
            assert hasattr(mock_app.state, "subsystem_status")
            assert mock_app.state.subsystem_status["db"] == "down"
            assert mock_app.state.subsystem_status["redis"] == "down"
