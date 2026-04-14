# SupremeAI - Master Project Documentation v3.1

**Last Updated:** April 13, 2026  
**Status:** ✅ **BUILD SUCCESSFUL** | Phase 1 Complete | Production Ready

> **AI-powered multi-agent system for automated Android app generation using Firebase, Google Cloud, and collaborative AI agents.**

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture & Layers](#architecture--layers)
3. [Current System Status](#current-system-status)
4. [Quick Start](#quick-start)
5. [Build & Configuration](#build--configuration)
6. [API Endpoints](#api-endpoints)
7. [Codebase Analysis & Cleanup](#codebase-analysis--cleanup)
8. [Development Guide](#development-guide)
9. [Deployment](#deployment)
10. [Troubleshooting](#troubleshooting)

---

## Project Overview

### Vision

> **"AI works, admin watches, approves when needed, and gets the APK."**

SupremeAI is a fully automated app development system where multiple AI agents collaborate to build, test, and deploy Android applications with minimal human intervention.

### Key Features

**Multi-Agent Intelligence System**

- X-Builder: Code generation and implementation
- Z-Architect: System design and optimization
- Consensus Engine: 70% approval requirement for major decisions
- King Mode: Admin override capability for critical decisions

**CommandHub - Complete Command Orchestration**

- Monitoring Commands: health-check, quota-status, metrics collection
- Data Refresh Commands: GitHub, Vercel, Firebase data synchronization
- REST API: Full integration with Spring Boot backend
- Python CLI Tool: `supcmd` for admin command execution
- Execution Engine: Sync & Async command processing with queue support

**Advanced Analytics & Monitoring**

- Real-time metrics streaming via WebSocket (2-second push intervals)
- Historical metrics persistence with Firestore
- Z-score anomaly detection (3-sigma analysis)
- Failure prediction using linear regression
- Auto-scaling recommendations based on resource utilization

**Multi-Channel Notifications**

- Email alerts with SMTP integration
- Slack notifications with color-coded embeds
- Discord alerts with severity indicators
- SMS via Twilio for critical alerts
- Severity-based escalation policies

**Authentication & Security**

- JWT token-based auth (24h access token, 7d refresh token)
- Secure password hashing (BCrypt)
- Admin-only user registration and promotion
- Firebase user storage with audit trails
- Browser auto-protection with login redirect

---

## Architecture & Layers

### Three-Layer Architecture

**Layer 0: AI Brain**

- Shared memory & performance scoreboard
- SystemLearning module (learns from errors & patterns)
- Multi-AI Consensus system (10 providers voting)

**Layer 1: Cloud Brain (Firebase)**

- Orchestration & Consensus engine
- Real-time database for project state
- Firestore collections: projects, api_providers, ai_agents, admin_logs

**Layer 2: AI Agents**

- X-Builder: Code generation
- Y-Reviewer: Code review & quality checks
- Z-Architect: System design & optimization

**Layer 3: App Generator**

- Template-based code generation
- Android/Flutter compilation
- APK signing & deployment

### Core Components

| Component | Purpose | Status |
|-----------|---------|--------|
| **AdminDashboard** | API key & AI agent management | ✅ Active |
| **MonitoringDashboard** | Real-time metrics & alerts | ✅ Active |
| **AuthenticationService** | JWT + user management | ✅ Active |
| **GitService** | Git operations & integration | ✅ Active |
| **FirebaseService** | Database persistence | ✅ Active |
| **AIProviderRoutingService** | Intelligent AI selection | ✅ Active |
| **QuotaService** | Resource allocation tracking | ✅ Active |
| **MetricsService** | Performance & usage tracking | ✅ Active |

---

## Current System Status

### Build Status (April 13, 2026)

```
✅ BUILD SUCCESSFUL (1m 31s)
✅ Compilation: 0 errors, 27 pre-existing warnings
✅ Tests: 81 integration tests
✅ Code Quality: Passing all critical checks
```

### Phase 1 - Critical Naming Fixes ✅ COMPLETE (2 hours)

**All critical naming violations fixed and verified:**

| Fix | Status | Details |
|-----|--------|---------|
| **HealthPingServiceService** → HealthPingService | ✅ Complete | File rename + class + logger updated |
| **EnterpriseResilienceOrchestratorServiceService** → EnterpriseResilienceOrchestratorService | ✅ Complete | File rename + class + controller synced (8 endpoints) |
| **suggestionService** → SuggestionService | ✅ Complete | File rename + class + controller updated |
| **ourService** | ✅ Deleted | Unused, empty file removed |
| **CICDService** | ✅ Updated | Removed incorrect @Deprecated annotation (still actively used) |

**Why CICDService Was NOT Deleted:**

- `CICDService`: Low-level local build/test execution (used by AgentOrchestrator, ProjectTypeManager)
- `CICDPipelineService`: High-level CI/CD pipeline orchestration
- These serve DIFFERENT purposes and both are needed

### Remaining Work

**Phase 2-12: Service Consolidation (Estimated 30-35 hours)**

| Phase | Focus | Est. Time | Status |
|-------|-------|-----------|--------|
| Phase 2 | Firebase consolidation (4 → 2 services) | 4 hours | 📋 Planned |
| Phase 3 | AI Provider Routing (5 → 2 services) | 5 hours | 📋 Planned |
| Phase 4 | Quota Management (4 → 2 services) | 4 hours | 📋 Planned |
| Phase 5 | Git Integration (5 → 2 services) | 4 hours | 📋 Planned |
| Phase 6 | Signing & Security (4 → 2 services) | 3 hours | 📋 Planned |
| Phase 7 | Metrics Collection (4 → 2 services) | 3 hours | 📋 Planned |
| Phase 8 | Learning & Seeding (4+ → 2 services) | 3 hours | 📋 Planned |
| Phase 9 | Consensus & Voting (3 → 1 service) | 2 hours | 📋 Planned |
| Phase 10 | Cost Intelligence (2 → 1 service) | 1 hour | 📋 Planned |
| Phase 11 | Internet Research (2 → 1 service) | 1 hour | 📋 Planned |
| Phase 12 | Final cleanup & testing | 2 hours | 📋 Planned |

### Codebase Metrics

- **Total Service Files:** 190+
- **Duplicates Identified:** 35+
- **Technical Debt:** 15-20% of codebase
- **Estimated LOC Reduction:** 70-100 KB
- **Build Time:** ~1m 30s
- **Test Coverage:** 81 integration tests

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
# Required variables:
# - FIREBASE_SERVICE_ACCOUNT
# - GEMINI_API_KEY
# - JWT_SECRET
# - BOOTSTRAP_TOKEN
# - GITHUB_TOKEN
```

### 3. Start Development Server

```bash
./gradlew run
```

**Access Points:**

- Admin Dashboard: http://localhost:8001
- Monitoring Dashboard: http://localhost:8000
- API Server: http://localhost:8080

### 4. First Admin Setup

1. Open http://localhost:8001
2. Login with SUPREMEAI_SETUP_TOKEN
3. Create first admin user (one-time setup)
4. Add AI providers
5. Create project and assign AI agents

---

## Build & Configuration

### Build Commands

```bash
# Full build with tests
./gradlew clean build

# Build without tests (faster)
./gradlew clean build -x test

# Run specific tests
./gradlew test --tests "ClassName"

# Build & run
./gradlew clean build && java -jar build/libs/supremeai.jar

# Watch for changes (development)
./gradlew watch
```

### Configuration Files

**Core Configuration:**

- `application.yml`: Main Spring Boot configuration
- `.env`: Environment variables
- `firebase.json`: Firebase project config
- `settings.gradle.kts`: Gradle settings

**Test Configuration:**

- `src/test/resources/application-test.properties`: Test-specific settings
- JWT Secret: `test-jwt-secret-key-for-testing-only-not-for-production`

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `FIREBASE_SERVICE_ACCOUNT` | Firebase service account JSON | Yes |
| `GEMINI_API_KEY` | Google Gemini API key | Yes |
| `OPENAI_API_KEY` | OpenAI API key | Yes |
| `JWT_SECRET` | JWT secret for token signing | Yes |
| `SUPREMEAI_SETUP_TOKEN` | One-time admin setup token | Yes |
| `GITHUB_TOKEN` | GitHub API token | Yes |
| `ADMIN_MODE` | AUTO, WAIT, or FORCE_STOP | No (defaults to AUTO) |

### Database Configuration

**Firebase Collections:**

```
projects/               # App projects
├── {projectId}/
│   ├── metadata
│   ├── code_generated
│   └── deployment_status

api_providers/         # Configured AI providers
├── {providerId}/
│   ├── name
│   ├── api_key
│   └── status

ai_agents/             # AI agent assignments
├── {agentId}/
│   ├── capability
│   ├── provider
│   └── performance_metrics

admin_logs/            # Audit trail
├── {logId}/
│   ├── user
│   ├── action
│   ├── timestamp
│   └── details
```

---

## API Endpoints

### Authentication

```
POST /api/auth/setup
  ⇒ Create first admin (SUPREMEAI_SETUP_TOKEN required, one-time only)
  
POST /api/auth/login
  ⇒ Admin login (returns JWT token)

POST /api/auth/refresh
  ⇒ Refresh JWT token

POST /api/auth/logout
  ⇒ Invalidate token
```

### Projects

```
GET  /api/projects
  ⇒ List all projects

POST /api/projects
  ⇒ Create new project

GET  /api/projects/{id}
  ⇒ Get project details

PUT  /api/projects/{id}
  ⇒ Update project

DELETE /api/projects/{id}
  ⇒ Delete project
```

### AI Providers

```
GET  /api/providers/available
  ⇒ List top 10 available AI providers

GET  /api/providers/configured
  ⇒ List active providers

POST /api/providers/add
  ⇒ Add new provider

POST /api/providers/test
  ⇒ Test provider connection

DELETE /api/providers/{id}
  ⇒ Remove provider
```

### Monitoring & Metrics

```
GET  /api/metrics/current
  ⇒ Real-time system metrics

GET  /api/metrics/history?days=7
  ⇒ Historical metrics

GET  /api/health
  ⇒ System health status

POST /api/commands/execute
  ⇒ Execute admin command
```

### CommandHub

```
POST /api/commands/health-check
  ⇒ Check system health

POST /api/commands/quota-status
  ⇒ Get quota usage

POST /api/commands/refresh-github
  ⇒ Sync GitHub data

POST /api/commands/refresh-firebase
  ⇒ Sync Firebase data
```

---

## Codebase Analysis & Cleanup

### Problem: Service Duplication

**Root Causes:**

1. Incremental changes created "Fixed" versions instead of updating originals
2. New features created new services instead of extending existing ones
3. Experimental features never cleaned up
4. No enforced naming standards or deprecation discipline

**Examples:**

- `FirebaseService.java` + `FirebaseServiceFixed.java` (duplicate with logging)
- `HealthPingServiceService` (double "Service" suffix violation)
- `suggestionService` (lowercase class name violation)
- 5 different AI routing services doing similar work

### Phase 1: Critical Naming Fixes ✅ COMPLETE

**Fixed Files:**

1. **HealthPingService** - File renamed ✅

   ```
   Before: HealthPingServiceService.java
   After:  HealthPingService.java
   Changes: Class name, logger reference
   ```

2. **EnterpriseResilienceOrchestratorService** - File renamed + controller sync ✅

   ```
   Before: EnterpriseResilienceOrchestratorServiceService.java
   After:  EnterpriseResilienceOrchestratorService.java
   Changes: Class name, logger, controller imports, @Autowired field, 8 endpoints
   Files modified: EnterpriseResilienceOrchestratorServiceController.java
   ```

3. **SuggestionService** - File renamed + controller sync ✅

   ```
   Before: suggestionService.java
   After:  SuggestionService.java
   Changes: Class name, logger, controller imports, @Autowired field, @RequestMapping
   Files modified: suggestionController.java
   ```

4. **ourService** - Deleted ✅

   ```
   Status: Unused, empty file
   Action: Safely removed
   ```

5. **CICDService** - Annotation corrected ✅

   ```
   Status: Marked @Deprecated but actively used
   Action: Removed incorrect @Deprecated annotation
   Reason: Different purpose from CICDPipelineService (low-level vs high-level)
   Usage: AgentOrchestrator.java, ProjectTypeManager.java
   ```

### Phase 2-12: Consolidation Plan

**Phase 2: Firebase (4 → 2 services) | 4 hours | Priority: HIGH**

| Current | Action | Merge Into |
|---------|--------|-----------|
| FirebaseService.java | KEEP | - |
| FirebaseServiceFixed.java | MERGE + DELETE | FirebaseService |
| OptimizedFirebaseSyncService.java | KEEP | - |
| ProjectAnalysisFirebaseService.java | REVIEW + DELETE/CONSOLIDATE | FirebaseService |

```
Consolidation Steps:
1. Copy logging & error handling from FirebaseServiceFixed to FirebaseService
2. Add retry logic and fallback handling
3. Delete FirebaseServiceFixed.java
4. Verify all imports
5. Run Firebase integration tests
```

**Phase 3: AI Provider Routing (5 → 2 services) | 5 hours | Priority: HIGH**

| Current | Action | Keep As |
|---------|--------|---------|
| AIProviderRoutingService.java | KEEP PRIMARY | - |
| AIProviderDiscoveryService.java | MERGE | AIProviderRoutingService |
| AICapabilityRouter.java | MERGE | AIProviderRoutingService |
| CapabilityBasedAIRoutingService.java | DELETE DUPLICATE | - |
| PublicAIRouter.java | EVALUATE | TBD |

**Phase 4: Quota Management (4 → 1-2 services) | 4 hours**

- Consolidate QuotaRotationService into QuotaService
- Evaluate per-user vs global separation

**Phase 5: Git Integration (5 → 2-3 services) | 4 hours**

- Consolidate GitIntegrationService into GitService
- Review GitHub-specific services

**Phase 6-12: Similar consolidations**

- Signing & Security, Metrics, Learning, Consensus, Cost Intelligence, Internet Research

---

## Development Guide

### Project Structure

```
supremeai/
├── src/main/java/org/example/
│   ├── service/              # Core business logic (190+ services)
│   ├── controller/           # REST API endpoints
│   ├── model/                # Data models
│   ├── config/               # Spring configuration
│   ├── util/                 # Utilities & helpers
│   └── firebase/             # Firebase integration
├── src/test/java/            # Integration tests (81 tests)
├── admin/                    # Admin dashboard (HTML/JS)
├── dashboard/                # Monitoring dashboard (Vue/TypeScript)
├── flutter_admin_app/        # Flutter mobile admin
├── command-hub/              # CommandHub CLI
├── docs/                     # Detailed documentation
└── colab/                    # Google Colab notebooks
```

### Code Style & Standards

**Naming Conventions:**

- ✅ ServiceName.java (NOT ServiceNameService.java)
- ✅ PascalCase for classes (NOT camelCase)
- ✅ Private fields with `_` prefix: `_logger`, `_service`
- ✅ Constants in UPPER_CASE
- ✅ Method names in camelCase

**Import & Organization:**

- Organize imports: java.*→ javax.* → org.springframework.*→ org.example.*
- Remove unused imports with markdownlint-cli before commit
- Use @Deprecated annotation only for actively moving away from code

**Logging:**

```java
private static final Logger _logger = LoggerFactory.getLogger(ClassName.class);

_logger.info("Operation started");
_logger.warn("Warning message");
_logger.error("Error occurred", exception);
```

**Testing:**

- Use @ActiveProfiles("test") for test configuration
- Test properties in: src/test/resources/application-test.properties
- 81 integration tests in src/test/java/org/example/

### Adding New Features

1. **Create Service Class**

   ```java
   @Service
   public class NewFeatureService {
       private static final Logger _logger = LoggerFactory.getLogger(NewFeatureService.class);
       
       public void execute() {
           // Implementation
       }
   }
   ```

2. **Create Controller Endpoint**

   ```java
   @RestController
   @RequestMapping("/api/new-feature")
   public class NewFeatureController {
       @Autowired
       private NewFeatureService newFeatureService;
       
       @PostMapping("/execute")
       public ResponseEntity<?> execute(@RequestBody Request req) {
           return ResponseEntity.ok(newFeatureService.execute());
       }
   }
   ```

3. **Update Tests**
   - Add integration test to src/test/java/
   - Use @ActiveProfiles("test")
   - Run: ./gradlew test

4. **Document Changes**
   - Update relevant .md files in docs/
   - Ensure markdown passes: npx markdownlint-cli --fix
   - Add code examples to documentation

### Running Tests

```bash
# All tests
./gradlew test

# Specific test
./gradlew test --tests "ClassName"

# Tests with coverage
./gradlew test jacocoTestReport

# Stop on first failure
./gradlew test --fail-fast
```

---

## Deployment

### Cloud Deployment (Cloud Run)

**Backend URL:** https://supremeai-565236080752.us-central1.run.app

```bash
# Build Docker image
docker build -f Dockerfile.production -t supremeai:latest .

# Push to Google Container Registry
gcloud builds submit --tag gcr.io/supremeai-565236080752/supremeai

# Deploy to Cloud Run
gcloud run deploy supremeai \
  --image gcr.io/supremeai-565236080752/supremeai \
  --platform managed \
  --region us-central1
```

### Local Docker Deployment

```bash
# Build image
docker build -f Dockerfile -t supremeai:dev .

# Run container
docker run -p 8080:8080 -p 8001:8001 -p 8000:8000 supremeai:dev

# Docker Compose (multi-service)
docker-compose up -d
```

### Environment Setup for Production

Create `.env.production`:

```
FIREBASE_SERVICE_ACCOUNT={json_key}
GEMINI_API_KEY={key}
OPENAI_API_KEY={key}
JWT_SECRET={long_random_string}
SUPREMEAI_SETUP_TOKEN={one_time_token}
GITHUB_TOKEN={token}
ADMIN_MODE=WAIT
```

### CI/CD Pipeline

**GitHub Actions Workflows:**

- `.github/workflows/java-ci.yml` - Build & test on every push
- `.github/workflows/deploy.yml` - Deploy to Cloud Run on main merge

**Automated Checks:**

- ✅ Java compilation (no errors)
- ✅ Tests pass (81 integration tests)
- ✅ Markdownlint (all .md files)
- ✅ Code quality (SonarQube)

---

## Troubleshooting

### Build Issues

**Problem:** Build fails with "cannot find symbol"

```
Solution: 
1. ./gradlew clean
2. Delete .gradle/ directory
3. ./gradlew build
```

**Problem:** Tests fail with PropertyPlaceholderHelper error

```
Solution:
1. Check src/test/resources/application-test.properties exists
2. Required: app.jwtSecret=test-jwt-secret-key-for-testing-only-not-for-production
3. Ensure test class has @ActiveProfiles("test")
```

**Problem:** "Type safety: RestTemplate.postForEntity() is not applicable"

```
Solution:
1. Use ResponseEntity without type parameter
2. Add @SuppressWarnings("rawtypes") above method
3. Cast result manually if needed
```

### Runtime Issues

**Problem:** Firebase connection fails

```
Solution:
1. Check FIREBASE_SERVICE_ACCOUNT environment variable
2. Verify firebase.json has correct project ID
3. Check Firebase project is active in Google Cloud Console
```

**Problem:** Admin dashboard shows "Not authenticated"

```
Solution:
1. Ensure SUPREMEAI_SETUP_TOKEN is set
2. First user creation: POST /api/auth/setup with token
3. Then login with admin credentials
4. Check JWT_SECRET is set and consistent
```

**Problem:** AI provider not responding

```
Solution:
1. Verify API key is correct in environment
2. Check rate limits (most providers have quotas)
3. Test provider connection: POST /api/providers/test
4. View logs for specific error message
```

### Performance Issues

**Problem:** Build takes too long

```
Solution:
1. Use: ./gradlew build -x test (skip tests)
2. Increase heap: GRADLE_OPTS="-Xmx4g"
3. Use --parallel flag: ./gradlew build --parallel
```

**Problem:** Dashboard is slow

```
Solution:
1. Check Firebase quota usage: GET /api/quota/firebase
2. Review metrics cleanup schedule
3. Clear old data: DELETE /api/metrics/before?date=2026-01-01
```

---

## Quick Reference

### Important URLs

| Service | URL | Port |
|---------|-----|------|
| API Server | http://localhost:8080 | 8080 |
| Admin Dashboard | http://localhost:8001 | 8001 |
| Monitoring Dashboard | http://localhost:8000 | 8000 |
| Production API | https://supremeai-565236080752.us-central1.run.app | 443 |

### Important Directories

| Directory | Purpose |
|-----------|---------|
| `src/main/java/org/example/service/` | Business logic (190+ services) |
| `src/main/java/org/example/controller/` | REST API endpoints |
| `src/test/java/` | Integration tests (81 tests) |
| `admin/` | Admin dashboard UI |
| `dashboard/` | Monitoring dashboard |
| `docs/` | Detailed documentation |

### Important Files

| File | Purpose |
|------|---------|
| `application.yml` | Spring Boot configuration |
| `firebase.json` | Firebase project config |
| `settings.gradle.kts` | Gradle settings |
| `build.gradle.kts` | Dependencies & build config |
| `.env` | Environment variables |

### Common Commands

```bash
# Build & Run
./gradlew build && java -jar build/libs/supremeai.jar

# Just Build
./gradlew build -x test

# Run Tests
./gradlew test

# Clean
./gradlew clean

# View Dependencies
./gradlew dependencies

# Run Single Test
./gradlew test --tests "SpecificTestClass"
```

---

## Contact & Support

- **GitHub:** https://github.com/paykaribazaronline/supremeai
- **Issues:** Report issues on GitHub Issues
- **Documentation:** See `/docs` directory for detailed guides
- **Contribution:** See [CONTRIBUTING.md](docs/CONTRIBUTING.md)

---

## License

MIT License - See [LICENSE](LICENSE) file for details

---

**Last Updated:** April 13, 2026 | Phase 1 Complete | Ready for Phase 2-12 Consolidation
