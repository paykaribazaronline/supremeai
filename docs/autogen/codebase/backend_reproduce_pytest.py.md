# 📄 ফাইল: backend/reproduce_pytest.py

**প্রকার:** .py  
**সাইজ:** 859 বাইট  
**আপডেট:** 2026-07-11T20:08:21.337271

---

## কোড

```py
import sys

from _pytest.monkeypatch import MonkeyPatch

# 1. Collection time: test_playwright_manager.py is imported
import core.playwright_manager as pm


module_instance_A = pm

# 2. test_core_missing_coverage.py runs
mp = MonkeyPatch()
mp.setitem(sys.modules, "playwright", None)
mp.setitem(sys.modules, "playwright.async_api", None)
mp.delitem(sys.modules, "core.playwright_manager", raising=False)

mp.undo()

# 3. test_playwright_manager.py runs
from unittest.mock import patch


print("pm is module_instance_A:", pm is module_instance_A)
print("sys.modules['core.playwright_manager'] is module_instance_A:", sys.modules["core.playwright_manager"] is module_instance_A)
patcher = patch("core.playwright_manager.async_playwright", return_value=123)
patcher.start()
print("pm.async_playwright is callable:", callable(pm.async_playwright))
patcher.stop()

```