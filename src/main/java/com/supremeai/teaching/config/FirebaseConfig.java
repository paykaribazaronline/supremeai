package com.supremeai.teaching.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Firebase Configuration for Teaching System
 * Initializes Firebase Admin SDK and provides Firestore bean
 */
@Configuration
public class FirebaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);
    
    @Value("${firebase.credentials.path:}")
    private String firebaseCredentialsPath;
    
    @Value("${firebase.project.id:supremeai-a}")
    private String projectId;
    
    @Bean
    public Firestore firestore() {
        try {
            // Check if Firebase app already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                GoogleCredentials credentials;
                
                // Try to load credentials from file if path provided
                if (firebaseCredentialsPath != null && !firebaseCredentialsPath.isEmpty()) {
                    credentials = GoogleCredentials.fromStream(new FileInputStream(firebaseCredentialsPath));
                } else {
                    // Use Application Default Credentials (works in GCP environment)
                    credentials = GoogleCredentials.getApplicationDefault();
                }
                
                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(projectId)
                    .build();
                
                FirebaseApp.initializeApp(options);
                logger.info("✅ Firebase initialized with project: {}", projectId);
            }
            
            return FirestoreClient.getFirestore();
        } catch (IOException e) {
            logger.warn("⚠️ Firebase initialization failed - using in-memory storage fallback");
            logger.warn("Reason: {}", e.getMessage());
            logger.warn("Set GOOGLE_APPLICATION_CREDENTIALS or firebase.credentials.path for persistence");
            // Return null, services will handle graceful fallback
            return null;
        }
    }
}
