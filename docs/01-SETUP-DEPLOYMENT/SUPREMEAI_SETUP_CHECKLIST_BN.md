# SupremeAI সেটআপ ও ডিপ্লয়মেন্ট চেকলিস্ট (বাংলা)

এই গাইডটি এই রিপোর বর্তমান স্ট্রাকচারের সাথে সামঞ্জস্য রেখে লেখা হয়েছে।

## 1) Environment Setup (আবশ্যক)

```bash
git clone https://github.com/paykaribazaronline/supremeai.git
cd supremeai
```

Java 17+ নিশ্চিত করুন।

Linux/macOS:

```bash
chmod +x gradlew
./gradlew clean build
./gradlew test
```

Windows PowerShell:

```powershell
.\gradlew.bat clean build
.\gradlew.bat test
```

## 2) API Keys Setup

```bash
cp .env.example .env
```

`.env` এ ন্যূনতম যেগুলো দিতে হবে:

- `GEMINI_API_KEY`
- `DEEPSEEK_API_KEY`
- `FIREBASE_PROJECT_ID`
- `FIREBASE_DATABASE_URL`
- `FIREBASE_SERVICE_ACCOUNT` (base64) অথবা `FIREBASE_CREDENTIALS_FILE`

Firebase service account base64 উদাহরণ:

```bash
cat service-account.json | base64
```

Windows PowerShell base64 উদাহরণ:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("service-account.json"))
```

## 3) Firebase Configuration (Critical)

Enable করুন:

- Firebase Authentication
- Cloud Firestore
- Firebase Storage
- Firebase Hosting
- Cloud Functions
- Firebase Cloud Messaging

Firestore rules (ন্যূনতম নিরাপদ কনফিগ):

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## 4) application.properties

রিপোতে এখন env-driven keys যোগ করা আছে। নিচের ভ্যারিয়েবলগুলো `.env` থেকে ফিড হবে:

- `secret.manager.backend` (env/gcp/aws/azure/vault)
- `gemini.api.key`
- `deepseek.api.key`
- `king.mode.enabled`
- `rate.limit.rpm`, `rate.limit.tpm`
- local dev datasource defaults (H2)

## 5) Multi-Agent System

গুরুত্বপূর্ণ flags:

- `consensus.threshold`
- `king.mode.enabled`
- `feature.key-rotation.enabled`

## 6) Docker Deployment

রুটে `docker-compose.yml` যোগ করা হয়েছে। চালাতে:

```bash
docker compose up -d --build
```

## 7) CI/CD

রিপোতে `java-ci.yml` workflow আগে থেকেই আছে এবং build/test/coverage/docker jobs চালায়।

GitHub Secrets এ অন্তত যোগ করুন:

- `FIREBASE_SERVICE_ACCOUNT`
- `GEMINI_API_KEY`
- `DEEPSEEK_API_KEY`

## 8) Monitoring

রিপোতে monitoring stack ফাইল আছে: `monitoring/docker-compose.monitoring.yml`।

## 9) Testing Strategy

```bash
./gradlew test
./gradlew jacocoTestReport
```

নোট: `integrationTest` এবং `e2eTest` task যদি define না থাকে, আগে Gradle tasks এ add করতে হবে।

## 10) Security Hardening

Never commit:

- `.env`
- `service-account.json`
- API keys
- private certificates

এই রিপোর `.gitignore` ইতিমধ্যে এগুলো ব্লক করে।

## Quick Verification Checklist

- Java 17+ installed
- `.env` configured
- `./gradlew clean build` success
- `./gradlew test` success
- App runs on port 8080
- Docker compose starts successfully
- Firebase auth + firestore validated
