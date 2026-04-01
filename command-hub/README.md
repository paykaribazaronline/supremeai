# SupremeAI CommandHub 🎛️

A comprehensive command orchestration system to manage, monitor, and improve your SupremeAI infrastructure.

## 📋 Overview

CommandHub provides a unified interface to:
- **Monitor** system health and performance
- **Execute** commands to improve data collection, AI providers, and deployments
- **Queue** async tasks for batch improvements
- **Track** command execution history and results
- **Manage** AI accounts, budgets, and quotas

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│               Admin Dashboard (Web)                  │
│            (React/Vue + WebSocket)                  │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────┴──────────────────────────────────┐
│                 REST API Layer                       │
│          (Spring Boot /api/commands/*)              │
└──────┬───────────────────────────────────────┬──────┘
       │                                       │
┌──────▼──────────────────┐     ┌─────────────▼──────┐
│   Command Executor       │     │  Message Queue      │
│   (Sync Commands)        │     │  (Async Commands)   │
│   - Monitor             │     │  - RabbitMQ/Kafka  │
│   - Health Check        │     │  - Redis           │
│   - Instant Updates     │     │  - Job persistence │
└──────┬──────────────────┘     └─────────────┬──────┘
       │                                       │
┌──────▼───────────────────────────────────────▼──────┐
│         CommandHub Core Framework                    │
│  ┌──────────────────────────────────────────────┐   │
│  │ Command Registry & Dispatcher Pattern         │   │
│  │ - Command Interface                          │   │
│  │ - CommandResult & Logging                    │   │
│  │ - Permission & Validation                    │   │
│  └──────────────────────────────────────────────┘   │
└──────┬──────────────────────────────────────────────┘
       │
┌──────▼──────────────────────────────────────────────┐
│      Existing SupremeAI Services                     │
│  - DataCollectorService                             │
│  - HybridDataCollector                              │
│  - AIAccountManager                                 │
│  - BudgetManager                                    │
│  - FirebaseService                                  │
│  - CloudDeploymentService                           │
└──────────────────────────────────────────────────────┘
```

## 📦 Modules

### 1. **core/** - Command Framework (Java)
Base classes and interfaces for the command system.

```
core/
├── Command.java              # Base interface
├── CommandExecutor.java      # Executes commands
├── CommandResult.java        # Command response
├── CommandRegistry.java      # All available commands
└── commands/
    ├── MonitoringCommands.java
    ├── DataCollectionCommands.java
    ├── ProviderManagementCommands.java
    ├── OptimizationCommands.java
    └── DeploymentCommands.java
```

### 2. **cli/** - Command-Line Tool (Go/Python)
Lightweight CLI for admins to execute commands.

```
cli/
├── main.go/main.py
├── client/
├── commands/
└── config.yaml
```

### 3. **dashboard/** - Admin Web UI (React/Vue)
Real-time command execution and monitoring dashboard.

### 4. **api/** - REST Endpoints
New Spring Boot controller for command management.

## 🚀 Quick Start

### 1. Start SupremeAI Backend

```bash
cd supremeai
./gradlew bootRun
```

### 2. Deploy CommandHub Services

```bash
cd command-hub
./deploy.sh
```

### 3. Access Admin Dashboard

```
http://localhost:3000
```

### 4. Use CLI

```bash
supcmd health-check
supcmd improve --all
supcmd quota-status
```

## 📝 Command Categories

| Category | Purpose | Trigger Type |
|----------|---------|--------------|
| **Monitoring** | Check system health, view metrics | Sync |
| **Data Collection** | Refresh GitHub/Vercel/Firebase data | Async |
| **Provider Management** | Add/update/rotate AI accounts | Sync/Async |
| **Auto-Healing** | Fix failures, optimize quotas | Async |
| **Deployment** | Trigger deployments, rollbacks | Async |
| **Cleanup** | Archive logs, cleanup cache | Async |
| **Configuration** | Update system settings | Sync |

## 🔧 Available Commands

### Monitoring

```bash
supcmd health-check              # Full system health
supcmd quota-status              # All quotas
supcmd provider-status [NAME]    # Specific provider
supcmd recent-errors             # Last 10 errors
```

### Data Refresh

```bash
supcmd refresh-github            # Fetch from GitHub API
supcmd refresh-vercel            # Fetch from Vercel API
supcmd refresh-firebase          # Fetch Firebase metrics
supcmd refresh --all             # All sources
```

### System Optimization

```bash
supcmd optimize-quotas           # Auto-adjust quotas
supcmd heal-failures             # Fix failed requests
supcmd rotate-keys               # Rotate API keys
supcmd cleanup-cache             # Clear cache
```

### Admin Actions

```bash
supcmd account-list              # List AI accounts
supcmd account-add [NAME] [KEY]  # Add new account
supcmd budget-set [ACCOUNT] $100 # Set budget
supcmd approve-provider [NAME]   # Approve provider
```

## 📊 Dashboard Features

- 📈 Real-time metrics and graphs
- 🎯 Command execution queue
- 📋 Command history & logs
- 🔔 Alerts and notifications
- 👥 User permissions & audit trail
- ⚙️ System configuration UI
- 🔄 Integration status
- 📱 Responsive mobile design

## 🔐 Security

- ✅ Role-based access control (RBAC)
- ✅ API token authentication
- ✅ Command validation & whitelist
- ✅ Audit logging
- ✅ Rate limiting per user
- ✅ Request signing for CLI

## 💾 Command Execution Flow

### Sync Commands (Real-time)

```
CLI/API Request 
  → CommandValidator 
    → CommandExecutor 
      → Result returned immediately 
        → Logged in Firestore
```

### Async Commands (Queue-based)

```
CLI/API Request 
  → CommandQueue 
    → Worker Pool 
      → Command Executor 
        → Results stored in DB 
          → WebSocket notification
```

## 🛠️ Tech Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Core Framework | Java 17 + Spring Boot | Command backend |
| CLI | Go / Python 3.9+ | Command-line tool |
| Dashboard | React 18 / Vue 3 | Web UI |
| Queue | RabbitMQ / Redis | Async jobs |
| Database | Firestore / PostgreSQL | Persistense |
| Communication | REST + WebSocket | Real-time updates |
| Auth | JWT + Firebase Auth | Security |

## 📚 Implementation Phases

### Phase 1: Core Framework ✅
- [x] Command base classes
- [x] Command registry
- [x] Basic executor

### Phase 2: Monitoring Commands 🔄
- [ ] Health check command
- [ ] Quota status command
- [ ] Metrics collection

### Phase 3: Data Collection Commands
- [ ] Refresh GitHub data
- [ ] Refresh Vercel status
- [ ] Refresh Firebase metrics

### Phase 4: Admin Dashboard
- [ ] React setup
- [ ] Component library
- [ ] Real-time updates

### Phase 5: CLI Tool
- [ ] Go/Python CLI
- [ ] Config management
- [ ] Auto-completion

### Phase 6: Message Queue
- [ ] RabbitMQ setup
- [ ] Job persistence
- [ ] Worker pool

### Phase 7: Production Ready
- [ ] Unit tests
- [ ] Integration tests
- [ ] Documentation
- [ ] Deployment guides

## 📖 Documentation Structure

```
docs/
├── ARCHITECTURE.md       # System design
├── DEPLOYMENT.md         # How to deploy
├── COMMAND_REFERENCE.md  # All commands
├── API_SPEC.yaml         # OpenAPI spec
└── USER_GUIDE.md         # End-user guide
```

## 🔗 Integration Points

CommandHub integrates with existing SupremeAI components:

1. **DataCollectorService** - Schedule data refresh
2. **AIAccountManager** - Manage accounts via commands
3. **BudgetManager** - Monitor & adjust budgets
4. **HybridDataCollector** - Run collection jobs
5. **CloudDeploymentService** - Trigger deployments
6. **AdminMessagePusher** - Send notifications
7. **FirebaseService** - Store command history

## 🎯 Benefits

✅ **Centralized Control** - One place to manage everything
✅ **Automation Ready** - Schedule commands, run in parallel
✅ **Real-time Monitoring** - Live dashboard with updates
✅ **Scalable** - Handle 1000s of commands/minute
✅ **Reliable** - Persistent queue for async jobs
✅ **Auditable** - Full command execution history
✅ **User-friendly** - CLI + Web UI + REST API

## 🚀 Next Steps

1. Review `ARCHITECTURE.md` for detailed design
2. Check `core/` folder for implementation
3. Run `deploy.sh` to start services
4. Access dashboard at `http://localhost:3000`
5. Use CLI: `supcmd health-check`

---

**Created:** March 27, 2026
**Status:** In Development
