package com.supremeai.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import java.util.Collections;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * প্রজেক্টে Firebase এবং Firestore ব্যবহার করতে এই ক্লাসটি প্রয়োজন।
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.project.id:supremeai-a}")
    private String projectId;

    @Value("${spring.cloud.gcp.firestore.database-id:(default)}")
    private String databaseId;

    @Value("${firebase.database.url:https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/}")
    private String databaseUrl;

    /** 
     * FirebaseApp‑কে ইনিশিয়ালাইজ করে এবং Spring‑এ Bean হিসেবে রেজিস্টার করে। 
     */
    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
    public FirebaseApp firebaseApp(GoogleCredentials credentials) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        log.info("Initializing Firebase Application for project: {}", projectId);
        
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .setDatabaseUrl(databaseUrl)
                .setStorageBucket(projectId + ".appspot.com")
                .build();

        return FirebaseApp.initializeApp(options);
    }

    @Bean
    @Primary
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
        // 1. Check for environment variable (useful for Cloud Run/Docker)
        String secretJson = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON");
        if (secretJson != null && !secretJson.isBlank()) {
            log.info("Firebase: Loading credentials from FIREBASE_SERVICE_ACCOUNT_JSON env var");
            return GoogleCredentials.fromStream(new ByteArrayInputStream(secretJson.getBytes(StandardCharsets.UTF_8)));
        }

        // 2. Try primary service account file
        InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
        if (serviceAccount == null) {
            // 3. Try alternative service account file name
            serviceAccount = getClass().getClassLoader().getResourceAsStream("service-account.json");
        }

        if (serviceAccount != null) {
            log.info("Firebase: Loading credentials from classpath JSON file");
            return GoogleCredentials.fromStream(serviceAccount)
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
        }

        // 4. Fallback to Application Default Credentials
        log.info("Firebase: Falling back to Application Default Credentials (ADC)");
        return GoogleCredentials.getApplicationDefault()
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
    }
}
