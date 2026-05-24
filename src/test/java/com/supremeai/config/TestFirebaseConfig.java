package com.supremeai.config.test;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

@TestConfiguration
public class TestFirebaseConfig {

  /**
   * Provides a mock GoogleCredentials bean for test environments.
   * This prevents tests from attempting to load actual service account keys
   * or application default credentials, which might not be available or desired
   * in tests.
   */
  @Bean
  @Primary // Ensures this mock bean is preferred over the real one during tests
  public GoogleCredentials mockGoogleCredentials() {
   return new GoogleCredentials() {
    @Override
    public void refresh() throws IOException {
     /* Do nothing for mock credentials */
    }
   };
  }

  /**
   * Dynamically registers properties for Firebase emulator hosts during tests.
   * This ensures tests connect to local emulators (e.g., Firestore on 8080)
   * and uses a mock project ID, adhering to the Zero Hardcoding Policy (Rule 13).
   */
  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
   registry.add("spring.cloud.gcp.firestore.emulator.host", () -> "localhost:8080");
   registry.add("firebase.emulator.firestore.host", () -> "localhost:8080");
   registry.add("spring.cloud.gcp.project-id", () -> "supremeai-test-project");
   registry.add("firebase.project-id", () -> "supremeai-test-project");
  }
}