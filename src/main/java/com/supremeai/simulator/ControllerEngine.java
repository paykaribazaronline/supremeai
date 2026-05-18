package com.supremeai.simulator;

import com.supremeai.model.SimulationScenario;
import com.supremeai.model.SimulationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Manages real-time parameter control, automated scenario execution, and performance monitoring.
 */
@Service
public class ControllerEngine {

    private static final Logger logger = LoggerFactory.getLogger(ControllerEngine.class);
    private final Random random = new Random();

    /**
     * Executes the simulation using parameters configured in the scenario.
     * Captures and records performance metrics on the result object.
     */
    public Mono<SimulationResult> runSimulation(SimulationScenario scenario, SimulationResult result) {
        logger.info("ControllerEngine starting simulation for scenario: {}", scenario.getScenarioId());

        return Mono.delay(Duration.ofSeconds(2)) // Simulate startup and deployment delay
            .map(ignored -> {
                result.getLogs().add("Environment allocated. Target app: " + scenario.getAppId());
                result.getLogs().add("Device Profile initialized: " + scenario.getDeviceProfile());
                
                // Parse dynamic simulation parameters
                Map<String, Object> params = scenario.getParameters();
                String networkSpeed = (String) params.getOrDefault("networkSpeed", "4G");
                Number concurrency = (Number) params.getOrDefault("concurrency", 10);
                Number testDurationSec = (Number) params.getOrDefault("durationSeconds", 15);
                
                result.getLogs().add(String.format("Applied parameters -> Network: %s, Concurrency: %s, Test Duration: %s seconds",
                    networkSpeed, concurrency, testDurationSec));
                
                // Mimic simulation actions & performance monitoring
                Map<String, Object> metrics = new HashMap<>();
                
                // Simulate metric values with some variations based on parameters
                int baselineLoadTime = 120; // ms
                if ("3G".equalsIgnoreCase(networkSpeed)) {
                    baselineLoadTime += 450;
                } else if ("2G".equalsIgnoreCase(networkSpeed)) {
                    baselineLoadTime += 1200;
                }
                
                int loadTimeMs = baselineLoadTime + random.nextInt(150);
                double cpuUsagePct = 15.0 + (concurrency.doubleValue() * 1.5) + (random.nextDouble() * 5.0);
                double memoryUsageMb = 120.0 + (concurrency.doubleValue() * 8.0) + random.nextInt(30);
                double errorRatePct = concurrency.doubleValue() > 50 ? (concurrency.doubleValue() - 50) * 0.4 : 0.0;
                
                if (random.nextDouble() < 0.05) { // 5% chance of spontaneous errors
                    errorRatePct += 2.5;
                }
                
                int responseTimeMs = 50 + (int)(concurrency.doubleValue() * 3.2) + random.nextInt(40);

                metrics.put("loadTimeMs", loadTimeMs);
                metrics.put("cpuUsagePct", Math.min(100.0, cpuUsagePct));
                metrics.put("memoryUsageMb", memoryUsageMb);
                metrics.put("errorRatePct", errorRatePct);
                metrics.put("responseTimeMs", responseTimeMs);
                
                result.setMetrics(metrics);
                
                result.getLogs().add("Simulation actions: [Tap Home -> Input Text -> Submit Form -> Rotate Device]");
                result.getLogs().add(String.format("Metrics collected: LoadTime: %dms, CPU: %.1f%%, Memory: %.1fMB, Errors: %.1f%%, RespTime: %dms",
                    loadTimeMs, cpuUsagePct, memoryUsageMb, errorRatePct, responseTimeMs));
                
                return result;
            });
    }
}
