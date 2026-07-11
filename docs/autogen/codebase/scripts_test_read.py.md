# 📄 ফাইল: scripts/test_read.py

**প্রকার:** .py  
**সাইজ:** 243 বাইট  
**আপডেট:** 2026-07-11T16:26:09.290440

---

## কোড

```py
#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import sys
from pathlib import Path
sys.stdout.reconfigure(encoding='utf-8')
fp = Path(r"C:\Users\n\supremeai\supremeai_2.0\backend\main.py")
txt = fp.read_text(encoding='utf-8')
print(txt[:200])

```