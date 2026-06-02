package com.supremeai.service;

import com.supremeai.dto.ChatRequest;
import com.supremeai.model.ChatMessage;
import com.supremeai.repository.ChatHistoryRepository;
import com.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final AutonomousQuestioningEngine questioningEngine;
    private final MultiAIVotingService votingService;
    private final ChatIntelligenceService chatIntelligenceService;
    private final EnhancedLearningService enhancedLearningService;
    private final NeuralChatService neuralChatService;
    private final ChatHistoryRepository chatHistoryRepository;

    @Autowired
    public ChatService(AutonomousQuestioningEngine questioningEngine,
                       MultiAIVotingService votingService,
                       ChatIntelligenceService chatIntelligenceService,
                       EnhancedLearningService enhancedLearningService,
                       NeuralChatService neuralChatService,
                       ChatHistoryRepository chatHistoryRepository) {
        this.questioningEngine = questioningEngine;
        this.votingService = votingService;
        this.chatIntelligenceService = chatIntelligenceService;
        this.enhancedLearningService = enhancedLearningService;
        this.neuralChatService = neuralChatService;
        this.chatHistoryRepository = chatHistoryRepository;
    }

    public Mono<Map<String, Object>> sendMessage(String message, boolean skipValidation) {
        if (message == null || message.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Message is required");
            return Mono.just(errorResponse);
        }

        return questioningEngine.validateAndQuestion(message, AutonomousQuestioningEngine.RequestType.GENERAL_AI)
            .flatMap(validation -> executeResponseForRequest(message, validation, skipValidation));
    }

    private Mono<Map<String, Object>> executeResponseForRequest(String message,
            AutonomousQuestioningEngine.ValidationResult validation, boolean skipValidation) {

        if (!skipValidation && validation != null && !validation.isComplete()) {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "CLARIFICATION_REQUIRED");
            response.put("clarity", validation.getClarityScore());
            response.put("questions", validation.getClarifyingQuestions());
            response.put("options", validation.getOptions());
            return Mono.just(response);
        }

        if (validation != null && validation.getIntentType() == AutonomousQuestioningEngine.IntentType.GREETING) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "হ্যালো! আমি SupremeAI. আজ আমি আপনার প্রজেক্ট বা কোডিংয়ের কাজে কীভাবে সহায়তা করতে পারি?");
            response.put("confidence", 1.0);
            response.put("sources", List.of("Local Memory"));
            response.put("tier", "LEVEL_0_BYPASS");
            response.put("pipeline", "direct_greeting");
            response.put("localMode", true);
            response.put("mode", "greeting");
            response.put("intent", "GREETING");
            return Mono.just(response);
        }

        boolean preferLocal = (validation != null
            && validation.getResponseStrategy() == AutonomousQuestioningEngine.ResponseStrategy.DIRECT_ANSWER)
            || (skipValidation && isSkipValidationDirectAnswer(message));

        if (preferLocal) {
            return neuralChatService.generateIntelligentResponse(message)
                .flatMap(neuralResponse -> {
                    if (shouldUseLocalResponse(neuralResponse)) {
                        return Mono.just(buildLocalResponse(neuralResponse, message));
                    }
                    logger.info("[ChatService] Local direct answer produced weak response; falling back to voting.");
                    return executeVotingAndResponse(message);
                })
                .onErrorResume(err -> {
                    logger.warn("[ChatService] Local direct answer path failed: {}. Falling back to voting.", err.getMessage());
                    return executeVotingAndResponse(message);
                });
        }

        return executeVotingAndResponse(message);
    }

    private boolean isSkipValidationDirectAnswer(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.startsWith("what is") || lower.startsWith("what are") || lower.startsWith("explain")
            || lower.startsWith("define") || lower.startsWith("tell me about") || lower.startsWith("who is")
            || lower.startsWith("when is");
    }

    private boolean shouldUseLocalResponse(NeuralChatService.NeuralResponse neuralResponse) {
        if (neuralResponse == null) return false;
        String tier = neuralResponse.getTier();
        if (tier == null) return false;
        return !"STUB_FALLBACK".equals(tier) && neuralResponse.getConfidence() >= 0.55;
    }

    private Map<String, Object> buildLocalResponse(NeuralChatService.NeuralResponse neuralResponse, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", neuralResponse.getAnswer());
        response.put("confidence", neuralResponse.getConfidence());
        response.put("sources", neuralResponse.getSources());
        response.put("tier", neuralResponse.getTier());
        response.put("pipeline", neuralResponse.getPipeline());
        response.put("localMode", true);

        var intent = chatIntelligenceService.classifyIntent(message);
        response.put("mode", intent.name().toLowerCase());
        response.put("intent", intent.name());

        return response;
    }

    private Mono<Map<String, Object>> executeVotingAndResponse(String message) {
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

                            var intent = chatIntelligenceService.classifyIntent(message);
                            response.put("mode", intent.name().toLowerCase());
                            response.put("intent", intent.name());

                            return response;
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

                var intent = chatIntelligenceService.classifyIntent(message);
                response.put("mode", intent.name().toLowerCase());
                response.put("intent", intent.name());

                chatIntelligenceService.handleIntelligence(
                    "default",
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

                return Mono.just(response);
            })
            .switchIfEmpty(Mono.defer(() -> {
                logger.info("Voting service returned empty. Routing to intelligent offline fallback pipeline...");
                if (neuralChatService == null) {
                    return Mono.just(Map.of("error", "AI services temporarily unavailable"));
                }
                return neuralChatService.generateIntelligentResponse(message)
                    .map(neuralResponse -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", neuralResponse.getAnswer());
                        response.put("confidence", neuralResponse.getConfidence());
                        response.put("sources", neuralResponse.getSources());
                        response.put("tier", neuralResponse.getTier());
                        response.put("pipeline", neuralResponse.getPipeline());
                        response.put("localMode", true);

                        var intent = chatIntelligenceService.classifyIntent(message);
                        response.put("mode", intent.name().toLowerCase());
                        response.put("intent", intent.name());

                        return response;
                    })
                    .onErrorResume(err -> {
                        logger.error("Intelligent fallback pipeline failed as well: {}", err.getMessage());
                        return Mono.just(Map.of("error", "AI services temporarily unavailable"));
                    });
            }))
            .onErrorResume(e -> {
                logger.error("Failed to get response via voting system. Routing to intelligent offline fallback pipeline...", e);
                if (neuralChatService == null) {
                    return Mono.just(Map.of("error", "AI services temporarily unavailable"));
                }
                return neuralChatService.generateIntelligentResponse(message)
                    .map(neuralResponse -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", neuralResponse.getAnswer());
                        response.put("confidence", neuralResponse.getConfidence());
                        response.put("sources", neuralResponse.getSources());
                        response.put("tier", neuralResponse.getTier());
                        response.put("pipeline", neuralResponse.getPipeline());
                        response.put("localMode", true);

                        var intent = chatIntelligenceService.classifyIntent(message);
                        response.put("mode", intent.name().toLowerCase());
                        response.put("intent", intent.name());

                        return response;
                    })
                    .onErrorResume(err -> {
                        logger.error("Intelligent fallback pipeline failed as well: {}", err.getMessage());
                        return Mono.just(Map.of("error", "AI services temporarily unavailable"));
                    });
            });
    }

    public Mono<Map<String, Object>> processChatWithHistory(ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = "default-session";
        }

        String finalSessionId = sessionId;
        String userMessage = request.getMessage();

        ChatMessage userMsg = new ChatMessage();
        userMsg.setId(UUID.randomUUID().toString());
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

                return Mono.just(fullHistoryBuilder.toString())
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
                                            aiMsg.setId(UUID.randomUUID().toString());
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
                                                    return response;
                                                });
                                        });
                                }

                                ChatMessage aiMsg = new ChatMessage();
                                aiMsg.setId(UUID.randomUUID().toString());
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
                                        return response;
                                    });
                            });
                    })
                    .onErrorResume(e -> {
                        logger.error("Failed to process chat with history for session: {}", finalSessionId, e);

                        return neuralChatService.generateIntelligentResponse(userMessage)
                            .flatMap(neuralResponse -> {
                                String localResponse = neuralResponse.getAnswer();

                                ChatMessage aiMsg = new ChatMessage();
                                aiMsg.setId(UUID.randomUUID().toString());
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
                                        return response;
                                    });
                            });
                    });
            });
    }

    public Flux<String> streamChat(ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = "default-session";
        }

        String finalSessionId = sessionId;
        String userMessage = request.getMessage();

        ChatMessage userMsg = new ChatMessage();
        userMsg.setId(UUID.randomUUID().toString());
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

                return Mono.just(fullHistoryBuilder.toString())
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

    public void submitFeedback(String messageId, boolean helpful, String userMessage, String aiResponse) {
        logger.info("Received feedback for message: {}, helpful: {}", messageId, helpful);

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
    }

    public Map<String, Object> getHistory(String agent, int limit) {
        List<Object> messages = new ArrayList<>();
        return Map.of(
            "messages", messages,
            "count", 0,
            "agent", agent != null ? agent : "default"
        );
    }

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

    public String generateLocalFallbackResponse(String prompt) {
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
