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

                if (jwtSecret.isBlank()) {
                    System.err.println("[SECURITY] FATAL: jwt.secret / JWT_SECRET is not set.");
                    System.err.println("[SECURITY] Set the JWT_SECRET environment variable (32+ chars) before starting the app.");
                    System.exit(1);
                }

                if (jwtSecret.length() < 32) {
                    System.err.println("[SECURITY] FATAL: jwt.secret must be at least 32 characters for HS256.");
                    System.exit(1);
                }

                boolean isTestOrLocal = "local".equals(profile) || "test".equals(profile) || "sandbox".equals(profile);
                if (!isTestOrLocal) {
                    if (jwtSecret.startsWith("supremeai-test")
                            || jwtSecret.contains("test-secret-key")) {
                        System.err.println("[SECURITY] FATAL: Test JWT secret detected in non-test environment (profile: '" + profile + "').");
                        System.exit(1);
                    }
                }

                String projectId = env.getProperty("spring.cloud.gcp.firestore.project-id", "").trim();
                if (projectId.isBlank()) {
                    System.err.println("[SECURITY] FATAL: spring.cloud.gcp.firestore.project-id is not configured.");
                    System.err.println("[SECURITY] Set GCP_PROJECT_ID or spring.cloud.gcp.firestore.project-id before starting.");
                    System.exit(1);
                }
            }
        );
        app.run(args);
    }
}
