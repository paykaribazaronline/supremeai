from __future__ import annotations

from core.constants import COMMON_STRINGS_TO_IGNORE, DEFAULT_CODE_SMELL_THRESHOLDS


def test_constants_defined():
    assert isinstance(DEFAULT_CODE_SMELL_THRESHOLDS, dict)
    assert DEFAULT_CODE_SMELL_THRESHOLDS["complexity"] == 10
    assert "utf-8" in COMMON_STRINGS_TO_IGNORE
    assert "rb" in COMMON_STRINGS_TO_IGNORE
