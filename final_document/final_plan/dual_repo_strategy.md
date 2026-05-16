# SupremeAI Dual-Repository & Automated Workflow Strategy

This document outlines the architecture and strategy for managing automated system updates using a dual-repository setup and a GitHub App bot.

## 1. Repository Information

| Role | Repository URL | Remote Name |
| :--- | :--- | :--- |
| **Production (Main)** | `https://github.com/paykaribazaronline/supremeai.git` | `origin` |
| **AI Updates (System)** | `https://github.com/SaifulHaqueNiloy/supremeai.git` | `system-auto` |

## 2. Automated System Bot (GitHub App)

The system uses a dedicated GitHub App to perform automated code updates. This ensures all AI-driven changes are tracked under a specific bot identity.

- **App Name:** `supremeai-bot`
- **App ID:** `3300194`
- **Identity Badge:** Commits will appear with a "Bot" badge on GitHub.

### Authentication Details
- **Private Key:** Converted to **PKCS#8** format and stored in `.env` as `GITHUB_APP_PRIVATE_KEY`.
- **Installation ID:** Stored dynamically in Firebase Firestore under the user profile (Field: `githubInstallationId`).
- **Current Baseline ID:** `122412142` (Linked to User ID: 1).

## 3. Proposed Deployment Workflow (Phase 2 Plan)

The objective is to create a secure bridge between automated AI updates and the production environment.

### Step 1: AI Fix/Improvement
- `SelfHealingService` detects an error or an improvement opportunity.
- The system generates and perfects the code using the Multi-AI Voting mechanism.

### Step 2: Push to System Repository
- SupremeAI uses the `GitHubAppService` to get a short-lived token.
- The system pushes the new code to the **AI Updates Repo** (`SaifulHaqueNiloy/supremeai`).

### Step 3: CI Validation
- A GitHub Action in the **AI Updates Repo** is triggered.
- The action performs:
    - Code linting and syntax checks.
    - Unit and integration tests.
    - Security scans.

### Step 4: Cross-Repository Merge Trigger
- If the CI process passes (100% success), a **Pull Request (PR)** is automatically opened from the AI Updates repo to the **Production Repo**.
- *Optional:* An admin/human receives a notification to approve the PR, or it can be auto-merged if confidence is 101%.

### Step 5: Final Production Deploy
- Once the code is merged into the Production Repo (`origin/master`), the primary CI/CD pipeline triggers the actual deployment to **Google Cloud Run** and **Firebase Hosting**.

## 4. Required Actions for Implementation
- [ ] **Secrets Sync:** Copy all required Action Secrets (GCP keys, Firebase tokens, etc.) from the Old Repo to the New Repo.
- [ ] **Workflow Configuration:** Set up `.github/workflows/ai-validation.yml` in the New Repo.
- [ ] **Cross-Repo Token:** Configure a GitHub PAT with `repo` scope to allow the bot to create PRs across repositories.

---
**Status:** Architecture Configured | Workflow Pending Implementation.
**Last Updated:** May 15, 2026
