package com.supremeai.controller;

import com.supremeai.model.UserGuide;
import com.supremeai.repository.UserGuideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Controller for user video guides/tutorials.
 * Supports multiple languages (Bangla, English).
 * 
 * Endpoints:
 *   GET    /api/guides              - List all published guides
 *   GET    /api/guides/{id}         - Get a specific guide
 *   GET    /api/guides/category/{cat} - Get guides by category
 *   GET    /api/guides/tag/{tag}    - Get guides by tag
 * 
 * Admin endpoints (protected by /api/admin/** pattern):
 *   POST   /api/admin/guides        - Create a new guide
 *   PUT    /api/admin/guides/{id}   - Update a guide
 *   DELETE /api/admin/guides/{id}   - Delete a guide
 */
@RestController
@RequestMapping("/api/guides")
public class GuideController {

    @Autowired
    private UserGuideRepository userGuideRepository;

    /**
     * GET /api/guides - Get all published video guides sorted by order.
     */
    @GetMapping
    public Flux<UserGuide> getAllGuides() {
        return userGuideRepository.findByIsPublished(true);
    }

    /**
     * GET /api/guides/{id} - Get a specific guide by ID.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<UserGuide>> getGuide(@PathVariable String id) {
        return userGuideRepository.findById(id)
                .filter(guide -> guide.getIsPublished() != null && guide.getIsPublished())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/guides/category/{category} - Get published guides by category.
     */
    @GetMapping("/category/{category}")
    public Flux<UserGuide> getGuidesByCategory(@PathVariable String category) {
        return userGuideRepository.findByCategoryAndIsPublished(category, true);
    }

    /**
     * GET /api/guides/tag/{tag} - Get published guides by tag.
     */
    @GetMapping("/tag/{tag}")
    public Flux<UserGuide> getGuidesByTag(@PathVariable String tag) {
        return userGuideRepository.findByTagsContaining(tag);
    }

    /**
     * GET /api/guides/languages - Get available languages for guides.
     */
    @GetMapping("/languages")
    public ResponseEntity<Map<String, String>> getAvailableLanguages() {
        // Return the supported languages
        // "en" = English, "bn" = Bangla
        return ResponseEntity.ok(Map.of(
            "en", "English",
            "bn", "বাংলা (Bangla)"
        ));
    }
}
