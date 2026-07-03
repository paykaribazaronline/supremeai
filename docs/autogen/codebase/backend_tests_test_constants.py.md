# 📄 ফাইল: backend/tests/test_constants.py

**প্রকার:** .py  
**সাইজ:** 362 বাইট  
**আপডেট:** 2026-07-03T21:37:07.689362

---

## কোড

```py
from __future__ import annotations

from core.constants import COMMON_STRINGS_TO_IGNORE, DEFAULT_CODE_SMELL_THRESHOLDS


def test_constants_defined():
    assert isinstance(DEFAULT_CODE_SMELL_THRESHOLDS, dict)
    assert DEFAULT_CODE_SMELL_THRESHOLDS["complexity"] == 10
    assert "utf-8" in COMMON_STRINGS_TO_IGNORE
    assert "rb" in COMMON_STRINGS_TO_IGNORE

```