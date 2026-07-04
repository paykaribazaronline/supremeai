# 📄 ফাইল: security-scan.yml

**প্রকার:** .yml  
**সাইজ:** 648 বাইট  
**আপডেট:** 2026-07-04T04:38:12.284769

---

## কোড

```yml
name: Security Blind Spot Scan

on:
  push:
    branches:
      - main
      - master
      - develop
  pull_request:
    branches:
      - main
      - master
      - develop
  workflow_dispatch: # Allows the workflow to be triggered manually from the Actions tab

jobs:
  security-scan:
    name: 🛡️ Auto Find Blind Spots
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.11'

      - name: Run Blind Spot Scanner
        run: python scripts/security/auto_find_blindspots.py
```