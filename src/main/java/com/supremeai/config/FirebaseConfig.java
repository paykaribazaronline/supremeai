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

    @Value("${firebase.database.url:https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/}")
    private String databaseUrl;

    /** 
     * FirebaseApp‑কে ইনিশিয়ালাইজ করে এবং Spring‑এ Bean হিসেবে রেজিস্টার করে। 
     */
    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        log.info("Initializing Firebase Application for project: {}", projectId);
        
        GoogleCredentials credentials = loadCredentials();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .setDatabaseUrl(databaseUrl)
                .setStorageBucket(projectId + ".appspot.com")
                .build();

        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        log.info("Initializing Firestore client");
        return FirestoreClient.getFirestore(firebaseApp);
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
            return GoogleCredentials.fromStream(serviceAccount);
        }

        // 4. Fallback to Application Default Credentials
        log.info("Firebase: Falling back to Application Default Credentials (ADC)");
        return GoogleCredentials.getApplicationDefault();
    }
}
