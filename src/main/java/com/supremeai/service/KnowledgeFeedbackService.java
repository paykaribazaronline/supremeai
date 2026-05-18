package com.supremeai.service;

import com.supremeai.model.SystemLearning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service to capture real-time feedback and system events as learned knowledge.
 * Part of the "Self-Correction Loop" for SupremeAI.
 */
@Service
public class KnowledgeFeedbackService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeFeedbackService.class);
    private final SystemLearningService learningService;

    public KnowledgeFeedbackService(SystemLearningService learningService) {
        this.learningService = learningService;
    }

    /**
     * Records a lesson learned from a system error or failure.
     */
    public Mono<SystemLearning> recordErrorLesson(String context, Throwable error, String suggestedFix) {
        String topic = "Error Lesson: " + context;
        String content = String.format("Error [%s] occurred in context [%s]. Resolution/Lesson: %s", 
                error.getClass().getSimpleName(), context, suggestedFix);
        
        SystemLearning lesson = createLesson(topic, "SYSTEM_RECOVERY", content, 0.95);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("error_type", error.getClass().getName());
        metadata.put("stacktrace_snippet", error.getMessage());
        lesson.setMetadata(metadata);
        
        logger.info("Recording autonomous error lesson: {}", topic);
        return learningService.addLearning(lesson);
    }

    /**
     * Records a performance optimization lesson.
     */
    public Mono<SystemLearning> recordPerformanceLesson(String operation, long durationMs, String optimization) {
        String topic = "Performance Insight: " + operation;
        String content = String.format("Operation [%s] took %dms. Optimization applied: %s", 
                operation, durationMs, optimization);
        
        SystemLearning lesson = createLesson(topic, "PERFORMANCE", content, 0.90);
        lesson.setMetadata(Map.of("duration_ms", durationMs, "operation", operation));
        
        logger.info("Recording performance insight: {}", topic);
        return learningService.addLearning(lesson);
    }

    /**
     * Records success patterns identified during high-concurrency tasks.
     */
    public Mono<SystemLearning> recordSuccessPattern(String patternName, String description) {
        String topic = "Success Pattern: " + patternName;
        SystemLearning lesson = createLesson(topic, "PATTERN_RECOGNITION", description, 0.85);
        
        logger.info("Recording success pattern: {}", topic);
        return learningService.addLearning(lesson);
    }

    private SystemLearning createLesson(String topic, String category, String content, double confidence) {
        SystemLearning lesson = new SystemLearning();
        lesson.setId("lesson_" + UUID.randomUUID().toString().substring(0, 8));
        lesson.setTopic(topic);
        lesson.setCategory(category);
        lesson.setContent(content);
        lesson.setConfidenceScore(confidence);
        lesson.setLearnedAt(LocalDateTime.now());
        lesson.setSources(List.of("autonomous-feedback-loop"));
        lesson.setPermanent(false);
        return lesson;
    }
}
