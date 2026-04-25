package com.supremeai.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

/**
 * AWS Secrets Manager integration service for secure secrets management.
 * 
 * Configuration:
 * - aws.secrets.enabled: Enable/disable AWS Secrets Manager (default: false)
 * - aws.region: AWS region (e.g., us-east-1)
 * - aws.secrets.prefix: Prefix for secret names (default: supremeai/)
 * 
 * Environment Variables:
 * - AWS_SECRETS_ENABLED: Set to "true" to enable AWS Secrets Manager
 * - AWS_REGION: AWS region
 * - AWS_SECRET_PREFIX: Prefix for secret names
 * - AWS_ACCESS_KEY_ID: AWS access key (optional if using IAM roles)
 * - AWS_SECRET_ACCESS_KEY: AWS secret key (optional if using IAM roles)
 */
@Service
@ConditionalOnProperty(name = "aws.secrets.enabled", havingValue = "true")
public class AwsSecretsService {

    private static final Logger log = LoggerFactory.getLogger(AwsSecretsService.class);

    @Value("${aws.region:${AWS_REGION:us-east-1}}")
    private String region;

    @Value("${aws.secrets.prefix:${AWS_SECRET_PREFIX:supremeai/}}")
    private String secretPrefix;

    private final SecretsManagerClient secretsClient;

    public AwsSecretsService() {
        this.secretsClient = SecretsManagerClient.builder()
            .region(Region.of(region))
            .build();
    }

    /**
     * Retrieve a secret value from AWS Secrets Manager.
     * 
     * @param secretKey The key of the secret to retrieve (without prefix)
     * @return The secret value as a Mono<String>
     */
    public Mono<String> getSecret(String secretKey) {
        return Mono.fromCallable(() -> {
            String secretName = secretPrefix + secretKey;

            GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

            try {
                GetSecretValueResponse response = secretsClient.getSecretValue(request);

                // Secrets Manager can store secrets as either a string or binary
                if (response.secretString() != null) {
                    return response.secretString();
                } else if (response.secretBinary() != null) {
                    return new String(response.secretBinary().asByteArray());
                } else {
                    log.warn("Secret found but empty: {}", secretName);
                    return null;
                }
            } catch (SecretsManagerException e) {
                if (e.statusCode() == 404) {
                    log.warn("Secret not found in AWS Secrets Manager: {}", secretName);
                    return null;
                }
                log.error("Failed to retrieve secret from AWS Secrets Manager: {}", e.getMessage());
                throw new RuntimeException("Failed to retrieve secret from AWS Secrets Manager", e);
            }
        })
        .onErrorResume(e -> {
            if (e instanceof RuntimeException && e.getCause() instanceof SecretsManagerException) {
                log.error("Error retrieving secret from AWS Secrets Manager: {}", e.getMessage());
            } else {
                log.error("Unexpected error retrieving secret: {}", e.getMessage());
            }
            return Mono.empty();
        });
    }

    /**
     * Check if AWS Secrets Manager is accessible.
     * 
     * @return True if accessible, false otherwise
     */
    public Mono<Boolean> healthCheck() {
        return Mono.fromCallable(() -> {
            try {
                // Try to list secrets as a health check
                secretsClient.listSecrets();
                return true;
            } catch (Exception e) {
                log.error("AWS Secrets Manager health check failed: {}", e.getMessage());
                return false;
            }
        })
        .onErrorResume(e -> {
            log.error("AWS Secrets Manager health check error: {}", e.getMessage());
            return Mono.just(false);
        });
    }
}
