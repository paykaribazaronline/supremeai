package com.supremeai.service;

import com.supremeai.model.SystemInstruction;
import com.supremeai.repository.SystemInstructionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Service for dynamically managing and providing instructions to AI agents.
 * Rules are fetched from Firestore with a file-based fallback (AGENTS.md).
 */
@Service
public class DynamicInstructionService {
    public DynamicInstructionService(SystemInstructionRepository instructionRepository) {
        this.instructionRepository = instructionRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(DynamicInstructionService.class);


    /**
     * Aggregates all active instructions into a single string for the AI prompt.
     * @param taskType The type of task (e.g., "build", "chat", "code") to filter rules.
     * @return A Mono containing the formatted instructions.
     */
    public Mono<String> getCombinedInstructions(String taskType) {
        return instructionRepository.findAllByIsActiveOrderByPriorityDesc(true)
                .filter(ins -> ins.getApplicableTaskTypes() == null || 
                               ins.getApplicableTaskTypes().isEmpty() || 
                               ins.getApplicableTaskTypes().contains(taskType))
                .map(SystemInstruction::getContent)
                .collect(Collectors.joining("\n\n"))
                .flatMap(dbInstructions -> {
                    if (dbInstructions.isEmpty()) {
                        return Mono.just(getFallbackRules());
                    }
                    return Mono.just(dbInstructions);
                })
                .onErrorResume(e -> {
                    logger.error("Error fetching dynamic instructions: {}", e.getMessage());
                    return Mono.just(getFallbackRules());
                });
    }

    /**
     * Fallback to AGENTS.md file if database is empty or unreachable.
     */
    private String getFallbackRules() {
        try {
            ClassPathResource resource = new ClassPathResource("AGENT_RULES.md");
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "# SupremeAI Default Build Rules\n- Follow clean architecture.\n- Use Spring Boot 3 standards.";
        }
    }
}
