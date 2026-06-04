package com.supremeai.service;

import com.supremeai.model.SystemConfig;
import com.supremeai.resilience.RetryableAIExecutor;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.netty.channel.ChannelOption;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

@Service
public class TelegramStorageService {

  private static final Logger logger = LoggerFactory.getLogger(TelegramStorageService.class);

  private static final long MAX_UPLOAD_SIZE_BYTES = 100L * 1024 * 1024; // 100MB
  private static final String TELDRIVE_BOT_TOKEN =
      System.getenv()
          .getOrDefault("TELDRIVE_BOT_TOKEN", System.getenv().getOrDefault("TG_BOT_TOKEN", ""));
  private static final String TELDRIVE_CHANNEL_ID =
      System.getenv()
          .getOrDefault("TELDRIVE_CHANNEL_ID", System.getenv().getOrDefault("TG_CHANNEL_ID", ""));
  private static final String TELDRIVE_URL =
      System.getenv().getOrDefault("TELDRIVE_URL", "https://teldrive-lhlwyikwlq-uc.a.run.app");
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration READ_TIMEOUT = Duration.ofMinutes(5);
  private static final Duration METRIC_UPDATE_DEBOUNCE = Duration.ofSeconds(30);

  @Autowired private ConfigService configService;

  @Autowired private com.google.cloud.firestore.Firestore firestore;

  @Autowired private RetryableAIExecutor retryExecutor;

  @Value("${secret.manager.enabled:false}")
  private boolean secretManagerEnabled;

  private final WebClient.Builder webClientBuilder;
  private WebClient dedicatedWebClient;
  private final AtomicLong lastMetricUpdate = new AtomicLong(0);
  private final Map<String, String> pendingMetricUpdates = new ConcurrentHashMap<>();

  private CircuitBreaker circuitBreaker;
  private Retry resilienceRetry;

  public TelegramStorageService(WebClient.Builder webClientBuilder) {
    this.webClientBuilder = webClientBuilder;
  }

  @PostConstruct
  public void init() {
    CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
    this.circuitBreaker = cbRegistry.circuitBreaker("teldrive");

    io.github.resilience4j.retry.RetryConfig retryConfig =
        io.github.resilience4j.retry.RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .retryExceptions(WebClientRequestException.class, WebClientResponseException.class)
            .build();
    RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
    this.resilienceRetry = retryRegistry.retry("teldrive");

    HttpClient httpClient =
        HttpClient.create()
            .responseTimeout(READ_TIMEOUT)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) CONNECT_TIMEOUT.toMillis());

    this.dedicatedWebClient =
        webClientBuilder
            .baseUrl(TELDRIVE_URL)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();

    logger.info(
        "[TELEGRAM] Service initialized — URL: {}, SecretManager: {}, MaxUpload: {}MB",
        TELDRIVE_URL,
        secretManagerEnabled,
        MAX_UPLOAD_SIZE_BYTES / 1024 / 1024);
  }

  @PreDestroy
  public void shutdown() {
    flushPendingMetrics();
  }

  private String resolveBotToken() {
    if (secretManagerEnabled) {
      return resolveFromSecretManager("teldrive-bot-token").orElse(TELDRIVE_BOT_TOKEN);
    }
    return TELDRIVE_BOT_TOKEN.isEmpty() ? getTokenFromFirestore() : TELDRIVE_BOT_TOKEN;
  }

  private String resolveChannelId() {
    if (TELDRIVE_CHANNEL_ID.isEmpty()) {
      return getChannelFromFirestore();
    }
    return TELDRIVE_CHANNEL_ID;
  }

  private String getTokenFromFirestore() {
    try {
      SystemConfig config = configService.getConfig();
      if (config != null && config.getTelegramConfig() != null) {
        return (String) config.getTelegramConfig().getOrDefault("botToken", "");
      }
    } catch (Exception e) {
      logger.warn("[TELEGRAM] Could not read botToken from Firestore: {}", e.getMessage());
    }
    return "";
  }

  private String getChannelFromFirestore() {
    try {
      SystemConfig config = configService.getConfig();
      if (config != null && config.getTelegramConfig() != null) {
        return (String) config.getTelegramConfig().getOrDefault("channelId", "");
      }
    } catch (Exception e) {
      logger.warn("[TELEGRAM] Could not read channelId from Firestore: {}", e.getMessage());
    }
    return "";
  }

  private java.util.Optional<String> resolveFromSecretManager(String secretId) {
    try {
      com.google.cloud.secretmanager.v1.SecretManagerServiceClient client =
          com.google.cloud.secretmanager.v1.SecretManagerServiceClient.create();
      com.google.cloud.secretmanager.v1.AccessSecretVersionRequest request =
          com.google.cloud.secretmanager.v1.AccessSecretVersionRequest.newBuilder()
              .setName("projects/supremeai-a/secrets/" + secretId + "/versions/latest")
              .build();
      String payload = client.accessSecretVersion(request).getPayload().getData().toStringUtf8();
      client.close();
      return java.util.Optional.of(payload);
    } catch (Exception e) {
      logger.warn("[TELEGRAM] Secret Manager read failed for {}: {}", secretId, e.getMessage());
      return java.util.Optional.empty();
    }
  }

  public Mono<Map<String, Object>> checkBotStatus() {
    String token = resolveBotToken();
    if (token.isEmpty()) {
      return Mono.just(Map.of("status", "DISCONNECTED", "message", "API Token missing"));
    }

    return Mono.fromCallable(
            () -> {
              return getTeldriveSettings();
            })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(
            settings -> {
              String url = (String) settings.getOrDefault("teldriveUrl", TELDRIVE_URL);
              return dedicatedWebClient
                  .get()
                  .uri("/api/v1/status")
                  .header("Authorization", "Bearer " + token)
                  .retrieve()
                  .bodyToMono(Map.class)
                  .map(
                      response -> {
                        Map<String, Object> typedResponse = (Map<String, Object>) response;
                        typedResponse.put("status", "CONNECTED");
                        typedResponse.put("lastSync", Instant.now().toString());
                        typedResponse.put(
                            "bot_name", typedResponse.getOrDefault("username", "TeldriveBot"));
                        return typedResponse;
                      });
            })
        .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
        .onErrorResume(
            e -> {
              logger.error("[TELEGRAM] Failed to connect to Teldrive: {}", e.getMessage());
              return Mono.just(Map.of("status", "ERROR", "message", e.getMessage()));
            });
  }

  public boolean isEnabled() {
    SystemConfig config = configService.getConfig();
    if (config == null || config.getTelegramConfig() == null) return false;
    Object enabled = config.getTelegramConfig().get("enabled");
    return enabled != null && (boolean) enabled;
  }

  public Mono<String> uploadFile(File file, String remotePath) {
    if (!isEnabled()) {
      logger.warn("[TELEGRAM] Upload skipped: Telegram storage is disabled");
      return Mono.error(new RuntimeException("Telegram storage is disabled"));
    }

    validateFileSize(file.length());

    String token = resolveBotToken();
    String channelId = resolveChannelId();
    String remoteFilePath = remotePath + "/" + file.getName();

    logger.info(
        "[TELEGRAM] Uploading: {} ({} bytes) to path: {}",
        file.getName(),
        file.length(),
        remoteFilePath);

    return Mono.fromCallable(() -> compressFile(file))
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(
            compressedData -> {
              MultipartBodyBuilder builder = new MultipartBodyBuilder();
              builder.part("file", compressedData).filename(file.getName() + ".gz");
              builder.part("path", remoteFilePath + ".gz");
              if (!channelId.isEmpty()) {
                builder.part("channelId", channelId);
              }

              return executeWithRetry(
                  () ->
                      dedicatedWebClient
                          .post()
                          .uri("/api/v1/upload")
                          .header("Authorization", "Bearer " + token)
                          .contentType(MediaType.MULTIPART_FORM_DATA)
                          .body(BodyInserters.fromMultipartData(builder.build()))
                          .retrieve()
                          .bodyToMono(Map.class)
                          .map(
                              response -> {
                                String fileUrl = (String) response.get("url");
                                logger.info(
                                    "[TELEGRAM] Uploaded: {} → {} (compressed {} bytes → {} bytes, ratio: {}%)",
                                    file.getName(),
                                    fileUrl,
                                    file.length(),
                                    compressedData.length,
                                    String.format(
                                        "%.1f",
                                        (1.0 - (double) compressedData.length / file.length())
                                            * 100));
                                queueMetricUpdate(compressedData.length);
                                return fileUrl != null ? fileUrl : "UPLOAD_SUCCESS";
                              }));
            })
        .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
        .onErrorResume(
            e -> {
              logger.error("[TELEGRAM] Upload failed for {}: {}", file.getName(), e.getMessage());
              return Mono.error(new RuntimeException("Teldrive upload failed: " + e.getMessage()));
            });
  }

  public Mono<String> uploadData(byte[] data, String fileName, String remotePath) {
    if (!isEnabled()) {
      logger.warn("[TELEGRAM] Data upload skipped: Telegram storage is disabled");
      return Mono.error(new RuntimeException("Telegram storage is disabled"));
    }

    validateFileSize(data.length);

    String token = resolveBotToken();
    String channelId = resolveChannelId();
    String remoteFilePath = remotePath + "/" + fileName;

    logger.info(
        "[TELEGRAM] Uploading data: {} ({} bytes) to path: {}",
        fileName,
        data.length,
        remoteFilePath);

    return Mono.fromCallable(() -> compressBytes(data))
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(
            compressedData -> {
              MultipartBodyBuilder builder = new MultipartBodyBuilder();
              builder.part("file", compressedData).filename(fileName + ".gz");
              builder.part("path", remoteFilePath + ".gz");
              if (!channelId.isEmpty()) {
                builder.part("channelId", channelId);
              }

              return executeWithRetry(
                  () ->
                      dedicatedWebClient
                          .post()
                          .uri("/api/v1/upload")
                          .header("Authorization", "Bearer " + token)
                          .contentType(MediaType.MULTIPART_FORM_DATA)
                          .body(BodyInserters.fromMultipartData(builder.build()))
                          .retrieve()
                          .bodyToMono(Map.class)
                          .map(
                              response -> {
                                String fileUrl = (String) response.get("url");
                                logger.info(
                                    "[TELEGRAM] Uploaded data: {} → {} ({} bytes → {} bytes, ratio: {}%)",
                                    fileName,
                                    fileUrl,
                                    data.length,
                                    compressedData.length,
                                    String.format(
                                        "%.1f",
                                        (1.0 - (double) compressedData.length / data.length)
                                            * 100));
                                queueMetricUpdate(compressedData.length);
                                return fileUrl != null ? fileUrl : "UPLOAD_SUCCESS";
                              }));
            })
        .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
        .onErrorResume(
            e -> {
              logger.error("[TELEGRAM] Data upload failed for {}: {}", fileName, e.getMessage());
              return Mono.error(new RuntimeException("Teldrive upload failed: " + e.getMessage()));
            });
  }

  public Mono<byte[]> downloadData(String remotePath) {
    String token = resolveBotToken();
    final String resolvedPath = remotePath.endsWith(".gz") ? remotePath : remotePath + ".gz";

    return executeWithRetry(
            () ->
                dedicatedWebClient
                    .get()
                    .uri("/api/v1/download?path=" + resolvedPath)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .flatMap(
                        gzippedData ->
                            Mono.fromCallable(() -> decompressBytes(gzippedData))
                                .subscribeOn(Schedulers.boundedElastic())))
        .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
        .onErrorResume(
            e -> {
              logger.error(
                  "[TELEGRAM] Data download failed from {}: {}", resolvedPath, e.getMessage());
              return Mono.error(
                  new RuntimeException("Teldrive download failed: " + e.getMessage()));
            });
  }

  private void validateFileSize(long size) {
    if (size > MAX_UPLOAD_SIZE_BYTES) {
      String sizeStr = formatSize(size);
      String maxStr = formatSize(MAX_UPLOAD_SIZE_BYTES);
      logger.error("[TELEGRAM] Upload rejected: file size {} exceeds limit {}", sizeStr, maxStr);
      throw new RuntimeException(String.format("File too large: %s exceeds 100MB limit", sizeStr));
    }
  }

  private byte[] compressFile(File file) throws Exception {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(baos);
        FileInputStream fis = new FileInputStream(file)) {
      byte[] buffer = new byte[8192];
      int len;
      while ((len = fis.read(buffer)) > 0) {
        gzip.write(buffer, 0, len);
      }
      gzip.finish();
      return baos.toByteArray();
    }
  }

  private byte[] compressBytes(byte[] data) throws Exception {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
      gzip.write(data);
      gzip.finish();
      return baos.toByteArray();
    }
  }

  private byte[] decompressBytes(byte[] gzippedData) throws Exception {
    try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(gzippedData);
        java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(bais);
        ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192];
      int len;
      while ((len = gzip.read(buffer)) > 0) {
        baos.write(buffer, 0, len);
      }
      return baos.toByteArray();
    }
  }

  private <T> Mono<T> executeWithRetry(java.util.function.Supplier<Mono<T>> task) {
    return Mono.defer(task)
        .retryWhen(
            reactor.util.retry.Retry.backoff(3, Duration.ofMillis(500))
                .filter(
                    e ->
                        e instanceof WebClientRequestException
                            || e instanceof WebClientResponseException)
                .doBeforeRetry(
                    sig ->
                        logger.warn(
                            "[TELEGRAM] Retry attempt {} for: {}",
                            sig.totalRetries() + 1,
                            sig.failure().getMessage())));
  }

  private Map<String, Object> getTeldriveSettings() {
    SystemConfig config = configService.getConfig();
    if (config == null || config.getTelegramConfig() == null) {
      return new java.util.HashMap<>();
    }
    Map<String, Object> telegram = new java.util.HashMap<>(config.getTelegramConfig());
    Map<String, Object> legacySettings = new java.util.HashMap<>();
    legacySettings.put("teldriveUrl", telegram.getOrDefault("teldriveUrl", TELDRIVE_URL));
    legacySettings.put("storageUsed", telegram.getOrDefault("storageUsed", "0 B"));
    legacySettings.put("telegram", telegram);
    return legacySettings;
  }

  public Mono<java.util.List<Map<String, Object>>> listFiles(String path) {
    String token = resolveBotToken();
    if (token.isEmpty()) {
      return Mono.just(java.util.Collections.emptyList());
    }

    return executeWithRetry(
            () ->
                dedicatedWebClient
                    .get()
                    .uri("/api/v1/files?path=" + path)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(
                        response -> {
                          Object items =
                              response.getOrDefault("items", response.getOrDefault("files", null));
                          if (items instanceof java.util.List) {
                            return (java.util.List<Map<String, Object>>) items;
                          }
                          return java.util.Collections.<Map<String, Object>>emptyList();
                        }))
        .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
        .onErrorResume(
            e -> {
              logger.error("[TELEGRAM] Failed to list files at {}: {}", path, e.getMessage());
              return Mono.just(java.util.Collections.emptyList());
            });
  }

  public Mono<String> getDownloadUrl(String fileId) {
    String token = resolveBotToken();

    return executeWithRetry(
            () ->
                dedicatedWebClient
                    .get()
                    .uri("/api/v1/files/" + fileId + "/url")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(response -> (String) response.get("url")))
        .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
        .onErrorResume(
            e -> {
              logger.error(
                  "[TELEGRAM] Failed to get download URL for {}: {}", fileId, e.getMessage());
              return Mono.error(new RuntimeException("Failed to get download URL"));
            });
  }

  private void queueMetricUpdate(long newBytes) {
    String threadName = Thread.currentThread().getName();
    pendingMetricUpdates.merge(
        threadName,
        String.valueOf(newBytes),
        (oldVal, newVal) -> String.valueOf(Long.parseLong(oldVal) + Long.parseLong(newVal)));

    long now = System.currentTimeMillis();
    long lastUpdate = lastMetricUpdate.get();
    if (now - lastUpdate > METRIC_UPDATE_DEBOUNCE.toMillis()) {
      if (lastMetricUpdate.compareAndSet(lastUpdate, now)) {
        flushPendingMetrics();
      }
    }
  }

  private void flushPendingMetrics() {
    long totalBytes = pendingMetricUpdates.values().stream().mapToLong(Long::parseLong).sum();
    pendingMetricUpdates.clear();

    if (totalBytes <= 0) return;

    try {
      SystemConfig current = configService.getConfig();
      if (current == null || current.getTelegramConfig() == null) return;

      Map<String, Object> telegram = new java.util.HashMap<>(current.getTelegramConfig());
      long currentSize = parseSize((String) telegram.getOrDefault("storageUsed", "0 B"));
      String newSizeFormatted = formatSize(currentSize + totalBytes);

      telegram.put("storageUsed", newSizeFormatted);
      current.setTelegramConfig(telegram);

      configService
          .updateConfig(current, "system", "127.0.0.1")
          .subscribe(
              saved ->
                  logger.debug(
                      "[TELEGRAM] Storage metrics updated: {} (+{} bytes)",
                      newSizeFormatted,
                      totalBytes),
              error ->
                  logger.error(
                      "[TELEGRAM] Failed to update storage metrics: {}", error.getMessage()));
    } catch (Exception e) {
      logger.error("[TELEGRAM] Failed to flush metrics: {}", e.getMessage());
    }
  }

  private long parseSize(String sizeStr) {
    try {
      if (sizeStr == null || sizeStr.isEmpty() || sizeStr.equals("0 B")) return 0L;
      String[] parts = sizeStr.split(" ");
      long value = Long.parseLong(parts[0]);
      String unit = parts[1].toUpperCase();
      return switch (unit) {
        case "KB" -> value * 1024;
        case "MB" -> value * 1024 * 1024;
        case "GB" -> value * 1024L * 1024 * 1024;
        default -> value;
      };
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
