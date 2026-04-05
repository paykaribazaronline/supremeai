package org.example.security;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.google.cloud.secretmanager.v1.AccessSecretVersionRequest;
import com.google.cloud.secretmanager.v1.AddSecretVersionRequest;
import com.google.cloud.secretmanager.v1.ProjectName;
import com.google.cloud.secretmanager.v1.SecretName;
import com.google.cloud.secretmanager.v1.SecretPayload;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;

import javax.annotation.PostConstruct;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Secret Manager Service
 * 
 * Manages sensitive configuration like API keys, passwords, and tokens.
 * In production, this integrates with cloud Secret Manager (AWS Secrets Manager,
 * Google Cloud Secret Manager, Azure Key Vault, HashiCorp Vault).
 * 
 * For development, falls back to environment variables.
 */
@Service
public class SecretManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SecretManager.class);
    
    @Value("${secret.manager.backend:env}")
    private String backend;

    @Value("${secret.manager.gcp.project-id:}")
    private String gcpProjectId;

    @Value("${secret.manager.aws.region:us-east-1}")
    private String awsRegion;

    @Value("${secret.manager.azure.vault-url:}")
    private String azureVaultUrl;
    
    // In-memory cache for secrets
    private final Map<String, String> secretCache = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initialize() {
        logger.info("🔐 Secret Manager initialized with backend: {}", backend);
    }
    
    /**
     * Get a secret by name
     */
    public String getSecret(String name) {
        if (name == null || name.isBlank()) {
            logger.warn("Cannot fetch secret with empty name");
            return null;
        }

        // Check cache first
        if (secretCache.containsKey(name)) {
            return secretCache.get(name);
        }
        
        // Fetch from backend
        String value = fetchSecret(name);
        
        if (value != null) {
            // Cache for future use
            secretCache.put(name, value);
        }
        
        return value;
    }
    
    /**
     * Update a secret
     */
    public void updateSecret(String name, String value) {
        if (name == null || name.isBlank()) {
            logger.warn("Cannot update secret with empty name");
            return;
        }

        logger.info("🔐 Updating secret: {}", name);
        
        // Update backend
        updateBackendSecret(name, value);
        
        // Update cache
        secretCache.put(name, value);
    }
    
    /**
     * Remove a secret from cache (force refresh)
     */
    public void invalidateCache(String name) {
        if (name == null || name.isBlank()) {
            return;
        }

        secretCache.remove(name);
    }
    
    /**
     * Fetch secret from configured backend
     */
    private String fetchSecret(String name) {
        String selectedBackend = normalizeBackend(backend);
        switch (selectedBackend) {
            case "gcp":
                return fetchFromGCPSecretManager(name);
            case "aws":
                return fetchFromAWSSecretsManager(name);
            case "azure":
                return fetchFromAzureKeyVault(name);
            case "vault":
                return fetchFromHashiCorpVault(name);
            case "env":
            default:
                return fetchFromEnvironment(name);
        }
    }
    
    /**
     * Update secret in configured backend
     */
    private void updateBackendSecret(String name, String value) {
        String selectedBackend = normalizeBackend(backend);
        switch (selectedBackend) {
            case "gcp":
                updateGCPSecret(name, value);
                break;
            case "aws":
                updateAWSSecret(name, value);
                break;
            case "azure":
                updateAzureSecret(name, value);
                break;
            case "vault":
                updateVaultSecret(name, value);
                break;
            case "env":
            default:
                // Cannot update environment variables at runtime
                logger.warn("Cannot update environment variable: {}", name);
                break;
        }
    }

    private String normalizeBackend(String backendValue) {
        if (backendValue == null) {
            return "env";
        }

        String normalized = backendValue
            .trim()
            .toLowerCase(Locale.ROOT)
            .replace("-", "")
            .replace("_", "")
            .replace(" ", "");

        switch (normalized) {
            case "gcp":
            case "gcpsecretmanager":
            case "googlecloudsecretmanager":
                return "gcp";
            case "aws":
            case "awssecretsmanager":
            case "secretsmanager":
                return "aws";
            case "azure":
            case "azurekeyvault":
            case "keyvault":
                return "azure";
            case "vault":
            case "hashicorpvault":
                return "vault";
            case "env":
            case "environment":
            default:
                return "env";
        }
    }
    
    // Backend-specific implementations
    
    private String fetchFromGCPSecretManager(String name) {
        if (gcpProjectId == null || gcpProjectId.isBlank()) {
            logger.warn("GCP backend selected but secret.manager.gcp.project-id is missing; falling back to env for {}", name);
            return fetchFromEnvironment(name);
        }

        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            SecretVersionName secretVersionName = SecretVersionName.of(gcpProjectId, name, "latest");
            AccessSecretVersionRequest request = AccessSecretVersionRequest.newBuilder()
                .setName(secretVersionName.toString())
                .build();
            return client.accessSecretVersion(request).getPayload().getData().toStringUtf8();
        } catch (Exception e) {
            logger.error("Failed to fetch secret {} from GCP: {}", name, e.getMessage());
            return fetchFromEnvironment(name);
        }
    }
    
    private String fetchFromAWSSecretsManager(String name) {
        try (SecretsManagerClient client = SecretsManagerClient.builder()
            .region(Region.of(awsRegion))
            .build()) {
            return client.getSecretValue(GetSecretValueRequest.builder().secretId(name).build()).secretString();
        } catch (Exception e) {
            logger.error("Failed to fetch secret {} from AWS: {}", name, e.getMessage());
            return fetchFromEnvironment(name);
        }
    }
    
    private String fetchFromAzureKeyVault(String name) {
        SecretClient client = buildAzureSecretClient();
        if (client == null) {
            return fetchFromEnvironment(name);
        }

        try {
            return client.getSecret(name).getValue();
        } catch (Exception e) {
            logger.error("Failed to fetch secret {} from Azure Key Vault: {}", name, e.getMessage());
            return fetchFromEnvironment(name);
        }
    }
    
    private String fetchFromHashiCorpVault(String name) {
        // In production, use Vault client
        return System.getenv(name);
    }
    
    private String fetchFromEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null) {
            // Try with different naming conventions
            value = System.getenv(name.replace("_", "").toLowerCase());
        }
        return value;
    }
    
    private void updateGCPSecret(String name, String value) {
        if (gcpProjectId == null || gcpProjectId.isBlank()) {
            logger.warn("Cannot update GCP secret {} because secret.manager.gcp.project-id is missing", name);
            return;
        }

        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            SecretName secretName = SecretName.of(gcpProjectId, name);
            client.addSecretVersion(
                AddSecretVersionRequest.newBuilder()
                    .setParent(secretName.toString())
                    .setPayload(SecretPayload.newBuilder().setData(ByteString.copyFromUtf8(value)).build())
                    .build()
            );
        } catch (Exception e) {
            logger.error("Failed to update GCP secret {}: {}", name, e.getMessage());
        }
    }
    
    private void updateAWSSecret(String name, String value) {
        try (SecretsManagerClient client = SecretsManagerClient.builder()
            .region(Region.of(awsRegion))
            .build()) {
            client.putSecretValue(
                PutSecretValueRequest.builder()
                    .secretId(name)
                    .secretString(value)
                    .build()
            );
        } catch (Exception e) {
            logger.error("Failed to update AWS secret {}: {}", name, e.getMessage());
        }
    }
    
    private void updateAzureSecret(String name, String value) {
        SecretClient client = buildAzureSecretClient();
        if (client == null) {
            logger.warn("Cannot update Azure secret {} because secret.manager.azure.vault-url is missing", name);
            return;
        }

        try {
            client.setSecret(name, value);
        } catch (Exception e) {
            logger.error("Failed to update Azure secret {}: {}", name, e.getMessage());
        }
    }
    
    private void updateVaultSecret(String name, String value) {
        logger.info("Would update HashiCorp Vault: {}", name);
    }

    private SecretClient buildAzureSecretClient() {
        if (azureVaultUrl == null || azureVaultUrl.isBlank()) {
            logger.warn("Azure backend selected but secret.manager.azure.vault-url is missing");
            return null;
        }

        try {
            return new SecretClientBuilder()
                .vaultUrl(azureVaultUrl)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
        } catch (Exception e) {
            logger.error("Failed to initialize Azure Key Vault client: {}", e.getMessage());
            return null;
        }
    }
}
