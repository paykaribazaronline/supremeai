# SupremeAI

A multi-agent system for automated app generation with AI-powered code analysis.

## Core Features
- **CodeFlow Module**: Real-time code analysis with 95% security detection, 100-point health scoring, and support for Java/TypeScript/Python. Analyzes 50-file repos in <30s.
- **Multi-AI Provider Routing**: Integrates Kimi, DeepSeek, Together AI with automatic failover.
- **Spring Boot Backend**: Java 21 with virtual threads, Firebase integration, JWT authentication.
- **React Dashboard**: TypeScript + Vite with Bengali/English i18n support.
- **Cross-Platform Tools**: IntelliJ plugin (v1.2.0, K2 compatible), Flutter admin app, VS Code extension scaffold.

## Quick Start
1. **Backend**: `./gradlew bootRun` (requires Java 21)
2. **Dashboard**: `cd dashboard && npm run dev` (http://localhost:3000)
3. **Firebase**: Add `service-account.json` to `src/main/resources/`

## Documentation
- [Architecture](ARCHITECTURE.md)
- [Contributing](CONTRIBUTING.md)
- [CodeFlow Module](CODEFLOW_MODULE_README.md)
- [Agent Guidelines](AGENTS.md)

## Status
- ✅ CodeFlow: Production-ready
- ⚠️ Backend: Compilation fixed, JWT issues pending
- ⚠️ Dashboard: Static data pending API integration
- ❌ VS Code Extension: Scaffold only

## License
[Add license here]