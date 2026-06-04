package com.supremeai.client;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** Simple HTTP client for the Pocket Lab tiny model service. */
@Component
public class PocketLabClient {

  private static final Logger log = LoggerFactory.getLogger(PocketLabClient.class);

  private final WebClient webClient;
  private final String pocketLabUrl;

  public PocketLabClient(@Value("${pocketlab.url:#{null}}") String configuredUrl) {
    this.pocketLabUrl =
        (configuredUrl != null && !configuredUrl.isBlank())
            ? configuredUrl
            : System.getenv("POCKET_LAB_URL");
    this.webClient = WebClient.builder().build();
  }

  public Mono<String> predict(String prompt) {
    if (prompt == null || prompt.isBlank()) {
      return Mono.empty();
    }
    if (pocketLabUrl == null || pocketLabUrl.isBlank()) {
      log.debug("[PocketLab] No URL configured; skipping local inference.");
      return Mono.empty();
    }
    Map<String, String> payload = new HashMap<>();
    payload.put("prompt", prompt);
    return webClient
        .post()
        .uri(pocketLabUrl)
        .bodyValue(payload)
        .retrieve()
        .bodyToMono(String.class)
        .filter(resp -> resp != null && !resp.isBlank())
        .doOnNext(
            resp -> log.debug("[PocketLab] Inference response received from {}", pocketLabUrl))
        .onErrorResume(
            e -> {
              log.warn("[PocketLab] Local inference failed: {}", e.getMessage());
              return Mono.empty();
            });
  }
}
