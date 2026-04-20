# SupremeAI - Multi-Agent App Generator System

**Status: ⚠️ Alpha (In Active Development)**

Welcome to SupremeAI! This repository contains a comprehensive multi-agent system for automated Android app generation. The platform is currently in **Alpha** phase and under active development. Expect breaking changes and incomplete features.

## 🌐 Environments

The system has a production endpoint (for testing only) and a local development environment:

- **Production (Test):** [https://supremeai-backend-565236080752.us-central1.run.app/admin.html](https://supremeai-backend-565236080752.us-central1.run.app/admin.html)
- **Local Development:** Follow setup instructions below.

## 📚 Documentation

All documentation is organized into the `docs_new/` directory:

- **Architecture** (`docs_new/architecture/`) - System design, technical specifications, and ADRs
- **Guides** (`docs_new/guides/`) - User guides, tutorials, contributing guidelines, and best practices
- **Reports** (`docs_new/reports/`) - Progress reports, verification documents, and analytics
- **Troubleshooting** (`docs_new/troubleshooting/`) - Debugging guides and issue resolution
- **Workflow** (`docs_new/workflow/`) - Project management, planning, and process documentation

Refer to `docs_new/guides/README.md` for the main documentation index.

## 🚀 Quick Start

1. Clone the repository
2. Set up Firebase credentials (see `docs_new/guides/01-SETUP-DEPLOYMENT/GOOGLE_CLOUD_DEPLOYMENT.md`)
3. Configure API keys in `src/main/resources/application.properties` or via environment variables
4. Run the app: `./gradlew bootRun`
5. Access admin dashboard at `http://localhost:8001`

## 🔧 Development

See `docs_new/guides/12-GUIDES/CONTRIBUTING.md` for development guidelines and contribution procedures.

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
