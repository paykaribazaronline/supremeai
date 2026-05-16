package com.supremeai.controller;

import com.supremeai.dto.PubSubMessageEnvelope;
import com.supremeai.service.PubSubConsumerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Endpoint for Pub/Sub push subscriptions.
 * Secure this endpoint in production using Pub/Sub verification tokens or OIDC.
 */
@RestController
@RequestMapping("/api/pubsub")
public class PubSubWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(PubSubWebhookController.class);

    @Autowired
    private PubSubConsumerService consumerService;

    @PostMapping("/push")
    public Mono<ResponseEntity<String>> handlePush(@RequestBody PubSubMessageEnvelope envelope) {
        logger.info("Received Pub/Sub push notification");
        
        return consumerService.consume(envelope)
            .thenReturn(ResponseEntity.ok("OK"))
            .onErrorResume(e -> {
                logger.error("Error processing push notification: {}", e.getMessage());
                return Mono.just(ResponseEntity.status(500).body("Error: " + e.getMessage()));
            });
    }
}
