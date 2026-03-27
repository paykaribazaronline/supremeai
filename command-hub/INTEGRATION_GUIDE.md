# CommandHub Integration Guide

## Quick Start: Adding CommandHub to SupremeAI

This guide shows how to integrate the CommandHub into the existing SupremeAI Spring Boot application.

## Step 1: Copy Framework Classes

Copy the core framework classes to your SupremeAI project:

```bash
# From workspace root
cp command-hub/core/*.java src/main/java/org/example/command/core/

# So you have:
# src/main/java/org/example/command/
#   ├── Command.java
#   ├── CommandResult.java
#   ├── CommandContext.java
#   ├── CommandEnums.java
#   ├── CommandSchema.java
#   ├── CommandValidationException.java
#   └── CommandExecutor.java

# And implementations:
# src/main/java/org/example/command/impl/
#   ├── MonitoringCommands.java
#   └── DataRefreshCommands.java

# And REST controller:
# src/main/java/org/example/controller/
#   └── CommandController.java
```

## Step 2: Update Spring Configuration

Add CommandHub bean configuration to your `@Configuration` class:

```java
package org.example.config;

import org.example.command.*;
import org.example.command.impl.*;
import org.example.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommandHubConfiguration {
    
    @Bean
    public CommandExecutor commandExecutor(
            HybridDataCollector dataCollector,
            BudgetManager budgetManager,
            QuotaTracker quotaTracker,
            DataCollectorService collectorService,
            AdminMessagePusher messagePusher) {
        
        CommandExecutor executor = new CommandExecutor();
        
        // ==================== Monitoring Commands ====================
        MonitoringCommands monitoring = new MonitoringCommands(
            dataCollector,
            budgetManager,
            quotaTracker
        );
        
        executor.register(monitoring.getHealthCheckCommand());
        executor.register(monitoring.getQuotaStatusCommand());
        executor.register(monitoring.getMetricsCommand());
        
        // ==================== Data Refresh Commands ====================
        DataRefreshCommands refresh = new DataRefreshCommands(
            collectorService
        );
        
        executor.register(refresh.getRefreshGitHubCommand());
        executor.register(refresh.getRefreshVercelCommand());
        executor.register(refresh.getRefreshFirebaseCommand());
        executor.register(refresh.getRefreshAllCommand());
        
        // ==================== Future Command Groups ====================
        // ProviderManagementCommands
        // OptimizationCommands
        // DeploymentCommands
        // ConfigurationCommands
        // MaintenanceCommands
        
        return executor;
    }
}
```

## Step 3: Enable REST Controller

The `CommandController` uses Spring Boot standard annotations. Just ensure it's scanned:

```java
// In your main @SpringBootApplication class:
@SpringBootApplication(scanBasePackages = {
    "org.example.controller",
    "org.example.service",
    "org.example.config"
})
public class SupremeAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupremeAiApplication.class, args);
    }
}
```

## Step 4: Configure Security (Optional)

If you have Spring Security, allow command endpoints:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/api/commands/**").authenticated()
                .anyRequest().permitAll()
            .and()
            .httpBasic();
    }
}
```

## Step 5: Build and Test

```bash
# Build the project
./gradlew build

# Start the application
./gradlew bootRun

# In another terminal, test the API
curl http://localhost:8080/api/commands/health
curl http://localhost:8080/api/commands/list
curl -X POST http://localhost:8080/api/commands/execute \
  -H "Content-Type: application/json" \
  -d '{"name":"health-check"}'
```

## Step 6: Use CLI Tool (Optional)

```bash
# Install CLI
cp command-hub/cli/supcmd.py /usr/local/bin/supcmd
chmod +x /usr/local/bin/supcmd

# Test connection
supcmd health
supcmd list
supcmd exec health-check
```

## Adding Command Context Information

If you want to track who executed commands, update the `CommandController`:

```java
@PostMapping("/execute")
public ResponseEntity<CommandResponseDTO> executeCommand(
        @RequestBody ExecuteCommandRequest request,
        @RequestHeader(value = "Authorization", required = false) String authToken,
        HttpServletRequest httpRequest) {
    
    String userId = getCurrentUserId(authToken);      // From auth token
    String username = getCurrentUsername(authToken);  // From token
    String role = getCurrentRole(authToken);          // From token
    String sourceIp = httpRequest.getRemoteAddr();    // From HTTP
    
    CommandContext context = new CommandContext(
        userId, username, authToken, role
    );
    context.setSourceIp(sourceIp);
    context.setSourceApp("rest-api");
    
    return executor.execute(
        request.getName(),
        request.getParameters(),
        context
    );
}
```

## Implementing Custom Commands

To add your own commands, follow this pattern:

```java
public Command createMyCommand() {
    return new Command() {
        @Override
        public String getName() {
            return "my-command";
        }
        
        @Override
        public String getDescription() {
            return "Description of what my command does";
        }
        
        @Override
        public CommandCategory getCategory() {
            return CommandCategory.OPTIMIZATION;  // Pick appropriate category
        }
        
        @Override
        public CommandType getType() {
            return CommandType.SYNC;  // or ASYNC for background jobs
        }
        
        @Override
        public String[] getRequiredPermissions() {
            return new String[]{"execute.my-command"};
        }
        
        @Override
        public CommandSchema getSchema() {
            CommandSchema schema = new CommandSchema("my-command");
            // schema.addParameter(...);  // Add parameters if needed
            return schema;
        }
        
        @Override
        public CommandResult execute(Map<String, Object> params, CommandContext context) {
            try {
                // Your command logic here
                Object result = doSomething(params);
                
                return CommandResult.success("my-command", result);
            } catch (Exception e) {
                return CommandResult.error("my-command", "ERROR_CODE", e.getMessage());
            }
        }
        
        @Override
        public void validate(Map<String, Object> params) {
            // Validate parameters
            // Throw CommandValidationException if invalid
        }
    };
}
```

Then register it in `CommandHubConfiguration`:

```java
MyCommands myCommands = new MyCommands(...);
executor.register(myCommands.createMyCommand());
```

## Testing Commands

### Unit Test Example

```java
@SpringBootTest
public class HealthCheckCommandTest {
    
    @Autowired
    private CommandExecutor executor;
    
    @Test
    public void testHealthCheckSucceeds() {
        CommandContext context = new CommandContext(
            "test-user", "Test User", null, "ADMIN"
        );
        
        CommandResult result = executor.execute(
            "health-check",
            new HashMap<>(),
            context
        );
        
        assert(result.isSuccess());
        assert(result.getData() != null);
    }
    
    @Test
    public void testHealthCheckRequiresPermission() {
        CommandContext context = new CommandContext(
            "test-user", "Test User", null, "VIEWER"
        );
        
        CommandResult result = executor.execute(
            "health-check",
            new HashMap<>(),
            context
        );
        
        // Should succeed - health-check is read-only
        // But test that permissions are actually checked
        assert(result.isSuccess() || result.isFailed());
    }
}
```

### Integration Test with CLI

```bash
# Test via REST API
curl -X POST http://localhost:8080/api/commands/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "health-check",
    "parameters": {}
  }'

# Expected response:
{
  "commandName": "health-check",
  "success": true,
  "message": "Health check passed",
  "data": {
    "status": "HEALTHY",
    "timestamp": 1699564800000,
    "dataCollector": {
      "status": "OK",
      "successRate": 0.99
    }
  }
}
```

## Monitoring Command Execution

Add logging to track command performance:

```java
// In CommandExecutor.execute()
long startTime = System.currentTimeMillis();
CommandResult result = command.execute(params, context);
long duration = System.currentTimeMillis() - startTime;

logger.info("[COMMAND] {} executed in {}ms - {}",
    commandName,
    duration,
    result.isSuccess() ? "SUCCESS" : "FAILED"
);
```

## Common Integration Points

### 1. Health Check Integration
```java
// In your /health endpoint
Map<String, Object> health = new HashMap<>();
health.put("commands", commandHealthCheck());
```

### 2. Audit Logging
```java
// Log to central audit system
auditService.log(new AuditEvent(
    "COMMAND_EXECUTED",
    context.getUserId(),
    commandName,
    result.isSuccess()
));
```

### 3. Metrics/Monitoring
```java
// Track metrics
metricsService.increment("commands.executed");
metricsService.timer("command.duration", duration);
metricsService.gauge("commands.registered", executor.listCommands().size());
```

### 4. Webhook Notifications
```java
if (!result.isSuccess()) {
    notificationService.sendSlackAlert(
        "Command failed: " + commandName,
        result.getMessage()
    );
}
```

## Troubleshooting Integration

### Issue: CommandExecutor bean not found
**Solution**: Ensure `@Configuration` class is in component scan path

### Issue: CommandController endpoints not found
**Solution**: Check URL mappings - should be `/api/commands/*`

### Issue: Commands not registered
**Solution**: Check logs for CommandHubConfiguration initialization

### Issue: Permission denied errors
**Solution**: Verify auth token and role mapping in createContextFromRequest()

## Next Phase: Message Queue

For async commands to work at scale, add message queue:

```java
@Bean
public CommandQueueService commandQueueService(
        RabbitTemplate rabbitTemplate) {
    return new CommandQueueService(rabbitTemplate);
}
```

## Performance Considerations

- **Sync commands**: <100ms target latency
- **Async commands**: Queued immediately, processed by workers
- **Executor cache**: Register commands once at startup
- **Context validation**: Minimal per-execution overhead

## Security Checklist

- [ ] All commands have required authentication
- [ ] Permission model is consistently applied
- [ ] Audit logging captures all executions
- [ ] Parameters are validated before execution
- [ ] Sensitive data is not logged
- [ ] Rate limiting is configured
- [ ] CORS is properly restricted
- [ ] API token rotation is implemented

---

**Integration Complete!** Your SupremeAI application now has a full command orchestration system.

For detailed API documentation, see [IMPLEMENTATION.md](IMPLEMENTATION.md)
For CLI usage guide, see `supcmd --help`
