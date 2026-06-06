# MERMD - Service Layer

## Overview

The Service layer contains all business logic for SupremeAI features.

## How It Works

### Architecture Flow

```
Controller → Service → Repository/External API → Response
```

## Key Services by Domain

### Simulator Services

| Service                      | Purpose                   |
| ---------------------------- | ------------------------- |
| `SimulatorService`           | Core simulator operations |
| `SimulatorDeploymentService` | App deployment            |
| `SimulatorSessionService`    | Session management        |
| `SimulatorQuotaService`      | Quota enforcement         |
| `DeviceEmulationService`     | Device profiles           |
| `SimulatorScreenshotService` | Screenshot capture        |

### AI/AI Provider Services

| Service                           | Purpose              |
| --------------------------------- | -------------------- |
| `AIProviderService`               | Provider management  |
| `NeuralChatService`               | Chat processing      |
| `ChatProcessingService`           | Message handling     |
| `MultiAIConsensusService`         | Response aggregation |
| `EnhancedMultiAIConsensusService` | Improved consensus   |
| `ProviderModelRegistry`           | Model catalog        |
| `ProviderMetadataService`         | Provider info        |
| `ParallelProviderService`         | Parallel requests    |

### Learning Services

| Service                      | Purpose                |
| ---------------------------- | ---------------------- |
| `EnhancedLearningService`    | Learning orchestration |
| `KnowledgeBaseService`       | Knowledge access       |
| `KnowledgeSeederService`     | Initial seeding        |
| `UserCodeLearningService`    | User code learning     |
| `LearningQuotaService`       | Learning quotas        |
| `LearningActivityLogService` | Activity logging       |

### User Management

| Service                        | Purpose            |
| ------------------------------ | ------------------ |
| `UserAccountService`           | Account operations |
| `AuthenticationService`        | Auth logic         |
| `UserApiKeyService`            | API key management |
| `UserBehaviorProfilingService` | User profiling     |

### Code Services

| Service                  | Purpose            |
| ------------------------ | ------------------ |
| `CodeGenerationService`  | Code generation    |
| `FullStackCodeGenerator` | Multi-platform gen |
| `MultiPlatformGenerator` | Platform-specific  |
| `CodeValidationService`  | Validation         |
| `Codeflow` services      | Analysis           |

### Admin Services

| Service                        | Purpose         |
| ------------------------------ | --------------- |
| `AdminDashboardFacadeService`  | Admin dashboard |
| `ProviderAdminService`         | Provider admin  |
| `SystemConfigSeeder`           | Config seeding  |
| `WorkflowOrchestrationService` | Workflow mgmt   |

## Common Patterns

### Reactive Programming

```java
@Service
public class ExampleService {
    public Mono<ResponseEntity<Object>> handleRequest(...) {
        return repository.findById(id)
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.notFound().build());
    }
}
```

### Transaction Management

- `@Transactional` for write operations
- Reactive transactions with `TransactionalOperator`

### Caching

- `@Cacheable` for read-heavy operations
- `ResponseCacheService` for AI responses
