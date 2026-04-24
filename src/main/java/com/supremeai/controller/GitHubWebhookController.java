package com.supremeai.controller;

import com.supremeai.service.SelfHealingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/github")
public class GitHubWebhookController {

    @Autowired
    private SelfHealingService selfHealingService;

    @PostMapping("/workflow")
    public ResponseEntity<String> handleWorkflowEvent(@RequestBody Map<String, Object> payload) {
        String action = (String) payload.get("action");
        Map<String, Object> workflowJob = (Map<String, Object>) payload.get("workflow_job");

        if (workflowJob != null && "completed".equals(action)) {
            String conclusion = (String) workflowJob.get("conclusion");
            if ("failure".equals(conclusion)) {
                String repo = (String) ((Map<String, Object>) payload.get("repository")).get("full_name");
                String workflowId = String.valueOf(workflowJob.get("id"));
                
                // Trigger Self-Healing
                selfHealingService.handleWorkflowFailure(repo, workflowId, "GitHub Action Failure Detected");
                return ResponseEntity.ok("Self-healing triggered");
            }
        }

        return ResponseEntity.ok("Event ignored");
    }
}
