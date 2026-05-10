package com.supremeai.controller;

import com.supremeai.model.KnowledgeDomain;
import com.supremeai.model.KnowledgeRecommendation;
import com.supremeai.service.KnowledgeService;
import com.supremeai.response.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/knowledge")
@PreAuthorize("hasRole('ADMIN')")
public class KnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    /**
     * Get knowledge snapshot for dashboard KPIs
     */
    @GetMapping("/snapshot")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getKnowledgeSnapshot() {
        return knowledgeService.getKnowledgeSnapshot()
                .map(data -> ResponseEntity.ok(ApiResponse.ok(data)));
    }

    /**
     * Get all learning domains
     */
    @GetMapping("/domains")
    public Mono<ResponseEntity<ApiResponse<List<KnowledgeDomain>>>> getAllDomains() {
        return knowledgeService.getAllDomains()
                .collectList()
                .map(data -> ResponseEntity.ok(ApiResponse.ok(data)));
    }

    /**
     * Register a new learning domain
     */
    @PostMapping("/domains")
    public Mono<ResponseEntity<ApiResponse<KnowledgeDomain>>> registerDomain(
            @RequestParam String name,
            @RequestBody List<String> keywords) {
        return knowledgeService.registerDomain(name, keywords)
                .map(data -> ResponseEntity.ok(ApiResponse.ok(data)));
    }

    /**
     * Start learning for a domain
     */
    @PostMapping("/domains/{domainId}/start")
    public Mono<ResponseEntity<ApiResponse<KnowledgeDomain>>> startLearning(@PathVariable String domainId) {
        return knowledgeService.startLearning(domainId)
                .map(data -> ResponseEntity.ok(ApiResponse.ok(data)));
    }

    /**
     * Process learning job for a domain
     */
    @PostMapping("/domains/{domainId}/process")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> processLearning(@PathVariable String domainId) {
        return knowledgeService.processLearningJob(domainId)
                .map(data -> ResponseEntity.ok(ApiResponse.ok(data)));
    }

    /**
     * Delete a domain
     */
    @DeleteMapping("/domains/{domainId}")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteDomain(@PathVariable String domainId) {
        return knowledgeService.deleteDomain(domainId)
                .then(Mono.just(ResponseEntity.ok(ApiResponse.ok(null))));
    }

    /**
     * Get pending recommendations
     */
    @GetMapping("/recommendations")
    public Mono<ResponseEntity<ApiResponse<List<KnowledgeRecommendation>>>> getPendingRecommendations() {
        return knowledgeService.getPendingRecommendations()
                .collectList()
                .map(data -> ResponseEntity.ok(ApiResponse.ok(data)));
    }

    /**
     * Generate new recommendations
     */
    @PostMapping("/recommendations/generate")
    public Mono<ResponseEntity<ApiResponse<List<KnowledgeRecommendation>>>> generateRecommendations() {
        return knowledgeService.generateRecommendations()
                .map(data -> ResponseEntity.ok(ApiResponse.ok(data)));
    }

    /**
     * Approve a recommendation
     */
    @PostMapping("/recommendations/{recommendationId}/approve")
    public Mono<ResponseEntity<ApiResponse<KnowledgeDomain>>> approveRecommendation(
            @PathVariable String recommendationId,
            @RequestParam String domainName,
            @RequestBody List<String> keywords) {
        return knowledgeService.approveRecommendation(recommendationId, domainName, keywords)
                .map(data -> ResponseEntity.ok(ApiResponse.ok(data)));
    }

    /**
     * Decline a recommendation
     */
    @PostMapping("/recommendations/{recommendationId}/decline")
    public Mono<ResponseEntity<ApiResponse<KnowledgeRecommendation>>> declineRecommendation(@PathVariable String recommendationId) {
        return knowledgeService.declineRecommendation(recommendationId)
                .map(data -> ResponseEntity.ok(ApiResponse.ok(data)));
    }
}