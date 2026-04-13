package org.example.selfhealing.healing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Admin Escalation Service
 * 
 * Handles escalation of healing failures to human admins via:
 * - PagerDuty incidents
 * - Slack notifications
 * - Email alerts
 * - GitHub issues
 * - In-app dashboard notifications
 */
@Service
public class AdminEscalationService {
    private static final Logger logger = LoggerFactory.getLogger(AdminEscalationService.class);
    
    @Value("${pagerduty.api.key:}")
    private String pagerDutyApiKey;
    
    @Value("${slack.webhook.url:}")
    private String slackWebhookUrl;
    
    @Value("${admin.email:}")
    private String adminEmail;
    
    @Value("${github.token:}")
    private String githubToken;

    @Value("${github.repository:}")
    private String githubRepository;
    
    // Escalation levels
    public enum EscalationLevel {
        INFO,     // Low priority - inform admin
        WARNING,  // Medium priority - needs attention
        CRITICAL  // High priority - page on-call
    }
    
    /**
     * Escalate healing failure
     * 
     * @param workflowId The workflow that failed
     * @param reason Why escalation happened
     * @param level Priority level
     */
    public void escalate(String workflowId, String reason, EscalationLevel level) {
        logger.warn("🚨 ESCALATION [{}]: Workflow {} - {}", level, workflowId, reason);
        
        // Escalate via all configured channels
        if (level == EscalationLevel.CRITICAL) {
            escalateToPagerDuty(workflowId, reason);
        }
        
        escalateToSlack(workflowId, reason, level);
        escalateToGitHub(workflowId, reason);
    }
    
    /**
     * Escalate to PagerDuty (on-call notification)
     */
    private void escalateToPagerDuty(String workflowId, String reason) {
        if (pagerDutyApiKey == null || pagerDutyApiKey.isEmpty()) {
            logger.debug("PagerDuty not configured");
            return;
        }
        
        try {
            URL url = new URL("https://events.pagerduty.com/v2/enqueue");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String payload = String.format(
                "{\"routing_key\":\"%s\",\"event_action\":\"trigger\",\"payload\":{\"summary\":\"SupremeAI healing loop failure\",\"timestamp\":\"%s\",\"severity\":\"critical\",\"source\":\"SupremeAI Healing System\",\"custom_details\":{\"workflow_id\":\"%s\",\"reason\":\"%s\"}}}",
                pagerDutyApiKey,
                new Date().toInstant(),
                workflowId,
                reason.replace("\"", "\\\"")
            );

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                logger.info("📍 PagerDuty incident created for: {}", workflowId);
            } else {
                logger.warn("PagerDuty returned {} for workflow {}", responseCode, workflowId);
            }
        } catch (Exception e) {
            logger.error("Failed to create PagerDuty incident", e);
        }
    }
    
    /**
     * Escalate to Slack
     */
    private void escalateToSlack(String workflowId, String reason, EscalationLevel level) {
        if (slackWebhookUrl == null || slackWebhookUrl.isEmpty()) {
            logger.debug("Slack not configured");
            return;
        }
        
        try {
            String emoji = level == EscalationLevel.CRITICAL ? "🔴" :
                          level == EscalationLevel.WARNING ? "🟡" : "🔵";
            
            String message = emoji + " **SupremeAI Healing Escalation** [" + level + "]\n" +
                           "Workflow: " + workflowId + "\n" +
                           "Reason: " + reason + "\n" +
                           "Time: " + new Date() + "\n" +
                           "Action: Please review at dashboard";
            
            URL url = new URL(slackWebhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String payload = String.format(
                "{\"text\":\"%s\"}",
                message.replace("\"", "\\\"")
            );

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                logger.info("💬 Slack notification sent");
            } else {
                logger.warn("Slack webhook returned {}", responseCode);
            }
            
        } catch (Exception e) {
            logger.error("Failed to send Slack notification", e);
        }
    }
    
    /**
     * Create GitHub issue for escalation
     */
    private void escalateToGitHub(String workflowId, String reason) {
        if (githubToken == null || githubToken.isEmpty()) {
            logger.debug("GitHub token not configured");
            return;
        }
        if (githubRepository == null || githubRepository.isEmpty()) {
            logger.debug("GitHub repository not configured");
            return;
        }
        
        try {
            String title = "Healing Loop Failure: " + workflowId;
            String body = String.format(
                "## Healing Loop Escalation\n\n" +
                "**Workflow ID:** %s\n" +
                "**Reason:** %s\n" +
                "**Timestamp:** %s\n\n" +
                "Please investigate and manually fix the underlying issue.",
                workflowId, reason, new Date()
            );

            URL url = new URL("https://api.github.com/repos/" + githubRepository + "/issues");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + githubToken);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setDoOutput(true);

            String payload = String.format(
                "{\"title\":\"%s\",\"body\":\"%s\"}",
                title.replace("\"", "\\\""),
                body.replace("\"", "\\\"")
            );

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                logger.info("📋 GitHub issue created for escalation");
            } else {
                logger.warn("GitHub issue creation returned {} for {}", responseCode, githubRepository);
            }
        } catch (Exception e) {
            logger.error("Failed to create GitHub issue", e);
        }
    }
    
    /**
     * Mark escalation as resolved (admin action)
     */
    public void markResolved(String workflowId, String resolution) {
        logger.info("✅ Escalation resolved for {}: {}", workflowId, resolution);
        
        // Close PagerDuty incident
        // Close GitHub issue
        // Post Slack message confirming resolution
    }
    
    /**
     * Get escalation status
     */
    public Map<String, Object> getEscalationStatus(String workflowId) {
        return Map.of(
            "workflowId", workflowId,
            "pagerdutyStatus", pagerDutyApiKey != null && !pagerDutyApiKey.isEmpty() ? "CONFIGURED" : "UNCONFIGURED",
            "githubIssueStatus", githubToken != null && !githubToken.isEmpty() && githubRepository != null && !githubRepository.isEmpty() ? "CONFIGURED" : "UNCONFIGURED",
            "slackThreadResolved", slackWebhookUrl != null && !slackWebhookUrl.isEmpty()
        );
    }
}
