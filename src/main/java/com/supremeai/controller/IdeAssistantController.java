package com.supremeai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/ide/assistant")
@CrossOrigin(origins = "*")
public class IdeAssistantController {

    private final com.supremeai.provider.AIProviderFactory providerFactory;

    public IdeAssistantController(com.supremeai.provider.AIProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    @PostMapping("/ask")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ResponseEntity<Map<String, String>>> askAssistant(@jakarta.validation.Valid @RequestBody com.supremeai.dto.IdeAssistantRequest request) {
        String prompt = request.getPrompt();
        String context = request.getContext();

        // Use new AIProvider interface
        return Mono.fromCallable(() -> {
            com.supremeai.provider.AIProvider provider = providerFactory.getProvider(request.getProvider());
            String response = provider.generate(prompt)
                    .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                    .block(reactor.timeout.Duration.ofSeconds(30));
            return ResponseEntity.ok(Map.of(
                    "response", response != null ? response : "",
                    "code", "",
                    "provider", provider.getName()
            ));
        }).onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage()))));
    }
}
