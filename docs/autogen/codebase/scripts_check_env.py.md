# 📄 ফাইল: scripts\check_env.py

**প্রকার:** .py  
**সাইজ:** 547 বাইট  
**আপডেট:** 2026-07-03T19:44:17.419872

---

## কোড

```py
from pathlib import Path

p = Path('.env')
data = p.read_bytes()
print('LEN', len(data))
print('NULL_FOUND', any(b == 0 for b in data))
print('NULL_COUNT', data.count(0))
indices = [i for i, b in enumerate(data) if b == 0]
print('NULL_INDICES', indices[:20])
for idx in indices[:20]:
    start = max(0, idx - 50)
    end = min(len(data), idx + 50)
    snippet = data[start:end]
    print('SNIPPET', idx, repr(snippet))

lines = data.splitlines(True)
for i, line in enumerate(lines, 1):
    if b'\x00' in line:
        print('LINE', i, repr(line))

```