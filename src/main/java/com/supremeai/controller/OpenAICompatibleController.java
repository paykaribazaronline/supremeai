package com.supremeai.controller;

import com.supremeai.provider.AIProvider;
import com.supremeai.service.SystemAutoDetectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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
 * Response format: standard OpenAI chat completion response.
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*", maxAge = 3600)
public class OpenAICompatibleController {

    private static final Logger logger = LoggerFactory.getLogger(OpenAICompatibleController.class);

    @Autowired
    private SystemAutoDetectService autoDetectService;

    /**
     * Handles chat completion requests with auto-detected model.
     */
    @PostMapping("/chat/completions")
    public ResponseEntity<Map<String, Object>> chatCompletions(@RequestBody Map<String, Object> body) {
        // Extract messages list
        List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");
        if (messages == null || messages.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "messages is required",
                "message", "Request body must contain a non-empty 'messages' array"
            ));
        }

        // Extract the last user message
        String prompt = extractLastUserMessage(messages);
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "no_user_message",
                "message", "No user message found in the messages array"
            ));
        }

        // Get an auto-detected provider (cached healthy provider)
        AIProvider provider;
        try {
            provider = autoDetectService.getProvider();
        } catch (Exception e) {
            logger.error("Auto-detection failed: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of(
                "error", "no_provider_available",
                "message", "No AI provider is currently available. Check API keys and network."
            ));
        }

        // Generate response
        String responseContent;
        try {
            responseContent = provider.generate(prompt).block();
            if (responseContent == null) {
                responseContent = "";
            }
        } catch (Exception e) {
            logger.error("Generation failed with provider {}: {}", provider.getName(), e.getMessage());
            // Invalidate cache for this provider as it seems unhealthy
            autoDetectService.clearCache();
            return ResponseEntity.status(502).body(Map.of(
                "error", "generation_failed",
                "message", "AI provider returned an error: " + e.getMessage()
            ));
        }

        // Build OpenAI-compatible response
        long timestamp = System.currentTimeMillis() / 1000L;
        String responseId = "chatcmpl-" + UUID.randomUUID();

        // Token estimation (approx chars/4)
        int promptTokens = Math.max(1, prompt.length() / 4);
        int completionTokens = Math.max(1, responseContent.length() / 4);
        int totalTokens = promptTokens + completionTokens;

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
        result.put("model", provider.getName()); // provider name as model identifier
        result.put("choices", List.of(choice));
        result.put("usage", usage);

        logger.debug("Returning chat completion from provider: {}", provider.getName());
        return ResponseEntity.ok(result);
    }

    /**
     * Extracts the last user message from a chat history.
     */
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
