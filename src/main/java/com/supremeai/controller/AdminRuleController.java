package com.supremeai.controller;

import com.supremeai.audit.Audited;
import com.supremeai.model.ProtocolRule;
import com.supremeai.repository.ProtocolRuleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/rules")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRuleController {

    private final ProtocolRuleRepository protocolRuleRepository;

    public AdminRuleController(ProtocolRuleRepository protocolRuleRepository) {
        this.protocolRuleRepository = protocolRuleRepository;
    }

    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> getRules(@RequestParam(defaultValue = "true") boolean active_only) {
        return (active_only
                ? protocolRuleRepository.findByActive(true).collectList()
                : protocolRuleRepository.findAll().collectList())
                .map(rules -> {
                    List<Map<String, Object>> ruleList = rules.stream()
                            .map(this::entityToMap)
                            .toList();
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "rules", ruleList
                    ));
                });
    }

    @PostMapping
    @Audited(resource = "protocol_rule", action = "CREATE")
    public Mono<ResponseEntity<Map<String, Object>>> createRule(@RequestBody Map<String, Object> request) {
        ProtocolRule rule = new ProtocolRule();
        rule.setId(java.util.UUID.randomUUID().toString());
        rule.setName((String) request.get("name"));
        rule.setType((String) request.get("type"));
        rule.setPattern((String) request.get("pattern"));
        rule.setAction((String) request.get("action"));
        rule.setSeverity((String) request.get("severity"));
        rule.setActive(request.get("isActive") != null ? (Boolean) request.get("isActive") : true);
        rule.setCreatedAt(LocalDateTime.now());

        return protocolRuleRepository.save(rule)
                .map(saved -> ResponseEntity.ok(Map.of(
                        "success", true,
                        "rule", entityToMap(saved)
                )));
    }

    @PutMapping("/{id}")
    @Audited(resource = "protocol_rule", action = "UPDATE")
    public Mono<ResponseEntity<Map<String, Object>>> updateRule(@PathVariable String id,
                                                                  @RequestBody Map<String, Object> request) {
        return protocolRuleRepository.findById(id)
                .flatMap(rule -> {
                    rule.setName((String) request.get("name"));
                    rule.setType((String) request.get("type"));
                    rule.setPattern((String) request.get("pattern"));
                    rule.setAction((String) request.get("action"));
                    rule.setSeverity((String) request.get("severity"));
                    rule.setActive(request.get("isActive") != null ? (Boolean) request.get("isActive") : true);
                    rule.setUpdatedAt(LocalDateTime.now());
                    return protocolRuleRepository.save(rule);
                })
                .map(updated -> ResponseEntity.ok(Map.of(
                        "success", true,
                        "rule", entityToMap(updated)
                )))
                .switchIfEmpty(Mono.just(ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "Rule not found"
                ))));
    }

    @DeleteMapping("/{id}")
    @Audited(resource = "protocol_rule", action = "DELETE")
    public Mono<ResponseEntity<Map<String, Object>>> deleteRule(@PathVariable String id) {
        return protocolRuleRepository.deleteById(id)
                .thenReturn(ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Rule deleted successfully"
                )));
    }

    private Map<String, Object> entityToMap(ProtocolRule rule) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", rule.getId());
        map.put("name", rule.getName());
        map.put("type", rule.getType());
        map.put("pattern", rule.getPattern());
        map.put("action", rule.getAction());
        map.put("severity", rule.getSeverity());
        map.put("isActive", rule.isActive());
        map.put("createdAt", rule.getCreatedAt() != null ? rule.getCreatedAt().toString() : null);
        map.put("updatedAt", rule.getUpdatedAt() != null ? rule.getUpdatedAt().toString() : null);
        map.put("lastTriggered", rule.getLastTriggered());
        return map;
    }
}