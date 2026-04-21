package com.supremeai.api;

import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import com.supremeai.service.FastPathAIService;
import com.supremeai.service.UnifiedQuotaService;
import com.supremeai.service.HumanUnderstandingService;
import com.supremeai.service.quota.QuotaExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private SystemLearningRepository learningRepository;

    @Autowired
    private UnifiedQuotaService quotaService;

    @Autowired
    private FastPathAIService fastPathAIService;

    @Autowired
    private HumanUnderstandingService humanUnderstandingService;

    @PostMapping("/send")
    public Mono<ResponseEntity<Object>> sendMessage(@RequestBody Map<String, String> request,
                                              @RequestHeader(value = "Authorization", required = false) String authHeader,
                                              HttpServletRequest httpRequest) {
        String message = request.get("message");
        String provider = request.getOrDefault("provider", "meta-llama");

        // Guest quota enforcement - no API key required
        // Using IP address as identifier for now
        String guestId = httpRequest.getRemoteAddr();
        
        try {
            quotaService.checkAndIncrement(guestId, "GUEST");
        } catch (Exception e) {
            return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error", "Guest quota exceeded",
                            "message", "You have reached your daily limit."
                    )));
        }

        // Sync with Firestore SystemLearning collection
        SystemLearning learningEntry = new SystemLearning();
        learningEntry.setId(UUID.randomUUID().toString());
        learningEntry.setTopic("User Interaction");
        learningEntry.setCategory("CHAT_INPUT");
        learningEntry.setContent(message);
        learningEntry.setLearnedAt(LocalDateTime.now());
        
        // Fast path AI generation
        String aiResponse = fastPathAIService.generateParallel(message, "groq", "ollama");

        // Automatically analyze human factors on EVERY interaction
        humanUnderstandingService.analyzeHumanFactors(message, aiResponse);

        learningRepository.save(learningEntry)
            .subscribe();

        return Mono.just(ResponseEntity.ok(Map.of(
                "response", aiResponse,
                "status", "LEARNED",
                "learningId", learningEntry.getId()
        )));
    }
}
