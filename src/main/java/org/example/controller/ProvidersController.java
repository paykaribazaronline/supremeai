package org.example.controller;

import org.example.model.APIProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * API Providers Controller
 * Manages AI API provider configurations
 */
@RestController
@RequestMapping("/api/providers")
@CrossOrigin(origins = "*")
public class ProvidersController {
    
    // In-memory store for demo (replace with DB in production)
    private static final List<APIProvider> providers = new ArrayList<>();

    static {
        // Initialize with sample providers
        APIProvider gpt = new APIProvider("OpenAI GPT-4", "LLM", "sk-xxx");
        gpt.setStatus("active");
        gpt.setModels(Arrays.asList("gpt-4", "gpt-3.5-turbo"));
        gpt.setLastTested(LocalDateTime.now());
        providers.add(gpt);
    }

    @GetMapping("/available")
    public ResponseEntity<?> getAvailableProviders() {
        try {
            List<Map<String, String>> available = Arrays.asList(
                Map.of("name", "OpenAI", "type", "LLM"),
                Map.of("name", "Google Gemini", "type", "LLM"),
                Map.of("name", "Anthropic Claude", "type", "LLM"),
                Map.of("name", "Mistral AI", "type", "LLM"),
                Map.of("name", "Cohere", "type", "LLM"),
                Map.of("name", "DeepSeek", "type", "LLM"),
                Map.of("name", "DALL-E", "type", "image"),
                Map.of("name", "Stable Diffusion", "type", "image"),
                Map.of("name", "Midjourney", "type", "image"),
                Map.of("name", "Google Speech-to-Text", "type", "voice")
            );
            return ResponseEntity.ok(available);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/configured")
    public ResponseEntity<?> getConfiguredProviders() {
        try {
            return ResponseEntity.ok(providers);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addProvider(@RequestBody APIProvider provider) {
        try {
            if (provider.getId() == null) {
                provider.setId(UUID.randomUUID().toString());
            }
            providers.add(provider);
            return ResponseEntity.ok(Map.of("success", true, "id", provider.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProvider(@PathVariable String id, @RequestBody APIProvider provider) {
        try {
            providers.removeIf(p -> p.getId().equals(id));
            provider.setId(id);
            providers.add(provider);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/test/{id}")
    public ResponseEntity<?> testProvider(@PathVariable String id) {
        try {
            APIProvider provider = providers.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
            
            if (provider == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Provider not found"));
            }

            provider.setLastTested(LocalDateTime.now());
            provider.setStatus("active");
            provider.setSuccessCount(provider.getSuccessCount() + 1);
            
            return ResponseEntity.ok(Map.of("success", true, "status", "active"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/remove")
    public ResponseEntity<?> removeProvider(@RequestBody Map<String, String> request) {
        try {
            String id = request.get("id");
            providers.removeIf(p -> p.getId().equals(id));
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
