# 🎮 SupremeAI Interactive Learning Tutorials

**Last Updated:** April 2, 2026
**Purpose:** Hands-on, step-by-step learning paths for every skill level

---

## 📋 How to Use This Guide

- Check off each step `[x]` as you complete it
- Time estimates are for a standard dev machine
- Prerequisites listed at the start of each track
- Each tutorial links to its relevant deep-dive document

---

## 🟢 BEGINNER TRACK — Get Up and Running

**Total time:** ~90 minutes
**Goal:** Deploy SupremeAI locally and generate your first app

### Tutorial 1: Clone and Run Locally (15 min)

> **Prereq:** Java 17+, Git, Firebase CLI

```bash
# 1. Clone the repo
git clone https://github.com/your-org/supremeai.git
cd supremeai

# 2. Copy environment template
cp ENVIRONMENT_CONFIGURATION.md .env.example   # read for variable names

# 3. Set required env vars
export GITHUB_TOKEN=your_token_here
export SUPREMEAI_SETUP_TOKEN=your_setup_token_here

# 4. Build
./gradlew build -x test

# 5. Run
./gradlew bootRun
```

**Checklist:**

- [ ] Repository cloned successfully
- [ ] Environment variables set (verify: `echo $GITHUB_TOKEN`)
- [ ] Build completes with `BUILD SUCCESSFUL`
- [ ] App starts on `http://localhost:8080`
- [ ] Health endpoint responds: `GET /actuator/health` → `{"status":"UP"}`

**Troubleshooting:** See [ENVIRONMENT_CONFIGURATION.md](ENVIRONMENT_CONFIGURATION.md)

---

### Tutorial 2: Configure Firebase (30 min)

> **Prereq:** Google account, Firebase project created

```bash
# 1. Install Firebase CLI
npm install -g firebase-tools

# 2. Login
firebase login

# 3. Initialize project (select Firestore + Hosting)
firebase init

# 4. Deploy Firestore rules
firebase deploy --only firestore:rules
```

**Checklist:**

- [ ] Firebase project created at console.firebase.google.com
- [ ] `firebase-test-creds.json` downloaded and placed in project root
- [ ] Firestore rules deployed successfully
- [ ] Collections created: `app_templates`, `generated_apps`, `generation_errors_and_fixes`
- [ ] Test connection: `GET /api/firebase/test` → success response

**Deep dive:** [FIREBASE_COLLECTIONS_SETUP.md](FIREBASE_COLLECTIONS_SETUP.md)

---

### Tutorial 3: Create First Admin User (15 min)

> **Prereq:** App running, SUPREMEAI_SETUP_TOKEN set

```bash
# Create default admin (one-time setup)
curl -X POST http://localhost:8080/api/auth/setup \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $SUPREMEAI_SETUP_TOKEN" \
  -d '{"username":"supremeai","password":"Admin@123456!"}'
```

**Checklist:**

- [ ] POST `/api/auth/setup` returns `201 Created`
- [ ] Login works: `POST /api/auth/login` with supremeai credentials
- [ ] JWT token received in response
- [ ] Admin dashboard accessible: `GET /api/admin/status`

**Security note:** The `/api/auth/init` endpoint is intentionally disabled. Always use the token-protected `/api/auth/setup`. See [COMMON_MISTAKES.md](COMMON_MISTAKES.md#mistake-1-using-open-auth-init-endpoint).

---

### Tutorial 4: Generate Your First App (30 min)

> **Prereq:** Admin user created, JWT token in hand

```bash
# Submit an app generation requirement
curl -X POST http://localhost:8080/api/extend/requirement \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "requirement": "create UserAuditService with methods: audit, log, export",
    "priority": "HIGH"
  }'

# Check generation status
curl http://localhost:8080/api/extend/status \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Checklist:**

- [ ] Requirement submitted successfully (201 response)
- [ ] Status endpoint shows generation in progress
- [ ] Generated service file appears in `src/main/java/org/example/`
- [ ] App recompiles without errors
- [ ] New endpoint available at `/api/useraudit/`

**Deep dive:** [HOW_TO_BUILD_APPS_FROM_PLANS.md](HOW_TO_BUILD_APPS_FROM_PLANS.md)

---

## 🔵 INTERMEDIATE TRACK — Master Admin Controls

**Total time:** ~120 minutes
**Goal:** Understand and use the 3-mode control system

### Tutorial 5: Admin Control Modes (20 min)

```bash
# Check current mode
curl http://localhost:8080/api/admin/mode \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Set to WAIT mode (approve all operations)
curl -X POST http://localhost:8080/api/admin/mode \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mode":"WAIT"}'

# Set to AUTO mode (instant execution)
curl -X POST http://localhost:8080/api/admin/mode \
  -d '{"mode":"AUTO"}' \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"

# Emergency stop
curl -X POST http://localhost:8080/api/admin/mode \
  -d '{"mode":"FORCE_STOP"}' \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

**Checklist:**

- [ ] AUTO mode: Generate an app and observe immediate execution
- [ ] WAIT mode: Submit requirement, observe it queued for approval
- [ ] Approve a queued operation from admin dashboard
- [ ] FORCE_STOP: Confirm all running operations halt within 5 seconds
- [ ] Audit trail visible: `GET /api/admin/audit` shows all mode changes

**Deep dive:** [ADMIN_CONTROL_COMPLETE_GUIDE.md](ADMIN_CONTROL_COMPLETE_GUIDE.md)

---

### Tutorial 6: Configure CI/CD Pipeline (45 min)

> **Prereq:** GitHub repository, Google Cloud account

**Checklist:**

- [ ] `cloudbuild.yaml` reviewed and understood
- [ ] GitHub Actions secrets set: `FIREBASE_TOKEN`, `GCP_SA_KEY`
- [ ] Push to `main` triggers workflow
- [ ] `docs-lint-fix.yml` auto-fixes markdown errors
- [ ] Build passes all quality gates
- [ ] Deployment to Cloud Run successful

**Deep dive:** [CICD_FIREBASE_SETUP.md](CICD_FIREBASE_SETUP.md)

---

### Tutorial 7: Monitor System Learning (30 min)

> **Goal:** See how SupremeAI learns from operations

```bash
# View learning dashboard
curl http://localhost:8080/api/learning/stats \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# View critical requirements
curl http://localhost:8080/api/learning/critical \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# View solutions by category
curl http://localhost:8080/api/learning/solutions/security \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Checklist:**

- [ ] Stats endpoint shows error count and confidence scores
- [ ] Critical requirements list includes security constraints
- [ ] Generate an intentional error, verify it's captured in learning system
- [ ] Confidence score for known error > 0.85
- [ ] Solution auto-applied on second occurrence of same error

**Deep dive:** [KNOWLEDGE_LEARNING_ARCHITECTURE.md](KNOWLEDGE_LEARNING_ARCHITECTURE.md)

---

### Tutorial 8: Multi-AI Consensus (25 min)

> **Goal:** Query the configured AI providers and analyze voting results

```bash
# Ask a question across all configured AIs
curl -X POST http://localhost:8080/api/consensus/ask \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"question":"What is the best database for this use case?"}'

# View consensus history
curl http://localhost:8080/api/consensus/history \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# View learning metrics from AI voting
curl http://localhost:8080/api/consensus/stats \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Checklist:**

- [ ] Consensus request returns votes from all configured providers
- [ ] Winning answer identified with vote percentage
- [ ] History shows past consensus decisions
- [ ] Stats show which AI providers agree most often
- [ ] Learning system updated with new consensus knowledge

---

## 🔴 ADVANCED TRACK — Scale to Production

**Total time:** ~195 minutes
**Goal:** Run SupremeAI in a production-grade environment

### Tutorial 9: Add a New AI Agent (60 min)

> **Goal:** Extend SupremeAI with a custom AI provider

**Steps:**

1. Review `MultiAIConsensusService.java` — understand the provider interface
2. Create a new provider class implementing the consensus contract
3. Register it in the admin-configured provider pool
4. Test that it contributes votes via `POST /api/consensus/ask`
5. Verify its results appear in `GET /api/consensus/history`

**Checklist:**

- [ ] New provider class created in `src/main/java/org/example/consensus/`
- [ ] Provider added to the configured provider registry
- [ ] Unit test written for the new provider
- [ ] Integration test: new provider votes appear in consensus results
- [ ] Learning system records the new provider's performance
- [ ] CI/CD passes with new provider included

**Reference:** [ARCHITECTURE_AND_IMPLEMENTATION.md](ARCHITECTURE_AND_IMPLEMENTATION.md)

---

### Tutorial 10: Production Deployment on Cloud Run (45 min)

```bash
# Build Docker image
docker build -t supremeai:latest .

# Test locally first
docker run -p 8080:8080 \
  -e GITHUB_TOKEN=$GITHUB_TOKEN \
  -e SUPREMEAI_SETUP_TOKEN=$SUPREMEAI_SETUP_TOKEN \
  supremeai:latest

# Deploy to Cloud Run
./deploy-cloud-run.sh
```

**Checklist:**

- [ ] Docker build completes without errors
- [ ] Local container passes health check
- [ ] Cloud Run service deployed to GCP
- [ ] Custom domain configured (optional)
- [ ] Minimum instances set to prevent cold starts
- [ ] Memory limit ≥ 2GB for SOLO mode, ≥ 8GB for MULTI-AI mode
- [ ] Secrets injected via GCP Secret Manager (not env vars)
- [ ] Auto-scaling configured: min=1, max=10

**Deep dive:** [PRODUCTION_DEPLOYMENT_GUIDE.md](PRODUCTION_DEPLOYMENT_GUIDE.md)

---

### Tutorial 11: Performance Tuning (90 min)

> **Goal:** Optimize for MULTI-AI mode (50-100 apps/day)

**Checklist:**

- [ ] Review current quota: `GET /api/admin/quota`
- [ ] Firebase indexing configured for high-frequency queries
- [ ] Connection pooling enabled in `application.properties`
- [ ] Memory cleanup threshold set at 7GB
- [ ] CPU throttling configured at 85% usage
- [ ] Queue size limit set to 50 requests
- [ ] Rate limiting active: 500 requests/hour for MULTI-AI mode
- [ ] Distributed tracing enabled for performance profiling
- [ ] Load test: simulate 5 concurrent app generations
- [ ] All benchmarks ≥ MULTI-AI targets from [QUOTA_CONFIG.properties](QUOTA_CONFIG.properties)

**Deep dive:** [DISTRIBUTED_TRACING_FAILOVER.md](DISTRIBUTED_TRACING_FAILOVER.md)

---

## 🛠️ QUICK REFERENCE COMMANDS

```bash
# Health check
curl http://localhost:8080/actuator/health

# Admin mode status
curl http://localhost:8080/api/admin/mode -H "Authorization: Bearer $TOKEN"

# Learning stats
curl http://localhost:8080/api/learning/stats -H "Authorization: Bearer $TOKEN"

# Generate an app
curl -X POST http://localhost:8080/api/extend/requirement \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"requirement":"YOUR_REQUIREMENT"}'

# Check Git status
curl http://localhost:8080/api/git/status -H "Authorization: Bearer $TOKEN"

# Emergency stop
curl -X POST http://localhost:8080/api/admin/mode \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"mode":"FORCE_STOP"}'
```

---

## 📊 Learning Progress Tracker

| Track      | Tutorials | Your Progress |
|------------|-----------|---------------|
| Beginner   | 1-4       | [ ] [ ] [ ] [ ] |
| Intermediate | 5-8     | [ ] [ ] [ ] [ ] |
| Advanced   | 9-11      | [ ] [ ] [ ]   |

**After completing all tracks:** You are ready to contribute to SupremeAI's codebase. See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

---

*Part of the SupremeAI documentation suite. Report issues via GitHub Issues.*
