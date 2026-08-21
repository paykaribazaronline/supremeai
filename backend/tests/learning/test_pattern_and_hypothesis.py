# backend/tests/learning/test_pattern_and_hypothesis.py
"""Tests for PatternDetector and HypothesisEngine."""

import pytest
from evolution.change_proposal import ChangeProposalManager, ChangeType, ProposalState
from learning.experience import ExperienceRecord, ExperienceStore
from learning.hypothesis_engine import HypothesisEngine, ImprovementHypothesis
from learning.pattern_detector import PatternDetector


def test_pattern_detector_identifies_syntax_failures():
    store = ExperienceStore()
    store.record(ExperienceRecord(task_id="t1", goal="write code", verified=False, failures=["AST Syntax Error at line 12"]))
    store.record(ExperienceRecord(task_id="t2", goal="write code", verified=False, failures=["Invalid syntax token: EOF"]))

    detector = PatternDetector(store=store)
    patterns = detector.analyze_patterns(min_support=2)

    assert len(patterns) >= 1
    assert patterns[0].pattern_type == "syntax_error_pattern"
    assert patterns[0].occurrence_count == 2
    assert "t1" in patterns[0].evidence_task_ids
    assert "t2" in patterns[0].evidence_task_ids


def test_hypothesis_engine_generates_and_converts_proposal():
    store = ExperienceStore()
    store.record(ExperienceRecord(task_id="b1", goal="multi-agent query", verified=False, failures=["BudgetExceededError: max token cost exceeded"]))
    store.record(ExperienceRecord(task_id="b2", goal="multi-agent query", verified=False, failures=["Token budget exhausted"]))

    detector = PatternDetector(store=store)
    proposal_mgr = ChangeProposalManager()
    engine = HypothesisEngine(detector=detector, proposal_manager=proposal_mgr)

    hypotheses = engine.generate_hypotheses()
    assert len(hypotheses) == 1
    assert hypotheses[0].category == "PARAMETER_TUNING"
    assert hypotheses[0].expected_gain > 0

    proposal = engine.convert_hypothesis_to_proposal(hypotheses[0])
    assert proposal is not None
    assert proposal.change_type == ChangeType.PARAMETER_TUNING
    assert proposal.state == ProposalState.DRAFTED
    assert proposal.target_module == "backend/runtime/budget_guard.py"
