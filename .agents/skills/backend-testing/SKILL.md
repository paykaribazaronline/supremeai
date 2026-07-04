---
name: backend-testing
description: How to set up, run, and test the SupremeAI backend (FastAPI + Poetry) locally. Use when running backend unit tests, importing/booting the app, or verifying backend changes.
---

# Backend testing (SupremeAI)

The backend lives in `backend/` and uses **Poetry** (Python 3.12).

## Setup
```bash
cd backend
poetry install --with dev --without ml   # full dep set (fastapi, loguru, litellm, ...)
```
Plain `pip install -r requirements.txt` does NOT fully provision the env — use Poetry.

## Required env vars
`core.config.Settings` validates `ENV` — it must be one of
`production | staging | local | test` (NOT `development`). Importing the app also
reads an encryption key. For local runs/tests use:
```bash
export ENV=local
export PYTHONPATH="$PWD"          # from inside backend/
export SUPREMEAI_ENCRYPTION_KEY="CwE60g_bA67m-mock-encryption-key-padded-len="  # mock, from CI
```
(These mirror the `SUPREMEAI Core CI` workflow env.)

## Running tests
```bash
cd backend
ENV=local PYTHONPATH=$PWD poetry run python -m pytest -q
# single file:
ENV=local PYTHONPATH=$PWD poetry run python -m pytest tests/test_output_validator.py -q
```
Note: repo CI runs `ruff check . --fix` and `ruff format .` as an auto-fix step, and
`pytest ... --cov=core --cov-fail-under=25`. `ruff check` (no format) is the gate that
must be clean; `ruff format` currently reports diffs repo-wide (pre-existing).

## Known blocker — full app cannot boot
`poetry run python -c "import main"` currently fails with a PRE-EXISTING, unrelated
error: `ImportError: cannot import name 'LongTermMemory' from memory.long_term_memory`
(the class is not defined in that module). Until that is fixed, the FastAPI server
cannot start and HTTP/Swagger-UI E2E testing is not possible — test backend logic by
importing individual modules (e.g. `core.output_validator`, `core.error_remediation`)
and calling functions directly.

## Before/after comparison technique
To compare `main` vs a feature branch for specific files without a second checkout:
```bash
git checkout main -- <file>      # temporarily load main's version into the worktree
# ... run the probe/test ...
git checkout HEAD -- <file>      # restore the branch version
```

## Config note
`EnhancedConfidenceScorer`/`OutputValidator` load `backend/config/constitutional_rules.json`
(hallucination patterns + confidence penalties). If that file is missing the ruleset is
empty and hallucination detection is silently disabled.
