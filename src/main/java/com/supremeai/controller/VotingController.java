package com.supremeai.controller;

import com.supremeai.agentorchestration.AutonomousVotingService;
import com.supremeai.agentorchestration.RequirementAnalyzerAI;
import com.supremeai.agentorchestration.VotingDecision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voting")
@CrossOrigin(origins = "*")
public class VotingController {

    @Autowired
    private AutonomousVotingService votingService;

    @Autowired
    private RequirementAnalyzerAI requirementAnalyzer;

    @PostMapping("/analyze")
    public ResponseEntity<List<String>> analyzeRequirement(@RequestBody Map<String, String> request) {
        String requirement = request.get("requirement");
        List<String> questions = requirementAnalyzer.generateClarifyingQuestions(requirement);
        return ResponseEntity.ok(questions);
    }

    @PostMapping("/vote")
    public ResponseEntity<VotingDecision> conductVote(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String context = request.getOrDefault("context", "");
        VotingDecision decision = votingService.conductVote(question, context);
        return ResponseEntity.ok(decision);
    }
}
