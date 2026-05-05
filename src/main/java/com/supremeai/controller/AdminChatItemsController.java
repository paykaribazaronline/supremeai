package com.supremeai.controller;

import com.supremeai.model.ChatPlan;
import com.supremeai.model.ChatCommand;
import com.supremeai.model.ChatRule;
import com.supremeai.service.ChatProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/chat-items")
public class AdminChatItemsController {

    private final ChatProcessingService chatProcessingService;

    public AdminChatItemsController(ChatProcessingService chatProcessingService) {
        this.chatProcessingService = chatProcessingService;
    }

    // Process user message (classification)
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

    // Get chat history
    @GetMapping("/history")
    public Mono<ResponseEntity<Map<String, Object>>> getHistory(
            @RequestParam(required = false) String user_id,
            @RequestParam(defaultValue = "100") int limit) {
        return chatProcessingService.getChatHistory(user_id, limit)
            .map(history -> ResponseEntity.ok(Map.of(
                "success", true,
                "chat_history", history
            )));
    }

    // Get pending confirmations
    @GetMapping("/pending")
    public Mono<ResponseEntity<Map<String, Object>>> getPending(
            @RequestParam(required = false) String user_id) {
        List<Map<String, Object>> pending = chatProcessingService.getPendingConfirmations(user_id);
        return Mono.just(ResponseEntity.ok(Map.of(
            "success", true,
            "items", pending
        )));
    }

    // Confirm or reject item
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

    // Get all rules (admin)
    @GetMapping("/rules")
    public Mono<ResponseEntity<Map<String, Object>>> getRules(
            @RequestParam(defaultValue = "true") boolean active_only) {
        return chatProcessingService.getRules(active_only)
            .map(rules -> ResponseEntity.ok(Map.of(
                "success", true,
                "rules", rules
            )));
    }

    // Get single rule
    @GetMapping("/rules/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> getRule(@PathVariable String id) {
        return chatProcessingService.getItemById("rule", id)
            .map(rule -> ResponseEntity.ok(Map.of(
                "success", true,
                "item", rule
            )))
            .switchIfEmpty(Mono.just(ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", "Rule not found"
            ))));
    }

    // Get all plans (admin)
    @GetMapping("/plans")
    public Mono<ResponseEntity<Map<String, Object>>> getPlans(
            @RequestParam(defaultValue = "true") boolean active_only) {
        return chatProcessingService.getPlans(active_only)
            .map(plans -> ResponseEntity.ok(Map.of(
                "success", true,
                "plans", plans
            )));
    }

    // Get single plan
    @GetMapping("/plans/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> getPlan(@PathVariable String id) {
        return chatProcessingService.getItemById("plan", id)
            .map(plan -> ResponseEntity.ok(Map.of(
                "success", true,
                "item", plan
            )))
            .switchIfEmpty(Mono.just(ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", "Plan not found"
            ))));
    }

    // Get all commands (admin)
    @GetMapping("/commands")
    public Mono<ResponseEntity<Map<String, Object>>> getCommands(
            @RequestParam(defaultValue = "true") boolean active_only) {
        return chatProcessingService.getCommands(active_only)
            .map(commands -> ResponseEntity.ok(Map.of(
                "success", true,
                "commands", commands
            )));
    }

    // Get single command
    @GetMapping("/commands/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> getCommand(@PathVariable String id) {
        return chatProcessingService.getItemById("command", id)
            .map(command -> ResponseEntity.ok(Map.of(
                "success", true,
                "item", command
            )))
            .switchIfEmpty(Mono.just(ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", "Command not found"
            ))));
    }

    // Get confirmation history
    @GetMapping("/confirmations")
    public Mono<ResponseEntity<Map<String, Object>>> getConfirmations(
            @RequestParam(required = false) String item_id,
            @RequestParam(required = false) String chat_id) {
        return chatProcessingService.getConfirmationHistory(item_id, chat_id)
            .map(history -> ResponseEntity.ok(Map.of(
                "success", true,
                "confirmations", history
            )));
    }
}
