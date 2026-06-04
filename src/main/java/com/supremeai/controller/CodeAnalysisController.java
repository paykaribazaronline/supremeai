package com.supremeai.controller;

import com.supremeai.service.MultiAIVotingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/code-analysis")
@PreAuthorize("hasRole('ADMIN')")
public class CodeAnalysisController {

    private final MultiAIVotingService votingService;

    public CodeAnalysisController(MultiAIVotingService votingService) {
        this.votingService = votingService;
    }

    @PostMapping("/analyze")
    public Mono<ResponseEntity<Map<String, Object>>> analyzeTestFailure(@RequestBody LogRequest request) {
        String prompt = "You are SupremeAI Code Analyzer. The following is a test failure log or error snippet from our CI/CD pipeline.\n" +
                        "Please analyze it, explain exactly why it failed, and provide the specific code fix.\n\n" +
                        "Log Details:\n" + request.getLog();

        // Multi-AI Voting to identify the most accurate fix
        return votingService.executeEnsembleVoting(prompt, null, 20000L)
                .map(result -> {
                    java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
                    body.put("success", true);
                    body.put("analysis", result.getBestResponse());
                    body.put("confidence", result.getAverageConfidence());
                    return ResponseEntity.ok(body);
                })
                .onErrorResume(e -> {
                    java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
                    body.put("success", false);
                    body.put("error", "AI Analysis failed: " + e.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().body(body));
                });
    }

    public static class LogRequest {
        private String log;
        public String getLog() { return log; }
        public void setLog(String log) { this.log = log; }
    }
}