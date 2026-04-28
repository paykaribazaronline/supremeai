package com.supremeai.controller;

import com.supremeai.service.SelfHealingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/github")
public class GitHubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

    @Autowired
    private SelfHealingService selfHealingService;

    @Autowired
    private WebSocketController webSocketController;

    @PostMapping("/workflow")
    public ResponseEntity<String> handleWorkflowEvent(@RequestBody Map<String, Object> payload) {
        String action = (String) payload.get("action");
        Map<String, Object> workflowJob = (Map<String, Object>) payload.get("workflow_job");

        if (workflowJob != null && "completed".equals(action)) {
            String conclusion = (String) workflowJob.get("conclusion");
            String workflowName = (String) workflowJob.get("workflow_name");
            String repo = (String) ((Map<String, Object>) payload.get("repository")).get("full_name");
            String workflowId = String.valueOf(workflowJob.get("id"));
            
            // Broadcast WebSocket notification to admin dashboard
            String status = "success".equals(conclusion) ? "success" : "failure";
            String message = String.format("Workflow '%s' %s for repo %s", 
                workflowName != null ? workflowName : "Unknown",
                "success".equals(conclusion) ? "completed successfully" : "failed",
                repo);
            String details = String.format("Workflow ID: %s, Conclusion: %s", workflowId, conclusion);
            
            try {
                webSocketController.broadcastPipelineNotification(status, message, details);
            } catch (Exception e) {
                log.error("Failed to broadcast pipeline notification: {}", e.getMessage());
            }

            if ("failure".equals(conclusion)) {
                // Trigger Self-Healing
                selfHealingService.handleWorkflowFailure(repo, workflowId, "GitHub Action Failure Detected");
                return ResponseEntity.ok("Self-healing triggered");
            }
        }

        return ResponseEntity.ok("Event ignored");
    }
}
