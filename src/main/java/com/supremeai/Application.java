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
                if (jwtSecret.startsWith("supremeai-test") && !"local".equals(profile) && !"test".equals(profile) && !"sandbox".equals(profile)) {
                    System.err.println("[SECURITY] FATAL: JWT_SECRET is using the default test secret value in profile '" + profile + "'. Refusing to start.");
                    System.err.println("[SECURITY] Set JWT_SECRET environment variable to a secure HS256 key before starting the application.");
                    System.exit(1);
                }
            }
        );
        SpringApplication.run(Application.class, args);
    }
}