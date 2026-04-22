package com.supremeai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.supremeai.service.MultiAIConsensusService;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired(required = false)
    private MultiAIConsensusService consensusService;

    @PostMapping("/send")
    public Mono<ResponseEntity<Object>> sendMessage(@RequestBody Map<String, Object> request) {
        String message = (String) request.get("message");
        String provider = (String) request.get("provider");
        String model = (String) request.get("model");

        if (message == null || message.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                .body(Map.of("error", "Message is required")));
        }

        logger.info("Received chat message: {}", message);

        // If consensus service is available, use it for multi-AI response
        if (consensusService != null) {
            try {
                List<String> providers = new ArrayList<>();
                if (provider != null && !provider.isEmpty()) {
                    providers.add(provider);
                } else {
                    providers.add("google");
                }

                var result = consensusService.askAllAIs(message, providers, 10000L);
                
                return Mono.just(ResponseEntity.ok(Map.of(
                    "message", result.getConsensusAnswer(),
                    "provider", provider != null ? provider : "google",
                    "model", model != null ? model : "default",
                    "confidence", result.getAverageConfidence(),
                    "timestamp", java.time.Instant.now().toString()
                )));
            } catch (Exception e) {
                logger.error("Failed to get AI response for message: {}", message, e);
                return Mono.just(ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get AI response: " + e.getMessage())));
            }
        } else {
            // Fallback response when consensus service is not available
            logger.warn("MultiAIConsensusService not available, returning fallback response");
            return Mono.just(ResponseEntity.ok(Map.of(
                "message", "AI service is currently initializing. Please try again in a moment.",
                "provider", provider != null ? provider : "google",
                "model", model != null ? model : "default",
                "confidence", 0.0,
                "timestamp", java.time.Instant.now().toString()
            )));
        }
    }

    @GetMapping("/history")
    public Mono<ResponseEntity<Object>> getHistory(
            @RequestParam(required = false) String agent,
            @RequestParam(defaultValue = "50") int limit) {
        return Mono.just(ResponseEntity.ok(Map.of(
            "messages", new ArrayList<>(),
            "count", 0,
            "agent", agent != null ? agent : "default"
        )));
    }

    @PostMapping("/feedback")
    public Mono<ResponseEntity<Object>> submitFeedback(@RequestBody Map<String, Object> request) {
        String messageId = (String) request.get("messageId");
        Boolean helpful = (Boolean) request.get("helpful");
        String comment = (String) request.get("comment");

        logger.info("Received feedback for message: {}, helpful: {}", messageId, helpful);

        return Mono.just(ResponseEntity.ok(Map.of(
            "status", "received",
            "messageId", messageId != null ? messageId : "unknown"
        )));
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Object>> health() {
        boolean consensusAvailable = consensusService != null;
        return Mono.just(ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "ChatController",
            "consensusServiceAvailable", consensusAvailable
        )));
    }
}
