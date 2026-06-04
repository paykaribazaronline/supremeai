package com.supremeai.service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * CD-04: Cost Transparency Report Calculates per-user and per-session cost breakdown and can export
 * as CSV.
 */
@Service
public class CostTransparencyReportService {

  private static final Logger log = LoggerFactory.getLogger(CostTransparencyReportService.class);

  @Autowired(required = false)
  private Environment environment;

  // Simulated usage data. In production, this would be fetched from Firestore usage_logs.
  private final List<UsageLog> usageLogs = new ArrayList<>();

  public CostTransparencyReportService() {
    // Production constructor is empty to prevent hardcoded mock logs
  }

  @PostConstruct
  public void init() {
    if (environment != null && Arrays.asList(environment.getActiveProfiles()).contains("test")) {
      // Mock data only loaded for test environment
      usageLogs.add(new UsageLog("user_001", "session_abc", "gpt-4", 1500, 500, 0.06));
      usageLogs.add(new UsageLog("user_001", "session_abc", "claude-3-opus", 2000, 800, 0.075));
      usageLogs.add(new UsageLog("user_002", "session_xyz", "gemini-1.5-pro", 1000, 300, 0.015));
    }
  }

  public void addUsageLogForTesting(
      String userId,
      String sessionId,
      String provider,
      int promptTokens,
      int completionTokens,
      double cost) {
    usageLogs.add(new UsageLog(userId, sessionId, provider, promptTokens, completionTokens, cost));
  }

  public Mono<Map<String, Object>> generateCostReport(String userId) {
    log.info("Generating cost report for user: {}", userId);

    double totalCost = 0;
    Map<String, Double> sessionCosts = new HashMap<>();
    Map<String, Double> providerCosts = new HashMap<>();

    for (UsageLog logEntry : usageLogs) {
      if (userId.equals(logEntry.userId)) {
        totalCost += logEntry.cost;
        sessionCosts.put(
            logEntry.sessionId, sessionCosts.getOrDefault(logEntry.sessionId, 0.0) + logEntry.cost);
        providerCosts.put(
            logEntry.provider, providerCosts.getOrDefault(logEntry.provider, 0.0) + logEntry.cost);
      }
    }

    Map<String, Object> report = new HashMap<>();
    report.put("userId", userId);
    report.put("totalCostUsd", totalCost);
    report.put("sessionBreakdown", sessionCosts);
    report.put("providerBreakdown", providerCosts);
    report.put("generatedAt", LocalDateTime.now().toString());

    return Mono.just(report);
  }

  public Mono<String> exportReportAsCsv(String userId) {
    return generateCostReport(userId)
        .map(
            report -> {
              StringBuilder csv = new StringBuilder();
              csv.append("Report Type,Cost Transparency Report\n");
              csv.append("User ID,").append(report.get("userId")).append("\n");
              csv.append("Generated At,").append(report.get("generatedAt")).append("\n");
              csv.append("Total Cost (USD),").append(report.get("totalCostUsd")).append("\n\n");

              csv.append("--- Session Breakdown ---\n");
              csv.append("Session ID,Cost (USD)\n");
              @SuppressWarnings("unchecked")
              Map<String, Double> sessions = (Map<String, Double>) report.get("sessionBreakdown");
              for (Map.Entry<String, Double> entry : sessions.entrySet()) {
                csv.append(entry.getKey()).append(",").append(entry.getValue()).append("\n");
              }

              csv.append("\n--- Provider Breakdown ---\n");
              csv.append("Provider,Cost (USD)\n");
              @SuppressWarnings("unchecked")
              Map<String, Double> providers = (Map<String, Double>) report.get("providerBreakdown");
              for (Map.Entry<String, Double> entry : providers.entrySet()) {
                csv.append(entry.getKey()).append(",").append(entry.getValue()).append("\n");
              }

              return csv.toString();
            });
  }

  private static class UsageLog {
    String userId;
    String sessionId;
    String provider;
    int promptTokens;
    int completionTokens;
    double cost;

    public UsageLog(
        String userId,
        String sessionId,
        String provider,
        int promptTokens,
        int completionTokens,
        double cost) {
      this.userId = userId;
      this.sessionId = sessionId;
      this.provider = provider;
      this.promptTokens = promptTokens;
      this.completionTokens = completionTokens;
      this.cost = cost;
    }
  }
}
