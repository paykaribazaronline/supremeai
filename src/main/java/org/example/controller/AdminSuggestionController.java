package org.example.controller;

import org.example.model.AdminSuggestion;
import org.example.service.AdminSuggestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * AdminSuggestionController — REST API for admin panel tab suggestions.
 *
 * POST /api/admin/suggestions        — submit a suggestion (save or apply now)
 * GET  /api/admin/suggestions        — list all suggestions
 * GET  /api/admin/suggestions?tab=X  — list suggestions for a specific tab
 * POST /api/admin/suggestions/{id}/apply — trigger immediate apply of a saved suggestion
 */
@RestController
@RequestMapping("/api/admin/suggestions")
public class AdminSuggestionController {

    @Autowired
    private AdminSuggestionService suggestionService;

    @PostMapping
    public ResponseEntity<?> submitSuggestion(@RequestBody AdminSuggestion suggestion) {
        if (suggestion.getSuggestion() == null || suggestion.getSuggestion().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Suggestion text must not be empty"));
        }
        if (suggestion.getTabKey() == null || suggestion.getTabKey().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "tabKey is required"));
        }
        AdminSuggestion saved = suggestionService.submit(suggestion);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<AdminSuggestion>> listSuggestions(
            @RequestParam(required = false) String tab) {
        if (tab != null && !tab.isBlank()) {
            return ResponseEntity.ok(suggestionService.listByTab(tab));
        }
        return ResponseEntity.ok(suggestionService.listAll());
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<?> applyNow(@PathVariable String id) {
        try {
            AdminSuggestion updated = suggestionService.applyNow(id);
            return ResponseEntity.ok(updated);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
