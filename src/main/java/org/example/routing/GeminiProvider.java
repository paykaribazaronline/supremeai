package org.example.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * Gemini provider implementation.
 *
 * <p>Uses the {@code generateContent} REST API. URL and API key come from
 * {@link AIProviderProperties} so nothing is hardcoded.
 */
public class GeminiProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);

    private final AIProviderProperties.ProviderConfig config;
    private final RestTemplate restTemplate;

    public GeminiProvider(AIProviderProperties.ProviderConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getName() {
        return "gemini";
    }

    @Override
    public AIRouter.AIResponse generate(String prompt, String taskType) {
        // API key is a query param for Gemini — build URL safely without embedding it in logs
        String url = UriComponentsBuilder.fromUriString(config.getBaseUrl())
                .queryParam("key", config.getApiKey())
                .build()
                .toUriString();

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.postForEntity(url, request, Map.class);
        } catch (Exception e) {
            throw new AIProviderException("gemini",
                    "HTTP call to Gemini failed: " + e.getMessage(), e);
        }

        String content = extractContent(response.getBody());
        return new AIRouter.AIResponse(OpenAICompatibleProvider.extractCode(content), true);
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<?, ?> body) {
        if (body == null) {
            throw new AIProviderException("gemini", "Gemini returned a null response body");
        }

        Object candidatesObj = body.get("candidates");
        if (!(candidatesObj instanceof List) || ((List<?>) candidatesObj).isEmpty()) {
            Object error = body.get("error");
            throw new AIProviderException("gemini",
                    "Gemini returned no candidates" + (error != null ? ": " + error : ""));
        }

        List<?> candidates = (List<?>) candidatesObj;
        Object first = candidates.get(0);
        if (!(first instanceof Map)) {
            throw new AIProviderException("gemini", "Gemini candidate is not a map");
        }

        Map<?, ?> candidateMap = (Map<?, ?>) first;
        Object contentObj = candidateMap.get("content");
        if (!(contentObj instanceof Map)) {
            throw new AIProviderException("gemini", "Gemini candidate has no content");
        }

        Object partsObj = ((Map<?, ?>) contentObj).get("parts");
        if (!(partsObj instanceof List) || ((List<?>) partsObj).isEmpty()) {
            throw new AIProviderException("gemini", "Gemini content has no parts");
        }

        Object partFirst = ((List<?>) partsObj).get(0);
        if (!(partFirst instanceof Map)) {
            throw new AIProviderException("gemini", "Gemini part is not a map");
        }

        Object text = ((Map<?, ?>) partFirst).get("text");
        if (text == null) {
            throw new AIProviderException("gemini", "Gemini part has no text");
        }
        return text.toString();
    }
}
