package com.supremeai.service;

import com.supremeai.model.APIProvider;
import com.supremeai.repository.ProviderRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.Comparator;

/**
 * Service to select the best available AI provider for a specific task role.
 * (নির্দিষ্ট কাজের জন্য সেরা এআই প্রোভাইডার খুঁজে বের করার সার্ভিস)
 */
@Service
public class ModelSelectorService {
    public ModelSelectorService(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }



    /**
     * Get the best provider for a specific role (e.g., 'coding', 'security').
     * If no provider is explicitly assigned to that role, falls back to the highest priority active provider.
     */
    public Mono<APIProvider> getBestProviderForRole(String role) {
        return providerRepository.findAll()
                .filter(p -> "active".equalsIgnoreCase(p.getStatus()))
                .filter(p -> p.getAssignedRoles() != null && p.getAssignedRoles().contains(role))
                .sort(Comparator.comparingInt(APIProvider::getPriority).reversed())
                .next()
                .switchIfEmpty(
                    // Fallback to general chat or any active provider
                    providerRepository.findAll()
                            .filter(p -> "active".equalsIgnoreCase(p.getStatus()))
                            .sort(Comparator.comparingInt(APIProvider::getPriority).reversed())
                            .next()
                );
    }
}
