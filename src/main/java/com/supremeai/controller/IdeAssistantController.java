package com.supremeai.controller;

import com.supremeai.ai.provider.GeminiProvider;
import com.supremeai.dto.AISolution;
import com.supremeai.dto.ProblemStatement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/ide/assistant")
@CrossOrigin(origins = "*")
public class IdeAssistantController {

    private final GeminiProvider geminiProvider;

    public IdeAssistantController(@org.springframework.beans.factory.annotation.Qualifier("legacyGeminiProvider") GeminiProvider geminiProvider) {
        this.geminiProvider = geminiProvider;
    }

    @PostMapping("/ask")
    public Mono<ResponseEntity<Map<String, String>>> askAssistant(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        String context = request.get("context"); // e.g., current file content

        if (prompt == null || prompt.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "Prompt is required")));
        }

        ProblemStatement problem = ProblemStatement.builder()
                .description(prompt)
                .context(context)
                .requiredOutputType("Markdown")
                .build();

        // Normally we'd do this non-blocking using WebClient inside the provider, 
        // but since we used Java 11 HttpClient synchronously in GeminiProvider, 
        // we wrap it in a blocking call handler (or just run it directly for now).
        return Mono.fromCallable(() -> {
            AISolution solution = geminiProvider.solve(problem);
            return ResponseEntity.ok(Map.of(
                    "response", solution.getSolutionContent(),
                    "code", solution.getGeneratedCode() != null ? solution.getGeneratedCode() : "",
                    "provider", solution.getProviderId()
            ));
        }).onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage()))));
    }
}
