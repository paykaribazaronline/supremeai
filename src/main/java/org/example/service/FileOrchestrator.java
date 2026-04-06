package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 3: File Orchestrator
 * 
 * The "Hand" that executes AI-generated code.
 * Provides sophisticated file management for code generation:
 * - Create/read/update/delete files
 * - Surgical code editing (string replacement)
 * - Directory management
 * - Execution logging to JSON
 * - Operation history and rollback support
 */
public class FileOrchestrator {
    private final Path baseProjectsPath;
    private final MemoryManager memoryManager;
    private final ObjectMapper mapper = new ObjectMapper();
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Execution logs per project (projectId -> operations list)
    private final Map<String, ArrayNode> executionLogs = new HashMap<>();

    public FileOrchestrator(String baseDir, MemoryManager memoryManager) {
        this.baseProjectsPath = Paths.get(baseDir);
        this.memoryManager = memoryManager;
        ensureBaseDirExists();
    }

    private void ensureBaseDirExists() {
        try {
            if (!Files.exists(baseProjectsPath)) {
                Files.createDirectories(baseProjectsPath);
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to create base projects directory: " + e.getMessage());
        }
    }

    // ============================================================================
    // CORE FILE OPERATIONS
    // ============================================================================

    /**
     * Create project directory structure
     */
    public Path createProjectStructure(String projectId) throws IOException {
        Path projectPath = baseProjectsPath.resolve(projectId);
        Files.createDirectories(projectPath);
        logOperation(projectId, "CREATE_PROJECT", projectId, "Project root created", true);
        
        // Initialize execution log
        executionLogs.put(projectId, mapper.createArrayNode());
        saveExecutionLog(projectId);
        
        return projectPath;
    }

    /**
     * Write content to a file (create or overwrite)
     */
    public void writeFile(String projectId, String relativePath, String content) throws IOException {
        Path filePath = getProjectPath(projectId).resolve(relativePath);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content, StandardCharsets.UTF_8, 
                         StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        logOperation(projectId, "WRITE_FILE", relativePath, 
                    "Created/overwritten file (" + content.length() + " chars)", true);
    }

    /**
     * Read content from a file
     */
    public String readFile(String projectId, String relativePath) throws IOException {
        Path filePath = getProjectPath(projectId).resolve(relativePath);
        
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + relativePath);
        }
        
        return Files.readString(filePath, StandardCharsets.UTF_8);
    }

    /**
     * Edit file by replacing a specific string (surgical edit)
     * Useful for modifying existing files without overwriting
     */
    public void editFile(String projectId, String relativePath, String oldContent, String newContent) throws IOException {
        String original = readFile(projectId, relativePath);
        
        if (!original.contains(oldContent)) {
            throw new IllegalArgumentException("String not found in file: " + relativePath);
        }
        
        String updated = original.replace(oldContent, newContent);
        writeFile(projectId, relativePath, updated);
        logOperation(projectId, "EDIT_FILE", relativePath, 
                    "Replaced " + oldContent.length() + " chars with " + newContent.length(), true);
    }

    /**
     * Append content to end of file
     */
    public void appendToFile(String projectId, String relativePath, String content) throws IOException {
        Path filePath = getProjectPath(projectId).resolve(relativePath);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content, StandardCharsets.UTF_8,
                         StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        logOperation(projectId, "APPEND_FILE", relativePath, 
                    "Appended " + content.length() + " chars", true);
    }

    /**
     * Delete a file
     */
    public void deleteFile(String projectId, String relativePath) throws IOException {
        Path filePath = getProjectPath(projectId).resolve(relativePath);
        
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + relativePath);
        }
        
        Files.delete(filePath);
        logOperation(projectId, "DELETE_FILE", relativePath, "File deleted", true);
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String projectId, String relativePath) {
        Path filePath = getProjectPath(projectId).resolve(relativePath);
        return Files.exists(filePath);
    }

    /**
     * Create a directory
     */
    public void createDirectory(String projectId, String relativePath) throws IOException {
        Path dirPath = getProjectPath(projectId).resolve(relativePath);
        Files.createDirectories(dirPath);
        logOperation(projectId, "CREATE_DIR", relativePath, "Directory created", true);
    }

    /**
     * List all files in a directory recursively
     */
    public List<String> listFiles(String projectId, String relativePath) throws IOException {
        Path dirPath = getProjectPath(projectId).resolve(relativePath);
        
        if (!Files.exists(dirPath)) {
            return new ArrayList<>();
        }
        
        return Files.walk(dirPath)
                .filter(Files::isRegularFile)
                .map(p -> dirPath.relativize(p).toString())
                .collect(Collectors.toList());
    }

    /**
     * List all immediate children of a directory
     */
    public List<Map<String, String>> listDirectory(String projectId, String relativePath) throws IOException {
        Path dirPath = getProjectPath(projectId).resolve(relativePath);
        List<Map<String, String>> items = new ArrayList<>();
        
        if (!Files.exists(dirPath)) {
            return items;
        }
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path path : stream) {
                Map<String, String> item = new HashMap<>();
                item.put("name", path.getFileName().toString());
                item.put("type", Files.isDirectory(path) ? "directory" : "file");
                item.put("size", String.valueOf(Files.size(path)));
                items.add(item);
            }
        }
        
        return items;
    }

    /**
     * Get total size of a project
     */
    public long getProjectSize(String projectId) throws IOException {
        Path projectPath = getProjectPath(projectId);
        
        if (!Files.exists(projectPath)) {
            return 0;
        }
        
        return Files.walk(projectPath)
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
    }

    /**
     * Get file count in project
     */
    public int getFileCount(String projectId) throws IOException {
        Path projectPath = getProjectPath(projectId);
        
        if (!Files.exists(projectPath)) {
            return 0;
        }
        
        return (int) Files.walk(projectPath)
                .filter(Files::isRegularFile)
                .count();
    }

    /**
     * Copy file from source to destination within project
     */
    public void copyFile(String projectId, String sourcePath, String destPath) throws IOException {
        Path source = getProjectPath(projectId).resolve(sourcePath);
        Path dest = getProjectPath(projectId).resolve(destPath);
        
        if (!Files.exists(source)) {
            throw new FileNotFoundException("Source not found: " + sourcePath);
        }
        
        Files.createDirectories(dest.getParent());
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        logOperation(projectId, "COPY_FILE", sourcePath + " -> " + destPath, "File copied", true);
    }

    /**
     * Search for files containing a pattern
     */
    public List<Map<String, Object>> searchFiles(String projectId, String pattern) throws IOException {
        List<Map<String, Object>> results = new ArrayList<>();
        Path projectPath = getProjectPath(projectId);
        
        if (!Files.exists(projectPath)) {
            return results;
        }
        
        Files.walk(projectPath)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        if (content.contains(pattern)) {
                            Map<String, Object> result = new HashMap<>();
                            result.put("file", projectPath.relativize(path).toString());
                            result.put("matches", content.split(pattern).length - 1);
                            results.add(result);
                        }
                    } catch (IOException ignored) {
                    }
                });
        
        return results;
    }

    // ============================================================================
    // EXECUTION LOGGING
    // ============================================================================

    /**
     * Log a file operation to project execution log
     */
    private void logOperation(String projectId, String action, String target, String details, boolean success) {
        try {
            // Get or create log for this project
            ArrayNode log = executionLogs.computeIfAbsent(projectId, k -> mapper.createArrayNode());
            
            ObjectNode operation = mapper.createObjectNode();
            operation.put("timestamp", LocalDateTime.now().format(formatter));
            operation.put("action", action);
            operation.put("target", target);
            operation.put("details", details);
            operation.put("success", success);
            log.add(operation);
            
            // Save to disk
            saveExecutionLog(projectId);
            
            // Log to console
            String status = success ? "✅" : "❌";
            System.out.println(String.format("%s [%s] %s: %s", status, action, target, details));
        } catch (Exception e) {
            System.err.println("❌ Logging failed: " + e.getMessage());
        }
    }

    /**
     * Save execution log to disk
     */
    private void saveExecutionLog(String projectId) throws IOException {
        ArrayNode log = executionLogs.get(projectId);
        if (log == null) {
            log = mapper.createArrayNode();
        }
        
        Path logPath = getProjectPath(projectId).resolve("execution_log.json");
        Files.createDirectories(logPath.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(logPath.toFile(), log);
    }

    /**
     * Get execution log for a project
     */
    public List<Map<String, Object>> getExecutionLog(String projectId) throws IOException {
        Path logPath = getProjectPath(projectId).resolve("execution_log.json");
        List<Map<String, Object>> log = new ArrayList<>();
        
        if (Files.exists(logPath)) {
            ArrayNode logArray = (ArrayNode) mapper.readTree(logPath.toFile());
            logArray.forEach(op -> {
                Map<String, Object> entry = mapper.convertValue(op, Map.class);
                log.add(entry);
            });
        }
        
        return log;
    }

    /**
     * Clear execution log
     */
    public void clearExecutionLog(String projectId) throws IOException {
        executionLogs.put(projectId, mapper.createArrayNode());
        saveExecutionLog(projectId);
        logOperation(projectId, "CLEAR_LOG", "execution_log.json", "Log cleared", true);
    }

    /**
     * Get operation count by type
     */
    public Map<String, Integer> getOperationStats(String projectId) throws IOException {
        List<Map<String, Object>> log = getExecutionLog(projectId);
        Map<String, Integer> stats = new HashMap<>();
        
        for (Map<String, Object> op : log) {
            String action = (String) op.get("action");
            stats.put(action, stats.getOrDefault(action, 0) + 1);
        }
        
        return stats;
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private Path getProjectPath(String projectId) {
        return baseProjectsPath.resolve(projectId);
    }

    /**
     * Return the absolute path of the project folder on disk.
     * Used by controllers that need to copy or push generated files.
     */
    public Path getProjectDirectory(String projectId) {
        return baseProjectsPath.resolve(projectId);
    }
}
