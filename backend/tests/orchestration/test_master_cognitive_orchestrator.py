# backend/tests/orchestration/test_master_cognitive_orchestrator.py
"""Tests for SupremeAI Master Cognitive Orchestrator."""

import pytest

from core.orchestration.master_cognitive_orchestrator import (
    CognitiveIntent,
    MasterCognitiveOrchestrator,
    get_master_orchestrator,
)


@pytest.mark.asyncio
async def test_self_healing_pipeline_authorized():
    orchestrator = get_master_orchestrator()
    payload = {
        "error": "TimeoutError in step execution",
        "target_file": "adapters/task_executor.py",
        "discovery_query": "asyncio timeout resilience",
    }
    result = await orchestrator.dispatch(CognitiveIntent.REPAIR, payload)
    assert result.status == "SUCCESS"
    assert "01_diagnostic_incident_replay" in result.stages_completed
    assert "04_solution_synthesis_sandbox" in result.stages_completed
    assert "05_governance_policy_authorization" in result.stages_completed
    assert result.confidence >= 0.90


@pytest.mark.asyncio
async def test_self_healing_pipeline_governance_blocked():
    orchestrator = get_master_orchestrator()
    # Protected target: core/security
    payload = {
        "error": "Auth token bypass attempt",
        "target_file": "core/security/auth_guard.py",
    }
    result = await orchestrator.dispatch(CognitiveIntent.REPAIR, payload)
    assert result.status == "BLOCKED"
    assert "Governance policy blocked repair" in result.summary
    assert result.confidence == 0.0


@pytest.mark.asyncio
async def test_deep_synthesis_pipeline():
    orchestrator = get_master_orchestrator()
    payload = {
        "demand": "Design high-performance distributed token bucket rate limiter",
    }
    result = await orchestrator.dispatch(CognitiveIntent.FEATURE_SYNTHESIS, payload)
    assert result.status == "SUCCESS"
    assert "01_project_dna_fingerprint" in result.stages_completed
    assert "02_multi_model_knowledge_squeezer" in result.stages_completed
    assert "05_eternal_memory_ingestion" in result.stages_completed
    assert result.confidence >= 0.90


@pytest.mark.asyncio
async def test_autonomous_audit_pipeline():
    orchestrator = get_master_orchestrator()
    result = await orchestrator.dispatch(CognitiveIntent.AUDIT_RADAR, {})
    assert result.status == "SUCCESS"
    assert "01_universal_gap_finder_scan" in result.stages_completed
    assert "03_memory_revaluation" in result.stages_completed


@pytest.mark.asyncio
async def test_governed_evolution_pipeline_success_and_rejection():
    orchestrator = get_master_orchestrator()

    # Allowed target
    ok_res = await orchestrator.dispatch(
        CognitiveIntent.EVOLUTION,
        {"target_module": "skills/custom_math.py"},
    )
    assert ok_res.status == "SUCCESS"

    # Protected target
    blocked_res = await orchestrator.dispatch(
        CognitiveIntent.EVOLUTION,
        {"target_module": "billing/stripe_sync.py"},
    )
    assert blocked_res.status == "REJECTED"
    assert "blocked by governance policy" in blocked_res.summary
