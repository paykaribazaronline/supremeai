package com.supremeai.cost;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

@Service
public class QuotaManager {

    // Map format: "Service_Feature" -> QuotaDefinition
    private final Map<String, QuotaDefinition> quotas = new ConcurrentHashMap<>();

    public QuotaManager() {
        initializeStandardQuotas();
    }

    private void initializeStandardQuotas() {
        // Groq API Limits
        registerQuota(new QuotaDefinition("Groq", "Requests", 14400, QuotaPeriod.DAILY)); // 14.4k req/day
        registerQuota(new QuotaDefinition("Groq", "Tokens", 100000, QuotaPeriod.DAILY));
        
        // Firebase Spark Plan Limits (Free)
        registerQuota(new QuotaDefinition("Firebase", "Reads", 50000, QuotaPeriod.DAILY));
        registerQuota(new QuotaDefinition("Firebase", "Writes", 20000, QuotaPeriod.DAILY));
        registerQuota(new QuotaDefinition("Firebase", "Deletes", 20000, QuotaPeriod.DAILY));
        registerQuota(new QuotaDefinition("Firebase", "Storage", 5.0, QuotaPeriod.MONTHLY)); // 5 GB
        
        // Google Cloud Platform Free Tier Limits
        registerQuota(new QuotaDefinition("GCP", "CloudRun_Compute", 180000, QuotaPeriod.MONTHLY)); // vCPU-seconds
        registerQuota(new QuotaDefinition("GCP", "CloudFunctions_Invocations", 2000000, QuotaPeriod.MONTHLY));
    }

    public void registerQuota(QuotaDefinition quota) {
        String key = generateKey(quota.getServiceName(), quota.getFeatureName());
        quotas.put(key, quota);
    }

    public boolean recordUsage(String serviceName, String featureName, double usageAmount) {
        String key = generateKey(serviceName, featureName);
        QuotaDefinition quota = quotas.get(key);
        
        if (quota == null) {
            // Unregistered service/feature, maybe log a warning
            return false;
        }

        if (quota.isLimitReached()) {
            return false; // Cannot proceed, limit reached
        }

        quota.addUsage(usageAmount);
        return true;
    }
    
    public QuotaDefinition getQuotaStatus(String serviceName, String featureName) {
         return quotas.get(generateKey(serviceName, featureName));
    }

    public List<QuotaDefinition> getAllQuotas() {
        return new ArrayList<>(quotas.values());
    }

    private String generateKey(String serviceName, String featureName) {
        return serviceName + "_" + featureName;
    }
}