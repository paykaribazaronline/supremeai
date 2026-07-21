# Import guard: some test collections fail if optional deps are missing.
# Keep this local to this test module.
import sys
from unittest.mock import MagicMock

import pytest

if "nats" not in sys.modules:
    sys.modules["nats"] = MagicMock()
    sys.modules["nats.errors"] = MagicMock()
    sys.modules["nats.js"] = MagicMock()
    sys.modules["nats.js.errors"] = MagicMock()

from core.orchestration.orchestrator import Orchestrator


@pytest.mark.anyio
async def test_decompose_intent_estimated_cost_exceeds_budget():
    orch = Orchestrator(interval_seconds=300)

    orch.skill_graph.find_execution_path = lambda a, b: ["a", "b", "c", "d", "e"]

    res = orch.decompose_intent(
        prompt="do something",
        start_skill="s",
        end_skill="e",
        max_token_cost=0.02,
    )

    assert res["success"] is False
    assert "exceeds budget" in res["error"].lower()


@pytest.mark.anyio
async def test_execute_skill_chain_success_updates_output_and_chain():
    orch = Orchestrator(interval_seconds=300)

    orch.skill_graph.get_fallback = lambda skill: None
    orch.skill_graph.update_edge_weight = lambda *args, **kwargs: None

    res = await orch.execute_skill_chain(["Skill_A", "Skill_B"], input_data={"x": 1})

    assert res["success"] is True
    assert res["executed_chain"] == ["Skill_A", "Skill_B"]
    assert res["output"]["processed_by"] == "Skill_B"


@pytest.mark.anyio
async def test_execute_skill_chain_failure_uses_fallback():
    orch = Orchestrator(interval_seconds=300)

    orch.skill_graph.update_edge_weight = lambda *args, **kwargs: None
    orch.skill_graph.get_fallback = lambda skill: (
        "Compensate_X" if skill == "Skill_B" else None
    )

    # Force the internal simulated failure condition for Skill_B
    res = await orch.execute_skill_chain(
        ["Skill_A", "Skill_B"],
        input_data={"data": {"trigger_failure": True}},
    )

    assert res["success"] is False
    assert res["fallback_executed"] == "Compensate_X"
    assert "last_successful_state" in res
