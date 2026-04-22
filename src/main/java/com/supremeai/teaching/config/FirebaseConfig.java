package com.supremeai.teaching.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);
    @Value("${firebase.config.path:src/main/resources/service-account.json}")
    private String configPath;

    @Value("${firebase.database.url:https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/}")
    private String databaseUrl;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                GoogleCredentials credentials = loadCredentials();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .setDatabaseUrl(databaseUrl)
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("FirebaseApp initialized successfully.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Firebase", e);
        }
    }

    private GoogleCredentials loadCredentials() throws IOException {
        // 1. Cloud Run: read FIREBASE_SERVICE_ACCOUNT_JSON env var (secret content mounted by --update-secrets)
        String secretJson = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON");
        if (secretJson != null && !secretJson.isBlank()) {
            log.info("Loading Firebase credentials from FIREBASE_SERVICE_ACCOUNT_JSON env var");
            try (InputStream stream = new ByteArrayInputStream(secretJson.getBytes(StandardCharsets.UTF_8))) {
                return GoogleCredentials.fromStream(stream);
            }
        }

        // 2. Try classpath resource (works when JAR includes service-account.json)
        InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("service-account.json");
        if (serviceAccount != null) {
            log.info("Loading Firebase credentials from classpath service-account.json");
            return GoogleCredentials.fromStream(serviceAccount);
        }

        // 3. Fallback to file system path
        if (configPath != null && !configPath.trim().isEmpty()) {
            try {
                log.info("Loading Firebase credentials from file: {}", configPath);
                return GoogleCredentials.fromStream(new FileInputStream(configPath));
            } catch (IOException e) {
                log.warn("Could not load Firebase credentials from file: {}", configPath);
            }
        }

        // 4. Use Application Default Credentials (ADC) on Cloud Run / GCP
        log.info("Falling back to Application Default Credentials for Firebase");
        return GoogleCredentials.getApplicationDefault();
    }
}
