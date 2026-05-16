package com.supremeai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.model.ChatMessage;
import com.supremeai.repository.ChatHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatArchiveService {

    private static final Logger logger = LoggerFactory.getLogger(ChatArchiveService.class);
    private final ChatHistoryRepository chatHistoryRepository;
    private final TelegramStorageService telegramStorageService;
    private final com.supremeai.repository.StorageMetadataRepository storageMetadataRepository;
    private final ObjectMapper objectMapper;

    public ChatArchiveService(ChatHistoryRepository chatHistoryRepository,
                              TelegramStorageService telegramStorageService,
                              com.supremeai.repository.StorageMetadataRepository storageMetadataRepository,
                              ObjectMapper objectMapper) {
        this.chatHistoryRepository = chatHistoryRepository;
        this.telegramStorageService = telegramStorageService;
        this.storageMetadataRepository = storageMetadataRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Every 6 hours, check for old messages and archive them to Telegram.
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void runAutoArchiving() {
        logger.info("[ARCHIVE] Starting automatic chat archiving process...");
        archiveOldMessages().subscribe();
    }

    public Mono<Void> archiveOldMessages() {
        // In a real scenario, we'd query for unique userIds. 
        // For now, we fetch all messages and group by userId.
        return chatHistoryRepository.findAll()
                .collectList()
                .flatMap(allMessages -> {
                    Map<String, List<ChatMessage>> groupedByUserId = allMessages.stream()
                            .filter(m -> m.getUserId() != null)
                            .collect(Collectors.groupingBy(ChatMessage::getUserId));

                    return Flux.fromIterable(groupedByUserId.entrySet())
                            .flatMap(entry -> {
                                String userId = entry.getKey();
                                List<ChatMessage> userMessages = entry.getValue();

                                if (userMessages.size() > 50) {
                                    // Sort by timestamp and take the oldest messages to archive
                                    List<ChatMessage> toArchive = userMessages.stream()
                                            .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                                            .limit(userMessages.size() - 20) // Keep at least 20 in Hot Storage
                                            .collect(Collectors.toList());

                                    return archiveMessagesForUser(userId, toArchive);
                                }
                                return Mono.empty();
                            })
                            .then();
                });
    }

    private Mono<Void> archiveMessagesForUser(String userId, List<ChatMessage> messages) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
            String fileName = "archive_" + userId + "_" + timestamp + ".json";
            byte[] jsonData = objectMapper.writeValueAsBytes(messages);
            String remotePath = "/supremeai/archives/chats/" + userId;
            long fileSize = jsonData.length;

            logger.info("[ARCHIVE] Archiving {} messages for user {} to Telegram", messages.size(), userId);

            return telegramStorageService.uploadData(jsonData, fileName, remotePath)
                    .flatMap(url -> {
                        logger.info("[ARCHIVE] Successfully archived to: {}", url);
                        
                        // Create metadata in Firestore
                        com.supremeai.model.StorageMetadata metadata = new com.supremeai.model.StorageMetadata(
                            fileName, remotePath, "TELEGRAM", "CHAT", fileSize
                        );
                        metadata.setUserId(userId);
                        metadata.setDownloadUrl(url);
                        metadata.setContentType("application/json");

                        return storageMetadataRepository.save(metadata)
                                .then(Flux.fromIterable(messages)
                                        .flatMap(chatHistoryRepository::delete)
                                        .then());
                    })
                    .onErrorResume(e -> {
                        logger.error("[ARCHIVE] Failed to archive for user {}: {}", userId, e.getMessage());
                        return Mono.empty();
                    });
        } catch (Exception e) {
            logger.error("[ARCHIVE] JSON Serialization error: {}", e.getMessage());
            return Mono.error(e);
        }
    }
}
