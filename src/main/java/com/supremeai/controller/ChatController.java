package com.supremeai.controller;

import com.supremeai.service.AutonomousQuestioningEngine;
import com.supremeai.service.MultiAIVotingService;
import com.supremeai.service.MultiAIConsensusService;
import com.supremeai.service.EnhancedLearningService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.supremeai.dto.ChatRequest;
import com.supremeai.dto.FeedbackRequest;
import com.supremeai.repository.ChatHistoryRepository;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.model.ChatMessage;
import java.time.LocalDateTime;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired(required = false)
    private MultiAIVotingService consensusService;

    @Autowired
    private AutonomousQuestioningEngine questioningEngine;

    @Autowired
    private MultiAIVotingService votingService;

    @Autowired(required = false)
    private EnhancedLearningService enhancedLearningService;

    @Autowired
    private com.supremeai.service.ChatIntelligenceService intelligenceService;

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private com.supremeai.service.ContextSummarizerService summarizerService;

    @Autowired
    private com.supremeai.service.NeuralChatService neuralChatService;

    private final CircuitBreaker aiCircuitBreaker;
    private final Retry aiRetry;

    public ChatController() {
        // Initialize circuit breaker and retry for AI operations
        this.aiCircuitBreaker = CircuitBreaker.ofDefaults("aiVotingService");
        this.aiRetry = Retry.ofDefaults("aiVotingService");
    }

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER', 'GUEST')")
    public Mono<ResponseEntity<Object>> sendMessage(@Valid @RequestBody ChatRequest request) {
        String message = request.getMessage();
        boolean skipValidation = request.isSkipValidation();

        if (message == null || message.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "Message is required")));
        }

        logger.info("Received chat message: {}", message);

        Mono<ResponseEntity<Object>> validationMono;
        if (!skipValidation) {
            validationMono = questioningEngine.validateAndQuestion(message, AutonomousQuestioningEngine.RequestType.GENERAL_AI)
                .flatMap(validation -> {
                    if (validation != null && !validation.isComplete() && validation.hasQuestions()) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("type", "CLARIFICATION_REQUIRED");
                        response.put("questions", validation.getClarifyingQuestions());
                        response.put("clarityScore", validation.getClarityScore());
                        response.put("message", "I need more information before I can give you a quality answer.");
                        return Mono.just(ResponseEntity.ok((Object) response));
                    }
                    return Mono.empty();
                });
        } else {
            validationMono = Mono.empty();
        }

        return validationMono
            .switchIfEmpty(Mono.defer(() -> executeVotingAndResponse(request, message)));
    }

    private Mono<ResponseEntity<Object>> executeVotingAndResponse(ChatRequest request, String message) {
        return votingService.executeEnsembleVoting(message, null, 15000L)
            .flatMap(votingResult -> {
                if (votingResult == null || votingResult.getBestResponse() == null) {
                    return neuralChatService.generateIntelligentResponse(message)
                        .map(neuralResponse -> {
                            Map<String, Object> response = new HashMap<>();
                            response.put("message", neuralResponse.getAnswer());
                            response.put("confidence", neuralResponse.getConfidence());
                            response.put("sources", neuralResponse.getSources());
                            response.put("tier", neuralResponse.getTier());
                            response.put("pipeline", neuralResponse.getPipeline());
                            response.put("localMode", true);

                            // Intent classification
                            var intent = intelligenceService.classifyIntent(message);
                            response.put("mode", intent.name().toLowerCase());
                            response.put("intent", intent.name());

                            return ResponseEntity.ok((Object) response);
                        });
                }

                String bestResponse = votingResult.getBestResponse();
                Double confidence = votingResult.getAverageConfidence();

                Map<String, Object> response = new HashMap<>();
                response.put("message", bestResponse);
                response.put("verdict", votingResult.getVerdict());
                response.put("confidence", confidence);
                response.put("modelsUsed", votingResult.getTotalModelsUsed());
                response.put("processingTimeMs", votingResult.getProcessingTimeMs());
                response.put("timestamp", java.time.Instant.now().toString());

                // Intent classification
                var intent = intelligenceService.classifyIntent(message);
                response.put("mode", intent.name().toLowerCase());
                response.put("intent", intent.name());

                // Side-effects (Learning) - non-blocking execution
                intelligenceService.handleIntelligence(
                    request.getAgentId() != null ? request.getAgentId() : "default",
                    message,
                    intent,
                    "ADMIN",
                    confidence
                ).subscribe(
                    result -> logger.debug("Intelligence handled successfully"),
                    error -> logger.error("Error handling intelligence: {}", error.getMessage())
                );

                if (enhancedLearningService != null) {
                    enhancedLearningService.learnFromNLPInteraction(
                        message,
                        bestResponse,
                        "voting_system",
                        confidence != null ? confidence : 0.5,
                        Map.of("modelsUsed", votingResult.getTotalModelsUsed())
                    ).subscribe(
                        saved -> logger.info("Successfully learned from NLP interaction"),
                        error -> logger.error("Failed to capture NLP learning: {}", error.getMessage())
                    );
                }

                return Mono.just(ResponseEntity.ok((Object) response));
            })
            .onErrorResume(e -> {
                logger.error("Failed to get response via voting system. Routing to intelligent offline fallback pipeline...", e);
                return neuralChatService.generateIntelligentResponse(message)
                    .map(neuralResponse -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", neuralResponse.getAnswer());
                        response.put("confidence", neuralResponse.getConfidence());
                        response.put("sources", neuralResponse.getSources());
                        response.put("tier", neuralResponse.getTier());
                        response.put("pipeline", neuralResponse.getPipeline());
                        response.put("localMode", true);

                        // Intent classification
                        var intent = intelligenceService.classifyIntent(message);
                        response.put("mode", intent.name().toLowerCase());
                        response.put("intent", intent.name());

                        return ResponseEntity.ok((Object) response);
                    });
            });
    }


    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    public Mono<ResponseEntity<Object>> getHistory(
            @RequestParam(required = false) String agent,
            @RequestParam(defaultValue = "50") int limit) {
        return Mono.just(ResponseEntity.ok(Map.of(
            "messages", new ArrayList<>(),
            "count", 0,
            "agent", agent != null ? agent : "default"
        )));
    }

    @PostMapping("/feedback")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER', 'GUEST')")
    public Mono<ResponseEntity<Object>> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        String messageId = request.getMessageId();
        boolean helpful = request.isHelpful();
        String userMessage = request.getUserMessage();
        String aiResponse = request.getAiResponse();

        logger.info("Received feedback for message: {}, helpful: {}", messageId, helpful);

        // Capture learning from feedback - this is valuable for NLP improvement
        if (enhancedLearningService != null && userMessage != null && aiResponse != null && messageId != null) {
            double qualityScore = helpful ? 1.0 : 0.3;
            enhancedLearningService.learnFromNLPInteraction(
                    userMessage,
                    aiResponse,
                    "feedback_system",
                    qualityScore,
                    Map.of("messageId", messageId, "helpful", helpful)
            ).subscribe(
                saved -> logger.info("Successfully captured learning from feedback"),
                error -> logger.error("Failed to capture feedback learning: {}", error.getMessage())
            );
        }

        return Mono.just(ResponseEntity.ok(Map.of("status", "received")));
    }

    /**
     * ডায়নামিকভাবে মোড সনাক্ত করার হেল্পার মেথড
     */
    private String detectMode(String message) {
        String lowerMsg = message.toLowerCase();
        if (lowerMsg.contains("architect") || lowerMsg.contains("design") || lowerMsg.contains("structure")) {
            return "architect";
        } else if (lowerMsg.contains("debug") || lowerMsg.contains("fix") || lowerMsg.contains("error") || lowerMsg.contains("issue")) {
            return "debug";
        } else if (lowerMsg.contains("review") || lowerMsg.contains("audit") || lowerMsg.contains("analyze")) {
            return "review";
        } else if (lowerMsg.contains("ask") || lowerMsg.contains("what") || lowerMsg.contains("how") || lowerMsg.contains("explain")) {
            return "ask";
        } else if (lowerMsg.contains("orchestrate") || lowerMsg.contains("manage") || lowerMsg.contains("coordinate")) {
            return "orchestrator";
        } else {
            return "code";
        }
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Object>> health() {
        return Mono.just(ResponseEntity.ok(Map.of(
            "status", "UP",
            "autonomous_questioning", "ACTIVE",
            "voting_system", "ACTIVE"
        )));
    }

    @PostMapping("/message")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER', 'GUEST')")
    public Mono<ResponseEntity<Object>> handleChatMessage(@Valid @RequestBody ChatRequest request) {
        return processChatWithHistory(request);
    }

    @PostMapping(value = "/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER', 'GUEST')")
    public Flux<String> handleChatStream(@Valid @RequestBody ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = "default-session";
        }
        
        String finalSessionId = sessionId;
        String userMessage = request.getMessage();

        ChatMessage userMsg = new ChatMessage();
        userMsg.setId(java.util.UUID.randomUUID().toString());
        userMsg.setUserId(finalSessionId);
        userMsg.setContent(userMessage);
        userMsg.setRole("user");
        userMsg.setTimestamp(LocalDateTime.now());

        return chatHistoryRepository.save(userMsg)
            .thenMany(chatHistoryRepository.findByUserIdOrderByTimestampAsc(finalSessionId))
            .collectList()
            .flatMapMany(history -> {
                StringBuilder fullHistoryBuilder = new StringBuilder();
                for (ChatMessage pastMsg : history) {
                    String role = pastMsg.getRole() != null ? pastMsg.getRole() : (pastMsg.isAdmin() ? "admin" : "user");
                    fullHistoryBuilder.append(role.toUpperCase()).append(": ").append(pastMsg.getContent()).append("\n");
                }
                
                if (history.isEmpty() || !history.get(history.size() - 1).getContent().equals(userMessage)) {
                    fullHistoryBuilder.append("USER: ").append(userMessage).append("\n");
                }

                return summarizerService.summarizeContext(fullHistoryBuilder.toString())
                    .flatMapMany(summarizedHistory -> {
                        String contextualPrompt = "You are SupremeAI, a highly intelligent coding and development assistant. Maintain a friendly and helpful tone.\n" +
                            "Below is the conversation context:\n\n" +
                            summarizedHistory + "\n\nAI: ";

                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        return votingService.streamVotes(contextualPrompt, null, 15000L)
                            .map(vote -> {
                                try {
                                    Map<String, Object> data = new HashMap<>();
                                    data.put("provider", vote.getProviderName());
                                    data.put("response", vote.getResponse());
                                    data.put("confidence", vote.getConfidence());
                                    return mapper.writeValueAsString(data);
                                } catch (Exception e) {
                                    return "{\"error\":\"Serialization failed\"}";
                                }
                            });
                    });
            });
    }

    private Mono<ResponseEntity<Object>> processChatWithHistory(ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = "default-session";
        }

        String finalSessionId = sessionId;
        String userMessage = request.getMessage();

        ChatMessage userMsg = new ChatMessage();
        userMsg.setId(java.util.UUID.randomUUID().toString());
        userMsg.setUserId(finalSessionId);
        userMsg.setContent(userMessage);
        userMsg.setRole("user");
        userMsg.setTimestamp(LocalDateTime.now());

        return chatHistoryRepository.save(userMsg)
            .thenMany(chatHistoryRepository.findByUserIdOrderByTimestampAsc(finalSessionId))
            .collectList()
            .flatMap(history -> {
                StringBuilder fullHistoryBuilder = new StringBuilder();
                for (ChatMessage pastMsg : history) {
                    String role = pastMsg.getRole() != null ? pastMsg.getRole() : (pastMsg.isAdmin() ? "admin" : "user");
                    fullHistoryBuilder.append(role.toUpperCase()).append(": ").append(pastMsg.getContent()).append("\n");
                }

                if (history.isEmpty() || !history.get(history.size() - 1).getContent().equals(userMessage)) {
                    fullHistoryBuilder.append("USER: ").append(userMessage).append("\n");
                }

                return summarizerService.summarizeContext(fullHistoryBuilder.toString());
            })
            .flatMap(summarizedHistory -> {
                String contextualPrompt = "You are SupremeAI, a highly intelligent coding and development assistant. Maintain a friendly and helpful tone.\n" +
                    "Below is the conversation context:\n\n" +
                    summarizedHistory + "\n\nAI: ";

                return votingService.executeEnsembleVoting(contextualPrompt, null, 15000L)
                    .flatMap(votingResult -> {
                        String rawResponse = (votingResult != null) ? votingResult.getBestResponse() : null;
                        if (rawResponse == null) {
                            return neuralChatService.generateIntelligentResponse(userMessage)
                                .flatMap(neuralResponse -> {
                                    String bestResponse = neuralResponse.getAnswer();
                                    ChatMessage aiMsg = new ChatMessage();
                                    aiMsg.setId(java.util.UUID.randomUUID().toString());
                                    aiMsg.setUserId(finalSessionId);
                                    aiMsg.setContent(bestResponse);
                                    aiMsg.setRole("ai");
                                    aiMsg.setTimestamp(LocalDateTime.now());

                                    return chatHistoryRepository.save(aiMsg)
                                        .map(savedAiMsg -> {
                                            Map<String, Object> response = new HashMap<>();
                                            response.put("success", true);
                                            response.put("message", "success");
                                            response.put("response", bestResponse);
                                            response.put("sessionId", finalSessionId);
                                            response.put("timestamp", java.time.Instant.now().toString());
                                            response.put("localMode", true);
                                            response.put("sources", neuralResponse.getSources());
                                            response.put("tier", neuralResponse.getTier());
                                            response.put("pipeline", neuralResponse.getPipeline());
                                            return ResponseEntity.ok((Object) response);
                                        });
                                });
                        }

                        ChatMessage aiMsg = new ChatMessage();
                        aiMsg.setId(java.util.UUID.randomUUID().toString());
                        aiMsg.setUserId(finalSessionId);
                        aiMsg.setContent(rawResponse);
                        aiMsg.setRole("ai");
                        aiMsg.setTimestamp(LocalDateTime.now());

                        return chatHistoryRepository.save(aiMsg)
                            .map(savedAiMsg -> {
                                Map<String, Object> response = new HashMap<>();
                                response.put("success", true);
                                response.put("message", "success");
                                response.put("response", rawResponse);
                                response.put("sessionId", finalSessionId);
                                response.put("timestamp", java.time.Instant.now().toString());
                                response.put("localMode", true);
                                return ResponseEntity.ok((Object) response);
                            });
                    });
            })
            .onErrorResume(e -> {
                logger.error("Failed to process chat with history for session: {}", finalSessionId, e);

                return neuralChatService.generateIntelligentResponse(userMessage)
                    .flatMap(neuralResponse -> {
                        String localResponse = neuralResponse.getAnswer();

                        ChatMessage aiMsg = new ChatMessage();
                        aiMsg.setId(java.util.UUID.randomUUID().toString());
                        aiMsg.setUserId(finalSessionId);
                        aiMsg.setContent(localResponse);
                        aiMsg.setRole("ai");
                        aiMsg.setTimestamp(LocalDateTime.now());

                        return chatHistoryRepository.save(aiMsg)
                            .map(savedAiMsg -> {
                                Map<String, Object> response = new HashMap<>();
                                response.put("success", true);
                                response.put("message", "AI temporarily unavailable, using intelligent offline fallback response");
                                response.put("response", localResponse);
                                response.put("sessionId", finalSessionId);
                                response.put("timestamp", java.time.Instant.now().toString());
                                response.put("localMode", true);
                                response.put("sources", neuralResponse.getSources());
                                response.put("tier", neuralResponse.getTier());
                                response.put("pipeline", neuralResponse.getPipeline());
                                return ResponseEntity.ok((Object) response);
                            });
                    });
            });
    }

    private String generateLocalFallbackResponse(String prompt) {
        String p = prompt.toLowerCase();
        if (p.contains("hello") || p.contains("hi") || p.contains("হ্যালো") || p.contains("নমস্কার")) {
            return "হ্যালো! আমি সুপ্রিমএআই। কোনো বাইরের API কী ছাড়াই আমি আপনার সাহায্য করছি।";
        } else if (p.contains("help") || p.contains("সাহায্য") || p.contains("কীভাবে")) {
            return "আমি কীভাবে সাহায্য করতে পারি:\n• কোড লিখতে ও বিশ্লেষণ করতে\n• বাগ ফিক্স করতে\n• প্রজেক্ট গঠন বল্ড করতে";
        } else if (p.contains("react") || p.contains("javascript")) {
            return "১. **প্রোজেক্ট সেটআপ**: `npx create-react-app my-app`\n২. **কম্পোনেন্ট**: ফাংশনাল কম্পোনেন্ট ব্যবহার করুন\n৩. **স্টেট**: useState এবং useEffect হুক ব্যবহার করুন";
        } else if (p.contains("java") || p.contains("spring")) {
            return "১. **স্প্রিং বুট**: spring initializr ব্যবহার করুন\n২. **REST**: @RestController যোগ করুন\n৩. **ডাটাবেস**: JPA এবং PostgreSQL কনফিগার করুন";
        }
        return "I can help you with many topics! Here's what I know about:\n\n" +
            "**Web:** React, Vue, Angular, Next.js, HTML, CSS, JavaScript, TypeScript\n" +
            "**Backend:** Java, Spring Boot, Python, Flask, Node.js, Express\n" +
            "**Mobile:** Flutter, Dart\n" +
            "**DevOps:** Docker, Kubernetes, Git, Linux, CI/CD\n" +
            "**Cloud:** AWS, GCP, Firebase\n" +
            "**Databases:** SQL, PostgreSQL, MongoDB, Firestore\n\n" +
            "Your question: \"" + prompt + "\"\n\n" +
            "Please try rephrasing with one of these topics for a detailed answer with code examples!";
    }
}
