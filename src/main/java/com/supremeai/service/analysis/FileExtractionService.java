package com.supremeai.service.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for extracting project files from ZIP uploads or Git repositories.
 */
@Service
public class FileExtractionService {

    private static final Logger log = LoggerFactory.getLogger(FileExtractionService.class);

    private static final long MAX_SIZE_BYTES = 100 * 1024 * 1024; // 100MB
    private static final int MAX_FILES = 500;
    private static final List<String> SKIP_DIRS = List.of(
        "node_modules", ".git", "dist", "build", "vendor", "target", "bin", "obj",
        "__pycache__", ".venv", "venv", ".idea", ".vscode"
    );

    /**
     * Extract files from a ZIP upload.
     */
    public List<File> extractFromZip(MultipartFile zipFile) throws IOException {
        validateFileSize(zipFile.getSize());

        String tempDir = createTempDirectory("analysis_zip_");
        log.info("Extracting ZIP to temporary directory: {}", tempDir);

        try {
            // Extract ZIP
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                new BufferedInputStream(zipFile.getInputStream()))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }

                    Path targetPath = Paths.get(tempDir, entry.getName());

                    // Security check: prevent zip slip attack
                    targetPath = targetPath.normalize();
                    if (!targetPath.startsWith(tempDir)) {
                        log.warn("Skipping suspicious ZIP entry: {}", entry.getName());
                        continue;
                    }

                    // Skip excluded directories
                    String pathStr = entry.getName();
                    if (shouldSkipPath(pathStr)) {
                        continue;
                    }

                    // Create parent directories
                    Files.createDirectories(targetPath.getParent());

                    // Extract file
                    try (OutputStream os = Files.newOutputStream(targetPath);
                         BufferedOutputStream bos = new BufferedOutputStream(os)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            bos.write(buffer, 0, len);
                        }
                    }
                }
            }

            return collectFiles(Paths.get(tempDir));
        } catch (IOException e) {
            cleanupDirectory(tempDir);
            throw e;
        }
    }

    /**
     * Checkout files from a Git repository.
     */
    public List<File> checkoutFromGit(String gitUrl, String branch) throws IOException, InterruptedException {
        String tempDir = createTempDirectory("analysis_git_");
        log.info("Cloning Git repository to temporary directory: {}", tempDir);

        try {
            // Clone repository
            List<String> cloneCmd = new ArrayList<>();
            cloneCmd.add("git");
            cloneCmd.add("clone");
            cloneCmd.add("--depth=1"); // Shallow clone for speed
            if (branch != null && !branch.isEmpty()) {
                cloneCmd.add("--branch");
                cloneCmd.add(branch);
            }
            cloneCmd.add(gitUrl);
            cloneCmd.add(tempDir);

            ProcessBuilder pb = new ProcessBuilder(cloneCmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Capture output for logging
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("Git clone output: {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Git clone failed with exit code: " + exitCode);
            }

            return collectFiles(Paths.get(tempDir));
        } catch (IOException | InterruptedException e) {
            cleanupDirectory(tempDir);
            throw e;
        }
    }

    /**
     * Delete temporary directory and all its contents.
     */
    public void cleanupDirectory(String directoryPath) {
        try {
            Path dir = Paths.get(directoryPath);
            if (Files.exists(dir)) {
                Files.walk(dir)
                    .sorted((a, b) -> b.compareTo(a)) // Delete children before parent
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            log.warn("Failed to delete temporary file: {}", p, e);
                        }
                    });
                log.info("Cleaned up temporary directory: {}", directoryPath);
            }
        } catch (IOException e) {
            log.error("Error cleaning up directory {}: {}", directoryPath, e.getMessage());
        }
    }

    /**
     * Validate file size limits.
     */
    private void validateFileSize(long size) {
        if (size > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException(
                String.format("File size exceeds maximum limit of %d MB", MAX_SIZE_BYTES / (1024 * 1024)));
        }
    }

    /**
     * Create a unique temporary directory.
     */
    private String createTempDirectory(String prefix) {
        try {
            return Files.createTempDirectory(prefix).toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary directory", e);
        }
    }

    /**
     * Collect source files from directory, respecting limits.
     */
    private List<File> collectFiles(Path rootDir) throws IOException {
        List<File> files = new ArrayList<>();
        List<String> supportedExtensions = List.of(
            ".java", ".js", ".ts", ".tsx", ".jsx", ".py", ".go", ".rb", ".php", ".cs",
            ".c", ".cpp", ".h", ".hpp", ".scala", ".kt", ".swift", ".m", ".mm"
        );

        Files.walk(rootDir)
            .filter(Files::isRegularFile)
            .filter(path -> {
                String fileName = path.getFileName().toString();
                // Skip hidden files
                if (fileName.startsWith(".")) {
                    return false;
                }
                // Check extension
                String ext = getFileExtension(fileName);
                return supportedExtensions.contains(ext);
            })
            .filter(path -> !shouldSkipPath(path.toString()))
            .limit(MAX_FILES)
            .forEach(path -> files.add(path.toFile()));

        log.info("Collected {} source files for analysis", files.size());
        if (files.size() >= MAX_FILES) {
            log.warn("Maximum file limit ({} ) reached. Additional files will be skipped.", MAX_FILES);
        }

        return files;
    }

    /**
     * Check if path should be skipped based on directory patterns.
     */
    private boolean shouldSkipPath(String path) {
        String normalized = path.replace('\\', '/');
        for (String skipDir : SKIP_DIRS) {
            if (normalized.contains("/" + skipDir + "/") || normalized.startsWith(skipDir + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get file extension from filename.
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return (lastDot == -1) ? "" : filename.substring(lastDot).toLowerCase();
    }
}
