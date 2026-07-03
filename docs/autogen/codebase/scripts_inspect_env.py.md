# 📄 ফাইল: scripts\inspect_env.py

**প্রকার:** .py  
**সাইজ:** 352 বাইট  
**আপডেট:** 2026-07-03T20:44:30.776226

---

## কোড

```py
from pathlib import Path

path = Path('.env')
raw = path.read_bytes()
lines = raw.splitlines(True)
print('TOTAL_LINES', len(lines))
for i, line in enumerate(lines, 1):
    if b'SUPABASE' in line or b'GOOGLE_CLOUD_PROJECT' in line or b'Automation Scripts' in line or b'\x00' in line:
        print(f'{i:03}: {repr(line)}')
    if i > 120:
        break

```