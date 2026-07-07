# 📄 ফাইল: scratch/smoke_check.py

**প্রকার:** .py  
**সাইজ:** 474 বাইট  
**আপডেট:** 2026-07-07T16:46:48.481457

---

## কোড

```py
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BACKEND = ROOT / 'backend'
sys.path.insert(0, str(BACKEND))
os.environ.setdefault('SUPABASE_DATABASE_URL_POOLER', 'sqlite+aiosqlite:///:memory:')

from fastapi.testclient import TestClient
from core.app import app

client = TestClient(app)
for path in ['/health', '/actuator/health', '/openapi.json']:
    response = client.get(path, timeout=30)
    print(path, response.status_code)

```