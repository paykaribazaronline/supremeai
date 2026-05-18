package com.supremeai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import com.supremeai.repository.StorageMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LearningArchiveService {

    private static final Logger logger = LoggerFactory.getLogger(LearningArchiveService.class);
    private final SystemLearningRepository repository;
    private final TelegramStorageService telegramStorageService;
    private final StorageMetadataRepository metadataRepository;
    private final ConfigService configService;
    private final ObjectMapper objectMapper;
    private LocalDateTime lastArchiveRun = LocalDateTime.now().minusHours(12);
    private LocalDateTime lastSyncRun = LocalDateTime.now().minusHours(12);
    private boolean isArchiving = false;
    private boolean isSyncing = false;

    public LearningArchiveService(SystemLearningRepository repository,
                                  TelegramStorageService telegramStorageService,
                                  StorageMetadataRepository metadataRepository,
                                  ObjectMapper objectMapper,
                                  ConfigService configService) {
        this.repository = repository;
        this.telegramStorageService = telegramStorageService;
        this.metadataRepository = metadataRepository;
        this.objectMapper = objectMapper;
        this.configService = configService;
    }

    /**
     * Periodically check if it's time to archive old system learning data based on admin config.
     */
    @Scheduled(fixedDelay = 60000) // check every minute
    public void runLearningArchiving() {
        if (isArchiving) return;

        long intervalHours = configService.getEffectiveSetting("learning_archive_interval_hours", 12L);
        if (java.time.Duration.between(lastArchiveRun, LocalDateTime.now()).toHours() < intervalHours) {
            return;
        }

        isArchiving = true;
        logger.info("[TELDRIVE-ARCHIVE] Starting system learning archival process (Interval: {}h)...", intervalHours);
        archiveOldLearnings()
            .doOnTerminate(() -> {
                lastArchiveRun = LocalDateTime.now();
                isArchiving = false;
            })
            .subscribe(
                success -> logger.info("[TELDRIVE-ARCHIVE] Archival check completed successfully"),
                error -> logger.error("[TELDRIVE-ARCHIVE] Error during archival process: {}", error.getMessage(), error)
            );
    }

    public Mono<Void> archiveOldLearnings() {
        // Find learnings older than X days (default 7) or if count > threshold (default 100)
        long retentionDays = configService.getEffectiveSetting("learning_hot_retention_days", 7L);
        long hotLimit = configService.getEffectiveSetting("learning_hot_limit_count", 100L);

        return repository.findAll()
                .collectList()
                .flatMap(allLearnings -> {
                    if (allLearnings.size() <= hotLimit) {
                        return Mono.empty(); // Keep at least hotLimit records in hot storage
                    }

                    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
                    
                    List<SystemLearning> toArchive = allLearnings.stream()
                            .filter(l -> {
                                if (l.getLearnedAt() == null) return true;
                                return l.getLearnedAt().isBefore(cutoffDate);
                            })
                            .limit(Math.max(0, allLearnings.size() - hotLimit))
                            .collect(Collectors.toList());

                    if (toArchive.isEmpty()) {
                        return Mono.empty();
                    }

                    return archiveBatch(toArchive);
                });
    }

    private Mono<Void> archiveBatch(List<SystemLearning> learnings) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
            String fileName = "learning_archive_" + timestamp + ".json";
            byte[] jsonData = objectMapper.writeValueAsBytes(learnings);
            String remotePath = "/supremeai/archives/learning";

            logger.info("[TELDRIVE-ARCHIVE] Archiving {} learning records to Teldrive Cloud", learnings.size());

            return telegramStorageService.uploadData(jsonData, fileName, remotePath)
                    .flatMap(url -> {
                        logger.info("[TELDRIVE-ARCHIVE] Successfully archived learning batch to: {}", url);
                        
                        // Save metadata to Firestore
                        com.supremeai.model.StorageMetadata metadata = new com.supremeai.model.StorageMetadata();
                        metadata.setFileName(fileName);
                        metadata.setRemotePath(remotePath + "/" + fileName);
                        metadata.setCategory("LEARNING_ARCHIVE");
                        metadata.setStorageProvider("TELDRIVE");
                        metadata.setDownloadUrl(url);
                        metadata.setSize((long) jsonData.length);
                        metadata.setContentType("application/json");
                        metadata.setCreatedAt(new Date());

                        return metadataRepository.save(metadata)
                                .thenMany(Flux.fromIterable(learnings))
                                .flatMap(l -> repository.deleteById(l.getId()))
                                .then();
                    })
                    .onErrorResume(e -> {
                        logger.error("[TELDRIVE-ARCHIVE] Failed to archive learning records: {}", e.getMessage());
                        return Mono.empty();
                    });
        } catch (Exception e) {
            logger.error("[LEARN-ARCHIVE] Serialization error: {}", e.getMessage());
            return Mono.error(e);
        }
    }

    /**
     * Periodically check if it's time to sync back data from Telegram to Firebase based on admin config.
     */
    @Scheduled(fixedDelay = 60000) // check every minute
    public void runLearningSync() {
        if (isSyncing) return;

        long intervalHours = configService.getEffectiveSetting("learning_sync_interval_hours", 12L);
        if (java.time.Duration.between(lastSyncRun, LocalDateTime.now()).toHours() < intervalHours) {
            return;
        }

        isSyncing = true;
        logger.info("[TELDRIVE-SYNC] Starting system learning sync from Teldrive (Interval: {}h)...", intervalHours);
        syncFromTelegram()
            .doOnTerminate(() -> {
                lastSyncRun = LocalDateTime.now();
                isSyncing = false;
            })
            .subscribe(
                success -> logger.info("[TELDRIVE-SYNC] Sync from Teldrive completed"),
                error -> logger.error("[TELDRIVE-SYNC] Sync from Teldrive failed: {}", error.getMessage())
            );
    }

    public Mono<Void> syncFromTelegram() {
        return metadataRepository.findByCategory("LEARNING_ARCHIVE")
                .flatMap(metadata -> {
                    logger.info("[TELDRIVE-SYNC] Syncing from archive file: {}", metadata.getFileName());
                    return telegramStorageService.downloadData(metadata.getRemotePath())
                            .flatMap(data -> {
                                try {
                                    List<SystemLearning> learnings = objectMapper.readValue(data,
                                            objectMapper.getTypeFactory().constructCollectionType(List.class, SystemLearning.class));
                                    
                                    logger.info("[TELDRIVE-SYNC] Restoring {} records from {}", learnings.size(), metadata.getFileName());
                                    
                                    return Flux.fromIterable(learnings)
                                            .flatMap(l -> repository.findById(l.getId())
                                                    .switchIfEmpty(repository.save(l)))
                                            .then();
                                } catch (Exception e) {
                                    logger.error("[LEARN-SYNC] Failed to parse archive {}: {}", metadata.getFileName(), e.getMessage());
                                    return Mono.empty();
                                }
                            });
                })
                .then();
    }
}
