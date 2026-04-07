package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.example.service.RequirementAnalyzer.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Self Extender
 * Writes generated code to files, recompiles, and auto-loads new components
 */
@Service
public class SelfExtender {
    private static final Logger logger = LoggerFactory.getLogger(SelfExtender.class);
    
    @Autowired
    private RequirementAnalyzer analyzerService;
    
    @Autowired
    private CodeGenerator codeGenerator;
    
    @Autowired
    private SystemLearningService learningService;
    
    @Autowired
    private HotReloadService hotReloadService;
    
    @Autowired
    private RequestQueueService requestQueueService;

    @Autowired(required = false)
    private IdleResearchService idleResearchService;

    @Autowired
    private LocalJsonStoreService jsonStore;
    
    private static final String SRC_PATH = "src/main/java/org/example";
    private static final String HISTORY_STORE_PATH = "self-extension/history.json";

    /** Persisted history of all extension commands submitted */
    private final List<Map<String, Object>> extensionHistory = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        List<Map<String, Object>> saved = jsonStore.read(
                HISTORY_STORE_PATH,
                new TypeReference<List<Map<String, Object>>>() {},
                List.of());
        extensionHistory.addAll(saved);
        logger.info("✅ SelfExtender ready — restored {} extension history entries", saved.size());
    }

    private void recordExtension(String requirement, boolean success, int filesCreated) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("requirement", requirement);
        record.put("success", success);
        record.put("filesCreated", filesCreated);
        record.put("timestamp", System.currentTimeMillis());
        extensionHistory.add(record);
        // Cap at 500
        while (extensionHistory.size() > 500) extensionHistory.remove(0);
        jsonStore.write(HISTORY_STORE_PATH, extensionHistory);
    }

    public List<Map<String, Object>> getExtensionHistory() {
        return new ArrayList<>(extensionHistory);
    }
    
    /**
     * Process new requirement and create code
     */
    public boolean implementRequirement(String requirementText) {
        try {
            // Notify research engine the system is working
            if (idleResearchService != null) {
                idleResearchService.notifyProjectActivity();
            }
            logger.info("🤖 Processing requirement: {}", requirementText);
            
            // 1. Analyze requirement
            Requirement req = analyzerService.analyze(requirementText);
            
            // 2. Record as critical if needed
            if (analyzerService.isCritical(requirementText)) {
                learningService.recordRequirement(req.name, requirementText);
            }
            
            // 3. Generate code
            Map<String, String> generated = codeGenerator.generateComplete(req);
            
            // 4. Write to files
            boolean written = writeFiles(req, generated);
            if (!written) {
                logger.error("❌ Failed to write files");
                return false;
            }
            
            // 5. Compile
            boolean compiled = recompile();
            if (!compiled) {
                logger.error("❌ Compilation failed");
                return false;
            }
            
            logger.info("✅ Requirement implemented: {} -> {} files created", req.name, generated.size());
            recordExtension(requirementText, true, generated.size());
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Failed to implement requirement: {}", e.getMessage());
            recordExtension(requirementText, false, 0);
            return false;
        }
    }
    
    /**
     * Write generated code to files
     */
    private boolean writeFiles(Requirement req, Map<String, String> generated) {
        try {
            for (Map.Entry<String, String> file : generated.entrySet()) {
                String type = file.getKey(); // Model, Service, Controller
                String code = file.getValue();
                
                String folder;
                if ("Model".equals(type)) {
                    folder = "model";
                } else if ("Controller".equals(type)) {
                    folder = "controller";
                } else {
                    folder = "service";
                }
                
                String filename = req.name + type + ".java";
                Path filepath = Paths.get(SRC_PATH, folder, filename);
                
                // Create directories if needed
                Files.createDirectories(filepath.getParent());
                
                // Write file
                Files.write(filepath, code.getBytes());
                logger.info("✅ Created: {}/{}", folder, filename);
            }
            
            return true;
        } catch (Exception e) {
            logger.error("❌ File writing error: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Recompile project
     */
    private boolean recompile() {
        try {
            logger.info("🔨 Recompiling...");
            
            ProcessBuilder pb = new ProcessBuilder("./gradlew", "compileJava");
            pb.directory(new File("."));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int errorCount = 0;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains("error:")) {
                    errorCount++;
                    logger.error("Compile error: {}", line);
                }
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                logger.info("✅ Compilation successful!");
                // ✅ TRY: Hot-reload the new code
                boolean hotReloaded = hotReloadService.loadNewClass(
                    "org.example.service.SelfExtender", 
                    "build/classes/java/main"
                );
                
                if (hotReloaded) {
                    logger.info("⚡ Hot-reload successful - new code loaded");
                } else {
                    logger.warn("⚠️ Hot-reload failed - restart required");
                    hotReloadService.gracefulRestart();
                }
                
                return true;
            } else {
                logger.error("❌ Compilation failed with code: {}", exitCode);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("❌ Recompile failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get status of self-extension
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "ready");
        status.put("message", "SelfExtender active - SupremeAI can create its own code");
        status.put("timestamp", System.currentTimeMillis());
        status.put("totalExtensions", extensionHistory.size());
        long successCount = extensionHistory.stream()
                .filter(e -> Boolean.TRUE.equals(e.get("success")))
                .count();
        status.put("successfulExtensions", successCount);
        status.put("failedExtensions", extensionHistory.size() - successCount);
        // Include last 10 entries
        int size = extensionHistory.size();
        status.put("recentHistory", extensionHistory.subList(Math.max(0, size - 10), size));
        
        return status;
    }
}
