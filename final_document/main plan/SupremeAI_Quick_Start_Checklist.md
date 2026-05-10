
# SupremeAI - Quick Start Checklist

## Stack: Spring Boot 3 (Java 21) + React/TypeScript + Flutter + Firebase/Firestore

---

## Immediate Actions (This Week)

### Day 1-2: Project Setup
- [ ] Create new branch: `feature/major-restructure`
- [ ] Review folder structure as per AGENTS.md
- [ ] Ensure all packages have proper `package com.supremeai.*` declarations
- [ ] Create `.env.example` file with all required env vars
- [ ] Update `.gitignore` (add .env, build/, *.class, node_modules/)

### Day 3-4: Database / Firestore
- [ ] Verify Firestore collections: `users`, `api_keys`, `agents`, `tasks`, `knowledge`, `simulator_profiles`
- [ ] Check Firestore security rules in `database.rules.json`
- [ ] Verify `UserSimulatorProfileRepository` Firestore integration works end-to-end
- [ ] Test Firestore connection: `./gradlew bootRun` and call `GET /api/simulator/profile`

### Day 5-7: Core Modules Verification
- [ ] Verify `AgentOrchestrationHub.java` is wired correctly
- [ ] Verify `ApiKeyRotationService.java` rotation logic
- [ ] Verify `DataLifecycleService.java` scheduler triggers
- [ ] Run full test suite: `./gradlew test`

---

## Next Week: Integration

### Day 8-10: GitHub Integration
- [ ] Verify `GitHubWebhookController` handles push events
- [ ] Test dual-repo push logic (main repo vs user repos)
- [ ] Verify trust-tier based access in service layer

### Day 11-12: Testing
- [ ] Run: `./gradlew jacocoTestReport`
- [ ] Check coverage report at `build/reports/jacoco/test/html/index.html`
- [ ] Add tests for any service below 10% coverage (JaCoCo minimum enforced)
- [ ] Target: `./gradlew test` — all green

### Day 13-14: CI/CD
- [ ] Verify `.github/workflows/` pipelines run on push
- [ ] Check `cloudbuild.yaml` for Cloud Build config
- [ ] Verify Docker build: `docker build -t supremeai .`

---

## Week 3: Dashboard & Frontend

### React Dashboard (`dashboard/`)
- [ ] Run: `cd dashboard && npm install && npm run dev`
- [ ] Verify `SimulatorDashboard.tsx` loads at `/simulator`
- [ ] Add `SimulatorDashboard` route in `AdminLayout.tsx` or main router
- [ ] Run type-check: `npm run type-check`

### Flutter Admin App (`supremeai/`)
- [ ] Run: `cd supremeai && flutter pub get && flutter run`
- [ ] Verify feature parity with React dashboard (AGENTS.md requirement)

---

## Week 4: Polish & Deploy

### Pre-Production Checklist
- [ ] Code review all new services (DataLifecycleService, PlanCompatibilityService, etc.)
- [ ] Verify no hardcoded secrets (grep for API keys in code)
- [ ] Run: `./gradlew clean build -x test` — must succeed
- [ ] Update `README.md` with setup instructions

### Deployment
- [ ] Deploy to staging: `gcloud run deploy supremeai-staging`
- [ ] Run smoke tests against staging
- [ ] Merge to main and deploy production

---

## Build Commands Reference

```bash
# Backend (Java 21 / Spring Boot 3)
./gradlew bootRun                    # Start dev server
./gradlew clean build -x test        # Build (skip tests)
./gradlew test                       # Run all tests
./gradlew jacocoTestReport           # Coverage report

# Dashboard (Vite/React)
cd dashboard && npm install
npm run dev                          # Dev server
npm run build                        # Production build
npm run type-check                   # TypeScript check

# VS Code Extension
cd supremeai-vscode-extension && npm install
npm run compile                      # Compile extension
npm run lint                         # Lint check

# Flutter
cd supremeai && flutter pub get
flutter run                          # Run on device/emulator
flutter build apk                    # Android build
```

---

## Environment Variables Required

```properties
# Firebase / Firestore
FIREBASE_PROJECT_ID=
FIREBASE_SERVICE_ACCOUNT_KEY=  # JSON string or path

# AI Providers
OPENAI_API_KEY=
GEMINI_API_KEY=
ANTHROPIC_API_KEY=

# Security
API_ENCRYPTION_KEY=             # 32-char AES key
JWT_SECRET=

# Simulator
SIMULATOR_PREVIEW_DOMAIN=       # e.g., simulator.your-domain.com
SIMULATOR_PREVIEW_SCHEME=https

# Optional
REDIS_HOST=localhost
REDIS_PORT=6379
PORT=8080
```

---

## Success Criteria

- [ ] All 22 plans have corresponding service modules
- [ ] Firestore collections match schema in SupremeAI_Simulator_Controller_Plan.md
- [ ] Tests pass: `./gradlew test` green
- [ ] JaCoCo coverage >= 10% (enforced minimum)
- [ ] No hardcoded secrets in codebase
- [ ] React dashboard loads with SimulatorDashboard panel
- [ ] Flutter app feature parity with web dashboard
- [ ] CI/CD pipeline green on GitHub Actions

---

**Last updated:** 2026-05-04  
**Stack:** Java 21 / Spring Boot 3 / React+TypeScript / Flutter / Firebase Firestore
