package com.supremeai.security;

import com.supremeai.model.SystemConfig;
import com.supremeai.repository.SystemConfigRepository;
import com.supremeai.service.FirebaseRealtimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service to retrieve secrets from Firebase Firestore.
 * It looks into the 'system_configs' collection, 'global_settings' document.
 */
@Service
public class FirebaseSecretsService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseSecretsService.class);

    @Autowired
    private FirebaseRealtimeService realtimeService;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    /**
     * Retrieve a secret from Firebase.
     * Checks Firestore first, then falls back to Realtime Database.
     * 
     * @param secretKey The key for the secret
     * @return The secret value as a Mono<String>
     */
    public Mono<String> getSecret(String secretKey) {
        String[] parts = secretKey.split("\\.");
        if (parts.length != 2) {
            log.debug("Invalid secret key format for Firebase: {}. Expected provider.key", secretKey);
            return Mono.empty();
        }

        String providerName = parts[0];
        String key = parts[1];

        // Try Firestore first
        return systemConfigRepository.findById("global_settings")
                .map(config -> config.getProviderSecret(providerName, key))
                .filter(value -> value != null && !value.isEmpty())
                // Fallback to Realtime Database
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Secret not found in Firestore, checking RTDB for: {}", secretKey);
                    return realtimeService.getData("config/api_keys")
                            .map(rtdbKeys -> {
                                // RTDB structure: config/api_keys/PROVIDER (all caps usually)
                                Object val = rtdbKeys.get(providerName.toUpperCase());
                                if (val == null) val = rtdbKeys.get(providerName);
                                return val != null ? String.valueOf(val) : null;
                            });
                }))
                .filter(value -> value != null && !value.isEmpty());
    }

    /**
     * Health check for Firebase connection.
     */
    public Mono<Boolean> healthCheck() {
        return systemConfigRepository.findById("global_settings")
                .map(config -> true)
                .defaultIfEmpty(false)
                .onErrorResume(e -> {
                    log.error("Firebase health check failed: {}", e.getMessage());
                    return Mono.just(false);
                });
    }
}
