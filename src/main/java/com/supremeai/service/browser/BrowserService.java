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
    private final BrowserTaskRepository taskRepository;
    private final BrowserFindingRepository findingRepository;

    private boolean isActive = false;
    private boolean autoLearnEnabled = true;
    private int pagesVisited = 0;
    private LocalDateTime startTime;
    private boolean pausedForAuth = false;

    public BrowserService(BrowserActivityRepository activityRepository,
                          StoredCredentialRepository credentialRepository,
                          UrlPermissionRepository urlPermissionRepository,
                          UrlPermissionRequestRepository urlPermissionRequestRepository,
                          SystemLearningRepository systemLearningRepository,
                          BrowserTaskRepository taskRepository,
                          BrowserFindingRepository findingRepository) {
        this.activityRepository = activityRepository;
        this.credentialRepository = credentialRepository;
        this.urlPermissionRepository = urlPermissionRepository;
        this.urlPermissionRequestRepository = urlPermissionRequestRepository;
        this.systemLearningRepository = systemLearningRepository;
        this.taskRepository = taskRepository;
        this.findingRepository = findingRepository;
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

    public Flux<BrowserActivity> getRecentActivity() {
        return activityRepository.findAllByOrderByTimestampDesc().take(50);
    }

    public Flux<StoredCredential> getAllCredentials() {
        return credentialRepository.findAll();
    }

    public Mono<StoredCredential> saveCredential(StoredCredential credential) {
        if (credential.getId() == null) {
            credential.setId(UUID.randomUUID().toString());
        }
        return credentialRepository.save(credential)
            .doOnSuccess(c -> logger.info("Saved credentials for {}", c.getWebsite()));
    }

    public Flux<UrlPermission> getAllowedUrls() {
        return urlPermissionRepository.findByType("allowed");
    }

    public Flux<UrlPermission> getDeniedUrls() {
        return urlPermissionRepository.findByType("denied");
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
        return activityRepository.findById(activityId)
            .flatMap(activity -> {
                activity.setStatus("completed");
                return activityRepository.save(activity);
            })
            .then();
    }

    public Mono<Void> skipAuth(String activityId) {
        this.pausedForAuth = false;
        return activityRepository.findById(activityId)
            .flatMap(activity -> {
                activity.setStatus("error");
                activity.setElementText("Authentication skipped by user");
                return activityRepository.save(activity);
            })
            .then();
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
}
