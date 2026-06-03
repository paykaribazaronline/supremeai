package com.supremeai.controller;

import com.supremeai.model.APIProvider;
import com.supremeai.service.ProviderMetadataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/providers")
public class ProvidersSuggestionController {
    public ProvidersSuggestionController(ProviderMetadataService providerMetadataService) {
        this.providerMetadataService = providerMetadataService;
    }



    /**
     * Model suggestion DTO returned to the frontend.
     * All data comes from Firestore api_providers at runtime — no hardcoded defaults.
     */
    public static class ModelSuggestion {
        private String id;
        private String name;
        private String provider;
        private String model;
        private String endpoint;

        public ModelSuggestion() {}

        public ModelSuggestion(String id, String name, String provider, String model, String endpoint) {
            this.id = id;
            this.name = name;
            this.provider = provider;
            this.model = model;
            this.endpoint = endpoint;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    }

    /**
     * Convert an APIProvider from Firestore cache into a ModelSuggestion.
     * No values are hardcoded — every field comes from the live metadata record.
     */
    private ModelSuggestion toSuggestion(APIProvider p) {
        String model = null;
        if (p.getModels() != null && !p.getModels().isEmpty()) {
            model = p.getModels().get(0);
        } else if (p.getModelName() != null && !p.getModelName().isBlank()) {
            model = p.getModelName();
        }
        return new ModelSuggestion(
            p.getId(),
            p.getName() != null && !p.getName().isBlank() ? p.getName() : p.getId(),
            p.getType(),
            model,
            p.getBaseUrl()
        );
    }

    private List<ModelSuggestion> allSuggestions() {
        Map<String, APIProvider> cache = providerMetadataService.getAllMetadata();
        return cache.values().stream()
                .filter(p -> p.getName() != null || p.getId() != null)
                .map(this::toSuggestion)
                .collect(Collectors.toList());
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<ModelSuggestion>> suggest(@RequestParam String q) {
        String query = q.toLowerCase();
        List<ModelSuggestion> matches = allSuggestions().stream()
            .filter(s ->
                (s.getName() != null && s.getName().toLowerCase().contains(query)) ||
                (s.getProvider() != null && s.getProvider().toLowerCase().contains(query)) ||
                (s.getModel() != null && s.getModel().toLowerCase().contains(query))
            )
            .collect(Collectors.toList());
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/templates")
    public ResponseEntity<List<ModelSuggestion>> allTemplates() {
        return ResponseEntity.ok(allSuggestions());
    }
}
