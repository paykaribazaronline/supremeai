package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collectionName = "infrastructure_advices")
public class InfrastructureAdvice {
    @DocumentId
    private String id;
    private String appId;
    private String appName;
    
    private String recommendedProvider; // e.g., GCP, Firebase, Vercel
    private String recommendedTier; // e.g., Free, Standard, Enterprise
    private Double estimatedMonthlyCost;
    
    private List<HostingComponent> components;
    private List<String> securityBestPractices;
    private List<String> scalabilityTips;
    
    private String summary;
    private Long createdAt;

    // Constructors
    public InfrastructureAdvice() {
    }

    public InfrastructureAdvice(String id, String appId, String appName, String recommendedProvider, 
                                String recommendedTier, Double estimatedMonthlyCost, List<HostingComponent> components, 
                                List<String> securityBestPractices, List<String> scalabilityTips, String summary, 
                                Long createdAt) {
        this.id = id;
        this.appId = appId;
        this.appName = appName;
        this.recommendedProvider = recommendedProvider;
        this.recommendedTier = recommendedTier;
        this.estimatedMonthlyCost = estimatedMonthlyCost;
        this.components = components;
        this.securityBestPractices = securityBestPractices;
        this.scalabilityTips = scalabilityTips;
        this.summary = summary;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public String getRecommendedProvider() { return recommendedProvider; }
    public void setRecommendedProvider(String recommendedProvider) { this.recommendedProvider = recommendedProvider; }

    public String getRecommendedTier() { return recommendedTier; }
    public void setRecommendedTier(String recommendedTier) { this.recommendedTier = recommendedTier; }

    public Double getEstimatedMonthlyCost() { return estimatedMonthlyCost; }
    public void setEstimatedMonthlyCost(Double estimatedMonthlyCost) { this.estimatedMonthlyCost = estimatedMonthlyCost; }

    public List<HostingComponent> getComponents() { return components; }
    public void setComponents(List<HostingComponent> components) { this.components = components; }

    public List<String> getSecurityBestPractices() { return securityBestPractices; }
    public void setSecurityBestPractices(List<String> securityBestPractices) { this.securityBestPractices = securityBestPractices; }

    public List<String> getScalabilityTips() { return scalabilityTips; }
    public void setScalabilityTips(List<String> scalabilityTips) { this.scalabilityTips = scalabilityTips; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    // Builder
    public static InfrastructureAdviceBuilder builder() {
        return new InfrastructureAdviceBuilder();
    }

    public static class InfrastructureAdviceBuilder {
        private String id;
        private String appId;
        private String appName;
        private String recommendedProvider;
        private String recommendedTier;
        private Double estimatedMonthlyCost;
        private List<HostingComponent> components;
        private List<String> securityBestPractices;
        private List<String> scalabilityTips;
        private String summary;
        private Long createdAt;

        public InfrastructureAdviceBuilder id(String id) { this.id = id; return this; }
        public InfrastructureAdviceBuilder appId(String appId) { this.appId = appId; return this; }
        public InfrastructureAdviceBuilder appName(String appName) { this.appName = appName; return this; }
        public InfrastructureAdviceBuilder recommendedProvider(String recommendedProvider) { this.recommendedProvider = recommendedProvider; return this; }
        public InfrastructureAdviceBuilder recommendedTier(String recommendedTier) { this.recommendedTier = recommendedTier; return this; }
        public InfrastructureAdviceBuilder estimatedMonthlyCost(Double estimatedMonthlyCost) { this.estimatedMonthlyCost = estimatedMonthlyCost; return this; }
        public InfrastructureAdviceBuilder components(List<HostingComponent> components) { this.components = components; return this; }
        public InfrastructureAdviceBuilder securityBestPractices(List<String> securityBestPractices) { this.securityBestPractices = securityBestPractices; return this; }
        public InfrastructureAdviceBuilder scalabilityTips(List<String> scalabilityTips) { this.scalabilityTips = scalabilityTips; return this; }
        public InfrastructureAdviceBuilder summary(String summary) { this.summary = summary; return this; }
        public InfrastructureAdviceBuilder createdAt(Long createdAt) { this.createdAt = createdAt; return this; }

        public InfrastructureAdvice build() {
            InfrastructureAdvice advice = new InfrastructureAdvice();
            advice.setId(id);
            advice.setAppId(appId);
            advice.setAppName(appName);
            advice.setRecommendedProvider(recommendedProvider);
            advice.setRecommendedTier(recommendedTier);
            advice.setEstimatedMonthlyCost(estimatedMonthlyCost);
            advice.setComponents(components);
            advice.setSecurityBestPractices(securityBestPractices);
            advice.setScalabilityTips(scalabilityTips);
            advice.setSummary(summary);
            advice.setCreatedAt(createdAt);
            return advice;
        }
    }

    public static class HostingComponent {
        private String name; // e.g., Frontend, Backend, DB
        private String service; // e.g., Cloud Run, Firestore
        private String reason;

        public HostingComponent() {
        }

        public HostingComponent(String name, String service, String reason) {
            this.name = name;
            this.service = service;
            this.reason = reason;
        }

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getService() { return service; }
        public void setService(String service) { this.service = service; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        // Builder for components
        public static HostingComponentBuilder builder() {
            return new HostingComponentBuilder();
        }

        public static class HostingComponentBuilder {
            private String name;
            private String service;
            private String reason;

            public HostingComponentBuilder name(String name) { this.name = name; return this; }
            public HostingComponentBuilder service(String service) { this.service = service; return this; }
            public HostingComponentBuilder reason(String reason) { this.reason = reason; return this; }

            public HostingComponent build() {
                return new HostingComponent(name, service, reason);
            }
        }
    }
}
