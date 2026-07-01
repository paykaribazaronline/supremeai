"""Tests for core.output_validator output validation classes."""
from core.output_validator import (
    MultiAICodeGenerator,
    EnhancedConfidenceScorer,
    HumanReviewPolicy,
    OutputValidator,
)


def test_multi_ai_code_generator_consensus_found():
    gen = MultiAICodeGenerator()
    result = gen.generate_with_consensus(
        "task",
        "line1\nline2\ncommon",
        "line3\ncommon",
        "common\nline4",
    )
    assert "common" in result["code"]
    assert 0 <= result["confidence"] <= 1.0


def test_multi_ai_code_generator_no_consensus():
    gen = MultiAICodeGenerator()
    result = gen.generate_with_consensus("task", "a\nb", "c\nd", "e\nf")
    assert result["code"] == "a\nb"


def test_enhanced_confidence_scorer_high():
    scorer = EnhancedConfidenceScorer()
    result = scorer.score("clean output", {"ai_reliability": 0.95, "external_score": 1.0})
    assert result["badge"] == "HIGH_CONFIDENCE"
    assert result["color"] == "green"


def test_enhanced_confidence_scorer_low_hallucination():
    scorer = EnhancedConfidenceScorer()
    result = scorer.score("see nadim9/supremeai", {"ai_reliability": 0.95, "external_score": 1.0})
    assert result["badge"] == "LOW_CONFIDENCE"
    assert result["should_warn"] is True


def test_human_review_policy_requires_for_code():
    policy = HumanReviewPolicy()
    assert policy.requires_human_review("python_code", {"overall": 0.95, "ai_reliability": 1.0}) is True


def test_human_review_policy_low_confidence():
    policy = HumanReviewPolicy()
    assert policy.requires_human_review("chat", {"overall": 0.6, "ai_reliability": 1.0}) is True


def test_human_review_policy_skip():
    policy = HumanReviewPolicy()
    assert policy.requires_human_review("chat", {"overall": 0.95, "ai_reliability": 1.0}) is False


def test_output_validator_validate_clean():
    validator = OutputValidator()
    result = validator.validate("some safe output")
    assert result["is_valid"] is True


def test_output_validator_validate_hallucination():
    validator = OutputValidator()
    result = validator.validate("check nadim9/supremeai")
    assert result["is_valid"] is False
    reflection = validator.self_reflect("check nadim9/supremeai")
    assert reflection["has_issues"] is True
    assert any("Hallucinated repo path" in issue for issue in reflection["issues"])
