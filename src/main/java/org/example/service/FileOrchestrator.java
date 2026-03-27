package org.example.service;

import java.io.IOException;
import java.nio.file.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileOrchestrator {
    private final Path baseProjectsPath;
    private final MemoryManager memoryManager;

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
            System.err.println("Failed to create base projects directory: " + e.getMessage());
        }
    }

    /**
     * Creates a new project directory structure.
     */
    public Path createProjectStructure(String projectId) throws IOException {
        Path projectPath = baseProjectsPath.resolve(projectId);
        Files.createDirectories(projectPath);
        logExecution(projectId, "CREATE_DIR", projectPath.toString(), "Project root created");
        return projectPath;
    }

    /**
     * Writes content to a specific file within a project.
     */
    public void writeFile(String projectId, String relativePath, String content) throws IOException {
        Path filePath = baseProjectsPath.resolve(projectId).resolve(relativePath);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        logExecution(projectId, "WRITE_FILE", relativePath, "File written successfully");
    }

    /**
     * Logs the execution to both MemoryManager and a local JSON file for the project.
     */
    private void logExecution(String projectId, String action, String target, String details) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        System.out.println(String.format("[%s] %s: %s (%s)", timestamp, action, target, details));
        
        // Record in global memory for AI learning
        memoryManager.recordSuccess("FILE_ORCHESTRATION", action + "_" + target, 0);
        
        // In the future, this will write to a project-specific execution_log.json
    }
}
