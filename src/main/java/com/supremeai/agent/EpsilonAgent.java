package com.supremeai.agent;

import com.supremeai.service.ResponseCacheService;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class EpsilonAgent implements AgentCapability {
  private static final Logger log = LoggerFactory.getLogger(EpsilonAgent.class);

  private final Map<String, ResourceMetrics> resourceMetrics = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

  @Autowired private AgentRuleService ruleService;

  @Autowired private ResponseCacheService cacheService;

  @Override
  public String getAgentId() {
    return "EPSILON";
  }

  @Override
  public String getAgentName() {
    return "Epsilon-Optimizer";
  }

  @Override
  public List<String> getTriggerKeywords() {
    return Arrays.asList(
        "optimize", "performance", "memory", "cpu", "resource", "efficiency", "speed");
  }

  @Override
  public Mono<String> process(String task, Map<String, Object> context) {
    log.info("[EpsilonAgent] Analyzing resource optimization for task: {}", task);

    return Mono.fromCallable(
            () -> {
              Double memoryUsage = (Double) context.getOrDefault("memoryUsageMb", 0.0);
              Double cpuUsage = (Double) context.getOrDefault("cpuUsagePct", 0.0);
              Long responseTime = (Long) context.getOrDefault("responseTimeMs", 0L);
              Integer concurrentRequests = (Integer) context.getOrDefault("concurrentRequests", 0);
              String endpoint = (String) context.getOrDefault("endpoint", "/api/unknown");

              ResourceMetrics metrics =
                  analyzeResource(
                      endpoint, memoryUsage, cpuUsage, responseTime, concurrentRequests);
              OptimizationPlan plan = generateOptimizationPlan(metrics);

              return generateReport(metrics, plan);
            })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
  }

  public ResourceMetrics analyzeResource(
      String endpoint, Double memoryMb, Double cpuPct, Long responseMs, Integer concurrent) {
    ResourceMetrics metrics = resourceMetrics.computeIfAbsent(endpoint, ResourceMetrics::new);

    if (memoryMb != null) metrics.recordMemory(memoryMb);
    if (cpuPct != null) metrics.recordCpu(cpuPct);
    if (responseMs != null) metrics.recordResponseTime(responseMs);
    if (concurrent != null) metrics.recordConcurrency(concurrent);

    return metrics;
  }

  public OptimizationPlan generateOptimizationPlan(ResourceMetrics metrics) {
    List<OptimizationAction> actions = new ArrayList<>();
    double score = 100.0;

    if (metrics.avgResponseTime > 2000) {
      actions.add(
          new OptimizationAction(
              "Enable caching", "HIGH", "Response time > 2s, consider L1/L2 cache"));
      score -= 20;
    }

    if (metrics.avgCpu > 80) {
      actions.add(
          new OptimizationAction(
              "Scale horizontally", "CRITICAL", "CPU > 80%, add more instances"));
      score -= 30;
    }

    if (metrics.avgMemory > 500) {
      actions.add(
          new OptimizationAction("Memory profiling", "HIGH", "Memory > 500MB, check for leaks"));
      score -= 25;
    }

    if (metrics.p99ResponseTime > 5000) {
      actions.add(
          new OptimizationAction(
              "Async processing", "MEDIUM", "Slow 99th percentile, offload work"));
      score -= 15;
    }

    if (actions.isEmpty()) {
      actions.add(
          new OptimizationAction("System optimal", "INFO", "No optimization needed at this time"));
    }

    return new OptimizationPlan(actions, score, score >= 80);
  }

  private String generateReport(ResourceMetrics metrics, OptimizationPlan plan) {
    StringBuilder report = new StringBuilder();
    report.append("[EpsilonAgent] Resource Optimization Report:\n\n");
    report.append("Endpoint: ").append(metrics.endpoint).append("\n");
    report.append("Current Metrics:\n");
    report.append("  Avg Response Time: ").append(metrics.avgResponseTime).append("ms\n");
    report.append("  99th Percentile: ").append(metrics.p99ResponseTime).append("ms\n");
    report.append("  Avg Memory: ").append(String.format("%.1f", metrics.avgMemory)).append("MB\n");
    report.append("  Avg CPU: ").append(String.format("%.1f", metrics.avgCpu)).append("%\n");
    report.append("  Max Concurrent: ").append(metrics.maxConcurrent).append("\n");
    report
        .append("  Health Score: ")
        .append(String.format("%.0f", plan.healthScore))
        .append("/100\n\n");

    report.append("Optimization Actions:\n");
    for (OptimizationAction action : plan.actions) {
      report
          .append("  [")
          .append(action.priority())
          .append("] ")
          .append(action.title())
          .append("\n    ")
          .append(action.description())
          .append("\n");
    }

    report.append("\nCache Statistics:\n");
    try {
      ResponseCacheService.CacheStats cacheStats = cacheService.getStats();
      report
          .append("  Hit Rate: ")
          .append(String.format("%.1f%%", cacheStats.hitRate() * 100))
          .append("\n");
      report.append("  Cache Size: ").append(cacheStats.size()).append(" entries\n");
    } catch (Exception e) {
      report.append("  (cache unavailable)\n");
    }

    return report.toString();
  }

  public record OptimizationAction(String title, String priority, String description) {}

  public record OptimizationPlan(
      List<OptimizationAction> actions, double healthScore, boolean isHealthy) {}

  public static class ResourceMetrics {
    private final String endpoint;
    private final List<Long> responseTimes = new ArrayList<>();
    private double avgMemory = 0;
    private double avgCpu = 0;
    private int maxConcurrent = 0;
    private double sumResponseTime = 0;
    private int responseCount = 0;

    public ResourceMetrics(String endpoint) {
      this.endpoint = endpoint;
    }

    public double avgResponseTime;
    public long p99ResponseTime = 0;

    public void recordMemory(double mb) {
      this.avgMemory = (this.avgMemory * 0.7 + mb * 0.3);
    }

    public void recordCpu(double pct) {
      this.avgCpu = (this.avgCpu * 0.7 + pct * 0.3);
    }

    public void recordResponseTime(long ms) {
      responseTimes.add(ms);
      sumResponseTime += ms;
      responseCount++;
      avgResponseTime = sumResponseTime / responseCount;

      responseTimes.sort(Long::compareTo);
      int p99Index = (int) (responseTimes.size() * 0.99);
      if (p99Index < responseTimes.size()) {
        p99ResponseTime = responseTimes.get(p99Index);
      }
    }

    public void recordConcurrency(int concurrent) {
      maxConcurrent = Math.max(maxConcurrent, concurrent);
    }
  }
}
