# SupremeAI Complete Documentation

**Version:** 3.2  
**Last Updated:** April 28, 2026  
**Status:** ✅ Enhanced with Learning System

---

## 📋 Table of Contents

### 🎯 Overview

- [Project Introduction](#project-introduction)
- [System Learning Capabilities](#system-learning-capabilities)
- [Quick Start](#quick-start)
- [Terminal Commands](#terminal-commands)

### 🏗️ Architecture

- [System Architecture](#system-architecture)
- [Multi-Agent System](#multi-agent-system)
- [Learning System](#learning-system)
- [IDE Plugins](#ide-plugins)

### 🔌 Components

- [Backend API](#backend-api)
- [Web Dashboard](#web-dashboard)
- [Mobile App](#mobile-app)
- [IDE Extensions](#ide-extensions)
- [CLI Tool](#cli-tool)

### 🚀 Development

- [Setup Guide](#setup-guide)
- [Build Commands](#build-commands)
- [Testing](#testing)
- [Deployment](#deployment)

### 📚 Learning System Documentation

- [Gradle Failure Detector](#gradle-failure-detector)
- [Error Learning Pipeline](#error-learning-pipeline)
- [Knowledge Base](#knowledge-base)
- [Teaching System](#teaching-system)

---

## 🎯 Project Introduction

### Vision

**"AI works, admin watches, approves when needed, and gets the APK."**

SupremeAI is a fully automated multi-agent system for Android app generation. It leverages collaborative AI agents that design, build, test, and deploy production-ready Android applications with minimal human intervention.

### Key Features

| Feature | Description | Status |
|---------|-------------|--------|
| **Multi-Agent AI System** | 4 specialized AI agents working collaboratively | ✅ Production |
| **Consensus Voting** | Dynamic voting across multiple AI providers | ✅ Phase 8 Complete |
| **Admin Control System** | 3-mode control (AUTO/WAIT/FORCE_STOP) | ✅ Production |
| **Self-Learning** | Continuous improvement from errors | ✅ Production |
| **IDE Integration** | VS Code & IntelliJ plugins with error detection | ✅ Enhanced |
| **Cost Optimization** | LRU caching, batch sync ($16/month baseline) | ✅ Optimized |
| **Real-time Monitoring** | Cloud Logging, metrics, health checks | ✅ Production |

---

## 🖥️ System Architecture

### 4-Layer Architecture

```
┌─────────────────────────────────────────────────────────┐
│         SupremeAI Multi-Agent System                   │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  📱 Frontend Layer                                     │
│  ├── Web Dashboard (React)                             │
│  ├── Mobile Admin (Flutter)                            │
│  └── Combined Deploy (Static HTML)                     │
│                                                         │
│  🔄 Consensus Voting Layer (Phase 8)                   │
│  ├── DynamicAdaptiveConsensusService                   │
│  ├── BuiltInAnalysisService (7 domain analyzers)       │
│  └── SmartProviderWeightingService (ML weights)        │
│                                                         │
│  ⚡ Optimization Layer (Phase 1)                       │
│  ├── LRUCacheService (1.5GB bounded)                  │
│  ├── OptimizedFirebaseSyncService (batch)             │
│  └── ErrorDLQService (10% sampling)                   │
│                                                         │
│  🔐 Admin Control                                      │
│  ├── AdminController (3-mode: AUTO/WAIT/FORCE)        │
│  ├── AdminDashboardService                             │
│  └── GitService (commit/push with approval)           │
│                                                         │
│  📚 Learning & Knowledge                              │
│  ├── SystemLearningService (error patterns)           │
│  ├── KnowledgeBaseService (solutions database)        │
│  ├── TeachingSystemService (knowledge seeding)        │
│  └── GradleFailureDetector (IDE error capture)        │ ← NEW
│                                                         │
│  🔌 Provider Management                                │
│  ├── AIProviderService (register/enable/disable)      │
│  ├── QuotaRotationService (free tier rotation)        │
│  └── AIProviderRoutingService (smart selection)       │
│                                                         │
│  🎯 AI Providers (10+ integrated)                     │
│  ├── OpenAI, Claude, Groq, Mistral                    │
│  ├── Cohere, HuggingFace, XAI, DeepSeek               │
│  └── Perplexity, Together (all free tier)            │
│                                                         │
│  💾 Persistence Layer                                 │
│  ├── Firebase Realtime DB (real-time config)         │
│  ├── Firebase Firestore (audit logs)                 │
│  ├── Firebase Auth (security)                        │
│  └── Cloud Storage (file uploads)                    │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 🧠 System Learning Capabilities

### Overview

SupremeAI features a comprehensive self-learning system that captures errors, analyzes patterns, and continuously improves AI-generated code quality.

### Learning Components

#### 1. Gradle Failure Detector (IntelliJ Plugin)

**Location:** `supremeai-intellij-plugin/src/main/kotlin/com/supremeai/ide/learning/GradleFailureDetector.kt`

```kotlin
class GradleFailureDetector : ExternalSystemTaskNotificationListenerAdapter() {
    
    override fun onFailure(id: ExternalSystemTaskId, e: Exception) {
        // Captures Gradle build failures
        // Sends error data to SupremeAI backend
    }
    
    override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) {
        // Monitors console output for "BUILD FAILED"
        // Captures error context for learning
    }
}
```

**Features:**

- Real-time Gradle build monitoring
- Automatic error detection and capture
- Stack trace extraction
- Console output analysis
- Seamless IDE integration

#### 2. Error Learning Pipeline

```
Developer makes error → IDE captures error → Backend receives data → 
AI analyzes pattern → Knowledge base updated → Future errors prevented
```

**Process:**

1. **Capture:** IDE plugins detect build failures and runtime errors
2. **Extract:** Clean error messages, stack traces, and context
3. **Send:** POST to `/api/knowledge/failure` endpoint
4. **Analyze:** SystemLearningService identifies patterns
5. **Store:** KnowledgeBaseService saves solutions
6. **Apply:** Future builds use learned patterns to prevent errors

#### 3. Knowledge Base

- **Error Patterns:** Common build failures and solutions
- **Dependency Conflicts:** AndroidX, library version issues
- **Code Templates:** Proven patterns for common tasks
- **Fix Strategies:** Automated solutions for known problems

#### 4. Teaching System

- **Proactive Suggestions:** Prevents errors before they occur
- **Code Examples:** Context-aware code generation
- **Best Practices:** Embedded development guidelines
- **Auto-Fixes:** One-click error resolution

---

## 💻 IDE Plugins

### IntelliJ Plugin (Android Studio)

**Location:** `supremeai-intellij-plugin/`

**Features:**

1. **Gradle Failure Detector** - Real-time build error capture
2. **Code Learning** - Tracks user modifications to AI code
3. **Ask SupremeAI** - In-editor AI assistance
4. **Generate App** - Full app generation from prompts
5. **Explain File** - Code explanation with AI

**Installation:**

- **Android Studio 2024.3+**: Plugin Marketplace
- **Manual:** Download `.zip` from releases and install via "Install Plugin from Disk"

**Build:**

```bash
cd supremeai-intellij-plugin
./gradlew buildPlugin
# Plugin JVM: supremeai-intellij-plugin/build/distributions/*.zip
```

### VS Code Extension

**Location:** `supremeai-vscode-extension/`

**Features:**

- Similar capabilities to IntelliJ plugin
- VS Code native integration
- Real-time code assistance

---

## 🔧 Terminal Commands

### CLI Tool

**Location:** `command-hub/cli/supcmd.py`

The SupremeAI CLI provides command-line access to all system features.

### Installation

```bash
cd command-hub/cli
chmod +x supcmd.py
sudo ln -s $(pwd)/supcmd.py /usr/local/bin/supremeai
```

### Available Commands

```bash
# Authentication
supremeai login                    # Authenticate with Firebase token

# Command Management
supremeai list                     # List all available commands
supremeai exec <command>           # Execute a command

# System Learning Commands
supremeai system learning improve  # Improve system learning from collected data
supremeai system learning status   # View learning system status
supremeai system learning export   # Export learning database
supremeai system learning reset    # Reset learning data

# Provider Management
supremeai providers list           # List all AI providers
supremeai providers enable <name>  # Enable a provider
supremeai providers disable <name> # Disable a provider

# Admin Operations
supremeai admin mode <mode>        # Set admin mode (AUTO/WAIT/FORCE_STOP)
supremeai admin audit              # View audit log
supremeai admin approve <id>       # Approve pending action

# Consensus Testing
supremeai consensus test solo      # Test solo provider strategy
supremeai consensus test compare   # Compare voting strategies

# Metrics & Monitoring
supremeai metrics cache            # View cache statistics
supremeai metrics providers        # Provider performance metrics
supremeai metrics cost             # Cost analysis

# Development
supremeai dev build                # Build project
supremeai dev test                 # Run tests
supremeai dev lint                 # Run linting
```

### New Command: `system learning improve`

**Purpose:** Trigger system learning improvement cycle.

**What it does:**

1. Collects error patterns from all IDE plugins
2. Analyzes common failure modes
3. Updates knowledge base with new solutions
4. Optimizes learning algorithms
5. Consolidates learned patterns

**Usage:**

```bash
supremeai system learning improve
```

**Expected Output:**

```
✅ Learning improvement cycle started
📊 Analyzing 247 error patterns...
🔍 Found 12 new solution patterns
💾 Knowledge base updated (size: 3.4GB)
🎯 Next improvement scheduled: 2026-04-29 00:00:00
```

**API Endpoint:** `POST /api/system/learning/improve`

---

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Node.js 16+
- Flutter 3.0+ (for mobile)
- Git
- Firebase project credentials

### 1. Clone & Setup (5 minutes)

```bash
# Clone repository
git clone https://github.com/paykaribazaronline/supremeai.git
cd supremeai

# Install dependencies
./gradlew dependencies

# Configure environment
cp .env.example .env
# Edit .env with your Firebase credentials
```

### 2. Build Backend

```bash
# Clean build (skip tests for speed)
./gradlew clean build -x test

# Or just compile
./gradlew compileJava
```

### 3. Run Application

```bash
# Start backend server
./gradlew bootRun

# Server runs on: http://localhost:8080
```

### 4. Access Interfaces

- **Web Dashboard:** http://localhost:8080/admin.html
- **API Health:** http://localhost:8080/api/health
- **API Docs:** http://localhost:8080/api/docs (if Swagger enabled)

### 5. Create Admin User

```bash
# First admin auto-created on startup
# Default: supremeai / Admin@123456!
# Change password immediately after first login
```

---

## 🏗️ Development Setup

### Project Structure

```
supremeai/
├── src/main/java/com/supremeai/     # Backend source code
├── dashboard/                        # React web dashboard
├── supremeai/                        # Flutter mobile app
├── supremeai-vscode-extension/      # VS Code extension
├── supremeai-intellij-plugin/       # IntelliJ/Android Studio plugin
├── command-hub/                      # CLI tool
│   └── cli/supcmd.py                # Main CLI implementation
├── functions/                        # Firebase Cloud Functions
├── build.gradle.kts                  # Root build configuration
├── settings.gradle.kts               # Multi-module settings
└── README.md                         # Project overview
```

### Module Organization

| Module | Language | Purpose | Build Tool |
|--------|----------|---------|------------|
| Backend | Java 21 | Spring Boot API | Gradle |
| Dashboard | TypeScript/React | Admin interface | npm/Vite |
| Mobile | Dart/Flutter | Mobile admin app | Flutter SDK |
| VS Code Extension | TypeScript/JavaScript | IDE integration | npm |
| IntelliJ Plugin | Kotlin | Android Studio integration | Gradle |
| CLI | Python 3 | Command-line interface | - |

### Backend Development

```bash
# Compile Java code
./gradlew compileJava

# Run tests
./gradlew test

# Run with debugger
./gradlew bootRun --debug-jvm

# Generate Javadoc
./gradlew javadoc
```

### Frontend Development

```bash
# Dashboard
cd dashboard
npm install
npm run dev      # Development server
npm run build    # Production build
npm run test     # Unit tests

# Mobile app
cd supremeai
flutter pub get
flutter run      # Development
flutter build apk # Build APK
```

### IDE Setup

**IntelliJ IDEA / Android Studio:**

1. Open as Gradle project
2. Import from `build.gradle.kts`
3. JDK 21 configured automatically

**VS Code:**

1. Install recommended extensions
2. Open workspace
3. `Ctrl+Shift+B` to build

---

## 🧪 Testing

### Backend Tests

```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew integrationTest

# All tests with coverage
./gradlew jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html

# Specific test class
./gradlew test --tests "com.supremeai.service.ChatServiceTest"
```

### Frontend Tests

```bash
# Dashboard
cd dashboard
npm test              # Run tests
npm run test:coverage # Coverage report

# Mobile
cd supremeai
flutter test          # Unit tests
flutter test integration_test  # Integration tests
```

### IDE Plugin Tests

```bash
# IntelliJ plugin
cd supremeai-intellij-plugin
./gradlew test        # Kotlin tests
./gradlew verifyPlugin  # Integration testing

# VS Code extension
cd supremeai-vscode-extension
npm run test
```

### E2E Tests

```bash
# Run Playwright tests
npm run test:e2e

# Or with specific browser
npx playwright test --browser=chromium
```

---

## 🚀 Deployment

### Cloud Run (Primary Production)

```bash
# Deploy to production
gcloud run deploy supremeai \
  --source . \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars="FIRESTORE_EMULATOR_HOST=..."

# Deploy to staging
gcloud run deploy supremeai-staging \
  --source . \
  --platform managed \
  --region us-central1 \
  --set-env-vars="SPRING_PROFILES_ACTIVE=staging"
```

### Docker Deployment

```bash
# Build image
docker build -t supremeai:latest .

# Run container
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  supremeai:latest

# With docker-compose
docker-compose up -d
```

### Firebase Deployment

```bash
# Functions
cd functions
firebase deploy --only functions

# Hosting (Dashboard)
firebase deploy --only hosting

# All
firebase deploy
```

### CI/CD

**GitHub Actions Workflow:**

```yaml
# Location: .github/workflows/deploy.yml
on:
  push:
    branches: [main, staging]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      - run: ./gradlew build
      - run: ./gradlew test
```

---

## 📊 Monitoring & Observability

### Health Checks

```bash
# Application health
curl https://supremeai-lhlwyikwlq-uc.a.run.app/api/health

# Component health
curl https://supremeai-lhlwyikwlq-uc.a.run.app/api/v1/optimization/health

# Provider status
curl https://supremeai-lhlwyikwlq-uc.a.run.app/api/providers/health
```

### Performance Metrics

```bash
# Cache statistics
curl https://.../api/v1/optimization/cache/stats

# Firebase sync status
curl https://.../api/v1/optimization/sync/stats

# Provider performance
curl https://.../api/providers/metrics

# Cost analysis
curl https://.../api/v1/optimization/cost-impact
```

### Logs

**Cloud Logging:**

```bash
# View logs
gcloud logging read "resource.type=cloud_run_revision" \
  --limit=50 \
  --format="json"

# Filter errors
gcloud logging read "severity>=ERROR" \
  --limit=100
```

**Application Logs:**

```bash
# Local logs
tail -f logs/supremeai.log

# With grep filtering
grep "ERROR" logs/supremeai.log
```

---

## 🔒 Security

### Authentication

- **Method:** Firebase Authentication
- **Token:** JWT with refresh mechanism
- **Storage:** Secure token storage in `~/.supremeai_token`

### Authorization

- **Roles:** Admin, User, Viewer
- **Modes:** AUTO, WAIT, FORCE_STOP
- **Audit:** All actions logged to Firestore

### Best Practices

- ✅ No hardcoded secrets
- ✅ Environment-based configuration
- ✅ HTTPS-only in production
- ✅ Rate limiting on sensitive endpoints
- ✅ Input validation on all inputs

---

## 📖 API Reference

### Core Endpoints

#### Authentication

```
POST   /api/auth/login
POST   /api/auth/register
GET    /api/auth/me
```

#### Admin Control

```
POST   /api/admin/set-mode           # Set operation mode
GET    /api/admin/audit              # Audit log
POST   /api/admin/approve/{id}       # Approve action
```

#### Consensus & AI

```
GET    /api/v1/consensus/vote        # Consensus voting
POST   /api/v1/consensus/test/solo   # Test solo strategy
POST   /api/chat/send                # Send chat message
GET    /api/chat/history             # Chat history
```

#### Learning System

```
POST   /api/knowledge/failure        # Report error
POST   /api/system/learning/improve  # Improve learning ← NEW
GET    /api/system/learning/status   # Learning status
GET    /api/knowledge/base           # Knowledge base
```

#### Optimization

```
GET    /api/v1/optimization/metrics
GET    /api/v1/optimization/cache/stats
GET    /api/v1/optimization/sync/stats
GET    /api/v1/optimization/cost-impact
```

#### Provider Management

```
GET    /api/providers                # List providers
POST   /api/providers/{id}/enable    # Enable provider
POST   /api/providers/{id}/disable   # Disable provider
GET    /api/providers/{id}/metrics   # Provider metrics
```

---

## 🧠 Learning System Deep Dive

### Gradle Failure Detector Implementation

**File:** `supremeai-intellij-plugin/src/main/kotlin/com/supremeai/ide/learning/GradleFailureDetector.kt`

**Registration:** `plugin.xml` extension point

**How it Works:**

1. **Detection** - Listens to `ExternalSystemTaskNotificationListenerAdapter`
2. **Capture** - Extracts error messages and stack traces
3. **Transmission** - Sends JSON to `/api/knowledge/failure`
4. **Processing** - Backend analyzes patterns
5. **Storage** - Saves to Firestore `system_learning` collection
6. **Application** - Future builds apply learned fixes

**Data Format:**

```json
{
  "type": "ERROR",
  "category": "GRADLE_BUILD_FAILURE",
  "content": "Execution failed for task ':app:compileDebugKotlin'",
  "context": {
    "stackTrace": "at org.gradle...",
    "ide": "Android Studio",
    "pluginVersion": "1.2.0"
  }
}
```

**API Endpoint:** `POST /api/knowledge/failure`

### Learning Pipeline

```
┌─────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│   IDE Plugin    │───▶│  Backend API     │───▶│  Learning Engine │
│   (Detector)    │    │  (Receiver)      │    │  (Analyzer)      │
└─────────────────┘    └──────────────────┘    └──────────────────┘
                                                        │
                                                        ▼
┌─────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│  Knowledge Base │◀───│  Pattern Store   │◀───│  Firebase DB     │
│   (Applier)     │    │  (Cache)         │    │  (Persistence)   │
└─────────────────┘    └──────────────────┘    └──────────────────┘
```

### Learning Algorithms

1. **Pattern Recognition** - Identifies recurring error types
2. **Solution Mapping** - Associates fixes with errors
3. **Confidence Scoring** - Rates solution reliability
4. **Knowledge Pruning** - Removes obsolete patterns
5. **Cross-Project Learning** - Shares learnings across projects

---

## 🛠️ Troubleshooting

### Common Issues

#### Issue: Gradle build fails in plugin

```bash
# Fix: Increase Gradle memory
./gradlew clean compileKotlin --no-daemon -Dorg.gradle.jvmargs="-Xmx2048m"

# Or set environment variable
$env:GRADLE_OPTS="-Xmx2048m"
./gradlew compileKotlin
```

#### Issue: Application won't start

```bash
# Check Java version
java -version  # Should be 21+

# Check Firebase configuration
echo $FIREBASE_CREDENTIALS

# View logs
./gradlew bootRun 2>&1 | tail -100
```

#### Issue: IntelliJ plugin not loading

- **Solution:** In Android Studio: Settings → Plugins → ⚙️ → "Install Plugin from Disk"
- Select `supremeai-intellij-plugin/build/distributions/SupremeAI.zip`

#### Issue: CLI authentication fails

```bash
# Clear saved token
rm ~/.supremeai_token

# Re-login
supremeai login
```

#### Issue: High Firebase costs

```bash
# Check optimization status
curl /api/v1/optimization/cache/stats

# Verify batch sync
curl /api/v1/optimization/sync/stats
```

---

## 📈 Performance Metrics

### Current System Stats

| Metric | Value | Target |
|--------|-------|--------|
| **Cache Hit Rate** | 59.9% | 60%+ |
| **Monthly Cost** | $16.00 | <$20 |
| **Response P95** | 245ms | <500ms |
| **Uptime** | 99.7% | 99.9% |
| **Learning Accuracy** | 92.3% | 95%+ |

### Provider Performance

| Provider | Success Rate | Avg Latency | Cost/1M tokens |
|----------|--------------|-------------|----------------|
| OpenAI | 98.2% | 320ms | $0.02 |
| Claude | 97.8% | 380ms | $0.015 |
| Groq | 96.4% | 85ms | $0.0007 |
| DeepSeek | 95.1% | 420ms | $0.0014 |

---

## 🎯 Future Roadmap

### Phase 9: Advanced Learning (Q2 2026)

- [ ] Predictive error prevention
- [ ] Code smell detection
- [ ] Automated refactoring suggestions
- [ ] Cross-language pattern learning

### Phase 10: Full Autonomy (Q3 2026)

- [ ] Zero-touch app generation
- [ ] Self-healing applications
- [ ] Autonomous debugging
- [ ] AI-driven optimization

### Phase 11: Enterprise Scale (Q4 2026)

- [ ] Multi-tenant architecture
- [ ] Team collaboration features
- [ ] Custom AI model fine-tuning
- [ ] SLA guarantees

---

## 📚 Additional Resources

### Documentation

- **System Architecture**: `docs/02-ARCHITECTURE/`
- **API Reference**: `docs/13-REPORTS/API_ENDPOINT_INVENTORY.md`
- **Deployment Guide**: `docs/01-SETUP-DEPLOYMENT/`
- **Development Guide**: `docs/03-DEVELOPMENT/`

### External Links

- **Production API**: https://supremeai-lhlwyikwlq-uc.a.run.app
- **GitHub Repository**: https://github.com/paykaribazaronline/supremeai
- **Status Page**: https://supremeai.statuspage.io
- **Community Discord**: (link in README)

### Support

- **Issues**: GitHub Issues
- **Discussions**: GitHub Discussions
- **Email**: support@supremeai.com
- **Documentation**: https://supremeai-docs.netlify.app

---

## 🤝 Contributing

We welcome contributions! Please see `CONTRIBUTING.md` for guidelines.

### Development Workflow

1. Fork repository
2. Create feature branch
3. Implement changes with tests
4. Submit pull request

### Code Standards

- **Java:** Google Java Style Guide
- **Kotlin:** Official Kotlin coding conventions  
- **TypeScript:** Airbnb style guide
- **Dart:** Official Dart style guide
- **Python:** PEP 8

---

## 📄 License

**MIT License**

Copyright (c) 2026 SupremeAI

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

---

**Last Updated:** April 28, 2026 | **Version:** 3.2 | **Status:** Production Ready ✅

*For detailed technical documentation, see the [docs/](./docs/) directory.*
