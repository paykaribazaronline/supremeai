# SupremeAI Documentation Standards

**Version:** 1.0  
**Effective:** April 2, 2026

---

## 📋 Documentation Rule

### Root Folder (c:\Users\Nazifa\supremeai\)

**ONLY these files allowed:**

- ✅ `README.md` - Main project overview
- ✅ `LICENSE` - License file
- ✅ `CODE_OF_CONDUCT.md` - Community guidelines
- ✅ `ARCHITECTURE_AND_IMPLEMENTATION.md` - Master architecture (moved to docs/)
- ✅ `.gitignore`, `.env.example`, etc - Config files

### docs/ Folder

**ALL documentation goes here:**

```
docs/
├── 00-START-HERE/
├── 01-SETUP-DEPLOYMENT/
├── 02-ARCHITECTURE/
│   └── ARCHITECTURE_AND_IMPLEMENTATION.md  ← MOVED HERE
├── 03-PHASES/
├── ...
└── README.md  ← Navigation hub
```

**Why this rule?**

1. Clear organization
2. No root clutter
3. Single source of truth
4. Better maintainability
5. Easier for new developers

---

## 🤖 For SupremeAI System Generation

When generating new documentation, **ALWAYS:**

```java
// Rule in CodeGenerationOrchestrator or SelfExtender

if (isDocumentation) {
  String filepath = determineDocPath(docType);
  
  // MUST BE in docs/ folder, NOT root
  if (!filepath.startsWith("docs/") && !filepath.startsWith("docs\\")) {
    filepath = "docs/" + filepath;
    logger.warn("Auto-correcting doc path to docs folder");
  }
  
  generateFile(filepath, content);
  
  logger.info("✅ Generated documentation in docs/ folder: {}", filepath);
}
```

### Categories for Auto-Generation

| Type | Location | Example |
|------|----------|---------|
| Architecture | `docs/02-ARCHITECTURE/` | System design docs |
| Implementation | `docs/10-IMPLEMENTATION/` | Implementation guides |
| Guides | `docs/12-GUIDES/` | How-to guides |
| API Docs | `docs/06-FEATURES/` | API references |
| Troubleshooting | `docs/09-TROUBLESHOOTING/` | Error fixes |
| CI/CD | `docs/08-CI-CD/` | Pipeline docs |

---

## ✅ Root Folder Cleanup Checklist

**Files to KEEP in root:**

- ✅ `README.md`
- ✅ `LICENSE`
- ✅ `CODE_OF_CONDUCT.md`
- ✅ `.gitignore`
- ✅ `build.gradle.kts` (build config)
- ✅ `settings.gradle.kts`
- ✅ `gradlew`, `gradlew.bat`

**Files to REMOVE/MOVE from root:**

- ❌ `ARCHITECTURE_AND_IMPLEMENTATION.md` → `docs/02-ARCHITECTURE/`
- ❌ `CONFIG_QUICK_REFERENCE.md` → `docs/12-GUIDES/`
- ❌ `ENVIRONMENT_CONFIGURATION.md` → `docs/01-SETUP-DEPLOYMENT/`
- ❌ Old phase reports → `docs/13-REPORTS/`
- ❌ Test reports → `docs/13-REPORTS/`

---

## 🔄 Implementation

When you see docs in root folder:

1. ❌ Delete from root
2. ✅ Move/reference in docs/ folder
3. ✅ Update navigation
4. 📝 Log the consolidation

---

**Status:** Ready for SupremeAI to enforce ✅
