# 📋 Commit f4e98eef763fc5bb9d6b57d6a2d2c14b48ab0134

## Commit Stats
```
commit f4e98eef763fc5bb9d6b57d6a2d2c14b48ab0134
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Wed Jul 8 16:39:03 2026 +0600

    ci: optimize missed caching for docs and desktop jobs

 .github/workflows/supreme-core-ci.yml | 18 ++++++++++++++----
 1 file changed, 14 insertions(+), 4 deletions(-)

```

## Diff Detail
```diff
commit f4e98eef763fc5bb9d6b57d6a2d2c14b48ab0134
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Wed Jul 8 16:39:03 2026 +0600

    ci: optimize missed caching for docs and desktop jobs

diff --git a/.github/workflows/supreme-core-ci.yml b/.github/workflows/supreme-core-ci.yml
index 9c6bf436b..5a45f5d4c 100644
--- a/.github/workflows/supreme-core-ci.yml
+++ b/.github/workflows/supreme-core-ci.yml
@@ -645,13 +645,15 @@ jobs:
           target: ${{ matrix.target }}
           override: true
 
+      - uses: pnpm/action-setup@v3
+        with:
+          version: 9.0.0
       - name: 📦 Set up Node.js and pnpm
         uses: actions/setup-node@v4
         with:
           node-version: ${{ env.NODE_VERSION }}
-      - uses: pnpm/action-setup@v3
-        with:
-          version: 9.0.0
+          cache: 'pnpm'
+          cache-dependency-path: '**/pnpm-lock.yaml'
 
       - name: ⬇️ Install Frontend Dependencies
         run: pnpm install --frozen-lockfile
@@ -759,9 +761,17 @@ jobs:
         uses: actions/setup-python@v5
         with:
           python-version: '3.11'
-      - name: Install dependencies
+      - name: Load Cached Virtualenv
+        id: cached-poetry-dependencies
+        uses: actions/cache@v4
+        with:
+          path: backend/.venv
+          key: venv-${{ runner.os }}-${{ hashFiles('backend/poetry.lock') }}
+      - name: Install dependencies (Only on Cache Miss)
+        if: steps.cached-poetry-dependencies.outputs.cache-hit != 'true'
         run: |
           pip install poetry litellm pyyaml httpx
+          cd backend && poetry config virtualenvs.in-project true
           cd backend && poetry install --sync --with dev --without ml,tools
           
       - name: 🤖 Generate AI Architecture Docs (ADR, DFD, OpenAPI)

```
