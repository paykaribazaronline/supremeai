package com.supremeai.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

/**
 * Firebase এবং Firestore কনফিগারেশন ক্লাস। Firebase App应立即初始化，避免在请求处理时出现 "FirebaseApp doesn't
 * exist" 错误。
 */
@Configuration
public class FirebaseConfig {

  private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

  @Value("${firebase.project.id:supremeai-a}")
  private String projectId;

  @Value("${spring.cloud.gcp.firestore.database-id:(default)}")
  private String databaseId;

  @Value(
      "${firebase.database.url:https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/}")
  private String databaseUrl;

  /** 초기화 FirebaseApp. @Lazy(false) 재성 스프링링 이 가능되어도 도 초기화. */
  @Bean
  @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
  @Lazy(false)
  public FirebaseApp firebaseApp(GoogleCredentials credentials) throws IOException {
    if (!FirebaseApp.getApps().isEmpty()) {
      return FirebaseApp.getInstance();
    }

    log.info("Initializing Firebase Application for project: {}", projectId);

    FirebaseOptions options =
        FirebaseOptions.builder()
            .setCredentials(credentials)
            .setProjectId(projectId)
            .setDatabaseUrl(databaseUrl)
            .setStorageBucket(projectId + ".appspot.com")
            .build();

    return FirebaseApp.initializeApp(options);
  }

  /** FirebaseAuth 초기화 ( firebaseApp */
  @Bean
  @Primary
  @Lazy(false)
  public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
    log.info("Initializing FirebaseAuth singleton");
    return FirebaseAuth.getInstance(firebaseApp);
  }

  @Bean
  @Primary
  @Lazy(false)
  public Firestore firestore(FirestoreOptions firestoreOptions) {
    return firestoreOptions.getService();
  }

  @Bean
  public FirestoreOptions firestoreOptions(GoogleCredentials credentials) {
    log.info("Creating FirestoreOptions for project: {} and database: {}", projectId, databaseId);
    return FirestoreOptions.newBuilder()
        .setProjectId(projectId)
        .setDatabaseId(databaseId)
        .setCredentials(credentials)
        .build();
  }

  @Bean
  public GcpProjectIdProvider gcpProjectIdProvider() {
    return () -> projectId;
  }

  @Bean
  @Primary
  public CredentialsProvider googleCredentialsProvider(GoogleCredentials credentials) {
    return FixedCredentialsProvider.create(credentials);
  }

  @Bean
  public GoogleCredentials googleCredentials() throws IOException {
    return loadCredentials();
  }

  private GoogleCredentials loadCredentials() throws IOException {
    // 1. Env var: FIREBASE_SERVICE_ACCOUNT_JSON (JSON payload directly)
    String secretJson = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON");
    if (secretJson != null && !secretJson.isBlank()) {
      if (secretJson.contains("YOUR_PRIVATE_KEY_HERE")) {
        log.warn(
            "Firebase: Detected placeholder in FIREBASE_SERVICE_ACCOUNT_JSON env var. Ignoring.");
      } else {
        log.info("Firebase: Loading credentials from FIREBASE_SERVICE_ACCOUNT_JSON env var");
        return GoogleCredentials.fromStream(
            new ByteArrayInputStream(secretJson.getBytes(StandardCharsets.UTF_8)));
      }
    }

    // 2. Try primary service account file
    byte[] credentialBytes = loadNonPlaceholderResource("firebase-service-account.json");
    if (credentialBytes == null) {
      // 3. Try alternative service account file name
      credentialBytes = loadNonPlaceholderResource("service-account.json");
    }

    if (credentialBytes != null) {
      log.info("Firebase: Loading credentials from classpath JSON file");
      return GoogleCredentials.fromStream(new ByteArrayInputStream(credentialBytes))
          .createScoped(
              Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
    }

    // 4. Fallback to Application Default Credentials
    try {
      log.info("Firebase: Falling back to Application Default Credentials (ADC)");
      return GoogleCredentials.getApplicationDefault()
          .createScoped(
              Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
    } catch (Exception e) {
      log.warn("========================================================================");
      log.warn(
          "WARNING: No valid Google Credentials found! Firestore will NOT be able to connect.");
      log.warn("Please configure FIREBASE_SERVICE_ACCOUNT_JSON env var or place a valid");
      log.warn("service-account.json file in the classpath/project root.");
      log.warn("Error message: {}", e.getMessage());
      log.warn("========================================================================");

      // Return anonymous credentials to allow app startup without real credentials
      return new GoogleCredentials() {
        @Override
        public com.google.auth.oauth2.AccessToken refreshAccessToken() throws IOException {
          return new com.google.auth.oauth2.AccessToken("", new java.util.Date(0));
        }
      };
    }
  }

  private byte[] loadNonPlaceholderResource(String name) throws IOException {
    InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
    if (stream == null) {
      return null;
    }
    try (stream) {
      byte[] bytes = stream.readAllBytes();
      String content = new String(bytes, StandardCharsets.UTF_8);
      if (content.contains("YOUR_PRIVATE_KEY_HERE")
          || content.contains("YOUR_PRIVATE_KEY_ID_HERE")
          || content.contains("REPLACE_WITH_ACTUAL_KEY_FROM_SECRET_MANAGER")
          || content.contains("REPLACE_WITH_SECRET_MANAGER")) {
        log.warn("Firebase: Detected placeholder in classpath file '{}'. Ignoring.", name);
        return null;
      }
      return bytes;
    }
  }
}
