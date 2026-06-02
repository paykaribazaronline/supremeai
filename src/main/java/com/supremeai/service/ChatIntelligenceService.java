package com.supremeai.service;

import com.supremeai.model.ChatRule;
import com.supremeai.model.ChatPlan;
import com.supremeai.repository.ChatRuleRepository;
import com.supremeai.repository.ChatPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ChatIntelligenceService {
    public ChatIntelligenceService(ChatRuleRepository ruleRepository, ChatPlanRepository planRepository, com.supremeai.repository.ChatAdminActionRepository adminActionRepository) {
        this.ruleRepository = ruleRepository;
        this.planRepository = planRepository;
        this.adminActionRepository = adminActionRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(ChatIntelligenceService.class);




    public enum Intent {
        COMMAND,
        RULE,
        PROJECT_PLAN,
        INFO_COLLECTION,
        CASUAL,
        DEBUG,
        ADMIN_ACTION
    }

    public Intent classifyIntent(String message) {
        String lower = message.toLowerCase();
        
        if (lower.startsWith("always") || lower.contains("must") || lower.contains("rule") || lower.contains("never")) {
            return Intent.RULE;
        }
        
        if (lower.startsWith("run") || lower.startsWith("execute") || lower.contains("command") || lower.contains("start")) {
            return Intent.COMMAND;
        }
        
        if (lower.contains("plan") || lower.contains("milestone") || lower.contains("roadmap")) {
            return Intent.PROJECT_PLAN;
        }
        
        if (lower.contains("what is") || lower.contains("how to") || lower.contains("explain") || lower.contains("search")) {
            return Intent.INFO_COLLECTION;
        }
        
        if (lower.contains("error") || lower.contains("fix") || lower.contains("debug") || lower.contains("logs")) {
            return Intent.DEBUG;
        }

        if (lower.contains("api") || lower.contains("key") || lower.contains("website") || lower.contains("learn") || lower.contains("audit")) {
            return Intent.ADMIN_ACTION;
        }
        
        return Intent.CASUAL;
    }

    public Mono<Void> handleIntelligence(String chatId, String message, Intent intent, String user, Double confidence) {
        if (intent == Intent.RULE) {
            ChatRule rule = new ChatRule();
            rule.setChatId(chatId);
            rule.setContent(message);
            rule.setConfidence(confidence);
            rule.setCreatedBy(user);
            rule.setCreatedAt(LocalDateTime.now());
            rule.setActive(true);
            logger.info("Automatically detected and saving RULE for chat {}: {}", chatId, message);
            return ruleRepository.save(rule).then();
        }
        
        if (intent == Intent.PROJECT_PLAN) {
            ChatPlan plan = new ChatPlan();
            plan.setChatId(chatId);
            plan.setContent(message);
            plan.setConfidence(confidence);
            plan.setCreatedBy(user);
            plan.setCreatedAt(LocalDateTime.now());
            plan.setActive(true);
            logger.info("Automatically detected and saving PROJECT_PLAN for chat {}: {}", chatId, message);
            return planRepository.save(plan).then();
        }

        if (intent == Intent.ADMIN_ACTION) {
            com.supremeai.model.ChatAdminAction action = new com.supremeai.model.ChatAdminAction();
            action.setChatId(chatId);
            action.setContent(message);
            action.setConfidence(confidence);
            action.setUserId(user);
            action.setActionType(determineActionType(message));
            action.setCreatedAt(LocalDateTime.now());
            action.setActive(true);
            logger.info("Automatically detected and saving ADMIN_ACTION for chat {}: {}", chatId, message);
            return adminActionRepository.save(action).then();
        }
        
        return Mono.empty();
    }

    private String determineActionType(String content) {
        String lower = content.toLowerCase();
        if (lower.contains("api") || lower.contains("key")) return "ADD_API";
        if (lower.contains("website") || lower.contains("learn")) return "LEARN_WEBSITE";
        if (lower.contains("audit") || lower.contains("security")) return "RUN_AUDIT";
        return "GENERAL_ADMIN";
    }
}
