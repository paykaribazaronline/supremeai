# backend/tests/evolution/test_canary_and_evolution_bridge.py
"""Tests for EvolutionBridge and CanaryRolloutController."""

import pytest
from evolution.canary_manager import CanaryRolloutController, get_canary_controller
from evolution.change_proposal import ChangeProposalManager, ChangeType, ProposalState
from learning.evolution_bridge import EvolutionBridge, get_evolution_bridge
from learning.experience import ExperienceRecord


def test_evolution_bridge_synthesizes_proposal_on_syntax_error():
    bridge = get_evolution_bridge()

    syntax_record = ExperienceRecord(
        task_id="task_syntax_err_99",
        goal="Generate Python class",
        verified=False,
        verification_score=0.3,
        failures=["Python Syntax Error: invalid syntax at line 2"],
    )

    proposal = bridge.process_experience_and_propose(syntax_record)

    assert proposal is not None
    assert proposal.change_type == ChangeType.PROMPT_OPTIMIZATION
    assert "task_syntax_err_99" in proposal.title
    assert proposal.state == ProposalState.DRAFTED


def test_canary_controller_rollout_and_promotion():
    proposal_mgr = ChangeProposalManager()
    canary_ctrl = CanaryRolloutController(proposal_manager=proposal_mgr)

    proposal = proposal_mgr.create_proposal(
        title="Cache hit rate enhancement",
        description="Tune TTL",
        change_type=ChangeType.PARAMETER_TUNING,
        diff_content={"ttl": 600},
        target_module="backend/core/cache.py",
        current_fitness=0.80,
    )

    # 1. Deploy canary
    deployed = canary_ctrl.deploy_canary(proposal.proposal_id, sample_ratio=0.20)
    assert deployed is True
    assert proposal.state == ProposalState.CANARY_ACTIVE

    # 2. Record 5 successful trials
    for _ in range(5):
        canary_ctrl.record_observation(proposal.proposal_id, success=True, latency_ms=50.0)

    # 3. Evaluate and promote
    promoted = canary_ctrl.evaluate_and_promote(proposal.proposal_id, min_trials=5, min_success_rate=0.80)
    assert promoted is True
    assert proposal.state == ProposalState.PROMOTED


def test_canary_controller_auto_rollback_on_regression():
    proposal_mgr = ChangeProposalManager()
    canary_ctrl = CanaryRolloutController(proposal_manager=proposal_mgr)

    proposal = proposal_mgr.create_proposal(
        title="Bad regression patch",
        description="Causes 500s",
        change_type=ChangeType.CODE_REFACTOR,
        diff_content={"bad_patch": True},
        target_module="backend/core/router.py",
        current_fitness=0.85,
    )

    canary_ctrl.deploy_canary(proposal.proposal_id)

    # 3 failed trials in a row should trigger auto rollback
    canary_ctrl.record_observation(proposal.proposal_id, success=False)
    canary_ctrl.record_observation(proposal.proposal_id, success=False)
    canary_ctrl.record_observation(proposal.proposal_id, success=False)

    assert proposal.state == ProposalState.ROLLED_BACK
    assert "High failure rate" in (proposal.rejection_reason or "")
