package com.supremeai.service;

import java.lang.management.ManagementFactory;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class SoloModeService {

  private static final Logger log = LoggerFactory.getLogger(SoloModeService.class);
  private final WebClient.Builder webClientBuilder;

  @Autowired
  public SoloModeService(WebClient.Builder webClientBuilder) {
    this.webClientBuilder = webClientBuilder;
  }

  public long getSystemRamBytes() {
    try {
      var osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
      return osBean.getTotalPhysicalMemorySize();
    } catch (Exception e) {
      log.warn("Failed to detect physical RAM, defaulting to 4GB", e);
      return 4L * 1024 * 1024 * 1024;
    }
  }

  public int getCpuCoresCount() {
    return Runtime.getRuntime().availableProcessors();
  }

  public String selectOllamaModel() {
    long ramBytes = getSystemRamBytes();
    double ramGb = (double) ramBytes / (1024 * 1024 * 1024);
    log.info("Detected system RAM: {} GB, CPU Cores: {}", String.format("%.2f", ramGb), getCpuCoresCount());
    if (ramGb >= 8.0) {
      return "llama3:8b";
    } else if (ramGb >= 4.0) {
      return "phi3:mini";
    } else {
      return "tinyllama";
    }
  }

  public Mono<String> askLocalModel(String prompt) {
    String modelName = selectOllamaModel();
    log.info("SoloMode: Calling local model {} for prompt", modelName);

    Map<String, Object> requestBody = Map.of(
        "model", modelName,
        "prompt", prompt,
        "stream", false
    );

    return webClientBuilder.build()
        .post()
        .uri("http://localhost:11434/api/generate")
        .bodyValue(requestBody)
        .retrieve()
        .bodyToMono(Map.class)
        .map(response -> {
          if (response != null && response.containsKey("response")) {
            return (String) response.get("response");
          }
          return "Error: Empty response from local model.";
        })
        .onErrorResume(e -> {
          log.warn("Local model API call failed, falling back to local fallback response: {}", e.getMessage());
          return Mono.just("[Offline Fallback Response] Local Ollama service is unreachable. Solution for: " + prompt);
        });
  }
}
