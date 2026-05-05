# SupremeAI Architecture

## System Overview
Monorepo with multi-agent AI system for automated app generation.

## Module Structure
```
supremeai/
├── src/main/java/com/supremeai/    # Spring Boot 3 backend (Java 21)
│   ├── codeflow/                    # CodeFlow analysis module (core feature)
│   ├── ai/                         # Multi-provider AI routing
│   ├── controller/                  # REST API endpoints
│   ├── service/                     # Business logic
│   └── repository/                 # Firebase data layer
├── dashboard/                       # React + TypeScript + Vite frontend
├── supremeai/                       # Flutter admin app (Android/iOS)
├── supremeai-vscode-extension/      # VS Code extension (scaffold)
├── supremeai-intellij-plugin/        # IntelliJ plugin (v1.2.0, K2 ready)
├── command-hub/                      # CLI tools
└── functions/                        # Firebase Cloud Functions
```

## Key Design Patterns
- **3-Layer Flow**: Controller (validation) → Service (business logic) → Repository (Firestore)
- **Security**: Permission checks in service layer, JWT auth
- **Cloud-First**: Firebase backend, no local fallbacks for core features
- **Solo-Capable**: Features work offline with degraded AI capabilities

## Critical Components
1. **CodeFlow Module**: Only production-ready feature. 409 Java files, 53k+ lines. Supports 13+ vulnerability scanners.
2. **AI Provider Routing**: 11 connectors with automatic failover. Keys from env vars.
3. **Firebase Integration**: Auth, Firestore, Functions. Modular SDK in dashboard, compat in legacy pages.

## Known Issues
- Firebase SDK mismatch between legacy login and React dashboard
- JWT "Forbidden" errors from token expiry
- Dashboard uses static data (pending API integration)
- Committed build artifacts bloat repo size (fixed via .gitignore)