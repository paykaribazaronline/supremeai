package com.supremeai.service;

import com.supremeai.model.SystemLearning;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class KnowledgeSeedDataProvider {

    public Flux<SystemLearning> provideAllSeeds() {
        return Flux.merge(
                seedCorePlans(),
                seedErrorSolutions(),
                seedAiPatterns(),
                seedBestPractices(),
                seedLifecyclePolicies(),
                seedRuntimeDiagnostics(),
                seedSpringBootErrorPatterns(),
                seedFrontendErrorPatterns(),
                seedJvmAndGcPatterns(),
                seedDatabaseErrorPatterns(),
                seedCloudAndDeploymentErrors(),
                seedAiProviderManagement(),
                seedUserAndPermissionManagement(),
                seedLocalAiModelSetup(),
                seedBrowserAutomationPatterns());
    }

    private SystemLearning makeLearning(
            String id,
            String title,
            String category,
            String content,
            List<String> tags,
            boolean isCritical,
            double confidence) {
        SystemLearning learning = new SystemLearning();
        learning.setId(id);
        learning.setTitle(title);
        learning.setCategory(category);
        learning.setContent(content);
        learning.setTags(tags);
        learning.setCritical(isCritical);
        learning.setConfidence(confidence);
        learning.setVersion(1L);
        learning.setCreatedAt(LocalDateTime.now());
        learning.setUpdatedAt(LocalDateTime.now());
        return learning;
    }

    private SystemLearning makeErrorSolution(
            String id, String title, String content, List<String> tags) {
        return makeLearning(id, title, "error-solutions", content, tags, true, 0.90);
    }

    private Flux<SystemLearning> seedCorePlans() {
        return Flux.fromIterable(
                List.of(
                        // ── Offline Operation ──────────────────────────────────────────────
                        makeLearning(
                                "lp-offline-01",
                                "Complete Offline Operation Plan",
                                "operations",
                                "[LOCAL-SEED]\n"
                                        + "1. Disable all external AI provider calls\n"
                                        + "2. Load relevant entries from core_knowledge.json\n"
                                        + "3. Fall back to local-seed templates for common tasks\n"
                                        + "4. Buffer all new observations for next online session\n"
                                        + "5. Notify admin of degraded mode via dashboard alert",
                                List.of("offline", "local-seed", "zero-ai", "degradation"),
                                false,
                                0.92),
                        makeLearning(
                                "lp-offline-02",
                                "Offline Greeting and Basic Interaction",
                                "operations",
                                "[LOCAL-SEED]\n"
                                        + "When AI is unavailable, respond to greetings and simple questions\n"
                                        + "from core_knowledge.json templates. Do not attempt API calls.",
                                List.of("greeting", "offline", "basic-interaction"),
                                false,
                                0.90),
                        makeLearning(
                                "lp-offline-03",
                                "Offline System Status Response",
                                "operations",
                                "[LOCAL-SEED]\n"
                                        + "Report system status from cached health data. Do not attempt to\n"
                                        + "fetch live metrics. Flag as stale if last update > 30 minutes.",
                                List.of("status", "health", "offline", "cached"),
                                false,
                                0.90),
                        makeLearning(
                                "lp-offline-04",
                                "Offline Build Pipeline Recovery",
                                "operations",
                                "[LOCAL-SEED]\n"
                                        + "1. Display last-known build status from Firestore build_logs\n"
                                        + "2. Guide user to run 'mvn clean verify' or 'npm run build' manually\n"
                                        + "3. Detect common Maven/Gradle dependency conflicts\n"
                                        + "4. Suggest clearing local gradle/maven cache (~/.m2 or ~/.gradle)",
                                List.of("build", "compile", "offline", "recovery"),
                                false,
                                0.91),
                        makeLearning(
                                "lp-offline-05",
                                "Offline Database Troubleshooting",
                                "operations",
                                "[LOCAL-SEED]\n"
                                        + "1. Check JDBC connection string in local .env (do not log full string)\n"
                                        + "2. Verify Firestore emulator is running for local dev\n"
                                        + "3. Check DB pool exhaustion: 'SHOW max_connections'\n"
                                        + "4. Run 'FLUSH PRIVILEGES' only after admin confirmation",
                                List.of("database", "sql", "troubleshooting", "offline"),
                                false,
                                0.90),
                        makeLearning(
                                "lp-offline-06",
                                "Offline Network Troubleshooting",
                                "operations",
                                "[LOCAL-SEED]\n"
                                        + "1. 'nslookup <host>' for DNS check\n"
                                        + "2. 'curl -v <host>' for HTTP reachability\n"
                                        + "3. 'ping <host>' for ICMP reachability\n"
                                        + "4. Check proxy: env | grep -i proxy\n"
                                        + "5. Check firewall: sudo iptables -L -n",
                                List.of("network", "dns", "troubleshooting", "offline"),
                                false,
                                0.90),
                        makeLearning(
                                "lp-offline-07",
                                "Offline Security Incident Response",
                                "operations",
                                "[LOCAL-SEED]\n"
                                        + "1. Rotate exposed credentials immediately (new JWT_SECRET)\n"
                                        + "2. Revoke all active sessions via Firestore invalidation\n"
                                        + "3. Force password reset for affected accounts\n"
                                        + "4. Capture evidence and append to security-log collection\n"
                                        + "5. Escalate to admin for external notification",
                                List.of("security", "incident", "response", "offline"),
                                true,
                                0.91),
                        makeLearning(
                                "lp-offline-08",
                                "Offline SSL Certificate Renewal Guide",
                                "operations",
                                "[LOCAL-SEED]\n"
                                        + "1. Check expiry: openssl x509 -in cert.pem -noout -dates\n"
                                        + "2. Renew with Let's Encrypt: certbot certonly --standalone\n"
                                        + "3. Import into Java truststore: keytool -importcert\n"
                                        + "4. Restart affected services",
                                List.of("ssl", "certificate", "offline", "renewal"),
                                false,
                                0.90),
                        makeLearning(
                                "lp-offline-09",
                                "Offline Rate Limit Recovery Steps",
                                "operations",
                                "[LOCAL-SEED]\n"
                                        + "1. Identify the quota-exceeded service from logs\n"
                                        + "2. Apply exponential backoff: wait 2^n seconds before retry\n"
                                        + "3. Cache frequent responses in Redis/LocalCache\n"
                                        + "4. Queue requests for later batch processing\n"
                                        + "5. If all providers exhausted → activate kill-switch restart",
                                List.of("rate-limit", "quota", "backoff", "offline"),
                                false,
                                0.90),
                        makeLearning(
                                "lp-offline-10",
                                "Complete AI Blackout — Kill Switch Procedure",
                                "operations",
                                "[LOCAL-SEED]\n"
                                        + "1. All external AI providers failed\n"
                                        + "2. Set AI_PROVIDER_PRIORITY to NONE in local config\n"
                                        + "3. Switch all handlers to use LOCAL_SEED fallback\n"
                                        + "4. Notify admin dashboard of blackout state\n"
                                        + "5. Schedule weekly cron job to test provider connectivity\n"
                                        + "6. Do NOT restart system without manual admin approval",
                                List.of("blackout", "kill-switch", "ai-down", "offline", "thunder-mode"),
                                true,
                                0.95),
                        // ── Knowledge Bootstrap from Zero ─────────────────────────────────
                        makeLearning(
                                "lp-bootstrap-01",
                                "Knowledge Bootstrap — Reconstruct from Firestore",
                                "recovery",
                                "[LOCAL-SEED]\n"
                                        + "1. Detect knowledge base corruption or emptiness\n"
                                        + "2. Query Firestore system_learning and core_knowledge_json\n"
                                        + "3. Rebuild knowledgeCache from Firestore snapshot\n"
                                        + "4. If Firestore also empty → fall back to autonomous_seed_knowledge.json\n"
                                        + "5. Restart agents with fresh knowledge context",
                                List.of("bootstrap", "zero-knowledge", "reconstruction", "firestore"),
                                false,
                                0.91),
                        makeLearning(
                                "lp-bootstrap-02",
                                "Knowledge Bootstrap — Autonomous Seed Fallback",
                                "recovery",
                                "[LOCAL-SEED]\n"
                                        + "If both Firestore and core_knowledge.json are unavailable,\n"
                                        + "load the last-known autonomous_seed_knowledge.json snapshot\n"
                                        + "(created by weekly cron job). Return degraded but functional.",
                                List.of("bootstrap", "autonomous-seed", "emergency", "recovery"),
                                false,
                                0.90),
                        makeLearning(
                                "lp-bootstrap-03",
                                "Knowledge Bootstrap — Weekly Cron Rebuild",
                                "recovery",
                                "Configure cron job to run every Sunday 02:00 UTC:\n"
                                        + "  curl -X POST https://api.supremeai/system/rebuild-seed\n"
                                        + "Script creates autonomous_seed_knowledge.json from current\n"
                                        + "Firestore snapshot. Enables zero-knowledge recovery.",
                                List.of("bootstrap", "cron", "scheduled", "seed-rebuild"),
                                false,
                                0.90),
                        makeLearning(
                                "lp-bootstrap-04",
                                "Offline User-Driven Learning",
                                "recovery",
                                "[LOCAL-SEED]\n"
                                        + "When AI is unavailable, extract knowledge from every user interaction.\n"
                                        + "1. Log the full user prompt + applied solution\n"
                                        + "2. Build system_learning entry from the pattern\n"
                                        + "3. Store in session-local memory; DM to Firestore next online\n"
                                        + "4. Flag for admin review if confidence > 0.9",
                                List.of("user-learning", "interaction", "knowledge-extraction", "offline"),
                                false,
                                0.88),
                        makeLearning(
                                "lp-bootstrap-05",
                                "Offline Template-Based Code Generation",
                                "recovery",
                                "[LOCAL-SEED]\n"
                                        + "Without AI: use curated templates stored in core_knowledge.json.\n"
                                        + "Templates cover Spring Boot controllers, DTOs, Firestore entities,\n"
                                        + "and React components. Fill template {{PLACEHOLDER}} tokens from request.",
                                List.of("template", "code-generation", "offline", "local-seed"),
                                false,
                                0.88),
                        // ── P2P Knowledge Sync ────────────────────────────────────────────
                        makeLearning(
                                "lp-p2p-01",
                                "P2P Knowledge Sync — Peer Exchange Protocol",
                                "operations",
                                "[LOCAL-SEED]\n"
                                        + "When cloud is unreachable, sync knowledge from local peers.\n"
                                        + "1. Discover peers on same LAN via mDNS / Zeroconf\n"
                                        + "2. Request knowledge diff (last changed timestamp)\n"
                                        + "3. Pull missing or updated entries via gRPC/serialized JSON\n"
                                        + "4. Validate checksums before applying local knowledge",
                                List.of("p2p", "knowledge-sync", "lan", "distributed", "recovery"),
                                false,
                                0.89),
                        makeLearning(
                                "lp-p2p-02",
                                "P2P Knowledge Sync — Conflict Resolution",
                                "operations",
                                "[LOCAL-SEED]\n"
                                        + "When two peers provide conflicting knowledge:\n"
                                        + "1. Use highest confidenceScore as tie-breaker\n"
                                        + "2. If scores equal, prefer most recent learnedAt\n"
                                        + "3. Log conflict in system_learning metadata for admin review\n"
                                        + "4. Never silently overwrite higher-confidence knowledge",
                                List.of("p2p", "conflict-resolution", "merge", "distributed"),
                                false,
                                0.88),
                        makeLearning(
                                "lp-p2p-03",
                                "P2P Knowledge Sync — Audit Trail",
                                "operations",
                                "[LOCAL-SEED]\n"
                                        + "Maintain immutable audit log of all knowledge changes:\n"
                                        + "- entryId, peerId, action (added/updated/removed), timestamp\n"
                                        + "- human-readable diff of what changed\n"
                                        + "- SHA-256 hash of entry for tamper detection\n"
                                        + "Store audit trail in Firestore knowledge_audit collection.",
                                List.of("p2p", "audit-trail", "integrity", "knowledge-sync"),
                                false,
                                0.87)));
    }

    // ─── Additional Core Knowledge Seeders ─────────────────────────────────────

    private Flux<SystemLearning> seedErrorSolutions() {
        return Flux.fromIterable(
                List.of(

                        // ── How to FIND errors ──────────────────────────────────────────
                        makeErrorSolution(
                                "es-find-01",
                                "Error Finding — Log File Location Guide",
                                "HOW TO FIND ERRORS:\n"
                                        + "1. Spring Boot logs: ./logs/application.log OR console output\n"
                                        + "2. Cloud Run logs: gcloud run services logs read supremeai-backend --region=us-central1\n"
                                        + "3. Firebase Functions: firebase functions:log\n"
                                        + "4. Firestore errors: GCP Console → Firestore → Usage tab\n"
                                        + "5. Frontend: Browser DevTools → Console tab (F12)\n"
                                        + "6. React build errors: terminal where 'npm run dev' is running\n"
                                        + "7. Test failures: ./gradlew test → build/reports/tests/test/index.html\n"
                                        + "8. Compile errors: ./gradlew compileJava 2>&1 | grep 'error:'\n"
                                        + "KEYWORDS TO SEARCH: 'ERROR', 'Exception', 'WARN', 'FATAL', 'failed'\n"
                                        + "COMMAND: grep -n 'ERROR\\|Exception' logs/application.log | tail -50",
                                List.of("find-error", "log-location", "error-detection", "diagnosis")),
                        makeErrorSolution(
                                "es-find-02",
                                "Error Location — Stack Trace Reading Guide",
                                "HOW TO READ A STACK TRACE:\n"
                                        + "Example stack trace:\n"
                                        + "  java.lang.NullPointerException: Cannot invoke method on null\n"
                                        + "    at com.supremeai.service.ChatService.process(ChatService.java:142)  ← ERROR LINE\n"
                                        + "    at com.supremeai.controller.ChatController.chat(ChatController.java:67)\n"
                                        + "    at ...\n"
                                        + "READING RULES:\n"
                                        + "  Line 1 = Exception type + short message\n"
                                        + "  Line 2 (first 'at') = EXACT file and line number of the error ← START HERE\n"
                                        + "  FileName.java:142 means open file, go to line 142\n"
                                        + "  Lines below = call chain (how we got there)\n"
                                        + "ACTION: Open the file mentioned in first 'at' line, check that line number.",
                                List.of("stack-trace", "error-location", "line-number", "reading")),
                        makeErrorSolution(
                                "es-find-03",
                                "Error Finding — Common Log Patterns",
                                "COMMON ERROR SIGNATURES IN LOGS:\n"
                                        + "  [ERROR] c.s.service.X - ... → Spring service error\n"
                                        + "  WARN  [main] o.s.b.w. ... → Spring Boot startup warning\n"
                                        + "  Caused by: → root cause (read this first, not the wrapping exception)\n"
                                        + "  Status 500 → backend threw uncaught exception\n"
                                        + "  Status 401 → authentication failed (JWT expired or missing)\n"
                                        + "  Status 403 → authorization failed (wrong role)\n"
                                        + "  Status 400 → bad request body (validation error)\n"
                                        + "  GREP TIPS:\n"
                                        + "  grep -A 20 'Exception' logs/application.log   # show 20 lines after match\n"
                                        + "  grep -B 5 'ERROR' logs/application.log         # show 5 lines before match\n"
                                        + "  grep 'ERROR' logs/*.log | tail -100             # last 100 errors across logs",
                                List.of("log-patterns", "grep", "error-signature", "http-status")),

                        // ── NullPointerException ──────────────────────────────────────
                        makeErrorSolution(
                                "es-npe-01",
                                "NullPointerException — Find, Locate, Solve",
                                "ERROR: java.lang.NullPointerException\n"
                                        + "HOW TO FIND: grep -n 'NullPointerException' logs/application.log\n"
                                        + "HOW TO LOCATE: Read first 'at com.supremeai...' line → open that file:line\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Calling method on object that was not initialized\n"
                                        + "  - Firestore document field missing → getField() returns null\n"
                                        + "  - Spring @Autowired bean not injected (missing @Component)\n"
                                        + "  - Optional.get() without isPresent() check\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Add null check: if (obj != null) { obj.method(); }\n"
                                        + "  2. Use Optional: Optional.ofNullable(obj).ifPresent(o -> o.method())\n"
                                        + "  3. Use Objects.requireNonNull(obj, 'obj must not be null')\n"
                                        + "  4. For Firestore: check document.exists() before getData()\n"
                                        + "  5. Enable Java 14+ helpful NPE: -XX:+ShowCodeDetailsInExceptionMessages",
                                List.of("npe", "null-pointer", "nullpointerexception", "null-check")),

                        // ── AI Provider Timeout ───────────────────────────────────────
                        makeErrorSolution(
                                "es-timeout-01",
                                "AI Provider Timeout — Find, Locate, Solve",
                                "ERROR: ReadTimeoutException / SocketTimeoutException / 504 Gateway Timeout\n"
                                        + "HOW TO FIND: grep -n 'TimeoutException\\|504\\|Read timed out' logs/application.log\n"
                                        + "HOW TO LOCATE: First 'at' line in stack trace → AIProviderService or WebClient call\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - SupremeAI Logic Provider slow response > 30s\n"
                                        + "  - Network latency or packet loss\n"
                                        + "  - Provider rate-limited and holding connection open\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Auto-switch: ProviderAdminService → rotate to next priority provider\n"
                                        + "  2. Set WebClient timeout: .responseTimeout(Duration.ofSeconds(25))\n"
                                        + "  3. If all providers fail → activate LOCAL_SEED fallback\n"
                                        + "  4. Check node status in Admin Dashboard\n"
                                        + "  5. Log error_signature for RCA: hash(exceptionClass+method+line)",
                                List.of("timeout", "provider", "fallback", "resilience", "504")),

                        // ── Rate Limit ────────────────────────────────────────────────
                        makeErrorSolution(
                                "es-rate-limit-01",
                                "Rate Limit 429 — Find, Locate, Solve",
                                "ERROR: HTTP 429 Too Many Requests\n"
                                        + "HOW TO FIND: grep -n '429\\|rate.limit\\|quota' logs/application.log\n"
                                        + "HOW TO LOCATE: AIProviderService.java → provider call returning 429\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - API quota exceeded (daily/minute limit)\n"
                                        + "  - Too many concurrent requests to same provider\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Exponential backoff: wait 2^n seconds (1s, 2s, 4s, 8s, cap=600s)\n"
                                        + "  2. Rotate to next provider: ProviderAdminService.rotatePriority()\n"
                                        + "  3. Cache frequent responses: Redis or ConcurrentHashMap\n"
                                        + "  4. Queue requests: use Reactor Flux.delayElements(Duration.ofMillis(200))\n"
                                        + "  5. Check quota dashboard: GCP Console → APIs & Services → Quotas",
                                List.of("rate-limit", "429", "backoff", "quota", "too-many-requests")),

                        // ── HTTP 500 ──────────────────────────────────────────────────
                        makeErrorSolution(
                                "es-http500-01",
                                "HTTP 500 Internal Server Error — Find, Locate, Solve",
                                "ERROR: HTTP 500 / WhiteLabel Error Page / 'No message available'\n"
                                        + "HOW TO FIND: grep -n '500\\|Internal Server Error\\|ERROR.*Controller' logs/application.log\n"
                                        + "HOW TO LOCATE:\n"
                                        + "  - Open logs → find timestamp of 500 → read 30 lines below\n"
                                        + "  - Look for 'Caused by:' → that is the real error\n"
                                        + "  - First 'at com.supremeai' line = your code's fault line\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Unhandled exception in @RestController method\n"
                                        + "  - Service returning null where ResponseEntity expected\n"
                                        + "  - Firestore timeout during request handling\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Add @ExceptionHandler(Exception.class) in @ControllerAdvice\n"
                                        + "  2. Wrap service calls in try-catch → return ResponseEntity.status(500)\n"
                                        + "  3. Check if @Autowired dependency is null (missing @Bean)\n"
                                        + "  4. Add logging: log.error('Failed: {}', e.getMessage(), e)",
                                List.of(
                                        "http-500", "internal-server-error", "controller-advice", "exception-handler")),

                        // ── ClassNotFoundException ────────────────────────────────────
                        makeErrorSolution(
                                "es-cnf-01",
                                "ClassNotFoundException / NoClassDefFoundError — Find, Locate, Solve",
                                "ERROR: ClassNotFoundException / NoClassDefFoundError\n"
                                        + "HOW TO FIND: grep -n 'ClassNotFoundException\\|NoClassDefFound' logs/application.log\n"
                                        + "HOW TO LOCATE: Stack trace → which class is missing\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Missing dependency in build.gradle or pom.xml\n"
                                        + "  - Dependency version conflict (two versions of same library)\n"
                                        + "  - JAR not included in final fat-jar (build issue)\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Add missing dependency to build.gradle: implementation 'group:artifact:version'\n"
                                        + "  2. Run: ./gradlew dependencies | grep <className> to find correct group\n"
                                        + "  3. Check conflicts: ./gradlew dependencyInsight --dependency <name>\n"
                                        + "  4. Rebuild: ./gradlew clean build\n"
                                        + "  5. For Spring beans: verify @Component/@Service annotation is present",
                                List.of("classnotfound", "dependency", "gradle", "missing-class", "build")),

                        // ── BeanCreationException ─────────────────────────────────────
                        makeErrorSolution(
                                "es-bean-01",
                                "Spring BeanCreationException — Find, Locate, Solve",
                                "ERROR: BeanCreationException / UnsatisfiedDependencyException\n"
                                        + "HOW TO FIND: grep -n 'BeanCreationException\\|UnsatisfiedDependency' logs/application.log\n"
                                        + "HOW TO LOCATE: Error message shows 'Error creating bean with name X'\n"
                                        + "  → X is the bean class name → open that file\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - @Autowired field type not found as a Spring bean\n"
                                        + "  - Circular dependency (A needs B, B needs A)\n"
                                        + "  - Missing @Service/@Component annotation\n"
                                        + "  - @Value('${property}') property key not found in application.properties\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Check that injected class has @Service/@Component/@Repository\n"
                                        + "  2. For circular: add @Lazy on one @Autowired field\n"
                                        + "  3. For missing property: add key to application.properties or .env\n"
                                        + "  4. Run: ./gradlew bootRun 2>&1 | grep 'BeanCreation' for full context",
                                List.of(
                                        "bean-creation",
                                        "spring",
                                        "autowired",
                                        "circular-dependency",
                                        "unsatisfied-dependency")),

                        // ── Firestore Errors ──────────────────────────────────────────
                        makeErrorSolution(
                                "es-firestore-01",
                                "Firestore UNAVAILABLE / DEADLINE_EXCEEDED — Find, Locate, Solve",
                                "ERROR: io.grpc.StatusRuntimeException: UNAVAILABLE / DEADLINE_EXCEEDED\n"
                                        + "HOW TO FIND: grep -n 'UNAVAILABLE\\|DEADLINE_EXCEEDED\\|StatusRuntimeException' logs/application.log\n"
                                        + "HOW TO LOCATE: Stack trace → FirestoreService or Repository class\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Firestore emulator not running (local dev)\n"
                                        + "  - GCP service account credentials missing or expired\n"
                                        + "  - Network timeout on Firestore gRPC channel\n"
                                        + "  - Reading large collections without pagination\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Local: firebase emulators:start --only firestore\n"
                                        + "  2. Check env: GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json\n"
                                        + "  3. Add retry: .withRetry(Retry.backoff(3, Duration.ofSeconds(1)))\n"
                                        + "  4. Paginate: collection.limit(100).get() instead of getAll()\n"
                                        + "  5. Check GCP Console → Firestore → Usage for quota issues",
                                List.of("firestore", "grpc", "unavailable", "deadline-exceeded", "emulator")),

                        // ── CORS Error ────────────────────────────────────────────────
                        makeErrorSolution(
                                "es-cors-01",
                                "CORS Error — Find, Locate, Solve",
                                "ERROR: Access to fetch blocked by CORS policy / No 'Access-Control-Allow-Origin' header\n"
                                        + "HOW TO FIND: Browser DevTools → Console → 'CORS' or 'blocked' messages\n"
                                        + "HOW TO LOCATE: SecurityConfig.java → corsConfigurationSource() method\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Frontend origin not in cors.allowed-origins whitelist\n"
                                        + "  - Missing OPTIONS pre-flight handling\n"
                                        + "  - Wildcard (*) used with credentials (incompatible)\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Add your origin to application.properties:\n"
                                        + "     cors.allowed-origins=http://localhost:5173,https://supremeai-a.web.app\n"
                                        + "  2. Ensure setAllowCredentials(true) + specific origins (not *)\n"
                                        + "  3. Restart backend after config change\n"
                                        + "  4. Test: curl -H 'Origin: http://localhost:5173' -I http://localhost:8080/api/health",
                                List.of("cors", "cross-origin", "access-control", "preflight", "frontend")),

                        // ── JWT Auth Error ────────────────────────────────────────────
                        makeErrorSolution(
                                "es-jwt-01",
                                "JWT 401 Unauthorized — Find, Locate, Solve",
                                "ERROR: HTTP 401 Unauthorized / JWT expired / Invalid token\n"
                                        + "HOW TO FIND: grep -n '401\\|JWT\\|token.*invalid\\|expired' logs/application.log\n"
                                        + "HOW TO LOCATE: JwtAuthFilter.java or AuthenticationFilter.java\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - JWT token expired (default: 24h)\n"
                                        + "  - JWT_SECRET changed but old tokens still in use\n"
                                        + "  - Frontend not sending Authorization: Bearer <token> header\n"
                                        + "  - Firebase ID token not verified correctly\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Frontend: check localStorage.getItem('authToken') exists\n"
                                        + "  2. Decode JWT at jwt.io to check expiry (exp field)\n"
                                        + "  3. Re-login to get fresh token\n"
                                        + "  4. Check: request header has 'Authorization: Bearer eyJ...'\n"
                                        + "  5. Server: verify JWT_SECRET in .env matches what was used to sign token",
                                List.of("jwt", "401", "unauthorized", "token-expired", "authentication")),

                        // ── DNS Error ─────────────────────────────────────────────────
                        makeErrorSolution(
                                "es-dns-01",
                                "DNS Resolution Failure — Find, Locate, Solve",
                                "ERROR: UnknownHostException / Could not resolve host\n"
                                        + "HOW TO FIND: grep -n 'UnknownHostException\\|could not resolve\\|DNS' logs/application.log\n"
                                        + "HOW TO LOCATE: Stack trace → WebClient or RestTemplate call line\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Test DNS: nslookup <hostname>  or  dig <hostname>\n"
                                        + "  2. Try alternate DNS: echo 'nameserver 8.8.8.8' | sudo tee /etc/resolv.conf\n"
                                        + "  3. Check /etc/hosts for conflicting override\n"
                                        + "  4. Check proxy: env | grep -i proxy\n"
                                        + "  5. Cloud Run: verify VPC connector if using private DNS",
                                List.of("dns", "unknown-host", "network", "resolution")),

                        // ── Flyway ────────────────────────────────────────────────────
                        makeErrorSolution(
                                "es-migration-01",
                                "Flyway Migration Failure — Find, Locate, Solve",
                                "ERROR: FlywayException / Migration checksum mismatch / Table already exists\n"
                                        + "HOW TO FIND: grep -n 'FlywayException\\|migration.*failed\\|checksum' logs/application.log\n"
                                        + "HOW TO LOCATE: src/main/resources/db/migration/V{version}__*.sql\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Edited an already-applied migration file (checksum changed)\n"
                                        + "  - Migration ran partially (table half-created)\n"
                                        + "  - Wrong migration version ordering\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. NEVER edit applied migrations — create a new V{n+1} file instead\n"
                                        + "  2. If checksum mismatch: flyway repair (only after DB backup!)\n"
                                        + "  3. If table exists: add IF NOT EXISTS to CREATE TABLE statement\n"
                                        + "  4. Check flyway_schema_history table for applied versions\n"
                                        + "  5. Rollback strategy: restore DB snapshot, then rerun migrations",
                                List.of("flyway", "migration", "database", "schema", "checksum")),

                        // ── OOM ───────────────────────────────────────────────────────
                        makeErrorSolution(
                                "es-oom-01",
                                "OutOfMemoryError — Find, Locate, Solve",
                                "ERROR: java.lang.OutOfMemoryError: Java heap space / GC overhead limit exceeded\n"
                                        + "HOW TO FIND: grep -n 'OutOfMemoryError\\|GC overhead\\|heap space' logs/application.log\n"
                                        + "HOW TO LOCATE: Heap dump analysis or thread dump (jstack <pid>)\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Loading entire Firestore collection into memory (no pagination)\n"
                                        + "  - Memory leak: ConcurrentHashMap growing without eviction\n"
                                        + "  - Infinite loop building large strings\n"
                                        + "  - Too many concurrent threads holding data\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Increase heap: java -Xmx2g -jar app.jar\n"
                                        + "  2. Cloud Run: set --memory=2Gi in deploy command\n"
                                        + "  3. Paginate Firestore: collection.limit(100) in loop\n"
                                        + "  4. Add eviction to cache: use Caffeine instead of ConcurrentHashMap\n"
                                        + "  5. Take heap dump: jmap -dump:format=b,file=heap.hprof <pid>\n"
                                        + "  6. Analyze with Eclipse MAT or VisualVM",
                                List.of("oom", "out-of-memory", "heap", "memory-leak", "gc")),

                        // ── Provider Health ───────────────────────────────────────────
                        makeErrorSolution(
                                "es-health-01",
                                "Provider Health Degradation — Find, Locate, Solve",
                                "ERROR: Provider error rate > 20% / Auto-quarantine triggered\n"
                                        + "HOW TO FIND: grep -n 'quarantine\\|DEGRADED\\|error.rate' logs/application.log\n"
                                        + "HOW TO LOCATE: ProviderAdminService.java → health monitoring methods\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Check provider status page directly\n"
                                        + "  2. Admin API: GET /api/admin/providers/health\n"
                                        + "  3. Force rotate: POST /api/admin/providers/rotate-priority\n"
                                        + "  4. Quarantine lifts automatically after 3 consecutive healthy probes\n"
                                        + "  5. If all providers degraded: activate LOCAL_SEED fallback mode",
                                List.of("provider-health", "quarantine", "monitoring", "degradation")),

                        // ── React/Frontend Build Error ────────────────────────────────
                        makeErrorSolution(
                                "es-react-build-01",
                                "React/Vite Build Error — Find, Locate, Solve",
                                "ERROR: Failed to compile / Module not found / TypeScript error\n"
                                        + "HOW TO FIND: Terminal running 'npm run dev' or 'npm run build'\n"
                                        + "HOW TO LOCATE: Error message shows file path + line:column number\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Missing npm package: Module not found\n"
                                        + "  - TypeScript type error: Type X is not assignable to type Y\n"
                                        + "  - Import path wrong (case-sensitive on Linux)\n"
                                        + "  - .env variable not prefixed with VITE_ (Vite requirement)\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Missing module: npm install <package-name>\n"
                                        + "  2. Type error: check TypeScript type definition or add 'as any'\n"
                                        + "  3. .env variables: must be VITE_MY_VAR (not MY_VAR) in Vite projects\n"
                                        + "  4. Clear cache: rm -rf node_modules/.vite && npm run dev\n"
                                        + "  5. Full rebuild: rm -rf node_modules && npm install && npm run build",
                                List.of(
                                        "react", "vite", "typescript", "build-error", "frontend", "module-not-found")),

                        // ── Spring Boot Startup Failure ───────────────────────────────
                        makeErrorSolution(
                                "es-startup-01",
                                "Spring Boot Application Startup Failure — Find, Locate, Solve",
                                "ERROR: Application failed to start / Port already in use / ApplicationContext failed\n"
                                        + "HOW TO FIND: Look at very beginning of startup logs for 'APPLICATION FAILED TO START'\n"
                                        + "HOW TO LOCATE: Log shows which bean or config caused failure\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Port 8080 already in use: 'Web server failed to start. Port 8080 was already in use'\n"
                                        + "  - Missing required property: 'Could not resolve placeholder'\n"
                                        + "  - Database connection failed at startup\n"
                                        + "  - Bean circular dependency\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Port in use: lsof -ti:8080 | xargs kill -9\n"
                                        + "  2. Or change port: server.port=8081 in application.properties\n"
                                        + "  3. Missing property: add to .env or application.properties\n"
                                        + "  4. Circular dependency: add @Lazy to one @Autowired\n"
                                        + "  5. View full startup error: ./gradlew bootRun 2>&1 | head -100",
                                List.of("startup-failure", "port-in-use", "application-context", "spring-boot"))));
    }

    private Flux<SystemLearning> seedAiPatterns() {
        return Flux.fromIterable(
                List.of(
                        makeLearning(
                                "pat-voting-01",
                                "Confidence-Weighted Multi-Model Voting",
                                "ai-patterns",
                                "When 2+ AI providers produce conflicting outputs:\n"
                                        + "1. Compute per-provider confidence scores\n"
                                        + "2. Weight responses by confidence, select winner by score\n"
                                        + "3. If gap < 0.15 between top-2: flag LOW_CONFIDENCE for admin review\n"
                                        + "4. Record final decision + confidence for RCA training",
                                List.of("voting", "multi-model", "confidence", "agreement"),
                                false,
                                0.93),
                        makeLearning(
                                "pat-fallback-01",
                                "Single-Model Double-Pass Consistency Check",
                                "ai-patterns",
                                "When only one AI provider is available:\n"
                                        + "1. Send the prompt twice with varied instruction framing\n"
                                        + "2. If responses agree (Levenshtein similarity > 75%): accept\n"
                                        + "3. If they disagree on key assertions: flag ADMIN_REVIEW\n"
                                        + "4. Never present a disputed single-model answer as confirmed fact",
                                List.of("fallback", "single-model", "double-pass", "consistency"),
                                false,
                                0.90),
                        makeLearning(
                                "pat-cascading-01",
                                "Cascading Failure Prevention — Circuit Breaker",
                                "ai-patterns",
                                "When a shared dependency (DB, Firestore, cache) fails simultaneously:\n"
                                        + "1. Circuit-breaker opens after 5 consecutive failures in 60s\n"
                                        + "2. All requests short-circuit → fallback template\n"
                                        + "3. Half-open probe every 30s; close on single success\n"
                                        + "4. Alert admin immediately on breaker OPEN state",
                                List.of("circuit-breaker", "cascading-failure", "resilience", "prevention"),
                                false,
                                0.91)));
    }

    private Flux<SystemLearning> seedBestPractices() {
        return Flux.fromIterable(
                List.of(
                        makeLearning(
                                "bp-sec-01",
                                "Secrets Management — Never Commit Credentials",
                                "best-practices",
                                "1. .env and .env.local MUST be in .gitignore\n"
                                        + "2. For production: use GCP Secret Manager\n"
                                        + "3. Inject secrets at deploy-time via --set-secrets in Cloud Run\n"
                                        + "4. Rotate JWT_SECRET and database passwords every 90 days\n"
                                        + "5. Audit secret access via GCP audit logs monthly",
                                List.of("secrets", "security", "gitignore", "credential-theft"),
                                false,
                                0.93),
                        makeLearning(
                                "bp-firestore-01",
                                "Firestore Security Rules — Deny by Default",
                                "best-practices",
                                "1. All sensitive collections require isAdmin() check\n"
                                        + "2. Catch-all rule: allow read, write: if false (strict deny default)\n"
                                        + "3. Never use isServiceAccount() for user-facing rules\n"
                                        + "4. Run 'firebase emulators:exec ./mvnw test' before each deploy\n"
                                        + "5. Use 'firebase rules:lint' to catch anti-patterns",
                                List.of("firestore", "security-rules", "deny-by-default", "rules-anti-pattern"),
                                false,
                                0.92),
                        makeLearning(
                                "bp-resilience-01",
                                "Spring Boot Resilience — Retry + Circuit Breaker",
                                "best-practices",
                                "1. Use @Retryable on all remote calls (maxAttempts=3, backoff=2s)\n"
                                        + "2. Use CircuitBreakerFactory for layered circuit breaker\n"
                                        + "3. Spring Cloud LoadBalancer for external provider routing\n"
                                        + "4. Health endpoint ('/api/health') must not depend on any provider\n"
                                        + "5. Log every retry/circuit-breaker event for RCA",
                                List.of("spring-boot", "resilience", "retry", "circuit-breaker", "load-balancer"),
                                false,
                                0.91),
                        makeLearning(
                                "bp-cors-01",
                                "CORS Configuration — Never Use Wildcard Origins in Production",
                                "best-practices",
                                "1. cors.allowed-origins: read from spring.config.import=optional:file:.env\n"
                                        + "2. Whitelist exact domains: https://app.example.com, not '*'\n"
                                        + "3. Match proxy-header strategy when behind reverse proxy (Cloud Run)\n"
                                        + "4. CSRF: exempt only /api/auth/** and /ws/** (static resources OK)\n"
                                        + "5. All admin routes: no CSRF exemption, enforce hasRole('ADMIN')",
                                List.of("cors", "csrf", "security", "spring-security", "origin-whitelist"),
                                false,
                                0.92),
                        makeLearning(
                                "bp-observability-01",
                                "Structured Logging — SLF4J with Logback JSON",
                                "best-practices",
                                "1. Never log plaintext secrets, tokens, or PII\n"
                                        + "2. Include requestId, userId, traceId in every log line\n"
                                        + "3. Use %d{ISO8601} %-5level [%thread] %logger - %msg%n pattern\n"
                                        + "4. ERROR → Sentry; WARN → admin dashboard; INFO → audit trail\n"
                                        + "5. Log volumes and rotation: 500MB/file, 30-day retention",
                                List.of("logging", "slf4j", "logback", "observability", "security"),
                                false,
                                0.90)));
    }

    private Flux<SystemLearning> seedLifecyclePolicies() {
        return Flux.fromIterable(
                List.of(
                        makeLearning(
                                "lc-backup-01",
                                "Firestore Knowledge Base Backup Policy",
                                "lifecycle",
                                "Procedures for maintaining knowledge base durability:\n"
                                        + "1. Daily automated backup via Firestore export (GCS bucket)\n"
                                        + "2. Weekly autonomous_seed_knowledge.json snapshot (cron Sunday 02:00 UTC)\n"
                                        + "3. Monthly full audit trail export (GCS → cloud archive)\n"
                                        + "4. Backup retention: hot=30 days / warm=1 year / cold=7 years\n"
                                        + "5. Validate backup integrity: checksum compare within 4 hours of restore",
                                List.of("backup", "firestore", "lifecycle", "disaster-recovery"),
                                false,
                                0.90),
                        makeLearning(
                                "lc-retention-01",
                                "Knowledge Entry Retention — Confidence and Age Rules",
                                "lifecycle",
                                "When to remove obsolete system_learning entries:\n"
                                        + "1. lastUsed > 90 days AND timesApplied == 0 → flag obsolete\n"
                                        + "2. confidenceScore < 0.10 for 7 consecutive decay cycles → archive\n"
                                        + "3. critical=true entries → never auto-archive; admin review required\n"
                                        + "4. Run applyRecencyDecay() monthly; review flagged entries weekly\n"
                                        + "5. Use knowledge_audit collection to track all removal decisions",
                                List.of("retention", "lifecycle", "obsolete", "confidence-decay"),
                                false,
                                0.89),
                        makeLearning(
                                "lc-upgrade-01",
                                "Stale Knowledge Entry Upgrade Protocol",
                                "lifecycle",
                                "When a core_knowledge.json entry references changed paths or tools:\n"
                                        + "1. flag the entry with STALE_DETECTED in metadata\n"
                                        + "2. Admin reviews within 2 weeks; appends UPDATE_NOTE\n"
                                        + "3. If entry is 365+ days since lastUsed time: escalate to ADMIN_REVIEW\n"
                                        + "4. Never remove an entry without a documented replacement",
                                List.of("stale-entry", "upgrade", "validation", "core-knowledge"),
                                false,
                                0.88)));
    }

    private Flux<SystemLearning> seedBrowserAutomationPatterns() {
        return Flux.fromIterable(
                List.of(
                        makeLearning(
                                "ba-stealth-01",
                                "Browser Stealth & VPN Anti-Tracking",
                                "browser-automation",
                                "[LOCAL-SEED]\n1. When scraping heavily protected sites (Cloudflare, DataDome), activate Stealth Mode.\n2. Pass 'useVpn': true to route the Playwright instance through a random node from the VPN pool.\n3. This prevents IP bans and maintains 100% uptime for autonomous research.",
                                List.of("browser", "stealth", "vpn", "anti-tracking", "playwright"),
                                true,
                                0.98),
                        makeLearning(
                                "ba-hitl-01",
                                "Human-in-the-Loop (HITL) Login Handling",
                                "browser-automation",
                                "[LOCAL-SEED]\n1. If a website prompts for Login or CAPTCHA, Playwright will NOT crash.\n2. The 'humanInTheLoop': true flag pauses the browser session automatically.\n3. User is notified in the Dashboard to manually fill credentials or solve CAPTCHA.\n4. Once resolved, user clicks 'Resume' and scraping continues.",
                                List.of("browser", "human-in-the-loop", "captcha", "login", "pause-resume"),
                                true,
                                0.95),
                        makeLearning(
                                "ba-cookie-01",
                                "Session Cookie Caching",
                                "browser-automation",
                                "[LOCAL-SEED]\n1. Enable 'storeSessionCookies': true for sites requiring authentication.\n2. Once human-in-the-loop login succeeds, the session cookie is saved to Firestore/Local Storage.\n3. Subsequent runs will use the cached cookie to auto-login silently in the background.",
                                List.of("browser", "cookies", "session", "auto-login"),
                                false,
                                0.94)));
    }

    // ── NEW KNOWLEDGE SEED METHODS ────────────────────────────────────────────

    private Flux<SystemLearning> seedRuntimeDiagnostics() {
        return Flux.fromIterable(
                List.of(
                        makeErrorSolution(
                                "rd-deadlock-01",
                                "Thread Deadlock — Find, Locate, Solve",
                                "ERROR: Application hangs / Thread BLOCKED in thread dump\n"
                                        + "HOW TO FIND: jstack <pid> | grep -A 10 'BLOCKED'\n"
                                        + "HOW TO LOCATE: Look for 'waiting to lock <0x...>' cycles\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Two threads acquiring locks in opposite order\n"
                                        + "  - synchronized blocks nested in wrong order\n"
                                        + "  - Reactor Mono.block() called inside reactive pipeline\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Always acquire locks in same order across all threads\n"
                                        + "  2. Use tryLock(timeout) instead of lock()\n"
                                        + "  3. NEVER call .block() inside a Flux/Mono chain\n"
                                        + "  4. Use ReentrantLock with timeout: lock.tryLock(5, TimeUnit.SECONDS)\n"
                                        + "  5. Thread dump: kill -3 <pid>  or  jstack <pid> > thread.dump",
                                List.of("deadlock", "thread", "blocked", "concurrency", "jstack")),
                        makeErrorSolution(
                                "rd-stackoverflow-01",
                                "StackOverflowError — Find, Locate, Solve",
                                "ERROR: java.lang.StackOverflowError\n"
                                        + "HOW TO FIND: grep -n 'StackOverflow' logs/application.log\n"
                                        + "HOW TO LOCATE: Stack trace repeats the same method — that is the infinite recursion\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Recursive method with no base case\n"
                                        + "  - toString() calling itself via Lombok @ToString on bidirectional relationship\n"
                                        + "  - Jackson serialization circular reference\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Add base case to recursive method\n"
                                        + "  2. Lombok: use @ToString(exclude = 'fieldName')\n"
                                        + "  3. Jackson: add @JsonIgnore on back-reference field\n"
                                        + "  4. Convert recursion to iteration with explicit Stack\n"
                                        + "  5. Increase stack size: java -Xss4m (last resort)",
                                List.of("stack-overflow", "recursion", "lombok", "jackson")),
                        makeErrorSolution(
                                "rd-serialization-01",
                                "Serialization / JSON Parse Error — Find, Locate, Solve",
                                "ERROR: JsonParseException / InvalidDefinitionException / MismatchedInputException\n"
                                        + "HOW TO FIND: grep -n 'JsonParseException\\|MismatchedInput\\|cannot deserialize' logs/application.log\n"
                                        + "HOW TO LOCATE: Stack trace → Jackson ObjectMapper or RestController @RequestBody line\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Frontend sending wrong JSON structure\n"
                                        + "  - Missing no-arg constructor on DTO\n"
                                        + "  - Field name mismatch (camelCase vs snake_case)\n"
                                        + "  - Date format mismatch\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Validate JSON: paste in jsonlint.com\n"
                                        + "  2. Add @JsonProperty('field_name') to match frontend field names\n"
                                        + "  3. Add no-arg constructor to DTO class\n"
                                        + "  4. Configure: spring.jackson.property-naming-strategy=SNAKE_CASE\n"
                                        + "  5. Date: use @JsonFormat(pattern='yyyy-MM-dd HH:mm:ss')",
                                List.of("json", "jackson", "serialization", "dto", "parse-error")),
                        makeErrorSolution(
                                "rd-connection-pool-01",
                                "Connection Pool Exhausted — Find, Locate, Solve",
                                "ERROR: Connection pool timeout / Unable to acquire connection\n"
                                        + "HOW TO FIND: grep -n 'pool.*timeout\\|Unable to acquire' logs/application.log\n"
                                        + "HOW TO LOCATE: DataSource config or R2DBC pool configuration\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Pool size too small for concurrent requests\n"
                                        + "  - Connections not being released (missing .close() or try-with-resources)\n"
                                        + "  - Long-running transactions holding connections\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Increase pool: spring.r2dbc.pool.max-size=50\n"
                                        + "  2. Set timeout: spring.r2dbc.pool.max-acquire-time=10s\n"
                                        + "  3. Always close connections: use try-with-resources\n"
                                        + "  4. Add connection validation: spring.r2dbc.pool.validation-query=SELECT 1\n"
                                        + "  5. Monitor: /actuator/metrics/r2dbc.pool.acquired",
                                List.of("connection-pool", "r2dbc", "database", "timeout", "exhausted"))));
    }

    private Flux<SystemLearning> seedSpringBootErrorPatterns() {
        return Flux.fromIterable(
                List.of(
                        makeErrorSolution(
                                "sb-validation-01",
                                "Spring @Valid Validation Failure — Find, Locate, Solve",
                                "ERROR: MethodArgumentNotValidException / ConstraintViolationException\n"
                                        + "HOW TO FIND: grep -n 'MethodArgumentNotValid\\|ConstraintViolation' logs/application.log\n"
                                        + "HOW TO LOCATE: @RestController method with @Valid @RequestBody\n"
                                        + "CAUSE: Request body failed @NotNull/@Size/@Email constraints\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Add global handler:\n"
                                        + "     @ExceptionHandler(MethodArgumentNotValidException.class)\n"
                                        + "     public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex)\n"
                                        + "  2. Return field errors: ex.getBindingResult().getFieldErrors()\n"
                                        + "  3. Frontend: display returned field error messages to user\n"
                                        + "  4. Check DTO: all fields have correct constraint annotations",
                                List.of("validation", "spring", "dto", "constraint", "400")),
                        makeErrorSolution(
                                "sb-async-01",
                                "Spring Async / CompletableFuture Error — Find, Locate, Solve",
                                "ERROR: ExecutionException wrapping real cause / Task rejected\n"
                                        + "HOW TO FIND: grep -n 'ExecutionException\\|Task.*rejected\\|AsyncRequestTimeoutException' logs/application.log\n"
                                        + "HOW TO LOCATE: @Async annotated method or CompletableFuture.get() call\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Thread pool queue full (rejection)\n"
                                        + "  - Exception inside @Async method swallowed by Future\n"
                                        + "  - Missing @EnableAsync on @Configuration class\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Unwrap: catch ExecutionException → getCause() for real error\n"
                                        + "  2. Add @EnableAsync to a @Configuration class\n"
                                        + "  3. Configure pool: spring.task.execution.pool.max-size=20\n"
                                        + "  4. Prefer Reactor Mono/Flux over @Async in reactive apps\n"
                                        + "  5. Set rejection policy: CallerRunsPolicy for graceful degradation",
                                List.of("async", "completablefuture", "thread-pool", "spring", "reactive")),
                        makeErrorSolution(
                                "sb-actuator-01",
                                "Actuator Health Down — Find, Locate, Solve",
                                "ERROR: GET /actuator/health returns DOWN or components degraded\n"
                                        + "HOW TO FIND: curl http://localhost:8080/actuator/health | python3 -m json.tool\n"
                                        + "HOW TO LOCATE: JSON response shows which component is DOWN\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Firestore/DB unreachable\n"
                                        + "  - Disk space low\n"
                                        + "  - Custom HealthIndicator returning DOWN\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Check component name in health response (e.g., 'firestore', 'db')\n"
                                        + "  2. Fix underlying issue (connection, credentials, disk)\n"
                                        + "  3. Enable details: management.endpoint.health.show-details=always\n"
                                        + "  4. Check custom indicators: grep -rn 'HealthIndicator' src/",
                                List.of("actuator", "health", "monitoring", "spring-boot"))));
    }

    private Flux<SystemLearning> seedFrontendErrorPatterns() {
        return Flux.fromIterable(
                List.of(
                        makeErrorSolution(
                                "fe-api-call-01",
                                "Frontend API Call Failing — Find, Locate, Solve",
                                "ERROR: fetch failed / Network Error / ERR_CONNECTION_REFUSED\n"
                                        + "HOW TO FIND: Browser DevTools → Network tab → failed requests (red)\n"
                                        + "HOW TO LOCATE: React service file making the API call\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Backend not running\n"
                                        + "  - Wrong API URL in .env (VITE_API_URL)\n"
                                        + "  - CORS blocking the request\n"
                                        + "  - JWT token expired\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Check backend: curl http://localhost:8080/api/health\n"
                                        + "  2. Check .env: VITE_API_URL=http://localhost:8080\n"
                                        + "  3. Check Network tab → Headers → see actual request URL\n"
                                        + "  4. If 401: re-login to get fresh token\n"
                                        + "  5. If CORS: see es-cors-01 solution",
                                List.of("frontend", "fetch", "api-call", "network-error", "react")),
                        makeErrorSolution(
                                "fe-state-01",
                                "React State / undefined Error — Find, Locate, Solve",
                                "ERROR: Cannot read properties of undefined / Cannot read property of null\n"
                                        + "HOW TO FIND: Browser DevTools → Console → error message + file:line\n"
                                        + "HOW TO LOCATE: Click error in console → opens source file at exact line\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Accessing state before data loads (async fetch)\n"
                                        + "  - API response structure different from expected\n"
                                        + "  - Array.map() on undefined value\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Add loading guard: if (!data) return <Loading />\n"
                                        + "  2. Optional chaining: data?.field?.subfield\n"
                                        + "  3. Default value: const items = data?.items ?? []\n"
                                        + "  4. Log API response: console.log('response:', JSON.stringify(response))\n"
                                        + "  5. Check Network tab → Response tab for actual API data shape",
                                List.of("react", "state", "undefined", "null", "frontend")),
                        makeErrorSolution(
                                "fe-routing-01",
                                "React Router 404 / Blank Page — Find, Locate, Solve",
                                "ERROR: Blank page after navigation / 404 on page refresh\n"
                                        + "HOW TO FIND: Browser console + Network tab\n"
                                        + "HOW TO LOCATE: Router configuration in App.jsx or router.js\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Missing route definition\n"
                                        + "  - Firebase Hosting not configured for SPA (single page app)\n"
                                        + "  - Vite base URL mismatch\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Add catch-all route: <Route path='*' element={<NotFound />} />\n"
                                        + "  2. Firebase Hosting: add 'rewrites' in firebase.json:\n"
                                        + "     {\"source\": \"**\", \"destination\": \"/index.html\"}\n"
                                        + "  3. Vite config: base: '/' in vite.config.js\n"
                                        + "  4. Use <BrowserRouter> not <HashRouter> for clean URLs",
                                List.of("react-router", "404", "spa", "firebase-hosting", "routing"))));
    }

    private Flux<SystemLearning> seedJvmAndGcPatterns() {
        return Flux.fromIterable(
                List.of(
                        makeErrorSolution(
                                "jvm-gc-01",
                                "GC Pauses / High GC Time — Find, Locate, Solve",
                                "SYMPTOM: Slow responses / GC overhead limit exceeded\n"
                                        + "HOW TO FIND: Add JVM flags: -verbose:gc -XX:+PrintGCDetails\n"
                                        + "  OR: grep -n 'GC\\|pause' logs/application.log\n"
                                        + "HOW TO LOCATE: GC logs → look for long 'Stop-the-world' pauses (>500ms)\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Too many short-lived objects (allocation pressure)\n"
                                        + "  - Heap too small for workload\n"
                                        + "  - Old generation filling up (memory leak)\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Switch to G1GC: -XX:+UseG1GC\n"
                                        + "  2. Set heap: -Xms512m -Xmx2g\n"
                                        + "  3. Tune G1: -XX:MaxGCPauseMillis=200\n"
                                        + "  4. Cloud Run: --memory=2Gi --cpu=2\n"
                                        + "  5. Profile with async-profiler to find allocation hotspots",
                                List.of("jvm", "gc", "garbage-collection", "performance", "heap")),
                        makeErrorSolution(
                                "jvm-metaspace-01",
                                "Metaspace OOM — Find, Locate, Solve",
                                "ERROR: java.lang.OutOfMemoryError: Metaspace\n"
                                        + "HOW TO FIND: grep -n 'Metaspace\\|OutOfMemoryError' logs/application.log\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Too many dynamically generated classes (reflection, CGLIB proxies)\n"
                                        + "  - Spring creating too many bean proxies\n"
                                        + "  - Memory leak from classloaders not being GCed\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Set limit: -XX:MaxMetaspaceSize=512m\n"
                                        + "  2. Monitor: jstat -gcmetacapacity <pid>\n"
                                        + "  3. Reduce dynamic class generation\n"
                                        + "  4. Check for classloader leaks in old hot-reload setups",
                                List.of("jvm", "metaspace", "oom", "classloader", "spring"))));
    }

    private Flux<SystemLearning> seedDatabaseErrorPatterns() {
        return Flux.fromIterable(
                List.of(
                        makeErrorSolution(
                                "db-query-slow-01",
                                "Slow Firestore Query — Find, Locate, Solve",
                                "SYMPTOM: API response >2s / Firestore query timeout\n"
                                        + "HOW TO FIND: GCP Console → Firestore → Monitoring → Query latency\n"
                                        + "  OR: Add timing log: long start = System.currentTimeMillis();\n"
                                        + "HOW TO LOCATE: Repository or service method doing Firestore query\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Missing composite index for multi-field query\n"
                                        + "  - Fetching entire collection then filtering in Java (N+1 problem)\n"
                                        + "  - No pagination (limit)\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Add index: GCP Console → Firestore → Indexes → Create composite\n"
                                        + "  2. Move filtering to Firestore: .whereEqualTo('field', value)\n"
                                        + "  3. Always add .limit(100) to queries\n"
                                        + "  4. Use .select() to fetch only needed fields\n"
                                        + "  5. Cache frequently read data: ConcurrentHashMap with TTL",
                                List.of("firestore", "slow-query", "index", "performance", "database")),
                        makeErrorSolution(
                                "db-transaction-01",
                                "Firestore Transaction Conflict — Find, Locate, Solve",
                                "ERROR: Transaction contention / Too much contention on documents\n"
                                        + "HOW TO FIND: grep -n 'contention\\|ABORTED\\|transaction' logs/application.log\n"
                                        + "HOW TO LOCATE: Code using firestore.runTransaction()\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Multiple concurrent transactions on same document\n"
                                        + "  - Transaction doing too much work (slow)\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Keep transactions short: read → compute → write only\n"
                                        + "  2. Use optimistic concurrency: read outside transaction, write in transaction\n"
                                        + "  3. Retry on ABORTED: implement exponential backoff retry\n"
                                        + "  4. Shard hot documents if write rate >1/sec",
                                List.of("firestore", "transaction", "contention", "database", "aborted"))));
    }

    private Flux<SystemLearning> seedCloudAndDeploymentErrors() {
        return Flux.fromIterable(
                List.of(
                        makeErrorSolution(
                                "cloud-cloudrun-01",
                                "Cloud Run Deployment Failure — Find, Locate, Solve",
                                "ERROR: Cloud Run service failed to start / Container failed health check\n"
                                        + "HOW TO FIND: gcloud run services describe supremeai-backend --region=us-central1\n"
                                        + "  OR: GCP Console → Cloud Run → Logs tab\n"
                                        + "HOW TO LOCATE: Container startup logs in Cloud Run log viewer\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Health check endpoint /api/health not responding in time\n"
                                        + "  - Missing environment variables (SECRET_KEY, GEMINI_API_KEY)\n"
                                        + "  - Container port not matching --port flag\n"
                                        + "  - OOM on startup\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Test locally: docker build -t app . && docker run -p 8080:8080 app\n"
                                        + "  2. Set env vars: gcloud run services update ... --set-env-vars KEY=VALUE\n"
                                        + "  3. Use Secret Manager: --set-secrets KEY=SECRET_NAME:latest\n"
                                        + "  4. Increase memory: --memory=2Gi\n"
                                        + "  5. Set startup timeout: --startup-cpu-boost --min-instances=1",
                                List.of("cloud-run", "deployment", "container", "gcp", "startup")),
                        makeErrorSolution(
                                "cloud-firebase-deploy-01",
                                "Firebase Hosting Deploy Failure — Find, Locate, Solve",
                                "ERROR: firebase deploy fails / Hosting deploy error\n"
                                        + "HOW TO FIND: firebase deploy 2>&1 | tee deploy.log\n"
                                        + "HOW TO LOCATE: deploy.log → look for Error: section\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Not logged in: run 'firebase login'\n"
                                        + "  - Wrong project: check firebase.json → hosting.site matches project\n"
                                        + "  - Build not done before deploy\n"
                                        + "  - public directory empty\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Login: firebase login --reauth\n"
                                        + "  2. Check project: firebase use --list\n"
                                        + "  3. Build first: cd dashboard && npm run build\n"
                                        + "  4. Verify: ls dashboard/dist/ (should have index.html)\n"
                                        + "  5. Deploy only hosting: firebase deploy --only hosting",
                                List.of("firebase", "hosting", "deploy", "frontend", "firebase-cli")),
                        makeErrorSolution(
                                "cloud-secret-01",
                                "GCP Secret Manager Access Denied — Find, Locate, Solve",
                                "ERROR: Permission denied / PERMISSION_DENIED accessing secret\n"
                                        + "HOW TO FIND: grep -n 'PERMISSION_DENIED\\|secretmanager' logs/application.log\n"
                                        + "HOW TO LOCATE: Code accessing SecretManagerServiceClient or env var loading\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Service account missing 'Secret Manager Secret Accessor' role\n"
                                        + "  - Secret name typo\n"
                                        + "  - Secret in wrong project\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Grant role: gcloud projects add-iam-policy-binding PROJECT_ID \\\n"
                                        + "       --member=serviceAccount:SA_EMAIL \\\n"
                                        + "       --role=roles/secretmanager.secretAccessor\n"
                                        + "  2. Verify secret: gcloud secrets list --project=PROJECT_ID\n"
                                        + "  3. Test access: gcloud secrets versions access latest --secret=SECRET_NAME\n"
                                        + "  4. Check service account used by Cloud Run: --service-account=SA_EMAIL",
                                List.of("secret-manager", "gcp", "iam", "permissions", "service-account")),
                        makeErrorSolution(
                                "cloud-ci-build-01",
                                "CI/CD Build Pipeline Failure — Find, Locate, Solve",
                                "ERROR: Makefile/deploy.sh fails / Gradle build fails in CI\n"
                                        + "HOW TO FIND: Check CI logs (GitHub Actions / Cloud Build)\n"
                                        + "HOW TO LOCATE: First red step in pipeline = root cause\n"
                                        + "COMMON CAUSES:\n"
                                        + "  - Missing build dependency (Node, Java version mismatch)\n"
                                        + "  - Environment variable not set in CI secrets\n"
                                        + "  - Dashboard build not done before backend JAR creation\n"
                                        + "  - Tests failing blocking deploy\n"
                                        + "HOW TO SOLVE:\n"
                                        + "  1. Build order: 1) frontend build 2) copy to static/ 3) ./gradlew build\n"
                                        + "  2. Add CI secrets: Settings → Secrets → Actions → add KEY\n"
                                        + "  3. Pin Java version: java-version: '21' in workflow\n"
                                        + "  4. Skip tests in CI if needed: ./gradlew build -x test\n"
                                        + "  5. Run deploy.sh locally first to validate steps",
                                List.of("ci-cd", "makefile", "gradle", "build", "github-actions"))));
    }

    private Flux<SystemLearning> seedAiProviderManagement() {
        return Flux.fromIterable(
                List.of(
                        makeLearning(
                                "pm-quarantine-01",
                                "Provider Quarantine Recovery",
                                "ai-provider-management",
                                "[LOCAL-SEED]\n1. Check why provider was quarantined in logs.\n2. Navigate to Admin Dashboard -> AI Providers.\n3. Click 'Release' on the quarantined provider to resume traffic.",
                                List.of("provider", "quarantine", "recovery"),
                                false,
                                0.95),
                        makeLearning(
                                "pm-config-01",
                                "Add New Provider Config",
                                "ai-provider-management",
                                "[LOCAL-SEED]\n1. Add document to Firestore 'api_providers' collection.\n2. Required fields: name, endpoint, type, isActive (bool), priority (int).\n3. Keys should be stored in Secret Manager, not plaintext.",
                                List.of("provider", "config", "firestore"),
                                false,
                                0.95),
                        makeLearning(
                                "pm-rotation-01",
                                "Provider Priority Rotation",
                                "ai-provider-management",
                                "[LOCAL-SEED]\n1. Lower priority number = higher precedence.\n2. To rotate, set failing provider priority to 999.\n3. Set backup provider priority to 1.\n4. System auto-routes within 30s.",
                                List.of("provider", "priority", "rotation"),
                                false,
                                0.95),
                        makeLearning(
                                "pm-circuit-01",
                                "Circuit Breaker Override",
                                "ai-provider-management",
                                "[LOCAL-SEED]\n1. Circuit breakers open after 3 consecutive failures.\n2. To force reset, restart the JVM or use the /api/admin/providers/{id}/release endpoint.\n3. Verify health endpoint before resetting.",
                                List.of("provider", "circuit-breaker", "reset"),
                                false,
                                0.95),
                        makeLearning(
                                "pm-ratelimit-01",
                                "Token Usage Limit Recovery",
                                "ai-provider-management",
                                "[LOCAL-SEED]\n1. 429 Too Many Requests indicates quota exceeded.\n2. System automatically applies exponential backoff.\n3. To resolve permanently, request quota increase from provider or rotate to backup.",
                                List.of("provider", "rate-limit", "quota"),
                                false,
                                0.95)));
    }

    private Flux<SystemLearning> seedUserAndPermissionManagement() {
        return Flux.fromIterable(
                List.of(
                        makeLearning(
                                "um-admin-01",
                                "Admin Role Assignment",
                                "user-management",
                                "[LOCAL-SEED]\n1. Open Firestore console.\n2. Navigate to 'users' collection.\n3. Find target user document.\n4. Update field 'tier' to 'ADMIN'.\n5. User must re-login to obtain updated JWT claims.",
                                List.of("user", "admin", "role", "permission"),
                                false,
                                0.95),
                        makeLearning(
                                "um-revoke-01",
                                "API Key Revocation",
                                "user-management",
                                "[LOCAL-SEED]\n1. Navigate to 'user_api_keys' collection.\n2. Delete or set 'isActive' = false for the compromised key.\n3. Changes take effect immediately due to live Firestore rules.",
                                List.of("user", "api-key", "revoke", "security"),
                                true,
                                0.95),
                        makeLearning(
                                "um-ratelimit-01",
                                "User Rate Limit Adjustment",
                                "user-management",
                                "[LOCAL-SEED]\n1. Open user document in Firestore.\n2. Add/update 'customRateLimit' object.\n3. Set 'requestsPerMinute' to desired integer.\n4. Takes precedence over global defaults.",
                                List.of("user", "rate-limit", "quota"),
                                false,
                                0.95),
                        makeLearning(
                                "um-jwt-01",
                                "JWT Token Invalidation",
                                "user-management",
                                "[LOCAL-SEED]\n1. To invalidate all tokens, rotate JWT_SECRET in environment.\n2. To invalidate single user, change their 'tokenVersion' in Firestore.\n3. All existing tokens will fail signature or version check.",
                                List.of("user", "jwt", "invalidate", "security"),
                                true,
                                0.95),
                        makeLearning(
                                "um-rbac-01",
                                "RBAC Policy Update",
                                "user-management",
                                "[LOCAL-SEED]\n1. Edit firestore.rules file.\n2. Use isAdmin() or isOwner() helper functions.\n3. Run 'firebase deploy --only firestore:rules' to apply.\n4. Default deny all.",
                                List.of("user", "rbac", "firestore", "rules"),
                                false,
                                0.95)));
    }

    private Flux<SystemLearning> seedLocalAiModelSetup() {
        return Flux.fromIterable(
                List.of(
                        makeLearning(
                                "lm-ollama-01",
                                "Local Ollama Integration",
                                "local-ai-setup",
                                "[LOCAL-SEED]\n1. Install Ollama and run 'ollama serve'.\n2. Pull model: 'ollama run llama3'.\n3. Add provider in SupremeAI: endpoint 'http://localhost:11434/v1', type 'ollama', model 'llama3'.\n4. Enable Solo Mode.",
                                List.of("local-ai", "ollama", "integration"),
                                false,
                                0.95),
                        makeLearning(
                                "lm-llamacpp-01",
                                "Llama.cpp Fallback Setup",
                                "local-ai-setup",
                                "[LOCAL-SEED]\n1. Run Llama.cpp server: './server -m model.gguf -c 2048'.\n2. Add provider config in SupremeAI pointing to Llama.cpp host/port.\n3. Use 'openai' provider type as Llama.cpp is API-compatible.",
                                List.of("local-ai", "llamacpp", "fallback"),
                                false,
                                0.95),
                        makeLearning(
                                "lm-gguf-01",
                                "GGUF Model Loading",
                                "local-ai-setup",
                                "[LOCAL-SEED]\n1. Download GGUF files from HuggingFace.\n2. Place in designated models/ directory.\n3. Ensure sufficient RAM/VRAM before starting the inference server.\n4. Recommended: Quantized Q4_K_M for 8GB RAM.",
                                List.of("local-ai", "gguf", "model-loading"),
                                false,
                                0.95),
                        makeLearning(
                                "lm-vram-01",
                                "VRAM Exhaustion Recovery",
                                "local-ai-setup",
                                "[LOCAL-SEED]\n1. If local inference crashes with CUDA Out of Memory.\n2. Restart inference engine with smaller context window (e.g., -c 1024).\n3. Or offload fewer layers to GPU (e.g., -ngl 20).",
                                List.of("local-ai", "vram", "oom", "recovery"),
                                false,
                                0.95),
                        makeLearning(
                                "lm-timeout-01",
                                "Local Model Timeout Tuning",
                                "local-ai-setup",
                                "[LOCAL-SEED]\n1. Local models are slower than cloud APIs.\n2. Increase read timeout in provider config to 120s or 300s.\n3. Monitor inference speed (tokens/sec) to set appropriate SLAs.",
                                List.of("local-ai", "timeout", "tuning"),
                                false,
                                0.95)));
    }
}
