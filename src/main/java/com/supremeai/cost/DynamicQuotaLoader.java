package com.supremeai.cost;

import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Service
public class DynamicQuotaLoader {

    private final QuotaManager quotaManager;

    public DynamicQuotaLoader(QuotaManager quotaManager) {
        this.quotaManager = quotaManager;
    }

    public void loadQuotasFromFile(String filePath) {
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(filePath)) {
            properties.load(in);

            for (String key : properties.stringPropertyNames()) {
                // Expected format: ServiceName.FeatureName.Limit=Value OR ServiceName.FeatureName.Period=Value
                // Example: OpenAI.GPT4.Limit=10000 
                //          OpenAI.GPT4.Period=DAILY
                
                if (key.endsWith(".Limit")) {
                    String baseKey = key.substring(0, key.length() - 6); // Remove ".Limit"
                    String[] parts = baseKey.split("\\.");
                    if (parts.length == 2) {
                        String serviceName = parts[0];
                        String featureName = parts[1];
                        double limit = Double.parseDouble(properties.getProperty(key));
                        
                        // Look for period, default to MONTHLY if not found
                        String periodStr = properties.getProperty(baseKey + ".Period", "MONTHLY");
                        QuotaPeriod period;
                        try {
                            period = QuotaPeriod.valueOf(periodStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            period = QuotaPeriod.MONTHLY; // Fallback
                        }

                        quotaManager.registerQuota(new QuotaDefinition(serviceName, featureName, limit, period));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load quotas from " + filePath + ": " + e.getMessage());
        }
    }
}