# CommandHub - Complete Implementation Guide

## Overview

CommandHub is a comprehensive command orchestration platform for SupremeAI. It provides:
- **Core Framework** - Java-based command pattern implementation
- **REST API** - HTTP endpoints for command execution
- **CLI Tool** - Python CLI for admin use
- **Dashboard** - Web UI for monitoring and management
- **Message Queue** - Async job processing

## Architecture


```
┌─────────────────────────────────────────────────────────────┐
│                    Admin/User Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │   CLI Tool   │  │   Dashboard  │  │   Mobile App     │  │
│  │  (Python)    │  │  (React)     │  │   (Flutter)      │  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   API Layer                                 │
│  Spring Boot REST Controller (/api/commands/*)              │
│  - /execute      - Execute commands                         │
│  - /list         - List available commands                  │
│  - /{name}       - Get command details                      │
│  - /history      - Execution history                        │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              Command Execution Engine                       │
│  CommandExecutor (Registry + Dispatcher)                    │
│  - Command registration                                     │
│  - Permission validation                                    │
│  - Parameter validation                                     │
│  - Execution + Logging                                      │
└─────────────────────────────────────────────────────────────┘
                    ↙              ↖
┌──────────────────────┐    ┌────────────────────┐
│  Sync Execution      │    │  Async Execution   │
│  (Immediate)         │    │  (Message Queue)   │
│                      │    │                    │
│ • Health checks     │    │ • Data refresh     │
│ • Status queries    │    │ • Deployments      │
│ • Metrics           │    │ • Long-running ops │
└──────────────────────┘    └────────────────────┘
                    ↓              ↓
┌──────────────────────┐    ┌────────────────────┐
│  System Services     │    │  Worker Processes  │
│                      │    │                    │
│ • DataCollector      │    │ • Queue consumer   │
│ • BudgetManager      │    │ • Job processor    │
│ • QuotaTracker       │    │ • Result store     │
│ • AdminMsgPusher     │    │ • Notification     │
└──────────────────────┘    └────────────────────┘
```

## Core Components

### 1. Command Framework (`command-hub/core/`)

The foundation for all commands:

**Command.java** - Base interface
```java
public interface Command {
    String getName();
    CommandResult execute(Map<String, Object> params, CommandContext context);
    void validate(Map<String, Object> params);
    CommandCategory getCategory();
    CommandType getType();  // SYNC or ASYNC
    String[] getRequiredPermissions();
    CommandSchema getSchema();
}
```

**CommandExecutor.java** - Registry and dispatcher
```java
executor.register(command);                    // Register command
CommandResult result = executor.execute(       // Execute with validation
    "command-name",
    parameters,
    context
);
```

### 2. MonitoringCommands (`command-hub/core/MonitoringCommands.java`)

System health and metrics:

#### health-check


- Status: **SYNC**
- Permission: `view.health`
- Returns: Overall system health

```bash
supcmd exec health-check
```

#### quota-status


- Status: **SYNC**
- Permission: `view.quotas`
- Returns: Quota usage for all providers

```bash
supcmd exec quota-status
```

#### metrics


- Status: **SYNC**
- Permission: `view.metrics`
- Returns: System performance metrics

```bash
supcmd exec metrics
```

### 3. DataRefreshCommands (`command-hub/core/DataRefreshCommands.java`)

Data collection and synchronization:

#### refresh-github

- Status: **ASYNC**
- Permission: `execute.refresh`
- Params: owner, repo

```bash
supcmd exec refresh-github -p owner supremeai -p repo core
```

#### refresh-vercel

- Status: **ASYNC**
- Permission: `execute.refresh`
- Params: projectId

```bash
supcmd exec refresh-vercel -p projectId my-project
```

#### refresh-firebase

- Status: **ASYNC**
- Permission: `execute.refresh`

```bash
supcmd exec refresh-firebase
```

#### refresh-all

- Status: **ASYNC**
- Permission: `execute.refresh`
- Refreshes all data sources in parallel

```bash
supcmd exec refresh-all
```

## REST API Usage

### 1. Execute Command
```bash
POST /api/commands/execute

{
  "name": "health-check",
  "parameters": {
    "detailed": true
  }
}

Response (200):
{
  "commandName": "health-check",
  "success": true,
  "message": "Health check passed",
  "data": {
    "status": "HEALTHY",
    "timestamp": 1699564800000,
    "services": {...}
  }
}

Response (202 for async):
{
  "commandName": "refresh-github",
  "success": true,
  "message": "Command queued",
  "data": {
    "jobId": "uuid-123"
  }
}
```

### 2. List Commands
```bash
GET /api/commands/list?category=MONITORING&type=SYNC

Response:
{
  "success": true,
  "message": "Commands retrieved successfully",
  "commands": [
    {
      "name": "health-check",
      "description": "Check overall system health",
      "category": "MONITORING",
      "type": "SYNC",
      "permissions": ["view.health"]
    },
    ...
  ]
}
```

### 3. Get Command Details
```bash
GET /api/commands/refresh-github

Response:
{
  "success": true,
  "message": "Command details retrieved",
  "command": {
    "name": "refresh-github",
    "description": "Refresh GitHub repository data",
    "category": "DATA_REFRESH",
    "type": "ASYNC",
    "permissions": ["execute.refresh"]
  },
  "parameters": [
    {
      "name": "owner",
      "type": "string",
      "required": false,
      "default": "supremeai"
    },
    {
      "name": "repo",
      "type": "string",
      "required": false,
      "default": "core"
    }
  ]
}
```

### 4. Health Check
```bash
GET /api/commands/health

Response: 200
Commands service is healthy
```

## CLI Usage

### Installation
```bash
# Copy supcmd.py to /usr/local/bin or add to PATH
cp command-hub/cli/supcmd.py /usr/local/bin/supcmd
chmod +x /usr/local/bin/supcmd

# Or run directly with Python
python3 command-hub/cli/supcmd.py
```

### Basic Commands

**Health Check**
```bash
supcmd health                     # Check API server
supcmd exec health-check          # Execute health-check command
```

**List Commands**
```bash
supcmd list                                    # List all
supcmd list --category MONITORING              # By category
supcmd list --type SYNC                        # By type
```

**Get Command Info**
```bash
supcmd info health-check
supcmd info refresh-github
```

**Execute Commands**
```bash
# Simple execution
supcmd exec health-check

# With parameters
supcmd exec refresh-github -p owner supremeai -p repo core

# With authentication
supcmd exec health-check --token YOUR_API_TOKEN
```

**Authentication**
```bash
# Save token for future use
supcmd login YOUR_API_TOKEN

# Verify authentication
supcmd health
```

### Using Different API Server
```bash
supcmd --url http://prod-api.example.com:8080 exec health-check
```

## Implementation Phases

### Phase 1: Core Framework ✅ COMPLETE
- ✅ Command interface and base classes
- ✅ CommandExecutor (registry + dispatcher)
- ✅ Permission/validation framework
- ✅ CommandResult and status handling

### Phase 2: Command Implementations ✅ IN PROGRESS
- ✅ MonitoringCommands (health-check, quota-status, metrics)
- ✅ DataRefreshCommands (github, vercel, firebase, all)
- ⏳ ProviderManagementCommands (accounts, budgets, approval)
- ⏳ OptimizationCommands (quotas, healing, key rotation)
- ⏳ DeploymentCommands (deploy, rollback, status)

### Phase 3: REST API Integration ✅ IN PROGRESS
- ✅ CommandController with all endpoints
- ✅ Request/Response DTOs
- ✅ Error handling and status codes
- ⏳ Swagger/OpenAPI documentation
- ⏳ Authentication middleware

### Phase 4: CLI Tool ✅ IN PROGRESS
- ✅ Python CLI implementation
- ✅ Command execution
- ✅ List/Info subcommands
- ✅ Authentication handling
- ⏳ Installation scripts
- ⏳ Auto-completion setup
- ⏳ Config file management

### Phase 5: Dashboard (Pending)
- ⏳ React/Vue web application
- ⏳ Command execution UI
- ⏳ Real-time monitoring
- ⏳ Execution history viewer
- ⏳ WebSocket integration

### Phase 6: Message Queue (Pending)
- ⏳ RabbitMQ/Redis setup
- ⏳ AsyncCommandWorker
- ⏳ Job persistence
- ⏳ Status tracking

### Phase 7: Production Hardening (Pending)
- ⏳ Comprehensive testing
- ⏳ Performance optimization
- ⏳ Scaling and load balancing
- ⏳ Disaster recovery
- ⏳ Audit logging

## Integration with SupremeAI

### Adding to Spring Boot Application

**1. Create beans in configuration:**
```java
@Configuration
public class CommandHubConfig {
    
    @Bean
    public CommandExecutor commandExecutor(
            HybridDataCollector dataCollector,
            BudgetManager budgetManager,
            DataCollectorService collectorService) {
        
        CommandExecutor executor = new CommandExecutor();
        
        // Register monitoring commands
        MonitoringCommands monitoring = new MonitoringCommands(
            dataCollector, budgetManager, quotaTracker
        );
        executor.register(monitoring.getHealthCheckCommand());
        executor.register(monitoring.getQuotaStatusCommand());
        executor.register(monitoring.getMetricsCommand());
        
        // Register data refresh commands
        DataRefreshCommands refresh = new DataRefreshCommands(collectorService);
        executor.register(refresh.getRefreshGitHubCommand());
        executor.register(refresh.getRefreshVercelCommand());
        executor.register(refresh.getRefreshFirebaseCommand());
        executor.register(refresh.getRefreshAllCommand());
        
        return executor;
    }
}
```

**2. Inject into controller:**
```java
@RestController
@RequestMapping("/api/commands")
public class CommandController {
    
    private final CommandExecutor executor;
    
    public CommandController(CommandExecutor executor) {
        this.executor = executor;
    }
}
```

### Existing Service Integration Points

- **HybridDataCollector** - Provides getHealth() for health checks
- **DataCollectorService** - getGitHubData(), getVercelStatus(), getFirebaseStatus()
- **BudgetManager** - getCurrent(), setBudget() for budget commands
- **QuotaTracker** - getAllStatus() for quota monitoring
- **AdminMessagePusher** - For sending notifications on command completion

## Security Considerations

### 1. Permission Model
```java
// Commands define required permissions
String[] getRequiredPermissions()  // ["view.health", "execute.refresh"]

// Context validates permissions
context.hasPermission("view.health")  // true/false

// Executor checks before execution
if (!context.hasPermission(perm)) {
    return error("Insufficient permissions");
}
```

### 2. Role-Based Access Control (RBAC)
```java
// Context tracks user roles
context.hasRole("ADMIN")    // Full access
context.hasRole("USER")     // Limited access
context.hasRole("VIEWER")   // Read-only
```

### 3. Rate Limiting (Planned)
- Limit commands per user per time period
- Different limits for sync vs async
- Burst allowance for critical commands

### 4. Audit Logging
- All command executions logged with:
  - Command name and parameters
  - User who executed it
  - Execution time and result
  - Source IP and application
  - Full stack traces on error

## Testing

### Unit Tests (Planned)
```java
@Test
public void testHealthCheckCommand() {
    MonitoringCommands commands = new MonitoringCommands(...);
    Command cmd = commands.getHealthCheckCommand();
    
    CommandResult result = cmd.execute(
        new HashMap<>(),
        createTestContext()
    );
    
    assert(result.isSuccess());
    assert(result.getData() instanceof Map);
}
```

### Integration Tests (Planned)
```bash
# Test REST API
curl -X POST http://localhost:8080/api/commands/execute \
  -H "Content-Type: application/json" \
  -d '{"name":"health-check"}'

# Test CLI
supcmd exec health-check
```

### Load Testing (Planned)
- Concurrent command execution
- Async job queue under load
- Memory and CPU profiling

## Troubleshooting

### CLI Connection Issues
```bash
# Check API server is running
supcmd health

# Use verbose output
supcmd --url http://localhost:9999 exec health-check

# Check auth token
supcmd login INVALID_TOKEN
```

### Command Execution Failures
```bash
# Get command details
supcmd info command-name

# Check permissions
# Review logs for permission denied errors

# Validate parameters
# Review parameter schema from 'supcmd info'
```

### API Server Issues
```bash
# Check logs
tail -f logs/supremeai.log

# Verify CommandExecutor beans registered
# Check Spring context initialization

# Test endpoint directly
curl http://localhost:8080/api/commands/health
```

## File Structure

```
command-hub/
├── README.md                           (This file)
├── core/
│   ├── Command.java                    (Interface)
│   ├── CommandResult.java              (Response wrapper)
│   ├── CommandContext.java             (User/auth context)
│   ├── CommandEnums.java               (Category, Type)
│   ├── CommandSchema.java              (Parameter validation)
│   ├── CommandValidationException.java (Custom exception)
│   ├── CommandExecutor.java            (Registry + dispatcher)
│   ├── MonitoringCommands.java         (Health, quota, metrics)
│   └── DataRefreshCommands.java        (GitHub, Vercel, Firebase)
├── rest/
│   └── CommandController.java          (Spring REST endpoints)
├── cli/
│   └── supcmd.py                       (Python CLI tool)
├── dashboard/                          (React/Vue app - pending)
│   └── README.md
└── docs/
    ├── API.md                          (REST API documentation)
    ├── CLI.md                          (CLI usage guide)
    └── ARCHITECTURE.md                 (Detailed architecture)
```

## Next Steps

1. **Test Current Implementation**
   - Compile and verify Command* classes
   - Start Spring Boot with CommandController
   - Test REST endpoints with curl/Postman

2. **Implement Additional Commands**
   - ProviderManagementCommands
   - OptimizationCommands
   - DeploymentCommands

3. **Add Message Queue Support**
   - Setup RabbitMQ/Redis
   - Create AsyncCommandWorker
   - Implement job persistence

4. **Build Web Dashboard**
   - React/Vue component library
   - Real-time WebSocket updates
   - Command execution forms

5. **Production Hardening**
   - Comprehensive test suite
   - Performance optimization
   - Security audit and hardening
   - Documentation completion

## Support

For issues or questions:
1. Check troubleshooting section above
2. Review command documentation with `supcmd info <command>`
3. Check API server logs for detailed errors
4. Enable debug logging for CLI: `SUPCMD_DEBUG=1 supcmd exec health-check`

---

**Last Updated:** 2024
**Version:** 0.2
**Status:** Early Phase Implementation
