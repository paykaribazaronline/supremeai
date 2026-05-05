# Repository Guidelines

## Project Structure & Module Organization

This is a monorepo featuring a multi-agent system for automated app generation.

- **Backend**: Spring Boot 3 application located in `src/main/java/com/supremeai/`.
- **Dashboards**:
  - React/TypeScript 3D dashboard in `dashboard/`.
  - Admin dashboard at `public/` (static files).
- **Mobile**: Flutter-based admin application in `supremeai/`.
- **IDE Extensions**:
  - VS Code extension in `supremeai-vscode-extension/`.
  - IntelliJ plugin in `supremeai-intellij-plugin/`.
- **CLI**: Core command-hub in `command-hub/`.
- **Serverless**: Firebase Cloud Functions in `functions/`.

## Build, Test, and Development Commands

- **Environment**: Java 21 (Required for Virtual Threads).
- **Backend (Gradle)**:
  - Run: `./gradlew bootRun`
  - Build (skip tests): `./gradlew clean build -x test`
  - Test: `./gradlew test`
  - Coverage: `./gradlew jacocoTestReport`
- **Dashboard (Vite/React)**:
  - Dev: `npm run dev` (inside `dashboard/`)
  - Build: `npm run build`
  - Type-check: `npm run type-check`
- **VS Code Extension**:
  - Compile: `npm run compile` (inside `supremeai-vscode-extension/`)
  - Lint: `npm run lint`

## Coding Style & Naming Conventions

- **Spring Boot 3-Layer Flow**: Controller (validation) -> Service (business logic) -> Repository/Data (Firestore/Storage).
- **Security**: Always keep permission and security checks in the service layer, not just controllers.
- **Feature Parity**: Ensure any user-facing capability in the React dashboard is mirrored in the Flutter admin app.
- **Cloud-First**: Prioritize cloud backend and providers over local fallbacks.
- **Solo-Capable**: Features must remain functional with meaningful output even if external AI providers are unavailable.
- **No Hardcoded Secrets**: Read API keys and tokens from environment variables or dynamic config APIs.

## Testing Guidelines

- **Framework**: Use JUnit 5 and Mockito for backend tests.
- **Coverage**: Minimum 10% line coverage is enforced via JaCoCo.
- **Execution**: Tests run in parallel by default. Use `./gradlew test` to verify changes.

## Commit & Pull Request Guidelines

- **Commit Prefixes**: Use `feat:`, `fix:`, `docs:`, or `Cleanup:` prefixes for commit messages.
- **PRs**: Follow the template in `.github/pull_request_template.md`. Ensure tests pass before submitting.
