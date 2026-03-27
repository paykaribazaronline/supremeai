package org.example.service;

import java.util.*;

/**
 * 💡 Auto-Suggestion Service (Dynamic Requirement Refinement)
 * Provides AI-driven suggestions to improve code and clarify requirements.
 */
public class AutoSuggestionService {
    private final AIAPIService apiService;

    public AutoSuggestionService(AIAPIService apiService) {
        this.apiService = apiService;
    }

    /**
     * Provide suggestions to clarify user requirements.
     */
    public List<String> suggest(String query) {
        System.out.println("💡 Generating suggestions for: " + query);
        
        String prompt = "Review this requirement and provide 3 short, actionable suggestions for clarity or performance: " + query;
        String response = apiService.callAI("BUILDER", prompt, List.of("GROQ", "DEEPSEEK"));
        
        if (response != null) {
            return Arrays.asList(response.split("\n"));
        }
        return Collections.emptyList();
    }
}
