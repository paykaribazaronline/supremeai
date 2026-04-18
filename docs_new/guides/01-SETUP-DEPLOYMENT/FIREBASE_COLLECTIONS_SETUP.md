# Firebase Collections Setup - Windows PowerShell

**Status:** 🟢 READY TO SETUP  
**Date:** April 2, 2026  
**8 Collections:** Ready with real data

---

## 🚀 Quick Start (3 Steps)

### **Step 1: Install Firebase CLI**

```powershell
npm install -g firebase-tools
```

### **Step 2: Login to Firebase**

```powershell
firebase login
firebase use supremeai-a
```

### **Step 3: Run Setup Script**

```powershell
python firebase_collections_setup.py
```

---

## 📦 What Gets Created

| Collection | Entries | Purpose |
|-----------|---------|---------|
| `app_templates` | 3 | Reusable app templates (Todo, Chat, Store) |
| `architectures` | 3 | AI-voted architectures |
| `ai_performance_by_task` | 4 | Track which AI is best |
| `patterns` | 4+ | Proven code patterns |
| `generation_errors_and_fixes` | 8 | Known errors + solutions |
| `generated_apps` | 3 | Track generated apps |
| `deployment_configs` | 3 | Deployment templates |
| `code_generators` | 1+ | Code templates |

---

## ✅ Prerequisites

```powershell
# Check Python installed
python --version
# Output: Python 3.9+

# Check Node installed
node --version
node -v
# Output: v18+

# Check npm installed
npm --version
# Output: 9+
```

### **If Missing - Install:**

```powershell
# Python: Download from python.org
# Node: Download from nodejs.org

# Or use Chocolatey:
choco install python nodejs
```

---

## 🔧 Installation Commands

### **1. Install Firebase Admin SDK**

```powershell
pip install firebase-admin

# Verify
python -c "import firebase_admin; print('✅ Firebase Admin SDK installed')"
```

### **2. Setup Credentials**

**Option A: Using Environment Variable** (Recommended)

```powershell
# Recommended for local development:
gcloud auth application-default login

# Verify ADC works
gcloud auth application-default print-access-token

# Optional file-based override if you need a service account JSON
$env:FIREBASE_CREDENTIALS_FILE = ".\secrets\service-account.json"

# Or use the standard Google env var
$env:GOOGLE_APPLICATION_CREDENTIALS = ".\secrets\service-account.json"

# Verify
echo $env:FIREBASE_CREDENTIALS_FILE
```

**Option B: Firebase CLI**

```powershell
firebase login
firebase use supremeai-a
```

### **3. Run Setup**

```powershell
cd c:\Users\Nazifa\supremeai

# Run Python script
python firebase_collections_setup.py

# Expected Output:
# ✅ Connected to Firebase!
# 📁 Creating app_templates collection...
#    ✅ Added 3 templates
# ... (all 8 collections)
# ✅ ✅ ✅ ALL COLLECTIONS CREATED SUCCESSFULLY! ✅ ✅ ✅
```

---

## 🔍 Verify Collections Created

### **Method 1: Firebase Console**

1. Open: https://console.firebase.google.com
2. Select: `supremeai-a` project
3. Click: Firestore Database
4. Check: All 8 collections visible

### **Method 2: Firebase CLI**

```powershell
firebase firestore:indexes create
firebase firestore:docs-list app_templates

# Or view all collections
firebase firestore:docs-list
```

### **Method 3: Python Script**

```powershell
python -c "
import firebase_admin
from firebase_admin import credentials, firestore

cred = credentials.Certificate(os.getenv('FIREBASE_CREDENTIALS_FILE'))
firebase_admin.initialize_app(cred)
db = firestore.client()

collections = db.collections()
for coll in collections:
    docs = coll.stream()
    count = sum(1 for _ in docs)
    print(f'✅ {coll.id}: {count} documents')
"
```

---

## 📊 Data Verification

After setup, verify data:

```powershell
# Check app_templates
firebase firestore:docs-list app_templates

# Check generated_apps
firebase firestore:docs-list generated_apps

# Check ai_performance_by_task
firebase firestore:docs-list ai_performance_by_task
```

---

## 🧪 Test Collections

```powershell
# Query a template
python -c "
import firebase_admin
from firebase_admin import credentials, firestore

cred = credentials.Certificate(os.getenv('FIREBASE_CREDENTIALS_FILE'))
firebase_admin.initialize_app(cred)
db = firestore.client()

doc = db.collection('app_templates').document('todo_app').get()
print('📝 Todo App Template:')
print(doc.to_dict())
"
```

---

## 📝 Manual Setup (If Script Fails)

If the Python script fails, you can manually create collections via Firebase Console:

### **1. Create app_templates Collection**

```json
{
  "name": "Todo Application",
  "complexity": "MEDIUM",
  "features": ["CRUD operations", "Search/filter", "Persistence"]
}
```

### **2. Create architectures Collection**

```json
{
  "scenario": "Todo/List CRUD app",
  "consensus": "REST API + Firebase",
  "confidence": 0.89
}
```

*(Continue for remaining collections)*

---

## 🐛 Troubleshooting

### **Error: "Credentials file not found"**

```
Solution:
1. Download service account key from Firebase Console
2. Save it outside the repo root, for example: .\secrets\service-account.json
3. Set: $env:FIREBASE_CREDENTIALS_FILE=".\secrets\service-account.json"
```

### **Error: "Permission denied"**

```
Solution:
1. Check Firebase permissions for service account
2. Ensure account has "Editor" role
3. Regenerate credentials
```

### **Error: "Connection timeout"**

```
Solution:
1. Check internet connection
2. Check Firebase project is active
3. Verify credentials are valid
```

### **Error: "Python not recognized"**

```
Solution:
1. Check Python installed: python --version
2. Add Python to PATH
3. Use: py firebase_collections_setup.py
```

---

## ✅ Success Checklist

- [ ] Python installed (python --version)
- [ ] Firebase admin SDK installed (pip list | findstr firebase)
- [ ] ADC configured or FIREBASE_CREDENTIALS_FILE set
- [ ] Environment variable set ($env:GOOGLE_APPLICATION_CREDENTIALS)
- [ ] Script executed (python firebase_collections_setup.py)
- [ ] All collections visible in Firebase Console
- [ ] Data verified in Firestore
- [ ] Ready for implementation!

---

## 🎯 What's Next

After setup completes:

1. ✅ **Phase 11:** Implement Teaching Backend Java code
2. ✅ **Phase 12:** Test app generation endpoint
3. ✅ **Phase 13:** Deploy to production

---

## 📞 Support Commands

```powershell
# List all Firebase CLI commands
firebase help

# List all collections
firebase firestore:docs-list

# View specific collection
firebase firestore:docs-list [collection]

# Import/Export data
firebase firestore:import firestore-export.json
firebase firestore:export firestore-export.json

# Emulator (for local testing)
firebase emulators:start
```

---

**Status:** 🟢 Ready to execute  
**Time:** 5-10 minutes to complete  
**Next:** Run `python firebase_collections_setup.py`
