# backend/tests/test_skill_graph.py
import pytest

pytest.importorskip("networkx")

from evolution.skill_graph import EvolutionSkillGraph
from core.orchestrator import Orchestrator

def test_type_compatibility():
    graph = EvolutionSkillGraph()
    assert graph.is_type_compatible("list", "json") is True
    assert graph.is_type_compatible("list", "str") is False
    assert graph.is_type_compatible("int", "float") is True
    assert graph.is_type_compatible("int", "str") is False
    assert graph.is_type_compatible("str", "int") is False  # Incompatible

def test_graph_chaining():
    graph = EvolutionSkillGraph()
    
    # Define Skill A (output: list)
    skill_a = {
        "inputs": [{"name": "in", "type": "str"}],
        "outputs": [{"name": "out", "type": "list"}]
    }
    # Define Skill B (input: json, output: str)
    skill_b = {
        "inputs": [{"name": "in", "type": "json"}],
        "outputs": [{"name": "out", "type": "str"}]
    }
    # Define Skill C (input: str, output: str)
    skill_c = {
        "inputs": [{"name": "in", "type": "str"}],
        "outputs": [{"name": "out", "type": "str"}]
    }

    graph.add_skill("Skill_A", skill_a)
    graph.add_skill("Skill_B", skill_b)
    graph.add_skill("Skill_C", skill_c)

    # Check if edges are created semantically
    # Skill_A (list) -> Skill_B (json) is compatible
    # Skill_B (str) -> Skill_C (str) is compatible
    assert graph.graph.has_edge("Skill_A", "Skill_B") is True
    assert graph.graph.has_edge("Skill_B", "Skill_C") is True

    # Check execution path
    path = graph.find_execution_path("Skill_A", "Skill_C")
    assert path == ["Skill_A", "Skill_B", "Skill_C"]

def test_orchestrator_integration():
    orchestrator = Orchestrator()
    
    skill_a = {
        "inputs": [{"name": "in", "type": "str"}],
        "outputs": [{"name": "out", "type": "list"}]
    }
    skill_b = {
        "inputs": [{"name": "in", "type": "json"}],
        "outputs": [{"name": "out", "type": "str"}],
        "fallback_skill": "Skill_A"
    }
    
    orchestrator.skill_graph.add_skill("Skill_A", skill_a)
    orchestrator.skill_graph.add_skill("Skill_B", skill_b)

    # Decompose intent within budget
    res = orchestrator.decompose_intent("Process and format data", "Skill_A", "Skill_B", max_token_cost=0.05)
    assert res["success"] is True
    assert res["execution_plan"] == ["Skill_A", "Skill_B"]

    # Decompose intent exceeding budget
    res_fail = orchestrator.decompose_intent("Process and format data", "Skill_A", "Skill_B", max_token_cost=0.01)
    assert res_fail["success"] is False
    assert "exceeds budget" in res_fail["error"]

@pytest.mark.anyio
async def test_fallback_and_weight_updates():
    orchestrator = Orchestrator()
    
    skill_a = {
        "inputs": [{"name": "in", "type": "str"}],
        "outputs": [{"name": "out", "type": "list"}]
    }
    skill_b = {
        "inputs": [{"name": "in", "type": "json"}],
        "outputs": [{"name": "out", "type": "str"}],
        "fallback_skill": "Skill_A"
    }

    orchestrator.skill_graph.add_skill("Skill_A", skill_a)
    orchestrator.skill_graph.add_skill("Skill_B", skill_b)

    # 1. Success execution chain
    result = await orchestrator.execute_skill_chain(["Skill_A", "Skill_B"], "raw-data")
    assert result["success"] is True
    
    # Weight should decrease on success (0.9 from 1.0)
    weight = orchestrator.skill_graph.graph["Skill_A"]["Skill_B"]["weight"]
    assert weight < 1.0

    # 2. Failure and Fallback Execution
    # We will trigger the failure by passing input data with trigger_failure flag
    # This will simulate exception on the second skill in chain.
    result_fail = await orchestrator.execute_skill_chain(["Skill_A", "Skill_B"], {"trigger_failure": True})
    assert result_fail["success"] is False
    assert result_fail["fallback_executed"] == "Skill_A"

    # 3. Dynamic Weight Penalty check
    orchestrator.skill_graph.update_edge_weight("Skill_A", "Skill_B", success=False)
    penalized_weight = orchestrator.skill_graph.graph["Skill_A"]["Skill_B"]["weight"]
    assert penalized_weight > weight
