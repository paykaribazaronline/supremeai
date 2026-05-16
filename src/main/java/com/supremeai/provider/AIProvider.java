package com.supremeai.provider;

import reactor.core.publisher.Mono;
import java.util.Map;

public interface AIProvider {
    String getName();
    Map<String, Object> getCapabilities();
    Mono<String> generate(String prompt);

    default Mono<String> generateContent(String prompt) {
        return generate(prompt);
    }
}
