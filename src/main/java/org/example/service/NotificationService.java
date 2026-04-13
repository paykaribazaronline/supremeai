package org.example.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 5: Notification Service
 * Sends alerts via Email, Slack, Discord, and SMS
 * SMS budget is configurable by admin via admin panel (not hardcoded)
 */
@Service
public class NotificationService {

    // SMS cost tracking
    private static final double SMS_COST_PER_MESSAGE = 0.0075; // $0.0075 per SMS (Twilio standard)
    private static final double DEFAULT_DAILY_SMS_BUDGET = 50.00; // Default $50, overridable by admin
    
    // Admin configurable SMS budget (can be set via admin dashboard)
    private double adminConfiguredDailyBudget = DEFAULT_DAILY_SMS_BUDGET;
    
    private LocalDate smsCostTrackingDate = LocalDate.now();
    private double smsCostToday = 0.0;

    private static class NotificationConfig {
        public String type; // EMAIL, SLACK, DISCORD, SMS
        public String endpoint;
        public String apiKey;
        public boolean enabled;
        public List<String> recipients = new ArrayList<>();

        NotificationConfig(String type) {
            this.type = type;
        }
    }

    private final Map<String, NotificationConfig> channels = new ConcurrentHashMap<>();
    private final List<String> notificationLog = Collections.synchronizedList(new ArrayList<>());

    public NotificationService() {
        initializeChannels();
    }

    /**
     * Initialize notification channels
     */
    private void initializeChannels() {
        // Email configuration
        NotificationConfig email = new NotificationConfig("EMAIL");
        email.endpoint = System.getenv("MAIL_SMTP_HOST") != null ? 
            System.getenv("MAIL_SMTP_HOST") : "localhost";
        email.apiKey = System.getenv("MAIL_API_KEY");
        email.enabled = email.apiKey != null;
        channels.put("EMAIL", email);

        // Slack configuration
        NotificationConfig slack = new NotificationConfig("SLACK");
        slack.endpoint = "https://hooks.slack.com/services/";
        slack.apiKey = System.getenv("SLACK_WEBHOOK_URL");
        slack.enabled = slack.apiKey != null;
        channels.put("SLACK", slack);

        // Discord configuration
        NotificationConfig discord = new NotificationConfig("DISCORD");
        discord.endpoint = "https://discordapp.com/api/webhooks/";
        discord.apiKey = System.getenv("DISCORD_WEBHOOK_URL");
        discord.enabled = discord.apiKey != null;
        channels.put("DISCORD", discord);

        // SMS configuration (Twilio)
        NotificationConfig sms = new NotificationConfig("SMS");
        sms.endpoint = "https://api.twilio.com/2010-04-01/Accounts/";
        sms.apiKey = System.getenv("TWILIO_AUTH_TOKEN");
        sms.enabled = sms.apiKey != null;
        channels.put("SMS", sms);
    }

    /**
     * Send alert via Email
     */
    public boolean sendEmailAlert(String to, String subject, String message) {
        NotificationConfig email = channels.get("EMAIL");
        if (!email.enabled) {
            logNotification("EMAIL", "DISABLED");
            return false;
        }

        try {
            // Simulated email send (replace with actual email service)
            String content = String.format(
                "To: %s\nSubject: %s\n\n%s",
                to, subject, message
            );
            logNotification("EMAIL", "SENT to " + to);
            return true;
        } catch (Exception e) {
            logNotification("EMAIL", "FAILED: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send alert via Slack
     */
    public boolean sendSlackAlert(String channel, String title, String message, String severity) {
        NotificationConfig slack = channels.get("SLACK");
        if (!slack.enabled) {
            logNotification("SLACK", "DISABLED");
            return false;
        }

        try {
            String color = getColorBySeverity(severity);
            String payload = String.format(
                "{\"channel\": \"%s\", \"attachments\": [{\"color\": \"%s\", \"title\": \"%s\", \"text\": \"%s\", \"ts\": %d}]}",
                channel, color, title, message, System.currentTimeMillis() / 1000
            );
            // Simulated Slack webhook call
            logNotification("SLACK", "SENT to #" + channel);
            return true;
        } catch (Exception e) {
            logNotification("SLACK", "FAILED: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send alert via Discord
     */
    public boolean sendDiscordAlert(String channel, String title, String message, String severity) {
        NotificationConfig discord = channels.get("DISCORD");
        if (!discord.enabled) {
            logNotification("DISCORD", "DISABLED");
            return false;
        }

        try {
            int color = getColorCodeBySeverity(severity);
            String payload = String.format(
                "{\"embeds\": [{\"title\": \"%s\", \"description\": \"%s\", \"color\": %d, \"timestamp\": \"%s\"}]}",
                title, message, color, System.currentTimeMillis()
            );
            // Simulated Discord webhook call
            logNotification("DISCORD", "SENT to #" + channel);
            return true;
        } catch (Exception e) {
            logNotification("DISCORD", "FAILED: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send alert via SMS (Twilio) with admin-configurable daily budget guard
     */
    public boolean sendSmsAlert(String phoneNumber, String message) {
        NotificationConfig sms = channels.get("SMS");
        if (!sms.enabled) {
            logNotification("SMS", "DISABLED");
            return false;
        }

        // Check if date has changed, reset cost tracking
        LocalDate today = LocalDate.now();
        if (!today.equals(smsCostTrackingDate)) {
            smsCostToday = 0.0;
            smsCostTrackingDate = today;
            logNotification("SMS_BUDGET", "Reset for new day - Budget: $" + String.format("%.2f", adminConfiguredDailyBudget));
        }

        // Check daily budget before sending
        double projectedCost = smsCostToday + SMS_COST_PER_MESSAGE;
        if (projectedCost > adminConfiguredDailyBudget) {
            logNotification("SMS", String.format(
                "BLOCKED: Budget limit exceeded. Today: $%.2f, Limit: $%.2f", 
                smsCostToday, adminConfiguredDailyBudget));
            return false;
        }

        try {
            // Limit message to 160 chars for SMS
            String smsMessage = message.length() > 160 ? 
                message.substring(0, 157) + "..." : message;
            
            // Simulated Twilio SMS call
            logNotification("SMS", "SENT to " + maskPhone(phoneNumber));
            
            // Track cost
            smsCostToday += SMS_COST_PER_MESSAGE;
            logNotification("SMS_BUDGET", String.format(
                "Cost: $%.4f, Today: $%.2f, Remaining: $%.2f",
                SMS_COST_PER_MESSAGE, smsCostToday, adminConfiguredDailyBudget - smsCostToday));
            
            return true;
        } catch (Exception e) {
            logNotification("SMS", "FAILED: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send multi-channel alert (escalation)
     */
    public Map<String, Object> sendEscalatedAlert(String title, String message, String severity) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Boolean> channels = new HashMap<>();

        // Escalation policy by severity
        if ("CRITICAL".equals(severity)) {
            // Send to all channels
            channels.put("email", sendEmailAlert("admin@supremeai.dev", title, message));
            channels.put("slack", sendSlackAlert("alerts-critical", title, message, severity));
            channels.put("discord", sendDiscordAlert("alerts", title, message, severity));
            channels.put("sms", sendSmsAlert("+1234567890", message));
        } else if ("ERROR".equals(severity)) {
            // Send to email and Slack
            channels.put("email", sendEmailAlert("admin@supremeai.dev", title, message));
            channels.put("slack", sendSlackAlert("alerts", title, message, severity));
        } else if ("WARNING".equals(severity)) {
            // Send to Slack only
            channels.put("slack", sendSlackAlert("alerts", title, message, severity));
        }

        result.put("escalationPolicy", severity);
        result.put("sent", channels);
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    /**
     * Get notification channel status
     */
    public Map<String, Object> getChannelStatus() {
        Map<String, Object> status = new HashMap<>();

        for (Map.Entry<String, NotificationConfig> entry : channels.entrySet()) {
            NotificationConfig config = entry.getValue();
            status.put(entry.getKey(), Map.of(
                "enabled", config.enabled,
                "configured", config.apiKey != null && !config.apiKey.isEmpty(),
                "endpoint", config.endpoint
            ));
        }

        return status;
    }

    /**
     * Get notification history
     */
    public List<String> getNotificationHistory(int limit) {
        return notificationLog.stream()
                .skip(Math.max(0, notificationLog.size() - limit))
                .toList();
    }

    /**
     * Add recipient to channel
     */
    public void addRecipient(String channelType, String recipient) {
        NotificationConfig config = channels.get(channelType);
        if (config != null) {
            config.recipients.add(recipient);
        }
    }

    /**
     * Get recipients for channel
     */
    public List<String> getRecipients(String channelType) {
        NotificationConfig config = channels.get(channelType);
        return config != null ? new ArrayList<>(config.recipients) : new ArrayList<>();
    }

    /**
     * Get color for Slack based on severity
     */
    private String getColorBySeverity(String severity) {
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> "#FF0000"; // Red
            case "ERROR" -> "#FF6600"; // Orange
            case "WARNING" -> "#FFFF00"; // Yellow
            default -> "#00FF00"; // Green
        };
    }

    /**
     * Get color code for Discord based on severity
     */
    private int getColorCodeBySeverity(String severity) {
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> 0xFF0000; // Red
            case "ERROR" -> 0xFF6600; // Orange
            case "WARNING" -> 0xFFFF00; // Yellow
            default -> 0x00FF00; // Green
        };
    }

    /**
     * Mask phone number for privacy
     */
    private String maskPhone(String phone) {
        if (phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }

    /**
     * Log notification event
     */
    private void logNotification(String channel, String status) {
        String log = String.format("[%s] %s: %s", 
            System.currentTimeMillis(), channel, status);
        notificationLog.add(log);

        // Keep only last 1000 logs
        if (notificationLog.size() > 1000) {
            notificationLog.remove(0);
        }
    }

    /**
     * Clear notification history
     */
    public void clearHistory() {
        notificationLog.clear();
    }

    /**
     * Get SMS cost tracking statistics
     */
    public Map<String, Object> getSmsCoststats() {
        LocalDate today = LocalDate.now();
        double remaining = adminConfiguredDailyBudget - smsCostToday;
        
        return Map.of(
            "date", today.toString(),
            "costPerMessage", SMS_COST_PER_MESSAGE,
            "dailyBudget", adminConfiguredDailyBudget,
            "spentToday", String.format("$%.2f", smsCostToday),
            "remaining", String.format("$%.2f", remaining),
            "percentUsed", String.format("%.1f%%", (smsCostToday / adminConfiguredDailyBudget) * 100),
            "maxMessagesRemaining", (int) (remaining / SMS_COST_PER_MESSAGE),
            "isBudgetExceeded", smsCostToday >= adminConfiguredDailyBudget
        );
    }

    /**
     * Set SMS daily budget (admin-only operation)
     * @param newBudget New daily SMS budget in dollars
     * @return Result of the operation
     */
    public Map<String, Object> setSmsDailyBudget(double newBudget) {
        if (newBudget <= 0) {
            logNotification("SMS_BUDGET", "REJECTED: Budget must be > 0");
            return Map.of(
                "success", false,
                "error", "Budget must be greater than $0",
                "currentBudget", adminConfiguredDailyBudget
            );
        }
        
        double oldBudget = adminConfiguredDailyBudget;
        adminConfiguredDailyBudget = newBudget;
        logNotification("SMS_BUDGET", String.format(
            "UPDATED by Admin: $%.2f → $%.2f", oldBudget, newBudget));
        
        return Map.of(
            "success", true,
            "previousBudget", String.format("$%.2f", oldBudget),
            "newBudget", String.format("$%.2f", newBudget),
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * Get current SMS budget configuration
     */
    public Map<String, Object> getSmsBudgetConfig() {
        return Map.of(
            "currentDailyBudget", String.format("$%.2f", adminConfiguredDailyBudget),
            "defaultBudget", String.format("$%.2f", DEFAULT_DAILY_SMS_BUDGET),
            "costPerMessage", String.format("$%.4f", SMS_COST_PER_MESSAGE),
            "estimatedMessagesPerDay", (int) (adminConfiguredDailyBudget / SMS_COST_PER_MESSAGE),
            "spentToday", String.format("$%.2f", smsCostToday),
            "budgetRemaining", String.format("$%.2f", adminConfiguredDailyBudget - smsCostToday),
            "trackedDate", smsCostTrackingDate.toString()
        );
    }

    /**
     * Reset SMS daily cost tracking (admin only, for manual reset or testing)
     */
    public void resetSmsDailyCost() {
        smsCostToday = 0.0;
        smsCostTrackingDate = LocalDate.now();
        logNotification("SMS_BUDGET", "Manually reset by admin");
    }
}
