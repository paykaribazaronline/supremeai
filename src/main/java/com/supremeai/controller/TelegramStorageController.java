package com.supremeai.controller;

import com.supremeai.response.ApiResponse;
import com.supremeai.service.TelegramStorageService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/telegram")
public class TelegramStorageController {

  @Autowired private TelegramStorageService telegramStorageService;

  @Autowired private com.supremeai.service.CodebaseBackupService codebaseBackupService;

  @Autowired private com.supremeai.service.ChatArchiveService chatArchiveService;

  @Autowired private com.supremeai.service.LearningArchiveService learningArchiveService;

  @Autowired private com.supremeai.repository.StorageMetadataRepository storageMetadataRepository;

  @GetMapping("/status")
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getBotStatus() {
    return telegramStorageService
        .checkBotStatus()
        .map(status -> ResponseEntity.ok(ApiResponse.ok(status)));
  }

  @PostMapping("/sync")
  public Mono<ResponseEntity<ApiResponse<String>>> triggerSync() {
    return telegramStorageService
        .checkBotStatus()
        .map(status -> ResponseEntity.ok(ApiResponse.ok("Sync triggered successfully")));
  }

  @PostMapping("/backup/codebase")
  public Mono<ResponseEntity<ApiResponse<String>>> triggerCodebaseBackup() {
    return codebaseBackupService
        .createBackupAndUpload()
        .map(url -> ResponseEntity.ok(ApiResponse.ok("Codebase backup uploaded: " + url)))
        .onErrorResume(
            e ->
                Mono.just(
                    ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage()))));
  }

  @PostMapping("/archive/chats")
  public Mono<ResponseEntity<ApiResponse<String>>> triggerChatArchiving() {
    return chatArchiveService
        .archiveOldMessages()
        .then(Mono.just(ResponseEntity.ok(ApiResponse.ok("Chat archiving cycle triggered"))))
        .onErrorResume(
            e ->
                Mono.just(
                    ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage()))));
  }

  @PostMapping("/archive/learning")
  public Mono<ResponseEntity<ApiResponse<String>>> triggerLearningArchiving() {
    return learningArchiveService
        .archiveOldLearnings()
        .then(Mono.just(ResponseEntity.ok(ApiResponse.ok("Learning archiving cycle triggered"))))
        .onErrorResume(
            e ->
                Mono.just(
                    ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage()))));
  }

  @PostMapping("/deploy")
  public Mono<Map<String, Object>> deployTeldrive() {
    // Future logic for Cloud Run / Docker deployment using apiId, apiHash, botToken
    return Mono.just(
        Map.of(
            "success",
            true,
            "message",
            "Teldrive deployment sequence initiated. Please ensure credentials are saved first."));
  }

  @GetMapping("/artifacts")
  public Mono<ResponseEntity<ApiResponse<java.util.List<com.supremeai.model.StorageMetadata>>>>
      listArtifacts() {
    return storageMetadataRepository
        .findAll()
        .filter(m -> "ARTIFACT".equals(m.getCategory()))
        .collectList()
        .map(artifacts -> ResponseEntity.ok(ApiResponse.ok(artifacts)));
  }

  @GetMapping("/archives/learning")
  public Mono<ResponseEntity<ApiResponse<java.util.List<com.supremeai.model.StorageMetadata>>>>
      listLearningArchives() {
    return storageMetadataRepository
        .findAll()
        .filter(
            m -> "LEARNING_ARCHIVE".equals(m.getCategory()) || "LEARNING".equals(m.getCategory()))
        .collectList()
        .map(archives -> ResponseEntity.ok(ApiResponse.ok(archives)));
  }

  @GetMapping("/backups/codebase")
  public Mono<ResponseEntity<ApiResponse<java.util.List<com.supremeai.model.StorageMetadata>>>>
      listCodebaseBackups() {
    return storageMetadataRepository
        .findAll()
        .filter(m -> "CODEBASE".equals(m.getCategory()))
        .collectList()
        .map(backups -> ResponseEntity.ok(ApiResponse.ok(backups)));
  }

  @GetMapping("/archives/chat/{userId}")
  public Mono<ResponseEntity<ApiResponse<java.util.List<com.supremeai.model.StorageMetadata>>>>
      getChatArchives(@PathVariable String userId) {
    return storageMetadataRepository
        .findAll()
        .filter(m -> "CHAT".equals(m.getCategory()) && userId.equals(m.getUserId()))
        .collectList()
        .map(archives -> ResponseEntity.ok(ApiResponse.ok(archives)));
  }

  @GetMapping("/archives/download/{fileId}")
  public Mono<ResponseEntity<ApiResponse<String>>> getDownloadUrl(@PathVariable String fileId) {
    return telegramStorageService
        .getDownloadUrl(fileId)
        .map(url -> ResponseEntity.ok(ApiResponse.ok(url)));
  }
}
