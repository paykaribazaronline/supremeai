package com.supremeai.controller;

import com.supremeai.model.ChatMessage;
import com.supremeai.service.MultiAIConsensusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/chat")
public class AdminChatController {

    @Autowired
    private MultiAIConsensusService consensusService;

    @PostMapping("/ask")
    public Mono<ResponseEntity<Object>> askAI(@RequestBody Map<String, Object> request) {
        String question = (String) request.get("question");
        List<String> providers = (List<String>) request.get("providers");
        
        if (providers == null || providers.isEmpty()) {
            providers = List.of("openai", "anthropic", "groq");
        }

        if (question == null || question.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                .body((Object) Map.of("error", "Question is required")));
        }

        try {
            var result = consensusService.askAllAIs(question, providers, 10000L);
            
            Map<String, Object> response = Map.of(
                "question", question,
                "answer", result.getConsensusAnswer(),
                "confidence", result.getAverageConfidence(),
                "consensusStrength", result.getStrength(),
                "votes", result.getProviderVotes(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );

            return Mono.just(ResponseEntity.ok((Object) response));
        } catch (Exception e) {
            return Mono.just(ResponseEntity.status(500)
                .body((Object) Map.of("error", "Failed to get AI response: " + e.getMessage())));
        }
    }

    @GetMapping("/history")
    public Mono<ResponseEntity<Object>> getHistory(@RequestParam(defaultValue = "50") int limit) {
        return Mono.just(ResponseEntity.ok((Object) Map.of(
            "messages", List.<ChatMessage>of(),
            "count", 0
        )));
    }

    @DeleteMapping("/history")
    public Mono<ResponseEntity<Object>> clearHistory() {
        return Mono.just(ResponseEntity.ok((Object) Map.of(
            "message", "History cleared (taste phase - in-memory only)"
        )));
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Object>> health() {
        return Mono.just(ResponseEntity.ok((Object) Map.of(
            "status", "UP",
            "service", "AdminChatController",
            "backend", "MultiAIConsensusService"
        )));
    }
}