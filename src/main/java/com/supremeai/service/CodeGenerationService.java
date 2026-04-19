package com.supremeai.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CodeGenerationService {

    /**
     * Generate a simple skeleton application based on flat spec (legacy).
     */
    public Map<String, Object> generate(Map<String, Object> spec) {
        return generateFromContext(Collections.emptyMap());
    }

    /**
     * Generate application from orchestration context (decisions: db, architecture, etc).
     * Context keys expected: "architecture", "database", "apiStyle", "authType", "frontend", "deployment"
     */
    public Map<String, Object> generateFromContext(Map<String, String> decisions) {
        String appName = "GeneratedApp";
        Map<String, String> files = new LinkedHashMap<>();

        // Derive tech stack from decisions (with defaults)
        String architecture = decisions.getOrDefault("architecture", "monolith");
        String database = decisions.getOrDefault("database", "PostgreSQL");
        String apiStyle = decisions.getOrDefault("apiStyle", "REST");
        String authType = decisions.getOrDefault("authType", "JWT");
        String frontend = decisions.getOrDefault("frontend", "React");
        String deployment = decisions.getOrDefault("deployment", "GCP");

        // Build dependencies based on decisions
        List<String> dependencies = new ArrayList<>();
        dependencies.add("implementation(\"org.springframework.boot:spring-boot-starter-web\")");
        dependencies.add("implementation(\"org.springframework.boot:spring-boot-starter-validation\")");
        
        // Database driver
        switch (database.toLowerCase()) {
            case "postgresql": 
                dependencies.add("runtimeOnly(\"org.postgresql:postgresql\")");
                break;
            case "mysql": 
                dependencies.add("runtimeOnly(\"com.mysql:mysql-connector-j\")");
                break;
            case "mongodb": 
                dependencies.add("implementation(\"org.springframework.boot:spring-boot-starter-data-mongodb\")");
                break;
        }

        // JWT auth
        if (authType.equalsIgnoreCase("JWT")) {
            dependencies.add("implementation(\"io.jsonwebtoken:jjwt-api:0.12.5\")");
            dependencies.add("runtimeOnly(\"io.jsonwebtoken:jjwt-impl:0.12.5\")");
            dependencies.add("runtimeOnly(\"io.jsonwebtoken:jjwt-jackson:0.12.5\")");
        }

        // Build.gradle
        String depsBlock = String.join(",\n                ", dependencies);
        String buildGradle = """
            plugins {
                id("org.springframework.boot") version "3.2.3"
                id("io.spring.dependency-management") version "1.1.4"
                java
            }
            group = "com.example"
            version = "1.0.0"
            java {
                sourceCompatibility = JavaVersion.VERSION_17
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                %s
            }
            tasks.getByName("test") {
                useJUnitPlatform()
            }
            """.formatted(depsBlock);
        files.put("build.gradle.kts", buildGradle);

        // Application class
        String appClass = """
            package com.example.generated;
            
            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            
            @SpringBootApplication
            public class GeneratedAppApplication {
                public static void main(String[] args) {
                    SpringApplication.run(GeneratedAppApplication.class, args);
                }
            }
            """;
        files.put("src/main/java/com/example/generated/GeneratedAppApplication.java", appClass);

        // Health controller
        String healthCtrl = """
            package com.example.generated.controller;
            
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;
            
            @RestController
            @RequestMapping("/api")
            public class HealthController {
                @GetMapping("/health")
                public Map<String, String> health() {
                    return Map.of("status", "UP", "database", "%s", "architecture", "%s");
                }
                
                @GetMapping("/info")
                public Map<String, String> info() {
                    return Map.of("app", "GeneratedApp", "version", "1.0.0");
                }
            }
            """.formatted(database, architecture);
        files.put("src/main/java/com/example/generated/controller/HealthController.java", healthCtrl);

        // Dockerfile if requested
        String dockerfile = """
            FROM eclipse-temurin:17-jre-alpine
            ARG JAR_FILE=build/libs/*.jar
            COPY ${JAR_FILE} app.jar
            ENTRYPOINT ["java","-jar","/app.jar"]
            """;
        files.put("Dockerfile", dockerfile);

        // README with decisions
        String readme = """
            # Generated Application
            
            Architecture: %s
            Database: %s
            API Style: %s
            Auth: %s
            Frontend: %s
            Deployment: %s
            
            ## Run
            
            ./gradlew bootRun
            
            ## API Endpoints
            
            - GET /api/health - health check
            - GET /api/info - app info
            
            Generated by SupremeAI (Sprint 2).
            """.formatted(architecture, database, apiStyle, authType, frontend, deployment);
        files.put("README.md", readme);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("appName", appName);
        result.put("files", files);
        result.put("fileCount", files.size());
        result.put("status", "GENERATED");
        result.put("decisions", decisions);
        result.put("message", "App generated from consensus decisions (Sprint 2)");
        return result;
    }
}
