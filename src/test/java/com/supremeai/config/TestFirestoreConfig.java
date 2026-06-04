package com.supremeai.config;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

/**
 * Test-only Firestore/v3 configuration.
 *
 * <p>When the Firestore emulator is running ({@code FIRESTORE_EMULATOR_HOST} set), FirestoreOptions
 * points to the emulator. When the env var is absent the bean is created with production defaults
 * so tests that do not touch Firestore are unaffected.
 */
@Configuration
@TestPropertySource(
    properties = {
      "spring.cloud.gcp.firestore.emulator.enabled=true",
      "spring.cloud.gcp.firestore.emulator.host=${FIRESTORE_EMULATOR_HOST:localhost:8081}",
      "spring.cloud.gcp.firestore.host=localhost:8081",
      "spring.cloud.gcp.firestore.project-id=supremeai-a",
      "spring.cloud.gcp.project-id=supremeai-a"
    })
public class TestFirestoreConfig {

  @Bean
  @Primary
  public Firestore firestore() {
    return FirestoreOptions.newBuilder()
        .setProjectId("supremeai-a")
        .setEmulatorHost("localhost:8081")
        .setCredentials(com.google.cloud.NoCredentials.getInstance())
        .build()
        .getService();
  }
}
