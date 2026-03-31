# System Architecture - SupremeAI

**Version:** 3.2 (Phase 11)  
**Last Updated:** March 31, 2026  
**Status:** Production-Ready with Advanced Features

---

## 📐 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENT LAYER                             │
├─────────────────┬───────────────────┬───────────────────────┤
│  Flutter Admin  │  Web Dashboard    │  REST API Clients     │
└────────┬────────┴─────────┬─────────┴──────────┬────────────┘
         │                  │                     │
         └──────────────────┼─────────────────────┘
                            │
         ┌──────────────────▼──────────────────┐
         │   API GATEWAY & LOAD BALANCER       │
         │  (Rate Limiting, Authentication)    │
         └──────────────────┬──────────────────┘
                            │
    ┌───────────────────────┼────────────────────────┐
    │                       │                        │
┌───▼──────────────┐ ┌──────▼──────────┐ ┌──────────▼─────────┐
│  REST API        │ │  WebSocket      │ │  Webhook Handler   │
│  v1.0            │ │  Real-time      │ │  Event Processing  │
└───┬──────────────┘ └──────┬──────────┘ └──────────┬─────────┘
    │                       │                        │
    └───────────┬───────────┴────────────┬───────────┘
                │                        │
     ┌──────────▼─────────────┐  ┌───────▼──────────────┐
     │   SERVICE LAYER        │  │  ASYNC PROCESSING    │
     ├────────────────────────┤  ├────────────────────┤
     │ • Authentication       │  │ • Task Queue       │
     │ • Project Management   │  │ • Background Jobs  │
     │ • Agent Orchestration  │  │ • Event Bus        │
     │ • Provider Management  │  └────────────────────┘
     │ • Metrics Collection   │
     │ • Request Processing   │
     └──────────┬─────────────┘
                │
    ┌───────────┴────────────┬──────────────┬──────────────┐
    │                        │              │              │
┌───▼──────────┐  ┌─────────▼─────┐  ┌────▼────────┐  ┌──▼─────────────┐
│   CACHE      │  │   DATABASE    │  │   STORAGE   │  │  MESSAGE QUEUE │
│   (Redis)    │  │   (Firebase)  │  │  (Cloud)    │  │   (Async)      │
└──────────────┘  └───────────────┘  └─────────────┘  └────────────────┘
```

---

## 🏗️ Component Architecture

### 1. **API Layer** (Port 8080)
```
REST API (HTTP/HTTPS)
├── /api/auth/          (Authentication)
├── /api/projects/      (Project Management)
├── /api/agents/        (AI Agent Management)
├── /api/providers/     (API Provider Management)
├── /api/metrics/       (Monitoring & Analytics)
├── /health             (Health Check)
└── /api/v2/*          (Next-Gen API - Versioned)
```

**Key Classes:**
- `ChatController` - Main API endpoint orchestrator
- `AuthenticationController` - JWT authentication
- `ProjectController` - Project CRUD operations
- `MetricsController` - Real-time metrics
- `ProviderManagementHandler` - API provider REST endpoints

### 2. **Service Layer** (Business Logic)
```
Core Services
├── AuthenticationService       (User auth + JWT)
├── AgentOrchestrator          (AI agent coordination)
├── ProjectManagementService   (Project lifecycle)
├── AIAPIService               (Generic AI calls)
├── MemoryManager              (Request context)
├── ConsensusEngine            (Multi-agent voting)
├── InternetSearchService      (Real-time data)
├── MetricsCollector          (Analytics)
├── CacheService              (Performance)
├── AuditLogger               (Security)
└── AlertManager              (Notifications)
```

**Key Data Flow:**
```
Request
  ↓
AuthenticationService (Token validation)
  ↓
Right Controller (Auth, Projects, Agents, etc.)
  ↓
Appropriate Service Layer
  ↓
Database (Firebase Firestore)
  ↓
Response + Metrics Logged
```

### 3. **Data Layer** (Firebase)

**Firestore Collections:**
```
supremeai-db/
├── projects/            (Project configurations)
│   └── {projectId}
│       ├── name
│       ├── description
│       ├── agents[]
│       └── config{}
├── api_providers/       (AI provider credentials)
│   └── {providerId}
│       ├── name
│       ├── apiKey (encrypted)
│       └── models[]
├── ai_agents/          (Agent definitions)
│   └── {agentId}
│       ├── role
│       ├── provider
│       └── model
├── users/              (Admin users)
│   └── {userId}
│       ├── email
│       ├── password (hashed)
│       └── role
└── admin_logs/         (Audit trail)
    └── {logId}
        ├── action
        ├── userId
        └── timestamp
```

### 4. **AI Agent Architecture**

**Three-Agent Consensus Model:**
```
Input Request
    ↓
┌───────────────────────────┐
│  ARCHITECT (Z-Architect)  │  DeepSeek Model
│  Role: Design & Plan      │  ├─ System design
│  Consensus: 70% required  │  ├─ Architecture
│  Priority: High           │  └─ Strategy
└───────────────────────────┘
    ↓
┌───────────────────────────┐
│  BUILDER (X-Builder)      │  GPT-4 Model
│  Role: Implement          │  ├─ Code generation
│  Consensus: 70% required  │  ├─ Implementation
│  Priority: High           │  └─ Execution
└───────────────────────────┘
    ↓
┌───────────────────────────┐
│  REVIEWER (Y-Reviewer)    │  Claude Model
│  Role: Quality Assurance  │  ├─ Code review
│  Consensus: 70% required  │  ├─ Best practices
│  Priority: High           │  └─ Quality gates
└───────────────────────────┘
    ↓
Consensus Vote (≥2 of 3 agree = PASS)
    ↓
Final Response
```

### 5. **Authentication Flow**

```
User Credentials
    ↓
AuthenticationService.authenticate()
    ├─ BCrypt password verification
    ├─ User lookup (Firebase)
    └─ JWT generation
    ↓
AccessToken (24h) + RefreshToken (7d)
    ↓
Client stores tokens
    ↓
Per API request:
    Authorization: Bearer {accessToken}
    ↓
AuthenticationFilter validates JWT
    ↓
Request proceeds or returns 401
    ↓
Token auto-refreshes if expired
```

### 6. **Request Processing Pipeline**

```
1. AUTHENTICATION
   ↓ AuthenticationFilter
   ↓ JWT validation
   ↓ User context set

2. RATE LIMITING
   ↓ RateLimitFilter
   ↓ Token bucket algorithm
   ↓ Per IP/User limits

3. REQUEST LOGGING
   ↓ AuditLogger
   ↓ Log to admin_logs collection

4. BUSINESS LOGIC
   ↓ Service layer processing
   ↓ Firebase operations
   ↓ External API calls

5. RESPONSE GENERATION
   ↓ Format response
   ↓ Add timing/metrics

6. METRICS COLLECTION
   ↓ Save to metrics cache
   ↓ Update dashboards
```

---

## 🔐 Security Architecture

### Authentication & Authorization
- **JWT-based:** 24h access tokens, 7d refresh tokens
- **Encryption:** BCrypt for passwords, AES for sensitive data
- **Rate Limiting:** 1000 req/min per IP, 5000 req/min authenticated
- **Audit Logging:** All API calls logged to admin_logs

### Data Protection
- **Encryption at Rest:** Sensitive fields encrypted using AES-256
- **Encryption in Transit:** HTTPS/TLS everywhere
- **API Keys:** Encrypted in Firebase, never logged
- **Private Network:** VPC/private subnets in cloud deployment

### Security Headers
- `Content-Security-Policy` - XSS protection
- `X-Frame-Options` - Clickjacking protection
- `X-Content-Type-Options` - MIME sniffing protection
- `Strict-Transport-Security` - HTTPS enforcement

---

## 📊 Monitoring & Observability

### Metrics Collection Pipeline
```
Application Code
    ↓ MetricsCollector
    ↓ Record: requests, latency, errors
    ↓
Real-time Cache (Redis)
    ↓
Aggregation Service (Hourly)
    ↓
Time-series Database
    ↓
Dashboard Visualization
```

### Key Metrics
- **Request Metrics:** count, latency, success rate
- **Agent Metrics:** accuracy, response time, cost
- **Cost Metrics:** per provider, per project, per API key
- **System Metrics:** CPU, memory, database load
- **Error Metrics:** error types, frequency, root causes

### Alerting System
```
Metrics Threshold Exceeded
    ↓ AlertManager
    ↓ Notification Type Decision
    ├─ Email Alert
    ├─ Slack Message
    ├─ Dashboard Alert
    └─ Webhook Trigger
```

---

## 🚀 Deployment Architecture

### Multi-Environment Strategy
```
Development (Local)
├─ Java backend (localhost:8080)
├─ Flutter dev (localhost:8081)
└─ Firebase emulator

Staging
├─ Docker containers (GCP Cloud Run)
├─ Firebase staging project
└─ Database: staging collection

Production
├─ Kubernetes cluster or Cloud Run
├─ Firebase production project
├─ Read replicas for databases
└─ CDN for static assets
```

### CI/CD Pipeline
```
Git Push → GitHub Actions
    ↓
1. Build & Compile
   ├─ Java: gradle clean build
   └─ Flutter: flutter build web
    ↓
2. Unit Tests
   └─ Coverage >80%
    ↓
3. Security Scan
   ├─ Dependency check
   └─ SAST analysis
    ↓
4. Deploy to Staging
   └─ Blue-green deploy
    ↓
5. Integration Tests
   └─ End-to-end validation
    ↓
6. Deploy to Production
   └─ Canary rollout (10% → 50% → 100%)
    ↓
7. Health Check
   └─ Automated monitoring
```

---

## 🔄 Scalability Features

### Horizontal Scaling
- **Stateless Services** - Easy to replicate
- **Load Balancing** - Distribute traffic
- **Database Scaling** - Firestore auto-scales
- **Cache Layer** - Redis for hot data

### Vertical Scaling
- **Resource Tuning** - CPU/memory optimization
- **Connection Pooling** - Efficient DB connections
- **Query Optimization** - Indexed Firestore queries
- **Async Processing** - Non-blocking operations

### Performance Optimization
- **Caching Strategy** - 3-tier cache (memory → Redis → DB)
- **Compression** - Gzip on all responses
- **Response Batching** - Batch API requests
- **Lazy Loading** - Load data on-demand

---

## 🔌 Integration Points

### External Services
1. **AI Providers:**
   - OpenAI (GPT-4, GPT-3.5)
   - Deepseek (deepseek-chat)
   - Anthropic (Claude 3 series)
   - Other providers via plugin system

2. **Firebase Suite:**
   - Firestore (primary database)
   - Authentication (optional)
   - Cloud Functions (async jobs)
   - Cloud Storage (file uploads)

3. **Infrastructure:**
   - Google Cloud Platform (primary)
   - Optional: AWS, Azure, Oracle Cloud

4. **Monitoring:**
   - Prometheus (metrics)
   - Grafana (visualization)
   - OpenTelemetry (tracing)

---

## 📦 Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Backend** | Java | 17 |
| **Framework** | Spring Boot | 3.2.3 |
| **Database** | Firebase (Firestore) | Latest |
| **Cache** | Redis | 7.0+ |
| **Frontend** | Flutter | 3.27.0 |
| **API Documentation** | OpenAPI 3.0 | - |
| **Build** | Gradle | 8.7 |
| **Container** | Docker | Latest |
| **Orchestration** | Kubernetes/Cloud Run | Latest |
| **CI/CD** | GitHub Actions | - |

---

## 📈 Future Architecture (Roadmap)

### Phase 12-13 Planned Enhancements
- **GraphQL Layer** - Secondary query interface
- **Event Sourcing** - Full audit trail
- **Microservices** - Domain-driven services
- **Machine Learning** - Cost prediction, agent optimization
- **Multi-Tenancy** - SaaS-ready architecture

See [PHASE11_ROADMAP.md](PHASE11_ROADMAP.md) for detailed timeline.

---

**For API details, see [API_REFERENCE.md](API_REFERENCE.md)**  
**For setup, see [README.md](README.md) and [QUICK_START_5MIN.md](QUICK_START_5MIN.md)**
