# Contributing to SupremeAI

Thank you for your interest in contributing to SupremeAI! This document explains our development process and guidelines for contributors.

## 🤝 Code of Conduct

We are committed to providing a welcoming and inspiring community for all. Please read and follow our [Code of Conduct](..\..\CODE_OF_CONDUCT.md).

---

## 📋 Quick Start

### Prerequisites

- **Java:** JDK 17+
- **Gradle:** 8.7+ (bundled with project)
- **Git:** Latest version

### Development Setup

```bash
# Clone the repository
git clone https://github.com/supremeai/supremeai.git
cd supremeai

# Install dependencies
./gradlew build

# Run tests
./gradlew test

# Start development server
./gradlew run
```

---

## 🌿 Git Workflow

### Branch Naming Convention

```bash
# Features
feature/x-builder/agent-consensus
feature/y-reviewer/code-quality
feature/z-architect/system-design

# Bug fixes
bugfix/firebase-deployment-timeout
hotfix/critical-memory-leak

# Documentation
docs/setup-instructions
docs/api-reference

# Refactoring
refactor/migrate-to-java17
```

### Commit Message Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```bash
# Feature
git commit -m "feat: add X-Builder agent consensus logic"

# Bug fix
git commit -m "fix: resolve Firebase timeout on Cloud Build"

# Documentation
git commit -m "docs: update setup instructions for Windows"

# Refactoring
git commit -m "refactor: migrate MetricsService to async stream processing"

# Performance
git commit -m "perf: optimize anomaly detection with caching"

# Tests
git commit -m "test: add comprehensive coverage for MLIntelligenceService"
```

### Pull Request Workflow

```bash
# 1. Create feature branch
git checkout -b feature/your-feature origin/main

# 2. Work on your feature
# ... make changes ...

# 3. Commit with conventional messages
git add .
git commit -m "feat: describe your feature"

# 4. Push to your fork
git push origin feature/your-feature

# 5. Create Pull Request on GitHub
# - Link related issues
# - Describe changes clearly
# - Wait for code review
```

---

## ✅ Code Quality Standards

### Java Code Style

- **Indentation:** 4 spaces
- **Line Length:** Max 120 characters
- **Naming:** camelCase for variables, PascalCase for classes
- **Documentation:** JavaDoc for public methods

### Example

```java
/**
 * Detects anomalies in metric stream using Z-score analysis.
 * 
 * @param metricName the name of the metric to analyze
 * @param values the list of metric values
 * @return map containing detected anomalies with classifications
 */
public Map<String, Object> detectAnomalies(String metricName, List<Double> values) {
    // Implementation
}
```

### Testing Requirements

- **Unit Tests:** Write tests for all public methods
- **Integration Tests:** Test service interactions
- **Coverage Target:** Minimum 75% code coverage

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests MLIntelligenceServiceTest

# Generate coverage report
./gradlew test jacocoTestReport
```

---

## 🐛 Reporting Issues

### Bug Report

Use the [Bug Report Template](..\..\.github\ISSUE_TEMPLATE\bug_report.md):

- **Title:** [BUG] Brief description
- **Agent:** Specify which agent (X-Builder/Y-Reviewer/Z-Architect)
- **Steps to Reproduce:** Clear reproduction steps
- **Logs:** Attach relevant logs (supremeai.log)
- **Environment:** Java version, OS, etc.

### Feature Request

Use the [Feature Request Template](..\..\.github\ISSUE_TEMPLATE\feature_request.md):

- **Title:** [FEATURE] Brief description
- **Use Case:** Why this feature is needed
- **Proposed Solution:** How it should work
- **Alternatives:** Other approaches considered

---

## 🚀 Development Phases

### Current Status (Phase 5)

- ✅ **Phase 1:** Foundation & Core Services
- ✅ **Phase 2:** Intelligence System (Ranking, Analysis)
- ✅ **Phase 3:** App Generator (Complete)
- ✅ **Phase 4:** Advanced Monitoring (Metrics, Alerts, WebSocket)
- ✅ **Phase 4.1:** WebSocket Real-Time + Phase 2 Intelligence
- ✅ **Phase 5:** Advanced Analytics & ML Intelligence

### Contributing to a Phase

1. Check [Phase Roadmap](..\02-ARCHITECTURE\PROJECT_ROADMAP.md)
2. Verify no one else is working on the same task
3. Comment on related issue: "I'm working on this"
4. Create feature branch: `feature/phase-X/your-task`
5. Submit PR when complete

---

## 📝 Documentation

### Updating Documentation

- Update relevant `.md` files when adding features
- Keep API documentation in sync with code
- Add examples for new endpoints
- Update CHANGELOG.md with breaking changes

### Adding API Documentation

```markdown
## POST /api/intelligence/ml/detect-anomalies
Detects anomalies in metric stream using 3-sigma Z-score method.

**Request:**
```json
{
  "metric": "memory_usage",
  "values": [45.2, 48.1, 47.9, 95.2, 49.1, 50.3]
}
```

**Response:**

```json
{
  "metric": "memory_usage",
  "anomalies": [
    {
      "value": 95.2,
      "zScore": 3.2,
      "type": "CRITICAL_ANOMALY"
    }
  ]
}
```

```

---

## 🔒 Security Guidelines

### Never Commit
- `.env` files with secrets
- `service-account.json` credentials
- API keys or tokens
- Database passwords
- Private keys (`.pem`, `.key`)

### Use Environment Variables

```bash
# Instead of hardcoding
export FIREBASE_SERVICE_ACCOUNT=$(cat service-account.json | base64)
export GEMINI_API_KEY=your_key_here
export DATABASE_URL=postgres://...
```

### Security Review Checklist

- [ ] No secrets in code
- [ ] Input validation on all endpoints
- [ ] SQL injection prevention
- [ ] XSS protection
- [ ] CORS configured correctly
- [ ] Authentication/Authorization checks
- [ ] Rate limiting on public endpoints

---

## 🧪 Testing Checklist

Before submitting a PR:

```bash
# 1. Run all tests
./gradlew test

# 2. Check code coverage
./gradlew jacocoTestReport

# 3. Build the project
./gradlew clean build

# 4. Run integration tests
./gradlew integrationTest

# 5. Check for security issues
./gradlew dependencyCheck

# 6. Format code
./gradlew formatCode
```

---

## 🚀 Docker & Deployment

### Local Docker Testing

```bash
# Build Docker image
docker build -t supremeai:latest .

# Run in Docker
docker run -p 8080:8080 \
  -e FIREBASE_SERVICE_ACCOUNT=$(cat service-account.json | base64) \
  supremeai:latest
```

### Deployment Test

- Test on local Docker first
- Test on Render staging environment
- Test on Google Cloud staging
- Only then deploy to production

---

## 👥 Team Roles

### Maintainers

- Review PRs
- Merge to main
- Release new versions
- Make architectural decisions

### Contributors

- Submit PRs for features/fixes
- Report issues
- Improve documentation
- Help review other PRs

### Community

- Ask questions in discussions
- Report bugs
- Request features
- Share how you're using SupremeAI

---

## 📞 Getting Help

- **Discussion:** [GitHub Discussions](https://github.com/supremeai/supremeai/discussions)
- **Issues:** [GitHub Issues](https://github.com/supremeai/supremeai/issues)
- **Email:** [contribution contact info]
- **Slack:** [Link to community Slack]

---

## 🎉 Recognition

Contributors are recognized in:

- CHANGELOG.md (for each release)
- GitHub Contributors page
- Release notes
- Project documentation

Thank you for contributing! 🚀
