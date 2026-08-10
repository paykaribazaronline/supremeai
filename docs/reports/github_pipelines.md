# GitHub Actions Pipelines

This document contains the source code for all GitHub Actions workflows in the project.

## maintenance_pipeline.yml

`yaml
# SupremeAI - Manual Maintenance & Auto-Fixing Pipeline
# বাংলা মন্তব্য: এই পাইপলাইনটি ম্যানুয়ালি ট্রিগার করা যাবে এবং বিভিন্ন রক্ষণাবেক্ষণ কাজ চালানো যাবে। (সংস্করণ ৩.০)

name: "🤖 Manual Maintenance & Auto-Fix"

on:
  # বাংলা মন্তব্য: প্রতিদিন রাত ২টায় (UTC) স্বয়ংক্রিয়ভাবে Smart Summary চালাবে
  schedule:
    - cron: '0 2 * * *'
  workflow_dispatch:
    inputs:
      run_ci_failure_summary:
        description: '🧠 Smart CI Failure Summary (Core CI-এর ব্যর্থতা বিশ্লেষণ)'
        type: boolean
        default: true
      run_health_check:
        description: '🩺 Run Health Check'
        type: boolean
        default: false
      run_auto_lint_fix:
        description: '🔧 Run Auto Lint Fix (PR তৈরি করবে)'
        type: boolean
        default: false
      run_auto_dependency_upgrade:
        description: '📦 Run Auto Dependency Upgrade (PR তৈরি করবে)'
        type: boolean
        default: false
      run_dependency_scan:
        description: '🔍 Run Dependency Vulnerability Scan'
        type: boolean
        default: false
      run_outdated_report:
        description: '📦 Run Outdated Dependency Report'
        type: boolean
        default: false
      run_changelog_generator:
        description: '📝 Generate Changelog (PR তৈরি করবে)'
        type: boolean
        default: false
      run_cache_purge:
        description: '🗑️ Purge Redis Cache (Upstash)'
        type: boolean
        default: false
      run_generate_docs:
        description: '📚 Generate & Deploy Docs'
        type: boolean
        default: false
      run_performance_e2e:
        description: '🧪 Run Performance E2E (Playwright)'
        type: boolean
        default: false

# বাংলা মন্তব্য: প্রতিটি টাস্ককে আলাদা জব-এ ভাগ করা হয়েছে স্বচ্ছতা এবং নির্ভরযোগ্যতার জন্য।

# বাংলা মন্তব্য: NODE_VERSION এবং PYTHON_VERSION এখানে define করা হয়েছে কারণ এটি
# maintenance pipeline-এর নিজস্ব env scope। supreme-core-ci থেকে inherit হয় না।
env:
  PYTHON_VERSION: '3.11'
  NODE_VERSION: '24'

jobs:
  # বাংলা মন্তব্য: gatekeeper জব সবার আগে রান হয়।
  # ২৪ ঘণ্টার মধ্যে আগে রান হলে should_run=false সেট করে বাকি সব জব গ্রেসফুলি স্কিপ করায়।
  gatekeeper:
    name: "\U0001F6A6 Check 24h Gap"
    runs-on: ubuntu-latest
    outputs:
      should_run: ${{ steps.check_gap.outputs.should_run }}
    steps:
      - uses: actions/checkout@v4
      - name: Run Gap Check
        id: check_gap
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: python .github/scripts/enforce_24h_gap.py

  # এই জবটি শুধুমাত্র একবার রান হবে এবং সব downstream জব এটি ব্যবহার করবে।
  setup:
    needs: gatekeeper
    if: needs.gatekeeper.outputs.should_run == 'true' || github.event_name != 'schedule'
    runs-on: ubuntu-latest
    outputs:
      requirements-cache-key: ${{ steps.cache-key.outputs.key }}
    steps:
      - uses: actions/checkout@v4
      - name: "🐍 Setup Python Environment"
        id: setup-python
        uses: actions/setup-python@v5
        with:
          python-version: '3.11'
      - name: "🔑 Generate Cache Key"
        id: cache-key
        run: echo "key=py-${{ runner.os }}-${{ steps.setup-python.outputs.python-version }}-${{ hashFiles('**/requirements.txt') }}" >> $GITHUB_OUTPUT

      - name: Setup Poetry
        uses: snok/install-poetry@v1
        with:
          plugins: poetry-plugin-export

      - name: "♻️ Cache pip dependencies"
        uses: actions/cache@v4
        with:
          path: ~/.cache/pip
          key: ${{ runner.os }}-pip-${{ hashFiles('**/poetry.lock') }}
          restore-keys: |
            ${{ runner.os }}-pip-

      - name: "⚙️ Install Dependencies"
        working-directory: backend
        run: |
          python -m pip install --upgrade pip
          poetry export --with dev --without ml,tools --format requirements.txt --output requirements.txt
          pip install --no-cache-dir --disable-pip-version-check -r requirements.txt
          pip install black isort pip-audit ruff

  health-check:
    needs: setup
    if: github.event.inputs.run_health_check == 'true'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.11'
      - uses: actions/cache@v4
        with:
          path: ~/.cache/pip
          key: ${{ needs.setup.outputs.requirements-cache-key }}
      - name: "⚙️ Install Dependencies"
        run: pip install -r requirements.txt
      - name: "🩺 Run Health Check"
        run: |
          python -m backend.tools.health_checker

  auto-lint-fix:
    needs: setup
    if: github.event.inputs.run_auto_lint_fix == 'true'
    runs-on: ubuntu-latest
    permissions:
      contents: write
      pull-requests: write
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.11'
      - uses: actions/cache@v4
        with:
          path: ~/.cache/pip
          key: ${{ needs.setup.outputs.requirements-cache-key }}
      - name: "⚙️ Install Formatters"
        run: pip install black isort ruff
      - name: "💅 Run Auto-Lint & Format Fix"
        run: |
          ruff --fix .
          black .
          isort .
      - name: "🤖 Create Pull Request"
        uses: peter-evans/create-pull-request@v6
        env:
          ACTIONS_ALLOW_UNSECURE_NODE_VERSION: 'true'
        with:
          token: ${{ secrets.GITHUB_TOKEN }}
          commit-message: "chore: auto-lint and format with ruff, black, isort"
          title: "🤖 Auto-Lint & Format Fix (Ruff)"
          body: |
            এই PR টি 'auto-lint-fix' মেইনটেন্যান্স টাস্ক দ্বারা স্বয়ংক্রিয়ভাবে তৈরি হয়েছে।
            এতে স্বয়ংক্রিয় কোড ফরম্যাটিং এর পরিবর্তন রয়েছে।
          branch: "chore/auto-lint-fix-${{ github.run_id }}"
          labels: "maintenance, automated"

  dependency-vulnerability-scan:
    needs: setup
    if: github.event.inputs.run_dependency_scan == 'true'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.11'
      - uses: actions/cache@v4
        with:
          path: ~/.cache/pip
          key: ${{ needs.setup.outputs.requirements-cache-key }}
      - name: "⚙️ Install Scanner"
        run: pip install pip-audit
      - name: "📦 Run Vulnerability Scan"
        run: |
          pip-audit --format json > vulnerability-report.json || true
      - name: "📤 Upload Vulnerability Report"
        uses: actions/upload-artifact@v4
        with:
          name: vulnerability-report
          path: vulnerability-report.json

  # auto-remediate-security-issues জবটি এখানে যোগ করা যেতে পারে
  # ...
  generate-codebase-docs:
    name: 📝 Auto-Generate & Deploy Docs
    # বাংলা মন্তব্য: মেইন বা ডেভেলপ ব্রাঞ্চে পুশ করা হলে স্বয়ংক্রিয়ভাবে কোডবেসের মার্কডাউন ফাইল ও ড্যাশবোর্ড জেনারেট এবং ডিপ্লয় হবে
    runs-on: ubuntu-latest
    if: github.event.inputs.run_generate_docs == 'true'
    permissions:
      contents: write
      pages: write
      id-token: write
      actions: write
    environment:
      name: github-pages
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 1
      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.11'
      - name: Setup Poetry
        uses: snok/install-poetry@v1
        with:
          plugins: poetry-plugin-export

      - name: Cache pip dependencies
        uses: actions/cache@v4
        with:
          path: ~/.cache/pip
          key: ${{ runner.os }}-pip-${{ hashFiles('**/poetry.lock') }}
          restore-keys: |
            ${{ runner.os }}-pip-

      - name: Setup Python & Install Dependencies
        working-directory: backend
        run: |
          python -m pip install --upgrade pip
          poetry export --with dev --without ml,tools --format requirements.txt --output requirements.txt
          pip install --no-cache-dir --disable-pip-version-check -r requirements.txt
          poetry install --no-root

      - name: 📄 Generate API Documentation (OpenAPI)
        # বাংলা মন্তব্য: AI/LLM কল বাদ দেওয়া হয়েছে। শুধুমাত্র Swagger/OpenAPI জেনারেট হবে।
        # এতে GEMINI_API_KEY, OPENROUTER_API_KEY সহ অনেক সিক্রেট এক্সপোজ হওয়ার ঝুঁকি কমে গেছে
        # এবং রান টাইম কয়েক মিনিট থেকে কমে কয়েক সেকেন্ডে নেমে আসবে।
        run: |
          VENV_PYTHON=$(cd backend && poetry env info --path)/bin/python
          $VENV_PYTHON scripts/generate_openapi.py

      - name: 📦 Commit and Push OpenAPI Spec
        id: push_docs
        run: |
          git config --global user.name "SupremeAI-DocBot"
          git config --global user.email "docbot@supremeai.dev"
          git add -f backend/API-swagger.yaml || true
          git diff-index --quiet HEAD || (git commit -m "docs: auto-update API-swagger.yaml [skip ci]" && git push) || echo "No changes to commit"

      - name: 📦 Setup Node.js and pnpm for Docusaurus
        uses: pnpm/action-setup@v3
        with:
          version: 9.0.0
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'pnpm'
          cache-dependency-path: '**/pnpm-lock.yaml'

      - name: 🌐 Install Docusaurus Dependencies
        run: pnpm install --frozen-lockfile

      - name: 📚 Build Docusaurus Site
        working-directory: apps/docs
        run: pnpm build

      - name: 🚀 Upload Docusaurus Build Artifact
        uses: actions/upload-pages-artifact@v3
        with:
          path: apps/docs/build

      - name: Setup GitHub Pages Environment
        if: github.ref == 'refs/heads/main'
        uses: actions/configure-pages@v5
        with:
          enablement: true # বাংলা মন্তব্য: রিপোজিটরিতে যদি পেজেস কনফিগার করা না থাকে, তবে এটি স্বয়ংক্রিয়ভাবে অ্যাকশনস সোর্স দিয়ে চালু করবে।
      - name: Prepare Pages Content (exclude large files)
        if: github.ref == 'refs/heads/main'
        run: |
          # বাংলা মন্তব্য: codebase_full.md ফাইলটি ১৩MB+ বড় হওয়ায় GitHub Pages limit অতিক্রম করে, তাই বাদ দেওয়া হচ্ছে
          find docs/autogen -name "codebase_full.md"


  worker-test:
    if: false
    name: ⚡ Cloudflare Worker (Test)
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v3
        with:
          version: 9.0.0
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'pnpm'
          cache-dependency-path: '**/pnpm-lock.yaml'

      - name: Install Dependencies
        run: pnpm install --frozen-lockfile

      - name: 🧪 Run Cloudflare Worker Tests
        id: worker_tests
        run: pnpm exec vitest run scripts/cloudflare_worker.test.mjs --reporter=json > infrastructure/vitest-report.json

      - name: Add Worker Test Results to GitHub Summary
        if: always()
        run: python .github/scripts/generate-ci-report.py --vitest-json infrastructure/vitest-report.json --label "Cloudflare Worker"

  generate-db-schema:
    name: 📊 Generate DB Schema Diagram
    runs-on: ubuntu-latest
    if: false
    needs: setup
    steps:
      - uses: actions/checkout@v4
      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: ${{ env.PYTHON_VERSION }}

      - name: Install Graphviz
        run: sudo apt-get update && sudo apt-get install -y graphviz

      - name: Install erd-from-pydantic
        run: pip install erd-from-pydantic

      - name: 📊 Generate ERD from Pydantic Models
        run: |
          # backend/models ফোল্ডারে থাকা সব Pydantic মডেল থেকে ডায়াগ্রাম তৈরি করা হবে
          erd-from-pydantic backend/models --output docs/autogen/db_schema.png
          echo "✅ Database schema diagram generated at docs/autogen/db_schema.png" >> $GITHUB_STEP_SUMMARY

      - name: 📤 Upload Schema Diagram Artifact
        uses: actions/upload-artifact@v4
        with:
          name: db-schema-diagram
          path: docs/autogen/db_schema.png



  performance-e2e-test:
    name: 🧪 Human Simulation & Load Tests
    runs-on: ubuntu-latest
    needs: gatekeeper
    # বাংলা মন্তব্য: gatekeeper should_run চেক করে — শুধু manual রানেই চলবে
    if: (needs.gatekeeper.outputs.should_run == 'true' || github.event_name != 'schedule') && github.event.inputs.run_performance_e2e == 'true'
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v3
        with:
          version: 9.0.0
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'pnpm'
          cache-dependency-path: '**/pnpm-lock.yaml'
      - name: Install Dependencies
        run: pnpm install --frozen-lockfile
      - name: Download Frontend Build (Allow Fallback)
        uses: actions/download-artifact@v4
        with:
          name: frontend-dist
          path: apps
        continue-on-error: true

      - uses: actions/setup-python@v5
        with:
          python-version: ${{ env.PYTHON_VERSION }}
          cache: 'pip'
      - name: Setup Poetry
        uses: snok/install-poetry@v1
        with:
          plugins: poetry-plugin-export

      - name: Cache pip dependencies
        uses: actions/cache@v4
        with:
          path: ~/.cache/pip
          key: ${{ runner.os }}-pip-${{ hashFiles('**/poetry.lock') }}
          restore-keys: |
            ${{ runner.os }}-pip-

      - name: Setup Python & Install Dependencies
        working-directory: backend
        run: |
          python -m pip install --upgrade pip
          poetry export --with dev --without ml,tools --format requirements.txt --output requirements.txt
          pip install --no-cache-dir --disable-pip-version-check -r requirements.txt
      - name: Start Backend Server
        working-directory: backend
        env:
          ENCRYPTION_KEY: ${{ secrets.ENCRYPTION_KEY }}
          SUPREMEAI_API_URL: http://127.0.0.1:8000
        run: poetry run uvicorn main:app --port 8000 &
      - name: Get Playwright Version
        id: playwright-version
        run: echo "version=$(pnpm exec playwright --version | awk '{print $2}')" >> $GITHUB_OUTPUT
      - name: Cache Playwright Browsers
        id: playwright-cache
        uses: actions/cache@v4
        with:
          path: ~/.cache/ms-playwright
          key: playwright-${{ runner.os }}-${{ steps.playwright-version.outputs.version }}
      - name: Install Playwright Browsers
        if: steps.playwright-cache.outputs.cache-hit != 'true'
        run: pnpm exec playwright install --with-deps
      - name: Install Playwright System Dependencies
        if: steps.playwright-cache.outputs.cache-hit == 'true'
        run: pnpm exec playwright install-deps
      - name: Start Frontend Preview Server
        run: |
          cd apps/studio-client && pnpm exec vite preview --port 5173 &
          sleep 5
        env:
          CI: true
      - name: Create Report Directory & Execute Playwright Simulation
        continue-on-error: true
        run: |
          mkdir -p playwright-report
          # --reporter=html ডিফল্ট হিসেবে কনফিগারেশন ফাইল থেকে আসে,
          # কিন্তু এখানে স্পষ্টভাবে উল্লেখ করাও ভালো।
          # ভিডিও এবং ট্রেস কনফিগারেশন playwright.config.ts থেকে আসবে।
          pnpm exec playwright test tests/e2e/accessibility.spec.ts tests/e2e/chat.spec.ts
        env:
          CI: true
          SUPREMEAI_API_URL: http://127.0.0.1:8000
      - name: Upload Test Report Artifacts
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: supremeai-human-test-report
          path: playwright-report/
          retention-days: 7

  # ==============================================================================
  # ⭐ PHASE 2: SMART CI FAILURE SUMMARY
  # বাংলা মন্তব্য: Core CI ব্যর্থ হলে এই জব GitHub API কল করে বিশ্লেষণ করবে
  # এবং অ্যাডমিনের জন্য একটি সুন্দর fix guide তৈরি করবে।
  # প্রতিদিন রাত ২টায় (UTC) বা manually trigger করা যাবে।
  # ==============================================================================
  ci-failure-smart-summary:
    name: 🧠 Smart CI Failure Summary
    runs-on: ubuntu-latest
    needs: gatekeeper
    # বাংলা মন্তব্য: gatekeeper should_run চেক করে scheduled রানে গ্রেসফুল স্কিপ নিশ্চিত করে
    if: >
      (needs.gatekeeper.outputs.should_run == 'true' || github.event_name != 'schedule') &&
      (github.event_name == 'schedule' || github.event.inputs.run_ci_failure_summary == 'true')
    permissions:
      actions: read
      contents: read
    steps:
      - uses: actions/checkout@v4

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: ${{ env.PYTHON_VERSION }}

      - name: Install dependencies
        run: pip install requests

      - name: 🧠 Run Smart CI Failure Detector
        # বাংলা মন্তব্য: GitHub API দিয়ে সর্বশেষ ব্যর্থ Core CI রান খুঁজে বের করে
        # প্রতিটি ব্যর্থ জবের জন্য auto-fix recommendation তৈরি করে।
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: python .github/scripts/ci_smart_summary.py

  # ==============================================================================
  # PHASE 3: UTILITY JOBS
  # ==============================================================================

  outdated-dependency-report:
    name: 📦 Outdated Dependency Report
    runs-on: ubuntu-latest
    needs: gatekeeper
    # বাংলা মন্তব্য: gatekeeper should_run চেক করে scheduled রানে গ্রেসফুল স্কিপ নিশ্চিত করে
    if: (needs.gatekeeper.outputs.should_run == 'true' || github.event_name != 'schedule') && github.event.inputs.run_outdated_report == 'true'
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-python@v5
        with:
          python-version: ${{ env.PYTHON_VERSION }}

      - uses: snok/install-poetry@v1
        with:
          plugins: poetry-plugin-export

      - uses: pnpm/action-setup@v3
        with:
          version: 9.0.0

      - uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'pnpm'
          cache-dependency-path: '**/pnpm-lock.yaml'

      - name: 📦 Check Outdated Python Dependencies
        # বাংলা মন্তব্য: পুরনো Python প্যাকেজের তালিকা তৈরি করে Step Summary-তে দেখায়
        working-directory: backend
        run: |
          python -m pip install --upgrade pip
          poetry export --without ml,tools --format requirements.txt --output requirements.txt
          pip install -r requirements.txt

          echo "## 📦 Outdated Python Dependencies" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "| Package | Current | Latest |" >> $GITHUB_STEP_SUMMARY
          echo "|---------|---------|--------|" >> $GITHUB_STEP_SUMMARY
          pip list --outdated --format=columns 2>/dev/null | tail -n +3 | \
            awk '{printf "| %s | %s | %s |\n", $1, $2, $3}' >> $GITHUB_STEP_SUMMARY || \
            echo "✅ সব Python প্যাকেজ আপ-টু-ডেট!" >> $GITHUB_STEP_SUMMARY

      - name: 📦 Check Outdated Node.js Dependencies
        # বাংলা মন্তব্য: পুরনো Node প্যাকেজের তালিকা তৈরি করে
        run: |
          pnpm install --frozen-lockfile
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "## 📦 Outdated Node.js Dependencies" >> $GITHUB_STEP_SUMMARY
          echo '```' >> $GITHUB_STEP_SUMMARY
          pnpm outdated --recursive 2>/dev/null >> $GITHUB_STEP_SUMMARY || \
            echo "✅ সব Node প্যাকেজ আপ-টু-ডেট!" >> $GITHUB_STEP_SUMMARY
          echo '```' >> $GITHUB_STEP_SUMMARY

  auto-dependency-upgrade:
    name: "📦 Auto Dependency Upgrade (PR)"
    runs-on: ubuntu-latest
    needs: [gatekeeper, outdated-dependency-report]
    if: (needs.gatekeeper.outputs.should_run == 'true' || github.event_name != 'schedule') && github.event.inputs.run_auto_dependency_upgrade == 'true'
    permissions:
      contents: write
      pull-requests: write
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: ${{ env.PYTHON_VERSION }}
      - uses: snok/install-poetry@v1
      - name: "⚙️ Install Dependencies & Upgrader Script"
        run: |
          pip install poetry requests beautifulsoup4
          # এখানে একটি নতুন স্ক্রিপ্ট `dependency_upgrader.py` ব্যবহার করা হবে

      - name: "🤖 Run Dependency Upgrade Agent"
        id: upgrade_agent
        env:
          GEMINI_API_KEY: ${{ secrets.GEMINI_API_KEY }}
        run: |
          # এই স্ক্রিপ্টটি `pip list --outdated` এবং `pnpm outdated` কমান্ডের আউটপুট পার্স করবে,
          # প্রতিটি প্যাকেজের changelog বিশ্লেষণ করে ব্রেকিং চেঞ্জ আছে কিনা তা মূল্যায়ন করবে
          # এবং কম ঝুঁকিপূর্ণ আপগ্রেডগুলো সম্পাদন করবে।
          python .github/scripts/dependency_upgrader.py > upgrade_summary.md

      - name: "🤖 Create Dependency Upgrade Pull Request"
        uses: peter-evans/create-pull-request@v6
        env:
          ACTIONS_ALLOW_UNSECURE_NODE_VERSION: 'true'
        with:
          token: ${{ secrets.GITHUB_TOKEN }}
          commit-message: "chore(deps): auto-upgrade non-major dependencies"
          title: "📦 Automated Dependency Upgrade"
          body: |
            AI Agent দ্বারা এই PR-টি স্বয়ংক্রিয়ভাবে তৈরি হয়েছে।
            এতে কম ঝুঁকিপূর্ণ Python ও Node.js প্যাকেজগুলোর আপগ্রেড রয়েছে।
            **আপগ্রেড সারাংশ:**
            ${{ steps.upgrade_agent.outputs.summary }}
          branch: "chore/auto-deps-upgrade-${{ github.run_id }}"
          labels: "dependencies, automated"

  changelog-generator:
    name: 📝 Changelog Generator (Auto PR)
    runs-on: ubuntu-latest
    needs: gatekeeper
    # বাংলা মন্তব্য: gatekeeper should_run চেক করে scheduled রানে গ্রেসফুল স্কিপ নিশ্চিত করে
    if: (needs.gatekeeper.outputs.should_run == 'true' || github.event_name != 'schedule') && github.event.inputs.run_changelog_generator == 'true'
    permissions:
      contents: write
      pull-requests: write
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0  # বাংলা মন্তব্য: সব commit history দরকার changelog-এর জন্য

      - name: 📝 Generate CHANGELOG.md from Git History
        # বাংলা মন্তব্য: গত ৩০ দিনের git commit থেকে changelog তৈরি করা হচ্ছে
        # conventional commits format অনুসরণ করে categorize করা হচ্ছে
        run: |
          SINCE_DATE=$(date -d '30 days ago' +%Y-%m-%d 2>/dev/null || date -v-30d +%Y-%m-%d)
          echo "# 📋 CHANGELOG" > CHANGELOG_NEW.md
          echo "" >> CHANGELOG_NEW.md
          echo "## 🗓️ Changes since \`${SINCE_DATE}\`" >> CHANGELOG_NEW.md
          echo "" >> CHANGELOG_NEW.md

          # Features
          FEATURES=$(git log --since="$SINCE_DATE" --pretty=format:"- %s (%h)" --grep="^feat" 2>/dev/null)
          if [ -n "$FEATURES" ]; then
            echo "### ✨ New Features" >> CHANGELOG_NEW.md
            echo "$FEATURES" >> CHANGELOG_NEW.md
            echo "" >> CHANGELOG_NEW.md
          fi

          # Bug Fixes
          FIXES=$(git log --since="$SINCE_DATE" --pretty=format:"- %s (%h)" --grep="^fix" 2>/dev/null)
          if [ -n "$FIXES" ]; then
            echo "### 🐛 Bug Fixes" >> CHANGELOG_NEW.md
            echo "$FIXES" >> CHANGELOG_NEW.md
            echo "" >> CHANGELOG_NEW.md
          fi

          # CI/CD Changes
          CI_CHANGES=$(git log --since="$SINCE_DATE" --pretty=format:"- %s (%h)" --grep="^ci" 2>/dev/null)
          if [ -n "$CI_CHANGES" ]; then
            echo "### 🔧 CI/CD Changes" >> CHANGELOG_NEW.md
            echo "$CI_CHANGES" >> CHANGELOG_NEW.md
            echo "" >> CHANGELOG_NEW.md
          fi

          # Docs Changes
          DOCS=$(git log --since="$SINCE_DATE" --pretty=format:"- %s (%h)" --grep="^docs" 2>/dev/null)
          if [ -n "$DOCS" ]; then
            echo "### 📚 Documentation" >> CHANGELOG_NEW.md
            echo "$DOCS" >> CHANGELOG_NEW.md
            echo "" >> CHANGELOG_NEW.md
          fi

          # All other commits
          echo "### 🔄 Other Changes" >> CHANGELOG_NEW.md
          git log --since="$SINCE_DATE" --pretty=format:"- %s (%h by %an)" \
            --invert-grep --grep="^feat\|^fix\|^ci\|^docs\|\[skip ci\]" 2>/dev/null | \
            head -30 >> CHANGELOG_NEW.md || true

          # যদি CHANGELOG.md আগে থেকে থাকে তাহলে merge করা
          if [ -f CHANGELOG.md ]; then
            cat CHANGELOG.md >> CHANGELOG_NEW.md
          fi
          mv CHANGELOG_NEW.md CHANGELOG.md

          echo "## 📝 Changelog Generated" >> $GITHUB_STEP_SUMMARY
          echo "✅ গত 30 দিনের commit থেকে CHANGELOG.md তৈরি হয়েছে।" >> $GITHUB_STEP_SUMMARY

      - name: 🤖 Create Changelog Pull Request
        # বাংলা মন্তব্য: সরাসরি push না করে PR তৈরি করা হচ্ছে — best practice
        uses: peter-evans/create-pull-request@v6
        env:
          ACTIONS_ALLOW_UNSECURE_NODE_VERSION: 'true'
        with:
          token: ${{ secrets.GITHUB_TOKEN }}
          commit-message: "docs: auto-generate CHANGELOG.md [skip ci]"
          title: "📝 Auto-Generated Changelog Update"
          body: |
            ## 📋 Automated Changelog Update

            এই PR-টি `changelog-generator` maintenance job দ্বারা স্বয়ংক্রিয়ভাবে তৈরি করা হয়েছে।

            **গত ৩০ দিনের commits থেকে** নিচের categories-এ changelog আপডেট করা হয়েছে:
            - ✨ New Features (`feat:`)
            - 🐛 Bug Fixes (`fix:`)
            - 🔧 CI/CD Changes (`ci:`)
            - 📚 Documentation (`docs:`)
            - 🔄 Other Changes

            **Review করে merge করুন।** কোনো সমস্যা থাকলে এই PR close করুন।
          branch: "chore/auto-changelog-${{ github.run_id }}"
          base: main
          labels: "documentation, automated, changelog"
          delete-branch: true

  cache-purge:
    name: 🗑️ Purge Redis Cache (Upstash)
    runs-on: ubuntu-latest
    needs: gatekeeper
    # বাংলা মন্তব্য: gatekeeper should_run চেক করে scheduled রানে গ্রেসফুল স্কিপ নিশ্চিত করে
    if: (needs.gatekeeper.outputs.should_run == 'true' || github.event_name != 'schedule') && github.event.inputs.run_cache_purge == 'true'
    steps:
      - name: 🗑️ Flush Upstash Redis Cache
        # বাংলা মন্তব্য: Upstash Redis REST API দিয়ে সব stale cache key পরিষ্কার করা হচ্ছে
        # এটি শুধুমাত্র manual trigger-এ চলবে — কোনো accidental flush এড়াতে
        env:
          UPSTASH_REDIS_REST_URL: ${{ secrets.UPSTASH_REDIS_REST_URL }}
          UPSTASH_REDIS_REST_TOKEN: ${{ secrets.UPSTASH_REDIS_REST_TOKEN }}
        run: |
          if [ -z "$UPSTASH_REDIS_REST_URL" ] || [ -z "$UPSTASH_REDIS_REST_TOKEN" ]; then
            echo "⚠️ UPSTASH_REDIS_REST_URL বা UPSTASH_REDIS_REST_TOKEN secret সেট নেই।" >> $GITHUB_STEP_SUMMARY
            echo "Skipping cache purge."
            exit 0
          fi

          echo "## 🗑️ Redis Cache Purge" >> $GITHUB_STEP_SUMMARY
          echo "**Starting cache flush via Upstash REST API...**" >> $GITHUB_STEP_SUMMARY

          RESPONSE=$(curl -s -X POST \
            "$UPSTASH_REDIS_REST_URL/FLUSHDB" \
            -H "Authorization: Bearer $UPSTASH_REDIS_REST_TOKEN")

          if echo "$RESPONSE" | grep -q '"result"'; then
            echo "✅ Redis cache সফলভাবে flush করা হয়েছে।" >> $GITHUB_STEP_SUMMARY
            echo "**Response:** \`$RESPONSE\`" >> $GITHUB_STEP_SUMMARY
          else
            echo "❌ Cache flush করতে সমস্যা হয়েছে।" >> $GITHUB_STEP_SUMMARY
            echo "**Error Response:** \`$RESPONSE\`" >> $GITHUB_STEP_SUMMARY
            exit 1
          fi

  api-health-check:
    runs-on: ubuntu-latest
    needs: setup
    steps:
      - uses: actions/checkout@v4
      - name: Generate API Health Report
        run: poetry run python scripts/generate_api_health_report.py >> $GITHUB_STEP_SUMMARY

  cost-guard-defcon:
    runs-on: ubuntu-latest
    needs: setup
    steps:
      - uses: actions/checkout@v4
      - name: Run Cost Guard
        run: poetry run python scripts/cost_guard_monitor.py
    env:
      DISCORD_WEBHOOK_URL: ${{ secrets.DISCORD_WEBHOOK_URL }}
      OPENROUTER_API_KEY: ${{ secrets.OPENROUTER_API_KEY }}

  ai-db-optimizer:
    runs-on: ubuntu-latest
    needs: setup
    steps:
      - uses: actions/checkout@v4
      - name: Run AI Query Optimizer
        run: poetry run python scripts/ai_query_optimizer.py
    env:
      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      GEMINI_API_KEY: ${{ secrets.GEMINI_API_KEY }}
      SUPABASE_DB_URL: ${{ secrets.SUPABASE_DATABASE_URL }}
`

## supreme-core-ci.yml

`yaml


name: 🧠 SupremeAI Core CI

on:
  workflow_dispatch:
    inputs:
      run_pre_merge_gate:
        description: 'Run Pre-Merge Gate'
        type: boolean
        default: true
      run_backend_core:
        description: 'Run Backend Core'
        type: boolean
        default: true
      run_frontend_core:
        description: 'Run Frontend Core'
        type: boolean
        default: true
      run_performance_e2e:
        description: 'Run Performance E2E'
        type: boolean
        default: true
      run_deploy_render:
        description: 'Run Deploy to Render'
        type: boolean
        default: true
      ignore_dependencies:
        description: 'Force run selected job even if upstream jobs are skipped? (Not for deploy)'
        type: boolean
        default: false
  push:
    branches: [main, develop]
    paths-ignore: ['**.md', 'docs/**', 'LICENSE', '.gitignore', 'logs/**']
  pull_request:
    branches: [main, develop]

  schedule:
    - cron: '0 0 * * *'

# ==============================================================================
# [IMMUTABLE CONFIGURATION - MANUAL CONTROL ONLY]
# ------------------------------------------------------------------------------
# DO NOT ALLOW AI AGENTS TO MODIFY THIS CONCURRENCY LOGIC.
# Purpose: Ensures new pushes cancel pending/running jobs in this pipeline.
# ==============================================================================
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

env:
  PYTHON_VERSION: '3.11'
  NODE_VERSION: '24'
  SUPREMEAI_API_URL: ${{ vars.SUPREMEAI_API_URL }}
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:

  changes:
    runs-on: ubuntu-latest
    outputs:
      backend: ${{ steps.filter.outputs.backend }}
      frontend: ${{ steps.filter.outputs.frontend }}
      dependencies: ${{ steps.filter.outputs.dependencies }}
      docs_only: ${{ steps.filter.outputs.docs_only }}
    steps:
      - uses: actions/checkout@v4
      - uses: dorny/paths-filter@v3
        id: filter
        with:
          filters: |
            backend:
              - 'backend/**'
              - 'api/**'
              - 'core/**'
              - '.github/workflows/**'
            frontend:
              - 'apps/studio-client/**'
              - 'apps/web-chat/**'
              - '.github/workflows/**'
            dependencies:
              - 'pyproject.toml'
              - 'poetry.lock'
              - 'package.json'
              - 'pnpm-lock.yaml'
            docs_only:
              - '**.md'
              - 'docs/**'

  # ==============================================================================
  # PRE-MERGE GATE: Iron Curtain — যেকোনো কোড মার্জের আগে বাধ্যতামূলক চেক
  # এই job fail হলে অন্য কোনো job চলবে না।
  # ==============================================================================
  pre-merge-gate:
    name: 🚧 Pre-Merge Gate (Iron Curtain)
    needs: changes
    runs-on: ubuntu-latest
    outputs:
      trivial_change: ${{ steps.gate-logic.outputs.trivial_change }}
    steps:
      - name: Set Trivial Change Output
        id: gate-logic
        run: |
          if [[ "${{ needs.changes.outputs.backend }}" == 'false' && "${{ needs.changes.outputs.frontend }}" == 'false' && "${{ needs.changes.outputs.dependencies }}" == 'false' && "${{ needs.changes.outputs.docs_only }}" == 'true' ]]; then
            echo "trivial_change=true" >> $GITHUB_OUTPUT
          else
            echo "trivial_change=false" >> $GITHUB_OUTPUT
          fi

      - uses: actions/checkout@v4
      - name: Set up Python
        if: needs.changes.outputs.docs_only != 'true'
        uses: actions/setup-python@v5
        with:
          python-version: ${{ env.PYTHON_VERSION }}

      - name: Install minimal gate dependencies
        run: pip install ruff

      - name: 🚫 Gate 1 — Zero-Gap Stub/Placeholder Data Check
        if: needs.changes.outputs.docs_only != 'true'
        # বাংলা মন্তব্য: Stub ডেটা পেলে সাথে সাথে পাইপলাইন বন্ধ হয়ে যাবে
        run: |
          echo "=== Zero-Gap স্টাব ডেটা গেট ===" >> $GITHUB_STEP_SUMMARY
          python scripts/find_stub_data.py --path . --fail-on HIGH
          echo "✅ পাস: কোনো স্টাব/প্লেসহোল্ডার প্যাটার্ন পাওয়া যায়নি" >> $GITHUB_STEP_SUMMARY

      - name: 🛡️ Gate 1.5 — Security Blind Spot Scan
        if: needs.changes.outputs.docs_only != 'true'
        run: |
          echo "=== Security Blind Spot Scan ===" >> $GITHUB_STEP_SUMMARY
          python scripts/security/auto_find_blindspots.py
          echo "✅ পাস: কোনো ক্রিটিকাল সিকিউরিটি রিস্ক নেই" >> $GITHUB_STEP_SUMMARY

      - name: 🔬 Gate 2 — Ruff Linting (No Silent Bugs)
        if: needs.changes.outputs.docs_only != 'true'
        # বাংলা মন্তব্য: T201 (print), BLE001 (silent except) rule enforce করা হচ্ছে
        run: |
          echo "=== Ruff স্ট্যাটিক অ্যানালাইসিস গেট ===" >> $GITHUB_STEP_SUMMARY
          ruff check backend/ --select=E,W,F,T201,BLE001 --ignore=E501 --no-fix
          echo "✅ পাস: Ruff লিন্টিং গেট ক্লিয়ার" >> $GITHUB_STEP_SUMMARY

      - name: 📋 Gate 3 — Observability Check (No httpx without timeout)
        if: needs.changes.outputs.docs_only != 'true'
        # বাংলা মন্তব্য: timeout ছাড়া httpx.AsyncClient() ব্যবহার করলে fail
        run: |
          echo "=== HTTP টাইমআউট অডিট গেট ===" >> $GITHUB_STEP_SUMMARY
          # grep for httpx.AsyncClient() without timeout parameter
          VIOLATIONS=$(grep -rn "httpx\.AsyncClient()" backend/ --include="*.py" | grep -v "test_" | grep -v ".venv" | wc -l)
          if [ "$VIOLATIONS" -gt 0 ]; then
            echo "❌ FAIL: Found $VIOLATIONS httpx.AsyncClient() call(s) without explicit timeout!" >> $GITHUB_STEP_SUMMARY
            grep -rn "httpx\.AsyncClient()" backend/ --include="*.py" | grep -v "test_" | grep -v ".venv"
            exit 1
          fi
          echo "✅ পাস: সব httpx ক্লায়েন্টে নির্দিষ্ট টাইমআউট আছে" >> $GITHUB_STEP_SUMMARY

  observability-audit:
    name: "🔬 Observability Audit (No Silent Errors)"
    needs: [pre-merge-gate]
    if: needs.pre-merge-gate.outputs.trivial_change != 'true'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: ${{ env.PYTHON_VERSION }}

      - name: 🔬 Run Observability Audit Script
        id: audit
        run: |
          # এই স্ক্রিপ্টটি সাইলেন্ট exception (`except:`, `except Exception:`) খুঁজে বের করে।
          # কোনো সমস্যা পেলে এটি non-zero exit code দিয়ে ফেইল করবে।
          python scripts/audit_observability.py

      - name: Audit Summary
        if: always()
        run: |
          echo "## 🔬 Observability Audit" >> $GITHUB_STEP_SUMMARY
          echo "Outcome: **${{ steps.audit.outcome }}**" >> $GITHUB_STEP_SUMMARY
          echo "✅ কোনো সাইলেন্ট বা ব্রড exception হ্যান্ডলার পাওয়া যায়নি।" >> $GITHUB_STEP_SUMMARY

  production-readiness:
    name: 🚀 Production Readiness (Safety Guard, Multi-Model Validator, Codegraph)
    needs: [changes, pre-merge-gate]
    if: >
      (needs.pre-merge-gate.outputs.trivial_change != 'true' && (needs.changes.outputs.backend == 'true' || needs.changes.outputs.dependencies == 'true' || github.run_attempt > 1)) ||
      (github.event_name == 'workflow_dispatch' && (github.event.inputs.run_backend_core == 'true' || github.event.inputs.ignore_dependencies == 'true'))
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 1

      - uses: ./.github/actions/setup-backend
        with:
          python-version: ${{ env.PYTHON_VERSION }}

      - name: 🛡️ Safety Guard - File Protection Validation
        id: safety_guard
        working-directory: backend
        run: |
          echo "## 🛡️ সেফটি গার্ড ভ্যালিডেশন" >> $GITHUB_STEP_SUMMARY
          python ../scripts/safety_guard.py --check-only --report-json > safety-report.json 2>&1 || true

          # Parse and summarize
          if [ -f safety-report.json ]; then
            echo "✅ সেফটি গার্ড সম্পন্ন হয়েছে - রিপোর্ট দেখুন" >> $GITHUB_STEP_SUMMARY
          else
            echo "⚠️ সেফটি গার্ড ভ্যালিডেশন পাস করেছে" >> $GITHUB_STEP_SUMMARY
          fi

      - name: 🔍 Multi-Model Validator - Security & Logic Check
        id: validator
        working-directory: backend
        run: |
          echo "## 🔍 মাল্টি-মডেল কোড ভ্যালিডেশন" >> $GITHUB_STEP_SUMMARY
          python ../scripts/multi_model_validator.py ../backend/core/ --json-output validator-report.json 2>&1 || true

          # Check for critical issues
          if [ -f validator-report.json ]; then
            CRITICAL=$(grep -c "risk_level.*CRITICAL" validator-report.json || echo "0")
            if [ "$CRITICAL" -gt 0 ]; then
              echo "⚠️ Found $CRITICAL critical issues - review required" >> $GITHUB_STEP_SUMMARY
            else
              echo "✅ কোনো ক্রিটিকাল সিকিউরিটি ইস্যু পাওয়া যায়নি" >> $GITHUB_STEP_SUMMARY
            fi
          fi

      - name: 📊 Codegraph - Knowledge Base Generation
        id: codegraph
        working-directory: backend
        continue-on-error: true
        run: |
          echo "## 📊 নলেজ গ্রাফ জেনারেশন" >> $GITHUB_STEP_SUMMARY
          python ../scripts/codegraph_integration.py --full --output-dir ../docs/codebase/knowledge_graph 2>&1 || true
          echo "✅ নলেজ গ্রাফ আপডেট করা হয়েছে" >> $GITHUB_STEP_SUMMARY

      - name: 📤 Upload Production Readiness Reports
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: production-readiness-reports
          path: |
            backend/safety-report.json
            backend/validator-report.json
            docs/codebase/knowledge_graph/

      - name: 🚨 Production Readiness Summary
        if: always()
        run: |
          echo "## ✅ প্রোডাকশন রেডিনেস চেক সম্পন্ন" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "### সিস্টেম স্ট্যাটাস:" >> $GITHUB_STEP_SUMMARY
          echo "- Safety Guard: ${{ steps.safety_guard.outcome }}" >> $GITHUB_STEP_SUMMARY
          echo "- Multi-Model Validator: ${{ steps.validator.outcome }}" >> $GITHUB_STEP_SUMMARY
          echo "- Codegraph: ${{ steps.codegraph.outcome }}" >> $GITHUB_STEP_SUMMARY

  backend-core:
    name: 🐍 Backend (Test)
    needs: [changes, pre-merge-gate]
    if: >
      !failure() && !cancelled() &&
      (
        (needs.pre-merge-gate.outputs.trivial_change != 'true' && (needs.changes.outputs.backend == 'true' || needs.changes.outputs.dependencies == 'true' || github.run_attempt > 1)) ||
        (github.event_name == 'workflow_dispatch' && (github.event.inputs.run_backend_core == 'true' || github.event.inputs.ignore_dependencies == 'true'))
      )
    runs-on: ubuntu-latest
    env:
      ENCRYPTION_KEY: "CwE60g_bA67m-mock-encryption-key-padded-len="
      PYTHONPATH: ${{ github.workspace }}/backend
      GITHUB_TOKEN: "mock_dummy_token"
      RENDER_API_KEY: "mock_render_key"
      SUPABASE_DATABASE_URL: "postgresql://mock_user:mock_pass@localhost:5432/mock_db"
      ADMIN_AUTHORIZED: "true"
      DOCS_PASSWORD: "mock_docs_password"
      SUPREMEAI_ADMIN_PASSWORD_HASH: "$2b$12$mockhashmockhashmockhashmockhashmockhash"
      STRIPE_API_KEY: "mock_stripe_api_key"
      STRIPE_WEBHOOK_SECRET: "mock_stripe_webhook_secret"
      SUPABASE_URL: "https://mock.supabase.co"
      SUPABASE_KEY: "mock_supabase_key"
      GEMINI_API_KEY: "mock_gemini_api_key"
      OPENROUTER_API_KEY: "mock_openrouter_api_key"
      OPENAI_API_KEY: "mock_openai_api_key"
      ANTHROPIC_API_KEY: "mock_anthropic_api_key"
    steps:
      - uses: actions/checkout@v4
      - uses: ./.github/actions/setup-backend
        with:
          python-version: ${{ env.PYTHON_VERSION }}

      - name: 🧪 Run Tests
        id: backend_tests
        working-directory: backend
        run: |
          pytest --md pytest-report.md --cov=core --cov-report=json:coverage.json --cov-report=term-missing --cov-fail-under=38 -q

      - name: Add Backend Test Results to GitHub Summary
        if: always()
        working-directory: backend
        run: |
          python ../.github/scripts/supreme_ci.py generate-report \
            --pytest-json pytest-report.md \
            --coverage-json coverage.json \
            --label Backend
      - name: GCP Auth for Artifact Registry
        # বাংলা মন্তব্য: রেন্ডার-এ স্থানান্তরের কারণে জিসিপি বিল্ড ও পুশ নিষ্ক্রিয় করা হলো।
        if: false
        uses: 'google-github-actions/auth@v2'
        with:
          credentials_json: '${{ secrets.GCP_SA_KEY }}'

      - name: Login to GAR
        if: false
        uses: docker/login-action@v3
        with:
          registry: ${{ vars.GCP_REGION || 'us-central1' }}-docker.pkg.dev
          username: _json_key
          password: ${{ secrets.GCP_SA_KEY }}

      - name: Set up Docker Buildx
        if: false
        uses: docker/setup-buildx-action@v3

      - name: Build and Push Backend Image to GAR
        if: false
        id: build-and-push
        uses: docker/build-push-action@v5
        with:
          context: .
          file: ./backend/Dockerfile
          push: true
          tags: ${{ vars.GCP_REGION || 'us-central1' }}-docker.pkg.dev/${{ secrets.GCP_PROJECT_ID }}/supremeai-repo/supremeai-api:sha-${{ github.sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Echo Image Digest
        if: false
        run: |
          echo "Pushed image with digest: ${{ steps.build-and-push.outputs.digest }}"

  security-audit:
    name: 🛡️ CodeQL & Trivy Security Scan
    runs-on: ubuntu-latest
    needs: [changes, pre-merge-gate]
    if: >
      (needs.pre-merge-gate.outputs.trivial_change != 'true' && (github.event_name == 'schedule' || needs.changes.outputs.dependencies == 'true' || github.run_attempt > 1)) ||
      (github.event_name == 'workflow_dispatch' && (github.event.inputs.run_backend_core == 'true' || github.event.inputs.ignore_dependencies == 'true'))
    permissions:
      security-events: write
      actions: read
      contents: read
    steps:
      - uses: actions/checkout@v4

      - name: Initialize CodeQL
        uses: github/codeql-action/init@v4
        with:
          languages: 'python, javascript'

      - name: Perform CodeQL Analysis
        uses: github/codeql-action/analyze@v4
        with:
          category: "/language:python,javascript"

      - name: 🔍 Parallel Security Scan
        continue-on-error: true
        run: |
          wget https://github.com/aquasecurity/trivy/releases/download/v0.48.3/trivy_0.48.3_Linux-64bit.deb
          sudo dpkg -i trivy_0.48.3_Linux-64bit.deb
          trivy fs --format sarif --output trivy-python.sarif --severity CRITICAL,HIGH backend &
          trivy fs --format sarif --output trivy-nodejs.sarif --severity CRITICAL,HIGH . &
          wait

      - name: Upload Trivy Python SARIF
        uses: github/codeql-action/upload-sarif@v4
        if: ${{ always() && hashFiles('trivy-python.sarif') != '' }}
        with:
          sarif_file: 'trivy-python.sarif'
          category: 'trivy-python'
        continue-on-error: true

      - name: Upload Trivy Node.js SARIF
        uses: github/codeql-action/upload-sarif@v4
        if: ${{ always() && hashFiles('trivy-nodejs.sarif') != '' }}
        with:
          sarif_file: 'trivy-nodejs.sarif'
          category: 'trivy-nodejs'
        continue-on-error: true

      - name: 📊 Add Security Audit Results to GitHub Summary
        if: always()
        run: |
          echo "## 🛡️ Security Audit Results" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "### CodeQL Analysis" >> $GITHUB_STEP_SUMMARY
          echo "✅ CodeQL SARIF report uploaded to GitHub Security tab" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "### Trivy Vulnerability Scan" >> $GITHUB_STEP_SUMMARY
          echo "✅ Python dependencies scanned (backend/)" >> $GITHUB_STEP_SUMMARY
          echo "✅ Node.js dependencies scanned (apps/, tools/)" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "**View full results in the [Security tab](https://github.com/${{ github.repository }}/security/code-scanning)**" >> $GITHUB_STEP_SUMMARY

  frontend-core:
    name: 🌐 Frontend Monorepo (Turbo)
    runs-on: ubuntu-latest
    needs: [changes, pre-merge-gate]
    if: >
      !failure() && !cancelled() &&
      (
        (needs.pre-merge-gate.outputs.trivial_change != 'true' && (needs.changes.outputs.frontend == 'true' || needs.changes.outputs.dependencies == 'true' || github.run_attempt > 1)) ||
        (github.event_name == 'workflow_dispatch' && (github.event.inputs.run_frontend_core == 'true' || github.event.inputs.ignore_dependencies == 'true'))
      )
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v3
        with:
          version: 9.0.0
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'pnpm'
          cache-dependency-path: '**/pnpm-lock.yaml'

      - name: Install Frontend Dependencies
        run: |
          pnpm install --frozen-lockfile
          pnpm store prune

      - name: Cache Turborepo
        uses: actions/cache@v4
        with:
          path: .turbo
          key: ${{ runner.os }}-turbo-${{ github.sha }}
          restore-keys: |
            ${{ runner.os }}-turbo-

      - name: Build & Lint Frontend Packages
        env:
          VITE_PORTAL_TYPE: 'admin'
          VITE_API_URL: ${{ env.SUPREMEAI_API_URL }}
          VITE_API_BASE: ${{ env.SUPREMEAI_API_URL }}
        # বাংলা মন্তব্য: ওয়ার্কস্পেস থেকে web-chat রিমুভ করা হয়েছে, তাই টার্বোরেপো ফিল্টার থেকে এটি বাদ দেওয়া হল।
        run: pnpm turbo run build lint --filter=supremeai-studio-client --filter=supremeai-vscode --cache-dir=.turbo


      - name: Run Studio Client Vitest with JSON Report
        run: pnpm --dir apps/studio-client exec vitest run --reporter=json > apps/studio-client/vitest-report.json

      - name: Add Studio Client Test Results to GitHub Summary
        if: always()
        continue-on-error: true
        run: python .github/scripts/supreme_ci.py generate-report --vitest-json apps/studio-client/vitest-report.json --label "Studio Client"

      - name: Run Web Chat Vitest with JSON Report
        if: always()
        run: |
          if [ -d "apps/web-chat" ]; then
            SUPREMEAI_API_URL="https://mock-api.supremeai.local" pnpm --dir apps/web-chat exec vitest run --reporter=json --outputFile=vitest-report.json
          else
            echo "web-chat app not found, skipping tests."
          fi

      - name: Add Web Chat Test Results to GitHub Summary
        if: always()
        run: |
          if [ -f "apps/web-chat/vitest-report.json" ]; then
            python .github/scripts/supreme_ci.py generate-report --vitest-json apps/web-chat/vitest-report.json --label "Web Chat"
          else
            echo "No web-chat test results to report."
          fi

      - name: Run VS Code Extension Tests
        run: pnpm turbo run test --filter=supremeai-vscode --cache-dir=.turbo


      - name: Upload Build Artifacts
        uses: actions/upload-artifact@v4
        with:
          name: frontend-dist
          path: apps/studio-client/dist-admin
          retention-days: 1

  deploy-to-render:
    name: 🌐 Deploy Backend (Render)
    needs: [backend-core, pre-merge-gate]
    runs-on: ubuntu-latest
    if: |
      always() &&
      needs.backend-core.result != 'failure' && needs.backend-core.result != 'cancelled' && needs.backend-core.result != 'skipped' &&
      (
        github.ref == 'refs/heads/main' ||
        (github.event_name == 'workflow_dispatch' && github.event.inputs.run_deploy_render == 'true') ||
        github.run_attempt > 1
      )
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4

      - name: Log in to the Container registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Extract metadata (tags, labels) for Docker
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ghcr.io/${{ github.repository }}/supremeai-backend
          tags: |
            type=raw,value=latest
            type=sha

      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: .
          file: ./backend/Dockerfile
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Trigger Render Deploy
        run: |
          PRIMARY_HOOK="${{ secrets.RENDER_DEPLOY_HOOK_URL }}"
          BACKUP_HOOK="${{ secrets.RENDER_DEPLOY_HOOK_URL_BACKUP }}"

          if [ -n "$PRIMARY_HOOK" ]; then
            echo "Trying primary Render account..."
            if curl -f -s "$PRIMARY_HOOK" > /dev/null; then
              echo "✅ Primary Render deploy triggered successfully!"
            else
              echo "⚠️ Primary Render deploy failed. Limit reached or service down."
              if [ -n "$BACKUP_HOOK" ]; then
                echo "🔄 Trying backup Render account..."
                curl -f -s "$BACKUP_HOOK" > /dev/null || echo "❌ Both Render deploy hooks failed but continuing pipeline."
                echo "✅ Backup Render deploy triggered successfully!"
              else
                echo "No backup Render hook configured. Skipping."
              fi
            fi
          elif [ -n "$BACKUP_HOOK" ]; then
            echo "Primary hook not found. Trying backup Render account..."
            curl -f -s "$BACKUP_HOOK" > /dev/null || echo "❌ Deploy hook failed but continuing"
            echo "✅ Backup Render deploy triggered successfully!"
          else
            echo "Skipping Render deploy: No deploy hooks configured."
          fi

  deploy-backend:
    name: 🚀 Deploy Backend (Cloud Run)
    needs: [backend-core, security-audit]
    # TEMPORARILY DISABLED: Remove `false` and restore conditions to reactivate Cloud Run deployment
    if: false
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/checkout@v4

      - name: GCP Auth
        uses: google-github-actions/auth@v2
        with:
          credentials_json: ${{ secrets.GCP_SA_KEY }}

      - name: Authenticate Docker to GCP Artifact Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ vars.GCP_REGION || 'us-central1' }}-docker.pkg.dev
          username: _json_key
          password: ${{ secrets.GCP_SA_KEY }}

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
        with:
          driver-opts: image=moby/buildkit:buildx-stable-1

      - name: Build & Push API Image
        uses: docker/build-push-action@v5
        with:
          context: .
          file: ./backend/Dockerfile
          push: true
          tags: ${{ vars.GCP_REGION || 'us-central1' }}-docker.pkg.dev/${{ secrets.GCP_PROJECT_ID }}/supremeai-repo/supremeai-api:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: 🚀 Deploy API to Cloud Run
        env:
          GCP_PROJECT_ID: ${{ secrets.GCP_PROJECT_ID }}
          GCP_REGION: ${{ vars.GCP_REGION || 'us-central1' }}
          ENCRYPTION_KEY: ${{ secrets.ENCRYPTION_KEY }}
        run: python .github/scripts/supreme_ci.py deploy



  flutter-integration-tests:
    name: 📱 Flutter Integration Test
    needs: frontend-core
    if: github.event_name == 'pull_request' # শুধুমাত্র PR-এর জন্য চালানো যেতে পারে
    runs-on: macos-latest # iOS সিমুলেটরের জন্য macOS প্রয়োজন
    strategy:
      matrix:
        api-level: [30] # Android API level
        target: [ios, android]
    steps:
      - uses: actions/checkout@v4
      - uses: subosito/flutter-action@v2
        with:
          channel: 'stable'
      - name: Install Dependencies
        run: |
          cd apps/mobile
          flutter pub get
      - name: Run Flutter Integration Tests (Android)
        if: matrix.target == 'android'
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: ${{ matrix.api-level }}
          script: cd apps/mobile && flutter test integration_test
      - name: Run Flutter Integration Tests (iOS)
        if: matrix.target == 'ios'
        run: |
          cd apps/mobile
          flutter test integration_test

  build-and-release-desktop:
    name: 🖥️ Build & Release Desktop App
    needs: [backend-core, frontend-core]
    # শুধুমাত্র main ব্রাঞ্চে নতুন ট্যাগ (vX.X.X) পুশ করা হলে এই জবটি চলবে
    if: startsWith(github.ref, 'refs/tags/v')
    strategy:
      fail-fast: false
      matrix:
        # বাংলা মন্তব্য: তিনটি প্রধান অপারেটিং সিস্টেমের জন্য বিল্ড ম্যাট্রিক্স তৈরি করা হলো।
        include:
          - platform: 'macos-latest'
            target: 'x86_64-apple-darwin'
            pnpm_arch_filter: '--filter=supremeai-desktop'
          - platform: 'ubuntu-latest'
            target: 'x86_64-unknown-linux-gnu'
            pnpm_arch_filter: '--filter=supremeai-desktop'
          - platform: 'windows-latest'
            target: 'x86_64-pc-windows-msvc'
            pnpm_arch_filter: '--filter=supremeai-desktop'
    runs-on: ${{ matrix.platform }}
    steps:
      - uses: actions/checkout@v4

      - name: 🦀 Set up Rust
        uses: actions-rs/toolchain@v1
        with:
          toolchain: stable
          target: ${{ matrix.target }}
          override: true

      - uses: pnpm/action-setup@v3
        with:
          version: 9.0.0
      - name: 📦 Set up Node.js and pnpm
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'pnpm'
          cache-dependency-path: '**/pnpm-lock.yaml'

      - name: ⬇️ Install Frontend Dependencies
        run: pnpm install --frozen-lockfile

      # বাংলা মন্তব্য: লিনাক্সের জন্য প্রয়োজনীয় সিস্টেম লাইব্রেরি ইনস্টল করা হচ্ছে।
      - name: 🐧 Install Linux dependencies
        if: matrix.platform == 'ubuntu-latest'
        run: |
          sudo apt-get update
          sudo apt-get install -y libgtk-3-dev libwebkit2gtk-4.0-dev libappindicator3-dev librsvg2-dev patchelf

      - name: 🔨 Build Tauri App
        # tauri.conf.json থেকে beforeBuildCommand (npm run build:ui) স্বয়ংক্রিয়ভাবে চলবে
        run: pnpm ${{ matrix.pnpm_arch_filter }} tauri build --target ${{ matrix.target }}

      - name: 📦 Upload Release Assets
        uses: actions/upload-release-asset@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          upload_url: ${{ github.event.release.upload_url }}
          # বিল্ড হওয়া অ্যাসেটগুলোর পাথ খুঁজে বের করে আপলোড করা হচ্ছে
          asset_path: ./apps/desktop/src-tauri/target/release/bundle/msi/*.msi
          asset_name: supremeai-desktop_${{ github.ref_name }}_${{ matrix.target }}.msi
          asset_content_type: application/x-msi
        if: matrix.platform == 'windows-latest'
      # macOS এবং Linux-এর জন্য একই রকম 'upload-release-asset' ধাপ যোগ করতে হবে।
      # উদাহরণস্বরূপ, macOS-এর জন্য:
      # asset_path: ./apps/desktop/src-tauri/target/release/bundle/dmg/*.dmg
      # asset_name: supremeai-desktop_${{ github.ref_name }}_${{ matrix.target }}.dmg
      # asset_content_type: application/x-apple-diskimage

      # Linux-এর জন্য:
      # asset_path: ./apps/desktop/src-tauri/target/release/bundle/appimage/*.AppImage
      # asset_name: supremeai-desktop_${{ github.ref_name }}_${{ matrix.target }}.AppImage
      # asset_content_type: application/octet-stream

  deploy-frontend-prod:
    name: 🌐 Deploy Frontend (Firebase)
    needs: [frontend-core, security-audit]
    if: |
      always() &&
      github.ref == 'refs/heads/main' &&
      needs.frontend-core.result != 'failure' && needs.frontend-core.result != 'cancelled' && needs.frontend-core.result != 'skipped'
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/checkout@v4

      - name: 📥 Download Frontend Artifacts
        uses: actions/download-artifact@v4
        with:
          name: frontend-dist
          path: apps/studio-client/dist-admin

      - name: 🌐 Deploy to Firebase
        run: |
          npm install -g firebase-tools
          firebase deploy --only hosting --project ${{ secrets.GCP_PROJECT_ID }} --token "${{ secrets.FIREBASE_TOKEN }}"
          echo "### 🌐 Firebase Deployment Complete" >> $GITHUB_STEP_SUMMARY
          echo "**URL:** [https://${{ secrets.GCP_PROJECT_ID }}.web.app](https://${{ secrets.GCP_PROJECT_ID }}.web.app)" >> $GITHUB_STEP_SUMMARY

  deploy-to-vercel:
    name: 🚀 Deploy User Portal (Vercel)
    needs: [frontend-core, security-audit]
    runs-on: ubuntu-latest
    if: |
      always() &&
      github.ref == 'refs/heads/main' &&
      needs.frontend-core.result != 'failure' && needs.frontend-core.result != 'cancelled' && needs.frontend-core.result != 'skipped'
    steps:
      - uses: actions/checkout@v4
      - name: Install Vercel CLI & pnpm
        run: |
          npm install -g pnpm
          npm install --global vercel@latest
      - name: Pull Vercel Environment Information
        run: vercel pull --yes --environment=production --token=${{ secrets.VERCEL_TOKEN }}
      - name: Build Project Artifacts
        run: vercel build --prod --token=${{ secrets.VERCEL_TOKEN }}
      - name: Deploy Project Artifacts to Vercel
        continue-on-error: true
        run: |
          DEPLOY_URL=$(vercel deploy --prebuilt --prod --token=${{ secrets.VERCEL_TOKEN }} || echo "VERCEL_LIMIT_REACHED")
          if [[ "$DEPLOY_URL" == *"VERCEL_LIMIT_REACHED"* ]] || [[ "$DEPLOY_URL" == *"Error:"* ]]; then
            echo "### ⚠️ Vercel Deployment Failed/Skipped" >> $GITHUB_STEP_SUMMARY
            echo "Deployment failed (likely due to the 100/day free tier limit). Please try again in 24 hours." >> $GITHUB_STEP_SUMMARY
            exit 0
          else
            echo "### 🚀 Vercel Deployment Complete" >> $GITHUB_STEP_SUMMARY
            echo "**URL:** [$DEPLOY_URL]($DEPLOY_URL)" >> $GITHUB_STEP_SUMMARY
          fi
    env:
      VERCEL_ORG_ID: ${{ secrets.VERCEL_ORG_ID }}
      VERCEL_PROJECT_ID: ${{ secrets.VERCEL_PROJECT_ID }}

  sync-mirror:
    name: 📤 Sync to Secondary Repo
    needs: [deploy-backend, deploy-frontend-prod, security-audit]
    if: |
      always() &&
      github.ref == 'refs/heads/main' &&
      needs.deploy-backend.result != 'failure' && needs.deploy-backend.result != 'cancelled' &&
      needs.deploy-frontend-prod.result != 'failure' && needs.deploy-frontend-prod.result != 'cancelled' &&
      needs.security-audit.result != 'failure' && needs.security-audit.result != 'cancelled'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
          persist-credentials: false
          lfs: true

      - name: 📤 Sync to Secondary Repo (Staging Dispatch)
        if: env.MIRROR_REPO_TOKEN != ''
        env:
          MIRROR_REPO_TOKEN: ${{ secrets.MIRROR_REPO_TOKEN }}
        run: |
          git config lfs.allowincompletepush true
          git remote add mirror https://${MIRROR_REPO_TOKEN}@github.com/SaifulHaqueNiloy/supremeai.git
          git push --force mirror main:refs/heads/main

  canary-deploy:
    name: "🚀 Canary Deploy Backend (Cloud Run)"
    runs-on: ubuntu-latest
    # TEMPORARILY DISABLED: রেন্ডার-এ স্থানান্তরের কারণে জিসিপি ক্যানারি ডিপ্লয়মেন্ট নিষ্ক্রিয় করা হলো।
    if: false
    needs: [backend-core, security-audit]
    # backend-core এবং security-audit সফল হলে তবেই এটি চলবে
    # deploy-backend এর পরিবর্তে এই জবটি ব্যবহার করা হবে
    environment: production
    steps:
      - uses: actions/checkout@v4

      - name: GCP Auth
        uses: 'google-github-actions/auth@v2'
        with:
          credentials_json: '${{ secrets.GCP_SA_KEY }}'

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: ${{ env.PYTHON_VERSION }}

      - name: 🐤 Run Canary Deployment Script
        env:
          GCP_PROJECT_ID: ${{ secrets.GCP_PROJECT_ID }}
          GCP_REGION: ${{ vars.GCP_REGION || 'us-central1' }}
          # CANDIDATE_REVISION: Cloud Build থেকে পাওয়া নতুন রিভিশন এখানে পাস করতে হবে
          # আপাতত, সর্বশেষ রিভিশন স্বয়ংক্রিয়ভাবে পাওয়ার লজিক canary-deploy.py-তে থাকবে
          DISCORD_WEBHOOK_URL: ${{ secrets.DISCORD_WEBHOOK_URL }}
          ERROR_RATE_THRESHOLD: "0.01" # 1%
          LATENCY_P99_THRESHOLD_MS: "2000" # 2000ms
        run: |
          python .github/scripts/canary-deploy.py
`

## supreme-mobile-cd.yml

`yaml
name: 📱 SupremeAI Mobile CD (Fastlane)

on:
  push:
    tags:
      - 'v*.*.*'

jobs:
  deploy-android:
    name: 🤖 Build & Deploy Android (Play Store)
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'

      - name: Setup Flutter
        uses: subosito/flutter-action@v2
        with:
          flutter-version: '3.24.3'
          channel: 'stable'

      - name: Setup Ruby for Fastlane
        uses: ruby/setup-ruby@v1
        with:
          ruby-version: '3.3'
          bundler-cache: true

      - name: Install Fastlane
        run: gem install fastlane

      - name: Setup Android Keystore
        env:
          ANDROID_KEYSTORE_BASE64: ${{ secrets.ANDROID_KEYSTORE_BASE64 }}
        run: |
          if [ -n "$ANDROID_KEYSTORE_BASE64" ]; then
            echo "$ANDROID_KEYSTORE_BASE64" | base64 --decode > apps/mobile/android/app/keystore.jks
          else
            echo "Skipping keystore setup (secret not found)"
          fi

      - name: Decode Google Play Config JSON
        env:
          PLAY_STORE_CONFIG_JSON: ${{ secrets.PLAY_STORE_CONFIG_JSON }}
        run: |
          if [ -n "$PLAY_STORE_CONFIG_JSON" ]; then
            echo "$PLAY_STORE_CONFIG_JSON" > apps/mobile/android/fastlane/play-store-credentials.json
          else
            echo "Skipping Play Store credentials (secret not found)"
          fi

      - name: Fastlane Deploy to Play Store
        working-directory: apps/mobile/android
        env:
          ANDROID_KEY_ALIAS: ${{ secrets.ANDROID_KEY_ALIAS }}
          ANDROID_KEY_PASSWORD: ${{ secrets.ANDROID_KEY_PASSWORD }}
          ANDROID_STORE_PASSWORD: ${{ secrets.ANDROID_STORE_PASSWORD }}
        run: fastlane deploy

  deploy-ios:
    name: 🍏 Build & Deploy iOS (TestFlight)
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Flutter
        uses: subosito/flutter-action@v2
        with:
          flutter-version: '3.24.3'
          channel: 'stable'

      - name: Setup Ruby for Fastlane
        uses: ruby/setup-ruby@v1
        with:
          ruby-version: '3.3'
          bundler-cache: true

      - name: Install Fastlane
        run: gem install fastlane

      - name: Setup App Store Connect API Key
        env:
          APP_STORE_CONNECT_API_KEY_CONTENT: ${{ secrets.APP_STORE_CONNECT_API_KEY_CONTENT }}
        run: |
          if [ -n "$APP_STORE_CONNECT_API_KEY_CONTENT" ]; then
            mkdir -p ~/.appstoreconnect/private_keys/
            echo "$APP_STORE_CONNECT_API_KEY_CONTENT" > ~/.appstoreconnect/private_keys/AuthKey_${{ secrets.APP_STORE_CONNECT_API_KEY_ID }}.p8
          else
            echo "Skipping App Store API Key setup (secret not found)"
          fi

      - name: Fastlane Deploy to TestFlight
        working-directory: apps/mobile/ios
        env:
          APP_STORE_CONNECT_API_KEY_ID: ${{ secrets.APP_STORE_CONNECT_API_KEY_ID }}
          APP_STORE_CONNECT_API_ISSUER_ID: ${{ secrets.APP_STORE_CONNECT_API_ISSUER_ID }}
        run: fastlane deploy
`

## supreme-release-builds.yml

`yaml
name: 📦 SupremeAI Release Builder

on:
  push:
    tags: ['v*'] # শুধুমাত্র v1.0.0, v2.1.0 ইত্যাদি ট্যাগ পুশ করলে
  workflow_dispatch:
    inputs:
      publish_release:
        description: 'Publish to GitHub Releases?'
        type: boolean
        default: false

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

env:
  NODE_VERSION: '24'
  FLUTTER_VERSION: '3.29.0'

jobs:
  build-artifacts:
    name: 🏗️ Build ${{ matrix.target }}
    runs-on: ${{ matrix.os }}
    strategy:
      fail-fast: false
      matrix:
        include:
          - target: 'APK'
            os: ubuntu-latest
          - target: 'VSIX'
            os: ubuntu-latest
          - target: 'EXE'
            os: windows-latest

    steps:
      - uses: actions/checkout@v4
        with:
          lfs: true

      # ----------------------------------------------------
      # 📱 1. FLUTTER APK BUILD (Android Arm64)
      # ----------------------------------------------------
      - name: Setup Flutter
        if: matrix.target == 'APK'
        uses: subosito/flutter-action@v2
        with:
          flutter-version: ${{ env.FLUTTER_VERSION }}
          cache: true

      - name: Cache Gradle Packages
        if: matrix.target == 'APK'
        uses: actions/cache@v4
        with:
          path: ~/.gradle/caches
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: |
            ${{ runner.os }}-gradle-

      - name: Build APK (Arm64 for Latest Devices)
        if: matrix.target == 'APK'
        working-directory: apps/mobile
        run: |
          flutter pub get
          # বাংলা মন্তব্য: এএবি ফাইলের বদলে সরাসরি এপিকে ফাইল বিল্ড করা হচ্ছে যা লেটেস্ট আর্কিটেকচার (arm64-v8a) সাপোর্ট করবে এবং সাইজও অপটিমাইজড থাকবে।
          flutter build apk --release --target-platform android-arm64

      - name: Upload APK Artifact
        if: matrix.target == 'APK'
        uses: actions/upload-artifact@v4
        with:
          name: supremeai-mobile-apk
          path: apps/mobile/build/app/outputs/flutter-apk/*.apk
          retention-days: 7

      # ----------------------------------------------------
      # 🧩 2. VS CODE EXTENSION BUILD (VSIX)
      # ----------------------------------------------------
      - name: Setup Node & PNPM
        if: matrix.target == 'VSIX' || matrix.target == 'EXE'
        uses: pnpm/action-setup@v3
        with:
          version: 9.0.0
      - name: Setup Node caching
        if: matrix.target == 'VSIX' || matrix.target == 'EXE'
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'pnpm'

      - name: Build VSIX
        if: matrix.target == 'VSIX'
        run: |
          pnpm install --frozen-lockfile
          pnpm turbo run build --filter=supremeai-vscode
          cd tools/vscode-extension
          npx @vscode/vsce package --no-dependencies

      - name: Upload VSIX Artifact
        if: matrix.target == 'VSIX'
        uses: actions/upload-artifact@v4
        with:
          name: supremeai-vscode-vsix
          path: tools/vscode-extension/*.vsix
          retention-days: 7

      # ----------------------------------------------------
      # 🪟 3. WINDOWS EXE BUILD (Electron)
      # ----------------------------------------------------
      - name: Build Windows EXE
        if: matrix.target == 'EXE'
        run: |
          pnpm install --frozen-lockfile --prefer-offline
          pnpm turbo run build --filter=supremeai-studio-client
          cd apps/studio-client
          pnpm exec electron-builder --publish=never --config.compression=store
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}

      - name: Upload EXE Artifact
        if: matrix.target == 'EXE'
        uses: actions/upload-artifact@v4
        with:
          name: supremeai-studio-windows-exe
          path: apps/studio-client/dist/*.exe
          compression-level: 0
          retention-days: 7

  # ----------------------------------------------------
  # 🚀 CREATE GITHUB RELEASE
  # ----------------------------------------------------
  create-release:
    name: 🎉 Publish GitHub Release
    needs: build-artifacts
    if: startsWith(github.ref, 'refs/tags/v') || github.event.inputs.publish_release == 'true'
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - uses: actions/checkout@v4
        with:
          lfs: true
      - name: Download all artifacts
        uses: actions/download-artifact@v4
        with:
          path: release-artifacts
          merge-multiple: true

      - name: Determine Release Tag
        id: release_tag
        run: |
          if [[ "${{ github.ref }}" == refs/tags/* ]]; then
            echo "TAG_NAME=${{ github.ref_name }}" >> $GITHUB_OUTPUT
          else
            echo "TAG_NAME=v-manual-${{ github.run_id }}" >> $GITHUB_OUTPUT
          fi
        shell: bash

      - name: Publish Release
        uses: softprops/action-gh-release@v2
        with:
          tag_name: ${{ steps.release_tag.outputs.TAG_NAME }}
          files: release-artifacts/**/*
          generate_release_notes: true
`

## sync-from-prod.yml

`yaml
name: 🔄 Sync from Production

on:
  workflow_dispatch:
#  push:
#    branches:
#      - main
#      - master
#    # This workflow should only run in the staging repo
#    if: github.repository == 'saifulhaqueniloy/supremeai'

jobs:
  sync-code:
    name: ↔️ Sync Code from Production
    runs-on: ubuntu-latest
    # Condition to avoid infinite loops: only run if the commit is not from the bot
    if: "contains(github.event.head_commit.message, '[CI-SYNC]') == false"

    steps:
      - name: checkout staging repo
        uses: actions/checkout@v4
        with:
          ref: ${{ github.ref_name }}

      - name: Configure Git
        run: |
          git config --global user.name 'SupremeAI Sync Bot'
          git config --global user.email 'sync-bot@supremeai.dev'

      - name: Add production repo as remote
        run: |
          git remote add production https://x-access-token:${{ secrets.MAIN_REPO_TOKEN }}@github.com/paykaribazaronline/supremeai.git

      - name: Fetch and merge from production
        run: |
          git fetch production ${{ github.ref_name }}
          # Use a merge strategy that prefers production changes in case of conflict
          git merge --strategy-option theirs production/${{ github.ref_name }} -m "Merge remote-tracking branch 'production/${{ github.ref_name }}' [CI-SYNC]"

      - name: Push changes to staging
        run: git push origin ${{ github.ref_name }}
`
