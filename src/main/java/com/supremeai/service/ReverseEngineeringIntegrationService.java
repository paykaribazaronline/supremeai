package com.supremeai.service;

import com.supremeai.model.EntityDefinition;
import com.supremeai.model.ReverseEngineeringJob;
import com.supremeai.repository.ReverseEngineeringJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bridges completed reverse engineering jobs to code generation.
 * When a reverse engineering job finishes, this service can automatically
 * generate an application that integrates with the discovered APIs.
 */
@Service
public class ReverseEngineeringIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(ReverseEngineeringIntegrationService.class);

    @Autowired
    private ReverseEngineeringJobRepository jobRepository;

    @Autowired
    private CodeGenerationService codeGenerationService;

    @Autowired
    private PubSubPublisherService pubSubPublisherService;

    @Autowired
    private SimulatorService simulatorService;

    private static final String PUBSUB_TOPIC = "reverse-engineering-jobs";

    /**
     * Called when a reverse engineering job completes.
     * Generates an app that uses the discovered APIs and optionally deploys to simulator.
     */
    public Mono<ReverseEngineeringJob> onJobCompletion(String jobId, String userId) {
        logger.info("[ReverseEngIntegration] Processing completed job: {}", jobId);

        return jobRepository.findByJobId(jobId)
            .flatMap(job -> {
                if (!job.getUserId().equals(userId)) {
                    logger.error("[ReverseEngIntegration] Unauthorized access attempt for job {} by user {}", jobId, userId);
                    return Mono.error(new RuntimeException("Unauthorized: job does not belong to user"));
                }
                if (!"COMPLETED".equals(job.getStatus())) {
                    logger.warn("[ReverseEngIntegration] Job {} is in status {}, cannot integrate yet", jobId, job.getStatus());
                    return Mono.empty();
                }

                // Build requirements from discovered APIs
                String requirements = buildRequirementsFromJob(job);
                Map<String, Object> apis = job.getDiscoveredApis();

                // Prepare entities from API endpoints
                List<EntityDefinition> entities = extractEntitiesFromApis(apis);

                // Trigger code generation with AI
                // Note: CodeGenerationService.generateAppWithAI is currently synchronous, wrapping in Mono.fromCallable or defer if needed
                return Mono.fromCallable(() -> codeGenerationService.generateAppWithAI(
                    "API Integration: " + job.getWebsiteUrl(),
                    requirements,
                    entities,
                    "PostgreSQL",
                    "JWT"
                )).flatMap(result -> {
                    String appId = (String) result.get("appId");
                    job.setGeneratedAppId(appId);
                    job.setStatus("INTEGRATED");
                    job.setUpdatedAt(new Date());
                    
                    return jobRepository.save(job)
                        .doOnNext(savedJob -> logger.info("[ReverseEngIntegration] Successfully integrated job {} into app {}", jobId, appId));
                });
            });
    }

    private String buildRequirementsFromJob(ReverseEngineeringJob job) {
        Map<String, Object> apis = job.getDiscoveredApis();
        if (apis != null && !apis.isEmpty()) {
            return String.format(
                "Build a complete Spring Boot 3 & React application that integrates with the following discovered APIs from %s: %s. " +
                "The app should include high-fidelity UI components, secure authentication, and real-time data sync.",
                job.getWebsiteUrl(),
                String.join(", ", apis.keySet())
            );
        }
        return String.format(
            "Create a premium application inspired by the design and data of %s.",
            job.getWebsiteUrl()
        );
    }

    private List<EntityDefinition> extractEntitiesFromApis(Map<String, Object> apis) {
        if (apis == null || apis.isEmpty()) return List.of();
        
        List<EntityDefinition> entities = new ArrayList<>();
        
        // Basic heuristic: create entities based on common API patterns (users, products, posts)
        apis.keySet().forEach(key -> {
            String entityName = key.substring(0, 1).toUpperCase() + key.substring(1).replaceAll("s$", "");
            if (entityName.length() > 2) {
                EntityDefinition entity = new EntityDefinition();
                entity.setName(entityName);
                entity.setTableName(key.toLowerCase());
                entities.add(entity);
            }
        });
        
        if (entities.isEmpty()) {
            EntityDefinition defaultEntity = new EntityDefinition();
            defaultEntity.setName("SiteData");
            defaultEntity.setTableName("site_data");
            entities.add(defaultEntity);
        }
        
        return entities;
    }

    /**
     * In production, this would be invoked via Pub/Sub or HTTP webhook.
     */
    public Mono<ReverseEngineeringJob> startJob(String userId, String websiteUrl, String taskType, String customInstructions, Map<String, Object> extraParams) {
        String jobId = "reveng_" + UUID.randomUUID().toString().substring(0, 12);
        ReverseEngineeringJob job = new ReverseEngineeringJob(jobId, userId, websiteUrl, taskType);
        job.setCustomInstructions(customInstructions);
        job.setStatus("PENDING");
        job.setCreatedAt(new Date());
        
        return jobRepository.save(job)
            .doOnNext(saved -> {
                logger.info("[ReverseEngIntegration] Job created: {} for {} (Type: {})", jobId, websiteUrl, taskType);
                
                // Publish to Pub/Sub
                Map<String, Object> message = new HashMap<>();
                message.put("jobId", jobId);
                message.put("userId", userId);
                message.put("websiteUrl", websiteUrl);
                message.put("taskType", taskType);
                message.put("customInstructions", customInstructions);
                
                if (extraParams != null) {
                    message.putAll(extraParams);
                }
                
                try {
                    pubSubPublisherService.publish(PUBSUB_TOPIC, message);
                    logger.info("[ReverseEngIntegration] Published job {} to topic {}", jobId, PUBSUB_TOPIC);
                } catch (Exception e) {
                    logger.error("[ReverseEngIntegration] Failed to publish to Pub/Sub: {}", e.getMessage());
                }
            });
    }

    /**
     * Simulate job completion (for testing). In production, Python FastAPI worker calls this.
     */
    public Mono<ReverseEngineeringJob> completeJob(String jobId, Map<String, Object> discoveredApis) {
        return jobRepository.findByJobId(jobId)
            .flatMap(job -> {
                job.setStatus("COMPLETED");
                job.setDiscoveredApis(discoveredApis);
                return jobRepository.save(job);
            })
            .doOnNext(job -> logger.info("[ReverseEngIntegration] Job marked completed: {}", jobId));
    }

    /**
     * Fetch recent reverse engineering jobs (for admin history).
     * Fetches all, sorts by createdAt descending, limits to N.
     */
    public Mono<List<ReverseEngineeringJob>> getRecentJobs(int limit) {
        return jobRepository.findAll()
            .collectList()
            .map(list -> list.stream()
                .sorted((j1, j2) -> {
                    if (j1.getCreatedAt() == null && j2.getCreatedAt() == null) return 0;
                    if (j1.getCreatedAt() == null) return 1;
                    if (j2.getCreatedAt() == null) return -1;
                    return j2.getCreatedAt().compareTo(j1.getCreatedAt());
                })
                .limit(limit)
                .collect(Collectors.toList())
            );
    }
}
