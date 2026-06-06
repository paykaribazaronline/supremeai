# MERMD - Controller Layer

## Overview

The Controller layer exposes REST APIs for all SupremeAI features.

## How It Works

### Architecture Flow

```
HTTP Request → Controller → Service → Response
```

## Controller Packages

### Main Controllers (`controller/`)

| Controller                 | Endpoints           | Purpose              |
| -------------------------- | ------------------- | -------------------- |
| `SimulatorController`      | `/api/simulator/**` | Simulator management |
| `ChatController`           | `/api/chat/**`      | AI chat              |
| `AuthenticationController` | `/api/auth/**`      | Authentication       |
| `APIKeyController`         | `/api/apikeys/**`   | API key management   |
| `KnowledgeBaseController`  | `/api/knowledge/**` | Knowledge base       |
| `HealthController`         | `/api/health/**`    | Health checks        |
| `AdminDashboardController` | `/api/admin/**`     | Admin operations     |
| `AIAgentsController`       | `/api/v1/agents/**` | AI agent management  |
| `WorkflowController`       | `/api/workflows/**` | Workflow management  |

### Subpackage Controllers

#### `controller/analysis/`

- `AnalysisController` - Code/system analysis

#### `controller/browser/`

- `BrowserController` - Browser automation

## Common Patterns

### Reactive REST Controller

```java
@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<UserSimulatorProfile>> getProfile(Authentication auth) {
        String userId = auth.getName();
        return simulatorService.getProfile(userId)
            .map(ResponseEntity::ok);
    }
}
```

### Security Annotations

- `@PreAuthorize("isAuthenticated()")` - Requires authentication
- `@PreAuthorize("hasRole('ADMIN')")` - Requires admin role

### Response Types

- `Mono<ResponseEntity<T>>` for reactive endpoints
- `ResponseEntity<?>` for standard responses
- `Map<String, Object>` for flexible responses

## Integration Points

- `SecurityConfig.java` - Endpoint security rules
- `AuthenticationFilter` - Token extraction
- `JwtAuthFilter` - JWT validation
