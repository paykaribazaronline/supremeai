package com.supremeai.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HashiCorp Vault integration service for secure secrets management.
 * 
 * Configuration:
 * - vault.enabled: Enable/disable Vault integration (default: false)
 * - vault.address: Vault server address (e.g., https://vault.example.com)
 * - vault.token: Vault authentication token
 * - vault.secret.path: Base path for secrets (default: secret/data/supremeai)
 * 
 * Environment Variables:
 * - VAULT_ENABLED: Set to "true" to enable Vault
 * - VAULT_ADDR: Vault server address
 * - VAULT_TOKEN: Vault authentication token
 * - VAULT_SECRET_PATH: Base path for secrets
 */
@Service
@ConditionalOnProperty(name = "vault.enabled", havingValue = "true")
public class VaultSecretsService {

    private static final Logger log = LoggerFactory.getLogger(VaultSecretsService.class);

    @Value("${vault.address:${VAULT_ADDR:}}")
    private String vaultAddress;

    @Value("${vault.token:${VAULT_TOKEN:}}")
    private String vaultToken;

    @Value("${vault.secret.path:${VAULT_SECRET_PATH:secret/data/supremeai}}")
    private String secretPath;

    private final HttpClient httpClient;

    public VaultSecretsService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    /**
     * Retrieve a secret value from Vault.
     * 
     * @param secretKey The key of the secret to retrieve
     * @return The secret value as a Mono<String>
     */
    public Mono<String> getSecret(String secretKey) {
        return Mono.fromCallable(() -> {
            String path = String.format("%s/%s", secretPath, secretKey);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/v1/%s", vaultAddress, path)))
                .header("X-Vault-Token", vaultToken)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {
                // Parse the response to extract the secret value
                // Response format: {"data":{"data":{"value":"..."}}}
                // For simplicity, we'll do a basic JSON parse
                String body = response.body();
                int valueIndex = body.indexOf("\"value\":");
                if (valueIndex > 0) {
                    int startQuote = body.indexOf("\"", valueIndex + 8);
                    int endQuote = body.indexOf("\"", startQuote + 1);
                    if (startQuote > 0 && endQuote > startQuote) {
                        return body.substring(startQuote + 1, endQuote);
                    }
                }
                log.warn("Could not parse secret value from Vault response for key: {}", secretKey);
                return null;
            } else if (response.statusCode() == 404) {
                log.warn("Secret not found in Vault: {}", secretKey);
                return null;
            } else {
                log.error("Failed to retrieve secret from Vault. Status: {}, Body: {}", 
                    response.statusCode(), response.body());
                throw new RuntimeException("Failed to retrieve secret from Vault");
            }
        })
        .onErrorResume(e -> {
            log.error("Error retrieving secret from Vault: {}", e.getMessage());
            return Mono.empty();
        });
    }

    /**
     * Check if Vault is accessible and properly configured.
     * 
     * @return True if Vault is accessible, false otherwise
     */
    public Mono<Boolean> healthCheck() {
        return Mono.fromCallable(() -> {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/v1/sys/health", vaultAddress)))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            return response.statusCode() == 200;
        })
        .onErrorResume(e -> {
            log.error("Vault health check failed: {}", e.getMessage());
            return Mono.just(false);
        });
    }
}
