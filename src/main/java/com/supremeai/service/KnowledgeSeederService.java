package com.supremeai.service;

import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Knowledge Seeder — seeds SupremeAI's core knowledge base into Firestore.
 *
 * Seeds the following collections:
 * - system_learning: 22 core plans, common error solutions, AI patterns, best practices
 *
 * Uses @PostConstruct with idempotent check (only seeds when collection is empty).
 * Follows the same pattern as GuideDataInitializer.
 */
@Component
public class KnowledgeSeederService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSeederService.class);

    @Autowired
    private SystemLearningRepository systemLearningRepository;

    @PostConstruct
    public void seedKnowledge() {
        systemLearningRepository.count()
            .flatMapMany(count -> {
                if (count == 0) {
                    log.info("[SEED] Firestore system_learning is empty — seeding knowledge base...");
                    return seedAll();
                } else {
                    log.info("[SEED] system_learning already has {} entries — skipping seed", count);
                    return Flux.empty();
                }
            })
            .subscribe(
                entry -> log.debug("[SEED] Saved: {}", entry.getId()),
                error -> log.error("[SEED] Failed to seed knowledge: {}", error.getMessage()),
                () -> log.info("[SEED] Knowledge base seed complete")
            );
    }

    private Flux<SystemLearning> seedAll() {
        return Flux.merge(
            seedCorePlans(),
            seedErrorSolutions(),
            seedAiPatterns(),
            seedBestPractices(),
            seedLifecyclePolicies()
        );
    }

    // ─── Core Plans Knowledge (22 Plans) ─────────────────────────────────────

    private Flux<SystemLearning> seedCorePlans() {
        List<SystemLearning> plans = List.of(
            makeLearning("plan-001", "Multi-Agent System", "plans",
                "Dynamic 0 to ∞ agents. Performance-based routing: best agent for specific task type. " +
                "Max 3 agents per task, max 10 in voting. 60% approval threshold. " +
                "System AI fallback when no agents available or all fail. " +
                "Task categories: Code Writing, Court Error Checking, Code Review, Voting, GitHub Push, CI/CD Check.",
                List.of("multi-agent", "orchestration", "routing", "voting"), true, 0.98),

            makeLearning("plan-002", "API Key Rotation", "plans",
                "Multiple API keys per AI model for free-tier maximization. " +
                "80% threshold rotation: pre-emptive before limit reached. " +
                "System AI backup when rotation impossible. " +
                "Key validation, leak detection, and automatic replacement. " +
                "Supported providers: OpenAI, Anthropic, Gemini, Groq, Cohere, Mistral, DeepSeek, Grok, LLaMA.",
                List.of("api-rotation", "key-management", "free-tier"), true, 0.95),

            makeLearning("plan-003", "Continuous Learning", "plans",
                "System learns continuously like a good student. Admin approval gate at topic level. " +
                "Sources: Wikipedia extractor, StackOverflow extractor. " +
                "ContentSanitizerService: PII masking (7 patterns), toxic code detection. " +
                "Quota: 1000 global/day, 50 per user/day. " +
                "Modes: AGGRESSIVE, BALANCED, MANUAL, PAUSED. Emergency pause available. " +
                "Knowledge versioning, recency decay (half-life 693 days), soft unlearning.",
                List.of("learning", "wikipedia", "stackoverflow", "pii", "quota"), true, 0.97),

            makeLearning("plan-004", "Intent Analysis", "plans",
                "Analyzes chat in real-time to determine: permanent rule vs planning vs command. " +
                "Confirms understanding with user only when uncertainty exists. " +
                "Learns user preference over time — avoids repeating same confirmations. " +
                "Saves confirmed intents to Firestore. Smart confirmation threshold improves with usage.",
                List.of("intent", "nlp", "confirmation", "learning"), true, 0.93),

            makeLearning("plan-005", "Plan Compatibility Analysis", "plans",
                "Compares new proposed plans with initial plan. Calculates compatibility score 0-100. " +
                "Breaking keywords reduce score: delete, drop, remove, deprecate, replace, migrate, reset. " +
                "Additive keywords are compatible: add, extend, enhance, improve, support, integrate. " +
                "Risk levels: LOW (>=80), MEDIUM (>=50), HIGH (<50). " +
                "User autonomy preserved — warns but does not block execution. " +
                "Analogy: 'building vs buying land' for user-friendly explanation.",
                List.of("compatibility", "plan-analysis", "risk"), true, 0.90),

            makeLearning("plan-006", "Dual Repository System", "plans",
                "Two-tier repository management: Main system repo (auto push) + User repos (conditional). " +
                "Trust-based tiers: HIGH trust = full automation, LOW trust = manual only. " +
                "GitHub Bot for access, manual fallback always available. " +
                "Transparent access list: user knows exactly what repos are accessed. " +
                "Pre-push verification before any user repo push.",
                List.of("github", "dual-repo", "trust", "automation"), true, 0.92),

            makeLearning("plan-007", "Dashboard & Plugin Settings", "plans",
                "User/Admin configurable settings panel. Features: auto-approve toggle (task-level rules), " +
                "language selection (EN/BN/multi), trust level management, permission scope control. " +
                "VS Code extension + IntelliJ plugin settings sync. " +
                "Task classification: LOW risk = auto approve, HIGH risk = manual review. " +
                "Settings stored in Firestore system_configs collection.",
                List.of("dashboard", "settings", "permissions", "vscode", "intellij"), false, 0.80),

            makeLearning("plan-008", "Adaptive Response Depth", "plans",
                "System adapts response verbosity to user preference. Default: concise and simple. " +
                "Details only when explicitly requested. Learns preference over time from user feedback. " +
                "Context awareness: complex technical topics get more detail automatically. " +
                "Response depth stored per userId in Firestore user preferences.",
                List.of("adaptive", "response", "ux", "preference"), true, 0.90),

            makeLearning("plan-009", "Smart Data Storage", "plans",
                "Minimal but informative data storage. Only save what's needed for future retrieval. " +
                "5-tier storage: hot (Redis), warm (Firestore), cool (archive), cold (export), deleted. " +
                "Recency-aware scoring for eviction decisions. " +
                "DataLifecycleService: auto-expire + soft delete (grace period) + hard delete scheduler.",
                List.of("storage", "lifecycle", "firestore", "redis", "tiering"), true, 0.88),

            makeLearning("plan-010", "API Limit Discovery", "plans",
                "Auto-discover free/paid tier limits for each AI provider. " +
                "Strategy: passive monitoring (count responses until 429 error) + doc scraping. " +
                "Saves discovered limits to Firestore for rotation planning. " +
                "Challenge: validation tests consume the very quota being measured. " +
                "Mitigation: track 429 errors and infer limits from real usage patterns.",
                List.of("api-limits", "discovery", "quota", "rate-limiting"), false, 0.70),

            makeLearning("plan-011", "Pre-Push Verification", "plans",
                "Verify before pushing to user repo. Check for others' changes (git fetch + diff). " +
                "Review code if remote changes found. Auto-merge safety: sensitive code = manual review. " +
                "GitHubWebhookController handles push events. " +
                "Conflict resolution: flag for manual review rather than auto-resolve.",
                List.of("pre-push", "git", "verification", "merge"), false, 0.75),

            makeLearning("plan-012", "Multi-Platform Expansion", "plans",
                "Beyond API generator: Web (React/Next.js), Mobile (Flutter/React Native), Desktop (Electron). " +
                "FullStackCodeGenerator: generates complete project structure. " +
                "MultiPlatformGenerator: platform-specific adaptations. " +
                "Manual delegation list: app store submission, code signing, certificate management. " +
                "Platform-specific requirements stored in knowledge base.",
                List.of("multi-platform", "flutter", "react", "generation"), false, 0.70),

            makeLearning("plan-013", "Marketing Strategy Advisor", "plans",
                "Business partner role beyond just coding. Generates launch plans, social media strategy, growth tactics. " +
                "MarketingAdvisorService: context-aware (product type, target audience, budget). " +
                "Channels: Twitter/X (#buildinpublic), LinkedIn, Reddit, Product Hunt. " +
                "Tactics: referral programs, content marketing, open-source utility, community Discord. " +
                "Budget-aware: $0 (organic only) to paid ads strategy.",
                List.of("marketing", "launch", "social-media", "growth"), true, 0.88),

            makeLearning("plan-014", "Vision & Image Integration", "plans",
                "Image understanding: screenshot error reading, visual data extraction, UI mockup parsing. " +
                "VisionService: processes images from AI provider vision APIs. " +
                "Free tier limited — hybrid approach: use vision only when text description insufficient. " +
                "Supported: OpenAI GPT-4V, Gemini Vision, Anthropic Claude Vision. " +
                "OCR via AdminOCRCard.tsx on dashboard.",
                List.of("vision", "image", "ocr", "multimodal"), false, 0.75),

            makeLearning("plan-015", "Hybrid Voice System", "plans",
                "Voice-to-text: capture on frontend (Web Speech API / MediaRecorder), process here. " +
                "HybridVoiceService: text normalization, language detection, Bengali correction. " +
                "Supported: English (high accuracy), Bengali/Bangla (medium ~70-80%). " +
                "Bengali romanized corrections: ami→আমি, tumi→তুমি, apni→আপনি etc. " +
                "Intent hints extracted: COMMAND_CREATE, COMMAND_DELETE, COMMAND_QUERY, QUESTION, COMMAND_UPDATE.",
                List.of("voice", "bengali", "speech", "nlp"), true, 0.88),

            makeLearning("plan-016", "CI/CD Sandbox", "plans",
                "CI/CD pipeline as testing gate: error = not merged, success = safe to proceed. " +
                "GitHub Actions workflows in .github/workflows/. " +
                "Cloud Build config in cloudbuild.yaml. " +
                "Test gate: ./gradlew test must pass. JaCoCo minimum 10% coverage enforced. " +
                "Sandbox: test in isolated environment before production push.",
                List.of("cicd", "github-actions", "testing", "sandbox"), false, 0.80),

            makeLearning("plan-017", "Data Lifecycle Management", "plans",
                "DataLifecycleService: auto-expire after TTL (default 30 days). " +
                "Soft delete → grace period (default 7 days) → hard delete. " +
                "Recovery: restore within grace period. " +
                "Schedulers: expiry job (every 1h), hard-delete job (every 6h). " +
                "Config: lifecycle.default.ttl.days, lifecycle.grace.period.days, lifecycle.cleanup.interval.ms.",
                List.of("lifecycle", "ttl", "cleanup", "soft-delete"), true, 0.92),

            makeLearning("plan-018", "Crowdsourced API Model", "plans",
                "TEMPORARY bootstrap solution: users share free AI API keys for premium access. " +
                "CRITICAL RISKS: ToS violation, security breach risk, trust barrier. " +
                "Mitigations: transparent access list, optional not forced, user choice, future removal. " +
                "This plan will be removed when dedicated API infrastructure is in place (Phase 5). " +
                "Do NOT implement key storage without AES-256 encryption.",
                List.of("crowdsource", "api-keys", "bootstrap", "risk"), false, 0.60),

            makeLearning("plan-019", "Brilliant Idea Detection", "plans",
                "IdeaDetectionService: heuristic scoring from conversation text. " +
                "Score >= 20 = brilliant idea flagged. " +
                "Innovation keywords (+10): novel, unique, breakthrough, revolutionary, innovative. " +
                "Monetization keywords (+8): revenue, monetize, profit, subscription, freemium. " +
                "Solution keywords (+6): solve, fix, automate, eliminate, streamline. " +
                "Question marks penalty (-2 each): reduces speculation flags. " +
                "Admin review queue: PENDING → APPROVED / REJECTED.",
                List.of("idea-detection", "heuristic", "admin-review"), true, 0.88),

            makeLearning("plan-020", "Learning from Examples", "plans",
                "Admin provides real-world examples. System extracts underlying logic/pattern. " +
                "Confirmation loop: system explains understood pattern, admin validates. " +
                "Generalized rule saved to knowledge base, applied to all future similar cases. " +
                "Challenge: iterative time — many examples needed. " +
                "Integration: SystemLearningService + GlobalKnowledgeBase.",
                List.of("learning-examples", "pattern-extraction", "generalization"), false, 0.80),

            makeLearning("plan-021", "Best Pattern Curation", "plans",
                "Observes AI agent work patterns, scores by quality metrics. " +
                "Keeps top 3 patterns per task type to avoid database bloat. " +
                "Quality scoring: success rate, response time, user satisfaction, error rate. " +
                "Dynamic expansion: when niche scenarios need more options, quota increases. " +
                "Storage: system_learning collection with learningType=APP_GENERATION.",
                List.of("patterns", "curation", "best-practices", "optimization"), false, 0.80),

            makeLearning("plan-022", "Simulator Controller", "plans",
                "Cloud-based app preview environment. Users can install, run, test generated apps. " +
                "SimulatorController: install/uninstall, session start/stop, device management. " +
                "SimulatorService: full lifecycle management with Firestore persistence. " +
                "SimulatorDeploymentService: URL generation, health check, Cloud Run (future). " +
                "SimulatorDashboard.tsx: React UI with quota display, device selector, iframe preview. " +
                "Quotas: FREE=3, BASIC=5, PRO=10, ENTERPRISE=20, ADMIN=50 simultaneous installs. " +
                "Auto-cleanup: apps expire after 7 days (configurable TTL). " +
                "Firestore collections: simulator_profiles, simulator_sessions, simulator_audit_log.",
                List.of("simulator", "cloud-run", "preview", "mobile-testing"), true, 0.85)
        );
        return systemLearningRepository.saveAll(plans);
    }

    // ─── Common Error Solutions ───────────────────────────────────────────────

    private Flux<SystemLearning> seedErrorSolutions() {
        List<SystemLearning> solutions = List.of(
            makeErrorSolution("err-001", "Firestore 429 Too Many Requests",
                "Reduce batch write size. Add exponential backoff. Use batched writes (max 500 ops). " +
                "Check Firestore quota in GCP console. Consider caching frequently read documents in Redis.",
                List.of("firestore", "429", "rate-limit", "batch")),

            makeErrorSolution("err-002", "Spring Boot Firebase Auth Token Expired",
                "Firebase ID tokens expire after 1 hour. Client must call firebase.auth().currentUser.getIdToken(true) " +
                "to force refresh. Backend JwtAuthFilter should return 401 with 'token-expired' error code " +
                "so frontend knows to refresh and retry.",
                List.of("firebase-auth", "jwt", "token-expired", "401")),

            makeErrorSolution("err-003", "Reactor Mono NullPointerException in flatMap",
                "Never return null inside a flatMap — use Mono.empty() instead. " +
                "Pattern: .flatMap(x -> x != null ? process(x) : Mono.empty()). " +
                "For optional results use .switchIfEmpty(Mono.just(default)).",
                List.of("reactor", "mono", "npe", "flatmap")),

            makeErrorSolution("err-004", "CORS Error from React Dashboard to Spring Boot",
                "Check CorsConfig.java — ensure allowed-origins includes the React dev server URL. " +
                "Set CORS_ALLOWED_ORIGINS env var to include http://localhost:5173 (Vite default). " +
                "For production, set to the actual Cloud Run URL.",
                List.of("cors", "react", "spring-boot", "vite")),

            makeErrorSolution("err-005", "Gradle Build OutOfMemoryError",
                "Add to gradle.properties: org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m. " +
                "Or set GRADLE_OPTS env var. For CI/CD, use --no-daemon flag.",
                List.of("gradle", "oom", "jvm", "build")),

            makeErrorSolution("err-006", "OpenAI API 429 Rate Limit",
                "Implement exponential backoff: wait 1s, 2s, 4s, 8s before retry. " +
                "Use ApiKeyRotationService to switch to a different key. " +
                "Set openai.retry.max=3 in properties. Log quota usage with LearningActivityLogService.",
                List.of("openai", "429", "rate-limit", "retry")),

            makeErrorSolution("err-007", "WebSocket Connection Refused in Dashboard",
                "Check WebSocketConfig.java — ensure /ws endpoint is registered. " +
                "VITE_WS_URL env var must point to correct server. " +
                "For local dev: ws://localhost:8080/ws. For prod: wss://your-cloud-run-url/ws. " +
                "SockJS fallback handles HTTP upgrade failures automatically.",
                List.of("websocket", "sockjs", "stomp", "connection")),

            makeErrorSolution("err-008", "Flutter App API Connection Error",
                "Ensure VITE_API_BASE_URL equivalent is set in Flutter as base URL constant. " +
                "For Android emulator, use 10.0.2.2 instead of localhost. " +
                "For physical device on same network, use machine's LAN IP. " +
                "Add internet permission in AndroidManifest.xml.",
                List.of("flutter", "android", "api", "connection")),

            makeErrorSolution("err-009", "Firestore Document Size Limit Exceeded",
                "Firestore max document size is 1 MiB. " +
                "Split large data into subcollections. " +
                "For knowledge entries, store content in Cloud Storage and reference URL. " +
                "Check ContentSanitizerService — trim oversized inputs before save.",
                List.of("firestore", "document-size", "limit", "subcollection")),

            makeErrorSolution("err-010", "Spring Security 403 on Admin Endpoints",
                "Ensure user has ROLE_ADMIN in Firebase custom claims. " +
                "In Firebase console: Admin SDK setCustomUserClaims(uid, {admin: true}). " +
                "JwtAuthFilter must extract and map these claims to Spring authorities. " +
                "Check SecurityConfig.java hasRole('ADMIN') vs hasAuthority('ROLE_ADMIN') — must match.",
                List.of("security", "403", "admin", "firebase-claims"))
        );
        return systemLearningRepository.saveAll(solutions);
    }

    // ─── AI Patterns ──────────────────────────────────────────────────────────

    private Flux<SystemLearning> seedAiPatterns() {
        List<SystemLearning> patterns = List.of(
            makePattern("pat-001", "Reactive Chain Pattern",
                "Always use Mono/Flux chains for Firestore operations. Never block with .block() in production. " +
                "Pattern: repository.findById(id).switchIfEmpty(Mono.error(new NotFoundException())).flatMap(entity -> process(entity)).map(result -> ResponseEntity.ok(result)). " +
                "For fire-and-forget: chain.subscribe() without blocking.",
                List.of("reactor", "mono", "flux", "firestore", "pattern")),

            makePattern("pat-002", "Agent Performance Routing",
                "Route tasks to best-performing agent for that task category. " +
                "Track: success rate per (agentId, taskCategory), average latency, error rate. " +
                "Selection: score = (successRate * 0.6) + (1/avgLatency * 0.3) + (1 - errorRate * 0.1). " +
                "Fallback: system AI when no agent scores above threshold (0.5). " +
                "Update scores after each task completion.",
                List.of("agent", "routing", "performance", "scoring")),

            makePattern("pat-003", "API Key Rotation Pattern",
                "Before each AI call: check current key usage vs limit. " +
                "If usage >= 80% of limit: rotate to next available key. " +
                "If all keys exhausted: use system AI fallback. " +
                "Track usage in memory (fast) + persist to Firestore every 10 calls (efficient). " +
                "Key health check: last-used timestamp + success rate.",
                List.of("api-key", "rotation", "pattern", "fallback")),

            makePattern("pat-004", "Learning Feedback Loop",
                "After each successful AI operation: extract pattern (input→output). " +
                "Store in system_learning with confidenceScore = task success rate. " +
                "Increment timesApplied counter on each reuse. " +
                "Recency decay: score *= exp(-0.001 * daysSinceLearn). " +
                "Best patterns (score >= 0.8, timesApplied >= 5) become timeless (bypass decay).",
                List.of("learning", "feedback", "pattern", "decay")),

            makePattern("pat-005", "Intent Classification Pattern",
                "Three-class intent detection: COMMAND (do something now), RULE (remember for future), PLAN (future action). " +
                "Signals: imperative verbs = COMMAND, 'always/never/remember' = RULE, 'tomorrow/plan/will' = PLAN. " +
                "Confidence threshold: 0.7 = auto-execute, 0.5-0.7 = confirm with user, <0.5 = ask for clarification. " +
                "Learning: track user correction rate per intent class.",
                List.of("intent", "classification", "nlp", "command", "rule")),

            makePattern("pat-006", "Multi-Agent Voting Pattern",
                "For critical decisions: distribute task to 3-10 agents simultaneously. " +
                "Each agent returns: answer + confidence score + reasoning. " +
                "Aggregation: weighted vote where weight = agent's historical accuracy for this task type. " +
                "Threshold: 60% approval for consensus. Tie-break: highest confidence single agent wins. " +
                "Log voting result with all individual responses for audit.",
                List.of("voting", "consensus", "multi-agent", "audit")),

            makePattern("pat-007", "Error Recovery Pattern",
                "Tier 1: Retry same agent (max 2 times, exponential backoff 1s/2s). " +
                "Tier 2: Route to next-best agent for that task category. " +
                "Tier 3: Simplify task and retry (reduce scope, break into subtasks). " +
                "Tier 4: System AI fallback with full task context. " +
                "Tier 5: Return error to user with helpful explanation and suggested next steps.",
                List.of("error-recovery", "retry", "fallback", "resilience"))
        );
        return systemLearningRepository.saveAll(patterns);
    }

    // ─── Best Practices ───────────────────────────────────────────────────────

    private Flux<SystemLearning> seedBestPractices() {
        List<SystemLearning> practices = List.of(
            makeBestPractice("bp-001", "Security: No Hardcoded Secrets",
                "NEVER hardcode API keys, passwords, or tokens in source code. " +
                "Always use environment variables: @Value(\"${key:}\") or System.getenv(). " +
                "Use UnifiedSecretsService for dynamic secret retrieval. " +
                "Use EncryptionService (AES-256-GCM) for secrets stored in Firestore. " +
                "Run: grep -r 'sk-\\|Bearer \\|api_key' src/ to detect leaks before commit.",
                List.of("security", "secrets", "encryption")),

            makeBestPractice("bp-002", "Firestore Write Efficiency",
                "Batch writes: use WriteBatch for <= 500 operations. " +
                "Avoid N+1: never loop and call save() individually — use saveAll(). " +
                "Read before write only when necessary — Firestore charges per operation. " +
                "Use @ServerTimestamp for createdAt/updatedAt instead of LocalDateTime. " +
                "Index only fields used in queries (defined in firestore.indexes.json).",
                List.of("firestore", "performance", "batch", "indexing")),

            makeBestPractice("bp-003", "React Dashboard Performance",
                "Use useCallback for handlers passed to child components. " +
                "Use React.memo for pure display components. " +
                "Debounce search inputs (300ms minimum). " +
                "Paginate large lists — never load all documents at once from Firestore. " +
                "Use Ant Design Table pagination: pageSize=10 default.",
                List.of("react", "performance", "antd", "pagination")),

            makeBestPractice("bp-004", "Spring Boot 3 Controller Pattern",
                "Controller: validation only, no business logic. " +
                "Service: business logic, security checks, Firestore operations. " +
                "Repository: data access only. " +
                "Use @PreAuthorize at method level — never skip security checks. " +
                "Return ResponseEntity<Map<String,Object>> for flexible responses. " +
                "Global exception handler via @RestControllerAdvice.",
                List.of("spring-boot", "controller", "service", "security")),

            makeBestPractice("bp-005", "Test Coverage Strategy",
                "Unit tests: pure service logic with mocked repositories (Mockito). " +
                "Integration tests: test controller→service→Firestore with Firebase emulator. " +
                "Use StepVerifier from reactor-test for Mono/Flux testing. " +
                "JaCoCo minimum 10% enforced — target 80%+ for critical services. " +
                "Run: ./gradlew test jacocoTestReport to generate coverage report.",
                List.of("testing", "junit5", "mockito", "jacoco", "reactor-test")),

            makeBestPractice("bp-006", "Bengali/Bangla UI Support",
                "All user-facing text should support both English (en) and Bengali (bn). " +
                "Store translations in Map<String,String> fields (matching UserGuide pattern). " +
                "Frontend: detect browser language or use user preference from Firestore. " +
                "Font: use 'Noto Sans Bengali' for proper Bengali character rendering. " +
                "Voice input: HybridVoiceService handles Bengali text normalization.",
                List.of("i18n", "bengali", "localization", "bangla")),

            makeBestPractice("bp-007", "Cloud-First Deployment",
                "Use Cloud Run for stateless services (auto-scales to 0). " +
                "Use Firestore (not PostgreSQL/MySQL) as primary database — no connection pool management. " +
                "Use Cloud Storage for files/APKs/images. " +
                "Use Firebase Hosting for React dashboard static files. " +
                "Environment-specific config: Spring profiles (local/cloud). " +
                "Health endpoint: GET /actuator/health must return 200 for Cloud Run readiness.",
                List.of("cloud-run", "firebase", "deployment", "gcp"))
        );
        return systemLearningRepository.saveAll(practices);
    }

    // ─── Lifecycle Policies ───────────────────────────────────────────────────

    private Flux<SystemLearning> seedLifecyclePolicies() {
        List<SystemLearning> policies = List.of(
            makeLearning("lc-001", "Chat History Retention", "lifecycle_policies",
                "Chat history TTL: 90 days for FREE users, 365 days for PRO/ENTERPRISE. " +
                "Soft delete after TTL: mark isDeleted=true, retain 7 days for recovery. " +
                "Hard delete: remove document from Firestore after grace period. " +
                "Exception: chat messages containing confirmed RULE intents are retained permanently.",
                List.of("chat", "retention", "ttl", "lifecycle"), false, 0.95),

            makeLearning("lc-002", "Simulator App Cleanup", "lifecycle_policies",
                "Installed simulator apps expire after 7 days (configurable per tier). " +
                "Daily cleanup job: check installedAt + TTL, trigger undeployFromSimulator. " +
                "Warning notification 24h before expiry. User can re-install on demand. " +
                "PRO/ENTERPRISE: 30 day TTL. ADMIN: no expiry.",
                List.of("simulator", "cleanup", "ttl", "expiry"), true, 0.92),

            makeLearning("lc-003", "Knowledge Base Decay Policy", "lifecycle_policies",
                "Knowledge entries decay over time: score *= exp(-0.001 * days). " +
                "Half-life approximately 693 days. " +
                "Timeless flag bypasses decay (for fundamental algorithms, core patterns). " +
                "Entries with score < 0.1 are marked obsolete (soft delete). " +
                "Admin can restore or permanently delete obsolete entries.",
                List.of("knowledge", "decay", "obsolete", "timeless"), true, 0.95),

            makeLearning("lc-004", "API Key Lifecycle", "lifecycle_policies",
                "API keys rotate automatically at 80% quota consumption. " +
                "Inactive keys (no use for 30 days): flagged for review, not auto-deleted. " +
                "Leaked/compromised keys: immediate revocation via ApiKeyRotationService. " +
                "Key audit log: every rotation event logged with timestamp, reason, userId. " +
                "Key recovery: soft-delete only, admin can restore within 7 days.",
                List.of("api-key", "lifecycle", "rotation", "audit"), true, 0.95),

            makeLearning("lc-005", "User Account Lifecycle", "lifecycle_policies",
                "Inactive accounts (no login for 90 days): email warning, then tier downgrade to GUEST. " +
                "Deleted accounts: data retained 30 days in archived state for recovery. " +
                "Hard delete: all Firestore documents, chat history, API keys purged after 30 days. " +
                "Admin accounts: never auto-expired. Require manual deactivation.",
                List.of("user", "account", "inactive", "deletion"), true, 0.92)
        );
        return systemLearningRepository.saveAll(policies);
    }

    // ─── Factory helpers ──────────────────────────────────────────────────────

    private SystemLearning makeLearning(String id, String topic, String category,
                                        String content, List<String> tags,
                                        boolean permanent, double confidence) {
        SystemLearning s = new SystemLearning(id, topic, category, content);
        s.setTags(tags);
        s.setPermanent(permanent);
        s.setConfidenceScore(confidence);
        s.setLearningType("KNOWLEDGE_SEED");
        s.setSuccess(true);
        s.setQualityScore(confidence);
        s.setTimesApplied(0);
        s.setLearnedAt(LocalDateTime.now());
        return s;
    }

    private SystemLearning makeErrorSolution(String id, String topic, String solution, List<String> tags) {
        SystemLearning s = makeLearning(id, topic, "error_solutions", solution, tags, true, 0.95);
        s.setType("ERROR_SOLUTION");
        s.setResolution(solution);
        s.setResolved(true);
        s.setSeverity("MEDIUM");
        return s;
    }

    private SystemLearning makePattern(String id, String topic, String content, List<String> tags) {
        return makeLearning(id, topic, "ai_patterns", content, tags, true, 0.92);
    }

    private SystemLearning makeBestPractice(String id, String topic, String content, List<String> tags) {
        return makeLearning(id, topic, "best_practices", content, tags, true, 0.95);
    }
}
