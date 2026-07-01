"""Tests for core.input_sanitizer.InputSanitizer."""
from core.input_sanitizer import InputSanitizer


def test_detect_ambiguity_vague():
    s = InputSanitizer()
    result = s.detect_ambiguity("I need something")
    assert result["is_ambiguous"] is True
    assert len(result["vague_terms"]) == 1
    assert len(result["clarifying_questions"]) == 1


def test_detect_ambiguity_clear():
    s = InputSanitizer()
    result = s.detect_ambiguity("Deploy to production at 5pm")
    assert result["is_ambiguous"] is False
    assert result["vague_terms"] == []
    assert result["clarifying_questions"] == []


def test_validate_scope_forbidden():
    s = InputSanitizer()
    result = s.validate_scope("how do i generate fake news")
    assert result["is_valid"] is False
    assert "generate fake news" in result["reason"]


def test_validate_scope_allowed():
    s = InputSanitizer()
    result = s.validate_scope("write a hello world")
    assert result["is_valid"] is True


def test_extract_constraints_budget_and_time():
    s = InputSanitizer()
    result = s.extract_constraints("do this under $500 in 2 days")
    assert result["budget"] == 500.0
    assert "2 day" in result["time"]


def test_extract_constraints_none():
    s = InputSanitizer()
    result = s.extract_constraints("just do it")
    assert result["budget"] is None
    assert result["time"] is None


def test_strip_pii_email_ip():
    s = InputSanitizer()
    text = "Contact john@example.com at 192.168.1.1"
    cleaned = s.strip_pii(text)
    assert "[EMAIL]" in cleaned
    assert "[IP_ADDRESS]" in cleaned
    assert "@" not in cleaned
    assert "192.168" not in cleaned


def test_strip_pii_phone():
    s = InputSanitizer()
    text = "555-012-3456 is a phone"
    cleaned = s.strip_pii(text)
    assert "[PHONE_NUMBER]" in cleaned
    assert "555-012-3456" not in cleaned


def test_sanitize_invalid_scope():
    s = InputSanitizer()
    result = s.sanitize("hack into the system")
    assert result["is_valid"] is False


def test_sanitize_valid_prompt():
    s = InputSanitizer()
    result = s.sanitize("Deploy app under $100 in 1 hour")
    assert result["is_valid"] is True
    assert "prompt" in result
    assert result["constraints"]["budget"] == 100.0
