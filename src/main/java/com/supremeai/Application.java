package com.supremeai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.google.cloud.spring.data.firestore.repository.config.EnableReactiveFirestoreRepositories;
import org.springframework.core.env.Environment;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableReactiveFirestoreRepositories
public class Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Application.class);
        app.addListeners((org.springframework.context.ApplicationListener<org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent>) event -> {
                Environment env = event.getEnvironment();
                String jwtSecret = env.getProperty("jwt.secret", "");
                String profile = env.getActiveProfiles().length > 0 ? env.getActiveProfiles()[0] : "default";
                boolean isTestOrLocal = "local".equals(profile) || "test".equals(profile) || "sandbox".equals(profile);
                if (!isTestOrLocal) {
                    if (jwtSecret.isBlank() || 
                        jwtSecret.startsWith("supremeai-test") || 
                        jwtSecret.contains("test-secret-key") || 
                        jwtSecret.length() < 32) {
                        System.err.println("[SECURITY] FATAL: Weak, blank, or test JWT_SECRET detected in production environment (profile: '" + profile + "').");
                        System.err.println("[SECURITY] A production JWT secret must be at least 32 characters (256 bits) long and must not use test placeholders.");
                        System.exit(1);
                    }
                }
            }
        );
        SpringApplication.run(Application.class, args);
    }
}