package com.supremeai.service.analysis;

import jakarta.annotation.PreDestroy;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileExtractionService {

  private static final Logger log = LoggerFactory.getLogger(FileExtractionService.class);

  private static final long MAX_SIZE_BYTES = 100 * 1024 * 1024; // 100MB compressed upload
  private static final long MAX_ENTRY_UNCOMPRESSED_BYTES = 200 * 1024 * 1024; // 200MB per entry
  private static final long MAX_TOTAL_UNCOMPRESSED_BYTES = 500 * 1024 * 1024; // 500MB total
  private static final int MAX_FILES = 500;
  private static final Duration GIT_CLONE_TIMEOUT = Duration.ofMinutes(5);
  private static final Set<String> ALLOWED_GIT_SCHEMES = Set.of("https");
  private static final List<String> SKIP_DIRS =
      List.of(
          "node_modules",
          ".git",
          "dist",
          "build",
          "vendor",
          "target",
          "bin",
          "obj",
          "__pycache__",
          ".venv",
          "venv",
          ".idea",
          ".vscode");

  private final Set<Path> tempDirectories = ConcurrentHashMap.newKeySet();

  @PreDestroy
  public void cleanupAllTempDirectories() {
    log.info(
        "[FileExtractionService] Running @PreDestroy cleanup of {} temp directories",
        tempDirectories.size());
    for (Path dir : tempDirectories) {
      try {
        cleanupDirectory(dir.toString());
      } catch (Exception e) {
        log.warn("[FileExtractionService] Failed to cleanup temp dir {}: {}", dir, e.getMessage());
      }
    }
    tempDirectories.clear();
  }

  /** Extract files from a ZIP upload. */
  public List<File> extractFromZip(MultipartFile zipFile) throws IOException {
    validateFileSize(zipFile.getSize());

    String tempDir = createTempDirectory("analysis_zip_");
    log.info("Extracting ZIP to temporary directory: {}", tempDir);

    long totalUncompressed = 0;

    try {
      try (java.util.zip.ZipInputStream zis =
          new java.util.zip.ZipInputStream(new BufferedInputStream(zipFile.getInputStream()))) {
        java.util.zip.ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
          if (entry.isDirectory()) {
            continue;
          }

          long uncompressedSize = entry.getSize();
          if (uncompressedSize > MAX_ENTRY_UNCOMPRESSED_BYTES) {
            throw new IOException(
                String.format(
                    "ZIP entry '%s' uncompressed size %d bytes exceeds per-entry limit of %d bytes",
                    entry.getName(), uncompressedSize, MAX_ENTRY_UNCOMPRESSED_BYTES));
          }

          totalUncompressed += uncompressedSize;
          if (totalUncompressed > MAX_TOTAL_UNCOMPRESSED_BYTES) {
            throw new IOException(
                String.format(
                    "Total uncompressed ZIP size %d bytes exceeds budget of %d bytes. Extraction aborted to prevent disk fill.",
                    totalUncompressed, MAX_TOTAL_UNCOMPRESSED_BYTES));
          }

          Path targetPath = Paths.get(tempDir, entry.getName());

          targetPath = targetPath.normalize();
          if (!targetPath.startsWith(tempDir)) {
            log.warn("Skipping suspicious ZIP entry: {}", entry.getName());
            continue;
          }

          String pathStr = entry.getName();
          if (shouldSkipPath(pathStr)) {
            continue;
          }

          Files.createDirectories(targetPath.getParent());

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

      List<File> result = collectFiles(Paths.get(tempDir));
      tempDirectories.remove(Paths.get(tempDir));
      return result;
    } catch (IOException e) {
      cleanupDirectory(tempDir);
      throw e;
    }
  }

  /** Checkout files from a Git repository. */
  public List<File> checkoutFromGit(String gitUrl, String branch)
      throws IOException, InterruptedException {
    validateGitUrl(gitUrl);

    String tempDir = createTempDirectory("analysis_git_");
    log.info("Cloning Git repository to temporary directory: {}", tempDir);

    try {
      List<String> cloneCmd = new ArrayList<>();
      cloneCmd.add("git");
      cloneCmd.add("clone");
      cloneCmd.add("--depth=1");
      if (branch != null && !branch.isEmpty()) {
        cloneCmd.add("--branch");
        cloneCmd.add(branch);
      }
      cloneCmd.add(gitUrl);
      cloneCmd.add(tempDir);

      ProcessBuilder pb = new ProcessBuilder(cloneCmd);
      pb.redirectErrorStream(true);
      Process process = pb.start();

      boolean finished =
          process.waitFor(GIT_CLONE_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
      if (!finished) {
        process.destroyForcibly();
        throw new IOException(
            "Git clone timed out after " + GIT_CLONE_TIMEOUT + " for URL: " + gitUrl);
      }

      int exitCode = process.exitValue();
      if (exitCode != 0) {
        throw new IOException(
            "Git clone failed with exit code: " + exitCode + " for URL: " + gitUrl);
      }

      List<File> result = collectFiles(Paths.get(tempDir));
      tempDirectories.remove(Paths.get(tempDir));
      return result;
    } catch (IOException | InterruptedException e) {
      cleanupDirectory(tempDir);
      throw e;
    }
  }

  private void validateGitUrl(String gitUrl) {
    if (gitUrl == null || gitUrl.isBlank()) {
      throw new IllegalArgumentException("Git URL must not be empty");
    }

    URI uri;
    try {
      uri = new URI(gitUrl);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid Git URL: " + gitUrl, e);
    }

    String scheme = uri.getScheme();
    if (scheme == null || !ALLOWED_GIT_SCHEMES.contains(scheme.toLowerCase())) {
      throw new IllegalArgumentException("Only HTTPS Git URLs are allowed. Got scheme: " + scheme);
    }

    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      throw new IllegalArgumentException("Git URL must contain a valid host");
    }

    List<String> blockedHosts =
        List.of(
            "169.254.169.254",
            "metadata.google.internal",
            "metadata.internal",
            "metadata",
            "localhost",
            "127.0.0.1",
            "0.0.0.0",
            "::1");
    String lowerHost = host.toLowerCase();
    for (String blocked : blockedHosts) {
      if (lowerHost.equals(blocked) || lowerHost.endsWith("." + blocked)) {
        throw new IllegalArgumentException(
            "Git clone to internal/metadata host is blocked: " + host);
      }
    }
  }

  public void cleanupDirectory(String directoryPath) {
    try {
      Path dir = Paths.get(directoryPath);
      if (Files.exists(dir)) {
        Files.walk(dir)
            .sorted((a, b) -> b.compareTo(a))
            .forEach(
                p -> {
                  try {
                    Files.deleteIfExists(p);
                  } catch (IOException e) {
                    log.warn("Failed to delete temporary file: {}", p, e);
                  }
                });
        log.debug("Cleaned up temporary directory: {}", directoryPath);
      }
    } catch (IOException e) {
      log.error("Error cleaning up directory {}: {}", directoryPath, e.getMessage());
    }
  }

  private void validateFileSize(long size) {
    if (size > MAX_SIZE_BYTES) {
      throw new IllegalArgumentException(
          String.format(
              "File size exceeds maximum limit of %d MB", MAX_SIZE_BYTES / (1024 * 1024)));
    }
  }

  private String createTempDirectory(String prefix) {
    try {
      Path tempDir = Files.createTempDirectory(prefix);
      tempDirectories.add(tempDir);
      return tempDir.toAbsolutePath().toString();
    } catch (IOException e) {
      throw new RuntimeException("Failed to create temporary directory", e);
    }
  }

  private List<File> collectFiles(Path rootDir) throws IOException {
    List<File> files = new ArrayList<>();
    List<String> supportedExtensions =
        List.of(
            ".java", ".js", ".ts", ".tsx", ".jsx", ".py", ".go", ".rb", ".php", ".cs", ".c", ".cpp",
            ".h", ".hpp", ".scala", ".kt", ".swift", ".m", ".mm");

    Files.walk(rootDir)
        .filter(Files::isRegularFile)
        .filter(
            path -> {
              String fileName = path.getFileName().toString();
              if (fileName.startsWith(".")) {
                return false;
              }
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

  private boolean shouldSkipPath(String path) {
    String normalized = path.replace('\\', '/');
    for (String skipDir : SKIP_DIRS) {
      if (normalized.contains("/" + skipDir + "/") || normalized.startsWith(skipDir + "/")) {
        return true;
      }
    }
    return false;
  }

  private String getFileExtension(String filename) {
    int lastDot = filename.lastIndexOf('.');
    return (lastDot == -1) ? "" : filename.substring(lastDot).toLowerCase();
  }
}
