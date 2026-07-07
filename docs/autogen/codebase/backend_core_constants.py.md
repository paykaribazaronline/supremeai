# 📄 ফাইল: backend/core/constants.py

**প্রকার:** .py  
**সাইজ:** 377 বাইট  
**আপডেট:** 2026-07-07T18:37:32.307145

---

## কোড

```py
"""
Refactored constants using DynamicConfigProxy
"""

from core.config_proxy import DynamicConfigProxy


async def get_default_code_smell_thresholds(proxy: DynamicConfigProxy) -> dict:
    return await proxy.get("DEFAULT_CODE_SMELL_THRESHOLDS")

async def get_common_strings_to_ignore(proxy: DynamicConfigProxy) -> list:
    return await proxy.get("COMMON_STRINGS_TO_IGNORE")

```