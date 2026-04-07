package org.example.learning;

import org.example.service.SystemLearningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Seeds deep engineering excellence knowledge into SupremeAI memory.
 * 
 * Teaches the system expert-level practices in:
 * - Root cause analysis & debugging methodology
 * - Systematic codebase auditing
 * - Cross-file consistency patterns
 * - Data persistence architecture
 * - Build system & dependency management
 * - Security-first development
 * - Performance diagnosis
 * - API design principles
 * - Error handling philosophy
 * - Git workflow excellence
 * 
 * These are lessons from real-world engineering — not theory.
 * Each technique includes concrete steps the system can follow.
 */
@Component
public class EngineeringExcellenceKnowledgeInitializer {

    private static final Logger logger = LoggerFactory.getLogger(EngineeringExcellenceKnowledgeInitializer.class);

    @Autowired
    private SystemLearningService learningService;

    @EventListener(ApplicationReadyEvent.class)
    public void seedEngineeringExcellence() {
        logger.info("🎓 EngineeringExcellenceKnowledgeInitializer: seeding deep engineering knowledge...");

        seedRootCauseAnalysis();
        seedSystematicDebugging();
        seedCodebaseAuditing();
        seedPersistencePatterns();
        seedAPIDesignPrinciples();
        seedErrorHandlingPhilosophy();
        seedSecurityEngineering();
        seedPerformanceDiagnosis();
        seedBuildSystemKnowledge();
        seedGitWorkflowExcellence();
        seedCrossFileConsistency();
        seedSpringBootMastery();

        logger.info("✅ EngineeringExcellenceKnowledgeInitializer: {} categories of engineering knowledge seeded.", 12);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 1. ROOT CAUSE ANALYSIS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private void seedRootCauseAnalysis() {
        learningService.recordTechnique(
            "DEBUGGING",
            "5-Why Root Cause Analysis",
            "Never fix symptoms — always trace back to the actual root cause by asking WHY 5 times. " +
            "Example: Dashboard data disappears → WHY? Server restarted → WHY data lost? ConcurrentHashMap is in-memory only → " +
            "WHY no persistence? Nobody added disk storage → ROOT CAUSE: Missing persistence layer.",
            Arrays.asList(
                "1. Observe the EXACT symptom (what fails, when, for whom).",
                "2. Ask WHY this symptom occurs — get the immediate cause.",
                "3. Ask WHY that immediate cause exists — get the deeper cause.",
                "4. Repeat until you reach a design-level or architecture-level root cause.",
                "5. Fix at the ROOT level, not the symptom level.",
                "6. Verify the fix resolves the original symptom AND similar cases."
            ),
            0.99,
            Map.of("source", "engineering-excellence", "priority", "critical",
                    "realExample", "Firebase getApplicationDefault() hangs on non-GCP machines — symptom was 'server takes 5 min to start', root cause was blocking credential lookup with no timeout")
        );

        learningService.recordTechnique(
            "DEBUGGING",
            "Reproduce Before You Fix",
            "Never attempt a fix without first reproducing the problem. A fix you cannot verify is not a fix.",
            Arrays.asList(
                "1. Get exact reproduction steps from the error/user report.",
                "2. Reproduce locally or in a test environment.",
                "3. Confirm you see the SAME error/behavior.",
                "4. Apply your fix.",
                "5. Re-run the SAME reproduction steps.",
                "6. Confirm the error is gone AND no new issues appeared."
            ),
            0.98,
            Map.of("source", "engineering-excellence", "priority", "high")
        );

        learningService.recordTechnique(
            "DEBUGGING",
            "Binary Search Debugging for Large Codebases",
            "When a bug could be in many places, use binary search: eliminate half the possibilities each step.",
            Arrays.asList(
                "1. List all possible locations/causes.",
                "2. Test the middle point — does the bug still happen?",
                "3. If yes, the bug is in the untested half. If no, it's in the tested half.",
                "4. Repeat until you narrow down to the exact line/component.",
                "5. Works for: git bisect, commenting out code blocks, disabling services one by one."
            ),
            0.95,
            Map.of("source", "engineering-excellence")
        );

        learningService.recordPattern(
            "DEBUGGING",
            "Separate STDERR from STDOUT when debugging processes",
            "Always capture stderr and stdout separately. Merged streams hide errors inside normal output. " +
            "Use ProcessBuilder.redirectErrorStream(false) and read both streams. " +
            "Check stderr for 'error', 'fatal', 'exception' keywords independently."
        );
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 2. SYSTEMATIC DEBUGGING
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private void seedSystematicDebugging() {
        learningService.recordTechnique(
            "DEBUGGING",
            "Startup Hang Diagnosis",
            "When a Spring Boot app hangs during startup, the cause is usually a blocking call in @PostConstruct or @Bean initialization. " +
            "Network calls (HTTP, DNS, metadata servers) are the most common culprits.",
            Arrays.asList(
                "1. Check if the app prints the Spring Boot banner — if yes, it's past basic init.",
                "2. Look for @PostConstruct methods that make network calls.",
                "3. Look for @Bean methods that call external services.",
                "4. Check for GoogleCredentials.getApplicationDefault() — this tries GCE metadata server and hangs 5+ minutes on non-GCP machines.",
                "5. Check for database connection timeouts with no fallback.",
                "6. Fix: Add environment variable guards, set connection timeouts, or catch and continue."
            ),
            0.99,
            Map.of("source", "engineering-excellence", "priority", "critical",
                    "realExample", "FirebaseService and FirebaseConfig both called getApplicationDefault() causing 10+ minute startup hang")
        );

        learningService.recordTechnique(
            "DEBUGGING",
            "Port Conflict Resolution",
            "When 'Port already in use' error occurs, the previous process wasn't properly terminated.",
            Arrays.asList(
                "1. Find the process: Get-NetTCPConnection -LocalPort 8080 (Windows) or lsof -i :8080 (Linux).",
                "2. Kill the process by PID.",
                "3. Also stop Gradle daemons: ./gradlew --stop (they can hold ports).",
                "4. Verify port is free before restarting.",
                "5. Prevention: Always gracefully shut down servers before restarting."
            ),
            0.95,
            Map.of("source", "engineering-excellence")
        );

        learningService.recordTechnique(
            "DEBUGGING",
            "Compilation Error Triage",
            "When build fails with errors, use a systematic approach rather than guessing.",
            Arrays.asList(
                "1. Read the FIRST error — later errors are often cascading from it.",
                "2. Check: Is it a missing import? Wrong method name? Type mismatch?",
                "3. For 'cannot find symbol': verify the class/method exists in the dependency or codebase.",
                "4. For API mismatches: check the actual method signature in the service class (e.g., read() vs load()).",
                "5. Fix one error, recompile, then fix the next.",
                "6. Never fix all errors blindly — cascading errors disappear when root error is fixed."
            ),
            0.97,
            Map.of("source", "engineering-excellence", "priority", "high")
        );

        learningService.recordPattern(
            "DEBUGGING",
            "Always check actual method signatures before calling",
            "When using a service, read its actual public methods first. Don't assume method names — " +
            "for example LocalJsonStoreService uses read()/write(), not load()/save(). " +
            "A 2-second check prevents a 30-second compile-fix cycle."
        );
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 3. CODEBASE AUDITING
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private void seedCodebaseAuditing() {
        learningService.recordTechnique(
            "CODE_ARCHITECTURE",
            "Systematic Service Audit — Find All Similar Bugs at Once",
            "When you find a bug in one service, AUDIT ALL services for the same pattern. " +
            "Don't fix one and leave 20 others broken. This is the #1 technique for preventing recurring issues.",
            Arrays.asList(
                "1. Find the bug pattern (e.g., 'data stored only in ConcurrentHashMap, lost on restart').",
                "2. Search the ENTIRE codebase for the same pattern (grep for ConcurrentHashMap, in-memory, etc.).",
                "3. List ALL affected services/files.",
                "4. Prioritize by impact: user-facing data > internal metrics > logs.",
                "5. Apply the SAME fix pattern to ALL affected services.",
                "6. Compile and test after each batch of fixes.",
                "7. Document: 'X out of Y services were affected, all fixed with pattern Z.'"
            ),
            0.99,
            Map.of("source", "engineering-excellence", "priority", "critical",
                    "realExample", "Found 21 out of 31 services lost data on restart — fixed 9 critical services with LocalJsonStoreService persistence pattern")
        );

        learningService.recordTechnique(
            "CODE_ARCHITECTURE",
            "Service Dependency Mapping",
            "Before modifying a service, trace its dependencies and dependents. A change in ServiceA may break ServiceB.",
            Arrays.asList(
                "1. Search for all @Autowired references to the service you're changing.",
                "2. Check constructor injections and @Bean definitions.",
                "3. Trace the call chain: Controller → Service → Repository/Firebase.",
                "4. If changing a constructor, find ALL callers (both Spring-managed and manual 'new' calls).",
                "5. Update ALL callers consistently.",
                "6. Compile to catch any missed references."
            ),
            0.96,
            Map.of("source", "engineering-excellence", "priority", "high",
                    "realExample", "QuotaTracker constructor changed from (FirebaseService) to (FirebaseService, LocalJsonStoreService) — had to update ServiceConfiguration.java AND AgentOrchestrator.java")
        );

        learningService.recordPattern(
            "CODE_ARCHITECTURE",
            "Count affected files before starting a cross-cutting fix",
            "Before applying a pattern across multiple files, count how many files are affected. " +
            "Plan the work: 5 files = do it now, 50 files = batch by priority, 500 files = automate with script. " +
            "Always compile after each batch to catch errors early."
        );
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 4. PERSISTENCE PATTERNS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private void seedPersistencePatterns() {
        learningService.recordTechnique(
            "DATA_PERSISTENCE",
            "In-Memory + Disk Dual-Write Pattern",
            "For fast reads with crash safety: keep ConcurrentHashMap for speed, persist to disk/DB for durability. " +
            "Read from memory (fast), write to both memory AND disk (safe).",
            Arrays.asList(
                "1. Use ConcurrentHashMap as the primary read source (O(1) lookup, thread-safe).",
                "2. On every WRITE operation: update map FIRST, then persist to disk.",
                "3. On startup (@PostConstruct): read from disk and populate the map.",
                "4. Persist method should be try-catch — disk failure shouldn't crash the app.",
                "5. Use TypeReference for proper JSON deserialization of generic types.",
                "6. Choose a clear store path: 'data/supremeai/{service-name}/{file}.json'."
            ),
            0.99,
            Map.of("source", "engineering-excellence", "priority", "critical",
                    "realExample", "ExistingProjectService, UserQuotaService, MetricsService, ActivitySummaryService all use this pattern")
        );

        learningService.recordTechnique(
            "DATA_PERSISTENCE",
            "Graceful Firebase Fallback",
            "Firebase may be unavailable locally. Always have a fallback: try Firebase first, catch exception, use local data.",
            Arrays.asList(
                "1. Wrap Firebase calls in try-catch.",
                "2. On failure, fall back to local ConcurrentHashMap/disk data.",
                "3. Log the fallback with WARN level (not ERROR — it's expected locally).",
                "4. For startup: check if credential env vars exist BEFORE calling Firebase init.",
                "5. Never call GoogleCredentials.getApplicationDefault() without checking GOOGLE_APPLICATION_CREDENTIALS first.",
                "6. Set metadata server timeout: System.setProperty('com.google.cloud.compute.metadata.timeout', '3000')."
            ),
            0.98,
            Map.of("source", "engineering-excellence", "priority", "critical")
        );

        learningService.recordRequirement(
            "Every user-facing data must survive server restarts",
            "Any data that an admin/user creates, updates, or configures MUST be persisted to disk or database. " +
            "ConcurrentHashMap alone is NOT sufficient — it's lost on JVM restart. " +
            "Use LocalJsonStoreService for disk persistence when Firebase is unavailable."
        );

        learningService.recordPattern(
            "DATA_PERSISTENCE",
            "Persist after EVERY mutation, not just periodically",
            "Call persistToStorage() after every add, update, remove, toggle operation. " +
            "Don't batch or delay — if the server crashes between mutations, you lose data. " +
            "The disk write cost is negligible compared to data loss cost."
        );
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 5. API DESIGN
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private void seedAPIDesignPrinciples() {
        learningService.recordTechnique(
            "API_DESIGN",
            "RESTful Endpoint Naming & Structure",
            "APIs should be predictable: noun-based paths, HTTP verbs for actions, consistent response format.",
            Arrays.asList(
                "1. Use nouns for resources: /api/projects, /api/users, /api/research.",
                "2. Use HTTP verbs: GET (read), POST (create/action), PUT (full update), PATCH (partial), DELETE.",
                "3. Return consistent JSON: { status, data/message, timestamp }.",
                "4. Use proper HTTP codes: 200 (ok), 201 (created), 400 (bad input), 401 (not authenticated), 403 (forbidden), 404 (not found).",
                "5. Group related endpoints: GET /api/research/settings + POST /api/research/settings.",
                "6. Use query params for filtering: GET /api/research/history?limit=10&domain=SECURITY."
            ),
            0.95,
            Map.of("source", "engineering-excellence")
        );

        learningService.recordTechnique(
            "API_DESIGN",
            "Partial Update Pattern (PATCH-style with POST)",
            "Allow updating only the fields the client sends, keep existing values for omitted fields.",
            Arrays.asList(
                "1. Accept a Map<String, Object> request body.",
                "2. Check request.containsKey('fieldName') for each configurable field.",
                "3. Only update fields that are present in the request.",
                "4. Return the updated values in the response.",
                "5. Validate each field independently (range checks, type checks).",
                "6. This avoids requiring clients to send ALL fields just to change one."
            ),
            0.94,
            Map.of("source", "engineering-excellence",
                    "realExample", "POST /api/research/settings accepts partial updates — send only {cycleIntervalMinutes: 10} without needing other fields")
        );

        learningService.recordPattern(
            "API_DESIGN",
            "Always validate input at the controller boundary",
            "Validate request body fields in the controller BEFORE passing to service. " +
            "Check: null/empty, type casting, range limits, string length, regex for patterns (branch names, emails). " +
            "Return 400 Bad Request with a clear error message for invalid input."
        );
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 6. ERROR HANDLING PHILOSOPHY
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private void seedErrorHandlingPhilosophy() {
        learningService.recordTechnique(
            "ERROR_HANDLING",
            "Fail Fast at Boundaries, Recover Gracefully Inside",
            "Validate inputs strictly at system boundaries (controllers). Inside services, catch and recover.",
            Arrays.asList(
                "1. Controller: validate ALL input → return 400 immediately if invalid.",
                "2. Service: try operation → catch exception → log → return fallback/default.",
                "3. Never silently swallow exceptions — always log at minimum WARN level.",
                "4. Include context in error messages: WHAT failed, WHERE (class/method), WHY (root cause).",
                "5. For non-critical paths (metrics, logging): catch and continue, don't crash the main flow.",
                "6. For critical paths (auth, data save): propagate error to caller."
            ),
            0.97,
            Map.of("source", "engineering-excellence", "priority", "high")
        );

        learningService.recordPattern(
            "ERROR_HANDLING",
            "Log-and-continue for optional dependencies",
            "When Firebase, external APIs, or optional services fail, log the error and continue with fallback behavior. " +
            "Example: Firebase init fails → log warning → continue with in-memory mode. " +
            "The system should ALWAYS start, even if some features are degraded."
        );

        learningService.recordPattern(
            "ERROR_HANDLING",
            "Include actionable context in error messages",
            "Bad: 'Error occurred'. Good: 'Firebase init failed: No credentials found — set FIREBASE_SERVICE_ACCOUNT_JSON or GOOGLE_APPLICATION_CREDENTIALS'. " +
            "Always tell the user/developer WHAT to do to fix it."
        );

        learningService.learnFromIncident(
            "STARTUP",
            "Server hangs for 5+ minutes during startup",
            "GoogleCredentials.getApplicationDefault() tries to contact GCE metadata server (169.254.169.254) which times out on non-GCP machines",
            "Check GOOGLE_APPLICATION_CREDENTIALS env var before calling getApplicationDefault(). Skip if not set.",
            Arrays.asList(
                "Check all @PostConstruct and @Bean methods for network calls",
                "Verify credential env vars exist before attempting remote auth",
                "Set metadata server timeout: System.setProperty('com.google.cloud.compute.metadata.timeout', '3000')",
                "Test startup on a local machine without cloud credentials"
            ),
            0.99,
            Map.of("affectedFiles", "FirebaseService.java, FirebaseConfig.java")
        );
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 7. SECURITY ENGINEERING
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private void seedSecurityEngineering() {
        learningService.recordRequirement(
            "Never expose initialization endpoints without token protection",
            "Open /api/auth/init endpoints are a critical security hole. " +
            "Always use token-protected /api/auth/setup with SUPREMEAI_SETUP_TOKEN environment variable. " +
            "The endpoint must be one-time-only (reject if users already exist)."
        );

        learningService.recordTechnique(
            "SECURITY",
            "Command Injection Prevention",
            "When executing system commands (git, build tools), NEVER use string concatenation for arguments.",
            Arrays.asList(
                "1. Use ProcessBuilder with array arguments: new ProcessBuilder('git', 'commit', '-m', message).",
                "2. Validate branch names against regex: ^[a-zA-Z0-9/_.-]+$ (no spaces, special chars).",
                "3. Validate commit messages: no backticks, no $(), no semicolons in critical positions.",
                "4. Set working directory explicitly: processBuilder.directory(new File(projectPath)).",
                "5. Capture stderr separately for error detection.",
                "6. Check exit code AND output for 'fatal'/'error' keywords."
            ),
            0.99,
            Map.of("source", "engineering-excellence", "priority", "critical")
        );

        learningService.recordTechnique(
            "SECURITY",
            "Environment Variable Security Pattern",
            "Sensitive values (tokens, API keys, passwords) must come from environment variables, never hardcoded.",
            Arrays.asList(
                "1. Define expected env vars: GITHUB_TOKEN, SUPREMEAI_SETUP_TOKEN, JWT_SECRET, etc.",
                "2. Check for null/empty on startup — log clear error messages if missing.",
                "3. Never log the actual value of a secret — log 'GITHUB_TOKEN: [SET]' or '[MISSING]'.",
                "4. For local development: use .env file (gitignored) or local.properties.",
                "5. For production: use platform secrets (Render, Railway, GCP Secret Manager).",
                "6. Rotate secrets regularly — design APIs to accept new keys without downtime."
            ),
            0.97,
            Map.of("source", "engineering-excellence", "priority", "high")
        );

        learningService.recordPattern(
            "SECURITY",
            "Validate at system boundaries, trust within service layer",
            "All input validation happens at the Controller. Once data passes validation, " +
            "service methods can trust it. This centralizes validation logic and prevents redundant checks. " +
            "Exception: security-critical operations (auth, payments) should double-check."
        );
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 8. PERFORMANCE DIAGNOSIS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private void seedPerformanceDiagnosis() {
        learningService.recordTechnique(
            "PERFORMANCE",
            "Identify Blocking Calls in Async Systems",
            "The #1 performance killer is a BLOCKING call in an otherwise async/concurrent system.",
            Arrays.asList(
                "1. Profile: measure time at each step of the critical path.",
                "2. Look for: CompletableFuture.get() without timeout, Thread.sleep(), synchronized blocks.",
                "3. Look for: network calls without timeout (HTTP, DNS, metadata servers).",
                "4. Look for: database queries without connection pool limits.",
                "5. Fix: add timeouts to ALL network calls, use async alternatives where possible.",
                "6. Test: measure startup time, request latency, throughput under load."
            ),
            0.96,
            Map.of("source", "engineering-excellence", "priority", "high")
        );

        learningService.recordPattern(
            "PERFORMANCE",
            "Cache aggressively, invalidate precisely",
            "Use ConcurrentHashMap for frequently-read, rarely-changed data (user profiles, config, API keys). " +
            "Invalidate on write, not on timer. This gives O(1) reads without stale data. " +
            "For collections: synchronizedList or CopyOnWriteArrayList depending on read/write ratio."
        );

        learningService.recordPattern(
            "PERFORMANCE",
            "Measure before optimizing — don't guess bottlenecks",
            "Always measure actual performance before optimizing. Use System.currentTimeMillis() around suspect code blocks. " +
            "Log durations for critical operations. The real bottleneck is rarely where you expect it."
        );
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 9. BUILD SYSTEM KNOWLEDGE
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private void seedBuildSystemKnowledge() {
        learningService.recordTechnique(
            "BUILD_PATTERNS",
            "Gradle Build Troubleshooting",
            "Common Gradle issues and their fixes for Spring Boot projects.",
            Arrays.asList(
                "1. 'Port already in use': kill old process + stop Gradle daemons (./gradlew --stop).",
                "2. 'compileJava failed': read FIRST error, fix it, recompile — later errors often cascade.",
                "3. Daemon issues: ./gradlew --stop then retry. Old daemons can hold resources.",
                "4. Dependency conflicts: check ./gradlew dependencies | grep 'CONFLICT'.",
                "5. Clean build: ./gradlew clean build — when incremental compilation acts weird.",
                "6. Always use compileJava first (fast) before full bootRun (slow) to check for errors."
            ),
            0.95,
            Map.of("source", "engineering-excellence")
        );

        learningService.recordPattern(
            "BUILD_PATTERNS",
            "Compile-first development cycle",
            "Before running the full app (bootRun), always compile first (compileJava). " +
            "compileJava takes ~10s vs bootRun takes ~30s+. If compile fails, you save 20+ seconds per iteration. " +
            "Cycle: edit → compileJava → fix errors → compileJava → bootRun."
        );
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 10. GIT WORKFLOW
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private void seedGitWorkflowExcellence() {
        learningService.recordTechnique(
            "GIT_OPERATIONS",
            "Safe Git Sync Workflow",
            "Always sync before making changes to avoid merge conflicts.",
            Arrays.asList(
                "1. git status — check for uncommitted changes.",
                "2. git stash — save local changes if any.",
                "3. git pull origin main — get latest remote changes.",
                "4. git stash pop — restore your local changes.",
                "5. Resolve conflicts if any.",
                "6. git add -A → git commit → git push origin main."
            ),
            0.97,
            Map.of("source", "engineering-excellence")
        );

        learningService.recordTechnique(
            "GIT_OPERATIONS",
            "Meaningful Commit Messages",
            "Commit messages should explain WHAT changed and WHY, not HOW.",
            Arrays.asList(
                "1. Format: 'type: short summary' — types: feat, fix, refactor, docs, chore.",
                "2. Body: list the key changes, one per line with dash prefix.",
                "3. Include affected components: 'fix: persistent storage for 9 services'.",
                "4. Include context: why the change was needed.",
                "5. Don't commit generated files, build outputs, or secrets.",
                "6. Group related changes in one commit — don't split a single fix across commits."
            ),
            0.93,
            Map.of("source", "engineering-excellence")
        );

        learningService.recordPattern(
            "GIT_OPERATIONS",
            "Always compile before committing",
            "Never commit code that doesn't compile. Run compileJava (or equivalent) before every git commit. " +
            "A broken commit wastes time for everyone who pulls it. " +
            "If using CI, a failing build also blocks the entire pipeline."
        );
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 11. CROSS-FILE CONSISTENCY
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private void seedCrossFileConsistency() {
        learningService.recordTechnique(
            "CODE_ARCHITECTURE",
            "Pattern Consistency Across Services",
            "When applying a pattern (persistence, error handling, logging), use the EXACT same approach in every service. " +
            "Inconsistency creates maintenance nightmares.",
            Arrays.asList(
                "1. Define the pattern once: which fields, which methods, which naming convention.",
                "2. Apply it identically across all services that need it.",
                "3. Same field names: STORE_PATH, same method names: persist(), restore().",
                "4. Same error handling: try-catch with logger.warn for non-critical, logger.error for critical.",
                "5. Same @PostConstruct pattern for loading persisted data on startup.",
                "6. When adding a new service later, copy the pattern exactly from an existing service."
            ),
            0.97,
            Map.of("source", "engineering-excellence", "priority", "high")
        );

        learningService.recordPattern(
            "CODE_ARCHITECTURE",
            "Grep the codebase after changing any shared interface",
            "When you change a method signature, constructor, or interface, GREP the entire codebase for all callers. " +
            "Every caller must be updated. Missing even one causes a compile error. " +
            "Use: grep -rn 'MethodName' src/ to find all references."
        );
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 12. SPRING BOOT MASTERY
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private void seedSpringBootMastery() {
        learningService.recordTechnique(
            "SPRING_BOOT",
            "Spring Bean Lifecycle Understanding",
            "Know when each initialization method runs to avoid ordering bugs.",
            Arrays.asList(
                "1. Constructor → called first, dependencies not yet injected.",
                "2. @Autowired fields → injected after constructor.",
                "3. @PostConstruct → called after all dependencies injected. SAFE to use @Autowired fields here.",
                "4. @EventListener(ApplicationReadyEvent) → called after ENTIRE context is ready. Safest for cross-service init.",
                "5. @Scheduled → starts after context ready, runs on timer.",
                "6. Rule: Use @PostConstruct for single-service init, ApplicationReadyEvent for cross-service init."
            ),
            0.96,
            Map.of("source", "engineering-excellence", "priority", "high")
        );

        learningService.recordTechnique(
            "SPRING_BOOT",
            "Service Layer Architecture",
            "Proper layering: Controller → Service → Repository/External. Each layer has a clear responsibility.",
            Arrays.asList(
                "1. Controller: HTTP handling, input validation, response formatting. NO business logic.",
                "2. Service: business logic, orchestration, transaction management. NO HTTP concerns.",
                "3. Repository/External: data access, external API calls. NO business rules.",
                "4. Models: data carriers, validation annotations. NO logic.",
                "5. Config: @Bean definitions, @Configuration. NO runtime logic.",
                "6. Controller never calls Repository directly — always through Service."
            ),
            0.95,
            Map.of("source", "engineering-excellence")
        );

        learningService.recordTechnique(
            "SPRING_BOOT",
            "Thread Safety in Singleton Beans",
            "All @Service and @Component beans are SINGLETON by default — one instance shared across all requests. " +
            "Mutable state must be thread-safe.",
            Arrays.asList(
                "1. Use ConcurrentHashMap, not HashMap, for shared mutable maps.",
                "2. Use AtomicLong/AtomicInteger, not long/int, for shared counters.",
                "3. Use volatile for fields read by multiple threads (e.g., config values).",
                "4. Use synchronized blocks for compound check-then-act operations.",
                "5. Stateless services are the safest — no fields to worry about.",
                "6. For collections: Collections.synchronizedList() or ConcurrentLinkedQueue."
            ),
            0.97,
            Map.of("source", "engineering-excellence", "priority", "high")
        );

        learningService.recordPattern(
            "SPRING_BOOT",
            "Prefer @Autowired over manual new for Spring-managed beans",
            "Never use 'new MyService()' for classes managed by Spring. They won't get their @Autowired dependencies injected. " +
            "Always use @Autowired, constructor injection, or @Bean factory methods. " +
            "Exception: when creating objects in non-Spring contexts (tests, manual orchestration)."
        );
    }
}
