package com.supremeai.controller;

import com.supremeai.repository.UserRepository;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
public class WebSocketController {

  private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

  @Autowired private SimpMessagingTemplate messagingTemplate;

  @Autowired private UserRepository userRepository;

  @MessageMapping("/dashboard/subscribe")
  @SendToUser("/topic/dashboard")
  @PreAuthorize("hasRole('USER')")
  public Mono<Map<String, Object>> subscribeToDashboard(Principal principal) {
    return getDashboardData();
  }

  @Scheduled(fixedRate = 30000) // Update every 30 seconds
  public void broadcastDashboardUpdates() {
    getDashboardData()
        .subscribe(data -> messagingTemplate.convertAndSend("/topic/dashboard", data));
  }

  @Scheduled(fixedRate = 10000)
  public void broadcastQuotaUpdates() {
    getGlobalQuotaData()
        .flatMap(
            globalData ->
                userRepository
                    .findAll()
                    .collectList()
                    .map(
                        users -> {
                          Map<String, Object> data = new HashMap<>(globalData);
                          List<Map<String, Object>> userQuotas =
                              users.stream()
                                  .map(
                                      user -> {
                                        Map<String, Object> uq = new HashMap<>();
                                        uq.put("userId", user.getFirebaseUid());
                                        uq.put("displayName", user.getDisplayName());
                                        uq.put("email", user.getEmail());
                                        uq.put("usedQuota", user.getCurrentUsage());
                                        uq.put("totalQuota", user.fetchMonthlyQuota());
                                        uq.put(
                                            "usagePercentage",
                                            user.fetchMonthlyQuota() > 0
                                                ? (double) user.getCurrentUsage()
                                                    / user.fetchMonthlyQuota()
                                                    * 100.0
                                                : 0.0);
                                        return uq;
                                      })
                                  .toList();
                          data.put("userQuotas", userQuotas);
                          return data;
                        }))
        .subscribe(quotaData -> messagingTemplate.convertAndSend("/topic/quota", quotaData));
  }

  /**
   * Broadcast pipeline deployment notification to all connected admins. Called from
   * GitHubWebhookController or DeploymentService.
   */
  public void broadcastPipelineNotification(String status, String message, String details) {
    Map<String, Object> notification = new HashMap<>();
    notification.put("type", "GITHUB_PIPELINE");
    notification.put("status", status.toLowerCase()); // success, failure, warning
    notification.put("message", message);
    notification.put("details", details);
    notification.put("timestamp", System.currentTimeMillis());

    messagingTemplate.convertAndSend("/topic/notifications", notification);
    log.info("Broadcast pipeline notification: {} - {}", status, message);
  }

  /** Broadcast system alert to all connected users. */
  public void broadcastSystemAlert(String level, String message) {
    Map<String, Object> alert = new HashMap<>();
    alert.put("type", "SYSTEM_ALERT");
    alert.put("status", level.toLowerCase()); // info, warning, error
    alert.put("message", message);
    alert.put("timestamp", System.currentTimeMillis());

    messagingTemplate.convertAndSend("/topic/notifications", alert);
    log.info("Broadcast system alert: {} - {}", level, message);
  }

  /**
   * Broadcast analysis job progress update. Called from ProjectAnalysisService during file/agent
   * scanning.
   */
  public void broadcastAnalysisProgress(
      String jobId,
      String projectName,
      String phase,
      int filesProcessed,
      int totalFiles,
      String currentAgent,
      int findingsSoFar,
      String message) {
    Map<String, Object> progress = new HashMap<>();
    progress.put("type", "ANALYSIS_PROGRESS");
    progress.put("jobId", jobId);
    progress.put("projectName", projectName);
    progress.put("phase", phase); // EXTRACTING, CHUNKING, SCANNING, FIXING, COMPLETED
    progress.put("filesProcessed", filesProcessed);
    progress.put("totalFiles", totalFiles);
    progress.put("currentAgent", currentAgent);
    progress.put("findingsSoFar", findingsSoFar);
    progress.put("message", message);
    progress.put("timestamp", System.currentTimeMillis());

    // Send to both general analysis topic and job-specific queue for isolation
    messagingTemplate.convertAndSend("/topic/analysis", progress);
    messagingTemplate.convertAndSend("/topic/analysis/" + jobId, progress);
    log.debug(
        "[AnalysisProgress] Job {}: {} - {}/{} files, agent={}, findings={}",
        jobId,
        phase,
        filesProcessed,
        totalFiles,
        currentAgent,
        findingsSoFar);
  }

  /** Broadcast analysis job completion event. */
  public void broadcastAnalysisCompletion(
      String jobId,
      String projectName,
      int totalFindings,
      Map<String, Integer> severitySummary,
      long durationMs) {
    Map<String, Object> completion = new HashMap<>();
    completion.put("type", "ANALYSIS_COMPLETE");
    completion.put("jobId", jobId);
    completion.put("projectName", projectName);
    completion.put("totalFindings", totalFindings);
    completion.put("severitySummary", severitySummary);
    completion.put("durationMs", durationMs);
    completion.put("timestamp", System.currentTimeMillis());

    messagingTemplate.convertAndSend("/topic/analysis", completion);
    messagingTemplate.convertAndSend("/topic/analysis/" + jobId, completion);
    log.info("[AnalysisComplete] Job {}: {} findings in {}ms", jobId, totalFindings, durationMs);
  }

  /** Broadcast learning update (new pattern learned) */
  public void broadcastLearningUpdate(String patternType, int count) {
    Map<String, Object> update = new HashMap<>();
    update.put("type", "LEARNING_UPDATE");
    update.put("status", "info");
    update.put("message", String.format("Learned %d new %s patterns", count, patternType));
    update.put("patternType", patternType);
    update.put("count", count);
    update.put("timestamp", System.currentTimeMillis());

    messagingTemplate.convertAndSend("/topic/notifications", update);
  }

  /** Broadcast system event for the learning dashboard */
  public void broadcastSystemEvent(
      String type, String domainId, Double progress, String fact, String message) {
    Map<String, Object> event = new HashMap<>();
    event.put("type", type);
    if (domainId != null) event.put("domainId", domainId);
    if (progress != null) event.put("progress", progress);
    if (fact != null) event.put("fact", fact);
    if (message != null) event.put("message", message);
    event.put("timestamp", System.currentTimeMillis());

    messagingTemplate.convertAndSend("/topic/system-events", event);
    log.debug("Broadcast system event: {} for domain {}", type, domainId);
  }

  /**
   * Broadcast app generation progress update. Called from CodeGenerationService during app
   * creation.
   */
  public void broadcastAppGenProgress(
      String requestId, String appName, String phase, int progressPercentage, String message) {
    Map<String, Object> progress = new HashMap<>();
    progress.put("type", "APP_GEN_PROGRESS");
    progress.put("requestId", requestId);
    progress.put("appName", appName);
    progress.put(
        "phase",
        phase); // INITIALIZING, ANALYZING, GENERATING_BACKEND, GENERATING_FRONTEND, FINALIZING,
    // COMPLETED
    progress.put("progress", progressPercentage);
    progress.put("message", message);
    progress.put("timestamp", System.currentTimeMillis());

    messagingTemplate.convertAndSend("/topic/app-gen", progress);
    messagingTemplate.convertAndSend("/topic/app-gen/" + requestId, progress);
    log.info(
        "[AppGenProgress] Request {}: {}% - {} - {}",
        requestId, progressPercentage, phase, message);
  }

  private Mono<Map<String, Object>> getDashboardData() {
    return userRepository
        .count()
        .map(
            userCount -> {
              Map<String, Object> data = new HashMap<>();

              Map<String, Object> stats = new HashMap<>();
              stats.put("totalUsers", userCount);
              stats.put("activeAIAgents", 12);
              stats.put("systemHealthScore", 98.5);
              stats.put("runningProjects", 5);
              stats.put("completedProjects", 142);
              stats.put("successRate", 99.2);
              stats.put("systemHealthStatus", "HEALTHY");
              stats.put("timestamp", System.currentTimeMillis());

              data.put("stats", stats);
              data.put("type", "dashboard_update");

              return data;
            });
  }

  private Mono<Map<String, Object>> getGlobalQuotaData() {
    return Mono.fromCallable(
        () -> {
          Map<String, Object> data = new HashMap<>();

          Map<String, Object> quotaStats = new HashMap<>();
          quotaStats.put("totalRequests", 15420);
          quotaStats.put("usedQuota", 8420);
          quotaStats.put("remainingQuota", 7000);
          quotaStats.put("usagePercentage", 54.6);
          quotaStats.put("timestamp", System.currentTimeMillis());

          data.put("quota", quotaStats);
          data.put("type", "quota_update");

          return data;
        });
  }
}
