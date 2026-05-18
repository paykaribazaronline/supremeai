package com.supremeai.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * CodeValidationService - Basic compilation/validation for generated code.
 * Taste phase: writes files to temp directory and runs basic syntax checks.
 * Sprint 2 P0: minimal implementation (placeholder for full validation).
 */
@Service
public class CodeValidationService {

    /**
     * Validate generated code files by checking for required content.
     * In taste phase, just verifies required files exist and are non-empty.
     *
     * @param files map of filename → file content
     * @return validation result map with "valid", "errors", "warnings"
     */
    public Map<String, Object> validate(Map<String, String> files) {
        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
        java.util.List<String> errors = new java.util.ArrayList<>();
        java.util.List<String> warnings = new java.util.ArrayList<>();

        // Check required files
        String[] requiredFiles = {
            "build.gradle.kts",
            "src/main/java/com/example/generated/GeneratedAppApplication.java"
        };

        for (String required : requiredFiles) {
            if (!files.containsKey(required)) {
                errors.add("Missing required file: " + required);
            }
        }

        // Basic content checks
        if (files.containsKey("build.gradle.kts")) {
            String content = files.get("build.gradle.kts");
            if (!content.contains("plugins")) {
                warnings.add("build.gradle.kts missing 'plugins' block");
            }
            if (!content.contains("dependencies")) {
                warnings.add("build.gradle.kts missing 'dependencies' block");
            }
        }

        if (files.containsKey("src/main/java/com/example/generated/GeneratedAppApplication.java")) {
            String content = files.get("src/main/java/com/example/generated/GeneratedAppApplication.java");
            if (!content.contains("@SpringBootApplication")) {
                errors.add("Main class missing @SpringBootApplication");
            }
            if (!content.contains("main(")) {
                errors.add("Main class missing main method");
            }
        }

        boolean valid = errors.isEmpty();
        result.put("valid", valid);
        result.put("errors", errors);
        result.put("warnings", warnings);
        result.put("fileCount", files.size());

        return result;
    }

    /**
     * Write generated files to disk for manual inspection (taste phase).
     * Returns directory path where files were written.
     * Uses NIO Files API with explicit UTF-8 encoding — no platform charset corruption.
     */
    public String writeToTempDirectory(Map<String, String> files, String appName) throws IOException {
        Path baseDir = Path.of(System.getProperty("java.io.tmpdir"),
                "supremeai-" + appName + "-" + System.currentTimeMillis());
        Files.createDirectories(baseDir);

        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path outFile = baseDir.resolve(entry.getKey());
            Files.createDirectories(outFile.getParent());
            Files.writeString(outFile, entry.getValue(), StandardCharsets.UTF_8);
        }

        return baseDir.toAbsolutePath().toString();
    }
}
