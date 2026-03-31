# 👑 SupremeAI - Admin সম্পূর্ণ গাইড (Device + Cloud) 🚀

**Version:** 3.5  
**Last Updated:** March 27, 2026  
**Language:** বাংলা (Bangla)

---

## ০. শুরু করার আগে আপনার যা দরকার

- একটি Windows/Mac/Linux কম্পিউটার

- Google Account (Firebase এর জন্য)

- কিছু API keys (Gemini, ChatGPT ইত্যাদি)

- 2-3 ঘন্টা সময়

---

## 📱 PHASE 1: Device এ Setup

### Step 1️⃣ - Java ইনস্টল করুন

```bash

# Windows এ জাভা ইনস্টল করুন (JDK 17+)

# Download from: https://www.oracle.com/java/technologies/downloads/

# Or use:

choco install openjdk17

# ভেরিফাই করুন:

java -version

```

### Step 2️⃣ - SupremeAI কোড Download করুন

```bash

# যেখানে আপনি কাজ করতে চান:

cd C:\Users\YourName\Desktop

# Clone করুন (বা ম্যানুয়ালি ডাউনলোড করুন)

git clone https://github.com/your-repo/supremeai.git
cd supremeai

```

### Step 3️⃣ - Android Studio ইনস্টল করুন (Flutter এর জন্য)

```bash

# ডাউনলোড: https://developer.android.com/studio

# Windows এ হবে সবচেয়ে সহজ

# Flutter ইনস্টল করুন:

choco install flutter

# ভেরিফাই করুন:

flutter --version

```

### Step 4️⃣ - প্রথমবার Build করুন

```bash
cd c:\Users\YourName\supremeai

# সব dependency ডাউনলোড করুন:

.\gradlew build

# রান করুন:

.\gradlew run

```

---

## ☁️ PHASE 2: Firebase (Cloud) Setup

### Step A: Firebase Project তৈরি করুন

1. **Google Cloud Console খুলুন:**
   - যান: https://console.firebase.google.com/

2. **নতুন Project তৈরি করুন:**
   ```

   "Create Project" বাটন ক্লিক করুন
   ↓
   Project Name দিন (e.g., "supremeai-prod")
   ↓
   Google Analytics চেক করুন
   ↓
   Create Project
   ```

3. **Firestore Database তৈরি করুন:**
   ```

   বাম পাশ → "Build" → "Firestore Database"
   ↓
   "Create Database"
   ↓
   Mode: Start in production mode
   ↓
   Location: asia-southeast1 (Bangladesh এর কাছাকাছি)
   ↓
   Enable
   ```

4. **Authentication সেটআপ করুন:**
   ```

   "Build" → "Authentication"
   ↓
   "Get Started"
   ↓
   Email/Password enable করুন
   ↓
   Google Sign-In enable করুন
   ```

5. **Storage বানান:**
   ```

   "Build" → "Storage"
   ↓
   "Get Started"
   ↓
   Security rules: Start in production mode
   ↓
   Continue
   ```

### Step B: Service Account Key বানান

```

⚙️ Settings (গিয়ার আইকন) ক্লিক করুন
↓
"Project Settings"
↓
"Service Accounts" ট্যাব
↓
"Generate New Private Key"
↓
JSON ফাইল ডাউনলোড হবে

```

**এই ফাইল আপনার Local Device এ রেখে দিন:**

```

c:\Users\YourName\supremeai\src\main\resources\service-account.json

```

### Step C: Firestore Security Rules সেট করুন

```

Firestore Database → Rules ট্যাব

```

Paste করুন এই rules:

```typescript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Admin সব কিছু করতে পারে
    match /{document=**} {
      allow read, write: if request.auth.uid != null;
    }
    
    // Projects
    match /projects/{projectId} {
      allow read, write: if request.auth.uid != null;
    }
    
    // Chats
    match /chats/{chatId} {
      allow read, write: if request.auth.uid != null;
    }
  }
}

```

Publish করুন। ✅

---

## 🔑 PHASE 3: API Keys Setup

### Step 1: Gemini API Key পান

```

1. Google AI Studio যান: https://aistudio.google.com/app/apikey
2. "Get API Key" → "Create API key in new project"
3. Copy করুন জেমিনি API key

```

### Step 2: ChatGPT/OpenAI API Key

```

1. যান: https://platform.openai.com/api-keys
2. Account লগিন করুন
3. "Create new secret key"
4. Copy করুন

```

### Step 3: DeepSeek API Key

```

1. যান: https://platform.deepseek.com/
2. Sign up করুন
3. API section থেকে key নিন

```

### Step 4: Keys সেট করুন Device এ

**Windows PowerShell খুলুন:**

```powershell

# Environment variables সেট করুন:

[Environment]::SetEnvironmentVariable("GEMINI_API_KEY", "আপনার-key-এখানে", "User")
[Environment]::SetEnvironmentVariable("OPENAI_API_KEY", "আপনার-key-এখানে", "User")
[Environment]::SetEnvironmentVariable("DEEPSEEK_API_KEY", "আপনার-key-এখানে", "User")

# PowerShell রিস্টার্ট করুন

Exit

```

**অথবা `.env` ফাইলে রাখুন:**

```

# c:\Users\YourName\supremeai\.env

GEMINI_API_KEY=sk-xxx...
OPENAI_API_KEY=sk-xxx...
DEEPSEEK_API_KEY=sk-xxx...

```

---

## 📊 PHASE 4: Daily Admin কাজ

### প্রতিদিন যা করবেন

#### ✅ Morning Checklist (সকালে)

```

1. Firebase Console খুলুন
   → Projects section দেখুন
   → কি কি projects তৈরি হয়েছে তা চেক করুন

2. Logs দেখুন:
   Firebase → Functions → Logs
   → কোন error আছে কিনা দেখুন

3. Database Size চেক করুন:
   Firestore → Storage
   → বড় হয়ে যাচ্ছে কিনা দেখুন

```

#### 🏃 Afternoon (যখন Users কাজ করছে)

```

1. Database Monitor করুন
   → কোন slow query আছে কিনা
   → API quota ব্যবহার হচ্ছে কিনা

2. Error logs দেখুন
   → কি error হচ্ছে
   → Fix করা লাগবে কিনা

```

#### 🛡️ Evening (সন্ধ্যায়)

```

1. Security check:
   → কেউ আনঅথরাইজড access চেষ্টা করেছে কিনা

2. Performance review:
   → App কিতনা দ্রুত কাজ করছে
   → কোন bottleneck আছে কিনা

3. Backup নিন:
   Firestore → Manage imports/exports
   → Export করুন সব data
   → Cloud Storage এ রাখুন

```

---

## 🚨 PHASE 5: Common Issues & Fix

### Issue #1: API Keys কাজ করছে না

```bash

# Solution:

1. keys ঠিকঠাক আছে কিনা চেক করুন
2. PowerShell রিস্টার্ট করুন
3. Main.java এ keys check করুন
4. ./gradlew run চালান আবার

```

### Issue #2: Firebase Connection ব্যর্থ

```bash

# Solution:

1. service-account.json পথ ঠিক আছে কিনা চেক করুন
2. Google Console এ Firebase enable আছে কিনা চেক করুন
3. Internet connection ঠিক আছে কিনা

```

### Issue #3: Too Many API Calls (Rate Limit)

```bash

# Solution:

Firebase → Functions → Memory/Timeout বাড়ান
↓
Cloud Functions settings দেখুন
↓
Timeout: 540 সেকেন্ড (9 মিনিট)
Memory: 512 MB বা বেশি

```

### Issue #4: Database খুব বড় হয়ে গেছে

```bash

# Solution:

1. Old data delete করুন:
   Firestore → Query → where date < "2025-01-01"
   ↓
   Delete old docs

2. Backup export করুন:
   Firestore → Manage exports
   ↓
   Export select collections
   ↓
   Cloud Storage এ save করুন

```

---

## 🎯 PHASE 6: Admin Dashboard (Daily Commands)

### Local Device এ Quick Commands

```powershell

# Build করুন

.\gradlew build

# Run করুন

.\gradlew run

# Tests চালান

.\gradlew test

# Specific app generate করুন:

# (Main.java এ task বদলে দিন)

.\gradlew run

# Logs দেখুন:

.\gradlew run --debug

# Clean করুন (fresh start):

.\gradlew clean build

```

---

## 📈 PHASE 7: Monitoring Dashboard Setup

### Firebase এ Real-time Dashboard দেখতে

**স্টেপ:**

1. Firebase Console খুলুন

2. Realtime Database → "Data" tab
3. দেখুন live updates

**বা Google Cloud Monitoring:**

```

Google Cloud Console
↓
Monitoring
↓
Create Dashboard
↓
Add:
  - API Calls Count
  - Error Rate
  - Response Time
  - Storage Used

```

---

## 💾 PHASE 8: Backup Strategy

### Daily Backup (প্রতিদিন 2 AM এ automatic)

**Setup করুন Cloud Functions এ:**

```javascript

// functions/backup.js
const functions = require('firebase-functions');
const admin = require('firebase-admin');

exports.dailyBackup = functions.pubsub
  .schedule('every day 02:00')
  .onRun(async (context) => {
    const db = admin.firestore();
    
    // সব data export করুন
    const collections = ['projects', 'chats', 'approvals'];
    
    for (let col of collections) {
      await db.collection(col).get().then(snap => {
        console.log(`Backed up ${col}: ${snap.size} docs`);
      });
    }
    
    return null;
  });

```

Deploy করুন:

```bash
firebase deploy --only functions:dailyBackup

```

---

## 🔐 PHASE 9: Security Checklist

### প্রতি সপ্তাহে (Weekly)

- [ ] Firestore rules update করুন

- [ ] API keys rotate করুন

- [ ] Access logs review করুন

- [ ] Unauthorized attempts check করুন

- [ ] Database backup verify করুন

### প্রতি মাসে (Monthly)

- [ ] Performance report generate করুন

- [ ] Cost analysis করুন (Firebase expenses)

- [ ] Security audit করুন

- [ ] API usage সীমা adjust করুন

---

## 📱 PHASE 10: Mobile এ Flutter App চালান

### একবার সব setup হয়ে গেলে

```bash
cd c:\Users\YourName\supremeai\supremeai

# Device connect করুন USB দিয়ে

# অথবা Emulator খুলুন

# Run করুন:

flutter run

# Build করুন APK:

flutter build apk --release

# Output: build/app/outputs/apk/release/app-release.apk

```

Generated APK ব্যবহারকারীদের দিয়ে দিন।

---

## 🎯 সারাংশ (Quick Reference)

| Step | কাজ | কোথায় | সময় |
|------|------|--------|------|
| 1 | Java ইনস্টল | Device | 10 min |
| 2 | Code ডাউনলোড | Device | 5 min |
| 3 | Firebase setup | Cloud | 15 min |
| 4 | API Keys | Both | 10 min |
| 5 | First Build | Device | 10 min |
| 6 | Daily Monitor | Cloud | 5 min/day |

---

## 📞 Support Contact

যদি কোনো সমস্যা হয়:

1. **Error Log দেখুন:**
   ```

   Firebase Console → Functions → Logs
   ```

2. **Local Debug চালান:**
   ```

   ./gradlew run --debug
   ```

3. **GitHub Issues খুলূন:**
   ```

   যদি bug পান তো issue তৈরি করুন
   ```

---

## ✅ Checklist (সেটআপ সম্পূর্ণ করার আগে)

- [ ] Java ইনস্টল করেছি

- [ ] Flutter ইনস্টল করেছি

- [ ] Code ডাউনলোড করেছি

- [ ] Firebase Project তৈরি করেছি

- [ ] Service account JSON ডাউনলোড করেছি

- [ ] API Keys সংগ্রহ করেছি

- [ ] Environment variables সেট করেছি

- [ ] ./gradlew build চালিয়েছি

- [ ] ./gradlew run সফল হয়েছে

- [ ] Firebase rules update করেছি

সব চেক হয়ে গেলে আপনি ready! 🚀

---

**Last Updated:** March 27, 2026  
**Created for:** SupremeAI Admin  
**Language:** বাংলা (Bangla)

---

**Happy Coding! 👑**
