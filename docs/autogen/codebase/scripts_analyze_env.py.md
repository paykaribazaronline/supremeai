# 📄 ফাইল: scripts\analyze_env.py

**প্রকার:** .py  
**সাইজ:** 584 বাইট  
**আপডেট:** 2026-07-03T21:21:02.002975

---

## কোড

```py
from pathlib import Path

path = Path('.env')
raw = path.read_bytes()
idx = raw.find(b'\x00')
print('TOTAL', len(raw))
print('FIRST_NULL', idx)
print('ASCII_PREFIX')
if idx == -1:
    print(raw.decode('utf-8', 'replace'))
else:
    print(raw[:idx].decode('utf-8', 'replace'))
print('---')
print('SUFFIX_DECODED_UTF16LE')
print(raw[idx:].decode('utf-16le', 'replace'))
print('---')
print('SUFFIX_DECODED_UTF16LE FIRST 40 LINES')
for i, line in enumerate(raw[idx:].decode('utf-16le', 'replace').splitlines(), 1):
    if i <= 40:
        print(f'{i:02}: {line}')
    else:
        break

```