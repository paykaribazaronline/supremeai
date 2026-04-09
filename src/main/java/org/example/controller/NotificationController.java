package org.example.controller;

import org.example.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Phase 5: Notification Controller
 * REST API for sending alerts via Email, Slack, Discord, SMS
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired(required = false)
    private NotificationService notificationService;

    /**
     * POST /api/notifications/email
     * Send email alert
     */
    @PostMapping("/email")
    public ResponseEntity<?> sendEmail(@RequestBody Map<String, String> request) {
        if (notificationService == null) {
            return ResponseEntity.ok(Map.of("message", "Notification service not available"));
        }

        String to = request.get("to");
        String subject = request.get("subject");
        String message = request.get("message");

        boolean sent = notificationService.sendEmailAlert(to, subject, message);
        return ResponseEntity.ok(Map.of(
            "status", sent ? "sent" : "failed",
            "channel", "EMAIL",
            "recipient", to
        ));
    }

    /**
     * POST /api/notifications/slack
     * Send Slack alert
     */
    @PostMapping("/slack")
    public ResponseEntity<?> sendSlack(@RequestBody Map<String, String> request) {
        if (notificationService == null) {
            return ResponseEntity.ok(Map.of("message", "Notification service not available"));
        }

        String channel = request.get("channel");
        String title = request.get("title");
        String message = request.get("message");
        String severity = request.getOrDefault("severity", "INFO");

        boolean sent = notificationService.sendSlackAlert(channel, title, message, severity);
        return ResponseEntity.ok(Map.of(
            "status", sent ? "sent" : "failed",
            "channel", "SLACK",
            "slackChannel", "#" + channel
        ));
    }

    /**
     * POST /api/notifications/discord
     * Send Discord alert
     */
    @PostMapping("/discord")
    public ResponseEntity<?> sendDiscord(@RequestBody Map<String, String> request) {
        if (notificationService == null) {
            return ResponseEntity.ok(Map.of("message", "Notification service not available"));
        }

        String channel = request.get("channel");
        String title = request.get("title");
        String message = request.get("message");
        String severity = request.getOrDefault("severity", "INFO");

        boolean sent = notificationService.sendDiscordAlert(channel, title, message, severity);
        return ResponseEntity.ok(Map.of(
            "status", sent ? "sent" : "failed",
            "channel", "DISCORD",
            "discordChannel", "#" + channel
        ));
    }

    /**
     * POST /api/notifications/sms
     * Send SMS alert
     */
    @PostMapping("/sms")
    public ResponseEntity<?> sendSms(@RequestBody Map<String, String> request) {
        if (notificationService == null) {
            return ResponseEntity.ok(Map.of("message", "Notification service not available"));
        }

        String phone = request.get("phoneNumber");
        String message = request.get("message");

        boolean sent = notificationService.sendSmsAlert(phone, message);
        return ResponseEntity.ok(Map.of(
            "status", sent ? "sent" : "failed",
            "channel", "SMS",
            "recipient", "****" + phone.substring(phone.length() - 4)
        ));
    }

    /**
     * POST /api/notifications/escalate
     * Send escalated alert (multi-channel by severity)
     */
    @PostMapping("/escalate")
    public ResponseEntity<?> sendEscalated(@RequestBody Map<String, String> request) {
        if (notificationService == null) {
            return ResponseEntity.ok(Map.of("message", "Notification service not available"));
        }

        String title = request.get("title");
        String message = request.get("message");
        String severity = request.getOrDefault("severity", "WARNING");

        return ResponseEntity.ok(notificationService.sendEscalatedAlert(title, message, severity));
    }

    /**
     * GET /api/notifications/channels
     * Get notification channel status
     */
    @GetMapping("/channels")
    public ResponseEntity<?> getChannelStatus() {
        if (notificationService == null) {
            return ResponseEntity.ok(Map.of("message", "Notification service not available"));
        }

        return ResponseEntity.ok(notificationService.getChannelStatus());
    }

    /**
     * GET /api/notifications/history?limit=50
     * Get notification history
     */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@RequestParam(defaultValue = "50") int limit) {
        if (notificationService == null) {
            return ResponseEntity.ok(Map.of("message", "Notification service not available"));
        }

        return ResponseEntity.ok(Map.of(
            "history", notificationService.getNotificationHistory(limit),
            "count", notificationService.getNotificationHistory(limit).size()
        ));
    }

    /**
     * POST /api/notifications/recipient?channel=EMAIL&recipient=...
     * Add recipient to channel
     */
    @PostMapping("/recipient")
    public ResponseEntity<?> addRecipient(
            @RequestParam String channel,
            @RequestParam String recipient) {
        if (notificationService == null) {
            return ResponseEntity.ok(Map.of("message", "Notification service not available"));
        }

        notificationService.addRecipient(channel, recipient);
        return ResponseEntity.ok(Map.of(
            "status", "added",
            "channel", channel,
            "recipient", recipient
        ));
    }

    /**
     * GET /api/notifications/sms/budget
     * Get current SMS daily budget configuration (ADMIN)
     * Returns current budget, default budget, cost per message, estimated messages per day
     */
    @GetMapping("/sms/budget")
    public ResponseEntity<?> getSmsBudgetConfig() {
        if (notificationService == null) {
            return ResponseEntity.ok(Map.of("message", "Notification service not available"));
        }
        
        return ResponseEntity.ok(notificationService.getSmsBudgetConfig());
    }

    /**
     * GET /api/notifications/sms/stats
     * Get SMS cost and budget tracking statistics
     * Returns: today's spending, remaining budget, percent used, messages remaining
     */
    @GetMapping("/sms/stats")
    public ResponseEntity<?> getSmsCostStats() {
        if (notificationService == null) {
            return ResponseEntity.ok(Map.of("message", "Notification service not available"));
        }
        
        return ResponseEntity.ok(notificationService.getSmsCoststats());
    }

    /**
     * POST /api/notifications/sms/budget/set
     * Set the daily SMS budget (ADMIN ONLY)
     * 
     * Request body: { "dailyBudget": 100.00 }
     * Example: Set budget to $100/day
     */
    @PostMapping("/sms/budget/set")
    public ResponseEntity<?> setSmsDailyBudget(@RequestBody Map<String, Object> request) {
        if (notificationService == null) {
            return ResponseEntity.ok(Map.of("message", "Notification service not available"));
        }
        
        // TODO: Add @Secured or @PreAuthorize annotation for admin-only access in production
        
        Object budgetObj = request.get("dailyBudget");
        if (budgetObj == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Missing 'dailyBudget' parameter",
                "example", Map.of("dailyBudget", 50.00)
            ));
        }
        
        try {
            double newBudget = Double.parseDouble(budgetObj.toString());
            return ResponseEntity.ok(notificationService.setSmsDailyBudget(newBudget));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Invalid budget format: must be a number",
                "example", budgetObj
            ));
        }
    }

    /**
     * POST /api/notifications/sms/budget/reset
     * Reset the daily SMS cost tracker to $0 (ADMIN ONLY)
     * Use when manually changing budget or for testing
     */
    @PostMapping("/sms/budget/reset")
    public ResponseEntity<?> resetSmsDailyCost() {
        if (notificationService == null) {
            return ResponseEntity.ok(Map.of("message", "Notification service not available"));
        }
        
        // TODO: Add @Secured or @PreAuthorize annotation for admin-only access in production
        
        notificationService.resetSmsDailyCost();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "SMS daily cost tracker reset to $0.00",
            "budgetConfig", notificationService.getSmsBudgetConfig(),
            "timestamp", System.currentTimeMillis()
        ));
    }
}
