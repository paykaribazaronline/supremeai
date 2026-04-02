# Flutter Admin App - Automated CI/CD Pipeline

## Overview

The Flutter Admin App now has a fully automated CI/CD pipeline integrated with GitHub Actions and Firebase Hosting. This document explains the automated deployment process.

**Status:** ✅ **COMPLETE** (March 31, 2026)

---

## Architecture

The CI/CD pipeline consists of 4 parallel stages that execute automatically on every push to `main` or `develop` branches:

```
Developer Push
    ↓
1. Flutter Build & Test (ubuntu-latest)
    ├─ Analyze Code
    ├─ Run Tests
    └─ Build Web App
    ↓
2. Deploy to Firebase Hosting (main only)
    └─ Deploy to https://supremeai-565236080752.web.app/admin/
    ↓
3. Deploy Android Release (main only)
    ├─ Build APK
    ├─ Build App Bundle (AAB)
    └─ Create GitHub Release Assets
    ↓
4. Quality Checks & Notifications
    ├─ Static Analysis
    ├─ Format Checks
    ├─ Coverage Reports
    └─ GitHub Summary
```

---

## Workflow File

**Location:** `.github/workflows/flutter-ci-cd.yml`

### Triggers

The workflow automatically runs on:

```yaml
- Push to main or develop branches
- Pull requests to main or develop
- Manual trigger via GitHub Actions UI (workflow_dispatch)
```

Only changes to `flutter_admin_app/**` will trigger the pipeline (saves CI resources).

---

## Pipeline Stages

### Stage 1: Flutter Build & Test

**Purpose:** Verify the app builds correctly and passes quality checks

**Steps:**

1. ✅ Checkout code
2. ✅ Install Flutter SDK (3.24.0)
3. ✅ Get dependencies (`flutter pub get`)
4. ✅ Analyze code (`flutter analyze`)
5. ✅ Format check (`flutter format`)
6. ✅ Run unit tests with coverage
7. ✅ Build web app (`flutter build web --release`)
8. ✅ Upload build artifacts to GitHub

**Output:**

- Build artifacts in `flutter_admin_app/build/web/`
- Test reports and coverage data
- Asset available for 30 days in GitHub Actions

---

### Stage 2: Firebase Hosting Deployment

**Purpose:** Automatically deploy the web app to Firebase Hosting

**Conditions:**

- Only runs on **push to main** (not on pull requests)
- Requires successful build from Stage 1

**What Happens:**

```bash
# 1. Build the web app
flutter build web --release

# 2. Deploy to Firebase Hosting target: flutter-admin
firebase deploy --only hosting:flutter-admin
```

**Firebase Hosting Details:**

- **Project ID:** supremeai-565236080752
- **Web App URL:** https://supremeai-565236080752.web.app/admin/
- **Domain:** Can be mapped to custom domain
- **Auto-HTTPS:** ✅ Enabled
- **CDN:** ✅ Global caching

**Deployment Artifacts:**

- Web app files from `flutter_admin_app/build/web/`
- Rewrite rule: all routes → `index.html` (for Flutter routing)

---

### Stage 3: Android Release Build

**Purpose:** Build Android APK and App Bundle for distribution

**Conditions:**

- Only runs on **push to main** (automated builds)
- Creates release assets when tags are pushed

**What Produces:**

```
1. APK Files (split-per-abi)
   - app-armeabi-v7a-release.apk
   - app-arm64-v8a-release.apk
   - app-x86-release.apk
   - app-x86_64-release.apk
   
2. App Bundle (AAB) for Google Play Store
   - app.aab
```

**Location:**

- `flutter_admin_app/build/app/outputs/flutter-apk/`
- `flutter_admin_app/build/app/outputs/bundle/release/`

**For Release Tags:**
If you create a GitHub release (tag), the APK and AAB files are automatically attached as release assets.

---

### Stage 4: Quality Checks & Notifications

**Purpose:** Perform final code quality checks and post a summary

**Steps:**

1. ✅ Static analysis with Dart analyzer
2. ✅ Format validation
3. ✅ Test coverage report
4. ✅ Upload coverage to Codecov
5. ✅ Post summary to GitHub Actions job summary

**Outputs:**

- Code quality metrics
- Test coverage percentage
- Workflow summary in GitHub Actions UI

---

## Environment Configuration

### Required GitHub Secrets

The following secrets must be configured in your GitHub repository:

#### 1. Firebase Service Account

```
Secret Name: FIREBASE_SERVICE_ACCOUNT_SUPREMEAI_A
Value: Firebase service account JSON (from Firebase Console)
```

**How to get it:**

1. Go to Firebase Console → Project Settings
2. Service Accounts tab → Generate new private key
3. Copy the JSON content
4. Add to GitHub Secrets at: Settings → Secrets and variables → Actions

#### 2. GitHub Token (Automatic)

```
Secret Name: GITHUB_TOKEN
Note: This is automatically provided by GitHub Actions
```

---

## Development Workflow

### For Local Development

```bash
# 1. Create a feature branch
git checkout -b feature/new-feature

# 2. Make changes to flutter_admin_app/
cd flutter_admin_app
flutter pub get
flutter run -d chrome

# 3. Commit and push (doesn't deploy yet - only to develop)
git add .
git commit -m "feat: add new feature"
git push origin feature/new-feature

# 4. Create Pull Request to main
# - This triggers build & test (no deployment)
# - Review the build output in GitHub Actions

# 5. Merge to main when approved
# - This triggers full CI/CD pipeline
# - Auto-deploys to Firebase Hosting
```

### Push to Main (Auto-Deploy)

```bash
# Merge PR or push directly to main
git checkout main
git pull origin main

# All code changes to flutter_admin_app/ are now deployed:
# ✅ Web app → Firebase Hosting
# ✅ Android APK → Build artifacts
# ✅ App Bundle → Available for Play Store
```

---

## Deployment URLs

After successful deployment:

### Web App

- **Live:** https://supremeai-565236080752.web.app/admin/
- **Login:** Use your SupremeAI admin credentials
- **Auto-HTTPS:** Yes
- **CDN:** Globally cached

### GitHub Actions

- View workflow runs: https://github.com/your-username/supremeai/actions
- Download APK/AAB from build artifacts
- View test reports and coverage

---

## Troubleshooting

### Build Fails with "Flutter not found"

**Solution:** The workflow automatically installs Flutter 3.24.0. If it fails:

```bash
# Check Flutter version in workflow
# File: .github/workflows/flutter-ci-cd.yml
# Line: FLUTTER_VERSION: '3.24.0'
```

### Firebase Deployment Fails

**Solution:** Verify the service account secret:

```bash
# 1. Go to GitHub → Settings → Secrets → Actions
# 2. Check FIREBASE_SERVICE_ACCOUNT_SUPREMEAI_A exists
# 3. Re-add if expired (Firebase redeploys expires credentials)
```

### Dependencies Timeout

The workflow has retries built-in:

```yaml
- 3 attempts for flutter pub get
- 15 minute timeout for builds
- 10 minute timeout for tests
```

If it still fails, check pub.dev status: https://pub.dev/

### APK Build Fails

**Solution:** Ensure Android SDK is available:

```yaml
steps:
  - uses: actions/setup-java@v4
    with:
      java-version: '17'
      distribution: 'temurin'
```

---

## Firebase Configuration

The `firebase.json` file now supports multiple hosting targets:

```json
{
  "hosting": [
    {
      "target": "main-dashboard",
      "public": "dashboard"
    },
    {
      "target": "flutter-admin",
      "public": "flutter_admin_app/build/web"
    }
  ]
}
```

### Deploy Targets

You can manually deploy to specific targets:

```bash
# Deploy only the dashboard
firebase deploy --only hosting:main-dashboard

# Deploy only the Flutter app
firebase deploy --only hosting:flutter-admin

# Deploy both
firebase deploy --only hosting
```

---

## Performance Metrics

### Typical Build Times

| Stage | Duration | Notes |
|-------|----------|-------|
| Build & Test | 8-12 min | Includes all tests + analysis |
| Firebase Deploy | 2-3 min | Web app only |
| Android Build | 5-8 min | Both APK and AAB |
| Quality Checks | 3-5 min | Analysis + coverage |
| **Total** | **10-15 min** | Parallel stages |

### Artifact Sizes

| Artifact | Size |
|----------|------|
| Web app (build/web/) | ~8-12 MB |
| APK files | ~25-45 MB each |
| App Bundle (AAB) | ~15-20 MB |

---

## Best Practices

### 1. Branch Strategy

```
main (stable)
  ↑
develop (integration)
  ↑
feature/* (development)
```

**Only main triggers deployment to production.**

### 2. Commit Messages

Use conventional commits for automation:

```bash
git commit -m "feat(flutter): add new dashboard screen"
git commit -m "fix(auth): fix token refresh issue"
git commit -m "style(ui): update color scheme"
```

### 3. Release Process

```bash
# 1. Create version tag (triggers release build)
git tag v1.0.0

# 2. Push tag
git push origin v1.0.0

# 3. APK/AAB automatically attached to GitHub Release
# 4. Manual: Upload AAB to Google Play Console
```

### 4. Monitoring Deployments

```bash
# Watch workflow in real-time
gh run watch <run-id>

# View latest deployment
gh run list --workflow=flutter-ci-cd.yml --limit=1

# Download artifacts
gh run download <run-id> --name flutter-web-build
```

---

## Manual Override (Emergency)

If you need to deploy without CI/CD:

```bash
# Build locally
cd flutter_admin_app
flutter build web --release

# Deploy to Firebase
firebase deploy --only hosting:flutter-admin --token <firebase-token>
```

---

## Next Steps

### Phase 1: Enable (Current)

- ✅ Automated web app deployment
- ✅ Android APK/AAB builds
- ✅ Code quality checks

### Phase 2: Production Optimization

- 🔄 Integrate with Google Play Console (auto-upload AAB)
- 🔄 App signing certificates (secure key storage)
- 🔄 Beta/Alpha testing tracks

### Phase 3: Advanced Releases

- 🔄 Semantic versioning automation
- 🔄 Release notes auto-generation
- 🔄 Changelog updates

### Phase 4: Analytics & Monitoring

- 🔄 Sentry integration (crash reporting)
- 🔄 Firebase Analytics
- 🔄 Performance monitoring

---

## Support

For issues or questions:

1. **Check build logs:** GitHub Actions → Workflow run → Step output
2. **Review Flutter docs:** https://flutter.dev/docs
3. **Firebase Hosting:** https://firebase.google.com/docs/hosting
4. **GitHub Actions:** https://docs.github.com/en/actions

---

**Last Updated:** March 31, 2026  
**Version:** 1.0.0  
**Maintained By:** SupremeAI Team
