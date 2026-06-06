package com.supremeai.agent;

import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DeltaAgent implements AgentCapability {
  private static final Logger log = LoggerFactory.getLogger(DeltaAgent.class);

  private static final Map<String, ProviderPricing> PROVIDER_PRICING = new HashMap<>();

  static {
    PROVIDER_PRICING.put("gpt-4", new ProviderPricing("openai", 0.03, 0.06));
    PROVIDER_PRICING.put("gpt-3.5-turbo", new ProviderPricing("openai", 0.001, 0.002));
    PROVIDER_PRICING.put("claude-3-opus", new ProviderPricing("anthropic", 0.015, 0.075));
    PROVIDER_PRICING.put("claude-3-sonnet", new ProviderPricing("anthropic", 0.003, 0.015));
    PROVIDER_PRICING.put("gemini-1.5-pro", new ProviderPricing("google", 0.0035, 0.0105));
    PROVIDER_PRICING.put("gemini-1.5-flash", new ProviderPricing("google", 0.0005, 0.0015));
  }

  private final Map<String, UsageSummary> userUsage = new ConcurrentHashMap<>();

  @Override
  public String getAgentId() {
    return "DELTA";
  }

  @Override
  public String getAgentName() {
    return "Delta-Cost";
  }

  @Override
  public List<String> getTriggerKeywords() {
    return Arrays.asList("cost", "budget", "usage", "expensive", "spend", "quota", "billing");
  }

  @Override
  public Mono<String> process(String task, Map<String, Object> context) {
    log.info("[DeltaAgent] Analyzing cost for task: {}", task);

    return Mono.fromCallable(
            () -> {
              String userId = (String) context.getOrDefault("userId", "unknown");
              Double promptTokens = (Double) context.getOrDefault("promptTokens", 0.0);
              Double completionTokens = (Double) context.getOrDefault("completionTokens", 0.0);
              String provider = (String) context.getOrDefault("provider", "gpt-4");
              Double hourlyBudget = (Double) context.getOrDefault("hourlyBudget", 10.0);
              Double dailyBudget = (Double) context.getOrDefault("dailyBudget", 100.0);

              double estimatedCost = calculateCost(provider, promptTokens, completionTokens);
              UsageSummary summary = trackUsage(userId, provider, estimatedCost);

              return generateCostReport(
                  userId, provider, estimatedCost, summary, hourlyBudget, dailyBudget);
            })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
  }

  public double calculateCost(String provider, double promptTokens, double completionTokens) {
    ProviderPricing pricing =
        PROVIDER_PRICING.getOrDefault(
            provider.toLowerCase(), new ProviderPricing("unknown", 0.01, 0.02));

    double promptCost = (promptTokens / 1000) * pricing.promptPer1k;
    double completionCost = (completionTokens / 1000) * pricing.completionPer1k;

    return promptCost + completionCost;
  }

  public UsageSummary trackUsage(String userId, String provider, double cost) {
    userUsage.putIfAbsent(userId, new UsageSummary(userId));
    UsageSummary summary = userUsage.get(userId);
    summary.addUsage(provider, cost);
    return summary;
  }

  public BudgetStatus checkBudget(String userId, double hourlyBudget, double dailyBudget) {
    UsageSummary summary = userUsage.getOrDefault(userId, new UsageSummary(userId));
    return new BudgetStatus(
        summary.hourlyTotal <= hourlyBudget,
        summary.dailyTotal <= dailyBudget,
        hourlyBudget - summary.hourlyTotal,
        dailyBudget - summary.dailyTotal);
  }

  private String generateCostReport(
      String userId,
      String provider,
      double estimatedCost,
      UsageSummary summary,
      double hourlyBudget,
      double dailyBudget) {
    BudgetStatus budget = checkBudget(userId, hourlyBudget, dailyBudget);

    StringBuilder report = new StringBuilder();
    report.append("[DeltaAgent] Cost Analysis Report:\n\n");
    report.append("Provider: ").append(provider).append("\n");
    report.append("Estimated Cost: $").append(String.format("%.4f", estimatedCost)).append("\n\n");
    report.append("Usage Summary (User: ").append(userId).append("):\n");
    report
        .append("  Hourly Spend: $")
        .append(String.format("%.2f", summary.hourlyTotal))
        .append("/")
        .append("$")
        .append(hourlyBudget)
        .append("\n");
    report
        .append("  Daily Spend: $")
        .append(String.format("%.2f", summary.dailyTotal))
        .append("/")
        .append("$")
        .append(dailyBudget)
        .append("\n");
    report
        .append("  Hourly Remaining: $")
        .append(String.format("%.2f", budget.hourlyRemaining()))
        .append("\n");
    report
        .append("  Daily Remaining: $")
        .append(String.format("%.2f", budget.dailyRemaining()))
        .append("\n\n");

    if (!budget.withinHourly()) {
      report.append("?? WARNING: Hourly budget exceeded!\n");
    }
    if (!budget.withinDaily()) {
      report.append("?? WARNING: Daily budget exceeded!\n");
    }

    report.append("\nProvider Pricing Table:\n");
    PROVIDER_PRICING.forEach(
        (p, pricing) ->
            report
                .append("  ")
                .append(p)
                .append(": $")
                .append(pricing.promptPer1k)
                .append("/1k prompt, $")
                .append(pricing.completionPer1k)
                .append("/1k completion\n"));

    return report.toString();
  }

  public record ProviderPricing(String vendor, double promptPer1k, double completionPer1k) {}

  public record BudgetStatus(
      boolean withinHourly, boolean withinDaily, double hourlyRemaining, double dailyRemaining) {}

  public static class UsageSummary {
    private final String userId;
    private double hourlyTotal = 0;
    private double dailyTotal = 0;
    private Map<String, Double> providerCosts = new ConcurrentHashMap<>();
    private Date lastResetDate = new Date();

    public UsageSummary(String userId) {
      this.userId = userId;
    }

    public void addUsage(String provider, double cost) {
      providerCosts.merge(provider, cost, Double::sum);
      hourlyTotal += cost;
      dailyTotal += cost;
    }

    public double getHourlyTotal() {
      return hourlyTotal;
    }

    public double getDailyTotal() {
      return dailyTotal;
    }

    public Map<String, Double> getProviderBreakdown() {
      return new HashMap<>(providerCosts);
    }
  }
}
