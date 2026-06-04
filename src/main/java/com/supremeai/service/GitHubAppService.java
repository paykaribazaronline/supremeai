package com.supremeai.service;

import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

/**
 * Service to handle GitHub App Authentication and Operations.
 * Uses the App ID and Private Key to generate JWTs and request Installation Tokens.
 */
@Service
public class GitHubAppService {

    private static final Logger log = LoggerFactory.getLogger(GitHubAppService.class);

    @Value("${github.app.id:}")
    private String appId;

    @Value("${github.app.private-key:}")
    private String privateKeyPem;

    private final WebClient webClient;

    public GitHubAppService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .build();
    }

    /**
     * Generates a JWT (JSON Web Token) to authenticate as the GitHub App.
     */
    public String generateAppJWT() throws Exception {
        if (appId == null || appId.isEmpty() || privateKeyPem == null || privateKeyPem.isEmpty()) {
            throw new IllegalStateException("GitHub App ID or Private Key is not configured.");
        }

        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + (10 * 60 * 1000); // 10 minutes max expiration

        // GitHub gives PKCS#1 RSA keys by default (BEGIN RSA PRIVATE KEY). 
        // Java natively prefers PKCS#8 (BEGIN PRIVATE KEY).
        // For this code, the PEM must be converted to PKCS#8 format.
        String privateKeyContent = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        return Jwts.builder()
                .issuer(appId)
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(expMillis))
                .signWith(privateKey)
                .compact();
    }

    /**
     * Exchanges the App JWT for a repository-specific Installation Access Token.
     */
    @SuppressWarnings("unchecked")
    public Mono<String> getInstallationToken(String targetInstallationId) {
        if (targetInstallationId == null || targetInstallationId.isEmpty()) {
            return Mono.error(new IllegalStateException("GitHub App Installation ID is required."));
        }

        return Mono.fromCallable(this::generateAppJWT)
                .flatMap(jwt -> webClient.post()
                        .uri("/app/installations/{installation_id}/access_tokens", targetInstallationId)
                        .header("Authorization", "Bearer " + jwt)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .map(response -> (String) response.get("token"))
                )
                .doOnError(e -> log.error("Failed to generate GitHub Installation Token", e));
    }

    /**
     * Retrieves the Git Clone URL with authentication token embedded.
     */
    public Mono<String> getAuthenticatedGitUrl(String repoOwner, String repoName, String targetInstallationId) {
        return getInstallationToken(targetInstallationId)
                .map(token -> String.format("https://x-access-token:%s@github.com/%s/%s.git", token, repoOwner, repoName));
    }
}
