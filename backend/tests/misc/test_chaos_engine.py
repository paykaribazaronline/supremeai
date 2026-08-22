import pytest

from core.resilience.chaos_engine import ChaosEngine


@pytest.mark.anyio
async def test_inject_fault_disabled(monkeypatch):
    monkeypatch.setenv("ENABLE_CHAOS_MODE", "false")
    engine = ChaosEngine()
    await engine.inject_fault()  # should not raise


@pytest.mark.anyio
async def test_inject_fault_noop_when_random_high(monkeypatch):
    monkeypatch.setenv("ENABLE_CHAOS_MODE", "true")
    engine = ChaosEngine()

    monkeypatch.setattr("core.resilience.chaos_engine.random.random", lambda: 0.99)
    await engine.inject_fault()


@pytest.mark.anyio
async def test_inject_fault_timeout(monkeypatch):
    monkeypatch.setenv("ENABLE_CHAOS_MODE", "true")
    engine = ChaosEngine()

    monkeypatch.setattr("core.resilience.chaos_engine.random.random", lambda: 0.01)
    monkeypatch.setattr(
        "core.resilience.chaos_engine.random.choice",
        lambda seq: "timeout",
    )

    with pytest.raises(TimeoutError):
        await engine.inject_fault()


@pytest.mark.anyio
async def test_inject_fault_llm_down(monkeypatch):
    monkeypatch.setenv("ENABLE_CHAOS_MODE", "true")
    engine = ChaosEngine()

    monkeypatch.setattr("core.resilience.chaos_engine.random.random", lambda: 0.01)
    monkeypatch.setattr(
        "core.resilience.chaos_engine.random.choice",
        lambda seq: "llm_down",
    )

    with pytest.raises(ConnectionError):
        await engine.inject_fault()
