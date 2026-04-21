package com.supremeai.api;

import com.supremeai.service.UnifiedQuotaService;
import com.supremeai.service.MultiAIConsensusService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Plugin/IDE Extension Controller
 * Enabled guest mode access.
 */
@RestController
@RequestMapping("/api/plugin")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PluginController {

    @Autowired
    private UnifiedQuotaService quotaService;

    @Autowired
    private MultiAIConsensusService consensusService;

    @PostMapping("/complete")
    public Mono<ResponseEntity<Object>> completeCode(@RequestBody Map<String, Object> request,
                                                     HttpServletRequest httpRequest) {
        String guestId = httpRequest.getRemoteAddr();
        
        if (!quotaService.checkAndIncrement(guestId, "GUEST")) {
            return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Daily limit exceeded")));
        }

        String prompt = (String) request.get("prompt");
        List<String> providers = (List<String>) request.getOrDefault("providers", List.of("meta-llama"));
        long timeout = ((Number) request.getOrDefault("timeout", 10000)).longValue();

        var result = consensusService.askAllAIs(prompt, providers, timeout);

        return Mono.just(ResponseEntity.ok(Map.of(
                "completion", result.getConsensus(),
                "confidence", result.getAverageConfidence()
        )));
    }

    @PostMapping("/chat")
    public Mono<ResponseEntity<Object>> pluginChat(@RequestBody Map<String, String> request,
                                                   HttpServletRequest httpRequest) {
        String guestId = httpRequest.getRemoteAddr();
        
        if (!quotaService.checkAndIncrement(guestId, "GUEST")) {
            return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Guest quota exceeded")));
        }

        String message = request.get("message");
        var result = consensusService.askAllAIs(message, List.of("meta-llama", "groq"), 15000);

        return Mono.just(ResponseEntity.ok(Map.of(
                "response", result.getConsensus(),
                "confidence", result.getAverageConfidence()
        )));
    }
}
