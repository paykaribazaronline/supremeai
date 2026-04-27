package com.supremeai.controller;

import com.supremeai.service.ChatProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/rules")
public class AdminRuleController {

    private final ChatProcessingService chatProcessingService;

    public AdminRuleController(ChatProcessingService chatProcessingService) {
        this.chatProcessingService = chatProcessingService;
    }

    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> getRules(
            @RequestParam(defaultValue = "true") boolean active_only) {
        List<Map<String, Object>> rules = chatProcessingService.getRules(active_only);
        return Mono.just(ResponseEntity.ok(Map.of(
            "success", true,
            "rules", rules
        )));
    }

    @GetMapping("/{id}")
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
}
