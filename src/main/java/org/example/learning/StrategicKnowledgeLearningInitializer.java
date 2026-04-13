package org.example.learning;

import org.example.service.SystemLearningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

/**
 * Seeds strategic product, governance, and operational knowledge into SupremeAI memory.
 */
@Component
public class StrategicKnowledgeLearningInitializer {

    private static final Logger logger = LoggerFactory.getLogger(StrategicKnowledgeLearningInitializer.class);

    @Autowired
    private SystemLearningService systemLearningService;

    @EventListener(ApplicationReadyEvent.class)
    public void seedStrategicKnowledge() {
        logger.info("ðŸ§  StrategicKnowledgeLearningInitializer: seeding strategic system knowledge...");

        seedAdminControlKnowledge();
        seedSecurityAndGitRules();
        seedQuotaAndPerformanceKnowledge();
        seedArchitectureKnowledge();
        seedDeploymentScopeKnowledge();
        seedAdvancedResearchFindings();
        seedExtendedEngineeringPatterns();
        seedUnlimitedEngineeringWisdom();
        seedAutonomousEngineeringMastery();

        logger.info("âœ… StrategicKnowledgeLearningInitializer: strategic knowledge seeded.");
    }

    private void seedAdminControlKnowledge() {
        systemLearningService.recordRequirement(
            "Three-mode admin control is mandatory",
            "All state-changing operations must respect AUTO, WAIT, and FORCE_STOP modes with admin override and audit trail."
        );
        systemLearningService.recordRequirement(
            "Firebase-only authentication must remain stable",
            "The system must not create hardcoded default users and should rely on Firebase Auth users with protected setup flows."
        );

        systemLearningService.recordTechnique(
            "ADMIN_CONTROL",
            "Check admin mode before mutating state",
            "Every generation, git, deployment, or queue-changing action must check the runtime admin mode first.",
            Arrays.asList(
                "Read current admin mode before starting any state-changing work.",
                "Proceed immediately in AUTO mode.",
                "Queue for approval in WAIT mode.",
                "Block immediately in FORCE_STOP mode."
            ),
            0.99,
            Map.of("source", "admin-control", "priority", "critical")
        );
    }

    private void seedSecurityAndGitRules() {
        systemLearningService.recordTechnique(
            "SECURITY",
            "Protect setup and auth paths",
            "Never open bootstrap or setup endpoints to unauthenticated callers just to make onboarding easier.",
            Arrays.asList(
                "Use /api/auth/setup with token protection instead of open init flows.",
                "Validate required auth environment variables at startup.",
                "Do not bypass auth checks to work around failing tests or demos."
            ),
            0.99,
            Map.of("source", "common-mistakes", "severity", "critical")
        );

        systemLearningService.recordTechnique(
            "GIT_OPERATIONS",
            "Execute Git safely and verify semantic results",
            "Git commands must be safe from injection and must verify real outcome instead of trusting process exit code only.",
            Arrays.asList(
                "Use ProcessBuilder array arguments, never shell string concatenation.",
                "Validate branch names against a safe regex before push or checkout.",
                "Capture stdout and stderr separately.",
                "Parse output for fatal, error, and nothing-to-commit states before reporting success."
            ),
            0.99,
            Map.of("source", "common-mistakes", "priority", "high")
        );

        systemLearningService.recordPattern(
            "SECURITY",
            "Never remove security controls to pass a build or unblock a feature",
            "Real fixes preserve setup-token validation, auth rules, and input validation. Fast but unsafe shortcuts create production incidents."
        );
    }

    private void seedQuotaAndPerformanceKnowledge() {
        systemLearningService.recordTechnique(
            "QUOTA",
            "Operate within solo and multi-AI limits",
            "SupremeAI must adapt generation rate, concurrency, and memory usage to the configured operating mode.",
            Arrays.asList(
                "SOLO mode target: 10 apps per day, 1 concurrent app, 30-second timeout, around 3 GB memory.",
                "MULTI_AI mode target: 100 apps per day, 5 concurrent apps, 5-second timeout, around 8 GB memory.",
                "Queue new work when the max queue size of 50 is reached.",
                "Trigger cleanup before memory exceeds 7 GB and alert when CPU exceeds 85 percent."
            ),
            0.97,
            Map.of("source", "QUOTA_CONFIG.properties", "priority", "high")
        );

        systemLearningService.recordPattern(
            "QUOTA",
            "High-confidence learned patterns should be reused aggressively",
            "The system already tracks around 90 learned patterns with roughly 0.92 average confidence, so known fixes should be favored over fresh guesswork when signatures match."
        );

        systemLearningService.recordTechnique(
            "PERFORMANCE",
            "Use mode-aware performance targets",
            "Generation quality should be judged against configured success-rate and code-quality targets instead of vague expectations.",
            Arrays.asList(
                "SOLO target: 0.92 success rate and 0.85 code quality.",
                "MULTI_AI target: 0.96 success rate and 0.95 code quality.",
                "Use cache, retry, and backoff before treating transient external failures as hard failures."
            ),
            0.95,
            Map.of("source", "QUOTA_CONFIG.properties")
        );
    }

    private void seedArchitectureKnowledge() {
        systemLearningService.recordTechnique(
            "ARCHITECTURE",
            "Use layered generation and service orchestration",
            "SupremeAI should generate and validate through the model, service, controller, validation, and deployment layers instead of mixing concerns.",
            Arrays.asList(
                "RequirementAnalyzer defines what is needed.",
                "MultiAIConsensusService helps choose the best architecture path.",
                "CodeGenerator and SelfExtender create and integrate the implementation.",
                "Validation and auto-fix services verify and repair before completion."
            ),
            0.98,
            Map.of("source", "system-architecture")
        );

        systemLearningService.recordPattern(
            "SYSTEM_PHILOSOPHY",
            "SupremeAI is always a student with great ability",
            "It should learn from past failures, multi-AI consensus, feedback loops, and verified outcomes instead of acting like a one-shot code generator."
        );
    }

    private void seedDeploymentScopeKnowledge() {
        systemLearningService.recordTechnique(
            "DEPLOYMENT_SCOPE",
            "Generate all platforms but publish with correct ownership boundaries",
            "SupremeAI can generate production artifacts broadly, but publishing responsibility differs by platform.",
            Arrays.asList(
                "SupremeAI can generate backend, web, mobile, and desktop code and artifacts.",
                "SupremeAI may automate web deployment.",
                "Android Play Store and iOS App Store publishing remain owner-controlled because legal accounts and signing assets belong to the owner.",
                "System guidance should export production-ready artifacts and owner instructions for mobile publishing."
            ),
            0.96,
            Map.of("source", "deployment-scope", "priority", "medium")
        );

        systemLearningService.recordPattern(
            "DEPLOYMENT_SCOPE",
            "Separate build generation from legal publication responsibility",
            "This keeps the system technically capable while respecting real-world ownership, signing, and store-account constraints."
        );
    }


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
                "Scan output for \"waiting to lock\" alongside \"locked\"",
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



    private void seedExtendedEngineeringPatterns() {
        // --- RESILIENCE & HEALING ---
        systemLearningService.recordTechnique(
            "RESILIENCE",
            "Auto-Healing Pod Restarts",
            "When memory leaks or unrecoverable thread deadlocks occur, relying on orchestration-level restarts is safer than application-level recovery attempts.",
            Arrays.asList(
                "Configure readiness and liveness probes correctly.",
                "Ensure application is stateless and gracefully shuts down on SIGTERM.",
                "Allow Kubernetes/Cloud Run to kill and replace the container."
            ),
            0.98,
            Map.of("evidence_type", "cloud_native_patterns", "risk_if_ignored", "Zombie containers consume resources without serving traffic.")
        );

        systemLearningService.recordTechnique(
            "RESILIENCE",
            "Fallback Cache Utilization",
            "When primary database is unreachable, serve stale data from cache to maintain read-only operations.",
            Arrays.asList(
                "Wrap database reads in try-catch.",
                "On exception, query distributed cache (e.g., Redis).",
                "Return cached data with a header indicating it is stale."
            ),
            0.92,
            Map.of("evidence_type", "architecture_pattern", "risk_if_ignored", "Complete read outage during brief database hiccups.")
        );

        // --- SECURITY ---
        systemLearningService.recordTechnique(
            "SECURITY",
            "Zero Trust Network Boundaries",
            "Do not assume internal network traffic is safe. Authenticate and authorize every microservice-to-microservice call.",
            Arrays.asList(
                "Use mTLS for all internal service communication.",
                "Enforce strict network policies dropping unapproved traffic.",
                "Require JWT or service tokens for all internal APIs."
            ),
            0.99,
            Map.of("evidence_type", "security_standard", "risk_if_ignored", "Lateral movement of attackers if one microservice is compromised.")
        );

        systemLearningService.recordTechnique(
            "SECURITY",
            "Automated Dependency Vulnerability Scanning",
            "Integrate tools like Dependabot or Snyk to block builds containing known CVEs.",
            Arrays.asList(
                "Add vulnerability scanning step to CI pipeline.",
                "Fail the build if Critical or High vulnerabilities are found.",
                "Automate PR creation for dependency updates."
            ),
            0.97,
            Map.of("evidence_type", "devsecops_practice", "risk_if_ignored", "Shipping code with known exploits.")
        );

        // --- PERFORMANCE ---
        systemLearningService.recordTechnique(
            "PERFORMANCE",
            "Asynchronous Log Appending",
            "Synchronous logging blocks application threads. Use async loggers to prevent logging from becoming a bottleneck under high load.",
            Arrays.asList(
                "Configure logback/log4j to use AsyncAppender.",
                "Set appropriate queue size and discarding threshold for INFO/DEBUG logs.",
                "Never discard ERROR or FATAL logs."
            ),
            0.95,
            Map.of("evidence_type", "benchmark", "risk_if_ignored", "Application throughput collapses when logging volume increases.")
        );

        systemLearningService.recordTechnique(
            "PERFORMANCE",
            "Database Index Optimization",
            "Identify slow queries and add composite indexes matching the WHERE and ORDER BY clauses.",
            Arrays.asList(
                "Enable slow query logging in the database.",
                "Analyze query execution plans (EXPLAIN).",
                "Add indexes covering the exact fields used in filtering and sorting."
            ),
            0.96,
            Map.of("evidence_type", "database_optimization", "risk_if_ignored", "Full table scans locking the database under load.")
        );

        // --- CI/CD & DEPLOYMENT ---
        systemLearningService.recordTechnique(
            "CI_CD",
            "Canary Release Strategy",
            "Deploy new code to a small subset of users (e.g., 5%) before full rollout to catch unpredicted production errors.",
            Arrays.asList(
                "Configure load balancer to route 5% of traffic to new version.",
                "Monitor error rates and latency for 15 minutes.",
                "If metrics degrade, automatically route traffic back to stable.",
                "If stable, gradually increase traffic to 100%."
            ),
            0.94,
            Map.of("evidence_type", "release_engineering", "risk_if_ignored", "Deploying a fatal bug to 100% of users simultaneously.")
        );

        systemLearningService.recordTechnique(
            "CI_CD",
            "Infrastructure as Code (IaC) Validation",
            "Treat infrastructure changes like application code. Validate Terraform/Pulumi scripts before applying.",
            Arrays.asList(
                "Run 'terraform plan' in CI to show changes.",
                "Use static analysis (e.g., tfsec) to check for misconfigurations (e.g., public S3 buckets).",
                "Require manual approval for destructive changes."
            ),
            0.98,
            Map.of("evidence_type", "infrastructure_practice", "risk_if_ignored", "Accidental deletion of production databases or exposure of private networks.")
        );

        // --- DATA INTEGRITY ---
        systemLearningService.recordTechnique(
            "DATA_INTEGRITY",
            "Soft Deletion Pattern",
            "Never hard-delete records. Mark them as deleted to allow for easy recovery and auditability.",
            Arrays.asList(
                "Add a 'deleted_at' timestamp column to critical tables.",
                "Update application queries to filter out records where 'deleted_at' is not null.",
                "Create a background job to permanently archive soft-deleted records after 90 days."
            ),
            0.95,
            Map.of("evidence_type", "data_management", "risk_if_ignored", "Permanent loss of user data due to bugs or accidental admin actions.")
        );

        systemLearningService.recordTechnique(
            "DATA_INTEGRITY",
            "Optimistic Concurrency Control",
            "Prevent lost updates in concurrent environments by using version numbers on database records.",
            Arrays.asList(
                "Add a 'version' integer column to the table.",
                "Include the version in the WHERE clause of UPDATE statements.",
                "Increment the version by 1 on update.",
                "If the update affects 0 rows, throw a ConcurrentModificationException."
            ),
            0.97,
            Map.of("evidence_type", "database_pattern", "risk_if_ignored", "Two users editing the same record overwrite each other's changes silently.")
        );
    }



    private void seedUnlimitedEngineeringWisdom() {
        // --- HIGH AVAILABILITY ---
        systemLearningService.recordTechnique(
            "HIGH_AVAILABILITY",
            "Multi-Region Failover Routing",
            "Route traffic to an alternative cloud region immediately if the primary region goes down, ensuring continuous operation.",
            Arrays.asList(
                "Deploy active-passive database replication across regions.",
                "Use Global Load Balancer to detect region failure.",
                "Automatically switch DNS routing to the healthy region."
            ),
            0.96,
            Map.of("evidence_type", "disaster_recovery", "risk_if_ignored", "Complete business outage during a cloud provider region failure.")
        );

        // --- QUEUES & ASYNC ---
        systemLearningService.recordTechnique(
            "MESSAGE_BROKER",
            "Exponential Backoff in Message Consumption",
            "Prevent overwhelming downstream services during partial outages by introducing exponentially increasing delays.",
            Arrays.asList(
                "Capture external API or database timeouts.",
                "Re-queue the message with an incremented 'attempt' count.",
                "Calculate delay: base_delay * (2 ^ attempt).",
                "Move to Dead Letter Queue after max attempts."
            ),
            0.98,
            Map.of("evidence_type", "distributed_systems", "risk_if_ignored", "Thundering herd problem that brings down recovering services.")
        );

        // --- OBSERVABILITY ---
        systemLearningService.recordTechnique(
            "OBSERVABILITY",
            "Distributed Tracing Injection",
            "Propagate a Trace ID across all microservices to track a single user request from ingress to database.",
            Arrays.asList(
                "Generate a UUID at the API Gateway (X-Request-ID).",
                "Include the UUID in all log statements using MDC.",
                "Pass the UUID in headers for downstream HTTP/RPC calls.",
                "Visualize bottlenecks using Jaeger or Zipkin."
            ),
            0.99,
            Map.of("evidence_type", "sre_practice", "risk_if_ignored", "Inability to debug why a specific request is slow across 5 different services.")
        );

        systemLearningService.recordTechnique(
            "OBSERVABILITY",
            "Semantic Alerting Thresholds",
            "Avoid alert fatigue by only triggering PagerDuty for actionable, business-impacting metrics, not raw CPU spikes.",
            Arrays.asList(
                "Monitor High Error Rate (5xx > 1%).",
                "Monitor High Latency (P99 > 2 seconds).",
                "Ignore brief CPU spikes if latency and error rate are normal."
            ),
            0.95,
            Map.of("evidence_type", "sre_alerting", "risk_if_ignored", "Engineers ignore pages because the system constantly alerts on non-issues.")
        );

        // --- STATE MANAGEMENT ---
        systemLearningService.recordTechnique(
            "STATE_MANAGEMENT",
            "Event Sourcing Architecture",
            "Instead of storing the current state, store a sequence of immutable events that led to the state.",
            Arrays.asList(
                "Define events (e.g., 'OrderCreated', 'ItemAdded').",
                "Append events to an append-only log.",
                "Project events into a read-model for fast querying.",
                "Replay events to debug historical states or rebuild read-models."
            ),
            0.85,
            Map.of("evidence_type", "architecture_pattern", "risk_if_ignored", "Losing the historical context of how data reached its current state.")
        );

        // --- AI SPECIFIC ---
        systemLearningService.recordTechnique(
            "AI_INTEGRATION",
            "Prompt Injection Sanitization",
            "Prevent users from manipulating AI responses by separating user input from system instructions.",
            Arrays.asList(
                "Use distinct system and user message roles.",
                "Filter user input for common jailbreak phrases.",
                "Wrap user input in distinct delimiters (e.g., XML tags).",
                "Validate AI output schema before processing."
            ),
            0.97,
            Map.of("evidence_type", "ai_security", "risk_if_ignored", "Malicious users bypass system constraints or leak system prompts.")
        );

        systemLearningService.recordTechnique(
            "AI_INTEGRATION",
            "LLM Output Schema Enforcement",
            "Never parse raw string output from LLMs. Force structured data (JSON/XML) for predictable processing.",
            Arrays.asList(
                "Instruct the LLM to return strictly valid JSON.",
                "Provide a JSON Schema in the prompt.",
                "Parse the response through a rigid parser.",
                "Retry with error feedback if parsing fails."
            ),
            0.98,
            Map.of("evidence_type", "ai_engineering", "risk_if_ignored", "Application crashes due to unexpected markdown or conversational text in AI response.")
        );

        // --- TESTING ---
        systemLearningService.recordTechnique(
            "TESTING",
            "Property-Based Testing",
            "Generate hundreds of random inputs to test the invariant properties of a function, catching edge cases unit tests miss.",
            Arrays.asList(
                "Define the invariant (e.g., reversing a list twice equals the original list).",
                "Use a library (e.g., jqwik) to generate random lists.",
                "Run the test 1000 times automatically.",
                "Shrink failing inputs to the minimal reproducing case."
            ),
            0.90,
            Map.of("evidence_type", "advanced_testing", "risk_if_ignored", "System fails on unexpected nulls, negative numbers, or empty strings in production.")
        );

        systemLearningService.recordTechnique(
            "TESTING",
            "Chaos Engineering Experiments",
            "Intentionally inject failures (latency, dropped packets, crashed pods) into staging to verify resilience mechanisms.",
            Arrays.asList(
                "Define the steady-state hypothesis.",
                "Introduce an error (e.g., block database port).",
                "Verify that Circuit Breakers and Fallbacks activate.",
                "Restore the system and ensure steady-state returns."
            ),
            0.88,
            Map.of("evidence_type", "resilience_engineering", "risk_if_ignored", "Fallbacks fail in production because they were never tested under real duress.")
        );
    }



    private void seedAutonomousEngineeringMastery() {
        // --- AI ORCHESTRATION ---
        systemLearningService.recordTechnique(
            "AI_ORCHESTRATION",
            "Context Window Compression Strategy",
            "When logs or source code exceed LLM context limits, summarize older historical context rather than truncating the end (which usually contains the actual error).",
            Arrays.asList(
                "Detect when payload tokens exceed 80% of model limit.",
                "Extract the top 100 lines (setup) and bottom 200 lines (stacktrace/error).",
                "Summarize the middle section into a brief structural description.",
                "Feed the compressed payload to the model."
            ),
            0.96,
            Map.of("evidence_type", "ai_engineering", "risk_if_ignored", "Model hallucinates fixes because the actual error message was truncated.")
        );

        systemLearningService.recordTechnique(
            "AI_ORCHESTRATION",
            "Multi-Agent Consensus Degradation",
            "If multiple AI models cannot reach consensus on a critical architecture decision, safely degrade to the highest-confidence solo model rather than blocking progress.",
            Arrays.asList(
                "Trigger MultiAIConsensusService with a timeout.",
                "If votes are tied or confidence is low, identify the model with the highest historical success rate for this category.",
                "Log the consensus failure and proceed with the solo model's choice."
            ),
            0.94,
            Map.of("evidence_type", "autonomous_systems", "risk_if_ignored", "System enters indefinite WAIT mode due to minor disagreements between models.")
        );

        // --- CODE GENERATION ---
        systemLearningService.recordTechnique(
            "CODE_GENERATION",
            "Iterative Compilation Feedback Loop",
            "Compile the codebase after every major component is generated, rather than waiting for the entire project to be generated.",
            Arrays.asList(
                "Generate Domain Models -> Compile.",
                "Generate Repositories -> Compile.",
                "Generate Services -> Compile.",
                "Feed compilation errors immediately back to the CodeGenerator before moving to the next layer."
            ),
            0.98,
            Map.of("evidence_type", "compiler_theory", "risk_if_ignored", "Accumulating 50 compilation errors across 10 files makes it impossible for the AI to fix them all at once.")
        );

        systemLearningService.recordTechnique(
            "CODE_GENERATION",
            "AST-Based Code Modification",
            "Parse code into an Abstract Syntax Tree (AST) to make targeted structural changes rather than relying on brittle regex or line-number string replacements.",
            Arrays.asList(
                "Use a language-specific parser (e.g., JavaParser, Babel).",
                "Locate the target MethodDeclaration or ClassDeclaration.",
                "Apply the modification to the AST node.",
                "Write the AST back to source code."
            ),
            0.93,
            Map.of("evidence_type", "static_analysis", "risk_if_ignored", "Regex replaces the wrong variable or corrupts syntax, causing cascading build failures.")
        );

        // --- SECURITY ---
        systemLearningService.recordTechnique(
            "SECURITY",
            "Ephemeral Build Sandboxing",
            "Always execute AI-generated code, tests, and builds in strictly isolated, ephemeral environments with zero access to internal metadata services.",
            Arrays.asList(
                "Spin up a temporary Docker container for the build.",
                "Disable network access to 169.254.169.254 (Cloud Metadata).",
                "Execute the build/test command.",
                "Destroy the container immediately after, regardless of success or failure."
            ),
            0.99,
            Map.of("evidence_type", "devsecops_practice", "risk_if_ignored", "Maliciously generated code extracts cloud credentials and exfiltrates them.")
        );

        // --- ARCHITECTURE ---
        systemLearningService.recordTechnique(
            "ARCHITECTURE",
            "Saga Pattern for Distributed Transactions",
            "Use compensating transactions to undo previous steps if a multi-service workflow fails, rather than relying on distributed locking.",
            Arrays.asList(
                "Define a sequence of local transactions.",
                "For each step, define a compensating action (e.g., 'Reserve Inventory' -> 'Release Inventory').",
                "If step N fails, trigger compensating actions for steps N-1 down to 1.",
                "Record the final state in a Saga execution log."
            ),
            0.95,
            Map.of("evidence_type", "architecture_pattern", "risk_if_ignored", "Distributed deadlocks or partial data inconsistencies across microservices.")
        );

        systemLearningService.recordTechnique(
            "ARCHITECTURE",
            "Strangler Fig Pattern",
            "Safely replace legacy monolithic code by intercepting calls at the edge and incrementally routing specific features to new microservices.",
            Arrays.asList(
                "Place an API Gateway in front of the legacy system.",
                "Build the new service alongside the legacy one.",
                "Configure the gateway to route traffic for specific endpoints to the new service.",
                "Gradually increase the routed endpoints until the legacy system can be decommissioned."
            ),
            0.97,
            Map.of("evidence_type", "architecture_pattern", "risk_if_ignored", "Big-bang rewrites fail because they attempt to replace too much complexity at once.")
        );

        // --- DEPLOYMENT ---
        systemLearningService.recordTechnique(
            "DEPLOYMENT",
            "Backward Compatible Schema Migrations",
            "Never drop a database column or table in the same deployment where the application code stops using it. Separate into two phases.",
            Arrays.asList(
                "Phase 1: Deploy code that ignores the deprecated column.",
                "Ensure all active instances of the app are running the new code.",
                "Phase 2: In a subsequent deployment, safely DROP the column."
            ),
            0.98,
            Map.of("evidence_type", "database_management", "risk_if_ignored", "Rolling deployments cause application crashes because old instances query a dropped column.")
        );

        // --- DATABASE ---
        systemLearningService.recordTechnique(
            "DATABASE_MANAGEMENT",
            "Connection Leak Active Detection",
            "Do not rely solely on application logic to close connections. Configure the pool to detect and kill abandoned connections.",
            Arrays.asList(
                "Configure connection pool (e.g., HikariCP) with leakDetectionThreshold.",
                "Set threshold to a reasonable maximum query time (e.g., 30000ms).",
                "Log the stack trace of the thread that originally checked out the leaked connection.",
                "Forcibly close the connection to prevent pool exhaustion."
            ),
            0.96,
            Map.of("evidence_type", "reliability_engineering", "risk_if_ignored", "Application slowly grinds to a halt as connections are checked out and lost due to unhandled exceptions.")
        );

        systemLearningService.recordTechnique(
            "DATABASE_MANAGEMENT",
            "Read Replica Offloading",
            "Protect the primary database from heavy analytical or non-critical read queries by routing them to asynchronously replicated read-only instances.",
            Arrays.asList(
                "Configure a Primary-Replica database topology.",
                "Use a read-write data source for POST/PUT/PATCH/DELETE operations.",
                "Use a read-only data source for heavy GET operations (e.g., reporting, dashboard stats).",
                "Accept eventual consistency for these read operations."
            ),
            0.92,
            Map.of("evidence_type", "scaling_pattern", "risk_if_ignored", "Heavy analytical queries lock tables or consume CPU, preventing users from saving critical transactions.")
        );
    }

}