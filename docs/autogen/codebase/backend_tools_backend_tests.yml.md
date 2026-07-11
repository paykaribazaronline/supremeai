# 📄 ফাইল: backend/tools/backend_tests.yml

**প্রকার:** .yml  
**সাইজ:** 1,546 বাইট  
**আপডেট:** 2026-07-11T13:46:44.168648

---

## কোড

```yml
name: Backend CI

on:
  push:
    branches: [ main ]
    paths:
      - 'backend/**'
  pull_request:
    branches: [ main ]
    paths:
      - 'backend/**'

jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        python-version: ["3.10", "3.11"]

    steps:
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Set up Python ${{ matrix.python-version }}
      uses: actions/setup-python@v4
      with:
        python-version: ${{ matrix.python-version }}

    - name: Cache dependencies
      uses: actions/cache@v3
      with:
        path: ~/.cache/pip
        key: ${{ runner.os }}-pip-${{ hashFiles('**/requirements.txt') }}
        restore-keys: |
          ${{ runner.os }}-pip-

    - name: Install dependencies
      run: |
        python -m pip install --upgrade pip
        pip install ruff pytest-cov
        pip install -r requirements.txt

    - name: Lint with ruff
      run: |
        ruff check backend/

    - name: Run Security Blind Spot Scanner
      run: |
        echo "Running custom security scanner..."
        python scripts/security/auto_find_blindspots.py

    - name: Run tests with pytest
      run: |
        pytest backend/tests/ --cov=backend --cov-report=xml --cov-fail-under=80

    - name: Upload coverage reports to Codecov
      uses: codecov/codecov-action@v3
      with:
        token: ${{ secrets.CODECOV_TOKEN }} # আপনার Codecov টোকেন এখানে যোগ করতে হবে
        files: ./coverage.xml
        fail_ci_if_error: true
```