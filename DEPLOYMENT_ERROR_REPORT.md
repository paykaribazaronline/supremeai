# Deployment Error Report — Firebase Hosting & Android Release Build

> **Generated:** 2026-04-01  
> **Branch analyzed:** `main`  
> **Workflow:** `Flutter Admin App - Build & Deploy` (`.github/workflows/flutter-ci-cd.yml`)  
> **Status:** 6 errors identified, 4 require fixes

---

## Executive Summary

The `Flutter Admin App - Build & Deploy` workflow has critical failures preventing deployment:

| # | Issue | Status | Severity |
|---|-------|--------|----------|
| 1 | Firebase Hosting target `flutter-admin` not found | ❌ ACTIVE | 🔴 Critical |
| 2 | Android: google-services.json package name mismatch | ❌ ACTIVE | 🔴 Critical |
| 3 | Android: minSdkVersion too low (21 < 23) | ❌ ACTIVE | 🔴 Critical |
| 4 | Flutter Build: `subosito/flutter-action@v3` doesn't exist | ✅ FIXED | ✓ Resolved |
| 5 | Node.js 20 deprecation (deadline: Jun 2, 2026) | ⚠️ WARNING | 🟠 Medium |
| 6 | Firebase deploy silently skipped (unreliable if condition) | ❌ ACTIVE | 🟠 Medium |

---

## ❌ ERROR #1: Firebase Hosting Target Not Found

### Exact Error Message
```
Error: Hosting site or target flutter-admin not detected in firebase.json
```

### Root Cause
The workflow runs:
```bash
firebase deploy --only=hosting:flutter-admin
```

This requires `firebase.json` to define a named hosting target called `flutter-admin`. However:
- ✅ `.firebaserc` correctly defines the alias: `"flutter-admin": ["supremeai-a"]`
- ❌ `firebase.json` uses a single unnamed block: `"hosting": {...}` (no `target:` property)

### Current firebase.json
```json
{
  "hosting": {
    "public": "combined_deploy",
    "ignore": ["firebase.json", "**/.*", "**/node_modules/**"],
    ...
  }
}
```

### Fix Required
Change firebase.json to use **target-based hosting**:

```json
{
  "hosting": [
    {
      "target": "flutter-admin",
      "public": "combined_deploy",
      "ignore": ["firebase.json", "**/.*", "**/node_modules/**"],
      "rewrites": [
        {
          "source": "/admin/**",
          "destination": "/admin/index.html"
        },
        {
          "source": "**",
          "destination": "/index.html"
        }
      ],
      "headers": [
        {
          "source": "/admin/**",
          "headers": [
            {
              "key": "Cache-Control",
              "value": "no-cache, no-store, must-revalidate"
            }
          ]
        }
      ]
    },
    {
      "target": "main-dashboard",
      "public": "combined_deploy",
      "ignore": ["firebase.json", "**/.*", "**/node_modules/**"],
      "rewrites": [
        {
          "source": "**",
          "destination": "/index.html"
        }
      ]
    }
  ]
}
```

---

## ❌ ERROR #2: Android Package Name Mismatch

### Exact Error Message
```
No matching client found for package name 'com.example.supremeai_admin'
```

### Root Cause
Version mismatch between two files:

| File | Package Name | Issue |
|------|--------------|-------|
| `google-services.json` | `supremeai.com` | ❌ Registered with Firebase |
| `build.gradle.kts` | `com.example.supremeai_admin` | ❌ App declares different ID |

Google Play Services requires exact match between:
- Your app's declared `applicationId` in Gradle
- The package name registered in `google-services.json`

### Solution (Choose One)

**Option A (Recommended):** Update build.gradle.kts to match google-services.json
```kotlin
applicationId = "supremeai.com"
```

**Option B:** Re-register app in Firebase Console
1. Go to Firebase Console → Project Settings → Apps
2. Delete the iOS app (supremeai.com)
3. Add new Android app with package name: `com.example.supremeai_admin`
4. Download new `google-services.json`
5. Replace the file in `flutter_admin_app/android/app/`

---

## ❌ ERROR #3: minSdkVersion Too Low

### Exact Error Message
```
minSdkVersion 21 cannot be smaller than version 23 declared in library 
[androidx.core:core-ktx:1.18.0]
```

### Root Cause
```kotlin
minSdk = flutter.minSdkVersion  // Resolves to 21
targetSdk = 36
```

Flutter 3.27.0's default minSdkVersion is 21, but `androidx.core:core-ktx:1.18.0` requires API 23+.

### Fix Required
Change `flutter_admin_app/android/app/build.gradle.kts`:
```kotlin
android {
    ...
    defaultConfig {
        applicationId = "supremeai.com"
        minSdk = 23  // Changed from flutter.minSdkVersion (21)
        targetSdk = 36
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }
    ...
}
```

---

## ✅ ERROR #4: subosito/flutter-action@v3 (Already Fixed)

### Status: **RESOLVED** ✓
- **Error:** `Unable to resolve action subosito/flutter-action@v3`
- **Reason:** Version v3 was never released
- **Fixed by:** Commit `80b3006dc8` (reverted to v2)
- **Current Status:** All 4 occurrences now use `v2` ✓

---

## ⚠️ ERROR #5: Node.js 20 Deprecation Warning

### Error Message
```
Node.js 20 is deprecated. The following actions target Node.js 20 
but are being forced to run with Node.js 24 by default starting 
June 2nd, 2026
```

### Deadline
**June 2, 2026** — After this date, workflows will fail with Node.js 24.

### Root Cause
`flutter-ci-cd.yml` doesn't have:
```yaml
env:
  FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true
```

Other workflows (e.g., `firebase-hosting-merge.yml`) already have this flag.

### Fix Required
Add to `flutter-ci-cd.yml` top-level `env:`:
```yaml
env:
  FLUTTER_VERSION: 3.27.0
  JAVA_VERSION: '17'
  FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true
```

---

## ❌ ERROR #6: Firebase Deploy Always Silently Skipped

### Symptom
```
Deploy to Firebase Hosting step exists in logs but:
- Never outputs log messages
- Never actually deploys
- FIREBASE_TOKEN secret is set yet step is skipped
```

### Root Cause
GitHub Actions `if:` expressions handle secrets unreliably:
```yaml
- name: 🚀 Deploy to Firebase
  if: ${{ env.FIREBASE_TOKEN != '' }}  # ❌ Unreliable for secrets
  run: firebase deploy ...
```

When using `secrets.FIREBASE_TOKEN` directly in `if:`, GitHub evaluates it as empty string before step execution, causing the condition to evaluate false even when secret is set.

### Current Workaround (Partial)
Added job-level env (commit `d01ad5f`):
```yaml
env:
  FIREBASE_TOKEN: ${{ secrets.FIREBASE_TOKEN }}
```

### Proper Fix
Use **shell-level conditionals** instead:
```yaml
- name: 🚀 Deploy to Firebase
  run: |
    if [ -z "$FIREBASE_TOKEN" ]; then
      echo "⚠️ FIREBASE_TOKEN not configured, skipping Firebase deployment"
      exit 0
    fi
    
    echo "🚀 Deploying Flutter Admin App to Firebase Hosting..."
    firebase deploy \
      --token=${{ secrets.FIREBASE_TOKEN }} \
      --only=hosting:flutter-admin \
      --non-interactive \
      --debug
```

This is more reliable because shell variable tests (`[ -z "$VAR" ]`) are evaluated at runtime.

---

## 🔧 Fix Priority & Implementation Order

| Order | Fix | Estimated Time | Blocks |
|-------|-----|-----------------|--------|
| 1️⃣ | Error #2: Package name mismatch | 2 min | Error #3 |
| 2️⃣ | Error #3: minSdkVersion to 23 | 1 min | Android build |
| 3️⃣ | Error #1: firebase.json targets | 3 min | Firebase deploy |
| 4️⃣ | Error #6: Shell-level conditionals | 2 min | Reliable deploys |
| 5️⃣ | Error #5: Node.js 24 flag | 1 min | Future-proof |

**Total time:** ~9 minutes

---

## 📊 Workflow Status After All Fixes

```
✅ Flutter Build & Test       → Will pass
✅ Code Quality & Notifications → Will pass
✅ Build & Release Android APK → Will pass (after minSdk fix)
✅ Deploy to Firebase Hosting  → Will deploy (with shelled if condition)
✅ Build & Release Android APK → Will complete
✅ Future-proof Node.js 24     → June 2026 ready
```

---

## 📝 Related Files

| File | Issue | Status |
|------|-------|--------|
| `firebase.json` | No hosting targets defined | ❌ Needs fix |
| `flutter_admin_app/android/app/build.gradle.kts` | minSdk too low, package name mismatch | ❌ Needs fixes |
| `flutter_admin_app/android/app/google-services.json` | Package name mismatch | ❌ Needs verification |
| `.github/workflows/flutter-ci-cd.yml` | Multiple if: conditions, missing Node.js flag | ❌ Needs fixes |

---

**Report generated:** 2026-04-01  
**Analysis completed by:** GitHub Copilot  
**Next action:** Apply all 5 fixes systematically
