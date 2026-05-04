package com.supremeai.ai.client;

public interface GeminiClient {
    String generateQuestions(String prompt);
    String analyze(String prompt);
}
