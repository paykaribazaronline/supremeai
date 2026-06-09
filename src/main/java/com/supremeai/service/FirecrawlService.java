package com.supremeai.service;

import com.supremeai.service.browser.BrowserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;

/**
 * FirecrawlService handles browser scraping using the Firecrawl.dev API.
 * It converts web pages into LLM-ready markdown format.
 */
@Service
public class FirecrawlService {

    private static final Logger log = LoggerFactory.getLogger(FirecrawlService.class);
    private final WebClient webClient;

    @Autowired
    private BrowserService browserService;

    @Autowired
    private ConfigService configService;

    private final AtomicInteger keyIndex = new AtomicInteger(0);

    public FirecrawlService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Rotates through the available Firecrawl API keys in a round-robin fashion.
     */
    private String getRotatedApiKey() {
        String keysCsv = configService.getEffectiveSetting("firecrawl_api_keys", "");
        if (keysCsv.isBlank()) return "";
        
        List<String> keys = Arrays.asList(keysCsv.split(","));
        int index = keyIndex.getAndIncrement() % keys.size();
        return keys.get(index).trim();
    }

    private String getBaseUrl() {
        return configService.getEffectiveSetting("firecrawl_api_url", "https://api.firecrawl.dev");
    }

    /**
     * Smart Scraper: Understands when Firecrawl is needed vs when a local browser is enough.
     * 
     * Logic:
     * 1. If the URL is a known AI interface (Kimi/Claude), use System Browser (Playwright).
     * 2. If scraping preference is manually set to SYSTEM_BROWSER, use it.
     * 3. Otherwise, try Firecrawl API for high-quality LLM-ready markdown.
     * 4. Fallback to System Browser if Firecrawl fails or no keys available.
     */
    public Mono<String> scrapeToMarkdown(String url) {
        // Heuristic: Is it a high-technical AI tool? These require session handling (Browser).
        boolean isAiTool = isWebAiInterface(url);
        
        String preference = configService.getEffectiveSetting("scraping_engine_preference", "AUTO");
        String apiKey = getRotatedApiKey();

        if (isAiTool || "SYSTEM_BROWSER".equalsIgnoreCase(preference) || apiKey.isBlank()) {
            log.info("[Scraper] Using System Browser (Playwright) for: {}", url);
            return browserService.scrapeToMarkdown(url); // Assumes BrowserService provides markdown
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[Firecrawl] API Key is missing. Scraping aborted for: {}", url);
            return Mono.error(new IllegalStateException("Firecrawl API Key is not configured."));
        }

        return webClient.post()
                .uri(getBaseUrl() + "/v0/scrape")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(Map.of("url", url))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> response.containsKey("markdown") ? (String) response.get("markdown") : "");
    }

    /**
     * Pro-Tip: Retrieves the list of high-technical web AI models from the database.
     * Used to dynamically route complex reasoning tasks to free web interfaces.
     */
    public List<String> getHighTechnicalWebAIs() {
        String list = configService.getEffectiveSetting("WEB_AI_HIGH_TECHNICAL", "kimi.ai,claude.ai");
        return Arrays.asList(list.split(","));
    }

    /**
     * Pro-Tip: Retrieves specific task models (Image/Video) from the database.
     */
    public List<String> getSpecificTaskWebAIs() {
        String list = configService.getEffectiveSetting("WEB_AI_SPECIFIC_TASK", "higgsfield.ai,runwayml.com");
        return Arrays.asList(list.split(","));
    }

    /**
     * Pro-Tip: Automatically determines if a URL belongs to a known free AI model
     * to apply specialized scraping/interaction logic.
     */
    public boolean isWebAiInterface(String url) {
        return getHighTechnicalWebAIs().stream().anyMatch(url::contains) ||
               getSpecificTaskWebAIs().stream().anyMatch(url::contains) ||
               configService.getEffectiveSetting("WEB_AI_CONVERSATIONAL", "").contains(url);
    }
}