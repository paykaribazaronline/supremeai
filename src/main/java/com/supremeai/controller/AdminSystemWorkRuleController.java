package com.supremeai.controller;

import com.supremeai.model.SystemWorkRule;
import com.supremeai.response.ApiResponse;
import com.supremeai.service.SystemWorkRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the System Work Rules admin page.
 *
 * Endpoints
 * ─────────
 *  GET    /api/admin/system-work-rules              — list all rules
 *  GET    /api/admin/system-work-rules/active       — list active rules only
 *  GET    /api/admin/system-work-rules/{ruleKey}    — fetch single rule
 *  POST   /api/admin/system-work-rules              — create
 *  PUT    /api/admin/system-work-rules/{ruleKey}    — update (full replace)
 *  DELETE /api/admin/system-work-rules/{ruleKey}    — delete
 *  POST   /api/admin/system-work-rules/seed/defaults — seed 6 well-known defaults
 *  POST   /api/admin/system-work-rules/sync/{ruleKey}— re-trigger conflict propagation
 */
@RestController
@RequestMapping("/api/admin/system-work-rules")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSystemWorkRuleController {

    private static final Logger log = LoggerFactory.getLogger(AdminSystemWorkRuleController.class);

    @Autowired
    private SystemWorkRuleService ruleService;

    // ─── LIST ────────────────────────────────────────────────────────────────

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<SystemWorkRule>>>> listAll() {
        return ruleService.getAllRules()
                .collectList()
                .map(rules -> ResponseEntity.ok(ApiResponse.ok(rules)));
    }

    @GetMapping("/active")
    public Mono<ResponseEntity<ApiResponse<List<SystemWorkRule>>>> listActive() {
        return ruleService.getActiveRules()
                .collectList()
                .map(rules -> ResponseEntity.ok(ApiResponse.ok(rules)));
    }

    @GetMapping("/category/{category}")
    public Mono<ResponseEntity<ApiResponse<List<SystemWorkRule>>>> listByCategory(
            @PathVariable String category) {
        return ruleService.getRulesByCategory(category)
                .collectList()
                .map(rules -> ResponseEntity.ok(ApiResponse.ok(rules)));
    }

    // ─── SINGLE ──────────────────────────────────────────────────────────────

    @GetMapping("/{ruleKey}")
    public Mono<ResponseEntity<ApiResponse<SystemWorkRule>>> getByRuleKey(
            @PathVariable String ruleKey) {
        return ruleService.getRuleByKey(ruleKey)
                .map(rule -> ResponseEntity.ok(ApiResponse.ok(rule)))
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.status(404)
                                .body(ApiResponse.<SystemWorkRule>error("Rule not found: " + ruleKey))));
    }

    // ─── CREATE ──────────────────────────────────────────────────────────────

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<SystemWorkRule>>> create(@RequestBody SystemWorkRule rule) {
        return ruleService.saveRule(rule)
                .map(saved -> {
                    log.info("[AdminSystemWorkRuleController] Created rule '{}'", saved.getRuleKey());
                    return ResponseEntity.ok(ApiResponse.ok(saved));
                });
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    @PutMapping("/{ruleKey}")
    public Mono<ResponseEntity<ApiResponse<SystemWorkRule>>> update(
            @PathVariable String ruleKey, @RequestBody SystemWorkRule updates) {

        return ruleService.getRuleByKey(ruleKey)
                .flatMap(existing -> {
                    if (updates.getRuleKey() != null && !updates.getRuleKey().isBlank()) {
                        existing.setRuleKey(updates.getRuleKey());
                    }
                    if (updates.getCategory() != null)    existing.setCategory(updates.getCategory());
                    if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
                    if (updates.getValue() != null)       existing.setValue(updates.getValue());
                    if (updates.getValueType() != null)   existing.setValueType(updates.getValueType());
                    if (updates.getStructuredValue() != null) existing.setStructuredValue(updates.getStructuredValue());
                    if (updates.getTargetDoc() != null)   existing.setTargetDoc(updates.getTargetDoc());
                    if (updates.getTargetField() != null) existing.setTargetField(updates.getTargetField());
                    if (updates.getPriority() > 0)        existing.setPriority(updates.getPriority());
                    if (updates.getChangeNote() != null)  existing.setChangeNote(updates.getChangeNote());
                    // enabled sentinel: -1 means "no change"; 0/1 are explicit
                    existing.setEnabled(updates.isEnabled());
                    return ruleService.saveRule(existing);
                })
                .map(saved -> ResponseEntity.ok(ApiResponse.ok(saved)))
                .defaultIfEmpty(ResponseEntity.status(404)
                        .body(ApiResponse.<SystemWorkRule>error("Rule not found: " + ruleKey)))
                .doOnError(e -> log.error("[AdminSystemWorkRuleController] Update failed: {}", e.getMessage(), e));
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    @DeleteMapping("/{ruleKey}")
    public Mono<ResponseEntity<ApiResponse<Void>>> delete(@PathVariable String ruleKey) {
        return ruleService.deleteRule(ruleKey)
                .thenReturn(ResponseEntity.ok(ApiResponse.<Void>ok(null)))
                .defaultIfEmpty(ResponseEntity.status(404)
                        .body(ApiResponse.<Void>error("Rule not found: " + ruleKey)))
                .doOnError(e -> log.error("[AdminSystemWorkRuleController] Delete failed: {}", e.getMessage(), e));
    }

    // ─── SEED DEFAULTS ───────────────────────────────────────────────────────

    @PostMapping("/seed/defaults")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> seedDefaults() {
        return ruleService.ensureDefaults()
                .map(results -> ResponseEntity.ok(ApiResponse.ok(results)))
                .doOnSuccess(r -> log.info("[AdminSystemWorkRuleController] Defaults seed complete={}", r));
    }

    // ─── MANUAL SYNC TRIGGER ─────────────────────────────────────────────────

    @PostMapping("/sync/{ruleKey}")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> sync(
            @PathVariable String ruleKey) {
        
        return ruleService.getRuleByKey(ruleKey)
                .flatMap(rule -> {
                    String targetDoc   = rule.getTargetDoc();
                    String targetField = rule.getTargetField() != null ? rule.getTargetField() : rule.getRuleKey();
                    return ruleService.saveRule(rule)
                            .map(saved -> {
                                Map<String, Object> data = new java.util.LinkedHashMap<>();
                                data.put("ruleKey", saved.getRuleKey());
                                data.put("value", saved.getValue());
                                data.put("targetDoc", targetDoc);
                                data.put("targetField", targetField);
                                data.put("lastSyncStatus", saved.getLastSyncStatus());
                                data.put("lastSyncedAt", saved.getLastSyncedAt());
                                return data;
                            });
                })
                .map(data -> ResponseEntity.ok(ApiResponse.ok(data)))
                .defaultIfEmpty(ResponseEntity.status(404)
                        .body(ApiResponse.error("Rule not found: " + ruleKey)));

    }
}
