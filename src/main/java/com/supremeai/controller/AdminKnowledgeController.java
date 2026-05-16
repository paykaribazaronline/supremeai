package com.supremeai.controller;

import com.supremeai.model.KnowledgeDomain;
import com.supremeai.model.KnowledgeRecommendation;
import com.supremeai.repository.KnowledgeDomainRepository;
import com.supremeai.repository.KnowledgeRecommendationRepository;
import com.supremeai.service.KnowledgeService;
import com.supremeai.response.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/knowledge")
@PreAuthorize("hasRole('ADMIN')")
public class AdminKnowledgeController extends BaseAdminController<Object, String> {

    private final KnowledgeDomainRepository domainRepository;
    private final KnowledgeRecommendationRepository recommendationRepository;
    private final KnowledgeService knowledgeService;

    @Autowired
    public AdminKnowledgeController(KnowledgeDomainRepository domainRepository, 
                                    KnowledgeRecommendationRepository recommendationRepository,
                                    KnowledgeService knowledgeService) {
        this.domainRepository = domainRepository;
        this.recommendationRepository = recommendationRepository;
        this.knowledgeService = knowledgeService;
    }

    @GetMapping("/snapshot")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getKnowledgeSnapshot() {
        try {
            Map<String, Object> data = knowledgeService.getKnowledgeSnapshot().block();
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (Exception e) {
            return handleErrorSync("Failed to get snapshot", e);
        }
    }

    @GetMapping("/domains")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDomains() {
        try {
            List<KnowledgeDomain> list = domainRepository.findAll().collectList().block();
            List<Map<String, Object>> uiDomains = list.stream().map(d -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", d.getId());
                m.put("name", d.getName());
                m.put("status", d.getStatus() != null ? d.getStatus().name() : "UNKNOWN");
                m.put("keywords", d.getKeywords() != null ? d.getKeywords() : List.of());
                m.put("knowledgeCount", d.getNodesDiscovered() != null ? d.getNodesDiscovered() : 0);
                return m;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.ok(uiDomains));
        } catch (Exception e) {
            return handleErrorSync("Failed to fetch domains", e);
        }
    }

    @PostMapping("/domains")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createDomain(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        List<String> keywords = (List<String>) body.get("keywords");
        
        if (name == null || name.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Name is required"));
        }

        KnowledgeDomain domain = new KnowledgeDomain(name, keywords != null ? keywords : List.of());
        try {
            KnowledgeDomain saved = domainRepository.save(domain).block();
            return ResponseEntity.ok(ApiResponse.ok(Map.of("domain", saved)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to create domain"));
        }
    }

    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecommendations() {
        try {
            List<KnowledgeRecommendation> list = recommendationRepository.findAll().collectList().block();
            List<Map<String, Object>> uiRecs = list.stream().map(r -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", r.getId());
                m.put("title", r.getTopic());
                m.put("description", r.getReasoning());
                m.put("confidence", r.getConfidence() != null ? r.getConfidence() : 0.0);
                m.put("suggestedKeywords", r.getKeywords() != null ? r.getKeywords() : List.of());
                m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : LocalDateTime.now().toString());
                return m;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.ok(uiRecs));
        } catch (Exception e) {
            return handleErrorSync("Failed to fetch recommendations", e);
        }
    }

    @PostMapping("/recommendations/{id}/approve")
    public ResponseEntity<ApiResponse<String>> approveRecommendation(@PathVariable String id) {
        try {
            KnowledgeRecommendation rec = recommendationRepository.findById(id).block();
            if (rec == null) return ResponseEntity.notFound().build();

            rec.setStatus(KnowledgeRecommendation.Status.APPROVED);
            rec.setProcessedAt(LocalDateTime.now());
            
            KnowledgeDomain newDomain = new KnowledgeDomain(rec.getTopic(), rec.getKeywords());
            
            recommendationRepository.save(rec).block();
            domainRepository.save(newDomain).block();
            
            return ResponseEntity.ok(ApiResponse.ok("Recommendation approved and domain created"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Approval failed"));
        }
    }

    @PostMapping("/recommendations/{id}/decline")
    public ResponseEntity<ApiResponse<String>> declineRecommendation(@PathVariable String id) {
        try {
            KnowledgeRecommendation rec = recommendationRepository.findById(id).block();
            if (rec == null) return ResponseEntity.notFound().build();

            rec.setStatus(KnowledgeRecommendation.Status.DECLINED);
            rec.setProcessedAt(LocalDateTime.now());
            recommendationRepository.save(rec).block();
            
            return ResponseEntity.ok(ApiResponse.ok("Recommendation declined"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Operation failed"));
        }
    }

    @PostMapping("/domains/{domainId}/start")
    public ResponseEntity<ApiResponse<KnowledgeDomain>> startLearning(@PathVariable String domainId) {
        try {
            KnowledgeDomain data = knowledgeService.startLearning(domainId).block();
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to start learning"));
        }
    }

    @PostMapping("/domains/{domainId}/process")
    public ResponseEntity<ApiResponse<Map<String, Object>>> processLearning(@PathVariable String domainId) {
        try {
            Map<String, Object> data = knowledgeService.processLearningJob(domainId).block();
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Processing failed"));
        }
    }

    @PostMapping("/recommendations/generate")
    public ResponseEntity<ApiResponse<List<KnowledgeRecommendation>>> generateRecommendations() {
        try {
            List<KnowledgeRecommendation> data = knowledgeService.generateRecommendations().block();
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Generation failed"));
        }
    }
}

