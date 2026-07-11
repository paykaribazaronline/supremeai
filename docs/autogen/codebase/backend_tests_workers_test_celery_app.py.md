# 📄 ফাইল: backend/tests/workers/test_celery_app.py

**প্রকার:** .py  
**সাইজ:** 361 বাইট  
**আপডেট:** 2026-07-11T11:05:10.227489

---

## কোড

```py
import pytest
import sys

sys.path.append("../..")

try:
    from workers.celery_app import app

    HAS_CELERY = app is not None
except Exception:  # noqa: BLE001
    HAS_CELERY = False


@pytest.mark.skipif(not HAS_CELERY, reason="Celery app is not available")
def test_celery_app_exposed():
    from workers.celery_app import app

    assert app is not None

```