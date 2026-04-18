# CI/Lint Hardening - Priority Execution Plan

## DO FIRST (Day 1 - 7 min)

| Priority | Task | Impact | Time |
|----------|------|--------|------|
| 1 | Remove `continue-on-error: true` from java-ci.yml | Lint bypass stops | 5m |
| 2 | Fix PR comment logic (add event check) in docs-lint-fix.yml | Prevent CI errors | 2m |

## DO SECOND (Day 2-3 - 25 min)

| Priority | Task | Impact | Time |
|----------|------|--------|------|
| 3 | Add changed-files fast lint strategy | Faster feedback | 15m |
| 4 | Setup branch protection rules | System-level enforcement | 10m |

## DO THIS WEEK (5 min)

| Priority | Task | Impact | Time |
|----------|------|--------|------|
| 5 | Deploy pre-commit hook setup | Local error catching | 5m |

## OPTIONAL (Later)

| Priority | Task | Impact | Time |
|----------|------|--------|------|
| 6 | Observability dashboard | Trend tracking | 1h |

---

**Total Time: 45 minutes for full hardening**
