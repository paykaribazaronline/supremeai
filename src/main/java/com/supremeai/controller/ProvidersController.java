package com.supremeai.controller;

import com.supremeai.model.APIProvider;
import com.supremeai.repository.ProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/providers")
public class ProvidersController {

    private static final Logger logger = LoggerFactory.getLogger(ProvidersController.class);

    @Autowired
    private ProviderRepository providerRepository;

    @GetMapping
    public Mono<ResponseEntity<Object>> getProviders() {
        return providerRepository.findAll()
                .collectList()
                .map(providers -> ResponseEntity.ok((Object) Map.of("providers", providers)))
                .onErrorResume(e -> {
                    logger.error("Failed to fetch providers", e);
                    Map<String, Object> errorBody = Map.of("error", "Failed to fetch providers: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(500).body((Object) errorBody));
                });
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> updateProvider(@RequestBody APIProvider provider) {
        return providerRepository.save(provider)
                .map(saved -> ResponseEntity.ok((Object) Map.of("message", "Provider updated successfully", "provider", saved)))
                .onErrorResume(e -> {
                    logger.error("Failed to update provider", e);
                    Map<String, Object> errorBody = Map.of("error", "Failed to update provider: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(500).body((Object) errorBody));
                });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Object>> deleteProvider(@PathVariable String id) {
        return providerRepository.deleteById(id)
                .then(Mono.just(ResponseEntity.ok((Object) Map.of("message", "Provider deleted"))))
                .onErrorResume(e -> {
                    logger.error("Failed to delete provider with id: {}", id, e);
                    Map<String, Object> errorBody = Map.of("error", "Failed to delete provider: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(500).body((Object) errorBody));
                });
    }
}
