# backend/tests/services/test_task_and_evolution_governance.py
"""Tests for TaskContract and ChangeProposal Governance (Audit P0 implementations)."""

import pytest
from core.task_contract import RiskLevel, TaskContract, TaskStatus, VerificationPolicy
from evolution.change_proposal import (
    ChangeProposalManager,
    ChangeType,
    ProposalState,
    get_change_manager,
)


def test_task_contract_lifecycle():
    task = TaskContract(
        goal="Refactor authentication handler",
        risk_level=RiskLevel.MEDIUM,
        verification_policy=VerificationPolicy.STRICT,
        required_capabilities=["python", "ast_analysis"],
    )

    assert task.status == TaskStatus.PENDING
    task.mark_in_progress(TaskStatus.PLANNING)
    assert task.status == TaskStatus.PLANNING
    assert len(task.execution_history) == 1

    task.mark_in_progress(TaskStatus.EXECUTING)
    assert task.status == TaskStatus.EXECUTING

    task.complete(result="Refactoring complete", confidence=0.98)
    assert task.status == TaskStatus.COMPLETED
    assert task.confidence == 0.98
    assert task.completed_at is not None

    data = task.to_dict()
    assert data["goal"] == "Refactor authentication handler"
    assert data["status"] == "completed"


@pytest.mark.asyncio
async def test_change_proposal_lifecycle_and_promotion():
    manager = get_change_manager()

    proposal = manager.create_proposal(
        title="Optimize temperature parameter",
        description="Decrease temperature to 0.2 for deterministic code generation",
        change_type=ChangeType.PARAMETER_TUNING,
        diff_content={"temperature": 0.2},
        target_module="backend/brain/model_router.py",
        current_fitness=0.82,
    )

    assert proposal.state == ProposalState.DRAFTED

    # Mock security scanner & benchmark callbacks
    async def mock_security_ok(prop):
        return True

    async def mock_benchmark_improved(prop):
        return 0.89  # Improved from 0.82

    promoted = await manager.evaluate_and_promote(
        proposal_id=proposal.proposal_id,
        security_scanner_cb=mock_security_ok,
        benchmarker_cb=mock_benchmark_improved,
    )

    assert promoted is True
    assert proposal.state == ProposalState.PROMOTED
    assert proposal.fitness_after == 0.89


@pytest.mark.asyncio
async def test_change_proposal_rejection_on_regression():
    manager = get_change_manager()

    proposal = manager.create_proposal(
        title="Flawed heuristic patch",
        description="Overly aggressive pruning",
        change_type=ChangeType.ROUTING_POLICY,
        diff_content={"prune_threshold": 0.99},
        target_module="backend/core/router.py",
        current_fitness=0.85,
    )

    async def mock_benchmark_regressed(prop):
        return 0.70  # Regressed!

    promoted = await manager.evaluate_and_promote(
        proposal_id=proposal.proposal_id,
        benchmarker_cb=mock_benchmark_regressed,
    )

    assert promoted is False
    assert proposal.state == ProposalState.REJECTED
    assert "Fitness regression" in proposal.rejection_reason
