package com.supremeai.service;

import com.supremeai.model.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.Instant;
import java.util.Map;

@Service
public class TelegramStorageService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramStorageService.class);

    @Autowired
    private ConfigService configService;

    @Autowired
    private com.google.cloud.firestore.Firestore firestore;

    private final WebClient.Builder webClientBuilder;

    public TelegramStorageService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * Helper to fetch teldrive settings from the unified ConfigService.
     */
    private Map<String, Object> getTeldriveSettings() {
        SystemConfig config = configService.getConfig();
        if (config == null || config.getTelegramConfig() == null) {
            logger.warn("[TELEGRAM] Telegram config not found in global settings, falling back to empty map");
            return new java.util.HashMap<>();
        }
        
        Map<String, Object> telegram = new java.util.HashMap<>(config.getTelegramConfig());
        // For compatibility with existing code that expects a nested 'telegram' object
        // and top-level 'teldriveUrl', we restructure it slightly
        Map<String, Object> legacySettings = new java.util.HashMap<>();
        legacySettings.put("teldriveUrl", telegram.getOrDefault("teldriveUrl", "http://localhost:8080"));
        legacySettings.put("storageUsed", telegram.getOrDefault("storageUsed", "0 B"));
        legacySettings.put("telegram", telegram);
        
        return legacySettings;
    }

    /**
     * Check the status of the Teldrive bot and update system config.
     */
    public Mono<Map<String, Object>> checkBotStatus() {
        Map<String, Object> teldriveSettings = getTeldriveSettings();
        if (teldriveSettings == null || teldriveSettings.isEmpty()) {
            return Mono.just(Map.of("status", "DISCONNECTED", "message", "Teldrive settings not found in Firestore"));
        }

        Map<String, Object> telegram = (Map<String, Object>) teldriveSettings.get("telegram");
        String url = (String) teldriveSettings.getOrDefault("teldriveUrl", "http://localhost:8080");
        String token = telegram != null ? (String) telegram.getOrDefault("botToken", "") : "";

        if (token.isEmpty()) {
            return Mono.just(Map.of("status", "DISCONNECTED", "message", "API Token missing"));
        }

        return webClientBuilder.build()
                .get()
                .uri(url + "/api/v1/status")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Map<String, Object> typedResponse = (Map<String, Object>) response;
                    typedResponse.put("status", "CONNECTED");
                    typedResponse.put("lastSync", Instant.now().toString());
                    typedResponse.put("bot_name", typedResponse.getOrDefault("username", "TeldriveBot"));
                    return typedResponse;
                })
                .onErrorResume(e -> {
                    logger.error("[TELEGRAM] Failed to connect to Teldrive: {}", e.getMessage());
                    return Mono.just(Map.of("status", "ERROR", "message", e.getMessage()));
                });
    }

    /**
     * Check if Telegram storage is enabled.
     */
    public boolean isEnabled() {
        SystemConfig config = configService.getConfig();
        if (config == null || config.getTelegramConfig() == null) return false;
        Object enabled = config.getTelegramConfig().get("enabled");
        return enabled != null && (boolean) enabled;
    }

    /**
     * Upload a physical file to Telegram via Teldrive.
     */
    public Mono<String> uploadFile(File file, String remotePath) {
        if (!isEnabled()) {
            logger.warn("[TELEGRAM] Upload skipped: Telegram storage is disabled in global settings");
            return Mono.error(new RuntimeException("Telegram storage is disabled"));
        }
        
        Map<String, Object> teldriveSettings = getTeldriveSettings();
        if (teldriveSettings == null) return Mono.error(new RuntimeException("Settings not found"));
        
        Map<String, Object> telegram = (Map<String, Object>) teldriveSettings.get("telegram");
        String url = (String) teldriveSettings.getOrDefault("teldriveUrl", "http://localhost:8080");
        String token = telegram != null ? (String) telegram.getOrDefault("botToken", "") : "";
        String channelId = telegram != null ? (String) telegram.getOrDefault("channelId", "") : "";

        if (token.isEmpty()) {
            logger.error("[TELEGRAM] Cannot upload: botToken is empty in Firestore config");
            return Mono.error(new RuntimeException("Telegram token is missing"));
        }

        logger.info("[TELEGRAM] Attempting upload to: {} (Channel: {})", url, channelId);
        logger.info("[TELEGRAM] Uploading file: {} ({} bytes) to path: {}", file.getName(), file.length(), remotePath);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new FileSystemResource(file));
        builder.part("path", remotePath + "/" + file.getName());
        if (!channelId.isEmpty()) {
            builder.part("channelId", channelId);
        }

        return webClientBuilder.build()
                .post()
                .uri(url + "/api/v1/upload")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    String fileUrl = (String) response.get("url");
                    logger.info("[TELEGRAM] Successfully uploaded file: {}", file.getName());
                    return fileUrl != null ? fileUrl : "UPLOAD_SUCCESS";
                })
                .onErrorResume(e -> {
                    logger.error("[TELEGRAM] File upload failed to {}: {}", url, e.getMessage());
                    logger.error("[TELEGRAM] Check if Teldrive service is running and token/channelId are correct in Firebase");
                    return Mono.error(new RuntimeException("Teldrive upload failed: " + e.getMessage()));
                });
    }

    /**
     * Upload raw data (e.g. JSON archives) to Telegram via Teldrive.
     */
    public Mono<String> uploadData(byte[] data, String fileName, String remotePath) {
        if (!isEnabled()) {
            logger.warn("[TELEGRAM] Data upload skipped: Telegram storage is disabled in global settings");
            return Mono.error(new RuntimeException("Telegram storage is disabled"));
        }

        Map<String, Object> teldriveSettings = getTeldriveSettings();
        if (teldriveSettings == null) return Mono.error(new RuntimeException("Settings not found"));
        
        Map<String, Object> telegram = (Map<String, Object>) teldriveSettings.get("telegram");
        String url = (String) teldriveSettings.getOrDefault("teldriveUrl", "http://localhost:8080");
        String token = telegram != null ? (String) telegram.getOrDefault("botToken", "") : "";
        String channelId = telegram != null ? (String) telegram.getOrDefault("channelId", "") : "";

        logger.info("[TELEGRAM] Uploading data: {} ({} bytes) to path: {}", fileName, data.length, remotePath);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", data).filename(fileName);
        builder.part("path", remotePath + "/" + fileName);
        if (!channelId.isEmpty()) {
            builder.part("channelId", channelId);
        }

        return webClientBuilder.build()
                .post()
                .uri(url + "/api/v1/upload")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    String fileUrl = (String) response.get("url");
                    logger.info("[TELEGRAM] Successfully uploaded data: {}", fileName);
                    
                    // Update storage metrics in Firestore
                    updateStorageMetrics(data.length);
                    
                    return fileUrl != null ? fileUrl : "UPLOAD_SUCCESS";
                })
                .onErrorResume(e -> {
                    logger.error("[TELEGRAM] Data upload failed to {}: {}", url, e.getMessage());
                    logger.error("[TELEGRAM] Verify Teldrive Cloud Run service is active and botToken/channelId in Firebase are valid");
                    return Mono.error(new RuntimeException("Teldrive upload failed: " + e.getMessage()));
                });
    }

    /**
     * Download raw data from Telegram via Teldrive.
     */
    public Mono<byte[]> downloadData(String remotePath) {
        Map<String, Object> teldriveSettings = getTeldriveSettings();
        if (teldriveSettings == null) return Mono.error(new RuntimeException("Settings not found"));

        Map<String, Object> telegram = (Map<String, Object>) teldriveSettings.get("telegram");
        String url = (String) teldriveSettings.getOrDefault("teldriveUrl", "http://localhost:8080");
        String token = telegram != null ? (String) telegram.getOrDefault("botToken", "") : "";

        return webClientBuilder.build()
                .get()
                .uri(url + "/api/v1/download?path=" + remotePath)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(byte[].class)
                .onErrorResume(e -> {
                    logger.error("[TELEGRAM] Data download failed from {}: {}", remotePath, e.getMessage());
                    return Mono.error(new RuntimeException("Teldrive download failed: " + e.getMessage()));
                });
    }

    private void updateStorageMetrics(long newBytes) {
        try {
            SystemConfig current = configService.getConfig();
            Map<String, Object> telegram = new java.util.HashMap<>(current.getTelegramConfig());
            
            long currentSize = parseSize((String) telegram.getOrDefault("storageUsed", "0 B"));
            String newSizeFormatted = formatSize(currentSize + newBytes);
            
            telegram.put("storageUsed", newSizeFormatted);
            current.setTelegramConfig(telegram);
            
            configService.updateConfig(current, "system", "127.0.0.1")
                .subscribe(
                    saved -> logger.info("[TELEGRAM] Updated storage usage in global config to: {}", newSizeFormatted),
                    error -> logger.error("[TELEGRAM] Failed to update storage metrics in global config: {}", error.getMessage())
                );
        } catch (Exception e) {
            logger.error("[TELEGRAM] Failed to update storage metrics: {}", e.getMessage());
        }
    }

    /**
     * List files from a specific path in Teldrive.
     */
    public Mono<java.util.List<Map<String, Object>>> listFiles(String path) {
        Map<String, Object> teldriveSettings = getTeldriveSettings();
        if (teldriveSettings == null) return Mono.just(java.util.Collections.emptyList());
        
        Map<String, Object> telegram = (Map<String, Object>) teldriveSettings.get("telegram");
        String url = (String) teldriveSettings.getOrDefault("teldriveUrl", "http://localhost:8080");
        String token = telegram != null ? (String) telegram.getOrDefault("botToken", "") : "";

        if (token.isEmpty()) {
            return Mono.just(java.util.Collections.emptyList());
        }

        return webClientBuilder.build()
                .get()
                .uri(url + "/api/v1/files?path=" + path)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Object items = response.getOrDefault("items", response.getOrDefault("files", null));
                    if (items instanceof java.util.List) {
                        return (java.util.List<Map<String, Object>>) items;
                    }
                    return java.util.Collections.<Map<String, Object>>emptyList();
                })
                .onErrorResume(e -> {
                    logger.error("[TELEGRAM] Failed to list files at {}: {}", path, e.getMessage());
                    return Mono.just(java.util.Collections.emptyList());
                });
    }

    /**
     * Get a direct download/stream link for a file by its ID.
     */
    public Mono<String> getDownloadUrl(String fileId) {
        Map<String, Object> teldriveSettings = getTeldriveSettings();
        if (teldriveSettings == null) return Mono.error(new RuntimeException("Settings not found"));
        
        Map<String, Object> telegram = (Map<String, Object>) teldriveSettings.get("telegram");
        String url = (String) teldriveSettings.getOrDefault("teldriveUrl", "http://localhost:8080");
        String token = telegram != null ? (String) telegram.getOrDefault("botToken", "") : "";

        return webClientBuilder.build()
                .get()
                .uri(url + "/api/v1/files/" + fileId + "/url")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("url"))
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to get download URL")));
    }

    private long parseSize(String sizeStr) {
        try {
            if (sizeStr == null || sizeStr.isEmpty() || sizeStr.equals("0 B")) return 0L;
            String[] parts = sizeStr.split(" ");
            long value = Long.parseLong(parts[0]);
            String unit = parts[1].toUpperCase();
            switch (unit) {
                case "KB": return value * 1024;
                case "MB": return value * 1024 * 1024;
                case "GB": return value * 1024 * 1024 * 1024;
                default: return value;
            }
        } catch (Exception e) {
            return 0L;
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)) + " MB";
        return (bytes / (1024 * 1024 * 1024)) + " GB";
    }
}
