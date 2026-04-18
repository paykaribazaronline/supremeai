# Repository Operations Reference

This reference documents the non-source operational assets now organized outside the repository root.

## Folder Layout

- `scripts/` - operational automation and utility scripts (`.ps1`, `.sh`, `.py`, `.bat`)
- `logs/` - runtime and test logs
- `logs/build-artifacts/` - build and test output snapshots (`.txt`)
- `data/` - generated analysis and exported data files (`.csv`)
- `public/` - static web assets and admin UI pages

## Scripts Guide

### Deployment Scripts

- `scripts/deploy-cloud-run.ps1`
- `scripts/deploy-cloud-run.sh`
- `scripts/deploy-to-cloudrun.ps1`
- `scripts/deploy-to-gcp.ps1`
- `scripts/deploy-supremeai-a.sh`

### Learning and Auth Scripts

- `scripts/activate_learning.ps1`
- `scripts/BOOTSTRAP_AND_TEST.ps1`
- `scripts/bootstrap_learn_test.ps1`
- `scripts/run_learning_test.ps1`
- `scripts/trigger_learning_now.ps1`
- `scripts/auth_and_learn.ps1`
- `scripts/direct_login.ps1`

### Documentation and Repository Scripts

- `scripts/enforce-doc-layout.ps1` - local root-doc policy checker/fixer
- `.github/scripts/enforce-doc-layout.sh` - CI root-doc policy checker/fixer
- `.github/scripts/doc-maintenance.sh` - markdown scan/fix batch utility
- `scripts/analyze_duplicates.ps1`
- `scripts/generate-analysis-reports.ps1`

### Flutter CI/CD Support

- `scripts/setup-flutter-cicd.ps1`
- `scripts/setup-flutter-cicd.sh`
- `scripts/verify-flutter-deployment.ps1`
- `scripts/verify-flutter-deployment.sh`

### Validation and Test Utilities

- `scripts/test_learning.ps1`
- `scripts/test_learning_app.ps1`
- `scripts/test_learning_live.bat`
- `scripts/test_resilience_endpoints.ps1`
- `scripts/test_tokens.ps1`
- `scripts/read_coverage.py`

## Static Asset Reference

### Public Pages

- `public/index.html`
- `public/dashboard.html`
- `public/admin-console.html`
- `public/login.html`
- `public/monitoring-dashboard.html`
- `public/COMPARISON_SUPREMEAI_INTERACTIVE.html`

## Maintenance Rules

1. Keep root folder minimal: build config + core project metadata only.
2. Put new operational scripts under `scripts/`.
3. Put generated logs under `logs/`.
4. Put generated text outputs under `logs/build-artifacts/`.
5. Put generated exports under `data/`.
6. Keep documentation under `docs/` and enforce with:
   - `scripts/enforce-doc-layout.ps1 check`
   - `.github/scripts/enforce-doc-layout.sh check`
