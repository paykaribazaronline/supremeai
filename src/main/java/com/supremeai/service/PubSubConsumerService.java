package com.supremeai.service;

import com.google.gson.Gson;
import com.supremeai.dto.PubSubMessageEnvelope;
import java.util.Base64;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service to process incoming Pub/Sub messages. Handles different topics and routes them to
 * appropriate integration services.
 */
@Service
public class PubSubConsumerService {

  private static final Logger logger = LoggerFactory.getLogger(PubSubConsumerService.class);
  private final Gson gson = new Gson();

  @Autowired private ReverseEngineeringIntegrationService revEngIntegrationService;

  @Autowired private CodeGenerationService codeGenerationService;

  /** Process a raw Pub/Sub push message. */
  public Mono<Void> consume(PubSubMessageEnvelope envelope) {
    if (envelope == null || envelope.getMessage() == null) {
      return Mono.empty();
    }

    String messageId = envelope.getMessage().getMessageId();
    String subscription = envelope.getSubscription();
    String dataBase64 = envelope.getMessage().getData();

    if (dataBase64 == null || dataBase64.isEmpty()) {
      logger.warn("Received empty Pub/Sub message: {}", messageId);
      return Mono.empty();
    }

    try {
      String decodedData = new String(Base64.getDecoder().decode(dataBase64));
      Map<String, Object> payload = gson.fromJson(decodedData, Map.class);

      logger.info("Processing Pub/Sub message {} from subscription {}", messageId, subscription);

      // Route based on subscription name or payload hints
      if (subscription != null) {
        if (subscription.contains("reverse-engineering-results")) {
          return handleReverseEngineeringResult(payload);
        } else if (subscription.contains("code-generation-jobs")) {
          return handleCodeGenerationJob(payload);
        }
      }

      // Fallback: check payload content
      if (payload.containsKey("jobId") && payload.containsKey("discoveredApis")) {
        return handleReverseEngineeringResult(payload);
      }

      logger.warn("Unknown Pub/Sub message format or source: {}", subscription);
      return Mono.empty();

    } catch (Exception e) {
      logger.error(
          "Failed to decode or process Pub/Sub message {}: {}", messageId, e.getMessage(), e);
      return Mono.error(e);
    }
  }

  private Mono<Void> handleReverseEngineeringResult(Map<String, Object> payload) {
    String jobId = (String) payload.get("jobId");
    String userId = (String) payload.get("userId");

    logger.info("[PubSubConsumer] Received RE result for job: {}", jobId);

    return revEngIntegrationService
        .onJobCompletion(jobId, userId)
        .then()
        .doOnError(
            e ->
                logger.error(
                    "Failed to integrate RE result for job {}: {}", jobId, e.getMessage()));
  }

  private Mono<Void> handleCodeGenerationJob(Map<String, Object> payload) {
    String appId = (String) payload.get("appId");
    logger.info("[PubSubConsumer] Received CodeGen job for app: {}", appId);

    // Trigger async code generation if needed
    // codeGenerationService.processAsyncJob(payload);

    return Mono.empty();
  }
}
