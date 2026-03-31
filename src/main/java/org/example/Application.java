package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
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
        return "🚀 SupremeAI Cloud Server is Running! Version: 3.5";
    }

    @GetMapping("/actuator/health")
    public String health() {
        return "{\"status\":\"UP\"}";
    }
}
