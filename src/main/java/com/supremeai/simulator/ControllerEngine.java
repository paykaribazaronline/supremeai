package com.supremeai.simulator;

import com.supremeai.model.SimulationResult;
import com.supremeai.model.SimulationScenario;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ControllerEngine {

  private static final Logger logger = LoggerFactory.getLogger(ControllerEngine.class);
  private static final Random random = new Random();

  public Mono<SimulationResult> runSimulation(
      SimulationScenario scenario, SimulationResult result) {
    logger.info("ControllerEngine starting simulation for scenario: {}", scenario.getScenarioId());

    DeviceProfile profile = DeviceProfiles.resolve(scenario.getDeviceProfile());

    return Mono.delay(Duration.ofSeconds(2))
        .map(
            ignored -> {
              result.getLogs().add("Env allocated -> profile=" + profile.getProfileId());
              result.getLogs().add("Target app: " + scenario.getAppId());
              result
                  .getLogs()
                  .add(
                      "Device: "
                          + profile.getDeviceName()
                          + " ("
                          + profile.getOs()
                          + " "
                          + profile.getOsVersion()
                          + ")");
              result
                  .getLogs()
                  .add(
                      "Screen: "
                          + profile.getScreenResolution()
                          + " @ "
                          + profile.getDensityDpi()
                          + "dpi");
              result
                  .getLogs()
                  .add(
                      "Network: "
                          + profile.getNetworkType()
                          + " "
                          + profile.getNetworkSpeedMbps()
                          + "Mbps");

              Map<String, Object> params = scenario.getParameters();
              if (params == null || params.isEmpty()) {
                params = new HashMap<>();
                params.put("networkSpeed", profile.getNetworkType());
                params.put("concurrency", 10);
                params.put("durationSeconds", 15);
              }
              scenario.setParameters(params);

              String networkSpeed =
                  (String) params.getOrDefault("networkSpeed", profile.getNetworkType());
              Number concurrency = (Number) params.getOrDefault("concurrency", 10);
              Number testDurationSec = (Number) params.getOrDefault("durationSeconds", 15);

              result
                  .getLogs()
                  .add(
                      String.format(
                          "Applied parameters -> Network: %s, Concurrency: %s, Test Duration: %s seconds",
                          networkSpeed, concurrency, testDurationSec));

              Map<String, Object> metrics = buildMetrics(profile, params);
              result.setMetrics(metrics);

              result
                  .getLogs()
                  .add(
                      "Simulation actions: [Launch App -> Tap Home -> Input Text -> Submit Form -> Rotate Device -> Verify Response]");
              result
                  .getLogs()
                  .add(
                      String.format(
                          "Metrics -> Load: %dms, CPU: %.1f%%, Mem: %.1fMB, Errors: %.1f%%, Resp: %dms",
                          (Integer) metrics.get("loadTimeMs"),
                          (Double) metrics.get("cpuUsagePct"),
                          (Double) metrics.get("memoryUsageMb"),
                          (Double) metrics.get("errorRatePct"),
                          (Integer) metrics.get("responseTimeMs")));
              result
                  .getLogs()
                  .add("Simulation completed successfully at " + java.time.LocalDateTime.now());

              return result;
            });
  }

  private Map<String, Object> buildMetrics(DeviceProfile profile, Map<String, Object> params) {
    Map<String, Object> metrics = new HashMap<>();
    String networkSpeed = (String) params.getOrDefault("networkSpeed", profile.getNetworkType());
    Number concurrency = (Number) params.getOrDefault("concurrency", 10);

    int baselineLoadTime = 120;
    switch (networkSpeed.toUpperCase()) {
      case "3G" -> baselineLoadTime += 450;
      case "2G" -> baselineLoadTime += 1200;
      case "WIFI", "ETHERNET" -> baselineLoadTime -= 20;
    }

    int loadTimeMs = baselineLoadTime + random.nextInt(150);
    double cpuUsagePct = 15.0 + (concurrency.doubleValue() * 1.5) + (random.nextDouble() * 5.0);
    double memoryUsageMb =
        (profile.getMemoryMb() != null ? profile.getMemoryMb() / 256.0 : 500.0)
            + (concurrency.doubleValue() * 8.0)
            + random.nextInt(30);
    double errorRatePct =
        concurrency.doubleValue() > 50 ? (concurrency.doubleValue() - 50) * 0.4 : 0.0;

    if (random.nextDouble() < 0.05) {
      errorRatePct += 2.5;
    }

    int responseTimeMs = 50 + (int) (concurrency.doubleValue() * 3.2) + random.nextInt(40);

    metrics.put("loadTimeMs", loadTimeMs);
    metrics.put("cpuUsagePct", Math.min(100.0, cpuUsagePct));
    metrics.put("memoryUsageMb", memoryUsageMb);
    metrics.put("errorRatePct", errorRatePct);
    metrics.put("responseTimeMs", responseTimeMs);

    return metrics;
  }
}
