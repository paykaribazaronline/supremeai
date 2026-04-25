package com.supremeai.model;

import java.util.Map;

public class AIBehaviorProfile {
    private String id;
    private String projectId;
    private String frameworkVersion;
    private SecurityStrictness securityStrictness;
    private PerformanceTradeoff performanceTradeoff;
    private Map<String, Object> additionalPreferences;

    public AIBehaviorProfile() {}

    public AIBehaviorProfile(String id, String projectId, String frameworkVersion,
                             SecurityStrictness securityStrictness, PerformanceTradeoff performanceTradeoff,
                             Map<String, Object> additionalPreferences) {
        this.id = id;
        this.projectId = projectId;
        this.frameworkVersion = frameworkVersion;
        this.securityStrictness = securityStrictness;
        this.performanceTradeoff = performanceTradeoff;
        this.additionalPreferences = additionalPreferences;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getFrameworkVersion() { return frameworkVersion; }
    public void setFrameworkVersion(String frameworkVersion) { this.frameworkVersion = frameworkVersion; }
    public SecurityStrictness getSecurityStrictness() { return securityStrictness; }
    public void setSecurityStrictness(SecurityStrictness securityStrictness) { this.securityStrictness = securityStrictness; }
    public PerformanceTradeoff getPerformanceTradeoff() { return performanceTradeoff; }
    public void setPerformanceTradeoff(PerformanceTradeoff performanceTradeoff) { this.performanceTradeoff = performanceTradeoff; }
    public Map<String, Object> getAdditionalPreferences() { return additionalPreferences; }
    public void setAdditionalPreferences(Map<String, Object> additionalPreferences) { this.additionalPreferences = additionalPreferences; }

    public enum SecurityStrictness {
        LOW, MEDIUM, HIGH
    }

    public enum PerformanceTradeoff {
        SPEED_OPTIMIZED, BALANCED, QUALITY_OPTIMIZED
    }
}
