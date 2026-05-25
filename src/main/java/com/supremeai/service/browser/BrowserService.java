package com.supremeai.service.browser;

import com.supremeai.model.SystemLearning;
import com.supremeai.model.browser.*;
import com.supremeai.repository.SystemLearningRepository;
import com.supremeai.repository.browser.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class BrowserService {
    private static final Logger logger = LoggerFactory.getLogger(BrowserService.class);

    private final BrowserActivityRepository activityRepository;
    private final StoredCredentialRepository credentialRepository;
    private final UrlPermissionRepository urlPermissionRepository;
    private final UrlPermissionRequestRepository urlPermissionRequestRepository;
    private final SystemLearningRepository systemLearningRepository;
    private final BrowserFindingRepository findingRepository;
    private final org.springframework.web.reactive.function.client.WebClient webClient;
    private final String automationUrl;

    private final BrowserTaskRepository taskRepository;
    private final com.supremeai.service.VisionService visionService;
    private final com.supremeai.security.EncryptionService encryptionService;
    private final com.supremeai.service.SystemWorkRuleService ruleService;

    private boolean isActive = false;
    private boolean autoLearnEnabled = true;
    private int pagesVisited = 0;
    private LocalDateTime startTime;
    private boolean pausedForAuth = false;
    private String pausedActivityId = null;

    public BrowserService(BrowserActivityRepository activityRepository,
                          StoredCredentialRepository credentialRepository,
                          UrlPermissionRepository urlPermissionRepository,
                          UrlPermissionRequestRepository urlPermissionRequestRepository,
                          SystemLearningRepository systemLearningRepository,
                          BrowserTaskRepository taskRepository,
                          BrowserFindingRepository findingRepository,
                          com.supremeai.service.VisionService visionService,
                          com.supremeai.security.EncryptionService encryptionService,
                          com.supremeai.service.SystemWorkRuleService ruleService,
                           org.springframework.web.reactive.function.client.WebClient webClient,
                          @org.springframework.beans.factory.annotation.Value("${supremeai.browser.automation-url}") String automationUrl) {
        this.activityRepository = activityRepository;
        this.credentialRepository = credentialRepository;
        this.urlPermissionRepository = urlPermissionRepository;
        this.urlPermissionRequestRepository = urlPermissionRequestRepository;
        this.systemLearningRepository = systemLearningRepository;
        this.taskRepository = taskRepository;
        this.findingRepository = findingRepository;
        this.visionService = visionService;
        this.encryptionService = encryptionService;
        this.ruleService = ruleService;
        this.webClient = webClient;
        this.automationUrl = automationUrl;
        this.startTime = LocalDateTime.now();
    }

    public Mono<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("isActive", isActive);
        status.put("pagesVisited", pagesVisited);
        status.put("startTime", startTime.toString());
        status.put("pausedForAuth", pausedForAuth);
        return Mono.just(Map.of("status", status));
    }

    public Mono<Void> startBrowsing() {
        this.isActive = true;
        this.startTime = LocalDateTime.now();
        logger.info("Browser activity started");
        return Mono.empty();
    }

    public Mono<Void> stopBrowsing() {
        this.isActive = false;
        logger.info("Browser activity stopped");
        return Mono.empty();
    }

    public Mono<String> getScreenshot() {
        return webClient.get()
            .uri(automationUrl + "/screenshot")
            .retrieve()
            .bodyToMono(Map.class)
            .map(res -> (String) res.get("screenshot"));
    }

    public Mono<Map<String, Object>> getAccessibilityTree() {
        return webClient.get()
            .uri(automationUrl + "/accessibility")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    public Mono<Void> navigateTo(String url) {
        return ruleService.getRuleByKey("BROWSER_AUTO_MODE")
            .flatMap(rule -> {
                if (!Boolean.parseBoolean(rule.getValue())) {
                    logger.warn("Browser Automation is disabled by system rule BROWSER_AUTO_MODE");
                    return Mono.error(new IllegalStateException("Browser Automation is disabled."));
                }
                return ruleService.getRuleByKey("BROWSER_RESTRICTED_URLS");
            })
            .flatMap(restrictedUrlsRule -> {
                String restricted = restrictedUrlsRule.getValue();
                if (restricted != null && !restricted.isEmpty()) {
                    for (String pattern : restricted.split(",")) {
                        if (url.contains(pattern.trim())) {
                            logger.warn("Blocked navigation to restricted URL (System Rule): {}", url);
                            return recordActivity(url, "blocked", "Security", "URL blocked by system rule: " + url).then(Mono.empty());
                        }
                    }
                }
                
                return urlPermissionRepository.findByType("denied")
                    .filter(p -> url.contains(p.getPattern()))
                    .hasElements()
                    .flatMap(isDenied -> {
                        if (isDenied) {
                            logger.warn("Blocked navigation to restricted URL (Local Policy): {}", url);
                            return recordActivity(url, "blocked", "Security", "URL blocked by policy: " + url).then(Mono.empty());
                        }
                        return webClient.post()
                            .uri(automationUrl + "/navigate")
                            .bodyValue(Map.of("url", url))
                            .retrieve()
                            .bodyToMono(Void.class)
                            .then(recordActivity(url, "navigate", "Navigation", "Navigating to requested URL: " + url))
                            .then();
                    });
            })
            .then();
    }

    public Mono<Void> click(String selector) {
        return ruleService.getRuleByKey("BROWSER_AUTO_MODE")
            .flatMap(rule -> {
                if (!Boolean.parseBoolean(rule.getValue())) {
                    logger.warn("Click action blocked: Browser Automation is disabled by BROWSER_AUTO_MODE");
                    return Mono.error(new IllegalStateException("Browser Automation is disabled."));
                }
                return webClient.post()
                    .uri(automationUrl + "/click")
                    .bodyValue(Map.of("selector", selector))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .then(recordActivity("", "click", "Click", "Clicked element: " + selector))
                    .then();
            });
    }

    public Mono<Void> fill(String selector, String value) {
        return ruleService.getRuleByKey("BROWSER_AUTO_MODE")
            .flatMap(rule -> {
                if (!Boolean.parseBoolean(rule.getValue())) {
                    logger.warn("Fill action blocked: Browser Automation is disabled by BROWSER_AUTO_MODE");
                    return Mono.error(new IllegalStateException("Browser Automation is disabled."));
                }
                return webClient.post()
                    .uri(automationUrl + "/fill")
                    .bodyValue(Map.of("selector", selector, "value", value))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .then(recordActivity("", "fill", "Fill", "Filled " + selector + " with value"))
                    .then();
            });
    }

    public Mono<Void> clickAt(int x, int y) {
        return ruleService.getRuleByKey("BROWSER_AUTO_MODE")
            .flatMap(rule -> {
                if (!Boolean.parseBoolean(rule.getValue())) {
                    logger.warn("ClickAt action blocked: Browser Automation is disabled by BROWSER_AUTO_MODE");
                    return Mono.error(new IllegalStateException("Browser Automation is disabled."));
                }
                return webClient.post()
                    .uri(automationUrl + "/click-at")
                    .bodyValue(Map.of("x", x, "y", y))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .then(recordActivity("", "click", "Click", "Clicked at (" + x + ", " + y + ")"))
                    .then();
            });
    }

    public Mono<Void> typeKey(String key) {
        return ruleService.getRuleByKey("BROWSER_AUTO_MODE")
            .flatMap(rule -> {
                if (!Boolean.parseBoolean(rule.getValue())) {
                    logger.warn("TypeKey action blocked: Browser Automation is disabled by BROWSER_AUTO_MODE");
                    return Mono.error(new IllegalStateException("Browser Automation is disabled."));
                }
                return webClient.post()
                    .uri(automationUrl + "/type-key")
                    .bodyValue(Map.of("key", key))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .then(recordActivity("", "type", "Type", "Typed key: " + key))
                    .then();
            });
    }

    public Mono<Void> scroll(String direction) {
        return ruleService.getRuleByKey("BROWSER_AUTO_MODE")
            .flatMap(rule -> {
                if (!Boolean.parseBoolean(rule.getValue())) {
                    logger.warn("Scroll action blocked: Browser Automation is disabled by BROWSER_AUTO_MODE");
                    return Mono.error(new IllegalStateException("Browser Automation is disabled."));
                }
                return webClient.post()
                    .uri(automationUrl + "/scroll")
                    .bodyValue(Map.of("direction", direction))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .then(recordActivity("", "scroll", "Scroll", "Scrolled page " + direction))
                    .then();
            });
    }

    public Mono<Void> waitMs(int ms) {
        return Mono.delay(java.time.Duration.ofMillis(ms)).then();
    }

    public Flux<BrowserActivity> getRecentActivity() {
        return activityRepository.findAllByOrderByTimestampDesc().take(50);
    }

    public Flux<StoredCredential> getAllCredentials(String userId) {
        return credentialRepository.findByUserId(userId)
            .map(c -> {
                c.setPassword("[ENCRYPTED]");
                if (c.getToken() != null && !c.getToken().isEmpty()) {
                    c.setToken("[ENCRYPTED]");
                }
                return c;
            });
    }

    public Mono<StoredCredential> saveCredential(StoredCredential credential) {
        if (credential.getId() == null) {
            credential.setId(UUID.randomUUID().toString());
        }
        if (credential.getUserId() == null || credential.getUserId().isEmpty()) {
            credential.setUserId("default");
        }
        
        // Encrypt sensitive data
        if (credential.getPassword() != null && !credential.getPassword().isEmpty()) {
            credential.setPassword(encryptionService.encrypt(credential.getPassword()));
        }
        if (credential.getToken() != null && !credential.getToken().isEmpty()) {
            credential.setToken(encryptionService.encrypt(credential.getToken()));
        }
        
        return credentialRepository.save(credential)
            .doOnSuccess(c -> logger.info("Saved encrypted credentials for {}", c.getWebsite()));
    }

    public Mono<Void> deleteCredential(String id) {
        return credentialRepository.deleteById(id);
    }

    public Flux<UrlPermission> getAllowedUrls(String userId) {
        return urlPermissionRepository.findByUserIdAndType(userId, "allowed");
    }

    public Flux<UrlPermission> getDeniedUrls(String userId) {
        return urlPermissionRepository.findByUserIdAndType(userId, "denied");
    }

    public Mono<Boolean> isUrlAllowed(String userId, String url) {
        return urlPermissionRepository.findByUserIdAndType(userId, "allowAll")
            .hasElements()
            .flatMap(allowAll -> {
                if (allowAll) return Mono.just(true);
                return urlPermissionRepository.findByUserIdAndType(userId, "denied")
                    .filter(p -> url.contains(p.getPattern()))
                    .hasElements()
                    .map(isDenied -> !isDenied);
            });
    }

    public Flux<UrlPermissionRequest> getPermissionRequests() {
        return urlPermissionRequestRepository.findAll();
    }

    public Mono<UrlPermission> addUrlPermission(UrlPermission permission) {
        if (permission.getId() == null) {
            permission.setId(UUID.randomUUID().toString());
        }
        return urlPermissionRepository.save(permission);
    }

    public Mono<UrlPermission> updateUrlPermission(String id, UrlPermission permission) {
        permission.setId(id);
        return urlPermissionRepository.save(permission);
    }

    public Mono<Void> deleteUrlPermission(String id) {
        return urlPermissionRepository.deleteById(id);
    }

    public Mono<Void> processPermissionDecision(String requestId, boolean approved) {
        return urlPermissionRequestRepository.findById(requestId)
            .flatMap(request -> {
                request.setStatus(approved ? "approved" : "denied");
                return urlPermissionRequestRepository.save(request)
                    .then(approved ? 
                        addUrlPermission(new UrlPermission(request.getUrl(), request.getPattern(), "allowed")) : 
                        addUrlPermission(new UrlPermission(request.getUrl(), request.getPattern(), "denied")))
                    .then();
            });
    }

    public Mono<Map<String, Object>> getSystemLearningStatus() {
        return systemLearningRepository.count()
            .map(count -> Map.of(
                "knowledgeNodes", count,
                "lastSync", LocalDateTime.now().toString(),
                "autoLearnEnabled", autoLearnEnabled
            ));
    }

    public Mono<Void> toggleAutoLearn(boolean enabled) {
        this.autoLearnEnabled = enabled;
        logger.info("Auto-learning {}", enabled ? "enabled" : "disabled");
        return Mono.empty();
    }

    public Mono<Void> resumeActivity(String activityId) {
        this.pausedForAuth = false;
        this.pausedActivityId = null;
        logger.info("Resumed browser activity {}", activityId);
        return Mono.empty();
    }

    public Mono<Void> skipAuth(String activityId) {
        this.pausedForAuth = false;
        this.pausedActivityId = null;
        logger.info("Skipped auth for activity {}", activityId);
        return Mono.empty();
    }

    public Mono<Void> pauseForManualCredential(String activityId) {
        this.pausedForAuth = true;
        this.pausedActivityId = activityId;
        logger.info("Paused for manual credential entry: {}", activityId);
        return Mono.empty();
    }

    public Mono<Map<String, Object>> getPausedState() {
        return Mono.just(Map.of(
            "paused", pausedForAuth,
            "activityId", pausedActivityId != null ? pausedActivityId : ""
        ));
    }

    public Flux<BrowserTask> getActiveTasks() {
        return taskRepository.findAll();
    }

    public Mono<BrowserTask> createActivityTask(String goal) {
        BrowserTask task = new BrowserTask();
        task.setId(UUID.randomUUID().toString());
        task.setGoal(goal);
        return taskRepository.save(task);
    }

    public Mono<Void> deleteTask(String id) {
        return taskRepository.deleteById(id);
    }

    public Flux<BrowserFinding> getFindingsForTask(String taskId) {
        return findingRepository.findByTaskId(taskId);
    }

    public Mono<BrowserFinding> createFinding(BrowserFinding finding) {
        if (finding.getId() == null) {
            finding.setId(UUID.randomUUID().toString());
        }
        return findingRepository.save(finding)
            .flatMap(saved -> {
                // Link finding to SystemLearning
                SystemLearning learning = new SystemLearning();
                learning.setId(UUID.randomUUID().toString());
                learning.setTopic("Finding: " + saved.getTitle());
                learning.setCategory(saved.getType());
                learning.setContent(saved.getContent());
                learning.setConfidenceScore(saved.getConfidence());
                learning.setLearnedAt(LocalDateTime.now());
                learning.setSources(java.util.List.of(saved.getUrl()));
                return systemLearningRepository.save(learning).thenReturn(saved);
            });
    }

    /**
     * Intelligent logic: Record a new browser activity and potentially learn from it.
     */
    public Mono<BrowserActivity> recordActivity(String url, String action, String title, String reasoning) {
        pagesVisited++;
        BrowserActivity activity = new BrowserActivity(UUID.randomUUID().toString(), url, action);
        activity.setTitle(title);
        activity.setReasoning(reasoning != null ? reasoning : "Autonomous exploration of " + url);
        
        // Intelligent check: Is this a new site?
        return checkAndRequestPermission(url)
            .then(autoLearn(url, title))
            .then(activityRepository.save(activity));
    }

    private Mono<Void> checkAndRequestPermission(String url) {
        // Semantic Auto-Approval Logic
        double trustScore = calculateTrustScore(url);
        
        if (trustScore > 0.8) {
            logger.info("Auto-approving high-trust URL: {} (Score: {})", url, trustScore);
            return addUrlPermission(new UrlPermission(url, url.split("/")[2], "allowed")).then();
        }

        return urlPermissionRepository.findAll()
            .filter(p -> url.contains(p.getPattern()))
            .collectList()
            .flatMap(list -> {
                if (list.isEmpty()) {
                    UrlPermissionRequest request = new UrlPermissionRequest();
                    request.setId(UUID.randomUUID().toString());
                    request.setUrl(url);
                    request.setPattern(url.split("/")[2]);
                    request.setReason("System attempted to access unverified URL. Trust Score: " + trustScore);
                    return urlPermissionRequestRepository.save(request).then();
                }
                return Mono.empty();
            });
    }

    private double calculateTrustScore(String url) {
        // Mock semantic trust score calculation
        if (url.contains(".gov") || url.contains(".edu") || url.contains("github.com") || url.contains("wikipedia.org")) {
            return 0.95;
        }
        if (url.contains("google.com") || url.contains("microsoft.com") || url.contains("spring.io")) {
            return 0.9;
        }
        return 0.5;
    }

    private Mono<Void> autoLearn(String url, String title) {
        if (!autoLearnEnabled) return Mono.empty();
        
        // Intelligent logic: generate a system learning node from the visit
        SystemLearning learning = new SystemLearning();
        learning.setId(UUID.randomUUID().toString());
        learning.setTopic("Web Research: " + title);
        learning.setCategory("RESEARCH");
        learning.setContent("System learned about " + title + " from " + url);
        learning.setConfidenceScore(0.75);
        learning.setLearnedAt(LocalDateTime.now());
        learning.setPermanent(false);
        learning.setLearningType("NLP");
        learning.setTags(java.util.List.of("browser", "auto-learned"));
        
        return systemLearningRepository.save(learning).then();
    }

    /**
     * Executes a single intelligent step for a browser task.
     */
    public Mono<Void> executeAutonomousStep(String taskId) {
        return ruleService.getRuleByKey("BROWSER_MAX_STEPS")
            .onErrorResume(e -> Mono.just(new com.supremeai.model.SystemWorkRule("BROWSER_MAX_STEPS", "AUTOMATION", "Max steps", "15")))
            .flatMap(maxStepsRule -> {
                int maxSteps = Math.min(15, Integer.parseInt(maxStepsRule.getValue()));
                return ruleService.getRuleByKey("BROWSER_TIMEOUT_MINUTES")
                    .onErrorResume(e -> Mono.just(new com.supremeai.model.SystemWorkRule("BROWSER_TIMEOUT_MINUTES", "AUTOMATION", "Timeout in minutes", "5")))
                    .flatMap(timeoutRule -> {
                        int timeoutMinutes = Integer.parseInt(timeoutRule.getValue());
                        return taskRepository.findById(taskId)
                            .flatMap(task -> {
                                if (pagesVisited >= maxSteps) {
                                    logger.warn("Browser task {} exceeded max steps ({})", taskId, maxSteps);
                                    task.setStatus("error");
                                    task.setErrorMessage("Maximum automation steps reached.");
                                    return taskRepository.save(task).then(Mono.empty());
                                }
                                if (task.getStartedAt() != null && 
                                    java.time.Duration.between(task.getStartedAt(), LocalDateTime.now()).toMinutes() >= timeoutMinutes) {
                                    logger.warn("Browser task {} timed out (duration > {} minutes)", taskId, timeoutMinutes);
                                    task.setStatus("error");
                                    task.setErrorMessage("Automation task execution timed out.");
                                    return taskRepository.save(task).then(Mono.empty());
                                }
                                return Mono.zip(
                                        getScreenshot(),
                                        getAccessibilityTree(),
                                        getCredentialContext(task.getLastUrl()))
                            .flatMap(tuple -> {
                                String base64 = tuple.getT1();
                                Map<String, Object> accessibilityTree = tuple.getT2();
                                String credContext = tuple.getT3();
                                
                                String prompt = "You are the SupremeAI Autonomous Browser Agent. Your objective is to think step-by-step to achieve the user's goal.\n" +
                                    "Current Goal: " + task.getGoal() + "\n" +
                                    "Current URL: " + task.getLastUrl() + "\n\n" +
                                    credContext + "\n\n" +
                                    "CRITICAL INSTRUCTIONS:\n" +
                                    "1. Examine the screenshot visually and use the Accessibility Tree for precise element targeting.\n" +
                                    "2. If you see overlays (cookies, popups), dismiss them first.\n" +
                                    "3. If the target information is not visible, use SCROLL to find it.\n" +
                                    "4. If you have found the requested data, use EXTRACT to save it.\n" +
                                    "5. Once the goal is fully achieved, use FINISH with a concise summary.\n\n" +
                                    "Accessibility Tree (JSON): " + accessibilityTree.toString() + "\n\n" +
                                    "Available Action Formats:\n" +
                                    "- NAVIGATE:URL:Reasoning\n" +
                                    "- CLICK:SELECTOR:Reasoning\n" +
                                    "- CLICK_AT:X,Y:Reasoning\n" +
                                    "- TYPE:SELECTOR:VALUE:Reasoning\n" +
                                    "- TYPE_KEY:KEY:Reasoning\n" +
                                    "- SCROLL:DIRECTION:Reasoning (DIRECTION can be 'down', 'up', 'top', 'bottom')\n" +
                                    "- WAIT:MS:Reasoning (e.g. 2000)\n" +
                                    "- EXTRACT:JSON_DATA:Reasoning (Save findings as JSON)\n" +
                                    "- FINISH:SUMMARY:Reasoning\n\n" +
                                    "Response Format: Exactly one line like 'ACTION:VALUE:REASONING'";

                                return visionService.analyzeImage(base64, com.supremeai.service.VisionService.AnalysisType.GENERAL)
                                    .timeout(java.time.Duration.ofSeconds(30))
                                    .flatMap(analysis -> {
                                        String response = analysis.getSummary().trim();
                                        if (response.contains("\n")) {
                                            response = response.split("\n")[0];
                                        }
                                        
                                        logger.info("AI Decision for task {}: {}", taskId, response);
                                        
                                        try {
                                            String[] parts = response.split(":", 3);
                                            String action = parts[0].toUpperCase();
                                            String value = parts.length > 1 ? parts[1] : "";
                                            String reasoning = parts.length > 2 ? parts[2] : "No reasoning provided";
                                            
                                            switch (action) {
                                                case "NAVIGATE":
                                                    return navigateTo(value).then(recordActivity(value, "navigate", "AI Agent", reasoning)).then();
                                                case "CLICK":
                                                    return click(value).then(recordActivity("", "click", "AI Agent", reasoning)).then();
                                                case "CLICK_AT":
                                                    String[] coords = value.split(",");
                                                    int x = Integer.parseInt(coords[0].trim());
                                                    int y = Integer.parseInt(coords[1].trim());
                                                    return clickAt(x, y).then(recordActivity("", "click", "AI Agent", reasoning)).then();
                                                case "TYPE":
                                                    String[] typeParts = value.split(":", 2);
                                                    return fill(typeParts[0], typeParts[1]).then(recordActivity("", "type", "AI Agent", reasoning)).then();
                                                case "TYPE_KEY":
                                                    return typeKey(value).then(recordActivity("", "type", "AI Agent", reasoning)).then();
                                                case "SCROLL":
                                                    return scroll(value).then(recordActivity("", "scroll", "AI Agent", reasoning)).then();
                                                case "WAIT":
                                                    int ms = Integer.parseInt(value.replaceAll("[^0-9]", ""));
                                                    return waitMs(ms).then(recordActivity("", "wait", "AI Agent", reasoning)).then();
                                                case "EXTRACT":
                                                    BrowserFinding finding = new BrowserFinding();
                                                    finding.setTaskId(taskId);
                                                    finding.setTitle("AI Extraction");
                                                    finding.setContent(value);
                                                    finding.setType("RESEARCH");
                                                    finding.setConfidence(0.9);
                                                    finding.setUrl(task.getLastUrl());
                                                    return createFinding(finding).then();
                                                case "FINISH":
                                                    task.setStatus("completed");
                                                    task.setCompletedAt(LocalDateTime.now());
                                                    task.setProgress(100);
                                                    return taskRepository.save(task)
                                                        .then(recordActivity("", "finish", "AI Agent", reasoning))
                                                        .then(recordStrategicLearning(task.getId()));
                                                default:
                                                    logger.warn("Unknown AI action: {}", action);
                                                    return Mono.empty();
                                            }
                                        } catch (Exception e) {
                                            logger.error("Failed to parse AI response: {}", response, e);
                                            return Mono.empty();
                                        }
                                    });
                            });
                    });
            });
        });
    }

    private Mono<String> getCredentialContext(String url) {
        if (url == null) return Mono.just("");
        String host = url.contains("://") ? url.split("/")[2] : url;

        return credentialRepository.findAll()
            .filter(c -> host.contains(c.getWebsite()) || url.contains(c.getWebsite()))
            .collectList()
            .map(list -> {
                if (list.isEmpty()) return "";
                StoredCredential c = list.get(0);

                // SECURITY: Never include decrypted passwords or tokens in context strings
                // that may be sent to AI providers or logged. Credentials are injected
                // directly into the Playwright browser layer, not into AI prompts.
                StringBuilder context = new StringBuilder("SECURITY CONTEXT: You have valid credentials for this domain.\n");
                context.append("- Username: ").append(c.getUsername()).append("\n");
                context.append("- Password: [REDACTED — injected directly into browser automation layer]\n");
                context.append("- Access Token: [REDACTED — injected directly into browser automation layer]\n");
                context.append("- Target Selectors: User(").append(c.getSelectorUsername() != null ? c.getSelectorUsername() : "autodetect")
                       .append("), Pass(").append(c.getSelectorPassword() != null ? c.getSelectorPassword() : "autodetect").append(")\n");
                context.append("If you encounter a login wall, the automation layer will inject credentials automatically.");

                return context.toString();
            });
    }

    /**
     * Synthesizes the entire mission history into a strategic learning node.
     * This directly aids in app generation and decision making.
     */
    private Mono<Void> recordStrategicLearning(String taskId) {
        return taskRepository.findById(taskId)
            .flatMap(task -> activityRepository.findAllByOrderByTimestampDesc()
                .filter(act -> act.getReasoning() != null && act.getReasoning().contains("AI Agent"))
                .collectList()
                .flatMap(activities -> {
                    String historyText = activities.stream()
                        .map(a -> a.getAction() + ": " + a.getReasoning())
                        .reduce("", (a, b) -> a + "\n" + b);

                    String prompt = "Analyze the following browsing history and the initial goal. \n" +
                        "Goal: " + task.getGoal() + "\n" +
                        "History: " + historyText + "\n\n" +
                        "Generate a STRATEGIC BLUEPRINT for SupremeAI's learning system. \n" +
                        "Include:\n" +
                        "1. HOW TO START similar apps/tasks.\n" +
                        "2. VOTING CRITERIA: What makes this successful vs failure.\n" +
                        "3. COMPONENT SUGGESTIONS: Based on what you saw on the web.\n" +
                        "Response Format: A detailed structured summary for app generation.";

                    return visionService.analyzeImage(null, com.supremeai.service.VisionService.AnalysisType.GENERAL)
                        .flatMap(analysis -> {
                            SystemLearning strategicLearning = new SystemLearning();
                            strategicLearning.setId(UUID.randomUUID().toString());
                            strategicLearning.setTopic("Strategic Blueprint: " + task.getGoal());
                            strategicLearning.setCategory("APP_GENERATION");
                            strategicLearning.setLearningType("APP_GENERATION");
                            strategicLearning.setContent(analysis.getSummary());
                            strategicLearning.setConfidenceScore(0.85);
                            strategicLearning.setLearnedAt(LocalDateTime.now());
                            strategicLearning.setTags(java.util.List.of("blueprint", "automation", "voting-logic"));
                            
                            Map<String, Object> metadata = new HashMap<>();
                            metadata.put("taskId", taskId);
                            metadata.put("sourceUrl", task.getLastUrl());
                            strategicLearning.setMetadata(metadata);

                            return systemLearningRepository.save(strategicLearning).then();
                        });
                })
            );
    }

    /**
     * Creates a background browser task and returns the task ID.
     * Persists to Firestore via {@link BrowserTaskRepository} with status {@code active}.
     */
    public Mono<String> createTask(String description) {
        BrowserTask task = new BrowserTask(description);
        task.setStatus("active");
        task.setProgress(0);
        return taskRepository.save(task)
            .map(saved -> {
                logger.info("Created browser task: {} — goal: {}", saved.getId(), description);
                return saved.getId();
            });
    }
}
