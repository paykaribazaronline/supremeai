"""Tests for core files that had 0% coverage.

Targets: user_profiler, decision_engine, auto_healer_service,
log_batcher, self_updater.
"""

import pytest

# ── user_profiler ──────────────────────────────────────────────────────────────


class TestUserProfiler:
    def test_user_mode_enum(self):
        from core.user_profiler import UserMode

        assert UserMode.FAST_TRACK == "FAST_TRACK"
        assert UserMode.LEARNING == "LEARNING"
        assert UserMode.PRODUCTION == "PRODUCTION"

    def test_user_profile_dataclass(self):
        from core.user_profiler import UserMode, UserProfile

        profile = UserProfile(user_id="test-123", mode=UserMode.FAST_TRACK)
        assert profile.user_id == "test-123"
        assert profile.mode == UserMode.FAST_TRACK
        assert profile.goals == []
        assert profile.preferences == {}

    def test_user_profile_default_mode(self):
        from core.user_profiler import UserMode, UserProfile

        profile = UserProfile(user_id="test-456")
        assert profile.mode == UserMode.FAST_TRACK

    @pytest.mark.asyncio
    async def test_classify_user(self):
        from core.user_profiler import UserProfiler

        profiler = UserProfiler()
        profile = await profiler.classify_user("test-user")
        assert profile.user_id == "test-user"
        assert profile.mode is not None

    @pytest.mark.asyncio
    async def test_update_from_history(self):
        from core.user_profiler import UserProfiler

        profiler = UserProfiler()
        # Should not raise
        await profiler.update_from_history("test-user", {"task": "test"})

    def test_modes_list(self):
        from core.user_profiler import UserProfiler

        assert "FAST_TRACK" in UserProfiler.MODES
        assert "LEARNING" in UserProfiler.MODES
        assert "PRODUCTION" in UserProfiler.MODES


# ── decision_engine ────────────────────────────────────────────────────────────


class TestDecisionEngine:
    def test_engine_import(self):
        from core.decision_engine import DecisionEngine

        engine = DecisionEngine()
        assert engine is not None

    def test_evaluate_action(self):
        from core.decision_engine import DecisionEngine

        engine = DecisionEngine()
        result = engine.evaluate(action="read", user_id="user-1")
        assert result is not None

    def test_evaluate_with_context(self):
        from core.decision_engine import DecisionEngine

        engine = DecisionEngine()
        result = engine.evaluate(
            action="delete", user_id="user-1", context={"risk": "high"}
        )
        assert result is not None


# ── auto_healer_service ────────────────────────────────────────────────────────


class TestAutoHealerService:
    def test_service_initialization(self):
        from core.auto_healer_service import AutoHealerService

        service = AutoHealerService()
        assert service is not None

    def test_start_stop(self):
        from core.auto_healer_service import AutoHealerService

        service = AutoHealerService()
        service.start()
        service.stop()

    def test_heal_all(self):
        from core.auto_healer_service import AutoHealerService

        service = AutoHealerService()
        result = service.heal_all()
        assert result is not None


# ── log_batcher ────────────────────────────────────────────────────────────────


class TestLogBatcher:
    def test_batcher_import(self):
        from core.observability.log_batcher import LogBatcherService

        batcher = LogBatcherService()
        assert batcher is not None

    def test_batcher_add_log(self):
        from core.observability.log_batcher import LogBatcherService

        batcher = LogBatcherService()
        batcher.add_log({"event": "test", "level": "info"})
        assert len(batcher._buffer) == 1

    def test_batcher_flush(self):
        from core.observability.log_batcher import LogBatcherService

        batcher = LogBatcherService()
        batcher.add_log({"event": "flush-test"})
        flushed = batcher.flush()
        assert len(flushed) >= 1
        assert len(batcher._buffer) == 0


# ── self_updater ────────────────────────────────────────────────────────────────


class TestSelfUpdater:
    def test_updater_initialization(self):
        from core.evolution.self_updater import SelfUpdater

        updater = SelfUpdater()
        assert updater is not None

    def test_apply_hotfix(self):
        import os
        import tempfile

        from core.evolution.self_updater import SelfUpdater

        updater = SelfUpdater()
        with tempfile.NamedTemporaryFile(mode="w", suffix=".py", delete=False) as f:
            f.write("# existing code\nx = 1\n")
            target_path = f.name

        try:
            result = updater.apply_hotfix(target_path, "x = 2")
            assert result is True
        finally:
            os.unlink(target_path)

    def test_apply_hotfix_invalid_path(self):
        from core.evolution.self_updater import SelfUpdater

        updater = SelfUpdater()
        result = updater.apply_hotfix("/etc/passwd", "malicious")
        assert result is False
