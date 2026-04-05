# Documentation Deduplication Report

Date: 2026-04-05

## Summary

This cleanup establishes `docs/` as the canonical documentation tree and reduces duplicate maintenance at repository root.

## Canonicalization Decisions

1. Root `ARCHITECTURE_AND_IMPLEMENTATION.md` -> redirect stub to `docs/02-ARCHITECTURE/ARCHITECTURE_AND_IMPLEMENTATION.md`
2. Root `CONFIG_QUICK_REFERENCE.md` -> redirect stub to `docs/01-SETUP-DEPLOYMENT/CONFIG_QUICK_REFERENCE.md`
3. Root `RESILIENCE_IMPLEMENTATION_SUMMARY.md` -> redirect stub to `docs/RESILIENCE_IMPLEMENTATION_SUMMARY.md`

## docs/ Structure Policy

- `docs/` is the source of truth for project documentation.
- Root docs with overlapping topics should remain thin redirect stubs only.
- New topic docs should be added once in the relevant `docs/<category>/` folder.
- Existing canonical docs must be updated in place rather than duplicated.

## Follow-up Candidates

- Review `README_UPDATED.md` and decide whether to merge into `README.md`. (pending)
- Review top-level `DOCUMENTATION_ORGANIZATION.md` and `DOCUMENTATION_MAINTENANCE_STRATEGY.md` for overlap with `docs/DOCUMENTATION_STANDARDS.md`. (pending)
- Add folder-level index pages where missing for large categories. (pending)

## Additional Progress (2026-04-05)

- Added `docs/10-IMPLEMENTATION/SERVICE_CONSOLIDATION_AND_VALIDATION_PLAN.md` as the implementation-side reference for service consolidation and validation rollout.
- CI/CD service path clarified: `CICDPipelineService` is canonical, `CICDService` marked deprecated.
