package com.supremeai.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test configuration that initialises a Firestore instance connected to the
 * local emulator whenever {@code FIRESTORE_EMULATOR_HOST} is set in the environment.
 *
 * <p>Add {@code @Import(FirestoreEmulatorConfig.class)} to any integration test,
 * or load it via an {@code application-test.properties} profile.</p>
 */
@TestConfiguration
public class FirestoreEmulatorConfig {

    @Bean
    public Firestore firestore() {
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            FirestoreOptions options = FirestoreOptions.getDefaultInstance().toBuilder()
                    .setCredentials(credentials)
                    .setProjectId("supremeai-test")
                    .build();
            return options.getService();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not create Firestore bean for tests — is the emulator running on "
                            + System.getenv().getOrDefault("FIRESTORE_EMULATOR_HOST", "localhost:8080"), e);
        }
    }
}
