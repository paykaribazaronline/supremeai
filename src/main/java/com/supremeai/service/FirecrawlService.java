package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * FirecrawlService handles browser scraping using the Firecrawl.dev API.
 * It converts web pages into LLM-ready markdown format.
 */
@Service
public class FirecrawlService {

    private static final Logger log = LoggerFactory.getLogger(FirecrawlService.class);
    private final WebClient webClient;

    @Value("${supremeai.firecrawl.api-key:}")
    private String apiKey;

    public FirecrawlService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.firecrawl.dev").build();
    }

    public Mono<String> scrapeToMarkdown(String url) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[Firecrawl] API Key is missing. Scraping aborted for: {}", url);
            return Mono.error(new IllegalStateException("Firecrawl API Key is not configured."));
        }

        return webClient.post()
                .uri("/v0/scrape")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(Map.of("url", url))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> response.containsKey("markdown") ? (String) response.get("markdown") : "");
    }
}