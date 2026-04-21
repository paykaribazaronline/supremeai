package com.supremeai.controller;

import com.supremeai.model.APIProvider;
import com.supremeai.repository.ProviderRepository;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/providers")
public class ProvidersController extends BaseAdminController<APIProvider, String> {

    private final ProviderRepository providerRepository;

    public ProvidersController(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @GetMapping
    public Mono<ResponseEntity<Object>> getProviders() {
        return wrapList(providerRepository.findAll(), "providers");
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> updateProvider(@RequestBody APIProvider provider) {
        return wrapSave(providerRepository.save(provider), "Provider updated successfully", provider);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Object>> deleteProvider(@PathVariable String id) {
        return wrapDelete(providerRepository.deleteById(id), "Provider deleted");
    }
}
