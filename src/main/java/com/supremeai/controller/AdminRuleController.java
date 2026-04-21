package com.supremeai.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simplified Rule-based control for System Evolution.
 * Allows managing system behavior directly from the Admin Dashboard,
 * replacing complex self-evolving algorithms.
 */
@RestController
@RequestMapping("/api/v1/admin/rules")
public class AdminRuleController {

    // Centralized rule store, managed via Admin Dashboard
    private final Map<String, String> activeRules = new ConcurrentHashMap<>();

    @PostMapping("/update")
    public String setSystemRule(@RequestParam String key, @RequestParam String value) {
        activeRules.put(key, value);
        return "Rule updated: " + key + " = " + value;
    }

    @GetMapping("/list")
    public Map<String, String> getAllRules() {
        return activeRules;
    }

    @DeleteMapping("/remove/{key}")
    public String removeRule(@PathVariable String key) {
        activeRules.remove(key);
        return "Rule removed: " + key;
    }
}
