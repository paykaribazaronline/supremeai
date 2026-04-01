# Deployment Error Report — Firebase Hosting & Android Release Build

> **Generated:** 2026-04-01
> **Branch analyzed:** `main`
> **Workflow:** `Flutter Admin App - Build & Deploy` (`.github/workflows/flutter-ci-cd.yml`)
> **Failing jobs:** `Deploy to Firebase Hosting` · `Build & Release Android APK`

---

## Summary

The `Flutter Admin App - Build & Deploy` workflow has been failing on every push to `main` since
the workstream implementation started. Two jobs are broken:

| # | Job | Status | Root Cause |
|---|-----|--------|------------|
| 1 | Deploy to Firebase Hosting | ❌ FAILED | `firebase.json` has no named hosting target `flutter-admin` |
| 2 | Build & Release Android APK | ❌ FAILED | `google-services.json` package name mismatch + minSdkVersion too low |
| 3 | Flutter Build & Test | ❌ FAILED (earlier) | `subosito/flutter-action@v3` does not exist |
| 4 | (warning) All jobs | ⚠️ WARNING | Node.js 20 deprecated in GitHub Actions |

---

## Error 1 — Firebase Hosting: Target `flutter-admin` Not Found

### Exact Error Message

```
Error: Hosting site or target flutter-admin not detected in firebase.json
##[error]Process completed with exit code 1.
```

### Affected Runs

- Run ID `23825412479` — commit `feat: Add Firebase configuration for Android and iOS`
- All previous runs of the `Deploy to Firebase Hosting` job

### Root Cause

The workflow deploys with this command:

```yaml
firebase deploy \
  --project=supremeai-a \
  --only=hosting:flutter-admin \   # ← references a NAMED target
  --token=${{ secrets.FIREBASE_TOKEN }}
```

`--only=hosting:flutter-admin` requires `firebase.json` to define a **named hosting target** called
`flutter-admin`. The current `firebase.json` only has a single unnamed `hosting` block:

```json
{
  "hosting": {
    "public": "combined_deploy",
    ...
  }
}
```

An unnamed hosting block does not have the identifier `flutter-admin`, so Firebase CLI cannot find
it and exits with the error above.

`.firebaserc` **does** correctly define the target alias:

```json
"targets": {
  "supremeai-a": {
    "hosting": {
      "flutter-admin": ["supremeai-a"],
      "main-dashboard": ["supremeai-a"]
    }
  }
}
```

But `.firebaserc` targets only work when `firebase.json` also uses the `target` field in its
hosting config.

### Solution

Change `firebase.json` to use the named target syntax so it matches `.firebaserc`:

```json
{
  "hosting": [
    {
      "target": "flutter-admin",
      "public": "combined_deploy/admin",
      "ignore": ["firebase.json", "**/.*", "**/node_modules/**"],
      "rewrites": [
        { "source": "**", "destination": "/index.html" }
      ]
    },
    {
      "target": "main-dashboard",
      "public": "combined_deploy",
      "ignore": ["firebase.json", "**/.*", "**/node_modules/**"],
      "rewrites": [
        { "source": "/admin/**", "destination": "/admin/index.html" },
        { "source": "**", "destination": "/index.html" }
      ]
    }
  ]
}
```

Then run `firebase target:apply hosting flutter-admin supremeai-a` once to link the target to
the Firebase project. The `firebase-hosting-merge.yml` workflow (which deploys successfully) uses
`FirebaseExtended/action-hosting-deploy@v0` and does NOT use named targets, which is why it works.

---

## Error 2 — Android Release Build: No Matching Client for Package Name

### Exact Error Message

```
Execution failed for task ':app:processReleaseGoogleServices'.
> No matching client found for package name 'com.example.supremeai_admin'
BUILD FAILED in 2m 59s
Gradle task assembleRelease failed with exit code 1
##[error]Process completed with exit code 1.
```

### Affected Runs

- Run ID `23825412479` — commit `feat: Add Firebase configuration for Android and iOS`

### Root Cause

The `google-services.json` was added with the wrong package name. The file registers the app
under `"package_name": "supremeai.com"`, but the actual Android app ID in `build.gradle.kts` is
`"com.example.supremeai_admin"`.

**In `flutter_admin_app/android/app/google-services.json`:**

```json
"android_client_info": {
  "package_name": "supremeai.com"   ← WRONG
}
```

**In `flutter_admin_app/android/app/build.gradle.kts`:**

```kotlin
applicationId = "com.example.supremeai_admin"   ← correct app ID
```

The `com.google.gms.google-services` Gradle plugin reads `google-services.json` at build time and
looks for an entry where `package_name` matches the app's `applicationId`. When no match is found,
it throws `No matching client found for package name 'com.example.supremeai_admin'`.

### Solution

Two options:

**Option A (Recommended) — Fix the `google-services.json`:**

Re-register the app in Firebase Console with the correct package name `com.example.supremeai_admin`
and download a new `google-services.json`. The `package_name` field in the JSON must exactly match
the `applicationId` in `build.gradle.kts`.

**Option B — Change the app ID to match the JSON:**

Change `applicationId` in `build.gradle.kts` from `com.example.supremeai_admin` to `supremeai.com`
(and also update `namespace`). This requires also updating `AndroidManifest.xml` and any Firebase
console registrations.

---

## Error 3 — Android Release Build: `minSdkVersion` Too Low

### Exact Error Message

```
uses-sdk:minSdkVersion 21 cannot be smaller than version 23 declared in library
[androidx.core:core-ktx:1.18.0]
Manifest merger failed: uses-sdk:minSdkVersion 21 cannot be smaller than version 23
BUILD FAILED in 3m 25s
Gradle task assembleRelease failed with exit code 1
##[error]Process completed with exit code 1.
```

### Affected Runs

- Run ID `23824231908` — commit `fix: Use correct flutter-action version v2`
- Run ID `23823802193` — commit `fix: GitHub Actions workflow syntax - use env instead of secrets`

### Root Cause

`build.gradle.kts` uses `minSdk = flutter.minSdkVersion`. Flutter 3.27.0 sets
`flutter.minSdkVersion = 21` by default. However, the `androidx.core:core-ktx:1.18.0` dependency
(pulled in by Flutter plugins) declares `minSdk = 23` in its own `AndroidManifest.xml`. The Android
Manifest Merger refuses to merge manifests when the app's `minSdkVersion` is lower than what a
dependency requires.

### Solution

Explicitly set `minSdk = 23` in `flutter_admin_app/android/app/build.gradle.kts`:

```kotlin
defaultConfig {
    applicationId = "com.example.supremeai_admin"
    minSdk = 23                           // was: flutter.minSdkVersion (21)
    targetSdk = 36
    versionCode = flutter.versionCode
    versionName = flutter.versionName
}
```

This was partially fixed by updating `compileSdk` and `targetSdk` to 36 (commit `e0b96cd58b`), but
`minSdk` was not explicitly overridden and still falls back to `flutter.minSdkVersion = 21`.

---

## Error 4 — Flutter Build: `subosito/flutter-action@v3` Does Not Exist

### Exact Error Message

```
##[error]Unable to resolve action `subosito/flutter-action@v3`, unable to find version `v3`
```

### Affected Runs

- Run ID `23824109231` — commit `fix: Resolve all GitHub Actions errors - Node.js 20 deprecation`

### Root Cause

Commit `0ecd02e448` (fix: Resolve all GitHub Actions errors) upgraded `subosito/flutter-action`
from `v2` to `v3` believing that `v3` exists and supports Node.js 22. However, the
`subosito/flutter-action` repository only has released up to `v2` — there is no `v3` tag.
GitHub Actions cannot resolve the action and all jobs depending on Flutter fail immediately.

### Solution

Keep `subosito/flutter-action@v2`. This was already fixed in the immediately following commit
`80b3006dc8` (fix: Use correct flutter-action version v2).

---

## Error 5 — Node.js 20 Deprecation Warning (Non-Blocking, Will Break June 2026)

### Warning Message

```
##[warning]Node.js 20 actions are deprecated. The following actions are running on Node.js 20
and may not work as expected: actions/checkout@v4, actions/download-artifact@v4,
actions/setup-java@v4. Actions will be forced to run with Node.js 24 by default starting
June 2nd, 2026. Node.js 20 will be removed from the runner on September 16th, 2026.
```

### Affected Runs

All runs of `flutter-ci-cd.yml` (not a failure today, but will cause failures after June 2, 2026)

### Root Cause

`flutter-ci-cd.yml` uses `actions/checkout@v4`, `actions/setup-java@v4`, and
`actions/download-artifact@v4`. These versions use the Node.js 20 runtime internally. GitHub will
force-migrate all Node.js 20 actions to Node.js 24 on June 2, 2026, which may break them if the
action authors have not updated their action code for Node.js 24 compatibility.

`firebase-hosting-merge.yml` already addresses this with:

```yaml
env:
  FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true
```

`flutter-ci-cd.yml` does not have this environment variable set.

### Solution

Add `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true` to the top-level `env:` block in
`flutter-ci-cd.yml`. This opts into Node.js 24 now (proactively) instead of being forced into
it on June 2, 2026:

```yaml
env:
  FLUTTER_VERSION: '3.27.0'
  FIREBASE_PROJECT: 'supremeai-a'
  FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true   # Add this line
```

---

## Error 6 — Firebase Hosting Deploy Always Skipped (Silent Logic Bug)

### Symptom

The `Deploy to Firebase Hosting` job runs but the actual deploy step is **always skipped** even
when `FIREBASE_TOKEN` is set as a repository secret. The "token not configured" warning step runs
instead.

### Root Cause

`flutter-ci-cd.yml` uses an `env:` block to expose the secret:

```yaml
env:
  FIREBASE_TOKEN: ${{ secrets.FIREBASE_TOKEN }}
```

Then checks `if: ${{ env.FIREBASE_TOKEN != '' }}`. However, when `FIREBASE_TOKEN` is not set as a
secret, `${{ secrets.FIREBASE_TOKEN }}` evaluates to an **empty string** — but crucially, the
`env:` variable is also set to an empty string, which means `env.FIREBASE_TOKEN != ''` evaluates
to `false`, and the deploy step is skipped.

Additionally, even when the secret IS configured, GitHub Actions masks secrets in `if:` expressions
by replacing the value with `***` before evaluation in some contexts, causing `!= ''` comparisons
to behave unexpectedly.

This is the **same underlying problem** as the old `secrets.FIREBASE_TOKEN` usage in `if:` — the
env-var workaround (added in commit `d01ad5fa86`) still does not reliably work.

### Solution

The most reliable approach is to remove the conditional entirely and let the step fail clearly if
the token is missing, OR use `continue-on-error: true` with a descriptive message:

```yaml
- name: 🚀 Deploy to Firebase
  run: |
    if [ -z "$FIREBASE_TOKEN" ]; then
      echo "⚠️ FIREBASE_TOKEN is not set. Skipping deploy."
      exit 0
    fi
    firebase deploy --project=supremeai-a --only=hosting:flutter-admin --token="$FIREBASE_TOKEN"
  env:
    FIREBASE_TOKEN: ${{ secrets.FIREBASE_TOKEN }}
```

Shell variable tests (`-z "$VAR"`) are more reliable than GitHub Actions expression syntax for
checking optional secrets.

---

## Error Summary Table

| # | Error | Workflow Job | Commit(s) Affected | Blocking? | Fixed? |
|---|-------|-------------|-------------------|-----------|--------|
| 1 | Firebase Hosting target `flutter-admin` not found in `firebase.json` | Deploy to Firebase Hosting | All main runs | ✅ YES | ❌ Not yet |
| 2 | `google-services.json` package name `supremeai.com` ≠ `applicationId` `com.example.supremeai_admin` | Build & Release Android APK | `709a696c3f` | ✅ YES | ❌ Not yet |
| 3 | `minSdkVersion 21` too low — `androidx.core:core-ktx:1.18.0` requires 23 | Build & Release Android APK | `80b3006dc8`, `d01ad5fa86` | ✅ YES | ❌ Not yet |
| 4 | `subosito/flutter-action@v3` does not exist | Flutter Build & Test + Quality Checks | `0ecd02e448` | ✅ YES | ✅ Fixed in `80b3006dc8` |
| 5 | Node.js 20 deprecation in GitHub Actions (sunset June 2, 2026) | All jobs | All runs | ⚠️ Future | ⚠️ Partial — `firebase-hosting-merge.yml` fixed, `flutter-ci-cd.yml` not yet |
| 6 | Firebase deploy step silently skipped due to env-var secret check logic bug | Deploy to Firebase Hosting | All runs since `d01ad5fa86` | ✅ YES (deploy never runs) | ❌ Not yet |
