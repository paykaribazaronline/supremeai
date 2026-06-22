"""
test_free_tier_tracker.py
=========================
Unit tests for the FreeTierTracker and TokenBudgetManager modules.

Tests cover:
- Rolling window RPM/TPM/RPD counting
- Provider availability checks
- Best-provider selection
- Rate-limit pausing
- Token estimation and prompt truncation
- Budget manager prepare_prompt
"""
from __future__ import annotations

import time

from core.free_tier_tracker import (
    FreeTierTracker,
    ProviderBudget,
    _Window,
    _DayWindow,
    get_tracker,
)
from core.token_budget import (
    TokenBudgetManager,
    estimate_tokens,
    truncate_to_token_limit,
    get_budget_manager,
)


# ===========================================================================
# _Window / _DayWindow unit tests
# ===========================================================================

class TestRollingWindow:
    def test_count_increments(self):
        w = _Window(window_seconds=60)
        w.add(token_count=100)
        w.add(token_count=200)
        assert w.count == 2

    def test_token_sum(self):
        w = _Window(window_seconds=60)
        w.add(token_count=100)
        w.add(token_count=300)
        assert w.token_sum == 400

    def test_eviction_old_entries(self, monkeypatch):
        """Entries older than window_seconds should be evicted."""
        w = _Window(window_seconds=1)
        w.timestamps.append(time.time() - 2)  # 2s old → should be evicted
        w.tokens.append(999)
        w.add(token_count=10)
        assert w.count == 1  # only the fresh one remains
        assert w.token_sum == 10


class TestDayWindow:
    def test_daily_count(self):
        dw = _DayWindow()
        dw.add()
        dw.add()
        assert dw.count == 2

    def test_seconds_until_reset(self):
        dw = _DayWindow()
        dw.add()
        secs = dw.seconds_until_oldest_expires()
        assert 86390 < secs <= 86400


# ===========================================================================
# ProviderBudget
# ===========================================================================

class TestProviderBudget:
    def _make_budget(self, rpm=10, tpm=1000, rpd=100) -> ProviderBudget:
        return ProviderBudget("test", {"rpm": rpm, "tpm": tpm, "rpd": rpd})

    def test_available_when_empty(self):
        b = self._make_budget()
        assert b.is_available() is True

    def test_unavailable_when_rpm_exceeded(self):
        b = self._make_budget(rpm=2)
        b.record(token_count=10)
        b.record(token_count=10)
        # 3rd should exceed rpm=2
        assert b.is_available() is False

    def test_unavailable_when_tpm_exceeded(self):
        b = self._make_budget(tpm=100)
        b.record(token_count=60)
        b.record(token_count=60)  # total 120 > 100
        assert b.is_available() is False

    def test_unavailable_when_rpd_exceeded(self):
        b = self._make_budget(rpd=2)
        b.record()
        b.record()
        assert b.is_available() is False

    def test_pause_blocks_requests(self):
        b = self._make_budget()
        b.pause(seconds=3600)
        assert b.is_available() is False

    def test_remaining_dict_structure(self):
        b = self._make_budget()
        r = b.remaining()
        assert "rpm_remaining" in r
        assert "tpm_remaining" in r
        assert "rpd_remaining" in r
        assert "available" in r


# ===========================================================================
# FreeTierTracker
# ===========================================================================

class TestFreeTierTracker:
    def _make_tracker(self) -> FreeTierTracker:
        # Use tiny limits for easy testing
        return FreeTierTracker(custom_limits={
            "alpha": {"rpm": 2, "tpm": 100, "rpd": 10},
            "beta":  {"rpm": 5, "tpm": 500, "rpd": 50},
            "gamma": {"rpm": 1, "tpm": 50,  "rpd": 5},
        })

    def test_record_and_availability(self):
        t = self._make_tracker()
        t.record("alpha", token_count=10)
        t.record("alpha", token_count=10)
        # alpha RPM=2, now used 2 — should be unavailable
        assert t.is_available("alpha") is False
        assert t.is_available("beta") is True

    def test_get_best_provider_skips_exhausted(self):
        t = self._make_tracker()
        # Exhaust alpha
        t.record("alpha")
        t.record("alpha")
        best = t.get_best_provider(candidates=["alpha", "beta"])
        assert best == "beta"

    def test_get_best_provider_returns_none_when_all_exhausted(self):
        t = self._make_tracker()
        # Exhaust all
        for _ in range(2):
            t.record("alpha")
        for _ in range(5):
            t.record("beta")
        t.record("gamma")
        best = t.get_best_provider(candidates=["alpha", "beta", "gamma"])
        assert best is None

    def test_mark_rate_limited_pauses_provider(self):
        t = self._make_tracker()
        assert t.is_available("alpha") is True
        t.mark_rate_limited("alpha", pause_seconds=3600)
        assert t.is_available("alpha") is False

    def test_get_status_has_all_providers(self):
        t = self._make_tracker()
        status = t.get_status()
        assert "providers" in status
        assert "alpha" in status["providers"]
        assert "beta" in status["providers"]

    def test_get_fallback_chain_excludes_failed(self):
        t = self._make_tracker()
        chain = t.get_fallback_chain("alpha", candidates=["alpha", "beta", "gamma"])
        assert "alpha" not in chain
        assert "beta" in chain

    def test_override_limits(self):
        t = self._make_tracker()
        t.override_limits("alpha", {"rpm": 100, "tpm": 10000, "rpd": 5000})
        # Now alpha should be available even after 2 requests
        t.record("alpha")
        t.record("alpha")
        assert t.is_available("alpha") is True

    def test_singleton_get_tracker(self):
        """get_tracker() should return the same singleton each time."""
        t1 = get_tracker()
        t2 = get_tracker()
        assert t1 is t2

    def test_unknown_provider_is_unavailable(self):
        t = self._make_tracker()
        assert t.is_available("nonexistent_provider") is False


# ===========================================================================
# Token estimation and truncation
# ===========================================================================

class TestTokenEstimation:
    def test_empty_string(self):
        assert estimate_tokens("") == 0

    def test_short_string(self):
        # 40 chars / 4 = 10 tokens
        text = "a" * 40
        assert estimate_tokens(text) == 10

    def test_code_ratio(self):
        code = "```python\ndef foo():\n    return 42\n```"
        tokens = estimate_tokens(code)
        assert tokens > 0

    def test_truncate_to_limit(self):
        long_text = "This is a sentence. " * 500  # ~10000 chars
        truncated = truncate_to_token_limit(long_text, max_tokens=100)
        assert estimate_tokens(truncated) <= 105  # small buffer for sentence boundary

    def test_no_truncation_when_within_limit(self):
        short_text = "Hello world."
        result = truncate_to_token_limit(short_text, max_tokens=1000)
        assert result == short_text


# ===========================================================================
# TokenBudgetManager
# ===========================================================================

class TestTokenBudgetManager:
    def _make_manager(self) -> TokenBudgetManager:
        return TokenBudgetManager(custom_budgets={
            "test_provider": {"max_input_tokens": 50, "max_output_tokens": 20},
            "large_provider": {"max_input_tokens": 200, "max_output_tokens": 100},
        })

    def test_prepare_prompt_no_truncation_needed(self):
        m = self._make_manager()
        short = "Hello"
        result, meta = m.prepare_prompt(short, provider="test_provider")
        assert meta["truncated"] is False
        assert result == short

    def test_prepare_prompt_truncates_long_prompt(self):
        m = self._make_manager()
        long_prompt = "word " * 500  # ~625 estimated tokens
        # reserve_output_tokens=False so max_input_tokens=200 is applied directly
        result, meta = m.prepare_prompt(long_prompt, provider="large_provider", reserve_output_tokens=False)
        assert meta["truncated"] is True
        assert estimate_tokens(result) <= 205  # within 200 + small sentence-boundary buffer

    def test_fits_in_budget_true_for_short(self):
        m = self._make_manager()
        assert m.fits_in_budget("short text", provider="test_provider") is True

    def test_fits_in_budget_false_for_long(self):
        m = self._make_manager()
        long_text = "x " * 1000
        assert m.fits_in_budget(long_text, provider="test_provider") is False

    def test_get_stats_returns_dict(self):
        m = self._make_manager()
        m.prepare_prompt("test", provider="test_provider")
        stats = m.get_stats()
        assert "test_provider" in stats
        assert stats["test_provider"]["total_calls"] == 1

    def test_estimate_method(self):
        m = self._make_manager()
        assert m.estimate("hello") > 0

    def test_singleton_get_budget_manager(self):
        m1 = get_budget_manager()
        m2 = get_budget_manager()
        assert m1 is m2
