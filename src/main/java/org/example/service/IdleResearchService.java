package org.example.service;

import org.example.model.ConsensusVote;
import org.example.model.ResearchTopic;
import org.example.model.SystemLearning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * IDLE RESEARCH ENGINE - SupremeAI's Continuous Self-Improvement
 * 
 * When the system is NOT running any project or generation task,
 * it automatically researches new topics, fills knowledge gaps,
 * and updates its learning system.
 *
 * Research Modes:
 * 1. GAP_ANALYSIS   - Find weak spots in current knowledge
 * 2. TREND_SCAN     - Research latest tech trends & best practices
 * 3. ERROR_MINING   - Deep-dive into recurring error patterns
 * 4. SKILL_EXPAND   - Learn new frameworks, tools, techniques
 * 5. SELF_REVIEW    - Review own past decisions for improvement
 */
@Service
public class IdleResearchService {
    private static final Logger logger = LoggerFactory.getLogger(IdleResearchService.class);

    @Autowired
    private SystemLearningService learningService;

    @Autowired
    private MultiAIConsensusService consensusService;

    @Autowired
    private RequestQueueService queueService;

    @Autowired(required = false)
    private FirebaseService firebaseService;

    @Autowired
    private AdminControlService adminControlService;

    // --- State ---
    private final AtomicBoolean researchActive = new AtomicBoolean(false);
    private final AtomicLong lastProjectActivity = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong lastResearchCycle = new AtomicLong(0);
    private final AtomicLong totalResearchCount = new AtomicLong(0);

    /**
     * Admin-controlled toggle: when false the scheduled idle research will not run.
     * Defaults to true so learning starts immediately after the next push.
     */
    private final AtomicBoolean learningEnabled = new AtomicBoolean(true);

    /** Idle threshold: system must be idle for 1 hour before research starts */
    private static final long IDLE_THRESHOLD_MS = 60 * 60 * 1000;

    /** Minimum gap between research cycles: 5 minutes */
    private static final long RESEARCH_COOLDOWN_MS = 5 * 60 * 1000;

    // --- Firebase quota tracking (free-tier guard) ---
    /** Firebase free tier daily write limit (conservative safe ceiling). */
    private static final long FIREBASE_DAILY_WRITE_LIMIT = 18_000;
    /** Firebase free tier daily read limit (conservative safe ceiling). */
    private static final long FIREBASE_DAILY_READ_LIMIT = 50_000;

    private final AtomicLong firebaseWriteCount = new AtomicLong(0);
    private final AtomicLong firebaseReadCount  = new AtomicLong(0);
    private volatile long quotaWindowStartMs = System.currentTimeMillis();

    /** Max topics per research cycle */
    private static final int MAX_TOPICS_PER_CYCLE = 3;

    /** Research history (bounded) */
    private final List<ResearchTopic> researchHistory =
            Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_HISTORY = 500;

    /** Topic queue — topics waiting to be researched */
    private final Queue<ResearchTopic> topicQueue = new ConcurrentLinkedQueue<>();

    /** Domains the engine cycles through */
    private static final List<String> RESEARCH_DOMAINS = List.of(
        "ARCHITECTURE", "SECURITY", "PERFORMANCE", "DEVOPS",
        "AI_ML", "TESTING", "DATABASE", "API_DESIGN",
        "MICROSERVICES", "CLOUD_NATIVE", "OBSERVABILITY", "RESILIENCE"
    );

    /** Domain-specific research questions for each cycle */
    private static final Map<String, List<String>> DOMAIN_QUESTIONS = new LinkedHashMap<>();
    static {
        DOMAIN_QUESTIONS.put("ARCHITECTURE", List.of(
            "What are the latest best practices for building modular monoliths?",
            "How to design event-driven architectures that scale without complexity?",
            "What patterns prevent tight coupling in Spring Boot microservices?",
            "How to implement CQRS effectively in Java applications?"
        ));
        DOMAIN_QUESTIONS.put("SECURITY", List.of(
            "What are the most common Spring Boot security misconfigurations in 2026?",
            "How to implement zero-trust architecture in backend services?",
            "What are best practices for API key rotation and secrets management?",
            "How to prevent SSRF and injection attacks in modern Java apps?"
        ));
        DOMAIN_QUESTIONS.put("PERFORMANCE", List.of(
            "What are the most effective caching strategies for Spring Boot APIs?",
            "How to optimize JVM garbage collection for high-throughput services?",
            "What are best practices for database connection pooling tuning?",
            "How to implement efficient pagination for large datasets?"
        ));
        DOMAIN_QUESTIONS.put("DEVOPS", List.of(
            "What are the latest CI/CD best practices for Java projects?",
            "How to implement canary deployments with Kubernetes?",
            "What are effective strategies for zero-downtime database migrations?",
            "How to design effective health checks and readiness probes?"
        ));
        DOMAIN_QUESTIONS.put("AI_ML", List.of(
            "How to effectively implement RAG (Retrieval-Augmented Generation) patterns?",
            "What are best practices for AI model fallback and load balancing?",
            "How to evaluate and compare outputs from multiple AI providers?",
            "What are effective prompt engineering patterns for code generation?"
        ));
        DOMAIN_QUESTIONS.put("TESTING", List.of(
            "What are the most effective integration testing strategies for Spring Boot?",
            "How to implement contract testing between microservices?",
            "What are best practices for testing async and concurrent code?",
            "How to achieve meaningful code coverage without brittle tests?"
        ));
        DOMAIN_QUESTIONS.put("DATABASE", List.of(
            "What are best practices for Firebase Realtime Database scaling?",
            "How to design efficient NoSQL data models for read-heavy workloads?",
            "What are effective strategies for data consistency across services?",
            "How to implement efficient full-text search without Elasticsearch?"
        ));
        DOMAIN_QUESTIONS.put("API_DESIGN", List.of(
            "What are the latest REST API design standards and conventions?",
            "How to implement effective API versioning strategies?",
            "What are best practices for API rate limiting and throttling?",
            "How to design idempotent APIs for distributed systems?"
        ));
        DOMAIN_QUESTIONS.put("MICROSERVICES", List.of(
            "What are effective service mesh patterns for Java microservices?",
            "How to implement saga pattern for distributed transactions?",
            "What are best practices for inter-service communication?",
            "How to handle cascading failures across microservices?"
        ));
        DOMAIN_QUESTIONS.put("CLOUD_NATIVE", List.of(
            "What are the latest cloud-native patterns for Java applications?",
            "How to implement effective auto-scaling strategies?",
            "What are best practices for cloud cost optimization?",
            "How to design stateless services that handle session state?"
        ));
        DOMAIN_QUESTIONS.put("OBSERVABILITY", List.of(
            "What are the three pillars of observability and how to implement them?",
            "How to implement distributed tracing in Spring Boot?",
            "What are effective alerting strategies that minimize noise?",
            "How to build custom metrics that actually predict problems?"
        ));
        DOMAIN_QUESTIONS.put("RESILIENCE", List.of(
            "What are the latest circuit breaker patterns beyond Hystrix?",
            "How to implement bulkhead isolation in Spring applications?",
            "What are effective retry strategies with backoff for distributed systems?",
            "How to implement graceful degradation when dependencies fail?"
        ));
    }

    private int domainCycleIndex = 0;
    private final Map<String, Integer> domainQuestionIndex = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────
    // STARTUP — Trigger learning immediately on first deploy
    // ─────────────────────────────────────────────────

    /**
     * On startup, reset the idle timer so the system starts learning
     * immediately on the next scheduled check (since the project was idle
     * for a long time before this push).
     */
    @PostConstruct
    public void onStartup() {
        // Set lastProjectActivity far in the past so the first check sees the system as idle
        lastProjectActivity.set(System.currentTimeMillis() - IDLE_THRESHOLD_MS - 1000);
        logger.info("🚀 IdleResearchService started — learning will begin on first idle check (learningEnabled={})",
                learningEnabled.get());
    }

    // ─────────────────────────────────────────────────
    // PUBLIC API — Called by other services
    // ─────────────────────────────────────────────────

    /**
     * Enable the auto-learning system (admin action).
     */
    public void enableLearning() {
        learningEnabled.set(true);
        logger.info("✅ Auto-learning system ENABLED by admin");
    }

    /**
     * Disable the auto-learning system (admin action).
     */
    public void disableLearning() {
        learningEnabled.set(false);
        logger.info("🛑 Auto-learning system DISABLED by admin");
    }

    /**
     * Whether auto-learning is currently enabled.
     */
    public boolean isLearningEnabled() {
        return learningEnabled.get();
    }

    /**
     * Return current Firebase quota usage for the rolling 24-hour window.
     */
    public Map<String, Object> getFirebaseQuotaStatus() {
        maybeResetQuotaWindow();
        Map<String, Object> quota = new LinkedHashMap<>();
        long writes = firebaseWriteCount.get();
        long reads  = firebaseReadCount.get();
        quota.put("dailyWriteCount",  writes);
        quota.put("dailyReadCount",   reads);
        quota.put("dailyWriteLimit",  FIREBASE_DAILY_WRITE_LIMIT);
        quota.put("dailyReadLimit",   FIREBASE_DAILY_READ_LIMIT);
        quota.put("writeUsagePct",    Math.round(writes * 100.0 / FIREBASE_DAILY_WRITE_LIMIT));
        quota.put("readUsagePct",     Math.round(reads  * 100.0 / FIREBASE_DAILY_READ_LIMIT));
        quota.put("writesRemaining",  Math.max(0, FIREBASE_DAILY_WRITE_LIMIT - writes));
        quota.put("readsRemaining",   Math.max(0, FIREBASE_DAILY_READ_LIMIT  - reads));
        quota.put("withinSafeLimit",  isWithinFirebaseQuota());
        quota.put("windowStartMs",    quotaWindowStartMs);
        quota.put("windowResetIn",    "24h window — resets at " + new Date(quotaWindowStartMs + 86_400_000));
        return quota;
    }

    /**
     * Notify the engine that a project/generation task is active.
     * Resets the idle timer so research doesn't interfere.
     */
    public void notifyProjectActivity() {
        lastProjectActivity.set(System.currentTimeMillis());
        if (researchActive.get()) {
            logger.info("🔬→🏗️ Project activity detected — pausing idle research");
            researchActive.set(false);
        }
    }

    /**
     * Manually trigger a research cycle (admin override).
     */
    public Map<String, Object> triggerResearchNow() {
        logger.info("🔬 Admin triggered manual research cycle");
        return executeResearchCycle("ADMIN_TRIGGER");
    }

    /**
     * Queue a specific topic for research.
     */
    public void queueResearchTopic(String domain, String question, String source) {
        ResearchTopic topic = new ResearchTopic();
        topic.setDomain(domain);
        topic.setQuestion(question);
        topic.setStatus("QUEUED");
        topic.setSource(source);
        topicQueue.add(topic);
        logger.info("📋 Queued research topic: [{}] {}", domain, question);
    }

    /**
     * Get research stats.
     */
    public Map<String, Object> getResearchStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("learningEnabled", learningEnabled.get());
        stats.put("isIdle", isSystemIdle());
        stats.put("researchActive", researchActive.get());
        stats.put("totalResearchCompleted", totalResearchCount.get());
        stats.put("topicsInQueue", topicQueue.size());
        stats.put("historySize", researchHistory.size());
        stats.put("lastResearchCycle", lastResearchCycle.get());
        stats.put("idleForMs", System.currentTimeMillis() - lastProjectActivity.get());
        stats.put("idleThresholdMs", IDLE_THRESHOLD_MS);
        stats.put("researchCooldownMs", RESEARCH_COOLDOWN_MS);
        stats.put("firebaseQuota", getFirebaseQuotaStatus());

        // Domain coverage
        Map<String, Long> domainCoverage = researchHistory.stream()
            .filter(t -> "COMPLETED".equals(t.getStatus()))
            .collect(Collectors.groupingBy(ResearchTopic::getDomain, Collectors.counting()));
        stats.put("domainCoverage", domainCoverage);

        // Recent research
        List<Map<String, Object>> recent = researchHistory.stream()
            .sorted(Comparator.comparingLong(ResearchTopic::getCreatedAt).reversed())
            .limit(10)
            .map(this::topicToMap)
            .collect(Collectors.toList());
        stats.put("recentResearch", recent);

        return stats;
    }

    /**
     * Get full research history.
     */
    public List<ResearchTopic> getResearchHistory() {
        return new ArrayList<>(researchHistory);
    }

    // ─────────────────────────────────────────────────
    // SCHEDULED — Automatic idle detection & research
    // ─────────────────────────────────────────────────

    /**
     * Every 60 seconds, check if system is idle → start research.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void checkIdleAndResearch() {
        try {
            // Respect admin toggle
            if (!learningEnabled.get()) {
                return;
            }

            // Don't research if admin set FORCE_STOP
            if (adminControlService.getPermissionMode().name().equals("FORCE_STOP")) {
                return;
            }

            if (!isSystemIdle()) {
                return;
            }

            // Guard Firebase free quota
            if (!isWithinFirebaseQuota()) {
                logger.warn("⚠️ Firebase free-tier quota nearly exhausted — skipping research cycle");
                return;
            }

            // Cooldown between cycles
            long timeSinceLastResearch = System.currentTimeMillis() - lastResearchCycle.get();
            if (timeSinceLastResearch < RESEARCH_COOLDOWN_MS) {
                return;
            }

            logger.info("💤 System idle for ≥1 h — starting autonomous research cycle");
            executeResearchCycle("IDLE_SCAN");

        } catch (Exception e) {
            logger.warn("⚠️ Idle research check failed: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────
    // CORE — Research execution
    // ─────────────────────────────────────────────────

    private Map<String, Object> executeResearchCycle(String trigger) {
        researchActive.set(true);
        lastResearchCycle.set(System.currentTimeMillis());
        Map<String, Object> cycleReport = new LinkedHashMap<>();
        cycleReport.put("trigger", trigger);
        cycleReport.put("startedAt", System.currentTimeMillis());

        List<ResearchTopic> completedTopics = new ArrayList<>();

        try {
            // Step 1: Identify what to research
            List<ResearchTopic> topics = selectResearchTopics();

            cycleReport.put("topicsSelected", topics.size());
            logger.info("🔬 Research cycle: {} topics selected", topics.size());

            // Step 2: Research each topic
            for (ResearchTopic topic : topics) {
                // Abort if project starts
                if (!isSystemIdle() && !"ADMIN_TRIGGER".equals(trigger)) {
                    logger.info("🔬→🏗️ Project started mid-research — aborting remaining topics");
                    break;
                }

                ResearchTopic result = researchTopic(topic);
                if ("COMPLETED".equals(result.getStatus())) {
                    completedTopics.add(result);

                    // Step 3: Feed findings into learning system
                    integrateIntoLearningSystem(result);
                    totalResearchCount.incrementAndGet();
                }

                addToHistory(result);
            }

            cycleReport.put("completed", completedTopics.size());
            cycleReport.put("finishedAt", System.currentTimeMillis());

            // Step 4: Identify next gaps for future research
            queueFollowUpTopics(completedTopics);

            logger.info("✅ Research cycle complete: {}/{} topics researched",
                    completedTopics.size(), topics.size());

        } catch (Exception e) {
            logger.error("❌ Research cycle failed: {}", e.getMessage());
            cycleReport.put("error", e.getMessage());
        } finally {
            researchActive.set(false);
        }

        // Persist cycle report to Firebase
        persistCycleReport(cycleReport);

        return cycleReport;
    }

    /**
     * Select topics to research this cycle:
     * 1. Drain queued topics first
     * 2. Analyze knowledge gaps
     * 3. Pick from rotating domain questions
     */
    private List<ResearchTopic> selectResearchTopics() {
        List<ResearchTopic> selected = new ArrayList<>();

        // Priority 1: Custom queued topics
        while (!topicQueue.isEmpty() && selected.size() < MAX_TOPICS_PER_CYCLE) {
            selected.add(topicQueue.poll());
        }

        // Priority 2: Error-pattern-driven research
        if (selected.size() < MAX_TOPICS_PER_CYCLE) {
            ResearchTopic errorTopic = identifyErrorPatternTopic();
            if (errorTopic != null) {
                selected.add(errorTopic);
            }
        }

        // Priority 3: Knowledge gap analysis
        if (selected.size() < MAX_TOPICS_PER_CYCLE) {
            ResearchTopic gapTopic = identifyKnowledgeGap();
            if (gapTopic != null) {
                selected.add(gapTopic);
            }
        }

        // Priority 4: Rotating domain exploration
        while (selected.size() < MAX_TOPICS_PER_CYCLE) {
            ResearchTopic domainTopic = getNextDomainTopic();
            if (domainTopic != null) {
                selected.add(domainTopic);
            } else {
                break;
            }
        }

        return selected;
    }

    /**
     * Analyze recurring errors and create a research topic to solve them.
     */
    private ResearchTopic identifyErrorPatternTopic() {
        try {
            Map<String, Object> stats = learningService.getLearningStats();
            Object topErrorsObj = stats.get("topErrors");

            if (topErrorsObj instanceof List) {
                List<?> topErrors = (List<?>) topErrorsObj;
                if (!topErrors.isEmpty()) {
                    Object first = topErrors.get(0);
                    String errorCategory = "GENERAL";
                    if (first instanceof Map) {
                        Map<?, ?> firstMap = (Map<?, ?>) first;
                        Object catVal = firstMap.get("category");
                        if (catVal != null) {
                            errorCategory = String.valueOf(catVal);
                        }
                    }

                    ResearchTopic topic = new ResearchTopic();
                    topic.setDomain(errorCategory);
                    topic.setTopic("Error Pattern Deep-Dive");
                    topic.setQuestion("What are the root causes and permanent solutions for recurring "
                        + errorCategory + " errors in Spring Boot systems? Provide step-by-step prevention strategies.");
                    topic.setStatus("QUEUED");
                    topic.setSource("ERROR_PATTERN");
                    return topic;
                }
            }
        } catch (Exception e) {
            logger.debug("No error patterns to research: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Identify domains where our knowledge is weakest.
     */
    private ResearchTopic identifyKnowledgeGap() {
        try {
            // Count learnings per domain
            Map<String, Long> domainCounts = new HashMap<>();
            for (String domain : RESEARCH_DOMAINS) {
                long count = learningService.getTechniques(domain).size()
                           + learningService.getSolutionsFor(domain).size();
                domainCounts.put(domain, count);
                // Each getTechniques / getSolutionsFor is a Firebase read
                firebaseReadCount.addAndGet(2);
            }

            // Find the domain with least knowledge
            String weakestDomain = domainCounts.entrySet().stream()
                .min(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);

            if (weakestDomain != null) {
                List<String> questions = DOMAIN_QUESTIONS.getOrDefault(weakestDomain, List.of());
                if (!questions.isEmpty()) {
                    int idx = domainQuestionIndex.getOrDefault(weakestDomain, 0) % questions.size();
                    domainQuestionIndex.put(weakestDomain, idx + 1);

                    ResearchTopic topic = new ResearchTopic();
                    topic.setDomain(weakestDomain);
                    topic.setTopic("Knowledge Gap: " + weakestDomain);
                    topic.setQuestion(questions.get(idx));
                    topic.setStatus("QUEUED");
                    topic.setSource("GAP_ANALYSIS");
                    return topic;
                }
            }
        } catch (Exception e) {
            logger.debug("Gap analysis skipped: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get next topic from the rotating domain cycle.
     */
    private ResearchTopic getNextDomainTopic() {
        if (RESEARCH_DOMAINS.isEmpty()) return null;

        String domain = RESEARCH_DOMAINS.get(domainCycleIndex % RESEARCH_DOMAINS.size());
        domainCycleIndex++;

        List<String> questions = DOMAIN_QUESTIONS.getOrDefault(domain, List.of());
        if (questions.isEmpty()) return null;

        int idx = domainQuestionIndex.getOrDefault(domain, 0) % questions.size();
        domainQuestionIndex.put(domain, idx + 1);

        ResearchTopic topic = new ResearchTopic();
        topic.setDomain(domain);
        topic.setTopic("Domain Exploration: " + domain);
        topic.setQuestion(questions.get(idx));
        topic.setStatus("QUEUED");
        topic.setSource("TREND");
        return topic;
    }

    /**
     * Research a single topic using multi-AI consensus.
     */
    private ResearchTopic researchTopic(ResearchTopic topic) {
        topic.setStatus("RESEARCHING");
        logger.info("🔬 Researching [{}]: {}", topic.getDomain(), topic.getQuestion());

        try {
            // Ask multi-AI consensus for deep research
            ConsensusVote vote = consensusService.askAllAI(topic.getQuestion());

            if (vote == null || vote.getWinningResponse() == null
                    || vote.getWinningResponse().contains("[QUOTA_EXCEEDED]")
                    || vote.getWinningResponse().contains("[NO_PROVIDERS_CONFIGURED]")) {
                topic.setStatus("FAILED");
                topic.setSummary("Research skipped: AI providers unavailable or quota exceeded");
                return topic;
            }

            // Extract findings from the consensus result
            topic.setSummary(vote.getWinningResponse());
            topic.setConfidenceScore(vote.getConfidenceScore());
            topic.setResearchDepth(vote.getTotalResponses());

            // Parse findings from individual provider responses
            if (vote.getProviderResponses() != null) {
                for (Map.Entry<String, String> entry : vote.getProviderResponses().entrySet()) {
                    String finding = "[" + entry.getKey() + "] " + truncate(entry.getValue(), 500);
                    topic.addFinding(finding);
                }
            }

            // Extract learnings from the vote itself
            if (vote.getLearnings() != null) {
                topic.setActionableInsights(new ArrayList<>(vote.getLearnings()));
            }

            topic.setStatus("COMPLETED");
            topic.setCompletedAt(System.currentTimeMillis());
            logger.info("✅ Research complete [{}]: confidence={}, depth={}",
                    topic.getDomain(), topic.getConfidenceScore(), topic.getResearchDepth());

        } catch (Exception e) {
            topic.setStatus("FAILED");
            topic.setSummary("Research failed: " + e.getMessage());
            logger.warn("⚠️ Research failed for [{}]: {}", topic.getDomain(), e.getMessage());
        }

        return topic;
    }

    /**
     * Feed research findings into the learning system as techniques/patterns.
     */
    private void integrateIntoLearningSystem(ResearchTopic topic) {
        try {
            // Record as a technique
            List<String> steps = new ArrayList<>();
            if (topic.getSummary() != null) {
                steps.add("Consensus finding: " + truncate(topic.getSummary(), 800));
            }
            for (String insight : topic.getActionableInsights()) {
                steps.add("Insight: " + truncate(insight, 300));
            }

            Map<String, Object> context = new HashMap<>();
            context.put("kind", "IDLE_RESEARCH");
            context.put("source", topic.getSource());
            context.put("researchDepth", topic.getResearchDepth());
            context.put("topicId", topic.getId());
            context.put("confidence", topic.getConfidenceScore());

            learningService.recordTechnique(
                topic.getDomain(),
                "Research: " + truncate(topic.getTopic(), 100),
                topic.getSummary() != null ? truncate(topic.getSummary(), 500) : "No summary",
                steps,
                topic.getConfidenceScore() != null ? topic.getConfidenceScore() : 0.7,
                context
            );
            // Each write to the learning system counts as one Firebase write
            firebaseWriteCount.incrementAndGet();

            // Also record as a pattern for each actionable insight
            for (String insight : topic.getActionableInsights()) {
                learningService.recordPattern(
                    topic.getDomain(),
                    truncate(insight, 300),
                    "Discovered during idle research on: " + topic.getQuestion()
                );
                firebaseWriteCount.incrementAndGet();
            }

            logger.info("📚 Integrated research [{}] into learning system: {} insights",
                    topic.getDomain(), topic.getActionableInsights().size());

        } catch (Exception e) {
            logger.warn("⚠️ Failed to integrate research into learning: {}", e.getMessage());
        }
    }

    /**
     * Based on completed research, queue follow-up questions.
     */
    private void queueFollowUpTopics(List<ResearchTopic> completed) {
        for (ResearchTopic topic : completed) {
            if (topic.getConfidenceScore() != null && topic.getConfidenceScore() < 0.6) {
                // Low confidence → need deeper research
                queueResearchTopic(
                    topic.getDomain(),
                    "Expand on previous research about " + topic.getTopic()
                        + ". Previous answer had low confidence. Provide more detailed, "
                        + "concrete examples and implementation steps.",
                    "FOLLOW_UP"
                );
            }
        }
    }

    // ─────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────

    /**
     * Check if the system is idle (no project activity for IDLE_THRESHOLD_MS).
     */
    private boolean isSystemIdle() {
        long idleTime = System.currentTimeMillis() - lastProjectActivity.get();
        return idleTime >= IDLE_THRESHOLD_MS;
    }

    /** Reset quota counters if 24 hours have passed since the window start. */
    private void maybeResetQuotaWindow() {
        long now = System.currentTimeMillis();
        if (now - quotaWindowStartMs >= 86_400_000L) {
            firebaseWriteCount.set(0);
            firebaseReadCount.set(0);
            quotaWindowStartMs = now;
            logger.info("🔄 Firebase quota counters reset for new 24-hour window");
        }
    }

    /**
     * Returns true when both read and write counts are well below the free-tier limit.
     * Stops learning at 80 % of the limit to leave headroom for normal app operations.
     */
    private boolean isWithinFirebaseQuota() {
        maybeResetQuotaWindow();
        return firebaseWriteCount.get() < (FIREBASE_DAILY_WRITE_LIMIT * 0.8)
            && firebaseReadCount.get()  < (FIREBASE_DAILY_READ_LIMIT  * 0.8);
    }

    private void addToHistory(ResearchTopic topic) {
        researchHistory.add(topic);
        // Trim history
        while (researchHistory.size() > MAX_HISTORY) {
            researchHistory.remove(0);
        }
    }

    private void persistCycleReport(Map<String, Object> report) {
        try {
            if (firebaseService != null && firebaseService.isInitialized()) {
                String key = "research_cycle_" + System.currentTimeMillis();
                firebaseService.getDatabase()
                    .getReference("system/idle_research/" + key)
                    .setValueAsync(report);
                // Count this as one write operation
                firebaseWriteCount.incrementAndGet();
            }
        } catch (Exception e) {
            logger.debug("Could not persist research cycle to Firebase: {}", e.getMessage());
        }
    }

    private Map<String, Object> topicToMap(ResearchTopic topic) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", topic.getId());
        map.put("domain", topic.getDomain());
        map.put("topic", topic.getTopic());
        map.put("question", topic.getQuestion());
        map.put("status", topic.getStatus());
        map.put("confidence", topic.getConfidenceScore());
        map.put("researchDepth", topic.getResearchDepth());
        map.put("source", topic.getSource());
        map.put("createdAt", topic.getCreatedAt());
        map.put("completedAt", topic.getCompletedAt());
        map.put("insightsCount", topic.getActionableInsights().size());
        return map;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
