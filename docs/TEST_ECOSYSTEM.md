# SupremeAI 2.0 — Test Ecosystem Documentation

_Last Updated: 2026-07-03_

## Overview

SupremeAI 2.0 implements a comprehensive, zero-cost test ecosystem across **backend**, **frontend**, and **end-to-end** layers, ensuring production-ready code quality and resilience.

---

## 1. 🐍 Backend Testing: Pytest + Coverage.py

### Configuration
- **File:** `backend/pyproject.toml`
- **Coverage Threshold:** `80%` (fail if below)
- **Output Format:** Markdown (pytest-md), JSON (for CI reporting)

### Run Locally
```bash
cd backend
poetry install --sync --with dev --without ml
poetry run pytest --cov=core --cov-report=term-missing --cov-fail-under=80
```

### CI Integration
```bash
poetry run pytest --md pytest-report.md \
  --cov=core --cov-report=json:coverage.json \
  --cov-report=term-missing --cov-fail-under=80 -q
```

### Test Structure
- **Location:** `backend/tests/`
- **Pattern:** `test_*.py` or `*_test.py`
- **Framework:** pytest with asyncio support (`pytest-asyncio`, `pytest-timeout`)
- **Key Packages:**
  - `pytest-cov`: Coverage analysis
  - `pytest-md`: Markdown report generation
  - `pytest-asyncio`: Async test support
  - `pytest-xdist`: Parallel test execution
  - `pytest-timeout`: Timeout enforcement

### Coverage Goals
| Component | Target | Current |
|-----------|--------|---------|
| core | 80% | TBD |
| api | 75% | TBD |
| models | 70% | TBD |

---

## 2. ⚡ Frontend Testing: Vitest

### Configuration
- **Files:**
  - `apps/studio-client/vitest.config.ts`
  - `apps/web-chat/vitest.config.ts`
  - `tools/vscode-extension/vitest.config.ts`

### Run Locally
```bash
# Install dependencies
pnpm install

# Studio Client tests
pnpm --dir apps/studio-client exec vitest run

# Web Chat tests
pnpm --dir apps/web-chat exec vitest run

# VS Code Extension tests
pnpm turbo run test --filter=supremeai-vscode
```

### CI Integration
```bash
# Run with JSON report for GitHub Summary
pnpm --dir apps/studio-client exec vitest run --reporter=json > apps/studio-client/vitest-report.json
pnpm --dir apps/web-chat exec vitest run --reporter=json > apps/web-chat/vitest-report.json
pnpm turbo run test --filter=supremeai-vscode
```

### Test Structure
- **Coverage:** Configured for React components and hooks
- **Environment:** jsdom (browser-like)
- **Globals:** Enabled (no need to import `describe`, `test`, etc.)
- **Key Packages:**
  - `vitest`: Fast unit test framework
  - `@testing-library/react`: React component testing
  - `@testing-library/dom`: DOM testing utilities

### Coverage Goals
| Package | Target | Current |
|---------|--------|---------|
| studio-client | 60% | TBD |
| web-chat | 60% | TBD |
| vscode-extension | 50% | TBD |

---

## 3. 🎭 End-to-End Testing: Playwright

### Configuration
- **File:** `tests/e2e/playwright.config.ts`
- **Browsers:** Chromium, Firefox, WebKit + Mobile Chrome, Mobile Safari
- **Timeout:** 30 seconds per test (default)
- **Retries:** 2 in CI, 0 locally

### Run Locally
```bash
# Install Playwright browsers
pnpm exec playwright install --with-deps

# Run tests
pnpm exec playwright test

# Run in headed mode (visual)
pnpm exec playwright test --headed

# Run specific test file
pnpm exec playwright test tests/e2e/chat.spec.ts
```

### CI Integration
```bash
pnpm exec playwright install --with-deps
pnpm exec playwright test
```

### Test Files
| File | Purpose | Coverage |
|------|---------|----------|
| `chat.spec.ts` | Chat UI and message sending | Studio Client chat UI |
| `admin-dashboard.spec.ts` | Admin panel functionality | Admin features |

### Playwright Best Practices
- **Auto-wait:** Playwright waits for elements to be actionable
- **Screenshots:** Captured on failure for debugging
- **Videos:** Retained on failure (CI only)
- **Traces:** Recorded on first retry for deep debugging

### Run Specific Tests
```bash
# Run chat tests only
pnpm exec playwright test chat.spec.ts

# Run with specific browser
pnpm exec playwright test --project=firefox

# Run with debug mode
pnpm exec playwright test --debug

# View report
pnpm exec playwright show-report
```

---

## 4. ⏱️ Load Testing: k6

### Configuration
- **File:** `scripts/k6/load_test.js`
- **Stages:** Ramp-up (30s, 20 VUs) → Hold (60s, 50 VUs) → Ramp-down (30s)
- **Thresholds:**
  - HTTP response time p95 < 500ms
  - Error rate < 5%

### Run Locally
```bash
# Install k6 (macOS)
brew install k6

# Install k6 (Ubuntu/Linux)
sudo apt-get install k6

# Run load test
k6 run scripts/k6/load_test.js

# Run with environment variable
SUPREMEAI_URL=http://api.supremeai.dev k6 run scripts/k6/load_test.js
```

### CI Integration
```bash
# Installed and run automatically in GitHub Actions
k6 run --out json=load-test-output.json scripts/k6/load_test.js
```

### Load Test Scenarios
- **Health Check:** Verifies `/health` endpoint
- **Actuator Health:** Checks `/actuator/health`
- **Task Execution:** Tests `/task/execute` endpoint
- **Duration:** ~2 minutes per run
- **Output:** JSON report uploaded as CI artifact

---

## 5. 🔄 GitHub Actions CI/CD Integration

### Workflow File
- **Location:** `.github/workflows/supreme-core-ci.yml`

### Test Jobs (in order)
1. **Backend Tests** (`backend-core`)
   - Python setup + Poetry install
   - Ruff lint + format
   - Pytest with 80% coverage
   - Auto-fix on failure

2. **Frontend Tests** (`frontend-core`)
   - pnpm install + Turbo build/lint
   - Vitest for studio-client and web-chat
   - VS Code extension tests
   - Playwright E2E tests
   - Artifact upload

3. **Load Tests** (`load-test`)
   - k6 load test against backend
   - JSON report upload

4. **Security Audit** (`security-audit`)
   - CodeQL analysis (Python + JavaScript)
   - Runs in parallel

### GitHub Step Summary Integration
All test results are automatically appended to the **GitHub Step Summary** for easy viewing:
- Backend coverage percentage and line count
- Frontend test pass/fail counts
- Playwright test results (desktop + mobile)
- k6 load test metrics

### Trigger Conditions
```yaml
on:
  push:
    branches: [main, develop]
    paths-ignore: ['**.md', 'docs/**', 'LICENSE', '.gitignore', 'logs/**']
  pull_request:
    branches: [main, develop]
  workflow_dispatch  # Manual trigger
```

---

## 6. 📊 Local Test Commands (Quick Reference)

### Backend
```bash
cd backend
poetry run pytest                          # Run all tests
poetry run pytest tests/core/test_*.py     # Run specific module
poetry run pytest -v --tb=short            # Verbose with short traceback
poetry run pytest --cov=core --cov-report=html  # Generate HTML coverage report
```

### Frontend (Studio Client)
```bash
pnpm --dir apps/studio-client vitest       # Watch mode
pnpm --dir apps/studio-client vitest run   # Single run
pnpm --dir apps/studio-client vitest --ui  # UI mode
```

### E2E (Playwright)
```bash
pnpm exec playwright test                  # Run all E2E tests
pnpm exec playwright test --headed         # Visible browser
pnpm exec playwright test --debug          # Debug mode with inspector
pnpm exec playwright show-report           # Open HTML report
```

### Load Test
```bash
SUPREMEAI_URL=http://127.0.0.1:8000 k6 run scripts/k6/load_test.js
```

### Full Test Suite (Locally)
```bash
# Terminal 1: Start backend dev server
cd backend
poetry run uvicorn main:app --reload

# Terminal 2: Start frontend dev server
pnpm --dir apps/studio-client dev

# Terminal 3: Run all tests
cd backend && poetry run pytest
pnpm exec playwright test
SUPREMEAI_URL=http://127.0.0.1:8000 k6 run scripts/k6/load_test.js
```

---

## 7. 🎯 Test Coverage Goals & Metrics

### Production Readiness Checklist
- ✅ Backend: 80% coverage
- ✅ Frontend: 60% coverage (components)
- ✅ E2E: Critical user paths covered
- ✅ Load: Can handle 50 concurrent users
- ✅ Security: CodeQL passes all checks

### Coverage Reporting
Coverage reports are generated locally in:
- `backend/htmlcov/` (HTML report)
- `apps/studio-client/coverage/` (HTML report)
- GitHub Step Summary (CI summary)

---

## 8. 🔧 Troubleshooting

### Backend Tests Fail
```bash
# Clear cache and retry
poetry cache clear pypi --all
poetry install --sync --with dev
poetry run pytest
```

### Frontend Tests Hang
```bash
# Clear pnpm store
pnpm store prune
pnpm install --force
pnpm --dir apps/studio-client vitest run
```

### Playwright Tests Fail on CI
```bash
# Ensure browsers are installed
pnpm exec playwright install --with-deps

# Run with debug output
pnpm exec playwright test --debug
```

### k6 Load Test Connection Errors
```bash
# Verify backend is running
curl http://127.0.0.1:8000/health

# Run k6 with verbose output
k6 run -v scripts/k6/load_test.js
```

---

## 9. 📚 References & Resources

- **Pytest Documentation:** https://docs.pytest.org
- **Vitest Documentation:** https://vitest.dev
- **Playwright Documentation:** https://playwright.dev
- **k6 Documentation:** https://k6.io/docs
- **GitHub Actions Documentation:** https://docs.github.com/en/actions

---

## 10. 📝 Contributing Test Guidelines

When adding new features:
1. **Backend:** Write pytest tests covering happy path + edge cases
2. **Frontend:** Write Vitest tests for components and hooks
3. **E2E:** Add Playwright tests for critical user workflows
4. **Load:** Update k6 script if adding new endpoints

### Test File Naming
- Backend: `test_<module>.py` (e.g., `test_auth.py`)
- Frontend: `<component>.spec.ts` (e.g., `Button.spec.ts`)
- E2E: `<feature>.spec.ts` (e.g., `chat.spec.ts`)

### Coverage Minimum
- Backend: 80%
- Frontend: 60%
- E2E: Critical paths only

---

## Report Generation

Test results are automatically compiled and appended to GitHub Step Summary:
- **Backend:** Coverage % + line count
- **Frontend:** Pass/fail count by package
- **E2E:** Browser-specific results
- **Load:** Throughput + latency metrics

See `.github/scripts/generate-ci-report.py` for implementation.

---

**Status:** ✅ Production Ready | **Last Validated:** 2026-07-03
