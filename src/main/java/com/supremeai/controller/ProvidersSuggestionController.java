package com.supremeai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/providers")
public class ProvidersSuggestionController {

    /**
     * Model suggestion DTO returned to the frontend.
     */
    public static class ModelSuggestion {
        private String id;
        private String name;
        private String provider;
        private String model;
        private String endpoint;

        public ModelSuggestion() {}

        public ModelSuggestion(String id, String name, String provider, String model, String endpoint) {
            this.id = id;
            this.name = name;
            this.provider = provider;
            this.model = model;
            this.endpoint = endpoint;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getProvider() { return provider; }
        public String getModel() { return model; }
        public String getEndpoint() { return endpoint; }
    }

    private static final List<ModelSuggestion> COMMON_PROVIDERS = List.of(
        new ModelSuggestion("openai", "OpenAI GPT-4o", "openai", "gpt-4o", "https://api.openai.com/v1/chat/completions"),
        new ModelSuggestion("openai-mini", "OpenAI GPT-4o Mini", "openai", "gpt-4o-mini", "https://api.openai.com/v1/chat/completions"),
        new ModelSuggestion("anthropic", "Anthropic Claude 3.5 Sonnet", "anthropic", "claude-3-5-sonnet-20240620", "https://api.anthropic.com/v1/messages"),
        new ModelSuggestion("anthropic-opus", "Anthropic Claude 3 Opus", "anthropic", "claude-3-opus-20240229", "https://api.anthropic.com/v1/messages"),
        new ModelSuggestion("groq", "Groq Llama 3 70B", "groq", "llama3-70b-8192", "https://api.groq.com/openai/v1/chat/completions"),
        new ModelSuggestion("groq-mixtral", "Groq Mixtral 8x7B", "groq", "mixtral-8x7b-32768", "https://api.groq.com/openai/v1/chat/completions"),
        new ModelSuggestion("gemini", "Google Gemini 1.5 Pro", "google", "gemini-1.5-pro", "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent"),
        new ModelSuggestion("gemini-flash", "Google Gemini 1.5 Flash", "google", "gemini-1.5-flash", "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"),
        new ModelSuggestion("cohere", "Cohere Command R+", "cohere", "command-r-plus", "https://api.cohere.ai/v1/chat"),
        new ModelSuggestion("together", "Together Llama 3 70B", "together", "meta-llama/Meta-Llama-3-70B-Instruct-Turbo", "https://api.together.xyz/v1/chat/completions"),
        new ModelSuggestion("deepseek", "DeepSeek V3", "deepseek", "deepseek-chat", "https://api.deepseek.com/chat/completions"),
        new ModelSuggestion("openrouter", "OpenRouter GPT-4o", "openrouter", "openai/gpt-4o", "https://openrouter.ai/api/v1/chat/completions")
    );

    @GetMapping("/suggest")
    public ResponseEntity<List<ModelSuggestion>> suggest(@RequestParam String q) {
        String query = q.toLowerCase();
        List<ModelSuggestion> matches = COMMON_PROVIDERS.stream()
            .filter(s -> 
                s.getName().toLowerCase().contains(query) ||
                s.getProvider().toLowerCase().contains(query) ||
                s.getModel().toLowerCase().contains(query)
            )
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/templates")
    public ResponseEntity<List<ModelSuggestion>> allTemplates() {
        return ResponseEntity.ok(COMMON_PROVIDERS);
    }
}
