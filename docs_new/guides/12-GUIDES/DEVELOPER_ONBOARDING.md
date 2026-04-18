# Developer Onboarding Guide

**Version:** 1.0  
**Last Updated:** April 5, 2026  
**Time to Complete:** 2-3 hours  
**Target Audience:** New developers joining the SupremeAI team

---

## Welcome to SupremeAI! 🎉

This guide will take you from zero to productive contributor in a few hours. Follow each step carefully.

---

## Table of Contents

1. [Pre-Requisites](#pre-requisites)
2. [Environment Setup](#environment-setup)
3. [Project Structure](#project-structure)
4. [Understanding the Codebase](#understanding-the-codebase)
5. [Running Locally](#running-locally)
6. [Development Workflow](#development-workflow)
7. [Testing](#testing)
8. [Common Tasks](#common-tasks)
9. [Resources](#resources)

---

## Pre-Requisites

### Required Software

| Software | Version | Purpose | Download |
|----------|---------|---------|----------|
| Java JDK | 17+ | Backend runtime | [Oracle](https://www.oracle.com/java/) or [OpenJDK](https://openjdk.org/) |
| Gradle | 8.7+ | Build tool | Bundled with project |
| Git | Latest | Version control | [git-scm.com](https://git-scm.com/) |
| IntelliJ IDEA | 2023+ | IDE (recommended) | [jetbrains.com](https://www.jetbrains.com/idea/) |
| Firebase CLI | Latest | Firebase tools | `npm install -g firebase-tools` |
| Google Cloud SDK | Latest | GCP deployment | [cloud.google.com/sdk](https://cloud.google.com/sdk) |

### Optional but Recommended

| Software | Purpose |
|----------|---------|
| Docker | Containerization |
| Postman | API testing |
| Node.js | Frontend tools |

### Accounts Needed

1. **GitHub** - Repository access
2. **Firebase** - Project access (ask admin to invite)
3. **Google Cloud** - GCP access (ask admin to invite)

---

## Environment Setup

### Step 1: Clone Repository

```bash
git clone https://github.com/paykaribazaronline/supremeai.git
cd supremeai
```

### Step 2: Verify Java Installation

```bash
java -version
# Should show: openjdk version "17" or higher

./gradlew --version
# Should show: Gradle 8.7 or higher
```

### Step 3: Configure Environment Variables

```bash
# Copy example environment file
cp .env.example .env

# Edit .env with your credentials
notepad .env  # Windows
# or
nano .env     # Linux/Mac
```

**Required variables:**

```properties
# Firebase
FIREBASE_SERVICE_ACCOUNT=your_base64_encoded_service_account
SUPREMEAI_FIREBASE_WEB_API_KEY=your_web_api_key

# AI Providers (at least one)
GEMINI_API_KEY=your_gemini_key
OPENAI_API_KEY=your_openai_key
DEEPSEEK_API_KEY=your_deepseek_key

# Security
JWT_SECRET=generate_a_long_random_string
BOOTSTRAP_TOKEN=your_bootstrap_token

# Admin
SUPREMEAI_ADMIN_EMAIL=your_email@example.com
SUPREMEAI_ADMIN_PASSWORD=your_secure_password
```

### Step 4: Firebase Setup

```bash
# Login to Firebase
firebase login

# Verify project access
firebase projects:list
# Should show: SupremeAI project
```

### Step 5: Verify Build

```bash
# Build the project
./gradlew clean build -x test

# Should show: BUILD SUCCESSFUL
```

---

## Project Structure

```
supremeai/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/supremeai/
│   │   │       ├── config/          # Configuration classes
│   │   │       ├── controller/      # REST API controllers
│   │   │       ├── service/         # Business logic
│   │   │       ├── model/           # Data models
│   │   │       ├── repository/      # Data access
│   │   │       ├── security/        # Auth & security
│   │   │       └── util/            # Utilities
│   │   └── resources/
│   │       ├── application.yml      # App config
│   │       └── static/              # Static assets
│   └── test/                        # Test files
├── docs/                            # Documentation
├── scripts/                         # Utility scripts
├── flutter_admin_app/              # Flutter admin UI
├── build.gradle.kts                # Build configuration
└── README.md                       # Project overview
```

### Key Directories Explained

| Directory | Purpose | When to Modify |
|-----------|---------|----------------|
| `controller` | API endpoints | Adding new APIs |
| `service` | Business logic | Changing how things work |
| `model` | Data structures | New data types |
| `config` | Configuration | New settings |
| `security` | Authentication | Auth changes |

---

## Understanding the Codebase

### Core Components

#### 1. Agent Orchestration (`org.example.agentorchestration`)

Coordinates AI agents. Key files:

- `AgentOrchestrationController.java` - API endpoints
- `AgentOrchestrator.java` - Core logic
- `AgentLearningController.java` - Learning system

#### 2. Code Generation (`org.example.api`)

Handles app generation. Key files:

- `CodeGenerationController.java` - Generation API
- `CodeValidationController.java` - Validation
- `ErrorFixingController.java` - Auto-healing

#### 3. Monitoring (`org.example.monitoring`)

System observability. Key files:

- `MetricsController.java` - Metrics API
- `AlertingController.java` - Alerts

### Data Flow

```
User Request
    ↓
Controller (REST API)
    ↓
Service (Business Logic)
    ↓
AI Agent (LLM Call)
    ↓
Consensus Engine
    ↓
Database (Firebase)
    ↓
Response
```

### Key Design Patterns

1. **Controller-Service-Repository**: Clean separation of concerns
2. **Circuit Breaker**: Resilience for external calls
3. **Observer**: Event-driven updates
4. **Strategy**: Pluggable AI providers

---

## Running Locally

### Option 1: Using Gradle (Recommended for Development)

```bash
# Start the application
./gradlew bootRun

# Application starts at http://localhost:8080
```

### Option 2: Using IDE

1. Open project in IntelliJ IDEA
2. Wait for Gradle sync
3. Find `SupremeAiApplication.java`
4. Click green play button

### Option 3: Using Docker

```bash
# Build Docker image
docker build -t supremeai .

# Run container
docker run -p 8080:8080 --env-file .env supremeai
```

### Verify Installation

```bash
# Health check
curl http://localhost:8080/actuator/health

# Should return: {"status":"UP"}
```

### Access Points

| Service | URL | Purpose |
|---------|-----|---------|
| API | http://localhost:8080 | REST API |
| Health | http://localhost:8080/actuator/health | System health |
| Admin | http://localhost:8001 | Admin dashboard |
| Monitor | http://localhost:8000 | Monitoring UI |

---

## Development Workflow

### Branch Strategy

```
main (production)
  ↓
develop (integration)
  ↓
feature/your-feature-name
```

### Making Changes

1. **Create branch**

   ```bash
   git checkout -b feature/my-feature
   ```

2. **Make changes**
   - Write code
   - Add tests
   - Update docs

3. **Test locally**

   ```bash
   ./gradlew test
   ./gradlew bootRun
   ```

4. **Commit**

   ```bash
   git add .
   git commit -m "feat: add new feature"
   ```

5. **Push and create PR**

   ```bash
   git push origin feature/my-feature
   ```

### Commit Message Format

```
type(scope): description

Types:
- feat: New feature
- fix: Bug fix
- docs: Documentation
- test: Tests
- refactor: Code refactoring
- chore: Maintenance

Examples:
feat(agent): add DeepSeek provider support
fix(auth): resolve JWT expiration issue
docs(api): update endpoint documentation
```

---

## Testing

### Running Tests

```bash
# All tests
./gradlew test

# Specific test
./gradlew test --tests AgentOrchestratorTest

# With coverage
./gradlew test jacocoTestReport
```

### Test Structure

```
src/test/
├── java/
│   └── com/supremeai/
│       ├── controller/     # Controller tests
│       ├── service/        # Service tests
│       └── integration/    # Integration tests
└── resources/              # Test resources
```

### Writing Tests

```java
@SpringBootTest
class AgentOrchestratorTest {
    
    @Autowired
    private AgentOrchestrator orchestrator;
    
    @Test
    void shouldOrchestrateAgents() {
        // Given
        Task task = new Task("test");
        
        // When
        Result result = orchestrator.execute(task);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
    }
}
```

---

## Common Tasks

### Adding a New API Endpoint

1. Create controller method:

```java
@RestController
@RequestMapping("/api/my-feature")
public class MyFeatureController {
    
    @GetMapping("/status")
    public ResponseEntity<Status> getStatus() {
        return ResponseEntity.ok(service.getStatus());
    }
}
```

2. Add service method:

```java
@Service
public class MyFeatureService {
    
    public Status getStatus() {
        // Implementation
    }
}
```

3. Write tests
4. Update API documentation

### Adding a New AI Provider

1. Implement `AIProvider` interface
2. Add configuration
3. Register in `ProviderRegistry`
4. Add tests

### Debugging

```bash
# Enable debug logging
./gradlew bootRun --debug

# Or set in application.yml
logging:
  level:
    com.supremeai: DEBUG
```

---

## Resources

### Essential Reading

1. [Architecture Overview](../02-ARCHITECTURE/ARCHITECTURE_AND_IMPLEMENTATION.md)
2. [Quick Start Guide](../00-START-HERE/QUICK_START_5MIN.md)
3. [API Reference](../13-REPORTS/API_ENDPOINT_INVENTORY.md)
4. [Contributing Guide](CONTRIBUTING.md)

### Getting Help

| Issue | Resource |
|-------|----------|
| Setup problems | [Troubleshooting](../09-TROUBLESHOOTING/QUICKSTART_TROUBLESHOOTING.md) |
| Architecture questions | [Architecture Doc](../02-ARCHITECTURE/ARCHITECTURE_AND_IMPLEMENTATION.md) |
| API questions | [API Inventory](../13-REPORTS/API_ENDPOINT_INVENTORY.md) |
| Security questions | [Security Guide](../05-AUTHENTICATION-SECURITY/SECURITY_GUIDE.md) |

### Tools & References

- [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Firebase Docs](https://firebase.google.com/docs)
- [Gradle Docs](https://docs.gradle.org/current/userguide/userguide.html)

---

## Onboarding Checklist

### Day 1: Setup

- [ ] Install required software
- [ ] Clone repository
- [ ] Configure environment variables
- [ ] Build project successfully
- [ ] Run application locally
- [ ] Access all dashboards

### Day 2: Understanding

- [ ] Read architecture overview
- [ ] Explore codebase structure
- [ ] Understand data flow
- [ ] Review key components
- [ ] Run tests successfully

### Day 3: First Contribution

- [ ] Pick a "good first issue"
- [ ] Create feature branch
- [ ] Make changes
- [ ] Write tests
- [ ] Create pull request

### Week 1: Integration

- [ ] Complete first PR merge
- [ ] Attend team standup
- [ ] Review team documentation
- [ ] Set up monitoring access
- [ ] Understand deployment process

---

## Feedback

Found issues with this guide? Please:

1. Create an issue in GitHub
2. Or suggest improvements via PR
3. Or contact the team lead

---

**Welcome to the team!** 🚀

---

**Last Updated:** April 5, 2026  
**Maintained by:** SupremeAI Dev Team  
**Status:** ✅ Complete
