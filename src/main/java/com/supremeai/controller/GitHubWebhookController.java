package com.supremeai.controller;

import com.supremeai.security.SecretManagerService;
import com.supremeai.service.SelfHealingService;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/webhooks/github")
public class GitHubWebhookController {

  private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

  private final SelfHealingService selfHealingService;
  private final WebSocketController webSocketController;
  private final SecretManagerService secretManagerService;
  private final RestTemplate restTemplate;

  @Autowired
  public GitHubWebhookController(
      SelfHealingService selfHealingService,
      WebSocketController webSocketController,
      SecretManagerService secretManagerService,
      RestTemplateBuilder restTemplateBuilder) {
    this.selfHealingService = selfHealingService;
    this.webSocketController = webSocketController;
    this.secretManagerService = secretManagerService;
    this.restTemplate = restTemplateBuilder.build();
  }

  @PostMapping("/workflow")
  public ResponseEntity<String> handleWorkflowEvent(@RequestBody Map<String, Object> payload) {
    String action = (String) payload.get("action");
    @SuppressWarnings("unchecked")
    Map<String, Object> workflowJob = (Map<String, Object>) payload.get("workflow_job");

    if (workflowJob != null && "completed".equals(action)) {
      String conclusion = (String) workflowJob.get("conclusion");
      String workflowName = (String) workflowJob.get("workflow_name");
      @SuppressWarnings("unchecked")
      Map<String, Object> repoMap = (Map<String, Object>) payload.get("repository");
      String repo = repoMap != null ? (String) repoMap.get("full_name") : "unknown";
      String workflowId = String.valueOf(workflowJob.get("id"));

      // Broadcast WebSocket notification to admin dashboard
      String status = "success".equals(conclusion) ? "success" : "failure";
      String message =
          String.format(
              "Workflow '%s' %s for repo %s",
              workflowName != null ? workflowName : "Unknown",
              "success".equals(conclusion) ? "completed successfully" : "failed",
              repo);
      String details = String.format("Workflow ID: %s, Conclusion: %s", workflowId, conclusion);

      try {
        webSocketController.broadcastPipelineNotification(status, message, details);
      } catch (Exception e) {
        log.error("Failed to broadcast pipeline notification: {}", e.getMessage());
      }

      // Trigger AI analysis on successful deployment
      if ("success".equals(conclusion)
          && (workflowName != null && workflowName.contains("Deploy"))) {
        try {
          triggerDeploymentAnalysis(payload);
        } catch (Exception e) {
          log.error("Failed to trigger deployment analysis: {}", e.getMessage());
        }
      }

      if ("failure".equals(conclusion)) {
        // Trigger Self-Healing
        selfHealingService.handleWorkflowFailure(
            repo, workflowId, "GitHub Action Failure Detected");
        return ResponseEntity.ok("Self-healing triggered");
      }
    }

    return ResponseEntity.ok("Event ignored");
  }

  /** Trigger AI-powered deployment analysis via Firebase Function */
  private void triggerDeploymentAnalysis(Map<String, Object> payload) {
    new Thread(
            () -> {
              try {
                Map<String, Object> analysisPayload = new HashMap<>();

                // Extract relevant info
                @SuppressWarnings("unchecked")
                Map<String, Object> workflowJob = (Map<String, Object>) payload.get("workflow_job");
                if (workflowJob != null) {
                  analysisPayload.put("runId", workflowJob.get("id"));
                  analysisPayload.put("workflowName", workflowJob.get("workflow_name"));
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> repository = (Map<String, Object>) payload.get("repository");
                if (repository != null) {
                  analysisPayload.put("author", repository.get("owner"));
                }

                // Get commit info and changed files from GitHub API
                String repoFullName = (String) repository.get("full_name");
                String headSha = (String) payload.get("after");

                if (repoFullName != null && headSha != null) {
                  String githubToken = secretManagerService.getSecret("GITHUB_TOKEN");
                  if (githubToken == null) {
                    githubToken = secretManagerService.getSecret("GITHUB_ACTIONS_TOKEN");
                  }
                  String commitUrl =
                      String.format(
                          "https://api.github.com/repos/%s/commits/%s", repoFullName, headSha);

                  log.info("Deployment analysis triggered for commit: {}", headSha);

                  // Send to Firebase Function for AI analysis
                  String functionUrl =
                      "https://us-central1-supremeai-a.cloudfunctions.net/analyzeDeployment";
                  Map<String, Object> requestBody = new HashMap<>();
                  requestBody.put(
                      "commitMessage",
                      "Deployment completed: " + analysisPayload.get("workflowName"));
                  requestBody.put("author", analysisPayload.get("author"));
                  requestBody.put("branch", "main");
                  requestBody.put("changedFiles", new String[] {"部署文件已更新"});
                  requestBody.put("runId", analysisPayload.get("runId"));

                  // Send the request to the analysis function
                  log.info("Sending deployment analysis payload to: {}", functionUrl);
                  restTemplate.postForEntity(functionUrl, requestBody, String.class);
                  log.info(
                      "Deployment analysis request sent successfully for runId: {}",
                      analysisPayload.get("runId"));
                }

              } catch (Exception e) {
                log.error("Error in deployment analysis trigger:", e);
              }
            })
        .start();
  }
}
