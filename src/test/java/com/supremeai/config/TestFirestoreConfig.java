package com.supremeai.config;

import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

/**
 * Test-only Firestore/v3 configuration.
 *
 * <p>When the Firestore emulator is running ({@code FIRESTORE_EMULATOR_HOST} set),
 * FirestoreOptions points to the emulator.  When the env var is absent the
 * bean is created with production defaults so tests that do not touch Firestore
 * are unaffected.</p>
 */
@Configuration
@TestPropertySource(properties = {
        "spring.cloud.gcp.firestore.emulator.enabled=true",
        "spring.cloud.gcp.firestore.emulator.host=${FIRESTORE_EMULATOR_HOST:localhost:8080}",
        "spring.cloud.gcp.firestore.project-id=supremeai-test"
})
public class TestFirestoreConfig {

    @Bean
    @Primary
    public Firestore firestore() {
        return FirestoreOptions.getDefaultInstance().getService();
    }
}