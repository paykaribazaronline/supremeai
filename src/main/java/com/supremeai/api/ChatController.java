package com.supremeai.api;

import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import com.supremeai.service.GuestQuotaService;
import com.supremeai.service.quota.QuotaExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private SystemLearningRepository learningRepository;

    @Autowired
    private GuestQuotaService guestQuotaService;

    @PostMapping("/send")
    public Mono<ResponseEntity<Object>> sendMessage(@RequestBody Map<String, String> request,
                                              @RequestHeader(value = "Authorization", required = false) String authHeader,
                                              HttpServletRequest httpRequest) {
        String message = request.get("message");
        String provider = request.getOrDefault("provider", "meta-llama");

        // Guest quota enforcement - no API key required
        String guestId = guestQuotaService.extractGuestIdentifier(httpRequest);
        
        try {
            guestQuotaService.validateAndIncrement(guestId);
        } catch (QuotaExceededException e) {
            return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error", "Guest quota exceeded",
                            "message", "You have reached your daily limit. Please try again tomorrow or register for an account.",
                            "currentUsage", e.getCurrentUsage(),
                            "quotaLimit", e.getQuotaLimit()
                    )));
        }

        // Sync with Firestore SystemLearning collection
        SystemLearning learningEntry = new SystemLearning();
        learningEntry.setId(UUID.randomUUID().toString());
        learningEntry.setTopic("User Interaction");
        learningEntry.setCategory("CHAT_INPUT");
        learningEntry.setContent(message);
        learningEntry.setLearnedAt(LocalDateTime.now());
        
        // Mock response for now, to be replaced with actual AI service call
        String aiResponse = "SupremeAI: I've received your message about '" + message + "'. I am currently processing this through " + provider + ".";

        return learningRepository.save(learningEntry)
                .map(saved -> (ResponseEntity<Object>) ResponseEntity.ok((Object) Map.of(
                        "response", aiResponse,
                        "status", "LEARNED",
                        "learningId", saved.getId(),
                        "guestRemaining", guestQuotaService.getRemainingQuota(guestId),
                        "guestLimit", guestQuotaService.getGuestQuotaLimit()
                )))
                .onErrorResume(e -> Mono.just((ResponseEntity<Object>) ResponseEntity.ok((Object) Map.of(
                        "response", aiResponse,
                        "status", "OFFLINE_MODE",
                        "error", e.getMessage(),
                        "guestRemaining", guestQuotaService.getRemainingQuota(guestId),
                        "guestLimit", guestQuotaService.getGuestQuotaLimit()
                ))));
    }
}
