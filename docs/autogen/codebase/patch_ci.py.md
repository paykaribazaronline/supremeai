# 📄 ফাইল: patch_ci.py

**প্রকার:** .py  
**সাইজ:** 3,396 বাইট  
**আপডেট:** 2026-07-11T19:51:42.121673

---

## কোড

```py
import re

filepath = '.github/workflows/supreme-core-ci.yml'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add schedule to 'on:' block
if 'schedule:' not in content:
    content = re.sub(
        r'(workflow_dispatch:\n(?:    inputs:\n(?:      .*?\n)*?)?)\n',
        r"\1\n  schedule:\n    - cron: '0 0 * * *'\n\n",
        content, count=1
    )

# 2. Add changes job
changes_job = """  changes:
    runs-on: ubuntu-latest
    outputs:
      backend: ${{ steps.filter.outputs.backend }}
      frontend: ${{ steps.filter.outputs.frontend }}
      dependencies: ${{ steps.filter.outputs.dependencies }}
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
            frontend:
              - 'apps/studio-client/**'
              - 'apps/web-chat/**'
            dependencies:
              - 'pyproject.toml'
              - 'poetry.lock'
              - 'package.json'
              - 'pnpm-lock.yaml'

"""
if 'changes:' not in content:
    content = re.sub(r'jobs:\n\n', f'jobs:\n\n{changes_job}', content, count=1)

# 3. Update backend-core
content = re.sub(
    r'(backend-core:\n    name: .*?\n    needs: )\[pre-merge-gate, production-readiness\](\n    runs-on: ubuntu-latest)',
    r"\1[changes, pre-merge-gate, production-readiness]\n    if: needs.changes.outputs.backend == 'true' || needs.changes.outputs.dependencies == 'true'\2",
    content, count=1
)

# 4. Update frontend-core
content = re.sub(
    r'(frontend-core:\n    name: .*?\n    runs-on: ubuntu-latest)',
    r"\1\n    needs: changes\n    if: needs.changes.outputs.frontend == 'true' || needs.changes.outputs.dependencies == 'true'",
    content, count=1
)

# 5. Update production-readiness
content = re.sub(
    r'(production-readiness:\n    name: .*?\n    needs: )pre-merge-gate(\n    runs-on: ubuntu-latest)',
    r"\1[changes, pre-merge-gate]\n    if: needs.changes.outputs.backend == 'true' || needs.changes.outputs.dependencies == 'true'\2",
    content, count=1
)

# 6. Update security-audit
content = re.sub(
    r'(security-audit:\n    name: .*?\n    runs-on: ubuntu-latest)',
    r"\1\n    needs: changes\n    if: github.event_name == 'schedule' || needs.changes.outputs.dependencies == 'true'",
    content, count=1
)

# 7. Update performance-e2e-test
content = re.sub(
    r'(performance-e2e-test:\n    name: .*?\n    needs: \[backend-core, frontend-core\]\n    runs-on: ubuntu-latest)',
    r"\1\n    if: github.event_name == 'pull_request' || (github.event_name == 'push' && github.ref == 'refs/heads/main')",
    content, count=1
)

# 8. Add [skip ci] to git commit messages
content = content.replace('git commit -m "fix(ci): Automatically apply lint fixes inline"', 'git commit -m "fix(ci): Automatically apply lint fixes inline [skip ci]"')
content = content.replace('git commit -m "style(ci): auto-fix formatting issues 🔧"', 'git commit -m "style(ci): auto-fix formatting issues 🔧 [skip ci]"')
content = content.replace('git commit -m "fix(ci): Automatically apply frontend lint fixes inline"', 'git commit -m "fix(ci): Automatically apply frontend lint fixes inline [skip ci]"')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Patching completed.")

```