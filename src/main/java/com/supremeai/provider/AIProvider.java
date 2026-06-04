package com.supremeai.provider;

import java.util.Map;
import reactor.core.publisher.Mono;

public interface AIProvider {
  String getName();

  Map<String, Object> getCapabilities();

  Mono<String> generate(String prompt);

  default Mono<String> generateContent(String prompt) {
    return generate(prompt);
  }
}
