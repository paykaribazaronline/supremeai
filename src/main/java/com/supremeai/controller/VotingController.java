package com.supremeai.controller;

import com.supremeai.service.MultiAIVotingService;
import com.supremeai.agentorchestration.RequirementAnalyzerAI;
import com.supremeai.agentorchestration.VotingDecision;
import com.supremeai.model.ConsensusResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/voting")
public class VotingController {

    @Autowired
    private MultiAIVotingService votingService;

    @Autowired
    private RequirementAnalyzerAI requirementAnalyzer;

    @Value("${supremeai.active.providers:groq,openai,anthropic,ollama}")
    private String activeProviders;

    // ===== CLARIFYING QUESTIONS ENDPOINT =====

    @PostMapping("/analyze")
    public ResponseEntity<List<String>> analyzeRequirement(@RequestBody Map<String, String> request) {
        String requirement = request.get("requirement");
        List<String> questions = requirementAnalyzer.generateClarifyingQuestions(requirement);
        return ResponseEntity.ok(questions);
    }

    // ===== VOTING ENDPOINTS =====

    @PostMapping("/vote")
    public ResponseEntity<VotingDecision> conductVote(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String context = request.getOrDefault("context", "");
        VotingDecision decision = votingService.conductDecisionVote(question, context);
        return ResponseEntity.ok(decision);
    }

    @PostMapping("/vote/consensus")
    @SuppressWarnings("unchecked")
    public Mono<ResponseEntity<Object>> voteOnQuestion(@RequestBody Map<String, Object> request) {
        String question = (String) request.get("question");
        List<String> providers = (List<String>) request.getOrDefault("providers",
            Arrays.asList(activeProviders.split(",")));

        if (question == null || question.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                .body((Object) Map.of("error", "Question is required")));
        }

        return votingService.askConsensus(question, providers, 10000L)
            .map(result -> ResponseEntity.ok((Object) Map.of(
                "question", result.getQuestion(),
                "consensus", result.getConsensusAnswer(),
                "confidence", result.getAverageConfidence(),
                "strength", result.getStrength(),
                "votes", result.getProviderVotes()
            )));
    }

    // ===== HISTORY ENDPOINT =====

    @GetMapping("/history")
    public Mono<ResponseEntity<Object>> getHistory(@RequestParam(defaultValue = "20") int limit) {
        return votingService.getConsensusHistory(limit)
            .collectList()
            .map(history -> {
                Map<String, Object> response = Map.of(
                    "history", history,
                    "count", history.size()
                );
                return ResponseEntity.ok((Object) response);
            });
    }

    // ===== HEALTH ENDPOINT =====

    @GetMapping("/health")
    public Mono<ResponseEntity<Object>> health() {
        return Mono.just(ResponseEntity.ok((Object) Map.of(
            "status", "UP",
            "service", "MultiAIVotingService",
            "providers", Arrays.asList(activeProviders.split(","))
        )));
    }

    // ===== STRATEGY COMPARISON ENDPOINT =====

    @PostMapping("/compare-strategies")
    @SuppressWarnings("unchecked")
    public Mono<ResponseEntity<Object>> compareStrategies(@RequestBody Map<String, Object> request) {
        String question = (String) request.get("question");
        List<String> providers = (List<String>) request.get("providers");

        if (providers == null || providers.size() < 2) {
            return Mono.just(ResponseEntity.badRequest()
                .body((Object) Map.of("error", "At least 2 providers required for comparison")));
        }

        return votingService.askConsensus(question, providers, 15000L)
            .map(result -> ResponseEntity.ok((Object) Map.of(
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
