package org.example.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deployment Service
 * Manages application deployment lifecycle and versioning
 */
public class DeploymentService {
    
    private final Map<String, DeploymentRecord> deployments = new ConcurrentHashMap<>();
    private final Map<String, ApplicationVersion> versions = new ConcurrentHashMap<>();
    private final List<DeploymentEvent> events = Collections.synchronizedList(new ArrayList<>());
    
    /**
     * Create new deployment
     */
    public DeploymentRecord createDeployment(String applicationName, String version, String environment) {
        String deploymentId = UUID.randomUUID().toString();
        DeploymentRecord record = new DeploymentRecord(
                deploymentId,
                applicationName,
                version,
                environment,
                System.currentTimeMillis()
        );
        deployments.put(deploymentId, record);
        recordEvent(deploymentId, "CREATED", "Deployment created");
        return record;
    }
    
    /**
     * Get deployment record
     */
    public DeploymentRecord getDeployment(String deploymentId) {
        return deployments.get(deploymentId);
    }
    
    /**
     * List all deployments
     */
    public Collection<DeploymentRecord> listDeployments() {
        return new ArrayList<>(deployments.values());
    }
    
    /**
     * List deployments by application
     */
    public List<DeploymentRecord> listDeploymentsByApplication(String applicationName) {
        return deployments.values().stream()
                .filter(d -> d.applicationName.equals(applicationName))
                .toList();
    }
    
    /**
     * List deployments by environment
     */
    public List<DeploymentRecord> listDeploymentsByEnvironment(String environment) {
        return deployments.values().stream()
                .filter(d -> d.environment.equals(environment))
                .toList();
    }
    
    /**
     * Start deployment
     */
    public void startDeployment(String deploymentId) {
        DeploymentRecord record = deployments.get(deploymentId);
        if (record != null) {
            record.status = DeploymentStatus.IN_PROGRESS;
            record.startedAt = System.currentTimeMillis();
            recordEvent(deploymentId, "STARTED", "Deployment started");
        }
    }
    
    /**
     * Complete deployment
     */
    public void completeDeployment(String deploymentId) {
        DeploymentRecord record = deployments.get(deploymentId);
        if (record != null) {
            record.status = DeploymentStatus.SUCCESS;
            record.completedAt = System.currentTimeMillis();
            recordEvent(deploymentId, "COMPLETED", "Deployment completed successfully");
        }
    }
    
    /**
     * Fail deployment
     */
    public void failDeployment(String deploymentId, String reason) {
        DeploymentRecord record = deployments.get(deploymentId);
        if (record != null) {
            record.status = DeploymentStatus.FAILED;
            record.completedAt = System.currentTimeMillis();
            record.failureReason = reason;
            recordEvent(deploymentId, "FAILED", "Deployment failed: " + reason);
        }
    }
    
    /**
     * Rollback deployment
     */
    public void rollbackDeployment(String deploymentId, String previousVersion) {
        DeploymentRecord record = deployments.get(deploymentId);
        if (record != null) {
            record.status = DeploymentStatus.ROLLED_BACK;
            record.rolledBackTo = previousVersion;
            record.completedAt = System.currentTimeMillis();
            recordEvent(deploymentId, "ROLLED_BACK", "Deployment rolled back to " + previousVersion);
        }
    }
    
    /**
     * Register application version
     */
    public ApplicationVersion registerVersion(String applicationName, String version, String artifactUrl, String releaseNotes) {
        String versionId = UUID.randomUUID().toString();
        ApplicationVersion appVersion = new ApplicationVersion(
                versionId,
                applicationName,
                version,
                artifactUrl,
                releaseNotes,
                System.currentTimeMillis()
        );
        versions.put(versionId, appVersion);
        return appVersion;
    }
    
    /**
     * Get version
     */
    public ApplicationVersion getVersion(String versionId) {
        return versions.get(versionId);
    }
    
    /**
     * List versions for application
     */
    public List<ApplicationVersion> listVersionsForApplication(String applicationName) {
        return versions.values().stream()
                .filter(v -> v.applicationName.equals(applicationName))
                .sorted((a, b) -> Long.compare(b.releasedAt, a.releasedAt))
                .toList();
    }
    
    /**
     * Get latest version
     */
    public ApplicationVersion getLatestVersion(String applicationName) {
        List<ApplicationVersion> appVersions = listVersionsForApplication(applicationName);
        return appVersions.isEmpty() ? null : appVersions.get(0);
    }
    
    /**
     * Record deployment event
     */
    private void recordEvent(String deploymentId, String eventType, String message) {
        events.add(new DeploymentEvent(
                UUID.randomUUID().toString(),
                deploymentId,
                eventType,
                message,
                System.currentTimeMillis()
        ));
    }
    
    /**
     * Get deployment events
     */
    public List<DeploymentEvent> getDeploymentEvents(String deploymentId) {
        return events.stream()
                .filter(e -> e.deploymentId.equals(deploymentId))
                .toList();
    }
    
    /**
     * Get deployment statistics
     */
    public Map<String, Object> getDeploymentStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalDeployments = deployments.size();
        long successfulDeployments = deployments.values().stream()
                .filter(d -> d.status == DeploymentStatus.SUCCESS)
                .count();
        long failedDeployments = deployments.values().stream()
                .filter(d -> d.status == DeploymentStatus.FAILED)
                .count();
        
        double successRate = totalDeployments > 0 ? (double) successfulDeployments / totalDeployments * 100 : 0;
        
        stats.put("totalDeployments", totalDeployments);
        stats.put("successfulDeployments", successfulDeployments);
        stats.put("failedDeployments", failedDeployments);
        stats.put("successRate", String.format("%.2f%%", successRate));
        stats.put("totalVersions", versions.size());
        stats.put("generatedAt", System.currentTimeMillis());
        
        return stats;
    }
    
    /**
     * Deployment Record
     */
    public static class DeploymentRecord {
        public String deploymentId;
        public String applicationName;
        public String version;
        public String environment;
        public DeploymentStatus status = DeploymentStatus.PENDING;
        public long createdAt;
        public long startedAt = 0;
        public long completedAt = 0;
        public String failureReason = null;
        public String rolledBackTo = null;
        
        public DeploymentRecord(String deploymentId, String applicationName, String version, 
                              String environment, long createdAt) {
            this.deploymentId = deploymentId;
            this.applicationName = applicationName;
            this.version = version;
            this.environment = environment;
            this.createdAt = createdAt;
        }
        
        public long getDuration() {
            if (completedAt == 0) return 0;
            return completedAt - startedAt;
        }
    }
    
    /**
     * Application Version
     */
    public static class ApplicationVersion {
        public String versionId;
        public String applicationName;
        public String version;
        public String artifactUrl;
        public String releaseNotes;
        public long releasedAt;
        public int downloadCount = 0;
        
        public ApplicationVersion(String versionId, String applicationName, String version,
                                String artifactUrl, String releaseNotes, long releasedAt) {
            this.versionId = versionId;
            this.applicationName = applicationName;
            this.version = version;
            this.artifactUrl = artifactUrl;
            this.releaseNotes = releaseNotes;
            this.releasedAt = releasedAt;
        }
    }
    
    /**
     * Deployment Event
     */
    public static class DeploymentEvent {
        public String eventId;
        public String deploymentId;
        public String eventType;
        public String message;
        public long timestamp;
        
        public DeploymentEvent(String eventId, String deploymentId, String eventType, String message, long timestamp) {
            this.eventId = eventId;
            this.deploymentId = deploymentId;
            this.eventType = eventType;
            this.message = message;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * Deployment Status
     */
    public enum DeploymentStatus {
        PENDING,
        IN_PROGRESS,
        SUCCESS,
        FAILED,
        ROLLED_BACK
    }
}
