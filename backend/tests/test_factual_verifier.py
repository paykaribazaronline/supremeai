import pytest

from core.factual_verifier import FactualVerifier
from core.factual_verifier import _safe_eval_math


def test_safe_eval_math_basic():
    assert _safe_eval_math("2 + 3") == 5
    assert _safe_eval_math("10 - 2") == 8
    assert _safe_eval_math("3 * 4") == 12
    assert _safe_eval_math("10 / 2") == 5.0
    assert _safe_eval_math("2 ** 3") == 8
    assert _safe_eval_math("10 % 3") == 1
    assert _safe_eval_math("10 // 3") == 3
    assert _safe_eval_math("-5") == -5
    assert _safe_eval_math("+3") == 3


def test_safe_eval_math_invalid():
    with pytest.raises(SyntaxError):
        _safe_eval_math("import os")
    with pytest.raises(ValueError):
        _safe_eval_math("__import__('os')")
    with pytest.raises(ValueError):
        _safe_eval_math("2 + unknown_var")


def test_verify_with_local_rag_no_rag(monkeypatch):
    verifier = FactualVerifier()
    verifier.local_rag = None
    result = verifier.verify_with_local_rag("test claim")
    assert result["is_verified"] is True
    assert result["method"] == "no_local_rag"


def test_verify_with_local_rag_with_matches():
    verifier = FactualVerifier()
    mock_rag = type(
        "MockRAG",
        (),
        {"semantic_search": lambda self, q: {"matches": [{"title": "Doc1"}, {"title": "Doc2"}]}},
    )()
    verifier.local_rag = mock_rag
    result = verifier.verify_with_local_rag("test claim")
    assert result["is_verified"] is True
    assert result["confidence"] == 0.6
    assert "Doc1" in result["supporting_sources"]


def test_verify_with_local_rag_no_matches():
    verifier = FactualVerifier()
    mock_rag = type("MockRAG", (), {"semantic_search": lambda self, q: {"matches": []}})()
    verifier.local_rag = mock_rag
    result = verifier.verify_with_local_rag("test claim")
    assert result["is_verified"] is True
    assert result["method"] == "no_matches"
    assert result["confidence"] == 0.3


def test_verify_math_correct():
    verifier = FactualVerifier()
    result = verifier.verify_math("2 + 2", "4")
    assert result["is_verified"] is True


def test_verify_math_incorrect():
    verifier = FactualVerifier()
    result = verifier.verify_math("2 + 2", "5")
    assert result["is_verified"] is False


def test_verify_simple_math_true():
    verifier = FactualVerifier()
    result = verifier.verify("2+2=4")
    assert result["is_verified"] is True


def test_verify_simple_math_false():
    verifier = FactualVerifier()
    result = verifier.verify("2+2=5")
    assert result["is_verified"] is False
    assert "Math error" in result["reason"]
