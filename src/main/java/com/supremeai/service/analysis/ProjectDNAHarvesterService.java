package com.supremeai.service.analysis;

import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import com.supremeai.service.DynamicSignatureRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Project DNA Harvester — Autonomous Architecture Awareness.
 *
 * <p>Scans the local filesystem to extract project-specific architectural patterns, technology
 * stack, and business logic structures. This supports the "Solo Mode" principle by allowing the
 * system to work 100% offline for project-specific tasks.
 */
@Service
public class ProjectDNAHarvesterService {

  private static final Logger log = LoggerFactory.getLogger(ProjectDNAHarvesterService.class);

  @Autowired private SystemLearningRepository systemLearningRepository;

  @Autowired private DynamicSignatureRegistry signatureRegistry;

  /** sProject root should be resolved dynamically or via environment. */
  private static final String PROJECT_ROOT = System.getProperty("user.dir");

  /** Start the autonomous DNA harvesting process. */
  public Mono<Void> harvestDNA() {
    log.info("🧬 Starting Project DNA Harvester...");

    return Mono.fromCallable(
            () -> {
              Map<String, Object> dna = new HashMap<>();
              dna.put("last_harvested", new Date());

              // 1. Analyze Tech Stack from build.gradle.kts
              dna.put("stack", analyzeTechStack());

              // 2. Analyze Additional Config Files (CD-03)
              dna.put("frameworks", analyzeOtherConfigs());

              // 3. Analyze Architecture from Directory Structure
              dna.put("architecture", analyzeArchitecture());

              // 4. Extract Main Package
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
        stack.put(
            "spring_boot_version",
            extractRegex(content, "id\\(\"org.springframework.boot\"\\) version \"([^\"]+)\""));

        // Externalized signatures via dynamic patterns (e.g., resilience4j, webflux)
        List<String> deps = new ArrayList<>();

        stack.put("core_dependencies", deps);
      }
    } catch (Exception e) {
      log.warn("Failed to analyze build.gradle.kts: {}", e.getMessage());
    }
    return stack;
  }

  private Map<String, Object> analyzeOtherConfigs() {
    Map<String, Object> frameworks = new HashMap<>();
    try {
      // Check for Node.js / Frontend frameworks - dynamically detect any framework
      Path packageJson = Paths.get(PROJECT_ROOT, "package.json");
      if (Files.exists(packageJson)) {
        String content = Files.readString(packageJson).toLowerCase();
        frameworks.put("frontend_dependencies", signatureRegistry.findMatches(content, "JS_DEP"));
      }

      // Check for Docker
      Path dockerCompose = Paths.get(PROJECT_ROOT, "docker-compose.yml");
      if (Files.exists(dockerCompose)) {
        String content = Files.readString(dockerCompose).toLowerCase();
        frameworks.put("docker_services", signatureRegistry.findMatches(content, "DOCKER"));
      }

      // Check for Python
      Path reqTxt = Paths.get(PROJECT_ROOT, "requirements.txt");
      if (Files.exists(reqTxt)) {
        String content = Files.readString(reqTxt).toLowerCase();
        frameworks.put("python_dependencies", signatureRegistry.findMatches(content, "PYTHON"));
      }

    } catch (Exception e) {
      log.warn("Failed to analyze other config files: {}", e.getMessage());
    }
    return frameworks;
  }

  private Map<String, Object> analyzeArchitecture() {
    Map<String, Object> arch = new HashMap<>();
    try {
      Path srcPath = Paths.get(PROJECT_ROOT, "src/main/java/com/supremeai");
      if (Files.exists(srcPath)) {
        List<String> subdirs =
            Files.list(srcPath)
                .filter(Files::isDirectory)
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());

        arch.put("modules", subdirs);
        arch.put(
            "pattern",
            subdirs.contains("service") && subdirs.contains("controller")
                ? "MVC / Layered"
                : "Microservices");
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
    solutions.append("Frameworks/Configs: ").append(dna.get("frameworks")).append("\n");
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
