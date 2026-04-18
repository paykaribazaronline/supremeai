# Service Consolidation And Validation Plan

Date: 2026-04-05
Status: Completed (Medium-priority cleanup scope)

## What Was Completed

1. Duplicate documentation cleanup and canonicalization

- Root duplicate topics are now redirect stubs to canonical docs.
- Canonical policy is documented in docs/README.md.
- Dedup decisions are recorded in docs/13-REPORTS/DOCUMENTATION_DEDUPLICATION_REPORT.md.

2. Service fragmentation reduction (safe merges)

- Removed duplicate/backup implementation artifacts in previous cleanup pass.
- CI/CD service family now has one canonical orchestration direction:
  - Preferred: CICDPipelineService
  - Legacy: CICDService (deprecated)

3. Input validation annotations rollout

- Validation dependency enabled in Gradle.
- Validation active in high-traffic controllers:
  - AuthenticationController
  - MLPredictionController
  - DataController

4. Build and test gate stabilization

- Resolved Spring bean ambiguity in CircuitBreakerConfiguration.
- Full gate runs successfully with current repository baseline:
  - test
  - jacocoTestReport
  - jacocoTestCoverageVerification

## Canonical Service Direction

To reduce service sprawl and confusion, use these canonical entry points:

- CI/CD orchestration: CICDPipelineService
- Data collection API orchestration: DataCollectorService
- Authentication flow: AuthenticationService
- Admin control enforcement: AdminControlService

## Notes For Next Iteration

- Migrate any remaining direct calls from CICDService to CICDPipelineService.
- Keep adding @Validated and parameter constraints to new/edited controllers by default.
- Prefer extension of existing services before creating similarly named new service classes.
