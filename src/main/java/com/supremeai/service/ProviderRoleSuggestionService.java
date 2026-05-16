package com.supremeai.service;

import com.supremeai.model.APIProvider;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Service to suggest optimal roles for AI providers based on their
 * model characteristics, benchmarks, and historical performance.
 */
@Service
public class ProviderRoleSuggestionService {

    private static final Map<String, List<String>> ROLE_KEYWORDS = Map.of(
        "coding", List.of("coder", "code", "deepseek", "llama-3-70b", "gpt-4"),
        "security", List.of("audit", "exploit", "security", "defense", "hacking"),
        "reasoning", List.of("o1", "pro", "large", "opus", "r1"),
        "fast_chat", List.of("flash", "mini", "haiku", "8b", "turbo"),
        "multimodal", List.of("vision", "audio", "omni", "gpt-4o", "gemini")
    );

    /**
     * Suggest roles for a given provider based on its metadata.
     */
    public List<String> suggestRoles(APIProvider provider) {
        Set<String> suggestions = new HashSet<>();
        String name = (provider.getName() + " " + provider.getType()).toLowerCase();

        for (Map.Entry<String, List<String>> entry : ROLE_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (name.contains(keyword)) {
                    suggestions.add(entry.getKey());
                    break;
                }
            }
        }

        // Default role if nothing matched
        if (suggestions.isEmpty()) {
            suggestions.add("general_chat");
        }

        return new ArrayList<>(suggestions);
    }
}
