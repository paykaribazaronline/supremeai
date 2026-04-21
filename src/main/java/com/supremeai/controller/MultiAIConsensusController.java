package com.supremeai.controller;

import com.supremeai.model.ConsensusResult;
import com.supremeai.model.ConsensusVote;
import com.supremeai.service.MultiAIConsensusService;
import com.supremeai.model.SystemConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/consensus")
public class MultiAIConsensusController {

    @Autowired
    private MultiAIConsensusService consensusService;
    
    @Value("${supremeai.active.providers:groq,openai,anthropic,ollama}")
    private String activeProviders;

    /**
     * POST /api/consensus/vote
     * Body: {"question": "...", "providers": ["openai","anthropic","groq"]}
     */
    @PostMapping("/vote")
    @SuppressWarnings("unchecked")
    public Mono<ResponseEntity<Object>> voteOnQuestion(@RequestBody Map<String, Object> request) {
        String question = (String) request.get("question");
        List<String> providers = (List<String>) request.getOrDefault("providers", 
            Arrays.asList(activeProviders.split(",")));
        
        if (question == null || question.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                .body((Object) Map.of("error", "Question is required")));
        }
        
        ConsensusResult result = consensusService.askAllAIs(question, providers, 10000L);
        
        return Mono.just(ResponseEntity.ok((Object) Map.of(
            "question", result.getQuestion(),
            "consensus", result.getConsensusAnswer(),
            "confidence", result.getAverageConfidence(),
            "strength", result.getStrength(),
            "votes", result.getProviderVotes()
        )));
    }

    /**
     * GET /api/consensus/history?limit=20
     * Returns recent consensus votes for audit/learning
     */
    @GetMapping("/history")
    public Mono<ResponseEntity<Object>> getHistory(@RequestParam(defaultValue = "20") int limit) {
        return consensusService.getHistory(limit)
            .collectList()
            .map(history -> {
                Map<String, Object> response = Map.of(
                    "history", history,
                    "count", history.size()
                );
                return ResponseEntity.ok((Object) response);
            });
    }

    /**
     * GET /api/consensus/health
     * Health check for consensus service
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Object>> health() {
        return Mono.just(ResponseEntity.ok((Object) Map.of(
            "status", "UP",
            "service", "MultiAIConsensusService",
            "providers", Arrays.asList(activeProviders.split(","))
        )));
    }

    /**
     * POST /api/consensus/compare-strategies
     * Compare different voting strategies
     */
    @PostMapping("/compare-strategies")
    @SuppressWarnings("unchecked")
    public Mono<ResponseEntity<Object>> compareStrategies(@RequestBody Map<String, Object> request) {
        String question = (String) request.get("question");
        List<String> providers = (List<String>) request.get("providers");
        
        if (providers == null || providers.size() < 2) {
            return Mono.just(ResponseEntity.badRequest()
                .body((Object) Map.of("error", "At least 2 providers required for comparison")));
        }

        ConsensusResult result = consensusService.askAllAIs(question, providers, 15000L);
        
        return Mono.just(ResponseEntity.ok((Object) Map.of(
            "question", question,
            "result", Map.of(
                "consensus", result.getConsensusAnswer(),
                "confidence", result.getAverageConfidence(),
                "voteCount", result.getProviderVotes().size(),
                "strategy", "MAJORITY"
            )
        )));
    }
}
