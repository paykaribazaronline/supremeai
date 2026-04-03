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
    private static final String TEACHING_FIREBASE_APP = "teaching-firebase-app";
    
    @Value("${firebase.credentials.path:}")
    private String firebaseCredentialsPath;
    
    @Value("${firebase.project.id:supremeai-a}")
    private String projectId;
    
    @Bean
    public Firestore firestore() {
        try {
            FirebaseApp firebaseApp;
            try {
                firebaseApp = FirebaseApp.getInstance(TEACHING_FIREBASE_APP);
            } catch (IllegalStateException noNamedApp) {
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

                firebaseApp = FirebaseApp.initializeApp(options, TEACHING_FIREBASE_APP);
                logger.info("✅ Firebase initialized with project: {} (app={})", projectId, TEACHING_FIREBASE_APP);
            }

            return FirestoreClient.getFirestore(firebaseApp);
        } catch (Exception e) {
            logger.warn("⚠️ Firebase initialization failed - using in-memory storage fallback");
            logger.warn("Reason: {}", e.getMessage());
            logger.warn("Set GOOGLE_APPLICATION_CREDENTIALS or firebase.credentials.path for persistence");
            // Return null, services will handle graceful fallback
            return null;
        }
    }
}
