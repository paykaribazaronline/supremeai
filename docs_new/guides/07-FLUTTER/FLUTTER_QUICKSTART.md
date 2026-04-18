# Flutter Admin App - Quick Start Guide (End-to-End)

## 🚀 Complete Setup in 5 Minutes

This guide walks you through setting up the fully automated Flutter CI/CD pipeline.

---

## Prerequisites

- ✅ Flutter SDK (3.24.0+) - [Install](https://flutter.dev/docs/get-started/install)
- ✅ Java 17+ - [Install](https://www.oracle.com/java/technologies/downloads/)
- ✅ Node.js & npm - [Install](https://nodejs.org/)
- ✅ Git & GitHub account - [Sign up](https://github.com)
- ✅ Firebase project - [Create](https://firebase.google.com)
- ✅ GitHub repository - [Create](https://github.com/new)

---

## Step 1: Local Setup (2 minutes)

### Windows (PowerShell)

```powershell
# Navigate to your repo
cd supremeai

# Run the setup script
.\setup-flutter-cicd.ps1
```

### macOS/Linux (Bash)

```bash
# Navigate to your repo
cd supremeai

# Make script executable
chmod +x setup-flutter-cicd.sh

# Run the setup script
./setup-flutter-cicd.sh
```

**What this does:**

- ✅ Checks Flutter, Java, Firebase CLI, npm
- ✅ Builds the Flutter web app
- ✅ Verifies Firebase configuration
- ✅ Displays setup summary

---

## Step 2: Firebase Authentication (1 minute)

### Generate Firebase Token

Open Terminal/PowerShell and run:

```bash
firebase login:ci
```

**What happens:**

1. Browser opens, asks you to log in to Google
2. After login, terminal displays a token
3. **Copy and save this token** (you'll need it next)

**Example output:**

```
✓ Success! Use this token to login on a CI server:

1//0gJBM-Q8-z4zaCgYIARAAGAsSNQ5...very_long_token_string...

Example command:
  firebase deploy --token "1//0gJBM-Q8..."
```

---

## Step 3: GitHub Secrets Configuration (1 minute)

### Add Firebase Token to GitHub

1. Go to your GitHub repository
2. **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Fill in:
   - **Name:** `FIREBASE_TOKEN`
   - **Secret:** Paste the token from Step 2
5. Click **Add secret** ✅

### Verify

```bash
gh secret list
# Should show:
# FIREBASE_TOKEN  Updated just now
```

---

## Step 4: GitHub Actions Workflow (1 minute)

The workflow file is already in place: `.github/workflows/flutter-ci-cd.yml`

**Automated stages:**

1. **Build & Test** - Compiles web app, runs tests
2. **Deploy to Firebase** - Pushes to hosting (main branch only)
3. **Build Android** - Creates APK and App Bundle
4. **Quality Checks** - Code analysis, coverage reports

---

## Step 5: Deploy! (Automatic)

### Trigger the Pipeline

Now just push code to GitHub:

```bash
# Create a feature branch
git checkout -b feature/ci-cd-setup

# Make a small change (or test with no changes)
git add .
git commit -m "feat: enable automated CI/CD pipeline"

# Push to GitHub
git push origin feature/ci-cd-setup

# Create a Pull Request on GitHub (optional - for testing)
# Or merge directly to main (triggers deployment)

git checkout main
git merge feature/ci-cd-setup
git push origin main
```

### Watch the Deployment

1. Go to: https://github.com/your-username/supremeai/actions
2. Click the latest workflow run
3. Watch the pipeline execute:
   - 🔨 Build starts (usually 30 seconds for simple file changes)
   - 🧪 Tests run
   - 🚀 Deploys to Firebase (if merging to main)
   - ✅ Complete!

**Typical times:**

- Build & Test: 5-10 minutes
- Firebase Deploy: 1-2 minutes
- Total: ~10 minutes

---

## 🎉 Success Indicators

When deployment succeeds, you'll see:

✅ **GitHub Actions:**

- All jobs show green checkmarks
- Deployment step shows: "Deploy to Firebase Hosting" with ✅

✅ **Firebase Console:**

- New deployment in Hosting → Deployment history
- Status: "Deployed"

✅ **Live Web App:**

- Accessible at: https://supremeai-a.web.app/admin/
- Shows your Flutter app

✅ **GitHub Notification:**

- PR comment shows: "✅ Flutter Build Successful!"
- Provides preview URL

---

## 📚 Documentation

| Document | Purpose |
|----------|---------|
| **CI_CD_AUTOMATION.md** | Detailed pipeline architecture and configuration |
| **GITHUB_SECRETS_SETUP.md** | Troubleshooting secrets and advanced setup |
| **.github/workflows/flutter-ci-cd.yml** | The actual workflow definition |
| **firebase.json** | Firebase hosting configuration |
| **.firebaserc** | Firebase project configuration |

---

## 🆘 Troubleshooting

### ❌ Build Fails with "Flutter not found"

```bash
# Verify Flutter is in PATH
flutter --version

# If not found, reinstall Flutter
flutter pub global activate
```

### ❌ Firebase Deployment Fails

```bash
# Check GitHub Secrets are set
gh secret list

# Verify Firebase token is valid
firebase deploy --only hosting:flutter-admin --token $FIREBASE_TOKEN --debug

# If token expired, generate new one
firebase login:ci
```

### ❌ Tests Fail

```bash
# Run tests locally first
cd flutter_admin_app
flutter test

# Fix issues, then push again
```

### ❌ Artifacts Not Created

```bash
# Check build directory exists
ls flutter_admin_app/build/web/

# If empty, rebuild
flutter build web --release --no-pub
```

### ❌ Stuck on "Building web app"

```bash
# This can take 2-3 minutes on first build
# Wait at least 5 minutes before canceling

# If it times out after 30 minutes:
# 1. Cancel the workflow
# 2. Check for pub.dev outages
# 3. Clean and rebuild: flutter clean && flutter pub get
```

---

## 🔄 Continuous Deployment Workflow

Once setup is complete:

```
Your Code Changes
    ↓
git push to main/develop
    ↓
GitHub Actions Triggers (automatically)
    ↓
Build & Test
    ↓
IF main branch: Deploy to Firebase
    ↓
Live at https://supremeai-a.web.app/admin/
```

**No manual deployment needed!** 🎉

---

## 📊 Monitoring Deployments

### Watch Real-Time in Terminal

```bash
# While workflow is running
gh run watch

# Or view latest runs
gh run list --workflow=flutter-ci-cd.yml
```

### View in GitHub UI

1. https://github.com/your-username/supremeai/actions
2. Click "Flutter Admin App - Build & Deploy"
3. Watch status of each stage

### Check Firebase Hosting

1. https://console.firebase.google.com
2. Select project
3. Go to Hosting
4. View Deployment history

---

## 🚀 Advanced Features

### Deploy to Multiple Environments

Edit `.github/workflows/flutter-ci-cd.yml`:

```yaml
# Add this after flutter build web
- name: Deploy to correct target
  run: |
    if [ "${{ github.ref }}" = "refs/heads/main" ]; then
      firebase deploy --only hosting:flutter-admin --token ${{ secrets.FIREBASE_TOKEN }}
    elif [ "${{ github.ref }}" = "refs/heads/develop" ]; then
      firebase deploy --only main-dashboard --token ${{ secrets.FIREBASE_TOKEN }}
    fi
```

### Automatic Changelog Generation

Add to your commit message:

```
feat: add user profile feature
[CHANGELOG] Added user profile screen with avatar support
```

### Release APK to Google Play

Add step to workflow:

```yaml
- name: Upload to Google Play
  uses: r0adkll/upload-google-play@v1
  with:
    serviceAccountJson: ${{ secrets.GOOGLE_PLAY_KEY_JSON }}
    packageName: com.supremeai.admin
    releaseFiles: flutter_admin_app/build/app/outputs/bundle/release/app.aab
```

---

## 📞 Getting Help

1. **Check logs:** GitHub Actions → Workflow → Step output
2. **Flutter docs:** https://flutter.dev/docs
3. **Firebase docs:** https://firebase.google.com/docs
4. **GitHub Actions:** https://docs.github.com/en/actions

---

## ✅ Checklist

Before considering setup complete:

- [ ] Flutter SDK installed and verified
- [ ] `flutter build web --release` works locally
- [ ] Firebase project created and configured
- [ ] `.firebaserc` and `firebase.json` configured
- [ ] Firebase token generated via `firebase login:ci`
- [ ] `FIREBASE_TOKEN` secret added to GitHub
- [ ] Pushed code to main branch
- [ ] GitHub Actions workflow completed successfully
- [ ] Web app live at Firebase URL
- [ ] Can view logs in GitHub Actions

---

## 🎯 Next Steps

1. **Customize the app:** Edit `flutter_admin_app/lib/` and push
2. **Add more screens:** Create new Dart files, they auto-deploy
3. **Configure domain:** Add custom domain in Firebase Hosting
4. **Monitor analytics:** Set up Firebase Analytics
5. **Enable offline mode:** Implement service workers
6. **Mobile app:** Build and release Android APK/AAB

---

## Summary

You now have:

✅ **Automated CI/CD Pipeline** - Builds and tests on every push  
✅ **Firebase Auto-Deploy** - Pushes to production automatically  
✅ **Code Quality Checks** - Analyzes code, runs tests  
✅ **Mobile Build** - Creates Android APK and App Bundle  
✅ **GitHub Notifications** - Comments on PRs with status  

**Time to deployment:** Push to main → Live in ~10 minutes ⚡

---

**Last Updated:** March 31, 2026  
**Version:** 1.0.0  
**Status:** ✅ Production Ready
