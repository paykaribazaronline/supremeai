package com.supremeai.api;

import com.supremeai.service.GuestQuotaService;
import com.supremeai.service.MultiAIConsensusService;
import com.supremeai.service.quota.QuotaExceededException;
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
 *
 * Enables guest mode access for VS Code, JetBrains IDEs and other editor extensions
 * No API key required - uses guest quota system controlled by admin
 */
@RestController
@RequestMapping("/api/plugin")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PluginController {

    @Autowired
    private GuestQuotaService guestQuotaService;

    @Autowired
    private MultiAIConsensusService consensusService;

    /**
     * Completion endpoint for IDE extensions - Guest mode enabled
     * No API key required, admin controls daily guest quota
     */
    @PostMapping("/complete")
    public Mono<ResponseEntity<Object>> completeCode(@RequestBody Map<String, Object> request,
                                                     HttpServletRequest httpRequest) {
        String guestId = guestQuotaService.extractGuestIdentifier(httpRequest);
        
        // Guest quota enforcement - no API key required
        try {
            guestQuotaService.validateAndIncrement(guestId);
        } catch (QuotaExceededException e) {
            return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error", "Daily limit exceeded",
                            "message", "Guest mode daily limit reached. Please add your API key or try again tomorrow.",
                            "limit", e.getQuotaLimit(),
                            "used", e.getCurrentUsage(),
                            "resetIn", "Midnight UTC"
                    )));
        }

        String prompt = (String) request.get("prompt");
        List<String> providers = (List<String>) request.getOrDefault("providers", List.of("meta-llama"));
        long timeout = ((Number) request.getOrDefault("timeout", 10000)).longValue();

        var result = consensusService.askAllAIs(prompt, providers, timeout);

        return Mono.just(ResponseEntity.ok(Map.of(
                "completion", result.getConsensus(),
                "confidence", result.getAverageConfidence(),
                "providerVotes", result.getVotes(),
                "guestRemaining", guestQuotaService.getRemainingQuota(guestId),
                "guestLimit", guestQuotaService.getGuestQuotaLimit()
        )));
    }

    /**
     * Get current guest status for plugin
     */
    @GetMapping("/status")
    public ResponseEntity<Object> getPluginStatus(HttpServletRequest httpRequest) {
        String guestId = guestQuotaService.extractGuestIdentifier(httpRequest);
        return ResponseEntity.ok(Map.of(
                "mode", "guest",
                "guestEnabled", true,
                "apiKeyRequired", false,
                "remaining", guestQuotaService.getRemainingQuota(guestId),
                "limit", guestQuotaService.getGuestQuotaLimit(),
                "quotaControlledByAdmin", true
        ));
    }

    /**
     * Chat endpoint for plugin - guest mode enabled
     */
    @PostMapping("/chat")
    public Mono<ResponseEntity<Object>> pluginChat(@RequestBody Map<String, String> request,
                                                   HttpServletRequest httpRequest) {
        String guestId = guestQuotaService.extractGuestIdentifier(httpRequest);
        
        try {
            guestQuotaService.validateAndIncrement(guestId);
        } catch (QuotaExceededException e) {
            return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error", "Guest quota exceeded",
                            "limit", e.getQuotaLimit(),
                            "used", e.getCurrentUsage()
                    )));
        }

        String message = request.get("message");
        var result = consensusService.askAllAIs(message, List.of("meta-llama", "groq"), 15000);

        return Mono.just(ResponseEntity.ok(Map.of(
                "response", result.getConsensus(),
                "confidence", result.getAverageConfidence(),
                "guestRemaining", guestQuotaService.getRemainingQuota(guestId)
        )));
    }
}
