# GitHub CI/CD Audit Report

**তারিখ:** ২০২৬-০৬-০৫  
**Audited By:** Antigravity AI  
**Project:** SupremeAI (`paykaribazaronline/supremeai`)  
**Scope:** `.github/workflows/` — সব workflow ফাইল, build configuration, এবং script analysis

---

## ১. সারসংক্ষেপ (Executive Summary)

> [!CAUTION]
> এই মুহূর্তে GitHub-এ push করলে **CI/CD FAIL হওয়ার সম্ভাবনা ৯০%+।**  
> দুটো critical কারণ: (১) Firebase Emulator কখনো start হয় না, (২) প্রয়োজনীয় Secrets সেট নেই।

| Workflow            | Status           | ঝুঁকি                                             |
| ------------------- | ---------------- | ------------------------------------------------- |
| `ai-validation.yml` | ❌ সম্ভাব্য FAIL | Secrets নেই, Backend start না হলে validation fail |
| `e2e-tests.yml`     | ❌ সম্ভাব্য FAIL | Firebase Emulator কখনো launch হয় না              |

---

## ২. Workflow বিশ্লেষণ

### ২.১ `ai-validation.yml` — AI Validation Pipeline

**Trigger:** `push` → `main`, `develop`, `master` | `pull_request` → একই branches  
**Timeout:** 15 মিনিট  
**Runner:** `ubuntu-latest`

#### ✅ ভালো দিক

| #   | বিষয়                                       | কারণ                                          |
| --- | ------------------------------------------- | --------------------------------------------- |
| 1   | `actions/checkout@v4`                       | সর্বশেষ stable version                        |
| 2   | `actions/setup-java@v4` + `cache: 'gradle'` | Gradle cache built-in                         |
| 3   | `actions/cache@v4` Gradle packages          | Duplicate cache — অতিরিক্ত কিন্তু ক্ষতিকর নয় |
| 4   | `concurrency` + `cancel-in-progress: true`  | ডুপ্লিকেট run বাতিল হয়                       |
| 5   | `paths-ignore: '**.md', 'docs/**'`          | Docs-only push-এ CI skip হয়                  |
| 6   | `timeout-minutes: 15`                       | Runaway job থেকে সুরক্ষা                      |
| 7   | Redis service healthcheck সহ                | ✅ সঠিক                                       |

#### ❌ সমস্যা

| #   | সমস্যা                                                              | লাইন          | ঝুঁকি স্তর     |
| --- | ------------------------------------------------------------------- | ------------- | -------------- |
| 1   | **`POCKETLAB_URL` Secret নেই**                                      | 75            | 🔴 Critical    |
| 2   | **`GOOGLE_APPLICATION_CREDENTIALS` Secret নেই**                     | 76            | 🔴 Critical    |
| 3   | **`validate_ai.sh` — backend চালু না থাকলে সব HTTP call fail**      | 73            | 🔴 Critical    |
| 4   | **Backend health loop: 90×3=270s wait, তবুও success নিশ্চিত নয়**   | 63-70         | 🟠 Medium      |
| 5   | **`config/skills-local.json` GitHub-এ track হয় না**                | script line 5 | 🟠 Medium      |
| 6   | **Duplicate Gradle cache** (setup-java cache + actions/cache উভয়ই) | 43, 45-51     | 🟡 Low (waste) |

---

### ২.২ `e2e-tests.yml` — E2E Tests

**Trigger:** `schedule: '0 2 * * *'` (daily 2AM) + `push` → dashboard/functions/config + `workflow_dispatch`  
**Timeout:** 25 মিনিট  
**Runner:** `ubuntu-latest`

#### ✅ ভালো দিক

| #   | বিষয়                                                       |
| --- | ----------------------------------------------------------- |
| 1   | `schedule` trigger — রাত্রে চলে, daytime runtime বাঁচায়    |
| 2   | `paths:` filter — শুধু relevant folder change-এ trigger     |
| 3   | `workflow_dispatch` — manual trigger সম্ভব                  |
| 4   | Playwright browser cache আছে                                |
| 5   | `continue-on-error: true` — backend fail হলেও পরের step চলে |
| 6   | Test reports ও backend logs upload হয়                      |

#### ❌ সমস্যা

| #   | সমস্যা                                                                                                                       | লাইন  | ঝুঁকি স্তর  |
| --- | ---------------------------------------------------------------------------------------------------------------------------- | ----- | ----------- |
| 1   | **🔴 Firebase Emulator কখনো start হয় না!** `FIRESTORE_EMULATOR_HOST=localhost:8081` সেট আছে কিন্তু emulator launch step নেই | 74-88 | 🔴 Critical |
| 2   | **Firebase Auth Emulator (`localhost:9099`) ও চালু হয় না**                                                                  | 114   | 🔴 Critical |
| 3   | **`npm install -g firebase-tools` নেই**                                                                                      | —     | 🔴 Critical |
| 4   | **`dashboard/frontend.log` path wrong** (`dashboard/` এ `frontend.log` save হয় না)                                          | 154   | 🟠 Medium   |
| 5   | **Playwright report `retention-days: 30`** — অপ্রয়োজনীয় storage usage                                                      | 145   | 🟡 Low      |
| 6   | **`bootRun` এ `--enable-preview` JVM args নেই** কিন্তু build-এ দরকার                                                         | 79    | 🟠 Medium   |

---

## ৩. Build Configuration বিশ্লেষণ

### `build.gradle.kts`

#### ✅ ভালো

- Spring Boot 3.4.5, Java 21 — সর্বশেষ LTS
- Jacoco coverage ratchet mechanism (auto-update)
- `--no-daemon` CI-তে ব্যবহার — ✅ সঠিক
- `layered` bootJar — Docker layer cache friendly
- `DuplicatesStrategy.EXCLUDE` — JAR conflict নেই
- `isZip64 = true` — large JAR support

#### ⚠️ সতর্কতা

- `--enable-preview` JVM args শুধু test-এ, কিন্তু `bootRun`-এ নেই — inconsistency
- `compileJava` তে `--enable-preview` নেই, শুধু `compileTestJava`-তে আছে
- `io.freefair.lombok:8.10` — latest check করা উচিত

### `gradle.properties`

```properties
org.gradle.daemon=true       # CI-তে daemon = false হওয়া উচিত!
org.gradle.caching=true      # ✅ ভালো
org.gradle.parallel=true     # ✅ ভালো
jacoco.line.minimum=0.33     # 33% coverage minimum
```

> [!WARNING]
> `org.gradle.daemon=true` GitHub Actions-এ কোনো কাজে আসে না কারণ প্রতিটি job নতুন container। `--no-daemon` flag already workflow-এ আছে তাই সমস্যা নেই, কিন্তু পরিষ্কার রাখতে CI-এ `false` করা ভালো।

---

## ৪. `scripts/validate_ai.sh` বিশ্লেষণ

| চেক                               | কী হয়                                                                |
| --------------------------------- | --------------------------------------------------------------------- |
| Health check (`/actuator/health`) | Backend চালু না থাকলে FAIL                                            |
| Inference (`/api/chat`)           | API key/secret নেই তাই FAIL                                           |
| Secrets hygiene check             | `config/.env` ও `firebase-service-account.json` git-tracked কিনা দেখে |

> [!IMPORTANT]
> `validate_ai.sh` এর `set -euo pipefail` আছে — যেকোনো error-এ script exit করবে এবং workflow fail হবে।

---

## ৫. GitHub Free Runtime Analysis

### বর্তমান অনুমানিত খরচ (প্রতি push)

| Workflow                         | অনুমানিত সময় | মাসিক push (অনুমান 50) | মোট মিনিট         |
| -------------------------------- | ------------- | ---------------------- | ----------------- |
| `ai-validation.yml`              | ~10-15 min    | 50                     | ~625 min          |
| `e2e-tests.yml` (push trigger)   | ~20-25 min    | 50                     | ~1125 min         |
| `e2e-tests.yml` (daily schedule) | ~20 min       | 30 days                | ~600 min          |
| **মোট**                          |               |                        | **~2350 min/মাস** |

> [!NOTE]
> GitHub Free Plan: **2000 মিনিট/মাস।** বর্তমান configuration-এ **limit অতিক্রম হওয়ার সম্ভাবনা আছে।**

---

## ৬. সুপারিশ ও Fix Plan

### 🔴 Critical (এখনই করো)

#### Fix 1: GitHub Secrets যোগ করো

Repository → Settings → Secrets and variables → Actions:

```
POCKETLAB_URL          = https://your-backend-url.com
GOOGLE_APPLICATION_CREDENTIALS = { ...service account JSON content... }
```

#### Fix 2: Firebase Emulator যোগ করো `e2e-tests.yml`-এ

```yaml
- name: Install & Start Firebase Emulators
  run: |
    npm install -g firebase-tools
    firebase emulators:start --only firestore,auth --project demo-supremeai &
    sleep 15
    echo "Firebase emulators started"
```

এটা "Run Backend" step-এর **আগে** যোগ করো।

#### Fix 3: Frontend log path ঠিক করো

```yaml
# e2e-tests.yml line 154 — পরিবর্তন করো:
path: |
  backend.log
  frontend.log    # dashboard/ prefix সরাও
```

---

### 🟠 Medium (শীঘ্রই করো)

#### Fix 4: `bootRun` এ `--enable-preview` যোগ করো

```kotlin
// build.gradle.kts tasks.bootRun-এ যোগ করো:
jvmArgs("--enable-preview")
```

#### Fix 5: Duplicate Gradle cache সরাও

`ai-validation.yml`-এ `actions/setup-java` cache এবং `actions/cache` — দুটো একসাথে আছে। শুধু `setup-java` এর `cache: 'gradle'` রাখো, আলাদা `actions/cache` step সরাও।

---

### 🟡 Runtime সাশ্রয় (Optimization)

#### Fix 6: E2E Push Trigger সীমিত করো

```yaml
# e2e-tests.yml — push trigger সরাও, শুধু schedule ও workflow_dispatch রাখো:
on:
  schedule:
    - cron: "0 2 * * *"
  workflow_dispatch:
  # push: REMOVE করো
```

**সাশ্রয়:** ~1125 min/মাস

#### Fix 7: Playwright retention কমাও

```yaml
retention-days: 7 # 30 থেকে কমাও
```

#### Fix 8: `validate_ai.sh` কে unit test-এর সাথে merge করো

আলাদা `bootRun` না চালিয়ে শুধু `./gradlew test`-এ validation করো যেখানে mock backend ব্যবহার হবে।

#### Fix 9: `gradle.properties`-এ CI daemon বন্ধ

```properties
# CI environment-এ override হবে:
org.gradle.daemon=false
```

অথবা workflow-এ `GRADLE_OPTS: "-Dorg.gradle.daemon=false"` env যোগ করো।

---

## ৭. Priority Matrix

```
HIGH IMPACT + EASY FIX:
  ✅ GitHub Secrets যোগ করো (5 মিনিট)
  ✅ E2E push trigger বন্ধ করো (2 মিনিট)
  ✅ Duplicate cache সরাও (2 মিনিট)

HIGH IMPACT + MEDIUM EFFORT:
  ⚙️ Firebase Emulator step যোগ করো (15 মিনিট)
  ⚙️ Frontend log path ঠিক করো (2 মিনিট)

LOW IMPACT + EASY FIX:
  📝 retention-days কমাও (1 মিনিট)
  📝 daemon=false CI-তে (1 মিনিট)
```

---

## ৮. অনুমানিত Runtime সাশ্রয় পরে

| আগে           | পরে          | সাশ্রয়                  |
| ------------- | ------------ | ------------------------ |
| ~2350 min/মাস | ~850 min/মাস | **~1500 min/মাস (~64%)** |

GitHub Free Limit (2000 min) এর মধ্যে থাকবে ✅

---

## ৯. Conclusion

| বিষয়              | মূল্যায়ন                               |
| ------------------ | --------------------------------------- |
| Workflow structure | ✅ ভালো                                 |
| Security hygiene   | ✅ ভালো (secrets hygiene check আছে)     |
| Caching strategy   | 🟡 Duplicate আছে, optimize করা যায়     |
| Firebase setup     | ❌ Missing — critical bug               |
| Secret management  | ❌ Secrets set করা নেই                  |
| Runtime efficiency | 🟠 E2E push trigger অপ্রয়োজনীয়        |
| Build config       | ✅ ভালো (layered JAR, coverage ratchet) |

> [!IMPORTANT]
> Push করার আগে অবশ্যই: (১) Secrets যোগ করো, (২) Firebase Emulator step যোগ করো।  
> এই দুটো ছাড়া CI pass হবে না।

---

_Report generated: 2026-06-05 | Audited by Antigravity AI_
