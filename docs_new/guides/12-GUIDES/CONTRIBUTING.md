# Contributing Guide

## Getting Started

### Prerequisites

- Java 17+
- Gradle 8.x
- Git

### Setup

1. Clone the repository:

```bash
git clone https://github.com/paykaribazaronline/supremeai.git
cd supremeai
```

2. Configure Firebase credentials (see [Google Cloud Deployment Guide](01-SETUP-DEPLOYMENT/GOOGLE_CLOUD_DEPLOYMENT.md))

3. Set up API keys in `src/main/resources/application.properties`:

```properties
OPENAI_API_KEY=sk-...
ANTHROPIC_API_KEY=sk-ant-...
GOOGLE_API_KEY=...
```

4. Run the application:

```bash
./gradlew bootRun
```

5. Access at `http://localhost:8001`

## Development Workflow

### Code Style

- Follow Spring Boot 3-layer architecture:
  - **Controller**: Validates input, returns responses
  - **Service**: Business logic, routing, consensus
  - **Repository/Data**: Data access operations
- Keep security/permission checks in the service layer
- Never hardcode secrets or API keys
- Make small, focused changes

### Build Commands

```bash
# Fast build (skip tests)
./gradlew clean build -x test

# Run all tests
./gradlew test

# Run with coverage
./gradlew jacocoTestReport

# Run locally
./gradlew bootRun
```

### Architecture Rules

- **Feature parity**: If a capability exists in React dashboard, mirror in Flutter admin (and vice versa)
- **Cloud-first**: Prefer cloud backend over local-only fallbacks
- **Solo-capable**: Features must work even if external AI providers are unavailable

## Project Structure

| Directory | Purpose |
|-----------|---------|
| `src/main/java/com/supremeai/` | Backend (Spring Boot) |
| `supremeai-vscode-extension/` | VS Code extension |
| `supremeai-intellij-plugin/` | IntelliJ plugin |
| `command-hub/` | CLI commands |
| `dashboard/` | React dashboard |
| `docs_new/` | Documentation |

## Pull Request Process

1. Create a feature branch from `main`
2. Make your changes
3. Run tests: `./gradlew test`
4. Update documentation if needed
5. Submit PR with clear description of changes

## Documentation

- All documentation lives in `docs_new/`
- Architecture decisions go in `docs_new/architecture/`
- User guides go in `docs_new/guides/`
- Reports go in `docs_new/reports/`

## Communication

- Use GitHub Issues for bug reports and feature requests
- Use Pull Requests for code changes
- Follow the [pull request template](../.github/pull_request_template.md)
