# SupremeAI - Environment Setup & Configuration Guide

**Version**: 1.0
**Last Updated**: 2026-04-29
**Target System**: Local Development + Cloud Deployment (Firebase + Cloud Run)

---

## Table of Contents

1. [Current Environment Status](#1-current-environment-status)
2. [Prerequisites & Tools](#2-prerequisites--tools)
3. [Local Development Setup](#3-local-development-setup)
4. [Environment Variables](#4-environment-variables)
5. [Firebase Configuration](#5-firebase-configuration)
6. [Database Setup](#6-database-setup)
7. [Cloud Functions Deployment](#7-cloud-functions-deployment)
8. [Backend (Spring Boot)](#8-backend-spring-boot)
9. [Dashboard (React)](#9-dashboard-react)
10. [Mobile (Flutter)](#10-mobile-flutter)
11. [IDE Extensions](#11-ide-extensions)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. Current Environment Status

### System Information

```
OS: Windows 11 (via compatibility, git bash working)
Platform: win32 (Windows PowerShell 5.1)
Project Root: C:\Users\Nazifa\supremeai
Git Repository: Yes (Git Bash recommended for *nix commands)
Time Zone: Asia/Dhaka (UTC+6)
Current Date: Wednesday, April 29, 2026
```

### Project Structure

```
supremeai/
├── backend/                    (NOT PRESENT - using root Gradle)
├── src/main/java/              (Spring Boot backend)
├── src/main/resources/         (Config files, static HTML/JS)
├── dashboard/                  (React TypeScript admin panel)
├── supremeai/                  (Flutter mobile app)
├── supremeai-vscode-extension/ (VS Code extension)
├── supremeai-intellij-plugin/  (IntelliJ plugin)
├── functions/                  (Firebase Cloud Functions)
├── public/                     (Static Firebase hosting files)
├── database.rules.json         (Firestore security rules)
├── firebase.json               (Firebase hosting config)
├── build.gradle.kts            (Gradle build script)
└── settings.gradle.kts         (Gradle settings)
```

---

## 2. Prerequisites & Tools

### Required Software

| Tool | Minimum Version | Recommended | Purpose |
|------|----------------|-------------|---------|
| **Java JDK** | 21 | 21.0.3+ | Spring Boot 3 + Virtual Threads |
| **Gradle** | 8.x | 8.5+ | Build tool (wrapper included) |
| **Node.js** | 18.x | 20.x LTS | React dashboard |
| **npm** | 9.x | 10.x | Package manager |
| **Firebase CLI** | 13.x | Latest | Deploy functions/hosting |
| **Git** | 2.x | Latest | Version control |
| **Docker** | 20.x | Latest | Containerization (optional) |
| **Google Cloud SDK** | 410.x | Latest | Cloud Run deployment |

### Install Commands

**Windows PowerShell (as Administrator)**:

```powershell
# Java JDK 21
winget install OpenJDK.OpenJDK.21

# Node.js 20 LTS
winget install OpenJS.NodeJS.LTS

# Git
winget install Git.Git

# Firebase CLI
npm install -g firebase-tools

# Google Cloud SDK
# Download installer from: https://cloud.google.com/sdk/docs/install
# Or use winget:
winget install Google.CloudSDK

# Docker Desktop
winget install Docker.DockerDesktop
```

**Verify Installations**:

```bash
java -version       # Should show '21'
node --version      # Should show 'v20.x'
npm --version       # Should show '10.x'
firebase --version  # Should show '13.x'
gcloud --version    # Should show '410.x'
git --version       # Should show '2.x'
docker --version    # Should show '20.x'
```

---

## 3. Local Development Setup

### 3.1: Clone Repository

```bash
# If not already cloned
git clone https://github.com/your-org/supremeai.git
cd supremeai
```

### 3.2: Configure Firebase Local Emulation

**Start Firebase emulators** (for local dev without cloud):

```bash
# In project root
firebase emulators:start --only functions,firestore,hosting

# This starts:
# - Firestore emulator on localhost:8080
# - Functions emulator on localhost:5001
# - Hosting emulator on localhost:5000
# - Auth emulator on localhost:9099
```

**Emulator UI**: http://localhost:4000

**Stop emulators**: `Ctrl+C`

### 3.3: Configure Java Backend

**Step 1**: Navigate to project root

```bash
cd supremeai  # Already here
```

**Step 2**: Create local environment file

```bash
# Copy example env file
cp .env.example .env

# Edit .env with your values
notepad .env  # Windows
# OR
code .env      # VS Code
```

**Step 3**: Set required environment variables in `.env`:

```bash
# Firebase
FIREBASE_PROJECT_ID=supremeai-a
FIREBASE_DATABASE_URL=https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app

# API Keys (get from respective platforms)
STEPFUN_API_KEY=sf-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
GROQ_API_KEY=gsk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
ANTHROPIC_API_KEY=sk-ant-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
GEMINI_API_KEY=AIza-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
MISTRAL_API_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# JWT Secret (generate random 256-bit key)
JWT_SECRET=$(openssl rand -base64 32)

# Optional: Backend URL for Cloud Functions to call
BACKEND_URL=http://localhost:8080
```

**Step 4**: Generate encryption key for API key storage

```bash
# Generate 32-byte base64 key
openssl rand -base64 32 > encryption.key

# Add to .env
echo "API_ENCRYPTION_KEY=$(cat encryption.key)" >> .env
```

### 3.4: Initialize Firebase in Project

```bash
# Login to Firebase
firebase login

# Link project to Firebase
firebase use --add

# Select project: supremeai-a
# Alias: dev (or staging/prod)
```

### 3.5: Grant Firebase Admin Permissions Locally

**Option A: Use Service Account Key** (recommended)

1. Go to Firebase Console: https://console.firebase.google.com
2. Project Settings → Service Accounts → Generate new private key
3. Save as `firebase-service-account.json` in project root
4. Set environment variable:

```bash
export GOOGLE_APPLICATION_CREDENTIALS="C:/Users/Nazifa/supremeai/firebase-service-account.json"
```

**Option B: Use `gcloud auth`** (if using Cloud SDK)

```bash
gcloud auth application-default login
```

### 3.6: Start Backend Server

```bash
# Compile and run
./gradlew bootRun

# OR run without tests (faster)
./gradlew bootRun -x test

# Expected output:
# 2026-04-29 00:05:05.123  INFO 12345 --- [  main] o.s.boot.SpringApplication               : SupremeAI started in 4.5 seconds (JVM: Java 21)
# 2026-04-29 00:05:05.456  INFO 12345 --- [  main] c.s.SupremeAiApplication                  : Server started on port 8080
```

**Access backend**:

- API Base URL: http://localhost:8080
- Health check: http://localhost:8080/actuator/health
- API docs: http://localhost:8080/swagger-ui.html (if enabled)

---

## 4. Environment Variables

### 4.1: Complete `.env` Template

Create `.env` file in project root:

```bash
# ========================================
# SupremeAI Environment Configuration
# Copy .env.example to .env and fill in values
# ========================================

# ---------- Firebase ----------
FIREBASE_PROJECT_ID=supremeai-a
FIREBASE_DATABASE_URL=https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app
FIREBASE_STORAGE_BUCKET=supremeai-a.firebasestorage.app
FIREBASE_EMULATOR_HOST=localhost:8080  # Set ONLY for local emulation

# ---------- API Keys (Required for AI providers) ----------
# Get free keys from respective platforms
STEPFUN_API_KEY=sf-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
GROQ_API_KEY=gsk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
ANTHROPIC_API_KEY=sk-ant-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
GEMINI_API_KEY=AIza-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
MISTRAL_API_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
HUGGINGFACE_API_KEY=hf-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
KIMI_API_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# ---------- Security ----------
# Generate with: openssl rand -base64 32
API_ENCRYPTION_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
JWT_SECRET=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# ---------- Backend ----------
# Port for local dev (default: 8080)
PORT=8080

# Backend URL (for Cloud Functions to call)
BACKEND_URL=http://localhost:8080

# ---------- GitHub Integration ----------
GITHUB_APP_ID=3300194
GITHUB_APP_CLIENT_ID=Iv23liZY31q8QhovvzQt
GITHUB_APP_INSTALLATION_ID=121998309
# Base64-encoded private key (get from GitHub App settings)
GITHUB_APP_PRIVATE_KEY_BASE64=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# ---------- Google Cloud ----------
# For Cloud Run deployment
GCP_PROJECT_ID=supremeai-a
GCP_REGION=us-central1

# ---------- Feature Flags ----------
FEATURE_V2_RANKING_ALGORITHM=false
FEATURE_AUTO_ROTATION=true
FEATURE_COST_OPTIMIZATION=true

# ---------- Quota Overrides (Optional) ----------
# Override daily quotas per provider (tokens/day)
GROQ_DAILY_QUOTA=14400
DEEPSEEK_DAILY_QUOTA=1000
STEPFUN_DAILY_QUOTA=50000

# ---------- Admin Test Token (Local only) ----------
SUPREME_ADMIN_TEST_TOKEN=dev-admin-token-local
# WARNING: Change this in production!
```

### 4.2: Environment Variables by Deployment Stage

**Local Development** (`.env` file):

```bash
SPRING_PROFILES_ACTIVE=local
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
```

**Staging** (Cloud Run env vars):

```bash
SPRING_PROFILES_ACTIVE=staging
CORS_ALLOWED_ORIGINS=https://staging.supremeai.com
STEPFUN_API_KEY=sf-staging-key
# ... other keys
```

**Production** (Cloud Run env vars):

```bash
SPRING_PROFILES_ACTIVE=production
CORS_ALLOWED_ORIGINS=https://supremeai.com,https://app.supremeai.com
STEPFUN_API_KEY=sf-production-key
# ... rotate keys regularly
```

---

## 5. Firebase Configuration

### 5.1: Initialize Firebase Project

```bash
# Login
firebase login

# Initialize project (if not done)
firebase init

# Select features:
# - Firestore: Configure security rules & indexes
# - Functions: Set up Cloud Functions
# - Hosting: Configure hosting
# - Storage: Set up Firebase Storage
```

### 5.2: Firestore Database Rules

**File**: `database.rules.json`

✅ **Already secured** (as of 2026-04-29):

```json
{
  "rules": {
    "projects": {
      "$projectId": {
        ".read": "auth != null",
        ".write": "auth != null && auth.token.admin === true"
      }
    },
    "requirements": {
      ".read": "auth != null",
      ".write": "auth != null && auth.token.admin === true"
    },
    "config": {
      ".read": "auth != null",
      ".write": "auth != null && auth.token.admin === true"
    }
    // ... other rules
  }
}
```

**Deploy rules**:

```bash
firebase deploy --only firestore:rules
```

### 5.3: Firebase Storage Setup

```bash
# Create storage bucket (if not exists)
# Already created: supremeai-a.firebasestorage.app

# Set CORS for storage (optional)
gsutil cors set cors.json gs://supremeai-a.firebasestorage.app
```

### 5.4: Firebase Authentication Setup

1. Go to Firebase Console → Authentication
2. Enable **Email/Password** provider
3. Enable **Google** provider (optional)
4. Set authorized domains:
   - localhost (for dev)
   - supremeai-a.web.app (Firebase hosting)
   - your custom domain

### 5.5: Firebase Admin SDK Path

The backend uses Application Default Credentials:

```java
// Spring Boot auto-detects credentials from:
// 1. GOOGLE_APPLICATION_CREDENTIALS env var
// 2. gcloud auth application-default login
// 3. Cloud Run / Cloud Functions default service account

FirebaseOptions options = FirebaseOptions.builder()
    .setCredentials(GoogleCredentials.getApplicationDefault())
    .setDatabaseUrl("https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app")
    .build();
```

---

## 6. Database Setup

### 6.1: Firestore Collections

Collections auto-created on first write:

- `users` - User accounts
- `projects` - Active projects
- `requirements` - User requirements
- `config` - System configuration
- `ai_pool` - AI agent pool
- `chat_history` - Chat messages
- `notifications` - Push notifications
- `audit_logs` - Audit trail
- `quota` - User quota tracking
- `api_providers` - Provider configurations
- `user_api_keys` - Encrypted user API keys

### 6.2: Initial Admin User

Create admin user manually or via script:

```bash
# Using Firebase Auth CLI
firebase auth:import users.json --hash-algo=SCRYPT

# Or create via Admin Dashboard UI:
# 1. Start backend: ./gradlew bootRun
# 2. Open: http://localhost:8080/admin/users
# 3. Create user, then set admin flag in Firestore:
# Collection: users/{uid}
# Field: isAdmin = true
# Field: role = "admin"
```

### 6.3: Seed Data (Optional)

Run SQL/NoSQL seed script to populate demo data:

```bash
# Place seed scripts in: src/main/resources/seed/
./gradlew bootRun --args="--spring.profiles.active=local-seed"
```

---

## 7. Cloud Functions Deployment

### 7.1: Functions Directory Structure

```
functions/
├── index.js              (main entry - HTTP triggers)
├── system-health.js       (health monitoring)
├── server-connection-monitor.js (server health)
├── package.json           (Node dependencies)
└── .firebaserc           (Firebase config)
```

### 7.2: Install Dependencies

```bash
cd functions
npm install

# Key dependencies already in package.json:
# - firebase-functions
# - firebase-admin
# - axios
# - exceljs
# - @google-cloud/vision
# - sockjs-client
# - @stomp/stompjs
```

### 7.3: Deploy All Functions

```bash
# From project root
firebase deploy --only functions

# Deploy specific function
firebase deploy --only functions:processRequirement

# Deploy with specific region
firebase deploy --only functions --region us-central1
```

### 7.4: Set Functions Config (Secrets)

```bash
# Set system secret for Java backend authentication
firebase functions:config:set system.secret="your-super-secret-key-here"

# Set backend URL for functions to call
firebase functions:config:set backend.url="https://your-backend-url.com"

# View current config
firebase functions:config:get

# Deploy config changes
firebase deploy --only functions
```

### 7.5: View Logs

```bash
# Real-time logs
firebase functions:log --only processRequirement

# All functions
firebase functions:log --only *

# Filter by severity
firebase functions:log --only error
```

---

## 8. Backend (Spring Boot)

### 8.1: Project Structure

```
src/main/java/com/supremeai/
├── SupremeAiApplication.java          (Main class)
├── config/                            (Config files)
│   ├── ProviderConfig.java
│   ├── SecurityConfig.java
│   └── WebConfig.java
├── controller/                        (REST endpoints)
│   ├── AgentOrchestrationController.java
│   ├── ProviderManagementController.java
│   └── UserApiKeyController.java
├── provider/                          (AI provider implementations)
│   ├── AIProvider.java               (Interface)
│   ├── AbstractHttpProvider.java
│   ├── OpenAIProvider.java
│   ├── GroqProvider.java
│   ├── StepFunProvider.java           (NEW - to be added)
│   └── AIProviderFactory.java
├── service/                           (Business logic)
│   ├── AIProviderService.java
│   ├── MultiAIConsensusService.java
│   ├── AdaptiveAgentOrchestrator.java
│   └── QuotaManager.java
├── model/                             (Data models)
│   ├── UserApiKey.java
│   ├── Provider.java
│   └── Requirement.java
└── repository/                        (Firestore DAO)
```

### 8.2: Build & Run

```bash
# Clean build (skip tests)
./gradlew clean build -x test

# Build with tests
./gradlew clean build

# Run
./gradlew bootRun

# Run with profile
./gradlew bootRun --args="--spring.profiles.active=dev"

# Run specific test
./gradlew test --tests "MultiAIConsensusServiceTest"
```

### 8.3: Package as JAR/Docker

```bash
# Create fat JAR
./gradlew bootJar

# JAR location: build/libs/supremeai-0.0.1-SNAPSHOT.jar

# Run JAR directly
java -jar build/libs/supremeai-*.jar

# Docker build (if Dockerfile exists)
./gradlew bootBuildImage --imageName=supremeai-backend

# Docker run
docker run -p 8080:8080 \
  -e STEPFUN_API_KEY=your-key \
  supremeai-backend
```

### 8.4: API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `POST /api/orchestrate/requirement` | Process requirement |
| `POST /api/ai/generate` | Single AI generation |
| `POST /api/vote/requirement` | Multi-AI consensus |
| `GET /api/providers/list` | List all providers |
| `POST /api/apikeys/add` | Add user API key |
| `GET /actuator/health` | Health check |
| `GET /actuator/metrics` | Prometheus metrics |

---

## 9. Dashboard (React)

### 9.1: Dashboard Structure

```
dashboard/
├── src/
│   ├── App.tsx                    (Router, auth guard)
│   ├── pages/
│   │   ├── LoginPage.tsx
│   │   ├── AdminDashboardUnified.tsx
│   │   ├── AdminUsers.tsx
│   │   ├── AdminProjects.tsx
│   │   └── ...
│   ├── components/
│   │   ├── APIKeysManager.tsx
│   │   ├── ChatWithAI.tsx
│   │   ├── ProgressMonitor.tsx
│   │   └── ...
│   ├── lib/
│   │   ├── firebase.ts            (Firebase init)
│   │   ├── authUtils.ts           (Auth helpers)
│   │   └── theme.ts               (Ant Design theme)
│   └── i18n/
│       └── conf.ts                (Internationalization)
├── index.html
├── vite.config.ts
├── tsconfig.json
└── package.json
```

### 9.2: Install Dependencies

```bash
cd dashboard

# Install all dependencies
npm install

# Specific versions used:
# - react: ^18.2.0
# - react-router-dom: ^6.x
# - antd: ^5.x
# - @stomp/stompjs: ^7.x
# - sockjs-client: ^1.6.1
# - firebase: ^10.7.1
```

### 9.3: Run Development Server

```bash
# Start dev server (hot reload)
npm run dev

# URL: http://localhost:5173
# Or configured port in vite.config.ts

# Build for production
npm run build

# Preview production build
npm run preview
```

### 9.4: Configure Firebase for Dashboard

Firebase config auto-injected via `firebase init` hosting:

**File**: `public/firebase-config.js` (if missing, create manually):

```javascript
window.firebaseConfig = {
  apiKey: "AIzaSy...",
  authDomain: "supremeai-a.firebaseapp.com",
  projectId: "supremeai-a",
  storageBucket: "supremeai-a.firebasestorage.app",
  messagingSenderId: "123456789",
  appId: "1:123456789:web:abc123",
  measurementId: "G-XXXXXX"
};
```

Dashboard loads this via:

```typescript
// src/lib/firebase.ts
const firebaseConfig = window.firebaseConfig || {
  // emulator fallback
  projectId: "supremeai-a",
  // ...
};
```

---

## 10. Mobile (Flutter)

### 10.1: Flutter Project

**Location**: `supremeai/` (Flutter app)

### 10.2: Setup Flutter

```bash
# Install Flutter SDK
# Download from: https://flutter.dev/docs/get-started/install

# Verify
flutter --version

# Get dependencies
flutter pub get

# Run app
flutter run

# Build APK
flutter build apk --release

# Build iOS (requires macOS)
flutter build ios --release
```

### 10.3: Firebase Configuration for Flutter

1. Download `google-services.json` from Firebase Console
2. Place in `supremeai/android/app/`
3. Download `GoogleService-Info.plist` for iOS
4. Place in `supremeai/ios/Runner/`
5. Update Firebase options in `lib/main.dart`

---

## 11. IDE Extensions

### 11.1: VS Code Extension

**Location**: `supremeai-vscode-extension/`

```bash
cd supremeai-vscode-extension

# Install dependencies
npm install

# Compile
npm run compile

# Run in VS Code (F5)
# Package
npm run package
```

### 11.2: IntelliJ Plugin

**Location**: `supremeai-intellij-plugin/`

```bash
cd supremeai-intellij-plugin

# Build with Gradle
./gradlew buildPlugin

# Run in IDE
./gradlew runIde

# Package
./gradlew buildPlugin
# Output: build/distributions/supremeai-intellij-plugin.zip
```

---

## 12. Troubleshooting

### 12.1: Port Already in Use

**Error**: `Port 8080 already in use`

**Fix**:

```bash
# Windows: Find process using port 8080
netstat -ano | findstr :8080
# Get PID (last column), then kill:
taskkill /PID <PID> /F

# Or use:
# Change port in application.properties:
server.port=8081

# Or set env var:
set PORT=8081
./gradlew bootRun
```

---

### 12.2: Firebase Emulator Won't Start

**Error**: `Emulator failed to start on port 5000`

**Fix**:

```bash
# Check if another Firebase instance running
# Kill all node processes:
taskkill /F /IM node.exe

# Clear emulator cache
firebase emulators:exec --only firestore 'echo "clean"'

# Reset emulators
firebase emulators:stop
firebase emulators:start --only firestore
```

---

### 12.3: Gradle Build Fails

**Error**: `Could not resolve dependencies`

**Fix**:

```bash
# Clear Gradle cache
./gradlew --stop
rd /s /q .gradle

# Re-download dependencies
./gradlew build --refresh-dependencies

# If still failing, check internet/firewall
```

---

### 12.4: "API Key not found" Errors

**Error**: `StepFun API key must be provided`

**Fix**:

```bash
# Check if .env loaded
echo $STEPFUN_API_KEY  # Should NOT be empty

# If empty:
# 1. Verify .env file exists in project root
# 2. NO spaces around = sign: KEY=value NOT KEY = value
# 3. Restart terminal (reloads env vars)
# 4. Manually export:
set STEPFUN_API_KEY=sf-your-key

# For IntelliJ/Eclipse: Set env var in Run Configuration
# - VM options: -DSTEPFUN_API_KEY=sf-your-key
```

---

### 12.5: Firebase Permission Denied

**Error**: `FirebaseError: Missing or insufficient permissions`

**Fix**:

1. Check Firestore rules:

   ```bash
   firebase deploy --only firestore:rules
   ```

2. Verify user is authenticated:

   ```javascript
   // In browser console
   firebase.auth().currentUser // Should not be null
   ```

3. Check custom claims:

   ```bash
   # Set admin claim (run once in Firebase console or script):
   firebase auth:export users.json
   # Edit users.json, add: "customClaims": {"admin": true}
   # Or use Admin SDK to set claim
   ```

---

### 12.6: Dashboard Shows Blank Page

**Error**: White screen, no errors

**Fix**:

```bash
# 1. Check browser console (F12) for errors
# 2. Clear cache: Ctrl+Shift+Delete → Clear all
# 3. Delete localStorage:
#    DevTools → Application → Storage → Clear
# 4. Rebuild dashboard:
cd dashboard
npm run build
firebase deploy --only hosting
```

---

### 12.7: Cloud Functions Cold Start Slow

**Symptom**: Functions take 5-10 seconds first call

**Fix**:

1. **Keep warm** - Schedule periodic pings:

   ```bash
   # In functions, add scheduled function to ping every 5 min
   exports.keepWarm = functions.pubsub.schedule('*/5 * * * *').onRun(...)
   ```

2. **Increase memory** (in `functions/package.json`):

   ```json
   {
     "functions": {
       "processRequirement": {
         "memory": "512MB",
         "timeout": "60s"
       }
     }
   }
   ```

3. **Use min instances** (reduce cold starts):

   ```bash
   firebase functions:upgrade --min-instances 1
   ```

---

### 12.8: CORS Errors

**Error**: `Access-Control-Allow-Origin` missing

**Fix**:

1. **Backend CORS config** (`src/main/java/com/supremeai/config/WebConfig.java`):

```java
@Bean
public CorsFilter corsFilter() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.asList(
        "http://localhost:5173",
        "http://localhost:3000",
        "https://supremeai-a.web.app"
    ));
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
    config.setAllowedHeaders(Arrays.asList("*"));
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
}
```

2. **Or set in `application.properties`**:

   ```properties
   cors.allowed-origins=http://localhost:5173,https://supremeai-a.web.app
   ```

---

### 12.9: Database Rules Not Updating

**Error**: Deployed rules unchanged

**Fix**:

```bash
# Force deploy with specific target
firebase deploy --only firestore:rules --force

# Check current deployed rules
firebase firestore:rules:get

# Compare with local file
firebase deploy --only firestore:rules --debug  # Shows full diff
```

---

### 12.10: API Key Rotation Not Working

**Error**: Keys not rotating on quota exceeded

**Fix**:

1. Verify `AIProviderService` is being called:

   ```java
   // In your code, catch 429 errors:
   if (error.getMessage().contains("429")) {
       aiProviderService.markKeyAsExhausted("stepfun", currentKey);
       String nextKey = aiProviderService.rotateKey("stepfun");
       // Retry with nextKey
   }
   ```

2. Check rotation logic in `MultiAIConsensusService.java` or `SelfHealingService.java`

3. Add logging:

   ```java
   log.info("Rotating {} key to index: {}", provider, nextIndex);
   ```

---

## Quick Start Summary

**One-command setup** (after cloning):

```bash
# 1. Install dependencies (already done)
# 2. Configure env
cp .env.example .env
# Edit .env with your API keys

# 3. Start Firebase emulators
firebase emulators:start --only firestore,functions

# 4. Start backend
./gradlew bootRun

# 5. Start dashboard
cd dashboard && npm run dev

# 6. Open browser
# Dashboard: http://localhost:5173
# Backend API: http://localhost:8080
# Firebase Emulator UI: http://localhost:4000
```

**You're ready to code!** 🚀

---

**Document Version**: 1.0
**Maintained By**: SupremeAI Team
**Last Updated**: 2026-04-29
