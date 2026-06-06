# Reactive .block() Migration Plan

## Problem Statement

The codebase contains **122+ `.block()` calls** across controllers, services, and orchestration layers. This anti-pattern can stall the reactive event-loop and increase latency.

## Priority Targets (High-Impact Areas)

### 1. Controllers (Block per-request)

- `ProvidersController.java` (~20 calls) - Convert to fully asynchronous responses
- `AdminKnowledgeController.java` (~15 calls) - Same approach

### 2. AI Provider Factory (`AIProviderFactory.java`)

- Multiple `.block()` calls for Firestore queries
- Solution: Cache provider metadata at startup; use `Mono.defer()` for lazy resolution

### 3. Orchestrators

- `AIFallbackOrchestrator.java` - `.block(Duration.ofSeconds(60))`
- `AdaptiveAgentOrchestrator.java` - bare `.block()` with comment
- `MultiAIVotingService.java` - `.next().block(Duration.ofSeconds(2))`

## Migration Strategy

### Phase 1: Non-blocking by Default

1. Identify blocking call sites via `grep -r "\.block\(" src/`
2. For each call, determine if the calling method is:
   - Controller endpoint → return `Mono<T>`/`Flux<T>` directly
   - Service method → accept `Mono<T>` parameter or return `Mono<T>`
   - Scheduled task → use `Schedulers.boundedElastic()` for blocking workloads

### Phase 2: Introduce Blocking Batches

Where blocking is unavoidable (e.g., legacy integration):

```java
@Configuration
public class BlockingConfig {
    @Bean
    public Scheduler blockingScheduler() {
        return Schedulers.newBoundedElastic(10, 100, "blocking-pool");
    }
}
```

### Phase 3: Eliminate Static Initialization Blocks

- `TelegramStorageService.java` static fields with `System.getenv()` → lazy resolution

## Quick Fixes

### For Controller Endpoints

```java
// Before
@GetMapping("/health")
public ResponseEntity<String> health() {
    String status = service.checkHealth().block();
    return ResponseEntity.ok(status);
}

// After
@GetMapping("/health")
public Mono<ResponseEntity<String>> health() {
    return service.checkHealth()
        .map(ResponseEntity::ok)
        .onErrorReturn(ResponseEntity.status(503).body("DOWN"));
}
```

### For Service Methods

```java
// Before
public String getApiKey() {
    return provider.getApiKey().block();
}

// After
public Mono<String> getApiKey() {
    return provider.getApiKey();
}
```

## Testing Strategy

- Run BlockHound tests: `BlockHoundCustomConfig.java` already in place
- Add `@Test` cases with `StepVerifier` to verify non-blocking behavior
- Use `reactor.core.scheduler.Schedulers.boundedElastic()` for tests that need blocking

## Timeline

- Phase 1: 2-3 days (identify + prioritize)
- Phase 2: 1-2 weeks (refactor high-impact areas)
- Phase 3: Ongoing (incremental improvements)
