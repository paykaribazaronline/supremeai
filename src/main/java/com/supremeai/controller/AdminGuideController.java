package com.supremeai.controller;

import com.supremeai.response.ApiResponse;
import java.util.Map;

import com.supremeai.model.UserGuide;
import com.supremeai.repository.UserGuideRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller for managing admin guides and documentation.
 */
@RestController
@RequestMapping("/api/admin/guides")
@PreAuthorize("hasRole('ADMIN')")
public class AdminGuideController extends BaseAdminController<UserGuide, String> {

    private final UserGuideRepository repository;

    public AdminGuideController(UserGuideRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getAllGuides() {
        return wrapList(repository.findAll(), "guides");
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> saveGuide(@RequestBody UserGuide guide) {
        return wrapSave(repository.save(guide), "guide");
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<String>>> deleteGuide(@PathVariable String id) {
        return wrapDelete(repository.deleteById(id), "Guide deleted successfully");
    }
}
