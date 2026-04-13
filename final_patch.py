import re

with open("src/main/java/org/example/learning/StrategicKnowledgeLearningInitializer.java", "r", encoding="utf-8") as f:
    content = f.read()

new_seed_method = """

    private void seedAdvancedResearchFindings() {
        systemLearningService.recordTechnique(
            "APP_CREATION",
            "Contract-First API Scaffolding",
            "Defining OpenAPI/GraphQL contracts before implementation ensures deterministic boundaries, allowing parallel development of frontend and backend without context drift.",
            Arrays.asList(
                "Define API specification in structured format",
                "Generate server stubs and client SDKs using schema",
                "Implement routing layer mapping to interface stubs",
                "Write interface-level integration tests"
            ),
            0.95,
            Map.of("evidence_type", "industry_practice", "risk_if_ignored", "Agents create tightly coupled implementations that diverge from client expectations, causing late-stage integration failures.")
        );

        systemLearningService.recordTechnique(
            "APP_CREATION",
            "Isolated State Management Scaffolding",
            "Decoupling state mutation from view/controller logic allows the system to operate deterministically and makes AI-driven refactoring much safer.",
            Arrays.asList(
                "Create dedicated state container/store directory",
                "Define pure functions for state mutations (reducers)",
                "Implement selector functions for read access",
                "Wire controllers to observe selectors instead of mutating directly"
            ),
            0.90,
            Map.of("evidence_type", "architecture_pattern", "risk_if_ignored", "Spaghetti state management leading to race conditions that automated debuggers cannot easily trace.")
        );

        systemLearningService.recordTechnique(
            "APP_CREATION",
            "Automated Schema Migration Setup",
            "Treating database schemas as immutable versioned code ensures reliable local fallbacks and safe rollbacks during automated deployments.",
            Arrays.asList(
                "Initialize versioned migration directory",
                "Create V1 schema with baseline tables",
                "Set up migration runner in application startup script",
                "Generate baseline rollback script"
            ),
            0.98,
            Map.of("evidence_type", "industry_practice", "risk_if_ignored", "Database schema drifts from codebase, breaking CI/CD pipelines and preventing reliable automated recovery.")
        );

        systemLearningService.recordTechnique(
            "ERROR_SOLVING",
            "Bisection Dependency Tracing",
            "When a build fails due to conflicting dependencies, binary search isolation is the most deterministic method to find the culprit without external AI.",
            Arrays.asList(
                "Extract full dependency tree",
                "Identify failing module",
                "Remove half of non-essential dependencies and compile",
                "If fails, culprit is in remaining half; repeat until isolated"
            ),
            0.85,
            Map.of("evidence_type", "benchmark", "risk_if_ignored", "Endless loops of guessing version numbers, leading to dependency hell.")
        );

        systemLearningService.recordTechnique(
            "ERROR_SOLVING",
            "OOM Memory Dump Analysis",
            "Out of Memory errors require structural heap analysis to identify leak origins rather than randomly tweaking JVM/Node flags.",
            Arrays.asList(
                "Configure local runtime to generate heap dump on OOM",
                "Trigger memory limit in sandboxed environment",
                "Parse heap dump for top 3 retained memory object classes",
                "Locate instantiation points of these classes in codebase"
            ),
            0.90,
            Map.of("evidence_type", "postmortem", "risk_if_ignored", "System crashes in production; arbitrary memory limit increases just delay the crash.")
        );

        systemLearningService.recordTechnique(
            "ERROR_SOLVING",
            "Deadlock Thread Interrogation",
            "When the system hangs, dumping and analyzing thread execution states deterministically identifies circular lock dependencies.",
            Arrays.asList(
                "Detect application timeout/hang",
                "Execute thread dump command (e.g., jstack/kill -3)",
                "Scan output for \\"waiting to lock\\" alongside \\"locked\\"",
                "Identify exact line numbers participating in circular wait"
            ),
            0.95,
            Map.of("evidence_type", "postmortem", "risk_if_ignored", "System freezes completely; automated restarts mask the underlying concurrency flaw.")
        );

        systemLearningService.recordTechnique(
            "ERROR_SOLVING",
            "Network Timeout Root-Cause Isolation",
            "Network timeouts can originate from DNS, firewall, or application limits; systematic probing isolates the exact layer.",
            Arrays.asList(
                "Ping target IP directly to test basic routing",
                "Execute DNS resolution test",
                "Perform TCP handshake timing test",
                "Check application-level connection pool exhaustion metrics"
            ),
            0.88,
            Map.of("evidence_type", "industry_practice", "risk_if_ignored", "Wasting time rewriting application network logic when the issue is infrastructure-level packet drop.")
        );

        systemLearningService.recordTechnique(
            "ARCHITECTURE",
            "Graceful Core Fallback",
            "Ensures the system remains operational (solo-first) when external booster AI APIs are unreachable or degraded.",
            Arrays.asList(
                "Wrap external AI calls in a unified interface",
                "Implement a rule-based or local-heuristic fallback class for the interface",
                "Catch external timeout/5xx errors",
                "Route execution to local fallback logic transparently"
            ),
            1.0,
            Map.of("evidence_type", "architecture_pattern", "risk_if_ignored", "System experiences hard outages whenever third-party services degrade, violating the solo-first capability rule.")
        );

        systemLearningService.recordTechnique(
            "ARCHITECTURE",
            "Circuit Breaker Implementation",
            "Prevents cascading failures by halting requests to a failing downstream service and allowing it time to recover.",
            Arrays.asList(
                "Wrap outbound RPC calls with circuit breaker state machine",
                "Configure error threshold and timeout duration",
                "On threshold breach, transition to OPEN state and fail fast",
                "After timeout, transition to HALF-OPEN to test recovery"
            ),
            0.95,
            Map.of("evidence_type", "architecture_pattern", "risk_if_ignored", "System exhaustion due to thread pools filling up waiting for dead downstream services.")
        );

        systemLearningService.recordTechnique(
            "ARCHITECTURE",
            "Idempotent Retry Wrapper",
            "Transient network errors are common; safe retries require operations to be idempotent so repeated calls don't duplicate state.",
            Arrays.asList(
                "Identify external mutation call",
                "Inject unique Idempotency-Key header or payload field",
                "Wrap call in exponential backoff retry loop",
                "Only retry on 429, 503, or network timeout"
            ),
            0.90,
            Map.of("evidence_type", "industry_practice", "risk_if_ignored", "Duplicate transactions, corrupted data, or unintended state changes during network hiccups.")
        );

        systemLearningService.recordTechnique(
            "ARCHITECTURE",
            "Event-Driven State Replication",
            "Decoupling services via asynchronous events prevents synchronous call chains from becoming monolithic points of failure.",
            Arrays.asList(
                "Identify bounded context state changes",
                "Publish domain event to message broker on state change",
                "Implement independent listeners in target services",
                "Handle out-of-order events using timestamp or versioning"
            ),
            0.85,
            Map.of("evidence_type", "architecture_pattern", "risk_if_ignored", "Tight coupling creates a distributed monolith where one service failure halts the entire system.")
        );

        systemLearningService.recordTechnique(
            "SECURITY",
            "Secrets Pre-Flight Validation",
            "Security cannot be weakened; scanning for hardcoded secrets before execution prevents credentials from entering version control or logs.",
            Arrays.asList(
                "Integrate entropy and regex-based secret scanner",
                "Hook scanner into pre-commit and pre-execution lifecycle",
                "Scan all generated code and configurations",
                "Block action and trigger WAIT mode if secret is detected"
            ),
            0.98,
            Map.of("evidence_type", "industry_practice", "risk_if_ignored", "Automated system inadvertently hardcodes and publishes live API keys or database credentials.")
        );

        systemLearningService.recordTechnique(
            "SECURITY",
            "Granular IAM Role Provisioning",
            "Cloud-first architecture requires strict least-privilege access; auto-generated components must not run as root/admin.",
            Arrays.asList(
                "Analyze infrastructure requirements for new component",
                "Generate specific IAM policy granting only required actions",
                "Attach policy to isolated service account",
                "Deploy component utilizing the isolated service account"
            ),
            0.95,
            Map.of("evidence_type", "industry_practice", "risk_if_ignored", "A compromised component or AI hallucination gains destructive access to the entire cloud environment.")
        );

        systemLearningService.recordTechnique(
            "SECURITY",
            "API Input Strict Sanitization",
            "All external data must be treated as hostile. Validation at the boundary prevents injection attacks.",
            Arrays.asList(
                "Define strict input schema (e.g., JSON Schema/Zod)",
                "Reject any payload containing unmapped fields",
                "Apply type coercion and bounds checking",
                "Sanitize all strings to prevent XSS/SQLi payloads"
            ),
            1.0,
            Map.of("evidence_type", "industry_practice", "risk_if_ignored", "System is vulnerable to remote code execution or database compromise via payload manipulation.")
        );

        systemLearningService.recordTechnique(
            "CI_CD",
            "Deterministic Pipeline Caching",
            "Speeds up isolated builds without risking cross-contamination by using exact hash keys of dependency manifests.",
            Arrays.asList(
                "Hash dependency lockfile (e.g., package-lock.json)",
                "Use hash as primary cache key for dependency layer",
                "Restore cache before install phase",
                "If cache miss, perform full install and upload new cache"
            ),
            0.95,
            Map.of("evidence_type", "industry_practice", "risk_if_ignored", "Slow, expensive builds that hinder rapid autonomous iteration.")
        );

        systemLearningService.recordTechnique(
            "CI_CD",
            "Immutable Artifact Promotion",
            "Ensures that what is tested is exactly what is deployed, preventing drift between environments.",
            Arrays.asList(
                "Build container/artifact once during CI phase",
                "Tag with unique git SHA",
                "Deploy exact SHA to staging for integration tests",
                "Promote same SHA to production upon test success"
            ),
            0.98,
            Map.of("evidence_type", "architecture_pattern", "risk_if_ignored", "Rebuilding for production introduces silent differences that cause production-only outages.")
        );

        systemLearningService.recordTechnique(
            "CI_CD",
            "Atomic Deployment Verification",
            "Verifies after each fix. A deployment is not complete until health checks pass against the live artifact.",
            Arrays.asList(
                "Deploy artifact to staging slot or canary pod",
                "Run automated health and readiness probes",
                "Execute critical path smoke tests",
                "If any check fails, automatically rollback and halt pipeline"
            ),
            1.0,
            Map.of("evidence_type", "industry_practice", "risk_if_ignored", "Broken code goes live and stays live, waiting for human intervention.")
        );

        systemLearningService.recordTechnique(
            "PERFORMANCE",
            "N+1 Query Elimination",
            "Database loops drastically degrade performance. Identifying and replacing them with batch queries is a high-value optimization.",
            Arrays.asList(
                "Analyze ORM logs or query traces for repetitive identical queries",
                "Identify the loop structure in codebase",
                "Refactor to extract IDs and use a single IN clause query",
                "Map results back to objects in memory"
            ),
            0.95,
            Map.of("evidence_type", "benchmark", "risk_if_ignored", "Application grinds to a halt as dataset grows, exhausting database connections.")
        );

        systemLearningService.recordTechnique(
            "PERFORMANCE",
            "Payload Pagination Strategy",
            "Returning unbounded lists will eventually cause OOM and network exhaustion. Defaulting to paginated responses guarantees stable memory usage.",
            Arrays.asList(
                "Identify endpoints returning lists/arrays",
                "Enforce default limit (e.g., 50 items)",
                "Implement cursor-based or offset-based database queries",
                "Return standard pagination metadata (next_cursor, total_count)"
            ),
            0.98,
            Map.of("evidence_type", "industry_practice", "risk_if_ignored", "System crashes when queried tables accumulate thousands of rows.")
        );

        systemLearningService.recordTechnique(
            "PERFORMANCE",
            "Connection Pool Throttling",
            "Prevents the application from overwhelming the database during traffic spikes by managing a strict pool of persistent connections.",
            Arrays.asList(
                "Determine max connections supported by DB tier",
                "Set application connection pool max size to DB Max / Application Instances",
                "Configure connection timeout and idle evictions",
                "Implement queue for requests waiting for connections"
            ),
            0.90,
            Map.of("evidence_type", "architecture_pattern", "risk_if_ignored", "Database crashes under load due to connection starvation, taking down the entire system.")
        );

        systemLearningService.recordTechnique(
            "QUOTA_POLICY",
            "External API Tiered Degradation",
            "Protects the system when third-party AI APIs hit rate limits or exhaust budget, enforcing the solo-first rule.",
            Arrays.asList(
                "Monitor 429 status codes and quota exhaustion metrics",
                "Upon detection, disable heavy optional features",
                "Switch critical features to smaller, local, or fallback models",
                "Queue non-critical asynchronous tasks until quota resets"
            ),
            0.95,
            Map.of("evidence_type", "industry_practice", "risk_if_ignored", "System completely stops functioning when billing limits are reached.")
        );

        systemLearningService.recordTechnique(
            "QUOTA_POLICY",
            "Predictive Rate Limit Backoff",
            "Prevents being temporarily banned by APIs by respecting rate limits via exponential backoff and jitter.",
            Arrays.asList(
                "Intercept outgoing external API calls",
                "Check local token bucket limits",
                "If close to limit, pre-emptively delay request",
                "If 429 received, apply exponential backoff (2^n + jitter) before retry"
            ),
            0.90,
            Map.of("evidence_type", "architecture_pattern", "risk_if_ignored", "Providers blacklist system IPs due to aggressive polling.")
        );

        systemLearningService.recordTechnique(
            "QUOTA_POLICY",
            "Token Bucket Quota Enforcement",
            "Controls internal usage of expensive resources, preventing runaway automated loops from consuming the entire budget.",
            Arrays.asList(
                "Implement token bucket algorithm for high-cost operations",
                "Define refill rate based on daily budget",
                "Deduct tokens prior to operation execution",
                "Transition system to WAIT mode if tokens hit zero"
            ),
            0.95,
            Map.of("evidence_type", "industry_practice", "risk_if_ignored", "An autonomous agent loop drains the entire monthly API budget in an hour.")
        );

        systemLearningService.recordTechnique(
            "INCIDENT_LEARNING",
            "Automated Stacktrace Fingerprinting",
            "To learn from errors without duplicating records, the system must deterministically hash errors based on structure, ignoring dynamic data.",
            Arrays.asList(
                "Extract exception class and top 3 relevant stack frames",
                "Strip line numbers, dynamic IDs, and memory addresses",
                "Concatenate and generate SHA-256 hash",
                "Use hash as primary key in Incident Learning database"
            ),
            0.95,
            Map.of("evidence_type", "architecture_pattern", "risk_if_ignored", "Learning database fills with thousands of identical errors, corrupting confidence metrics.")
        );

        systemLearningService.recordTechnique(
            "INCIDENT_LEARNING",
            "Remediation Efficacy Scoring",
            "System must verify after each fix and score the fix attempt, allowing the learning system to prioritize proven solutions.",
            Arrays.asList(
                "Apply proposed fix to codebase",
                "Execute targeted test suite",
                "If tests pass, increment 'success' counter for that fix signature",
                "If tests fail, increment 'failure' counter and reduce confidence score"
            ),
            0.90,
            Map.of("evidence_type", "benchmark", "risk_if_ignored", "System repeats the same ineffective fixes in a loop.")
        );

        systemLearningService.recordTechnique(
            "INCIDENT_LEARNING",
            "Telemetry Snapshotting on Fatal",
            "When the system enters FORCE_STOP due to a critical unhandled exception, it must preserve the exact state for root cause analysis.",
            Arrays.asList(
                "Catch unhandled FATAL exception at root level",
                "Dump environment variables (scrubbing secrets)",
                "Capture last 100 log lines and current memory usage",
                "Serialize to immutable snapshot file and alert admin"
            ),
            0.98,
            Map.of("evidence_type", "postmortem", "risk_if_ignored", "Crucial context is lost on crash, making manual admin resolution impossible.")
        );

        systemLearningService.recordTechnique(
            "OPERATIONS",
            "Automated Liveness/Readiness Probing",
            "Infrastructure orchestrators (like Kubernetes or Cloud Run) need deterministic signals to route traffic safely or restart stalled containers.",
            Arrays.asList(
                "Implement /health/liveness endpoint returning 200 immediately",
                "Implement /health/readiness endpoint checking DB and local cache connectivity",
                "Configure orchestrator to poll endpoints",
                "Fail readiness if local dependencies are unreachable"
            ),
            1.0,
            Map.of("evidence_type", "industry_practice", "risk_if_ignored", "Traffic is routed to a container that is up but unable to process requests, causing silent failures.")
        );

        systemLearningService.recordTechnique(
            "OPERATIONS",
            "Zero-Downtime Rollback Execution",
            "If verification after a fix fails in production, the system must autonomously revert without dropping ongoing user requests.",
            Arrays.asList(
                "Detect verification failure post-deployment",
                "Signal load balancer to shift 100% traffic to previous healthy replica set",
                "Wait for active connections on broken replica to drain",
                "Terminate broken replica and log incident"
            ),
            0.90,
            Map.of("evidence_type", "architecture_pattern", "risk_if_ignored", "Rollbacks cause dropped connections and user-facing 502 errors.")
        );

        systemLearningService.recordTechnique(
            "BACKEND_SERVICES",
            "Transaction Boundary Enforcement",
            "Multi-step database mutations must be atomic to prevent partial data states when automated fixes or interruptions occur.",
            Arrays.asList(
                "Identify logical unit of work",
                "Wrap database calls in explicit transaction block",
                "Catch any exception during operation",
                "Execute strict rollback on exception before bubbling error up"
            ),
            0.98,
            Map.of("evidence_type", "industry_practice", "risk_if_ignored", "Database is left in an inconsistent state, causing cascading logical errors that AI cannot easily debug.")
        );

        systemLearningService.recordTechnique(
            "BACKEND_SERVICES",
            "Dead Letter Queue Triage",
            "Asynchronous tasks that repeatedly fail must be isolated to prevent blocking the queue and to allow the system to learn from the failure.",
            Arrays.asList(
                "Configure maximum delivery attempts on async queue",
                "Route failed messages to dedicated Dead Letter Queue (DLQ)",
                "Trigger incident learning routine on DLQ insertion",
                "Alert admin if DLQ depth exceeds threshold"
            ),
            0.95,
            Map.of("evidence_type", "architecture_pattern", "risk_if_ignored", "Poison pill messages continuously consume worker resources, halting processing of healthy messages.")
        );
    }
"""

idx = content.find("        seedDeploymentScopeKnowledge();")
if idx != -1:
    insert_pos = idx + len("        seedDeploymentScopeKnowledge();")
    content = content[:insert_pos] + "\n        seedAdvancedResearchFindings();" + content[insert_pos:]
    
    class_end = content.rfind("}")
    content = content[:class_end] + new_seed_method + "\n}"
    
    with open("src/main/java/org/example/learning/StrategicKnowledgeLearningInitializer.java", "w", encoding="utf-8") as f:
        f.write(content)
