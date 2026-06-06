package com.supremeai.agent;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ZetaAgent implements AgentCapability {
  private static final Logger log = LoggerFactory.getLogger(ZetaAgent.class);

  private final Map<String, UsageHistory> userUsageHistory = new ConcurrentHashMap<>();

  @Override
  public String getAgentId() {
    return "ZETA";
  }

  @Override
  public String getAgentName() {
    return "Zeta-Finance";
  }

  @Override
  public List<String> getTriggerKeywords() {
    return Arrays.asList("budget", "finance", "cost", "spend", "prediction", "forecast", "billing");
  }

  @Override
  public Mono<String> process(String task, Map<String, Object> context) {
    log.info("[ZetaAgent] Performing budget prediction for task: {}", task);

    return Mono.fromCallable(
            () -> {
              String userId = (String) context.getOrDefault("userId", "unknown");
              Double currentMonthSpend = (Double) context.getOrDefault("currentSpend", 0.0);
              Double dailyAvg = (Double) context.getOrDefault("dailyAvg", 0.0);
              Integer daysRemaining = (Integer) context.getOrDefault("daysRemaining", 30);
              Double forecastBudget = (Double) context.getOrDefault("forecastBudget", 500.0);

              UsageHistory history = getUserHistory(userId);
              BudgetPrediction prediction =
                  predictBudget(
                      history, currentMonthSpend, dailyAvg, daysRemaining, forecastBudget);

              return generateBudgetReport(userId, prediction);
            })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
  }

  public UsageHistory getUserHistory(String userId) {
    return userUsageHistory.computeIfAbsent(userId, UsageHistory::new);
  }

  public BudgetPrediction predictBudget(
      UsageHistory history,
      Double currentSpend,
      Double dailyAvg,
      Integer daysRemaining,
      Double forecastBudget) {
    double projectedTotal = currentSpend + (dailyAvg * daysRemaining);
    double projectedRunway = (forecastBudget - projectedTotal) / dailyAvg;
    boolean overBudget = projectedTotal > forecastBudget;

    List<ForecastScenario> scenarios = new ArrayList<>();
    scenarios.add(
        new ForecastScenario(
            "Conservative",
            dailyAvg * 0.9,
            projectedTotal * 0.9,
            overBudget ? "Safe" : "On Track"));
    scenarios.add(
        new ForecastScenario(
            "Expected", dailyAvg, projectedTotal, overBudget ? "Over Budget" : "On Track"));
    scenarios.add(
        new ForecastScenario(
            "Aggressive",
            dailyAvg * 1.2,
            projectedTotal * 1.2,
            overBudget ? "Severe Overage" : "Monitor"));

    return new BudgetPrediction(
        currentSpend,
        dailyAvg,
        projectedTotal,
        forecastBudget,
        Math.max(0, projectedRunway),
        overBudget,
        scenarios);
  }

  private String generateBudgetReport(String userId, BudgetPrediction prediction) {
    StringBuilder report = new StringBuilder();
    report.append("[ZetaAgent] Budget Prediction Report:\n\n");
    report.append("User: ").append(userId).append("\n");
    report
        .append("Current Month-to-Date: $")
        .append(String.format("%.2f", prediction.currentSpend()))
        .append("\n");
    report
        .append("Daily Average: $")
        .append(String.format("%.2f", prediction.dailyAvg()))
        .append("\n");
    report
        .append("Forecast Budget: $")
        .append(String.format("%.2f", prediction.forecastBudget()))
        .append("\n");
    report
        .append("Projected Total: $")
        .append(String.format("%.2f", prediction.projectedTotal()))
        .append("\n\n");

    if (prediction.overBudget()) {
      report
          .append("?? BUDGET ALERT: Projected overspend by $")
          .append(String.format("%.2f", prediction.projectedTotal() - prediction.forecastBudget()))
          .append("\n");
      report
          .append("Estimated runway: ")
          .append(String.format("%.1f", prediction.projectedRunway()))
          .append(" days remaining\n\n");
    } else {
      report.append("? Within forecast budget\n");
      report
          .append("Runway remaining: ")
          .append(String.format("%.1f", prediction.projectedRunway()))
          .append(" days\n\n");
    }

    report.append("Forecast Scenarios:\n");
    for (ForecastScenario s : prediction.scenarios()) {
      report
          .append("  ")
          .append(s.scenario())
          .append(": $")
          .append(String.format("%.2f", s.projectedTotal()))
          .append(" - ")
          .append(s.status())
          .append("\n");
    }

    report.append("\nRecommendations:\n");
    if (prediction.dailyAvg() > prediction.forecastBudget() / 30) {
      report.append("  - Consider downgrading model tier for routine tasks\n");
      report.append("  - Enable response caching to reduce API calls\n");
    }
    report.append("  - Set up automated budget alerts at 50%, 75%, 90% thresholds\n");
    report.append("  - Review and optimize high-cost endpoints\n");

    return report.toString();
  }

  public record ForecastScenario(
      String scenario, double dailyRate, double projectedTotal, String status) {}

  public record BudgetPrediction(
      double currentSpend,
      double dailyAvg,
      double projectedTotal,
      double forecastBudget,
      double projectedRunway,
      boolean overBudget,
      List<ForecastScenario> scenarios) {}

  public static class UsageHistory {
    private final String userId;
    private final List<DailyUsage> dailyUsages = new ArrayList<>();
    private final Map<String, Double> categorySpending = new ConcurrentHashMap<>();

    public UsageHistory(String userId) {
      this.userId = userId;
    }

    public void addDailyUsage(LocalDate date, double amount, String category) {
      dailyUsages.add(new DailyUsage(date, amount));
      categorySpending.merge(category, amount, Double::sum);
    }

    public double getAverageDailyUsage(int days) {
      if (dailyUsages.isEmpty()) return 0.0;
      double sum = dailyUsages.stream().mapToDouble(d -> d.amount).sum();
      return sum / Math.min(days, dailyUsages.size());
    }

    public Map<String, Double> getCategoryBreakdown() {
      return new HashMap<>(categorySpending);
    }
  }

  private record DailyUsage(LocalDate date, double amount) {}
}
