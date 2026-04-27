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
@RequestMapping("/api/admin/chat")
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
        List<Map<String, Object>> history = chatProcessingService.getChatHistory(user_id, limit);
        return Mono.just(ResponseEntity.ok(Map.of(
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
        List<Map<String, Object>> rules = chatProcessingService.getRules(active_only);
        return Mono.just(ResponseEntity.ok(Map.of(
            "success", true,
            "rules", rules
        )));
    }

    // Get single rule
    @GetMapping("/rules/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> getRule(@PathVariable String id) {
        Map<String, Object> rule = chatProcessingService.getItemById("rule", id);
        if (rule != null) {
            return Mono.just(ResponseEntity.ok(Map.of(
                "success", true,
                "item", rule
            )));
        }
        return Mono.just(ResponseEntity.status(404).body(Map.of(
            "success", false,
            "message", "Rule not found"
        )));
    }

    // Get all plans (admin)
    @GetMapping("/plans")
    public Mono<ResponseEntity<Map<String, Object>>> getPlans(
            @RequestParam(defaultValue = "true") boolean active_only) {
        List<Map<String, Object>> plans = chatProcessingService.getPlans(active_only);
        return Mono.just(ResponseEntity.ok(Map.of(
            "success", true,
            "plans", plans
        )));
    }

    // Get single plan
    @GetMapping("/plans/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> getPlan(@PathVariable String id) {
        Map<String, Object> plan = chatProcessingService.getItemById("plan", id);
        if (plan != null) {
            return Mono.just(ResponseEntity.ok(Map.of(
                "success", true,
                "item", plan
            )));
        }
        return Mono.just(ResponseEntity.status(404).body(Map.of(
            "success", false,
            "message", "Plan not found"
        )));
    }

    // Get all commands (admin)
    @GetMapping("/commands")
    public Mono<ResponseEntity<Map<String, Object>>> getCommands(
            @RequestParam(defaultValue = "true") boolean active_only) {
        List<Map<String, Object>> commands = chatProcessingService.getCommands(active_only);
        return Mono.just(ResponseEntity.ok(Map.of(
            "success", true,
            "commands", commands
        )));
    }

    // Get single command
    @GetMapping("/commands/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> getCommand(@PathVariable String id) {
        Map<String, Object> command = chatProcessingService.getItemById("command", id);
        if (command != null) {
            return Mono.just(ResponseEntity.ok(Map.of(
                "success", true,
                "item", command
            )));
        }
        return Mono.just(ResponseEntity.status(404).body(Map.of(
            "success", false,
            "message", "Command not found"
        )));
    }

    // Get confirmation history
    @GetMapping("/confirmations")
    public Mono<ResponseEntity<Map<String, Object>>> getConfirmations(
            @RequestParam(required = false) String item_id,
            @RequestParam(required = false) String chat_id) {
        List<Map<String, Object>> history = chatProcessingService.getConfirmationHistory(item_id, chat_id);
        return Mono.just(ResponseEntity.ok(Map.of(
            "success", true,
            "confirmations", history
        )));
    }
}
