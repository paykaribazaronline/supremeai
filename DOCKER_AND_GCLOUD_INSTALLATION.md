# Docker & Google Cloud SDK Installation Guide

## 📋 Prerequisites Check

**Already Installed:** ✅

- Git (2.53.0)
- Java 17 (LTS)

**Need to Install:** ❌

- Docker Desktop
- Google Cloud SDK

---

## 🐳 Step 1: Install Docker Desktop

### Download

1. Go to: **https://www.docker.com/products/docker-desktop**
2. Click **"Download for Windows"**
3. Choose your Windows version:
   - Windows 11/10 Pro/Enterprise: Use WSL 2 backend (recommended)
   - Windows Home: Will automatically use WSL 2

### Installation Steps

1. Run the downloaded `Docker Desktop Installer.exe`
2. Check the box: **"Install required Windows components for WSL 2"**
3. Click **Install**
4. **IMPORTANT:** When installation completes, restart your computer
5. After restart, Docker Desktop should autostart
6. Wait for Docker icon in system tray to show "Docker is running"

### Verify Docker Installation

```powershell
# Open NEW PowerShell window after restart
docker --version

# Expected output:
# Docker version 25.0.0 (or later), build e758fe5
```

If you see the version, Docker is installed! ✅

---

## ☁️ Step 2: Install Google Cloud SDK

### Download

1. Go to: **https://cloud.google.com/sdk/docs/install-sdk**
2. Click **Windows 64-bit Interactive Installer**
3. Save the file: `google-cloud-sdk-installer.exe`

### Installation Steps

1. Run `google-cloud-sdk-installer.exe`
2. Accept the license agreement
3. Choose installation location (default is fine: `C:\Program Files (x86)\Google\Cloud SDK`)
4. Check box: **"Start Google Cloud SDK Shell"**
5. Click **Install**
6. Wait for installation to complete (5-10 minutes)
7. Uncheck "Start Google Cloud SDK Shell" and click **Finish**
8. **IMPORTANT:** Open a NEW PowerShell window (don't use the SDK shell)

### Verify Google Cloud SDK Installation

```powershell
# Open NEW PowerShell window (CMD Prompt or PowerShell)
gcloud --version

# Expected output:
# Google Cloud SDK 476.0.0 (or later)
# bq 2.1.0
# ...
```

If you see the version, gcloud is installed! ✅

---

## 🔑 Step 3: Setup Google Cloud Authentication

### Login to Google Account

```powershell
gcloud auth login
```

This will:

1. Open your default web browser
2. Show Google login page
3. You login with your Google account
4. Grant permissions to Google Cloud SDK
5. Browser shows confirmation code
6. PowerShell automatically completes

### Verify Authentication

```powershell
gcloud auth list

# Should show your email as active
```

---

## 📂 Step 4: Create GCP Project

### Create New Project

```powershell
gcloud projects create supremeai-production --name="SupremeAI Production"
```

### Set as Default Project

```powershell
gcloud config set project supremeai-production

# Verify it's set:
gcloud config list
# Should show: project = supremeai-production
```

---

## 🔧 Step 5: Enable Required APIs

```powershell
gcloud services enable `
  run.googleapis.com `
  firestore.googleapis.com `
  cloudbuild.googleapis.com `
  containerregistry.googleapis.com `
  --quiet
```

This enables the services needed for deployment.

---

## 🐳 Step 6: Configure Docker Authentication

```powershell
gcloud auth configure-docker

# When prompted, type 'y' and press Enter
```

This allows Docker to push images to Google Container Registry.

---

## ✅ Verification Checklist

After all steps, verify everything works:

```powershell
# 1. Verify Docker
docker --version
# Should show: Docker version 25.0.0 (or later)

# 2. Verify Google Cloud SDK
gcloud --version
# Should show: Google Cloud SDK 476.0.0 (or later)

# 3. Verify Google Cloud Authentication
gcloud auth list
# Should show your email

# 4. Verify Project is Set
gcloud config list
# Should show: project = supremeai-production

# 5. Test Docker daemon
docker ps
# Should show: CONTAINER ID, IMAGE, COMMAND, etc. headers
# (No error messages)
```

---

## 🚀 Ready to Deploy

Once all verifications pass, you can deploy:

```powershell
cd c:\Users\Nazifa\supremeai
.\deploy-to-gcp.ps1 -DeployBoth
```

---

## 🛠️ Troubleshooting

### Docker Issues

**Problem:** "Docker command not found"

- **Solution:** Restart PowerShell/CMD, or restart computer if just installed

**Problem:** "Cannot connect to Docker daemon"

- **Solution:**
  1. Open Docker Desktop (search in Start menu)
  2. Wait for icon in system tray to show "Docker is running"
  3. Try docker --version again

**Problem:** "WSL 2 installation required"

- **Solution:**
  1. Docker installer will prompt you to enable WSL 2
  2. It will install Windows subsystem for Linux automatically
  3. Restart your computer
  4. Run Docker Desktop again

### Google Cloud SDK Issues

**Problem:** "gcloud command not found"

- **Solution:**
  1. Restart PowerShell/CMD
  2. If still doesn't work: Add SDK to PATH
  3. Run `$env:Path -split ';' | Select-String 'google'` to check

**Problem:** "Authentication failed"

- **Solution:**
  1. Run: `gcloud auth login --browser-only`
  2. If still fails, check internet connection
  3. Try from different browser

**Problem:** "Project creation failed"

- **Solution:**
  1. Ensure you have active Google Cloud account
  2. Check you have billing enabled
  3. Wait a moment and try again

---

## ⏱️ Time Estimates

| Task | Time |
|------|------|
| Download Docker | 5-10 min |
| Install Docker | 5 min |
| Restart computer | 5 min |
| Download gcloud | 2 min |
| Install gcloud | 10 min |
| Login to Google | 2 min |
| Create GCP project | 2 min |
| Enable APIs | 3 min |
| Configure Docker auth | 1 min |
| **TOTAL** | **~35 minutes** |

---

## 📞 Support

If you get stuck:

1. Read the error message carefully
2. Check the Troubleshooting section above
3. Google Cloud docs: https://cloud.google.com/docs
4. Docker docs: https://docs.docker.com/

---

## Next Steps (After Installation)

Once installation is complete:

```powershell
# 1. Verify all tools installed
docker --version
gcloud --version

# 2. Login to Google Cloud
gcloud auth login

# 3. Setup project
gcloud projects create supremeai-production --name="SupremeAI Production"
gcloud config set project supremeai-production

# 4. Enable APIs
gcloud services enable run.googleapis.com firestore.googleapis.com cloudbuild.googleapis.com containerregistry.googleapis.com --quiet

# 5. Configure Docker
gcloud auth configure-docker --quiet

# 6. Deploy!
cd c:\Users\Nazifa\supremeai
.\deploy-to-gcp.ps1 -DeployBoth
```

You'll be live in ~15 minutes! 🚀

---

**Status:** Ready to install
**Created:** March 28, 2026
