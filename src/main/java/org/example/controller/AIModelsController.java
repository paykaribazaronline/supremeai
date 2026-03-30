package org.example.controller;

import org.example.model.AIModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI Models Controller
 * Manages AI model search and discovery
 */
@RestController
@RequestMapping("/api/models")
@CrossOrigin(origins = "*")
public class AIModelsController {

    private static final List<AIModel> models = new ArrayList<>();

    static {
        // Initialize with sample models
        String[][] modelData = {
            {"GPT-4", "OpenAI", "Natural Language"},
            {"GPT-3.5-turbo", "OpenAI", "Code Generation"},
            {"Claude 3", "Anthropic", "Analysis"},
            {"Gemini Pro", "Google", "Multimodal"},
            {"Mistral-7B", "Mistral", "Efficient"},
            {"DeepSeek-67B", "DeepSeek", "Advanced"},
            {"Llama 2", "Meta", "Open Source"},
            {"DALL-E 3", "OpenAI", "Image Generation"},
            {"Stable Diffusion", "Stability", "Image Synthesis"},
            {"Whisper", "OpenAI", "Speech Recognition"}
        };

        for (int i = 0; i < modelData.length; i++) {
            AIModel model = new AIModel(modelData[i][0], modelData[i][1]);
            model.setRank(i + 1);
            model.setPerformance(95.0 - (i * 2));
            model.setAccuracy(0.92 - (i * 0.02));
            model.setCostPerRequest(0.001 + (i * 0.0005));
            model.setCapabilities(Arrays.asList(modelData[i][2], "Analysis", "Optimization"));
            models.add(model);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchModels(@RequestParam(defaultValue = "") String query) {
        try {
            List<AIModel> results;
            if (query.isEmpty()) {
                results = models; // Return top 10
            } else {
                results = new ArrayList<>();
                String q = query.toLowerCase();
                for (AIModel model : models) {
                    if (model.getName().toLowerCase().contains(q) ||
                        model.getProvider().toLowerCase().contains(q) ||
                        model.getCapabilities().stream().anyMatch(c -> c.toLowerCase().contains(q))) {
                        results.add(model);
                    }
                }
            }
            return ResponseEntity.ok(results.subList(0, Math.min(10, results.size())));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addModel(@RequestBody AIModel model) {
        try {
            if (model.getId() == null) {
                model.setId(UUID.randomUUID().toString());
            }
            models.add(model);
            return ResponseEntity.ok(Map.of("success", true, "id", model.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
