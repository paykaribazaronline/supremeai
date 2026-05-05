# Project Guidelines

## Code Style

- Follow Spring Boot 3-layer flow: controller validates input, service owns business logic, data access stays in dedicated data/service helpers.
- Keep security/permission checks in service layer for sensitive operations, not only in controllers.
- Avoid hardcoded secrets, provider keys, setup tokens, and AI model IDs; read from environment or dynamic provider/config APIs.
- Prefer small, focused edits; do not refactor unrelated code in the same change.

## Architecture

- Core backend: src/main/java/org/example
- Primary layers:
  - controller: REST endpoints and request/response validation
  - service: business logic, routing, consensus, automation
  - model/config/exception/filter/security/audit: domain types and cross-cutting concerns
- Multi-surface product (feature parity expected):
  - React dashboard: dashboard/src
  - Flutter admin app: flutter_admin_app/lib
  - CommandHub: command-hub

## Build and Test

- Windows default commands:
  - Build (fast): .\gradlew.bat clean build -x test
  - Full test run: .\gradlew.bat test
  - Run backend: .\gradlew.bat bootRun
- Optional checks:
  - Coverage: .\gradlew.bat jacocoTestReport
  - Markdown lint/fix: npx markdownlint-cli --fix <file>
- If a change touches dashboard or Flutter, run local checks in that subproject before finalizing.

## Conventions

- Feature parity rule: if a user-facing capability exists in React dashboard, mirror it in Flutter admin app (and vice versa).
- Cloud-first rule: prefer cloud backend/config/providers over local-only fallbacks.
- Solo-capable rule: features must return meaningful output even if external AI providers are unavailable.
- Use safe process execution patterns (validated args, no shell injection shortcuts, capture stderr separately when relevant).

## Link Targets

- System overview and current status: MASTER_PROJECT_DOCUMENTATION.md
- Documentation index: docs/README.md
- Architecture details: docs/02-ARCHITECTURE/ARCHITECTURE_AND_IMPLEMENTATION.md
- Troubleshooting: docs/00-START-HERE/QUICKSTART_TROUBLESHOOTING.md
- Contributing standards: docs/12-GUIDES/CONTRIBUTING.md
- CommandHub details: command-hub/README.md
