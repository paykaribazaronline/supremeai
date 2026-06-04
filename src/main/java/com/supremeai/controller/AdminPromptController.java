package com.supremeai.controller;

import com.supremeai.response.ApiResponse;
import com.supremeai.service.AutonomousQuestioningEngine;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/** AdminPromptController - Handles prompt analysis and suggestions for admins. */
@RestController
@RequestMapping("/api/admin/prompt")
public class AdminPromptController {

  private static final Logger log = LoggerFactory.getLogger(AdminPromptController.class);
  private final AutonomousQuestioningEngine questioningEngine;

  @Autowired
  public AdminPromptController(AutonomousQuestioningEngine questioningEngine) {
    this.questioningEngine = questioningEngine;
  }

  @PostMapping("/analyze")
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> analyzePrompt(
      @RequestBody Map<String, String> body) {
    String prompt = body.get("prompt");
    if (prompt == null || prompt.trim().isEmpty()) {
      return Mono.just(ResponseEntity.badRequest().body(ApiResponse.error("Prompt is required")));
    }

    return questioningEngine
        .validateAndQuestion(prompt, AutonomousQuestioningEngine.RequestType.GENERAL_AI)
        .map(
            result -> {
              Map<String, Object> analysis = new HashMap<>();
              analysis.put("originalInput", result.getOriginalInput());
              analysis.put("clarityScore", result.getClarityScore());
              analysis.put("isComplete", result.isComplete());
              analysis.put("clarifyingQuestions", result.getClarifyingQuestions());
              return ResponseEntity.ok(ApiResponse.ok(analysis));
            });
  }

  @GetMapping("/suggestions")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getPromptSuggestions() {
    List<Map<String, Object>> suggestions =
        List.of(
            Map.of(
                "category",
                "Scope",
                "questions",
                List.of(
                    "What is the target user base?",
                    "Is this a prototype or production system?",
                    "What are the key features needed?")),
            Map.of(
                "category",
                "Tech Stack",
                "questions",
                List.of(
                    "Preferred programming language?",
                    "Frontend framework preference?",
                    "Database requirements?")),
            Map.of(
                "category",
                "Constraints",
                "questions",
                List.of(
                    "Timeline expectations?",
                    "Budget considerations?",
                    "Existing systems to integrate with?")));

    return ResponseEntity.ok(
        ApiResponse.ok(
            Map.of("suggestions", suggestions, "timestamp", System.currentTimeMillis())));
  }
}
