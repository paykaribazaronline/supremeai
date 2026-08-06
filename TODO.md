# SupremeAI 2.0 — Phases 13–17 Audit TODO

## Phase 14 — Dependency / Supply Chain Audit
- [x] Run `pip-audit` on backend dependencies → **54 CVEs in 9 packages** (aiohttp, cryptography, ecdsa, httplib2, litellm, pillow, pyasn1, pydantic-settings, python-dotenv)
- [x] Check GitHub Actions workflows for unpinned actions → **AUDIT-006 confirmed** (all `@vX` tags, no SHA-pin)

## Phase 14.5 — RBAC & Data Privacy
- [x] Verify tenant isolation in multi-tenant queries → **Clean** (insight_mage, churn_prophet, cost_guard use `_tenant_id`/hard-isolated routes)
- [x] Review PII log-masking audit → **AUDIT-017 FIXED**: `multi_account_rotator.py` no longer logs raw OTP codes & verification links in plaintext

## Phase 14.75 — LLM Cost Guard Validation
- [x] Validate `cost_guard.py` tier enforcement → **FINDING**: `validate_budget()`/`record_spend()` only used in tests, NOT wired into task_router/llm routing
- [x] Confirm `check_budget()` wiring → used in `llm_gateway.py` + `lifespan.py`
- [x] Run cost guard tests → **35 passed** (66% coverage on cost_guard.py)

## Phase 13.5 — Cross-App API Contract Testing
- [x] Compare backend API schemas vs studio-client usage → **FINDING AUDIT-018**: Broken contract endpoints
  - `/api/voice/voices` (chatService.getVoices) — voice router only has `/stream_audio`
  - `/api/skills/catalog` (skillsService) — skills router NOT registered in routers.py
  - `/api/files/` (useSupremeStore) — no such endpoint
  - `/api/v1/media/generate-upload-url` — exists ✅ (media router mounted at "")
  - `/api/session/{id}/stream` — exists ✅ (session_stream.py)

## Phase 15 — Docs vs Code Consistency
- [ ] Cross-verify key docs claims vs actual code

## Phase 16 — E2E Tests
- [ ] Run full `pytest --cov=backend` (note: subset gave 15% coverage, fail-under=38)

## Phase 17 — Rollback Plan Documentation
- [x] Create `docs/operations/rollback-plan.md` (blue/green deployment procedure)

## Final
- [ ] Update master log with findings
</content>
