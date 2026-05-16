# Fast Analysis System - Quick Reference

## Mermaid Component Diagram

```mermaid
graph TD
    subgraph "Client"
        DASH[Dashboard React]
    end
    
    subgraph "API Layer"
        CTRL[AnalysisController]
        STREAM[StreamingController<br/>SSE]
        WS[WebSocketHandler]
    end
    
    subgraph "Orchestration"
        ORCH[ProjectAnalysisService<br/>Orchestrator]
        COORD[AgentCoordinator]
        CONTEXT[ContextSelector<br/>(RAG)]
        INCR[IncrementalService]
        AGG[ResultAggregator]
    end
    
    subgraph "Agents<br/>(Parallel)"
        SEC[Security Scanner]
        ARCH[Architecture Analyzer]
        QUAL[Code Quality]
        DEP[Dependency Checker]
        PERF[Performance Profiler]
        DOCS[Doc Gap Finder]
    end
    
    subgraph "Data Layer"
        FS[Firestore]
        CACHE[Redis Cache]
        EMB[code_embeddings<br/>(vector)]
        HIST[analysis_history]
    end
    
    DASH --> CTRL & STREAM & WS
    CTRL --> ORCH
    STREAM --> SSE stream --> DASH
    WS --> WS stream --> DASH
    ORCH --> COORD & CONTEXT & INCR
    COORD --> SEC & ARCH & QUAL & DEP & PERF & DOCS
    CONTEXT --> EMB
    INCR --> FS & GIT
    SEC & ARCH & QUAL & DEP & PERF & DOCS --> AGG
    AGG --> FS & CACHE & WS
    EMB --> FS
    HIST --> FS
```

---

## Firestore Collection Schema

### analysis_jobs (master tracking)

```javascript
{
  id: "job_123",
  projectId: "proj_456",
  userId: "auth_uid_789",
  status: "RUNNING", // QUEUED|RUNNING|COMPLETED|FAILED
  branch: "main",
  commit: "abc123",
  baseCommit: "previous_sha", // null for full analysis
  triggeredBy: "MANUAL", // MANUAL|WEBHOOK|SCHEDULED
  diffMode: false,
  totalFiles: 1247,
  analyzedFiles: 0,
  agentCounts: { security: 400, architecture: 1247, quality: 1247, dependencies: 1, performance: 1247, docs: 1247 },
  findingsCount: { critical: 2, high: 5, medium: 12, low: 23, info: 45 },
  startedAt: Timestamp,
  completedAt: Timestamp|null,
  durationMs: null,
  cacheHit: false,
  errorMessage: null,
  createdAt: Timestamp
}
// Indexes: userId+status+createdAt, projectId+createdAt
```

### analysis_cache (baseline storage)

```javascript
{
  id: "proj_456_abc123", // Composite PK
  projectId: "proj_456",
  commitSHA: "abc123",
  branch: "main",
  fileHashes: {
    "src/main/java/Foo.java": "sha256:xyz",
    "README.md": "sha256:abc"
  },
  fileCount: 1247,
  totalSizeBytes: 2457600,
  analysisBaseline: "gzipped JSON findings per file",
  embeddingVersion: 1,
  createdAt: Timestamp,
  expiresAt: Timestamp + 30d
}
```

### code_embeddings (vector search)

```javascript
// Option B: Firestore Native (v2-compatible)
{
  id: "embed_uuid",
  projectId: "proj_456",
  filePath: "src/main/java/UserService.java",
  chunkIndex: 0, // 0-indexed chunks for large files
  content: "public class UserService { ... }",
  embedding: [0.012, -0.045, ..., 0.089], // 1536-dim array
  language: "java",
  embeddingModel: "text-embedding-ada-002",
  createdAt: Timestamp
}
// Query: nearest neighbor search by cosine similarity
// For production: upgrade to Firestore Data Connect with pgvector
```

### agent_configs (rule engine)

```javascript
{
  id: "security-scanner",
  name: "security-scanner",
  displayName: "Security Scanner",
  description: "OWASP Top 10, secrets, injection patterns",
  enabled: true,
  priority: 1, // Run first (critical issues)
  timeoutMs: 15000,
  tokenBudget: 32000,
  rules: [
    {
      id: "SQL_INJECTION",
      pattern: "(?i)(createStatement|executeQuery|Statement\\.execute)\\(.+\\+.*",
      severity: "CRITICAL",
      message: "SQL injection risk: string concatenation",
      fixSuggestion: "Use PreparedStatement with parameterized query",
      fixDescription: "Replace string concatenation with PreparedStatement setParameter()",
      category: "A03-Injection",
      cweId: "CWE-89",
      tags: ["sql", "injection", "security"],
      enabled: true
    },
    {
      id: "HARDCODED_SECRET",
      pattern: "(?i)(password|secret|api_key|token)\\s*=\\s*[\"'][^\"']{8,}[\"']",
      severity: "CRITICAL",
      message: "Hardcoded secret in source code",
      fixSuggestion: "Move secret to environment variable",
      tags: ["secrets", "credentials"],
      enabled: true
    }
  ],
  semanticQueries: [
    "SQL database queries",
    "password handling",
    "API authentication",
    "encryption",
    "input validation"
  ],
  excludedPaths: [
    "**/test/**",
    "**/*Test.java",
    "**/node_modules/**"
  ],
  dependencies: [], // No dependencies
  version: 3,
  updatedAt: Timestamp
}
```

---

## Controller Endpoints Overview

```java
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {
    
    private final ProjectAnalysisService analysisService;
    
    // Start analysis (async job)
    @PostMapping("/start")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> startAnalysis(
        @RequestBody @Valid AnalysisRequest request,
        Authentication auth
    ) {
        request.setUserId(auth.getName());
        return analysisService.startAnalysis(request)
            .map(job -> ResponseEntity.accepted().body(ApiResponse.ok(Map.of(
                "jobId", job.getId(),
                "status", job.getStatus(),
                "streamUrl", "/api/analysis/" + job.getId() + "/stream",
                "estimatedTimeSeconds", estimateDuration(job)
            ))));
    }
    
    // Get job status/result
    @GetMapping("/{jobId}")
    public Mono<ResponseEntity<ApiResponse<AnalysisResult>>> getResult(
        @PathVariable String jobId,
        Authentication auth
    ) {
        return analysisService.getResult(jobId, auth.getName())
            .map(result -> ResponseEntity.ok(ApiResponse.ok(result)))
            .defaultIfEmpty(ResponseEntity.status(404)
                .body(ApiResponse.error("Job not found")));
    }
    
    // Cancel running job
    @DeleteMapping("/{jobId}")
    public Mono<ResponseEntity<ApiResponse<String>>> cancel(
        @PathVariable String jobId,
        Authentication auth
    ) {
        return analysisService.cancelAnalysis(jobId, auth.getName())
            .thenReturn(ResponseEntity.ok(ApiResponse.ok("Cancelled")));
    }
    
    // Incremental analysis only
    @PostMapping("/incremental")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> incrementalAnalysis(
        @RequestBody IncrementalAnalysisRequest request,
        Authentication auth
    ) {
        // Validates git diff exists
        // Runs only on changed files + impacted
    }
}

@Controller
@RequestMapping("/api/analysis")
public class AnalysisStreamingController {
    
    private final ProjectAnalysisService analysisService;
    
    // SSE endpoint
    @GetMapping(value = "/{jobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(
        @PathVariable String jobId,
        Authentication auth
    ) {
        return analysisService.streamEvents(jobId)
            .map(event -> ServerSentEvent.<String>builder()
                .event(event.type())
                .id(event.id())
                .data(toJson(event))
                .build());
    }
}
```

---

## Agent Interface & Sample Implementation

### Interface

```java
public interface AnalysisAgent {
    String getAgentName();                    // e.g., "security-scanner"
    String getDisplayName();                  // "Security Scanner"
    int getPriority();                        // 1-10
    List<String> getDependencies();           // Agent dependencies
    long getTimeoutMs();                      // 10000
    boolean isApplicable(String filePath, String language);
    
    /**
     * Core analysis method - emits findings as they're discovered.
     */
    Flux<AnalysisFinding> analyze(
        AnalysisContext context,
        AgentConfig config,
        Sinks.Many<AgentProgressUpdate> progressSink
    );
}
```

### SecurityScannerAgent Skeleton

```java
@Component
@RequiredArgsConstructor
public class SecurityScannerAgent implements AnalysisAgent {
    
    private final ConfigService configService;
    private final PatternRegistry patternRegistry;
    
    @Override
    public String getAgentName() { return "security-scanner"; }
    
    @Override
    public String getDisplayName() { return "Security Scanner"; }
    
    @Override
    public int getPriority() { return 1; } // Highest: critical issues first
    
    @Override
    public Flux<AnalysisFinding> analyze(
        AnalysisContext ctx,
        AgentConfig config,
        Sinks.Many<AgentProgressUpdate> progress
    ) {
        progress.tryEmitNext(AgentProgressUpdate.started(getAgentName(), ctx));
        
        return Flux.fromIterable(ctx.files())
            .filter(file -> isApplicable(file.path(), file.language()))
            .flatMap(file -> analyzeFile(file, config))
            .doOnNext(f -> progress.tryEmitNext(
                AgentProgressUpdate.findingFound(getAgentName(), f)
            ))
            .doOnComplete(() -> progress.tryEmitNext(
                AgentProgressUpdate.completed(getAgentName())
            ));
    }
    
    private Flux<AnalysisFinding> analyzeFile(
        AnalysisContext.CodeFile file,
        AgentConfig config
    ) {
        String content = file.content();
        return Flux.fromIterable(config.rules())
            .filter(Rule::enabled)
            .flatMap(rule -> {
                Matcher m = rule.pattern().matcher(content);
                List<AnalysisFinding> findings = new ArrayList<>();
                while (m.find()) {
                    int line = countLines(content, m.start());
                    findings.add(AnalysisFinding.builder()
                        .id(UUID.randomUUID().toString())
                        .agentName(getAgentName())
                        .severity(rule.severity())
                        .filePath(file.path())
                        .line(line)
                        .message(rule.message())
                        .codeSnippet(extractSnippet(content, line))
                        .fixSuggestion(rule.fixSuggestion())
                        .confidence(rule.confidence())
                        .category(rule.category())
                        .tags(rule.tags())
                        .build());
                }
                return Flux.fromIterable(findings);
            });
    }
    
    @Override
    public boolean isApplicable(String path, String language) {
        String ext = FileUtils.getExtension(path);
        return switch (ext) {
            case "java", "py", "js", "ts", "go", "rb", "php" -> true;
            default -> false;
        };
    }
}
```

---

## Context Selection Algorithm (RAG)

```java
@Service
public class ContextSelectionService {
    
    /**
     * Build agent-specific file lists using:
     * 1. Always-include: changed files (if incremental)
     * 2. Semantic search: query embeddings for agent's domain
     * 3. Dependency impact: files that import changed files
     * 4. Token budget limit: truncate to budget per agent
     */
    public Mono<AgentFileBundle> selectForAgent(
        String agentName,
        AnalysisContext baseContext,
        AgentConfig config
    ) {
        // Step 1: Get changed files (if incremental)
        Set<String> changedFiles = baseContext.changedFiles().keySet();
        
        // Step 2: Vector search via semantic queries
        return vectorSearch(config.semanticQueries())
            .map(results -> {
                Set<String> candidateFiles = new HashSet<>(changedFiles);
                for (CodeChunk chunk : results) {
                    candidateFiles.add(chunk.filePath());
                }
                return candidateFiles;
            })
            .flatMap(candidateFiles -> {
                // Step 3: Dependency impact analysis
                return impactAnalysis.analyze(candidateFiles);
            })
            .map(allFiles -> {
                // Step 4: Sort by relevance score & truncate by token budget
                List<FileScore> scored = scoreFiles(allFiles, agentName, config);
                int tokensUsed = 0;
                List<AnalysisContext.CodeFile> selected = new ArrayList<>();
                
                for (FileScore fs : scored) {
                    int fileTokens = countTokens(fs.file().content());
                    if (tokensUsed + fileTokens > config.tokenBudget()) {
                        break; // Budget exceeded
                    }
                    selected.add(fs.file());
                    tokensUsed += fileTokens;
                }
                return new AgentFileBundle(agentName, selected);
            });
    }
}
```

---

## Incremental Analysis Flow

```java
// IncrementalAnalysisService
public Mono<IncrementalAnalysisResult> analyzeIncremental(
    String projectId,
    String baseCommit,
    String headCommit
) {
    return gitService.diff(projectId, baseCommit, headCommit)
        .flatMap(diff -> {
            // 1. Changed files list
            List<String> changed = diff.getChangedFiles();
            
            // 2. Impact analysis - follow imports/dependencies
            return dependencyService.computeImpactSet(changed)
                .map(impactedFiles -> 
                    new IncrementalAnalysisResult(changed, impactedFiles)
                );
        })
        .flatMap(result -> {
            // 3. Load cached baseline for unchanged files
            return cacheService.loadBaseline(projectId, baseCommit)
                .map(baseline -> {
                    result.setBaseline(baseline);
                    return result;
                });
        })
        .map(this::filterContext); // Return only files needing re-analysis
}
```

---

## Result Aggregation Algorithm

```java
public AnalysisResult aggregate(
    Flux<AnalysisFinding> findings,
    AnalysisJob job
) {
    // 1. Deduplicate (same file+line+message across agents)
    Map<FindingKey, AnalysisFinding> deduped = new LinkedHashMap<>();
    
    // 2. Priority sort (severity desc, file asc, line asc)
    List<AnalysisFinding> sorted = findings
        .sorted(Comparator
            .comparing((AnalysisFinding f) -> f.severity().ordinal()).reversed()
            .thenComparing(f -> f.filePath())
            .thenComparingInt(f -> f.line())
        )
        .toList();
    
    // 3. Group by agent for stats
    Map<String, AgentSummary> byAgent = sorted.stream()
        .collect(Collectors.groupingBy(
            AnalysisFinding::agentName,
            Collectors.collectingAndThen(
                Collectors.toList(),
                list -> new AgentSummary(
                    list.get(0).agentName(),
                    list.size(),
                    list.stream()
                        .mapToLong(f -> f.durationMs().orElse(0L))
                        .max().orElse(0L)
                )
            )
        ));
    
    // 4. Generate recommendations
    List<String> recommendations = RecommendationEngine.generate(sorted);
    
    // 5. Compute overall confidence (weighted by agent reliability)
    double confidence = calculateConfidence(sorted, byAgent);
    
    return new AnalysisResult(
        job.getId(),
        sorted,
        byAgent.values().stream().toList(),
        summarizeBySeverity(sorted),
        recommendations,
        confidence,
        System.currentTimeMillis() - job.getStartedAt().toEpochMilli()
    );
}
```

---

## Frontend: Real-Time Hook Pattern

```typescript
// hooks/useSSEAnalysis.ts
import { useEffect, useRef, useState } from 'react';

export function useSSEAnalysis(jobId: string) {
  const [status, setStatus] = useState<'idle'|'running'|'completed'|'error'>('idle');
  const [findings, setFindings] = useState<Finding[]>([]);
  const [summary, setSummary] = useState<Summary>({critical:0,high:0,medium:0,low:0});
  const eventSourceRef = useRef<EventSource | null>(null);
  
  useEffect(() => {
    if (!jobId) return;
    
    const es = new EventSource(`/api/analysis/${jobId}/stream`);
    eventSourceRef.current = es;
    
    es.onmessage = (e) => {
      const data = JSON.parse(e.data);
      handleEvent(data);
    };
    
    es.onerror = () => {
      setStatus('error');
      es.close();
    };
    
    return () => es.close();
  }, [jobId]);
  
  const handleEvent = (data: any) => {
    switch (data.event) {
      case 'job_started':
        setStatus('running');
        break;
      case 'finding':
        setFindings(prev => [data.data, ...prev]);
        setSummary(prev => ({
          ...prev,
          [data.data.severity.toLowerCase()]: prev[data.data.severity as keyof Summary] + 1
        }));
        break;
      case 'agent_completed':
        // update per-agent status
        break;
      case 'completed':
        setStatus('completed');
        setSummary(data.data.summary);
        break;
    }
  };
  
  return { status, findings, summary };
}
```

---

## Configuration Properties

```yaml
# application-analysis.yml
supremeai:
  analysis:
    # Parallel agent configuration
    max-parallel-agents: 6
    default-agent-timeout: 10000  # 10s
    
    # Token budget per agent type (context window)
    token-budgets:
      security: 32000
      architecture: 64000
      quality: 32000
      dependencies: 16000
      performance: 32000
      docs: 16000
    
    # Caching
    cache:
      baseline-ttl: 2592000  # 30d in seconds
      embedding-ttl: 604800  # 7d
      result-ttl: 86400     # 1d
      redis-prefix: "anl:"
    
    # RAG settings
    rag:
      embedding-model: "text-embedding-ada-002"
      vector-dim: 1536
      top-k-results: 50
      similarity-threshold: 0.75  # cosine similarity
      include-dependencies: true
    
    # Incremental settings
    incremental:
      compute-impact: true  # follow imports
      max-impact-depth: 2   # transitive dependency depth
      baseline-auto-cleanup: true
      baseline-retention-days: 30
    
    # Performance targets
    targets:
      small-project: 100    # files
      small-time: 10        # seconds
      medium-project: 1000
      medium-time: 30
      large-project: 10000
      large-time: 120
      first-finding-ms: 5000
    
    # Security
    max-files-per-job: 50000
    max-job-duration: 600  # seconds
    rate-limit-per-user: 10  # jobs/hour
```

---

## Gradle Dependencies to Add

```gradle
// build.gradle
dependencies {
    // Reactive
    implementation 'io.projectreactor:reactor-core:3.6.5'
    implementation 'io.projectreactor:reactor-test:3.6.5'
    
    // Firestore (already present)
    implementation 'com.google.cloud:spring-cloud-gcp-starter-firestore:4.12.0'
    
    // Code parsing (optional agents)
    implementation 'com.github.javaparser:javaparser-core:3.26.2'  // Java
    implementation 'org.eclipse.jdt:org.eclipse.jdt.core:3.35.0'   // Java AST
    implementation 'com.pinterest:ktlint:1.2.1' // Kotlin optional
    // Note: TS/JS/Python parsing done client-side or with Node.js microservice
    
    // Graph (for dependency analysis)
    implementation 'org.jgrapht:jgrapht-core:1.5.2'
    
    // String similarity
    implementation 'org.apache.commons:commons-text:1.12.0'
    
    // Embeddings (optional for local)
    implementation 'com.google.cloud:google-cloud-aiplatform:2.28.0'  // Vertex AI
    
    // Testing
    testImplementation 'org.springframework:spring-test'
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.mockito:mockito-core'
    testImplementation 'io.projectreactor:reactor-test'
}
```

---

## Quick Start: Run Locally

```bash
# 1. Start Firebase emulators (Firestore + Auth)
firebase emulators:start --only firestore,auth

# 2. Start Spring Boot backend
./gradlew bootRun --args="--spring.profiles.active=local,analysis"

# 3. Start React dashboard
cd dashboard && npm run dev

# 4. Create test project (upload zip or git clone)
# Dashboard: http://localhost:3000/admin/analysis
# API: http://localhost:8080/api/analysis/start
```

---

## Monitoring Dashboards (Grafana/Prometheus)

| Dashboard | Key Metrics |
|-----------|-------------|
| Analysis Overview | Jobs/sec, avg duration, success rate |
| Agent Performance | Per-agent duration (p50, p95, p99) |
| Findings Distribution | Severity breakdown, top categories |
| Cache Effectiveness | Hit rate, eviction rate |
| Resource Utilization | CPU, memory per service instance |
| Error Tracking | Agent failures, timeouts, exceptions |

---

## Example: End-to-End Flow

```bash
# User clicks "Analyze" on project "supremeai-backend"
→ POST /api/analysis/start:
   {projectId: "proj_backend", branch: "main", agents: ["security","quality"]}

← 202 Accepted:
   {jobId: "job_a1b2c3", status: "QUEUED", streamUrl: "/api/analysis/job_a1b2c3/stream"}

# Backend: creates analysis_jobs document, dispatches worker

→ WS: {"event":"job_started","data":{"jobId":"job_a1b2c3","totalFiles":1247}}

→ SSE:
   event: agent_started
   data: {"agent":"security","timestamp":"..."}

   event: agent_progress
   data: {"agent":"security","filesProcessed":200,"totalFiles":400,"pct":50}

   event: finding
   data: {"id":"f1","severity":"CRITICAL","file":"UserController.java","line":42,...}

   event: agent_completed
   data: {"agent":"security","durationMs":8200,"findingsCount":8}

   event: agent_started
   data: {"agent":"quality"}

   event: completed
   data: {"summary":{"critical":2,"high":5},"confidence":0.94}

← GET /api/analysis/job_a1b2c3:
   Final JSON with all findings, recommendations, metrics

# Dashboard: displays live updates + final report
```

---

## Summary Table: Agent Responsibilities

| Agent | Priority | Timeout | Files | CWE Top | Key Patterns | AI Enhancement |
|-------|----------|---------|-------|---------|--------------|---------------|
| Security | 1 | 15s | All | 20+ | OWASP Top 10, secrets | Semantic vuln search |
| Architecture | 2 | 20s | All | N/A | God class, cycles, coupling | Design smell detection |
| Quality | 3 | 15s | All | N/A | Complexity, duplication | Code style norms |
| Dependencies | 4 | 10s | Manifest only | CVE DB | Outdated, vulnerable | License compliance |
| Performance | 5 | 20s | All | N/A | N+1, memory leaks, inefficiency | Hotspot analysis |
| Docs | 6 | 10s | All | N/A | Missing Javadoc/README | Coverage gaps |

**Total parallel runtime:** ~25 seconds (all agents complete)

---

*This design is production-ready for Cloud Run, stateless worker design, fully parallel, and uses existing SupremeAI stack.*
