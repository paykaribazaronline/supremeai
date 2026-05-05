package com.supremeai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Blue-green deployment configuration.
 * Supports zero-downtime deployments with traffic switching.
 */
@Slf4j
@Configuration
public class BlueGreenDeploymentConfig implements ApplicationRunner {

    @Value("${deployment.strategy:blue-green}")
    private String deploymentStrategy;

    @Value("${deployment.color:blue}")
    private String deploymentColor;

    @Value("${deployment.version:1.0.0}")
    private String deploymentVersion;

    // Track active deployment
    private final AtomicReference<DeploymentInfo> activeDeployment = new AtomicReference<>();

    @Override
    public void run(ApplicationArguments args) {
        DeploymentInfo info = DeploymentInfo.builder()
                .color(deploymentColor)
                .version(deploymentVersion)
                .strategy(deploymentStrategy)
                .startTime(Instant.now())
                .build();
        
        activeDeployment.set(info);
        log.info("Active deployment: {} (version: {}, strategy: {})", 
                deploymentColor, deploymentVersion, deploymentStrategy);
    }

    /**
     * Get current active deployment info.
     */
    public DeploymentInfo getActiveDeployment() {
        return activeDeployment.get();
    }

    /**
     * Switch traffic to new deployment (blue-green switch).
     */
    public void switchTraffic(String newColor, String newVersion) {
        DeploymentInfo current = activeDeployment.get();
        DeploymentInfo newDeployment = DeploymentInfo.builder()
                .color(newColor)
                .version(newVersion)
                .strategy(deploymentStrategy)
                .startTime(Instant.now())
                .previousColor(current.getColor())
                .previousVersion(current.getVersion())
                .build();
        
        activeDeployment.set(newDeployment);
        log.info("Switched traffic to {} (version: {})", newColor, newVersion);
    }

    /**
     * Rollback to previous deployment.
     */
    public void rollback() {
        DeploymentInfo current = activeDeployment.get();
        if (current.getPreviousColor() != null) {
            DeploymentInfo rollbackDeployment = DeploymentInfo.builder()
                    .color(current.getPreviousColor())
                    .version(current.getPreviousVersion())
                    .strategy(deploymentStrategy)
                    .startTime(Instant.now())
                    .build();
            
            activeDeployment.set(rollbackDeployment);
            log.warn("Rolled back to {} (version: {})", 
                    current.getPreviousColor(), current.getPreviousVersion());
        }
    }

    @lombok.Builder
    @lombok.Data
    public static class DeploymentInfo {
        private String color;
        private String version;
        private String strategy;
        private Instant startTime;
        private String previousColor;
        private String previousVersion;
    }
}