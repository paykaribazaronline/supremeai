package com.supremeai.service;

import com.supremeai.model.*;
import com.supremeai.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import com.supremeai.fallback.AIFallbackOrchestrator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ChatProcessingService.class);

    private final ChatClassifier chatClassifier;
    private final ChatRuleRepository chatRuleRepository;
    private final ChatPlanRepository chatPlanRepository;
    private final ChatCommandRepository chatCommandRepository;
    private final ChatConfirmationRepository chatConfirmationRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final ChatAdminActionRepository chatAdminActionRepository;
    private final AIFallbackOrchestrator fallbackOrchestrator;

    private final AIProviderService aiProviderService;
    private final com.supremeai.service.browser.BrowserService browserService;
    private final AdminProviderValidationService validationService;
    private final CyberSecuritySkillService cyberSecuritySkillService;
    private final EnhancedLearningService enhancedLearningService;
    private final KnowledgeService knowledgeService;

    // In-memory pending confirmations (like Flask)
    private final Map<String, PendingItem> pendingConfirmations = new ConcurrentHashMap<>();

    public ChatProcessingService(ChatClassifier chatClassifier,
            ChatRuleRepository chatRuleRepository,
            ChatPlanRepository chatPlanRepository,
            ChatCommandRepository chatCommandRepository,
            ChatConfirmationRepository chatConfirmationRepository,
            ChatHistoryRepository chatHistoryRepository,
            ChatAdminActionRepository chatAdminActionRepository,
            AIFallbackOrchestrator fallbackOrchestrator,
            AIProviderService aiProviderService,
            com.supremeai.service.browser.BrowserService browserService,
            AdminProviderValidationService validationService,
            CyberSecuritySkillService cyberSecuritySkillService,
            EnhancedLearningService enhancedLearningService,
            KnowledgeService knowledgeService) {
        this.chatClassifier = chatClassifier;
        this.chatRuleRepository = chatRuleRepository;
        this.chatPlanRepository = chatPlanRepository;
        this.chatCommandRepository = chatCommandRepository;
        this.chatConfirmationRepository = chatConfirmationRepository;
        this.chatHistoryRepository = chatHistoryRepository;
        this.chatAdminActionRepository = chatAdminActionRepository;
        this.fallbackOrchestrator = fallbackOrchestrator;
        this.aiProviderService = aiProviderService;
        this.browserService = browserService;
        this.validationService = validationService;
        this.cyberSecuritySkillService = cyberSecuritySkillService;
        this.enhancedLearningService = enhancedLearningService;
        this.knowledgeService = knowledgeService;
    }

    public Mono<Map<String, Object>> processMessage(String userId, String message, boolean isAdmin) {
        // Sanitize user input to prevent XSS
        String sanitizedMessage = Jsoup.clean(message, Safelist.basic()
                .addTags("br", "p", "strong", "em", "code")
                .addProtocols("a", "href", "https"));

        // Save chat message
        ChatMessage chatMsg = new ChatMessage(userId, sanitizedMessage, isAdmin);
        chatMsg.setId(generateId("chat"));

        return chatHistoryRepository.save(chatMsg)
                .flatMap(savedMsg -> {
                    String chatId = savedMsg.getId();

                    // Classify message
                    ChatClassifier.ClassificationResult classification = chatClassifier.classify(message);
                    ChatClassifier.ChatType chatType = classification.getChatType();
                    double confidence = classification.getConfidence();
                    String reason = classification.getReason();

                    Map<String, Object> result = new HashMap<>();
                    result.put("chat_id", chatId);
                    result.put("message", message);
                    result.put("chat_type", chatType.name().toLowerCase());
                    result.put("confidence", confidence);
                    result.put("reason", reason);
                    result.put("needs_confirmation", false);
                    result.put("item_id", null);
                    result.put("item_type", null);

                    if (chatType != ChatClassifier.ChatType.NORMAL) {
                        // Extract content
                        String content = chatClassifier.extractContent(message, chatType);

                        // Save item based on type
                        return savePendingItem(chatType, chatId, content, confidence, userId)
                                .map(itemId -> {
                                    result.put("needs_confirmation", true);
                                    result.put("item_id", itemId);
                                    result.put("item_type", chatType.name().toLowerCase());
                                    result.put("content", content);

                                    // Track pending confirmation
                                    pendingConfirmations.put(itemId, new PendingItem(
                                            chatId, chatType.name().toLowerCase(), content, confidence, userId,
                                            isAdmin));
                                    return result;
                                });
                    }

                    return chatHistoryRepository.findByUserIdOrderByTimestampAsc(userId)
                            .collectList()
                            .flatMap(history -> {
                                StringBuilder promptBuilder = new StringBuilder();
                                promptBuilder.append(
                                        "You are SupremeAI, a highly intelligent coding and development assistant. Maintain a friendly and helpful tone.\n");
                                promptBuilder.append(
                                        "Below is the conversation history. Respond appropriately to the last user message considering the context:\n\n");

                                int startIdx = Math.max(0, history.size() - 5);
                                for (int i = startIdx; i < history.size(); i++) {
                                    ChatMessage pastMsg = history.get(i);
                                    String role = pastMsg.getRole() != null ? pastMsg.getRole()
                                            : (pastMsg.isAdmin() ? "admin" : "user");
                                    promptBuilder.append(role.toUpperCase()).append(": ").append(pastMsg.getContent())
                                            .append("\n");
                                }

                                if (history.isEmpty()
                                        || !history.get(history.size() - 1).getContent().equals(message)) {
                                    promptBuilder.append("USER: ").append(message).append("\n");
                                }

                                promptBuilder.append("AI: ");
                                String contextualPrompt = promptBuilder.toString();

                                return fallbackOrchestrator.executeWithSupremeIntelligence(
                                        "chat", "casual_chat", contextualPrompt, userId);
                            })
                            .onErrorResume(e -> Mono.just("আমি দুঃখিত, এই মুহূর্তে আমি উত্তর দিতে পারছি না। (AI Error: "
                                    + e.getMessage() + ")"))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(aiResponse -> {
                                ChatMessage aiMsg = new ChatMessage();
                                aiMsg.setId(generateId("chat_ai"));
                                aiMsg.setUserId(userId);
                                aiMsg.setContent(aiResponse);
                                aiMsg.setRole("ai");
                                aiMsg.setTimestamp(LocalDateTime.now());

                                result.put("response", aiResponse);

                                // Trigger Autonomous Learning
                                enhancedLearningService.learnFromInteraction(userId, message, aiResponse)
                                        .subscribeOn(Schedulers.parallel())
                                        .subscribe(
                                                v -> log.info("💡 Interaction learned for user: {}", userId),
                                                e -> log.error("⚠️ Failed to learn from interaction: {}",
                                                        e.getMessage()));

                                return chatHistoryRepository.save(aiMsg).thenReturn(result);
                            });
                });
    }

    private Mono<String> savePendingItem(ChatClassifier.ChatType chatType, String chatId, String content,
            double confidence, String userId) {
        String itemId = generateId(chatType.name().toLowerCase());

        return switch (chatType) {
            case RULE -> {
                ChatRule rule = new ChatRule(chatId, content, confidence, userId);
                rule.setId(itemId);
                rule.setCreatedAt(LocalDateTime.now());
                rule.setActive(true);
                yield chatRuleRepository.save(rule).thenReturn(itemId);
            }
            case PLAN -> {
                ChatPlan plan = new ChatPlan(chatId, content, confidence, userId);
                plan.setId(itemId);
                plan.setCreatedAt(LocalDateTime.now());
                plan.setActive(true);
                yield chatPlanRepository.save(plan).thenReturn(itemId);
            }
            case COMMAND -> {
                ChatCommand command = new ChatCommand(chatId, content, confidence, userId);
                command.setId(itemId);
                command.setCreatedAt(LocalDateTime.now());
                command.setActive(true);
                yield chatCommandRepository.save(command).thenReturn(itemId);
            }
            case ADMIN_ACTION -> {
                String actionType = determineAdminActionType(content);
                ChatAdminAction action = new ChatAdminAction(chatId, actionType, content, confidence, userId);
                action.setId(itemId);
                action.setCreatedAt(LocalDateTime.now());
                action.setActive(true);
                yield chatAdminActionRepository.save(action).thenReturn(itemId);
            }
            default -> Mono.just(itemId);
        };
    }

    private String determineAdminActionType(String content) {
        String lower = content.toLowerCase();
        if (lower.contains("api") || lower.contains("key"))
            return "ADD_API";
        if (lower.contains("website") || lower.contains("learn"))
            return "LEARN_WEBSITE";
        if (lower.contains("test") || lower.contains("health"))
            return "TEST_API";
        if (lower.contains("audit") || lower.contains("security"))
            return "RUN_AUDIT";
        return "GENERAL_ADMIN";
    }

    public Mono<Map<String, Object>> confirmItem(String itemId, boolean confirmed, String userId) {
        PendingItem pending = pendingConfirmations.get(itemId);
        if (pending == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "আইটেমটি পেন্ডিং কনফার্মেশনে পাওয়া যায়নি");
            response.put("item_id", itemId);
            return Mono.just(response);
        }

        // Save confirmation record
        ChatConfirmation confirmation = new ChatConfirmation(
                pending.getChatId(),
                pending.getChatType(),
                itemId,
                confirmed,
                userId);
        confirmation.setId(generateId("conf"));
        confirmation.setConfirmedAt(LocalDateTime.now());

        return chatConfirmationRepository.save(confirmation)
                .flatMap(savedConf -> updateItemStatus(pending.getChatType(), itemId, confirmed)
                        .thenReturn(savedConf))
                .map(savedConf -> {
                    // Remove from pending
                    pendingConfirmations.remove(itemId);

                    String statusMessage = confirmed
                            ? pending.getChatType() + " সফলভাবে কনফার্ম করা হয়েছে"
                            : pending.getChatType() + " প্রত্যাখ্যান করা হয়েছে";

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", statusMessage);
                    response.put("item_id", itemId);
                    response.put("item_type", pending.getChatType());
                    response.put("confirmed", confirmed);
                    response.put("confirmation_id", savedConf.getId());

                    return response;
                });
    }

    private Mono<Void> updateItemStatus(String itemType, String itemId, boolean active) {
        return switch (itemType) {
            case "rule" -> chatRuleRepository.findById(itemId)
                    .flatMap(rule -> {
                        rule.setActive(active);
                        rule.setUpdatedAt(LocalDateTime.now());
                        return chatRuleRepository.save(rule);
                    }).then();
            case "plan" -> chatPlanRepository.findById(itemId)
                    .flatMap(plan -> {
                        plan.setActive(active);
                        plan.setUpdatedAt(LocalDateTime.now());
                        return chatPlanRepository.save(plan);
                    }).then();
            case "command" -> chatCommandRepository.findById(itemId)
                    .flatMap(cmd -> {
                        cmd.setActive(active);
                        cmd.setUpdatedAt(LocalDateTime.now());
                        return chatCommandRepository.save(cmd);
                    }).then();
            case "admin_action" -> chatAdminActionRepository.findById(itemId)
                    .flatMap(action -> {
                        action.setActive(active);
                        action.setUpdatedAt(LocalDateTime.now());
                        return chatAdminActionRepository.save(action)
                                .flatMap(saved -> active ? executeAdminAction(saved) : Mono.empty());
                    }).then();
            default -> Mono.empty();
        };
    }

    private Mono<Void> executeAdminAction(ChatAdminAction action) {
        return switch (action.getActionType()) {
            case "ADD_API" -> {
                log.info("[ADD_API] Parsing provider from action content: {}", action.getContent());
                try {
                    // Parse JSON content: {"name":"...","type":"...","apiKey":"..."}
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> fields = mapper.readValue(
                            action.getContent(), java.util.HashMap.class);

                    String name = String.valueOf(fields.getOrDefault("name", action.getContent()));
                    String type = String.valueOf(fields.getOrDefault("type",
                            fields.getOrDefault("provider", "openai")));
                    String apiKey = String.valueOf(fields.getOrDefault("apiKey", ""));

                    APIProvider provider = new APIProvider(
                            "chat_" + System.currentTimeMillis(),
                            name,
                            type,
                            "active");
                    provider.setApiKey(apiKey);
                    provider.setDescription("Added via SupremeAI Chat");
                    provider.setAddedAt(new Date());

                    aiProviderService.saveProvider(provider);
                    log.info("[ADD_API] Provider '{}' (type={}) saved successfully", name, type);

                    // Log the action completion back to Firestore
                    chatAdminActionRepository.save(action)
                            .subscribe(
                                    saved -> log.info("[ADD_API] Action record updated"),
                                    err -> log.warn("[ADD_API] Failed to update action record: {}", err.getMessage()));

                } catch (Exception e) {
                    log.error("[ADD_API] Failed to parse content or create provider: {}", e.getMessage(), e);
                }
                yield Mono.empty();
            }
            case "LEARN_WEBSITE" -> {
                String content = action.getContent();
                List<String> topics = Arrays.stream(content.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
                log.info("[LEARN_WEBSITE] Initiating scraping for {} topics: {}", topics.size(), topics);
                yield knowledgeService.processMultipleWebsites(topics);
            }
            case "TEST_API" -> Mono.fromRunnable(() -> {
                log.info("[TEST_API] Triggering on-demand provider validation...");
                if (validationService != null) {
                    java.util.Map<String, Object> result = validationService.testAllProviders();
                    log.info("[TEST_API] Validation complete: {}", result);
                }
            }).then();
            case "RUN_AUDIT" -> Mono.fromRunnable(() -> log.info("[RUN_AUDIT] Security audit command received. "
                    + "Current score: 66/100 — see docs/audit/ for full report.")).then();
            default -> Mono.empty();
        };
    }

    public List<Map<String, Object>> getPendingConfirmations(String userId) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map.Entry<String, PendingItem> entry : pendingConfirmations.entrySet()) {
            PendingItem p = entry.getValue();
            if (userId == null || p.getUserId().equals(userId)) {
                Map<String, Object> item = new HashMap<>();
                item.put("item_id", entry.getKey());
                item.put("chat_id", p.getChatId());
                item.put("item_type", p.getChatType());
                item.put("content", p.getContent());
                item.put("confidence", p.getConfidence());
                item.put("user_id", p.getUserId());
                item.put("is_admin", p.isAdmin());
                items.add(item);
            }
        }
        return items;
    }

    public Mono<List<Map<String, Object>>> getRules(boolean activeOnly) {
        return (activeOnly
                ? chatRuleRepository.findByActive(true).collectList()
                : chatRuleRepository.findAll().collectList())
                .map(this::convertEntityList);
    }

    public Mono<List<Map<String, Object>>> getPlans(boolean activeOnly) {
        return (activeOnly
                ? chatPlanRepository.findByActive(true).collectList()
                : chatPlanRepository.findAll().collectList())
                .map(this::convertEntityList);
    }

    public Mono<List<Map<String, Object>>> getCommands(boolean activeOnly) {
        return (activeOnly
                ? chatCommandRepository.findByActive(true).collectList()
                : chatCommandRepository.findAll().collectList())
                .map(this::convertEntityList);
    }

    private <T> List<Map<String, Object>> convertEntityList(List<T> entities) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (entities == null)
            return list;
        for (T entity : entities) {
            Map<String, Object> map = new HashMap<>();
            if (entity instanceof ChatRule rule) {
                map.put("id", rule.getId());
                map.put("chat_id", rule.getChatId());
                map.put("content", rule.getContent());
                map.put("confidence", rule.getConfidence());
                map.put("created_by", rule.getCreatedBy());
                map.put("created_at", rule.getCreatedAt() != null ? rule.getCreatedAt().toString() : null);
                map.put("active", rule.isActive());
            } else if (entity instanceof ChatPlan plan) {
                map.put("id", plan.getId());
                map.put("chat_id", plan.getChatId());
                map.put("content", plan.getContent());
                map.put("confidence", plan.getConfidence());
                map.put("created_by", plan.getCreatedBy());
                map.put("created_at", plan.getCreatedAt() != null ? plan.getCreatedAt().toString() : null);
                map.put("active", plan.isActive());
            } else if (entity instanceof ChatCommand cmd) {
                map.put("id", cmd.getId());
                map.put("chat_id", cmd.getChatId());
                map.put("content", cmd.getContent());
                map.put("confidence", cmd.getConfidence());
                map.put("created_by", cmd.getCreatedBy());
                map.put("created_at", cmd.getCreatedAt() != null ? cmd.getCreatedAt().toString() : null);
                map.put("active", cmd.isActive());
            }
            list.add(map);
        }
        return list;
    }

    public Mono<List<Map<String, Object>>> getChatHistory(String userId, int limit) {
        return (userId != null
                ? chatHistoryRepository.findByUserId(userId)
                : chatHistoryRepository.findAll())
                .take(limit)
                .collectList()
                .map(this::convertChatMessageList);
    }

    public Mono<Map<String, Object>> getItemById(String itemType, String itemId) {
        return switch (itemType) {
            case "rule" -> chatRuleRepository.findById(itemId).map(this::entityToMap);
            case "plan" -> chatPlanRepository.findById(itemId).map(this::entityToMap);
            case "command" -> chatCommandRepository.findById(itemId).map(this::entityToMap);
            default -> Mono.empty();
        };
    }

    private Map<String, Object> entityToMap(Object entity) {
        Map<String, Object> map = new HashMap<>();
        if (entity instanceof ChatRule rule) {
            map.put("id", rule.getId());
            map.put("chat_id", rule.getChatId());
            map.put("content", rule.getContent());
            map.put("confidence", rule.getConfidence());
            map.put("created_by", rule.getCreatedBy());
            map.put("created_at", rule.getCreatedAt() != null ? rule.getCreatedAt().toString() : null);
            map.put("active", rule.isActive());
        } else if (entity instanceof ChatPlan plan) {
            map.put("id", plan.getId());
            map.put("chat_id", plan.getChatId());
            map.put("content", plan.getContent());
            map.put("confidence", plan.getConfidence());
            map.put("created_by", plan.getCreatedBy());
            map.put("created_at", plan.getCreatedAt() != null ? plan.getCreatedAt().toString() : null);
            map.put("active", plan.isActive());
        } else if (entity instanceof ChatCommand cmd) {
            map.put("id", cmd.getId());
            map.put("chat_id", cmd.getChatId());
            map.put("content", cmd.getContent());
            map.put("confidence", cmd.getConfidence());
            map.put("created_by", cmd.getCreatedBy());
            map.put("created_at", cmd.getCreatedAt() != null ? cmd.getCreatedAt().toString() : null);
            map.put("active", cmd.isActive());
        }
        return map;
    }

    private List<Map<String, Object>> convertChatMessageList(List<ChatMessage> messages) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (messages == null)
            return list;
        for (ChatMessage msg : messages) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", msg.getId());
            map.put("user_id", msg.getUserId());
            map.put("content", msg.getContent());
            map.put("role", msg.getRole());
            map.put("created_at", msg.getTimestamp() != null ? msg.getTimestamp().toString() : null);
            list.add(map);
        }
        return list;
    }

    private List<Map<String, Object>> convertConfirmationList(List<ChatConfirmation> confirmations) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (confirmations == null)
            return list;
        for (ChatConfirmation conf : confirmations) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", conf.getId());
            map.put("item_id", conf.getItemId());
            map.put("chat_id", conf.getChatId());
            map.put("confirmed_by", conf.getConfirmedBy());
            map.put("confirmed_at", conf.getConfirmedAt() != null ? conf.getConfirmedAt().toString() : null);
            list.add(map);
        }
        return list;
    }

    public Mono<List<Map<String, Object>>> getConfirmationHistory(String itemId, String chatId) {
        return (itemId != null
                ? chatConfirmationRepository.findByItemId(itemId).collectList()
                : chatId != null
                        ? chatConfirmationRepository.findByChatId(chatId).collectList()
                        : chatConfirmationRepository.findAll().collectList())
                .map(this::convertConfirmationList);
    }

    private String generateId(String prefix) {
        return prefix + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSS"));
    }

    // Inner class for pending items
    private static class PendingItem {
        private final String chatId;
        private final String chatType;
        private final String content;
        private final double confidence;
        private final String userId;
        private final boolean isAdmin;

        public PendingItem(String chatId, String chatType, String content, double confidence,
                String userId, boolean isAdmin) {
            this.chatId = chatId;
            this.chatType = chatType;
            this.content = content;
            this.confidence = confidence;
            this.userId = userId;
            this.isAdmin = isAdmin;
        }

        public String getChatId() {
            return chatId;
        }

        public String getChatType() {
            return chatType;
        }

        public String getContent() {
            return content;
        }

        public double getConfidence() {
            return confidence;
        }

        public String getUserId() {
            return userId;
        }

        public boolean isAdmin() {
            return isAdmin;
        }
    }
}
