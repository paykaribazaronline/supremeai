package com.supremeai.controller;

import com.supremeai.security.SecretManagerService;
import com.supremeai.provider.AIProvider;
import com.supremeai.service.SystemAutoDetectService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;
import reactor.core.publisher.Mono;

/**
 * OpenAI Compatible API Endpoint
 *
 * Provides a standard OpenAI-style chat completion endpoint that auto-detects the best available model.
 * Used by external tools like Continue.dev to integrate with SupremeAI without needing to know specific providers.
 *
 * Endpoint: POST /api/v1/chat/completions
 *
 * Request format (OpenAI compatible):
 * {
 *   "model": "auto" (optional, ignored - auto-detection is always used),
 *   "messages": [{"role": "user", "content": "Your prompt"}]
 * }
 *
 * Authentication: Requires X-Authorized-Key header when SUPREMEAI_API_KEY env var is set.
 * Rate limiting is applied via RateLimiterFilter for all callers.
 *
 * Response format: standard OpenAI chat completion response.
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*", maxAge = 3600)
public class OpenAICompatibleController {

    private static final Logger logger = LoggerFactory.getLogger(OpenAICompatibleController.class);

    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(30);

    @Autowired
    private SystemAutoDetectService autoDetectService;

    private final SecretManagerService secretManagerService;

    public OpenAICompatibleController(SystemAutoDetectService autoDetectService, SecretManagerService secretManagerService) {
        this.autoDetectService = autoDetectService;
        this.secretManagerService = secretManagerService;
    }

    private ResponseEntity<Map<String, Object>> checkExternalApiKey(HttpServletRequest request) {
        String expectedKey = secretManagerService.getSecret("SUPREMEAI_API_KEY");
        if (expectedKey != null && !expectedKey.isBlank()) {
            String providedKey = request.getHeader("X-Authorized-Key");
            if (!expectedKey.equals(providedKey)) {
                logger.warn("Unauthorized /api/v1/chat/completions — missing or invalid X-Authorized-Key from IP: {}",
                        request.getRemoteAddr());
                return ResponseEntity.status(401).body(Map.of(
                        "error", "UNAUTHORIZED",
                        "message", "Missing or invalid X-Authorized-Key header. Set SUPREMEAI_API_KEY to enable this endpoint."
                ));
            }
        }
        return null;
    }

    @PostMapping("/chat/completions")
    public Mono<ResponseEntity<Map<String, Object>>> chatCompletions(HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        ResponseEntity<Map<String, Object>> authFail = checkExternalApiKey(request);
        if (authFail != null) {
            return Mono.just(authFail);
        }

        Object messagesObj = body.get("messages");
        if (messagesObj == null) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                "error", "messages is required",
                "message", "Request body must contain a non-empty 'messages' array"
            )));
        }

        if (!(messagesObj instanceof List)) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                "error", "messages must be a list",
                "message", "The 'messages' field must be an array"
            )));
        }

        List<?> rawMessages = (List<?>) messagesObj;
        if (rawMessages.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                "error", "messages is required",
                "message", "Request body must contain a non-empty 'messages' array"
            )));
        }

        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) rawMessages;

        String prompt = extractLastUserMessage(messages);
        if (prompt == null || prompt.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                "error", "no_user_message",
                "message", "No user message found in the messages array"
            )));
        }

        return autoDetectService.getProvider()
            .flatMap(provider -> provider.generate(prompt)
                .timeout(BLOCK_TIMEOUT)
                .map(responseContent -> {
                    if (responseContent == null) responseContent = "";
                    return buildResponse(provider, prompt, responseContent);
                })
                .onErrorResume(e -> {
                    logger.error("Generation failed with provider {}: {}", provider.getName(), e.getMessage());
                    autoDetectService.clearCache();
                    return Mono.just(ResponseEntity.status(502).body(Map.of(
                        "error", "generation_failed",
                        "message", "AI provider returned an error: " + e.getMessage()
                    )));
                })
            )
            .onErrorResume(e -> {
                logger.error("Auto-detection failed: {}", e.getMessage());
                return Mono.just(ResponseEntity.status(503).body(Map.of(
                    "error", "no_provider_available",
                    "message", "No AI provider is currently available. Check API keys and network."
                )));
            });
    }

    private ResponseEntity<Map<String, Object>> buildResponse(AIProvider provider, String prompt, String responseContent) {
        long timestamp = System.currentTimeMillis() / 1000L;
        String responseId = "chatcmpl-" + UUID.randomUUID();

        int promptTokens = Math.max(1, prompt.length() / 4);
        int completionTokens = Math.max(1, responseContent.length() / 4);
        int totalTokens = Math.max(1, promptTokens + completionTokens);

        Map<String, Object> messageResponse = new LinkedHashMap<>();
        messageResponse.put("role", "assistant");
        messageResponse.put("content", responseContent);

        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0);
        choice.put("message", messageResponse);
        choice.put("finish_reason", "stop");

        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("prompt_tokens", promptTokens);
        usage.put("completion_tokens", completionTokens);
        usage.put("total_tokens", totalTokens);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", responseId);
        result.put("object", "chat.completion");
        result.put("created", timestamp);
        result.put("model", provider.getName());
        result.put("choices", List.of(choice));
        result.put("usage", usage);

        logger.debug("Returning chat completion from provider: {}", provider.getName());
        return ResponseEntity.ok(result);
    }

    private String extractLastUserMessage(List<Map<String, String>> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Map<String, String> msg = messages.get(i);
            if (msg != null && "user".equalsIgnoreCase(msg.get("role"))) {
                return msg.get("content");
            }
        }
        return null;
    }
}