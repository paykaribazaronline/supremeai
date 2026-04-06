package org.example.routing;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
public class AIRouter {
    
    // Admin configurable via application.properties or Firebase
    @Value("${ai.priority.order:kimi,deepseek,gemini}")
    private String priorityOrder;
    
    @Value("${ai.kimi.api-key:}")
    private String kimiKey;
    
    @Value("${ai.deepseek.api-key:}")
    private String deepseekKey;
    
    @Value("${ai.gemini.api-key:}")
    private String geminiKey;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    // Admin can change this runtime via API
    private volatile List<String> aiSequence;
    
    // Use @PostConstruct so @Value fields are already injected before we use them
    @PostConstruct
    public void init() {
        this.aiSequence = parseSequence(priorityOrder);
    }
    
    /**
     * Admin sets new sequence: "deepseek,kimi,gemini"
     */
    public void setAISequence(String sequence) {
        this.aiSequence = parseSequence(sequence);
        // Save to Firebase for persistence
        saveToFirebase(sequence);
    }
    
    public String getCurrentSequence() {
        return String.join(",", aiSequence);
    }
    
    /**
     * Route task to AI based on admin priority
     */
    public AIResponse generateCode(String prompt, String taskType) {
        List<String> errors = new ArrayList<>();
        
        for (String aiName : aiSequence) {
            try {
                System.out.println("Trying: " + aiName);
                AIResponse response = callAI(aiName, prompt, taskType);
                
                if (response.isSuccess()) {
                    return response.withUsedAI(aiName);
                }
                
            } catch (Exception e) {
                errors.add(aiName + ": " + e.getMessage());
                continue; // Try next AI
            }
        }
        
        // All failed
        throw new RuntimeException("All AI failed: " + errors);
    }
    
    private AIResponse callAI(String aiName, String prompt, String taskType) {
        switch (aiName.toLowerCase()) {
            case "kimi":
                return callKimi(prompt, taskType);
            case "deepseek":
                return callDeepSeek(prompt, taskType);
            case "gemini":
                return callGemini(prompt, taskType);
            default:
                throw new IllegalArgumentException("Unknown AI: " + aiName);
        }
    }
    
    private AIResponse callKimi(String prompt, String taskType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + kimiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> body = new HashMap<>();
        body.put("model", "kimi-k2.5");
        body.put("messages", List.of(
            Map.of("role", "system", "content", "You are an Android app developer. Generate clean, production-ready code."),
            Map.of("role", "user", "content", prompt)
        ));
        body.put("temperature", 0.2);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "https://api.moonshot.cn/v1/chat/completions",
            request,
            Map.class
        );
        
        String code = extractCode(response.getBody());
        return new AIResponse(code, true);
    }
    
    private AIResponse callDeepSeek(String prompt, String taskType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + deepseekKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> body = new HashMap<>();
        body.put("model", "deepseek-coder");
        body.put("messages", List.of(
            Map.of("role", "system", "content", "Expert Android developer. Generate code with best practices."),
            Map.of("role", "user", "content", prompt)
        ));
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "https://api.deepseek.com/v1/chat/completions",
            request,
            Map.class
        );
        
        String code = extractCode(response.getBody());
        return new AIResponse(code, true);
    }
    
    private AIResponse callGemini(String prompt, String taskType) {
        // Gemini implementation
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + geminiKey;
        
        Map<String, Object> content = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
            )
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(content, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        
        String code = extractGeminiCode(response.getBody());
        return new AIResponse(code, true);
    }
    
    private String extractCode(Map response) {
        // Parse OpenAI-compatible response
        List choices = (List) response.get("choices");
        Map choice = (Map) choices.get(0);
        Map message = (Map) choice.get("message");
        String content = (String) message.get("content");
        
        // Extract code blocks
        if (content.contains("```")) {
            int start = content.indexOf("```") + 3;
            int end = content.indexOf("```", start);
            if (end > start) {
                String lang = content.substring(start, content.indexOf("\n", start));
                return content.substring(start + lang.length() + 1, end).trim();
            }
        }
        return content;
    }
    
    private String extractGeminiCode(Map response) {
        List candidates = (List) response.get("candidates");
        Map candidate = (Map) candidates.get(0);
        Map content = (Map) candidate.get("content");
        List parts = (List) content.get("parts");
        Map part = (Map) parts.get(0);
        return (String) part.get("text");
    }
    
    private List<String> parseSequence(String sequence) {
        return Arrays.asList(sequence.toLowerCase().split(","));
    }
    
    private void saveToFirebase(String sequence) {
        // TODO: Firebase integration
        System.out.println("Saving sequence to Firebase: " + sequence);
    }
    
    // Inner class for response
    public static class AIResponse {
        private final String code;
        private final boolean success;
        private String usedAI;
        
        public AIResponse(String code, boolean success) {
            this.code = code;
            this.success = success;
        }
        
        public AIResponse withUsedAI(String ai) {
            this.usedAI = ai;
            return this;
        }
        
        public String getCode() { return code; }
        public boolean isSuccess() { return success; }
        public String getUsedAI() { return usedAI; }
    }
}
