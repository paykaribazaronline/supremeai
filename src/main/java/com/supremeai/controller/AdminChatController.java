package com.supremeai.controller;

import com.supremeai.service.ChatProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/chat")
public class AdminChatController {

    private final ChatProcessingService chatProcessingService;

    public AdminChatController(ChatProcessingService chatProcessingService) {
        this.chatProcessingService = chatProcessingService;
    }

    @PostMapping("/message")
    public Mono<ResponseEntity<Map<String, Object>>> processMessage(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("user_id");
        String message = (String) request.get("message");
        Boolean isAdmin = (Boolean) request.get("is_admin");

        if (userId == null || message == null) {
            return Mono.just(ResponseEntity.badRequest()
                .body(Map.of("error", "user_id and message are required")));
        }

        Map<String, Object> result = chatProcessingService.processMessage(
            userId, message, isAdmin != null && isAdmin
        );

        return Mono.just(ResponseEntity.ok(result));
    }

    @GetMapping("/history")
    public Mono<ResponseEntity<Map<String, Object>>> getHistory(
            @RequestParam(required = false) String user_id,
            @RequestParam(defaultValue = "100") int limit) {
        List<Map<String, Object>> history = chatProcessingService.getChatHistory(user_id, limit);
        return Mono.just(ResponseEntity.ok(Map.of(
            "success", true,
            "chat_history", history
        )));
    }

    @GetMapping("/pending")
    public Mono<ResponseEntity<Map<String, Object>>> getPending(
            @RequestParam(required = false) String user_id) {
        List<Map<String, Object>> pending = chatProcessingService.getPendingConfirmations(user_id);
        return Mono.just(ResponseEntity.ok(Map.of(
            "success", true,
            "items", pending
        )));
    }

    @PostMapping("/confirm")
    public Mono<ResponseEntity<Map<String, Object>>> confirmItem(@RequestBody Map<String, Object> request) {
        String itemId = (String) request.get("item_id");
        Boolean confirmed = (Boolean) request.get("confirmed");
        String userId = (String) request.get("user_id");

        if (itemId == null || confirmed == null || userId == null) {
            return Mono.just(ResponseEntity.badRequest()
                .body(Map.of("error", "item_id, confirmed, and user_id are required")));
        }

        Map<String, Object> result = chatProcessingService.confirmItem(itemId, confirmed, userId);
        boolean success = (boolean) result.get("success");

        return success
            ? Mono.just(ResponseEntity.ok(result))
            : Mono.just(ResponseEntity.badRequest().body(result));
    }
}
