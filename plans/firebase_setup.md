# 🔥 Firebase Master Setup Guide
**Timestamp:** 2024-05-20 17:00
**Status:** COMPLETE

## 1. Firebase Console Actions
- Create project "SupremeAI".
- Enable **Firestore Database** (Test Mode).
- Enable **Authentication** (Email/Password).

## 2. Security & Credentials
- Go to Project Settings > Service Accounts.
- Generate New Private Key (JSON).
- Rename to `service-account.json`.
- Place in: `src/main/resources/service-account.json`.

## 3. Java Integration
- The `FirebaseService.java` is already configured to read this file.
- **Dependencies:** `firebase-admin` and `google-cloud-firestore` are added in `build.gradle.kts`.

## 4. Git Security
- Ensure `service-account.json` is listed in `.gitignore` to prevent leaks.
