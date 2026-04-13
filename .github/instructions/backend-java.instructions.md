---
description: "Use when editing Spring Boot backend Java code in src/main/java or src/test/java. Enforces 3-layer architecture, service-level security checks, safe command execution, and no hardcoded secrets or AI model IDs."
name: "Backend Java Rules"
applyTo: ["src/main/java/**/*.java", "src/test/java/**/*.java"]
---
# Backend Java Rules

- Follow 3-layer flow: controller validates input, service owns business logic, and data access stays in dedicated helpers.
- Keep permission and admin-mode checks in service methods for sensitive operations, not only in controllers.
- Never hardcode secrets, provider keys, setup tokens, model names, model IDs, model URLs, or model versions.
- Read dynamic provider and model catalogs from backend or provider APIs; use cache only as fallback.
- Prefer cloud-first behavior; local-only fallback is allowed only when cloud is unavailable.
- Use safe process execution patterns: validated arguments, no shell-string concatenation shortcuts, and separate stderr capture.
- Validate critical environment variables before use, including GITHUB_TOKEN and SUPREMEAI_SETUP_TOKEN where required.
- For deprecations or removals, verify usages before deletion and preserve behavior unless explicitly changing it.
- Keep edits focused and minimal; avoid unrelated refactors in the same change.

## Build and Verify

- Fast compile check: .\gradlew.bat clean build -x test
- Full tests: .\gradlew.bat test
- Coverage (optional): .\gradlew.bat jacocoTestReport

## Link Targets

- Architecture: docs/02-ARCHITECTURE/ARCHITECTURE_AND_IMPLEMENTATION.md
- Master project doc: MASTER_PROJECT_DOCUMENTATION.md
- Troubleshooting: docs/00-START-HERE/QUICKSTART_TROUBLESHOOTING.md
- Contributing: docs/12-GUIDES/CONTRIBUTING.md
