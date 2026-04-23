package com.supremeai.ai.client;

import java.util.List;

public interface GeminiClient {
    String generateQuestions(String prompt);
    String analyze(String prompt);
}
