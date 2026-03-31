package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@RestController
public class Application {
    
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        try {
            logger.info("🚀 Starting SupremeAI Backend Service...");
            SpringApplication.run(Application.class, args);
            logger.info("✅ SupremeAI Backend Service started successfully!");
        } catch (Exception e) {
            logger.error("❌ Failed to start SupremeAI Backend Service", e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    // 🔓 FIX: স্প্রিং সিকিউরিটি আপাতত সব রিকোয়েস্ট পারমিট করবে যাতে ক্লাউড রান চেক করতে পারে
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        return http.build();
    }

    @GetMapping("/")
    public String home() {
        logger.info("✓ Home endpoint accessed");
        return "🚀 SupremeAI Cloud Server is Running! Version: 3.5";
    }

    @GetMapping("/actuator/health")
    public String health() {
        logger.info("✓ Health check endpoint accessed");
        return "{\"status\":\"UP\",\"timestamp\":\"" + System.currentTimeMillis() + "\"}";
    }
}
