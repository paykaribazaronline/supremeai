package org.example.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible provider implementation shared by Kimi and DeepSeek.
 *
 * <p>URL, model, and API key come from {@link AIProviderProperties} — nothing
 * is hardcoded here.
 */
public class OpenAICompatibleProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAICompatibleProvider.class);

    private final String name;
    private final AIProviderProperties.ProviderConfig config;
    private final RestTemplate restTemplate;

    public OpenAICompatibleProvider(String name,
                                    AIProviderProperties.ProviderConfig config,
                                    RestTemplate restTemplate) {
        this.name = name;
        this.config = config;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public AIRouter.AIResponse generate(String prompt, String taskType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + config.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "model", config.getModel(),
                "messages", List.of(
                        Map.of("role", "system",
                               "content", "You are an expert software developer. Generate clean, production-ready code."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.postForEntity(config.getBaseUrl(), request, Map.class);
        } catch (Exception e) {
            // Do not expose the API key in the exception message
            throw new AIProviderException(name,
                    "HTTP call to provider '" + name + "' failed: " + e.getMessage(), e);
        }

        String content = extractContent(response.getBody());
        return new AIRouter.AIResponse(extractCode(content), true);
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<?, ?> body) {
        if (body == null) {
            throw new AIProviderException(name, "Provider '" + name + "' returned a null response body");
        }

        Object choicesObj = body.get("choices");
        if (!(choicesObj instanceof List) || ((List<?>) choicesObj).isEmpty()) {
            // Surface any 'error' field the API returned
            Object error = body.get("error");
            throw new AIProviderException(name,
                    "Provider '" + name + "' returned no choices" + (error != null ? ": " + error : ""));
        }

        List<?> choices = (List<?>) choicesObj;
        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map)) {
            throw new AIProviderException(name, "Provider '" + name + "' choice is not a map");
        }

        Map<?, ?> choiceMap = (Map<?, ?>) firstChoice;
        Object messageObj = choiceMap.get("message");
        if (!(messageObj instanceof Map)) {
            throw new AIProviderException(name, "Provider '" + name + "' choice has no message");
        }

        Object contentObj = ((Map<?, ?>) messageObj).get("content");
        if (contentObj == null) {
            throw new AIProviderException(name, "Provider '" + name + "' message has no content");
        }
        return contentObj.toString();
    }

    /** Extracts the first fenced code block, or returns raw content if none found. */
    static String extractCode(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        int fenceStart = content.indexOf("```");
        if (fenceStart < 0) {
            return content.trim();
        }
        // Skip the opening fence line (may include a language tag)
        int lineEnd = content.indexOf('\n', fenceStart);
        if (lineEnd < 0) {
            return content.trim();
        }
        int codeStart = lineEnd + 1;
        int fenceEnd = content.indexOf("```", codeStart);
        if (fenceEnd <= codeStart) {
            // Closing fence not found — return everything after opening fence
            return content.substring(codeStart).trim();
        }
        return content.substring(codeStart, fenceEnd).trim();
    }
}
