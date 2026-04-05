# 🎯 Start Here - SupremeAI Quick Start

## ⚡ Get Started in 5 Minutes

### 1. **Clone & Setup** (1 min)

```bash
git clone https://github.com/supremeai/supremeai.git
cd supremeai
```

### 2. **Install Dependencies** (2 min)

**For Java Backend:**

```bash
./gradlew build
```

**For Flutter Frontend:**

```bash
cd flutter_admin_app
flutter pub get
```

### 3. **Configure Environment** (1 min)

```bash
# Copy example environment file
cp .env.example .env

# Update with your settings (Firebase, GCP credentials)
nano .env
```

### 4. **Run the Application** (1 min)

**Backend:**

```bash
./gradlew run
```

**Frontend (separate terminal):**

```bash
cd flutter_admin_app
flutter run
```

## 📚 What's Next?

### For Beginners

- 📖 Read [Architecture Overview](../02-ARCHITECTURE/PROJECT_STRUCTURE.md)
- 🚀 Follow [Deployment Guide](../01-SETUP-DEPLOYMENT/)
- 🛠️ Check [Troubleshooting](../09-TROUBLESHOOTING/)

### For Developers

- 💻 Explore [Phase Implementation](../03-PHASES/)
- 🔐 Setup [Authentication](../05-AUTHENTICATION-SECURITY/)  
- 🔄 Configure [CI/CD](../08-CI-CD/)

### For DevOps/SRE

- 🐳 Review [Deployment Options](../01-SETUP-DEPLOYMENT/)
- 📊 Check [Monitoring Setup](../11-PROJECT-MANAGEMENT/)  
- 🛡️ Implement [Security](../05-AUTHENTICATION-SECURITY/)

## ❓ Troubleshooting

**Build fails?**
→ See [Troubleshooting Guide](../09-TROUBLESHOOTING/COMPILATION_FIX.md)

**Deployment issues?**
→ Check [Deployment Errors](../01-SETUP-DEPLOYMENT/DEPLOYMENT_ERROR_REPORT.md)

**Permission errors?**
→ Review [GCP Permissions](../05-AUTHENTICATION-SECURITY/GCP_PERMISSION_FIX_GUIDE.md)

## 🎯 Key Resources

| Resource | Purpose |
|----------|---------|
| [Full Documentation](../README.md) | Complete docs index |
| [Architecture](../02-ARCHITECTURE/) | System design |
| [Phases 1-10](../03-PHASES/) | Implementation roadmap |
| [Admin Guide](../04-ADMIN/) | Operations |
| [Security](../05-AUTHENTICATION-SECURITY/) | Security setup |

---

**Time to first success:** ~10 minutes ⏱️  
**Need help?** → Check [Troubleshooting](../09-TROUBLESHOOTING/)  
**Want to dive deeper?** → See [Full Documentation](../README.md)
