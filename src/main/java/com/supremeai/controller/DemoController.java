package com.supremeai.controller;

import com.supremeai.service.SupremeAIBrain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Demo controller for testing 4-layer resilience without authentication.
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {
    
    private final SupremeAIBrain brain;
    
    @Autowired
    public DemoController(SupremeAIBrain brain) {
        this.brain = brain;
    }
    
    @GetMapping("/think")
    public Mono<Map<String, Object>> think(@RequestParam String prompt) {
        return brain.thinkDemo(prompt)
            .map(answer -> {
                Map<String, Object> result = new HashMap<>();
                result.put("prompt", prompt);
                result.put("response", answer);
                result.put("resilience", "4-layer active (Core → Firestore → Browser → HelperAI)");
                return result;
            });
    }
}