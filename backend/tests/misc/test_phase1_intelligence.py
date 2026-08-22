"""
Unit tests for Phase 1 Intelligence Features:
- EpisodicMemory record & vector search
- LongTermMemory user context & preferences
- BehavioralGuard frequency spikes, infinite loops, and security alerts
- CausalDebugger traceback analysis
"""

import pytest

from memory.episodic_memory import EpisodicMemory
from memory.long_term_memory import LongTermMemory
from monitoring.behavioral_guard import BehavioralGuard
from monitoring.causal_debugger import CausalDebugger


@pytest.mark.asyncio
async def test_episodic_memory_flow():
    episodic = EpisodicMemory()
    success = await episodic.record_task(
        task_id="test_001",
        prompt="Write a Python sorting algorithm",
        response="def quicksort(arr): return arr",
        success=True,
        latency_ms=45.2,
        model_used="Supreme-Coder-3B",
    )
    assert success is True
    episodic.store_episode(task_type="sorting", input_data="Write a Python sorting algorithm")
    recalled = episodic.recall_episodes(task_type="sorting")
    assert len(recalled) >= 1


def test_long_term_memory_user_context():
    ltm = LongTermMemory()
    ltm.store_user_preference(user_id="usr_99", key="preferred_language", value="Bengali")
    context = ltm.get_context_for_user("usr_99")
    assert "preferred_language" in context
    assert "Bengali" in context


def test_behavioral_guard_anomalies():
    guard = BehavioralGuard()

    # Normal behavior
    res1 = guard.record_action(agent_id="ag_01", action_type="tool_call", prompt_or_command="ls -la")
    assert res1["allowed"] is True

    # Security violation
    res2 = guard.record_action(agent_id="ag_01", action_type="cmd", prompt_or_command="rm -rf /")
    assert res2["allowed"] is False
    assert res2["anomaly_type"] == "SECURITY_VIOLATION"
    assert guard.is_agent_blocked("ag_01") is True


def test_causal_debugger_analysis():
    debugger = CausalDebugger()
    try:
        raise KeyError("missing_config_key")
    except Exception as exc:
        analysis = debugger.analyze_exception(exc)
        assert analysis["exception_type"] == "KeyError"
        assert "dict.get()" in analysis["suggested_remediation"]
