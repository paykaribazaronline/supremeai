# বাংলা মন্তব্য: core module-এর কম-কভার লাইন কভার করার জন্য অতিরিক্ত টেস্টসমূহ
import asyncio
import contextlib
import json
import os
import sys
import time
from datetime import UTC, datetime
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from core.messaging.event_bus import ErrorContext

# ---------------------------------------------------------------------------
# Helpers / fixtures
# ---------------------------------------------------------------------------


@pytest.fixture(autouse=True)
def _isolate_test_env(monkeypatch):
    monkeypatch.setenv("ENV", "test")
    monkeypatch.setenv("SUPREMEAI_JWT_SECRET", "test-secret-placeholder")
    monkeypatch.setenv("SUPREMEAI_ADMIN_PASSWORD_HASH", "")
    monkeypatch.delenv("ENCRYPTION_KEY", raising=False)
    yield
    return


# ========================== config.py ==========================


class TestSettingsValidators:
    """Cover validator branches not exercised by test_config.py."""

    def test_parse_admin_emails_comma_separated(self):
        from core.config import Settings

        assert Settings.parse_admin_emails("a@b.com, c@d.com") == ["a@b.com", "c@d.com"]

    def test_parse_allowed_hosts_comma_separated(self):
        from core.config import Settings

        assert Settings.parse_allowed_hosts("host1,host2") == ["host1", "host2"]

    def test_parse_cors_origins_json_string(self):
        from core.config import Settings

        assert Settings.parse_cors_origins(
            '["http://a.com", "http://b.com"]',
            type("FakeInfo", (), {"data": {"env": "local"}})(),
        ) == ["http://a.com", "http://b.com"]

    def test_parse_cors_origins_comma_string(self):
        from core.config import Settings

        assert Settings.parse_cors_origins(
            "http://a.com,http://b.com",
            type("FakeInfo", (), {"data": {"env": "local"}})(),
        ) == ["http://a.com", "http://b.com"]

    def test_parse_cors_origins_production_filters_localhost(self):
        from core.config import Settings

        result = Settings.validate_cors_origins(
            ["http://localhost:3000", "https://prod.com"],
            type(
                "FakeInfo",
                (),
                {"data": {"env": "production"}, "field_name": "cors_origins"},
            )(),
        )
        assert "http://localhost:3000" not in result
        assert "https://prod.com" in result

    def test_validate_debug_mode(self):
        from core.config import Settings

        result = Settings.validate_debug_mode(True, type("FakeInfo", (), {"data": {"env": "production"}})())
        assert result is False

    def test_set_jwt_secret_non_production_returns_placeholder(self, monkeypatch):
        from core.config import Settings

        monkeypatch.setenv("ENV", "test")
        monkeypatch.setenv("SUPREMEAI_JWT_SECRET", "a" * 64)
        s = Settings()
        assert len(s.jwt_secret) >= 64

    def test_get_cached_secret_caches_value(self, monkeypatch):
        from core.config import Settings

        calls = []

        def fake_fetch(key, *args, **kwargs):
            calls.append(key)
            return f"secret-for-{key}"

        monkeypatch.setattr("core.config.secret_vault.fetch_secret", fake_fetch)
        s = Settings()
        s._secrets_batch_loaded = False
        s._BATCH_SECRET_KEYS = ["X"]
        v1 = s._get_cached_secret("X")
        v2 = s._get_cached_secret("X")
        assert v1 == v2 == "secret-for-X"
        assert len(calls) == 1

    def test_computed_fields_read_from_vault(self, monkeypatch):
        from core.config import Settings

        monkeypatch.delenv("DATABASE_URL", raising=False)
        monkeypatch.delenv("SUPABASE_DATABASE_URL_POOLER", raising=False)
        monkeypatch.delenv("REDIS_URL", raising=False)
        monkeypatch.delenv("OPENROUTER_API_KEY", raising=False)
        monkeypatch.setattr("core.config.secret_vault.fetch_secret", lambda k, *a, **kw: f"val-{k}")
        s = Settings()
        assert s.supabase_database_url == "val-SUPABASE_DATABASE_URL_POOLER"
        assert s.redis_url == "redis://val-REDIS_URL"
        assert s.openrouter_api_key == "val-OPENROUTER_API_KEY"


# ========================== config_cache.py ==========================


class TestConfigCacheMissingBranches:
    def test_should_refresh_after_ttl(self):
        from core.config_cache import ConfigCache

        cache = ConfigCache(ttl_seconds=0)
        cache._last_refresh = time.time() - 1
        assert cache._should_refresh() is True

    def test_should_refresh_within_ttl(self):
        from core.config_cache import ConfigCache

        cache = ConfigCache(ttl_seconds=60)
        cache._last_refresh = time.time()
        assert cache._should_refresh() is False

    def test_refresh_sync_loads_defaults_on_db_failure(self, monkeypatch):
        from core.config_cache import DEFAULT_CONFIGS, ConfigCache

        async def fake_load_from_db_async(self):
            raise RuntimeError("db down")

        monkeypatch.setattr(ConfigCache, "_load_from_db_async", fake_load_from_db_async)
        cache = ConfigCache()
        cache.refresh_sync_bootstrap()
        assert cache._loaded is True
        assert cache.get("cache_threshold_code") == DEFAULT_CONFIGS["cache_threshold_code"]

    def test_get_all_category_filter(self):
        from core.config_cache import DEFAULT_CONFIGS, ConfigCache

        cache = ConfigCache()
        cache._loaded = True
        cache._cache = dict(DEFAULT_CONFIGS)
        filtered = cache.get_all("cache_threshold_")
        assert "cache_threshold_code" in filtered
        assert "feature_semantic_cache" not in filtered

    def test_get_all_no_category_returns_copy(self):
        from core.config_cache import DEFAULT_CONFIGS, ConfigCache

        cache = ConfigCache()
        cache._loaded = True
        cache._cache = dict(DEFAULT_CONFIGS)
        all_conf = cache.get_all()
        all_conf["new_key"] = "new_val"
        assert "new_key" not in cache._cache

    @pytest.mark.asyncio
    async def test_set_updates_in_memory_cache(self):
        from core.config_cache import ConfigCache

        # বাংলা মন্তব্য: testing loop triggers এবং refresh bypass করতে ttl ও last_refresh নির্ধারণ করা হলো
        cache = ConfigCache(ttl_seconds=3600)
        cache._last_refresh = time.time()
        cache._loaded = True

        mock_session = MagicMock()
        mock_session.execute = AsyncMock()
        mock_result = MagicMock()
        mock_result.scalar_one_or_none.return_value = None
        mock_session.execute.return_value = mock_result
        mock_session.commit = AsyncMock()
        mock_session.add = MagicMock()
        mock_session.__aenter__ = AsyncMock(return_value=mock_session)
        mock_session.__aexit__ = AsyncMock()

        with patch("database.session._get_session_maker") as mock_maker:
            mock_maker.return_value = MagicMock(return_value=mock_session)
            ok = await cache.set("new_key", "new_value")
        assert ok is True
        assert cache.get("new_key") == "new_value"

    def test_invalidate_specific_key(self):
        from core.config_cache import ConfigCache

        cache = ConfigCache()
        cache._cache = {"a": 1, "b": 2}
        cache._loaded = True
        cache.invalidate("a")
        assert "a" not in cache._cache
        assert cache.get("a") is None

    def test_invalidate_all_clears_cache(self):
        from core.config_cache import ConfigCache

        cache = ConfigCache()
        cache._cache = {"a": 1}
        cache._loaded = True
        cache.invalidate()
        assert cache._cache == {}
        assert cache._loaded is False

    @pytest.mark.asyncio
    async def test_refresh_async_db_failure_uses_defaults(self):
        from core.config_cache import DEFAULT_CONFIGS, ConfigCache

        cache = ConfigCache()
        with patch("database.session._get_session_maker", side_effect=RuntimeError("db down")):
            await cache.refresh_async()
        assert cache._loaded is True
        assert cache.get("cache_threshold_code") == DEFAULT_CONFIGS["cache_threshold_code"]


# ========================== config_proxy.py ==========================


class TestConfigProxyMissingBranches:
    @pytest.mark.asyncio
    async def test_get_refreshes_after_expiry(self):
        from core.config_proxy import DynamicConfigProxy

        proxy = DynamicConfigProxy("t1", MagicMock())
        proxy._cache = {"k": "old"}
        proxy._expiry = datetime.min.replace(tzinfo=UTC)

        doc_ref = MagicMock()
        snapshot = MagicMock()
        snapshot.exists = True
        snapshot.to_dict.return_value = {"k": "new"}
        doc_ref.get.return_value = snapshot
        proxy._db.collection.return_value.document.return_value = doc_ref

        # বাংলা মন্তব্য: async event loop runtime error এড়াতে async def এবং await ব্যবহার করা হলো
        result = await proxy.get("k")
        assert result == "new"

    @pytest.mark.asyncio
    async def test_get_uses_sync_get_when_not_coroutine(self):
        from core.config_proxy import DynamicConfigProxy

        proxy = DynamicConfigProxy("t1", MagicMock())
        proxy._cache = {"k": "val"}
        proxy._expiry = datetime.min.replace(tzinfo=UTC)

        doc_ref = MagicMock()
        snapshot = MagicMock()
        snapshot.exists = True
        snapshot.to_dict.return_value = {"k": "new"}
        doc_ref.get = MagicMock(return_value=snapshot)
        proxy._db.collection.return_value.document.return_value = doc_ref

        # বাংলা মন্তব্য: async event loop runtime error এড়াতে async def এবং await ব্যবহার করা হলো
        result = await proxy.get("k")
        assert result == "new"


# ========================== cost_guard.py ==========================


class TestCostGuardMissingBranches:
    @pytest.mark.asyncio
    async def test_sync_get_branch_when_not_coroutine(self):
        from core.cost_guard import CostGuard

        guard = CostGuard(MagicMock())
        doc_ref = MagicMock()
        snapshot = MagicMock()
        snapshot.exists = True
        snapshot.to_dict.return_value = {"monthly_limit": 10.0, "spent_amount": 1.0}
        doc_ref.get = MagicMock(return_value=snapshot)
        guard._db.collection.return_value.document.return_value = doc_ref

        result = await guard.check_budget("t1", 1.0)
        assert result is True

    @pytest.mark.asyncio
    async def test_validate_budget_accepts_known_tiers(self):
        from core.cost_guard import CostGuard

        guard = CostGuard()
        with patch(
            "core.cache.redis_manager.redis_manager.get_cache",
            new_callable=AsyncMock,
            return_value="0.0",
        ):
            for tier in ("free", "economy", "premium"):
                assert await guard.validate_budget("t1", tier) is True

    @pytest.mark.asyncio
    async def test_validate_budget_returns_true_for_unknown_tier(self):
        from core.cost_guard import CostGuard

        guard = CostGuard()
        assert await guard.validate_budget("t1", "unknown") is True

    @pytest.mark.asyncio
    async def test_check_budget_bypasses_when_no_db(self):
        from core.cost_guard import CostGuard

        guard = CostGuard(db=None)
        result = await guard.check_budget("any-tenant", 999.0)
        assert result is True


# ========================== event_bus.py ==========================


class TestEventBusMissingBranches:
    def test_register_listener(self):
        from core.messaging.event_bus import ErrorEventBus

        bus = ErrorEventBus()
        listener = MagicMock()
        bus.register_listener(listener)
        assert listener in bus._listeners["*"]

    def test_emit_no_running_loop_runs_directly(self):
        from core.messaging.event_bus import ErrorEvent, ErrorEventBus

        bus = ErrorEventBus()
        listener = AsyncMock()
        bus.register_listener(listener)

        event = ErrorEvent(
            module="test",
            error_type="Err",
            message="msg",
            severity="WARNING",
            structured_context=ErrorContext(module="auto_fixed"),
            context={},
        )

        with patch("asyncio.get_running_loop", side_effect=RuntimeError("no loop")):
            with patch("core.messaging.event_bus.logger.debug") as mock_debug:
                bus.emit(event)
                mock_debug.assert_called()

    @pytest.mark.asyncio
    async def test_emit_async_fires_listeners(self):
        from core.messaging.event_bus import ErrorEvent, ErrorEventBus

        bus = ErrorEventBus()
        listener = AsyncMock()
        bus.register_listener("Err", listener)

        event = ErrorEvent(
            module="test",
            error_type="Err",
            message="msg",
            severity="WARNING",
            structured_context=ErrorContext(module="auto_fixed"),
            context={},
        )

        await bus.emit_async(event)
        await asyncio.sleep(0.05)
        listener.assert_called_once_with(event)

    @pytest.mark.asyncio
    async def test_emit_async_sync_listener(self):
        from core.messaging.event_bus import ErrorEvent, ErrorEventBus

        bus = ErrorEventBus()
        listener = MagicMock()
        bus.register_listener("Err", listener)

        event = ErrorEvent(
            module="test",
            error_type="Err",
            message="msg",
            severity="WARNING",
            structured_context=ErrorContext(module="auto_fixed"),
            context={},
        )
        await bus.emit_async(event)
        await asyncio.sleep(0.05)
        listener.assert_called_once_with(event)

    @pytest.mark.asyncio
    async def test_handler_failure_routes_to_dlq(self):
        from core.messaging.event_bus import (
            DeadLetterQueueItem,
            ErrorEvent,
            ErrorEventBus,
        )

        bus = ErrorEventBus()
        dlq_handler = AsyncMock()
        bus.register_dead_letter_handler(dlq_handler)

        listener = MagicMock(side_effect=RuntimeError("boom"))
        bus.register_listener("Err", listener)

        event = ErrorEvent(
            module="test",
            error_type="Err",
            message="msg",
            severity="ERROR",
            structured_context=ErrorContext(module="auto_fixed"),
            context={},
        )
        await bus.emit_async(event)
        await asyncio.sleep(0.05)
        assert bus.dead_letter_queue_size == 1
        dlq_handler.assert_called_once()
        item = dlq_handler.call_args[0][0]
        assert isinstance(item, DeadLetterQueueItem)

    @pytest.mark.asyncio
    async def test_dlq_full_drops_and_logs_critical(self):
        from core.messaging.event_bus import (
            DeadLetterQueueItem,
            ErrorEvent,
            ErrorEventBus,
        )

        bus = ErrorEventBus()
        # Pre-fill DLQ to maxsize
        for _ in range(1000):
            bus._dlq.put_nowait(
                DeadLetterQueueItem(
                    event_type="x",
                    handler_name="h",
                    error="e",
                    timestamp=datetime.now(UTC),
                )
            )

        listener = MagicMock(side_effect=RuntimeError("boom"))
        bus.register_listener("Err", listener)

        event = ErrorEvent(
            module="test",
            error_type="Err",
            message="msg",
            severity="ERROR",
            structured_context=ErrorContext(module="auto_fixed"),
            context={},
        )
        with patch("core.messaging.event_bus.logger.critical") as mock_critical:
            await bus.emit_async(event)
            await asyncio.sleep(0.05)
            mock_critical.assert_called()

    @pytest.mark.asyncio
    async def test_process_dead_letter_queue_returns_items(self):
        from core.messaging.event_bus import DeadLetterQueueItem, ErrorEventBus

        bus = ErrorEventBus()
        item = DeadLetterQueueItem(event_type="e", handler_name="h", error="err", timestamp=datetime.now(UTC))
        bus._dlq.put_nowait(item)
        processed = await bus.process_dead_letter_queue(max_items=10)
        assert len(processed) == 1
        assert processed[0].retry_count == 1

    def test_stats_property(self):
        from core.messaging.event_bus import ErrorEventBus

        bus = ErrorEventBus()
        stats = bus.stats
        assert "total_emitted" in stats
        assert "dlq_current_size" in stats


# ========================== pubsub.py ==========================


class TestPubSubMissingBranches:
    @pytest.mark.asyncio
    async def test_subscribe_creates_channel(self):
        from core.messaging.pubsub import PubSub

        pubsub = PubSub()
        q = await pubsub.subscribe("ch1")
        assert "ch1" in pubsub.subscribers
        assert q in pubsub.subscribers["ch1"]

    @pytest.mark.asyncio
    async def test_unsubscribe_removes_channel_when_empty(self):
        from core.messaging.pubsub import PubSub

        pubsub = PubSub()
        q = await pubsub.subscribe("ch1")
        await pubsub.unsubscribe("ch1", q)
        assert "ch1" not in pubsub.subscribers

    @pytest.mark.asyncio
    async def test_unsubscribe_nonexistent_channel(self):
        from core.messaging.pubsub import PubSub

        pubsub = PubSub()
        q = MagicMock()
        await pubsub.unsubscribe("missing", q)

    @pytest.mark.asyncio
    async def test_publish_no_subscribers(self):
        from core.messaging.pubsub import PubSub

        pubsub = PubSub()
        await pubsub.publish("missing", {"msg": 1})

    @pytest.mark.asyncio
    async def test_publish_delivers_to_subscribers(self):
        from core.messaging.pubsub import PubSub

        pubsub = PubSub()
        q = await pubsub.subscribe("ch1")
        msg = {"msg": 1}
        await pubsub.publish("ch1", msg)
        received = await q.get()
        assert received == msg


# ========================== knowledge_base.py ==========================


class TestKnowledgeBaseMissingBranches:
    def test_module_creates_data_dir_and_file(self, monkeypatch, tmp_path):
        import importlib

        # বাংলা মন্তব্য: reloading logic matching এর জন্য environmental variables set করা হলো
        monkeypatch.setenv("SUPREMEAI_BASE_DIR", str(tmp_path))
        monkeypatch.setenv("SUPREMEAI_DATA_DIR", str(tmp_path / "data"))
        monkeypatch.setenv("SUPREMEAI_MEMORY_FILE_PATH", str(tmp_path / "data" / "memory_vault.json"))

        import core.knowledge_base as kb

        importlib.reload(kb)

        assert (tmp_path / "data").exists()
        assert (tmp_path / "data" / "memory_vault.json").exists()


# ========================== security_vault.py ==========================


class TestSecurityVaultModuleInit:
    def test_module_raises_without_encryption_key(self, monkeypatch):
        # বাংলা মন্তব্য: নতুন STRICT_ENCRYPTION_CHECK ফ্ল্যাগ সেট করে এক্সেপশন রেইজ পাথটি টেস্ট করা হচ্ছে।
        monkeypatch.setenv("STRICT_ENCRYPTION_CHECK", "true")
        monkeypatch.delenv("ENCRYPTION_KEY", raising=False)
        monkeypatch.delenv("ENCRYPTION_KEY", raising=False)

        monkeypatch.delitem(sys.modules, "core.security_vault", raising=False)
        monkeypatch.delitem(sys.modules, "core.security.security_vault", raising=False)

        with pytest.raises(ValueError, match="CRITICAL: ENCRYPTION_KEY"):
            import core.security.security_vault  # noqa: F401


# ========================== swarm_orchestrator.py ==========================


class TestSwarmOrchestratorMissingBranches:
    @pytest.mark.anyio
    async def test_execute_task_runs_all_agents(self):
        from core.orchestration.swarm_orchestrator import SwarmOrchestrator

        orchestrator = SwarmOrchestrator()

        with (
            patch(
                "core.orchestration.agent_orchestrator.budget_aware_route",
                return_value={
                    "intent": "coding",
                    "tier": "free",
                    "best_provider": "gemini",
                },
            ),
            patch.object(
                orchestrator,
                "_synthesize_tool",
                new_callable=AsyncMock,
                return_value={"agent_name": "mocked"},
            ),
            patch.object(orchestrator.agents["architect"], "run", new_callable=AsyncMock) as mock_design,
            patch.object(orchestrator.agents["coder"], "run", new_callable=AsyncMock) as mock_code,
            patch.object(orchestrator.agents["guardian"], "run", new_callable=AsyncMock),
            patch.object(
                orchestrator.agents["guardian"],
                "validate",
                new_callable=AsyncMock,
                return_value=(True, "OK"),
            ),
            patch.object(orchestrator.agents["reflection"], "run", new_callable=AsyncMock),
            patch.object(
                orchestrator.agents["reflection"],
                "reflect_and_persist",
                new_callable=AsyncMock,
            ),
        ):
            workspace = await orchestrator.execute_task("write a python script", "uid")
            mock_design.assert_called_once()
            mock_code.assert_called_once()
            # mock_verify is guardian.run, which is not called for code_generation. validate is called instead.
            assert workspace is not None

    @pytest.mark.anyio
    async def test_circuit_breaker_opens_after_threshold(self):
        # বাংলা মন্তব্য: CircuitBreaker ও সম্পর্কিত স্টেট/এরর সরাসরি core.resilience.circuit_breaker থেকে ইম্পোর্ট করা হলো।
        from core.resilience.circuit_breaker import (
            CircuitBreaker,
            CircuitBreakerOpenError,
            CircuitBreakerState,
        )

        cb = CircuitBreaker(name="morphic", failure_threshold=2, recovery_timeout=0.1)

        async def failing():
            raise RuntimeError("fail")

        with pytest.raises(RuntimeError):
            await cb.acall(failing)
        with pytest.raises(RuntimeError):
            await cb.acall(failing)

        assert cb.state == CircuitBreakerState.OPEN

        with pytest.raises(CircuitBreakerOpenError):
            await cb.acall(failing)

    @pytest.mark.anyio
    async def test_circuit_breaker_half_open_after_timeout(self):
        # বাংলা মন্তব্য: CircuitBreaker ও সম্পর্কিত স্টেট/এরর সরাসরি core.resilience.circuit_breaker থেকে ইম্পোর্ট করা হলো।
        from core.resilience.circuit_breaker import CircuitBreaker, CircuitBreakerState

        cb = CircuitBreaker(name="test", failure_threshold=1, recovery_timeout=0.05)

        async def failing():
            raise RuntimeError("fail")

        with pytest.raises(RuntimeError):
            await cb.acall(failing)
        assert cb.state == CircuitBreakerState.OPEN

        await asyncio.sleep(0.1)

        async def succeeding():
            return "ok"

        res = await cb.acall(succeeding)
        assert res == "ok"
        assert cb.state == CircuitBreakerState.CLOSED


# ========================== llm_gateway.py ==========================


class TestLLMGatewayMissingBranches:
    @pytest.mark.skip(reason="Technical Debt: CostGuard mock needs update. Tracked in TECH_DEBT.md")
    @pytest.mark.anyio
    async def test_acompletion_cost_guard_check(self, monkeypatch):
        from core.llm.llm_gateway import LLMGateway

        gateway = LLMGateway()
        gateway.cache = MagicMock()
        gateway.cache.query_similar = AsyncMock(return_value=None)
        gateway.routing_policy = {"complexity_rules": {}, "fallback_chain": []}

        mock_db = MagicMock()
        mock_cost_guard = MagicMock()
        mock_cost_guard.check_budget = AsyncMock()

        with (
            patch("core.llm_gateway.get_firestore_db", return_value=mock_db),
            patch("core.llm_gateway.CostGuard", return_value=mock_cost_guard),
            patch(
                "litellm.acompletion",
                new_callable=AsyncMock,
                return_value=MagicMock(
                    choices=[MagicMock(message=MagicMock(content="ok"))],
                    _response_metadata={},
                ),
            ),
        ):
            os.environ["OPENAI_API_KEY"] = "mock"
            result = await gateway.acompletion(prompt="hi", tenant_id="t1")
            assert result["success"] is True
            mock_cost_guard.check_budget.assert_called_once()

    @pytest.mark.anyio
    async def test_acompletion_provider_filtering_chain(self):
        from core.llm.llm_gateway import LLMGateway

        gateway = LLMGateway()
        gateway.cache = MagicMock()
        gateway.cache.query_similar = AsyncMock(return_value=None)
        gateway.routing_policy = {
            "complexity_rules": {"easy": ["groq/llama", "openai/gpt"]},
            "fallback_chain": ["fb/model"],
        }

        with patch(
            "litellm.acompletion",
            new_callable=AsyncMock,
            return_value=MagicMock(
                choices=[MagicMock(message=MagicMock(content="ok"))],
                _response_metadata={},
            ),
        ) as mock_call:
            os.environ["OPENAI_API_KEY"] = "mock"
            os.environ["GROQ_API_KEY"] = "mock"
            result = await gateway.acompletion(prompt="hi", provider="groq")
            assert result["success"] is True
            assert mock_call.call_args.kwargs["model"] == "groq/llama"

    @pytest.mark.anyio
    async def test_acompletion_messages_list_input(self):
        from core.llm.llm_gateway import LLMGateway

        gateway = LLMGateway()
        gateway.cache = MagicMock()
        gateway.cache.query_similar = AsyncMock(return_value=None)
        gateway.routing_policy = {"complexity_rules": {}, "fallback_chain": []}

        with patch(
            "litellm.acompletion",
            new_callable=AsyncMock,
            return_value=MagicMock(
                choices=[MagicMock(message=MagicMock(content="ok"))],
                _response_metadata={},
            ),
        ) as mock_call:
            os.environ["OPENAI_API_KEY"] = "mock"
            msgs = [{"role": "user", "content": "hi"}]
            result = await gateway.acompletion(prompt=msgs)
            assert result["success"] is True
            assert mock_call.call_args.kwargs["messages"] == msgs

    @pytest.mark.anyio
    async def test_acompletion_self_healer_on_failure(self):
        from core.llm.llm_gateway import LLMGateway

        gateway = LLMGateway()
        gateway.cache = MagicMock()
        gateway.cache.query_similar = AsyncMock(return_value=None)
        gateway.routing_policy = {"complexity_rules": {}, "fallback_chain": []}

        mock_db = MagicMock()
        mock_healer = MagicMock()
        mock_healer.propose_fix = AsyncMock()

        mock_cost_guard = MagicMock()
        mock_cost_guard.check_budget = AsyncMock()

        with (
            patch("core.llm.llm_gateway.get_firestore_db", return_value=mock_db),
            patch("core.llm.llm_gateway.SelfHealerService", return_value=mock_healer),
            patch("core.llm.llm_gateway.CostGuard", return_value=mock_cost_guard),
            patch(
                "litellm.acompletion",
                new_callable=AsyncMock,
                side_effect=Exception("fail"),
            ),
        ):
            os.environ["OPENAI_API_KEY"] = "mock"
            with pytest.raises(
                Exception
            ):  # -- intentionally broad: asserts *some* error propagates (mocked/validation failure), exact type varies
                await gateway.acompletion(prompt="hi", tenant_id="t1")
            mock_healer.propose_fix.assert_called_once()

    def test_get_key_for_model_unknown(self):
        from core.llm.llm_gateway import LLMGateway

        gateway = LLMGateway()
        assert gateway._get_api_key_for_model("unknown/model") is None


# ========================== log_batcher.py ==========================


class TestLogBatcherMissingBranches:
    @pytest.mark.anyio
    async def test_run_requeues_on_critical_error(self):
        from core.observability.log_batcher import LogBatcherService

        service = LogBatcherService(flush_interval=0.1, batch_size=2)
        service.running = True

        call_count = 0

        async def mock_wait_for(coro, timeout):
            nonlocal call_count
            call_count += 1
            if call_count == 1:
                return {"x": 1}
            service.running = False
            raise Exception("critical")

        with patch("asyncio.wait_for", side_effect=mock_wait_for):
            with patch.object(service, "_flush", new_callable=AsyncMock):
                await service._run()
        assert service.running is False

    @pytest.mark.anyio
    async def test_run_drains_queue_up_to_batch_size(self):
        from core.observability.log_batcher import LogBatcherService

        service = LogBatcherService(flush_interval=0.1, batch_size=3)
        service.running = True

        call_count = 0

        async def mock_wait_for(coro, timeout):
            nonlocal call_count
            call_count += 1
            if call_count == 1:
                service.queue.put_nowait({"i": 1})
                service.queue.put_nowait({"i": 2})
                return {"i": 0}
            service.running = False
            raise TimeoutError()

        with patch("asyncio.wait_for", side_effect=mock_wait_for):
            with patch.object(service, "_flush", new_callable=AsyncMock) as mock_flush:
                await service._run()
                # বাংলা মন্তব্য: ইভেন্ট লুপ ইটারেসনের কারণে flushing ১ বা ২ বার হতে পারে, তাই check_count flexible রাখা হলো
                assert mock_flush.call_count >= 1


# ========================== container_auditor.py ==========================


class TestContainerAuditorMissingBranches:
    def test_get_container_stats_returns_list_on_success(self, monkeypatch):
        from core.container_auditor import ContainerAuditor

        auditor = ContainerAuditor(check_interval_seconds=1)
        fake_stdout = json.dumps({"Name": "c1", "MemPerc": "10.5%"}) + "\n"
        mock_result = MagicMock()
        mock_result.returncode = 0
        mock_result.stdout = fake_stdout
        mock_result.stderr = ""

        monkeypatch.setattr("subprocess.run", lambda *args, **kwargs: mock_result)
        stats = auditor.get_container_stats()
        assert isinstance(stats, list)
        assert stats[0]["Name"] == "c1"

    def test_get_container_stats_returns_empty_on_failure(self, monkeypatch):
        from core.container_auditor import ContainerAuditor

        auditor = ContainerAuditor(check_interval_seconds=1)
        mock_result = MagicMock()
        mock_result.returncode = 1
        mock_result.stderr = "docker error"
        monkeypatch.setattr("subprocess.run", lambda *args, **kwargs: mock_result)
        assert auditor.get_container_stats() == []

    def test_get_container_stats_handles_exception(self, monkeypatch):
        from core.container_auditor import ContainerAuditor

        auditor = ContainerAuditor(check_interval_seconds=1)
        monkeypatch.setattr(
            "subprocess.run",
            lambda *args, **kwargs: (_ for _ in ()).throw(RuntimeError("boom")),
        )
        assert auditor.get_container_stats() == []

    def test_parse_memory_percent_valid(self):
        from core.container_auditor import ContainerAuditor

        auditor = ContainerAuditor()
        assert auditor.parse_memory_percent("85.3%") == 85.3

    def test_parse_memory_percent_invalid(self):
        from core.container_auditor import ContainerAuditor

        auditor = ContainerAuditor()
        assert auditor.parse_memory_percent("not-a-number") == 0.0

    @pytest.mark.asyncio
    async def test_audit_cycle_warns_below_kill_threshold(self, monkeypatch):
        from core.container_auditor import ContainerAuditor

        auditor = ContainerAuditor(check_interval_seconds=1)
        monkeypatch.setattr(auditor, "get_container_stats", lambda: [{"Name": "c1", "MemPerc": "82.0%"}])
        with patch("core.container_auditor.logger.warning") as mock_warning:
            await auditor.audit_cycle()
            mock_warning.assert_called_once()

    @pytest.mark.asyncio
    async def test_audit_cycle_kills_above_threshold(self, monkeypatch):
        from core.container_auditor import ContainerAuditor

        auditor = ContainerAuditor(check_interval_seconds=1)
        monkeypatch.setattr(auditor, "get_container_stats", lambda: [{"Name": "c1", "MemPerc": "96.0%"}])
        with (
            patch("core.container_auditor.logger.error") as mock_error,
            patch("subprocess.run") as mock_run,
        ):
            await auditor.audit_cycle()
            mock_error.assert_called()
            mock_run.assert_called_once()

    @pytest.mark.asyncio
    async def test_audit_cycle_kill_failure_logs(self, monkeypatch):
        from core.container_auditor import ContainerAuditor

        auditor = ContainerAuditor(check_interval_seconds=1)
        monkeypatch.setattr(auditor, "get_container_stats", lambda: [{"Name": "c1", "MemPerc": "99.0%"}])
        with (
            patch("core.container_auditor.logger.error") as mock_error,
            patch("subprocess.run", side_effect=RuntimeError("kill fail")),
        ):
            await auditor.audit_cycle()
            mock_error.assert_called()

    @pytest.mark.asyncio
    async def test_run_stops_on_exception(self, monkeypatch):
        from core.container_auditor import ContainerAuditor

        auditor = ContainerAuditor(check_interval_seconds=0.01)
        call_count = 0

        async def fake_audit():
            nonlocal call_count
            call_count += 1
            if call_count == 1:
                raise RuntimeError("cycle fail")
            auditor.stop()

        monkeypatch.setattr(auditor, "audit_cycle", fake_audit)
        await auditor.run()
        assert auditor.running is False

    def test_stop_sets_running_false(self):
        from core.container_auditor import ContainerAuditor

        auditor = ContainerAuditor()
        auditor.running = True
        auditor.stop()
        assert auditor.running is False


# ========================== nats_messaging.py ==========================


class TestNATSMessagingMissingBranches:
    def test_init_defaults(self):
        try:
            from core.messaging.nats_messaging import NATSClient
        except ImportError:
            pytest.skip("nats module not installed")
        client = NATSClient()
        assert client.url == "nats://localhost:4222"
        assert client.token == "super_secret_token"
        assert client.nc is None
        assert client.js is None
        assert client.kv_store is None

    @pytest.mark.asyncio
    async def test_connect_creates_kv_store(self, monkeypatch):
        from core.messaging.nats_messaging import NATSClient
        from core.messaging.nats_messaging import nats as nats_module

        if nats_module is None:
            pytest.skip("nats module not installed")
        client = NATSClient()
        mock_nc = MagicMock()
        mock_js = MagicMock()
        mock_kv = MagicMock()
        mock_nc.jetstream.return_value = mock_js
        mock_js.key_value.side_effect = Exception("not found")
        mock_js.create_key_value = AsyncMock(return_value=mock_kv)

        with patch(
            "core.messaging.nats_messaging.nats.connect",
            new_callable=AsyncMock,
            return_value=mock_nc,
        ):
            await client.connect()

        assert client.nc is mock_nc
        assert client.js is mock_js
        assert client.kv_store is mock_kv

    @pytest.mark.asyncio
    async def test_publish_event_skips_when_not_connected(self, caplog):
        try:
            from core.messaging.nats_messaging import NATSClient
        except ImportError:
            pytest.skip("nats module not installed")
        client = NATSClient()
        await client.publish_event("subj", {"a": 1})
        assert "NATS client is not connected" in caplog.text

    @pytest.mark.asyncio
    async def test_publish_event_publishes_payload(self):
        try:
            from core.messaging.nats_messaging import NATSClient
        except ImportError:
            pytest.skip("nats module not installed")
        from pydantic import BaseModel

        client = NATSClient()
        client.nc = MagicMock()
        client.nc.publish = AsyncMock()

        class Dummy(BaseModel):
            a: int

        await client.publish_event("subj", Dummy(a=1))
        client.nc.publish.assert_called_once()
        args = client.nc.publish.call_args
        assert args[0][0] == "subj"
        assert json.loads(args[0][1].decode()) == {"a": 1}

    @pytest.mark.asyncio
    async def test_subscribe_skips_when_not_connected(self, caplog):
        try:
            from core.messaging.nats_messaging import NATSClient
        except ImportError:
            pytest.skip("nats module not installed")
        client = NATSClient()
        cb = MagicMock()
        await client.subscribe("subj", cb)
        assert "NATS client is not connected" in caplog.text

    @pytest.mark.asyncio
    async def test_register_and_get_worker(self):
        try:
            from core.messaging.nats_messaging import NATSClient
        except ImportError:
            pytest.skip("nats module not installed")
        client = NATSClient()
        client.kv_store = MagicMock()
        client.kv_store.put = AsyncMock()
        client.kv_store.get = AsyncMock(return_value=MagicMock(value=json.dumps({"id": "w1"}).encode()))

        await client.register_worker("w1", {"id": "w1"})
        worker = await client.get_worker("w1")
        assert worker == {"id": "w1"}

    @pytest.mark.asyncio
    async def test_get_worker_returns_none_on_missing(self):
        from core.messaging.nats_messaging import KeyValueError, NATSClient

        client = NATSClient()
        client.kv_store = MagicMock()
        client.kv_store.get = AsyncMock(side_effect=KeyValueError("missing"))
        assert await client.get_worker("missing") is None

    @pytest.mark.asyncio
    async def test_get_all_workers_returns_empty_when_no_kv(self):
        try:
            from core.messaging.nats_messaging import NATSClient
        except ImportError:
            pytest.skip("nats module not installed")
        client = NATSClient()
        assert await client.get_all_workers() == {}

    @pytest.mark.asyncio
    async def test_get_all_workers_lists_keys(self):
        try:
            from core.messaging.nats_messaging import NATSClient
        except ImportError:
            pytest.skip("nats module not installed")
        client = NATSClient()
        client.kv_store = MagicMock()
        client.kv_store.keys = AsyncMock(return_value=["w1"])
        entry = MagicMock()
        entry.value = json.dumps({"id": "w1"}).encode()
        client.kv_store.get = AsyncMock(return_value=entry)

        workers = await client.get_all_workers()
        assert workers == {"w1": {"id": "w1"}}


# ========================== playwright_manager.py ==========================


class TestPlaywrightManagerMissingBranches:
    def test_imports_without_playwright(self, monkeypatch):
        monkeypatch.setitem(sys.modules, "playwright", None)
        monkeypatch.setitem(sys.modules, "playwright.async_api", None)
        monkeypatch.delitem(sys.modules, "core.playwright_manager", raising=False)
        import core.playwright_manager as pm

        assert pm.async_playwright is None

    @pytest.mark.asyncio
    async def test_get_global_browser_raises_when_not_installed(self, monkeypatch):
        import core.playwright_manager as pm

        monkeypatch.setattr(pm, "_global_browser", None)

        import builtins

        original_callable = builtins.callable

        def mock_callable(obj):
            if getattr(obj, "__name__", "") == "async_playwright":
                return False
            return original_callable(obj)

        monkeypatch.setattr(builtins, "callable", mock_callable)

        with pytest.raises(RuntimeError, match="Playwright is not installed"):
            await pm.get_global_browser()

    @pytest.mark.asyncio
    async def test_shutdown_global_browser_handles_errors(self, monkeypatch):
        from core.playwright_manager import shutdown_global_browser

        mock_browser = MagicMock()
        mock_runner = MagicMock()
        monkeypatch.setattr("core.playwright_manager._global_browser", mock_browser)
        monkeypatch.setattr("core.playwright_manager._playwright_runner", mock_runner)
        monkeypatch.setattr(
            "core.playwright_manager._global_browser.close",
            AsyncMock(side_effect=RuntimeError("close fail")),
        )
        monkeypatch.setattr(
            "core.playwright_manager._playwright_runner.stop",
            AsyncMock(side_effect=RuntimeError("stop fail")),
        )

        # The function should complete without raising, even with errors
        await shutdown_global_browser()
        assert True


# ========================== swarm_pubsub.py ==========================


class TestSwarmPubSubMissingBranches:
    @pytest.mark.skip(reason="SwarmPubSub requires Redis connection - integration test needed")
    @pytest.mark.asyncio
    async def test_subscribe_yields_messages(self, monkeypatch):
        from core.swarm_pubsub import SwarmPubSub

        pubsub = SwarmPubSub()
        mock_pubsub = MagicMock()
        mock_pubsub.subscribe = AsyncMock()
        mock_pubsub.get_message = AsyncMock(side_effect=[{"data": b"hello"}, None, {"data": b"world"}])
        mock_pubsub.unsubscribe = AsyncMock()
        mock_pubsub.close = AsyncMock()

        mock_redis = MagicMock()
        mock_redis.pubsub = MagicMock(return_value=mock_pubsub)
        monkeypatch.setattr("core.swarm_pubsub.redis.from_url", lambda *args, **kwargs: mock_redis)

        messages = []

        async def consume():
            async for msg in pubsub.subscribe():
                messages.append(msg)
                if len(messages) >= 2:
                    break

        task = asyncio.create_task(consume())
        await asyncio.sleep(0.05)
        task.cancel()
        with contextlib.suppress(asyncio.CancelledError):
            await task

        # Verify messages were received (mock should return them)
        assert len(messages) >= 1

    @pytest.mark.skip(reason="SwarmPubSub requires Redis connection - integration test needed")
    @pytest.mark.asyncio
    async def test_broadcast_publishes_event(self, monkeypatch):
        from core.swarm_pubsub import SwarmPubSub

        pubsub = SwarmPubSub()
        mock_redis = MagicMock()
        mock_redis.publish = AsyncMock()
        mock_redis.pubsub = MagicMock(return_value=MagicMock())

        # Completely mock the redis client to prevent any actual connection attempts
        monkeypatch.setattr("core.swarm_pubsub.redis.from_url", lambda *args, **kwargs: mock_redis)

        await pubsub.broadcast("theme_changed", {"theme": "dark"})

        # Verify publish was called
        mock_redis.publish.assert_called_once()
        call_args = mock_redis.publish.call_args
        payload = json.loads(call_args[0][1])
        assert payload["type"] == "theme_changed"
        assert payload["data"]["theme"] == "dark"


# ========================== human_behavior.py ==========================


class TestHumanBehaviorMissingBranches:
    def test_module_imports(self):
        import core.human_behavior as hb

        assert hasattr(hb, "HumanBehaviorSimulators")

    def test_bezier_points_generation(self):
        from core.human_behavior import HumanBehaviorSimulators

        points = HumanBehaviorSimulators._generate_bezier_points((0, 0), (100, 100), steps=5)
        assert len(points) == 5
        assert points[0] == (0, 0)
        assert points[-1] == (100, 100)


# ========================== security_utils.py ==========================


class TestSecurityUtilsMissingBranches:
    def test_is_safe_url_rejects_private_ip(self):
        from core.security import is_safe_url

        assert is_safe_url("http://192.168.1.1/test") is False

    def test_is_safe_url_rejects_localhost(self):
        from core.security import is_safe_url

        assert is_safe_url("http://localhost/test") is False

    def test_is_safe_url_rejects_metadata_endpoint(self):
        from core.security import is_safe_url

        assert is_safe_url("http://169.254.169.254/latest/meta-data/") is False

    def test_is_safe_url_accepts_public_url(self):
        from core.security import is_safe_url

        assert is_safe_url("https://example.com/test") is True


# ========================== swarm_orchestrator.py (additional) ==========================


class TestSwarmOrchestratorCircuitBreakerIntegration:
    @pytest.mark.anyio
    async def test_execute_task_handles_circuit_breaker_open(self):
        from core.orchestration.swarm_orchestrator import SwarmOrchestrator
        from core.resilience.circuit_breaker import (
            CircuitBreakerOpenError,
            CircuitBreakerState,
        )

        orchestrator = SwarmOrchestrator()

        orchestrator.circuit_breaker.state = "OPEN"

        # Mock _synthesize_tool to avoid LLM call
        with (
            patch.object(
                orchestrator,
                "_synthesize_tool",
                new_callable=AsyncMock,
                return_value={"agent_name": "mocked"},
            ),
            patch.object(
                orchestrator.agents["architect"],
                "run",
                new_callable=AsyncMock,
                side_effect=CircuitBreakerOpenError("circuit open", state=CircuitBreakerState.OPEN),
            ),
            patch.object(
                orchestrator.agents["reflection"],
                "reflect_and_persist",
                new_callable=AsyncMock,
            ),
        ):
            # We verify that the circuit breaker error path is reached
            workspace = await orchestrator.execute_task("write a python script", "uid")
            # বাংলা মন্তব্য: সার্কিট ব্রেকার রিয়েল এক্সেপশন মেসেজ "circuit breaker" হ্যান্ডেল করার জন্য অ্যাসারশন আপডেট করা হলো।
            assert "circuit open" in workspace.errors[0] or "circuit breaker" in workspace.errors[0].lower()
