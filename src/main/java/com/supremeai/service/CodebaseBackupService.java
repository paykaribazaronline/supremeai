package com.supremeai.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CodebaseBackupService {

  private static final Logger logger = LoggerFactory.getLogger(CodebaseBackupService.class);
  private final TelegramStorageService telegramStorageService;
  private final com.supremeai.repository.StorageMetadataRepository storageMetadataRepository;

  @Value("${supremeai.backup.project-root:#{systemProperties['user.dir']}}") // হার্ডকোডেড পাথ সরিয়ে বর্তমান ডিরেক্টরি ডিফল্ট করা হয়েছে।
  private String projectRoot;

  @Value("${supremeai.backup.excluded-dirs:node_modules,.gradle,build,.git,.gemini,out,target,.idea,.vscode}") 
  private List<String> excludedDirs; // বাদ দেওয়া ডিরেক্টরিগুলো এখন কনফিগারযোগ্য।

  public CodebaseBackupService(
      TelegramStorageService telegramStorageService,
      com.supremeai.repository.StorageMetadataRepository storageMetadataRepository) {
    this.telegramStorageService = telegramStorageService;
    this.storageMetadataRepository = storageMetadataRepository;
  }

  /** Scheduled backup every day at 3 AM. */
  @Scheduled(cron = "0 0 3 * * *")
  public void runAutoBackup() {
    logger.info("[BACKUP] Starting scheduled daily codebase backup...");
    createBackupAndUpload().subscribe();
  }

  public Mono<String> createBackupAndUpload() {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
    String zipFileName = "supremeai_backup_" + timestamp + ".zip";
    Path tempZipPath = Paths.get(System.getProperty("java.io.tmpdir"), zipFileName);

    return Mono.fromCallable(
            () -> {
              zipDirectory(Paths.get(projectRoot), tempZipPath);
              return tempZipPath.toFile();
            })
        .flatMap(
            zipFile -> {
              String remotePath = "/supremeai/backups/codebase";
              long fileSize = zipFile.length();
              return telegramStorageService
                  .uploadFile(zipFile, remotePath)
                  .flatMap(
                      url -> {
                        // Create metadata in Firestore
                        com.supremeai.model.StorageMetadata metadata =
                            new com.supremeai.model.StorageMetadata(
                                zipFileName, remotePath, "TELEGRAM", "CODEBASE", fileSize);
                        metadata.setDownloadUrl(url);
                        metadata.setContentType("application/zip");
                        return storageMetadataRepository.save(metadata).thenReturn(url);
                      })
                  .doFinally(
                      signal -> {
                        // Clean up temp file
                        try {
                          Files.deleteIfExists(tempZipPath);
                          logger.info("[BACKUP] Temporary zip file deleted: {}", tempZipPath);
                        } catch (IOException e) {
                          logger.warn("[BACKUP] Failed to delete temp zip: {}", e.getMessage());
                        }
                      });
            })
        .onErrorResume(
            e -> {
              logger.error("[BACKUP] Backup failed: {}", e.getMessage());
              return Mono.error(e);
            });
  }

  private void zipDirectory(Path sourcePath, Path zipPath) throws IOException {
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
      Files.walkFileTree(
          sourcePath,
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
              if (excludedDirs.contains(dir.getFileName().toString())) { // কনফিগারেবল লিস্ট অনুযায়ী ডিরেক্টরি স্কিপ করা হচ্ছে।
                return FileVisitResult.SKIP_SUBTREE;
              }
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
              String relativePath = sourcePath.relativize(file).toString();
              zos.putNextEntry(new ZipEntry(relativePath));
              try (FileInputStream fis = new FileInputStream(file.toFile())) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) >= 0) {
                  zos.write(buffer, 0, length);
                }
              }
              zos.closeEntry();
              return FileVisitResult.CONTINUE;
            }
          });
    }
  }
}
