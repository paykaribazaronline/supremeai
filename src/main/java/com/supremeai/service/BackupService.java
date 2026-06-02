package com.supremeai.service;

import com.supremeai.model.SystemConfig;
import com.supremeai.service.TelegramStorageService;
import com.supremeai.service.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * BackupService - Handles automated system backups to Telegram (Cold Storage).
 */
@Service
public class BackupService {
    public BackupService(TelegramStorageService telegramStorageService, ConfigService configService) {
        this.telegramStorageService = telegramStorageService;
        this.configService = configService;
    }


    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);



    /**
     * Scheduled task to backup the codebase daily at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void scheduleDailyBackup() {
        logger.info("[BACKUP] Starting automated daily codebase backup...");
        performCodebaseBackup().subscribe(
            url -> logger.info("[BACKUP] Automated backup completed successfully: {}", url),
            error -> logger.error("[BACKUP] Automated backup failed: {}", error.getMessage())
        );
    }

    /**
     * Performs a full codebase backup and uploads it to Telegram.
     */
    public Mono<String> performCodebaseBackup() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String zipFileName = "supremeai_backup_" + timestamp + ".zip";
        Path sourcePath = Paths.get(System.getProperty("user.dir"));
        Path zipPath = Paths.get(System.getProperty("java.io.tmpdir"), zipFileName);

        return Mono.fromCallable(() -> {
            createZipArchive(sourcePath, zipPath);
            return zipPath.toFile();
        })
        .flatMap(file -> telegramStorageService.uploadFile(file, "/backups/codebase/" + timestamp)
            .doFinally(signal -> {
                try {
                    Files.deleteIfExists(zipPath);
                } catch (IOException e) {
                    logger.warn("[BACKUP] Failed to delete temporary backup file: {}", e.getMessage());
                }
            })
        )
        .onErrorResume(e -> {
            logger.error("[BACKUP] Backup process failed: {}", e.getMessage());
            return Mono.error(e);
        });
    }

    private void createZipArchive(Path sourceDir, Path zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    // Skip hidden directories like .git, .gradle, etc.
                    String dirName = dir.getFileName().toString();
                    if (dirName.startsWith(".") || dirName.equals("node_modules") || dirName.equals("build") || dirName.equals("target")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String relativePath = sourceDir.relativize(file).toString();
                    zos.putNextEntry(new ZipEntry(relativePath));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
