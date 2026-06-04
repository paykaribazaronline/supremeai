# 🐙 SupremeAI: GitHub CI/CD & Automation Architecture

This document outlines the GitHub Actions workflows, secrets management, and testing pipelines used in SupremeAI.

## 1. System Architecture Overview

The following diagram illustrates the complete GitHub Actions architecture and how the CI/CD pipelines trigger, execute, and validate the system.

```mermaid
flowchart TD
    Developer[Developer Push/PR] --> Trigger{GitHub Events}
    Cron[Nightly Cron Schedule] --> Trigger
    
    Trigger -->|Push/PR to Main| PipelineA[AI Validation Pipeline]
    Trigger -->|Cron / Push to Config| PipelineB[E2E Tests Pipeline]
    
    subgraph AI Validation Pipeline
        direction TB
        A1[Setup JDK 21 & Gradle] --> A2[Start Redis Alpine Service]
        A2 --> A3[Build & Run Unit Tests]
        A3 --> A4[Start Spring Boot Backend]
        A4 --> A5[Execute AI Validation Script]
        A5 --> A6[Upload Test Reports Artifacts]
    end
    
    subgraph E2E Tests Pipeline
        direction TB
        B1[Setup Node 18 & JDK 21] --> B2[Install Playwright Chromium]
        B2 --> B3[Start Firebase Emulators & Backend]
        B3 --> B4[Start Vite Dashboard]
        B4 --> B5[Create Local Test User]
        B5 --> B6[Run Playwright E2E Tests]
        B6 --> B7[Upload E2E Report Artifacts]
    end
    
    PipelineA --> Result[GitHub Actions Success/Failure]
    PipelineB --> Result
    Result --> Deploy(Manual Deploy / Next Stage)
```

## 2. Workflows Step-By-Step

### 2.1 AI Validation Pipeline (`ai-validation.yml`)
Triggers on `push` and `pull_request` to `main`, `develop`, and `master` branches.

**Step-by-Step Execution:**
1. **Repository Checkout:** Clones the repo with depth 1.
2. **Environment Setup:** Configures JDK 21 (Temurin) and restores Gradle cache.
3. **Run Unit Tests:** Executes `./gradlew clean test`.
4. **Service Initialization:** Spins up a Redis container and starts the Spring Boot backend (`test` profile). It polls `localhost:8080/actuator/health` until healthy.
5. **AI Validation:** Runs `bash scripts/validate_ai.sh` using secrets (`POCKETLAB_URL` & GCP Credentials).
6. **Artifact Upload:** Uploads Gradle test reports.

### 2.2 End-to-End Tests (`e2e-tests.yml`)
Runs nightly via cron and on pushes to `dashboard/`, `functions/`, or `config/`.

**Step-by-Step Execution:**
1. **Environment Setup:** Sets up Node.js 18, JDK 21, and caches Playwright browsers.
2. **Dependencies Install:** Installs Dashboard dependencies and Playwright Chromium.
3. **Backend & Emulators:** Starts Spring Boot connected to local Firebase Auth and Firestore emulators.
4. **Frontend Dashboard:** Runs `npm run dev` to serve the Vite app on port 5173.
5. **Test Data Setup:** Creates a test user via Firebase Auth emulator REST API.
6. **E2E Testing:** Runs Playwright UI tests against the running local environment.
7. **Artifact Upload:** Uploads Playwright HTML report and backend logs.

## 3. Secrets & Variables
The pipelines securely inject the following GitHub Secrets:
- `POCKETLAB_URL`: Endpoint for AI pocket lab edge node.
- `GOOGLE_APPLICATION_CREDENTIALS`: GCP service account key for integrations.

## 4. Concurrency & Performance
Both workflows use `concurrency` groups mapping to the workflow name and Git branch (`${{ github.workflow }}-${{ github.ref }}`). If a new commit is pushed while a pipeline is running, `cancel-in-progress: true` stops the old run, saving valuable compute minutes.
