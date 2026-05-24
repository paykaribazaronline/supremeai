package com.supremeai.service.analysis;

import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Project DNA Harvester — Autonomous Architecture Awareness.
 * 
 * Scans the local filesystem to extract project-specific architectural patterns,
 * technology stack, and business logic structures. This supports the "Solo Mode"
 * principle by allowing the system to work 100% offline for project-specific tasks.
 */
@Service
public class ProjectDNAHarvesterService {

    private static final Logger log = LoggerFactory.getLogger(ProjectDNAHarvesterService.class);

    @Autowired
    private SystemLearningRepository systemLearningRepository;

    private static final String PROJECT_ROOT = "/home/nazifarabbu/supremeai";

    /**
     * Start the autonomous DNA harvesting process.
     */
    public Mono<Void> harvestDNA() {
        log.info("🧬 Starting Project DNA Harvester...");
        
        return Mono.fromCallable(() -> {
            Map<String, Object> dna = new HashMap<>();
            dna.put("last_harvested", new Date());
            
            // 1. Analyze Tech Stack from build.gradle.kts
            dna.put("stack", analyzeTechStack());
            
            // 2. Analyze Architecture from Directory Structure
            dna.put("architecture", analyzeArchitecture());
            
            // 3. Extract Main Package
            dna.put("main_package", "com.supremeai");
            
            return dna;
        })
        .flatMap(this::persistDNA)
        .then();
    }

    private Map<String, Object> analyzeTechStack() {
        Map<String, Object> stack = new HashMap<>();
        try {
            Path buildFile = Paths.get(PROJECT_ROOT, "build.gradle.kts");
            if (Files.exists(buildFile)) {
                String content = Files.readString(buildFile);
                
                stack.put("java_version", content.contains("JavaVersion.VERSION_21") ? "21" : "unknown");
                stack.put("spring_boot_version", extractRegex(content, "id\\(\"org.springframework.boot\"\\) version \"([^\"]+)\""));
                
                List<String> deps = new ArrayList<>();
                if (content.contains("spring-cloud-gcp-starter-data-firestore")) deps.add("Firestore (Reactive)");
                if (content.contains("spring-boot-starter-webflux")) deps.add("WebFlux (Non-blocking)");
                if (content.contains("resilience4j")) deps.add("Resilience4j (Circuit Breaker)");
                if (content.contains("jjwt")) deps.add("JWT Security");
                if (content.contains("postgresql")) deps.add("PostgreSQL");
                
                stack.put("core_dependencies", deps);
            }
        } catch (Exception e) {
            log.warn("Failed to analyze build.gradle.kts: {}", e.getMessage());
        }
        return stack;
    }

    private Map<String, Object> analyzeArchitecture() {
        Map<String, Object> arch = new HashMap<>();
        try {
            Path srcPath = Paths.get(PROJECT_ROOT, "src/main/java/com/supremeai");
            if (Files.exists(srcPath)) {
                List<String> subdirs = Files.list(srcPath)
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
                
                arch.put("modules", subdirs);
                arch.put("pattern", subdirs.contains("service") && subdirs.contains("controller") ? "MVC / Layered" : "Microservices");
                arch.put("is_reactive", subdirs.contains("repository") ? "True (Spring Data)" : "False");
            }
        } catch (Exception e) {
            log.warn("Failed to analyze directory structure: {}", e.getMessage());
        }
        return arch;
    }

    private Mono<Void> persistDNA(Map<String, Object> dna) {
        SystemLearning learning = new SystemLearning();
        learning.setId("PROJECT_DNA_SNAPSHOT");
        learning.setCategory("PROJECT_ARCHITECTURE");
        learning.setTitle("SupremeAI Autonomous DNA Snapshot");
        learning.setContent("Automated extraction of project architectural patterns and tech stack.");
        
        StringBuilder solutions = new StringBuilder();
        solutions.append("Tech Stack: ").append(dna.get("stack")).append("\n");
        solutions.append("Architecture: ").append(dna.get("architecture")).append("\n");
        solutions.append("Main Package: ").append(dna.get("main_package"));
        
        learning.setSolutions(List.of(solutions.toString()));
        learning.setConfidence(1.0);
        learning.setPermanent(true);
        learning.setMetadata(dna);
        learning.setUpdatedAt(java.time.LocalDateTime.now());

        log.info("📁 Saving Project DNA Snapshot to system_learning repository");
        return systemLearningRepository.save(learning).then();
    }

    private String extractRegex(String content, String patternStr) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patternStr);
        java.util.regex.Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : "unknown";
    }
}
