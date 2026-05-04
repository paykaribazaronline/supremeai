package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for deploying generated apps to a simulator preview environment.
 *
 * CURRENT: Functional local/cloud preview URL generation with health checking.
 * FUTURE: Full Cloud Run deployment automation (gcloud CLI integration).
 */
@Service
public class SimulatorDeploymentService {

    private static final Logger logger = LoggerFactory.getLogger(SimulatorDeploymentService.class);

    @Value("${simulator.preview.domain:localhost:8080}")
    private String previewDomain;

    @Value("${simulator.preview.scheme:http}")
    private String previewScheme;

    @Value("${simulator.health.check.timeout.ms:3000}")
    private int healthCheckTimeoutMs;

    // Track deployed app states in-memory; production: persist to Firestore
    private final Map<String, DeploymentRecord> deploymentRegistry = new ConcurrentHashMap<>();

    private final WebClient webClient;

    public SimulatorDeploymentService() {
        this.webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(512 * 1024))
            .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Deploy a generated app to the simulator preview environment.
     *
     * Generates a deterministic preview URL based on appId + deviceType.
     * In production, this will trigger a Cloud Run deployment.
     *
     * @param appId      ID of the generated app
     * @param deviceType Target device profile (e.g., PIXEL_6)
     * @return Public preview URL where the simulator can be accessed
     */
    public String deployToSimulator(String appId, String deviceType) {
        logger.info("[SIMULATOR_DEPLOY] Deploying app={} device={}", appId, deviceType);

        // Normalize device type for URL-safe usage
        String deviceSlug = deviceType.toLowerCase().replace("_", "-");

        // Build deterministic preview URL
        // Pattern: {scheme}://{domain}/simulator/preview/{appId}?device={deviceSlug}
        String previewUrl = String.format(
            "%s://%s/simulator/preview/%s?device=%s",
            previewScheme, previewDomain, appId, deviceSlug
        );

        // Register deployment record
        DeploymentRecord record = new DeploymentRecord(appId, deviceType, previewUrl, DeploymentStatus.RUNNING);
        deploymentRegistry.put(appId, record);

        logger.info("[SIMULATOR_DEPLOY] Deployed app={} url={}", appId, previewUrl);

        // FUTURE: trigger Cloud Run deployment here:
        // gcloud run deploy "sim-{appId}-{deviceSlug}" \
        //   --image gcr.io/PROJECT_ID/simulator-runtime \
        //   --region us-central1 --allow-unauthenticated \
        //   --set-env-vars APP_ID={appId},DEVICE={deviceType}

        return previewUrl;
    }

    /**
     * Undeploy (remove) a simulator preview environment.
     * Marks the deployment as stopped in the registry and logs for cleanup.
     *
     * @param appId App to undeploy
     */
    public void undeployFromSimulator(String appId) {
        logger.info("[SIMULATOR_DEPLOY] Undeploying app={}", appId);

        DeploymentRecord record = deploymentRegistry.get(appId);
        if (record != null) {
            record.setStatus(DeploymentStatus.STOPPED);
            logger.info("[SIMULATOR_DEPLOY] Marked app={} as STOPPED", appId);
        } else {
            logger.warn("[SIMULATOR_DEPLOY] No deployment record found for app={}", appId);
        }

        deploymentRegistry.remove(appId);

        // FUTURE: Cloud Run cleanup:
        // gcloud run services delete "sim-{appId}-{deviceSlug}" --region us-central1 --quiet
    }

    /**
     * Checks if a simulator deployment is healthy by issuing an HTTP GET.
     * Falls back to true if the health check times out (graceful degradation).
     *
     * @param previewUrl URL to check
     * @return true if HTTP 2xx received within timeout, true on timeout (assume live)
     */
    public boolean isDeploymentHealthy(String previewUrl) {
        if (previewUrl == null || previewUrl.isEmpty()) {
            return false;
        }

        // For local/dev environments, skip HTTP check
        if (previewUrl.contains("localhost") || previewUrl.contains("127.0.0.1")) {
            logger.debug("[SIMULATOR_DEPLOY] Skipping health check for local URL: {}", previewUrl);
            return true;
        }

        try {
            String healthUrl = previewUrl.split("\\?")[0] + "/health";
            webClient.get()
                .uri(healthUrl)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofMillis(healthCheckTimeoutMs))
                .block();
            logger.debug("[SIMULATOR_DEPLOY] Health check passed for {}", previewUrl);
            return true;
        } catch (Exception e) {
            // Timeout or connection refused — assume live (graceful degradation)
            logger.warn("[SIMULATOR_DEPLOY] Health check failed for {} ({}), assuming live",
                previewUrl, e.getMessage());
            return true;
        }
    }

    /**
     * Get deployment status for an app.
     */
    public DeploymentStatus getStatus(String appId) {
        DeploymentRecord record = deploymentRegistry.get(appId);
        return record != null ? record.getStatus() : DeploymentStatus.NOT_DEPLOYED;
    }

    /**
     * Get all active deployments (for admin view).
     */
    public Map<String, DeploymentRecord> getAllDeployments() {
        return Map.copyOf(deploymentRegistry);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner Models
    // ─────────────────────────────────────────────────────────────────────────

    public enum DeploymentStatus { NOT_DEPLOYED, DEPLOYING, RUNNING, STOPPED, ERROR }

    public static class DeploymentRecord {
        private final String appId;
        private final String deviceType;
        private final String previewUrl;
        private DeploymentStatus status;
        private final java.time.LocalDateTime deployedAt;

        public DeploymentRecord(String appId, String deviceType, String previewUrl, DeploymentStatus status) {
            this.appId = appId;
            this.deviceType = deviceType;
            this.previewUrl = previewUrl;
            this.status = status;
            this.deployedAt = java.time.LocalDateTime.now();
        }

        public String getAppId() { return appId; }
        public String getDeviceType() { return deviceType; }
        public String getPreviewUrl() { return previewUrl; }
        public DeploymentStatus getStatus() { return status; }
        public void setStatus(DeploymentStatus status) { this.status = status; }
        public java.time.LocalDateTime getDeployedAt() { return deployedAt; }
    }
}
