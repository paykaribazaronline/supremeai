package com.supremeai.controller;

import com.supremeai.dto.ChatRequest;
import com.supremeai.dto.FeedbackRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.supremeai.service.ChatService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Deprecated(since = "2.0", forRemoval = true)
    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER', 'GUEST')")
    public Mono<ResponseEntity<Object>> sendMessage(@Valid @RequestBody ChatRequest request) {
        logger.warn("Deprecated /api/chat/send endpoint called. Processing with legacy handler.");

        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body((Object) Map.of("error", "Message is required")));
        }

        String message = request.getMessage();
        boolean skipValidation = request.isSkipValidation();

        return chatService.sendMessage(message, skipValidation)
                .map(response -> ResponseEntity.ok((Object) response))
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(e -> {
                    logger.error("Error in sendMessage", e);
                    return Mono.just(ResponseEntity.status(503).body((Object) Map.of("error", "AI services temporarily unavailable")));
                });
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    public Mono<ResponseEntity<Object>> getHistory(
            @RequestParam(required = false) String agent,
            @RequestParam(defaultValue = "50") int limit) {
        return Mono.just(ResponseEntity.ok(chatService.getHistory(agent, limit)));
    }

    @PostMapping("/feedback")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER', 'GUEST')")
    public Mono<ResponseEntity<Object>> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        chatService.submitFeedback(request.getMessageId(), request.isHelpful(), request.getUserMessage(), request.getAiResponse());
        return Mono.just(ResponseEntity.ok(Map.of("status", "received")));
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Object>> health() {
        return Mono.just(ResponseEntity.ok(Map.of(
            "status", "UP",
            "autonomous_questioning", "ACTIVE",
            "voting_system", "ACTIVE"
        )));
    }

    @PostMapping("/message")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER', 'GUEST')")
    public Mono<ResponseEntity<Object>> handleChatMessage(@Valid @RequestBody ChatRequest request) {
        return chatService.processChatWithHistory(request)
                .map(response -> ResponseEntity.ok((Object) response))
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(e -> {
                    logger.error("Error processing chat message", e);
                    return Mono.just(ResponseEntity.status(503).body((Object) Map.of("error", "AI services temporarily unavailable")));
                });
    }

    @PostMapping(value = "/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER', 'GUEST')")
    public Flux<String> handleChatStream(@Valid @RequestBody ChatRequest request) {
        return chatService.streamChat(request);
    }
}
