# 📄 ফাইল: scratch/verify_project_health.py

**প্রকার:** .py  
**সাইজ:** 1,130 বাইট  
**আপডেট:** 2026-07-07T06:42:45.594276

---

## কোড

```py
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BACKEND = ROOT / 'backend'
FRONTEND = ROOT / 'apps' / 'studio-client'

os.environ.setdefault('SUPABASE_DATABASE_URL_POOLER', 'sqlite+aiosqlite:///:memory:')
sys.path.insert(0, str(BACKEND))
os.environ['PYTHONPATH'] = str(BACKEND) + os.pathsep + os.environ.get('PYTHONPATH', '')

from fastapi.testclient import TestClient
from core.app import app

client = TestClient(app)
print('=== FastAPI smoke checks ===')
for path in ['/health', '/actuator/health', '/docs', '/openapi.json']:
    try:
        response = client.get(path, timeout=30)
        print(path, response.status_code)
        print(response.text[:400].replace('\n', ' ')[:400])
    except Exception as exc:
        print(path, 'ERROR', exc)

print('\n=== Frontend production build ===')
result = subprocess.run(
    ['npm', 'run', 'build'],
    cwd=FRONTEND,
    capture_output=True,
    text=True,
    timeout=1800,
)
print('exit_code=', result.returncode)
if result.stdout:
    print(result.stdout[-4000:])
if result.stderr:
    print(result.stderr[-4000:])

```