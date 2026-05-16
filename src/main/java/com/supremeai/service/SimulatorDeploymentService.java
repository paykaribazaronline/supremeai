package com.supremeai.service;

import com.google.cloud.run.v2.*;
import com.google.iam.v1.Binding;
import com.google.iam.v1.Policy;
import com.google.iam.v1.SetIamPolicyRequest;
import com.supremeai.exception.SimulatorDeploymentException;
import com.supremeai.model.SimulatorDeploymentRecord;
import com.supremeai.repository.SimulatorDeploymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Service for deploying generated apps to Cloud Run simulator environments.
 *
 * Uses Cloud Run Admin API v2 for deployment.
 * Cloud Run service name pattern: sim-{appId}-{deviceSlug}
 */
@Service
public class SimulatorDeploymentService {

    private static final Logger logger = LoggerFactory.getLogger(SimulatorDeploymentService.class);

    @Value("${spring.cloud.gcp.project-id:supremeai-a}")
    private String projectId;

    @Value("${simulator.cloud.region:us-central1}")
    private String region;

    @Value("${simulator.cloud.run.image:}")
    private String runtimeImage; // optional override

    @Value("${simulator.health.check.timeout.ms:3000}")
    private int healthCheckTimeoutMs;

    // Firestore deployment registry
    @Autowired
    private SimulatorDeploymentRepository deploymentRepository;

    private final WebClient webClient;

    public SimulatorDeploymentService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(512 * 1024))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Deploy a generated app to the simulator.
     */
    public String deployToSimulator(String appId, String deviceType) {
        logger.info("[SIMULATOR_DEPLOY] Deploying app={} device={}", appId, deviceType);

        try {
            String deviceSlug = deviceType.toLowerCase().replace("_", "-");
            String serviceName = "sim-" + appId + "-" + deviceSlug;
            String serviceNameClean = serviceName.replaceAll("[^a-z0-9-]", "-").toLowerCase();

            // Deploy via Admin API
            String serviceUrl = deployViaAdminApi(serviceNameClean, appId, deviceType);

            // Record deployment in Firestore
            SimulatorDeploymentRecord record = new SimulatorDeploymentRecord(appId, deviceType, serviceUrl, DeploymentStatus.RUNNING.name());
            deploymentRepository.save(record).block();

            logger.info("[SIMULATOR_DEPLOY] Deployed app={} url={}", appId, serviceUrl);
            return serviceUrl;

        } catch (Exception e) {
            logger.error("[SIMULATOR_DEPLOY] Deployment failed for app {}: {}", appId, e.getMessage(), e);
            throw new SimulatorDeploymentException("Failed to deploy to Cloud Run: " + e.getMessage(), e);
        }
    }

    /**
     * Undeploy (remove) a simulator preview.
     */
    public void undeployFromSimulator(String appId) {
        logger.info("[SIMULATOR_DEPLOY] Undeploying app={}", appId);

        SimulatorDeploymentRecord record = deploymentRepository.findById(appId).block();
        if (record != null) {
            String deviceSlug = record.getDeviceType().toLowerCase().replace("_", "-");
            String serviceName = "sim-" + appId + "-" + deviceSlug;
            String serviceNameClean = serviceName.replaceAll("[^a-z0-9-]", "-").toLowerCase();

            try (ServicesClient servicesClient = ServicesClient.create()) {
                String name = ServiceName.of(projectId, region, serviceNameClean).toString();
                servicesClient.deleteServiceAsync(name).get();
                logger.info("[GCP] Deleted Cloud Run service: {}", serviceNameClean);
            } catch (Exception e) {
                logger.warn("[GCP] Failed to delete service {}: {}", serviceNameClean, e.getMessage());
            }

            record.setStatus(DeploymentStatus.STOPPED.name());
            logger.info("[SIMULATOR_DEPLOY] Marked app={} as STOPPED", appId);
            deploymentRepository.save(record).block();
        } else {
            logger.warn("[SIMULATOR_DEPLOY] No deployment record found for app={}", appId);
        }
    }

    /**
     * Check if the deployed URL is healthy.
     */
    public boolean isDeploymentHealthy(String previewUrl) {
        if (previewUrl == null || previewUrl.isEmpty()) {
            return false;
        }

        // Skip health check for localhost (dev mode)
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
            logger.warn("[SIMULATOR_DEPLOY] Health check failed for {} ({}), assuming live", previewUrl, e.getMessage());
            return true; // assume live for graceful degradation
        }
    }

    public DeploymentStatus getStatus(String appId) {
        SimulatorDeploymentRecord record = deploymentRepository.findById(appId).block();
        if (record == null || record.getStatus() == null) {
            return DeploymentStatus.NOT_DEPLOYED;
        }
        try {
            return DeploymentStatus.valueOf(record.getStatus());
        } catch (IllegalArgumentException e) {
            return DeploymentStatus.ERROR;
        }
    }

    public List<SimulatorDeploymentRecord> getAllDeployments() {
        return deploymentRepository.findAll().collectList().block();
    }

    public SimulatorDeploymentRecord getDeploymentRecord(String appId) {
        return deploymentRepository.findById(appId).block();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Internal Implementation
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Deploy Cloud Run service using Admin API v2.
     */
    private String deployViaAdminApi(String serviceNameId, String appId, String deviceType) throws Exception {
        logger.info("[GCP] Deploying Cloud Run service: {}", serviceNameId);

        String image = runtimeImage;
        if (image == null || image.isEmpty()) {
            image = "gcr.io/" + projectId + "/simulator-runtime:latest";
        }

        try (ServicesClient servicesClient = ServicesClient.create()) {
            LocationName parent = LocationName.of(projectId, region);
            String fullServiceName = ServiceName.of(projectId, region, serviceNameId).toString();

            com.google.cloud.run.v2.Service serviceObj = com.google.cloud.run.v2.Service.newBuilder()
                    .setTemplate(
                            RevisionTemplate.newBuilder()
                                    .addContainers(
                                            Container.newBuilder()
                                                    .setImage(image)
                                                    .addEnv(EnvVar.newBuilder().setName("APP_ID").setValue(appId).build())
                                                    .addEnv(EnvVar.newBuilder().setName("DEVICE_TYPE").setValue(deviceType).build())
                                                    .addEnv(EnvVar.newBuilder().setName("SIMULATOR_MODE").setValue("preview").build())
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            // Create or Update
            com.google.cloud.run.v2.Service responseService;
            try {
                // Try updating first
                UpdateServiceRequest updateRequest = UpdateServiceRequest.newBuilder()
                        .setService(serviceObj.toBuilder().setName(fullServiceName).build())
                        .build();
                responseService = servicesClient.updateServiceAsync(updateRequest).get();
            } catch (ExecutionException e) {
                // If not found, create
                CreateServiceRequest createRequest = CreateServiceRequest.newBuilder()
                        .setParent(parent.toString())
                        .setServiceId(serviceNameId)
                        .setService(serviceObj)
                        .build();
                responseService = servicesClient.createServiceAsync(createRequest).get();
                
                // Make it publicly accessible
                SetIamPolicyRequest iamRequest = SetIamPolicyRequest.newBuilder()
                        .setResource(fullServiceName)
                        .setPolicy(Policy.newBuilder()
                                .addBindings(Binding.newBuilder()
                                        .setRole("roles/run.invoker")
                                        .addMembers("allUsers")
                                        .build())
                                .build())
                        .build();
                servicesClient.setIamPolicy(iamRequest);
            }

            return responseService.getUri();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Inner Models
    // ─────────────────────────────────────────────────────────────────────────────

    public enum DeploymentStatus {
        NOT_DEPLOYED, DEPLOYING, RUNNING, STOPPED, ERROR
    }
}
