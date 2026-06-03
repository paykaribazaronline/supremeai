package com.supremeai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Production Deployment Configuration - Phase 4
 * Configures production-ready settings for deployment, monitoring, and operations.
 */
@Configuration
@EnableScheduling
public class ProductionDeploymentConfig {
    public ProductionDeploymentConfig(String activeProfile, int serverPort, String exposedEndpoints, String cloudRegion, String simulatorImage, int responseTimeThreshold, double errorRateThreshold, long healthCheckInterval, int metricsRetentionHours) {
        this.activeProfile = activeProfile;
        this.serverPort = serverPort;
        this.exposedEndpoints = exposedEndpoints;
        this.cloudRegion = cloudRegion;
        this.simulatorImage = simulatorImage;
        this.responseTimeThreshold = responseTimeThreshold;
        this.errorRateThreshold = errorRateThreshold;
        this.healthCheckInterval = healthCheckInterval;
        this.metricsRetentionHours = metricsRetentionHours;
    }


    private static final Logger logger = LoggerFactory.getLogger(ProductionDeploymentConfig.class);










    @Bean
    public DeploymentInfo deploymentInfo() {
        return new DeploymentInfo(
                isProduction(),
                activeProfile,
                serverPort,
                cloudRegion,
                simulatorImage,
                responseTimeThreshold,
                errorRateThreshold,
                healthCheckInterval,
                metricsRetentionHours
        );
    }

    public boolean isProduction() {
        return "prod".equals(activeProfile) || activeProfile.contains("prod");
    }

    public boolean isStaging() {
        return "staging".equals(activeProfile) || activeProfile.contains("staging");
    }

    /**
     * Deployment environment information
     */
    public static class DeploymentInfo {
        private final boolean production;
        private final String profile;
        private final int port;
        private final String region;
        private final String image;
        private final int responseTimeThreshold;
        private final double errorRateThreshold;
        private final long healthCheckInterval;
        private final int metricsRetentionHours;

        public DeploymentInfo(boolean production, String profile, int port, String region,
                              String image, int responseTimeThreshold, double errorRateThreshold,
                              long healthCheckInterval, int metricsRetentionHours) {
            this.production = production;
            this.profile = profile;
            this.port = port;
            this.region = region;
            this.image = image;
            this.responseTimeThreshold = responseTimeThreshold;
            this.errorRateThreshold = errorRateThreshold;
            this.healthCheckInterval = healthCheckInterval;
            this.metricsRetentionHours = metricsRetentionHours;
        }

        public boolean isProduction() { return production; }
        public String getProfile() { return profile; }
        public int getPort() { return port; }
        public String getRegion() { return region; }
        public String getImage() { return image; }
        public int getResponseTimeThreshold() { return responseTimeThreshold; }
        public double getErrorRateThreshold() { return errorRateThreshold; }
        public long getHealthCheckInterval() { return healthCheckInterval; }
        public int getMetricsRetentionHours() { return metricsRetentionHours; }

        @Override
        public String toString() {
            return String.format("DeploymentInfo{profile=%s, prod=%s, port=%d, region=%s}",
                    profile, production, port, region);
        }
    }
}