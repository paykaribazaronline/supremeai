# SupremeAI - Multi-Agent App Generator System v3.1

> **AI-powered multi-agent system for automated Android app generation using Firebase, Google Cloud, and collaborative AI agents.**

[![Java CI Build & Test](https://github.com/paykaribazaronline/supremeai/actions/workflows/java-ci.yml/badge.svg)](https://github.com/paykaribazaronline/supremeai/actions/workflows/java-ci.yml)
[![Firebase Hosting](https://img.shields.io/badge/Firebase-Active-green?style=flat-square&logo=firebase)](https://github.com/paykaribazaronline/supremeai)
[![Java](https://img.shields.io/badge/Java-17-blue?style=flat-square&logo=oracle)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen?style=flat-square&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)
[![Contributions Welcome](https://img.shields.io/badge/Contributions-Welcome-brightgreen?style=flat-square)](docs/12-GUIDES/CONTRIBUTING.md)
[![Code Coverage](https://codecov.io/gh/paykaribazaronline/supremeai/branch/main/graph/badge.svg)](https://codecov.io/gh/paykaribazaronline/supremeai)

---

## Vision

> **"AI works, admin watches, approves when needed, and gets the APK."**

SupremeAI is a fully automated app development system where multiple AI agents collaborate to build, test, and deploy Android applications with minimal human intervention.

---

## Key Features

### Multi-Agent Intelligence System

- **X-Builder:** Code generation and implementation
- **Z-Architect:** System design and optimization
- **Consensus Engine:** 70% approval requirement for major decisions
- **King Mode:** Admin override capability for critical decisions

### CommandHub - Complete Command Orchestration (NEW)

- **Monitoring Commands:** health-check, quota-status, metrics collection
- **Data Refresh Commands:** GitHub, Vercel, Firebase data synchronization
- **REST API:** Full integration with Spring Boot backend
- **Python CLI Tool:** `supcmd` for admin command execution
- **Execution Engine:** Sync & Async command processing with queue support

### Advanced Analytics & Monitoring

- Real-time metrics streaming via WebSocket (2-second push intervals)
- Historical metrics persistence with Firestore
- Z-score anomaly detection (3-sigma analysis)
- Failure prediction using linear regression
- Auto-scaling recommendations based on resource utilization

### Multi-Channel Notifications

- Email alerts with SMTP integration
- Slack notifications with color-coded embeds
- Discord alerts with severity indicators
- SMS via Twilio for critical alerts
- Severity-based escalation policies

### Core Architecture

- **Layer 0: AI Brain** - Shared memory & performance scoreboard
- **Layer 1: Cloud Brain (Firebase)** - Orchestration & Consensus engine
- **Layer 2: AI Agents** - X-Builder, Y-Reviewer, Z-Architect
- **Layer 3: App Generator** - Template-based code generation

---

## Quick Start

### Prerequisites

- **Java:** JDK 17+ ([Download](https://www.oracle.com/java/technologies/downloads/))
- **Gradle:** 8.7+ (bundled with project)
- **Firebase Account:** Free tier suffices
- **Git:** Latest version

### 1. Clone & Setup

```bash
# Clone repository
git clone https://github.com/paykaribazaronline/supremeai.git
cd supremeai

# Build project
./gradlew clean build

# Run tests
./gradlew test
```

### 2. Configure Environment

```bash
# Copy example environment file
cp .env.example .env

# Edit with your credentials
# Required: FIREBASE_SERVICE_ACCOUNT, GEMINI_API_KEY, JWT_SECRET, BOOTSTRAP_TOKEN
```

See [Environment Variables Reference](docs/01-SETUP-DEPLOYMENT/ENVIRONMENT_VARIABLES_REFERENCE.md) for complete configuration.

### 3. Start Development Server

```bash
# Run Spring Boot application
./gradlew bootRun

# Application starts at http://localhost:8080
# Admin dashboard: http://localhost:8080/admin
# Monitoring dashboard: http://localhost:8080/monitoring
```

### 4. Deploy

```bash
# Firebase Hosting (automatic via GitHub Actions)
git push origin main

# Google Cloud (manual trigger)
gcloud builds submit

# Docker (local testing)
docker build -t supremeai:latest .
docker run -p 8080:8080 supremeai:latest
```

---

## Implementation Phases

| Phase | Status | Features |
|-------|--------|----------|
| **1** | Complete | Foundation, Auth, Admin |
| **2** | Complete | Intelligence & Ranking |
| **3** | Complete | App Generator |
| **4** | Complete | Monitoring |
| **4.1** | Complete | WebSocket, Real-time |
| **5** | Complete | Analytics & ML |
| **6-7** | In Progress | Viz & Automation |
| **8-10** | Planned | Full Automation |

---

## Documentation

### Getting Started

- [Quick Start (5 Minutes)](docs/00-START-HERE/QUICK_START_5MIN.md)
- [Developer Onboarding](docs/12-GUIDES/DEVELOPER_ONBOARDING.md)
- [Architecture Overview](docs/02-ARCHITECTURE/ARCHITECTURE_AND_IMPLEMENTATION.md)

### Configuration & Deployment

- [Environment Variables](docs/01-SETUP-DEPLOYMENT/ENVIRONMENT_VARIABLES_REFERENCE.md)
- [Production Deployment](docs/01-SETUP-DEPLOYMENT/PRODUCTION_DEPLOYMENT_GUIDE.md)
- [Firebase Setup](docs/01-SETUP-DEPLOYMENT/FIREBASE_COLLECTIONS_SETUP.md)

### API & Development

- [API Endpoint Inventory](docs/13-REPORTS/API_ENDPOINT_INVENTORY.md)
- [Contributing Guide](docs/12-GUIDES/CONTRIBUTING.md)
- [Glossary of Terms](docs/12-GUIDES/GLOSSARY.md)

### Troubleshooting

- [Common Mistakes](docs/09-TROUBLESHOOTING/COMMON_MISTAKES.md)
- [Troubleshooting Guide](docs/00-START-HERE/QUICKSTART_TROUBLESHOOTING.md)
- [Security Guide](docs/05-AUTHENTICATION-SECURITY/SECURITY_GUIDE.md)

### Complete Documentation Index

See [docs/README.md](docs/README.md) for the full documentation index (107+ documents).

---

## API Reference

**Analytics:** 8 endpoints | **Notifications:** 8 endpoints | **ML:** 6 endpoints
**Monitoring:** 42+ endpoints | **Performance:** 16+ endpoints

See [API Endpoint Inventory](docs/13-REPORTS/API_ENDPOINT_INVENTORY.md) for detailed API specs.

---

## Security

Never commit:

- `.env` files with secrets
- `service-account.json` credentials
- API keys or encryption keys

**See:** [Security Guide](docs/05-AUTHENTICATION-SECURITY/SECURITY_GUIDE.md) | [Security Audit](docs/05-AUTHENTICATION-SECURITY/SECURITY_AUDIT_REPORT.md)

---

## Contributing

See [CONTRIBUTING.md](docs/12-GUIDES/CONTRIBUTING.md) for guidelines.

---

## License

MIT License - [LICENSE](LICENSE)

---

## Status (April 2026)

**Phases 1-5:** Complete (9,000+ LOC)  
**Build:** SUCCESS | **Deployments:** Active

---

"AI works, Admin watches, Approve when needed, Get the APK"

---

**Last Updated:** April 5, 2026
