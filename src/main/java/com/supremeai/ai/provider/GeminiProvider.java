package com.supremeai.ai.provider;

import com.supremeai.dto.AISolution;
import com.supremeai.dto.ProblemStatement;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service("legacyGeminiProvider")
public class GeminiProvider implements AIProvider {

    @Value("${gemini.api.key:default_key}")
    private String apiKey;

    private final HttpClient httpClient;

    public GeminiProvider() {
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String getId() {
        return "gemini";
    }

    @Override
    public AISolution solve(ProblemStatement problem) {
        if ("default_key".equals(apiKey) || apiKey.isBlank()) {
            return fallbackSolution(problem);
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent?key=" + apiKey;
            String prompt = String.format("Problem: %s\\nContext: %s\\nOutput Type: %s", 
                problem.getDescription().replace("\"", "\\\""), 
                problem.getContext() != null ? problem.getContext().replace("\"", "\\\"") : "", 
                problem.getRequiredOutputType() != null ? problem.getRequiredOutputType() : "Text");

            String requestBody = "{\n" +
                    "  \"contents\": [{\n" +
                    "    \"parts\":[{\"text\": \"" + prompt.replace("\n", "\\n") + "\"}]\n" +
                    "  }]\n" +
                    "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                String text = extractTextFromJson(responseBody);
                String code = extractCodeFromMarkdown(text);
                
                return AISolution.builder()
                        .providerId(getId())
                        .solutionContent(text)
                        .generatedCode(code)
                        .selfScore(0.9)
                        .build();
            } else {
                return fallbackSolution(problem);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return fallbackSolution(problem);
        }
    }

    private String extractTextFromJson(String json) {
        // Simple regex to extract text from Gemini response
        Pattern pattern = Pattern.compile("\"text\"\\s*:\\s*\"(.*?)\"");
        Matcher matcher = pattern.matcher(json.replace("\\n", "\n"));
        if (matcher.find()) {
            return matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return "No response text found.";
    }

    private String extractCodeFromMarkdown(String text) {
        Pattern pattern = Pattern.compile("```(?:java|js|javascript|python|go)?\\n([\\s\\S]*?)```");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return text;
    }

    private AISolution fallbackSolution(ProblemStatement problem) {
        return AISolution.builder()
                .providerId(getId())
                .solutionContent("Gemini solution for: " + problem.getDescription())
                .generatedCode("// Mock code for " + problem.getDescription())
                .selfScore(0.5)
                .build();
    }
}
