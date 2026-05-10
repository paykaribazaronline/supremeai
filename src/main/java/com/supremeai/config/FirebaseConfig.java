package com.supremeai.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

/**
 * প্রজেক্টে Firebase ব্যবহার করতে এই ক্লাসটি প্রয়োজন।
 * এটি DEFAULT নামের একটী FirebaseApp তৈরি করে, যাতে
 * AuthenticationController‑এ {@code FirebaseAuth.getInstance()}
 * সঠিকভাবে কাজ করে।
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
     * Controlled by 'firebase.enabled' property (default true).
     */
    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
    public FirebaseApp firebaseApp() throws IOException {
        log.info("Initializing Firebase Application for project: {} and database: {}", projectId, databaseUrl);
        
        // রিসোর্স ফোল্ডারে (src/main/resources) firebase-service-account.json নামের ফাইলodziallyতি থাকে,
        // সেটি ব্যবহার করা হবে।
        InputStream serviceAccount = getClass().getClassLoader()
                .getResourceAsStream("firebase-service-account.json");

        FirebaseOptions.Builder builder = FirebaseOptions.builder()
                .setProjectId(projectId)
                .setDatabaseUrl(databaseUrl)
                .setStorageBucket(projectId + ".appspot.com");

        if (serviceAccount != null) {
            log.info("Firebase: Loading credentials from bundled firebase-service-account.json");
            builder.setCredentials(GoogleCredentials.fromStream(serviceAccount));
        } else {
            try {
                log.info("Firebase: Attempting to load Application Default Credentials (ADC)");
                builder.setCredentials(GoogleCredentials.getApplicationDefault());
            } catch (IOException e) {
                log.error("Firebase: Credentials not found! " +
                        "Please place firebase-service-account.json in src/main/resources/ or set GOOGLE_APPLICATION_CREDENTIALS.");
                throw e;
            }
        }

        // Cloud configuration completed

         FirebaseOptions options = builder.build();
         
         // "DEFAULT" calls app রেজিস্টার – AuthenticationController‑এ এটাই প্রত্যশিত
         try {
             return FirebaseApp.getInstance();
         } catch (IllegalStateException e) {
             log.info("Firebase: Registering [DEFAULT] app instance");
             return FirebaseApp.initializeApp(options);
         }
    }
}

