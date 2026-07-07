# 📄 ফাইল: backend/core/constants.py

**প্রকার:** .py  
**সাইজ:** 382 বাইট  
**আপডেট:** 2026-07-07T12:28:01.405058

---

## কোড

```py
"""
Project-wide constants to promote maintainability and DRY principles.
"""

# Code Smell Detector default thresholds
DEFAULT_CODE_SMELL_THRESHOLDS = {
    "complexity": 10,
    "lines": 75,
    "args": 5,
    "class_methods": 15,
}

# Common strings to ignore when detecting "magic strings"
COMMON_STRINGS_TO_IGNORE = {"", "utf-8", "rb", "wb", "r", "w", "a", "x", "b", "t", "+"}

```