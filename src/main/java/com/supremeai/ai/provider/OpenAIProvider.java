package com.supremeai.ai.provider;

import com.supremeai.dto.AISolution;
import com.supremeai.dto.ProblemStatement;

public class OpenAIProvider implements AIProvider {
    private final String apiKey;

    public OpenAIProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getId() {
        return "openai";
    }

    @Override
    public AISolution solve(ProblemStatement problem) {
        return AISolution.builder()
                .providerId(getId())
                .solutionContent("OpenAI solution for: " + problem.getDescription())
                .build();
    }
}
