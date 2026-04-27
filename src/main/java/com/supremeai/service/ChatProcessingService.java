package com.supremeai.service;

import com.supremeai.model.*;
import com.supremeai.repository.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatProcessingService {

    private final ChatClassifier chatClassifier;
    private final ChatRuleRepository chatRuleRepository;
    private final ChatPlanRepository chatPlanRepository;
    private final ChatCommandRepository chatCommandRepository;
    private final ChatConfirmationRepository chatConfirmationRepository;
    private final ChatHistoryRepository chatHistoryRepository;

    // In-memory pending confirmations (like Flask)
    private final Map<String, PendingItem> pendingConfirmations = new ConcurrentHashMap<>();

    public ChatProcessingService(ChatClassifier chatClassifier,
                                 ChatRuleRepository chatRuleRepository,
                                 ChatPlanRepository chatPlanRepository,
                                 ChatCommandRepository chatCommandRepository,
                                 ChatConfirmationRepository chatConfirmationRepository,
                                 ChatHistoryRepository chatHistoryRepository) {
        this.chatClassifier = chatClassifier;
        this.chatRuleRepository = chatRuleRepository;
        this.chatPlanRepository = chatPlanRepository;
        this.chatCommandRepository = chatCommandRepository;
        this.chatConfirmationRepository = chatConfirmationRepository;
        this.chatHistoryRepository = chatHistoryRepository;
    }

    public Map<String, Object> processMessage(String userId, String message, boolean isAdmin) {
        // Save chat message
        ChatMessage chatMsg = new ChatMessage(userId, message, isAdmin);
        chatMsg.setId(generateId("chat"));
        chatHistoryRepository.save(chatMsg).subscribe();

        String chatId = chatMsg.getId();

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
            String itemId = savePendingItem(chatType, chatId, content, confidence, userId);

            result.put("needs_confirmation", true);
            result.put("item_id", itemId);
            result.put("item_type", chatType.name().toLowerCase());
            result.put("content", content);

            // Track pending confirmation
            pendingConfirmations.put(itemId, new PendingItem(
                chatId, chatType.name().toLowerCase(), content, confidence, userId, isAdmin
            ));
        }

        return result;
    }

    private String savePendingItem(ChatClassifier.ChatType chatType, String chatId, String content,
                                    double confidence, String userId) {
        String itemId = generateId(chatType.name().toLowerCase());

        switch (chatType) {
            case RULE -> {
                ChatRule rule = new ChatRule(chatId, content, confidence, userId);
                rule.setId(itemId);
                rule.setCreatedAt(LocalDateTime.now());
                rule.setActive(true);
                chatRuleRepository.save(rule).subscribe();
            }
            case PLAN -> {
                ChatPlan plan = new ChatPlan(chatId, content, confidence, userId);
                plan.setId(itemId);
                plan.setCreatedAt(LocalDateTime.now());
                plan.setActive(true);
                chatPlanRepository.save(plan).subscribe();
            }
            case COMMAND -> {
                ChatCommand command = new ChatCommand(chatId, content, confidence, userId);
                command.setId(itemId);
                command.setCreatedAt(LocalDateTime.now());
                command.setActive(true);
                chatCommandRepository.save(command).subscribe();
            }
        }

        return itemId;
    }

    public synchronized Map<String, Object> confirmItem(String itemId, boolean confirmed, String userId) {
        Map<String, Object> response = new HashMap<>();

        PendingItem pending = pendingConfirmations.get(itemId);
        if (pending == null) {
            response.put("success", false);
            response.put("message", "আইটেমটি পেন্ডিং কনফার্মেশনে পাওয়া যায়নি");
            response.put("item_id", itemId);
            return response;
        }

        // Save confirmation record
        ChatConfirmation confirmation = new ChatConfirmation(
            pending.getChatId(),
            pending.getChatType(),
            itemId,
            confirmed,
            userId
        );
        confirmation.setId(generateId("conf"));
        confirmation.setConfirmedAt(LocalDateTime.now());
        chatConfirmationRepository.save(confirmation).subscribe();

        // Update item active status
        updateItemStatus(pending.getChatType(), itemId, confirmed);

        // Remove from pending
        pendingConfirmations.remove(itemId);

        String statusMessage = confirmed
            ? pending.getChatType() + " সফলভাবে কনফার্ম করা হয়েছে"
            : pending.getChatType() + " প্রত্যাখ্যান করা হয়েছে";

        response.put("success", true);
        response.put("message", statusMessage);
        response.put("item_id", itemId);
        response.put("item_type", pending.getChatType());
        response.put("confirmed", confirmed);
        response.put("confirmation_id", confirmation.getId());

        return response;
    }

    private void updateItemStatus(String itemType, String itemId, boolean active) {
        switch (itemType) {
            case "rule" -> chatRuleRepository.findById(itemId)
                .doOnNext(rule -> {
                    rule.setActive(active);
                    rule.setUpdatedAt(LocalDateTime.now());
                    chatRuleRepository.save(rule).subscribe();
                }).subscribe();
            case "plan" -> chatPlanRepository.findById(itemId)
                .doOnNext(plan -> {
                    plan.setActive(active);
                    plan.setUpdatedAt(LocalDateTime.now());
                    chatPlanRepository.save(plan).subscribe();
                }).subscribe();
            case "command" -> chatCommandRepository.findById(itemId)
                .doOnNext(cmd -> {
                    cmd.setActive(active);
                    cmd.setUpdatedAt(LocalDateTime.now());
                    chatCommandRepository.save(cmd).subscribe();
                }).subscribe();
        }
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

    public List<Map<String, Object>> getRules(boolean activeOnly) {
        List<ChatRule> rules;
        if (activeOnly) {
            rules = chatRuleRepository.findByActiveTrue().collectList().block();
        } else {
            rules = chatRuleRepository.findAll().collectList().block();
        }
        return convertEntityList(rules);
    }

    public List<Map<String, Object>> getPlans(boolean activeOnly) {
        List<ChatPlan> plans;
        if (activeOnly) {
            plans = chatPlanRepository.findByActiveTrue().collectList().block();
        } else {
            plans = chatPlanRepository.findAll().collectList().block();
        }
        return convertEntityList(plans);
    }

    public List<Map<String, Object>> getCommands(boolean activeOnly) {
        List<ChatCommand> commands;
        if (activeOnly) {
            commands = chatCommandRepository.findByActiveTrue().collectList().block();
        } else {
            commands = chatCommandRepository.findAll().collectList().block();
        }
        return convertEntityList(commands);
    }

    private <T> List<Map<String, Object>> convertEntityList(List<T> entities) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (entities == null) return list;
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

    public List<Map<String, Object>> getChatHistory(String userId, int limit) {
        List<ChatMessage> chats;
        if (userId != null) {
            chats = chatHistoryRepository.findAll()
                .filter(msg -> msg.getUserId() != null && msg.getUserId().equals(userId))
                .collectList()
                .block();
        } else {
            chats = chatHistoryRepository.findAll().collectList().block();
        }
        if (chats == null) return Collections.emptyList();

        chats.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage chat : chats.subList(0, Math.min(limit, chats.size()))) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", chat.getId());
            map.put("user_id", chat.getUserId());
            map.put("message", chat.getContent());
            map.put("is_admin", chat.isAdmin());
            map.put("timestamp", chat.getTimestamp() != null ? chat.getTimestamp().toString() : null);
            result.add(map);
        }
        return result;
    }

    public Map<String, Object> getItemById(String itemType, String itemId) {
        return switch (itemType) {
            case "rule" -> chatRuleRepository.findById(itemId).map(this::entityToMap).block();
            case "plan" -> chatPlanRepository.findById(itemId).map(this::entityToMap).block();
            case "command" -> chatCommandRepository.findById(itemId).map(this::entityToMap).block();
            default -> null;
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

    public List<Map<String, Object>> getConfirmationHistory(String itemId, String chatId) {
        List<ChatConfirmation> confirmations;
        if (itemId != null) {
            confirmations = chatConfirmationRepository.findByItemId(itemId).collectList().block();
        } else if (chatId != null) {
            confirmations = chatConfirmationRepository.findByChatId(chatId).collectList().block();
        } else {
            confirmations = chatConfirmationRepository.findAll().collectList().block();
        }
        if (confirmations == null) return Collections.emptyList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatConfirmation c : confirmations) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("chat_id", c.getChatId());
            map.put("item_type", c.getItemType());
            map.put("item_id", c.getItemId());
            map.put("confirmed", c.isConfirmed());
            map.put("confirmed_by", c.getConfirmedBy());
            map.put("confirmed_at", c.getConfirmedAt() != null ? c.getConfirmedAt().toString() : null);
            result.add(map);
        }
        return result;
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

        public String getChatId() { return chatId; }
        public String getChatType() { return chatType; }
        public String getContent() { return content; }
        public double getConfidence() { return confidence; }
        public String getUserId() { return userId; }
        public boolean isAdmin() { return isAdmin; }
    }
}
