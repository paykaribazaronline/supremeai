package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HostingComponent {
        private String name; // e.g., Frontend, Backend, DB
        private String service; // e.g., Cloud Run, Firestore
        private String reason;
    }
}
