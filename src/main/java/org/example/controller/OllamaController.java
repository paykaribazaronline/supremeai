package org.example.controller;

import org.example.model.OllamaProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/ollama")
public class OllamaController {

    @Autowired
    private OllamaProvider ollamaProvider;

    @GetMapping("/generate")
    public String generate(@RequestParam String prompt) {
        return ollamaProvider.generateCode(prompt);
    }
    
    @GetMapping("/health")
    public boolean health() {
        return ollamaProvider.isHealthy();
    }
}
