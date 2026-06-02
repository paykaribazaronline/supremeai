package com.supremeai.security;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Optional;

/**
 * Service to manage secrets using Google Cloud Secret Manager.
 * Falls back to environment variables if Secret Manager is not available or if in local mode.
 */
@Service
public class SecretManagerService {
    public SecretManagerService(String projectId, boolean enabled, String activeProfile) {
        this.projectId = projectId;
        this.enabled = enabled;
        this.activeProfile = activeProfile;
    }


    private static final Logger log = LoggerFactory.getLogger(SecretManagerService.class);




    private SecretManagerServiceClient client;

    public boolean isProduction() {
        return "prod".equals(activeProfile) || activeProfile.contains("prod");
    }

    @PostConstruct
    public void init() {
        if (enabled) {
            try {
                this.client = SecretManagerServiceClient.create();
                log.info("Google Secret Manager Service initialized for project: {}", projectId);
            } catch (IOException e) {
                log.error("Failed to initialize Google Secret Manager client: {}", e.getMessage());
                this.enabled = false;
            }
        } else {
            log.info("Google Secret Manager is disabled. Using environment variables/local properties.");
        }
    }

    /**
     * Retrieves a secret value. Priority: Secret Manager -> Environment Variable -> Optional Default.
     */
    public String getSecret(String secretId) {
        if (enabled && client != null) {
            try {
                SecretVersionName secretVersionName = SecretVersionName.of(projectId, secretId, "latest");
                AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
                return response.getPayload().getData().toStringUtf8();
            } catch (Exception e) {
                log.warn("Could not retrieve secret {} from Secret Manager: {}.", secretId, e.getMessage());
            }
        }

        // Fallback to environment variable (standard Java way) - BLOCKED in production!
        if (isProduction()) {
            log.error("CRITICAL SECURITY AUDIT FAILURE: Env var fallback for secret '{}' is BLOCKED in production!", secretId);
            return null;
        }

        String envValue = System.getenv(secretId.toUpperCase().replace("-", "_"));
        if (envValue != null) {
            return envValue;
        }

        return null;
    }

    public Optional<String> getSecretOptional(String secretId) {
        return Optional.ofNullable(getSecret(secretId));
    }

    @PreDestroy
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
