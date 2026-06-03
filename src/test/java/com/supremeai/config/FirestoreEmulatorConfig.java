package com.supremeai.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test configuration that initialises a Firestore instance connected to the
 * local emulator whenever {@code FIRESTORE_EMULATOR_HOST} is set in the environment.
 *
 * <p>When the env var is absent or the emulator is unreachable, the bean resolves
 * to {@code null} — Spring Boot skips null beans and all other non-Firestore tests
 * continue normally.  Only tests that explicitly start the emulator will create
 * a live Firestore connection.</p>
 *
 * <p>Add {@code @Import(FirestoreEmulatorConfig.class)} to any integration test,
 * or load it via an {@code application-test.properties} profile.</p>
 */
@TestConfiguration
public class FirestoreEmulatorConfig {

    private static final Logger log = LoggerFactory.getLogger(FirestoreEmulatorConfig.class);

    @Bean
    public Firestore firestore() {
        String emulatorHost = System.getenv().getOrDefault("FIRESTORE_EMULATOR_HOST", "");
        if (emulatorHost.isBlank()) {
            log.info("[TestConfig] FIRESTORE_EMULATOR_HOST not set — returning null Firestore bean");
            return null;
        }
        log.info("[TestConfig] Creating Firestore bean pointing to emulator at {}", emulatorHost);
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            FirestoreOptions options = FirestoreOptions.getDefaultInstance().toBuilder()
                    .setCredentials(credentials)
                    .setProjectId("supremeai-test")
                    .setEmulatorHost(emulatorHost)
                    .build();
            return options.getService();
        } catch (Exception e) {
            log.warn("[TestConfig] Could not reach emulator at {} — returning null Firestore bean."
                    + " Tests requiring live Firestore must start the emulator first. Non-fatal.", emulatorHost, e);
            return null; // Non-fatal: Spring skips null beans
        }
    }
}
