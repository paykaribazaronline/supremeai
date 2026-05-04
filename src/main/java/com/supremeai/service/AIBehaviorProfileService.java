package com.supremeai.service;

import com.supremeai.model.AIBehaviorProfile;
import com.supremeai.repository.AIBehaviorProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Map;

@Profile("!local")
@Service
public class AIBehaviorProfileService {
    private static final Logger logger = LoggerFactory.getLogger(AIBehaviorProfileService.class);

    @Autowired
    private AIBehaviorProfileRepository profileRepository;

    public Mono<AIBehaviorProfile> getProfileForProject(String projectId) {
        return profileRepository.findFirstByProjectId(projectId)
                .switchIfEmpty(Mono.defer(this::getDefaultProfile));
    }

    public Mono<AIBehaviorProfile> saveProfile(AIBehaviorProfile profile) {
        if (profile.getId() == null || profile.getId().isEmpty()) {
            profile.setId(java.util.UUID.randomUUID().toString());
        }
        return profileRepository.save(profile)
                .doOnSuccess(saved -> logger.info("Saved AI behavior profile for project: {}", saved.getProjectId()));
    }

    public Flux<AIBehaviorProfile> getAllProfiles() {
        return profileRepository.findAll();
    }

    public Mono<Void> deleteProfile(String id) {
        return profileRepository.deleteById(id)
                .doOnSuccess(v -> logger.info("Deleted AI behavior profile: {}", id));
    }

    public Mono<AIBehaviorProfile> getDefaultProfile() {
        AIBehaviorProfile defaultProfile = new AIBehaviorProfile();
        defaultProfile.setId("default");
        defaultProfile.setProjectId("default");
        defaultProfile.setFrameworkVersion("Spring Boot 3.2.3");
        defaultProfile.setSecurityStrictness(AIBehaviorProfile.SecurityStrictness.MEDIUM);
        defaultProfile.setPerformanceTradeoff(AIBehaviorProfile.PerformanceTradeoff.BALANCED);
        defaultProfile.setAdditionalPreferences(Map.of(
                "includeTests", true,
                "includeDocker", true,
                "javaVersion", "17"
        ));
        return Mono.just(defaultProfile);
    }

    public void applyProfileToContext(AIBehaviorProfile profile, Map<String, Object> generationContext) {
        if (profile == null) return;

        if (profile.getFrameworkVersion() != null) {
            if (profile.getFrameworkVersion().contains("Spring")) {
                generationContext.put("springBootVersion", extractVersion(profile.getFrameworkVersion()));
            }
        }

        if (profile.getSecurityStrictness() != null) {
            switch (profile.getSecurityStrictness()) {
                case HIGH:
                    generationContext.put("securityLevel", "high");
                    generationContext.put("enableEncryption", true);
                    generationContext.put("enableAuditLogging", true);
                    break;
                case MEDIUM:
                    generationContext.put("securityLevel", "medium");
                    generationContext.put("enableEncryption", false);
                    generationContext.put("enableAuditLogging", true);
                    break;
                case LOW:
                    generationContext.put("securityLevel", "low");
                    generationContext.put("enableEncryption", false);
                    generationContext.put("enableAuditLogging", false);
                    break;
            }
        }

        if (profile.getPerformanceTradeoff() != null) {
            switch (profile.getPerformanceTradeoff()) {
                case SPEED_OPTIMIZED:
                    generationContext.put("optimizeFor", "speed");
                    generationContext.put("enableCaching", true);
                    generationContext.put("minifyCode", true);
                    break;
                case QUALITY_OPTIMIZED:
                    generationContext.put("optimizeFor", "quality");
                    generationContext.put("includeComments", true);
                    generationContext.put("enableDetailedLogging", true);
                    break;
                case BALANCED:
                    generationContext.put("optimizeFor", "balanced");
                    break;
            }
        }

        if (profile.getAdditionalPreferences() != null) {
            generationContext.putAll(profile.getAdditionalPreferences());
        }

        logger.info("Applied behavior profile to generation context for project: {}", profile.getProjectId());
    }

    private String extractVersion(String frameworkVersion) {
        String[] parts = frameworkVersion.split(" ");
        for (String part : parts) {
            if (part.matches("\\d+\\.\\d+(\\.\\d+)?")) {
                return part;
            }
        }
        return "3.2.3";
    }
}
