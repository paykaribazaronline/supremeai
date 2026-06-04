package com.supremeai.simulator;

import com.supremeai.model.SimulationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Processes simulation result metrics and generates optimization recommendations. */
@Service
public class ResultAnalyzer {

  private static final Logger logger = LoggerFactory.getLogger(ResultAnalyzer.class);

  /** Evaluates simulation result metrics and adds targeted optimization recommendations. */
  public Mono<SimulationResult> analyze(SimulationResult result) {
    logger.info("Analyzing simulation results for result ID: {}", result.getResultId());

    return Mono.fromCallable(
        () -> {
          Map<String, Object> metrics = result.getMetrics();
          List<String> recommendations = new ArrayList<>();

          if (metrics == null || metrics.isEmpty()) {
            recommendations.add(
                "No metrics collected. Recommend running simulation with interactive parameters.");
            result.setRecommendations(recommendations);
            return result;
          }

          // Extract performance values
          Number loadTimeNum = (Number) metrics.getOrDefault("loadTimeMs", 0);
          Number cpuUsageNum = (Number) metrics.getOrDefault("cpuUsagePct", 0.0);
          Number memoryUsageNum = (Number) metrics.getOrDefault("memoryUsageMb", 0.0);
          Number errorRateNum = (Number) metrics.getOrDefault("errorRatePct", 0.0);
          Number responseTimeNum = (Number) metrics.getOrDefault("responseTimeMs", 0);

          int loadTimeMs = loadTimeNum.intValue();
          double cpuUsagePct = cpuUsageNum.doubleValue();
          double memoryUsageMb = memoryUsageNum.doubleValue();
          double errorRatePct = errorRateNum.doubleValue();
          int responseTimeMs = responseTimeNum.intValue();

          // 1. Analyze load time (Threshold: > 1000ms)
          if (loadTimeMs > 1000) {
            recommendations.add(
                "High startup latency detected ("
                    + loadTimeMs
                    + "ms). Consider enabling code-splitting, lazy-loading widgets, and optimizing heavy static assets.");
          } else {
            recommendations.add(
                "Excellent startup latency ("
                    + loadTimeMs
                    + "ms). Keep using standard tree shaking and asset minification.");
          }

          // 2. Analyze CPU usage (Threshold: > 70%)
          if (cpuUsagePct > 70.0) {
            recommendations.add(
                "High CPU peak load ("
                    + String.format("%.1f", cpuUsagePct)
                    + "%). Avoid unnecessary widget rebuilds and throttle active listener callbacks.");
          }

          // 3. Analyze Memory usage (Threshold: > 250MB)
          if (memoryUsageMb > 250.0) {
            recommendations.add(
                "Memory footprints are high ("
                    + String.format("%.1f", memoryUsageMb)
                    + "MB). Optimize caching strategies and ensure all listeners, controllers, and streams are closed on widget dispose.");
          }

          // 4. Analyze Error rate (Threshold: > 0%)
          if (errorRatePct > 0.0) {
            recommendations.add(
                "Failures detected during scenario execution ("
                    + String.format("%.1f", errorRatePct)
                    + "%). Audit exception handling logic and review console logs for uncaught network/null-pointer issues.");
          }

          // 5. Analyze Response time (Threshold: > 200ms)
          if (responseTimeMs > 200) {
            recommendations.add(
                "High interactive response latency ("
                    + responseTimeMs
                    + "ms). Consider offloading intensive computations to background isolates (Dart) or optimizing server-side database indices.");
          }

          if (recommendations.isEmpty()) {
            recommendations.add(
                "System is fully optimized! Performance parameters are within flawless thresholds.");
          }

          result.setRecommendations(recommendations);
          result.getLogs().add("Result Analysis completed at " + java.time.LocalDateTime.now());

          return result;
        });
  }
}
