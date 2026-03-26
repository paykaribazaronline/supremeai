package org.example.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SelfOptimizer {
    private final MemoryManager memoryManager;
    private final FirebaseService firebaseService;
    private final String buildFilePath = "build.gradle.kts";

    public SelfOptimizer(MemoryManager memoryManager, FirebaseService firebaseService) {
        this.memoryManager = memoryManager;
        this.firebaseService = firebaseService;
    }

    /**
     * Scans for outdated dependencies and logs them for potential upgrades.
     * In a real production system, this would call a Maven/Gradle metadata API.
     */
    public void scanAndOptimizeDependencies() {
        System.out.println("\n🔍 [SELF-OPTIMIZER] Scanning for efficiency upgrades...");
        
        try {
            Path path = Paths.get(buildFilePath);
            if (!Files.exists(path)) return;
            
            String content = Files.readString(path);
            Map<String, String> currentVersions = extractVersions(content);
            
            // Simulated "Best Versions" found from cloud metadata
            Map<String, String> latestVersions = new HashMap<>();
            latestVersions.put("firebase-admin", "9.2.0"); // Current
            latestVersions.put("google-cloud-firestore", "3.39.0"); // Found Upgrade
            latestVersions.put("okhttp", "5.3.2"); // Found Upgrade
            latestVersions.put("jackson-databind", "2.21.2"); // Found Upgrade

            latestVersions.forEach((dep, latest) -> {
                String current = currentVersions.get(dep);
                if (current != null && !current.equals(latest)) {
                    System.out.println("✨ [OPTIMIZER] Found better version for " + dep + ": " + current + " -> " + latest);
                    logOptimizationOpportunity(dep, current, latest);
                    
                    // Auto-propose to Firebase for Admin approval
                    firebaseService.sendNotification("admin", "Efficiency Upgrade Available", 
                        "Upgrade " + dep + " to " + latest + " for better performance.", "update");
                }
            });

        } catch (IOException e) {
            System.err.println("Failed to read build file: " + e.getMessage());
        }
    }

    private Map<String, String> extractVersions(String content) {
        Map<String, String> versions = new HashMap<>();
        // Simple regex to find common patterns in your build.gradle.kts
        Pattern pattern = Pattern.compile("implementation\\(\"com\\..*?:(.*?):([0-9.]+)\"\\)");
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String fullPath = matcher.group(0);
            String version = matcher.group(2);
            if (fullPath.contains("firestore")) versions.put("google-cloud-firestore", version);
            if (fullPath.contains("firebase-admin")) versions.put("firebase-admin", version);
            if (fullPath.contains("okhttp")) versions.put("okhttp", version);
            if (fullPath.contains("jackson-databind")) versions.put("jackson-databind", version);
        }
        return versions;
    }

    private void logOptimizationOpportunity(String dep, String oldVer, String newVer) {
        // Record in memory so the system "knows" it's working on an older version
        memoryManager.recordFailure("DEPENDENCY_OPTIMIZATION", "SelfOptimizer", 
            "Running on outdated " + dep + " v" + oldVer + ". Recommendation: v" + newVer);
    }
}
