package com.supremeai.service;

import com.supremeai.learning.active.ActiveInternetScraper.ScrapedIssue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AutonomousBrowserService {

  private static final Logger logger = LoggerFactory.getLogger(AutonomousBrowserService.class);

  @Autowired private ConfigService configService;

  @Autowired private WebClient.Builder webClientBuilder;

  /** Playwright browser automation server URL. Used as a fallback if not configured dynamically. */
  @Value("${supremeai.browser.automation-url:http://localhost:3001}")
  private String defaultPlaywrightUrl;

  /** Maximum autonomous research steps per Solo Mode session. */
  @Value("${supremeai.solo.max-steps:5}")
  private int soloMaxSteps;

  /** Per-step timeout in milliseconds for Solo Mode Playwright research. */
  @Value("${supremeai.solo.step-timeout-ms:30000}")
  private long soloStepTimeoutMs;

  /** Solo Mode deep research using the Playwright browser automation server. */
  public Mono<List<ScrapedIssue>> playwrightResearch(String prompt, List<String> keywords) {
    String automationUrl =
        configService.getEffectiveSetting("browser_automation_url", defaultPlaywrightUrl);

    if (automationUrl == null || automationUrl.isBlank()) {
      logger.debug("Playwright automation URL not configured — skipping deep research");
      return Mono.just(List.of());
    }

    String query = (keywords != null && !keywords.isEmpty()) ? String.join(" ", keywords) : prompt;

    // Phase 1: Discover URLs via DuckDuckGo HTML search
    String searchUrl =
        "https://html.duckduckgo.com/html/?q="
            + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);

    logger.info("[Solo Mode] Phase 1 — Playwright deep research for query: {}", query);

    return webClientBuilder
        .build()
        .get()
        .uri(searchUrl)
        .retrieve()
        .bodyToMono(String.class)
        .timeout(java.time.Duration.ofMillis(soloStepTimeoutMs))
        .map(searchHtml -> extractUrlsFromDdg(searchHtml))
        .flatMapMany(Flux::fromIterable)
        .take(soloMaxSteps)
        // Phase 2: Deep-scrape each URL via Playwright server
        .flatMap(
            url ->
                deepScrapeUrl(url, prompt)
                    .onErrorResume(
                        e -> {
                          logger.warn(
                              "[Solo Mode] Deep-scrape failed for {}: {}", url, e.getMessage());
                          return Mono.empty();
                        }))
        .collectList()
        .doOnSuccess(
            results ->
                logger.info(
                    "[Solo Mode] Playwright deep research complete — {} pages extracted",
                    results.size()))
        .doOnError(e -> logger.warn("[Solo Mode] Playwright research failed: {}", e.getMessage()))
        .onErrorResume(e -> Mono.just(List.of()));
  }

  /** Extracts result URLs from DuckDuckGo HTML search results. */
  private List<String> extractUrlsFromDdg(String html) {
    List<String> urls = new ArrayList<>();
    if (html == null || html.isBlank()) return urls;

    String lower = html.toLowerCase();
    int idx = 0;
    while (urls.size() < soloMaxSteps) {
      int aTag = lower.indexOf("class=\"result__a\"", idx);
      if (aTag == -1) break;
      int hrefStart = lower.indexOf("href=\"", aTag);
      if (hrefStart == -1) break;
      hrefStart += 6;
      int hrefEnd = lower.indexOf('"', hrefStart);
      if (hrefEnd == -1) break;
      String url = html.substring(hrefStart, hrefEnd);
      // DDG wraps external URLs in /l/?uddg=...
      if (url.startsWith("/l/")) {
        int uddgIdx = url.indexOf("uddg=");
        if (uddgIdx != -1) {
          url =
              java.net.URLDecoder.decode(
                  url.substring(uddgIdx + 5), java.nio.charset.StandardCharsets.UTF_8);
        }
      }
      if (url.startsWith("http") && !url.contains("duckduckgo.com")) {
        urls.add(url);
      }
      idx = hrefEnd;
    }
    return urls;
  }

  /** Navigates to a URL via the Playwright server and extracts the full page text. */
  private Mono<ScrapedIssue> deepScrapeUrl(String url, String prompt) {
    String automationUrl =
        configService.getEffectiveSetting("browser_automation_url", defaultPlaywrightUrl);
    Map<String, Object> payload = new HashMap<>();
    payload.put("url", url);
    payload.put("useStealth", true);
    payload.put("useVpn", true);
    payload.put("humanInTheLoop", true);
    payload.put("storeSessionCookies", true);

    return webClientBuilder
        .build()
        .post()
        .uri(automationUrl + "/navigate")
        .bodyValue(payload)
        .retrieve()
        .bodyToMono(Void.class)
        .timeout(java.time.Duration.ofMillis(soloStepTimeoutMs))
        .then(Mono.delay(java.time.Duration.ofMillis(2000)))
        .then(
            webClientBuilder
                .build()
                .get()
                .uri(automationUrl + "/extract-text")
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(java.time.Duration.ofMillis(soloStepTimeoutMs))
                .map(
                    body -> {
                      String text =
                          body != null && body.get("text") != null
                              ? body.get("text").toString().trim()
                              : "";
                      String title =
                          url.replaceFirst("https?://", "")
                              .replaceFirst("/$", "")
                              .replaceFirst("www\\.", "");
                      if (text.length() > 3000) {
                        text = text.substring(0, 3000) + "...";
                      }
                      ScrapedIssue issue =
                          new ScrapedIssue("Deep Research: " + title, text, "Playwright Browser");
                      issue.setSourceAuthority(0.85);
                      issue.setRawConfidence(0.80);
                      return issue;
                    }));
  }
}
