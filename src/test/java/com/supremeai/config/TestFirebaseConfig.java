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

  static {
      System.setProperty("FIREBASE_AUTH_EMULATOR_HOST", "localhost:9099");
      System.setProperty("FIRESTORE_EMULATOR_HOST", "localhost:8081");
  }

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

  @Bean
  @Primary
  public com.google.api.gax.core.CredentialsProvider mockCredentialsProvider() {
      return () -> com.google.cloud.NoCredentials.getInstance();
  }

  /**
   * Dynamically registers properties for Firebase emulator hosts during tests.
   * This ensures tests connect to local emulators (e.g., Firestore on 8081)
   * and uses the correct project ID supremeai-a.
   */
  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.cloud.gcp.firestore.emulator.host", () -> "localhost:8081");
    registry.add("spring.cloud.gcp.firestore.host", () -> "localhost:8081");
    registry.add("firebase.emulator.firestore.host", () -> "localhost:8081");
    registry.add("spring.cloud.gcp.project-id", () -> "supremeai-a");
    registry.add("firebase.project-id", () -> "supremeai-a");
    registry.add("firebase.project.id", () -> "supremeai-a");
  }
}