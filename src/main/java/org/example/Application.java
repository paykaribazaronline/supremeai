package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @GetMapping("/")
    public String home() {
        return "🚀 SupremeAI Cloud Server is Running! Version: 3.5 (Phase 1)";
    }

    @GetMapping("/actuator/health")
    public String health() {
        return "{\"status\":\"UP\", \"cloud\":\"GCP\", \"project\":\"supremeai-a\"}";
    }
}
