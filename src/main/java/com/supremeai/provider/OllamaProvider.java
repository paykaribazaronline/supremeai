package com.supremeai.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Optimized Ollama provider implementation
 * Tuned for maximum performance with local LLaMA 3 70B
 */
@Component
public class OllamaProvider implements AIProvider {

    @Override
    public String getName() {
        return "ollama";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "model", "llama3:70b",
                "local", true,
                "maxContext", 8192
        );
    }

    private static final Logger logger = LoggerFactory.getLogger(OllamaProvider.class);

    private final WebClient webClient;
    private static final String DEFAULT_MODEL = "llama3:70b";
    private static final String BASE_URL = "http://localhost:11434";

    public OllamaProvider() {
        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        logger.info("OllamaProvider initialized with optimized configuration");
    }

    @Override
    public String generate(String prompt) {
        OllamaRequest request = new OllamaRequest(
                DEFAULT_MODEL,
                prompt,
                false,
                new OllamaOptions(
                        8192,
                        0.7f,
                        0.95f,
                        64,
                        Runtime.getRuntime().availableProcessors()
                )
        );

        try {
            return webClient.post()
                    .uri("/api/generate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaResponse.class)
                    .timeout(Duration.ofSeconds(60))
                    .map(OllamaResponse::response)
                    .block();

        } catch (Exception e) {
            logger.error("Ollama generation failed: {}", e.getMessage());
            throw new RuntimeException("Ollama unavailable", e);
        }
    }

    /**
     * Optimized Ollama request with performance tuning parameters
     */
    record OllamaRequest(
            String model,
            String prompt,
            boolean stream,
            OllamaOptions options
    ) {}

    record OllamaOptions(
            @JsonProperty("num_ctx") int numCtx,
            float temperature,
            @JsonProperty("top_p") float topP,
            @JsonProperty("num_batch") int numBatch,
            @JsonProperty("num_thread") int numThread
    ) {}

    record OllamaResponse(
            String response,
            boolean done,
            @JsonProperty("total_duration") long totalDuration,
            @JsonProperty("eval_count") long evalCount,
            @JsonProperty("eval_duration") long evalDuration
    ) {}
}
