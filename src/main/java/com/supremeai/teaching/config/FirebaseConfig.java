package com.supremeai.teaching.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.config.path:src/main/resources/service-account.json}")
    private String configPath;

    @Value("${firebase.database.url:https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/}")
    private String databaseUrl;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                GoogleCredentials credentials;
                try {
                    // Try loading from classpath first
                    InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("service-account.json");
                    if (serviceAccount != null) {
                        credentials = GoogleCredentials.fromStream(serviceAccount);
                    } else if (configPath != null && !configPath.trim().isEmpty()) {
                        // Fallback to file system
                        credentials = GoogleCredentials.fromStream(new FileInputStream(configPath));
                    } else {
                        // Use Application Default Credentials for Cloud Run
                        credentials = GoogleCredentials.getApplicationDefault();
                    }
                } catch (Exception e) {
                    // Fallback to Application Default Credentials if file not found
                    System.out.println("Could not load service-account.json, falling back to Application Default Credentials");
                    credentials = GoogleCredentials.getApplicationDefault();
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .setDatabaseUrl(databaseUrl)
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("FirebaseApp initialized successfully.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
