# SupremeAI - Multi-Agent App Generator System

**Status: ⚠️ Alpha (In Active Development)**

Welcome to SupremeAI! This repository contains a comprehensive multi-agent system for automated Android app generation. The platform is currently in **Alpha** phase and under active development. Expect breaking changes and incomplete features.

## 📊 Feature Status

| Feature | Status | Notes |
|---------|--------|-------|
| **Backend API** | ✅ Working | Spring Boot controllers, agent orchestration, learning system |
| **Admin Dashboard** | ✅ Working | Available at `/admin.html` and localhost:8001 |
| **Multi-Agent System** | ⚠️ Partial | X-Builder, Z-Architect, and 11 providers configured - coverage varies |
| **Android App Generator** | ⚠️ Partial | Core pipeline exists, needs end-to-end verification |
| **IntelliJ Plugin** | ✅ Working | K2 mode analysis implemented, v1.2.0 built successfully |
| **VS Code Extension** | ⚠️ In Progress | Extension scaffolded, needs completion |
| **Authentication** | ⚠️ Partial | Basic auth infrastructure present, needs hardening |
| **Provider Coverage** | ⚠️ Partial | Multiple API providers supported - configure keys in `application.properties` |
| **Self-Healing/Resilience** | ⚠️ Partial | Circuit breakers, recovery config, watchdog health checks exist |
| **ML/Analytics** | ⚠️ Partial | Vector database, prediction models, analytics controllers present |
| **K8s/Docker Deployment** | ✅ Available | Dockerfile, cloudbuild.yaml, k8s-service.yaml configured |

## 🌐 Environments

The system has a production endpoint (for testing only) and a local development environment:

- **Production (Test):** [https://supremeai-lhlwyikwlq-uc.a.run.app/admin.html](https://supremeai-lhlwyikwlq-uc.a.run.app/admin.html)
- **Local Development:** Follow setup instructions below.

## 📚 Documentation

All documentation is organized into the `docs_new/` directory:

- **[Documentation Index](docs_new/README.md)** - Main documentation hub
- **[API Endpoints](docs_new/guides/API_ENDPOINTS.md)** - Complete REST endpoint reference
- **[Multi-Agent System](docs_new/architecture/MULTI_AGENT_SYSTEM.md)** - Agent architecture and status
- **[Provider Coverage](docs_new/guides/PROVIDER_COVERAGE.md)** - AI provider configuration guide
- **[IDE Plugins](docs_new/guides/IDE_PLUGINS_STATUS.md)** - VS Code & IntelliJ plugin status
- **[URL Redirects](docs_new/guides/URL_REDIRECT_SETUP.md)** - Old URL migration guide
- **[Contributing Guide](docs_new/guides/12-GUIDES/CONTRIBUTING.md)** - How to contribute

### Documentation Structure

| Directory | Purpose |
|-----------|---------|
| `docs_new/architecture/` | System design, technical specifications, ADRs |
| `docs_new/guides/` | User guides, setup, tutorials, contributing |
| `docs_new/reports/` | Progress reports, verification, analytics |
| `docs_new/troubleshooting/` | Debugging guides, common issues |
| `docs_new/workflow/` | Project management, planning, processes |

## 🚀 Quick Start

1. Clone the repository
2. Set up Firebase credentials (see [Setup Guide](docs_new/guides/01-SETUP-DEPLOYMENT/GOOGLE_CLOUD_DEPLOYMENT.md))
3. Configure API keys in `src/main/resources/application.properties` or via environment variables
4. Run the app: `./gradlew bootRun`
5. Access admin dashboard at `http://localhost:8001`

**Quick Links:**

- [API Endpoints](docs_new/guides/API_ENDPOINTS.md) - All available REST endpoints
- [Provider Setup](docs_new/guides/PROVIDER_COVERAGE.md) - Configure AI providers
- [IDE Plugins](docs_new/guides/IDE_PLUGINS_STATUS.md) - VS Code & IntelliJ setup

## 🔧 Development

See [Contributing Guide](docs_new/guides/12-GUIDES/CONTRIBUTING.md) for development guidelines and contribution procedures.

### Quick Commands

```bash
# Build (skip tests)
./gradlew clean build -x test

# Run tests
./gradlew test

# Run locally
./gradlew bootRun
```

**Related Documentation:**

- [IDE Plugins](docs_new/guides/IDE_PLUGINS_STATUS.md) - VS Code & IntelliJ setup
- [Multi-Agent System](docs_new/architecture/MULTI_AGENT_SYSTEM.md) - Agent architecture
- [Contributing Guide](docs_new/guides/12-GUIDES/CONTRIBUTING.md) - Full development guidelines

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
